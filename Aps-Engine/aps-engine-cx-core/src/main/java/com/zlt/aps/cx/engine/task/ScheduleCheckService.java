package com.zlt.aps.cx.engine.task;

import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.engine.common.CxEngineQuotaCommonService;
import com.zlt.aps.common.engine.domain.EngineProductConstructionInfo;
import com.zlt.aps.common.engine.domain.MdmMonthPlanMain;
import com.zlt.aps.common.engine.domain.MdmMonthProdPlan;
import com.zlt.aps.common.engine.service.MdmMonthPlanMainService;
import com.zlt.aps.common.engine.service.MdmMonthProdPlanService;
import com.zlt.aps.common.engine.utils.BeanConverUtil;
import com.zlt.aps.common.engine.utils.CollectionUtil;
import com.zlt.aps.common.engine.utils.GenerageMapKeyUtils;
import com.zlt.aps.cx.api.domain.dto.CxProductConstructionInfoDto;
import com.zlt.aps.cx.api.domain.entity.CxMachineInfo;
import com.zlt.aps.cx.api.domain.entity.CxPlanProductStatus;
import com.zlt.aps.cx.api.domain.entity.CxScheduleResult;
import com.zlt.aps.cx.engine.common.CommonCacheService;
import com.zlt.aps.cx.engine.constants.CxEngineConstants;
import com.zlt.aps.cx.engine.constants.CxParamCodeConstants;
import com.zlt.aps.cx.engine.constants.CxPrefixConstants;
import com.zlt.aps.cx.engine.domain.CxEngineAutoScheduleRecord;
import com.zlt.aps.cx.engine.domain.CxEngineEmbryoMonthPlanSurplus;
import com.zlt.aps.cx.engine.domain.CxEngineMonthPlanSurplus;
import com.zlt.aps.cx.engine.domain.CxEngineMonthStock;
import com.zlt.aps.cx.engine.domain.CxEngineScheduleResult;
import com.zlt.aps.cx.engine.domain.CxEngineStock;
import com.zlt.aps.cx.engine.enums.ClassEnums;
import com.zlt.aps.cx.engine.exception.CxScheduleEngineException;
import com.zlt.aps.cx.engine.mapper.CxEngineStockMapper;
import com.zlt.aps.cx.engine.mapper.CxScheduleEngineMapper;
import com.zlt.aps.cx.engine.service.CxEngineAutoScheduleRecordService;
import com.zlt.aps.cx.engine.service.CxEngineEmbryoMonthPlanSurplusService;
import com.zlt.aps.cx.engine.service.CxEngineMachineInfoService;
import com.zlt.aps.cx.engine.service.CxEngineMonthPlanSurplusService;
import com.zlt.aps.cx.engine.utils.CxScheduleUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 *  成型排程引擎校验
 */
@Component("scheduleCheckService")
@Slf4j
public class ScheduleCheckService {

    @Autowired
    private CxScheduleEngineMapper cxScheduleEngineMapper;

    @Autowired
    private CxEngineMachineInfoService cxEngineMachineInfoService;

    @Autowired
    private CxEngineQuotaCommonService cxEngineQuotaCommonService;

    @Autowired
    private CxEngineAutoScheduleRecordService cxEngineAutoScheduleRecordService;

    @Autowired
    private CxEngineMonthPlanSurplusService cxEngineMonthPlanSurplusService;

    @Autowired
    private CxEngineEmbryoMonthPlanSurplusService cxEngineEmbryoMonthPlanSurplusService;

    @Autowired
    private CommonCacheService cacheService;

    @Autowired
    private CxEngineStockMapper cxEngineStockMapper;

    @Autowired
    private MdmMonthPlanMainService mdmMonthPlanMainService;

    @Autowired
    private MdmMonthProdPlanService mdmMonthProdPlanService;

    //所有施工信息
    private Map<String, EngineProductConstructionInfo> engineConstructionInfoMap;

    private Map<String,String> cxParamsMap;

    /**
     * 验证成型机
     * @param machineCode
     * @param errorMsg
     * @return
     */
    public CxMachineInfo validateCxMachine(String machineCode,StringBuilder errorMsg){
        //验证成型机
        Map<String,CxMachineInfo> cxMachineInfoMap=cxEngineQuotaCommonService.getCxMachineInfoFromRedis();
        if(StringUtils.isEmpty(cxMachineInfoMap)||!cxMachineInfoMap.containsKey(machineCode)){
            errorMsg.append(I18nUtil.getMessage("cx.engine.machine.not.exsit")) ;
            return null;
        }
        return  cxMachineInfoMap.get(machineCode);
    }

    /**
     * 验证施工信息
     * @return
     */
    public EngineProductConstructionInfo validateConstructionInfo(String embryoCode,String bomDataVersion,StringBuilder errorMsg){
        StringBuilder sb=new StringBuilder();
        String key=GenerageMapKeyUtils.createMapKey(embryoCode,bomDataVersion);
        if(StringUtils.isEmpty(engineConstructionInfoMap)){
            sb.append(I18nUtil.getMessage("cx.engine.construction.empty.exception")) ;
        }else if(!engineConstructionInfoMap.containsKey(key)){
            sb.append(I18nUtil.getMessage("cx.engine.construction.embryo.not.exist")) ;
        }
        if(StringUtils.isNotEmpty(sb)){
            errorMsg.append(sb);
            return null;
        }
        return engineConstructionInfoMap.get(key);
    }

