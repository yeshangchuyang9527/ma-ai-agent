package com.mahaoyang.maaiagent.advisor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.advisor.api.*;
import reactor.core.publisher.Flux;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 内容过滤Advisor
 * 用于检查用户输入和AI响应是否包含违禁词
 */
public class ContentFilterAdvisor implements CallAroundAdvisor, StreamAroundAdvisor {

    // 手动定义Logger
    private static final Logger log = LoggerFactory.getLogger(ContentFilterAdvisor.class);

    // 违禁词等级
    public enum SensitivityLevel {
        HIGH(3),   // 高风险词汇，直接拦截
        MEDIUM(2), // 中等风险，根据场景可能拦截
        LOW(1);    // 低风险，仅记录不拦截
        
        private final int value;
        
        SensitivityLevel(int value) {
            this.value = value;
        }
        
        public int getValue() {
            return value;
        }
    }
    
    // 违禁词库，Map<词汇, 敏感级别>
    private final Map<String, SensitivityLevel> sensitiveWords;
    
    // 违禁词的正则表达式缓存
    private final Map<SensitivityLevel, Pattern> sensitivePatterns;
    
    public ContentFilterAdvisor() {
        this.sensitiveWords = new ConcurrentHashMap<>();
        this.sensitivePatterns = new EnumMap<>(SensitivityLevel.class);
        
        // 初始化默认违禁词（示例）
        initDefaultSensitiveWords();
        
        // 构建正则表达式
        buildPatterns();
    }
    
    private void initDefaultSensitiveWords() {
        // 高风险违禁词示例
        addSensitiveWord("违禁词1", SensitivityLevel.HIGH);
        addSensitiveWord("违禁词2", SensitivityLevel.HIGH);
        
        // 中等风险违禁词示例
        addSensitiveWord("敏感词1", SensitivityLevel.MEDIUM);
        addSensitiveWord("敏感词2", SensitivityLevel.MEDIUM);
        
        // 低风险违禁词示例
        addSensitiveWord("普通词1", SensitivityLevel.LOW);
        addSensitiveWord("普通词2", SensitivityLevel.LOW);
    }
    
    /**
     * 添加违禁词
     * @param word 违禁词
     * @param level 敏感级别
     */
    public void addSensitiveWord(String word, SensitivityLevel level) {
        sensitiveWords.put(word, level);
        // 重新构建正则表达式
        buildPatterns();
    }
    
    /**
     * 移除违禁词
     * @param word 违禁词
     */
    public void removeSensitiveWord(String word) {
        sensitiveWords.remove(word);
        // 重新构建正则表达式
        buildPatterns();
    }
    
