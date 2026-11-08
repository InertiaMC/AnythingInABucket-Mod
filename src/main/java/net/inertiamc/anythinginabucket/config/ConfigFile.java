package net.inertiamc.anythinginabucket.config;

import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * {@code config/anythinginabucket/config.yml}. Written from the built-in copy when missing and never rewritten. If the
 * file can't be read, the current settings (or the built-in ones at startup) stay in effect.
 */
public final class ConfigFile {

    private final Path file;
    private final Logger logger;

    public ConfigFile(Path folder, Logger logger) {
        this.file = folder.resolve("config.yml");
        this.logger = logger;
    }

    /** {@code error} is null when the file was read. */
    public record Loaded(Settings settings, String error) {}

    public Loaded load(Settings current) {
        try {
            if (Files.notExists(file)) {
                Files.createDirectories(file.getParent());
                try (InputStream in = builtIn()) {
                    Files.copy(in, file);
                }
            }
            Settings settings = Settings.read(FlatYaml.parse(Files.readString(file, StandardCharsets.UTF_8)));
            settings.problems().forEach(p -> logger.warn("config.yml: {}", p));
            return new Loaded(settings, null);
        } catch (IOException | RuntimeException e) {
            String error = "config.yml could not be read: " + e.getMessage();
            logger.error(error);
            return new Loaded(current != null ? current : builtInSettings(), error);
        }
    }

    public static Settings builtInSettings() {
        try (InputStream in = builtIn()) {
            return Settings.read(FlatYaml.parse(new String(in.readAllBytes(), StandardCharsets.UTF_8)));
        } catch (IOException e) {
            throw new IllegalStateException("The built-in config.yml is missing", e);
        }
    }

    private static InputStream builtIn() throws IOException {
        InputStream in = ConfigFile.class.getResourceAsStream("/assets/anythinginabucket/config.yml");
        if (in == null) throw new IOException("built-in config.yml missing from the jar");
        return in;
    }
}
