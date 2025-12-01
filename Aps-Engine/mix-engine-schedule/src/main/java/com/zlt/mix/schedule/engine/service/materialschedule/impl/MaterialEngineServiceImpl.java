package com.zlt.mix.schedule.engine.service.materialschedule.impl;

import static com.zlt.mix.common.core.utils.MixCommonUtil.compare;
import static com.zlt.mix.common.core.utils.MixCommonUtil.getDouble;
import static com.zlt.mix.common.core.utils.MixCommonUtil.getInt;
import static com.zlt.mix.common.engine.constants.EngineConstants.COMMONLY_USED_NO;
import static com.zlt.mix.common.engine.constants.EngineConstants.COMMONLY_USED_YES;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.pagehelper.util.StringUtil;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.utils.bean.BeanUtils;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.mix.common.core.constant.ZltConstant;
import com.zlt.mix.common.core.utils.BigDecimalUtil;
import com.zlt.mix.common.core.utils.DateUtil;
import com.zlt.mix.common.engine.constants.EngineConstants;
import com.zlt.mix.common.engine.service.AutoScheduleLogService;
import com.zlt.mix.common.engine.service.impl.IncrementService;
import com.zlt.mix.schedule.api.domain.entity.MaterialScheduleResult;
import com.zlt.mix.schedule.api.domain.entity.MaterialSpanReceive;
import com.zlt.mix.schedule.api.domain.entity.MaterialSpanSend;
import com.zlt.mix.schedule.engine.mapper.MaterialEngineMapper;
import com.zlt.mix.schedule.engine.service.basicdata.MachineEngineService;
import com.zlt.mix.schedule.engine.service.basicdata.ParamsEngineService;
import com.zlt.mix.schedule.engine.service.basicdata.RecipeEngineService;
import com.zlt.mix.schedule.engine.service.basicdata.StockEngineService;
import com.zlt.mix.schedule.engine.service.materialschedule.MaterialClassEngineService;
import com.zlt.mix.schedule.engine.service.materialschedule.MaterialEngineService;
import com.zlt.mix.schedule.engine.service.materialschedule.MaterialSpanEngineService;
import com.zlt.mix.schedule.engine.vo.MaterialAreaMachineVo;
import com.zlt.mix.schedule.engine.vo.MaterialScheduleResultVo;
import com.zlt.mix.schedule.engine.vo.MaterialSpanVo;
import com.zlt.mix.setting.api.domain.entity.LhflMachine;
import com.zlt.mix.setting.api.domain.entity.MesPmtRecipe;

import lombok.extern.slf4j.Slf4j;

/**
 * 硫磺辅料日计划排程引擎Service业务层处理
 */
@Slf4j
@Service
public class MaterialEngineServiceImpl implements MaterialEngineService {

    @Resource
    private IncrementService incrementService;
    @Resource
    private MaterialEngineMapper materialEngineMapper;
    @Resource
    private ParamsEngineService paramsEngineService;
    @Resource
    private StockEngineService stockEngineService;
    @Resource
    private MachineEngineService machineEngineService;
    @Resource
    private RecipeEngineService recipeEngineService;
    @Resource
    private MaterialClassEngineService materialClassEngineService;
    @Resource
    private MaterialSpanEngineService materialSpanEngineService;
    @Resource
    private AutoScheduleLogService autoScheduleLogService;
    private String division = "\r\n---------------------------------------------------\r\n";  //日志分割符

    /**
     * 硫磺辅料自动排程接口
     * @param scheduleDate
     * @param mixArea
     */
    @Transactional(rollbackFor=Exception.class)
    public void autoSchedule(Date scheduleDate, String mixArea) {
        List<MaterialScheduleResultVo> scheduleList = materialEngineMapper.listBaseMaterialSchedule(scheduleDate, mixArea); //从终炼母炼日计划排程统计出来的排产数据
        List<MaterialScheduleResultVo> spanReceiveList = materialEngineMapper.listSpanReceive(scheduleDate, mixArea);  //跨区接收列表
        // 跨区请求需要区分出增量与减量请求，增量排在最开始，减量在全部排完之后再去扣减
		List<MaterialScheduleResultVo> spanMergeScheduleList = materialClassEngineService.mergeSubtractSpanSchedule(spanReceiveList); // 合并跨区计划
		List<MaterialScheduleResultVo> spanAddScheduleList = spanMergeScheduleList.stream().filter(s -> s.getDemandQty() != null && s.getDemandQty() > 0).collect(Collectors.toList()); // 跨区增量请求
		List<MaterialScheduleResultVo> spanSubScheduleList = spanMergeScheduleList.stream().filter(s -> s.getDemandQty() != null && s.getDemandQty() < 0).collect(Collectors.toList()); // 跨区减量请求
		spanAddScheduleList.forEach(s -> s.setGlueMidExpectStartTime(DateUtils.addHours(scheduleDate, -12))); // 由于跨区增量优先级最高，胶料需求时间统一设置成最早的开班时间（两班制中班）
		scheduleList.addAll(spanAddScheduleList);
        if(scheduleList.isEmpty()) {
            log.error("查询出硫磺辅料排程基础信息列表为空");
            throw new RuntimeException("无硫磺辅料排程数据生成，请检查【终炼母炼日计划】的排程数据是否已经生成!");
        }
		
		String batchNo = incrementService.getSequence3(EngineConstants.MATERIAL_SCHEDULE_PREFIX + mixArea + DateUtil.formatDateYmd(scheduleDate));  //创建批次号
        List<MaterialAreaMachineVo> areaMaterialList = scheduleList.stream().map(r->new MaterialAreaMachineVo(r.getMixArea(), r.getMachineCode(), r.getMaterialName())).collect(Collectors.toList()); //密炼区+物料名称
		List<MaterialScheduleResultVo> safeStockScheduleList = materialEngineMapper.listSafeStockMaterialSchedule(mixArea);	// 安全库存排产列表
		this.mergeSafeStockList(areaMaterialList, safeStockScheduleList, scheduleList, scheduleDate, mixArea); // 将安全库存列表合并至《密炼区+物料名称》列表中
		this.mergeSafeStockList(areaMaterialList, spanSubScheduleList, scheduleList, scheduleDate, mixArea); // 将跨区接收列表合并至《密炼区+物料名称》列表中
		
        Map<String, String> params = paramsEngineService.mapLhflParams(mixArea);   //硫磺辅料参数设置map
        Map<String, Double> totalStockMap = stockEngineService.mapMaterialStock(scheduleDate, areaMaterialList);   //硫磺辅料总库存map
        Map<String, Double> stockMap = new HashMap<>();   //硫磺辅料库存map
        stockMap.putAll(totalStockMap);
        Map<String, String> materialMachineMap = machineEngineService.mapMaterialMachine(areaMaterialList);  //硫磺辅料、辅料机、班别的对应关系map
        this.integrationClassSchedule(mixArea, scheduleList, materialMachineMap); // 将各班需求量整合到最早的一个班中
        
        Map<String, LhflMachine> machineMap = machineEngineService.mapLhflMachineInfo(mixArea);  //辅料称重机信息map
        Map<String, MesPmtRecipe> recipeVersionMap =  recipeEngineService.mapLhflRecipeVersionInfo(areaMaterialList);  //硫磺辅料配方版本map
        Map<String, Double> safeStockMap = safeStockScheduleList.stream().collect(Collectors.toMap(MaterialScheduleResultVo::getMaterialName, MaterialScheduleResultVo::getSafeStockQty));	// 硫磺辅料安全库存map
        List<MaterialScheduleResultVo> publishScheduleList = this.listPublishSchedule(scheduleDate, mixArea, batchNo, machineMap, params); // 自动排程前已发布的排程记录
        this.substractPublishQty(scheduleList, safeStockScheduleList, publishScheduleList); // 需求计划扣减已发布量
        List<MaterialScheduleResultVo> totalScheduleList = new ArrayList<>(scheduleList);
        totalScheduleList.addAll(safeStockScheduleList); // 将安全库存备库排产物料也放置到列表中一起填充排产信息
        totalScheduleList.addAll(spanSubScheduleList); // 将跨区接收物料也放置到列表中一起填充排产信息

        for(MaterialScheduleResultVo schedule : totalScheduleList) {
            schedule.setScheduleDate(scheduleDate);  //排产日期
            String materialName = schedule.getMaterialName(); //物料名称
            schedule.setBatchNo(batchNo);
            schedule.setBaseValue(null);   //基本信息
            if(StringUtils.isBlank(schedule.getDataSource())) {
                schedule.setDataSource(ZltConstant.MATERIAL_SCHEDULE_SOURCE_AUTO);   //数据来源
            }
            schedule.setReleaseStatus(ZltConstant.NO_RELEASE);   //发布状态
            schedule.setMachineCode(this.getMachineCode(mixArea, schedule, materialName, materialMachineMap));   //设置机台编号
            this.fitRecipeVersion(schedule, recipeVersionMap);  //设置配方版本信息

            schedule.setPublishSuccessCount(0);
            schedule.setStockQty(stockMap.getOrDefault(mixArea + materialName, 0D));  //设置库存
            Double safeStock = safeStockMap.getOrDefault(materialName, 0D);
            schedule.setSafeStockQty(safeStock);	// 设置安全库存
            schedule.setCommonlyUsed(COMMONLY_USED_NO);  //设置是否为常用规格
            this.fitDemandPlanning(schedule);   //设置【需求计划】
//            this.setScheduleMachineInfo(schedule);   //从现有的排产记录中拿到机台的产能 和 班制（不直接从机台表拿，是因为此时机台表的产能和班制肯已经被修改了）
            this.countClassWeightTime(schedule, machineMap, getInt(params.get(EngineConstants.MACHINE_DEFAULT_CAPACITY)), getInt(params.get(EngineConstants.DINNER_TIME)));   //根据机台产能，计算硫磺辅料称重 需要的时间戳（同时计算出【机台班制】，【单车称重消耗时间（毫秒）】）
            this.actualWeightForGlue(schedule, getInt(params.get(EngineConstants.PRE_PREPARE_TIME)));  //计算称重预计开始时间（胶料开始生产前，至少要提前{prePrepareTime}分，进行硫磺辅料称重）
        }

        this.reduceStock(scheduleList, stockMap);  //用库存扣减各班计划量, 并计算出常用规格当日的总计划量(先扣中班，在扣夜班，在扣白班)
        //计算各班计划量、计划开始时间、计划完成时间、生产顺序
        this.materialClassEngineService.staScheduleClassInfo(scheduleList, params, machineMap, publishScheduleList);
        //创建安全库存的排产记录。
        this.materialClassEngineService.createSafeStockSchedule(safeStockScheduleList, scheduleList, machineMap, getInt(params.get(EngineConstants.MATERIAL_INTERVAL_TIME)));
        // 处理减量跨区请求
        this.materialClassEngineService.batchSpanReceived(spanSubScheduleList, scheduleList, getInt(params.get(EngineConstants.MATERIAL_INTERVAL_TIME)));
		String spanRemark = I18nUtil.getMessage("engine.material.span.recive.remark"); // 排产班次的备注信息
		spanAddScheduleList.forEach(s -> {
			// 跨区加量的记录在有排产的班次上补上固定备注信息
			if (s.getMidPlanQty() != null && s.getMidPlanQty() > 0) {
				s.setMidRemark(spanRemark);
			}
			if (s.getNightPlanQty() != null && s.getNightPlanQty() > 0) {
				s.setNightRemark(spanRemark);
			}
			if (s.getDayPlanQty() != null && s.getDayPlanQty() > 0) {
				s.setDayRemark(spanRemark);
			}
		});
        
        // 将已发布记录全部重新添加到列表中
        scheduleList.addAll(publishScheduleList);
        //最后按机台、中班顺序排序后。设置订单号
        scheduleList = scheduleList.stream().sorted(Comparator.comparing(MaterialScheduleResultVo::getMachineCode, Comparator.nullsLast(String::compareTo))
                .thenComparing(MaterialScheduleResultVo::getMidProduceOrder, Comparator.nullsLast(Integer::compareTo))).collect(Collectors.toList());
        scheduleList.forEach(r-> {
        	if (StringUtil.isEmpty(r.getOrderNo())) { // 只有新生成的排产计划需要生成工单号
            	r.setOrderNo(incrementService.getSequence4(batchNo));
        	}
        });

        //同步硫磺辅料排程记录到日志表中
        this.synclueScheduleToLog(scheduleDate, mixArea);
        //把最终的硫磺辅料排程记录 进行入库
        materialEngineMapper.batchInsertMaterialSchedule(scheduleList);

        // 处理排程初始日志
        materialEngineMapper.deleteMaterialInitLog(scheduleDate, mixArea);
        materialEngineMapper.syncMaterialScheduleToInitLog(scheduleDate, mixArea);
    }

