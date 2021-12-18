package cfig.packable

import cfig.Avb

@ExperimentalUnsignedTypes
class VBMetaParser: IPackable {
    override fun capabilities(): List<String> {
        return listOf("^vbmeta\\.img$", "^vbmeta\\_[a-z]+.img$")
    }

    override fun unpack(fileName: String) {
        Avb().parseVbMeta(fileName)
    }

    override fun pack(fileName: String) {
        Avb().packVbMetaWithPadding(fileName)
    }
}
