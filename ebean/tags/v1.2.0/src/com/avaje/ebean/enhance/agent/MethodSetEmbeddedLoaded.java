package com.avaje.ebean.enhance.agent;

import java.util.List;

import com.avaje.ebean.enhance.asm.ClassVisitor;
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
public class MethodSetEmbeddedLoaded implements Opcodes, EnhanceConstants {

	/**
	 * Generate the _ebean_setEmbeddedLoaded() method.
	 * 
	 * <pre>
	 * public void _ebean_setEmbeddedLoaded() {
	 *  // for each embedded bean field...
	 * 	entityBeanIntercept.setEmbeddedLoaded(embeddedBeanField);
	 * }
	 * </pre>
	 */
	public static void addMethod(ClassVisitor cv, ClassMeta classMeta) {
		
		String className = classMeta.getClassName();
		
		MethodVisitor mv;
		
		mv = cv.visitMethod(ACC_PUBLIC, "_ebean_setEmbeddedLoaded", "()V", null, null);
		mv.visitCode();
		
		Label labelBegin = null;
		List<FieldMeta> allFields = classMeta.getAllFields();
		for (int i = 0; i < allFields.size(); i++) {
			FieldMeta fieldMeta = allFields.get(i);
			if (fieldMeta.isEmbedded()){
				
				Label l0 = new Label();
				if (labelBegin == null){
					labelBegin = l0;
				}
				
				mv.visitLabel(l0);
				mv.visitLineNumber(0, l0);
				mv.visitVarInsn(ALOAD, 0);
				mv.visitFieldInsn(GETFIELD, className, INTERCEPT_FIELD, "Lcom/avaje/ebean/bean/EntityBeanIntercept;");
				mv.visitVarInsn(ALOAD, 0);
				fieldMeta.appendSwitchGet(mv, classMeta, false);
				mv.visitMethodInsn(INVOKEVIRTUAL, "com/avaje/ebean/bean/EntityBeanIntercept", "setEmbeddedLoaded", "(Ljava/lang/Object;)V");
			}
		}
		
		Label l2 = new Label();
		if (labelBegin == null){
			labelBegin = l2;
		}
		mv.visitLabel(l2);
		mv.visitLineNumber(1, l2);
		mv.visitInsn(RETURN);
		Label l3 = new Label();
		mv.visitLabel(l3);
		mv.visitLocalVariable("this", "L"+className+";", null, labelBegin, l3, 0);
		mv.visitMaxs(2, 1);
		mv.visitEnd();
	}
}
