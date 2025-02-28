---
title: Maven
slug: /plugins/maven
sidebar_position: 3
---

# Wirespec Maven Plugin

![Maven Central](https://img.shields.io/maven-central/v/community.flock.wirespec.plugin.maven/wirespec-maven-plugin)

The Wirespec Maven plugin allows you to seamlessly integrate Wirespec compilation into your Maven build process. This enables you to automatically generate code from your Wirespec definitions whenever you build your project, ensuring that your implementation code is always up-to-date with your specifications.

## Installation

To use the Wirespec Maven plugin, you need to add it to your `pom.xml` file. Here's how:

1.  **Add the Plugin Dependency:**

```xml
<build>
    <plugins>
        <plugin>
            <groupId>community.flock.wirespec.plugin.maven</groupId>
            <artifactId>wirespec-maven-plugin</artifactId>
            <version>{{WIRESPEC_VERSION}}</version>
            <executions>
                <execution>
                    <id>typescript</id>
                    <goals>
                        <goal>compile</goal>
                    </goals>
                    <configuration>
                        <input>${project.basedir}/src/main/wirespec</input>
                        <output>${project.basedir}/src/main/typescript/generated</output>
                        <languages>
                            <language>Kotlin</language>
                        </languages>
                    </configuration>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```
2. **Run maven:**  
    `mvn wirespec:compile`

## Bill of materials
Wirespec comes with a bill of materials (BOM). Importing this in the dependency manager of the `pom.xml` makes sure you have access to a curated set of dependencies:

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>community.flock.wirespec</groupId>
            <artifactId>bom</artifactId>
            <version>{{WIRESPEC_VERSION}}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

```
