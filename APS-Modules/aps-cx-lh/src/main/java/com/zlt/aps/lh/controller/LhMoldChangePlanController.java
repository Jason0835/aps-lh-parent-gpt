package com.zlt.aps.lh.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.ImportLog;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.api.gateway.system.service.IImportErrorLogService;
import com.ruoyi.api.gateway.system.service.IImportLogService;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.lh.api.domain.dto.LhScheduleImportFileDTO;
import com.zlt.aps.lh.api.domain.entity.LhMoldChangePlan;
import com.zlt.aps.lh.service.ILhMoldChangePlanService;
import com.zlt.bill.common.controller.AbstractDocBizController;
import com.zlt.bill.common.service.IDocService;
import com.zlt.common.utils.ImportExcelUtils;
import com.zlt.common.utils.PubUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.time.DateFormatUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.List;

/**
* Copyright (c) 2022, All rights reserved。
* 文件名称：LhMoldChangePlanController.java
* 描    述：模具变动单 控制层类：....
*@author zlt
*@date 2025-02-17
*@version 1.0
*
 *  修改记录：
*     修改时间：...
*     修 改 人：zlt
*     修改内容：...
*/
@Slf4j
@Api(tags = "模具变动单")
@RestController
@RequestMapping("/lhMoldChangePlan")
public class LhMoldChangePlanController extends AbstractDocBizController<LhMoldChangePlan> {

    @Autowired
    private ILhMoldChangePlanService lhMoldChangePlanService;
    @Autowired
    private IImportLogService iImportLogService;
    @Autowired
    private IImportErrorLogService iImportErrorLogService;


    @Log(title = "ui.data.column.lhParams.modelName")
    @ApiOperation("生成换模计划")
    @PostMapping("/generateMoldReplacementPlan")
    public AjaxResult generateMoldReplacementPlan(@RequestBody LhMoldChangePlan queryVO){
        lhMoldChangePlanService.moldReplacementPlan(queryVO);
        return AjaxResult.success();
    }
    /**
     * 查询模具变动单列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody LhMoldChangePlan queryVO) {
        return super.list(queryVO);
    }


    /**
     * 保存
     */
    @Log(title = "ui.data.column.lhMoldChangePlan.modelName", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("保存")
    @PostMapping("/save")
    @Override
    public AjaxResult save(@RequestBody LhMoldChangePlan billVO){
        return super.save(billVO);
    }

    /**
     * 删除
     */
    @Log(title = "ui.data.column.lhMoldChangePlan.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除")
    @DeleteMapping("/remove")
    @Override
    public AjaxResult removeByIds(@RequestBody List<Long> ids){
        return super.removeByIds(ids);
    }


    /**
     * 获取模具变动单详细信息
     */
    @ApiOperation("获取详细信息")
    @GetMapping(value = "/{billId}")
    @Override
    public LhMoldChangePlan getInfo(@PathVariable("billId") Long billId) {
        return super.getInfo(billId);
    }


    /**
     * 根据集合导入模具变动单数据
     * @param importContext 导入上下文
     * @param updateSupport 已存在记录是否更新
     * @return 结果
     */
    @Log(title = "ui.data.column.lhMoldChangePlan.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData/{updateSupport}")
    @Override
    public AjaxResult importData(@RequestBody ImportContext importContext, @PathVariable("updateSupport") boolean updateSupport) throws Exception {
        return super.importData(importContext,updateSupport);
    }

    /**
     * 导出列表
     */
    @Log(title = "模具变动单", businessType = BusinessType.EXPORT)
    @ApiOperation("导出数据")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody LhMoldChangePlan queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return super.exportData(queryVO, fileName, response);
    }

    @Override
    public List<LhMoldChangePlan> listExportData(LhMoldChangePlan port) {
        startPage("LH_MACHINE_CODE asc,CHANGE_TIME asc");
        QueryWrapper<LhMoldChangePlan> queryWrapper = new QueryWrapper<>();
        this.builderCondition(queryWrapper, port);
        return lhMoldChangePlanService.selectList(queryWrapper);
    }

    @Override
    protected IDocService getDocService(){
        return lhMoldChangePlanService;
    }

