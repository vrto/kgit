package kgit.base

import kgit.data.Oid
import kgit.data.TYPE_BLOB
import kgit.data.TYPE_TREE

class Tree(private val entries: List<Entry>) : Iterable<Tree.Entry> {

    class Entry(val type: String, val oid: Oid, val name: String) {
        init {
            require(!name.contains('/'))
            require(name !in (listOf(".", "..")))
        }

        override fun toString() = "$type $oid $name"
    }

    data class FileState(val path: String, val oid: Oid)

    fun parseState(basePath: String, treeLoader: (Oid) -> Tree): List<FileState> = entries.map { entry ->
        val path = basePath + entry.name
        when (entry.type) {
            TYPE_BLOB -> listOf(FileState(path, entry.oid))
            TYPE_TREE -> treeLoader(entry.oid).parseState("$path/", treeLoader)
            else -> throw IllegalStateException("Unknown object type ${entry.type}")
        }
    }.flatten()

    override fun toString() = entries.joinToString(separator = "\n") { it.toString() }

    override fun iterator() = entries.iterator()
}

fun List<Tree.Entry>.toTree() = Tree(this)