/*
 * Copyright 2009-2012 UnboundID Corp.
 * All Rights Reserved.
 */
/*
 * Copyright (C) 2009-2012 UnboundID Corp.
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
package com.unboundid.ldap.sdk.migrate.ldapjdk;



import java.io.Serializable;

import com.unboundid.asn1.ASN1OctetString;
import com.unboundid.ldap.sdk.Control;
import com.unboundid.ldap.sdk.controls.ManageDsaITRequestControl;
import com.unboundid.ldap.sdk.controls.PasswordExpiredControl;
import com.unboundid.ldap.sdk.controls.PasswordExpiringControl;
import com.unboundid.util.Extensible;
import com.unboundid.util.NotMutable;
import com.unboundid.util.ThreadSafety;
import com.unboundid.util.ThreadSafetyLevel;



/**
 * This class provides a data structure that holds information about an LDAP
 * control.
 * <BR><BR>
 * This class is primarily intended to be used in the process of updating
 * applications which use the Netscape Directory SDK for Java to switch to or
 * coexist with the UnboundID LDAP SDK for Java.  For applications not written
 * using the Netscape Directory SDK for Java, the {@link Control} class should
 * be used instead.
 */
@Extensible()
@NotMutable()
@ThreadSafety(level=ThreadSafetyLevel.COMPLETELY_THREADSAFE)
public class LDAPControl
       implements Serializable
{
  /**
   * The OID for the ManageDsaIT request control.
   */
  public static final String MANAGEDSAIT =
       ManageDsaITRequestControl.MANAGE_DSA_IT_REQUEST_OID;



  /**
   * The OID for the password expired control.
   */
  public static final String PWEXPIRED =
       PasswordExpiredControl.PASSWORD_EXPIRED_OID;



  /**
   * The OID for the password expiring control.
   */
  public static final String PWEXPIRING =
       PasswordExpiringControl.PASSWORD_EXPIRING_OID;



  /**
   * The serial version UID for this serializable class.
   */
  private static final long serialVersionUID = 7828506470553016637L;



  // Indicates whether this control is critical.
  private final boolean isCritical;

  // The value for this control.
  private final byte[] value;

  // The OID for this control.
  private final String oid;



  /**
   * Creates a new LDAP control from the provided control.
   *
   * @param  control  The control to use to create this control.
   */
  public LDAPControl(final Control control)
  {
    oid        = control.getOID();
    isCritical = control.isCritical();

    if (control.hasValue())
    {
      value = control.getValue().getValue();
    }
    else
    {
      value = null;
    }
  }



  /**
   * Creates a new LDAP control with the specified information.
   *
   * @param  id        The OID for the control.
   * @param  critical  Indicates whether the control should be marked critical.
   * @param  vals      The encoded value for the control.
   */
  public LDAPControl(final String id, final boolean critical, final byte[] vals)
  {
    oid        = id;
    isCritical = critical;
    value      = vals;
  }



  /**
   * Retrieves the OID for this control.
   *
   * @return  The OID for this control.
   */
  public String getID()
  {
    return oid;
  }



  /**
   * Indicates whether this control is marked critical.
   *
   * @return  {@code true} if this control is marked critical, or {@code false}
   *          if not.
   */
  public boolean isCritical()
  {
    return isCritical;
  }



  /**
   * Retrieves the value for this control, if available.
   *
   * @return  The value for this control, or {@code null} if there is none.
   */
  public byte[] getValue()
  {
    return value;
  }



  /**
   * Converts this LDAP control to a {@link Control} object.
   *
   * @return  The {@code Control} object for this LDAP control.
   */
  public final Control toControl()
  {
    if (value == null)
    {
      return new Control(oid, isCritical, null);
    }
    else
    {
      return new Control(oid, isCritical, new ASN1OctetString(value));
    }
  }



  /**
   * Converts the provided array of controls to an array of LDAP controls.
   *
   * @param  ldapControls  The LDAP controls to be converted.
   *
   * @return  The corresponding array of controls.
   */
  public static Control[] toControls(final LDAPControl[] ldapControls)
  {
    if (ldapControls == null)
    {
      return null;
    }

    final Control[] controls = new Control[ldapControls.length];
    for (int i=0; i < ldapControls.length; i++)
    {
      controls[i] = ldapControls[i].toControl();
    }

    return controls;
  }



  /**
   * Converts the provided array of LDAP controls to an array of controls.
   *
   * @param  controls  The controls to be converted.
   *
   * @return  The corresponding array of LDAP controls.
   */
  public static LDAPControl[] toLDAPControls(final Control[] controls)
  {
    if (controls == null)
    {
      return null;
    }

    final LDAPControl[] ldapControls = new LDAPControl[controls.length];
    for (int i=0; i < controls.length; i++)
    {
      ldapControls[i] = new LDAPControl(controls[i]);
    }

    return ldapControls;
  }



  /**
   * Creates a duplicate of this control.
   *
   * @return  A duplicate of this control.
   */
  public LDAPControl duplicate()
  {
    return new LDAPControl(oid, isCritical, value);
  }



  /**
   * Retrieves a string representation of this control.
   *
   * @return  A string representation of this control.
   */
  @Override()
  public String toString()
  {
    return toControl().toString();
  }
}
