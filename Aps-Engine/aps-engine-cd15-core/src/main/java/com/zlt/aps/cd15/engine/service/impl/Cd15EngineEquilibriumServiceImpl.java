package com.zlt.aps.cd15.engine.service.impl;

import static com.alibaba.fastjson.JSON.toJSONString;
import static com.zlt.aps.common.core.utils.ApsCommonUtil.getDoubleOrDefault;
import static com.zlt.aps.common.core.utils.ApsCommonUtil.logSplit;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.springframework.stereotype.Service;

import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.cd15.engine.service.Cd15EngineEquilibriumService;
import com.zlt.aps.cd15.engine.vo.Cd15EquilibriumVo;
import com.zlt.aps.cd15.engine.vo.Cd15ScheduleResultVo;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.core.utils.BigDecimalUtil;
import com.zlt.aps.common.core.utils.BigDecimalUtils;
import com.zlt.aps.common.engine.constants.EngineConstants;
import com.zlt.aps.common.engine.service.AutoScheduleLogService;
import com.zlt.aps.common.engine.utils.CollectionUtil;

/**
 * 15度裁断排产均衡服务接口
 * 
 * @Description
 * @Author hakimrayn
 * @Date 2021-7-9 15:26:28
 * @Version 1.0
 */
@Service("cd15EngineEquilibriumService")
public class Cd15EngineEquilibriumServiceImpl implements Cd15EngineEquilibriumService {
	private static final String DEFAULT_PLAN_DIFFERENCE_RATE = "15"; // 默认中班总量和夜班总量差额百分比：15%
	private static final String DEFAULT_SUPPLY_TIME_PASS = "12"; // 默认库存供应时长小时数：12小时
    private static final String DEFAULT_EQUAL_SHARE_THRESHOLD = "500"; // 需求量超过该值早夜班对半分
    private final static String DEFAULT_ONE_ROLL_NUM = "2"; // 一次生产卷数默认值
    private final static String DEFAULT_CRIMP_LENGTH = "190"; // 卷曲长度默认值：190
    
	@Resource
	private AutoScheduleLogService autoScheduleLogService;

	/**
	 * 均衡处理 检测中班与晚班的差异率是否超过最大差异率，超过则尽量尝试将多的一部分产量转移（中班转移到晚班，或者晚班转移到中班）
	 * 
	 * @Author hakimryan
	 * @Description
	 * @Date 2021-7-9 15:27:55
	 * @Param scheduleList 15度裁断排产结果
     * @Param paramsMap 系统参数
	 * @Return
	 */
	@Override
	public void scheduleEquilibrium(List<Cd15ScheduleResultVo> scheduleList, Map<String, String> paramsMap) {
		// 系统参数类型转换
		BigDecimal planDifferenceRateNum = BigDecimalUtils.valueOf(paramsMap.getOrDefault(EngineConstants.PLAN_DIFFERENCE_RATE, DEFAULT_PLAN_DIFFERENCE_RATE));
		// 百分数转成小数
		planDifferenceRateNum = planDifferenceRateNum.divide(BigDecimalUtils.HUNDRED);
		BigDecimal supplyTimePassNum = BigDecimalUtils.valueOf(paramsMap.getOrDefault(EngineConstants.SUPPLY_TIME_PASS, DEFAULT_SUPPLY_TIME_PASS));
        BigDecimal shareThreshold = BigDecimalUtils.valueOf(paramsMap.getOrDefault(EngineConstants.EQUAL_SHARE_THRESHOLD, DEFAULT_EQUAL_SHARE_THRESHOLD));
        BigDecimal oneRollNum = new BigDecimal(paramsMap.getOrDefault(EngineConstants.ONE_ROLL_NUM, DEFAULT_ONE_ROLL_NUM)); // 一次生产卷数
        BigDecimal crimpLength = new BigDecimal(paramsMap.getOrDefault(EngineConstants.CRIMP_LENGTH, DEFAULT_CRIMP_LENGTH)); // 卷曲长度
        BigDecimal oneProductQty = oneRollNum.multiply(crimpLength); // 最低生产数 = 一次生产卷数 * 卷长
        
		// 均衡运算前的排产结果，用于日志记录
		String oldScheduleList = toJSONString(scheduleList);
		// 批次号
		String batchNo = CollectionUtil.firstElement(scheduleList).getBatchNo();

		this.equilibriumDay1(scheduleList, shareThreshold, oneProductQty);
        this.equilibriumDay2(scheduleList, shareThreshold, oneProductQty);
		
		// 均衡算法需要根据每个机台单独均衡 modify by 20220113
		Map<String, List<Cd15ScheduleResultVo>> scheduleMachineMap = scheduleList.stream()
				// 过滤掉无机台或者多机台的排产记录
				.filter(s -> StringUtils.isNotEmpty(s.getMachineId()) && !s.getMachineId().contains(","))
				// 按机台ID分组
				.collect(Collectors.groupingBy(Cd15ScheduleResultVo::getMachineId));
		for (List<Cd15ScheduleResultVo> groupingList : scheduleMachineMap.values()) {
			// 各机台单独均衡
			this.equilibriumSingleMachine(groupingList, planDifferenceRateNum, supplyTimePassNum);
		}
//		this.equalShare(scheduleList, equalShareThreshold);
		// 新增计算日志
		this.insertCalculateLog(batchNo, oldScheduleList, scheduleList, planDifferenceRateNum, supplyTimePassNum,
				this.createEquilibrimeVo(scheduleList));
	}
	
