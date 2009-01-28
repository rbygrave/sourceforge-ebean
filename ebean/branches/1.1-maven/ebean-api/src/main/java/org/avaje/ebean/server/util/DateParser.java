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
package org.avaje.ebean.server.util;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;

/**
 * Helper for parsing Strings to dates and timestamps.
 * Uses the JDBC Date and Timestamp formats.
 */
public class DateParser implements Serializable {

	private static final long serialVersionUID = -6897534819654972183L;

	/**
	 * For use with hypen separators.
	 */
	private SimpleDateFormat[] hypenFormatterList;
    
	/**
	 * For use with slash separators.
	 */
	private SimpleDateFormat[] slashFormatterList;
    
	/**
	 * Create the DateParser.
	 */
	public DateParser(){
        initHypenList();
        initSlashList();
	}

	/**
     * Parse and return as a Timestamp.
     * Uses jdbc Date and Timestamp formats.
	 */
	public java.sql.Timestamp parseTimestamp(String formatedDate){
		java.util.Date d = parse(formatedDate);
		if (d == null){
			throw new RuntimeException("Could not parse timestamp ["+formatedDate+"]");

		} else {
			return new java.sql.Timestamp(d.getTime());
		}
	}
	
	/**
     * Parse and return as a sql Date.
     * Uses jdbc Date and Timestamp formats.
	 */
	public java.sql.Date parseDate(String formatedDate){
		java.util.Date d = parse(formatedDate);
		if (d == null){
			throw new RuntimeException("Could not parse date ["+formatedDate+"]");
			
		} else {
			return new java.sql.Date(d.getTime());
		}
	}
	
	/**
     * Parse and return as a util Date.
     * Uses jdbc Date and Timestamp formats.
	 */
	public java.util.Date parse(String formatedDate){
		if (formatedDate == null){
		    return null;
        }
        int length = formatedDate.length();

        // assume its a short date format
        int formatType = 0;
        if (length > 19){
            // its long format with millis
            formatType = 2;
        } else if (length > 10){
            // its long format but no millis
            formatType = 1;
        }
        
        boolean isHypen = formatedDate.indexOf('-')>-1;
        
        SimpleDateFormat formatter = null;
        if (isHypen){
            formatter = hypenFormatterList[formatType];
        } else {
            // if not hypen assuming a slash is used
            formatter = slashFormatterList[formatType];
        }
        try {
        	synchronized (formatter) {
                return formatter.parse(formatedDate);				
			}
        } catch (ParseException ex){
            throw new RuntimeException("Format of date ["+formatedDate+"] errored", ex);
        }
	}

    private void initHypenList() {
        hypenFormatterList = new SimpleDateFormat[3];
        hypenFormatterList[0] = new SimpleDateFormat("yyyy-MM-dd");
        hypenFormatterList[1] = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        hypenFormatterList[2] = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S");
    }
    
    private void initSlashList() {
        slashFormatterList = new SimpleDateFormat[3];
        slashFormatterList[0] = new SimpleDateFormat("yyyy/MM/dd");
        slashFormatterList[1] = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        slashFormatterList[2] = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.S");
    }
    

			
}
