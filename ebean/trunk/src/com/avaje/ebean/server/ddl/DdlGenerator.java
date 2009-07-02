package com.avaje.ebean.server.ddl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.PrintStream;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.PersistenceException;

import com.avaje.ebean.Transaction;
import com.avaje.ebean.config.ServerConfig;
import com.avaje.ebean.config.dbplatform.DatabasePlatform;
import com.avaje.ebean.config.naming.NamingConvention;
import com.avaje.ebean.server.core.InternalEbeanServer;

/**
 * Controls the generation of DDL and potentially runs the resulting scripts.
 */
public class DdlGenerator {

	final InternalEbeanServer server;

	final DatabasePlatform dbPlatform;

	PrintStream out = System.out;

	int summaryLength = 80;

	boolean debug = true;

	boolean generateDdl;
	boolean runDdl;

	String dropContent;
	String createContent;
	String dropFile;
	String createFile;

	NamingConvention namingConvention;

	public DdlGenerator(InternalEbeanServer server, DatabasePlatform dbPlatform, ServerConfig serverConfig) {
		this.server = server;
		this.dbPlatform = dbPlatform;
		this.generateDdl = serverConfig.isDdlGenerate();
		this.runDdl = serverConfig.isDdlRun();
		this.namingConvention = serverConfig.getNamingConvention();
	}

	/**
	 * Generate the DDL and then run the DDL based on property settings (ebean.ddl.generate and ebean.ddl.run etc).
	 */
	public void execute() {
		generateDdl();
		runDdl();
	}

	/**
	 * Generate the DDL drop and create scripts if the properties have been set.
	 */
	public void generateDdl() {
		if (generateDdl) {
			writeDrop(getDropFileName());
			writeCreate(getCreateFileName());
		}
	}

	/**
	 * Run the DDL drop and DDL create scripts if properties have been set.
	 */
	public void runDdl() {

		if (runDdl) {
			try {
				if (dropContent == null){
					dropContent = readFile(getDropFileName());
				}
				if (createContent == null){
					createContent = readFile(getCreateFileName());
				}
				runScript(true, dropContent);
				runScript(false, createContent);

			} catch (IOException e){
				String msg = "Error reading drop/create script from file system";
				throw new RuntimeException(msg, e);
			}
		}
	}

	protected void writeDrop(String dropFile) {

//		String c = generateDropDdl();
		try {
			String c = generateDropDdl();
			writeFile(dropFile, c);

		} catch (IOException e) {
			String msg = "Error generating Drop DDL";
			throw new PersistenceException(msg, e);
		}
	}

	protected void writeCreate(String createFile) {

//		String c = generateCreateDdl();
		try {
			String c = generateCreateDdl();
			writeFile(createFile, c);

		} catch (IOException e) {
			String msg = "Error generating Create DDL";
			throw new PersistenceException(msg, e);
		}
	}

	public String generateDropDdl() {

		DdlGenContext ctx = createContext();

		DropTableVisitor drop = new DropTableVisitor(ctx);
		VisitorUtil.visit(server, drop);

		DropSequenceVisitor dropSequence = new DropSequenceVisitor(ctx);
		VisitorUtil.visit(server, dropSequence);


		dropContent = ctx.getContent();
		return dropContent;
	}

	public String generateCreateDdl() {

		DdlGenContext ctx = createContext();
		CreateTableVisitor create = new CreateTableVisitor(ctx);
		VisitorUtil.visit(server, create);

		CreateSequenceVisitor createSequence = new CreateSequenceVisitor(ctx);
		VisitorUtil.visit(server, createSequence);

		AddForeignKeysVisitor fkeys = new AddForeignKeysVisitor(ctx);
		VisitorUtil.visit(server, fkeys);

		createContent = ctx.getContent();
		return createContent;
	}

	protected String getDropFileName() {
		return server.getName() + "-drop.sql";
	}

	protected String getCreateFileName() {
		return server.getName() + "-create.sql";
	}