	/**
	 * 和过去机台编号
	 * 
	 * @param mixArea            密炼区
	 * @param schedule           排产情况
	 * @param materialName       物料名称
	 * @param materialMachineMap 物料、机台、班别对照关系
	 * @return
	 */
	private String getMachineCode(String mixArea, MaterialScheduleResultVo schedule, String materialName,
			Map<String, String> materialMachineMap) {
		String machineCode = schedule.getMachineCode();
		// 没有机台的情况获取机台编号
		if (StringUtils.isBlank(machineCode)) {
			selectMachine: {
				String machineKey = mixArea + materialName;
				machineCode = materialMachineMap.get(machineKey + EngineConstants.CLASS_MID);
				if (StringUtils.isNotBlank(machineCode)) {
					break selectMachine;
				}

				machineCode = materialMachineMap.get(machineKey + EngineConstants.CLASS_NIGHT);
				if (StringUtils.isNotBlank(machineCode)) {
					break selectMachine;
				}

				machineCode = materialMachineMap.get(machineKey + EngineConstants.CLASS_DAY);
				if (StringUtils.isNotBlank(machineCode)) {
					break selectMachine;
				}
				machineCode = "";
			}
		}
		return machineCode;
	}

	/**
	 * 将各班需求量整合到最早的一个班中
	 * 
	 * @param mixArea            密炼区
	 * @param scheduleList       排产列表
	 * @param materialMachineMap 物料、机台、班别对照关系
	 */
	private void integrationClassSchedule(String mixArea, List<MaterialScheduleResultVo> scheduleList,
			Map<String, String> materialMachineMap) {
		// 判断是否需要拆分到不同的机台上
		List<MaterialScheduleResultVo> newScheduleList = new ArrayList<>(scheduleList);
		for (MaterialScheduleResultVo schedule : newScheduleList) {
			String materialName = schedule.getMaterialName(); // 物料名称
			String materialMachineKey = mixArea + materialName;

			// 将计划量尽量向前集中，看最早的有可用机台的是哪一班
			// 判断各班是否有机台可生产，从白班往前校验
			// 各班可用机台
			String midMachineCode = materialMachineMap.get(materialMachineKey + EngineConstants.CLASS_MID);
			String nightMachineCode = materialMachineMap.get(materialMachineKey + EngineConstants.CLASS_NIGHT);
			String dayMachineCode = materialMachineMap.get(materialMachineKey + EngineConstants.CLASS_DAY);
			// 各班是否有机台可用
			boolean isMidMachineEnable = midMachineCode != null;
			boolean isNightMachineEnable = nightMachineCode != null;
			boolean isDayMachineEnable = dayMachineCode != null;
			
			// 先判断是否至少有一个班有机台可生产
			if (!isMidMachineEnable && !isNightMachineEnable && !isDayMachineEnable) {
				// 一个可用机台都没有，则不需要继续判断
				continue;
			}

			String machineCode = "";
			// 各班计划量
			Double midPlanQty = Optional.ofNullable(schedule.getMidPlanQty()).orElse(0D);
			Double nightPlanQty = Optional.ofNullable(schedule.getNightPlanQty()).orElse(0D);
			Double dayPlanQty = Optional.ofNullable(schedule.getDayPlanQty()).orElse(0D);
			Double totalPlanQty = BigDecimalUtil.add(midPlanQty, nightPlanQty, dayPlanQty);
			if (isMidMachineEnable) {
				machineCode = midMachineCode;
				midPlanQty = totalPlanQty;
				nightPlanQty = 0D;
				dayPlanQty = 0D;
			} else if (isNightMachineEnable) {
				machineCode = nightMachineCode;
				midPlanQty = 0D;
				nightPlanQty = totalPlanQty;
				dayPlanQty = 0D;
			} else {
				machineCode = dayMachineCode;
				midPlanQty = 0D;
				nightPlanQty = 0D;
				dayPlanQty = totalPlanQty;
			}
			schedule.setMachineCode(machineCode);
			schedule.setMidPlanQty(midPlanQty);
			schedule.setNightPlanQty(nightPlanQty);
			schedule.setDayPlanQty(dayPlanQty);
		}
	}

    /**
     * 需求计划扣减已发布量
     * @param scheduleList	需求计划
     * @param safeStockScheduleList	备库计划
     * @param publishScheduleList	已发布计划
     */
	private void substractPublishQty(List<MaterialScheduleResultVo> scheduleList, List<MaterialScheduleResultVo> safeStockScheduleList,
			List<MaterialScheduleResultVo> publishScheduleList) {
		// 统计各硫磺辅料已发布到mes的计划量
		Map<String, Double> publishQtyMap = publishScheduleList.stream().filter(r -> r.getTotalPlanQty() != null)
				.collect(Collectors.groupingBy(MaterialScheduleResultVo::getMaterialName,
						Collectors.summingDouble(MaterialScheduleResultVo::getTotalPlanQty)));
		Map<String, List<MaterialScheduleResultVo>> scheduleMap = scheduleList.stream()
				.collect(Collectors.groupingBy(MaterialScheduleResultVo::getMaterialName));
		Map<String, List<MaterialScheduleResultVo>> safeStockMap = safeStockScheduleList.stream()
				.collect(Collectors.groupingBy(MaterialScheduleResultVo::getMaterialName));

		// 遍历已发布物料，从本次自动排程的列表中移除已发布的量
		for (Entry<String, Double> entry : publishQtyMap.entrySet()) {
			String materialName = entry.getKey();
			Double publishQty = entry.getValue(); // 已发布量

			// 处理备库计划
			List<MaterialScheduleResultVo> safeStockList = safeStockMap.get(materialName);
			if (CollectionUtils.isNotEmpty(safeStockList)) {
				publishQty = this.removePublishSafeStockSchedule(safeStockScheduleList, safeStockList, publishQty);
			}

			// 处理需求计划
			List<MaterialScheduleResultVo> materialScheduleList = scheduleMap.get(materialName);
			if (CollectionUtils.isNotEmpty(materialScheduleList)) {
				publishQty = this.removePublishSchedule(scheduleList, materialScheduleList, publishQty);
			}
		}
	}

	/**
	 * 从备库排产列表中移除已发布的物料
	 * 
	 * @param safeStockScheduleList 备库排产列表
	 * @param safeStockList         指定物料的排产列表
	 * @param publishQty            已发布量
	 * @return
	 */
	private Double removePublishSafeStockSchedule(List<MaterialScheduleResultVo> safeStockScheduleList,
			List<MaterialScheduleResultVo> safeStockList, Double publishQty) {
		Double surplusPublishQty = publishQty;
		for (int i = safeStockList.size() - 1; i >= 0; i--) {
			if (surplusPublishQty.compareTo(0D) == 0) {
				break;
			}
			MaterialScheduleResultVo schedule = safeStockList.get(i);
			Double safeStock = Optional.ofNullable(schedule.getSafeStockQty()).orElse(0D); // 安全库存量
			if (surplusPublishQty >= safeStock) { // 如果已发布量大于等于安全库存量，则说明提前插单的计划已经可以满足需求，不需要继续排这笔排程
				safeStockList.remove(i);
				for (int j = 0, size = safeStockScheduleList.size(); j < size; j++) {
					if (safeStockScheduleList.get(j) == schedule) {
						safeStockScheduleList.remove(j);
						break;
					}
				}
				surplusPublishQty = BigDecimalUtil.sub(surplusPublishQty, safeStock);
			} else if (surplusPublishQty > 0) { // 如果已发布量并没有大于安全库存量，则只需扣减掉已发布的量即可
				schedule.setSafeStockQty(BigDecimalUtil.sub(safeStock, surplusPublishQty));
				surplusPublishQty = 0D;
			}
		}
		return surplusPublishQty;
	}

