package com.zlt.aps.mp.report.service;

import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.mp.api.domain.dto.MonthPlanCompareDto;
import com.zlt.aps.mp.api.domain.vo.MonthPlanCompareVo;

import java.util.List;

/**
 * 月计划与实际产量对比报表服务
 *
 * @author APS
 * @date 2026-08-13
 */
public interface IMonthPlanCompareService {

    /**
     * 查询月计划与实际产量对比列表（全量，用于导出）
     * <p>每个SKU返回4行（月计划/实际产量/差异/完成率）</p>
     * <p>不读取 queryDto.pageNum/pageSize/offset/materialKeys，按全量查询</p>
     *
     * @param queryDto 查询参数
     * @return 结果列表
     */
    List<MonthPlanCompareVo> listMonthPlanCompare(MonthPlanCompareDto queryDto);

    /**
     * 查询月计划与实际产量对比列表（分页，用于列表展示）
     * <p>按 SKU 分页，total 为 SKU 总数，rows 为当前页 SKU 的 4 行 VO</p>
     *
     * @param queryDto 查询参数（需包含 pageNum/pageSize）
     * @return 分页结果
     */
    TableDataInfo listMonthPlanComparePage(MonthPlanCompareDto queryDto);

    /**
     * 导出月计划与实际产量对比数据
     *
     * @param queryDto 查询参数
     * @return Excel 文件字节数组
     */
    byte[] exportMonthPlanCompare(MonthPlanCompareDto queryDto);
}
