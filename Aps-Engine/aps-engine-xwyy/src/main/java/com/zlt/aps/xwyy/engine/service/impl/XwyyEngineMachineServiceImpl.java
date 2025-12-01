package com.zlt.aps.xwyy.engine.service.impl;

import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.core.utils.BigDecimalUtil;
import com.zlt.aps.common.core.utils.BigDecimalUtils;
import com.zlt.aps.common.engine.constants.EngineConstants;
import com.zlt.aps.common.engine.enums.OpenMachineClassEnums;
import com.zlt.aps.common.engine.service.AutoScheduleLogService;
import com.zlt.aps.common.engine.utils.CollectionUtil;
import com.zlt.aps.xwyy.api.domain.entity.XwyyMachineInfo;
import com.zlt.aps.xwyy.engine.mapper.XwyyEngineMachineRollMappingMapper;
import com.zlt.aps.xwyy.engine.mapper.XwyyEngineMapper;
import com.zlt.aps.xwyy.engine.mapper.XwyyEngineSpecifyMachineMapper;
import com.zlt.aps.xwyy.engine.service.XwyyEngineMachineService;
import com.zlt.aps.xwyy.engine.vo.XwyyMachineRollMappingVo;
import com.zlt.aps.xwyy.engine.vo.XwyyParamsVo;
import com.zlt.aps.xwyy.engine.vo.XwyyScheduleResultVo;
import com.zlt.aps.xwyy.engine.vo.XwyySpecifyMachineVo;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

import static com.alibaba.fastjson.JSON.toJSONString;
import static com.zlt.aps.common.core.utils.ApsCommonUtil.logSplit;

/**
 * 纤维压延生产线服务实现类
 * 
 * @Description
 * @Author hakimrayn
 * @Date 2021-7-22 11:24:00
 * @Version 1.0
 */
@Service("xwyyEngineMachineService")
public class XwyyEngineMachineServiceImpl implements XwyyEngineMachineService {
	@Autowired
	private XwyyEngineSpecifyMachineMapper xwyyEngineSpecifyMachineMapper;
	@Autowired
	private XwyyEngineMachineRollMappingMapper xwyyEngineMachineRollMappingMapper;
	@Resource
	private AutoScheduleLogService autoScheduleLogService;
	
	private final static Double MIN_PLAN_QTY = 2300D;
	private final static String CLASS_HOUR = "12"; // 班次时长
	
	/**
	 * 日志分割符
	 */
	private String division = "\r\n---------------------------------------------------\r\n";

	/**
	 * 为纤维压延排程安排生产线
	 * 
	 * @Author hakimryan
	 * @Description
	 * @Date 2021-7-22 11:29:58
	 * @param scheduleList 90度裁断排产
	 */
	@Override
	public void scheduleMachine(List<XwyyScheduleResultVo> scheduleList) {
		// 批次号
		String batchNo = CollectionUtil.firstElement(scheduleList).getBatchNo();
		autoScheduleLogService.insertXwyyScheduleLog(batchNo, "", "2.1、生产线安排",
				"优先查看是否有帘线大卷与定点机台的配置，有则安排该生产线；无则查看是否有帘线大卷与机台的映射配置，有则查看该配置是否有在定点机台中配置为不生产，没有则安排该生产线");
		// 抓取钢压大卷与机台的对照关系
		// 限制作业
		Map<String, String> canWorkMap = this.getXwyySpecifyMachineMap(EngineConstants.JOB_TYPE_CAN);
		// 不可作业
		Map<String, String> notWorkMap = this.getXwyySpecifyMachineMap(EngineConstants.JOB_TYPE_NOT);

		// 抓取大卷与机台的对照关系
		List<XwyyMachineRollMappingVo> machineRollList = xwyyEngineMachineRollMappingMapper
				.selectXwyyMachineRollMappingList();
		Map<String, String> machineRollMap = machineRollList.stream().collect(
				Collectors.toMap(XwyyMachineRollMappingVo::getBigRollCode, XwyyMachineRollMappingVo::getMachineId));
		// 记录日志
		String logDetail = logSplit("大卷与定点机台配置：" + toJSONString(canWorkMap), "大卷不可作业的机台配置：" + toJSONString(notWorkMap),
				"大卷与机台的映射关系：" + toJSONString(machineRollMap));
		autoScheduleLogService.insertXwyyScheduleLog(batchNo, "", "2.2、生产线安排基础数据日志", logDetail);

		for (XwyyScheduleResultVo scheduleResult : scheduleList) {
			if (StringUtils.isNotBlank(scheduleResult.getMachineId())) {
				// 判断如果已经有机台了，则说明是从上一次排程复制下来的，可以直接跳过
				continue;
			}

			// 1、先看大卷与定点机台是否有匹配的关系
			String bigRollCode = scheduleResult.getBigRollCode();
			String machineId = null;
			if (canWorkMap.containsKey(bigRollCode)) {
				machineId = canWorkMap.get(bigRollCode);
			}
			// 2、再看映射表有没有匹配，只有定点机台没有匹配上才需要看
			if (machineId == null && machineRollMap.containsKey(bigRollCode)) {
				machineId = machineRollMap.get(bigRollCode);
				// 除了大卷匹配上，还要求机台沒有被设置为“不可作业”
				machineId = this.removeNotWorkMachineId(notWorkMap, bigRollCode, machineId);
			}
			scheduleResult.setMachineId(machineId);
		}
		// 记录日志
		autoScheduleLogService.insertXwyyScheduleLog(batchNo, "", "2.3、生产线安排完成", "安排生产线后排程记录：" + toJSONString(scheduleList));
	}

