package app.revanced.patches.music.video.information.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint

object SeekFingerprint : MethodFingerprint(
    strings = listOf("Attempting to seek during an ad")
)