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

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.avaje.ebeaninternal.net.AuthenticateResponse;
import com.avaje.ebeaninternal.net.Command;
import com.avaje.ebeaninternal.net.CommandContext;
import com.avaje.ebeaninternal.net.CommandList;
import com.avaje.ebeaninternal.net.Constants;

/**
 * Server side processor for client commands.
 */
public class CommandProcessor implements ConnectionProcessor, Constants {

	private static final Logger logger = Logger.getLogger(CommandProcessor.class.getName());
	
    CommandContextManager contextManager;
    
    Authenticate authenticate;
    
    CommandSecurity commandSecurity;
    
    boolean useSessionId = false;
    
    long sessCounter = 0;
    
    boolean embedMethodInfo = false;

    /**
     * Create the processor specifying whether to include Authenticate.
     */
    public CommandProcessor() {
        
    }
    
    /**
     * Set the handler for Authentication.
     */
    public void setAuthenticate(Authenticate authenticate) {
        this.authenticate = authenticate;
    }

    /**
     * Set the handler for Context management.
     */
    public void setContextManager(CommandContextManager contextManager) {
        this.contextManager = contextManager;
    }

    /**
     * Set the handler for command security.
     */
    public void setCommandSecurity(CommandSecurity commandSecurity) {
        this.commandSecurity = commandSecurity;
    }

    /**
     * Set whether sessionIds should be used.
     */
    public void setUseSessionId(boolean useSessionId) {
        this.useSessionId = useSessionId;
    }

    /**
     * Authenticate the user.
     */
    public AuthenticateResponse authenticate(String un, String pwd, Object obj){
        return authenticate.authenticate(un, pwd, obj);
    }
    
    private String nextSessionId() {        
        synchronized (this) {
            return ++sessCounter+"";
        }
    }
    
    /**
     * execute the command in the given context.
     */
    protected void execute(CommandContext context, Command command){
        if (commandSecurity != null){
            if (!commandSecurity.allow(context, command)) {
                String m = "Disallowed Command "+command+" "+context;
            	logger.log(Level.SEVERE, m);
                return;
            }
        }
        command.execute(context);
    }
    
    
    /**
     * Process the request.
     * <p>
     * The request contains either a Command or a CommandList.
     * </p>
     */
    public void process(IoConnection request) {
        
        Headers h = request.getHeaders();
        try {
        	
        	Object responseObject = null;
            
            CommandContext context = contextManager.getContext(h);
            context.setProcessor(this);
            
            Object payload = request.readObject();
            if (payload instanceof Command){
                Command command = (Command)payload;
                execute(context, command);
                responseObject = command;
                
            } else {
                CommandList commandList = (CommandList)payload;
    
                List<Command> cmdList = commandList.list();
                for (int i = 0; i < cmdList.size(); i++) {
                	Command cmd = (Command)cmdList.get(i);
                    execute(context, cmd);
                }
                responseObject = commandList;
            }
            
            Headers response = new Headers();
            response.setSuccess();
            
            if (useSessionId){
                String sessId = h.get(SESSION_ID_KEY);
                if (sessId == null){
                    // send the sessionId back to client
                    sessId = nextSessionId();
                    response.set(SESSION_ID_KEY, sessId);
                }
            }
            
            request.writeObject(response);
            request.writeObject(responseObject);
            request.flush();
           
            
        } catch (Exception ex) {
            String msg = "Error handling request:" + h;
        	logger.log(Level.SEVERE, msg, ex);

            try {
                Headers res = new Headers();
                res.setThrowable(ex);
                request.writeObject(res).flush();
            } catch (IOException e) {
                String m = "Error sending error response back to client:" + e.getMessage();
            	logger.log(Level.SEVERE, m, e);
            }
        }
    }
    
}
