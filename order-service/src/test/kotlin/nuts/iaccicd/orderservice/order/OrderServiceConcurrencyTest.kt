package nuts.iaccicd.orderservice.order

import nuts.iaccicd.orderservice.order.model.OrderCreateRequest
import nuts.iaccicd.orderservice.order.model.OrderItemCreateRequest
import nuts.iaccicd.orderservice.order.service.OrderService
import nuts.iaccicd.orderservice.product.entity.ProductEntity
import nuts.iaccicd.orderservice.product.repository.ProductRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@Testcontainers
@SpringBootTest
@Suppress("NonAsciiCharacters")
class OrderServiceConcurrencyTest {

    companion object {
        @Container
        val postgres = PostgreSQLContainer("postgres:16-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test")

        @JvmStatic
        @DynamicPropertySource
        fun props(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url") { postgres.jdbcUrl }
            registry.add("spring.datasource.username") { postgres.username }
            registry.add("spring.datasource.password") { postgres.password }
            registry.add("spring.jpa.hibernate.ddl-auto") { "validate" }
            registry.add("spring.flyway.enabled") { "true" }

            // JWT 설정이 없으면 컨텍스트 로딩이 깨질 수 있어(빈 생성 시), 테스트용으로 주입
            registry.add("app.jwt.issuer") { "test-issuer" }
            registry.add("app.jwt.secret") { "test-secret-test-secret-test-secret-test-secret" }
            registry.add("app.jwt.access-token-ttl-seconds") { "3600" }
        }
    }

    @Autowired
    lateinit var orderService: OrderService

    @Autowired
    lateinit var productRepository: ProductRepository

    @Test
    fun `동시에 주문해도 재고는 음수가 되지 않고 성공 주문 수는 재고 한도 내로 제한된다`() {
        // given
        val product = productRepository.save(
            ProductEntity(
                name = "concurrency-item",
                price = 1000L,
                stock = 10L
            )
        )
        val productId = product.id!!

        val threads = 30
        val qtyPerOrder = 1L

        val startGate = CountDownLatch(1)
        val doneGate = CountDownLatch(threads)

        val successCount = AtomicInteger(0)
        val conflictCount = AtomicInteger(0)
        val otherFailCount = AtomicInteger(0)

        val pool = Executors.newFixedThreadPool(threads)

        try {
            val tasks = (1..threads).map {
                Callable {
                    startGate.await(5, TimeUnit.SECONDS)
                    try {
                        orderService.createOrder(
                            OrderCreateRequest(
                                items = listOf(
                                    OrderItemCreateRequest(productId = productId, quantity = qtyPerOrder)
                                )
                            ),
                            userId = null
                        )
                        successCount.incrementAndGet()
                    } catch (e: IllegalStateException) {
                        // 재고 부족 -> 409로 매핑되는 케이스
                        conflictCount.incrementAndGet()
                    } catch (e: Exception) {
                        otherFailCount.incrementAndGet()
                    } finally {
                        doneGate.countDown()
                    }
                }
            }

            tasks.forEach { pool.submit(it) }

            // when
            startGate.countDown()
            val finished = doneGate.await(30, TimeUnit.SECONDS)
            assertTrue(finished, "tasks did not finish in time")

            // then
            val reloaded = productRepository.findById(productId).orElseThrow()
            assertTrue(reloaded.stock >= 0L, "stock must not be negative")

            // 재고 10, 주문당 1개면 성공은 최대 10
            assertEquals(10, successCount.get(), "success orders should be capped by stock")
            assertEquals(threads - 10, conflictCount.get() + otherFailCount.get(), "remaining requests should fail")
        } finally {
            pool.shutdownNow()
        }
    }
}