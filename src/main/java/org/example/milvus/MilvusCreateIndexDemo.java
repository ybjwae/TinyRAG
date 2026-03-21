package org.example.milvus;

import io.milvus.v2.client.ConnectConfig;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.common.IndexParam;
import io.milvus.v2.service.index.request.CreateIndexReq;

import java.util.List;
import java.util.Map;

public class MilvusCreateIndexDemo {

    public static void main(String[] args) {
        //  连接 Milvus
        ConnectConfig connectConfig = ConnectConfig.builder()
                .uri("http://192.168.75.134:19530")
                .build();
        MilvusClientV2 client = new MilvusClientV2(connectConfig);

        // 为向量字段创建 HNSW 索引
        IndexParam vectorIndex  = IndexParam.builder()
                .fieldName("vector")
                .indexType(IndexParam.IndexType.HNSW)
                .metricType(IndexParam.MetricType.COSINE) // 索引类型为 HNSW，距离度量为 Cosine
                .extraParams(Map.of(
                        "M", 16,        // 每个向量的最大连接数
                        "efConstruction", 256   // 建索引时的搜索宽度
                ))
                .build();

        // 为 category 标量字段创建倒排索引（加速过滤查询）
        IndexParam categoryIndex = IndexParam.builder()
                .fieldName("category")
                .indexType(IndexParam.IndexType.TRIE)
                .build();

        CreateIndexReq createIndexReq = CreateIndexReq.builder()
                .collectionName("customer_service_chunks")
                .indexParams(List.of(vectorIndex, categoryIndex))
                .build();
        client.createIndex(createIndexReq);
        System.out.println("索引创建成功");
    }
}
