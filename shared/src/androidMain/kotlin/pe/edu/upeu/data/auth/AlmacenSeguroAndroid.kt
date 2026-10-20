package pe.edu.upeu.data.auth

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Solo el texto cifrado queda en preferencias; la clave vive en Android Keystore. */
class AlmacenSeguroAndroid(contexto: Context) : AlmacenSeguro, VinculacionProtegida {
    private val preferencias = contexto.getSharedPreferences("sesion_segura", Context.MODE_PRIVATE)
    private val alias = "pharmamobil.jwt.v1"

    private fun clave(): SecretKey {
        val almacen = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (almacen.getKey(alias, null) as? SecretKey)?.let { return it }
        val generador = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generador.init(KeyGenParameterSpec.Builder(alias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build())
        return generador.generateKey()
    }

    override fun guardarToken(token: String) {
        guardarCifrado("token", token)
    }

    private fun guardarCifrado(campo: String, valor: String) {
        val cifrador = Cipher.getInstance("AES/GCM/NoPadding")
        cifrador.init(Cipher.ENCRYPT_MODE, clave())
        val iv = Base64.encodeToString(cifrador.iv, Base64.NO_WRAP)
        val datos = Base64.encodeToString(cifrador.doFinal(valor.toByteArray(Charsets.UTF_8)), Base64.NO_WRAP)
        check(preferencias.edit().putString(campo, "$iv:$datos").commit())
    }

    override fun leerToken(): String? = leerCifrado("token")

    private fun leerCifrado(campo: String): String? {
        val guardado = preferencias.getString(campo, null) ?: return null
        return try {
            val partes = guardado.split(':', limit = 2)
            if (partes.size != 2) return null
            val iv = Base64.decode(partes[0], Base64.NO_WRAP)
            val cifrado = Base64.decode(partes[1], Base64.NO_WRAP)
            val cifrador = Cipher.getInstance("AES/GCM/NoPadding")
            cifrador.init(Cipher.DECRYPT_MODE, clave(), GCMParameterSpec(128, iv))
            String(cifrador.doFinal(cifrado), Charsets.UTF_8)
        } catch (_: Exception) {
            if (campo == "cuenta_vinculada") {
                error("No se puede leer la vinculación protegida de esta instalación")
            }
            preferencias.edit().remove(campo).commit()
            null
        }
    }

    override fun borrarToken() {
        preferencias.edit().remove("token").commit()
    }

    @Synchronized
    override fun verificarOVincular(claveCuenta: String): String? {
        require(claveCuenta.isNotBlank())
        val anterior = leerCifrado("cuenta_vinculada")
        if (anterior != null) {
            val partes = anterior.split('\n', limit = 2)
            check(partes.size == 2 && partes[0].matches(Regex("pharma-cuenta-[0-9a-f]{32}\\.db"))) {
                "La vinculación protegida no es válida"
            }
            return partes[0].takeIf { partes[1] == claveCuenta }
        }
        val aleatorio = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val nombre = "pharma-cuenta-${aleatorio.joinToString("") { "%02x".format(it) }}.db"
        guardarCifrado("cuenta_vinculada", "$nombre\n$claveCuenta")
        return nombre
    }
}
