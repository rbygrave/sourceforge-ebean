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

import com.avaje.ebeaninternal.net.AuthenticateResponse;

/**
 * For testing purposes only. Please look at the code.
 */
public class TestAuthenticate implements Authenticate {

    int userCounter = 0;
    
    /**
     * Please look at the code.
     */
    public AuthenticateResponse authenticate(String un, String pwd, Object obj){
    
        if (un != null) {
            if (un.equalsIgnoreCase("invalid")){
                return new AuthenticateResponse(AuthenticateResponse.INVALID_USER);
            }
            if (un.equalsIgnoreCase("invalidpwd")) {
                return new AuthenticateResponse(AuthenticateResponse.INVALID_USER_PWD);
            }
            if (un.equalsIgnoreCase("inactive")) {
                return new AuthenticateResponse(AuthenticateResponse.INACTIVE_USER);
            }
        }
       
        
        String id = null;
        synchronized (this) {
            id = (++userCounter)+"";
        }
        
        AuthenticateResponse res = new AuthenticateResponse(id);
        return res;
    }
}
