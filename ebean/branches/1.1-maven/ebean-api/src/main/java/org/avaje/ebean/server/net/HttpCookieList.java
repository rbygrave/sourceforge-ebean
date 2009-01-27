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

import java.net.URLConnection;
import java.util.*;

/**
 * A list of HttpCookies.
 */
public class HttpCookieList {

	private Hashtable<String,HttpCookie> cookieMap = new Hashtable<String,HttpCookie>();

	HttpCookieDateParser dateParser = new HttpCookieDateParser();
	
	/**
	 * Read the cookies from the urlConnection.
	 */
	public void getCookiesFromConnection(URLConnection urlconnection) {

		String headerKey;
		for (int i = 1; (headerKey = urlconnection.getHeaderFieldKey(i)) != null; i++) {
			if (headerKey.equalsIgnoreCase("set-cookie")) {
				String rawCookieHeader = urlconnection.getHeaderField(i);
		
				HttpCookie cookie = new HttpCookie(rawCookieHeader, dateParser);
				cookieMap.put(cookie.getName(), cookie);
			}
		}
	}

	/**
	 * Set the cookies to the urlConnection.
	 */
	public void setCookiesToConnection(URLConnection urlconnection) {

		if (cookieMap.size() < 1) {
			return;
		}

		StringBuffer sb = new StringBuffer();
		boolean first = true;
		Enumeration<HttpCookie> e = cookieMap.elements();
		while (e.hasMoreElements()) {
			HttpCookie cookie = (HttpCookie) e.nextElement();
			if (first) {
				first = false;
			} else {
				sb.append("; ");
			}
			sb.append(cookie.getNameValue());
		}

		urlconnection.setRequestProperty("Cookie", sb.toString());
	}

}
