package com.avaje.ebean.enhance.agent;

import java.util.ArrayList;

import com.avaje.ebean.enhance.asm.AnnotationVisitor;
import com.avaje.ebean.enhance.asm.MethodVisitor;
import com.avaje.ebean.enhance.asm.Type;
import com.avaje.ebean.enhance.asm.commons.MethodAdviceAdapter;

/**
 * Adapts a method to support Transactional.
 * <p>
 * Adds a TxScope and ScopeTrans local variables. On normal exit makes a call
 * out via InternalServer to end the scopeTrans depending on the exit type
 * opcode (ATHROW vs ARETURN etc) and whether particular throwable's cause a
 * rollback or not.
 * </p>
 */
public class ScopeTransAdapter extends MethodAdviceAdapter {

	static final Type txScopeType = Type.getType("Lcom/avaje/ebean/TxScope;");
	static final Type scopeTransType = Type.getType("Lcom/avaje/ebean/bean/ScopeTrans;");
	static final Type internalServerType = Type.getType("Lcom/avaje/ebean/bean/InternalServer;");

	final AnnotationInfo annotationInfo;

	final ClassAdapterTransactional owner;
	
	boolean transactional;

	int posTxScope;
	int posScopeTrans;
	
	public ScopeTransAdapter(ClassAdapterTransactional owner, final MethodVisitor mv, final int access, final String name, final String desc) {
		super(mv, access, name, desc);
		this.owner = owner;
		
		// inherit from class level Transactional annotation
		AnnotationInfo parentInfo = owner.classAnnotationInfo;
		
		// inherit from interface method transactional annotation
		AnnotationInfo interfaceInfo = owner.getInterfaceTransactionalInfo(name, desc);
		if (parentInfo == null){
			parentInfo = interfaceInfo;
		} else {
			parentInfo.setParent(interfaceInfo);
		}
		
		// inherit transactional annotations from parentInfo
		annotationInfo = new AnnotationInfo(parentInfo);
		
		// default based on whether Transactional annotation
		// is at the class level or on interface method
		transactional = parentInfo != null; 
	}

	public boolean isTransactional() {
		return transactional;
	}
	
