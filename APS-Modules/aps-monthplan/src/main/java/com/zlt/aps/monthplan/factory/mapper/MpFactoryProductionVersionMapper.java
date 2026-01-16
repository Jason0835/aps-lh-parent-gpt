package com.zlt.aps.monthplan.factory.mapper;

import com.zlt.aps.monthplan.api.domain.entity.FactoryMonthPlanProductionFinalResult;
import com.zlt.aps.monthplan.api.domain.entity.MpFactoryProductionVersion;
import com.zlt.aps.monthplan.api.domain.vo.FactoryProductionParamVo;
import com.zlt.core.dao.basemapper.CommBaseMapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

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
 * @date 20251203
 */
@Mapper
public interface MpFactoryProductionVersionMapper extends CommBaseMapper<MpFactoryProductionVersion> {
    /**
     * 根据分厂、年份、月份。需求版本，删除需求版本计划
     * t_mp_product_require_plan
     * t_mp_order_plan_allocation
     * t_mp_month_require_stock
     *
     * @param factoryProductionParam
     * @return
     */
    @Deprecated
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
    int deletedByProductionVersion(FactoryProductionParamVo factoryProductionParam);

    /**
     * 根据分厂、年份、月份。需求版本，排产版本，删除对应的排产版本计划（针对只有最后一个版本的情况，不能直接删除版本表的数据）
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
    int deletedLastVersionByProductionVersion(FactoryProductionParamVo factoryProductionParam);

    /**
     * 根据分厂、年份、月份。需求版本，排产版本，查询对应的排产版本计划数量
     *
     * @param factoryProductionParam 参数
     * @return 结果
     */
    int selectCountByProductionVersion(FactoryProductionParamVo factoryProductionParam);

    /**
     * 根据工厂、年份、月份、需求版本删除对应的版本版本数据
     * t_mp_proc_version 版本记录保留一条，清除排产版本信息，其它排产版本记录删除
     * t_mp_proc_month_plan_init 初始化需求版本的所有删除
     * t_mp_proc_no_production_plan 未排产计划需求版本的所有删除
     * t_mp_moulding_day_result  需求版本对应的模具排产版本 所有删除
     * t_mp_mould_day_detail_log 需求版本对应的模具排产版本日志 所有删除
     * t_mp_mould_use_status_log 模具状态日志 所有删除
     * t_mp_mould_lh_log 模具硫化组日志 所有删除
     *
     * @param factoryProductionParam
     * @return
     */
    int deletedProductionVersionAndUpdateLastFlag(FactoryProductionParamVo factoryProductionParam);

    /**
     * 查询对应年月+分厂的需求计划版本
     *
     * @param query 查询条件
     */
    List<String> versionList(FactoryMonthPlanProductionFinalResult query);

    /**
     * 查询对应年月+分厂+需求计划版本的分厂月计划版本
     */
    List<String> productionVersionList(FactoryMonthPlanProductionFinalResult query);
}