    /**
     * 验证自动排程抓取记录
     * @param scheduleDate
     * @return
     */
    public CxEngineAutoScheduleRecord validateAutoScheduleRecord(Date scheduleDate,StringBuilder tipMsg){
        //获取排程抓取记录取得工单号和生产排程计划版本号
        String scheduleDateStr= DateUtils.parseDateToStr("yyyyMMdd",scheduleDate);
        CxEngineAutoScheduleRecord autoScheduleRecord=selectSheduleRecordByScheduleDate(scheduleDateStr);
        if(autoScheduleRecord==null){ //Joran 2021-08-03 如果不存在则创建一条记录
            //Joran 2021-11-25 前端已有校验，引擎端提示校验去掉
            //tipMsg.append(I18nUtil.getMessage("cx.engine.auto.schedule.record.empty.tip"));
        }
        return autoScheduleRecord;
    }

    /**
     * 验证主计划主表信息
     * @param scheduleDate
     * @return
     */
    public MdmMonthPlanMain validateMdmMonthPlanMain(Date scheduleDate){
        //获取生产排程版本
        MdmMonthPlanMain planVersion=mdmMonthPlanMainService.getValidPlanMainVersion(scheduleDate);
        if(planVersion==null){
            throw new CxScheduleEngineException(I18nUtil.getMessage("cx.engine.auto.plan.main.empty.error"));
        }
        return planVersion;
    }

    /**
     * 增加月度版本明细中施工版本不全验证
     * @param apsMonthVersion
     */
    public  void  validateProdPlanBomDataVersion(String apsMonthVersion){
        //Joran 2021-12-15 添加月度计划明细中施工版本不全校验start
        MdmMonthProdPlan  condition= new MdmMonthProdPlan();
        condition.setMonthPlanApsVersion(apsMonthVersion);
        List<MdmMonthProdPlan> prodPlanList=mdmMonthProdPlanService.selectMdmMonthProdPlanList(condition);
        if(StringUtils.isNotEmpty(prodPlanList)){
            for(MdmMonthProdPlan mdmMonthProdPlan:prodPlanList){
                if(StringUtils.isEmpty(mdmMonthProdPlan.getBomDataVersion())){
                    throw new CxScheduleEngineException(I18nUtil.getMessage("cx.engine.validate.prodPlan.bomDataVersion.empty"));
                }
            }
        }
        //Joran 2021-12-15 添加月度计划明细中施工版本不全校验end
    }

    /**
     * 创建自动排程抓取记录
     * @param scheduleDate
     */
    public CxEngineAutoScheduleRecord createAutoScheduleRecord(Date scheduleDate){
        //获取排程抓取记录取得工单号和生产排程计划版本号
        String scheduleDateStr= DateUtils.parseDateToStr("yyyyMMdd",scheduleDate);
        //获取生产排程版本
        MdmMonthPlanMain planVersion=mdmMonthPlanMainService.getValidPlanMainVersion(scheduleDate);
        if(planVersion==null){
            throw new CxScheduleEngineException(I18nUtil.getMessage("cx.engine.auto.plan.main.empty.error"));
        }
        //成型自动排程批次号
        String  cxBatchNo=cacheService.getCxSequence(CxPrefixConstants.SCHEDULE_BATCH_NO_PREFIX+scheduleDateStr, CxPrefixConstants.CX_BATCH_NO_PREFIX+scheduleDateStr);
        //生成一条自动排程插单记录
        return cxEngineAutoScheduleRecordService.generagAutoScheduleRecord(scheduleDate,planVersion.getMonthPlanApsVersion(),cxBatchNo, CxEngineConstants.AUTO_SCHEDULE_STATUS_SUCCESS,"自动排程前插单");
    }

    /**
     * 验证寸口提醒信息
     * @param cxMachineInfo
     * @param engineConstructionInfo
     * @param tipMsg
     */
    public void checkDimension(CxMachineInfo cxMachineInfo,EngineProductConstructionInfo engineConstructionInfo,StringBuilder tipMsg){
        //获取到当前成型机信息
        Double dimensionMiniMum =cxMachineInfo.getDimensionMiniMum();//寸口下限
        Double dimensionMaxiMum =cxMachineInfo.getDimensionMaxiMum();//寸口上限
        Double dimension=engineConstructionInfo.getDimension();//施工寸口
        if(dimensionMiniMum!=null && dimensionMiniMum>dimension){
           String minMsg=StringUtils.format(I18nUtil.getMessage("cx.engine.check.dimension.min.tip"), dimensionMiniMum,dimension);
           tipMsg.append(minMsg);
        }

        if(dimensionMaxiMum!=null && dimensionMaxiMum<dimension){
            String minMsg=StringUtils.format(I18nUtil.getMessage("cx.engine.check.dimension.max.tip"), dimensionMaxiMum,dimension);
            tipMsg.append(minMsg);
        }

        //验证胎胚代码是否符合条件
        String embryoCodePrefix=cacheService.getEmbryoCodePrefix(cxMachineInfo.getMachineType(),cxParamsMap);
        String embryoCode=engineConstructionInfo.getEmbryoCode();
        if(!StringUtils.startsWithIgnoreCase(embryoCode,embryoCodePrefix)){
            String minMsg=StringUtils.format(I18nUtil.getMessage("cx.engine.check.embryoCode.tip"), cxMachineInfo.getMachineName(),embryoCode);
            tipMsg.append(minMsg);
        }


    }

