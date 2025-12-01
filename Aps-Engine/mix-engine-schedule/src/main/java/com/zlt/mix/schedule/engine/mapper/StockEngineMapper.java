package com.zlt.mix.schedule.engine.mapper;

import com.zlt.mix.schedule.engine.vo.GlueAreaMachineVo;
import com.zlt.mix.schedule.engine.vo.GlueConsumeVo;
import com.zlt.mix.schedule.engine.vo.MaterialAreaMachineVo;
import com.zlt.mix.setting.api.domain.entity.*;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
 * 库存相关mapper
 */
public interface StockEngineMapper {

    /**
     * 获取安全库存列表
     * @param glueAreaMachineList 终炼胶+密炼区+机台列表
     * @return
     */
    List<GlueSafeStock> listGlueSafeStock(@Param("glueAreaMachineList") List<GlueAreaMachineVo> glueAreaMachineList);

    /**
     * 获取终炼胶库存列表
     * @param planDate 计划日期
     * @param glueAreaMachineList 终炼胶+密炼区+机台列表
     * @return
     */
    List<GlueStock> listFinalGlueStock(@Param("planDate") Date planDate, @Param("glueAreaMachineList") List<GlueAreaMachineVo> glueAreaMachineList);

    /**
     * 获取终炼胶库存列表
     *
     * @param planDate 计划日期
     * @param mixArea  密炼区
     * @return
     */
    List<GlueStock> listFinalGlueStockByMixArea(@Param("planDate") Date planDate, @Param("mixArea") String mixArea);

    /**
     * 获取昨日胶料排程白班计划量
     * @param mixArea  密炼区
     * @param planDate 昨日计划日期
     * @return
     */
    List<GlueConsumeVo> listLastGlueDayPlan(@Param("mixArea") String mixArea, @Param("planDate") Date planDate);

    /**
     * 获取昨日胶料排程白班计划量和机台信息
     * @param mixArea  密炼区
     * @param planDate 昨日计划日期
     * @return
     */
    List<GlueConsumeVo> listLastGlueDayPlanAndMachine(@Param("mixArea") String mixArea, @Param("planDate") Date planDate);

    /**
     * 获取昨日日用量
     * @param mixArea
     * @param planDate 昨日计划日期
     * @param glueUnclaimedImport 设置为1时使用导入的白班待支领量计算预计库存数，设置为其他数值时使用mes的待支领量计算
     * @return
     */
    List<GlueConsumeVo> listLastGlueUnclaimed(@Param("mixArea") String mixArea, @Param("planDate") Date planDate, @Param("glueUnclaimedImport") String glueUnclaimedImport);

    /**
     * 获取母炼胶库存列表
     * @param planDate 计划日期
     * @param glueAreaMachineList 终炼胶+密炼区+机台列表
     * @return
     */
    List<MlGlueStock> listMotherGlueStock(@Param("planDate") Date planDate, @Param("glueAreaMachineList") List<GlueAreaMachineVo> glueAreaMachineList);

    /**
     * 获取胶料前一天排程8-16点的计划量
     * @param mixArea  密炼区
     * @param planDate  计划日期
     * @return
     */
    List<GlueConsumeVo> listGlueLast8And16Plan(@Param("mixArea") String mixArea, @Param("planDate") Date planDate);

    /**
     * 获取胶料当天8-12点的完成量
     * @param mixArea  密炼区
     * @param planDate  计划日期
     * @return
     */
    List<GlueConsumeVo> listGlue8And112Finsih(@Param("mixArea") String mixArea, @Param("planDate") Date planDate);

    /**
     * 获取硫磺辅料库存列表
     * @param scheduleDate 排程日期
     * @param areaMaterialList 物料名称+密炼区
     * @return
     */
    List<LhflGlueStock> listMaterialStock(@Param("scheduleDate") Date scheduleDate, @Param("areaMaterialList") List<MaterialAreaMachineVo> areaMaterialList);

    /**
     * 计算出胶料8-16点预计要消耗的硫磺辅料车数
     * @param scheduleDate  排程日期
     * @param areaMaterialList 物料名称+密炼区
     * @return
     */
    List<GlueConsumeVo> listGlueLastDayPlan(@Param("scheduleDate") Date scheduleDate, @Param("areaMaterialList") List<MaterialAreaMachineVo> areaMaterialList);

    /**
     * 计算出胶料8-12点实际要消耗硫磺辅料车数
     * @param scheduleDate  排程日期
     * @param areaMaterialList 物料名称+密炼区
     * @return
     */
    List<GlueConsumeVo> listGlueLastDayFinish(@Param("scheduleDate") Date scheduleDate, @Param("areaMaterialList") List<MaterialAreaMachineVo> areaMaterialList);

    /**
     * 查询硫磺辅料安全库存
     * @param mixArea 密炼区
     * @return
     */
    List<LhflSafeStock> listMaterialSafeStock(@Param("mixArea") String mixArea);

}
