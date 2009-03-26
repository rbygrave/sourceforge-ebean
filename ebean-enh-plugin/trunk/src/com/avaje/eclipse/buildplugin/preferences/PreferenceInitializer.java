package com.avaje.eclipse.buildplugin.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

import com.avaje.eclipse.buildplugin.BuildPluginActivator;


/**
 * Class used to initialise default preference values.
 */
public class PreferenceInitializer extends AbstractPreferenceInitializer {

	public void initializeDefaultPreferences() {
		IPreferenceStore store = BuildPluginActivator.getDefault().getPreferenceStore();
		store.setDefault(PreferenceConstants.P_PLUGIN_DEBUG_LEVEL, "1");
		store.setDefault(PreferenceConstants.P_ENHANCE_DEBUG_LEVEL, "1");
	}

}
