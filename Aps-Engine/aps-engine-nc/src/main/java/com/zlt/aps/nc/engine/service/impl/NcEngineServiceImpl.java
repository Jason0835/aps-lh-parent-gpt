package com.zlt.aps.nc.engine.service.impl;

import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.utils.SecurityUtils;
import com.ruoyi.common.core.utils.bean.BeanUtils;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.core.utils.BigDecimalUtil;
import com.zlt.aps.common.core.utils.BigDecimalUtils;
import com.zlt.aps.common.engine.common.CxEngineQuotaCommonService;
import com.zlt.aps.common.engine.constants.EngineConstants;
import com.zlt.aps.common.engine.domain.EngineConstructionInfo;
import com.zlt.aps.common.engine.enums.OpenMachineClassEnums;
import com.zlt.aps.common.engine.mapper.CommonMapper;
import com.zlt.aps.common.engine.service.AutoScheduleLogService;
import com.zlt.aps.common.engine.service.impl.IncrementService;
import com.zlt.aps.common.engine.utils.CollectionUtil;
import com.zlt.aps.nc.api.domain.entity.NcMachineInfo;
import com.zlt.aps.nc.api.domain.entity.NcScheduleResult;
import com.zlt.aps.nc.engine.mapper.NcEngineMapper;
import com.zlt.aps.nc.engine.mapper.NcEngineStockMapper;
import com.zlt.aps.nc.engine.service.*;
import com.zlt.aps.nc.engine.vo.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

import static com.alibaba.fastjson.JSON.toJSONString;
import static com.zlt.aps.common.core.utils.ApsCommonUtil.*;

@Slf4j
@Service
public class NcEngineServiceImpl implements NcEngineService {

    @Resource
    private NcEngineMapper ncEngineMapper;
    @Resource
    private NcEngineGlueService ncEngineGlueService;
    @Resource
    private NcEngineStockService ncEngineStockService;
    @Resource
    private NcEngineMachineService ncEngineMachineService;
    @Resource
    private IncrementService incrementService;
    @Resource
    private NcEngineLossService ncEngineLossService;
    @Resource
    private NcEngineMonthSurplusService ncEngineMonthSurplusService;
    @Resource
    private CxEngineQuotaCommonService cxEngineQuotaCommonService;
    @Resource
    private CommonMapper commonMapper;
    @Resource
    private AutoScheduleLogService autoScheduleLogService;
    @Resource
    private NcEngineCurlRollService ncEngineCurlRollService;
    @Resource
    private NcEngineStockMapper ncEngineStockMapper;
    private String division = "\r\n---------------------------------------------------\r\n";  //日志分割符

    /**
     * 排程参数预设值，参数设置取不到值时使用这些预设值
     */
    private final static String DEFAULT_STANDARD_CRIMP_LENGTH = "84"; // 卷曲标准长度
    private final static String DEFAULT_CURL_DECIMAL_ROUNDING = "0.3"; // 卷曲数小数取整值
    private final static String DEFAULT_CLOSE_OUT_DAYS = "1"; // 共用规格收尾判断天数
    private final static String DEFAULT_TOOL_ROLL_NUM = "4"; // 工装包含大卷数
    private final static String DEFAULT_PRODUCT_STOCK_HOUR = "12"; // 保库存供应时长
    private final static String DEFAULT_LARGE_DEMAND = "24"; // 需求量超过该值的算大需求量规格（默认24卷）
    private final static String DEFAULT_DELAY_PLAN_QTY = "1000"; // 早班需求低于该值的规格可以推迟到下一个班再做
    private final static String DEFAULT_LARGE_DEMAND_REDUCE = "0.7"; // 大需求量规格备库比例
    private final static String DEFAULT_EQUAL_SHARE_THRESHOLD = "500"; // 需求量超过该值早夜班对半分
    private final static String DEFAULT_MID_CAPACITY = "344"; // 中班定额
    private final static String DEFAULT_NIGHT_CAPACITY = "252"; // 夜班定额
    private final static Double MIN_PLAN_QTY = 27D; // 最小排产量限制
	/**
	 * 生产阶段校验开关状态：打开
	 */
	private final static String PRODUCTION_STAGE_ON = "1";
    private final static BigDecimal HOUR24 = new BigDecimal("24"); // 24小时
    private final static BigDecimal TWO = new BigDecimal("2"); // 用于计算平分

    /**
     * 内衬胶自动排程
     *
     * @param scheduleDate 排程日期，格式：yyyy-MM-dd
     */
    @Transactional(rollbackFor=Exception.class)
    public void autoNcSchedule(String scheduleDate) {
        String userName = SecurityUtils.getUsername();  //用户名称
        String cxBatchNo = "";  //成型批次号
        String batchNo = this.createBatchNo(scheduleDate);  //内衬排程批次号
        Map<String, String> paramsMap = this.getParamsMap();  // 获取工序参数map
        Map<String, String> mapAssistSpec = this.mapAssistSpec(); //获得外协规格Map
        String productionStage = paramsMap.get(EngineConstants.PRODUCTION_STAGE_PRODUCE);  //仅投产阶段规格排产标识
        BigDecimal standardCurlLength = new BigDecimal(paramsMap.getOrDefault(EngineConstants.STANDARD_CRIMP_LENGTH, DEFAULT_STANDARD_CRIMP_LENGTH)); // 卷曲标准长度
        standardCurlLength = standardCurlLength.compareTo(BigDecimal.ZERO) > 0? standardCurlLength: new BigDecimal(DEFAULT_STANDARD_CRIMP_LENGTH); // 卷曲标准长度防错处理，不合法的配置都按默认值处理
//        BigDecimal curlDecimalRounding = new BigDecimal(paramsMap.getOrDefault(EngineConstants.CURL_DECIMAL_ROUNDING, DEFAULT_CURL_DECIMAL_ROUNDING)); // 卷曲数小数取整值，小数部分大于等于该值的进位，否则舍弃
//        BigDecimal midPlanQtyReference = new BigDecimal(paramsMap.getOrDefault(EngineConstants.MID_PLAN_QTY_REFERENCE, DEFAULT_MID_PLAN_QTY_REFERENCE)); // 夜班计划参考值，用于均衡
//        BigDecimal closeOutDays = new BigDecimal(paramsMap.getOrDefault(EngineConstants.CLOSE_OUT_DAYS, DEFAULT_CLOSE_OUT_DAYS)); // 共用规格收尾判断天数
        BigDecimal largeDemand = new BigDecimal(paramsMap.getOrDefault(EngineConstants.LARGE_DEMAND, DEFAULT_LARGE_DEMAND)); // 大需求量阈值
        BigDecimal largeDemandReduce = new BigDecimal(paramsMap.getOrDefault(EngineConstants.LARGE_DEMAND_REDUCE, DEFAULT_LARGE_DEMAND_REDUCE)); // 工装包含大卷数量
        BigDecimal delayPlanQty = new BigDecimal(paramsMap.getOrDefault(EngineConstants.DELAY_PLAN_QTY, DEFAULT_DELAY_PLAN_QTY)); // 工装包含大卷数量
        BigDecimal toolRollNum = new BigDecimal(paramsMap.getOrDefault(EngineConstants.TOOL_ROLL_NUM, DEFAULT_TOOL_ROLL_NUM)); // 工装包含大卷数量
        BigDecimal bisectThreshold = new BigDecimal(paramsMap.getOrDefault(EngineConstants.EQUAL_SHARE_THRESHOLD, DEFAULT_EQUAL_SHARE_THRESHOLD)); // 平分阈值
        double paramLossRate = getDouble(paramsMap.get(EngineConstants.LOSS_RATE));
        double mergeThreshold = getDouble(paramsMap.get(EngineConstants.MERGE_PLAN_THRESHOLD));
        BigDecimal productStockHour = new BigDecimal(paramsMap.getOrDefault(EngineConstants.PRODUCT_STOCK_HOUR, DEFAULT_PRODUCT_STOCK_HOUR));
        double productStockDay = productStockHour.divide(HOUR24, 2, RoundingMode.HALF_UP).doubleValue();
        List<NcScheduleResultVo> scheduleList = ncEngineMapper.statNcScheduleBase(scheduleDate, productionStage);  //根据成型排程记录 统计出 内衬胶排程记录基础数据
        if (scheduleList == null || scheduleList.isEmpty()) {
            log.info("根据成型排程记录 统计出 内衬胶排程记录基础数据 为空");
            autoScheduleLogService.insertNcScheduleLog(batchNo, "", "自动排程失败", "自动排程失败，原因：成型排程数据为空，或没有在施工信息中找到对应的物料"); //添加日志
            throw new RuntimeException(I18nUtil.getMessage("engine.auto.scheule.tip1"));
        }
        //过滤掉成型4个班的计划量都为0的数据
        scheduleList = scheduleList.stream().filter(s -> (s.getCxClass2Plan()+s.getCxClass3Plan()+s.getCxClass4Plan()+s.getCxClass5Plan())>0).collect(Collectors.toList());
        autoScheduleLogService.insertNcScheduleLog(batchNo, "", "根据成'型排程记录'统计出内衬胶排程记录基础数据",  toJSONString(scheduleList));
        this.ValidatedConstruction(scheduleDate, batchNo, productionStage, mapAssistSpec);   //证成型排程记录的胎胚code在施工表中是否都能找到对应记录，如果不能则提示
        Map<String, String> glueSeqMap = ncEngineGlueService.getGlueSeqMap();  //获取胶料序号map
        Map<String, String> specifyCanMachineMap = ncEngineMachineService.getSpecifyMachineMap(EngineConstants.JOB_TYPE_CAN); //获得内衬代码和定点机台的map
        Map<String, String> specifyNotMachineMap = ncEngineMachineService.getSpecifyMachineMap(EngineConstants.JOB_TYPE_NOT); //获得内衬代码和定点机台的不可作业map

        Map<String, Double> planStockMap = ncEngineStockService.getPlanStockMap(batchNo, scheduleDate, getDouble(paramsMap.get(EngineConstants.STOCK_LOSS_RATE)));  //计算内衬16点预计库存
        Map<String, Double> stockMap = this.loadNcStock(scheduleDate); // 加载库存
        Map<String, Double> lastDayMidPlanMap = this.loadLastDayMidPlan(scheduleDate); // 加载昨日早班计划
        Map<String, Double> lossRateMap = ncEngineLossService.getLossRateMap();   //损耗率map
        Map<String, NcMonthSurplusVo> monthSurplus = ncEngineMonthSurplusService.getMonthSurplus(scheduleDate);  //获得月度计划剩余量、完成量
        Map<String, BigDecimal> ncCurlLengthMap = ncEngineCurlRollService.getNcCurlLengthMap(); // 胎侧卷曲设置
        this.baseDataLog(batchNo, glueSeqMap, specifyCanMachineMap, planStockMap, lossRateMap, monthSurplus, paramsMap); //把基础数据假如到日志中
        Map<String, NcTotalPlanQtyVo> totalPlanQtyMap = new HashMap<>();  //每个生产线的计划量汇总MAP
        List<NcMachineInfo> allMachineList = ncEngineMachineService.listNcMachine();
        NcTotalPlanQtyVo totalPlanQtyVo = new NcTotalPlanQtyVo();  //胎面中班和夜班总计划量Vo
        for (NcScheduleResultVo scheduleVo : scheduleList) {
            cxBatchNo = scheduleVo.getCxBatchNo();
            scheduleVo.setBatchNo(batchNo);    //批次号
            String orderNo = this.createOrderNo(batchNo);   //创建工单号
            scheduleVo.setOrderNo(orderNo);
            BigDecimal curlLength = ncCurlLengthMap.getOrDefault(scheduleVo.getLiningCode(), standardCurlLength); // 卷曲长度
            if (curlLength.equals(BigDecimal.ZERO)) {
                curlLength = standardCurlLength;
            }
            BigDecimal toolCapacity = curlLength.multiply(toolRollNum); // 一个工装包含的卷数
            scheduleVo.getParams().put(EngineConstants.STANDARD_CRIMP_LENGTH, curlLength); // 一卷的长度
            scheduleVo.getParams().put(EngineConstants.TOOL_CAPACITY, toolCapacity); // 一个工装的长度
            scheduleVo.getParams().put(EngineConstants.LARGE_DEMAND_REDUCE, largeDemandReduce); // 大需求量不生产工装数
            scheduleVo.getParams().put(EngineConstants.DELAY_PLAN_QTY, delayPlanQty); // 低于参数的计划量可以推迟到下个班做

            scheduleVo.setGlueSeq(glueSeqMap.get(scheduleVo.getGlueCode()));  //胶料序号
            autoScheduleLogService.insertNcScheduleLog(batchNo, orderNo, "根据'胶料顺序集合'设置胶料序号",
                    logSplit("胶料顺序集合：" + toJSONString(glueSeqMap), "数据结果：" + toJSONString(scheduleVo))); //添加日志

//            this.chooseMachine(scheduleVo, specifyCanMachineMap);  //选择生产线
//            scheduleVo.setPlanStockQty(planStockMap.getOrDefault(scheduleVo.getLiningCode(), 0D));  //16点预计库存
            scheduleVo.setStockQty(stockMap.getOrDefault(scheduleVo.getLiningCode(), 0D));  // 库存
            scheduleVo.setSurplusQty(Optional.ofNullable(monthSurplus.get(scheduleVo.getLiningCode())).map(NcMonthSurplusVo::getMonthRemainQty).orElse(0D)); // 剩余量
            scheduleVo.setLastMidPlanQty(lastDayMidPlanMap.getOrDefault(scheduleVo.getLiningCode(), 0D)); // 上一天早班库存
            scheduleVo.setPlanStockQty(BigDecimalUtils.qtySub(BigDecimalUtil.add(scheduleVo.getStockQty(), scheduleVo.getLastMidPlanQty()), scheduleVo.getCxClass1Plan())); // 计算19点预计库存
            autoScheduleLogService.insertNcScheduleLog(batchNo, orderNo, "根据'16点预计库存集合'设置库存",
                    logSplit("16点预计库存集合：" + toJSONString(planStockMap), "数据结果：" + toJSONString(scheduleVo))); //添加日志

            this.newComputeSupplyTime(scheduleVo, scheduleVo.getPlanStockQty());  //库存供应时长
            this.computeNcPlanQty(scheduleVo, totalPlanQtyVo, lossRateMap, paramLossRate, mergeThreshold, toolCapacity, productStockDay, largeDemand, bisectThreshold);  //计算内衬中班和夜班计划量
//            this.computeNcCurlRoll(scheduleVo, ncCurlLengthMap, standardCurlLength, closeOutSpecList, curlDecimalRounding, totalPlanQtyMap); // 计算卷曲数
            this.setStatusAndCloseTip(scheduleVo, monthSurplus.get(scheduleVo.getLiningCode()), getDouble(paramsMap.get(EngineConstants.CLOSE_OUT_NUM)));  //设置收尾提示标识 和 生产状态字段

            if(BigDecimalUtil.add(scheduleVo.getCxClass1Plan(), scheduleVo.getCxClass2Plan(), scheduleVo.getCxClass3Plan(), scheduleVo.getCxClass4Plan()) == 0D) {
                //判断成型前4个班是否有计划量，若无计划，判断为新投产规格，半部件计划计划量放在“预计划”栏位中，中班和夜班计划都显示为0；若有计划，半部件计划正常排产
                scheduleVo.setPrePlanQty(BigDecimalUtil.add(scheduleVo.getDayPlanQty(), scheduleVo.getNightPlanQty()));
                scheduleVo.setDayPlanQty(0D);
                scheduleVo.setNightPlanQty(0D);
            }
            scheduleVo.setIsRelease(ApsConstant.NO_RELEASE);
            scheduleVo.setDelFlag(ApsConstant.DEL_FLAG_NORMAL);
            scheduleVo.setCreateTime(new Date());
            scheduleVo.setCreateBy(userName);
        }
//        this.equilibrium(scheduleList, paramsMap, totalPlanQtyMap);  //中班和夜班计排程计划量均衡处理
        this.equilibriumDay1(scheduleList, totalPlanQtyVo, paramsMap);
        this.equilibriumDay2(scheduleList, totalPlanQtyVo, paramsMap);  //中班和夜班计排程计划量均衡处理
//        this.glueMerge(batchNo, scheduleList, paramsMap.get(EngineConstants.GLUE_MERGE_THRESHOLD), paramsMap.get(EngineConstants.GLUE_MERGE_THRESHOLD_MAX));  //同胶料合并生产
        this.chooseMachineByCapacity(scheduleList, allMachineList, specifyCanMachineMap, specifyNotMachineMap);  //选择生产线
        this.setProduceOrder(scheduleList);  //设置白班和夜班的生产顺序

        List<NcScheduleResultVo> existScheduleList = this.ncEngineMapper.listNcEnginSchedule(scheduleDate);  //查询当天已经存在的排产记录
        this.syncNcScheduleToLog(scheduleDate);  //把排程数据同步到log表
        this.createScheduleRecord(scheduleDate, cxBatchNo, batchNo);  //创建自动排程记录

        List<NcScheduleResultVo> assistScheduleList = scheduleList.stream().filter(r -> mapAssistSpec.containsKey(r.getLiningCode())).collect(Collectors.toList()); //过滤出外协排程数据
        scheduleList = scheduleList.stream().filter(r -> !mapAssistSpec.containsKey(r.getLiningCode())).collect(Collectors.toList());  //过滤出非外协的排产数据
        if(StringUtils.isNotEmpty(assistScheduleList)) {
            ncEngineMapper.batchCreateAssistScheduleResult(assistScheduleList);   //批量新增外协排程结果数据
        }

        scheduleList = this.mergeExistSchedule(batchNo, scheduleList, existScheduleList);  //如果当天排程已经存在，则把当天的排程合并到 自动排程的列表中
        if(StringUtils.isNotEmpty(scheduleList)) {
            ncEngineMapper.batchCreateScheduleResult(scheduleList);   //批量新增非外协排程结果数据
        }
    }

