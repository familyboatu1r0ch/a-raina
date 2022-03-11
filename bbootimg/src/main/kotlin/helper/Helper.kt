package cfig.helper

import cfig.io.Struct3
import com.google.common.math.BigIntegerMath
import org.apache.commons.codec.binary.Hex
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream
import org.apache.commons.compress.compressors.gzip.GzipParameters
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream
import org.apache.commons.exec.CommandLine
import org.apache.commons.exec.DefaultExecutor
import org.apache.commons.exec.ExecuteException
import org.apache.commons.exec.PumpStreamHandler
import org.slf4j.LoggerFactory
import java.io.*
import java.math.BigInteger
import java.math.RoundingMode
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.attribute.PosixFilePermission
import java.security.MessageDigest
import java.util.*
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import java.util.zip.ZipException
import javax.crypto.Cipher

@OptIn(ExperimentalUnsignedTypes::class)
class Helper {
    companion object {
        private val gcfg: Properties = Properties().apply {
            load(Helper::class.java.classLoader.getResourceAsStream("general.cfg"))
        }

        fun prop(k: String): String {
            return gcfg.getProperty(k)
        }

        fun joinWithNulls(vararg source: ByteArray?): ByteArray {
            val baos = ByteArrayOutputStream()
            for (src in source) {
                src?.let {
                    if (src.isNotEmpty()) baos.write(src)
                }
            }
            return baos.toByteArray()
        }

        fun ByteArray.paddingWith(pageSize: UInt, paddingHead: Boolean = false): ByteArray {
            val paddingNeeded = round_to_multiple(this.size.toUInt(), pageSize) - this.size.toUInt()
            return if (paddingNeeded > 0u) {
                if (paddingHead) {
                    join(Struct3("${paddingNeeded}x").pack(null), this)
                } else {
                    join(this, Struct3("${paddingNeeded}x").pack(null))
                }
            } else {
                this
            }
        }

        fun join(vararg source: ByteArray): ByteArray {
            val baos = ByteArrayOutputStream()
            for (src in source) {
                if (src.isNotEmpty()) baos.write(src)
            }
            return baos.toByteArray()
        }

        fun toHexString(inData: UByteArray): String {
            val sb = StringBuilder()
            for (i in inData.indices) {
                sb.append(Integer.toString((inData[i].toInt().and(0xff)) + 0x100, 16).substring(1))
            }
            return sb.toString()
        }

        fun toHexString(inData: ByteArray): String {
            val sb = StringBuilder()
            for (i in inData.indices) {
                sb.append(Integer.toString((inData[i].toInt().and(0xff)) + 0x100, 16).substring(1))
            }
            return sb.toString()
        }

        fun fromHexString(s: String): ByteArray {
            val len = s.length
            val data = ByteArray(len / 2)
            var i = 0
            while (i < len) {
                data[i / 2] = ((Character.digit(s[i], 16) shl 4) + Character.digit(s[i + 1], 16)).toByte()
                i += 2
            }
            return data
        }

        fun extractFile(fileName: String, outImgName: String, offset: Long, length: Int) {
            if (0 == length) {
                return
            }
            RandomAccessFile(fileName, "r").use { inRaf ->
                RandomAccessFile(outImgName, "rw").use { outRaf ->
                    inRaf.seek(offset)
                    val data = ByteArray(length)
                    assert(length == inRaf.read(data))
                    outRaf.write(data)
                }
            }
        }

        fun round_to_multiple(size: UInt, page: UInt): UInt {
            val remainder = size % page
            return if (remainder == 0U) {
                size
            } else {
                size + page - remainder
            }
        }

        fun round_to_multiple(size: Long, page: Int): Long {
            val remainder = size % page
            return if (remainder == 0L) {
                size
            } else {
                size + page - remainder
            }
        }

        fun round_to_multiple(size: Int, page: Int): Int {
            val remainder = size % page
            return if (remainder == 0) {
                size
            } else {
                size + page - remainder
            }
        }

        fun pyAlg2java(alg: String): String {
            return when (alg) {
                "sha1" -> "sha-1"
                "sha224" -> "sha-224"
                "sha256" -> "sha-256"
                "sha384" -> "sha-384"
                "sha512" -> "sha-512"
                else -> throw IllegalArgumentException("unknown algorithm: [$alg]")
            }
        }

        fun dumpToFile(dumpFile: String, data: ByteArray) {
            log.info("Dumping data to $dumpFile ...")
            FileOutputStream(dumpFile, false).use { fos ->
                fos.write(data)
            }
            log.info("Dumping data to $dumpFile done")
        }

        fun String.deleteIfExists() {
            if (File(this).exists()) {
                log.info("deleting $this")
                File(this).delete()
            }
        }

        fun String.check_call(inWorkdir: String? = null): Boolean {
            val ret: Boolean
            try {
                val cmd = CommandLine.parse(this)
                log.run {
                    info("CMD: $cmd, workDir: $inWorkdir")
                }
                val exec = DefaultExecutor()
                inWorkdir?.let { exec.workingDirectory = File(it) }
                exec.execute(cmd)
                ret = true
            } catch (e: java.lang.IllegalArgumentException) {
                log.error("$e: can not parse command: [$this]")
                throw e
            } catch (e: ExecuteException) {
                log.error("$e: can not exec command")
                throw e
            } catch (e: IOException) {
                log.error("$e: can not exec command")
                throw e
            }
            return ret
        }

        fun String.check_output(): String {
            val outputStream = ByteArrayOutputStream()
            log.info(this)
            DefaultExecutor().let {
                it.streamHandler = PumpStreamHandler(outputStream)
                it.execute(CommandLine.parse(this))
            }
            log.info(outputStream.toString().trim())
            return outputStream.toString().trim()
        }

        fun String.pumpRun(): Array<ByteArrayOutputStream> {
            val outStream = ByteArrayOutputStream()
            val errStream = ByteArrayOutputStream()
            log.info("CMD: $this")
            DefaultExecutor().let {
                it.streamHandler = PumpStreamHandler(outStream, errStream)
                it.execute(CommandLine.parse(this))
            }
            log.info("stdout [$outStream]")
            log.info("stderr [$errStream]")
            return arrayOf(outStream, errStream)
        }

        fun powerRun3(cmdline: CommandLine, inputStream: InputStream?): Array<Any> {
            var ret = true
            val outStream = ByteArrayOutputStream()
            val errStream = ByteArrayOutputStream()
            log.info("CMD: $cmdline")
            try {
                DefaultExecutor().let {
                    it.streamHandler = PumpStreamHandler(outStream, errStream, inputStream)
                    it.execute(cmdline)
                }
            } catch (e: ExecuteException) {
                log.error("fail to execute [${cmdline}]")
                ret = false
            }
            log.debug("stdout [$outStream]")
            log.debug("stderr [$errStream]")
            return arrayOf(ret, outStream.toByteArray(), errStream.toByteArray())
        }

        fun powerRun2(cmd: String, inputStream: InputStream?): Array<Any> {
            var ret = true
            val outStream = ByteArrayOutputStream()
            val errStream = ByteArrayOutputStream()
            log.info("CMD: $cmd")
            try {
                DefaultExecutor().let {
                    it.streamHandler = PumpStreamHandler(outStream, errStream, inputStream)
                    it.execute(CommandLine.parse(cmd))
                }
            } catch (e: ExecuteException) {
                log.error("fail to execute [$cmd]")
                ret = false
            }
            log.debug("stdout [$outStream]")
            log.debug("stderr [$errStream]")
            return arrayOf(ret, outStream.toByteArray(), errStream.toByteArray())
        }

        fun powerRun(cmd: String, inputStream: InputStream?): Array<ByteArray> {
            val outStream = ByteArrayOutputStream()
            val errStream = ByteArrayOutputStream()
            log.info("CMD: $cmd")
            try {
                DefaultExecutor().let {
                    it.streamHandler = PumpStreamHandler(outStream, errStream, inputStream)
                    it.execute(CommandLine.parse(cmd))
                }
            } catch (e: ExecuteException) {
                log.error("fail to execute [$cmd]")
            }
            log.debug("stdout [$outStream]")
            log.debug("stderr [$errStream]")
            return arrayOf(outStream.toByteArray(), errStream.toByteArray())
        }

        fun hashFileAndSize(vararg inFiles: String?): ByteArray {
            val md = MessageDigest.getInstance("SHA1")
            for (item in inFiles) {
                if (null == item) {
                    md.update(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN)
                            .putInt(0)
                            .array())
                    log.debug("update null $item: " + toHexString((md.clone() as MessageDigest).digest()))
                } else {
                    val currentFile = File(item)
                    FileInputStream(currentFile).use { iS ->
                        var byteRead: Int
                        val dataRead = ByteArray(1024)
                        while (true) {
                            byteRead = iS.read(dataRead)
                            if (-1 == byteRead) {
                                break
                            }
                            md.update(dataRead, 0, byteRead)
                        }
                        log.debug("update file $item: " + toHexString((md.clone() as MessageDigest).digest()))
                        md.update(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN)
                                .putInt(currentFile.length().toInt())
                                .array())
                        log.debug("update SIZE $item: " + toHexString((md.clone() as MessageDigest).digest()))
                    }
                }
            }

