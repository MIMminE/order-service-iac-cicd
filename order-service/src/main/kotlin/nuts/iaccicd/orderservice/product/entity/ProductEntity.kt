package nuts.iaccicd.orderservice.product.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.OffsetDateTime

@Entity
@Table(
    name = "products",
    indexes = [
        Index(name = "idx_products_name", columnList = "name")
    ]
)
class ProductEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false, length = 120)
    var name: String,

    @Column(nullable = false)
    var price: Long,

    @Column(nullable = false)
    var stock: Long,

    @Column(name = "created_at", nullable = false)
    var createdAt: OffsetDateTime = OffsetDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: OffsetDateTime = OffsetDateTime.now()
) {
    fun decreaseStock(quantity: Long) {
        require(quantity > 0) { "quantity must be positive" }
        if (stock < quantity) {
            throw IllegalStateException("Insufficient stock. stock=$stock, quantity=$quantity")
        }
        stock -= quantity
        updatedAt = OffsetDateTime.now()
    }
}