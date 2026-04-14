package com.zlt.aps.lh.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.constant.FactoryConstant;
import com.zlt.aps.lh.component.OrderNoGenerator;
import com.zlt.aps.lh.api.domain.entity.LhMouldChangePlan;
import com.zlt.aps.lh.mapper.LhMouldChangePlanEntityMapper;
import com.zlt.aps.lh.service.ILhMouldChangePlanService;
import com.zlt.bill.common.controller.AbstractDocBizController;
import com.zlt.bill.common.service.IDocService;
import com.zlt.common.utils.PubUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jodd.util.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.collections4.CollectionUtils;

/**
* Copyright (c) 2022, All rights reserved。
* 文件名称：LhMouldChangePlanController.java
* 描    述：模具交替计划 控制层类
*@author APS Team
*@date 2026-04-01
*@version 1.0
*
 * 修改记录：
*     修改时间：...
*     修 改 人：...
*     修改内容：...
*/
@Slf4j
@Api(tags = "模具交替计划")
@RestController
@RequestMapping("/lhMouldChangePlan")
public class LhMouldChangePlanController extends AbstractDocBizController<LhMouldChangePlan> {

    @Autowired
    private ILhMouldChangePlanService lhMouldChangePlanService;

    @Resource
    private LhMouldChangePlanEntityMapper lhMouldChangePlanMapper;
    @Resource
    private OrderNoGenerator orderNoGenerator;

    /**
     * 查询模具交替计划列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody LhMouldChangePlan queryVO) {
        return super.list(queryVO);
    }

    /**
     * 保存
     */
    @Log(title = "ui.data.column.lhMouldChangePlan.modelName", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("保存")
    @PostMapping("/save")
    @Override
    public AjaxResult save(@RequestBody LhMouldChangePlan billVO){
        if (StringUtil.isBlank(billVO.getFactoryCode())) {
            billVO.setFactoryCode(FactoryConstant.DEFAULT_FACTORY_CODE);
        }
        if (billVO.getId() == null) {
            billVO.setOrderNo(orderNoGenerator.generateMouldChangeOrderNo(new Date()));
            billVO.setIsRelease(ApsConstant.NO_RELEASE);
            billVO.setMouldStatus(ApsConstant.FALSE);
        } else {
            LhMouldChangePlan origin = lhMouldChangePlanMapper.selectById(billVO.getId());
            if (origin != null) {
                billVO.setOrderNo(origin.getOrderNo());
                if (ApsConstant.IS_RELEASE.equals(origin.getIsRelease())) {
                    billVO.setIsRelease(ApsConstant.APS_STRING_4);
                    billVO.setMouldStatus(ApsConstant.FALSE);
                } else {
                    billVO.setIsRelease(origin.getIsRelease());
                    billVO.setMouldStatus(origin.getMouldStatus());
                }
            }
        }
        return super.save(billVO);
    }

    /**
     * 删除
     */
    @Log(title = "ui.data.column.lhMouldChangePlan.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除")
    @DeleteMapping("/remove")
    @Override
    public AjaxResult removeByIds(@RequestBody List<Long> ids){
        if (CollectionUtils.isNotEmpty(ids)) {
            QueryWrapper<LhMouldChangePlan> wrapper = new QueryWrapper<>();
            wrapper.in("ID", ids);
            wrapper.eq("IS_RELEASE", "1");
            List<LhMouldChangePlan> releasedList = lhMouldChangePlanMapper.selectList(wrapper);
            if (CollectionUtils.isNotEmpty(releasedList)) {
                String details = releasedList.stream()
                        .map(item -> String.format("%s/%s",
                                StringUtil.isNotBlank(item.getLhResultBatchNo()) ? item.getLhResultBatchNo() : "-",
                                StringUtil.isNotBlank(item.getOrderNo()) ? item.getOrderNo() : "-"))
                        .collect(Collectors.joining("; "));
                String msg = I18nUtil.getMessage("ui.data.alert.lhMouldChangePlan.releaseCannotDelete");
                msg = StringUtils.format(msg, details);
                return AjaxResult.error(msg);
            }
        }
        return super.removeByIds(ids);
    }

    /**
     * 获取模具交替计划详细信息
     */
    @ApiOperation("获取详细信息")
    @GetMapping(value = "/{billId}")
    @Override
    public LhMouldChangePlan getInfo(@PathVariable("billId") Long billId) {
        return super.getInfo(billId);
    }

    /**
     * 根据集合导入模具交替计划数据
     * @param importContext 导入上下文
     * @param updateSupport 已存在记录是否更新
     * @return 结果
     */
    @Log(title = "ui.data.column.lhMouldChangePlan.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    @Override
    public AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        return super.importData(importContext,updateSupport);
    }

    /**
     * 导出列表
     */
    @Log(title = "模具交替计划", businessType = BusinessType.EXPORT)
    @ApiOperation("导出数据")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody LhMouldChangePlan queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return super.exportData(queryVO, fileName, response);
    }

