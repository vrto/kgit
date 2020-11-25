package kgit.base

import kgit.data.*
import java.io.File

class KGit(private val objectDb: ObjectDatabase) {

    fun writeTree(directory: String = objectDb.workDir): Oid {
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
            require(parts.size == 3) { "Expected three lines: type, oid, name" }
            Tree.Entry(type = parts[0], oid = Oid(parts[1]), name = parts[2])
        }.toTree()
    }

    fun commit(message: String): Oid {
        val treeOid = writeTree()
        val parent = objectDb.getHead()?.oidOrNull
        val commit = Commit(treeOid, parent, message)
        return objectDb.hashObject(commit.toString().encodeToByteArray(), TYPE_COMMIT).also {
            objectDb.setHead(it.toDirectRef())
        }
    }

    fun getCommit(oid: Oid): Commit {
        val raw = objectDb.getObject(oid, TYPE_COMMIT)
        val lines = raw.split("\n")
            .filter { it.isNotEmpty() } // empty lines are just readability

        val treeOid = lines.first { it.startsWith("tree") }.drop("tree ".length).toOid()
        val parentOid = lines.find { it.startsWith("parent") }?.drop("parent ".length)?.toOid()
        val msg = lines.last()

        return Commit(treeOid, parentOid, msg)
    }

    fun checkout(oid: Oid) {
        val commit = getCommit(oid)
        readTree(commit.treeOid, "${objectDb.workDir}/")
        objectDb.setHead(oid.toDirectRef())
    }

    fun tag(tagName: String, oid: Oid) {
        objectDb.updateRef("refs/tags/$tagName", oid.toDirectRef())
    }

    fun getOid(name: String): Oid {
        val locationsToTry = listOf(name.unaliasHead(), "refs/$name", "refs/tags/$name", "refs/heads/$name")
        return locationsToTry
            .mapNotNull { objectDb.getRef(refName = it, deref = false)?.oidOrNull }
            .firstOrNull()
            ?: name.toOid()
    }

    private fun String.unaliasHead() = when(this) {
        "@" -> "HEAD"
        else -> this
    }

    fun listCommitsAndParents(refOids: List<Oid>): List<Oid> {
        val oids: MutableList<Oid?> = refOids.toMutableList()
        val visited = linkedSetOf<Oid>() // preserving order of inserts

        while (oids.isNotEmpty()) {
            val oid = oids.removeFirstOrNull()
            if (oid == null || oid in visited)
                continue
            visited.add(oid)

            val commit = getCommit(oid)
            oids.add(0, commit.parentOid)
        }

        return visited.toList()
    }

    fun createBranch(name: String, startPoint: Oid) {
        objectDb.updateRef("refs/heads/$name", startPoint.toDirectRef())
    }
}