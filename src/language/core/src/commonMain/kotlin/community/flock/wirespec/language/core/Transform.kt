package community.flock.wirespec.language.core

/**
 * Transformer interface for AST mutations.
 * Override specific methods to transform only certain node types.
 */
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

/**
 * Creates an Transformer from lambda functions.
 */
fun transformer(
    transformType: (Type, Transformer) -> Type = { t, transformer -> t.transformChildren(transformer) },
    transformElement: (Element, Transformer) -> Element = { e, transformer -> e.transformChildren(transformer) },
    transformStatement: (Statement, Transformer) -> Statement = { s, transformer -> s.transformChildren(transformer) },
    transformExpression: (Expression, Transformer) -> Expression = { e, transformer -> e.transformChildren(transformer) },
    transformField: (Field, Transformer) -> Field = { f, transformer -> f.transformChildren(transformer) },
    transformParameter: (Parameter, Transformer) -> Parameter = { p, transformer -> p.transformChildren(transformer) },
    transformConstructor: (Constructor, Transformer) -> Constructor = { c, transformer -> c.transformChildren(transformer) },
    transformCase: (Case, Transformer) -> Case = { c, transformer -> c.transformChildren(transformer) },
): Transformer = object : Transformer {
    override fun transformType(type: Type): Type = transformType(type, this)
    override fun transformElement(element: Element): Element = transformElement(element, this)
    override fun transformStatement(statement: Statement): Statement = transformStatement(statement, this)
    override fun transformExpression(expression: Expression): Expression = transformExpression(expression, this)
    override fun transformField(field: Field): Field = transformField(field, this)
    override fun transformParameter(parameter: Parameter): Parameter = transformParameter(parameter, this)
    override fun transformConstructor(constructor: Constructor): Constructor = transformConstructor(constructor, this)
    override fun transformCase(case: Case): Case = transformCase(case, this)
}

// Type transformation

fun Type.transformChildren(transformer: Transformer): Type = when (this) {
    is Type.Array -> copy(elementType = transformer.transformType(elementType))
    is Type.Dict -> copy(
        keyType = transformer.transformType(keyType),
        valueType = transformer.transformType(valueType),
    )
    is Type.Custom -> copy(generics = generics.map { transformer.transformType(it) })
    is Type.Nullable -> copy(type = transformer.transformType(type))
    is Type.Integer, is Type.Number, Type.String, Type.Boolean, Type.Bytes, Type.Unit -> this
}

fun Type.transform(transformer: Transformer): Type = transformer.transformType(this)

// Element transformation

fun Element.transformChildren(transformer: Transformer): Element = when (this) {
    is File -> copy(elements = elements.map { transformer.transformElement(it) })
    is Package -> this
    is Import -> this
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
    is Static -> copy(
        elements = elements.map { transformer.transformElement(it) },
        extends = extends?.let { transformer.transformType(it) as Type.Custom },
    )
    is Interface -> copy(
        elements = elements.map { transformer.transformElement(it) },
        extends = extends?.let { transformer.transformType(it) as Type.Custom },
    )
    is Union -> copy(
        extends = extends?.let { transformer.transformType(it) as Type.Custom },
    )
    is Enum -> copy(
        extends = extends?.let { transformer.transformType(it) as Type.Custom },
        fields = fields.map { transformer.transformField(it) },
        constructors = constructors.map { transformer.transformConstructor(it) },
        elements = elements.map { transformer.transformElement(it) },
    )
    is RawElement -> this
}

fun Element.transform(transformer: Transformer): Element = transformer.transformElement(this)

// Field transformation

fun Field.transformChildren(transformer: Transformer): Field = copy(type = transformer.transformType(type))

fun Field.transform(transformer: Transformer): Field = transformer.transformField(this)

// Parameter transformation

fun Parameter.transformChildren(transformer: Transformer): Parameter = copy(type = transformer.transformType(type))

fun Parameter.transform(transformer: Transformer): Parameter = transformer.transformParameter(this)

// Constructor transformation

fun Constructor.transformChildren(transformer: Transformer): Constructor = copy(
    parameters = parameters.map { transformer.transformParameter(it) },
    body = body.map { transformer.transformStatement(it) },
)

fun Constructor.transform(transformer: Transformer): Constructor = transformer.transformConstructor(this)

// Statement transformation

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
    is Switch -> copy(
        expression = transformer.transformExpression(expression),
        cases = cases.map { transformer.transformCase(it) },
        default = default?.map { transformer.transformStatement(it) },
    )
    is NullLiteral -> this
    is VariableReference -> this
    is PropertyAccess -> copy(receiver = transformer.transformExpression(receiver))
    is MethodCall -> copy(
        receiver = receiver?.let { transformer.transformExpression(it) },
        arguments = arguments.mapValues { transformer.transformExpression(it.value) },
    )
    is EnumReference -> copy(enumType = transformer.transformType(enumType) as Type.Custom)
    is BinaryOp -> copy(
        left = transformer.transformExpression(left),
        right = transformer.transformExpression(right),
    )
    is StaticCall -> copy(arguments = arguments.mapValues { transformer.transformExpression(it.value) })
    is ClassLiteral -> copy(type = transformer.transformType(type))
    is TypeDescriptor -> copy(type = transformer.transformType(type))
    is AnonymousClass -> copy(
        baseType = transformer.transformType(baseType) as Type.Custom,
        typeArguments = typeArguments.map { transformer.transformType(it) },
        methods = methods.map { transformer.transformElement(it) as Function },
    )
}

