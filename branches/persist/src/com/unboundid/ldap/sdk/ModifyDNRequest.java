/*
 * Copyright 2007-2010 UnboundID Corp.
 * All Rights Reserved.
 */
/*
 * Copyright (C) 2008-2010 UnboundID Corp.
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



import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import com.unboundid.asn1.ASN1Boolean;
import com.unboundid.asn1.ASN1Buffer;
import com.unboundid.asn1.ASN1BufferSequence;
import com.unboundid.asn1.ASN1Element;
import com.unboundid.asn1.ASN1OctetString;
import com.unboundid.asn1.ASN1Sequence;
import com.unboundid.ldap.protocol.LDAPMessage;
import com.unboundid.ldap.protocol.LDAPResponse;
import com.unboundid.ldap.protocol.ProtocolOp;
import com.unboundid.ldif.LDIFModifyDNChangeRecord;
import com.unboundid.util.InternalUseOnly;
import com.unboundid.util.Mutable;
import com.unboundid.util.ThreadSafety;
import com.unboundid.util.ThreadSafetyLevel;

import static com.unboundid.ldap.sdk.LDAPMessages.*;
import static com.unboundid.util.Debug.*;
import static com.unboundid.util.StaticUtils.*;
import static com.unboundid.util.Validator.*;



/**
 * This class implements the processing necessary to perform an LDAPv3 modify DN
 * operation, which can be used to rename and/or move an entry or subtree in the
 * directory.  A modify DN request contains the DN of the target entry, the new
 * RDN to use for that entry, and a flag which indicates whether to remove the
 * current RDN attribute value(s) from the entry.  It may optionally contain a
 * new superior DN, which will cause the entry to be moved below that new parent
 * entry.
 * <BR><BR>
 * Note that some directory servers may not support all possible uses of the
 * modify DN operation.  In particular, some servers may not support the use of
 * a new superior DN, especially if it may cause the entry to be moved to a
 * different database or another server.  Also, some servers may not support
 * renaming or moving non-leaf entries (i.e., entries that have one or more
 * subordinates).
 * <BR><BR>
 * {@code ModifyDNRequest} objects are mutable and therefore can be altered and
 * re-used for multiple requests.  Note, however, that {@code ModifyDNRequest}
 * objects are not threadsafe and therefore a single {@code ModifyDNRequest}
 * object instance should not be used to process multiple requests at the same
 * time.
 * <BR><BR>
 * <H2>Example</H2>
 * The following example demonstrates the process for performing a modify DN
 * operation.  In this case, it will rename "ou=People,dc=example,dc=com" to
 * "ou=Users,dc=example,dc=com".  It will not move the entry below a new parent.
 * <PRE>
 *   ModifyDNRequest modifyDNRequest =
 *        new ModifyDNRequest("ou=People,dc=example,dc=com", "ou=Users", true);
 *
 *   try
 *   {
 *     LDAPResult modifyDNResult = connection.modifyDN(modifyDNRequest);
 *
 *     System.out.println("The entry was renamed successfully.");
 *   }
 *   catch (LDAPException le)
 *   {
 *     System.err.println("The modify DN operation failed.");
 *   }
 * </PRE>
 */
