package community.flock.wirespec.integration.jvm.extension

import community.flock.wirespec.integration.jvm.scala.generated.model.{Archive, Empty, Entry, Owner, Todo, TodoCategory, TodoId}

// Exercises the generated Scala builders; BuilderExtensionTest calls these at runtime.
object ScalaBuilderCheck {
  def buildsCaseClass(): Unit = {
    val todo = Todo.builder()
      .withId(TodoId("1"))
      .withName("Groceries")
      .withFinal(false)
      .withOwner(Owner("Ann"))
      .withTags(List("home"))
      .withHouseNumber(42L)
      .withDescription("Milk")
      .build()

    check(todo == Todo(TodoId("1"), "Groceries", false, Owner("Ann"), List("home"), 42L, Some("Milk")), s"unexpected $todo")
    check(todo.toBuilder().withName("Laundry").build() == todo.copy(name = "Laundry"), "toBuilder lost a field")
  }

  def buildsWithEnumAndEmptyType(): Unit = {
    val archive = Archive.builder().withCategory(TodoCategory.LIFE).withEmpty(Empty()).build()
    check(archive == Archive(TodoCategory.LIFE, Empty()), s"unexpected $archive")
  }

  // Todo and Archive are members of the union Entry, so they and their builder companions share its file.
  def buildsUnionMembers(): Unit = {
    val entries: List[Entry] = List(
      Archive.builder().withCategory(TodoCategory.WORK).withEmpty(Empty()).build(),
      Todo.builder().withId(TodoId("2")).withName("Laundry").withFinal(true).withOwner(Owner("Bo")).withTags(List.empty).withHouseNumber(7L).build(),
    )
    check(entries.collect { case todo: Todo => todo.name } == List("Laundry"), s"unexpected $entries")
  }

  def missingRequiredFieldMessage(): String =
    try {
      Todo.builder().withId(TodoId("1")).build()
      "no exception"
    } catch {
      case e: IllegalStateException => e.getMessage
    }

  private def check(condition: Boolean, message: => String): Unit =
    if (!condition) throw new AssertionError(message)
}
