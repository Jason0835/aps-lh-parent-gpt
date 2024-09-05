package com.zlt.aps.xwyy.engine.service.impl;

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
import com.zlt.aps.common.engine.constants.EngineConstants;
import com.zlt.aps.common.engine.service.AutoScheduleLogService;
import com.zlt.aps.common.engine.utils.CollectionUtil;
import com.zlt.aps.xwyy.engine.mapper.XwyyEngineMachineRollMappingMapper;
import com.zlt.aps.xwyy.engine.mapper.XwyyEngineSpecifyMachineMapper;
import com.zlt.aps.xwyy.engine.service.XwyyEngineMachineService;
import com.zlt.aps.xwyy.engine.vo.XwyyMachineRollMappingVo;
import com.zlt.aps.xwyy.engine.vo.XwyyScheduleResultVo;
import com.zlt.aps.xwyy.engine.vo.XwyySpecifyMachineVo;

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