	/**
	 * 从排产列表中移除已发布的物料
	 * 
	 * @param scheduleList         排产列表
	 * @param materialScheduleList 指定物料的排产列表
	 * @param publishQty           已发布量
	 * @return
	 */
	private Double removePublishSchedule(List<MaterialScheduleResultVo> scheduleList,
			List<MaterialScheduleResultVo> materialScheduleList, Double publishQty) {
		Double surplusPublishQty = publishQty;
		for (int i = materialScheduleList.size() - 1; i >= 0; i--) {
			if (surplusPublishQty.compareTo(0D) == 0) {
				break;
			}
			MaterialScheduleResultVo schedule = materialScheduleList.get(i);
			Double demandQty = Optional.ofNullable(schedule.getDemandQty()).orElse(0D); // 需求量
			if (surplusPublishQty >= demandQty) { // 如果已发布量大于等于需求量，则说明提前插单的计划已经可以满足需求，不需要继续排这笔排程
				materialScheduleList.remove(i);
				for (int j = 0, size = scheduleList.size(); j < size; j++) {
					if (scheduleList.get(j) == schedule) {
						scheduleList.remove(j);
						break;
					}
				}
				surplusPublishQty = BigDecimalUtil.sub(surplusPublishQty, demandQty);
			} else if (surplusPublishQty > 0) { // 如果已发布量并没有大于需求量，则只需扣减掉已发布的量即可
				Double dayPlanQty = Optional.ofNullable(schedule.getDayPlanQty()).orElse(0D);
				Double nightPlanQty = Optional.ofNullable(schedule.getNightPlanQty()).orElse(0D);
				Double midPlanQty = Optional.ofNullable(schedule.getMidPlanQty()).orElse(0D);
				// 各班需求量从后往前依次扣减，扣完一班继续扣下一班，直至扣完
				caculate: {
					if (dayPlanQty >= surplusPublishQty) {
						dayPlanQty = BigDecimalUtil.sub(dayPlanQty, surplusPublishQty);
						break caculate;
					}
					surplusPublishQty = BigDecimalUtil.sub(surplusPublishQty, dayPlanQty);
					dayPlanQty = 0D;
					if (nightPlanQty >= surplusPublishQty) {
						nightPlanQty = BigDecimalUtil.sub(nightPlanQty, surplusPublishQty);
						break caculate;
					}
					surplusPublishQty = BigDecimalUtil.sub(surplusPublishQty, nightPlanQty);
					nightPlanQty = 0D;
					if (midPlanQty >= surplusPublishQty) {
						midPlanQty = BigDecimalUtil.sub(midPlanQty, surplusPublishQty);
						break caculate;
					}
					surplusPublishQty = BigDecimalUtil.sub(surplusPublishQty, midPlanQty);
					midPlanQty = 0D;
				}
				schedule.setTotalPlanQty(BigDecimalUtil.add(dayPlanQty, nightPlanQty, midPlanQty));
				schedule.setDayPlanQty(dayPlanQty);
				schedule.setNightPlanQty(nightPlanQty);
				schedule.setMidPlanQty(midPlanQty);
				surplusPublishQty = 0D;
			}
		}
		return surplusPublishQty;
	}
	
    /**
     * 根据已发布计划计算机台的各班可开班时间
     * @param publishMap	已发布计划列表
     * @param machineMap	机台列表
     * @param materialIntervalTime	各排程间隔时长
     */
	private void countMachineClassTime(Map<String, List<MaterialScheduleResultVo>> publishMap,
			Map<String, LhflMachine> machineMap, int materialIntervalTime) {
		// 根据已发布排产记录，确定机台每一班的可开班时间
		for (Entry<String, List<MaterialScheduleResultVo>> entry : publishMap.entrySet()) {
			String machineCode = entry.getKey();
			List<MaterialScheduleResultVo> publisSchedule = entry.getValue();
			LhflMachine machine = machineMap.get(machineCode);
			if (machine != null) {
				// 中班
				Date midClassStartTime = publisSchedule.stream().map(MaterialScheduleResultVo::getMidExpectFinishTime)
						.filter(Objects::nonNull).max(Date::compareTo).orElse(null);
				if (midClassStartTime != null) {
					midClassStartTime = DateUtils.addMinutes(midClassStartTime, materialIntervalTime);
				}
				machine.setMidClassStartTime(midClassStartTime);
				// 夜班
				Date nightClassStartTime = publisSchedule.stream()
						.map(MaterialScheduleResultVo::getNightExpectFinishTime).filter(Objects::nonNull)
						.max(Date::compareTo).orElse(null);
				if (nightClassStartTime != null) {
					nightClassStartTime = DateUtils.addMinutes(nightClassStartTime, materialIntervalTime);
				}
				machine.setNightClassStartTime(nightClassStartTime);
				// 白班
				Date dayClassStartTime = publisSchedule.stream().map(MaterialScheduleResultVo::getDayExpectFinishTime)
						.filter(Objects::nonNull).max(Date::compareTo).orElse(null);
				if (dayClassStartTime != null) {
					dayClassStartTime = DateUtils.addMinutes(dayClassStartTime, materialIntervalTime);
				}
				machine.setDayClassStartTime(dayClassStartTime);
			}
		}
	}

    /**
     * 自动排程前已发布的排程记录，且要将各班的时间往前挪排产时间往前
     * @param scheduleDate	排产日期
     * @param mixArea	密炼区
     * @param batchNo	排产批次
     * @param materialIntervalTime	每个排程之间的间隔时长
     * @return
     */
	private List<MaterialScheduleResultVo> listPublishSchedule(Date scheduleDate, String mixArea, String batchNo,
			Map<String, LhflMachine> machineMap, Map<String, String> params) {
		int materialIntervalTime = getInt(params.get(EngineConstants.MATERIAL_INTERVAL_TIME)); // 不同规格之间的排程间隔时间
		int defaultCapacity = getInt(params.get(EngineConstants.MACHINE_DEFAULT_CAPACITY)); // 机台默认产能（车/时）
		int dinnerTime = getInt(params.get(EngineConstants.DINNER_TIME)); // 用餐时间

		// 取出曾经发布成功的记录
		MaterialScheduleResult scheduleParams = new MaterialScheduleResult();
		scheduleParams.setScheduleDate(scheduleDate);
		scheduleParams.setMixArea(mixArea);
		scheduleParams.setPublishSuccessCount(1);
		List<MaterialScheduleResultVo> publishScheduleList = materialEngineMapper.listMaterialSchedule(scheduleParams,
				null);

		// 按机台对已发布明细分好组
		Map<String, List<MaterialScheduleResultVo>> publishMap = publishScheduleList.stream()
				.collect(Collectors.groupingBy(MaterialScheduleResultVo::getMachineCode));
		for (List<MaterialScheduleResultVo> publishList : publishMap.values()) {
			publishList.forEach(schedule -> {
				schedule.setId(null);
				schedule.setBaseValue(null);
				schedule.setBatchNo(batchNo);
				schedule.setReleaseStatus(ZltConstant.WAIT_RELEASING);
				// 填充默认开班时间
				this.countClassWeightTime(schedule, machineMap, defaultCapacity, dinnerTime);
			});
			// 直接全部重算每个已发布计划每一班的时间
			materialClassEngineService.modifyMidProduceOrder(publishList, materialIntervalTime);
			materialClassEngineService.modifyNightProduceOrder(publishList, materialIntervalTime);
			materialClassEngineService.modifyDayProduceOrder(publishList, materialIntervalTime);
		}
		this.countMachineClassTime(publishMap, machineMap, materialIntervalTime); // 将已发布的排产记录结束时间作为机台的开班时间
		return publishScheduleList;
	}

    /**
     * 将安全库存列表合并至《密炼区+物料名称》列表中
     * @param areaMaterialList	密炼区+物料名称列表
     * @param safeStockScheduleList	安全库存列表
     * @param scheduleList	终炼母炼需求列表
     * @param scheduleDate	排产日
     * @param mixArea	密炼区
     */
	private void mergeSafeStockList(List<MaterialAreaMachineVo> areaMaterialList,
			List<MaterialScheduleResultVo> safeStockScheduleList, List<MaterialScheduleResultVo> scheduleList,
			Date scheduleDate, String mixArea) {
		String glueScheduleBatchNo = scheduleList.stream().map(MaterialScheduleResultVo::getGlueScheduleBatchNo).filter(Objects::nonNull).findAny().orElse(null);
		for (MaterialScheduleResultVo safeStock: safeStockScheduleList) {// 将安全库存备库物料添加到排产物料列表中
			safeStock.setMixArea(mixArea);
			safeStock.setScheduleDate(scheduleDate);
			safeStock.setGlueMidPlanQty(0D);
			safeStock.setGlueNightPlanQty(0D);
			safeStock.setGlueDayPlanQty(0D);
			safeStock.setGlueScheduleBatchNo(glueScheduleBatchNo);
			String materialName = safeStock.getMaterialName();
			if (areaMaterialList.stream().noneMatch(r -> Objects.equals(materialName, r.getMaterialName()))) {
				areaMaterialList.add(new MaterialAreaMachineVo(mixArea, null, materialName));
			}
		}
	}

    /**
     * 自动排程后，根据跨区设置表，自动生产相应的跨区发送和接收记录
     * @param mixArea 排程密炼区
     * @param scheduleDate  排程日期
     * @return
     */
    public MaterialSpanVo autoCreateSpanRecord(String mixArea, Date scheduleDate) {
        List<MaterialSpanSend> spanSendList = materialSpanEngineService.listAutoLhflSpanSetting(mixArea, scheduleDate); //询出需要委托其他密炼区生产的硫磺辅料信息
        if(spanSendList.isEmpty()) {
            return null;
        }

        MaterialSpanVo materialSpanVo = new MaterialSpanVo();
        List<MaterialSpanReceive> spanReceiveList = new ArrayList<>();  //跨区接收记录对象
        List<MaterialAreaMachineVo> areaMaterialList = spanSendList.stream().map(r->new MaterialAreaMachineVo(r.getMaterialName())).collect(Collectors.toList()); //物料名称
        Map<String, String> materialMachineMap = machineEngineService.mapMaterialMachine(areaMaterialList);  //硫磺辅料 与 辅料机对应关系map
        Map<String, MesPmtRecipe> recipeVersionMap =  recipeEngineService.mapLhflRecipeVersionInfo(areaMaterialList);  //硫磺辅料配方版本map

        for(int i = 0; i < spanSendList.size(); i++) {
            MaterialSpanSend materialSpanSend = spanSendList.get(i);
            materialSpanSend.setBaseValue(null);
            materialSpanSend.setSendPerson(materialSpanSend.getCreateBy());
            materialSpanSend.setReceiveStatus(ZltConstant.RECEIVE_STATUS_NO);  //接收状态：未接收
            materialSpanSend.setIsAuto(ZltConstant.IS_AUTO_YES);

            //创建跨区接收对象
            MaterialSpanReceive materialSpanReceive = new MaterialSpanReceive();
            BeanUtils.copyProperties(materialSpanSend, materialSpanReceive);
            //默认设置接收数量 = 发送数据量
//            materialSpanReceive.setMidReceiveQty(materialSpanReceive.getMidSendQty());
//            materialSpanReceive.setNightReceiveQty(materialSpanReceive.getNightSendQty());
//            materialSpanReceive.setDayReceiveQty(materialSpanReceive.getDaySendQty());
            //设置被委托密炼那的默认机台和配方信息
            String machineKey = materialSpanReceive.getEntrustedMixArea() + materialSpanReceive.getMaterialName();
            String machineCode = materialMachineMap.get(machineKey + EngineConstants.CLASS_MID);
            if (machineCode == null) {
            	machineCode = materialMachineMap.get(machineKey + EngineConstants.CLASS_NIGHT);
            }
            if (machineCode == null) {
            	machineCode = materialMachineMap.getOrDefault(machineKey + EngineConstants.CLASS_DAY, "");
            }
            materialSpanReceive.setMachineCode(machineCode);
            this.fitRecipeVersion(materialSpanReceive, recipeVersionMap);   //设置配方版本信息
            spanReceiveList.add(materialSpanReceive);
        }
        materialSpanVo.setSpanSendList(spanSendList);
        materialSpanVo.setSpanReceiveList(spanReceiveList);
        return materialSpanVo;
    }