	/**
	 * 根据产能选机台
	 *
	 * @param scheduleList 排程数据
     * @param machineQuataHoure 机台产能时长
	 */
	@Override
	public void chooseMachineByCapacity(List<XwyyScheduleResultVo> scheduleList, BigDecimal machineQuataHoure) {
		// 根据机台产能选机台
		List<XwyyMachineInfo> allMachineList = this.listXwyyMachine();
		Map<String, String> specifyCanMachineMap = this.getXwyySpecifyMachineMap(EngineConstants.JOB_TYPE_CAN);
		Map<String, String> specifyNotMachineMap = this.getXwyySpecifyMachineMap(EngineConstants.JOB_TYPE_NOT);

		// 机台夜班已占用产能
		Map<Long, BigDecimal> midCapacityMap = new HashMap<>(16);
		// 机台白班已占用产能
		Map<Long, BigDecimal> nightCapacityMap = new HashMap<>(16);

		// 先对排产计划
		List<XwyyScheduleResultVo> chooseMachineScheduleList = scheduleList.stream().sorted((o1, o2) -> {
			Integer flag1 = specifyCanMachineMap.containsKey(o1.getBigRollCode()) ? 1 : 2;
			Integer flag2 = specifyCanMachineMap.containsKey(o2.getBigRollCode()) ? 1 : 2;
			if (flag1.compareTo(flag2) != 0) { // 先看哪个有定点机台，有定点机台的优先选机台
				return flag1.compareTo(flag2);
			}
			// 如果定点机台设置一样，则按计划量从大到小
			BigDecimal planQty1 = BigDecimalUtils.add(o1.getDayPlanQty(), o1.getNightPlanQty());
			BigDecimal planQty2 = BigDecimalUtils.add(o2.getDayPlanQty(), o2.getNightPlanQty());
			return planQty2.compareTo(planQty1);
		}).collect(Collectors.toList());

		// 根据夜班计划分配机台
		for (XwyyScheduleResultVo scheduleVo : chooseMachineScheduleList) {
			Double midPlanQty = scheduleVo.getDayPlanQty();
			String classCode = String.valueOf(OpenMachineClassEnums.CLASS_TWO.getClassIndex()); // 夜班
			List<XwyyMachineInfo> optionalMachineList = this.searchOptionalMachineList(scheduleVo, classCode, midCapacityMap,
					allMachineList, specifyCanMachineMap, specifyNotMachineMap); // 检索当班可选机台
			if (CollectionUtil.isEmpty(optionalMachineList)) {
				continue;
			}
			// 如果有匹配机台，则直接取第一个机台赋值
			XwyyMachineInfo machine = CollectionUtil.firstElement(optionalMachineList);
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
			// 添加日志
			chooseMachineLog(scheduleVo, specifyCanMachineMap, specifyNotMachineMap);
		}

		// 剩余没有分配到机台的排程检查早班是否有可分配机台
		for (XwyyScheduleResultVo scheduleVo : chooseMachineScheduleList) {
			if (StringUtils.isNotEmpty(scheduleVo.getMachineId())) {
				continue;
			}
			// 早班
			String classCode = String.valueOf(OpenMachineClassEnums.CLASS_THREE.getClassIndex());
			List<XwyyMachineInfo> optionalMachineList = this.searchOptionalMachineList(scheduleVo, classCode, nightCapacityMap,
					// 检索当班可选机台
					allMachineList, specifyCanMachineMap, specifyNotMachineMap);
			if (CollectionUtil.isEmpty(optionalMachineList)) {
				continue;
			}
			// 如果有匹配机台，则直接取第一个机台赋值
			XwyyMachineInfo machine = CollectionUtil.firstElement(optionalMachineList);
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
			// 添加日志
			chooseMachineLog(scheduleVo, specifyCanMachineMap, specifyNotMachineMap);
		}
		
		this.caculateMachineQuata(chooseMachineScheduleList, allMachineList, machineQuataHoure); // 根据机台定额重算计划量
		
        // 重算个数与总量
		for (XwyyScheduleResultVo resultVo: chooseMachineScheduleList) {
		    BigDecimal standardSize = (BigDecimal) resultVo.getParams().get(EngineConstants.STANDARD_SIZE);
		    double dayPlanQty = resultVo.getDayPlanQty();
            double nightPlanQty = resultVo.getNightPlanQty();
            double totalPlanQty = BigDecimalUtil.add(dayPlanQty, nightPlanQty);
            resultVo.setDayPlanQtyNum(BigDecimalUtil.div(dayPlanQty, standardSize.doubleValue(), 1));
            resultVo.setNightPlanQtyNum(BigDecimalUtil.div(nightPlanQty, standardSize.doubleValue(), 1));
            resultVo.setTotalPlan(totalPlanQty);
            resultVo.setTotalPlanNum(BigDecimalUtils.add(resultVo.getDayPlanQtyNum(), resultVo.getNightPlanQtyNum()));
		}
	}
	
