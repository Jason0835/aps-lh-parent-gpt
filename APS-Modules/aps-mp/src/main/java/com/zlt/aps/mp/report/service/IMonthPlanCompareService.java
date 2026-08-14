package com.zlt.aps.mp.report.service;

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
     * 查询月计划与实际产量对比列表
     * <p>每个SKU返回4行（月计划/实际产量/差异/完成率）</p>
     *
     * @param queryDto 查询参数
     * @return 结果列表
     */
    List<MonthPlanCompareVo> listMonthPlanCompare(MonthPlanCompareDto queryDto);

    /**
     * 导出月计划与实际产量对比数据
     *
     * @param queryDto 查询参数
     * @return Excel 文件字节数组
     */
    byte[] exportMonthPlanCompare(MonthPlanCompareDto queryDto);
}
