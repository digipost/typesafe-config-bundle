# Typesafe config bundle for Dropwizard

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/no.digipost/typesafe-config-bundle/badge.svg)](https://maven-badges.herokuapp.com/maven-central/no.digipost/typesafe-config-bundle)
![](https://github.com/digipost/typesafe-config-bundle/workflows/Build%20and%20deploy/badge.svg)
[![License](https://img.shields.io/badge/license-Apache%202-blue)](https://github.com/digipost/typesafe-config-bundle/blob/main/LICENCE)

## Usage
```java
bootstrap.addBundle(new TypeSafeConfigBundle());
```

## Features

* .conf or .yml format supported
* config for multiple environments in same file

## Examples

```yml
# defaults
logging:
  loggers:
    # output final config to log
    "no.digipost.dropwizard.TypeSafeConfigFactory": debug

database:
  driverClass: org.postgresql.Driver

# environment specific #
environments:
  local:
    database:
      driverClass: org.hsqldb.jdbc.JDBCDriver
      user: SA
      password:

  test:
    database:
      user: test
      password: test
```
