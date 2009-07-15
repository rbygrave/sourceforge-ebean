package com.avaje.eclipse.buildplugin.builder;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;

import com.avaje.ebean.enhance.agent.Transformer;
import com.avaje.eclipse.buildplugin.BuildPluginActivator;

public final class EnhanceBuilder extends IncrementalProjectBuilder {

	public static final String BUILDER_ID = "com.avaje.eclipse.buildplugin.enhanceBuilder";

	private String getPath(IClasspathEntry entry){
		
		switch (entry.getContentKind()) {
		case IClasspathEntry.CPE_CONTAINER:
			System.out.println("Container: "+entry);
			return null;

		case IClasspathEntry.CPE_LIBRARY:
			System.out.println("CPE_LIBRARY: "+entry);
			return null;

		case IClasspathEntry.CPE_PROJECT:
			System.out.println("CPE_PROJECT: "+entry);
			return null;

		case IClasspathEntry.CPE_SOURCE:
			System.out.println("CPE_SOURCE: "+entry);
			IPath p = entry.getOutputLocation();
			return p.toString();
			
		case IClasspathEntry.CPE_VARIABLE:
			System.out.println("CPE_VARIABLE: "+entry);
			return null;

		default:
			return null;
		}
	}
	
	private List<String> getPaths(IClasspathEntry[] resolvedClasspath) {
		
		List<String> list = new ArrayList<String>();
		
		for (int i = 0; i < resolvedClasspath.length; i++) {
			String cp = getPath(resolvedClasspath[i]);
			if (cp != null){
				list.add(cp);
			}
		}
		
		return list;
	}
	
	@SuppressWarnings("unchecked")
	protected IProject[] build(int kind, Map args, IProgressMonitor monitor)
			throws CoreException {
		
		IProject project = getProject();
		IJavaProject javaProject = JavaCore.create(project);
		IClasspathEntry[] resolvedClasspath = javaProject.getResolvedClasspath(true);
		
		List<String> cp = getPaths(resolvedClasspath);
		
		if (kind == FULL_BUILD) {
			fullBuild(javaProject, monitor, cp);
		} else {
			IResourceDelta delta = getDelta(project);
			if (delta == null) {
				fullBuild(javaProject, monitor, cp);
			} else {
				incrementalBuild(javaProject, delta, monitor, cp);
			}
		}
		return null;
	}

	private void fullBuild(IJavaProject javaProject, final IProgressMonitor monitor, List<String> cp)
			throws CoreException {
		try {
			getProject().accept(new ResourceVisitor(javaProject, monitor, cp));
		} catch (CoreException e) {
			BuildPluginActivator.logError("Error with fullBuild", e);
		}
	}

	private void incrementalBuild(IJavaProject javaProject, IResourceDelta delta,
			IProgressMonitor monitor, List<String> cp) throws CoreException {
		
		delta.accept(new DeltaVisitor(javaProject, monitor, cp));
	}
	
	private String getClassBinDirectory(IFile file, String className){
		String rawLocation = file.getRawLocation().toString();
		int len = rawLocation.length() - className.length() - ".class".length();
		return rawLocation.substring(0, len);
	}
	
	private void checkResource(IJavaProject project, IResource resource, IProgressMonitor monitor, List<String> cp) {
		
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

				IType type = project.findType(className);
				IClassFile classFile = type.getClassFile();
				byte[] classbytes = classFile.getBytes();
				
				System.out.println("Got bytes "+classbytes.length+" = "+classBytes.length);
				
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
		
		private final IProgressMonitor monitor;
		private final List<String> cp;
		private final IJavaProject javaProject;
		
		private DeltaVisitor(IJavaProject javaProject, IProgressMonitor monitor, List<String> cp) {
			this.javaProject = javaProject;
			this.monitor = monitor;
			this.cp = cp;
		}

		public boolean visit(IResourceDelta delta) throws CoreException {
			IResource resource = delta.getResource();
			switch (delta.getKind()) {
			case IResourceDelta.ADDED:
				checkResource(javaProject, resource, monitor, cp);
				break;
			case IResourceDelta.REMOVED:
				break;
			case IResourceDelta.CHANGED:
				checkResource(javaProject, resource, monitor, cp);
				break;
			}
			//return true to continue visiting children.
			return true;
		}
	}

	private class ResourceVisitor implements IResourceVisitor {
		
		private final IProgressMonitor monitor;
		private final List<String> cp;
		private final IJavaProject javaProject;
		private ResourceVisitor(IJavaProject javaProject, final IProgressMonitor monitor, List<String> cp) {
			this.javaProject = javaProject;
			this.monitor = monitor;
			this.cp = cp;
		}
		public boolean visit(IResource resource) {
			checkResource(javaProject, resource, monitor, cp);
			return true;
		}
	}
	
}
