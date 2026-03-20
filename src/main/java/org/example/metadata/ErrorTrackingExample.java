package org.example.metadata;

import org.springframework.ai.document.Document;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ErrorTrackingExample {

    /**
     * 根据关键词查找可能有问题的 chunks
     */
    public static List<Document> findChunksByKeyword(
            List<Document> allChunks,
            String keyword) {

        List<Document> results = new ArrayList<>();
        for (Document chunk : allChunks) {
            if (chunk.getText().contains(keyword)) {
                results.add(chunk);
            }
        }
        return results;
    }

    /**
     *  展示 chunk 的详细信息，方便人工审核
     */
    public static void displayChunkDetails(Document chunk) {
        Map<String, Object> metadata = chunk.getMetadata();

        System.out.println("=== Chunk 详情 ===");
        System.out.println("内容: " + chunk.getText());
        System.out.println("文档 ID: " + metadata.get("doc_id"));
        System.out.println("文件名: " + metadata.get("file_name"));
        System.out.println("Chunk 序号: " + metadata.get("chunk_index"));
        System.out.println("原文位置: " + metadata.get("start_offset")
                + " - " + metadata.get("end_offset"));
        System.out.println("创建时间: " + metadata.get("created_at"));
        System.out.println("来源: " + metadata.get("source_url"));
        System.out.println();
    }

    /**
     *  模拟更新 chunk(实际项目中需要调用向量数据库的 api)
     */
    public static void updateChunk(Document chunk, String newContent) {
        System.out.println(">>> 更新 Chunk");
        System.out.println("原内容: " + chunk.getText());
        System.out.println("新内容: " + newContent);
        System.out.println("文档 ID: " + chunk.getMetadata().get("doc_id"));
        System.out.println("Chunk 序号: " + chunk.getMetadata().get("chunk_index"));
        System.out.println();
    }


    public static void main(String[] args) {
        // 模拟知识库中的 chunks
        List<Document> allChunks = new ArrayList<>();

        Map<String, Object> meta1 = new HashMap<>();
        meta1.put("doc_id", "doc_reimbursement_v1");
        meta1.put("file_name", "报销流程.pdf");
        meta1.put("chunk_index", 2);
        meta1.put("start_offset", 150);
        meta1.put("end_offset", 220);
        meta1.put("created_at", "2023-06-01T10:00:00Z");
        meta1.put("source_url", "https://docs.company.com/reimbursement_v1.pdf");
        allChunks.add(new Document(
                "报销时需打印发票并贴在报销单上，提交给财务部审核。",
                meta1
        ));

        Map<String, Object> meta2 = new HashMap<>();
        meta2.put("doc_id", "doc_reimbursement_v2");
        meta2.put("file_name", "报销流程_v2.pdf");
        meta2.put("chunk_index", 1);
        meta2.put("start_offset", 80);
        meta2.put("end_offset", 140);
        meta2.put("created_at", "2024-01-15T14:00:00Z");
        meta2.put("source_url", "https://docs.company.com/reimbursement_v2.pdf");
        allChunks.add(new Document(
                "报销时在系统中上传电子发票，无需打印纸质版。",
                meta2
        ));

        // 场景：用户反馈"贴发票"的说法已经过时
        System.out.println(">>> 用户反馈：系统说要贴发票，但新流程不需要了\n");

        // 1. 查找包含“发票”的 chunks
        List<Document> suspectedChunks = findChunksByKeyword(allChunks, "发票");
        System.out.println("找到 " + suspectedChunks.size() + " 个相关 chunks:\n");

        // 2. 展示 chunk 的详细信息，供人工审核
        for (Document chunk : suspectedChunks) {
            displayChunkDetails(chunk);
        }

        // 3. 人工确认第一个 chunk 是过时的，需要删除或者更新
        Document outdatedChunk = suspectedChunks.get(0);
        System.out.println(">>> 确认 chunk_index=2 的内容已过时，准备删除\n");

        // 4.  执行删除（实际项目中调用向量数据库的删除 api）
        System.out.println(">>> 删除过时 chunk:");
        System.out.println("文档 ID: " + outdatedChunk.getMetadata().get("doc_id"));
        System.out.println("Chunk 序号: " + outdatedChunk.getMetadata().get("chunk_index"));
        System.out.println("\n>>> 操作完成，问题已修正");
    }

}
