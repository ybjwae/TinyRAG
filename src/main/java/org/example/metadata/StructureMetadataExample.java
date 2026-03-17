package org.example.metadata;

import org.springframework.ai.document.Document;

import java.util.HashMap;
import java.util.Map;

public class StructureMetadataExample {

    public static Document createChunkWithStructure(
            String content,
            String h1Title,
            String h2Title,
            int pageNumber) {

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("h1_title", h1Title);
        metadata.put("h2_title", h2Title);
        metadata.put("page_number", pageNumber);

        // 组合成完整的标题路径
        String fullTitle = h1Title + " > " + h2Title;
        metadata.put("title", fullTitle);

        return new Document(content, metadata);
    }

    public static void main(String[] args) {
        String chunkContent = "新员工试用期为 3 个月，试用期内工资为正式工资的 80%。";

        Document chunk = createChunkWithStructure(
                chunkContent,
                "第二章 员工入职",
                "2.1 试用期规定",
                5
        );

        System.out.println("Chunk content: " + chunk.getText());
        System.out.println("Title path: " + chunk.getMetadata().get("title"));
        System.out.println("Page: " + chunk.getMetadata().get("page_number"));
    }


}
