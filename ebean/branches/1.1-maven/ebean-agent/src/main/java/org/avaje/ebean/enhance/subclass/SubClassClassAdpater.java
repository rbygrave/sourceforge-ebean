package org.avaje.ebean.enhance.subclass;

import java.util.logging.Logger;

import org.avaje.ebean.enhance.agent.AlreadyEnhancedException;
import org.avaje.ebean.enhance.agent.ClassMeta;
import org.avaje.ebean.enhance.agent.EnhanceConstants;
import org.avaje.ebean.enhance.agent.EnhanceContext;
import org.avaje.ebean.enhance.agent.IndexFieldWeaver;
import org.avaje.ebean.enhance.agent.InterceptField;
import org.avaje.ebean.enhance.agent.MethodEquals;
import org.avaje.ebean.enhance.agent.NoEnhancementRequiredException;
import org.avaje.ebean.enhance.agent.VisitMethodParams;
import org.avaje.ebean.enhance.asm.AnnotationVisitor;
import org.avaje.ebean.enhance.asm.ClassAdapter;
import org.avaje.ebean.enhance.asm.ClassVisitor;
import org.avaje.ebean.enhance.asm.FieldVisitor;
import org.avaje.ebean.enhance.asm.MethodVisitor;
import org.avaje.ebean.enhance.asm.Opcodes;

public class SubClassClassAdpater extends ClassAdapter implements EnhanceConstants {

	static final Logger logger = Logger.getLogger(SubClassClassAdpater.class.getName());
		
	final EnhanceContext enhanceContext;
	
	final ClassLoader classLoader;
	
	final ClassMeta classMeta;
	
	final String subClassSuffix;
	
	boolean firstField = true;

	boolean firstMethod = true;
	
	public SubClassClassAdpater(String subClassSuffix, ClassVisitor cv, ClassLoader classLoader, EnhanceContext context) {
		super(cv);
		this.subClassSuffix = subClassSuffix;
		this.classLoader = classLoader;
		this.enhanceContext = context;
		this.classMeta = context.createClassMeta();
	}
	
	public boolean isLog(int level){
		return classMeta.isLog(level);
	}
	
	public void log(String msg){
		classMeta.log(msg);
	}
	
	/**
	 * Create the class definition replacing the className and super class.
	 */
	public void visit(int version, int access, String name, String signature, String superName,
			String[] interfaces) {

		// Note: interfaces can be an empty array but not null
		int n = 1 + interfaces.length;
		String[] c = new String[n];
		for (int i = 0; i < interfaces.length; i++) {
			c[i] = interfaces[i];
			if (c[i].equals(C_ENTITYBEAN)) {
				throw new AlreadyEnhancedException(name);
			}
		}

		// Add the EntityBean interface
		c[c.length - 1] = C_ENTITYBEAN;
		
		if (!superName.equals("java/lang/Object")){
			
			ClassMeta superMeta = enhanceContext.getSuperMeta(superName, classLoader);
			if (superMeta != null) {
				classMeta.setSuperMeta(superMeta);
				if (classMeta.isLog(2)){
					classMeta.log("entity inheritance "+superMeta.getDescription());
				}
			}
		}
		
		// adjust the superName and name as we
		// are actually creating a subclass of 
		// the class being visited
		superName = name;
		name = name+subClassSuffix;
		
		classMeta.setClassName(name, superName);

		super.visit(version, access, name, signature, superName, c);
	}

	@Override
	public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
		classMeta.addClassAnnotation(desc);
		return super.visitAnnotation(desc, visible);
	}

	/**
	 * The ebeanIntercept field is added once but thats all. Note the other
	 * fields are defined in the superclass.
	 */
	public FieldVisitor visitField(int access, String name, String desc, String signature,
			Object value) {

		if (firstField) {
			if (!classMeta.isEntityEnhancementRequired()) {
				// bit of a rough way to skip the rest of the visiting etc
				// but we now have visited the interfaces and annotations
				throw new NoEnhancementRequiredException();
			} else {
				firstField = false;
			}
		}

		if ((access & Opcodes.ACC_STATIC) != 0) {
			// no interception of static fields
			if (classMeta.isLog(2)){
				classMeta.log("Skip intercepting static field "+name);					
			}
			return null;
		}
		
		// read the field and associated annotations...
		if (classMeta.isLog(5)){
			classMeta.log(" ... reading field:"+name+" desc:"+desc);
		}
		
		return classMeta.createLocalFieldVisitor(name, desc);
	}

	/**
	 * Replace the method code with calls to super. Add the intercept code as
	 * required.
	 */
	public MethodVisitor visitMethod(int access, String name, String desc, String signature,
			String[] exceptions) {

		boolean entityEnhance = classMeta.isEntityEnhancementRequired();
		
		if (entityEnhance && firstMethod){
			// always add these fields for subclass generation
			InterceptField.addField(cv);
			MethodEquals.addIdentityField(cv);
			firstMethod = false;
		}

		if (entityEnhance){

	        VisitMethodParams params = new VisitMethodParams(cv, access, name, desc, signature, exceptions);

			if (isDefaultConstructor(access, name, desc, signature, exceptions)){
				SubClassConstructor.add(params, classMeta);
				return null;
			}
			
			if (isSpecialMethod(access, name, desc)) {
				return null;
			}
			
			// register the method so that we can check
			// if it exists when GetterSetterMethods.add()
			// is called. May not exist on read only type
			// entity beans such as the internal meta beans.
			classMeta.addExistingSuperMethod(name, desc);
		}
		
		return null;
	}

	/**
	 * Add methods to get and set the entityBeanIntercept. Also add the
	 * writeReplace method to control serialisation.
	 */
	public void visitEnd() {

		if (!classMeta.isEntityEnhancementRequired()){
			throw new NoEnhancementRequiredException();
		}
		
		// Add the _ebean_getIntercept() _ebean_setIntercept() methods
		InterceptField.addGetterSetter(cv, classMeta.getClassName());

		// Add getter and setter methods for both local
		// and inherited properties
		GetterSetterMethods.add(cv, classMeta);

		// Add extra methods such as getField(index) etc
		IndexFieldWeaver.addMethods(cv, classMeta);
				
		// add a writeReplace method to control serialisation
		MethodWriteReplace.add(cv, classMeta);
		
		// register with the context
		enhanceContext.addClassMeta(classMeta);
		
		super.visitEnd();
	}

	/**
	 * Return true if this is the default (no arg) constructor.
	 */
	private boolean isDefaultConstructor(int access, String name, String desc, String signature,
			String[] exceptions){

		if (name.equals("<init>") && desc.equals("()V")) {
			classMeta.setHasDefaultConstructor(true);
			return true;
		}
		
		return false;
	}
	
	/**
	 * Take note of hashcode and equals.
	 */
	private boolean isSpecialMethod(int access, String name, String desc) {
		
		if (name.equals("hashCode") && desc.equals("()I")) {
			classMeta.setHasEqualsOrHashcode(true);
			return true;
		}

		if (name.equals("equals") && desc.equals("(Ljava/lang/Object;)Z")) {
			classMeta.setHasEqualsOrHashcode(true);
			return true;
		}
		
		return false;
	}
	
	
}
