package file.engine.uwp.utils;

import file.engine.uwp.info.UWPInfo;
import lombok.SneakyThrows;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class GetIconUtil {
    private static final int MAX_WIDTH = 200;
    private static final int MAX_HEIGHT = 200;


    @SuppressWarnings("DuplicatedCode")
    @SneakyThrows
    public static ImageIcon getIcon(UWPInfo uwpInfo) {
        ProcessBuilder psProcessBuilder = new ProcessBuilder("powershell.exe", "Get-AppxPackage", "-Name", uwpInfo.getName());
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
                    if ("InstallLocation".equals(split[0].trim()) && split.length > 1) {
                        String installLocation = split[1].trim();
                        File appxManifest = new File(installLocation, "AppxManifest.xml");
                        SAXReader saxReader = new SAXReader();
                        Document doc = saxReader.read(appxManifest);
                        Element rootElement = doc.getRootElement();
                        Element properties = rootElement.element("Properties");
                        Element logo = properties.element("Logo");
                        String logoPath = logo.getStringValue();
                        File file = new File(installLocation, logoPath);
                        File logoLocation = file.getParentFile();
                        String logoName = file.getName();
                        String logoNamePrefix = removeFileNameSuffix(logoName);
                        logoNamePrefix += ".scale-";
                        File[] logoFiles = logoLocation.listFiles();
                        if (logoFiles == null || logoFiles.length == 0) {
                            return null;
                        }
                        for (File listFile : logoFiles) {
                            String logoFileName = listFile.getName();
                            if (logoFileName.startsWith(logoNamePrefix)) {
                                return changeIcon(new ImageIcon(listFile.getAbsolutePath()), MAX_WIDTH, MAX_HEIGHT);
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    private static String removeFileNameSuffix(String fileName) {
        int pos = fileName.lastIndexOf('.');
        return fileName.substring(0, pos);
    }

    public static ImageIcon changeIcon(ImageIcon icon, int width, int height) {
        try {
            Image image = icon.getImage().getScaledInstance(width, height, Image.SCALE_DEFAULT);
            return new ImageIcon(image);
        } catch (NullPointerException e) {
            return null;
        }
    }
}
