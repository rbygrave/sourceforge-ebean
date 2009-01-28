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
package com.avaje.lib.log;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.logging.ErrorManager;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

/**
 * Base object for Handler that writes to a Writer.
 * <p>
 * It handles various features including the changing of the writer/outputstream
 * with automatic flushing/closing of an existing writer/outputstream.
 * </p>
 */
public abstract class BaseWriterHandler extends Handler {

	OutputStream output;

	boolean doneHeader;

	Writer writer;

	public BaseWriterHandler() {
		super();
	}

	/**
	 * Create with a default formatter and OutputStream.
	 */
	public BaseWriterHandler(OutputStream out, Formatter formatter) {
		super();
		// configure();
		setFormatter(formatter);
		setOutputStream(out);
	}

	/**
	 * Write a record.
	 */
	public abstract void publish(LogRecord record);

	/**
	 * Change the output stream.
	 * <P>
	 * If there is a current output stream then the <tt>Formatter</tt>'s tail
	 * string is written and the stream is flushed and closed. Then the output
	 * stream is replaced with the new output stream.
	 * 
	 * @param out
	 *            New output stream. May not be null.
	 * @exception SecurityException
	 *                if a security manager exists and if the caller does not
	 *                have <tt>LoggingPermission("control")</tt>.
	 */
	protected synchronized void setOutputStream(OutputStream out) throws SecurityException {
		if (out == null) {
			throw new NullPointerException();
		}
		flushAndClose();
		output = out;
		doneHeader = false;
		String encoding = getEncoding();
		if (encoding == null) {
			writer = new OutputStreamWriter(output);
		} else {
			try {
				writer = new OutputStreamWriter(output, encoding);
			} catch (UnsupportedEncodingException ex) {
				// This shouldn't happen. The setEncoding method
				// should have validated that the encoding is OK.
				throw new Error("Unexpected exception " + ex);
			}
		}
	}

	/**
	 * Set the character encoding.
	 */
	public synchronized void setEncoding(String encoding) throws SecurityException,
			java.io.UnsupportedEncodingException {

		super.setEncoding(encoding);
		if (output == null) {
			return;
		}
		// Replace the current writer with a writer for the new encoding.
		flush();
		if (encoding == null) {
			writer = new OutputStreamWriter(output);
		} else {
			writer = new OutputStreamWriter(output, encoding);
		}
	}

	/**
	 * Format and publish a <tt>LogRecord</tt>.
	 */
	public synchronized void publishMessage(String msg) {

		try {
			if (!doneHeader) {
				writer.write(getFormatter().getHead(this));
				doneHeader = true;
			}
			if (msg == null) {
				throw new NullPointerException("msg is null?");
			}
			writer.write(msg);
			writer.flush();

		} catch (Exception ex) {
			reportError(null, ex, ErrorManager.WRITE_FAILURE);
		}
	}

	/**
	 * Flush any buffered messages.
	 */
	public synchronized void flush() {
		if (writer != null) {
			try {
				writer.flush();
			} catch (Exception ex) {
				reportError(null, ex, ErrorManager.FLUSH_FAILURE);
			}
		}
	}

	protected synchronized void flushAndClose() throws SecurityException {
		// checkAccess();
		if (writer != null) {
			try {
				if (!doneHeader) {
					writer.write(getFormatter().getHead(this));
					doneHeader = true;
				}
				writer.write(getFormatter().getTail(this));
				writer.flush();
				writer.close();
			} catch (Exception ex) {
				// We don't want to throw an exception here, but we
				// report the exception to any registered ErrorManager.
				reportError(null, ex, ErrorManager.CLOSE_FAILURE);
			}
			writer = null;
			output = null;
		}
	}

	/**
	 * Close the current output stream.
	 * <p>
	 * The <tt>Formatter</tt>'s "tail" string is written to the stream before
	 * it is closed. In addition, if the <tt>Formatter</tt>'s "head" string
	 * has not yet been written to the stream, it will be written before the
	 * "tail" string.
	 * 
	 * @exception SecurityException
	 *                if a security manager exists and if the caller does not
	 *                have LoggingPermission("control").
	 * @exception SecurityException
	 *                if a security manager exists and if the caller does not
	 *                have LoggingPermission("control").
	 */
	public synchronized void close() throws SecurityException {
		flushAndClose();
	}
}
