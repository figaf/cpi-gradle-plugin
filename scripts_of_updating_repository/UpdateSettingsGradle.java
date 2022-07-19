import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class UpdateSettingsGradle {

    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            throw new IllegalArgumentException("First argument of application should be a path to gradle settings file");
        }

        Path filePath = Paths.get(args[0]);
        String gradleSettings = new String(Files.readAllBytes(filePath), StandardCharsets.UTF_8);
        gradleSettings = gradleSettings.replaceAll(":iflow-(.*)\\(\"(.*)\\/(.*)\"\\)", ":iflow-$1(\"$2/IntegrationFlow/$3\")");
        gradleSettings = gradleSettings.replaceAll(":vm-(.*)\\(\"(.*)\\/(.*)\"\\)", ":vm-$1(\"$2/ValueMapping/$3\")");
        gradleSettings = gradleSettings.replaceAll(":sc-(.*)\\(\"(.*)\\/(.*)\"\\)", ":sc-$1(\"$2/ScriptCollection/$3\")");
        gradleSettings = gradleSettings.replaceAll(":mm-(.*)\\(\"(.*)\\/(.*)\"\\)", ":mm-$1(\"$2/MessageMapping/$3\")");
        Files.write(filePath, gradleSettings.getBytes(StandardCharsets.UTF_8), StandardOpenOption.WRITE);
    }
}
