package org.sonatype.nexus.maven.staging;

import com.google.common.annotations.VisibleForTesting;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Parent for all "CLI" staging MOJOs (goals) which can be configured from the CLI (or pom) but do not
 * require a project.
 *
 * @since 1.0.0
 */
abstract public class StagingActionMojo
  extends StagingMojo
{
  @Parameter(property = "tag")
  protected String tag;

  @VisibleForTesting
  String getTag() throws MojoExecutionException {
    if (tag == null || tag.isEmpty()) {
      tag = getTagFromPropertiesFile();
    }
    return tag;
  }

  @VisibleForTesting
  void setTag(final String tag) {
    this.tag = tag;
  }
}
