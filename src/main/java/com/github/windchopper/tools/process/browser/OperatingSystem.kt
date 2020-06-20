@file:Suppress("unused")

package com.github.windchopper.tools.process.browser

import java.util.*

enum class OperatingSystem(private val token: String) {

    WIN32("win"), MACOS("mac"), LINUX("nux");

    companion object {

        fun detect(): OperatingSystem? {
            with (System.getProperty("os.name")?.toLowerCase()) {
                return EnumSet.allOf(OperatingSystem::class.java)
                    .firstOrNull { this
                        ?.contains(it.token)
                        ?:false }
            }
        }

    }

}