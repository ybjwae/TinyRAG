package org.example.metadata;

import org.springframework.ai.document.Document;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AccessControlExample {

    public static Document createChunkWithACL(
            String content,
            List<String> accessRoles,
            List<String> accessDepartments,
            String sensitivityLevel) {

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("access_roles", accessRoles);
        metadata.put("access_departments", accessDepartments);
        metadata.put("sensitivity_level", sensitivityLevel);

        return new Document(content, metadata);
    }

    public static void main(String[] args) {
        String chunkContent = "2024 年公司整体营收目标为 10 亿元，净利润目标为 2 亿元。";

        Document chunk = createChunkWithACL(
                chunkContent,
                Arrays.asList("ceo", "cfo", "finance_manager"),
                Arrays.asList("finance", "executive"),
                "confidential"
        );

        System.out.println("Chunk content: " + chunk.getText());
        System.out.println("Access roles: " + chunk.getMetadata().get("access_roles"));
        System.out.println("Sensitivity: " + chunk.getMetadata().get("sensitivity_level"));
    }
}
