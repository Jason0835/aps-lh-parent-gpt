package com.zlt.aps.gsq.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.gsq.api.domain.entity.GsqTwiningDiscSpec;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 钢丝圈缠绕盘-规格关系 Mapper 接口
 *
 * @author zlt
 * @date 2026-08-21
 */
public interface GsqTwiningDiscSpecMapper extends BaseMapper<GsqTwiningDiscSpec> {

    /**
     * 查询缠绕盘-规格关系列表（关联缠绕盘主表反显名称/英寸/排列方式）
     *
     * @param entity 查询条件（缠绕盘编码/钢丝圈编号/状态/工厂/数据来源/orderStr排序）
     * @return 规格关系列表（含反显字段）
     */
    List<GsqTwiningDiscSpec> listDiscSpec(GsqTwiningDiscSpec entity);

    /**
     * 校验缠绕盘+钢丝圈规格组合是否已存在（0=唯一，>0=不唯一）
     *
     * @param entity 实体（缠绕盘编码+钢丝圈编号，更新时含id排除自身）
     * @return 已存在记录数
     */
    int checkUnique(@Param("entity") GsqTwiningDiscSpec entity);

    /**
     * MES缠绕盘规格关系同步专用批量插入（XML显式列，绕过MetaObjectHandler，CREATE_BY='MES'）
     * 仅用于MES新增组合（已存在的走LambdaUpdateWrapper按ID更新）
     *
     * @param list 待插入的规格关系列表（已反显钢丝圈名称）
     */
    void batchInsertMesSpec(List<GsqTwiningDiscSpec> list);
}
