package pe.edu.upeu.data.auth

interface AlmacenSeguro {
    fun guardarToken(token: String)
    fun leerToken(): String?
    fun borrarToken()
    /** Datos verificados por el servidor; las plataformas sin soporte mantienen el acceso offline cerrado. */
    fun guardarIdentidadValidada(datos: String): Boolean = false
    fun leerIdentidadValidada(): String? = null
    fun borrarIdentidadValidada() = Unit
}
