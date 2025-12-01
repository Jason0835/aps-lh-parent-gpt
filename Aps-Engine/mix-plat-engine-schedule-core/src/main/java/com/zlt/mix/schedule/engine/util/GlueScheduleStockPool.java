package com.zlt.mix.schedule.engine.util;

import com.ruoyi.common.core.utils.DateUtils;
import com.zlt.mix.common.core.utils.BigDecimalUtil;
import com.zlt.mix.common.core.utils.CollectionUtil;
import com.zlt.mix.schedule.engine.constants.GlueEngineConstants;
import com.zlt.mix.schedule.engine.vo.DailyReturnGlueStockVo;
import com.zlt.mix.schedule.engine.vo.GlueStockVo;
import com.zlt.mix.schedule.engine.vo.MesPmtRecipeVo;
import com.zlt.mix.schedule.engine.vo.MesPmtRecipeWeightVo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 库存池，包括终炼胶、母炼胶、返回胶、不合格胶
 * 
 * @author hakimryan
 *
 */
public class GlueScheduleStockPool {
	/**
	 * 对象虚拟ID，用于数据新增前的唯一识别码，因此是小于0
	 */
	private Long id = -1L;
	/**
	 * 密炼区
	 */
	private String mixArea;
	/**
	 * 库存日期
	 */
	private Date stockDate;
	/**
	 * 当前时间点
	 */
	private Date currentDate;
	/**
	 * 终炼胶库存
	 */
	Map<String, List<GlueStockVo>> glueStockMap;
	/**
	 * 母炼胶库存
	 */
	Map<String, List<GlueStockVo>> mlGlueStockMap;
	/**
	 * 返回胶库存
	 */
	Map<String, List<GlueStockVo>> fhGlueStockMap;
	/**
	 * 不合格胶库存
	 */
	Map<String, List<GlueStockVo>> bhgGlueStockMap;
	/**
	 * 日返回胶数量
	 */
	Map<String, DailyReturnGlueStockVo> returnGlueStockMap;
	/**
	 * 安全库存
	 */
	private Map<String, BigDecimal> safeStockMap;
	/**
	 * 已完成量
	 */
	private Map<String, BigDecimal> finishQtyMap;
	/**
	 * 忽略库存的到期时间
	 */
	private boolean ignoreVaildTime;

	private GlueScheduleStockPool(Date stockDate, String mixArea) {
		this.mixArea = mixArea;
		this.stockDate = stockDate;
		this.glueStockMap = new HashMap<>();
		this.mlGlueStockMap = new HashMap<>();
		this.fhGlueStockMap = new HashMap<>();
		this.bhgGlueStockMap = new HashMap<>();
		this.safeStockMap = new HashMap<>();
		this.finishQtyMap = new HashMap<>();
		this.returnGlueStockMap = new HashMap<>();
	}

	public GlueScheduleStockPool(Date scheduleDate, String mixArea, List<GlueStockVo> glueStockList,
			List<GlueStockVo> mlGlueStockList, List<GlueStockVo> fhGlueStockList, List<GlueStockVo> bhgGlueStockList,
			Map<String, BigDecimal> safeStockMap, Map<String, BigDecimal> finishQtyMap,
			Map<String, DailyReturnGlueStockVo> returnGlueStockMap, boolean ignoreVaildTime) {
		this(scheduleDate, mixArea);

		// 将库存按胶料号分组
		this.glueStockMap = glueStockList.stream().collect(Collectors.groupingBy(GlueStockVo::getGlue));
		this.mlGlueStockMap = mlGlueStockList.stream().collect(Collectors.groupingBy(GlueStockVo::getGlue));
		this.fhGlueStockMap = fhGlueStockList.stream().collect(Collectors.groupingBy(GlueStockVo::getGlue));
		this.bhgGlueStockMap = bhgGlueStockList.stream().collect(Collectors.groupingBy(GlueStockVo::getGlue));
		this.returnGlueStockMap = returnGlueStockMap;// TODO 需要通过计算得出日返回胶量
		this.currentDate = DateUtils.getNowDate(); // 初始化时默认服务器时间为当前时间点
		this.safeStockMap = safeStockMap;
		this.finishQtyMap = finishQtyMap;
		this.ignoreVaildTime = ignoreVaildTime;
	}

