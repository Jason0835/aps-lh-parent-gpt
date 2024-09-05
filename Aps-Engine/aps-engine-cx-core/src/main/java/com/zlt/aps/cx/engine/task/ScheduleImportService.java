package com.zlt.aps.cx.engine.task;

import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.utils.SecurityUtils;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.engine.common.CxEngineQuotaCommonService;
import com.zlt.aps.common.engine.domain.EngineProductConstructionInfo;
import com.zlt.aps.common.engine.domain.LhEngineTireConstructionInfo;
import com.zlt.aps.common.engine.domain.MdmMonthPlanMain;
import com.zlt.aps.common.engine.service.AutoScheduleLogService;
import com.zlt.aps.common.engine.service.LhEngineTireConstructionInfoService;
import com.zlt.aps.common.engine.utils.BeanConverUtil;
import com.zlt.aps.common.engine.utils.GenerageMapKeyUtils;
import com.zlt.aps.cx.api.domain.entity.CxMachineInfo;
import com.zlt.aps.cx.api.domain.entity.CxScheduleResult;
import com.zlt.aps.cx.api.domain.entity.CxScheduleTaskTime;
import com.zlt.aps.cx.engine.common.CommonCacheService;
import com.zlt.aps.cx.engine.constants.CxEngineConstants;
import com.zlt.aps.cx.engine.constants.CxPrefixConstants;
import com.zlt.aps.cx.engine.domain.CxEngineEmbryoMonthPlanSurplus;
import com.zlt.aps.cx.engine.domain.CxEngineScheduleLimit;
import com.zlt.aps.cx.engine.domain.CxEngineScheduleResult;
import com.zlt.aps.cx.engine.domain.CxPlanProductStatus;
import com.zlt.aps.cx.engine.exception.CxScheduleEngineException;
import com.zlt.aps.cx.engine.mapper.CxScheduleEngineMapper;
import com.zlt.aps.cx.engine.service.CxEngineAutoScheduleRecordService;
import com.zlt.aps.cx.engine.service.CxEngineEmbryoMonthPlanSurplusService;
import com.zlt.aps.cx.engine.service.CxEngineScheduleLimitService;
import com.zlt.aps.cx.engine.service.CxPlanProductStatusService;
import com.zlt.aps.cx.engine.service.CxScheduleTaskTimeService;
import com.zlt.aps.cx.engine.utils.CxScheduleUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * 排程导入数据自动计算部分数据填充
 */
@Component("scheduleImportService")
@Slf4j
public class ScheduleImportService {

    @Autowired
    private CommonCacheService commonCacheService;

    @Autowired
    private CxEngineAutoScheduleRecordService cxEngineAutoScheduleRecordService;
    @Autowired
    private CxScheduleEngineMapper cxScheduleEngineMapper;

    @Autowired
    private CxEngineEmbryoMonthPlanSurplusService cxEngineEmbryoMonthPlanSurplusService;

    @Autowired
    private CxEngineQuotaCommonService cxEngineQuotaCommonService;

    @Autowired
    private ScheduleCheckService scheduleCheckService;

    @Autowired
    private AutoScheduleLogService autoScheduleLogService;

    @Autowired
    private CxEngineScheduleLimitService cxEngineScheduleLimitService;

    @Autowired
    private CxPlanProductStatusService cxPlanProductStatusService;

    @Autowired
    private LhEngineTireConstructionInfoService lhEngineTireConstructionInfoService;

    @Autowired
    private CxScheduleTaskTimeService cxScheduleTaskTimeService;

    /**
     * 全部施工信息
     */
    private Map<String, EngineProductConstructionInfo> engineConstructionInfoMap;

    /**
     * 全部成型机台信息
     */
    private Map<String, CxMachineInfo> cxMachineInfoMap;

    //获取成型排产限制设置列表
    private Map<String, List<CxEngineScheduleLimit>> scheduleLimitMap;

    private String division = "\r\n---------------------------------------------------\r\n";  //日志分割符

