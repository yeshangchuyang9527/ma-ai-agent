package com.mahaoyang.maaiagent.demo.invoke;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONUtil;

/**
 * HTTP 方式调用
 */
public class HutoolRequestAiInvoke {
    public static void main(String[] args) {
        // API URL
        String url = "https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation";

        // API Key
        String apiKey = "sk-ac5c9886ca994b38ba4fed8948a62dda";

        // 请求体数据
        String body = JSONUtil.createObj()
                .put("model", "qwen-plus")
                .put("input", JSONUtil.createObj()
                        .put("messages", JSONUtil.createArray()
                                .put(JSONUtil.createObj()
                                        .put("role", "system")
                                        .put("content", "You are a helpful assistant."))
                                .put(JSONUtil.createObj()
                                        .put("role", "user")
                                        .put("content", "你是谁？"))))
                .put("parameters", JSONUtil.createObj()
                        .put("result_format", "message"))
                .toString();

        // 发送 POST 请求
        HttpResponse response = HttpRequest.post(url)
                .header("Authorization", "Bearer " + apiKey) // 设置 Authorization 头
                .header("Content-Type", "application/json") // 设置 Content-Type
                .body(body) // 设置请求体
                .execute(); // 执行请求

        // 输出响应结果
        System.out.println("Status Code: " + response.getStatus());
        System.out.println("Response Body: " + response.body());
    }
}