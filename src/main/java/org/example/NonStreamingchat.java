package org.example;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import okhttp3.*;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class NonStreamingchat {

    // api 地址
    private  static final String API_URL = "https://api.siliconflow.cn/v1/chat/completions";

    // api key
    private static final String API_KEY = "YOUR_API_KEY";

    public static void main(String[] args) throws IOException {

        // 1. 构建请求体 JSON
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", "Qwen/Qwen3-32B");
        requestBody.addProperty("temperature", 0.2);
        requestBody.addProperty("max_tokens", 1024);
        requestBody.addProperty("stream", false);

        // 构建 message 数组
        JsonArray messages = new JsonArray();

        // 构建 system message: 定义模型的行为规则
        JsonObject systemMessage = new JsonObject();
        systemMessage.addProperty("role", "system");
        systemMessage.addProperty("content", "你是一个专业的电商客服助手，回答要简洁明了。");
        messages.add(systemMessage);
        
        // 构建 user message: 描述用户意图
        JsonObject userMessage = new JsonObject();
        userMessage.addProperty("role", "user");
        userMessage.addProperty("content", "买了一周的东西还能退吗？");
        messages.add(userMessage);
        
        requestBody.add("messages", messages);
        
        // 2. 创建 OkHttp 客户端（设置超时时间，应对大模型响应可能较慢）
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build();

        // 3. 构建 HTTP 请求
        Request request = new Request.Builder()
                .url(API_URL)
                .header("Authorization", "Bearer " + API_KEY)
                .header("Content-Type", "application/json")
                .post(RequestBody.create(
                        requestBody.toString(),
                        MediaType.parse("application/json")
                ))
                .build();

        // 4. 发送 HTTP 请求并获取响应
        try (Response response = client.newCall(request).execute()){
            if (!response.isSuccessful()) {
                System.out.println("请求失败，状态码：" + response.code());
                System.out.println("错误信息：" + response.body().string());
                return;
            }

            // 5. 解析 JSON 响应
            String responseBody = response.body().string();
            Gson gson = new Gson();
            JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);

            // 提取模型的回答
            String answer = jsonResponse
                    .getAsJsonArray("choices")
                    .get(0).getAsJsonObject()
                    .getAsJsonObject("message")
                    .get("content").getAsString();

            // 提取 finish_reason
            String finishReason = jsonResponse
                    .getAsJsonArray("choices")
                    .get(0).getAsJsonObject()
                    .get("finish_reason").getAsString();

            // 提取 token 用量
            JsonObject usage = jsonResponse.getAsJsonObject("usage");
            int promptTokens = usage.get("prompt_tokens").getAsInt();
            int completionTokens = usage.get("completion_tokens").getAsInt();
            int totalTokens = usage.get("total_tokens").getAsInt();

            // 6. 打印结果
            System.out.println("=== 模型回答 ===");
            System.out.println(answer);
            System.out.println();
            System.out.println("=== 调用信息 ===");
            System.out.println("结束原因：" + finishReason);
            System.out.println("输入 Token：" + promptTokens);
            System.out.println("输出 Token：" + completionTokens);
            System.out.println("总 Token：" + totalTokens);
        }
    }
}
