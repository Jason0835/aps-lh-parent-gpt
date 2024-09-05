package com.zlt.aps.lh.engine.task;

import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.engine.domain.LhEngineTireConstructionInfo;
import com.zlt.aps.common.engine.utils.BeanConverUtil;
import com.zlt.aps.cx.api.domain.entity.CxScheduleResult;
import com.zlt.aps.lh.api.domain.dto.LhScheduleResultDto;
import com.zlt.aps.lh.engine.common.LhCommonService;
import com.zlt.aps.lh.engine.constants.LhEngineConstants;
import com.zlt.aps.lh.engine.domain.LhEngineScheduleResult;
import com.zlt.aps.lh.engine.enums.LhClassShiftEnum;
import com.zlt.aps.lh.engine.exception.LhEngineException;
import com.zlt.aps.lh.engine.mapper.CommonCxEngineMapper;
import com.zlt.aps.lh.engine.mapper.CommonLhEngineMapper;
import com.zlt.aps.lh.engine.service.LhEngineAutoScheduleRecordService;
import com.zlt.aps.lh.engine.util.LhEngineScheduleUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
  * 硫化排程导入引擎处理任务
  * @ClassName LhEngineScheduleImportTask
  * @Description TODO
  * @Author Joran.Zhang
  * @Date 2021/8/12 14:14
  * @Version 1.0
**/
@Component("lhEngineScheduleImportTask")
@Slf4j
public class LhEngineScheduleImportTask {

    @Autowired
    private LhScheduleTaskCheck lhScheduleTaskCheck;

    @Autowired
    private LhEngineAutoScheduleRecordService lhEngineAutoScheduleRecordService;

    @Autowired
    private LhCommonService lhCommonService;
    @Autowired
    private CommonCxEngineMapper commonCxEngineMapper;
    @Autowired
    private CommonLhEngineMapper commonLhEngineMapper;

    /**
     * 硫化排程导入数据填充校验
     * @param lhScheduleResultDtoList
     * @param scheduleDate
     * @return
     * @throws LhEngineException
     */
    @Transactional
    public void lhScheduleImport(List<LhScheduleResultDto> lhScheduleResultDtoList, Map<String,List<LhEngineTireConstructionInfo>> sapTireConstructionListMap, Date scheduleDate) throws LhEngineException {
        //验证数据集合是否为空
        if(StringUtils.isEmpty(lhScheduleResultDtoList)){
            throw new LhEngineException(I18nUtil.getMessage("lh.engine.import.importData.empty.error"));
        }
        //验证排程日期
        if(scheduleDate==null){
            throw new LhEngineException(I18nUtil.getMessage("lh.engine.import.scheduleDate.empty.error"));
        }
        String scheduleDateStr= DateUtils.parseDateToStr("yyyy-MM-dd",scheduleDate);
        Map<String,String> cxParams=lhCommonService.loadCxParams();
        //获取成型排程
        CxScheduleResult scheduleResult=new CxScheduleResult();
        scheduleResult.setScheduleDateStr(scheduleDateStr);
        List<CxScheduleResult> cxScheduleResultList=this.commonCxEngineMapper.selectCxScheduleResultList(scheduleResult);
        //项目经理确认，如果成型还没有自动排程的话，允许直接插入
        String cxBatchNo="";
       /* if(StringUtils.isEmpty(cxScheduleResultList)){
            throw new LhEngineException(I18nUtil.getMessage("lh.engine.import.cxScheduleResult.empty.error"));
        }*/
        //获取成型批次号
        if(StringUtils.isNotEmpty(cxScheduleResultList)){
            cxBatchNo=cxScheduleResultList.get(0).getCxBatchNo();
        }
        //数据转换
        List<LhEngineScheduleResult> lhEngineScheduleResultList= BeanConverUtil.converList(lhScheduleResultDtoList,LhEngineScheduleResult.class);
        //创建硫化自动排程批次号
        String lhBatchNo=lhCommonService.createBatchNo(LhEngineConstants.LH_AUTO_BATCH_NO_PREFIX,scheduleDateStr);
        //删除硫化自动排程记录表
        lhEngineAutoScheduleRecordService.reGenerageRecord(cxBatchNo,lhBatchNo,scheduleDateStr, LhEngineConstants.LH_AUTO_RECORD_STATUS_SUCCESS);
        //删除硫化排程
        lhScheduleTaskCheck.syncScheduleToLog(scheduleDateStr);
        for(LhEngineScheduleResult lhEngineScheduleResult:lhEngineScheduleResultList){
            lhEngineScheduleResult.initLhPlanQty();
            //创建工单号
            lhEngineScheduleResult.setOrderNo(lhCommonService.createOrderNo(LhEngineConstants.LH_AUTO_ORDER_NO_PREFIX,scheduleDateStr));
            //硫化时长
            lhEngineScheduleResult.setLhTime(Double.valueOf(lhCommonService.getSingleLhTime(lhEngineScheduleResult.getSapCode(),sapTireConstructionListMap)));
            lhEngineScheduleResult.setProductionStatus(LhEngineConstants.LH_SCHEDULE_PRODUCT_STATUS_UNDO);
            //计算各个班次硫化开始时间结束时间
            calcClassShiftTime(scheduleDate,lhEngineScheduleResult,cxParams);
            lhEngineScheduleResult.setIsRelease(LhEngineConstants.LH_SCHEDULE_IS_RELEASE_NO);//未发布
        }
        //批量插入
        if(StringUtils.isNotEmpty(lhEngineScheduleResultList)){
            commonLhEngineMapper.batchInsertLhScheduleResult(lhEngineScheduleResultList);
        }

    }

