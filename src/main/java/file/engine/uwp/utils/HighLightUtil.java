package file.engine.uwp.utils;

import java.awt.*;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class HighLightUtil {

    /**
     * 高亮显示
     *
     * @param html     待处理的html
     * @param keywords 高亮关键字
     * @return 处理后带html
     */
    private static String highLight(String html, String[] keywords, Color highLightColor) {
        StringBuilder regexPatternBuilder = new StringBuilder();
        List<String> collect = Arrays.stream(keywords).sorted((o1, o2) -> o2.length() - o1.length()).collect(Collectors.toList());
        for (String keyword : collect) {
            if (!keyword.isBlank()) {
                if (".".equals(keyword)) {
                    keyword = "\\.";
                } else if (keyword.startsWith(File.separator)) {
                    continue;
                }
                regexPatternBuilder.append(keyword).append("|");
            }
        }
        if (PinyinUtil.isStringContainChinese(html)) {
            // 挑出所有的中文字符
            Map<String, String> chinesePinyinMap = PinyinUtil.getChinesePinyinMap(html);
            // 转换成拼音后和keywords匹配，如果发现匹配出成功，则添加到正则表达式中
            chinesePinyinMap.entrySet()
                    .stream()
                    .filter(pair -> Arrays.stream(keywords)
                            .anyMatch(each -> each.toLowerCase(Locale.ROOT).contains(pair.getValue().toLowerCase(Locale.ROOT))))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
                    .forEach((k, v) -> regexPatternBuilder.append(k).append("|"));
        }
        if (regexPatternBuilder.length() > 0) {
            String pattern = regexPatternBuilder.substring(0, regexPatternBuilder.length() - 1);
            Pattern compile = RegexUtil.getPattern(pattern, Pattern.CASE_INSENSITIVE);
            Matcher matcher = compile.matcher(html);
            html = matcher.replaceAll((matchResult) -> {
                String group = matchResult.group();
                String s = "#" + ColorUtil.parseColorHex(highLightColor);
                return "<span style=\"color: " + s + ";\">" + group + "</span>";
            });
        }
        return html;
    }

    /**
     * 根据path或command生成显示html
     *
     * @return html
     */
    public static String getHtml(String name, String secondLine, String[] keywords, Color normalColor, Color highLightColor) {
        String colorHex = "#" + ColorUtil.parseColorHex(normalColor);
        String template = "<html><body style=\"color: " + colorHex + ";\">%s</body></html>";
        return String.format(template, "<div>" +
                highLight(name, keywords, highLightColor) + "<br>" +
                "<div>" +
                secondLine +
                "</div>" +
                "</div>");
    }
}
