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
        val (fromState, toState, allPaths) = orig.compareTo(changed)
        return allPaths
            .filter { fromState[it] != toState[it] }
            .map { diffBlobs(orig = fromState[it], changed = toState[it], path = it) }
    }

    private fun diffBlobs(orig: Oid?, changed: Oid?, path: String): String {
        val (origTemp, changedTemp) = createTempFiles(orig, changed, extension = "comparable")

        return "diff --unified --show-c-function --label $path $origTemp --label $path $changedTemp"
            .runCommand(File(data.workDir))
    }

    fun listFileChanges(orig: ComparableTree, changed: ComparableTree): List<FileChange> {
        val (fromState, toState, allPaths) = orig.compareTo(changed)
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

    fun mergeTrees(orig: ComparableTree, changed: ComparableTree): List<FileMerge> {
        val (fromState, toState, allPaths) = orig.compareTo(changed)
        return allPaths
            .map { FileMerge(path = it, content = mergeBlobs(orig = fromState[it], changed = toState[it])) }
    }

    private fun mergeBlobs(orig: Oid?, changed: Oid?): String {
        val (origTemp, changedTemp) = createTempFiles(orig, changed, extension = "diff")

        return "diff -DHEAD $origTemp $changedTemp"
            .runCommand(File(data.workDir))
    }

    private fun createTempFiles(orig: Oid?, changed: Oid?, extension: String): Pair<File, File> {
        val origTemp = File.createTempFile("orig", extension).apply {
            writeText(orig.readLines().joinToString(separator = "\n", postfix = "\n"))
            deleteOnExit()
        }
        val changedTemp = File.createTempFile("changed", extension).apply {
            writeText(changed.readLines().joinToString(separator = "\n", postfix = "\n"))
            deleteOnExit()
        }
        return origTemp to changedTemp
    }
    private fun Oid?.readLines() = this?.let {
        data.getObject(it, TYPE_BLOB).split("\n")
    } ?: emptyList()
}

typealias ComparableTree = List<Tree.FileState>

fun ComparableTree.asMap(): Map<String, Oid> = this.map { it.path to it.oid }.toMap()

private fun Map<String, Oid>.combinePaths(other: Map<String, Oid>) = (map { it.key } + other.map { it.key }).toSet()

fun ComparableTree.compareTo(other: ComparableTree) = TreeComparison(
    fromState = this.asMap(),
    toState = other.asMap(),
    allPaths = this.asMap().combinePaths(other.asMap()))

data class TreeComparison(val fromState: Map<String, Oid>,
                          val toState: Map<String, Oid>,
                          val allPaths: Set<String>)

data class FileChange(val path: String, val action: ChangeAction)
enum class ChangeAction { NEW, DELETED, MODIFIED, NO_CHANGE }
data class FileMerge(val path: String, val content: String)