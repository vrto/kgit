package kgit

import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest

const val KGIT_DIR = ".kgit"
const val OBJECTS_DIR = ".kgit/objects"

object Data {

    fun init() {
        Files.createDirectory(Path.of(KGIT_DIR))
    }

    fun hashObject(data: ByteArray): String {
        ensureObjectsDirectory()

        val digest: ByteArray = MessageDigest.getInstance("SHA-1").digest(data)

        // OID is they key in the Object Database
        val oid = digest.joinToString(separator = "") { "%02x".format(it) }

        // write actual binary content using the OID
        val obj = Files.createFile(Path.of("$OBJECTS_DIR/$oid"))
        Files.write(obj, data)

        return oid
    }

    private fun ensureObjectsDirectory() {
        val objectsDirectory = Path.of(OBJECTS_DIR)
        if (!Files.exists(objectsDirectory)) {
            Files.createDirectory(objectsDirectory)
        }
    }

    fun getObject(oid: String): String {
        val obj = Path.of("$OBJECTS_DIR/$oid")
        return String(Files.readAllBytes(obj))
    }
}