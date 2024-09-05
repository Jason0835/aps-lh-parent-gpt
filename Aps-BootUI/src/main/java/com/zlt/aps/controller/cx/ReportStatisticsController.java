package com.zlt.aps.controller.cx;

import com.ruoyi.api.gateway.system.domain.ExportLog;
import com.ruoyi.api.gateway.system.domain.SysDictData;
import com.ruoyi.api.gateway.system.service.IExportLogService;
import com.ruoyi.api.gateway.system.service.ISysDictDataCacheService;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.utils.ExportUtil;
import com.zlt.aps.cx.api.domain.dto.ReportStatisticsDto;
import com.zlt.aps.cx.api.service.IReportStatisticsService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 报表统计Controller
 * @author: Chen
 * @since: 2022/4/25 10:19
 */
@Api(tags = "报表统计")
@Controller
@RequestMapping("/cx/reportStatistics")
public class ReportStatisticsController {

    private final String prefix = "cx/reportStatistics";

    @Autowired
    private IReportStatisticsService iReportStatisticsService;
    @Autowired
    private ISysDictDataCacheService iSysDictDataCacheService;
    @Autowired
    private IExportLogService iExportLogService;

    /**
     * 跳转至主页面
     */
    @RequiresPermissions("cx:reportStatistics:view")
    @GetMapping
    public String toIndex(ModelMap modelMap) {
        modelMap.put("initDate", DateUtils.parseDateToStr("yyyy-MM-dd", new Date()));
        return prefix + "/reportStatistics";
    }

    /**
     * 根据条件查询报表统计列表
     */
    @ApiOperation("根据条件查询报表统计列表")
    @RequiresPermissions("cx:reportStatistics:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(ReportStatisticsDto dto) {
        return iReportStatisticsService.list(dto);
    }

    /**
     * 导出报表统计列表
     */
    @ApiOperation("导出报表统计列表")
    @RequiresPermissions("cx:reportStatistics:export")
    @GetMapping("/export")
    @ResponseBody
    public void export(HttpServletResponse response, ReportStatisticsDto dto) throws IOException {
        Map<String, String> procedureCodeMap = iSysDictDataCacheService.getType("PROCEDURE_CODE").stream()
                .collect(Collectors.toMap(SysDictData::getDictValue, SysDictData::getDictLabel));
        dto.setProcedureCodeMap(procedureCodeMap);
        byte[] data = iReportStatisticsService.export(dto);
        String fileName = I18nUtil.getMessage("ui.data.column.reportStatistics.modelName");
        ExportLog exportLog = ExportUtil.uploadAndExportExcelByByte(response, data, fileName, dto.toString(), ApsConstant.PROCEDURE_CODE_CX);
        iExportLogService.add(exportLog);
    }
}