	/**
	 * 移除已经过期库存胶料库存
	 * 
	 * @param glueStockMap 胶料库存数据
	 * @return
	 */
	private void removeValidGlueStock(Map<String, List<GlueStockVo>> glueStockMap) {
		for (Entry<String, List<GlueStockVo>> entry : glueStockMap.entrySet()) {
			// 胶料号
			String glueCode = entry.getKey();
			// 胶料库存
			List<GlueStockVo> stockList = entry.getValue();
			List<GlueStockVo> newStockList = null;
			for (GlueStockVo stock : stockList) {
				// 库存到期时间
				Date validTime = stock.getValidTime();
				if (validTime == null || validTime.compareTo(this.currentDate) <= 0) {
					if (CollectionUtil.isEmpty(newStockList)) {
						newStockList = new ArrayList<>(stockList);
					}
					// 到期时间为空或者早于当前系统时间，则这批库存要忽略掉
					newStockList.remove(stock);
				}
			}
			if (newStockList != null) {
				// 如果有需要调整，则重新放到map中
				glueStockMap.put(glueCode, newStockList);
			}
		}
	}

	/**
	 * 复制一份库存
	 * 
	 * @return
	 */
	public GlueScheduleStockPool copyStockPool() {
		GlueScheduleStockPool stockPool = new GlueScheduleStockPool(this.stockDate, this.mixArea);
		stockPool.copyStockMap(this.glueStockMap, stockPool.glueStockMap);
		stockPool.copyStockMap(this.mlGlueStockMap, stockPool.mlGlueStockMap);
		stockPool.copyStockMap(this.fhGlueStockMap, stockPool.fhGlueStockMap);
		stockPool.copyStockMap(this.bhgGlueStockMap, stockPool.bhgGlueStockMap);
		for (Entry<String, DailyReturnGlueStockVo> entry : this.returnGlueStockMap.entrySet()) {
			String glue = entry.getKey();
			DailyReturnGlueStockVo stock = entry.getValue();
			DailyReturnGlueStockVo newStock = new DailyReturnGlueStockVo(stock.getGlue(), stock.getStockWeight(),
					stock.getNightStock(), stock.getDayStock());
			stockPool.returnGlueStockMap.put(glue, newStock);
		}
		stockPool.finishQtyMap.putAll(this.finishQtyMap);
		stockPool.safeStockMap.putAll(this.safeStockMap);
		stockPool.currentDate = this.currentDate;
		stockPool.ignoreVaildTime = this.ignoreVaildTime;
		stockPool.id = this.id;
		return stockPool;
	}

	/**
	 * 将源map中的库存数据全部复制到目标Map中
	 * 
	 * @param sourceStockMap 源库存map
	 * @param targetStockMap 目标库存map
	 */
	private void copyStockMap(Map<String, List<GlueStockVo>> sourceStockMap,
			Map<String, List<GlueStockVo>> targetStockMap) {
		sourceStockMap.entrySet().forEach(entry -> {
			List<GlueStockVo> stockList = entry.getValue();
			List<GlueStockVo> newStockList = new ArrayList<>();
			for (GlueStockVo stock : stockList) {
				GlueStockVo newStock = new GlueStockVo(stock.getGlue(), stock.getStockNum(), stock.getStockWeight(),
						stock.getValidTime());
				newStockList.add(newStock);
			}
			targetStockMap.put(entry.getKey(), newStockList);
		});

	}

	/**
	 * 获取指定物料库存的最接近当前时间点的到期时间
	 * 
	 * @param glueCode  胶料号
	 * @param majorType 物料类型
	 * @return
	 */
	public Date getValidTime(String glueCode, String majorType) {
		List<GlueStockVo> stockList = this.getStock(glueCode, majorType);
		if (!CollectionUtil.isEmpty(stockList)) {
			return CollectionUtil.firstElement(stockList).getValidTime();
		}
		return null;
	}

