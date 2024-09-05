package com.zlt.aps.cx.engine.task;

import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.utils.SecurityUtils;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.engine.domain.EngineProductConstructionInfo;
import com.zlt.aps.common.engine.domain.MdmMonthPlanMain;
import com.zlt.aps.common.engine.domain.TCxEmbryoMonthPlanSurplus;
import com.zlt.aps.common.engine.planmain.MdmMonthPlanAmountSumService;
import com.zlt.aps.common.engine.result.ValidateResult;
import com.zlt.aps.common.engine.service.AutoScheduleLogService;
import com.zlt.aps.common.engine.utils.BeanConverUtil;
import com.zlt.aps.cx.api.domain.entity.CxMachineInfo;
import com.zlt.aps.cx.api.domain.entity.CxScheduleResult;
import com.zlt.aps.cx.engine.constants.CxEngineConstants;
import com.zlt.aps.cx.engine.constants.CxPrefixConstants;
import com.zlt.aps.cx.engine.domain.CxEngineAutoScheduleRecord;
import com.zlt.aps.cx.engine.domain.CxEngineEmbryoMonthPlanSurplus;
import com.zlt.aps.cx.engine.domain.CxEngineMonthPlanSurplus;
import com.zlt.aps.cx.engine.domain.CxEngineScheduleResult;
import com.zlt.aps.cx.engine.exception.CxScheduleEngineException;
import com.zlt.aps.cx.engine.mapper.CxScheduleEngineMapper;
import com.zlt.aps.cx.engine.service.CxEngineSpecifyMachineService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.List;

import static com.alibaba.fastjson.JSON.toJSONString;

/**
 * 成型工序排程插单引擎
 */
@Component("insertTaskService")
@Slf4j
public class InsertTaskService {

    @Autowired
    private CxScheduleEngineMapper cxScheduleEngineMapper;

    @Autowired
    private ScheduleCheckService scheduleCheckService;

    @Autowired
    private CxEngineSpecifyMachineService cxEngineSpecifyMachineService;

    @Resource
    private AutoScheduleLogService autoScheduleLogService;

    @Autowired
    private MdmMonthPlanAmountSumService mdmMonthPlanAmountSumService;

    private String division = "\r\n---------------------------------------------------\r\n";  //日志分割符

