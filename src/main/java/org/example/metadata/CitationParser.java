package org.example.metadata;

import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CitationParser {

    /**
     *  从模型回答中提取所有引用编号
     */
    public static Set<Integer> extractCitations(String answer) {
        Set<Integer> citations = new TreeSet<>();
        Pattern pattern = Pattern.compile("\\[(\\d+)]");
        Matcher matcher = pattern.matcher(answer);
        while (matcher.find()) {
            citations.add(Integer.parseInt(matcher.group(1)));
        }
        return citations;
    }

    /**
     * 将引用编号替换为可点击的链接（HTML 格式）
     */
    public static String renderCitations(String answer, Map<Integer, ChunkMeta> chunkMetaMap) {
        Pattern pattern = Pattern.compile("\\[(\\d+)]");
        Matcher matcher = pattern.matcher(answer);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            int index = Integer.parseInt(matcher.group(1));
            ChunkMeta meta = chunkMetaMap.get(index);
            if (meta != null) {
                String link = String.format(
                        "<a href=\"%s\" title=\"%s\" class=\"citation\">[%d]</a>",
                        meta.getSourceUrl(), meta.getSource(), index
                );
                matcher.appendReplacement(result, link);
            }
        }
        matcher.appendTail(result);
        return result.toString();
    }

    static class ChunkMeta {
        private String source;      // 来源文档名
        private String sourceUrl;   // 原文链接
        private String updateTime;  // 更新时间

        public ChunkMeta(String source, String sourceUrl, String updateTime) {
            this.source = source;
            this.sourceUrl = sourceUrl;
            this.updateTime = updateTime;
        }

        public String getSource() {
            return source;
        }

        public String getSourceUrl() {
            return sourceUrl;
        }

        public String getUpdateTime() {
            return updateTime;
        }
    }

}
