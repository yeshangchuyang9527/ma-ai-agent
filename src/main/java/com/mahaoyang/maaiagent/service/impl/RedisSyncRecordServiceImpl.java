package com.mahaoyang.maaiagent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mahaoyang.maaiagent.domain.entity.RedisSyncRecord;
import com.mahaoyang.maaiagent.mapper.RedisSyncRecordMapper;
import com.mahaoyang.maaiagent.service.RedisSyncRecordService;
import org.springframework.stereotype.Service;

/**
 * Redis同步记录Service实现类
 */
@Service
public class RedisSyncRecordServiceImpl extends ServiceImpl<RedisSyncRecordMapper, RedisSyncRecord> implements RedisSyncRecordService {
    
    @Override
    public RedisSyncRecord getByChatId(String chatId) {
        LambdaQueryWrapper<RedisSyncRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(RedisSyncRecord::getChatId, chatId);
        return getOne(queryWrapper);
    }
} 