    /**
     * 批量导入引擎接口
     * @param scheduleDate
     * @param mixArea
     * @param list
     */
    public void batchAddEngineSchedule(Date scheduleDate, String mixArea, List<MaterialScheduleResult> list) {
        if(list.isEmpty()) {
            return;
        }
        List<MaterialScheduleResultVo> scheduleList = new ArrayList<>();
        List<MaterialAreaMachineVo> areaMaterialList = list.stream().map(r->new MaterialAreaMachineVo(r.getMixArea(), r.getMachineCode(), r.getMaterialName())).collect(Collectors.toList()); //密炼区+物料名称
        Map<String, String> params = paramsEngineService.mapLhflParams(mixArea);   //硫磺辅料参数设置map
        Map<String, Double> totalStockMap = stockEngineService.mapMaterialStock(scheduleDate, areaMaterialList);   //硫磺辅料总库存map
        Map<String, Double> stockMap = new HashMap<>();   //硫磺辅料库存map
        stockMap.putAll(totalStockMap);
        Map<String, LhflMachine> machineMap = machineEngineService.mapLhflMachineInfo(mixArea);  //辅料称重机信息map
        Map<String, String> commonlyUsedMap = this.mapCommonlyUsedMaterial(mixArea, scheduleDate, areaMaterialList, getInt(params.get(EngineConstants.COMMONLY_USED_DAY)));  //常用规格map
        Map<String, String> materialMap = recipeEngineService.mapBasMaterial(areaMaterialList);  //获取物料名称和物料编号的map
        Map<String, MaterialScheduleResultVo> commonUsePlanMap = new HashMap<>();  //常用规格总计划量
        String batchNo = this.materialEngineMapper.queryMaterialBatchNo(mixArea, scheduleDate);
        if(StringUtils.isBlank(batchNo)) {
            batchNo = incrementService.getSequence3(EngineConstants.MATERIAL_SCHEDULE_PREFIX + mixArea + DateUtil.formatDateYmd(scheduleDate));  //创建批次号
        }

        for(MaterialScheduleResult scheduleEntity : list) {
            MaterialScheduleResultVo schedule = new MaterialScheduleResultVo();
            BeanUtils.copyProperties(scheduleEntity, schedule);
            schedule.setScheduleDate(scheduleDate);  //排产日期
            String materialName = schedule.getMaterialName(); //物料名称
            schedule.setBatchNo(batchNo);
            schedule.setOrderNo(incrementService.getSequence4(batchNo));
            schedule.setBaseValue(null);   //基本信息
            schedule.setDataSource(ZltConstant.MATERIAL_SCHEDULE_SOURCE_IMPORT);   //数据来源
            schedule.setReleaseStatus(ZltConstant.NO_RELEASE);   //发布状态
            schedule.setPublishSuccessCount(0);
            schedule.setMaterialCode(materialMap.get(schedule.getMaterialName()));  //根据物料名称，获取物料编号
            schedule.setRecipeMaterialCode(schedule.getMaterialCode());
            schedule.setMidPlanQty(schedule.getMidPlanQty() == null ? 0D : schedule.getMidPlanQty());
            schedule.setNightPlanQty(schedule.getNightPlanQty() == null ? 0D : schedule.getNightPlanQty());
            schedule.setDayPlanQty(schedule.getDayPlanQty() == null ? 0D : schedule.getDayPlanQty());
            schedule.setStockQty(stockMap.getOrDefault(mixArea + materialName, 0D));  //设置库存
            schedule.setCommonlyUsed((commonlyUsedMap.get(schedule.getMaterialName()) == null) ? COMMONLY_USED_NO : COMMONLY_USED_YES);  //设置是否为常用规格
            schedule.setDemandQty(BigDecimalUtil.add(getDouble(schedule.getMidPlanQty()), getDouble(schedule.getNightPlanQty()), getDouble(schedule.getDayPlanQty())));  //重新计算需求量
            schedule.setTotalPlanQty(BigDecimalUtil.add(schedule.getMidPlanQty(), schedule.getNightPlanQty(), schedule.getDayPlanQty()));
            if(commonlyUsedMap.get(schedule.getMaterialName()) != null) {
                commonUsePlanMap.put(materialName, schedule);   //保存常用规格对象
            }
            this.fitDemandPlanning(schedule);   //设置【需求计划】
            this.countClassWeightTime(schedule, machineMap, getInt(params.get(EngineConstants.MACHINE_DEFAULT_CAPACITY)), getInt(params.get(EngineConstants.DINNER_TIME)));   //根据机台产能，计算硫磺辅料称重 需要的时间戳（同时计算出【机台班制】，【单车称重消耗时间（毫秒）】）
            this.actualWeightForGlue(schedule, getInt(params.get(EngineConstants.PRE_PREPARE_TIME)));  //计算称重预计开始时间（胶料开始生产前，至少要提前{prePrepareTime}分，进行硫磺辅料称重）
            scheduleList.add(schedule);
        }

        //把排程记录按机台进行分组
        Map<String, List<MaterialScheduleResultVo>> scheduleMap = scheduleList.stream().collect(Collectors.groupingBy(r->r.getMachineCode()));
        scheduleList.clear();
        for(Map.Entry<String, List<MaterialScheduleResultVo>> entry : scheduleMap.entrySet()) {
            List<MaterialScheduleResultVo> scheduleGroupList = entry.getValue();
            materialClassEngineService.modifyMidProduceOrder(scheduleGroupList, getInt(params.get(EngineConstants.MATERIAL_INTERVAL_TIME)));  //重新计算中班预计完成时间
            materialClassEngineService.modifyNightProduceOrder(scheduleGroupList, getInt(params.get(EngineConstants.MATERIAL_INTERVAL_TIME)));  //重新计算夜班预计完成时间
            materialClassEngineService.modifyDayProduceOrder(scheduleGroupList, getInt(params.get(EngineConstants.MATERIAL_INTERVAL_TIME)));  //重新计算白班预计完成时间
            scheduleList.addAll(scheduleGroupList);
        }

        //同步硫磺辅料排程记录到日志表中
        this.synclueScheduleToLog(scheduleDate, mixArea);
        //把最终的硫磺辅料排程记录 进行入库
        materialEngineMapper.batchInsertMaterialSchedule(scheduleList);

        // 处理排程初始日志
        materialEngineMapper.deleteMaterialInitLog(scheduleDate, mixArea);
        materialEngineMapper.syncMaterialScheduleToInitLog(scheduleDate, mixArea);
    }

    /**
     * 插单引擎接口（插后重新刷新同一个机台下的 预计完成时间）
     * @param schedule
     * @return
     */
    public List<MaterialScheduleResult> addEngineSchedule(MaterialScheduleResult schedule) {
        String mixArea = schedule.getMixArea();
        String machineCode = schedule.getMachineCode();
        String materialName = schedule.getMaterialName();
        Date scheduleDate = schedule.getScheduleDate();
        List<MaterialScheduleResult> resultList = new ArrayList<>();

        List<MaterialAreaMachineVo> areaMaterialList = new ArrayList<>();
        areaMaterialList.add(new MaterialAreaMachineVo(mixArea, machineCode, schedule.getMaterialName()));
        Map<String, Double> totalStockMap = stockEngineService.mapMaterialStock(scheduleDate, areaMaterialList);   //硫磺辅料总库存map
        Map<String, LhflMachine> machineMap = machineEngineService.mapLhflMachineInfo(mixArea);  //辅料称重机信息map
        Map<String, String> params = paramsEngineService.mapLhflParams(mixArea);   //硫磺辅料参数设置map
        Map<String, String> commonlyUsedMap = this.mapCommonlyUsedMaterial(mixArea, scheduleDate, areaMaterialList, getInt(params.get(EngineConstants.COMMONLY_USED_DAY)));  //常用规格map
        Map<String, String> materialMap = recipeEngineService.mapBasMaterial(areaMaterialList);  //获取物料名称和物料编号的map
        String batchNo = this.materialEngineMapper.queryMaterialBatchNo(mixArea, scheduleDate);
        if(StringUtils.isBlank(batchNo)) {
            batchNo = incrementService.getSequence3(EngineConstants.MATERIAL_SCHEDULE_PREFIX + mixArea + DateUtil.formatDateYmd(scheduleDate));  //创建批次号
        }
        schedule.setBatchNo(batchNo);
        schedule.setOrderNo(incrementService.getSequence4(batchNo));
        schedule.setBaseValue(null);   //基本信息
        schedule.setReleaseStatus(ZltConstant.NO_RELEASE);   //发布状态
        schedule.setDataSource(ZltConstant.MATERIAL_SCHEDULE_SOURCE_ADD);   //数据来源
        schedule.setPublishSuccessCount(0);
        schedule.setMaterialCode(materialMap.get(schedule.getMaterialName()));  //根据物料名称，获取物料编号
        schedule.setRecipeMaterialCode(schedule.getMaterialCode());
        schedule.setMidPlanQty(schedule.getMidPlanQty() == null ? 0D : schedule.getMidPlanQty());
        schedule.setNightPlanQty(schedule.getNightPlanQty() == null ? 0D : schedule.getNightPlanQty());
        schedule.setDayPlanQty(schedule.getDayPlanQty() == null ? 0D : schedule.getDayPlanQty());
        schedule.setStockQty(totalStockMap.getOrDefault(mixArea + materialName, 0D));  //设置库存
        schedule.setCommonlyUsed((commonlyUsedMap.get(schedule.getMaterialName()) == null) ? COMMONLY_USED_NO : COMMONLY_USED_YES);  //设置是否为常用规格
        schedule.setTotalPlanQty(BigDecimalUtil.add(getDouble(schedule.getMidPlanQty()), getDouble(schedule.getNightPlanQty()), getDouble(schedule.getDayPlanQty())));  //重新计算总计划

        List<MaterialScheduleResultVo> scheduleList = materialEngineMapper.listMaterialSchedule(schedule, Arrays.asList(new String[]{machineCode})); //查询出和被修改的排程相同机台的记录
        MaterialScheduleResultVo addScheduleVo = new MaterialScheduleResultVo();
        BeanUtils.copyProperties(schedule, addScheduleVo);
        scheduleList.add(addScheduleVo);

        for(MaterialScheduleResultVo scheduleVo : scheduleList) {
            this.countClassWeightTime(scheduleVo, machineMap, getInt(params.get(EngineConstants.MACHINE_DEFAULT_CAPACITY)), getInt(params.get(EngineConstants.DINNER_TIME)));   //根据机台产能，计算硫磺辅料称重 需要的时间戳（同时计算出【机台班制】，【单车称重消耗时间（毫秒）】）
            this.actualWeightForGlue(scheduleVo, getInt(params.get(EngineConstants.PRE_PREPARE_TIME)));  //计算称重预计开始时间（胶料开始生产前，至少要提前{prePrepareTime}分，进行硫磺辅料称重）
        }

        materialClassEngineService.modifyMidProduceOrder(scheduleList, getInt(params.get(EngineConstants.MATERIAL_INTERVAL_TIME)));  //重新计算中班预计完成时间
        materialClassEngineService.modifyNightProduceOrder(scheduleList, getInt(params.get(EngineConstants.MATERIAL_INTERVAL_TIME)));  //重新计算夜班预计完成时间
        materialClassEngineService.modifyDayProduceOrder(scheduleList, getInt(params.get(EngineConstants.MATERIAL_INTERVAL_TIME)));  //重新计算白班预计完成时间
        scheduleList.forEach(r->{
            MaterialScheduleResult result = r;
            resultList.add(r);
        });
        return resultList;
    }

