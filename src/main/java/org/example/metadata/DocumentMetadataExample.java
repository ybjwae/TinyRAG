package org.example.metadata;


import org.springframework.ai.document.Document;

import java.util.HashMap;
import java.util.Map;

public class DocumentMetadataExample {

    public static Document createChunkWithDocMetadata(
            String content,
            String docId,
            String sourceUrl,
            String fileName) {

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("docId", docId);
        metadata.put("sourceUrl", sourceUrl);
        metadata.put("fileName", fileName);

        return new Document(content, metadata);
    }

    public static void main(String[] args) {
        String chunkContent = "自签收之日起 7 天内，商品未经使用且不影响二次销售的，"
                + "消费者可申请七天无理由退货。";

        Document chunk = createChunkWithDocMetadata(
                chunkContent,
                "doc_20260316_001",
                "https://docs.company.com/policy/return.pdf",
                "退货政策.pdf"
        );

        System.out.println("Chunk content: " + chunk.getText());
        System.out.println("Metadata: " + chunk.getMetadata());
    }
}
