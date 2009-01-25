package com.avaje.ebean.enhance.maven;

import java.io.File;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

import com.avaje.ebean.enhance.agent.Transformer;
import com.avaje.ebean.enhance.ant.AntEnhanceTask;
import com.avaje.ebean.enhance.ant.OfflineFileTransform;

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
 *    &lt;plugin>
 *      &lt;groupId>org.avaje&lt;/groupId>
 *      &lt;artifactId>ebean&lt;/artifactId>
 *      &lt;version>1.0.2-SNAPSHOT&lt;/version>
 *      &lt;executions>
 *        &lt;execution>
 *          &lt;id>compile&lt;/id>
 *          &lt;phase>compile&lt;/phase>
 *          &lt;goals>
 *            &lt;goal>enhance&lt;/goal>
 *          &lt;/goals>
 *        &lt;/execution>
 *      &lt;/executions>
 *      &lt;configuration>
 *        &lt;classSource>target/classes&lt;/classSource>
 *        &lt;packages>com.avaje.ebean.meta.**, com.acme.myapp.entity.**&lt;/packages>
 *        &lt;transformArgs>debug=1&lt;/transformArgs>
 *      &lt;/configuration>
 *    &lt;/plugin&gt;
 * </pre>
 * @author Paul Mendelson
 * @version $Revision$, $Date$
 * @since 1.0.3, Jan, 2009
 * @see com.avaje.ebean.enhance.ant.AntEnhanceTask
 * @goal enhance
 */
public class EnhanceTask extends AbstractMojo {

	/**
	 * Set the directory holding the class files we want to transform.
	 * @parameter
	 */
	String classSource;

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
		File f = new File("");
		Log log = getLog();
		log.info("Current Directory: "+f.getAbsolutePath());
		Transformer t = new Transformer(classSource, transformArgs);
		ClassLoader cl = AntEnhanceTask.class.getClassLoader();
		log.info("classSource="+classSource+"  transformArgs="+transformArgs
			+"  classDestination="+classDestination+"  packages="+packages);
		OfflineFileTransform ft = new OfflineFileTransform(t, cl, classSource, classDestination);
		ft.process(packages);
	}
}