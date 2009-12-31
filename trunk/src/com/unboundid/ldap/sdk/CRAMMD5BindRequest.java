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



import java.util.logging.Level;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.sasl.Sasl;
import javax.security.sasl.SaslClient;

import com.unboundid.asn1.ASN1OctetString;
import com.unboundid.util.DebugType;
import com.unboundid.util.InternalUseOnly;
import com.unboundid.util.NotMutable;
import com.unboundid.util.ThreadSafety;
import com.unboundid.util.ThreadSafetyLevel;

import static com.unboundid.ldap.sdk.LDAPMessages.*;
import static com.unboundid.util.Debug.*;
import static com.unboundid.util.StaticUtils.*;
import static com.unboundid.util.Validator.*;



/**
 * This class provides a SASL CRAM-MD5 bind request implementation as described
 * in draft-ietf-sasl-crammd5.  The CRAM-MD5 mechanism can be used to
 * authenticate over an insecure channel without exposing the credentials
 * (although it requires that the server have access to the clear-text
 * password).    It is similar to DIGEST-MD5, but does not provide as many
 * options, and provides slightly weaker protection because the client does not
 * contribute any of the random data used during bind processing.
 * <BR><BR>
 * Elements included in a CRAM-MD5 bind request include:
 * <UL>
 *   <LI>Authentication ID -- A string which identifies the user that is
 *       attempting to authenticate.  It should be an "authzId" value as
 *       described in section 5.2.1.8 of
 *       <A HREF="http://www.ietf.org/rfc/rfc4513.txt">RFC 4513</A>.  That is,
 *       it should be either "dn:" followed by the distinguished name of the
 *       target user, or "u:" followed by the username.  If the "u:" form is
 *       used, then the mechanism used to resolve the provided username to an
 *       entry may vary from server to server.</LI>
 *   <LI>Password -- The clear-text password for the target user.</LI>
 * </UL>
 * <H2>Example</H2>
 * The following example demonstrates the process for performing a CRAM-MD5
 * bind against a directory server with a username of "john.doe" and a password
 * of "password":
 * <PRE>
 *   CRAMMD5BindRequest bindRequest =
 *        new CRAMMD5BindRequest("u:john.doe", "password");
 *   try
 *   {
 *     BindResult bindResult = connection.bind(bindRequest);
 *     // If we get here, then the bind was successful.
 *   }
 *   catch (LDAPException le)
 *   {
 *     // The bind failed for some reason.
 *   }
 * </PRE>
 */
