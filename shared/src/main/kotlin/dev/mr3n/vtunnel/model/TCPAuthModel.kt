package dev.mr3n.vtunnel.model

import com.auth0.jwt.interfaces.DecodedJWT

data class TCPAuthModel(val id: String) {
    constructor(jwt: DecodedJWT): this(jwt.getClaim("id").asString())
}