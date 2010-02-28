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
package com.avaje.ebean.server.net;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * An HttpServletRequest based IoConnection.
 */
public class ServletConnection extends IoConnection {

    private final HttpServletRequest request;
    
    private final HttpServletResponse response;
    
    /**
     * Create with the HttpServlet request and response.
     */
    public ServletConnection(HttpServletRequest req, HttpServletResponse res) throws IOException {
        super(req.getInputStream(), res.getOutputStream());
        this.request = req;
        this.response = res;
    }

	public HttpServletRequest getRequest() {
		return request;
	}

	public HttpServletResponse getResponse() {
		return response;
	}
    
    
}
