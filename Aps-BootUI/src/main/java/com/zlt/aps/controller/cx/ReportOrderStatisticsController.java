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
import com.zlt.aps.cx.api.domain.dto.ReportClassAccuracyDto;
import com.zlt.aps.cx.api.domain.dto.ReportOrderStatisticsDto;
import com.zlt.aps.cx.api.service.IReportOrderStatisticsService;
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
 * 工单完成统计报表
 * @author: Chen
 * @since: 2022/8/10 10:59
 */
@Api(tags = "工单完成统计报表")
@Controller
@RequestMapping("/cx/reportOrderStatistics")
public class ReportOrderStatisticsController {

    @Autowired
    private IReportOrderStatisticsService iReportOrderStatisticsService;
    @Autowired
    private ISysDictDataCacheService iSysDictDataCacheService;
    @Autowired
    private IExportLogService iExportLogService;

    private final String prefix = "cx/reportOrderStatistics";

    /**
     * 跳转至主页面
     */
    @RequiresPermissions("cx:reportOrderStatistics:view")
    @GetMapping()
    public String toIndex(ModelMap modelMap) {
        modelMap.put("initDate", DateUtils.parseDateToStr("yyyy-MM-dd", DateUtils.addDays(new Date(), 1)));  //当前日期+1天
        return prefix + "/reportOrderStatistics";
    }

    /**
     * 根据条件查询工单完成统计报表列表
     */
    @ApiOperation("根据条件查询工单完成统计报表列表")
    @RequiresPermissions("cx:reportOrderStatistics:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(ReportOrderStatisticsDto dto) {
        // 根据汇总方式返回数据
        if ("1".equals(dto.getStatisticalMethod())) {
            return iReportOrderStatisticsService.selectReportStatisticsList(dto);
        }else {
            return iReportOrderStatisticsService.selectReportSummaryList(dto);
        }
    }

    /**
     * 导出工单完成统计报表
     */
    @ApiOperation("导出工单完成统计报表")
    @RequiresPermissions("cx:reportOrderStatistics:export")
    @GetMapping("/export")
    @ResponseBody
    public void export(HttpServletResponse response, ReportOrderStatisticsDto dto) throws IOException {
        Map<String, String> procedureCodeMap = iSysDictDataCacheService.getType("PROCEDURE_CODE").stream()
                .collect(Collectors.toMap(SysDictData::getDictValue, SysDictData::getDictLabel));
        dto.setProcedureCodeMap(procedureCodeMap);
        //获取字节流数据
        byte[] data = iReportOrderStatisticsService.export(dto);
        if (data == null) {
            return;
        }
        String fileName = I18nUtil.getMessage("ui.data.column.reportOrderStatistics.modelName");
        ExportLog exportLog = ExportUtil.uploadAndExportExcelByByte(response, data, fileName, dto.toString(), ApsConstant.PROCEDURE_CODE_CX);
        iExportLogService.add(exportLog);
    }
}