    /**
     * 修改了各班计划量，修改了顺序。都需要把此机台下的排产重新进行计算
     * @param oldSchedule  修改前的排产信息
     * @param newSchedule  修改后的排产信息
     */
    public List<MaterialScheduleResult> retrySchedule(MaterialScheduleResult oldSchedule, MaterialScheduleResult newSchedule) {
        List<MaterialScheduleResult> resultList = new ArrayList<>();
        String mixArea = newSchedule.getMixArea();
        String machineName = oldSchedule.getMachineName();
        List<String> machineList = new ArrayList<>();
        machineList.add(newSchedule.getMachineCode());
        if(StringUtils.isNotBlank(oldSchedule.getMachineCode()) && !newSchedule.getMachineCode().equals(oldSchedule.getMachineCode())) {
            machineList.add(oldSchedule.getMachineCode());
        }
        List<MaterialScheduleResultVo> scheduleList = materialEngineMapper.listMaterialSchedule(newSchedule, machineList); //查询出和被修改的排程相同机台的记录
        if(scheduleList.isEmpty()) {
           return resultList;
        }
        Map<String, String> params = paramsEngineService.mapLhflParams(mixArea);   //硫磺辅料参数设置map
        Map<String, LhflMachine> machineMap = machineEngineService.mapLhflMachineInfo(mixArea);  //辅料称重机信息map
        newSchedule.setTotalPlanQty(BigDecimalUtil.add(getDouble(newSchedule.getMidPlanQty()), getDouble(newSchedule.getNightPlanQty()), getDouble(newSchedule.getDayPlanQty())));  //重新计算总计划

        for(MaterialScheduleResultVo schedule : scheduleList) {
            this.countClassWeightTime(schedule, machineMap, getInt(params.get(EngineConstants.MACHINE_DEFAULT_CAPACITY)), getInt(params.get(EngineConstants.DINNER_TIME)));   //根据机台产能，计算硫磺辅料称重 需要的时间戳（同时计算出【机台班制】，【单车称重消耗时间（毫秒）】）
            this.actualWeightForGlue(schedule, getInt(params.get(EngineConstants.PRE_PREPARE_TIME)));  //计算称重预计开始时间（胶料开始生产前，至少要提前{prePrepareTime}分，进行硫磺辅料称重）
        }

        //修改了生产顺序 或者 进行了调量（start）
        if(!compare(oldSchedule.getMidProduceOrder(), newSchedule.getMidProduceOrder()) || !compare(oldSchedule.getMidPlanQty(), newSchedule.getMidPlanQty()) ) {  //修改了中班生顺序或中班计划量
            materialClassEngineService.modifyMidProduceOrder(scheduleList, getInt(params.get(EngineConstants.MATERIAL_INTERVAL_TIME)));
        }
        if(!compare(oldSchedule.getNightProduceOrder(), newSchedule.getNightProduceOrder()) || !compare(oldSchedule.getNightPlanQty(), newSchedule.getNightPlanQty())) {  //修改了夜班生产顺序或夜班计划量
            materialClassEngineService.modifyNightProduceOrder(scheduleList, getInt(params.get(EngineConstants.MATERIAL_INTERVAL_TIME)));
        }
        if(!compare(oldSchedule.getDayProduceOrder(), newSchedule.getDayProduceOrder())|| !compare(oldSchedule.getDayPlanQty(), newSchedule.getDayPlanQty())) {  //修改了白班生产顺或白班计划量
            materialClassEngineService.modifyDayProduceOrder(scheduleList, getInt(params.get(EngineConstants.MATERIAL_INTERVAL_TIME)));
        }
        //修改了生产顺序 或者 进行了调量（end）

//        //做了转机台”操作
//        if(!compare(oldSchedule.getMachineCode(), newSchedule.getMachineCode())) {
//            materialClassEngineService.modifyMachine(scheduleList, getInt(params.get(EngineConstants.MATERIAL_INTERVAL_TIME)));
//        }
        scheduleList.forEach(r->{
            if(r.getMidPlanQty() == null || r.getMidPlanQty() == 0) {
                r.setMidExpectStartTime(null);
                r.setMidExpectFinishTime(null);
            }
            if(r.getNightPlanQty() == null || r.getNightPlanQty() == 0) {
                r.setNightExpectStartTime(null);
                r.setNightExpectFinishTime(null);
            }
            if(r.getDayPlanQty() == null || r.getDayPlanQty() == 0) {
                r.setDayExpectStartTime(null);
                r.setDayExpectFinishTime(null);
            }
            r.setMachineName(machineName);  //回显前端的机台名称
            resultList.add(r);
        });
        return resultList;
    }

    /**
     * 转机台后默认把转机台的排产放到最后，并重新计算顺序和预计完成时间
     * @param oldSchedule  转机台前的排产记录
     * @param schedule 排程记录
     * @return
     */
    public MaterialScheduleResult retryMachine(MaterialScheduleResult oldSchedule, MaterialScheduleResult schedule) {
        String mixArea = schedule.getMixArea();
        List<MaterialAreaMachineVo> areaMaterialList = new ArrayList<>();
        Map<String, String> params = paramsEngineService.mapLhflParams(mixArea);   //硫磺辅料参数设置map
        Map<String, LhflMachine> machineMap = machineEngineService.mapLhflMachineInfo(mixArea);  //辅料称重机信息map
        areaMaterialList.add(new MaterialAreaMachineVo(mixArea, schedule.getMachineCode(), schedule.getMaterialName()));
        Map<String, MesPmtRecipe> recipeVersionMap =  recipeEngineService.mapLhflRecipeVersionInfo(areaMaterialList);  //获取新机台的硫磺辅料配方版本map
        this.fitRecipeVersion(schedule, recipeVersionMap);  //转机台后重新设置配方版本信息

        Integer oldClassShift = oldSchedule.getClassShift();
        if(oldClassShift == null) {
            LhflMachine oldMachine = machineMap.get(oldSchedule.getMachineCode());
            oldClassShift = (oldMachine != null ? oldMachine.getClassShift() : EngineConstants.CLASS_SHIFT_THREE);  //旧机台班制
        }

        MaterialScheduleResultVo scheduleVo = new MaterialScheduleResultVo();
        BeanUtils.copyProperties(schedule, scheduleVo);
        this.setScheduleMachineInfo(scheduleVo);   //从现有的排产记录中拿到机台的产能 和 班制（不直接从机台表拿，是因为此时机台表的产能和班制肯已经被修改了）
        this.countClassWeightTime(scheduleVo, machineMap, getInt(params.get(EngineConstants.MACHINE_DEFAULT_CAPACITY)), getInt(params.get(EngineConstants.DINNER_TIME)));   //根据机台产能，计算硫磺辅料称重 需要的时间戳（同时计算出【机台班制】，【单车称重消耗时间（毫秒）】）
        this.actualWeightForGlue(scheduleVo, getInt(params.get(EngineConstants.PRE_PREPARE_TIME)));  //计算称重预计开始时间（胶料开始生产前，至少要提前{prePrepareTime}分，进行硫磺辅料称重）

        MaterialScheduleResult maxSchedule = materialEngineMapper.maxMachineOrderAndFinishTime(schedule);  //查询出机台下各班最大的顺序 和 预计完成时间
        maxSchedule = (maxSchedule == null ? new MaterialScheduleResult() : maxSchedule);
        //新计算顺序和预计完成时间
        materialClassEngineService.retryMachine(scheduleVo, maxSchedule, getInt(params.get(EngineConstants.MATERIAL_INTERVAL_TIME)), oldClassShift);
        schedule = scheduleVo;
        return schedule;
    }

