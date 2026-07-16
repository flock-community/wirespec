package community.flock.wirespec.integration.kotest.extension

import community.flock.wirespec.integration.kotest.extension.EndpointShape.BodyFieldShape

/**
 * Renders the single reusable per-record builder class `<Type>Builder` (emitted once in the
 * type's own `<Type>Dsl.kt`) and the `registerPath` lines that apply it. The type,
 * endpoint-body and channel-payload DSLs reference it by name instead of emitting their own.
 */
internal object RecordBuilder {

    fun builderName(typeName: String): String = "${typeName}Builder"

    /** Nested fields reference the nested type's own `<Nested>Builder`; this renderer never emits it. */
    fun renderBuilderClass(typeName: String, fields: List<BodyFieldShape>): String = buildString {
        appendLine("@WirespecScenarioDsl")
        appendLine("public class ${builderName(typeName)} {")
        fields.forEach { f ->
            val fieldRef = KotlinIdentifier.escape(f.name)
            val blockRef = KotlinIdentifier.escape("_${f.name}Block")
            val blockFn = KotlinIdentifier.escape("${f.name}Block")
            when (f) {
                is BodyFieldShape.Primitive ->
                    appendLine("    public var $fieldRef: Gen<${f.kotlinType}>? = null")
                is BodyFieldShape.NestedObject -> {
                    val nested = builderName(f.typeName)
                    appendLine("    public var $fieldRef: Gen<${f.typeName}>? = null")
                    appendLine("    @PublishedApi internal var $blockRef: ($nested.() -> Unit)? = null")
                    appendLine("    public fun $blockFn(block: $nested.() -> Unit) { $blockRef = block }")
                }
                is BodyFieldShape.NestedList -> {
                    val nested = builderName(f.elementTypeName)
                    appendLine("    public var $fieldRef: Gen<List<${f.elementTypeName}>>? = null")
                    appendLine("    @PublishedApi internal var $blockRef: ($nested.() -> Unit)? = null")
                    appendLine("    public fun $blockFn(block: $nested.() -> Unit) { $blockRef = block }")
                }
            }
        }
        append("}")
    }

    /**
     * `registerPath(...)` lines applying a builder's override `Gen`s onto a
     * `KotestWirespecGeneratorBuilder` receiver, drilling into nested `<field>Block` sub-builders.
     * [path] is the wire-name segments so far (a nested list adds a `"*"` wildcard segment).
     */
    fun renderRegistration(
        fields: List<BodyFieldShape>,
        receiver: String,
        path: List<String>,
        indent: String,
    ): String = buildString {
        fields.forEach { f ->
            val fieldRef = "$receiver.${KotlinIdentifier.escape(f.name)}"
            val blockRef = "$receiver.${KotlinIdentifier.escape("_${f.name}Block")}"
            val segs = (path + f.name).joinToString(", ") { "\"$it\"" }
            when (f) {
                is BodyFieldShape.Primitive ->
                    appendLine("$indent$fieldRef?.let { registerPath($segs) { it } }")
                is BodyFieldShape.NestedObject -> {
                    val nestedVar = KotlinIdentifier.escape("nested_${f.name}")
                    appendLine("$indent$fieldRef?.let { registerPath($segs) { it } }")
                    appendLine("$indent$blockRef?.let { block ->")
                    appendLine("$indent    val $nestedVar = ${builderName(f.typeName)}().apply(block)")
                    append(renderRegistration(f.fields, nestedVar, path + f.name, "$indent    "))
                    appendLine("$indent}")
                }
                is BodyFieldShape.NestedList -> {
                    val nestedVar = KotlinIdentifier.escape("nested_${f.name}")
                    appendLine("$indent$fieldRef?.let { registerPath($segs) { it } }")
                    appendLine("$indent$blockRef?.let { block ->")
                    appendLine("$indent    val $nestedVar = ${builderName(f.elementTypeName)}().apply(block)")
                    append(renderRegistration(f.fields, nestedVar, path + f.name + "*", "$indent    "))
                    appendLine("$indent}")
                }
            }
        }
    }
}
