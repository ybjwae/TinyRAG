package org.example.milvus;

import io.milvus.v2.client.ConnectConfig;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.service.collection.request.LoadCollectionReq;
import io.milvus.v2.service.vector.request.SearchReq;
import io.milvus.v2.service.vector.request.data.BaseVector;
import io.milvus.v2.service.vector.request.data.FloatVec;
import io.milvus.v2.service.vector.response.SearchResp;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.example.milvus.MilvusInsertDemo.getEmbedding;

public class MilvusDemo {

    public static void main(String[] args) throws IOException {
        // 连接 Milvus
        ConnectConfig connectConfig = ConnectConfig.builder()
                .uri("http://192.168.75.134:19530")
                .build();
        MilvusClientV2 client = new MilvusClientV2(connectConfig);

        // 加载 Collection
        client.loadCollection(LoadCollectionReq.builder()
                .collectionName("customer_service_chunks")
                .build());

        // 用户的问题
        String query = "买了东西不想要了怎么退货？";

        // 向量化用户问题
        List<List<Float>> queryVectors = getEmbedding(List.of(query));

        List<BaseVector> milvusQueryVectors = queryVectors.stream()
                .map(FloatVec::new)
                .collect(Collectors.toList());

        // 执行向量检索
        SearchReq searchReq = SearchReq.builder()
                .collectionName("customer_service_chunks")
                .data(milvusQueryVectors)           // 查询向量
                .topK(3)                      // 返回最相似的 3 个结果
                .outputFields(List.of("chunk_text", "doc_id", "category"))  // 需要返回的字段
                .annsField("vector")          // 指定在哪个向量字段上检索
                .filter("category == \"return_policy\"")  // 只搜索退货政策类
                .searchParams(Map.of("ef", 128))  // HNSW 检索时的搜索宽度
                .build();

        SearchResp searchResp = client.search(searchReq);

        // 输出检索结果
        List<List<SearchResp.SearchResult>> results = searchResp.getSearchResults();
        for (List<SearchResp.SearchResult> resultList : results) {
            System.out.println("=== 检索结果 ===");
            for (int i = 0; i < resultList.size(); i++) {
                SearchResp.SearchResult result = resultList.get(i);
                System.out.println("Top-" + (i + 1) + "：");
                System.out.println("  相似度分数：" + result.getScore());
                System.out.println("  分类：" + result.getEntity().get("category"));
                System.out.println("  文档ID：" + result.getEntity().get("doc_id"));
                System.out.println("  内容：" + result.getEntity().get("chunk_text"));
                System.out.println();
            }
        }
    }
}
