package file.engine.uwp.utils;

import file.engine.uwp.info.UWPInfo;
import lombok.SneakyThrows;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import javax.swing.*;
import java.awt.*;
import java.io.File;

public class GetIconUtil {
    private static final int MAX_WIDTH = 200;
    private static final int MAX_HEIGHT = 200;


    @SneakyThrows
    public static ImageIcon getIcon(UWPInfo uwpInfo) {
        String installLocation = UWPInfoUtil.getUwpDetail(uwpInfo, "InstallLocation");
        if (installLocation.isEmpty()) {
            return null;
        }
        File appManifest = new File(installLocation, "AppxManifest.xml");
        return getIconFromManifest(appManifest);
    }

    private static ImageIcon getIconFromManifest(File appxManifest) throws DocumentException {
        String installLocation = appxManifest.getParentFile().getAbsolutePath();
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
        if (logoFiles == null) {
            return null;
        }
        for (File listFile : logoFiles) {
            String logoFileName = listFile.getName();
            if (logoFileName.startsWith(logoNamePrefix)) {
                return changeIcon(new ImageIcon(listFile.getAbsolutePath()), MAX_WIDTH, MAX_HEIGHT);
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
