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
package com.avaje.ebean.server.text.csv;

import java.io.IOException;
import java.io.Reader;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import com.avaje.ebean.EbeanServer;
import com.avaje.ebean.Transaction;
import com.avaje.ebean.bean.EntityBean;
import com.avaje.ebean.server.deploy.BeanDescriptor;
import com.avaje.ebean.server.el.ElPropertyValue;
import com.avaje.ebean.text.StringParser;
import com.avaje.ebean.text.TextException;
import com.avaje.ebean.text.csv.CsvCallback;
import com.avaje.ebean.text.csv.CsvReader;

/**
 * 
 * @author rbygrave
 */
public class TCsvReader<T> implements CsvReader<T> {

    private static final Logger logger = Logger.getLogger(TCsvReader.class.getName());
    
    private final EbeanServer server;

    private final BeanDescriptor<T> descriptor;

    private final List<CsvColumn> columnList = new ArrayList<CsvColumn>();

    private final CsvColumn ignoreColumn = new CsvColumn();

    private boolean treatEmptyStringAsNull = true;

    private boolean ignoreHeader;
    
    private int logInfoFrequency = 0;
    
    /**
     * The transaction to use (if not using CsvCallback).
     */
    protected Transaction transaction;
    
    /**
     * Flag set when we created the transaction.
     */
    protected boolean createdTransaction;

    /**
     * The batch size used for JDBC statement batching.
     */
    protected int persistBatchSize = 20;
    
    
    
    public TCsvReader(EbeanServer server, BeanDescriptor<T> descriptor) {
        this.server = server;
        this.descriptor = descriptor;
    }

    public void setPersistBatchSize(int persistBatchSize) {
        this.persistBatchSize = persistBatchSize;
    }

    public void setIgnoreHeader(boolean ignoreHeader) {
        this.ignoreHeader = ignoreHeader;
    }
    
    

    public void setLogInfoFrequency(int logInfoFrequency) {
        this.logInfoFrequency = logInfoFrequency;
    }

    public void addIgnore() {
        columnList.add(ignoreColumn);
    }

    public void addProperty(String propertyName) {
        addProperty(propertyName, null);
    }

    public void addReference(String propertyName) {
        addProperty(propertyName, null, true);
    }

    public void addProperty(String propertyName, StringParser parser) {
        addProperty(propertyName, parser, false);
    }

    public void addDateTime(String propertyName, String dateTimeFormat) {

        ElPropertyValue elProp = descriptor.getElGetValue(propertyName);
        if (!elProp.isDateTimeCapable()) {
            throw new TextException("Property " + propertyName + " is not DateTime capable");
        }

        SimpleDateFormat sdf = new SimpleDateFormat(dateTimeFormat);
        DateTimeParser parser = new DateTimeParser(sdf, dateTimeFormat, elProp);

        CsvColumn column = new CsvColumn(elProp, parser, false);
        columnList.add(column);
    }

    public void addProperty(String propertyName, StringParser parser, boolean reference) {

        ElPropertyValue elProp = descriptor.getElGetValue(propertyName);
        if (parser == null) {
            parser = elProp.getStringParser();
        }
        CsvColumn column = new CsvColumn(elProp, parser, reference);
        columnList.add(column);
    }

    public void process(Reader reader) throws IOException {
        process(reader, null);
    }
    
    public void process(Reader reader, CsvCallback<T> callback) throws IOException {

        long startTime = System.currentTimeMillis();
        
        CsvUtilReader utilReader = new CsvUtilReader(reader);

        if (callback == null){
            initTransactionIfRequired();
        }
        int row = 0;
        
        if (ignoreHeader){
            String[] line = utilReader.readNext();
            logger.fine("... ignoring header "+Arrays.toString(line));
        }
        
        try {
            do {
                ++row;
                String[] line = utilReader.readNext();
                if (line == null) {
                    break;
    
                } else if (line.length != columnList.size()) {
                    // we have not got the expected number of columns
                    String msg = "Error at line " + row + ". Expected [" + columnList.size() + "] columns "
                            + "but instead we have [" + line.length + "].  Line[" + Arrays.toString(line) + "]";
                    throw new TextException(msg);
    
                } else {
                    T bean = processLine(line);
                    if (callback != null) {
                        callback.processBean(row, bean, line);
                    } else {
                        server.save(bean, transaction);
                    }
                    if (logInfoFrequency > 0 && (row % logInfoFrequency == 0)){
                        logger.info("processed "+row+" rows");
                    }
                    
                }
            } while (true);
            
            commitTransactionIfCreated();
            
            long exeTime = System.currentTimeMillis() - startTime;
            
            logger.info("CsvReader finished, rows["+(row-1)+"] exeMillis["+exeTime+"]");
            
        } finally {
            endTransactionIfCreated();
        }
        
        
    }
        
