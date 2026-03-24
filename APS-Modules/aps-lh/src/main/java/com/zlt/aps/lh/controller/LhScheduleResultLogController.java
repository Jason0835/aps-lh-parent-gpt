package com.zlt.aps.lh.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.lh.api.domain.entity.LhScheduleResultLog;
import com.zlt.aps.lh.mapper.LhScheduleResultLogEntityMapper;
import com.zlt.aps.lh.service.ILhScheduleResultLogService;
import com.zlt.bill.common.controller.AbstractDocBizController;
import com.zlt.bill.common.service.IDocService;
import com.zlt.common.utils.PubUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：LhScheduleResultLogController.java
 * 描    述：硫化排程结果日志 控制层类：....
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-03-27
 */
@Slf4j
@Api(tags = "硫化排程结果日志")
@RestController
@RequestMapping("/lhScheduleResultLog")
public class LhScheduleResultLogController extends AbstractDocBizController<LhScheduleResultLog> {

    @Autowired
    private ILhScheduleResultLogService lhScheduleResultLogService;

    @Autowired
    private LhScheduleResultLogEntityMapper entityMapper;

    /**
     * 查询硫化排程结果日志列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody LhScheduleResultLog queryVO) {
        return super.list(queryVO);
    }

    @Override
    protected String getOrderBy() {
        return "create_time desc";
    }

    /**
     * 保存
     */
    @Log(title = "ui.data.column.lhScheduleResultLog.modelName", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("保存")
    @PostMapping("/save")
    @Override
    public AjaxResult save(@RequestBody LhScheduleResultLog billVO) {
        return super.save(billVO);
    }

    /**
     * 删除
     */
    @Log(title = "ui.data.column.lhScheduleResultLog.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除")
    @DeleteMapping("/remove")
    @Override
    public AjaxResult removeByIds(@RequestBody List<Long> ids) {
        return super.removeByIds(ids);
    }


    /**
     * 获取硫化排程结果日志详细信息
     */
    @ApiOperation("获取详细信息")
    @GetMapping(value = "/{billId}")
    @Override
    public LhScheduleResultLog getInfo(@PathVariable("billId") Long billId) {
        return super.getInfo(billId);
    }


    /**
     * 根据集合导入硫化排程结果日志数据
     *
     * @param importContext 导入上下文
     * @param updateSupport 已存在记录是否更新
     * @return 结果
     */
    @Log(title = "ui.data.column.lhScheduleResultLog.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    @Override
    public AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        return super.importData(importContext, updateSupport);
    }

    /**
     * 导出列表
     */
    @Log(title = "硫化排程结果日志", businessType = BusinessType.EXPORT)
    @ApiOperation("导入数据")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody LhScheduleResultLog queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return super.exportData(queryVO, fileName, response);
    }