    /**
     * 插单参数验证
     * @param scheduleResult
     * @return
     */
    public ValidateResult preCheck(CxScheduleResult scheduleResult){
        scheduleCheckService.initBaseData();//每一次调用检查都用初始化
        StringBuilder errorMsg=new StringBuilder();
        String embryoCode=scheduleResult.getEmbryoCode();//胎胚代码
        String machineCode=scheduleResult.getCxMachineCode();//成型机台编号
        String bomDataVersion=scheduleResult.getBomDataVersion();//施工版本信息
        if(scheduleResult.getScheduleDate()==null){
            errorMsg.append(I18nUtil.getMessage("cx.engine.insert.scheduleDate.empty.error")) ;
        }
        if(StringUtils.isEmpty(scheduleResult.getTaskType())){
            errorMsg.append(I18nUtil.getMessage("cx.engine.insert.taskType.empty.error")) ;
        }
        if(StringUtils.isEmpty(scheduleResult.getSapCode())){
            errorMsg.append(I18nUtil.getMessage("cx.engine.insert.sapCode.empty.error")) ;
        }
        if(StringUtils.isEmpty(embryoCode)){
            errorMsg.append(I18nUtil.getMessage("cx.engine.insert.embryoCode.empty.error")) ;
        }

        if(StringUtils.isEmpty(bomDataVersion)){
            errorMsg.append(I18nUtil.getMessage("cx.engine.insert.bomDataVersion.empty.error")) ;
        }

        if(StringUtils.isEmpty(scheduleResult.getStorageLocation())){
            errorMsg.append(I18nUtil.getMessage("cx.engine.insert.storageLocation.empty.error")) ;
        }
        if(StringUtils.isEmpty(machineCode)){
            errorMsg.append(I18nUtil.getMessage("cx.engine.insert.machineCode.empty.error")) ;
        }
        //验证成型机
        CxMachineInfo cxMachineInfo=scheduleCheckService.validateCxMachine(machineCode,errorMsg);
        //获取到施工信息
        EngineProductConstructionInfo engineConstructionInfo=scheduleCheckService.validateConstructionInfo(embryoCode,bomDataVersion,errorMsg);

        //获取插单的计划总量
        int totalPlanQty=insertTotalPlanQty(scheduleResult);

        if(totalPlanQty<=0){
            errorMsg.append(I18nUtil.getMessage("cx.engine.insert.planQty.limit.error"));
        }
        //主线业务校验
        if(StringUtils.isNotEmpty(errorMsg)){
            return ValidateResult.error(errorMsg.toString());
        }
        //验证施工信息寸口
        if(engineConstructionInfo.getDimension()==null){
            errorMsg.append(I18nUtil.getMessage("cx.engine.construction.dimension.empty.error")) ;
        }
        //验证主计划版本主表信息
        MdmMonthPlanMain mdmMonthPlanMain=scheduleCheckService.validateMdmMonthPlanMain(scheduleResult.getScheduleDate());
        //Joran 2021-12-15 验证月度计划明细中施工版本是否完全
        scheduleCheckService.validateProdPlanBomDataVersion(mdmMonthPlanMain.getMonthPlanApsVersion());
        StringBuilder tipMsg= new StringBuilder("");
        scheduleCheckService.validateAutoScheduleRecord(scheduleResult.getScheduleDate(),tipMsg);
        //验证如果来源是主计划的存在，则证明是待投产插单，月度外胎汇总表相关不需要进行操作（20210728 调整为存在则不允许进行插单，只能进行投产）
        scheduleCheckService.listCxEngineMonthPlanSurplus(mdmMonthPlanMain.getMonthPlanApsVersion(),scheduleResult.getSapCode());
        //验证如果来源是主计划的存在，则证明是待投产插单，月度胎胚汇总表相关不需要进行操作
        List<CxEngineEmbryoMonthPlanSurplus> existEmbryoList=scheduleCheckService.listCxEngineEmbryoMonthPlanSurplus(mdmMonthPlanMain.getMonthPlanApsVersion(),scheduleResult.getEmbryoCode());
        //胎胚月度汇总表数据月度剩余量校验
        CxEngineEmbryoMonthPlanSurplus mpsMonthEmbryoPlanSurPlus=null;
        if(StringUtils.isNotEmpty(existEmbryoList)){
            //遍历查找如果找到了则验证是否来源插单，如果来源插单的话数据累加，如果是主计划则不处理
            for(CxEngineEmbryoMonthPlanSurplus cxEngineEmbryoMonthPlanSurplus:existEmbryoList){
                String dataSource=cxEngineEmbryoMonthPlanSurplus.getDataSource();
                //数据来源为主计划
                if(CxEngineConstants.CX_MONTH_PLAN_SURPLUS_DATA_SOURCE_MPS.equals(dataSource)){
                   //如果胎胚汇总且数据来源于主计划的不允许插单，只能进行投产
                  errorMsg.append(I18nUtil.getMessage("cx.engine.insert.use.insert.error"));//月度下发计划中存在规格不允许插单，请进行投产！
                }else if(CxEngineConstants.CX_MONTH_PLAN_SURPLUS_DATA_SOURCE_INSERT.equals(dataSource)){
                    mpsMonthEmbryoPlanSurPlus=cxEngineEmbryoMonthPlanSurplus;
                    break;//跳出循环
                }
            }
        }
        //主线业务校验
        if(StringUtils.isNotEmpty(errorMsg)){
            return ValidateResult.error(errorMsg.toString());
        }
        //验证月度剩余量
        if(mpsMonthEmbryoPlanSurPlus!=null){
            scheduleCheckService.checkMonthRemainQty(mpsMonthEmbryoPlanSurPlus,totalPlanQty,scheduleResult.getSapCode(),scheduleResult.getEmbryoCode(),scheduleResult.getScheduleDate(),tipMsg);
        }
        //寸口验证提示语
        scheduleCheckService.checkDimension(cxMachineInfo,engineConstructionInfo,tipMsg);
        //成型机定点相关信息验证
        this.cxEngineSpecifyMachineService.validateSpecifyMachine(scheduleResult.getSapCode(),scheduleResult.getEmbryoCode(),machineCode,tipMsg);
        //验证排程结果是否存在
        //scheduleCheckService.validateScheduleResult(scheduleResult.getScheduleDate(),scheduleResult.getSapCode(),scheduleResult.getEmbryoCode(),machineCode,tipMsg);
        //提示各个有计划的班次可安排最大的计划量
        scheduleCheckService.validateClassShiftPlanQty(scheduleResult,tipMsg);
        //Joran 2021-11-05 验证通过标记到redis
        scheduleCheckService.validateRedisMark(scheduleCheckService.createKey(SecurityUtils.getUsername(), CxPrefixConstants.CX_INSERT_VALIDATE_PREFIX));
        if(StringUtils.isNotEmpty(tipMsg)){
            return ValidateResult.success(tipMsg.toString());
        }
        return ValidateResult.success();
    }

