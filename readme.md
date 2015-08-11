# Typesafe config bundle for Dropwizard

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
