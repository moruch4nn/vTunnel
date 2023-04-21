package dev.mr3n.vtunnel.model

import kotlinx.serialization.Serializable

@Serializable
data class NewConnectionNotify(val port: Int, val token: String)
