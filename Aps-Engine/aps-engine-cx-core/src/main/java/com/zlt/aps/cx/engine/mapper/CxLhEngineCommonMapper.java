package com.zlt.aps.cx.engine.mapper;

import com.zlt.aps.cx.engine.domain.CxEngineMesMoldAdjustPlan;
import com.zlt.aps.cx.engine.domain.CxEngineSapImportBadNumber;
import com.zlt.aps.cx.engine.domain.CxInProductionSpec;
import com.zlt.aps.cx.engine.domain.MesLhProductionSpec;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 成型/硫化工序 模具变动单相关公用Mapper
 */
@Mapper
public interface CxLhEngineCommonMapper {

    /**
     * 自动排程日期对应的模具变动单临时表数据迁移到日志表
     * @param scheduleDate
     * @return
     */
    int syncMoldChagePlanToLog(@Param("scheduleDate") String scheduleDate, @Param("sourceCxOrder") String sourceCxOrder);

    /**
     * 删除模具变动单数据
     * @param scheduleDate
     * @return
     */
    int deleteLhEngineMoldChangePlanByScheduleDate(@Param("scheduleDate") String scheduleDate, @Param("sourceCxOrder") String sourceCxOrder);

    /**
     * 临时获取导入废次品数更新到最新成型排程中
     * @param cxEngineSapImportBadNumber
     * @return
     */
    List<CxEngineSapImportBadNumber> selectSapImportBadNumberList(CxEngineSapImportBadNumber cxEngineSapImportBadNumber);

    /**
     * 移除接口不良表中的数据
     * @param month
     * @return
     */
    int  removeBadNumberByMonth(@Param("month") String month);

    /**
     * 根据日期进行获取在产规格
     * @param productDate yyyyMMdd
     * @return 所有成型机台当前在产的规格数
     */
    List<CxInProductionSpec> selectInProductionSpecByDate(@Param("productDate") String productDate);

    /**
     *  根据日期进行获取对应的mes的的换膜计划信息
     * @param cxEngineMesMoldAdjustPlan
     * @return
     */
    List<CxEngineMesMoldAdjustPlan> selectMesMoldAdjustPlanList(CxEngineMesMoldAdjustPlan cxEngineMesMoldAdjustPlan);

    /**
     * 根据日期进行获取硫化工序在产规格
     * @param productDate yyyyMMdd
     * @return 所有硫化机台当前在产的规格数
     */
    List<MesLhProductionSpec> selectLhInProductionSpecByDate(@Param("productDate") String productDate);
}
