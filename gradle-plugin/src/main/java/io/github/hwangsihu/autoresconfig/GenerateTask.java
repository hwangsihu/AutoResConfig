package io.github.hwangsihu.autoresconfig;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;

public abstract class GenerateTask extends DefaultTask {

    protected final Collection<String> locales;
    protected final Collection<String> displayLocales;

    @OutputDirectory
    public abstract DirectoryProperty getOutputDir();

    @Inject
    public GenerateTask(Collection<String> locales, Collection<String> displayLocales) {
        this.locales = locales;
        this.displayLocales = displayLocales;
    }

    @TaskAction
    public void generate() throws IOException {
        File dir = getOutputDir().get().getAsFile();
        if (dir.exists()) {
            Path root = dir.toPath();
            try {
                Files.walkFileTree(root, new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        Files.delete(file);
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult postVisitDirectory(Path d, IOException exc) throws IOException {
                        if (!d.equals(root)) Files.delete(d);
                        return FileVisitResult.CONTINUE;
                    }
                });
            } catch (IOException ignored) {
            }
        } else {
            Files.createDirectories(dir.toPath());
        }
    }

    public final void createFile(File file) throws IOException {
        if (!file.exists()) {
            if (!file.getParentFile().exists()) {
                if (!file.getParentFile().mkdirs()) {
                    throw new IOException("Failed to create " + file.getParentFile());
                }
            }
            if (!file.createNewFile()) {
                throw new IOException("Failed to create " + file);
            }
        }
    }
}
