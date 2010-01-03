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



import java.io.File;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.List;

import com.unboundid.asn1.ASN1OctetString;
import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.Entry;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.ldap.sdk.schema.AttributeTypeDefinition;
import com.unboundid.ldap.sdk.schema.ObjectClassDefinition;
import com.unboundid.ldif.LDIFWriter;
import com.unboundid.util.CommandLineTool;
import com.unboundid.util.Mutable;
import com.unboundid.util.ThreadSafety;
import com.unboundid.util.ThreadSafetyLevel;
import com.unboundid.util.args.ArgumentException;
import com.unboundid.util.args.ArgumentParser;
import com.unboundid.util.args.FileArgument;
import com.unboundid.util.args.StringArgument;

import static com.unboundid.ldap.sdk.persist.PersistMessages.*;
import static com.unboundid.util.Debug.*;
import static com.unboundid.util.StaticUtils.*;



/**
 * This class provides a tool which can be used to generate LDAP attribute
 * type and object class definitions which may be used to store objects
 * created from a specified Java class.  The given class must be included in the
 * classpath of the JVM used to invoke the tool, and must be marked with the
 * {@link LDAPObject} annotation.
 */
@Mutable()
@ThreadSafety(level=ThreadSafetyLevel.NOT_THREADSAFE)
public final class GenerateSchemaFromSource
       extends CommandLineTool
       implements Serializable
{
  /**
   * The serial version UID for this serializable class.
   */
  private static final long serialVersionUID = 1029934829295836935L;



  // Arguments used by this tool.
  private FileArgument   outputFileArg;
  private StringArgument classNameArg;



  /**
   * Parse the provided command line arguments and perform the appropriate
   * processing.
   *
   * @param  args  The command line arguments provided to this program.
   */
  public static void main(final String[] args)
  {
    final ResultCode resultCode = main(args, System.out, System.err);
    if (resultCode != ResultCode.SUCCESS)
    {
      System.exit(resultCode.intValue());
    }
  }



  /**
   * Parse the provided command line arguments and perform the appropriate
   * processing.
   *
   * @param  args       The command line arguments provided to this program.
   * @param  outStream  The output stream to which standard out should be
   *                    written.  It may be {@code null} if output should be
   *                    suppressed.
   * @param  errStream  The output stream to which standard error should be
   *                    written.  It may be {@code null} if error messages
   *                    should be suppressed.
   *
   * @return  A result code indicating whether the processing was successful.
   */
  public static ResultCode main(final String[] args,
                                final OutputStream outStream,
                                final OutputStream errStream)
  {
    final GenerateSchemaFromSource tool =
         new GenerateSchemaFromSource(outStream, errStream);
    return tool.runTool(args);
  }



  /**
   * Creates a new instance of this tool.
   *
   * @param  outStream  The output stream to which standard out should be
   *                    written.  It may be {@code null} if output should be
   *                    suppressed.
   * @param  errStream  The output stream to which standard error should be
   *                    written.  It may be {@code null} if error messages
   *                    should be suppressed.
   */
  public GenerateSchemaFromSource(final OutputStream outStream,
                                  final OutputStream errStream)
  {
    super(outStream, errStream);
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public String getToolName()
  {
    return "generate-schema-from-source";
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public String getToolDescription()
  {
    return INFO_GEN_SCHEMA_TOOL_DESCRIPTION.get();
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public void addToolArguments(final ArgumentParser parser)
         throws ArgumentException
  {
    classNameArg = new StringArgument('c', "javaClass", true, 1,
         INFO_GEN_SCHEMA_VALUE_PLACEHOLDER_CLASS.get(),
         INFO_GEN_SCHEMA_ARG_DESCRIPTION_JAVA_CLASS.get());
    parser.addArgument(classNameArg);

    outputFileArg = new FileArgument('f', "outputFile", true, 1,
         INFO_GEN_SCHEMA_VALUE_PLACEHOLDER_PATH.get(),
         INFO_GEN_SCHEMA_ARG_DESCRIPTION_OUTPUT_FILE.get(), false, true, true,
         false);
    parser.addArgument(outputFileArg);
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public ResultCode doToolProcessing()
  {
    // Load the specified Java class.
    final String className = classNameArg.getValue();
    final Class<?> targetClass;
    try
    {
      targetClass = Class.forName(className);
    }
    catch (Exception e)
    {
      debugException(e);
      err(ERR_GEN_SCHEMA_CANNOT_LOAD_CLASS.get(className));
      return ResultCode.PARAM_ERROR;
    }


    // Create an LDAP persister for the class and use it to ensure that the
    // class is valid.
    final LDAPPersister<?> persister;
    try
    {
      persister = (LDAPPersister<?>) LDAPPersister.class.getConstructor(
           Class.class).newInstance(targetClass);
    }
    catch (Exception e)
    {
      debugException(e);
      err(ERR_GEN_SCHEMA_INVALID_CLASS.get(className, getExceptionMessage(e)));
      return ResultCode.LOCAL_ERROR;
    }


    // Use the persister to generate the attribute type and object class
    // definitions.
    final List<AttributeTypeDefinition> attrTypes;
    try
    {
      attrTypes = persister.constructAttributeTypes();
    }
    catch (Exception e)
    {
      debugException(e);
      err(ERR_GEN_SCHEMA_ERROR_CONSTRUCTING_ATTRS.get(className,
           getExceptionMessage(e)));
      return ResultCode.LOCAL_ERROR;
    }

    final List<ObjectClassDefinition> objectClasses;
    try
    {
      objectClasses = persister.constructObjectClasses();
    }
    catch (Exception e)
    {
      debugException(e);
      err(ERR_GEN_SCHEMA_ERROR_CONSTRUCTING_OCS.get(className,
           getExceptionMessage(e)));
      return ResultCode.LOCAL_ERROR;
    }


    // Convert the attribute type and object class definitions into their
    // appropriate string representations.
    int i=0;
    final ASN1OctetString[] attrTypeValues =
         new ASN1OctetString[attrTypes.size()];
    for (final AttributeTypeDefinition d : attrTypes)
    {
      attrTypeValues[i++] = new ASN1OctetString(d.toString());
    }

    i=0;
    final ASN1OctetString[] ocValues =
         new ASN1OctetString[objectClasses.size()];
    for (final ObjectClassDefinition d : objectClasses)
    {
      ocValues[i++] = new ASN1OctetString(d.toString());
    }


    // Construct a schema entry to be written.
    final Entry schemaEntry = new Entry("cn=schema",
         new Attribute("objectClass", "top", "ldapSubentry", "subschema"),
         new Attribute("cn", "schema"),
         new Attribute("attributeTypes", attrTypeValues),
         new Attribute("objectClasses", ocValues));


    // Write the schema entry to the specified file.
    final File outputFile = outputFileArg.getValue();
    try
    {
      final LDIFWriter ldifWriter = new LDIFWriter(outputFile);
      ldifWriter.writeEntry(schemaEntry);
      ldifWriter.close();
    }
    catch (final Exception e)
    {
      debugException(e);
      err(ERR_GEN_SCHEMA_CANNOT_WRITE_SCHEMA.get(outputFile.getAbsolutePath(),
           getExceptionMessage(e)));
      return ResultCode.LOCAL_ERROR;
    }


    return ResultCode.SUCCESS;
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public LinkedHashMap<String[],String> getExampleUsages()
  {
    final LinkedHashMap<String[],String> examples =
         new LinkedHashMap<String[],String>(1);

    final String[] args =
    {
      "--javaClass", "com.example.MyClass",
      "--outputFile", "MyClass-schema.ldif"
    };
    examples.put(args, INFO_GEN_SCHEMA_EXAMPLE_1.get());

    return examples;
  }
}
