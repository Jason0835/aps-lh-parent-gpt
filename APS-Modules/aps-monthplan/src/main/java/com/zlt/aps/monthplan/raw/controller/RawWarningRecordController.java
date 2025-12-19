package com.zlt.aps.monthplan.raw.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.common.security.annotation.RequiresPermissions;
import com.zlt.aps.maindata.mapper.RawWarningRecordEntityMapper;
import com.zlt.aps.maindata.service.IRawWarningRecordService;
import com.zlt.aps.monthplan.api.domain.entity.RawWarningRecord;
import com.zlt.aps.monthplan.raw.service.IRawWarningService;
import com.zlt.common.utils.PubUtil;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import lombok.extern.slf4j.Slf4j;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.ruoyi.common.core.web.page.TableDataInfo;

import com.zlt.bill.common.controller.AbstractDocBizController;
import com.zlt.bill.common.service.IDocService ;

/**
* Copyright (c) 2022, All rights reserved。
* 文件名称：RawWarningRecordController.java
* 描    述：原材料预警记录 控制层类：....
*@author zlt
*@date 2025-12-17
*@version 1.0
*
 *  修改记录：
*     修改时间：...
*     修 改 人：zlt
*     修改内容：...
*/
@Slf4j
@Api(tags = "原材料预警记录")
@RestController
@RequestMapping("/rawWarningRecord")
public class RawWarningRecordController extends AbstractDocBizController<RawWarningRecord> {

    @Autowired
    private IRawWarningRecordService rawWarningRecordService;

    @Autowired
    private RawWarningRecordEntityMapper entityMapper;

    @Autowired
    private IRawWarningService rawWarningService;

    /**
     * 查询原材料预警记录列表
     */
    @RequiresPermissions( "maindata:rawWarningRecord:list")
    @ApiOperation("查询列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody RawWarningRecord queryVO) {
        return super.list(queryVO);
    }

    @Override
    protected String getOrderBy() {
        return "create_time desc";
    }

    /**
     * 获取原材料预警记录详细信息
     */
    @RequiresPermissions( "maindata:rawWarningRecord:query")
    @ApiOperation("获取详细信息")
    @GetMapping(value = "/{billId}")
    @Override
    public RawWarningRecord getInfo(@PathVariable("billId") Long billId) {
        return super.getInfo(billId);
    }


    /**
     * 导出列表
     */
    @RequiresPermissions( "maindata:rawWarningRecord:export")
    @Log(title = "原材料预警记录", businessType = BusinessType.EXPORT)
    @ApiOperation("导入数据")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody RawWarningRecord queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return super.exportData(queryVO, fileName, response);
    }

    @Override
    protected List<RawWarningRecord> listExportData(RawWarningRecord obj) {
        QueryWrapper<RawWarningRecord> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, obj);
        return entityMapper.selectList(wrapper);
    }

    @Override
    protected IDocService getDocService(){
        return rawWarningRecordService;
    }

    /**
     * 条件拼接
     *
     * @param queryWrapper
     * @param queryVO
     */
    @Override
    protected void builderCondition(QueryWrapper<RawWarningRecord> queryWrapper, RawWarningRecord queryVO) {
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("factoryCode")), "FACTORY_CODE", queryVO.getFieldValueByFieldName("factoryCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("warningType")), "WARNING_TYPE", queryVO.getFieldValueByFieldName("warningType"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("materialCode")), "MATERIAL_CODE", queryVO.getFieldValueByFieldName("materialCode"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("materialDesc")), "MATERIAL_DESC", queryVO.getFieldValueByFieldName("materialDesc"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("warningLevel")), "WARNING_LEVEL", queryVO.getFieldValueByFieldName("warningLevel"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("warningTitle")), "WARNING_TITLE", queryVO.getFieldValueByFieldName("warningTitle"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("warningContent")), "WARNING_CONTENT", queryVO.getFieldValueByFieldName("warningContent"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("relatedMonth")), "RELATED_MONTH", queryVO.getFieldValueByFieldName("relatedMonth"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("relatedWeek")), "RELATED_WEEK", queryVO.getFieldValueByFieldName("relatedWeek"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("warningData")), "WARNING_DATA", queryVO.getFieldValueByFieldName("warningData"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("status")), "STATUS", queryVO.getFieldValueByFieldName("status"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("handler")), "HANDLER", queryVO.getFieldValueByFieldName("handler"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("handleTime")), "HANDLE_TIME", queryVO.getFieldValueByFieldName("handleTime"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("handleOpinion")), "HANDLE_OPINION", queryVO.getFieldValueByFieldName("handleOpinion"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("notified")), "NOTIFIED", queryVO.getFieldValueByFieldName("notified"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("notifyTime")), "NOTIFY_TIME", queryVO.getFieldValueByFieldName("notifyTime"));
    }


    @Override
    protected String getTypeCode(){
        return "S3521";
    }

    @RequiresPermissions( "maindata:rawWarningRecord:executeUsageWarning")
    @PostMapping("/execute-usage-warning")
    @ApiOperation("执行用量偏差预警")
    public AjaxResult executeUsageWarning(@RequestParam("factoryCode") String factoryCode,
                                          @RequestParam("year") Integer year,
                                          @RequestParam("week") Integer week,
                                          @RequestParam("month") Integer month){
        return rawWarningService.executeUsageDeviationWarning(factoryCode, year, week, month);
    }

    @RequiresPermissions( "maindata:rawWarningRecord:executeNewMaterialWarning")
    @PostMapping("/execute-new-material-warning")
    @ApiOperation("执行新材料预警")
    public AjaxResult executeNewMaterialWarning(@RequestParam("factoryCode") String factoryCode,
                                                @RequestParam("year") Integer year,
                                                @RequestParam("month") Integer month) {
        return rawWarningService.executeNewMaterialWarning(factoryCode, year, month);
    }

    @RequiresPermissions( "maindata:rawWarningRecord:syncActualUsage")
    @PostMapping("/sync-actual-usage")
    @ApiOperation("同步周维度原材料实际用量数据")
    public AjaxResult syncActualUsage(@RequestParam("factoryCode") String factoryCode,
                                      @RequestParam("year") Integer year,
                                      @RequestParam("week") Integer week,
                                      @RequestParam("month") Integer month){
        return rawWarningService.syncWeekActualUsage(factoryCode, year, week, month);
    }

    @RequiresPermissions( "maindata:rawWarningRecord:handleWarning")
    @PostMapping("/handle-warning")
    @ApiOperation("处理预警记录")
    public AjaxResult handleWarning(@RequestParam("id") Long id,
                                    @RequestParam("handler") String handler,
                                    @RequestParam("opinion") String opinion) {
        return rawWarningService.handleWarning(id, handler, opinion);
    }

    @RequiresPermissions( "maindata:rawWarningRecord:handleWarning")
    @GetMapping("/statistics")
    @ApiOperation("获取预警统计")
    public AjaxResult getStatistics(@RequestParam("factoryCode") String factoryCode,
                                    @RequestParam(value = "warningType", required = false) String warningType,
                                    @RequestParam(value = "days", required = false) Integer days) {
        Map<String, Object> statistics = rawWarningService.getWarningStatistics(
                factoryCode, warningType, days);
        return AjaxResult.success(statistics);
    }
}
