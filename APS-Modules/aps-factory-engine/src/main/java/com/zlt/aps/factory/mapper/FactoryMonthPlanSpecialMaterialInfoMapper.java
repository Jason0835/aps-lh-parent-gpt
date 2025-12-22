package com.zlt.aps.factory.mapper;

import com.zlt.aps.factory.domain.vo.EmbryoSpecialMaterialInfoVo;
import com.zlt.aps.factory.domain.vo.SpecialMaterialStockVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 月度计划-特殊材料业务SQL接口定义
 *
 * @author ZLT
 * @date 20251222
 */
@Mapper
public interface FactoryMonthPlanSpecialMaterialInfoMapper {
    /**
     * 获取工厂下排产版本：含有特殊原材料的胎胚特殊材料bom信息
     *
     * @param factoryCode       工厂编码
     * @param year              年份
     * @param month             月份
     * @param monthPlanVersion  需求计划版本
     * @param productionVersion 排产版本
     * @return
     */
    List<EmbryoSpecialMaterialInfoVo> getSpecialMaterialEmbryoInfo(@Param("factoryCode") String factoryCode, @Param("year") Integer year, @Param("month") Integer month, @Param("monthPlanVersion") String monthPlanVersion, @Param("productionVersion") String productionVersion);

    /**
     * 获取工厂下排产版本：特殊原材料的库存信息
     *
     * @param factoryCode       工厂编码
     * @return
     */
    List<SpecialMaterialStockVo> getSpecialMaterialStockInfo(@Param("factoryCode") String factoryCode);


}
