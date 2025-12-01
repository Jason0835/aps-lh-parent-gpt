package com.zlt.mix.schedule.engine.service.basicdata.impl;

import com.ruoyi.common.core.utils.DateUtils;
import com.zlt.mix.common.core.utils.BigDecimalUtil;
import com.zlt.mix.common.core.utils.GenerageMapKeyUtils;
import com.zlt.mix.schedule.engine.mapper.StockEngineMapper;
import com.zlt.mix.schedule.engine.service.basicdata.StockEngineService;
import com.zlt.mix.schedule.engine.vo.GlueAreaMachineVo;
import com.zlt.mix.schedule.engine.vo.GlueConsumeVo;
import com.zlt.mix.schedule.engine.vo.MaterialAreaMachineVo;
import com.zlt.mix.setting.api.domain.entity.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 引擎部分库存相关ServiceImpl
 */
@Service
@Slf4j
public class StockEngineServiceImpl implements StockEngineService {

    @Resource
    private StockEngineMapper stockEngineMapper;

    /**
     * 获取胶料胶安全库存map
     * @param areaGlueList 要查询的胶料列表
     * @return map，key--密炼区+胶料号， value--安全库存值
     */
    public Map<String, GlueSafeStock> mapGlueSafeStock(List<GlueAreaMachineVo> areaGlueList) {

        Map<String, GlueSafeStock> map = new HashMap<>();
        List<GlueSafeStock> glueSafeStockList = stockEngineMapper.listGlueSafeStock(areaGlueList);
        if(!glueSafeStockList.isEmpty()) {
            for(GlueSafeStock glueSafeStock : glueSafeStockList) {
                map.put(glueSafeStock.getMixArea() + glueSafeStock.getGlue(), glueSafeStock);
            }
        }

        return map;
    }

    /**
     * 获取胶料胶安全库存map
     * @param areaGlue 要查询的胶料
     * @return map，key--密炼区+胶料号， value--安全库存值
     */
    public Map<String, GlueSafeStock> mapGlueSafeStock(GlueAreaMachineVo areaGlue) {
        return mapGlueSafeStock(Arrays.asList(areaGlue));
    }

    /**
     * 获取终炼胶预计库存map：预计库存 = 8点钟库存+ APS昨日白班终胶计划量 - 昨日白班待支领量；（结果小于0以0计算）
     *
     * @param planDate             计划日期
     * @param mixArea              密炼区
     * @param lastGlueDayPlanMap   昨日胶料排程白班计划量Map
     * @param lastGlueUnclaimedMap 昨日白班支领量Map
     * @param reserveGlueRecipeMap 胶料配方映射的反转白班计划量的Map
     * @return map，key--密炼区+胶料号， value--库存值
     */
    public Map<String, Double> mapFinalGlueStock(Date planDate, String mixArea, Map<String, Double> lastGlueDayPlanMap, Map<String, Double> lastGlueUnclaimedMap,  Map<String, String> reserveGlueRecipeMap) {
        Map<String, Double> expectStockMap = new HashMap<>();  //预计库存map
        planDate = DateUtils.addDays(planDate, -1);  //获取前一天日期

        //查询当前库存
        // Map<String, Double> currentStockMap = new HashMap<>();
        List<GlueStock> finalGlueStockList = stockEngineMapper.listFinalGlueStockByMixArea(planDate, mixArea);
        if(!finalGlueStockList.isEmpty()) {
            for(GlueStock glueStock : finalGlueStockList) {
                // 如果是不区分掺胶和纯胶库存，将纯胶库存转为掺胶库存
                String glue = glueStock.getGlue();
                if (reserveGlueRecipeMap.containsKey(glue)) {
                    glue = reserveGlueRecipeMap.get(glue);
                }
                glueStock.setGlue(glue);
                // currentStockMap.put(glueStock.getMixArea() + glue, glueStock.getStockNum().doubleValue());
            }
        }
        
        // 按密炼区+胶料分组：计算预计库存,8点钟库存+ APS昨日白班终胶计划量 - 昨日白班待支领量；（结果小于0以0计算）
        finalGlueStockList.stream().collect(Collectors.groupingBy(v -> GenerageMapKeyUtils.createMapKey(v.getMixArea(), v.getGlue()))).forEach((mixAreaGlueKey, itemList) -> {
            GlueStock currentStockVo = itemList.get(0);

            String key = currentStockVo.getMixArea() + currentStockVo.getGlue();
            // 当前库存
            double currentStock = itemList.stream().map(GlueStock::getStockNum).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add).doubleValue();
            Double lastGlueDayPlan = lastGlueDayPlanMap.getOrDefault(key, 0D);  //昨日终炼胶排程白班计划量
            Double lastGlueUnclaimed = lastGlueUnclaimedMap.getOrDefault(key, 0D);  //获取昨日的日用量
            double expectStock = BigDecimalUtil.add(currentStock, lastGlueDayPlan);
            expectStock = BigDecimalUtil.sub(expectStock, lastGlueUnclaimed);
            // 负数库存表示对应日用量，白班已完成的部分不够使用，还需要夜班补充
            // expectStock = (expectStock < 0D) ? 0D : expectStock;
            expectStockMap.put(key, expectStock);
            
        });