	/**
     * 均衡第一天夜班与第二天的计划
     *
     * @param scheduleList   排程列表
     * @param oneProductQty  均分阈值
     * @param oneProductQty  最低生产量
     */
    private void equilibriumDay1(List<Cd15ScheduleResultVo> scheduleList, BigDecimal bisectThreshold, BigDecimal oneProductQty) {
        double totalDayPlanQty = scheduleList.stream().mapToDouble(Cd15ScheduleResultVo::getDayPlanQty1).sum(); // 夜班总计划量
        double totalNightPlanQty = scheduleList.stream().mapToDouble(Cd15ScheduleResultVo::getNightPlanQty1).sum(); // 早班总计划量
        double totalNextDayPlanQty = scheduleList.stream().mapToDouble(Cd15ScheduleResultVo::getNextDayPlanQty).sum(); // 次日夜班总计划量
        double midPlanQtyReference = BigDecimalUtils.avg(0, RoundingMode.DOWN, totalDayPlanQty, totalNightPlanQty, totalNextDayPlanQty).doubleValue(); // 计划平均值
        double difNum = BigDecimalUtil.sub(totalDayPlanQty, midPlanQtyReference); // 早班和平均值的差值
        if (difNum == 0) {
            return;
        }
        boolean isNightClassPass = difNum > 0; // 夜班是否超量
        // 超量，需要从库存比较大的开始调整（倒序）；不足时需要从供需比例较小的开始调整（顺序）
        scheduleList = scheduleList.stream().sorted((r1, r2) -> {
            BigDecimal classStock1 = BigDecimalUtils.sub(r1.getClassStock(), r1.getCxClass3Plan());
            BigDecimal classStock2 = BigDecimalUtils.sub(r2.getClassStock(), r2.getCxClass3Plan());
            if (isNightClassPass) {
                // 夜班超量，将交接班库存较充足的转移到早班（倒序）
                return classStock2.compareTo(classStock1);
            } else {
                // 早班超量，将交接班库存较低的转移到夜班（顺序）
                return classStock1.compareTo(classStock2);
            }
        }).collect(Collectors.toList());
        for (Cd15ScheduleResultVo scheduleVo: scheduleList) {
            if (scheduleVo.getIsNightSpec()) { // 固定夜班规格，不处理
                continue;
            }
            if (ApsConstant.STATUS_ENABLE.equals(scheduleVo.getCloseOutSpecFlag())) { // 收尾规格不处理
                continue;
            }
            BigDecimal crimpLength = (BigDecimal)scheduleVo.getParams().get(EngineConstants.CRIMP_LENGTH); // 满工装长度
            // 成型需求量
            BigDecimal cxDay1PlanQty = BigDecimalUtils.add(scheduleVo.getCxClass1Plan(), scheduleVo.getCxClass2Plan());
            BigDecimal cxDay2PlanQty = BigDecimalUtils.add(cxDay1PlanQty, scheduleVo.getCxClass3Plan()); // 前3个成型的需求量
            // 1#钢带
            BigDecimal stockQty1 = BigDecimalUtils.valueOf(scheduleVo.getStock1Qty1());
            BigDecimal lastMidPlanQty1 = BigDecimalUtils.valueOf(scheduleVo.getLastMidPlanQty1());
            BigDecimal dayPlanQty1 = BigDecimalUtils.valueOf(scheduleVo.getDayPlanQty1());
            BigDecimal nightPlanQty1 = BigDecimalUtils.valueOf(scheduleVo.getNightPlanQty1());
            BigDecimal nextDayPlanQty1 = BigDecimalUtils.valueOf(scheduleVo.getNextDayPlanQty());
            // 2#钢带
            BigDecimal stockQty2 = BigDecimalUtils.valueOf(scheduleVo.getStock1Qty2());
            BigDecimal lastMidPlanQty2 = BigDecimalUtils.valueOf(scheduleVo.getLastMidPlanQty2());
            BigDecimal dayPlanQty2 = BigDecimalUtils.valueOf(scheduleVo.getDayPlanQty2());
            BigDecimal nightPlanQty2 = BigDecimalUtils.valueOf(scheduleVo.getNightPlanQty2());
            BigDecimal nextDayPlanQty2 = BigDecimalUtils.valueOf(scheduleVo.getNextDayPlanQty2());
            
            BigDecimal dayAddPlan1 = BigDecimal.ZERO; // 1#钢带夜班增加量
            BigDecimal dayAddPlan2 = BigDecimal.ZERO; // 2#钢带夜班增加量
            BigDecimal nightAddPlan1 = BigDecimal.ZERO; // 1#钢带早班增加量
            BigDecimal nightAddPlan2 = BigDecimal.ZERO; // 2#钢带早班增加量
            BigDecimal nextDayAddPlan1 = BigDecimal.ZERO; // 1#钢带次日夜班增加量
            BigDecimal nextDayAddPlan2 = BigDecimal.ZERO; // 2#钢带次日夜班增加量
            if (isNightClassPass) { // 夜班超量，则从夜班推迟到隔天早班
                BigDecimal day1LackStock1 = cxDay1PlanQty.subtract(stockQty1.add(lastMidPlanQty1));
                BigDecimal day1LackStock2 = cxDay1PlanQty.subtract(stockQty2.add(lastMidPlanQty2));
                // 当天库存不足的不能推迟
                if (day1LackStock1.compareTo(BigDecimal.ZERO) > 0 || day1LackStock2.compareTo(BigDecimal.ZERO) > 0) {
                    continue;
                }

                // 第二天交班库存不足，如果成型顺位为1也不能推迟
                BigDecimal day2LackStock1 = cxDay2PlanQty.subtract(stockQty1.add(lastMidPlanQty1));
                BigDecimal day2LackStock2 = cxDay2PlanQty.subtract(stockQty2.add(lastMidPlanQty2));
                if (scheduleVo.getClass3Sort() <= 1 && (day2LackStock1.compareTo(BigDecimal.ZERO) > 0 || day2LackStock2.compareTo(BigDecimal.ZERO) > 0)) {
                    continue;
                }
                nightAddPlan1 = BigDecimalUtils.floor(dayPlanQty1, crimpLength); // 1#钢带早班加量
                nightAddPlan2 = BigDecimalUtils.floor(dayPlanQty2, crimpLength); // 2#钢带早班加量
                dayAddPlan1 = nightAddPlan1.negate(); // 1#钢带夜班减量
                dayAddPlan2 = nightAddPlan2.negate(); // 2#钢带夜班减量
            } else if (nightPlanQty1.compareTo(BigDecimal.ZERO) > 0 || nightPlanQty2.compareTo(BigDecimal.ZERO) > 0) { // 隔天超量，且早班大于0，则从早班转移到夜班
                dayAddPlan1 = BigDecimalUtils.floor(nightAddPlan1, crimpLength);// 1#钢带夜班加量
                dayAddPlan2 = BigDecimalUtils.floor(nightAddPlan2, crimpLength);// 2#钢带夜班加量
                nightAddPlan1 = dayAddPlan1.negate(); // 1#钢带早班减量
                nightAddPlan2 = dayAddPlan2.negate(); // 2#钢带早班减量
            }
            if (dayAddPlan1.compareTo(BigDecimal.ZERO) == 0 && dayAddPlan2.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }
            // 先算一下是否调整后差异反而更大
            double newTotalDayPlanQty = BigDecimalUtils.add(totalDayPlanQty, dayAddPlan1).doubleValue();
            double newDifNum = BigDecimalUtil.sub(newTotalDayPlanQty, midPlanQtyReference); // 早班和平均值的差值
            if (Math.abs(newDifNum) > Math.abs(difNum)) { // 如果更大跳过该规格
                continue;
            }
            // 更新各班计划量
            scheduleVo.setDayPlanQty1(dayPlanQty1.add(dayAddPlan1).doubleValue());
            scheduleVo.setDayPlanQty2(dayPlanQty2.add(dayAddPlan2).doubleValue());
            scheduleVo.setNightPlanQty1(nightPlanQty1.add(nightAddPlan1).doubleValue());
            scheduleVo.setNightPlanQty2(nightPlanQty2.add(nightAddPlan2).doubleValue());
            scheduleVo.setNextDayPlanQty(nextDayPlanQty1.add(nextDayAddPlan1).doubleValue());
            scheduleVo.setNextDayPlanQty2(nextDayPlanQty2.add(nextDayAddPlan2).doubleValue());
            if (dayAddPlan1.compareTo(BigDecimal.ZERO) != 0) {
                scheduleVo.setClassStock(this.getClassStock(scheduleVo)); // 夜班计划有变动，需要重算交接班库存
            }
            totalDayPlanQty = newTotalDayPlanQty;
            totalNightPlanQty = BigDecimalUtils.add(totalNightPlanQty, nightAddPlan1).doubleValue();
            totalNextDayPlanQty = BigDecimalUtils.add(totalNextDayPlanQty, nextDayAddPlan1).doubleValue();
            difNum = newDifNum;
            if (isNightClassPass ^ difNum > 0) { // 如果计算前后差值符号相反则直接结束
                break;
            }
        }
    }
    
