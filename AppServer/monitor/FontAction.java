package com.apo.apps.AppServer.monitor;
/********************************************************************
* @(#)FontAction.java 1.00 20110115
* Copyright (c) 2011 by Richard T. Salamone, Jr. All rights reserved.
*
* FontAction: Extends JTable to display an arbitrary CSV file.
*
* @author Rick Salamone
* @version 1.00, 20110115 rts created
*******************************************************/
import com.shanebow.util.SBProperties;
import java.awt.event.*;
import java.awt.*;
import javax.swing.*;
import javax.swing.event.*;

/**
* Display a modal dialog, centered on the main window, which
* contains general information about both this application and 
* the system on which it is running.
*
*<P> The system information includes a running snapshot of the 
* object heap size. A button is provided to coax the JVM to 
* perform garbage collection.
*/
public final class FontAction
	extends AbstractAction
	{
	public static final String CMD_NAME="Row Sizer";
	public static final String SIZE_PROP_NAME="usr.table.font.size";

  /**
  * Constructor. 
  * 
  * @param aFrame parent window to which this dialog is attached.
  */
  public FontAction(JFrame aFrame)
		{
		super(CMD_NAME + "...");
		fFrame = aFrame;
		fSizeSlider = createSlider();
		putValue(SHORT_DESCRIPTION, CMD_NAME );
		putValue(LONG_DESCRIPTION, "Adjust the size of the table rows");
		putValue(MNEMONIC_KEY, new Integer(KeyEvent.VK_T) );
		}

	public void addChangeListener(ChangeListener l)
		{
		fSizeSlider.addChangeListener(l);
		}

	public void actionPerformed(ActionEvent e)
		{
		JLabel[] options = { new JLabel(
			"<HTML><BODY><I>Drag slider knob to size the table rows</I>") };
		JOptionPane.showOptionDialog(fFrame, fSizeSlider, CMD_NAME,
			JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE,
			null, options, null);
		}

	// PRIVATE //
	private JSlider createSlider()
		{
		SBProperties props = SBProperties.getInstance();
		int size = props.getInt(SIZE_PROP_NAME, 12);
		JSlider sizeSlider = new JSlider(JSlider.HORIZONTAL, 5, 25, size)
			{
			@Override public String toString() { return SIZE_PROP_NAME; }
			};
		sizeSlider.setMajorTickSpacing(5); // sets numbers for big tick marks
		sizeSlider.setMinorTickSpacing(1);  // smaller tick marks
		sizeSlider.setPaintTicks(true);     // display the ticks
		sizeSlider.setPaintLabels(true);    // show the numbers
		sizeSlider.setToolTipText("Font point size");
		sizeSlider.addChangeListener(new ChangeListener()
			{
			public void stateChanged(ChangeEvent e)
				{
				int fontSize = ((JSlider)e.getSource()).getValue();
				SBProperties.set(SIZE_PROP_NAME, "" + fontSize );
				}
			});
		return sizeSlider;
		}

	private JFrame fFrame;
	private JSlider fSizeSlider;
	}