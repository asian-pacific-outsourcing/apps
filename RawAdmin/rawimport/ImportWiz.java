package com.apo.apps.RawAdmin.rawimport;
/********************************************************************
* @(#)ImportWiz.java 1.00 20100523
* Copyright © 2010-2013 by Richard T. Salamone, Jr. All rights reserved.
* 
* Raw Admin Import Settings: The user interface for importing raw
* contacts from csv into the database.
*
* @author Rick Salamone
* @version 1.00
* 20100603 now passing the default field values to ImportWorker
* 20101004 default field values handled by contact add logic
* 20130216 rts modified imports
*******************************************************/
import com.apo.contact.DlgFile;
import com.shanebow.tools.fileworker.DlgProgress;
import com.shanebow.util.SBLog;
import java.awt.Dimension;
import java.awt.event.*;
import java.io.File;
import java.util.*;
import javax.swing.JButton;
import javax.swing.border.*;

public final class ImportWiz
	extends DlgProgress
	implements ActionListener
	{
	private static final String VIEW_ERRORS = "View Errors";
	JButton btnViewErrors = new JButton(VIEW_ERRORS);

	public ImportWiz()
		{
		super( "Importing Contacts" );
		btnViewErrors.addActionListener(this);
		btnViewErrors.setVisible(false);
		}

	@Override
	protected void promptClose()
		{
		super.promptClose();
		if ( !ImportWorker.logFails.isEmpty())
			btnViewErrors.setVisible(true);
		}

	@Override
	public void actionPerformed(ActionEvent e)
		{
		super.actionPerformed(e);
		String cmd = e.getActionCommand();
		if ( cmd.equals(VIEW_ERRORS))
			ImportFails.launch();
		}

	public void onBrowseFiles()
		{
		File[] files = DlgFile.getFiles(DlgFile.FT_IMPORT_CONTACTS);
		onStart( files );
		}

	public void onStart(File[] files )
		{
		try
			{
			ImportWorker bgImport = new ImportWorker( files );
			bgImport.logTrace = getLog();
			bgImport.addPropertyChangeListener(this);
			bgImport.execute();
			}
		catch ( Exception e) {}
		}
	}
