# backend-base

This library is meant to be extended later by other libraries.  It implements a very small telemetry API that does not do much, require extension to add more capability.

## Requirements
* JDK 11+
* Apache Maven 3.8.1+

## Devworkspace telemetry support
Since version backend-base version 0.0.33, Che server workspace telemetry support has been replaced in favour of DevWorkspace telemetry.
Version 0.0.32 is the latest version supporting Che server workspace telemetry.

## Building

Be sure to set `devworkspace.id` in `src/main/resources/application.properties` or set it on the command line during the maven run:

```shell script
mvn package -Ddevworkspace.id=fake-devworkspace
```

For a native Quarkus image, run:

```shell script
mvn package -Pnative -Dnative-image.docker-build=true -Ddevworkspace.id=fake-devworkspace
[docker|podman] build -f src/main/docker/Dockerfile.native -t quarkus/telemetry-backend-base .
```

## Testing

### Unit testing

```shell script
mvn test
```

###  Native-mode integration testing

```shell script
mvn verify -Pnative -Dquarkus.test.native-image-profile=test
```