@Mutable()
@ThreadSafety(level=ThreadSafetyLevel.NOT_THREADSAFE)
public final class ModifyDNRequest
       extends UpdatableLDAPRequest
       implements ReadOnlyModifyDNRequest, ResponseAcceptor, ProtocolOp
{
  /**
   * The BER type for the new superior element.
   */
  private static final byte NEW_SUPERIOR_TYPE = (byte) 0x80;



  /**
   * The serial version UID for this serializable class.
   */
  private static final long serialVersionUID = -2325552729975091008L;



  // The queue that will be used to receive response messages from the server.
  private final LinkedBlockingQueue<LDAPResponse> responseQueue =
       new LinkedBlockingQueue<LDAPResponse>();

  // Indicates whether to delete the current RDN value from the entry.
  private boolean deleteOldRDN;

  // The message ID from the last LDAP message sent from this request.
  private int messageID = -1;

  // The current DN of the entry to rename.
  private String dn;

  // The new RDN to use for the entry.
  private String newRDN;

  // The new superior DN for the entry.
  private String newSuperiorDN;



  /**
   * Creates a new modify DN request that will rename the entry but will not
   * move it below a new entry.
   *
   * @param  dn            The current DN for the entry to rename.  It must not
   *                       be {@code null}.
   * @param  newRDN        The new RDN for the target entry.  It must not be
   *                       {@code null}.
   * @param  deleteOldRDN  Indicates whether to delete the current RDN value
   *                       from the target entry.
   */
  public ModifyDNRequest(final String dn, final String newRDN,
                         final boolean deleteOldRDN)
  {
    super(null);

    ensureNotNull(dn, newRDN);

    this.dn           = dn;
    this.newRDN       = newRDN;
    this.deleteOldRDN = deleteOldRDN;

    newSuperiorDN = null;
  }



  /**
   * Creates a new modify DN request that will rename the entry but will not
   * move it below a new entry.
   *
   * @param  dn            The current DN for the entry to rename.  It must not
   *                       be {@code null}.
   * @param  newRDN        The new RDN for the target entry.  It must not be
   *                       {@code null}.
   * @param  deleteOldRDN  Indicates whether to delete the current RDN value
   *                       from the target entry.
   */
  public ModifyDNRequest(final DN dn, final RDN newRDN,
                         final boolean deleteOldRDN)
  {
    super(null);

    ensureNotNull(dn, newRDN);

    this.dn           = dn.toString();
    this.newRDN       = newRDN.toString();
    this.deleteOldRDN = deleteOldRDN;

    newSuperiorDN = null;
  }



  /**
   * Creates a new modify DN request that will rename the entry and will
   * optionally move it below a new entry.
   *
   * @param  dn             The current DN for the entry to rename.  It must not
   *                        be {@code null}.
   * @param  newRDN         The new RDN for the target entry.  It must not be
   *                        {@code null}.
   * @param  deleteOldRDN   Indicates whether to delete the current RDN value
   *                        from the target entry.
   * @param  newSuperiorDN  The new superior DN for the entry.  It may be
   *                        {@code null} if the entry is not to be moved below a
   *                        new parent.
   */
  public ModifyDNRequest(final String dn, final String newRDN,
                         final boolean deleteOldRDN, final String newSuperiorDN)
  {
    super(null);

    ensureNotNull(dn, newRDN);

    this.dn            = dn;
    this.newRDN        = newRDN;
    this.deleteOldRDN  = deleteOldRDN;
    this.newSuperiorDN = newSuperiorDN;
  }



  /**
   * Creates a new modify DN request that will rename the entry and will
   * optionally move it below a new entry.
   *
   * @param  dn             The current DN for the entry to rename.  It must not
   *                        be {@code null}.
   * @param  newRDN         The new RDN for the target entry.  It must not be
   *                        {@code null}.
   * @param  deleteOldRDN   Indicates whether to delete the current RDN value
   *                        from the target entry.
   * @param  newSuperiorDN  The new superior DN for the entry.  It may be
   *                        {@code null} if the entry is not to be moved below a
   *                        new parent.
   */
  public ModifyDNRequest(final DN dn, final RDN newRDN,
                         final boolean deleteOldRDN, final DN newSuperiorDN)
  {
    super(null);

    ensureNotNull(dn, newRDN);

    this.dn            = dn.toString();
    this.newRDN        = newRDN.toString();
    this.deleteOldRDN  = deleteOldRDN;

    if (newSuperiorDN == null)
    {
      this.newSuperiorDN = null;
    }
    else
    {
      this.newSuperiorDN = newSuperiorDN.toString();
    }
  }



  /**
   * Creates a new modify DN request that will rename the entry but will not
   * move it below a new entry.
   *
   * @param  dn            The current DN for the entry to rename.  It must not
   *                       be {@code null}.
   * @param  newRDN        The new RDN for the target entry.  It must not be
   *                       {@code null}.
   * @param  deleteOldRDN  Indicates whether to delete the current RDN value
   *                       from the target entry.
   * @param  controls      The set of controls to include in the request.
   */
  public ModifyDNRequest(final String dn, final String newRDN,
                         final boolean deleteOldRDN, final Control[] controls)
  {
    super(controls);

    ensureNotNull(dn, newRDN);

    this.dn           = dn;
    this.newRDN       = newRDN;
    this.deleteOldRDN = deleteOldRDN;

    newSuperiorDN = null;
  }



  /**
   * Creates a new modify DN request that will rename the entry but will not
   * move it below a new entry.
   *
   * @param  dn            The current DN for the entry to rename.  It must not
   *                       be {@code null}.
   * @param  newRDN        The new RDN for the target entry.  It must not be
   *                       {@code null}.
   * @param  deleteOldRDN  Indicates whether to delete the current RDN value
   *                       from the target entry.
   * @param  controls      The set of controls to include in the request.
   */
  public ModifyDNRequest(final DN dn, final RDN newRDN,
                         final boolean deleteOldRDN, final Control[] controls)
  {
    super(controls);

    ensureNotNull(dn, newRDN);

    this.dn           = dn.toString();
    this.newRDN       = newRDN.toString();
    this.deleteOldRDN = deleteOldRDN;

    newSuperiorDN = null;
  }



  /**
   * Creates a new modify DN request that will rename the entry and will
   * optionally move it below a new entry.
   *
   * @param  dn             The current DN for the entry to rename.  It must not
   *                        be {@code null}.
   * @param  newRDN         The new RDN for the target entry.  It must not be
   *                        {@code null}.
   * @param  deleteOldRDN   Indicates whether to delete the current RDN value
   *                        from the target entry.
   * @param  newSuperiorDN  The new superior DN for the entry.  It may be
   *                        {@code null} if the entry is not to be moved below a
   *                        new parent.
   * @param  controls      The set of controls to include in the request.
   */
  public ModifyDNRequest(final String dn, final String newRDN,
                         final boolean deleteOldRDN, final String newSuperiorDN,
                         final Control[] controls)
  {
    super(controls);

    ensureNotNull(dn, newRDN);

    this.dn            = dn;
    this.newRDN        = newRDN;
    this.deleteOldRDN  = deleteOldRDN;
    this.newSuperiorDN = newSuperiorDN;
  }



  /**
   * Creates a new modify DN request that will rename the entry and will
   * optionally move it below a new entry.
   *
   * @param  dn             The current DN for the entry to rename.  It must not
   *                        be {@code null}.
   * @param  newRDN         The new RDN for the target entry.  It must not be
   *                        {@code null}.
   * @param  deleteOldRDN   Indicates whether to delete the current RDN value
   *                        from the target entry.
   * @param  newSuperiorDN  The new superior DN for the entry.  It may be
   *                        {@code null} if the entry is not to be moved below a
   *                        new parent.
   * @param  controls      The set of controls to include in the request.
   */
  public ModifyDNRequest(final DN dn, final RDN newRDN,
                         final boolean deleteOldRDN, final DN newSuperiorDN,
                         final Control[] controls)
  {
    super(controls);

    ensureNotNull(dn, newRDN);

    this.dn            = dn.toString();
    this.newRDN        = newRDN.toString();
    this.deleteOldRDN  = deleteOldRDN;

    if (newSuperiorDN == null)
    {
      this.newSuperiorDN = null;
    }
    else
    {
      this.newSuperiorDN = newSuperiorDN.toString();
    }
  }



  /**
   * {@inheritDoc}
   */
  public String getDN()
  {
    return dn;
  }



  /**
   * Specifies the current DN of the entry to move/rename.
   *
   * @param  dn  The current DN of the entry to move/rename.  It must not be
   *             {@code null}.
   */
  public void setDN(final String dn)
  {
    ensureNotNull(dn);

    this.dn = dn;
  }



  /**
   * Specifies the current DN of the entry to move/rename.
   *
   * @param  dn  The current DN of the entry to move/rename.  It must not be
   *             {@code null}.
   */
  public void setDN(final DN dn)
  {
    ensureNotNull(dn);

    this.dn = dn.toString();
  }



  /**
   * {@inheritDoc}
   */
  public String getNewRDN()
  {
    return newRDN;
  }



  /**
   * Specifies the new RDN for the entry.
   *
   * @param  newRDN  The new RDN for the entry.  It must not be {@code null}.
   */
  public void setNewRDN(final String newRDN)
  {
    ensureNotNull(newRDN);

    this.newRDN = newRDN;
  }



  /**
   * Specifies the new RDN for the entry.
   *
   * @param  newRDN  The new RDN for the entry.  It must not be {@code null}.
   */
  public void setNewRDN(final RDN newRDN)
  {
    ensureNotNull(newRDN);

    this.newRDN = newRDN.toString();
  }



  /**
   * {@inheritDoc}
   */
  public boolean deleteOldRDN()
  {
    return deleteOldRDN;
  }



  /**
   * Specifies whether the current RDN value should be removed from the entry.
   *
   * @param  deleteOldRDN  Specifies whether the current RDN value should be
   *                       removed from the entry.
   */
  public void setDeleteOldRDN(final boolean deleteOldRDN)
  {
    this.deleteOldRDN = deleteOldRDN;
  }



  /**
   * {@inheritDoc}
   */
  public String getNewSuperiorDN()
  {
    return newSuperiorDN;
  }



  /**
   * Specifies the new superior DN for the entry.
   *
   * @param  newSuperiorDN  The new superior DN for the entry.  It may be
   *                        {@code null} if the entry is not to be removed below
   *                        a new parent.
   */
  public void setNewSuperiorDN(final String newSuperiorDN)
  {
    this.newSuperiorDN = newSuperiorDN;
  }



  /**
   * Specifies the new superior DN for the entry.
   *
   * @param  newSuperiorDN  The new superior DN for the entry.  It may be
   *                        {@code null} if the entry is not to be removed below
   *                        a new parent.
   */
  public void setNewSuperiorDN(final DN newSuperiorDN)
  {
    if (newSuperiorDN == null)
    {
      this.newSuperiorDN = null;
    }
    else
    {
      this.newSuperiorDN = newSuperiorDN.toString();
    }
  }



  /**
   * {@inheritDoc}
   */
  public byte getProtocolOpType()
  {
    return LDAPMessage.PROTOCOL_OP_TYPE_MODIFY_DN_REQUEST;
  }



  /**
   * {@inheritDoc}
   */
  public void writeTo(final ASN1Buffer writer)
  {
    final ASN1BufferSequence requestSequence =
         writer.beginSequence(LDAPMessage.PROTOCOL_OP_TYPE_MODIFY_DN_REQUEST);
    writer.addOctetString(dn);
    writer.addOctetString(newRDN);
    writer.addBoolean(deleteOldRDN);

    if (newSuperiorDN != null)
    {
      writer.addOctetString(NEW_SUPERIOR_TYPE, newSuperiorDN);
    }
    requestSequence.end();
  }



  /**
   * Encodes the modify DN request protocol op to an ASN.1 element.
   *
   * @return  The ASN.1 element with the encoded modify DN request protocol op.
   */
  ASN1Element encodeProtocolOp()
  {
    final ASN1Element[] protocolOpElements;
    if (newSuperiorDN == null)
    {
      protocolOpElements = new ASN1Element[]
      {
        new ASN1OctetString(dn),
        new ASN1OctetString(newRDN),
        new ASN1Boolean(deleteOldRDN)
      };
    }
    else
    {
      protocolOpElements = new ASN1Element[]
      {
        new ASN1OctetString(dn),
        new ASN1OctetString(newRDN),
        new ASN1Boolean(deleteOldRDN),
        new ASN1OctetString(NEW_SUPERIOR_TYPE, newSuperiorDN)
      };
    }

    return new ASN1Sequence(LDAPMessage.PROTOCOL_OP_TYPE_MODIFY_DN_REQUEST,
                            protocolOpElements);
  }



  /**
   * Sends this modify DN request to the directory server over the provided
   * connection and returns the associated response.
   *
   * @param  connection  The connection to use to communicate with the directory
   *                     server.
   * @param  depth       The current referral depth for this request.  It should
   *                     always be one for the initial request, and should only
   *                     be incremented when following referrals.
   *
   * @return  An LDAP result object that provides information about the result
   *          of the modify DN processing.
   *
   * @throws  LDAPException  If a problem occurs while sending the request or
   *                         reading the response.
   */
  @Override()
  protected LDAPResult process(final LDAPConnection connection, final int depth)
            throws LDAPException
  {
    if (connection.synchronousMode())
    {
      return processSync(connection, depth);
    }

    final long requestTime = System.nanoTime();
    processAsync(connection, null);

    try
    {
      // Wait for and process the response.
      final LDAPResponse response;
      try
      {
        final long responseTimeout = getResponseTimeoutMillis(connection);
        if (responseTimeout > 0)
        {
          response = responseQueue.poll(responseTimeout, TimeUnit.MILLISECONDS);
        }
        else
        {
          response = responseQueue.take();
        }
      }
      catch (InterruptedException ie)
      {
        debugException(ie);
        throw new LDAPException(ResultCode.LOCAL_ERROR,
             ERR_MODDN_INTERRUPTED.get(connection.getHostPort()), ie);
      }

      return handleResponse(connection, response, requestTime, depth);
    }
    finally
    {
      connection.deregisterResponseAcceptor(messageID);
    }
  }



  /**
   * Sends this modify DN request to the directory server over the provided
   * connection and returns the message ID for the request.
   *
   * @param  connection      The connection to use to communicate with the
   *                         directory server.
   * @param  resultListener  The async result listener that is to be notified
   *                         when the response is received.  It may be
   *                         {@code null} only if the result is to be processed
   *                         by this class.
   *
   * @return  The LDAP message ID for the modify DN request that was sent to the
   *          server.
   *
   * @throws  LDAPException  If a problem occurs while sending the request.
   */
  int processAsync(final LDAPConnection connection,
                   final AsyncResultListener resultListener)
      throws LDAPException
  {
    // Create the LDAP message.
    messageID = connection.nextMessageID();
    final LDAPMessage message = new LDAPMessage(messageID, this, getControls());


    // If the provided async result listener is {@code null}, then we'll use
    // this class as the message acceptor.  Otherwise, create an async helper
    // and use it as the message acceptor.
    if (resultListener == null)
    {
      connection.registerResponseAcceptor(messageID, this);
    }
    else
    {
      final AsyncHelper helper = new AsyncHelper(connection,
           LDAPMessage.PROTOCOL_OP_TYPE_MODIFY_DN_RESPONSE, resultListener,
           getIntermediateResponseListener());
      connection.registerResponseAcceptor(messageID, helper);
    }


    // Send the request to the server.
    try
    {
      debugLDAPRequest(this);
      connection.getConnectionStatistics().incrementNumModifyDNRequests();
      connection.sendMessage(message);
      return messageID;
    }
    catch (LDAPException le)
    {
      debugException(le);

      connection.deregisterResponseAcceptor(messageID);
      throw le;
    }
  }



  /**
   * Processes this modify DN operation in synchronous mode, in which the same
   * thread will send the request and read the response.
   *
   * @param  connection  The connection to use to communicate with the directory
   *                     server.
   * @param  depth       The current referral depth for this request.  It should
   *                     always be one for the initial request, and should only
   *                     be incremented when following referrals.
   *
   * @return  An LDAP result object that provides information about the result
   *          of the modify DN processing.
   *
   * @throws  LDAPException  If a problem occurs while sending the request or
   *                         reading the response.
   */
  private LDAPResult processSync(final LDAPConnection connection,
                                 final int depth)
          throws LDAPException
  {
    // Create the LDAP message.
    messageID = connection.nextMessageID();
    final LDAPMessage message =
         new LDAPMessage(messageID,  this, getControls());


    // Set the appropriate timeout on the socket.
    try
    {
      connection.getConnectionInternals().getSocket().setSoTimeout(
           (int) getResponseTimeoutMillis(connection));
    }
    catch (Exception e)
    {
      debugException(e);
    }


    // Send the request to the server.
    final long requestTime = System.nanoTime();
    debugLDAPRequest(this);
    connection.getConnectionStatistics().incrementNumModifyDNRequests();
    connection.sendMessage(message);

    final LDAPResponse response = connection.readResponse(messageID);
    return handleResponse(connection, response, requestTime, depth);
  }



  /**
   * Performs the necessary processing for handling a response.
   *
   * @param  connection   The connection used to read the response.
   * @param  response     The response to be processed.
   * @param  requestTime  The time the request was sent to the server.
   * @param  depth        The current referral depth for this request.  It
   *                      should always be one for the initial request, and
   *                      should only be incremented when following referrals.
   *
   * @return  The modify DN result.
   *
   * @throws  LDAPException  If a problem occurs.
   */
  private LDAPResult handleResponse(final LDAPConnection connection,
                                    final LDAPResponse response,
                                    final long requestTime, final int depth)
          throws LDAPException
  {
    if (response == null)
    {
      final long waitTime = System.currentTimeMillis() - requestTime;
      throw new LDAPException(ResultCode.TIMEOUT,
           ERR_MODIFY_DN_CLIENT_TIMEOUT.get(waitTime,
                connection.getHostPort()));
    }

    connection.getConnectionStatistics().incrementNumModifyDNResponses(
         System.nanoTime() - requestTime);
    if (response instanceof ConnectionClosedResponse)
    {
      // The connection was closed while waiting for the response.
      final ConnectionClosedResponse ccr = (ConnectionClosedResponse) response;
      final String message = ccr.getMessage();
      if (message == null)
      {
        throw new LDAPException(ResultCode.SERVER_DOWN,
             ERR_CONN_CLOSED_WAITING_FOR_MODIFY_DN_RESPONSE.get(
                  connection.getHostPort(), toString()));
      }
      else
      {
        throw new LDAPException(ResultCode.SERVER_DOWN,
             ERR_CONN_CLOSED_WAITING_FOR_MODIFY_DN_RESPONSE_WITH_MESSAGE.get(
                  connection.getHostPort(), toString(), message));
      }
    }

    final LDAPResult result = (LDAPResult) response;
    if ((result.getResultCode().equals(ResultCode.REFERRAL)) &&
        followReferrals(connection))
    {
      if (depth >= connection.getConnectionOptions().getReferralHopLimit())
      {
        return new LDAPResult(messageID, ResultCode.REFERRAL_LIMIT_EXCEEDED,
                              ERR_TOO_MANY_REFERRALS.get(),
                              result.getMatchedDN(), result.getReferralURLs(),
                              result.getResponseControls());
      }

      return followReferral(result, connection, depth);
    }
    else
    {
      return result;
    }
  }



  /**
   * Attempts to follow a referral to perform a modify DN operation in the
   * target server.
   *
   * @param  referralResult  The LDAP result object containing information about
   *                         the referral to follow.
   * @param  connection      The connection on which the referral was received.
   * @param  depth           The number of referrals followed in the course of
   *                         processing this request.
   *
   * @return  The result of attempting to process the modify DN operation by
   *          following the referral.
   *
   * @throws  LDAPException  If a problem occurs while attempting to establish
   *                         the referral connection, sending the request, or
   *                         reading the result.
   */
  private LDAPResult followReferral(final LDAPResult referralResult,
                                    final LDAPConnection connection,
                                    final int depth)
          throws LDAPException
  {
    for (final String urlString : referralResult.getReferralURLs())
    {
      try
      {
        final LDAPURL referralURL = new LDAPURL(urlString);
        final String host = referralURL.getHost();

        if (host == null)
        {
          // We can't handle a referral in which there is no host.
          continue;
        }

        final ModifyDNRequest modifyDNRequest;
        if (referralURL.baseDNProvided())
        {
          modifyDNRequest =
               new ModifyDNRequest(referralURL.getBaseDN().toString(),
                                   newRDN, deleteOldRDN, newSuperiorDN,
                                   getControls());
        }
        else
        {
          modifyDNRequest = this;
        }

        final LDAPConnection referralConn =
             connection.getReferralConnection(referralURL, connection);
        try
        {
          return modifyDNRequest.process(referralConn, depth+1);
        }
        finally
        {
          referralConn.setDisconnectInfo(DisconnectType.REFERRAL, null, null);
          referralConn.close();
        }
      }
      catch (LDAPException le)
      {
        debugException(le);
      }
    }

    // If we've gotten here, then we could not follow any of the referral URLs,
    // so we'll just return the original referral result.
    return referralResult;
  }



  /**
   * {@inheritDoc}
   */
  @InternalUseOnly()
  public void responseReceived(final LDAPResponse response)
         throws LDAPException
  {
    try
    {
      responseQueue.put(response);
    }
    catch (Exception e)
    {
      debugException(e);
      throw new LDAPException(ResultCode.LOCAL_ERROR,
           ERR_EXCEPTION_HANDLING_RESPONSE.get(getExceptionMessage(e)), e);
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
  public ModifyDNRequest duplicate()
  {
    return duplicate(getControls());
  }



  /**
   * {@inheritDoc}
   */
  public ModifyDNRequest duplicate(final Control[] controls)
  {
    final ModifyDNRequest r = new ModifyDNRequest(dn, newRDN, deleteOldRDN,
         newSuperiorDN, controls);

    if (followReferralsInternal() != null)
    {
      r.setFollowReferrals(followReferralsInternal());
    }

    r.setResponseTimeoutMillis(getResponseTimeoutMillis(null));

    return r;
  }



  /**
   * {@inheritDoc}
   */
  public LDIFModifyDNChangeRecord toLDIFChangeRecord()
  {
    return new LDIFModifyDNChangeRecord(this);
  }



  /**
   * {@inheritDoc}
   */
  public String[] toLDIF()
  {
    return toLDIFChangeRecord().toLDIF();
  }



  /**
   * {@inheritDoc}
   */
  public String toLDIFString()
  {
    return toLDIFChangeRecord().toLDIFString();
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public void toString(final StringBuilder buffer)
  {
    buffer.append("ModifyDNRequest(dn='");
    buffer.append(dn);
    buffer.append("', newRDN='");
    buffer.append(newRDN);
    buffer.append("', deleteOldRDN=");
    buffer.append(deleteOldRDN);

    if (newSuperiorDN != null)
    {
      buffer.append(", newSuperiorDN='");
      buffer.append(newSuperiorDN);
      buffer.append('\'');
    }

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