    /**
     * 条件拼接
     *
     * @param queryWrapper
     * @param queryVO
     */
    @Override
    protected void builderCondition(QueryWrapper<LhMoldChangePlan> queryWrapper, LhMoldChangePlan queryVO) {
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("scheduleDate")), "SCHEDULE_DATE", queryVO.getFieldValueByFieldName("scheduleDate"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("factoryCode")), "FACTORY_CODE", queryVO.getFieldValueByFieldName("factoryCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("moldBatchNo")), "MOLD_BATCH_NO", queryVO.getFieldValueByFieldName("moldBatchNo"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("lhMachineCode")), "LH_MACHINE_CODE", queryVO.getFieldValueByFieldName("lhMachineCode"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("lhMachineName")), "LH_MACHINE_NAME", queryVO.getFieldValueByFieldName("lhMachineName"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("beforeSpecCode")), "BEFORE_SPEC_CODE", queryVO.getFieldValueByFieldName("beforeSpecCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("beforeSpecDesc")), "BEFORE_SPEC_DESC", queryVO.getFieldValueByFieldName("beforeSpecDesc"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("tireRoughStock")), "TIRE_ROUGH_STOCK", queryVO.getFieldValueByFieldName("tireRoughStock"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("changeType")), "CHANGE_TYPE", queryVO.getFieldValueByFieldName("changeType"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("afterSpecCode")), "AFTER_SPEC_CODE", queryVO.getFieldValueByFieldName("afterSpecCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("afterSpecDesc")), "AFTER_SPEC_DESC", queryVO.getFieldValueByFieldName("afterSpecDesc"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("stockArea")), "STOCK_AREA", queryVO.getFieldValueByFieldName("stockArea"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("changeTime")), "CHANGE_TIME", queryVO.getFieldValueByFieldName("changeTime"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("sourceCxOrder")), "SOURCE_CX_ORDER", queryVO.getFieldValueByFieldName("sourceCxOrder"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("isRelease")), "IS_RELEASE", queryVO.getFieldValueByFieldName("isRelease"));
        queryWrapper.ge(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("changeTimeStart")), "CHANGE_TIME", queryVO.getFieldValueByFieldName("changeTimeStart"));
        if (PubUtil.isNotEmpty(queryVO.getChangeTimeEnd())) {
            queryWrapper.le(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("changeTimeEnd")), "CHANGE_TIME", DateFormatUtils.format(queryVO.getChangeTimeEnd(), "yyyy-MM-dd 23:59:59"));
        }
    }


    /**
     * 导入数据
     */
    @Log(title = "ui.data.column.port.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入换模计划")
    @PostMapping("/importMoldChangePlan")
    public AjaxResult importMoldChangePlan(@RequestBody LhScheduleImportFileDTO lhScheduleImportFileDTO) throws Exception {

        Date beginTime = DateUtils.getNowDate();
        ImportLog importLog = ImportExcelUtils.getImportLogAndUploadFile(lhScheduleImportFileDTO.getImportContext().getFileBytes(), lhScheduleImportFileDTO.getImportContext().getImportFilePath(), lhScheduleImportFileDTO.getImportContext().getProcedureCode(), lhScheduleImportFileDTO.getImportContext().getFunctionName(), lhScheduleImportFileDTO.getImportContext().getOriFileName(), 1);
        importLog = this.iImportLogService.add(importLog);
        ExcelUtil<LhMoldChangePlan> util = new ExcelUtil<>(this.getTClass());
        InputStream is = new ByteArrayInputStream(lhScheduleImportFileDTO.getImportContext().getFileBytes());
        List<LhMoldChangePlan> list = util.importExcel(is);
        AjaxResult ajaxResult = lhMoldChangePlanService.importMoldChangePlan(list, importLog.getId(), lhScheduleImportFileDTO.getScheduleDate());
        Date endTime = DateUtils.getNowDate();
        importLog.setRowCount(list.size());
        importLog.setBeginTime(beginTime);
        importLog.setEndTime(endTime);
        importLog.setSpendTime(DateUtils.getDiffTime(endTime, beginTime));
        ImportExcelUtils.updateImportLogAndFormatMsg(importLog, ajaxResult, this.iImportLogService);
        ImportExcelUtils.saveImportErrorLogs(ajaxResult, this.iImportErrorLogService);
        return ajaxResult;
    }


    @Override
    protected String getTypeCode(){
        return "0301";
    }

    @Override
    protected String getOrderBy() {
        return "create_time desc";
    }
}
