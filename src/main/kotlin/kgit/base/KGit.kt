package kgit.base

import kgit.base.Tree.FileState
import kgit.data.*
import kgit.diff.ComparableTree
import kgit.diff.Diff
import java.io.File
import java.util.*

class KGit(private val data: ObjectDatabase, private val diff: Diff) {

    fun init() {
        data.init()
        data.updateRef("HEAD", RefValue(symbolic = true, value = "refs/heads/master"))
    }

    fun writeTree(directory: String = data.workDir): Oid {
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
                        val oid = data.hashObject(it.readBytes(), TYPE_BLOB)
                        Tree.Entry(TYPE_BLOB, oid, it.name)
                    }
                }
            }.toTree()

        val rawBytes = tree.toString().encodeToByteArray()
        return data.hashObject(rawBytes, TYPE_TREE)
    }

    fun readTree(treeOid: Oid, basePath: String = "./") {
        File(basePath).emptyDir()
        val tree = getTree(treeOid).parseState(basePath, ::getTree)
        tree.forEach {
            val obj = data.getObject(it.oid, expectedType = TYPE_BLOB)
            File(it.path).apply {
                createNewFileWithinHierarchy()
                writeText(obj)
            }
        }
    }

    private fun readTreeMerged(headTree: Oid, otherTree: Oid) {
        File(data.workDir).emptyDir()
        diff.mergeTrees(getComparableTree(headTree), getComparableTree(otherTree)).forEach { (path, content) ->
            File(path).apply {
                createNewFileWithinHierarchy()
                writeText(content)
            }
        }
    }

    internal fun getTree(oid: Oid): Tree {
        val rawTree = data.getObject(oid, expectedType = TYPE_TREE)
        val lines = rawTree.split("\n")
        return lines.map {
            val parts = it.split(" ")
            require(parts.size == 3) { "Expected three lines: type, oid, name" }
            Tree.Entry(type = parts[0], oid = Oid(parts[1]), name = parts[2])
        }.toTree()
    }

    internal fun getComparableTree(oid: Oid): ComparableTree = getTree(oid).parseState("${data.workDir}/", this::getTree)

    fun getWorkingTree(): List<FileState> = File(data.workDir)
        .walk()
        .filterNot { it.isDirectory }
        .filterNot { it.isIgnored() }
        .map {
            FileState(it.path, data.hashObject(it.readBytes(), TYPE_BLOB))
        }
        .toList()

    fun commit(message: String): Oid {
        val treeOid = writeTree()
        val parents = mutableListOf<Oid>().apply {
            data.getHead().oidOrNull?.let { add(it) }
            data.getMergeHead()?.let {
                add(it.oid)
                data.deleteRef("MERGE_HEAD", deref = false)
            }
        }
        val commit = Commit(treeOid, parents, message)
        return data.hashObject(commit.toString().encodeToByteArray(), TYPE_COMMIT).also {
            data.setHead(it.toDirectRef())
        }
    }

    fun getCommit(oid: Oid): Commit {
        val raw = data.getObject(oid, TYPE_COMMIT)
        val lines = raw.split("\n")
            .filter { it.isNotEmpty() } // empty lines are just readability

        val treeOid = lines.first { it.startsWith("tree") }.drop("tree ".length).toOid()
        val parentOids = lines
            .filter { it.startsWith("parent") }
            .map { it.drop("parent ".length).toOid() }
        val msg = lines.last()

        return Commit(treeOid, parentOids, msg)
    }

    fun checkout(name: String) {
        val oid = getOid(name)
        val commit = getCommit(oid)
        readTree(commit.treeOid, "${data.workDir}/")

        if (name.isBranch()) {
            data.setHead(RefValue(symbolic = true, value = "refs/heads/$name"), deref = false)
        } else {
            data.setHead(oid.toDirectRef(), deref = false)
        }
    }

    fun tag(tagName: String, oid: Oid) {
        data.updateRef("refs/tags/$tagName", oid.toDirectRef())
    }

    fun getOid(name: String): Oid {
        val locationsToTry = listOf(name.unaliasHead(), "refs/$name", "refs/tags/$name", "refs/heads/$name")
        return locationsToTry
            .mapNotNull { data.getRef(refName = it, deref = true).oidOrNull }
            .firstOrNull()
            ?: name.toOid()
    }

    private fun String.unaliasHead() = when(this) {
        "@" -> "HEAD"
        else -> this
    }

    fun listCommitsAndParents(refOids: List<Oid>): List<Oid> {
        val oids: LinkedList<Oid> = LinkedList(refOids)
        val visited = linkedSetOf<Oid>() // preserving order of inserts

        while (oids.isNotEmpty()) {
            val oid = oids.removeFirstOrNull()
            if (oid == null || oid in visited)
                continue
            visited.add(oid)

            val commit = getCommit(oid)

            // first parent next
            commit.parentOids
                .takeIf { it.isNotEmpty() }
                ?.first()
                .let { oids.push(it) }

            // other parents later
            commit.parentOids
                .takeIf { it.size > 1 }
                ?.drop(1)
                ?.let { oids.addAll(it) }
        }

        return visited.toList()
    }

    fun createBranch(name: String, startPoint: Oid) {
        data.updateRef("refs/heads/$name", startPoint.toDirectRef())
    }

    fun getBranchName(): String? {
        val head = data.getRef("HEAD", deref = false)
        return when {
            head.symbolic -> head.value.drop("ref: refs/heads/".length)
            else -> null
        }
    }

    fun listBranches(): List<String> = data.iterateRefs()
        .map { it.name }
        .filter { it.startsWith("heads/") }
        .map { it.drop("heads/".length) }

    private fun String.isBranch() = data.getRef("refs/heads/$this").oidOrNull != null

    fun reset(oid: Oid) {
        data.setHead(oid.toDirectRef())
    }

    fun merge(other: Oid) {
        val headCommit = getCommit(data.getHead().oid)
        val otherCommit = getCommit(other)
        data.updateRef("MERGE_HEAD", RefValue(symbolic = false, value = other.value))
        readTreeMerged(headCommit.treeOid, otherCommit.treeOid)
    }

    fun mergeBase(oid1: Oid, oid2: Oid): Oid {
        val parents1 = listCommitsAndParents(listOf(oid1))
        val parents2 = listCommitsAndParents(listOf(oid2))
        return parents2.first { it in parents1 }
    }

}