    /**
     * 成型工序进行插单操作
     * @param cxScheduleResult
     */
    @Transactional
    public void insertScheduleOrder(CxScheduleResult cxScheduleResult) throws CxScheduleEngineException {
        //Joran 2021-11-04验证是否通过没通过是没有设置键
        String key= scheduleCheckService.createKey(SecurityUtils.getUsername(),CxPrefixConstants.CX_INSERT_VALIDATE_PREFIX);
        if(!scheduleCheckService.isValidate(key)){
            throw new CxScheduleEngineException(I18nUtil.getMessage("cx.engine.insert.no.check.error"));
        }
        scheduleCheckService.initBaseData();//每一次调用检查都用初始化
        CxEngineScheduleResult scheduleResult=new CxEngineScheduleResult();
        //属性复制
        BeanUtils.copyProperties(cxScheduleResult,scheduleResult);
        //复制对象
        formatterPlanQty(scheduleResult);
        //获取排程抓取记录取得工单号和生产排程计划版本号
        CxEngineAutoScheduleRecord autoScheduleRecord=scheduleCheckService.validateAutoScheduleRecord(scheduleResult.getScheduleDate(),new StringBuilder());
        if(autoScheduleRecord==null){
            //创建排程记录
            autoScheduleRecord = scheduleCheckService.createAutoScheduleRecord(cxScheduleResult.getScheduleDate());
        }
        String monthPlanApsVersion=autoScheduleRecord==null?"":autoScheduleRecord.getMonthPlanApsVersion();
        String cxBatchNo=autoScheduleRecord==null?"":autoScheduleRecord.getCxBatchNo();
        StringBuilder errorMsg=new StringBuilder();
        //获取到施工信息
        EngineProductConstructionInfo engineConstructionInfo=scheduleCheckService.validateConstructionInfo(cxScheduleResult.getEmbryoCode(),cxScheduleResult.getBomDataVersion(),errorMsg);
        //验证成型机
        CxMachineInfo cxMachineInfo=scheduleCheckService.validateCxMachine(cxScheduleResult.getCxMachineCode(),errorMsg);
        if(StringUtils.isNotEmpty(errorMsg)){
            throw new CxScheduleEngineException(errorMsg.toString());
        }
        scheduleResult.setCxMachineType(cxMachineInfo.getMachineType());//2021-12-15设置成型机台类别
        scheduleResult.setCxMachineName(cxMachineInfo.getMachineName());//设置成型机台名称
        scheduleResult.setWorkShifts(StringUtils.isEmpty(cxMachineInfo.getClassShift())?3:Integer.valueOf(cxMachineInfo.getClassShift()));
        scheduleResult.setMonthStock(0);//月结库存
        scheduleResult.setNewSpecFlag(true);//Joran 2022-03-03 标记插单都是新投产的规格
        scheduleResult.setToProduct(CxEngineConstants.TO_PRODUCT_YES);//Joran 2022-04-11 插单规格默认标记为要投产的规格
        scheduleResult.setDataSource(CxEngineConstants.CX_SCHEDULE_DATA_SOURCE_INSERT);//数据来源：插单
        //数据填充库存/工单号
        scheduleCheckService.dataFilling(scheduleResult,engineConstructionInfo);
        /**
         * Joran 2021-12-04 多版本改造成型插单不再往月度汇总表写入数据start
         */
         boolean singleDataVersion= false;
         if(singleDataVersion){
             //生成成型工序外胎汇总表数据
             validateInsertCxMonthPlanSurPlus(scheduleResult,monthPlanApsVersion);
             //生成成型工序外胎汇总表数据
             CxEngineEmbryoMonthPlanSurplus cxEngineEmbryoMonthPlanSurplus=validateInsertCxEngineEmbryoMonthPlanSurPlus(scheduleResult,monthPlanApsVersion);
             if(cxEngineEmbryoMonthPlanSurplus!=null){
                 //生成各个工序月度计划汇总表数据以及重算
                 TCxEmbryoMonthPlanSurplus updateEmbryoMonthPlanSurplus= BeanConverUtil.conver(cxEngineEmbryoMonthPlanSurplus,TCxEmbryoMonthPlanSurplus.class);
                 updateEmbryoMonthPlanSurplus.setMonthPlanQty(BigDecimal.valueOf(cxEngineEmbryoMonthPlanSurplus.getMonthPlanQty()));//计划量
                 updateEmbryoMonthPlanSurplus.setMonthPlanModifyQty(BigDecimal.valueOf(scheduleResult.getMonthRemainQty()));//计划调整量(2021-09-28 调整为给月度汇总是当前插单的总量，半部件重算进行累加)
                 updateEmbryoMonthPlanSurplus.setMonthFinishQty(BigDecimal.valueOf(cxEngineEmbryoMonthPlanSurplus.getMonthFinishQty()));//完成量
                 updateEmbryoMonthPlanSurplus.setMonthRemainQty(BigDecimal.valueOf(cxEngineEmbryoMonthPlanSurplus.getMonthRemainQty()));//月度剩余量
                 updateEmbryoMonthPlanSurplus.setEmbryoBadQty(BigDecimal.valueOf(cxEngineEmbryoMonthPlanSurplus.getEmbryoBadQty()));//不良数量
                 updateEmbryoMonthPlanSurplus.setLastMonthStock(BigDecimal.valueOf(cxEngineEmbryoMonthPlanSurplus.getLastMonthStock()));//月结库存
                 updateEmbryoMonthPlanSurplus.setMaterialCode(cxEngineEmbryoMonthPlanSurplus.getEmbryoCode());//胎胚代码
                 updateEmbryoMonthPlanSurplus.setDataSource(Integer.valueOf(cxEngineEmbryoMonthPlanSurplus.getDataSource()));//数据来源
                 try {
                     mdmMonthPlanAmountSumService.recalculateHalfPartByInsertEmbryo(autoScheduleRecord.getMonthPlanApsVersion(),updateEmbryoMonthPlanSurplus);
                 } catch (Exception e) {
                     e.printStackTrace();
                     throw new CxScheduleEngineException(I18nUtil.getMessage("cx.engine.insert.calcPlanSum.error"));
                 }
             }
         }
        /**
         * Joran 2021-12-04 多版本改造成型插单不再往月度汇总表写入数据end
         */

        StringBuilder logDetail= new StringBuilder();
        logDetail.append("操作人员：").append(SecurityUtils.getUsername()).append(division);
        logDetail.append("操作时间：").append(DateUtils.getTime()).append(division);
        CxEngineScheduleResult existResult=scheduleCheckService.validateScheduleResult(scheduleResult.getScheduleDate(),scheduleResult.getSapCode(),scheduleResult.getEmbryoCode(),scheduleResult.getCxMachineCode(),scheduleResult.getBomDataVersion(),new StringBuilder());
        //插入或者更新成型排程结果表
        if(existResult!=null){//更新累加各个班次计划量
            scheduleResult.setId(existResult.getId());
            //插入插单更新日志
            logDetail.append("排程结果数据：").append(toJSONString(scheduleResult)).append(division);
            autoScheduleLogService.insertCxScheduleLog(existResult.getCxBatchNo(), existResult.getOrderNo(), "【插单】更新成型排程数据",
                    logDetail.toString()); //添加日志
            cxScheduleEngineMapper.updateScheduleResultPlanQty(scheduleResult);
        }else{
            //获取到抓取记录后设置批次号
            scheduleResult.setCxBatchNo(cxBatchNo);
            scheduleResult.setCreateBy(SecurityUtils.getUsername());
            //插入插单日志日志
            logDetail.append("排程结果数据：").append(toJSONString(scheduleResult)).append(division);
            autoScheduleLogService.insertCxScheduleLog(scheduleResult.getCxBatchNo(), scheduleResult.getOrderNo(), "【插单】生成成型排程数据",
                    logDetail.toString()); //添加日志
            cxScheduleEngineMapper.insertCxScheduleResult(scheduleResult);
        }
        //Joran 2021-11-05 进行移除验证通过标记
        scheduleCheckService.delValidateRedisMark(key);

    }

