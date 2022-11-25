package com.apo.apps.manager.MailManager;
/********************************************************************
* @(#)ReportControls.java 1.00 20100821
* Copyright © 2011 by Richard T. Salamone, Jr. All rights reserved.
*
* ReportControls: Interface for Mail Manager to select and filter
* contacts for processing.
*
* @author Rick Salamone
* @version 1.00 20110303, rts created
* @version 1.01 20110310, rts increased show limits
* @version 1.02 20110517, rts added qualified leads to Actions for newsletter
* @version 1.51, 20110601 rts handles email change requests
*******************************************************/
import com.apo.contact.Raw;
import com.apo.contact.Dispo;
import com.apo.contact.Source;
import com.apo.contact.HTRCount;
import com.apo.contact.edit.CheckDispo;
import com.apo.contact.edit.EditHTRCount;
import com.apo.contact.edit.EditSource;
import com.apo.contact.touch.TouchCode;
import com.shanebow.dao.*;
import com.shanebow.dao.edit.*;
import com.apo.employee.Role;
import com.shanebow.ui.calendar.MonthCalendar;
import com.shanebow.ui.SBDialog;
import com.shanebow.ui.SpinTextField;
import com.shanebow.ui.layout.LabeledPairPanel;
import com.shanebow.ui.LAF;
import com.shanebow.util.SBDate;
import com.shanebow.util.SBProperties;
import java.awt.*;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.border.*;

public final class ReportControls
	extends JPanel
	{
	private static final String[] SHOW = { "1000", "2000" };

	protected final MonthCalendar calendar = new MonthCalendar();
	private final JComboBox     cbChoices = new JComboBox(); // select touch code
	private final JComboBox     cbShow = new JComboBox(SHOW);
	private final EditDateRange editDateRange = new EditDateRange(EditDateRange.VERTICAL);
	private final EditCountry   cbCountry = new EditCountry();
	private final EditHTRCount  edHTR = new EditHTRCount();
	private final SelectRelationalOperator htrOp = new SelectRelationalOperator();
	private final EditSource    cbSource = new EditSource();
	private final JTextField    tfMisc = new JTextField();
	private final JTextField    tfName = new JTextField();
	private final CheckDispo    chkDispo = new CheckDispo(0,4);

	public ReportControls(Component dlgContact, JTable table)
		{
		super( new BorderLayout());

		calendar.addPropertyChangeListener(
			MonthCalendar.TIMECHANGED_PROPERTY_NAME, editDateRange);
		calendar.setOpaque(false);

		JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		splitPane.setTopComponent(calendar);
		splitPane.setBottomComponent(new JScrollPane(filtersPanel()));
		splitPane.setDividerLocation(190); //XXX ignored in some releases bug 4101306
		add(splitPane, BorderLayout.CENTER);
		}

	public void updateHistory()
		{
//		tfMisc.addToHistory();
//		tfOrder.addToHistory();
		}

	private JComponent filtersPanel()
		{
		SBProperties props = SBProperties.getInstance();
//		cbChoices.addItem("--");
		cbChoices.addItem(TouchCode.MAILREQ);
		cbChoices.addItem(TouchCode.MAILSENT);
		cbChoices.addItem(TouchCode.QUALIFIED);
		cbChoices.addItem(TouchCode.EMAILCHG);
		cbChoices.addItemListener(new ItemListener()
			{
			@Override public void itemStateChanged(ItemEvent e)
				{
				boolean haveChoice = cbChoices.getSelectedIndex() != 0;
				editDateRange.setEnabled(haveChoice);
				cbSource.setEnabled(haveChoice);
				}
			});
		String maxShow = props.getProperty("app.fetch.max.items", "");
		if ( !maxShow.isEmpty())
			cbShow.addItem(maxShow);
		cbShow.setSelectedIndex(2);

		LabeledPairPanel p = new LabeledPairPanel();  // "Filters" );
		p.setBorder(LAF.getStandardBorder());

		p.addRow(new Section("Action"), cbChoices);
		p.addRow(     "", editDateRange );
		p.addRow(   "by", cbSource );

		p.addRow(new Section(), new Section());
		p.addRow(new Section("Filters"), new JLabel());
		p.addRow( "Country", cbCountry );
		p.addRow( "Name", tfName );
		p.addRow( "misc", tfMisc );

		p.addRow(new Section(), new Section());
		p.addRow(new Section("Limit"), cbShow ); // cbShow, new JLabel("contacts"));

		return p;
		}

	public int getMaxShowCount()
		{
		try { return Integer.parseInt((String)cbShow.getSelectedItem()); }
		catch (Exception e) { return -1; }
		}

	public String getSQL()
		{
		return "SELECT * FROM raw"
		          + "\n WHERE " + getWhereClause()
		          + "\n ORDER BY " + getOrderClause();
		}

	public String getOrderClause()
		{
		return "id ASC";
		}

	public final Object getAction() { return cbChoices.getSelectedItem(); }

	public final String getWhereClause()
		{
		long dates[] = getDateRange();
		Object choice = cbChoices.getSelectedItem();
		Country countryID = null;
		Source sourceID;
		try
			{
			countryID = cbCountry.get();
			sourceID = cbSource.get();
			}
		catch (Exception e)
			{
			SBDialog.inputError(e.toString());
			return null;
			}
		String it = "noAnswer >= 0";
		if ( countryID != Country.XX )
			it += " AND countryID = " + countryID.dbRepresentation();

		if ( choice instanceof TouchCode )
			{
			TouchCode code = (TouchCode)choice;
			it += " AND EXISTS (SELECT NULL FROM touch WHERE touch.contactID = raw.id";
			it += " AND touch.touchCode=" + code.dbRepresentation();
			if ( sourceID != Source.XX )
				it += " AND touch.employeeID = " + sourceID.dbRepresentation();
			it += " AND touch.when BETWEEN " + dates[0] + " AND " + dates[1] + ")";
			}
		else if ( choice instanceof ScheduledAction )
			{
			ScheduledAction scheduled = (ScheduledAction)choice;
			it += " AND " + scheduled.sql();
			if ( sourceID != Source.XX )
				it += " AND sourceID = " + sourceID.dbRepresentation();
			it += " AND " + Raw.dbField(Raw.CALLBACK)
			    + " BETWEEN " + dates[0] + " AND " + dates[1];
			}
		String name = tfName.getText();
		if ( !name.isEmpty())
			it += " AND name Like '%" + name + "%'";
		String misc = tfMisc.getText();
		if ( !misc.isEmpty())
			it += " AND " + misc;
		return it;
		}

	public long[] getDateRange() { return editDateRange.getDateRange(); }
	}

final class ScheduledAction
	{
	private final String m_desc;
	private final String m_sql;
	public ScheduledAction( String desc, Dispo[] dispos )
		{
		m_desc = desc;
		String sql = Raw.dbField(Raw.DISPO) + " IN ";
		for ( int i = 0; i < dispos.length; i++ )
			sql += ((i == 0)? "(" : ",") + dispos[i].dbRepresentation();
		sql += ")";
		m_sql = sql;
		}

	public String toString() { return "Scheduled for " + m_desc; }
	public String sql() { return m_sql; }
	}

class Section extends JLabel
	{
	public Section()
		{
		super( "<html><HR width=5000>" );
		}
	public Section(String title)
		{
		super( "<html><B>" + title );
		}
	}
