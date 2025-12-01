package com.zlt.aps.monthplan.factory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.monthplan.api.domain.entity.MpHistorySaleQty;
import com.zlt.aps.monthplan.api.domain.vo.CalcStockingResultVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MpHistorySaleQtyMapper.java
 * 描    述：历史销售记录Mapper接口
 *
 * @author hsc
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：hsc
 * 修改内容：...
 * @date 2025-02-13
 */
@Mapper
public interface MpHistorySaleQtyMapper extends BaseMapper<MpHistorySaleQty> {

    /**
     * 查询
     *
     * @param mpHistorySaleQty
     * @return
     */
    List<MpHistorySaleQty> selectMpHistorySaleQtyList(MpHistorySaleQty mpHistorySaleQty);

    /**
     * 查询计算备货数据
     *
     * @param startYearMonth 开始年月
     * @param endYearMonth   结束年月
     * @param tireType       轮胎类型
     * @return
     */
    List<CalcStockingResultVo> selectCalcStocking(@Param("startYearMonth") String startYearMonth, @Param("endYearMonth") String endYearMonth, @Param("tireType") String tireType, @Param("month") Long month);

    /**
     * 批量插入历史消费记录
     *
     * @param historySaleQties
     */
    void batchInsertHistorySaleQty(@Param("list") List<MpHistorySaleQty> historySaleQties);

    /**
     * 更新历史销售记录
     *
     * @param dates1 开始日期
     * @param dates2 结束日期
     * @return 结果
     */
    int updateByDayHisSale(@Param("startDate") String dates1, @Param("endDate") String dates2);

    /**
     * 新增不存在的历史销售记录
     *
     * @return 结果
     */
    int insertByDayHisSaleNotExist();
}
