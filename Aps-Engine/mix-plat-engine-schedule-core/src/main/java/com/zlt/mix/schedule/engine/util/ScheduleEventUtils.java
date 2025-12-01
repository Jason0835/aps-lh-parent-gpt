package com.zlt.mix.schedule.engine.util;

import com.ruoyi.common.core.utils.DateUtils;
import com.zlt.mix.common.core.utils.BigDecimalUtil;
import com.zlt.mix.common.core.utils.GenerageMapKeyUtils;
import com.zlt.mix.schedule.engine.constants.GlueEngineConstants;
import com.zlt.mix.schedule.engine.util.event.ContinueProductEvent;
import com.zlt.mix.schedule.engine.util.event.FinishFirstBatchEvent;
import com.zlt.mix.schedule.engine.util.event.FinishProductEvent;
import com.zlt.mix.schedule.engine.vo.*;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 排产事件工具
 */
public class ScheduleEventUtils {

    /**
     * 分厂需求标记标记
     */
    private final static String FACTORY_REQUIRE_MARK = "△";
    /**
     * 首批完成冷却车数默认值：16车
     */
    private final static String DEFAULT_FIRST_BATCH_GLUE_NUM = "16";

    private final static BigDecimal ONE_THOUSAND = new BigDecimal("1000");

    /**
     * 根据班制设置排产的字段：班制计划量、班制开始时间、班制结束时间，重算总计划
     *
     * @param scheduleResult 排产
     * @param shiftClass     班制
     * @param productedQty   生产量
     * @param currentTime    当前时间
     * @param finishTime     完成时间
     * @param isRequire      是否分厂需求
     */
    public static void setShiftPlanField(GlueScheduleResultVo scheduleResult, int shiftClass, BigDecimal productedQty, Date currentTime, Date finishTime, boolean isRequire) {
        // 根据时间设置对应班次的数据
        if (shiftClass == GlueEngineConstants.SHIFT_CLASS_MID) {
            scheduleResult.setMidPlanQty(productedQty.doubleValue());
            scheduleResult.setMidExpectStartTime(currentTime);
            scheduleResult.setMidExpectFinishTime(finishTime);
            if (isRequire) { // 属于分厂需求优先生产的计划，在对应班次备注增加符号标注
                scheduleResult.setMidRemark(FACTORY_REQUIRE_MARK);
            }
        } else if (shiftClass == GlueEngineConstants.SHIFT_CLASS_NIGHT) {
            scheduleResult.setNightPlanQty(productedQty.doubleValue());
            scheduleResult.setNightExpectStartTime(currentTime);
            scheduleResult.setNightExpectFinishTime(finishTime);
            if (isRequire) { // 属于分厂需求优先生产的计划，在对应班次备注增加符号标注
                scheduleResult.setNightRemark(FACTORY_REQUIRE_MARK);
            }
        } else {
            scheduleResult.setDayPlanQty(productedQty.doubleValue());
            scheduleResult.setDayExpectStartTime(currentTime);
            scheduleResult.setDayExpectFinishTime(finishTime);
            if (isRequire) { // 属于分厂需求优先生产的计划，在对应班次备注增加符号标注
                scheduleResult.setDayRemark(FACTORY_REQUIRE_MARK);
            }
        }

        // 总计划量重算 = 中班计划量 + 夜班计划量 + 白班计划量
        Double totalPlanQty = BigDecimalUtil.add(scheduleResult.getMidPlanQty(), scheduleResult.getNightPlanQty(),
                scheduleResult.getDayPlanQty());
        scheduleResult.setTotalPlanQty(totalPlanQty);
    }

