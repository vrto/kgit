package kgit.remote

import kgit.data.ObjectDatabase

class Remote {

    fun fetch(remotePath: String): List<String> = getRemoteRefs(remotePath, prefix = "heads/")

    private fun getRemoteRefs(remotePath: String, prefix: String): List<String> {
        val remoteData = ObjectDatabase(remotePath)
        return remoteData.iterateRefs()
            .map { it.name }
            .filter { it.contains(prefix) }
    }
}