        // 补充如果库存未导入 且 有支领量的记录
        Set<String> finalGlueStockSet = finalGlueStockList.stream().map(v -> v.getMixArea() + v.getGlue()).collect(Collectors.toSet());
        lastGlueUnclaimedMap.forEach((key, lastGlueUnclaimed) -> {
            if (finalGlueStockSet.contains(key)) {
                return;
            }

            // 当前库存
            double currentStock = 0D;
            Double lastGlueDayPlan = lastGlueDayPlanMap.getOrDefault(key, 0D);  //昨日终炼胶排程白班计划量
            double expectStock = BigDecimalUtil.add(currentStock, lastGlueDayPlan);
            expectStock = BigDecimalUtil.sub(expectStock, lastGlueUnclaimed);
            // 负数库存表示对应日用量，白班已完成的部分不够使用，还需要夜班补充
            // expectStock = (expectStock < 0D) ? 0D : expectStock;
            expectStockMap.put(key, expectStock);
        });

        return expectStockMap;
    }

    /**
     * 获取昨日胶料排程白班计划量
     *
     * @param mixArea              密炼区
     * @param planDate             计划日期
     * @param glueRecipeMap        胶料配方映射的胶料名称Map
     * @param reserveGlueRecipeMap 胶料配方映射的反转白班计划量Map
     * @return
     */
    public Map<String, Double> mapLastGlueDayPlan(String mixArea, Date planDate, Map<String, String> glueRecipeMap, Map<String, String> reserveGlueRecipeMap) {
        planDate = DateUtils.addDays(planDate, -1);  //获取前一天日期
        Map<String, Double> lastGlueDayPlanMap = new HashMap<>();
        List<GlueConsumeVo> lastGlueDayPlanList = stockEngineMapper.listLastGlueDayPlan(mixArea, planDate);
        if (lastGlueDayPlanList != null && !lastGlueDayPlanList.isEmpty()) {
            for (GlueConsumeVo itemConsume : lastGlueDayPlanList) {
                String glue = itemConsume.getGlue();
                // 如果是WA的ZZ配方 且 区分掺胶和纯胶的日用量 ，将掺胶转为纯胶
                String glueRecipeMapKey = itemConsume.getGlueRecipeMapKey();
                if (glueRecipeMap.containsKey(glueRecipeMapKey)) {
                    glue = glueRecipeMap.get(glueRecipeMapKey);
                }
                
                // 如果是不区分纯胶和掺胶日用量，将纯胶转为掺胶
                if(reserveGlueRecipeMap.containsKey(glue)) {
                    glue = reserveGlueRecipeMap.get(glue);
                }

                // 合计统计昨日早班计划
                Double dayPlanQty = lastGlueDayPlanMap.getOrDefault(itemConsume.getMixArea() + glue, 0D);
                lastGlueDayPlanMap.put(itemConsume.getMixArea() + glue, BigDecimalUtil.add(dayPlanQty, itemConsume.getDayPlanQty()));
            }
        }
        return lastGlueDayPlanMap;
    }

    /**
     * 获取昨日胶料排程白班计划量和机台信息
     * @param mixArea 密炼区
     * @param planDate 计划日期
     * @return
     */
    public Map<String, List<GlueConsumeVo>> mapLastGlueDayPlanAndMachine(String mixArea, Date planDate) {
        planDate = DateUtils.addDays(planDate, -1);  //获取前一天日期
        Map<String, List<GlueConsumeVo>> lastGlueDayPlanAndMachineMap = new HashMap<>();
        List<GlueConsumeVo> lastGlueDayPlanList = stockEngineMapper.listLastGlueDayPlanAndMachine(mixArea, planDate);
        if(lastGlueDayPlanList != null && !lastGlueDayPlanList.isEmpty()) {
            lastGlueDayPlanAndMachineMap = lastGlueDayPlanList.stream().collect(Collectors.groupingBy(GlueConsumeVo::getGlue));
        }
        return lastGlueDayPlanAndMachineMap;
    }

    /**
     * 获取昨日胶料排程白班计划量。按照密炼区+胶料+机台汇总
     *
     * @param mixArea  密炼区
     * @param planDate 计划日期
     * @return 炼区+胶料+机台汇总的白班计划量
     */
    public Map<String, Double> mapLastGlueMachineDayPlan(String mixArea, Date planDate) {
        planDate = DateUtils.addDays(planDate, -1);  //获取前一天日期
        Map<String, Double> lastGlueDayPlanMap = new HashMap<>();
        List<GlueConsumeVo> lastGlueDayPlanList = stockEngineMapper.listLastGlueDayPlanAndMachine(mixArea, planDate);
        if (lastGlueDayPlanList != null && !lastGlueDayPlanList.isEmpty()) {
            lastGlueDayPlanMap = lastGlueDayPlanList.stream()
                    .collect(Collectors.toMap(k -> GenerageMapKeyUtils.createMapKey(k.getGlue(), k.getMachineCode()), GlueConsumeVo::getDayPlanQty));
        }
        return lastGlueDayPlanMap;
    }

    /**
     * 获取昨日日用量（根据汇总计划计算）
     * @param mixArea 密炼区
     * @param planDate 计划日期
     * @param glueUnclaimedImport 设置为1时使用导入的白班待支领量计算预计库存数，设置为其他数值时使用mes的待支领量计算
     * @return
     */
    public Map<String, Double> mapLastGlueUnclaimed(String mixArea, Date planDate, String glueUnclaimedImport) {
        planDate = DateUtils.addDays(planDate, -1);  //获取前一天日期
        Map<String, Double> lastGlueUnclaimedMap = new HashMap<>();
        List<GlueConsumeVo> lastGlueUnclaimedList = stockEngineMapper.listLastGlueUnclaimed(mixArea, planDate, glueUnclaimedImport);
        if(lastGlueUnclaimedList != null && !lastGlueUnclaimedList.isEmpty()) {
            lastGlueUnclaimedMap = lastGlueUnclaimedList.stream().collect(Collectors.toMap(k->k.getMixArea() + k.getGlue(), GlueConsumeVo::getShelfNum));
        }
        return lastGlueUnclaimedMap;
    }

