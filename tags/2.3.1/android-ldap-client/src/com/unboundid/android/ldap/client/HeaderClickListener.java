/*
 * Copyright 2009-2012 UnboundID Corp.
 * All Rights Reserved.
 */
/*
 * Copyright (C) 2009-2012 UnboundID Corp.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPLv2 only)
 * or the terms of the GNU Lesser General Public License (LGPLv2.1 only)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see <http://www.gnu.org/licenses>.
 */
package com.unboundid.android.ldap.client;



import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.view.View.OnClickListener;

import com.unboundid.ldap.sdk.Entry;

import static com.unboundid.android.ldap.client.Logger.*;



/**
 * This class provides an on-long-click listener that is meant to display a set
 * of options whenever the user long-clicks on the header for an entry.
 */
final class HeaderClickListener
      implements OnClickListener
{
  /**
   * The tag that will be used for log messages generated by this class.
   */
  private static final String LOG_TAG = "HeaderClickListener";



  // The activity that created this on-long-click listener.
  private final Activity activity;

  // The entry for this listener.
  private final Entry entry;



  /**
   * Creates a new header on-long-click listener with the provided entry.
   *
   * @param  activity  The activity that created this on-long-click listener.
   * @param  entry     The entry for this on-click listener.
   */
  HeaderClickListener(final Activity activity, final Entry entry)
  {
    logEnter(LOG_TAG, "<init>", activity, entry);

    this.activity = activity;
    this.entry    = entry;
  }



  /**
   * Takes any appropriate action after a long click on the entry header.
   *
   * @param  view      The view for the item that was clicked.
   */
  public void onClick(final View view)
  {
    logEnter(LOG_TAG, "onClick", view);

    final Intent i = new Intent(activity, HeaderOptions.class);
    i.putExtra(HeaderOptions.BUNDLE_FIELD_ENTRY, entry);
    activity.startActivity(i);
  }
}
