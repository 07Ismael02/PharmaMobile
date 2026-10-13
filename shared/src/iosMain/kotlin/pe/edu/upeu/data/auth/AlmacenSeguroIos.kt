@file:OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)

package pe.edu.upeu.data.auth

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import platform.Foundation.CFBridgingRelease
import platform.Foundation.CFBridgingRetain
import platform.CoreFoundation.CFDictionaryAddValue
import platform.CoreFoundation.CFDictionaryCreateMutable
import platform.CoreFoundation.CFMutableDictionaryRef
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.CFTypeRefVar
import platform.CoreFoundation.kCFAllocatorDefault
import platform.CoreFoundation.kCFBooleanTrue
import platform.CoreFoundation.kCFTypeDictionaryKeyCallBacks
import platform.CoreFoundation.kCFTypeDictionaryValueCallBacks
import platform.Foundation.NSData
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.errSecSuccess
import platform.Security.kSecAttrAccessible
import platform.Security.kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecReturnData
import platform.Security.kSecValueData

/** El token permanece en Keychain y no se copia a preferencias ni a logs. */
class AlmacenSeguroIos : AlmacenSeguro {

    /** Construye la consulta base (servicio + cuenta) y libera la memoria CoreFoundation al terminar. */
    private inline fun <T> conConsulta(
        completar: (CFMutableDictionaryRef?) -> Unit,
        usar: (CFMutableDictionaryRef?) -> T
    ): T {
        val consulta = CFDictionaryCreateMutable(
            kCFAllocatorDefault, 0,
            kCFTypeDictionaryKeyCallBacks.ptr, kCFTypeDictionaryValueCallBacks.ptr
        )
        val servicio = CFBridgingRetain(SERVICIO)
        val cuenta = CFBridgingRetain(CUENTA)
        try {
            CFDictionaryAddValue(consulta, kSecClass, kSecClassGenericPassword)
            CFDictionaryAddValue(consulta, kSecAttrService, servicio)
            CFDictionaryAddValue(consulta, kSecAttrAccount, cuenta)
            completar(consulta)
            return usar(consulta)
        } finally {
            servicio?.let { CFRelease(it) }
            cuenta?.let { CFRelease(it) }
            consulta?.let { CFRelease(it) }
        }
    }

    override fun guardarToken(token: String) {
        borrarToken()
        val datos = CFBridgingRetain(NSString.create(string = token).dataUsingEncoding(NSUTF8StringEncoding))
        try {
            val estado = conConsulta({ consulta ->
                CFDictionaryAddValue(consulta, kSecValueData, datos)
                CFDictionaryAddValue(consulta, kSecAttrAccessible, kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly)
            }) { SecItemAdd(it, null) }
            check(estado == errSecSuccess) { "No se pudo guardar la sesión en Keychain" }
        } finally {
            datos?.let { CFRelease(it) }
        }
    }

    override fun leerToken(): String? = memScoped {
        val resultado = alloc<CFTypeRefVar>()
        val estado = conConsulta({ consulta ->
            CFDictionaryAddValue(consulta, kSecReturnData, kCFBooleanTrue)
            CFDictionaryAddValue(consulta, kSecMatchLimit, kSecMatchLimitOne)
        }) { SecItemCopyMatching(it, resultado.ptr) }
        if (estado != errSecSuccess) return@memScoped null
        val datos = CFBridgingRelease(resultado.value) as? NSData ?: return@memScoped null
        NSString.create(data = datos, encoding = NSUTF8StringEncoding)?.toString()
    }

    override fun borrarToken() {
        conConsulta({}) { SecItemDelete(it) }
    }

    private companion object {
        const val SERVICIO = "pe.edu.upeu.pharmamobil.sesion"
        const val CUENTA = "jwt"
    }
}
