package kgit

import java.io.File
import java.io.IOException

object Base {

    fun writeTree(directory: String = "."): String {
        val children = File(directory).listFiles()!!
        val tree = children
            .filterNot { it.isIgnored() }
            .map {
                when {
                    it.isDirectory -> {
                        val oid = writeTree(it.absolutePath)
                        Tree.Entry(TYPE_TREE, oid, it.name)
                    }
                    else -> {
                        val oid = Data.hashObject(it.readBytes(), TYPE_BLOB)
                        Tree.Entry(TYPE_BLOB, oid, it.name)
                    }
                }
            }.toTree()

        val rawBytes = tree.toString().encodeToByteArray()
        return Data.hashObject(rawBytes, TYPE_TREE)
    }

    fun readTree(treeOid: String, basePath: String = "./") {
        File(basePath).emptyDir()
        val tree = getTree(treeOid).parseState(basePath)
        tree.forEach {
            val obj = Data.getObject(it.oid, expectedType = TYPE_BLOB)
            File(it.path).apply {
                createNewFileWithinHierarchy()
                writeText(obj)
            }
        }
    }

    internal fun getTree(oid: String): Tree {
        val rawTree = Data.getObject(oid, expectedType = TYPE_TREE)
        val lines = rawTree.split("\n")
        return lines.map {
            val parts = it.split(" ")
            require(parts.size == 3)
            Tree.Entry(type = parts[0], oid = parts[1], name = parts[2])
        }.toTree()
    }

    //TODO ditch stringly typed OIDs
    fun commit(message: String, directory: String = "."): String {
        val treeOid = writeTree(directory)
        val commit = Commit(treeOid, message)
        return Data.hashObject(commit.toString().encodeToByteArray(), TYPE_COMMIT)
    }
}

// TODO move tree to a separate file
class Tree(private val entries: List<Entry>) : Iterable<Tree.Entry> {

    class Entry(val type: String, val oid: String, val name: String) {
        init {
            require(!name.contains('/'))
            require(name !in (listOf(".", "..")))
        }

        override fun toString() = "$type $oid $name"
    }

    class FileState(val path: String, val oid: String)

    fun parseState(basePath: String): List<FileState> = entries.map { entry ->
        val path = basePath + entry.name
        when (entry.type) {
            TYPE_BLOB -> listOf(FileState(path, entry.oid))
            TYPE_TREE -> Base.getTree(entry.oid).parseState("$path/")
            else -> throw IllegalStateException("Unknown object type ${entry.type}")
        }
    }.flatten()

    override fun toString() = entries.joinToString(separator = "\n") { it.toString() }

    override fun iterator() = entries.iterator()
}

fun List<Tree.Entry>.toTree() = Tree(this)

//TODO separate file
data class Commit(val treeOid: String, val message: String) {
    override fun toString() =
        """tree $treeOid
        |
        |$message
        """.trimMargin()
}

//TODO Separate to file extensions
private fun File.isIgnored(): Boolean = this.path.contains(KGIT_DIR)

private fun File.createNewFileWithinHierarchy() =
    try {
        createNewFile()
    } catch (e: IOException) {
        parentFile.mkdirs()
        createNewFile()
    }

private fun File.emptyDir() {
    // delete all files
    this.walk()
        .filter { !it.isIgnored() }
        .filter { it.isFile }
        .forEach { it.delete() }

    // clean up dirs & subdirs
    this.walk()
        .filter { !it.isIgnored() }
        .filter { it.isDirectory }
        .filter { it.path != this.path } // no self-delete
        .forEach { it.delete() }
}