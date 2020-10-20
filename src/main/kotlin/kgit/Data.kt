package kgit

import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest

const val KGIT_DIR = ".kgit"

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
        val obj = Files.createFile(Path.of("$KGIT_DIR/objects/$oid"))
        Files.write(obj, data)

        return oid
    }

    private fun ensureObjectsDirectory() {
        val objectsDirectory = Path.of("$KGIT_DIR/objects")
        if (!Files.exists(objectsDirectory)) {
            Files.createDirectory(objectsDirectory)
        }
    }
}