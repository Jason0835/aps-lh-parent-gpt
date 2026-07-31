package com.zlt.aps.gdyy.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.gdyy.api.domain.entity.GdyyScheduleResult;
import com.zlt.aps.gdyy.domain.vo.GdyyScheduleResultTemplateImportVO;
import com.zlt.bill.common.service.IDocService;

import java.util.List;

/**
 * 钢带压延排程结果 服务接口。
 */
public interface IGdyyScheduleResultService extends IDocService<GdyyScheduleResult> {

    /**
     * 校验工厂+排程日期+大卷编号+机台编码唯一性。
     */
    String checkUnique(GdyyScheduleResult entity);

    /**
     * 导入排程结果。
     */
    AjaxResult importData(List<GdyyScheduleResult> list, boolean updateSupport, Long importLogId);

    /**
     * 调量。
     */
    AjaxResult changeQty(GdyyScheduleResult entity);

    /**
     * 转机台。
     */
    AjaxResult changeMachine(GdyyScheduleResult entity);

    /**
     * 发布到MES。
     */
    AjaxResult publish(GdyyScheduleResult entity);

    /**
     * 更改发布状态。
     */
    AjaxResult changeReleaseStatus(GdyyScheduleResult entity);

    /**
     * 获取合计信息。
     */
    AjaxResult getSummaryVo(GdyyScheduleResult queryVO);

    /**
     * 导入完成量。
     */
    AjaxResult importFinishQty(List<GdyyScheduleResult> list, boolean updateSupport, Long importLogId);

    /**
     * 按固定生产计划模板整体覆盖导入。
     *
     * @param rows 导入明细
     * @param condition 导入范围
     * @param updateSupport 是否更新
     * @return 导入结果
     */
    AjaxResult importScheduleTemplate(List<GdyyScheduleResultTemplateImportVO> rows,
                                      GdyyScheduleResult condition,
                                      boolean updateSupport);

    /**
     * 使用固定生产计划模板导出。
     *
     * @param currentResults 当前查询结果
     * @param queryVO 导出条件
     * @return Excel文件字节
     */
    byte[] exportData(List<GdyyScheduleResult> currentResults,
                      GdyyScheduleResult queryVO);
}
