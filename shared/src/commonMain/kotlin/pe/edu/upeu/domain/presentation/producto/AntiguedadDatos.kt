package pe.edu.upeu.domain.presentation.producto

/** El instante y la hora actual están expresados en segundos Unix. */
fun antiguedadDatos(instante: Long?, ahora: Long): String {
    if (instante == null) return "Nunca sincronizado"
    val segundos = (ahora - instante).coerceAtLeast(0)
    if (segundos < 60) return "Actualizado hace menos de un minuto"
    val minutos = segundos / 60
    if (minutos < 60) return "Actualizado hace $minutos ${if (minutos == 1L) "minuto" else "minutos"}"
    val horas = minutos / 60
    if (horas < 24) return "Actualizado hace $horas ${if (horas == 1L) "hora" else "horas"}"
    val dias = horas / 24
    return "Actualizado hace $dias ${if (dias == 1L) "día" else "días"}"
}
