package community.flock.wirespec.ir.core

import kotlin.js.JsName

interface Transformer {
    fun transformType(type: Type): Type = type.transformChildren(this)
    fun transformElement(element: Element): Element = element.transformChildren(this)
    fun transformStatement(statement: Statement): Statement = statement.transformChildren(this)
    fun transformExpression(expression: Expression): Expression = expression.transformChildren(this)
    fun transformField(field: Field): Field = field.transformChildren(this)
    fun transformParameter(parameter: Parameter): Parameter = parameter.transformChildren(this)
    fun transformConstructor(constructor: Constructor): Constructor = constructor.transformChildren(this)
    fun transformCase(case: Case): Case = case.transformChildren(this)
}

@Dsl
class TransformerBuilder @PublishedApi internal constructor() {
    private var transformType: ((Type, Transformer) -> Type)? = null
    private var transformElement: ((Element, Transformer) -> Element)? = null
    private var transformStatement: ((Statement, Transformer) -> Statement)? = null
    private var transformExpression: ((Expression, Transformer) -> Expression)? = null
    private var transformField: ((Field, Transformer) -> Field)? = null
    private var transformParameter: ((Parameter, Transformer) -> Parameter)? = null
    private var transformConstructor: ((Constructor, Transformer) -> Constructor)? = null
    private var transformCase: ((Case, Transformer) -> Case)? = null

    fun type(transform: (Type, Transformer) -> Type) {
        transformType = transform
    }
    fun element(transform: (Element, Transformer) -> Element) {
        transformElement = transform
    }
    fun statement(transform: (Statement, Transformer) -> Statement) {
        transformStatement = transform
    }
    fun expression(transform: (Expression, Transformer) -> Expression) {
        transformExpression = transform
    }
    fun field(transform: (Field, Transformer) -> Field) {
        transformField = transform
    }
    fun parameter(transform: (Parameter, Transformer) -> Parameter) {
        transformParameter = transform
    }

    @JsName("constructorNode")
    fun constructor(transform: (Constructor, Transformer) -> Constructor) {
        transformConstructor = transform
    }
    fun case(transform: (Case, Transformer) -> Case) {
        transformCase = transform
    }

    fun statementAndExpression(block: (Statement, Transformer) -> Statement) {
        statement(block)
        expression { e, t ->
            (e as? Statement)?.let { block(it, t) } ?: e.transformChildren(t)
        }
    }

    @PublishedApi
    internal fun build(): Transformer = object : Transformer {
        override fun transformType(type: Type): Type = transformType?.invoke(type, this) ?: type.transformChildren(this)
        override fun transformElement(element: Element): Element = transformElement?.invoke(element, this) ?: element.transformChildren(this)
        override fun transformStatement(statement: Statement): Statement = transformStatement?.invoke(statement, this) ?: statement.transformChildren(this)
        override fun transformExpression(expression: Expression): Expression = transformExpression?.invoke(expression, this) ?: expression.transformChildren(this)
        override fun transformField(field: Field): Field = transformField?.invoke(field, this) ?: field.transformChildren(this)
        override fun transformParameter(parameter: Parameter): Parameter = transformParameter?.invoke(parameter, this) ?: parameter.transformChildren(this)
        override fun transformConstructor(constructor: Constructor): Constructor = transformConstructor?.invoke(constructor, this) ?: constructor.transformChildren(this)
        override fun transformCase(case: Case): Case = transformCase?.invoke(case, this) ?: case.transformChildren(this)
    }
}

inline fun transformer(block: TransformerBuilder.() -> Unit): Transformer = TransformerBuilder().apply(block).build()

fun Type.transformChildren(transformer: Transformer): Type = when (this) {
    is Type.Array -> copy(elementType = transformer.transformType(elementType))
    is Type.Dict -> copy(
        keyType = transformer.transformType(keyType),
        valueType = transformer.transformType(valueType),
    )
    is Type.Custom -> copy(generics = generics.map { transformer.transformType(it) })
    is Type.Nullable -> copy(type = transformer.transformType(type))
    is Type.Integer, is Type.Number, Type.Any, Type.String, Type.Boolean, Type.Bytes, Type.Unit, Type.Wildcard, Type.Reflect, is Type.IntegerLiteral, is Type.StringLiteral -> this
}

