package com.mahaoyang.maaiagent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mahaoyang.maaiagent.domain.entity.RedisSyncRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * Redis同步记录Mapper接口
 */
@Mapper
public interface RedisSyncRecordMapper extends BaseMapper<RedisSyncRecord> {
} 