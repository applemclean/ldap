/*
 * Copyright 2022 Ping Identity Corporation
 * All Rights Reserved.
 */
/*
 * Copyright 2022 Ping Identity Corporation
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
 * Copyright (C) 2022 Ping Identity Corporation
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
package com.unboundid.ldap.sdk.unboundidds.controls;



import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.unboundid.asn1.ASN1OctetString;
import com.unboundid.ldap.sdk.Control;
import com.unboundid.ldap.sdk.DecodeableControl;
import com.unboundid.ldap.sdk.IntermediateResponse;
import com.unboundid.ldap.sdk.JSONControlDecodeHelper;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.LDAPResult;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.ldap.sdk.SearchResultEntry;
import com.unboundid.ldap.sdk.SearchResultReference;
import com.unboundid.util.Debug;
import com.unboundid.util.NotMutable;
import com.unboundid.util.NotNull;
import com.unboundid.util.Nullable;
import com.unboundid.util.StaticUtils;
import com.unboundid.util.ThreadSafety;
import com.unboundid.util.ThreadSafetyLevel;
import com.unboundid.util.json.JSONArray;
import com.unboundid.util.json.JSONField;
import com.unboundid.util.json.JSONObject;
import com.unboundid.util.json.JSONValue;

import static com.unboundid.ldap.sdk.unboundidds.controls.ControlMessages.*;



/**
 * This class provides an implementation of a response control that may be used
 * to encapsulate a set of one or more other controls represented as JSON
 * objects.
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
 * <BR>
 * This control has an OID of 1.3.6.1.4.1.30221.2.5.65, and it takes a value
 * that must be a JSON object that contains a single field, {@code controls},
 * whose value is an array of the JSON representations of the response controls
 * returned by the server.  The JSON representations of the controls is the one
 * generated by the {@link Control#toJSONControl()} method, and is the one
 * expected by the {@link Control#decodeJSONControl} method.  In particular,
 * each control should have at least an {@code oid} field that specifies the OID
 * for the control, and a {@code criticality} field that indicates whether the
 * control is considered critical.  If the control has a value, then either the
 * {@code value-base64} field should be used to provide a base64-encoded
 * representation of the value, or the {@code value-json} field should be used
 * to provide a JSON-formatted representation of the value for controls that
 * support it.
 * <BR><BR>
 * As with all response controls, the criticality for JSON-formatted response
 * controls should be {@code false}.
 *
 * @see  JSONFormattedRequestControl
 */
