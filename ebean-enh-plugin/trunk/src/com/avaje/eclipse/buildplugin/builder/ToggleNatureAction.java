package com.avaje.eclipse.buildplugin.builder;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

import com.avaje.eclipse.buildplugin.BuildPluginActivator;


public class ToggleNatureAction implements IObjectActionDelegate {

	private IProject project;
	
	private static final String describeEnable = "Enable Ebean Enhancement";
	
	private static final String describeDisable = "Disable Ebean Enhancement";
	
	
	public ToggleNatureAction() {
		
	}

	public void run(IAction action) {
		
		if (project == null || !project.isAccessible()) {
			BuildPluginActivator.logError("Error: ... project is null or not accessible (on run)?",null);
			
		} else {
			toggleNature(action, project);
		}
	}

	public void selectionChanged(IAction action, ISelection selection) {
		
	}

	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
		
		ISelection selection = targetPart.getSite().getSelectionProvider()
				.getSelection();
		
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection sel = (IStructuredSelection) selection;
			Object firstElement = sel.getFirstElement();

			if (firstElement instanceof IProject) {
				project = (IProject) firstElement;
				
			} else if (firstElement instanceof IJavaProject) {
				IJavaProject jp = (IJavaProject)firstElement;
				project = jp.getProject();
			} 
			
			if (project == null){
				BuildPluginActivator.logError("... Toggle not on a Project??",null);
				
			} else {
				try {
					
					if (!project.isAccessible()) {
						BuildPluginActivator.logError("... project not accessible on Toggle??", null);
						
					} else {
						if (project.hasNature(EnhanceNature.NATURE_ID)) {
							action.setText(describeDisable);
						} else {
							action.setText(describeEnable);
						}
					}
					action.setEnabled(project.isAccessible());
					
				} catch (CoreException e) {
					BuildPluginActivator.logError("Error setting menu text",e);
				}
			}
		}
	}

	/**
	 * Toggles sample nature on a project
	 * 
	 * @param project
	 *            to have sample nature added or removed
	 */
	private void toggleNature(IAction action, IProject project) {
		
		
		try {
			IProjectDescription description = project.getDescription();
			String[] natures = description.getNatureIds();

			for (int i = 0; i < natures.length; ++i) {
				if (EnhanceNature.NATURE_ID.equals(natures[i])) {
					// Remove the nature
					String[] newNatures = new String[natures.length - 1];
					System.arraycopy(natures, 0, newNatures, 0, i);
					System.arraycopy(natures, i + 1, newNatures, i,
							natures.length - i - 1);
					description.setNatureIds(newNatures);
					project.setDescription(description, null);

					action.setText(describeEnable);
					
					if (BuildPluginActivator.getDebugLevel() >= 1){
						BuildPluginActivator.logInfo("... Enhancement disabled!");						
					}
					
					return;
				}
			}

			// Add the nature
			String[] newNatures = new String[natures.length + 1];
			System.arraycopy(natures, 0, newNatures, 0, natures.length);
			newNatures[natures.length] = EnhanceNature.NATURE_ID;
			description.setNatureIds(newNatures);
			project.setDescription(description, null);

			action.setText(describeDisable);
			
			if (BuildPluginActivator.getDebugLevel() >= 1){
				BuildPluginActivator.logInfo("... Enhancement enabled!!!");						
			}
			

		} catch (CoreException e) {
			BuildPluginActivator.logError("Error toggling Nature - adding/removing builder", e);
		}
	}

}