fun Statement.transform(transformer: Transformer): Statement = transformer.transformStatement(this)

// Expression transformation

fun Expression.transformChildren(transformer: Transformer): Expression = when (this) {
    is RawExpression -> this
    is Statement -> transformChildren(transformer) as Expression
}

fun Expression.transform(transformer: Transformer): Expression = transformer.transformExpression(this)

// Case transformation

fun Case.transformChildren(transformer: Transformer): Case = copy(
    value = transformer.transformExpression(value),
    body = body.map { transformer.transformStatement(it) },
    type = type?.let { transformer.transformType(it) },
)

fun Case.transform(transformer: Transformer): Case = transformer.transformCase(this)

// Match and Transform DSL

/**
 * Match and transform types using pattern matching.
 */
inline fun <reified T : Type> Element.transformMatching(
    crossinline transform: (T) -> Type,
): Element = transform(
    transformer(
        transformType = { type, transformer ->
            val transformed = if (type is T) transform(type) else type
            transformed.transformChildren(transformer)
        },
    ),
)

/**
 * Match and transform elements using pattern matching.
 */
inline fun <reified T : Element> Element.transformMatchingElements(
    crossinline transform: (T) -> Element,
): Element = transform(
    transformer(
        transformElement = { element, transformer ->
            val transformed = if (element is T) transform(element) else element
            transformed.transformChildren(transformer)
        },
    ),
)

/**
 * Match and transform fields using a predicate.
 */
fun Element.transformFieldsWhere(
    predicate: (Field) -> Boolean,
    transform: (Field) -> Field,
): Element = transform(
    transformer(
        transformField = { field, transformer ->
            val transformed = if (predicate(field)) transform(field) else field
            transformed.transformChildren(transformer)
        },
    ),
)

/**
 * Match and transform types by name.
 */
fun Element.transformTypeByName(
    name: String,
    transform: (Type.Custom) -> Type,
): Element = transformMatching<Type.Custom> { type ->
    if (type.name == name) transform(type) else type
}

/**
 * Rename a custom type throughout the AST.
 */
fun Element.renameType(oldName: String, newName: String): Element = transformTypeByName(oldName) { it.copy(name = newName) }

/**
 * Rename a field throughout the AST.
 */
fun Element.renameField(oldName: String, newName: String): Element = transformFieldsWhere({ it.name == oldName }) { it.copy(name = newName) }

/**
 * Match and transform parameters using a predicate.
 */
fun Element.transformParametersWhere(
    predicate: (Parameter) -> Boolean,
    transform: (Parameter) -> Parameter,
): Element = transform(
    transformer(
        transformParameter = { parameter, transformer ->
            val transformed = if (predicate(parameter)) transform(parameter) else parameter
            transformed.transformChildren(transformer)
        },
    ),
)

// Visitor interface for traversing AST without mutation

/**
 * Visitor interface for traversing AST nodes.
 */
interface Visitor {
    fun visitType(type: Type) {
        type.visitChildren(this)
    }
    fun visitElement(element: Element) {
        element.visitChildren(this)
    }
    fun visitStatement(statement: Statement) {
        statement.visitChildren(this)
    }
    fun visitExpression(expression: Expression) {
        expression.visitChildren(this)
    }
    fun visitField(field: Field) {
        field.visitChildren(this)
    }
    fun visitParameter(parameter: Parameter) {
        parameter.visitChildren(this)
    }
    fun visitConstructor(constructor: Constructor) {
        constructor.visitChildren(this)
    }
    fun visitCase(case: Case) {
        case.visitChildren(this)
    }
}

// Visitor child traversal functions

fun Type.visitChildren(visitor: Visitor) {
    when (this) {
        is Type.Array -> visitor.visitType(elementType)
        is Type.Dict -> {
            visitor.visitType(keyType)
            visitor.visitType(valueType)
        }
        is Type.Custom -> generics.forEach { visitor.visitType(it) }
        is Type.Nullable -> visitor.visitType(type)
        is Type.Integer, is Type.Number, Type.String, Type.Boolean, Type.Bytes, Type.Unit -> {}
    }
}

