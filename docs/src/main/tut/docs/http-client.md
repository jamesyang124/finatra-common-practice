---
layout: docs
title: Http Client
---

# Http Client

By the gitter8 project - [https://github.com/forthy/finatra.g8](https://github.com/forthy/finatra.g8) supplied `RichHttpClient`, we can extend the http clients as below:

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

After the protocol resolution has been dispatched to proper finagle client services, we can abstract the http client by its url routing from `provideHttpClient`. This method will then be invoke for creating specific http client:

```scala
@Singleton
class ServiceClients @Inject()(implicit val mapper: FinatraObjectMapper, serviceConfigs: Configs) extends Logging {
  lazy val identityHttpClient: HttpClient = provideHttpClient(serviceConfigs.hostConfig.identityHost)
  lazy val profileHttpClient: HttpClient  = provideHttpClient(serviceConfigs.hostConfig.profileHost)
  lazy val oAuthHttpClient: HttpClient    = provideHttpClient(serviceConfigs.hostConfig.oAuthHost)
  lazy val captchaHttpClient: HttpClient  = provideHttpClient(serviceConfigs.hostConfig.captchaHost)

  def executeProfileHttpClient(r: Request)(respHandling: Response => Maybe[Response]): Future[Maybe[Response]] = {
    profileHttpClient
      .execute(r)
      .map(respHandling)
      .rescue {
        case NonFatal(t) =>
          error(s"profileHttpClient handle request future failed - request: $r | exception: $t")
          Future(KO(Errors(ProfileHttpClientRequestError)))
      }
  }

  def executeIdentityHttpClient(r: Request)(respHandling: Response => Maybe[Response]): Future[Maybe[Response]] = {
    identityHttpClient
      .execute(r)
      .map(respHandling)
      .rescue {
        case NonFatal(t) =>
          error(s"IdentityHttpClient handle request future failed - request: $r | exception: $t")
          Future(KO(Errors(IdentityHttpClientRequestError)))
      }
  }

  def executeCaptchaHttpClient(r: Request)(respHandling: Response => Maybe[Response]): Future[Maybe[Response]] = {
    captchaHttpClient
      .execute(r)
      .map(respHandling)
      .rescue {
        case NonFatal(t) =>
          error(s"CaptchaHttpClient handle request future failed - request: $r | exception: $t")
          Future(KO(Errors(CaptchaHttpClientRequestError)))
      }
  }

  def executeOAuthHttpClient(r: Request)(respHandling: Response => Maybe[Response]): Future[Maybe[Response]] = {
    oAuthHttpClient
      .execute(r)
      .map(respHandling)
      .rescue {
        case NonFatal(t) =>
          error(s"OAuthHttpClient handle request future failed - request: $r | exception: $t")
          Future(KO(Errors(OAuthHttpClientRequestError)))
      }
  }
}
```

`Configs` is an dependency containers for providing host config from environment variables. It's implemented by `grafter` with scala macros.

Note that we set each http client instance as `lazy`, so it will be evaluated when it is called first time. This evaluation approach will help the tests inject its mocked `Configs` object without preloading client instances.

For the service layer, the service only handle the request payload and response handling by helper method such as `executeOAuthHttpClient`. This decouple the http client implementation details and service application flow.


```scala
class CreatePhoneIdentityService @Inject()(serviceClients: ServiceClients, mapper: FinatraObjectMapper)
    extends Logging {

  private[this] def createPhoneIdentity(r: CreatePhoneIdentityRequest, validateResponse: Response => Maybe[Response]) =
    post("/priv/accountidentity/identity/phone/create")
      .header(HttpHeaders.ContentType, ContentType.JSON.contentTypeName)
      .body(mapper.writeValueAsString(r))
      .|>(req => serviceClients.executeIdentityHttpClient(req)(validateResponse))

  private[this] def responseHandling(r: Response): Maybe[Response] = r.status match {
    case Status.Ok         => OK(r)
    case Status.Conflict   => KO(Errors(PhoneIsUsedError))
    case Status.BadRequest => KO(Errors(IdentityServiceBadRequestError))
    case _ =>
      error(s"identity service respond with invalid HTTP status code - $r")
      KO(Errors(IdentityServiceResponseError))
  }

  def apply(request: CreatePhoneIdentityRequest): Future[Either[Errors, CreateIdentityResponse]] = {
    (for {
      rawResponse <- FutureEither(createPhoneIdentity(request, responseHandling))
    } yield rawResponse).future
  }
}
```

The `.|>(req => serviceClients.executeIdentityHttpClient(req)(validateResponse))` is a `PipeOperator` which chain the previous result as argument in its input function.
