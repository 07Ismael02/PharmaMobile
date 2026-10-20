package pe.edu.upeu.data.auth

import io.ktor.http.Url

/** Vinculación de una sola cuenta por instalación; no se elimina durante logout. */
interface VinculacionProtegida {
    /** Solo se llama tras una respuesta autenticada del servidor. Devuelve null para otra cuenta. */
    fun verificarOVincular(claveCuenta: String): String?
}

/** El backend impone unicidad de username sin distinguir mayúsculas, no su inmutabilidad. */
fun claveCuentaValidada(urlBase: String, usernameConfirmado: String): String {
    val url = Url(urlBase)
    val usuario = usernameConfirmado.trim().lowercase()
    require(usuario.isNotEmpty() && usuario.none { it.isISOControl() })
    require(url.host.isNotBlank() && url.protocol.name in setOf("http", "https"))
    val origen = "${url.protocol.name.lowercase()}://${url.host.lowercase()}:${url.port}${url.encodedPath.trimEnd('/')}"
    return "$origen\u0000$usuario"
}
