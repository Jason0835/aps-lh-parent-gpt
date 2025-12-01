package com.zlt.aps.cd90.engine.service.impl;

import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.cd90.api.domain.entity.Cd90MachineInfo;
import com.zlt.aps.cd90.engine.mapper.Cd90EngineMachineRollMappingMapper;
import com.zlt.aps.cd90.engine.mapper.Cd90EngineSpecifyMachineMapper;
import com.zlt.aps.cd90.engine.service.Cd90EngineMachineService;
import com.zlt.aps.cd90.engine.vo.Cd90MachineRollMappingVo;
import com.zlt.aps.cd90.engine.vo.Cd90ScheduleResultVo;
import com.zlt.aps.cd90.engine.vo.Cd90SpecifyMachineVo;
import com.zlt.aps.common.core.utils.BigDecimalUtil;
import com.zlt.aps.common.core.utils.BigDecimalUtils;
import com.zlt.aps.common.engine.constants.EngineConstants;
import com.zlt.aps.common.engine.enums.OpenMachineClassEnums;
import com.zlt.aps.common.engine.service.AutoScheduleLogService;
import com.zlt.aps.common.engine.utils.CollectionUtil;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import static com.alibaba.fastjson.JSON.toJSONString;
import static com.zlt.aps.common.core.utils.ApsCommonUtil.logSplit;

/**
 * 90度裁断生产线服务实现类
 * 
 * @Description
 * @Author hakimrayn
 * @Date 2021-7-14 11:24:00
 * @Version 1.0
 */
@Service("cd90EngineMachineService")
public class Cd90EngineMachineServiceImpl implements Cd90EngineMachineService {
	@Autowired
	private Cd90EngineSpecifyMachineMapper cd90EngineSpecifyMachineMapper;
	@Autowired
	private Cd90EngineMachineRollMappingMapper cd90EngineMachineRollMappingMapper;
	@Resource
	private AutoScheduleLogService autoScheduleLogService;

	/**
	 * 日志分割符
	 */
	private String division = "\r\n---------------------------------------------------\r\n";

	/**
	 * 为90度裁断排程安排生产线
	 * 
	 * @Author hakimryan
	 * @Description
	 * @Date 2021-7-14 11:29:58
	 * @param scheduleList 90度裁断排产
	 */
	@Override
	public void scheduleMachine(List<Cd90ScheduleResultVo> scheduleList) {
		// 批次号
		String batchNo = CollectionUtil.firstElement(scheduleList).getBatchNo();
		autoScheduleLogService.insertCd90ScheduleLog(batchNo, "", "2.1、生产线安排",
				"优先查看是否有帘线编号与机台的映射配置，有则安排该生产线；无则查看是否有大卷编号与机台的映射配置，以及对应钢带是否有配置在该机台不生产，符合条件则安排该生产线");
		// 抓取帘布与机台的对照关系
		// 限制作业
		Map<String, String> canWorkMap = this.getCd90SpecifyMachineMap(EngineConstants.JOB_TYPE_CAN);
		// 不可作业
		Map<String, String> notWorkMap = this.getCd90SpecifyMachineMap(EngineConstants.JOB_TYPE_NOT);

		// 抓取大卷与机台的对照关系
		List<Cd90MachineRollMappingVo> machineRollList = cd90EngineMachineRollMappingMapper
				.selectCd90MachineRollMappingList();
		Map<String, String> machineRollMap = machineRollList.stream().collect(
				Collectors.toMap(Cd90MachineRollMappingVo::getBigRollCode, Cd90MachineRollMappingVo::getMachineId));
		// 记录日志
		String logDetail = logSplit("帘布与机台的限制作业配置：" + toJSONString(canWorkMap),
				"帘布不可作业机台的配置：" + toJSONString(notWorkMap), "大卷与机台的映射关系：" + toJSONString(machineRollMap));
		autoScheduleLogService.insertCd90ScheduleLog(batchNo, "", "2.2、生产线安排基础数据日志", logDetail);

		for (Cd90ScheduleResultVo scheduleResult : scheduleList) {
			if (StringUtils.isNotBlank(scheduleResult.getMachineId())) {
				// 判断如果已经有机台了，则说明是从上一次排程复制下来的，可以直接跳过
				continue;
			}

			// 1、先看帘布编号
			String clothCode = scheduleResult.getClothCode();
			String machineId = null;
			if (canWorkMap.containsKey(clothCode)) {
				machineId = canWorkMap.get(clothCode);
			}
			// 2、再看大卷，只有帘布没有匹配上才需要看
			String bigRollCode = scheduleResult.getBigRollCode();
			if (machineId == null && machineRollMap.containsKey(bigRollCode)) {
				machineId = machineRollMap.get(bigRollCode);
				// 除了大卷匹配上，还要求机台沒有被设置为“不可作业”
				machineId = this.removeNotWorkMachineId(notWorkMap, clothCode, machineId);
			}
			scheduleResult.setMachineId(machineId);
		}
		// 记录日志
		autoScheduleLogService.insertCd90ScheduleLog(batchNo, "", "2.3、生产线安排完成", "安排生产线后排程记录：" + toJSONString(scheduleList));
	}

