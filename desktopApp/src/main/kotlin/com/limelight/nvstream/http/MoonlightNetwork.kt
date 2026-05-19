package com.limelight.nvstream.http

import okhttp3.*
import org.bouncycastle.asn1.x500.X500NameBuilder
import org.bouncycastle.asn1.x500.style.BCStyle
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo
import org.bouncycastle.cert.X509v3CertificateBuilder
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.openssl.jcajce.JcaPEMWriter
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import org.xmlpull.v1.XmlPullParserFactory
import java.io.*
import java.math.BigInteger
import java.net.Proxy
import java.net.Socket
import java.security.*
import java.security.cert.CertificateException
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.spec.PKCS8EncodedKeySpec
import java.util.*
import java.util.concurrent.TimeUnit
import javax.net.ssl.*
import com.limelight.shared.model.GameInfo

// 🔑 1. Standard Multiplatform Crypto Provider Interface
interface LimelightCryptoProvider {
    fun getClientCertificate(): X509Certificate
    fun getClientPrivateKey(): PrivateKey
    fun getPemEncodedClientCertificate(): ByteArray
    fun encodeBase64String(data: ByteArray): String
}

// 🏢 2. Desktop JVM Specific Cryptography & Certificate Management
class DesktopCryptoProvider : LimelightCryptoProvider {
    private val dataDir = File(System.getProperty("user.home"), ".moonlight-personal").apply { mkdirs() }
    private val certFile = File(dataDir, "client.crt")
    private val keyFile = File(dataDir, "client.key")
    
    private var cert: X509Certificate? = null
    private var key: PrivateKey? = null
    private var pemCertBytes: ByteArray? = null
    
    private val bcProvider = BouncyCastleProvider().also {
        Security.addProvider(it)
    }

    override fun getClientCertificate(): X509Certificate {
        synchronized(this) {
            cert?.let { return it }
            if (loadCertKeyPair()) return cert!!
            generateCertKeyPair()
            loadCertKeyPair()
            return cert!!
        }
    }

    override fun getClientPrivateKey(): PrivateKey {
        synchronized(this) {
            key?.let { return it }
            if (loadCertKeyPair()) return key!!
            generateCertKeyPair()
            loadCertKeyPair()
            return key!!
        }
    }

    override fun getPemEncodedClientCertificate(): ByteArray {
        synchronized(this) {
            getClientCertificate()
            return pemCertBytes!!
        }
    }

    override fun encodeBase64String(data: ByteArray): String {
        return Base64.getEncoder().encodeToString(data)
    }

