package avb.desc

import avb.blob.Header
import cfig.Helper
import cfig.io.Struct3
import org.apache.commons.codec.binary.Hex
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.security.MessageDigest

@ExperimentalUnsignedTypes
class HashDescriptor(var flags: UInt = 0U,
                     var partition_name: String = "",
                     var hash_algorithm: String = "",
                     var image_size: ULong = 0U,
                     var salt: ByteArray = byteArrayOf(),
                     var digest: ByteArray = byteArrayOf(),
                     var partition_name_len: UInt = 0U,
                     var salt_len: UInt = 0U,
                     var digest_len: UInt = 0U)
    : Descriptor(TAG, 0U, 0) {
    var flagsInterpretation: String = ""
        get() {
            var ret = ""
            if (this.flags and Header.HashDescriptorFlags.AVB_HASH_DESCRIPTOR_FLAGS_DO_NOT_USE_AB.inFlags.toUInt() == 1U) {
                ret += "1:no-A/B system"
            } else {
                ret += "0:A/B system"
            }
            return ret
        }

    constructor(data: InputStream, seq: Int = 0) : this() {
        val info = Struct3(FORMAT_STRING).unpack(data)
        this.tag = info[0] as ULong
        this.num_bytes_following = info[1] as ULong
        this.image_size = info[2] as ULong
        this.hash_algorithm = info[3] as String
        this.partition_name_len = info[4] as UInt
        this.salt_len = info[5] as UInt
        this.digest_len = info[6] as UInt
        this.flags = info[7] as UInt
        this.sequence = seq
        val expectedSize = Helper.round_to_multiple(
                SIZE - 16 + (partition_name_len + salt_len + digest_len).toLong(), 8).toULong()
        if (this.tag != TAG || expectedSize != this.num_bytes_following) {
            throw IllegalArgumentException("Given data does not look like a |hash| descriptor")
        }
        val payload = Struct3("${this.partition_name_len}s${this.salt_len}b${this.digest_len}b").unpack(data)
        assert(3 == payload.size)
        this.partition_name = payload[0] as String
        this.salt = payload[1] as ByteArray
        this.digest = payload[2] as ByteArray
    }

    override fun encode(): ByteArray {
        val payload_bytes_following = SIZE + this.partition_name.length + this.salt.size + this.digest.size - 16L
        this.num_bytes_following = Helper.round_to_multiple(payload_bytes_following, 8).toULong()
        val padding_size = num_bytes_following - payload_bytes_following.toUInt()
        val desc = Struct3(FORMAT_STRING).pack(
                TAG,
                this.num_bytes_following,
                this.image_size,
                this.hash_algorithm,
                this.partition_name.length,
                this.salt.size,
                this.digest.size,
                this.flags,
                null)
        val padding = Struct3("${padding_size}x").pack(null)
        return Helper.join(desc, partition_name.toByteArray(), this.salt, this.digest, padding)
    }

    fun verify(image_file: String) {
        val hasher = MessageDigest.getInstance(Helper.pyAlg2java(hash_algorithm))
        hasher.update(this.salt)
        hasher.update(File(image_file).readBytes())
        val digest = hasher.digest()
    }

    fun update(image_file: String, use_persistent_digest: Boolean = false): HashDescriptor {
        //salt
        if (this.salt.isEmpty()) {
            //If salt is not explicitly specified, choose a hash that's the same size as the hash size
            val expectedDigestSize = MessageDigest.getInstance(Helper.pyAlg2java(hash_algorithm)).digest().size
            FileInputStream(File("/dev/urandom")).use {
                val randomSalt = ByteArray(expectedDigestSize)
                it.read(randomSalt)
                log.warn("salt is empty, using random salt[$expectedDigestSize]: " + Helper.toHexString(randomSalt))
                this.salt = randomSalt
            }
        } else {
            log.info("preset salt[${this.salt.size}] is valid: ${Hex.encodeHexString(this.salt)}")
        }

        //size
        this.image_size = File(image_file).length().toULong()

        //flags
        if (this.flags and 1U == 1U) {
            log.info("flag: use_ab = 0")
        } else {
            log.info("flag: use_ab = 1")
        }

        if (!use_persistent_digest) {
            //hash digest
            val newDigest = MessageDigest.getInstance(Helper.pyAlg2java(hash_algorithm)).apply {
                update(salt)
                update(File(image_file).readBytes())
            }.digest()
            log.info("Digest(salt + file): " + Helper.toHexString(newDigest))
            this.digest = newDigest
        }

        return this
    }

    companion object {
        const val TAG: ULong = 2U
        private const val RESERVED = 60
        private const val SIZE = 72 + RESERVED
        private const val FORMAT_STRING = "!3Q32s4L${RESERVED}x"
        private val log = LoggerFactory.getLogger(HashDescriptor::class.java)
    }

    override fun toString(): String {
        return "HashDescriptor(TAG=$TAG, image_size=$image_size, hash_algorithm=$hash_algorithm, flags=$flags, partition_name='$partition_name', salt=${Helper.toHexString(salt)}, digest=${Helper.toHexString(digest)})"
    }
}
