package com.avaje.eclipse.buildplugin.builder;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URL;
import java.util.Arrays;
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
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.JavaRuntime;

import com.avaje.ebean.enhance.agent.Transformer;
import com.avaje.ebean.enhance.agent.UrlPathHelper;
import com.avaje.eclipse.buildplugin.BuildPluginActivator;

public final class EnhanceBuilder extends IncrementalProjectBuilder {

	public static final String BUILDER_ID = "com.avaje.eclipse.buildplugin.enhanceBuilder";

	@SuppressWarnings("unchecked")
	protected IProject[] build(int kind, Map args, IProgressMonitor monitor)
			throws CoreException {
		
		IProject project = getProject();
				
		if (kind == FULL_BUILD) {
			fullBuild(monitor);
		} else {
			IResourceDelta delta = getDelta(project);
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

	private void incrementalBuild(IResourceDelta delta, IProgressMonitor monitor) throws CoreException {
		
		delta.accept(new DeltaVisitor(monitor));
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
					BuildPluginActivator.logInfo("... processing class: "+className);
				}
				
				IProject project = getProject();
				IJavaProject javaProject = JavaCore.create(project);
				

				String[] ideClassPath = JavaRuntime.computeDefaultRuntimeClassPath(javaProject);
				
				URL[] paths = UrlPathHelper.convertToUrl(ideClassPath);

				if (BuildPluginActivator.getDebugLevel() >= 2){
					BuildPluginActivator.logInfo("... classpath: "+Arrays.toString(paths));
				}
				
				Transformer et = new Transformer(paths, "debug="+enhanceDebug);

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
		
		private final IProgressMonitor monitor;
		
		private DeltaVisitor(IProgressMonitor monitor) {
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
		
		private final IProgressMonitor monitor;
		
		private ResourceVisitor(final IProgressMonitor monitor) {
			this.monitor = monitor;
		}
		
		public boolean visit(IResource resource) {
			checkResource(resource, monitor);
			return true;
		}
	}
	
}
