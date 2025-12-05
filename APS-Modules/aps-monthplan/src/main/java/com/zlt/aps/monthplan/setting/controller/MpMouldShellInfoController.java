package com.zlt.aps.monthplan.setting.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.security.annotation.RequiresPermissions;
import com.zlt.aps.maindata.mapper.MpMouldShellInfoEntityMapper;
import com.zlt.aps.maindata.service.IMpMouldShellInfoService;
import com.zlt.aps.monthplan.api.domain.entity.MpMouldShellInfo;
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
 * 文件名称：MpMouldShellInfoController.java
 * 描    述：模壳台账 控制层类：....
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
@Api(tags = "模壳台账")
@RestController
@RequestMapping("/mpMouldShellInfo")
public class MpMouldShellInfoController extends AbstractDocBizController<MpMouldShellInfo> {

    @Autowired
    private IMpMouldShellInfoService mpMouldShellInfoService;

    @Autowired
    private MpMouldShellInfoEntityMapper entityMapper;

    /**
     * 查询模壳台账列表
     */
    @RequiresPermissions("monthplan:mpMouldShellInfo:list")
    @ApiOperation("查询列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody MpMouldShellInfo queryVO) {
        return super.list(queryVO);
    }

    @Override
    protected String getOrderBy() {
        return "create_time desc";
    }

    /**
     * 保存
     */
    @Log(title = "ui.data.column.mpMouldShellInfo.modelName", businessType = BusinessType.INSERT_OR_UPDATE)
    @RequiresPermissions("monthplan:mpMouldShellInfo:save")
    @ApiOperation("保存")
    @PostMapping("/save")
    @Override
    public AjaxResult save(@RequestBody MpMouldShellInfo billVO) {
        return super.save(billVO);
    }

    /**
     * 删除
     */
    @Log(title = "ui.data.column.mpMouldShellInfo.modelName", businessType = BusinessType.DELETE)
    @RequiresPermissions("monthplan:mpMouldShellInfo:remove")
    @ApiOperation("删除")
    @DeleteMapping("/remove")
    @Override
    public AjaxResult removeByIds(@RequestBody List<Long> ids) {
        return super.removeByIds(ids);
    }


    /**
     * 获取模壳台账详细信息
     */
    @RequiresPermissions("monthplan:mpMouldShellInfo:query")
    @ApiOperation("获取详细信息")
    @GetMapping(value = "/{billId}")
    @Override
    public MpMouldShellInfo getInfo(@PathVariable("billId") Long billId) {
        return super.getInfo(billId);
    }


    /**
     * 根据集合导入模壳台账数据
     *
     * @param importContext 导入上下文
     * @param updateSupport 已存在记录是否更新
     * @return 结果
     */
    @RequiresPermissions("monthplan:mpMouldShellInfo:import")
    @Log(title = "ui.data.column.mpMouldShellInfo.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    @Override
    public AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        return super.importData(importContext, updateSupport);
    }

    /**
     * 导出列表
     */
    @RequiresPermissions("monthplan:mpMouldShellInfo:export")
    @Log(title = "模壳台账", businessType = BusinessType.EXPORT)
    @ApiOperation("导入数据")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody MpMouldShellInfo queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return super.exportData(queryVO, fileName, response);
    }

    @Override
    protected List<MpMouldShellInfo> listExportData(MpMouldShellInfo obj) {
        QueryWrapper<MpMouldShellInfo> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, obj);
        return entityMapper.selectList(wrapper);
    }

    @Override
    protected IDocService getDocService() {
        return mpMouldShellInfoService;
    }

    /**
     * 条件拼接
     *
     * @param queryWrapper
     * @param queryVO
     */
    @Override
    protected void builderCondition(QueryWrapper<MpMouldShellInfo> queryWrapper, MpMouldShellInfo queryVO) {
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("factoryCode")), "FACTORY_CODE", queryVO.getFieldValueByFieldName("factoryCode"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("moldModelCode")), "MOLD_MODEL_CODE", queryVO.getFieldValueByFieldName("moldModelCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("qty")), "QTY", queryVO.getFieldValueByFieldName("qty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("machineQty")), "MACHINE_QTY", queryVO.getFieldValueByFieldName("machineQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("onHandQty")), "ON_HAND_QTY", queryVO.getFieldValueByFieldName("onHandQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("outBoundQty")), "OUT_BOUND_QTY", queryVO.getFieldValueByFieldName("outBoundQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("outBoundPlanQty")), "OUT_BOUND_PLAN_QTY", queryVO.getFieldValueByFieldName("outBoundPlanQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("deplaneQty")), "DEPLANE_QTY", queryVO.getFieldValueByFieldName("deplaneQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("forceOutBoundQty")), "FORCE_OUT_BOUND_QTY", queryVO.getFieldValueByFieldName("forceOutBoundQty"));
    }

    @Override
    protected String getTypeCode() {
        return "MP0208";
    }

    /**
     * 抓取MES数据
     *
     * @return 结果
     */
    @ApiOperation("抓取MES数据")
    @PostMapping("/mesCapture")
    public AjaxResult mesCapture() {
        return mpMouldShellInfoService.mesCapture();
    }

}
