/*
 * Copyright 2009-2010 UnboundID Corp.
 * All Rights Reserved.
 */
/*
 * Copyright (C) 2009-2010 UnboundID Corp.
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
package com.unboundid.ldap.sdk.persist;



import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.Entry;
import com.unboundid.ldap.sdk.schema.AttributeTypeDefinition;

import static com.unboundid.ldap.sdk.persist.PersistMessages.*;
import static com.unboundid.util.Debug.*;
import static com.unboundid.util.StaticUtils.*;
import static com.unboundid.util.Validator.*;



/**
 * This class provides a data structure that holds information about an
 * annotated field.
 */
final class FieldInfo
      implements Serializable
{
  /**
   * The serial version UID for this serializable class.
   */
  private static final long serialVersionUID = -5715642176677596417L;



  // Indicates whether attempts to populate the associated field should fail if
  // the LDAP attribute has a value that is not valid for the data type of the
  // field.
  private final boolean failOnInvalidValue;

  // Indicates whether attempts to populate the associated field should fail if
  // the LDAP attribute has multiple values but the field can only hold a single
  // value.
  private final boolean failOnTooManyValues;

  // Indicates whether the associated field should be included in the entry
  // created for an add operation.
  private final boolean includeInAdd;

  // Indicates whether the associated field should be considered for inclusion
  // in the set of modifications used for modify operations.
  private final boolean includeInModify;

  // Indicates whether the associated field is part of the RDN.
  private final boolean includeInRDN;

  // Indicates whether the associated field is required when decoding.
  private final boolean isRequiredForDecode;

  // Indicates whether the associated field is required when encoding.
  private final boolean isRequiredForEncode;

  // Indicates whether the associated field supports multiple values.
  private final boolean supportsMultipleValues;

  // The class that contains the associated field.
  private final Class<?> containingClass;

  // The field with which this object is associated.
  private final Field field;

  // The filter usage for the associated field.
  private final FilterUsage filterUsage;

  // The encoder used for this field.
  private final LDAPFieldEncoder encoder;

  // The name of the associated attribute type.
  private final String attributeName;

  // The default values for the field to use for object instantiation.
  private final String[] defaultDecodeValues;

  // The default values for the field to use for add operations.
  private final String[] defaultEncodeValues;

  // The names of the object classes for the associated attribute.
  private final String[] objectClasses;



  /**
   * Creates a new field info object from the provided field.
   *
   * @param  f  The field to use to create this object.  It must not be
   *            {@code null} and it must be marked with the {@code LDAPField}
   *            annotation.
   * @param  c  The class which holds the field.  It must not be {@code null}
   *            and it must be marked with the {@code LDAPObject} annotation.
   *
   * @throws  LDAPPersistException  If a problem occurs while processing the
   *                                given field.
   */
  FieldInfo(final Field f, final Class<?> c)
       throws LDAPPersistException
  {
    ensureNotNull(f, c);

    field = f;
    f.setAccessible(true);

    final LDAPField  a = f.getAnnotation(LDAPField.class);
    if (a == null)
    {
      throw new LDAPPersistException(ERR_FIELD_INFO_FIELD_NOT_ANNOTATED.get(
           f.getName(), c.getName()));
    }

    final LDAPObject o = c.getAnnotation(LDAPObject.class);
    if (o == null)
    {
      throw new LDAPPersistException(ERR_FIELD_INFO_CLASS_NOT_ANNOTATED.get(
           c.getName()));
    }

    containingClass     = c;
    failOnInvalidValue  = a.failOnInvalidValue();
    includeInRDN        = a.inRDN();
    includeInAdd        = (includeInRDN || a.inAdd());
    includeInModify     = ((! includeInRDN) && a.inModify());
    filterUsage         = a.filterUsage();
    isRequiredForDecode = a.requiredForDecode();
    isRequiredForEncode = (includeInRDN || a.requiredForEncode());
    defaultDecodeValues = a.defaultDecodeValue();
    defaultEncodeValues = a.defaultEncodeValue();

    final int modifiers = f.getModifiers();
    if (Modifier.isFinal(modifiers))
    {
      throw new LDAPPersistException(ERR_FIELD_INFO_FIELD_FINAL.get(
           f.getName(), c.getName()));
    }

    if (Modifier.isStatic(modifiers))
    {
      throw new LDAPPersistException(ERR_FIELD_INFO_FIELD_STATIC.get(
           f.getName(), c.getName()));
    }

    try
    {
      encoder = a.encoderClass().newInstance();
    }
    catch (Exception e)
    {
      debugException(e);
      throw new LDAPPersistException(ERR_FIELD_INFO_CANNOT_GET_ENCODER.get(
           a.encoderClass().getName(), f.getName(), c.getName(),
           getExceptionMessage(e)), e);
    }

    if (! encoder.supportsType(f.getGenericType()))
    {
      throw new LDAPPersistException(
           ERR_FIELD_INFO_ENCODER_UNSUPPORTED_TYPE.get(
                encoder.getClass().getName(), f.getName(), c.getName(),
                f.getGenericType()));
    }

    supportsMultipleValues = encoder.supportsMultipleValues(f);
    if (supportsMultipleValues)
    {
      failOnTooManyValues = false;
    }
    else
    {
      failOnTooManyValues = a.failOnTooManyValues();
      if (defaultDecodeValues.length > 1)
      {
        throw new LDAPPersistException(
             ERR_FIELD_INFO_UNSUPPORTED_MULTIPLE_DEFAULT_DECODE_VALUES.get(
                  f.getName(), c.getName()));
      }

      if (defaultEncodeValues.length > 1)
      {
        throw new LDAPPersistException(
             ERR_FIELD_INFO_UNSUPPORTED_MULTIPLE_DEFAULT_ENCODE_VALUES.get(
                  f.getName(), c.getName()));
      }
    }

    final String attrName = a.attribute();
    if ((attrName == null) || (attrName.length() == 0))
    {
      attributeName = f.getName();
    }
    else
    {
      attributeName = attrName;
    }

    final StringBuilder invalidReason = new StringBuilder();
    if (! PersistUtils.isValidLDAPName(attributeName, invalidReason))
    {
      throw new LDAPPersistException(ERR_FIELD_INFO_INVALID_ATTR_NAME.get(
           f.getName(), c.getName(), invalidReason.toString()));
    }

    final String structuralClass;
    if (o.structuralClass().length() == 0)
    {
      structuralClass = getUnqualifiedClassName(c);
    }
    else
    {
      structuralClass = o.structuralClass();
    }

    final String[] ocs = a.objectClass();
    if ((ocs == null) || (ocs.length == 0))
    {
      objectClasses = new String[] { structuralClass };
    }
    else
    {
      objectClasses = ocs;
    }

    for (final String s : objectClasses)
    {
      if (! s.equalsIgnoreCase(structuralClass))
      {
        boolean found = false;
        for (final String oc : o.auxiliaryClass())
        {
          if (s.equalsIgnoreCase(oc))
          {
            found = true;
            break;
          }
        }

        if (! found)
        {
          throw new LDAPPersistException(ERR_FIELD_INFO_INVALID_OC.get(
               f.getName(), c.getName(), s));
        }
      }
    }
  }



  /**
   * Retrieves the field with which this object is associated.
   *
   * @return  The field with which this object is associated.
   */
  Field getField()
  {
    return field;
  }



  /**
   * Retrieves the class that is marked with the {@link LDAPObject} annotation
   * and contains the associated field.
   *
   * @return  The class that contains the associated field.
   */
  Class<?> getContainingClass()
  {
    return containingClass;
  }



  /**
   * Indicates whether attempts to initialize an object should fail if the LDAP
   * attribute has a value that cannot be stored in the associated field.
   *
   * @return  {@code true} if an exception should be thrown if an LDAP attribute
   *          has a value that cannot be assigned to the associated field, or
   *          {@code false} if the field should remain uninitialized.
   */
  boolean failOnInvalidValue()
  {
    return failOnInvalidValue;
  }



  /**
   * Indicates whether attempts to initialize an object should fail if the
   * LDAP attribute has multiple values but the associated field can only hold a
   * single value.  Note that the value returned from this method may be
   * {@code false} even when the annotation has a value of {@code true} if the
   * associated field supports multiple values.
   *
   * @return  {@code true} if an exception should be thrown if an attribute has
   *          too many values to hold in the associated field, or {@code false}
   *          if the first value returned should be assigned to the field.
   */
  boolean failOnTooManyValues()
  {
    return failOnTooManyValues;
  }



  /**
   * Indicates whether the associated field should be included in entries
   * generated for add operations.  Note that the value returned from this
   * method may be {@code true} even when the annotation has a value of
   * {@code false} if the associated field is to be included in entry RDNs.
   *
   * @return  {@code true} if the associated field should be included in entries
   *         generated for add operations, or {@code false} if not.
   */
  boolean includeInAdd()
  {
    return includeInAdd;
  }



  /**
   * Indicates whether the associated field should be considered for inclusion
   * in the set of modifications generated for modify operations.  Note that the
   * value returned from this method may be {@code false} even when the
   * annotation has a value of {@code true} for the {@code inModify} element if
   * the associated field is to be included in entry RDNs.
   *
   * @return  {@code true} if the associated field should be considered for
   *          inclusion in the set of modifications generated for modify
   *          operations, or {@code false} if not.
   */
  boolean includeInModify()
  {
    return includeInModify;
  }



  /**
   * Indicates whether the associated field should be used to generate entry
   * RDNs.
   *
   * @return  {@code true} if the associated field should be used to generate
   *          entry RDNs, or {@code false} if not.
   */
  boolean includeInRDN()
  {
    return includeInRDN;
  }



  /**
   * Retrieves the filter usage for the associated field.
   *
   * @return  The filter usage for the associated field.
   */
  FilterUsage getFilterUsage()
  {
    return filterUsage;
  }



  /**
   * Indicates whether the associated field should be considered required for
   * decode operations.
   *
   * @return  {@code true} if the associated field should be considered required
   *          for decode operations, or {@code false} if not.
   */
  boolean isRequiredForDecode()
  {
    return isRequiredForDecode;
  }



  /**
   * Indicates whether the associated field should be considered required for
   * encode operations.    Note that the value returned from this method may be
   * {@code true} even when the annotation has a value of {@code true} for the
   * {@code requiredForEncode} element if the associated field is to be included
   * in entry RDNs.
   *
   * @return  {@code true} if the associated field should be considered required
   *          for encode operations, or {@code false} if not.
   */
  boolean isRequiredForEncode()
  {
    return isRequiredForEncode;
  }



  /**
   * Retrieves the encoder that should be used for the associated field.
   *
   * @return  The encoder that should be used for the associated field.
   */
  LDAPFieldEncoder getEncoder()
  {
    return encoder;
  }



  /**
   * Retrieves the name of the LDAP attribute used to hold values for the
   * associated field.
   *
   * @return  The name of the LDAP attribute used to hold values for the
   *          associated field.
   */
  String getAttributeName()
  {
    return attributeName;
  }



  /**
   * Retrieves the set of default values that should be assigned to the
   * associated field if there are no values for the corresponding attribute in
   * the LDAP entry.
   *
   * @return  The set of default values for use when instantiating the object,
   *          or an empty array if no default values are defined.
   */
  String[] getDefaultDecodeValues()
  {
    return defaultDecodeValues;
  }



  /**
   * Retrieves the set of default values that should be used when creating an
   * entry for an add operation if the associated field does not itself have any
   * values.
   *
   * @return  The set of default values for use in add operations, or an empty
   *          array if no default values are defined.
   */
  String[] getDefaultEncodeValues()
  {
    return defaultEncodeValues;
  }



  /**
   * Retrieves the names of the object classes containing the associated
   * attribute.
   *
   * @return  The names of the object classes containing the associated
   *          attribute.
   */
  String[] getObjectClasses()
  {
    return objectClasses;
  }



  /**
   * Indicates whether the associated field can hold multiple values.
   *
   * @return  {@code true} if the associated field can hold multiple values, or
   *          {@code false} if not.
   */
  boolean supportsMultipleValues()
  {
    return supportsMultipleValues;
  }



  /**
   * Constructs a definition for an LDAP attribute type which may be added to
   * the directory server schema to allow it to hold the value of the associated
   * field.  Note that the object identifier used for the constructed attribute
   * type definition is not required to be valid or unique.
   *
   * @return  The constructed attribute type definition.
   *
   * @throws  LDAPPersistException  If the LDAP field encoder does not support
   *                                encoding values for the associated field
   *                                type.
   */
  public AttributeTypeDefinition constructAttributeType()
         throws LDAPPersistException
  {
    return constructAttributeType(DefaultOIDAllocator.getInstance());
  }



  /**
   * Constructs a definition for an LDAP attribute type which may be added to
   * the directory server schema to allow it to hold the value of the associated
   * field.  Note that the object identifier used for the constructed attribute
   * type definition is not required to be valid or unique.
   *
   * @param  a  The OID allocator to use to generate the object identifier.  It
   *            must not be {@code null}.
   *
   * @return  The constructed attribute type definition.
   *
   * @throws  LDAPPersistException  If the LDAP field encoder does not support
   *                                encoding values for the associated field
   *                                type.
   */
  public AttributeTypeDefinition constructAttributeType(final OIDAllocator a)
         throws LDAPPersistException
  {
    return encoder.constructAttributeType(field, a);
  }



  /**
   * Encodes the value for the associated field from the provided object to an
   * attribute.
   *
   * @param  o                   The object containing the field to be encoded.
   * @param  ignoreRequiredFlag  Indicates whether to ignore the value of the
   *                             {@code requiredForEncode} setting.  If this is
   *                             {@code true}, then this method will always
   *                             return {@code null} if the field does not have
   *                             a value even if this field is marked as
   *                             required for encode processing.
   *
   * @return  The attribute containing the encoded representation of the field
   *          value if it is non-{@code null}, an encoded representation of the
   *          default add values if the associated field is {@code null} but
   *          default values are defined, or {@code null} if the associated
   *          field is {@code null} and there are no default values.
   *
   * @throws  LDAPPersistException  If a problem occurs while encoding the
   *                                value of the associated field for the
   *                                provided object, or if the field is marked
   *                                as required but is {@code null} and does not
   *                                have any default add values.
   */
  Attribute encode(final Object o, final boolean ignoreRequiredFlag)
            throws LDAPPersistException
  {
    try
    {
      final Object fieldValue = field.get(o);
      if (fieldValue == null)
      {
        if (defaultEncodeValues.length > 0)
        {
          return new Attribute(attributeName, defaultEncodeValues);
        }

        if (isRequiredForEncode && (! ignoreRequiredFlag))
        {
          throw new LDAPPersistException(
               ERR_FIELD_INFO_MISSING_REQUIRED_VALUE.get(field.getName(),
                    containingClass.getName()));
        }

        return null;
      }

      return encoder.encodeFieldValue(field, fieldValue, attributeName);
    }
    catch (LDAPPersistException lpe)
    {
      debugException(lpe);
      throw lpe;
    }
    catch (Exception e)
    {
      debugException(e);
      throw new LDAPPersistException(ERR_FIELD_INFO_CANNOT_ENCODE.get(
           field.getName(), containingClass.getName(), getExceptionMessage(e)),
           e);
    }
  }



  /**
   * Sets the value of the associated field in the given object from the
   * information contained in the provided attribute.
   *
   * @param  o  The object for which to update the associated field.
   * @param  e  The entry being decoded.
   *
   * @throws  LDAPPersistException  If a problem occurs while updating the
   *                                provided object.
   */
  void decode(final Object o, final Entry e)
       throws LDAPPersistException
  {
    Attribute a = e.getAttribute(attributeName);
    if (a == null)
    {
      if (defaultDecodeValues.length > 0)
      {
        a = new Attribute(attributeName, defaultDecodeValues);
      }
      else
      {
        if (isRequiredForDecode)
        {
          throw new LDAPPersistException(
               ERR_FIELD_INFO_MISSING_REQUIRED_ATTRIBUTE.get(
                    containingClass.getName(), e.getDN(), attributeName,
                    field.getName()));
        }

        encoder.setNull(field, o);
        return;
      }
    }

    if (failOnTooManyValues && (a.size() > 1))
    {
      throw new LDAPPersistException(ERR_FIELD_INFO_FIELD_NOT_MULTIVALUED.get(
           a.getName(), field.getName(), containingClass.getName()));
    }

    try
    {
      encoder.decodeField(field, o, a);
    }
    catch (LDAPPersistException lpe)
    {
      debugException(lpe);
      if (failOnInvalidValue)
      {
        throw lpe;
      }
    }
  }
}
