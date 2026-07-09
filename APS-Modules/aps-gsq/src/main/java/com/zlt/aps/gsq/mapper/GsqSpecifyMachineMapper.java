package com.zlt.aps.gsq.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.gsq.api.domain.entity.GsqSpecifyMachine;

import java.util.List;

/**
 * 钢丝圈定点机台Mapper接口
 *
 * @author zlt
 * @date 2026-07-08
 */
public interface GsqSpecifyMachineMapper extends BaseMapper<GsqSpecifyMachine> {

    /**
     * 查询钢丝圈定点机台列表（左联机台信息表反显生产线名称）
     *
     * @param entity 查询条件
     * @return 列表
     */
    List<GsqSpecifyMachine> listSpecifyMachine(GsqSpecifyMachine entity);

    /**
     * 校验"钢丝圈代码+生产线"组合是否已存在
     *
     * @param entity 实体
     * @return 已存在数量（0表示唯一，>0表示不唯一）
     */
    int checkUnique(GsqSpecifyMachine entity);

    /**
     * 批量合并保存（存在则更新，否则新增），用于导入场景
     *
     * @param list 待保存数据集合
     */
    void mergeSql(List<GsqSpecifyMachine> list);
}
