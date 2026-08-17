package com.zlt.aps.mp.report.controller;

import com.ruoyi.api.gateway.system.domain.ExportLog;
import com.ruoyi.api.gateway.system.service.IExportLogService;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.utils.ServletUtils;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.mp.api.domain.dto.MonthPlanCompareDto;
import com.zlt.aps.mp.report.service.IMonthPlanCompareService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;

/**
 * 月计划与实际产量对比报表 Controller
 *
 * @author APS
 * @date 2026-08-13
 */
@Slf4j
@Api(tags = "月计划与实际产量对比报表")
@RestController
@RequestMapping("/monthPlanCompare")
public class MonthPlanCompareController extends BaseController {

    @Autowired
    private IMonthPlanCompareService monthPlanCompareService;

    @Autowired
    private IExportLogService iExportLogService;

    /**
     * 查询月计划与实际产量对比列表（分页）
     * <p>按 SKU 分页，total 为 SKU 总数，rows 为当前页 SKU 的 4 行 VO</p>
     *
     * @param queryDto 查询条件（含 pageNum/pageSize）
     * @return TableDataInfo 结果（total=SKU总数，rows=当前页4×N行VO）
     */
    @ApiOperation("查询月计划与实际产量对比列表（分页）")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody MonthPlanCompareDto queryDto) {
        // 手动分页（按SKU分页，避免PageHelper对4行VO分页导致total/rows不一致）
        return monthPlanCompareService.listMonthPlanComparePage(queryDto);
    }

    /**
     * 导出月计划与实际产量对比数据
     *
     * @param entity   查询条件
     * @param fileName 文件名
     * @return Excel 文件字节数组
     */
    @ApiOperation("导出月计划与实际产量对比数据")
    @PostMapping("/export")
    public byte[] export(@RequestBody MonthPlanCompareDto entity, @RequestParam("fileName") String fileName) {
        Date beginTime = DateUtils.getNowDate();
        byte[] resultBytes = monthPlanCompareService.exportMonthPlanCompare(entity);
        Date endTime = DateUtils.getNowDate();
        // 记录导出日志
        ExportLog exportLog = new ExportLog();
        exportLog.setProcedureCode("0");
        exportLog.setExportParams(entity.toString());
        String uri = ServletUtils.getRequest().getRequestURI();
        exportLog.setFunctionCode(uri.split("/")[1]);
        exportLog.setFunctionName(fileName);
        exportLog.setFileName(fileName + ".xlsx");
        exportLog.setBeginTime(beginTime);
        exportLog.setEndTime(endTime);
        exportLog.setSpendTime(DateUtils.getDiffTime(endTime, beginTime));
        this.iExportLogService.add(exportLog);
        return resultBytes;
    }
}
