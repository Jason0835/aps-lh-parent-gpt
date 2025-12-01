package com.zlt.mix.schedule.engine.service.decompose.impl;

import com.ruoyi.common.utils.StringUtils;
import com.zlt.mix.common.core.constant.ZltConstant;
import com.zlt.mix.common.core.enums.ProductDayFlagEnum;
import com.zlt.mix.common.core.utils.BigDecimalUtil;
import com.zlt.mix.common.core.utils.DateUtil;
import com.zlt.mix.common.core.utils.GenerageMapKeyUtils;
import com.zlt.mix.common.engine.constants.EngineConstants;
import com.zlt.mix.common.engine.service.impl.IncrementService;
import com.zlt.mix.schedule.api.domain.entity.GlueDecomposePlan;
import com.zlt.mix.schedule.api.domain.entity.GlueSpanReceive;
import com.zlt.mix.schedule.api.domain.entity.GlueSpanSend;
import com.zlt.mix.schedule.engine.constants.GlueEngineConstants;
import com.zlt.mix.schedule.engine.mapper.DecomposeEngineMapper;
import com.zlt.mix.schedule.engine.service.basicdata.*;
import com.zlt.mix.schedule.engine.service.decompose.DecomposeEngineService;
import com.zlt.mix.schedule.engine.service.decompose.MotherGlueDecomposeService;
import com.zlt.mix.schedule.engine.vo.*;
import com.zlt.mix.setting.api.domain.entity.GlueSafeStock;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 分解胶料需求量引擎Service业务层处理
 */
