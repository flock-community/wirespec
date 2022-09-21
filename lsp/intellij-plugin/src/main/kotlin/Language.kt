// Copyright 2000-2022 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package community.flock.wire_spec.lsp.intellij_plugin

class Language : com.intellij.lang.Language("wire-spec") {

    companion object{
        val INSTANCE: Language = Language()
    }
}