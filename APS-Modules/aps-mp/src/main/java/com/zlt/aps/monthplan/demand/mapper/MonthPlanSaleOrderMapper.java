package com.zlt.aps.monthplan.demand.mapper;

import com.zlt.aps.monthplan.api.domain.entity.MonthPlanSaleOrder;
import com.zlt.aps.monthplan.api.domain.itf.InSaleOrderDto;
import com.zlt.aps.monthplan.api.domain.vo.MonthPlanSaleRequirePlanVo;
import com.zlt.aps.monthplan.factory.dto.YearSaleMinProdVo;
import com.zlt.core.dao.basemapper.CommBaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MonthPlanSaleOrderMapper.java
 * 描    述：月度销售计划订单Mapper接口
 *
 * @author ZLT
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：ZLT
 * 修改内容：...
 * @date 2025-02-14
 */
@Mapper
public interface MonthPlanSaleOrderMapper extends CommBaseMapper<MonthPlanSaleOrder> {
    /**
     * 获取需要新增的近12个月年销量超过saleTotalQty需要新增的最小批量ProductCode集合
     *
     * @param factoryCode  分厂编码
     * @param year         年
     * @param month        月
     * @param saleTotalQty 总销量
     * @return
     */
    List<YearSaleMinProdVo> getNeedInsertProductMinConfigurationList(@Param("factoryCode") String factoryCode, @Param("year") Integer year, @Param("month") Integer month, @Param("saleTotalQty") Integer saleTotalQty);

    /**
     * 根据近12个月销量总值，更新上调控制水位值为1
     *
     * @param factoryCode  分厂
     * @param year         年份
     * @param month        月份
     * @param saleTotalQty 销售总量
     * @return
     */
    int updateMinProdUpQtyToOne(@Param("factoryCode") String factoryCode, @Param("year") Integer year, @Param("month") Integer month, @Param("saleTotalQty") Integer saleTotalQty);

    /**
     * 根据近12个月销量总值小于saleTotalQty，则对upQty=1的记录
     * 更新上调控制水位值为默认值defaultQty
     *
     * @param factoryCode  分厂
     * @param year         年份
     * @param month        月份
     * @param saleTotalQty 销售总量
     * @param defaultQty   默认值
     * @return
     */
    int updateMinProdUpQtyToDefault(@Param("factoryCode") String factoryCode,
                                    @Param("year") Integer year,
                                    @Param("month") Integer month,
                                    @Param("saleTotalQty") Integer saleTotalQty,
                                    @Param("defaultQty") Integer defaultQty);

    /**
     * 根据分厂、年份、月份删除对应的销售记录
     *
     * @param factoryCode 分厂编码
     * @param year        年份
     * @param month       月份
     * @return
     */
    int deletedByYearAndMonth(@Param("factoryCode") String factoryCode, @Param("year") Integer year, @Param("month") Integer month);

    /**
     * 根据分厂、年份、月份，更新是否重要客户标识
     * 匹配客户信息中重要客户标识为1 则是重要客户，否则为0
     *
     * @param updateCondition 匹配数据条件
     * @return
     */
    int updateImportantCustomFlag(MonthPlanSaleRequirePlanVo updateCondition);

    /**
     * 根据分厂、年份、月份，更新是否必保计划
     * 匹配必保计划根据客户及物料编码标识为1 则是必保计划，否则为0
     *
     * @param updateCondition 匹配数据条件
     * @return
     */
    int updateEnsurePlan(MonthPlanSaleRequirePlanVo updateCondition);

    /**
     * 根据分厂、年份、月份，更新物料的信息
     * 物料规格，公用规格类型，花纹，品牌等信息
     *
     * @param updateCondition
     * @return
     */
    int updateProductInfo(MonthPlanSaleRequirePlanVo updateCondition);

    /**
     * 将中间库的外销订单数据更新到销售订单表
     *
     * @param inSaleOrderDto 外销订单数据
     * @return 结果
     */
    int updateOutSaleOrder(InSaleOrderDto inSaleOrderDto);

    /**
     * 将中间库的外销订单数据更新到销售订单表
     *
     * @param inSaleOrderDto 外销订单数据
     * @return 结果
     */
    int insertOutSaleOrder(InSaleOrderDto inSaleOrderDto);
}