    /**
     * 根据机台定额重算计划量
     * @param scheduleList      排产计划
     * @param allMachineList    机台
     */
    private void caculateMachineQuata(List<XwyyScheduleResultVo> scheduleList, List<XwyyMachineInfo> allMachineList, BigDecimal machineQuataHour) {
        BigDecimal classHour = BigDecimalUtils.valueOf(CLASS_HOUR);
        // 计算机台总产能
        for (XwyyMachineInfo machine: allMachineList) {
            // 查找安排在此机台上的规格
            List<XwyyScheduleResultVo> matchScheduleList = scheduleList.stream()
                    .filter(s -> StringUtils.isNotEmpty(s.getMachineId()) 
                            && !s.getMachineId().contains(",")
                            && machine.getId().equals(new Long(s.getMachineId()))) // 根据机台ID过滤
                    .collect(Collectors.toList());
            if (CollectionUtils.isEmpty(matchScheduleList)) {
                continue;
            }
            BigDecimal quata = machine.getQuata(); // 产能
            if (machineQuataHour.compareTo(classHour) != 0) { // 非整版生产，则需要乘以生产小时数除以班次小时数
                quata = quata.multiply(machineQuataHour).divide(classHour, 1, RoundingMode.HALF_UP);
            }
            String openMachineClass = machine.getOpenMachineClass();
            int openClass = 0; // 开机班数
            if (StringUtils.isNotEmpty(openMachineClass)) {
                openClass = openMachineClass.split(",").length;
            }
            Double totalQuata = BigDecimalUtils.multiply(quata, new BigDecimal(openClass)).doubleValue(); // 总定额
            // 总计划量要等于定额，多或者少了的差值直接加到计划量最大的规格上，差值也要修正成整卷，每次处理一卷
            Double totalPlan = matchScheduleList.stream().map(XwyyScheduleResultVo::getTotalPlan).reduce(0D, (a, b) -> BigDecimalUtil.add(a, b));
            if (totalPlan.doubleValue() == totalQuata.doubleValue()) {
                return;
            }
            BigDecimal diffPlan = BigDecimalUtils.sub(totalQuata, totalPlan); // 差值
            boolean isDiff = diffPlan.compareTo(BigDecimal.ZERO) != 0;
            boolean isCapacityPass = diffPlan.compareTo(BigDecimal.ZERO) < 0;
            while (isDiff) {
                XwyyScheduleResultVo mostSchedule = matchScheduleList.stream().filter(scheduleVo -> {
                    if (isCapacityPass && scheduleVo.getTotalPlan() < MIN_PLAN_QTY) { // 超量的情况下，不处理已经小于最小值的规格
                        return false;
                    }
                    BigDecimal lineLength = (BigDecimal) scheduleVo.getParams().get(EngineConstants.ORIGINAL_LINE_LENGTH); // 只有差异量达到一卷原丝的规格才做处理
                    return !isCapacityPass || scheduleVo.getTotalPlan().doubleValue() > lineLength.doubleValue(); // 超量时，现有计划量至少保留一卷
                }).sorted((r1, r2) -> this.compareResultPlan(r1, r2, isCapacityPass))
                        .findFirst().orElse(null); // 取出总计划量最大的计划，计划量一样时取需求量最小的
                if (mostSchedule == null) {
                    break;
                }
                BigDecimal originalLineLength = (BigDecimal) mostSchedule.getParams().get(EngineConstants.ORIGINAL_LINE_LENGTH); // 原丝长度
                BigDecimal addPlan = isCapacityPass? originalLineLength.negate(): originalLineLength; // 一次处理一卷原丝的长度，正负号要与差值一致
                double dayPlanQty = mostSchedule.getDayPlanQty();
                double nightPlanQty = mostSchedule.getNightPlanQty();
                if (dayPlanQty > nightPlanQty) {
                    dayPlanQty = BigDecimalUtils.add(dayPlanQty, addPlan).doubleValue();
                } else {
                    nightPlanQty = BigDecimalUtils.add(nightPlanQty, addPlan).doubleValue();
                }
                totalPlan = BigDecimalUtils.add(totalPlan, addPlan).doubleValue();
                diffPlan = diffPlan.subtract(addPlan); // 差值扣减掉已处理的量
                mostSchedule.setDayPlanQty(dayPlanQty);
                mostSchedule.setNightPlanQty(nightPlanQty);
                mostSchedule.setTotalPlan(BigDecimalUtils.qtyAdd(mostSchedule.getTotalPlan(), addPlan));
                if (isCapacityPass ^ diffPlan.compareTo(BigDecimal.ZERO) < 0) {
                    break;
                }
                isDiff = diffPlan.compareTo(BigDecimal.ZERO) != 0;
            }
        }
    }
    
