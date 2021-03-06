package com.github.mvysny.karibudsl.v10

import com.vaadin.flow.data.binder.*
import com.vaadin.flow.data.converter.*
import com.vaadin.flow.component.HasValue
import com.vaadin.flow.component.textfield.EmailField
import com.vaadin.flow.component.textfield.TextArea
import com.vaadin.flow.component.textfield.TextField
import com.vaadin.flow.data.validator.*
import java.lang.reflect.Method
import java.math.BigDecimal
import java.math.BigInteger
import java.time.*
import java.util.*
import kotlin.reflect.KMutableProperty1
import java.time.LocalDateTime

/**
 * Trims the user input string before storing it into the underlying property data source. Vital for mobile-oriented apps:
 * Android keyboard often adds whitespace to the end of the text when auto-completion occurs. Imagine storing a username ending with a space upon registration:
 * such person can no longer log in from his PC unless he explicitly types in the space.
 * @param blanksToNulls if true then a blank String value is passed as `null` to the model. Defaults to false.
 */
fun <BEAN> Binder.BindingBuilder<BEAN, String?>.trimmingConverter(blanksToNulls: Boolean = false): Binder.BindingBuilder<BEAN, String?> =
        withConverter(object : Converter<String?, String?> {
            override fun convertToModel(value: String?, context: ValueContext?): Result<String?> {
                var trimmedValue: String? = value?.trim()
                if (blanksToNulls && trimmedValue.isNullOrBlank()) {
                    trimmedValue = null
                }
                return Result.ok(trimmedValue)
            }

            override fun convertToPresentation(value: String?, context: ValueContext?): String? {
                // must not return null here otherwise TextField will fail with NPE:
                // workaround for https://github.com/vaadin/framework/issues/8664
                return value ?: ""
            }
        })

fun <BEAN> Binder.BindingBuilder<BEAN, String?>.toInt(): Binder.BindingBuilder<BEAN, Int?> =
        withConverter(StringToIntegerConverter(karibuDslI18n("cantConvertToInteger")))

@JvmName("doubleToInt")
fun <BEAN> Binder.BindingBuilder<BEAN, Double?>.toInt(): Binder.BindingBuilder<BEAN, Int?> =
        withConverter(DoubleToIntConverter)

fun <BEAN> Binder.BindingBuilder<BEAN, String?>.toDouble(
        errorMessage: String = karibuDslI18n("cantConvertToDecimal")
): Binder.BindingBuilder<BEAN, Double?> =
        withConverter(StringToDoubleConverter(errorMessage))

fun <BEAN> Binder.BindingBuilder<BEAN, String?>.toLong(
        errorMessage: String = karibuDslI18n("cantConvertToInteger")
): Binder.BindingBuilder<BEAN, Long?> =
        withConverter(StringToLongConverter(errorMessage))

@JvmName("doubleToLong")
fun <BEAN> Binder.BindingBuilder<BEAN, Double?>.toLong(): Binder.BindingBuilder<BEAN, Long?> =
        withConverter(DoubleToLongConverter)

fun <BEAN> Binder.BindingBuilder<BEAN, String?>.toBigDecimal(
        errorMessage: String = karibuDslI18n("cantConvertToDecimal")
): Binder.BindingBuilder<BEAN, BigDecimal?> =
        withConverter(StringToBigDecimalConverter(errorMessage))

@JvmName("doubleToBigDecimal")
fun <BEAN> Binder.BindingBuilder<BEAN, Double?>.toBigDecimal(): Binder.BindingBuilder<BEAN, BigDecimal?> =
        withConverter(DoubleToBigDecimalConverter)

fun <BEAN> Binder.BindingBuilder<BEAN, String?>.toBigInteger(): Binder.BindingBuilder<BEAN, BigInteger?> =
        withConverter(StringToBigIntegerConverter(karibuDslI18n("cantConvertToInteger")))

@JvmName("doubleToBigInteger")
fun <BEAN> Binder.BindingBuilder<BEAN, Double?>.toBigInteger(): Binder.BindingBuilder<BEAN, BigInteger?> =
        withConverter(DoubleToBigIntegerConverter)

