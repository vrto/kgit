package kgit.remote

import kgit.base.KGit
import kgit.data.*
import kgit.diff.Diff

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

    fun push(remotePath: String, refName: String) {
        val localRef = localData.getRef(refName).value.takeIf { it.toOidOrNull() != null }
            ?: throw IllegalArgumentException("$refName branch does not exist!")
        val objectsToPush = localKgit.listObjectsInCommits(listOf(localRef.toOid()))
        objectsToPush.forEach { localData.pushObject(it, remotePath) }

        val remoteData = ObjectDatabase(remotePath)
        remoteData.updateRef(refName, RefValue(value = localRef))
    }
}