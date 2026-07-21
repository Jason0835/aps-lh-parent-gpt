package com.zlt.aps.gsq.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.gsq.api.domain.entity.GsqStock;

import java.util.List;

/**
 * 钢丝圈库存管理Mapper接口
 *
 * @author zlt
 * @date 2026-07-08
 */
public interface GsqStockMapper extends BaseMapper<GsqStock> {

    /**
     * 校验"库存日期+钢丝圈代码"组合是否已存在
     *
     * @param entity 实体
     * @return 已存在数量（0表示唯一，>0表示不唯一）
     */
    int checkUnique(GsqStock entity);

    /**
     * 批量合并保存（存在则更新，否则新增），用于导入场景
     *
     * @param list 待保存数据集合
     */
    void mergeSql(List<GsqStock> list);
}
