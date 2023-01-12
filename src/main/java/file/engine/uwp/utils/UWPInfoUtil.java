package file.engine.uwp.utils;

import file.engine.uwp.info.UWPInfo;
import lombok.SneakyThrows;

import java.io.*;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class UWPInfoUtil {


    @SuppressWarnings("DuplicatedCode")
    public static ConcurrentHashMap<String, UWPInfo> getUwpInfo(Map<String, UWPInfo> lastMap) {
        ConcurrentHashMap<String, UWPInfo> uwpInfos = new ConcurrentHashMap<>();
        String tempPath = System.getProperty("java.io.tmpdir");
        File psFile = new File(tempPath, "getuwp.ps1");
        try (BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(psFile));
             BufferedInputStream in = new BufferedInputStream(Objects.requireNonNull(UWPInfoUtil.class.getResourceAsStream("/getuwp.ps1")))) {
            in.transferTo(out);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try {
            ProcessBuilder psProcessBuilder = new ProcessBuilder("powershell.exe", "-File", psFile.getAbsolutePath());
            Process psProcess = psProcessBuilder.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(psProcess.getInputStream(), System.getProperty("sun.jnu.encoding")))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isEmpty()) {
                        continue;
                    }
                    // 开始获取
                    ArrayList<String> strings = new ArrayList<>();
                    strings.add(line);
                    while ((line = reader.readLine()) != null) {
                        if (line.isEmpty()) {
                            break;
                        }
                        strings.add(line);
                    }
                    int size = strings.size();
                    UWPInfo uwpInfo = new UWPInfo();
                    for (int i = 0; i < 11; i++) {
                        StringBuilder eachInfo = new StringBuilder(strings.get(i));
                        int j;
                        for (j = i + 1; j < size; j++) {
                            String nextStr = strings.get(j);
                            if (!nextStr.contains(": ")) {
                                eachInfo.append(nextStr.trim());
                            } else {
                                if (j != i + 1) {
                                    i = j;
                                }
                                break;
                            }
                        }
                        String[] split = RegexUtil.getPattern(": ", 0).split(eachInfo);
                        if (split.length > 1) {
                            injectField(uwpInfo, split[0].trim(), split[1].trim());
                        } else {
                            injectField(uwpInfo, split[0].trim(), "");
                        }
                    }
                    String key = uwpInfo.toString();
                    uwpInfos.put(key, uwpInfo);
                    if (lastMap.containsKey(key)) {
                        UWPInfo lastUwpInfo = lastMap.get(key);
                        uwpInfo.setIcon(lastUwpInfo.getIcon());
                    }
                }
            }
            psProcess.waitFor();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        return uwpInfos;
    }

    @SneakyThrows
    private static void injectField(UWPInfo entity, String name, String value) {
        Field declaredField = entity.getClass().getDeclaredField(name);
        declaredField.setAccessible(true);
        declaredField.set(entity, value);
    }
}
