package com.apo.apps.RawAdmin;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;

public final class ContactTableDelMenuItem extends JCheckBoxMenuItem
	{
	private static final ContactTableDelMenuItem _me = new ContactTableDelMenuItem();
	public static final ContactTableDelMenuItem getInstance() { return _me; }
	public static boolean checked() { return _me.isSelected(); }

	private ContactTableDelMenuItem()
		{
		super("Allow table delete");
		setSelected(false);
		}
	}
