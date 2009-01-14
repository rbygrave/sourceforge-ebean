package com.avaje.ebean.enhance.agent;

import java.util.logging.Logger;

import com.avaje.ebean.enhance.asm.AnnotationVisitor;
import com.avaje.ebean.enhance.asm.EmptyVisitor;
import com.avaje.ebean.enhance.asm.FieldVisitor;
import com.avaje.ebean.enhance.asm.MethodVisitor;
import com.avaje.ebean.enhance.asm.Opcodes;

/**
 * Used by ClassMetaReader to read information about a class.
 * <p>
 * Reads the information by visiting the byte codes rather than using
 * ClassLoader. This gets around issues where the annotations are not seen
 * (silently ignored) if they are not in the class path.
 * </p>
 */
public class ClassMetaReaderVisitor extends EmptyVisitor implements EnhanceConstants {

	static final Logger logger = Logger.getLogger(ClassMetaReaderVisitor.class.getName());

	final EnhanceContext enhanceContext;

	final ClassMeta classMeta;

	final boolean readMethodMeta;
	
	boolean implementsEntityBean;

	public ClassMetaReaderVisitor(boolean readMethodMeta, EnhanceContext context) {
		super();
		this.readMethodMeta = readMethodMeta;
		this.enhanceContext = context;
		this.classMeta = context.createClassMeta();
	}

	public ClassMeta getClassMeta() {
		return classMeta;
	}

	public boolean isLog(int level) {
		return classMeta.isLog(level);
	}

	public void log(String msg) {
		classMeta.log(msg);
	}

	/**
	 * Create the class definition replacing the className and super class.
	 */
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {

		// Note: interfaces can be an empty array but not null
		for (int i = 0; i < interfaces.length; i++) {
			if (interfaces[i].equals(C_ENTITYBEAN)) {
				implementsEntityBean = true;
			}
		}

		classMeta.setClassName(name, superName);
		super.visit(version, access, name, signature, superName, interfaces);
	}

	@Override
	public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
		
		classMeta.addClassAnnotation(desc);
		
		AnnotationVisitor av = super.visitAnnotation(desc, visible);
		
		if (desc.equals(EnhanceConstants.AVAJE_TRANSACTIONAL_ANNOTATION)) {
			// we have class level Transactional annotation
			// which will act as default for all methods in this class
			return new AnnotationInfoVisitor(null, classMeta.classAnnotationInfo, av);

		} else {
			return av;
		}
	}

	/**
	 * The ebeanIntercept field is added once but thats all. Note the other
	 * fields are defined in the superclass.
	 */
	public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {

		if ((access & Opcodes.ACC_STATIC) != 0) {
			// no interception of static fields
			if (isLog(2)) {
				log("Skip static field " + name);
			}
			return super.visitField(access, name, desc, signature, value);
		}

		return classMeta.createLocalFieldVisitor(name, desc);
	}

	/**
	 * Not interested.
	 */
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {

		boolean staticAccess = ((access & Opcodes.ACC_STATIC) != 0);
		
		MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
		if (!staticAccess && readMethodMeta){
			return classMeta.createMethodVisitor(mv, access, name, desc);
			
		} else {
			// not interested in the methods...
			return mv;
		}
	}

}
