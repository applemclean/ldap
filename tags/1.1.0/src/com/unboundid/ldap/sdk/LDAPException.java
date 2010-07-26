/*
 * Copyright 2007-2009 UnboundID Corp.
 * All Rights Reserved.
 */
/*
 * Copyright (C) 2008-2009 UnboundID Corp.
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



import com.unboundid.util.LDAPSDKException;
import com.unboundid.util.NotExtensible;
import com.unboundid.util.NotMutable;
import com.unboundid.util.ThreadSafety;
import com.unboundid.util.ThreadSafetyLevel;



/**
 * This class defines an exception that can be thrown if a problem occurs while
 * performing LDAP-related processing.  An LDAP exception can include all of
 * the elements of an {@link LDAPResult}, so that all of the response elements
 * will be available.
 */
@NotExtensible()
@NotMutable()
@ThreadSafety(level=ThreadSafetyLevel.COMPLETELY_THREADSAFE)
public class LDAPException
       extends LDAPSDKException
{
  /**
   * The serial version UID for this serializable class.
   */
  private static final long serialVersionUID = -4257171063946350327L;



  /**
   * An empty array that will be used when no controls were provided.
   */
  protected static final Control[] NO_CONTROLS = new Control[0];



  /**
   * An empty array that will be used when no referrals were provided.
   */
  protected static final String[] NO_REFERRALS = new String[0];



  // The set of response controls for this LDAP exception.
  private final Control[] responseControls;

  // The result code for this LDAP exception.
  private final ResultCode resultCode;

  // The set of referral URLs for this LDAP exception.
  private final String[] referralURLs;

  // The matched DN for this LDAP exception.
  private final String matchedDN;



  /**
   * Creates a new LDAP exception with the provided result code.  A default
   * message (based on the result code) will be used.
   *
   * @param  resultCode  The result code for this LDAP exception.
   */
  public LDAPException(final ResultCode resultCode)
  {
    super(resultCode.getName());

    this.resultCode = resultCode;

    matchedDN        = null;
    referralURLs     = NO_REFERRALS;
    responseControls = NO_CONTROLS;
  }



  /**
   * Creates a new LDAP exception with the provided result code.  A default
   * message (based on the result code) will be used.
   *
   * @param  resultCode  The result code for this LDAP exception.
   * @param  cause       The underlying exception that triggered this exception.
   */
  public LDAPException(final ResultCode resultCode, final Throwable cause)
  {
    super(resultCode.getName(), cause);

    this.resultCode = resultCode;

    matchedDN        = null;
    referralURLs     = NO_REFERRALS;
    responseControls = NO_CONTROLS;
  }



  /**
   * Creates a new LDAP exception with the provided result code and message.
   *
   * @param  resultCode    The result code for this LDAP exception.
   * @param  errorMessage  The error message for this LDAP exception.
   */
  public LDAPException(final ResultCode resultCode, final String errorMessage)
  {
    super(errorMessage);

    this.resultCode = resultCode;

    matchedDN        = null;
    referralURLs     = NO_REFERRALS;
    responseControls = NO_CONTROLS;
  }



  /**
   * Creates a new LDAP exception with the provided result code and message.
   *
   * @param  resultCode    The result code for this LDAP exception.
   * @param  errorMessage  The error message for this LDAP exception.
   * @param  cause         The underlying exception that triggered this
   *                       exception.
   */
  public LDAPException(final ResultCode resultCode, final String errorMessage,
                       final Throwable cause)
  {
    super(errorMessage, cause);

    this.resultCode = resultCode;

    matchedDN        = null;
    referralURLs     = NO_REFERRALS;
    responseControls = NO_CONTROLS;
  }



  /**
   * Creates a new LDAP exception with the provided information.
   *
   * @param  resultCode    The result code for this LDAP exception.
   * @param  errorMessage  The error message for this LDAP exception.
   * @param  matchedDN     The matched DN for this LDAP exception.
   * @param  referralURLs  The set of referral URLs for this LDAP exception.
   */
  public LDAPException(final ResultCode resultCode, final String errorMessage,
                       final String matchedDN, final String[] referralURLs)
  {
    super(errorMessage);

    this.resultCode = resultCode;
    this.matchedDN  = matchedDN;

    if (referralURLs == null)
    {
      this.referralURLs = NO_REFERRALS;
    }
    else
    {
      this.referralURLs = referralURLs;
    }

    responseControls = NO_CONTROLS;
  }



  /**
   * Creates a new LDAP exception with the provided information.
   *
   * @param  resultCode    The result code for this LDAP exception.
   * @param  errorMessage  The error message for this LDAP exception.
   * @param  matchedDN     The matched DN for this LDAP exception.
   * @param  referralURLs  The set of referral URLs for this LDAP exception.
   * @param  cause         The underlying exception that triggered this
   *                       exception.
   */
  public LDAPException(final ResultCode resultCode, final String errorMessage,
                       final String matchedDN, final String[] referralURLs,
                       final Throwable cause)
  {
    super(errorMessage, cause);

    this.resultCode = resultCode;
    this.matchedDN  = matchedDN;

    if (referralURLs == null)
    {
      this.referralURLs = NO_REFERRALS;
    }
    else
    {
      this.referralURLs = referralURLs;
    }

    responseControls = NO_CONTROLS;
  }



  /**
   * Creates a new LDAP exception with the provided information.
   *
   * @param  resultCode    The result code for this LDAP exception.
   * @param  errorMessage  The error message for this LDAP exception.
   * @param  matchedDN     The matched DN for this LDAP exception.
   * @param  referralURLs  The set of referral URLs for this LDAP exception.
   * @param  controls      The set of response controls for this LDAP exception.
   */
  public LDAPException(final ResultCode resultCode, final String errorMessage,
                       final String matchedDN, final String[] referralURLs,
                       final Control[] controls)
  {
    super(errorMessage);

    this.resultCode = resultCode;
    this.matchedDN  = matchedDN;

    if (referralURLs == null)
    {
      this.referralURLs = NO_REFERRALS;
    }
    else
    {
      this.referralURLs = referralURLs;
    }

    if (controls == null)
    {
      responseControls = NO_CONTROLS;
    }
    else
    {
      responseControls = controls;
    }
  }



  /**
   * Creates a new LDAP exception with the provided information.
   *
   * @param  resultCode    The result code for this LDAP exception.
   * @param  errorMessage  The error message for this LDAP exception.
   * @param  matchedDN     The matched DN for this LDAP exception.
   * @param  referralURLs  The set of referral URLs for this LDAP exception.
   * @param  controls      The set of response controls for this LDAP exception.
   * @param  cause         The underlying exception that triggered this
   *                       exception.
   */
  public LDAPException(final ResultCode resultCode, final String errorMessage,
                       final String matchedDN, final String[] referralURLs,
                       final Control[] controls, final Throwable cause)
  {
    super(errorMessage, cause);

    this.resultCode = resultCode;
    this.matchedDN  = matchedDN;

    if (referralURLs == null)
    {
      this.referralURLs = NO_REFERRALS;
    }
    else
    {
      this.referralURLs = referralURLs;
    }

    if (controls == null)
    {
      responseControls = NO_CONTROLS;
    }
    else
    {
      responseControls = controls;
    }
  }



  /**
   * Creates a new LDAP exception using the information contained in the
   * provided LDAP result object.
   *
   * @param  ldapResult  The LDAP result object containing the information to
   *                     use for this LDAP exception.
   */
  public LDAPException(final LDAPResult ldapResult)
  {
    super((ldapResult.getDiagnosticMessage() == null)
          ? ldapResult.getResultCode().getName()
          : ldapResult.getDiagnosticMessage());

    resultCode       = ldapResult.getResultCode();
    matchedDN        = ldapResult.getMatchedDN();
    referralURLs     = ldapResult.getReferralURLs();
    responseControls = ldapResult.getResponseControls();
  }



  /**
   * Creates a new LDAP exception using the information contained in the
   * provided LDAP result object.
   *
   * @param  ldapResult  The LDAP result object containing the information to
   *                     use for this LDAP exception.
   * @param  cause       The underlying exception that triggered this exception.
   */
  public LDAPException(final LDAPResult ldapResult, final Throwable cause)
  {
    super(((ldapResult.getDiagnosticMessage() == null)
           ? ldapResult.getResultCode().getName()
           : ldapResult.getDiagnosticMessage()),
          cause);

    resultCode       = ldapResult.getResultCode();
    matchedDN        = ldapResult.getMatchedDN();
    referralURLs     = ldapResult.getReferralURLs();
    responseControls = ldapResult.getResponseControls();
  }



  /**
   * Retrieves the result code for this LDAP exception.
   *
   * @return  The result code for this LDAP exception.
   */
  public final ResultCode getResultCode()
  {
    return resultCode;
  }



  /**
   * Retrieves the matched DN for this LDAP exception.
   *
   * @return  The matched DN for this LDAP exception, or {@code null} if there
   *          is none.
   */
  public final String getMatchedDN()
  {
    return matchedDN;
  }



  /**
   * Retrieves the set of referral URLs for this LDAP exception.
   *
   * @return  The set of referral URLs for this LDAP exception, or an empty
   *          list if there are none.
   */
  public final String[] getReferralURLs()
  {
    return referralURLs;
  }



  /**
   * Indicates whether this result contains at least one control.
   *
   * @return  {@code true} if this result contains at least one control, or
   *          {@code false} if not.
   */
  public final boolean hasResponseControl()
  {
    return (responseControls.length > 0);
  }



  /**
   * Indicates whether this result contains at least one control with the
   * specified OID.
   *
   * @param  oid  The object identifier for which to make the determination.  It
   *              must not be {@code null}.
   *
   * @return  {@code true} if this result contains at least one control with
   *          the specified OID, or {@code false} if not.
   */
  public final boolean hasResponseControl(final String oid)
  {
    for (final Control c : responseControls)
    {
      if (c.getOID().equals(oid))
      {
        return true;
      }
    }

    return false;
  }



  /**
   * Retrieves the set of response controls for this LDAP exception.
   *
   * @return  The set of response controls for this LDAP exception, or an empty
   *          list if there are none.
   */
  public final Control[] getResponseControls()
  {
    return responseControls;
  }



  /**
   * Retrieves the response control with the specified OID.
   *
   * @param  oid  The OID of the control to retrieve.
   *
   * @return  The response control with the specified OID, or {@code null} if
   *          there is no such control.
   */
  public final Control getResponseControl(final String oid)
  {
    for (final Control c : responseControls)
    {
      if (c.getOID().equals(oid))
      {
        return c;
      }
    }

    return null;
  }



  /**
   * Creates a new {@code LDAPResult} object from this exception.
   *
   * @return  The {@code LDAPResult} object created from this exception.
   */
  public final LDAPResult toLDAPResult()
  {
    return new LDAPResult(-1, resultCode, getMessage(), matchedDN, referralURLs,
                          responseControls);
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public void toString(final StringBuilder buffer)
  {
    buffer.append("LDAPException(resultCode=");
    buffer.append(resultCode);

    final String errorMessage = getMessage();
    if (errorMessage != null)
    {
      buffer.append(", errorMessage='");
      buffer.append(errorMessage);
      buffer.append('\'');
    }

    if (matchedDN != null)
    {
      buffer.append(", matchedDN='");
      buffer.append(matchedDN);
      buffer.append('\'');
    }

    if (referralURLs.length > 0)
    {
      buffer.append(", referralURLs={");

      for (int i=0; i < referralURLs.length; i++)
      {
        if (i > 0)
        {
          buffer.append(", ");
        }

        buffer.append('\'');
        buffer.append(referralURLs[i]);
        buffer.append('\'');
      }

      buffer.append('}');
    }

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



  /**
   * {@inheritDoc}
   */
  @Override()
  public final String getExceptionMessage()
  {
    return toString();
  }
}