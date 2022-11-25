package com.apo.apps.RawAdmin;
/********************************************************************
* @(#)FileFlavor.java 1.00 20100807
* Copyright 2010 by Richard T. Salamone, Jr. All rights reserved.
*
* FileFlavor: Determines the type of an apo csv file based on the
* contents of the first line (the header). Required for dnd.
*
* @author Rick Salamone
* @version 1.00 20100807 created from the APO import
* @version 1.01 20101004 modified to handle new import added restore
* @version 1.02 20101121 added old import format
* @version 1.03 20101121 added master bobby
*******************************************************/
import com.apo.contact.Raw;
import com.shanebow.util.CSV;
import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class FileFlavor
	{

	public static final String HEAD_CALL_LOG=
		"\"Account\",\"From\",\"To\",\"Country\",\"Description\",\"Connect Time\","
	 + "\"Charged Time, min:sec\",\"Charged Time, sec.\",\"Charged Amount, USD\"";
	public static final String OLD_HEAD_IMPORT_CONTACTS=
		"id,name,position,company,phone,mobile,email,address,website,"
		+ "countryID,type,disposition,sourceID,regionID,noAnswer,callBack,page";

	public static final String HEAD_MB
		= "Stg,Front Sent,AO Name,AO Last Name,AOI1,AOI2,Cape ID,Select,I.D.,Title,"
		+ "First,Last,TQ,Verifier,Email,Position ,Company,Address,City,postal code,"
		+ "Country,Region,Nationality,Telephone,Alternate,Fax,Mobile,Qualifier,"
		+ "Verification,Date Created,500k Rand/Yr,Emailed";

	public static final String HEAD_IMPORT_CONTACTS = Raw.DMCSV_HEADER;
	public static final String HEAD_RESTORE_CONTACTS = Raw.ID_HEADER;
	public static final String HEAD_UPDATE_CONTACTS = "id,";

	public static final FileFlavor UNKNOWN
	  = new FileFlavor("Unrecognized File Type", null);
	public static final FileFlavor OLD_IMPORT_CONTACTS
	  = new FileFlavor("Old Import Contacts", OLD_HEAD_IMPORT_CONTACTS);
	public static final FileFlavor IMPORT_CONTACTS
	  = new FileFlavor("Import Contacts", HEAD_IMPORT_CONTACTS);
	public static final FileFlavor RESTORE_CONTACTS
	  = new FileFlavor("Restore Contacts", HEAD_RESTORE_CONTACTS);
	public static final FileFlavor UPDATE_CONTACTS
	  = new FileFlavor("Update Contacts", HEAD_UPDATE_CONTACTS);
	public static final FileFlavor IMPORT_CALL_LOG
	  = new FileFlavor("Import Call Log", HEAD_CALL_LOG);
	public static final FileFlavor MASTER_BOBBY
	  = new FileFlavor("Master Bobby", HEAD_MB);

	public static FileFlavor discern(File file)
		{
		BufferedReader stream = null;
		try
			{
			stream = new BufferedReader(new FileReader(file));
			String header = stream.readLine();

			if ( header.toLowerCase().equals(HEAD_IMPORT_CONTACTS.toLowerCase()))
				return IMPORT_CONTACTS;

			if ( header.toLowerCase().startsWith(HEAD_MB.toLowerCase()))
				return MASTER_BOBBY;

			if ( header.equals(HEAD_CALL_LOG))
				return IMPORT_CALL_LOG;

			header = header.toLowerCase();
			// distinguish between contact export/restore and contact update files
			if ( !header.startsWith(HEAD_UPDATE_CONTACTS.toLowerCase()))
				return UNKNOWN; // both types start with "id,"

			// read the 1st field in 1st data line to see if it has a valid id
			String dataLine = stream.readLine();
			String idString = (dataLine == null)? "" // no data
			                 : CSV.split(dataLine,1)[0];
			if ( idString.isEmpty()) // must be blank id in import files
				{
				if ( header.startsWith(HEAD_RESTORE_CONTACTS.toLowerCase()))
					return IMPORT_CONTACTS;
				if ( header.startsWith(OLD_HEAD_IMPORT_CONTACTS.toLowerCase()))
					return OLD_IMPORT_CONTACTS;
				}

			Long.parseLong(idString); // if exception, then invalid id
			return UPDATE_CONTACTS;
			}
		catch ( Exception e )
			{
			// com.shanebow.util.SBLog.write( "Exception: " + e );
			return UNKNOWN;
			}
		finally
			{
			try { if ( stream != null ) stream.close(); }
			catch (Exception e) {}
			}
		}

	private String m_title;
	private String m_header;
	private FileFlavor( String title, String header )
		{
		m_title = title;
		m_header = header;
		}

	public final String toString()  { return m_title; }
	public final String getTitle()  { return m_title; }
	public final String getHeader() { return m_header; }
	}
