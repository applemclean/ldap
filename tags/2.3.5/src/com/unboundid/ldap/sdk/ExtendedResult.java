/*
 * Copyright 2007-2013 UnboundID Corp.
 * All Rights Reserved.
 */
/*
 * Copyright (C) 2008-2013 UnboundID Corp.
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



import java.util.ArrayList;

import com.unboundid.asn1.ASN1OctetString;
import com.unboundid.asn1.ASN1StreamReader;
import com.unboundid.asn1.ASN1StreamReaderSequence;
import com.unboundid.util.Extensible;
import com.unboundid.util.NotMutable;
import com.unboundid.util.ThreadSafety;
import com.unboundid.util.ThreadSafetyLevel;

import static com.unboundid.ldap.sdk.LDAPMessages.*;
import static com.unboundid.util.Debug.*;
import static com.unboundid.util.StaticUtils.*;



/**
 * This class provides a data structure for holding information about the result
 * of processing an extended operation.  It includes all of the generic LDAP
 * result elements as described in the {@link LDAPResult} class, but it may also
 * include the following elements:
 * <UL>
 *   <LI>Response OID -- An optional OID that can be used to identify the type
 *       of response.  This may be used if there can be different types of
 *       responses for a given request.</LI>
 *   <LI>Value -- An optional element that provides the encoded value for this
 *       response.  If a value is provided, then the encoding for the value
 *       depends on the type of extended result.</LI>
 * </UL>
 */