    @Override
    protected List<LhMouldChangePlan> listExportData(LhMouldChangePlan obj) {
        QueryWrapper<LhMouldChangePlan> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, obj);
        return lhMouldChangePlanMapper.selectList(wrapper);
    }

    @Override
    protected IDocService getDocService(){
        return lhMouldChangePlanService;
    }

    @Override
    protected String[] getQueryFormulas() {
        return lhMouldChangePlanService.getQueryFormulas();
    }

    /**
     * 条件拼接 - 所有数据库字段都支持查�?
     *
     * @param queryWrapper
     * @param queryVO
     */
    @Override
    protected void builderCondition(QueryWrapper<LhMouldChangePlan> queryWrapper, LhMouldChangePlan queryVO) {
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFactoryCode()), "FACTORY_CODE", queryVO.getFactoryCode());
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getLhResultBatchNo()), "LH_RESULT_BATCH_NO", queryVO.getLhResultBatchNo());
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getOrderNo()), "ORDER_NO", queryVO.getOrderNo());
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getPlanDate()), "PLAN_DATE", queryVO.getPlanDate());
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getPlanOrder()), "PLAN_ORDER", queryVO.getPlanOrder());
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getScheduleDate()), "SCHEDULE_DATE", queryVO.getScheduleDate());
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getLeftRightMould()), "LEFT_RIGHT_MOULD", queryVO.getLeftRightMould());
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getLhMachineCode()), "LH_MACHINE_CODE", queryVO.getLhMachineCode());
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getLhMachineName()), "LH_MACHINE_NAME", queryVO.getLhMachineName());
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getBeforeMaterialCode()), "BEFORE_MATERIAL_CODE", queryVO.getBeforeMaterialCode());
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getBeforeMaterialDesc()), "BEFORE_MATERIAL_DESC", queryVO.getBeforeMaterialDesc());
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getChangeMouldType()), "CHANGE_MOULD_TYPE", queryVO.getChangeMouldType());
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getAfterMaterialCode()), "AFTER_MATERIAL_CODE", queryVO.getAfterMaterialCode());
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getAfterMaterialDesc()), "AFTER_MATERIAL_DESC", queryVO.getAfterMaterialDesc());
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getMouldCode()), "MOULD_CODE", queryVO.getMouldCode());
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getIsRelease()), "IS_RELEASE", queryVO.getIsRelease());
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getMouldStatus()), "MOULD_STATUS", queryVO.getMouldStatus());
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getRemark()), "REMARK", queryVO.getRemark());

        // 计划日期区间查询 - 前端daterange会自动拆分出planDateStart和planDateEnd
        queryWrapper.ge(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("planDateStart")), "PLAN_DATE", queryVO.getFieldValueByFieldName("planDateStart"));
        if (PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("planDateEnd"))) {
            queryWrapper.le("PLAN_DATE", queryVO.getFieldValueByFieldName("planDateEnd"));
        }

        // 排程日期区间查询 - 前端daterange会自动拆分出scheduleDateStart和scheduleDateEnd
        queryWrapper.ge(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("scheduleDateStart")), "SCHEDULE_DATE", queryVO.getFieldValueByFieldName("scheduleDateStart"));
        if (PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("scheduleDateEnd"))) {
            queryWrapper.le("SCHEDULE_DATE", queryVO.getFieldValueByFieldName("scheduleDateEnd"));
        }
    }

    @Override
    protected String getTypeCode(){
        return "0114";
    }

    @Override
    protected String getOrderBy() {
        return "plan_date desc, create_time desc";
    }

    /**
     * 排程发布
     */
    @Log(title = "ui.data.column.lhMouldChangePlan.modelName", businessType = BusinessType.PUBLISH)
    @ApiOperation("排程发布")
    @PostMapping("/issueSchedule")
    public AjaxResult issueSchedule(@RequestBody List<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.param.error"));
        }
        return lhMouldChangePlanService.issueSchedule(ids);
    }

    /**
     * 按查询条件排程发布（仅支持单日排程日期）
     */
    @Log(title = "ui.data.column.lhMouldChangePlan.modelName", businessType = BusinessType.PUBLISH)
    @ApiOperation("按查询条件排程发布")
    @PostMapping("/issueScheduleByQuery")
    public AjaxResult issueScheduleByQuery(@RequestBody LhMouldChangePlan queryVO) {
        if (queryVO == null || PubUtil.isEmpty(queryVO.getScheduleDate())) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.param.error"));
        }
        // 只允许单日排程日期，不支持区间下发
        if (PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("scheduleDateStart"))
                || PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("scheduleDateEnd"))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.param.error"));
        }

        // 忽略前端的发布状态筛选，强制下发 未发布/待发布
        queryVO.setIsRelease(null);

        QueryWrapper<LhMouldChangePlan> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, queryVO);
        wrapper.in("IS_RELEASE", Arrays.asList(ApsConstant.NO_RELEASE, ApsConstant.WAIT_RELEASING));
        wrapper.select("ID");
        List<LhMouldChangePlan> list = lhMouldChangePlanMapper.selectList(wrapper);
        if (CollectionUtils.isEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.alert.lhMouldChangePlan.noData"));
        }
        List<Long> ids = list.stream().map(LhMouldChangePlan::getId).collect(Collectors.toList());
        return lhMouldChangePlanService.issueSchedule(ids);
    }

}




