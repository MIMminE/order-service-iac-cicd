package nuts.iaccicd.orderservice.order.model

import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull

data class OrderCreateRequest(
    @field:NotEmpty
    @field:Valid
    val items: List<OrderItemCreateRequest>
)

data class OrderItemCreateRequest(
    @field:NotNull
    var productId: Long?,

    @field:Min(1)
    val quantity: Long
)

data class OrderCreateResponse(
    val orderId: Long,
    val totalAmount: Long
)

data class OrderItemResponse(
    val productId: Long,
    val quantity: Long,
    val unitPrice: Long,
    val lineAmount: Long
)

data class OrderResponse(
    val orderId: Long,
    val status: String,
    val totalAmount: Long,
    val items: List<OrderItemResponse>
)