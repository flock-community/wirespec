// Copyright 2000-2022 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package community.flock.wire_spec.lsp.intellij_plugin;

public class Language extends com.intellij.lang.Language {

  public static final Language INSTANCE = new Language();

  private Language() {
    super("wire-spec");
  }

}