    /**
     * 添加生产完成事件和首批停放事件
     *
     * @param queue          排产队列
     * @param scheduleResult 排程
     * @param productedQty   生产量
     * @param params         参数
     * @param finishTime     完成时间
     * @param mixTime        炼胶时间
     * @param intervalTime   间隔时间
     * @param currentTime    当前时间
     */
    public static void addFinishEvent(ScheduleEventQueue queue,
                                      GlueScheduleResultVo scheduleResult,
                                      BigDecimal productedQty,
                                      Map<String, String> params,
                                      Date finishTime,
                                      Long mixTime,
                                      Long intervalTime,
                                      Date currentTime,
                                      boolean updateProductTimeTag) {
        // 往队列添加生产完成事件
        Integer switchTime = new Integer(params.getOrDefault(GlueEngineConstants.SCHEDULE_SWITCH_TIME, "0")); // 排程切换时间
        Date finishEventTime = DateUtils.addSeconds(finishTime, switchTime.intValue());
        queue.addEvent(new FinishProductEvent(scheduleResult, updateProductTimeTag), finishEventTime);

        // 添加首批生产完成事件，首批完成生产后就开始计算停放时间
        // 计算首批完成时间
        Long firstBatchNum = new Long(
                params.getOrDefault(GlueEngineConstants.FIRST_BATCH_GLUE_NUM, DEFAULT_FIRST_BATCH_GLUE_NUM)); // 首批车数
        Date finishFistBatchEventTime;
        if (BigDecimalUtil.valueOf(firstBatchNum).compareTo(productedQty) < 0) {
            Long firstBatchProductTime = (mixTime + intervalTime) * firstBatchNum.longValue(); // 总生产时间 = （炼胶时间 +
            // 间隔时间）
            finishFistBatchEventTime = DateUtils.addSeconds(currentTime, (int) firstBatchProductTime.longValue()); // 完成生产时间
        } else {
            finishFistBatchEventTime = finishEventTime;
        }
        queue.addEvent(new FinishFirstBatchEvent(scheduleResult, productedQty), finishFistBatchEventTime);
    }

    /**
     * 根据称重计算可以生产的车数
     *
     * @param glueStock         库存
     * @param recipe            配方
     * @param newProductQty     可生产量
     * @param minStock          最小生产量
     * @param toProductQty      可生产量
     * @param requireDifference 分厂需求量
     * @return 根据库存限制的可生产量
     */
    public static BigDecimal getProductQtyByWeight(GlueScheduleStockPool glueStock,
                                                   MesPmtRecipeVo recipe,
                                                   BigDecimal newProductQty,
                                                   BigDecimal minStock,
                                                   BigDecimal toProductQty,
                                                   BigDecimal requireDifference) {
        // 判断配方是否叶子节点 或者 需要塑胶
        boolean isLeaf = recipe.getRecipeWeightList().stream()
                .noneMatch(weight -> checkStockMajorType(weight.getMajorType()));

        if (isLeaf) {
            // 叶子节点不需要判断其原料库存
            return toProductQty;
        }

        BigDecimal maxSetWeight = RecipeUtil.getMaxSetWeight(recipe.getRecipeWeightList()); // 获取称重配方中最大的终炼母炼胶重量
        for (MesPmtRecipeWeightVo recipeWeight : recipe.getRecipeWeightList()) {
            BigDecimal currentProductQty = newProductQty;
            String glueCode = recipeWeight.getRecipeMaterialName(); // 子胶编号
            String majorType = recipeWeight.getMajorType();
            BigDecimal setWeight = BigDecimalUtil.valueOfZero(recipeWeight.getSetWeight()); // 子胶的单车消耗量
            String realMajorType = RecipeUtil.getMajorType(glueCode, majorType, setWeight, maxSetWeight); // 真正的物料类型
            BigDecimal stockNum; // 子胶可生产车数
            if (checkMixMajorType(realMajorType) || GlueEngineConstants.MAJOR_TYPE_SL.equals(realMajorType)) {
                // 掺胶或塑胶类型，需要按重量计算
                setWeight = setWeight.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ONE : setWeight;
                // 取出掺胶的库存重量
                BigDecimal stockWeight = glueStock.getStockWeight(glueCode, majorType);
                stockNum = stockWeight.divide(setWeight, 0, RoundingMode.DOWN); // 库存重量换算成可提供生产车数
            } else if (GlueEngineConstants.MAJOR_TYPE_ML.equals(realMajorType)) {
                // 母炼胶类型，需要按车数计算
                BigDecimal conversionRatio = recipeWeight.getConversionRatio(); // 换算比率
                if (conversionRatio == null || conversionRatio.compareTo(BigDecimal.ZERO) == 0) {
                    stockNum = BigDecimal.ZERO;
                } else {
                    // 母炼胶库存 * 换算比率 = 父物料的可生产车数
                    stockNum = glueStock.getStockNum(glueCode, majorType).multiply(conversionRatio).setScale(0,
                            RoundingMode.DOWN);
                }
            } else {
                continue;
            }
            if (stockNum.compareTo(minStock) < 0) {
                // 当库存量少于最小生产数，且不是受限机台产能时，直接跳过本排程
                if (toProductQty.compareTo(stockNum) <= 0) {
                    // 例外：如果剩余待排产数量小于现有库存，则不需要拆分，直接全部排完
                    currentProductQty = toProductQty;
                } else {
//                    // 如果存在分厂需求量，并且现有库存足够分厂需求量，拆分进行排产
//                    if (requireDifference != null && requireDifference.compareTo(stockNum) <= 0) {
//                        currentProductQty = stockNum;
//                    } else {
                        currentProductQty = BigDecimal.ZERO;
//                    }
                }
            } else { // 按原料库存限制计划量，防止原料库存不足无法排产的情况
                currentProductQty = BigDecimalUtil.least(stockNum, currentProductQty);
            }
            // 以较小的排产量作为可排产量
            newProductQty = BigDecimalUtil.least(currentProductQty, newProductQty);
        }
        return newProductQty;
    }

