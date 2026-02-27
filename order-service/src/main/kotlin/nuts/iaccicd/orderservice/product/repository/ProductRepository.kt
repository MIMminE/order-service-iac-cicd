package nuts.iaccicd.orderservice.product.repository

import jakarta.persistence.LockModeType
import nuts.iaccicd.orderservice.product.entity.ProductEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param


interface ProductRepository : JpaRepository<ProductEntity, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from ProductEntity p where p.id = :id")
    fun findByIdForUpdate(@Param("id") id: Long): ProductEntity?
}