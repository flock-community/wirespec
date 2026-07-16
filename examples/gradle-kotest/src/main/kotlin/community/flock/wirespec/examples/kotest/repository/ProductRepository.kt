package community.flock.wirespec.examples.kotest.repository

import community.flock.wirespec.examples.kotest.generated.model.Product
import community.flock.wirespec.examples.kotest.generated.model.ProductId
import org.springframework.stereotype.Repository
import java.util.concurrent.ConcurrentHashMap

/** In-memory store for [Product]s, keyed by their refined [ProductId]. */
@Repository
class ProductRepository {
    private val store = ConcurrentHashMap<String, Product>()

    suspend fun save(product: Product): Product = product.also { store[it.id.value] = it }

    suspend fun findById(id: ProductId): Product? = store[id.value]

    suspend fun findAll(): List<Product> = store.values.toList()
}