    /**
     * 比对排产计划，按供需比排
     * @param scheduleVo1
     * @param scheduleVo2
     * @param isPass    是否超产能
     * @return
     */
    private int compareResultPlan(XwyyScheduleResultVo scheduleVo1, XwyyScheduleResultVo scheduleVo2, boolean isPass) {
        // 一天消耗量
        Double cxPlanQty1 = BigDecimalUtil.add(scheduleVo1.getCxClass3Plan(), scheduleVo1.getCxClass4Plan());
        Double cxPlanQty2 = BigDecimalUtil.add(scheduleVo2.getCxClass3Plan(), scheduleVo2.getCxClass4Plan());
        // 一天生产量+库存
        Double planQty1 = BigDecimalUtil.add(scheduleVo1.getTodayStock(), scheduleVo1.getTotalPlan());
        Double planQty2 = BigDecimalUtil.add(scheduleVo2.getTodayStock(), scheduleVo2.getTotalPlan());

        // 供需比
        BigDecimal stockRate1 = BigDecimalUtils.div(planQty1, cxPlanQty1, 2); 
        BigDecimal stockRate2 = BigDecimalUtils.div(planQty2, cxPlanQty2, 2); 
        if (isPass) {
            return stockRate2.compareTo(stockRate1); // 超产能需要扣减，因此先处理比率高的，即库存多或消耗少的的（倒序）
        } else {
            return stockRate1.compareTo(stockRate2);
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
	private List<XwyyMachineInfo> searchOptionalMachineList(XwyyScheduleResultVo scheduleVo, String classCode, Map<Long, BigDecimal> capacityMap, List<XwyyMachineInfo> allMachineList, Map<String, String> specifyCanMachineMap, Map<String, String> specifyNotMachineMap) {
		String beadCode = scheduleVo.getOriginalLineCode(); // 代码
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
		List<XwyyMachineInfo> optionalMachineList = allMachineList.stream().filter(m -> {
					// 排除定点不可生产机台
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
				}).filter(m -> {
					// 如果有设置定点机台，则仅选中定点机台
					if (CollectionUtils.isNotEmpty(machineIds)) {
						return machineIds.contains(String.valueOf(m.getId()));
					}
					return true;
				}).filter(m -> StringUtils.contains(m.getOpenMachineClass(), classCode))
				// 对应班次可用
				.sorted(new Comparator<XwyyMachineInfo>() {
					// 按剩余产能升序排序
					@Override
					public int compare(XwyyMachineInfo m1, XwyyMachineInfo m2) {
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
	private void chooseMachineLog(XwyyScheduleResultVo scheduleVo, Map<String, String> specifyCanMachineMap, Map<String, String> specifyNotMachineMap) {
		StringBuffer logDetail = new StringBuffer();
		logDetail.append("①优先选择“定点机台中限制作业集合”匹配上的机台;②如果没有，在选择“口型板与机台对应关系集合”的机台信息，不过需要过滤掉'定点机台中不可作业'中的机台").append(division);
		logDetail.append("定点机台中限制作业集合：" + toJSONString(specifyCanMachineMap)).append(division);
		logDetail.append("定点机台中不可作业集合：" + toJSONString(specifyNotMachineMap)).append(division);
		logDetail.append("结果数据：" + toJSONString(scheduleVo)).append(division);
		autoScheduleLogService.insertXwyyScheduleLog(scheduleVo.getBatchNo(), scheduleVo.getOrderNo(), "设置生产线（机台）", logDetail.toString());
	}

	/**
	 * 查询所有机台信息
	 *
	 * @return 机台信息
	 */
	public List<XwyyMachineInfo> listXwyyMachine() {
		return xwyyEngineSpecifyMachineMapper.listXwyyMachine();
	}

	/**
	 * 移除原机台ID中已配置为不生产该钢带的机台ID
	 * 
	 * @Author hakimryan
	 * @Description
	 * @Date 2021-8-9 15:43:26
	 * @param notWorkMap  不生产机台映射表
	 * @param bigRollCode 需生产的大卷编号
	 * @param machineId   原机台ID
	 * @return
	 */
	private String removeNotWorkMachineId(Map<String, String> notWorkMap, String bigRollCode, String machineId) {
		String newMachineId = machineId;
		// 判断定点机台中是否有配置不生产该大卷的机台
		if (notWorkMap.containsKey(bigRollCode)) {
			// 取出不生产该大卷的机台ID
			String notMachineId = notWorkMap.get(bigRollCode);

			// 通过匹配机台ID，确认映射关系中是否存在已配置为不生产的机台ID
			String[] machineIdArr = machineId.split(",");
			String[] notMachineIdArr = notMachineId.split(",");
			// 过滤掉不生产的机台ID
			List<String> newMachineIdList = Arrays.stream(machineIdArr)
					.filter(n -> Arrays.stream(notMachineIdArr).noneMatch(x -> n.equals(x)))
					.collect(Collectors.toList());
			if (newMachineIdList.size() > 0) {
				// 如果有保留的机台ID，重新合并赋值
				newMachineId = StringUtils.join(newMachineIdList, ',');
			} else {
				// 没有保留的机台ID，则机台放空
				newMachineId = null;
			}
		}
		return newMachineId;
	}

	/**
	 * 获取指定作业类型的定点机映射关系
	 * 
	 * @Author hakimryan
	 * @Description
	 * @Date 2021-8-9 15:43:26
	 * @param jobType
	 * @return
	 */
	private Map<String, String> getXwyySpecifyMachineMap(String jobType) {
		List<XwyySpecifyMachineVo> xwyySpecifyMachineList = xwyyEngineSpecifyMachineMapper
				.selectXwyySpecifyMachineList(jobType);
		Map<String, String> xwyySpecifyMachineMap = xwyySpecifyMachineList.stream()
				.collect(Collectors.toMap(XwyySpecifyMachineVo::getBigRollCode, XwyySpecifyMachineVo::getMachineId));
		return xwyySpecifyMachineMap;
	}
}
