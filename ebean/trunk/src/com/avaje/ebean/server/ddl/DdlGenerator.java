package com.avaje.ebean.server.ddl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.PersistenceException;

import com.avaje.ebean.Transaction;
import com.avaje.ebean.server.core.InternalEbeanServer;
import com.avaje.ebean.server.plugin.PluginProperties;

public class DdlGenerator {

	final InternalEbeanServer server;
	final PluginProperties properties;

	PrintStream out = System.out;

	int summaryLength = 40;

	boolean debug = true;

	String dropContent;
	String createContent;
	String dropFile;
	String createFile;

	public DdlGenerator(InternalEbeanServer server) {
		this.server = server;
		this.properties = server.getPlugin().getDbConfig().getProperties();
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
		if (properties.getPropertyBoolean("ddl.generate", true)) {
			writeDrop();
			writeCreate();
		}
	}
	
	/**
	 * Run the DDL drop and DDL create scripts if properties have been set.
	 */
	public void runDdl() {
		
		boolean runBoth = properties.getPropertyBoolean("ddl.run", true);
		boolean runDrop = properties.getPropertyBoolean("ddl.runDrop", runBoth);
		boolean runCreate = properties.getPropertyBoolean("ddl.runCreate", runBoth);
		
		if (runDrop) {
			runScript(true, dropContent);
		}
		if (runCreate){
			runScript(false, createContent);
		}
	}

	public void writeDrop() {

		try {
			String c = genDropDdl();
			dropFile = getDropFile();
			writeFile(dropFile, c);

		} catch (IOException e) {
			String msg = "Error generating Drop DDL";
			throw new PersistenceException(msg, e);
		}
	}

	public void writeCreate() {

		try {
			String c = genCreateDdl();
			createFile = getCreateFile();
			writeFile(createFile, c);

		} catch (IOException e) {
			String msg = "Error generating Create DDL";
			throw new PersistenceException(msg, e);
		}
	}

	public String genDropDdl() {

		DdlGenContext ctx = createContext();

		DropTableVisitor drop = new DropTableVisitor(ctx);
		VisitorUtil.visit(server, drop);

		dropContent = ctx.getContent();
		return dropContent;
	}

	public String genCreateDdl() {

		DdlGenContext ctx = createContext();
		CreateTableVisitor create = new CreateTableVisitor(ctx);

		VisitorUtil.visit(server, create);

		AddForeignKeysVisitor fkeys = new AddForeignKeysVisitor(ctx);
		VisitorUtil.visit(server, fkeys);
		
		createContent = ctx.getContent();
		return createContent;
	}

	protected String getDropFile() {
		String dropFile = properties.getProperty("ddl.dropfile", null);
		if (dropFile != null) {
			return dropFile;
		}
		return server.getName() + "-drop.sql";
	}

	protected String getCreateFile() {
		String file = properties.getProperty("ddl.createfile", null);
		if (file != null) {
			return file;
		}
		return server.getName() + "-create.sql";
	}

	protected DdlGenContext createContext() {
		return server.getPlugin().getDbConfig().getDbSpecific().createDdlGenContext();
	}

	protected void writeFile(String fileName, String fileContent) throws IOException {

		File f = new File(fileName);

		FileWriter fw = new FileWriter(f);
		fw.write(fileContent);
		fw.flush();
		fw.close();
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

		} finally {
			t.end();
		}
	}

	/**
	 * Execute the list of statements.
	 */
	private void runStatements(boolean expectErrors, List<String> statements, Connection c) {

		for (int i = 0; i < statements.size(); i++) {
			String oneOf = (i + 1) + " of " + statements.size();
			runStatement(expectErrors, oneOf, statements.get(i), c);
		}
	}

	/**
	 * Execute the statement.
	 */
	private void runStatement(boolean expectErrors, String oneOf, String stmt, Connection c) {

		try {

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
				sb.append(s);
				if (s.endsWith(";")) {
					statements.add(sb.toString().trim());
					sb = new StringBuilder();
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
