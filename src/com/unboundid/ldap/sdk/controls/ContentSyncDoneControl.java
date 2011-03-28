/*
 * Copyright 2010-2011 UnboundID Corp.
 * All Rights Reserved.
 */
/*
 * Copyright (C) 2010-2011 UnboundID Corp.
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
package com.unboundid.ldap.sdk.controls;



import java.util.ArrayList;

import com.unboundid.asn1.ASN1Boolean;
import com.unboundid.asn1.ASN1Constants;
import com.unboundid.asn1.ASN1Element;
import com.unboundid.asn1.ASN1OctetString;
import com.unboundid.asn1.ASN1Sequence;
import com.unboundid.ldap.sdk.Control;
import com.unboundid.ldap.sdk.DecodeableControl;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.util.Debug;
import com.unboundid.util.NotMutable;
import com.unboundid.util.StaticUtils;
import com.unboundid.util.ThreadSafety;
import com.unboundid.util.ThreadSafetyLevel;

import static com.unboundid.ldap.sdk.controls.ControlMessages.*;



/**
 * This class provides an implementation of the LDAP content synchronization
 * done control as defined in
 * <a href="http://www.ietf.org/rfc/rfc4533.txt">RFC 4533</a>.  Directory
 * servers may include this control in the search result done message for a
 * search request containing the content synchronization request control.  See
 * the documentation for the {@link ContentSyncRequestControl} class for more
 * information about using the content synchronization operation.
 */
