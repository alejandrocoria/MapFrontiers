package games.alejandrocoria.mapfrontiers.common.config;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class TomlConfigIO {
    public static CommentedFileConfig open(Path path) throws IOException {
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        return CommentedFileConfig.builder(path).sync().preserveInsertionOrder().build();
    }

    private TomlConfigIO() {
    }
}
