---
layout: docs
title: Input Validation
---

# Input Validation

Same type of input validation can be reused by hand-crafted annotation interface or finatra's `MethodValidation` annotation. Though the first approach is more succinct in case class declaration, but the validation logic will not directly present in its declaration. Instead, We suggest to use finatra's `MethodValidation` helpers for readability.

### Customized Annotation Interface

Define annotation interface first:

```scala
// EmailFormatInternal.java
import com.twitter.finatra.validation.Validation;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target({ PARAMETER })
@Retention(RUNTIME)
@Validation(validatedBy = EmailFormatValidator.class)
public @interface EmailFormatInternal {
}
```

Once the annotation interface is defined, supply the validator such as `EmailFormatValidator`:

```scala
import java.util.regex.Pattern
import com.twitter.finatra.validation.{ValidationMessageResolver, ValidationResult, Validator}

class EmailFormatValidator(validationMessageResolver: ValidationMessageResolver,
                                         annotation: EmailFormatInternal)
    extends Validator[EmailFormatInternal, Any](validationMessageResolver, annotation) {

  override def isValid(value: Any): ValidationResult = {
    value match {
      case Some(e: EmailAddress) =>
        validationResult(e)
      case emailValue: EmailAddress =>
        validationResult(emailValue)
      case None      => ValidationResult.validate(true, "")
      case s: String => validationResult(EmailAddress(s))
      case _ =>
        throw new IllegalArgumentException(s"Class [${value.getClass}}] is not supported")
    }
  }

  private def validationResult(value: EmailAddress) = {
    ValidationResult.validate(validate(value), s"Invalid email format, email:${value.email}")
  }

  private def validate(email: com.htc.cs.account.domain.account.EmailAddress): Boolean = {
    val pattern: Pattern = Pattern.compile(
      """^((([a-z]|\d|[!#\$%&'\*\+\-\/=\?\^_`{\|}~]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])+(\.([a-z]|\d|[!#\$%&'\*\+\-\/=\?\^_`{\|}~]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])+)*)|((\x22)((((\x20|\x09)*(\x0d\x0a))?(\x20|\x09)+)?(([\x01-\x08\x0b\x0c\x0e-\x1f\x7f]|\x21|[\x23-\x5b]|[\x5d-\x7e]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(\\([\x01-\x09\x0b\x0c\x0d-\x7f]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF]))))*(((\x20|\x09)*(\x0d\x0a))?(\x20|\x09)+)?(\x22)))@((([a-z]|\d|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(([a-z]|\d|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])*([a-z]|\d|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])))\.)+(([a-z]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(([a-z]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])*([a-z]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])))$""",
      Pattern.CASE_INSENSITIVE
    )

    pattern.matcher(email.email).find()
  }
}
```

The `ValidationMessageResolver`, `ValidationResult`, `Validator` are necessary for creating customized annotation, but the validation logic simply locate at `validate` private method. Also, `isValid` method is required to be overriden.

Finally, define a type alias as annotation format:

```scala
import scala.annotation.meta.param

package object validation {
  type EmailFormat        = EmailFormatInternal @param
}
```

Then prefix it in the field of case class declaration by `EmailFormat` type alias:

```scala
import com.twitter.finatra.request.Header
import com.twitter.finatra.validation.NotEmpty

case class ChangeEmailRequest(@EmailFormat      email: EmailAddress,
                              @NotEmpty @Header token: String)
```

### MethodValidation

Finatra's `@MethodValidation` annotate a customized method definition in case class declaration body.

> A method validation is a case class method annotated with `@MethodValidation` which is intended to be used for validating fields of the cases class during request parsing.

```scala
case class InvalidInputAccountIdError extends SimpleError("Invalid input account id format")

