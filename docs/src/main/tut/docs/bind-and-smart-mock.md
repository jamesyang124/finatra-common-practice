---
layout: docs
title: Bind and smartMock
---

## Bind

Since Fiantra 2.11, the way to inject mock objects in test is changed, example in follow:

```scala
class SomeServiceTest extends IntegrationTest with Mockito {
  private[this] val cacheService = smartMock[GuavaCacheService]
  private[this] val mockConfig   = smartMock[Configs]
  mockConfig.hostConfig returns TestConfigs.prod.host
  mockConfig.captchaCacheConfig returns TestConfigs.prod.captchaCache

  override protected val injector: Injector =
    TestInjector(modules = Seq(CustomJacksonModule))
      .bind[GuavaCacheService](cacheService)
      .bind[Configs](mockConfig)
      .create

  test("TBC") {
    mockConfig.hostConfig returns TestConfigs.prod2.host
  }
}
```

The mocked object needs to declare and fill the mock before `injector`'s evaluation. In later, override the return mock in test code block is workable. If the mock object is declare as `lazy` evaluate instead of just `val`, it might allow to fill the mock until each running of testing block.
