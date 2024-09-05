package com.zlt.aps.lh.engine.service.impl;

import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.engine.domain.LhEngineTireConstructionInfo;
import com.zlt.aps.common.engine.result.ValidateResult;
import com.zlt.aps.cx.api.domain.entity.CxChangeLhMachine;
import com.zlt.aps.cx.api.domain.entity.CxScheduleResult;
import com.zlt.aps.lh.api.domain.dto.LhScheduleResultDto;
import com.zlt.aps.lh.api.domain.entity.LhMoldChangePlan;
import com.zlt.aps.lh.engine.exception.LhEngineException;
import com.zlt.aps.lh.engine.service.LhEngineService;
import com.zlt.aps.lh.engine.task.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 硫化工序引擎相关接口
 */
@Service("lhEngineService")
@Slf4j
public class LhEngineServiceImpl implements LhEngineService {

    @Autowired
    private MoldChangePlanTask moldChangePlanTask;

    @Autowired
    private LhEngineInsertScheduleTask lhEngineInsertScheduleTask;

    @Autowired
    private LhEngineChangeMachineScheduleTask lhEngineChangeMachineScheduleTask;

    @Autowired
    private LhEngineScheduleImportTask lhEngineScheduleImportTask;

    @Autowired
    private LhEngineNewAutoScheduleTask lhEngineNewAutoScheduleTask;

    /**
     * 生成模具变动单,调用校验接口及生成接口
     * @param scheduleDate
     * @throws LhEngineException
     */
    @Override
    public String moldChangePlanTask(String scheduleDate) throws LhEngineException {
        String checkMsg=moldChangePlanTask.preChangePlanCheck(scheduleDate);
        if(StringUtils.isEmpty(checkMsg)){
            moldChangePlanTask.moldChangePlanTask(scheduleDate);
        }
       return  checkMsg;
    }

    /**
     * 根据成型排程结果表进行单条排程模具变动单生成
     * @param cxScheduleResult
     * @throws LhEngineException
     */
    @Override
    public void singleMoldChangePlanTask(CxScheduleResult cxScheduleResult,List<String> historyMachineCodeList) throws LhEngineException {
        moldChangePlanTask.singleMoldChangePlanTask(cxScheduleResult,historyMachineCodeList);
    }

    /**
     *  单排程确认硫化机后进行模具变动单生成
     * @param cxScheduleResult 成型排程结果
     * @param historyMachineCodeList 历史排程
     * @param list 成型排程硫化机关系列表
     * @throws LhEngineException
     */
    @Override
    public void singleMoldChangePlanTaskByChange(CxScheduleResult cxScheduleResult, List<String> historyMachineCodeList, List<CxChangeLhMachine> list) throws LhEngineException {
        moldChangePlanTask.singleMoldChangePlanTaskByChange(cxScheduleResult,historyMachineCodeList,list);
    }

    /**
     * 添加模具变动单进行验证
     * @param lhMoldChangePlan
     * @return
     */
    @Override
    public ValidateResult insertChangePlanTask(LhMoldChangePlan lhMoldChangePlan) {
        return moldChangePlanTask.moldPlanPreCheck(lhMoldChangePlan);
    }

    /**
     * 硫化工序自动排程入口
     * @param scheduleDate
     * @throws LhEngineException
     */
    @Override
    public void autoLhSchedule(Date scheduleDate) throws LhEngineException {
        String dateStr= DateUtils.parseDateToStr("yyyy-MM-dd",scheduleDate);
         //lhEngineAutoScheduleTask.autoSchedule(dateStr);
        lhEngineNewAutoScheduleTask.autoSchedule(dateStr);
    }

    /**
     * 插单前置校验数据填充方法
     * @param lhScheduleResultDto
     * @return
     */
    @Override
    public ValidateResult inertLhScheduleResultPreCheck(LhScheduleResultDto lhScheduleResultDto) {
        return lhEngineInsertScheduleTask.insertPreCheck(lhScheduleResultDto);
    }

    /**
     * 硫化插单引擎
     * @param lhScheduleResultDto
     * @throws LhEngineException
     */
    @Override
    public void insertLhScheduleOrder(LhScheduleResultDto lhScheduleResultDto)throws LhEngineException  {
        if(lhScheduleResultDto==null){
            throw new LhEngineException(I18nUtil.getMessage("lh.engine.input.params.empty.error"));
        }
        lhEngineInsertScheduleTask.lhScheduleResultInsertOrder(lhScheduleResultDto);
    }

    /**
     * 转机台引擎前置校验
     * @param lhScheduleResultDto
     * @param changeLhMachineCode 转入机台
     * @return
     */
    @Override
    public ValidateResult changeLhMachinePreCheck(LhScheduleResultDto lhScheduleResultDto, String changeLhMachineCode) {
        if(lhScheduleResultDto==null){
            throw new LhEngineException(I18nUtil.getMessage("lh.engine.input.params.empty.error"));
        }
        if(StringUtils.isEmpty(changeLhMachineCode)){
            throw new LhEngineException(I18nUtil.getMessage("lh.engine.input.machineCode.empty.error"));
        }

        return lhEngineChangeMachineScheduleTask.changeMachinePreCheck(lhScheduleResultDto,changeLhMachineCode);
    }

    /**
     * 执行转机台操作排程结果更新硫化机台
     * @param lhScheduleResultDto
     * @param changeLhMachineCode
     * @throws LhEngineException
     */
    @Override
    public void changeLhMachine(LhScheduleResultDto lhScheduleResultDto, String changeLhMachineCode) throws LhEngineException {
        if(lhScheduleResultDto==null){
            throw new LhEngineException(I18nUtil.getMessage("lh.engine.input.params.empty.error"));
        }
        if(StringUtils.isEmpty(changeLhMachineCode)){
            throw new LhEngineException(I18nUtil.getMessage("lh.engine.input.machineCode.empty.error"));
        }
        lhEngineChangeMachineScheduleTask.changeMachine(lhScheduleResultDto,changeLhMachineCode);
    }

    /**
     * 硫化排程批量导入
     * @param lhScheduleResultDtoList
     * @param scheduleDate
     * @throws LhEngineException
     */
    @Override
    public void lhScheduleResultImport(List<LhScheduleResultDto> lhScheduleResultDtoList, Map<String,List<LhEngineTireConstructionInfo>> sapTireConstructionListMap, Date scheduleDate) throws LhEngineException {
        lhEngineScheduleImportTask.lhScheduleImport(lhScheduleResultDtoList,sapTireConstructionListMap,scheduleDate);
    }
}
