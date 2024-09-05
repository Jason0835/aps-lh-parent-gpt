package com.zlt.aps.cx.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.apache.commons.lang.math.NumberUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.core.utils.BigDecimalUtil;
import com.zlt.aps.common.engine.constants.EngineConstants;
import com.zlt.aps.common.engine.utils.CollectionUtil;
import com.zlt.aps.cx.api.domain.dto.CxParamsDto;
import com.zlt.aps.cx.api.domain.dto.LhMachineInfoDto;
import com.zlt.aps.cx.api.domain.entity.CxScheduleResult;
import com.zlt.aps.cx.api.domain.entity.CxStock;
import com.zlt.aps.cx.entity.CxParams;
import com.zlt.aps.cx.mapper.LhMachineInfoEmbyroStockMapper;
import com.zlt.aps.cx.service.CxParamsService;
import com.zlt.aps.cx.service.LhMachineInfoEmbyroStockService;

/**
 * 硫化机台-胎胚库存接口
 * 
 * @Description
 * @Author hakimryan
 * @Date 2021-12-17 10:03:21
 */
@Service("lhMachineInfoEmbyroStockService")
public class LhMachineInfoEmbyroStockServiceImpl implements LhMachineInfoEmbyroStockService {

	@Resource
	private LhMachineInfoEmbyroStockMapper lhMachineInfoEmbyroStockMapper;

	@Autowired
	private CxParamsService cxParamsService;
	/**
	 * 硫化机台空闲天数默认值：7天
	 */
	private final static String DEFAULT_LH_MACHINE_FREE_DAY = "7";

	/**
	 * 查询硫化机台列表，根据胎胚库存顺序排序
	 */
	@Override
	public List<LhMachineInfoDto> getList() {
		return this.getList(null);
	}

	/**
	 * 查询硫化机台列表，根据胎胚库存顺序排序<br/>
	 * 参数为滤条件：机台名称<br/>
	 * 需要计算胎胚库存在各个对应硫化机上的分配量，计算逻辑：<br/>
	 * 1、取出7天内的成型排程，天数可通过参数配置。<br/>
	 * 2、从排程记录取出每个硫化机最后一次使用的模数情况<br/>
	 * 3、按胎胚分组合计所有硫化机可用模数，再计算每个硫化机的可用模数占比<br/>
	 * 4、库存分配量 = 胎胚库存 * 可用模数占比<br/>
	 * 返回的结果需要按要求排好序
	 * 
	 */
	@Override
	public List<LhMachineInfoDto> getList(String machineName) {
		// 排产日期（默认当前服务器时间）
		Date scheduleDate = DateUtils.parseDate(DateUtils.getDate());
		// 完整的可用硫化机台列表
		List<LhMachineInfoDto> lhMachineList = lhMachineInfoEmbyroStockMapper.selectAll(machineName);
		// 硫化机台排产日最大空闲天数内的所有成型排程结果（按排产时间倒序排序）
		List<CxScheduleResult> cxScheduleList = this.getCxScheduleResultList(scheduleDate, machineName);
		// 排产日成型胎胚库存列表
		List<CxStock> selectCxStockList = this.lhMachineInfoEmbyroStockMapper.selectCxStock(scheduleDate);

		// 转换硫化机台基本信息map<硫化机台编码，硫化机台基本信息实体>
		Map<String, LhMachineInfoDto> lhMachineMap = lhMachineList.stream()
				.collect(Collectors.toMap(LhMachineInfoDto::getMachineCode, Function.identity(), (m1, m2) -> m2));
		// 转换库存map<胎胚编码，库存量>
		Map<String, Long> stockMap = selectCxStockList.stream()
				.collect(Collectors.toMap(CxStock::getEmbryoCode, CxStock::getStockNum));
		// 按胎胚汇总统计各硫化机的排程相关信息map<机台编号&&胎胚代码，机台信息>
		Map<String, LhMachineInfoDto> machineEmbryoMap = this.groupingMachineEmbryo(cxScheduleList);
		// 计算库存分配量
		List<LhMachineInfoDto> machineList = this.caculateAssignedStock(lhMachineMap, stockMap, machineEmbryoMap);

		// 完成硫化机台的库存统计后，按如下规则排序
		// 1、按月剩余量小于等于0 或者 大于0分成两组，小于等于0在前，大于0在后
		// 2、上述同一组数据再按 成型状态 是否等于已收尾（2）分成两组，已收尾在前，其他在后
		// 3、上述分组后，各组按以下规则排序：胎胚分配库存 > 硫化机台名称 > id的优先级，正序排序
		List<LhMachineInfoDto> resultList = machineList.stream()
				// 按月度剩余量分组排序
				.sorted(this.createMonthPlanOsComparator()
						// 按成型完成状态分组排序
						.thenComparing(this.createCxStatusComparator())
						// 组内排序第一序列：胎胚库存
						.thenComparing(Comparator.comparing(LhMachineInfoDto::getEmbyroStock))
						// 组内排序第二序列：硫化机台名称
						.thenComparing(Comparator.comparing(LhMachineInfoDto::getMachineName))
						// 组内排序第三序列：id
						.thenComparing(Comparator.comparing(LhMachineInfoDto::getId)))
				.collect(Collectors.toList());
		return resultList;
	}

