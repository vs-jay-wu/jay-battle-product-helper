package com.viewsonic.classswift.ui.widget

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class NetworkDisconnectViewLayoutTest {

    @Test
    fun `network disconnect layout should include close action entry`() {
        val layoutPath = resolveLayoutFile()
        val xml = String(Files.readAllBytes(layoutPath))

        assertTrue(
            "network disconnect layout should include tv_close id",
            xml.contains("""android:id="@+id/tv_close"""")
        )
        assertTrue(
            "network disconnect layout should use common_close text resource",
            xml.contains("""android:text="@string/common_close"""")
        )
        assertTrue(
            "close action should provide a selectable ripple background",
            xml.contains("""android:background="?attr/selectableItemBackgroundBorderless"""")
        )
        assertTrue(
            "network disconnect container should use figma-aligned horizontal padding",
            xml.contains("""android:paddingHorizontal="10.66dp"""")
        )
    }

    private fun resolveLayoutFile(): Path {
        return resolveFromClassLocation(
            anchor = NetworkDisconnectViewLayoutTest::class.java,
            relativePath = "app/src/main/res/layout-xxhdpi/view_network_disconnect.xml"
        )
    }

    private fun resolveFromClassLocation(anchor: Class<*>, relativePath: String): Path {
        val codeSourceLocation = checkNotNull(anchor.protectionDomain?.codeSource?.location) {
            "Cannot resolve code source location for ${anchor.name}"
        }
        val anchorPath = Paths.get(codeSourceLocation.toURI()).toAbsolutePath().normalize()
        var current: Path? = if (Files.isDirectory(anchorPath)) anchorPath else anchorPath.parent

        while (current != null) {
            val candidate = current.resolve(relativePath).normalize()
            if (Files.isRegularFile(candidate)) {
                return candidate
            }
            current = current.parent
        }

        error("Cannot resolve $relativePath from anchor path $anchorPath")
    }
}