	/**
	 * 移除原机台ID中已配置为不生产该钢带的机台ID
	 * 
	 * @Author hakimryan
	 * @Description
	 * @Date 2021-8-9 15:43:26
	 * @param notWorkMap 不生产机台映射表
	 * @param clothCode  需生产的帘布编号
	 * @param machineId  原机台ID
	 * @return
	 */
	private String removeNotWorkMachineId(Map<String, String> notWorkMap, String clothCode, String machineId) {
		String newMachineId = machineId;
		// 判断定点机台中是否有配置不生产该帘布的机台
		if (notWorkMap.containsKey(clothCode)) {
			// 取出不生产该帘布的机台ID
			String notMachineId = notWorkMap.get(clothCode);

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
	private Map<String, String> getCd90SpecifyMachineMap(String jobType) {
		List<Cd90SpecifyMachineVo> cd90SpecifyMachineList = cd90EngineSpecifyMachineMapper
				.selectCd90SpecifyMachineList(jobType);
		Map<String, String> cd90SpecifyMachineMap = cd90SpecifyMachineList.stream()
				.collect(Collectors.toMap(Cd90SpecifyMachineVo::getClothCode, Cd90SpecifyMachineVo::getMachineId));
		return cd90SpecifyMachineMap;
	}

	/**
	 * 根据机台产能选择机台
	 *
	 * @param scheduleList 排程结果列表
	 */
	@Override
	public void chooseMachineByCapacity(List<Cd90ScheduleResultVo> scheduleList) {
	    if (CollectionUtil.isEmpty(scheduleList)) {
	        return;
	    }
		// 根据机台产能选机台
		List<Cd90MachineInfo> allMachineList = this.listCd90Machine();
		Map<String, String> specifyCanMachineMap = this.getCd90SpecifyMachineMap(EngineConstants.JOB_TYPE_CAN);
		Map<String, String> specifyNotMachineMap = this.getCd90SpecifyMachineMap(EngineConstants.JOB_TYPE_NOT);
        Map<String, Long> clothMachineMap = cd90EngineSpecifyMachineMapper
                .listLastDayPlanMachine(CollectionUtil.firstElement(scheduleList).getScheduleDate()).stream()
                .filter(r -> NumberUtils.isDigits(r.getMachineId()) && StringUtils.isNotEmpty(r.getClothCode()))
                .collect(Collectors.toMap(Cd90SpecifyMachineVo::getClothCode, r -> new Long(r.getMachineId()))); // 已排规格，初始为上一个班的规格
        // 抓取大卷与机台的对照关系
        List<Cd90MachineRollMappingVo> machineRollList = cd90EngineMachineRollMappingMapper
                .selectCd90MachineRollMappingList();
        Map<String, String> machineRollMap = machineRollList.stream().collect(
                Collectors.toMap(Cd90MachineRollMappingVo::getBigRollCode, Cd90MachineRollMappingVo::getMachineId));

		// 机台夜班已占用产能
		Map<Long, BigDecimal> midCapacityMap = new HashMap<>(16);
		// 机台白班已占用产能
		Map<Long, BigDecimal> nightCapacityMap = new HashMap<>(16);

		// 先对排产计划
		List<Cd90ScheduleResultVo> chooseMachineScheduleList = scheduleList.stream().sorted((o1, o2) -> {
		    // 先比较定点机台
			String specifyMachine1 = specifyCanMachineMap.get(o1.getClothCode());
			String specifyMachine2 = specifyCanMachineMap.get(o2.getClothCode());
			int resut = this.compareSpecifyMachine(specifyMachine1, specifyMachine2);
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
			BigDecimal planQty1 = BigDecimalUtils.add(o1.getDayPlanQty(), o1.getNightPlanQty());
			BigDecimal planQty2 = BigDecimalUtils.add(o2.getDayPlanQty(), o2.getNightPlanQty());
			return planQty2.compareTo(planQty1);
		}).collect(Collectors.toList());

		// 根据夜班计划分配机台
		for (Cd90ScheduleResultVo scheduleVo : chooseMachineScheduleList) {
			Double midPlanQty = scheduleVo.getDayPlanQty();
			if (midPlanQty == null || midPlanQty <= 0) {
				continue;
			}
			// 夜班
			String classCode = String.valueOf(OpenMachineClassEnums.CLASS_TWO.getClassIndex());
			// 检索当班可选机台
			List<Cd90MachineInfo> optionalMachineList = this.searchOptionalMachineList(scheduleVo, classCode, midCapacityMap,
					allMachineList, specifyCanMachineMap, specifyNotMachineMap, machineRollMap, clothMachineMap);
			if (CollectionUtil.isEmpty(optionalMachineList)) {
				continue;
			}
			// 如果有匹配机台，则直接取第一个机台赋值
			Cd90MachineInfo machine = CollectionUtil.firstElement(optionalMachineList);
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
			clothMachineMap.put(scheduleVo.getClothCode(), machineId);
			// 添加日志
			this.chooseMachineLog(scheduleVo, specifyCanMachineMap, specifyNotMachineMap);
		}

		// 剩余没有分配到机台的排程检查早班是否有可分配机台
		for (Cd90ScheduleResultVo scheduleVo : chooseMachineScheduleList) {
			if (StringUtils.isNotEmpty(scheduleVo.getMachineId())) {
				continue;
			}
			// 早班
			String classCode = String.valueOf(OpenMachineClassEnums.CLASS_THREE.getClassIndex());
			// 检索当班可选机台
			List<Cd90MachineInfo> optionalMachineList = this.searchOptionalMachineList(scheduleVo, classCode, nightCapacityMap,
					allMachineList, specifyCanMachineMap, specifyNotMachineMap, machineRollMap, clothMachineMap);
			if (CollectionUtil.isEmpty(optionalMachineList)) {
				continue;
			}
			// 如果有匹配机台，则直接取第一个机台赋值
			Cd90MachineInfo machine = CollectionUtil.firstElement(optionalMachineList);
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
	        clothMachineMap.put(scheduleVo.getClothCode(), machineId);
			// 添加日志
			this.chooseMachineLog(scheduleVo, specifyCanMachineMap, specifyNotMachineMap);
		}
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
	 * 查询90度裁断机台列表
	 *
	 * @return 结果
	 */
	private List<Cd90MachineInfo> listCd90Machine() {
		return cd90EngineSpecifyMachineMapper.listCd90Machine();
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
     * @param machineRollMap       大卷机台映射表
     * @param clothMachineMap      已排规格
     * @return 机台列表
     */
    private List<Cd90MachineInfo> searchOptionalMachineList(Cd90ScheduleResultVo scheduleVo, String classCode,
            Map<Long, BigDecimal> capacityMap, List<Cd90MachineInfo> allMachineList,
            Map<String, String> specifyCanMachineMap, Map<String, String> specifyNotMachineMap,
            Map<String, String> machineRollMap, Map<String, Long> clothMachineMap) {
		String clothCode = scheduleVo.getClothCode(); // 帘布代码
		String bigRollCode = scheduleVo.getBigRollCode(); // 大卷代码
		// 定点机台ID列表
		String specifyMachineIds = specifyCanMachineMap.get(clothCode);
		List<String> machineIds;
		// 如果有设置定点机台，需要把非定点全部过滤掉
		if (StringUtils.isNotEmpty(specifyMachineIds)) {
			machineIds = Arrays.asList(specifyMachineIds.split(","));
		} else {
			machineIds = new ArrayList<>(0);
		}
		// 可选机台
		List<Cd90MachineInfo> optionalMachineList = allMachineList.stream().filter(m -> {
					// 排除定点不可生产机台
					String machineId = String.valueOf(m.getId());
					String notMachine = specifyNotMachineMap.get(clothCode);
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
				}).filter(m -> StringUtils.contains(m.getOpenMachineClass(), classCode))
				// 对应班次可用
				.sorted(new Comparator<Cd90MachineInfo>() {
                    @Override
                    public int compare(Cd90MachineInfo m1, Cd90MachineInfo m2) {
                        // 同一个规格优先排在已排过相同规格的机台上
                        Long scheduleMachineId = clothMachineMap.getOrDefault(clothCode, 0L);
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
	 *
	 * @param scheduleVo           排程数据
	 * @param specifyCanMachineMap 定点机台限制作业
	 * @param specifyNotMachineMap 定点机台不可作业
	 */
	private void chooseMachineLog(Cd90ScheduleResultVo scheduleVo, Map<String, String> specifyCanMachineMap, Map<String, String> specifyNotMachineMap) {
		StringBuffer logDetail = new StringBuffer();
		logDetail.append("①优先选择“定点机台中限制作业集合”匹配上的机台;②如果没有，在选择“口型板与机台对应关系集合”的机台信息，不过需要过滤掉'定点机台中不可作业'中的机台").append(division);
		logDetail.append("定点机台中限制作业集合：" + toJSONString(specifyCanMachineMap)).append(division);
		logDetail.append("定点机台中不可作业集合：" + toJSONString(specifyNotMachineMap)).append(division);
		logDetail.append("结果数据：" + toJSONString(scheduleVo)).append(division);
		autoScheduleLogService.insertCd90ScheduleLog(scheduleVo.getBatchNo(), scheduleVo.getOrderNo(), "设置生产线（机台）", logDetail.toString());
	}

}
