/**
 * Copyright (C) 2009 Authors
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
package com.avaje.ebeaninternal.server.cluster.mcast;

import java.util.ArrayList;
import java.util.List;

public class AckResendMessages {

    ArrayList<Message> messages = new ArrayList<Message>();
    
    public String toString() {
        return messages.toString();
    }
    
    public int size() {
        return messages.size();
    }
    
    public void add(MessageAck ack){
        messages.add(ack);
    }
    
    public void add(MessageResend resend){
        messages.add(resend);
    }
    
    public List<Message> getMessages() {
        return messages;
    }
}
