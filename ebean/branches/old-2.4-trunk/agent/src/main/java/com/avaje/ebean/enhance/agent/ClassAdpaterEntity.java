package com.avaje.ebean.enhance.agent;

import com.avaje.ebean.enhance.asm.AnnotationVisitor;
import com.avaje.ebean.enhance.asm.ClassAdapter;
import com.avaje.ebean.enhance.asm.ClassVisitor;
import com.avaje.ebean.enhance.asm.FieldVisitor;
import com.avaje.ebean.enhance.asm.MethodVisitor;
import com.avaje.ebean.enhance.asm.Opcodes;

/**
 * ClassAdapter for enhancing entities.
 * <p>
 * Used for javaagent or ant etc to modify the class with field interception.
 * </p>
 * <p>
 * This is NOT used for subclass generation.
 * </p>
 */
public class ClassAdpaterEntity extends ClassAdapter implements EnhanceConstants {
		
	private final EnhanceContext enhanceContext;
	
	private final ClassLoader classLoader;
	
	private final ClassMeta classMeta;

	private boolean firstMethod = true;

	public ClassAdpaterEntity(ClassVisitor cv, ClassLoader classLoader, EnhanceContext context) {
		super(cv);
		this.classLoader = classLoader;
		this.enhanceContext = context;
		this.classMeta = context.createClassMeta();
	}

	/**
	 * Log that the class has been enhanced.
	 */
	public void logEnhanced() {
		classMeta.logEnhanced();
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

		classMeta.setClassName(name, superName);

		int n = 1 + interfaces.length;
		String[] c = new String[n];
		for (int i = 0; i < interfaces.length; i++) {
			c[i] = interfaces[i];
			if (c[i].equals(C_ENTITYBEAN)) {
				classMeta.setEntityBeanInterface(true);
			}
			if (c[i].equals(C_SCALAOBJECT)) {
				classMeta.setScalaInterface(true);
			}
			if (c[i].equals(C_GROOVYOBJECT)) {
				classMeta.setGroovyInterface(true);
			}
		}

		if (classMeta.hasEntityBeanInterface()){
			// Just use the original interfaces
			c = interfaces;
		} else {
			// Add the EntityBean interface
			c[c.length - 1] = C_ENTITYBEAN;
		}
		
		if (!superName.equals("java/lang/Object")){
			// read information about superClasses... 
			if (classMeta.isLog(7)){
				classMeta.log("read information about superClasses "+superName
					+" to see if it is entity/embedded/mappedSuperclass");
			}
			ClassMeta superMeta = enhanceContext.getSuperMeta(superName, classLoader);
			if (superMeta != null && superMeta.isEntity()){
				// the superClass is an entity/embedded/mappedSuperclass...
				classMeta.setSuperMeta(superMeta);
				if (classMeta.isLog(1)){
					classMeta.log("entity extends "+superMeta.getDescription());
				}
			} else {
				if (classMeta.isLog(7)){
					if (superMeta == null){
						classMeta.log("unable to read superMeta for "+superName);
					} else {
						classMeta.log("superMeta "+superName+" is not an entity/embedded/mappedsuperclass "+superMeta.getClassAnnotations());
					}
				}	
			}
		}

		super.visit(version, access, name, signature, superName, c);
	}

