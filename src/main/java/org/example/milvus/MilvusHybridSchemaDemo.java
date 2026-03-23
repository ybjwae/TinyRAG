package org.example.milvus;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.milvus.common.clientenum.FunctionType;
import io.milvus.v2.client.ConnectConfig;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.common.ConsistencyLevel;
import io.milvus.v2.common.DataType;
import io.milvus.v2.common.IndexParam;
import io.milvus.v2.service.collection.request.AddFieldReq;
import io.milvus.v2.service.collection.request.CreateCollectionReq;
import io.milvus.v2.service.collection.request.HasCollectionReq;
import io.milvus.v2.service.collection.request.LoadCollectionReq;
import io.milvus.v2.service.index.request.CreateIndexReq;
import io.milvus.v2.service.vector.request.AnnSearchReq;
import io.milvus.v2.service.vector.request.HybridSearchReq;
import io.milvus.v2.service.vector.request.InsertReq;
import io.milvus.v2.service.vector.request.SearchReq;
import io.milvus.v2.service.vector.request.data.EmbeddedText;
import io.milvus.v2.service.vector.request.data.FloatVec;
import io.milvus.v2.service.vector.request.ranker.RRFRanker;
import io.milvus.v2.service.vector.response.InsertResp;
import io.milvus.v2.service.vector.response.SearchResp;
import lombok.SneakyThrows;
import okhttp3.*;

import java.io.IOException;
import java.util.*;

public class MilvusHybridSchemaDemo {

    private static final String COLLECTION = "customer_service_hybrid";

    private static final String SILICONFLOW_API_KEY = "sk-xxxxxxxxxxxxxxxxxxxxx";
    private static final String EMBEDDING_URL = "https://api.siliconflow.cn/v1/embeddings";
    private static final String EMBEDDING_MODEL = "Qwen/Qwen3-Embedding-8B";

    private static final Gson GSON = new Gson();
    private static final OkHttpClient HTTP_CLIENT = new OkHttpClient();

    /** 三种检索模式 */
    public enum SearchMode {
        DENSE_ONLY,     // 纯向量检索
        SPARSE_ONLY,    // 纯 BM25 检索
        HYBRID          // Dense + Sparse 混合检索
    }

    /** 检索参数配置 */
    public static class SearchConfig {
        public int denseRecallTopK = 20;
        public int sparseRecallTopK = 20;
        public int finalTopK = 8;

        public int nprobe = 16;
        public double dropRatioSearch = 0.2;
        public int rrfK = 60;

        public List<String> outFields = List.of("text");
        public ConsistencyLevel consistencyLevel = ConsistencyLevel.BOUNDED;

        public static SearchConfig defaults() {
            return new SearchConfig();
        }
    }

    public static void main(String[] args) {
        MilvusClientV2 client = new MilvusClientV2(ConnectConfig.builder()
                .uri("http://192.168.75.134:19530")
                .build());

        createCollectionIfAbsentAndLoad(client);

        String query = "订单号 2026012345 的物流状态";
        SearchConfig cfg = SearchConfig.defaults();

        // 依次跑三种模式做对比
        for (SearchMode mode : SearchMode.values()) {
            SearchResp resp = runSearch(client, query, mode, cfg);
            printSearchResults(resp, mode);
        }
    }

        // ==================== Collection 创建与数据加载 ====================
        
