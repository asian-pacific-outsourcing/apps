package com.apo.apps.caller;
/********************************************************************
* @(#)CallerGUI.java 1.00 2010
* Copyright 2010-2011 by Richard T. Salamone, Jr. All rights reserved.
*
* CallerGUI:
*
* @author Rick Salamone
* @version 1.00, 2010
* 20110509 rts decoupled the actions from this gui
* 20110603 rts modified for background prefetch
*******************************************************/
import com.apo.contact.Raw;
import com.apo.contact.Dispo;
import com.apo.contact.Source;
import com.apo.contact.edit.RawPanel;
import com.apo.contact.edit.LabeledCounter;
import com.apo.contact.report.RawTableModel;
import com.apo.contact.touch.ContactHistory;
import com.apo.contact.touch.TouchCode;
import com.shanebow.dao.*;
import com.apo.employee.Role;
import com.apo.net.Access;
import com.apo.order.*;
import com.shanebow.ui.SBDialog;
import com.shanebow.util.SBLog;
import com.shanebow.util.SBProperties;
import com.shanebow.ui.LAF;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public final class CallerGUI
	extends JPanel
	{
	static private CallerGUI _instance;
	static public CallerGUI getInstance() { return _instance; }

	private static final int POLL_WORK_DELAY = 60;

	private final boolean        m_autoNext;
	private final RawPanel       fRawPanel;
	private ContactHistory       fHistoryTab;
	private OrderTable           fOrderTab;
	private final LabeledCounter fCounter;
	private final Titled         fTitleMe;
	private final TouchCode      fCallTouchCode;
	private final FetchWorker    fFetchWorker;

	private final JLabel         fClock;
	private final Duration       fTime = new Duration(0);
	Timer timer;
	int ticks = 0;

	private final ActionList fActions; // Contact Actions: role & user dependent

	public final Object[] getMenuItems() { return fActions.toArray(); }
	public final Raw getEdited() { return fRawPanel.getEdited(); }
	public final Raw getUnedited() { return fRawPanel.getUnedited(); }
	public final ContactID getRawID() { return fRawPanel.getID(); }
	public final Address getAddress() { return fRawPanel.getAddress(); }
	public final EMailAddress getEMailAddress() { return fRawPanel.getEMailAddress(); }
	public final void set(int aRFN, DataField aValue) { fRawPanel.set(aRFN,aValue); }
	public final void fireAction(String aKey) { fActions.fireAction(aKey); }
	public final void newFetchWork() { fFetchWorker.dequeue(); }

	public CallerGUI(Titled aTitleMe)
		{
		this(aTitleMe, new ActionList(Access.getRole()));
		}

	public CallerGUI(Titled aTitleMe, ActionList aActionList)
		{
		super(new BorderLayout());
		fTitleMe = aTitleMe;
		fActions = aActionList;
		Role role = Access.getRole();
		m_autoNext = SBProperties.getInstance().getBoolean("autoNext");
		fClock = m_autoNext? new JLabel("00:00") : null;
		fCallTouchCode = role.touchCode();
		Dispo[] dispos = role.isManager()? Access.accessibleDispos().toArray(new Dispo[0])
		               : null;

		fRawPanel = new RawPanel(role.layoutColumns(), role.rawFields(), dispos)
			{
			@Override public void setDirty(boolean on)
				{
				super.setDirty(on);
				boolean isDirty = fRawPanel.isDirty();
				fActions.setDirty(isDirty, haveDispo());
				}
			};
		fRawPanel.setEnabled(Raw.EMAIL, role.isManager() || role.access() == Access.TQ);

		fCounter = new LabeledCounter("Dispositioned");
		add(mainPanel(), BorderLayout.CENTER);
		_instance = this;
		fFetchWorker = m_autoNext? new FetchWorker() : null;
		}

	public void onConnect()
		{
		int delay = 1000;
		timer = new Timer(delay, new ActionListener()
			{
			public void actionPerformed(ActionEvent e) { onTick(); }
			});
		timer.start();
		if ( m_autoNext )
			fetchNext();
		}

	private JComponent mainPanel()
		{
		SBProperties props = SBProperties.getInstance();
		if ( !Access.allowedTo(Access.AO|Access.LO)
		&& !props.getBoolean("call.hist.view", false))
			return fRawPanel;
		JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		split.setTopComponent( fRawPanel );
		split.setBottomComponent( bottomPanel(props));
		split.setDividerLocation( -1 );
		split.setOneTouchExpandable(true);
		split.setContinuousLayout(true);
		return split;
		}

	DlgEditOrder fDlgEditOrder;
	private JComponent bottomPanel(SBProperties props)
		{
		ImageIcon icon = new ImageIcon("image/byellow.gif");
		JTabbedPane tabbedPane = new JTabbedPane();
		if ( Access.allowedTo(Access.AO|Access.LO))
			{
			fOrderTab = new OrderTable(fDlgEditOrder = new DlgEditOrder(null));
			tabbedPane.addTab("Orders", icon, new JScrollPane(fOrderTab), "Contact Orders" );
			}
		if ( props.getBoolean("call.hist.view", false))
			{
			fHistoryTab = new ContactHistory();
			tabbedPane.addTab("History", icon, fHistoryTab, "Contact History" );
			}
		if ( Access.allowedTo(Access.LO))
			{
			JTextArea fNotesTab = new JTextArea();
			fNotesTab.setText("This area is for the loader's notes\n"
			    + "but, it DOES NOT WORK YET!!");
			tabbedPane.addTab("Notes", icon, fNotesTab, "Loader Notes" );
			}
		return tabbedPane;
		}

	public JPanel getControlPanel()
		{
		SBProperties props = SBProperties.getInstance();
		JPanel p = new JPanel();
		Dimension edgeSpacer = new Dimension(5, 0);
		p.setLayout(new BoxLayout(p, BoxLayout.LINE_AXIS));
//		p.add(Box.createRigidArea(edgeSpacer));
		if ( m_autoNext )
			{
			p.add( fClock );
			p.add(Box.createRigidArea(edgeSpacer));
			p.add(fCounter);
			}
		else if (Access.getRole().isManager())
			{
			JPanel find = new JPanel();
			JTextField tfFindID = new JTextField(7);
			find.add( new JLabel("Fetch ID:"));
			find.add( tfFindID );
			tfFindID.addActionListener( new ActionListener()
				{
				public void actionPerformed(ActionEvent e)
					{
					ContactID id = null;
					try { id = ContactID.parse(e.getActionCommand()); }
					catch (Exception ex)
						{
						SBDialog.inputError( "Malformed Contact ID\n" + ex.getMessage());
						return;
						}
					cmdFetchID(id);
					((JTextField)e.getSource()).setText("");
					}
				});
			p.add(find);
			}
		p.add(Box.createHorizontalGlue());
		p.add(fActions.getButtonPanel());
		return p;
		}

	public final void release()
		{
		try
			{
			Raw.DAO.release( fRawPanel.getUnedited());
			clear();
			}
		catch (Exception e) {} // SBDialog.error("Release Error", e.getMessage()); }
		}

	private void onTick()
		{
		++ticks;
		fTime.set(ticks);
		if ( fClock != null )
			fClock.setText(fTime.toString());
//		if (( ticks % POLL_WORK_DELAY ) == 0 )
			{
			if (( getRawID() == null ) && m_autoNext )
				fetchNext();
			else
		if (( ticks % POLL_WORK_DELAY ) == 0 )
				try { Raw.DAO.keepAlive(); }
				catch (Exception e) { SBDialog.error("Keep Alive Failed", e.getMessage()); }
			}
		}

	private void fetchNext()
		{
		if ( fFetchWorker.hasWork())
			display(fFetchWorker.dequeue());
		else
			sayStatus( "No work is scheduled right now" );
/*************
		try 
			{
			Raw raw = Raw.DAO.getWork("x");
			display( raw );
			if ( raw == null )
				sayStatus( "No work is scheduled right now" );
			}
		catch (Exception e) { SBDialog.fatalError(e.getMessage()); }
*************/
		}

	final Source getUID() { return Source.find(Access.getUID()); }

	public final boolean cmdFetchID(ContactID id)
		{
		return cmdCommit() && fetchID(id);
		}

	public final boolean cmdCommit()
		{
		if ( getRawID() == null )
			return true; // because we're done saving
		if ( !fRawPanel.isDirty())
			return true;
		Raw edited = fRawPanel.getEdited();
		if ( edited != null )
			finishUp(edited, null, null, null, "");
		return (edited != null);
		}

	public final void finishUp( Raw raw, Source assignTo, Dispo dispo,
		When callback, String touchDetails )
		{
		sayStatus( "Updating contact #" + raw.id() + "..." );
		short uID = Access.getUID();
		raw = new Raw( raw, assignTo, dispo, callback );

		if ( !Access.getRole().isManager() && dispo.isHTR())
			raw.htr().increment();
		try
			{
			long timeNow = Raw.DAO.getServerTime();
			Raw.DAO.update( raw, true, fCallTouchCode, touchDetails, timeNow, uID );
			RawTableModel.updateAll(raw);
			fCounter.increment();
			ticks = 0;
			}
		catch (Exception e) { SBDialog.error( "Error", e.getMessage()); }
		if ( m_autoNext )
			{
			clear();
			fetchNext();
			}
		else
			{
			fRawPanel.setContact(raw);
			SBDialog.inform(LAF.getDialogTitle("Update Contact"), "" + raw + " successfully updated");
			}
		}

	private boolean fetchID( ContactID id )
		{
		if ( id == null )
			{
			clear();
			return false;
			}
		try { display(Raw.DAO.fetch(id)); return true; }
		catch (Exception e) { sayStatus( e.getMessage()); return false; }
		}

	private void clear()
		{
		display((Raw)null);
		}

	public final void display( Raw aRaw )
		{
		fRawPanel.setContact(aRaw);
		sayStatus(fRawPanel.getTitle());
		refreshHistory();
		if ( fOrderTab != null )
			fOrderTab.fetch(aRaw);
		fActions.enableContactActions(aRaw);
		}

	public final void display( Work aWork )
		{
		Raw raw = aWork.getRaw();
		fRawPanel.setContact(raw);
		sayStatus(fRawPanel.getTitle());
		if ( fHistoryTab != null ) // authorized to view history
			fHistoryTab.reset( aWork.getHistory());
		if ( fOrderTab != null )
			fOrderTab.reset(aWork.getOrders());
		Dispo dispo = raw.dispo();
		fActions.enableContactActions(raw);
		}

	public void editOrder(Order aOrder)
		{
		if ( aOrder != null )
			{
			fDlgEditOrder.setOrder(aOrder);
			fDlgEditOrder.setVisible(true);
			}
		}

	public final Order getMostRecentOrder()
		{
		OrderTableModel otm = ((OrderTableModel)fOrderTab.getModel());
		return (otm.getRowCount() > 0)? otm.get(0) : null;
		}

	protected final void log ( String msg, Object... args )
		{
		SBLog.write( getClass().getSimpleName(), String.format(msg,args));
		}

	private void sayStatus( String msg )
		{
		if ( fTitleMe != null )
			fTitleMe.setTitle(LAF.getDialogTitle(msg));
		log ( msg );
		}

	public final void refreshHistory()
		{
		if ( fHistoryTab != null ) // authorized to view history
			fHistoryTab.setContact( getRawID());
		}

	public void orderAdded( Order aOrder )
		{
		if ( fOrderTab != null )
			((OrderTableModel)fOrderTab.getModel()).add(0, aOrder);
		}
	} // 592, 680
