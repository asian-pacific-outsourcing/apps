package com.apo.apps.manager.Mgr.assign;
/********************************************************************
* @(#)AssignWork.java 1.00 2010
* Copyright 2010-2011 by Richard T. Salamone, Jr. All rights reserved.
*
* AssignWork.java: Allows a manager to (re)assign contacts to the
* salesmen.
*
* @author Rick Salamone
* @version 1.00, 20101107 rts initial demo
* @version 1.01, 20110419 rts checks user access to roles
*******************************************************/
import com.apo.contact.Raw;
import com.apo.contact.Dispo;
import com.apo.contact.HTRCount;
import com.apo.contact.Source;
import com.apo.contact.edit.EditHTRCount;
import com.apo.contact.report.RawTable;
import com.apo.contact.report.RawTableModel;
import com.apo.employee.*;
import com.apo.net.Access;
import com.shanebow.ui.LAF;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import java.text.*;
import java.awt.datatransfer.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import javax.swing.tree.*;

// For filters panel
import com.shanebow.dao.*;
import com.shanebow.dao.edit.*;
import com.shanebow.ui.layout.LabeledPairPanel;

public final class AssignWork
	extends JPanel
	implements ActionListener
	{
	private static final int LEFT_PANEL_WIDTH=200;
	private static final Object[] AO_CHOICES = { "Unassigned", Dispo.TOL, Dispo.KOL, Dispo.CO };

	private final AssignWorkTable fAssignedTable = new AssignWorkTable();
	private final AssignWorkTable fUnassignedTable = new AssignWorkTable();
	private final JComboBox       cbAssignAO = new JComboBox(AO_CHOICES);
	private final EditCountry     cbCountry = new EditCountry();
	private final EditCountry     cbHomeland = new EditCountry();
	private final EditHTRCount    edHTR = new EditHTRCount();
	private final SelectRelationalOperator htrOp = new SelectRelationalOperator();

	public AssignWork()
		{
		super(new BorderLayout());
		JPanel leftPanel = createVerticalBoxPanel();

		//LEFT COLUMN
		final JTree tree = createUserTree();
		tree.setRootVisible(false);
		tree.getSelectionModel().setSelectionMode
			(TreeSelectionModel.SINGLE_TREE_SELECTION);

		tree.addTreeSelectionListener(new TreeSelectionListener()
			{
			public void valueChanged(TreeSelectionEvent e)
				{
				// Returns the last path element of the selection.
				// Following line only works for single selection model
				DefaultMutableTreeNode node
					= (DefaultMutableTreeNode)tree.getLastSelectedPathComponent();

				if (node == null) // Nothing is selected
					return;

				Object nodeInfo = node.getUserObject();
				if (nodeInfo instanceof Role ) // !node.isLeaf())
					displayUnassignedContactsFor((Role)nodeInfo);
				else
					{
					DefaultMutableTreeNode parent
						= (DefaultMutableTreeNode)node.getParent();
					displayContactsAssignedTo((Role)parent.getUserObject(), (User)nodeInfo);
					}
				}
			});
		tree.setTransferHandler(new RawTreeTransferHandler(fAssignedTable));
		tree.setDropMode(DropMode.ON);
		tree.setDragEnabled(true);

		JScrollPane treeView = createScroller(tree, "Staff");
		treeView.setPreferredSize(new Dimension(LEFT_PANEL_WIDTH, 350)); // w, h
		leftPanel.add(treeView);

		JComponent fFiltersPanel = filtersPanel();
		leftPanel.add(createScroller(fFiltersPanel, "Filters"));

		//RIGHT COLUMN
		JSplitPane rightPanel = new JSplitPane(JSplitPane.VERTICAL_SPLIT );
		rightPanel.setContinuousLayout(true);
		rightPanel.setDividerLocation(300); // -1: honor upper left comp preferred size
		rightPanel.setTopComponent(createScroller(fUnassignedTable, "Unassigned"));
		rightPanel.setBottomComponent(createScroller(fAssignedTable, "Assigned"));

		JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                                              leftPanel, rightPanel);
		splitPane.setContinuousLayout(true);
		splitPane.setDividerLocation(LEFT_PANEL_WIDTH);

		add(splitPane, BorderLayout.CENTER);
		add(AssignWorkTransferHandler.statusBar(), BorderLayout.PAGE_END);
		setBorder(LAF.getStandardBorder());
		}

	protected JPanel createVerticalBoxPanel()
		{
		JPanel p = new JPanel();
		p.setLayout(new BoxLayout(p, BoxLayout.PAGE_AXIS));
		p.setBorder(LAF.getStandardBorder());
		return p;
		}

	private void displayContactsAssignedTo(Role aRole, User aUser)
		{
		fAssignedTable.setAssignedTo(Source.find(aUser.id()), aRole);
		String title = "Assigned to " + aRole.code() + " " + aUser;
		setScrollerTitle(fAssignedTable, title);
		String where = "SELECT * FROM " + Raw.DB_TABLE
		             + " WHERE " + Raw.dbField(Raw.CALLER) + "=" + aUser.id()
//+ " AND noAnswer <= 5"
		             + " AND " + Raw.dbField(Raw.DISPO)
		               + " IN " + Dispo.dbCriteriaList(aRole.fetchDispos())
		             + " ORDER BY Name ASC";
		((RawTableModel)fAssignedTable.getModel()).fetch( -1, where );
		}

	@Override public void actionPerformed(ActionEvent e)
		{
		Role role = fUnassignedTable.getRole();
		if ( role != null )
			displayUnassignedContactsFor(role);
		}

	private void displayUnassignedContactsFor(Role aRole)
		{
		fUnassignedTable.setAssignedTo(Source.XX, aRole);
		String title = aRole.shortName() + " ";
int show = 100;
		Dispo[] fetchDispos = aRole.fetchDispos();
		Country country = Country.XX;
		Country homeland = Country.XX;
		Source assignee = Source.XX;
		HTRCount htr = HTRCount.DEFAULT_MAX_HTR;
		try
			{
			country = cbCountry.get();
			homeland = cbHomeland.get();
			htr = edHTR.get();
			}
		catch(Exception e) {} // all combo boxes - no worries
		if (( aRole.access() == Access.AO )
		&&  ( cbAssignAO.getSelectedIndex() != 0 ))
			{
			fetchDispos = new Dispo[1];
			fetchDispos[0] = (Dispo)cbAssignAO.getSelectedItem();
			assignee = null;
			title += " Reassign " + fetchDispos[0];
			}
		else title += "Unassigned Contacts";
		if ( fetchDispos == null || fetchDispos.length == 0 )
			return; // leave the previous display up
		busy(true);
		setScrollerTitle(fUnassignedTable, title);
		String where = "SELECT * FROM raw"
		             + " WHERE " + Raw.dbField(Raw.DISPO)
		                       + " IN " + Dispo.dbCriteriaList(fetchDispos)
		             + " AND " + Raw.dbField(Raw.HTRCOUNT)
		                       + htrOp.getSelectedItem() + htr.dbRepresentation();
		if ( assignee != null )
			where += " AND sourceID = " + assignee.dbRepresentation();
		if ( country != Country.XX )
			where += " AND " + Raw.dbField(Raw.COUNTRYID) + "=" + country.dbRepresentation();
		if ( homeland != Country.XX )
			where += " AND " + Raw.dbField(Raw.HOMELAND) + "=" + homeland.dbRepresentation();
		where += " ORDER BY Name ASC";
		((RawTableModel)fUnassignedTable.getModel()).fetch( show, where );
		busy(false);
		cbAssignAO.setEnabled(aRole.access() == Access.AO);
		}

	private final void busy(boolean aBusy)
		{
		setCursor( Cursor.getPredefinedCursor( aBusy? Cursor.WAIT_CURSOR : Cursor.DEFAULT_CURSOR ));
		cbAssignAO.setEnabled(!aBusy);
		cbCountry.setEnabled(!aBusy);
		cbHomeland.setEnabled(!aBusy);
		edHTR.setEnabled(!aBusy);
		htrOp.setEnabled(!aBusy);
		}

	private JComponent filtersPanel()
		{
		LabeledPairPanel p = new LabeledPairPanel();  // "Filters" );
		p.setBorder(LAF.getStandardBorder());

		p.addRow( "Assign", cbAssignAO );
		cbAssignAO.setEnabled(false);
		cbAssignAO.setSelectedIndex(0);
		p.addRow( "Country", cbCountry );
		p.addRow( "Nationality", cbHomeland );
		p.addRow(  "HTR", htrOp, edHTR );
		htrOp.setSelectedItem("<=");
		cbAssignAO.addActionListener(this);
		cbCountry.addActionListener(this);
		cbHomeland.addActionListener(this);
		return p;
		}

	private JTree createUserTree()
		{
		DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode("Users by Role");
		for ( Role role : Role.getAll())
			{
			if ( !role.isCaller() || !Access.allowedTo(role.access()))
				continue;
			DefaultMutableTreeNode roleNode = new DefaultMutableTreeNode(role);
			rootNode.add(roleNode);
			for ( User user : User.listBy(role))
				if ( !user.isManager())
					roleNode.add(new DefaultMutableTreeNode(user));
			}
		DefaultTreeModel model = new DefaultTreeModel(rootNode);
		return new JTree(model);
		}

	private void setScrollerTitle(JComponent comp, String title)
		{
		JScrollPane scroller = (JScrollPane)comp.getParent().getParent();
		scroller.setBorder(BorderFactory.createTitledBorder(title));
		}

	private JScrollPane createScroller(JComponent comp, String title)
		{
		JScrollPane scroller = new JScrollPane(comp);
		if (title != null)
			scroller.setBorder(BorderFactory.createTitledBorder(title));
		return scroller;
		}
	}
