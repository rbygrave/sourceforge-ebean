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
package com.avaje.ebean.server.lib.sql;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.avaje.ebean.server.lib.ConfigProperties;
import com.avaje.ebean.server.lib.GlobalProperties;
import com.avaje.ebean.server.lib.util.MailEvent;
import com.avaje.ebean.server.lib.util.MailListener;
import com.avaje.ebean.server.lib.util.MailMessage;
import com.avaje.ebean.server.lib.util.MailSender;
import com.avaje.lib.log.LogFactory;

/**
 * A simple smtp email alert that sends a email message
 * on dataSourceDown and dataSourceUp etc.
 * <ul>
 * <li>alert.fromuser = the from user name 
 * <li>alert.fromemail = the from email account
 * <li>alert.toemail = comma delimited list of email accounts to email
 * <li>alert.mailserver = the smpt server name
 * </ul>
 */
public class SimpleAlerter implements DataSourceAlertListener, MailListener {

	private static final Logger logger = LogFactory.get(SimpleAlerter.class);
	
	
    //boolean sendInBackGround = true;

    /**
     * Create a SimpleAlerter.
     */
    public SimpleAlerter() {
    }
    
    /**
     * If the email failed then log the error.
     */
    public void handleEvent(MailEvent event) {
        Throwable e =  event.getError();
        if (e != null){
        	logger.log(Level.SEVERE, null, e);
        }
    }
    
    /**
     * Send the dataSource down alert.
     */
    public void dataSourceDown(String dataSourceName) {
        String msg = getSubject(true, dataSourceName);
        sendMessage(msg, msg);
    }
    
    /**
     * Send the dataSource up alert.
     */
    public void dataSourceUp(String dataSourceName) {
        String msg = getSubject(false, dataSourceName);
        sendMessage(msg, msg);
    }
    
    /**
     * Send the warning message.
     */
    public void warning(String subject, String msg) {
        sendMessage(subject, msg);
    }
    
    private String getSubject(boolean isDown, String dsName) {
        String msg = "The DataSource "+dsName;
        if (isDown){
            msg += " is DOWN!!";
        } else {
            msg += " is UP.";
        }
        return msg;
    }
    
    private void sendMessage(String subject, String msg){
        
    	ConfigProperties properties = GlobalProperties.getConfigProperties();
    	
        String fromUser 		= properties.getProperty("alert.fromuser");
        String fromEmail 		= properties.getProperty("alert.fromemail");
        String mailServerName 	= properties.getProperty("alert.mailserver");
        String toEmail 			= properties.getProperty("alert.toemail");        

        if (mailServerName == null){
            //throw new RuntimeException("alert.mailserver not set...");
            return;
        }
        
        MailMessage data = new MailMessage();
        data.setSender(fromUser, fromEmail);
        data.addBodyLine(msg);
        data.setSubject(subject);
        
        String[] toList = toEmail.split(",");
        if (toList.length==0) {
            throw new RuntimeException("alert.toemail has not been set?");
        }
        for (int i = 0; i < toList.length; i++) {
            data.addRecipient(null, toList[i].trim());
        }
        
        MailSender sender = new MailSender(mailServerName);
        sender.setMailListener(this);
        sender.sendInBackground(data);
    }
    
       
}
