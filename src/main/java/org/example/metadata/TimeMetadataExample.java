package org.example.metadata;

import org.springframework.ai.document.Document;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class TimeMetadataExample {

    public static Document createChunkWithTime(
            String content,
            LocalDateTime createAt,
            LocalDateTime effectiveDate,
            LocalDateTime expirationDate) {

        Map<String, Object> metadata = new HashMap<>();

        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        metadata.put("created_at", createAt.format(formatter));

        if (effectiveDate != null) {
            metadata.put("effective_date", effectiveDate.format(formatter));
        }

        if (expirationDate != null) {
            metadata.put("expiration_date", expirationDate.format(formatter));
        }

        return new Document(content, metadata);
    }

    public static void main(String[] args) {
        String chunkContent = "员工差旅住宿费用报销上限：一线城市 500 元/晚，二线城市 300 元/晚。";

        Document chunk = createChunkWithTime(
                chunkContent,
                LocalDateTime.now(),
                LocalDateTime.of(2024, 4, 1, 0, 0),
                LocalDateTime.of(2024, 12, 31, 23, 59)
        );

        System.out.println("Chunk content: " + chunk.getText());
        System.out.println("Effective from: " + chunk.getMetadata().get("effective_date"));
        System.out.println("Expires at: " + chunk.getMetadata().get("expiration_date"));
    }
}
