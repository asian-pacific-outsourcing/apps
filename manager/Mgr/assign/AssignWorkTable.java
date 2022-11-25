package com.apo.apps.manager.Mgr.assign;
/********************************************************************
* @(#)AssignWork.java 1.00 20110203
* Copyright 2011 by Richard T. Salamone, Jr. All rights reserved.
*
* AssignWork.java: Allows a manager to (re)assign contacts to the
* salesmen.
*
* @author Rick Salamone
* @version 1.00, 20110203 rts initial demo
*******************************************************/
import com.apo.contact.Raw;
import com.apo.contact.report.RawTable;
import com.apo.contact.report.RawTableModel;
import com.apo.contact.Source;
import com.apo.employee.Role;
import java.util.*;
import java.awt.Rectangle;
import java.io.IOException;
import java.awt.datatransfer.*;
import javax.swing.*;

public class AssignWorkTable
	extends RawTable
	{
	private Role fRole;
	private Source fAssignee;

	public AssignWorkTable()
		{
		super(new RawTableModel());
		setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		setTransferHandler(new RawTableTransferHandler());
//		setDragEnabled(true);
		}

	final void setAssignedTo(Source aAssignee, Role aRole)
		{
		fAssignee = aAssignee;
		fRole = aRole;
		setDragEnabled(fAssignee != null);
		}
	final Source getAssignedTo() { return fAssignee; }
	final Role getRole() { return fRole; }

	final void append( Raw[] raws )
		{
		RawTableModel model = (RawTableModel)getModel();
		for ( Raw raw : raws )
			model.add(raw);
		}
	}

class RawTableTransferHandler
	extends AssignWorkTransferHandler
	{
	@Override public boolean canImport(TransferSupport support)
		{
		return support.isDrop()
		    && support.isDataFlavorSupported(DataFlavor.stringFlavor);
		}

	@Override public boolean importData(TransferSupport support)
		{
		if (!canImport(support))
			return false;

		// Get the contacts that are being dropped
		Raw[] raws = getTransferContacts(support);
		if ( raws == null )
			return false;

		// fetch the drop location
		int row = ((JTable.DropLocation)support.getDropLocation()).getRow();

		AssignWorkTable table = (AssignWorkTable)support.getComponent();
		RawTableModel tableModel = (RawTableModel)table.getModel();
		changeAssignments(raws, table.getAssignedTo());
		for ( Raw raw : raws )
			tableModel.add(row++, raw);

		Rectangle rect = table.getCellRect(--row, 0, false);
		if (rect != null)
			table.scrollRectToVisible(rect);
		return true;
		}

	@Override public int getSourceActions(JComponent c) { return MOVE; }

	@Override protected Transferable createTransferable(JComponent c)
		{
		JTable table = (JTable)c;
		int[] indices = table.getSelectedRows();
		RawTableModel tableModel = (RawTableModel)table.getModel();

		StringBuffer buff = new StringBuffer();
		for (int row : indices )
			{
			buff.append(tableModel.get(row).toCSV());
			buff.append("\n");
			}
		buff.deleteCharAt(buff.length() - 1); // remove last newline
		return new StringSelection(buff.toString());
		}

	@Override protected void exportDone(JComponent c, Transferable t, int action)
		{
		if (action == MOVE)
			{
			JTable table = (JTable)c;
			int[] indices = table.getSelectedRows();
			RawTableModel tableModel = (RawTableModel)table.getModel();
			int numSelected = indices.length;
			while ( numSelected-- > 0 )
				tableModel.removeRow(indices[numSelected]);
			}
		} 
	}