    private fun loadCertKeyPair(): Boolean {
        if (!certFile.exists() || !keyFile.exists()) return false
        try {
            val certBytes = certFile.readBytes()
            val cleanCertBytes = certBytes.filter { it != 0x0D.toByte() }.toByteArray()
            val keyBytes = keyFile.readBytes()
            
            val cf = CertificateFactory.getInstance("X.509", bcProvider)
            cert = cf.generateCertificate(ByteArrayInputStream(cleanCertBytes)) as X509Certificate
            pemCertBytes = cleanCertBytes
            
            val kf = KeyFactory.getInstance("RSA", bcProvider)
            key = kf.generatePrivate(PKCS8EncodedKeySpec(keyBytes))
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    private fun generateCertKeyPair() {
        try {
            val kpg = KeyPairGenerator.getInstance("RSA", bcProvider)
            kpg.initialize(2048)
            val kp = kpg.generateKeyPair()
            
            val now = Date()
            val cal = Calendar.getInstance().apply {
                time = now
                add(Calendar.YEAR, 20)
            }
            val expirationDate = cal.time
            val serial = BigInteger(64, SecureRandom()).abs()
            
            val nameBuilder = X500NameBuilder(BCStyle.INSTANCE).apply {
                addRDN(BCStyle.CN, "NVIDIA GameStream Client")
            }
            val name = nameBuilder.build()
            
            val certBuilder = X509v3CertificateBuilder(
                name, serial, now, expirationDate, Locale.ENGLISH, name,
                SubjectPublicKeyInfo.getInstance(kp.public.encoded)
            )
            
            val sigGen = JcaContentSignerBuilder("SHA256withRSA").setProvider(bcProvider).build(kp.private)
            val generatedCert = JcaX509CertificateConverter().setProvider(bcProvider).getCertificate(certBuilder.build(sigGen))
            
            // Save cert in Unix PEM format
            val sw = StringWriter()
            JcaPEMWriter(sw).use { it.writeObject(generatedCert) }
            val pemStr = sw.buffer.toString().replace("\r", "")
            certFile.writeBytes(pemStr.toByteArray(Charsets.UTF_8))
            
            // Save key in PKCS8 format
            keyFile.writeBytes(kp.private.encoded)
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }
}

// 🛡️ 3. Standard Helper Methods to Serialize Certificates for Persistence
fun X509Certificate.toPemString(): String {
    val sw = StringWriter()
    JcaPEMWriter(sw).use { it.writeObject(this) }
    return sw.buffer.toString()
}

fun String.toX509Certificate(): X509Certificate? {
    try {
        val bc = BouncyCastleProvider()
        Security.addProvider(bc)
        val cf = CertificateFactory.getInstance("X.509", bc)
        return cf.generateCertificate(ByteArrayInputStream(this.toByteArray())) as X509Certificate
    } catch (e: Exception) {
        e.printStackTrace()
        return null
    }
}

// 📡 4. Moonlight Client-Side Pairing Manager
class PairingManager(
    private val http: NvHTTP,
    cryptoProvider: LimelightCryptoProvider
) {
    private val pk: PrivateKey = cryptoProvider.getClientPrivateKey()
    private val cert: X509Certificate = cryptoProvider.getClientCertificate()
    private val pemCertBytes: ByteArray = cryptoProvider.getPemEncodedClientCertificate()
    private var serverCert: X509Certificate? = null

    enum class PairState {
        NOT_PAIRED,
        PAIRED,
        PIN_WRONG,
        FAILED,
        ALREADY_IN_PROGRESS
    }

    private fun bytesToHex(bytes: ByteArray): String {
        val hexChars = CharArray(bytes.size * 2)
        val hexArray = "0123456789ABCDEF".toCharArray()
        for (j in bytes.indices) {
            val v = bytes[j].toInt() and 0xFF
            hexChars[j * 2] = hexArray[v ushr 4]
            hexChars[j * 2 + 1] = hexArray[v and 0x0F]
        }
        return String(hexChars)
    }

    private fun hexToBytes(s: String): ByteArray {
        val len = s.length
        require(len % 2 == 0) { "Illegal string length: $len" }
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] = ((Character.digit(s[i], 16) shl 4) + Character.digit(s[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }

    private fun extractPlainCert(text: String): X509Certificate? {
        val certText = NvHTTP.getXmlString(text, "plaincert", false) ?: return null
        val certBytes = hexToBytes(certText)
        val cf = CertificateFactory.getInstance("X.509")
        return cf.generateCertificate(ByteArrayInputStream(certBytes)) as X509Certificate
    }

    private fun generateRandomBytes(length: Int): ByteArray {
        val rand = ByteArray(length)
        SecureRandom().nextBytes(rand)
        return rand
    }

    private fun saltPin(salt: ByteArray, pin: String): ByteArray {
        val pinBytes = pin.toByteArray(charset("UTF-8"))
        val saltedPin = ByteArray(salt.size + pinBytes.size)
        System.arraycopy(salt, 0, saltedPin, 0, salt.size)
        System.arraycopy(pinBytes, 0, saltedPin, salt.size, pinBytes.size)
        return saltedPin
    }

    private fun getSha256SignatureInstanceForKey(key: Key): Signature {
        return when (key.algorithm) {
            "RSA" -> Signature.getInstance("SHA256withRSA")
            "EC" -> Signature.getInstance("SHA256withECDSA")
            else -> throw NoSuchAlgorithmException("Unhandled key algorithm: ${key.algorithm}")
        }
    }

    private fun verifySignature(data: ByteArray, signature: ByteArray, cert: java.security.cert.Certificate): Boolean {
        val sig = getSha256SignatureInstanceForKey(cert.publicKey)
        sig.initVerify(cert.publicKey)
        sig.update(data)
        return sig.verify(signature)
    }

    private fun signData(data: ByteArray, key: PrivateKey): ByteArray {
        val sig = getSha256SignatureInstanceForKey(key)
        sig.initSign(key)
        sig.update(data)
        return sig.sign()
    }

    private fun performBlockCipher(blockCipher: org.bouncycastle.crypto.BlockCipher, input: ByteArray): ByteArray {
        val blockSize = blockCipher.blockSize
        val blockRoundedSize = (input.size + (blockSize - 1)) and (blockSize - 1).inv()
        val blockRoundedInputData = Arrays.copyOf(input, blockRoundedSize)
        val blockRoundedOutputData = ByteArray(blockRoundedSize)
        var offset = 0
        while (offset < blockRoundedSize) {
            blockCipher.processBlock(blockRoundedInputData, offset, blockRoundedOutputData, offset)
            offset += blockSize
        }
        return blockRoundedOutputData
    }

    private fun decryptAes(encryptedData: ByteArray, aesKey: ByteArray): ByteArray {
        val aesEngine = org.bouncycastle.crypto.engines.AESLightEngine()
        aesEngine.init(false, org.bouncycastle.crypto.params.KeyParameter(aesKey))
        return performBlockCipher(aesEngine, encryptedData)
    }

    private fun encryptAes(plaintextData: ByteArray, aesKey: ByteArray): ByteArray {
        val aesEngine = org.bouncycastle.crypto.engines.AESLightEngine()
        aesEngine.init(true, org.bouncycastle.crypto.params.KeyParameter(aesKey))
        return performBlockCipher(aesEngine, plaintextData)
    }

    private fun generateAesKey(hashAlgo: PairingHashAlgorithm, keyData: ByteArray): ByteArray {
        return Arrays.copyOf(hashAlgo.hashData(keyData), 16)
    }

    private fun concatBytes(a: ByteArray, b: ByteArray): ByteArray {
        val c = ByteArray(a.size + b.size)
        System.arraycopy(a, 0, c, 0, a.size)
        System.arraycopy(b, 0, c, a.size, b.size)
        return c
    }

    fun getPairedCert(): X509Certificate? = serverCert

    fun pair(serverInfo: String, pin: String): PairState {
        val hashAlgo: PairingHashAlgorithm = if (http.getServerMajorVersion(serverInfo) >= 7) {
            Sha256PairingHash()
        } else {
            Sha1PairingHash()
        }

        val salt = generateRandomBytes(16)
        val aesKey = generateAesKey(hashAlgo, saltPin(salt, pin))

        val getCert = http.executePairingCommand(
            "phrase=getservercert&salt=" +
                    bytesToHex(salt) + "&clientcert=" + bytesToHex(pemCertBytes),
            false
        )
        if (NvHTTP.getXmlString(getCert, "paired", true) != "1") {
            return PairState.FAILED
        }

        serverCert = extractPlainCert(getCert)
        if (serverCert == null) {
            http.unpair()
            return PairState.ALREADY_IN_PROGRESS
        }

        http.setServerCert(serverCert!!)

        val randomChallenge = generateRandomBytes(16)
        val encryptedChallenge = encryptAes(randomChallenge, aesKey)

        val challengeResp = http.executePairingCommand("clientchallenge=" + bytesToHex(encryptedChallenge), true)
        if (NvHTTP.getXmlString(challengeResp, "paired", true) != "1") {
            http.unpair()
            return PairState.FAILED
        }

        val encServerChallengeResponse = hexToBytes(NvHTTP.getXmlString(challengeResp, "challengeresponse", true))
        val decServerChallengeResponse = decryptAes(encServerChallengeResponse, aesKey)

        val serverResponse = Arrays.copyOfRange(decServerChallengeResponse, 0, hashAlgo.getHashLength())
        val serverChallenge = Arrays.copyOfRange(
            decServerChallengeResponse,
            hashAlgo.getHashLength(),
            hashAlgo.getHashLength() + 16
        )

        val clientSecret = generateRandomBytes(16)
        val challengeRespHash = hashAlgo.hashData(
            concatBytes(
                concatBytes(serverChallenge, cert.signature),
                clientSecret
            )
        )
        val challengeRespEncrypted = encryptAes(challengeRespHash, aesKey)
        val secretResp = http.executePairingCommand("serverchallengeresp=" + bytesToHex(challengeRespEncrypted), true)
        if (NvHTTP.getXmlString(secretResp, "paired", true) != "1") {
            http.unpair()
            return PairState.FAILED
        }

        val serverSecretResp = hexToBytes(NvHTTP.getXmlString(secretResp, "pairingsecret", true))
        val serverSecret = Arrays.copyOfRange(serverSecretResp, 0, 16)
        val serverSignature = Arrays.copyOfRange(serverSecretResp, 16, serverSecretResp.size)

        if (!verifySignature(serverSecret, serverSignature, serverCert!!)) {
            http.unpair()
            return PairState.FAILED
        }

        val serverChallengeRespHash = hashAlgo.hashData(
            concatBytes(
                concatBytes(randomChallenge, serverCert!!.signature),
                serverSecret
            )
        )
        if (!Arrays.equals(serverChallengeRespHash, serverResponse)) {
            http.unpair()
            return PairState.PIN_WRONG
        }

        val clientPairingSecret = concatBytes(clientSecret, signData(clientSecret, pk))
        val clientSecretResp = http.executePairingCommand("clientpairingsecret=" + bytesToHex(clientPairingSecret), true)
        if (NvHTTP.getXmlString(clientSecretResp, "paired", true) != "1") {
            http.unpair()
            return PairState.FAILED
        }

        val pairChallenge = http.executePairingChallenge()
        if (NvHTTP.getXmlString(pairChallenge, "paired", true) != "1") {
            http.unpair()
            return PairState.FAILED
        }

        return PairState.PAIRED
    }

    private interface PairingHashAlgorithm {
        fun getHashLength(): Int
        fun hashData(data: ByteArray): ByteArray
    }

    private class Sha1PairingHash : PairingHashAlgorithm {
        override fun getHashLength(): Int = 20
        override fun hashData(data: ByteArray): ByteArray {
            val md = MessageDigest.getInstance("SHA-1")
            return md.digest(data)
        }
    }

    private class Sha256PairingHash : PairingHashAlgorithm {
        override fun getHashLength(): Int = 32
        override fun hashData(data: ByteArray): ByteArray {
            val md = MessageDigest.getInstance("SHA-256")
            return md.digest(data)
        }
    }

    companion object {
        fun generatePinString(): String {
            val r = SecureRandom()
            return String.format(null as Locale?, "%d%d%d%d",
                r.nextInt(10), r.nextInt(10),
                r.nextInt(10), r.nextInt(10))
        }
    }
}

// 🌐 5. Moonlight HTTP/HTTPS Connection Protocol Handler
class NvHTTP(
    private val ip: String,
    private val httpsPort: Int = 47984,
    private val uniqueId: String = "0123456789ABCDEF",
    private var serverCert: X509Certificate? = null,
    private val cryptoProvider: LimelightCryptoProvider = DesktopCryptoProvider()
) {
    private val baseUrlHttp = HttpUrl.Builder().scheme("http").host(ip).port(47989).build()
    private val baseUrlHttps = HttpUrl.Builder().scheme("https").host(ip).port(httpsPort).build()
    
    val pairingManager = PairingManager(this, cryptoProvider)

    private val keyManager = object : X509KeyManager {
        override fun chooseClientAlias(keyTypes: Array<out String>?, issuers: Array<out Principal>?, socket: Socket?): String = "Limelight-RSA"
        override fun chooseServerAlias(keyType: String?, issuers: Array<out Principal>?, socket: Socket?): String? = null
        override fun getCertificateChain(alias: String?): Array<X509Certificate> = arrayOf(cryptoProvider.getClientCertificate())
        override fun getClientAliases(keyType: String?, issuers: Array<out Principal>?): Array<out String>? = null
        override fun getPrivateKey(alias: String?): PrivateKey = cryptoProvider.getClientPrivateKey()
        override fun getServerAliases(keyType: String?, issuers: Array<out Principal>?): Array<out String>? = null
    }

    private val trustManager = object : X509TrustManager {
        override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
        override fun checkClientTrusted(certs: Array<out X509Certificate>?, authType: String?) {}
        override fun checkServerTrusted(certs: Array<out X509Certificate>?, authType: String?) {
            val hostCert = serverCert ?: return
            if (certs != null && certs.isNotEmpty()) {
                if (certs[0].encoded.contentEquals(hostCert.encoded)) {
                    return
                }
                throw CertificateException("Server certificate mismatch")
            }
        }
    }

    private fun getOkHttpClient(enableTls: Boolean): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(3000, TimeUnit.MILLISECONDS)
            .readTimeout(5000, TimeUnit.MILLISECONDS)
            .proxy(Proxy.NO_PROXY)
            
        if (enableTls) {
            val sc = SSLContext.getInstance("TLS")
            sc.init(arrayOf(keyManager), arrayOf(trustManager), SecureRandom())
            builder.sslSocketFactory(sc.socketFactory, trustManager)
            builder.hostnameVerifier { _, _ -> true }
        }
        return builder.build()
    }

    fun setServerCert(cert: X509Certificate) {
        this.serverCert = cert
    }

    fun executePairingCommand(args: String, enableReadTimeout: Boolean): String {
        val url = baseUrlHttp.newBuilder()
            .addPathSegment("pair")
            .query("devicename=roth&updateState=1&$args")
            .addQueryParameter("uniqueid", uniqueId)
            .addQueryParameter("uuid", UUID.randomUUID().toString())
            .build()
            
        val request = Request.Builder().url(url).build()
        getOkHttpClient(false).newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Failed to pairing cmd: ${response.code}")
            return response.body?.string() ?: ""
        }
    }

    fun executePairingChallenge(): String {
        val url = baseUrlHttps.newBuilder()
            .addPathSegment("pair")
            .query("devicename=roth&updateState=1&phrase=pairchallenge")
            .addQueryParameter("uniqueid", uniqueId)
            .addQueryParameter("uuid", UUID.randomUUID().toString())
            .build()
            
        val request = Request.Builder().url(url).build()
        getOkHttpClient(true).newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Failed to pairing challenge: ${response.code}")
            return response.body?.string() ?: ""
        }
    }

    fun unpair() {
        val url = baseUrlHttp.newBuilder().addPathSegment("unpair").build()
        val request = Request.Builder().url(url).build()
        try {
            getOkHttpClient(false).newCall(request).execute().close()
        } catch (_: Exception) {}
    }

    fun getServerInfo(likelyOnline: Boolean): String {
        val url = if (serverCert != null) {
            baseUrlHttps.newBuilder().addPathSegment("serverinfo").build()
        } else {
            baseUrlHttp.newBuilder().addPathSegment("serverinfo").build()
        }
        
        val request = Request.Builder().url(url).build()
        getOkHttpClient(serverCert != null).newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Failed serverinfo: ${response.code}")
            return response.body?.string() ?: ""
        }
    }

    fun getServerMajorVersion(serverInfo: String): Int {
        val version = getXmlString(serverInfo, "appversion", true)
        return version.split(".").firstOrNull()?.toIntOrNull() ?: 1
    }

    fun getAppList(): List<GameInfo> {
        val url = baseUrlHttps.newBuilder()
            .addPathSegment("applist")
            .addQueryParameter("uniqueid", uniqueId)
            .addQueryParameter("uuid", UUID.randomUUID().toString())
            .build()
            
        val request = Request.Builder().url(url).build()
        getOkHttpClient(true).newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Failed applist: ${response.code}")
            val xml = response.body?.string() ?: ""
            return parseAppList(xml)
        }
    }

