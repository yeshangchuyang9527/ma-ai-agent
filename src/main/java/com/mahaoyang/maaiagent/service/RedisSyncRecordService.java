package com.mahaoyang.maaiagent.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mahaoyang.maaiagent.domain.entity.RedisSyncRecord;

/**
 * Redis同步记录Service接口
 */
public interface RedisSyncRecordService extends IService<RedisSyncRecord> {
    /**
     * 根据聊天ID获取同步记录
     *
     * @param chatId 聊天ID
     * @return 同步记录
     */
    RedisSyncRecord getByChatId(String chatId);
} 