@Extensible()
@NotMutable()
@ThreadSafety(level=ThreadSafetyLevel.COMPLETELY_THREADSAFE)
public class ExtendedResult
       extends LDAPResult
{
  /**
   * The BER type for the extended response OID element.
   */
  private static final byte TYPE_EXTENDED_RESPONSE_OID = (byte) 0x8A;



  /**
   * The BER type for the extended response value element.
   */
  private static final byte TYPE_EXTENDED_RESPONSE_VALUE = (byte) 0x8B;



  /**
   * The serial version UID for this serializable class.
   */
  private static final long serialVersionUID = -6885923482396647963L;



  // The encoded value for this extended response, if available.
  private final ASN1OctetString value;

  // The OID for this extended response, if available.
  private final String oid;



  /**
   * Creates a new extended result with the provided information.
   *
   * @param  messageID          The message ID for the LDAP message that is
   *                            associated with this LDAP result.
   * @param  resultCode         The result code from the response.
   * @param  diagnosticMessage  The diagnostic message from the response, if
   *                            available.
   * @param  matchedDN          The matched DN from the response, if available.
   * @param  referralURLs       The set of referral URLs from the response, if
   *                            available.
   * @param  oid                The OID for this extended response, if
   *                            available.
   * @param  value              The encoded value for this extended response, if
   *                            available.
   * @param  responseControls   The set of controls from the response, if
   *                            available.
   */
  public ExtendedResult(final int messageID, final ResultCode resultCode,
                        final String diagnosticMessage, final String matchedDN,
                        final String[] referralURLs, final String oid,
                        final ASN1OctetString value,
                        final Control[] responseControls)
  {
    super(messageID, resultCode, diagnosticMessage, matchedDN, referralURLs,
          responseControls);

    this.oid   = oid;
    this.value = value;
  }



  /**
   * Creates a new extended result with the information contained in the
   * provided LDAP result.  The extended result will not have an OID or value.
   *
   * @param  result  The LDAP result whose content should be used for this
   *                 extended result.
   */
  public ExtendedResult(final LDAPResult result)
  {
    super(result);

    oid   = null;
    value = null;
  }



  /**
   * Creates a new extended result initialized from all of the elements of the
   * provided extended response.
   *
   * @param  extendedResult  The extended response to use to initialize this
   *                           extended response.
   */
  protected ExtendedResult(final ExtendedResult extendedResult)
  {
    this(extendedResult.getMessageID(), extendedResult.getResultCode(),
         extendedResult.getDiagnosticMessage(), extendedResult.getMatchedDN(),
         extendedResult.getReferralURLs(), extendedResult.getOID(),
         extendedResult.getValue(), extendedResult.getResponseControls());
  }



  /**
   * Creates a new extended result object with the provided message ID and with
   * the protocol op and controls read from the given ASN.1 stream reader.
   *
   * @param  messageID        The LDAP message ID for the LDAP message that is
   *                          associated with this extended result.
   * @param  messageSequence  The ASN.1 stream reader sequence used in the
   *                          course of reading the LDAP message elements.
   * @param  reader           The ASN.1 stream reader from which to read the
   *                          protocol op and controls.
   *
   * @return  The decoded extended result.
   *
   * @throws  LDAPException  If a problem occurs while reading or decoding data
   *                         from the ASN.1 stream reader.
   */
  static ExtendedResult readExtendedResultFrom(final int messageID,
                             final ASN1StreamReaderSequence messageSequence,
                             final ASN1StreamReader reader)
         throws LDAPException
  {
    try
    {
      final ASN1StreamReaderSequence protocolOpSequence =
           reader.beginSequence();
      final ResultCode resultCode = ResultCode.valueOf(reader.readEnumerated());

      String matchedDN = reader.readString();
      if (matchedDN.length() == 0)
      {
        matchedDN = null;
      }

      String diagnosticMessage = reader.readString();
      if (diagnosticMessage.length() == 0)
      {
        diagnosticMessage = null;
      }

      String[] referralURLs = null;
      String oid = null;
      ASN1OctetString value = null;
      while (protocolOpSequence.hasMoreElements())
      {
        final byte type = (byte) reader.peek();
        switch (type)
        {
          case TYPE_REFERRAL_URLS:
            final ArrayList<String> refList = new ArrayList<String>(1);
            final ASN1StreamReaderSequence refSequence = reader.beginSequence();
            while (refSequence.hasMoreElements())
            {
              refList.add(reader.readString());
            }
            referralURLs = new String[refList.size()];
            refList.toArray(referralURLs);
            break;

          case TYPE_EXTENDED_RESPONSE_OID:
            oid = reader.readString();
            break;

          case TYPE_EXTENDED_RESPONSE_VALUE:
            value = new ASN1OctetString(type, reader.readBytes());
            break;

          default:
            throw new LDAPException(ResultCode.DECODING_ERROR,
                 ERR_EXTENDED_RESULT_INVALID_ELEMENT.get(toHex(type)));
        }
      }

      Control[] controls = NO_CONTROLS;
      if (messageSequence.hasMoreElements())
      {
        final ArrayList<Control> controlList = new ArrayList<Control>(1);
        final ASN1StreamReaderSequence controlSequence = reader.beginSequence();
        while (controlSequence.hasMoreElements())
        {
          controlList.add(Control.readFrom(reader));
        }

        controls = new Control[controlList.size()];
        controlList.toArray(controls);
      }

      return new ExtendedResult(messageID, resultCode, diagnosticMessage,
                                matchedDN, referralURLs, oid, value, controls);
    }
    catch (LDAPException le)
    {
      debugException(le);
      throw le;
    }
    catch (Exception e)
    {
      debugException(e);
      throw new LDAPException(ResultCode.DECODING_ERROR,
           ERR_EXTENDED_RESULT_CANNOT_DECODE.get(getExceptionMessage(e)), e);
    }
  }



  /**
   * Retrieves the OID for this extended result, if available.
   *
   * @return  The OID for this extended result, or {@code null} if none is
   *          available.
   */
  public final String getOID()
  {
    return oid;
  }



  /**
   * Indicates whether this extended result has a value.
   *
   * @return  {@code true} if this extended result has a value, or
   *          {@code false} if not.
   */
  public final boolean hasValue()
  {
    return (value != null);
  }



  /**
   * Retrieves the encoded value for this extended result, if available.
   *
   * @return  The encoded value for this extended result, or {@code null} if
   *          none is available.
   */
  public final ASN1OctetString getValue()
  {
    return value;
  }



  /**
   * Retrieves the user-friendly name for the extended result, if available.
   * If no user-friendly name has been defined, but a response OID is available,
   * then that will be returned.  If neither a user-friendly name nor a response
   * OID are available, then {@code null} will be returned.
   *
   * @return  The user-friendly name for this extended request, the response OID
   *          if a user-friendly name is not available but a response OID is, or
   *          {@code null} if neither a user-friendly name nor a response OID
   *          are available.
   */
  public String getExtendedResultName()
  {
    // By default, we will return the OID (which may be null).  Subclasses
    // should override this to provide the user-friendly name.
    return oid;
  }



  /**
   * Retrieves a string representation of this extended response.
   *
   * @return  A string representation of this extended response.
   */
  @Override()
  public String toString()
  {
    final StringBuilder buffer = new StringBuilder();
    toString(buffer);
    return buffer.toString();
  }



  /**
   * Appends a string representation of this extended response to the provided
   * buffer.
   *
   * @param  buffer  The buffer to which a string representation of this
   *                 extended response will be appended.
   */
  @Override()
  public void toString(final StringBuilder buffer)
  {
    buffer.append("ExtendedResult(resultCode=");
    buffer.append(getResultCode());

    final int messageID = getMessageID();
    if (messageID >= 0)
    {
      buffer.append(", messageID=");
      buffer.append(messageID);
    }

    final String diagnosticMessage = getDiagnosticMessage();
    if (diagnosticMessage != null)
    {
      buffer.append(", diagnosticMessage='");
      buffer.append(diagnosticMessage);
      buffer.append('\'');
    }

    final String matchedDN = getMatchedDN();
    if (matchedDN != null)
    {
      buffer.append(", matchedDN='");
      buffer.append(matchedDN);
      buffer.append('\'');
    }

    final String[] referralURLs = getReferralURLs();
    if (referralURLs.length > 0)
    {
      buffer.append(", referralURLs={");
      for (int i=0; i < referralURLs.length; i++)
      {
        if (i > 0)
        {
          buffer.append(", ");
        }

        buffer.append(referralURLs[i]);
      }
      buffer.append('}');
    }

    if (oid != null)
    {
      buffer.append(", oid=");
      buffer.append(oid);
    }

    final Control[] responseControls = getResponseControls();
    if (responseControls.length > 0)
    {
      buffer.append(", responseControls={");
      for (int i=0; i < responseControls.length; i++)
      {
        if (i > 0)
        {
          buffer.append(", ");
        }

        buffer.append(responseControls[i]);
      }
      buffer.append('}');
    }

    buffer.append(')');
  }
}