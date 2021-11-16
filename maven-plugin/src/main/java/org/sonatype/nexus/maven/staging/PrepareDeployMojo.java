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