    /**
     * Create a transaction if one is not already active and set its batch mode and batch size.
     */
    protected void initTransactionIfRequired() {
        
        transaction = server.currentTransaction();
        if (transaction == null || !transaction.isActive()){
            
            transaction = server.beginTransaction();
            createdTransaction = true;
            if (persistBatchSize > 1){
                logger.info("CsvReader creating transaction, batchSize["+persistBatchSize+"]");
                transaction.setBatchMode(true);
                transaction.setBatchSize(persistBatchSize);
                
            } else {
                // explicitly turn off JDBC batching in case
                // is has been turned on globally
                transaction.setBatchMode(false);
                logger.info("CsvReader creating transaction for CsvReader with no JDBC batching");
            }
        }
    }
    
    /**
     * If we created a transaction commit it.
     * We have successfully processed all the rows.
     */
    protected void commitTransactionIfCreated() {
        if (createdTransaction){
            transaction.commit();
            logger.info("CsvReader Committed transaction");
        }
    }

    /**
     * Rollback the transaction if we where not successful in processing all the rows.
     */
    protected void endTransactionIfCreated() {
        if (createdTransaction){
            transaction.end();
        }
    }
    
    @SuppressWarnings("unchecked")
    protected T processLine(String[] line) {

        EntityBean entityBean = descriptor.createEntityBean();
        T bean = (T) entityBean;

        int columnPos = 0;
        for (; columnPos < line.length; columnPos++) {
            convertAndSetColumn(columnPos, line[columnPos], entityBean);
        }

        return bean;
    }

    protected void convertAndSetColumn(int columnPos, String strValue, Object bean) {

        if (strValue.length() == 0 && treatEmptyStringAsNull) {
            return;
        }

        CsvColumn c = columnList.get(columnPos);
        c.convertAndSet(strValue, bean);
    }

    /**
     * Processes a column in the csv content.
     */
    public static class CsvColumn {

        private final ElPropertyValue elProp;
        private final StringParser parser;
        private final boolean ignore;
        private final boolean reference;

        /**
         * Constructor for the IGNORE column.
         */
        private CsvColumn() {
            this.elProp = null;
            this.parser = null;
            this.reference = false;
            this.ignore = true;
        }

        /**
         * Construct with a property and parser.
         */
        public CsvColumn(ElPropertyValue elProp, StringParser parser, boolean reference) {
            this.elProp = elProp;
            this.parser = parser;
            this.reference = reference;
            this.ignore = false;
        }

        /**
         * Convert the string to the appropriate value and set it to the bean.
         */
        public void convertAndSet(String strValue, Object bean) {

            if (!ignore) {
                Object value = parser.parse(strValue);
                elProp.elSetValue(bean, value, true, reference);
            }
        }
    }

    /**
     * A StringParser for converting custom date/time/datetime strings into
     * appropriate java types (Date, Calendar, SQL Date, Time, Timestamp, JODA
     * etc).
     */
    private static class DateTimeParser implements StringParser {

        private final DateFormat dateFormat;
        private final ElPropertyValue elProp;
        private final String format;

        DateTimeParser(DateFormat dateFormat, String format, ElPropertyValue elProp) {
            this.dateFormat = dateFormat;
            this.elProp = elProp;
            this.format = format;
        }

        public Object parse(String value) {
            try {
                Date dt = dateFormat.parse(value);
                return elProp.parseDateTime(dt.getTime());

            } catch (ParseException e) {
                throw new TextException("Error parsing [" + value + "] using format[" + format + "]", e);
            }
        }

    }
}