    /**
     * 加载当天库存
     *
     * @param scheduleDate
     * @return
     */
    private Map<String, Double> loadNcStock(String scheduleDate) {
        return ncEngineStockMapper.listNcStock(scheduleDate).stream()
                .filter(v -> StringUtils.isNotEmpty(v.getLiningCode()))
                .collect(Collectors.toMap(NcStockVo::getLiningCode, NcStockVo::getStockNum));
    }

    /**
     * 加载上一天的早班计划
     *
     * @param scheduleDate
     * @return
     */
    private Map<String, Double> loadLastDayMidPlan(String scheduleDate) {
        return ncEngineStockMapper.listLastDayMidPlan(scheduleDate).stream()
                .filter(v -> StringUtils.isNotEmpty(v.getLiningCode()))
                .collect(Collectors.toMap(NcStockConsumeVo::getLiningCode, NcStockConsumeVo::getConsume));
    }

    /**
     * 均衡第一天夜班与第二天的计划
     *
     * @param scheduleList   排程列表
     * @param paramsMap      工序参数
     * @param totalPlanQtyVo 成型中班和夜班总计划量Vo
     */
    private void equilibriumDay1(List<NcScheduleResultVo> scheduleList, NcTotalPlanQtyVo totalPlanQtyVo, Map<String, String> paramsMap) {
//        BigDecimal midCapacity = new BigDecimal(paramsMap.getOrDefault("MID_CAPACITY", DEFAULT_MID_CAPACITY)); // 中班定额
        BigDecimal nightCapacity = new BigDecimal(paramsMap.getOrDefault("NIGHT_CAPACITY", DEFAULT_NIGHT_CAPACITY)); // 夜班定额
        BigDecimal standardCurlLength = new BigDecimal(paramsMap.getOrDefault(EngineConstants.STANDARD_CRIMP_LENGTH, DEFAULT_STANDARD_CRIMP_LENGTH)); // 卷曲标准长度
        BigDecimal bisectThreshold = new BigDecimal(paramsMap.getOrDefault(EngineConstants.EQUAL_SHARE_THRESHOLD, DEFAULT_EQUAL_SHARE_THRESHOLD)); // 平分阈值
        double midPlanQtyReference = nightCapacity.multiply(standardCurlLength).doubleValue(); // 平衡基准，夜班计划为低于夜班定额的最大数
        double totalDayPlanQty = totalPlanQtyVo.getTotalDayPlanQty(); // 夜班总计划量
        double totalNightPlanQty = totalPlanQtyVo.getTotalNightPlanQty(); // 早班总计划量
        double totalNextDayPlanQty = totalPlanQtyVo.getTotalNextDayPlanQty(); // 次日夜班总计划量
        // 再处理其余的
        double difNum = BigDecimalUtil.sub(totalDayPlanQty, midPlanQtyReference); // 早班和平均值的差值
        boolean isNightClassPass = difNum > 0; // 夜班是否超量
        scheduleList = scheduleList.stream()
                .sorted((r1, r2) -> {
                    if (isNightClassPass) {
                        BigDecimal initStock1 = BigDecimalUtils.add(r1.getStockQty(), r1.getLastMidPlanQty());
                        BigDecimal initStock2 = BigDecimalUtils.add(r2.getStockQty(), r2.getLastMidPlanQty());
                        BigDecimal cxClassPlan1 = BigDecimalUtils.add(r1.getCxClass1Plan(), r1.getCxClass2Plan());
                        BigDecimal cxClassPlan2 = BigDecimalUtils.add(r2.getCxClass1Plan(), r2.getCxClass2Plan());
                        BigDecimal classStock1 = BigDecimalUtils.sub(initStock1, cxClassPlan1);
                        BigDecimal classStock2 = BigDecimalUtils.sub(initStock2, cxClassPlan2);
                        // 夜班超量，将交接班库存较充足的转移到早班（倒序）
                        return classStock2.compareTo(classStock1);
                    } else {
                        BigDecimal classStock1 = BigDecimalUtils.sub(r1.getClassStock(), r1.getCxClass3Plan());
                        BigDecimal classStock2 = BigDecimalUtils.sub(r2.getClassStock(), r2.getCxClass3Plan());
                        // 早班超量，将交接班库存较充低的转移到夜班（顺序）
                        return classStock1.compareTo(classStock2);
                    }
                }).collect(Collectors.toList());
        for (NcScheduleResultVo scheduleVo: scheduleList) {
            boolean isCloseOutSpec = ApsConstant.STATUS_ENABLE.equals(scheduleVo.getCloseOutSpecFlag()); // 是否收尾规格
            BigDecimal dayPlanQty = BigDecimalUtils.valueOf(scheduleVo.getDayPlanQty());
            BigDecimal nightPlanQty = BigDecimalUtils.valueOf(scheduleVo.getNightPlanQty());
            BigDecimal nextDayPlanQty = BigDecimalUtils.valueOf(scheduleVo.getNextDayPlanQty());
            BigDecimal toolCapacity = (BigDecimal)scheduleVo.getParams().get(EngineConstants.TOOL_CAPACITY); // 满工装长度
//            BigDecimal cxPlanQty2 = BigDecimalUtils.add(scheduleVo.getCxClass3Plan(), scheduleVo.getCxClass4Plan()); // 第二天型需求总量
            BigDecimal dayAddPlan = BigDecimal.ZERO; // 夜班增加量
            BigDecimal nightAddPlan = BigDecimal.ZERO; // 早班增加量
            BigDecimal nextDayAddPlan = BigDecimal.ZERO; // 次日夜班增加量

            if (isNightClassPass) { // 夜班超量，则从夜班转移到隔天早班
                BigDecimal cxPlanQty1 = BigDecimalUtils.add(scheduleVo.getCxClass1Plan(), scheduleVo.getCxClass2Plan()); // 第一天成型需求量
                BigDecimal lackStock = BigDecimalUtils.sub(cxPlanQty1, BigDecimalUtils.add(scheduleVo.getStockQty(), scheduleVo.getLastMidPlanQty()));
                lackStock = lackStock.compareTo(BigDecimal.ZERO) <= 0? BigDecimal.ZERO: BigDecimalUtils.ceil(lackStock, toolCapacity);
                if (lackStock.compareTo(dayPlanQty) >= 0) { // 转移后剩余的计划量不能少于第一天的库存缺口
                    continue;
                }
                nightAddPlan = dayPlanQty.subtract(lackStock);
                nightAddPlan = BigDecimalUtils.least(nightAddPlan, difNum);
                if (!isCloseOutSpec) { // 非收尾计划，需要按工装容量取整
                    nightAddPlan = BigDecimalUtils.ceil(nightAddPlan, toolCapacity);
                }
                dayAddPlan = nightAddPlan.negate();
            } else if (nightPlanQty.compareTo(BigDecimal.ZERO) > 0) { // 隔天超量，且早班大于0，则从早班转移到夜班
                nightAddPlan = BigDecimalUtils.greatest(difNum, nightPlanQty.negate()); //负数，要取最接近0的（最大值）
                if (!isCloseOutSpec) { // 非收尾计划，需要按工装容量取整
                    nightAddPlan = BigDecimalUtils.ceil(nightAddPlan, toolCapacity);
                }
                dayAddPlan = nightAddPlan.negate();
                BigDecimal lastMidPlanQty = BigDecimalUtils.valueOf(scheduleVo.getLastMidPlanQty());
                // 如果夜班本身没有安排计划，且上一天早班有安排计划，需要检查加上转移量之后是否达到均分阈值
                if (dayPlanQty.compareTo(BigDecimal.ZERO) == 0 && lastMidPlanQty.compareTo(BigDecimal.ZERO) > 0 && dayAddPlan.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal newDayPlanQty = dayPlanQty.add(dayAddPlan);
                    if (newDayPlanQty.add(lastMidPlanQty).compareTo(bisectThreshold) <= 0) {
                        continue; // ，而上一天早班加夜班计划量没有达到均分阈值，则不能转移
                    }
                }
            } else {
                continue;
            }
            // 先算一下是否调整后差异反而更大
            double newTotalDayPlanQty = BigDecimalUtils.add(totalDayPlanQty, dayAddPlan).doubleValue();
            double newDifNum = BigDecimalUtil.sub(newTotalDayPlanQty, midPlanQtyReference); // 早班和平均值的差值
            if (Math.abs(newDifNum) > Math.abs(difNum)) { // 如果更大跳过该规格
                continue;
            }
            // 更新各班计划量
            scheduleVo.setDayPlanQty(dayPlanQty.add(dayAddPlan).doubleValue());
            scheduleVo.setNightPlanQty(nightPlanQty.add(nightAddPlan).doubleValue());
            scheduleVo.setNextDayPlanQty(nextDayPlanQty.add(nextDayAddPlan).doubleValue());
            if (dayAddPlan.compareTo(BigDecimal.ZERO) != 0) {
                scheduleVo.setClassStock(this.getClassStock(scheduleVo)); // 夜班计划有变动，需要重算交接班库存
            }
            totalDayPlanQty = newTotalDayPlanQty;
            totalNightPlanQty = BigDecimalUtils.add(totalNightPlanQty, nightAddPlan).doubleValue();
            totalNextDayPlanQty = BigDecimalUtils.add(totalNextDayPlanQty, nextDayAddPlan).doubleValue();
            difNum = newDifNum;
            if (isNightClassPass ^ difNum > 0) { // 如果计算前后差值符号相反则直接结束
                break;
            }
        }
        totalPlanQtyVo.setTotalDayPlanQty(totalDayPlanQty); // 早班总计划里量
        totalPlanQtyVo.setTotalNightPlanQty(totalNightPlanQty); // 早班总计划里量
        totalPlanQtyVo.setTotalNextDayPlanQty(totalNextDayPlanQty); // 次日夜班总计划量
    }

    /**
     * 计算交接班库存
     * @param scheduleVo
     * @return
     */
    private Double getClassStock(NcScheduleResultVo scheduleVo) {
        BigDecimal planQty = BigDecimalUtils.add(scheduleVo.getStockQty(), scheduleVo.getLastMidPlanQty(), scheduleVo.getDayPlanQty());
        BigDecimal cxPlanQty = BigDecimalUtils.add(scheduleVo.getCxClass1Plan(), scheduleVo.getCxClass2Plan());
        return planQty.subtract(cxPlanQty).doubleValue();
    }

    /**
     * 均衡第二天早夜班库存
     *
     * @param scheduleList    排程列表
     * @param totalPlanQtyVo  中班和夜班总计划量Vo
     * @param bisectThreshold 中夜班平分阈值，超过该数值的计划中夜班平分
     */
    private void equilibriumDay2(List<NcScheduleResultVo> scheduleList, NcTotalPlanQtyVo totalPlanQtyVo, Map<String, String> paramsMap) {
        if (CollectionUtil.isEmpty(scheduleList)) {
            return;
        }
        BigDecimal midCapacity = new BigDecimal(paramsMap.getOrDefault("MID_CAPACITY", DEFAULT_MID_CAPACITY)); // 中班定额
        BigDecimal nightCapacity = new BigDecimal(paramsMap.getOrDefault("NIGHT_CAPACITY", DEFAULT_NIGHT_CAPACITY)); // 夜班定额
        BigDecimal standardCurlLength = new BigDecimal(paramsMap.getOrDefault(EngineConstants.STANDARD_CRIMP_LENGTH, DEFAULT_STANDARD_CRIMP_LENGTH)); // 卷曲标准长度
        BigDecimal bisectThreshold = new BigDecimal(paramsMap.getOrDefault(EngineConstants.EQUAL_SHARE_THRESHOLD, DEFAULT_EQUAL_SHARE_THRESHOLD)); // 平分阈值
        BigDecimal toolRollNum = new BigDecimal(paramsMap.getOrDefault(EngineConstants.TOOL_ROLL_NUM, DEFAULT_TOOL_ROLL_NUM)); // 工装包含大卷数量
        Double toolCapacity = standardCurlLength.multiply(toolRollNum).doubleValue(); // 一个工装包含的卷数
        double nightPlanQtyReference = midCapacity.multiply(standardCurlLength).doubleValue(); // 早班平衡基准值
        this.equalShare(CollectionUtil.firstElement(scheduleList).getBatchNo(), scheduleList, totalPlanQtyVo, bisectThreshold); // 先做中夜班均衡
        double totalNightPlanQty = totalPlanQtyVo.getTotalNightPlanQty(); // 早班总计划里量
        double totalNextDayPlanQty = totalPlanQtyVo.getTotalNextDayPlanQty(); // 次日夜班总计划量
        double difNum = BigDecimalUtil.sub(totalNightPlanQty, nightPlanQtyReference); //早班和平衡基准值的差额
        boolean isNightClassPass = difNum > 0;  //true：早班超量，false：次日夜班超量
        if (Math.abs(difNum) <= toolCapacity) { // 如果差异少于一车，直接结束
            return;
        }
        isNightClassPass = difNum > 0;

//        if (isNightClassPass) {
//            // 次日夜班超量，说明库存充足，都再提前做隔天的，需要从供需比例较小的（库存比较小的）开始调整
//            scheduleList = scheduleList.stream().sorted(Comparator.comparing(NcScheduleResultVo::getNightPlanQty)).collect(Collectors.toList());
//        } else {
//            // 次日夜班超量，说明库存充足，都再提前做隔天的，需要从供需比例较小的（库存比较小的）开始调整
//            scheduleList = scheduleList.stream().sorted(Comparator.comparing(NcScheduleResultVo::getNextDayPlanQty)).collect(Collectors.toList());
//        }
        // 按早班库存缺口排序
        boolean isNightClassPassSort = isNightClassPass;
        scheduleList = scheduleList.stream().sorted((s1, s2) -> {
            BigDecimal lackStock1 = BigDecimalUtils.sub(s1.getClassStock(), s1.getCxClass3Plan());
            BigDecimal lackStock2 = BigDecimalUtils.sub(s2.getClassStock(), s2.getCxClass3Plan());
            if (isNightClassPassSort) { // 早班超量，要推迟，先处理缺口小的 （顺序）
                return lackStock1.compareTo(lackStock2);
            } else { // 次夜班超量，要提前，先处理缺口大的（倒序）
                return lackStock2.compareTo(lackStock1);
            }
        }).collect(Collectors.toList());

        for (NcScheduleResultVo scheduleVo : scheduleList) {
            BigDecimal nightPlanQty = BigDecimalUtils.valueOf(scheduleVo.getNightPlanQty());
            BigDecimal nextDayPlanQty = BigDecimalUtils.valueOf(scheduleVo.getNextDayPlanQty());
            BigDecimal nightAddPlan;
            BigDecimal nextDayAddPlan;
            if (scheduleVo.getIsEqualShare()) {
                continue; // 已均分的计划不需要处理
            }
            if (isNightClassPass) {
                // 计算早班库存缺口
                double lackStock = BigDecimalUtil.sub(scheduleVo.getCxClass3Plan(), scheduleVo.getClassStock());
                if (lackStock > 0 && nextDayPlanQty.doubleValue() < lackStock) { // 只要有库存缺口，且次日夜班计划量低于库存缺口，就不要转移到次日夜班
                    continue;
                }
                nightAddPlan = nightPlanQty.negate();
                nextDayAddPlan = nightAddPlan.negate();
            } else {
                nightAddPlan = nextDayPlanQty;
                nextDayAddPlan = nightAddPlan.negate();
            }
            if (nightAddPlan.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }
            BigDecimal newTotalNightPlanQty = BigDecimalUtils.add(totalNightPlanQty, nightAddPlan);
            BigDecimal newTotalNextDayPlanQty = BigDecimalUtils.add(totalNextDayPlanQty, nextDayAddPlan);
            BigDecimal newDifNum = BigDecimalUtils.sub(newTotalNightPlanQty, nightPlanQtyReference); // 计算后早班和平均值的差值
            if (newDifNum.abs().doubleValue() > Math.abs(difNum)) { // 如果更大跳过该规格
                continue;
            }

            scheduleVo.setNightPlanQty(BigDecimalUtils.add(nightPlanQty, nightAddPlan).doubleValue());
            scheduleVo.setNextDayPlanQty(BigDecimalUtils.add(nextDayPlanQty, nextDayAddPlan).doubleValue());
            totalNightPlanQty = newTotalNightPlanQty.doubleValue();
            totalNextDayPlanQty = newTotalNextDayPlanQty.doubleValue();
            difNum = newDifNum.doubleValue(); // 重算差异
            if (Math.abs(difNum) <= toolCapacity || isNightClassPass ^ difNum > 0) { // 差异少于一车、或者计算前后差值符号相反则直接结束
                break;
            }
        }
        totalPlanQtyVo.setTotalNightPlanQty(totalNightPlanQty); // 早班总计划里量
        totalPlanQtyVo.setTotalNextDayPlanQty(totalNextDayPlanQty); // 次日夜班总计划量
    }

