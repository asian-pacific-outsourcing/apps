package com.apo.apps.RawAdmin;
/********************************************************************
* @(#)BulkUpdateWiz.java 1.00 20100821
* Copyright © 2010-2013 by Richard T. Salamone, Jr. All rights reserved.
*
* BulkUpdateWiz: Wizzard to prompt user for an csv file containing
* contact updates, then launches a database update in the background.
*
* @author Rick Salamone
* @version 1.00
* 20100821 rts created from call log import
* 20130216 rts modified imports
*******************************************************/
import com.apo.contact.DlgFile;
import com.shanebow.tools.fileworker.DlgProgress;
import com.shanebow.tools.fileworker.FileWorker;
import java.awt.event.ActionEvent;
import java.io.File;
import javax.swing.JButton;

final class BulkUpdateWiz extends DlgProgress
	{
	private FileWorker worker;

	BulkUpdateWiz()
		{
		super("Updating Contacts");
		setCancelable(true);
		}

	BulkUpdateWiz(File file)
		{
		this();
		if (( file != null ) && file.canRead())
			onStart(file);
		}

	public void actionPerformed(ActionEvent e)
		{
		super.actionPerformed(e);
		String cmd = ((JButton)e.getSource()).getText();
		if ( cmd.equals(CMD_CANCEL))
			worker.cancel(true);
		}

	public void onBrowseFile()
		{
		File file = DlgFile.get(DlgFile.FT_UPDATE_CONTACTS);
		if (( file != null ) && file.canRead())
			onStart(file);
		}

	private void onStart(File file)
		{
		try
			{
			log("Updating from " + file.getName());

			worker = new FileWorker(new BulkUpdateLineParser(getLog()), file);
			worker.addPropertyChangeListener( this );
			worker.execute();
			}
		catch ( Exception e)
			{
			log("Failed to launch: " + e.getMessage());
			promptClose();
			return;
			}
		}
	}
