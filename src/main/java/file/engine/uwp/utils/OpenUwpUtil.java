package file.engine.uwp.utils;

import lombok.SneakyThrows;

public class OpenUwpUtil {

    @SneakyThrows
    public static void openUWP(String uwpFamilyName) {
        String shellName = String.format("shell:AppsFolder\\%s!App", uwpFamilyName);
        String[] strings = {
                "explorer.exe", shellName
        };
        Runtime.getRuntime().exec(strings);
    }
}
