package com.zlt.aps.cd90.engine.service.impl;

import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.cd90.engine.service.Cd90EngineEquilibriumService;
import com.zlt.aps.cd90.engine.vo.Cd90EquilibriumVo;
import com.zlt.aps.cd90.engine.vo.Cd90ScheduleResultVo;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.core.utils.BigDecimalUtil;
import com.zlt.aps.common.core.utils.BigDecimalUtils;
import com.zlt.aps.common.engine.constants.EngineConstants;
import com.zlt.aps.common.engine.service.AutoScheduleLogService;
import com.zlt.aps.common.engine.utils.CollectionUtil;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.alibaba.fastjson.JSON.toJSONString;
import static com.zlt.aps.common.core.utils.ApsCommonUtil.getDoubleOrDefault;
import static com.zlt.aps.common.core.utils.ApsCommonUtil.logSplit;

/**
 * 90度裁断排产均衡服务接口
 * 
 * @Description
 * @Author hakimrayn
 * @Date 2021-7-14 15:26:28
 * @Version 1.0
 */
@Service("cd90EngineEquilibriumService")
public class Cd90EngineEquilibriumServiceImpl implements Cd90EngineEquilibriumService {
	// 默认中班总量和夜班总量差额百分比：15%
	private static final Double DEFAULT_PLAN_DIFFERENCE_RATE = 15D;
	// 默认库存供应时长小时数：12小时
	private static final Double DEFAULT_SUPPLY_TIME_PASS = 12D;
	// 一百，用于百分数 -> 小数的单位换算
	private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
    private final static Double DEFAULT_EQUAL_SHARE_THRESHOLD = 500D; // 需求量超过该值早夜班对半分
	@Resource
	private AutoScheduleLogService autoScheduleLogService;

	/**
	 * 均衡处理 检测中班与晚班的差异率是否超过最大差异率，超过则尽量尝试将多的一部分产量转移（中班转移到晚班，或者晚班转移到中班）
	 * 
	 * @Author hakimryan
	 * @Description
	 * @Date 2021-7-14 15:27:55
	 * @Param scheduleList 90度裁断排产结果
	 * @Param planDifferenceRate 系统参数：中班总量和夜班总量差额百分比
	 * @Param supplyTimePass 系统参数：库存供应时长小时数
	 * @Param equalShareThreshold 系统参数：各班计划量均分阈值
	 * @Return
	 */
	@Override
	public void scheduleEquilibrium(List<Cd90ScheduleResultVo> scheduleList, String planDifferenceRate,
			String supplyTimePass, String equalShareThreshold) {
		// 系统参数类型转换
		BigDecimal planDifferenceRateNum = BigDecimal
				.valueOf(getDoubleOrDefault(planDifferenceRate, DEFAULT_PLAN_DIFFERENCE_RATE));
        BigDecimal shareThreshold = BigDecimalUtils
                .valueOf(getDoubleOrDefault(equalShareThreshold, DEFAULT_EQUAL_SHARE_THRESHOLD));
		// 单位换算：百分数转成小数
		planDifferenceRateNum = planDifferenceRateNum.divide(ONE_HUNDRED);
		BigDecimal supplyTimePassNum = BigDecimal.valueOf(getDoubleOrDefault(supplyTimePass, DEFAULT_SUPPLY_TIME_PASS));
		// 均衡运算前的排产结果，用于日志记录
		String oldScheduleList = toJSONString(scheduleList);
		// 批次号
		String batchNo = CollectionUtil.firstElement(scheduleList).getBatchNo();

        // 先分别均衡第一第二天计划
        this.equilibriumDay1(scheduleList, shareThreshold);
        this.equilibriumDay2(scheduleList, shareThreshold);
		
		// 均衡算法需要根据每个机台单独均衡 modify by 20220113
		Map<String, List<Cd90ScheduleResultVo>> scheduleMachineMap = scheduleList.stream()
				// 过滤掉无机台或者多机台的排产记录
				.filter(s -> StringUtils.isNotEmpty(s.getMachineId()) && !s.getMachineId().contains(","))
				// 按机台ID分组
				.collect(Collectors.groupingBy(Cd90ScheduleResultVo::getMachineId));
		for (List<Cd90ScheduleResultVo> groupingList : scheduleMachineMap.values()) {
			// 各机台单独均衡
			this.equilibriumSingleMachine2(groupingList, planDifferenceRateNum, supplyTimePassNum);
		}
//		this.equalShare(scheduleList, equalShareThreshold);
		// 新增计算日志
		this.insertCalculateLog(batchNo, oldScheduleList, scheduleList, planDifferenceRate, supplyTimePass,
				this.createEquilibrimeVo(scheduleList));
	}
	