    /**
     * 转机台（新）。转机台后，创建新的排产记录；之前的记录保留。新机台上的各班计划量=原计划量 -  完成量
     * @param oldSchedule  转机台前的排产记录
     * @param schedule 排程记录
     * @return
     */
    public List<MaterialScheduleResult> retryMachineNew(MaterialScheduleResult oldSchedule, MaterialScheduleResult schedule) {
        String mixArea = oldSchedule.getMixArea();
        Integer oldClassShift = oldSchedule.getClassShift();  //旧机台的班制
        String oldMachineCode = oldSchedule.getMachineCode(); //旧机台编号
        String newMachineCode = schedule.getMachineCode();  //新机台编号
        Map<String, String> params = paramsEngineService.mapLhflParams(mixArea);   //硫磺辅料参数设置map
        Map<String, LhflMachine> machineMap = machineEngineService.mapLhflMachineInfo(mixArea);  //辅料称重机信息map

        MaterialScheduleResultVo newScheduleVo = this.createChangeMachineEntity(oldSchedule, schedule);  //转机台后的排产记录,并修改旧机台上此规格计划量
        this.setScheduleMachineInfo(newScheduleVo);   //从现有的排产记录中拿到机台的产能 和 班制（不直接从机台表拿，是因为此时机台表的产能和班制肯已经被修改了）
        this.countClassWeightTime(newScheduleVo, machineMap, getInt(params.get(EngineConstants.MACHINE_DEFAULT_CAPACITY)), getInt(params.get(EngineConstants.DINNER_TIME)));   //根据机台产能，计算硫磺辅料称重 需要的时间戳（同时计算出【机台班制】，【单车称重消耗时间（毫秒）】）
        this.actualWeightForGlue(newScheduleVo, getInt(params.get(EngineConstants.PRE_PREPARE_TIME)));  //计算称重预计开始时间（胶料开始生产前，至少要提前{prePrepareTime}分，进行硫磺辅料称重）
        materialClassEngineService.transferClassShiftPlan(newScheduleVo, oldClassShift);  //转机台时，前后机台的班制不一样时，需要根据规则把计划量合并到新机台的班次中

        List<MaterialScheduleResultVo> scheduleList = materialEngineMapper.listMaterialSchedule(newScheduleVo, Arrays.asList(new String[]{oldMachineCode, newMachineCode})); //查询出和被修改的排程相同机台的记录
        this.checkProduceOrderRepeat(schedule, scheduleList.stream().filter(s -> s.getMachineCode().equals(newMachineCode)).collect(Collectors.toList())); // 校验不能与新机台的顺序重复
        
        for(MaterialScheduleResultVo scheduleVo : scheduleList) {
            if(scheduleVo.getId().longValue() == oldSchedule.getId().longValue()) {
                BeanUtils.copyProperties(oldSchedule, scheduleVo);
            }
            this.countClassWeightTime(scheduleVo, machineMap, getInt(params.get(EngineConstants.MACHINE_DEFAULT_CAPACITY)), getInt(params.get(EngineConstants.DINNER_TIME)));   //根据机台产能，计算硫磺辅料称重 需要的时间戳（同时计算出【机台班制】，【单车称重消耗时间（毫秒）】）
            this.actualWeightForGlue(scheduleVo, getInt(params.get(EngineConstants.PRE_PREPARE_TIME)));  //计算称重预计开始时间（胶料开始生产前，至少要提前{prePrepareTime}分，进行硫磺辅料称重）
        }
        scheduleList.add(newScheduleVo);

        Map<String, List<MaterialScheduleResultVo>> machineScheduleMap = scheduleList.stream().collect(Collectors.groupingBy(MaterialScheduleResultVo::getMachineCode));
        List<MaterialScheduleResult> resultList = new ArrayList<>();  //返回结果集合
        machineScheduleMap.forEach((k, v) -> {
            materialClassEngineService.modifyMidProduceOrder(v, getInt(params.get(EngineConstants.MATERIAL_INTERVAL_TIME)));  //重新计算中班预计完成时间
            materialClassEngineService.modifyNightProduceOrder(v, getInt(params.get(EngineConstants.MATERIAL_INTERVAL_TIME)));  //重新计算夜班预计完成时间
            materialClassEngineService.modifyDayProduceOrder(v, getInt(params.get(EngineConstants.MATERIAL_INTERVAL_TIME)));  //重新计算白班预计完成时间
            resultList.addAll(v);
        });
        //把计划量为0的 相关字段置空
        resultList.forEach(r->{
            if(r.getMidPlanQty() == null || r.getMidPlanQty() == 0) {
                materialClassEngineService.clearClassField(r, EngineConstants.CLASS_MID);
            }
            if(r.getNightPlanQty() == null || r.getNightPlanQty() == 0) {
                materialClassEngineService.clearClassField(r, EngineConstants.CLASS_NIGHT);
            }
            if(r.getDayPlanQty() == null || r.getDayPlanQty() == 0) {
                materialClassEngineService.clearClassField(r, EngineConstants.CLASS_DAY);
            }
        });
        return resultList;
    }
    

	/**
	 * 检查转机台是否有顺序重复的记录，有重复直接返回错误提示
	 * 
	 * @param scheduleResultVo
	 * @param machineScheduleList
	 */
    @Override
	public void checkProduceOrderRepeat(MaterialScheduleResult scheduleResultVo,
			List<MaterialScheduleResultVo> machineScheduleList) {
		if (scheduleResultVo.getMidPlanQty() != null && scheduleResultVo.getMidPlanQty() > 0
				&& scheduleResultVo.getMidProduceOrder() != null) {
			Integer produceOrder = scheduleResultVo.getMidProduceOrder();
			if (machineScheduleList.stream().anyMatch(
					s -> s.getMidProduceOrder() != null && produceOrder.compareTo(s.getMidProduceOrder()) == 0)) {
				throw new RuntimeException("中班生产顺序重复，无法转机台！");
			}
		}
		if (scheduleResultVo.getNightPlanQty() != null && scheduleResultVo.getNightPlanQty() > 0
				&& scheduleResultVo.getNightProduceOrder() != null) {
			Integer produceOrder = scheduleResultVo.getNightProduceOrder();
			if (machineScheduleList.stream().anyMatch(
					s -> s.getNightProduceOrder() != null && produceOrder.compareTo(s.getNightProduceOrder()) == 0)) {
				throw new RuntimeException("夜班生产顺序重复，无法转机台！");
			}
		}
		if (scheduleResultVo.getDayPlanQty() != null && scheduleResultVo.getDayPlanQty() > 0
				&& scheduleResultVo.getDayProduceOrder() != null) {
			Integer produceOrder = scheduleResultVo.getDayProduceOrder();
			if (machineScheduleList.stream().anyMatch(
					s -> s.getDayProduceOrder() != null && produceOrder.compareTo(s.getDayProduceOrder()) == 0)) {
				throw new RuntimeException("白班生产顺序重复，无法转机台！");
			}
		}
	}

    /**
     * //转机台后的排产记录,并修改旧机台上此规格计划量
     * @param oldSchedule 转机台前的排产信息
     * @param schedule 页面传过来的新机台的排产信息
     * @return
     */
    private MaterialScheduleResultVo createChangeMachineEntity(MaterialScheduleResult oldSchedule, MaterialScheduleResult schedule) {
        MaterialScheduleResultVo newScheduleVo = new MaterialScheduleResultVo();  //转机台后的排产记录
        BeanUtils.copyProperties(oldSchedule, newScheduleVo);
        newScheduleVo.setId(null);
        newScheduleVo.setBaseValue(null);
        newScheduleVo.setOrderNo(incrementService.getSequence4(newScheduleVo.getBatchNo())); //重新创建订单号
        newScheduleVo.setMachineCode(schedule.getMachineCode());  //设置新机台
        newScheduleVo.setRecipeType(schedule.getRecipeType());  //设置新配方类型
        newScheduleVo.setRecipeVersionId(schedule.getRecipeVersionId());  //设置新配方版本号
        newScheduleVo.setRecipeStage(schedule.getRecipeStage());  //配方阶段
        newScheduleVo.setReleaseStatus(ZltConstant.NO_RELEASE);   //发布状态
        newScheduleVo.setDataSource(ZltConstant.MATERIAL_SCHEDULE_SOURCE_MACHINE);   //数据来源:转机台
        newScheduleVo.setMidProduceOrder(schedule.getMidProduceOrder());   //中班顺序
        newScheduleVo.setNightProduceOrder(schedule.getNightProduceOrder()); //夜班顺序
        newScheduleVo.setDayProduceOrder(schedule.getDayProduceOrder());  //白班舒心
        newScheduleVo.setPublishSuccessCount(0);
        Double midPlanQty = BigDecimalUtil.sub(oldSchedule.getMidPlanQty(), oldSchedule.getMidFinishQty());  //新机台中班计划量
        Double nightPlanQty = BigDecimalUtil.sub(oldSchedule.getNightPlanQty(), oldSchedule.getNightFinishQty());  //新机台夜班计划量
        Double dayPlanQty = BigDecimalUtil.sub(oldSchedule.getDayPlanQty(), oldSchedule.getDayFinishQty());  //新机台白班计划量
        newScheduleVo.setMidPlanQty(midPlanQty < 0 ? 0 : midPlanQty);
        newScheduleVo.setNightPlanQty(nightPlanQty < 0 ? 0 : nightPlanQty);
        newScheduleVo.setDayPlanQty(dayPlanQty < 0 ? 0 : dayPlanQty);
        newScheduleVo.setTotalPlanQty(BigDecimalUtil.add(getDouble(newScheduleVo.getMidPlanQty()), getDouble(newScheduleVo.getNightPlanQty()), getDouble(newScheduleVo.getDayPlanQty())));  //重新计算总计划

        //重新计算旧排产的计划量
        oldSchedule.setMidPlanQty(oldSchedule.getMidFinishQty());
        oldSchedule.setNightPlanQty(oldSchedule.getNightFinishQty());
        oldSchedule.setDayPlanQty(oldSchedule.getDayFinishQty());
        oldSchedule.setTotalPlanQty(BigDecimalUtil.add(getDouble(oldSchedule.getMidPlanQty()), getDouble(oldSchedule.getNightPlanQty()), getDouble(oldSchedule.getDayPlanQty())));  //重新计算总计划
        return newScheduleVo;
    }