	/**
	 * 创建查询结果排序器——剩余量<br/>
	 * 排序规则：月剩余量小于等于0的在前，其余在后
	 * 
	 * @return
	 */
	private Comparator<LhMachineInfoDto> createMonthPlanOsComparator() {
		return new Comparator<LhMachineInfoDto>() {
			@Override
			public int compare(LhMachineInfoDto o1, LhMachineInfoDto o2) {
				// 取出剩余量，空的转换成0比较
				Integer monthPlanOs1 = Optional.ofNullable(o1.getMonthPlanOs()).orElse(0);
				Integer monthPlanOs2 = Optional.ofNullable(o2.getMonthPlanOs()).orElse(0);
				// 剩余量处理情况，小于等于0的算已处理完成
				boolean isFinish1 = monthPlanOs1.compareTo(0) <= 0;
				boolean isFinish2 = monthPlanOs2.compareTo(0) <= 0;
				if (isFinish1 ^ isFinish2) {
					// 当两个机台剩余量处理情况不一样时，未完成的在后
					return !isFinish1 ? 1 : -1;
				}
				// 其余情况不调整顺序
				return 0;
			}
		};
	}

	/**
	 * 创建查询结果排序器——成型状态<br/>
	 * 排序规则：成型状态 = 已收尾（2）在前，其余在后
	 * 
	 * @return
	 */
	private Comparator<LhMachineInfoDto> createCxStatusComparator() {
		return new Comparator<LhMachineInfoDto>() {
			@Override
			public int compare(LhMachineInfoDto o1, LhMachineInfoDto o2) {
				// 判断两个机台的成型状态是否已收尾
				boolean isFinish1 = EngineConstants.PRODUCTION_STATUS_FINISH.equals(o1.getCxProductionStatus());
				boolean isFinish2 = EngineConstants.PRODUCTION_STATUS_FINISH.equals(o2.getCxProductionStatus());
				if (isFinish1 ^ isFinish2) {
					// 当两个机台的收尾状态不一样时，未收尾的在后
					return !isFinish1 ? 1 : -1;
				}
				// 其余情况不调整顺序
				return 0;
			}
		};
	}

