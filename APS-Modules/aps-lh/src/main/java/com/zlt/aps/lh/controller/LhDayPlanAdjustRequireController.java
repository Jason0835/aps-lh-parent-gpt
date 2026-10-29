package com.zlt.aps.lh.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.lh.api.domain.dto.LhDayPlanAdjustRequireImportDTO;
import com.zlt.aps.lh.api.domain.entity.LhDayPlanAdjustRequire;
import com.zlt.aps.lh.api.domain.vo.LhDayPlanAdjustRequireSummaryVo;
import com.zlt.aps.lh.service.ILhDayPlanAdjustRequireService;
import com.zlt.bill.common.controller.AbstractDocBizController;
import com.zlt.bill.common.service.IDocService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 硫化日计划调整需求控制器。
 */
@Api(tags = "硫化日计划调整需求")
@RestController
@RequestMapping("/lhDayPlanAdjustRequire")
public class LhDayPlanAdjustRequireController extends AbstractDocBizController<LhDayPlanAdjustRequire> {

    @Autowired
    private ILhDayPlanAdjustRequireService lhDayPlanAdjustRequireService;

    /**
     * 查询列表。
     *
     * @param queryVO 查询条件
     * @return 分页列表
     */
    @ApiOperation("查询硫化日计划调整需求列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody LhDayPlanAdjustRequire queryVO) {
        try {
            return lhDayPlanAdjustRequireService.listPage(queryVO);
        } catch (IllegalArgumentException exception) {
            TableDataInfo tableDataInfo = new TableDataInfo();
            tableDataInfo.setCode(500);
            tableDataInfo.setMsg(exception.getMessage());
            return tableDataInfo;
        }
    }

    /**
     * 查询汇总。
     *
     * @param queryVO 查询条件，与列表一致
     * @return 全部匹配行的调整量汇总
     */
    @ApiOperation("查询硫化日计划调整需求汇总")
    @PostMapping("/summary")
    public AjaxResult summary(@RequestBody LhDayPlanAdjustRequire queryVO) {
        try {
            LhDayPlanAdjustRequireSummaryVo summaryVo = lhDayPlanAdjustRequireService.summary(queryVO);
            return AjaxResult.success(summaryVo);
        } catch (IllegalArgumentException exception) {
            return AjaxResult.error(exception.getMessage());
        }
    }

    /**
     * 保存当前行。
     *
     * @param entity 当前行数据
     * @return 保存结果
     */
    @Log(title = "ui.data.column.lhDayPlanAdjustRequire.modelName",
            businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("保存硫化日计划调整需求")
    @PostMapping("/save")
    @Override
    public AjaxResult save(@RequestBody LhDayPlanAdjustRequire entity) {
        try {
            lhDayPlanAdjustRequireService.saveRow(entity);
            return AjaxResult.success(I18nUtil.getMessage(
                    "ui.data.alert.lhDayPlanAdjustRequire.saveSuccess"));
        } catch (IllegalArgumentException exception) {
            return AjaxResult.error(exception.getMessage());
        }
    }

    @Override
    protected IDocService getDocService() {
        return lhDayPlanAdjustRequireService;
    }

    /**
     * 导出硫化日计划调整需求。
     *
     * @param queryVO  查询条件，与列表一致但不分页
     * @param fileName 导出文件名
     * @param response 响应对象，由基类导出流程持有
     * @return Excel 文件字节
     * @throws IOException 文件写出异常
     */
    @Log(title = "ui.data.column.lhDayPlanAdjustRequire.modelName",
            businessType = BusinessType.EXPORT)
    @ApiOperation("导出硫化日计划调整需求")
    @PostMapping("/exportData/{fileName}")
    public byte[] exportData(@RequestBody LhDayPlanAdjustRequire queryVO,
                             @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return lhDayPlanAdjustRequireService.exportData(queryVO, fileName);
    }

    /**
     * 导入硫化日计划调整需求。
     *
     * @param importDTO     导入文件上下文及页面工厂、年月
     * @param updateSupport 已存在调整槽位是否允许覆盖
     * @return 导入结果
     * @throws Exception 导入日志或文件处理异常
     */
    @Log(title = "ui.data.column.lhDayPlanAdjustRequire.modelName",
            businessType = BusinessType.IMPORT)
    @ApiOperation("导入硫化日计划调整需求")
    @PostMapping("/importData")
    public AjaxResult importData(@RequestBody LhDayPlanAdjustRequireImportDTO importDTO,
                                 @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        return lhDayPlanAdjustRequireService.importData(importDTO, updateSupport);
    }

    @Override
    protected String[] getQueryFormulas() {
        return lhDayPlanAdjustRequireService.getQueryFormulas();
    }

    @Override
    protected void builderCondition(
            QueryWrapper<LhDayPlanAdjustRequire> queryWrapper,
            LhDayPlanAdjustRequire queryVO) {
        queryWrapper.eq(StringUtils.isNotBlank(queryVO.getFactoryCode()),
                "FACTORY_CODE", queryVO.getFactoryCode());
        queryWrapper.eq(queryVO.getYearMonth() != null,
                "YEAR_MONTH", queryVO.getYearMonth());
        queryWrapper.like(StringUtils.isNotBlank(queryVO.getMaterialCode()),
                "MATERIAL_CODE", queryVO.getMaterialCode());
        queryWrapper.eq(StringUtils.isNotBlank(queryVO.getProductStatus()),
                "PRODUCT_STATUS", queryVO.getProductStatus());
    }

    @Override
    protected String getTypeCode() {
        return "0";
    }

    @Override
    protected String getOrderBy() {
        return "MATERIAL_CODE asc, PRODUCT_STATUS asc, ADJUST_COUNT asc";
    }
}
