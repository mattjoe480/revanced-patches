package app.revanced.util.resources

import app.revanced.extensions.doRecursively
import app.revanced.patcher.data.ResourceContext
import app.revanced.util.enum.CategoryType
import org.w3c.dom.Element
import org.w3c.dom.Node

private fun Node.adoptChild(tagName: String, block: Element.() -> Unit) {
    val child = ownerDocument.createElement(tagName)
    child.block()
    appendChild(child)
}

private fun Node.cloneNodes(parent: Node) {
    val node = cloneNode(true)
    parent.appendChild(node)
    parent.removeChild(this)
}

private fun Node.insertNode(tagName: String, targetNode: Node, block: Element.() -> Unit) {
    val child = ownerDocument.createElement(tagName)
    child.block()
    parentNode.insertBefore(child, targetNode)
}


internal object MusicResourceHelper {

    internal const val YOUTUBE_MUSIC_SETTINGS_PATH = "res/xml/settings_headers.xml"

    internal const val YOUTUBE_MUSIC_SETTINGS_KEY = "revanced_extended_settings"

    internal const val RETURN_YOUTUBE_DISLIKE_SETTINGS_KEY = "revanced_ryd_settings"

    private const val YOUTUBE_MUSIC_PREFERENCE_SCREEN_TAG_NAME =
        "PreferenceScreen"

    private const val YOUTUBE_MUSIC_PREFERENCE_TAG_NAME =
        "com.google.android.apps.youtube.music.ui.preference.SwitchCompatPreference"

    private const val YOUTUBE_MUSIC_PREFERENCE_TARGET_CLASS =
        "com.google.android.libraries.strictmode.penalties.notification.FullStackTraceActivity"

    internal var targetPackage = "com.google.android.apps.youtube.music"

    internal fun ResourceContext.setMicroG(newPackage: String) {
        targetPackage = newPackage
        replacePackageName()
    }

    private fun setMusicPreferenceCategory(newCategory: String) {
        CategoryType.entries.forEach { preference ->
            if (newCategory == preference.value)
                preference.added = true
        }
    }

    private fun included(category: String): Boolean {
        CategoryType.entries.forEach { preference ->
            if (category == preference.value)
                return preference.added
        }
        return false
    }

    internal fun ResourceContext.addMusicPreferenceCategory(
        category: String
    ) {
        this.xmlEditor[YOUTUBE_MUSIC_SETTINGS_PATH].use { editor ->
            val tags = editor.file.getElementsByTagName("PreferenceScreen")
            List(tags.length) { tags.item(it) as Element }
                .filter { it.getAttribute("android:key").contains(YOUTUBE_MUSIC_SETTINGS_KEY) }
                .forEach {
                    if (!included(category)) {
                        it.adoptChild(YOUTUBE_MUSIC_PREFERENCE_SCREEN_TAG_NAME) {
                            setAttribute("android:title", "@string/revanced_category_$category")
                            setAttribute("android:key", "revanced_settings_$category")
                        }
                        setMusicPreferenceCategory(category)
                    }
                }
        }
    }

    internal fun ResourceContext.sortMusicPreferenceCategory(
        category: String
    ) {
        this.xmlEditor[YOUTUBE_MUSIC_SETTINGS_PATH].use { editor ->
            editor.file.doRecursively loop@{
                if (it !is Element) return@loop

                it.getAttributeNode("android:key")?.let { attribute ->
                    if (attribute.textContent == "revanced_settings_$category") {
                        it.cloneNodes(it.parentNode)
                    }
                }
            }
        }
        replacePackageName()
    }

    private fun ResourceContext.replacePackageName() {
        this[YOUTUBE_MUSIC_SETTINGS_PATH].writeText(
            this[YOUTUBE_MUSIC_SETTINGS_PATH].readText()
                .replace("\"com.google.android.apps.youtube.music\"", "\"" + targetPackage + "\"")
        )
    }

    internal fun ResourceContext.addMusicPreference(
        category: String,
        key: String,
        defaultValue: String,
        dependencyKey: String
    ) {
        this.xmlEditor[YOUTUBE_MUSIC_SETTINGS_PATH].use { editor ->
            val tags = editor.file.getElementsByTagName(YOUTUBE_MUSIC_PREFERENCE_SCREEN_TAG_NAME)
            List(tags.length) { tags.item(it) as Element }
                .filter { it.getAttribute("android:key").contains("revanced_settings_$category") }
                .forEach {
                    it.adoptChild(YOUTUBE_MUSIC_PREFERENCE_TAG_NAME) {
                        setAttribute("android:title", "@string/$key" + "_title")
                        setAttribute("android:summary", "@string/$key" + "_summary")
                        setAttribute("android:key", key)
                        setAttribute("android:defaultValue", defaultValue)
                        if (dependencyKey != "") {
                            setAttribute("android:dependency", dependencyKey)
                        }
                    }
                }
        }
    }

