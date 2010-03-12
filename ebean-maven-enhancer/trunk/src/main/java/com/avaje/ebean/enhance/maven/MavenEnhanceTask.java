package com.avaje.ebean.enhance.maven;

import java.io.File;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

import com.avaje.ebean.enhance.agent.Transformer;
import com.avaje.ebean.enhance.ant.OfflineFileTransform;
import com.avaje.ebean.enhance.ant.TransformationListener;

/**
 * A Maven Plugin that can enhance entity beans etc for use by Ebean.
 * <p>
 * You can use this plugin as part of your build process to enhance entity
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
 *    &lt;plugin&gt;
 *      &lt;groupId&gt;org.avaje&lt;/groupId&gt;
 *      &lt;artifactId&gt;ebean-enhancer-plugin&lt;/artifactId&gt;
 *      &lt;version&gt;2.5&lt;/version&gt;
 *      &lt;executions&gt;
 *        &lt;execution&gt;
 *          &lt;id&gt;main&lt;/id&gt;
 *          &lt;phase&gt;process-classes&lt;/phase&gt;
 *          &lt;goals&gt;
 *            &lt;goal&gt;enhance&lt;/goal&gt;
 *          &lt;/goals&gt;
 *        &lt;/execution&gt;
 *      &lt;/executions&gt;
 *      &lt;configuration&gt;
 *        &lt;classSource&gt;target/classes&lt;/classSource&gt;
 *        &lt;packages&gt;com.avaje.ebean.meta.**, com.acme.myapp.entity.**&lt;/packages&gt;
 *        &lt;transformArgs&gt;debug=1&lt;/transformArgs&gt;
 *      &lt;/configuration&gt;
 *    &lt;/plugin&gt;
 * </pre>
 * <p>To invoke explicitly:<br/>
 * <code>
 * &nbsp;&nbsp;&nbsp;&nbsp;mvn ebean-enhancer:enhance
 * </code>
 * </p>
 * @author Paul Mendelson
 * @version $Revision$, $Date$
 * @since 2.5, Mar, 2009
 * @see com.avaje.ebean.enhance.ant.AntEnhanceTask
 * @goal enhance
 * @phase process-classes
 */
public class MavenEnhanceTask extends AbstractMojo {
	/**
	 * the classpath used to search for e.g. inerited classes
	 * @parameter
	 */
	private String classpath;

	/**
	 * Set the directory holding the class files we want to transform.
	 * @parameter
	 */
	private String classSource;

	/**
	 * Set the destination directory where we will put the transformed classes.
	 * <p>
	 * This is commonly the same as the classSource directory.
	 * </p>
	 * @parameter
	 */
	String classDestination;

	/**
	 * Set the arguments passed to the transformer.
	 * @parameter
	 */
	String transformArgs;

	/**
	 * Set the package name to search for classes to transform.
	 * <p>
	 * If the package name ends in "/**" then this recursively transforms all
	 * sub packages as well.
	 * </p>
	 * @parameter
	 */
	String packages;

	public void execute() throws MojoExecutionException{
		final Log log = getLog();
		if(classSource==null)
			classSource="target/classes";
		if(classDestination==null)
			classDestination=classSource;
		File f = new File("");
		log.info("Current Directory: "+f.getAbsolutePath());

		StringBuilder extraClassPath = new StringBuilder();
		extraClassPath.append(classSource);
		if (classpath != null)
		{
			if (!extraClassPath.toString().endsWith(";"))
			{
				extraClassPath.append(";");
			}
			extraClassPath.append(classpath);
		}
		Transformer t = new Transformer(extraClassPath.toString(), transformArgs);
		ClassLoader cl = MavenEnhanceTask.class.getClassLoader();
		log.info("classSource="+classSource+"  transformArgs="+transformArgs
			+"  classDestination="+classDestination+"  packages="+packages);
		OfflineFileTransform ft = new OfflineFileTransform(t, cl, classSource, classDestination);
		ft.setListener(new TransformationListener() {
			
			public void logEvent(String msg) {
				log.info(msg);
			}
			
			public void logError(String msg) {
			}
		});
		ft.process(packages);
	}
}