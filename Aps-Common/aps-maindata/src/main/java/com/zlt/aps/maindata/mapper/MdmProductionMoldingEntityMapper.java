package com.zlt.aps.maindata.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.monthplan.api.domain.entity.MdmProductionMolding;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

/**
 * @Entity com.zlt.lean.engine.domain.FactoryProductionProduct
 */
@Repository
public interface MdmProductionMoldingEntityMapper extends BaseMapper<MdmProductionMolding> {

    public int checkFactoryProductionProductUnique(MdmProductionMolding mdmProductionMolding);

    /**
     * 根据分厂可生产最后一天的月度计划，生成指定月份的成型机正在生产的品种
     *
     * @param username           触发人
     * @param factoryCode        分厂编号
     * @param fromYear           月度计划对应年
     * @param fromMonth          月度计划对应月
     * @param generateYear       生成年
     * @param generateMonth      生成月
     * @param productionDayField 指定日期生产量
     * @return 结果数
     */
    int generateByMonthPlan(@Param("username") String username,
                            @Param("factoryCode") String factoryCode,
                            @Param("fromYear") int fromYear,
                            @Param("fromMonth") int fromMonth,
                            @Param("generateYear") Integer generateYear,
                            @Param("generateMonth") Integer generateMonth,
                            @Param("productionDayField") String productionDayField);
}