    /**
     * 获取连续生产的剩余量
     *
     * @param scheduleResultList 完成排产记录
     * @param bindScheduleResult 连续生产记录
     * @return 连续生产排产的剩余量
     */
    public static double getContinueSurplusQty(List<GlueScheduleResultVo> scheduleResultList, GlueScheduleResultVo bindScheduleResult) {
        double producted = 0D;
        String continueKey = GenerageMapKeyUtils.createMapKey(bindScheduleResult.getGlue(), bindScheduleResult.getRecipeType());
        for (GlueScheduleResultVo item : scheduleResultList) {
            String itemKey = GenerageMapKeyUtils.createMapKey(item.getGlue(), item.getRecipeType());
            if (continueKey.equals(itemKey)) {
                producted = BigDecimalUtil.add(producted, item.getTotalPlanQty(), 0D);
            }
        }
        // 计算还需要生产的车数
        Double requireQty = bindScheduleResult.getRequireQty();
        return BigDecimalUtil.sub(requireQty, producted);
    }

    /**
     * 扣减分厂需求
     *
     * @param factoryRequireMap 分厂需求
     * @param glueCode          胶料代码
     * @param productedQty      已生产量
     * @return 是否分厂需求
     */
    public static boolean reduceFactoryRequire(Map<String, GlueFactoryRequireVo> factoryRequireMap, String glueCode, BigDecimal productedQty) {
        boolean isRequire = false;
        GlueFactoryRequireVo factoryRequire = factoryRequireMap.get(glueCode);
        if (factoryRequire != null) {
            isRequire = true;
            BigDecimal requireDifference = Optional.ofNullable(factoryRequire.getRequireDifference())
                    .orElse(BigDecimal.ZERO);
            BigDecimal newRequire = requireDifference.subtract(productedQty); // 需求量扣减已排量
            if (newRequire.compareTo(BigDecimal.ZERO) <= 0) {
                factoryRequireMap.remove(glueCode); // 只要需求量扣减完，则可以直接移除该胶料的需求量
            } else {
                factoryRequire.setRequireDifference(newRequire);
            }
        }
        return isRequire;
    }