    /**
     * 导入结合排程日期以及班次计划量计算出排程对应的班次开始时间结束时间
     * @param scheduleDate
     * @param lhEngineScheduleResult
     */
    private void calcClassShiftTime(Date scheduleDate, LhEngineScheduleResult lhEngineScheduleResult,Map<String,String> cxParams) {
        //2022-06-13 与建伟确认，一个机台不会存在两个规格计划，所以按单规格计算时间即可
        //硫化白班开始时间
        //排程日期格式化
        Date lastDate=DateUtils.addDays(scheduleDate,-1);
        Date classShiftBeginTime= LhEngineScheduleUtils.formatDateByZero(lastDate);
        //昨日白班的开班时间
        Date lastDayThreeShiftBeginTime=DateUtils.addHours(classShiftBeginTime,8);
        Integer planQty=0;
        for(LhClassShiftEnum lhClassShiftEnum:LhClassShiftEnum.values()){
            Date shiftBeginTime= DateUtils.addHours(lastDayThreeShiftBeginTime,8 * lhClassShiftEnum.getClassIndex());
            planQty=LhEngineScheduleUtils.getLhClassPlanQty(lhEngineScheduleResult,lhClassShiftEnum);
            if(planQty>0){
                LhEngineScheduleUtils.setClassShiftStartTime(lhEngineScheduleResult,lhClassShiftEnum,shiftBeginTime);
                //计算结束时间,如果有写左右模信息则按单模进行计算，否则按双模
                Integer useMoldNumber=StringUtils.isEmpty(lhEngineScheduleResult.getLeftRightMold())?2:1;
                //开始计算班次结束时间
                calcClassShiftEndTime(lhEngineScheduleResult,lhClassShiftEnum,shiftBeginTime,planQty,useMoldNumber,cxParams);
            }
        }
    }

    /**
     * 计算班次结束时间
     * @param lhEngineScheduleResult 当前硫化排程对象
     * @param cls 当前班次
     * @param classShiftBeginTime 班次开始时间
     * @param planQty 计划量
     * @param useMoldNumber 使用模数
     */
    private void calcClassShiftEndTime(LhEngineScheduleResult lhEngineScheduleResult, LhClassShiftEnum cls, Date classShiftBeginTime, Integer planQty, Integer useMoldNumber,Map<String,String> cxParams) {
        if(planQty==null || useMoldNumber==null){
            log.error("计算班次结束时间异常，不进行计算");
        }
        //如果为2个模的时候，则计划量/2 *单胎硫化时长 计算出总消耗时长
        int singleMoldPlanQty= (int) Math.ceil(planQty / useMoldNumber);

        Double singleTireLhTime=lhEngineScheduleResult.getLhTime();
        Integer brushBagTime=lhCommonService.getBrushBagTime(cxParams);
        //向上取整
        BigDecimal useTotalTimeBigDecimal=BigDecimal.valueOf(singleMoldPlanQty).multiply(BigDecimal.valueOf(singleTireLhTime + brushBagTime)).setScale(2, RoundingMode.UP);

        //换算成秒来计算
        Integer useTotalTimeSecond=useTotalTimeBigDecimal.multiply(BigDecimal.valueOf(60)).setScale(0,RoundingMode.CEILING).intValue();

        Date classShiftEndTime=DateUtils.addSeconds(classShiftBeginTime,useTotalTimeSecond);
        //保存计算的结束时间
        LhEngineScheduleUtils.setClassShiftEndTime(lhEngineScheduleResult,cls,classShiftEndTime);

    }


}
