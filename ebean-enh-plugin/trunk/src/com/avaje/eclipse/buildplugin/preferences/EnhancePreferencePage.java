package com.avaje.eclipse.buildplugin.preferences;

import org.eclipse.jface.preference.ComboFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.avaje.eclipse.buildplugin.BuildPluginActivator;


/**
 * This class represents a preference page that
 * is contributed to the Preferences dialog. By 
 * subclassing <samp>FieldEditorPreferencePage</samp>, we
 * can use the field support built into JFace that allows
 * us to create a page that is small and knows how to 
 * save, restore and apply itself.
 * <p>
 * This page is used to modify preferences only. They
 * are stored in the preference store that belongs to
 * the main plug-in class. That way, preferences can
 * be accessed directly via the preference store.
 */
public class EnhancePreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	private String toggleMenuDescription = 
		"Enhancement is enabled/disabled per project:\n"
		+" - select the project\n"
		+" - right mouse button menu\n"
		+" - Enable/Disable Ebean Enhancement";

	public EnhancePreferencePage() {
		super(GRID);
		BuildPluginActivator activator = BuildPluginActivator.getDefault();
		if (activator == null){
			BuildPluginActivator.logError("Plugin not activated when creating Preference Page?", null);
		} else {
			setPreferenceStore(activator.getPreferenceStore());
		}
	}
	
	/**
	 * Creates the field editors. Field editors are abstractions of
	 * the common GUI blocks needed to manipulate various types
	 * of preferences. Each field editor knows how to save and
	 * restore itself.
	 */
	public void createFieldEditors() {

		addField(new LabelFieldEditor(toggleMenuDescription, getFieldEditorParent()));		
		addField(new SpacerFieldEditor(getFieldEditorParent()));

		
		addField(new ComboFieldEditor(
				PreferenceConstants.P_ENHANCE_DEBUG_LEVEL,
			"Enhancement Logging:       ",
			new String[][] { { "0 - No Logging", "0" }, {"1 - Minimum Logging", "1"}, {"2", "2"},{"3", "3"},{"4", "4"},
						{"5", "5"},{"6", "6"},{"7", "7"},{"8", "8"},{"9", "9"},{"10 - Maximum Logging", "10"}}
		, getFieldEditorParent()));

		addField(new LabelPairFieldEditor("Purpose:","Logging the enhancement process", getFieldEditorParent()));
		addField(new LabelPairFieldEditor("Location:","${workspace}/.metadata/.plugins/\ncom.avaje.ebean.enhancer.plugin.log/enhance.log", getFieldEditorParent()));
		
		addField(new SpacerFieldEditor(getFieldEditorParent()));
		
		addField(new ComboFieldEditor(
				PreferenceConstants.P_PLUGIN_DEBUG_LEVEL,
			"Plugin Logging:",
			new String[][] { { "No Logging", "0" }, {"Minimum Logging", "1"}, {"Full Logging", "2"} }
		, getFieldEditorParent()));
		
		addField(new LabelPairFieldEditor("Purpose:","Logging this plugin", getFieldEditorParent()));
		addField(new LabelPairFieldEditor("Location:","${workspace}/.metadata/.log", getFieldEditorParent()));

		addField(new SpacerFieldEditor(getFieldEditorParent()));
		addField(new LabelFieldEditor("Note: For eclipse 3.4 you can view this log via (window - show view - error log).", getFieldEditorParent()));		

	}

	public void init(IWorkbench workbench) {
	}
	
}