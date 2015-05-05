/*
 * Copyright 2009-2014 UnboundID Corp.
 * All Rights Reserved.
 */
/*
 * Copyright (C) 2009-2014 UnboundID Corp.
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

import static com.unboundid.android.ldap.client.Logger.*;



/**
 * This class provides an on-click listener that is meant to dial the phone
 * with a given number when the associated view is clicked.
 */
final class PhoneNumberClickListener
      implements OnClickListener
{
  /**
   * The tag that will be used for log messages generated by this class.
   */
  private static final String LOG_TAG = "PhoneNumberListener";



  // The activity that created this on-click listener.
  private final Activity activity;

  // The number to be dialed.
  private final String number;



  /**
   * Creates a new phone number on-click listener that will dial the provided
   * telephone number when the associated view is clicked.
   *
   * @param  activity  The activity that created this on-click listener.
   * @param  number    The number to be dialed.
   */
  PhoneNumberClickListener(final Activity activity, final String number)
  {
    logEnter(LOG_TAG, "<init>", activity, number);

    this.activity = activity;
    this.number   = number;
  }



  /**
   * Indicates that the associated view was clicked and that the associated
   * number should be dialed.
   *
   * @param  view  The view that was clicked.
   */
  public void onClick(final View view)
  {
    logEnter(LOG_TAG, "onClick", view);

    final Intent i = new Intent(activity, PhoneNumberOptions.class);
    i.putExtra(PhoneNumberOptions.BUNDLE_FIELD_PHONE_NUMBER, number);
    activity.startActivity(i);
  }
}