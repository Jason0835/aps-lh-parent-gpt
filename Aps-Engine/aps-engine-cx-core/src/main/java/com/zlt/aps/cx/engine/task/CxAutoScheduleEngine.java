package com.zlt.aps.cx.engine.task;

import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.engine.domain.LhEngineTireConstructionInfo;
import com.zlt.aps.common.engine.domain.MdmMonthProdPlan;
import com.zlt.aps.common.engine.service.AutoScheduleLogService;
import com.zlt.aps.common.engine.service.LhEngineTireConstructionInfoService;
import com.zlt.aps.common.engine.service.MdmMonthPlanMainService;
import com.zlt.aps.common.engine.service.MdmMonthProdPlanService;
import com.zlt.aps.common.engine.utils.BeanConverUtil;
import com.zlt.aps.cx.engine.common.CommonCacheService;
import com.zlt.aps.cx.engine.constants.CxEngineConstants;
import com.zlt.aps.cx.engine.constants.CxPrefixConstants;
import com.zlt.aps.cx.engine.domain.CxEngineAutoScheduleRecord;
import com.zlt.aps.cx.engine.domain.CxEngineLastDaySupplePlan;
import com.zlt.aps.cx.engine.domain.CxEngineScheduleResult;
import com.zlt.aps.cx.engine.domain.CxPlanProductStatus;
import com.zlt.aps.cx.engine.exception.CxScheduleEngineException;
import com.zlt.aps.cx.engine.mapper.CxLhEngineCommonMapper;
import com.zlt.aps.cx.engine.mapper.CxScheduleEngineMapper;
import com.zlt.aps.cx.engine.service.CxEngineAutoScheduleRecordService;
import com.zlt.aps.cx.engine.service.CxEngineLastDaySupplePlanService;
import com.zlt.aps.cx.engine.service.CxPlanProductStatusService;
import com.zlt.aps.cx.engine.utils.CxScheduleUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * 成型自动排程引擎
 */
@Component("cxAutoScheduleEngine")
@Slf4j
public class CxAutoScheduleEngine {
    @Autowired
    private CxScheduleEngineMapper cxScheduleEngineMapper;

    @Autowired
    private MdmMonthPlanMainService mdmMonthPlanMainService;

    @Autowired
    private CommonCacheService cacheService;

    @Autowired
    private CxEngineAutoScheduleRecordService cxEngineAutoScheduleRecordService;

    @Autowired
    private CxPlanProductStatusService cxPlanProductStatusService;

    @Autowired
    private MdmMonthProdPlanService mdmMonthProdPlanService;

    @Autowired
    private AutoScheduleLogService autoScheduleLogService;

    @Autowired
    private CxEngineLastDaySupplePlanService cxEngineLastDaySupplePlanService;

    @Autowired
    private CxLhEngineCommonMapper cxLhEngineCommonMapper;

    @Autowired
    private CxAutoScheduleEngineService cxAutoScheduleEngineService;

    @Autowired
    private LhEngineTireConstructionInfoService lhEngineTireConstructionInfoService;

    private String division = "\r\n---------------------------------------------------\r\n";  //日志分割符

