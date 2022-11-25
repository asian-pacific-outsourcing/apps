package com.apo.apps.AppServer;
/********************************************************************
* @(#)Role.java 1.00 20110203
* Copyright (c) 2011 by Richard T. Salamone, Jr. All rights reserved.
*
* RoleFileDAO: Saves and retrieves Role objects from a text file.
*
* @author Rick Salamone
* @version 1.00, 20110329 rts created
*******************************************************/
import com.apo.employee.Role;
import com.shanebow.util.SBLog;
import java.io.*;
import java.util.HashMap;

public final class RoleFileDAO
	{
	private static final String filespec = "roles.dat";
	static void freeze()
		{
		try
			{
			PrintWriter file = new PrintWriter ( filespec );
			file.println ( "#,Name,Nationality,Calling Code,Mail Lead Time,GMT Offset");
			for ( Role role : Role.getAll())
				file.println ( role.marshall());
			file.close();
			}
		catch (Exception e)
			{
			SBLog.write( "Role File", "Error Saving roles to '" + filespec + "': " + e.toString());
			}
		}

	static void freeze(HashMap<Long, String> _roleStrings)
		{
		try
			{
			PrintWriter file = new PrintWriter ( filespec );
			for ( String roleString : _roleStrings.values())
				file.println ( roleString );
			file.close();
			}
		catch (Exception e)
			{
			SBLog.write( "Role File", "Error Saving roles to '" + filespec + "': " + e.toString());
			}
		}

	static HashMap<Long, String> thaw()
		{
		HashMap<Long, String> map = new HashMap<Long, String>();
		BufferedReader reader = null;
		try
			{
			reader = new BufferedReader(new FileReader(filespec));
			reader.readLine(); // ignore header
			String text;
			while ((text = reader.readLine()) != null )
//				unmarshall(text);
System.out.println( "***\n" + text );
			return map;
			}
		catch (Exception e) { System.out.println( filespec + ": " + e ); }
		finally { try { if (reader != null) reader.close(); } catch (Exception e){}}
		return null;
		}
	}
