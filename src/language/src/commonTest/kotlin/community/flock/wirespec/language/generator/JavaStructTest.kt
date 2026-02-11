package community.flock.wirespec.language.generator

import community.flock.wirespec.language.core.file
import kotlin.test.Test
import kotlin.test.assertContains

class JavaStructTest {

    @Test
    fun testEmptyStructEmitAsRecord() {
        val file = file("EmptyStruct") {
            `package`("com.example")
            struct("Empty") {}
        }

        val output = JavaGenerator.generate(file)

        // Should be a record
        assertContains(output, "public record Empty")
    }

    @Test
    fun testEmptyStructWithInterfaceEmitAsRecord() {
        val file = file("EmptyRecordWithInterface") {
            `package`("com.example")
            union("MyUnion") {
                member("Empty")
            }
            struct("Empty") {}
        }

        val output = JavaGenerator.generate(file)

        // Should be a record
        assertContains(output, "public record Empty")
        assertContains(output, "implements MyUnion")
    }

    @Test
    fun testEmptyStructExtendingInterfaceEmitAsRecord() {
        val file = file("EmptyStructExtendingInterface") {
            `package`("com.example")
            `interface`("MyInterface") {}
            struct("MyStruct") {
                implements(type("MyInterface"))
            }
        }

        val output = JavaGenerator.generate(file)

        // Should be a record
        assertContains(output, "public record MyStruct")
        assertContains(output, "implements MyInterface")
    }

    @Test
    fun testEmptyStructExtendingClassEmitAsClass() {
        val file = file("EmptyStructExtendingClass") {
            `package`("com.example")
            struct("MyClass") {}
            struct("MyStruct") {
                implements(type("MyClass"))
            }
        }

        val output = JavaGenerator.generate(file)

        // Should be a record implementing the struct (treated as interface)
        assertContains(output, "public record MyStruct")
        assertContains(output, "implements MyClass")
    }

    @Test
    fun testStructWithFieldsEmitAsRecord() {
        val file = file("FieldStruct") {
            `package`("com.example")
            struct("WithFields") {
                field("id", string)
            }
        }

        val output = JavaGenerator.generate(file)

        // Should be a record
        assertContains(output, "public record WithFields")
        assertContains(output, "String id")
    }

    @Test
    fun testStructWithExtendsEmitAsClass() {
        val file = file("ExtendsStruct") {
            `package`("com.example")
            struct("Extending") {
                implements(type("Base"))
                field("id", string)
            }
        }

        val output = JavaGenerator.generate(file)

        // Should be a record implementing the interface (unknown type treated as interface)
        assertContains(output, "public record Extending")
        assertContains(output, "implements Base")
    }

    @Test
    fun testRecordWithInterface() {
        val file = file("RecordWithInterface") {
            `package`("com.example")
            union("MyUnion") {
                member("MyRecord")
            }
            struct("MyRecord") {
                field("id", string)
            }
        }

        val output = JavaGenerator.generate(file)

        // Should be a record implementing the union
        assertContains(output, "public record MyRecord")
        assertContains(output, "implements MyUnion")
        // Check syntax: record MyRecord (...) implements MyUnion {
        assertContains(output, "public record MyRecord (")
        assertContains(output, "String id")
        assertContains(output, ") implements MyUnion {")
    }

    @Test
    fun testStructExtendingInterfaceEmitAsRecord() {
        val file = file("StructExtendingInterface") {
            `package`("com.example")
            `interface`("MyInterface") {}
            struct("MyStruct") {
                implements(type("MyInterface"))
                field("id", string)
            }
        }

        val output = JavaGenerator.generate(file)

        // Should be a record because MyInterface is an interface
        assertContains(output, "public record MyStruct")
        assertContains(output, "implements MyInterface")
        assertContains(output, "public record MyStruct (")
        assertContains(output, "String id")
        assertContains(output, ") implements MyInterface {")
    }

    @Test
    fun testStructExtendingStructEmitAsClass() {
        val file = file("StructExtendingStruct") {
            `package`("com.example")
            struct("Base") {
                field("name", string)
            }
            struct("Derived") {
                implements(type("Base"))
                field("id", string)
            }
        }

        val output = JavaGenerator.generate(file)

        // Should be a record implementing the struct (treated as interface)
        assertContains(output, "public record Derived")
        assertContains(output, "implements Base")
    }
}
