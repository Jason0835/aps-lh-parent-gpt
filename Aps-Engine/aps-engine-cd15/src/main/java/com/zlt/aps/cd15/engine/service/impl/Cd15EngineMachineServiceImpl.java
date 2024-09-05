package com.zlt.aps.cd15.engine.service.impl;

import static com.alibaba.fastjson.JSON.toJSONString;
import static com.zlt.aps.common.core.utils.ApsCommonUtil.logSplit;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.cd15.engine.mapper.Cd15EngineMachineRollMappingMapper;
import com.zlt.aps.cd15.engine.mapper.Cd15EngineSpecifyMachineMapper;
import com.zlt.aps.cd15.engine.service.Cd15EngineMachineService;
import com.zlt.aps.cd15.engine.vo.Cd15MachineRollMappingVo;
import com.zlt.aps.cd15.engine.vo.Cd15ScheduleResultVo;
import com.zlt.aps.cd15.engine.vo.Cd15SpecifyMachineVo;
import com.zlt.aps.common.engine.constants.EngineConstants;
import com.zlt.aps.common.engine.service.AutoScheduleLogService;
import com.zlt.aps.common.engine.utils.CollectionUtil;

/**
 * 15度裁断生产线服务实现类
 * 
 * @Description
 * @Author hakimrayn
 * @Date 2021-7-9 11:24:00
 * @Version 1.0
 */
@Service("cd15EngineMachineService")
public class Cd15EngineMachineServiceImpl implements Cd15EngineMachineService {
	@Autowired
	private Cd15EngineSpecifyMachineMapper cd15EngineSpecifyMachineMapper;
	@Autowired
	private Cd15EngineMachineRollMappingMapper cd15EngineMachineRollMappingMapper;
	@Resource
	private AutoScheduleLogService autoScheduleLogService;

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
}