	@Override
	public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
		classMeta.addClassAnnotation(desc);
		return super.visitAnnotation(desc, visible);
	}

	/**
	 * Return true if this is the enhancement marker field.
	 * <p>
	 * The existence of this field is used to confirm that the class has been
	 * enhanced (rather than solely relying on the EntityBean interface). 
	 * <p>
	 */
	private boolean isEbeanFieldMarker(String name, String desc, String signature) {
		
		if (name.equals(MarkerField._EBEAN_MARKER)){
			if (!desc.equals("Ljava/lang/String;")){
				String m = "Error: _EBEAN_MARKER field of wrong type? "+desc;
				classMeta.log(m);
			}
			return true;
		}
		return false;
	}

	private boolean isPropertyChangeListenerField(String name, String desc, String signature) {
		if (desc.equals("Ljava/beans/PropertyChangeSupport;")){
			return true;
		}
		return false;
	}
	
	/**
	 * The ebeanIntercept field is added once but thats all. Note the other
	 * fields are defined in the superclass.
	 */
	public FieldVisitor visitField(int access, String name, String desc, String signature,
			Object value) {

		if ((access & Opcodes.ACC_STATIC) != 0) {
			// static field...
			if (isEbeanFieldMarker(name, desc, signature)){
				classMeta.setAlreadyEnhanced(true);
				if (isLog(2)){
					log("Found ebean marker field "+name+" "+value);					
				}				
			} else {
				if (isLog(2)){
					log("Skip intercepting static field "+name);					
				}
			}

			// no interception of static fields
			return super.visitField(access, name, desc, signature, value);
		}
		
		if (isPropertyChangeListenerField(name, desc, signature)) {
			//classMeta.setExistingPropertyChangeSupport(name);
			if (isLog(1)){
				classMeta.log("Found existing PropertyChangeSupport field "+name);
			}
			// no interception on PropertyChangeSupport field
			return super.visitField(access, name, desc, signature, value);
		}
		
		if ((access & Opcodes.ACC_TRANSIENT) != 0) {
			if (isLog(2)){
				log("Skip intercepting transient field "+name);					
			}			
			// no interception of transient fields
			return super.visitField(access, name, desc, signature, value);
		}
		
		// this will hide the field on the super object...
		// but being in a different ClassLoader means we don't
		// get access to those 'real' private fields
		FieldVisitor fv = super.visitField(access, name, desc, signature, value);
		
		return classMeta.createLocalFieldVisitor(cv, fv, name, desc);
	}

	/**
	 * Replace the method code with field interception.
	 */
	public MethodVisitor visitMethod(int access, String name, String desc, String signature,
			String[] exceptions) {
		
		if (firstMethod){
			if (!classMeta.isEntityEnhancementRequired()) {
				// skip the rest of the visiting etc
				throw new NoEnhancementRequiredException();
			}
			
			if (classMeta.hasEntityBeanInterface()){
				log("Enhancing when EntityBean interface already exists!");
			}
			
			// always add the marker field on every enhanced class
			String marker = MarkerField.addField(cv, classMeta.getClassName());
			if (isLog(4)){
				log("... add marker field \""+marker+"\"");					
			}
			
			if (!classMeta.isSuperClassEntity()){
				// only add the intercept and identity fields if 
				// the superClass is not also enhanced
				if (isLog(4)){
					log("... add intercept and identity fields");					
				}
				InterceptField.addField(cv);
				MethodEquals.addIdentityField(cv);
				
			}
			firstMethod = false;
		}


		classMeta.addExistingMethod(name, desc);
		
		if (isDefaultConstructor(name, desc)){
		    // make sure the access is public
	        MethodVisitor mv =  super.visitMethod(Opcodes.ACC_PUBLIC, name, desc, signature, exceptions);
			// also create the entityBeanIntercept object
			return new ConstructorAdapter(mv, classMeta, desc);
		}

	    MethodVisitor mv =  super.visitMethod(access, name, desc, signature, exceptions);

		if (interceptEntityMethod(access, name, desc, signature, exceptions)) {
			// change the method replacing the relevant GETFIELD PUTFIELD with
			// our special field methods with interception... 
			return new MethodFieldAdapter(mv, classMeta, name+" "+desc);				
		}
		
		// just leave as is, no interception etc
		return mv;
	}

	/**
	 * Add methods to get and set the entityBeanIntercept. Also add the
	 * writeReplace method to control serialisation.
	 */
	public void visitEnd() {

		if (!classMeta.isEntityEnhancementRequired()){
			throw new NoEnhancementRequiredException();
		}
		
		if (!classMeta.hasDefaultConstructor()){
		    DefaultConstructor.add(cv, classMeta);
		}

		MarkerField.addGetMarker(cv, classMeta.getClassName());
		
		if (!classMeta.isSuperClassEntity()){
			// Add the _ebean_getIntercept() _ebean_setIntercept() methods
			InterceptField.addGetterSetter(cv, classMeta.getClassName());
			
			// Add add/removePropertyChangeListener methods
			MethodPropertyChangeListener.addMethod(cv, classMeta);
		}
		
		// Add the field set/get methods which are used in place
		// of GETFIELD PUTFIELD instructions
		classMeta.addFieldGetSetMethods(cv);
		
		//Add the getField(index) and setField(index) methods
		IndexFieldWeaver.addMethods(cv, classMeta);
		
		MethodSetEmbeddedLoaded.addMethod(cv, classMeta);
		MethodIsEmbeddedNewOrDirty.addMethod(cv, classMeta);
        MethodNewInstance.addMethod(cv, classMeta);
		
		// register with the agentContext
		enhanceContext.addClassMeta(classMeta);
		
		super.visitEnd();
	}

	private boolean isDefaultConstructor(String name, String desc){

		if (name.equals("<init>")) {
			if (desc.equals("()V")) {
				classMeta.setHasDefaultConstructor(true);
			}
			return true;
		}
		
		return false;
	}
	
	private boolean interceptEntityMethod(int access, String name, String desc, String signature,
			String[] exceptions) {

		if ((access & Opcodes.ACC_STATIC) != 0) {
			// no interception of static methods?
			if (isLog(2)){
				log("Skip intercepting static method "+name);					
			}
			return false;
		}

		if (name.equals("hashCode") && desc.equals("()I")) {
			classMeta.setHasEqualsOrHashcode(true);
			return true;
		}

		if (name.equals("equals") && desc.equals("(Ljava/lang/Object;)Z")) {
			classMeta.setHasEqualsOrHashcode(true);
			return true;
		}
		
		if (name.equals("toString") && desc.equals("()Ljava/lang/String;")) {
			// don't intercept toString as its is used
			// during debugging etc
			return false;
		}

		return true;
	}
}
