package com.zlt.aps.gsq.engine.service.impl;

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
import com.zlt.aps.gsq.api.domain.dto.GsqScheduleResultDto;
import com.zlt.aps.gsq.api.domain.entity.GsqMachineInfo;
import com.zlt.aps.gsq.engine.mapper.GsqEngineMapper;
import com.zlt.aps.gsq.engine.mapper.GsqEngineStockMapper;
import com.zlt.aps.gsq.engine.service.*;
import com.zlt.aps.gsq.engine.vo.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.math.NumberUtils;
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
public class GsqEngineServiceImpl implements GsqEngineService {

    @Resource
    private GsqEngineMapper gsqEngineMapper;
    @Resource
    private GsqEngineStockService gsqEngineStockService;
    @Resource
    private GsqEngineMachineService gsqEngineMachineService;
    @Resource
    private IncrementService incrementService;
    @Resource
    private GsqEngineLossService gsqEngineLossService;
    @Resource
    private GsqEngineMonthSurplusService gsqEngineMonthSurplusService;
    @Resource
    private CxEngineQuotaCommonService cxEngineQuotaCommonService;
    @Resource
    private CommonMapper commonMapper;
    @Resource
    private GsqEngineStockMapper gsqEngineStockMapper;
    @Resource
    private AutoScheduleLogService autoScheduleLogService;
    private String division = "\r\n---------------------------------------------------\r\n";  //日志分割符
    private final static BigDecimal HOUR24 = new BigDecimal("24"); // 24小时
    private final static String DEFAULT_TOOL_CAPACITY = "110"; // 工装容量默认值
    private final static String DEFAULT_PRODUCT_STOCK_HOUR = "24"; // 保库存供应时长
    private final static String DEFAULT_LARGE_DEMAND = "2000"; // 需求量超过该值的算大需求量规格，库存应该需要控制，且超过该值早夜班对半分
    private final static String DEFAULT_BIG_SIZE_SPEC = "17"; // 大尺寸阈值，超过该尺寸的规格比较难做，不要集中在一个班做
    private final static String DEFAULT_EQUAL_SHARE_THRESHOLD = "500"; // 需求量超过该值早夜班对半分

    /**
     * 钢丝圈胶自动排程
     *
     * @param scheduleDate 排程日期，格式：yyyy-MM-dd
     */
    @Transactional(rollbackFor=Exception.class)
    public void autoGsqSchedule(String scheduleDate) {
        String username = SecurityUtils.getUsername(); //用户账号
        String tqBatchNo = "";  //胎圈批次号
        String batchNo = this.createBatchNo(scheduleDate);  //钢丝圈排程批次号
        GsqScheduleParams params = this.loadParams();  // 获取工序参数map
        Map<String, String> mapAssistSpec = this.mapAssistSpec(); //获得外协规格Map
        String productionStage = params.getProductionStage();  //仅投产阶段规格排产标识
        List<GsqScheduleResultVo> scheduleList = gsqEngineMapper.statGsqScheduleBase(scheduleDate, productionStage);  //根据成型排程记录 统计出 钢丝圈胶排程记录基础数据
        if (scheduleList == null || scheduleList.isEmpty()) {
            log.info("根据胎圈排程记录 统计出 钢丝圈胶排程记录基础数据 为空");
            autoScheduleLogService.insertGsqScheduleLog(batchNo, "", "自动排程失败", "自动排程失败，原因：胎圈排程数据为空，或没有在施工信息中找到对应的物料！"); //添加日志
            throw new RuntimeException(I18nUtil.getMessage("engine.auto.scheule.gsq.tip"));
        }
        //过滤掉钢丝圈3个班的计划量都为0（扣减库存之前的计划量）的数据
//        scheduleList = scheduleList.stream().filter(s -> (s.getMidPlanQty()+s.getNightPlanQty())>0).collect(Collectors.toList());
        autoScheduleLogService.insertGsqScheduleLog(batchNo, "", "根据成'型排程记录'统计出钢丝圈胶排程记录基础数据",  toJSONString(scheduleList));
        this.ValidatedConstruction(scheduleDate, batchNo, productionStage, mapAssistSpec);   //证成型排程记录的胎胚code在施工表中是否都能找到对应记录，如果不能则提示
        Map<String, String> twiningDiscMachineMap = gsqEngineMachineService.getTwiningDiscMachineMap(); //获得钢丝圈代码和缠绕盘map (key = 规格尺寸~排列方式 )
        Map<String, String> twiningDiscMap = gsqEngineMachineService.getTwiningDiscMap(scheduleDate); //获得钢丝圈代码和缠绕盘（value = 规格尺寸~排列方式）map
        Map<String, String> specifyCanMachineMap = gsqEngineMachineService.getSpecifyMachineMap(EngineConstants.JOB_TYPE_CAN); //获得钢丝圈代码和定点机台的限制作业map
        Map<String, String> specifyNotMachineMap = gsqEngineMachineService.getSpecifyMachineMap(EngineConstants.JOB_TYPE_NOT); //获得钢丝圈代码和定点机台的不可作业map
        List<GsqMachineInfo> allMachineList = gsqEngineMachineService.listGsqMachine();
        Map<String, Double> planStockMap = gsqEngineStockService.getPlanStockMap(batchNo, scheduleDate, params.getStockLossRate());  //计算钢丝圈16点预计库存
        Map<String, Double> stockMap = this.loadTqStock(scheduleDate); // 加载库存
        Map<String, Double> lastDayMidPlanMap = this.loadLastDayMidPlan(scheduleDate); // 加载昨日早班计划
        List<String> steelRingCodeList = scheduleList.stream().map(GsqScheduleResultVo::getSteelRingCode).distinct().collect(Collectors.toList());
        Map<String, BigDecimal> reserveStockMap = gsqEngineStockService.getReserveStockMap(steelRingCodeList, params.getStockRatio().doubleValue());  // 取预生产库存倍数Map
        Map<String, Double> lossRateMap = gsqEngineLossService.getLossRateMap();   //损耗率map
//        Map<String, String> quotaParamMap = this.getQuotaParamMap(scheduleDate, productionStage);   //获取钢丝圈对应的成型胎胚code和机台code
        Map<String, GsqMonthSurplusVo> monthSurplus = gsqEngineMonthSurplusService.getMonthSurplus(scheduleDate);  //获得月度计划剩余量、完成量
        this.baseDataLog(batchNo, twiningDiscMachineMap, twiningDiscMap, specifyCanMachineMap, specifyNotMachineMap, planStockMap, lossRateMap, monthSurplus, params); //把基础数据假如到日志中
        GsqTotalPlanQtyVo totalPlanQtyVo = new GsqTotalPlanQtyVo();  //钢丝圈中班和夜班总计划量Vo
        for (GsqScheduleResultVo scheduleVo : scheduleList) {
            tqBatchNo = scheduleVo.getTqBatchNo();
            scheduleVo.setBatchNo(batchNo);    //批次号
            String orderNo = this.createOrderNo(batchNo);   //创建工单号
            scheduleVo.setOrderNo(orderNo);
            scheduleVo.setStockQty(stockMap.getOrDefault(scheduleVo.getSteelRingCode(), 0D));  // 库存
//            scheduleVo.setPlanStockQty(planStockMap.getOrDefault(scheduleVo.getSteelRingCode(), 0D));
            scheduleVo.setLastMidPlanQty(lastDayMidPlanMap.getOrDefault(scheduleVo.getSteelRingCode(), 0D)); // 上一天早班库存
            scheduleVo.setPlanStockQty(BigDecimalUtils.qtySub(BigDecimalUtil.add(scheduleVo.getStockQty(), scheduleVo.getLastMidPlanQty()), scheduleVo.getCxClass1Plan())); // 计算19点预计库存
            scheduleVo.getParams().put(EngineConstants.BIG_SIZE_SPEC, params.getBigSizeSpec());

            autoScheduleLogService.insertGsqScheduleLog(batchNo, orderNo, "根据'16点预计库存集合'设置库存",
                    logSplit("16点预计库存集合：" + toJSONString(planStockMap), "数据结果：" + toJSONString(scheduleVo))); //添加日志

            this.newComputeSupplyTime(scheduleVo, scheduleVo.getPlanStockQty());  //库存供应时长
            this.computeReserveStock(scheduleVo, reserveStockMap.getOrDefault(scheduleVo.getSteelRingCode(), params.getStockRatio()));
            this.computeGsqPlanQty(scheduleVo, totalPlanQtyVo, lossRateMap, params);  //计算钢丝圈各班计划量
            this.setStatusAndCloseTip(scheduleVo, monthSurplus.get(scheduleVo.getSteelRingCode()), params.getCloseOutNum());  //设置收尾提示标识 和 生产状态字段
            scheduleVo.setIsRelease(ApsConstant.NO_RELEASE);
            scheduleVo.setDelFlag(ApsConstant.DEL_FLAG_NORMAL);
            scheduleVo.setCreateTime(new Date());
            scheduleVo.setCreateBy(username);
        }
//        this.equilibrium(batchNo, scheduleList, paramsMap, totalPlanQtyVo);  //中班和夜班计排程计划量均衡处理
        this.equilibriumDay1(scheduleList, totalPlanQtyVo, params);
        this.equilibriumDay2(scheduleList, totalPlanQtyVo, params); // 均衡第二天中夜班计划量
        this.chooseMachine(scheduleList, allMachineList, specifyCanMachineMap, specifyNotMachineMap, twiningDiscMachineMap, twiningDiscMap);  //选择生产线
        this.setProduceOrder(scheduleList);  //设置各班生产顺序

        List<GsqScheduleResultVo> existScheduleList = this.gsqEngineMapper.listGsqEnginSchedule(scheduleDate);  //查询当天已经存在的排产记录
        this.syncGsqScheduleToLog(scheduleDate);  //把排程数据同步到log表，删除历史外协排程数据
        this.createScheduleRecord(scheduleDate, tqBatchNo, batchNo);  //创建自动排程记录
        List<GsqScheduleResultVo> assistScheduleList = scheduleList.stream().filter(r -> mapAssistSpec.containsKey(r.getSteelRingCode())).collect(Collectors.toList()); //过滤出外协排程数据
        scheduleList = scheduleList.stream().filter(r -> !mapAssistSpec.containsKey(r.getSteelRingCode())).collect(Collectors.toList());  //过滤出非外协的排产数据
        if(StringUtils.isNotEmpty(assistScheduleList)) {
            gsqEngineMapper.batchCreateAssistScheduleResult(assistScheduleList);   //批量新增外协排程结果数据
        }

        scheduleList = this.mergeExistSchedule(batchNo, scheduleList, existScheduleList);  //如果当天排程已经存在，则把当天的排程合并到 自动排程的列表中
        if(StringUtils.isNotEmpty(scheduleList)) {
            gsqEngineMapper.batchCreateScheduleResult(scheduleList);   //批量新增非外协排程结果数据
        }
    }

    /**
     * 更新计划量统计对象
     * @param scheduleList
     * @param totalPlanQtyVo
     */
    private void refreshTotalPlanQtyVo(List<GsqScheduleResultVo> scheduleList, GsqTotalPlanQtyVo totalPlanQtyVo) {
        Double totalMidPlanQty = scheduleList.stream().mapToDouble(GsqScheduleResultVo::getMidPlanQty).sum();
        Double totalDayPlanQty = scheduleList.stream().mapToDouble(GsqScheduleResultVo::getDayPlanQty).sum();
        Double totalNightPlanQty = scheduleList.stream().mapToDouble(GsqScheduleResultVo::getNightPlanQty).sum();
        totalPlanQtyVo.setTotalMidPlanQty(totalMidPlanQty);
        totalPlanQtyVo.setTotalDayPlanQty(totalDayPlanQty);
        totalPlanQtyVo.setTotalNightPlanQty(totalNightPlanQty);
        totalPlanQtyVo.setTotalPlanQty(BigDecimalUtil.add(totalDayPlanQty, totalNightPlanQty, totalMidPlanQty));
    }

