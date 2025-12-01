package com.zlt.mix.schedule.engine.service.basicdata;

import com.zlt.mix.schedule.engine.vo.GlueAreaMachineVo;
import com.zlt.mix.schedule.engine.vo.GlueConsumeVo;
import com.zlt.mix.schedule.engine.vo.MaterialAreaMachineVo;
import com.zlt.mix.setting.api.domain.entity.GlueSafeStock;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 引擎部分库存相关Service
 */
public interface StockEngineService {

    /**
     * 获取胶料胶安全库存map
     * @param areaGlueList 要查询的胶料列表
     * @return map，key--密炼区+胶料号， value--安全库存值
     */
    Map<String, GlueSafeStock> mapGlueSafeStock(List<GlueAreaMachineVo> areaGlueList);

    /**
     * 获取胶料胶安全库存map
     * @param areaGlue 要查询的胶料
     * @return map，key--密炼区+胶料号， value--安全库存值
     */
    Map<String, GlueSafeStock> mapGlueSafeStock(GlueAreaMachineVo areaGlue);

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
    Map<String, Double> mapFinalGlueStock(Date planDate, String mixArea, Map<String, Double> lastGlueDayPlanMap, Map<String, Double> lastGlueUnclaimedMap, Map<String, String> reserveGlueRecipeMap);

    /**
     * 获取昨日胶料排程白班计划量
     *
     * @param mixArea              密炼区
     * @param planDate             计划日期
     * @param glueRecipeMap        胶料配方映射的胶料名称Map
     * @param reserveGlueRecipeMap 胶料配方映射的反转白班计划量Map
     * @return
     */
    Map<String, Double> mapLastGlueDayPlan(String mixArea, Date planDate, Map<String, String> glueRecipeMap, Map<String, String> reserveGlueRecipeMap);

    /**
     * 获取昨日胶料排程白班计划量和机台信息
     * @param mixArea 密炼区
     * @param planDate 计划日期
     * @return
     */
    Map<String, List<GlueConsumeVo>> mapLastGlueDayPlanAndMachine(String mixArea, Date planDate);

    /**
     * 获取昨日胶料排程白班计划量。按照胶料+机台汇总
     *
     * @param mixArea  密炼区
     * @param planDate 计划日期
     * @return 胶料+机台汇总的白班计划量
     */
    Map<String, Double> mapLastGlueMachineDayPlan(String mixArea, Date planDate);

    /**
     * 获取昨日日用量
     * @param mixArea 密炼区
     * @param planDate 计划日期
     * @param glueUnclaimedImport 设置为1时使用导入的白班待支领量计算预计库存数，设置为其他数值时使用mes的待支领量计算
     * @return
     */
    Map<String, Double> mapLastGlueUnclaimed(String mixArea, Date planDate, String glueUnclaimedImport);

//    /**
//     * 获取终炼胶库存map
//     * @param planDate 计划日期
//     * @param glueUnclaimedImport 设置为1时使用导入的白班待支领量计算预计库存数，设置为其他数值时使用mes的待支领量计算
//     * @param glueAreaMachine 终炼胶+密炼区+机台对象
//     * @return map，key--密炼区+胶料号， value--库存值
//     */
//    Map<String, Double> mapFinalGlueStock(Date planDate , String glueUnclaimedImport, GlueAreaMachineVo glueAreaMachine);

    /**
     * 获取母炼胶库存map
     * @param planDate 计划日期
     * @param glueAreaMachineList 终炼胶+密炼区+机台列表
     * @return map，key--密炼区+胶料号， value--库存值
     */
    Map<String, Double> mapMotherGlueStock(Date planDate, List<GlueAreaMachineVo> glueAreaMachineList);

    /**
     * 获取母炼胶库存map
     * @param planDate 计划日期
     * @param glueAreaMachine 终炼胶+密炼区+机台对象
     * @return map，key--密炼区+胶料号， value--库存值
     */
    Map<String, Double> mapMotherGlueStock(Date planDate, GlueAreaMachineVo glueAreaMachine);


    /**
     * 获取胶料前一天排程8-16点的计划量
     * @param mixArea  密炼区
     * @param planDate  计划日期
     * @return
     */
    Map<String, Double> mapGlueLast8And16Plan(String mixArea, Date planDate);

    /**
     * 获取胶料当天8-12点的完成量
     * @param mixArea  密炼区
     * @param planDate  计划日期
     * @return
     */
    Map<String, Double> mapGlue8And112Finsih(String mixArea, Date planDate);

    /**
     * 获取硫磺辅料库存map
     * @param scheduleDate 排程日期
     * @param areaMaterialList 终炼胶+密炼区+机台列表
     * @return map，key--密炼区+胶料号， value--库存值
     */
    Map<String, Double> mapMaterialStock(Date scheduleDate, List<MaterialAreaMachineVo> areaMaterialList);
    
    /**
     * 获取硫磺辅料安全库存
     * @param mixArea
     * @return
     */
    Map<String, Double> mapMaterialSafeStock(String mixArea); 
}
