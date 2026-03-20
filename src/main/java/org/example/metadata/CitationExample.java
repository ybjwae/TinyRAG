package org.example.metadata;

import org.springframework.ai.document.Document;

import java.util.HashMap;
import java.util.Map;

public class CitationExample {

    /**
     *  从 chunk 元数据生成引用信息
     */
    public static String generateCitation(Document chunk) {
        Map<String, Object> metadata = chunk.getMetadata();

        String filename = (String) metadata.get("filename");
        String title = (String) metadata.get("title");
        Integer pageNumber = (Integer) metadata.get("page_number");
        String sourceUrl = (String) metadata.get("source_url");

        StringBuilder citation = new StringBuilder();
        citation.append("**依据**：");

        if (filename != null) {
            citation.append("《").append(filename).append("》");
        }

        if (title != null) {
            citation.append(" ").append(title);
        }

        if (pageNumber != null) {
            citation.append("，第").append(pageNumber).append("页");
        }

        if (sourceUrl != null) {
            citation.append("\n\n[查看原文]（").append(sourceUrl).append("）");
        }

        return citation.toString();
    }

    public static void main(String[] args) {
        //  模拟一个检索到的 chunk
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("filename", "员工手册.pdf");
        metadata.put("title", "第二章 员工入职 > 2.1 试用期规定");
        metadata.put("page_number", 5);
        metadata.put("source_url", "https://example.com/employee-handbook.pdf");

        Document chunk = new Document(
                "新员工试用期为 3 个月，试用期内工资为正式工资的 80%。",
                metadata);

        String citation = generateCitation(chunk);
        System.out.println("Answer: " + chunk.getText());
        System.out.println("\n" + citation);
    }

}
