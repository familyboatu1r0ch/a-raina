package avb

import avb.blob.Header
import org.apache.commons.codec.binary.Hex
import org.junit.Test
import java.io.ByteArrayInputStream

@OptIn(ExperimentalUnsignedTypes::class)
class HeaderTest {

    @Test
    fun readHeader() {
        val vbmetaHeaderStr = "4156423000000001000000000000000000000000000000000000010000000000000000000000000000000000000000000000000000000000000000000000000000000000000000c8000000000000000000000000000000c80000000000000000000000000000000000000000000000c800000000000000000000000000000000617662746f6f6c20312e312e3000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000"
        val header2 = Header(ByteArrayInputStream(Hex.decodeHex(vbmetaHeaderStr)))
        println(header2.toString())
    }
}
