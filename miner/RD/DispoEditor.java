package com.apo.apps.miner.RD;
/********************************************************************
* @(#)DispoEditor.java	1.00 10/05/29
* Copyright (c) 2010-2011 by Richard T. Salamone, Jr. All rights reserved.
*
* DispoEditor: An abstract cell editor for dispositons rendered
* as radio buttons in a JTable.
*
* @author Rick Salamone
* @version 1.00 20100529 rts created
* @version 1.01 20110316 rts removed no arg c'tor & check for null choices
*******************************************************/
import com.apo.contact.Dispo;
import com.apo.contact.edit.EditDispo;
import com.shanebow.util.SBLog;
import javax.swing.AbstractCellEditor;
import javax.swing.table.TableCellEditor;
import javax.swing.JTable;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class DispoEditor extends AbstractCellEditor
	implements TableCellEditor, ActionListener
	{
	Dispo currentDispo;
	EditDispo radio;

	public DispoEditor(Dispo[] aChoices)
		{
		radio = new EditDispo(aChoices);
		radio.addActionListener(this);
		}

	/**
	* Handles events from radio group
	*/
	public void actionPerformed(ActionEvent e)
		{
		String cmd = e.getActionCommand();
		// SBLog.write( "cmd: " + cmd );
		if ( radio.get() == currentDispo )
			{
			radio.clear();
			currentDispo = Dispo.XX;
			}
		else
			currentDispo = radio.get();
		fireEditingStopped();
		}

	// Implement the one CellEditor method that AbstractCellEditor doesn't
	public Object getCellEditorValue()
		{
		return currentDispo;
		}

	// Implement the one method defined by TableCellEditor
	public Component getTableCellEditorComponent( JTable table, Object disposition,
                                                 boolean isSelected,
                                                 int row, int column )
		{
		currentDispo = (Dispo)disposition;
		radio.setForeground(table.getSelectionForeground());
		radio.setBackground(table.getSelectionBackground());
		return radio;
		}
	}
