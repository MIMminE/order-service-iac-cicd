package nuts.iaccicd.orderservice.auth.controller

import jakarta.validation.Valid
import nuts.iaccicd.orderservice.auth.model.LoginRequest
import nuts.iaccicd.orderservice.auth.model.SignupRequest
import nuts.iaccicd.orderservice.auth.model.TokenResponse
import nuts.iaccicd.orderservice.auth.service.AuthService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/auth")
class AuthController(
    private val authService: AuthService
) {

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    fun signup(@Valid @RequestBody request: SignupRequest) {
        authService.signup(request)
    }

    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequest): TokenResponse {
        return authService.login(request)
    }
}