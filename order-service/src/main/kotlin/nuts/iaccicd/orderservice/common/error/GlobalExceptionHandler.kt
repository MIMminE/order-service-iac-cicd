package nuts.iaccicd.orderservice.common.error

import jakarta.persistence.EntityNotFoundException
import jakarta.servlet.http.HttpServletRequest
import nuts.iaccicd.orderservice.common.response.ApiResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.security.authorization.AuthorizationDeniedException
import org.springframework.security.access.AccessDeniedException

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(
        ex: MethodArgumentNotValidException,
        request: HttpServletRequest
    ): ResponseEntity<ApiResponse<Nothing>> {
        val fieldErrors: Map<String, String> = ex.bindingResult
            .allErrors
            .filterIsInstance<FieldError>()
            .associate { it.field to (it.defaultMessage ?: "invalid") }

        val error = ApiErrorResponse.of(
            errorCode = ErrorCode.VALIDATION_ERROR,
            message = ErrorCode.VALIDATION_ERROR.defaultMessage,
            status = HttpStatus.BAD_REQUEST.value(),
            path = request.requestURI,
            details = mapOf("fieldErrors" to fieldErrors)
        )
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.fail(error))
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleBadRequest(
        ex: IllegalArgumentException,
        request: HttpServletRequest
    ): ResponseEntity<ApiResponse<Nothing>> {
        val error = ApiErrorResponse.of(
            errorCode = ErrorCode.VALIDATION_ERROR,
            message = ex.message ?: ErrorCode.VALIDATION_ERROR.defaultMessage,
            status = HttpStatus.BAD_REQUEST.value(),
            path = request.requestURI
        )
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.fail(error))
    }

    @ExceptionHandler(EntityNotFoundException::class)
    fun handleNotFound(
        ex: EntityNotFoundException,
        request: HttpServletRequest
    ): ResponseEntity<ApiResponse<Nothing>> {
        val error = ApiErrorResponse.of(
            errorCode = ErrorCode.NOT_FOUND,
            message = ex.message ?: ErrorCode.NOT_FOUND.defaultMessage,
            status = HttpStatus.NOT_FOUND.value(),
            path = request.requestURI
        )
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.fail(error))
    }

    @ExceptionHandler(IllegalStateException::class)
    fun handleConflict(
        ex: IllegalStateException,
        request: HttpServletRequest
    ): ResponseEntity<ApiResponse<Nothing>> {
        // 재고 부족 같은 비즈니스 충돌을 여기로 매핑
        val error = ApiErrorResponse.of(
            errorCode = ErrorCode.CONFLICT,
            message = ex.message ?: ErrorCode.CONFLICT.defaultMessage,
            status = HttpStatus.CONFLICT.value(),
            path = request.requestURI
        )
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.fail(error))
    }

    @ExceptionHandler(AuthorizationDeniedException::class)
    fun handleAuthorizationDenied(
        ex: AuthorizationDeniedException,
        request: HttpServletRequest
    ): ResponseEntity<ApiResponse<Nothing>> {
        val error = ApiErrorResponse.of(
            errorCode = ErrorCode.FORBIDDEN,
            message = ErrorCode.FORBIDDEN.defaultMessage,
            status = HttpStatus.FORBIDDEN.value(),
            path = request.requestURI
        )
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.fail(error))
    }

    @ExceptionHandler(AccessDeniedException::class)
    fun handleAccessDenied(
        ex: AccessDeniedException,
        request: HttpServletRequest
    ): ResponseEntity<ApiResponse<Nothing>> {
        val error = ApiErrorResponse.of(
            errorCode = ErrorCode.FORBIDDEN,
            message = ErrorCode.FORBIDDEN.defaultMessage,
            status = HttpStatus.FORBIDDEN.value(),
            path = request.requestURI
        )
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.fail(error))
    }

    @ExceptionHandler(Exception::class)
    fun handleUnexpected(
        ex: Exception,
        request: HttpServletRequest
    ): ResponseEntity<ApiResponse<Nothing>> {
        val error = ApiErrorResponse.of(
            errorCode = ErrorCode.INTERNAL_ERROR,
            message = ErrorCode.INTERNAL_ERROR.defaultMessage,
            status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
            path = request.requestURI
        )
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.fail(error))
    }
}