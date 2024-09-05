package com.zlt.aps.cx.engine.task;

import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.utils.SecurityUtils;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.engine.domain.EngineProductConstructionInfo;
import com.zlt.aps.common.engine.result.ValidateResult;
import com.zlt.aps.common.engine.service.AutoScheduleLogService;
import com.zlt.aps.common.engine.utils.BeanConverUtil;
import com.zlt.aps.cx.api.domain.entity.CxMachineInfo;
import com.zlt.aps.cx.api.domain.entity.CxScheduleResult;
import com.zlt.aps.cx.engine.constants.CxEngineConstants;
import com.zlt.aps.cx.engine.constants.CxPrefixConstants;
import com.zlt.aps.cx.engine.domain.CxEngineScheduleResult;
import com.zlt.aps.cx.engine.exception.CxScheduleEngineException;
import com.zlt.aps.cx.engine.mapper.CxScheduleEngineMapper;
import com.zlt.aps.cx.engine.service.CxEngineSpecifyMachineService;
import com.zlt.aps.cx.engine.utils.CxScheduleUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static com.zlt.aps.common.core.utils.ApsCommonUtil.logSplit;

/**
 * 成型排程转机台引擎
 */
@Component("changeMachineTaskService")
public class ChangeMachineTaskService {

    @Autowired
    private CxScheduleEngineMapper cxScheduleEngineMapper;

    @Resource
    private AutoScheduleLogService autoScheduleLogService;

    @Autowired
    private CxEngineSpecifyMachineService cxEngineSpecifyMachineService;

    @Autowired
    private ScheduleCheckService scheduleCheckService;

    /**
     * 转机台验证
     * @param cxScheduleResult 排程结果
     * @param machineCode 要转换的机台
     * @return
     */
    public ValidateResult preChangeCheck(CxScheduleResult cxScheduleResult,String machineCode){
        init();//重复调用重新初始化
        //前一步参数非空校验
        StringBuilder errorMsg=new StringBuilder();
        String sourceMachineCode=cxScheduleResult.getCxMachineCode();
        if(StringUtils.isEmpty(sourceMachineCode)){
            errorMsg.append(I18nUtil.getMessage("cx.engine.change.machine.task.no.machine.error"));
        }
        if(StringUtils.isNotEmpty(sourceMachineCode)&&sourceMachineCode.equals(machineCode)){
            return ValidateResult.error(I18nUtil.getMessage("cx.engine.change.machine.same.tip"));
        }
        //1.验证当前转机台机台状态信息和机台信息
        //验证成型机
        CxMachineInfo cxMachineInfo=scheduleCheckService.validateCxMachine(machineCode,errorMsg);
        Double dimension=cxScheduleResult.getSpecDimension();//获取当前规格的寸口
        if(dimension==null){
            errorMsg.append(I18nUtil.getMessage("cx.engine.change.machine.schedule.dimension.error"));
        }

        //获取到施工信息
        EngineProductConstructionInfo engineConstructionInfo=scheduleCheckService.validateConstructionInfo(cxScheduleResult.getEmbryoCode(),cxScheduleResult.getBomDataVersion(),errorMsg);
        if(StringUtils.isNotEmpty(errorMsg)){
            return ValidateResult.error(errorMsg.toString());
        }
        //验证施工信息寸口
        if(engineConstructionInfo.getDimension()==null){
            errorMsg.append(I18nUtil.getMessage("cx.engine.construction.dimension.empty.error")) ;
        }

        StringBuilder tipMsg=new StringBuilder();
        //寸口验证提示语
        scheduleCheckService.checkDimension(cxMachineInfo,engineConstructionInfo,tipMsg);
        //验证当前成型机排产的规格寸口
        CxEngineScheduleResult condition=new CxEngineScheduleResult();
        condition.setScheduleDate(cxScheduleResult.getScheduleDate());
        condition.setCxMachineCode(machineCode);
        List<CxEngineScheduleResult> cxEngineScheduleResultList=this.cxScheduleEngineMapper.selectCxScheduleResultList(condition);
        //验证在转入机台是否存在投产规格
        String existKey= CxScheduleUtils.getMapKeyByInputString(cxScheduleResult.getSapCode(),cxScheduleResult.getEmbryoCode(),cxScheduleResult.getBomDataVersion());
        //如果存在排程记录则对寸口进行验证，没有排程任务则不进行规格寸口比对
        if(StringUtils.isNotEmpty(cxEngineScheduleResultList)){

            //Joran 2022-03-11 将所有机台的寸口全部列出来start
            StringBuilder dimensionDetail=new StringBuilder();
            Set<Integer> existDimension=new TreeSet<>();
            for(CxEngineScheduleResult cxEngineScheduleResult:cxEngineScheduleResultList){
                Double resultSpecDimension=cxEngineScheduleResult.getSpecDimension();//获取当前规格的寸口
                if(resultSpecDimension==null){
                    continue;
                }
                Integer specDimension= BigDecimal.valueOf(resultSpecDimension).intValue();
                if(!existDimension.contains(specDimension)){
                    existDimension.add(specDimension);
                    if(StringUtils.isNotEmpty(dimensionDetail)){
                        dimensionDetail.append(",").append(specDimension);
                    }else{
                        dimensionDetail.append(specDimension);
                    }
                }
            }
            //Joran 2022-03-11 将所有机台的寸口全部列出来end
            for(CxEngineScheduleResult cxEngineScheduleResult:cxEngineScheduleResultList){
                Double resultSpecDimension=cxEngineScheduleResult.getSpecDimension();//获取当前规格的寸口
                if(resultSpecDimension==null){
                    continue;
                }
                if(!dimension.equals(resultSpecDimension)){
                    Integer specDimension= BigDecimal.valueOf(dimension).intValue();
                    tipMsg.append(StringUtils.format(I18nUtil.getMessage("cx.engine.change.machine.dimension.different.tip"),dimensionDetail.toString(),specDimension));
                    break;//直接跳出循环
                }
             //验证转入机台是否生产该规格
             String machKey=CxScheduleUtils.getMapKeyByInputString(cxEngineScheduleResult.getSapCode(),cxEngineScheduleResult.getEmbryoCode(),cxEngineScheduleResult.getBomDataVersion());
             if(machKey.equals(existKey)){
                 tipMsg.append(StringUtils.format(I18nUtil.getMessage("cx.engine.change.machine.exist.specInfo.tip"),resultSpecDimension,dimension));
                 break;//直接跳出循环
             }

            }
        }

        //成型机定点相关信息验证
        this.cxEngineSpecifyMachineService.validateSpecifyMachine(cxScheduleResult.getSapCode(),cxScheduleResult.getEmbryoCode(),machineCode,tipMsg);
        //验证定额班次计划量
        CxScheduleResult validateScheduleResult= BeanConverUtil.conver(cxScheduleResult,CxScheduleResult.class);
        validateScheduleResult.setCxMachineCode(machineCode);
        //提示各个有计划的班次可安排最大的计划量
        scheduleCheckService.validateClassShiftPlanQty(validateScheduleResult,tipMsg);
        //Joran 2021-11-05 验证通过标记到redis
        scheduleCheckService.validateRedisMark(scheduleCheckService.createKey(SecurityUtils.getUsername(), CxPrefixConstants.CX_CHANGE_MACHINE_PREFIX));
        if(StringUtils.isNotEmpty(tipMsg)){
            return ValidateResult.success(tipMsg.toString());
        }
        return ValidateResult.success();
    }

