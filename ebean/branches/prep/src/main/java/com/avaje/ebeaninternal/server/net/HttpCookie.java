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

import java.net.URL;
import java.util.Date;
import java.util.StringTokenizer;

/**
 * A HttpCookie.
 */
public class HttpCookie {
	
	private Date expirationDate;

	private String nameAndValue;

	private String path;

	private String domain;

	private boolean isSecure;

	HttpCookieDateParser dateParser;

	/**
	 * Create the HttpCookie with a raw cookie string to parse.
	 */
	public HttpCookie(String rawCookieString, HttpCookieDateParser dateParser) {
		this.isSecure = false;
		this.dateParser = dateParser;
		parseCookieString(rawCookieString);
	}

	/**
	 * Create the HttpCookie with separate values.
	 */
	public HttpCookie(Date expirationDate, String nameAndValue, String path, String domain,
			boolean isSecure) {
		this.isSecure = false;
		this.expirationDate = new Date(expirationDate.getTime());
		this.nameAndValue = nameAndValue;
		this.path = path;
		this.domain = domain;
		this.isSecure = isSecure;
	}

	/**
	 * Create getting defaults from the URL.
	 */
	public HttpCookie(URL url, String rawCookieString) {
		this.isSecure = false;
		parseCookieString(rawCookieString);
		applyDefaults(url);
	}

	/**
	 * Apply path and domain from the url.
	 */
	private void applyDefaults(URL url) {
		if (domain == null) {
			domain = url.getHost();
		}
		if (path == null) {
			path = url.getFile();
			int i = path.lastIndexOf("/");
			if (i > -1) {
				path = path.substring(0, i);
			}
		}
	}

	private void parseCookieString(String s) {

		StringTokenizer stringtokenizer = new StringTokenizer(s, ";");
		stringtokenizer.hasMoreTokens();

		// the name and value of the cookie...
		nameAndValue = stringtokenizer.nextToken().trim();

		// parse out the additional parameters...
		while (stringtokenizer.hasMoreTokens()) {
			String s1 = stringtokenizer.nextToken().trim();
			if (s1.equalsIgnoreCase("secure")) {
				isSecure = true;
			} else {
				int i = s1.indexOf("=");
				if (i >= 0) {
					// get the name value pair...
					String name = s1.substring(0, i);
					String value = s1.substring(i + 1);
					if (name.equalsIgnoreCase("path")) {
						path = value;

					} else if (name.equalsIgnoreCase("domain")) {
						if (value.indexOf(".") == 0) {
							domain = value.substring(1);
						} else {
							domain = value;
						}
					} else if (name.equalsIgnoreCase("expires")) {
						expirationDate = parseExpireDate(value);
					}
				}
			}
		}
	}

	/**
	 * Return the name and value.
	 */
	public String getNameValue() {
		return nameAndValue;
	}

	/**
	 * Return the cookie name.
	 */
	public String getName() {
		int i = nameAndValue.indexOf("=");
		return nameAndValue.substring(0, i);
	}

	/**
	 * Return the domain.
	 */
	public String getDomain() {
		return domain;
	}

	/**
	 * Return the path.
	 */
	public String getPath() {
		return path;
	}

	/**
	 * Return the expiration date.
	 */
	public Date getExpirationDate() {
		return expirationDate;
	}

	/**
	 * Return true if it has expired.
	 */
	public boolean hasExpired() {
		return expirationDate != null && expirationDate.getTime() <= System.currentTimeMillis();
	}

	/**
	 * Return true if it is secure.
	 */
	public boolean isSecure() {
		return isSecure;
	}

	private Date parseExpireDate(String expireDate) {
		return dateParser.parse(expireDate);
	}

	public String toString() {
		String s = nameAndValue;
		if (expirationDate != null)
			s = s + "; expires=" + expirationDate;
		if (path != null)
			s = s + "; path=" + path;
		if (domain != null)
			s = s + "; domain=" + domain;
		if (isSecure)
			s = s + "; secure";
		return s;
	}


}
