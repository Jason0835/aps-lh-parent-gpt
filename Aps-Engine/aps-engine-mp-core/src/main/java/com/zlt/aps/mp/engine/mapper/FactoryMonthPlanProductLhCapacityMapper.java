package com.zlt.aps.mp.engine.mapper;

import com.zlt.aps.mp.engine.domain.vo.CycleStructureMinLhMachineQtyVo;
import com.zlt.aps.mp.engine.domain.vo.MonthCycleGroupInfoVo;
import com.zlt.aps.mp.engine.domain.vo.MonthPlanProductLhCapacityVo;
import com.zlt.aps.mp.engine.domain.vo.MonthPlanStructureLhRatioVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 月度计划-硫化产能相关业务SQL定义
 *
 * @author ZLT
 * @date 20251210
 */
@Mapper
public interface FactoryMonthPlanProductLhCapacityMapper {
    /**
     * 根据需求计划，获取对应的SKU的日硫化量
     * 包含MES日硫化量，标准的日硫化量，APS计算的日硫化量，硫化总时间单位(s)
     *
     * @param factoryCode      工厂编码
     * @param year             年份
     * @param month            月份
     * @param monthPlanVersion 需求计划版本
     * @return
     */
    List<MonthPlanProductLhCapacityVo> getProductionLhCapacityInfo(@Param("factoryCode") String factoryCode,
                                                                   @Param("year") Integer year,
                                                                   @Param("month") Integer month,
                                                                   @Param("monthPlanVersion") String monthPlanVersion);

    /**
     * 获取工厂对应的结构硫化配比信息
     *
     * @param factoryCode       工厂编码
     * @param structureNameList 结构信息
     * @return
     */
    List<MonthPlanStructureLhRatioVo> getStructureLhRatioInfo(@Param("factoryCode") String factoryCode, @Param("structureNameList") List<String> structureNameList);

    /**
     * 获取工厂周期结构最低硫化配比信息
     *
     * @param factoryCode 工厂编码
     * @param year        年份
     * @param month       月份
     * @return
     */
    List<CycleStructureMinLhMachineQtyVo> getCycleStructureMinLhRatioInfo(@Param("factoryCode") String factoryCode,
                                                                          @Param("year") Integer year,
                                                                          @Param("month") Integer month);

    /**
     * 获取工厂的月周期结构排产清单信息
     *
     * @param factoryCode 工厂编码
     * @param year        年份
     * @param month       月份
     * @return
     */
    List<MonthCycleGroupInfoVo> getMonthCycleGroupListInfo(@Param("factoryCode") String factoryCode,
                                                           @Param("year") Integer year,
                                                           @Param("month") Integer month);
}
