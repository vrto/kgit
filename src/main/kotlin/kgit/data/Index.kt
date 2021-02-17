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

    private fun toRawMap(): Map<String, String> = data.toMap()

}