    /**
     * 当密炼区当天已经进行了硫磺辅料自动排程后，再去接收跨区的硫磺辅料的生产计划，此时接收的数据都会被安排到对应机台的最后去
     * @param mixArea  密炼区
     * @param scheduleDate  排程日期
     * @param receiveIds  跨区接收列表
     * @return
     */
    public List<MaterialScheduleResult> spanReceivedEngine(String mixArea, Date scheduleDate, List<Long> receiveIds) {
        String batchNo = this.materialEngineMapper.queryMaterialBatchNo(mixArea, scheduleDate);  //查询批次号
        if(StringUtils.isBlank(batchNo)) {
            //批次号为空，说明当天此密炼区，还未进行自动排程。
            return null;
        }

        List<MaterialScheduleResult> resultList = new ArrayList<>();
		List<MaterialScheduleResultVo> scheduleList = materialEngineMapper.listSpanReceiveByIds(receiveIds); // 合并跨区计划，需要处理
		scheduleList = materialClassEngineService.mergeSubtractSpanSchedule(scheduleList); // 如果有扣减需求，需要合并扣减量
		if (CollectionUtils.isEmpty(scheduleList)) { // 合并后可能新增需求与扣减需求刚好全部抵消，因此不需要对现有排程变更
			return null;
		}

        MaterialScheduleResultVo scheduleParams = new MaterialScheduleResultVo();
        scheduleParams.setScheduleDate(scheduleDate);
        scheduleParams.setMixArea(mixArea);
        Map<String, List<MaterialScheduleResultVo>> materialScheduleMap = materialEngineMapper.listMaterialSchedule(scheduleParams, null).stream().collect(Collectors.groupingBy(MaterialScheduleResult::getMaterialName));
        List<MaterialAreaMachineVo> areaMaterialList = scheduleList.stream().map(r->new MaterialAreaMachineVo(r.getMixArea(), r.getMachineCode(), r.getMaterialName())).collect(Collectors.toList()); //密炼区+物料名称
        Map<String, String> params = paramsEngineService.mapLhflParams(mixArea);   //硫磺辅料参数设置map
        Map<String, Double> totalStockMap = stockEngineService.mapMaterialStock(scheduleDate, areaMaterialList);   //硫磺辅料总库存map
        Map<String, LhflMachine> machineMap = machineEngineService.mapLhflMachineInfo(mixArea);  //辅料称重机信息map
        Map<String, MaterialScheduleResult> maxScheduleMap = new HashMap<>();

        for(MaterialScheduleResultVo schedule : scheduleList) {
            String materialName = schedule.getMaterialName(); //物料名称
            schedule.setBatchNo(batchNo);
            schedule.setScheduleDate(scheduleDate);  //排产日期
            schedule.setBaseValue(null);   //基本信息
            schedule.setReleaseStatus(ZltConstant.NO_RELEASE);   //发布状态
            schedule.setPublishSuccessCount(0);
            schedule.setStockQty(totalStockMap.getOrDefault(mixArea + materialName, 0D));  //设置库存
            schedule.setOrderNo(incrementService.getSequence4(batchNo));  //创建订单号

            MaterialScheduleResultVo scheduleVo = new MaterialScheduleResultVo();
            BeanUtils.copyProperties(schedule, scheduleVo);
            this.setScheduleMachineInfo(scheduleVo);   //从现有的排产记录中拿到机台的产能 和 班制（不直接从机台表拿，是因为此时机台表的产能和班制肯已经被修改了）
            this.countClassWeightTime(scheduleVo, machineMap, getInt(params.get(EngineConstants.MACHINE_DEFAULT_CAPACITY)), getInt(params.get(EngineConstants.DINNER_TIME)));   //根据机台产能，计算硫磺辅料称重 需要的时间戳（同时计算出【机台班制】，【单车称重消耗时间（毫秒）】）
            this.actualWeightForGlue(scheduleVo, getInt(params.get(EngineConstants.PRE_PREPARE_TIME)));  //计算称重预计开始时间（胶料开始生产前，至少要提前{prePrepareTime}分，进行硫磺辅料称重）

            //查询出机台下各班最大的顺序 和 预计完成时间
            MaterialScheduleResult maxSchedule = maxScheduleMap.get(materialName);
            if(maxSchedule == null) {
                maxSchedule = materialEngineMapper.maxMachineOrderAndFinishTime(schedule);
                maxSchedule = (maxSchedule == null ? new MaterialScheduleResult() : maxSchedule);
                maxScheduleMap.put(materialName, maxSchedule);
            }
            List<MaterialScheduleResultVo> oldList = materialScheduleMap.get(materialName); // 同一物料的排程记录
            //新计算顺序和预计完成时间
            List<MaterialScheduleResultVo> modifyList = materialClassEngineService.spanReceivedClassEngine(scheduleVo, maxSchedule, oldList, getInt(params.get(EngineConstants.MATERIAL_INTERVAL_TIME)));
            maxScheduleMap.put(materialName, maxSchedule);
            
            resultList.addAll(modifyList);
        }
        return resultList;
    }

    /**
     * 从现有的排产记录中拿到机台的产能 和 班制
     * @param scheduleVo
     */
    private void setScheduleMachineInfo(MaterialScheduleResultVo scheduleVo) {
        LhflMachine scheduleMachine = this.materialEngineMapper.queryScheduleMachineInfo(scheduleVo.getMixArea(), scheduleVo.getScheduleDate(), scheduleVo.getMachineCode());
        if(scheduleMachine != null) {
            scheduleVo.setCapacity(scheduleMachine.getCapacity().intValue());
            scheduleVo.setClassShift(scheduleMachine.getClassShift());
        } else {
            scheduleVo.setCapacity(null);
            scheduleVo.setClassShift(null);
        }
    }

    /**
     * //用库存扣减各班计划量, 并计算出常用规格当日的总计划量(先扣中班，在扣夜班，在扣白班)
     * @param scheduleList
     * @param stockMap
     */
    private void reduceStock(List<MaterialScheduleResultVo> scheduleList, Map<String, Double> stockMap) {
        for(Map.Entry<String, Double> entry : stockMap.entrySet()) {
            String k = entry.getKey();
            Double stock = entry.getValue();
            //用库存扣减中班计划量
            scheduleList = scheduleList.stream().sorted(Comparator.comparing(MaterialScheduleResultVo::getGlueMidExpectStartTime, Comparator.nullsLast(Date::compareTo))).collect(Collectors.toList());
            for(MaterialScheduleResultVo schedule : scheduleList) {
                if(k.equals(schedule.getMixArea() + schedule.getMaterialName())) {
                    Double midPlanQty = schedule.getMidPlanQty();  //中班计划量
                    Double midPlanQtyInit = schedule.getMidPlanQty();  //中班计划量（计算前）
                    if(stock > 0) {
                        //扣减中班计划量
                        midPlanQty = (stock > midPlanQty) ? 0D : BigDecimalUtil.sub(midPlanQty, stock);
                        schedule.setMidPlanQty(midPlanQty);
                        schedule.setGlueMidExpectStartTime(midPlanQty > 0 ? schedule.getGlueMidExpectStartTime() : null);
                        stock = (stock > midPlanQtyInit) ? BigDecimalUtil.sub(stock, midPlanQtyInit) : 0D;
                        schedule.setTotalPlanQty(midPlanQty);   //设置总计划
                        stockMap.put(schedule.getMixArea() + schedule.getMaterialName(), stock);
                    }
                }
            }

            //用库存扣减夜班计划量
            scheduleList = scheduleList.stream().sorted(Comparator.comparing(MaterialScheduleResultVo::getGlueNightExpectStartTime, Comparator.nullsLast(Date::compareTo))).collect(Collectors.toList());
            for(MaterialScheduleResultVo schedule : scheduleList) {
                if(k.equals(schedule.getMixArea() + schedule.getMaterialName())) {
                    Double nightPlanQty = schedule.getNightPlanQty();  //夜班计划量
                    Double nightPlanQtyInit = schedule.getNightPlanQty();  //夜班计划量（计算前）
                    if(stock > 0) {
                        //扣减中班计划量
                        nightPlanQty = (stock > nightPlanQty) ? 0D : BigDecimalUtil.sub(nightPlanQty, stock);
                        schedule.setNightPlanQty(nightPlanQty);
                        schedule.setGlueNightExpectStartTime(nightPlanQty > 0 ? schedule.getGlueNightExpectStartTime() : null);
                        stock = (stock > nightPlanQtyInit) ? BigDecimalUtil.sub(stock, nightPlanQtyInit) : 0D;
                        schedule.setTotalPlanQty(nightPlanQty);   //设置总计划
                        stockMap.put(schedule.getMixArea() + schedule.getMaterialName(), stock);
                    }
                }
            }

            //用库存扣减夜班计划量
            scheduleList = scheduleList.stream().sorted(Comparator.comparing(MaterialScheduleResultVo::getGlueDayExpectStartTime, Comparator.nullsLast(Date::compareTo))).collect(Collectors.toList());
            for(MaterialScheduleResultVo schedule : scheduleList) {
                if(k.equals(schedule.getMixArea() + schedule.getMaterialName())) {
                    Double dayPlanQty = schedule.getDayPlanQty();  //白班计划量
                    Double dayPlanQtyInit = schedule.getDayPlanQty();  //白班计划量（计算前）
                    if(stock > 0) {
                        //扣减中班计划量
                        dayPlanQty = (stock > dayPlanQty) ? 0D : BigDecimalUtil.sub(dayPlanQty, stock);
                        schedule.setDayPlanQty(dayPlanQty);
                        schedule.setGlueDayExpectStartTime(dayPlanQty > 0 ? schedule.getGlueNightExpectStartTime() : null);
                        stock = (stock > dayPlanQtyInit) ? BigDecimalUtil.sub(stock, dayPlanQtyInit) : 0D;
                        schedule.setTotalPlanQty(dayPlanQty);   //设置总计划
                        stockMap.put(schedule.getMixArea() + schedule.getMaterialName(), stock);
                    }
                }
            }
        }

        scheduleList.forEach(r->r.setTotalPlanQty(BigDecimalUtil.add(r.getMidPlanQty(), r.getNightPlanQty(), r.getDayPlanQty())));
    }

    /**
     * 设置配方版本信息
     * @param schedule 硫磺辅料排程对象
     * @param recipeVersionMap  硫磺辅料配方版本map
     */
    private void fitRecipeVersion(MaterialScheduleResult schedule, Map<String, MesPmtRecipe> recipeVersionMap) {
        if(StringUtils.isBlank(schedule.getMachineCode()) || StringUtils.isBlank(schedule.getMaterialName()) || StringUtils.isNotBlank(schedule.getRecipeType())) {
            return;
        }
        MesPmtRecipe recipe = recipeVersionMap.get(schedule.getMachineCode() + schedule.getMaterialName());
        if(recipe != null) {
            schedule.setRecipeVersionId(recipe.getRecipeVersionId());  //设置配方版本号
            schedule.setRecipeType(recipe.getRecipeType());   //设置配方类型
            schedule.setRecipeStage(recipe.getProductStage());   //设置配方阶段
        }
    }

    /**
     * 设置配方版本信息
     * @param spanReceive 硫磺辅料跨区接收对象
     * @param recipeVersionMap  硫磺辅料配方版本map
     */
    private void fitRecipeVersion(MaterialSpanReceive spanReceive, Map<String, MesPmtRecipe> recipeVersionMap) {
        if(StringUtils.isBlank(spanReceive.getMachineCode()) || StringUtils.isBlank(spanReceive.getMaterialName())) {
            return;
        }
        MesPmtRecipe recipe = recipeVersionMap.get(spanReceive.getMachineCode() + spanReceive.getMaterialName());
        if(recipe != null) {
            spanReceive.setRecipeVersionId(recipe.getRecipeVersionId());  //设置配方版本号
            spanReceive.setRecipeType(recipe.getRecipeType());   //设置配方类型
            spanReceive.setRecipeStage(recipe.getProductStage());   //设置配方阶段
        }
    }

