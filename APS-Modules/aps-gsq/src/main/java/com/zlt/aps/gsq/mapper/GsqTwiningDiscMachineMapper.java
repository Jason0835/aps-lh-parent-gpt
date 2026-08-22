package com.zlt.aps.gsq.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.gsq.api.domain.entity.GsqTwiningDiscMachine;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 钢丝圈缠绕盘-机台关系 Mapper 接口
 *
 * @author zlt
 * @date 2026-08-20
 */
public interface GsqTwiningDiscMachineMapper extends BaseMapper<GsqTwiningDiscMachine> {

    /**
     * 查询缠绕盘-机台关系列表（关联缠绕盘主表与机台信息表，含名称/英寸/排列方式反显）
     *
     * @param entity 查询条件（缠绕盘编码/机台编号/状态/工厂/数据来源）
     * @return 列表
     */
    List<GsqTwiningDiscMachine> listDiscMachine(GsqTwiningDiscMachine entity);

    /**
     * 校验缠绕盘+机台组合是否已存在
     *
     * @param entity 实体（缠绕盘编码+机台编号）
     * @return 已存在数量（0表示唯一，>0表示不唯一）
     */
    int checkUnique(@Param("entity") GsqTwiningDiscMachine entity);

    /**
     * MES缠绕盘机台关系同步专用批量插入（XML显式列，绕过MetaObjectHandler，CREATE_BY='MES'）
     *
     * @param list 待插入的机台关系列表（均为APS中不存在的新增组合）
     */
    void batchInsertMesMachine(List<GsqTwiningDiscMachine> list);
}
