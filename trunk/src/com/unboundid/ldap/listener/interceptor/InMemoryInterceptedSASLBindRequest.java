/*
 * Copyright 2014 UnboundID Corp.
 * All Rights Reserved.
 */
/*
 * Copyright (C) 2014 UnboundID Corp.
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
package com.unboundid.ldap.listener.interceptor;



import com.unboundid.ldap.sdk.GenericSASLBindRequest;
import com.unboundid.util.NotExtensible;
import com.unboundid.util.ThreadSafety;
import com.unboundid.util.ThreadSafetyLevel;



/**
 * This class provides an API that can be used in the course of processing a
 * SASL bind request via the {@link InMemoryOperationInterceptor} API.
 */
@NotExtensible()
@ThreadSafety(level=ThreadSafetyLevel.INTERFACE_NOT_THREADSAFE)
public interface InMemoryInterceptedSASLBindRequest
       extends InMemoryInterceptedRequest
{
  /**
   * Retrieves the bind request to be processed.
   *
   * @return  The bind request to be processed.
   */
  GenericSASLBindRequest getRequest();



  /**
   * Replaces the bind request to be processed.
   *
   * @param  bindRequest  The bind request that should be processed instead of
   *                      the one that was originally received from the client.
   *                      It must not be {@code null}.
   */
  void setRequest(final GenericSASLBindRequest bindRequest);
}
