package com.zlt.aps.cd15.engine.service.impl;

import com.ruoyi.common.core.utils.bean.BeanUtils;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.cd15.api.domain.entity.Cd15MachineInfo;
import com.zlt.aps.cd15.engine.mapper.Cd15EngineMachineMapper;
import com.zlt.aps.cd15.engine.mapper.Cd15EngineMachineRollMappingMapper;
import com.zlt.aps.cd15.engine.mapper.Cd15EngineSpecifyMachineMapper;
import com.zlt.aps.cd15.engine.service.Cd15EngineMachineService;
import com.zlt.aps.cd15.engine.utils.Cd15EngineUtils;
import com.zlt.aps.cd15.engine.vo.Cd15MachineRollMappingVo;
import com.zlt.aps.cd15.engine.vo.Cd15ScheduleResultVo;
import com.zlt.aps.cd15.engine.vo.Cd15SpecifyMachineVo;
import com.zlt.aps.common.core.utils.BigDecimalUtil;
import com.zlt.aps.common.core.utils.BigDecimalUtils;
import com.zlt.aps.common.engine.constants.EngineConstants;
import com.zlt.aps.common.engine.enums.MachineOneOutTwo;
import com.zlt.aps.common.engine.enums.OpenMachineClassEnums;
import com.zlt.aps.common.engine.service.AutoScheduleLogService;
import com.zlt.aps.common.engine.service.impl.IncrementService;
import com.zlt.aps.common.engine.utils.CollectionUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.checkerframework.checker.units.qual.s;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.alibaba.fastjson.JSON.toJSONString;
import static com.zlt.aps.common.core.utils.ApsCommonUtil.logSplit;

/**
 * 15度裁断生产线服务实现类
 * 
 * @Description
 * @Author hakimrayn
 * @Date 2021-7-9 11:24:00
 * @Version 1.0
 */
@Service("cd15EngineMachineService")
@Slf4j
public class Cd15EngineMachineServiceImpl implements Cd15EngineMachineService {
	@Autowired
	private Cd15EngineSpecifyMachineMapper cd15EngineSpecifyMachineMapper;
	@Autowired
	private Cd15EngineMachineRollMappingMapper cd15EngineMachineRollMappingMapper;
	@Resource
	private AutoScheduleLogService autoScheduleLogService;
	@Autowired
	private Cd15EngineMachineMapper cd15EngineMachineMapper;
	@Resource
	private IncrementService incrementService;

	/**
	 * 为15度裁断排程安排生产线
	 */
	@Override
	public void scheduleMachine(List<Cd15ScheduleResultVo> scheduleList) {
		// 批次号
		String batchNo = CollectionUtil.firstElement(scheduleList).getBatchNo();
		autoScheduleLogService.insertCd15ScheduleLog(batchNo, "", "2.1、生产线安排",
				"优先查看是否有钢带编号与机台的映射配置，有则安排该生产线；无则查看是否有大卷编号与机台的映射配置，" + "以及对应钢带是否有配置在该机台不生产，符合条件则安排该生产线");
		// 抓取钢带与机台的对照关系
		// 限制作业
		Map<String, String> canWorkMap = this.getCd15SpecifyMachineMap(EngineConstants.JOB_TYPE_CAN);
		// 不可作业
		Map<String, String> notWorkMap = this.getCd15SpecifyMachineMap(EngineConstants.JOB_TYPE_NOT);

		// 抓取大卷与机台的对照关系
		List<Cd15MachineRollMappingVo> machineRollList = cd15EngineMachineRollMappingMapper
				.selectCd15MachineRollMappingList();
		Map<String, String> machineRollMap = machineRollList.stream().collect(
				Collectors.toMap(Cd15MachineRollMappingVo::getBigRollCode, Cd15MachineRollMappingVo::getMachineId));
		// 记录日志
		String logDetail = logSplit("钢带与机台的限制作业配置：" + toJSONString(canWorkMap),
				"钢带不可作业机台的配置：" + toJSONString(notWorkMap), "2.2、大卷与机台的映射关系：" + toJSONString(machineRollMap));
		autoScheduleLogService.insertCd15ScheduleLog(batchNo, "", "生产线安排基础数据日志", logDetail);

		for (Cd15ScheduleResultVo scheduleResult : scheduleList) {
			if (StringUtils.isNotBlank(scheduleResult.getMachineId())) {
				// 判断如果已经有机台了，则说明是从上一次排程复制下来的，可以直接跳过
				continue;
			}

			// 1、先看钢带编号
			String steelStripCode = scheduleResult.getSteelStripCode1();
			String machineId = null;
			if (canWorkMap.containsKey(steelStripCode)) {
				machineId = canWorkMap.get(steelStripCode);
			}
			// 2、再看大卷，只有钢带没有匹配上才需要看
			String bigRollCode = scheduleResult.getBigRollCode();
			if (machineId == null && machineRollMap.containsKey(bigRollCode)) {
				machineId = machineRollMap.get(bigRollCode);
				// 除了大卷匹配上，还要求机台沒有被设置为“不可作业”
				machineId = this.removeNotWorkMachineId(notWorkMap, steelStripCode, machineId);
			}
			scheduleResult.setMachineId(machineId);
		}
		// 记录日志
		autoScheduleLogService.insertCd15ScheduleLog(batchNo, "", "2.3、生产线安排完成",
				"安排生产线后排程记录：" + toJSONString(scheduleList));
	}

