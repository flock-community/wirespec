package community.flock.wire_spec.lsp.intellij_plugin;

import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.javascript.nodejs.interpreter.NodeJsInterpreter;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.wso2.lsp4intellij.IntellijLanguageClient;
import org.wso2.lsp4intellij.client.languageserver.serverdefinition.RawCommandServerDefinition;
import com.intellij.javascript.nodejs.interpreter.NodeJsInterpreterManager;

import java.io.*;
import java.nio.file.Path;
import java.util.Objects;

public class Preloading implements StartupActivity {

    public void runActivity(@NotNull Project project) {

        NodeJsInterpreter nodeJsInterpreter = NodeJsInterpreterManager.getInstance(project).getInterpreter();

        File script = copyScriptToPluginFolder();

        assert nodeJsInterpreter != null;
        String[] command = new String[]{nodeJsInterpreter.toString(), script.toString(), "--stdio"};
        IntellijLanguageClient.addServerDefinition(new RawCommandServerDefinition("ws", command));
    }

    private File copyScriptToPluginFolder(){
        IdeaPluginDescriptor plugin = PluginManagerCore.getPlugin(PluginId.getId("community.flock.wire_spec.lsp.intellij_plugin"));
        assert plugin != null;
        Path script = plugin.getPluginPath().resolve("lsp.js");

        System.out.println(plugin.getPluginPath().toString());
        try {
            InputStream inputStream = Objects.requireNonNull(plugin.getPluginClassLoader()).getResourceAsStream("static/index.js");
            assert inputStream != null;
            FileUtils.copyInputStreamToFile(inputStream, script.toFile());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return script.toFile();
    }
}