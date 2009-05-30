package com.avaje.ebean.enhance.agent;

import com.avaje.ebean.enhance.asm.ClassVisitor;
import com.avaje.ebean.enhance.asm.FieldVisitor;
import com.avaje.ebean.enhance.asm.Label;
import com.avaje.ebean.enhance.asm.MethodVisitor;
import com.avaje.ebean.enhance.asm.Opcodes;

/**
 * Used to detect if a class has been enhanced.
 * <p>
 * Moved to use this over just relying on the existence of the EntityBean interface
 * to make the enhancement more robust.
 * </p>
 */
public class MethodPropertyChangeListener implements Opcodes, EnhanceConstants {

	/**
	 * Add the PropertyChangeSupport _ebean_pcs field.
	 */
	public static void addField(ClassVisitor cv, ClassMeta classMeta) {
		FieldVisitor fv = cv.visitField(ACC_PROTECTED + ACC_TRANSIENT, "_ebean_pcs", "Ljava/beans/PropertyChangeSupport;", null, null);
		fv.visitEnd();
	}

	/**
	 * Add the addPropertyChangeListener and removePropertyChangeListener methods.
	 */
	public static void addMethod(ClassVisitor cv, ClassMeta classMeta) {
		addAddListenerMethod(cv, classMeta);
		addRemoveListenerMethod(cv, classMeta);
	}
	
	/**
	 * Generate the addPropertyChangeListener method.
	 * 
	 * <pre>
	 *   public void addPropertyChangeListener(PropertyChangeListener listener) {
	 *     if (_ebean_pcs == null){
	 *       _ebean_pcs = new PropertyChangeSupport(this);
	 *     }
	 *     _ebean_pcs.addPropertyChangeListener(listener);
	 *   }
	 * </pre>
	 */
	private static void addAddListenerMethod(ClassVisitor cv, ClassMeta classMeta) {
		
		String className = classMeta.getClassName();
		
		MethodVisitor mv;

		mv = cv.visitMethod(ACC_PUBLIC, "addPropertyChangeListener", "(Ljava/beans/PropertyChangeListener;)V", null, null);
		mv.visitCode();
		Label l0 = new Label();
		mv.visitLabel(l0);
		mv.visitLineNumber(46, l0);
		mv.visitVarInsn(ALOAD, 0);
		mv.visitFieldInsn(GETFIELD, className, "_ebean_pcs", "Ljava/beans/PropertyChangeSupport;");
		Label l1 = new Label();
		mv.visitJumpInsn(IFNONNULL, l1);
		Label l2 = new Label();
		mv.visitLabel(l2);
		mv.visitLineNumber(47, l2);
		mv.visitVarInsn(ALOAD, 0);
		mv.visitTypeInsn(NEW, "java/beans/PropertyChangeSupport");
		mv.visitInsn(DUP);
		mv.visitVarInsn(ALOAD, 0);
		mv.visitMethodInsn(INVOKESPECIAL, "java/beans/PropertyChangeSupport", "<init>", "(Ljava/lang/Object;)V");
		mv.visitFieldInsn(PUTFIELD, className, "_ebean_pcs", "Ljava/beans/PropertyChangeSupport;");
		mv.visitLabel(l1);
		mv.visitLineNumber(49, l1);
		mv.visitVarInsn(ALOAD, 0);
		mv.visitFieldInsn(GETFIELD, className, "_ebean_pcs", "Ljava/beans/PropertyChangeSupport;");
		mv.visitVarInsn(ALOAD, 1);
		mv.visitMethodInsn(INVOKEVIRTUAL, "java/beans/PropertyChangeSupport", "addPropertyChangeListener", "(Ljava/beans/PropertyChangeListener;)V");
		Label l3 = new Label();
		mv.visitLabel(l3);
		mv.visitLineNumber(50, l3);
		mv.visitInsn(RETURN);
		Label l4 = new Label();
		mv.visitLabel(l4);
		mv.visitLocalVariable("this", "L"+className+";", null, l0, l4, 0);
		mv.visitLocalVariable("listener", "Ljava/beans/PropertyChangeListener;", null, l0, l4, 1);
		mv.visitMaxs(4, 2);
		mv.visitEnd();
	}

	/**
	 * Add the removePropertyChangeListener method.
	 * 
	 * <pre>
	 * 	public void removePropertyChangeListener(PropertyChangeListener listener) {
	 *    if (_ebean_pcs != null){
	 *        _ebean_pcs.removePropertyChangeListener(listener);
	 *    }
	 *  }
	 * </pre>
	 */
	private static void addRemoveListenerMethod(ClassVisitor cv, ClassMeta classMeta) {
		
		String className = classMeta.getClassName();
		
		MethodVisitor mv;
		
		mv = cv.visitMethod(ACC_PUBLIC, "removePropertyChangeListener", "(Ljava/beans/PropertyChangeListener;)V", null, null);
		mv.visitCode();
		Label l0 = new Label();
		mv.visitLabel(l0);
		mv.visitLineNumber(53, l0);
		mv.visitVarInsn(ALOAD, 0);
		mv.visitFieldInsn(GETFIELD, className, "_ebean_pcs", "Ljava/beans/PropertyChangeSupport;");
		Label l1 = new Label();
		mv.visitJumpInsn(IFNULL, l1);
		Label l2 = new Label();
		mv.visitLabel(l2);
		mv.visitLineNumber(54, l2);
		mv.visitVarInsn(ALOAD, 0);
		mv.visitFieldInsn(GETFIELD, className, "_ebean_pcs", "Ljava/beans/PropertyChangeSupport;");
		mv.visitVarInsn(ALOAD, 1);
		mv.visitMethodInsn(INVOKEVIRTUAL, "java/beans/PropertyChangeSupport", "removePropertyChangeListener", "(Ljava/beans/PropertyChangeListener;)V");
		mv.visitLabel(l1);
		mv.visitLineNumber(56, l1);
		mv.visitInsn(RETURN);
		Label l3 = new Label();
		mv.visitLabel(l3);
		mv.visitLocalVariable("this", "L"+className+";", null, l0, l3, 0);
		mv.visitLocalVariable("listener", "Ljava/beans/PropertyChangeListener;", null, l0, l3, 1);
		mv.visitMaxs(2, 2);
		mv.visitEnd();
		

	}
}
