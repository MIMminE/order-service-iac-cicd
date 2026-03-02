package nuts.iaccicd.orderservice.product

import nuts.iaccicd.orderservice.auth.entity.UserEntity
import nuts.iaccicd.orderservice.auth.entity.UserRole
import nuts.iaccicd.orderservice.auth.jwt.JwtTokenProvider
import nuts.iaccicd.orderservice.auth.repository.UserRepository
import nuts.iaccicd.orderservice.product.model.ProductCreateRequest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import com.fasterxml.jackson.databind.ObjectMapper

@Suppress("NonAsciiCharacters")
@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class ProductAdminAccessMvcTest {

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

            // JwtProperties 주입용 (컨텍스트 로딩/토큰 생성용)
            registry.add("app.jwt.issuer") { "test-issuer" }
            registry.add("app.jwt.secret") { "test-secret-test-secret-test-secret-test-secret" }
            registry.add("app.jwt.access-token-ttl-seconds") { "3600" }
        }
    }

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var passwordEncoder: PasswordEncoder

    @Autowired
    lateinit var jwtTokenProvider: JwtTokenProvider

    @Test
    fun `ADMIN 토큰이면 상품 생성이 201`() {
        val admin = passwordEncoder.encode("password1234")?.let {
            userRepository.save(
                UserEntity(
                    email = "admin@test.com",
                    passwordHash = it,
                    role = UserRole.ADMIN
                )
            )
        }

        val token = admin?.let {
            jwtTokenProvider.createAccessToken(
                userId = it.id!!,
                email = admin.email,
                role = admin.role.name
            )
        }

        val body = ProductCreateRequest(name = "item", price = 1000L, stock = 10L)

        mockMvc.post("/products") {
            contentType = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer $token")
            content = objectMapper.writeValueAsString(body)
        }.andExpect {
            status { isCreated() }
            content { contentTypeCompatibleWith(MediaType.APPLICATION_JSON) }
            jsonPath("$.id") { exists() }
            jsonPath("$.name") { value("item") }
            jsonPath("$.price") { value(1000) }
            jsonPath("$.stock") { value(10) }
        }
    }

    @Test
    fun `USER 토큰이면 상품 생성이 403`() {
        val user = passwordEncoder.encode("password1234")?.let {
            userRepository.save(
                UserEntity(
                    email = "user@test.com",
                    passwordHash = it,
                    role = UserRole.USER
                )
            )
        }

        val token = user?.let {
            jwtTokenProvider.createAccessToken(
                userId = it.id!!,
                email = user.email,
                role = user.role.name
            )
        }

        val body = ProductCreateRequest(name = "item2", price = 1000L, stock = 10L)

        val result = mockMvc.post("/products") {
            contentType = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer $token")
            content = objectMapper.writeValueAsString(body)
        }.andReturn()

        assertEquals(403, result.response.status)

    }

    @Test
    fun `토큰 없으면 상품 생성이 401`() {
        val body = ProductCreateRequest(name = "item3", price = 1000L, stock = 10L)

        val result = mockMvc.post("/products") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(body)
        }.andReturn()

        assertEquals(401, result.response.status)
    }
}