fun Element.transformChildren(transformer: Transformer): Element = when (this) {
    is File -> copy(elements = elements.map { transformer.transformElement(it) })
    is Package -> this
    is Import -> copy(type = transformer.transformType(type) as Type.Custom)
    is Struct -> copy(
        fields = fields.map { transformer.transformField(it) },
        constructors = constructors.map { transformer.transformConstructor(it) },
        interfaces = interfaces.map { transformer.transformType(it) as Type.Custom },
        elements = elements.map { transformer.transformElement(it) },
    )
    is Function -> copy(
        parameters = parameters.map { transformer.transformParameter(it) },
        returnType = returnType?.let { transformer.transformType(it) },
        body = body.map { transformer.transformStatement(it) },
    )
    is Namespace -> copy(
        elements = elements.map { transformer.transformElement(it) },
        extends = extends?.let { transformer.transformType(it) as Type.Custom },
    )
    is Interface -> copy(
        elements = elements.map { transformer.transformElement(it) },
        extends = extends.map { transformer.transformType(it) as Type.Custom },
        fields = fields.map { transformer.transformField(it) },
    )
    is Union -> copy(
        extends = extends?.let { transformer.transformType(it) as Type.Custom },
        members = members.map { transformer.transformType(it) as Type.Custom },
        typeParameters = typeParameters.map {
            TypeParameter(
                transformer.transformType(it.type),
                it.extends.map { e -> transformer.transformType(e) },
            )
        },
    )
    is Enum -> copy(
        extends = extends?.let { transformer.transformType(it) as Type.Custom },
        fields = fields.map { transformer.transformField(it) },
        constructors = constructors.map { transformer.transformConstructor(it) },
        elements = elements.map { transformer.transformElement(it) },
    )
    is Main -> copy(
        statics = statics.map { transformer.transformElement(it) },
        body = body.map { transformer.transformStatement(it) },
    )
    is RawElement -> this
}

fun Field.transformChildren(transformer: Transformer): Field = copy(type = transformer.transformType(type))

fun Parameter.transformChildren(transformer: Transformer): Parameter = copy(type = transformer.transformType(type))

fun Constructor.transformChildren(transformer: Transformer): Constructor = copy(
    parameters = parameters.map { transformer.transformParameter(it) },
    body = body.map { transformer.transformStatement(it) },
)

fun Statement.transformChildren(transformer: Transformer): Statement = when (this) {
    is PrintStatement -> copy(expression = transformer.transformExpression(expression))
    is ReturnStatement -> copy(expression = transformer.transformExpression(expression))
    is ConstructorStatement -> copy(
        type = transformer.transformType(type),
        namedArguments = namedArguments.mapValues { transformer.transformExpression(it.value) },
    )
    is Literal -> copy(type = transformer.transformType(type))
    is LiteralList -> copy(
        values = values.map { transformer.transformExpression(it) },
        type = transformer.transformType(type),
    )
    is LiteralMap -> copy(
        values = values.mapValues { transformer.transformExpression(it.value) },
        keyType = transformer.transformType(keyType),
        valueType = transformer.transformType(valueType),
    )
    is Assignment -> copy(value = transformer.transformExpression(value))
    is ErrorStatement -> copy(message = transformer.transformExpression(message))
    is AssertStatement -> copy(
        expression = transformer.transformExpression(expression),
    )
    is Switch -> copy(
        expression = transformer.transformExpression(expression),
        cases = cases.map { transformer.transformCase(it) },
        default = default?.map { transformer.transformStatement(it) },
    )
    is RawExpression -> this
    is NullLiteral -> this
    is NullableEmpty -> this
    is VariableReference -> this
    is FieldCall -> copy(receiver = receiver?.let { transformer.transformExpression(it) })
    is FunctionCall -> copy(
        receiver = receiver?.let { transformer.transformExpression(it) },
        arguments = arguments.mapValues { transformer.transformExpression(it.value) },
    )
    is ArrayIndexCall -> copy(
        receiver = transformer.transformExpression(receiver),
        index = transformer.transformExpression(index),
    )
    is EnumReference -> copy(enumType = transformer.transformType(enumType) as Type.Custom)
    is EnumValueCall -> copy(expression = transformer.transformExpression(expression))
    is BinaryOp -> copy(
        left = transformer.transformExpression(left),
        right = transformer.transformExpression(right),
    )
    is TypeDescriptor -> copy(type = transformer.transformType(type))
    is NullCheck -> copy(
        expression = transformer.transformExpression(expression),
        body = transformer.transformExpression(body),
        alternative = alternative?.let { transformer.transformExpression(it) },
    )
    is NullableMap -> copy(
        expression = transformer.transformExpression(expression),
        body = transformer.transformExpression(body),
        alternative = transformer.transformExpression(alternative),
    )
    is NullableOf -> copy(expression = transformer.transformExpression(expression))
    is NullableGet -> copy(expression = transformer.transformExpression(expression))
    is Constraint.RegexMatch -> copy(value = transformer.transformExpression(value))
    is Constraint.BoundCheck -> copy(value = transformer.transformExpression(value))
    is NotExpression -> copy(expression = transformer.transformExpression(expression))
    is IfExpression -> copy(
        condition = transformer.transformExpression(condition),
        thenExpr = transformer.transformExpression(thenExpr),
        elseExpr = transformer.transformExpression(elseExpr),
    )
    is MapExpression -> copy(
        receiver = transformer.transformExpression(receiver),
        body = transformer.transformExpression(body),
    )
    is FlatMapIndexed -> copy(
        receiver = transformer.transformExpression(receiver),
        body = transformer.transformExpression(body),
    )
    is ListConcat -> copy(lists = lists.map { transformer.transformExpression(it) })
    is StringTemplate -> copy(
        parts = parts.map {
            when (it) {
                is StringTemplate.Part.Text -> it
                is StringTemplate.Part.Expr -> StringTemplate.Part.Expr(transformer.transformExpression(it.expression))
            }
        },
    )
}

