package org.example.embedding;

public class CosineSimilarity {

    /**
     * 计算两个向量的余弦相似度
     *
     * @param vectorA 向量A
     * @param vectorB 向量B
     * @return 余弦相似度, 值范围[-1.0, 1.0]
     */
    public static double calculate(double[] vectorA, double[] vectorB) {
        if (vectorA.length != vectorB.length) {
            throw new IllegalArgumentException(
                    "两个向量的维度必须相同，vectorA: " + vectorA.length
                            + ", vectorB: " + vectorB.length);
        }

        double dotProduct = 0.0;    // 点积
        double normA = 0.0;         // 向量A的模
        double normB = 0.0;         // 向量B的模

        for (int i = 0; i < vectorA.length; i++) {
            dotProduct += vectorA[i] * vectorB[i];
            normA += vectorA[i] * vectorA[i];
            normB += vectorB[i] * vectorB[i];
        }

        normA = Math.sqrt(normA);
        normB = Math.sqrt(normB);

        // 避免除零
        if (normA == 0.0 || normB == 0.0) {
            return 0.0;
        }

        return dotProduct / (normA * normB);
    }

    public static void main(String[] args) {
        // 模拟三个文本的向量（实际维度会高得多，这里用 5 维演示）
        double[] returnPolicy = {0.8, 0.1, 0.9, 0.2, 0.7};   // "七天无理由退货"
        double[] returnQuery = {0.75, 0.15, 0.85, 0.25, 0.65}; // "买了一周还能退吗"
        double[] logistics = {0.1, 0.9, 0.2, 0.8, 0.1};        // "物流配送时效说明"

        double sim1 = calculate(returnPolicy, returnQuery);
        double sim2 = calculate(returnPolicy, logistics);

        System.out.println("「七天无理由退货」vs「买了一周还能退吗」：" + String.format("%.4f", sim1));
        System.out.println("「七天无理由退货」vs「物流配送时效说明」：" + String.format("%.4f", sim2));
    }
}
