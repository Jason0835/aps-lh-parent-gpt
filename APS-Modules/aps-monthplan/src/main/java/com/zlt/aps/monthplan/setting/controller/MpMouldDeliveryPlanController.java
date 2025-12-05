package com.zlt.aps.monthplan.setting.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.security.annotation.RequiresPermissions;
import com.zlt.aps.maindata.mapper.MpMouldDeliveryPlanEntityMapper;
import com.zlt.aps.maindata.service.IMpMouldDeliveryPlanService;
import com.zlt.aps.monthplan.api.domain.entity.MpMouldDeliveryPlan;
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
 * 文件名称：MpMouldDeliveryPlanController.java
 * 描    述：模具到货计划 控制层类：....
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-12-05
 */
@Slf4j
@Api(tags = "模具到货计划")
@RestController
@RequestMapping("/mpMouldDeliveryPlan")
public class MpMouldDeliveryPlanController extends AbstractDocBizController<MpMouldDeliveryPlan> {

    @Autowired
    private IMpMouldDeliveryPlanService mpMouldDeliveryPlanService;

    @Autowired
    private MpMouldDeliveryPlanEntityMapper entityMapper;

    /**
     * 查询模具到货计划列表
     */
    @RequiresPermissions("monthplan:mpMouldDeliveryPlan:list")
    @ApiOperation("查询列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody MpMouldDeliveryPlan queryVO) {
        return super.list(queryVO);
    }

    @Override
    protected String getOrderBy() {
        return "create_time desc";
    }

    /**
     * 保存
     */
    @Log(title = "ui.data.column.mpMouldDeliveryPlan.modelName", businessType = BusinessType.INSERT_OR_UPDATE)
    @RequiresPermissions("monthplan:mpMouldDeliveryPlan:save")
    @ApiOperation("保存")
    @PostMapping("/save")
    @Override
    public AjaxResult save(@RequestBody MpMouldDeliveryPlan billVO) {
        return super.save(billVO);
    }

    /**
     * 删除
     */
    @Log(title = "ui.data.column.mpMouldDeliveryPlan.modelName", businessType = BusinessType.DELETE)
    @RequiresPermissions("monthplan:mpMouldDeliveryPlan:remove")
    @ApiOperation("删除")
    @DeleteMapping("/remove")
    @Override
    public AjaxResult removeByIds(@RequestBody List<Long> ids) {
        return super.removeByIds(ids);
    }


    /**
     * 获取模具到货计划详细信息
     */
    @RequiresPermissions("monthplan:mpMouldDeliveryPlan:query")
    @ApiOperation("获取详细信息")
    @GetMapping(value = "/{billId}")
    @Override
    public MpMouldDeliveryPlan getInfo(@PathVariable("billId") Long billId) {
        return super.getInfo(billId);
    }


    /**
     * 根据集合导入模具到货计划数据
     *
     * @param importContext 导入上下文
     * @param updateSupport 已存在记录是否更新
     * @return 结果
     */
    @RequiresPermissions("monthplan:mpMouldDeliveryPlan:import")
    @Log(title = "ui.data.column.mpMouldDeliveryPlan.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    @Override
    public AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        return super.importData(importContext, updateSupport);
    }

    /**
     * 导出列表
     */
    @RequiresPermissions("monthplan:mpMouldDeliveryPlan:export")
    @Log(title = "模具到货计划", businessType = BusinessType.EXPORT)
    @ApiOperation("导入数据")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody MpMouldDeliveryPlan queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return super.exportData(queryVO, fileName, response);
    }

    @Override
    protected List<MpMouldDeliveryPlan> listExportData(MpMouldDeliveryPlan obj) {
        QueryWrapper<MpMouldDeliveryPlan> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, obj);
        return entityMapper.selectList(wrapper);
    }

    @Override
    protected IDocService getDocService() {
        return mpMouldDeliveryPlanService;
    }

    /**
     * 条件拼接
     *
     * @param queryWrapper
     * @param queryVO
     */
    @Override
    protected void builderCondition(QueryWrapper<MpMouldDeliveryPlan> queryWrapper, MpMouldDeliveryPlan queryVO) {
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("factoryCode")), "FACTORY_CODE", queryVO.getFieldValueByFieldName("factoryCode"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("mouldCode")), "MOULD_CODE", queryVO.getFieldValueByFieldName("mouldCode"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("materialCode")), "MATERIAL_CODE", queryVO.getFieldValueByFieldName("materialCode"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("mesMaterialCode")), "MES_MATERIAL_CODE", queryVO.getFieldValueByFieldName("mesMaterialCode"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("materialDesc")), "MATERIAL_DESC", queryVO.getFieldValueByFieldName("materialDesc"));

        queryWrapper.ge(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("shipmentDateStartTime")), "SHIPMENT_DATE", queryVO.getFieldValueByFieldName("shipmentDateStartTime"));
        queryWrapper.le(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("shipmentDateEndTime")), "SHIPMENT_DATE", queryVO.getFieldValueByFieldName("shipmentDateEndTime"));

        queryWrapper.ge(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("boardingDateStartTime")), "BOARDING_DATE", queryVO.getFieldValueByFieldName("boardingDateStartTime"));
        queryWrapper.le(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("boardingDateEndTime")), "BOARDING_DATE", queryVO.getFieldValueByFieldName("boardingDateEndTime"));
    }


    @Override
    protected String getTypeCode() {
        return "MP0203";
    }


}
