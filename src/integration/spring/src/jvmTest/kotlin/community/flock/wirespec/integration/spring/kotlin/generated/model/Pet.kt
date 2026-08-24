package community.flock.wirespec.integration.spring.kotlin.generated.model
import community.flock.wirespec.kotlin.Wirespec
import kotlin.reflect.typeOf
data class Pet(
  val id: Long?,
  val name: String,
  val category: Category?,
  val photoUrls: List<String>,
  val tags: List<Tag>?,
  val status: PetStatus?
) : Wirespec.Shape {
  override fun validate(): List<String> =
    (category?.let { it.validate().map { e -> "category.${e}" } } ?: emptyList<String>()) + (tags?.let { it.flatMapIndexed { i, el -> el.validate().map { e -> "tags[${i}].${e}" } } } ?: emptyList<String>())
}