    /**
     * 批量导入成型排程数据自动填充
     * @param cxScheduleResultList 导入日期
     * @param scheduleDate 排程日期
     */
    @Transactional
    public List<ImportErrorLog> batchImportSchedule(List<CxScheduleResult> cxScheduleResultList, Date scheduleDate, Long importLogId)throws CxScheduleEngineException {
        List<ImportErrorLog> errorLogList=new ArrayList<>();
        StringBuilder errorMsg=new StringBuilder();
        //验证数据集合是否为空
        if(StringUtils.isEmpty(cxScheduleResultList)){
            throw new CxScheduleEngineException(I18nUtil.getMessage("cx.engine.import.list.empty.error"));
        }
        //验证排程日期
        if(scheduleDate==null){
            throw new CxScheduleEngineException(I18nUtil.getMessage("cx.engine.import.scheduleDate.empty.error"));
        }
        //加载成型排程限制
        scheduleLimitMap=cxEngineScheduleLimitService.getCxScheduleLimitMachineCodeMap();
        String scheduleDateStr= DateUtils.parseDateToStr("yyyyMMdd",scheduleDate);
        List<CxEngineScheduleResult> cxEngineScheduleResultList= BeanConverUtil.converList(cxScheduleResultList,CxEngineScheduleResult.class);
        //删除成型自动排程记录表
        cxEngineAutoScheduleRecordService.deleteAutoScheduleRecordByScheduleDate(scheduleDateStr);
        //删除成型排程结果表数据
        commonCacheService.syncCxScheduleToLog(scheduleDateStr,"","");
        //获取所有成型机台信息
        cxMachineInfoMap =cxEngineQuotaCommonService.getCxMachineInfoFromRedis();
        if(StringUtils.isEmpty(cxMachineInfoMap)){
            throw new CxScheduleEngineException(I18nUtil.getMessage("cx.engine.import.machine.error"));
        }
        //成型自动排程批次号
        String cxBatchNo=commonCacheService.getCxSequence(CxPrefixConstants.SCHEDULE_BATCH_NO_PREFIX+scheduleDateStr,CxPrefixConstants.CX_BATCH_NO_PREFIX+scheduleDateStr);
        //获取生产排程版本
        MdmMonthPlanMain planVersion=scheduleCheckService.validateMdmMonthPlanMain(scheduleDate);
        String monthPlanApsVersion=planVersion.getMonthPlanApsVersion();
        //根据生产排程版本信息获取对应的月度胎胚汇总信息
        Map<String, CxEngineEmbryoMonthPlanSurplus> cxEngineEmbryoMonthPlanSurplusMap= cxEngineEmbryoMonthPlanSurplusService.listCxEmbryoMonthPlanSurplusByMonthPlanApsVersion(monthPlanApsVersion);
        if(StringUtils.isEmpty(cxEngineEmbryoMonthPlanSurplusMap)){
            throw new CxScheduleEngineException(I18nUtil.getMessage("cx.engine.import.embryo.sulPlus.error"));
        }

        ImportErrorLog errorLog=null;
        //设置批次号、工单号
        //获取收尾提示量
        Map<String,String> cxParams=commonCacheService.loadCxParamsMap();
        Integer closeOutNumber=commonCacheService.getCloseOutTipSetting(cxParams);
        StringBuilder monthRemainQtyLog=new StringBuilder();
        List<CxPlanProductStatus> updateProductStatusList=new ArrayList<>(cxEngineScheduleResultList.size());
        CxPlanProductStatus updateProductStatus=null;
        //校验成功导入列表
        List<CxEngineScheduleResult> successList= new ArrayList<>(cxEngineScheduleResultList.size());

        //Joran 2021-12-04 进行施工版本信息从月度计划获取，规则：多个版本或者月度计划不存在的排程计划不进行版本填充
        // 施工版本调整为完全取自投产施工表，不再受月度计划影响，20220906 moidfy by hak
        scheduleCheckService.fillingBomDataVersion(scheduleDate,cxEngineScheduleResultList);
        // 加载全部胎胚的施工信息
        // 填充胎胚版本时，会重新刷新投产施工信息，因此取值要在施工版本填充完成之后执行，moidfy by 20220321 hak
        engineConstructionInfoMap=cxEngineQuotaCommonService.loadEngineConstructionMapFromRedis();
        if(StringUtils.isEmpty(engineConstructionInfoMap)){
           throw new CxScheduleEngineException(I18nUtil.getMessage("cx.engine.import.construction.error"));
        }
        //1.填充库存信息
        StringBuilder updateStockLog =new StringBuilder();
        commonCacheService.updateLastDayTaskStock(cxEngineScheduleResultList,scheduleDate,updateStockLog,false);
        if(StringUtils.isNotEmpty(updateStockLog)){
            autoScheduleLogService.insertCxScheduleLog(cxEngineScheduleResultList.get(0).getCxBatchNo(), "", "成型排程导入更新库存",updateStockLog.toString()); //添加日志
        }

        for(CxEngineScheduleResult cxEngineScheduleResult:cxEngineScheduleResultList){
            //格式化各个班次的计划领，若为空则补0
            cxEngineScheduleResult.initPlanQty();
            String embryoCode= cxEngineScheduleResult.getEmbryoCode();
            String bomDataVersion=cxEngineScheduleResult.getBomDataVersion();
            String key = GenerageMapKeyUtils.createMapKey(embryoCode,bomDataVersion);
            String machineCode=cxEngineScheduleResult.getCxMachineCode();
            if(engineConstructionInfoMap.containsKey(key)){
                EngineProductConstructionInfo engineConstructionInfo=engineConstructionInfoMap.get(key);
                cxEngineScheduleResult.setSpecDimension(engineConstructionInfo.getDimension());//设置寸口
                //cxEngineScheduleResult.setSpecDesc(engineConstructionInfo.getSpecDesc());//设置规格型号
            }
            //验证机台信息
            if(!cxMachineInfoMap.containsKey(machineCode)){
                errorMsg.append(StringUtils.format(I18nUtil.getMessage("cx.engine.import.machine.noFound.error"),machineCode));
                errorLog=new ImportErrorLog();
                errorLog.setImportLogId(importLogId);
                errorLog.setErrorDetail(StringUtils.format(I18nUtil.getMessage("cx.engine.import.errorLog.machine.noFound.error"),cxEngineScheduleResult.getSapCode(),cxEngineScheduleResult.getEmbryoCode(),cxEngineScheduleResult.getCxMachineCode()));
                errorLogList.add(errorLog);
                continue;
            }else{
                CxMachineInfo cxMachineInfo=cxMachineInfoMap.get(machineCode);
                cxEngineScheduleResult.setWorkShifts(StringUtils.isEmpty(cxMachineInfo.getClassShift())?3:Integer.valueOf(cxMachineInfo.getClassShift()));
                cxEngineScheduleResult.setCxMachineName(cxMachineInfo.getMachineName());//设置成型机台名称
                cxEngineScheduleResult.setCxMachineType(cxMachineInfo.getMachineType());//2021-12-15 设置成型机台类型
            }

            //Joran 2021-12-02 施工版本，需要从月度计划获取投产阶段的版本
            updateProductStatus=new CxPlanProductStatus();
            updateProductStatus.setMonthPlanApsVersion(monthPlanApsVersion);
            updateProductStatus.setSapCode(cxEngineScheduleResult.getSapCode());
            updateProductStatus.setUpdateBy(SecurityUtils.getUsername());
            updateProductStatus.setUpdateTime(DateUtils.getNowDate());
            updateProductStatus.setEmbryoCode(embryoCode);
            updateProductStatus.setBomDataVersion(bomDataVersion);
            monthRemainQtyLog.append("胎胚代码：").append(embryoCode).append(",机台编号：").append(machineCode).append(",获取到施工寸口:").append(cxEngineScheduleResult.getSpecDimension()).append(division);

            String  specDimension=cxEngineScheduleResult.getSpecDimension()==null?"":cxEngineScheduleResult.getSpecDimension().toString();
            //设置最大硫化班数
            commonCacheService.maxLhShiftCount(cxEngineScheduleResult,specDimension,0D,scheduleLimitMap);
            //预设置不提示，若符合条件则会重新赋值
            cxEngineScheduleResult.setProductionStatus(CxEngineConstants.PRODUCTION_STATUS_UNDO);
            cxEngineScheduleResult.setMarkCloseOutTip(CxEngineConstants.CLOSE_OUT_TIP_NO);
            cxEngineScheduleResult.setIsRelease(CxEngineConstants.IS_PUBLISH_NO);//未发布
            //Joran 2021-11-30设置成型排程结果表数据来源为导入
            cxEngineScheduleResult.setDataSource(CxEngineConstants.CX_SCHEDULE_DATA_SOURCE_IMPORT);
            //Joran 2021-11-19项目经理+测试提出硫化机信息为空的话则为待投产否则默认给硫化状态为投产中start
            if(StringUtils.isEmpty(cxEngineScheduleResult.getLhMachineCode())){
                cxEngineScheduleResult.setTaskType(CxEngineConstants.TASK_TYPE_TODO);
            }else{
                cxEngineScheduleResult.setTaskType(CxEngineConstants.TASK_TYPE_DOING);
            }
            //Joran 2021-11-19项目经理+测试提出硫化机信息为空的话则为待投产否则默认给硫化状态为投产中end
            cxEngineScheduleResult.setCxBatchNo(cxBatchNo);
            //生成工单号
            cxEngineScheduleResult.setOrderNo(commonCacheService.getCxSequence(CxPrefixConstants.SCHEDULE_ORDER_NO_PREFIX+scheduleDateStr, CxPrefixConstants.CX_ORDER_NO_PREFIX+scheduleDateStr));
            if(cxEngineEmbryoMonthPlanSurplusMap.containsKey(embryoCode)){
                CxEngineEmbryoMonthPlanSurplus cxEngineEmbryoMonthPlanSurplus=cxEngineEmbryoMonthPlanSurplusMap.get(embryoCode);
                cxEngineScheduleResult.setMonthRemainQty(cxEngineEmbryoMonthPlanSurplus.getMonthRemainQty());
                cxEngineScheduleResult.setMonthFinishQty(cxEngineEmbryoMonthPlanSurplus.getMonthFinishQty()==null?0:cxEngineEmbryoMonthPlanSurplus.getMonthFinishQty());//Joran 2021-07-20 冗余成型月度完成量
                cxEngineScheduleResult.setCxMonthFinishQty(cxEngineEmbryoMonthPlanSurplus.getMonthFinishQty()==null?0:cxEngineEmbryoMonthPlanSurplus.getMonthFinishQty());//Joran 2021-07-20 冗余成型月度完成量
                //Joran 2021-09-22 处理导入的时候有导入前一天三班计划量的时候start
                Integer class3PlannedQty=cxEngineScheduleResult.getClass3PlannedQty()==null?0:cxEngineScheduleResult.getClass3PlannedQty();
                Integer monthRemainQty=cxEngineEmbryoMonthPlanSurplus.getMonthRemainQty();
                monthRemainQty-=class3PlannedQty;
                monthRemainQtyLog.append("获取到的月度剩余量：").append(monthRemainQty).append(division);
                //Joran 2021-09-22 处理导入的时候有导入前一天三班计划量的时候end
                if(monthRemainQty<=0){//月度胎胚汇总表中存在且已收尾的记录
                    cxEngineScheduleResult.setProductionStatus(CxEngineConstants.PRODUCTION_STATUS_CLOSE_OUT);
                    monthRemainQtyLog.append("月度剩余量小于等于0：").append("状态变更为：已收尾").append(division);
                }else if(monthRemainQty<=closeOutNumber){//标记收尾提示
                    cxEngineScheduleResult.setMarkCloseOutTip(CxEngineConstants.CLOSE_OUT_TIP_YES);
                    monthRemainQtyLog.append("工序参数收尾提示量：").append(closeOutNumber).append(",标记收尾提醒！").append(division);
                }
            }else{
                //Joran 2021-09-23 没有匹配到胎胚月度汇总数据时，则进行默认收尾操作
                log.debug("【成型排程导入】胎胚代码："+embryoCode+",未找到对应的月度汇总数据，规格进行收尾标记");
                monthRemainQtyLog.append("【成型排程导入】胎胚代码：").append(embryoCode).append(",未找到对应的月度汇总数据，规格进行收尾标记！").append(division);
                cxEngineScheduleResult.setProductionStatus(CxEngineConstants.PRODUCTION_STATUS_CLOSE_OUT);
            }
            //计算单班硫化量
            commonCacheService.calcSingleShiftLhQty(cxEngineScheduleResult);

            //硫化外胎施工信息start
            LhEngineTireConstructionInfo condition=new LhEngineTireConstructionInfo();
            List<LhEngineTireConstructionInfo> constructionInfoList=lhEngineTireConstructionInfoService.selectLhTireConstructionInfoList(condition);
            Map<String,List<LhEngineTireConstructionInfo>> sapTireConstructionListMap=new HashMap<>();
            if(StringUtils.isNotEmpty(constructionInfoList)){
                sapTireConstructionListMap=constructionInfoList.stream().collect(Collectors.groupingBy(lhEngineScheduleResult -> lhEngineScheduleResult.getSapCode()));
            }
            //硫化外胎施工信息end

            //Joran 2022-01-03 设置外胎规格描述
            commonCacheService.setSpecDescBySapCode(cxEngineScheduleResult,sapTireConstructionListMap);
            //计算最小硫化机需求数
            if(cxEngineScheduleResult.calcMonthRemainQty()>0){
                commonCacheService.calcLeastLhMachineQtyByMonthRemainQty(cxEngineScheduleResult,cxEngineScheduleResult.calcMonthRemainQty(),sapTireConstructionListMap);
            }
            //计算各个班次的可硫化班次
            CxScheduleUtils.calcAllClassAvailableLhShift(cxEngineScheduleResult);
            updateProductStatusList.add(updateProductStatus);
            //保存成功列表数据
            successList.add(cxEngineScheduleResult);
        }

        //Joran 2021-09-23 添加月度汇总相关日志记录start
        if(StringUtils.isNotEmpty(monthRemainQtyLog)){
            autoScheduleLogService.insertCxScheduleLog(cxEngineScheduleResultList.get(0).getCxBatchNo(), "", "成型排程导入获取胎胚月度汇总数据",monthRemainQtyLog.toString()); //添加日志
        }
        //Joran 2021-09-23 添加月度汇总相关日志记录end

        //Joran 2021-10-12 批量更新导入数据的投产状态更新start
        if(StringUtils.isNotEmpty(updateProductStatusList)){
            cxPlanProductStatusService.updateProductStatusToWaitPublish(updateProductStatusList);
        }
        //Joran 2021-10-12 批量更新导入数据的投产状态更新end

        if(StringUtils.isNotEmpty(successList)){
            //Joran 2021-12-20 导入进行剩余成功列表标记自动排计划标识start
            List<CxEngineScheduleResult> insertList =new ArrayList<>(successList.size());
            //Joran 2022-01-06 导入进行导入的数据生产班次确认生产顺序start
            classSortSet(successList);
            //Joran 2022-01-06 导入进行导入的数据生产班次确认生产顺序end

            //Joran 2021-12-21 按机台进行拆分
            Map<String, List<CxEngineScheduleResult>> machineMapList= CxScheduleUtils.splitTaskByCxMachine(successList);
            //用来存储所有任务时间
            List<CxScheduleTaskTime> scheduleTaskTimeList =new ArrayList<>();
            for(Map.Entry<String, List<CxEngineScheduleResult>> entry:machineMapList.entrySet()){
                String cxMachineCode=entry.getKey();
                List<CxEngineScheduleResult> machineList= entry.getValue();
                if(StringUtils.isNotEmpty(machineList)){
                    //Joran 2022-02-18 同机台任务进行排序
                    CxScheduleUtils.scheduleTaskMachinePlanSort(machineList);
                    Map<String,CxEngineScheduleResult> markMap=new HashMap<>();
                    //Joran 2021-12-21 同机台根据胎胚代码进行分组
                    Map<String, List<CxEngineScheduleResult>> embryoCodeListMap=  CxScheduleUtils.splitTaskByEmbryoCode(machineList);

                    for(Map.Entry<String, List<CxEngineScheduleResult>> embryoCodeEntry:embryoCodeListMap.entrySet()){
                        String embryoCode=embryoCodeEntry.getKey();
                        List<CxEngineScheduleResult> embryoCodeList= embryoCodeEntry.getValue();
                        if(StringUtils.isNotEmpty(embryoCodeList)){
                            if(embryoCodeList.size()==1){
                                embryoCodeList.get(0).setToProduct(CxEngineConstants.TO_PRODUCT_YES);//标记为自动排产
                                insertList.add(embryoCodeList.get(0));
                            }else{
                                //Joran 2022-02-18 根据生产顺序进行排序
                                CxScheduleUtils.sortByPlanSort(embryoCodeList);
                                for (CxEngineScheduleResult cxEngineScheduleResult:embryoCodeList){
                                    if(cxEngineScheduleResult.getDayTotalPlanQty()>0){
                                        if(!markMap.containsKey(embryoCode)){
                                            markMap.put(cxEngineScheduleResult.getEmbryoCode(),cxEngineScheduleResult);
                                            cxEngineScheduleResult.setToProduct(CxEngineConstants.TO_PRODUCT_YES);//标记为自动排产
                                        }else{
                                            cxEngineScheduleResult.setToProduct(CxEngineConstants.TO_PRODUCT_NO);//标记为不自动排产
                                        }
                                    }else{
                                        cxEngineScheduleResult.setToProduct(CxEngineConstants.TO_PRODUCT_NO);//标记为不自动排产
                                    }
                                    insertList.add(cxEngineScheduleResult);
                                }
                            }
                        }

                    }
                    //Joran 2022-06-02 添加成型机导入任务时间计算,此处代码注释，等月度计划版本全部确认完后，再进行时间计算
                   /* if(log.isDebugEnabled()){
                        log.debug("计算时间的机台编号：》》》》》》》》》》》》》》》》》》"+cxMachineCode);
                    }*/
                    //commonCacheService.calcMachineTaskTime(cxMachineCode,machineList,scheduleTaskTimeList,CxEngineConstants.CX_SCHEDULE_DATA_SOURCE_IMPORT,engineConstructionInfoMap,cxParams);
                }
            }
            //Joran 2022-06-06 清空缓存数据
            //commonCacheService.clearCacheData();

            //Joran 2022-06-17 批量进行成型任务时间存储
            if(StringUtils.isNotEmpty(scheduleTaskTimeList)){
                cxScheduleTaskTimeService.batchInsertCxScheduleTaskTime(scheduleTaskTimeList);
            }

            //Joran 2021-12-20 导入进行剩余成功列表标记自动排计划标识end
            if(StringUtils.isNotEmpty(insertList)){

                //检查机台类型上是否存在不一致的胎胚 add by pancd+ 20230904
                checkMachineDiffEmbryoCode(insertList,cxMachineInfoMap);

                //Joran 2021-12-27 进行导入数据硫化状态栏位标记start
                commonCacheService.cxScheduleResultLhTaskTypeCloseOut(insertList,monthPlanApsVersion);
                //Joran 2021-12-27 进行导入数据硫化状态栏位标记end
                cxScheduleEngineMapper.batchInsertCxScheduleResult(insertList);
                cxEngineAutoScheduleRecordService.generagAutoScheduleRecord(scheduleDate,planVersion.getMonthPlanApsVersion(),cxBatchNo, CxEngineConstants.AUTO_SCHEDULE_STATUS_SUCCESS,"批量导入成功");
            }
        }else{
            cxEngineAutoScheduleRecordService.generagAutoScheduleRecord(scheduleDate,planVersion.getMonthPlanApsVersion(),cxBatchNo, CxEngineConstants.AUTO_SCHEDULE_STATUS_FAILE,"批量导入失败");
        }
        return errorLogList;

    }