    /**
     * 均衡第一天夜班与第二天的计划
     *
     * @param scheduleList   排程列表
     * @param paramsMap      工序参数
     * @param totalPlanQtyVo 胎圈中班和夜班总计划量Vo
     */
    private void equilibriumDay1(List<GsqScheduleResultVo> scheduleList, GsqTotalPlanQtyVo totalPlanQtyVo, GsqScheduleParams params) {
        BigDecimal toolCapacity = BigDecimalUtils.valueOf(params.getToolCapacity());
        double totalMidPlanQty = totalPlanQtyVo.getTotalMidPlanQty(); // 夜班总计划量
        double totalNightPlanQty = totalPlanQtyVo.getTotalNightPlanQty(); // 早班总计划量
        double totalDayPlanQty = totalPlanQtyVo.getTotalDayPlanQty(); // 次日夜班总计划量
        double midPlanQtyReference = BigDecimalUtils.avg(0, RoundingMode.UP, totalMidPlanQty, totalNightPlanQty, totalDayPlanQty).doubleValue(); // 计划平均值
        double difNum = BigDecimalUtil.sub(totalMidPlanQty, midPlanQtyReference); // 早班和平均值的差值
        if (difNum == 0) {
            return;
        }
        boolean isNightClassPass = difNum > 0; // 夜班是否超量
        scheduleList = scheduleList.stream().sorted((r1, r2) -> {
            BigDecimal classStock1 = BigDecimalUtils.sub(r1.getClassStock(), r1.getTqClass1PlanQty());
            BigDecimal classStock2 = BigDecimalUtils.sub(r2.getClassStock(), r2.getTqClass1PlanQty());
            if (isNightClassPass) {
                // 夜班超量，将交接班库存较充足的转移到早班（倒序）
                return classStock2.compareTo(classStock1);
            } else {
                // 早班超量，将交接班库存较充低的转移到夜班（顺序）
                return classStock1.compareTo(classStock2);
            }
        }).collect(Collectors.toList());

        for (GsqScheduleResultVo scheduleVo: scheduleList) {
            if (ApsConstant.STATUS_ENABLE.equals(scheduleVo.getCloseOutSpecFlag())) {
                continue; // 收尾规格不处理
            }
            BigDecimal midPlanQty = BigDecimalUtils.valueOf(scheduleVo.getMidPlanQty());
            BigDecimal nightPlanQty = BigDecimalUtils.valueOf(scheduleVo.getNightPlanQty());
            BigDecimal dayPlanQty = BigDecimalUtils.valueOf(scheduleVo.getDayPlanQty());

            double classStock2 = scheduleVo.getClassStock(); // 第二天交接班库存
            BigDecimal midAddPlan = BigDecimal.ZERO; // 夜班增加量
            BigDecimal nightAddPlan = BigDecimal.ZERO; // 早班增加量
            BigDecimal dayAddPlan = BigDecimal.ZERO; // 次日夜班增加量
            boolean isLargeDemand = BigDecimalUtil.add(scheduleVo.getTqClass1PlanQty(), scheduleVo.getTqClass2PlanQty()) >= params.getLargeDemand(); // 大需求量标记
            boolean isMidSpec = Arrays.stream(params.getMidSpec()).anyMatch(scheduleVo.getSteelRingCode()::equals); // 是否固定早班规格
            boolean isNightSpec = Arrays.stream(params.getNightSpec()).anyMatch(scheduleVo.getSteelRingCode()::equals); // 是否固定夜班规格
            BigDecimal nextDayTqPlanQty = BigDecimalUtils.add(scheduleVo.getTqClass1PlanQty(), scheduleVo.getTqClass2PlanQty());
            BigDecimal movePlanQty  = BigDecimalUtils.ceil(nextDayTqPlanQty.divide(BigDecimal.TEN, 0, RoundingMode.UP), toolCapacity); // 转移量，总需求量的10%
            if (isLargeDemand) { // 夜班超量且是大规格
                BigDecimal avgPlanQty = BigDecimalUtils.avg(0, RoundingMode.UP, midPlanQty, nightPlanQty, dayPlanQty);
                BigDecimal movePlanQty1 = BigDecimalUtils.ceil(midPlanQty.subtract(avgPlanQty), toolCapacity).abs(); // 挪超过/低于平均值的部分
                movePlanQty = BigDecimalUtils.greatest(movePlanQty1, movePlanQty); // 取两个算法最大的
            }
            if (isNightClassPass) { // 夜班超量，则从夜班转移到隔天早班，但是库存可供应时长要超过12小时
                BigDecimal addPlan = BigDecimalUtils.floor(BigDecimalUtils.least(movePlanQty, midPlanQty, classStock2), toolCapacity); // 只能挪超量、交接班库存、计划量的最小值
                if (isNightSpec) { // 固定夜班则加到夜班上
                    dayAddPlan = addPlan; // 次日夜班加
                } else {
                    nightAddPlan = addPlan; // 早班加
                }
                midAddPlan = addPlan.negate(); // 夜班减
            } else if (nightPlanQty.compareTo(BigDecimal.ZERO) > 0 && !isMidSpec) { // 隔天超量，且早班大于0，则从早班转移到夜班。固定早班计划不调整
                midAddPlan = BigDecimalUtils.floor(BigDecimalUtils.least(movePlanQty, nightPlanQty), toolCapacity); // 夜班加
                nightAddPlan = midAddPlan.negate(); // 早班减
            }
            if (midAddPlan.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }
            // 先算一下是否调整后差异反而更大
            double newTotalDayPlanQty = BigDecimalUtils.add(totalMidPlanQty, midAddPlan).doubleValue();
            double newDifNum = BigDecimalUtil.sub(newTotalDayPlanQty, midPlanQtyReference); // 早班和平均值的差值
            if (Math.abs(newDifNum) > Math.abs(difNum)) { // 如果更大跳过该规格
                continue;
            }
            // 更新各班计划量
            scheduleVo.setMidPlanQty(midPlanQty.add(midAddPlan).doubleValue());
            scheduleVo.setNightPlanQty(nightPlanQty.add(nightAddPlan).doubleValue());
            scheduleVo.setDayPlanQty(dayPlanQty.add(dayAddPlan).doubleValue());
            if (midAddPlan.compareTo(BigDecimal.ZERO) != 0) {
                scheduleVo.setClassStock(this.getClassStock(scheduleVo)); // 夜班计划有变动，需要重算交接班库存
            }
            totalMidPlanQty = newTotalDayPlanQty;
            totalNightPlanQty = BigDecimalUtils.add(totalNightPlanQty, nightAddPlan).doubleValue();
            totalDayPlanQty = BigDecimalUtils.add(totalDayPlanQty, dayAddPlan).doubleValue();
            difNum = newDifNum;
            if (isNightClassPass ^ difNum > 0) { // 如果计算前后差值符号相反则直接结束
                break;
            }
        }
        totalPlanQtyVo.setTotalMidPlanQty(totalMidPlanQty); // 早班总计划里量
        totalPlanQtyVo.setTotalNightPlanQty(totalNightPlanQty); // 早班总计划里量
        totalPlanQtyVo.setTotalDayPlanQty(totalDayPlanQty); // 次日夜班总计划量
    }

    /**
     * 计算交接班库存
     * @param scheduleVo
     * @return
     */
    private Double getClassStock(GsqScheduleResultVo scheduleVo) {
        BigDecimal planQty = BigDecimalUtils.add(scheduleVo.getStockQty(), scheduleVo.getLastMidPlanQty(), scheduleVo.getMidPlanQty());
        BigDecimal cxPlanQty = BigDecimalUtils.add(scheduleVo.getCxClass1Plan(), scheduleVo.getCxClass2Plan());
        return planQty.subtract(cxPlanQty).doubleValue();
    }

    /**
     * 均衡第二天早夜班库存
     *
     * @param scheduleList   排程列表
     * @param paramsMap      工序参数
     * @param totalPlanQtyVo 胎圈中班和夜班总计划量Vo
     */
    private void equilibriumDay2(List<GsqScheduleResultVo> scheduleList, GsqTotalPlanQtyVo totalPlanQtyVo, GsqScheduleParams params) {
        this.equalShare(scheduleList, params); // 中夜班均分
        this.refreshTotalPlanQtyVo(scheduleList, totalPlanQtyVo); // 刷新统计值
        double totalNightPlanQty = totalPlanQtyVo.getTotalNightPlanQty(); // 早班总计划里量
        double totalDayPlanQty = totalPlanQtyVo.getTotalDayPlanQty(); // 次日夜班总计划量
        double toolCapacity = params.getToolCapacity(); // 满工装数
        double difNum = BigDecimalUtil.sub(totalDayPlanQty, totalNightPlanQty); //早班和次日夜班的计划量差额
        if (Math.abs(difNum) <= toolCapacity) { // 差异少于一个工装，无需处理
            return;
        }
        double bigSizeNgintPlanQty = scheduleList.stream().filter(s -> this.isBigSizeSpec(s)).collect(Collectors.summarizingDouble(GsqScheduleResultVo::getNightPlanQty)).getSum(); // 早班大尺寸规格数量
        double bigSizeDayPlanQty = scheduleList.stream().filter(s -> this.isBigSizeSpec(s)).collect(Collectors.summarizingDouble(GsqScheduleResultVo::getDayPlanQty)).getSum(); // 夜班大尺寸规格数量

        boolean isNightClassPass = difNum < 0;  //true：早班超量，false：次日夜班超量
        if (isNightClassPass) {
            // 早班超量，说明库存不足，需要从供需比例较大的（库存比较足的）开始调整
            scheduleList = scheduleList.stream()
                    .sorted(this.bigSizeSpecComparator() // 大尺寸大规格优先
                            .thenComparing(GsqScheduleResultVo::getSupplyDemandRatio, Comparator.reverseOrder()))
                    .collect(Collectors.toList());
        } else {
            // 次日夜班超量，说明库存充足，都再提前做隔天的，需要从供需比例较小的（库存比较小的）开始调整
            scheduleList = scheduleList.stream()
                    .sorted(this.bigSizeSpecComparator() // 大尺寸规格优先
                            .thenComparing(GsqScheduleResultVo::getSupplyDemandRatio))
                    .collect(Collectors.toList());
        }

        for (GsqScheduleResultVo scheduleVo: scheduleList) {
            if (ApsConstant.STATUS_ENABLE.equals(scheduleVo.getCloseOutSpecFlag())) { // 收尾规格不调整
                continue;
            }
            double nightPlanQty = scheduleVo.getNightPlanQty();
            double dayPlanQty = scheduleVo.getDayPlanQty();
            if (nightPlanQty == dayPlanQty) { // 中夜班计划量相等的不调整
                continue;
            }
            if (Arrays.stream(params.getMidSpec()).anyMatch(scheduleVo.getSteelRingCode()::equals)) { // 强制中班的规格不调整
                continue;
            }
            if (Arrays.stream(params.getNightSpec()).anyMatch(scheduleVo.getSteelRingCode()::equals)) { // 强制夜班的规格也不调整
                continue;
            }

            // 尝试平衡第二天早夜班的计划量
            boolean isNightPlanQtyLarger = nightPlanQty > dayPlanQty; // 本规格夜班计划量较大
            double diffPlanQty = BigDecimalUtil.sub(dayPlanQty, nightPlanQty); // 本计划的差异值，次日夜班 - 早班
            boolean isBigSizeSpec = this.isBigSizeSpec(scheduleVo);
            // 尝试平衡第二天早夜班的计划量
            if (isBigSizeSpec) { // 大尺寸，要同时判断大尺寸规格的总计划量
                boolean isBigSizeNightPlanQtyLarger = bigSizeNgintPlanQty > bigSizeDayPlanQty; // 大规格夜班总计划量较大
                if (isNightPlanQtyLarger != isBigSizeNightPlanQtyLarger) { // 本规格计划量较高的班次与大规格的相同才有必要调换
                    continue;
                }
            }

            if (isNightPlanQtyLarger ^ isNightClassPass) { // 本规格计划量较高的班次与总计划的相同才有必要调换
                continue;
            }
            if (Math.abs(diffPlanQty) > Math.abs(difNum)) { // 如果差异值超过了总差异，则不处理
                continue;
            }
            if (isNightPlanQtyLarger && scheduleVo.getClassStock() < scheduleVo.getCxClass3Plan()) { // 早班较大，只有交接班库存超过早班需求才能挪到夜班
                continue;
            }
            scheduleVo.setNightPlanQty(dayPlanQty);
            scheduleVo.setDayPlanQty(nightPlanQty);
            totalNightPlanQty = BigDecimalUtil.add(totalNightPlanQty, diffPlanQty); // 总早班更新为：总早班 + (次日夜班 - 早班)
            totalDayPlanQty = BigDecimalUtil.sub(totalDayPlanQty, diffPlanQty); // 总夜班更新为：总夜班 - (次日夜班 - 早班)
            if (isBigSizeSpec) {
                bigSizeNgintPlanQty = BigDecimalUtil.add(bigSizeNgintPlanQty, diffPlanQty); // 大尺寸总早班更新为：总早班 + (次日夜班 - 早班)
                bigSizeDayPlanQty = BigDecimalUtil.sub(bigSizeDayPlanQty, diffPlanQty); // 大尺寸总夜班更新为：总夜班 - (次日夜班 - 早班)
            }
            difNum = BigDecimalUtil.sub(totalDayPlanQty, totalNightPlanQty); // 重算差异
            if (Math.abs(difNum) <= toolCapacity || isNightClassPass ^ difNum < 0) { // 差异不足一个工装、或者计算前后差值符号相反则直接结束
                break;
            }
        }
        totalPlanQtyVo.setTotalNightPlanQty(totalNightPlanQty); // 早班总计划里量
        totalPlanQtyVo.setTotalDayPlanQty(totalDayPlanQty); // 次日夜班总计划量
    }


