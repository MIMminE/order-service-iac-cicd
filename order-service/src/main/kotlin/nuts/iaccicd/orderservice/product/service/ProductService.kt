package nuts.iaccicd.orderservice.product.service

import jakarta.persistence.EntityNotFoundException
import nuts.iaccicd.orderservice.product.entity.ProductEntity
import nuts.iaccicd.orderservice.product.model.ProductCreateRequest
import nuts.iaccicd.orderservice.product.model.ProductResponse
import nuts.iaccicd.orderservice.product.repository.ProductRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ProductService(
    private val productRepository: ProductRepository
) {

    @Transactional
    fun create(request: ProductCreateRequest): ProductResponse {
        val saved = productRepository.save(
            ProductEntity(
                name = request.name,
                price = request.price,
                stock = request.stock
            )
        )

        return ProductResponse(
            id = saved.id!!,
            name = saved.name,
            price = saved.price,
            stock = saved.stock
        )
    }

    @Transactional(readOnly = true)
    fun get(productId: Long): ProductResponse {
        val p = productRepository.findById(productId)
            .orElseThrow { EntityNotFoundException("Product not found. id=$productId") }

        return ProductResponse(
            id = p.id!!,
            name = p.name,
            price = p.price,
            stock = p.stock
        )
    }

    @Transactional(readOnly = true)
    fun list(): List<ProductResponse> {
        return productRepository.findAll()
            .sortedBy { it.id ?: Long.MAX_VALUE }
            .map {
                ProductResponse(
                    id = it.id!!,
                    name = it.name,
                    price = it.price,
                    stock = it.stock
                )
            }
    }
}