    /**
     * 检查机台类型上是否存在不一致的胎胚
     * @param insertList
     */
    private void checkMachineDiffEmbryoCode( List<CxEngineScheduleResult> insertList,Map<String, CxMachineInfo> cxMachineInfoMap){
        if(StringUtils.isEmpty(insertList)){
            return;
        }
        Map<String,List<CxEngineScheduleResult>> machineTaskMap= CxScheduleUtils.splitTaskByCxMachine(insertList);
        CxMachineInfo cxMachineInfo;
        List<String> diffMachineList = new ArrayList<>();
        for(Map.Entry<String,List<CxEngineScheduleResult>> entry:machineTaskMap.entrySet()){
            String cxMachineCode=entry.getKey();
            List<CxEngineScheduleResult> machineList=entry.getValue();
            String machineType = machineList.get(0).getCxMachineType();
            for (CxEngineScheduleResult cxScheduleResult:machineList){
                if(CxEngineConstants.MACHINE_TYPE_ONCE.equals(machineType)){
                    //一次法机台，出现二次法胎胚，列入差异机台列表
                    if (cxScheduleResult.getEmbryoCode().startsWith("E")) {
                        cxMachineInfo = cxMachineInfoMap.get(cxMachineCode);
                        diffMachineList.add(cxMachineInfo == null ? "":cxMachineInfo.getMachineName());
                        break;
                    }
                }else{
                    //二次法机台，出现一次法胎胚，列入差异机台列表
                    if (cxScheduleResult.getEmbryoCode().startsWith("Y")) {
                        cxMachineInfo = cxMachineInfoMap.get(cxMachineCode);
                        diffMachineList.add(cxMachineInfo == null ? "":cxMachineInfo.getMachineName());
                        break;
                    }
                }
            }
        }

        if(StringUtils.isNotEmpty(diffMachineList)){
            diffMachineList = diffMachineList.stream().distinct().collect(Collectors.toList());
            String machineStr = diffMachineList.stream().collect(Collectors.joining(","));
            String errorStr = String.format(I18nUtil.getMessage("cx.engine.import.embryo.diff.error"),machineStr);
            throw new CxScheduleEngineException(errorStr);
        }
    }
    /**
     * 进行各个班次设定
     * @param insertList
     */
    private void classSortSet(List<CxEngineScheduleResult> insertList) {
        Map<String,List<CxEngineScheduleResult>> machineTaskMap=CxScheduleUtils.splitTaskByCxMachine(insertList);
        for(Map.Entry<String,List<CxEngineScheduleResult>> entry:machineTaskMap.entrySet()){
            List<CxEngineScheduleResult> class1List=new ArrayList<>();
            List<CxEngineScheduleResult> class2List=new ArrayList<>();
            List<CxEngineScheduleResult> class3List=new ArrayList<>();
            List<CxEngineScheduleResult> class4List=new ArrayList<>();
            List<CxEngineScheduleResult> class5List=new ArrayList<>();
            List<CxEngineScheduleResult> machineList=entry.getValue();
            //先按各个班次的计划量进行分组,经过分组后每个集合里面只会存储班次有计划量的对象 start
            for(CxEngineScheduleResult cxScheduleResult:machineList){
                if(cxScheduleResult.getClass1PlanQty()>0){
                    class1List.add(cxScheduleResult);
                    continue;
                }
                if(cxScheduleResult.getClass2PlanQty()>0){
                    class2List.add(cxScheduleResult);
                    continue;
                }
                if(cxScheduleResult.getClass3PlanQty()>0){
                    class3List.add(cxScheduleResult);
                    continue;
                }
                if(cxScheduleResult.getClass4PlanQty()>0){
                    class4List.add(cxScheduleResult);
                    continue;
                }
                if(cxScheduleResult.getClass5PlanQty()>0){
                    class5List.add(cxScheduleResult);
                    continue;
                }
            }
            //先按各个班次的计划量进行分组,经过分组后每个集合里面只会存储班次有计划量的对象 end
            int class2Sort=0;
            int class3Sort=0;
            int class4Sort=0;
            int class5Sort=0;
            if(StringUtils.isNotEmpty(class1List)){
                insertList.removeAll(class1List);
                int max=class1List.size();
                int step=0;
                for(CxEngineScheduleResult cxEngineScheduleResult1:class1List){
                    if(cxEngineScheduleResult1.getClass2PlanQty()>0){
                        cxEngineScheduleResult1.setClass1Sort(max);
                        cxEngineScheduleResult1.setClass2Sort(class2Sort+1);
                    }else{
                        cxEngineScheduleResult1.setClass1Sort(step+1);
                    }
                    if(cxEngineScheduleResult1.getClass3PlanQty()>0){
                        cxEngineScheduleResult1.setClass3Sort(class3Sort+1);
                    }

                    if(cxEngineScheduleResult1.getClass4PlanQty()>0){
                        cxEngineScheduleResult1.setClass4Sort(class4Sort+1);
                    }

                    if(cxEngineScheduleResult1.getClass5PlanQty()>0){
                        cxEngineScheduleResult1.setClass5Sort(class5Sort+1);
                    }

                }
                insertList.addAll(class1List);
            }

            if(StringUtils.isNotEmpty(class2List)){
                insertList.removeAll(class2List);
                for(CxEngineScheduleResult cxEngineScheduleResult2:class2List){
                    if(cxEngineScheduleResult2.getClass3PlanQty()>0){
                        cxEngineScheduleResult2.setClass2Sort(class2Sort+1);
                        cxEngineScheduleResult2.setClass3Sort(class3Sort+1);
                    }else{
                        cxEngineScheduleResult2.setClass2Sort(class2Sort+1);
                    }

                    if(cxEngineScheduleResult2.getClass4PlanQty()>0){
                        cxEngineScheduleResult2.setClass4Sort(class4Sort+1);
                    }

                    if(cxEngineScheduleResult2.getClass5PlanQty()>0){
                        cxEngineScheduleResult2.setClass5Sort(class5Sort+1);
                    }
                }
                insertList.addAll(class2List);
            }

            if(StringUtils.isNotEmpty(class3List)){
                insertList.removeAll(class3List);
                for(CxEngineScheduleResult cxEngineScheduleResult3:class3List){
                    if(cxEngineScheduleResult3.getClass4PlanQty()>0){
                        cxEngineScheduleResult3.setClass3Sort(class3Sort+1);
                        cxEngineScheduleResult3.setClass4Sort(class4Sort+1);
                    }else{
                        cxEngineScheduleResult3.setClass3Sort(class3Sort+1);
                    }
                    if(cxEngineScheduleResult3.getClass5PlanQty()>0){
                        cxEngineScheduleResult3.setClass5Sort(class5Sort+1);
                    }
                }
                insertList.addAll(class3List);
            }

            if(StringUtils.isNotEmpty(class4List)){
                insertList.removeAll(class4List);
                for(CxEngineScheduleResult cxEngineScheduleResult4:class4List){
                    if(cxEngineScheduleResult4.getClass5PlanQty()>0){
                        cxEngineScheduleResult4.setClass4Sort(class4Sort+1);
                        cxEngineScheduleResult4.setClass5Sort(class5Sort+1);
                    }else{
                        cxEngineScheduleResult4.setClass4Sort(class4Sort+1);
                    }
                }
                insertList.addAll(class4List);
            }

            if(StringUtils.isNotEmpty(class5List)){
                insertList.removeAll(class5List);
                for(CxEngineScheduleResult cxEngineScheduleResult5:class5List){
                    cxEngineScheduleResult5.setClass5Sort(class5Sort+1);
                }
                insertList.addAll(class5List);
            }
        }

    }

}