	/**
	 * 获取指定物料类型库存，只取当前时间点还有效的库存
	 * 
	 * @param glueCode    胶料号
	 * @param majorType   物料类型
	 * @param currentDate 当前时间点
	 * @return
	 */
	private List<GlueStockVo> getStock(String glueCode, String majorType) {
		List<GlueStockVo> stockList = this.getStockMap(glueCode, majorType).get(glueCode);
		if (stockList == null) {
			return new ArrayList<>(0);
		}
		return stockList;
	}

	/**
	 * 获取指定物料类型库存车数
	 * 
	 * @param glueCode  胶料号
	 * @param majorType 物料类型
	 * @return
	 */
	public BigDecimal getStockNum(String glueCode, String majorType) {
		return this.getStock(glueCode, majorType).stream().map(GlueStockVo::getStockNum)
				.collect(Collectors.reducing(BigDecimal.ZERO, Function.identity(), BigDecimal::add));
	}

	/**
	 * 获取指定物料类型库存重量
	 * 
	 * @param glueCode  胶料号
	 * @param majorType 物料类型
	 * @return
	 */
	public BigDecimal getStockWeight(String glueCode, String majorType) {
		return this.getStock(glueCode, majorType).stream().map(GlueStockVo::getStockWeight)
				.collect(Collectors.reducing(BigDecimal.ZERO, Function.identity(), BigDecimal::add));
	}

	/**
	 * 增加库存量
	 * 
	 * @param glueCode  胶料编号
	 * @param majorType 胶料类型
	 * @param stockNum  库存增量
	 */
	public void addStock(String glueCode, String majorType, BigDecimal stockNum) {
		this.addNewStock(glueCode, majorType, stockNum, BigDecimal.ZERO);
	}

	/**
	 * 增加库存重量
	 * 
	 * @param glueCode    胶料编号
	 * @param majorType   胶料类型
	 * @param stockWeight 库存重量增量
	 */
	public void addStockWeight(String glueCode, String majorType, BigDecimal stockWeight) {
		this.addNewStock(glueCode, majorType, BigDecimal.ZERO, stockWeight);
	}

	/**
	 * 增加库存
	 * 
	 * @param glueCode    胶料编号
	 * @param majorType   胶料类型
	 * @param stockNum    库存增量
	 * @param stockWeight 库存重量增量
	 */
	private void addNewStock(String glueCode, String majorType, BigDecimal stockNum, BigDecimal stockWeight) {
		Map<String, List<GlueStockVo>> stockMap = this.getStockMap(glueCode, majorType);
		List<GlueStockVo> stockList = stockMap.get(glueCode);
		if (stockList == null) {
			stockList = new ArrayList<>();
			stockMap.put(glueCode, stockList);
		}
		// 直接新增一个库存数据，日期默认为排产日的1周后
		Date validTime = DateUtils.addDays(stockDate, 7);
		stockList.add(new GlueStockVo(glueCode, stockNum, stockWeight, validTime));
	}

	/**
	 * 扣减库存量
	 * 
	 * @param glueCode  胶料编号
	 * @param majorType 胶料类型
	 * @param stockNum  库存扣减量
	 */
	public void subtractStock(String glueCode, String majorType, BigDecimal stockNum) {
		Map<String, List<GlueStockVo>> stockMap = this.getStockMap(glueCode, majorType);
		List<GlueStockVo> stockList = stockMap.get(glueCode);
		if (stockList == null) {
			// 如果本身就没有库存，则直接返回
			return;
		}
		// 应扣减量
		BigDecimal subtractNum = stockNum;
		for (GlueStockVo stock : stockList) {
			// 按顺序扣减
			if (stock != null) {
				BigDecimal oldStockNum = Optional.ofNullable(stock.getStockNum()).orElse(BigDecimal.ZERO);
				// 没有可扣减库存，直接看下一个库存
				if (oldStockNum.compareTo(BigDecimal.ZERO) == 0) {
					continue;
				}
				// 库存 = 原库存 - 扣减量
				BigDecimal newStockNum = oldStockNum.subtract(subtractNum);
				// 库存不会为负数
				stock.setStockNum(BigDecimalUtil.greatest(newStockNum, BigDecimal.ZERO));
				// 应扣减量移除本次的库存量
				subtractNum = subtractNum.subtract(oldStockNum);
				if (subtractNum.compareTo(BigDecimal.ZERO) <= 0) {
					// 全部待扣减量归零后，结束循环
					break;
				}
			}
		}
	}

