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
package org.avaje.ebean.server.net;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;

import org.avaje.ebean.server.lib.ConfigProperties;
import org.avaje.ebean.server.lib.GlobalProperties;
import org.avaje.lib.log.LogFactory;

/**
 * Http implementation of NetClient.
 */
public class HttpClient implements IoConnectionFactory {

	private static final Logger logger = LogFactory.get(HttpClient.class);
	
    private static final String HTTP = "http";
    
    private static final String HTTPS = "https";
    
    protected String host = "127.0.0.1";

    protected int port = 80;

    protected int securePort = 443;
    
    protected String url = "/app/jcr/client";

    protected HttpCookieList cookieList = new HttpCookieList();

    /**
     * Create a HttpClient.
     */
    public HttpClient() {
        initialiseHostNameVerifier();
    }

    /**
     * initialise a HostnameVerifier for use with HttpsURLConnection.
     */
    public void initialiseHostNameVerifier() {
        HostnameVerifier verifier = new NoHostnameVerify();
        
        ConfigProperties props = GlobalProperties.getConfigProperties();
        
        String hnv = props.getProperty("avaje.httpclient.hostnameverifier");
        if (hnv != null){
            try {
                Class<?> clz = Class.forName(hnv);
                verifier = (HostnameVerifier)clz.newInstance();
            } catch (Exception ex){
                throw new RuntimeException(ex);
            }
        }
        HttpsURLConnection.setDefaultHostnameVerifier(verifier);
    }
    
    /**
     * Create a connection.
     */
    public IoConnection createConnection(boolean secure) throws IOException {
        URLConnection urlConn = createURLConnection(secure, -1);
        HttpConnection httpConn = new HttpConnection(urlConn, cookieList);
        return httpConn;
    }

    /**
     * Create a URLConnection with the secure and length parameters.
     */
    protected URLConnection createURLConnection(boolean secure, int contentLength) throws IOException {

        URL dataURL = null;
        if (secure) {
            dataURL = new URL(HTTPS, host, securePort, url);
        } else {
            dataURL = new URL(HTTP, host, port, url);
        }
         
        URLConnection connection = dataURL.openConnection();

        connection.setUseCaches(false);
        connection.setDoOutput(true);
        if (contentLength > 0) {
            connection.setRequestProperty("Content-Length", "" + contentLength);
        }
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

        return connection;
    }

    /**
     * Return the host name.
     */
    public String getHost() {
        return host;
    }

    /**
     * Set the host name.
     */
    public void setHost(String host) {
        this.host = host;
    }

    /**
     * Return the port.
     */
    public int getPort() {
        return port;
    }

    /**
     * Set the port.
     */
    public void setPort(int httpPort) {
        this.port = httpPort;
    }

    /**
     * Return the port for secure connections.
     */
    public int getSecurePort() {
        return securePort;
    }

    /**
     * Set the port for secure connections.
     */
    public void setSecurePort(int httpsPort) {
        this.securePort = httpsPort;
    }

    /**
     * Return the url used for connections.
     */
    public String getUrl() {
        return url;
    }

    /**
     * Set the url.
     */
    public void setUrl(String url) {
        this.url = url;
    }

    static class NoHostnameVerify implements HostnameVerifier {
        
        public boolean verify(String urlHostName, SSLSession session) {
            String ph = session.getPeerHost();
            if (logger.isLoggable(Level.FINE)){
                logger.fine("HostnameVerifier.verify() " + urlHostName + ":" + ph);            	
            }
            return true;
        }
    }
}
