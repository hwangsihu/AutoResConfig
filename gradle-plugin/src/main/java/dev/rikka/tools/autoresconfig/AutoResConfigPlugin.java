package dev.rikka.tools.autoresconfig;

import com.android.build.api.dsl.ApplicationExtension;
import com.android.build.api.variant.AndroidComponentsExtension;
import com.android.build.api.variant.Variant;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.tasks.TaskProvider;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
public class AutoResConfigPlugin implements Plugin<Project> {

    private static final Map<String, String> displayLocaleMap = new HashMap<>();

    static {
        displayLocaleMap.put("zh-CN", "zh-Hans");
        displayLocaleMap.put("zh-TW", "zh-Hant");
    }

    private final Logger logger = Logging.getLogger(AutoResConfigPlugin.class);

    private void collectModifiers(File dir, Collection<String> output) throws IOException {
        if (!dir.exists() || !dir.isDirectory()) return;
        try (var stream = Files.list(dir.toPath())) {
            output.addAll(stream
                    .filter(file -> {
                        try {
                            return Files.isDirectory(file)
                                    && file.toFile().getName().startsWith("values-")
                                    && Files.exists(file.resolve("strings.xml"))
                                    && Files.size(file.resolve("strings.xml")) > 62;
                        } catch (IOException e) {
                            return false;
                        }
                    })
                    .map(java.nio.file.Path::getFileName)
                    .map(path -> path.toFile().getName().substring("values-".length()))
                    .filter(s -> s.split("-").length <= 3)
                    .collect(Collectors.toList()));
        }
    }

    private Collection<String> convertModifiersToLocales(Collection<String> modifiers) {
        var locales = new ArrayList<String>();
        for (String modifier : modifiers) {
            String locale;
            if (modifier.startsWith("b+")) {
                String[] names = modifier.substring("b+".length()).split("\\+", 2);
                locale = names.length == 2 ? names[0] + "-" + names[1] : names[0];
            } else {
                String[] names = modifier.split("-", 2);
                locale = names.length == 2 ? names[0] + "-" + names[1].substring("r".length()) : names[0];
            }
            locales.add(locale);
        }
        locales.sort(String.CASE_INSENSITIVE_ORDER);
        return locales;
    }

    private Collection<String> convertLocalesToDisplayLocales(Collection<String> locales) {
        var result = new ArrayList<String>();
        for (String locale : locales) {
            String d = displayLocaleMap.get(locale);
            result.add(d == null ? locale : d);
        }
        return result;
    }

    @Override
    public void apply(Project project) {
        var extension = project.getExtensions().create("autoResConfig", AutoResConfigExtension.class);

        project.getPlugins().withId("com.android.base", plugin -> {
            var resDir = project.file("src/main/res");
            var modifiers = new HashSet<String>();
            modifiers.add("en");
            try {
                collectModifiers(resDir, modifiers);
            } catch (IOException e) {
                logger.warn("AutoResConfig: Failed to collect modifiers from " + resDir, e);
            }
            var sortedMods = new ArrayList<>(modifiers);
            sortedMods.sort(String.CASE_INSENSITIVE_ORDER);
            var locales = convertModifiersToLocales(sortedMods);
            var displayLocales = convertLocalesToDisplayLocales(locales);

            @SuppressWarnings("unchecked")
            AndroidComponentsExtension<?, ?, Variant> androidComponents =
                    (AndroidComponentsExtension<?, ?, Variant>) project.getExtensions().getByType(AndroidComponentsExtension.class);

androidComponents.onVariants(androidComponents.selector().all(), variant -> {
                var variantName = variant.getName();
                var variantNameCapitalized = Util.capitalize(variantName);

                if (extension.getGenerateClass().get()) {
                    var taskName = "generate" + variantNameCapitalized + "AutoResConfigSource";
                    TaskProvider<GenerateJavaTask> task = project.getTasks().register(taskName,
                            GenerateJavaTask.class, extension, locales, displayLocales);
                    variant.getSources().getJava().addGeneratedSourceDirectory(task, GenerateJavaTask::getOutputDir);
                }

                if (extension.getGenerateRes().get()) {
                    var taskName = "generate" + variantNameCapitalized + "AutoResConfigRes";
                    TaskProvider<GenerateResTask> task = project.getTasks().register(taskName,
                            GenerateResTask.class, extension, locales, displayLocales);
                    variant.getSources().getRes().addGeneratedSourceDirectory(task, GenerateResTask::getOutputDir);
                }

                if (extension.getGenerateLocaleConfig().get()) {
                    var taskName = "generate" + variantNameCapitalized + "AutoResConfigLocaleConfigRes";
                    TaskProvider<GenerateLocaleConfigResTask> task = project.getTasks().register(taskName,
                            GenerateLocaleConfigResTask.class, locales, displayLocales);
                    variant.getSources().getRes().addGeneratedSourceDirectory(task, GenerateLocaleConfigResTask::getOutputDir);
                }
            });
        });
    }
}
