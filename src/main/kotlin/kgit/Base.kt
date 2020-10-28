package kgit

import java.io.File

object Base {

    fun writeTree(directory: String = "."): String {
        val children = File(directory).listFiles()!!
        val entries = children
            .filterNot { it.isIgnored() }
            .map {
                when {
                    it.isDirectory -> {
                        val oid = writeTree(it.absolutePath)
                        "$TYPE_TREE $oid ${it.name}"
                    }
                    else -> {
                        val oid = Data.hashObject(it.readBytes(), TYPE_BLOB)
                        "$TYPE_BLOB $oid ${it.name}"
                    }
                }
            }
        val tree = entries.joinToString(separator = "\n")
        return Data.hashObject(tree.encodeToByteArray(), TYPE_TREE)
    }
}

private fun File.isIgnored(): Boolean = this.path.contains(KGIT_DIR)
