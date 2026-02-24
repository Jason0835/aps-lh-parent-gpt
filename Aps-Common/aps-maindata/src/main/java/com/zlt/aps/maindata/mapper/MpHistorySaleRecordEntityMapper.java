package com.zlt.aps.maindata.mapper;

import com.zlt.aps.monthplan.api.domain.entity.MpHistorySaleRecord;
import com.zlt.core.dao.basemapper.CommBaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MpHistorySaleRecordMapper.java
 * 描    述：历史销售记录Mapper接口
 *@author zlt
 *@date 2025-12-11
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */
@Mapper
public interface MpHistorySaleRecordEntityMapper extends CommBaseMapper<MpHistorySaleRecord> {

    /**
     * 查询滚动月销量汇总值
     * @param factoryCode 工厂
     * @param minYearMonth 最小统计年月
     * @param maxYearMonth 最大统计年月
     * @return 结果
     */
    List<MpHistorySaleRecord> selectRollMonthSaleQty(@Param("factoryCode") String factoryCode,
                                                     @Param("minYearMonth") String minYearMonth,
                                                     @Param("maxYearMonth") String maxYearMonth);

    /**
     * 查询区域销量
     *
     * @param factoryCode  分厂
     * @param codeList     物料编号列表
     * @param minYearMonth 最小统计年月
     * @param maxYearMonth 最大统计年月
     * @return 结果
     */
    List<MpHistorySaleRecord> selectSumQtyGroupByArea(@Param("factoryCode") String factoryCode,
                                                      @Param("minYearMonth") String minYearMonth,
                                                      @Param("maxYearMonth") String maxYearMonth,
                                                      @Param("list") List<String> codeList);

    /**
     * 查询月销量
     *
     * @param factoryCode  分厂
     * @param codeList     物料编号列表
     * @param minYearMonth 最小统计年月
     * @param maxYearMonth 最大统计年月
     * @return 结果
     */
    List<MpHistorySaleRecord> selectSumQtyGroupByMonth(@Param("factoryCode") String factoryCode,
                                                       @Param("minYearMonth") String minYearMonth,
                                                       @Param("maxYearMonth") String maxYearMonth,
                                                       @Param("list") List<String> codeList);
}
