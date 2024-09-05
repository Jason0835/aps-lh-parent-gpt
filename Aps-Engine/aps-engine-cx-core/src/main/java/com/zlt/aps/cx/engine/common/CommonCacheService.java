package com.zlt.aps.cx.engine.common;

import com.alibaba.fastjson.JSON;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.utils.SecurityUtils;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.engine.common.CxEngineQuotaCommonService;
import com.zlt.aps.common.engine.constants.EngineConstants;
import com.zlt.aps.common.engine.domain.EngineProductConstructionInfo;
import com.zlt.aps.common.engine.domain.LhEngineTireConstructionInfo;
import com.zlt.aps.common.engine.domain.MdmMonthPlanMain;
import com.zlt.aps.common.engine.domain.MdmMonthProdPlan;
import com.zlt.aps.common.engine.domain.TCxEmbryoMonthPlanSurplus;
import com.zlt.aps.common.engine.planmain.MdmMonthPlanAmountSumService;
import com.zlt.aps.common.engine.service.AutoScheduleLogService;
import com.zlt.aps.common.engine.service.LhEngineTireConstructionInfoService;
import com.zlt.aps.common.engine.service.MdmMonthPlanMainService;
import com.zlt.aps.common.engine.service.MdmMonthProdPlanService;
import com.zlt.aps.common.engine.utils.BeanConverUtil;
import com.zlt.aps.common.engine.utils.GenerageMapKeyUtils;
import com.zlt.aps.cx.api.domain.dto.CxLastDaySupplePlanDto;
import com.zlt.aps.cx.api.domain.dto.CxParamsDto;
import com.zlt.aps.cx.api.domain.entity.CxScheduleResult;
import com.zlt.aps.cx.api.domain.entity.CxScheduleTaskTime;
import com.zlt.aps.cx.engine.constants.CxEngineConstants;
import com.zlt.aps.cx.engine.constants.CxParamCodeConstants;
import com.zlt.aps.cx.engine.constants.CxPrefixConstants;
import com.zlt.aps.cx.engine.domain.CxAutoScheduleTask;
import com.zlt.aps.cx.engine.domain.CxEngineEmbryoMonthPlanSurplus;
import com.zlt.aps.cx.engine.domain.CxEngineHolidaySetting;
import com.zlt.aps.cx.engine.domain.CxEngineLastDaySupplePlan;
import com.zlt.aps.cx.engine.domain.CxEngineMonthPlanSurplus;
import com.zlt.aps.cx.engine.domain.CxEngineMonthStock;
import com.zlt.aps.cx.engine.domain.CxEngineSapSpecMoldUse;
import com.zlt.aps.cx.engine.domain.CxEngineScheduleLimit;
import com.zlt.aps.cx.engine.domain.CxEngineScheduleResult;
import com.zlt.aps.cx.engine.domain.CxEngineStock;
import com.zlt.aps.cx.engine.domain.CxPlanProductStatus;
import com.zlt.aps.cx.engine.enums.ClassEnums;
import com.zlt.aps.cx.engine.exception.CxScheduleEngineException;
import com.zlt.aps.cx.engine.mapper.CxEngineHolidaySettingMapper;
import com.zlt.aps.cx.engine.mapper.CxEngineParamsMapper;
import com.zlt.aps.cx.engine.mapper.CxEngineStockMapper;
import com.zlt.aps.cx.engine.mapper.CxLhEngineCommonMapper;
import com.zlt.aps.cx.engine.mapper.CxScheduleEngineMapper;
import com.zlt.aps.cx.engine.service.CxEngineAutoScheduleRecordService;
import com.zlt.aps.cx.engine.service.CxEngineEmbryoMonthPlanSurplusService;
import com.zlt.aps.cx.engine.service.CxEngineGroupMachineListService;
import com.zlt.aps.cx.engine.service.CxEngineMonthPlanSurplusService;
import com.zlt.aps.cx.engine.service.CxPlanProductStatusService;
import com.zlt.aps.cx.engine.service.CxScheduleTaskTimeService;
import com.zlt.aps.cx.engine.task.CxEngineCommonService;
import com.zlt.aps.cx.engine.utils.CxScheduleUtils;
import com.zlt.aps.lh.api.domain.dto.LhParamsDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.support.atomic.RedisAtomicLong;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.alibaba.fastjson.JSON.toJSONString;

/**
 * 提供自增长流水号
 */
@Component("commonCacheService")
@Slf4j
public class CommonCacheService {

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private CxEngineParamsMapper cxEngineParamsMapper;

    @Autowired
    private CxEngineHolidaySettingMapper cxEngineHolidaySettingMapper;

    @Autowired
    private CxEngineStockMapper cxEngineStockMapper;

    @Autowired
    private LhEngineTireConstructionInfoService lhEngineTireConstructionInfoService;

    @Autowired
    private CxEngineGroupMachineListService cxEngineGroupMachineListService;

    @Autowired
    private CxEngineMonthPlanSurplusService cxEngineMonthPlanSurplusService;
    @Autowired
    private CxScheduleEngineMapper cxScheduleEngineMapper;
    @Autowired
    private MdmMonthPlanMainService mdmMonthPlanMainService;
    @Autowired
    private CxEngineEmbryoMonthPlanSurplusService cxEngineEmbryoMonthPlanSurplusService;

    @Autowired
    private AutoScheduleLogService autoScheduleLogService;
    @Autowired
    private MdmMonthPlanAmountSumService mdmMonthPlanAmountSumService;
    @Autowired
    private MdmMonthProdPlanService mdmMonthProdPlanService;
    @Autowired
    private CxPlanProductStatusService cxPlanProductStatusService;
    @Autowired
    private CxEngineAutoScheduleRecordService cxEngineAutoScheduleRecordService;

    @Autowired
    private CxEngineQuotaCommonService cxEngineQuotaCommonService;

    @Autowired
    private CxEngineCommonService cxEngineCommonService;

    @Autowired
    private CxLhEngineCommonMapper cxLhEngineCommonMapper;

    @Autowired
    private CxScheduleTaskTimeService cxScheduleTaskTimeService;

    //机台小时集合
    private Map<String,Double> machineShiftHourMap;

    private Map<String, EngineProductConstructionInfo> engineConstructionInfoMap;

    private Map<String,String > cxParamsMap;

    /**
     *  班次开始时间
     */
    private Map<String,Date> classShiftDateTime;

    private String division = "\r\n---------------------------------------------------\r\n";  //日志分割符

    /**
     * 根据Key获取流水号
     * @param key
     * @return
     */
    public Long getIncrementNumber(String key){
        RedisAtomicLong entityIdCounter=new RedisAtomicLong(key,redisTemplate.getConnectionFactory());
        Long counter=entityIdCounter.incrementAndGet();
        if ((null == counter || counter.longValue() == 1)) {// 初始设置过期时间
            log.debug("【自动生成流水号】设置过期时间为7天!");
            entityIdCounter.expire(7, TimeUnit.DAYS);// 单位天
        }
        return counter;
    }

    /**
     * 判断key是否存在
     * @param key
     * @return
     */
    public boolean hasKey(String key){
       return redisTemplate.hasKey(key);
    }

    /**
     * 设置key和过期时间设置
     * @param key key值
     * @param value 内容
     * @param time 超时时长
     * @param timeUnit 超时时间单位
     * @return
     */
    public Boolean setIfAbsent(String key,String value,Long time,TimeUnit timeUnit){
        return redisTemplate.opsForValue().setIfAbsent(key,value,time,timeUnit);
    }

    /**
     * 删除key值
     * @param key
     */
    public void delRedisKey(String key){
        redisTemplate.delete(key);
    }

    /**
     * 根据key和前缀获取流水号
     * @param key
     * @param prefix
     * @return
     */
    public String getCxSequence(String key,String prefix){
        Long cxSequenceNo = getIncrementNumber(key + prefix);
        return CxScheduleUtils.getSequence(prefix,cxSequenceNo);
    }

    /**
     * 加载成型工序参数信息
     * @return
     */
    public Map<String,String> loadCxParamsMap(){
        Map<String,String> params=new HashMap<>();
        List<CxParamsDto> cxParamsDtoList=this.cxEngineParamsMapper.listParams(new CxParamsDto());
        if(StringUtils.isNotEmpty(cxParamsDtoList)){
            params=new HashMap<>();
            for (CxParamsDto cxParamsDto:cxParamsDtoList)
            {
                params.put(cxParamsDto.getParamCode(),cxParamsDto.getParamValue());
            }
        }
        return params;
    }

    /**
     * //计算单班硫化量
     * 单班硫化量=（单班时间-10）/(单条硫化时间+2) * 模数
     * @param target
     */
    public void calcSingleShiftLhQty(CxEngineScheduleResult target) {
        //加载成型参数
        Map<String,String>  cxParamsMap=loadCxParamsMap();
        Integer shiftTime=getShiftTime(cxParamsMap);
        Integer brushBagTime=getBrushBagTime(cxParamsMap);
        Double singleLhTime=getSingleLhTime(target.getSapCode(), target.getEmbryoCode());
        Double moldNum=1D;
        if(target.getLhMachineQty()==null){
            log.error("工单号："+target.getOrderNo()+"计算单班硫化量时，没有硫化机数量。按单模进行计算");
        }else{
            //Joran 2021-08-17 硫化机数调整为使用模数 不再乘以2
            //moldNum=target.getLhMachineQty() * 2D;//硫化机台 * 单硫化机模数为2
            moldNum=target.getLhMachineQty();//（2021-08-17属性 变更为使用模数）
        }
        Integer singleShiftLhQty=calcSingleMoldQty(moldNum,shiftTime,singleLhTime,brushBagTime);
        target.setSingleShiftLhQty(singleShiftLhQty);
        target.setLhSingleTireTime(singleLhTime);//设置单胎硫化时长
    }

    /**
     * 只查一次成型参数
     * @param target
     * @param cxParamsMap
     */
    public void calcSingleShiftLhQtyByCxParams(CxEngineScheduleResult target,Map<String,String>  cxParamsMap, Map<String,List<LhEngineTireConstructionInfo>> sapTireConstructionListMap){
        if(StringUtils.isEmpty(cxParamsMap)){
            cxParamsMap=loadCxParamsMap();
        }
        Integer shiftTime=getShiftTime(cxParamsMap);
        Integer brushBagTime=getBrushBagTime(cxParamsMap);
        Double singleLhTime=getSingleLhTimeByLhConstruction(target.getSapCode(),sapTireConstructionListMap);
        Double moldNum=1D;
        if(target.getLhMachineQty()!=null){
            moldNum=target.getLhMachineQty();//（2021-08-17属性 变更为使用模数）
        }
        Integer singleShiftLhQty=calcSingleMoldQty(moldNum,shiftTime,singleLhTime,brushBagTime);
        target.setSingleShiftLhQty(singleShiftLhQty);
        target.setLhSingleTireTime(singleLhTime);//设置单胎硫化时长
    }


    /**
     * 获取班次总时长=班次总时长-固定损耗时长
     * @return
     */
    private Integer getShiftTime(Map<String,String>  cxParamsMap) {
        if(StringUtils.isNotEmpty(cxParamsMap)&&cxParamsMap.containsKey(CxParamCodeConstants.CLASS_SHIFT_MAX_TIME)&&cxParamsMap.containsKey(CxParamCodeConstants.CLASS_LOSSRATE_TIME)){
            String classShiftTimeParams=cxParamsMap.get(CxParamCodeConstants.CLASS_SHIFT_MAX_TIME);
            Integer classShiftTime=null;
            if(StringUtils.isNotEmpty(classShiftTimeParams)){
                classShiftTime=Integer.valueOf(classShiftTimeParams);
            }
            Integer classShiftLossTime=null;
            String classShiftLossTimeParams=cxParamsMap.get(CxParamCodeConstants.CLASS_LOSSRATE_TIME);
            if(StringUtils.isNotEmpty(classShiftLossTimeParams)){
                classShiftLossTime=Integer.valueOf(classShiftLossTimeParams);
            }
            if(classShiftTime!=null&&classShiftLossTime!=null){
                return classShiftTime - classShiftLossTime;
            }
        }
        return 480-10;
    }

    /**
     * 获取刷囊时间
     * @return
     */
    public Integer getBrushBagTime(Map<String,String>  cxParamsMap){
        if(StringUtils.isNotEmpty(cxParamsMap)&&cxParamsMap.containsKey(CxParamCodeConstants.BRUSH_BAG_TIME)){
            String brushBagTimeParams=cxParamsMap.get(CxParamCodeConstants.BRUSH_BAG_TIME);
            if(StringUtils.isNotEmpty(brushBagTimeParams)){
                return Integer.valueOf(brushBagTimeParams);
            }
        }
        return 2;
    }

    /**
     * 传入模数计算单班硫化量
     * @param target
     * @param moldNum
     */
    public void calcSingleShiftLhQtyByMoldNum(CxEngineScheduleResult target,Double moldNum, Map<String,List<LhEngineTireConstructionInfo>> sapTireConstructionListMap) {
        //加载成型参数
        Map<String,String>  cxParamsMap=loadCxParamsMap();
        Integer shiftTime=getShiftTime(cxParamsMap);
        Integer brushBagTime=getBrushBagTime(cxParamsMap);
        Double singleLhTime=BigDecimal.ZERO.doubleValue();
        if(StringUtils.isEmpty(sapTireConstructionListMap)){
            singleLhTime=getSingleLhTime(target.getSapCode(), target.getEmbryoCode());
        }else{
            singleLhTime=getSingleLhTimeByLhConstruction(target.getSapCode(),sapTireConstructionListMap);
        }
        Integer singleShiftLhQty=calcSingleMoldQty(moldNum,shiftTime,singleLhTime,brushBagTime);
        log.debug("【计算单班硫化量】单模计算，SAP品号："+target.getSapCode()+";，胎胚代码："+target.getEmbryoCode()+",单条硫化时长："+singleLhTime+"，硫化机模数："+moldNum+"，计算可硫化单班硫化量="+singleShiftLhQty);
        target.setSingleShiftLhQty(singleShiftLhQty);
        target.setLhSingleTireTime(singleLhTime);//设置单胎硫化时长
    }



    /**
     * 计算单班单模产量
     * @param target
     */
    public Integer calcSingleMoldShiftLhQty(CxEngineScheduleResult target, Map<String,List<LhEngineTireConstructionInfo>> sapTireConstructionListMap) {
        //加载成型参数
        Map<String,String>  cxParamsMap=loadCxParamsMap();
        Integer shiftTime=getShiftTime(cxParamsMap);
        Integer brushBagTime=getBrushBagTime(cxParamsMap);
        Double singleLhTime=BigDecimal.ZERO.doubleValue();
        if(StringUtils.isEmpty(sapTireConstructionListMap)){
            singleLhTime=getSingleLhTime(target.getSapCode(), target.getEmbryoCode());
        }else{
            singleLhTime=getSingleLhTimeByLhConstruction(target.getSapCode(),sapTireConstructionListMap);
        }
        Double moldNum=1d;//单模
        Integer singleShiftLhQty=calcSingleMoldQty(moldNum,shiftTime,singleLhTime,brushBagTime);
        log.debug("【计算单班单模产量】SAP品号："+target.getSapCode()+";胎胚代码："+target.getEmbryoCode()+"--->单班单模产量："+singleShiftLhQty);
        target.setLhSingleTireTime(singleLhTime);//设置单胎硫化时长
        return singleShiftLhQty;
    }

    /**
     * 根据SPA品号和胎胚代码获取单条硫化时长
     * //单条硫化时间后续调整为从硫化施工信息中获取
     * @param sapCode
     * @param embryoCode
     * @return
     */
    private Double getSingleLhTime(String sapCode, String embryoCode) {
        if(StringUtils.isEmpty(sapCode)||StringUtils.isEmpty(embryoCode)){
            log.error("【获取单条硫化时间】通过SAP品号和胎胚代码获取单条硫化时长异常");
        }
        Double lhTime=lhEngineTireConstructionInfoService.getLhTireTimeBySapCode(sapCode,embryoCode);
        log.debug("【获取单条硫化时间】SAP品号："+sapCode+";胎胚代码："+embryoCode+"--->单条硫化时长="+lhTime);
        return lhTime;
    }