	/**
	 * 计算库存分配量，并拼装成前端查询对象
	 * 
	 * @param lhMachineMap     硫化机台基本信息集合，map结构：<硫化机台编码，硫化机台实体>
	 * @param stockMap         库存集合，map结构：<胎胚编码，库存量>
	 * @param machineEmbryoMap 硫化机台胎胚分配集合，map结构：<机台编号&&胎胚代码，硫化机排程>
	 * @return
	 */
	private List<LhMachineInfoDto> caculateAssignedStock(Map<String, LhMachineInfoDto> lhMachineMap,
			Map<String, Long> stockMap, Map<String, LhMachineInfoDto> machineEmbryoMap) {
		// 每个胎胚所分配硫化机的模数map<胎胚编号，map<硫化机编号，模数>>
		Map<String, Map<String, Integer>> embryoMoldMap = this.changeToEmbryoMoldMap(machineEmbryoMap);
		// 复制一份硫化机台map，用于区分哪个机台已经分配了胎胚库存
		Map<String, LhMachineInfoDto> surplusLhMachineMap = new HashMap<>(lhMachineMap);
		List<LhMachineInfoDto> queryList = new ArrayList<>();
		// 遍历胎胚分配模数，计算各胎胚的模数占比，并根据模数占比分配胎胚库存量到各机台上
		for (Entry<String, Map<String, Integer>> entry : embryoMoldMap.entrySet()) {
			// 胎胚编号
			String embryoCode = entry.getKey();
			// 机台模数情况
			Map<String, Integer> moldMap = entry.getValue();
			// 胎胚库存数
			Long stockNum = stockMap.getOrDefault(embryoCode, 0L);
			// 胎胚已安排的总模数
			Integer totalMoldNum = moldMap.entrySet().stream().collect(Collectors.summingInt(m -> m.getValue()));
			int size = moldMap.size();
			int index = 0;
			// 已分配库存
			Long assignedStock = 0L;
			// 计算前先对机台模式分配情况排序，优先级：模数（倒序） > 机台编号（正序），防止每次排序结果可能出现变动
			List<Entry<String, Integer>> entryList = moldMap.entrySet().stream()
					// 按模数倒序排序
					.sorted(Comparator.comparing(Entry<String, Integer>::getValue, Comparator.reverseOrder())
							// 按机台编号正序排序
							.thenComparing(Comparator.comparing(Entry<String, Integer>::getKey)))
					.collect(Collectors.toList());
			// 遍历各硫化机的总模数分配量
			for (Entry<String, Integer> moldEntry : entryList) {
				// 机台编号
				String machineCode = moldEntry.getKey();
				// 模数分配量
				Integer moldNum = moldEntry.getValue();
				// 分配库存量
				Long stock;
				// 计算库存分配
				if (index < size - 1) {
					// 非最后一笔，分配库存量 = 总库存 * 模数分配量 / 总模数
					stock = (long) BigDecimalUtil.div(BigDecimalUtil.mul(stockNum, moldNum), totalMoldNum, 0);
				} else {
					// 最后一笔为防止小数舍入产生尾差，分配库存量 = 总库存 - 库存总分配量
					stock = stockNum > assignedStock ? stockNum - assignedStock : 0;
				}

				// 取出硫化机台信息
				LhMachineInfoDto machineInfo = lhMachineMap.get(machineCode);
				if (machineInfo != null) {
					// 将机台信息与库存份配量封装至查询对象中
					LhMachineInfoDto queryMachineInfo = machineEmbryoMap
							.get(this.createLhMachineKey(machineInfo.getMachineCode(), embryoCode));
					if (queryMachineInfo != null) {
						// 补全机台信息
						queryMachineInfo.setId(machineInfo.getId());
						queryMachineInfo.setMachineName(machineInfo.getMachineName());
						queryMachineInfo.setEmbyroStock(stock);
						// 计算可硫化班数
						Double lhShift = this.caculateClassAvailableLhShift(queryMachineInfo);
						queryMachineInfo.setClassAvailableLhShift(lhShift);
						// 机台分配情况添加至查询结果列表中
						queryList.add(queryMachineInfo);
						// 将分批库存的机台从map中移除
						surplusLhMachineMap.remove(machineCode);
						// 每匹配上一次，机台的胎胚数 + 1
						machineInfo.setEmbryoNum(machineInfo.getEmbryoNum() + 1);
					}
				}
				// 本机台分配量累加至总分配量中
				assignedStock += stock;
				index++;
			}
		}
		// 将机台的胎胚数赋值给查询对象
		for (LhMachineInfoDto queryMachineInfo : queryList) {
			LhMachineInfoDto machineInfo = lhMachineMap.get(queryMachineInfo.getMachineCode());
			queryMachineInfo.setEmbryoNum(machineInfo.getEmbryoNum());
		}
		// 将未分配库存的机台添加至查询结果列表中
		queryList.addAll(surplusLhMachineMap.values());
		return queryList;
	}

	/**
	 * 根据硫化机台胎胚分配情况计算可硫化班数<br/>
	 * 计算公式：胎胚库存 / 单班硫化量
	 * 
	 * @param lhMachineInfo 硫化机台分配信息
	 * @return
	 */
	private Double caculateClassAvailableLhShift(LhMachineInfoDto lhMachineInfo) {
		// 从机台取出库存分配量
		BigDecimal stock = new BigDecimal(lhMachineInfo.getEmbyroStock());
		// 从机台取出单班硫化量
		Integer singleShiftLhQtySingleShiftLhQty = lhMachineInfo.getSingleShiftLhQty();
		Double lhShift = 0D;
		if (singleShiftLhQtySingleShiftLhQty != null && singleShiftLhQtySingleShiftLhQty != 0) {
			// = 胎胚库存 / 单班硫化量，结果保留两位小数
			lhShift = stock.divide(new BigDecimal(singleShiftLhQtySingleShiftLhQty), 2, RoundingMode.HALF_UP)
					.doubleValue();
		}
		return lhShift;
	}

