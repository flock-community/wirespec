package community.flock.wirespec.examples.kotest.service

import community.flock.wirespec.examples.kotest.generated.model.Product
import community.flock.wirespec.examples.kotest.generated.model.ProductId
import community.flock.wirespec.examples.kotest.generated.model.ProductInput
import community.flock.wirespec.examples.kotest.repository.ProductRepository
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class ProductService(private val repository: ProductRepository) {

    suspend fun list(): List<Product> = repository.findAll()

    suspend fun get(id: ProductId): Product? = repository.findById(id)

    suspend fun create(input: ProductInput): Product = repository.save(
        Product(
            id = ProductId(UUID.randomUUID().toString()),
            sku = input.sku,
            name = input.name,
            price = input.price,
        ),
    )
}
