package com.zlt.aps.mp.engine.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.zlt.aps.monthplan.api.domain.entity.MdmProductStock;

/**
 * 月度计划-成品库存业务SQL接口定义
 *
 * @author ZLT
 * @date 20251227
 */
@Mapper
public interface FactoryMonthPlanMdmProductStockMapper {
    /**
     * 获取工厂下排产版本：所有成品库存信息
     *
     * @param factoryCode       工厂编码
     * @param year              年份
     * @param month             月份
     * @return
     */
    List<MdmProductStock> getMdmProductStock(@Param("factoryCode") String factoryCode, @Param("year") Integer year, @Param("month") Integer month);
}
