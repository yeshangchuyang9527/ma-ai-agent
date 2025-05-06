package com.mahaoyang.maaiagent.schedule;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mahaoyang.maaiagent.app.LoveApp;
import com.mahaoyang.maaiagent.domain.entity.ChatMessageRecord; // 显式指定完整包路径
import com.mahaoyang.maaiagent.domain.entity.RedisSyncRecord;
import com.mahaoyang.maaiagent.service.ChatMessageRecordService;
import com.mahaoyang.maaiagent.service.RedisSyncRecordService;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.Date;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class RedisScheduleTask {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(RedisScheduleTask.class);


    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private ChatMessageRecordService chatMessageRecordService;

    @Resource
    private RedisSyncRecordService redisSyncRecordService;

    /**
     * 增量同步Redis数据到数据库
     * 每天凌晨执行一次
     */
    @Scheduled(cron = "0 0 0 * * ?")
    public void incrementalSynchronizeRedisDataToDatabase() {
        try {
            Set<String> keys = stringRedisTemplate.keys("chat:*");
            if (keys == null || keys.isEmpty()) return;

            ObjectMapper objectMapper = new ObjectMapper();
            
            for (String key : keys) {
                String chatId = key.replace("chat:", "");
                
                // 检查是否已同步过该chatId
                RedisSyncRecord syncRecord = redisSyncRecordService.getByChatId(chatId);
                if (syncRecord == null) {
                    // 首次同步
                    processAndSaveNewRecord(key, chatId, objectMapper);
                } else {
                    // 检查是否需要更新
                    checkAndUpdateExistingRecord(key, chatId, syncRecord, objectMapper);
                }
            }
            
            log.info("增量同步Redis数据到数据库完成");
        } catch (Exception e) {
            log.error("增量同步任务执行异常", e);
        }
    }
    
    /**
     * 处理并保存新记录
     */
    private void processAndSaveNewRecord(String key, String chatId, ObjectMapper objectMapper) {
        try {
            String stringValue = stringRedisTemplate.opsForValue().get(key);
            if (stringValue == null) return;
            
            // 找到JSON开始的位置（第一个{字符）
            int jsonStartIndex = stringValue.indexOf("{");
            if (jsonStartIndex == -1) {
                log.warn("无法在字符串中找到JSON开始位置: {}", stringValue);
                return;
            }
            
            // 提取JSON部分
            String jsonStr = stringValue.substring(jsonStartIndex);
            
            // 解析JSON
            LoveApp.LoveReport loveReport = 
                    objectMapper.readValue(jsonStr, LoveApp.LoveReport.class);
            
            // 创建聊天记录
            ChatMessageRecord record = new ChatMessageRecord();
            record.setChatId(chatId);
            record.setTitle(loveReport.title());
            record.setSuggestions(objectMapper.writeValueAsString(loveReport.suggestions()));
            record.setCreateTime(new Date());
            chatMessageRecordService.save(record);
            
            // 记录同步状态
            RedisSyncRecord syncRecord = new RedisSyncRecord();
            syncRecord.setChatId(chatId);
            syncRecord.setLastSyncTime(new Date());
            redisSyncRecordService.save(syncRecord);
            
            log.info("新增聊天记录，chatId: {}", chatId);
        } catch (Exception e) {
            log.error("处理新记录异常, chatId: {}", chatId, e);
        }
    }
    
    /**
     * 检查并更新现有记录
     */
    private void checkAndUpdateExistingRecord(String key, String chatId, RedisSyncRecord syncRecord, ObjectMapper objectMapper) {
        try {
            String stringValue = stringRedisTemplate.opsForValue().get(key);
            if (stringValue == null) return;
            
            // 找到JSON开始的位置
            int jsonStartIndex = stringValue.indexOf("{");
            if (jsonStartIndex == -1) {
                log.warn("无法在字符串中找到JSON开始位置: {}", stringValue);
                return;
            }
            
            // 提取JSON部分
            String jsonStr = stringValue.substring(jsonStartIndex);
            
            // 获取现有记录
            ChatMessageRecord existingRecord = chatMessageRecordService.lambdaQuery()
                    .eq(ChatMessageRecord::getChatId, chatId)
                    .one();
            
            if (existingRecord == null) {
                // 数据库中不存在该记录，直接保存
                processAndSaveNewRecord(key, chatId, objectMapper);
                return;
            }
            
            // 解析JSON
            LoveApp.LoveReport loveReport = 
                    objectMapper.readValue(jsonStr, LoveApp.LoveReport.class);
            
            // 检查数据是否有变化
            String newSuggestions = objectMapper.writeValueAsString(loveReport.suggestions());
            boolean needUpdate = false;
            
            if (!newSuggestions.equals(existingRecord.getSuggestions())) {
                existingRecord.setSuggestions(newSuggestions);
                needUpdate = true;
            }
            
            if (!loveReport.title().equals(existingRecord.getTitle())) {
                existingRecord.setTitle(loveReport.title());
                needUpdate = true;
            }
            
            if (needUpdate) {
                // 更新聊天记录
                chatMessageRecordService.updateById(existingRecord);
                
                // 更新同步时间
                syncRecord.setLastSyncTime(new Date());
                redisSyncRecordService.updateById(syncRecord);
                
                log.info("更新聊天记录，chatId: {}", chatId);
            }
        } catch (Exception e) {
            log.error("检查和更新现有记录异常, chatId: {}", chatId, e);
        }
    }

    /**
     * 全量同步Redis数据到数据库（保留原有方法，现在不建议使用）
     */
    @Scheduled(cron = "0 0 1 * * ?") // 改为凌晨1点执行，避免与增量同步冲突
    public void synchronizeRedisDataToDatabase() {
        try {
            Set<String> keys = stringRedisTemplate.keys("chat:*");
            if (keys == null || keys.isEmpty()) return;

            ObjectMapper objectMapper = new ObjectMapper();
            
            for (String key : keys) {
                String chatId = key.replace("chat:", "");
                String stringValue = stringRedisTemplate.opsForValue().get(key);

                if (stringValue != null) {
                    // 找到JSON开始的位置（第一个{字符）
                    int jsonStartIndex = stringValue.indexOf("{");
                    if (jsonStartIndex != -1) {
                        // 提取JSON部分
                        String jsonStr = stringValue.substring(jsonStartIndex);
                        
                        // 解析JSON
                        LoveApp.LoveReport loveReport = 
                                objectMapper.readValue(jsonStr, LoveApp.LoveReport.class);
                        
                        // 创建记录并保存
                        ChatMessageRecord record = new ChatMessageRecord();
                        record.setChatId(chatId);
                        record.setTitle(loveReport.title());
                        record.setSuggestions(objectMapper.writeValueAsString(loveReport.suggestions()));
                        record.setCreateTime(new Date());
                        chatMessageRecordService.saveOrUpdate(record);
                    } else {
                        log.warn("无法在字符串中找到JSON开始位置: {}", stringValue);
                    }
                }
            }
        } catch (Exception e) {
            log.error("定时任务执行异常", e);
        }
    }

    /**
     * 解析出Json数据
     */
    private static LoveApp.LoveReport parseJson(String json) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(json, LoveApp.LoveReport.class);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
