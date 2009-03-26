package com.avaje.eclipse.buildplugin.builder;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import com.avaje.ebean.enhance.agent.Transformer;
import com.avaje.eclipse.buildplugin.BuildPluginActivator;

public final class EnhanceBuilder extends IncrementalProjectBuilder {

	public static final String BUILDER_ID = "com.avaje.eclipse.buildplugin.enhanceBuilder";

	@SuppressWarnings("unchecked")
	protected IProject[] build(int kind, Map args, IProgressMonitor monitor)
			throws CoreException {
		if (kind == FULL_BUILD) {
			fullBuild(monitor);
		} else {
			IResourceDelta delta = getDelta(getProject());
			if (delta == null) {
				fullBuild(monitor);
			} else {
				incrementalBuild(delta, monitor);
			}
		}
		return null;
	}

	private void fullBuild(final IProgressMonitor monitor)
			throws CoreException {
		try {
			getProject().accept(new ResourceVisitor(monitor));
		} catch (CoreException e) {
			BuildPluginActivator.logError("Error with fullBuild", e);
		}
	}

	private void incrementalBuild(IResourceDelta delta,
			IProgressMonitor monitor) throws CoreException {
		delta.accept(new DeltaVisitor(monitor));
	}
	
	private String getClassBinDirectory(IFile file, String className){
		String rawLocation = file.getRawLocation().toString();
		int len = rawLocation.length() - className.length() - ".class".length();
		return rawLocation.substring(0, len);
	}
	
	private void checkResource(IResource resource, IProgressMonitor monitor) {
		
		if (resource instanceof IFile && resource.getName().endsWith(".class")) {
			IFile file = (IFile) resource;
			InputStream is = null;
			try {
				
				int pluginDebug = BuildPluginActivator.getDebugLevel();
				int enhanceDebug = BuildPluginActivator.getEnhanceDebugLevel();
				
				is = file.getContents();
				byte[] classBytes = readBytes(is);
				
				String className = DetermineClass.getClassName(classBytes);
				
				if (pluginDebug >= 2){
					BuildPluginActivator.logInfo("... processing class: "+className, null);
				}

				// use this to find other class files for inheritance purposes
				String classBinDirectory = getClassBinDirectory(file, className);
				Transformer et = new Transformer(classBinDirectory, "debug="+enhanceDebug);

				PrintStream transformLog = null;
				if (enhanceDebug > 0){
					transformLog = BuildPluginActivator.createTransformLog();
					et.setLogout(transformLog);
				}
				try {
					byte[] outBytes = et.transform(null, className, null, null, classBytes);
					
					if (outBytes != null){
						
						ByteArrayInputStream bais = new ByteArrayInputStream(outBytes);
						file.setContents(bais, true, false, monitor);
						
						if (pluginDebug >= 1){
							BuildPluginActivator.logInfo("enhanced: "+className);
						}
					}
				} finally {
					if (transformLog != null){
						transformLog.close();
					}
				}
				
			} catch (Exception e) {
				BuildPluginActivator.logError("Error during enhancement", e);
				
			} finally {
				if (is != null){
					try {
						is.close();
					} catch (IOException e) {
						BuildPluginActivator.logError("Error closing inputStream", e);
					}
				}
			}
		}
	}

	private byte[] readBytes(InputStream in) throws IOException {
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		BufferedInputStream bi = new BufferedInputStream(in);
		byte[] buf = new byte[1024];
		int len = -1;
		while ((len = bi.read(buf)) > -1) {
			baos.write(buf, 0, len);			
		}
		return baos.toByteArray();
	}
	
	private class DeltaVisitor implements IResourceDeltaVisitor {
		
		final IProgressMonitor monitor;
		
		DeltaVisitor(IProgressMonitor monitor) {
			this.monitor = monitor;
		}

		public boolean visit(IResourceDelta delta) throws CoreException {
			IResource resource = delta.getResource();
			switch (delta.getKind()) {
			case IResourceDelta.ADDED:
				checkResource(resource, monitor);
				break;
			case IResourceDelta.REMOVED:
				break;
			case IResourceDelta.CHANGED:
				checkResource(resource, monitor);
				break;
			}
			//return true to continue visiting children.
			return true;
		}
	}

	private class ResourceVisitor implements IResourceVisitor {
		final IProgressMonitor monitor;
		ResourceVisitor(final IProgressMonitor monitor) {
			this.monitor = monitor;
		}
		public boolean visit(IResource resource) {
			checkResource(resource, monitor);
			return true;
		}
	}
	
}
