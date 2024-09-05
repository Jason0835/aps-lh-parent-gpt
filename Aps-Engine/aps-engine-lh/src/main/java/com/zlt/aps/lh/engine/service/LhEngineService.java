package com.zlt.aps.lh.engine.service;


import com.zlt.aps.common.engine.domain.LhEngineTireConstructionInfo;
import com.zlt.aps.common.engine.result.ValidateResult;
import com.zlt.aps.cx.api.domain.entity.CxChangeLhMachine;
import com.zlt.aps.cx.api.domain.entity.CxScheduleResult;
import com.zlt.aps.lh.api.domain.dto.LhScheduleResultDto;
import com.zlt.aps.lh.api.domain.entity.LhMoldChangePlan;
import com.zlt.aps.lh.engine.exception.LhEngineException;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 硫化工序引擎相关统一入口
 */
public interface LhEngineService {

    /**
     * 模具变动单生成接口
     * @param scheduleDate
     */
    public String moldChangePlanTask(String scheduleDate) throws LhEngineException;

    /**
     * 根据成型排程结果表进行单条排程模具变动单生成
     * @param cxScheduleResult 成型排程结果
     * @param historyMachineCodeList 旧的机台信息
     * @throws LhEngineException
     */
    void singleMoldChangePlanTask(CxScheduleResult cxScheduleResult,List<String> historyMachineCodeList) throws LhEngineException;

    /**
     *  根据成型排程和历史的机台列表以及 当前最新的硫化机关系列表进行模具变动单生成
     * @param cxScheduleResult 成型排程结果
     * @param historyMachineCodeList 历史排程
     * @param list
     * @throws LhEngineException
     */
    void singleMoldChangePlanTaskByChange(CxScheduleResult cxScheduleResult,List<String> historyMachineCodeList,List<CxChangeLhMachine> list) throws LhEngineException;

    /**
     * 新增模具变动单
     * @param lhMoldChangePlan
     * @return
     */
    public ValidateResult insertChangePlanTask(LhMoldChangePlan lhMoldChangePlan);

    /**
     * 硫化自动排程
     * @param scheduleDate
     * @return
     * @throws LhEngineException
     */
    public void autoLhSchedule(Date scheduleDate) throws LhEngineException;

    /**
     * 硫化排程插单
     * @param lhScheduleResultDto
     * @return
     */
    public  ValidateResult inertLhScheduleResultPreCheck(LhScheduleResultDto lhScheduleResultDto);

    /**
     * 硫化插单引擎
     * @param lhScheduleResultDto
     */
    public void insertLhScheduleOrder(LhScheduleResultDto lhScheduleResultDto) throws LhEngineException;

    /**
     * 转机台前置验证
     * @param lhScheduleResultDto
     * @param changeLhMachineCode 转入机台
     * @return
     */
    public  ValidateResult changeLhMachinePreCheck(LhScheduleResultDto lhScheduleResultDto,String changeLhMachineCode);

    /**
     * 转机台数据更新
     * @param lhScheduleResultDto
     * @param changeLhMachineCode
     * @throws LhEngineException
     */
    public void changeLhMachine(LhScheduleResultDto lhScheduleResultDto,String changeLhMachineCode) throws LhEngineException;

    /**
     * 硫化工序排程数据批量导入
     * @param lhScheduleResultDtoList
     * @param scheduleDate
     * @throws LhEngineException
     */
    public void lhScheduleResultImport(List<LhScheduleResultDto> lhScheduleResultDtoList, Map<String,List<LhEngineTireConstructionInfo>> sapTireConstructionListMap, Date scheduleDate) throws LhEngineException;
}