	@Override
	public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
		if (desc.equals(EnhanceConstants.AVAJE_TRANSACTIONAL_ANNOTATION)) {
			transactional = true;
		}
		AnnotationVisitor av = super.visitAnnotation(desc, visible);
		return new AnnotationInfoVisitor(null, annotationInfo, av);
	}

	private void setTxType(Object txType){
		
		mv.visitVarInsn(ALOAD, posTxScope);
		mv.visitLdcInsn(txType.toString());
		mv.visitMethodInsn(INVOKESTATIC, "com/avaje/ebean/TxType", "valueOf", "(Ljava/lang/String;)Lcom/avaje/ebean/TxType;");
		mv.visitMethodInsn(INVOKEVIRTUAL, "com/avaje/ebean/TxScope", "setType", "(Lcom/avaje/ebean/TxType;)Lcom/avaje/ebean/TxScope;");
		mv.visitInsn(POP);
	}
	
	private void setTxIsolation(Object txIsolation){
		
		mv.visitVarInsn(ALOAD, posTxScope);
		mv.visitLdcInsn(txIsolation.toString());
		mv.visitMethodInsn(INVOKESTATIC, "com/avaje/ebean/TxIsolation", "valueOf", "(Ljava/lang/String;)Lcom/avaje/ebean/TxIsolation;");
		mv.visitMethodInsn(INVOKEVIRTUAL, "com/avaje/ebean/TxScope", "setIsolation", "(Lcom/avaje/ebean/TxIsolation;)Lcom/avaje/ebean/TxScope;");
		mv.visitInsn(POP);
	}
	
	private void setReadOnly(Object readOnlyObj){

		boolean readOnly = (Boolean)readOnlyObj;
		mv.visitVarInsn(ALOAD, posTxScope);
		if (readOnly){
			mv.visitInsn(ICONST_1);
		} else {
			mv.visitInsn(ICONST_0);
		}
		mv.visitMethodInsn(INVOKEVIRTUAL, "com/avaje/ebean/TxScope", "setReadOnly", "(Z)Lcom/avaje/ebean/TxScope;");
	}
	
	/**
	 * Add bytecode to add the noRollbackFor throwable types to the TxScope.
	 */
	private void setNoRollbackFor(Object noRollbackFor){

		ArrayList<?> list = (ArrayList<?>)noRollbackFor;
		
		for (int i = 0; i < list.size(); i++) {
			
			Type throwType =  (Type)list.get(i);
			
			mv.visitVarInsn(ALOAD, posTxScope);
			mv.visitLdcInsn(throwType);
			mv.visitMethodInsn(INVOKEVIRTUAL, txScopeType.getInternalName(), "setNoRollbackFor", "(Ljava/lang/Class;)Lcom/avaje/ebean/TxScope;");
			mv.visitInsn(POP);
		}
	}
	
	/**
	 * Add bytecode to add the rollbackFor throwable types to the TxScope.
	 */
	private void setRollbackFor(Object rollbackFor){

		ArrayList<?> list = (ArrayList<?>)rollbackFor;
		
		for (int i = 0; i < list.size(); i++) {
			
			Type throwType =  (Type)list.get(i);
			
			mv.visitVarInsn(ALOAD, posTxScope);
			mv.visitLdcInsn(throwType);
			mv.visitMethodInsn(INVOKEVIRTUAL, txScopeType.getInternalName(), "setRollbackFor", "(Ljava/lang/Class;)Lcom/avaje/ebean/TxScope;");
			mv.visitInsn(POP);
		}
	}
	
	@Override
	protected void onMethodEnter() {

		if (!transactional) {
			return;
		}
		
		// call back to owner to log debug information
		owner.transactionalMethod(methodName, methodDesc, annotationInfo);

		posTxScope = newLocal(txScopeType);
		posScopeTrans = newLocal(scopeTransType);

		mv.visitTypeInsn(NEW, txScopeType.getInternalName());
		mv.visitInsn(DUP);
		mv.visitMethodInsn(INVOKESPECIAL, txScopeType.getInternalName(), "<init>", "()V");
		mv.visitVarInsn(ASTORE, posTxScope);
		
		Object txType = annotationInfo.getValue("type");
		if (txType != null){
			setTxType(txType);
		}
		
		Object txIsolation = annotationInfo.getValue("isolation");
		if (txIsolation != null){
			setTxIsolation(txIsolation);
		}
		
		Object readOnly = annotationInfo.getValue("readOnly");
		if (readOnly != null){
			setReadOnly(readOnly);
		}
		
		Object noRollbackFor = annotationInfo.getValue("noRollbackFor");
		if (noRollbackFor != null){
			setNoRollbackFor(noRollbackFor);
		}
		
		Object rollbackFor = annotationInfo.getValue("rollbackFor");
		if (rollbackFor != null){
			setRollbackFor(rollbackFor);
		}
		

		mv.visitVarInsn(ALOAD, posTxScope);
		mv.visitMethodInsn(INVOKESTATIC, internalServerType.getInternalName(), "createScopeTrans", "("
				+ txScopeType.getDescriptor() + ")" + scopeTransType.getDescriptor());
		mv.visitVarInsn(ASTORE, posScopeTrans);
	}

	@Override
	protected void onMethodExit(int opcode) {

		if (!transactional) {
			return;
		}

		if (opcode == RETURN) {
			visitInsn(ACONST_NULL);

		} else if (opcode == ARETURN || opcode == ATHROW) {
			dup();

		} else {
			if (opcode == LRETURN || opcode == DRETURN) {
				dup2();
			} else {
				dup();
			}
			box(Type.getReturnType(this.methodDesc));
		}
		visitIntInsn(SIPUSH, opcode);
		loadLocal(posScopeTrans);

		visitMethodInsn(INVOKESTATIC, internalServerType.getInternalName(), "onExitScopeTrans", "(Ljava/lang/Object;I"
				+ scopeTransType.getDescriptor() + ")V");
	}

//	 private void test(int opCode, Object returnOrThrowable) {
//		 boolean b = Boolean.TRUE;
//	 TxScope txScope = new TxScope();
//	 txScope.setType(TxType.valueOf("MANDATORY"));
//	 txScope.setNoRollbackFor(RuntimeException.class);
//	 txScope.setNoRollbackFor(IOException.class);
//	 txScope.setReadonly(true);
//	 txScope.setReadonly(false);
//
//	 
//	 ScopeTrans t = InternalServer.createScopeTrans(txScope);
//	 //ScopeTrans t = new ScopeTrans();
//	 System.out.println("stuff...");
//	 InternalServer.onExitScopeTrans(returnOrThrowable, opCode, t);
//	 }
}
