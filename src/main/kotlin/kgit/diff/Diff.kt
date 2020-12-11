package kgit.diff

import com.github.difflib.DiffUtils
import com.github.difflib.UnifiedDiffUtils
import kgit.base.Tree
import kgit.data.ObjectDatabase
import kgit.data.Oid
import kgit.data.TYPE_BLOB
import kgit.diff.ChangeAction.*

class Diff(private val data: ObjectDatabase) {

    fun diffTrees(orig: ComparableTree, changed: ComparableTree): List<String> {
        val fromState = orig.asMap()
        val toState = changed.asMap()
        val allPaths = fromState.combinePaths(toState)
        return allPaths
            .filter { fromState[it] != toState[it] }
            .map { diffBlobs(orig = fromState[it], changed = toState[it], path = it) }
            .flatten()
    }

    private fun Map<String, Oid>.combinePaths(other: Map<String, Oid>) = (map { it.key } + other.map { it.key }).toSet()

    private fun diffBlobs(orig: Oid?, changed: Oid?, path: String): List<String> {
        val origLines = orig.readLines()
        val changedLines = changed.readLines()
        val patch = DiffUtils.diff(origLines, changedLines)
        return UnifiedDiffUtils.generateUnifiedDiff(path, path, origLines, patch, 0)
    }

    fun listFileChanges(orig: ComparableTree, changed: ComparableTree): List<FileChange> {
        val fromState = orig.asMap()
        val toState = changed.asMap()
        val allPaths = fromState.combinePaths(toState)
        return allPaths
            .map { FileChange(it, it.determineAction(fromState, toState)) }
            .filter { it.action != NO_CHANGE }
    }

    private fun Oid?.readLines() = this?.let {
        data.getObject(it, TYPE_BLOB).split("\n")
    } ?: emptyList()
}

private fun String.determineAction(originalState: Map<String, Oid>, newState: Map<String, Oid>) = when {
    originalState[this] == null -> NEW
    newState[this] == null -> DELETED
    originalState[this] != newState[this] -> MODIFIED
    else -> NO_CHANGE
}

typealias ComparableTree = List<Tree.FileState>
fun ComparableTree.asMap() = this.map { it.path to it.oid }.toMap()

data class FileChange(val path: String, val action: ChangeAction)
enum class ChangeAction {
    NEW, DELETED, MODIFIED, NO_CHANGE
}