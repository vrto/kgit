package kgit.remote

import kgit.data.ObjectDatabase

class Remote {

    fun fetch(remotePath: String): List<String> {
        val remoteData = ObjectDatabase(remotePath)
        return remoteData.iterateRefs()
            .map { it.name }
            .filter { it.contains("heads/") }
    }
}