    /**
     * 根据产能选机台
     *
     * @param scheduleList         排程数据
     * @param allMachineList       所有机台
     * @param specifyCanMachineMap 定点机台
     * @param specifyNotMachineMap 不可作业机台
     */
    private void chooseMachineByCapacity(List<NcScheduleResultVo> scheduleList, List<NcMachineInfo> allMachineList, Map<String, String> specifyCanMachineMap, Map<String, String> specifyNotMachineMap) {
        // 机台夜班已占用产能
        Map<Long, BigDecimal> midCapacityMap = new HashMap<>(16);
        // 机台白班已占用产能
        Map<Long, BigDecimal> nightCapacityMap = new HashMap<>(16);

        // 先对排产计划
        List<NcScheduleResultVo> chooseMachineScheduleList = scheduleList.stream().sorted((o1, o2) -> {
            Integer flag1 = specifyCanMachineMap.containsKey(o1.getLiningCode()) ? 1 : 2;
            Integer flag2 = specifyCanMachineMap.containsKey(o2.getLiningCode()) ? 1 : 2;
            if (flag1.compareTo(flag2) != 0) { // 先看哪个有定点机台，有定点机台的优先选机台
                return flag1.compareTo(flag2);
            }
            // 如果定点机台设置一样，则按计划量从大到小
            BigDecimal planQty1 = BigDecimalUtils.add(o1.getDayPlanQty(), o1.getNightPlanQty());
            BigDecimal planQty2 = BigDecimalUtils.add(o2.getDayPlanQty(), o2.getNightPlanQty());
            return planQty2.compareTo(planQty1);
        }).collect(Collectors.toList());

        // 根据夜班计划分配机台
        for (NcScheduleResultVo scheduleVo : chooseMachineScheduleList) {
            Double midPlanQty = scheduleVo.getDayPlanQty();
            if (midPlanQty == null || midPlanQty <= 0) {
                continue;
            }
            String classCode = String.valueOf(OpenMachineClassEnums.CLASS_TWO.getClassIndex()); // 夜班
            List<NcMachineInfo> optionalMachineList = this.searchOptionalMachineList(scheduleVo, classCode, midCapacityMap,
                    allMachineList, specifyCanMachineMap, specifyNotMachineMap); // 检索当班可选机台
            if (CollectionUtil.isEmpty(optionalMachineList)) {
                continue;
            }
            // 如果有匹配机台，则直接取第一个机台赋值
            NcMachineInfo machine = CollectionUtil.firstElement(optionalMachineList);
            Long machineId = machine.getId();
            scheduleVo.setMachineId(String.valueOf(machineId));
            //检查机台，如果早班不作业，则把计划量都转移到夜班
            if (!machine.getOpenMachineClass().contains(String.valueOf(OpenMachineClassEnums.CLASS_THREE.getClassIndex()))) {
                scheduleVo.setDayPlanQty(BigDecimalUtil.add(midPlanQty, scheduleVo.getNightPlanQty()));
                scheduleVo.setNightPlanQty(0D);
            }
            // 占用机台各班产能
            midCapacityMap.put(machineId, midCapacityMap.getOrDefault(machineId, BigDecimal.ZERO).add(BigDecimalUtils.valueOf(scheduleVo.getDayPlanQty())));
            nightCapacityMap.put(machineId, nightCapacityMap.getOrDefault(machineId, BigDecimal.ZERO).add(BigDecimalUtils.valueOf(scheduleVo.getNightPlanQty())));
            chooseMachineLog(scheduleVo, specifyCanMachineMap, specifyNotMachineMap); // 添加日志
        }

        // 剩余没有分配到机台的排程检查早班是否有可分配机台
        for (NcScheduleResultVo scheduleVo : chooseMachineScheduleList) {
            if (StringUtils.isNotEmpty(scheduleVo.getMachineId())) {
                continue;
            }
            // 早班
            String classCode = String.valueOf(OpenMachineClassEnums.CLASS_THREE.getClassIndex());
            List<NcMachineInfo> optionalMachineList = this.searchOptionalMachineList(scheduleVo, classCode, nightCapacityMap,
                    // 检索当班可选机台
                    allMachineList, specifyCanMachineMap, specifyNotMachineMap);
            if (CollectionUtil.isEmpty(optionalMachineList)) {
                continue;
            }
            // 如果有匹配机台，则直接取第一个机台赋值
            NcMachineInfo machine = CollectionUtil.firstElement(optionalMachineList);
            Long machineId = machine.getId();
            scheduleVo.setMachineId(String.valueOf(machineId));
            //检查机台，如果夜班不作业，则把计划量都转移到早班
            if (!machine.getOpenMachineClass().contains(String.valueOf(OpenMachineClassEnums.CLASS_TWO.getClassIndex()))) {
                scheduleVo.setNightPlanQty(BigDecimalUtil.add(scheduleVo.getDayPlanQty(), scheduleVo.getNightPlanQty()));
                scheduleVo.setDayPlanQty(0D);
            }
            // 占用机台各班产能
            midCapacityMap.put(machineId, midCapacityMap.getOrDefault(machineId, BigDecimal.ZERO).add(BigDecimalUtils.valueOf(scheduleVo.getDayPlanQty())));
            nightCapacityMap.put(machineId, nightCapacityMap.getOrDefault(machineId, BigDecimal.ZERO).add(BigDecimalUtils.valueOf(scheduleVo.getNightPlanQty())));
            chooseMachineLog(scheduleVo, specifyCanMachineMap, specifyNotMachineMap); // 添加日志
        }
    }

    /**
     * 选择排程对应机台列表
     *
     * @param scheduleVo           排程
     * @param classCode            班制
     * @param capacityMap          机台产能map
     * @param allMachineList       所有机台
     * @param specifyCanMachineMap 定点机台
     * @param specifyNotMachineMap 不可作业机台
     * @return 机台列表
     */
    private List<NcMachineInfo> searchOptionalMachineList(NcScheduleResultVo scheduleVo, String classCode, Map<Long, BigDecimal> capacityMap, List<NcMachineInfo> allMachineList, Map<String, String> specifyCanMachineMap, Map<String, String> specifyNotMachineMap) {
        String beadCode = scheduleVo.getLiningCode(); // 胎侧代码
        String mouthPlateCode = scheduleVo.getMouthPlateCode(); // 口型板code
        // 定点机台ID列表
        String specifyMachineIds = specifyCanMachineMap.get(beadCode);
        List<String> machineIds;
        // 如果有设置定点机台，需要把非定点全部过滤掉
        if (StringUtils.isNotEmpty(specifyMachineIds)) {
            machineIds = Arrays.asList(specifyMachineIds.split(","));
        } else {
            machineIds = new ArrayList<>(0);
        }
        // 可选机台
        List<NcMachineInfo> optionalMachineList = allMachineList.stream().filter(m -> {// 排除定点不可生产机台
                    String machineId = String.valueOf(m.getId());
                    String notMachine = specifyNotMachineMap.get(beadCode);
                    if (StringUtils.isEmpty(notMachine)) {
                        return true;
                    }
                    String[] notMachineIds = notMachine.split(",");
                    for (String notMachineId : notMachineIds) {
                        return Objects.equals(machineId, notMachineId);
                    }
                    return true;
                }).filter(m -> { // 如果有设置定点机台，则仅选中定点机台
                    if (CollectionUtils.isNotEmpty(machineIds)) {
                        return machineIds.contains(String.valueOf(m.getId()));
                    }
                    return true;
                }).filter(m -> StringUtils.contains(m.getOpenMachineClass(), classCode)) // 对应班次可用
                /*.filter(m -> {// 寸口需要在工装范围内
                    String toolingInfo = m.getToolingInfo(); // 工装信息
                    if (StringUtils.isNotEmpty(toolingInfo)) {
                        String[] toolingInfoArr = toolingInfo.split("-");
                        BigDecimal minDimension = BigDecimal.ZERO;
                        BigDecimal maxDimension = BigDecimal.ZERO;
                        if (toolingInfoArr.length > 0) {
                            String minDimensionStr = toolingInfoArr[0];
                            minDimension = NumberUtils.isDigits(minDimensionStr) ? new BigDecimal(minDimensionStr)
                                    : BigDecimal.ZERO; // 寸口限制最小
                        }
                        if (toolingInfoArr.length > 1) {
                            String maxDimensionStr = toolingInfoArr[1];
                            maxDimension = NumberUtils.isDigits(maxDimensionStr) ? new BigDecimal(maxDimensionStr)
                                    : BigDecimal.ZERO; // 寸口限制最大
                            if (minDimension.compareTo(maxDimension) > 0) {
                                maxDimension = minDimension;
                            }
                        }
                        if (minDimension.compareTo(dimension) > 0) {
                            return false;
                        }
                        if (maxDimension.compareTo(dimension) < 0) {
                            return false;
                        }
                        return true;
                    }
                    return false;
                })*/
                .sorted(new Comparator<NcMachineInfo>() {// 按剩余产能升序排序
                    @Override
                    public int compare(NcMachineInfo m1, NcMachineInfo m2) {
                        return capacityMap.getOrDefault(m1.getId(), BigDecimal.ZERO)
                                .compareTo(capacityMap.getOrDefault(m2.getId(), BigDecimal.ZERO));
                    }
                }).collect(Collectors.toList());
        return optionalMachineList;
    }

    /**
     * 设置生产线日志
     *
     * @param scheduleVo           排程数据
     * @param specifyCanMachineMap 定点机台限制作业
     * @param specifyNotMachineMap 定点机台不可作业
     */
    private void chooseMachineLog(NcScheduleResultVo scheduleVo, Map<String, String> specifyCanMachineMap, Map<String, String> specifyNotMachineMap) {
        StringBuffer logDetail = new StringBuffer();
        logDetail.append("①优先选择“定点机台中限制作业集合”匹配上的机台;②如果没有，在选择“口型板与机台对应关系集合”的机台信息，不过需要过滤掉'定点机台中不可作业'中的机台").append(division);
        logDetail.append("定点机台中限制作业集合：" + toJSONString(specifyCanMachineMap)).append(division);
        logDetail.append("定点机台中不可作业集合：" + toJSONString(specifyNotMachineMap)).append(division);
        logDetail.append("结果数据：" + toJSONString(scheduleVo)).append(division);
        autoScheduleLogService.insertNcScheduleLog(scheduleVo.getBatchNo(), scheduleVo.getOrderNo(), "设置生产线（机台）", logDetail.toString());
    }

    /**
     * 获取当天的收尾规格列表
     * @param scheduleDate	排程日期
     * @param closeOutDays	判断收尾的天数
     * @return
     */
	private List<String> getCloseOutSpecList(String scheduleDate, BigDecimal closeOutDays, String productionStage) {
		BigDecimal queryCloseOutDays = closeOutDays.compareTo(BigDecimal.ONE) >= 0? closeOutDays.subtract(BigDecimal.ONE): BigDecimal.ZERO; // 判断天数需要减1，0才是查1天的数据进行判断
		boolean isProductionStage = PRODUCTION_STAGE_ON.equals(productionStage); // 判断是否只看投产规格
		return ncEngineMapper.listCloseOutSpec(DateUtils.parseDate(scheduleDate), queryCloseOutDays.intValue(), isProductionStage);
	}

	/**
	 * 计算胎面卷曲长度
     *
	 * @param scheduleVo          胎面排程
	 * @param tmCurlLengthMap     各规格卷曲长度配置
	 * @param standardCurlLength  卷曲标准长度
	 * @param closeOutSpecList    收尾规格列表
	 * @param curlDecimalRounding 卷曲数小数取整值
	 * @param totalPlanQtyMap     每个生产线的计划量汇总MAP
	 */
	private void computeNcCurlRoll(NcScheduleResultVo scheduleVo, Map<String, BigDecimal> tmCurlLengthMap,
			BigDecimal standardCurlLength, List<String> closeOutSpecList, BigDecimal curlDecimalRounding,
			Map<String, NcTotalPlanQtyVo> totalPlanQtyMap) {
		String liningCode = scheduleVo.getLiningCode();
		if (closeOutSpecList.contains(liningCode)) { // 收尾规格，则直接返回
	        scheduleVo.setCloseOutSpecFlag(ApsConstant.STATUS_ENABLE); // 打上收尾标记
			return;
		}
		BigDecimal curlLength = tmCurlLengthMap.get(liningCode); // 本规格的卷曲长度
		if (curlLength == null || curlLength.compareTo(BigDecimal.ZERO) <= 0) { // 不合法的配置都按默认值为准
			curlLength = standardCurlLength;
		}
		BigDecimal dayPlanQty = BigDecimalUtil.getValue(scheduleVo.getDayPlanQty());
		BigDecimal nightPlanQty = BigDecimalUtil.getValue(scheduleVo.getNightPlanQty());
		BigDecimal totalPlanQty = dayPlanQty.add(nightPlanQty); // 本规格胎面的总计划量

		BigDecimal planNum = totalPlanQty.divide(curlLength, 1, RoundingMode.UP); // 卷数，保留1位小数
		// 卷数小数部分处理
		if (planNum.subtract(planNum.setScale(0, RoundingMode.DOWN)).compareTo(curlDecimalRounding) >= 0) {
			planNum = planNum.setScale(0, RoundingMode.UP); // 如果小数部分大于等于卷曲数小数取整值，直接进位
		} else if (planNum.compareTo(curlDecimalRounding) < 0) {
			planNum = planNum.setScale(0, RoundingMode.UP); // 如果原计划卷数比最小取整卷数少，也直接进位
		} else {
			planNum = planNum.setScale(0, RoundingMode.DOWN); // 其余情况舍去小数部分
		}
		BigDecimal newPlanQty = planNum.multiply(curlLength).setScale(0, RoundingMode.UP); // 新计划量
		BigDecimal planQtyDifference = newPlanQty.subtract(totalPlanQty);

		if (planQtyDifference.compareTo(BigDecimal.ZERO) == 0) {
			return;
		}
		boolean isDay = dayPlanQty.compareTo(BigDecimal.ZERO) > 0; // 是否安排在中班
		if (isDay) {
			dayPlanQty = newPlanQty;
			scheduleVo.setDayPlanQty(dayPlanQty.doubleValue());
		} else {
			nightPlanQty = newPlanQty;
			scheduleVo.setNightPlanQty(nightPlanQty.doubleValue());
		}

		// 将增加的量补到汇总值中
		String key = scheduleVo.getMachineId(); // 机台id作为Map的key
		key = StringUtils.isBlank(key) ? "" : key;
		NcTotalPlanQtyVo totalPlanQtyVo = totalPlanQtyMap.getOrDefault(key, new NcTotalPlanQtyVo()); // 取出对应生产线的计划量汇总对象
		if (isDay) {
			totalPlanQtyVo.setTotalDayPlanQty(
					BigDecimalUtil.getValue(totalPlanQtyVo.getTotalDayPlanQty()).add(planQtyDifference).doubleValue());
		} else {
			totalPlanQtyVo.setTotalNightPlanQty(
					BigDecimalUtil.getValue(totalPlanQtyVo.getTotalNightPlanQty()).add(planQtyDifference).doubleValue());
		}
		totalPlanQtyVo.setTotalPlanQty(
				BigDecimalUtil.add(totalPlanQtyVo.getTotalDayPlanQty(), totalPlanQtyVo.getTotalNightPlanQty()));
		totalPlanQtyMap.put(key, totalPlanQtyVo);
	}


