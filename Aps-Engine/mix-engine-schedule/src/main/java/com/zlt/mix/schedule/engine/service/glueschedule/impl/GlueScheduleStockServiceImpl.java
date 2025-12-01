package com.zlt.mix.schedule.engine.service.glueschedule.impl;

import com.ruoyi.common.core.utils.DateUtils;
import com.zlt.mix.common.engine.utils.CollectionUtil;
import com.zlt.mix.schedule.engine.constants.GlueEngineConstants;
import com.zlt.mix.schedule.engine.mapper.GlueScheduleStockMapper;
import com.zlt.mix.schedule.engine.service.glueschedule.GlueScheduleStockService;
import com.zlt.mix.schedule.engine.util.GlueScheduleStockPool;
import com.zlt.mix.schedule.engine.vo.DailyReturnGlueStockVo;
import com.zlt.mix.schedule.engine.vo.GlueFinishVo;
import com.zlt.mix.schedule.engine.vo.GlueStockVo;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 胶料排程库存服务
 * 
 * @author hakimryan
 *
 */
@Service
public class GlueScheduleStockServiceImpl implements GlueScheduleStockService {
	@Autowired
	private GlueScheduleStockMapper glueScheduleStockMapper;

	/**
	 * 忽略库存的到期时间
	 */
	private boolean ignoreVaildTime = true;

	/**
	 * 初始话库存池
	 * 
	 * @param scheduleDate 排产日
	 * @param mixArea      密炼区
	 * @return
	 */
	@Override
	public GlueScheduleStockPool buildStockPool(Date scheduleDate, String mixArea, Map<String, String> reserveGlueRecipeMap) {
		Date stockDate = DateUtils.addDays(scheduleDate, -1); // 抓取排程日前一天的库存
		// 查询排产日期所有胶料的库存
		List<GlueStockVo> glueStockList = glueScheduleStockMapper.listGlueStock(stockDate, mixArea); // 终炼胶库存
		// 终炼如果不区分掺胶和纯胶，将纯胶的库存的转为掺胶的库存
		convertGlueRecipe(reserveGlueRecipeMap, glueStockList);
		List<GlueStockVo> mlGlueStockList = glueScheduleStockMapper.listMlGlueStock(stockDate, mixArea); // 母炼胶
		List<GlueStockVo> fhGlueStockList = glueScheduleStockMapper.listFhGlueStock(stockDate, mixArea); // 返回胶
		List<GlueStockVo> bhgGlueStockList = glueScheduleStockMapper.listBhgGlueStock(stockDate, mixArea); // 不合格胶
		if (ignoreVaildTime) { // 如果需要忽略库存到期时间对逻辑的影响，直接将当天库存数据的到期时间全部覆盖为库存日后7天
			Date validTime = DateUtils.addDays(stockDate, 7);
			glueStockList.stream().forEach(s -> {
				s.setValidTime(validTime);
			});
			mlGlueStockList.stream().forEach(s -> {
				s.setValidTime(validTime);
			});
			fhGlueStockList.stream().forEach(s -> {
				s.setValidTime(validTime);
			});
			bhgGlueStockList.stream().forEach(s -> {
				s.setValidTime(validTime);
			});
		}

		Map<String, BigDecimal> finishQtyMap = glueScheduleStockMapper.list12AmFinishQty(stockDate, mixArea).stream()
				.collect(Collectors.toMap(GlueFinishVo::getOrderNo, GlueFinishVo::getFinishQty)); // 12点各工单的已生产量
		Map<String, BigDecimal> safeStockMap = glueScheduleStockMapper.listSafeStock(mixArea).stream()
				.collect(Collectors.toMap(GlueStockVo::getGlue, GlueStockVo::getStockNum, (s1, s2) -> s1)); // 安全库存设置
		Map<String, DailyReturnGlueStockVo> returnGlueStockMap = glueScheduleStockMapper
				.listDailyReturnGlueStock(scheduleDate, mixArea).stream()
				.collect(Collectors.toMap(DailyReturnGlueStockVo::getGlue, Function.identity(), (s1, s2) -> s1)); // 预计返回胶统计

		// 各库存数据放置到库存池中
		return new GlueScheduleStockPool(stockDate, mixArea, glueStockList, mlGlueStockList, fhGlueStockList,
				bhgGlueStockList, safeStockMap, finishQtyMap, returnGlueStockMap, ignoreVaildTime);
	}

