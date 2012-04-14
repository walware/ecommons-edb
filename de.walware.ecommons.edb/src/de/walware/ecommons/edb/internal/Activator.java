package de.walware.ecommons.edb.internal;

import org.eclipse.core.runtime.Plugin;
import org.osgi.framework.BundleContext;


/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends Plugin {
	
	public static final String PLUGIN_ID = "de.walware.ecommons.edb";
	
	
	private static Activator gPlugin;
	
	public static Activator getDefault() {
		return gPlugin;
	}
	
	
	/**
	 * The constructor
	 */
	public Activator() {
	}
	
	
	@Override
	public void start(final BundleContext context) throws Exception {
		super.start(context);
		gPlugin = this;
	}
	
	@Override
	public void stop(final BundleContext context) throws Exception {
		gPlugin = null;
		super.stop(context);
	}
	
}