    /**
     * 验证成型排程记录的胎胚code在施工表中是否都能找到对应记录，如果不能则提示
     * @param scheduleDate 排程日志
     * @param batchNo 批次号
     * @param productionStage 仅投产阶段规格排产标识
     */
    private void ValidatedConstruction(String scheduleDate, String batchNo, String productionStage, Map<String, String> mapAssistSpec) {
        List<EngineConstructionInfo> list = ncEngineMapper.listNcNeedConstruction(scheduleDate, productionStage);
        list = list.stream().filter(r -> !mapAssistSpec.containsKey(r.getInsideCode())).collect(Collectors.toList());  //校验忽略掉 外协规格，只校验 不是外协的规格
        for(EngineConstructionInfo construction : list) {
            List<String> errorColumns = new ArrayList<>();
            String embryoCode = construction.getEmbryoCode().split(",")[0];  //成型排程结果对应的胎胚代码
            String[] versionArray = construction.getBomDataVersion().split(",");
            String embryoVersion = versionArray.length > 0 ? versionArray[0] : "";  //施工版本
            if(construction.getEmbryoCode().split(",").length < 2) {
                //施工表胎胚代码为空
                errorColumns.add("\"" + I18nUtil.getMessage("ui.construction.embryoCode") + "\"");
            }
            if(versionArray.length < 2) {
                //施工表版本为空
                errorColumns.add("\"" + I18nUtil.getMessage("ui.construction.embryoVersion") + "\"");
            }
            if(StringUtils.isBlank(construction.getInsideCode())) {
                //施工表内衬代码为空
                errorColumns.add("\"" + I18nUtil.getMessage("ui.construction.insideCode") + "\"");
            }
            if(StringUtils.isBlank(construction.getInsideRubber())) {
                //施工表内衬胶料为空
                errorColumns.add("\"" + I18nUtil.getMessage("ui.construction.insideRubber") + "\"");
            }
            if(construction.getSidewallLength() == null || construction.getSidewallLength() == 0) {
                //施工表内衬长(胎侧长)为空
                errorColumns.add("\"" + I18nUtil.getMessage("ui.construction.sidewallLength") + "\"");
            }
            if(!errorColumns.isEmpty()) {
                String tip = StringUtils.format(I18nUtil.getMessage("engine.auto.scheule.construction.validate"), embryoCode, embryoVersion, String.join(",", errorColumns));
                autoScheduleLogService.insertNcScheduleLog(batchNo, "", "自动排程失败", tip); //添加日志
                throw new RuntimeException(tip);
            }
        }
    }

    /**
     * 把外协规格列表转成Map
     * @return
     */
    private Map<String, String> mapAssistSpec() {
        Map<String, String> map = new HashMap<>();
        List<String> listAssistSpec = this.ncEngineMapper.listAssistSpec();
        if(listAssistSpec == null || listAssistSpec.size() == 0) {
            return map;
        }
        for(String assistSpec : listAssistSpec) {
            map.put(assistSpec, "1");
        }
        return map;
    }

    /**
     * 内衬插单
     * @param scheduleVo
     */
    public int inertNcOrder(NcScheduleResultVo scheduleVo) {
        String scheduleDate = DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD, scheduleVo.getScheduleDate()); //排程日期
        List<NcScheduleResultVo> scheduleList = new ArrayList<>();
        scheduleList.add(scheduleVo);
        return this.batchSaveNcSchedule(scheduleDate, scheduleList, true);
    }

    /**
     * 批量更新或新增排程记录信息
     * @param scheduleDate 排程日志，格式：yyyy-MM-dd
     * @param scheduleList 排程数据
     */
    public int batchSaveNcSchedule(String scheduleDate, List<NcScheduleResultVo> scheduleList) {
        return this.batchSaveNcSchedule(scheduleDate, scheduleList, false);
    }

    /**
     * 批量更新或新增排程记录信息
     * @param scheduleDate 排程日志，格式：yyyy-MM-dd
     * @param scheduleList 排程数据
     * @param isUpdate 相同唯一键是否做更新操作。true：是
     */
    @Transactional(rollbackFor=Exception.class)
    public int batchSaveNcSchedule(String scheduleDate, List<NcScheduleResultVo> scheduleList, boolean isUpdate) {
        if(scheduleList == null || scheduleList.isEmpty()) {
            return -1;
        }
        String batchNo = "";
        if(isUpdate) {
            batchNo = ncEngineMapper.getNcCurrentBatchNo(scheduleDate);  //查询当前排程的批次号
        }
        if(StringUtils.isBlank(batchNo)) {
            //当前的批次号为空，说明还没”自动排程“，那么自己生成一个排程批次号
            batchNo = this.createBatchNo(scheduleDate);  //内衬排程批次号
            this.createScheduleRecord(scheduleDate, "", batchNo);  //创建自动排程记录
            this.syncNcScheduleToLog(scheduleDate);  //把排程数据同步到log表
        }
        autoScheduleLogService.insertNcScheduleLog(batchNo, "", "插单或批量导入初始数据", toJSONString(scheduleList));  //添加日志

        List<String> liningCodes = scheduleList.stream().map(NcScheduleResultVo::getLiningCode).collect(Collectors.toList());
        Map<String, String> paramsMap = this.getParamsMap();  // 获取工序参数map
        String productionStage = paramsMap.get(EngineConstants.PRODUCTION_STAGE_PRODUCE);  //仅投产阶段规格排产标识
        Map<String, NcScheduleBaseInfoVo> scheduleBaseInfoMap = getScheduleBaseInfoMap(scheduleDate, liningCodes, productionStage);  //根据内衬代码查询对应的内衬基础信息
        Map<String, String> glueSeqMap = ncEngineGlueService.getGlueSeqMap();  //获取胶料序号map
        Map<String, Double> planStockMap = ncEngineStockService.getPlanStockMap(batchNo, scheduleDate, getDouble(paramsMap.get(EngineConstants.STOCK_LOSS_RATE)));  //计算内衬16点预计库存
        Map<String, NcMonthSurplusVo> monthSurplus = ncEngineMonthSurplusService.getMonthSurplus(scheduleDate);  //获得月度计划剩余量、完成量
        autoScheduleLogService.insertNcScheduleLog(batchNo, "", "插单或批量导入基础数据", logSplit("半部件基础数据信息:" + toJSONString(scheduleBaseInfoMap),
                "胶料序号map：" + glueSeqMap, "16点预计库存：" + planStockMap, "月度计划剩余量、完成量：" + monthSurplus, "工序参数map：" + paramsMap));  //添加日志

        for(NcScheduleResultVo schedule : scheduleList) {
            schedule.setBatchNo(batchNo);  //批次号
            String orderNo = this.createOrderNo(batchNo); //工单号
            schedule.setOrderNo(orderNo);  //工单号
            NcScheduleBaseInfoVo baseInfoVo = scheduleBaseInfoMap.get(schedule.getLiningCode());
            if(baseInfoVo != null) {
                BeanUtils.copyProperties(baseInfoVo, schedule);
            }
            Double dayPlanQty = schedule.getDayPlanQty();  //中班计划量
            schedule.setDayPlanQty(dayPlanQty == null ? 0D : dayPlanQty);
            Double nightPlanQty = schedule.getNightPlanQty();  //夜班计划量
            schedule.setNightPlanQty(nightPlanQty == null ? 0D : nightPlanQty);

            schedule.setGlueSeq(glueSeqMap.get(schedule.getGlueCode()));  //胶料序号
            schedule.setStockQty(planStockMap.getOrDefault(schedule.getLiningCode(), 0D));  //16点预计库存
            this.newComputeSupplyTime(schedule, schedule.getStockQty());  //库存供应时长
            this.setStatusAndCloseTip(schedule, monthSurplus.get(schedule.getLiningCode()), getDouble(paramsMap.get(EngineConstants.CLOSE_OUT_NUM)));  //设置收尾提示标识 和 生产状态字段
            schedule.setIsRelease(ApsConstant.NO_RELEASE);
            schedule.setDelFlag(ApsConstant.DEL_FLAG_NORMAL);
            schedule.setCreateTime(new Date());
            schedule.setCreateBy(SecurityUtils.getUsername());
            schedule.setCloseOutSpecFlag(ApsConstant.STATUS_DISABLE);
        }
        autoScheduleLogService.insertNcScheduleLog(batchNo, "", "插单或批量导入最终数据", toJSONString(scheduleList));  //添加日志
        return ncEngineMapper.mergeNcScheduleResult(scheduleList);  //批量合并排程结果表（根据唯一字段，做更新或新增）
    }

    /**
     * 根据内衬代码查询对应的内衬基础信息
     * @param scheduleDate
     * @return
     */
    private Map<String, NcScheduleBaseInfoVo> getScheduleBaseInfoMap(String scheduleDate, List<String> liningCodes, String productionStage) {
        Map<String, NcScheduleBaseInfoVo> map = new HashMap<>();
        List<NcScheduleBaseInfoVo> list = ncEngineMapper.listNcScheduleBaseInfo(liningCodes, ""); //查询出胎面在施工表的基础信息
        if(!StringUtils.isEmpty(list)) {
            map = list.stream().collect(Collectors.toMap(NcScheduleBaseInfoVo::getLiningCode, baseInfoVo->baseInfoVo));
        }

        Map<String, NcScheduleBaseInfoVo> hasCxMap = new HashMap<>();
        List<NcScheduleResultVo> hasCxlist = ncEngineMapper.statNcScheduleBase(scheduleDate, productionStage); //查询出在有对应成型排程的胎面基础信息
        for(NcScheduleResultVo info : hasCxlist) {
            NcScheduleBaseInfoVo baseInfoVo = new NcScheduleBaseInfoVo();
            BeanUtils.copyProperties(info, baseInfoVo);
            hasCxMap.put(info.getLiningCode(), baseInfoVo);
        }

        map.putAll(hasCxMap);  //有对应成型排程的胎面基础信息 覆盖掉，没有成型排程的胎面基础信息
        return map;
    }

