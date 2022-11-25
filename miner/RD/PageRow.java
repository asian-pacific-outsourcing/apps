package com.apo.apps.miner.RD;
/********************************************************************
* @(#)PageTableModel.java	1.00 10/06/04
* Copyright (c) 2010-2011 by Richard T. Salamone, Jr. All rights reserved.
*
* PageTableModel: Manages the table data displayed for one checked out
* page that is being dispositioned after the TQ process.  Reads the
* contact records on a given page from the data base. User can edit
* selected fields (disposition, callback date, phone #s, email). The
* commit method causes edits to be validated and written back to the
* database.
*
* @author Rick Salamone
* @version 1.00 06/04/10
* @version 2.00 20110316 rts major overhaul to use Raw DAO
*******************************************************/
import com.apo.contact.Raw;
import com.apo.contact.Dispo;
import com.apo.contact.HTRCount;
import com.shanebow.dao.*;
import com.shanebow.ui.SBDialog;
import com.shanebow.util.SBLog;

final class PageRow
	{
	static final ColumnSpec[] COLS =
		{
		// Visible     field,      width, edit,  Class
		new ColumnSpec(Raw.ID,         5, false, ContactID.class ),
		new ColumnSpec(Raw.NAME,      60, false, ContactName.class ),
		new ColumnSpec(Raw.PHONE,     30, true,  PhoneNumber.class ),
		new ColumnSpec(Raw.MOBILE,    30, true,  PhoneNumber.class ),
//		new ColumnSpec(Raw.EMAIL,     50, true,  String.class ),
		new ColumnSpec(Raw.CALLBACK,  20, true,  When.class ),
		new ColumnSpec(Raw.DISPO,    220, true,  Dispo.class ),
		};

	public static int getColumnCount() { return COLS.length; }
	public static String getColumnName(int column)
		{
		return Raw.getLabelText( COLS[column].m_field );
		}
	public static boolean isEditable(int column)
		{
		return COLS[column].m_edit;
		}
	public static Class getColumnClass(int column)
		{
		return COLS[column].m_class;
		}

	private final Raw fUnedited;
	private final DataField[] fEdits = new DataField[Raw.NUM_FIELDS];
	private boolean      m_dirty = false;

	public PageRow( Raw aRaw )
		{
		fUnedited = aRaw;
		for ( int f = 0; f < Raw.NUM_FIELDS; f++ )
			fEdits[f] = aRaw.getDefensiveCopy(f);

		fEdits[Raw.DISPO] = Dispo.XX;
		fEdits[Raw.CALLBACK] = new When();
		fEdits[Raw.PAGE] = CheckOutID.CHECKED_IN;
		}

	public Object getCol( int c )
		{
		return fEdits[COLS[c].m_field];
		}

	public void setValueAt(Object value, int col)
		{
		m_dirty = true;
		int field = COLS[col].m_field;
		System.out.format( "setValueAt(%s, %S) %s\n",
		      value.toString(), Raw.getLabelText(field), value.getClass().getSimpleName());
		if ( value instanceof DataField )
			{
			fEdits[field] = (DataField)value;
			return;
			}
		try { fEdits[field] = Raw.parse(field, value.toString()); }
		catch ( DataFieldException ex ) { SBDialog.inputError(ex.getMessage()); }
		}

	public ContactID getContactID() { return fUnedited.id(); }
	public boolean isDirty() { return m_dirty; }
	public boolean isConsistent()
		{
		if (fEdits[Raw.PHONE].isEmpty() && fEdits[Raw.MOBILE].isEmpty() && fEdits[Raw.ALTPHONE].isEmpty())
			return SBDialog.inputError( "ID #" + fUnedited.id() + ": At least one phone required." );
		return true;
		}

	public Raw getEdits()
		{
		if ( !fEdits[Raw.CALLBACK].isEmpty())
			{
			fEdits[Raw.DISPO] = Dispo.CB;
			if ( ((Dispo)fEdits[Raw.DISPO]).isHTR())
				((HTRCount)fEdits[Raw.HTRCOUNT]).increment();
			}
		else if ( fEdits[Raw.DISPO].isEmpty()) // not dispositioned
			{
			fEdits[Raw.DISPO] = fUnedited.dispo();
			fEdits[Raw.CALLBACK] = fUnedited.callback();
			}
		else if ( ((Dispo)fEdits[Raw.DISPO]).isHTR())
			((HTRCount)fEdits[Raw.HTRCOUNT]).increment();
		return new Raw(fEdits);
		}
	}

class ColumnSpec
	{
	int    m_field;
	int    m_width;
	Class  m_class;
	boolean m_edit;

	public ColumnSpec ( int field, int width, boolean edit, Class c )
		{
		m_field = field;
		m_width = width;
		m_class = c;
		m_edit = edit;
		}
	}