//    /**
//     * 获取终炼胶库存map
//     * @param planDate 计划日期
//     * @param glueUnclaimedImport 设置为1时使用导入的白班待支领量计算预计库存数，设置为其他数值时使用mes的待支领量计算
//     * @param glueAreaMachine 终炼胶+密炼区+机台对象
//     * @return map，key--密炼区+胶料号， value--库存值
//     */
//    public Map<String, Double> mapFinalGlueStock(Date planDate, String glueUnclaimedImport, GlueAreaMachineVo glueAreaMachine) {
//        return mapFinalGlueStock(planDate, glueUnclaimedImport, Arrays.asList(glueAreaMachine));
//    }

    /**
     * 获取母炼胶库存map
     * @param planDate 计划日期
     * @param glueAreaMachineList 终炼胶+密炼区+机台列表
     * @return map，key--密炼区+胶料号， value--库存值
     */
    public Map<String, Double> mapMotherGlueStock(Date planDate, List<GlueAreaMachineVo> glueAreaMachineList) {
        Map<String, Double> map = new HashMap<>();
        if(glueAreaMachineList.isEmpty()) {
            return map;
        }
        List<MlGlueStock> motherGlueStockList = stockEngineMapper.listMotherGlueStock(planDate, glueAreaMachineList);
        if(!motherGlueStockList.isEmpty()) {
            for(MlGlueStock motherGlueStock : motherGlueStockList) {
                map.put(motherGlueStock.getMixArea() + motherGlueStock.getGlue(), motherGlueStock.getStockNum().doubleValue());
            }
        }
        return map;
    }

    /**
     * 获取母炼胶库存map
     * @param planDate 计划日期
     * @param glueAreaMachine 终炼胶+密炼区+机台对象
     * @return map，key--密炼区+胶料号， value--库存值
     */
    public Map<String, Double> mapMotherGlueStock(Date planDate, GlueAreaMachineVo glueAreaMachine) {
        return mapMotherGlueStock(planDate, Arrays.asList(glueAreaMachine));
    }

    /**
     * 获取胶料前一天排程8-16点的计划量
     * @param mixArea  密炼区
     * @param planDate  计划日期
     * @return
     */
    public Map<String, Double> mapGlueLast8And16Plan(String mixArea, Date planDate) {
        Map<String, Double> map = new HashMap<>();
        List<GlueConsumeVo> list = stockEngineMapper.listGlueLast8And16Plan(mixArea, planDate);
        if(list != null && !list.isEmpty()) {
            map = list.stream().collect(Collectors.toMap(k->k.getMixArea()+k.getGlue(), GlueConsumeVo::getDayPlanQty));
        }
        return map;
    }

    /**
     * 获取胶料当天8-12点的完成量
     * @param mixArea  密炼区
     * @param planDate  计划日期
     * @return
     */
    public Map<String, Double> mapGlue8And112Finsih(String mixArea, Date planDate) {
        Map<String, Double> map = new HashMap<>();
        List<GlueConsumeVo> list = stockEngineMapper.listGlue8And112Finsih(mixArea, planDate);
        if(list != null && !list.isEmpty()) {
            map = list.stream().collect(Collectors.toMap(k->k.getMixArea()+k.getGlue(), GlueConsumeVo::getFinishQty));
        }
        return map;
    }

    /**
     * 获取硫磺辅料16点预计库存库存map(16点预计库存 = 12点实时库存 - 12到16点硫磺辅料预计消耗量)
     * @param scheduleDate 排程日期
     * @param areaMaterialList 终炼胶+密炼区+机台列表
     * @return map，key--密炼区+胶料号， value--库存值
     */
    public Map<String, Double> mapMaterialStock(Date scheduleDate, List<MaterialAreaMachineVo> areaMaterialList) {
        Map<String, Double> map = new HashMap<>();
        //计算12点到16点硫磺辅料计划消耗量 = 8点到16点胶料计划 - 8点到12点胶料生产量
        Map<String ,Double> countMaterialConsume = this.countMaterialConsume(scheduleDate, areaMaterialList);

        //获取硫磺辅料库存列表
        List<LhflGlueStock> materialStockList = stockEngineMapper.listMaterialStock(scheduleDate, areaMaterialList);
        if(!materialStockList.isEmpty()) {
            for(LhflGlueStock materialStock : materialStockList) {
                String key = materialStock.getMixArea() + materialStock.getMaterialName();
                Double planStock = BigDecimalUtil.sub(materialStock.getStockNum().doubleValue(), countMaterialConsume.getOrDefault(key, 0D));
                map.put(key, planStock);
            }
        }
        return map;
    }

    /**
     * 计算12点到16点硫磺辅料计划消耗量 = 8点到16点胶料计划 - 8点到12点胶料生产量
     * @param scheduleDate
     * @param areaMaterialList
     * @return
     */
    private Map<String ,Double> countMaterialConsume(Date scheduleDate, List<MaterialAreaMachineVo> areaMaterialList) {
        Map<String, Double> result = new HashMap<>();
        //计算出胶料8-16点预计要消耗的硫磺辅料车数
        List<GlueConsumeVo> glueLastDayPlanList = stockEngineMapper.listGlueLastDayPlan(scheduleDate, areaMaterialList);
        //计算出胶料8-12点实际要消耗硫磺辅料车数
        List<GlueConsumeVo> glueLastDayFinishList = stockEngineMapper.listGlueLastDayFinish(scheduleDate, areaMaterialList);
        Map<String ,Double> glueLastDayFinishMap = glueLastDayFinishList.stream().collect(Collectors.toMap(k->k.getMixArea()+k.getMaterialName(), GlueConsumeVo::getFinishQty));

        if (glueLastDayPlanList.isEmpty()) {
            return result;
        }
        for(GlueConsumeVo glueConsumeVo : glueLastDayPlanList) {
            String key = glueConsumeVo.getMixArea() + glueConsumeVo.getMixArea();
            Double consume = BigDecimalUtil.sub(glueConsumeVo.getDayPlanQty(), glueLastDayFinishMap.getOrDefault(key, 0D)); //计算12点到16点硫磺辅料计划消耗量
            consume = (consume <= 0 ? 0D : consume);  //小于0的话，直接赋值0
            result.put(key, consume);
        }
        return result;
    }

    /**
     * 获取硫磺辅料安全库存
     * @param mixArea
     * @return
     */
    public Map<String, Double> mapMaterialSafeStock(String mixArea) {
    	return stockEngineMapper.listMaterialSafeStock(mixArea).stream()
    			.collect(Collectors.toMap(LhflSafeStock::getMaterial, v -> v.getSafeStock().doubleValue(), (v1, v2) -> v2));
    }
}
