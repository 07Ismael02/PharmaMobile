package pe.edu.upeu.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class CategoriaResponseDto(
    val id: Long,
    val nombre: String,
    val estado: Boolean
)
