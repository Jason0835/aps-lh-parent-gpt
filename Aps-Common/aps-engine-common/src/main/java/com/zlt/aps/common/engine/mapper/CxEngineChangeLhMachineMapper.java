package com.zlt.aps.common.engine.mapper;

import com.zlt.aps.cx.api.domain.entity.CxChangeLhMachine;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface CxEngineChangeLhMachineMapper {

    /**
     * 加载全部的成型排程硫化机关系列表
     * @param cxChangeLhMachine
     * @return
     */
    List<CxChangeLhMachine> listChangeLhMachineList(CxChangeLhMachine cxChangeLhMachine);

    /**
     * 根据排程日期和其他条件进行按照工单合并机台信息
     * @param cxChangeLhMachine
     * @return
     */
    List<CxChangeLhMachine> splitCxOrderMachineList(CxChangeLhMachine cxChangeLhMachine);

    /**
     * 删除日期和类型对应的硫化机关系
     * @param scheduleDateStr 删除排程日期
     * @param dataSource 数据来源
     * @return
     */
    int deleteChangeLhMachineByScheduleDate(@Param("scheduleDateStr") String scheduleDateStr, @Param("dataSource") String dataSource, @Param("cxOrderNo") String cxOrderNo);

    /**
     * 批量创建排程硫化机关系
     * @param cxChangeLhMachineList
     * @return
     */
    int batchInsertCxChangeLhMachine(@Param("cxChangeLhMachineList") List<CxChangeLhMachine> cxChangeLhMachineList);
}