    /**
     * 验证胎胚维度汇总表数据
     * @param scheduleResult
     * @param monthPlanApsVersion
     * @return
     */
    private CxEngineEmbryoMonthPlanSurplus validateInsertCxEngineEmbryoMonthPlanSurPlus(CxEngineScheduleResult scheduleResult, String monthPlanApsVersion) {
        //验证如果来源是主计划的存在，则证明是待投产插单，月度汇总表相关不需要进行操作
        List<CxEngineEmbryoMonthPlanSurplus> existEmbryoList=scheduleCheckService.listCxEngineEmbryoMonthPlanSurplus(monthPlanApsVersion,scheduleResult.getEmbryoCode());
        if(StringUtils.isEmpty(existEmbryoList)){//如果成型外胎汇总表数据不存在则创建
            //生成成型工序外胎计划量汇总表数据
            CxEngineEmbryoMonthPlanSurplus cxEngineEmbryoMonthPlanSurplus =scheduleCheckService.insertEngineEmbryoMonthPlanSurplus(scheduleResult,monthPlanApsVersion);
            //插入插单日志日志
            autoScheduleLogService.insertCxScheduleLog(scheduleResult.getCxBatchNo(), scheduleResult.getOrderNo(), "插单生成成型外胎汇总表数据",
                    "生成月度胎胚汇总表数据：" + toJSONString(cxEngineEmbryoMonthPlanSurplus)); //添加日志
            return cxEngineEmbryoMonthPlanSurplus;
        }else{
            //遍历查找如果找到了则验证是否来源插单，如果来源插单的话数据累加，如果是主计划则不处理
            for(CxEngineEmbryoMonthPlanSurplus cxEngineEmbryoMonthPlanSurplus:existEmbryoList){
                //数据来源为插单
                if(CxEngineConstants.CX_MONTH_PLAN_SURPLUS_DATA_SOURCE_INSERT.equals(cxEngineEmbryoMonthPlanSurplus.getDataSource())){
                    scheduleCheckService.updateCxEngineEmbryoMonthPlanSurplus(cxEngineEmbryoMonthPlanSurplus,scheduleResult.getMonthRemainQty());
                    //Joran 2021-11-02 设置成型产量start
                    scheduleResult.setCxMonthFinishQty(cxEngineEmbryoMonthPlanSurplus.getMonthFinishQty());//成型产量
                    scheduleResult.setMonthFinishQty(cxEngineEmbryoMonthPlanSurplus.getMonthFinishQty());//成型产量
                    //Joran 2021-11-02 设置成型产量end
                    //插入插单日志日志
                    autoScheduleLogService.insertCxScheduleLog(scheduleResult.getCxBatchNo(), scheduleResult.getOrderNo(), "更新生成成型胎胚汇总表数据",
                            "插单更新】增加计划调整量：" +scheduleResult.getMonthRemainQty()); //添加日志
                    return cxEngineEmbryoMonthPlanSurplus;
                }
            }
        }
        log.debug("【插单操作】成型胎胚计划汇总表无需操作");
        return null;
    }

