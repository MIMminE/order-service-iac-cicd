package nuts.iaccicd.orderservice.auth.jwt

import nuts.iaccicd.orderservice.auth.config.JwtProperties
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@Component
class JwtTokenProvider(
    private val props: JwtProperties,
    private val objectMapper: ObjectMapper
) {
    private val base64UrlEncoder = Base64.getUrlEncoder().withoutPadding()
    private val base64UrlDecoder = Base64.getUrlDecoder()

    fun createAccessToken(userId: Long, email: String, role: String): String {
        val header = mapOf(
            "alg" to "HS256",
            "typ" to "JWT"
        )

        val now = Instant.now()
        val exp = now.plusSeconds(props.accessTokenTtlSeconds)

        val payload = mapOf(
            "iss" to props.issuer,
            "sub" to userId.toString(),
            "email" to email,
            "role" to role,
            "iat" to now.epochSecond,
            "exp" to exp.epochSecond
        )

        val headerPart = base64UrlEncodeJson(header)
        val payloadPart = base64UrlEncodeJson(payload)
        val signingInput = "$headerPart.$payloadPart"
        val signature = hmacSha256(signingInput, props.secret)

        return "$signingInput.$signature"
    }

    fun parseAndValidate(token: String): JwtClaims {
        val parts = token.split(".")
        if (parts.size != 3) throw JwtException("Invalid token format")

        val headerPart = parts[0]
        val payloadPart = parts[1]
        val signaturePart = parts[2]

        val signingInput = "$headerPart.$payloadPart"
        val expectedSig = hmacSha256(signingInput, props.secret)
        if (!constantTimeEquals(expectedSig, signaturePart)) {
            throw JwtException("Invalid token signature")
        }

        val payloadJson = String(base64UrlDecoder.decode(payloadPart), StandardCharsets.UTF_8)
        val node = objectMapper.readTree(payloadJson)

        val iss = node["iss"]?.asString() ?: throw JwtException("Missing iss")
        if (iss != props.issuer) throw JwtException("Invalid issuer")

        val sub = node["sub"]?.asString() ?: throw JwtException("Missing sub")
        val userId = sub.toLongOrNull() ?: throw JwtException("Invalid sub")

        val email = node["email"]?.asString() ?: throw JwtException("Missing email")
        val role = node["role"]?.asString() ?: "USER"

        val exp = node["exp"]?.asLong() ?: throw JwtException("Missing exp")
        val now = Instant.now().epochSecond
        if (now >= exp) throw JwtException("Token expired")

        return JwtClaims(
            userId = userId,
            email = email,
            role = role
        )
    }

    private fun base64UrlEncodeJson(value: Any): String {
        val jsonBytes = objectMapper.writeValueAsBytes(value)
        return base64UrlEncoder.encodeToString(jsonBytes)
    }

    private fun hmacSha256(signingInput: String, secret: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        val key = SecretKeySpec(secret.toByteArray(StandardCharsets.UTF_8), "HmacSHA256")
        mac.init(key)
        val sigBytes = mac.doFinal(signingInput.toByteArray(StandardCharsets.UTF_8))
        return base64UrlEncoder.encodeToString(sigBytes)
    }

    private fun constantTimeEquals(a: String, b: String): Boolean {
        if (a.length != b.length) return false
        var result = 0
        for (i in a.indices) {
            result = result or (a[i].code xor b[i].code)
        }
        return result == 0
    }
}

data class JwtClaims(
    val userId: Long,
    val email: String,
    val role: String
)

class JwtException(message: String) : RuntimeException(message)