package nuts.iaccicd.orderservice.common.response

import com.fasterxml.jackson.annotation.JsonInclude
import nuts.iaccicd.orderservice.common.error.ApiErrorResponse


@JsonInclude(JsonInclude.Include.NON_NULL)
data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val error: ApiErrorResponse? = null
) {
    companion object {
        fun <T> ok(data: T): ApiResponse<T> = ApiResponse(success = true, data = data)
        fun fail(error: ApiErrorResponse): ApiResponse<Nothing> = ApiResponse(success = false, error = error)
    }
}