	/**
	 * 扣减库存重量
	 * 
	 * @param glueCode    胶料编号
	 * @param majorType   胶料类型
	 * @param stockWeight 库存重量增量
	 */
	public void subtractStockWeight(String glueCode, String majorType, BigDecimal stockWeight) {
		Map<String, List<GlueStockVo>> stockMap = this.getStockMap(glueCode, majorType);
		List<GlueStockVo> stockList = stockMap.get(glueCode);
		if (stockList == null) {
			// 如果本身就没有库存，则直接返回
			return;
		}
		// 应扣减量
		BigDecimal subtractWeight = stockWeight;
		for (GlueStockVo stock : stockList) {
			// 按顺序扣减
			if (stock != null) {
				BigDecimal oldStockWeight = Optional.ofNullable(stock.getStockWeight()).orElse(BigDecimal.ZERO);
				// 没有可扣减库存，直接看下一个库存
				if (oldStockWeight.compareTo(BigDecimal.ZERO) == 0) {
					continue;
				}
				// 库存 = 原库存 - 扣减量
				BigDecimal newStockWeight = oldStockWeight.subtract(subtractWeight);
				// 库存不会为负数
				stock.setStockWeight(BigDecimalUtil.greatest(newStockWeight, BigDecimal.ZERO));
				// 应扣减量移除本次的库存量
				subtractWeight = subtractWeight.subtract(oldStockWeight);
				if (subtractWeight.compareTo(BigDecimal.ZERO) <= 0) {
					// 全部待扣减量归零后，结束循环
					break;
				}
			}
		}
	}

	/**
	 * 获取指定物料类型的库存信息
	 * 
	 * @param glueCode  胶料号，用来判断真正的物料类型
	 * @param majorType 物料类型
	 * @return
	 */
	private Map<String, List<GlueStockVo>> getStockMap(String glueCode, String majorType) {
		String realMajorType = RecipeUtil.getMajorType(glueCode, majorType); // 获取真正的物料类型
		if (majorType == null) {
			return new HashMap<>();
		}
		Map<String, List<GlueStockVo>> stockMap;
		switch (realMajorType) {
		case GlueEngineConstants.MAJOR_TYPE_ML:
			stockMap = this.mlGlueStockMap;
			break;
		// 塑炼胶暂时也存在母炼胶的库存中
		case GlueEngineConstants.MAJOR_TYPE_SL: 
			stockMap = this.mlGlueStockMap;
			break;
		case GlueEngineConstants.MAJOR_TYPE_ZL:
			stockMap = this.glueStockMap;
			break;
		case GlueEngineConstants.MAJOR_TYPE_FH:
			stockMap = this.fhGlueStockMap;
			break;
		case GlueEngineConstants.MAJOR_TYPE_BHG:
			stockMap = this.bhgGlueStockMap;
			break;
		case GlueEngineConstants.MAJOR_TYPE_WASH:
		case GlueEngineConstants.MAJOR_TYPE_MIX:
			// 洗胶、掺胶，都需要根据原物料类型判断从哪个库存获取
			if (GlueEngineConstants.MAJOR_TYPE_ML.equals(majorType)) {
				stockMap = this.mlGlueStockMap;
			} else {
				stockMap = this.glueStockMap;
			}
			break;
		default:
			stockMap = new HashMap<>();
		}
		return stockMap;
	}

	/**
	 * 根据上级胶料扣减对应的库存
	 * 
	 * @param upGlueRequireNum 上级胶料的需求量
	 * @param recipe           上级胶料的配方
	 */
	public void subtractChildGlueStock(BigDecimal upGlueRequireNum, MesPmtRecipeVo recipe) {
		this.subtractChildGlueStock(upGlueRequireNum, recipe, false); // 掺胶也需要处理
	}

