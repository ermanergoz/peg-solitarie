package com.app.pegsolitaire.testing

import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * The Fredoka binary Google Fonts serves for the app's title and headline roles, copied into a
 * file in this folder (see `fixtures/fonts/README.md` on the test classpath).
 *
 * The copy is needed because a font loads from a file and a classpath resource is not one. The
 * fixture does not sit in a root `fonts/` directory: one there shadows the directory Robolectric
 * loads the platform's own fonts from, and every Robolectric test then fails to start.
 */
fun TemporaryFolder.shippedFaceFile(): File {
    val file = newFile(SHIPPED_FACE_FILE_NAME)
    val resource =
        checkNotNull(Thread.currentThread().contextClassLoader?.getResourceAsStream(SHIPPED_FACE_RESOURCE)) {
            "missing test fixture $SHIPPED_FACE_RESOURCE"
        }
    resource.use { input -> file.outputStream().use { output -> input.copyTo(output) } }
    return file
}

private const val SHIPPED_FACE_RESOURCE = "fixtures/fonts/Fredoka-Variable.ttf"
private const val SHIPPED_FACE_FILE_NAME = "Fredoka-Variable.ttf"
