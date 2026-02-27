package nuts.iaccicd.orderservice.auth.jwt

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import nuts.iaccicd.orderservice.common.error.ApiErrorResponse
import nuts.iaccicd.orderservice.common.error.ErrorCode
import nuts.iaccicd.orderservice.common.response.ApiResponse
import org.springframework.http.HttpStatus
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

@Component
class RestAuthenticationEntryPoint(
    private val objectMapper: ObjectMapper
) : AuthenticationEntryPoint {

    override fun commence(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authException: AuthenticationException
    ) {
        val error = ApiErrorResponse.of(
            errorCode = ErrorCode.UNAUTHORIZED,
            message = "Unauthorized",
            status = HttpStatus.UNAUTHORIZED.value(),
            path = request.requestURI
        )
        write(response, HttpStatus.UNAUTHORIZED.value(), ApiResponse.fail(error))
    }

    private fun write(res: HttpServletResponse, status: Int, body: Any) {
        res.status = status
        res.contentType = "application/json"
        res.characterEncoding = "UTF-8"
        res.writer.write(objectMapper.writeValueAsString(body))
    }
}

@Component
class RestAccessDeniedHandler(
    private val objectMapper: ObjectMapper
) : AccessDeniedHandler {


    override fun handle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        accessDeniedException: AccessDeniedException
    ) {
       val error = ApiErrorResponse.of(
            errorCode = ErrorCode.FORBIDDEN,
            message = "Forbidden",
            status = HttpStatus.FORBIDDEN.value(),
            path = request.requestURI
        )
        response.status = HttpStatus.FORBIDDEN.value()
        response.contentType = "application/json"
        response.characterEncoding = "UTF-8"
        response.writer.write(objectMapper.writeValueAsString(ApiResponse.fail(error)))
    }
}