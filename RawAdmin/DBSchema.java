package com.apo.apps.RawAdmin;
/********************************************************************
* @(#)DBSchema.java 1.00 20100407
* Copyright (c) 2010 by Richard T. Salamone, Jr. All rights reserved.
*
* DBSchema: Contains database table definitions and methods to create
* and drop tables using SQL commands.
*
* @author Rick Salamone
* @version 1.00 20100407 RTS
* @version 1.01 20100615 RTS renamed to DBSchema
* @version 1.02 20101003 RTS added altPhone to contact schema
* @version 1.03 20101003 RTS added touch schema
*******************************************************/
import com.shanebow.dao.DBStatement;
import com.shanebow.util.SBLog;
import java.sql.SQLException;

public final class DBSchema
	{
	public static final String TBL_LOOKUP_SCHEMA
			= "   id           long         NOT NULL," // IDENTITY(1,1),";
			+ "   value        varchar(?)   NOT NULL,"
			+ "   CONSTRAINT testPK PRIMARY KEY(id)";

	public static final String TBL_TOUCH_SCHEMA
			= "   contactID    long         NOT NULL,"
			+ "   when         long         NOT NULL,"
			+ "   employeeID   short        NOT NULL,"
			+ "   touchCode    byte         NOT NULL,"
			+ "   details      varchar(255) NULL,"
			+ "   CONSTRAINT testPK PRIMARY KEY(contactID,when,employeeID)";

	public static final String TBL_CONTACT_SCHEMA // =  "CREATE TABLE " + tblName + "(";
			= "   id           long         NOT NULL," // IDENTITY(1,1),";
			+ "   name         varchar(50)  NOT NULL,"
			+ "   position     varchar(30)  NULL,"
			+ "   company      varchar(30)  NULL,"
			+ "   phone        varchar(16)  NULL,"
			+ "   mobile       varchar(16)  NULL,"
			+ "   altphone     varchar(16)  NULL,"
			+ "   email        varchar(100) NULL,"
			+ "   address      varchar(80)  NULL,"
			+ "   countryID    long         NULL,"
			+ "   type         char(1)      NOT NULL,"
			+ "   disposition  short        NOT NULL,"
			+ "   sourceID     long         NOT NULL,"
			+ "   regionID     short        NULL,"
			+ "   noAnswer     short        NULL,"
			+ "   callBack     long         NULL,"
			+ "   page         long         NULL,"
			+ "   website      varchar(100) NULL,"
			+ "   CONSTRAINT testPK PRIMARY KEY(id)";

	public static final String TBL_ORDER_SCHEMA // =  "CREATE TABLE " + tblName + "(";
			= "   id           long         NOT NULL," // IDENTITY(1,1),";
			+ "   rawID        long         NOT NULL,"
			+ "   open         long         NOT NULL,"
			+ "   symbol       varchar(20)  NOT NULL,"
			+ "   qty          short        NOT NULL,"
			+ "   cost         short        NOT NULL,"
			+ "   aoID         long         NOT NULL,"
			+ "   loID         long         NOT NULL,"
			+ "   status       short        NOT NULL,"
			+ "   activity     long         NOT NULL,"
			+ "   notes        varchar(255) NULL,"
			+ "   commission   short        NOT NULL,"
			+ "   fees         short        NOT NULL,"
			+ "   CONSTRAINT testPK PRIMARY KEY(id)";

	public static final boolean dropTable(String tblName)
		throws SQLException
		{
		DBStatement db = new DBStatement();
		try { db.executeUpdate("DROP TABLE " + tblName + ";"); }
		catch (SQLException e) { return false; }
		finally { if (db != null) db.close(); }
		return true;
		}

	public static final boolean createTable(String tblName, String schema, Object... args )
		throws SQLException
		{
		DBStatement db = new DBStatement();
		try
			{
			String sql =  "CREATE TABLE " + tblName + "(" + schema + ");";
			int count =  db.executeUpdate( sql, args );
			log("Created table " + tblName + ": " + count );
			}
		catch ( Exception e ) { return false; }
		finally { if (db != null) db.close(); }
		return true;
		}

	private static void log ( String msg )
		{
		SBLog.write ( SBLog.APP, "DBSchema", msg );
		}
	}