    /**
     * 插入外胎月度汇总表数据
     * @param scheduleResult
     * @param monthPlanApsVersion
     */
    private CxEngineMonthPlanSurplus validateInsertCxMonthPlanSurPlus(CxEngineScheduleResult scheduleResult, String monthPlanApsVersion) {
        //验证如果来源是主计划的存在，则证明是待投产插单，月度汇总表相关不需要进行操作
        List<CxEngineMonthPlanSurplus> existList = scheduleCheckService.listCxEngineMonthPlanSurplus(monthPlanApsVersion,scheduleResult.getSapCode());
        if(StringUtils.isEmpty(existList)){//如果成型外胎汇总表数据不存在则创建
            //生成成型工序外胎计划量汇总表数据
            CxEngineMonthPlanSurplus cxMonthPlanSurPlus =scheduleCheckService.insertCxMonthPlanSurplus(scheduleResult,monthPlanApsVersion);
            //插入插单日志日志
            autoScheduleLogService.insertCxScheduleLog(scheduleResult.getCxBatchNo(), scheduleResult.getOrderNo(), "插单生成成型外胎汇总表数据",
                    "【插单生成】生成月度汇总表数据：" + toJSONString(cxMonthPlanSurPlus)); //添加日志
            return cxMonthPlanSurPlus;
        }else{
            //遍历查找如果找到了则验证是否来源插单，如果来源插单的话数据累加，如果是主计划则不处理
            for(CxEngineMonthPlanSurplus cxEngineMonthPlanSurplus:existList){
                //数据来源为插单
                if(CxEngineConstants.CX_MONTH_PLAN_SURPLUS_DATA_SOURCE_INSERT.equals(cxEngineMonthPlanSurplus.getDataSource())){
                    scheduleCheckService.updateCxEngineMonthPlanSurplus(cxEngineMonthPlanSurplus,scheduleResult.getMonthRemainQty());
                    //插入插单日志日志
                    autoScheduleLogService.insertCxScheduleLog(scheduleResult.getCxBatchNo(), scheduleResult.getOrderNo(), "更新生成成型外胎汇总表数据",
                            "【插单更新】增加计划调整量：" +scheduleResult.getMonthRemainQty()); //添加日志
                    return cxEngineMonthPlanSurplus;
                }
            }
        }
        log.debug("【插单操作】成型外胎计划汇总表无需操作");
        return null;

    }

