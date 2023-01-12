package file.engine.uwp.utils;

import file.engine.uwp.info.UWPInfo;
import lombok.SneakyThrows;
import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.io.File;
import java.util.Iterator;

public class OpenUwpUtil {

    // TODO 当有多个入口进行选择
    @SneakyThrows
    public static void openUWP(UWPInfo uwpInfo) {
        File appxManifest = UWPInfoUtil.getAppManifest(uwpInfo);
        if (appxManifest == null) {
            return;
        }
        SAXReader saxReader = new SAXReader();
        Document doc = saxReader.read(appxManifest);
        Element rootElement = doc.getRootElement();
        Element applications = rootElement.element("Applications");
        Iterator<Element> elementIterator = applications.elementIterator();
        if (elementIterator.hasNext()) {
            Element applicationEle = elementIterator.next();
            Attribute id = applicationEle.attribute("Id");
            String shellName = String.format("shell:AppsFolder\\%s!%s", uwpInfo.getFamilyName(), id.getValue());
            String[] strings = {
                    "explorer.exe", shellName
            };
            Runtime.getRuntime().exec(strings);
        }
    }
}
