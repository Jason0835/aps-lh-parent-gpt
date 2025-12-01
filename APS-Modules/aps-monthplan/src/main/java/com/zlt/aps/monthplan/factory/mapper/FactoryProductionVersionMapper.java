package com.zlt.aps.monthplan.factory.mapper;

import com.zlt.aps.monthplan.api.domain.entity.FactoryProductionVersion;
import com.zlt.aps.monthplan.api.domain.vo.FactoryProductionParamVo;
import com.zlt.core.dao.basemapper.CommBaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：FactoryProductionVersionMapper.java
 * 描    述：分厂月度计划排程版本Mapper接口
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-03-19
 */
@Mapper
public interface FactoryProductionVersionMapper extends CommBaseMapper<FactoryProductionVersion> {
    /**
     * 根据分厂、年份、月份。需求版本，删除需求版本计划
     * t_mp_product_require_plan
     * t_mp_order_plan_allocation
     * t_mp_month_require_stock
     *
     * @param factoryProductionParam
     * @return
     */
    int deletedMonthPlanRequireVersion(FactoryProductionParamVo factoryProductionParam);

    /**
     * 根据分厂、年份、月份。需求版本，排产版本，删除对应的排产版本计划
     * t_mp_proc_version
     * t_mp_proc_month_plan_init
     * t_mp_proc_no_production_record
     * t_mp_proc_no_production_plan
     * t_mp_moulding_day_result
     * t_mp_moulding_day_result_detail
     * t_mp_moulding_helper
     *
     * @param factoryProductionParam
     * @return
     */
    int deletedProductionVersion(FactoryProductionParamVo factoryProductionParam);

    /**
     * 根据分厂、年份、月份。需求版本，排产版本，删除对应的最后排产版本计划
     * t_mp_proc_version 做更新
     * t_mp_proc_month_plan_init
     * t_mp_proc_no_production_record
     * t_mp_proc_no_production_plan
     * t_mp_moulding_day_result
     * t_mp_moulding_day_result_detail
     * t_mp_moulding_helper
     *
     * @param factoryProductionParam
     * @return
     */
    int deletedProductionVersionByLast(FactoryProductionParamVo factoryProductionParam);
}