fun Expression.transformChildren(transformer: Transformer): Expression = when (this) {
    is ClassReference -> copy(type = transformer.transformType(type))
    is RawExpression -> this
    is Statement -> transformChildren(transformer) as Expression
}

fun Case.transformChildren(transformer: Transformer): Case = copy(
    value = transformer.transformExpression(value),
    body = body.map { transformer.transformStatement(it) },
    type = type?.let { transformer.transformType(it) },
)

@Suppress("UNCHECKED_CAST")
@PublishedApi
internal fun <T : Element> T.transform(transformer: Transformer): T = transformer.transformElement(this) as T

@Dsl
class TransformScope<E : Element> @PublishedApi internal constructor(
    @PublishedApi internal var element: Element,
) {
    inline fun <reified M : Type> matching(crossinline transform: (M) -> Type) {
        element = element.transformMatching(transform)
    }

    inline fun <reified M : Element> matchingElements(crossinline transform: (M) -> Element) {
        element = element.transformMatchingElements(transform)
    }

    fun fieldsWhere(predicate: (Field) -> Boolean, transform: (Field) -> Field) {
        element = element.transformFieldsWhere(predicate, transform)
    }

    fun fields(transform: (Field) -> Field) {
        fieldsWhere({ true }, transform)
    }

    fun parametersWhere(predicate: (Parameter) -> Boolean, transform: (Parameter) -> Parameter) {
        element = element.transformParametersWhere(predicate, transform)
    }

    fun parameters(transform: (Parameter) -> Parameter) {
        parametersWhere({ true }, transform)
    }

    fun renameType(oldName: String, newName: String) {
        element = element.renameType(oldName, newName)
    }

    fun renameField(oldName: Name, newName: Name) {
        element = element.renameField(oldName, newName)
    }

    fun renameField(oldName: String, newName: String) {
        element = element.renameField(oldName, newName)
    }

    fun typeByName(name: String, transform: (Type.Custom) -> Type) {
        element = element.transformTypeByName(name, transform)
    }

    inline fun <reified T> injectBefore(
        crossinline produce: (T) -> List<Element>,
    )
        where T : Element, T : HasElements {
        element = element.injectBefore(produce)
    }

    inline fun <reified T> injectAfter(
        crossinline produce: (T) -> List<Element>,
    )
        where T : Element, T : HasElements {
        element = element.injectAfter(produce)
    }

    fun apply(transformer: Transformer) {
        element = element.transform(transformer)
    }

    fun type(transform: (Type, Transformer) -> Type) {
        element = element.transform(transformer { type(transform) })
    }

    fun statement(transform: (Statement, Transformer) -> Statement) {
        element = element.transform(transformer { statement(transform) })
    }

    fun expression(transform: (Expression, Transformer) -> Expression) {
        element = element.transform(transformer { expression(transform) })
    }

    fun field(transform: (Field, Transformer) -> Field) {
        element = element.transform(transformer { field(transform) })
    }

    fun parameter(transform: (Parameter, Transformer) -> Parameter) {
        element = element.transform(transformer { parameter(transform) })
    }

    @JsName("constructorNode")
    fun constructor(transform: (Constructor, Transformer) -> Constructor) {
        element = element.transform(transformer { constructor(transform) })
    }

    fun case(transform: (Case, Transformer) -> Case) {
        element = element.transform(transformer { case(transform) })
    }

    fun statementAndExpression(block: (Statement, Transformer) -> Statement) {
        apply(transformer { statementAndExpression(block) })
    }
}