    public static void createCollectionIfAbsentAndLoad(MilvusClientV2 client) {
        Boolean exists = client.hasCollection(
                HasCollectionReq.builder().collectionName(COLLECTION).build()
        );

        if (!Boolean.TRUE.equals(exists)) {
            // 1) Schema
            CreateCollectionReq.CollectionSchema schema = client.createSchema();

            schema.addField(AddFieldReq.builder()
                    .fieldName("id").dataType(DataType.Int64)
                    .isPrimaryKey(true).autoID(true).build());

            schema.addField(AddFieldReq.builder()
                    .fieldName("text").dataType(DataType.VarChar)
                    .maxLength(8192).enableAnalyzer(true).build());

            schema.addField(AddFieldReq.builder()
                    .fieldName("text_dense").dataType(DataType.FloatVector)
                    .dimension(4096).build());

            schema.addField(AddFieldReq.builder()
                    .fieldName("text_sparse").dataType(DataType.SparseFloatVector).build());

            schema.addFunction(CreateCollectionReq.Function.builder()
                    .functionType(FunctionType.BM25)
                    .name("text_bm25_emb")
                    .inputFieldNames(List.of("text"))
                    .outputFieldNames(List.of("text_sparse"))
                    .build());

            // 2) Create collection
            client.createCollection(CreateCollectionReq.builder()
                    .collectionName(COLLECTION).collectionSchema(schema).build());

            // 3) Index
            IndexParam denseIndex = IndexParam.builder()
                    .fieldName("text_dense")
                    .indexType(IndexParam.IndexType.AUTOINDEX)
                    .metricType(IndexParam.MetricType.COSINE).build();

            IndexParam sparseIndex = IndexParam.builder()
                    .fieldName("text_sparse")
                    .indexType(IndexParam.IndexType.AUTOINDEX)
                    .metricType(IndexParam.MetricType.BM25).build();

            client.createIndex(CreateIndexReq.builder()
                    .collectionName(COLLECTION)
                    .indexParams(List.of(denseIndex, sparseIndex)).build());

            // 4) Insert demo data
            List<JsonObject> rows = Arrays.asList(
                    buildRow("订单号 2026012345 的物流状态：已发货，预计 1 月 28 日送达，承运商顺丰速运。"),
                    buildRow("物流规则总述：标准订单 48 小时内发货，偏远地区可能延迟 1-2 天。"),
                    buildRow("发货时效说明：付款成功后，普通商品 24-48 小时内发货，预售商品以详情页为准。"),
                    buildRow("异常签收处理：如包裹显示已签收但未收到，请在 48 小时内联系客服核实。"),
                    buildRow("订单查询入口：登录 APP → 我的订单 → 输入订单号即可查看物流详情。"),
                    buildRow("退货政策：收到商品 7 天内可申请无理由退货，需保持商品完好。")
            );

            InsertResp insertResp = client.insert(InsertReq.builder()
                    .collectionName(COLLECTION).data(rows).build());
            System.out.println("插入数据条数：" + insertResp.getInsertCnt());
        }

        client.loadCollection(LoadCollectionReq.builder()
                .collectionName(COLLECTION).build());
        System.out.println("Collection 已就绪并加载：" + COLLECTION);
    }

        // ==================== 三种检索模式 ====================
        
    @SneakyThrows
    public static SearchResp runSearch(MilvusClientV2 client,
                                       String queryText,
                                       SearchMode mode,
                                       SearchConfig cfg) {
        return switch (mode) {
            case DENSE_ONLY -> runDenseOnly(client, queryText, cfg);
            case SPARSE_ONLY -> runSparseOnly(client, queryText, cfg);
            default -> runHybrid(client, queryText, cfg);
        };
    }

    /** 纯向量检索 */
    private static SearchResp runDenseOnly(MilvusClientV2 client,
                                           String queryText,
                                           SearchConfig cfg) throws IOException {
        List<Float> queryVec = getEmbedding(queryText);
        Map<String, Object> params = new HashMap<>();
        params.put("metric_type", "COSINE");
        params.put("nprobe", cfg.nprobe);

        return client.search(SearchReq.builder()
                .collectionName(COLLECTION)
                .annsField("text_dense")
                .data(Collections.singletonList(new FloatVec(queryVec)))
                .topK(cfg.finalTopK)
                .outputFields(cfg.outFields)
                .searchParams(params)
                .consistencyLevel(cfg.consistencyLevel)
                .build());
    }

    /** 纯 BM25 检索 */
    private static SearchResp runSparseOnly(MilvusClientV2 client,
                                            String queryText,
                                            SearchConfig cfg) {
        Map<String, Object> params = new HashMap<>();
        params.put("metric_type", "BM25");
        params.put("drop_ratio_search", cfg.dropRatioSearch);

        return client.search(SearchReq.builder()
                .collectionName(COLLECTION)
                .annsField("text_sparse")
                .data(Collections.singletonList(new EmbeddedText(queryText)))
                .topK(cfg.finalTopK)
                .outputFields(cfg.outFields)
                .searchParams(params)
                .consistencyLevel(cfg.consistencyLevel)
                .build());
    }