fun Element.visitChildren(visitor: Visitor) {
    when (this) {
        is File -> elements.forEach { visitor.visitElement(it) }
        is Package -> {}
        is Import -> {}
        is Struct -> {
            fields.forEach { visitor.visitField(it) }
            constructors.forEach { visitor.visitConstructor(it) }
            interfaces.forEach { visitor.visitType(it) }
            elements.forEach { visitor.visitElement(it) }
        }
        is Function -> {
            parameters.forEach { visitor.visitParameter(it) }
            returnType?.let { visitor.visitType(it) }
            body.forEach { visitor.visitStatement(it) }
        }
        is Static -> {
            elements.forEach { visitor.visitElement(it) }
            extends?.let { visitor.visitType(it) }
        }
        is Interface -> {
            elements.forEach { visitor.visitElement(it) }
            extends?.let { visitor.visitType(it) }
        }
        is Union -> {
            extends?.let { visitor.visitType(it) }
        }
        is Enum -> {
            extends?.let { visitor.visitType(it) }
            fields.forEach { visitor.visitField(it) }
            constructors.forEach { visitor.visitConstructor(it) }
            elements.forEach { visitor.visitElement(it) }
        }
        is RawElement -> {}
    }
}

fun Field.visitChildren(visitor: Visitor) {
    visitor.visitType(type)
}

fun Parameter.visitChildren(visitor: Visitor) {
    visitor.visitType(type)
}

fun Constructor.visitChildren(visitor: Visitor) {
    parameters.forEach { visitor.visitParameter(it) }
    body.forEach { visitor.visitStatement(it) }
}

fun Statement.visitChildren(visitor: Visitor) {
    when (this) {
        is PrintStatement -> visitor.visitExpression(expression)
        is ReturnStatement -> visitor.visitExpression(expression)
        is ConstructorStatement -> {
            visitor.visitType(type)
            namedArguments.values.forEach { visitor.visitExpression(it) }
        }
        is Literal -> visitor.visitType(type)
        is LiteralList -> {
            values.forEach { visitor.visitExpression(it) }
            visitor.visitType(type)
        }
        is LiteralMap -> {
            values.values.forEach { visitor.visitExpression(it) }
            visitor.visitType(keyType)
            visitor.visitType(valueType)
        }
        is Assignment -> visitor.visitExpression(value)
        is ErrorStatement -> visitor.visitExpression(message)
        is Switch -> {
            visitor.visitExpression(expression)
            cases.forEach { visitor.visitCase(it) }
            default?.forEach { visitor.visitStatement(it) }
        }
        is NullLiteral -> {}
        is VariableReference -> {}
        is PropertyAccess -> visitor.visitExpression(receiver)
        is MethodCall -> {
            receiver?.let { visitor.visitExpression(it) }
            arguments.values.forEach { visitor.visitExpression(it) }
        }
        is EnumReference -> visitor.visitType(enumType)
        is BinaryOp -> {
            visitor.visitExpression(left)
            visitor.visitExpression(right)
        }
        is StaticCall -> arguments.values.forEach { visitor.visitExpression(it) }
        is ClassLiteral -> visitor.visitType(type)
        is TypeDescriptor -> visitor.visitType(type)
        is AnonymousClass -> {
            visitor.visitType(baseType)
            typeArguments.forEach { visitor.visitType(it) }
            methods.forEach { visitor.visitElement(it) }
        }
    }
}

fun Expression.visitChildren(visitor: Visitor) {
    when (this) {
        is RawExpression -> {}
        is Statement -> visitChildren(visitor)
    }
}

fun Case.visitChildren(visitor: Visitor) {
    visitor.visitExpression(value)
    body.forEach { visitor.visitStatement(it) }
    type?.let { visitor.visitType(it) }
}

// Convenience extension functions for traversal

fun Element.visit(visitor: Visitor) = visitor.visitElement(this)

/**
 * Traverse all types in the AST.
 */
fun Element.forEachType(action: (Type) -> Unit) {
    visit(object : Visitor {
        override fun visitType(type: Type) {
            action(type)
            type.visitChildren(this)
        }
    })
}

/**
 * Traverse all elements in the AST.
 */
fun Element.forEachElement(action: (Element) -> Unit) {
    visit(object : Visitor {
        override fun visitElement(element: Element) {
            action(element)
            element.visitChildren(this)
        }
    })
}

/**
 * Traverse all fields in the AST.
 */
fun Element.forEachField(action: (Field) -> Unit) {
    visit(object : Visitor {
        override fun visitField(field: Field) {
            action(field)
            field.visitChildren(this)
        }
    })
}

/**
 * Collect all types in the AST.
 */
fun Element.collectTypes(): List<Type> = buildList {
    forEachType { add(it) }
}

/**
 * Collect all elements in the AST.
 */
fun Element.collectElements(): List<Element> = buildList {
    forEachElement { add(it) }
}

/**
 * Collect all custom type names used in the AST.
 */
fun Element.collectCustomTypeNames(): Set<String> = buildSet {
    forEachType { type ->
        if (type is Type.Custom) add(type.name)
    }
}

/**
 * Find all elements matching a predicate.
 */
inline fun <reified T : Element> Element.findAll(): List<T> = buildList {
    forEachElement { element ->
        if (element is T) add(element)
    }
}

/**
 * Find all types matching a predicate.
 */
inline fun <reified T : Type> Element.findAllTypes(): List<T> = buildList {
    forEachType { type ->
        if (type is T) add(type)
    }
}
