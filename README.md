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
# NXRM3 Maven Staging Plugin

This plugin allows Maven projects to be staged in a Nexus Repository Manager 3 repository.

# Example configuration

The basic build configuration requires a url (nexusUrl), repository to deploy (repository) and a server (serverId).
```
      <plugin>
        <groupId>org.sonatype.plugins</groupId>
        <artifactId>nexus-staging-maven-plugin</artifactId>
        <version>${project.version}</version>
        <extensions>true</extensions>
        <configuration>
          <nexusUrl>http://localhost:8081</nexusUrl>
          
          <!-- The server "id" element from settings to use authentication from settings.xml-->
          <serverId>local-nexus</serverId>
         
          <!-- Which repository to deploy to -->
          <repository>maven-releases</repository>
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

# Example usage

The plugin currently requires that a tag is specified via ```-Dtag``` or as a parameter in the plugin configuration 
in the pom file.

e.g. ```mvn install nexus-staging:deploy -Dtag=test``` or 

```
<plugin>
    ...
    <configuration>
      ...
      <tag>...</tag>
       ...
```

# Mutation testing

Run ```mvn -DwithHistory org.pitest:pitest-maven:mutationCoverage``` to calculate mutation coverage. This needs to be 
run from inside the maven-plugin directory rather than at the root of the project to detect the tests.

# Integration testing

To run the integration tests against a docker instance specify the port you would like the tests to run on:

```-Dnexus.it.port=8085``` 

e.g. ```mvn clean install -Dnexus.it.port=8085```

To run the integration tests against a local instance use the profile ```local-nexus3```

e.g. ```mvn clean install -Plocal-nexus3```