    /**
     * 加载成型外胎汇总表数据
     * @param monthPlanApsVersion
     * @param sapCode
     * @return
     */
    public List<CxEngineMonthPlanSurplus> listCxEngineMonthPlanSurplus(String monthPlanApsVersion,String sapCode){
        CxEngineMonthPlanSurplus condition=new CxEngineMonthPlanSurplus();
        if(StringUtils.isNotEmpty(monthPlanApsVersion)){
            condition.setMonthPlanApsVersion(monthPlanApsVersion);
        }
        condition.setSapCode(sapCode);
        List<CxEngineMonthPlanSurplus> existList=this.cxEngineMonthPlanSurplusService.listCxEngineMonthPlanSurplus(condition);
        return  existList;
    }

    /**
     * 加载成型胎胚汇总表数据
     * @param monthPlanApsVersion
     * @param embryoCode
     * @return
     */
    public List<CxEngineEmbryoMonthPlanSurplus> listCxEngineEmbryoMonthPlanSurplus(String monthPlanApsVersion, String embryoCode){
        CxEngineEmbryoMonthPlanSurplus condition=new CxEngineEmbryoMonthPlanSurplus();
        if(StringUtils.isNotEmpty(monthPlanApsVersion)){
            condition.setMonthPlanApsVersion(monthPlanApsVersion);
        }
        condition.setEmbryoCode(embryoCode);
        List<CxEngineEmbryoMonthPlanSurplus> existList=this.cxEngineEmbryoMonthPlanSurplusService.selectCxEmbryoMonthPlanSurplusList(condition);
        return  existList;
    }

    /**
     * 验证超产提醒
     * @param mpsMonthEmbryoPlanSurPlus
     * @param sapCode
     * @param embryoCode
     * @param scheduleDate
     */
    public void checkMonthRemainQty(CxEngineEmbryoMonthPlanSurplus mpsMonthEmbryoPlanSurPlus,int totalPlanQty,String sapCode,String embryoCode,Date scheduleDate,StringBuilder tipMsg){
        //查询插单日期对应的排程结果中已经排的总计划量
        CxEngineScheduleResult cxEngineScheduleResult=new CxEngineScheduleResult();
        cxEngineScheduleResult.setSapCode(sapCode);
        cxEngineScheduleResult.setEmbryoCode(embryoCode);
        cxEngineScheduleResult.setScheduleDate(scheduleDate);
        Integer schedulePlanQty=cxScheduleEngineMapper.selectSchedulePlanQtyByCondition(cxEngineScheduleResult);
        schedulePlanQty=schedulePlanQty==null?0:schedulePlanQty;
        int monthRemainQty=mpsMonthEmbryoPlanSurPlus.getMonthRemainQty();//月度剩余量
        if(totalPlanQty+schedulePlanQty>monthRemainQty){
            String msg=StringUtils.format(I18nUtil.getMessage("cx.engine.remain.out.tip"), monthRemainQty,schedulePlanQty,totalPlanQty,totalPlanQty+schedulePlanQty-monthRemainQty);
            tipMsg.append(msg);
        }
    }

    /**
     * 验证排程结果是否存在提醒
     * @param scheduleDate
     * @param sapCode
     * @param embryoCode
     * @param machineCode
     * @param tipMsg
     */
    public CxEngineScheduleResult validateScheduleResult(Date scheduleDate,String sapCode,String embryoCode,String machineCode,String bomDataVersion,StringBuilder tipMsg){
        CxEngineScheduleResult cxEngineScheduleResult=new CxEngineScheduleResult();
        if(scheduleDate!=null){
            cxEngineScheduleResult.setScheduleDate(scheduleDate);
        }
        if(StringUtils.isNotEmpty(sapCode)){
            cxEngineScheduleResult.setSapCode(sapCode);
        }
        if(StringUtils.isNotEmpty(embryoCode)){
            cxEngineScheduleResult.setEmbryoCode(embryoCode);
        }
        if(StringUtils.isNotEmpty(machineCode)){
            cxEngineScheduleResult.setCxMachineCode(machineCode);
        }
        if(StringUtils.isNotEmpty(bomDataVersion)){
            cxEngineScheduleResult.setBomDataVersion(bomDataVersion);
        }
        CxEngineScheduleResult existResult=this.cxScheduleEngineMapper.selectScheduleResult(cxEngineScheduleResult);
        if(existResult!=null){
            tipMsg.append(I18nUtil.getMessage("cx.engine.schedule.repeat.tip"));
        }
        return  existResult;
    }