@Slf4j
@Service
    public class DecomposeEngineServiceImpl implements DecomposeEngineService {

    @Resource
    private DecomposeEngineMapper decomposeEngineMapper;
    @Resource
    private IncrementService incrementService;
    @Resource(name="MotherGlueDecomposeServiceImpl")
    private MotherGlueDecomposeService motherGlueDecomposeService;
    @Resource(name="MotherRecipeDecomposeServiceImpl")
    private MotherGlueDecomposeService motherRecipeDecomposeService;
    @Resource
    private StockEngineService stockEngineService;
    @Resource
    private MachineEngineService machineEngineService;
    @Resource
    private RecipeEngineService recipeEngineService;
    @Resource
    private ParamsEngineService paramsEngineService;
    @Resource
    private MixingMinProductEngineService mixingMinProductEngineService;
    @Resource
    private MixingGlueRecipeMapEngineService mixingGlueRecipeMapEngineService;



    /**
     * 根据终炼胶的汇总计划分解出对应的母炼胶的日计划
     * @param planDate 计划日期，格式：yyyy-MM-dd
     * @param paramMixArea 密炼区
     */
    @Transactional(rollbackFor = Exception.class)
    public void decomposePlan(Date planDate, String paramMixArea) {
        if(planDate == null) {
            log.error("计划日期为空");
            return;
        }
        List<GlueDecomposePlan> decomposePlanList = decomposeEngineMapper.listWaitForDecomposePlan(planDate, paramMixArea); //查询出带分解的终炼胶计划列表
        if(decomposePlanList == null || decomposePlanList.isEmpty()) {
            log.error("查询出带分解的终炼胶计划列表为空");
            return;
        }
        
        Map<String, List<GlueDecomposePlan>> decomposePlanMap = decomposePlanList.stream().collect(Collectors.groupingBy(GlueDecomposePlan::getMixArea)); //根据密炼区进行分组

        for(Map.Entry<String, List<GlueDecomposePlan>> entry : decomposePlanMap.entrySet()) {
            String mixArea = entry.getKey(); //密炼区
            Map<String, String> params = paramsEngineService.mapGlueParams(mixArea);   //胶料参数信息
            Map<String, String> glueMachineMap = machineEngineService.mapGlueMachine(mixArea);  //胶料对应的机台Map
            List<GlueDecomposePlan> finalGluelist = entry.getValue();  //某密炼区下的胶料数据
            String batchNo = incrementService.getSequence3(EngineConstants.DECOMPOSE_PREFIX + mixArea + DateUtil.formatDateYmd(planDate));  //创建批次号
            List<GlueAreaMachineVo> finalGlueAreaMachineList = finalGluelist.stream().map(r->new GlueAreaMachineVo(r.getMixArea(), r.getGlue(), r.getMachineCode())).collect(Collectors.toList()); //胶料+密炼区+机台列表

            Map<String, List<GlueAreaMachineVo>> motherGlueMap = new HashMap<>();
            if(EngineConstants.DECOMPOSE_GLUE_TYPE_0.equals(params.get(EngineConstants.DECOMPOSE_GLUE_TYPE))) {
                //分解胶料方式：拿“终炼母炼分解表”来进行分解
                motherGlueMap = motherGlueDecomposeService.parseGlueDecompose(mixArea, finalGlueAreaMachineList, glueMachineMap);  //终炼胶对应的母炼胶列表Map
            } else {
                //分解胶料方式：直接拿配方表进行分解
                motherGlueMap = motherRecipeDecomposeService.parseGlueDecompose(mixArea, finalGlueAreaMachineList, glueMachineMap);  //终炼胶对应的母炼胶列表Map
            }
            List<GlueDecomposePlan> gluePlanList = buildDecomposePlan(mixArea, planDate, params, finalGluelist, batchNo, finalGlueAreaMachineList, motherGlueMap);  //构建出终炼母炼分解胶料需求量对象数据（库存、计划量、生产量，终炼母炼层级等）

            // 查询最少排产的用量
            Map<String, BigDecimal> mixingMinProductMap = mixingMinProductEngineService.mapMixingMinProduct(mixArea);
            // 相同胶料合并，重算生产量
            gluePlanList = mergeDecomposePlanByGlue(gluePlanList, mixingMinProductMap,  true);

            this.synclueDecomposePlanToLog(planDate, mixArea);   //同步之前的数据到日志表
            
            // 对应有生产量的胶料，其称重配方存在塑胶，需要补充塑胶的用量
            MesPmtRecipeVo recipeParams = new MesPmtRecipeVo();
            recipeParams.setMixArea(mixArea);
            Map<String, MesPmtRecipeVo> mesPmtRecipeMap = recipeEngineService.mapSLGLueRecipe(recipeParams);
            List<GlueDecomposePlan> slDecomposePlanList = getSLDecomposePlanList(planDate, mixArea, gluePlanList, mesPmtRecipeMap, mixingMinProductMap);
            gluePlanList.addAll(slDecomposePlanList);

            decomposeEngineMapper.batchInsertGlueDecomposePlan(gluePlanList);  //把分解好的计划列表 进行入库
            gluePlanList.clear();
        }
    }

    /**
     * 补充分解计划对应塑炼胶的用量
     *
     * @param planDate        计划日期
     * @param mixArea         密炼区
     * @param gluePlanList    分解计划
     * @param mesPmtRecipeMap 塑炼胶称重配方
     * @return 塑炼胶计划
     */
    private List<GlueDecomposePlan> getSLDecomposePlanList(Date planDate,
                                                           String mixArea,
                                                           List<GlueDecomposePlan> gluePlanList,
                                                           Map<String, MesPmtRecipeVo> mesPmtRecipeMap,
                                                           Map<String, BigDecimal> mixingMinProductMap) {
        List<GlueDecomposePlan> slDecomposeList = new ArrayList<>();
        if (CollectionUtils.isEmpty(gluePlanList)) {
            return slDecomposeList;
        }

        // 记录当前分解塑炼胶的用量
        Map<String, Long> slMap = new HashMap<>();
        // 记录当前分解计划判断塑炼胶是否收尾
        Map<String, String> slFinishMap = new HashMap<>();
        // 记录塑胶是否为自动排产产生的
        Map<String, String> dataSourceMap = new HashMap<>(); 
        for (GlueDecomposePlan itemPlan : gluePlanList) {
            if (itemPlan.getProduceQty() == null || itemPlan.getProduceQty() <= 0) {
                continue;
            }
            MesPmtRecipeVo recipeVo = mesPmtRecipeMap.get(GenerageMapKeyUtils.createMapKey(itemPlan.getGlue(), itemPlan.getMachineCode()));
            if (recipeVo == null) {
                continue;
            }
            // 如果当前胶料是塑炼胶，跳过
            if (GlueEngineConstants.MAJOR_TYPE_SL.equals(recipeVo.getMajorType())) {
                continue;
            }
            List<MesPmtRecipeWeightVo> recipeWeightList = recipeVo.getRecipeWeightList();
            if (CollectionUtils.isEmpty(recipeWeightList)) {
                continue;
            }
            String dayFlag = itemPlan.getDayFlag();

            // 根据分解计划的计划量和称重比例，计算对应的塑胶计划量
            for (MesPmtRecipeWeightVo itemWeight : recipeWeightList) {
                if (itemWeight.getConversionRatio() == null || StringUtils.isBlank(itemWeight.getRecipeMaterialName())) {
                    continue;
                }
                String weightName = itemWeight.getRecipeMaterialName();
                String slKey = GenerageMapKeyUtils.createMapKey(weightName, dayFlag);
                Long slQty = slMap.getOrDefault(slKey, 0L);
                slQty += itemWeight.getConversionRatio().multiply(BigDecimal.valueOf(itemPlan.getProduceQty()))
                        .setScale(0, RoundingMode.CEILING).longValue();
                slMap.put(slKey, slQty);
                // 如果有非收尾，标记为非收尾
                if (ZltConstant.IS_FINISHING_NO.equals(itemPlan.getIsFinishing())) {
                    slFinishMap.put(weightName, ZltConstant.IS_FINISHING_NO);
                }
                String dataSource = itemPlan.getDataSource();
                if (ZltConstant.DECOMPOSE_SOURCE_AUTO.equals(dataSource)) {
                    // 如果有分解计划，标记塑胶为分解计划产生的
                    dataSourceMap.put(weightName, dataSource);
                }else if(!dataSourceMap.containsKey(weightName)){
                    // 如果没有映射，记录产生的来源
                    dataSourceMap.put(weightName, dataSource);
                }
            }
        }

        if (slMap.isEmpty()) {
            return slDecomposeList;
        }

        GlueDecomposePlan plan = gluePlanList.get(0);

        // 查询对应安全库存和母炼库存（塑炼胶，暂时记录对应库存到母炼库存中）
        List<GlueAreaMachineVo> areaGlueList = new ArrayList<>();
        for (String key : slMap.keySet()) {
            String glue = key.split(GenerageMapKeyUtils.SPLT_CHAR)[0];
            GlueAreaMachineVo areaMachineVo = new GlueAreaMachineVo();
            areaMachineVo.setMixArea(mixArea);
            areaMachineVo.setGlue(glue);
            areaGlueList.add(areaMachineVo);
        }
        Map<String, Double> motherStockMap = stockEngineService.mapMotherGlueStock(planDate, areaGlueList);
        Map<String, GlueSafeStock> safeStockMap = stockEngineService.mapGlueSafeStock(areaGlueList);
        // 查询胶料配方映射的胶料名称Map
        Map<String, String> glueRecipeMap = mixingGlueRecipeMapEngineService.mapGlueRecipe(mixArea);
        // 查询胶料配方映射的反转白班计划量的Map
        Map<String, String> reserveGlueRecipeMap = mixingGlueRecipeMapEngineService.mapReserveGlueRecipe(mixArea);
        // 昨日排产白班的计划计划量
        Map<String, Double> lastGlueDayPlanMap = stockEngineService.mapLastGlueDayPlan(mixArea, planDate, glueRecipeMap, reserveGlueRecipeMap);
        // 获取塑炼胶昨日白班的消耗量
        Map<String, Double> lastGlueConsumePlanMap = getSlLastConsumeMap(planDate, mixArea, mesPmtRecipeMap);

        List<String> glueList = slMap.keySet().stream().map(key -> key.split(GenerageMapKeyUtils.SPLT_CHAR)[0]).collect(Collectors.toList());
        Map<String, String> glueMachineMap = machineEngineService.mapGlueMachineByGLueList(mixArea, glueList);  //胶料对应的机台Map

        Map<String, GlueDecomposePlan> day1PlanMap = slMap.entrySet().stream()
                .filter(e -> e.getKey().split(GenerageMapKeyUtils.SPLT_CHAR)[1].equals(ProductDayFlagEnum.DAY1.getCode()))
                .collect(Collectors.toMap(e -> mixArea + e.getKey().split(GenerageMapKeyUtils.SPLT_CHAR)[0], e -> {
                    String montherKey = mixArea + e.getKey().split(GenerageMapKeyUtils.SPLT_CHAR)[0];
                    GlueDecomposePlan itemDecompose = new GlueDecomposePlan();
                    itemDecompose.setPlanQty(BigDecimalUtil.valueOf(e.getValue()).doubleValue());
                    Double stockNum = motherStockMap.getOrDefault(montherKey, 0D);
                    Double motherGlueDayPlan = lastGlueDayPlanMap.getOrDefault(montherKey, 0D);
                    Double hightConsume = lastGlueConsumePlanMap.getOrDefault(montherKey, 0D);
                    stockNum = BigDecimalUtil.add(stockNum, motherGlueDayPlan);
                    stockNum = BigDecimalUtil.sub(stockNum, hightConsume);
//                    stockNum = (stockNum <= 0 ? 0D : stockNum);
                    itemDecompose.setStockQty(stockNum);
                    this.countProduceQty(itemDecompose);
                    return itemDecompose;
                }));
        
        // 构建塑炼胶的分解计划
        slMap.forEach((key, planQty) -> {
            String glue = key.split(GenerageMapKeyUtils.SPLT_CHAR)[0];
            String dayFlag = key.split(GenerageMapKeyUtils.SPLT_CHAR)[1];
            boolean isDay1 = ProductDayFlagEnum.DAY1.getCode().equals(dayFlag);
            String montherKey = mixArea + glue;
            GlueDecomposePlan day1ItemDecompose = day1PlanMap.get(montherKey);
            GlueDecomposePlan itemDecompose = isDay1? day1ItemDecompose: new GlueDecomposePlan();
            itemDecompose.setDataSource(dataSourceMap.getOrDefault(glue, ZltConstant.DECOMPOSE_SOURCE_AUTO));
            itemDecompose.setBatchNo(plan.getBatchNo());
            itemDecompose.setCollectBatchNo(plan.getCollectBatchNo());
            itemDecompose.setPlanDate(plan.getPlanDate());
            itemDecompose.setMixArea(plan.getMixArea());
            // 默认机台
            itemDecompose.setMachineCode(glueMachineMap.get(montherKey));
            itemDecompose.setGlue(glue);
            itemDecompose.setPlanQty(Double.valueOf(planQty));
            //计算预计库存 = 8点钟库存+ APS昨日白班母胶计划量 - 预计昨日白班胶料消耗量
            Double motherGlueDayPlan = isDay1? lastGlueDayPlanMap.getOrDefault(montherKey, 0D): 0D;
            if (!isDay1) { // 第二天分解接话，需要根据第一天的分解计划重算预计库存
                motherGlueDayPlan = 0D;
                if (day1ItemDecompose != null) {
                    Double day2StockNum = BigDecimalUtil.sub(BigDecimalUtil.add(day1ItemDecompose.getStockQty(), day1ItemDecompose.getProduceQty()), day1ItemDecompose.getPlanQty());
                    itemDecompose.setStockQty(day2StockNum);
                } else { // 如果上一天没有生产该塑炼胶（库存足够），则根据实际库存与消耗两计算预计库存
                    Double stockNum = motherStockMap.getOrDefault(montherKey, 0D); // 塑炼胶实际库存
                    Double hightConsume = lastGlueConsumePlanMap.getOrDefault(montherKey, 0D); // 上级胶消耗量
                    itemDecompose.setStockQty(BigDecimalUtil.sub(stockNum, hightConsume)); // 预计库存
                }
            }
            // 安全库存、预生产库库存倍数，计算对应生产量
            GlueSafeStock safeStock = safeStockMap.getOrDefault(montherKey, new GlueSafeStock());
            itemDecompose.setSafeStockQty(Optional.ofNullable(safeStock.getSafeStock()).orElse(BigDecimal.ZERO).doubleValue());
            // 塑炼类似母炼胶，无需考虑预生产库存倍数
            // double reserveStockRate = Optional.ofNullable(safeStock.getReserveStockRate()).orElse(BigDecimal.ZERO).doubleValue();
            // itemDecompose.setReserveStockRate(BigDecimalUtil.div(reserveStockRate, GlueEngineConstants.DAILY_DOSE_STOCK_RATE, 4));
            // 塑炼胶收尾标记
            itemDecompose.setIsFinishing(slFinishMap.getOrDefault(glue ,ZltConstant.IS_FINISHING_YES));
            // 计算终炼胶料的生产计划量
            this.countProduceQty(itemDecompose);
            // 补够起步量
            BigDecimal minProduct = mixingMinProductMap.get(itemDecompose.getGlue());
            computeStartProduceQty(itemDecompose, minProduct, itemDecompose.getDataSource(), motherGlueDayPlan);
            // 暂不设置对应上级胶相关字段，避免自动排产重复计算优先级，计算优先级直接根据对应一段胶的优先级调高

            itemDecompose.setDayFlag(dayFlag);
            itemDecompose.setBaseValue(null);
            slDecomposeList.add(itemDecompose);
        });

        return slDecomposeList;
    }

    /**
     * 获取昨日白班塑炼胶的消耗量
     * 
     * @param planDate 计划日期
     * @param mixArea 密炼区
     * @param mesPmtRecipeMap 塑料胶分解的配方
     * @return 昨日白班塑炼胶的消耗量
     */
    private Map<String, Double> getSlLastConsumeMap(Date planDate, String mixArea, Map<String, MesPmtRecipeVo> mesPmtRecipeMap) {
        // 获取昨日白班塑炼胶的消耗量
        Map<String, Double> lastGlueConsumePlanMap = new HashMap<>();
        // 昨日白班的计划量
        Map<String, Double> lastGlueMachinePlanMap = stockEngineService.mapLastGlueMachineDayPlan(mixArea, planDate);
        lastGlueMachinePlanMap.forEach((glueMachineKey, dayPlan) -> {
            MesPmtRecipeVo recipeVo = mesPmtRecipeMap.get(glueMachineKey);
            if (recipeVo == null) {
                return;
            }
            List<MesPmtRecipeWeightVo> recipeWeightList = recipeVo.getRecipeWeightList();
            if (CollectionUtils.isEmpty(recipeWeightList)) {
                return;
            }

            // 根据分解计划的计划量和称重比例，计算对应的塑胶消耗量
            for (MesPmtRecipeWeightVo itemWeight : recipeWeightList) {
                if (itemWeight.getConversionRatio() == null || StringUtils.isBlank(itemWeight.getRecipeMaterialName())) {
                    continue;
                }
                if(!GlueEngineConstants.MAJOR_TYPE_SL.equals(itemWeight.getMajorType())){
                    continue;
                }

                String montherKey = mixArea + itemWeight.getRecipeMaterialName();
                // 合计各个塑炼胶的消耗量
                Double consumeNum = lastGlueConsumePlanMap.getOrDefault(montherKey, 0D);
                consumeNum = BigDecimalUtil.add(consumeNum, itemWeight.getConversionRatio().multiply(BigDecimal.valueOf(dayPlan))
                        .setScale(0, RoundingMode.CEILING).longValue());
                if (consumeNum <= 0) {
                    consumeNum = 0D;
                }
                lastGlueConsumePlanMap.put(montherKey, consumeNum);
            }
        });
        
        return lastGlueConsumePlanMap;
    }

    /**
     * 相同胶料合并，重算生产量，如果是合并的记录，需要从最上层的母胶开始重算
     *
     * @param gluePlanList 待合并的分解列表
     * @param recalculate  是否需要重新计算
     * @return 合并后的分解列表
     */
    private List<GlueDecomposePlan> mergeDecomposePlanByGlue(List<GlueDecomposePlan> gluePlanList, Map<String, BigDecimal> mixingMinProductMap, boolean recalculate) {
        if (CollectionUtils.isEmpty(gluePlanList)) {
            return gluePlanList;
        }
        // 记录合并后记录、重复记录的对应关系
        Map<String, GlueDecomposePlan> mergeDecomposePlanMap = new HashMap<>();
        Set<String> repeatDecomposeSet = new HashSet<>();

        // 合并计划量，重算库存量和生产量（此时如果有同一个分解多段胶料都重复的情况，只有最上层的重复记录的计划量是正确的）
        for (GlueDecomposePlan item : gluePlanList) {
            // 重新插入新记录
            item.setId(null);
            String mapKey = GenerageMapKeyUtils.createMapKey(item.getGlue(), item.getDayFlag());
            GlueDecomposePlan repeat = mergeDecomposePlanMap.get(mapKey);
            if (repeat == null) {
                mergeDecomposePlanMap.put(mapKey, item);
                continue;
            }

            repeatDecomposeSet.add(repeat.getGlue());
            // 合并计划量
            repeat.setPlanQty(BigDecimalUtil.add(repeat.getPlanQty(), item.getPlanQty()));
            // 有非收尾就标记为非收尾
            List<String> finishingList = Stream.of(repeat.getIsFinishing(), item.getIsFinishing()).filter(StringUtils::isNotBlank).collect(Collectors.toList());
            if (finishingList.contains(ZltConstant.IS_FINISHING_NO)) {
                repeat.setIsFinishing(ZltConstant.IS_FINISHING_NO);
            }
            if (StringUtils.isNotBlank(item.getUpGlue()) && StringUtils.isNotBlank(repeat.getUpGlue())) {
                List<String> upGlueList = Arrays.stream(repeat.getUpGlue().split(",")).filter(StringUtils::isNotBlank).collect(Collectors.toList());
                if (recalculate) { // 只有分解需要重新计算库存
                    // 如果两者的上级胶料不一致，需要重算库存量
                    if (!upGlueList.contains(item.getUpGlue())) {
                        // 库存+昨日计划-昨日总消耗
                        double originStock = BigDecimalUtil.valueOfZero(repeat.getOriginStockQty()).doubleValue();
                        double lastDayPlan = BigDecimalUtil.valueOfZero(repeat.getLastDayPlan()).doubleValue();
                        double lastDayConsume = BigDecimalUtil.add(0D, item.getLastDayConsume(), repeat.getLastDayConsume());
                        originStock = BigDecimalUtil.sub(BigDecimalUtil.add(originStock, lastDayPlan), lastDayConsume);
                        repeat.setStockQty(originStock > 0 ? originStock : 0D);
                    }
                }

                // 汇总对应上级胶、上级胶机台、终炼胶机台
                if (!upGlueList.contains(item.getUpGlue())) {
                    repeat.setUpGlue(Stream.of(repeat.getUpGlue(), item.getUpGlue())
                            .filter(StringUtils::isNotBlank).collect(Collectors.joining(",")));
                    repeat.setUpMachineCode(Stream.of(repeat.getUpMachineCode(), item.getUpMachineCode())
                            .filter(StringUtils::isNotBlank).collect(Collectors.joining(",")));
                    repeat.setFinalGlueMachine(Stream.of(repeat.getFinalGlueMachine(), item.getFinalGlueMachine())
                            .filter(StringUtils::isNotBlank).collect(Collectors.joining(",")));
                }
            }

            // 重算生产量
            this.countProduceQty(repeat);
            mergeDecomposePlanMap.put(mapKey, repeat);
        }

        ArrayList<GlueDecomposePlan> mergeDecomposePlanList = new ArrayList<>(mergeDecomposePlanMap.values());
        if (repeatDecomposeSet.isEmpty()) {
            return mergeDecomposePlanList;
        }

        // 重新计算计划量和生产量（处理同一个分解多段胶料都重复的情况）
        List<GlueAreaMachineVo> repeatList = mergeDecomposePlanList.stream().map(v -> new GlueAreaMachineVo(v.getMixArea(), v.getGlue(), v.getMachineCode())).collect(Collectors.toList());
        Map<String, Double> glueWeightMap = recipeEngineService.mapGlueWeight(repeatList, null);
        Map<String, Double> minGlueWeightMap = recipeEngineService.mapMinGlueWeight(repeatList, null);
        // 记录重算过的记录
        Set<String> recalculateSet = new HashSet<>();
        // 校验分解记录存在循环
        Set<String> checkRing = new HashSet<>();
        // 对应上级胶的生产量变更的情况，会影响下级胶的计划量，需要重新计算
        for (String glue : repeatDecomposeSet) {
            recursionRecalculateRepeat(glue, glueWeightMap, minGlueWeightMap, recalculateSet, mergeDecomposePlanMap, repeatDecomposeSet, checkRing, mixingMinProductMap);
        }

        return mergeDecomposePlanList;
    }

    /**

     * 递归重新计算对应上级胶合并过的下级胶的计划量和生产量
     *
     * @param glue                  当前计算的记录
     * @param glueWeightMap         单车重量
     * @param minGlueWeightMap      最小单车重量
     * @param recalculateSet        已经重新计算过的记录
     * @param mergeDecomposePlanMap 合并后的记录
     * @param repeatDecomposeSet    重复的分解记录
     * @param checkRing             重复的分解记录
     */
    private void recursionRecalculateRepeat(String glue, 
                                            Map<String, Double> glueWeightMap,
                                            Map<String, Double> minGlueWeightMap,
                                            Set<String> recalculateSet,
                                            Map<String, GlueDecomposePlan> mergeDecomposePlanMap,
                                            Set<String> repeatDecomposeSet,
                                            Set<String> checkRing,
                                            Map<String, BigDecimal> mixingMinProductMap) {
        // 重复记录跳过
        if (recalculateSet.contains(glue)) {
            return;
        }

        GlueDecomposePlan glueDecomposePlan = mergeDecomposePlanMap.get(glue);
        if (glueDecomposePlan == null) {
            recalculateSet.add(glue);
            return;
        }

        // 没有上级胶，无需重新计算
        String upGlues = glueDecomposePlan.getUpGlue();
        if (StringUtils.isBlank(upGlues)) {
            recalculateSet.add(glue);
            return;
        }

        String[] upGlueArray = upGlues.split(",");
        List<String> repeatUpGlueList = Arrays.stream(upGlueArray).filter(repeatDecomposeSet::contains).collect(Collectors.toList());
        // 上级胶没有重复记录，无需重新计算
        if (CollectionUtils.isEmpty(repeatUpGlueList)) {
            recalculateSet.add(glue);
            return;
        }

        // 如果配方存在循环，当前记录标记已计算
        if (checkRing.contains(glue)) {
            recalculateSet.add(glue);
            return;
        }

        // 上级胶也是重复的分解记录，先重新计算上级胶的计划量和生产量
        checkRing.add(glue);
        for (String upGlue : repeatUpGlueList) {
            recursionRecalculateRepeat(upGlue, glueWeightMap, minGlueWeightMap, recalculateSet, mergeDecomposePlanMap, repeatDecomposeSet, checkRing, mixingMinProductMap);
        }
        checkRing.remove(glue);

        // 根据上级胶的计划量，重算当前胶料的计划量
        double sumPlan = 0D;
        GlueDecomposePlan tempSonGlue = new GlueDecomposePlan();
        BeanUtils.copyProperties(glueDecomposePlan, tempSonGlue);
        for (String upGlue : upGlueArray) {
            GlueDecomposePlan upGlueDecompose = mergeDecomposePlanMap.get(upGlue);
            if (upGlueDecompose == null) {
                continue;
            }

            countMaxPlanQty(upGlueDecompose, tempSonGlue, glueWeightMap, minGlueWeightMap);
            sumPlan = BigDecimalUtil.add(sumPlan, tempSonGlue.getPlanQty());
        }
        glueDecomposePlan.setPlanQty(sumPlan);
        this.countProduceQty(glueDecomposePlan);
        // 补够起步量
        BigDecimal minProduct = mixingMinProductMap.get(glueDecomposePlan.getGlue());
        computeStartProduceQty(glueDecomposePlan, minProduct, glueDecomposePlan.getDataSource(), glueDecomposePlan.getLastDayPlan());
        // 记录已计算
        recalculateSet.add(glue);
    }

    /**
     * 委托方因为机台为空没办法计算出生产量，所以需要跨区确定机台后，在重新计算胶料的生产量
     * @param planDate  计划日期
     * @param mixArea   委托方的密炼区
     * @param retryReceiveIdList 需要重新计算生产量的接收记录的id
     * @return
     */
    public GlueSendReceiveVo retrySpanProductQty(Date planDate, String mixArea, List<Long> retryReceiveIdList) {
        List<GlueDecomposeSpanVo> list = decomposeEngineMapper.listRetrySpanProductQty(planDate, mixArea, retryReceiveIdList);
        if(list.isEmpty()) {
            return null;
        }

        List<GlueAreaMachineVo> glueList = list.stream().map(r->new GlueAreaMachineVo(r.getGlue())).collect(Collectors.toList());
        Map<String, Double> glueWeightMap = recipeEngineService.mapGlueWeight(glueList, null);  /*胶料单车总重Map*/
        GlueDecomposePlan hightGlue = null;  //上级胶

        List<GlueSpanSend> sendList = new ArrayList<>();  //发送列表
        List<GlueSpanReceive> receiveList = new ArrayList<>();  //接收列表
        List<GlueDecomposePlan> glueDecomposePlanList = new ArrayList<>();  //分解胶料计划列表

        boolean sonRetryCount = false; //子胶计划量是否重新计算（因为只要有一个父级胶计划量没有计算出来，都会导致全部子胶计划量没办法计算，所以子胶都需要重新计算）
        for (GlueDecomposeSpanVo glueSpanVo : list) {
            String glue = glueSpanVo.getGlue();
            if (glue.indexOf("/") < 0 ) {  //母炼胶才需要重新计算生产量
                hightGlue = glueSpanVo;
                sonRetryCount = false;
                continue;
            }

            boolean isMachineNull = StringUtils.isBlank(glueSpanVo.getMachineCode());
            GlueDecomposeSpanVo tempGlueSpanVo = new GlueDecomposeSpanVo();
            String oldMachineCode = glueSpanVo.getMachineCode();
            if(isMachineNull) {
                //委托密炼区的机台为空的话，则是指被委托密炼区的机台
                glueSpanVo.setMachineCode(glueSpanVo.getReceiveMachineCode());
            }

            if(sonRetryCount || glueSpanVo.getReceiveId() != null) {
                // todo 跨区相关，暂不修改母胶取最小配方计算计划量
                //如果上级胶的计划量计算有问题，那么上级胶计划量重算后，子胶的分解表计划量都需要重新计算
                this.countPlanAndProduceQty(hightGlue, glueSpanVo, glueWeightMap);  //重新计算计划量和生产量
                if(isMachineNull) {
                    BeanUtils.copyProperties(glueSpanVo, tempGlueSpanVo);
                    tempGlueSpanVo.setMachineCode(oldMachineCode);
                    glueDecomposePlanList.add(tempGlueSpanVo);
                } else {
                    glueDecomposePlanList.add(glueSpanVo);
                }
            }

            if(glueSpanVo.getReceiveId() != null) {
                GlueSpanSend glueSpanSend = new GlueSpanSend();
                glueSpanSend.setId(glueSpanVo.getSendId());
                glueSpanSend.setSendQty(glueSpanVo.getProduceQty().longValue());
                sendList.add(glueSpanSend);

                GlueSpanReceive glueSpanReceive = new GlueSpanReceive();
                glueSpanReceive.setId(glueSpanVo.getReceiveId());
                glueSpanReceive.setSendId(glueSpanVo.getSendId());
                glueSpanReceive.setSendQty(glueSpanVo.getProduceQty().longValue());
                receiveList.add(glueSpanReceive);
                sonRetryCount = true;
            }
            hightGlue = glueSpanVo;
        }
        if(sendList.isEmpty() || receiveList.isEmpty()) {
            return null;
        }
        return new GlueSendReceiveVo(sendList, receiveList, glueDecomposePlanList);
    }

    /**
     * 构建出终炼母炼分解胶料需求量对象数据（库存、计划量、生产量，终炼母炼层级等）
     * @param mixArea  密炼区
     * @param planDate  计划日期
     * @param params  参数信息
     * @param glueFatherlist  父胶集合
     * @param batchNo   批次号
     * @param fatherGlueAreaMachineList   父胶的密炼区+胶料名称 信息计划
     * @param motherGlueMap 子胶
     * @return
     */
    private List<GlueDecomposePlan> buildDecomposePlan(String mixArea, Date planDate, Map<String, String> params, List<GlueDecomposePlan> glueFatherlist, String batchNo,
                                                       List<GlueAreaMachineVo> fatherGlueAreaMachineList, Map<String, List<GlueAreaMachineVo>> motherGlueMap) {
        List<GlueDecomposePlan> gluePlanList = new ArrayList<>();  //最终分解完成后的胶料分解计划列表
        List<GlueAreaMachineVo> motherAreaGlueList = new ArrayList<>();  //终炼胶对应的母炼胶列表
        motherGlueMap.forEach((k, v)-> motherAreaGlueList.addAll(v));  //拿到这次分解计划中全部终炼胶对应的母炼胶2022-08-04
        List<GlueAreaMachineVo> areaGlueList = new ArrayList<>(fatherGlueAreaMachineList);  //终炼胶 和 母炼胶的list集合
        areaGlueList.addAll(motherAreaGlueList);
        // 查询胶料配方映射的胶料名称Map
        Map<String, String> glueRecipeMap = mixingGlueRecipeMapEngineService.mapGlueRecipe(mixArea);
        // 查询胶料配方映射的反转白班计划量的Map
        Map<String, String> reserveGlueRecipeMap = mixingGlueRecipeMapEngineService.mapReserveGlueRecipe(mixArea);
        Map<String, GlueSafeStock> safeStockMap = stockEngineService.mapGlueSafeStock(areaGlueList);  //终炼母炼安全库存map
        Map<String, Double> lastGlueDayPlanMap = stockEngineService.mapLastGlueDayPlan(mixArea, planDate, glueRecipeMap, reserveGlueRecipeMap);  //获取昨日胶料排程白班计划量Map
        Map<String, List<GlueConsumeVo>> lastGlueDayPlanAndMachineMap = stockEngineService.mapLastGlueDayPlanAndMachine(mixArea, planDate);  //获取昨日胶料排程白班计划量和机台信息Map
        Map<String, Double> lastGlueUnclaimedMap = stockEngineService.mapLastGlueUnclaimed(mixArea, planDate, params.get(GlueEngineConstants.GLUE_UNCLAIMED_IMPORT));  //获取昨日日用量
        Map<String, Double> finalStockMap = stockEngineService.mapFinalGlueStock(planDate, mixArea, lastGlueDayPlanMap, lastGlueUnclaimedMap, reserveGlueRecipeMap);  //终炼胶库存Map
        Map<String, Double> motherStockMap = stockEngineService.mapMotherGlueStock(planDate, motherAreaGlueList);  //母炼胶库存Map
//        Map<String, Double> glueLast8And16PlanMap = stockEngineService.mapGlueLast8And16Plan(mixArea, planDate); //获取胶料前一天排程8-16点的计划量
//        Map<String, Double> glue8And112FinsihMap = stockEngineService.mapGlue8And112Finsih(mixArea, planDate); //取胶料当天8-12点的完成量
        Map<String, Double> glueWeightMap = recipeEngineService.mapGlueWeight(areaGlueList, null);  /*胶料单车总重Map*/
        Map<String, Double> minGlueWeightMap = recipeEngineService.mapMinGlueWeight(areaGlueList, null);  /*胶料最小单车总重Map*/
        // 查询最少排产的用量
        Map<String, BigDecimal> mixingMinProductMap = mixingMinProductEngineService.mapMixingMinProduct(mixArea);
        // 第二天预估交接班库存
        Map<String, Double> finalStockDay2Map = new HashMap<>();
        Map<String, Double> motherStockDay2Map = new HashMap<>();

        for(GlueDecomposePlan decomposePlan : glueFatherlist) {
            String glue = decomposePlan.getGlue();  //终炼胶代号
            decomposePlan.setBatchNo(batchNo);  //设置批次号
            decomposePlan.setDayFlag(ProductDayFlagEnum.DAY1.getCode());
            String oriDataSource = decomposePlan.getDataSource();
            decomposePlan.setDataSource(ZltConstant.DECOMPOSE_SOURCE_AUTO);  //设置数据来源：分解计划
//            decomposePlan.setStockQty(this.countStockCar(decomposePlan, finalStockMap, glueWeightMap));  //根据库存重量，计算出终炼胶库存车数
            decomposePlan.setStockQty(finalStockMap.getOrDefault(decomposePlan.getMixArea() + decomposePlan.getGlue(), 0D));  //终炼胶库存
            GlueSafeStock safeStock = safeStockMap.getOrDefault(decomposePlan.getMixArea() + decomposePlan.getGlue(), new GlueSafeStock());
            decomposePlan.setSafeStockQty(Optional.ofNullable(safeStock.getSafeStock()).orElse(BigDecimal.ZERO).doubleValue());  //终炼胶安全库存
            // 终炼胶的备库比例
            double zlStockRate = Double.parseDouble(
                    params.getOrDefault(GlueEngineConstants.ZL_DAILY_DOSE_STOCK_RATE, String.valueOf(GlueEngineConstants.ZL_DEFAULT_DAILY_DOSE_STOCK_RATE)));
            // 先根据母炼的备库考虑，先计算母炼的计划量
            double mlStockRate = Double.parseDouble(
                    params.getOrDefault(GlueEngineConstants.ML_DAILY_DOSE_STOCK_RATE, String.valueOf(GlueEngineConstants.ML_DEFAULT_DAILY_DOSE_STOCK_RATE)));
            // 预生产库存倍数需要除以备库比例
            double reserveStockRate = Optional.ofNullable(safeStock.getReserveStockRate()).orElse(BigDecimal.ZERO).doubleValue();
            decomposePlan.setReserveStockRate(BigDecimalUtil.div(reserveStockRate, zlStockRate, 4));
            decomposePlan.setBaseValue(decomposePlan.getId());
            Double dayPlanQty = lastGlueDayPlanMap.getOrDefault(mixArea + decomposePlan.getGlue(), 0D);
            BigDecimal minProduct = mixingMinProductMap.get(decomposePlan.getGlue());
            List<GlueAreaMachineVo> motherGlueAreaList = motherGlueMap.get(glue);  //获取此终炼胶对应的母炼胶列表
            // 原始计划量
            Double oriPlanQty = decomposePlan.getPlanQty();
            if (EngineConstants.IS_FINISHING_YES.equals(decomposePlan.getIsFinishing()) || ZltConstant.DECOMPOSE_SOURCE_ADD.equals(oriDataSource)) {
                mlStockRate = 1D;
                zlStockRate = 1D;
            }
            decomposePlan.setPlanQty(BigDecimalUtil.roundUp(BigDecimalUtil.mul(oriPlanQty, mlStockRate), 0));
            this.countProduceQty(decomposePlan);  //计算终炼胶料的生产计划量
            computeStartProduceQty(decomposePlan, minProduct, oriDataSource, dayPlanQty);
            //根据终炼胶和其他基础信息，创建全部母炼胶的对象信息
            List<GlueDecomposePlan> motherGlueList = createMotherGlueDecomposePlan(decomposePlan, motherGlueAreaList, safeStockMap, motherStockMap,
                    lastGlueDayPlanMap, lastGlueDayPlanAndMachineMap, glueWeightMap, minGlueWeightMap, mixingMinProductMap, oriDataSource);
            // 后根据终炼的备库考虑，再计算终炼的生产量
            decomposePlan.setPlanQty(BigDecimalUtil.roundUp(BigDecimalUtil.mul(oriPlanQty, zlStockRate), 0));
            this.countProduceQty(decomposePlan);  //计算终炼胶料的生产计划量
            computeStartProduceQty(decomposePlan, minProduct, oriDataSource, dayPlanQty);
            gluePlanList.add(decomposePlan);
            gluePlanList.addAll(motherGlueList);
            
            // 计算第二天计划量
            GlueDecomposePlan day2DecomposePlan = new GlueDecomposePlan();
            BeanUtils.copyProperties(decomposePlan, day2DecomposePlan);
            day2DecomposePlan.setDayFlag(ProductDayFlagEnum.DAY2.getCode());
            Double day2Stock = BigDecimalUtil.sub(BigDecimalUtil.add(decomposePlan.getStockQty(), decomposePlan.getProduceQty()), decomposePlan.getPlanQty());
            day2DecomposePlan.setStockQty(day2Stock);
            finalStockDay2Map.put(decomposePlan.getMixArea() + decomposePlan.getGlue(), day2Stock);
            for (GlueDecomposePlan motherDecomposePlan: motherGlueList) {
                Double day2MotherStock = BigDecimalUtil.sub(BigDecimalUtil.add(motherDecomposePlan.getStockQty(), motherDecomposePlan.getProduceQty()), motherDecomposePlan.getPlanQty());
                motherStockDay2Map.put(motherDecomposePlan.getMixArea() + motherDecomposePlan.getGlue(), day2MotherStock);
            }
            this.countProduceQty(day2DecomposePlan);  //计算终炼胶料的生产计划量
            computeStartProduceQty(day2DecomposePlan, minProduct, oriDataSource, 0D);
            //根据终炼胶和其他基础信息，创建全部母炼胶的对象信息
            List<GlueDecomposePlan> day2MotherGlueList = createMotherGlueDecomposePlan(day2DecomposePlan, motherGlueAreaList, safeStockMap, motherStockDay2Map,
                    new HashMap<>(), new HashMap<>(), glueWeightMap, minGlueWeightMap, mixingMinProductMap, oriDataSource); 
            day2DecomposePlan.setPlanQty(BigDecimalUtil.roundUp(BigDecimalUtil.mul(oriPlanQty, zlStockRate), 0));
            this.countProduceQty(day2DecomposePlan);  //计算终炼胶料的生产计划量
            computeStartProduceQty(day2DecomposePlan, minProduct, oriDataSource, 0D);
            gluePlanList.add(day2DecomposePlan);
            gluePlanList.addAll(day2MotherGlueList);
        }
        return gluePlanList;
    }

    /**
     * 如果有最小排程起步限制，补够起步量
     *
     * @param decomposePlan 分解计划
     * @param minProduct    最小批量
     * @param oriDataSource 分解数据来源
     * @param dayPlanQty    昨日日计划
     * @return 是否有调整计划量
     */
    private boolean computeStartProduceQty(GlueDecomposePlan decomposePlan, BigDecimal minProduct, String oriDataSource, Double dayPlanQty) {
        // 如果非收尾 且 不是手动新增的分解，有生产量，白班计划+生产量不足排产起步量的限制，需要调整到起步量
        if (minProduct != null && !EngineConstants.IS_FINISHING_YES.equals(decomposePlan.getIsFinishing())
                && !ZltConstant.DECOMPOSE_SOURCE_ADD.equals(oriDataSource)) {
            if (dayPlanQty == null) {
                dayPlanQty = 0D;
            }
            double produceQty = BigDecimalUtil.add(dayPlanQty, decomposePlan.getProduceQty(), 0D);
//            double produceQty = decomposePlan.getProduceQty();
            if (produceQty > 0 && produceQty < minProduct.doubleValue()) {
                decomposePlan.setProduceQty(BigDecimalUtil.sub(minProduct.doubleValue(), dayPlanQty));
//                decomposePlan.setProduceQty(minProduct.doubleValue());
                return true;
            }
        }
        return false;
    }

    /**
     * 根据库存重量，计算出库存车数
     * @param decomposePlan 分解胶料计划对象
     * @param stockMap  库存map
     * @param glueWeightMap  胶料单车总重map
     * @return 库存车数
     */
    private Double countStockCar(GlueDecomposePlan decomposePlan, Map<String, Double> stockMap, Map<String, Double> glueWeightMap) {
        Double stockCar = 0D;
        String weightKey = decomposePlan.getGlue() + decomposePlan.getMachineCode();
        Double stockWeight = stockMap.getOrDefault(decomposePlan.getMixArea() + decomposePlan.getGlue(), 0D);  //胶料库存重量
        Double glueWeight = glueWeightMap.getOrDefault(weightKey, 0D);  //胶料单车总重
        if(stockWeight != 0 && glueWeight != 0) {
            stockCar = BigDecimalUtil.roundDown(BigDecimalUtil.div(stockWeight, glueWeight), 0);  //库存车=库存重量/单车总重， 结果向下取整
        }
        return stockCar;
    }

    private List<GlueDecomposePlan> buildDecomposePlan(String mixArea, Date planDate, Map<String, String> params, GlueDecomposePlan glueDecomposePlan, String batchNo,
                                                       GlueAreaMachineVo fatherGlueAreaMachine, Map<String, List<GlueAreaMachineVo>> motherGlueMap) {
        return buildDecomposePlan(mixArea, planDate, params, Arrays.asList(glueDecomposePlan), batchNo, Arrays.asList(fatherGlueAreaMachine), motherGlueMap);
    }

    /**
     * 分解胶料需求--新增(可以新增终炼胶，也可以新增母炼胶。新增后要把现新增的胶料的子胶 也一起计算新增进去)
     * @param plan
     */
    @Transactional(rollbackFor = Exception.class)
    public void addDecomposePlan(GlueDecomposePlan plan) {
        Date planDate = plan.getPlanDate();
        String mixArea = plan.getMixArea();
        String gule = plan.getGlue();
        String machineCode = plan.getMachineCode();
        Map<String, String> glueMachineMap = machineEngineService.mapGlueMachine(mixArea);  //胶料对应的机台Map

        String batchNo = decomposeEngineMapper.queryDecomposePlanBatchNo(planDate, mixArea);
        if(StringUtils.isBlank(batchNo)) {
            //如果没有查询到此计划日期的批次号，则创建一个
            batchNo = incrementService.getSequence3(EngineConstants.DECOMPOSE_PREFIX + mixArea + DateUtil.formatDateYmd(planDate));  //创建批次号
        }
        plan.setBatchNo(batchNo);
        plan.setDataSource(ZltConstant.DECOMPOSE_SOURCE_ADD);  //设置数据来源：新增
        plan.setFinalGlueMachine(plan.getGlue());
        plan.setBaseValue(plan.getId());

        Map<String, String> params = paramsEngineService.mapGlueParams(mixArea);
        Map<String, List<GlueAreaMachineVo>> motherGlueMap = new HashMap<>();
        if(EngineConstants.DECOMPOSE_GLUE_TYPE_0.equals(params.get(EngineConstants.DECOMPOSE_GLUE_TYPE))) {
            //分解胶料方式：拿“终炼母炼分解表”来进行分解
            motherGlueMap = motherGlueDecomposeService.parseGlueDecompose(mixArea, Arrays.asList(new GlueAreaMachineVo(mixArea, gule, machineCode)), glueMachineMap);  //终炼胶对应的母炼胶列表Map
        } else {
            //分解胶料方式：直接拿配方表进行分解
            motherGlueMap = motherRecipeDecomposeService.parseGlueDecompose(mixArea, Arrays.asList(new GlueAreaMachineVo(mixArea, gule, machineCode)), glueMachineMap);  //终炼胶对应的母炼胶列表Map
        }
        if(motherGlueMap.isEmpty()) {
            //如果motherGlueMap为空，则新增的胶料有可能是个母炼胶，则需要获取这个母炼胶的下级全部胶料
            List<GlueAreaMachineVo> motherSunList = new ArrayList<>();
            if(EngineConstants.DECOMPOSE_GLUE_TYPE_0.equals(params.get(EngineConstants.DECOMPOSE_GLUE_TYPE))) {
                //分解胶料方式：拿“终炼母炼分解表”来进行分解
                motherSunList = motherGlueDecomposeService.parseGlueDecomposeByMother(plan, glueMachineMap);
            } else {
                //分解胶料方式：直接拿配方表进行分解
                motherSunList = motherRecipeDecomposeService.parseGlueDecomposeByMother(plan, glueMachineMap);
            }
            motherGlueMap.put(gule, motherSunList);
        }

        List<GlueDecomposePlan> gluePlanList = new ArrayList<>();
        // gluePlanList.add(plan);
        List<GlueDecomposePlan> motherGluePlanList = buildDecomposePlan(mixArea, planDate,params, plan, batchNo, new GlueAreaMachineVo(mixArea, gule), motherGlueMap);  //构建出终炼母炼分解胶料需求量对象数据（库存、计划量、生产量，终炼母炼层级等）
        motherGluePlanList.forEach(r->r.setDataSource(ZltConstant.DECOMPOSE_SOURCE_ADD));
        gluePlanList = motherGluePlanList;
        // 加载炼胶单规格最小排产数
        Map<String, BigDecimal> mixingMinProductMap = mixingMinProductEngineService.mapMixingMinProduct(mixArea);
        gluePlanList = mergeDecomposePlanByHistory(gluePlanList, mixingMinProductMap); // 合并历史可能存在重复胶料的记录
        decomposeEngineMapper.updateDecomposePlanDelFlag(gluePlanList);   //新增前把当天已经有的分解计划，先逻辑删除
        decomposeEngineMapper.batchInsertGlueDecomposePlan(gluePlanList);  //把分解好的计划列表 进行入口
        
        // 如果新增的分解计划，涉及到塑炼胶，需要重新计算塑炼的胶的计划量
        updateSLDecomposePlan(mixArea, planDate, gluePlanList, mixingMinProductMap);
    }

    /**
     * 新增补充塑炼胶分解计划
     *
     * @param mixArea      密炼区
     * @param planDate     计划日期
     * @param gluePlanList 分解计划
     */
    public void updateSLDecomposePlan(String mixArea, Date planDate, List<GlueDecomposePlan> gluePlanList, Map<String, BigDecimal> mixingMinProductMap) {
        // 塑炼胶配方
        MesPmtRecipeVo recipeParams = new MesPmtRecipeVo();
        recipeParams.setMixArea(mixArea);
        Map<String, MesPmtRecipeVo> pmtRecipeVoMap = this.recipeEngineService.mapSLGLueRecipe(recipeParams);

        // 判断新增/修改的分解计划，是否涉及需要塑料胶的计划
        boolean slKey = false;
        for (GlueDecomposePlan item : gluePlanList) {
            String mapKey = GenerageMapKeyUtils.createMapKey(item.getGlue(), item.getMachineCode());
            if (pmtRecipeVoMap.containsKey(mapKey)) {

                slKey = true;
                break;
            }
        }
        if (!slKey) {
            return;
        }

        // 如果存在塑炼计划，查询除塑炼胶之外的历史分解计划，计算对应塑料胶的计划量
        List<GlueDecomposePlan> historyList = decomposeEngineMapper.selectDecomposeList(mixArea, planDate);
        // 目前塑炼胶最下级，无需考虑塑炼胶还会分解的场景
        List<GlueDecomposePlan> slDecomposePlanList = getSLDecomposePlanList(planDate, mixArea, historyList, pmtRecipeVoMap, mixingMinProductMap);
        if(CollectionUtils.isEmpty(slDecomposePlanList)) {
            return;
        }
        
        // 删除历史的塑炼计划
        decomposeEngineMapper.updateDecomposePlanDelFlag(slDecomposePlanList);
        // 保存重新计算的塑炼计划
        decomposeEngineMapper.batchInsertGlueDecomposePlan(slDecomposePlanList);
    }

    /**
     * 合并历史的分解记录
     * 
     * @param gluePlanList 新增的分解记录
     * @return 合并后的分解记录
     */
    private List<GlueDecomposePlan> mergeDecomposePlanByHistory(List<GlueDecomposePlan> gluePlanList, Map<String, BigDecimal> mixingMinProductMap) {
        // 查询历史重复胶料的记录，目前只有母炼胶可能重复
        List<GlueDecomposePlan> historyListByPlanList = decomposeEngineMapper.selectHistoryListByPlanList(gluePlanList)
                .stream().filter(v -> StringUtils.isNotBlank(v.getUpGlue())).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(historyListByPlanList)) {
            return gluePlanList;
        }

        // 之前记录可能存在多种上级胶合并的场景，优先排在前面
        List<GlueDecomposePlan> mergeList = new ArrayList<>();
        mergeList.addAll(historyListByPlanList);
        mergeList.addAll(gluePlanList);

        // 合并重复的胶料记录
        return mergeDecomposePlanByGlue(mergeList, mixingMinProductMap, false);
    }

    /**
     * 修改了安全库存、生产量、机台后，当前记录以及它的子胶的计划量、生产量都需要重新计算
     * @param lastGlueDecompose  上级胶
     * @param isModifyProduceQty   是否直接修改 生产量
     * @return
     */
    public List<GlueDecomposePlan> recalculateDecomposePlan(GlueDecomposePlan lastGlueDecompose, boolean isModifyProduceQty) {
        List<GlueDecomposePlan> resultList = new ArrayList<>();
        Map<String, Double> glueWeightMap = recipeEngineService.mapGlueWeight(lastGlueDecompose.getPlanDate());  /*胶料单车总重Map*/
        Map<String, Double> minGlueWeightMap = recipeEngineService.mapMinGlueWeight(lastGlueDecompose.getPlanDate());  /*胶料单车总重Map*/
        //如果是在页面上直接修改生产量，则不需要通过公式在计算生产量
        if(!isModifyProduceQty) {
            if (StringUtils.isNotBlank(lastGlueDecompose.getUpGlue())) {
                // //如果不是终炼胶，则生产量需要重新计算（upGlue为空，则说明不是终炼胶）
                // GlueDecomposePlan fatherGlueDecomposePlan = decomposeEngineMapper.queryDecompose(lastGlueDecompose.getPlanDate(), lastGlueDecompose.getMixArea(), lastGlueDecompose.getUpGlue());
                // if(fatherGlueDecomposePlan != null) {
                //     this.countMaxPlanAndProduceQty(fatherGlueDecomposePlan, lastGlueDecompose, glueWeightMap, minGlueWeightMap);  //重新计算母胶的计划量 和 生产量
                // }
                // 母炼也只需要重算生产量，计划量是根据备库系数预估的，目前母炼单车重量取所有配方最小值来计算，不考虑机台，可以无需重新计算
                this.countProduceQty(lastGlueDecompose);
            } else {
                this.countProduceQty(lastGlueDecompose);  //如果是终炼胶，修改了非【生产量】的字段后，只需要重新计算
            }
        }

        // 计算调整前后的差值
        GlueDecomposePlan oldPlan = decomposeEngineMapper.queryDecompose(lastGlueDecompose.getPlanDate(), lastGlueDecompose.getMixArea(), lastGlueDecompose.getGlue());
        if (oldPlan != null && oldPlan.getProduceQty() != null && lastGlueDecompose.getProduceQty() != null) {
            lastGlueDecompose.setProduceQtyDiff(BigDecimalUtil.sub(oldPlan.getProduceQty(), lastGlueDecompose.getProduceQty()));
        }

//        lastGlueDecompose.setUpGlue(null);   //设置成null，表示用mybatis进行save的时候，不修改此字段的值
//        lastGlueDecompose.setUpMachineCode(null);   //设置成null，表示用mybatis进行save的时候，不修改此字段的值
//        lastGlueDecompose.setFinalGlueMachine(null);   //设置成null，表示用mybatis进行save的时候，不修改此字段的值
        resultList.add(lastGlueDecompose);

        //对胶料进行递归，把胶料下的全部母胶的计划量和生产量重新进行计算
        this.recursionDecomposePlan(resultList, lastGlueDecompose, glueWeightMap, minGlueWeightMap);
        return resultList;
    }

    /**
     * 对胶料进行递归，把胶料下的全部母胶的计划量和生产量重新进行计算
     *
     * @param resultList 胶料分解列表
     * @param fatherGlueDecompose 上级分解胶料
     * @param glueWeightMap 胶料+机台 - 单车重量
     * @param minGlueWeightMap 胶料 - 最小单车重量
     */
    public void recursionDecomposePlan(List<GlueDecomposePlan> resultList, GlueDecomposePlan fatherGlueDecompose, Map<String, Double> glueWeightMap, Map<String, Double> minGlueWeightMap) {
        GlueDecomposePlan sonGlueDecomposePlan = decomposeEngineMapper.querySonDecompose(fatherGlueDecompose.getPlanDate(), fatherGlueDecompose.getMixArea(), fatherGlueDecompose.getGlue(), fatherGlueDecompose.getFinalGlueMachine());
        if (sonGlueDecomposePlan == null) {
            return;
        }
        if (StringUtils.isNotBlank(sonGlueDecomposePlan.getUpGlue()) && sonGlueDecomposePlan.getUpGlue().contains(",")) {
            // 如果是多个上级胶，其他未修改的上级胶的生产量保持原计算，当前修改的上级胶的生产量重新计算
            String mixArea = sonGlueDecomposePlan.getMixArea();
            Date planDate = sonGlueDecomposePlan.getPlanDate();
            List<GlueDecomposePlan> allFatherList = Arrays.stream(sonGlueDecomposePlan.getUpGlue().split(","))
                    .filter(StringUtils::isNotBlank)
                    .filter(v -> !v.equals(fatherGlueDecompose.getGlue()))
                    .map(v -> {
                        GlueDecomposePlan item = new GlueDecomposePlan();
                        item.setPlanDate(planDate);
                        item.setMixArea(mixArea);
                        item.setGlue(v);
                        return item;
                    }).collect(Collectors.toList());
            allFatherList = decomposeEngineMapper.selectHistoryListByPlanList(allFatherList);
            allFatherList.add(fatherGlueDecompose);
            GlueDecomposePlan tempSonGlue = new GlueDecomposePlan();
            BeanUtils.copyProperties(sonGlueDecomposePlan, tempSonGlue);
            // // 汇总计划量
            // sonGlueDecomposePlan.setPlanQty(0D);
            // for (GlueDecomposePlan itemFather : allFatherList) {
            //     this.countMaxPlanQty(itemFather, tempSonGlue, glueWeightMap, minGlueWeightMap);  //重新计算母胶的计划量
            //     sonGlueDecomposePlan.setPlanQty(BigDecimalUtil.add(sonGlueDecomposePlan.getPlanQty(), tempSonGlue.getPlanQty()));
            // }
            // this.countProduceQty(sonGlueDecomposePlan);  //计算胶料的生产计划量
            // 更新机台编号
            String[] upGlueArray = sonGlueDecomposePlan.getUpGlue().split(",");
            if (StringUtils.isNotBlank(sonGlueDecomposePlan.getUpMachineCode())) {
                String[] upMachineCodeArray = sonGlueDecomposePlan.getUpMachineCode().split(",");
                for (int i = 0; i < upGlueArray.length; i++) {
                    if (upGlueArray[i].equals(fatherGlueDecompose.getGlue())) {
                        if (upMachineCodeArray.length >= (i + 1)) {
                            upMachineCodeArray[i] = fatherGlueDecompose.getMachineCode();
                            sonGlueDecomposePlan.setUpMachineCode(String.join(",", upMachineCodeArray));
                        }
                        break;
                    }
                }
            }
            
        } else {
            // 单个上级胶
            // this.countMaxPlanAndProduceQty(fatherGlueDecompose, sonGlueDecomposePlan, glueWeightMap, minGlueWeightMap);  //重新计算母胶的计划量 和 生产量
            sonGlueDecomposePlan.setUpMachineCode(fatherGlueDecompose.getMachineCode());  //上级胶料的机台code
        }

        // 子胶的计划量根据上级胶的生产量差值来计算计划量的差值：如果是负数差值，向上取整；如果是正数差值，向下取整
        Double produceQtyDiff = fatherGlueDecompose.getProduceQtyDiff();
        if (produceQtyDiff != null) {
            String highKey = fatherGlueDecompose.getGlue() + fatherGlueDecompose.getMachineCode();  //上级胶map的key：胶料代号+机台编号;
            //计算母炼胶日计划（车）= （终炼胶生产量 * 终炼胶单车总重）/ 母炼胶的单车总重
            String motherKey1 = sonGlueDecomposePlan.getGlue();  //母炼胶map的key：胶料代号
            Double motherPlanDiff = getMotherPlanQty(produceQtyDiff, glueWeightMap, minGlueWeightMap, highKey, motherKey1);
            if (motherPlanDiff != null) {
                // 如果是负数差值，向上取整；如果是正数差值，向下取整
                motherPlanDiff = motherPlanDiff < 0 ? BigDecimalUtil.roundUp(motherPlanDiff, 0) : BigDecimalUtil.roundDown(motherPlanDiff, 0);
                sonGlueDecomposePlan.setPlanQty(BigDecimalUtil.greatest(0D, BigDecimalUtil.sub(sonGlueDecomposePlan.getPlanQty(), motherPlanDiff)));
            }
        }

        // 记录生产量的差值，重新计算生产量
        Double beforeProduceQty = sonGlueDecomposePlan.getProduceQty();
        this.countProduceQty(sonGlueDecomposePlan);
        sonGlueDecomposePlan.setProduceQtyDiff(BigDecimalUtil.sub(beforeProduceQty, sonGlueDecomposePlan.getProduceQty()));
        
        sonGlueDecomposePlan.setUpdateBy(fatherGlueDecompose.getUpdateBy());
        sonGlueDecomposePlan.setUpdateTime(fatherGlueDecompose.getUpdateTime());
        resultList.add(sonGlueDecomposePlan);
        this.recursionDecomposePlan(resultList, sonGlueDecomposePlan, glueWeightMap, minGlueWeightMap);  //继续递归母胶
    }

    /**
     * 根据终炼胶和其他基础信息，创建全部母炼胶的对象信息
     *
     * @param finalGlue                    终炼胶对象
     * @param motherGlueAreaList           终炼胶对应的母炼胶列表
     * @param safeStockMap                 安全库存map
     * @param motherStockMap               母炼胶库存Map
     * @param lastGlueDayPlanMap           获取胶料前一天排程白班8-16点的计划量
     * @param lastGlueDayPlanAndMachineMap 取胶料前一天排程白班8-16点的计划量和机台map
     * @param glueWeightMap                终炼母炼胶单车总重Map
     * @param minGlueWeightMap             终炼母炼胶最小单车总重Map
     * @return
     */
    private List<GlueDecomposePlan> createMotherGlueDecomposePlan(GlueDecomposePlan finalGlue, List<GlueAreaMachineVo> motherGlueAreaList, Map<String, GlueSafeStock> safeStockMap,
                                                                  Map<String, Double> motherStockMap, Map<String, Double> lastGlueDayPlanMap, Map<String, List<GlueConsumeVo>> lastGlueDayPlanAndMachineMap,
                                                                  Map<String, Double> glueWeightMap, Map<String, Double> minGlueWeightMap, 
                                                                  Map<String, BigDecimal> mixingMinProductMap,
                                                                  String oriDataSource) {
        LinkedList<GlueDecomposePlan> motherGlueDecomposeList = new LinkedList<>();  //根据终炼胶分解出的母炼胶最终列表
        if(motherGlueAreaList == null || motherGlueAreaList.isEmpty()) {
            return motherGlueDecomposeList;
        }
        GlueDecomposePlan highGlue = finalGlue;  //上级胶对象

        for(int i = motherGlueAreaList.size()-1; i>=0;i--) {
            GlueAreaMachineVo motherGlueArea = motherGlueAreaList.get(i);
            String highKey = highGlue.getGlue() + highGlue.getMachineCode();  //上级胶map的key：胶料代号+机台编号
            String motherKey = motherGlueArea.getMixArea() + motherGlueArea.getGlue();  //母炼胶map的key：密炼区+胶料代号
            GlueSafeStock safeStock = safeStockMap.getOrDefault(motherKey, new GlueSafeStock());
            Double motherSafeStock = Optional.ofNullable(safeStock.getSafeStock()).orElse(BigDecimal.ZERO).doubleValue();  //母炼胶安全库存
            Double motherStock = motherStockMap.getOrDefault(motherKey, 0D);  //母炼胶当天库存

            GlueDecomposePlan motherGlue = new GlueDecomposePlan();
            motherGlue.setBatchNo(highGlue.getBatchNo());  //设置批次号
            motherGlue.setCollectBatchNo(highGlue.getCollectBatchNo());  //设置对应汇总胶料需求计划的批次号
            motherGlue.setPlanDate(finalGlue.getPlanDate());   //计划日期
            motherGlue.setDataSource(ZltConstant.DECOMPOSE_SOURCE_AUTO);  //设置数据来源：分解计划
            motherGlue.setMixArea(motherGlueArea.getMixArea());  //设置密炼区
            motherGlue.setGlue(motherGlueArea.getGlue());   //设置母炼胶代号
            motherGlue.setSafeStockQty(motherSafeStock);  //设置母炼胶安全库存
            motherGlue.setMachineCode(motherGlueArea.getMachineCode());  //设置母炼胶对应机台
            motherGlue.setFinalGlueMachine(finalGlue.getFinalGlueMachine());  //设置此母炼胶对应的终炼胶
            motherGlue.setUpGlue(highGlue.getGlue());  //上级胶名称
            motherGlue.setUpMachineCode(highGlue.getMachineCode());  //上级胶机台
            motherGlue.setIsFinishing(highGlue.getIsFinishing());   //设置收尾标识，和上级胶一致
            motherGlue.setBaseValue(motherGlue.getId());
            motherGlue.setDayFlag(finalGlue.getDayFlag());
//            motherGlue.setStockQty(this.countStockCar(motherGlue, motherStockMap, glueWeightMap));   //根据库存重量，计算出母炼胶库存车数
            motherStock = this.expectStockNew(motherStock, highGlue, motherGlue, minGlueWeightMap, lastGlueDayPlanMap, lastGlueDayPlanAndMachineMap);  //计算16点预计库存
            motherGlue.setStockQty(motherStock);   //设置母炼胶库存
            this.countMaxPlanAndProduceQty(highGlue, motherGlue, glueWeightMap, minGlueWeightMap);  //计算计划量和生产量
            // 母炼胶胶如果非收尾 且 不是手动新增的分解，有生产量，白班计划+生产量不足排产起步量的限制，需要调整到起步量
            BigDecimal minProduct = mixingMinProductMap.get(motherGlue.getGlue());
            this.computeStartProduceQty(motherGlue, minProduct, oriDataSource, lastGlueDayPlanMap.getOrDefault(motherKey, 0D));

            motherGlueDecomposeList.addLast(motherGlue);  //把数据加到list末尾
            highGlue = motherGlue;  //重新设置上级胶对象
        }
        return motherGlueDecomposeList;
    }

    /**
     * 计算母炼胶预计库存 = 母炼胶12点实际库存 - （上级胶8-16点计划量 - 上级胶8-12点计划完成量）* （终炼胶配方重量 / 母炼胶配方重量）
     * @param stockNum 母炼胶12点实际库存
     * @param highGlue 上级胶料信息
     * @param motherGlue  母炼胶信息
     * @param glueWeightMap  胶料的单车总重信息
     * @param glueLast8And16PlanMap 获取胶料前一天排程8-16点的计划量
     * @param glue8And112FinsihMap 取胶料当天8-12点的完成量
     * @return
     */
    private Double expectStock(Double stockNum, GlueDecomposePlan highGlue, GlueDecomposePlan motherGlue, Map<String, Double> glueWeightMap,
                               Map<String, Double> glueLast8And16PlanMap,Map<String, Double> glue8And112FinsihMap) {
        if(stockNum.equals(0D)) {
            return stockNum;
        }
        String highKey = highGlue.getGlue() + highGlue.getMachineCode();  //上级胶map的key：胶料代号+机台编号;
        String highKey1 = highGlue.getMixArea() + highGlue.getGlue();   //上级胶key：密炼区+胶料名称
        String motherKey = motherGlue.getGlue() + motherGlue.getMachineCode();  //母炼胶map的key：胶料代号+机台编号

        Double highWight = glueWeightMap.getOrDefault(highKey, 0D);   //上级胶单车总重
        Double motherWeight = glueWeightMap.getOrDefault(motherKey, 0D);  //母炼胶单车总重
        Double glueLast8And16Plan = glueLast8And16PlanMap.getOrDefault(highKey1, 0D);  //上级胶8-16点计划量
        Double glue8And112Finsih = glue8And112FinsihMap.getOrDefault(highKey1, 0D);  //上级胶8-12点完成量
        if(motherWeight == 0D || highWight == 0D) {
            return stockNum;
        }
        Double hightConsume = BigDecimalUtil.sub(glueLast8And16Plan, glue8And112Finsih);  //上级胶计划12-16点消耗量
        Double motherConsume = BigDecimalUtil.div(BigDecimalUtil.mul(hightConsume, highWight), motherWeight);  //母炼胶计划12-16点消耗量
        motherConsume = BigDecimalUtil.roundUp(motherConsume, 0);
        motherConsume = (motherConsume <= 0 ? 0D : motherConsume);  //计划消耗量小于0的情况，则直接赋值0
        stockNum = BigDecimalUtil.sub(stockNum, motherConsume);
        stockNum = (stockNum <= 0 ? 0D : stockNum);
        return stockNum;
    }

    /**
     * 计算母炼胶预计库存 = 8点钟库存+ APS昨日白班母胶计划量 - 预计昨日白班计划量的上级胶消耗量 * （上级胶配方重量 / 母炼胶配方重量）
     * （结果小于0以0计算）
     * @param stockNum 母炼胶8点实际库存
     * @param highGlue 上级胶料信息
     * @param motherGlue  母炼胶信息
     * @param glueWeightMap  胶料最小单车总重信息
     * @param lastGlueDayPlanMap 胶料前一天排程8-16点白班的计划量Map
     * @param lastGlueDayPlanAndMachineMap 胶料前一天排程8-16点白班的计划量和机台信息Map
     * @return
     */
    private Double expectStockNew(Double stockNum, GlueDecomposePlan highGlue, GlueDecomposePlan motherGlue, Map<String, Double> glueWeightMap,
                                  Map<String, Double> lastGlueDayPlanMap, Map<String, List<GlueConsumeVo>> lastGlueDayPlanAndMachineMap) {
//        if(stockNum.equals(0D)) {
//            return stockNum;
//        }
        String hightGlue = highGlue.getGlue();  //上级胶名称
        String motherKey = motherGlue.getGlue();  //母炼胶map的key：胶料代号
        String motherKey1 = motherGlue.getMixArea() + motherGlue.getGlue();  //母炼胶map的key：密炼区+胶料名称

        Double motherWeight = glueWeightMap.getOrDefault(motherKey, 0D);  //母炼胶单车总重
        Double motherGlueDayPlan = lastGlueDayPlanMap.getOrDefault(motherKey1, 0D);  //母胶8-16点计划量
        List<GlueConsumeVo> highGlueDayPlanAndMachineList = lastGlueDayPlanAndMachineMap.get(hightGlue);  //上级胶8-16点计划量和机台信息
        // 记录原始库存和昨日计划，合并胶料场景可能需要重算库存
        motherGlue.setOriginStockQty(stockNum);
        motherGlue.setLastDayPlan(motherGlueDayPlan);
        if(motherWeight == 0D) {
            return BigDecimalUtil.add(stockNum, motherGlueDayPlan);
        }

        //计算预计昨日白班胶料消耗量 = 预计昨日白班计划量的上级胶消耗量 * （上级胶配方重量 / 母炼胶配方重量）
        Double hightConsume = 0D;
        if(highGlueDayPlanAndMachineList != null) {
            for(GlueConsumeVo dayPlanAndMachine : highGlueDayPlanAndMachineList) {
                Double highWight = dayPlanAndMachine.getFormulaWeight();
                Double highDayPlan = dayPlanAndMachine.getDayPlanQty();
                Double hightConsumeTemp = BigDecimalUtil.mul(highDayPlan, highWight);
                hightConsumeTemp = BigDecimalUtil.div(hightConsumeTemp, motherWeight);
                hightConsume = hightConsume + hightConsumeTemp;
            }
            hightConsume = BigDecimalUtil.roundUp(hightConsume, 0);
        }
        // 记录母胶昨日白班的消耗量，合并胶料场景可能需要重算库存
        motherGlue.setLastDayConsume(hightConsume);

        //计算预计库存 = 8点钟库存+ APS昨日白班母胶计划量 - 预计昨日白班胶料消耗量
        stockNum = BigDecimalUtil.add(stockNum, motherGlueDayPlan);
        stockNum = BigDecimalUtil.sub(stockNum, hightConsume);
        stockNum = (stockNum <= 0 ? 0D : stockNum);
        return stockNum;
    }

    /**
     * 计算计划量
     *
     * @param highGlue      上级胶信息
     * @param motherGlue    母胶信息
     * @param glueWeightMap 胶料的单车总重信息
     * @return 配方是否完整
     */
    private boolean countPlanQty(GlueDecomposePlan highGlue, GlueDecomposePlan motherGlue, Map<String, Double> glueWeightMap) {
        String highKey = highGlue.getGlue() + highGlue.getMachineCode();  //上级胶map的key：胶料代号+机台编号;
        //计算母炼胶日计划（车）= （终炼胶生产量 * 终炼胶单车总重）/ 母炼胶的单车总重, 在计算母炼胶生产量 = 日计划 - 库存 + 安全库存
        String motherKey1 = motherGlue.getGlue() + motherGlue.getMachineCode();  //母炼胶map的key：胶料代号+机台编号
        return countGluePlanQty(highGlue, motherGlue, glueWeightMap, glueWeightMap, highKey, motherKey1);
    }

    /**
     * 下级胶按照最小配方重量计算计划量
     *
     * @param highGlue         上级胶信息
     * @param motherGlue       母胶信息
     * @param glueWeightMap    胶料的单车总重信息
     * @param minGlueWeightMap 胶料的最小单车总重信息
     * @return 配方是否完整
     */
    private boolean countMaxPlanQty(GlueDecomposePlan highGlue, GlueDecomposePlan motherGlue, Map<String, Double> glueWeightMap, Map<String, Double> minGlueWeightMap) {
        String highKey = highGlue.getGlue() + highGlue.getMachineCode();  //上级胶map的key：胶料代号+机台编号;
        //计算母炼胶日计划（车）= （终炼胶生产量 * 终炼胶单车总重）/ 母炼胶的单车总重
        String motherKey1 = motherGlue.getGlue();  //母炼胶map的key：胶料代号
        return countGluePlanQty(highGlue, motherGlue, glueWeightMap, minGlueWeightMap, highKey, motherKey1);
    }

    /**
     * 计算母炼胶的计划量
     *
     * @param highGlue         上级胶
     * @param motherGlue       母胶
     * @param glueWeightMap    上级胶的单车重量Map
     * @param minGlueWeightMap 母炼胶的单车重量Map
     * @param highKey          上级胶的配方key
     * @param motherKey1       母炼胶的配方key
     * @return 配方是否正确
     */
    private boolean countGluePlanQty(GlueDecomposePlan highGlue, GlueDecomposePlan motherGlue, Map<String, Double> glueWeightMap, Map<String, Double> minGlueWeightMap, String highKey, String motherKey1) {
        Double motherPlanQty = getMotherPlanQty(highGlue.getProduceQty(), glueWeightMap, minGlueWeightMap, highKey, motherKey1);
        if (motherPlanQty == null) {
            motherGlue.setPlanQty(0D);
            return false;
        }
        motherPlanQty = BigDecimalUtil.roundUp(motherPlanQty, 0);  //最终计算出母炼胶日计划量(向上取整)
        motherGlue.setPlanQty(motherPlanQty);

        return true;
    }

    /**
     * 获取未取整的母炼胶的计划量
     */
    private Double getMotherPlanQty(Double highProduceQty, Map<String, Double> glueWeightMap, Map<String, Double> minGlueWeightMap, String highKey, String motherKey1) {
        Double highWight = glueWeightMap.getOrDefault(highKey, 0D);   //上级胶单车总重
        Double motherWeight = minGlueWeightMap.getOrDefault(motherKey1, 0D);  //母炼胶单车总重
        if (highProduceQty == null || motherWeight == 0D || highWight == 0D) {
            log.error("母炼胶单车总重为空");
            return null;
        }

        return BigDecimalUtil.div(BigDecimalUtil.mul(highProduceQty, highWight), motherWeight);
    }

    /**
     * 计算计划量 和 生产量
     * @param highGlue  上级胶信息
     * @param motherGlue   母胶信息
     * @param glueWeightMap  胶料的单车总重信息
     */
    private void countPlanAndProduceQty(GlueDecomposePlan highGlue, GlueDecomposePlan motherGlue, Map<String, Double> glueWeightMap) {
        // 计算计划量
        if (countPlanQty(highGlue, motherGlue, glueWeightMap)) {
            //计算胶料的生产计划量
            this.countProduceQty(motherGlue);
        } else {
            motherGlue.setProduceQty(0D);
        }
    }

    /**
     * 母炼胶按照最小配方重量 计算计划量 和 生产量
     *
     * @param highGlue         上级胶信息
     * @param motherGlue       母胶信息
     * @param glueWeightMap    胶料的单车总重信息
     * @param minGlueWeightMap 胶料的最小单车总重信息
     */
    private void countMaxPlanAndProduceQty(GlueDecomposePlan highGlue, GlueDecomposePlan motherGlue, Map<String, Double> glueWeightMap, Map<String, Double> minGlueWeightMap) {
        // 计算计划量
        if (countMaxPlanQty(highGlue, motherGlue, glueWeightMap, minGlueWeightMap)) {
            //计算胶料的生产计划量
            this.countProduceQty(motherGlue);
        } else {
            motherGlue.setProduceQty(0D);
        }
    }

    /**
     * 计算胶料的生产计划量, 生产计划 = 日计划 * (1 + 预生产库存倍数【如果是非收尾的胶料】) - 库存 + 安全库存(如果是还没收尾的胶料，则生产量要在加上安全库存)
     * @param glue
     */
    private void countProduceQty(GlueDecomposePlan glue) {
        Double planQty = glue.getPlanQty() == null ? 0D : glue.getPlanQty();
        Double stock = glue.getStockQty() == null ? 0D : glue.getStockQty();
        Double safeStock = glue.getSafeStockQty() == null ? 0D : glue.getSafeStockQty();
        Double reserveStockRate = glue.getReserveStockRate() == null ? 0D : glue.getReserveStockRate();
        //生产计划 = 日计划 - 库存 + 安全库存
        Double produceQty = BigDecimalUtil.sub(planQty, stock);
        if(!EngineConstants.IS_FINISHING_YES.equals(glue.getIsFinishing())) {
            //如果是不要收尾的胶料，则生产量要在加上安全库存、预生产库存倍数 * 日计划
            produceQty = BigDecimalUtil.add(BigDecimalUtil.add(produceQty, safeStock), 
                    BigDecimalUtil.roundUp(BigDecimalUtil.mul(planQty, reserveStockRate), 0));
        }
        produceQty = (produceQty < 0 ? 0D : produceQty);
        produceQty = BigDecimalUtil.roundUp(produceQty, 0);  //向上取整
        // 尽量凑够5的倍数
        double produceQtyRoundUp = GlueEngineConstants.PRODUCE_QTY_ROUND_UP;
        double remainder = produceQty % produceQtyRoundUp;
        if (remainder > 0) {
            produceQty = BigDecimalUtil.add(produceQty, BigDecimalUtil.sub(produceQtyRoundUp, remainder));
        }
        glue.setProduceQty(produceQty);
    }


    /**
     * 同步分解胶料计划到日志表中
     * @param planDate 计划日期
     * @param mixArea 密炼区
     */
    private void synclueDecomposePlanToLog(Date planDate, String mixArea) {
        decomposeEngineMapper.synclueDecomposePlanToLog(planDate, mixArea);
        decomposeEngineMapper.deleteGlueDecomposePlan(planDate, mixArea);  //删除胶料分解计划
    }
}
