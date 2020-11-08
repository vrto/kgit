package kgit.base

import kgit.data.*
import java.io.File

class KGit(private val objectDb: ObjectDatabase) {

    fun writeTree(directory: String = "."): Oid {
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
                        val oid = objectDb.hashObject(it.readBytes(), TYPE_BLOB)
                        Tree.Entry(TYPE_BLOB, oid, it.name)
                    }
                }
            }.toTree()

        val rawBytes = tree.toString().encodeToByteArray()
        return objectDb.hashObject(rawBytes, TYPE_TREE)
    }

    fun readTree(treeOid: Oid, basePath: String = "./") {
        File(basePath).emptyDir()
        val tree = getTree(treeOid).parseState(basePath, ::getTree)
        tree.forEach {
            val obj = objectDb.getObject(it.oid, expectedType = TYPE_BLOB)
            File(it.path).apply {
                createNewFileWithinHierarchy()
                writeText(obj)
            }
        }
    }

    internal fun getTree(oid: Oid): Tree {
        val rawTree = objectDb.getObject(oid, expectedType = TYPE_TREE)
        val lines = rawTree.split("\n")
        return lines.map {
            val parts = it.split(" ")
            require(parts.size == 3)
            Tree.Entry(type = parts[0], oid = Oid(parts[1]), name = parts[2])
        }.toTree()
    }

    fun commit(message: String, directory: String = "."): Oid {
        val treeOid = writeTree(directory)
        val commit = Commit(treeOid, message)
        return objectDb.hashObject(commit.toString().encodeToByteArray(), TYPE_COMMIT)
    }
}