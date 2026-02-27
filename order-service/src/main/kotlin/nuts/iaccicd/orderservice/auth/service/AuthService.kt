package nuts.iaccicd.orderservice.auth.service

import nuts.iaccicd.orderservice.auth.entity.UserEntity
import nuts.iaccicd.orderservice.auth.entity.UserRole
import nuts.iaccicd.orderservice.auth.jwt.JwtTokenProvider
import nuts.iaccicd.orderservice.auth.model.LoginRequest
import nuts.iaccicd.orderservice.auth.model.SignupRequest
import nuts.iaccicd.orderservice.auth.model.TokenResponse
import nuts.iaccicd.orderservice.auth.repository.UserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


@Service
class AuthService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtTokenProvider: JwtTokenProvider
) {

    @Transactional
    fun signup(request: SignupRequest) {
        if (userRepository.existsByEmail(request.email)) {
            throw IllegalStateException("Email already exists")
        }

        passwordEncoder.encode(request.password)?.let {
            userRepository.save(
                UserEntity(
                    email = request.email,
                    passwordHash = it,
                    role = UserRole.USER
                )
            )
        }
    }

    @Transactional(readOnly = true)
    fun login(request: LoginRequest): TokenResponse {
        val user = userRepository.findByEmail(request.email)
            ?: throw IllegalArgumentException("Invalid credentials")

        if (!passwordEncoder.matches(request.password, user.passwordHash)) {
            throw IllegalArgumentException("Invalid credentials")
        }

        val token = jwtTokenProvider.createAccessToken(
            userId = user.id!!,
            email = user.email,
            role = user.role.name
        )
        return TokenResponse(accessToken = token)
    }
}