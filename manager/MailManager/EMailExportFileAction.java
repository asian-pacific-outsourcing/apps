package com.apo.apps.manager.MailManager;
/********************************************************************
* @(#)EMailExportFileAction.java 1.00 20110530
* Copyright (c) 2011 by Richard T. Salamone, Jr. All rights reserved.
*
* EMailExportFileAction: Action to output the results of a QL fetch into a
* file appropriate for iContact.
*
* @author Rick Salamone
* @version 1.00, 20110530 rts created
*******************************************************/
import com.apo.contact.Raw;
import com.apo.contact.report.RawTableModel;
import com.shanebow.ui.LAF;
import com.shanebow.ui.SBDialog;
import com.shanebow.util.SBProperties;
import java.awt.event.ActionEvent;
import java.io.*;
import javax.swing.AbstractAction;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

public final class EMailExportFileAction
	extends AbstractAction
	{
	private final RawTableModel fModel;

	public EMailExportFileAction( RawTableModel aModel )
		{
		super( "iContact File" );
		fModel = aModel;
		}

	@Override public void actionPerformed(ActionEvent evt)
		{
		File file = getFile();
		if ( file == null )
			return;

		try
			{
			PrintWriter pw = new PrintWriter ( file );
				pw.println( "Contact ID,Name, EMail Address" );
			int numRows = fModel.getRowCount();
			for ( int row = 0; row < numRows; row++ )
				{
				Raw raw = fModel.get(row);
				String line = buildLine(raw);
				if ( line == null )
					return;
				pw.println( line);
				}
			pw.close();
			}
		catch (IOException e)
			{
			SBDialog.error( "Error Saving Table Data",
				file.toString() + " Error: " + e.getMessage());
			return;
			}
		}

	private String buildLine(Raw raw)
		{
		return raw.id() + "," + raw.name() + "," + raw.eMail();
		}

	private File getFile()
		{
		JFileChooser chooser = new JFileChooser();
		SBProperties props = SBProperties.getInstance();
		if ( props != null )
			{
			File saveDir = props.getFile("usr.dir.mailmerge");
			if ( saveDir == null )
				saveDir = props.getFile("usr.cwd");
			if ( saveDir != null )
				chooser.setCurrentDirectory(saveDir);
			}
		FileNameExtensionFilter filter
			 = new FileNameExtensionFilter( "CSV, Comma separated values", "csv");
		chooser.setFileFilter(filter);
		chooser.setDialogTitle(LAF.getDialogTitle("Save QL Data File As"));
		int returnVal = chooser.showSaveDialog(null);
		if(returnVal != JFileChooser.APPROVE_OPTION)
			return null;
		if ( props != null )
			props.setProperty("usr.dir.mailmerge", chooser.getCurrentDirectory());
		return chooser.getSelectedFile();
		}
	}
