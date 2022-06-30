// Copyright 2021 yuyezhong@gmail.com
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package cfig.bcb

import cc.cfig.io.Struct
import cfig.helper.Helper
import org.slf4j.LoggerFactory
import java.io.FileInputStream

data class VirtualABMsg(
    var version: Int = 0,
    var magic: ByteArray = byteArrayOf(),
    var mergeStatus: Int = 0,
    var sourceSlot: Int = 0,
    var reserved: ByteArray = byteArrayOf()
) {
    companion object {
        private const val FORMAT_STRING = "b4bbb57b"
        const val SIZE = 64
        private val log = LoggerFactory.getLogger("VirtualABMsg")
        private const val MAGIC = "b00a7456"

        init {
            assert(SIZE == Struct(FORMAT_STRING).calcSize())
        }
    }

    constructor(fis: FileInputStream) : this() {
        val info = Struct(FORMAT_STRING).unpack(fis)
        this.version = (info[0] as ByteArray)[0].toInt()
        this.magic = info[1] as ByteArray
        this.mergeStatus = (info[2] as ByteArray)[0].toInt()
        this.sourceSlot = (info[3] as ByteArray)[0].toInt()
        this.reserved = info[4] as ByteArray
        if (MAGIC != Helper.Companion.toHexString(this.magic)) {
            throw IllegalArgumentException("stream is not VirtualAB message")
        }
    }

    fun encode(): ByteArray {
        return Struct(FORMAT_STRING).pack(
            this.version,
            this.magic,
            this.mergeStatus,
            this.sourceSlot,
            0
        )
    }

    override fun toString(): String {
        return "VABMsg(v=$version, magic=${Helper.toHexString(magic)}, mergeStatus=$mergeStatus:${MergeStatus.values().get(this.mergeStatus)}, sourceSlot=$sourceSlot)"
    }


    enum class MergeStatus(val status: Int) {
        NONE(0),
        UNKNOWN(1),
        SNAPSHOTTED(2),
        MERGING(3),
        CANCELLED(4)
    }
}