    /**
     * 计算交接班库存
     * @param scheduleVo
     * @return
     */
    private Double getClassStock(Cd15ScheduleResultVo scheduleVo) {
        BigDecimal planQty = BigDecimalUtils.add(scheduleVo.getStock1Qty1(), scheduleVo.getLastMidPlanQty1(), scheduleVo.getDayPlanQty1());
        BigDecimal cxPlanQty = BigDecimalUtils.add(scheduleVo.getCxClass1Plan(), scheduleVo.getCxClass2Plan());
        return planQty.subtract(cxPlanQty).doubleValue();
    }
	
	/**
     * 均衡第二天早夜班库存
     *
     * @param scheduleList    排程列表
     * @param bisectThreshold 均分阈值
     * @param oneProductQty   一次性最低生产量
     */
    private void equilibriumDay2(List<Cd15ScheduleResultVo> scheduleList, BigDecimal bisectThreshold, BigDecimal oneProductQty) {
        this.equalShare(scheduleList, bisectThreshold, oneProductQty); // 先均分中夜班计划量
        double totalNightPlanQty = scheduleList.stream().mapToDouble(Cd15ScheduleResultVo::getNightPlanQty1).sum(); // 早班总计划量
        double totalNextDayPlanQty = scheduleList.stream().mapToDouble(Cd15ScheduleResultVo::getNextDayPlanQty).sum(); // 次日夜班总计划量
        double difNum = BigDecimalUtil.sub(totalNextDayPlanQty, totalNightPlanQty); //早班和次日夜班的计划量差额
        if (difNum == 0) {
            return;
        }

        boolean isDayClassPass = (difNum < 0);  //true：早班超量，false：次日夜班超量
        if (isDayClassPass) {
            // 早班超量，说明库存不足，需要从供需比例较大的（库存比较足的）开始调整
            scheduleList = scheduleList.stream().sorted(Comparator.comparing(Cd15ScheduleResultVo::getSupplyDemandRatio, Comparator.reverseOrder())).collect(Collectors.toList());
        } else {
            // 次日夜班超量，说明库存充足，都再提前做隔天的，需要从供需比例较小的（库存比较小的）开始调整
            scheduleList = scheduleList.stream().sorted(Comparator.comparing(Cd15ScheduleResultVo::getSupplyDemandRatio)).collect(Collectors.toList());
        }

        for (Cd15ScheduleResultVo scheduleVo : scheduleList) {
            if (scheduleVo.getIsNightSpec()) { // 固定夜班规格，不处理
                continue;
            }
            if (ApsConstant.STATUS_ENABLE.equals(scheduleVo.getCloseOutSpecFlag())) { // 收尾规格不调整
                continue;
            }
            double nightPlanQty1 = scheduleVo.getNightPlanQty1(); // 1#钢带早班计划
            double nightPlanQty2 = scheduleVo.getNightPlanQty2(); // 2#钢带早班计划
            double nextDayPlanQty1 = scheduleVo.getNextDayPlanQty(); // 1#钢带夜班计划
            double nextDayPlanQty2 = scheduleVo.getNextDayPlanQty2(); // 2#钢带夜班计划
            if (nightPlanQty1 == nextDayPlanQty1) { // 中夜班计划量相等的不调整
                continue;
            }
            // 尝试平衡第二天早夜班的计划量
            boolean isNightPlanQtyLarger = nightPlanQty1 > nextDayPlanQty1; // 本规格早班计划量较大
            boolean isTotalNightPlanQtyLarger = totalNightPlanQty > totalNextDayPlanQty; // 合计值早班计划量较大
            double diffPlanQty = BigDecimalUtil.sub(nextDayPlanQty1, nightPlanQty1); // 本计划的差异值，次日夜班 - 早班
            if (isNightPlanQtyLarger != isTotalNightPlanQtyLarger) { // 本规格计划量较高的班次与总计划的相同才有必要调换
                continue;
            } else if (scheduleVo.getClassStock() < scheduleVo.getCxClass3Plan() && nextDayPlanQty1 <= 0) { // 如果交接班库存不足，且早班计划量较大，则不动
                continue;
            } else if (Math.abs(diffPlanQty) > Math.abs(difNum)) { // 如果差异值超过了总差异，则不处理
                continue;
            }
            double tempNightPlanQty = nightPlanQty1;
            nightPlanQty1 = nextDayPlanQty1;
            nextDayPlanQty1 = tempNightPlanQty;
            tempNightPlanQty = nightPlanQty2;
            nightPlanQty2 = nextDayPlanQty2;
            nextDayPlanQty2 = tempNightPlanQty;
            scheduleVo.setNightPlanQty1(nightPlanQty1);
            scheduleVo.setNightPlanQty2(nightPlanQty2);
            scheduleVo.setNextDayPlanQty(nextDayPlanQty1);
            scheduleVo.setNextDayPlanQty2(nextDayPlanQty2);
            totalNightPlanQty = BigDecimalUtil.add(totalNightPlanQty, diffPlanQty); // 总早班更新为：总早班 + (次日夜班 - 早班)
            totalNextDayPlanQty = BigDecimalUtil.sub(totalNextDayPlanQty, diffPlanQty); // 总夜班更新为：总夜班 - (次日夜班 - 早班)
            difNum = BigDecimalUtil.sub(totalNextDayPlanQty, totalNightPlanQty); // 重算差异
            if (isDayClassPass ^ difNum < 0) { // 如果计算前后差值符号相反则直接结束
                break;
            }
        }
    }
    