    /**
     * 计划量格式化
     * @param cxScheduleResult
     */
    private void formatterPlanQty(CxEngineScheduleResult cxScheduleResult) {
        Integer totalPlanQty=0;
        if(cxScheduleResult.getClass1PlanQty()==null){
            cxScheduleResult.setClass1PlanQty(0);
        }
        totalPlanQty+=cxScheduleResult.getClass1PlanQty();
        if(cxScheduleResult.getClass2PlanQty()==null){
            cxScheduleResult.setClass2PlanQty(0);
        }
        totalPlanQty+=cxScheduleResult.getClass2PlanQty();
        if(cxScheduleResult.getClass3PlanQty()==null){
            cxScheduleResult.setClass3PlanQty(0);
        }
        totalPlanQty+=cxScheduleResult.getClass3PlanQty();
        if(cxScheduleResult.getClass4PlanQty()==null){
            cxScheduleResult.setClass4PlanQty(0);
        }
        totalPlanQty+=cxScheduleResult.getClass4PlanQty();
        if(cxScheduleResult.getClass5PlanQty()==null){
            cxScheduleResult.setClass5PlanQty(0);
        }
        totalPlanQty+=cxScheduleResult.getClass5PlanQty();
        //将任务累加汇总成月度剩余量
        cxScheduleResult.setMonthRemainQty(totalPlanQty);
    }

    /**
     * 获取插单的计划总任务量
     * @param cxScheduleResult
     * @return
     */
    public Integer insertTotalPlanQty(CxScheduleResult cxScheduleResult){
        Integer totalPlanQty=0;
        if(cxScheduleResult.getClass1PlanQty()==null){
            cxScheduleResult.setClass1PlanQty(0);
        }
        totalPlanQty+=cxScheduleResult.getClass1PlanQty();
        if(cxScheduleResult.getClass2PlanQty()==null){
            cxScheduleResult.setClass2PlanQty(0);
        }
        totalPlanQty+=cxScheduleResult.getClass2PlanQty();
        if(cxScheduleResult.getClass3PlanQty()==null){
            cxScheduleResult.setClass3PlanQty(0);
        }
        totalPlanQty+=cxScheduleResult.getClass3PlanQty();
        if(cxScheduleResult.getClass4PlanQty()==null){
            cxScheduleResult.setClass4PlanQty(0);
        }
        totalPlanQty+=cxScheduleResult.getClass4PlanQty();
        if(cxScheduleResult.getClass5PlanQty()==null){
            cxScheduleResult.setClass5PlanQty(0);
        }
        totalPlanQty+=cxScheduleResult.getClass5PlanQty();
        return  totalPlanQty;
    }

}