    internal fun ResourceContext.addMusicPreferenceWithIntent(
        category: String,
        key: String,
        dependencyKey: String
    ) {
        this.xmlEditor[YOUTUBE_MUSIC_SETTINGS_PATH].use { editor ->
            val tags = editor.file.getElementsByTagName(YOUTUBE_MUSIC_PREFERENCE_SCREEN_TAG_NAME)
            List(tags.length) { tags.item(it) as Element }
                .filter { it.getAttribute("android:key").contains("revanced_settings_$category") }
                .forEach {
                    it.adoptChild("Preference") {
                        setAttribute("android:title", "@string/$key" + "_title")
                        setAttribute("android:summary", "@string/$key" + "_summary")
                        setAttribute("android:key", key)
                        if (dependencyKey != "") {
                            setAttribute("android:dependency", dependencyKey)
                        }
                        this.adoptChild("intent") {
                            setAttribute("android:targetPackage", targetPackage)
                            setAttribute("android:data", key)
                            setAttribute(
                                "android:targetClass",
                                YOUTUBE_MUSIC_PREFERENCE_TARGET_CLASS
                            )
                        }
                    }
                }
        }
    }

    internal fun ResourceContext.addMusicPreferenceWithoutSummary(
        category: String,
        key: String,
        defaultValue: String
    ) {
        this.xmlEditor[YOUTUBE_MUSIC_SETTINGS_PATH].use { editor ->
            val tags = editor.file.getElementsByTagName(YOUTUBE_MUSIC_PREFERENCE_SCREEN_TAG_NAME)
            List(tags.length) { tags.item(it) as Element }
                .filter { it.getAttribute("android:key").contains("revanced_settings_$category") }
                .forEach {
                    it.adoptChild(YOUTUBE_MUSIC_PREFERENCE_TAG_NAME) {
                        setAttribute("android:title", "@string/$key" + "_title")
                        setAttribute("android:key", key)
                        setAttribute("android:defaultValue", defaultValue)
                    }
                }
        }
    }

    internal fun ResourceContext.addReVancedMusicPreference(
        key: String
    ) {
        this.xmlEditor[YOUTUBE_MUSIC_SETTINGS_PATH].use { editor ->
            with(editor.file) {
                doRecursively loop@{
                    if (it !is Element) return@loop
                    it.getAttributeNode("android:key")?.let { attribute ->
                        if (attribute.textContent == "settings_header_about_youtube_music" && it.getAttributeNode(
                                "app:allowDividerBelow"
                            ).textContent == "false"
                        ) {
                            it.insertNode("PreferenceScreen", it) {
                                setAttribute(
                                    "android:title",
                                    "@string/" + key + "_title"
                                )
                                setAttribute("android:key", key)
                                setAttribute("app:allowDividerAbove", "false")
                                setAttribute("app:allowDividerAbove", "false")
                            }
                            it.getAttributeNode("app:allowDividerBelow").textContent = "true"
                            return@loop
                        }
                    }
                }

                doRecursively loop@{
                    if (it !is Element) return@loop

                    it.getAttributeNode("app:allowDividerBelow")?.let { attribute ->
                        if (attribute.textContent == "true") {
                            attribute.textContent = "false"
                        }
                    }
                }
            }
        }
    }

    internal fun ResourceContext.hookPreference(
        key: String,
        fragment: String
    ) {
        this.xmlEditor[YOUTUBE_MUSIC_SETTINGS_PATH].use { editor ->
            with(editor.file) {
                doRecursively loop@{
                    if (it !is Element) return@loop
                    it.getAttributeNode("android:key")?.let { attribute ->
                        if (attribute.textContent == "settings_header_about_youtube_music" && it.getAttributeNode(
                                "app:allowDividerBelow"
                            ).textContent == "false"
                        ) {
                            it.insertNode("Preference", it) {
                                setAttribute("android:persistent", "false")
                                setAttribute(
                                    "android:title",
                                    "@string/" + key + "_title"
                                )
                                setAttribute("android:key", key)
                                setAttribute("android:fragment", fragment)
                                setAttribute("app:allowDividerAbove", "false")
                                setAttribute("app:allowDividerAbove", "false")
                            }
                            it.getAttributeNode("app:allowDividerBelow").textContent = "true"
                            return@loop
                        }
                    }
                }

                doRecursively loop@{
                    if (it !is Element) return@loop

                    it.getAttributeNode("app:allowDividerBelow")?.let { attribute ->
                        if (attribute.textContent == "true") {
                            attribute.textContent = "false"
                        }
                    }
                }
            }
        }
    }
}