    /**
     * 创建成型外胎计划汇总表
     * @param scheduleResult
     * @param monthPlanApsVersion
     * @return
     */
    public CxEngineMonthPlanSurplus insertCxMonthPlanSurplus(CxEngineScheduleResult scheduleResult, String monthPlanApsVersion){
        log.debug("【插单操作】新增成型外胎计划量汇总表数据");
        CxEngineMonthPlanSurplus cxMonthPlanSurPlus =new CxEngineMonthPlanSurplus();
        cxMonthPlanSurPlus.setMonthPlanApsVersion(monthPlanApsVersion);//月度计划生产排程版本
        cxMonthPlanSurPlus.setYear(DateUtils.parseDateToStr("yyyy",scheduleResult.getScheduleDate()));
        cxMonthPlanSurPlus.setMonth(DateUtils.parseDateToStr("MM",scheduleResult.getScheduleDate()));
        cxMonthPlanSurPlus.setSapCode(scheduleResult.getSapCode());
        //Joran 2021-10-30 添加插单外胎保留胎胚代码
        cxMonthPlanSurPlus.setEmbryoCode(scheduleResult.getEmbryoCode());
        cxMonthPlanSurPlus.setMonthPlanQty(0);//计划总量
        cxMonthPlanSurPlus.setMonthFinishQty(0);//月度完成量
        cxMonthPlanSurPlus.setPlanModifyQty(scheduleResult.getMonthRemainQty());//月度调整量
        cxMonthPlanSurPlus.setLastMonthStock(0);//月结库存
        cxMonthPlanSurPlus.setSapBadQty(0);//不良数
        cxMonthPlanSurPlus.setMonthRemainQty(scheduleResult.getMonthRemainQty());//月度剩余量
        cxMonthPlanSurPlus.setDataSource(CxEngineConstants.CX_MONTH_PLAN_SURPLUS_DATA_SOURCE_INSERT);//数据来源设置为插单
        cxEngineMonthPlanSurplusService.insertCxMonthPlanSurplus(cxMonthPlanSurPlus);
        return cxMonthPlanSurPlus;
    }

    /**
     * 创建成型胎胚月度计划汇总表
     * @param scheduleResult
     * @param monthPlanApsVersion
     * @return
     */
    public CxEngineEmbryoMonthPlanSurplus insertEngineEmbryoMonthPlanSurplus(CxEngineScheduleResult scheduleResult, String monthPlanApsVersion){
        log.debug("【插单操作】新增成型胎胚计划量汇总表数据");
        CxEngineEmbryoMonthPlanSurplus cxEngineEmbryoMonthPlanSurplus =new CxEngineEmbryoMonthPlanSurplus();
        cxEngineEmbryoMonthPlanSurplus.setMonthPlanApsVersion(monthPlanApsVersion);//月度计划生产排程版本
        cxEngineEmbryoMonthPlanSurplus.setYear(DateUtils.parseDateToStr("yyyy",scheduleResult.getScheduleDate()));
        cxEngineEmbryoMonthPlanSurplus.setMonth(DateUtils.parseDateToStr("MM",scheduleResult.getScheduleDate()));
        cxEngineEmbryoMonthPlanSurplus.setEmbryoCode(scheduleResult.getEmbryoCode());
        cxEngineEmbryoMonthPlanSurplus.setMonthPlanQty(0);//计划总量
        cxEngineEmbryoMonthPlanSurplus.setMonthFinishQty(0);//月度完成量
        cxEngineEmbryoMonthPlanSurplus.setMonthPlanModifyQty(scheduleResult.getMonthRemainQty());//月度调整量
        cxEngineEmbryoMonthPlanSurplus.setLastMonthStock(0);//月结库存
        cxEngineEmbryoMonthPlanSurplus.setEmbryoBadQty(0);//不良数
        cxEngineEmbryoMonthPlanSurplus.setMonthRemainQty(scheduleResult.getMonthRemainQty());//月度剩余量
        cxEngineEmbryoMonthPlanSurplus.setDataSource(CxEngineConstants.CX_MONTH_PLAN_SURPLUS_DATA_SOURCE_INSERT);//数据来源设置为插单
        //插入胎胚汇总表
        cxEngineEmbryoMonthPlanSurplusService.insertCxEmbryoMonthPlanSurplus(cxEngineEmbryoMonthPlanSurplus);
        return cxEngineEmbryoMonthPlanSurplus;
    }

    /**
     * 更新月度汇总计划表计划量
     * @param cxMonthPlanSurPlus
     * @param monthRemainQty
     */
    public void updateCxEngineMonthPlanSurplus(CxEngineMonthPlanSurplus cxMonthPlanSurPlus,Integer monthRemainQty){
        int monthTotal=cxMonthPlanSurPlus.getMonthPlanQty();
        monthTotal+=monthRemainQty;
        //此处只将计划调整量设值，由SQL进行累加操作
        cxMonthPlanSurPlus.setPlanModifyQty(monthRemainQty); //Joran 2021-07-28 更新计划修正量
        cxMonthPlanSurPlus.setPlanModifyQty(monthRemainQty); //Joran 2021-11-30 更新月度剩余量
        log.debug("【插单操作】更新成型外胎计划量汇总表数据来源插单的计划量,最新计划量="+monthTotal);
        cxMonthPlanSurPlus.setUpdateTime(DateUtils.getNowDate());
        cxEngineMonthPlanSurplusService.updateCxMonthPlanSurplus(cxMonthPlanSurPlus);
    }

