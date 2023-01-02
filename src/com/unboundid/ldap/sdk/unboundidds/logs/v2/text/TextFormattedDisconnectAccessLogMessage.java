/*
 * Copyright 2022-2023 Ping Identity Corporation
 * All Rights Reserved.
 */
/*
 * Copyright 2022-2023 Ping Identity Corporation
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
 * Copyright (C) 2022-2023 Ping Identity Corporation
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
package com.unboundid.ldap.sdk.unboundidds.logs.v2.text;



import com.unboundid.ldap.sdk.unboundidds.logs.AccessLogMessageType;
import com.unboundid.ldap.sdk.unboundidds.logs.LogException;
import com.unboundid.ldap.sdk.unboundidds.logs.v2.DisconnectAccessLogMessage;
import com.unboundid.util.NotMutable;
import com.unboundid.util.NotNull;
import com.unboundid.util.Nullable;
import com.unboundid.util.ThreadSafety;
import com.unboundid.util.ThreadSafetyLevel;



/**
 * This class provides a data structure that holds information about a
 * text-formatted disconnect access log message.
 * <BR>
 * <BLOCKQUOTE>
 *   <B>NOTE:</B>  This class, and other classes within the
 *   {@code com.unboundid.ldap.sdk.unboundidds} package structure, are only
 *   supported for use against Ping Identity, UnboundID, and
 *   Nokia/Alcatel-Lucent 8661 server products.  These classes provide support
 *   for proprietary functionality or for external specifications that are not
 *   considered stable or mature enough to be guaranteed to work in an
 *   interoperable way with other types of LDAP servers.
 * </BLOCKQUOTE>
 */
@NotMutable()
@ThreadSafety(level=ThreadSafetyLevel.COMPLETELY_THREADSAFE)
public final class TextFormattedDisconnectAccessLogMessage
       extends TextFormattedAccessLogMessage
       implements DisconnectAccessLogMessage
{
  /**
   * The serial version UID for this serializable class.
   */
  private static final long serialVersionUID = 3518004656591505358L;



  // The disconnect reason for this log message.
  @Nullable private final String disconnectReason;

  // The disconnect reason for this log message.
  @Nullable private final String disconnectMessage;

  // The requester IP address for this log message.
  @Nullable private final String requesterIPAddress;



  /**
   * Creates a new text-formatted disconnect access log message from the
   * provided message string.
   *
   * @param  logMessageString  The string representation of this log message.
   *                           It must not be {@code null}.
   *
   * @throws  LogException  If the provided string cannot be parsed as a valid
   *                        log message.
   */
  public TextFormattedDisconnectAccessLogMessage(
              @NotNull final String logMessageString)
         throws LogException
  {
    this(new TextFormattedLogMessage(logMessageString));
  }



  /**
   * Creates a new text-formatted disconnect access log message from the
   * provided message.
   *
   * @param  logMessage  The log message to use to create this disconnect access
   *                     log message.  It must not be {@code null}.
   */
  TextFormattedDisconnectAccessLogMessage(
       @NotNull final TextFormattedLogMessage logMessage)
  {
    super(logMessage);

    disconnectReason =
         getString(TextFormattedAccessLogFields.DISCONNECT_REASON);
    disconnectMessage =
         getString(TextFormattedAccessLogFields.DISCONNECT_MESSAGE);
    requesterIPAddress =
         getString(TextFormattedAccessLogFields.REQUESTER_IP_ADDRESS);
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  @NotNull()
  public AccessLogMessageType getMessageType()
  {
    return AccessLogMessageType.DISCONNECT;
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  @Nullable()
  public String getDisconnectReason()
  {
    return disconnectReason;
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  @Nullable()
  public String getDisconnectMessage()
  {
    return disconnectMessage;
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  @Nullable()
  public String getRequesterIPAddress()
  {
    return requesterIPAddress;
  }
}
