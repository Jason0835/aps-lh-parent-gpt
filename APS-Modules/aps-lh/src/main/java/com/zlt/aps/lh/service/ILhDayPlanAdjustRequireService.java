package com.zlt.aps.lh.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.common.engine.domain.LhDayPlanAdjustVo;
import com.zlt.aps.lh.api.domain.dto.LhDayPlanAdjustRequireImportDTO;
import com.zlt.aps.lh.api.domain.entity.LhDayPlanAdjustRequire;
import com.zlt.aps.lh.api.domain.vo.LhDayPlanAdjustRequireSummaryVo;
import com.zlt.bill.common.service.IDocService;

import java.time.YearMonth;
import java.util.List;

/**
 * 硫化日计划调整需求服务。
 */
public interface ILhDayPlanAdjustRequireService extends IDocService<LhDayPlanAdjustRequire> {

    /**
     * 查询月计划驱动的调整需求列表。
     *
     * @param queryVO 查询条件
     * @return 分页列表
     */
    TableDataInfo listPage(LhDayPlanAdjustRequire queryVO);

    /**
     * 汇总当前查询条件下全部月计划行的调整量。
     *
     * @param queryVO 查询条件
     * @return 调整1、调整2、调整3和调整后总合计
     */
    LhDayPlanAdjustRequireSummaryVo summary(LhDayPlanAdjustRequire queryVO);

    /**
     * 保存当前行三次调整。
     *
     * @param entity 当前行数据
     */
    void saveRow(LhDayPlanAdjustRequire entity);

    /**
     * 按查询条件导出全部匹配行，不分页且忽略排序参数。
     *
     * @param queryVO 查询条件，与列表一致
     * @param fileName 导出文件名
     * @return Excel 文件字节
     */
    byte[] exportData(LhDayPlanAdjustRequire queryVO, String fileName);

    /**
     * 导入硫化日计划调整需求。
     *
     * @param importDTO 导入文件上下文及页面工厂、年月
     * @param updateSupport 已存在调整槽位是否允许覆盖
     * @return 导入结果
     * @throws Exception 导入日志或文件处理异常
     */
    AjaxResult importData(LhDayPlanAdjustRequireImportDTO importDTO, boolean updateSupport) throws Exception;

    /**
     * 获取查询公式。
     *
     * @return 查询公式
     */
    String[] getQueryFormulas();

    /**
     * 获取指定年月，工厂、Sku的硫化日计划调整信息
     *
     * @param yearMonth 年-月
     * @param factoryList 工厂信息
     * @param materialCodeList Sku编码
     * @return 调整信息列表
     */
    List<LhDayPlanAdjustVo> getMonthPlanLhDayAdjustList(YearMonth yearMonth, List<String> factoryList, List<String> materialCodeList);
}