    /**
     * 计算最小硫化机需求数量
     * @return
     */
    public void calcLeastLhMachineQty(CxEngineScheduleResult target, CxPlanProductStatus cxPlanProductStatus,List<CxEngineSapSpecMoldUse> sapSpecMoldUseList,Map<String,List<LhEngineTireConstructionInfo>> sapTireConstructionListMap){
        Date scheduleDate=target.getScheduleDate();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(scheduleDate);
        int maxDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
        int scheduleDay=calendar.get(Calendar.DAY_OF_MONTH);
        CxEngineHolidaySetting holidayCondition=new CxEngineHolidaySetting();
        holidayCondition.setHolidayStartDate(scheduleDate);
        holidayCondition.setHolidayEndDate(CxScheduleUtils.getLastDayOfMonth(scheduleDate));
        int holidayCount=cxEngineHolidaySettingMapper.countOfHolidaySetting(holidayCondition);

       //剩余天数
       int remainDays=maxDay-scheduleDay-holidayCount+1;
       //总计划量
       int totalTaskQty=cxPlanProductStatus.getMonthPlanTotalQty();
       //每日最少计划量
       //int leastDayPlan=BigDecimal.valueOf(totalTaskQty/remainDays).setScale(2, RoundingMode.UP).intValue();
       //Joran 2022-01-03 计算值先不做处理，保证值太小无法处理问题
       BigDecimal leastDayPlanDic=BigDecimal.valueOf(totalTaskQty/remainDays).setScale(2, RoundingMode.UP);
       //单班单模产量
       int singleMoldQty=BigDecimal.ONE.intValue();
       boolean reCalcuate=true;
       if(StringUtils.isNotEmpty(sapSpecMoldUseList)){
            reCalcuate=useSapSpecMoldUse(target,sapSpecMoldUseList);
       }
        if(!reCalcuate){
            singleMoldQty=target.getSingleShiftLhQty();
        }else{
            singleMoldQty=calcSingleMoldShiftLhQty(target,sapTireConstructionListMap);
        }

       //计算需要的最少模数(向上取整模)
       //Double leastMoldNum=BigDecimal.valueOf((double)leastDayPlan/(singleMoldQty * 3)).setScale(3,RoundingMode.UP).doubleValue();
       //Joran 2022-01-03 计算值先不做处理，保证值太小无法处理问题
       BigDecimal leastMoldNumBig=leastDayPlanDic.divide(BigDecimal.valueOf(singleMoldQty * 3),5,RoundingMode.UP);
       //Joran 2021-12-31 当最小模数大于0小于1的时候按最少1个模来算start
       Double leastMoldNum=BigDecimal.ONE.doubleValue();
       if(leastMoldNumBig.compareTo(BigDecimal.ONE)>0){
           leastMoldNum=leastMoldNumBig.setScale(3,RoundingMode.UP).doubleValue();
       }
        //硫化机台最少数量
        //Integer leastLhMachineQty=BigDecimal.valueOf(leastMoldNum/2).setScale(0,RoundingMode.UP).intValue();
       BigDecimal leastLhMachineQtyDic=leastMoldNumBig.divide(BigDecimal.valueOf(2),5,RoundingMode.UP);
       Integer leastMachineQty=BigDecimal.ONE.intValue();
        if(leastLhMachineQtyDic.compareTo(BigDecimal.ONE)>0){
            leastMachineQty=leastLhMachineQtyDic.intValue();
        }
        target.setMinimumLhMachineReqQty(leastMachineQty);//设置最小硫化机需求数
        //2022-01-03 最小硫化机比对数
        Double leastLhMachineComQty= CxEngineConstants.MIN_LH_MACHINE_COM_QTY.doubleValue();
        if(leastLhMachineQtyDic.compareTo(CxEngineConstants.MIN_LH_MACHINE_COM_QTY)>0){
            leastLhMachineComQty=leastLhMachineQtyDic.setScale(1,RoundingMode.UP).doubleValue();
        }
        //2021-12-27 添加最小硫化机比对数
        target.setMinimumLhMachineComQty(leastLhMachineComQty);//2021-12-27 设置最小硫化机比对数

        //Joran 2021-12-31 当最小模数大于0小于1的时候按最少1个模来算end
       if(reCalcuate){
           calcSingleShiftLhQtyByMoldNum(target,leastMoldNum,sapTireConstructionListMap);
       }

    }

    /**
     *  根据sap进行初始模数使用
     * @param target
     * @param sapSpecMoldUseList
     * @return
     */
    private boolean useSapSpecMoldUse(CxEngineScheduleResult target, List<CxEngineSapSpecMoldUse> sapSpecMoldUseList) {
        boolean unSet=true;
        //1.根据sap品号+胎胚代码进行分组
        Map<String,CxEngineSapSpecMoldUse> sapMoldUseListMap= sapSpecMoldUseList.stream().collect(
                    Collectors.toMap(
                            cxEngineSapSpecMoldUse -> GenerageMapKeyUtils.createMapKey(
                                    cxEngineSapSpecMoldUse.getSapCode(),
                                    cxEngineSapSpecMoldUse.getEmbryoCode()
                            ),cxEngineSapSpecMoldUse ->cxEngineSapSpecMoldUse
                    )
        );
        String emptyEmbryoCodeKey=GenerageMapKeyUtils.createMapKey(target.getSapCode(),null);
        String sapEmbryoCodeKey=GenerageMapKeyUtils.createMapKey(target.getSapCode(),target.getEmbryoCode());
        Double moldNum=null;
        if(sapMoldUseListMap.containsKey(sapEmbryoCodeKey)){
            CxEngineSapSpecMoldUse cxEngineSapSpecMoldUse=sapMoldUseListMap.get(sapEmbryoCodeKey);
            //Joran 2022-03-16 新投产的规格将模数进行设置可用模数
            if(target.getNewSpecFlag()){
                target.setLhMachineQty(BigDecimal.valueOf(cxEngineSapSpecMoldUse.getMoldNum()).doubleValue());
            }
            moldNum=BigDecimal.valueOf(cxEngineSapSpecMoldUse.getMoldNum()).doubleValue();
            unSet=false;
        }else if(sapMoldUseListMap.containsKey(emptyEmbryoCodeKey)){
            CxEngineSapSpecMoldUse cxEngineSapSpecMoldUse=sapMoldUseListMap.get(emptyEmbryoCodeKey);
            //Joran 2022-03-16 新投产的规格将模数进行设置可用模数
            if(target.getNewSpecFlag()){
                target.setLhMachineQty(BigDecimal.valueOf(cxEngineSapSpecMoldUse.getMoldNum()).doubleValue());
            }
            moldNum=BigDecimal.valueOf(cxEngineSapSpecMoldUse.getMoldNum()).doubleValue();
            unSet=false;
        }
        //有设置了默认投产班数
        if(!unSet){
            calcSingleShiftLhQtyByMoldNum(target,moldNum,null);
        }

        return unSet;
    }

    /**
     * 计算最小硫化机需求数量
     * @return
     */
    public void calcLeastLhMachineQtyByMonthRemainQty(CxEngineScheduleResult target,Integer monthRemainQty, Map<String,List<LhEngineTireConstructionInfo>> sapTireConstructionListMap){
        Date scheduleDate=target.getScheduleDate();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(scheduleDate);
        int maxDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
        int scheduleDay=calendar.get(Calendar.DAY_OF_MONTH);
        CxEngineHolidaySetting holidayCondition=new CxEngineHolidaySetting();
        holidayCondition.setHolidayStartDate(scheduleDate);
        holidayCondition.setHolidayEndDate(CxScheduleUtils.getLastDayOfMonth(scheduleDate));
        int holidayCount=cxEngineHolidaySettingMapper.countOfHolidaySetting(holidayCondition);

        //剩余天数
        //Joran 2021-09-14 当月份最后一天时则剩余天数最少设置为1天start
        int remainDays;
        if(maxDay==scheduleDay){
            remainDays=1;
        }else{
            remainDays=maxDay-scheduleDay-holidayCount+1;
        }
        if(remainDays<=0){
            remainDays=1;
        }
        //Joran 2021-09-14 当月份最后一天时则剩余天数最少设置为1天end
        //月度剩余量
        int totalTaskQty=monthRemainQty;
        BigDecimal leastDayPlanDic=BigDecimal.valueOf(totalTaskQty/remainDays).setScale(2, RoundingMode.UP);
        //每日最少计划量
        /*int leastDayPlan=BigDecimal.valueOf(totalTaskQty/remainDays).setScale(2, RoundingMode.UP).intValue();
        //单班单模产量
        int singleMoldQty=calcSingleMoldShiftLhQty(target);

        //计算需要的最少模数(向上取整模)
        Double leastMoldNum=BigDecimal.valueOf((double)leastDayPlan/(singleMoldQty * 3 )).setScale(3,RoundingMode.UP).doubleValue();

        //2021-12-27 添加最小硫化机比对数
        Double leastLhMachineComQty= BigDecimal.valueOf(leastMoldNum/2).setScale(1,RoundingMode.UP).doubleValue();

        //硫化机台最少数量
        Integer leastLhMachineQty=BigDecimal.valueOf(leastMoldNum/2).setScale(0,RoundingMode.UP).intValue();
        target.setMinimumLhMachineReqQty(leastLhMachineQty);//设置最小硫化机需求数
        target.setMinimumLhMachineComQty(leastLhMachineComQty);//2021-12-27 设置最小硫化机比对数*/
        //Joran 2022-01-03 计算值先不做处理，保证值太小无法处理问题
        int singleMoldQty=calcSingleMoldShiftLhQty(target,sapTireConstructionListMap);
        //一天至少模具数=每日至少的计划量/(单班单模产量*3班)
        BigDecimal leastMoldNumBig=leastDayPlanDic.divide(BigDecimal.valueOf(singleMoldQty * 3),5,RoundingMode.UP);
        //Joran 2021-12-31 当最小模数大于0小于1的时候按最少1个模来算start
        Double leastMoldNum=BigDecimal.ONE.doubleValue();
        if(leastMoldNumBig.compareTo(BigDecimal.ONE)>0){
            leastMoldNum=leastMoldNumBig.setScale(3,RoundingMode.UP).doubleValue();
        }
        //硫化机台最少数量 = 一天至少模具数/2（一台硫化机有2个模）
        BigDecimal leastLhMachineQtyDic=leastMoldNumBig.divide(BigDecimal.valueOf(2),5,RoundingMode.UP);
        Integer leastMachineQty=BigDecimal.ONE.intValue();
        if(leastLhMachineQtyDic.compareTo(BigDecimal.ONE)>0){
            leastMachineQty=leastLhMachineQtyDic.intValue();
        }
        target.setMinimumLhMachineReqQty(leastMachineQty);//设置最小硫化机需求数
        //2022-01-03 最小硫化机比对数
        Double leastLhMachineComQty= CxEngineConstants.MIN_LH_MACHINE_COM_QTY.doubleValue();
        if(leastLhMachineQtyDic.compareTo(CxEngineConstants.MIN_LH_MACHINE_COM_QTY)>0){
            leastLhMachineComQty=leastLhMachineQtyDic.setScale(1,RoundingMode.UP).doubleValue();
        }
        //2021-12-27 添加最小硫化机比对数
        target.setMinimumLhMachineComQty(leastLhMachineComQty);//2021-12-27 设置最小硫化机比对数
        if(target.getLhMachineQty()==null||target.getLhMachineQty()==0){
            calcSingleShiftLhQtyByMoldNum(target,leastMoldNum,sapTireConstructionListMap);
        }
    }

    /**
     * 获取成型收尾提示量设置信息
     * @return
     */
    public Integer getCloseOutTipSetting(Map<String,String>  cxParamsMap) {
        Integer closeOutTipQty=null;
        if(StringUtils.isNotEmpty(cxParamsMap)&&cxParamsMap.containsKey(CxParamCodeConstants.CLOSE_OUT_TIP_QTY)){
            String closeOutTipQtyParam=cxParamsMap.get(CxParamCodeConstants.CLOSE_OUT_TIP_QTY);
            if(StringUtils.isEmpty(closeOutTipQtyParam)){
                log.error("获取收尾提示设置量异常");
                throw new CxScheduleEngineException(I18nUtil.getMessage("cx.engine.common.cache.close.out.param.exception"));
            }
            closeOutTipQty=Integer.valueOf(closeOutTipQtyParam);
        }else{
            throw new CxScheduleEngineException(I18nUtil.getMessage("cx.engine.common.cache.close.out.param.exception"));
        }
        return closeOutTipQty;
    }

    /**
     * 更新库存信息
     * @param cxEngineScheduleResultList
     * @param scheduleDate
     */
    public void updateLastDayTaskStock(List<CxEngineScheduleResult> cxEngineScheduleResultList, Date scheduleDate,StringBuilder logDetail,boolean isLastDate) {
        Date lastDate=scheduleDate;
        if(!isLastDate){
            lastDate=DateUtils.addDays(scheduleDate,-1);
        }
        //获取排程日期前一天日期
        String lastDateStr= DateUtils.parseDateToStr("yyyyMMdd",lastDate);
        CxEngineStock stockCondition=new CxEngineStock();
        stockCondition.setStockDateStr(lastDateStr);
        List<CxEngineStock> stockList =this.cxEngineStockMapper.selectCxEngineStockList(stockCondition);
        Map<String,Integer> stockMap=null;
        Map<String,String> monthStockMap=new HashMap<>();
        if(StringUtils.isNotEmpty(stockList)){
            stockMap=stockList.stream().collect(Collectors.toMap(cxEngineStock -> GenerageMapKeyUtils.createMapKey(cxEngineStock.getEmbryoCode(),cxEngineStock.getBomDataVersion()),CxEngineStock::getStockRealNum));
        }else{ //没有库存信息时则默认都为0
            for(CxEngineScheduleResult cxEngineScheduleResult:cxEngineScheduleResultList){
                cxEngineScheduleResult.setTotalStock(0);//没有库存，默认为0
                cxEngineScheduleResult.setCalcTotalStock(0);//没有库存，默认为0
            }
            logDetail.append("没有库存信息，默认库存初始化为0").append(division);
        }

        //Joran 2021-10-13 填充月结库存信息start
        CxEngineMonthStock monthStock=new CxEngineMonthStock();
        monthStock.setStockMonthStr(DateUtils.parseDateToStr("yyyyMM",DateUtils.addMonths(lastDate,-1)));
        List<CxEngineMonthStock> monthStockList=this.cxEngineStockMapper.selectCxEngineMonthStockList(monthStock);
        if(StringUtils.isNotEmpty(monthStockList)){
            monthStockMap=monthStockList.stream().collect(Collectors.toMap(cxEngineStock -> GenerageMapKeyUtils.createMapKey(cxEngineStock.getEmbryoCode(),cxEngineStock.getBomDataVersion()),CxEngineMonthStock::getStockNum));
            if(StringUtils.isNotEmpty(monthStockMap)){
                for(CxEngineScheduleResult cxEngineScheduleResult:cxEngineScheduleResultList){
                    String embryoCode=cxEngineScheduleResult.getEmbryoCode();
                    String bomDataVersion=cxEngineScheduleResult.getBomDataVersion();
                    String key= GenerageMapKeyUtils.createMapKey(embryoCode,bomDataVersion);
                    if(monthStockMap.containsKey(key)){
                        String monthStockNum=monthStockMap.get(key);
                        cxEngineScheduleResult.setMonthStock(Integer.valueOf(monthStockNum));
                    }else{
                        cxEngineScheduleResult.setMonthStock(0);
                    }
                }
            }
        }
        //Joran 2021-10-13 填充月结库存信息end
        if(StringUtils.isNotEmpty(stockMap)){
            //计算胎胚模数
            Map<String,Integer> embryoCodeMoldNum=new HashMap<>();
            //汇总同胎胚记录数
            Map<String,List<CxEngineScheduleResult>> embryoCodeListMap=new HashMap<>();
            //遍历汇总同胎胚总模数、总记录数start
            calcEmbryoCodeMap(embryoCodeMoldNum,embryoCodeListMap,cxEngineScheduleResultList);
            //遍历汇总同胎胚总模数、总记录数end

            if(StringUtils.isNotEmpty(embryoCodeListMap)){
                for(Map.Entry<String,List<CxEngineScheduleResult>> entry:embryoCodeListMap.entrySet()){
                    String key=entry.getKey();//胎胚+版本
                    Integer totalMoldNum=embryoCodeMoldNum.get(key);//拿到总模数
                    List<CxEngineScheduleResult> embryoCodeList=entry.getValue();//结果集
                    if (embryoCodeList.size()>1){
                        //遍历按比例分库存
                        int index=0;
                        int calcTotalStock=0;//参与计算的库存
                        for(CxEngineScheduleResult embryoCodeResult:embryoCodeList){
                            String embryoCode=embryoCodeResult.getEmbryoCode();

                            //成型未投产的，不参与分配 by pancd+ 20230830
                            if (CxEngineConstants.TO_PRODUCT_NO.equals(embryoCodeResult.getToProduct())){
                                log.debug("【模数比例库存设置】胎胚代码："+embryoCode+",比例计算库存："+0+",原因：未投产");
                                embryoCodeResult.setCalcTotalStock(0);
                                if(stockMap.containsKey(key)){
                                    embryoCodeResult.setTotalStock(stockMap.get(key));
                                }else{
                                    embryoCodeResult.setTotalStock(0);
                                }
                                continue;
                            }

                            if(stockMap.containsKey(key)){
                                Integer stockCount=stockMap.get(key);
                                Integer calcStock=0;
                                embryoCodeResult.setTotalStock(stockCount);
                                if(index==embryoCodeList.size()-1){
                                    calcStock=stockCount-calcTotalStock;
                                }else{
                                    Integer calcMoldNum=embryoCodeResult.getCalcMoldNum();//计算模数
                                    totalMoldNum = totalMoldNum == null ? calcMoldNum:totalMoldNum;//总模数为空，则等于计算模数 pancd+ 20230909
                                    calcStock=BigDecimal.valueOf((double)calcMoldNum/totalMoldNum * stockCount).setScale(0, RoundingMode.UP).intValue();
                                    calcTotalStock+=calcStock;
                                }
                                log.debug("【模数比例库存设置】胎胚代码："+embryoCode+",总库存："+stockCount+",比例计算库存："+calcStock);
                                logDetail.append("【模数比例库存设置】胎胚代码："+embryoCode+",总库存："+stockCount+",比例计算库存："+calcStock).append(division);
                                embryoCodeResult.setCalcTotalStock(calcStock);
                            }else{
                                //Joran 2021-11-04 任务项没有库存数据时设值为0
                                embryoCodeResult.setTotalStock(0);
                                embryoCodeResult.setCalcTotalStock(0);//没有库存，默认为0
                                log.debug("【模数比例库存设置】胎胚代码："+embryoCode+",总库存："+0+",比例计算库存："+0+",原因：未找到库存信息");
                                logDetail.append("【模数比例库存设置】胎胚代码："+embryoCode+",总库存："+0+",比例计算库存："+0+",原因：未找到库存信息").append(division);
                            }
                            index++;
                            String title="【"+embryoCode+"设置库存】:";
                            logDetail.append(title).append(division).append("库存数：").append(stockMap.get(embryoCode)).append(division);
                        }

                    }else{
                        CxEngineScheduleResult embryoCodeResult=embryoCodeList.get(0);
                        Integer totalStock=0;
                        embryoCodeResult.setTotalStock(0);//没有库存，默认为0
                        embryoCodeResult.setCalcTotalStock(0);//没有库存，默认为0
                        String embryoCode=embryoCodeResult.getEmbryoCode();
                        if(stockMap.containsKey(key)){
                            totalStock=stockMap.get(key);
                            embryoCodeResult.setTotalStock(totalStock);
                            embryoCodeResult.setCalcTotalStock(totalStock);
                        }
                        log.debug("【模数比例库存设置】胎胚代码："+embryoCode+",总库存："+totalStock+",比例计算库存："+totalStock);
                        logDetail.append("【模数比例库存设置】胎胚代码："+embryoCode+",总库存："+totalStock+",比例计算库存："+totalStock).append(division);
                        String title="【"+embryoCode+"设置库存】:";
                        logDetail.append(title).append(division).append("库存数：").append(stockMap.get(key)).append(division).append("【模数比例库存设置】:").append("总库存：").append(totalStock)
                                .append(",比例计算库存：").append(totalStock).append(division);

                    }
                }

            }
        }
    }

