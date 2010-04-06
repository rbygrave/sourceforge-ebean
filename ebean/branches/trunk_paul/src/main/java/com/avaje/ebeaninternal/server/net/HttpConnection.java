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
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLConnection;

/**
 * A HttpConnection implementation of IoConnection.
 */
public class HttpConnection extends IoConnection {

    URLConnection urlConnection;
    
    HttpCookieList cookieList;
    
    /**
     * Create with a connection and cookielist.
     */
    public HttpConnection(URLConnection urlConnection, HttpCookieList cookieList) {
        super(null,null);
        this.urlConnection = urlConnection;
        this.cookieList = cookieList;
    }
    
    /**
     * Disconnect.
     */
    public void disconnect() throws IOException {
        super.disconnect();
    }

    
    /**
     * Get the outputStream.
     */
    public OutputStream getOutputStream() throws IOException {
        if (os == null){
            cookieList.setCookiesToConnection(urlConnection);
            os = urlConnection.getOutputStream();
        }
        return os;
    }
    
    /**
     * Get the inputStream.
     */
    public InputStream getInputStream() throws IOException {
        if (is == null){
            cookieList.getCookiesFromConnection(urlConnection);
            is = urlConnection.getInputStream();
        }
        return is;
    }

}
