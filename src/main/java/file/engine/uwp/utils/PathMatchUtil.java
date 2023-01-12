package file.engine.uwp.utils;

import java.io.File;

@SuppressWarnings({"IndexOfReplaceableByContains"})
public class PathMatchUtil {

    /**
     * 判断文件路径是否满足当前匹配结果（该方法由check方法使用），检查文件路径使用check方法。
     *
     * @param name         uwp文件名
     * @param isIgnoreCase 是否忽略大小写
     * @return 如果匹配成功则返回true
     * @see #check(String, String[], String, String[])  ;
     */
    private static boolean notMatched(String name, boolean isIgnoreCase, String[] keywords) {
        for (String eachKeyword : keywords) {
            if (eachKeyword == null || eachKeyword.isEmpty()) {
                continue;
            }
            char firstChar = eachKeyword.charAt(0);
            if (firstChar == '/' || firstChar == File.separatorChar) {
                continue;
            }
            //转换大小写
            if (isIgnoreCase) {
                name = name.toLowerCase();
                eachKeyword = eachKeyword.toLowerCase();
            }
            //开始匹配
            if (name.indexOf(eachKeyword) == -1) {
                if (PinyinUtil.isStringContainChinese(name)) {
                    String pinyin = PinyinUtil.toPinyin(name, "");
                    if (pinyin.indexOf(eachKeyword) == -1) {
                        return true;
                    }
                } else {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 检查文件路径是否匹配所有输入规则
     *
     * @param name uwp应用名称
     * @return true如果满足所有条件 否则false
     */
    public static boolean check(String name, String[] searchCase, String searchText, String[] keywords) {
        if (notMatched(name, true, keywords)) {
            return false;
        }
        if (searchCase == null) {
            return true;
        }
        for (String eachCase : searchCase) {
            switch (eachCase) {
                case "full":
                    if (!searchText.equalsIgnoreCase(name)) {
                        return false;
                    }
                    break;
                case "case":
                    if (notMatched(name, false, keywords)) {
                        return false;
                    }
            }
        }
        //所有规则均已匹配
        return true;
    }
}
