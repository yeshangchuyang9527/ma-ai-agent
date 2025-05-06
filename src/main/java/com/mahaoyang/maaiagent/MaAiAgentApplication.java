// 修改启动类启用定时任务
package com.mahaoyang.maaiagent;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling; // 新增导入

@EnableScheduling  // 启用定时任务
@SpringBootApplication
@MapperScan("com.mahaoyang.maaiagent.mapper")
public class MaAiAgentApplication {
    public static void main(String[] args) {
        SpringApplication.run(MaAiAgentApplication.class, args);
    }
}