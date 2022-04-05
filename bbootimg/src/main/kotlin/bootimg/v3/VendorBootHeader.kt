package cfig.bootimg.v3

import cfig.io.Struct3
import org.slf4j.LoggerFactory
import java.io.InputStream

@OptIn(ExperimentalUnsignedTypes::class)
class VendorBootHeader(
    var headerVersion: Int = 0,
    var pageSize: Int = 0,
    var kernelLoadAddr: Long = 0,
    var ramdiskLoadAddr: Long = 0,
    var vndRamdiskSize: Int = 0,
    var cmdline: String = "",
    var tagsLoadAddr: Long = 0,
    var product: String = "",
    var headerSize: Int = 0,
    var dtbSize: Int = 0,
    var dtbLoadAddr: Long = 0,
    var vrtSize: Int = 0,
    var vrtEntryNum: Int = 0,
    var vrtEntrySize: Int = 0,
    var bootconfigSize: Int = 0
) {
    @Throws(IllegalArgumentException::class)
    constructor(iS: InputStream?) : this() {
        if (iS == null) {
            return
        }
        log.warn("VendorBootHeader constructor")
        val info = Struct3(FORMAT_STRING).unpack(iS)
        assert(16 == info.size)
        if (info[0] != magic) {
            throw IllegalArgumentException("stream doesn't look like Android Vendor Boot Image")
        }
        this.headerVersion = (info[1] as UInt).toInt()
        this.pageSize = (info[2] as UInt).toInt()
        this.kernelLoadAddr = (info[3] as UInt).toLong()
        this.ramdiskLoadAddr = (info[4] as UInt).toLong()
        this.vndRamdiskSize = (info[5] as UInt).toInt()
        this.cmdline = info[6] as String
        this.tagsLoadAddr = (info[7] as UInt).toLong()
        this.product = info[8] as String
        this.headerSize = (info[9] as UInt).toInt()
        this.dtbSize = (info[10] as UInt).toInt()
        this.dtbLoadAddr = (info[11] as ULong).toLong()
        this.vrtSize = (info[12] as UInt).toInt()
        this.vrtEntryNum = (info[13] as UInt).toInt()
        this.vrtEntrySize = (info[14] as UInt).toInt()
        this.bootconfigSize = (info[15] as UInt).toInt()

        if (this.headerSize !in arrayOf(VENDOR_BOOT_IMAGE_HEADER_V3_SIZE, VENDOR_BOOT_IMAGE_HEADER_V4_SIZE)) {
            throw IllegalArgumentException("header size " + this.headerSize + " invalid")
        }
        if (this.headerVersion !in 3..4) {
            throw IllegalArgumentException("header version " + this.headerVersion + " invalid")
        }
    }

    fun encode(): ByteArray {
        return Struct3(FORMAT_STRING).pack(
            magic,
            headerVersion,
            pageSize,
            kernelLoadAddr,
            ramdiskLoadAddr,
            vndRamdiskSize,
            cmdline,
            tagsLoadAddr,
            product,
            headerSize,
            dtbSize,
            dtbLoadAddr,
            vrtSize,
            vrtEntryNum,
            vrtEntrySize,
            bootconfigSize
        )
    }

    companion object {
        private val log = LoggerFactory.getLogger(VendorBootHeader::class.java)
        const val magic = "VNDRBOOT"
        const val VENDOR_BOOT_IMAGE_HEADER_V3_SIZE = 2112
        const val VENDOR_BOOT_IMAGE_HEADER_V4_SIZE = 2128
        const val FORMAT_STRING = "8s" + //magic
                "I" + //header version
                "I" + //page size
                "I" + //kernel physical load addr
                "I" + //ramdisk physical load addr
                "I" + //vendor ramdisk size
                "2048s" + //cmdline
                "I" + //kernel tag load addr
                "16s" + //product name
                "I" + //header size
                "I" + //dtb size
                "Q" + //dtb physical load addr
                "I" + //[v4] vendor ramdisk table size
                "I" + //[v4] vendor ramdisk table entry num
                "I" + //[v4] vendor ramdisk table entry size
                "I"   //[v4] bootconfig size

        init {
            assert(Struct3(FORMAT_STRING).calcSize() == VENDOR_BOOT_IMAGE_HEADER_V4_SIZE)
        }
    }

    override fun toString(): String {
        return "VendorBootHeader(headerVersion=$headerVersion, pageSize=$pageSize, kernelLoadAddr=$kernelLoadAddr, ramdiskLoadAddr=$ramdiskLoadAddr, vndRamdiskSize=$vndRamdiskSize, cmdline='$cmdline', tagsLoadAddr=$tagsLoadAddr, product='$product', headerSize=$headerSize, dtbSize=$dtbSize, dtbLoadAddr=$dtbLoadAddr, vrtSize=$vrtSize, vrtEntryNum=$vrtEntryNum, vrtEntrySize=$vrtEntrySize, bootconfigSize=$bootconfigSize)"
    }


}
