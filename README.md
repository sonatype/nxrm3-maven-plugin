<!--

    Sonatype Nexus (TM) Open Source Version
    Copyright (c) 2019-present Sonatype, Inc.
    All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.

    This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
    which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.

    Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
    of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
    Eclipse Foundation. All other trademarks are the property of their respective owners.

-->
# NXRM3 Maven Plugin

[![Maven Central](https://img.shields.io/maven-central/v/org.sonatype.plugins/nxrm3-maven-plugin.svg?label=Maven%20Central)](https://search.maven.org/artifact/org.sonatype.plugins/nxrm3-maven-plugin)

This [Repository Manager for Maven Plugin](https://help.sonatype.com/integrations/nexus-and-continuous-integration/repository-manager-for-maven-plugin) handles [Nexus Repository Manager 3 staging operations](https://help.sonatype.com/repomanager3/nexus-repository-administration/staging) for Maven projects.

# Example configuration

The basic build configuration requires a url (nexusUrl), repository to deploy (repository) and a server (serverId).
```
      <plugin>
        <groupId>org.sonatype.plugins</groupId>
        <artifactId>nxrm3-maven-plugin</artifactId>
        <version><!-- choose a version --></version>
        <configuration>
          <nexusUrl>http://localhost:8081</nexusUrl>
          
          <!-- The server "id" element from settings to use authentication from settings.xml-->
          <serverId>local-nexus</serverId>
         
          <!-- Which repository to deploy to -->
          <repository>maven-releases</repository>
          
          <!-- Skip the staging deploy mojo -->
          <skipNexusStagingDeployMojo>true</skipNexusStagingDeployMojo>
        </configuration>
      </plugin>
```

To override the default deploy goal add the following to the plugin. This can be used if more control is desired over 
when the plugins deploy goal is activated. 

```
        <executions>
          <execution>
            <id>default-deploy</id>
            <phase>deploy</phase>
            <goals>
              <goal>deploy</goal>
            </goals>
          </execution>
        </executions>
```
    
# Allowing deployment of -SNAPSHOTs

The `nxrm3` plugin will not deploy `-SNAPSHOT` artifacts, and the Maven Deploy plugin will not tag release artifacts when deploying to Nexus.

To allow your project to switch between deployment mechanisms, you will need the `prepare-deploy` goal bound to the lifecycle, e.g.

```
      <plugin>
        <groupId>org.sonatype.plugins</groupId>
        <artifactId>nxrm3-maven-plugin</artifactId>
        <version><!-- choose a version --></version>
        <configuration>
          <nexusUrl>http://localhost:8081</nexusUrl>
          
          <!-- The server "id" element from settings to use authentication from settings.xml-->
          <serverId>local-nexus</serverId>
         
          <!-- Which repository to deploy to -->
          <repository>maven-releases</repository>
        </configuration>
        <executions>
          <execution>
            <id>default-prepare-deploy</id>
            <goals>
              <goal>prepare-deploy</goal> <!-- this will set maven-deploy.skip to true for release versions -->
            </goals>
          </execution>
          <execution>
            <id>default-deploy</id>
            <phase>deploy</phase>
            <goals>
              <goal>deploy</goal> <!-- this will only deploy release versions -->
            </goals>
          </execution>
        </executions>
      </plugin>
```

# Staging
## Example staging usage

The plugin allows a tag to be specified via ```-Dtag``` or as a parameter in the plugin configuration in the pom file.

e.g. ```mvn install nxrm3:staging-deploy -Dtag=test``` or 

```
<plugin>
    ...
    <configuration>
      ...
      <tag>...</tag>
       ...
```

If no tag is specified, one will be generated in the format:
 
 ```<artifactId>-<version>-<timestamp>``` 

e.g. ```myproject-1.5.7-1550242817039```

### Performing a staging move
The plugin currently provides a means for performing a ```move``` of artifacts. The move is performed by conducting a search in a repository for all artifacts tagged with a defined tag. The move operation has three configuration
properties, ``tag``, ```sourceRepository```, and ```destinationRepository```.

The tag can be specified via ```-Dtag``` or the plugin configuration in the pom file. If a tag has not been 
specified, the plugin will attempt to find a tag previously used (and stored) in the ```target``` directory 
of project's the build.

```sourceRepository``` is an optional configuration property which can be specified via ```-DsourceRepository``` 
or the plugin configuration in the pom file. 

NOTE: If the source repository property is not specified, the plugin will default to the ```repository```
property in the plugin configuration in the pom file.

The target repository is a required configuration property specified via ```-DdestinationRepository``` or within the 
plugin configuration in the pom file.

#### Staging Move Usage Example
e.g. ```mvn nxrm3:staging-move -Dtag=build-123 -DsourceRepository=maven-dev -DdestinationRepository=maven-qa``` 

```
<plugin>
    ...
    <configuration>
      ...
      <!--Optional configuration -->
      <sourceRepository>...</sourceRepository>
       ...
       
      <!--Required configuration -->
      <destinationRepository>...</destinationRepository>
        ...
        
      <tag>...</tag>
```

### Performing a staging delete
The plugin provides a means for performing a ```delete``` of tagged artifacts. The delete operates in a similar way to the move operation whereby it performs a search for all artifacts with the specified ```tag```.
The delete operation makes use of a single property ```tag``` and operates as described in [Example staging usage](#example-staging-usage)

#### Staging Delete Usage Example
```mvn nxrm3:staging-delete -Dtag=build-123```

Note: Delete searches **all** repositories for tagged assets.

# Deferred Deployment

There is sometimes a desire to stage the deployment separately from performing the upload. This capability exists via
deferred deployment for release artifacts (Snapshots are not supported). 

The pom file requires configuration as described in [Example configuration](#example-configuration) to which you may add 
a ```stageLocally``` property either within the pom or on the command-line as seen in the example below.

You can also use the ```altStagingDirectory``` property to specify the directory in which to perform the local staging. If not specified, this will default to ```target/nexus-staging``` and will be removed
with ```mvn clean```.

e.g. ,```mvn clean package nxrm3:deploy -DstageLocally -DaltStagingDirectory=myStagingDir``` or

```
<plugin>
    ...
    <configuration>
      ...
      <altStagingDirectory>myStagingDir</altStagingDirectory>
       ...
```

When locally staged, a suitable structure will be automatically created within the staging directory along with a
file (```.index```) that holds metadata to allow subsequent uploading. You can also move this staging directory and
have subsequent uploads performed from the new location as long as the new directory location is specified through the 
```altStagingDirectory``` property on the upload command-line.

e.g., ```mvn nxrm3:upload ... -DaltStagingDirectory=<directory>```

### Deferred Deployment Example

The following example stages the deployment but defers the upload:

```mvn clean package nxrm3:deploy -DstageLocally```

You may now move the staging directory or leave it in place. To guarantee deployment integrity, nothing within this directory structure
should be modified externally, and you should provide appropriate ```altStagingDirectory```
properties where necessary.

Use the following at a later time to upload what was previously staged:

```mvn nxrm3:upload -DserverId=<serverID> -Drepository=<repository> -DnexusUrl=<nexusUrl>```

The parameters above are required as they would typically be within the pom.xml as described in 
[Example configuration](#example-configuration), but the pom is not required as part of the upload.


# Mutation testing

Run ```./mvnw -DwithHistory org.pitest:pitest-maven:mutationCoverage``` to calculate mutation coverage. This needs to be 
run from inside the maven-plugin directory rather than at the root of the project to detect the tests.

# Integration testing

To run the integration tests against a docker instance specify the port you would like the tests to run on:

```-Dnexus.it.port=8085``` 

e.g. ```./mvnw clean install -Dnexus.it.port=8085```

To run the integration tests against a local instance use the profile ```local-nexus3```

e.g. ```./mvnw clean install -Plocal-nexus3```

# How to publish to Maven Central

https://sonatype.atlassian.net/wiki/spaces/BNR/pages/388301035/Publishing+Directly+to+Maven+Central
Steps were added to the jenkinsfile-release to automate this.
To check if the release was successfully published check https://search.maven.org/artifact/org.sonatype.plugins/nxrm3-maven-plugin


# Getting Help

Looking to contribute or need some help?

* If you have an issue, file a ticket [on our public JIRA](https://issues.sonatype.org/projects/NEXUS/) using the identifying component 'nxrm3-maven-plugin'
* If you have questions, ask on our [Nexus Repository User List](https://groups.google.com/a/glists.sonatype.com/forum/?hl=en#!forum/nexus-users)
