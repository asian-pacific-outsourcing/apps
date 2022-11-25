package com.apo.apps.manager.Mgr.assign;
/********************************************************************
* @(#)AssignWork.java 1.00 2010
* Copyright (c) 2010-2011 by Richard T. Salamone, Jr. All rights reserved.
*
* AssignWork.java: Allows a manager to (re)assign contacts to the
* salesmen.
*
* @author Rick Salamone
* @version 1.00, 20101107 rts initial demo
*******************************************************/
import com.apo.contact.Raw;
import com.apo.contact.Dispo;
import com.apo.contact.Source;
import com.apo.employee.*;
import com.apo.net.Access;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import java.text.*;
import java.awt.datatransfer.*;

import javax.swing.*;
import javax.swing.text.*;
import javax.swing.tree.*;

public final class RawTreeTransferHandler
	extends AssignWorkTransferHandler
	{
	AssignWorkTable fAssignedTable;
	public RawTreeTransferHandler(AssignWorkTable aAssignedTable)
		{
		super();
		fAssignedTable = aAssignedTable;
		}

	public boolean canImport(TransferHandler.TransferSupport info)
		{
		return info.isDataFlavorSupported(DataFlavor.stringFlavor)
		    && (getDropPath(info) != null);
		}

	public boolean importData(TransferHandler.TransferSupport info)
		{
		if (!info.isDrop())
			return false;

		if (!info.isDataFlavorSupported(DataFlavor.stringFlavor))
			return sayError("Tree doesn't accept a drop of this type.");

		Object[] nodes = getDropPath(info);
		if ( nodes == null ) // not a leaf
			return false;
		Role nodeRole = (Role)((DefaultMutableTreeNode)nodes[1]).getUserObject();
		User nodeUser = (User)((DefaultMutableTreeNode)nodes[2]).getUserObject();
		Source newOwner = Source.find(nodeUser.id());

		Raw[] raws = getTransferContacts(info); // get raws being dropped
		if ( raws == null )
			return false;

		Source currOwner = raws[0].assignedTo();
		if ( newOwner.equals(currOwner))
			return sayError("Already assigned to " + currOwner + "!");

		for ( Raw raw : raws )
			if ( !validDispo(nodeRole, raw.dispo()))
				return sayError(nodeRole.shortName()
					+ "'s don't call contacts dispositioned as " + raw.dispo().nameAndCode());

		changeAssignments( raws, newOwner ); // do the import
		if ( newOwner.equals( fAssignedTable.getAssignedTo()))
			fAssignedTable.append( raws );
		return true;
		}

	private boolean validDispo(Role aRole, Dispo aDispo)
		{
		return aRole.callsDispo(aDispo)
			|| ((aRole.access()==Access.AO)
					&& (aDispo==Dispo.KOL || aDispo==Dispo.TOL || aDispo==Dispo.CO));
		}
	private Object[] getDropPath(TransferSupport info)
		{
		JTree.DropLocation dl = (JTree.DropLocation)info.getDropLocation();
		Object[] nodes = ((TreePath)dl.getPath()).getPath();
		return (nodes.length < 3)? null : nodes; // null if not a leaf
		}

	public int getSourceActions(JComponent c) { return NONE; }

	protected Transferable createTransferable(JComponent c) { return null; }
	}