package com.zlt.aps.gsq.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.gsq.api.domain.entity.GsqTwiningDiscSub;

import java.util.List;

/**
 * 钢丝圈缠绕盘明细 Mapper 接口
 *
 * @author zlt
 * @date 2026-07-08
 */
public interface GsqTwiningDiscSubMapper extends BaseMapper<GsqTwiningDiscSub> {

    /**
     * 按主表ID查询子表明细（仅查未逻辑删除数据）
     *
     * @param discId 主表ID
     * @return 子表明细列表
     */
    List<GsqTwiningDiscSub> listSubByDiscId(Long discId);
}
