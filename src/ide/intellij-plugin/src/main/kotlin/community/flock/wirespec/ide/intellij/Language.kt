// Copyright 2000-2022 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package community.flock.wirespec.ide.intellij

import com.intellij.lang.Language as IntellijLanguage

object Language : IntellijLanguage("wirespec") {
    private fun readResolve(): Any = Language
}
