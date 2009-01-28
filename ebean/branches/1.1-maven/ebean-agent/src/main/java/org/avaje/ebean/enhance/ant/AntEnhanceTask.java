package org.avaje.ebean.enhance.ant;

import java.io.File;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import org.avaje.ebean.enhance.agent.Transformer;

/**
 * An ANT task that can enhance entity beans etc for use by Ebean.
 * <p>
 * You can use this ANT task as part of your build process to enhance entity
 * beans etc.
 * </p>
 * <p>
 * The parameters are:
 * <ul>
 * <li> <b>classSource</b> This is the root directory where the .class files
 * are found. </li>
 * <li> <b>classDestination</b> This is the root directory where the .class
 * files are written to. If this is left out then this defaults to the
 * <b>classSource</b>. </li>
 * <li> <b>packages</b> A comma delimited list of packages that is searched for
 * classes that need to be enhanced. If the package ends with ** or * then all
 * subpackages are also searched. </li>
 * <li> <b>transformArgs</b> Arguments passed to the transformer. Typically a
 * debug level in the form of debug=1 etc. </li>
 * </ul>
 * </p>
 * 
 * <pre class="code">
 *   
 * 	 &lt;taskdef name=&quot;ebeanEnhance&quot; classname=&quot;org.avaje.ebean.enhance.ant.AntEnhanceTask&quot; classpath=&quot;bin&quot; /&gt;
 * 
 *   &lt;target name=&quot;enhance&quot; depends=&quot;compile&quot;&gt;
 *       &lt;ebeanEnhance 
 *            classSource=&quot;${bin.dir}&quot; 
 *            packages=&quot;org.avaje.ebean.meta.**, com.acme.myapp.entity.**&quot; 
 *            transformArgs=&quot;debug=1&quot; /&gt;
 *   &lt;/target&gt;
 *   
 * </pre>
 */
public class AntEnhanceTask extends Task {

	String classSource;

	String classDestination;

	String transformArgs;

	String packages;

	@Override
	public void execute() throws BuildException {
		File f = new File("");
		System.out.println("Current Directory: "+f.getAbsolutePath());
				
		Transformer t = new Transformer(classSource, transformArgs);
	
		ClassLoader cl = AntEnhanceTask.class.getClassLoader();
		OfflineFileTransform ft = new OfflineFileTransform(t, cl, classSource, classDestination);

		ft.process(packages);
	}

	/**
	 * Set the directory holding the class files we want to transform.
	 */
	public void setClassSource(String source) {
		this.classSource = source;
	}

	/**
	 * Set the destination directory where we will put the transformed classes.
	 * <p>
	 * This is commonly the same as the classSource directory.
	 * </p>
	 */
	public void setClassDestination(String destination) {
		this.classDestination = destination;
	}

	/**
	 * Set the arguments passed to the transformer.
	 */
	public void setTransformArgs(String transformArgs) {
		this.transformArgs = transformArgs;
	}

	/**
	 * Set the package name to search for classes to transform.
	 * <p>
	 * If the package name ends in "/**" then this recursively transforms all
	 * sub packages as well.
	 * </p>
	 */
	public void setPackages(String packages) {
		this.packages = packages;
	}

}
