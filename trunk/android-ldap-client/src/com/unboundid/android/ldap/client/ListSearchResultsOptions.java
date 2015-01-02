/*
 * Copyright 2009-2015 UnboundID Corp.
 * All Rights Reserved.
 */
/*
 * Copyright (C) 2009-2015 UnboundID Corp.
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
import android.os.Bundle;
import android.view.View;
import android.text.ClipboardManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.unboundid.ldap.sdk.Entry;

import static com.unboundid.android.ldap.client.Logger.*;



/**
 * This class provides an Android activity that may be used to display a set
 * of options to perform on a search result entry.
 */
public final class ListSearchResultsOptions
       extends Activity
       implements OnItemClickListener
{
  /**
   * The name of the field used to provide the server instance.
   */
  public static final String BUNDLE_FIELD_ENTRY = "ENTRY";



  /**
   * The tag that will be used for log messages generated by this class.
   */
  private static final String LOG_TAG = "ListResultsOptions";



  // The entry that was selected.
  private volatile Entry entry = null;



  /**
   * Performs all necessary processing when this activity is started or resumed.
   */
  @Override()
  protected void onResume()
  {
    logEnter(LOG_TAG, "onResume");

    super.onResume();

    setContentView(R.layout.layout_popup_menu);
    setTitle(R.string.activity_label);


    // Get the instance on which to operate.
    final Intent intent = getIntent();
    final Bundle extras = intent.getExtras();

    entry = (Entry) extras.getSerializable(BUNDLE_FIELD_ENTRY);
    setTitle(getString(R.string.list_search_results_options_title,
         entry.getDN()));


    // Populate the list of options.
    final String[] options =
    {
      getString(R.string.list_search_results_options_option_view_formatted),
      getString(R.string.list_search_results_options_option_view_ldif),
      getString(R.string.list_search_results_options_option_copy_dn),
      getString(R.string.list_search_results_options_option_copy_ldif)
    };

    final ListView optionList =
         (ListView) findViewById(R.id.layout_popup_menu_list_view);
    final ArrayAdapter<String> listAdapter = new ArrayAdapter<String>(this,
         android.R.layout.simple_list_item_1, options);
    optionList.setAdapter(listAdapter);
    optionList.setOnItemClickListener(this);
  }



  /**
   * Takes any appropriate action after a list item was clicked.
   *
   * @param  parent    The list containing the item that was clicked.
   * @param  item      The item that was clicked.
   * @param  position  The position of the item that was clicked.
   * @param  id        The ID of the item that was clicked.
   */
  public void onItemClick(final AdapterView<?> parent, final View item,
                          final int position, final long id)
  {
    logEnter(LOG_TAG, "onItemClick", parent, item, position, id);

    // Figure out which item was clicked and take the appropriate action.
    switch (position)
    {
      case 0:
        doViewFormatted();
        break;
      case 1:
        doViewLDIF();
        break;
      case 2:
        doCopyDN();
        break;
      case 3:
        doCopyLDIF();
        break;
      default:
        break;
    }
    finish();
  }



  /**
   * Launch the intent to view a formatted representation of the entry.
   */
  private void doViewFormatted()
  {
    final Intent i = new Intent(this, ViewEntry.class);
    i.putExtra(ViewEntry.BUNDLE_FIELD_ENTRY, entry);
    startActivity(i);
  }


  /**
   * Launch the intent to view an LDIF representation of the entry.
   */
  private void doViewLDIF()
  {
    final Intent i = new Intent(this, ViewLDIF.class);
    i.putExtra(ViewLDIF.BUNDLE_FIELD_ENTRY, entry);
    startActivity(i);
  }



  /**
   * Copies the entry DN to the clipboard.
   */
  private void doCopyDN()
  {
    logEnter(LOG_TAG, "doCopyDN");

    final ClipboardManager clipboard =
         (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
    clipboard.setText(entry.getDN());
  }



  /**
   * Copies the LDIF representation of the entry to the clipboard.
   */
  private void doCopyLDIF()
  {
    logEnter(LOG_TAG, "doCopyLDIF");

    final ClipboardManager clipboard =
         (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
    clipboard.setText(entry.toLDIFString());
  }
}
