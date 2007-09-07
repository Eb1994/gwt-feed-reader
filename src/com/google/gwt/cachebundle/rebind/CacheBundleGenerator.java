/*
 * Copyright 2007 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwt.cachebundle.rebind;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.ext.BadPropertyValueException;
import com.google.gwt.core.ext.Generator;
import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.PropertyOracle;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.dev.util.Util;
import com.google.gwt.user.rebind.ClassSourceFileComposerFactory;
import com.google.gwt.user.rebind.SourceWriter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URL;

/**
 * Copies selected files into module output with strong names and generates the
 * CacheBundle mappings.
 */
public class CacheBundleGenerator extends Generator {
  /**
   * The name of a deferred binding property that determines whether or not this
   * generator will rename the incoming resources to strong file names.
   */
  private static final String ENABLE_RENAMING = "CacheBundle.enableRenaming";

  /**
   * The metadata tag name that specifies the classpath location of the target
   * resource.
   */
  private static final String METADATA_TAG = "gwt.resource";

  public String generate(TreeLogger logger, GeneratorContext context,
      String typeName) throws UnableToCompleteException {

    // The TypeOracle knows about all types in the type system
    final TypeOracle typeOracle = context.getTypeOracle();

    // Get a reference to the type that the generator should implement
    final JClassType sourceType = typeOracle.findType(typeName);

    // Ensure that the requested type exists
    if (sourceType == null) {
      logger.log(TreeLogger.ERROR, "Could not find requested typeName", null);
      throw new UnableToCompleteException();
    }

    // Pick a name for the generated class to not conflict. Enclosing class
    // names must be preserved.
    final String generatedSimpleSourceName =
        generateSimpleSourceName(sourceType.getName());

    // Begin writing the generated source.
    final ClassSourceFileComposerFactory f =
        new ClassSourceFileComposerFactory(sourceType.getPackage().getName(),
            generatedSimpleSourceName);

    // The generated class needs to be able to determine the module base URL
    f.addImport(GWT.class.getName());

    // Determine the interface to implement
    if (sourceType.isInterface() != null) {
      f.addImplementedInterface(sourceType.getQualifiedSourceName());

    } else {
      // The incoming type wasn't a plain interface, we don't support
      // abstract base classes
      logger.log(TreeLogger.ERROR, sourceType.getQualifiedSourceName()
          + " is not an interface.", null);
      throw new UnableToCompleteException();
    }

    // All source gets written through this Writer
    final PrintWriter out =
        context.tryCreate(logger, sourceType.getPackage().getName(),
            generatedSimpleSourceName);

    // If an implementation already exists, we don't need to do any work
    if (out != null) {

      // We really use a SourceWriter since it's convenient
      final SourceWriter sw = f.createSourceWriter(context, out);

      JMethod[] methods = sourceType.getMethods();

      for (int i = 0; i < methods.length; i++) {
        JMethod m = methods[i];

        // The local URL by which the generator can access the file's content
        URL resourceUrl = getResourceUrlFromMetaData(logger, m);

        // Just for convenience when examining the generated file
        sw.println("// " + resourceUrl.toExternalForm());

        // Strip off all but the access modifiers
        sw.print(m.getReadableDeclaration(false, true, true, true, true));
        sw.println(" {");
        sw.indent();

        // The expression to return the user.
        String expression = addToOutput(logger, context, resourceUrl);

        // Return the value. Simple expressions will generally be inlined
        // as part of the compiler's optimization passes
        sw.println("return " + expression + ";");

        sw.outdent();
        sw.println("}");
      }

      // Write the generated code to disk
      sw.commit(logger);
    }

    // Return the name of the concrete class
    return f.getCreatedClassName();
  }