            return md.digest()
        }

        fun assertFileEquals(file1: String, file2: String) {
            val hash1 = hashFileAndSize(file1)
            val hash2 = hashFileAndSize(file2)
            log.info("$file1 hash ${toHexString(hash1)}, $file2 hash ${toHexString(hash2)}")
            if (hash1.contentEquals(hash2)) {
                log.info("Hash verification passed: ${toHexString(hash1)}")
            } else {
                log.error("Hash verification failed")
                throw UnknownError("Do not know why hash verification fails, maybe a bug")
            }
        }

        fun modeToPermissions(inMode: Int): Set<PosixFilePermission> {
            var mode = inMode
            val PERMISSIONS_MASK = 4095
            // setgid/setuid/sticky are not supported.
            val MAX_SUPPORTED_MODE = 511
            mode = mode and PERMISSIONS_MASK
            if (mode and MAX_SUPPORTED_MODE != mode) {
                throw IOException("Invalid mode: $mode")
            }
            val allPermissions = PosixFilePermission.values()
            val result: MutableSet<PosixFilePermission> = EnumSet.noneOf(PosixFilePermission::class.java)
            for (i in allPermissions.indices) {
                if (mode and 1 == 1) {
                    result.add(allPermissions[allPermissions.size - i - 1])
                }
                mode = mode shr 1
            }
            return result
        }

        private val log = LoggerFactory.getLogger("Helper")
    }
}
