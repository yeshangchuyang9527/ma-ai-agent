package com.mahaoyang.maaiagent.demo.invoke;

import dev.langchain4j.community.model.dashscope.QwenChatModel;
import dev.langchain4j.model.chat.ChatLanguageModel;

/**
 *@Author: 叶上初阳
 *
 */
public class LangChainAiInvoke {

    public static void main(String[] args) {
        ChatLanguageModel qwenChatModel = QwenChatModel.builder()
                .apiKey(TestApiKey.API_KEY)
                .modelName("qwen-max")
                .build();
        String anwser = qwenChatModel.chat("你好,我叫马浩阳,正在学AI应用开发");
        System.out.println(anwser);
    }
}
