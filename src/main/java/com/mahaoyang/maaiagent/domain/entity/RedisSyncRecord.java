package com.mahaoyang.maaiagent.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.util.Date;

/**
 * Redis同步记录表
 * @TableName redis_sync_record
 */
@TableName(value ="redis_sync_record")
@Data
public class RedisSyncRecord implements Serializable {
    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 会话id
     */
    @TableField("chat_id")
    private String chatId;

    /**
     * 上次同步时间
     */
    @TableField("last_sync_time")
    private Date lastSyncTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getChatId() {
        return chatId;
    }

    public void setChatId(String chatId) {
        this.chatId = chatId;
    }

    public Date getLastSyncTime() {
        return lastSyncTime;
    }

    public void setLastSyncTime(Date lastSyncTime) {
        this.lastSyncTime = lastSyncTime;
    }

    @Override
    public String toString() {
        return "RedisSyncRecord{" +
                "id=" + id +
                ", chatId='" + chatId + '\'' +
                ", lastSyncTime=" + lastSyncTime +
                '}';
    }
}