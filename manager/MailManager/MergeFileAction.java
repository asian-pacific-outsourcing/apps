package com.apo.apps.manager.MailManager;
/********************************************************************
* @(#)MergeFileAction.java 1.00 20110303
* Copyright (c) 2011 by Richard T. Salamone, Jr. All rights reserved.
*
* MergeFileAction: Action to output the results of a brochure request
* fetch into a file appropriate for iContact.
*
* @author Rick Salamone
* @version 1.00, 20110303 rts created
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

public final class MergeFileAction
	extends AbstractAction
	{
	private final int LABEL_LINES = 8;
	private final RawTableModel fModel;

	public MergeFileAction( RawTableModel aModel )
		{
		super( "Merge File" );
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
				pw.println( "F1|F2|F3|F4|F5|F6|F7|F8" );
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
		String line = raw.name() + "|" + raw.company()
			+ "|" + raw.address() + "|" + raw.country();
		String[] pieces = line.split("\\|");
		int numCols = pieces.length;
		int diff = LABEL_LINES - numCols;
// System.out.println("Pieces: " + numCols + " " + line);
		if ( diff < 0 )
			{
			SBDialog.error( "Too Many Address Lines",
				raw.toString() + " has too many address lines.\nPlease fix and try again.");
			return null;
			}
		while ( diff-- > 0 )
			{
			line = line + "|";
			if ( diff-- > 0 )
			line = "|" + line;
			}
		return line;
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
/*********
		FileNameExtensionFilter filter
			 = new FileNameExtensionFilter( "CSV, Comma separated values", "csv");
		chooser.setFileFilter(filter);
*********/
		chooser.setDialogTitle(LAF.getDialogTitle("Save Merge Data File As"));
		int returnVal = chooser.showSaveDialog(null);
		if(returnVal != JFileChooser.APPROVE_OPTION)
			return null;
		if ( props != null )
			props.setProperty("usr.dir.mailmerge", chooser.getCurrentDirectory());
		return chooser.getSelectedFile();
		}
	}

