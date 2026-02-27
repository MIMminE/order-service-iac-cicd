package nuts.iaccicd.orderservice.order.service

import jakarta.persistence.EntityNotFoundException
import nuts.iaccicd.orderservice.order.entity.OrderEntity
import nuts.iaccicd.orderservice.order.entity.OrderItemEntity
import nuts.iaccicd.orderservice.order.model.OrderCreateRequest
import nuts.iaccicd.orderservice.order.model.OrderCreateResponse
import nuts.iaccicd.orderservice.order.model.OrderItemResponse
import nuts.iaccicd.orderservice.order.model.OrderResponse
import nuts.iaccicd.orderservice.order.repository.OrderRepository
import nuts.iaccicd.orderservice.product.repository.ProductRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class OrderService(
    private val orderRepository: OrderRepository,
    private val productRepository: ProductRepository
) {

    @Transactional
    fun createOrder(request: OrderCreateRequest, userId: Long? = null): OrderCreateResponse {
        // productId 중복이 있을 수 있으니 합쳐서 처리(락/차감 단순화)
        val merged = request.items
            .groupBy { it.productId ?: throw IllegalArgumentException("productId is required") }
            .mapValues { (_, items) -> items.sumOf { it.quantity } }

        val order = OrderEntity(userId = userId)

        var totalAmount = 0L

        // 데드락 가능성 낮추기 위해 productId 정렬 후 락 획득
        for ((productId, quantity) in merged.toSortedMap()) {
            val product = productRepository.findByIdForUpdate(productId)
                ?: throw EntityNotFoundException("Product not found. id=$productId")

            product.decreaseStock(quantity)

            val unitPrice = product.price
            val lineAmount = unitPrice * quantity
            totalAmount += lineAmount

            order.addItem(
                OrderItemEntity(
                    product = product,
                    quantity = quantity,
                    unitPrice = unitPrice,
                    lineAmount = lineAmount
                )
            )
        }

        order.totalAmount = totalAmount

        val saved = orderRepository.save(order)
        return OrderCreateResponse(orderId = saved.id!!, totalAmount = saved.totalAmount)
    }

    @Transactional(readOnly = true)
    fun getOrder(orderId: Long): OrderResponse {
        val order = orderRepository.findById(orderId)
            .orElseThrow { EntityNotFoundException("Order not found. id=$orderId") }

        val items = order.items.map {
            OrderItemResponse(
                productId = it.product.id!!,
                quantity = it.quantity,
                unitPrice = it.unitPrice,
                lineAmount = it.lineAmount
            )
        }

        return OrderResponse(
            orderId = order.id!!,
            status = order.status.name,
            totalAmount = order.totalAmount,
            items = items
        )
    }
}