package com.mahaoyang.maaiagent.controller;

import com.mahaoyang.maaiagent.schedule.RedisScheduleTask;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Resource;

@RestController
@RequestMapping("/api/test")
public class TestController {

    @Resource
    private RedisScheduleTask redisScheduleTask;

    @GetMapping("/sync-redis")
    public String testRedisSyncTask() {
        try {
            redisScheduleTask.synchronizeRedisDataToDatabase();
            return "Redis同步任务已手动触发，请查看日志获取执行结果";
        } catch (Exception e) {
            return "任务执行出错: " + e.getMessage();
        }
    }
}