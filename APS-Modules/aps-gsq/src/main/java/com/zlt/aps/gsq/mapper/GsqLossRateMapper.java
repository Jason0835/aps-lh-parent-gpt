package com.zlt.aps.gsq.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.gsq.api.domain.entity.GsqLossRate;

import java.util.List;

/**
 * 钢丝圈损耗率管理Mapper接口
 *
 * @author zlt
 * @date 2026-07-08
 */
public interface GsqLossRateMapper extends BaseMapper<GsqLossRate> {

    /**
     * 查询钢丝圈损耗率列表（左联机台信息表反显机台名称）
     *
     * @param entity 查询条件
     * @return 列表
     */
    List<GsqLossRate> listLossRate(GsqLossRate entity);

    /**
     * 校验"钢丝圈编码+机台编码"组合是否已存在
     *
     * @param entity 实体
     * @return 已存在数量（0表示唯一，>0表示不唯一）
     */
    int checkUnique(GsqLossRate entity);
}