@NotMutable()
@ThreadSafety(level=ThreadSafetyLevel.COMPLETELY_THREADSAFE)
public final class JSONFormattedResponseControl
       extends Control
       implements DecodeableControl
{
  /**
   * The OID (1.3.6.1.4.1.30221.2.5.64) for the JSON-formatted response control.
   */
  @NotNull public static final  String JSON_FORMATTED_RESPONSE_OID =
       "1.3.6.1.4.1.30221.2.5.65";



  /**
   * The name of the field used to hold the array of embedded controls in the
   * JSON representation of this control.
   */
  @NotNull private static final String JSON_FIELD_CONTROLS = "controls";



  /**
   * The serial version UID for this serializable class.
   */
  private static final long serialVersionUID = -5437134160392183276L;



  // A JSON object with an encoded representation of the value for this control.
  @NotNull private final JSONObject encodedValue;

  // A list of the JSON objects representing embedded controls within this
  // response control.
  @NotNull private final List<JSONObject> controlObjects;



  /**
   * Creates a new empty control instance that is intended to be used only for
   * decoding controls via the {@code DecodeableControl} interface.
   */
  JSONFormattedResponseControl()
  {
    encodedValue = null;
    controlObjects = Collections.emptyList();
  }



  /**
   * Creates a new instance of this control with the specified criticality and
   * set of controls.
   *
   * @param  encodedValue    A JSON object with an encoded representation of
   *                         the value for this control.  It may be
   *                         {@code null} if the control should not have a
   *                         value.
   * @param  controlObjects  A collection of JSON objects representing the
   *                         response controls generated by the server.  It
   *                         must not be {@ocde null}, and should not be empty.
   */
  private JSONFormattedResponseControl(
               @NotNull final JSONObject encodedValue,
               @NotNull final List<JSONObject> controlObjects)
  {
    super(JSON_FORMATTED_RESPONSE_OID, false,
         new ASN1OctetString(encodedValue.toSingleLineString()));

    this.encodedValue = encodedValue;
    this.controlObjects = controlObjects;
  }



  /**
   * Creates a new {@code JSONFormattedResponseControl} with the provided set of
   * embedded controls.
   *
   * @param  controls    The collection of controls to embed within this
   *                     response.  This must not be {@code null} or empty.
   *
   * @return  The {@code JSONFormattedResponseControl} that was created.
   */
  @NotNull()
  public static JSONFormattedResponseControl createWithControls(
              @NotNull final Control... controls)
  {
    return createWithControls(StaticUtils.toList(controls));
  }



  /**
   * Creates a new {@code JSONFormattedResponseControl} with the provided set of
   * embedded controls.
   *
   * @param  controls    The collection of controls to embed within this
   *                     response control.  This must not be {@code null} or
   *                     empty.
   *
   * @return  The {@code JSONFormattedResponseControl} that was created.
   */
  @NotNull()
  public static JSONFormattedResponseControl createWithControls(
              @NotNull final Collection<Control> controls)
  {
    final List<Control> controlList = new ArrayList<>(controls);
    final List<JSONObject> controlObjects = new ArrayList<>(controls.size());
    for (final Control c : controls)
    {
      controlObjects.add(c.toJSONControl());
    }

    final JSONObject encodedValue = new JSONObject(
         new JSONField(JSON_FIELD_CONTROLS, new JSONArray(controlObjects)));

    return new JSONFormattedResponseControl(encodedValue,
         Collections.unmodifiableList(controlObjects));
  }



  /**
   * Creates a new {@code JSONFormattedResponseControl} with the provided set of
   * embedded JSON objects.
   *
   * @param  controlObjects  The collection of JSON objects that represent the
   *                         encoded controls to embed within this response
   *                         control.  This must not be {@code null} or empty.
   *                         Note that no attempt will be made to validate the
   *                         JSON objects as controls.
   *
   * @return  The {@code JSONFormattedResponseControl} that was created.
   */
  @NotNull()
  public static JSONFormattedResponseControl createWithControlObjects(
              @Nullable final JSONObject... controlObjects)
  {
    return createWithControlObjects(StaticUtils.toList(controlObjects));
  }



  /**
   * Creates a new {@code JSONFormattedResponseControl} with the provided set of
   * embedded JSON objects.
   *
   * @param  controlObjects  The collection of JSON objects that represent the
   *                         encoded controls to embed within this response
   *                         control.  This must not be {@code null} or empty.
   *                         Note that no attempt will be made to validate the
   *                         JSON objects as controls.
   *
   * @return  The {@code JSONFormattedResponseControl} that was created.
   */
  @NotNull()
  public static JSONFormattedResponseControl createWithControlObjects(
              @NotNull final Collection<JSONObject> controlObjects)
  {
    final List<JSONObject> controlObjectList = new ArrayList<>(controlObjects);
    final JSONObject encodedValue = new JSONObject(
         new JSONField(JSON_FIELD_CONTROLS, new JSONArray(controlObjectList)));

    return new JSONFormattedResponseControl(encodedValue,
         Collections.unmodifiableList(controlObjectList));
  }



  /**
   * Creates a new instance of this control that is decoded from the provided
   * generic control information.
   * generic control.  Note that if the provided control has a value, it will be
   * validated to ensure that it is a JSON object containing only a
   * {@code controls} field whose value is an array of JSON objects that appear
   * to be well-formed generic JSON controls, but it will not make any attempt
   * to validate in a control-specific manner.
   *
   * @param  oid         The OID for the control.  It must not be {@code null}.
   * @param  isCritical  Indicates whether the control is considered critical.
   * @param  value       The value for this control.  It should not be
   *                     {@code null} because this control requires a value, and
   *                     an exception will be thrown if the given value is
   *                     {@code null}.
   *
   * @throws LDAPException  If a problem is encountered while attempting to
   *                         decode the provided information as a JSON-formatted
   *                         response control.
   */
  public JSONFormattedResponseControl(@NotNull final String oid,
                                      final boolean isCritical,
                                      @Nullable final ASN1OctetString value)
         throws LDAPException
  {
    super(oid, isCritical, value);

    if (value == null)
    {
      throw new LDAPException(ResultCode.DECODING_ERROR,
           ERR_JSON_FORMATTED_RESPONSE_NO_VALUE.get());
    }

    try
    {
      encodedValue = new JSONObject(value.stringValue());
    }
    catch (final Exception e)
    {
      Debug.debugException(e);
      throw new LDAPException(ResultCode.DECODING_ERROR,
           ERR_JSON_FORMATTED_RESPONSE_VALUE_NOT_JSON.get(), e);
    }


    final List<String> unrecognizedFields =
           JSONControlDecodeHelper.getControlObjectUnexpectedFields(
                encodedValue, JSON_FIELD_CONTROLS);
    if (! unrecognizedFields.isEmpty())
    {
      throw new LDAPException(ResultCode.DECODING_ERROR,
           ERR_JSON_FORMATTED_RESPONSE_UNRECOGNIZED_FIELD.get(
                unrecognizedFields.get(0)));
    }


    final List<JSONValue> controlValues =
         encodedValue.getFieldAsArray(JSON_FIELD_CONTROLS);
    if (controlValues == null)
    {
      throw new LDAPException(ResultCode.DECODING_ERROR,
           ERR_JSON_FORMATTED_RESPONSE_VALUE_MISSING_CONTROLS.get(
                JSON_FIELD_CONTROLS));
    }

    if (controlValues.isEmpty())
    {
      throw new LDAPException(ResultCode.DECODING_ERROR,
           ERR_JSON_FORMATTED_RESPONSE_VALUE_EMPTY_CONTROLS.get(
                JSON_FIELD_CONTROLS));
    }

    final List<JSONObject> controlObjectsList =
         new ArrayList<>(controlValues.size());
    for (final JSONValue controlValue : controlValues)
    {
      if (controlValue instanceof JSONObject)
      {
        final JSONObject embeddedControlObject = (JSONObject) controlValue;

        try
        {
          new JSONControlDecodeHelper(embeddedControlObject, true, true, false);
          controlObjectsList.add(embeddedControlObject);
        }
        catch (final LDAPException e)
        {
          Debug.debugException(e);
          throw new LDAPException(ResultCode.DECODING_ERROR,
               ERR_JSON_FORMATTED_RESPONSE_VALUE_NOT_CONTROL.get(
                    JSON_FIELD_CONTROLS,
                    embeddedControlObject.toSingleLineString(), e.getMessage()),
               e);
        }
      }
      else
      {
        throw new LDAPException(ResultCode.DECODING_ERROR,
             ERR_JSON_FORMATTED_RESPONSE_VALUE_CONTROL_NOT_OBJECT.get(
                  JSON_FIELD_CONTROLS));
      }
    }


    controlObjects = Collections.unmodifiableList(controlObjectsList);
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  @NotNull()
  public JSONFormattedResponseControl decodeControl(
              @NotNull final String oid,
              final boolean isCritical,
              @Nullable final ASN1OctetString value)
          throws LDAPException
  {
    return new JSONFormattedResponseControl(oid, isCritical, value);
  }



  /**
   * Retrieves a list of the JSON objects that represent the embedded response
   * controls.  These JSON objects may not have been validated to ensure that
   * they represent valid controls.
   *
   * @return  A list of the JSON objects that represent the embedded response
   *          controls.
   */
  @NotNull()
  public List<JSONObject> getControlObjects()
  {
    return controlObjects;
  }



  /**
   * Attempts to retrieve a decoded representation of the embedded response
   * controls using the specified behavior.
   *
   * @param  behavior                The behavior to use when parsing JSON
   *                                 objects as controls.  It must not be
   *                                 {@code null}.
   * @param  nonFatalDecodeMessages  An optional list that may be updated with
   *                                 messages about any JSON objects that could
   *                                 not be parsed as valid controls, but that
   *                                 should not result in an exception as per
   *                                 the provided behavior.  This may be
   *                                 {@code null} if no such messages are
   *                                 desired.  If it is non-{@code null}, then
   *                                 the list must be updatable.
   *
   * @return  A decoded representation of the embedded response controls, or an
   *          empty list if none of the embedded JSON objects can be parsed as
   *          valid controls but that should not result in an exception as per
   *          the provided behavior.
   *
   * @throws  LDAPException  If any of the JSON objects cannot be parsed as a
   *                         valid control
   */
  @NotNull()
  public synchronized List<Control> decodeEmbeddedControls(
              @NotNull final JSONFormattedControlDecodeBehavior behavior,
              @Nullable final List<String> nonFatalDecodeMessages)
         throws LDAPException
  {
    // Iterate through the controls and try to decode them.
    boolean errorEncountered = false;
    final List<Control> controlList = new ArrayList<>(controlObjects.size());
    final List<String> fatalMessages = new ArrayList<>(controlObjects.size());
    for (final JSONObject controlObject : controlObjects)
    {
      // First, try to decode the JSON object as a generic control without any
      // specific decoding based on its OID.
      final JSONControlDecodeHelper jsonControl;
      try
      {
        jsonControl = new JSONControlDecodeHelper(controlObject,
             behavior.strict(), true, false);
      }
      catch (final LDAPException e)
      {
        Debug.debugException(e);
        errorEncountered = true;

        if (behavior.throwOnUnparsableObject())
        {
          fatalMessages.add(e.getMessage());
        }
        else if (nonFatalDecodeMessages != null)
        {
          nonFatalDecodeMessages.add(e.getMessage());
        }

        continue;
      }


      // If the control is itself an embedded JSON-formatted response control,
      // see how we should handle it.
      if (jsonControl.getOID().equals(JSON_FORMATTED_RESPONSE_OID))
      {
        if (! behavior.allowEmbeddedJSONFormattedControl())
        {
          final String message =
               ERR_JSON_FORMATTED_RESPONSE_DISALLOWED_EMBEDDED_CONTROL.get();
          if (jsonControl.getCriticality())
          {
            fatalMessages.add(message);
          }
          else if (nonFatalDecodeMessages != null)
          {
            nonFatalDecodeMessages.add(message);
          }

          continue;
        }
      }


      // Try to actually decode the JSON object as a control, potentially using
      // control-specific logic based on its OID.
      try
      {
        controlList.add(Control.decodeJSONControl(controlObject,
             behavior.strict(), false));
      }
      catch (final LDAPException e)
      {
        Debug.debugException(e);
        errorEncountered = true;

        if (jsonControl.getCriticality())
        {
          if (behavior.throwOnInvalidCriticalControl())
          {
            fatalMessages.add(e.getMessage());
          }
          else if (nonFatalDecodeMessages != null)
          {
            nonFatalDecodeMessages.add(e.getMessage());
          }
        }
        else
        {
          if (behavior.throwOnInvalidNonCriticalControl())
          {
            fatalMessages.add(e.getMessage());
          }
          else if (nonFatalDecodeMessages != null)
          {
            nonFatalDecodeMessages.add(e.getMessage());
          }
        }
      }
    }


    //  If there are any fatal messages, then we'll throw an exception with
    //  them.
    if (! fatalMessages.isEmpty())
    {
      throw new LDAPException(ResultCode.DECODING_ERROR,
           StaticUtils.concatenateStrings(fatalMessages));
    }


    return Collections.unmodifiableList(controlList);
  }



  /**
   * Extracts a JSON-formatted control from the provided LDAP result.
   *
   * @param  result  The LDAP result from which to retrieve the JSON-formatted
   *                 response control.
   *
   * @return  The JSON-formatted response control contained in the provided
   *          LDAP result, or {@code null} if the result did not contain a
   *          JSON-formatted response control.
   *
   * @throws  LDAPException  If a problem is encountered while attempting to
   *                         decode the JSON-formatted response control
   *                         contained in the provided LDAP result.
   */
  @Nullable()
  public static JSONFormattedResponseControl get(
              @NotNull final LDAPResult result)
         throws LDAPException
  {
    return get(result.getResponseControl(JSON_FORMATTED_RESPONSE_OID));
  }



  /**
   * Extracts a JSON-formatted control from the provided search result entry.
   *
   * @param  entry  The search result entry from which to retrieve the
   *                JSON-formatted response control.
   *
   * @return  The JSON-formatted response control contained in the provided
   *          search result entry, or {@code null} if the entry did not contain
   *          a JSON-formatted response control.
   *
   * @throws  LDAPException  If a problem is encountered while attempting to
   *                         decode the JSON-formatted response control
   *                         contained in the provided search result entry.
   */
  @Nullable()
  public static JSONFormattedResponseControl get(
              @NotNull final SearchResultEntry entry)
         throws LDAPException
  {
    return get(entry.getControl(JSON_FORMATTED_RESPONSE_OID));
  }



  /**
   * Extracts a JSON-formatted control from the provided search result
   * reference.
   *
   * @param  reference  The search result reference from which to retrieve the
   *                    JSON-formatted response control.
   *
   * @return  The JSON-formatted response control contained in the provided
   *          search result reference, or {@code null} if the reference did not
   *          contain a JSON-formatted response control.
   *
   * @throws  LDAPException  If a problem is encountered while attempting to
   *                         decode the JSON-formatted response control
   *                         contained in the provided search result reference.
   */
  @Nullable()
  public static JSONFormattedResponseControl get(
              @NotNull final SearchResultReference reference)
         throws LDAPException
  {
    return get(reference.getControl(JSON_FORMATTED_RESPONSE_OID));
  }



  /**
   * Extracts a JSON-formatted control from the provided intermediate response.
   *
   * @param  response  The intermediate response from which to retrieve the
   *                   JSON-formatted response control.
   *
   * @return  The JSON-formatted response control contained in the provided
   *          intermediate response, or {@code null} if the response did not
   *          contain a JSON-formatted response control.
   *
   * @throws  LDAPException  If a problem is encountered while attempting to
   *                         decode the JSON-formatted response control
   *                         contained in the provided intermediate response.
   */
  @Nullable()
  public static JSONFormattedResponseControl get(
              @NotNull final IntermediateResponse response)
         throws LDAPException
  {
    return get(response.getControl(JSON_FORMATTED_RESPONSE_OID));
  }



  /**
   * Retrieves the provided control as a JSON-formatted response control.
   *
   * @param  c  The control to retrieve as a JSON-formatted response control.
   *            It may optionally be {@code null}.
   *
   * @return  A JSON-formatted response control that is a representation of the
   *          provided control, or {@code null} if the provided control is
   *          {@code null}.
   *
   * @throws  LDAPException  If the provided control cannot be parsed as a valid
   *                         JSON-formatted response control.
   */
  @Nullable()
  private static JSONFormattedResponseControl get(@Nullable final Control c)
          throws LDAPException
  {
    if (c == null)
    {
      return null;
    }

    if (c instanceof JSONFormattedResponseControl)
    {
      return (JSONFormattedResponseControl) c;
    }
    else
    {
      return new JSONFormattedResponseControl(c.getOID(), c.isCritical(),
           c.getValue());
    }
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  @NotNull()
  public String getControlName()
  {
    return INFO_CONTROL_NAME_JSON_FORMATTED_RESPONSE.get();
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  @NotNull()
  public JSONObject toJSONControl()
  {
    return new JSONObject(
         new JSONField(JSONControlDecodeHelper.JSON_FIELD_OID,
              JSON_FORMATTED_RESPONSE_OID),
         new JSONField(JSONControlDecodeHelper.JSON_FIELD_CONTROL_NAME,
              INFO_CONTROL_NAME_JSON_FORMATTED_RESPONSE.get()),
         new JSONField(JSONControlDecodeHelper.JSON_FIELD_CRITICALITY,
              isCritical()),
         new JSONField(JSONControlDecodeHelper.JSON_FIELD_VALUE_JSON,
              encodedValue));
  }



  /**
   * Attempts to decode the provided object as a JSON representation of a
   * JSON-formatted response control.
   *
   * @param  controlObject  The JSON object to be decoded.  It must not be
   *                        {@code null}.
   * @param  strict         Indicates whether to use strict mode when decoding
   *                        the provided JSON object.  If this is {@code true},
   *                        then this method will throw an exception if the
   *                        provided JSON object contains any unrecognized
   *                        fields.  If this is {@code false}, then unrecognized
   *                        fields will be ignored.
   *
   * @return  The JSON-formatted response control that was decoded from the
   *          provided JSON object.
   *
   * @throws  LDAPException  If the provided JSON object cannot be parsed as a
   *                         valid JSON-formatted response control.
   */
  @NotNull()
  public static JSONFormattedResponseControl decodeJSONControl(
              @NotNull final JSONObject controlObject,
              final boolean strict)
         throws LDAPException
  {
    final JSONControlDecodeHelper jsonControl = new JSONControlDecodeHelper(
         controlObject, strict, true, true);

    final ASN1OctetString rawValue = jsonControl.getRawValue();
    if (rawValue != null)
    {
      return new JSONFormattedResponseControl(jsonControl.getOID(),
           jsonControl.getCriticality(), rawValue);
    }


    final JSONObject valueObject = jsonControl.getValueObject();

    final List<JSONValue> controlValues =
         valueObject.getFieldAsArray(JSON_FIELD_CONTROLS);
    if (controlValues == null)
    {
      throw new LDAPException(ResultCode.DECODING_ERROR,
           ERR_JSON_FORMATTED_RESPONSE_DECODE_VALUE_MISSING_CONTROLS.get(
                controlObject.toSingleLineString(), JSON_FIELD_CONTROLS));
    }

    if (controlValues.isEmpty())
    {
      throw new LDAPException(ResultCode.DECODING_ERROR,
           ERR_JSON_FORMATTED_RESPONSE_DECODE_VALUE_EMPTY_CONTROLS.get(
                controlObject.toSingleLineString(), JSON_FIELD_CONTROLS));
    }

    final List<JSONObject> controlObjectsList =
         new ArrayList<>(controlValues.size());
    for (final JSONValue controlValue : controlValues)
    {
      if (controlValue instanceof JSONObject)
      {
        final JSONObject embeddedControlObject = (JSONObject) controlValue;

        try
        {
          new JSONControlDecodeHelper(embeddedControlObject, strict, true,
               false);
          controlObjectsList.add(embeddedControlObject);
        }
        catch (final LDAPException e)
        {
          Debug.debugException(e);
          throw new LDAPException(ResultCode.DECODING_ERROR,
               ERR_JSON_FORMATTED_RESPONSE_DECODE_VALUE_NOT_CONTROL.get(
                    controlObject.toSingleLineString(), JSON_FIELD_CONTROLS,
                    embeddedControlObject.toSingleLineString(), e.getMessage()),
               e);
        }
      }
      else
      {
        throw new LDAPException(ResultCode.DECODING_ERROR,
             ERR_JSON_FORMATTED_RESPONSE_DECODE_VALUE_CONTROL_NOT_OBJECT.get(
                  controlObject.toSingleLineString(),
                  JSON_FIELD_CONTROLS));
      }
    }


    if (strict)
    {
      final List<String> unrecognizedFields =
           JSONControlDecodeHelper.getControlObjectUnexpectedFields(
                valueObject, JSON_FIELD_CONTROLS);
      if (! unrecognizedFields.isEmpty())
      {
        throw new LDAPException(ResultCode.DECODING_ERROR,
             ERR_JSON_FORMATTED_RESPONSE_DECODE_UNRECOGNIZED_FIELD.get(
                  controlObject.toSingleLineString(),
                  unrecognizedFields.get(0)));
      }
    }


    return new JSONFormattedResponseControl(valueObject,
         Collections.unmodifiableList(controlObjectsList));
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public void toString(@NotNull final StringBuilder buffer)
  {
    buffer.append("JSONFormattedResponseControl(isCritical=");
    buffer.append(isCritical());
    buffer.append(", valueObject=");
    encodedValue.toSingleLineString(buffer);
    buffer.append(')');
  }
}
