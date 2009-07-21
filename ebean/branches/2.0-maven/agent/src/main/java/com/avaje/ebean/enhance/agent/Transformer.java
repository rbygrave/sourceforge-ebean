package com.avaje.ebean.enhance.agent;

import java.io.File;
import java.io.PrintStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.ProtectionDomain;
import java.util.ArrayList;

import com.avaje.ebean.enhance.asm.ClassReader;
import com.avaje.ebean.enhance.asm.ClassWriter;

/**
 * A Class file Transformer that enhances entity beans.
 * <p>
 * This is used as both a javaagent or via an ANT task (or other off line approach). 
 * </p>
 */
public class Transformer implements ClassFileTransformer {

	public static void premain(String agentArgs, Instrumentation inst) {

		Transformer t = new Transformer("", agentArgs);
		inst.addTransformer(t);

		if (t.getLogLevel() > 0) {
			System.out.println("premain loading Transformer args:" + agentArgs);
		}
	}

	private static final int CLASS_WRITER_COMPUTEFLAGS = ClassWriter.COMPUTE_FRAMES + ClassWriter.COMPUTE_MAXS;

	
	private final EnhanceContext enhanceContext;

	private boolean performDetect;
	private boolean transformTransactional;
	private boolean transformEntityBeans;
	
	public Transformer(String extraClassPath, String agentArgs) {
		this(parseClassPaths(extraClassPath), agentArgs);
	}
	
	public Transformer(URL[] extraClassPath, String agentArgs) {
		this.enhanceContext = new EnhanceContext(extraClassPath, agentArgs);
		this.performDetect = enhanceContext.getPropertyBoolean("detect", true);
		this.transformTransactional = enhanceContext.getPropertyBoolean("transactional", true);
		this.transformEntityBeans = enhanceContext.getPropertyBoolean("entity", true);
	}
	
	/**
	 * Change the logout to something other than system out.
	 */
	public void setLogout(PrintStream logout){
		this.enhanceContext.setLogout(logout);
	}
	
	public void log(int level, String msg){
		enhanceContext.log(level, msg);
	}
	
	public int getLogLevel() {
		return enhanceContext.getLogLevel();
	}

	
	public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
			ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {

		try {

			// ignore JDK and JDBC classes etc  
			if (enhanceContext.isIgnoreClass(className)) {
				return null;
			}

			ClassAdapterDetectEnhancement detect = null;
			
			if (performDetect){
				enhanceContext.log(5, "performing detection on "+className);
				detect = detect(loader, classfileBuffer);
			}
			
			if (detect == null){
				// default only looks entity beans to enhance
				enhanceContext.log(1, "no detection so enhancing entity "+className);
				return entityEnhancement(loader, classfileBuffer);
			} 
			
			if (transformEntityBeans && detect.isEntity()){
				
				if (detect.isEnhancedEntity()){
					detect.log(1, "already enhanced entity");
					
				} else {
					// 
					detect.log(2, "performing entity transform");
					return entityEnhancement(loader, classfileBuffer);
				}
			}
			
			if (transformTransactional && detect.isTransactional()){
				
				if (detect.isEnhancedTransactional()){
					detect.log(1, "already enhanced transactional");

				} else {
					detect.log(2, "performing transactional transform");
					return transactionalEnhancement(loader, classfileBuffer);
				}
			}
				
			return null;

		} catch (NoEnhancementRequiredException e){
			// the class is an interface
			log(8, "No Enhancement required "+e.getMessage());
			return null;
			
		} catch (Exception e) {
			// a safety net for unexpected errors
			// in the transformation
			enhanceContext.log(e);
			return null;
		}
	}

	/**
	 * Perform entity bean enhancement.
	 */
	private byte[] entityEnhancement(ClassLoader loader, byte[] classfileBuffer) {
				
		ClassReader cr = new ClassReader(classfileBuffer);
		ClassWriter cw = new ClassWriter(CLASS_WRITER_COMPUTEFLAGS);
		ClassAdpaterEntity ca = new ClassAdpaterEntity(cw, loader, enhanceContext);
		try {
			
			cr.accept(ca, 0);
			
			if (ca.isLog(1)) {
				ca.logEnhanced();
			}
			
			if (enhanceContext.isReadOnly()){
				return null;
				
			} else {
				return cw.toByteArray();
			}
			
		} catch (AlreadyEnhancedException e) {
			if (ca.isLog(1)) {
				ca.log("already enhanced entity");
			}
			return null;
			
		} catch (NoEnhancementRequiredException e) {
			if (ca.isLog(2)) {
				ca.log("skipping... no enhancement required");
			}
			return null;
		}
	}
	
	
	/**
	 * Perform transactional enhancement.
	 */
	private byte[] transactionalEnhancement(ClassLoader loader, byte[] classfileBuffer) {
		
		ClassReader cr = new ClassReader(classfileBuffer);
		ClassWriter cw = new ClassWriter(CLASS_WRITER_COMPUTEFLAGS);
		ClassAdapterTransactional ca = new ClassAdapterTransactional(cw, loader, enhanceContext);
		
		try {
			
			cr.accept(ca, 0);
			
			if (ca.isLog(1)) {
				ca.log("enhanced");
			}
			
			if (enhanceContext.isReadOnly()){
				return null;
				
			} else {
				return cw.toByteArray();
			}
			
		} catch (AlreadyEnhancedException e) {
			if (ca.isLog(1)) {
				ca.log("already enhanced");
			}
			return null;
			
		} catch (NoEnhancementRequiredException e) {
			if (ca.isLog(0)) {
				ca.log("skipping... no enhancement required");
			}
			return null;
		}
	}
	
	/**
	 * Helper method to split semi-colon separated class paths into a URL array.
	 */
	public static URL[] parseClassPaths(String extraClassPath){

		try {

			ArrayList<URL> list = new ArrayList<URL>();

			if (extraClassPath == null || extraClassPath.trim().length()==0){
				// its an empty list...
				
			} else {
				// split path using ;
				String[] stringPaths = extraClassPath.split(";");
				for (int i = 0; i < stringPaths.length; i++) {
					File f = new File(stringPaths[i].trim());
					URL u = f.toURL();
					list.add(u);
				}
			}
			
			return list.toArray(new URL[list.size()]);
			
			
		} catch (MalformedURLException e){
			throw new RuntimeException(e);
		}
	}


	/**
	 * Read the bytes quickly trying to detect if it needs entity or
	 * transactional enhancement.
	 */
	private ClassAdapterDetectEnhancement detect(ClassLoader classLoader, byte[] classfileBuffer) {
				
		ClassAdapterDetectEnhancement detect = new ClassAdapterDetectEnhancement(classLoader, enhanceContext);
	
		// skip what we can...
		ClassReader cr = new ClassReader(classfileBuffer);
		cr.accept(detect, ClassReader.SKIP_CODE+ClassReader.SKIP_DEBUG+ClassReader.SKIP_FRAMES);
		
		return detect;
	}
}