    /**
     * 单规格排产数量达到设定值（equalShareThreshold）时，中夜班数量对半分
     * @param scheduleList 排程列表
     * @param params  排产参数
     */
    private void equalShare(List<GsqScheduleResultVo> scheduleList, GsqScheduleParams params) {
        BigDecimal bisectThreshold = params.getEqualShareThreshold(); // 各班计划量均分阈值
        BigDecimal toolCapacity = BigDecimalUtils.valueOf(params.getToolCapacity()); // 满工装长度
        // 次日早夜班总计划量超过阈值的平分中夜班计划量
        for (GsqScheduleResultVo scheduleVo : scheduleList) {
            BigDecimal nightPlanQty = BigDecimalUtils.valueOf(scheduleVo.getNightPlanQty());
            BigDecimal dayPlanQty = BigDecimalUtils.valueOf(scheduleVo.getDayPlanQty());
            BigDecimal nextPlanQty = nightPlanQty.add(dayPlanQty);
            BigDecimal nextPlanQtyNum = nextPlanQty.divide(toolCapacity, 1, RoundingMode.HALF_UP); // 工装数
            if (nightPlanQty.equals(toolCapacity) && dayPlanQty.equals(toolCapacity)) { // 早夜班各一车的情况下合并
                scheduleVo.setNightPlanQty(BigDecimalUtils.add(nightPlanQty, dayPlanQty).doubleValue());
                scheduleVo.setDayPlanQty(0D);
                continue;
            }
            if (Arrays.stream(params.getMidSpec()).anyMatch(scheduleVo.getSteelRingCode()::equals)) { // 固定早班的规格不处理
                continue;
            }
            if (Arrays.stream(params.getNightSpec()).anyMatch(scheduleVo.getSteelRingCode()::equals)) { // 固定夜班的规格不处理
                continue;
            }
            if (nextPlanQty.compareTo(bisectThreshold) < 0) {
                if (nightPlanQty.compareTo(BigDecimal.ZERO) > 0) {
                    scheduleVo.setNightPlanQty(nextPlanQty.doubleValue());
                    scheduleVo.setDayPlanQty(0D);
                    continue;
                }
            } else { // 超过指定计划量，或者大尺寸规格的以工装的为单位平分
                BigDecimal newDayPlanQty = BigDecimalUtils.multiply(BigDecimalUtils.half(nextPlanQtyNum), toolCapacity); // 夜班平分后的计划量，先换算成工装数，平分后再换算成米数
                newDayPlanQty = BigDecimalUtils.least(newDayPlanQty, nextPlanQty); // 取整后的量不能超过总量
                BigDecimal newNightPlanQty = nextPlanQty.subtract(newDayPlanQty); // 早班计划 = 总计划 - 早班计划
                scheduleVo.setNightPlanQty(newNightPlanQty.doubleValue());
                scheduleVo.setDayPlanQty(newDayPlanQty.doubleValue());
            }
        }
    }

    /**
     * 大尺寸规格排序比对器，大尺寸在前
     * @return
     */
    private Comparator<GsqScheduleResultVo> bigSizeSpecComparator() {
        return new Comparator<GsqScheduleResultVo>() {
            @Override
            public int compare(GsqScheduleResultVo arg0, GsqScheduleResultVo arg1) {
                Integer bigSizeSpecFlag0 = isBigSizeSpec(arg0)? 1: -1;
                Integer bigSizeSpecFlag1 = isBigSizeSpec(arg1)? 1: -1;
                return bigSizeSpecFlag1.compareTo(bigSizeSpecFlag0);
            }
        };
    }

    /**
     * 判断是否大尺寸规格
     * @param scheduleVo
     * @return
     */
    private boolean isBigSizeSpec(GsqScheduleResultVo scheduleVo) {
//        BigDecimal bigSizeSpec = (BigDecimal) scheduleVo.getParams().get(EngineConstants.BIG_SIZE_SPEC);
//        return scheduleVo.getDimension().compareTo(bigSizeSpec) >= 0;
        return false;// TODO 暂不考虑该场景
    }

    /**
     * 加载当天库存
     *
     * @param scheduleDate
     * @return
     */
    private Map<String, Double> loadTqStock(String scheduleDate) {
        return gsqEngineStockMapper.listGsqStock(scheduleDate).stream()
                .filter(v -> StringUtils.isNotEmpty(v.getSteelRingCode()))
                .collect(Collectors.toMap(GsqStockVo::getSteelRingCode, GsqStockVo::getStockNum));
    }

    /**
     * 加载上一天的早班计划
     *
     * @param scheduleDate
     * @return
     */
    private Map<String, Double> loadLastDayMidPlan(String scheduleDate) {
        return gsqEngineStockMapper.listLastDayMidPlan(scheduleDate).stream()
                .filter(v -> StringUtils.isNotEmpty(v.getSteelRingCode()))
                .collect(Collectors.toMap(GsqStockConsumeVo::getSteelRingCode, GsqStockConsumeVo::getConsume));
    }


    /**
     * 根据预生产库存倍数重算计划量
     * @param scheduleVo 计划量
     * @param reserveStockRate 预生产库存倍数
     */
    private void computeReserveStock(GsqScheduleResultVo scheduleVo, BigDecimal reserveStockRate) {
        //中班计划量
        double midPlanQty = scheduleVo.getMidPlanQty();
        //夜班计划量
        double nightPlanQty = scheduleVo.getNightPlanQty();
        //白班计划量
        double dayPlanQty = scheduleVo.getDayPlanQty();

        scheduleVo.setMidPlanQty(Math.ceil(BigDecimalUtil.mul(midPlanQty, reserveStockRate.doubleValue())));
        scheduleVo.setNightPlanQty(Math.ceil(BigDecimalUtil.mul(nightPlanQty, reserveStockRate.doubleValue())));
        scheduleVo.setDayPlanQty(Math.ceil(BigDecimalUtil.mul(dayPlanQty, reserveStockRate.doubleValue())));
    }

