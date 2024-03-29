/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2019-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
package org.sonatype.nexus.maven.staging;

/**
 * POJO to store an artifact info
 */
public class ArtifactInfo
{
  private String group;
  private String artifactId;
  private String version;
  private String tag;
  private String classifier;
  private String packaging;
  private String extension;
  private String pomFileName;
  private String pluginPrefix;

  public String getGroup() {
    return group;
  }

  public void setGroup(final String group) {
    this.group = group;
  }

  public String getArtifactId() {
    return artifactId;
  }

  public void setArtifactId(final String artifactId) {
    this.artifactId = artifactId;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(final String version) {
    this.version = version;
  }

  public String getTag() {
    return tag;
  }

  public void setTag(final String tag) {
    this.tag = tag;
  }


  public String getClassifier() {
    return classifier;
  }

  public void setClassifier(final String classifier) {
    this.classifier = classifier;
  }

  public String getPackaging() {
    return packaging;
  }

  public void setPackaging(final String packaging) {
    this.packaging = packaging;
  }

  public String getExtension() {
    return extension;
  }

  public void setExtension(final String extension) {
    this.extension = extension;
  }

  public String getPomFileName() {
    return pomFileName;
  }

  public void setPomFileName(final String pomFileName) {
    this.pomFileName = pomFileName;
  }

  public String getPluginPrefix() {
    return pluginPrefix;
  }

  public void setPluginPrefix(final String pluginPrefix) {
    this.pluginPrefix = pluginPrefix;
  }
}
