package dev.mr3n.vtunnel.model

import com.auth0.jwt.interfaces.DecodedJWT
import kotlinx.datetime.Instant
import kotlinx.datetime.toKotlinInstant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class InitAuthModel(
    val name: String,
    @SerialName("forced_hosts")
    val forcedHosts: List<String>,
    val iss: String,
    val exp: Instant,
    val aud: List<String>) {
    constructor(jwt: DecodedJWT): this(jwt.getClaim("name").asString(), jwt.getClaim("forced_hosts").asList(String::class.java),jwt.issuer,jwt.expiresAtAsInstant.toKotlinInstant(),jwt.audience)
}