    /**
     * 根据排程日期进行成型计划自动排程
     * @param cxMachineCode 机台编码
     * @param scheduleDate 自动排程日期
     * @throws CxScheduleEngineException
     */
    @Transactional
    public synchronized void autoSchedule(String cxMachineCode,Date scheduleDate) throws CxScheduleEngineException {
        String scheduleDateStr= DateUtils.parseDateToStr("yyyyMMdd",scheduleDate);
        //成型自动排程批次号
        String cxBatchNo=cacheService.getCxSequence(CxPrefixConstants.SCHEDULE_BATCH_NO_PREFIX+scheduleDateStr, CxPrefixConstants.CX_BATCH_NO_PREFIX+scheduleDateStr);
        //删除成型自动排程记录表
        cxEngineAutoScheduleRecordService.deleteAutoScheduleRecordByScheduleDate(scheduleDateStr);
        //删除成型排程结果表数据
        syncCxScheduleToLog(scheduleDateStr,cxMachineCode,"");

        //获取月度计划版本信息
        String monthPlanApsVersion=cacheService.getMdmMonthPlanMainByDate(scheduleDate,cxBatchNo);

        StringBuilder logDetail = new StringBuilder("【成型自动排程前置任务调整为增补计划表日志】：").append(division);
        String title="【成型自动排程前置任务调整为增补计划表日志】";

        //获取增补计划（前一日排程全部计划）
        List<CxEngineScheduleResult>  lastDayTaskList = getCxEngineLastDaySupplePlans(cxMachineCode, scheduleDate, cxBatchNo, logDetail);

        //Joran 2022-02-23 查询汇总月度计划初稿数据到排程结果表最新计划数中
        cacheService.updateNewestPlanQty(lastDayTaskList,scheduleDate);

        //更新库存到排程对应规格
        StringBuilder updateStockLog =new StringBuilder();
        cacheService.updateLastDayTaskStock(lastDayTaskList,scheduleDate,updateStockLog,false);
        if(StringUtils.isNotEmpty(updateStockLog)){
            logDetail.append(updateStockLog);
        }
        //移除已收尾的数据
        cacheService.closeOutRemove(monthPlanApsVersion,lastDayTaskList,null,true);
        if(StringUtils.isEmpty(lastDayTaskList)){
            //前一天全部收尾，错误提示
            throw  new CxScheduleEngineException(I18nUtil.getMessage("cx.engine.auto.lastDay.all.closeOutAll.error"));
        }

        //硫化外胎施工信息start
        LhEngineTireConstructionInfo lhEngineTireConstructionInfo=new LhEngineTireConstructionInfo();
        List<LhEngineTireConstructionInfo> constructionInfoList=lhEngineTireConstructionInfoService.selectLhTireConstructionInfoList(lhEngineTireConstructionInfo);
        Map<String,List<LhEngineTireConstructionInfo>> sapTireConstructionListMap=new HashMap<>();
        if(StringUtils.isNotEmpty(constructionInfoList)){
            sapTireConstructionListMap=constructionInfoList.stream().collect(Collectors.groupingBy(lhEngineScheduleResult -> lhEngineScheduleResult.getSapCode()));
        }
        //硫化外胎施工信息end

        //复制前一天次日一班的数据到新任务一班中
        List<CxEngineScheduleResult> newTaskList=new ArrayList<>(lastDayTaskList.size());
        StringBuilder copyLog=new StringBuilder();
        cacheService.copyLastDayTaskToNewTask(cxBatchNo,newTaskList,lastDayTaskList,sapTireConstructionListMap,scheduleDate,false,copyLog);
        if(StringUtils.isNotEmpty(copyLog)){
            logDetail.append(copyLog);
        }

        //Joran 2021-12-23 对比当前排程已经在排程计划中的直接移除这一部分投产数据start
        cacheService.removePlanProductStatusList(monthPlanApsVersion,scheduleDate);
        //Joran 2021-12-23 对比当前排程已经在排程计划中的直接移除这一部分投产数据end
        List<CxPlanProductStatus> cxPlanProductStatusList=cacheService.getPlanProductStatusByApsVersion(monthPlanApsVersion,null,scheduleDate);

        //Joran 2021-12-31 根据前一天计划列表进行投产状态表更新start
        if(StringUtils.isNotEmpty(cxPlanProductStatusList)){
            cacheService.updatePlanProductStatusList(newTaskList,cxPlanProductStatusList);
        }
        //Joran 2021-12-31 根据前一天计划列表进行投产状态表更新end

        //根据月度计划APS版本进行SAP+胎胚代码+库存地点计划量合并
        List<MdmMonthProdPlan> mdmMonthProdPlanList=this.mdmMonthProdPlanService.selectMonthTotalPlanQtyByApsVersion(monthPlanApsVersion);

        if(StringUtils.isEmpty(cxPlanProductStatusList)){
            logDetail.append("【cxAutoScheduleEngine.autoSchedule】自动排程获取未投产列表数据为空").append(division);
        }

        if(StringUtils.isEmpty(mdmMonthProdPlanList)){
            logDetail.append("【cxAutoScheduleEngine.autoSchedule】自动排程获取月度计划明细汇总列表数据为空").append(division);
        }
        //任务安排start
        cacheService.defaultToProduct(newTaskList);
        //1.现有根据成型机机台进行任务列表拆分
        Map<String,List<CxEngineScheduleResult>> machineTaskMap= CxScheduleUtils.splitTaskByCxMachine(newTaskList);
        //Joran 2022-01-07 根据胎胚进行类型汇总,同胎胚库存不重复进行累加
        Map<String,Double> sameDimensionAvailableClassOneShiftMap=new HashMap<>(); //处理同寸口平均可硫化班次
        Map<String,Integer> embryoCodeTypeTotalMap=cacheService.generateEmbryoTypeMap(newTaskList,sameDimensionAvailableClassOneShiftMap, BigDecimal.ZERO.intValue(),logDetail);
        try{
            //Joran 2022-03-15 新增自动排程相关日志
            autoScheduleLogService.insertCxScheduleLog("", "", title, logDetail.toString());
            cxAutoScheduleEngineService.autoSchedule(scheduleDate,cxBatchNo,machineTaskMap,cxPlanProductStatusList,mdmMonthProdPlanList,embryoCodeTypeTotalMap,sameDimensionAvailableClassOneShiftMap,sapTireConstructionListMap);
            cxEngineAutoScheduleRecordService.generagAutoScheduleRecord(scheduleDate,monthPlanApsVersion,cxBatchNo, CxEngineConstants.AUTO_SCHEDULE_STATUS_SUCCESS,"自动排程成功");
            cxScheduleEngineMapper.updateCxScheduleResultBatchNoByScheduleDate(scheduleDateStr,cxBatchNo);
        }catch (Exception e){
            log.error("【自动排程异常】："+e.getMessage());
            e.printStackTrace();
            cxEngineAutoScheduleRecordService.generagAutoScheduleRecord(scheduleDate,monthPlanApsVersion,cxBatchNo, CxEngineConstants.AUTO_SCHEDULE_STATUS_FAILE,"自动排程异常");
            throw e;
        }finally {
            //Joran 2022-05-18 进行机台时间计算对象缓存释放
            cacheService.clearCacheData();
        }
        //Joran 2022-02-23 开始对三班及次一 次二班的计划进行自动安排调整
    }

