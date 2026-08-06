package com.zlt.aps.gdyy.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.ImportLog;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.api.gateway.system.service.IImportLogService;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.gdyy.api.domain.dto.GdyyScheduleImportDTO;
import com.zlt.aps.gdyy.api.domain.entity.GdyyScheduleResult;
import com.zlt.aps.gdyy.domain.vo.GdyyScheduleResultTemplateImportVO;
import com.zlt.aps.gdyy.mapper.GdyyScheduleResultMapper;
import com.zlt.aps.gdyy.service.IGdyyScheduleResultService;
import com.zlt.aps.utils.AppUtils;
import com.zlt.bill.common.controller.AbstractDocBizController;
import com.zlt.bill.common.service.IDocService;
import com.zlt.common.utils.ImportExcelUtils;
import com.zlt.common.utils.PubUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Date;
import java.util.List;

/**
 * 钢带压延排程结果 控制层。
 * 排程算法入口在本服务中声明，具体算法不在本模块实现。
 */
@Api(tags = "钢带压延排程结果")
@RestController
@RequestMapping("/gdyy/scheduleResult")
public class GdyyScheduleResultController extends AbstractDocBizController<GdyyScheduleResult> {

    @Resource
    private IGdyyScheduleResultService gdyyScheduleResultService;

    @Resource
    private GdyyScheduleResultMapper gdyyScheduleResultMapper;
    @Resource
    private IImportLogService importLogService;

    @ApiOperation("查询钢带压延排程结果列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody GdyyScheduleResult queryVO) {
        return super.list(queryVO);
    }

    @Log(title = "ui.data.column.gdyyScheduleResult.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除钢带压延排程结果")
    @PostMapping("/remove")
    @Override
    public AjaxResult removeByIds(@RequestBody List<Long> ids) {
        if (ids != null && !ids.isEmpty()) {
            LambdaQueryWrapper<GdyyScheduleResult> wrapper = new LambdaQueryWrapper<>();
            wrapper.in(GdyyScheduleResult::getId, ids);
            wrapper.gt(GdyyScheduleResult::getPublishSuccessCount, 0);
            if (gdyyScheduleResultMapper.selectCount(wrapper) > 0) {
                return AjaxResult.error(I18nUtil.getMessage("ui.data.column.gdyyScheduleResult.hasPublishedCanNotDelete"));
            }
        }
        return super.removeByIds(ids);
    }

    @ApiOperation("获取钢带压延排程结果详情")
    @GetMapping("/getInfo/{id}")
    @Override
    public GdyyScheduleResult getInfo(@PathVariable("id") Long id) {
        return super.getInfo(id);
    }

