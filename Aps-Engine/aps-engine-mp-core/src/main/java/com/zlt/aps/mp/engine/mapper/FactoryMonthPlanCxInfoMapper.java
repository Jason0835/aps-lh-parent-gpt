package com.zlt.aps.mp.engine.mapper;

import com.zlt.aps.mp.engine.domain.vo.CxDevicePlanShutInfoVo;
import com.zlt.aps.mp.engine.domain.vo.CxMachineBaseInfoVo;
import com.zlt.aps.mp.engine.daylimit.CxWorkWearInfoVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
 * 月度计划-续作规格业务SQL接口定义
 *
 * @author ZLT
 * @date 20251210
 */
@Mapper
public interface FactoryMonthPlanCxInfoMapper {
    /**
     * 获取工厂的成型基础信息
     * 包含成型基础信息，固定结构，固定SKU，不可作业结构，不可作业SKU
     *
     * @param factoryCode 工厂编码
     * @return
     */
    List<CxMachineBaseInfoVo> getMachineBaseInfo(@Param("factoryCode") String factoryCode);

    /**
     * 获取工厂的成型工装信息
     *
     * @param factoryCode 工厂编码
     * @return
     */
    List<CxWorkWearInfoVo> getWorkWearInfo(@Param("factoryCode") String factoryCode);

    /**
     * 获取工厂周期范围内的成型机维修信息
     *
     * @param factoryCode         工厂编码
     * @param productionStartDate 周期起始日期
     * @param productionEndDate   周期结束日期
     * @return
     */
    List<CxDevicePlanShutInfoVo> getDevicePlanShutInfo(@Param("factoryCode") String factoryCode,
                                                       @Param("productionStartDate") Date productionStartDate,
                                                       @Param("productionEndDate") Date productionEndDate);
}
