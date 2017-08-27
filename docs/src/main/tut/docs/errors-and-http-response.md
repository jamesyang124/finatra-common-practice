---
layout: docs
title: Errors and Http Response
---

# Errors

Since `Errors` library provides an `ErrorBase` base class, we can extend it for API output error message.

```scala
sealed trait ControllerError extends ErrorBase {
  val code: Int
  val msg: String
  override def represent(includeWhen: Boolean): String =
    if (includeWhen) {
      s"""
         | {
         |   "code": $code,
         |   "msg": $msg,
         |   "when": $when
         | }
      """.stripMargin
    } else {
      s"""
         | {
         |   "code": $code,
         |   "msg": $msg
         | }
      """.stripMargin
    }
}
```

Then define error message such as:

```scala
object ControllerError {
  case object EmailAccountExistedControllerError extends ControllerError {
    val code: Int   = 1023
    val msg: String = EmailIsUsedError.name
  }

  case object PhoneAccountExistedControllerError extends ControllerError {
    val code: Int   = 1130
    val msg: String = PhoneIsUsedError.name
  }

  case object InvalidCaptchaControllerError extends ControllerError {
    val code: Int   = 9
    val msg: String = CaptchaServiceBadRequestError.name
  }
}
```

The `msg` string is copied from the `SimpleError` instances:

```scala
object EmailIsUsedError               extends SimpleError("request email is already in used.")
object PhoneIsUsedError               extends SimpleError("request phone is already in used.")
```

For service or repo domain, errors are just `SimpleError` for simplicity. In default request input validation, Finatra will take care of case class mapping exception for it and output a list of errors. To follow this approach, validation errors are also based from `SimpleError`.

The principle is not set up error code inside the system, but return error code by external services' need.

## Convention of Http Status Code

Here is a list of design decision for response Http status code:

|HTTP Status|Code|Description|
|:---|:--:|:--|
|Ok|200|Successfully process request **with response body**|
|Created|201|Response when a **create action** is successfully processed|
|NoContent|204|Successfully process request **without response body**|
|BadRequest|400|Respond to inform client about validation errors, **must return `code` and `msg` as JSON if client need to handle it**, otherwise it would be `SimpleError` format or default finatra validation errors|
|No Found|404|Respond when resource is not existed or cannot be reached.|
|Conflict|409|Respond when request does not meet unique constraint requirement|
|Failed Dependency|424|if other dependent service respond unexpected response, or unaddressable response, service should respond failed dependency for it|
|Internal Server Error|500|Service internal failed|