    private fun parseAppList(xml: String): List<GameInfo> {
        val list = mutableListOf<GameInfo>()
        try {
            val factory = XmlPullParserFactory.newInstance()
            val xpp = factory.newPullParser()
            xpp.setInput(StringReader(xml))
            
            var eventType = xpp.eventType
            val currentTag = Stack<String>()
            var appId = ""
            var appName = ""
            
            while (eventType != org.xmlpull.v1.XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    org.xmlpull.v1.XmlPullParser.START_TAG -> {
                        currentTag.push(xpp.name)
                    }
                    org.xmlpull.v1.XmlPullParser.END_TAG -> {
                        val closed = currentTag.pop()
                        if (closed == "App") {
                            if (appId.isNotEmpty() && appName.isNotEmpty()) {
                                list.add(GameInfo(id = appId.toIntOrNull() ?: 0, name = appName, boxArtUrl = ""))
                            }
                            appId = ""
                            appName = ""
                        }
                    }
                    org.xmlpull.v1.XmlPullParser.TEXT -> {
                        val peek = currentTag.peek()
                        if (peek == "ID") {
                            appId = xpp.text
                        } else if (peek == "AppTitle") {
                            appName = xpp.text
                        }
                    }
                }
                eventType = xpp.next()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    companion object {
        fun getXmlString(xml: String, tag: String, throwIfMissing: Boolean): String {
            val match = Regex("<$tag>(.*?)</$tag>").find(xml)
            val value = match?.groups?.get(1)?.value
            if (value == null && throwIfMissing) {
                throw IOException("Missing field: $tag")
            }
            return value ?: ""
        }
    }
}
