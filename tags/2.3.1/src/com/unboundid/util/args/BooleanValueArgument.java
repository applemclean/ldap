/*
 * Copyright 2010-2012 UnboundID Corp.
 * All Rights Reserved.
 */
/*
 * Copyright (C) 2010-2012 UnboundID Corp.
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
package com.unboundid.util.args;



import com.unboundid.util.Mutable;
import com.unboundid.util.StaticUtils;
import com.unboundid.util.ThreadSafety;
import com.unboundid.util.ThreadSafetyLevel;

import static com.unboundid.util.args.ArgsMessages.*;



/**
 * Creates a new argument that is intended to represent Boolean states based on
 * the value provided for this argument.  This is similar to the
 * {@link BooleanArgument} argument type, except that the Boolean value for this
 * argument must be explicitly specified, whereas the Boolean value for the
 * {@code BooleanArgument} class is inferred based on whether the argument
 * was present.
 * <BR><BR>
 * Arguments of this type must always have exactly one value.  Values of "true",
 * "t", "yes", "y", "on", and "1" will be interpreted as representing a Boolean
 * value of {@code true}, and values of "false", "f", "no", "n", "off", and "0"
 * will be interpreted as representing a Boolean value of {@code false}.  No
 * other values will be allowed.
 */
@Mutable()
@ThreadSafety(level=ThreadSafetyLevel.NOT_THREADSAFE)
public final class BooleanValueArgument
       extends Argument
{
  /**
   * The serial version UID for this serializable class.
   */
  private static final long serialVersionUID = -3903872574065550222L;



  // The default value for this argument.
  private final Boolean defaultValue;

  // The provided value for this argument.
  private Boolean value;



  /**
   * Creates a new Boolean value argument with no default value.
   *
   * @param  shortIdentifier   The short identifier for this argument.  It may
   *                           not be {@code null} if the long identifier is
   *                           {@code null}.
   * @param  longIdentifier    The long identifier for this argument.  It may
   *                           not be {@code null} if the short identifier is
   *                           {@code null}.
   * @param  isRequired        Indicates whether this argument is required to
   *                           be provided.
   * @param  valuePlaceholder  A placeholder to display in usage information to
   *                           indicate that a value must be provided.  It must
   *                           not be {@code null}.
   * @param  description       A human-readable description for this argument.
   *                           It must not be {@code null}.
   *
   * @throws  ArgumentException  If there is a problem with the definition of
   *                             this argument.
   */
  public BooleanValueArgument(final Character shortIdentifier,
                              final String longIdentifier,
                              final boolean isRequired,
                              final String valuePlaceholder,
                              final String description)
         throws ArgumentException
  {
    this(shortIdentifier, longIdentifier, isRequired, valuePlaceholder,
         description, null);
  }



  /**
   * Creates a new Boolean value argument with the specified default value.
   *
   * @param  shortIdentifier   The short identifier for this argument.  It may
   *                           not be {@code null} if the long identifier is
   *                           {@code null}.
   * @param  longIdentifier    The long identifier for this argument.  It may
   *                           not be {@code null} if the short identifier is
   *                           {@code null}.
   * @param  isRequired        Indicates whether this argument is required to
   *                           be provided.
   * @param  valuePlaceholder  A placeholder to display in usage information to
   *                           indicate that a value must be provided.  It must
   *                           not be {@code null}.
   * @param  description       A human-readable description for this argument.
   *                           It must not be {@code null}.
   * @param  defaultValue      The default value that will be used for this
   *                           argument if no values are provided.  It may be
   *                           {@code null} if there should not be a default
   *                           value.
   *
   * @throws  ArgumentException  If there is a problem with the definition of
   *                             this argument.
   */
  public BooleanValueArgument(final Character shortIdentifier,
                              final String longIdentifier,
                              final boolean isRequired,
                              final String valuePlaceholder,
                              final String description,
                              final Boolean defaultValue)
         throws ArgumentException
  {
    super(shortIdentifier, longIdentifier, isRequired, 1, valuePlaceholder,
         description);

    if (valuePlaceholder == null)
    {
      throw new ArgumentException(
           ERR_ARG_MUST_TAKE_VALUE.get(getIdentifierString()));
    }

    this.defaultValue = defaultValue;

    value = null;
  }



  /**
   * Creates a new Boolean value argument that is a "clean" copy of the provided
   * source argument.
   *
   * @param  source  The source argument to use for this argument.
   */
  private BooleanValueArgument(final BooleanValueArgument source)
  {
    super(source);

    defaultValue = source.defaultValue;
    value        = null;
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  protected boolean hasDefaultValue()
  {
    return (defaultValue != null);
  }



  /**
   * Retrieves the default value for this argument, if defined.
   *
   * @return  The default value for this argument, or {@code null} if none is
   *          defined.
   */
  public Boolean getDefaultValue()
  {
    return defaultValue;
  }



  /**
   * Retrieves the value for this argument, if one was provided.
   *
   * @return  The value for this argument.  If no value was provided but a
   *          default value was defined, then the default value will be
   *          returned.  If no value was provided and no default value was
   *          defined, then {@code null} will be returned.
   */
  public Boolean getValue()
  {
    if (value == null)
    {
      return defaultValue;
    }
    else
    {
      return value;
    }
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  protected void addValue(final String valueString)
            throws ArgumentException
  {
    if (value != null)
    {
      throw new ArgumentException(
           ERR_ARG_MAX_OCCURRENCES_EXCEEDED.get(getIdentifierString()));
    }

    final String lowerStr = StaticUtils.toLowerCase(valueString);
    if (lowerStr.equals("true") || lowerStr.equals("t") ||
        lowerStr.equals("yes") || lowerStr.equals("y") ||
        lowerStr.equals("on") || lowerStr.equals("1"))
    {
      value = Boolean.TRUE;
    }
    else if (lowerStr.equals("false") || lowerStr.equals("f") ||
             lowerStr.equals("no") || lowerStr.equals("n") ||
             lowerStr.equals("off") || lowerStr.equals("0"))
    {
      value = Boolean.FALSE;
    }
    else
    {
      throw new ArgumentException(ERR_ARG_VALUE_NOT_ALLOWED.get(
           valueString, getIdentifierString()));
    }
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public String getDataTypeName()
  {
    return INFO_BOOLEAN_VALUE_TYPE_NAME.get();
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public String getValueConstraints()
  {
    return INFO_BOOLEAN_VALUE_CONSTRAINTS.get();
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public BooleanValueArgument getCleanCopy()
  {
    return new BooleanValueArgument(this);
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public void toString(final StringBuilder buffer)
  {
    buffer.append("BooleanValueArgument(");
    appendBasicToStringInfo(buffer);

    if (defaultValue != null)
    {
      buffer.append(", defaultValue=");
      buffer.append(defaultValue);
    }

    buffer.append(')');
  }
}