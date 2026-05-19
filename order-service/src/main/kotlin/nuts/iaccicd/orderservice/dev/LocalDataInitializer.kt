package nuts.iaccicd.orderservice.dev

import nuts.iaccicd.orderservice.auth.entity.UserEntity
import nuts.iaccicd.orderservice.auth.entity.UserRole
import nuts.iaccicd.orderservice.auth.repository.UserRepository
import nuts.iaccicd.orderservice.product.entity.ProductEntity
import nuts.iaccicd.orderservice.product.repository.ProductRepository
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Profile
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
@Profile("!prod")
class LocalDataInitializer(
    private val userRepository: UserRepository,
    private val productRepository: ProductRepository,
    private val passwordEncoder: PasswordEncoder
) : ApplicationRunner {

    @Transactional
    override fun run(args: ApplicationArguments) {
        if (!userRepository.existsByEmail("admin@orders.local")) {
            userRepository.save(
                UserEntity(
                    email = "admin@orders.local",
                    passwordHash = passwordEncoder.encode("password123") ?: error("Password encoding failed"),
                    role = UserRole.ADMIN
                )
            )
        }

        if (productRepository.count() == 0L) {
            productRepository.saveAll(
                listOf(
                    ProductEntity(name = "Daily Tote Bag", price = 42000, stock = 120),
                    ProductEntity(name = "Cotton Zip Hoodie", price = 69000, stock = 80),
                    ProductEntity(name = "Ceramic Mug Set", price = 28000, stock = 150),
                    ProductEntity(name = "Desk Organizer", price = 36000, stock = 95)
                )
            )
        }
    }
}
