package com.zlt.aps.common.engine.service;


import com.zlt.aps.cx.api.domain.entity.CxChangeLhMachine;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
  *  成型排程绑定硫化机关系信息逻辑接口
  * @ClassName CxEngineChangeLhMachineService
  * @Description 用于统一处理排程工单和硫化机关系统一查询接口
  * @Author Joran.Zhang
  * @Date 2022/4/14 10:51
  * @Version 1.0
**/
public interface CxEngineChangeLhMachineService {

    /**
     * 根据条件查询排程对应的硫化机关系
     * @param cxChangeLhMachine
     * @return
     */
    List<CxChangeLhMachine> listChangeLhMachineList(CxChangeLhMachine cxChangeLhMachine);

    /**
     * 根据排程日期查询日期工单对应的硫化机台信息
     * @param scheduleDate 排程日期
     * @param dataSource 数据来源：0：成型排程；1：增补计划
     * @param cxOrderNo 成型工单号
     * @return
     */
    Map<String,String> splitCxOrderWithLhMachines(Date scheduleDate, String dataSource,String cxOrderNo);

    /**
     * 删除排程日期查询日期工单对应的硫化机台信息
     * @param scheduleDate 排程日期
     * @param dataSource 数据来源：0：成型排程；1：增补计划
     * @param cxOrderNo 成型工单号
     * @return
     */
    int deleteChangeLhMachineByScheduleDate(Date scheduleDate, String dataSource, String cxOrderNo);

    /**
     * 批量新增排程和硫化机关系数据
     * @param cxChangeLhMachineList
     * @return
     */
    int batchInsertCxChangeLhMachine(List<CxChangeLhMachine> cxChangeLhMachineList);

    /**
     * 根据排程日期，创建日期对应的增补计划排程硫化机台关系
     * @param scheduleDate 增补计划日期
     * @return
     */
    void buildSuppleCxChangeLhMachine(Date scheduleDate, String dataSource);
}
