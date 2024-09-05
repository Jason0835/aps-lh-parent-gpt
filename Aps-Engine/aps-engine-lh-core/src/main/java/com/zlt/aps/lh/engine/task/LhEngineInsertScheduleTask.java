package com.zlt.aps.lh.engine.task;

import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.utils.SecurityUtils;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.engine.result.ValidateResult;
import com.zlt.aps.common.engine.service.AutoScheduleLogService;
import com.zlt.aps.common.engine.utils.BeanConverUtil;
import com.zlt.aps.lh.api.domain.dto.LhScheduleResultDto;
import com.zlt.aps.lh.engine.common.LhCommonService;
import com.zlt.aps.lh.engine.constants.LhEngineConstants;
import com.zlt.aps.lh.engine.domain.LhEngineScheduleResult;
import com.zlt.aps.lh.engine.exception.LhEngineException;
import com.zlt.aps.lh.engine.mapper.CommonLhEngineMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

import static com.alibaba.fastjson.JSON.toJSONString;

/**
 * 硫化工序插单引擎核心算法任务
 */
@Component("lhEngineInsertScheduleTask")
@Slf4j
public class LhEngineInsertScheduleTask {

    @Autowired
    private LhScheduleTaskCheck lhScheduleTaskCheck;

    @Autowired
    private LhCommonService lhCommonService;

    @Autowired
    private CommonLhEngineMapper commonLhEngineMapper;

    @Resource
    private AutoScheduleLogService autoScheduleLogService;

    private String division = "\r\n---------------------------------------------------\r\n";  //日志分割符

    /**
     * 硫化工序插单操作数据验证
     * @param lhScheduleResultDto
     * @return
     */
    public ValidateResult insertPreCheck(LhScheduleResultDto lhScheduleResultDto){
        StringBuilder errorMsg=new StringBuilder();
        //参数验证
        lhScheduleTaskCheck.validateInsertParams(lhScheduleResultDto,errorMsg);
        if(StringUtils.isNotEmpty(errorMsg)){
            lhScheduleResultDto.setIsSuccess(false);
            return ValidateResult.error(errorMsg.toString());
        }

        //验证施工相关信息
        lhScheduleTaskCheck.validateSapCode(lhScheduleResultDto,errorMsg);
        if(StringUtils.isNotEmpty(errorMsg)){
            lhScheduleResultDto.setIsSuccess(false);
            return ValidateResult.error(errorMsg.toString());
        }

        StringBuilder tipMsg=new StringBuilder();
        String scheduleDate= DateUtils.parseDateToStr("yyyy-MM-dd",lhScheduleResultDto.getScheduleDate());
        //重复插单校验
        lhScheduleTaskCheck.reInsertCheck(lhScheduleResultDto,scheduleDate,errorMsg);
        if(StringUtils.isNotEmpty(errorMsg)){
            lhScheduleResultDto.setIsSuccess(false);
            return ValidateResult.error(errorMsg.toString());
        }
        //根据排程日期验证自动排程抓取记录
        lhScheduleTaskCheck.checkLhAutoScheduleRecord(lhScheduleResultDto,scheduleDate,errorMsg,tipMsg);
        if(StringUtils.isNotEmpty(errorMsg)){
            return ValidateResult.error(errorMsg.toString());
        }
        //验证通过，确认提示信息
        lhScheduleResultDto.setIsSuccess(true); //设置验证通过
        if(StringUtils.isNotEmpty(tipMsg)){
            return ValidateResult.success(tipMsg.toString());
        }
        return ValidateResult.success();
    }

    /**
     * 硫化工序排程插单
     * @param lhScheduleResultDto
     */
    @Transactional
    public void lhScheduleResultInsertOrder(LhScheduleResultDto lhScheduleResultDto){
        //基础检查入参
        if(lhScheduleResultDto==null){
            throw new LhEngineException(I18nUtil.getMessage("lh.engine.input.params.empty.error"));
        }
        //检查验证是否通过
        if(!lhScheduleResultDto.getIsSuccess()){
            throw new LhEngineException(I18nUtil.getMessage("lh.engine.insert.no.check.error"));
        }
        String scheduleDate= DateUtils.parseDateToStr("yyyy-MM-dd",lhScheduleResultDto.getScheduleDate());
        //验证批次号
        String cxBatchNo=lhScheduleResultDto.getCxBatchNo();
        //硫化批次号
        String lhBatchNo=lhScheduleResultDto.getBatchNo();
        //硫化批次号为空，则需要重新进行生成抓取记录
        if(StringUtils.isEmpty(lhBatchNo)){
            lhBatchNo=lhCommonService.createBatchNo(LhEngineConstants.LH_AUTO_BATCH_NO_PREFIX,scheduleDate);
            //新生成硫化排程抓取记录
            lhScheduleTaskCheck.createRecord(cxBatchNo,lhBatchNo,scheduleDate,LhEngineConstants.LH_AUTO_RECORD_STATUS_SUCCESS);
        }
        //设置规格明细
        lhScheduleTaskCheck.validateSapCode(lhScheduleResultDto,new StringBuilder());
        //对象转换
        LhEngineScheduleResult insertScheduleResult= BeanConverUtil.conver(lhScheduleResultDto, LhEngineScheduleResult.class);
        Double lhTime=lhCommonService.getSingleLhTime(insertScheduleResult.getSapCode());
        insertScheduleResult.setLhTime(Double.valueOf(lhTime));//硫化时长
        insertScheduleResult.setOrderNo(lhCommonService.createOrderNo(LhEngineConstants.LH_AUTO_ORDER_NO_PREFIX,scheduleDate));
        insertScheduleResult.setProductionStatus(LhEngineConstants.LH_SCHEDULE_PRODUCT_STATUS_UNDO);
        insertScheduleResult.setIsRelease(LhEngineConstants.LH_SCHEDULE_IS_RELEASE_NO);//未发布
        //Joran 2021-11-02 设置日计划量
        setScheduleDelayPlan(insertScheduleResult);
        commonLhEngineMapper.insertLhEngineScheduleResult(insertScheduleResult);
        StringBuilder logDetail= new StringBuilder();
        logDetail.append("操作人员：").append(SecurityUtils.getUsername()).append(division);
        logDetail.append("操作时间：").append(DateUtils.getTime()).append(division);
        logDetail.append("排程结果数据：").append(toJSONString(insertScheduleResult)).append(division);
        //插入插单日志日志
        autoScheduleLogService.insertLhScheduleLog(insertScheduleResult.getBatchNo(), insertScheduleResult.getOrderNo(), "【插单】生成硫化排程数据",
                logDetail.toString()); //添加日志
    }

    /**
     * 设置日计划量
     * @param lhEngineScheduleResult
     */
    private void setScheduleDelayPlan(LhEngineScheduleResult lhEngineScheduleResult) {
        //初始化各个班次硫化量
        lhEngineScheduleResult.initLhPlanQty();
        Integer delayPlanQty=0;
        delayPlanQty+=lhEngineScheduleResult.getClass1PlanQty();
        delayPlanQty+=lhEngineScheduleResult.getClass2PlanQty();
        delayPlanQty+=lhEngineScheduleResult.getClass3PlanQty();
        lhEngineScheduleResult.setDailyPlanQty(delayPlanQty);
    }


}
