package community.flock.wirespec.integration.spring.kotlin.generated

import community.flock.wirespec.kotlin.Wirespec
import kotlin.reflect.typeOf

data class Pet(
  val id: Long?,
  val name: String,
  val category: Category?,
  val photoUrls: List<String>,
  val tags: List<Tag>?,
  val status: PetStatus?
)