	/**
	 * 终炼如果不区分掺胶和纯胶，将纯胶的库存的转为掺胶的库存
	 *
	 * @param reserveGlueRecipeMap 查询胶料配方映射反转的Map
	 * @param glueStockList        终炼胶库存信息
	 */
	private void convertGlueRecipe(Map<String, String> reserveGlueRecipeMap, List<GlueStockVo> glueStockList) {
		if (CollectionUtils.isEmpty(glueStockList) || reserveGlueRecipeMap.isEmpty()) {
			return;
		}

		// 终炼如果不区分掺胶和纯胶日用量，将纯胶的库存的转为掺胶的库存
		for (GlueStockVo glueStockVo : glueStockList) {
			if (reserveGlueRecipeMap.containsKey(glueStockVo.getGlue())) {
				glueStockVo.setGlue(reserveGlueRecipeMap.get(glueStockVo.getGlue()));
			}
		}
	}

	/**
	 * 只加载指定类型物料的库存池
	 * 
	 * @param scheduleDate 排产日
	 * @param mixArea      密炼区
	 * @param majorType    物料类型
	 * @return
	 */
	@Override
	public GlueScheduleStockPool buildStockPool(Date scheduleDate, String mixArea, Map<String, String> reserveGlueRecipeMap, String... majorType) {
		Date stockDate = DateUtils.addDays(scheduleDate, -1); // 抓取排程日前一天的库存
		if (majorType == null || majorType.length == 0) {
			return null;
		}
		List<String> majorTypeList = Arrays.asList(majorType);
		// 查询排产日期所有胶料的库存
		List<GlueStockVo> glueStockList; // 终炼胶库存
		List<GlueStockVo> mlGlueStockList; // 母炼胶
		List<GlueStockVo> fhGlueStockList; // 返回胶
		List<GlueStockVo> bhgGlueStockList; // 不合格胶
		if (majorTypeList.contains(GlueEngineConstants.MAJOR_TYPE_ZL)) {
			glueStockList = glueScheduleStockMapper.listGlueStock(stockDate, mixArea); // 终炼胶库存
			// 终炼如果不区分掺胶和纯胶，将纯胶的库存的转为掺胶的库存
			convertGlueRecipe(reserveGlueRecipeMap, glueStockList);
		} else {
			glueStockList = CollectionUtil.emptyList();
		}
		if (majorTypeList.contains(GlueEngineConstants.MAJOR_TYPE_ML)) {
			mlGlueStockList = glueScheduleStockMapper.listMlGlueStock(stockDate, mixArea); // 母炼胶
		} else {
			mlGlueStockList = CollectionUtil.emptyList();
		}
		if (majorTypeList.contains(GlueEngineConstants.MAJOR_TYPE_FH)) {
			fhGlueStockList = glueScheduleStockMapper.listFhGlueStock(stockDate, mixArea); // 返回胶
		} else {
			fhGlueStockList = CollectionUtil.emptyList();
		}
		if (majorTypeList.contains(GlueEngineConstants.MAJOR_TYPE_BHG)) {
			bhgGlueStockList = glueScheduleStockMapper.listBhgGlueStock(stockDate, mixArea); // 不合格胶
		} else {
			bhgGlueStockList = CollectionUtil.emptyList();
		}
		if (ignoreVaildTime) { // 如果需要忽略库存到期时间对逻辑的影响，直接将当天库存数据的到期时间全部覆盖为库存日后7天
			Date validTime = DateUtils.addDays(stockDate, 7);
			glueStockList.stream().forEach(s -> {
				s.setValidTime(validTime);
			});
			mlGlueStockList.stream().forEach(s -> {
				s.setValidTime(validTime);
			});
			fhGlueStockList.stream().forEach(s -> {
				s.setValidTime(validTime);
			});
			bhgGlueStockList.stream().forEach(s -> {
				s.setValidTime(validTime);
			});
		}
		// 各库存数据放置到库存池中
		return new GlueScheduleStockPool(stockDate, mixArea, glueStockList, mlGlueStockList, fhGlueStockList,
				bhgGlueStockList, new HashMap<>(), new HashMap<>(), new HashMap<>(), ignoreVaildTime);
	}
}