    @Log(title = "ui.data.column.gdyyScheduleResult.modelName", businessType = BusinessType.INSERT)
    @ApiOperation("新增/编辑钢带压延排程结果（插单）")
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody GdyyScheduleResult entity) {
        return super.save(entity);
    }

    @ApiOperation("校验钢带压延排程结果唯一性")
    @PostMapping("/checkUnique")
    public String checkUnique(@RequestBody GdyyScheduleResult entity) {
        return gdyyScheduleResultService.checkUnique(entity);
    }

    @Log(title = "ui.data.column.gdyyScheduleResult.modelName", businessType = BusinessType.UPDATE)
    @ApiOperation("调量")
    @PostMapping("/changeQty")
    public AjaxResult changeQty(@RequestBody GdyyScheduleResult entity) {
        return gdyyScheduleResultService.changeQty(entity);
    }

    @Log(title = "ui.data.column.gdyyScheduleResult.modelName", businessType = BusinessType.UPDATE)
    @ApiOperation("转机台")
    @PostMapping("/changeMachine")
    public AjaxResult changeMachine(@RequestBody GdyyScheduleResult entity) {
        return gdyyScheduleResultService.changeMachine(entity);
    }

    @Log(title = "ui.data.column.gdyyScheduleResult.modelName", businessType = BusinessType.UPDATE)
    @ApiOperation("发布")
    @PostMapping("/publish")
    public AjaxResult publish(@RequestBody GdyyScheduleResult entity) {
        return gdyyScheduleResultService.publish(entity);
    }

    @Log(title = "ui.data.column.gdyyScheduleResult.modelName", businessType = BusinessType.UPDATE)
    @ApiOperation("更改发布状态")
    @PostMapping("/changeReleaseStatus")
    public AjaxResult changeReleaseStatus(@RequestBody GdyyScheduleResult entity) {
        return gdyyScheduleResultService.changeReleaseStatus(entity);
    }

    @ApiOperation("获取合计信息")
    @PostMapping("/getSummaryVo")
    public AjaxResult getSummaryVo(@RequestBody GdyyScheduleResult queryVO) {
        return gdyyScheduleResultService.getSummaryVo(queryVO);
    }

    @Log(title = "ui.data.column.gdyyScheduleResult.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入钢带压延排程结果")
    @PostMapping("/importData")
    @Override
    public AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        return super.importData(importContext, updateSupport);
    }

    @Log(title = "ui.data.column.gdyyScheduleResult.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("按固定模板导入钢带压延排程结果")
    @PostMapping("/importDataByCust/{updateSupport}")
    public AjaxResult importDataByCust(@PathVariable("updateSupport") boolean updateSupport,
                                       @RequestBody GdyyScheduleImportDTO importDTO) throws Exception {
        Date beginTime = DateUtils.getNowDate();
        ImportContext importContext = importDTO.getImportContext();
        ImportLog importLog = ImportExcelUtils.getImportLogAndUploadFile(
                importContext.getFileBytes(), importContext.getImportFilePath(),
                importContext.getProcedureCode(), importContext.getFunctionName(),
                importContext.getOriFileName(), 1);
        importLog = this.importLogService.add(importLog);
        // 第1行按隐藏字段键匹配VO，第2至4行是标题和表头，从第5行开始读取导入明细
        ExcelUtil<GdyyScheduleResultTemplateImportVO> excelUtil =
                new ExcelUtil<>(GdyyScheduleResultTemplateImportVO.class);
        List<GdyyScheduleResultTemplateImportVO> rows = excelUtil.importExcel(
                new ByteArrayInputStream(importContext.getFileBytes()), 0, 4, -1);
        AjaxResult ajaxResult = this.gdyyScheduleResultService.importScheduleTemplate(
                rows, importDTO.getScheduleResult(), updateSupport);
        Date endTime = DateUtils.getNowDate();
        importLog.setRowCount(rows.size());
        importLog.setBeginTime(beginTime);
        importLog.setEndTime(endTime);
        importLog.setSpendTime(DateUtils.getDiffTime(endTime, beginTime));
        ImportExcelUtils.updateImportLogAndFormatMsg(
                importLog, ajaxResult, this.importLogService);
        return ajaxResult;
    }

    @Log(title = "ui.data.column.gdyyScheduleResult.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入完成量")
    @PostMapping("/importFinishQty")
    public AjaxResult importFinishQty(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        return super.importData(importContext, updateSupport);
    }

    @Log(title = "ui.data.column.gdyyScheduleResult.modelName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出钢带压延排程结果")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody GdyyScheduleResult queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return this.gdyyScheduleResultService.exportData(
                this.listExportData(queryVO), queryVO);
    }

    @Override
    protected List<GdyyScheduleResult> listExportData(GdyyScheduleResult obj) {
        QueryWrapper<GdyyScheduleResult> wrapper = new QueryWrapper<>();
        builderCondition(wrapper, obj);
        List<GdyyScheduleResult> list = gdyyScheduleResultMapper.selectList(wrapper);
        AppUtils.formatData(list, getQueryFormulas());
        return list;
    }

    @Override
    protected IDocService getDocService() {
        return gdyyScheduleResultService;
    }

    @Override
    protected void builderCondition(QueryWrapper<GdyyScheduleResult> qw, GdyyScheduleResult vo) {
        qw.eq(PubUtil.isNotEmpty(vo.getFactoryCode()), "FACTORY_CODE", vo.getFactoryCode());
        qw.eq(vo.getScheduleDate() != null, "SCHEDULE_DATE", vo.getScheduleDate());
        qw.like(PubUtil.isNotEmpty(vo.getBigRollCode()), "BIG_ROLL_CODE", vo.getBigRollCode());
        qw.eq(PubUtil.isNotEmpty(vo.getMachineCode()), "MACHINE_CODE", vo.getMachineCode());
        qw.eq(PubUtil.isNotEmpty(vo.getIsRelease()), "IS_RELEASE", vo.getIsRelease());
        qw.eq(PubUtil.isNotEmpty(vo.getBatchNo()), "BATCH_NO", vo.getBatchNo());
    }

    @Override
    protected String getTypeCode() {
        return "GDYY_SCHEDULE_RESULT";
    }

    @Override
    protected String getOrderBy() {
        return "MACHINE_CODE ASC, BIG_ROLL_CODE ASC, SCHEDULE_DATE DESC";
    }
}
