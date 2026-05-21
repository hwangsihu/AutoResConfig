package io.github.hwangsihu.autoresconfig;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collection;
import java.util.stream.Collectors;

public abstract class GenerateLocaleConfigResTask extends GenerateTask {

    @Inject
    public GenerateLocaleConfigResTask(Collection<String> locales, Collection<String> displayLocales) {
        super(locales, displayLocales);
    }

    @Override
    public void generate() throws IOException {
        super.generate();

        File file = new File(getOutputDir().get().getAsFile(), "xml/locales_config.xml");
        createFile(file);

        try (PrintStream os = new PrintStream(file)) {
            write(os);
        }
    }

    public void write(PrintStream os) {
        String content = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<locale-config xmlns:android=\"http://schemas.android.com/apk/res/android\">\n" +
                "%s\n" +
                "</locale-config>\n";

        var localesString = locales.stream().map(s -> "<locale android:name=\"" + s + "\"/>").collect(Collectors.joining("\n"));

        os.printf(content, localesString);
    }
}
