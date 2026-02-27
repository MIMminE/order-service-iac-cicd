package nuts.iaccicd.orderservice.common.error

import java.time.OffsetDateTime

data class ApiErrorResponse(
    val code: String,
    val message: String,
    val status: Int,
    val path: String,
    val timestamp: OffsetDateTime = OffsetDateTime.now(),
    val details: Map<String, Any?>? = null
) {
    companion object {
        fun of(
            errorCode: ErrorCode,
            message: String = errorCode.defaultMessage,
            status: Int,
            path: String,
            details: Map<String, Any?>? = null
        ): ApiErrorResponse {
            return ApiErrorResponse(
                code = errorCode.code,
                message = message,
                status = status,
                path = path,
                details = details
            )
        }
    }
}