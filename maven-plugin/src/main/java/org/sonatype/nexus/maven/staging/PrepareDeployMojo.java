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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/**
 * Goal to set the {@code maven.deploy.skip} property to {@code true} if the current project version is a release
 * version.
 *
 * @since 1.0.4
 */
@Mojo(name = "prepare-deploy", requiresOnline = false, threadSafe = true, defaultPhase = LifecyclePhase.INITIALIZE)
public class PrepareDeployMojo extends StagingMojo {
    @Parameter(defaultValue = "${project}", readonly = true)
    protected MavenProject project;

    @Parameter(defaultValue = "maven.deploy.skip")
    private String deploySkipPropertyName;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (!project.getArtifact().isSnapshot()) {
            getLog().info("Setting " + deploySkipPropertyName + " to true as " + project.getArtifact().getVersion()
                    + " is a release version");
            project.getProperties().setProperty(deploySkipPropertyName, "true");
        }
    }
}
