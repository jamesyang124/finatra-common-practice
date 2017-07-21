---
layout: docs
title: Test with Docker-it-scala
---

## Setup

Set up docker it Scala container environment:

```scala
import com.whisk.docker.{DockerContainer, DockerKit, DockerReadyChecker}

trait DockerMongodbService extends DockerKit {
  val DefaultMongodbPort = 27017
  val mongodbContainer: DockerContainer = DockerContainer("mongo:3.4.5")
    .withPorts(DefaultMongodbPort -> Some(DefaultMongodbPort))
    .withReadyChecker(DockerReadyChecker.LogLineContains("waiting for connections on port"))
    .withCommand("mongod", "--nojournal", "--smallfiles", "--syncdelay", "0")

  abstract override def dockerContainers: List[DockerContainer] =
    mongodbContainer :: super.dockerContainers
}
```

The `.withReadyChecker` will try to caught the string `"waiting for connections on port"` prompted during mongo launch, then docker-it-scala will hook up tests environment with containers.

## Test Integration

Mix-in related implementations in the integration or feature tests:

```scala
class UpdateStockItemServiceTest
    extends IntegrationTest
    with DockerTestKit
    with DockerKitSpotify
    with DockerMongodbService {
  override val injector: Injector = TestInjector(modules = Seq(CustomJacksonModule, MongoClientModule)).create

  implicit val pc = PatienceConfig(Span(20, Seconds), Span(1, Second))

  test("TBC") {
    pending
  }
}

class ValidateIdentityFeatureTest
    extends FeatureTest
    with Mockito
    with DockerTestKit
    with DockerKitSpotify
    with DockerMongodbService {

  implicit val pc = PatienceConfig(Span(20, Seconds), Span(1, Second))

  override val server: EmbeddedHttpServer = new EmbeddedHttpServer(twitterServer = new Server)

  private[this] val mapper            = server.injector.instance[FinatraObjectMapper, CamelCaseMapper]

  test("TBA") {
    pending
  }
}
```