    @Override
    protected List<LhScheduleResultLog> listExportData(LhScheduleResultLog obj) {
        QueryWrapper<LhScheduleResultLog> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, obj);
        return entityMapper.selectList(wrapper);
    }

    @Override
    protected IDocService getDocService() {
        return lhScheduleResultLogService;
    }

    /**
     * 条件拼接
     *
     * @param queryWrapper
     * @param queryVO
     */
    @Override
    protected void builderCondition(QueryWrapper<LhScheduleResultLog> queryWrapper, LhScheduleResultLog queryVO) {
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("factoryCode")), "FACTORY_CODE", queryVO.getFieldValueByFieldName("factoryCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("batchNo")), "BATCH_NO", queryVO.getFieldValueByFieldName("batchNo"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("orderNo")), "ORDER_NO", queryVO.getFieldValueByFieldName("orderNo"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("lhMachineCode")), "LH_MACHINE_CODE", queryVO.getFieldValueByFieldName("lhMachineCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("leftRightMold")), "LEFT_RIGHT_MOLD", queryVO.getFieldValueByFieldName("leftRightMold"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("lhMachineName")), "LH_MACHINE_NAME", queryVO.getFieldValueByFieldName("lhMachineName"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("productCode")), "PRODUCT_CODE", queryVO.getFieldValueByFieldName("productCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("specsCode")), "SPECS_CODE", queryVO.getFieldValueByFieldName("specsCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("embryoCode")), "EMBRYO_CODE", queryVO.getFieldValueByFieldName("embryoCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("embryoStock")), "EMBRYO_STOCK", queryVO.getFieldValueByFieldName("embryoStock"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("specDesc")), "SPEC_DESC", queryVO.getFieldValueByFieldName("specDesc"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("stockArea")), "STOCK_AREA", queryVO.getFieldValueByFieldName("stockArea"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("lhTime")), "LH_TIME", queryVO.getFieldValueByFieldName("lhTime"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("dailyPlanQty")), "DAILY_PLAN_QTY", queryVO.getFieldValueByFieldName("dailyPlanQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("scheduleDate")), "SCHEDULE_DATE", queryVO.getFieldValueByFieldName("scheduleDate"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("productionStatus")), "PRODUCTION_STATUS", queryVO.getFieldValueByFieldName("productionStatus"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("class1PlanQty")), "CLASS1_PLAN_QTY", queryVO.getFieldValueByFieldName("class1PlanQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("class1StartTime")), "CLASS1_START_TIME", queryVO.getFieldValueByFieldName("class1StartTime"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("class1EndTime")), "CLASS1_END_TIME", queryVO.getFieldValueByFieldName("class1EndTime"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("class1Analysis")), "CLASS1_ANALYSIS", queryVO.getFieldValueByFieldName("class1Analysis"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("class1FinishQty")), "CLASS1_FINISH_QTY", queryVO.getFieldValueByFieldName("class1FinishQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("class2PlanQty")), "CLASS2_PLAN_QTY", queryVO.getFieldValueByFieldName("class2PlanQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("class2StartTime")), "CLASS2_START_TIME", queryVO.getFieldValueByFieldName("class2StartTime"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("class2EndTime")), "CLASS2_END_TIME", queryVO.getFieldValueByFieldName("class2EndTime"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("class2Analysis")), "CLASS2_ANALYSIS", queryVO.getFieldValueByFieldName("class2Analysis"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("class2FinishQty")), "CLASS2_FINISH_QTY", queryVO.getFieldValueByFieldName("class2FinishQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("class3PlanQty")), "CLASS3_PLAN_QTY", queryVO.getFieldValueByFieldName("class3PlanQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("class3StartTime")), "CLASS3_START_TIME", queryVO.getFieldValueByFieldName("class3StartTime"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("class3EndTime")), "CLASS3_END_TIME", queryVO.getFieldValueByFieldName("class3EndTime"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("class3Analysis")), "CLASS3_ANALYSIS", queryVO.getFieldValueByFieldName("class3Analysis"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("class3FinishQty")), "CLASS3_FINISH_QTY", queryVO.getFieldValueByFieldName("class3FinishQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("class4PlanQty")), "CLASS4_PLAN_QTY", queryVO.getFieldValueByFieldName("class4PlanQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("class4StartTime")), "CLASS4_START_TIME", queryVO.getFieldValueByFieldName("class4StartTime"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("class4EndTime")), "CLASS4_END_TIME", queryVO.getFieldValueByFieldName("class4EndTime"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("class4Analysis")), "CLASS4_ANALYSIS", queryVO.getFieldValueByFieldName("class4Analysis"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("class4FinishQty")), "CLASS4_FINISH_QTY", queryVO.getFieldValueByFieldName("class4FinishQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("class5PlanQty")), "CLASS5_PLAN_QTY", queryVO.getFieldValueByFieldName("class5PlanQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("class5StartTime")), "CLASS5_START_TIME", queryVO.getFieldValueByFieldName("class5StartTime"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("class5EndTime")), "CLASS5_END_TIME", queryVO.getFieldValueByFieldName("class5EndTime"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("class5Analysis")), "CLASS5_ANALYSIS", queryVO.getFieldValueByFieldName("class5Analysis"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("class5FinishQty")), "CLASS5_FINISH_QTY", queryVO.getFieldValueByFieldName("class5FinishQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("class6PlanQty")), "CLASS6_PLAN_QTY", queryVO.getFieldValueByFieldName("class6PlanQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("class6StartTime")), "CLASS6_START_TIME", queryVO.getFieldValueByFieldName("class6StartTime"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("class6EndTime")), "CLASS6_END_TIME", queryVO.getFieldValueByFieldName("class6EndTime"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("class6Analysis")), "CLASS6_ANALYSIS", queryVO.getFieldValueByFieldName("class6Analysis"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("class6FinishQty")), "CLASS6_FINISH_QTY", queryVO.getFieldValueByFieldName("class6FinishQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("isDelivery")), "IS_DELIVERY", queryVO.getFieldValueByFieldName("isDelivery"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("isRelease")), "IS_RELEASE", queryVO.getFieldValueByFieldName("isRelease"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("publishSuccessCount")), "PUBLISH_SUCCESS_COUNT", queryVO.getFieldValueByFieldName("publishSuccessCount"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("newestPublishTime")), "NEWEST_PUBLISH_TIME", queryVO.getFieldValueByFieldName("newestPublishTime"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("dataSource")), "DATA_SOURCE", queryVO.getFieldValueByFieldName("dataSource"));
    }


    @Override
    protected String getTypeCode() {
        return "0202";
    }


}
