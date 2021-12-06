package avb.desc

abstract class Descriptor(var tag: ULong, var num_bytes_following: ULong, var sequence: Int = 0) {
    abstract fun encode(): ByteArray
}