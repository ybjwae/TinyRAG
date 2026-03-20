package org.example.metadata;

import org.springframework.ai.document.Document;

import java.util.HashMap;
import java.util.Map;

public class CustomMetadataExample {

    public static Document createChunkWithCustomMetadata(
            String content,
            String productCategory,
            String policyType,
            int priority) {

        Map<String, Object> metadata = new HashMap<>();

        // 通用元数据
        metadata.put("doc_id", "policy_001");
        metadata.put("create_at", "2026-03-20T10:00:00Z");

        //  业务自定义元数据
        metadata.put("product_category", productCategory);
        metadata.put("policy_type", policyType);
        metadata.put("priority", priority); // 优先级

        return new Document(content, metadata);
    }

    public static void main(String[] args) {
        String chunkContent = "生鲜食品、定制商品、贴身衣物等特殊品类不适用七天无理由退货。";

        Document chunk = createChunkWithCustomMetadata(
                chunkContent,
                "fresh_food",
                "return",
                1);

        System.out.println("Chunk content: " + chunk.getText());
        System.out.println("Product category: " + chunk.getMetadata().get("product_category"));
        System.out.println("Priority: " + chunk.getMetadata().get("priority"));
    }

}
