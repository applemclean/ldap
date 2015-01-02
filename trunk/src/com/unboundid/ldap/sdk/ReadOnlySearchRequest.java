/*
 * Copyright 2007-2015 UnboundID Corp.
 * All Rights Reserved.
 */
/*
 * Copyright (C) 2008-2015 UnboundID Corp.
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
package com.unboundid.ldap.sdk;



import java.util.List;

import com.unboundid.util.NotExtensible;
import com.unboundid.util.ThreadSafety;
import com.unboundid.util.ThreadSafetyLevel;



/**
 * This interface defines a set of methods that may be safely called in an LDAP
 * search request without altering its contents.  This interface must not be
 * implemented by any class other than {@link SearchRequest}.
 * <BR><BR>
 * This interface does not inherently provide the assurance of thread safety for
 * the methods that it exposes, because it is still possible for a thread
 * referencing the object which implements this interface to alter the request
 * using methods not included in this interface.  However, if it can be
 * guaranteed that no thread will alter the underlying object, then the methods
 * exposed by this interface can be safely invoked concurrently by any number of
 * threads.
 */
@NotExtensible()
@ThreadSafety(level=ThreadSafetyLevel.INTERFACE_NOT_THREADSAFE)
public interface ReadOnlySearchRequest
       extends ReadOnlyLDAPRequest
{
  /**
   * Retrieves the base DN for this search request.
   *
   * @return  The base DN for this search request.
   */
  String getBaseDN();



  /**
   * Retrieves the scope for this search request.
   *
   * @return  The scope for this search request.
   */
  SearchScope getScope();



  /**
   * Retrieves the dereference policy that should be used by the server for any
   * aliases encountered during search processing.
   *
   * @return  The dereference policy that should be used by the server for any
   *          aliases encountered during search processing.
   */
  DereferencePolicy getDereferencePolicy();



  /**
   * Retrieves the maximum number of entries that should be returned by the
   * server when processing this search request.
   *
   * @return  The maximum number of entries that should be returned by the
   *          server when processing this search request, or zero if there is
   *          no limit.
   */
  int getSizeLimit();



  /**
   * Retrieves the maximum length of time in seconds that the server should
   * spend processing this search request.
   *
   * @return  The maximum length of time in seconds that the server should
   *          spend processing this search request, or zero if there is no
   *          limit.
   */
  int getTimeLimitSeconds();



  /**
   * Indicates whether the server should return only attribute names in matching
   * entries, rather than both names and values.
   *
   * @return  {@code true} if matching entries should include only attribute
   *          names, or {@code false} if matching entries should include both
   *          attribute names and values.
   */
  boolean typesOnly();



  /**
   * Retrieves the filter that should be used to identify matching entries.
   *
   * @return  The filter that should be used to identify matching entries.
   */
  Filter getFilter();



  /**
   * Retrieves the set of requested attributes to include in matching entries.
   *
   * @return  The set of requested attributes to include in matching entries, or
   *          an empty array if the default set of attributes (all user
   *          attributes but no operational attributes) should be requested.
   */
  List<String> getAttributeList();



  /**
   * {@inheritDoc}
   */
  SearchRequest duplicate();



  /**
   * {@inheritDoc}
   */
  SearchRequest duplicate(final Control[] controls);
}
