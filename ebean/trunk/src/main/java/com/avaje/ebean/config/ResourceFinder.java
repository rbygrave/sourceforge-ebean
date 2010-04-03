package com.avaje.ebean.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContext;

/**
 * Helper used to find a resource that is either a file or a classpath element.
 * The protocol is:
 * <ol>
 * <li>If the resource is qualified as a file (examples include file:f,
 * file://f), the class will only look in the file space.</li>
 * <li>If the resource is qualified as a classpath element (examples include
 * classpath:x, classpath://x), the class will only look in the classpath space.
 * </li>
 * <li>If a servletContext is provided, the class will check under the WEB-INF
 * subdirectory of the servlet context
 * <li>If the resource is unqualified, the class will check in the file space
 * first and then check the classpath space if not found in the file space.
 * </ol>
 */
public final class ResourceFinder {

	private static final String QUALIFIER4FILE = "file:";
	private static final String QUALIFIER4RESOURCE = "classpath:";
	private static Logger logger = Logger.getLogger(PropertyMapLoader.class.getName());
	private String mLabel;
	private ResourceType mType;
	private InputStream mStream;
	private boolean mIsFound;

	public enum ResourceType {
		FILE(QUALIFIER4FILE), CLASSPATH(QUALIFIER4RESOURCE);
		private String mQualifier;

		ResourceType(String qualifier) {
			mQualifier = qualifier;
		}

		public String getResourceLabel(String path) {
			return mQualifier + path;
		}
	};

	public ResourceFinder(String fileName, ServletContext servletContext) {
		mLabel=null;
		mType=null;
		mStream=null;
		mIsFound=false;
		if (fileName.startsWith(QUALIFIER4FILE)) {
			fileName = fileName.substring(QUALIFIER4FILE.length());
			if (fileName.startsWith("//")) {
				fileName = fileName.substring(2);
			}
			findFile(fileName, null);
		} else if (fileName.startsWith(QUALIFIER4RESOURCE)) {
			fileName = fileName.substring(QUALIFIER4RESOURCE.length());
			if (fileName.startsWith("//")) {
				fileName = fileName.substring(2);
			}
			findInClassPath(fileName);
		} else {
			findFile(fileName, servletContext);
			if (!mIsFound) {
				findInClassPath(fileName);
			}
		}
		if (!mIsFound) {
			logger.warning("Could not find " + fileName + " in File System, Classpath"
					+ (servletContext != null ? ", or servlet path" : ""));
		} else if (logger.isLoggable(Level.FINE)) {
			logger.fine("find " + fileName + " space=" + mType + ", location=" + getSourcePath());
		}
	}

	private void findFile(String fileName, ServletContext servletContext) {
		if (servletContext != null) {
			mStream = servletContext.getResourceAsStream("/WEB-INF/" + fileName);
			if (mStream != null) {
				logger.fine(fileName + " found in WEB-INF");
				mType = ResourceType.FILE;
				mLabel = servletContext.getRealPath("/WEB-INF/" + fileName);
				mIsFound = true;
				return;
			}
		}
		File f = new File(fileName);

		if (f.exists()) {
			logger.fine(fileName + " found in file system");
			try {
				mStream = new FileInputStream(f);
			} catch (FileNotFoundException ex) {
				// already made the check so this
				// should never be thrown
				throw new RuntimeException(ex);
			}
			mType = ResourceType.FILE;
			try {
				mLabel = f.getCanonicalPath();
			} catch (IOException e) {
				logger.log(Level.WARNING, "File=" + f, e);
				mLabel = f.getAbsolutePath();
			}
			mIsFound = true;
		}
	}

	private void findInClassPath(String fileName) {
		mStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(fileName);
		if (mStream != null) {
			if(logger.isLoggable(Level.FINE)) {
				logger.fine(fileName + " found in "+Thread.currentThread().getContextClassLoader().getResource(fileName).toExternalForm());
			}
			mType = ResourceType.CLASSPATH;
			mLabel = fileName;
			mIsFound = true;
			return;
		}
	}

	public InputStream getInputSteram() {
		return mStream;
	}

	public boolean exists() {
		return mIsFound;
	}

	public String getSourcePath() {
		return mType!=null?mType.getResourceLabel(mLabel):"***";
	}

}
