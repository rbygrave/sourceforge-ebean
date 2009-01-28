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
package org.avaje.ebean.server.net;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.avaje.ebean.server.lib.ConfigProperties;
import org.avaje.ebean.server.lib.GlobalProperties;
import org.avaje.ebean.server.lib.thread.ThreadPool;
import org.avaje.ebean.server.lib.thread.ThreadPoolManager;
import org.avaje.lib.log.LogFactory;

/**
 * Serverside multithreaded socket listener. Accepts connections and dispatches
 * them to an appropriate handler.
 * <p>
 * This is designed as a single port listener, where part of the connection
 * protocol determines which service the client is requesting (rather than a
 * port per service).
 * </p>
 * <p>
 * It has its own daemon background thread that handles the accept() loop on the
 * ServerSocket.
 * </p>
 */
public class SocketListener implements Runnable {

	private static final Logger logger = LogFactory.get(SocketListener.class);
	
    /**
     * The port the SocketListener uses.
     */
    int port = -1;

    /**
     * The length of the socket accept timeout.
     */
    int listenTimeout = 60000;

    /**
     * The server socket used to listen for requests.
     */
    ServerSocket serverListenSocket;

    /**
     * shutting down flag.
     */
    boolean doingShutdown = false;

    /**
     * The listening thread.
     */
    Thread listenerThread;

    /**
     * Whether the listening thread is busy assigning a request to a thread.
     */
    boolean isActive = false;

    /**
     * The pool of threads that actually do the parsing execution of requests.
     */
    ThreadPool threadPool;

    /**
     * A cache of the ConnectionProcessor.
     */
    HashMap<String,ConnectionProcessor> processorMap = new HashMap<String, ConnectionProcessor>();

    final ConfigProperties configProperties;
    
    /**
     * Construct with a given thread pool name.
     */
    public SocketListener(String threadPoolName, int port) {
    	configProperties = GlobalProperties.getConfigProperties();
        threadPool = ThreadPoolManager.getThreadPool(threadPoolName);
        this.port = port;
    }

    /**
     * Returns the port the listener is using.
     */
    public int getPort() {
        return port;
    }

    /**
     * Start listening for requests.
     */
    public void startListening() throws IOException {

        serverListenSocket = new ServerSocket(port);
        serverListenSocket.setSoTimeout(listenTimeout);

        this.listenerThread = new Thread(this, "avaje.lib.SocketListener");
        this.listenerThread.setDaemon(true);
        this.listenerThread.start();
    }

    /**
     * Gets a given service (RequestHandler) based on the serviceKey.
     */
    public ConnectionProcessor getRequestProcessor(String serviceKey) throws IOException {

        ConnectionProcessor handler = (ConnectionProcessor) processorMap.get(serviceKey);
        if (handler == null) {
            synchronized (processorMap) {
                handler = (ConnectionProcessor) processorMap.get(serviceKey);
                if (handler == null) {
                    handler = createRequestHandler(serviceKey);
                    processorMap.put(serviceKey, handler);
                }
            }
        }
        return handler;
    }

    /**
     * Factory method that creates the RequestHandler for a particular
     * serviceKey. Note that RequestHandler's should be thread safe and only one
     * is required (rather than one per request).
     * 
     * @param serviceKey the string key of the service.
     * @exception NetException indicates there was a problem creating the
     *                RequestHandler.
     * @return the RequestHandler
     */
    protected ConnectionProcessor createRequestHandler(String serviceKey) throws IOException {

        serviceKey = serviceKey.toLowerCase();
        String handlerName = configProperties.getProperty("connectionprocessor." + serviceKey);
        if (handlerName == null) {
            String msg = "No ConnectionProcessor has been defined for [" + serviceKey + "]";
            throw new IOException(msg);
        }
        try {
            Class<?> hClass = Class.forName(handlerName);
            ConnectionProcessor h = (ConnectionProcessor) hClass.newInstance();
            return h;

        } catch (Exception e) {
        	logger.log(Level.SEVERE, null, e);
            throw new IOException("Exception " + e.getMessage());
        }
    }

    /**
     * Register the service (RequestHandler) with a given serviceKey. Note: That
     * you don't have to register this way and that if you don't the service
     * will be looked up via the service mapping props file.
     */
    public void registerRequestHandler(String serviceKey, ConnectionProcessor handler) {
        synchronized (processorMap) {
            processorMap.put(serviceKey, handler);
        }
    }

    public void registerRequestHandler(String serviceKey, String requestHandlerClassName) {
        try {
            Class<?> c = Class.forName(requestHandlerClassName);
            ConnectionProcessor requestHandler = (ConnectionProcessor) c.newInstance();
            registerRequestHandler(serviceKey, requestHandler);
        } catch (Exception e) {
            String msg = "Error creating RequestHandler [" + serviceKey + "]["
                    + requestHandlerClassName + "]";
            logger.log(Level.SEVERE, msg, e);
        }
    }

    /**
     * Shutdown this listener.
     */
    public void shutdown() {
        doingShutdown = true;
        try {
            if (isActive) {
                synchronized (listenerThread) {
                    try {
                        listenerThread.wait(1000);
                    } catch (InterruptedException e) {
                        // OK to ignore as expected to Interrupt for shutdown.
                        ;
                    }
                }
            }
            listenerThread.interrupt();
            serverListenSocket.close();
        } catch (IOException e) {
        	logger.log(Level.SEVERE, null, e);
        }
    }

    protected Runnable createRunnable(SocketListener listener, Socket clientSocket) {
    	return new SocketDispatcher(this, clientSocket);
    }
    
    /**
     * This is a runnable and so this must be public. Don't call this externally
     * but rather call the startListening() method.
     */
    public void run() {
        // run in loop until doingShutdown is true...
        while (!doingShutdown) {
            try {
                synchronized (listenerThread) {
                    Socket clientSocket = serverListenSocket.accept();

                    isActive = true;
                    
                    Runnable request = createRunnable(this, clientSocket);
                    threadPool.assign(request, true);

                    isActive = false;
                }
            } catch (SocketException e) {
                if (doingShutdown) {
                    String msg = "org.avaje.lib.SocketListener> doingShutdown and accept threw:"
                            + e.getMessage();
                    logger.info(msg);

                } else {
                	logger.log(Level.SEVERE, null, e);
                }

            } catch (InterruptedIOException e) {
                // this will happen when the server is very quiet... aka no
                // requests
                logger.fine("Possibly expected due to accept timeout?" + e.getMessage());

            } catch (IOException e) {
                // log it and continue in the loop...
            	logger.log(Level.SEVERE, null, e);
            }
        }
    }

}
