package community.flock.wirespec.ir.transformer

import community.flock.wirespec.ir.core.Element
import community.flock.wirespec.ir.core.Field
import community.flock.wirespec.ir.core.Name
import community.flock.wirespec.ir.core.Namespace
import community.flock.wirespec.ir.core.Package
import community.flock.wirespec.ir.core.RawElement
import community.flock.wirespec.ir.core.Struct
import community.flock.wirespec.ir.core.Type
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NestedStructTransformTest {

    // qualifyNestedRefs

    @Test
    fun qualifyNestedRefsRenamesMatchingCustomType() {
        val struct = Struct(
            name = Name.of("Person"),
            fields = listOf(
                Field(Name.of("address"), Type.Custom("Address")),
            ),
        )

        val result = struct.qualifyNestedRefs(setOf("Address"))

        assertEquals(Type.Custom("PersonAddress"), result.fields[0].type)
    }

    @Test
    fun qualifyNestedRefsLeavesNonMatchingCustomTypeUnchanged() {
        val struct = Struct(
            name = Name.of("Person"),
            fields = listOf(
                Field(Name.of("address"), Type.Custom("Address")),
                Field(Name.of("company"), Type.Custom("Company")),
            ),
        )

        val result = struct.qualifyNestedRefs(setOf("Address"))

        assertEquals(Type.Custom("PersonAddress"), result.fields[0].type)
        assertEquals(Type.Custom("Company"), result.fields[1].type)
    }

    @Test
    fun qualifyNestedRefsWithEmptySetLeavesTypesUnchanged() {
        val struct = Struct(
            name = Name.of("Person"),
            fields = listOf(
                Field(Name.of("address"), Type.Custom("Address")),
            ),
        )

        val result = struct.qualifyNestedRefs(emptySet())

        assertEquals(Type.Custom("Address"), result.fields[0].type)
    }

    @Test
    fun qualifyNestedRefsRenamesInsideArray() {
        val struct = Struct(
            name = Name.of("Person"),
            fields = listOf(
                Field(Name.of("addresses"), Type.Array(Type.Custom("Address"))),
            ),
        )

        val result = struct.qualifyNestedRefs(setOf("Address"))

        assertEquals(Type.Array(Type.Custom("PersonAddress")), result.fields[0].type)
    }

    @Test
    fun qualifyNestedRefsRenamesInsideNullable() {
        val struct = Struct(
            name = Name.of("Person"),
            fields = listOf(
                Field(Name.of("address"), Type.Nullable(Type.Custom("Address"))),
            ),
        )

        val result = struct.qualifyNestedRefs(setOf("Address"))

        assertEquals(Type.Nullable(Type.Custom("PersonAddress")), result.fields[0].type)
    }

    @Test
    fun qualifyNestedRefsRenamesInsideDictValueAndKey() {
        val struct = Struct(
            name = Name.of("Registry"),
            fields = listOf(
                Field(Name.of("byKey"), Type.Dict(Type.Custom("Key"), Type.Custom("Item"))),
            ),
        )

        val result = struct.qualifyNestedRefs(setOf("Key", "Item"))

        assertEquals(
            Type.Dict(Type.Custom("RegistryKey"), Type.Custom("RegistryItem")),
            result.fields[0].type,
        )
    }

    @Test
    fun qualifyNestedRefsRenamesInsideGenericTypeArguments() {
        val struct = Struct(
            name = Name.of("Container"),
            fields = listOf(
                Field(Name.of("box"), Type.Custom("Box", listOf(Type.Custom("Item")))),
            ),
        )

        val result = struct.qualifyNestedRefs(setOf("Item"))

        assertEquals(
            Type.Custom("Box", listOf(Type.Custom("ContainerItem"))),
            result.fields[0].type,
        )
    }

    @Test
    fun qualifyNestedRefsStripsNestedStructElements() {
        val struct = Struct(
            name = Name.of("Person"),
            fields = listOf(Field(Name.of("name"), Type.String)),
            elements = listOf(
                Struct(Name.of("Address"), listOf(Field(Name.of("street"), Type.String))),
                Struct(Name.of("Phone"), listOf(Field(Name.of("number"), Type.String))),
            ),
        )

        val result = struct.qualifyNestedRefs(setOf("Address", "Phone"))

        assertTrue(result.elements.none { it is Struct })
    }

    @Test
    fun qualifyNestedRefsPreservesNonStructElements() {
        val raw = RawElement("// keep me")
        val struct = Struct(
            name = Name.of("Person"),
            fields = listOf(Field(Name.of("name"), Type.String)),
            elements = listOf(
                Struct(Name.of("Address"), listOf(Field(Name.of("street"), Type.String))),
                raw,
            ),
        )

        val result = struct.qualifyNestedRefs(setOf("Address"))

        assertEquals(listOf<Element>(raw), result.elements)
    }

    @Test
    fun qualifyNestedRefsKeepsParentNameUnchanged() {
        val struct = Struct(
            name = Name.of("Person"),
            fields = listOf(Field(Name.of("address"), Type.Custom("Address"))),
        )

        val result = struct.qualifyNestedRefs(setOf("Address"))

        assertEquals(Name.of("Person"), result.name)
    }

    @Test
    fun qualifyNestedRefsPrefixUsesPascalCaseOfParentName() {
        val struct = Struct(
            name = Name.of("user_profile"),
            fields = listOf(Field(Name.of("address"), Type.Custom("Address"))),
        )

        val result = struct.qualifyNestedRefs(setOf("Address"))

        // "user_profile" -> "UserProfile" -> "UserProfileAddress"
        assertEquals(Type.Custom("UserProfileAddress"), result.fields[0].type)
    }

    // flattenNestedStructs

    @Test
    fun flattenNestedStructsLeavesEmptyNamespaceUnchanged() {
        val namespace = Namespace(name = Name.of("api"), elements = emptyList())

        val result = namespace.flattenNestedStructs()

        assertEquals(emptyList(), result.elements)
    }

    @Test
    fun flattenNestedStructsPassesThroughNonStructElements() {
        val raw = RawElement("// raw")
        val pkg = Package("com.example")
        val namespace = Namespace(
            name = Name.of("api"),
            elements = listOf(raw, pkg),
        )

        val result = namespace.flattenNestedStructs()

        assertEquals(listOf<Element>(raw, pkg), result.elements)
    }

    @Test
    fun flattenNestedStructsPassesThroughStructWithoutNested() {
        val struct = Struct(
            name = Name.of("Person"),
            fields = listOf(Field(Name.of("name"), Type.String)),
        )
        val namespace = Namespace(
            name = Name.of("api"),
            elements = listOf(struct),
        )

        val result = namespace.flattenNestedStructs()

        assertEquals(listOf<Element>(struct), result.elements)
    }

    @Test
    fun flattenNestedStructsLiftsSingleNestedStructAndQualifiesParentReference() {
        val nested = Struct(
            name = Name.of("Address"),
            fields = listOf(Field(Name.of("street"), Type.String)),
        )
        val parent = Struct(
            name = Name.of("Person"),
            fields = listOf(
                Field(Name.of("name"), Type.String),
                Field(Name.of("address"), Type.Custom("Address")),
            ),
            elements = listOf(nested),
        )
        val namespace = Namespace(
            name = Name.of("api"),
            elements = listOf(parent),
        )

        val result = namespace.flattenNestedStructs()

        // Order: flattened nested structs come first, then the (de-nested) parent.
        assertEquals(2, result.elements.size)

        val flattened = result.elements[0] as Struct
        assertEquals(Name.of("PersonAddress"), flattened.name)
        // The lifted struct's own fields are preserved as-is.
        assertEquals(Type.String, flattened.fields[0].type)

        val flatParent = result.elements[1] as Struct
        assertEquals(Name.of("Person"), flatParent.name)
        // The parent's reference to the nested type was prefixed.
        assertEquals(Type.Custom("PersonAddress"), flatParent.fields[1].type)
        // The parent no longer carries the nested struct as a child element.
        assertTrue(flatParent.elements.none { it is Struct })
    }

    @Test
    fun flattenNestedStructsLiftsMultipleNestedStructs() {
        val parent = Struct(
            name = Name.of("Person"),
            fields = listOf(
                Field(Name.of("address"), Type.Custom("Address")),
                Field(Name.of("phone"), Type.Custom("Phone")),
            ),
            elements = listOf(
                Struct(Name.of("Address"), listOf(Field(Name.of("street"), Type.String))),
                Struct(Name.of("Phone"), listOf(Field(Name.of("number"), Type.String))),
            ),
        )
        val namespace = Namespace(
            name = Name.of("api"),
            elements = listOf(parent),
        )

        val result = namespace.flattenNestedStructs()

        assertEquals(3, result.elements.size)
        val structs = result.elements.filterIsInstance<Struct>()
        val names = structs.map { it.name }
        assertEquals(
            listOf(Name.of("PersonAddress"), Name.of("PersonPhone"), Name.of("Person")),
            names,
        )

        val flatParent = structs.last()
        assertEquals(Type.Custom("PersonAddress"), flatParent.fields[0].type)
        assertEquals(Type.Custom("PersonPhone"), flatParent.fields[1].type)
    }

    @Test
    fun flattenNestedStructsLeavesUnrelatedReferencesUnchanged() {
        val parent = Struct(
            name = Name.of("Person"),
            fields = listOf(
                Field(Name.of("address"), Type.Custom("Address")),
                Field(Name.of("company"), Type.Custom("Company")),
            ),
            elements = listOf(
                Struct(Name.of("Address"), listOf(Field(Name.of("street"), Type.String))),
            ),
        )
        val namespace = Namespace(
            name = Name.of("api"),
            elements = listOf(parent),
        )

        val result = namespace.flattenNestedStructs()

        val flatParent = result.elements.filterIsInstance<Struct>().first { it.name == Name.of("Person") }
        assertEquals(Type.Custom("PersonAddress"), flatParent.fields[0].type)
        assertEquals(Type.Custom("Company"), flatParent.fields[1].type)
    }

    @Test
    fun flattenNestedStructsQualifiesNestedReferencesInsideArrayAndNullable() {
        val parent = Struct(
            name = Name.of("Person"),
            fields = listOf(
                Field(Name.of("addresses"), Type.Array(Type.Custom("Address"))),
                Field(Name.of("phone"), Type.Nullable(Type.Custom("Phone"))),
            ),
            elements = listOf(
                Struct(Name.of("Address"), listOf(Field(Name.of("street"), Type.String))),
                Struct(Name.of("Phone"), listOf(Field(Name.of("number"), Type.String))),
            ),
        )
        val namespace = Namespace(
            name = Name.of("api"),
            elements = listOf(parent),
        )

        val result = namespace.flattenNestedStructs()

        val flatParent = result.elements.filterIsInstance<Struct>().first { it.name == Name.of("Person") }
        assertEquals(Type.Array(Type.Custom("PersonAddress")), flatParent.fields[0].type)
        assertEquals(Type.Nullable(Type.Custom("PersonPhone")), flatParent.fields[1].type)
    }

    @Test
    fun flattenNestedStructsPreservesOrderAcrossMixedElements() {
        val raw = RawElement("// keep")
        val standalone = Struct(
            name = Name.of("Status"),
            fields = listOf(Field(Name.of("code"), Type.Integer())),
        )
        val parent = Struct(
            name = Name.of("Person"),
            fields = listOf(Field(Name.of("address"), Type.Custom("Address"))),
            elements = listOf(
                Struct(Name.of("Address"), listOf(Field(Name.of("street"), Type.String))),
            ),
        )
        val namespace = Namespace(
            name = Name.of("api"),
            elements = listOf(raw, standalone, parent),
        )

        val result = namespace.flattenNestedStructs()

        // raw and standalone untouched and in original positions; parent expands into [PersonAddress, Person].
        assertEquals(4, result.elements.size)
        assertEquals(raw, result.elements[0])
        assertEquals(standalone, result.elements[1])
        assertEquals(Name.of("PersonAddress"), (result.elements[2] as Struct).name)
        assertEquals(Name.of("Person"), (result.elements[3] as Struct).name)
    }

    @Test
    fun flattenNestedStructsKeepsNamespaceMetadata() {
        val namespace = Namespace(
            name = Name.of("api"),
            elements = listOf(
                Struct(
                    name = Name.of("Person"),
                    fields = listOf(Field(Name.of("address"), Type.Custom("Address"))),
                    elements = listOf(
                        Struct(Name.of("Address"), listOf(Field(Name.of("street"), Type.String))),
                    ),
                ),
            ),
            extends = Type.Custom("Base"),
        )

        val result = namespace.flattenNestedStructs()

        assertEquals(Name.of("api"), result.name)
        assertEquals(Type.Custom("Base"), result.extends)
    }
}
