# Typesafe config bundle for Dropwizard

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


# Development

## Releasing

The library is released and deployed to
[Maven Central Repository](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22no.digipost%22%20a%3A%22typesafe-config-bundle%22)
using the [Maven Release Plugin](https://maven.apache.org/maven-release/maven-release-plugin/). Standard procedure with

```bash
mvn release:prepare
...
mvn release:perform
```
(Remember to close/release staging repositories at [Sonatype](https://oss.sonatype.org))
In addition, the plugin has been configured to only work with your local Git repository, so after the release has been performed,
you must remember to push to remote:

```bash
git push --follow-tags
```

You can configure Git to always also push relevant tags when pushing branches, and then you can omit `--follow-tags`:

```bash
git config --global push.followTags true  # this is only needed once
git push                                  # and then you can push and --follow-tags will be implied
```
