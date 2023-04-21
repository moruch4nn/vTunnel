package dev.mr3n.vtunnel.model

import kotlinx.serialization.Serializable

@Serializable
data class AuthFrame(val token: String)