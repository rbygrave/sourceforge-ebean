/**
 * Copyright (C) 2006  Robin Bygrave
 * 
 * This file is part of Ebean.
 * 
 * Ebean is free software; you can redistribute it and/or modify it 
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or
 * (at your option) any later version.
 *  
 * Ebean is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with Ebean; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA  
 */
package com.avaje.ebeaninternal.server.net;

import java.io.ObjectInputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * Processes ebean requests from http clients.
 */
public class ClientRequestServlet extends HttpServlet {

    private static final long serialVersionUID = -6030990877455396341L;
    
    private static final Logger logger = Logger.getLogger(ClientRequestServlet.class.getName());
    
    private transient CommandProcessor processor;
    
    /**
     * Create the servlet.
     */
    public ClientRequestServlet() {
        initProcessor();
    }


    private void initProcessor() {
        String dftl = ServletContextManager.class.getName();
        
        UtilFactory utilFactory = new UtilFactory();
        CommandContextManager cm = utilFactory.createCommandContextManager(dftl);
        
        CommandSecurity cs = utilFactory.createCommandSecurity(null);
        
        Authenticate au = utilFactory.createAuthenticate(null);
        
        processor = new CommandProcessor();
        processor.setUseSessionId(true);
        processor.setContextManager(cm);
        processor.setCommandSecurity(cs);
        processor.setAuthenticate(au);
    }

    /**
     * Process the GET request.
     */
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, java.io.IOException {

        doPost(request, response);
    }

    /**
     * Process the POST request.
     */
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, java.io.IOException {

        try {

            HttpSession session = request.getSession(true);
            String httpSessionId = session.getId();
            
            ServletConnection conn = new ServletConnection(request, response);
            
            // setup BeanObjectInputStream...
                        
            ObjectInputStream ois = conn.getObjectInputStream();
            
            // read the headers and set HttpSession 
            Headers headers = (Headers)ois.readObject();
            headers.set("server.httpsessionid", httpSessionId);
            headers.setSession(session);
            
            conn.setHeaders(headers);
            
            processor.process(conn);

        } catch (Exception e) {
        	logger.log(Level.SEVERE, null, e);
        }
    }

}