    /**
     * 更新月度汇总计划表计划量
     * @param cxEngineEmbryoMonthPlanSurplus
     * @param monthRemainQty
     */
    public void updateCxEngineEmbryoMonthPlanSurplus(CxEngineEmbryoMonthPlanSurplus cxEngineEmbryoMonthPlanSurplus,Integer monthRemainQty){
        int monthTotal=cxEngineEmbryoMonthPlanSurplus.getMonthPlanQty();
        monthTotal+=monthRemainQty;
        //此处只将计划总量设置，由SQL进行累加操作
        cxEngineEmbryoMonthPlanSurplus.setMonthPlanModifyQty(monthRemainQty); //更新累加计划调整量
        cxEngineEmbryoMonthPlanSurplus.setMonthRemainQty(monthRemainQty);//更新累加的剩余量
        log.debug("【插单操作】更新成型胎胚计划量汇总表数据来源插单的计划量,最新计划量="+monthTotal);
        cxEngineEmbryoMonthPlanSurplus.setUpdateTime(DateUtils.getNowDate());
        cxEngineEmbryoMonthPlanSurplusService.updateCxEmbryoMonthPlanSurplus(cxEngineEmbryoMonthPlanSurplus);
    }

    /**
     * 数据填充
     * @param cxScheduleResult
     * @param engineConstructionInfo
     */
    public void dataFilling(CxEngineScheduleResult cxScheduleResult, EngineProductConstructionInfo engineConstructionInfo) {
        cxScheduleResult.setSpecDimension(engineConstructionInfo.getDimension());//寸口
        //cxScheduleResult.setSpecDesc(engineConstructionInfo.getSpecDesc());//规格描述
        cxScheduleResult.setClass1Sort(0);
        cxScheduleResult.setClass2Sort(0);
        cxScheduleResult.setClass3Sort(0);
        cxScheduleResult.setClass4Sort(0);
        cxScheduleResult.setClass5Sort(0);
        cxScheduleResult.setClass3PlannedQty(0);
        cxScheduleResult.setMarkCloseOutTip(CxEngineConstants.CLOSE_OUT_TIP_NO);
        cxScheduleResult.setIsRelease(CxEngineConstants.IS_PUBLISH_NO);//未发布
        cxScheduleResult.setProductionStatus(CxEngineConstants.PRODUCTION_STATUS_UNDO);//未生产
        //库存填充
        fillCxStock(cxScheduleResult);
        cacheService.calcLeastLhMachineQtyByMonthRemainQty(cxScheduleResult,cxScheduleResult.getMonthRemainQty(),null);//计算最小硫化机需求数
        //Joran 2022-01-03 通过硫化施工设置规格型号信息
        cacheService.setSpecDescBySapCode(cxScheduleResult,null);
        CxScheduleUtils.calcAllClassAvailableLhShift(cxScheduleResult);//计算各班硫化班次
    }

    /**
     * 库存填充
     * @param cxScheduleResult
     */
    public void  fillCxStock(CxEngineScheduleResult cxScheduleResult){
        Date scheduleDate=cxScheduleResult.getScheduleDate();
        //获取前一天
        Date lastDate=DateUtils.addDays(scheduleDate,-1);
        String scheduleDateStr= DateUtils.parseDateToStr("yyyyMMdd",lastDate);
        CxEngineStock condition =new CxEngineStock();
        condition.setStockDateStr(scheduleDateStr);
        condition.setEmbryoCode(cxScheduleResult.getEmbryoCode());
        List<CxEngineStock> stockList=this.cxEngineStockMapper.selectCxEngineStockList(condition);
        if(StringUtils.isEmpty(stockList)){
            cxScheduleResult.setTotalStock(0);
        }else{
            CxEngineStock stock =stockList.get(0);
            cxScheduleResult.setTotalStock(stock.getStockRealNum());//设置库存
        }

        //Joran 2021-11-02 填充月结库存信息start
        CxEngineMonthStock monthStock=new CxEngineMonthStock();
        monthStock.setStockMonthStr(DateUtils.parseDateToStr("yyyyMM",DateUtils.addMonths(scheduleDate,-1)));
        monthStock.setEmbryoCode(cxScheduleResult.getEmbryoCode());
        List<CxEngineMonthStock> monthStockList=this.cxEngineStockMapper.selectCxEngineMonthStockList(monthStock);
        if(StringUtils.isNotEmpty(monthStockList)){
            CxEngineMonthStock cxEngineMonthStock=monthStockList.get(0);
            if(StringUtils.isNotEmpty(cxEngineMonthStock.getStockNum())){
                cxScheduleResult.setMonthStock(Integer.valueOf(cxEngineMonthStock.getStockNum()));
            }else{
                cxScheduleResult.setMonthStock(0);
            }
        }else{
            cxScheduleResult.setMonthStock(0);
        }
        //Joran 2021-11-02 填充月结库存信息end
        //生成工单号
        cxScheduleResult.setOrderNo(cacheService.getCxSequence(CxPrefixConstants.SCHEDULE_ORDER_NO_PREFIX+scheduleDateStr, CxPrefixConstants.CX_ORDER_NO_PREFIX+scheduleDateStr));

    }



    /**
     * 数据初始化
     * @ClassName InsertTaskService
     * @Description 初始化相应的施工信息
     * @Author Joran.Zhang
     * @Date 2021/7/20 15:55
     * @Version 1.0
     **/
    public void initBaseData() {
        //加载全部胎胚的施工信息
        engineConstructionInfoMap=cxEngineQuotaCommonService.loadEngineConstructionMapFromRedis();
        cxParamsMap=cacheService.loadCxParamsMap();
    }

    /**
     * 根据排程日期获取抓取记录
     * @param scheduleDate
     * @return
     */
    public CxEngineAutoScheduleRecord selectSheduleRecordByScheduleDate(String scheduleDate){
        return cxEngineAutoScheduleRecordService.selectAutoScheduleRecordByScheduleDate(scheduleDate);
    }

