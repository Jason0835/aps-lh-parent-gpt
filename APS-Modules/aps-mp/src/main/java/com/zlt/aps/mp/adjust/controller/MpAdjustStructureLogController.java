package com.zlt.aps.mp.adjust.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zlt.aps.mp.adjust.mapper.MpAdjustStructureLogEntityMapper;
import com.zlt.aps.mp.adjust.service.IMpAdjustStructureLogService;
import com.zlt.aps.monthplan.api.domain.entity.MpAdjustStructureLog;
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


import com.ruoyi.common.core.web.page.TableDataInfo;

import com.zlt.bill.common.controller.AbstractDocBizController;
import com.zlt.bill.common.service.IDocService ;

/**
* Copyright (c) 2022, All rights reserved。
* 文件名称：MpAdjustStructureLogController.java
* 描    述：调整-操作日志 控制层类：....
*@author zlt
*@date 2025-12-19
*@version 1.0
*
 *  修改记录：
*     修改时间：...
*     修 改 人：zlt
*     修改内容：...
*/
@Slf4j
@Api(tags = "调整-操作日志")
@RestController
@RequestMapping("/mpAdjustStructureLog")
public class MpAdjustStructureLogController extends AbstractDocBizController<MpAdjustStructureLog> {

    @Autowired
    private IMpAdjustStructureLogService mpAdjustStructureLogService;

    @Autowired
    private MpAdjustStructureLogEntityMapper entityMapper;

    /**
     * 查询调整-操作日志列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody MpAdjustStructureLog queryVO) {
        return super.list(queryVO);
    }

    @Override
    protected String getOrderBy() {
        return "create_time desc";
    }

    /**
     * 保存
     */
    @Log(title = "ui.data.column.mpAdjustStructureLog.modelName", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("保存")
    @PostMapping("/save")
    @Override
    public AjaxResult save(@RequestBody MpAdjustStructureLog billVO){
        return super.save(billVO);
    }

    /**
     * 删除
     */
    @Log(title = "ui.data.column.mpAdjustStructureLog.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除")
    @DeleteMapping("/remove")
    @Override
    public AjaxResult removeByIds(@RequestBody List<Long> ids){
        return super.removeByIds(ids);
    }


    /**
     * 获取调整-操作日志详细信息
     */
    @ApiOperation("获取详细信息")
    @GetMapping(value = "/{billId}")
    @Override
    public MpAdjustStructureLog getInfo(@PathVariable("billId") Long billId) {
        return super.getInfo(billId);
    }


    /**
     * 根据集合导入调整-操作日志数据
     * @param importContext 导入上下文
     * @param updateSupport 已存在记录是否更新
     * @return 结果
     */
    @Log(title = "ui.data.column.mpAdjustStructureLog.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    @Override
    public AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        return super.importData(importContext,updateSupport);
    }

    /**
     * 导出列表
     */
    @Log(title = "调整-操作日志", businessType = BusinessType.EXPORT)
    @ApiOperation("导出数据")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody MpAdjustStructureLog queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return super.exportData(queryVO, fileName, response);
    }

    @Override
    protected List<MpAdjustStructureLog> listExportData(MpAdjustStructureLog obj) {
        QueryWrapper<MpAdjustStructureLog> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, obj);
        return entityMapper.selectList(wrapper);
    }

    @Override
    protected IDocService getDocService(){
        return mpAdjustStructureLogService;
    }

    /**
     * 条件拼接
     *
     * @param queryWrapper
     * @param queryVO
     */
    @Override
    protected void builderCondition(QueryWrapper<MpAdjustStructureLog> queryWrapper, MpAdjustStructureLog queryVO) {
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("structureName")), "STRUCTURE_NAME", queryVO.getFieldValueByFieldName("structureName"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("scheduledMachines")), "SCHEDULED_MACHINES", queryVO.getFieldValueByFieldName("scheduledMachines"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("mesMaterialCode")), "MES_MATERIAL_CODE", queryVO.getFieldValueByFieldName("mesMaterialCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("materialCode")), "MATERIAL_CODE", queryVO.getFieldValueByFieldName("materialCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("materialDesc")), "MATERIAL_DESC", queryVO.getFieldValueByFieldName("materialDesc"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("hasSpecialMaterial")), "HAS_SPECIAL_MATERIAL", queryVO.getFieldValueByFieldName("hasSpecialMaterial"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("previousNetQty")), "PREVIOUS_NET_QTY", queryVO.getFieldValueByFieldName("previousNetQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("beforeNetQtyChange")), "BEFORE_NET_QTY_CHANGE", queryVO.getFieldValueByFieldName("beforeNetQtyChange"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("beforeMonthScheduledQty")), "BEFORE_MONTH_SCHEDULED_QTY", queryVO.getFieldValueByFieldName("beforeMonthScheduledQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("beforePendingQty")), "BEFORE_PENDING_QTY", queryVO.getFieldValueByFieldName("beforePendingQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("beforeConfirmAdjustQty")), "BEFORE_CONFIRM_ADJUST_QTY", queryVO.getFieldValueByFieldName("beforeConfirmAdjustQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("beforeAdjustPriority")), "BEFORE_ADJUST_PRIORITY", queryVO.getFieldValueByFieldName("beforeAdjustPriority"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("beforeActualAdjustQty")), "BEFORE_ACTUAL_ADJUST_QTY", queryVO.getFieldValueByFieldName("beforeActualAdjustQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("beforeAdjustReason")), "BEFORE_ADJUST_REASON", queryVO.getFieldValueByFieldName("beforeAdjustReason"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("afterNetQtyChange")), "AFTER_NET_QTY_CHANGE", queryVO.getFieldValueByFieldName("afterNetQtyChange"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("afterMonthScheduledQty")), "AFTER_MONTH_SCHEDULED_QTY", queryVO.getFieldValueByFieldName("afterMonthScheduledQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("afterPendingQty")), "AFTER_PENDING_QTY", queryVO.getFieldValueByFieldName("afterPendingQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("afterConfirmAdjustQty")), "AFTER_CONFIRM_ADJUST_QTY", queryVO.getFieldValueByFieldName("afterConfirmAdjustQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("afterAdjustPriority")), "AFTER_ADJUST_PRIORITY", queryVO.getFieldValueByFieldName("afterAdjustPriority"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("afterActualAdjustQty")), "AFTER_ACTUAL_ADJUST_QTY", queryVO.getFieldValueByFieldName("afterActualAdjustQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("afterAdjustReason")), "AFTER_ADJUST_REASON", queryVO.getFieldValueByFieldName("afterAdjustReason"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("translationDirection")), "TRANSLATION_DIRECTION", queryVO.getFieldValueByFieldName("translationDirection"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("isTranslationDirection")), "IS_TRANSLATION_DIRECTION", queryVO.getFieldValueByFieldName("isTranslationDirection"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("action")), "ACTION", queryVO.getFieldValueByFieldName("action"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("operator")), "OPERATOR", queryVO.getFieldValueByFieldName("operator"));
    }


    @Override
    protected String getTypeCode(){
        return "MP0808";
    }


}