    /**
     * 组装同胎胚模数和记录数信息
     * @param embryoCodeMoldNum
     * @param embryoCodeListMap
     * @param lastDayTaskList
     */
    private void calcEmbryoCodeMap(Map<String, Integer> embryoCodeMoldNum, Map<String, List<CxEngineScheduleResult>> embryoCodeListMap, List<CxEngineScheduleResult> lastDayTaskList) {
        List<CxEngineScheduleResult> cxEngineScheduleResultList=null;
        for(CxEngineScheduleResult cxEngineScheduleResult:lastDayTaskList){
            cxEngineScheduleResultList=new ArrayList<>();
            Integer calcTotalMoldNum=0;
            String embryoCode=cxEngineScheduleResult.getEmbryoCode();
            String bomDataVersion=cxEngineScheduleResult.getBomDataVersion();
            String key=GenerageMapKeyUtils.createMapKey(embryoCode,bomDataVersion);

            if(embryoCodeMoldNum.containsKey(key)){
                calcTotalMoldNum=embryoCodeMoldNum.get(key);
            }
            if(embryoCodeListMap.containsKey(key)){
                cxEngineScheduleResultList=embryoCodeListMap.get(key);
            }

            Integer moldNum=1;
            Double lhMachineQty=cxEngineScheduleResult.getLhMachineQty();
            //如果没有选择硫化机则按1个模进行计算
            if(lhMachineQty!=null&&lhMachineQty>0){
                moldNum=BigDecimal.valueOf(lhMachineQty).intValue();
            }
            cxEngineScheduleResult.setCalcMoldNum(moldNum);//保存计算的模数

            if (CxEngineConstants.TO_PRODUCT_YES.equals(cxEngineScheduleResult.getToProduct())){
                //投产的，才能分配模具 pancd+ 20230909
                calcTotalMoldNum+=moldNum;
                embryoCodeMoldNum.put(key,calcTotalMoldNum);
            }
            cxEngineScheduleResultList.add(cxEngineScheduleResult);
            embryoCodeListMap.put(key,cxEngineScheduleResultList);
        }
    }

    /**
     * 根据机台类型获取胎胚代码前缀信息
     * @param machineType
     * @return
     */
    public String getEmbryoCodePrefix(String machineType,Map<String,String> cxParamsMap){
        String prefix="";
        //Joran 2021-09-09 功能优化，若传入的成型参数集合为空则进行第一次数据加载，若还是空则表示数据没有维护
        if(StringUtils.isEmpty(cxParamsMap)){
            cxParamsMap=loadCxParamsMap();
        }
        if(StringUtils.isEmpty(cxParamsMap)){
            throw new IllegalArgumentException(I18nUtil.getMessage("cx.engine.auto.param.exception.error"));
        }
        if(CxEngineConstants.MACHINE_TYPE_ONCE.equals(machineType)){
            prefix=cxParamsMap.get(CxParamCodeConstants.ONCE_EMBRYOCODE_PREFIX);
        }else if(CxEngineConstants.MACHINE_TYPE_TWICE.equals(machineType)){
            prefix=cxParamsMap.get(CxParamCodeConstants.TWICE_EMBRYOCODE_PREFIX);
        }else{
            throw new IllegalArgumentException(I18nUtil.getMessage("cx.engine.auto.param.exception.error"));
        }

        if(StringUtils.isEmpty(prefix)){
            throw new IllegalArgumentException(I18nUtil.getMessage("cx.engine.auto.param.exception.error"));
        }
        return prefix;
    }

    /**
     * 获取最大可硫化班次
     * @param cxEngineScheduleResult
     * @param specDimension
     * @param avgAvailableLhShift
     * @param scheduleLimitMap
     * @return
     */
    public Double maxLhShiftCount(CxEngineScheduleResult cxEngineScheduleResult,String specDimension,Double avgAvailableLhShift,Map<String, List<CxEngineScheduleLimit>> scheduleLimitMap){
        String key=cxEngineScheduleResult.getCxMachineCode();
        if(cxEngineScheduleResult.getMaximumClassQty()!=null){
            return Double.valueOf(cxEngineScheduleResult.getMaximumClassQty());
        }
        //Joran 2021-12-16机台设置投产班次，如果有设置直接优先取机台投产班次start
       /* Map<String,Double> machineProductShiftMap=cxEngineGroupMachineListService.getCxMachineProudctShift();
        if(StringUtils.isNotEmpty(machineProductShiftMap)&&machineProductShiftMap.containsKey(key)){
            Double machineProductShifts=machineProductShiftMap.get(key);
            log.debug("【获取机台设定可投产班次】，成型机台编号："+key+"寸口："+specDimension+",可投产班次数："+machineProductShifts);
            //自动排程设置最大班数
            cxEngineScheduleResult.setMaximumClassQty(BigDecimal.valueOf(machineProductShifts).doubleValue());
            return  machineProductShifts;
        }*/
        //Joran 2021-12-16机台设置投产班次，如果有设置直接优先取机台投产班次end

        Map<String,String> cxParamsMap =loadCxParamsMap();
        //没有设置则取默认工序参数可硫化最大班次
        String defaultLhClassShifts = cxParamsMap.get(CxParamCodeConstants.DEFAULT_LH_CLASS_SHIFTS);
        if (cxEngineScheduleResult == null || cxEngineScheduleResult.getLhMachineQty() == null || cxEngineScheduleResult.getLhMachineQty() >= 2 || cxEngineScheduleResult.getLhMachineQty() == 0) {
            //没有模数也按照双模计算
            defaultLhClassShifts = cxParamsMap.get(CxParamCodeConstants.DEFAULT_DOUBLE_LH_CLASS_SHIFTS);
        }
        if(StringUtils.isNotEmpty(scheduleLimitMap) && scheduleLimitMap.containsKey(key)){
            List<CxEngineScheduleLimit> scheduleLimitList=scheduleLimitMap.get(key);
            if(StringUtils.isEmpty(scheduleLimitList)){
                log.debug("【获取最大可硫化班次设置参数】默认值,成型机台编号："+key+"寸口："+specDimension+",最大可硫化班次："+defaultLhClassShifts);
                return Double.valueOf(defaultLhClassShifts);
            }
            //遍历判断当前平均可硫化班次在什么范围区间，匹配到的直接取
            for(CxEngineScheduleLimit cxEngineScheduleLimit:scheduleLimitList){
                Double tireAvgLhStockMinimun=cxEngineScheduleLimit.getTireAvgLhStockMinimun();
                Double tireAveLhStockMaximun=cxEngineScheduleLimit.getTireAveLhStockMaximun();
                Double specDimensionDouble=cxEngineScheduleLimit.getSpecDimension();
                Double specDimensionInput=null;
                if(StringUtils.isNotEmpty(specDimension)){
                    specDimensionInput=Double.valueOf(specDimension);
                }
                if(specDimensionDouble.equals(specDimensionInput)&&avgAvailableLhShift>=tireAvgLhStockMinimun&&avgAvailableLhShift<=tireAveLhStockMaximun){
                    log.debug("【获取最大可硫化班次设置参数】匹配设置获取到最大班次,成型机台编号："+key+"寸口："+specDimension+",最大可硫化班次："+cxEngineScheduleLimit.getMaxLhClass());
                    cxEngineScheduleResult.setMaximumClassQty(BigDecimal.valueOf(cxEngineScheduleLimit.getMaxLhClass()).doubleValue());
                    return  cxEngineScheduleLimit.getMaxLhClass();
                }
            }
        }
        log.debug("【获取最大可硫化班次设置参数】未匹配到设置，取默认值,成型机台编号："+key+"寸口："+specDimension+",最大可硫化班次："+defaultLhClassShifts);
        //自动排程设置最大班数
        cxEngineScheduleResult.setMaximumClassQty(Double.valueOf(defaultLhClassShifts));
        return Double.valueOf(defaultLhClassShifts);
    }

    /**
     * 成型排程结果表进行外胎规格设定
     * @param cxEngineScheduleResult
     */
    public void setSpecDescBySapCode(CxEngineScheduleResult cxEngineScheduleResult,Map<String,List<LhEngineTireConstructionInfo>> sapTireConstructionListMap){
        if(cxEngineScheduleResult==null){
            log.error("【设置规格型号】,入参错误!");
            return;
        }
        String sapCode =cxEngineScheduleResult.getSapCode();
        String embryoCode=cxEngineScheduleResult.getEmbryoCode();
        if(StringUtils.isEmpty(sapCode)||StringUtils.isEmpty(embryoCode)){
            log.error("【设置规格型号】通过SAP品号和胎胚代码获取规格型号异常");
        }
        LhEngineTireConstructionInfo lhEngineTireConstructionInfo=null;
        if(StringUtils.isNotEmpty(sapTireConstructionListMap)&&sapTireConstructionListMap.containsKey(sapCode)){
            List<LhEngineTireConstructionInfo> lhEngineTireConstructionInfoList=sapTireConstructionListMap.get(sapCode);
            if(StringUtils.isEmpty(lhEngineTireConstructionInfoList)){
                log.error("【设置规格型号】,未找到硫化施工信息!");
                return;
            }
            lhEngineTireConstructionInfo=lhEngineTireConstructionInfoList.get(0);
        }else{
            lhEngineTireConstructionInfo=lhEngineTireConstructionInfoService.getLhConstructionInfoByCondition(sapCode,embryoCode);
        }

        if(lhEngineTireConstructionInfo==null){
            log.error("【设置规格型号】,未找到硫化施工信息!");
            return;
        }
        cxEngineScheduleResult.setSpecDesc(lhEngineTireConstructionInfo.getSpecDesc());
        log.debug("【设置规格型号】SAP品号："+sapCode+";胎胚代码："+embryoCode+"--->规格描述="+cxEngineScheduleResult.getSpecDesc());
    }

    /**
     * 成型导入硫化收尾状态标记
     * @param successList
     * @param monthPlanApsVersion
     */
    public void cxScheduleResultLhTaskTypeCloseOut(List<CxEngineScheduleResult> successList,String monthPlanApsVersion){
        Map<String, CxEngineMonthPlanSurplus> cxEngineMonthPlanSurplusMap=cxEngineMonthPlanSurplusService.listCxMonthPlanSurplusByMonthPlanApsVersion(monthPlanApsVersion);
        if(StringUtils.isNotEmpty(cxEngineMonthPlanSurplusMap)){
            for(CxEngineScheduleResult cxScheduleTask:successList){
                String sapCode=cxScheduleTask.getSapCode();
                if(cxEngineMonthPlanSurplusMap.containsKey(sapCode)){
                    CxEngineMonthPlanSurplus cxEngineMonthPlanSurplus=cxEngineMonthPlanSurplusMap.get(sapCode);
                    if(cxEngineMonthPlanSurplus.getMonthRemainQty()<=0){
                        cxScheduleTask.setTaskType(CxEngineConstants.TASK_CLOSE_OUT);//硫化状态为收尾
                    }
                }else{
                    cxScheduleTask.setTaskType(CxEngineConstants.TASK_CLOSE_OUT);//硫化状态为收尾
                }
            }
        }
    }

    /**
     * 获取月度剩余量不进行安排投产设定值
     * @return 设定值
     */
    public Integer getUnProductMonthRemainQty(int monthPlanQty,Map<String,String>  cxParamsMap) {
        BigDecimal limitUndoQty= BigDecimal.ZERO;
        if(StringUtils.isEmpty(cxParamsMap)){
            cxParamsMap=loadCxParamsMap();
        }
        if(StringUtils.isNotEmpty(cxParamsMap)&&cxParamsMap.containsKey(CxParamCodeConstants.MONTH_PLAN_NO_SCHEDULE_VALUE)){
            String noScheduleQtyParam=cxParamsMap.get(CxParamCodeConstants.MONTH_PLAN_NO_SCHEDULE_VALUE);
            if(StringUtils.isEmpty(noScheduleQtyParam)){
                log.error("获取月度计划量不排产设定值失败");
                throw new CxScheduleEngineException(I18nUtil.getMessage("cx.engine.common.month.plan.qty.noSchedule.param.exception"));
            }
            int noScheduleQtyQty=Integer.valueOf(noScheduleQtyParam);
            //若月度计划量>=月度计划量不排产设定值(参数：MONTH_PLAN_NO_SCHEDULE_VALUE)
            //则按月度计划不排产比例，设置不排产数量
            if (monthPlanQty>=noScheduleQtyQty){
                //==============================================
                if(StringUtils.isNotEmpty(cxParamsMap)&&cxParamsMap.containsKey(CxParamCodeConstants.MONTH_REMAIN_QTY_MIN_PERCENT)){
                    String limitUndoPercentParam=cxParamsMap.get(CxParamCodeConstants.MONTH_REMAIN_QTY_MIN_PERCENT);
                    if(StringUtils.isEmpty(limitUndoPercentParam)){
                        log.error("获取剩余量不排产比例设定（%）失败");
                        throw new CxScheduleEngineException(I18nUtil.getMessage("cx.engine.common.month.remain.qty.limit.param.exception"));
                    }
                    BigDecimal limitUndoPercent=new BigDecimal(limitUndoPercentParam);
                    limitUndoPercent = limitUndoPercent.divide(BigDecimal.valueOf(100));
                    //不排产量= 月度计划*剩余量不排产比例
                    limitUndoQty =  BigDecimal.valueOf(monthPlanQty).multiply(limitUndoPercent).setScale(0,RoundingMode.UP);
                    return limitUndoQty.intValue();
                }else{
                    throw new CxScheduleEngineException(I18nUtil.getMessage("cx.engine.common.month.remain.qty.limit.param.exception"));
                }
                //==============================================
            }
        }

        return 0;
    }

