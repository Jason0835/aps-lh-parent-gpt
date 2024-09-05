package com.zlt.aps.cx.controller;

import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.cx.api.domain.dto.ReportStatisticsDto;
import com.zlt.aps.cx.service.ReportStatisticsService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 报表统计Controller
 * @author: Chen
 * @since: 2022/4/25 10:19
 */
@Api(tags = "报表统计信息维护接口")
@RestController
@RequestMapping("/cx/reportStatistics")
public class ReportStatisticsController extends BaseController {

    @Autowired
    private ReportStatisticsService reportStatisticsService;

    // 查询条件开始日期和结束日期不能超过的间隔天数
    @Value("${intervalDays}")
    private String intervalDays;

    /**
     * 根据条件查询报表统计列表
     */
    @ApiOperation("根据条件查询报表统计列表")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody ReportStatisticsDto dto) {
        if (ObjectUtils.isNull(dto.getStartTime(), dto.getEndTime())) {
            throw new RuntimeException(I18nUtil.getMessage("ui.data.message.reportStatistics.dateNotNull"));
        }
        if (dto.getEndTime().after(DateUtils.addDays(dto.getStartTime(), Integer.parseInt(intervalDays)))) {
            throw new RuntimeException(String.format(I18nUtil.getMessage("ui.data.message.reportStatistics.moreThanTheNumberOfDays"), intervalDays));
        }
//        startPage();
        dto.setOrderStr(orderStr());
        List<ReportStatisticsDto> list = reportStatisticsService.selectReportStatisticsList(dto);
        return getDataTable(list);
    }

    /**
     * 导出报表统计数据
     */
    @ApiOperation("导出报表统计数据")
    @PostMapping("/export")
    public byte[] export(@RequestBody ReportStatisticsDto dto){
        if (ObjectUtils.isNull(dto.getStartTime(), dto.getEndTime())) {
            throw new RuntimeException(I18nUtil.getMessage("ui.data.message.reportStatistics.dateNotNull"));
        }
        if (dto.getEndTime().after(DateUtils.addDays(dto.getStartTime(), Integer.parseInt(intervalDays)))) {
            throw new RuntimeException(I18nUtil.getMessage("ui.data.message.reportStatistics.moreThanFifteenDays"));
        }
        dto.setOrderStr(orderStr());
        return reportStatisticsService.export(dto);
    }
}