fun <BEAN> Binder.BindingBuilder<BEAN, LocalDate?>.toDate(): Binder.BindingBuilder<BEAN, Date?> =
        withConverter(LocalDateToDateConverter(browserTimeZone))

@JvmName("localDateTimeToDate")
fun <BEAN> Binder.BindingBuilder<BEAN, LocalDateTime?>.toDate(): Binder.BindingBuilder<BEAN, Date?> =
        withConverter(LocalDateTimeToDateConverter(browserTimeZone))

fun <BEAN> Binder.BindingBuilder<BEAN, LocalDate?>.toInstant(): Binder.BindingBuilder<BEAN, Instant?> =
        withConverter(LocalDateToInstantConverter(browserTimeZone))

@JvmName("localDateTimeToInstant")
fun <BEAN> Binder.BindingBuilder<BEAN, LocalDateTime?>.toInstant(): Binder.BindingBuilder<BEAN, Instant?> =
        withConverter(LocalDateTimeToInstantConverter(browserTimeZone))

/**
 * Allows you to create [BeanValidationBinder] like this: `beanValidationBinder<Person>()` instead of `BeanValidationBinder(Person::class.java)`
 */
inline fun <reified T : Any> beanValidationBinder(): BeanValidationBinder<T> = BeanValidationBinder(T::class.java)

/**
 * Allows you to bind the component directly in the component's definition. E.g.
 * ```
 * textField("Name:") {
 *   bind(binder).trimmingConverter().bind(Person::name)
 * }
 * ```
 */
fun <BEAN, FIELDVALUE> HasValue<*, FIELDVALUE>.bind(binder: Binder<BEAN>): Binder.BindingBuilder<BEAN, FIELDVALUE> {
    var builder: Binder.BindingBuilder<BEAN, FIELDVALUE> = binder.forField(this)

    // fix NPE for TextField and TextArea by having a converter which converts null to "" and back.
    @Suppress("UNCHECKED_CAST")
    if (this is TextField || this is TextArea || this is EmailField) {
        builder = builder.withNullRepresentation("" as FIELDVALUE)
    }
    return builder
}

/**
 * A type-safe binding which binds only to a property of given type, found on given bean.
 * @param prop the bean property
 */
fun <BEAN, FIELDVALUE> Binder.BindingBuilder<BEAN, FIELDVALUE>.bind(prop: KMutableProperty1<BEAN, out FIELDVALUE?>): Binder.Binding<BEAN, FIELDVALUE?> =
// oh crap, don't use binding by getter and setter - validations won't work!
// we need to use bind(String) even though that will use undebuggable crappy Java 8 lambdas :-(
//        bind({ bean -> prop.get(bean) }, { bean, value -> prop.set(bean, value) })
        bind(prop.name)

/**
 * A converter that converts between [LocalDate] and [Instant].
 * @property zoneId the time zone id to use.
 */
class LocalDateToInstantConverter(val zoneId: ZoneId = browserTimeZone) : Converter<LocalDate?, Instant?> {
    override fun convertToModel(localDate: LocalDate?, context: ValueContext): Result<Instant?> =
            Result.ok(localDate?.atStartOfDay(zoneId)?.toInstant())

    override fun convertToPresentation(date: Instant?, context: ValueContext): LocalDate? =
            date?.atZone(zoneId)?.toLocalDate()
}

/**
 * A converter that converts between [LocalDateTime] and [Instant].
 * @property zoneId the time zone to use
 */
class LocalDateTimeToInstantConverter(val zoneId: ZoneId = browserTimeZone) : Converter<LocalDateTime?, Instant?> {
    override fun convertToModel(localDate: LocalDateTime?, context: ValueContext): Result<Instant?> =
            Result.ok(localDate?.atZone(zoneId)?.toInstant())

    override fun convertToPresentation(date: Instant?, context: ValueContext): LocalDateTime? =
            date?.atZone(zoneId)?.toLocalDateTime()
}

object DoubleToIntConverter : Converter<Double?, Int?> {
    override fun convertToPresentation(value: Int?, context: ValueContext?): Double? = value?.toDouble()
    override fun convertToModel(value: Double?, context: ValueContext?): Result<Int?> = Result.ok(value?.toInt())
}