	/**
	 * 按胎胚汇总统计各硫化机的排程相关信息
	 * 
	 * @param cxScheduleList 成型排程
	 * @return
	 */
	private Map<String, LhMachineInfoDto> groupingMachineEmbryo(List<CxScheduleResult> cxScheduleList) {
		// 机台各胎胚模数分配map<机台编号，map<胎胚代码，机台信息>>，表示一个机台同一天的所有胎胚安排情况
		Map<String, Map<String, LhMachineInfoDto>> machineEmbryoMap = new HashMap<>();
		// 遍历排产结果
		for (CxScheduleResult scheduleResult : cxScheduleList) {
			String embryoCode = scheduleResult.getEmbryoCode();
			Date scheduleDate = scheduleResult.getScheduleDate();
			String moldDesc = scheduleResult.getLhMachineChangeMoldDesc();
			if (StringUtils.isEmpty(moldDesc)) {
				continue;
			}
			// 解析硫化模数，解析结果：map<硫化机台编号，总模数>
			Map<String, Integer> parseMoldMap = this.parseMold(moldDesc);
			// 将模数合并至汇总map中
			for (Entry<String, Integer> entry : parseMoldMap.entrySet()) {
				// 机台编号
				String machineCode = entry.getKey();
				// 模数
				int moldNum = entry.getValue();
				// 构建该机台本次排产情况
				LhMachineInfoDto newMachineInfo = new LhMachineInfoDto();
				newMachineInfo.setMachineCode(machineCode);
				newMachineInfo.setEmbryoCode(embryoCode);
				newMachineInfo.setScheduleDate(scheduleDate);
				newMachineInfo.setMoldNum(moldNum);
				newMachineInfo.setSpecDimension(scheduleResult.getSpecDimension());
				newMachineInfo.setCxProductionStatus(scheduleResult.getProductionStatus());
				newMachineInfo.setLhProductionStatus(scheduleResult.getTaskType());
				newMachineInfo.setMonthPlanOs(scheduleResult.getMonthPlanOs());
				newMachineInfo.setSingleShiftLhQty(scheduleResult.getSingleShiftLhQty());
				// 通过机台编号查找是否已排产
				Map<String, LhMachineInfoDto> machineMap = machineEmbryoMap.get(machineCode);
				if (CollectionUtil.isEmpty(machineMap)) {
					// 没有排产则直接存起来，Key用胎胚号代表该机台安排了该胎胚
					machineMap = new HashMap<>();
					machineMap.put(embryoCode, newMachineInfo);
					machineEmbryoMap.put(machineCode, machineMap);
				} else {
					// 如果已经有排产，取出任意一个排产情况进行数据校验处理
					LhMachineInfoDto machineInfo = CollectionUtil.firstElement(machineMap.entrySet()).getValue();
					if (machineInfo.getScheduleDate().compareTo(scheduleDate) < 0) {
						// 由于各机台只保留一天的排产情况，因此当前排产的排产日晚于机台已排产的最晚排产日，则清除旧机台的已排产信息
						machineMap.clear();
						// 保留最新的机台排产信息
						machineMap.put(embryoCode, newMachineInfo);
					} else if (machineInfo.getScheduleDate().compareTo(scheduleDate) == 0) {
						// 如果当前排产日与机台最晚排产日为同一天，则按胎胚编号合并模数
						machineInfo = machineMap.get(embryoCode);
						if (machineInfo != null) {
							machineInfo.setMoldNum(machineInfo.getMoldNum() + moldNum);
						} else {
							// 原列表没有安排该胎胚，则直接保存到列表中
							machineMap.put(embryoCode, newMachineInfo);
						}
					}
					// 早于机台最晚排产日的不需要处理
				}
			}
		}
		// 转换Map结构
		return this.changeToMachineEmbryoMap(machineEmbryoMap);
	}

	/**
	 * Map转换：Map<机台编号, Map<胎胚代码，机台信息>> 转换为 map<机台编号&&胎胚代码，硫化机信息>
	 * 
	 * @param embryoMachineListMap 胎胚机台信息map
	 * @return
	 */
	private Map<String, LhMachineInfoDto> changeToMachineEmbryoMap(
			Map<String, Map<String, LhMachineInfoDto>> embryoMachineListMap) {
		Map<String, LhMachineInfoDto> machineEmbryoMap = new HashMap<>();
		for (Map<String, LhMachineInfoDto> machineMap : embryoMachineListMap.values()) {
			for (LhMachineInfoDto machineInfo : machineMap.values()) {
				machineEmbryoMap.put(this.createLhMachineKey(machineInfo), machineInfo);
			}
		}
		return machineEmbryoMap;
	}

