package kgit

import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest

const val KGIT_DIR = ".kgit"
const val OBJECTS_DIR = ".kgit/objects"

const val TYPE_BLOB = "blob"
const val TYPE_TREE = "tree"
const val TYPE_COMMIT = "commit"

private const val nullByte = 0.toChar().toByte()


object Data {

    fun init() {
        Files.createDirectory(Path.of(KGIT_DIR))
    }

    fun hashObject(data: ByteArray, type: String): String {
        ensureObjectsDirectory()

        val input =  type.encodeToByteArray() + nullByte + data
        val digest: ByteArray = MessageDigest.getInstance("SHA-1").digest(input)

        // OID is they key in the Object Database
        val oid = digest.joinToString(separator = "") { "%02x".format(it) }

        // write actual binary content using the OID
        val obj = Files.createFile(Path.of("$OBJECTS_DIR/$oid"))
        Files.write(obj, input)

        return oid
    }

    private fun ensureObjectsDirectory() {
        val objectsDirectory = Path.of(OBJECTS_DIR)
        if (!Files.exists(objectsDirectory)) {
            Files.createDirectory(objectsDirectory)
        }
    }

    fun getObject(oid: String, expectedType: String): String {
        val obj = Path.of("$OBJECTS_DIR/$oid")
        val allBytes = Files.readAllBytes(obj)

        // null byte separates the type from the content
        val separatorPos = allBytes.indexOfFirst { it == nullByte }

        val type = String(allBytes.filterIndexed { index, _ -> index < separatorPos }.toByteArray())
        if (type != expectedType) {
            throw InvalidTypeException(expected = expectedType, actual = type)
        }

        // plain content
        return String(allBytes.filterIndexed { index, _ -> index > separatorPos }.toByteArray())
    }
}

class InvalidTypeException(expected: String, actual: String)
    : RuntimeException("Expected $expected, got $actual")