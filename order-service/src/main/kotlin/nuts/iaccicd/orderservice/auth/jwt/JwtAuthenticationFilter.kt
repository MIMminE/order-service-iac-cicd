package nuts.iaccicd.orderservice.auth.jwt

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter

class JwtAuthenticationFilter(
    private val jwtTokenProvider: JwtTokenProvider
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val authHeader = request.getHeader("Authorization")
        val token = extractBearerToken(authHeader)

        if (token != null) {
            try {
                val claims = jwtTokenProvider.parseAndValidate(token)
                val authorities = listOf(SimpleGrantedAuthority("ROLE_${claims.role}"))
                val principal = AuthPrincipal(claims.userId, claims.email, claims.role)
                val authentication = UsernamePasswordAuthenticationToken(principal, null, authorities)
                SecurityContextHolder.getContext().authentication = authentication
            } catch (_: JwtException) {
                // 토큰이 잘못된 경우: 인증정보 세팅하지 않고 통과
                // 보호 API 접근 시 401로 떨어짐
                SecurityContextHolder.clearContext()
            }
        }

        filterChain.doFilter(request, response)
    }

    private fun extractBearerToken(authHeader: String?): String? {
        if (authHeader.isNullOrBlank()) return null
        if (!authHeader.startsWith("Bearer ")) return null
        return authHeader.removePrefix("Bearer ").trim().ifBlank { null }
    }
}

data class AuthPrincipal(
    val userId: Long,
    val email: String,
    val role: String
)