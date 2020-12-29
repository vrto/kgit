package kgit.diff

import kgit.base.Tree
import kgit.data.ObjectDatabase
import kgit.data.Oid
import kgit.data.TYPE_BLOB
import kgit.diff.ChangeAction.*
import kgit.runCommand
import java.io.File

class Diff(private val data: ObjectDatabase) {

    fun diffTrees(orig: ComparableTree, changed: ComparableTree): List<String> {
        val (_, fromState, toState, allPaths) = orig.compareTo(other = changed)
        return allPaths
            .filter { fromState[it] != toState[it] }
            .map { diffBlobs(orig = fromState[it], changed = toState[it], path = it) }
    }

    private fun diffBlobs(orig: Oid?, changed: Oid?, path: String): String {
        val origTemp = orig.toTempFile("orig", "comparable")
        val changedTemp = changed.toTempFile("changed", "comparable")

        return "diff --unified --show-c-function --label $path $origTemp --label $path $changedTemp"
            .runCommand(File(data.workDir))
    }

    fun listFileChanges(orig: ComparableTree, changed: ComparableTree): List<FileChange> {
        val (_, fromState, toState, allPaths) = orig.compareTo(other = changed)
        return allPaths
            .map { FileChange(it, it.determineAction(fromState, toState)) }
            .filter { it.action != NO_CHANGE }
    }

    private fun String.determineAction(originalState: Map<String, Oid>, newState: Map<String, Oid>) = when {
        originalState[this] == null -> NEW
        newState[this] == null -> DELETED
        originalState[this] != newState[this] -> MODIFIED
        else -> NO_CHANGE
    }

    fun mergeTrees(base: ComparableTree, orig: ComparableTree, changed: ComparableTree): List<FileMerge> {
        val (baseState, fromState, toState, allPaths) = orig.compareTo(base, changed)
        return allPaths.map { FileMerge(
            path = it,
            content = mergeBlobs(base = baseState[it], orig = fromState[it], changed = toState[it]))
        }
    }

    private fun mergeBlobs(base: Oid?, orig: Oid?, changed: Oid?): String {
        val baseTemp = base.toTempFile("base", "diff")
        val origTemp = orig.toTempFile("orig", "diff")
        val changedTemp = changed.toTempFile("changed", "diff")

        return "diff3 -m -L HEAD $origTemp -L BASE $baseTemp -L MERGE_HEAD $changedTemp"
            .runCommand(File(data.workDir))
    }

    private fun Oid?.toTempFile(name: String, extension: String) = File.createTempFile(name, extension).apply {
        writeText(this@toTempFile.readLines().joinToString(separator = "\n", postfix = "\n"))
        deleteOnExit()
    }

    private fun Oid?.readLines() = this?.let {
        data.getObject(it, TYPE_BLOB).split("\n")
    } ?: emptyList()
}

typealias ComparableTree = List<Tree.FileState>

fun ComparableTree.asMap(): Map<String, Oid> = this.map { it.path to it.oid }.toMap()

private fun Map<String, Oid>.combinePaths(other: Map<String, Oid>) = (map { it.key } + other.map { it.key }).toSet()

fun ComparableTree.compareTo(base: ComparableTree? = null, other: ComparableTree) = TreeComparison(
    baseState = base?.asMap() ?: emptyMap(),
    fromState = this.asMap(),
    toState = other.asMap(),
    allPaths = this.asMap().combinePaths(other.asMap()))

data class TreeComparison(val baseState: Map<String, Oid>,
                          val fromState: Map<String, Oid>,
                          val toState: Map<String, Oid>,
                          val allPaths: Set<String>)

data class FileChange(val path: String, val action: ChangeAction)
enum class ChangeAction { NEW, DELETED, MODIFIED, NO_CHANGE }
data class FileMerge(val path: String, val content: String)