    /**
     * 存在接续生产的排产，优先排在后面生产
     *
     * @param queue           排产队列
     * @param currentSchedule 当前排产记录
     * @param currentTime     当前时间
     * @param machineProduct  机台产能对象
     */
    public static void continueSchedule(ScheduleEventQueue queue,
                                        GlueScheduleResultVo currentSchedule,
                                        Date currentTime,
                                        GlueScheduleMachineProductVo machineProduct) {
        if (currentSchedule == null) {
            return;
        }
        Integer currentShiftClass = ShiftClassUtil.getShiftClass(currentTime); // 当前班次为开始时间的所在班次
        // 校验机台状态，如果为关机状态，直接退出
        if (!machineProduct.getStatus(currentShiftClass)) {
            return;
        }
        if (currentSchedule.getBindScheduleResult() == null) {
            return;
        }

        // 如果当前排程存在需要绑定生产的记录，不考虑单班最大排产数，将需求量全部排上（如果是掺胶配方，可能无法堆满需求），尽可能扣减对应母胶的库存
        GlueScheduleResultVo bindScheduleResult = currentSchedule.getBindScheduleResult();
        // 计算排产的剩余量
        double surplusQty;
        if (Boolean.TRUE.equals(bindScheduleResult.getProductionModelTag())) {
            // 如果是生产模式,剩余量就是需求量
            surplusQty = bindScheduleResult.getRequireQty();
        } else {
            // 接续生产
            surplusQty = ScheduleEventUtils.getContinueSurplusQty(queue.getScheduleResult(), bindScheduleResult);
        }
        if (surplusQty <= 0) {
            return;
        }
        // 间隔时间、参数配置
        Map<String, Long> mixingTimeMap = queue.getMixingTimeMap();
        Map<String, String> params = queue.getParams();

        // 优先胶料如果还有计划量，先跳过接续排产，尽量要同胶料换班优先处理
        if (currentSchedule.getPlanQty() != null && currentSchedule.getProductedQty() != null
                && currentSchedule.getPlanQty().compareTo(currentSchedule.getProductedQty()) > 0) {
            return;
        }

        BigDecimal toProductQty = BigDecimal.valueOf(surplusQty);
        MesPmtRecipeVo recipe = bindScheduleResult.getPmtRecipe();
        // 计算生产时间
        Long mixTime = recipe.getSummerMixTime(); // 炼胶时间
        Long intervalTime = mixingTimeMap.getOrDefault(GenerageMapKeyUtils.createMapKey(bindScheduleResult.getGlue(), bindScheduleResult.getMachineCode()),
                new Long(params.get(GlueEngineConstants.MIX_INTERVAL_TIME))); // 炼胶间隔时间
        BigDecimal actualProductQty = getActualProductQty(currentShiftClass, currentTime, machineProduct, mixTime, intervalTime, toProductQty, bindScheduleResult);

        // 添加占用机台事件，提前预占机台
        queue.addEvent(new ContinueProductEvent(machineProduct, actualProductQty, bindScheduleResult), currentTime);

        long productTime = (mixTime + intervalTime) * actualProductQty.longValue(); // 总生产时间 = （炼胶时间 + 间隔时间）* 计划数
        Date finishTime = DateUtils.addSeconds(currentTime, (int) productTime); // 完成生产时间

        // 加上间隔时间，接续生产
        Integer switchTime = new Integer(params.getOrDefault(GlueEngineConstants.SCHEDULE_SWITCH_TIME, "0"));
        Date finishEventTime = DateUtils.addSeconds(finishTime, switchTime.intValue());
        
        // 如果有可以生产的车数，占用对应的机台产能
        if (actualProductQty.compareTo(BigDecimal.ZERO) > 0) {
            // 删除分厂需求量
            boolean isRequire = ScheduleEventUtils.reduceFactoryRequire(queue.getFactoryRequireMap(), bindScheduleResult.getGlue(), actualProductQty);
            // 根据班制设置排产的字段：班制计划量、班制开始时间、班制结束时间
            ScheduleEventUtils.setShiftPlanField(bindScheduleResult, currentShiftClass, actualProductQty, currentTime, finishTime, isRequire);
            // 提前扣减产能
            // 扣减掉生产时长，扣减量 = 当前时间 - 开始生产时间
            BigDecimal machineProductTime = machineProduct.getProductTime(currentShiftClass);
            machineProductTime = machineProductTime.subtract(new BigDecimal(finishEventTime.getTime() - currentTime.getTime())
                    .divide(ONE_THOUSAND, 0, RoundingMode.DOWN)); // 换算成秒
            machineProduct.updateProductTime(machineProductTime, currentShiftClass);
            // 预排停放和生产完成事件
            ScheduleEventUtils.addFinishEvent(queue, bindScheduleResult, actualProductQty, params, finishTime, mixTime, intervalTime, currentTime, false);
        }

        if (actualProductQty.compareTo(toProductQty) >= 0) {
            // 全部生产完成，尝试将连续胶料的连续胶料进行排产，可能是单班连续，也可能是换班连续
            continueSchedule(queue, currentSchedule.getBindScheduleResult(), finishEventTime, machineProduct);

            return;
        }

        // 如果当前班次产能不足，提前占用下个班次
        Integer nextShiftClass = ShiftClassUtil.getNextShiftClass(currentShiftClass);
        if (nextShiftClass == null
                || !machineProduct.getStatus(nextShiftClass)
                || machineProduct.getProductTime(nextShiftClass) == null
                || machineProduct.getProductTime(nextShiftClass).compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        Date endTime = ShiftClassUtil.getShiftClassEndTime(bindScheduleResult.getScheduleDate(), currentShiftClass);
        Date maxEndTime = finishEventTime.compareTo(endTime) > 0 ? finishEventTime : endTime;
        continueSchedule(queue, currentSchedule, maxEndTime, machineProduct);
    }

    /**
     * 获取实际可生产量
     */
    public static BigDecimal getActualProductQty(Integer currentShiftClass, Date currentTime, GlueScheduleMachineProductVo machineProduct, Long mixTime, Long intervalTime, BigDecimal toProductQty, GlueScheduleResultVo bindScheduleResult) {
        Long continueProductTime = (mixTime + intervalTime) * toProductQty.longValue(); // 总生产时间 = （炼胶时间 + 间隔时间）* 计划数
        BigDecimal expectProductTime = BigDecimalUtil.valueOf(continueProductTime); // 预计生产时长

        // 判断完成时间时间是否超过班次结束时间，如果超过了同样要限制生产量
        Date classEndTime = ShiftClassUtil.getShiftClassEndTime(bindScheduleResult.getScheduleDate(), currentShiftClass); // 本版结束时间
        BigDecimal surplusClassTime = BigDecimalUtil.valueOf(classEndTime.getTime() - currentTime.getTime())
                .divide(ONE_THOUSAND, 0, RoundingMode.DOWN); // 本班剩余可生产时长 = 班次结束时间 - 当前时间
        BigDecimal machineProductTime = machineProduct.getProductTime(currentShiftClass); // 机台剩余时长

        // 剩余生产时长超过本班/本机台剩余产能的情况下，需要限制生产量
        BigDecimal actualProductQty;
        if (expectProductTime.compareTo(BigDecimalUtil.least(surplusClassTime, machineProductTime)) > 0) {
            BigDecimal actualTime = BigDecimalUtil.least(surplusClassTime, machineProductTime);
            // 实际可生产车数 = 可生产时长 / （炼胶时间 + 间隔时间），结果向下取整
            actualProductQty = actualTime.divide(new BigDecimal(mixTime + intervalTime), 0,
                    RoundingMode.DOWN);
        } else {
            actualProductQty = toProductQty;
        }
        return actualProductQty;
    }

    /**
     * 判断物料类型是否是掺胶物料
     *
     * @param majorType 物料类型
     * @return
     */
    public static boolean checkMixMajorType(String majorType) {
        return GlueEngineConstants.MIX_MAJOR_TYPE.contains(majorType);
    }

    /**
     * 判断物料类型是否会消耗库存的物料
     *
     * @param majorType 物料类型
     * @return
     */
    public static boolean checkStockMajorType(String majorType) {
        return GlueEngineConstants.STOCK_MAJOR_TYPE.contains(majorType) || GlueEngineConstants.MAJOR_TYPE_SL.equals(majorType);
    }

    /**
     * 计算总计划量
     */
    public static double getTotalQtyByGlue(List<GlueScheduleResultVo> productionList, String glue) {
        double sumPlanQty = 0D;
        if (CollectionUtils.isEmpty(productionList) || StringUtils.isBlank(glue)) {
            return sumPlanQty;
        }

        for (GlueScheduleResultVo item : productionList) {
            if (glue.equals(item.getGlue())) {
                Double totalPlanQty = item.getTotalPlanQty();
                sumPlanQty = BigDecimalUtil.add(sumPlanQty, totalPlanQty);
            }
        }
        return sumPlanQty;
    }
}