    /**
     * 验证成型排程记录的胎胚code在施工表中是否都能找到对应记录，如果不能则提示
     * @param scheduleDate 排程日志
     * @param batchNo 批次号
     * @param productionStage 仅投产阶段规格排产标识
     */
    private void ValidatedConstruction(String scheduleDate, String batchNo, String productionStage, Map<String, String> mapAssistSpec) {
        List<String> list = commonMapper.listLossConstructionForTq(scheduleDate);
        if(list != null && !list.isEmpty()) {
            String tip = I18nUtil.getMessage("gsq.engine.auto.scheule.validated");
            String embryoCodes = String.join(",", list);
            tip = String.format(tip, embryoCodes);
            autoScheduleLogService.insertGsqScheduleLog(batchNo, "", "自动排程失败", "自动排程失败，原因：成型排程数据为空，或没有在施工信息中找到对应的物料"); //添加日志
            throw new RuntimeException(tip);
        }

        List<EngineConstructionInfo> list1 = gsqEngineMapper.listGsqNeedConstruction(scheduleDate, productionStage);
        list1 = list1.stream().filter(r -> !mapAssistSpec.containsKey(r.getBeadCode())).collect(Collectors.toList());  //校验忽略掉 外协规格，只校验 不是外协的规格
        for(EngineConstructionInfo construction : list1) {
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
            if(StringUtils.isBlank(construction.getBeadCode())) {
                //施工表钢丝圈代码为空
                errorColumns.add("\"" + I18nUtil.getMessage("ui.construction.beadCode") + "\"");
            }
            if(StringUtils.isBlank(construction.getBeadType())) {
                //施工表钢丝类型代码为空
                errorColumns.add("\"" + I18nUtil.getMessage("ui.construction.beadType") + "\"");
            }
            if(StringUtils.isBlank(construction.getBeadArrange())) {
                //施工表钢丝圈排列为空
                errorColumns.add("\"" + I18nUtil.getMessage("ui.construction.beadArrange") + "\"");
            }
            if(construction.getDimension() == null || construction.getDimension() == 0) {
                //施工表钢丝圈长为空
                errorColumns.add("\"" + I18nUtil.getMessage("ui.construction.dimension") + "\"");
            }
            if(!errorColumns.isEmpty()) {
                String tip = StringUtils.format(I18nUtil.getMessage("engine.auto.scheule.construction.validate"), embryoCode, embryoVersion, String.join(",", errorColumns));
                autoScheduleLogService.insertGsqScheduleLog(batchNo, "", "自动排程失败", tip); //添加日志
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
        List<String> listAssistSpec = this.gsqEngineMapper.listAssistSpec();
        if(listAssistSpec == null || listAssistSpec.size() == 0) {
            return map;
        }
        for(String assistSpec : listAssistSpec) {
            map.put(assistSpec, "1");
        }
        return map;
    }

    /**
     * 钢丝圈插单
     * @param scheduleVo
     */
    public int inertGsqOrder(GsqScheduleResultVo scheduleVo) {
        String scheduleDate = DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD, scheduleVo.getScheduleDate()); //排程日期
        List<GsqScheduleResultVo> scheduleList = new ArrayList<>();
        scheduleList.add(scheduleVo);
        return this.batchSaveGsqSchedule(scheduleDate, scheduleList, true);
    }

    /**
     * 批量更新或新增排程记录信息
     * @param scheduleDate 排程日志，格式：yyyy-MM-dd
     * @param scheduleList 排程数据
     */
    public int batchSaveGsqSchedule(String scheduleDate, List<GsqScheduleResultVo> scheduleList) {
        return this.batchSaveGsqSchedule(scheduleDate, scheduleList, false);
    }

    /**
     * 批量更新或新增排程记录信息
     * @param scheduleDate 排程日志，格式：yyyy-MM-dd
     * @param scheduleList 排程数据
     * @param isUpdate 相同唯一键是否做更新操作。true：是
     */
    @Transactional(rollbackFor=Exception.class)
    public int batchSaveGsqSchedule(String scheduleDate, List<GsqScheduleResultVo> scheduleList, boolean isUpdate) {
        if(scheduleList == null || scheduleList.isEmpty()) {
            return -1;
        }
        String batchNo = "";
        if(isUpdate) {
            batchNo = gsqEngineMapper.getGsqCurrentBatchNo(scheduleDate);  //查询当前排程的批次号
        }
        if(StringUtils.isBlank(batchNo)) {
            //当前的批次号为空，说明还没”自动排程“，那么自己生成一个排程批次号
            batchNo = this.createBatchNo(scheduleDate);  //钢丝圈排程批次号
            this.createScheduleRecord(scheduleDate, "", batchNo);  //创建自动排程记录
            this.syncGsqScheduleToLog(scheduleDate);  //把排程数据同步到log表
        }
        autoScheduleLogService.insertGsqScheduleLog(batchNo, "", "插单或批量导入初始数据", toJSONString(scheduleList));  //添加日志

        List<String> steelRingCodes = scheduleList.stream().map(GsqScheduleResultVo::getSteelRingCode).collect(Collectors.toList());
        GsqScheduleParams params = this.loadParams();  // 获取工序参数map
        String productionStage = params.getProductionStage();  //仅投产阶段规格排产标识
        Map<String, GsqScheduleBaseInfoVo> scheduleBaseInfoMap = getScheduleBaseInfoMap(scheduleDate, steelRingCodes, productionStage);  //根据钢丝圈代码查询对应的钢丝圈基础信息
        Map<String, Double> planStockMap = gsqEngineStockService.getPlanStockMap(batchNo, scheduleDate, params.getStockLossRate());  //计算钢丝圈16点预计库存
        Map<String, GsqMonthSurplusVo> monthSurplus = gsqEngineMonthSurplusService.getMonthSurplus(scheduleDate);  //获得月度计划剩余量、完成量
        autoScheduleLogService.insertGsqScheduleLog(batchNo, "", "插单或批量导入基础数据", logSplit("半部件基础数据信息:" + toJSONString(scheduleBaseInfoMap),
                "16点预计库存：" + planStockMap, "月度计划剩余量、完成量：" + monthSurplus, "工序参数map（这里值用到：" + toJSONString(params)));  //添加日志

        for(GsqScheduleResultVo schedule : scheduleList) {
            schedule.setBatchNo(batchNo);  //批次号
            String orderNo = this.createOrderNo(batchNo); //工单号
            schedule.setOrderNo(orderNo);  //工单号
            GsqScheduleBaseInfoVo baseInfoVo = scheduleBaseInfoMap.get(schedule.getSteelRingCode());
            if(baseInfoVo != null) {
                BeanUtils.copyProperties(baseInfoVo, schedule);
            }

            Double midPlanQty = schedule.getMidPlanQty();  //中班计划量
            schedule.setMidPlanQty(midPlanQty == null ? 0D : midPlanQty);
            Double nightPlanQty = schedule.getNightPlanQty();  //夜班计划量
            schedule.setNightPlanQty(nightPlanQty == null ? 0D : nightPlanQty);
            Double dayPlanQty = schedule.getDayPlanQty();  //白班计划量
            schedule.setDayPlanQty(dayPlanQty == null ? 0D : dayPlanQty);

            schedule.setStockQty(planStockMap.getOrDefault(schedule.getSteelRingCode(), 0D));  //16点预计库存
            this.newComputeSupplyTime(schedule, schedule.getStockQty());  //库存供应时长
            this.setStatusAndCloseTip(schedule, monthSurplus.get(schedule.getSteelRingCode()), params.getCloseOutNum());  //设置收尾提示标识 和 生产状态字段
            schedule.setUnitConsume(2D);  //单耗：1条胎需要2个钢丝圈
            schedule.setIsRelease(ApsConstant.NO_RELEASE);
            schedule.setDelFlag(ApsConstant.DEL_FLAG_NORMAL);
            schedule.setCreateTime(new Date());
            schedule.setCreateBy(SecurityUtils.getUsername());
        }
        autoScheduleLogService.insertGsqScheduleLog(batchNo, "", "插单或批量导入最终数据", toJSONString(scheduleList));  //添加日志
        return gsqEngineMapper.mergeGsqScheduleResult(scheduleList);  //批量合并排程结果表（根据唯一字段，做更新或新增）
    }

    /**
     * 根据钢丝圈代码查询对应的钢丝圈基础信息
     * @param scheduleDate
     * @return
     */
    private Map<String, GsqScheduleBaseInfoVo> getScheduleBaseInfoMap(String scheduleDate, List<String> steelRingCodes, String productionStage) {
        Map<String, GsqScheduleBaseInfoVo> map = new HashMap<>();
        List<GsqScheduleBaseInfoVo> list = gsqEngineMapper.listGsqScheduleBaseInfo(steelRingCodes, ""); //查询出钢丝圈在施工表的基础信息
        if(!StringUtils.isEmpty(list)) {
            map = list.stream().collect(Collectors.toMap(GsqScheduleBaseInfoVo::getSteelRingCode, baseInfoVo->baseInfoVo));
        }

        Map<String, GsqScheduleBaseInfoVo> hasCxMap = new HashMap<>();
        List<GsqScheduleResultVo> hasCxlist = gsqEngineMapper.statGsqScheduleBase(scheduleDate, productionStage); //查询出在有对应成型排程的胎面基础信息
        for(GsqScheduleResultVo info : hasCxlist) {
            GsqScheduleBaseInfoVo baseInfoVo = new GsqScheduleBaseInfoVo();
            BeanUtils.copyProperties(info, baseInfoVo);
            hasCxMap.put(info.getSteelRingCode(), baseInfoVo);
        }

        map.putAll(hasCxMap);  //有对应成型排程的胎面基础信息 覆盖掉，没有成型排程的胎面基础信息
        return map;
    }

//    /**
//     * 转机台后，修改排程结果表相应字段数据
//     * @param oldMachineIds  转机台前，旧的机台id
//     * @param scheduleResultVo
//     */
//    public void changeGsqMachine(String oldMachineIds, GsqScheduleResultVo scheduleResultVo) {
//        String batchNo = scheduleResultVo.getBatchNo();  //批次号
//        String orderNo = scheduleResultVo.getOrderNo();  //工单号
//        autoScheduleLogService.insertGsqScheduleLog(batchNo, orderNo, "转机台初始数据", logSplit("转机台前的机台ID：" + oldMachineIds, "页面提交的信息：" + toJSONString(scheduleResultVo)));  //添加日志
//        Map<String, Double> lossRateMap = gsqEngineLossService.getLossRateMap();   //损耗率map
//        Map<String, String> paramsMap = this.getParamsMap();  // 获取工序参数map
//        double paramLossRate = getDouble(paramsMap.get(EngineConstants.LOSS_RATE));
//
//        //转机台后，不同机台的损耗率不一样，需要重新计算计划量
//        double oldLossRate = gsqEngineLossService.getLossRate(scheduleResultVo.getSteelRingCode(), oldMachineIds, lossRateMap, paramLossRate);  //计算出转机台前的耗损率
//        double lossRate = gsqEngineLossService.getLossRate(scheduleResultVo.getSteelRingCode(), scheduleResultVo.getMachineId(), lossRateMap, paramLossRate);  //计算出新机台的耗损率
//        autoScheduleLogService.insertGsqScheduleLog(batchNo, orderNo, "转机台需要根据不同机台耗损率重新计算计划量",
//                logSplit("重新计算计划量规则：先要根据之前机台的耗损率推算出之前在没有加上耗损率之前的计划量A，然后再用计划量A * 当前机台对应的耗损率，计算出最终的计划量", "转机台前的耗损率：" + oldLossRate + "转机台后的耗损率：" + lossRate));  //添加日志
//
//
//        Double midPlanQty = scheduleResultVo.getMidPlanQty();  //中班计划量
//        if(midPlanQty != null) {
//            midPlanQty = BigDecimalUtil.div(midPlanQty, 1 + oldLossRate, 4); //计算出之前没有加上损耗量的 计划量
//            midPlanQty = BigDecimalUtil.add(midPlanQty, BigDecimalUtil.mul(midPlanQty, lossRate));
//            scheduleResultVo.setNightPlanQty(midPlanQty);
//        }
//        Double nightPlanQty = scheduleResultVo.getNightPlanQty();  //夜班计划量
//        if(nightPlanQty != null) {
//            nightPlanQty = BigDecimalUtil.div(nightPlanQty, 1 + oldLossRate, 4); //计算出之前没有加上损耗量的 计划量
//            nightPlanQty = BigDecimalUtil.add(nightPlanQty, BigDecimalUtil.mul(nightPlanQty, lossRate));
//            scheduleResultVo.setNightPlanQty(nightPlanQty);
//        }
//        Double dayPlanQty = scheduleResultVo.getDayPlanQty();  //白班计划量
//        if(dayPlanQty != null) {
//            dayPlanQty = BigDecimalUtil.div(dayPlanQty, 1 + oldLossRate, 4); //计算出之前没有加上损耗量的 计划量
//            dayPlanQty = BigDecimalUtil.add(dayPlanQty, BigDecimalUtil.mul(dayPlanQty, lossRate));
//            scheduleResultVo.setDayPlanQty(dayPlanQty);
//        }
//        autoScheduleLogService.insertGsqScheduleLog(batchNo, orderNo, "转机台结束后的排程数据", toJSONString(scheduleResultVo));  //添加日志
//    }

    /**
     * 确认自动排程机台
     * @param scheduleResult  排程信息
     */
    public void confirmGsqMachine(GsqScheduleResultDto scheduleResult) {
        String batchNo = scheduleResult.getBatchNo();  //批次号
        String orderNo = scheduleResult.getOrderNo();  //工单号
        autoScheduleLogService.insertGsqScheduleLog(batchNo, orderNo, "确认机台初始数据", logSplit( "页面提交的信息：" + toJSONString(scheduleResult)));  //添加日志
        Map<String, Double> lossRateMap = gsqEngineLossService.getLossRateMap();   //损耗率map
        GsqScheduleParams params = this.loadParams();  // 获取工序参数map
        double paramLossRate = params.getLossRate();

        //耗损率
        double lossRate = gsqEngineLossService.getLossRate(scheduleResult.getSteelRingCode(), scheduleResult.getMachineId(), lossRateMap, paramLossRate);  //计算出新机台的耗损率
        autoScheduleLogService.insertGsqScheduleLog(batchNo, orderNo, "确认机台耗损率", "耗损率：" + lossRate);  //添加日志

        Double midPlanQty = scheduleResult.getMidPlanQty();  //中班计划量
        if(midPlanQty != null) {
            midPlanQty = BigDecimalUtil.add(midPlanQty, BigDecimalUtil.mul(midPlanQty, lossRate));
            scheduleResult.setMidPlanQty(BigDecimalUtil.roundUp(midPlanQty,0));
        }
        Double nightPlanQty = scheduleResult.getNightPlanQty();  //夜班计划量
        if(nightPlanQty != null) {
            nightPlanQty = BigDecimalUtil.add(nightPlanQty, BigDecimalUtil.mul(nightPlanQty, lossRate));
            scheduleResult.setNightPlanQty(BigDecimalUtil.roundUp(nightPlanQty,0));
        }
        Double dayPlanQty = scheduleResult.getDayPlanQty();  //白班计划量
        if(dayPlanQty != null) {
            dayPlanQty = BigDecimalUtil.add(dayPlanQty, BigDecimalUtil.mul(dayPlanQty, lossRate));
            scheduleResult.setDayPlanQty(BigDecimalUtil.roundUp(dayPlanQty,0));
        }
        autoScheduleLogService.insertGsqScheduleLog(batchNo, orderNo, "确认机台结束后的排程数据", toJSONString(scheduleResult));  //添加日志
    }

    /**
     * 均衡日志
     * @param scheduleList
     * @param paramsMap
     * @param totalPlanQtyVo
     */
    private void equilibriumLog(String batchNo, String oldScheduleList, List<GsqScheduleResultVo> scheduleList, Map<String, String> paramsMap, GsqTotalPlanQtyVo totalPlanQtyVo) {
        StringBuffer logDetail = new StringBuffer("");
        logDetail.append("对排产结果进行均衡操作。中班总量和夜班总量的差额百分比超过了参数配置的百分比，则需要做均衡处理，也就是说要把其中一班的计划量合并到另外一班，" +
                "一直合并到中班和夜班计划量总量的差额不超过参数配置的百分比。其中中班合并到夜班还需要遵循一个规则，就是只有库存供应时长必须要大于参数配置的值的时候，才允许从中班合并到夜班。").append(division);
        logDetail.append("各班总计划量：" + toJSONString(totalPlanQtyVo)).append(division);
        logDetail.append("参数配置集合，这里要用到‘PLAN_DIFFERENCE_RATE（中班总量和夜班总量差额百分比）’和‘SUPPLY_TIME_PASS（库存供应时长小时数）’：" + toJSONString(paramsMap)).append(division);
        logDetail.append("均衡前的排程数据列表：" + oldScheduleList).append(division);
        logDetail.append("均衡后的排产数据列表：" + toJSONString(scheduleList));
        autoScheduleLogService.insertGsqScheduleLog(batchNo, "", "计划量均衡处理", logDetail.toString());
    }

    /**
     * 根据库存供应时长，设置中班和夜班的生产顺序（有计划量的才设置生产顺序）
     * @param scheduleList
     */
    private void setProduceOrder(List<GsqScheduleResultVo> scheduleList) {
        int midProduceOrder = 1;  //中班生产顺序
        int nightProduceOrder = 1;  //夜班生产顺序
        int dayProduceOrder = 1; //白班生产顺序

        //根据库存供应时长升序排序
        scheduleList = scheduleList.stream().sorted(Comparator.comparing(GsqScheduleResultVo::getSupplyTime)).collect(Collectors.toList());
        for(GsqScheduleResultVo scheduleVo : scheduleList) {
            Double midPlanQty = scheduleVo.getMidPlanQty();
            Double nightPlanQty = scheduleVo.getNightPlanQty();
            Double dayPlanQty = scheduleVo.getDayPlanQty();
            Integer notOrderTag = scheduleVo.getNotOrderTag();  //不需要参与生产顺序排程tag。值不为空，表示不需要参与排序

            if(midPlanQty > 0 && notOrderTag == null) {
                scheduleVo.setMidProduceOrder(midProduceOrder++);
            }
            if(nightPlanQty > 0 && notOrderTag == null) {
                scheduleVo.setNightProduceOrder(nightProduceOrder++);
            }
            if(dayPlanQty > 0 && notOrderTag == null) {
                scheduleVo.setDayProduceOrder(dayProduceOrder++);
            }
            autoScheduleLogService.insertGsqScheduleLog(scheduleVo.getBatchNo(), scheduleVo.getOrderNo(), "设置生产顺序字段",
                    logSplit("根据库存供应时长(从小到大)，设置各班的生产顺序（有计划量的才设置生产顺序）", "设置后的排程数据：" + toJSONString(scheduleVo)));  //添加日志
        }
    }

    /**
     * 设置收尾提示标识 和 生产状态字段
     * @param scheduleResultVo
     * @param monthSurplusVo
     * @param closeOutNum  参数配置表设置的 提示收尾阈值
     */
    private void setStatusAndCloseTip(GsqScheduleResultVo scheduleResultVo, GsqMonthSurplusVo monthSurplusVo, Double closeOutNum) {
        if(monthSurplusVo == null) {
            scheduleResultVo.setMarkCloseOutTip(EngineConstants.CLOSE_TIP_NOT);
            scheduleResultVo.setProductionStatus(EngineConstants.PRODUCTION_STATUS_NOT);
            log.error("月计划汇总数据为空，物料编号为：", scheduleResultVo.getSteelRingCode());
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
        autoScheduleLogService.insertGsqScheduleLog(scheduleResultVo.getBatchNo(), scheduleResultVo.getOrderNo(), "设置收尾提示标识markCloseOutTip",
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
        autoScheduleLogService.insertGsqScheduleLog(scheduleResultVo.getBatchNo(), scheduleResultVo.getOrderNo(), "修改生产状态productionStatus",
                logSplit("①完成量为0，对应生产状态：未生产;②完成量大于0，月度计划量也大于0，说明出于生产中;③月度计划量小于等于0，说明出于生产完成",
                        "月度计划剩余量：" + monthRemainQty + ",月度计划完成量：" + monthFinishQty, "最终的排程数据：" + toJSONString(scheduleResultVo)));  //添加日志
    }

    /**
     * 如果当天排程已经存在，则把当天的排程合并到 自动排程的列表中
     * @param batchNo   批次号
     * @param autoScheduleList   自动排程列表
     * @param existScheduleList  当天已经存在的排产记录
     */
    private List<GsqScheduleResultVo> mergeExistSchedule(String batchNo, List<GsqScheduleResultVo> autoScheduleList, List<GsqScheduleResultVo> existScheduleList) {
        if(StringUtils.isEmpty(existScheduleList)) {
            return autoScheduleList;
        }
        List<GsqScheduleResultVo> mergeList = new ArrayList<>();

        Map<String, List<GsqScheduleResultVo>> existScheduleMap = existScheduleList.stream().filter(s->s.getPublishSuccessCount()>0)
                .collect(Collectors.groupingBy(GsqScheduleResultVo::getSteelRingCode)); //拿到重排前，已经有发布给MES的排产数据。key为 半部件规格代码

        for(GsqScheduleResultVo autoSchedule : autoScheduleList) {
            List<GsqScheduleResultVo> existScheduleGroupList = existScheduleMap.get(autoSchedule.getSteelRingCode());

            if(existScheduleGroupList != null && existScheduleGroupList.size() == 1) {
                //对应规格重排前已经发布，并且此规格重排前只有一条排程记录（只对应了一个机台）
                GsqScheduleResultVo existSchedule = existScheduleGroupList.get(0);
                //重排前的数据如果已经发布过，在重新排程后仍有相应的生产需求，计划量按照重新自动排程的计划量安排；订单号需要和之前发布个mes的订单号一致
                autoSchedule.setOrderNo(existSchedule.getOrderNo());  //订单号
                autoSchedule.setPublishSuccessCount(existSchedule.getPublishSuccessCount());
                autoSchedule.setNewestPublishTime(existSchedule.getNewestPublishTime());
                autoSchedule.setIsRelease(ApsConstant.WAIT_RELEASING);  //发布状态修改
                autoSchedule.setMachineId(existSchedule.getMachineId());  //机台沿用重排前的机台
                mergeList.add(autoSchedule);
            } else if(existScheduleGroupList != null && existScheduleGroupList.size() > 1) {
                //对应规格重排前已经发布，并且此规格重排前只有多条排程记录（对应了多个机台）。那需要保留重排之前的排产，并且要把此规格重排后的各班的计划量，拼接到备注中
                String remarkTip = I18nUtil.getMessage("reschedule.double.spec.remark1");
                remarkTip = StringUtils.format(remarkTip, stripZeros(autoSchedule.getMidPlanQty()), stripZeros(autoSchedule.getNightPlanQty()) ,stripZeros(autoSchedule.getDayPlanQty()));
                for(GsqScheduleResultVo existSchedule : existScheduleGroupList) {
                    existSchedule.setBatchNo(batchNo);
                    existSchedule.setRemark(remarkTip);
                    mergeList.add(existSchedule);
                }
            } else {
                //对应的规格，重排前没有找到相应记录
                mergeList.add(autoSchedule);
            }
            existScheduleMap.remove(autoSchedule.getSteelRingCode());
        }

        //重排前的已发布的规格如果没有在重排后的列表中，则需要把对应的规格也加入到最新的排程列表中
        for(List<GsqScheduleResultVo> list : existScheduleMap.values()) {
            list.forEach(r->r.setBatchNo(batchNo));
            mergeList.addAll(list);
        }
        return mergeList;
    }

    /**
     * 创建自动排程记录
     *
     * @param scheduleDate 排程日期
     * @param tqBatchNo    对成型批次号
     * @param batchNo      钢丝圈批次号
     */
    private void createScheduleRecord(String scheduleDate, String tqBatchNo, String batchNo) {
        Map<String, Object> params = new HashMap<>();
        params.put("scheduleDate", scheduleDate);
        params.put("tqBatchNo", tqBatchNo);
        params.put("batchNo", batchNo);
        params.put("userName", SecurityUtils.getUsername());  //用户名
        gsqEngineMapper.createScheduleRecord(params);
    }

    /**
     * 把排程数据同步到log表
     *
     * @param scheduleDate 排程日期，格式：yyyy-MM-dd
     */
    private void syncGsqScheduleToLog(String scheduleDate) {
        gsqEngineMapper.syncGsqScheduleToLog(scheduleDate);
        gsqEngineMapper.deleteGsqSchedule(scheduleDate);
        gsqEngineMapper.deleteGsqAssistSchedule(scheduleDate);
    }

    /**
     * 生产线挑选(优先选择“定点机台”匹配上的机台，如果没有，在选择“口型板”的机台信息)
     *
     * @param scheduleVo
     * @param specifyCanMachineMap  定点机台中限制作业map
     * @param specifyNotMachineMap  定点机台中不可作业
     * @param twiningDiscMachineMap 缠绕盘和机台关系集合（key = 规格尺寸~排列方式）
     * @param twiningDiscMap 获得钢丝圈代码和缠绕盘集合（value = 规格尺寸~排列方式）map
     */
    private void chooseMachine(List<GsqScheduleResultVo> scheduleList, List<GsqMachineInfo> allMachineList, Map<String, String> specifyCanMachineMap, Map<String, String> specifyNotMachineMap,
                               Map<String, String> twiningDiscMachineMap, Map<String, String> twiningDiscMap) {
        if (CollectionUtils.isEmpty(scheduleList)) {
            return;
        }
        Map<Long, BigDecimal> midCapacityMap = new HashMap<>(); // 机台夜班已占用产能
        Map<Long, BigDecimal> nightCapacityMap = new HashMap<>(); // 机台白班已占用产能
        Map<String, Long> plannedMachineMap = gsqEngineMachineService
                .getLastDayPlanMachine(CollectionUtil.firstElement(scheduleList).getScheduleDate());// 已排规格，初始为上一个班的规格
        // 先对排产计划排序
        List<GsqScheduleResultVo> chooseMachineScheduleList = scheduleList.stream().sorted(new Comparator<GsqScheduleResultVo>() {
            @Override
            public int compare(GsqScheduleResultVo o1, GsqScheduleResultVo o2) {
                Integer flag1 = specifyCanMachineMap.containsKey(o1.getSteelRingCode())? 1: 2;
                Integer flag2 = specifyCanMachineMap.containsKey(o2.getSteelRingCode())? 1: 2;
                if (flag1.compareTo(flag2) != 0) { // 先看哪个有定点机台，有定点机台的优先选机台
                    return flag1.compareTo(flag2);
                }
                // 如果定点机台设置一样，则按计划量从大到小
                BigDecimal planQty1 = BigDecimalUtils.add(o1.getMidPlanQty(), o1.getNightPlanQty());
                BigDecimal planQty2 = BigDecimalUtils.add(o2.getMidPlanQty(), o2.getNightPlanQty());
                return planQty2.compareTo(planQty1);
            }
        }).collect(Collectors.toList());

        // 根据夜班计划分配机台
        for (GsqScheduleResultVo scheduleVo : chooseMachineScheduleList) {
            Double midPlanQty = scheduleVo.getMidPlanQty();
            if (midPlanQty == null || midPlanQty <= 0) {
                continue;
            }
            String classCode = String.valueOf(OpenMachineClassEnums.CLASS_TWO.getClassIndex()); // 夜班
            List<GsqMachineInfo> optionalMachineList = this.searchOptionalMachineList(scheduleVo, classCode, midCapacityMap,
                    allMachineList, specifyCanMachineMap, specifyNotMachineMap, twiningDiscMap, plannedMachineMap); // 检索当班可选机台
            if (CollectionUtil.isEmpty(optionalMachineList)) {
                continue;
            }
            // 如果有匹配机台，则直接取第一个机台赋值
            GsqMachineInfo machine = CollectionUtil.firstElement(optionalMachineList);
            Long machineId = machine.getId();
            scheduleVo.setMachineId(String.valueOf(machineId));
            //检查机台，如果早班不作业，则把计划量都转移到夜班
            if (!machine.getOpenMachineClass().contains(String.valueOf(OpenMachineClassEnums.CLASS_THREE.getClassIndex()))) {
                scheduleVo.setMidPlanQty(BigDecimalUtil.add(midPlanQty, scheduleVo.getNightPlanQty()));
                scheduleVo.setNightPlanQty(0D);
            }
            // 占用机台各班产能
            midCapacityMap.put(machineId, midCapacityMap.getOrDefault(machineId, BigDecimal.ZERO).add(BigDecimalUtils.valueOf(scheduleVo.getMidPlanQty())));
            nightCapacityMap.put(machineId, nightCapacityMap.getOrDefault(machineId, BigDecimal.ZERO).add(BigDecimalUtils.valueOf(scheduleVo.getNightPlanQty())));
            plannedMachineMap.put(scheduleVo.getSteelRingCode(), machineId);
            chooseMachineLog(scheduleVo, specifyCanMachineMap, specifyNotMachineMap, twiningDiscMachineMap, twiningDiscMap); // 添加日志
        }

        // 剩余没有分配到机台的排程检查早班是否有可分配机台
        for (GsqScheduleResultVo scheduleVo : chooseMachineScheduleList) {
            if (StringUtils.isNotEmpty(scheduleVo.getMachineId())) {
                continue;
            }
            String classCode = String.valueOf(OpenMachineClassEnums.CLASS_THREE.getClassIndex()); // 早班
            List<GsqMachineInfo> optionalMachineList = this.searchOptionalMachineList(scheduleVo, classCode, nightCapacityMap,
                    allMachineList, specifyCanMachineMap, specifyNotMachineMap, twiningDiscMachineMap, plannedMachineMap); // 检索当班可选机台
            if (CollectionUtil.isEmpty(optionalMachineList)) {
                continue;
            }
            // 如果有匹配机台，则直接取第一个机台赋值
            GsqMachineInfo machine = CollectionUtil.firstElement(optionalMachineList);
            Long machineId = machine.getId();
            scheduleVo.setMachineId(String.valueOf(machineId));
            //检查机台，如果夜班不作业，则把计划量都转移到早班
            if (!machine.getOpenMachineClass().contains(String.valueOf(OpenMachineClassEnums.CLASS_TWO.getClassIndex()))) {
                scheduleVo.setNightPlanQty(BigDecimalUtil.add(scheduleVo.getMidPlanQty(), scheduleVo.getNightPlanQty()));
                scheduleVo.setMidPlanQty(0D);
            }
            // 占用机台各班产能
            midCapacityMap.put(machineId, midCapacityMap.getOrDefault(machineId, BigDecimal.ZERO).add(BigDecimalUtils.valueOf(scheduleVo.getMidPlanQty())));
            nightCapacityMap.put(machineId, nightCapacityMap.getOrDefault(machineId, BigDecimal.ZERO).add(BigDecimalUtils.valueOf(scheduleVo.getNightPlanQty())));
            plannedMachineMap.put(scheduleVo.getSteelRingCode(), machineId);
            chooseMachineLog(scheduleVo, specifyCanMachineMap, specifyNotMachineMap, twiningDiscMachineMap, twiningDiscMap); // 添加日志
        }
    }

    /**
     * 检索复合条件的机台
     * 1、根据机台状态、开机班次确定哪个班次可以排<br/>
     * 2、选择机台要看机台已分配产能，优先选择已分配产能较低的机台<br/>
     * 3、可选择机台限制：1、定点机台（包括限制生产和不可生产）。2、施工的寸口必须在机台的“工装信息”范围之内<br/>
     * 4、胎圈规格根据优先级分配到机台。优先级：1、定点限制生产机台；2、可供成型生产时长最低的
     * @param scheduleVo
     * @param classCode
     * @param capacityMap
     * @param allMachineList
     * @param specifyCanMachineMap
     * @param specifyNotMachineMap
     * @param mouthPlateMachineMap
     * @param plannedMachineMap 已排产机台
     * @return
     */
    private List<GsqMachineInfo> searchOptionalMachineList(GsqScheduleResultVo scheduleVo, String classCode,
            Map<Long, BigDecimal> capacityMap, List<GsqMachineInfo> allMachineList,
            Map<String, String> specifyCanMachineMap, Map<String, String> specifyNotMachineMap,
            Map<String, String> twiningDiscMachineMap, Map<String, Long> plannedMachineMap) {
        String steelRingCode = scheduleVo.getSteelRingCode(); // 钢丝圈代码
        BigDecimal dimension = scheduleVo.getDimension(); // 寸口
        // 定点机台ID列表
        String specifyMachineIds = specifyCanMachineMap.get(steelRingCode);
        specifyMachineIds = StringUtils.isBlank(specifyMachineIds) ? twiningDiscMachineMap.get(twiningDiscMachineMap.get(steelRingCode))
                : specifyMachineIds; // 从钢丝缠绕盘设置中找机台
        List<String> machineIds;
        // 如果有设置定点机台，需要把非定点全部过滤掉
        if (StringUtils.isNotEmpty(specifyMachineIds)) {
            machineIds = Arrays.asList(specifyMachineIds.split(","));
        } else {
            machineIds = new ArrayList<>(0);
        }
        // 可选机台
        List<GsqMachineInfo> optionalMachineList = allMachineList.stream().filter(m -> {// 排除定点不可生产机台
            String machineId = String.valueOf(m.getId());
            String notMachine = specifyNotMachineMap.get(steelRingCode);
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
                .filter(m -> {// 寸口需要在工装范围内
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
                    return true; // 没配置工装信息，相当于全部都可以生产
                }).sorted(new Comparator<GsqMachineInfo>() {// 按剩余产能升序排序
                    @Override
                    public int compare(GsqMachineInfo m1, GsqMachineInfo m2) {
                        // 同一个规格优先排在已排过相同规格的机台上
                        Long scheduleMachineId = plannedMachineMap.getOrDefault(steelRingCode, 0L);
                        Integer hasMachine1 = m1.getId().equals(scheduleMachineId) ? 0 : 1;
                        Integer hasMachine2 = m2.getId().equals(scheduleMachineId) ? 0 : 1;
                        int result = hasMachine1.compareTo(hasMachine2);
                        if (result != 0) {
                            return result;
                        }
                        // 按剩余产能升序排序
                        BigDecimal capacity1 = capacityMap.getOrDefault(m1.getId(), BigDecimal.ZERO);
                        BigDecimal capacity2 = capacityMap.getOrDefault(m2.getId(), BigDecimal.ZERO);
                        result = capacity1.compareTo(capacity2);
                        if (result != 0) {
                            return result;
                        }
                        result = m1.getId().compareTo(m2.getId());
                        return result;
                    }
                }).collect(Collectors.toList());
        return optionalMachineList;
    }

    /**
     * 设置生产线日志
     * @param scheduleVo
     * @param specifyCanMachineMap
     * @param specifyNotMachineMap
     * @param twiningDiscMachineMap
     * @param twiningDiscMap
     */
    private void chooseMachineLog(GsqScheduleResultVo scheduleVo, Map<String, String> specifyCanMachineMap, Map<String, String> specifyNotMachineMap,
                                  Map<String, String> twiningDiscMachineMap, Map<String, String> twiningDiscMap) {
        StringBuffer logDetail = new StringBuffer("");
        logDetail.append("①优先选择“定点机台中限制作业集合”匹配上的机台;②如果没有，在选择“缠绕盘与机台对应关系集合”的机台信息，不过需要过滤掉'定点机台中不可作业'中的机台").append(division);
        logDetail.append("定点机台中限制作业集合：" + toJSONString(specifyCanMachineMap)).append(division);
        logDetail.append("定点机台中不可作业集合：" + toJSONString(specifyNotMachineMap)).append(division);
        logDetail.append("缠绕盘（尺寸~排列方式）与机台对应关系集合：" + toJSONString(twiningDiscMachineMap)).append(division);
        logDetail.append("钢丝圈和缠绕盘（尺寸~排列方式）对应计划：" + toJSONString(twiningDiscMachineMap)).append(division);
        logDetail.append("结果数据：" + toJSONString(scheduleVo)).append(division);
        autoScheduleLogService.insertGsqScheduleLog(scheduleVo.getBatchNo(), scheduleVo.getOrderNo(), "设置生产线（机台）", logDetail.toString());
    }

    /**
     * （新）计算并设置供成型库存供应时长（小时）。
     * 具体算法：从1班开始判断，预计库存-1班的计划大于等于0时，供应时长+8小时；预计库存-1班计划-2班计划大于等于0时，供应时长+16小时；
     *         预计库存-1班计划-2班计划-3班计划小于0，供应时长=16个小时+（((预计库存-1班计划-2班计划)/3班计划)*8）
     * @param scheduleVo
     * @param stockQty
     */
    private void newComputeSupplyTime(GsqScheduleResultVo scheduleVo,  Double stockQty) {
        Double cxClass1Plan = (scheduleVo.getCxClass1Plan() == null ? 0D : scheduleVo.getCxClass1Plan());  //对应成型一班的计划量
        Double cxClass2Plan = (scheduleVo.getCxClass2Plan() == null ? 0D : scheduleVo.getCxClass2Plan());  //对应成型二班的计划量
        Double cxClass3Plan = (scheduleVo.getCxClass3Plan() == null ? 0D : scheduleVo.getCxClass3Plan());  //对应成型三班的计划量
        Double cxClass4Plan = (scheduleVo.getCxClass4Plan() == null ? 0D : scheduleVo.getCxClass4Plan());  //对应成型次日一班的计划量
        Double cxClass5Plan = (scheduleVo.getCxClass5Plan() == null ? 0D : scheduleVo.getCxClass5Plan());  //对应成型次日一班的计划量
        autoScheduleLogService.insertGsqScheduleLog(scheduleVo.getBatchNo(), scheduleVo.getOrderNo(), "计算库存供应时长前数据",
                logSplit("具体算法：从1班开始判断，预计库存-1班的计划大于等于0时，供应时长+8小时；预计库存-1班计划-2班计划大于等于0时，供应时长+16小时；预计库存-1班计划-2班计划-3班计划小于0，供应时长=16个小时+（((预计库存-1班计划-2班计划)/3班计划)*8）；以此类推到第5班",
                        "物料编号：" + scheduleVo.getSteelRingCode() + "，16点预计库存：" + stockQty + "，对应成型一班的计划量：" + 0 + "，对应成型二班的计划量：" + cxClass2Plan + "，对应成型三班的计划量：" + cxClass3Plan + "，对应成型次日一班的计划量：" + cxClass4Plan + "，对应成型次日二班的计划量：" + cxClass5Plan));

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
        autoScheduleLogService.insertGsqScheduleLog(scheduleVo.getBatchNo(), scheduleVo.getOrderNo(), "计算库存供应时长结束","物料编号：" + scheduleVo.getSteelRingCode() + "，库存供应时长=" + scheduleVo.getSupplyTime());
    }

    /**
     * 根据对应成型每班计划量，计算库存供应时长
     * @param scheduleVo  排程实体
     * @param remnantStock 剩余库存
     * @param classPlan 对应成型的计划量
     * @return false：不需要再根据其他班在计算了。 true：还需要根据其他班计划量，继续计算库存供应时长
     */
    private boolean oneComputeSupplyTime(GsqScheduleResultVo scheduleVo,Double remnantStock, Double classPlan) {
        Double supplyTime = scheduleVo.getSupplyTime();
        supplyTime = (supplyTime == null ? 0D : supplyTime);
        if(BigDecimalUtil.sub(remnantStock, classPlan) >= 0) {
            //如果剩余库存 大于 对应班次库存，则库存供应时长直接+8小时
            scheduleVo.setSupplyTime(BigDecimalUtil.add(supplyTime, 12));  //库存供应时长加12小时
            return true;
        } else {
            //如果剩余库存 小宇 对应班次库存，则库存供应时长在加上：((剩余库存)/对应班班计划)*12小时
            double classSupplyTime = BigDecimalUtil.mul(BigDecimalUtil.div(remnantStock, classPlan), 12);
            supplyTime = supplyTime + BigDecimalUtil.roundDown(classSupplyTime, 1);  //设置库存供应时长向下保留1位小数
            scheduleVo.setSupplyTime(supplyTime);
            autoScheduleLogService.insertGsqScheduleLog(scheduleVo.getBatchNo(), scheduleVo.getOrderNo(), "计算库存供应时长结束","物料编号：" + scheduleVo.getSteelRingCode() + "，库存供应时长=" + scheduleVo.getSupplyTime());
            return false;
        }
    }

    /**
     * 计算并设置库存供应时长（小时）= 库存/(成型定额*单耗)*8小时
     * @param quotaParamMap 成型机台code和胎胚代码集合，格式：成型机台code$胎胚代码
     * @param stockQty 16点预计库存
     */
    private void computeSupplyTime(GsqScheduleResultVo scheduleVo, Map<String, String> quotaParamMap, Double stockQty) {
        String quotaKeys = quotaParamMap.get(scheduleVo.getSteelRingCode());  //获得钢丝圈对应的成型的机台code和胎胚code
        if(StringUtils.isBlank(quotaKeys)) {
            scheduleVo.setSupplyTime(0D);
            scheduleVo.setMidSysAnalysis("当天成型排程不需要此规格的钢丝圈所以’库存供应时长‘为0");
            scheduleVo.setNightSysAnalysis("当天成型排程不需要此规格的钢丝圈所以’库存供应时长‘为0");
            scheduleVo.setDaySysAnalysis("当天成型排程不需要此规格的钢丝圈所以’库存供应时长‘为0");
            scheduleVo.setNotOrderTag(0);   //生产顺序字段 不参与排序
            autoScheduleLogService.insertGsqScheduleLog(scheduleVo.getBatchNo(), scheduleVo.getOrderNo(), "计算库存供应时长", "当天成型排程不需要此规格的钢丝圈所以’库存供应时长‘为0");
            return;
        }
        String [] quotaKeyArray = quotaKeys.split(",");
        Integer cxQuota = cxEngineQuotaCommonService.getCxMachineQuota(quotaKeyArray);  //成型定额
        Double quota = cxQuota * 2D;   //钢丝圈定额(1条胎=2个钢丝圈)
        if(quota == 0) {
            scheduleVo.setSupplyTime(0D);
        } else {
            Double supplyTime = stockQty / quota * 8;  //库存可供成型连续生产的时长
            supplyTime = BigDecimalUtil.add(supplyTime, addComputeSupplyTime(scheduleVo)); //如果成型一班计划量为0，那么库存供应时长增加8小时；如果成型一班、二班计划量为0，那么供应时长增加16小时；以此类推
            scheduleVo.setSupplyTime(BigDecimalUtil.roundDown(supplyTime, 1)); //设置困存公用时长向下保留2位小数
        }
        autoScheduleLogService.insertGsqScheduleLog(scheduleVo.getBatchNo(), scheduleVo.getOrderNo(), "计算库存供应时长",
                logSplit("库存供应时长supplyTime（小时）= 库存/(成型定额*单耗)*8小时;其中成型定额取成型定额的平均值，单耗也是取平均单耗", "成型定额：" + cxQuota + "，半制品平均单耗(1条胎需要消耗2个钢丝圈)：2" ,
                        "计算后的结果数据：" + toJSONString(scheduleVo)));
    }

    /**
     * 如果成型一班计划量为0，那么库存供应时长增加8小时；如果成型一班、二班计划量为0，那么供应时长增加16小时；以此类推
     * @param scheduleVo
     * @return
     */
    private int addComputeSupplyTime(GsqScheduleResultVo scheduleVo) {
        int count = 0;
        int addTime = 8;  //每班8小时
        if(scheduleVo.getCxClass1Plan() == null || scheduleVo.getCxClass1Plan() == 0) {
            count++;
        } else {
            return count * addTime;
        }

        if(scheduleVo.getCxClass2Plan() == null || scheduleVo.getCxClass2Plan() == 0) {
            count++;
        } else {
            return count * addTime;
        }

        if(scheduleVo.getCxClass3Plan() == null || scheduleVo.getCxClass3Plan() == 0) {
            count++;
        } else {
            return count * addTime;
        }

        if(scheduleVo.getCxClass4Plan() == null || scheduleVo.getCxClass4Plan() == 0) {
            count++;
        } else {
            return count * addTime;
        }

        if(scheduleVo.getCxClass5Plan() == null || scheduleVo.getCxClass5Plan() == 0) {
            count++;
        }
        return count * addTime;
    }

    /**
     * 计算钢丝圈中班和夜班计划量
     * @param scheduleVo
     * @param totalPlanQtyVo 计划量总计VO
     * @param lossMap 耗损率map
     * @param paramLossRate 工序参数中配置的耗损率
     * @param mergeThreshold 往前一班合并计划量阈值
     */
    private void computeGsqPlanQty(GsqScheduleResultVo scheduleVo, GsqTotalPlanQtyVo totalPlanQtyVo, Map<String, Double> lossMap, GsqScheduleParams params) {
        double paramLossRate = params.getLossRate();
        double mergeThreshold = params.getMergeThreshold();
        BigDecimal toolCapacity = BigDecimalUtils.valueOf(params.getToolCapacity());
        BigDecimal equalShareThreshold = BigDecimalUtils.valueOf(params.getEqualShareThreshold());
        boolean isCloseOutSpec = ApsConstant.STATUS_ENABLE.equals(scheduleVo.getCloseOutSpecFlag()); // 是否收尾规格
        String oldScheduleResult = toJSONString(scheduleVo); //没看是计算前的排程数据json字符串（日志使用）
        Double stockQty = scheduleVo.getStockQty(); //库存
        double lastMidPlanQty = scheduleVo.getLastMidPlanQty(); // 前日早班计划
        double lastMidTqPlanQty = scheduleVo.getLastMidTqPlanQty(); // 前日胎圈早班计划
        double lastNightTqPlanQty = scheduleVo.getLastNightTqPlanQty(); // 前日胎圈夜班计划量
        boolean isMidSpec = Arrays.stream(params.getMidSpec()).anyMatch(scheduleVo.getSteelRingCode()::equals); // 是否强制排在早班
        boolean isNightSpec = Arrays.stream(params.getNightSpec()).anyMatch(scheduleVo.getSteelRingCode()::equals); // 是否强制排在夜班
        if (isMidSpec == true && isMidSpec == isNightSpec) { // 强制早报同时也强制夜班，属于设置有问题，等同于都不强制
            isMidSpec = false;
            isNightSpec = false;
        }
        //夜班计划量
        double midPlanQty = scheduleVo.getMidPlanQty();
        double initMidPlanQty = midPlanQty; // 胎圈早班计划
        //早班计划量
        double nightPlanQty = scheduleVo.getNightPlanQty();
        double initNightPlanQty = nightPlanQty; // 胎圈次日夜班计划
        double supplyClass = params.getProductStockDay(); // 预生产库存天数
        double dayPlanQty = 0D;
        double totalConsumeQty = BigDecimalUtil.add(lastMidTqPlanQty, lastNightTqPlanQty, initMidPlanQty, initNightPlanQty); // 总需求量，两天四个班的总计划量

        // 计算交接班库存 = 库存 + 上一天钢丝圈计划量总量 - 上一天胎圈两个班的消耗量
        // 交接班库存要按生产几个小时库存算，例如预生产12小时库存，则交接班库存要 > 当天钢丝圈需求量 / 2，最多超过一车（110个）
        double tqPlanQty1 = BigDecimalUtil.add(lastMidTqPlanQty, lastNightTqPlanQty);// 第一天两个班消耗量
        double tqPlanQty2 = BigDecimalUtil.add(initMidPlanQty, initNightPlanQty);// 第二天两个班消耗量
        double tqPlanQty3 = tqPlanQty2;// 第三天两个班消耗量（预估，与第二天一样）
        double classStock1 = stockQty; // 第一天交接班库存，初始为当天库存
        double classStock2 = BigDecimalUtil.roundDown(BigDecimalUtil.mul(tqPlanQty2, supplyClass), 0);
        if (tqPlanQty2 > params.getLargeDemand()) { // 如果是大需求量规格，需要控制一半 + 一车的库存
            double newClassStock2 = BigDecimalUtil.add(BigDecimalUtil.div(tqPlanQty2, 2, 0), toolCapacity.doubleValue());
            classStock2 = classStock2 > newClassStock2? newClassStock2: classStock2; // 计算后的新库存更大，则保留原库存
        }
        // 计算第一天相关数值
        double gsqPlanQty1 = BigDecimalUtil.add(BigDecimalUtil.sub(classStock2, classStock1), tqPlanQty1);// 第一天计划量 = 第二天交接班库存 - 第一天交接班库存 + 第一天胎圈两个班的消耗量
        gsqPlanQty1 = gsqPlanQty1 > 0? gsqPlanQty1: 0D; // 上一天交接班库存过多会计算成负数，需要处理成0
        double gsqClass1PlanQty1 = lastMidPlanQty;// 第一天早班计划 = 前日早班计划
        double gsqClass2PlanQty1 = BigDecimalUtil.sub(gsqPlanQty1, gsqClass1PlanQty1);// 第一天夜班计划 = 等于第一天胎圈计划 - 第一天早班计划
        double day1LackPlanStock = BigDecimalUtil.sub(tqPlanQty1, classStock1); // 第一天交接班库存不足的量
        if (isMidSpec && day1LackPlanStock <= 0) { // 强制排在早班的规格夜班不排（库存不足时还是要补量）
            gsqClass2PlanQty1 = 0D;
        } else if (isNightSpec) { // 强制排在夜班的规格，要备足明天早班的需求量
            // 早班库存 + 早班计划 + 夜班计划 - 第一天需求量 = 第二天早班可用的量（即第二天早班需求量）
            // => 夜班计划 = 第一天需求量 + 第二天早班需求量 - （早班库存 + 早班计划）
            double newGsqClass2PlanQty1 = BigDecimalUtil.sub(BigDecimalUtil.add(tqPlanQty1, initMidPlanQty), BigDecimalUtil.add(classStock1, gsqClass1PlanQty1));
            gsqClass2PlanQty1 = BigDecimalUtils.greatest(newGsqClass2PlanQty1, newGsqClass2PlanQty1).doubleValue(); // 取两者较大值
        } else if (day1LackPlanStock <= 0 && gsqClass2PlanQty1 <= toolCapacity.doubleValue()) { // 如果第一天交接班库存充足，第二天交接班库存缺不足一个工装，则先不生产，减少换工装
            gsqClass2PlanQty1 = 0D;
        }
        gsqClass2PlanQty1 = this.planQtyRounding(scheduleVo, gsqClass2PlanQty1, toolCapacity, totalConsumeQty, isCloseOutSpec,
                OpenMachineClassEnums.CLASS_TWO); // 整车取整
        midPlanQty = gsqClass2PlanQty1; // 夜班计划
        scheduleVo.setMidPlanQty(midPlanQty);
        // 根据排好的计划量重算相关数值
        gsqPlanQty1 = BigDecimalUtil.add(gsqClass1PlanQty1, midPlanQty); // 刷新第一天胎圈计划量
        classStock2 = BigDecimalUtil.sub(BigDecimalUtil.add(gsqPlanQty1, classStock1), tqPlanQty1);// 刷新第二天交接班库存
        scheduleVo.setClassStock(classStock2); // 保存交接班库存，用于均衡计算
        scheduleVo.setSupplyDemandRatio(BigDecimalUtil.div(classStock2, tqPlanQty2, 4)); // 计算交接班库存供需比率，第二天交接班库存 / 胎圈第二天需求量，用于均衡计算
        scheduleVo.setTqClass1PlanQty(initMidPlanQty);
        scheduleVo.setTqClass2PlanQty(initNightPlanQty);

        // 计算第二天相关数值
        double classStock3 = BigDecimalUtil.roundDown(BigDecimalUtil.mul(tqPlanQty3, supplyClass), 0); // 第三天交接班库存，第三天胎圈两个班的消耗量 * 预生产天数
        if (tqPlanQty3 >= params.getLargeDemand()) { // 如果是大需求量规格，需要控制一半 + 一车的库存
            double newClassStock3 = BigDecimalUtil.add(BigDecimalUtil.div(tqPlanQty3, 2, 0), toolCapacity.doubleValue());
            classStock3 = classStock3 > newClassStock3? newClassStock3: classStock3; // 计算后的新库存更大，则保留原库存
        }
        double gsqPlanQty2 = BigDecimalUtil.add(BigDecimalUtil.sub(classStock3, classStock2), tqPlanQty2);// 第二天钢丝圈计划量 = 第三天交接班库存 - 第二天交接班库存 + 第二天胎圈两个班的消耗量
        gsqPlanQty2 = gsqPlanQty2 > 0? gsqPlanQty2: 0D; // 上一天交接班库存过多会计算成负数，需要处理成0
        double lackPlanQty = BigDecimalUtil.sub(tqPlanQty2, classStock2); // 第二天交接班库存缺口
        double class3lackPlanQty = BigDecimalUtil.sub(initMidPlanQty, classStock2); // 第二天早班库存缺口
        double gsqClass1PlanQty2 = lackPlanQty; // 第二天早班计划，默认为当天库存缺口
        if (isMidSpec) { // 强制排在早班的规格全部排在早班
            gsqClass1PlanQty2 = gsqPlanQty2;
        } else if (isNightSpec) { // 强制夜班的规格，早班不排计划
            gsqClass1PlanQty2 = 0D;
        } else if (this.isBigSizeSpec(scheduleVo) && lackPlanQty < BigDecimalUtils.half(tqPlanQty2).doubleValue()) {
            // 如果是大尺寸规格，且库存缺口低于第二天需求量的一半，则需求量的一半
            gsqClass1PlanQty2 = BigDecimalUtils.half(tqPlanQty2).doubleValue(); // 取交接班库存与第二天需求量一般的较大值
        } else if (tqPlanQty3 >= params.getLargeDemand()) { // 如果计划量是大需求量，则直接早夜班计划量对半分
            gsqClass1PlanQty2 = BigDecimalUtils.half(tqPlanQty3).doubleValue();
        } else if (class3lackPlanQty <= 0 && lackPlanQty <= toolCapacity.doubleValue()) { // 如果早班交接班库存充足，晚班的库存缺口不足一个工装，则先不生产，减少换工装
            gsqClass1PlanQty2 = 0D;
        }
        if (gsqClass1PlanQty2 > 0 && gsqClass1PlanQty2 < gsqPlanQty2 && (gsqClass1PlanQty2 <= toolCapacity.doubleValue()
                || BigDecimalUtil.sub(gsqPlanQty2, gsqClass1PlanQty2) <= toolCapacity.doubleValue())) {
            gsqClass1PlanQty2 = tqPlanQty2; // 如果中班夜班都有计划量，且有任意一个班计划量不足一个工装，则合并到早班完成
        }
        nightPlanQty = this.planQtyRounding(scheduleVo, gsqClass1PlanQty2, toolCapacity, totalConsumeQty, isCloseOutSpec,
                OpenMachineClassEnums.CLASS_THREE); // 早班计划 = 第二天早班计划整车取整
        scheduleVo.setNightPlanQty(nightPlanQty);
        double gsqClass2PlanQty2 = BigDecimalUtil.sub(gsqPlanQty2, gsqClass1PlanQty2);// 第二天夜班计划 = 等于第二天胎圈计划 - 第二天早班计划
        if (isMidSpec) { // 强制排在早班的规格全部排在早班
            gsqClass2PlanQty2 = 0D;
        } else if (BigDecimalUtil.sub(tqPlanQty2, BigDecimalUtil.add(classStock2, gsqClass1PlanQty2)) <= 0
                && gsqClass2PlanQty2 <= toolCapacity.doubleValue()) {
            // 如果库存 + 早班计划已满足一天需求量，且隔天的库存缺口不足一个工装，则先不做，减少规格切换次数
            gsqClass2PlanQty2 = 0D;
        }
        dayPlanQty = this.planQtyRounding(scheduleVo, gsqClass2PlanQty2, toolCapacity, totalConsumeQty, isCloseOutSpec,
                OpenMachineClassEnums.CLASS_FOUR); // 次日夜班计划 = 第二天夜班计划整车取整
        scheduleVo.setDayPlanQty(dayPlanQty);

        String machineId = scheduleVo.getMachineId();  //机台id
        double lossRate = 0;
        //只有单个机台的时候，自动排程才计算耗损率
        if(StringUtils.isNotBlank(machineId) && !machineId.contains(",")) {
            //计划量要加上耗损量
            lossRate = gsqEngineLossService.getLossRate(scheduleVo.getSteelRingCode(), scheduleVo.getMachineId(), lossMap, paramLossRate);
            midPlanQty = BigDecimalUtil.add(midPlanQty, BigDecimalUtil.mul(midPlanQty, lossRate));
            nightPlanQty = BigDecimalUtil.add(nightPlanQty, BigDecimalUtil.mul(nightPlanQty, lossRate));
            dayPlanQty = BigDecimalUtil.add(dayPlanQty, BigDecimalUtil.mul(dayPlanQty, lossRate));
        }

        scheduleVo.setMidPlanQty(BigDecimalUtil.roundUp(midPlanQty,0));
        scheduleVo.setNightPlanQty(BigDecimalUtil.roundUp(nightPlanQty,0));
        scheduleVo.setDayPlanQty(BigDecimalUtil.roundUp(dayPlanQty,0));

        //计算各班总计划量
        totalPlanQtyVo.setTotalMidPlanQty(BigDecimalUtil.add(totalPlanQtyVo.getTotalMidPlanQty(), midPlanQty));
        totalPlanQtyVo.setTotalNightPlanQty(BigDecimalUtil.add(totalPlanQtyVo.getTotalNightPlanQty(), nightPlanQty));
        totalPlanQtyVo.setTotalDayPlanQty(BigDecimalUtil.add(totalPlanQtyVo.getTotalDayPlanQty(), dayPlanQty));
        totalPlanQtyVo.setTotalPlanQty(BigDecimalUtil.add(totalPlanQtyVo.getTotalMidPlanQty(), totalPlanQtyVo.getTotalNightPlanQty(), totalPlanQtyVo.getTotalDayPlanQty()));
        this.computeGsqPlanQtyLog(oldScheduleResult, scheduleVo, lossMap, paramLossRate, lossRate, mergeThreshold);  //添加日志
    }

    /**
     * 计划量取整
     * @param scheduleVo        排产记录
     * @param planQty           原计划量
     * @param toolCapacity      一个胎圈车可以放的胎圈量
     * @param totalConsumeQty   总需期间量，用于判断收尾规格是否超量
     * @param isCloseOutSpec    是否收尾
     * @param classNum          当前班次，从前日早班开始
     * @return
     */
    private double planQtyRounding(GsqScheduleResultVo scheduleVo, double planQty, BigDecimal toolCapacity,
            Double totalConsumeQty, boolean isCloseOutSpec, OpenMachineClassEnums classNum) {
        if (planQty <= 0D) { // 不排的情况直接返回0即可
            return 0D;
        }
        double roudingPlanQty = BigDecimalUtils.valueOf(planQty).divide(toolCapacity, 0, RoundingMode.CEILING)
                .multiply(toolCapacity).doubleValue(); // 取整车
        if (isCloseOutSpec) { // 如果是收尾规格，需要判断已排计划不允许超过总需求量
            if (classNum == null) {
                return roudingPlanQty;
            }
            OpenMachineClassEnums lastClass = classNum;
            if (classNum != OpenMachineClassEnums.CLASS_ONE) { // 取出上一班的班次
                Integer classIndex = classNum.getClassIndex();
                lastClass = OpenMachineClassEnums.getClassEnums(classIndex - 1);
            }
            double lastPlanCumulative = this.getGsqClassPlanCumulative(scheduleVo, lastClass); // 到上个班次班次班的累计已排计划量
            double newPlanQty = BigDecimalUtil.add(lastPlanCumulative, roudingPlanQty, scheduleVo.getStockQty()); // 库存+已排计划+本班计划
            // 如果库存+计划已经超过总需求量，则本班的计划量要限制住不允许超量
            if (newPlanQty > totalConsumeQty) {
                Double increaseMidPlanQty = BigDecimalUtil.sub(newPlanQty, totalConsumeQty);
                Double result = BigDecimalUtil.sub(roudingPlanQty, increaseMidPlanQty);
                return result > 0? result: 0D;
            }
        }
        return roudingPlanQty;
    }

    /**
     * 获取各班计划量的累计值（从前日早班开始）
     * @param scheduleVo
     * @param classNum
     * @return
     */
    private Double getGsqClassPlanCumulative(GsqScheduleResultVo scheduleVo, OpenMachineClassEnums classNum) {
        Double planQty = 0D;
        if (classNum == null) {
            return planQty;
        }
        planQty = BigDecimalUtil.add(planQty, scheduleVo.getLastMidPlanQty());
        if (classNum == OpenMachineClassEnums.CLASS_ONE) {
            return planQty;
        }
        planQty = BigDecimalUtil.add(planQty, scheduleVo.getMidPlanQty());
        if (classNum == OpenMachineClassEnums.CLASS_TWO) {
            return planQty;
        }
        return BigDecimalUtil.add(planQty, scheduleVo.getNightPlanQty());
    }

    private void computeGsqPlanQtyLog(String oldScheduleResult, GsqScheduleResultVo scheduleVo, Map<String, Double> lossMap, double paramLossRate, double lossRate, double mergeThreshold) {
        StringBuffer logDetail = new StringBuffer("");
        logDetail.append("开始计算中班和夜班计划量").append(division);
        logDetail.append("计算前排程数据：" + oldScheduleResult).append(division);
        logDetail.append("在sql中计算各班的计划量。钢丝圈中班计划量=胎圈中班+夜班；钢丝圈夜班计划量=胎圈白天计划量；钢丝圈白天计划量=胎圈次日中班计划量").append(division);
        logDetail.append("根据库存重新计算中班计划量midPlanQty：根据库存重新计算中班计划量midPlanQty：（原中班计划量midPlanQty>库存stockQty） ？（ 原中班计划量-库存）：0").append(division);
        logDetail.append("根据库存重新计算夜班计划量nightPlanQty：（原中班计划量midPlanQty>库存stockQty） ？原夜班计划量nightPlanQty ： （原中班计划量midPlanQty + 原夜班计划量nightPlanQty - 库存stockQty）").append(division);
        logDetail.append("根据库存重新计算白班计划量dayPlanQty：（原中班计划量+夜班计划量 > 库存） ？原白班计划量 ： （原中班计划量+原夜班计划量+原白班计划量 - 库存）").append(division);
        logDetail.append("钢丝圈耗损率集合：" + toJSONString(lossMap) + "  参数配置耗损率：" + paramLossRate).append(division);
        logDetail.append("获得损耗率（从损耗率表获取对应的损耗率，获取顺序：机台+物料编号 > 钢丝圈代码 > 机台 >工序参数配置），耗损率：" + lossRate).append(division);
        logDetail.append("重新计算中班计划量和夜班计划量(计划量 = 计划量 + 计划量 * 耗损率)，计划量要加上耗损率的损耗数").append(division);
//        logDetail.append("如果中班计划量>0，那么中班计划量=中班计划量+夜班计划量，夜班计划量=0（为了让相同的胶在同一个班生产，而且又不能延误生产）,往前一班合并计划量阈值参数值：" + mergeThreshold).append(division);
        logDetail.append("计划量计算好后的排程数据：" + toJSONString(scheduleVo));
        autoScheduleLogService.insertGsqScheduleLog(scheduleVo.getBatchNo(), scheduleVo.getOrderNo(), "计算各班计划量", logDetail.toString());
    }

    /**
     * 创建批次号
     * @param scheduleDate
     * @return
     */
    private String createBatchNo(String scheduleDate) {
        scheduleDate = scheduleDate.replace("-", "");
        return incrementService.getSequence3(EngineConstants.GSQ_BATCH_NO_PREFIX + scheduleDate);
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
    private GsqScheduleParams loadParams() {
        List<GsqParamsVo> list = this.gsqEngineMapper.listGsqParams();
        Map<String, String> paramsMap = list.stream()
                .collect(Collectors.toMap(GsqParamsVo::getParamCode, GsqParamsVo::getParamValue));

        GsqScheduleParams params = new GsqScheduleParams();

        params.setProductionStage(paramsMap.get(EngineConstants.PRODUCTION_STAGE_PRODUCE));
        params.setLossRate(getDouble(paramsMap.get(EngineConstants.LOSS_RATE)));
        params.setMergeThreshold(getDouble(paramsMap.get(EngineConstants.MERGE_PLAN_THRESHOLD)));
        params.setCloseOutNum(getDouble(paramsMap.get(EngineConstants.CLOSE_OUT_NUM)));
        params.setToolCapacity(getDouble(paramsMap.getOrDefault(EngineConstants.TOOL_CAPACITY, DEFAULT_TOOL_CAPACITY)));
        BigDecimal productStockHour = new BigDecimal(paramsMap.getOrDefault(EngineConstants.PRODUCT_STOCK_HOUR, DEFAULT_PRODUCT_STOCK_HOUR));
        params.setProductStockDay(productStockHour.divide(HOUR24, 2, RoundingMode.HALF_UP).doubleValue()); // 小时换算成天数
        params.setLargeDemand(getDouble(paramsMap.getOrDefault(EngineConstants.LARGE_DEMAND, DEFAULT_LARGE_DEMAND)));
        params.setBigSizeSpec(BigDecimalUtils.valueOf(paramsMap.getOrDefault(EngineConstants.BIG_SIZE_SPEC, DEFAULT_BIG_SIZE_SPEC)));
        params.setStockLossRate(getDouble(paramsMap.getOrDefault(EngineConstants.STOCK_LOSS_RATE, "0")));
        params.setStockRatio(BigDecimalUtils.valueOf(paramsMap.getOrDefault(EngineConstants.STOCK_RATIO, "1")));
        params.setMidSpec(paramsMap.getOrDefault(EngineConstants.MID_SPEC, "").split(","));
        params.setNightSpec(paramsMap.getOrDefault(EngineConstants.NIGHT_SPEC, "").split(","));
        params.setSupplyTime(getDoubleOrDefault(paramsMap.get(EngineConstants.SUPPLY_TIME_PASS), 12D));
        params.setEqualShareThreshold(new BigDecimal(paramsMap.getOrDefault(EngineConstants.EQUAL_SHARE_THRESHOLD, DEFAULT_EQUAL_SHARE_THRESHOLD))); // 平分阈值

        return params;
    }

    /**
     * 获取钢丝圈对应的成型胎胚code和机台code
     * @param scheduleDate 排程日期，格式：yyyy-MM-dd
     * @return
     */
    private Map<String, String> getQuotaParamMap(String scheduleDate, String productionStage) {
        List<GsqQuotaParam> list = gsqEngineMapper.listQuotaParam(scheduleDate, productionStage);
        Map<String, String> map = list.stream().collect(Collectors.toMap(GsqQuotaParam::getSteelRingCode, GsqQuotaParam::getQuotaKeys));
        return map == null ? new HashMap<>() : map;
    }

    /**
     * 自动排程基础表的数据日志
     * @param batchNo 自动排程批次号
     * @param twiningDiscMachineMap 缠绕盘和机台关系集合（key = 规格尺寸~排列方式）
     * @param twiningDiscMap 获得钢丝圈代码和缠绕盘集合（value = 规格尺寸~排列方式）map
     * @param specifyCanMachineMap 定点机台和机台的限制作业集合
     * @param specifyNotMachineMap 定点集合和机台的不可作业集合
     * @param planStockMap 16点预计库存集合
     * @param lossRateMap 耗损率集合
     * @param monthSurplus 月度计划剩余量、完成量集合
     * @param paramsMap 参数设置集合
     */
    private void baseDataLog(String batchNo, Map<String, String> twiningDiscMachineMap, Map<String, String> twiningDiscMap, Map<String, String> specifyCanMachineMap,
                             Map<String, String> specifyNotMachineMap, Map<String, Double> planStockMap, Map<String, Double> lossRateMap,
                             Map<String, GsqMonthSurplusVo> monthSurplus, GsqScheduleParams params) {
        StringBuffer logDetail = new StringBuffer("");
        logDetail.append("缠绕盘和机台关系集合（key=规格尺寸~排列方式）：" + toJSONString(twiningDiscMachineMap)).append(division);
        logDetail.append("钢丝圈代码和缠绕盘计划（value=规格尺寸~排列方式）：" + toJSONString(twiningDiscMap)).append(division);
        logDetail.append("定点机台和机台的限制作业集合：" + toJSONString(specifyCanMachineMap)).append(division);
        logDetail.append("定点集合和机台的不可作业集合：" + toJSONString(specifyNotMachineMap)).append(division);
        logDetail.append("16点预计库存集合：" + toJSONString(planStockMap)).append(division);
        logDetail.append("耗损率集合：" + toJSONString(lossRateMap)).append(division);
        logDetail.append("月度计划剩余量、完成量集合：" + toJSONString(monthSurplus)).append(division);
        logDetail.append("参数设置集合：" + toJSONString(params)).append(division);
        autoScheduleLogService.insertGsqScheduleLog(batchNo, "", "自动排程基础表的数据日志", logDetail.toString());
    }

    @Override
    public void batchUpdateBatchNoAndOrderNo(String scheduleDate) {
        List<GsqScheduleResultVo> scheduleResultVoList = gsqEngineMapper.listGsqEnginSchedule(scheduleDate);
        //查询当前排程的批次号
        String batchNo = gsqEngineMapper.getGsqCurrentBatchNo(scheduleDate);
        if (StringUtils.isBlank(batchNo)) {
            //当前的批次号为空，说明还没”自动排程“或者做的批量导入（需要删掉已排的数据），那么自己生成一个排程批次号
            //排程批次号
            batchNo = this.createBatchNo(scheduleDate);
            //创建自动排程记录
            this.createScheduleRecord(scheduleDate, "", batchNo);
        }
        for (GsqScheduleResultVo scheduleResult : scheduleResultVoList) {
            //批次号
            scheduleResult.setBatchNo(batchNo);
            //工单号
            String orderNo = this.createOrderNo(batchNo);
            scheduleResult.setOrderNo(orderNo);
        }

        if (CollectionUtils.isNotEmpty(scheduleResultVoList)) {
            gsqEngineMapper.batchUpdateBatchNoAndOrderNo(scheduleResultVoList);
        }
    }
}
