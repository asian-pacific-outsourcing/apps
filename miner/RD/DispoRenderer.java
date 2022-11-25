package com.apo.apps.miner.RD;
/********************************************************************
* @(#)DispoRenderer.java	1.00 10/05/29
* Copyright (c) 2010 by Richard T. Salamone, Jr. All rights reserved.
*
* DispoRenderer: A TableCellRenderer that displays the raw disposition
* as radio buttons in a JTable. This class just extends EditDispo to
* to implement TableCellRenderer.
*
* @version 1.00 05/29/10
* @author Rick Salamone
*******************************************************/
import com.apo.contact.Dispo;
import com.apo.contact.edit.EditDispo;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;
import java.awt.Color;
import java.awt.Component;

public final class DispoRenderer extends EditDispo
	implements TableCellRenderer
	{
	public DispoRenderer(Dispo[] availableDispos)
		{
		super(availableDispos);
		}

	public Component getTableCellRendererComponent(
                            JTable table, Object disposition,
                            boolean isSelected, boolean hasFocus,
                            int row, int column)
		{
		Dispo dispo = (Dispo)disposition;
		if ( dispo == null )
			dispo = Dispo.XX;
		if ( isSelected )
			{
			setForeground(table.getSelectionForeground());
			setBackground(table.getSelectionBackground());
			}
		else
			{
			setForeground(table.getForeground());
			setBackground(table.getBackground());
			}
		if ( dispo == Dispo.XX )
			clear();
		else
			select( dispo );
		return this;
		}
	}
