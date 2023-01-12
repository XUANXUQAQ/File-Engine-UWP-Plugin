package file.engine.uwp;

import file.engine.uwp.info.UWPInfo;
import file.engine.uwp.utils.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.io.*;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import static file.engine.uwp.utils.RegexUtil.semicolon;

public class PluginMain extends Plugin {
    private ConcurrentHashMap<String, UWPInfo> uwpInfoMap = new ConcurrentHashMap<>();
    private String searchText;
    private String[] searchCase;
    private String[] keywords;
    private long startSearchTime;
    private boolean isStartSearch = false;
    private final ExecutorService threadPool = Executors.newCachedThreadPool();
    private final ImageIcon pluginIcon = new ImageIcon(Objects.requireNonNull(this.getClass().getResource("/grid.png")));
    private boolean exitFlag = false;
    private Color backgroundColor;
    private Color labelChosenColor;
    private Color labelFontColor;
    private Color highLightColor;
    private int pluginIconSideLength = 0;
    @SuppressWarnings("FieldCanBeLocal")
    private final String configsPath = "plugins/Plugin configuration files/uwp";

    /**
     * 当用户修改FIle-Engine的设置后，将调用此函数。
     *
     * @param configs configs
     */
    @Override
    public void configsChanged(Map<String, Object> configs) {
        backgroundColor = new Color((Integer) configs.get("defaultBackground"));
        labelChosenColor = new Color((Integer) configs.get("labelColor"));
        labelFontColor = new Color((Integer) configs.get("fontColor"));
        highLightColor = new Color((Integer) configs.get("fontColorWithCoverage"));
    }

    /**
     * 当搜索栏文本更改时，将调用此函数。
     *
     * @param text Example : 当在搜索框输入 "&gt;examplePlugin TEST" 时, 参数text值为 "TEST"
     */
    @Override
    public void textChanged(String text) {
        if (!text.isEmpty()) {
            clearResultQueue();
            final int i = text.lastIndexOf('|');
            if (i == -1) {
                searchText = text;
                searchCase = null;
            } else {
                searchText = text.substring(0, i);
                var searchCaseStr = text.substring(i + 1);
                if (searchCaseStr.isEmpty()) {
                    searchCase = null;
                } else {
                    String[] tmpSearchCase = semicolon.split(searchCaseStr);
                    searchCase = new String[tmpSearchCase.length];
                    for (int j = 0; j < tmpSearchCase.length; j++) {
                        searchCase[j] = tmpSearchCase[j].trim();
                    }
                }
            }
            keywords = semicolon.split(searchText);
            startSearchTime = System.currentTimeMillis();
            isStartSearch = true;
        }
    }

