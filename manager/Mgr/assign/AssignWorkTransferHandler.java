package com.apo.apps.manager.Mgr.assign;
/********************************************************************
* @(#)AssignWork.java 1.00 2010
* Copyright 2010 by Richard T. Salamone, Jr. All rights reserved.
* Copyright (c) 1995 - 2008 Sun Microsystems, Inc.  All rights reserved.
*
* AssignWork.java: Allows a manager to (re)assign contacts to the
* salesmen.
*
* @author Rick Salamone
* @version 1.00, 20110214 rts initial demo
*******************************************************/
import com.apo.contact.Raw;
import com.apo.contact.report.RawTableModel;
import com.apo.contact.Dispo;
import com.apo.contact.Source;
import com.apo.employee.*;
import com.apo.net.Access;
import java.util.*;
//import java.awt.*;
//import java.awt.event.*;
//import java.text.*;
import java.awt.datatransfer.*;
import javax.swing.*;

public class AssignWorkTransferHandler
	extends TransferHandler
	{
	static JLabel _statusBar = new JLabel("<HTML><B><I>To begin, double click on the user role you are assigning..." );
	static JLabel statusBar() { return _statusBar; }
	boolean sayError(String msg)
		{
		displayDropLocation( "<HTML><FONT COLOR=RED><B>" + msg );
		return false;
		}

	protected final void displayDropLocation(final String string)
		{
		SwingUtilities.invokeLater(new Runnable()
			{
			public void run() { _statusBar.setText(string); }
			});
		}

	protected final Raw[] getTransferContacts(TransferSupport info)
		{
		// Get the string that is being dropped.
		Transferable t = info.getTransferable();
		try
			{
			String data = (String)t.getTransferData(DataFlavor.stringFlavor);
			String[] csvs = data.split("\n");
			if ( csvs.length > 100 )
				{
				sayError( "Exceeded limit of 100 contacts per assignment!" );
				return null;
				}
			Raw[] contacts = new Raw[csvs.length];
			for ( int i = 0; i < csvs.length; i++ )
				contacts[i] = new Raw(csvs[i]);
			return contacts;
			}
		catch (Exception e) { return null; }
		}

	protected final boolean changeAssignments(Raw[] aRaws, Source aNewOwner)
		{
		// Display a dialog with the drop information.
		Source currOwner = aRaws[0].assignedTo();
		String msg = String.format("Changing %d contact assignments from %s, to %s",
			 aRaws.length,
			(currOwner.isEmpty()? "unassigned" : "assigned to " + currOwner),
			(aNewOwner.isEmpty()? "unassigned" : "assigned to " + aNewOwner));
		displayDropLocation(msg);

		try
			{
			StringBuffer csvIDs = new StringBuffer();
			for ( Raw raw : aRaws )
				csvIDs.append(raw.id().toString() + ",");
			csvIDs.deleteCharAt(csvIDs.length() - 1);
			Raw.DAO.assign( aNewOwner, Access.usrID(), csvIDs.toString());

			for ( int i = 0; i < aRaws.length; i++ )
				{
				Dispo d = aRaws[i].dispo();
				aRaws[i] = new Raw(aRaws[i], aNewOwner,
					(d==Dispo.TOL || d==Dispo.KOL || d==Dispo.CO)? Dispo.ACB : null, null);
				}

			for ( Raw raw : aRaws )
				RawTableModel.updateAll(raw);

			return true;
			}
		catch (Exception e) { e.printStackTrace(); return sayError("ERROR: " + e.toString()); }
		}
	}