    /**
     * 投产进行入参基础校验
     * @param cxPlanProductStatus
     * @param errorMsg
     */
    public void productValidateParam(CxPlanProductStatus cxPlanProductStatus,StringBuilder errorMsg){
        String embryoCode=cxPlanProductStatus.getEmbryoCode();//胎胚代码
        String machineCode=cxPlanProductStatus.getCxMachineCode();//成型机台编号
        if(cxPlanProductStatus.getScheduleDate()==null){
            errorMsg.append(I18nUtil.getMessage("cx.engine.product.shceduleDate.empty.error")) ;
        }
        if(StringUtils.isEmpty(cxPlanProductStatus.getSapCode())){
            errorMsg.append(I18nUtil.getMessage("cx.engine.product.sapCode.empty.error")) ;
        }
        if(StringUtils.isEmpty(embryoCode)){
            errorMsg.append(I18nUtil.getMessage("cx.engine.product.embryoCode.empty.error")) ;
        }
        if(StringUtils.isEmpty(cxPlanProductStatus.getBomDataVersion())){
            errorMsg.append(I18nUtil.getMessage("cx.engine.product.bomDataVersion.empty.error")) ;
        }
        if(StringUtils.isEmpty(cxPlanProductStatus.getStorageLocation())){
            errorMsg.append(I18nUtil.getMessage("cx.engine.product.storageLocation.empty.error")) ;
        }
        if(StringUtils.isEmpty(machineCode)){
            errorMsg.append(I18nUtil.getMessage("cx.engine.product.machineCode.empty.error")) ;
        }
    }

    /**
     * 添加插单的有计划量的计划量提示
     * @param scheduleResult
     * @param tipMsg
     */
    public void validateClassShiftPlanQty(CxScheduleResult scheduleResult, StringBuilder tipMsg) {
        //获取成型机台的所有成型排程任务
        CxEngineScheduleResult condition=new CxEngineScheduleResult();
        condition.setScheduleDate(scheduleResult.getScheduleDate());//排程日期
        condition.setCxMachineCode(scheduleResult.getCxMachineCode());
        List<CxEngineScheduleResult> machineTaskList=this.cxScheduleEngineMapper.selectCxScheduleResultList(condition);
        if(StringUtils.isEmpty(machineTaskList)){
            return;
        }
        //从一班开始设置班次计划量提示语
        setClassShiftTip(scheduleResult,machineTaskList,tipMsg);
    }

    /**
     * 设置班次计划量提示语
     * @param scheduleResult
     * @param machineTaskList
     * @param tipMsg
     */
    private void setClassShiftTip(CxScheduleResult scheduleResult, List<CxEngineScheduleResult> machineTaskList, StringBuilder tipMsg) {
       Integer classShiftIndex=1;
       CxEngineScheduleResult cxEngineScheduleResult= BeanConverUtil.conver(scheduleResult,CxEngineScheduleResult.class);
        cxEngineScheduleResult.initPlanQty();
        do{
            ClassEnums cls= ClassEnums.getClassEnums(classShiftIndex);
            if(CxScheduleUtils.getCurrentClassPlanQty(cxEngineScheduleResult,cls)>0){
                //设置班次计划提示语
                setShiftPlanQtyTip(scheduleResult,machineTaskList,cls,tipMsg);
            }
            classShiftIndex++;
        }while(classShiftIndex<6);

    }
    
