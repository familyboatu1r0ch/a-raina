package avb.desc

import cfig.Helper
import cfig.io.Struct3
import org.junit.Assert
import java.io.InputStream

@ExperimentalUnsignedTypes
class KernelCmdlineDescriptor(
        var flags: UInt = 0U,
        var cmdlineLength: UInt = 0U,
        var cmdline: String = "")
    : Descriptor(TAG, 0U, 0) {
    var flagsInterpretation: String = ""
        get() {
            var ret = ""
            if (this.flags and flagHashTreeEnabled == flagHashTreeEnabled) {
                ret += "$flagHashTreeEnabled: hashTree Enabled"
            } else if (this.flags and flagHashTreeDisabled == flagHashTreeDisabled) {
                ret += "$flagHashTreeDisabled: hashTree Disabled"
            }
            return ret
        }

    @Throws(IllegalArgumentException::class)
    constructor(data: InputStream, seq: Int = 0) : this() {
        val info = Struct3(FORMAT_STRING).unpack(data)
        this.tag = info[0] as ULong
        this.num_bytes_following = info[1] as ULong
        this.flags = info[2] as UInt
        this.cmdlineLength = info[3] as UInt
        this.sequence = seq
        val expectedSize = Helper.round_to_multiple(SIZE.toUInt() - 16U + this.cmdlineLength, 8U)
        if ((this.tag != TAG) || (this.num_bytes_following != expectedSize.toULong())) {
            throw IllegalArgumentException("Given data does not look like a kernel cmdline descriptor")
        }
        this.cmdline = Struct3("${this.cmdlineLength}s").unpack(data)[0] as String
    }

    override fun encode(): ByteArray {
        val num_bytes_following = SIZE - 16 + cmdline.toByteArray().size
        val nbf_with_padding = Helper.round_to_multiple(num_bytes_following.toLong(), 8)
        val padding_size = nbf_with_padding - num_bytes_following
        val desc = Struct3(FORMAT_STRING).pack(
                TAG,
                nbf_with_padding,
                this.flags,
                cmdline.length)
        val padding = Struct3("${padding_size}x").pack(null)
        return Helper.join(desc, cmdline.toByteArray(), padding)
    }

    companion object {
        const val TAG: ULong = 3U
        const val SIZE = 24
        const val FORMAT_STRING = "!2Q2L" //# tag, num_bytes_following (descriptor header), flags, cmdline length (bytes)
        //AVB_KERNEL_CMDLINE_FLAGS_USE_ONLY_IF_HASHTREE_NOT_DISABLED
        const val flagHashTreeEnabled = 1U
        //AVB_KERNEL_CMDLINE_FLAGS_USE_ONLY_IF_HASHTREE_DISABLED
        const val flagHashTreeDisabled = 2U

        init {
            Assert.assertEquals(SIZE, Struct3(FORMAT_STRING).calcSize())
        }
    }
}