  /**
   * Add a CacheBundle-referenced file to the module's output. The extension of
   * the resource will be preserved to ensure mime-types are correct.
   * 
   * @return A Java expression that will give the final URL of the resource
   */
  protected String addToOutput(TreeLogger logger, GeneratorContext context,
      URL resource) throws UnableToCompleteException {
    byte[] bytes = Util.readURLAsBytes(resource);

    PropertyOracle propertyOracle = context.getPropertyOracle();
    String enableRenaming = null;
    try {
      enableRenaming = propertyOracle.getPropertyValue(logger, ENABLE_RENAMING);
    } catch (BadPropertyValueException e) {
      logger.log(TreeLogger.ERROR, "Bad value for " + ENABLE_RENAMING, e);
      throw new UnableToCompleteException();
    }

    String fileName = resource.getPath();
    String outputName;

    if (Boolean.parseBoolean(enableRenaming)) {
      String strongName = Util.computeStrongName(bytes);

      // Determine the extension of the original file
      String extension;
      int lastIdx = fileName.lastIndexOf('.');
      if (lastIdx != -1) {
        extension = fileName.substring(lastIdx + 1);
      } else {
        extension = "noext";
      }

      // The name will be MD5.cache.ext
      outputName = strongName + ".cache." + extension;

    } else {
      outputName = fileName.substring(fileName.lastIndexOf('/') + 1);
    }

    // Ask the context for an OutputStream into the named resource
    OutputStream out = context.tryCreateResource(logger, outputName);

    // This would be null if the resource has already been created in the output
    // (because two or more resources had identical content).
    if (out != null) {
      try {
        InputStream in = resource.openStream();
        Util.copy(logger, in, out);
        in.close();

      } catch (IOException e) {
        logger.log(TreeLogger.ERROR, "Unable to copy resource "
            + resource.toExternalForm() + " to output name " + outputName, e);
        throw new UnableToCompleteException();
      }

      // If there's an error, this won't be called and there will be nothing
      // created in the output directory.
      context.commitResource(logger, out);

      logger.log(TreeLogger.DEBUG, "Copied " + resource.toExternalForm()
          + " to " + outputName, null);
    }

    // Adding the module base URL ensures that the resources will be accessible
    // even in a cross-site inclusion scenario.
    return "GWT.getModuleBaseURL() + \"" + outputName + "\"";
  }

  /**
   * Given a user-defined type name, determine the type name for the generated
   * class.
   */
  protected String generateSimpleSourceName(String sourceType) {
    return sourceType.replaceAll("\\.", "_") + "_externalBundle";
  }

  /**
   * Determine the resource to include for a given method declaration. Assume
   * this is only called for valid methods. This is a cut-down version of the
   * logic found in ImageBundleBuilder
   */
  private URL getResourceUrlFromMetaData(TreeLogger logger, JMethod method)
      throws UnableToCompleteException {

    String[][] md = method.getMetaData(METADATA_TAG);

    if (md.length != 1) {
      logger.log(TreeLogger.ERROR, "Method " + method.getName()
          + " does not have a @" + METADATA_TAG + " annotation", null);
      throw new UnableToCompleteException();
    }

    // Metadata is available, so get the resource name from the metadata
    int lastTagIndex = md.length - 1;
    int lastValueIndex = md[lastTagIndex].length - 1;
    String resourceNameFromMetaData = md[lastTagIndex][lastValueIndex];

    // Make sure the name is either absolute or package-relative.
    if (resourceNameFromMetaData.indexOf("/") == -1) {
      String pkgName = method.getEnclosingType().getPackage().getName();
      // This construction handles the default package correctly, too.
      resourceNameFromMetaData =
          pkgName.replace('.', '/') + "/" + resourceNameFromMetaData;
    }

    // Make sure that the resource exists on the classpath. In the future,
    // this code will have to be changed if resources are loaded from the
    // source path or public path.
    URL resourceURL =
        getClass().getClassLoader().getResource(resourceNameFromMetaData);
    if (resourceURL == null) {
      logger.log(TreeLogger.ERROR, "Resource " + resourceNameFromMetaData
          + " not found on classpath. "
          + "Is the name specified as Class.getResource() would expect?", null);
      throw new UnableToCompleteException();
    }

    // In the future, it would be desirable to be able to automatically
    // determine the resource name to use from the method declaration. We're
    // currently limited by the inability to list the contents of the classpath
    // and not having a set number of file extensions to empirically test.

    return resourceURL;
  }
}