//    /**
//     * 转机台后，修改排程结果表相应字段数据
//     * @param oldMachineIds  转机台前，旧的机台id
//     * @param scheduleResult
//     */
//    public void changeNcMachine(String oldMachineIds, NcScheduleResult scheduleResult) {
//        String batchNo = scheduleResult.getBatchNo();  //批次号
//        String orderNo = scheduleResult.getOrderNo();  //工单号
//        autoScheduleLogService.insertNcScheduleLog(batchNo, orderNo, "转机台初始数据", logSplit("转机台前的机台ID：" + oldMachineIds, "页面提交的信息：" + toJSONString(scheduleResult)));  //添加日志
//        Map<String, Double> lossRateMap = ncEngineLossService.getLossRateMap();   //损耗率map
//        Map<String, String> paramsMap = this.getParamsMap();  // 获取工序参数map
//        double paramLossRate = getDouble(paramsMap.get(EngineConstants.LOSS_RATE));
//
//        //转机台后，不同机台的损耗率不一样，需要重新计算计划量
//        double oldLossRate = ncEngineLossService.getLossRate(scheduleResult.getLiningCode(), oldMachineIds, lossRateMap, paramLossRate);  //计算出转机台前的耗损率
//        double lossRate = ncEngineLossService.getLossRate(scheduleResult.getLiningCode(), scheduleResult.getMachineId(), lossRateMap, paramLossRate);
//        autoScheduleLogService.insertNcScheduleLog(batchNo, orderNo, "转机台需要根据不同机台耗损率重新计算计划量",
//                logSplit("重新计算计划量规则：先要根据之前机台的耗损率推算出之前在没有加上耗损率之前的计划量A，然后再用计划量A * 当前机台对应的耗损率，计算出最终的计划量", "转机台前的耗损率：" + oldLossRate + "转机台后的耗损率：" + lossRate));  //添加日志
//
//        Double dayPlanQty = scheduleResult.getDayPlanQty();  //中班计划量
//        if(dayPlanQty != null) {
//            dayPlanQty = BigDecimalUtil.div(dayPlanQty, 1 + oldLossRate, 4); //计算出之前没有加上损耗量的 计划量
//            dayPlanQty = BigDecimalUtil.add(dayPlanQty, BigDecimalUtil.mul(dayPlanQty, lossRate));
//            scheduleResult.setDayPlanQty(dayPlanQty);
//        }
//        Double nightPlanQty = scheduleResult.getNightPlanQty();  //夜班计划量
//        if(nightPlanQty != null) {
//            nightPlanQty = BigDecimalUtil.div(nightPlanQty, 1 + oldLossRate, 4); //计算出之前没有加上损耗量的 计划量
//            nightPlanQty = BigDecimalUtil.add(nightPlanQty, BigDecimalUtil.mul(nightPlanQty, lossRate));
//            scheduleResult.setNightPlanQty(nightPlanQty);
//        }
//        autoScheduleLogService.insertNcScheduleLog(batchNo, orderNo, "转机台结束后的排程数据", toJSONString(scheduleResult));  //添加日志
//    }

    /**
     * 确认自动排程机台
     * @param scheduleResult  排程信息
     */
    public void confirmNcMachine(NcScheduleResult scheduleResult) {
        String batchNo = scheduleResult.getBatchNo();  //批次号
        String orderNo = scheduleResult.getOrderNo();  //工单号
        autoScheduleLogService.insertNcScheduleLog(batchNo, orderNo, "确认机台初始数据", logSplit( "页面提交的信息：" + toJSONString(scheduleResult)));  //添加日志
        Map<String, Double> lossRateMap = ncEngineLossService.getLossRateMap();   //损耗率map
        Map<String, String> paramsMap = this.getParamsMap();  // 获取工序参数map
        double paramLossRate = getDouble(paramsMap.get(EngineConstants.LOSS_RATE));

        //耗损率
        double lossRate = ncEngineLossService.getLossRate(scheduleResult.getLiningCode(), scheduleResult.getMachineId(), lossRateMap, paramLossRate);  //计算出新机台的耗损率
        autoScheduleLogService.insertNcScheduleLog(batchNo, orderNo, "确认机台耗损率", "耗损率：" + lossRate);  //添加日志

        Double dayPlanQty = scheduleResult.getDayPlanQty();  //中班计划量
        if(dayPlanQty != null) {
            dayPlanQty = BigDecimalUtil.add(dayPlanQty, BigDecimalUtil.mul(dayPlanQty, lossRate));
            scheduleResult.setDayPlanQty(BigDecimalUtil.roundUp(dayPlanQty, 0));
        }
        Double nightPlanQty = scheduleResult.getNightPlanQty();  //夜班计划量
        if(nightPlanQty != null) {
            nightPlanQty = BigDecimalUtil.add(nightPlanQty, BigDecimalUtil.mul(nightPlanQty, lossRate));
            scheduleResult.setNightPlanQty(BigDecimalUtil.roundUp(nightPlanQty, 0));
        }
        autoScheduleLogService.insertNcScheduleLog(batchNo, orderNo, "确认机台结束后的排程数据", toJSONString(scheduleResult));  //添加日志
    }

    /**
     * 手动均衡和重新设置生产顺序
     * @param scheduleDate 排程日期,格式：yyyy-mm-dd
     */
    public void handEquilibriumAndProduceOrder(String scheduleDate) {
        List<NcScheduleResultVo> scheduleList = ncEngineMapper.listNcEnginSchedule(scheduleDate);
        if(StringUtils.isEmpty(scheduleList)) {
            return;
        }

        String batchNo = "";
        Map<String, NcTotalPlanQtyVo> totalPlanQtyMap = new HashMap<>();  //每个生产线的计划量汇总MAP
        for(NcScheduleResultVo schedule : scheduleList ) {
            batchNo = schedule.getBatchNo();
            Double dayPlanQty = schedule.getDayPlanQty();  //中班计划量
            Double nightPlanQty = schedule.getNightPlanQty();  //夜班计划量
            //如果中班计划量>0，那么中班计划量=中班计划量+夜班计划量，夜班计划量=0（不二次排程逻辑处理）
            if (dayPlanQty > 0) {
                dayPlanQty = BigDecimalUtil.add(dayPlanQty, nightPlanQty);
                nightPlanQty = 0D;
                schedule.setDayPlanQty(dayPlanQty);
                schedule.setNightPlanQty(nightPlanQty);
            }
            if(schedule.getMachineId() == null) {
                schedule.setMachineId("");
            }
            if(schedule.getSupplyTime() == null) {
                schedule.setSupplyTime(0D);
            }
            //计算中班总计划量 和 夜班总计划量
            this.groupTotalPlanQtyMap(schedule, totalPlanQtyMap);
        }

        Map<String, String> paramsMap = this.getParamsMap();  // 获取工序参数map
        this.equilibrium(scheduleList, paramsMap, totalPlanQtyMap);  //均衡
        BigDecimal equalShareThreshold = new BigDecimal(paramsMap.getOrDefault(EngineConstants.EQUAL_SHARE_THRESHOLD, DEFAULT_EQUAL_SHARE_THRESHOLD)); // 平分阈值
        this.equalShare(batchNo, scheduleList, null, equalShareThreshold);  //单规格排产数量达到设定值时，中夜班数量对半分
        this.setProduceOrder(scheduleList);  //生产顺序重新计算
        ncEngineMapper.createTempTable();
        ncEngineMapper.insertTempTable(scheduleList);
        ncEngineMapper.batchUpdateProduceOrder(scheduleDate, scheduleList);  //批量更新各班的计划量和生产顺序
//        ncEngineMapper.dropTempTable();
    }

    /**
     * 手动 同胶料合并生产
     * @param scheduleDate
     */
    public void handGlueMerge(String scheduleDate) {
        List<NcScheduleResultVo> scheduleList = ncEngineMapper.listNcEnginSchedule(scheduleDate);
        if(StringUtils.isEmpty(scheduleList)) {
            return;
        }
        Map<String, String> paramsMap = this.getParamsMap();  // 获取工序参数map
        String batchNo = scheduleList.get(0).getBatchNo();  //批次号
        this.glueMerge(batchNo, scheduleList, paramsMap.get(EngineConstants.GLUE_MERGE_THRESHOLD), paramsMap.get(EngineConstants.GLUE_MERGE_THRESHOLD_MAX));  //同胶料合并生产
        ncEngineMapper.createTempTable();
        ncEngineMapper.insertTempTable(scheduleList);
        ncEngineMapper.batchUpdatePlanQty(scheduleDate, scheduleList);  //批量更新各班的计划量
//        ncEngineMapper.dropTempTable();
    }

    /**
     * 中班和夜班计排程计划量均衡处理(根据生产线进行分组均衡)
     *
     * @param scheduleList   排程列表
     * @param paramsMap      工序参数
     * @param totalPlanQtyMap 每个生产线的计划量汇总MAP
     */
    private void equilibrium(List<NcScheduleResultVo> scheduleList, Map<String, String> paramsMap, Map<String, NcTotalPlanQtyVo> totalPlanQtyMap) {
        Map<String, List<NcScheduleResultVo>> map = scheduleList.stream().collect(Collectors.groupingBy(s->s.getMachineId()));
        scheduleList.clear();
        map.forEach((key, valueList) -> {
            this.equilibriumOne(valueList, paramsMap, totalPlanQtyMap.get(key));
            scheduleList.addAll(valueList);
        });
    }

    /**
     * 中班和夜班计排程计划量均衡处理
     *
     * @param scheduleList   排程列表
     * @param paramsMap      工序参数
     * @param totalPlanQtyVo 内衬中班和夜班总计划量Vo
     */
    private void equilibriumOne(List<NcScheduleResultVo> scheduleList, Map<String, String> paramsMap, NcTotalPlanQtyVo totalPlanQtyVo) {
        String batchNo = "";  //批次号
        String oldScheduleList = toJSONString(scheduleList);
        double difRate = getDoubleOrDefault(paramsMap.get(EngineConstants.PLAN_DIFFERENCE_RATE) ,100D);  //参数配置：中班总量和夜班总量差额百分比
        double supplyTimePass = getDoubleOrDefault(paramsMap.get(EngineConstants.SUPPLY_TIME_PASS),12D);;  //参数配置：库存供应时长小时数
        double difNum = BigDecimalUtil.sub(totalPlanQtyVo.getTotalDayPlanQty(), totalPlanQtyVo.getTotalNightPlanQty()); //中班和夜班计划量差额
        double actualDifRate = Math.abs(difNum) / totalPlanQtyVo.getTotalPlanQty() * 100;  //实际中班和夜班总计划量差额百分比
        if (actualDifRate > difRate) {
            //中班总量和夜班总量的差额百分比超过了参数配置的百分比，则需要做均衡处理
            boolean isDayClassPass = (difNum > 0);  //true：中班超量，false：夜班超量
            if (isDayClassPass) {
                //中班超量，排程结果按中班计划量，从小到大排序
                scheduleList = scheduleList.stream().sorted(Comparator.comparing(NcScheduleResultVo::getDayPlanQty)).collect(Collectors.toList());
            } else {
                //夜班超量，排程结果按夜班计划量，从小到大排序
                scheduleList = scheduleList.stream().sorted(Comparator.comparing(NcScheduleResultVo::getNightPlanQty)).collect(Collectors.toList());
            }
            //开始计划量均衡处理
            double lastDifRate = actualDifRate;  //上一次的中班总量和夜班总量的差额百分比
            for (NcScheduleResultVo resultVo : scheduleList) {
                batchNo = resultVo.getBatchNo();
                double supplyTime = resultVo.getSupplyTime() == null ? 0D : resultVo.getSupplyTime(); //库存供应时长
                double dayPlanQty = resultVo.getDayPlanQty();    //中班计划量
                double nightPlanQty = resultVo.getNightPlanQty();  //夜班计划量

                if (isDayClassPass) {  //中班超量，中班移到夜班
                    if (dayPlanQty == 0 || supplyTime <= supplyTimePass) {
                        //库存供应时长 超过supplyTimePass的， 才允许拆到夜班生产
                        continue;
                    }
                    double totalDayPlan = BigDecimalUtil.sub(totalPlanQtyVo.getTotalDayPlanQty(), dayPlanQty);
                    double totalNightPlan = BigDecimalUtil.add(totalPlanQtyVo.getTotalNightPlanQty(), dayPlanQty);
                    double newDifNum = BigDecimalUtil.sub(totalDayPlan, totalNightPlan); //中班和夜班计划量差额
                    double newDifRate = Math.abs(newDifNum) / totalPlanQtyVo.getTotalPlanQty() * 100;   //计算新的差额率
                    if(newDifRate >= lastDifRate) {
                        //如果调整后的差额率，比上次调整的高，那上次调整的数据是最均衡的。均衡处理全部结束
                        break;
                    }

                    resultVo.setDayPlanQty(0D);
                    resultVo.setNightPlanQty(BigDecimalUtil.add(dayPlanQty, nightPlanQty));
                    //重新计算中班和夜班的总计划量
                    totalPlanQtyVo.setTotalDayPlanQty(totalDayPlan);
                    totalPlanQtyVo.setTotalNightPlanQty(totalNightPlan);
                    lastDifRate = newDifRate;
                } else {  //夜班超量，夜班移到中班
                    if (nightPlanQty == 0) {
                        continue;
                    }
                    double totalDayPlan =  BigDecimalUtil.add(totalPlanQtyVo.getTotalDayPlanQty(), nightPlanQty);
                    double totalNightPlan = BigDecimalUtil.sub(totalPlanQtyVo.getTotalNightPlanQty(), nightPlanQty);
                    double newDifNum = BigDecimalUtil.sub(totalDayPlan, totalNightPlan); //中班和夜班计划量差额
                    double newDifRate = Math.abs(newDifNum) / totalPlanQtyVo.getTotalPlanQty() * 100;   //计算新的差额率
                    if(newDifRate >= lastDifRate) {
                        //如果调整后的差额率，比上次调整的高，那上次调整的数据是最均衡的。均衡处理全部结束
                        break;
                    }

                    resultVo.setDayPlanQty(BigDecimalUtil.add(dayPlanQty, nightPlanQty));
                    resultVo.setNightPlanQty(0D);
                    //重新计算中班和夜班的总计划量
                    totalPlanQtyVo.setTotalDayPlanQty(totalDayPlan);
                    totalPlanQtyVo.setTotalNightPlanQty(totalNightPlan);
                    lastDifRate = newDifRate;
                }
            }
        }
        this.equilibriumLog(batchNo, oldScheduleList, scheduleList, paramsMap, totalPlanQtyVo);  //添加日志
    }

    /**
     * 单规格排产数量达到设定值时，中夜班计划平分
     * @param scheduleList 排程列表
     * @param bisectThreshold  各班计划量均分阈值
     */
    private void equalShare(String batchNo, List<NcScheduleResultVo> scheduleList, NcTotalPlanQtyVo totalPlanQtyVo, BigDecimal bisectThreshold) {
        double totalNightPlanQty = 0D;
        double totalNextDayPlanQty = 0D;
        if (totalPlanQtyVo != null) {
            totalNightPlanQty = totalPlanQtyVo.getTotalNightPlanQty();
            totalNextDayPlanQty = totalPlanQtyVo.getTotalNextDayPlanQty();
        }
        // 次日早夜班总计划量超过阈值的平分中夜班计划量
        for (NcScheduleResultVo scheduleVo : scheduleList) {
            BigDecimal toolCapacity = (BigDecimal)scheduleVo.getParams().get(EngineConstants.TOOL_CAPACITY); // 满工装长度
            BigDecimal nightPlanQty = BigDecimalUtils.valueOf(scheduleVo.getNightPlanQty());
            BigDecimal nextDayPlanQty = BigDecimalUtils.valueOf(scheduleVo.getNextDayPlanQty());
            BigDecimal totalPlanQty = BigDecimalUtils.add(nightPlanQty, nextDayPlanQty);
            if (totalPlanQty.compareTo(bisectThreshold) > 0) { // 隔天总计划量超过均分阈值
                BigDecimal avgPlanQty = BigDecimalUtils.ceil(BigDecimalUtils.half(totalPlanQty), toolCapacity); // 超过的要将计划均分到每个班
                boolean isNightClassLarge = totalNightPlanQty > totalNextDayPlanQty;
                BigDecimal newNightPlanQty = isNightClassLarge? totalPlanQty.subtract(avgPlanQty): avgPlanQty;
                BigDecimal newNextDayPlanQty = !isNightClassLarge? totalPlanQty.subtract(avgPlanQty): avgPlanQty;
                scheduleVo.setNightPlanQty(newNightPlanQty.doubleValue());
                scheduleVo.setNextDayPlanQty(newNextDayPlanQty.doubleValue());
                scheduleVo.setIsEqualShare(true);
                // 重算总量
                totalNightPlanQty = BigDecimalUtil.add(totalNightPlanQty, newNightPlanQty.subtract(nightPlanQty).doubleValue());
                totalNextDayPlanQty = BigDecimalUtil.add(totalNextDayPlanQty, newNextDayPlanQty.subtract(nextDayPlanQty).doubleValue());
                continue;
            }
            // 未达到均分阈值的规格，要集中到一个班生产
            if (scheduleVo.getNightPlanQty() > 0) { // 早班有需求量则合并到早班，否则合并到次日夜班
                scheduleVo.setNightPlanQty(totalPlanQty.doubleValue());
                scheduleVo.setNextDayPlanQty(0D);
                totalNightPlanQty = BigDecimalUtil.add(totalNightPlanQty, nextDayPlanQty.doubleValue());
                totalNextDayPlanQty = BigDecimalUtil.sub(totalNextDayPlanQty, nextDayPlanQty.doubleValue());
            } else {
                scheduleVo.setNightPlanQty(0D);
                scheduleVo.setNextDayPlanQty(totalPlanQty.doubleValue());
                totalNightPlanQty = BigDecimalUtil.sub(totalNightPlanQty, nightPlanQty.doubleValue());
                totalNextDayPlanQty = BigDecimalUtil.add(totalNextDayPlanQty, nightPlanQty.doubleValue());
            }
            scheduleVo.setIsEqualShare(false);
        }
        if (totalPlanQtyVo != null) {
            totalPlanQtyVo.setTotalNightPlanQty(totalNightPlanQty); // 早班总计划量
            totalPlanQtyVo.setTotalNextDayPlanQty(totalNextDayPlanQty); // 次日夜班总计划量
        }
        autoScheduleLogService.insertNcScheduleLog(batchNo, "", "单规格排产数量达到设定值时，中夜班数量对半分", logSplit("班计划量均分阈值:" + bisectThreshold,
                "均分后排程数据：" + toJSONString(scheduleList)));  //添加日志
    }

    /**
     * 同一个机台，胶料一样的排程记录，供应时长有一个小于等于{GLUE_MERGE_THRESHOLD}参数，则计划量都归并到中班;
     * 反之如果供应时长都大于{GLUE_MERGE_THRESHOLD}参数，则计划量都归并到夜班，在此情况下其中要是有记录的供应时长又大于{GLUE_MERGE_THRESHOLD_MAX}参数，则把计划量归并到【预计划】字段中，中班和夜班计划量变0
     *
     * @param scheduleList 排程列表
     * @param glueMergethreshold  同胶料合并生产预计库存可供应时长参数
     * @param glueMergethresholdMax  同胶料归并生产可供应时长(MAX)
     */
    private void glueMerge(String batchNo, List<NcScheduleResultVo> scheduleList, String glueMergethreshold, String glueMergethresholdMax) {
        Double threshold = 12D;
        Double thresholdMax = 28D;
        try {
            threshold = Double.parseDouble(glueMergethreshold);
        } catch (NumberFormatException e) {
            log.error("同胶料合并生产预计库存可供应时长参数转换错误");
        }
        try {
            thresholdMax = Double.parseDouble(glueMergethresholdMax);
        } catch (NumberFormatException e) {
            log.error("同胶料合并生产预计库存可供应时长(Max)参数转换错误");
        }

        //根据机台+胶料进行分组
        Map<String, List<NcScheduleResultVo>> groupMap = scheduleList.stream().collect(Collectors.groupingBy(v -> v.getGlueCode() + v.getMachineId()));
        scheduleList.clear();

        for(List<NcScheduleResultVo> list : groupMap.values()) {
            boolean isPassParam = this.compareSupplyTime(list, threshold);  //判断集合中的库存供应时长 是否 有小于参数值的

            for(NcScheduleResultVo scheduleVo : list) {
                Double dayPlanQty = scheduleVo.getDayPlanQty();
                Double nightPlanQty = scheduleVo.getNightPlanQty();
                if(isPassParam) {
                    //库存供应时长 有小于 {GLUE_MERGE_THRESHOLD}参数值.计划量全归并到中班
                    scheduleVo.setDayPlanQty(BigDecimalUtil.add(dayPlanQty, nightPlanQty));
                    scheduleVo.setNightPlanQty(0D);
                } else {
                    //如果供应时长都大于{GLUE_MERGE_THRESHOLD}参数，则计划量都归并到夜班，在此情况下其中要是有记录的供应时长又大于{GLUE_MERGE_THRESHOLD_MAX}参数，则把计划量归并到【预计划】字段中，中班和夜班计划量变0
                    Double supplyTime = scheduleVo.getSupplyTime();
                    if(supplyTime > thresholdMax ) {
                        //则把计划量归并到【预计划】字段中，中班和夜班计划量变0
                        if(scheduleVo.getDayPlanQty() > 0 || scheduleVo.getNightPlanQty() > 0) {
                            scheduleVo.setPrePlanQty(BigDecimalUtil.add(dayPlanQty, nightPlanQty));  //设置预计库存
                            scheduleVo.setDayPlanQty(0D);
                            scheduleVo.setNightPlanQty(0D);
                        }
                    } else {
                        //计划量都归并到夜班
                        scheduleVo.setDayPlanQty(0D);
                        scheduleVo.setNightPlanQty(BigDecimalUtil.add(dayPlanQty, nightPlanQty));
                    }
                }
            }
            scheduleList.addAll(list);
        }
        autoScheduleLogService.insertNcScheduleLog(batchNo, "", "同胶料合并生产", logSplit("同胶料合并生产预计库存可供应时长参数:" + glueMergethreshold,
                "同胶料合并生产后排程数据：" + toJSONString(scheduleList)));  //添加日志
    }

    /**
     * 判断集合中是否有 库存供应时长 小于 参数值
     * @param list
     * @param equalShareThreshold 同胶料合并生产预计库存可供应时长参数
     * @return
     */
    private boolean compareSupplyTime(List<NcScheduleResultVo> list, Double equalShareThreshold) {
        for(NcScheduleResultVo schedule : list) {
            if(schedule.getSupplyTime() <= equalShareThreshold) {
                return true;
            }
        }
        return false;
    }

    /**
     * 计算每个生产线的中班总计划量、夜班总计划量，以及总计划量
     * @param scheduleVo 排程数据
     * @param totalPlanQtyMap 每个生产线的计划量汇总MAP
     */
    private void groupTotalPlanQtyMap(NcScheduleResultVo scheduleVo, Map<String, NcTotalPlanQtyVo> totalPlanQtyMap) {
        String key = scheduleVo.getMachineId();  //机台id作为Map的key
        key = StringUtils.isBlank(key) ? "" : key;
        NcTotalPlanQtyVo totalPlanQtyVo = totalPlanQtyMap.getOrDefault(key, new NcTotalPlanQtyVo());  //取出对应生产线的计划量汇总对象

        Double dayPlanQty = scheduleVo.getDayPlanQty();  //中班计划量
        Double nightPlanQty = scheduleVo.getNightPlanQty();  //夜班计划量
        totalPlanQtyVo.setTotalDayPlanQty(BigDecimalUtil.add(totalPlanQtyVo.getTotalDayPlanQty(), dayPlanQty));
        totalPlanQtyVo.setTotalNightPlanQty(BigDecimalUtil.add(totalPlanQtyVo.getTotalNightPlanQty(), nightPlanQty));
        totalPlanQtyVo.setTotalPlanQty(BigDecimalUtil.add(totalPlanQtyVo.getTotalDayPlanQty(), totalPlanQtyVo.getTotalNightPlanQty()));
        totalPlanQtyMap.put(key, totalPlanQtyVo);  //计算完毕后，把生产线的计划量汇总对象重新存入map中
    }

    /**
     * 均衡日志
     * @param scheduleList
     * @param paramsMap
     * @param totalPlanQtyVo
     */
    private void equilibriumLog(String batchNo, String oldScheduleList, List<NcScheduleResultVo> scheduleList, Map<String, String> paramsMap, NcTotalPlanQtyVo totalPlanQtyVo) {
        StringBuffer logDetail = new StringBuffer("");
        logDetail.append("对排产结果进行均衡操作。中班总量和夜班总量的差额百分比超过了参数配置的百分比，则需要做均衡处理，也就是说要把其中一班的计划量合并到另外一班，" +
                "一直合并到中班和夜班计划量总量的差额不超过参数配置的百分比。其中中班合并到夜班还需要遵循一个规则，就是只有库存供应时长必须要大于参数配置的值的时候，才允许从中班合并到夜班。").append(division);
        logDetail.append("各班总计划量：" + toJSONString(totalPlanQtyVo)).append(division);
        logDetail.append("参数配置集合，这里要用到‘PLAN_DIFFERENCE_RATE（中班总量和夜班总量差额百分比）’和‘SUPPLY_TIME_PASS（库存供应时长小时数）’：" + toJSONString(paramsMap)).append(division);
        logDetail.append("均衡前的排程数据列表：" + oldScheduleList).append(division);
        logDetail.append("均衡后的排产数据列表：" + toJSONString(scheduleList));
        autoScheduleLogService.insertNcScheduleLog(batchNo, "", "计划量均衡处理", logDetail.toString());
    }

    /**
     * 根据机台+胶料进行分组，然后在根据库存供应时长，设置中班和夜班的生产顺序（有计划量的才设置生产顺序）
     * @param scheduleList
     */
    private void setProduceOrder(List<NcScheduleResultVo> scheduleList) {
        //根据机台+胶料进行分组
        Map<String, List<NcScheduleResultVo>> groupMap = scheduleList.stream().collect(Collectors.groupingBy(v -> v.getGlueCode() + v.getMachineId()));
        scheduleList.clear();

        for(List<NcScheduleResultVo> list : groupMap.values()) {
            int dayProduceOrder = 1; //白班生产顺序
            int nightProduceOrder = 1;  //夜班生产顺序
            //根据库存供应时长升序排序
            list = list.stream().sorted(Comparator.comparing(NcScheduleResultVo::getSupplyTime)).collect(Collectors.toList());
            for(NcScheduleResultVo scheduleVo : list) {
                Double dayPlanQty = scheduleVo.getDayPlanQty();
                Double nightPlanQty = scheduleVo.getNightPlanQty();
                if(dayPlanQty > 0) {
                    scheduleVo.setDayProduceOrder(dayProduceOrder++);
                }
                if(nightPlanQty > 0) {
                    scheduleVo.setNightProduceOrder(nightProduceOrder++);
                }
                autoScheduleLogService.insertNcScheduleLog(scheduleVo.getBatchNo(), scheduleVo.getOrderNo(), "设置生产顺序字段",
                        logSplit("根据机台+胶料进行分组，然后在根据库存供应时长(从小到大)，设置中班和夜班的生产顺序（有计划量的才设置生产顺序）", "设置后的排程数据：" + toJSONString(scheduleVo)));  //添加日志
            }
            scheduleList.addAll(list);
        }
    }

    /**
     * 设置收尾提示标识 和 生产状态字段
     * @param scheduleResultVo
     * @param monthSurplusVo
     * @param closeOutNum  参数配置表设置的 提示收尾阈值
     */
    private void setStatusAndCloseTip(NcScheduleResultVo scheduleResultVo, NcMonthSurplusVo monthSurplusVo, Double closeOutNum) {
        if(monthSurplusVo == null) {
            scheduleResultVo.setMarkCloseOutTip(EngineConstants.CLOSE_TIP_NOT);
            scheduleResultVo.setProductionStatus(EngineConstants.PRODUCTION_STATUS_NOT);
            return;
        }
        Double monthFinishQty = monthSurplusVo.getMonthFinishQty();  //月度计划完成量
        Double monthRemainQty = monthSurplusVo.getMonthRemainQty();  //月度计划剩余量
        if(monthRemainQty < closeOutNum) {
            //剩余量小宇等于“临近收尾阈值”，设置收尾提示
            scheduleResultVo.setMarkCloseOutTip(EngineConstants.CLOSE_TIP_NEED);
        } else {
            scheduleResultVo.setMarkCloseOutTip(EngineConstants.CLOSE_TIP_NOT);
        }
        autoScheduleLogService.insertNcScheduleLog(scheduleResultVo.getBatchNo(), scheduleResultVo.getOrderNo(), "设置收尾提示标识markCloseOutTip",
                logSplit("剩余量小宇等于“临近收尾阈值”，设置收尾提示","月度计划剩余量：" + monthRemainQty + ",提示收尾阈值：" + closeOutNum, "最终的排程数据：" + toJSONString(scheduleResultVo)));  //添加日志

        if(monthFinishQty == 0D) {
            //没有完成量
            scheduleResultVo.setProductionStatus(EngineConstants.PRODUCTION_STATUS_NOT);
        } else if(monthFinishQty > 0D && monthRemainQty > 0) {
            //完成量大于0，月度计划量也大于0，说明出于生产中
            scheduleResultVo.setProductionStatus(EngineConstants.PRODUCTION_STATUS_ING);
        } else if(monthRemainQty <= 0) {
            //月度计划量小于等于0，说明出于生产完成
            scheduleResultVo.setProductionStatus(EngineConstants.PRODUCTION_STATUS_FINISH);
        }
        autoScheduleLogService.insertNcScheduleLog(scheduleResultVo.getBatchNo(), scheduleResultVo.getOrderNo(), "修改生产状态productionStatus",
                logSplit("①完成量为0，对应生产状态：未生产;②完成量大于0，月度计划量也大于0，说明出于生产中;③月度计划量小于等于0，说明出于生产完成",
                        "月度计划剩余量：" + monthRemainQty + ",月度计划完成量：" + monthFinishQty, "最终的排程数据：" + toJSONString(scheduleResultVo)));  //添加日志
    }

    /**
     * 如果当天排程已经存在，则把当天的排程合并到 自动排程的列表中
     * @param batchNo   批次号
     * @param autoScheduleList   自动排程列表
     * @param existScheduleList  当天已经存在的排产记录
     */
    private List<NcScheduleResultVo> mergeExistSchedule(String batchNo, List<NcScheduleResultVo> autoScheduleList, List<NcScheduleResultVo> existScheduleList) {
        if(StringUtils.isEmpty(existScheduleList)) {
            return autoScheduleList;
        }
        List<NcScheduleResultVo> mergeList = new ArrayList<>();

        Map<String, List<NcScheduleResultVo>> existScheduleMap = existScheduleList.stream().filter(s->s.getPublishSuccessCount()>0)
                .collect(Collectors.groupingBy(NcScheduleResultVo::getLiningCode)); //拿到重排前，已经有发布给MES的排产数据。key为 半部件规格代码

        for(NcScheduleResultVo autoSchedule : autoScheduleList) {
            List<NcScheduleResultVo> existScheduleGroupList = existScheduleMap.get(autoSchedule.getLiningCode());

            if(existScheduleGroupList != null && existScheduleGroupList.size() == 1) {
                //对应规格重排前已经发布，并且此规格重排前只有一条排程记录（只对应了一个机台）
                NcScheduleResultVo existSchedule = existScheduleGroupList.get(0);
                //重排前的数据如果已经发布过，在重新排程后仍有相应的生产需求，计划量按照重新自动排程的计划量安排；订单号需要和之前发布个mes的订单号一致
                autoSchedule.setOrderNo(existSchedule.getOrderNo());  //订单号
                autoSchedule.setPublishSuccessCount(existSchedule.getPublishSuccessCount());
                autoSchedule.setNewestPublishTime(existSchedule.getNewestPublishTime());
                autoSchedule.setIsRelease(ApsConstant.WAIT_RELEASING);  //发布状态修改
                autoSchedule.setMachineId(existSchedule.getMachineId());  //机台沿用重排前的机台
                mergeList.add(autoSchedule);
            } else if(existScheduleGroupList != null && existScheduleGroupList.size() > 1) {
                //对应规格重排前已经发布，并且此规格重排前只有多条排程记录（对应了多个机台）。那需要保留重排之前的排产，并且要把此规格重排后的各班的计划量，拼接到备注中
                String remarkTip = I18nUtil.getMessage("reschedule.double.spec.remark");
                remarkTip = StringUtils.format(remarkTip, stripZeros(autoSchedule.getDayPlanQty()), stripZeros(autoSchedule.getNightPlanQty()));
                for(NcScheduleResultVo existSchedule : existScheduleGroupList) {
                    existSchedule.setBatchNo(batchNo);
                    existSchedule.setRemark(remarkTip);
                    mergeList.add(existSchedule);
                }
            } else {
                //对应的规格，重排前没有找到相应记录
                mergeList.add(autoSchedule);
            }
            existScheduleMap.remove(autoSchedule.getLiningCode());
        }

        //重排前的已发布的规格如果没有在重排后的列表中，则需要把对应的规格也加入到最新的排程列表中
        for(List<NcScheduleResultVo> list : existScheduleMap.values()) {
            list.forEach(r->r.setBatchNo(batchNo));
            mergeList.addAll(list);
        }
        return mergeList;
    }

    /**
     * 创建自动排程记录
     *
     * @param scheduleDate 排程日期
     * @param cxBatchNo    对成型批次号
     * @param batchNo      内衬批次号
     */
    private void createScheduleRecord(String scheduleDate, String cxBatchNo, String batchNo) {
        Map<String, Object> params = new HashMap<>();
        params.put("scheduleDate", scheduleDate);
        params.put("cxBatchNo", cxBatchNo);
        params.put("batchNo", batchNo);
        params.put("userName", SecurityUtils.getUsername());  //用户名
        ncEngineMapper.createScheduleRecord(params);
    }

    /**
     * 把排程数据同步到log表
     *
     * @param scheduleDate 排程日期，格式：yyyy-MM-dd
     */
    private void syncNcScheduleToLog(String scheduleDate) {
        ncEngineMapper.syncNcScheduleToLog(scheduleDate);
        ncEngineMapper.deleteNcSchedule(scheduleDate);
        ncEngineMapper.deleteNcAssistSchedule(scheduleDate);
    }

    /**
     * 生产线挑选(优先选择“定点机台”匹配上的机台，如果没有，在选择“口型板”的机台信息)
     *
     * @param scheduleVo
     * @param specifyMachineMap
     */
    private void chooseMachine(NcScheduleResultVo scheduleVo, Map<String, String> specifyMachineMap) {
        String liningCode = scheduleVo.getLiningCode();  //内衬代码
        String machineIds = specifyMachineMap.get(liningCode);
        scheduleVo.setMachineId(machineIds == null ? "" : machineIds);
    }

    /**
     * （新）计算并设置供成型库存供应时长（小时）。
     * 具体算法：从1班开始判断，预计库存-1班的计划大于等于0时，供应时长+8小时；预计库存-1班计划-2班计划大于等于0时，供应时长+16小时；
     *         预计库存-1班计划-2班计划-3班计划小于0，供应时长=16个小时+（((预计库存-1班计划-2班计划)/3班计划)*8）
     * @param scheduleVo
     * @param stockQty
     */
    private void newComputeSupplyTime(NcScheduleResultVo scheduleVo,  Double stockQty) {
        Double cxClass1Plan = (scheduleVo.getCxClass1Plan() == null ? 0D : scheduleVo.getCxClass1Plan());  //对应成型一班的计划量
        Double cxClass2Plan = (scheduleVo.getCxClass2Plan() == null ? 0D : scheduleVo.getCxClass2Plan());  //对应成型二班的计划量
        Double cxClass3Plan = (scheduleVo.getCxClass3Plan() == null ? 0D : scheduleVo.getCxClass3Plan());  //对应成型三班的计划量
        Double cxClass4Plan = (scheduleVo.getCxClass4Plan() == null ? 0D : scheduleVo.getCxClass4Plan());  //对应成型次日一班的计划量
        Double cxClass5Plan = (scheduleVo.getCxClass5Plan() == null ? 0D : scheduleVo.getCxClass5Plan());  //对应成型次日一班的计划量
        autoScheduleLogService.insertNcScheduleLog(scheduleVo.getBatchNo(), scheduleVo.getOrderNo(), "计算库存供应时长前数据",
                logSplit("具体算法：从1班开始判断，预计库存-1班的计划大于等于0时，供应时长+12小时；预计库存-1班计划-2班计划大于等于0时，供应时长+24小时；预计库存-1班计划-2班计划-3班计划小于0，供应时长=24个小时+（((预计库存-1班计划-2班计划)/3班计划)*12）；以此类推到第5班",
                        "物料编号：" + scheduleVo.getLiningCode() + ",7点预计库存：" + stockQty + "，对应成型一班的计划量：" + 0 + "，对应成型二班的计划量：" + cxClass2Plan + "，对应成型三班的计划量：" + cxClass3Plan + "，对应成型次日一班的计划量：" + cxClass4Plan + "，对应成型次日二班的计划量：" + cxClass5Plan));

        //根据1班计算库存供应时长
        double remnantStock = stockQty;    //剩余库存
//        if(!oneComputeSupplyTime(scheduleVo, remnantStock, cxClass1Plan)) {
//            return;
//        }

        //根据2班计算库存供应时长
        remnantStock = BigDecimalUtil.sub(remnantStock, 0);  //重新计算剩余库存
        if(!oneComputeSupplyTime(scheduleVo, remnantStock, cxClass2Plan)) {
            return;
        }

        //根据3班计算库存供应时长
        remnantStock = BigDecimalUtil.sub(remnantStock, cxClass2Plan);  //重新计算剩余库存
        if(!oneComputeSupplyTime(scheduleVo, remnantStock, cxClass3Plan)) {
            return;
        }

        //根据次日1班计算库存供应时长
        remnantStock = BigDecimalUtil.sub(remnantStock, cxClass3Plan);  //重新计算剩余库存
        if(!oneComputeSupplyTime(scheduleVo, remnantStock, cxClass4Plan)) {
            return;
        }

        //根据次日2班计算库存供应时长
        remnantStock = BigDecimalUtil.sub(remnantStock, cxClass4Plan);  //重新计算剩余库存
        if(!oneComputeSupplyTime(scheduleVo, remnantStock, cxClass5Plan)) {
            return;
        }
        autoScheduleLogService.insertNcScheduleLog(scheduleVo.getBatchNo(), scheduleVo.getOrderNo(), "计算库存供应时长结束","物料编号：" + scheduleVo.getLiningCode() + "，库存供应时长=" + scheduleVo.getSupplyTime());
    }

    /**
     * 根据对应成型每班计划量，计算库存供应时长
     * @param scheduleVo  排程实体
     * @param remnantStock 剩余库存
     * @param classPlan 对应成型的计划量
     * @return false：不需要再根据其他班在计算了。 true：还需要根据其他班计划量，继续计算库存供应时长
     */
    private boolean oneComputeSupplyTime(NcScheduleResultVo scheduleVo,Double remnantStock, Double classPlan) {
        Double supplyTime = scheduleVo.getSupplyTime();
        supplyTime = (supplyTime == null ? 0D : supplyTime);
        if(BigDecimalUtil.sub(remnantStock, classPlan) >= 0) {
            //如果剩余库存 大于 对应班次库存，则库存供应时长直接+12小时
            scheduleVo.setSupplyTime(BigDecimalUtil.add(supplyTime, 12));  //库存供应时长加12小时
            return true;
        } else {
            //如果剩余库存 小宇 对应班次库存，则库存供应时长在加上：((剩余库存)/对应班班计划)*12小时
            double classSupplyTime = BigDecimalUtil.mul(BigDecimalUtil.div(remnantStock, classPlan), 12);
            supplyTime = supplyTime + BigDecimalUtil.roundDown(classSupplyTime, 1);  //设置库存供应时长向下保留1位小数
            scheduleVo.setSupplyTime(supplyTime);
            autoScheduleLogService.insertNcScheduleLog(scheduleVo.getBatchNo(), scheduleVo.getOrderNo(), "计算库存供应时长结束","物料编号：" + scheduleVo.getLiningCode() + "，库存供应时长=" + scheduleVo.getSupplyTime());
            return false;
        }
    }

    /**
     * 计算库存供应时长（小时）= 库存/(成型定额*单耗)*8小时
     * @param quotaKeys 成型机台code和胎胚代码，格式：成型机台code$胎胚代码
     * @param stockQty 16点预计库存
     * @param unitConsume 单耗
     */
    private void computeSupplyTime(NcScheduleResultVo scheduleVo, String quotaKeys, Double stockQty, Double unitConsume) {
        if(StringUtils.isBlank(quotaKeys)) {
            scheduleVo.setSupplyTime(0D);
            autoScheduleLogService.insertNcScheduleLog(scheduleVo.getBatchNo(), scheduleVo.getOrderNo(), "计算库存供应时长", "库存供应时长为空，原因：没找到对应的成型排程记录");
            return;
        }
        String [] quotaKeyArray = quotaKeys.split(",");
        Integer cxQuota = cxEngineQuotaCommonService.getCxMachineQuota(quotaKeyArray);  //成型定额
        unitConsume = BigDecimalUtil.div(unitConsume, 1000);   //单耗把毫米转成米
        Double quota = BigDecimalUtil.mul(cxQuota, unitConsume);   //定额
        if(quota == 0) {
            scheduleVo.setSupplyTime(0D);;
        } else {
            Double supplyTime = stockQty / quota * 8;  //库存可供成型连续生产的时长
            supplyTime = BigDecimalUtil.add(supplyTime, addComputeSupplyTime(scheduleVo)); //如果成型一班计划量为0，那么库存供应时长增加8小时；如果成型一班、二班计划量为0，那么供应时长增加16小时；以此类推
            scheduleVo.setSupplyTime(BigDecimalUtil.roundDown(supplyTime, 1)); //设置困存公用时长向下保留2位小数
        }
        autoScheduleLogService.insertNcScheduleLog(scheduleVo.getBatchNo(), scheduleVo.getOrderNo(), "计算库存供应时长",
                logSplit("库存供应时长supplyTime（小时）= 库存/(成型定额*单耗)*8小时;其中成型定额取成型定额的平均值，单耗也是取平均单耗", "成型定额：" + cxQuota + "，半制品平均单耗：" + unitConsume,
                        "计算后的结果数据：" + toJSONString(scheduleVo)));
    }

    /**
     * 如果成型一班计划量为0，那么库存供应时长增加8小时；如果成型一班、二班计划量为0，那么供应时长增加16小时；以此类推
     * @param scheduleVo
     * @return
     */
    private int addComputeSupplyTime(NcScheduleResultVo scheduleVo) {
        int count = 0;
        int addTime = 12;  //每班12小时
        if(scheduleVo.getCxClass1Plan() == 0) {
            count++;
        } else {
            return count * addTime;
        }

        if(scheduleVo.getCxClass2Plan() == 0) {
            count++;
        } else {
            return count * addTime;
        }

        if(scheduleVo.getCxClass3Plan() == 0) {
            count++;
        } else {
            return count * addTime;
        }

        if(scheduleVo.getCxClass4Plan() == 0) {
            count++;
        } else {
            return count * addTime;
        }

        if(scheduleVo.getCxClass5Plan() == 0) {
            count++;
        }
        return count * addTime;
    }

    /**
     * 计算胎面中班和夜班计划量
     *
     * @param scheduleVo
     * @param totalPlanQtyVo  计划量总计VO
     * @param lossMap         耗损率map
     * @param paramLossRate   工序参数中配置的耗损率
     * @param mergeThreshold  往前一班合并计划量阈值
     * @param toolCapacity    取整数
     * @param productStockDay 预生产库存天数
     * @param largeDemand     大需求量规格
     * @param bisectThreshold 均分阈值
     */
    private void computeNcPlanQty(NcScheduleResultVo scheduleVo, NcTotalPlanQtyVo totalPlanQtyVo,
                                  Map<String, Double> lossMap, double paramLossRate, double mergeThreshold, BigDecimal toolCapacity,
                                  double productStockDay, BigDecimal largeDemand, BigDecimal bisectThreshold) {
        scheduleVo.setCloseOutSpecFlag(ApsConstant.STATUS_ENABLE); // 收尾标记默认非收尾
        String oldScheduleResult = toJSONString(scheduleVo); // 没计算前的排程数据json字符串（日志使用）
        Double stockQty = scheduleVo.getStockQty(); // 库存
        Double lastMidPlanQty = scheduleVo.getLastMidPlanQty(); // 前日白班计划
        BigDecimal curlLength = (BigDecimal)scheduleVo.getParams().get(EngineConstants.STANDARD_CRIMP_LENGTH); // 一个卷曲长度
        BigDecimal delayPlanQty = (BigDecimal)scheduleVo.getParams().get(EngineConstants.DELAY_PLAN_QTY); // 大需求量不生产的工装数
        BigDecimal largeDemandReduce = (BigDecimal)scheduleVo.getParams().get(EngineConstants.LARGE_DEMAND_REDUCE); // 大需求量备库比例
//        Double totalConsumeQty = this.getCxClassPlanCumulative(scheduleVo, OpenMachineClassEnums.CLASS_FOUR); // 总需求量，前四个班
//        totalConsumeQty = BigDecimalUtils.greatest(totalConsumeQty, scheduleVo.getSurplusQty()).doubleValue(); // 取四个半的消耗量与剩余量的最大值
        Double totalConsumeQty = scheduleVo.getSurplusQty(); // 剩余量
        double supplyClass = productStockDay; // 预生产库存天数，如果是收尾规格，则直接按整天生产
//        if (isCloseOutSpec) { // 收尾规格，总需求要加上第五个班的计划
//            totalConsumeQty = BigDecimalUtil.add(totalConsumeQty, scheduleVo.getCxClass5Plan());
//        } else { // 如果没有收尾，则总需求需要再加上三四班之和，要乘以预生产库存天数
//            double cxPlanQty2 = BigDecimalUtil.add(scheduleVo.getCxClass3Plan(), scheduleVo.getCxClass4Plan());
//            cxPlanQty2 = BigDecimalUtil.mul(cxPlanQty2, supplyClass);
//            totalConsumeQty = BigDecimalUtil.add(totalConsumeQty, cxPlanQty2);
//        }

        // 每个早班计算交接班库存 = 上一天交接班库存 + 上一天成型计划量总量 - 上一天成型两个班的消耗量
        // 交接班库存要按生产几个小时库存算，例如预生产12小时库存，则交接班库存要 > 当天成型需求量 / 2，最多超过一车（110个）
        // 上一天成型计划总量原则上平均分配给两个班，但是早班的计划量要 > 上一天成型两个班的需求量 - 上一天交接班库存
        double cxPlanQty1 = BigDecimalUtil.add(scheduleVo.getCxClass1Plan(), scheduleVo.getCxClass2Plan());// 第一天成型两个班消耗量
        double cxPlanQty2 = BigDecimalUtil.add(scheduleVo.getCxClass3Plan(), scheduleVo.getCxClass4Plan());// 第二天成型两个班消耗量
        double cxPlanQty3 = cxPlanQty2;// 第三天成型两个班消耗量（成型没有，如果未收尾暂时先预计与第二天一样）
        double classStock1 = stockQty; // 第一天交接班库存，初始为当天库存
        double classStock2 = BigDecimalUtil.roundDown(BigDecimalUtil.mul(cxPlanQty2, supplyClass), 0); // 第二天交接班库存，第二天成型两个班的消耗量 * 预生产天数
        if (lastMidPlanQty > 0) { // 早班有计划则，交接班库存可以只不要超过早班的需求两
            classStock2 = BigDecimalUtils.least(classStock2, scheduleVo.getCxClass3Plan()).doubleValue(); // 交接班库存控制最多是明天早班的需求量
        }
//        if (cxPlanQty2 >= BigDecimalUtils.valueOf(DEFAULT_LARGE_DEMAND2).multiply(toolCapacity).doubleValue()) { // 大于60需求的超大需求规格，只需要备一半
////            BigDecimal newClassStock2 = BigDecimalUtils.half(cxPlanQty2);
//            BigDecimal newClassStock2 = BigDecimalUtils.sub(cxPlanQty2, largeDemandReduce.multiply(toolCapacity));
//            classStock2 = BigDecimalUtils.least(classStock2, newClassStock2).doubleValue(); // 取结算前后的较小值
//        } else
        if (cxPlanQty2 > largeDemand.multiply(curlLength).doubleValue()) {
            BigDecimal newClassStock2 = BigDecimalUtils.multiply(cxPlanQty2, largeDemandReduce, true);  // 大需求量，直接按设定的备库比率备货
            classStock2 = BigDecimalUtils.least(classStock2, newClassStock2).doubleValue(); // 取结算前后的较小值
        }
        // 计算第一天相关数值
        double planQty1 = BigDecimalUtil.add(BigDecimalUtil.sub(classStock2, classStock1), cxPlanQty1);// 第一天成型计划量 = 第二天交接班库存 - 第一天交接班库存 + 第一天成型两个班的消耗量
        planQty1 = planQty1 > 0 ? planQty1 : 0D; // 上一天交接班库存过多会计算成负数，需要处理成0
        double class1PlanQty1 = lastMidPlanQty;// 第一天早班计划 = 前日早班计划
        double class2PlanQty1 = BigDecimalUtil.sub(planQty1, class1PlanQty1);// 第一天夜班计划 = 等于第一天成型计划 - 第一天早班计划
        // 第一天库存缺口
        double lackPlanQtyDay1 = BigDecimalUtils.sub(cxPlanQty1, BigDecimalUtils.add(classStock1, class1PlanQty1)).doubleValue();
        if (lastMidPlanQty > 0 && lackPlanQtyDay1 <= 0 && scheduleVo.getCxClass3Plan() < delayPlanQty.doubleValue()) {
            class2PlanQty1 = 0D; // 如果早班有排计划了，且成型需求已满足，同时第二天早班需求量不足可推迟生产的计划量，则可以先不做
        }
        if (lastMidPlanQty > 0 && class2PlanQty1 > 0) {
            // 如果早夜班都有排计划，则尝试将夜班补够均分阈值，尝试补够“两天需求量减库存的差值”与“均分阈值”的较小值
            BigDecimal lackPlanQtyAll = BigDecimalUtils.sub(BigDecimalUtils.add(cxPlanQty1, cxPlanQty2), classStock1);
            BigDecimal day1PlanQty = BigDecimalUtils.least(bisectThreshold, lackPlanQtyAll);
            BigDecimal day1PlanQtyDiff = day1PlanQty.subtract(BigDecimalUtils.add(lastMidPlanQty, class2PlanQty1));
            if (day1PlanQtyDiff.compareTo(BigDecimal.ZERO) > 0) {
                class2PlanQty1 = BigDecimalUtil.add(class2PlanQty1, day1PlanQtyDiff.doubleValue());
            }
        }
        double newClass2PlanQty1 = this.planQtyRounding(scheduleVo, class2PlanQty1, toolCapacity, totalConsumeQty,
                OpenMachineClassEnums.CLASS_TWO); // 整车取整
        // 如果超出整工装的计划量不足半个工装，则减一个工装的量，夜班先不做，因为占用工装太多
//        if (class2PlanQty1 > toolCapacity.doubleValue() && newClass2PlanQty1 > class2PlanQty1) {
//            double remainder = BigDecimalUtil.sub(toolCapacity.doubleValue(), BigDecimalUtil.sub(newClass2PlanQty1, class2PlanQty1));
//            if (BigDecimalUtils.valueOf(remainder).compareTo(BigDecimalUtils.half(toolCapacity)) <= 0) {
//                newClass2PlanQty1 = BigDecimalUtil.sub(newClass2PlanQty1, toolCapacity.doubleValue());
//            }
//        }

        double dayPlanQty = newClass2PlanQty1; // 夜班计划
        scheduleVo.setDayPlanQty(dayPlanQty);
        // 根据排好的计划量重算相关数值
        planQty1 = BigDecimalUtil.add(class1PlanQty1, dayPlanQty); // 刷新第一天成型计划量
        classStock2 = BigDecimalUtil.sub(BigDecimalUtil.add(planQty1, classStock1), cxPlanQty1);// 刷新第二天交接班库存
        scheduleVo.setClassStock(classStock2); // 保存交接班库存，用于均衡计算
        scheduleVo.setSupplyDemandRatio(BigDecimalUtil.div(classStock2, cxPlanQty2, 4)); // 计算交接班库存供需比率，第二天交接班库存 / 成型第二天需求量，用于均衡计算

        // 计算第二天相关数值
        double classStock3 = BigDecimalUtil.roundDown(BigDecimalUtil.mul(cxPlanQty3, supplyClass), 0); // 第三天交接班库存，第三天成型两个班的消耗量 * 预生产天数
        if (classStock3 > largeDemand.multiply(curlLength).doubleValue()) {
            BigDecimal newClassStock3 = BigDecimalUtils.multiply(classStock3, largeDemandReduce, true);  // 大需求量，直接按设定的备库比率备货
            classStock3 = BigDecimalUtils.least(classStock3, newClassStock3).doubleValue(); // 取结算前后的较小值
        }
        double planQty2 = BigDecimalUtil.add(BigDecimalUtil.sub(classStock3, classStock2), cxPlanQty2);// 第二天成型计划量 = 第三天交接班库存 - 第二天交接班库存 + 第二天成型两个班的消耗量
        planQty2 = BigDecimalUtils.upToZero(planQty2).doubleValue(); // 上一天交接班库存过多会计算成负数，需要处理成0
        double lackPlanQty = BigDecimalUtil.sub(cxPlanQty2, classStock2); // 早班先补交接班库存缺口
        double class1PlanQty2 = this.planQtyRounding(scheduleVo, lackPlanQty, toolCapacity, totalConsumeQty,
                OpenMachineClassEnums.CLASS_THREE); // 第二天早班计划优先补库存缺口，整车取整
        double nightPlanQty = class1PlanQty2; // 早班计划
        scheduleVo.setNightPlanQty(nightPlanQty);
        double class2PlanQty2 = BigDecimalUtil.sub(planQty2, class1PlanQty2);// 第二天夜班计划 = 等于第二天成型计划 - 第二天早班计划
        // 如果交接班库存 - 第二天需求已经超出第三天的一半，则不需要排产（主要针对小批量）
        if (lackPlanQty < 0 && Math.abs(lackPlanQty) > BigDecimalUtils.half(cxPlanQty3).doubleValue()) {
            class2PlanQty2 = 0D;
        }
        double newClass2PlanQty2 = this.planQtyRounding(scheduleVo, class2PlanQty2, toolCapacity, totalConsumeQty,
                OpenMachineClassEnums.CLASS_FOUR); // 次日夜班计划 = 第二天夜班计划整车取整
        // 如果超出整工装的计划量不足半个工装，则减一个工装的量，夜班先不做，因为占用工装太多
        if (class2PlanQty2 > toolCapacity.doubleValue() && newClass2PlanQty2 > class2PlanQty2) { // 收尾的不处理
            double remainder = BigDecimalUtil.sub(toolCapacity.doubleValue(), BigDecimalUtil.sub(newClass2PlanQty2, class2PlanQty2));
            if (BigDecimalUtils.valueOf(remainder).compareTo(BigDecimalUtils.half(toolCapacity)) <= 0) {
                newClass2PlanQty2 = BigDecimalUtil.sub(newClass2PlanQty2, toolCapacity.doubleValue());
            }
        }
        double nextDayPlanQty = newClass2PlanQty2;
        scheduleVo.setNextDayPlanQty(nextDayPlanQty);

        // 收尾规格计划如果不足一车，合并到上一个班
//        if (nextDayPlanQty > 0 && nextDayPlanQty < toolCapacity.doubleValue()) {
//            nightPlanQty = BigDecimalUtil.add(nightPlanQty, nextDayPlanQty);
//            nextDayPlanQty = 0D;
//        }
//        if (nightPlanQty > 0 && nightPlanQty < toolCapacity.doubleValue()) {
//            nextDayPlanQty = BigDecimalUtil.add(nightPlanQty, nextDayPlanQty);
//            nightPlanQty = 0D;
//        }

        /*String oldScheduleResult = toJSONString(scheduleVo); //没看是计算前的排程数据json字符串（日志使用）
        Double stockQty = scheduleVo.getStockQty(); //库存
//        double unitConsume = BigDecimalUtil.div(scheduleVo.getUnitConsume(), 1000D, 3);
        //计算夜班计划量 = 成型三班消耗胎面计划量（次日成型早班计划）
        double dayPlanQty = scheduleVo.getCxClass3Plan();
        double initDayPlanQty = dayPlanQty;
        //计算早班计划量 = 成型四班消耗胎面计划量（次日成型夜班计划）
        double nightPlanQty = scheduleVo.getCxClass4Plan();

        //根据库存重新计算中班计划量：（原中班计划量>库存） ？（ 原中班计划量-库存） ： 0
        dayPlanQty = (initDayPlanQty > stockQty) ? BigDecimalUtil.sub(dayPlanQty, stockQty) : 0;
        //根据库存重新计算夜班计划量：（原中班计划量>库存） ？原夜班计划量 ： （原中班计划量+原夜班计划量 - 库存）
        nightPlanQty = (initDayPlanQty > stockQty) ? nightPlanQty : BigDecimalUtil.sub(BigDecimalUtil.add(initDayPlanQty, nightPlanQty), stockQty);
        nightPlanQty = (nightPlanQty < 0) ? 0D : nightPlanQty;*/

        String machineId = scheduleVo.getMachineId();  //机台id
        double lossRate = 0;
        //只有单个机台的时候，自动排程才计算耗损率
        if(StringUtils.isNotBlank(machineId) && !machineId.contains(",")) {
            //计划量要加上耗损量
            lossRate = ncEngineLossService.getLossRate(scheduleVo.getLiningCode(), scheduleVo.getMachineId(), lossMap, paramLossRate);
            dayPlanQty = BigDecimalUtil.add(dayPlanQty, BigDecimalUtil.mul(dayPlanQty, lossRate));
            nightPlanQty = BigDecimalUtil.add(nightPlanQty, BigDecimalUtil.mul(nightPlanQty, lossRate));
            nextDayPlanQty = BigDecimalUtil.add(nextDayPlanQty, BigDecimalUtil.mul(nextDayPlanQty, lossRate));
        }

        //如果中班计划量>0，那么中班计划量=中班计划量+夜班计划量，夜班计划量=0（为了让相同的胶在同一个班生产，而且又不能延误生产）
//        if (dayPlanQty > 0) {
//            dayPlanQty = BigDecimalUtil.add(dayPlanQty, nightPlanQty);
//            nightPlanQty = 0D;
//        }

        //计划量向上取整
        dayPlanQty = BigDecimalUtil.roundUp(dayPlanQty, 0);
        nightPlanQty = BigDecimalUtil.roundUp(nightPlanQty, 0);
        nextDayPlanQty = BigDecimalUtil.roundUp(nextDayPlanQty, 0);
        scheduleVo.setDayPlanQty(dayPlanQty);
        scheduleVo.setNightPlanQty(nightPlanQty);
        scheduleVo.setNextDayPlanQty(nextDayPlanQty);
        scheduleVo.setIsEqualShare(false);
//        scheduleVo.setCloseOutSpecFlag(ApsConstant.STATUS_DISABLE);

        //计算中班总计划量 和 夜班总计划量
//        this.groupTotalPlanQtyMap(scheduleVo, totalPlanQtyMap);
        totalPlanQtyVo.setTotalDayPlanQty(BigDecimalUtil.add(totalPlanQtyVo.getTotalDayPlanQty(), dayPlanQty));
        totalPlanQtyVo.setTotalNightPlanQty(BigDecimalUtil.add(totalPlanQtyVo.getTotalNightPlanQty(), nightPlanQty));
        totalPlanQtyVo.setTotalNextDayPlanQty(BigDecimalUtil.add(totalPlanQtyVo.getTotalNextDayPlanQty(), nextDayPlanQty));
        totalPlanQtyVo.setTotalPlanQty(BigDecimalUtil.add(totalPlanQtyVo.getTotalDayPlanQty(), totalPlanQtyVo.getTotalNightPlanQty(), totalPlanQtyVo.getTotalNextDayPlanQty()));

        this.computeNcPlanQtyLog(oldScheduleResult, scheduleVo, lossMap, paramLossRate, lossRate);  //添加日志
    }

    /**
     * 获取各班需求量的累计值（从前日早班开始）
     *
     * @param scheduleVo
     * @param classNum
     * @return
     */
    private Double getCxClassPlanCumulative(NcScheduleResultVo scheduleVo, OpenMachineClassEnums classNum) {
        Double cxClass1Plan = (scheduleVo.getCxClass1Plan() == null ? 0D : scheduleVo.getCxClass1Plan());  //对应成型前日早班的计划量
        Double cxClass2Plan = (scheduleVo.getCxClass2Plan() == null ? 0D : scheduleVo.getCxClass2Plan());  //对应成型夜班的计划量
        Double cxClass3Plan = (scheduleVo.getCxClass3Plan() == null ? 0D : scheduleVo.getCxClass3Plan());  //对应成型早班的计划量
        Double cxClass4Plan = (scheduleVo.getCxClass4Plan() == null ? 0D : scheduleVo.getCxClass4Plan());  //对应成型次日夜班的计划量
        Double cxClass5Plan = (scheduleVo.getCxClass5Plan() == null ? 0D : scheduleVo.getCxClass5Plan());  //对应成型次日早班的计划量
        Double planQty = 0D;
        if (classNum == null) {
            return planQty;
        }
        planQty = BigDecimalUtil.add(planQty, cxClass1Plan);
        if (classNum == OpenMachineClassEnums.CLASS_ONE) {
            return planQty;
        }
        planQty = BigDecimalUtil.add(planQty, cxClass2Plan);
        if (classNum == OpenMachineClassEnums.CLASS_TWO) {
            return planQty;
        }
        planQty = BigDecimalUtil.add(planQty, cxClass3Plan);
        if (classNum == OpenMachineClassEnums.CLASS_THREE) {
            return planQty;
        }
        planQty = BigDecimalUtil.add(planQty, cxClass4Plan);
        if (classNum == OpenMachineClassEnums.CLASS_FOUR) {
            return planQty;
        }
        return BigDecimalUtil.add(planQty, cxClass5Plan);
    }

    /**
     * 计划量取整
     *
     * @param scheduleVo      排产记录
     * @param planQty         原计划量
     * @param toolCapacity    一车可以放的胎面量
     * @param totalConsumeQty 总需期间量，用于判断收尾规格是否超量
     * @param isCloseOutSpec  是否收尾
     * @param classNum        当前班次，从前日早班开始
     * @return
     */
    private double planQtyRounding(NcScheduleResultVo scheduleVo, double planQty, BigDecimal toolCapacity,
                                   Double totalConsumeQty, OpenMachineClassEnums classNum) {
        if (planQty <= 0D) { // 不排的情况直接返回0即可
            return 0D;
        }
        double roudingPlanQty = BigDecimalUtils.valueOf(planQty).divide(toolCapacity, 0, RoundingMode.CEILING)
                .multiply(toolCapacity).doubleValue(); // 取整车
        if (classNum == null) {
            return roudingPlanQty;
        }
        OpenMachineClassEnums lastClass = classNum;
        if (classNum != OpenMachineClassEnums.CLASS_ONE) { // 取出上一班的班次
            Integer classIndex = classNum.getClassIndex();
            lastClass = OpenMachineClassEnums.getClassEnums(classIndex - 1);
        }
        double lastPlanCumulative = this.getNcClassPlanCumulative(scheduleVo, lastClass); // 到上个班次班次班的累计已排计划量
        double newPlanQty = BigDecimalUtil.add(lastPlanCumulative, roudingPlanQty, scheduleVo.getStockQty()); // 库存+已排计划+本班计划
        double result = roudingPlanQty;
        // 如果库存+计划已经超过总需求量，则本班的计划量要限制住不允许超量
        if (newPlanQty > totalConsumeQty) {
            Double increaseMidPlanQty = BigDecimalUtil.sub(newPlanQty, totalConsumeQty);
            result = BigDecimalUtil.sub(roudingPlanQty, increaseMidPlanQty);
            result = result > 0? result: 0D;
        }
        scheduleVo.setCloseOutSpecFlag(newPlanQty >= totalConsumeQty? ApsConstant.STATUS_ENABLE: ApsConstant.STATUS_DISABLE);
        if (result < MIN_PLAN_QTY) { // 只差一点点，不处理
            return 0;
        }
        return result;
    }

    /**
     * 获取各班计划量的累计值（从前日早班开始）
     *
     * @param scheduleVo
     * @param classNum
     * @return
     */
    private Double getNcClassPlanCumulative(NcScheduleResultVo scheduleVo, OpenMachineClassEnums classNum) {
        Double planQty = 0D;
        if (classNum == null) {
            return planQty;
        }
        planQty = BigDecimalUtil.add(planQty, scheduleVo.getLastMidPlanQty());
        if (classNum == OpenMachineClassEnums.CLASS_ONE) {
            return planQty;
        }
        planQty = BigDecimalUtil.add(planQty, scheduleVo.getDayPlanQty());
        if (classNum == OpenMachineClassEnums.CLASS_TWO) {
            return planQty;
        }
        planQty = BigDecimalUtil.add(planQty, scheduleVo.getNightPlanQty());
        if (classNum == OpenMachineClassEnums.CLASS_THREE) {
            return planQty;
        }
        return BigDecimalUtil.add(planQty, scheduleVo.getNextDayPlanQty());
    }

    private void computeNcPlanQtyLog(String oldScheduleResult, NcScheduleResultVo scheduleVo, Map<String, Double> lossMap, double paramLossRate, double lossRate) {
        StringBuffer logDetail = new StringBuffer("");
        logDetail.append("开始计算中班和夜班计划量").append(division);
        logDetail.append("计算前排程数据：" + oldScheduleResult).append(division);
        logDetail.append("计算中班计划量dayPlanQty = （成型一班消耗内衬计划量cxClass1Plan + 成型二班消耗内衬计划量CxClass2Plan）").append(division);
        logDetail.append("计算夜班计划量nightPlanQty =（成型三班消耗内衬计划量cxClass3Plan + 成型次日一班消耗内衬计划量cxClass4Plan）").append(division);
        logDetail.append("根据库存重新计算中班计划量dayPlanQty：（原中班计划量dayPlanQty > 库存stockQty） ？（ 原中班计划量-库存） ： 0").append(division);
        logDetail.append("根据库存重新计算夜班计划量nightPlanQty：（原中班计划量dayPlanQty>库存stockQty） ？原夜班计划量nightPlanQty ： （原中班计划量dayPlanQty + 原夜班计划量nightPlanQty - 库存stockQty）").append(division);
        logDetail.append("内衬耗损率集合：" + toJSONString(lossMap) + "  参数配置耗损率：" + paramLossRate).append(division);
        logDetail.append("获得损耗率（从损耗率表获取对应的损耗率，获取顺序：机台+物料编号 > 内衬代码 > 机台 >工序参数配置），耗损率：" + lossRate).append(division);
        logDetail.append("重新计算中班计划量和夜班计划量(计划量 = 计划量 + 计划量 * 耗损率)，计划量要加上耗损率的损耗数").append(division);
        logDetail.append("如果中班计划量>0，那么中班计划量=中班计划量+夜班计划量，夜班计划量=0（为了让相同的胶在同一个班生产，而且又不能延误生产）").append(division);
        logDetail.append("计划量计算好后的排程数据：" + toJSONString(scheduleVo));
        autoScheduleLogService.insertNcScheduleLog(scheduleVo.getBatchNo(), scheduleVo.getOrderNo(), "计算各班计划量", logDetail.toString());
    }

    /**
     * 创建批次号
     * @param scheduleDate
     * @return
     */
    private String createBatchNo(String scheduleDate) {
        scheduleDate = scheduleDate.replace("-", "");
        return incrementService.getSequence3(EngineConstants.NC_BATCH_NO_PREFIX + scheduleDate);
    }

    /**
     * 创建工单号
     * @param batchNo 批次号
     * @return
     */
    private String createOrderNo(String batchNo) {
        return incrementService.getSequence4(batchNo);
    }

    /**
     * 获取工序参数map
     * @return
     */
    private Map<String, String> getParamsMap() {
        List<NcParamsVo> list = this.ncEngineMapper.listNcParams();
        Map<String, String> map = list.stream().collect(Collectors.toMap(NcParamsVo::getParamCode, NcParamsVo::getParamValue));
        return map == null ? new HashMap<>() : map;
    }

    /**
     * 自动排程基础表的数据日志
     * @param batchNo 自动排程批次号
     * @param glueSeqMap 胶料顺序集合
     * @param specifyMachineMap 定点机台和机台的限制作业集合
     * @param planStockMap 16点预计库存集合
     * @param lossRateMap 耗损率集合
     * @param monthSurplus 月度计划剩余量、完成量集合
     * @param paramsMap 参数设置集合
     */
    private void baseDataLog(String batchNo, Map<String, String> glueSeqMap, Map<String, String> specifyMachineMap, Map<String, Double> planStockMap, Map<String, Double> lossRateMap,
                             Map<String, NcMonthSurplusVo> monthSurplus,Map<String, String> paramsMap) {
        StringBuffer logDetail = new StringBuffer("");
        logDetail.append("胶料顺序集合：" + toJSONString(glueSeqMap)).append(division);
        logDetail.append("定点机台和机台的限制作业集合：" + toJSONString(specifyMachineMap)).append(division);
        logDetail.append("16点预计库存集合：" + toJSONString(planStockMap)).append(division);
        logDetail.append("耗损率集合：" + toJSONString(lossRateMap)).append(division);
        logDetail.append("月度计划剩余量、完成量集合：" + toJSONString(monthSurplus)).append(division);
        logDetail.append("参数设置集合：" + toJSONString(paramsMap)).append(division);
        autoScheduleLogService.insertNcScheduleLog(batchNo, "", "自动排程基础表的数据日志", logDetail.toString());
    }

    @Override
    public void batchUpdateBatchNoAndOrderNo(String scheduleDate) {
        List<NcScheduleResultVo> scheduleResultVoList = ncEngineMapper.listNcEnginSchedule(scheduleDate);
        //查询当前排程的批次号
        String batchNo = ncEngineMapper.getNcCurrentBatchNo(scheduleDate);
        if (StringUtils.isBlank(batchNo)) {
            //当前的批次号为空，说明还没”自动排程“或者做的批量导入（需要删掉已排的数据），那么自己生成一个排程批次号
            //胎面排程批次号
            batchNo = this.createBatchNo(scheduleDate);
            //创建自动排程记录
            this.createScheduleRecord(scheduleDate, "", batchNo);
            //把排程数据同步到log表
//            this.syncNcScheduleToLog(scheduleDate);
        }
        for (NcScheduleResultVo scheduleResult : scheduleResultVoList) {
            //批次号
            scheduleResult.setBatchNo(batchNo);
            //工单号
            String orderNo = this.createOrderNo(batchNo);
            scheduleResult.setOrderNo(orderNo);
        }
        if (CollectionUtils.isNotEmpty(scheduleResultVoList)) {
            ncEngineMapper.batchUpdateBatchNoAndOrderNo(scheduleResultVoList);
        }
    }
}
