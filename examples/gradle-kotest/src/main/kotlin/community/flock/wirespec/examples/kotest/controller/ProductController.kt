package community.flock.wirespec.examples.kotest.controller

import community.flock.wirespec.examples.kotest.generated.endpoint.CreateProduct
import community.flock.wirespec.examples.kotest.generated.endpoint.GetProduct
import community.flock.wirespec.examples.kotest.generated.endpoint.GetProducts
import community.flock.wirespec.examples.kotest.generated.model.Error
import community.flock.wirespec.examples.kotest.service.ProductService
import org.springframework.web.bind.annotation.RestController

/**
 * Implements the Wirespec-generated product `*.Handler` interfaces. The routing annotations
 * (`@GetMapping`, `@PostMapping`) live on those generated interfaces — added by the Spring IR
 * extension — so this controller only supplies behaviour.
 */
@RestController
class ProductController(
    private val service: ProductService,
) : GetProducts.Handler,
    GetProduct.Handler,
    CreateProduct.Handler {

    override suspend fun getProducts(request: GetProducts.Request): GetProducts.Response<*> = GetProducts.Response200(service.list())

    override suspend fun getProduct(request: GetProduct.Request): GetProduct.Response<*> = service.get(request.path.id)
        ?.let { GetProduct.Response200(it) }
        ?: GetProduct.Response404(Error(code = 404, message = "Product not found: ${request.path.id.value}"))

    override suspend fun createProduct(request: CreateProduct.Request): CreateProduct.Response<*> = CreateProduct.Response201(service.create(request.body))
}
