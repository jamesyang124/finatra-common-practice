---
layout: docs
title: Introduction
---

# Finagle

Finatra is a RPC framework based on [Finagle](https://twitter.github.io/finagle/guide/) and **TwitterServer** module. It takes the role for JSON request/response processing. It also integrate popular `scala-test` test framework to help developers start their TDD as soon as possible. It is essential components are `controller` and `service`. Both of it are passed into twitter future context therefore the client request processing is asynchronous by default.

Here is an introduction for the finagle and future based event processing:

[Finagle Under the Hood - by Vladimir Kostyukov](https://youtu.be/kfs-dtbG0kY)

In rest of the document, we will introducing the common practice for each pieces of **controller**, **service**, **http client**, **request/response handling**, and **input validation**.

## Controller

Controller require each response should be wrapped in twitter future, then finatra will dispatch it down to netty request processing. Controller is usually implemented for following principle:

1. Only handle request and response.
3. Compose proper types for services from input request.
3. Compose proper response for clients from internal service response.
2. Dispatch validated request to the internal services.
4. Wrapped service layer error to proper Http error response.
5. Support API document for external service integration.

Common case of a controller will be like this:

```scala
package service.controllers.email

@Singleton
class EmailSignUpController @Inject()(implicit protected val swagger: Swagger, emailSignUpService: EmailSignUpService)
    extends SwaggerController {

  postWithDoc("/api/service/v1/mobile/email/create")(createEmailAccountDoc) {
    r: MobileEmailSignUpRequest =>
      emailSignUpService(r).map({
        case OK(_)               => response.noContent
        case KO(Errors(List(e))) => errorsToHttpResponse(e)
      })
  }

  postWithDoc("/api/service/v1/desktop/email/create")(createEmailAccountDoc) {
    r: DesktopEmailSignUpRequest =>
      emailSignUpService(r).map({
        case OK(_)               => response.noContent
        case KO(Errors(List(e))) => errorsToHttpResponse(e)
      })
  }

  private[this] def errorsToHttpResponse(e: ErrorBase) = e match {
    case EmailIsUsedError =>
      response.conflict.json(EmailAccountExistedControllerError)
    case CaptchaServiceBadRequestError =>
      response.badRequest.json(InvalidCaptchaControllerError)
    case t @ _ =>
      error(s"[EmailSignUpController] email sign up internal service dependency error $t")
      response.status(Status.FailedDependency).json(t)
  }
}
```

Notice that the description of swagger document is drag out to same package level object `SwaggerDocument`:

```scala
package service.controllers.email

object SwaggerDocument {
  private[email] val createEmailAccountDoc: Operation => Operation = { o =>
    o.tag("create email account")
      .summary("create for email account")
      .description("create for email account.")
      .consumes("application/json")
      .produces("application/json")
      .response(InternalServerError.code, new Response().description(InternalServerError.reason))
  }

  private[email] val emailSignUpDoc: Operation => Operation = { o =>
    o.tag("...")
      .summary("sign up email account")
      .description("...")
      .consumes("application/json")
      .produces("application/json")
      .response(InternalServerError.code, new Response().description(InternalServerError.reason))
  }
}
```

The service are injected by Scala extension for Google's juice injection - [https://github.com/codingwell/scala-guice](https://github.com/codingwell/scala-guice)

We separate the http response handling to `errorsToHttpResponse` method which convert output response from `ErrorBase` inherited errors. So the `postWithDoc` controller method provide concrete purpose for API readability:

> Controller should only focus on the functionality of request/response transformation, service dispatching, and API document injection.

## Service

Service may handle the application logic or gluing other services for the composition. Basically each service should take single duty for DRY principle. And the higer abstraction level services orchestra single duty services to a larger service unit to fulfill business or application requirement.

A common single duty service will be very similar like this:

```scala
@Singleton
class TokenVerificationService @Inject()(serviceClients: ServiceClients, mapper: FinatraObjectMapper) extends Logging {

  private[this] def issueVerification(r: TokenVerificationRequest, validateResponse: Response => Maybe[Response]) = {
    post("/verify/v1/issue")
      .header(HttpHeaders.ContentType, ContentType.JSON.contentTypeName)
      .body(mapper.writeValueAsString(r))
      .|>(req => serviceClients.executeVerificationHttpClient(req)(validateResponse))
  }

  private[this] def responseHandling(r: Response): Maybe[Response] = r.status match {
    case Status.Ok           => OK(r)
    case Status.BadRequest   => KO(Errors(VerificationServiceBadRequestError))
    case Status.Unauthorized => KO(Errors(VerificationServiceUnauthorizedError))
    case _ =>
      error(s"verification service respond with invalid HTTP status code - response: $r")
      KO(Errors(VerificationServiceResponseError))
  }

  private[this] def parseResponse(resp: Response) = {
    Try(mapper.parse[TokenVerificationResponse](resp.contentString)) match {
      case Return(x) => Future(OK(x))
      case Throw(t) =>
        error(s"finatra object mapper parse response failed - response: $resp | exception: $t")
        Future(KO(Errors(TokenVerificationResponseParseError)))
    }
  }

  def apply(r: TokenVerificationRequest): Future[Maybe[TokenVerificationResponse]] = {
    (for {
      rawResponse <- FutureEither(issueVerification(r, responseHandling))
      resp        <- FutureEither(parseResponse(rawResponse))
    } yield resp).future
  }
}
```

The `|>` operator is a pipe operator for data transformation, check the topic of pip operator for learning its usage.

Service `apply` method takes a part for defining data processing flow, and response transformation if required. This single duty service will be composed to other higher abstraction level service such as:

```scala
class EmailSignUpService @Inject()(validateCaptcha: ValidateCaptchaService,
                                   createEmailIdentity: CreateEmailIdentityService,
                                   createEnterpriseEmailProfile: CreateEnterpriseEmailProfileService,
                                   emailSignUpVerification: TokenVerificationService)
    extends Logging {

  private[this] def toCreateEmailIdentityRequest(acc: InputEnterpriseEmailAccount) =
    CreateEmailIdentityRequest(
      password = acc.password.value,
      connectionId = ConnectionId(acc.email.value)
    ).|>(p => Future(OK(p)))

  private[this] def toEnterpriseEmailProfile(response: CreateIdentityResponse, account: InputEnterpriseEmailAccount) =
    EnterpriseProfile(
      id = response.accountId,
      firstName = account.firstName,
      lastName = account.lastName,
      email = account.email.value
    ).|>(p => Future(OK(p)))

  private[this] def toTokenVerificationRequest(id: AccountId, req: EnterpriseEmailSignUpRequest) =
    TokenVerificationRequest(topic = EmailSignUp,
                             accountId = id,
                             languageCode = req.account.languageCode.value)
      .|>(p => Future(OK(p)))

  private[this] def toResponse(iResp: CreateIdentityResponse) =
    EnterpriseEmailSignUpResponse(iResp.accountId)

  def apply(req: EnterpriseEmailSignUpRequest): Future[Maybe[EnterpriseEmailSignUpResponse]] = {
    (for {
      _                <- FutureEither(validateCaptcha(req.captcha))
      identityReq      <- FutureEither(toCreateEmailIdentityRequest(req.account))
      identityResponse <- FutureEither(createEmailIdentity(identityReq))
      profile          <- FutureEither(toEnterpriseEmailProfile(identityResponse, req.account))
      _                <- FutureEither(createEnterpriseEmailProfile(profile))
      verifyReq        <- FutureEither(toTokenVerificationRequest(identityResponse.accountId, req))
      _                <- FutureEither(emailSignUpVerification(verifyReq))
    } yield toResponse(identityResponse)).future
  }
}
```

The `EmailSignUpService` is a highly abstraction for the business requirement. During each service transfer, the data type must reshape to another type in order to enter single duty services. By this mean, the data transformation will be controlled by `EmailSignUpService` and compose to a higher abstraction level service as its naming.
