package nuts.iaccicd.orderservice.order.repository

import nuts.iaccicd.orderservice.order.entity.OrderEntity
import org.springframework.data.jpa.repository.JpaRepository

interface OrderRepository : JpaRepository<OrderEntity, Long>