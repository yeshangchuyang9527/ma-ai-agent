package com.mahaoyang.maaiagent.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 对话记录表
 * @TableName chat_message_record
 */
@TableName(value ="chat_message_record", autoResultMap = true)
@Data
public class ChatMessageRecord implements Serializable {
    /**
     * 主键
     */
    private Long id;

    /**
     * 会话id
     */
    @TableField("chat_id")
    private String chatId;

    /**
     * 标题
     */
    private String title;

    /**
     * 对话内容 - 存储JSON字符串
     */
    private String suggestions;
    
    /**
     * 创建时间
     */
    @TableField("create_time")
    private Date createTime;

    /**
     * 更新时间
     */
    @TableField("update_time")
    private Date updateTime;

    /**
     * 建议列表 - 非数据库字段，用于业务逻辑
     */
    @TableField(exist = false)
    private List<String> suggestionList;

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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSuggestions() {
        return suggestions;
    }

    public void setSuggestions(String suggestions) {
        this.suggestions = suggestions;
    }

    public List<String> getSuggestionList() {
        if (this.suggestions == null) {
            return null;
        }
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(this.suggestions, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return null;
        }
    }

    public void setSuggestionList(List<String> suggestionList) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            this.suggestions = objectMapper.writeValueAsString(suggestionList);
        } catch (Exception e) {
            this.suggestions = null;
        }
        this.suggestionList = suggestionList;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }
}