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
          
          <!-- The server "id" element from settings to use authentication from -->
          <serverId>local-nexus</serverId>
         
          <!-- Which repository to deploy to -->
          <repository>maven-releases</repository>
        </configuration>
      </plugin>
```

To override the default deploy goal add the following to the plugin

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

The plugin currently requires that a tag is specified via ```-Dtag```

e.g. ```mvn install nexus-staging:deploy -Dtag=test```
#Integration testing
To run the integration tests against a docker instance specifty the port you would like the tests to run on:

```-Dnexus.it.port=8085``` 

e.g. ```mvn clean install -Dnexus.it.port=8085```

To run the integration tests against a local instance use the profile ```local-nexus3```

e.g. ```mvn clean install -Plocal-nexus3```