    /**
     * 获取增补计划（前一日排程全部计划）
     * @param cxMachineCode
     * @param scheduleDate
     * @param cxBatchNo
     * @param logDetail
     * @return
     */
    private List<CxEngineScheduleResult> getCxEngineLastDaySupplePlans(String cxMachineCode, Date scheduleDate, String cxBatchNo, StringBuilder logDetail) {
        Date lastDate=DateUtils.addDays(scheduleDate,-1);
        String lastDateStr= DateUtils.parseDateToStr("yyyyMMdd",lastDate);
        logDetail.append("获取【").append(lastDateStr).append("】").append("调整后计划").append(division);

        CxEngineLastDaySupplePlan condition= new CxEngineLastDaySupplePlan();
        condition.setSuppleDateStr(lastDateStr);
        if (StringUtils.isNotEmpty(cxMachineCode)){
            condition.setCxMachineCode(cxMachineCode);
        }
        List<CxEngineLastDaySupplePlan> cxEngineLastDaySupplePlanList =this.cxEngineLastDaySupplePlanService.selectSupplePlanListByCondition(condition);
        if(StringUtils.isEmpty(cxEngineLastDaySupplePlanList)){
            log.debug("未加载到前一天调整确认后排程结果数据,{}",DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS, lastDate));
            String errorMsg=I18nUtil.getMessage("cx.engine.auto.lastDay.schedule.empty.error");
            cxEngineAutoScheduleRecordService.generagAutoScheduleRecord(scheduleDate,null, cxBatchNo, CxEngineConstants.AUTO_SCHEDULE_STATUS_FAILE,errorMsg);
            throw new CxScheduleEngineException(errorMsg);
        }

        //转换为前一日排程任务
        return BeanConverUtil.converList(cxEngineLastDaySupplePlanList,CxEngineScheduleResult.class);
    }

    /**
     * 把排程数据同步到log表
     *
     * @param scheduleDate 排程日期，格式：yyyyMMdd
     */
    public void syncCxScheduleToLog(String scheduleDate,String cxMachineCode,String sourceCxOrder) {
        cxScheduleEngineMapper.syncCxScheduleToLog(scheduleDate,cxMachineCode);
        cxScheduleEngineMapper.deleteCxSchedule(scheduleDate,cxMachineCode);
        //Joran 2022-03-31 逻辑删除自动停排数据
        cxScheduleEngineMapper.deleteScheduleStopInfoByScheduleDate(scheduleDate,cxMachineCode);

        //Joran 2021-09-07 删除模具变动单临时表数据
        cxLhEngineCommonMapper.syncMoldChagePlanToLog(scheduleDate,sourceCxOrder);
        cxLhEngineCommonMapper.deleteLhEngineMoldChangePlanByScheduleDate(scheduleDate,sourceCxOrder);
    }


}
