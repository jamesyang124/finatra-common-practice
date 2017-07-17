---
layout: docs
title: Input Validation
---

# Input Validations

Below list common input validations for, `email`, `phone`, `accountId`, etc.

```scala
case class InputCaptcha(@NotEmpty imageKey: String, @NotEmpty response: String, nonce: Option[String] = None)

case class InputAccountId(@NotEmpty value: String) extends WrappedValue[String] {
  @MethodValidation
  def validatePasswordInput: ValidationResult = {
    val res = Try(UUID.fromString(value)).isReturn

    ValidationResult.validate(res, InvalidInputAccountIdError.represent(true))
  }
}
```

*Email* should follow *RFC 5322* standard and support unicode pattern:

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

*Phone* should be validated by gogole phone number lib:

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

*Language code* should from `Locale.getAvailableLocales`:

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

*Password* input should follow at least 1 upper case, 1 lower case, 1 numerical digit, and the input length is greater than or eqaul to 8:

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
