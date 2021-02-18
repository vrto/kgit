package kgit.data

import com.beust.klaxon.Klaxon
import java.io.File

class Index(private val path: String) {

    private val json = Klaxon()
    private val data: MutableMap<String, String>

    init {
        data = File(path)
            .takeIf { it.exists() }
            ?.let { json.parse<Map<String, String>>(it)?.toMutableMap() }
            ?: HashMap()
    }

    operator fun get(fileName: String): Oid? = data[fileName]?.toOid()

    operator fun set(fileName: String, oid: Oid) {
        data[fileName] = oid.toString()

        val jsonToSave = json.toJsonString(this.toRawMap())
        File(path).writeText(jsonToSave)
    }

    fun isEmpty() = data.isEmpty()

    fun getSize() = data.keys.size

    fun inflate(): Map<String, Any> {
        fun insert(dirNames: List<String>, oid: String, indexAsTree: IndexNode) {
            val first = dirNames.first()
            if (dirNames.size == 1) {
                indexAsTree[first] = oid.toOid()
                return
            }
            @Suppress("UNCHECKED_CAST")
            val result = indexAsTree.getOrPut(first) { mutableMapOf<String, Any>() } as IndexNode
            insert(dirNames.drop(1), oid, result)
        }

        val indexAsTree = mutableMapOf<String, Any>()
        data.forEach { (path, oid) -> insert(path.split("/"), oid, indexAsTree) }
        return indexAsTree
    }

    private fun toRawMap(): Map<String, String> = data.toMap()

}

private typealias IndexNode=MutableMap<String, Any>