    /**
     * 获取月度剩余量不进行安排投产设定值
     * @return 设定值
     */
    public String getEmbryoCodePrefix(Map<String,String>  cxParamsMap,String prefixCode) {
        String  prefix="";
        if(StringUtils.isEmpty(cxParamsMap)){
            cxParamsMap=loadCxParamsMap();
        }
        if(StringUtils.isNotEmpty(cxParamsMap)&&cxParamsMap.containsKey(prefixCode)){
            prefix=cxParamsMap.get(prefixCode);
        }

        return prefix;
    }

    /**
     * 获取前一天成型排程计划列表
     * @param scheduleDate 排程日期
     * @param cxBatchNo  成型批次号
     * @param cxMachineCode 成型机台编号
     * @param inertLogFlag  是否插入日志
     * @return
     */
    public List<CxEngineScheduleResult> getLastPlanResultList(Date scheduleDate,String cxBatchNo, String cxMachineCode,boolean inertLogFlag,boolean isLastDate) {
        Date lastDate=scheduleDate;
        if(!isLastDate){
            lastDate=DateUtils.addDays(scheduleDate,-1);
        }
        CxEngineScheduleResult condition=new CxEngineScheduleResult();
        condition.setCxScheduleDate(DateUtils.parseDateToStr("yyyyMMdd",lastDate));
        //若有成型机台则根据机台编号加载机台前一天任务计划
        if(StringUtils.isNotEmpty(cxMachineCode)){
            condition.setCxMachineCode(cxMachineCode);
        }
        List<CxEngineScheduleResult> lastDayResultList=cxScheduleEngineMapper.selectCxScheduleResultList(condition);
        if(StringUtils.isEmpty(lastDayResultList)){
            log.debug("未加载到前一天机台排程结果数据,{}",DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS,lastDate));
            String errorMsg=I18nUtil.getMessage("cx.engine.auto.lastDay.schedule.empty.error");
            if(inertLogFlag){
                cxEngineAutoScheduleRecordService.generagAutoScheduleRecord(scheduleDate,null,cxBatchNo, CxEngineConstants.AUTO_SCHEDULE_STATUS_FAILE,errorMsg);
            }
            throw new CxScheduleEngineException(errorMsg);
        }
        return lastDayResultList;
    }

    /**
     * 获取前一天成型排程计划列表
     * @param scheduleDate 排程日期
     * @return
     */
    public List<CxEngineScheduleResult> listScheduleTaskListByScheduleDate(Date scheduleDate) {
        Date lastDate=scheduleDate;
        CxEngineScheduleResult condition=new CxEngineScheduleResult();
        condition.setCxScheduleDate(DateUtils.parseDateToStr("yyyyMMdd",lastDate));
        List<CxEngineScheduleResult> lastDayResultList=cxScheduleEngineMapper.selectCxScheduleResultWithCloseOutList(condition);
        return lastDayResultList;
    }

    /**
     * 自动排程进行硫化收尾、成型完成量刷新
     * @param monthPlanApsVersion
     * @param scheduleDate
     */
    public void lhTaskCloseOut(String monthPlanApsVersion, Date scheduleDate,boolean isLastDate) {
        Date lastDate=scheduleDate;
        if(!isLastDate){
            lastDate=DateUtils.addDays(scheduleDate,-1);
        }
        //获取生产排程版本
        MdmMonthPlanMain planVersion=mdmMonthPlanMainService.getValidPlanMainVersion(lastDate);
        CxEngineScheduleResult condition=new CxEngineScheduleResult();
        //成型状态收尾
        condition.setProductionStatus(CxEngineConstants.PRODUCTION_STATUS_CLOSE_OUT);
        if(planVersion!=null&&!monthPlanApsVersion.equals(planVersion.getMonthPlanApsVersion())){
            //根据传入的版本进行获取 成型状态收尾硫化状态未收尾的成型排程
            monthPlanApsVersion=planVersion.getMonthPlanApsVersion();
        }
        condition.setApsMonthVersion(monthPlanApsVersion);
        //不包含前一天的数据
        condition.setCxScheduleDate(DateUtils.parseDateToStr("yyyyMMdd",lastDate));
        List<CxEngineScheduleResult> needCloseOutList=cxScheduleEngineMapper.selectCxCloseOutScheduleResultList(condition);
        if(StringUtils.isNotEmpty(needCloseOutList)){
            closeOutRemove(monthPlanApsVersion,needCloseOutList,null,false);
        }
    }

    /**
     *  剔除掉收尾规格列表（并进行收尾提示标记）
     *  2021-06-23 跟项目经理确认，若第三班有安排收尾，
     *  在次日排计划，默认会严格按照计划执行收尾。
     * @Author Joran.Zhang
     * @Description
     * @Date 2021/6/23 14:36
     * @param lastDayTaskList 前一天排程
     * @return
     */
    public void closeOutRemove(String monthPlanApsVersion,List<CxEngineScheduleResult> lastDayTaskList,Map<String,Integer> embryoCodeMap,Boolean isSupple) {
        if(StringUtils.isEmpty(lastDayTaskList)){
            throw new CxScheduleEngineException(I18nUtil.getMessage("cx.engine.auto.copy.lastDay.schedule.error"));
        }
        List<CxEngineScheduleResult> removeList=new ArrayList<>();

        //Joran 2021-09-27 添加根据外胎进行成型排程硫化状态（任务类型）收尾处理start
        cxScheduleResultLhTaskTypeCloseOut(lastDayTaskList,monthPlanApsVersion);
        //Joran 2021-09-27 添加根据外胎进行成型排程硫化状态（任务类型）收尾处理end
        //加载成型月度胎胚剩余量汇总表
        Map<String, CxEngineEmbryoMonthPlanSurplus> cxEngineEmbryoMonthPlanSurplusMap=cxEngineEmbryoMonthPlanSurplusService.listCxEmbryoMonthPlanSurplusByMonthPlanApsVersion(monthPlanApsVersion);
        //Joran 2022-01-14 临时从导入的废次品数据中根据sap获取废次品数据
        StringBuilder sb=new StringBuilder();
        if(StringUtils.isNotEmpty(cxEngineEmbryoMonthPlanSurplusMap)){
            //Joran 2022-04-01 用于存储插单同胎胚总计划量(昨天三班至次二班的总计划量)
             Map<String,Integer> insertEmbryoPlanQty=new HashMap<>();
            //Joran 2022-04-01 遍历记录全部插单规格如果遇到相同胎胚的进行累加处理当做月度剩余量start
            for(CxEngineScheduleResult cxEngineScheduleResult:lastDayTaskList){
                String embryoCode=cxEngineScheduleResult.getEmbryoCode();
                if(!CxEngineConstants.CX_SCHEDULE_DATA_SOURCE_INSERT.equals(cxEngineScheduleResult.getDataSource())&&cxEngineEmbryoMonthPlanSurplusMap.containsKey(embryoCode)){
                    continue;
                }
                Integer monthRemainQty=0;
                if(insertEmbryoPlanQty.containsKey(embryoCode)){
                    monthRemainQty=insertEmbryoPlanQty.get(embryoCode);
                    monthRemainQty+=cxEngineScheduleResult.getAfterClass3PlanQty();
                }else{
                    monthRemainQty=cxEngineScheduleResult.getAfterClass3PlanQty();
                }
                insertEmbryoPlanQty.put(embryoCode,monthRemainQty);
            }
            //Joran 2022-04-01 遍历记录全部插单规格如果遇到相同胎胚的进行累加处理当做月度剩余量end

            for(CxEngineScheduleResult cxScheduleTask:lastDayTaskList){
                //预设置不提示，若符合条件则会重新赋值
                cxScheduleTask.setMarkCloseOutTip(CxEngineConstants.CLOSE_OUT_TIP_NO);
                String embryoCode=cxScheduleTask.getEmbryoCode();
                if(cxEngineEmbryoMonthPlanSurplusMap.containsKey(embryoCode)){
                    CxEngineEmbryoMonthPlanSurplus cxEngineEmbryoMonthPlanSurplus=cxEngineEmbryoMonthPlanSurplusMap.get(embryoCode);
                    cxScheduleTask.setMonthRemainQty(cxEngineEmbryoMonthPlanSurplus.getMonthRemainQty());//Joran 2021-06-25 冗余月度剩余量
                    cxScheduleTask.setCxMonthFinishQty(cxEngineEmbryoMonthPlanSurplus.getMonthFinishQty()==null?0:cxEngineEmbryoMonthPlanSurplus.getMonthFinishQty());//Joran 2021-07-20 冗余成型月度完成量
                    cxScheduleTask.setMonthFinishQty(cxEngineEmbryoMonthPlanSurplus.getMonthFinishQty()==null?0:cxEngineEmbryoMonthPlanSurplus.getMonthFinishQty());//Joran 2021-07-20 冗余成型月度完成量
                    cxScheduleTask.setRejectQty(cxEngineEmbryoMonthPlanSurplus.getEmbryoBadQty());//Joran 2021-10-14 不良数记录到废次品数栏位
                    cxScheduleTask.setActualOverProduction(cxEngineEmbryoMonthPlanSurplus.getActualOverProduction());//Joran 2021-10-14 实际超欠产情况
                    Integer actualOverProduction=cxScheduleTask.getActualOverProduction();
                    Integer expectedOverProduction=cxScheduleTask.getExpectedOverProduction()==null?0:cxScheduleTask.getExpectedOverProduction();
                    cxScheduleTask.setDifferenceOverProduction(actualOverProduction-expectedOverProduction);//Joran 2021-10-14 预计超欠产情况
                    if(cxEngineEmbryoMonthPlanSurplus.getIsCloseOut()){//月度胎胚汇总表中存在且已收尾的记录
                        cxScheduleTask.setUpdateBy(SecurityUtils.getUsername());//更新任务
                        cxScheduleTask.setUpdateTime(DateUtils.getNowDate());
                        removeList.add(cxScheduleTask);
                        sb.append("可收尾规格SAP：").append(cxScheduleTask.getSapCode()).append("胎胚代码").append(cxScheduleTask.getEmbryoCode()).append(division);
                    }else if(StringUtils.isNotEmpty(embryoCodeMap)&&embryoCodeMap.containsKey(embryoCode)){
                        Integer otherPlanQty=embryoCodeMap.get(embryoCode);
                        //当前月度剩余量
                        Integer monthRemainQty=cxEngineEmbryoMonthPlanSurplus.getMonthRemainQty();
                        if(monthRemainQty>otherPlanQty){
                            monthRemainQty-=otherPlanQty;
                            cxScheduleTask.setMonthRemainQty(monthRemainQty);
                        }else{
                            cxScheduleTask.setUpdateBy(SecurityUtils.getUsername());//更新任务
                            cxScheduleTask.setUpdateTime(DateUtils.getNowDate());
                            removeList.add(cxScheduleTask);
                            sb.append("机台重排可收尾规格SAP：").append(cxScheduleTask.getSapCode()).append("胎胚代码").append(cxScheduleTask.getEmbryoCode()).append(division);
                        }

                    }
                }else if(CxEngineConstants.CX_SCHEDULE_DATA_SOURCE_INSERT.equals(cxScheduleTask.getDataSource())&& insertEmbryoPlanQty.containsKey(embryoCode)){//Joran 2022-04-01 插单规格设置月度计划量为总计划量
                    Integer monthRemainQty=insertEmbryoPlanQty.get(embryoCode);
                    sb.append("插单规格月度剩余量特殊处理，规格SAP：").append(cxScheduleTask.getSapCode()).append("胎胚代码").append(cxScheduleTask.getEmbryoCode()).append(division);
                    if(monthRemainQty>0){
                        cxScheduleTask.setCxMonthFinishQty(BigDecimal.ZERO.intValue());
                        cxScheduleTask.setMonthFinishQty(BigDecimal.ZERO.intValue());
                        cxScheduleTask.setMonthRemainQty(monthRemainQty);
                    }else{
                        removeList.add(cxScheduleTask);
                        sb.append("插单规格没有任务剩余量进行收尾，收尾规格SAP：").append(cxScheduleTask.getSapCode()).append("胎胚代码").append(cxScheduleTask.getEmbryoCode()).append(division);
                    }

                }else{
                    cxScheduleTask.setUpdateBy(SecurityUtils.getUsername());//更新任务
                    cxScheduleTask.setUpdateTime(DateUtils.getNowDate());
                    removeList.add(cxScheduleTask);
                    sb.append("月度计划规格没有剩余量进行收尾，收尾规格SAP：").append(cxScheduleTask.getSapCode()).append("胎胚代码").append(cxScheduleTask.getEmbryoCode()).append(division);
                }
            }
            //移除收尾规格
            if(StringUtils.isNotEmpty(removeList)&&!isSupple){
                //更新生产状态为收尾状态
                cxScheduleEngineMapper.updateProductStatusToCloseOut(removeList);
                lastDayTaskList.removeAll(removeList);
                //构建收尾移除日志
                buildCloseOutLog(cxEngineEmbryoMonthPlanSurplusMap,lastDayTaskList,removeList,sb.toString());
            }
        }else{
            log.debug("月度计划版本号：{}，【移除收尾规格】未找到相应的月度汇总数据",monthPlanApsVersion);
        }
    }

    /**
     * 构建删除收尾日志
     * @param cxEngineEmbryoMonthPlanSurplusMap
     * @param lastDayTaskList
     * @param removeList
     * @param logDetail
     */
    public void buildCloseOutLog(Map<String, CxEngineEmbryoMonthPlanSurplus> cxEngineEmbryoMonthPlanSurplusMap,List<CxEngineScheduleResult> lastDayTaskList,List<CxEngineScheduleResult> removeList,String logDetail){
        if(StringUtils.isNotEmpty(removeList)){
            StringBuffer logSb = new StringBuffer("");
            logSb.append("成型月度计划胎胚剩余集合：" + toJSONString(cxEngineEmbryoMonthPlanSurplusMap)).append(division);
            logSb.append("前一天成型排程结果列表’：" + toJSONString(lastDayTaskList)).append(division);
            logSb.append("标记删除明细：" + logDetail).append(division);
            logSb.append("收尾结果列表：" + toJSONString(removeList));
            autoScheduleLogService.insertCxScheduleLog(removeList.get(0).getCxBatchNo(), "", "移除前一日收尾规格", logSb.toString());
        }
    }

    /**
     * 更新外胎、胎胚维度的月度汇总表计划调整量、月度剩余量
     * @param beforeScheduleResult
     * @param differencePlanQty
     */
    public void updateSapEmbryoSurplus(CxEngineScheduleResult beforeScheduleResult, Integer differencePlanQty) {
        //1.更新外胎维度汇总表 计划修正量、月度剩余量
        CxEngineMonthPlanSurplus updateSapCodeSurPlus =new CxEngineMonthPlanSurplus();
        updateSapCodeSurPlus.setUpdateInsertQty(differencePlanQty);
        updateSapCodeSurPlus.setSapCode(beforeScheduleResult.getSapCode());
        updateSapCodeSurPlus.setMonthPlanApsVersion(beforeScheduleResult.getApsMonthVersion());
        cxEngineMonthPlanSurplusService.updateMonthPlanSurplusBySapCodeVersion(updateSapCodeSurPlus);
        //2.更新胎胚维度汇总表计划修正量、月度剩余量
        CxEngineEmbryoMonthPlanSurplus updateEmbryoCodeSurPlus=new CxEngineEmbryoMonthPlanSurplus();
        updateEmbryoCodeSurPlus.setUpdateInsertQty(differencePlanQty);
        updateEmbryoCodeSurPlus.setEmbryoCode(beforeScheduleResult.getEmbryoCode());
        updateEmbryoCodeSurPlus.setMonthPlanApsVersion(beforeScheduleResult.getApsMonthVersion());
        cxEngineEmbryoMonthPlanSurplusService.updateMonthPlanSurplusByEmbryoCodeVersion(updateEmbryoCodeSurPlus);
        //3.更新胎胚代码进行半部件重新生成
        List<CxEngineEmbryoMonthPlanSurplus> updateEmbryoCodePartList=cxEngineEmbryoMonthPlanSurplusService.selectCxEmbryoMonthPlanSurplusList(updateEmbryoCodeSurPlus);
        if(StringUtils.isNotEmpty(updateEmbryoCodePartList)){
            List<TCxEmbryoMonthPlanSurplus> list=new ArrayList<>(updateEmbryoCodePartList.size());
            for(CxEngineEmbryoMonthPlanSurplus cxEngineEmbryoMonthPlanSurplus:updateEmbryoCodePartList){
                TCxEmbryoMonthPlanSurplus updateEmbryoMonthPlanSurplus= BeanConverUtil.conver(cxEngineEmbryoMonthPlanSurplus,TCxEmbryoMonthPlanSurplus.class);
                updateEmbryoMonthPlanSurplus.setMonthPlanQty(BigDecimal.valueOf(cxEngineEmbryoMonthPlanSurplus.getMonthPlanQty()));//计划量
                updateEmbryoMonthPlanSurplus.setMonthPlanModifyQty(BigDecimal.valueOf(cxEngineEmbryoMonthPlanSurplus.getMonthPlanModifyQty()));//计划调整量
                updateEmbryoMonthPlanSurplus.setMonthFinishQty(BigDecimal.valueOf(cxEngineEmbryoMonthPlanSurplus.getMonthFinishQty()));//完成量
                updateEmbryoMonthPlanSurplus.setMonthRemainQty(BigDecimal.valueOf(cxEngineEmbryoMonthPlanSurplus.getMonthRemainQty()));//月度剩余量
                updateEmbryoMonthPlanSurplus.setEmbryoBadQty(BigDecimal.valueOf(cxEngineEmbryoMonthPlanSurplus.getEmbryoBadQty()));//不良数量
                updateEmbryoMonthPlanSurplus.setLastMonthStock(BigDecimal.valueOf(cxEngineEmbryoMonthPlanSurplus.getLastMonthStock()));//月结库存
                updateEmbryoMonthPlanSurplus.setMaterialCode(cxEngineEmbryoMonthPlanSurplus.getEmbryoCode());//胎胚代码
                updateEmbryoMonthPlanSurplus.setDataSource(Integer.valueOf(cxEngineEmbryoMonthPlanSurplus.getDataSource()));//数据来源
                list.add(updateEmbryoMonthPlanSurplus);
            }
            // 2021.12.02 Gim 暂时弃用
//            mdmMonthPlanAmountSumService.buildHalfPartByEmbryoCastor(beforeScheduleResult.getApsMonthVersion(),list);
        }

    }

    /**
     * 删除外胎汇总表、胎胚汇总表中插单且创建时间为当前排程日期的数据
     * @param scheduleDate
     */
    public void synRemoveSurplus(Date scheduleDate,String monthPlanApsVersion,String sapCode,String embryoCode) {
        int result=0;
        String scheduleDateStr=DateUtils.parseDateToStr("yyyy-MM-dd",scheduleDate);
        //删除当天外胎插单汇总表数据
        CxEngineMonthPlanSurplus deleteMonthPlanSurplus=new CxEngineMonthPlanSurplus();
        deleteMonthPlanSurplus.setStartTime(scheduleDateStr+" 00:00:00");
        deleteMonthPlanSurplus.setEndTime(scheduleDateStr+" 23:59:59");
        if(StringUtils.isNotEmpty(sapCode)){
            deleteMonthPlanSurplus.setSapCode(sapCode);
        }
        result+=cxEngineMonthPlanSurplusService.deleteMonthPlanSurplusByDataSource(deleteMonthPlanSurplus);

        CxEngineEmbryoMonthPlanSurplus deleteEmbryoMonthPlanSurplus=new CxEngineEmbryoMonthPlanSurplus();
        deleteEmbryoMonthPlanSurplus.setStartTime(scheduleDateStr+" 00:00:00");
        deleteEmbryoMonthPlanSurplus.setEndTime(scheduleDateStr+" 23:59:59");
        if(StringUtils.isNotEmpty(embryoCode)){
            deleteEmbryoMonthPlanSurplus.setEmbryoCode(embryoCode);
        }
        result+=cxEngineEmbryoMonthPlanSurplusService.deleteEmbryoMonthPlanSurplusByDataSource(deleteEmbryoMonthPlanSurplus);
        //调用月度汇总全部进行重算
        if(result>0){
            mdmMonthPlanAmountSumService.recalculateByApsVersion(monthPlanApsVersion);
        }else{
            log.debug("【月度汇总表不进行重算】，删除插单记录数为0");
        }

    }

    /**
     * 汇总月度计划初稿的计划更新到排程中SAP+胎胚的最新计划数中
     * @param lastDayTaskList
     * @param scheduleDate
     */
    public void updateNewestPlanQty(List<CxEngineScheduleResult> lastDayTaskList, Date scheduleDate) {
        //获取下个月的数据
        Date nextMonth=DateUtils.addMonths(scheduleDate,1);
        String year=DateUtils.parseDateToStr(DateUtils.YYYY,nextMonth);
        String month=DateUtils.parseDateToStr("MM",nextMonth);;
        Map<String, MdmMonthProdPlan> nextMonthPlanDraftMap=mdmMonthProdPlanService.nextMonthPlanDraft(year,month, EngineConstants.IS_FINALIZED_NO,"");
        if(StringUtils.isNotEmpty(nextMonthPlanDraftMap)){
            for(CxEngineScheduleResult cxEngineScheduleResult:lastDayTaskList){
                String machKey= GenerageMapKeyUtils.createMapKey(cxEngineScheduleResult.getSapCode(),cxEngineScheduleResult.getEmbryoCode(),cxEngineScheduleResult.getBomDataVersion());
                if(nextMonthPlanDraftMap.containsKey(machKey)){
                    MdmMonthProdPlan nextMonthPlan=nextMonthPlanDraftMap.get(machKey);
                    cxEngineScheduleResult.setNewestPlanQty(nextMonthPlan.getMonthTotalPlanQty());
                }
            }
        }
    }

    /**
     * 因为版本更新需要对投产数据进行移除并状态更新为已投产
     * @param monthPlanApsVersion
     * @param scheduleDate
     */
    public void removePlanProductStatusList(String monthPlanApsVersion, Date scheduleDate) {
        //如果存在已投产的规格则进行标记为已投产且从投产列表移除
        if(StringUtils.isNotEmpty(monthPlanApsVersion)&&scheduleDate!=null){
            String scheduleMonth=DateUtils.parseDateToStr("yyyyMM",scheduleDate);
            cxPlanProductStatusService.batchUpdateProductStatusToProduct(monthPlanApsVersion,scheduleMonth);
        }
    }

    /**
     * 根据生产排程版本进行待投产列表数据获取
     * @param monthPlanApsVersion
     * @return
     */
    public List<CxPlanProductStatus> getPlanProductStatusByApsVersion(String monthPlanApsVersion,String productStatus,Date scheduleDate) {
        CxPlanProductStatus productStatusCondition=new CxPlanProductStatus();
        productStatusCondition.setMonthPlanApsVersion(monthPlanApsVersion);
        if(StringUtils.isEmpty(productStatus)){
            productStatusCondition.setProductStatusWithOut(CxEngineConstants.MDM_PLAN_PRODUCT_STATUS_YES);//Joran 2021-08-04 获取除已投产的其他状态
        }else{
            productStatusCondition.setProductStatus(productStatus);
        }
        //Joran 2022-04-11 进行获取结束日期设置范围start
        boolean isChange=true;
        if(isChange){
            String maxEndDate=getMaxEndDate(scheduleDate);
            productStatusCondition.setEndDate(maxEndDate);
        }
        //Joran 2022-04-11 进行获取结束日期设置范围end

        List<CxPlanProductStatus> cxPlanProductStatusList=this.cxPlanProductStatusService.loadAviableProductList(productStatusCondition);
        return cxPlanProductStatusList;
    }

    /**
     * 月度计划衔接时，根据前一天的计划来进行投产表更新
     * @param lastMonthTaskList
     * @param cxPlanProductStatusList
     */
    public void updatePlanProductStatusList(List<CxEngineScheduleResult> lastMonthTaskList, List<CxPlanProductStatus> cxPlanProductStatusList) {
        //将成型排程结果按照外胎+胎胚组合map
        Map<String,CxEngineScheduleResult> scheduleResultMap=lastMonthTaskList.stream().collect(
                Collectors.toMap(
                        cxEngineScheduleResult -> GenerageMapKeyUtils.createMapKey(
                                cxEngineScheduleResult.getSapCode(),
                                cxEngineScheduleResult.getEmbryoCode(),
                                cxEngineScheduleResult.getBomDataVersion()
                        ),
                        scheduleResult -> scheduleResult,(k1,k2)->k1
                )
        );
        List<CxPlanProductStatus> productList=new ArrayList<>();
        //遍历所有的待投产集合
        for(CxPlanProductStatus cxPlanProductStatus:cxPlanProductStatusList){
            String sapCode=cxPlanProductStatus.getSapCode();
            String embryoCode=cxPlanProductStatus.getEmbryoCode();
            String bomDataVersion=cxPlanProductStatus.getBomDataVersion();
            String key=GenerageMapKeyUtils.createMapKey(sapCode,embryoCode,bomDataVersion);
            if(scheduleResultMap.containsKey(key)){
                productList.add(cxPlanProductStatus);
            }
        }
        //如果存在已投产的规格则进行标记为已投产且从投产列表移除
        if(StringUtils.isNotEmpty(productList)){
            cxPlanProductStatusService.updateProductStatusToProduct(productList);
            cxPlanProductStatusList.removeAll(productList);
        }
    }

    /**
     * 处理插单月度汇总、外胎汇总、半部件汇总相关数据逻辑
     * @param cxScheduleResult
     */
    public void handleInsertOrder(CxScheduleResult cxScheduleResult) {
        CxEngineScheduleResult beforeScheduleResult=cxScheduleEngineMapper.selectCxEngineScheduleResultById(cxScheduleResult.getId());
        //当前调量的排程项是插单需要对外胎、胎胚汇总数据进行
        if(beforeScheduleResult!=null&&CxEngineConstants.CX_SCHEDULE_DATA_SOURCE_INSERT.equals(beforeScheduleResult.getDataSource())){
            //调整后的计划量
            CxEngineScheduleResult afterScheduleResult=BeanConverUtil.conver(cxScheduleResult,CxEngineScheduleResult.class);
            //获取调量前的总计划量
            Integer beforeDayPlanQty=beforeScheduleResult.getDayTotalPlanQty();
            //获取调量后的总计划量
            Integer afterDayPlanQty=afterScheduleResult.getDayTotalPlanQty();
            //调量前后有差异的情况时，需要进行外胎、胎胚、半部件汇总表调整
            if(!beforeDayPlanQty.equals(afterDayPlanQty)){
                //计算出差值
                Integer differencePlanQty=afterDayPlanQty-beforeDayPlanQty;
                updateSapEmbryoSurplus(beforeScheduleResult,differencePlanQty);
            }
        }
    }

    /**
     * 获取定额信息
     * @param cxMachineCode  成型机台
     * @param embryoCode  胎胚代码
     * @param
     * @return
     */
    public Integer getQuotaByMachineEmbryoCode(String cxMachineCode,String embryoCode,String bomDataVersion,StringBuilder logDetail) {
        Integer machineQuota=cxEngineQuotaCommonService.getCxMachineQuota(cxMachineCode,embryoCode,bomDataVersion);
        log.debug("【获取定额】获取成型机定额数据，机台编号："+cxMachineCode+"，胎胚代码："+embryoCode+"，定额数："+machineQuota);
        logDetail.append("【获取定额】获取成型机定额数据，机台编号："+cxMachineCode+"，胎胚代码："+embryoCode+"，定额数："+machineQuota).append(division);
        return machineQuota;
    }

    /**
     * 加载施工信息
     * @return
     */
    public Map<String, EngineProductConstructionInfo>  loadEngineConstructionMapFromRedis(){
        return cxEngineQuotaCommonService.loadEngineConstructionMapFromRedis();
    }

    /**
     * 根据定额，班次安排的计划量 计算班次剩余时间
     * @param quota
     * @param planQty
     * @return
     */
    public Double getClassShiftRemainTime(Double classShiftHour,Integer quota,Integer planQty){
        if(planQty>=quota){
            return BigDecimal.ZERO.doubleValue();
        }
        Double usedTime=getUsedTime(quota,planQty);
        Double remainTime=classShiftHour-usedTime; //班次时长扣减掉已经安排的时长算出剩余时间
        return remainTime;
    }


    /**
     * 结合定额以及计划量获取计划所使用的的时间
     * @param quota 当前定额
     * @param planQty 当前计划
     * @return
     */
    public Double getUsedTime(Integer quota,Integer planQty){
        BigDecimal hourCountBig=BigDecimal.valueOf(quota).divide(BigDecimal.valueOf(CxEngineConstants.CLASS_SHIFT_HOUR)); //一个小时生产多少
        BigDecimal planQtyBig=BigDecimal.valueOf(planQty);
        Double usedTime=planQtyBig.divide(hourCountBig,3, BigDecimal.ROUND_DOWN).doubleValue(); //已经消耗的时间
        return usedTime;
    }

    /**
     * 规格切换所需要的时长
     * @param engineConstructionInfoMap
     * @param cxParamsMap
     * @param afterKey
     * @param beforeKey
     * @param logDetail
     * @return
     */
    public Double taskChangeSpecTime(Map<String, EngineProductConstructionInfo> engineConstructionInfoMap,Map<String,String> cxParamsMap,String afterKey,String beforeKey,StringBuilder logDetail) {
        if(StringUtils.isEmpty(beforeKey)){
            return CxEngineConstants.ZERO;
        }else if(afterKey.equals(beforeKey)){
            return  CxEngineConstants.ZERO;
        }
        //获取两个规格间的施工信息
        EngineProductConstructionInfo afterSpec=engineConstructionInfoMap.get(afterKey);
        EngineProductConstructionInfo beforeSpec=engineConstructionInfoMap.get(beforeKey);
        if(afterSpec==null||beforeSpec==null){
            throw new CxScheduleEngineException(StringUtils.format(I18nUtil.getMessage("cx.engine.auto.change.spec.time.error"), GenerageMapKeyUtils.getEmbryoCodeByCreateKey(beforeKey), GenerageMapKeyUtils.getEmbryoCodeByCreateKey(afterKey)));
        }
        Double beforeNoseWidth=beforeSpec.getNoseWidth();//机头宽度
        if(beforeNoseWidth==null){
            throw new CxScheduleEngineException(StringUtils.format(I18nUtil.getMessage("cx.engine.auto.beforeNoseWidth.error"),GenerageMapKeyUtils.getEmbryoCodeByCreateKey(beforeKey)));
        }
        Double beforeFlipDiscDiameter=beforeSpec.getFlipDiscDiameter();//扣圈盘直径

        Double afterNoseWidth=afterSpec.getNoseWidth();//机头宽度
        Double afterFlipDiscDiameter=afterSpec.getFlipDiscDiameter();//扣圈盘直径

        //扣圈盘直径和机头宽度完全一样，不需要进行工装更换
        if(beforeFlipDiscDiameter!=null&&beforeFlipDiscDiameter.equals(afterFlipDiscDiameter)&&beforeNoseWidth.equals(afterNoseWidth)){
            return CxEngineConstants.ZERO;
        }else if(beforeNoseWidth.equals(afterNoseWidth)){
            return CxEngineConstants.ZERO;
        }
        String minChangeSpecTime=cxParamsMap.get(CxParamCodeConstants.CX_MIN_CHANGE_SPEC_TIME);
        if(StringUtils.isEmpty(minChangeSpecTime)){
            throw new CxScheduleEngineException(I18nUtil.getMessage("cx.engine.auto.change.spec.min.time.param.error"));
        }
        //根据配置获取更换规格切换时间
        Double minChangeSpecTimeMin=Double.valueOf(minChangeSpecTime);
        Double minChangeSpecHour=BigDecimal.valueOf(minChangeSpecTimeMin/CxEngineConstants.ONE_MINUTE_SECOND).setScale(CxEngineConstants.TWO_SCALE, BigDecimal.ROUND_HALF_UP).doubleValue();
        logDetail.append("【计算更换工装时长】前规格胎胚："+beforeKey+",机头宽度："+beforeNoseWidth+",扣圈盘直径:"+beforeFlipDiscDiameter+"。后规格胎胚："+afterKey+",机头宽度："+afterNoseWidth+",扣圈盘直径:"+afterFlipDiscDiameter+"，更换工装时长："+minChangeSpecHour+"(小时)").append(division);
        return minChangeSpecHour;
    }

    /**
     * 根据排程日期获取对应的月度计划版本信息
     * @param scheduleDate 排程日期
     * @param cxBatchNo
     * @return 日期对应月度计划版本号
     */
    public String  getMdmMonthPlanMainByDate(Date scheduleDate,String cxBatchNo){
        String monthPlanApsVersion="";
        MdmMonthPlanMain planVersion=mdmMonthPlanMainService.getValidPlanMainVersion(scheduleDate);
        if(planVersion==null){
            String errorMsg= I18nUtil.getMessage("cx.engine.auto.plan.main.empty.error");
            cxEngineAutoScheduleRecordService.generagAutoScheduleRecord(scheduleDate,null,cxBatchNo, CxEngineConstants.AUTO_SCHEDULE_STATUS_FAILE,errorMsg);
            throw new CxScheduleEngineException(errorMsg);
        }
        monthPlanApsVersion=planVersion.getMonthPlanApsVersion();
        return monthPlanApsVersion;
    }

    /**
     * 复制前一天的任务列表
     * 将前一天排程的次日一班计划量复制到新任务一班计划
     * @param newTaskList
     * @param lastDayTaskList
     */
    public void copyLastDayTaskToNewTask(String cxBatchNo,List<CxEngineScheduleResult> newTaskList, List<CxEngineScheduleResult> lastDayTaskList,Map<String,List<LhEngineTireConstructionInfo>> sapTireConstructionListMap, Date scheduleDate,boolean coverClass3PlannedQty,StringBuilder logDetail) {
        if(StringUtils.isEmpty(lastDayTaskList)){
            throw new CxScheduleEngineException(I18nUtil.getMessage("cx.engine.auto.copy.lastDay.schedule.error"));
        }
        CxEngineScheduleResult target=null;
        scheduleDate=CxScheduleUtils.formatDateByZero(scheduleDate);
        String scheduleDateStr=DateUtils.parseDateToStr("yyyyMMdd",scheduleDate);
        logDetail.append("copyLastDayTaskToNewTask:复制前一天任务机台").append(division);
        //Joran 2022-03-30 只加载一次成型参数
        Map<String,String> cxParams=loadCxParamsMap();
        for(CxEngineScheduleResult lastTask:lastDayTaskList){
            lastTask.setBaseVale(null);
            target= CxScheduleUtils.copyCxScheduleTask(lastTask,cxBatchNo,scheduleDate,coverClass3PlannedQty);
            target.setOrderNo(getCxSequence(CxPrefixConstants.SCHEDULE_ORDER_NO_PREFIX+scheduleDateStr, CxPrefixConstants.CX_ORDER_NO_PREFIX+scheduleDateStr));

            //计算单班硫化量
            calcSingleShiftLhQtyByCxParams(target,cxParams,sapTireConstructionListMap);
            //计算最小硫化机需求数
            if(target.calcMonthRemainQty()>0){
                calcLeastLhMachineQtyByMonthRemainQty(target,target.calcMonthRemainQty(),sapTireConstructionListMap);
            }
            logDetail.append("机台名称：").append(lastTask.getCxMachineName()).append(",胎胚代码:").append(lastTask.getEmbryoCode()).append(",计算单班硫化量=").append(lastTask.getSingleShiftLhQty()).append("，计算最小硫化机数=").append(target.getMinimumLhMachineReqQty()).append(division);
            newTaskList.add(target);
        }
    }

    /**
     * 设置默认自动安排计划标记
     * @param lastDayScheduleResultList
     */
    public void defaultToProduct(List<CxEngineScheduleResult> lastDayScheduleResultList) {
        //Joran 2021-12-24 如果投产标记为空的则默认先设置为自动安排start
        for(CxEngineScheduleResult cxEngineScheduleResult:lastDayScheduleResultList){
            if(StringUtils.isEmpty(cxEngineScheduleResult.getToProduct())){
                cxEngineScheduleResult.setToProduct(CxEngineConstants.TO_PRODUCT_YES);
            }
        }
        //Joran 2021-12-24 如果投产标记为空的则默认先设置为自动安排end
    }

    /**
     * 加载成型机台日期在产规格
     * @param productDate 生产日期，空默认为当前系统日期
     * @return <机台编号,胎胚代码>
     */
    public Map<String,String> loadAllMachineInProductionSpecByDate(Date productDate){
        return cxEngineCommonService.cxMachineInProductSpecMap(productDate);
    }

    /**
     * 汇总胎胚类型的库存总数
     * @param newTaskList
     * @return
     */
    public Map<String, Integer> generateEmbryoTypeMap(List<CxEngineScheduleResult> newTaskList,Map<String,Double> sameDimensionAvailableClassOneShiftMap,Integer classIndex,StringBuilder logDetail) {
        logDetail.append("汇总胎胚类型的库存总数").append(division);
        Map<String,Integer> embryoTypeTotalMap=new HashMap<>();
        Map<String,String> cxParams=loadCxParamsMap();
        Integer onceTotalStock=0;
        Integer twiceTotalStock=0;
        //一次法
        String oncePrefix= getEmbryoCodePrefix(cxParams, CxParamCodeConstants.ONCE_EMBRYOCODE_PREFIX);
        //二次法
        String twicePrefix= getEmbryoCodePrefix(cxParams, CxParamCodeConstants.TWICE_EMBRYOCODE_PREFIX);

        List<CxEngineScheduleResult> onceTaskList=new ArrayList<>();

        List<CxEngineScheduleResult> twiceTaskList=new ArrayList<>();

        if(StringUtils.isNotEmpty(newTaskList)){
            //记录已经计算过的胎胚，不再重复进行计算
            Map<String,String> embryoCodeKey=new HashMap<>();
            //遍历进行胎胚类型汇总start
            for(CxEngineScheduleResult cxEngineScheduleResult:newTaskList){
                CxScheduleUtils.calcAllClassAvailableLhShift(cxEngineScheduleResult);
                String embryoCode=cxEngineScheduleResult.getEmbryoCode();
                if(StringUtils.startsWithIgnoreCase(embryoCode,oncePrefix)&&!embryoCodeKey.containsKey(embryoCode)){
                    onceTotalStock+=cxEngineScheduleResult.getTotalStock();
                    onceTaskList.add(cxEngineScheduleResult);
                }else if(StringUtils.startsWithIgnoreCase(embryoCode,twicePrefix)&&!embryoCodeKey.containsKey(embryoCode)){
                    twiceTotalStock+=cxEngineScheduleResult.getTotalStock();
                    twiceTaskList.add(cxEngineScheduleResult);
                }
                embryoCodeKey.put(embryoCode,embryoCode);
            }
            //遍历进行胎胚类型汇总end

            if(StringUtils.isNotEmpty(onceTaskList)){
                Map<String,List<CxEngineScheduleResult>> onceDimensionTaskMap= CxScheduleUtils.splitTaskByDimension(oncePrefix,onceTaskList);
                logDetail.append("一次法按寸口拆分集合数据【").append(JSON.toJSONString(onceDimensionTaskMap)).append("】").append(division);
                //处理同寸口一班可硫化班次平均值计算start
                if(StringUtils.isNotEmpty(onceDimensionTaskMap)){
                    generageDimensionTaskMap(onceDimensionTaskMap,sameDimensionAvailableClassOneShiftMap,classIndex,logDetail);
                }
                //处理一次法同寸口一班可硫化班次平均值计算end
            }
            if(StringUtils.isNotEmpty(twiceTaskList)){
                Map<String,List<CxEngineScheduleResult>> twiceDimensionTaskMap= CxScheduleUtils.splitTaskByDimension(twicePrefix,twiceTaskList);
                logDetail.append("二次法按寸口拆分集合数据【").append(JSON.toJSONString(twiceDimensionTaskMap)).append("】").append(division);
                if(StringUtils.isNotEmpty(twiceDimensionTaskMap)){
                    generageDimensionTaskMap(twiceDimensionTaskMap,sameDimensionAvailableClassOneShiftMap,classIndex,logDetail);
                }
            }
        }
        embryoTypeTotalMap.put(oncePrefix,onceTotalStock);
        embryoTypeTotalMap.put(twicePrefix,twiceTotalStock);

        return embryoTypeTotalMap;

    }

    /**
     * 构建轮胎类型+寸口集合组装数据
     * @param onceDimensionTaskMap
     * @param sameDimensionAvailableClassOneShiftMap
     */
    private void generageDimensionTaskMap(Map<String, List<CxEngineScheduleResult>> onceDimensionTaskMap, Map<String, Double> sameDimensionAvailableClassOneShiftMap,Integer classIndex,StringBuilder logDetail) {
        logDetail.append("开始计算不同寸口的平均可硫化班次，当前班次下标：【").append(classIndex).append("】").append(division);
            for(Map.Entry<String,List<CxEngineScheduleResult>> entry:onceDimensionTaskMap.entrySet()){
            List<CxEngineScheduleResult> dimensionTaskList=entry.getValue();
            Double availableLhShift=CxScheduleUtils.calcAvgAvailableLhShiftIndex(dimensionTaskList,classIndex);
            sameDimensionAvailableClassOneShiftMap.put(entry.getKey(),availableLhShift);
            logDetail.append("当前寸口：【").append(entry.getKey()).append("】,当前平均可硫化班次数=").append(availableLhShift).append(division);
        }
    }

    /**
     * 构建班制顺序
     * @param cxAutoScheduleTaskListMap
     * @param classSortMap
     */
    public void buildClassSort(Map<String, List<CxAutoScheduleTask>> cxAutoScheduleTaskListMap, Map<ClassEnums, Integer> classSortMap) {
        List<CxAutoScheduleTask> allTaskList=new ArrayList<>();
        //遍历将所有任务放一起
        for(Map.Entry<String, List<CxAutoScheduleTask>> entry:cxAutoScheduleTaskListMap.entrySet()){
            allTaskList.addAll(entry.getValue());
        }
        //降序排序
        CxScheduleUtils.sortDescByScheduleTaskClassShift(allTaskList);
        //遍历所有任务班次
        for(CxAutoScheduleTask cxAutoScheduleTask:allTaskList){
            int classShift=cxAutoScheduleTask.getClassShift();//拿到班次
            ClassEnums cls=ClassEnums.getClassEnums(classShift);
            //否则则判断当前班次跟重复班次是否相同
            if(classSortMap.containsKey(cls)){
                int sort=classSortMap.get(cls)+1;
                classSortMap.put(cls,sort);
            }else{
                classSortMap.put(cls,1);
            }
        }
    }

    /**
     * 创建班次剩余量任务安排
     * @param nextCxEngineScheduleResult
     * @param currentShiftPlanQty
     */
    public CxAutoScheduleTask createClassShiftRemainQty(CxEngineScheduleResult nextCxEngineScheduleResult,int classShift,Integer currentShiftPlanQty,int taskQty,int continuePlanQty,Double remainTime) {
        CxAutoScheduleTask autoScheduleTask=CxScheduleUtils.createScheduleTask(nextCxEngineScheduleResult,classShift,taskQty,continuePlanQty,remainTime);
        autoScheduleTask.setCurrentShiftPlanQty(currentShiftPlanQty);
        autoScheduleTask.setScheduleDate(DateUtils.parseDateToStr("yyyyMMdd",nextCxEngineScheduleResult.getScheduleDate()));
        autoScheduleTask.setCxOrderNo(nextCxEngineScheduleResult.getOrderNo());
        autoScheduleTask.setRemainTime(CxEngineConstants.ZERO);
        autoScheduleTask.setClassShiftHour(remainTime);
        return  autoScheduleTask;
    }

    /**
     * //计算单班硫化量
     * 单班硫化量=（单班时间-10）/(单条硫化时间+2) * 模数
     * @param target
     */
    public void calcSupplePlanSingleShiftLhQty(CxLastDaySupplePlanDto target) {
        //加载成型参数
        Map<String,String>  cxParamsMap=loadCxParamsMap();
        Integer shiftTime=getShiftTime(cxParamsMap);
        Integer brushBagTime=getBrushBagTime(cxParamsMap);
        Double singleLhTime=getSingleLhTime(target.getSapCode(), target.getEmbryoCode());
        Double moldNum=1D;
        if(target.getLhMachineQty()!=null){
            moldNum=target.getLhMachineQty();
        }
        BigDecimal moldNumDecimal=BigDecimal.valueOf(moldNum);
        BigDecimal singleShiftLhQtyDecimal=BigDecimal.valueOf(((double)shiftTime/(singleLhTime + brushBagTime))).setScale(2, BigDecimal.ROUND_DOWN).multiply(moldNumDecimal);
        target.setSingleShiftLhQty(singleShiftLhQtyDecimal.intValue());
        target.setLhSingleTireTime(singleLhTime);//设置单胎硫化时长
    }

    /**
     * 根据SPA品号获取单条硫化时长
     * //单条硫化时间后续调整为从BOM信息中获取
     * @param sapCode
     * @return
     */
    public Double getSingleLhTimeByLhConstruction(String sapCode,Map<String, List<LhEngineTireConstructionInfo>> sapTireConstructionListMap) {
        if(StringUtils.isEmpty(sapCode)){
            log.error("【获取单条硫化时间】通过SAP品号获取单条硫化时长异常");
            return BigDecimal.ZERO.doubleValue();
        }

        if(StringUtils.isEmpty(sapTireConstructionListMap)){
            log.error("【硫化外胎施工异常】：当前外胎施工信息为空");
            return BigDecimal.ZERO.doubleValue();
        }
        Double lhTime=lhEngineTireConstructionInfoService.getSingleTireTimeBySap(sapCode,null,sapTireConstructionListMap);
        return lhTime;
    }

    /**
     * 单班硫化量计算公式抽取
     * @param moldNum 总模数
     * @param shiftTime 班次时长
     * @param singleLhTime 单胎硫化时长
     * @param brushBagTime 刷囊时间
     * @return 单班硫化量
     */
    private Integer calcSingleMoldQty(Double moldNum, Integer shiftTime, Double singleLhTime, Integer brushBagTime) {
        Integer singleShiftLhQty=1;
        BigDecimal moldNumDecimal=BigDecimal.valueOf(moldNum);
        Double divisor=singleLhTime + brushBagTime;
        if(Double.valueOf(0).equals(divisor)){
            throw new CxScheduleEngineException("计算异常：除数为0！");
        }
        BigDecimal singleShiftLhQtyDecimal=BigDecimal.valueOf((shiftTime/divisor)).setScale(0, BigDecimal.ROUND_DOWN).multiply(moldNumDecimal);
        if(singleShiftLhQtyDecimal.intValue()>0){
            singleShiftLhQty=singleShiftLhQtyDecimal.intValue();
        }
        return singleShiftLhQty;
    }

    /**
     * 获取最大的结束日期
     * @param scheduleDate
     * @return
     */
    private String getMaxEndDate(Date scheduleDate) {
      Map<String,String> cxParamsMap=loadCxParamsMap();
      Integer step= CxEngineConstants.DEFAULT_MAX_STEP;
        if(StringUtils.isNotEmpty(cxParamsMap)&&cxParamsMap.containsKey(CxParamCodeConstants.MAX_PRODUCT_END_DATE_STEP)){
            String maxEndDateStepParams=cxParamsMap.get(CxParamCodeConstants.MAX_PRODUCT_END_DATE_STEP);
            if(StringUtils.isNotEmpty(maxEndDateStepParams)){
                step = Integer.valueOf(maxEndDateStepParams);
            }
        }
        Date maxEndDate=DateUtils.addDays(scheduleDate,step);
        return DateUtils.parseDateToStr("yyyyMMdd",maxEndDate);

    }

    /**
     * 把排程数据同步到log表
     * @param scheduleDate 排程日期，格式：yyyyMMdd
     */
    public void syncCxScheduleToLog(String scheduleDate,String cxMachineCode,String sourceCxOrder){
        cxEngineCommonService.syncCxScheduleToLog(scheduleDate,cxMachineCode,sourceCxOrder);
    }

    /**
     * 自动排程调用不需要进行初始化基础数据
     * @param machineCode
     * @param machineScheduleList
     * @param dataSource
     * @param engineConstructionInfoMap
     * @param cxParamsMap
     */
    public void calcMachineTaskTime(String machineCode,List<CxEngineScheduleResult> machineScheduleList,List<CxScheduleTaskTime> scheduleTaskTimeList,String dataSource,Map<String, EngineProductConstructionInfo> engineConstructionInfoMap,Map<String,String>cxParamsMap){
        if(StringUtils.isNotEmpty(engineConstructionInfoMap)){
            this.engineConstructionInfoMap=engineConstructionInfoMap;
        }
        if(StringUtils.isNotEmpty(cxParamsMap)){
            this.cxParamsMap=cxParamsMap;
        }
        calcMachineTaskTime(machineCode,machineScheduleList,scheduleTaskTimeList,dataSource,false);
    }


    /**
     *  单机台任务计算预计开始结束时间
     * @param machineScheduleList
     */
    public void calcMachineTaskTime(String machineCode,List<CxEngineScheduleResult> machineScheduleList,List<CxScheduleTaskTime> scheduleTaskTimeList,String dataSource,boolean needInit){
        if(StringUtils.isEmpty(machineScheduleList)){
            return;
        }
        if(needInit){
            engineConstructionInfoMap=loadEngineConstructionMapFromRedis();
            cxParamsMap=loadCxParamsMap();
        }

        //初始化各个班次时长
        initShiftHourMap(machineCode);
        //获取到当前排程日期
        Date scheduleDate=machineScheduleList.get(0).getScheduleDate();
        String machineName=machineScheduleList.get(0).getCxMachineName();
        Date lastDate=DateUtils.addDays(scheduleDate,-1);

        /**
         * 初始化各个班次开始时间
         */
        initShiftDateMap(lastDate);

        //机台三班班次开始时间
        Date estimateStartTime= DateUtils.addHours(CxScheduleUtils.formatDateByZero(lastDate),8);

        Integer classIndex=BigDecimal.ZERO.intValue();
        //初始顺序从0开始
        Integer productOrder=BigDecimal.ZERO.intValue();
        //前一个规格对象，初始为空
        CxEngineScheduleResult lastScheduleResult=null;
        //前一个规格的结束时间
        Date lastEstimateEndTime=null;
        Set<String> ignoreSetOrderNo=new HashSet<>(); //可以是班次+工单号
        while(classIndex<=ClassEnums.CLASS_FIVE.getClassIndex()){
            List<CxEngineScheduleResult> needAnalysisList=new ArrayList<>();
            productOrder+=1;//顺序从1
            CxScheduleTaskTime cxScheduleTaskTime=null;
            //1.从昨日三班有计划量开始算
            for(CxEngineScheduleResult cxEngineScheduleResult: machineScheduleList){
                String key=GenerageMapKeyUtils.createMapKey(classIndex+"",cxEngineScheduleResult.getOrderNo());
                if(ignoreSetOrderNo.contains(key)){
                    continue;
                }
                if(CxScheduleUtils.getCurrentClassPlanQtyByShiftIndex(cxEngineScheduleResult,classIndex)>0){
                    needAnalysisList.add(cxEngineScheduleResult);
                }
            }

            //2.如果三班计划中存在两个有计划则只能按照班次排序来区分先后,如果只有一个证明是单任务满班排载
            if(StringUtils.isEmpty(needAnalysisList)){
                //班次没计划开始时间后移
                estimateStartTime= DateUtils.addHours(estimateStartTime,8);
                classIndex+=1;
                continue;
                //break;
            }else if(StringUtils.isNotEmpty(needAnalysisList) && needAnalysisList.size()==1){
                //开始进行任务时间数据组装分析
                CxEngineScheduleResult currentOnlyTask=needAnalysisList.get(0);
                cxScheduleTaskTime=new CxScheduleTaskTime();
                classIndex=buildTaskTime(currentOnlyTask,lastScheduleResult,machineCode,productOrder,dataSource,scheduleDate,estimateStartTime,lastEstimateEndTime,cxScheduleTaskTime,classIndex);

                //解析下一个规格，且将结束时间先放到下个规格参数上。
                classIndex =analysisEndTime(currentOnlyTask,classIndex,machineScheduleList,cxScheduleTaskTime.getEstimateStartTime(),cxScheduleTaskTime,ignoreSetOrderNo);
                //将当前规格置为前规格后续循环使用
                lastScheduleResult=currentOnlyTask;
                //Joran 2022-06-23 只有有一个时间没有计算出来，该时间段作废，不再进行计算start
                if(cxScheduleTaskTime.getEstimateStartTime()==null ||cxScheduleTaskTime.getEstimateEndTime()==null){
                   break;
                }
                //Joran 2022-06-23 只有有一个时间没有计算出来，该时间段作废，不再进行计算end
                //前一个规格的结束时间
                lastEstimateEndTime=cxScheduleTaskTime.getEstimateEndTime();
                scheduleTaskTimeList.add(cxScheduleTaskTime);
            }else { //当前班次存在多个规格时，最多两个规格，否则会无法拿准顺序！
                //先进行生产顺序排序
                needAnalysisList= productSortByList(classIndex,needAnalysisList);
                ClassEnums cls=ClassEnums.getClassEnums(classIndex);
                String className="无班次";
                if(StringUtils.isEmpty(needAnalysisList)){
                    if(cls==null){
                        className=cls.getClassName();
                    }
                    throw new CxScheduleEngineException(StringUtils.format(I18nUtil.getMessage("cx.engine.import.errorLog.classShift.morePlan.error"),className,machineName));
                }
                //标记下个班次有计划地
                Integer nextPlanHasCount=0;
                for(CxEngineScheduleResult cxEngineScheduleResult :needAnalysisList){
                    String key=GenerageMapKeyUtils.createMapKey(classIndex+"",cxEngineScheduleResult.getOrderNo());
                    //忽略同工单号再同一个班次的计划
                    if(ignoreSetOrderNo.contains(key)){
                        continue;
                    }
                    cxScheduleTaskTime=new CxScheduleTaskTime();
                    //代码注释，目前成型计划会有多个规格多个班次都有计划，计划有问题导致排序计算有问题
                    cls= ClassEnums.getClassEnums(classIndex+1);
                    if(cls!=null&&CxScheduleUtils.getCurrentClassPlanQtyByShiftIndex(cxEngineScheduleResult,classIndex+1)>0){
                        className=cls.getClassName();
                        nextPlanHasCount+=1;
                        continue;
                    }
                    //构建任务时间对象
                    classIndex=buildTaskTime(cxEngineScheduleResult,lastScheduleResult,machineCode,productOrder,dataSource,scheduleDate,estimateStartTime,lastEstimateEndTime,cxScheduleTaskTime,classIndex);
                    //爆班的直接不做处理start
                    if(classIndex > ClassEnums.CLASS_FIVE.getClassIndex()){
                        break;
                    }
                    //爆班的直接不做处理end
                    String shiftKey=GenerageMapKeyUtils.createMapKey(machineCode,classIndex +"");
                    Double remainTime=machineShiftHourMap.get(shiftKey);
                    Integer machineQuota=getQuotaByMachineEmbryoCode(machineCode,cxEngineScheduleResult.getEmbryoCode(),cxEngineScheduleResult.getBomDataVersion(),new StringBuilder());
                    Integer currentPlanQty=CxScheduleUtils.getCurrentClassPlanQtyByShiftIndex(cxEngineScheduleResult,classIndex);
                    Double usedTime=getUsedTime(machineQuota,currentPlanQty);
                    remainTime=remainTime-usedTime; //班次时长扣减掉已经安排的时长算出剩余时间

                    Integer useMinutes=BigDecimal.valueOf(usedTime * CxEngineConstants.ONE_MINUTE_SECOND ).setScale(1, RoundingMode.UP).intValue();
                    Date estimateEndTime=DateUtils.addMinutes(cxScheduleTaskTime.getEstimateStartTime(),useMinutes);
                    cxScheduleTaskTime.setEstimateEndTime(estimateEndTime);
                    updateMachineShiftHourMap(machineCode,classIndex ,remainTime);
                    //将当前规格置为前规格后续循环使用
                    lastScheduleResult=cxEngineScheduleResult;
                    //前一个规格的结束时间
                    lastEstimateEndTime=cxScheduleTaskTime.getEstimateEndTime();
                    scheduleTaskTimeList.add(cxScheduleTaskTime);
                    ignoreSetOrderNo.add(key);
                    if(remainTime<=BigDecimal.ZERO.intValue()&&classIndex<ClassEnums.CLASS_FIVE.getClassIndex()){
                        classIndex+=1;
                    }
                }

                Integer planCount=needAnalysisList.size();
                if(planCount.equals(nextPlanHasCount)){
                    throw new CxScheduleEngineException(StringUtils.format(I18nUtil.getMessage("cx.engine.import.errorLog.classShift.morePlan.error"),className,machineName));
                }

            }
        }
       /* if(StringUtils.isNotEmpty(scheduleTaskTimeList)){
            cxScheduleTaskTimeService.batchInsertCxScheduleTaskTime(scheduleTaskTimeList);
        }*/
    }

    /**
     * 构建任务时间对象
     * @param cxEngineScheduleResult
     * @param lastScheduleResult
     * @param machineCode
     * @param productOder
     * @param dataSource
     * @param scheduleDate
     * @param estimateStartTime
     * @param lastEstimateEndTime
     * @param cxScheduleTaskTime
     * @param classIndex
     * @return
     */
    private Integer buildTaskTime(CxEngineScheduleResult cxEngineScheduleResult, CxEngineScheduleResult lastScheduleResult, String machineCode, Integer productOder,String dataSource, Date scheduleDate, Date estimateStartTime,Date lastEstimateEndTime, CxScheduleTaskTime cxScheduleTaskTime, Integer classIndex) {
        cxEngineScheduleResult.setPlanSort(productOder);
        cxScheduleTaskTime.setProductOrder(cxEngineScheduleResult.getPlanSort());//生产顺序
        cxScheduleTaskTime.setMachineCode(machineCode);
        cxScheduleTaskTime.setDataSource(dataSource);
        cxScheduleTaskTime.setCxOrderNo(cxEngineScheduleResult.getOrderNo());
        cxScheduleTaskTime.setScheduleDate(scheduleDate);
        //开始时间
        cxScheduleTaskTime.setEstimateStartTime(estimateStartTime);
        if(lastScheduleResult!=null){
            classIndex =nextSpecStartTime(machineCode,classIndex,cxEngineScheduleResult,lastScheduleResult,lastEstimateEndTime,cxScheduleTaskTime);
        }
        return classIndex;
    }


    /**
     * 生产顺序排序
     * @param needAnalysisList
     */
    private List<CxEngineScheduleResult> productSortByList(int classIndex,List<CxEngineScheduleResult> needAnalysisList) {
        List<CxEngineScheduleResult> sortList=new ArrayList<>(needAnalysisList.size());
         //是否通过前规格排序
         boolean isBefore=false;
         //是否通过后规格排序
         boolean isAfter=false;
         if(classIndex>=ClassEnums.CLASS_ONE.getClassIndex() && classIndex<ClassEnums.CLASS_FIVE.getClassIndex()){
            //通过前后规格排序
            isBefore=true;
            isAfter=true;
        }else if(classIndex==BigDecimal.ZERO.intValue()){
             //通过后规格排序
             isAfter=true;
         }else if(classIndex==ClassEnums.CLASS_FIVE.getClassIndex()){
             //通过前规格排序
             isBefore=true;
         }else{
            return sortList;
        }
        if(StringUtils.isEmpty(needAnalysisList)){
            return needAnalysisList;
        }else{
            //前后规格一起排序
            if(isBefore&&isAfter){
                beforeAfterSpecSort(classIndex,needAnalysisList,sortList);
            }else if(isBefore){
                beforeSpecSort(classIndex,needAnalysisList,sortList);
            }else if(isAfter){
                afterSpecSort(classIndex,needAnalysisList,sortList);
            }
        }
        if(StringUtils.isEmpty(sortList)){
            return needAnalysisList;
        }
        return sortList;
    }

    /**
     * 后规格排序
     * @param classIndex
     * @param needAnalysisList
     * @param sortList
     */
    private void afterSpecSort(int classIndex, List<CxEngineScheduleResult> needAnalysisList, List<CxEngineScheduleResult> sortList) {
        //根据后规格排序
        List<CxEngineScheduleResult> afterList=new ArrayList<>();
        for(CxEngineScheduleResult cxEngineScheduleResult:needAnalysisList){
            int afterPlanQty=CxScheduleUtils.getCurrentClassPlanQtyByShiftIndex(cxEngineScheduleResult,classIndex+1);
            if(afterPlanQty<=0){
                sortList.add(cxEngineScheduleResult);
            }else{
                afterList.add(cxEngineScheduleResult);
            }
        }
        if(StringUtils.isNotEmpty(afterList)){
            sortList.addAll(afterList);
        }
    }

    /**
     * 结合前规格排序
     * @param classIndex
     * @param needAnalysisList
     * @param sortList
     */
    private void beforeSpecSort(int classIndex, List<CxEngineScheduleResult> needAnalysisList, List<CxEngineScheduleResult> sortList) {
        List<CxEngineScheduleResult> beforeList=new ArrayList<>();
        for (CxEngineScheduleResult cxEngineScheduleResult:needAnalysisList){
            int beforePlanQty=CxScheduleUtils.getCurrentClassPlanQtyByShiftIndex(cxEngineScheduleResult,classIndex-1);
            if(beforePlanQty>0){
                sortList.add(cxEngineScheduleResult);
            }
        }
        if(StringUtils.isNotEmpty(beforeList)){
            sortList.addAll(beforeList);
        }
    }

    /**
     * 前后规格一起排序
     * @param classIndex
     * @param needAnalysisList
     * @param sortList
     */
    private void beforeAfterSpecSort(int classIndex, List<CxEngineScheduleResult> needAnalysisList, List<CxEngineScheduleResult> sortList) {
        Set<String> sortOrderNo=new HashSet<>();
        //根据前规格排序有值的就是优先
        for (CxEngineScheduleResult cxEngineScheduleResult:needAnalysisList){
            int beforePlanQty=CxScheduleUtils.getCurrentClassPlanQtyByShiftIndex(cxEngineScheduleResult,classIndex-1);
            if(beforePlanQty>0){
                sortOrderNo.add(cxEngineScheduleResult.getOrderNo());
                sortList.add(cxEngineScheduleResult);
            }
        }
        //根据后规格排序
        List<CxEngineScheduleResult> afterList=new ArrayList<>();
        for(CxEngineScheduleResult cxEngineScheduleResult:needAnalysisList){
            String orderNo=cxEngineScheduleResult.getOrderNo();
            if(sortOrderNo.contains(orderNo)){
                continue;
            }
            int afterPlanQty=CxScheduleUtils.getCurrentClassPlanQtyByShiftIndex(cxEngineScheduleResult,classIndex+1);
            if(afterPlanQty<=0){
                sortList.add(cxEngineScheduleResult);
            }else{
                afterList.add(cxEngineScheduleResult);
            }
        }

        if(StringUtils.isNotEmpty(afterList)){
            sortList.addAll(afterList);
        }

    }


    /**
     * 解析结束时间
     * @param currentOnlyTask 当前规格
     * @param machineScheduleList 当前机台全部任务
     * @param estimateStartTime 开始时间
     * @return 当前任务结束时间
     */
    private Integer analysisEndTime(CxEngineScheduleResult currentOnlyTask,Integer classIndex, List<CxEngineScheduleResult> machineScheduleList, Date estimateStartTime,CxScheduleTaskTime cxScheduleTaskTime,Set<String> ignoreSetOrderNo) {
        if(classIndex>ClassEnums.CLASS_FIVE.getClassIndex()){
            return classIndex;
        }
        //下一个规格开始班次
        String machineCode=currentOnlyTask.getCxMachineCode();
        Date estimateEndTime=estimateStartTime;
        Integer machineQuota=getQuotaByMachineEmbryoCode(machineCode,currentOnlyTask.getEmbryoCode(),currentOnlyTask.getBomDataVersion(),new StringBuilder());
        Integer currentPlanQty=CxScheduleUtils.getCurrentClassPlanQtyByShiftIndex(currentOnlyTask,classIndex);
        if(currentPlanQty >0 ){
            String key=GenerageMapKeyUtils.createMapKey(machineCode,classIndex +"");
            Double remainTime=machineShiftHourMap.get(key);
            Double usedTime=getUsedTime(machineQuota,currentPlanQty);
            remainTime=remainTime-usedTime; //班次时长扣减掉已经安排的时长算出剩余时间
            if(remainTime<BigDecimal.ZERO.doubleValue()){
                remainTime=BigDecimal.ZERO.doubleValue();
            }
            Integer useMinutes=BigDecimal.valueOf(usedTime * CxEngineConstants.ONE_MINUTE_SECOND ).setScale(1, RoundingMode.UP).intValue();
            estimateEndTime=DateUtils.addMinutes(estimateEndTime,useMinutes);
            updateMachineShiftHourMap(machineCode,classIndex ,remainTime);
        }
        String currentOrderNo=currentOnlyTask.getOrderNo();
        while(classIndex<=ClassEnums.CLASS_FIVE.getClassIndex()){
            ClassEnums nextClass=ClassEnums.getClassEnums(classIndex+1);
            if(nextClass!=null){
                currentPlanQty=CxScheduleUtils.getCurrentClassPlanQtyByShiftIndex(currentOnlyTask,classIndex+1);
            }else{
                currentPlanQty=CxScheduleUtils.getCurrentClassPlanQtyByShiftIndex(currentOnlyTask,classIndex);
            }

            if(currentPlanQty<=0){
                //estimateEndTime=DateUtils.addHours(estimateStartTime,8);
                cxScheduleTaskTime.setEstimateEndTime(estimateEndTime);
                updateMachineShiftHourMap(machineCode,classIndex,BigDecimal.ZERO.doubleValue());
                classIndex+=1;
                break;
            }else{ //下个班次当前规格还有计划
                if(currentPlanQty < machineQuota){
                    //下个班次有计划的话，需要更新下个班次时间，获取下个班次
                    if(nextClass!=null){
                        classIndex=nextClass.getClassIndex();
                    }
                    String key=GenerageMapKeyUtils.createMapKey(machineCode,classIndex +"");
                    Double remainTime=machineShiftHourMap.get(key);
                    Double usedTime=getUsedTime(machineQuota,currentPlanQty);
                    remainTime=remainTime-usedTime; //班次时长扣减掉已经安排的时长算出剩余时间
                    Integer useMinutes=BigDecimal.valueOf(usedTime * CxEngineConstants.ONE_MINUTE_SECOND ).setScale(1, RoundingMode.UP).intValue();
                    estimateEndTime=DateUtils.addMinutes(estimateEndTime,useMinutes);
                    cxScheduleTaskTime.setEstimateEndTime(estimateEndTime);
                    updateMachineShiftHourMap(machineCode,classIndex ,remainTime);

                    //Joran 2022-06-03 因为当前班次计划几乎不会满定额排，所以这个时候如果没有满班定额时，需要判断当前规格下个班次是否还有计划，有的话继续读start
                    /*while(validateCurrentSpecNextFlag(currentOnlyTask,classIndex+1)){
                        classIndex+=1;
                        currentPlanQty=CxScheduleUtils.getCurrentClassPlanQtyByShiftIndex(currentOnlyTask,classIndex);
                        if(currentPlanQty < machineQuota){
                            key=GenerageMapKeyUtils.createMapKey(machineCode,classIndex +"");
                            remainTime=machineShiftHourMap.get(key);
                            usedTime=getUsedTime(machineQuota,currentPlanQty);
                            remainTime=remainTime-usedTime; //班次时长扣减掉已经安排的时长算出剩余时间
                            useMinutes=BigDecimal.valueOf(usedTime * CxEngineConstants.ONE_MINUTE_SECOND ).setScale(1, RoundingMode.UP).intValue();
                            estimateEndTime=DateUtils.addMinutes(estimateEndTime,useMinutes);
                            cxScheduleTaskTime.setEstimateEndTime(estimateEndTime);
                            updateMachineShiftHourMap(machineCode,classIndex ,remainTime);
                        }else{
                            estimateEndTime=DateUtils.addHours(estimateEndTime,8);
                            updateMachineShiftHourMap(machineCode,classIndex ,BigDecimal.ZERO.doubleValue());
                            cxScheduleTaskTime.setEstimateEndTime(estimateEndTime);
                            classIndex+=1;
                            //Joran 2022-05-18 当拿到下一个班次时最后一个班时且是满班就直接班次往后加跳出迭代
                            if(classIndex==ClassEnums.CLASS_FIVE.getClassIndex()){
                                return classIndex+1;
                            }
                        }

                    }*/
                    //Joran 2022-06-03 因为当前班次计划几乎不会满定额排，所以这个时候如果没有满班定额时，需要判断当前规格下个班次是否还有计划，有的话继续读end
                    //遍历当前计划如果全部计划不包括自身，没有存在计划量时，则班次往下移动start
                    boolean needNextClassIndex=validateNextFlag(machineScheduleList,classIndex,currentOrderNo);
                    while(needNextClassIndex&&classIndex<ClassEnums.CLASS_FIVE.getClassIndex()){
                        //classIndex+=1;
                        classIndex+=1;
                        currentPlanQty=CxScheduleUtils.getCurrentClassPlanQtyByShiftIndex(currentOnlyTask,classIndex);
                        if(currentPlanQty < machineQuota){
                            key=GenerageMapKeyUtils.createMapKey(machineCode,classIndex +"");
                            remainTime=machineShiftHourMap.get(key);
                            usedTime=getUsedTime(machineQuota,currentPlanQty);
                            remainTime=remainTime-usedTime; //班次时长扣减掉已经安排的时长算出剩余时间
                            useMinutes=BigDecimal.valueOf(usedTime * CxEngineConstants.ONE_MINUTE_SECOND ).setScale(1, RoundingMode.UP).intValue();
                            estimateEndTime=DateUtils.addMinutes(estimateEndTime,useMinutes);
                            cxScheduleTaskTime.setEstimateEndTime(estimateEndTime);
                            updateMachineShiftHourMap(machineCode,classIndex ,remainTime);
                        }else{
                            estimateEndTime=DateUtils.addHours(estimateEndTime,8);
                            updateMachineShiftHourMap(machineCode,classIndex ,BigDecimal.ZERO.doubleValue());
                            cxScheduleTaskTime.setEstimateEndTime(estimateEndTime);
                            classIndex+=1;
                            //Joran 2022-05-18 当拿到下一个班次时最后一个班时且是满班就直接班次往后加跳出迭代
                            if(classIndex==ClassEnums.CLASS_FIVE.getClassIndex()){
                                return classIndex+1;
                            }
                        }
                        needNextClassIndex=validateNextFlag(machineScheduleList,classIndex,currentOrderNo);
                    }
                    String ignoreSetOrderNoKey=GenerageMapKeyUtils.createMapKey(classIndex +"",currentOnlyTask.getOrderNo());
                    ignoreSetOrderNo.add(ignoreSetOrderNoKey);
                    //遍历当前计划如果全部计划不包括自身，没有存在计划量时，则班次往下移动end
                    break;
                }else{
                    estimateEndTime=DateUtils.addHours(estimateEndTime,8);
                    updateMachineShiftHourMap(machineCode,classIndex ,BigDecimal.ZERO.doubleValue());
                    cxScheduleTaskTime.setEstimateEndTime(estimateEndTime);
                    classIndex+=1;
                    //Joran 2022-05-18 当拿到下一个班次时最后一个班时且是满班就直接班次往后加跳出迭代
                    if(classIndex==ClassEnums.CLASS_FIVE.getClassIndex()){
                        return classIndex+1;
                    }

                }
            }
        }
        return classIndex;
    }

    /**
     * 因为当前的成型计划很多几乎没有满班排，所以需要看下个班次是否有计划如果有计划直接还是读取当前规格
     * @param currentOnlyTask
     * @param nextClsIndex
     * @return
     */
    private boolean validateCurrentSpecNextFlag(CxEngineScheduleResult currentOnlyTask, int nextClsIndex) {
        Boolean skipToNextCls=false;
        ClassEnums nextCls=ClassEnums.getClassEnums(nextClsIndex);
        if(nextCls!=null&& CxScheduleUtils.getCurrentClassPlanQty(currentOnlyTask,nextCls)>0){
            //标记为当前规格跳到下一个班次
            skipToNextCls=true;
        }
        return skipToNextCls;
    }

    /**
     * 遍历机台全部任务验证是否需要往下一个班次
     * @param machineScheduleList
     * @param nextClassIndex
     * @return
     */
    private boolean validateNextFlag(List<CxEngineScheduleResult> machineScheduleList, Integer nextClassIndex,String currentOrderNo) {
        boolean needNextClassIndex=true;
        //相同班次下个班次有计划量
        for(CxEngineScheduleResult cxEngineScheduleResult:machineScheduleList){
            //如果下个计划只有当前工单时还是需要往下个班次start
            if(currentOrderNo.equals(cxEngineScheduleResult.getOrderNo())){
                continue;
            }
            //如果下个计划只有当前工单时还是需要往下个班次end
            Integer otherSpecPlanQty=CxScheduleUtils.getCurrentClassPlanQtyByShiftIndex(cxEngineScheduleResult,nextClassIndex);
            if(otherSpecPlanQty>0){
               return false;
            }
        }
        return needNextClassIndex;
    }

    /**
     * 计算下个规格开始时间
     * @param currentOnlyTask 当前规格
     * @param lastScheduleResult 前规格
     * @param lastEstimateEndTime 前规格结束时间
     * @return
     */
    private int nextSpecStartTime(String machineCode, int classIndex,CxEngineScheduleResult currentOnlyTask,CxEngineScheduleResult lastScheduleResult,
                                    Date lastEstimateEndTime,CxScheduleTaskTime cxScheduleTaskTime) {
        String key=GenerageMapKeyUtils.createMapKey(machineCode,classIndex+"");
        Double shiftRemainTime=machineShiftHourMap.get(key);
        //当前规格
        String afterKey=GenerageMapKeyUtils.createMapKey(currentOnlyTask.getEmbryoCode(),currentOnlyTask.getBomDataVersion());
        //前规格
        String beforeKey=GenerageMapKeyUtils.createMapKey(lastScheduleResult.getEmbryoCode(),lastScheduleResult.getBomDataVersion());
        //更换工装时间
        Double changeSpecTime=taskChangeSpecTime(engineConstructionInfoMap,cxParamsMap,afterKey,beforeKey,new StringBuilder());
        if(shiftRemainTime >= BigDecimal.ZERO.doubleValue()){
            //二分之一更换工装时间
            Double halfChangeSpecTime=BigDecimal.valueOf(changeSpecTime).divide(BigDecimal.valueOf(2)).doubleValue();
            if(shiftRemainTime <= halfChangeSpecTime){ //小于二分之一班次,新规格下一个班次扣除整个更换工装时长
                //更新班次剩余时长为0
                updateMachineShiftHourMap(machineCode,classIndex, BigDecimal.ZERO.doubleValue());
                //前规格剩余时间小于更换工装工时一半，则下个班扣除整个更换工装时长
                classIndex+=1;
                ClassEnums cls =ClassEnums.getClassEnums(classIndex);
                if(cls==null){
                    return classIndex;
                }
                Double classShiftHour=machineShiftHourMap.get(key);
                shiftRemainTime=classShiftHour - changeSpecTime ;

                updateMachineShiftHourMap(machineCode,classIndex, shiftRemainTime);
                //获取班次开始时间
                Date shiftStartTime=classShiftDateTime.get(classIndex+"");
                Integer useMinutes=BigDecimal.valueOf(changeSpecTime * CxEngineConstants.ONE_MINUTE_SECOND ).setScale(1, RoundingMode.UP).intValue();
                Date estimateStartTime=DateUtils.addMinutes(shiftStartTime,useMinutes);
                cxScheduleTaskTime.setEstimateStartTime(estimateStartTime);
            } else if(shiftRemainTime>=changeSpecTime){ //剩余时间大于二分之一更换工装时间 且大于更换工装时间时，则进行工装时间扣除
                shiftRemainTime=shiftRemainTime-changeSpecTime; //剩余时间
                //更新班次剩余时长为0
                updateMachineShiftHourMap(machineCode,classIndex, shiftRemainTime);
                Integer useMinutes=BigDecimal.valueOf(changeSpecTime * CxEngineConstants.ONE_MINUTE_SECOND ).setScale(1, RoundingMode.UP).intValue();
                Date estimateStartTime=DateUtils.addMinutes(lastEstimateEndTime,useMinutes);
                cxScheduleTaskTime.setEstimateStartTime(estimateStartTime);
            }else{ //当前规格当前班次扣掉二分之一的时长，下一个规格 下一个班次扣除二分之一时长
                //班次剩余时长先扣除
                updateMachineShiftHourMap(machineCode,classIndex, BigDecimal.ZERO.doubleValue());
                //继续往下一个班次
                classIndex+=1;
                ClassEnums nextCls =ClassEnums.getClassEnums(classIndex);
                if(nextCls==null){
                    cxScheduleTaskTime.setEstimateStartTime(null);//开始时间置空
                    return classIndex;
                }
                key=GenerageMapKeyUtils.createMapKey(machineCode,classIndex+"");
                shiftRemainTime=machineShiftHourMap.get(key);
                shiftRemainTime=shiftRemainTime-halfChangeSpecTime; //剩余时间
                //班次剩余时长先扣除
                updateMachineShiftHourMap(machineCode,classIndex, shiftRemainTime);
                //获取班次开始时间
                Date shiftStartTime=classShiftDateTime.get(classIndex+"");
                Integer useMinutes=BigDecimal.valueOf(halfChangeSpecTime * CxEngineConstants.ONE_MINUTE_SECOND ).setScale(1, RoundingMode.UP).intValue();
                Date estimateStartTime=DateUtils.addMinutes(shiftStartTime,useMinutes);
                cxScheduleTaskTime.setEstimateStartTime(estimateStartTime);

            }
        }
        return classIndex;
    }

    /**
     * 初始化班次可用时间
     * @param machineCode
     */
    private void initShiftHourMap(String machineCode) {
        if(StringUtils.isEmpty(machineShiftHourMap)){
            machineShiftHourMap=new HashMap<>();
        }
        //Joran 2022-02-25 进行前日三班班次时长初始化start
        String key=GenerageMapKeyUtils.createMapKey(machineCode,BigDecimal.ZERO.toString());
        if(!machineShiftHourMap.containsKey(key)){
            machineShiftHourMap.put(key, CxEngineConstants.CLASS_SHIFT_HOUR);
        }
        //Joran 2022-02-25 进行前日三班班次时长初始化end
        for (ClassEnums cls : ClassEnums.values()) {
            key=GenerageMapKeyUtils.createMapKey(machineCode,cls.getClassIndex()+"");
            if(!machineShiftHourMap.containsKey(key)){
                machineShiftHourMap.put(key, CxEngineConstants.CLASS_SHIFT_HOUR);
            }
        }
    }

    /**
     * 更新班次剩余时长
     * @param cxMachineCode
     * @param classIndex
     */
    private void updateMachineShiftHourMap(String cxMachineCode, Integer classIndex, Double remainTime) {
        String key=GenerageMapKeyUtils.createMapKey(cxMachineCode,classIndex+"");
        if(machineShiftHourMap.containsKey(key)){
            machineShiftHourMap.put(key,remainTime);
        }else{
            machineShiftHourMap.put(key,remainTime);
        }
    }


    /**
     * 初始化各个班次开始时间
     * @param scheduleDate
     */
    private void initShiftDateMap(Date scheduleDate) {
        if(StringUtils.isEmpty(classShiftDateTime)){
            classShiftDateTime= new HashMap<>();
            Date estimateStartTime= DateUtils.addHours(CxScheduleUtils.formatDateByZero(scheduleDate),8);
            //三班开始时间
            classShiftDateTime.put(BigDecimal.ZERO.toString(),estimateStartTime);
            for(ClassEnums cls:ClassEnums.values()){
                estimateStartTime=DateUtils.addHours(estimateStartTime,8 );
                classShiftDateTime.put(cls.getClassIndex()+"",estimateStartTime);
            }
        }
    }


    /**
     * 调用后用来进行缓存清空
     */
    public void clearCacheData(){
        engineConstructionInfoMap=null;
        cxParamsMap=null;
        machineShiftHourMap=null;
        classShiftDateTime=null;
    }


}