	/**
	 * 根据上级胶料扣减对应的库存
	 * 
	 * @param upGlueRequireNum 上级胶料的需求量
	 * @param recipe           上级胶料的配方
	 * @param onlyMlGlue       是否只处理母胶
	 */
	public void subtractChildGlueStock(BigDecimal upGlueRequireNum, MesPmtRecipeVo recipe, boolean onlyMlGlue) {
		BigDecimal maxSetWeight = RecipeUtil.getMaxSetWeight(recipe.getRecipeWeightList()); // 获取称重配方中最大的终炼母炼胶重量
		for (MesPmtRecipeWeightVo recipeWeight : recipe.getRecipeWeightList()) {
			String majorType = recipeWeight.getMajorType(); // 物料类型
			String glueCode = recipeWeight.getRecipeMaterialName(); // 胶料
			BigDecimal setWeight = BigDecimalUtil.valueOfZero(recipeWeight.getSetWeight());
			String realMajorType = RecipeUtil.getMajorType(glueCode, majorType, setWeight, maxSetWeight);
			if (GlueEngineConstants.MIX_MAJOR_TYPE.contains(realMajorType) && !onlyMlGlue) { // 如果只处理母胶，则跳过掺较的处理
				// 掺胶，需要按重量扣减
				BigDecimal glueConsumeQty = upGlueRequireNum.multiply(setWeight); // 取整后的总重
				this.subtractStockWeight(glueCode, majorType, glueConsumeQty); // 扣减掉对应掺胶的库存重量
			} else if (GlueEngineConstants.MAJOR_TYPE_ML.equals(realMajorType)) {
				// 母炼胶类型，需要按车数扣减
				BigDecimal conversionRatio = recipeWeight.getConversionRatio(); // 换算比率
				if (conversionRatio == null || BigDecimal.ZERO.compareTo(conversionRatio) == 0) {
					conversionRatio = BigDecimal.ONE;// 如果转换率为0或者空，则直接按1处理
				}
				BigDecimal glueConsumeQty = upGlueRequireNum.divide(conversionRatio, 0, RoundingMode.UP); // 取整后的总车数
				this.subtractStock(glueCode, majorType, glueConsumeQty); // 扣减掉对应母炼胶的库存车数
			} else if (GlueEngineConstants.MAJOR_TYPE_SL.contains(realMajorType)) {
				// 塑炼类型，需要按重量扣减称重
				BigDecimal glueConsumeWeight = upGlueRequireNum.multiply(setWeight); // 取整后的总重
				this.subtractStockWeight(glueCode, majorType, glueConsumeWeight); // 扣减掉对应塑炼胶的库存重量
				// 还需要扣减车数，便于查看对应预计库存
				if (recipe.getLotTotalWeight() != null) {
					double conversionRatio = BigDecimalUtil.div(setWeight.doubleValue(), recipe.getLotTotalWeight(), 4);
					double glueConsumeQty = BigDecimalUtil.mul(conversionRatio, upGlueRequireNum.doubleValue());
					this.subtractStock(glueCode, majorType, BigDecimal.valueOf(glueConsumeQty).setScale(0, RoundingMode.CEILING)); // 扣减掉对应塑炼炼胶的库存车数
				}
			}
		}
	}

	/**
	 * 获取合格胶（终炼/母炼胶）库存量
	 * 
	 * @param glueCode 胶料编号
	 * @return
	 */
	public BigDecimal getQualifiedGlueStockNum(String glueCode) {
		BigDecimal stockNum = this.getStockNum(glueCode, GlueEngineConstants.MAJOR_TYPE_ZL);
		if (stockNum.compareTo(BigDecimal.ZERO) <= 0) {
			return this.getStockNum(glueCode, GlueEngineConstants.MAJOR_TYPE_ML);
		}
		return stockNum;
	}

	/**
	 * 获取合格胶（终炼/母炼胶）库存量
	 * 
	 * @return
	 */
	public List<GlueStockVo> getQualifiedGlueStock() {
		List<GlueStockVo> qualifiedGlueList = new ArrayList<>();
		// 终炼胶库存
		for (Entry<String, List<GlueStockVo>> entry : this.glueStockMap.entrySet()) {
			qualifiedGlueList.addAll(entry.getValue());
		}
		// 母炼胶库存
		for (Entry<String, List<GlueStockVo>> entry : this.mlGlueStockMap.entrySet()) {
			qualifiedGlueList.addAll(entry.getValue());
		}
		return qualifiedGlueList;
	}