    /**
      * 设置班次计划提醒(有计划量)
      * @ClassName ScheduleCheckService
      * @Description TODO
      * @Author Joran.Zhang
      * @Date 2021/8/13 9:51
      * @Version 1.0
    **/
    private void setShiftPlanQtyTip(CxScheduleResult scheduleResult,List<CxEngineScheduleResult> machineTaskList, ClassEnums cls, StringBuilder tipMsg) {
        String machineCode=scheduleResult.getCxMachineCode();
        //按班次的顺序进行升序
        CxScheduleUtils.resultSortAscByClassShiftSort(machineTaskList,cls);
        //遍历机台成型排程任务验证计划量是否满额
        Double remainTime= CxEngineConstants.CLASS_SHIFT_HOUR;//默认班次时间
        CxEngineScheduleResult lastCxEngineScheduleResult=null;
        for (CxEngineScheduleResult cxEngineScheduleResult : machineTaskList) {
            Integer planQty=CxScheduleUtils.getCurrentClassPlanQty(cxEngineScheduleResult,cls);
            if(planQty>0){
                //获取到机台对应的定额
                Integer specQuota=getQuotaByMachineEmbryoCode(machineCode,cxEngineScheduleResult.getEmbryoCode(),cxEngineScheduleResult.getBomDataVersion());
                BigDecimal hourCountBig=BigDecimal.valueOf(specQuota).divide(BigDecimal.valueOf(CxEngineConstants.CLASS_SHIFT_HOUR)); //一个小时生产多少
                BigDecimal planQtyBig=BigDecimal.valueOf(planQty);
                Double usedTime=planQtyBig.divide(hourCountBig,2, BigDecimal.ROUND_DOWN).doubleValue();
                remainTime-=usedTime; //班次时长扣减掉已经安排的时长算出剩余时间
                cxEngineScheduleResult.setRemainTime(remainTime);
                if(specQuota<=planQty){
                    tipMsg.append(StringUtils.format(I18nUtil.getMessage("cx.engine.insert.class" + cls.getClassIndex() + "PlanQty.tip"), 0));
                    return;
                }
                if(lastCxEngineScheduleResult!=null){
                    String lastEmbryoCode= lastCxEngineScheduleResult.getEmbryoCode();
                    String lastBomDataVersion=lastCxEngineScheduleResult.getBomDataVersion();
                    String lastKey=GenerageMapKeyUtils.createMapKey(lastEmbryoCode,lastBomDataVersion);
                    String embryoCode= cxEngineScheduleResult.getEmbryoCode();
                    String bomDataVersion=cxEngineScheduleResult.getBomDataVersion();
                    String key=GenerageMapKeyUtils.createMapKey(embryoCode,bomDataVersion);
                    Double changeSpecTime=CxScheduleUtils.changeSpecTime(cxEngineScheduleResult.getCxMachineType(),engineConstructionInfoMap.get(lastKey),engineConstructionInfoMap.get(key),minChangeSpecTimeByParams());
                    if (lastCxEngineScheduleResult.getRemainTime()<=0D||lastCxEngineScheduleResult.getRemainTime()<=changeSpecTime) {
                        tipMsg.append(StringUtils.format(I18nUtil.getMessage("cx.engine.insert.class" + cls.getClassIndex() + "PlanQty.tip"), 0));
                        return;
                    }
                    //更新剩余时间
                    remainTime-=changeSpecTime;
                    cxEngineScheduleResult.setRemainTime(remainTime);
                }
                lastCxEngineScheduleResult=cxEngineScheduleResult;
            }
        }

        //验证插单的计划量是否超出定额
        if(lastCxEngineScheduleResult==null){
         Integer planQty=CxScheduleUtils.getCurrentClassPlanQty(scheduleResult,cls);
         Integer quota=getQuotaByMachineEmbryoCode(scheduleResult.getCxMachineCode(),scheduleResult.getEmbryoCode(),scheduleResult.getBomDataVersion());
         if(planQty>quota){
             tipMsg.append(StringUtils.format(I18nUtil.getMessage("cx.engine.insert.class" + cls.getClassIndex() + "PlanQty.tip"), quota));
         }
         return;
        }

        setClassTipMsg(lastCxEngineScheduleResult,scheduleResult,cls,tipMsg);
    }

    /**
     * 具体提示语
     * @param cxEngineScheduleResult 自动排程排序最大的规格
     * @param cxScheduleResult 当前插入的规格
     * @param cls 班次
     * @param tipMsg 提示
     */
    private void setClassTipMsg(CxEngineScheduleResult cxEngineScheduleResult,CxScheduleResult cxScheduleResult,ClassEnums cls,StringBuilder tipMsg) {
        Integer planQty= CxScheduleUtils.getCurrentClassPlanQty(cxScheduleResult,cls);
        Double remainTime =cxEngineScheduleResult.getRemainTime();//获取剩余时间
        String beforeKey= GenerageMapKeyUtils.createMapKey(cxEngineScheduleResult.getEmbryoCode(),cxEngineScheduleResult.getBomDataVersion());
        String afterKey=GenerageMapKeyUtils.createMapKey(cxScheduleResult.getEmbryoCode(),cxScheduleResult.getBomDataVersion());
        Double changeSpecTime=CxScheduleUtils.changeSpecTime(cxEngineScheduleResult.getCxMachineType(),engineConstructionInfoMap.get(beforeKey),engineConstructionInfoMap.get(afterKey),minChangeSpecTimeByParams());
        if(remainTime <= CxEngineConstants.ZERO) {//没有剩余量表示占满了
            tipMsg.append(StringUtils.format(I18nUtil.getMessage("cx.engine.insert.class" + cls.getClassIndex() + "PlanQty.tip"), 0));
            return;
        }else if(remainTime<=changeSpecTime){
            tipMsg.append(StringUtils.format(I18nUtil.getMessage("cx.engine.insert.class" + cls.getClassIndex() + "PlanQty.tip"), 0));
            return;
        }else{
            Integer quota=getQuotaByMachineEmbryoCode(cxEngineScheduleResult.getCxMachineCode(),cxScheduleResult.getEmbryoCode(),cxScheduleResult.getBomDataVersion());
            //实际剩余时间
            BigDecimal remainTimeBig=BigDecimal.valueOf(remainTime-changeSpecTime); //剩余时间
            BigDecimal hourCountBig=BigDecimal.valueOf(quota).divide(BigDecimal.valueOf(CxEngineConstants.CLASS_SHIFT_HOUR)); //一个小时生产多少
            BigDecimal currentPlanQty=remainTimeBig.multiply(hourCountBig).setScale(0,BigDecimal.ROUND_DOWN);
            if(currentPlanQty.intValue()<planQty){
                tipMsg.append(StringUtils.format(I18nUtil.getMessage("cx.engine.insert.class" + cls.getClassIndex() + "PlanQty.tip"), currentPlanQty.intValue()));
                return;
            }
        }
    }

