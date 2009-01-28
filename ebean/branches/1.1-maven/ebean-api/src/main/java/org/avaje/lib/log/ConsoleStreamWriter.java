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

import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.logging.ErrorManager;
import java.util.logging.Handler;

/**
 * Helper used by ConsoleHandler to typically handle either System.out or
 * System.err.
 */
public class ConsoleStreamWriter {

	/**
	 * The stream written to.
	 */
	final OutputStreamWriter writer;

	/**
	 * The handler that owns this ConsoleStreamWriter.
	 */
	final Handler handler;

	boolean doneHeader;

	/**
	 * Create a ConsoleStreamWriter
	 * 
	 * @param handler
	 *            the handler that owns this ConsoleStreamWriter
	 * @param stream
	 *            the stream that will be written to
	 * @param encoding
	 *            the encoding of the output
	 * @throws SecurityException
	 */
	public ConsoleStreamWriter(Handler handler, PrintStream stream, String encoding)
			throws SecurityException {

		this.handler = handler;
		
		if (encoding == null) {
			writer = new OutputStreamWriter(stream);
		} else {
			try {
				writer = new OutputStreamWriter(stream, encoding);

			} catch (UnsupportedEncodingException ex) {
				throw new Error("Unexpected exception ", ex);
			}
		}
	}

	/**
	 * Write a message to the stream.
	 */
	public synchronized void write(String msg) {
		try {
			if (!doneHeader) {
				String head = handler.getFormatter().getHead(handler);
				writer.write(head);
				doneHeader = true;
			}
			writer.write(msg);
			writer.flush();

		} catch (Exception ex) {
			error(null, ex, ErrorManager.WRITE_FAILURE);
		}
	}

	/**
	 * Flush any buffered messages.
	 */
	public synchronized void flush() {
		try {
			writer.flush();
		} catch (Exception ex) {
			error(null, ex, ErrorManager.FLUSH_FAILURE);
		}
	}

	/**
	 * Close the current output stream.
	 */
	public synchronized void close() throws SecurityException {
		try {
			writer.flush();
			writer.close();
		} catch (Exception ex) {
			error(null, ex, ErrorManager.FLUSH_FAILURE);
		}
	}

	/**
	 * Report an internal error.
	 */
	protected void error(String msg, Exception ex, int code) {
		ErrorManager errMgr = handler.getErrorManager();
		if (errMgr != null) {
			errMgr.error(msg, ex, code);
		} else {
			ex.printStackTrace();
		}
	}
}