@NotMutable()
@ThreadSafety(level=ThreadSafetyLevel.NOT_THREADSAFE)
public final class CRAMMD5BindRequest
       extends SASLBindRequest
       implements CallbackHandler
{
  /**
   * The name for the CRAM-MD5 SASL mechanism.
   */
  public static final String CRAMMD5_MECHANISM_NAME = "CRAM-MD5";



  /**
   * The serial version UID for this serializable class.
   */
  private static final long serialVersionUID = -4556570436768136483L;



  // The password for this bind request.
  private final ASN1OctetString password;

  // The message ID from the last LDAP message sent from this request.
  private int messageID = -1;

  // The authentication ID string for this bind request.
  private final String authenticationID;



  /**
   * Creates a new SASL CRAM-MD5 bind request with the provided authentication
   * ID and password.  It will not include any controls.
   *
   * @param  authenticationID  The authentication ID for this bind request.  It
   *                           must not be {@code null}.
   * @param  password          The password for this bind request.  It must not
   *                           be {@code null}.
   */
  public CRAMMD5BindRequest(final String authenticationID,
                            final String password)
  {
    this(authenticationID, new ASN1OctetString(password), NO_CONTROLS);

    ensureNotNull(password);
  }



  /**
   * Creates a new SASL CRAM-MD5 bind request with the provided authentication
   * ID and password.  It will not include any controls.
   *
   * @param  authenticationID  The authentication ID for this bind request.  It
   *                           must not be {@code null}.
   * @param  password          The password for this bind request.  It must not
   *                           be {@code null}.
   */
  public CRAMMD5BindRequest(final String authenticationID,
                            final byte[] password)
  {
    this(authenticationID, new ASN1OctetString(password), NO_CONTROLS);

    ensureNotNull(password);
  }



  /**
   * Creates a new SASL CRAM-MD5 bind request with the provided authentication
   * ID and password.  It will not include any controls.
   *
   * @param  authenticationID  The authentication ID for this bind request.  It
   *                           must not be {@code null}.
   * @param  password          The password for this bind request.  It must not
   *                           be {@code null}.
   */
  public CRAMMD5BindRequest(final String authenticationID,
                            final ASN1OctetString password)
  {
    this(authenticationID, password, NO_CONTROLS);
  }



  /**
   * Creates a new SASL CRAM-MD5 bind request with the provided authentication
   * ID, password, and set of controls.
   *
   * @param  authenticationID  The authentication ID for this bind request.  It
   *                           must not be {@code null}.
   * @param  password          The password for this bind request.  It must not
   *                           be {@code null}.
   * @param  controls          The set of controls to include in the request.
   */
  public CRAMMD5BindRequest(final String authenticationID,
                            final String password, final Control... controls)
  {
    this(authenticationID, new ASN1OctetString(password), controls);

    ensureNotNull(password);
  }



  /**
   * Creates a new SASL CRAM-MD5 bind request with the provided authentication
   * ID, password, and set of controls.
   *
   * @param  authenticationID  The authentication ID for this bind request.  It
   *                           must not be {@code null}.
   * @param  password          The password for this bind request.  It must not
   *                           be {@code null}.
   * @param  controls          The set of controls to include in the request.
   */
  public CRAMMD5BindRequest(final String authenticationID,
                            final byte[] password, final Control... controls)
  {
    this(authenticationID, new ASN1OctetString(password), controls);

    ensureNotNull(password);
  }



  /**
   * Creates a new SASL CRAM-MD5 bind request with the provided authentication
   * ID, password, and set of controls.
   *
   * @param  authenticationID  The authentication ID for this bind request.  It
   *                           must not be {@code null}.
   * @param  password          The password for this bind request.  It must not
   *                           be {@code null}.
   * @param  controls          The set of controls to include in the request.
   */
  public CRAMMD5BindRequest(final String authenticationID,
                            final ASN1OctetString password,
                            final Control... controls)
  {
    super(controls);

    ensureNotNull(authenticationID, password);

    this.authenticationID = authenticationID;
    this.password         = password;
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public String getSASLMechanismName()
  {
    return CRAMMD5_MECHANISM_NAME;
  }



  /**
   * Retrieves the authentication ID for this bind request.
   *
   * @return  The authentication ID for this bind request.
   */
  public String getAuthenticationID()
  {
    return authenticationID;
  }



  /**
   * Retrieves the string representation of the password for this bind request.
   *
   * @return  The string representation of the password for this bind request.
   */
  public String getPasswordString()
  {
    return password.stringValue();
  }



  /**
   * Retrieves the bytes that comprise the the password for this bind request.
   *
   * @return  The bytes that comprise the password for this bind request.
   */
  public byte[] getPasswordBytes()
  {
    return password.getValue();
  }



  /**
   * Sends this bind request to the target server over the provided connection
   * and returns the corresponding response.
   *
   * @param  connection  The connection to use to send this bind request to the
   *                     server and read the associated response.
   * @param  depth       The current referral depth for this request.  It should
   *                     always be one for the initial request, and should only
   *                     be incremented when following referrals.
   *
   * @return  The bind response read from the server.
   *
   * @throws  LDAPException  If a problem occurs while sending the request or
   *                         reading the response.
   */
  @Override()
  protected BindResult process(final LDAPConnection connection, final int depth)
            throws LDAPException
  {
    final SaslClient saslClient;
    final String[] mechanisms = { CRAMMD5_MECHANISM_NAME };

    try
    {
      saslClient = Sasl.createSaslClient(mechanisms, null, "ldap",
                                         connection.getConnectedAddress(), null,
                                         this);
    }
    catch (Exception e)
    {
      debugException(e);
      throw new LDAPException(ResultCode.LOCAL_ERROR,
           ERR_CRAMMD5_CANNOT_CREATE_SASL_CLIENT.get(getExceptionMessage(e)),
           e);
    }

    final SASLHelper helper = new SASLHelper(this, connection,
         CRAMMD5_MECHANISM_NAME, saslClient, getControls(),
         getResponseTimeoutMillis(connection));

    try
    {
      return helper.processSASLBind();
    }
    finally
    {
      messageID = helper.getMessageID();
    }
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public CRAMMD5BindRequest getRebindRequest(final String host, final int port)
  {
    return new CRAMMD5BindRequest(authenticationID, password, getControls());
  }



  /**
   * Handles any necessary callbacks required for SASL authentication.
   *
   * @param  callbacks  The set of callbacks to be handled.
   */
  @InternalUseOnly()
  public void handle(final Callback[] callbacks)
  {
    for (final Callback callback : callbacks)
    {
      if (callback instanceof NameCallback)
      {
        ((NameCallback) callback).setName(authenticationID);
      }
      else if (callback instanceof PasswordCallback)
      {
        ((PasswordCallback) callback).setPassword(
             password.stringValue().toCharArray());
      }
      else
      {
        // This is an unexpected callback.
        if (debugEnabled(DebugType.LDAP))
        {
          debug(Level.WARNING, DebugType.LDAP,
                "Unexpected CRAM-MD5 SASL callback of type " +
                callback.getClass().getName());
        }
      }
    }
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public int getLastMessageID()
  {
    return messageID;
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public CRAMMD5BindRequest duplicate()
  {
    return duplicate(getControls());
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public CRAMMD5BindRequest duplicate(final Control[] controls)
  {
    final CRAMMD5BindRequest bindRequest =
         new CRAMMD5BindRequest(authenticationID, password, controls);
    bindRequest.setResponseTimeoutMillis(getResponseTimeoutMillis(null));
    return bindRequest;
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public void toString(final StringBuilder buffer)
  {
    buffer.append("CRAMMD5BindRequest(authenticationID='");
    buffer.append(authenticationID);
    buffer.append('\'');

    final Control[] controls = getControls();
    if (controls.length > 0)
    {
      buffer.append(", controls={");
      for (int i=0; i < controls.length; i++)
      {
        if (i > 0)
        {
          buffer.append(", ");
        }

        buffer.append(controls[i]);
      }
      buffer.append('}');
    }

    buffer.append(')');
  }
}