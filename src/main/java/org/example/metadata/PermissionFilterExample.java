package org.example.metadata;

import org.springframework.ai.document.Document;

import java.util.*;
import java.util.stream.Collectors;

public class PermissionFilterExample {

    /**
     * 模拟向量数据库中的 chunks
     */
    private static List<Document> mockChunks() {
        List<Document> chunks = new ArrayList<>();

        // chunk 1: 公开信息
        Map<String, Object> meta1 = new HashMap<>();
        meta1.put("sensitivity_level", "public");
        meta1.put("access_roles", Arrays.asList("employee"));
        chunks.add(new Document("员工手册规定，所有员工享有带薪年假。", meta1));

        // chunk 2: 人事部内部信息
        Map<String, Object> meta2 = new HashMap<>();
        meta2.put("sensitivity_level", "confidential");
        meta2.put("access_departments", Arrays.asList("hr", "executive"));
        chunks.add(new Document("年终奖为月薪的 2-6 倍，根据绩效等级确定。", meta2));

        // chunk 3: 技术部可见信息
        Map<String, Object> meta3 = new HashMap<>();
        meta3.put("sensitivity_level", "internal");
        meta3.put("access_departments", Arrays.asList("tech", "product"));
        chunks.add(new Document("技术部员工可申请远程办公，每周最多 2 天。", meta3));

        return chunks;
    }

    /**
     *  根据用户权限过滤 chunks
     */
    public static List<Document> filterByPermission(
            List<Document> chunks,
            String userRole,
            String userDepartment){

        return chunks.stream()
                .filter(chunk -> hasPermission(chunk, userRole, userDepartment))
                .collect(Collectors.toList());
    }

    /**
     * 判断用户是否有权限访问某个 chunk
     */
    private static boolean hasPermission(
            Document chunk,
            String userRole,
            String userDepartment) {

        Map<String, Object> metadata = chunk.getMetadata();

        // 公开信息，所有人都能看
        String sensitivityLevel = (String) metadata.get("sensitivity_level");
        if ("public".equals(sensitivityLevel)) {
            return true;
        }

        // 检查角色权限
        List<String> accessRoles = (List<String>) metadata.get("access_roles");
        if (accessRoles != null && accessRoles.contains(userRole)) {
            return true;
        }

        // 检查部门权限
        List<String> accessDepartments = (List<String>) metadata.get("access_departments");
        if (accessDepartments != null && accessDepartments.contains(userDepartment)) {
            return true;
        }

        return false;
    }

    public static void main(String[] args) {
        List<Document> chunks = mockChunks();

        // 场景 1: 技术部普通员工
        System.out.println("=== 技术部员工小李能看到的内容 ===");
        List<Document> techResults = filterByPermission(chunks, "employee", "tech");
        techResults.forEach(chunk -> System.out.println(chunk.getText()));

        // 场景 2: 人事部经理
        System.out.println("\n=== 人事部经理能看到的内容 ===");
        List<Document> hrResults = filterByPermission(chunks, "manager", "hr");
        hrResults.forEach(chunk -> System.out.println(chunk.getText()));
    }

}
