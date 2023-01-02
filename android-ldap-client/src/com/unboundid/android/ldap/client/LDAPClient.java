/*
 * Copyright 2009-2023 Ping Identity Corporation
 * All Rights Reserved.
 */
/*
 * Copyright 2009-2023 Ping Identity Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/*
 * Copyright (C) 2009-2023 Ping Identity Corporation
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



import java.util.Collections;
import java.util.LinkedList;
import java.util.Map;
import java.util.StringTokenizer;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import static com.unboundid.android.ldap.client.Logger.*;
import static com.unboundid.util.StaticUtils.*;



/**
 * This class provides an Android activity that is the entry point for the
 * UnboundID LDAP client.  It may be used to list the set of available directory
 * servers, and allows the user to select a server, create a new server, or edit
 * or remove an existing server.
 */
public final class LDAPClient
       extends Activity
       implements OnClickListener, OnItemClickListener, OnItemLongClickListener
{
  /**
   * The tag that will be used for log messages generated by this class.
   */
  private static final String LOG_TAG = "LDAPClient";



  // The set of defined server instances.
  private volatile Map<String,ServerInstance> instances = null;



  /**
   * Creates a new instance of this class.
   */
  public LDAPClient()
  {
    logEnter(LOG_TAG, "<init>");

    // No implementation required.
  }



  /**
   * Performs all appropriate processing needed whenever this activity is
   * started or resumed.
   */
  @Override()
  protected void onResume()
  {
    logEnter(LOG_TAG, "onResume");

    super.onResume();

    setContentView(R.layout.layout_list_servers);
    setTitle(R.string.activity_label);


    // Get the list of defined instances.
    try
    {
      instances = ServerInstance.getInstances(this);
    }
    catch (final Exception e)
    {
      logException(LOG_TAG, "onResume", e);

      final Intent i = new Intent(this, PopUp.class);
      i.putExtra(PopUp.BUNDLE_FIELD_TITLE,
           getString(R.string.ldap_client_popup_title_error));
      i.putExtra(PopUp.BUNDLE_FIELD_TEXT,
           getString(R.string.ldap_client_popup_text_error,
                getExceptionMessage(e)));
      startActivity(i);

      instances = Collections.emptyMap();
    }


    // Populate the list of servers.
    final StringBuilder buffer = new StringBuilder();
    final LinkedList<String> listStrings = new LinkedList<String>();
    for (final ServerInstance i : instances.values())
    {
      buffer.setLength(0);
      buffer.append(i.getID());
      buffer.append(EOL);
      buffer.append(i.getHost());
      buffer.append(':');
      buffer.append(i.getPort());

      if (i.useSSL())
      {
        buffer.append(' ');
        buffer.append(getString(R.string.ldap_client_parenthetical_using_ssl));
      }
      else if (i.useStartTLS())
      {
        buffer.append(' ');
        buffer.append(getString(
             R.string.ldap_client_parenthetical_using_start_tls));
      }

      listStrings.add(buffer.toString());
    }

    final ListView serverList =
         (ListView) findViewById(R.id.layout_list_servers_panel_list_servers);
    final ArrayAdapter<String> listAdapter = new ArrayAdapter<String>(this,
         android.R.layout.simple_list_item_1, listStrings);
    serverList.setAdapter(listAdapter);
    serverList.setTextFilterEnabled(true);
    serverList.setOnItemClickListener(this);
    serverList.setOnItemLongClickListener(this);
    serverList.refreshDrawableState();


    // Add an on-click listener to the "Define a New Server" button.
    final Button addButton =
         (Button) findViewById(R.id.layout_list_servers_button_new_server);
    addButton.setOnClickListener(this);
    addButton.setEnabled(true);
  }



  /**
   * Takes any appropriate action after a button has been clicked.
   *
   * @param  view  The view for the button that was clicked.
   */
  public void onClick(final View view)
  {
    logEnter(LOG_TAG, "onClick", view);

    // This must have been the "New Server" button.
    if (view.getId() == R.id.layout_list_servers_button_new_server)
    {
      final Intent i = new Intent(this, AddServer.class);
      startActivity(i);
    }
  }



  /**
   * Takes any appropriate action after a list item has been long-clicked.
   *
   * @param  parent    The view for the list in which the item exists.
   * @param  view      The view for the item that was clicked.
   * @param  position  The position of the item in the list.
   * @param  id        The ID of the item that was clicked.
   */
  public void onItemClick(final AdapterView<?> parent, final View view,
                          final int position, final long id)
  {
    logEnter(LOG_TAG, "onItemClick", parent, view, position, id);

    final TextView textView = (TextView) view;
    final StringTokenizer tokenizer =
         new StringTokenizer(textView.getText().toString(), "\r\n");
    final ServerInstance instance = instances.get(tokenizer.nextToken());
    search(instance);
  }



  /**
   * Takes any appropriate action after a list item has been clicked.
   *
   * @param  parent    The view for the list in which the item exists.
   * @param  view      The view for the item that was clicked.
   * @param  position  The position of the item in the list.
   * @param  id        The ID of the item that was clicked.
   *
   * @return  {@code true} if this method consumed the click, or {@code false}
   *          if not.
   */
  public boolean onItemLongClick(final AdapterView<?> parent, final View view,
                                 final int position, final long id)
  {
    logEnter(LOG_TAG, "onItemLongClick", parent, view, position, id);

    final TextView textView = (TextView) view;
    final StringTokenizer tokenizer =
         new StringTokenizer(textView.getText().toString(), "\r\n");
    final ServerInstance instance = instances.get(tokenizer.nextToken());

    final Intent i = new Intent(this, ListServerOptions.class);
    i.putExtra(ListServerOptions.BUNDLE_FIELD_INSTANCE, instance);
    startActivity(i);
    return logReturn(LOG_TAG, "onItemLongClick", true);
  }



  /**
   * Displays the form to search the selected server instance.
   *
   * @param  instance  The instance in which to perform the search.
   */
  private void search(final ServerInstance instance)
  {
    logEnter(LOG_TAG, "search", instance);

    final Intent i = new Intent(this, SearchServer.class);
    i.putExtra(SearchServer.BUNDLE_FIELD_INSTANCE, instance);
    startActivity(i);
  }
}
