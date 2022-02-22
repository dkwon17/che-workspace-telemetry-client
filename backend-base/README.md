# backend-base

This library is meant to be extended later by other libraries.  It implements a very small telemetry API that does not do much, require extension to add more capability.

## Requirements
* JDK 11+
* Apache Maven 3.8.1+

## Building

Be sure to set `devworkspace.id` in `src/main/resources/application.properties` or set it on the command line during the maven run:

```shell script
mvn package -Ddevworkspace.id=foo
```

For a native Quarkus image, run:

```shell script
mvn package -Pnative -Dnative-image.docker-build=true -Ddevworkspace.id=foo
[docker|podman] build -f src/main/docker/Dockerfile.native -t quarkus/telemetry-backend-base .
```

## Testing

### Unit testing

```shell script
mvn test
```

###  Native-mode integration testing

#### Prerequisites

+ A Running Che Cluster in CRC, Minikube, Kind, etc.
+ A devworkspace ID of a devworkspace in the cluster

In the `src/main/resources/application.properties` file, add:
```
%test.devworkspace.id=<devworkspace ID>
```

Then run:
```shell script
kubectl config set-context --current --namespace=<namespace that the devworkspace is in>
mvn verify -Pnative -Dquarkus.test.native-image-profile=test
```
