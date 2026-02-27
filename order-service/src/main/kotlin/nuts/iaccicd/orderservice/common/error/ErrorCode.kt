package nuts.iaccicd.orderservice.common.error

enum class ErrorCode(val code: String, val defaultMessage: String) {
    VALIDATION_ERROR("VALIDATION_ERROR", "Validation failed"),
    NOT_FOUND("NOT_FOUND", "Resource not found"),
    CONFLICT("CONFLICT", "Conflict"),
    UNAUTHORIZED("UNAUTHORIZED", "Unauthorized"),
    FORBIDDEN("FORBIDDEN", "Forbidden"),
    INTERNAL_ERROR("INTERNAL_ERROR", "Internal server error")
}