case class InputAccountId(@NotEmpty value: String) extends WrappedValue[String] {
  @MethodValidation
  def validatePasswordInput: ValidationResult = {
    val res = Try(UUID.fromString(value)).isReturn

    ValidationResult.validate(res, InvalidInputAccountIdError.represent(true))
  }
}
```

### Conclusion

Define validation logic for the case class request, also define validation logic in each field of it. For the input request, build validation types to guard the value of each input. Leverage Finatra predefined annotation such as `@NotEmpty`, `@Range`, etc for DRY principle.

```scala
case class EmailAccountRequest(email: InputEmail,
                               languageCode: InputLanguageCode,
                               @NotEmpty firstName: String,
                               @NotEmpty lastName: String)
```

### Examples

#### Account Id

*Account Id* is validated by `java.util.UUID#fromString` method.

```scala
case class InputAccountId(@NotEmpty value: String) extends WrappedValue[String] {
  @MethodValidation
  def validatePasswordInput: ValidationResult = {
    val res = Try(UUID.fromString(value)).isReturn

    ValidationResult.validate(res, InvalidInputAccountIdError.represent(true))
  }
}
```

#### Email

*Email* format follow *RFC 5322* standard and support unicode pattern.

```scala
case class InputEmail(@NotEmpty value: String) extends WrappedValue[String] {
  @MethodValidation
  def validatePasswordInput: ValidationResult = {
    val pattern =
      """^((([a-z]|\d|[!#\$%&'\*\+\-\/=\?\^_`{\|}~]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])+(\.([a-z]|\d|[!#\$%&'\*\+\-\/=\?\^_`{\|}~]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])+)*)|((\x22)((((\x20|\x09)*(\x0d\x0a))?(\x20|\x09)+)?(([\x01-\x08\x0b\x0c\x0e-\x1f\x7f]|\x21|[\x23-\x5b]|[\x5d-\x7e]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(\\([\x01-\x09\x0b\x0c\x0d-\x7f]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF]))))*(((\x20|\x09)*(\x0d\x0a))?(\x20|\x09)+)?(\x22)))@((([a-z]|\d|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(([a-z]|\d|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])*([a-z]|\d|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])))\.)+(([a-z]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(([a-z]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])*([a-z]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])))$"""

    ValidationResult.validate(value.matches(pattern), InvalidInputEmailError.represent(true))
  }
}
```

#### Phone

*Phone* is validated by goole phone number library - [https://github.com/googlei18n/libphonenumber](https://github.com/googlei18n/libphonenumber)

```scala
case class InputPhone(value: String, countryCode: Int) {

  @MethodValidation
  def validateInputPhone: ValidationResult = {
    val res = for {
      p <- PhoneUtils.parse(value, countryCode)
      _ <- PhoneUtils.checkPhoneNumber(p, countryCode)
    } yield true

    ValidationResult.validate(res.isRight, InvalidInputPhoneError.represent(true))
  }
}
```

#### Language Code

*Language code* is one of the value from `Locale.getAvailableLocales` list.

```scala
case class InputLanguageCode(@NotEmpty value: String) extends WrappedValue[String] {
  @MethodValidation
  def validateLanguageCode: ValidationResult = {
    val locales: Seq[String] = Locale.getAvailableLocales.toList.map(_.toString.replace("_", "-"))
    val res                  = value.length != 0 && locales.contains(value)

    ValidationResult.validate(res, InvalidInputLanguageCodeError.represent(true))
  }
}
```

#### Password

*Password* input format require at least 1 upper case, 1 lower case, 1 numerical digit, and the input length is greater than or equal to 8.

```scala
case class InputPassword(@NotEmpty value: String) extends WrappedValue[String] {

  @MethodValidation
  def validatePasswordInput: ValidationResult = {
    val pattern                 = """(?=.*[a-z])(?=.*[A-Z])(?=.*[\d]).{8,}"""
    val validate: () => Boolean = () => value.matches(pattern)

    ValidationResult.validate(validate(), InvalidInputPasswordError.represent(true))
  }
}
```