	protected DdlGenContext createContext() {
		return new DdlGenContext(dbPlatform.getDbTypeMap(), dbPlatform.getDbDdlSyntax(), namingConvention);
	}

	protected void writeFile(String fileName, String fileContent) throws IOException {

		File f = new File(fileName);

		FileWriter fw = new FileWriter(f);
		try {
			fw.write(fileContent);
			fw.flush();
		} finally {
			fw.close();
		}
	}

	protected String readFile(String fileName) throws IOException {

		File f = new File(fileName);
		if (!f.exists()){
			return null;
		}

		StringBuilder buf = new StringBuilder();

		FileReader fr = new FileReader(f);
		LineNumberReader lr = new LineNumberReader(fr);
		try {
			String s = null;
			while ((s = lr.readLine()) != null){
				buf.append(s).append("\n");
			}
		} finally {
			lr.close();
		}

		return buf.toString();
	}

	/**
	 * Execute all the DDL statements in the script.
	 */
	public void runScript(boolean expectErrors, String content) {

		StringReader sr = new StringReader(content);
		List<String> statements = parseStatements(sr);

		Transaction t = server.createTransaction();
		try {
			Connection connection = t.getConnection();

			out.println("runScript");
			out.flush();

			runStatements(expectErrors, statements, connection);

			out.println("... end of script");
			out.flush();

			t.commit();

		} catch (Exception e){
			String msg = "Error runing script";
			throw new PersistenceException(msg, e);
		} finally {
			t.end();
		}
	}

	/**
	 * Execute the list of statements.
	 */
	private void runStatements(boolean expectErrors, List<String> statements, Connection c) {

		for (int i = 0; i < statements.size(); i++) {
			String xOfy = (i + 1) + " of " + statements.size();
			runStatement(expectErrors, xOfy, statements.get(i), c);
		}
	}

	/**
	 * Execute the statement.
	 */
	private void runStatement(boolean expectErrors, String oneOf, String stmt, Connection c) {

		try {

			// trim and remove trailing ; or /
			stmt = stmt.trim();
			if (stmt.endsWith(";")){
				stmt = stmt.substring(0, stmt.length()-1);
			} else if (stmt.endsWith("/")) {
				stmt = stmt.substring(0, stmt.length()-1);
			}

			if (debug) {
				out.println("executing "+oneOf+" "+ getSummary(stmt));
				out.flush();
			}

			PreparedStatement pstmt = c.prepareStatement(stmt);
			pstmt.execute();

		} catch (Exception e) {
			if (expectErrors){
				out.println(" ... ignoring error executing "+getSummary(stmt)+"  error: "+e.getMessage());
				out.flush();
			} else {
				String msg = "Error executing " + stmt;
				throw new RuntimeException(msg, e);
			}
		}
	}

	/**
	 * Break up the sql in reader into a list of statements using the semi-colon
	 * character;
	 */
	protected List<String> parseStatements(StringReader reader) {

		try {
			BufferedReader br = new BufferedReader(reader);

			ArrayList<String> statements = new ArrayList<String>();

			StringBuilder sb = new StringBuilder();
			String s;
			while ((s = br.readLine()) != null) {
				s = s.trim();
				int semiPos = s.indexOf(';');
				if (semiPos == -1) {
					sb.append(s);

				} else if (semiPos == s.length()-1) {
					// semicolon at end of line
					sb.append(s);
					statements.add(sb.toString().trim());
					sb = new StringBuilder();

				} else {
					// semicolon in middle of line
					String preSemi = s.substring(0, semiPos);
					sb.append(preSemi);
					statements.add(sb.toString().trim());
					sb = new StringBuilder();
					sb.append(s.substring(semiPos+1));

				}
			}

			return statements;
		} catch (IOException e) {
			throw new PersistenceException(e);
		}
	}

	private String getSummary(String s){
		if (s.length() > summaryLength){
			return s.substring(0, summaryLength).trim()+"...";
		}
		return s;
	}
}