object DoubleToLongConverter : Converter<Double?, Long?> {
    override fun convertToPresentation(value: Long?, context: ValueContext?): Double? = value?.toDouble()
    override fun convertToModel(value: Double?, context: ValueContext?): Result<Long?> = Result.ok(value?.toLong())
}

object DoubleToBigDecimalConverter : Converter<Double?, BigDecimal?> {
    override fun convertToPresentation(value: BigDecimal?, context: ValueContext?): Double? = value?.toDouble()
    override fun convertToModel(value: Double?, context: ValueContext?): Result<BigDecimal?> = Result.ok(value?.toBigDecimal())
}

object DoubleToBigIntegerConverter : Converter<Double?, BigInteger?> {
    override fun convertToPresentation(value: BigInteger?, context: ValueContext?): Double? = value?.toDouble()
    override fun convertToModel(value: Double?, context: ValueContext?): Result<BigInteger?> {
        val bi = if (value == null) null else BigInteger(value.toLong().toString())
        return Result.ok(bi)
    }
}

class StringNotBlankValidator(val errorMessage: String = "must not be blank") : Validator<String?> {
    override fun apply(value: String?, context: ValueContext): ValidationResult = when {
        value.isNullOrBlank() -> ValidationResult.error(errorMessage)
        else -> ValidationResult.ok()
    }
}

fun <BEAN> Binder.BindingBuilder<BEAN, String?>.validateNotBlank(
        errorMessage: String = "must not be blank"
): Binder.BindingBuilder<BEAN, String?> =
        withValidator(StringNotBlankValidator(errorMessage))

fun <BEAN> Binder.BindingBuilder<BEAN, String?>.validEmail(
        errorMessage: String = "must be a valid email address"
): Binder.BindingBuilder<BEAN, String?> =
        withValidator(EmailValidator(errorMessage))

fun <BEAN> Binder.BindingBuilder<BEAN, Float?>.validateInRange(range: ClosedRange<Float>): Binder.BindingBuilder<BEAN, Float?> =
        withValidator(FloatRangeValidator("must be in $range", range.start, range.endInclusive))

@JvmName("validateIntInRange")
fun <BEAN> Binder.BindingBuilder<BEAN, Int?>.validateInRange(range: IntRange): Binder.BindingBuilder<BEAN, Int?> =
        withValidator(IntegerRangeValidator("must be in $range", range.start, range.endInclusive))

@JvmName("validateLongInRange")
fun <BEAN> Binder.BindingBuilder<BEAN, Long?>.validateInRange(range: LongRange): Binder.BindingBuilder<BEAN, Long?> =
        withValidator(LongRangeValidator("must be in $range", range.start, range.endInclusive))

@JvmName("validateDoubleInRange")
fun <BEAN> Binder.BindingBuilder<BEAN, Double?>.validateInRange(range: ClosedRange<Double>): Binder.BindingBuilder<BEAN, Double?> =
        withValidator(DoubleRangeValidator("must be in $range", range.start, range.endInclusive))

@JvmName("validateBigIntegerInRange")
fun <BEAN> Binder.BindingBuilder<BEAN, BigInteger?>.validateInRange(range: ClosedRange<BigInteger>): Binder.BindingBuilder<BEAN, BigInteger?> =
        withValidator(BigIntegerRangeValidator("must be in $range", range.start, range.endInclusive))

@JvmName("validateBigDecimalInRange")
fun <BEAN> Binder.BindingBuilder<BEAN, BigDecimal?>.validateInRange(range: ClosedRange<BigDecimal>): Binder.BindingBuilder<BEAN, BigDecimal?> =
        withValidator(BigDecimalRangeValidator("must be in $range", range.start, range.endInclusive))

/**
 * Guesses whether the binder has been configured with read-only.
 *
 * Since Binder doesn't remember whether it is read-only, we have to guess.
 */
val Binder<*>.guessIsReadOnly: Boolean
    get() {
        val bindingsGetter: Method = Binder::class.java.getDeclaredMethod("getBindings")
        bindingsGetter.isAccessible = true
        val bindings: Collection<Binder.Binding<*, *>> = bindingsGetter.invoke(this) as Collection<Binder.Binding<*, *>>
        return bindings.any { it.setter != null && it.isReadOnly }
    }