    /**
     * 设置【需求计划】字段值
     * @param schedule
     */
    private void fitDemandPlanning(MaterialScheduleResultVo schedule) {
        List<String> list = new ArrayList<>();
        if(schedule.getGlueMidExpectStartTime() != null && schedule.getGlueMidPlanQty() > 0) {
            list.add(DateUtil.formatDatetime(schedule.getGlueMidExpectStartTime()) + EngineConstants.DEMAND_PLANNING_DIVISION + schedule.getGlueMidPlanQty().intValue());
        }
        if(schedule.getGlueNightExpectStartTime() != null && schedule.getGlueNightPlanQty() > 0) {
            list.add(DateUtil.formatDatetime(schedule.getGlueNightExpectStartTime()) + EngineConstants.DEMAND_PLANNING_DIVISION + schedule.getGlueNightPlanQty().intValue());
        }
        if(schedule.getGlueDayExpectStartTime() != null && schedule.getGlueDayPlanQty() > 0) {
            list.add(DateUtil.formatDatetime(schedule.getGlueDayExpectStartTime()) + EngineConstants.DEMAND_PLANNING_DIVISION + schedule.getGlueDayPlanQty().intValue());
        }
        if(!list.isEmpty()) {
            schedule.setDemandPlanning(String.join(",", list));
        }
    }

    /**
     * 根据需求计划，把各班计划量还原到初始状态
     * @param schedule
     */
    private void reverseDemandPlanning(MaterialScheduleResultVo schedule) {
        schedule.setMidPlanQty(0D);
        schedule.setNightPlanQty(0D);
        schedule.setDayPlanQty(0D);
        String demandPlanning = schedule.getDemandPlanning();
        if(StringUtils.isBlank(demandPlanning)) {
            return;
        }
        String[] demandPlanningArray = demandPlanning.split(",");
        for (int i = 0; i < demandPlanningArray.length; i++) {
            String[] timePlan = demandPlanningArray[i].split(EngineConstants.DEMAND_PLANNING_DIVISION);
            Date glueDemandPlanTime = DateUtils.dateTime("yyyy-MM-dd HH:mm:ss", timePlan[0]);
            Double planQty = Double.parseDouble(timePlan[1]);

            Calendar calendar = Calendar.getInstance();
            calendar.setTime(glueDemandPlanTime);
            int hour = calendar.get(Calendar.HOUR_OF_DAY);  //小时数
            if(hour >= 16 && hour <= 23) {
                //中班
                schedule.setGlueMidExpectStartTime(glueDemandPlanTime);
                schedule.setMidPlanQty(planQty);
            } else if(hour >= 0 && hour <= 7) {
                //夜班
                schedule.setGlueNightExpectStartTime(glueDemandPlanTime);
                schedule.setNightPlanQty(planQty);
            } else if(hour >= 8 && hour <= 15) {
                //白班
                schedule.setGlueDayExpectStartTime(glueDemandPlanTime);
                schedule.setDayPlanQty(planQty);
            }
        }
    }

    /**
     * 计算称重预计开始时间（胶料开始生产前，至少要提前{prePrepareTime}分，进行硫磺辅料称重）
     * @param schedule
     * @param prePrepareTime 前置准备时间（单位：分）
     */
    private void actualWeightForGlue(MaterialScheduleResultVo schedule, int prePrepareTime) {
        long prePrepareTimeMillis = prePrepareTime * 60 * 1000;
        if(schedule.getGlueMidExpectStartTime() != null) {
            Date time = addTimeMillis(schedule.getGlueMidExpectStartTime(), 0 - prePrepareTimeMillis);
            if(schedule.getMidClassStartTime() != null && time.before(schedule.getMidClassStartTime())) {
                time = schedule.getMidClassStartTime();
            }
            schedule.setGlueMidExpectStartTime(time);
        }
        if(schedule.getGlueNightExpectStartTime() != null) {
            Date time = addTimeMillis(schedule.getGlueNightExpectStartTime(), 0 - prePrepareTimeMillis);
            if(schedule.getNightClassStartTime() != null && time.before(schedule.getNightClassStartTime())) {
                time = schedule.getNightClassStartTime();
            }
            schedule.setGlueNightExpectStartTime(time);
        }
        if(schedule.getGlueDayExpectStartTime() != null) {
            Date time = addTimeMillis(schedule.getGlueDayExpectStartTime(), 0 - prePrepareTimeMillis);
            if(schedule.getDayClassStartTime() != null && time.before(schedule.getDayClassStartTime())) {
                time = schedule.getDayClassStartTime();
            }
            schedule.setGlueDayExpectStartTime(time);
        }
    }

    /**
     * 根据机台产能，计算出【机台班制】，【单车称重消耗时间（毫秒）】,【各个班的实际开始时间、实际结束时间】
     * @param schedule
     * @param machineMap  机台信息map
     * @param defaultCapacity  机台默认产能（车/时）
     * @param dinnerTime  每班预留用餐时间（分钟）
     */
    private void countClassWeightTime(MaterialScheduleResultVo schedule, Map<String, LhflMachine> machineMap, int defaultCapacity, int dinnerTime) {
        Integer classShift = schedule.getClassShift();   //机台班制
        Integer capacity = schedule.getCapacity();    //机台产能(车/时)
        LhflMachine machine = machineMap.get(schedule.getMachineCode());
        if(capacity == null) {
            capacity = (machine != null ? machine.getCapacity().intValue() : defaultCapacity);  //机台产能(车/时)
        }
        if(classShift == null) {
            classShift = (machine != null ? machine.getClassShift() : EngineConstants.CLASS_SHIFT_THREE);  //机台班制
        }
        schedule.setClassShift(classShift);
        schedule.setCapacity(capacity);
        Date scheduleDate = schedule.getScheduleDate();  //排程日期
        schedule.setMachine(machine); // 对应的硫化机
        if(classShift == EngineConstants.CLASS_SHIFT_ONE) {
            //长白班，只有白班（第二天8:00 -- 16:00）
        	Date dayStartTime = DateUtils.addHours(scheduleDate, 8); // 白班开班时间
        	Date dayClassStartTime = Optional.ofNullable(machine).map(LhflMachine::getDayClassStartTime).orElse(dayStartTime);// 白班可开始时间
            schedule.setDayClassStartTime(dayClassStartTime); // 如果机台有要求
            schedule.setDayClassEndTime(DateUtils.addHours(dayStartTime, 8));
            schedule.setDayClassEndTime(DateUtils.addMinutes(schedule.getDayClassEndTime(), 0 - dinnerTime));  //每班预留用餐时间（分钟）
        }
        if(classShift == EngineConstants.CLASS_SHIFT_TWO) {
            //两班制，中班（12:00 -- 00:00）,夜班（第二天00:00 -- 12:00）
        	Date midStartTime = DateUtils.addHours(scheduleDate, -12); // 中班开班时间
        	Date midClassStartTime = Optional.ofNullable(machine).map(LhflMachine::getMidClassStartTime).orElse(midStartTime);// 中班可开始时间
        	Date nightStartTime = scheduleDate; // 夜班开班时间
        	Date nightClassStartTime = Optional.ofNullable(machine).map(LhflMachine::getNightClassStartTime).orElse(nightStartTime);// 夜班可开始时间
            schedule.setMidClassStartTime(midClassStartTime);
            schedule.setMidClassEndTime(nightStartTime);
            schedule.setMidClassEndTime(DateUtils.addMinutes(schedule.getMidClassEndTime(), 0 - dinnerTime));  //每班预留用餐时间（分钟）
            schedule.setNightClassStartTime(nightClassStartTime);
            schedule.setNightClassEndTime(DateUtils.addHours(nightStartTime, 12));
            schedule.setNightClassEndTime(DateUtils.addMinutes(schedule.getNightClassEndTime(), 0 - dinnerTime));   //每班预留用餐时间（分钟）
        }
        if(classShift == EngineConstants.CLASS_SHIFT_THREE) {
            //三班制，中班（16:00 -- 00:00）,夜班（第二天00:00 -- 08:00）,白班（第二天08:00 -- 16:00）
        	Date midStartTime = DateUtils.addHours(scheduleDate, -8); // 中班开班时间
        	Date midClassStartTime = Optional.ofNullable(machine).map(LhflMachine::getMidClassStartTime).orElse(midStartTime);// 中班可开始时间
        	Date nightStartTime = scheduleDate; // 夜班开班时间
        	Date nightClassStartTime = Optional.ofNullable(machine).map(LhflMachine::getNightClassStartTime).orElse(nightStartTime);// 夜班可开始时间
        	Date dayStartTime = DateUtils.addHours(scheduleDate, 8); // 白班开班时间
        	Date dayClassStartTime = Optional.ofNullable(machine).map(LhflMachine::getDayClassStartTime).orElse(dayStartTime);// 白班可开始时间
            schedule.setMidClassStartTime(midClassStartTime);
            schedule.setMidClassEndTime(nightStartTime);
            schedule.setMidClassEndTime(DateUtils.addMinutes(schedule.getMidClassEndTime(), 0 - dinnerTime));  //每班预留用餐时间（分钟）
            schedule.setNightClassStartTime(nightClassStartTime);
            schedule.setNightClassEndTime(dayStartTime);
            schedule.setNightClassEndTime(DateUtils.addMinutes(schedule.getNightClassEndTime(), 0 - dinnerTime));   //每班预留用餐时间（分钟）
            schedule.setDayClassStartTime(dayClassStartTime);
            schedule.setDayClassEndTime(DateUtils.addHours(dayStartTime, 8));
            schedule.setDayClassEndTime(DateUtils.addMinutes(schedule.getDayClassEndTime(), 0 - dinnerTime));   //每班预留用餐时间（分钟）
        }
        long singleCarTime = (1 * 60 * 60 * 1000) / capacity.longValue();  //单车称重消耗时间（毫秒）
        schedule.setSingleCarTime(singleCarTime);
    }

    /**
     * 常用规格map
     * @param mixArea 密炼区
     * @param scheduleDate 排程日期
     * @param areaMaterialList 密炼区+物料名称
     * @param commonlyUsedDay 近{commonlyUsedDay}日内都有排程的规格，就是常用规格
     * @return
     */
    private Map<String, String> mapCommonlyUsedMaterial(String mixArea, Date scheduleDate, List<MaterialAreaMachineVo> areaMaterialList, int commonlyUsedDay) {
        List<String> list = materialEngineMapper.listCommonlyUsedMaterial(mixArea, scheduleDate, areaMaterialList, commonlyUsedDay);
        return list.stream().collect(Collectors.toMap(r->r, r->COMMONLY_USED_YES.toString()));
    }

    /**
     * 同步分解胶料计划到日志表中
     * @param scheduleDate 排程日期
     * @param mixArea 密炼区
     */
    private void synclueScheduleToLog(Date scheduleDate, String mixArea) {
        materialEngineMapper.syncMaterialScheduleToLog(scheduleDate, mixArea);
        materialEngineMapper.deleteMaterialSchedule(scheduleDate, mixArea);
    }

    /**
     * 计算增加毫秒后的新时间
     * @param time  原来的时间
     * @param timeMillis  需要增加的时间戳
     * @return
     */
    private Date addTimeMillis(Date time, long timeMillis) {
        long resultTimeMillis = time.getTime() + timeMillis;
        return new Date(resultTimeMillis);
    }
}
