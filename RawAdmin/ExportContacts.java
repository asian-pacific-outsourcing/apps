package com.apo.apps.RawAdmin;
/********************************************************************
* @(#)ImportContacts.java 1.00 20100531
* Copyright 2010 by Richard T. Salamone, Jr. All rights reserved.
*
* Export Contacts: Dumps the contact database into a csv file.
*
* @author Rick Salamone
* @version 1.00 20100531
* @version 1.01 RTS 20100822 now promts for save file
* @version 1.02 RTS 20101003 checks null columns handles any table
*******************************************************/
import com.apo.contact.DlgFile;
import com.apo.contact.*;
import com.shanebow.dao.DBStatement;
import com.shanebow.ui.SBDialog;
import com.shanebow.util.SBLog;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

public class ExportContacts
	{
	public static void prompt()
		{
		File file = DlgFile.get(DlgFile.FT_EXPORT_CONTACTS);
		if ( file != null )
			writeCSV(file, Raw.DB_TABLE);
		}

	private static synchronized boolean writeCSV( final File file, String table )
		{
		String stmt = "SELECT * FROM " + table + " ORDER BY id";
		PrintWriter csv = null;
		DBStatement db = null;
		ResultSet rs = null;
		ResultSetMetaData meta = null;
		int total = 0;
		try
			{
			csv = new PrintWriter ( file );
			db = new DBStatement();
			rs = db.executeQuery( stmt );
			meta = rs.getMetaData();
			int numberOfColumns =  meta.getColumnCount();

			// Get the column names and print them.
			// Then we can close the connection.
			for ( int column = 1; column <= numberOfColumns; column++ )
				csv.print(((column>1)?",":"") + meta.getColumnLabel(column));
			csv.println();

			while ( rs.next())
				{
				for ( int column = 1; column <= numberOfColumns; column++ )
					csv.print(((column>1)?",":"") + quoted(rs.getString(column)));
				csv.println();
				++total;
				}
			}
		catch (IOException e) { return SBDialog.error( "IO Error", e.getMessage()); }
		catch (SQLException e) { return SBDialog.error( "DB Error", e.getMessage()); }
		finally
			{
			try
				{
				if ( csv != null ) csv.close();
				if (  rs != null )  rs.close();
				if (  db != null )  db.close();
				}
			catch ( Exception e ) {}
			}
		SBDialog.inform ( "Export Successful",
		                  "Exported " + total + " records to " + file );
		return true;
		}

	private static String quoted(String s)
		{
		return (s==null) ? "" : ("\"" + s + "\"");
		}
	}
