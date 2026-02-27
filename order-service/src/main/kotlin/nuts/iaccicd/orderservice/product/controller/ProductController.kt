package nuts.iaccicd.orderservice.product.controller

import jakarta.validation.Valid
import nuts.iaccicd.orderservice.product.model.ProductCreateRequest
import nuts.iaccicd.orderservice.product.model.ProductListResponse
import nuts.iaccicd.orderservice.product.model.ProductResponse
import nuts.iaccicd.orderservice.product.service.ProductService
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController


@RestController
@RequestMapping("/products")
class ProductController(
    private val productService: ProductService
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    fun create(@Valid @RequestBody request: ProductCreateRequest): ProductResponse {
        return productService.create(request)
    }

    @GetMapping("/{productId}")
    fun get(@PathVariable productId: Long): ProductResponse {
        return productService.get(productId)
    }

    @GetMapping
    fun list(): ProductListResponse {
        return ProductListResponse(items = productService.list())
    }
}