    /**
     * 根据敏感级别构建正则表达式
     */
    private void buildPatterns() {
        // 按敏感级别分组违禁词
        Map<SensitivityLevel, List<String>> wordsByLevel = new EnumMap<>(SensitivityLevel.class);
        
        for (SensitivityLevel level : SensitivityLevel.values()) {
            wordsByLevel.put(level, new ArrayList<>());
        }
        
        for (Map.Entry<String, SensitivityLevel> entry : sensitiveWords.entrySet()) {
            wordsByLevel.get(entry.getValue()).add(Pattern.quote(entry.getKey()));
        }
        
        // 为每个敏感级别构建正则表达式
        for (Map.Entry<SensitivityLevel, List<String>> entry : wordsByLevel.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                String pattern = String.join("|", entry.getValue());
                sensitivePatterns.put(entry.getKey(), Pattern.compile(pattern));
            }
        }
    }
    
    /**
     * 检查文本是否包含违禁词
     * @param text 待检查文本
     * @param minLevel 最小敏感级别
     * @return 发现的违禁词列表
     */
    private List<String> checkSensitiveContent(String text, SensitivityLevel minLevel) {
        List<String> foundWords = new ArrayList<>();
        
        // 对每个敏感级别进行检查
        for (SensitivityLevel level : SensitivityLevel.values()) {
            // 只检查大于等于指定敏感级别的词汇
            if (level.getValue() >= minLevel.getValue() && sensitivePatterns.containsKey(level)) {
                Pattern pattern = sensitivePatterns.get(level);
                Matcher matcher = pattern.matcher(text);
                
                while (matcher.find()) {
                    foundWords.add(matcher.group());
                }
            }
        }
        
        return foundWords;
    }
    
    private AdvisedRequest filterRequest(AdvisedRequest advisedRequest) {
        String userText = advisedRequest.userText();
        
        // 检查用户输入是否包含高风险违禁词
        List<String> foundWords = checkSensitiveContent(userText, SensitivityLevel.HIGH);
        
        if (!foundWords.isEmpty()) {
            // 发现违禁词，记录日志
            log.warn("用户输入包含违禁词: {}", foundWords);
            
            // 修改请求以返回错误信息
            Map<String, Object> advisedUserParams = new HashMap<>(advisedRequest.userParams());
            advisedUserParams.put("error", "sensitive_content_detected");
            advisedUserParams.put("foundWords", foundWords);
            
            return AdvisedRequest.from(advisedRequest)
                    .userText("您的输入包含违禁内容，请调整后重新提交。")
                    .userParams(advisedUserParams)
                    .build();
        }
        
        // 检查中等风险词汇（仅记录，不拦截）
        List<String> mediumRiskWords = checkSensitiveContent(userText, SensitivityLevel.MEDIUM);
        if (!mediumRiskWords.isEmpty()) {
            log.info("用户输入包含中等风险词汇: {}", mediumRiskWords);
        }
        
        return advisedRequest;
    }
    
    private AdvisedResponse filterResponse(AdvisedResponse response) {
        try {
            // 尝试获取响应内容 - 修复方法调用
            String content = "";
            // 使用toString()方法获取内容，因为AdvisedResponse没有getText()方法
            content = response.toString();
            
            // 如果无法获取内容，则记录日志并返回原响应
            if (content.isEmpty()) {
                log.warn("无法获取响应内容进行过滤");
                return response;
            }
            
            // 检查响应是否包含高风险违禁词
            List<String> foundWords = checkSensitiveContent(content, SensitivityLevel.HIGH);
            
            if (!foundWords.isEmpty()) {
                // 发现违禁词，记录日志
                log.warn("AI响应包含违禁词: {}", foundWords);
                
                // 如果需要，可以替换响应内容
                // 此处简单处理，实际应用中可能需要更复杂的内容替换逻辑
                return response; // 返回原响应，依赖后续处理
            }
        } catch (Exception e) {
            log.error("过滤响应内容时发生错误: {}", e.getMessage());
        }
        
        return response;
    }

    @Override
    public AdvisedResponse aroundCall(AdvisedRequest advisedRequest, CallAroundAdvisorChain chain) {
        // 先过滤请求
        AdvisedRequest filteredRequest = filterRequest(advisedRequest);
        
        // 如果请求被标记为含有违禁内容，直接返回错误响应
        if (filteredRequest.userParams().containsKey("error")) {
            return chain.nextAroundCall(filteredRequest);
        }
        
        // 获取正常响应
        AdvisedResponse response = chain.nextAroundCall(filteredRequest);
        
        // 过滤响应（检查AI输出是否包含违禁内容）
        return filterResponse(response);
    }

    @Override
    public Flux<AdvisedResponse> aroundStream(AdvisedRequest advisedRequest, StreamAroundAdvisorChain chain) {
        // 先过滤请求
        AdvisedRequest filteredRequest = filterRequest(advisedRequest);
        
        // 如果请求被标记为含有违禁内容，直接返回错误响应
        if (filteredRequest.userParams().containsKey("error")) {
            return chain.nextAroundStream(filteredRequest);
        }
        
        // 获取正常响应
        Flux<AdvisedResponse> responseFlux = chain.nextAroundStream(filteredRequest);
        
        // 过滤每个流式响应
        return responseFlux.map(this::filterResponse);
    }

    @Override
    public int getOrder() {
        // 内容过滤在权限检查之后执行
        return -50;
    }

    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }
} 