    /**
     * 插单获取定额数
     * @param cxMachineCode
     * @param embryoCode
     * @return
     */
    private Integer getQuotaByMachineEmbryoCode(String cxMachineCode,String embryoCode,String bomDataVersion) {
        Integer machineQuota=cxEngineQuotaCommonService.getCxMachineQuota(cxMachineCode,embryoCode,bomDataVersion);
        log.debug("【插单获取定额】获取成型机定额数据，机台编号："+cxMachineCode+"，胎胚代码："+embryoCode+"，定额数："+machineQuota);
        return machineQuota;
    }

    /**
     * 获取工序参数维护的更换工装最小时长
     * @return
     */
    private Double minChangeSpecTimeByParams(){
        String minChangeSpecTime=cxParamsMap.get(CxParamCodeConstants.CX_MIN_CHANGE_SPEC_TIME);
        if(StringUtils.isEmpty(minChangeSpecTime)){
            throw new IllegalArgumentException(I18nUtil.getMessage("cx.engine.auto.change.spec.min.time.param.error"));
        }
        //根据配置获取更换规格切换时间
        Double minChangeSpecTimeMin=Double.valueOf(minChangeSpecTime);
        return minChangeSpecTimeMin;
    }

    /**
     * 验证通过标记添加标记 失效为5分钟
     * @param key
     */
    public void validateRedisMark(String key){
        cacheService.setIfAbsent(key,"validate.success",5L, TimeUnit.MINUTES);
    }

    /**
     * 成功后移除标记
     * @param key
     */
    public void delValidateRedisMark(String key){
        cacheService.delRedisKey(key);
    }

    /**
     * 创建rediskey
     * @return
     */
    public String createKey(String prefix,String keyFrom) {
        return   prefix+":"+ keyFrom;
    }


    /**
     * 判断是否有值
     * @return
     */
    public boolean isValidate(String key){
        return  cacheService.hasKey(key);
    }

    /**
     *  根据月度计划版本获取月度计划明细数据
     * @param monthApsVersion
     * @return
     */
    public Map<String,List<MdmMonthProdPlan>> getMonthProdPlanMap(String monthApsVersion){
        Map<String, List<MdmMonthProdPlan>> resultMap= mdmMonthProdPlanService.selectMonthPlanListBymonthPlanApsVersion(monthApsVersion);
        return resultMap;
    }

    /**
     *  排程导入列表根据月度计划明细进行施工版本信息填充
     * @param scheduleDate
     * @param cxEngineScheduleResultList
     */
	public void fillingBomDataVersion(Date scheduleDate, List<CxEngineScheduleResult> cxEngineScheduleResultList) {
		if (scheduleDate == null || CollectionUtils.isEmpty(cxEngineScheduleResultList)) {
			return;
		}

		// 取出本次导入数据的所有胎胚号
		List<String> embryoCodeList = cxEngineScheduleResultList.stream()
				.filter(schedule -> schedule.getEmbryoCode() != null).map(CxEngineScheduleResult::getEmbryoCode)
				.distinct().collect(Collectors.toList());
		if (CollectionUtils.isEmpty(embryoCodeList)) {
			return;
		}

		// 查询胎胚号对应的施工信息
		List<CxProductConstructionInfoDto> constructionList = cxScheduleEngineMapper
				.listMultipleVersionConstruction(scheduleDate, embryoCodeList);
		if (CollectionUtils.isEmpty(constructionList)) {
			return;
		}
		Map<String, List<CxProductConstructionInfoDto>> constructionMap = constructionList.stream()
				.collect(Collectors.groupingBy(CxProductConstructionInfoDto::getEmbryoCode)); // 按胎胚号对施工信息分组

		// 遍历待导入的成型数据，按规则设置胎胚版本
		for (CxEngineScheduleResult cxEngineScheduleResult : cxEngineScheduleResultList) {
			String embryoCode = cxEngineScheduleResult.getEmbryoCode(); // 胎胚号
			if (embryoCode == null) {
				continue;
			}
			List<CxProductConstructionInfoDto> embryoConstructionList = constructionMap.get(embryoCode); // 该胎胚号对应的所有施工版本
			if (CollectionUtils.isEmpty(embryoConstructionList)) {
				continue;
			}

			// 根据施工版本的不同情况设置胎胚版本
			String embryoVersion;
			if (embryoConstructionList.size() == 1) { // 该胎胚只有一个有效版本，则直接设置该版本
				CxProductConstructionInfoDto construction = CollectionUtil.firstElement(embryoConstructionList);
				embryoVersion = construction.getEmbryoVersion();
			} else if (embryoConstructionList.stream().anyMatch(s -> s.isScheduleFlag())) { // 有多个有效版本，但是在7天内曾有过排产，版本留空
				embryoVersion = null;
			} else { // 有多个有效版本，但是在7天内都没有排程，则直接取最新的一个版本
				embryoVersion = embryoConstructionList.stream().max((c1, c2) -> {
					// 比较创建时间
					int result = ObjectUtils.compare(c1.getCreateTime(), c2.getCreateTime());
					if (result != 0) {
						return result;
					}
					// 创建时间相同则比较ID（插入数据库的时间先后）
					return ObjectUtils.compare(c1.getId(), c2.getId());
				}).map(CxProductConstructionInfoDto::getEmbryoVersion).orElse(null);
			}
			cxEngineScheduleResult.setBomDataVersion(embryoVersion);
		}
		// 清除施工表缓存
		cxEngineQuotaCommonService.delCacheConstructionInfoMap();
	}
}