	/**
	 * 移除原机台ID中已配置为不生产该钢带的机台ID
	 * 
	 * @Author hakimryan
	 * @Description
	 * @Date 2021-8-9 15:23:26
	 * @param notWorkMap     不生产机台映射表
	 * @param steelStripCode 需生产的钢带编号
	 * @param machineId      原机台ID
	 * @return
	 */
	private String removeNotWorkMachineId(Map<String, String> notWorkMap, String steelStripCode, String machineId) {
		String newMachineId = machineId;
		// 判断定点机台中是否有配置不生产该钢带的机台
		if (notWorkMap.containsKey(steelStripCode)) {
			// 取出配置为不生产该钢带的机台ID
			String notMachineId = notWorkMap.get(steelStripCode);

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
	 * @Date 2021-8-9 15:02:12
	 * @param jobType 作业类型
	 * @return
	 */
	private Map<String, String> getCd15SpecifyMachineMap(String jobType) {
		List<Cd15SpecifyMachineVo> cd15SpecifyMachineList = cd15EngineSpecifyMachineMapper
				.selectCd15SpecifyMachineList(jobType);
		Map<String, String> cd15SpecifyMachineMap = cd15SpecifyMachineList.stream()
				.collect(Collectors.toMap(Cd15SpecifyMachineVo::getSteelStripCode, Cd15SpecifyMachineVo::getMachineId));
		return cd15SpecifyMachineMap;
	}

    /**
     * 获取机台对应钢带代码映射关系
     *
     * @param jobType 作业类型
     * @return 结果
     * @Author steve
     * @Description
     * @Date 2025-7-17
     */
    private Map<String, Set<String>> getCd15SpecifyMachineMap4Machine(String jobType) {
        List<Cd15SpecifyMachineVo> cd15SpecifyMachineList = cd15EngineSpecifyMachineMapper
                .selectCd15SpecifyMachineList(jobType);
        Map<String, Set<String>> resultMap = new HashMap<>(16);
        for (Cd15SpecifyMachineVo specifyMachineVo : cd15SpecifyMachineList) {
            String machineId = specifyMachineVo.getMachineId();
            Set<String> steelStripCodeList = resultMap.getOrDefault(machineId, new HashSet<>());
            String[] steelStripCodeArr = StringUtils.defaultIfBlank(specifyMachineVo.getSteelStripCode(), "").split(",");
            steelStripCodeList.add(Arrays.toString(steelStripCodeArr));
            resultMap.put(machineId, steelStripCodeList);
        }
        return resultMap;
    }

	/**
	 * 处理一出二机台，如果记录的机台支持一出二，则只需要计算1#钢带的计划(清空2#钢带、库存、工艺)，
	 * 如果不支持，则复制一条记录，清空1#钢带、计划量
	 *
	 * @param scheduleList 排程结果列表
	 */
	@Override
	public List<Cd15ScheduleResultVo> handleOneOutTwoMachine(List<Cd15ScheduleResultVo> scheduleList) {
		// 查询支持的一出二机台
		List<Cd15MachineInfo> notOutTwoMachineList = cd15EngineMachineMapper.selectNotOutTwoMachineList(new Cd15MachineInfo());
		List<Long> machineIdList = notOutTwoMachineList.stream().map(Cd15MachineInfo::getId).collect(Collectors.toList());
		List<Cd15ScheduleResultVo> addResultList = new ArrayList<>();
		for (Cd15ScheduleResultVo scheduleVo : scheduleList) {
		    // 是否支持分裁，如果两段钢带任意一个班的排产量不一样，都要标记为不支持
		    boolean isOutTwo = BigDecimalUtils.safeCompare(scheduleVo.getDayPlanQty1(), scheduleVo.getDayPlanQty2()) == 0
                    && BigDecimalUtils.safeCompare(scheduleVo.getNightPlanQty1(), scheduleVo.getNightPlanQty2())  == 0
                    && BigDecimalUtils.safeCompare(scheduleVo.getNextDayPlanQty(), scheduleVo.getNextDayPlanQty2())  == 0;
            Cd15ScheduleResultVo matchScheduleVo = null; // 钢带重复的计划
            if (isOutTwo) { // 如果同一个钢带存在多种的组合方式（1#钢带A可与2#钢带B组合、也可与2#钢带C组合），同样要拆分
                matchScheduleVo = scheduleList.stream()
                        .filter(s -> Objects.equals(s.getSteelStripCode1(), scheduleVo.getSteelStripCode1())
                                && !Objects.equals(s.getSteelStripCode2(), scheduleVo.getSteelStripCode2())
                                || !Objects.equals(s.getSteelStripCode1(), scheduleVo.getSteelStripCode1())
                                        && Objects.equals(s.getSteelStripCode2(), scheduleVo.getSteelStripCode2()))
                        .findAny().orElse(null);
                isOutTwo = matchScheduleVo == null;
            }
		    if (isOutTwo) { // 初始值为支持的时候才取检查机台
	            String machineId = scheduleVo.getMachineId();
	            if (StringUtils.isNotEmpty(machineId)) { // 检查分配的机台是否支持一出二
	                isOutTwo = Arrays.stream(machineId.split(",")).anyMatch(id -> machineIdList.contains(new Long(id)));
	            }
		    }
            
			if (isOutTwo) {// 支持一出二，则机台更新为该机台号
			    scheduleVo.setIsOutTwo(MachineOneOutTwo.TWO.getIndex());
			} else {
			    scheduleVo.setIsOutTwo(MachineOneOutTwo.ONE.getIndex());
			    
			    if (matchScheduleVo != null) { // 如果存在钢带重复的情况
                    // 哪一号钢带重复就把这个钢带合并至匹配计划上，另一个单独拆分出来
                    if (Objects.equals(scheduleVo.getSteelStripCode1(), matchScheduleVo.getSteelStripCode1())) {
                        // 1#重复的情况，把1#的计划量合并过去，保留2#
                        matchScheduleVo.setDayPlanQty1(BigDecimalUtil.add(matchScheduleVo.getDayPlanQty1(), scheduleVo.getDayPlanQty1()));
                        matchScheduleVo.setNightPlanQty1(BigDecimalUtil.add(matchScheduleVo.getNightPlanQty1(), scheduleVo.getNightPlanQty1()));
                        matchScheduleVo.setNextDayPlanQty(BigDecimalUtil.add(matchScheduleVo.getNextDayPlanQty(), scheduleVo.getNextDayPlanQty()));
                        // 2#计划量提到1#的位置
                        scheduleVo.setSteelStripCode1(null);
                        scheduleVo.setStock1Qty1(null);
                        scheduleVo.setCraft1(null);
                        scheduleVo.setSupplyTime1(scheduleVo.getSupplyTime2());
                        scheduleVo.setDayPlanQty1(scheduleVo.getDayPlanQty2());
                        scheduleVo.setNightPlanQty1(scheduleVo.getNightPlanQty2());
                        scheduleVo.setNextDayPlanQty(scheduleVo.getNextDayPlanQty2());
                    } else {
                        // 2#重复的情况，把2#的计划量合并过去，保留1#
                        matchScheduleVo.setDayPlanQty2(BigDecimalUtil.add(matchScheduleVo.getDayPlanQty2(), scheduleVo.getDayPlanQty2()));
                        matchScheduleVo.setNightPlanQty2(BigDecimalUtil.add(matchScheduleVo.getNightPlanQty2(), scheduleVo.getNightPlanQty2()));
                        matchScheduleVo.setNextDayPlanQty2(BigDecimalUtil.add(matchScheduleVo.getNextDayPlanQty2(), scheduleVo.getNextDayPlanQty2()));
                        // 2#计划量清0
                        scheduleVo.setSteelStripCode2(null);
                        scheduleVo.setStock1Qty2(null);
                        scheduleVo.setSupplyTime2(null);
                    }
			    } else { // 无重复钢带
	                // 复制一条记录清空1#钢带、1#库存、1#工艺
	                Cd15ScheduleResultVo newScheduleVo = new Cd15ScheduleResultVo();
	                BeanUtils.copyProperties(scheduleVo, newScheduleVo);
	                newScheduleVo.setSteelStripCode1(null);
	                newScheduleVo.setStock1Qty1(null);
	                newScheduleVo.setCraft1(null);
	                // 赋值2#钢带的成型供应时长
	                newScheduleVo.setSupplyTime1(scheduleVo.getSupplyTime2());
	                // 重新生产工单号
	                newScheduleVo.setOrderNo(incrementService.getSequence4(newScheduleVo.getBatchNo()));
	                // 排产量直接使用2段排产量
	                newScheduleVo.setDayPlanQty1(scheduleVo.getDayPlanQty2());
	                newScheduleVo.setNightPlanQty1(scheduleVo.getNightPlanQty2());
	                newScheduleVo.setNextDayPlanQty(scheduleVo.getNextDayPlanQty2());
	                addResultList.add(newScheduleVo);
	                // 1#钢带的排产记录清空2号相关的数据
	                scheduleVo.setSteelStripCode2(null);
	                scheduleVo.setStock1Qty2(null);
	                scheduleVo.setCraft2(null);
	                scheduleVo.setSupplyTime2(null);
			    }
			}
		}
		scheduleList.addAll(addResultList);
		return scheduleList;
	}

    /**
     * 根据机台产能选择机台
     * 根据规格选机台
     *
     * @param scheduleList 排程结果列表
     */
    @Override
    public void chooseMachineByCapacity(List<Cd15ScheduleResultVo> scheduleList) {
        // 根据机台产能选机台
        List<Cd15MachineInfo> allMachineList = this.listCd15Machine();
        // 各机台生产定额
        Map<String, String> specifyCanMachineMap = this.getCd15SpecifyMachineMap(EngineConstants.JOB_TYPE_CAN);
        Map<String, String> specifyNotMachineMap = this.getCd15SpecifyMachineMap(EngineConstants.JOB_TYPE_NOT);
        Map<String, Long> steelStripMachineMap = cd15EngineSpecifyMachineMapper
                .listLastDayPlanMachine(CollectionUtil.firstElement(scheduleList).getScheduleDate()).stream()
                .filter(r -> NumberUtils.isDigits(r.getMachineId()) && StringUtils.isNotEmpty(r.getSteelStripCode()))
                .collect(Collectors.toMap(Cd15SpecifyMachineVo::getSteelStripCode, r -> new Long(r.getMachineId()))); // 已排规格，初始为上一个班的规格
        // 抓取大卷与机台的对照关系
        List<Cd15MachineRollMappingVo> machineRollList = cd15EngineMachineRollMappingMapper
                .selectCd15MachineRollMappingList();
        Map<String, String> machineRollMap = machineRollList.stream().collect(
                Collectors.toMap(Cd15MachineRollMappingVo::getBigRollCode, Cd15MachineRollMappingVo::getMachineId));

        // 机台夜班已占用产能
        Map<Long, BigDecimal> midCapacityMap = scheduleList.stream()
                .filter(r -> r.getMachineId() != null) // 取出有配置定点机台的计划（支持1出2）
                .collect(Collectors.groupingBy(r -> new Long(r.getMachineId()), // 按机台分组统计已排计划量
                        Collectors.collectingAndThen(Collectors.summingDouble(Cd15ScheduleResultVo::getDayPlanQty1),
                                planQty -> BigDecimalUtils.valueOf(planQty))));
        // 机台白班已占用产能
        Map<Long, BigDecimal> nightCapacityMap = scheduleList.stream()
                .filter(r -> r.getMachineId() != null) // 取出有配置定点机台的计划（支持1出2）
                .collect(Collectors.groupingBy(r -> new Long(r.getMachineId()), // 按机台分组统计已排计划量
                        Collectors.collectingAndThen(Collectors.summingDouble(Cd15ScheduleResultVo::getNightPlanQty1),
                                planQty -> BigDecimalUtils.valueOf(planQty))));

        // 先对排产计划
        List<Cd15ScheduleResultVo> chooseMachineScheduleList = scheduleList.stream().sorted((o1, o2) -> {
            // 相同大卷的先排
            String bigRollCode1 = String.valueOf(o1.getBigRollCode());
            String bigRollCode2 = String.valueOf(o2.getBigRollCode());
            int resut = bigRollCode1.compareTo(bigRollCode2);
            if (resut != 0) {
                return resut;
            }
            // 先比较定点机台
            String specifyMachine1 = specifyCanMachineMap.get(Cd15EngineUtils.getSteelStripCode(o1));
            String specifyMachine2 = specifyCanMachineMap.get(Cd15EngineUtils.getSteelStripCode(o2));
            resut = this.compareSpecifyMachine(specifyMachine1, specifyMachine2);
            if (resut != 0) {
                return resut;
            }
            // 比较大卷映射机台
            String rollMachine1 = machineRollMap.get(o1.getBigRollCode());
            String rollMachine2 = machineRollMap.get(o2.getBigRollCode());
            resut = this.compareSpecifyMachine(rollMachine1, rollMachine2);
            if (resut != 0) {
                return resut;
            }
            // 如果机台设置都一样，则按计划量从大到小
            BigDecimal planQty1 = BigDecimalUtils.add(o1.getDayPlanQty1(), o1.getNightPlanQty1());
            BigDecimal planQty2 = BigDecimalUtils.add(o2.getDayPlanQty1(), o2.getNightPlanQty1());
            return planQty2.compareTo(planQty1);
        }).collect(Collectors.toList());

        // 班次大卷对应机台Map
        Map<String, Cd15MachineInfo> midBigRollMachineMap = new HashMap<>(16);
        Map<String, Cd15MachineInfo> nightBigRollMachineMap = new HashMap<>(16);
        // 根据夜班计划分配机台
        for (Cd15ScheduleResultVo scheduleVo : chooseMachineScheduleList) {
            if (StringUtils.isNotEmpty(scheduleVo.getMachineId())) { // 如果已经有分配机台id，则说明是一出二机台，清空即可
                scheduleVo.setMachineId(null);
            }
            Double midPlanQty = scheduleVo.getDayPlanQty1();
            if (midPlanQty == null || midPlanQty <= 0) {
                continue;
            }
            // 夜班
            String classCode = String.valueOf(OpenMachineClassEnums.CLASS_TWO.getClassIndex());
            double craft1 = Double.parseDouble(StringUtils.defaultIfBlank(scheduleVo.getCraft1(), "0"));
            double craft2 = Double.parseDouble(StringUtils.defaultIfBlank(scheduleVo.getCraft2(), "0"));
            // 检索当班可选机台
            List<Cd15MachineInfo> optionalMachineList = this.searchOptionalMachineList(scheduleVo, classCode, midCapacityMap,
                    allMachineList, specifyCanMachineMap, specifyNotMachineMap, machineRollMap, steelStripMachineMap, midPlanQty, craft1, craft2);
            if (CollectionUtil.isEmpty(optionalMachineList)) {
                continue;
            }
            String bigRollCode = scheduleVo.getBigRollCode();
            // 如果有匹配机台，则直接取第一个机台赋值
            Cd15MachineInfo machine = CollectionUtil.firstElement(optionalMachineList);
            /*
            // 大卷对应机台选择逻辑
            if (optionalMachineList.size() > 1 && midBigRollMachineMap.containsKey(bigRollCode)) {
                // 如果胶料已有机台，优先用对应机台
                machine = midBigRollMachineMap.get(bigRollCode);
                // 如果机台产能加计划超过定额，才选其他的机台
                if (midCapacityMap.containsKey(machine.getId()) && machine.getQuata() != null
                        && midCapacityMap.get(machine.getId()).add(BigDecimal.valueOf(midPlanQty))
                        .compareTo(machine.getQuata()) > 0) {
                    machine = CollectionUtil.firstElement(optionalMachineList);
                    if (!midBigRollMachineMap.containsKey(bigRollCode)) {
                        midBigRollMachineMap.put(bigRollCode, machine);
                    }
                }
            } else {
                machine = CollectionUtil.firstElement(optionalMachineList);
                if (!midBigRollMachineMap.containsKey(bigRollCode)) {
                    midBigRollMachineMap.put(bigRollCode, machine);
                }
            }*/
            Long machineId = machine.getId();
            scheduleVo.setMachineId(String.valueOf(machineId));
            //检查机台，如果早班不作业，则把计划量都转移到夜班
            if (!machine.getOpenMachineClass().contains(String.valueOf(OpenMachineClassEnums.CLASS_THREE.getClassIndex()))) {
                scheduleVo.setDayPlanQty1(BigDecimalUtil.add(midPlanQty, scheduleVo.getNightPlanQty1()));
                scheduleVo.setNightPlanQty1(0D);
            }
            // 占用机台各班产能
            BigDecimal dayPlanQty = BigDecimalUtils.valueOf(scheduleVo.getDayPlanQty1());
            BigDecimal nightPlanQty = BigDecimalUtils.valueOf(scheduleVo.getNightPlanQty1());
            boolean isOutTwo = MachineOneOutTwo.TWO.getIndex().equals(machine.getIsOutTwo()); // 一出二的情况下，要占用双倍的产能
            dayPlanQty = isOutTwo? dayPlanQty.multiply(BigDecimalUtils.TWO): dayPlanQty;
            nightPlanQty = isOutTwo? nightPlanQty.multiply(BigDecimalUtils.TWO): nightPlanQty;

            midCapacityMap.put(machineId, midCapacityMap.getOrDefault(machineId, BigDecimal.ZERO).add(dayPlanQty));
            nightCapacityMap.put(machineId, nightCapacityMap.getOrDefault(machineId, BigDecimal.ZERO).add(nightPlanQty));
        }

        // 剩余没有分配到机台的排程检查早班是否有可分配机台
        for (Cd15ScheduleResultVo scheduleVo : chooseMachineScheduleList) {
            if (StringUtils.isNotEmpty(scheduleVo.getMachineId())) {
                continue;
            }
            double craft1 = Double.parseDouble(StringUtils.defaultIfBlank(scheduleVo.getCraft1(), "0"));
            double craft2 = Double.parseDouble(StringUtils.defaultIfBlank(scheduleVo.getCraft2(), "0"));
            // 早班
            String classCode = String.valueOf(OpenMachineClassEnums.CLASS_THREE.getClassIndex());
            // 检索当班可选机台
            List<Cd15MachineInfo> optionalMachineList = this.searchOptionalMachineList(scheduleVo, classCode, nightCapacityMap,
                    allMachineList, specifyCanMachineMap, specifyNotMachineMap, machineRollMap, steelStripMachineMap, scheduleVo.getNightPlanQty1(), craft1, craft2);
            if (CollectionUtil.isEmpty(optionalMachineList)) {
                continue;
            }
            String bigRollCode = scheduleVo.getBigRollCode();
            // 如果有匹配机台，则直接取第一个机台赋值
            Cd15MachineInfo machine = CollectionUtil.firstElement(optionalMachineList);
            /*
            // 大卷对应机台选择逻辑
            if (optionalMachineList.size() > 1 && nightBigRollMachineMap.containsKey(bigRollCode)) {
                // 如果胶料已有机台，优先用对应机台
                machine = nightBigRollMachineMap.get(bigRollCode);
                // 如果机台产能加计划超过定额，才选其他的机台
                if (nightCapacityMap.containsKey(machine.getId()) && machine.getQuata() != null
                        && nightCapacityMap.get(machine.getId()).add(BigDecimal.valueOf(scheduleVo.getNightPlanQty1()))
                        .compareTo(machine.getQuata()) > 0) {
                    machine = CollectionUtil.firstElement(optionalMachineList);
                    if (!nightBigRollMachineMap.containsKey(bigRollCode)) {
                        nightBigRollMachineMap.put(bigRollCode, machine);
                    }
                }
            } else {
                machine = CollectionUtil.firstElement(optionalMachineList);
                if (!nightBigRollMachineMap.containsKey(bigRollCode)) {
                    nightBigRollMachineMap.put(bigRollCode, machine);
                }
            }*/
            Long machineId = machine.getId();
            scheduleVo.setMachineId(String.valueOf(machineId));
            //检查机台，如果夜班不作业，则把计划量都转移到早班
            if (!machine.getOpenMachineClass().contains(String.valueOf(OpenMachineClassEnums.CLASS_TWO.getClassIndex()))) {
                scheduleVo.setNightPlanQty1(BigDecimalUtil.add(scheduleVo.getDayPlanQty1(), scheduleVo.getNightPlanQty1()));
                scheduleVo.setDayPlanQty1(0D);
            }
            // 占用机台各班产能
            BigDecimal dayPlanQty = BigDecimalUtils.valueOf(scheduleVo.getDayPlanQty1());
            BigDecimal nightPlanQty = BigDecimalUtils.valueOf(scheduleVo.getNightPlanQty1());
            boolean isOutTwo = MachineOneOutTwo.TWO.getIndex().equals(machine.getIsOutTwo()); // 一出二的情况下，要占用双倍的产能
            dayPlanQty = isOutTwo? dayPlanQty.multiply(BigDecimalUtils.TWO): dayPlanQty;
            nightPlanQty = isOutTwo? nightPlanQty.multiply(BigDecimalUtils.TWO): nightPlanQty;
            
            midCapacityMap.put(machineId, midCapacityMap.getOrDefault(machineId, BigDecimal.ZERO).add(dayPlanQty));
            nightCapacityMap.put(machineId, nightCapacityMap.getOrDefault(machineId, BigDecimal.ZERO).add(nightPlanQty));
        }
    }

    /**
     * 根据机台产能选机台
     * 根据机台选规格
     *
     * @param scheduleList 排程结果
     */
    @Override
    public void chooseMachineByCapacity4Machine(List<Cd15ScheduleResultVo> scheduleList) {
        // 根据机台产能选机台
        List<Cd15MachineInfo> allMachineList = this.listCd15Machine();
        Map<Long, Cd15MachineInfo> machineInfoMap = new HashMap<>(16);
        if (CollectionUtils.isNotEmpty(allMachineList)) {
            machineInfoMap = allMachineList.stream().collect(Collectors.toMap(Cd15MachineInfo::getId, Function.identity()));
        }
        // 各机台生产定额
        Map<String, String> specifyCanMachineMap = this.getCd15SpecifyMachineMap(EngineConstants.JOB_TYPE_CAN);
        Map<String, Set<String>> specifyCanMachine4MachineMap = this.getCd15SpecifyMachineMap4Machine(EngineConstants.JOB_TYPE_CAN);
        Map<String, String> specifyNotMachineMap = this.getCd15SpecifyMachineMap(EngineConstants.JOB_TYPE_NOT);
        Map<String, Long> steelStripMachineMap = cd15EngineSpecifyMachineMapper
                .listLastDayPlanMachine(CollectionUtil.firstElement(scheduleList).getScheduleDate()).stream()
                .filter(r -> NumberUtils.isDigits(r.getMachineId()) && StringUtils.isNotEmpty(r.getSteelStripCode()))
                .collect(Collectors.toMap(Cd15SpecifyMachineVo::getSteelStripCode, r -> new Long(r.getMachineId()))); // 已排规格，初始为上一个班的规格
        // 抓取大卷与机台的对照关系
        List<Cd15MachineRollMappingVo> machineRollList = cd15EngineMachineRollMappingMapper
                .selectCd15MachineRollMappingList();
        Map<String, String> machineRollMap = machineRollList.stream().collect(
                Collectors.toMap(Cd15MachineRollMappingVo::getBigRollCode, Cd15MachineRollMappingVo::getMachineId));

        Map<Long, BigDecimal> midCapacityMap = new HashMap<>(); // 机台夜班已占用产能
        Map<Long, BigDecimal> nightCapacityMap = new HashMap<>(); // 机台白班已占用产能
        Map<Long, BigDecimal> nextDayCapacityMap = new HashMap<>(); // 机台次日夜班已占用产能

        // 先对排产计划
        List<Cd15ScheduleResultVo> chooseMachineScheduleList = scheduleList.stream().sorted((o1, o2) -> {
            // 相同大卷的先排
            String bigRollCode1 = String.valueOf(o1.getBigRollCode());
            String bigRollCode2 = String.valueOf(o2.getBigRollCode());
            int resut = bigRollCode1.compareTo(bigRollCode2);
            if (resut != 0) {
                return resut;
            }
            // 先比较定点机台
            String specifyMachine1 = specifyCanMachineMap.get(Cd15EngineUtils.getSteelStripCode(o1));
            String specifyMachine2 = specifyCanMachineMap.get(Cd15EngineUtils.getSteelStripCode(o2));
            resut = this.compareSpecifyMachine(specifyMachine1, specifyMachine2);
            if (resut != 0) {
                return resut;
            }
            // 比较大卷映射机台
            String rollMachine1 = machineRollMap.get(o1.getBigRollCode());
            String rollMachine2 = machineRollMap.get(o2.getBigRollCode());
            resut = this.compareSpecifyMachine(rollMachine1, rollMachine2);
            if (resut != 0) {
                return resut;
            }
            // 如果机台设置都一样，则按计划量从大到小
            BigDecimal planQty1 = BigDecimalUtils.add(o1.getDayPlanQty1(), o1.getNightPlanQty1());
            BigDecimal planQty2 = BigDecimalUtils.add(o2.getDayPlanQty1(), o2.getNightPlanQty1());
            return planQty2.compareTo(planQty1);
        }).collect(Collectors.toList());

        // 班次大卷对应机台Map
        Map<String, List<String>> midBigRollMachineMap = new HashMap<>(16);
        Map<String, List<String>> nightBigRollMachineMap = new HashMap<>(16);
        Map<String, List<String>> dayBigRollMachineMap = new HashMap<>(16);

        // 定点机台对应的大卷先匹配
//        for (Cd15ScheduleResultVo scheduleResultVo : chooseMachineScheduleList) {
//            String steelStripCode = Cd15EngineUtils.getSteelStripCode(scheduleResultVo);
//            String machineId = specifyCanMachineMap.get(steelStripCode);
//            if (specifyCanMachineMap.containsKey(steelStripCode)) {
//                midBigRollMachineMap.put(scheduleResultVo.getBigRollCode(), machineInfoMap.get(Long.valueOf(machineId)));
//                nightBigRollMachineMap.put(scheduleResultVo.getBigRollCode(), machineInfoMap.get(Long.valueOf(machineId)));
//            }
//        }

        // 按机台优先级排
        allMachineList = allMachineList.stream()
                .sorted(Comparator.comparing(m -> String.valueOf(m.getRemark())))
                .collect(Collectors.toList());

        // 如果定点机台没匹配上
        for (Cd15MachineInfo machineInfo : allMachineList) {
            Long machineId = machineInfo.getId();
            String machineIdStr = String.valueOf(machineInfo.getId());
            Double steelStripWidth = machineInfo.getSteelStripWidth();
//            boolean isOutTwo = MachineOneOutTwo.TWO.getIndex().equals(machineInfo.getIsOutTwo());

            Set<String> steelStripCodeList = specifyCanMachine4MachineMap.getOrDefault(machineIdStr, new HashSet<>());

            List<Cd15ScheduleResultVo> machineNullScheduleList = chooseMachineScheduleList.stream()
                    .filter(item -> item.getMachineId() == null)
                    .filter(item -> CollectionUtils.isEmpty(steelStripCodeList) || steelStripCodeList.contains(Cd15EngineUtils.getSteelStripCode(item)))
                    .filter(item -> {
                        // 排除定点不可生产机台
                        String notMachine = specifyNotMachineMap.getOrDefault(Cd15EngineUtils.getSteelStripCode(item), "");
                        if (StringUtils.isEmpty(notMachine)) {
                            return true;
                        }
                        List<String> notMachineIds = Arrays.asList(notMachine.split(","));
                        return !notMachineIds.contains(machineIdStr);
                    })
                    .filter(item -> {
                        // 如果有配置了映射关系，则只要包含该机台即可
                        String bigRollCode = item.getBigRollCode();
                        if (machineRollMap.containsKey(bigRollCode)) {
                            String[] matchIds = machineRollMap.get(bigRollCode).split(",");
                            return Arrays.asList(matchIds).contains(machineIdStr);
                        }
                        return true;
                    })
                    .filter(item -> {
                        double craft1 = Double.parseDouble(StringUtils.defaultIfBlank(item.getCraft1(), "0"));
                        double craft2 = Double.parseDouble(StringUtils.defaultIfBlank(item.getCraft2(), "0"));
                        double craftSum = craft1 + craft2;
                        double craftMax = Math.max(craft1, craft2);
                        if (MachineOneOutTwo.TWO.getIndex().equals(machineInfo.getIsOutTwo())) {
                            // 如果是一出二，机台宽度大于等于两个工艺的和
                            return steelStripWidth >= craftSum;
                        } else if (MachineOneOutTwo.ONE.getIndex().equals(machineInfo.getIsOutTwo())) {
                            // 如果是一出一，机台宽度大于等于两个工艺的最大值
                            return steelStripWidth >= craftMax;
                        }
                        return false;
                    })
                    .collect(Collectors.toList());

            BigDecimal quata = machineInfo.getQuata();
            // 夜班：符合条件的赋值机台，占用产能，如果产能不够，就跳过当前机台，切换至下一机台
            for (Cd15ScheduleResultVo scheduleVo : machineNullScheduleList) {
                if (scheduleVo.getDayPlanQty1() == null || scheduleVo.getDayPlanQty1() <= 0) {
                    continue;
                }
                BigDecimal midCapacity = midCapacityMap.getOrDefault(machineId, BigDecimal.ZERO);
                // 判断产能是否足够
                BigDecimal dayPlanQty = BigDecimalUtils.add(scheduleVo.getDayPlanQty1(), scheduleVo.getDayPlanQty2());
                BigDecimal nightPlanQty = BigDecimalUtils.add(scheduleVo.getNightPlanQty1(), scheduleVo.getNightPlanQty2());
                BigDecimal nextDayPlanQty = BigDecimalUtils.add(scheduleVo.getNextDayPlanQty(), scheduleVo.getNextDayPlanQty2());
                if (midCapacity.doubleValue() + dayPlanQty.doubleValue() > quata.doubleValue()) {
                    continue;
                }

                midCapacityMap.put(machineId, midCapacityMap.getOrDefault(machineId, BigDecimal.ZERO).add(dayPlanQty));
                nightCapacityMap.put(machineId, nightCapacityMap.getOrDefault(machineId, BigDecimal.ZERO).add(nightPlanQty));
                nextDayCapacityMap.put(machineId, nextDayCapacityMap.getOrDefault(machineId, BigDecimal.ZERO).add(nextDayPlanQty));
                scheduleVo.setMachineId(machineIdStr);
                List<String> machineList = midBigRollMachineMap.get(scheduleVo.getBigRollCode());
                if (CollectionUtils.isEmpty(machineList)) {
                    machineList = new ArrayList<>();
                    midBigRollMachineMap.put(scheduleVo.getBigRollCode(), machineList);
                }
                machineList.add(String.valueOf(machineId));
            }

            // 早班：符合条件的赋值机台，占用产能，如果产能不够，就跳过当前机台，切换至下一机台
            for (Cd15ScheduleResultVo scheduleVo : machineNullScheduleList) {
                // 已经有机台的跳过
                if (scheduleVo.getMachineId() != null) {
                    continue;
                }
                if (scheduleVo.getDayPlanQty1() > 0) { // 夜班有量但是又没有机台，说明本机台的夜班产能已满，跳过
                    continue;
                }
                if (scheduleVo.getNightPlanQty1() == null || scheduleVo.getNightPlanQty1() <= 0) {
                    continue;
                }
                BigDecimal nightCapacity = nightCapacityMap.getOrDefault(machineId, BigDecimal.ZERO);
                // 判断产能是否足够
                BigDecimal dayPlanQty = BigDecimalUtils.add(scheduleVo.getDayPlanQty1(), scheduleVo.getDayPlanQty2());
                BigDecimal nightPlanQty = BigDecimalUtils.add(scheduleVo.getNightPlanQty1(), scheduleVo.getNightPlanQty2());
                BigDecimal nextDayPlanQty = BigDecimalUtils.add(scheduleVo.getNextDayPlanQty(), scheduleVo.getNextDayPlanQty2());
                if (nightCapacity.add(nightPlanQty).doubleValue() > quata.doubleValue()) {
                    continue;
                }

                midCapacityMap.put(machineId, midCapacityMap.getOrDefault(machineId, BigDecimal.ZERO).add(dayPlanQty));
                nightCapacityMap.put(machineId, nightCapacityMap.getOrDefault(machineId, BigDecimal.ZERO).add(nightPlanQty));
                nextDayCapacityMap.put(machineId, nextDayCapacityMap.getOrDefault(machineId, BigDecimal.ZERO).add(nextDayPlanQty));
                scheduleVo.setMachineId(machineIdStr);
                List<String> machineList = nightBigRollMachineMap.get(scheduleVo.getBigRollCode());
                if (CollectionUtils.isEmpty(machineList)) {
                    machineList = new ArrayList<>();
                    nightBigRollMachineMap.put(scheduleVo.getBigRollCode(), machineList);
                }
                machineList.add(String.valueOf(machineId));
            }

            // 次日夜班：符合条件的赋值机台，占用产能，如果产能不够，就跳过当前机台，切换至下一机台
            for (Cd15ScheduleResultVo scheduleVo : machineNullScheduleList) {
                // 已经有机台的跳过
                if (scheduleVo.getMachineId() != null) {
                    continue;
                }
                if (scheduleVo.getDayPlanQty1() > 0 || scheduleVo.getNightPlanQty1() > 0) { // 夜早班有量但是又没有机台，说明本机台的夜早班产能已满，跳过
                    continue;
                }
                if (scheduleVo.getNextDayPlanQty() == null || scheduleVo.getNextDayPlanQty() <= 0) {
                    continue;
                }
                BigDecimal nextDayCapacity = nextDayCapacityMap.getOrDefault(machineId, BigDecimal.ZERO);
                // 判断产能是否足够
                BigDecimal dayPlanQty = BigDecimalUtils.add(scheduleVo.getDayPlanQty1(), scheduleVo.getDayPlanQty2());
                BigDecimal nightPlanQty = BigDecimalUtils.add(scheduleVo.getNightPlanQty1(), scheduleVo.getNightPlanQty2());
                BigDecimal nextDayPlanQty = BigDecimalUtils.add(scheduleVo.getNextDayPlanQty(), scheduleVo.getNextDayPlanQty2());
                if (nextDayCapacity.add(nextDayPlanQty).doubleValue() > quata.doubleValue()) {
                    continue;
                }

                midCapacityMap.put(machineId, midCapacityMap.getOrDefault(machineId, BigDecimal.ZERO).add(dayPlanQty));
                nightCapacityMap.put(machineId, nightCapacityMap.getOrDefault(machineId, BigDecimal.ZERO).add(nightPlanQty));
                nextDayCapacityMap.put(machineId, nextDayCapacityMap.getOrDefault(machineId, BigDecimal.ZERO).add(nextDayPlanQty));
                scheduleVo.setMachineId(machineIdStr);
                List<String> machineList = dayBigRollMachineMap.get(scheduleVo.getBigRollCode());
                if (CollectionUtils.isEmpty(machineList)) {
                    machineList = new ArrayList<>();
                    dayBigRollMachineMap.put(scheduleVo.getBigRollCode(), machineList);
                }
                machineList.add(String.valueOf(machineId));
            }
        }
        
        // 最后没有匹配上机台的，直接强制安排到同大卷的机台上
        scheduleList.stream().filter(s -> StringUtils.isEmpty(s.getMachineId())).forEach(s -> {
            String bigRollCode = s.getBigRollCode();
            List<String> machineList = midBigRollMachineMap.get(bigRollCode);
            if (CollectionUtils.isNotEmpty(machineList)) {
                s.setMachineId(CollectionUtil.firstElement(machineList));
                return;
            }
            machineList = nightBigRollMachineMap.get(bigRollCode);
            if (CollectionUtils.isNotEmpty(machineList)) {
                s.setMachineId(CollectionUtil.firstElement(machineList));
                return;
            }
            machineList = dayBigRollMachineMap.get(bigRollCode);
            if (CollectionUtils.isNotEmpty(machineList)) {
                s.setMachineId(CollectionUtil.firstElement(machineList));
                return;
            }
        });
        
        log.debug("夜班产能map：{}", midCapacityMap);
        log.debug("早班产能map：{}", nightCapacityMap);
        log.debug("次日夜班产能map：{}", nextDayCapacityMap);
    }
    
    /**
     * 查询斜裁度裁断机台列表
     *
     * @return 结果
     */
    private List<Cd15MachineInfo> listCd15Machine() {
        return cd15EngineSpecifyMachineMapper.listCd15Machine();
    }


    /**
     * 比较特殊机台
     * @param machineCode1
     * @param machineCode2
     * @return
     */
    private Integer compareSpecifyMachine(String machineCode1, String machineCode2) {
        boolean isSpecify1 = StringUtils.isNotEmpty(machineCode1);
        boolean isSpecify2 = StringUtils.isNotEmpty(machineCode2);
        if (isSpecify1 ^ isSpecify2) { // 先看哪个配置有特定机台，有配置特定机台的优先选机台
            Integer flag1 = isSpecify1? 1: 2;
            Integer flag2 = isSpecify2? 1: 2;
            return flag1.compareTo(flag2);
        }
        // 如果都有配置特定机台，则先看可选机台较少的
        if (isSpecify1 && isSpecify2) {
            Integer machineNum1 = machineCode1.split(",").length;
            Integer machineNum2 = machineCode2.split(",").length;
            return machineNum1.compareTo(machineNum2);
        }
        return 0;
    }
    /**
     * 选择排程对应机台列表
     *
     * @param scheduleVo           排程
     * @param classCode            班制
     * @param capacityMap          机台产已占能map
     * @param allMachineList       所有机台
     * @param specifyCanMachineMap 定点机台
     * @param specifyNotMachineMap 不可作业机台
     * @param machineRollMap       大卷机台映射表
     * @param steelStripMachineMap 钢带已排机台映射表
     * @param planQty              计划量
     * @return 机台列表
     */
    private List<Cd15MachineInfo> searchOptionalMachineList(Cd15ScheduleResultVo scheduleVo, String classCode,
            Map<Long, BigDecimal> capacityMap, List<Cd15MachineInfo> allMachineList,
            Map<String, String> specifyCanMachineMap, Map<String, String> specifyNotMachineMap,
                                                            Map<String, String> machineRollMap, Map<String, Long> steelStripMachineMap, Double planQty,
                                                            double craft1, double craft2) {
        String steelStripCode = Cd15EngineUtils.getSteelStripCode(scheduleVo); // 帘布代码
        String bigRollCode = scheduleVo.getBigRollCode(); // 大卷代码
        BigDecimal schedulePlanQty = BigDecimalUtils.valueOf(planQty);
        // 定点机台ID列表
        String specifyMachineIds = specifyCanMachineMap.get(steelStripCode);
        List<String> machineIds;
        // 如果有设置定点机台，需要把非定点全部过滤掉
        if (StringUtils.isNotEmpty(specifyMachineIds)) {
            machineIds = Arrays.asList(specifyMachineIds.split(","));
        } else {
            machineIds = new ArrayList<>(0);
        }
//        double craftSum = craft1 + craft2;
//        double craftMax = Math.max(craft1, craft2);
        // 可选机台
        List<Cd15MachineInfo> optionalMachineList = allMachineList.stream().filter(m -> {
                    // 排除定点不可生产机台
                    String machineId = String.valueOf(m.getId());
                    String notMachine = specifyNotMachineMap.get(steelStripCode);
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
                    if (org.apache.commons.collections.CollectionUtils.isNotEmpty(machineIds)) {
                        return machineIds.contains(String.valueOf(m.getId()));
                    }
                    return true;
                }).filter(m -> {
                    // 如果有配置了映射关系，则只要包含该机台即可
                    String machineId = String.valueOf(m.getId());
                    if (machineRollMap.containsKey(bigRollCode)) {
                        String[] matchIds = machineRollMap.get(bigRollCode).split(",");
                        return Arrays.stream(matchIds).anyMatch(id -> Objects.equals(machineId, id));
                    }
                    return true;
                })
//                .filter(m -> Objects.equals(m.getIsOutTwo(), scheduleVo.getIsOutTwo())) // 一出二模式相同
                .filter(m -> StringUtils.contains(m.getOpenMachineClass(), classCode))// 对应班次可用
                /*.filter(m -> {
                    if (MachineOneOutTwo.TWO.getIndex().equals(m.getIsOutTwo())) {
                        // 如果是一出二，机台宽度大于等于两个工艺的和
                        return m.getSteelStripWidth() >= craftSum;
                    } else if (MachineOneOutTwo.ONE.getIndex().equals(m.getIsOutTwo())) {
                        // 如果是一出一，机台宽度大于等于两个工艺的最大值
                        return m.getSteelStripWidth() >= craftMax;
                    }
                    return false;
                })*/
//                .filter(m -> {
//                    if (schedulePlanQty.compareTo(BigDecimal.ZERO) <= 0) {
//                        return true; // 如果没有计划，则不限制机台
//                    }
//                    BigDecimal capacity = capacityMap.getOrDefault(m.getId(), BigDecimal.ZERO); // 已占用产能
//                    BigDecimal quata = BigDecimalUtils.valueOf(m.getQuata()); // 班产定额
//                    return schedulePlanQty.add(capacity).compareTo(quata) <= 0; // 计划 + 已占产能 <= 定额，则允许排产
//                }) // 剩余产能足够
                .sorted(new Comparator<Cd15MachineInfo>() {
                    // 按剩余产能升序排序
                    @Override
                    public int compare(Cd15MachineInfo m1, Cd15MachineInfo m2) {
                        // 同一个规格优先排在已排过相同规格的机台上
                        Long scheduleMachineId = steelStripMachineMap.getOrDefault(steelStripCode, 0L);
                        Integer hasMachine1 = m1.getId().equals(scheduleMachineId) ? 0 : 1;
                        Integer hasMachine2 = m2.getId().equals(scheduleMachineId) ? 0 : 1;
                        int result = hasMachine1.compareTo(hasMachine2);
                        if (result != 0) {
                            return result;
                        }
                        // 如果已经达到定额，则优先级往后
                        BigDecimal capacity1 = capacityMap.getOrDefault(m1.getId(), BigDecimal.ZERO);
                        BigDecimal capacity2 = capacityMap.getOrDefault(m2.getId(), BigDecimal.ZERO);
                        Integer passCapacity1 = schedulePlanQty.add(capacity1).compareTo(BigDecimalUtils.valueOf(m1.getQuata())) <= 0? 0: 1; // 已占产能是否超过定额，>0超过
                        Integer passCapacity2 = schedulePlanQty.add(capacity2).compareTo(BigDecimalUtils.valueOf(m2.getQuata())) <= 0? 0: 1; // 已占产能是否超过定额，>0超过
                        result = passCapacity1.compareTo(passCapacity2);
                        if (result != 0) {
                            return result;
                        }
                        // 按已占产能升序排序（已占产能多的优先，先把一个机台沾满再排下一个）
                        result = capacity1.compareTo(capacity2);
                        if (result != 0) {
                            return result;
                        }
                        result = String.valueOf(m1.getRemark()).compareTo(String.valueOf(m2.getRemark()));
                        return result;
                    }
                }).collect(Collectors.toList());
        return optionalMachineList;
    }
}
