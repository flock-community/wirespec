package community.flock.wire_spec.lsp.intellij_plugin;

import com.intellij.openapi.application.PreloadingActivity;
import com.intellij.openapi.progress.ProgressIndicator;
import org.wso2.lsp4intellij.IntellijLanguageClient;
import org.wso2.lsp4intellij.client.languageserver.serverdefinition.RawCommandServerDefinition;
import org.wso2.lsp4intellij.requests.Timeouts;

public class Preloading extends PreloadingActivity {
    public void preload(ProgressIndicator indicator) {
        IntellijLanguageClient.setTimeout(Timeouts.INIT, 15000);
        IntellijLanguageClient.addServerDefinition(new RawCommandServerDefinition("ws", new String[]{"node", "/Users/willemveelenturf/projects/flock/wirespec/wire-spec/lsp/node/server/build/index.js", "--stdio", "--inspect=6009"}));
    }
}