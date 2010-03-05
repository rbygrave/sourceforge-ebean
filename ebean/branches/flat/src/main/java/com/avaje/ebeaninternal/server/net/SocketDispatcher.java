/**
 *  Copyright (C) 2006  Robin Bygrave
 *  
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *  
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *  
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 */
package com.avaje.ebeaninternal.server.net;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This parses and dispatches a request to the appropriate handler.
 * <p>
 * Looks up the appropriate RequestHandler 
 * and then gets it to process the Client request.<P>
 * </p>
 * Note that this is a Runnable because it is assigned to the ThreadPool.
 */
public class SocketDispatcher implements Runnable {

	private static final Logger logger = Logger.getLogger(SocketDispatcher.class.getName());
	
    private Socket clientSocket;
    
    private SocketListener listener;
	
    /**
	 *  Create including the Listener (used to lookup the Request Handler) and
	 *  the socket itself. 
	 */
	public SocketDispatcher(SocketListener listener, Socket clientSocket) {
		this.clientSocket = clientSocket;
		this.listener = listener;
	}

	/**
	 * Setup ObjectInputStream and read the headers.
	 */
	protected Headers readHeaders(SocketConnection request) throws Exception {
		ObjectInputStream ois = request.getObjectInputStream();
		return (Headers)ois.readObject();
	}
	
	/**
	 *  This will parse out the command.  Lookup the appropriate Handler and 
	 *  pass the information to the handler for processing.
	 *  <P>Dev Note: the command parsing is processed here so that it is preformed
	 *  by the assigned thread rather than the listeners thread.</P>
	 */
	public void run() {
		try {
			
			try {
				
				SocketConnection request = new SocketConnection(clientSocket);

				Headers headers = readHeaders(request);
				
				request.setHeaders(headers);
				
				String processorId = headers.getProcessorId();
			
				// lookup the RequestHandler
				ConnectionProcessor reqHandler = listener.getRequestProcessor(processorId);		

				// get it to process the request....
				
				reqHandler.process(request);
				

				
			} catch (Exception e) {
				// the RequestHandler could not be found or initialised...
				// Fatal Configuration/Coding error... not expecting to handle this
				// in code but get the administrator/developer to fix the issue.
				// For example, not put the Handler in the props file.
				logger.log(Level.SEVERE, "Error handling message", e);
				// let the client know aswell...
				Headers response = new Headers();
				response.setThrowable(e);
				
				OutputStream os = clientSocket.getOutputStream();
				ObjectOutputStream oos = new ObjectOutputStream(os);
				
				oos.writeObject(response);
				os.flush();
			}
			
		} catch (IOException e) {
			// Error parsing out the Header of the request...
			// Not expecting anyone to handle this via code.  
			// Report the error and fix the code.
			logger.log(Level.SEVERE, null, e);
		}
	}


}; 
