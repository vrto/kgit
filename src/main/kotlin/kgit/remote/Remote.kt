package kgit.remote

import kgit.data.NamedRefValue
import kgit.data.ObjectDatabase

class Remote(private val data: ObjectDatabase) {

    fun fetch(remotePath: String): List<String> {
        val remoteRefs = getRemoteRefs(remotePath, prefix = "heads/")
        remoteRefs.forEach { ref ->
            val refName = ref.name.replace("heads", "remote")
            data.updateRef("refs/$refName", ref.ref)
        }
        return remoteRefs.map { it.name }
    }

    private fun getRemoteRefs(remotePath: String, prefix: String): List<NamedRefValue> {
        val remoteData = ObjectDatabase(remotePath)
        return remoteData.iterateRefs()
            .filter { it.name.contains(prefix) }
    }
}