package com.mahaoyang.maaiagent.advisor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.advisor.api.*;
import reactor.core.publisher.Flux;

/**
 * 自定义日志记录Advisor
 * 记录AI对话的请求和响应日志
 */
public class MyLoggerAdvisor implements CallAroundAdvisor, StreamAroundAdvisor {

    private static final Logger log = LoggerFactory.getLogger(MyLoggerAdvisor.class);

    /**
     * 处理请求日志
     * @param advisedRequest 请求
     * @return 处理后的请求
     */
    private AdvisedRequest logRequest(AdvisedRequest advisedRequest) {
        // 记录请求信息
        String userId = (String) advisedRequest.userParams().getOrDefault("userId", "anonymous");
        String conversationId = (String) advisedRequest.userParams().getOrDefault("conversationId", "default");
        
        log.info("用户[{}]在会话[{}]中发送请求: {}", userId, conversationId, advisedRequest.userText());
        
        return advisedRequest;
    }
    
    /**
     * 处理响应日志
     * @param response 响应
     * @return 处理后的响应
     */
    private AdvisedResponse logResponse(AdvisedResponse response) {
        // 记录响应信息
        log.info("AI响应: {}", response.toString());

        return response;
    }

    @Override
    public AdvisedResponse aroundCall(AdvisedRequest advisedRequest, CallAroundAdvisorChain chain) {
        // 记录请求
        AdvisedRequest loggedRequest = logRequest(advisedRequest);

        // 获取响应
        long startTime = System.currentTimeMillis();
        AdvisedResponse response = chain.nextAroundCall(loggedRequest);
        long endTime = System.currentTimeMillis();

        // 记录响应及耗时
        log.info("请求处理耗时: {}ms", (endTime - startTime));

        return logResponse(response);
    }

    @Override
    public Flux<AdvisedResponse> aroundStream(AdvisedRequest advisedRequest, StreamAroundAdvisorChain chain) {
        // 记录请求
        AdvisedRequest loggedRequest = logRequest(advisedRequest);
        
        // 开始计时
        long startTime = System.currentTimeMillis();
        
        // 获取响应流
        Flux<AdvisedResponse> responseFlux = chain.nextAroundStream(loggedRequest);
        
        // 在流完成时记录总耗时
        return responseFlux
                .doOnComplete(() -> {
                    long endTime = System.currentTimeMillis();
                    log.info("流式请求处理耗时: {}ms", (endTime - startTime));
                })
                .map(this::logResponse);
    }

    @Override
    public int getOrder() {
        // 日志记录放在最后执行
        return 0;
    }

    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }
}