@NotMutable()
@ThreadSafety(level=ThreadSafetyLevel.COMPLETELY_THREADSAFE)
public final class ContentSyncDoneControl
       extends Control
       implements DecodeableControl
{
  /**
   * The OID (1.3.6.1.4.1.4203.1.9.1.3) for the sync done control.
   */
  public static final String SYNC_DONE_OID = "1.3.6.1.4.1.4203.1.9.1.3";



  /**
   * The serial version UID for this serializable class.
   */
  private static final long serialVersionUID = -2723009401737612274L;



  // The synchronization state cookie.
  private final ASN1OctetString cookie;

  // Indicates whether to refresh information about deleted entries.
  private final boolean refreshDeletes;



  /**
   * Creates a new empty control instance that is intended to be used only for
   * decoding controls via the {@code DecodeableControl} interface.
   */
  ContentSyncDoneControl()
  {
    cookie         = null;
    refreshDeletes = false;
  }



  /**
   * Creates a new content synchronization done control that provides updated
   * information about the state of a content synchronization session.
   *
   * @param  cookie          A cookie with an updated synchronization state.  It
   *                         may be {@code null} if no updated state is
   *                         available.
   * @param  refreshDeletes  Indicates whether the synchronization processing
   *                         has completed a delete phase.
   */
  public ContentSyncDoneControl(final ASN1OctetString cookie,
                                final boolean refreshDeletes)
  {
    super(SYNC_DONE_OID, false, encodeValue(cookie, refreshDeletes));

    this.cookie          = cookie;
    this.refreshDeletes = refreshDeletes;
  }



  /**
   * Creates a new content synchronization done control which is decoded from
   * the provided information from a generic control.
   *
   * @param  oid         The OID for the control used to create this control.
   * @param  isCritical  Indicates whether the control is marked critical.
   * @param  value       The encoded value for the control.
   *
   * @throws  LDAPException  If the provided control cannot be decoded as a
   *                         content synchronization done control.
   */
  public ContentSyncDoneControl(final String oid, final boolean isCritical,
                                final ASN1OctetString value)
         throws LDAPException
  {
    super(oid, isCritical, value);

    if (value == null)
    {
      throw new LDAPException(ResultCode.DECODING_ERROR,
           ERR_SYNC_DONE_NO_VALUE.get());
    }

    ASN1OctetString c = null;
    Boolean         r = null;

    try
    {
      final ASN1Sequence s = ASN1Sequence.decodeAsSequence(value.getValue());
      for (final ASN1Element e : s.elements())
      {
        switch (e.getType())
        {
          case ASN1Constants.UNIVERSAL_OCTET_STRING_TYPE:
            if (c == null)
            {
              c = ASN1OctetString.decodeAsOctetString(e);
            }
            else
            {
              throw new LDAPException(ResultCode.DECODING_ERROR,
                   ERR_SYNC_DONE_VALUE_MULTIPLE_COOKIES.get());
            }
            break;

          case ASN1Constants.UNIVERSAL_BOOLEAN_TYPE:
            if (r == null)
            {
              r = ASN1Boolean.decodeAsBoolean(e).booleanValue();
            }
            else
            {
              throw new LDAPException(ResultCode.DECODING_ERROR,
                   ERR_SYNC_DONE_VALUE_MULTIPLE_REFRESH_DELETE.get());
            }
            break;

          default:
            throw new LDAPException(ResultCode.DECODING_ERROR,
                 ERR_SYNC_DONE_VALUE_INVALID_ELEMENT_TYPE.get(
                      StaticUtils.toHex(e.getType())));
        }
      }
    }
    catch (final LDAPException le)
    {
      throw le;
    }
    catch (final Exception e)
    {
      Debug.debugException(e);

      throw new LDAPException(ResultCode.DECODING_ERROR,
           ERR_SYNC_DONE_VALUE_CANNOT_DECODE.get(
                StaticUtils.getExceptionMessage(e)), e);
    }

    cookie = c;

    if (r == null)
    {
      refreshDeletes = false;
    }
    else
    {
      refreshDeletes = r;
    }
  }



  /**
   * Encodes the provided information into a form suitable for use as the value
   * of this control.
   *
   * @param  cookie          A cookie with an updated synchronization state.  It
   *                         may be {@code null} if no updated state is
   *                         available.
   * @param  refreshDeletes  Indicates whether the synchronization processing
   *                         has completed a delete phase.
   *
   * @return  An ASN.1 octet string containing the encoded control value.
   */
  private static ASN1OctetString encodeValue(final ASN1OctetString cookie,
                                             final boolean refreshDeletes)
  {
    final ArrayList<ASN1Element> elements = new ArrayList<ASN1Element>(2);

    if (cookie != null)
    {
      elements.add(cookie);
    }

    if (refreshDeletes)
    {
      elements.add(new ASN1Boolean(refreshDeletes));
    }

    return new ASN1OctetString(new ASN1Sequence(elements).encode());
  }



  /**
   * {@inheritDoc}
   */
  public ContentSyncDoneControl decodeControl(final String oid,
                                              final boolean isCritical,
                                              final ASN1OctetString value)
         throws LDAPException
  {
    return new ContentSyncDoneControl(oid, isCritical, value);
  }



  /**
   * Retrieves a cookie providing updated state information for the
   * synchronization session, if available.
   *
   * @return  A cookie providing updated state information for the
   *          synchronization session, or {@code null} if none was included in
   *          the control.
   */
  public ASN1OctetString getCookie()
  {
    return cookie;
  }



  /**
   * Indicates whether the synchronization processing has completed a delete
   * phase.
   *
   * @return  {@code true} if the synchronization processing has completed a
   *          delete phase, or {@code false} if not.
   */
  public boolean refreshDeletes()
  {
    return refreshDeletes;
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public String getControlName()
  {
    return INFO_CONTROL_NAME_CONTENT_SYNC_DONE.get();
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public void toString(final StringBuilder buffer)
  {
    buffer.append("ContentSyncDoneControl(");

    if (cookie != null)
    {
      buffer.append("cookie='");
      StaticUtils.toHex(cookie.getValue(), buffer);
      buffer.append("', ");
    }

    buffer.append("refreshDeletes=");
    buffer.append(refreshDeletes);
    buffer.append(')');
  }
}