    /** 混合检索：Dense + Sparse，RRF 融合 */
    private static SearchResp runHybrid(MilvusClientV2 client,
                                        String queryText,
                                        SearchConfig cfg) throws IOException {
        List<Float> queryVec = getEmbedding(queryText);

        AnnSearchReq denseReq = AnnSearchReq.builder()
                .vectorFieldName("text_dense")
                .vectors(Collections.singletonList(new FloatVec(queryVec)))
                .params("{\"nprobe\": " + cfg.nprobe + "}")
                .topK(cfg.denseRecallTopK)
                .build();

        AnnSearchReq sparseReq = AnnSearchReq.builder()
                .vectorFieldName("text_sparse")
                .vectors(Collections.singletonList(new EmbeddedText(queryText)))
                .params("{\"drop_ratio_search\": " + cfg.dropRatioSearch + "}")
                .topK(cfg.sparseRecallTopK)
                .build();

        HybridSearchReq hybridReq = HybridSearchReq.builder()
                .collectionName(COLLECTION)
                .searchRequests(List.of(denseReq, sparseReq))
                .ranker(new RRFRanker(cfg.rrfK))
                .topK(cfg.finalTopK)
                .consistencyLevel(cfg.consistencyLevel)
                .outFields(cfg.outFields)
                .build();

        return client.hybridSearch(hybridReq);
    }

    private static void printSearchResults(SearchResp resp, SearchMode mode) {
        System.out.println("\n===== Mode: " + mode + " =====");
        List<List<SearchResp.SearchResult>> results = resp.getSearchResults();
        for (List<SearchResp.SearchResult> oneQueryResults : results) {
            for (int i = 0; i < oneQueryResults.size(); i++) {
                SearchResp.SearchResult r = oneQueryResults.get(i);
                System.out.println("Top-" + (i + 1) + " score=" + r.getScore() + ", id=" + r.getId());
                Object text = r.getEntity() == null ? null : r.getEntity().get("text");
                System.out.println("  " + text);
            }
        }
    }

        // ==================== 工具方法 ====================
        
    @SneakyThrows
    private static JsonObject buildRow(String text) {
        JsonObject row = new JsonObject();
        row.addProperty("text", text);
        List<Float> denseVector = getEmbedding(text);
        JsonArray arr = new JsonArray();
        for (Float f : denseVector) arr.add(f);
        row.add("text_dense", arr);
        return row;
    }

    /** 调用 SiliconFlow Embedding API 生成密集向量 */
    private static List<Float> getEmbedding(String text) throws IOException {
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", EMBEDDING_MODEL);
        requestBody.add("input", GSON.toJsonTree(List.of(text)));

        Request request = new Request.Builder()
                .url(EMBEDDING_URL)
                .addHeader("Authorization", "Bearer " + SILICONFLOW_API_KEY)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(
                        GSON.toJson(requestBody),
                        MediaType.parse("application/json")))
                .build();

        try (Response response = HTTP_CLIENT.newCall(request).execute()) {
            String body = Objects.requireNonNull(response.body()).string();
            if (!response.isSuccessful()) {
                throw new IOException("Embedding API 调用失败 http=" + response.code() + ", body=" + body);
            }

            JsonObject json = GSON.fromJson(body, JsonObject.class);
            JsonArray dataArray = json.getAsJsonArray("data");

            JsonArray embeddingArray = dataArray.get(0).getAsJsonObject().getAsJsonArray("embedding");
            if (embeddingArray == null) {
                throw new IOException("Embedding API 返回 embedding 为空，原始响应: " + body);
            }

            List<Float> vector = new ArrayList<>(embeddingArray.size());
            for (int i = 0; i < embeddingArray.size(); i++) {
                vector.add(embeddingArray.get(i).getAsFloat());
            }
            return vector;
        }
    }
}