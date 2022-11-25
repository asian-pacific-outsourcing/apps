package com.apo.apps.caller.actions;
/********************************************************************
* @(#)AOActionNewOrder.java 1.00 20110217
* Copyright 2011 by Richard T. Salamone, Jr. All rights reserved.
*
* AOActionNewOrder: Action class to take a new contact order.
*
* @author Rick Salamone
* @version 1.00, 20110217 rts created
*******************************************************/
import com.apo.apps.caller.CallerGUI;
import com.apo.contact.Raw;
import com.apo.contact.SBRadioGroup;
import com.apo.order.*;
import com.shanebow.dao.*;
import com.shanebow.dao.edit.*;
import com.shanebow.ui.LAF;
import com.shanebow.ui.SBAction;
import com.shanebow.ui.SBDialog;
import java.awt.*;
import java.awt.event.ActionEvent;
import javax.swing.*;

public final class AOActionNewOrder
	extends SBAction
	{
	private DlgAddOrder dlgAddOrder = new DlgAddOrder();

	public AOActionNewOrder()
		{
		super("Order", 'O', "Write an order for this contact", "dollar" );
		}

	@Override public void actionPerformed(ActionEvent e)
		{
		CallerGUI gui = CallerGUI.getInstance();
		Raw edited = gui.getEdited();
		if ( edited == null )
			return;
		Order order = gui.getMostRecentOrder();
		if ( order != null )
			{
			gui.editOrder(order);
			return;
			}
		dlgAddOrder.editNewOrder(edited);
		order = dlgAddOrder.getOrder();
		if ( order == null )
			return;
		try
			{
			OrderStatus status = order.status();
System.out.println("order status: " + status );
			OrderID oid = Order.DAO.add( order );
			gui.orderAdded(new Order(oid, order));
			When callback = new When( Raw.DAO.getServerTime() + status.callbackDelay());
			gui.finishUp( edited, null, status.dispo(), callback, "" );
			}
		catch (Exception ex) { SBDialog.error("Data Access Error", ex.getMessage()); }
		}
	}