    /**
     * 转机台操作
     * @param cxScheduleResult
     * @param machineCode
     */
    @Transactional
    public void changeCxMachine(CxScheduleResult cxScheduleResult,String machineCode) throws CxScheduleEngineException{
        //Joran 2021-11-04验证是否通过没通过是没有设置键
        String key= scheduleCheckService.createKey(SecurityUtils.getUsername(),CxPrefixConstants.CX_CHANGE_MACHINE_PREFIX);
        if(!scheduleCheckService.isValidate(key)){
            throw new CxScheduleEngineException(I18nUtil.getMessage("cx.engine.change.machine.no.check.error"));
        }
        init();//重复调用重新初始化

        CxEngineScheduleResult cxEngineScheduleResult =new CxEngineScheduleResult();
        BeanUtils.copyProperties(cxScheduleResult,cxEngineScheduleResult);
        //验证通过后进行机台变更
        StringBuilder errorMsg=new StringBuilder();
        CxMachineInfo beforeCxMachineInfo=scheduleCheckService.validateCxMachine(cxScheduleResult.getCxMachineCode(),new StringBuilder());
        String beforeCxMachineName="已停用或禁用";
        if(beforeCxMachineInfo!=null){
            beforeCxMachineName=beforeCxMachineInfo.getMachineName();
        }
        CxMachineInfo cxMachineInfo=scheduleCheckService.validateCxMachine(machineCode,errorMsg);
        if(StringUtils.isNotEmpty(errorMsg)){
            throw new CxScheduleEngineException(errorMsg.toString());
        }

        cxEngineScheduleResult.setCxMachineName(cxMachineInfo.getMachineName());
        cxEngineScheduleResult.setCxMachineCode(cxMachineInfo.getMachineCode());
        //Joran 2021-12-15 机台类别冗余到成型排程结果表
        cxEngineScheduleResult.setCxMachineType(cxMachineInfo.getMachineType());
        //班制
        cxEngineScheduleResult.setWorkShifts(StringUtils.isEmpty(cxMachineInfo.getClassShift())?3:Integer.valueOf(cxMachineInfo.getClassShift()));
        cxEngineScheduleResult.setUpdateBy(SecurityUtils.getUsername());
        cxEngineScheduleResult.setUpdateTime(DateUtils.getNowDate());
        cxEngineScheduleResult.setRemark("原始机台：" + beforeCxMachineName+",转入机台：" + cxMachineInfo.getMachineName());
        this.cxScheduleEngineMapper.updateScheduleCxMachine(cxEngineScheduleResult);
        //插入转机台日志
        autoScheduleLogService.insertCxScheduleLog(cxEngineScheduleResult.getCxBatchNo(), cxEngineScheduleResult.getOrderNo(), "转机台日志",
                logSplit("原始机台：" + beforeCxMachineName, ",转入机台：" + cxMachineInfo.getMachineName(),"操作人员："+ SecurityUtils.getUsername(),"操作时间："+DateUtils.getTime())); //添加日志

        //Joran 2021-11-05 进行移除验证通过标记
        scheduleCheckService.delValidateRedisMark(key);
    }

    /**
     * 每次调用检查前都要先清空属性防止缓存
     */
    public void init(){
        scheduleCheckService.initBaseData();
    }

}