	/**
	 * Map转换：Map<机台编号, 机台信息> 转换为 map<胎胚编号，map<硫化机编号，模数>>
	 * 
	 * @param machineMoldMap 机台信息map
	 * @return
	 */
	private Map<String, Map<String, Integer>> changeToEmbryoMoldMap(Map<String, LhMachineInfoDto> machineEmbryoMap) {
		Map<String, Map<String, Integer>> embryoMoldMap = new HashMap<>();
		for (LhMachineInfoDto machineInfo : machineEmbryoMap.values()) {
			// 根据胎胚 > 机台编号汇总模数
			String embryoCode = machineInfo.getEmbryoCode();
			String machineCode = machineInfo.getMachineCode();
			Map<String, Integer> moldMap = embryoMoldMap.get(embryoCode);
			if (moldMap == null) {
				moldMap = new HashMap<>();
				embryoMoldMap.put(embryoCode, moldMap);
			}
			int moldNum = moldMap.getOrDefault(machineCode, 0);
			moldMap.put(machineCode, moldNum + machineInfo.getMoldNum());
		}
		return embryoMoldMap;
	}

	/**
	 * 解析硫化模数 <BR/>
	 * 解析规则：更换类型描述的格式如下<BR/>
	 * 硫化机code1:拆模换code:模数molds;硫化机code2:点数换code:模数molds<BR/>
	 * 需要分解字符串取出各个栏位的信息
	 * 
	 * @param moldDesc 成型排程的硫化模数描述
	 * @return
	 */
	private Map<String, Integer> parseMold(String moldDesc) {
		Map<String, Integer> parseMoldMap = Arrays.stream(moldDesc.split(";"))
				// 分解字符串，取出1、硫化机code，2、拆模换code，3、模数molds
				.map(m -> m.split(":"))
				// 过滤格式不符合规范的数据
				.filter(m -> m.length >= 3)
				// 过滤硫化机台编号为空的数据
				.filter(m -> StringUtils.isNotEmpty(m[0]))
				// 过滤模数不正确的数据
				.filter(m -> NumberUtils.isDigits(m[2]))
				// 按硫化机台编号分组统计模数
				// m[0]为硫化机code
				// m[2]为模数molds
				// 为防止硫化机重复，需要按硫化机分组后，合计模数
				.collect(Collectors.groupingBy(m -> m[0], Collectors.summingInt(m -> Integer.parseInt(m[2]))));
		return parseMoldMap;
	}

	/**
	 * 获取成型排程列表，返回硫化机台最大空闲天数内的所有成型排程结果
	 * 
	 * @param scheduleDate 排产日
	 * @param machineName  机台名称
	 * @return
	 */
	private List<CxScheduleResult> getCxScheduleResultList(Date scheduleDate, String machineName) {
		// 获取硫化机台空闲天数配置
		int freeDay = this.getLhMachineFreeDay();
		// 开始时间为排产日 - 硫化机台空闲天数
		Date startDate = DateUtils.addDays(scheduleDate, -freeDay);
		// 结束时间为排产日
		Date endDate = scheduleDate;
		List<CxScheduleResult> cxScheduleList = lhMachineInfoEmbyroStockMapper.selectCxScheduleResult(startDate,
				endDate, machineName);
		return cxScheduleList;
	}

	/**
	 * 获取硫化机台空闲天数配置
	 * 
	 * @return
	 */
	private int getLhMachineFreeDay() {
		// 获取机台参数：硫化机空闲天数
		CxParams params = new CxParams();
		params.setParamCode(EngineConstants.LH_MACHINE_FREE_DAY);
		CxParamsDto config = CollectionUtil.firstElement(cxParamsService.selectParamsList(params));
		String freeDayConfig = config != null ? config.getParamValue() : null;
		// 需要对参数判空，如果为空则返回预设的默认值
		freeDayConfig = Optional.ofNullable(freeDayConfig).orElse(DEFAULT_LH_MACHINE_FREE_DAY);
		int freeDay = Integer.parseInt(freeDayConfig);
		// 空闲天数不能小于0，小于0则直接返回0
		return freeDay > 0 ? freeDay : 0;
	}

	/**
	 * 创建硫化机台排程情况唯一键<br/>
	 * key格式：机台编号&&胎胚代码
	 * 
	 * @param machineInfo 机台信息
	 * @return
	 */
	private String createLhMachineKey(LhMachineInfoDto machineInfo) {
		return this.createLhMachineKey(machineInfo.getMachineCode(), machineInfo.getEmbryoCode());
	}

	/**
	 * 创建硫化机台排程情况唯一键<br/>
	 * key格式：机台编号&&胎胚代码
	 * 
	 * @param machineCode 机台编号
	 * @param embryoCode  胎胚代码
	 * @return
	 */
	private String createLhMachineKey(String machineCode, String embryoCode) {
		return StringUtils.join(new String[] { machineCode, embryoCode }, "&&");
	}
}