    /**
     * 单规格排产数量达到设定值（equalShareThreshold）时，中夜班数量对半分
     * @param scheduleList 排程列表
     * @param bisectThreshold  各班计划量均分阈值
     */
    private void equalShare(List<Cd15ScheduleResultVo> scheduleList, BigDecimal bisectThreshold, BigDecimal oneProductQty) {
        for (Cd15ScheduleResultVo scheduleVo : scheduleList) {
            if (scheduleVo.getIsNightSpec()) { // 固定夜班规格，不处理
                continue;
            }
            BigDecimal crimpLength = (BigDecimal)scheduleVo.getParams().get(EngineConstants.CRIMP_LENGTH); // 满工装长度
            BigDecimal nightPlanQty1 = BigDecimalUtils.valueOf(scheduleVo.getNightPlanQty1());
            BigDecimal nightPlanQty2 = BigDecimalUtils.valueOf(scheduleVo.getNightPlanQty2());
            BigDecimal nextDayPlanQty1 = BigDecimalUtils.valueOf(scheduleVo.getNextDayPlanQty());
            BigDecimal nextDayPlanQty2 = BigDecimalUtils.valueOf(scheduleVo.getNextDayPlanQty2());
            BigDecimal nextPlanQty1 = nightPlanQty1.add(nextDayPlanQty1);
            BigDecimal nextPlanQty2 = nightPlanQty2.add(nextDayPlanQty2);
            if (nextPlanQty1.compareTo(BigDecimal.ZERO) <= 0 && nextPlanQty2.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            boolean isCloseOutSpec = ApsConstant.STATUS_ENABLE.equals(scheduleVo.getCloseOutSpecFlag()); // 收尾标记
            BigDecimal nextPlanQtyNum1 = nextPlanQty1.divide(crimpLength, 1, RoundingMode.HALF_UP); // 工装数
            BigDecimal nextPlanQtyNum2 = nextPlanQty2.divide(crimpLength, 1, RoundingMode.HALF_UP); // 工装数
            if (nextPlanQty1.compareTo(bisectThreshold) > 0 || nextPlanQty2.compareTo(bisectThreshold) > 0) { // 超过指定计划量，则以工装的为单位平分
                BigDecimal newNightPlanQty1 = BigDecimalUtils.half(nextPlanQtyNum1).multiply(crimpLength); // 夜班平分后的计划量，先换算成工装数，平分后再换算成米数
                newNightPlanQty1 = BigDecimalUtils.least(newNightPlanQty1, nextPlanQty1); // 取整后的量不能超过总量
                BigDecimal newNextDayPlanQty1 = nextPlanQty1.subtract(newNightPlanQty1); // 夜班计划 = 总计划 - 早班计划
                scheduleVo.setNightPlanQty1(newNightPlanQty1.doubleValue());
                scheduleVo.setNextDayPlanQty(newNextDayPlanQty1.doubleValue());
                
                BigDecimal newNightPlanQty2 = BigDecimalUtils.half(nextPlanQtyNum2).multiply(crimpLength); // 夜班平分后的计划量，先换算成工装数，平分后再换算成米数
                newNightPlanQty2 = BigDecimalUtils.least(newNightPlanQty2, nextPlanQty1); // 取整后的量不能超过总量
                BigDecimal newNextDayPlanQty2 = nextPlanQty1.subtract(newNightPlanQty2); // 夜班计划 = 总计划 - 早班计划
                scheduleVo.setNightPlanQty2(newNightPlanQty2.doubleValue());
                scheduleVo.setNextDayPlanQty2(newNextDayPlanQty2.doubleValue());
            } else { // 非收尾规格，没有达到均分阈值的，要合并计划量
                if (!isCloseOutSpec) {
                    nextPlanQty1 = BigDecimalUtils.greatest(nextPlanQty1, oneProductQty);
                    nextPlanQty2 = BigDecimalUtils.greatest(nextPlanQty2, oneProductQty);
                }
                BigDecimal newNightPlanQty1 = nightPlanQty1;
                BigDecimal newNextDayPlanQty1 = nextDayPlanQty1;
                BigDecimal newNightPlanQty2 = nightPlanQty2;
                BigDecimal newNextDayPlanQty2 = nextDayPlanQty2;
                if (nightPlanQty1.compareTo(BigDecimal.ZERO) > 0) {
                    newNightPlanQty1 = nextPlanQty1;
                    newNextDayPlanQty1 = BigDecimal.ZERO;
                } else {
                    newNightPlanQty1 = BigDecimal.ZERO;
                    newNextDayPlanQty1 = nextPlanQty1;
                }
                if (nightPlanQty2.compareTo(BigDecimal.ZERO) > 0) {
                    newNightPlanQty2 = nextPlanQty2;
                    newNextDayPlanQty2 = BigDecimal.ZERO;
                } else {
                    newNightPlanQty2 = BigDecimal.ZERO;
                    newNextDayPlanQty2 = nextPlanQty2;
                }
                scheduleVo.setNightPlanQty1(newNightPlanQty1.doubleValue());
                scheduleVo.setNextDayPlanQty(newNextDayPlanQty1.doubleValue());
                scheduleVo.setNightPlanQty2(newNightPlanQty2.doubleValue());
                scheduleVo.setNextDayPlanQty2(newNextDayPlanQty2.doubleValue());
            }
        }
    }

	/**
	 * 单规格排产数量达到设定值（equalShareThreshold）时，中夜班数量对半分
	 * 
	 * @param scheduleList        排程列表
	 * @param equalShareThreshold 各班计划量均分阈值
	 */
	private void equalShare(List<Cd15ScheduleResultVo> scheduleList, String equalShareThreshold) {
		if (StringUtils.isBlank(equalShareThreshold)) {
			return;
		}
		Integer threshold = Integer.parseInt(equalShareThreshold);
		for (Cd15ScheduleResultVo schedule : scheduleList) {
			// 一天总计划量
			Double totalPlay = BigDecimalUtil.add(schedule.getDayPlanQty1(), schedule.getNightPlanQty1());
			if (totalPlay >= threshold) {
				Double equalSharePlan = BigDecimalUtil.div(totalPlay, 2);
				// 均分后，中班向上取整
				schedule.setDayPlanQty1(BigDecimalUtil.roundUp(equalSharePlan, 0));
				// 均分后，夜班向下取整
				schedule.setNightPlanQty1(BigDecimalUtil.roundDown(equalSharePlan, 0));
			}
		}
	}

	/**
	 * 各机台单独做排产均衡均衡
	 * 
	 * @param scheduleList          同一个机台的所有排程信息
	 * @param planDifferenceRateNum 中班总量和夜班总量差额百分比
	 * @param supplyTimePassNum     库存供应时长小时数
	 */
	private void equilibriumSingleMachine(List<Cd15ScheduleResultVo> scheduleList, BigDecimal planDifferenceRateNum,
			BigDecimal supplyTimePassNum) {
		// 构建均衡值对象，用于计算是否均衡
		Cd15EquilibriumVo equilibriumVo = this.createEquilibrimeVo(scheduleList);
		// 是否中班比晚班的计划量多
		boolean isDayQtyMore = equilibriumVo.getDayPlanQty().compareTo(equilibriumVo.getNightPlanQty()) > 0;
		if (isDayQtyMore) {
			// 中班较大，按中班计划量从小到大排序
			scheduleList = scheduleList.stream().sorted(Comparator.comparing(Cd15ScheduleResultVo::getDayPlanQty1))
					.collect(Collectors.toList());
		} else {
			// 晚班较大，按晚班计划量从小到大排序
			scheduleList = scheduleList.stream().sorted(Comparator.comparing(Cd15ScheduleResultVo::getNightPlanQty1))
					.collect(Collectors.toList());
		}
		// 记录上一次的差异率，初始化为第一次比对的差异率
		BigDecimal lastDifferenceRate = equilibriumVo.getDifferenceRate();
		// 如果差异率超过临界值，说明不均衡，需要做均衡处理
		for (Cd15ScheduleResultVo scheduleVo : scheduleList) {
			// 取出中班与晚班的计划量
			BigDecimal dayPlanQty1 = BigDecimal.valueOf(scheduleVo.getDayPlanQty1());
			BigDecimal nightPlanQty1 = BigDecimal.valueOf(scheduleVo.getNightPlanQty1());
			// 可供时长（插单数据时长为空）
			BigDecimal supplyTime = BigDecimal.valueOf(Optional.ofNullable(scheduleVo.getSupplyTime1()).orElse(0D));
			// 是否有做转移的标志
			boolean isChange = false;
			// 开始判断转移
			if (isDayQtyMore) {
				// 如果中班多于晚班，则尝试将一笔SUPPLY_TIME（可供时长） > 12的中班计划量转移到晚班计划量中
				if (dayPlanQty1.compareTo(BigDecimal.ZERO) > 0 && supplyTime.compareTo(supplyTimePassNum) > 0) {
					this.changeDayAndNightPlanQty(scheduleVo);
					isChange = true;
				}
			} else {
				// 如果中班少于晚班，则尝试将一笔晚班计划量转移到中班计划量中；
				if (nightPlanQty1.compareTo(BigDecimal.ZERO) > 0) {
					this.changeDayAndNightPlanQty(scheduleVo);
					isChange = true;
				}
			}
			if (isChange) {
				// 构建均衡值对象，判断是否均衡
				Cd15EquilibriumVo scheduleEquilibriumVo = this.createEquilibrimeVo(scheduleList);
				BigDecimal currentDifferenceRate = scheduleEquilibriumVo.getDifferenceRate();
				if (currentDifferenceRate == null) {
					// 如果本次运算后没有计算出差异率，说明本次转移将一个班的所有计划量全转移到另一班了，因此无法计算差异率（差异率无穷大）
					// 此情况需取消本次转移，还原中班与晚班的计划量，并结束运算
					this.changeDayAndNightPlanQty(scheduleVo);
					break;
				} else if (lastDifferenceRate != null && currentDifferenceRate.compareTo(lastDifferenceRate) > 0) {
					// 如果不均衡，则判断是否比上一次的差异率大，如果大了，则取消本次转移，保留上一次的运算结果，并结束运算
					// 还原中班与晚班的计划量
					this.changeDayAndNightPlanQty(scheduleVo);
					break;
				} else {
					// 如果上述情况均不符合，则保留本次运算结果，继续判断下一笔
					lastDifferenceRate = currentDifferenceRate;
				}
			}
		}
	}

	/**
	 * 添加均衡日志
	 * 
	 * @param scheduleList
	 * @param paramsMap
	 * @param totalPlanQtyVo
	 */
	private void insertCalculateLog(String batchNo, String oldScheduleList, List<Cd15ScheduleResultVo> scheduleList,
			BigDecimal planDifferenceRate, BigDecimal supplyTimePass, Cd15EquilibriumVo totalPlanQtyVo) {
		String logDetail = logSplit(
				"对排产结果进行均衡操作。中班总量和夜班总量的差额百分比超过了参数配置的百分比，则需要做均衡处理，也就是说要把其中一班的计划量合并到另外一班，"
						+ "一直合并到中班和夜班计划量总量的差额不超过参数配置的百分比。其中中班合并到夜班还需要遵循一个规则，就是只有库存供应时长必须要大于参数配置的值的时候，才允许从中班合并到夜班。",
				"参数配置‘PLAN_DIFFERENCE_RATE（中班总量和夜班总量差额百分比）’:" + planDifferenceRate + "，以及‘SUPPLY_TIME_PASS（库存供应时长小时数）’："
						+ supplyTimePass,
				"各班总计划量：" + toJSONString(totalPlanQtyVo), "均衡前的排程数据列表：" + oldScheduleList,
				"均衡后的排产数据列表：" + toJSONString(scheduleList));
		autoScheduleLogService.insertCd15ScheduleLog(batchNo, "", "5.1、计划量均衡处理", logDetail);
	}

	/**
	 * 调换中班与晚班的计划量
	 * 
	 * @Author hakimryan
	 * @Description
	 * @Date 2021-7-10 10:34:13
	 * @Param scheduleVo 15度排产计划值对象
	 * @Return
	 */
	private void changeDayAndNightPlanQty(Cd15ScheduleResultVo scheduleVo) {
		// 获取中班与晚班的计划量
		Double dayPlanQty = scheduleVo.getDayPlanQty1();
		Double nightPlanQty = scheduleVo.getNightPlanQty1();
		// 交换赋值
		scheduleVo.setDayPlanQty1(nightPlanQty);
		scheduleVo.setNightPlanQty1(dayPlanQty);
	}

	/**
	 * 判断是否均衡
	 * 
	 * @Author hakimryan
	 * @Description
	 * @Date 2021-7-10 10:26:48
	 * @Param differenceRate 差异率
	 * @Param planDifferenceRate 配置差异率标准值
	 * @Return true：以均衡；false：未均衡
	 */
	private boolean isBalance(BigDecimal differenceRate, BigDecimal planDifferenceRate) {
		return differenceRate != null && planDifferenceRate.compareTo(differenceRate) > 0;
	}

	/**
	 * 构建均衡值对象，用于计算目前的排产是否均衡
	 * 
	 * @Author hakimryan
	 * @Description
	 * @Date 2021-7-10 10:26:59
	 * @Param scheduleList 排产记录
	 * @Return
	 */
	private Cd15EquilibriumVo createEquilibrimeVo(List<Cd15ScheduleResultVo> scheduleList) {
		// 将晚班计划量与中班计划量加总
		BigDecimal dayPlanQty = BigDecimal.ZERO;
		BigDecimal nightPlanQty = BigDecimal.ZERO;
		for (Cd15ScheduleResultVo scheduleVo : scheduleList) {
			dayPlanQty = dayPlanQty.add(BigDecimal.valueOf(scheduleVo.getDayPlanQty1()));
			nightPlanQty = nightPlanQty.add(BigDecimal.valueOf(scheduleVo.getNightPlanQty1()));
		}
		// 是否中班比晚班的计划量多
		boolean isDayQtyMore = dayPlanQty.compareTo(nightPlanQty) > 0;
		// 差异率，默认为空
		BigDecimal differenceRate = null;
		// 需要做除数为0的校验。如果除数为0，保持差异率为空
		if (isDayQtyMore && nightPlanQty.compareTo(BigDecimal.ZERO) > 0) {
			// 中班较大，公式（中班 - 晚班）/ 晚班
			differenceRate = dayPlanQty.subtract(nightPlanQty).divide(nightPlanQty, 4, RoundingMode.HALF_UP);
		} else if (!isDayQtyMore && dayPlanQty.compareTo(BigDecimal.ZERO) > 0) {
			// 晚班较大，公式（晚班 - 中班）/ 中班
			differenceRate = nightPlanQty.subtract(dayPlanQty).divide(dayPlanQty, 4, RoundingMode.HALF_UP);
		}
		Cd15EquilibriumVo equilibriumVo = new Cd15EquilibriumVo();
		equilibriumVo.setDayPlanQty(dayPlanQty);
		equilibriumVo.setNightPlanQty(nightPlanQty);
		equilibriumVo.setDifferenceRate(differenceRate);
		return equilibriumVo;
	}

}
