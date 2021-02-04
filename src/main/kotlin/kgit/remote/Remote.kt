package kgit.remote

import kgit.base.KGit
import kgit.data.*
import kgit.diff.Diff
import kgit.remote.PushResult.*

class Remote(private val localData: ObjectDatabase, private val localKgit: KGit) {

    fun fetch(remotePath: String): List<String> {
        val remoteData = ObjectDatabase(remotePath)
        val remoteRefs = remoteData.getRemoteRefs(prefix = "heads/")
        val remoteKgit = KGit(remoteData, Diff(remoteData))

        remoteKgit.downloadObjects(remoteRefs, remotePath)
        localData.updateLocalRefs(remoteRefs)

        return remoteRefs.map { it.name }
    }

    private fun ObjectDatabase.getRemoteRefs(prefix: String): List<NamedRefValue> = iterateRefs()
        .filter { it.name.contains(prefix) }

    private fun KGit.downloadObjects(remoteRefs: List<NamedRefValue>, remotePath: String) {
        val objects = listObjectsInCommits(remoteRefs.map { it.ref.oid })
        objects.forEach { localData.fetchObjectIfMissing(it, remotePath) }
    }

    private fun ObjectDatabase.updateLocalRefs(remoteRefs: List<NamedRefValue>) {
        remoteRefs.forEach { ref ->
            val refName = ref.name.replace("heads", "remote")
            updateRef("refs/$refName", ref.ref)
        }
    }

    fun push(remotePath: String, refName: String): PushResult {
        val remoteData = ObjectDatabase(remotePath)
        val remoteRefs = remoteData.getRemoteRefs(prefix = "heads/")
        val remoteRef = remoteRefs.find { "refs/${it.name}" == refName }
        val localRef = localData.getRef(refName).value.takeIf { it.toOidOrNull() != null }
            ?: return UNKNOWN_REF

        if (remoteRef != null /* null -> new ref, that's OK */ && remoteRef.isNotAncestor(localRef)) {
            return FORCE_PUSH_REJECTED
        }

        val knownRemoteRefs = remoteRefs
            .filter { localData.objectExists(it.ref.oid) }
            .map { it.ref.oid }

        val remoteObjects = localKgit.listObjectsInCommits(knownRemoteRefs)
            .distinct()

        val localObjects = localKgit.listObjectsInCommits(listOf(localRef.toOid()))
            .distinct()

        val objectsToPush = localObjects - remoteObjects
        objectsToPush.forEach {
            localData.pushObject(it, remotePath)
        }

        remoteData.updateRef(refName, RefValue(value = localRef))

        return OK
    }

    private fun NamedRefValue.isNotAncestor(localRef: String) =
        !localKgit.isAncestor(localRef.toOid(), ref.oid)
}

enum class PushResult {
    OK, UNKNOWN_REF, FORCE_PUSH_REJECTED
}