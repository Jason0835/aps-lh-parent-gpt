package com.zlt.aps.mp.report.mapper;

import com.zlt.aps.mp.api.domain.dto.MonthPlanCompareDto;
import com.zlt.aps.mp.api.domain.entity.FactoryMonthPlanProductionFinalResult;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 月计划与实际产量对比报表 Mapper
 *
 * @author APS
 * @date 2026-08-13
 */
@Mapper
public interface MonthPlanCompareMapper {

    /**
     * 查询月计划定稿主数据总数（用于分页 total）
     *
     * @param queryDto 查询参数
     * @return SKU 总数
     */
    int selectFinalListCount(MonthPlanCompareDto queryDto);

    /**
     * 查询月计划定稿主数据（含每日计划量 DAY_1 ~ DAY_31）
     * <p>当 queryDto.pageNum/pageSize/offset 不为空时按分页查询，否则查全量（导出场景）</p>
     *
     * @param queryDto 查询参数
     * @return 定稿列表
     */
    List<FactoryMonthPlanProductionFinalResult> selectFinalList(MonthPlanCompareDto queryDto);

    /**
     * 查询每日实际完成量（按 物料编码 + LH_TYPE + 日期日 聚合）
     * <p>
     * 返回 Map 字段：
     * materialCode - 物料编码
     * lhType       - 示方类型（对应产品状态）
     * dayNum       - 日期日（1~31）
     * finishQty    - 完成量合计
     * </p>
     * <p>当 queryDto.materialKeys 不为空时，仅查询当前页 SKU 对应的实际产量</p>
     *
     * @param queryDto 查询参数
     * @return 每日完成量列表
     */
    List<Map<String, Object>> selectDailyFinishQtyList(@Param("queryDto") MonthPlanCompareDto queryDto);
}
