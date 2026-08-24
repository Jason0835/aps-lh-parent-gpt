package com.zlt.aps.tq.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.tq.api.domain.entity.TqMachineChuck;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface TqMachineChuckMapper extends BaseMapper<TqMachineChuck> {

    /**
     * 查询胎圈机台寸口对应列表（关联机台信息表反显机台名称）
     *
     * @param entity 查询条件（机台编码）
     * @return 机台寸口对应列表
     */
    List<TqMachineChuck> listMachineChuck(TqMachineChuck entity);

    /**
     * 按机台编码集合查询寸口记录（含已逻辑删除记录）
     * <p>表存在 (MACHINE_CODE, CHUCK_CODE) 唯一索引，逻辑删除记录仍占用唯一键，
     * 唯一性判断与"复活已删除记录"处理均需读取已删除数据，
     * 自定义SQL绕过框架逻辑删除自动过滤</p>
     *
     * @param machineCodes 机台编码集合
     * @return 机台寸口记录（含已删除）
     */
    List<TqMachineChuck> listIncludeDeleted(@Param("machineCodes") List<String> machineCodes);

    /**
     * 复活已逻辑删除的机台寸口记录并覆盖业务字段
     * <p>新增时若同维度存在已删除记录，直接插入会撞唯一索引，
     * 改为复活该记录：IS_DELETE 置回 0 并全覆盖寸口名称/英寸尺寸/备注</p>
     *
     * @param machineChuck 待复活记录（需携带库中记录ID及覆盖字段）
     * @return 影响行数
     */
    int reviveMachineChuck(TqMachineChuck machineChuck);

    /**
     * 删除全部机台寸口对应（逻辑删除）
     */
    void deleteAllMachineChuck();
}
