package nuts.iaccicd.orderservice.product.model

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class ProductCreateRequest(
    @field:NotBlank
    @field:Size(max = 120)
    val name: String,

    @field:Min(0)
    val price: Long,

    @field:Min(0)
    val stock: Long
)

data class ProductResponse(
    val id: Long,
    val name: String,
    val price: Long,
    val stock: Long
)

data class ProductListResponse(
    val items: List<ProductResponse>
)