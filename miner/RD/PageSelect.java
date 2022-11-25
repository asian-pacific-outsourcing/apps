package com.apo.apps.miner.RD;
/********************************************************************
* @(#)_RawDispo.java	1.00 10/06/02
* Copyright (c) 2010 by Richard T. Salamone, Jr. All rights reserved.
*
* PageSelect: Component to allow user to select a checked out page of
* records from the customer database.
*
* @version 1.00 06/02/10
* @author Rick Salamone
* 20100610 RTS 1.01 reset remembers previous index & reselects it
*******************************************************/
// import com.shanebow.dao.DBStatement;
import com.apo.contact.Raw;
import com.shanebow.ui.SBDialog;
// import java.sql.*;
import javax.swing.JComboBox;

final public class PageSelect extends JComboBox
	{
	public PageSelect()
		{
		super();
		reset();
		}

	public void reset ()
		{
	SBDialog.fatalError("PageSelect MUST REIMPLEMENTED TO USE DAO");
	/*******************
	// Note: this code works fine if the app has direct access to the db
		String sql = "SELECT DISTINCT page"
		           + " FROM " + Raw.DB_TABLE
		           + " WHERE page <> 0"
		           + " ORDER BY page;";
		DBStatement db = null;
		ResultSet rs = null;
		int selectIdx = 0;
		if ( getItemCount() > 0 )
			{
			selectIdx = getSelectedIndex();
			removeAllItems();
			}
		try
			{
			db = new DBStatement();
			rs = db.executeQuery(sql);
			while ( rs.next())
				addItem(rs.getString(1));
			if ( selectIdx < getItemCount())
				setSelectedIndex(selectIdx);
			}
		catch ( SQLException e )
			{
			SBDialog.error( "Database Error", e.getMessage()
			                + "\nExiting" );
			System.exit(1);
			}
		finally { if ( db != null ) { db.closeResultSet(rs); db.close(); }}
	*******************/
		}
	}
