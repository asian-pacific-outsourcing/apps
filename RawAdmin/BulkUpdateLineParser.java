package com.apo.apps.RawAdmin;
/********************************************************************
* @(#)BulkUpdateLineParser.java 1.00 20100523
* Copyright © 2010-2013 by Richard T. Salamone, Jr. All rights reserved.
*
* BulkUpdateLineParser: Parses lines of the csv file containing
* telephone records (from the phone company), to create a new
* call record in the database for each.
*
* @author Rick Salamone
* @version 1.00 05/23/10
* 20100523 rts created
* 20100810 rts now background via FileWorker
* 20100923 rts ignores blank columns at end
* 20110606 rts endProcessing declares exception
* 20130216 rts modified imports
*******************************************************/
import com.apo.contact.Raw;
import com.shanebow.dao.DataField;
import com.shanebow.dao.DataFieldException;
import com.shanebow.util.CSV;
import com.shanebow.dao.DBStatement;
import com.shanebow.tools.fileworker.FileLineParser;
import com.shanebow.ui.SBDialog;
import com.shanebow.util.MessageLogger;
import com.shanebow.util.SBLog;
import java.sql.SQLException;

final class BulkUpdateLineParser
	implements FileLineParser
	{
	private static final String MODULE="Bulk Update";
	private static final String ID_MUST_BE_FIRST="First column must be 'id'";
	private static final String BAD_FIELD=" Unrecognized field name in header";
	private static final String AT_LEAST_TWO_FIELDS="File must conatain 'id' and"
	                                               + " at least one other column";

	private int            m_csvColumns = 0;
	private int[]          m_fields; // array of field indicies
	private String         m_stmt;  // the SQL UPDATE statement with '?' for values
	private int            m_errCount = 0; // running count of errors/exceptions
	private DBStatement    db = null;
	private MessageLogger  logTrace = null;

	BulkUpdateLineParser(MessageLogger logTrace)
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
		String[] headers = CSV.split(text, m_csvColumns);

		// ignore any blank columns at end - Excel macros can cause problems
		while ((m_csvColumns > 0) && headers[m_csvColumns - 1].isEmpty())
			--m_csvColumns;

		if ( m_csvColumns < 2 )
			return SBDialog.error( MODULE, AT_LEAST_TWO_FIELDS );

		String idFieldName = Raw.dbField(Raw.ID);
		if ( !headers[0].equalsIgnoreCase(idFieldName))
			return SBDialog.error( MODULE, ID_MUST_BE_FIRST + "\nfound: '" + headers[0] + "'" );

		m_fields = new int[m_csvColumns];
		for ( int i = 1; i < m_csvColumns; i++ )
			{
			int fieldNumber = Raw.dbFieldIndex( headers[i] );
			if ( fieldNumber == -1 ) // not found
				return SBDialog.error( MODULE, "Column " + i + BAD_FIELD + ": '" + headers[i] + "'" );

			m_fields[i] = fieldNumber;
			}
		m_stmt = "UPDATE " + Raw.DB_TABLE + " SET";
		for ( int i = 1; i < m_csvColumns; i++ )
			m_stmt += ((i > 1)? ", " : " ") + headers[i] + " = ?";
		m_stmt += " WHERE " + idFieldName + " = ?;";
		return true;
		}

	public void beginProcessing() throws Exception
		{
		db = new DBStatement();
		}

	public void endProcessing(int line, int totalLines )
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
		Object[] args = new DataField[m_csvColumns];
		args[m_csvColumns-1] = Raw.parse(Raw.ID, pieces[0]);
		for ( int i = 0; i < m_csvColumns-1; i++ )
			args[i] = Raw.parse(m_fields[i+1], pieces[i+1]);
		db.executeUpdate( m_stmt, args );
		}
	}