@Suppress("UNCHECKED_CAST")
inline fun <E : Element> E.transform(block: TransformScope<E>.() -> Unit): E {
    val scope = TransformScope<E>(this)
    scope.block()
    return scope.element as E
}

@PublishedApi
internal inline fun <reified M : Type, E : Element> E.transformMatching(
    crossinline transform: (M) -> Type,
): E = transform(
    transformer {
        type { type, transformer ->
            val transformed = if (type is M) transform(type) else type
            transformed.transformChildren(transformer)
        }
    },
)

@PublishedApi
internal inline fun <reified M : Element, E : Element> E.transformMatchingElements(
    crossinline transform: (M) -> Element,
): E = transform(
    transformer {
        element { element, transformer ->
            val transformed = if (element is M) transform(element) else element
            transformed.transformChildren(transformer)
        }
    },
)

internal fun <T : Element> T.transformFieldsWhere(
    predicate: (Field) -> Boolean,
    transform: (Field) -> Field,
): T = transform(
    transformer {
        field { field, transformer ->
            val transformed = if (predicate(field)) transform(field) else field
            transformed.transformChildren(transformer)
        }
    },
)

internal fun <T : Element> T.transformTypeByName(
    name: String,
    transform: (Type.Custom) -> Type,
): T = transformMatching { type: Type.Custom ->
    if (type.name == name) transform(type) else type
}

internal fun <T : Element> T.renameType(oldName: String, newName: String): T = transformTypeByName(oldName) { it.copy(name = newName) }

internal fun <T : Element> T.renameField(oldName: Name, newName: Name): T = transformFieldsWhere({ it.name == oldName }) { it.copy(name = newName) }

internal fun <T : Element> T.renameField(oldName: String, newName: String): T = renameField(Name.of(oldName), Name.of(newName))

internal fun <T : Element> T.transformParametersWhere(
    predicate: (Parameter) -> Boolean,
    transform: (Parameter) -> Parameter,
): T = transform(
    transformer {
        parameter { parameter, transformer ->
            val transformed = if (predicate(parameter)) transform(parameter) else parameter
            transformed.transformChildren(transformer)
        }
    },
)

@Suppress("UNCHECKED_CAST")
@PublishedApi
internal fun <T : Element> T.withElements(elements: List<Element>): T = (
    when (this) {
        is File -> copy(elements = elements)
        is Struct -> copy(elements = elements)
        is Namespace -> copy(elements = elements)
        is Interface -> copy(elements = elements)
        is Enum -> copy(elements = elements)
        is Main -> this
        else -> this
    }
    ) as T

@PublishedApi
internal inline fun <reified T, E : Element> E.injectBefore(
    crossinline produce: (T) -> List<Element>,
): E where T : Element, T : HasElements = transformMatchingElements<T, E> { element ->
    val injected = produce(element)
    if (injected.isNotEmpty()) element.withElements(injected + element.elements) else element
}

@PublishedApi
internal inline fun <reified T, E : Element> E.injectAfter(
    crossinline produce: (T) -> List<Element>,
): E where T : Element, T : HasElements = transformMatchingElements<T, E> { element ->
    val injected = produce(element)
    if (injected.isNotEmpty()) element.withElements(element.elements + injected) else element
}

internal fun Element.forEachType(action: (Type) -> Unit) {
    transform(
        transformer {
            type { type, tr ->
                action(type)
                type.transformChildren(tr)
            }
        },
    )
}

@PublishedApi
internal fun Element.forEachElement(action: (Element) -> Unit) {
    transform(
        transformer {
            element { element, tr ->
                action(element)
                element.transformChildren(tr)
            }
        },
    )
}

internal fun Element.forEachField(action: (Field) -> Unit) {
    transform(
        transformer {
            field { field, tr ->
                action(field)
                field.transformChildren(tr)
            }
        },
    )
}

internal fun Element.collectTypes(): List<Type> = buildList {
    forEachType { add(it) }
}

fun Element.collectCustomTypeNames(): Set<String> = buildSet {
    forEachType { type ->
        if (type is Type.Custom) add(type.name)
    }
}

inline fun <reified T : Element> HasElements.findElement(): T? = elements.filterIsInstance<T>().firstOrNull()

inline fun <reified T : Element> HasElements.findElement(predicate: (T) -> Boolean): T? = elements.filterIsInstance<T>().firstOrNull(predicate)

inline fun <reified T : Element> Element.findAll(): List<T> = buildList {
    forEachElement { element ->
        if (element is T) add(element)
    }
}

internal inline fun <reified T : Type> Element.findAllTypes(): List<T> = buildList {
    forEachType { type ->
        if (type is T) add(type)
    }
}
