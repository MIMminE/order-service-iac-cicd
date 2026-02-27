package nuts.iaccicd.orderservice.order.controller

import jakarta.validation.Valid
import nuts.iaccicd.orderservice.auth.jwt.AuthPrincipal
import nuts.iaccicd.orderservice.order.model.OrderCreateRequest
import nuts.iaccicd.orderservice.order.model.OrderCreateResponse
import nuts.iaccicd.orderservice.order.model.OrderResponse
import nuts.iaccicd.orderservice.order.service.OrderService
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/orders")
class OrderController(
    private val orderService: OrderService
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @Valid @RequestBody request: OrderCreateRequest,
        @AuthenticationPrincipal principal: AuthPrincipal?
    ): OrderCreateResponse {
        return orderService.createOrder(request, userId = principal?.userId)
    }

    @GetMapping("/{orderId}")
    fun get(@PathVariable orderId: Long): OrderResponse {
        return orderService.getOrder(orderId)
    }
}