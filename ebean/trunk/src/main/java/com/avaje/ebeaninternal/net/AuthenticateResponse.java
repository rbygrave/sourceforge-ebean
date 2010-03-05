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
package com.avaje.ebeaninternal.net;

/**
 * Response from an authentication request.
 */
public class AuthenticateResponse {

	/**
	 * The authentication suceeded.
	 */
    public static final int SUCCESS =  0;

    /**
     * The user was invalid error.
     */
    public static final int INVALID_USER =  1;
    
    /**
     * The user password was invalid error.
     */
    public static final int INVALID_USER_PWD =  2;
    
    /**
     * The user is currently inactive error.
     */
    public static final int INACTIVE_USER =  3;
    
    
    String userId;
    
    /**
     * The code indicating the 
     */
    int code = SUCCESS;
    
    /**
     * Constructor for unsuccessful authentication.
     */
    public AuthenticateResponse(int invalidCode) {
        this.code = invalidCode;
    }
    
    /**
     * Constructor for successful authentication.
     */
    public AuthenticateResponse(String userId) {
        this.code = SUCCESS;
        this.userId = userId;
    }

    /**
     * Return true if the authentication suceeded.
     */
    public boolean isSuccess() {
        return (code == SUCCESS);
    }
    
    /**
     * Return the userId.
     */
    public String getUserId() {
        return userId;
    }
    
    /**
     * Return the response code.
     * One of SUCCESS, INVALID_USER, INVALID_USER_PWD, INACTIVE_USER.
     */
    public int getCode() {
        return code;
    }
    
}
