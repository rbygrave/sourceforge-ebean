package org.avaje.ebean.enhance.agent;

import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.avaje.ebean.enhance.asm.ClassReader;

/**
 * Reads class information as an alternative to using a ClassLoader.
 * <p>
 * Used because if annotation classes are not in the classpath they are silently
 * dropped from the class information. We are especially interested to know if
 * super classes are entities during enhancement.
 * </p>
 */
public class ClassMetaReader {

	private static Logger logger = Logger.getLogger(ClassMetaReader.class.getName());

	Map<String, ClassMeta> cache = new HashMap<String, ClassMeta>();

	final EnhanceContext enhanceContext;

	final URL[] urls;

	public ClassMetaReader(EnhanceContext enhanceContext, URL[] urls) {
		this.urls = urls == null ? new URL[0] : urls;
		this.enhanceContext = enhanceContext;
	}

	public ClassMeta get(boolean readAnnotations, String name, ClassLoader classLoader) throws ClassNotFoundException {
		return getWithCache(readAnnotations, name, classLoader);
	}

	private ClassMeta getWithCache(boolean readAnnotations, String name, ClassLoader classLoader)
			throws ClassNotFoundException {
		
		synchronized (cache) {
			ClassMeta meta = cache.get(name);
			if (meta == null) {
				meta = readFromResource(readAnnotations, name, classLoader);
				if (meta != null) {
					if (meta.isCheckSuperClassForEntity()) {
						ClassMeta superMeta = getWithCache(readAnnotations, meta.getSuperClassName(), classLoader);
						if (superMeta != null && superMeta.isEntity()) {
							meta.setSuperMeta(superMeta);
						}
					}
					cache.put(name, meta);
				}
			}
			return meta;
		}
	}

	private ClassMeta readFromResource(boolean readAnnotations, String className, ClassLoader classLoader)
			throws ClassNotFoundException {

		String resource = className.replace('.', '/') + ".class";

		try {

			final URLClassLoader cl = new URLClassLoader(urls, classLoader);

			// read the class bytes, and define the class
			URL url = cl.getResource(resource);
			if (url == null) {
				throw new ClassNotFoundException(className);
			}

			InputStream is = url.openStream();
			byte[] classBytes = InputStreamTransform.readBytes(is);

			ClassReader cr = new ClassReader(classBytes);
			ClassMetaReaderVisitor ca = new ClassMetaReaderVisitor(readAnnotations, enhanceContext);

			cr.accept(ca, 0);

			return ca.getClassMeta();

		} catch (Exception e) {
			logger.log(Level.SEVERE, "Extra ClassPath URLS: " + Arrays.toString(urls));

			String msg = "Error trying to read the class info for " + resource;
			logger.log(Level.SEVERE, msg, e);
			return null;
		}

	}

}
