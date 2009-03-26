package com.avaje.eclipse.buildplugin.builder;

import com.avaje.ebean.enhance.asm.ClassReader;
import com.avaje.ebean.enhance.asm.EmptyVisitor;

/**
 * Utility used to read the bytes to determine the className.
 */
public class DetermineClass  {

	public static String getClassName(byte[] classBytes){
		
		ClassReader cr = new ClassReader(classBytes);
		DetermineClassVisitor cv = new DetermineClassVisitor();
		try {
			cr.accept(cv, ClassReader.SKIP_CODE+ClassReader.SKIP_DEBUG+ClassReader.SKIP_FRAMES);
			
			// should not get to here...
			throw new RuntimeException("Expected DetermineClassVisitor to throw GotClassName?");

		} catch (GotClassName e){
			// used to skip reading the rest of the class bytes...
			return e.getClassName();
		}
	}
	
	private static class DetermineClassVisitor extends EmptyVisitor {
		
		@Override
		public void visit(int version, int access, String name, String signature,
				String superName, String[] interfaces) {
			
			throw new GotClassName(name);
		}
	}

	
	private static class GotClassName extends RuntimeException {
		
		private static final long serialVersionUID = 2869058158272107957L;

		final String className;
		
		public GotClassName(String className) {
			super(className);
			this.className = className.replace('/', '.');
		}
		public String getClassName() {
			return className;
		}
	}
	
}
