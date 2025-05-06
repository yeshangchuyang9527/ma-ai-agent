package com.mahaoyang.maaiagent.advisor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.advisor.api.*;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 权限校验Advisor
 * 用于验证用户是否有权限执行特定的AI操作
 */
public class AuthorizationAdvisor implements CallAroundAdvisor, StreamAroundAdvisor {

    // 手动定义Logger
    private static final Logger log = LoggerFactory.getLogger(AuthorizationAdvisor.class);

    // 保存用户角色及其权限映射
    private final Map<String, Set<String>> rolePermissions;
    
    // 用户ID到角色的映射
    private final Map<String, String> userRoles;
    
    // 用户配额跟踪（每日限额）
    private final Map<String, Integer> userQuota;

    public AuthorizationAdvisor() {
        // 初始化权限结构
        this.rolePermissions = new ConcurrentHashMap<>();
        this.userRoles = new ConcurrentHashMap<>();
        this.userQuota = new ConcurrentHashMap<>();
        
        // 初始化一些默认权限（实际项目中应从配置或数据库加载）
        initDefaultPermissions();
    }
    
    private void initDefaultPermissions() {
        // 示例：设置不同角色的权限
        Set<String> adminPermissions = Set.of("all_models", "unlimited_tokens", "sensitive_content");
        Set<String> premiumPermissions = Set.of("premium_models", "high_tokens", "tool_access");
        Set<String> basicPermissions = Set.of("basic_models", "low_tokens");
        
        rolePermissions.put("admin", adminPermissions);
        rolePermissions.put("premium", premiumPermissions);
        rolePermissions.put("basic", basicPermissions);
        
        // 示例：设置一些用户角色
        userRoles.put("user1", "admin");
        userRoles.put("user2", "premium");
        userRoles.put("user3", "basic");
    }
    
    /**
     * 检查用户是否有权限
     * @param userId 用户ID
     * @param permission 所需权限
     * @return 是否有权限
     */
    private boolean hasPermission(String userId, String permission) {
        String role = userRoles.getOrDefault(userId, "basic");
        Set<String> permissions = rolePermissions.getOrDefault(role, Set.of());
        
        // 特殊处理：admin角色有all_models权限时视为拥有所有权限
        if (role.equals("admin") && permissions.contains("all_models")) {
            return true;
        }
        
        return permissions.contains(permission);
    }
    
    /**
     * 检查用户是否超出配额
     * @param userId 用户ID
     * @return 是否超出配额
     */
    private boolean isOverQuota(String userId) {
        int quota = userQuota.getOrDefault(userId, 0);
        String role = userRoles.getOrDefault(userId, "basic");
        
        // 根据角色设置不同的配额限制
        int limit = switch (role) {
            case "admin" -> Integer.MAX_VALUE; // 管理员无限制
            case "premium" -> 1000;            // 高级用户每日1000次
            default -> 100;                    // 基础用户每日100次
        };
        
        return quota >= limit;
    }
    
    /**
     * 更新用户使用配额
     * @param userId 用户ID
     */
    private void updateQuota(String userId) {
        userQuota.compute(userId, (key, value) -> (value == null) ? 1 : value + 1);
    }

    private AdvisedRequest processRequest(AdvisedRequest advisedRequest) {
        // 从请求中提取用户ID
        Map<String, Object> params = advisedRequest.userParams();
        String userId = (String) params.getOrDefault("userId", "anonymous");
        String modelName = (String) params.getOrDefault("model", "basic_model");
        
        // 进行权限检查
        boolean hasModelPermission = hasPermission(userId, determineModelPermission(modelName));
        boolean isWithinQuota = !isOverQuota(userId);
        
        if (!hasModelPermission) {
            // 如果没有权限，修改请求以返回权限错误信息
            Map<String, Object> advisedUserParams = new HashMap<>(advisedRequest.userParams());
            advisedUserParams.put("error", "permission_denied");
            
            return AdvisedRequest.from(advisedRequest)
                    .userText("对不起，您没有权限使用该模型。")
                    .userParams(advisedUserParams)
                    .build();
        }
        
        if (!isWithinQuota) {
            // 如果超出配额，修改请求以返回配额错误信息
            Map<String, Object> advisedUserParams = new HashMap<>(advisedRequest.userParams());
            advisedUserParams.put("error", "quota_exceeded");
            
            return AdvisedRequest.from(advisedRequest)
                    .userText("对不起，您已超出今日使用配额。")
                    .userParams(advisedUserParams)
                    .build();
        }
        
        // 通过检查，更新配额
        updateQuota(userId);
        
        // 记录请求信息
        log.info("用户 [{}] 权限检查通过，使用模型: {}", userId, modelName);
        
        return advisedRequest;
    }
    
    /**
     * 根据模型名称确定所需权限
     */
    private String determineModelPermission(String modelName) {
        return switch (modelName) {
            case "qwen-max", "qwen-plus", "qwen-turbo" -> "premium_models";
            default -> "basic_models";
        };
    }

    @Override
    public AdvisedResponse aroundCall(AdvisedRequest advisedRequest, CallAroundAdvisorChain chain) {
        AdvisedRequest processedRequest = processRequest(advisedRequest);
        
        // 检查是否有错误
        if (processedRequest.userParams().containsKey("error")) {
            // 返回错误信息，直接进入下一个顾问处理链
            return chain.nextAroundCall(processedRequest);
        }
        
        // 继续调用链
        return chain.nextAroundCall(processedRequest);
    }

    @Override
    public Flux<AdvisedResponse> aroundStream(AdvisedRequest advisedRequest, StreamAroundAdvisorChain chain) {
        AdvisedRequest processedRequest = processRequest(advisedRequest);
        
        // 检查是否有错误
        if (processedRequest.userParams().containsKey("error")) {
            // 返回错误信息，直接进入下一个顾问处理链
            return chain.nextAroundStream(processedRequest);
        }
        
        // 继续调用链
        return chain.nextAroundStream(processedRequest);
    }

    @Override
    public int getOrder() {
        // 权限检查应该最先执行
        return -100;
    }

    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }
} 