    /**
     * 单规格排产数量达到设定值（equalShareThreshold）时，中夜班数量对半分
     * 
     * @param scheduleList        排程列表
     * @param equalShareThreshold 各班计划量均分阈值
     */
    private void equalShare(List<Cd90ScheduleResultVo> scheduleList, String equalShareThreshold) {
        if (StringUtils.isBlank(equalShareThreshold)) {
            return;
        }
        Integer threshold = Integer.parseInt(equalShareThreshold);
        for (Cd90ScheduleResultVo schedule : scheduleList) {
            // 总计划量
            Double totalPlay = BigDecimalUtil.add(schedule.getDayPlanQty(), schedule.getNightPlanQty());
            if (totalPlay >= threshold) {
                Double equalSharePlan = BigDecimalUtil.div(totalPlay, 2);
                // 均分后，中班向上取整
                schedule.setDayPlanQty(BigDecimalUtil.roundUp(equalSharePlan, 0));
                // 均分后，夜班向下取整
                schedule.setNightPlanQty(BigDecimalUtil.roundDown(equalSharePlan, 0));
            }
        }
    }
	
    /**
     * 均衡第一天夜班与第二天的计划
     *
     * @param scheduleList   排程列表
     * @param midPlanQtyReference 均衡
     */
    private void equilibriumDay1(List<Cd90ScheduleResultVo> scheduleList, BigDecimal bisectThreshold) {
        double totalDayPlanQty = scheduleList.stream().mapToDouble(Cd90ScheduleResultVo::getDayPlanQty).sum(); // 夜班总计划量
        double totalNightPlanQty = scheduleList.stream().mapToDouble(Cd90ScheduleResultVo::getNightPlanQty).sum(); // 早班总计划量
        double totalNextDayPlanQty = scheduleList.stream().mapToDouble(Cd90ScheduleResultVo::getNextDayPlanQty).sum(); // 次日夜班总计划量
        double midPlanQtyReference = BigDecimalUtils.avg(0, RoundingMode.DOWN, totalDayPlanQty, totalNightPlanQty, totalNextDayPlanQty).doubleValue(); // 计划平均值
        // 日志记录
        double oldTotalDayPlanQty = totalDayPlanQty; // 夜班总计划量
        double oldTotalNightPlanQty = totalNightPlanQty; // 早班总计划量
        double oldTotalNextDayPlanQty = totalNextDayPlanQty; // 次日夜班总计划量
        
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
                // 早班超量，将交接班库存较充低的转移到夜班（顺序）
                return classStock1.compareTo(classStock2);
            }
        }).collect(Collectors.toList());
        for (Cd90ScheduleResultVo scheduleVo: scheduleList) {
            if (ApsConstant.STATUS_ENABLE.equals(scheduleVo.getCloseOutSpecFlag())) { // 收尾规格不处理
                continue;
            }
            if (isNightClassPass && scheduleVo.getIsNightSpec()) { // 夜班超量，则不处理固定夜班的规格
                continue;
            }
            BigDecimal toolCapacity = (BigDecimal)scheduleVo.getParams().get(EngineConstants.TOOL_CAPACITY); // 满工装长度
            if (Math.abs(difNum) < toolCapacity.doubleValue()) {
                break; // 差异不足一卷时停止处理
            }
            Boolean isLargeDemandSpec = scheduleVo.getIsLargeDemandSpec();
            BigDecimal dayPlanQty = BigDecimalUtils.valueOf(scheduleVo.getDayPlanQty());
            BigDecimal nightPlanQty = BigDecimalUtils.valueOf(scheduleVo.getNightPlanQty());
            BigDecimal nextDayPlanQty = BigDecimalUtils.valueOf(scheduleVo.getNextDayPlanQty());
            BigDecimal nextDayCxPlanQty = BigDecimalUtils.add(scheduleVo.getCxClass3Plan(), scheduleVo.getCxClass4Plan()); // 第二天成型需求量
            double classStock2 = scheduleVo.getClassStock(); // 第二天交接班库存
            BigDecimal dayAddPlan = BigDecimal.ZERO; // 夜班增加量
            BigDecimal nightAddPlan = BigDecimal.ZERO; // 早班增加量
            BigDecimal nextDayAddPlan = BigDecimal.ZERO; // 次日夜班增加量
            BigDecimal movePlanQty  = BigDecimalUtils.ceil(nextDayCxPlanQty.divide(BigDecimal.TEN, 0, RoundingMode.UP), toolCapacity); // 转移量，总需求量的10%
            if (isLargeDemandSpec) { // 夜班超量且是大规格
                BigDecimal avgPlanQty = BigDecimalUtils.avg(0, RoundingMode.UP, dayPlanQty, nightPlanQty, nextDayPlanQty);
                BigDecimal movePlanQty1 = BigDecimalUtils.ceil(dayPlanQty.subtract(avgPlanQty), toolCapacity).abs(); // 挪超过/低于平均值的部分
                movePlanQty = BigDecimalUtils.greatest(movePlanQty, movePlanQty1); // 取两个算法最大的
            }
            if (isNightClassPass) { // 夜班超量，则从夜班转移到隔天早班，但是不能导致夜班缺量
                if (classStock2 <= toolCapacity.doubleValue()) { // 交接班库存不足一车的也不处理
                    continue;
                }
                nightAddPlan = BigDecimalUtils.floor(BigDecimalUtils.least(movePlanQty, dayPlanQty), toolCapacity); // 早班加量
                dayAddPlan = nightAddPlan.negate(); // 夜班减量
            } else if (nightPlanQty.compareTo(BigDecimal.ZERO) > 0) { // 隔天超量，且早班大于0，则从早班转移到夜班
                dayAddPlan = BigDecimalUtils.floor(BigDecimalUtils.least(movePlanQty, nightPlanQty), toolCapacity);// 夜班加量
                if (BigDecimalUtils.add(classStock2, dayAddPlan).subtract(nextDayCxPlanQty).compareTo(toolCapacity) >= 0) { // 转移后交接班库存比第二天需求量还多一个工装的不处理
                    continue;
                }
                nightAddPlan = dayAddPlan.negate(); // 早班减量
                
                BigDecimal lastMidPlanQty = BigDecimalUtils.valueOf(scheduleVo.getLastMidPlanQty());
                // 如果夜班本身没有安排计划，且上一天早班有安排计划，需要检查加上转移量之后是否达到均分阈值
                if (dayPlanQty.compareTo(BigDecimal.ZERO) == 0 && lastMidPlanQty.compareTo(BigDecimal.ZERO) > 0 && dayAddPlan.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal newDayPlanQty = dayPlanQty.add(dayAddPlan);
                    if (newDayPlanQty.add(lastMidPlanQty).compareTo(bisectThreshold) <= 0) {
                        continue; // ，而上一天早班加夜班计划量没有达到均分阈值，则不能转移
                    }
                }
            }
            if (dayAddPlan.compareTo(BigDecimal.ZERO) == 0) {
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
        if (CollectionUtils.isNotEmpty(scheduleList)) {
            String batchNo = CollectionUtil.firstElement(scheduleList).getBatchNo();
            String logDetail = logSplit("均衡基准值：" + midPlanQtyReference,
                    "夜班计划，均衡前：" + oldTotalDayPlanQty + "，均衡后：" + totalDayPlanQty,
                    "早班计划，均衡前：" + oldTotalNightPlanQty + "，均衡后：" + totalNightPlanQty,
                    "次日夜班计划，均衡前：" + oldTotalNextDayPlanQty + "，均衡后：" + totalNextDayPlanQty);
            autoScheduleLogService.insertCd90ScheduleLog(batchNo, "", "5.2、第一第二天计划量均衡处理", logDetail);
        }
    }
    
    /**
     * 计算交接班库存
     * @param scheduleVo
     * @return
     */
    private Double getClassStock(Cd90ScheduleResultVo scheduleVo) {
        BigDecimal planQty = BigDecimalUtils.add(scheduleVo.getStockQty(), scheduleVo.getLastMidPlanQty(), scheduleVo.getDayPlanQty());
        BigDecimal cxPlanQty = BigDecimalUtils.add(scheduleVo.getCxClass1Plan(), scheduleVo.getCxClass2Plan());
        return planQty.subtract(cxPlanQty).doubleValue();
    }
    
    /**
     * 均衡第二天早夜班库存
     *
     * @param scheduleList    排程列表
     * @param totalPlanQtyVo  中班和夜班总计划量Vo
     */
    private void equilibriumDay2(List<Cd90ScheduleResultVo> scheduleList, BigDecimal bisectThreshold) {
        this.equalShare(scheduleList, bisectThreshold); // 先均分中夜班计划量
        double totalNightPlanQty = scheduleList.stream().mapToDouble(Cd90ScheduleResultVo::getNightPlanQty).sum(); // 早班总计划量
        double totalNextDayPlanQty = scheduleList.stream().mapToDouble(Cd90ScheduleResultVo::getNextDayPlanQty).sum(); // 次日夜班总计划量
        double oldTotalNightPlanQty = totalNightPlanQty; // 早班总计划量
        double oldTotalNextDayPlanQty = totalNextDayPlanQty; // 次日夜班总计划量
        double toolCapacity = ((BigDecimal)CollectionUtil.firstElement(scheduleList).getParams().get(EngineConstants.TOOL_CAPACITY)).doubleValue(); // 满工装长度
        double difNum = BigDecimalUtil.sub(totalNextDayPlanQty, totalNightPlanQty); //早班和次日夜班的计划量差额
        if (Math.abs(difNum) <= toolCapacity) { // 差异少于一个工装，无需处理
            return;
        }

        boolean isDayClassPass = (difNum < 0);  //true：早班超量，false：次日夜班超量
        if (isDayClassPass) {
            // 早班超量，说明库存不足，需要从供需比例较大的（库存比较足的）开始调整
            scheduleList = scheduleList.stream().sorted(Comparator.comparing(Cd90ScheduleResultVo::getSupplyDemandRatio, Comparator.reverseOrder())).collect(Collectors.toList());
        } else {
            // 次日夜班超量，说明库存充足，都再提前做隔天的，需要从供需比例较小的（库存比较小的）开始调整
            scheduleList = scheduleList.stream().sorted(Comparator.comparing(Cd90ScheduleResultVo::getSupplyDemandRatio)).collect(Collectors.toList());
        }

        for (Cd90ScheduleResultVo scheduleVo : scheduleList) {
            if (ApsConstant.STATUS_ENABLE.equals(scheduleVo.getCloseOutSpecFlag())) { // 收尾规格不调整
                continue;
            }
            if (scheduleVo.getIsNightSpec()) { // 固定夜班规格，不处理
                continue;
            }
            double nightPlanQty = scheduleVo.getNightPlanQty(); // 早班计划
            double nextDayPlanQty = scheduleVo.getNextDayPlanQty(); // 夜班计划
            if (nightPlanQty == nextDayPlanQty) { // 中夜班计划量相等的不调整
                continue;
            }
            // 尝试平衡第二天早夜半的计划量
            boolean isNightPlanQtyLarger = nightPlanQty > nextDayPlanQty; // 本规格早班计划量较大
            boolean isTotalNightPlanQtyLarger = totalNightPlanQty > totalNextDayPlanQty; // 合计值早班计划量较大
            double diffPlanQty = BigDecimalUtil.sub(nextDayPlanQty, nightPlanQty); // 本计划的差异值，次日夜班 - 早班
            if (isNightPlanQtyLarger != isTotalNightPlanQtyLarger) { // 本规格计划量较高的班次与总计划的相同才有必要调换
                continue;
            } else if (scheduleVo.getClassStock() < scheduleVo.getCxClass3Plan() && nextDayPlanQty <= 0) { // 如果交接班库存不足，且早班计划量较大，则不动
                continue;
            } else if (Math.abs(diffPlanQty) > Math.abs(difNum)) { // 如果差异值超过了总差异，则不处理
                continue;
            }
            scheduleVo.setNightPlanQty(nextDayPlanQty);
            scheduleVo.setNextDayPlanQty(nightPlanQty);
            totalNightPlanQty = BigDecimalUtil.add(totalNightPlanQty, diffPlanQty); // 总早班更新为：总早班 + (次日夜班 - 早班)
            totalNextDayPlanQty = BigDecimalUtil.sub(totalNextDayPlanQty, diffPlanQty); // 总夜班更新为：总夜班 - (次日夜班 - 早班)
            difNum = BigDecimalUtil.sub(totalNextDayPlanQty, totalNightPlanQty); // 重算差异
            if (Math.abs(difNum) <= toolCapacity || isDayClassPass ^ difNum < 0) { // 差异不足一个工装、或者计算前后差值符号相反则直接结束
                break;
            }
        }
        if (CollectionUtils.isNotEmpty(scheduleList)) {
            String batchNo = CollectionUtil.firstElement(scheduleList).getBatchNo();
            String logDetail = logSplit("早班计划，均衡前：" + oldTotalNightPlanQty + "，均衡后：" + totalNightPlanQty,
                    "次日夜班计划，均衡前：" + oldTotalNextDayPlanQty + "，均衡后：" + totalNextDayPlanQty);
            autoScheduleLogService.insertCd90ScheduleLog(batchNo, "", "5.3、第二天计划量均衡处理", logDetail);
        }
    }
    
    /**
     * 单规格排产数量达到设定值（equalShareThreshold）时，中夜班数量对半分
     * @param scheduleList 排程列表
     * @param bisectThreshold  各班计划量均分阈值
     */
    private void equalShare(List<Cd90ScheduleResultVo> scheduleList, BigDecimal bisectThreshold) {
        // 次日早夜班总计划量超过2个工装（10卷）的先平分
        for (Cd90ScheduleResultVo scheduleVo : scheduleList) {
            BigDecimal toolCapacity = (BigDecimal)scheduleVo.getParams().get(EngineConstants.TOOL_CAPACITY); // 满工装长度
            if (scheduleVo.getIsNightSpec()) { // 固定夜班规格，不处理
                continue;
            }
            BigDecimal nightPlanQty = BigDecimalUtils.valueOf(scheduleVo.getNightPlanQty());
            BigDecimal nextDayPlanQty = BigDecimalUtils.valueOf(scheduleVo.getNextDayPlanQty());
//            if (nightPlanQty.compareTo(nextDayPlanQty) == 0) {
//                continue;
//            }
            BigDecimal nextPlanQty = nightPlanQty.add(nextDayPlanQty);
            BigDecimal nextPlanQtyNum = nextPlanQty.divide(toolCapacity, 1, RoundingMode.HALF_UP); // 工装数
            if (nextPlanQty.compareTo(bisectThreshold) < 0) {// 少于8卷直接合并
                if (nightPlanQty.compareTo(BigDecimal.ZERO) > 0) {
                    scheduleVo.setNightPlanQty(nextPlanQty.doubleValue());
                    scheduleVo.setNextDayPlanQty(0D);
                }
            } else { // 超过指定计划量，则以工装的为单位平分
                BigDecimal newNightPlanQty = nextPlanQtyNum.divide(BigDecimalUtils.TWO, 0, RoundingMode.UP).multiply(toolCapacity); // 夜班平分后的计划量，先换算成工装数，平分后再换算成米数
                newNightPlanQty = BigDecimalUtils.least(newNightPlanQty, nextPlanQty); // 取整后的量不能超过总量
                BigDecimal newNextDayPlanQty = nextPlanQty.subtract(newNightPlanQty); // 夜班计划 = 总计划 - 早班计划
                scheduleVo.setNightPlanQty(newNightPlanQty.doubleValue());
                scheduleVo.setNextDayPlanQty(newNextDayPlanQty.doubleValue());
            }
        }
    }

	private void equilibriumSingleMachine(List<Cd90ScheduleResultVo> scheduleList, BigDecimal planDifferenceRateNum,
			BigDecimal supplyTimePassNum) {
		// 构建均衡值对象，用于计算是否均衡
		Cd90EquilibriumVo equilibriumVo = this.createEquilibrimeVo(scheduleList);
		// 是否中班比晚班的计划量多
		boolean isDayQtyMore = equilibriumVo.getDayPlanQty().compareTo(equilibriumVo.getNightPlanQty()) > 0;
		if (isDayQtyMore) {
			// 中班较大，按中班计划量从小到大排序
			scheduleList = scheduleList.stream().sorted(Comparator.comparing(Cd90ScheduleResultVo::getDayPlanQty))
					.collect(Collectors.toList());
		} else {
			// 晚班较大，按晚班计划量从小到大排序
			scheduleList = scheduleList.stream().sorted(Comparator.comparing(Cd90ScheduleResultVo::getNightPlanQty))
					.collect(Collectors.toList());
		}
		// 记录上一次的差异率，初始化为第一次比对的差异率
		BigDecimal lastDifferenceRate = equilibriumVo.getDifferenceRate();
		// 如果差异率超过临界值，说明不均衡，需要做均衡处理
		for (Cd90ScheduleResultVo scheduleVo : scheduleList) {
			// 取出中班与晚班的计划量
			BigDecimal dayPlanQty1 = BigDecimal.valueOf(scheduleVo.getDayPlanQty());
			BigDecimal nightPlanQty1 = BigDecimal.valueOf(scheduleVo.getNightPlanQty());
			// 可供时长（插单数据时长为空）
			BigDecimal supplyTime = BigDecimal.valueOf(Optional.ofNullable(scheduleVo.getSupplyTime()).orElse(0D));
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
				Cd90EquilibriumVo scheduleEquilibriumVo = this.createEquilibrimeVo(scheduleList);
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

	private void equilibriumSingleMachine2(List<Cd90ScheduleResultVo> scheduleList, BigDecimal planDifferenceRateNum,
										   BigDecimal supplyTimePassNum) {
		// 构建均衡值对象，用于计算是否均衡
		Cd90EquilibriumVo equilibriumVo = this.createEquilibrimeVo(scheduleList);
		// 是否中班比晚班的计划量多
		boolean isDayQtyMore = equilibriumVo.getDayPlanQty().compareTo(equilibriumVo.getNightPlanQty()) > 0;
		if (isDayQtyMore) {
			// 中班较大，按中班计划量从小到大排序
			scheduleList = scheduleList.stream().sorted(Comparator.comparing(Cd90ScheduleResultVo::getDayPlanQty))
					.collect(Collectors.toList());
		} else {
			// 晚班较大，按晚班计划量从小到大排序
			scheduleList = scheduleList.stream().sorted(Comparator.comparing(Cd90ScheduleResultVo::getNightPlanQty))
					.collect(Collectors.toList());
		}
		// 记录上一次的差异率，初始化为第一次比对的差异率
		BigDecimal lastDifferenceRate = equilibriumVo.getDifferenceRate();
		BigDecimal totalNightPlanQty = equilibriumVo.getNightPlanQty(); // 早班总计划里量
		BigDecimal totalNextDayPlanQty = equilibriumVo.getNextDayPlanQty(); // 次日夜班总计划量
		BigDecimal difNum = BigDecimalUtils.sub(totalNextDayPlanQty, totalNightPlanQty); //早班和次日夜班的计划量差额
		// 如果差异率超过临界值，说明不均衡，需要做均衡处理
		for (Cd90ScheduleResultVo scheduleVo : scheduleList) {
			/*// 取出中班与晚班的计划量
			BigDecimal dayPlanQty1 = BigDecimal.valueOf(scheduleVo.getDayPlanQty());
			BigDecimal nightPlanQty1 = BigDecimal.valueOf(scheduleVo.getNightPlanQty());
			// 可供时长（插单数据时长为空）
			BigDecimal supplyTime = BigDecimal.valueOf(Optional.ofNullable(scheduleVo.getSupplyTime()).orElse(0D));
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
				Cd90EquilibriumVo scheduleEquilibriumVo = this.createEquilibrimeVo(scheduleList);
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
			}*/
			double nightPlanQty = scheduleVo.getNightPlanQty();
			double nextDayPlanQty = scheduleVo.getNextDayPlanQty();
			// 尝试平衡第二天早夜半的计划量
			double classStock2 = scheduleVo.getClassStock();
			double cxPlanQty2 = BigDecimalUtil.add(scheduleVo.getCxClass3Plan(), scheduleVo.getCxClass4Plan());// 第二天成型两个班消耗量
			boolean isNightPlanQtyLarger = nightPlanQty > nextDayPlanQty; // 本规格夜班计划量较大
			boolean isTotalNightPlanQtyLarger = totalNightPlanQty.compareTo(totalNextDayPlanQty) > 0; // 合计值夜班计划量较大
			BigDecimal diffPlanQty = BigDecimalUtils.sub(BigDecimal.valueOf(nextDayPlanQty), BigDecimal.valueOf(nightPlanQty)); // 本计划的差异值，次日夜班 - 早班
            /*if (new BigDecimal(scheduleVo.getSpecSize()).compareTo(BIG_SIZE_SPEC) >= 0) { // 大尺寸，要同时判断大尺寸规格的总计划量
                boolean isBigSizeNightPlanQtyLarger = bigSizeNgintPlanQty > bigSizeDayPlanQty; // 大规格夜班总计划量较大
                if (isNightPlanQtyLarger != isBigSizeNightPlanQtyLarger) { // 本规格计划量较高的班次与大规格的相同才有必要调换
                    continue;
                }
            } else */
			if (isNightPlanQtyLarger != isTotalNightPlanQtyLarger) { // 本规格计划量较高的班次与总计划的相同才有必要调换
				continue;
			} else if (classStock2 < cxPlanQty2 && !isNightPlanQtyLarger) { // 如果交接班库存不足，且早班计划量较大，则不动
				continue;
			} else if (Math.abs(diffPlanQty.doubleValue()) > Math.abs(difNum.doubleValue())) { // 如果差异值超过了总差异，则不处理
				continue;
			}
			double tempNightPlanQty = nightPlanQty;
			nightPlanQty = nextDayPlanQty;
			nextDayPlanQty = tempNightPlanQty;
			scheduleVo.setNightPlanQty(nightPlanQty);
			scheduleVo.setNextDayPlanQty(nextDayPlanQty);
			totalNightPlanQty = BigDecimalUtils.add(totalNightPlanQty, diffPlanQty); // 总早班更新为：总早班 + (次日夜班 - 早班)
			totalNextDayPlanQty = BigDecimalUtils.sub(totalNextDayPlanQty, diffPlanQty); // 总夜班更新为：总夜班 - (次日夜班 - 早班)
			difNum = BigDecimalUtils.sub(totalNextDayPlanQty, totalNightPlanQty); // 重算差异
		}
	}

	/**
	 * 添加均衡日志
	 * 
	 * @param scheduleList
	 * @param totalPlanQtyVo
	 */
	private void insertCalculateLog(String batchNo, String oldScheduleList, List<Cd90ScheduleResultVo> scheduleList,
			String planDifferenceRate, String supplyTimePass, Cd90EquilibriumVo totalPlanQtyVo) {
		String logDetail = logSplit(
				"对排产结果进行均衡操作。中班总量和夜班总量的差额百分比超过了参数配置的百分比，则需要做均衡处理，也就是说要把其中一班的计划量合并到另外一班，"
						+ "一直合并到中班和夜班计划量总量的差额不超过参数配置的百分比。其中中班合并到夜班还需要遵循一个规则，就是只有库存供应时长必须要大于参数配置的值的时候，才允许从中班合并到夜班。",
				"参数配置‘PLAN_DIFFERENCE_RATE（中班总量和夜班总量差额百分比）’:" + planDifferenceRate + "，以及‘SUPPLY_TIME_PASS（库存供应时长小时数）’："
						+ supplyTimePass,
				"各班总计划量：" + toJSONString(totalPlanQtyVo), "均衡前的排程数据列表：" + oldScheduleList,
				"均衡后的排产数据列表：" + toJSONString(scheduleList));
		autoScheduleLogService.insertCd90ScheduleLog(batchNo, "", "5.1、计划量均衡处理", logDetail);
	}

	/**
	 * 调换中班与晚班的计划量
	 * 
	 * @Author hakimryan
	 * @Description
	 * @Date 2021-7-14 10:34:13
	 * @Param scheduleVo 15度排产计划值对象
	 * @Return
	 */
	private void changeDayAndNightPlanQty(Cd90ScheduleResultVo scheduleVo) {
		// 获取中班与晚班的计划量
		Double dayPlanQty = scheduleVo.getDayPlanQty();
		Double nightPlanQty = scheduleVo.getNightPlanQty();
		// 交换赋值
		scheduleVo.setDayPlanQty(nightPlanQty);
		scheduleVo.setNightPlanQty(dayPlanQty);
	}

	/**
	 * 判断是否均衡
	 * 
	 * @Author hakimryan
	 * @Description
	 * @Date 2021-7-14 10:26:48
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
	 * @Date 2021-7-14 10:26:59
	 * @Param scheduleList 排产记录
	 * @Return
	 */
	private Cd90EquilibriumVo createEquilibrimeVo(List<Cd90ScheduleResultVo> scheduleList) {
		// 将晚班计划量与中班计划量加总
		BigDecimal dayPlanQty = BigDecimal.ZERO;
		BigDecimal nightPlanQty = BigDecimal.ZERO;
		BigDecimal nextDayPlanQty = BigDecimal.ZERO;
		for (Cd90ScheduleResultVo scheduleVo : scheduleList) {
			dayPlanQty = dayPlanQty.add(BigDecimal.valueOf(scheduleVo.getDayPlanQty()));
			nightPlanQty = nightPlanQty.add(BigDecimal.valueOf(scheduleVo.getNightPlanQty()));
			nextDayPlanQty = nextDayPlanQty.add(BigDecimal.valueOf(scheduleVo.getNextDayPlanQty()));
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
		Cd90EquilibriumVo equilibriumVo = new Cd90EquilibriumVo();
		equilibriumVo.setDayPlanQty(dayPlanQty);
		equilibriumVo.setNightPlanQty(nightPlanQty);
		equilibriumVo.setNextDayPlanQty(nightPlanQty);
		equilibriumVo.setDifferenceRate(differenceRate);
		return equilibriumVo;
	}

}
