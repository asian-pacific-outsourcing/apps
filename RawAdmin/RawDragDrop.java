package com.apo.apps.RawAdmin;
/********************************************************************
* @(#)RawDragDrop.java 1.00 2009
* Copyright 2009-2010 by Richard T. Salamone, Jr. All rights reserved.
*
* RawDragDrop: Extends the dnd TransferHandler to launch import
* of apo import and update files.
*
* @author Rick Salamone
* @version 1.00 08/07/10 created from the APO import
*******************************************************/
import com.apo.apps.RawAdmin.FileFlavor;
import com.apo.apps.RawAdmin.rawimport.ImportWiz;
import com.shanebow.ui.SBAudio;
import com.shanebow.util.SBLog;
import javax.swing.TransferHandler;
import java.io.*;
import java.net.*;
import java.awt.datatransfer.*;
import java.util.List;
import java.util.Vector;

final class RawDragDrop
	extends TransferHandler
	{
	private boolean m_copyEnabled = false;

	RawDragDrop() { super(); }

	private void log ( String fmt, Object... args )
		{
		SBLog.write ( "DND " + String.format(fmt,args));
		}

	public void setCopyEnabled(boolean on) { m_copyEnabled = on; }

	public boolean canImport( TransferHandler.TransferSupport support )
		{
		if ( !support.isDataFlavorSupported(DataFlavor.javaFileListFlavor))
			return false;

		if ( m_copyEnabled )
			{
			boolean copySupported = (COPY & support.getSourceDropActions()) == COPY;
			if ( !copySupported )
				return false;

			support.setDropAction(COPY);
			}
		return true;
		}

	@SuppressWarnings("unchecked")
	public boolean importData(TransferHandler.TransferSupport support)
		{
		if ( !canImport(support))
			return false;
		String name = "";
		Transferable t = support.getTransferable();
		try
			{
			Vector<File> importFiles = null;
			List<File> list =
				(List<File>)t.getTransferData(DataFlavor.javaFileListFlavor);
			for ( File file : list )
				{
				name = file.getName();
				if ( name.toLowerCase().endsWith(".csv"))
					{
					FileFlavor csvFlavor = FileFlavor.discern(file);
					if ( csvFlavor == FileFlavor.UPDATE_CONTACTS )
						new CheckInBulkWiz(file);
					else if ( csvFlavor == FileFlavor.IMPORT_CALL_LOG )
						log("CALL LOG");// new ImportCalls(file);
					else
						{
						if ( importFiles == null )
							importFiles = new Vector<File>(1);
						importFiles.add(file);
						}
					}
				else if ( name.endsWith(SBAudio.AU_FILE_EXT.toUpperCase())
						||   name.endsWith(SBAudio.WAV_FILE_EXT.toUpperCase()))
					SBAudio.playClip(file.getPath());
				else log("import(" + file + "): " + "Not supported" );
				}
			if ( importFiles != null )
				new ImportWiz().onStart(importFiles.toArray(new File[0]));
			}
		catch (UnsupportedFlavorException e)
			{
			log(name + ": " + e );
			return false;
			}
		catch (IOException e)
			{
			log(name + ": " + e );
			return false;
			}
		catch (Exception e)
			{
			log( e.toString());
			return false;
			}
		return true;
		}
	}
