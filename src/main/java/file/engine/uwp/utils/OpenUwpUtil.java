package file.engine.uwp.utils;

import file.engine.uwp.dllInterface.GetIndirectString;
import file.engine.uwp.info.UWPInfo;
import lombok.SneakyThrows;
import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import javax.swing.*;
import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class OpenUwpUtil {

    @SneakyThrows
    private static String getIndirectString(String packageFullName, String url) {
        String template = "@{%s? %s}"; //[packageFullName]? [ms-resource://...]
        String resourceIndirectString = String.format(template, packageFullName, url);
        return GetIndirectString.SHLoadIndirectString(resourceIndirectString);
    }

    // 当有多个入口进行选择
    @SneakyThrows
    public static void openUWP(UWPInfo uwpInfo) {
        String installLocation = UWPInfoUtil.getUwpDetail(uwpInfo, "InstallLocation");
        if (installLocation.isEmpty()) {
            return;
        }
        File appxManifest = new File(installLocation, "AppxManifest.xml");
        SAXReader saxReader = new SAXReader();
        Document doc = saxReader.read(appxManifest);
        Element rootElement = doc.getRootElement();
        Element applications = rootElement.element("Applications");
        Iterator<Element> applicationElementIterator = applications.elementIterator();
        // 第一个参数为familyName, 第二个为application Id
        String shellNameTemplate = "shell:AppsFolder\\%s!%s";
        HashMap<String, String> startMap = new HashMap<>();
        while (applicationElementIterator.hasNext()) {
            Element applicationEle = applicationElementIterator.next();
            Attribute id = applicationEle.attribute("Id");
            Element visualElements = applicationEle.element("VisualElements");
            Attribute displayName = visualElements.attribute("DisplayName");
            String displayNameVal = displayName.getValue();
            if (displayNameVal.startsWith("ms-resource:")) {
                displayNameVal = getIndirectString(uwpInfo.getFullName(), displayNameVal);
            }
            startMap.put(displayNameVal, String.format(shellNameTemplate, uwpInfo.getFamilyName(), id.getValue()));
        }
        String key = "";
        if (startMap.size() > 1) {
            Set<String> options = startMap.keySet();
            Object[] objects = options.toArray();
            key = (String) JOptionPane.showInputDialog(null,
                    "存在多个入口，请选择需要打开的程序",
                    "提示",
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    objects,
                    objects[0]);
        } else {
            for (String s : startMap.keySet()) {
                key = s;
            }
        }
        if (key.isEmpty()) {
            return;
        }
        String[] strings = {
                "explorer.exe", startMap.get(key)
        };
        Runtime.getRuntime().exec(strings);
    }

    /**
     * 遍历当前节点元素下面的所有(元素的)子节点
     */
    private static void arrayNodes(Map<String, Object> nodeMap, Element node) {
        System.out.println("----------------------------");
        // 当前节点的名称、文本内容和属性
        System.out.println("当前节点名称：" + node.getName());// 当前节点名称
        if (!(node.getTextTrim().equals(""))) {
            System.out.println("当前节点的内容：" + node.getTextTrim());// 当前节点名称
        }
        String nodeName = node.getName();
        String nodeValue = node.getTextTrim();
        nodeMap.put(nodeName, nodeValue);
        // 当前节点下面子节点迭代器
        Iterator<Element> it = node.elementIterator();
        // 遍历
        while (it.hasNext()) {
            // 获取某个子节点对象
            Element e = it.next();
            // 对子节点进行遍历
            arrayNodes(nodeMap, e);
        }
    }
}
