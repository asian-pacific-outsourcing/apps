package com.apo.apps.RawAdmin;
/********************************************************************
* @(#)CheckInParser.java 1.00 20100523
* Copyright © 2010-2013 by Richard T. Salamone, Jr. All rights reserved.
*
* CheckInParser: Parses lines of the csv file containing dispos
* and discovered mobile numbers for contacts called by TQers.
*
* This differs from a straight update in that blank entries are not
* erased from the database. Additionally, the HTR count is updated
* as necessary. Finally, the checked out to page is always set to
* zero (thereby, checking the contact back in.
*
* @author Rick Salamone
* @version 2.04
* 20100523 rts created bulk update class
* 20100810 rts now background via FileWorker
* 20100829 rts ignores blank fields, increments htr count
* 20100924 rts ignores blank trailing column headers
* 20101006 rts records touches
* 20101011 rts uses touch fail to identify duplicate id in file
* 20110606 rts endProcessing declares exception
* 20130216 rts modified imports
*******************************************************/
import com.apo.admin.TouchDB;
import com.apo.contact.Raw;
import com.apo.contact.touch.TouchCode;
import com.shanebow.dao.ContactID;
import com.shanebow.dao.DataField;
import com.apo.contact.Dispo;
import com.shanebow.dao.DataFieldException;
import com.shanebow.util.CSV;
import com.shanebow.dao.DBStatement;
import com.shanebow.tools.fileworker.FileLineParser;
import com.shanebow.ui.SBDialog;
import com.shanebow.util.MessageLogger;
import com.shanebow.util.SBLog;
import java.sql.SQLException;

final class CheckInParser
	implements FileLineParser
	{
	// @TODO: use login name to obtain touch.employeeID
private static final short TOUCH_EMPLOYEE=(short)19;
	private static final String MODULE="Check In Parser";
	private static final String ID_MUST_BE_FIRST="First column must be 'id'";
	private static final String BAD_FIELD="Unrecognized field name in header";
	private static final String AT_LEAST_TWO_FIELDS="File must conatain 'id' and"
	                                               + " at least one other column";
	private static final String STMT_PREFIX
		= "UPDATE " + Raw.DB_TABLE + " SET page=0";
	private static final String STMT_POSTFIX
		= " WHERE " + Raw.dbField(Raw.ID) + " = ";
	private static final String STMT_INC_HTR
		= "," + Raw.dbField(Raw.HTRCOUNT)
		+ " = " 	+ Raw.dbField(Raw.HTRCOUNT) + " + 1";

	private int            m_csvColumns = 0;
	private int[]          m_fields;       // array of field indicies
	private String[]       m_headers;      // the db field names for m_fields
	private int            m_errCount = 0; // running count of errors/exceptions
	private DBStatement    db = null;
	private MessageLogger  logTrace = null;
	private long when = 0;

	CheckInParser(MessageLogger logTrace)
		{
		this.logTrace = logTrace;
		}
 
	public int getErrorCount()     { return m_errCount; }
	public int getDuplicateCount() { return FileLineParser.DUPS_NA; }

	private void appLog ( String format, Object... args )
		{
		String msg = String.format( format, args );
		SBLog.write( MODULE, msg );
		}

	private void trace ( String format, Object... args )
		{
		logTrace.write( String.format( format, args ));
		}

	public boolean checkHeaders( String text )
		{
		m_csvColumns = CSV.columnCount(text);
		m_headers = CSV.split(text, m_csvColumns);

		// ignore any blank columns at end - Excel macros can cause problems
		while ((m_csvColumns > 0) && m_headers[m_csvColumns - 1].isEmpty())
			--m_csvColumns;

		if ( m_csvColumns < 2 )
			return SBDialog.error( MODULE, AT_LEAST_TWO_FIELDS );

		String idFieldName = Raw.dbField(Raw.ID);
		if ( !m_headers[0].equalsIgnoreCase(idFieldName))
			return SBDialog.error( MODULE, ID_MUST_BE_FIRST + "\nfound: '" + m_headers[0] + "'" );

		m_fields = new int[m_csvColumns];
		for ( int i = 1; i < m_csvColumns; i++ )
			{
			int fieldNumber = Raw.dbFieldIndex( m_headers[i] );
			if ( fieldNumber == -1 ) // not found
				return SBDialog.error( MODULE, BAD_FIELD + ": '" + m_headers[i] + "'" );

			m_fields[i] = fieldNumber;
			}
		return true;
		}

	public void beginProcessing() throws Exception
		{
		db = new DBStatement();
		when = com.shanebow.util.SBDate.timeNow();
		}

	public void endProcessing(int line, int totalLines )
		throws Exception
		{
		if ( db != null ) db.close();
		String msg = "Bulk Update"
		           + "\n*** Processed " + line + " of " + totalLines + " lines"
		           + "\n*** Errors: " + m_errCount;
		appLog(msg);
		}

	public void processLine(int line, String text) throws Exception
		{
		try { _processLine( line, text.trim()); }
		catch (DataFieldException e)
			{
			trace( "Line #%d, Error #%d: %s", line, ++m_errCount, e.toString());
			}
		catch (Exception t)
			{
			++m_errCount;
			trace( "Line #%d, Error #%d: %s", line, m_errCount, t.toString());
			trace( "csv: " + text + "\nlast sql statement: " + db.getSQL()
					         + "\nTry cleaning file and rerun import" );
			throw t;
			}
		}

	private void _processLine( int line, String text )
		throws DataFieldException, SQLException
		{
		if ( text.isEmpty())
			throw new DataFieldException( "Blank line" );
		String[] pieces = CSV.split(text, m_csvColumns );
		ContactID contactID = ContactID.parse(pieces[0]);
		Dispo dispo = Dispo.XX;
		String stmt = STMT_PREFIX;
		for ( int i = 1; i < m_csvColumns; i++ )
			{
			if (m_fields[i] == Raw.PAGE)
				continue; // page is always set to 0
			else
				{
				String trimmed = pieces[i].trim();
				DataField value = Raw.parse(m_fields[i], trimmed );
				if ( m_fields[i] == Raw.DISPO )
					{
					dispo = (Dispo)value;
					if (dispo.isHTR())
						stmt += STMT_INC_HTR;
					}
				else if ( trimmed.isEmpty()) // don't update field if blank
					continue;
				stmt += ", " + m_headers[i] + " = " + value.dbRepresentation();
				}
			}
		try { TouchDB.add( db, TouchCode.CHECKIN,  contactID, when, TOUCH_EMPLOYEE, dispo.toString()); }
		catch (SQLException e)
			{
			throw new DataFieldException("id " + contactID
				+ " appears multiple times in file" );
			}
		stmt += STMT_POSTFIX + contactID.dbRepresentation();
//		trace( stmt );
		db.executeUpdate( stmt );
//		TouchDB.add( db, TouchCode.CHECKIN,  contactID, when, "19", "" );
		}
	}
