package io.github.hwangsihu.autoresconfig;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collection;
import java.util.stream.Collectors;

public abstract class GenerateJavaTask extends GenerateTask {

    private final AutoResConfigExtension extension;

    @Inject
    public GenerateJavaTask(AutoResConfigExtension extension,
                            Collection<String> locales, Collection<String> displayLocales) {
        super(locales, displayLocales);
        this.extension = extension;
    }

    @Override
    public void generate() throws IOException {
        super.generate();

        String fqcn = extension.getGeneratedClassFullName().get();
        File file = new File(getOutputDir().get().getAsFile(),
                String.join("/", fqcn.split("\\.")) + ".java");
        createFile(file);

        try (PrintStream os = new PrintStream(file)) {
            write(os);
        }
    }

    public void write(PrintStream os) {
        String content = "package %s;\n" +
                "\n" +
                "public final class %s {\n" +
                "    public static final String[] LOCALES = {%s%s};\n" +
                "    public static final String[] DISPLAY_LOCALES = {%s%s};\n" +
                "}\n";

        var generatedClassFullName = extension.getGeneratedClassFullName().get();
        var index = generatedClassFullName.lastIndexOf('.');
        var packageName = generatedClassFullName.substring(0, index);
        var className = generatedClassFullName.substring(index + 1);

        var localesString = locales.stream().map(s -> "\"" + s + "\"").collect(Collectors.joining(","));
        var displayLocalesString = displayLocales.stream().map(s -> "\"" + s + "\"").collect(Collectors.joining(","));
        var firstItem = extension.getGeneratedArrayFirstItem().getOrElse("").isEmpty() ? ""
                : ("\"" + extension.getGeneratedArrayFirstItem().get() + "\",");

        os.printf(content,
                packageName,
                className,
                firstItem,
                localesString,
                firstItem,
                displayLocalesString);
    }
}
