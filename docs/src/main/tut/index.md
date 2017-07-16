---
layout: home
---

# Finatra Integrations

This document provide the practice for finatra `2.11` integrating with grafter, docker-it-scala, and the usage for Pipe operator. The hamster `FutureEither` monad transformer will also be used in examples.

...

Based on the finatra g8 porject - [https://github.com/forthy/finatra.g8](https://github.com/forthy/finatra.g8).

# Http Client

```scala
package object client {
  private[client] val toInetAddress: (String, Int) => String = (h, p) => s"inet!$h:$p"
  private[client] val defaultRetryPolicy = Some(
    constantRetry(start = 100.millis, numRetries = 3, shouldRetry = NonFatalExceptions))

  private[client] val toClientService: (Uri) => (Service[Request, Response]) = uri =>
    uri.host.get.|>(host =>
      uri.protocol match {
        case Some("https") => RichHttpClient.newSslClientService(host, toInetAddress(host, 443))
        case _             => RichHttpClient.newClientService(toInetAddress(host, uri.port.getOrElse(80)))
    })

  def provideHttpClient(url: String, retryPolicy: Option[RetryPolicy[Try[Response]]] = defaultRetryPolicy)(
      implicit mapper: FinatraObjectMapper): HttpClient = {
    Uri
      .parse(url)
      .#!("[provideHttpClient] scala uri: ")
      .|>(u => {
        new HttpClient(hostname = u.host.get,
                       httpService = toClientService(u),
                       mapper = mapper,
                       retryPolicy = retryPolicy)
      })

  }
}
```
