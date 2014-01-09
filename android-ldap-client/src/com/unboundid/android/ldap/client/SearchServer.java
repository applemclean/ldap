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



import java.util.LinkedList;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.ldap.sdk.SearchResult;
import com.unboundid.ldap.sdk.SearchResultEntry;

import static com.unboundid.android.ldap.client.Logger.*;
import static com.unboundid.util.StaticUtils.*;



/**
 * This class provides an Android activity that may be used to search a
 * directory server.  It allows the user to enter the search criteria.
 */
public final class SearchServer
       extends Activity
       implements OnClickListener, StringProvider
{
  /**
   * The name of the field used to hold the serialized instance.
   */
  public static final String BUNDLE_FIELD_INSTANCE = "SEARCH_SERVER_INSTANCE";



  /**
   * The name of the field used to hold the search type.
   */
  public static final String BUNDLE_FIELD_TYPE = "SEARCH_SERVER_TYPE";



  /**
   * The name of the field used to hold the search text.
   */
  public static final String BUNDLE_FIELD_TEXT = "SEARCH_SERVER_TEXT";



  /**
   * The tag that will be used for log messages generated by this class.
   */
  private static final String LOG_TAG = "SearchServer";



  // The position of the selected search type.
  private int searchTypePosition = -1;

  // The progress dialog displayed while the search is in progress.
  private volatile ProgressDialog progressDialog = null;

  // The server instance to search.
  private ServerInstance instance = null;

  // The search text.
  private String searchText = null;



  /**
   * Performs all necessary processing when this activity is created.
   *
   * @param  state  The state information for this activity.
   */
  @Override()
  protected void onCreate(final Bundle state)
  {
    logEnter(LOG_TAG, "onCreate", state);

    super.onCreate(state);

    final Intent i = getIntent();
    final Bundle extras = i.getExtras();
    restoreState(extras);
  }



  /**
   * Performs all necessary processing when this activity is started or resumed.
   */
  @Override()
  protected void onResume()
  {
    logEnter(LOG_TAG, "onResume");

    super.onResume();

    setContentView(R.layout.layout_search_server);
    setTitle(R.string.activity_label);

    // Populate the list of available search types.
    final Spinner typeSpinner =
         (Spinner) findViewById(R.id.layout_search_server_spinner_search_type);
    final ArrayAdapter<CharSequence> typeAdapter =
         ArrayAdapter.createFromResource(this,
              R.array.search_server_search_type_list,
              android.R.layout.simple_spinner_item);
    typeAdapter.setDropDownViewResource(
         android.R.layout.simple_spinner_dropdown_item);
    typeSpinner.setAdapter(typeAdapter);
    typeSpinner.setSelection(searchTypePosition);

    // Populate the search text.
    final EditText textField =
         (EditText) findViewById(R.id.layout_search_server_field_search_text);
    textField.setText(searchText);

    // Add an on-click listener to the search button.
    final Button searchButton =
         (Button) findViewById(R.id.layout_search_server_button_search);
    searchButton.setOnClickListener(this);
  }



  /**
   * Performs all necessary processing when the instance state needs to be
   * saved.
   *
   * @param  state  The state information to be saved.
   */
  @Override()
  protected void onSaveInstanceState(final Bundle state)
  {
    logEnter(LOG_TAG, "onSaveInstanceState", state);

    saveState(state);
  }



  /**
   * Performs all necessary processing when the instance state needs to be
   * restored.
   *
   * @param  state  The state information to be restored.
   */
  @Override()
  protected void onRestoreInstanceState(final Bundle state)
  {
    logEnter(LOG_TAG, "onRestoreInstanceState", state);

    restoreState(state);
  }



  /**
   * Takes any appropriate action after a button has been clicked.
   *
   * @param  view  The view for the button that was clicked.
   */
  public void onClick(final View view)
  {
    logEnter(LOG_TAG, "onClick", view);

    // There is only one button that can be clicked, so the user wants to
    // perform the search.

    // First, make sure that a search text value has been provided.
    final EditText textField =
         (EditText) findViewById(R.id.layout_search_server_field_search_text);
    searchText = textField.getText().toString();
    if (searchText.length() == 0)
    {
      final Intent i = new Intent(this, PopUp.class);
      i.putExtra(PopUp.BUNDLE_FIELD_TITLE,
           getString(R.string.search_server_popup_title_no_search_text));
      i.putExtra(PopUp.BUNDLE_FIELD_TEXT,
           getString(R.string.search_server_popup_text_no_search_text));
      startActivity(i);
      return;
    }

    // Determine the type of search to perform. and build the filter from it.
    final String filterString;
    final Spinner typeSpinner =
         (Spinner) findViewById(R.id.layout_search_server_spinner_search_type);
    switch (typeSpinner.getSelectedItemPosition())
    {
      case 0:
        filterString = "(sn=" + searchText + ')';
        break;
      case 1:
        filterString = "(givenName=" + searchText + ')';
        break;
      case 2:
        filterString = "(cn=" + searchText + ')';
        break;
      case 3:
        filterString = "(mail=" + searchText + ')';
        break;
      case 4:
        filterString = "(uid=" + searchText + ')';
        break;
      case 5:
        filterString = searchText;
        break;
      default:
        return;
    }

    // Make sure that the filter is valid.
    final Filter filter;
    try
    {
      filter = Filter.create(filterString);
    }
    catch (LDAPException le)
    {
      logException(LOG_TAG, "onClick", le);

      final Intent i = new Intent(this, PopUp.class);
      i.putExtra(PopUp.BUNDLE_FIELD_TITLE,
           getString(R.string.search_server_popup_title_invalid_filter));
      i.putExtra(PopUp.BUNDLE_FIELD_TEXT,
           getString(R.string.search_server_popup_text_invalid_filter,
                filterString, getExceptionMessage(le)));
      startActivity(i);
      return;
    }


    // Create a thread to process the search.
    final SearchThread searchThread = new SearchThread(this, instance, filter);
    searchThread.start();


    // Create a progress dialog to display while the search is in progress.
    progressDialog = new ProgressDialog(this);
    progressDialog.setTitle(getString(
         R.string.search_server_progress_searching));
    progressDialog.setIndeterminate(true);
    progressDialog.setCancelable(false);
    progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
    progressDialog.show();
  }



  /**
   * Indicates that the search is complete.
   *
   * @param  result  The result returned from the search.
   */
  void searchDone(final SearchResult result)
  {
    logEnter(LOG_TAG, "searchDone", result);

    final ResultCode resultCode = result.getResultCode();
    if (resultCode != ResultCode.SUCCESS)
    {
      final Intent i = new Intent(this, PopUp.class);
      switch (resultCode.intValue())
      {
        case ResultCode.SIZE_LIMIT_EXCEEDED_INT_VALUE:
          i.putExtra(PopUp.BUNDLE_FIELD_TITLE, getString(
               R.string.search_server_popup_title_size_limit_exceeded));
          i.putExtra(PopUp.BUNDLE_FIELD_TEXT,
               getString(R.string.search_server_popup_text_size_limit_exceeded,
                    SearchThread.SIZE_LIMIT));
          break;

        case ResultCode.TIME_LIMIT_EXCEEDED_INT_VALUE:
          i.putExtra(PopUp.BUNDLE_FIELD_TITLE, getString(
               R.string.search_server_popup_title_time_limit_exceeded));
          i.putExtra(PopUp.BUNDLE_FIELD_TEXT,
               getString(R.string.search_server_popup_text_time_limit_exceeded,
                    SearchThread.TIME_LIMIT_SECONDS));
          break;

        default:
          i.putExtra(PopUp.BUNDLE_FIELD_TITLE,
               getString(R.string.search_server_popup_title_search_error));
          i.putExtra(PopUp.BUNDLE_FIELD_TEXT,
               getString(R.string.search_server_popup_text_search_error,
                    result.getDiagnosticMessage(),
                    result.getResultCode().getName()));
          break;
      }

      progressDialog.dismiss();
      startActivity(i);
      return;
    }

    final int entryCount = result.getEntryCount();
    if (entryCount == 0)
    {
      final Intent i = new Intent(this, PopUp.class);
      i.putExtra(PopUp.BUNDLE_FIELD_TITLE,
           getString(R.string.search_server_popup_title_no_entries_returned));
      i.putExtra(PopUp.BUNDLE_FIELD_TEXT,
           getString(R.string.search_server_popup_text_no_entries_returned));
      progressDialog.dismiss();
      startActivity(i);
    }
    else if (entryCount == 1)
    {
      final Intent i = new Intent(this, ViewEntry.class);
      i.putExtra(ViewEntry.BUNDLE_FIELD_ENTRY,
                 result.getSearchEntries().get(0));
      progressDialog.dismiss();
      startActivity(i);
    }
    else
    {
      final LinkedList<SearchResultEntry> entries =
           new LinkedList<SearchResultEntry>(result.getSearchEntries());

      final Intent i = new Intent(this, ListSearchResults.class);
      i.putExtra(ListSearchResults.BUNDLE_FIELD_INSTANCE, instance);
      i.putExtra(ListSearchResults.BUNDLE_FIELD_ENTRIES, entries);
      progressDialog.dismiss();
      startActivity(i);
    }
  }



  /**
   * Restores the state of this activity from the provided bundle.
   *
   * @param  state  The bundle containing the state information.
   */
  private void restoreState(final Bundle state)
  {
    logEnter(LOG_TAG, "restoreState", state);

    instance = (ServerInstance) state.getSerializable(BUNDLE_FIELD_INSTANCE);

    searchTypePosition = state.getInt(BUNDLE_FIELD_TYPE, 0);

    searchText = state.getString(BUNDLE_FIELD_TEXT);
    if (searchText == null)
    {
      searchText = "";
    }
  }



  /**
   * Saves the state of this activity to the provided bundle.
   *
   * @param  state  The bundle containing the state information.
   */
  private void saveState(final Bundle state)
  {
    logEnter(LOG_TAG, "saveState", state);

    state.putSerializable(BUNDLE_FIELD_INSTANCE, instance);

    final Spinner typeSpinner =
         (Spinner) findViewById(R.id.layout_search_server_spinner_search_type);
    searchTypePosition = typeSpinner.getSelectedItemPosition();
    state.putInt(BUNDLE_FIELD_TYPE, searchTypePosition);

    final EditText textField =
         (EditText) findViewById(R.id.layout_search_server_field_search_text);
    searchText = textField.getText().toString();
    state.putString(BUNDLE_FIELD_TEXT, searchText);
  }
}
