package com.mahaoyang.maaiagent.app;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.mahaoyang.maaiagent.advisor.AuthorizationAdvisor;
import com.mahaoyang.maaiagent.advisor.ContentFilterAdvisor;
import com.mahaoyang.maaiagent.advisor.MyLoggerAdvisor;
import com.mahaoyang.maaiagent.chatmemory.FileBaseChatMemory;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.Serializable;
import java.util.List;

import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;
import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY;

/**
 *@Author: 叶上初阳
 *
 */
@Component
public class LoveApp {

    private static final Logger log = LoggerFactory.getLogger(LoveApp.class);

    private final ChatClient chatClient;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource // 注入Jackson JSON处理器
    private ObjectMapper objectMapper;

    private static final String SYSTEM_PROMPT = "扮演深耕恋爱心理领域的专家。开场向用户表明身份，告知用户可倾诉恋爱难题。" +
            "围绕单身、恋爱、已婚三种状态提问：单身状态询问社交圈拓展及追求心仪对象的困扰；" +
            "恋爱状态询问沟通、习惯差异引发的矛盾；已婚状态询问家庭责任与亲属关系处理的问题。" +
            "引导用户详述事情经过、对方反应及自身想法，以便给出专属解决方案。";

    @Resource
    private VectorStore loveAppVectorStore;

    @Resource
    private Advisor loveAppRagCloudAdvisor;

    /**
     * 初始化 ChatClient
     * @param dashscopeChatModel
     */
    public LoveApp(ChatModel dashscopeChatModel) {
        // 初始化基于文件的对话记忆
        String fileDir = System.getProperty("user.dir") + "/tmp/chat-memory";
        ChatMemory chatMemory = new FileBaseChatMemory(fileDir);

//        // 初始化基于内存的对话记忆
//        ChatMemory chatMemory = new InMemoryChatMemory();
        chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(
                        // 权限校验Advisor（最先执行）
                        new AuthorizationAdvisor(),
                        // 内容过滤Advisor
                        new ContentFilterAdvisor(),
                        // 对话记忆Advisor
                        new MessageChatMemoryAdvisor(chatMemory),
                        // 自定义日志 Advisor, 可按需开启
                        new MyLoggerAdvisor()
//                        // 自定义推理增强 Advisor, 可按需开启
//                        new ReReadingAdvisor()
                )
                .build();
    }

    /**
     * AI 基础对话(支持多轮对话记忆)
     * @param message
     * @param chatId
     * @return
     */
    public String doChat(String message, String chatId){
        ChatResponse chatResponse = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        // 现在可以正常使用手动声明的日志
        log.info("content: {}", content);
        return content;
    }

    public record LoveReport(String title, List<String> suggestions) implements Serializable {
    }

    /**
     * AI 恋爱报告功能(实战结构化输出)
     * @param message
     * @param chatId
     * @return
     */
    public LoveReport doChatWithReport(String message, String chatId){
        LoveReport loveReport = chatClient
                .prompt()
                .system(SYSTEM_PROMPT + "每次对话后都要生成恋爱结果，标题为{用户名}的恋爱报告，内容为建议列表")
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                .call()
                .entity(LoveReport.class);
        log.info("loveReport: {}", loveReport);

        try {
            stringRedisTemplate.opsForValue().set("chat:" + chatId,
                    objectMapper.writeValueAsString(loveReport));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON解析错误", e);
        }

        return loveReport;
    }

    // AI 恋爱知识库问答功能

    /**
     * 和 RAG 知识库进行对话
     * @param message
     * @param chatId
     * @return
     */
    public String doChatWithRag(String message, String chatId) {
        ChatResponse chatResponse = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                // 开启日志, 便于观察效果
                .advisors(new MyLoggerAdvisor())
                // 应用 RAG 知识库问答
//                .advisors(new QuestionAnswerAdvisor(loveAppVectorStore))
                // 应用 RAG 检索增强服务(基于云知识库)
                .advisors(loveAppRagCloudAdvisor)
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }


}