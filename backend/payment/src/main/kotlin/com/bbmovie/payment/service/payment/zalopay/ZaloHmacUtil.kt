package com.bbmovie.payment.service.payment.zalopay

import java.util.LinkedList
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@Suppress("unused")
object ZaloHmacUtil {

    const val HMACMD5 = "HmacMD5"
    const val HMACSHA1 = "HmacSHA1"
    const val HMACSHA256 = "HmacSHA256"
    const val HMACSHA512 = "HmacSHA512"
    val UTF8CHARSET = Charsets.UTF_8
    val HMACS = LinkedList(listOf("UnSupport", "HmacSHA256", "HmacMD5", "HmacSHA384", "HMacSHA1", "HmacSHA512"))

    private fun hmacEncode(algorithm: String, key: String, data: String): ByteArray? {
        val macGenerator = try {
            Mac.getInstance(algorithm).apply {
                init(SecretKeySpec(key.toByteArray(UTF8CHARSET), algorithm))
            }
        } catch (ex: Exception) {
            null
        }

        val dataByte = data.toByteArray(UTF8CHARSET)
        return macGenerator?.doFinal(dataByte)
    }

    fun hmacHexStringEncode(algorithm: String, key: String, data: String): String? {
        return hmacEncode(algorithm, key, data)?.let {
            ZaloHexStringUtil.byteArrayToHexString(it)
        }
    }
}