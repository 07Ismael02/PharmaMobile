package pe.edu.upeu.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import pe.edu.upeu.data.remote.dto.CategoriaResponseDto

class CategoriaApi(private val client: HttpClient) {
    suspend fun listar(): List<CategoriaResponseDto> = client.get("categorias").body()
}
