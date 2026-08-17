package com.zlt.aps.controller.maindata;

import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.mp.api.domain.dto.MonthPlanCompareDto;
import com.zlt.aps.mp.api.service.IMonthPlanCompareRemoteService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * 月计划与实际产量对比报表 BootUI Controller
 * <p>
 * 标准链路：Vue → BootUI(/monthplan/monthPlanCompare) → Gateway → aps-monthplan(/monthPlanCompare)
 * </p>
 *
 * @author APS
 * @date 2026-08-13
 */
@Slf4j
@Controller
@RequestMapping("/monthplan/monthPlanCompare")
@Api(tags = "月计划与实际产量对比报表")
public class MonthPlanCompareUIController {

    @Autowired
    private IMonthPlanCompareRemoteService monthPlanCompareRemoteService;

    /**
     * 查询月计划与实际产量对比列表
     *
     * @param queryDto 查询条件
     * @return TableDataInfo 结果
     */
    @RequiresPermissions("report:monthPlanCompare:list")
    @ApiOperation("查询月计划与实际产量对比列表")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(MonthPlanCompareDto queryDto) {
        return monthPlanCompareRemoteService.listMonthPlanCompare(queryDto);
    }

    /**
     * 导出月计划与实际产量对比数据
     *
     * @param response 响应对象
     * @param entity   查询条件
     * @throws IOException IO异常
     */
    @RequiresPermissions("report:monthPlanCompare:export")
    @ApiOperation("导出月计划与实际产量对比数据")
    @GetMapping("/export")
    @ResponseBody
    public void export(HttpServletResponse response, MonthPlanCompareDto entity) throws IOException {
        String fileName = I18nUtil.getMessage("ui.data.column.monthPlanCompare.modelName");
        byte[] excelBytes = monthPlanCompareRemoteService.exportMonthPlanCompare(entity, fileName);
        ByteArrayInputStream in = new ByteArrayInputStream(excelBytes);
        ExcelUtil.setResponseHeader(response, fileName, ".xlsx");
        IOUtils.copy(in, response.getOutputStream());
        response.flushBuffer();
    }
}
