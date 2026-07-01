package community.flock.wirespec.examples.kotest.repository

import community.flock.wirespec.examples.kotest.generated.model.Product
import community.flock.wirespec.examples.kotest.generated.model.ProductId
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.springframework.stereotype.Repository

@Repository
class ProductRepository {

    private val mutex = Mutex()
    private val store = mutableMapOf<ProductId, Product>()

    suspend fun findAll(): List<Product> = mutex.withLock { store.values.toList() }

    suspend fun findById(id: ProductId): Product? = mutex.withLock { store[id] }

    suspend fun save(product: Product): Product = mutex.withLock {
        store[product.id] = product
        product
    }
}
