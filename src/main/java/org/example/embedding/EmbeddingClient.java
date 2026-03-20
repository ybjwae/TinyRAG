package org.example.embedding;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class EmbeddingClient {

    private static final String API_URL = "https://api.siliconflow.cn/v1/embeddings";
    private static final String MODEL = "Qwen/Qwen3-Embedding-8B";

    private final String apiKey;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public EmbeddingClient(String apiKey) {
        this.apiKey = apiKey;
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 将一组文本转成向量
     *
     * @param texts 要向量化的文本列表
     * @return 每段文本对应的向量（double 数组）
     */
    public List<double[]> embed(List<String> texts) throws Exception {
        // 构造请求体
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", MODEL);
        requestBody.put("input", texts);

        String jsonBody = objectMapper.writeValueAsString(requestBody);

        // 发送 HTTP 请求
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("API 调用失败，状态码：" + response.statusCode()
                    + "，响应：" + response.body());
        }

        // 解析响应 提取向量
        JsonNode root = objectMapper.readTree(response.body());
        JsonNode dataArray = root.get("data");

        List<double[]> embeddings = new ArrayList<>();
        for (JsonNode item : dataArray) {
            JsonNode embeddingNode = item.get("embedding");
            double[] vector = new double[embeddingNode.size()];
            for (int i = 0; i < embeddingNode.size(); i++) {
                vector[i] = embeddingNode.get(i).asDouble();
            }
            embeddings.add(vector);
        }

        return embeddings;
    }

    /**
     * 将单段文本转成向量（便捷方法）
     */
    public double[] embed(String text) throws Exception {
        return embed(List.of(text)).get(0);
    }

    /**
     * 批量向量化，按批次处理，避免触发 API 的速率限制
     *
     * @param texts 要向量化的文本列表
     * @param batchSize 每批次处理的文本数量（建议 20~50）
     * @return 所有文本对应的向量（double 数组）
     */
    public List<double[]> embedInBatches(List<String> texts, int batchSize) throws Exception {
        List<double[]> allEmbeddings = new ArrayList<>();

        for (int i = 0; i < texts.size(); i += batchSize) {
            int end = Math.min(i + batchSize, texts.size());
            List<String> batch = texts.subList(i, end);

            System.out.printf("向量化进度：%d/%d%n", end, texts.size());
            List<double[]> batchEmbeddings = embed(batch);
            allEmbeddings.addAll(batchEmbeddings);

            // 简单的限流：每批之间等一下，避免触发 API 的速率限制
            if (end < texts.size()) {
                Thread.sleep(200);
            }
        }

        return allEmbeddings;
    }

    /**
     * 批量向量化，并发处理，避免触发 API 的速率限制
     *
     * @param texts 要向量化的文本列表
     * @param batchSize 每批次处理的文本数量（建议 20~50）
     * @param maxConcurrently 最大并发数量（一般 3~5）
     * @return 所有文本对应的向量（double 数组）
     */
    public List<double[]> embedConcurrently(List<String> texts, int batchSize,
                                            int maxConcurrently) throws Exception {

        ExecutorService executor = Executors.newFixedThreadPool(maxConcurrently);
        List<Future<List<double[]>>> futures = new ArrayList<>();

        for (int i = 0; i < texts.size(); i += batchSize) {
            int start = i;
            int end = Math.min(i + batchSize, texts.size());
            List<String> batch = texts.subList(start, end);

            futures.add(executor.submit(() -> embed(batch)));
        }

        List<double[]> allEmbeddings = new ArrayList<>();
        for (Future<List<double[]>> future : futures) {
            allEmbeddings.addAll(future.get());
        }

        executor.shutdown();
        return allEmbeddings;
    }

    public List<double[]> embedWithRetry(List<String> texts, int maxRetries) throws Exception {
        Exception lastException = null;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                return embed(texts);
            }catch (Exception e) {
                lastException = e;
                System.err.printf("第 %d 次调用失败：%s，%s%n",
                        attempt, e.getMessage(),
                        attempt < maxRetries ? "正在重试..." : "重试失败");

                if (attempt < maxRetries) {
                    // 指数退避：第 1 次等 1 秒，第 2 次等 2 秒，第 3 次等 4 秒
                    Thread.sleep(1000L * (1 << (attempt - 1)));
                }
            }
        }

        throw new RuntimeException("向量化失败，已重试 " + maxRetries + " 次", lastException);
    }
}
