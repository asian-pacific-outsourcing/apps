package com.apo.apps.caller;
/********************************************************************
* @(#)Titled.java 1.00 20110116
* Copyright 2010 by Richard T. Salamone, Jr. All rights reserved.
*
* Titled: Interface to denote an object that has a title property.
* Useful for when we want to add a title to our parent who might
* be a frame or a dialog - Their common ancestor does not support
* get/setTitle - but dialogs and frames do!!
*
* @author Rick Salamone
* @version 1.00, 20110116
*******************************************************/

public interface Titled
	{
	public void setTitle(String title);
	public String getTitle();
	}