    /**
     * 启动File-Engine时，将调用该函数。
     * 您可以在此处初始化插件
     */
    @Override
    public void loadPlugin(Map<String, Object> configs) throws RuntimeException {
        checkEvent("file.engine.event.handler.impl.database.PrepareSearchEvent", Collections.emptyMap());
        checkEvent("file.engine.event.handler.impl.frame.searchBar.HideSearchBarEvent", Collections.emptyMap());
        File pluginFolder = new File(configsPath);
        if (!pluginFolder.exists()) {
            if (!pluginFolder.mkdirs()) {
                throw new RuntimeException("mkdir " + pluginFolder + "failed.");
            }
        }
        File dllFile = new File(pluginFolder, "getIndirectString.dll");
        try (BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(dllFile));
             BufferedInputStream in = new BufferedInputStream(Objects.requireNonNull(this.getClass().getResourceAsStream("/getIndirectString.dll")))) {
            in.transferTo(out);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.load(dllFile.getAbsolutePath());
        backgroundColor = new Color((Integer) configs.get("defaultBackground"));
        labelChosenColor = new Color((Integer) configs.get("labelColor"));
        labelFontColor = new Color((Integer) configs.get("fontColor"));
        highLightColor = new Color((Integer) configs.get("fontColorWithCoverage"));
        threadPool.submit(() -> {
            // 每隔5分钟更新uwp应用列表
            long startFetchUwpInfoTime = 0;
            final long timeout = 5 * 60 * 1000; // 5 min
            while (!exitFlag) {
                if (System.currentTimeMillis() - startFetchUwpInfoTime > timeout) {
                    startFetchUwpInfoTime = System.currentTimeMillis();
                    uwpInfoMap = UWPInfoUtil.getUwpInfo(uwpInfoMap);
                    for (UWPInfo v : uwpInfoMap.values()) {
                        if (v.getIcon() == null) {
                            try {
                                ImageIcon icon = GetIconUtil.getIcon(v);
                                v.setIcon(icon);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        if (exitFlag) {
                            break;
                        }
                    }
                }
                try {
                    TimeUnit.MILLISECONDS.sleep(50);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        threadPool.submit(() -> {
            final long timeout = 100;
            while (!exitFlag) {
                if (isStartSearch && System.currentTimeMillis() - startSearchTime > timeout) {
                    isStartSearch = false;
                    for (Map.Entry<String, UWPInfo> uwpInfo : uwpInfoMap.entrySet()) {
                        if (PathMatchUtil.check(uwpInfo.getValue().getDisplayName(), searchCase, searchText, keywords)) {
                            addToResultQueue(uwpInfo.getKey());
                        }
                    }
                }
                try {
                    TimeUnit.MILLISECONDS.sleep(50);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    /**
     * 当File-Engine关闭时，将调用该函数。
     */
    @Override
    public void unloadPlugin() {
        exitFlag = true;
    }

    /**
     * 当File-Engine进入插件模式后，每释放一次键盘该方法将会执行一次。有关按键释放事件的定义，请参见 swing KeyEvent 的类描述。
     * 请注意：由于上下键用于切换显示结果，所以按下键盘上下键不会触发该方法(key code 38和40不会触发)。
     *
     * @param e      KeyEvent, Which key on the keyboard is released.
     * @param result Currently selected content.
     */
    @Override
    public void keyReleased(KeyEvent e, String result) {
        if (e.getKeyCode() == KeyEvent.VK_ENTER) {
            UWPInfo uwpInfo = uwpInfoMap.get(result);
            if (uwpInfo != null) {
                sendEventToFileEngine("file.engine.event.handler.impl.frame.searchBar.HideSearchBarEvent");
                OpenUwpUtil.openUWP(uwpInfo);
            }
        }
    }

    /**
     * 当File-Engine进入插件模式后，每按下一次键盘该方法将会执行一次。有关按键按下事件的定义，请参见 swing KeyEvent 的类描述。
     * 请注意：由于上下键用于切换显示结果，所以按下键盘上下键不会触发该方法(key code 38和40不会触发)。
     *
     * @param e      KeyEvent, Which key on the keyboard is pressed.
     * @param result Currently selected content.
     */
    @Override
    public void keyPressed(KeyEvent e, String result) {

    }

    /**
     * 当File-Engine进入插件模式后，每按下和释放一次键盘该方法将会执行一次。有关按键事件的定义，请参见 swing KeyEvent 的类描述。
     * 请注意：由于上下键用于切换显示结果，所以按下键盘上下键不会触发该方法(key code 38和40不会触发)。
     *
     * @param e      KeyEvent, Which key on the keyboard is pressed.
     * @param result Currently selected content.
     */
    @Override
    public void keyTyped(KeyEvent e, String result) {

    }

    /**
     * 当File-Engine进入插件模式后，鼠标每点击窗口以下该方法将会执行一次。有关鼠标事件的定义，请参见swing MouseEvent的类描述。
     *
     * @param e      Mouse event
     * @param result Currently selected content.
     */
    @Override
    public void mousePressed(MouseEvent e, String result) {
        if (e.getClickCount() == 2) {
            UWPInfo uwpInfo = uwpInfoMap.get(result);
            if (uwpInfo != null) {
                sendEventToFileEngine("file.engine.event.handler.impl.frame.searchBar.HideSearchBarEvent");
                OpenUwpUtil.openUWP(uwpInfo);
            }
        }
    }

    /**
     * 当File-Engine进入插件模式后，鼠标每释放一次该方法将会执行一次。有关鼠标事件的定义，请参见swing MouseEvent的类描述。
     *
     * @param e      Mouse event
     * @param result Currently selected content
     */
    @Override
    public void mouseReleased(MouseEvent e, String result) {

    }

    /**
     * 当File-Engine的搜索框被打开该方法将会被调用一次。并不需要进入插件模式。
     *
     * @param showingMode 显示模式
     *                    <p>
     *                    目前File-Engine有两种模式：普通显示和贴靠资源管理器显示，对应的showingMode为 NORMAL_SHOWING， EXPLORER_ATTACH
     */
    @Override
    public void searchBarVisible(String showingMode) {

    }

    /**
     * 获取插件图标。它可以是png，jpg。
     * 尽量让图标变小，否则会占用太多内存。
     *
     * @return icon
     */
    @Override
    public ImageIcon getPluginIcon() {
        return pluginIcon;
    }

    /**
     * 获取插件的官方网站。
     *
     * @return official site
     */
    @Override
    public String getOfficialSite() {
        return "https://github.com/XUANXUQAQ/File-Engine-UWP-Plugin";
    }

    /**
     * 获取插件版本信息
     *
     * @return version
     */
    @Override
    public String getVersion() {
        return VersionUtil._getPluginVersion();
    }

    /**
     * 获取插件描述，插件的介绍信息以及使用方法。插件的介绍信息将会显示在设置界面。
     * 只需在外部写入描述，并将其粘贴然后return。
     *
     * @return description
     */
    @Override
    public String getDescription() {
        return "该插件用于支持uwp应用搜索\n图标来自：https://fonts.google.com/icons?selected=Material%20Icons%3Agrid_view%3A";
    }

    /**
     * 检查当前版本是否为最新版本。
     *
     * @return true or false
     * @see #getUpdateURL()
     */
    @Override
    @SuppressWarnings({"unused", "RedundantThrows"})
    public boolean isLatest() throws Exception {
        return VersionUtil._isLatest();
    }

    /**
     * 返回下载插件jar文件的url
     * 当**isLatest**返回false，将会弹出窗口询问用户是否更新，如果用户点击确定，则会调用该方法来下载新版本的插件。
     *
     * @return download url
     * @see #isLatest()
     */
    @Override
    public String getUpdateURL() {
        return VersionUtil._getUpdateURL();
    }

    /**
     * 当插件返回了结果到File-Engine，File-Engine尝试显示时，将会调用该方法。
     *
     * @param result   current selected content.
     * @param label    需要显示的JLabel.
     * @param isChosen 如果当前label是目前被用户选中的，您应该将标签设置为不同的背景，背景颜色可以通过loadPlugins和configsChanged方法的参数获得。
     *                 <p>
     *                 您只能设置JLabel的图标、文本和背景，请不要设置其他属性，如border，name以及其他
     */
    @Override
    public void showResultOnLabel(String result, JLabel label, boolean isChosen) {
        if (pluginIconSideLength == 0) {
            pluginIconSideLength = label.getHeight() / 3;
        }
        UWPInfo uwpInfo = uwpInfoMap.get(result);
        if (uwpInfo != null) {
            String displayName = uwpInfo.getDisplayName();
            String html = HighLightUtil.getHtml(displayName, uwpInfo.getName(), keywords, labelFontColor, highLightColor);
            label.setText(html);
            ImageIcon icon = uwpInfo.getIcon();
            if (icon == null) {
                label.setIcon(pluginIcon);
            } else {
                icon = GetIconUtil.changeIcon(icon, pluginIconSideLength, pluginIconSideLength);
                uwpInfo.setIcon(icon);
                label.setIcon(icon);
            }
        }
        if (isChosen) {
            label.setBackground(labelChosenColor);
        } else {
            label.setBackground(backgroundColor);
        }
    }

    /**
     * 获取插件的作者名，将会显示在插件的设置界面。
     *
     * @return author name
     */
    @Override
    public String getAuthor() {
        return "XUANXU";
    }


    /**
     * File-Engine中拥有一个事件处理系统，每一个事件被处理，该方法将会被调用一次。你可以在此处监听主程序当前处理了什么事件。也可以通过该接口实现多个插件互相通信。
     * 你可以通过 sendEventToFileEngine() 方法发送事件
     *
     * @param c             事件类
     * @param eventInstance 事件实例
     * @see #sendEventToFileEngine(String, Object...)
     * @see #sendEventToFileEngine(Event)
     */
    @SuppressWarnings("unchecked")
    @Override
    public void eventProcessed(Class<?> c, Object eventInstance) {
        if ("file.engine.event.handler.impl.database.PrepareSearchEvent".equals(c.getName())) {
            try {
                Class<?> superclass = c.getSuperclass();
                Field searchTextField = superclass.getDeclaredField("searchText");
                Field searchCaseField = superclass.getDeclaredField("searchCase");
                Field keywordsField = superclass.getDeclaredField("keywords");
                Supplier<String> searchTextSupplier = (Supplier<String>) searchTextField.get(eventInstance);
                Supplier<String[]> searchCaseSupplier = (Supplier<String[]>) searchCaseField.get(eventInstance);
                Supplier<String[]> keywordsSupplier = (Supplier<String[]>) keywordsField.get(eventInstance);
                searchText = searchTextSupplier.get();
                searchCase = searchCaseSupplier.get();
                keywords = keywordsSupplier.get();
                startSearchTime = System.currentTimeMillis();
                isStartSearch = true;
                clearResultQueue();
            } catch (NoSuchFieldException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    //--------------------------------------------------------------------------------------------------------------

    /**
     * Do Not Remove, this is used for File-Engine to get message from the plugin.
     * You can show message using "displayMessage(String caption, String message)"
     *
     * @return String[2], the first string is caption, the second string is message.
     * @see #displayMessage(String, String)
     */
    @SuppressWarnings("unused")
    public String[] getMessage() {
        return _getMessage();
    }

    /**
     * Do Not Remove, this is used for File-Engine to get results from the plugin
     * You can add result using "addToResultQueue(String result)".
     *
     * @return result
     * @see #addToResultQueue(String)
     */
    @SuppressWarnings("unused")
    public String pollFromResultQueue() {
        return _pollFromResultQueue();
    }

    /**
     * Do Not Remove, this is used for File-Engine to check the API version.
     *
     * @return Api version
     */
    @SuppressWarnings("unused")
    public int getApiVersion() {
        return _getApiVersion();
    }

    /**
     * Do Not Remove, this is used for File-Engine to clear results to prepare for the next time.
     *
     * @see #addToResultQueue(String)
     * @see #pollFromResultQueue()
     */
    @SuppressWarnings("unused")
    public void clearResultQueue() {
        _clearResultQueue();
    }

    /**
     * Do Not Remove, this is used for File-Engine to poll the event that send from the plugin.
     * The object array contains two parts.
     * object[0] contains the fully-qualified name of class.
     * object[1] contains the params that the event need to build an instance.
     * To send an event to File-Engine
     *
     * @return Event
     * @see #sendEventToFileEngine(String, Object...)
     * @see #sendEventToFileEngine(Event)
     */
    @SuppressWarnings("unused")
    public Object[] pollFromEventQueue() {
        return _pollFromEventQueue();
    }

    /**
     * Do Not Remove, this is used for File-Engine to replace the handler which the plugin is registered.
     * The object array contains two parts.
     * object[0] contains the fully-qualified name of class.
     * object[1] contains a consumer to hande the event.
     *
     * @return Event handler
     * @see #registerFileEngineEventHandler(String, BiConsumer)
     */
    @SuppressWarnings("unused")
    public Object[] pollFromEventHandlerQueue() {
        return _pollEventHandlerQueue();
    }

    /**
     * Do Not Remove, this is used for File-Engine to restore the handler which the plugin is registered.
     *
     * @return Event class fully-qualified name
     * @see #restoreFileEngineEventHandler(String)
     */
    @SuppressWarnings("unused")
    public String restoreFileEngineEventHandler() {
        return _pollFromRestoreQueue();
    }
}
