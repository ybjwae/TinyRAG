package org.example.chunk;

import java.util.ArrayList;
import java.util.List;

public class OverlappingChunker {

    /**
     * 重叠分块——在固定大小的基础上，相邻块之间保留重叠区域
     */
    public static List<String> chunk(String text, int chunkSize, int overlapSize) {
        if (overlapSize > chunkSize) {
            throw new IllegalArgumentException("overlapSize must be less than chunkSize");
        }
        List<String> chunks = new ArrayList<>();
        int step = chunkSize - overlapSize;
        int strat = 0;
        while (strat < text.length()) {
            int end = Math.min(strat + chunkSize, text.length());
            chunks.add(text.substring(strat, end));
            strat += step;
        }
        return chunks;
    }

    public static void main(String[] args) {
        String text = "自签收之日起 7 天内，商品未经使用且不影响二次销售的，"
                + "消费者可申请七天无理由退货。生鲜食品、定制商品、贴身衣物等"
                + "特殊品类不适用此规则，具体以商品详情页标注为准。"
                + "退货运费由消费者承担，如因商品质量问题退货，运费由商家承担。";

        List<String> chunks = chunk(text, 40, 10);

        for (int i = 0; i < chunks.size(); i++) {
            System.out.println("chunk " + i + ": " + chunks.get(i));
        }
    }
}
