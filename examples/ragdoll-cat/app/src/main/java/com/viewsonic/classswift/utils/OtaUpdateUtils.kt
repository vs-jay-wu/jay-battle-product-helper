package com.viewsonic.classswift.utils

import com.viewsonic.classswift.BuildConfig

object OtaUpdateUtils {
    /**
     *  version name should be following Semantic Versioning format
     *  ex. MAJOR.MINOR.PATCH -> 1.2.3
     */
    fun getApkDownloadFileName(versionName: String): String {
        val sanitizedVersion = versionName.replace(".", "_")
        return "ClassSwift_Service_${sanitizedVersion}.apk"
    }

    /**
     *  Converts a server-published 3-part version name (MAJOR.MINOR.HOTFIX) to its
     *  comparable versionCode under the ClassSwift Service scheme:
     *
     *    versionCode = MAJOR * 100_000 + MINOR * 1_000 + HOTFIX * 100 + INTERNAL
     *
     *  Server feeds publish 3-part names (no INTERNAL); the INTERNAL component
     *  is treated as 0 here so a comparison against a local 4-part build
     *  correctly rejects updating an INTERNAL build "back" to its release peer.
     */
    fun releaseVersionNameToVersionCode(versionName: String = BuildConfig.VERSION_NAME): Int {
        val parts = versionName.split(".")
            .mapNotNull { it.toIntOrNull() }
            .take(3)
        val padded = parts + List(3 - parts.size) { 0 } // pad with zeros if needed
        val (major, minor, hotfix) = padded

        return major * 100_000 +
                minor * 1_000 +
                hotfix * 100
    }
}