package pe.edu.upeu.pharmamobil.platform

import android.os.Build
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class InfoDispositivoAndroidTest {
    @Test
    fun muestraSistemaYVersionRealesDeAndroid() {
        val informacion = InfoDispositivo()

        assertEquals("Android", informacion.sistema)
        assertEquals(Build.VERSION.RELEASE, informacion.version)
        assertTrue(informacion.version.isNotBlank())
    }
}
