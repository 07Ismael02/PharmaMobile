package pe.edu.upeu.domain.auth

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

class SesionTest {
    private val sesion = Sesion("jwt-privado", "almacen", "Almacén", setOf("CATALOGO_LEER"))

    @Test
    fun permisosDistinguenLecturaYEscritura() {
        assertTrue(sesion.puede("CATALOGO_LEER"))
        assertFalse(sesion.puede("CATALOGO_ESCRIBIR"))
    }

    @Test
    fun tokenYClaveNoAparecenEnRepresentacion() {
        assertFalse(sesion.toString().contains("jwt-privado"))
        assertFalse(Credenciales("almacen", "clave-privada").toString().contains("clave-privada"))
    }

    @Test
    fun coordinadorInvalidaSesionAlExpirar() {
        val coordinador = CoordinadorSesion()
        coordinador.activa(sesion)
        assertEquals(sesion, coordinador.estado.value.sesion)
        coordinador.expirada()
        assertNull(coordinador.estado.value.sesion)
        assertFalse(coordinador.estado.value.cargando)
    }

    @Test
    fun casosDeUsoDeleganYNoDuplicanSolicitudes() = runBlocking {
        val repositorio = RepositorioFalso()
        assertEquals(sesion, IniciarSesionUseCase(repositorio)(Credenciales("almacen", "clave")))
        assertEquals(sesion, RestaurarSesionUseCase(repositorio)())
        CerrarSesionUseCase(repositorio)()
        assertEquals(1, repositorio.logins)
        assertEquals(1, repositorio.restauraciones)
        assertEquals(1, repositorio.cierres)
    }

    private inner class RepositorioFalso : AuthRepository {
        var logins = 0
        var restauraciones = 0
        var cierres = 0
        override suspend fun iniciarSesion(credenciales: Credenciales): Sesion {
            logins++
            return sesion
        }
        override suspend fun sesionGuardada(): Sesion? {
            restauraciones++
            return sesion
        }
        override suspend fun cerrarSesion() { cierres++ }
    }
}