	/**
	 * 获取安全库存
	 * 
	 * @return
	 */
	public BigDecimal getSafeStock(String glueCode) {
		return safeStockMap.getOrDefault(glueCode, BigDecimal.ZERO);
	}

	/**
	 * 获取所有日返回胶库存
	 * 
	 * @param glue
	 * @return
	 */
	public List<DailyReturnGlueStockVo> listReturnStockWeight() {
		return new ArrayList<>(returnGlueStockMap.values());
	}

	/**
	 * 获取日返回胶库存
	 * 
	 * @param glue
	 * @return
	 */
	public BigDecimal getReturnStockWeight(String glue) {
		DailyReturnGlueStockVo stock = returnGlueStockMap.get(glue);
		return Optional.ofNullable(stock).map(DailyReturnGlueStockVo::getStockWeight).orElse(BigDecimal.ZERO);
	}

	/**
	 * 获取指定班次日返回胶重量
	 * 
	 * @param glue       胶料
	 * @param shiftClass 班次
	 * @return
	 */
	public BigDecimal getReturnWeight(String glue, Integer shiftClass, boolean isSingleClass) {
		DailyReturnGlueStockVo stock = returnGlueStockMap.get(glue);
		if (stock == null) {
			return BigDecimal.ZERO;
		}
		switch (shiftClass) {
		case GlueEngineConstants.SHIFT_CLASS_NIGHT:
			return stock.getNightStock();
		case GlueEngineConstants.SHIFT_CLASS_DAY:
			if (isSingleClass) {
				return stock.getDayStock();
			} else {
				return stock.getDayStock().add(stock.getNightStock());
			}
		default:
			return BigDecimal.ZERO;
		}
	}

	/**
	 * 扣减对应班次的日返回胶量
	 * 
	 * @param glue           胶料
	 * @param shiftClass     班次
	 * @param subtractWeight 扣减量
	 */
	public void subtractReturnGlueWeight(String glue, Integer shiftClass, BigDecimal subtractWeight) {
		DailyReturnGlueStockVo stock = returnGlueStockMap.get(glue);
		if (stock == null) {
			return;
		}
		BigDecimal nightStock = stock.getNightStock();
		BigDecimal surplusWeight = BigDecimalUtil.greatest(subtractWeight.subtract(nightStock), BigDecimal.ZERO);
		if (nightStock.compareTo(subtractWeight) >= 0) {
			stock.setNightStock(nightStock.subtract(subtractWeight));
		} else {
			stock.setNightStock(BigDecimal.ZERO);
		}
		if (shiftClass == GlueEngineConstants.SHIFT_CLASS_NIGHT) {
			return;
		}
		BigDecimal dayStock = stock.getDayStock();
		if (dayStock.compareTo(surplusWeight) >= 0) {
			stock.setDayStock(dayStock.subtract(surplusWeight));
		} else {
			stock.setDayStock(BigDecimal.ZERO);
		}
	}

	/**
	 * 获取全部安全库存
	 * 
	 * @return
	 */
	public Map<String, BigDecimal> getSafeStock() {
		return safeStockMap;
	}

	/**
	 * 获取已生产量
	 * 
	 * @return
	 */
	public BigDecimal getFinishQty(String glueCode) {
		return finishQtyMap.getOrDefault(glueCode, BigDecimal.ZERO);
	}

	/**
	 * 更新当前时间点，需要移除已过期的数据
	 * 
	 * @param currentDate
	 */
	public void updateCurrentDate(Date currentDate) {
		this.currentDate = currentDate;
		if (!ignoreVaildTime) {
			this.removeValidGlueStock(this.glueStockMap);
			this.removeValidGlueStock(this.mlGlueStockMap);
			this.removeValidGlueStock(this.fhGlueStockMap);
			this.removeValidGlueStock(this.bhgGlueStockMap);
		}
	}

	/**
	 * 生成下一个ID
	 * 
	 * @return
	 */
	public Long nextId() {
		return --this.id;
	}

	public String getMixArea() {
		return mixArea;
	}
}
