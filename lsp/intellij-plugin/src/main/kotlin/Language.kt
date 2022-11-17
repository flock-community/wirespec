// Copyright 2000-2022 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package community.flock.wirespec.lsp.intellij_plugin

import com.intellij.lang.Language as IntellijLanguage

class Language : IntellijLanguage("wirespec") {

    companion object {
        val INSTANCE: Language = Language()
    }
}