package org.example.metadata;

import org.springframework.ai.document.Document;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PositionMetadataExample {

    /**
     * 对文本进行分块，并记录每个 chunk 的位置信息
     */
    public static List<Document> chunkWithPosition(String fullText, int chunkSize, int overlap) {

        List<Document> chunks = new ArrayList<>();
        int step = chunkSize - overlap;
        int start = 0;
        int chunkIndex = 0;

        while (start < fullText.length()) {
            int end = Math.min(start + chunkSize, fullText.length());
            String chunk = fullText.substring(start, end);

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("start_offset", start);
            metadata.put("end_offset", end);
            metadata.put("chunk_index", chunkIndex);
            metadata.put("total_length", fullText.length());

            chunks.add(new Document(chunk, metadata));

            start += step;
            chunkIndex++;
        }

        return chunks;
    }

    public static void main(String[] args) {
        String fullText = "自签收之日起 7 天内，商品未经使用且不影响二次销售的，"
                + "消费者可申请七天无理由退货。生鲜食品、定制商品、贴身衣物等"
                + "特殊品类不适用此规则，具体以商品详情页标注为准。";

        List<Document> chunks = chunkWithPosition(fullText, 40, 10);

        for (Document chunk : chunks) {
            System.out.println("=== Chunk " + chunk.getMetadata().get("chunk_index") + " ===");
            System.out.println("Content: " + chunk.getText());
            System.out.println("Position: " + chunk.getMetadata().get("start_offset")
                    + " - " + chunk.getMetadata().get("end_offset"));
            System.out.println();
        }
    }
}
