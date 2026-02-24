package com.zlt.aps.monthplan.setting.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.constant.FactoryConstant;
import com.zlt.aps.maindata.mapper.MdmDevicePlanShutEntityMapper;
import com.zlt.aps.maindata.service.IMdmDevicePlanShutService;
import com.zlt.aps.monthplan.api.domain.entity.MdmDevicePlanShut;
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
 * 文件名称：MdmDevicePlanShutController.java
 * 描    述：0106基础数据_设备计划停机 控制层类：....
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-12-04
 */
@Slf4j
@Api(tags = "0106基础数据_设备计划停机")
@RestController
@RequestMapping("/mdmDevicePlanShut")
public class MdmDevicePlanShutController extends AbstractDocBizController<MdmDevicePlanShut> {

    @Autowired
    private IMdmDevicePlanShutService mdmDevicePlanShutService;

    @Autowired
    private MdmDevicePlanShutEntityMapper entityMapper;

    /**
     * 查询0106基础数据_设备计划停机列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody MdmDevicePlanShut queryVO) {
        return super.list(queryVO);
    }

    @Override
    protected String getOrderBy() {
        return "create_time desc";
    }

    /**
     * 保存
     */
    @Log(title = "ui.data.column.mdmDevicePlanShut.modelName", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("保存")
    @PostMapping("/save")
    @Override
    public AjaxResult save(@RequestBody MdmDevicePlanShut billVO) {
        billVO.setFactoryCode(FactoryConstant.DEFAULT_FACTORY_CODE);
        return super.save(billVO);
    }

    /**
     * 删除
     */
    @Log(title = "ui.data.column.mdmDevicePlanShut.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除")
    @DeleteMapping("/remove")
    @Override
    public AjaxResult removeByIds(@RequestBody List<Long> ids) {
        return super.removeByIds(ids);
    }


    /**
     * 获取0106基础数据_设备计划停机详细信息
     */
    @ApiOperation("获取详细信息")
    @GetMapping(value = "/{billId}")
    @Override
    public MdmDevicePlanShut getInfo(@PathVariable("billId") Long billId) {
        return super.getInfo(billId);
    }


    /**
     * 根据集合导入0106基础数据_设备计划停机数据
     *
     * @param importContext 导入上下文
     * @param updateSupport 已存在记录是否更新
     * @return 结果
     */
    @Log(title = "ui.data.column.mdmDevicePlanShut.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    @Override
    public AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        return super.importData(importContext, updateSupport);
    }

    /**
     * 导出列表
     */
    @Log(title = "0106基础数据_设备计划停机", businessType = BusinessType.EXPORT)
    @ApiOperation("导入数据")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody MdmDevicePlanShut queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return super.exportData(queryVO, fileName, response);
    }

    @Override
    protected List<MdmDevicePlanShut> listExportData(MdmDevicePlanShut obj) {
        QueryWrapper<MdmDevicePlanShut> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, obj);
        return entityMapper.selectList(wrapper);
    }

    @Override
    protected IDocService getDocService() {
        return mdmDevicePlanShutService;
    }

    /**
     * 条件拼接
     *
     * @param queryWrapper
     * @param queryVO
     */
    @Override
    protected void builderCondition(QueryWrapper<MdmDevicePlanShut> queryWrapper, MdmDevicePlanShut queryVO) {
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("factoryCode")), "FACTORY_CODE", queryVO.getFieldValueByFieldName("factoryCode"));
//        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("procCode")), "PROC_CODE", queryVO.getFieldValueByFieldName("procCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("machineType")), "MACHINE_TYPE", queryVO.getFieldValueByFieldName("machineType"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("machineCode")), "MACHINE_CODE", queryVO.getFieldValueByFieldName("machineCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("machineStopType")), "MACHINE_STOP_TYPE", queryVO.getFieldValueByFieldName("machineStopType"));
        queryWrapper.ge(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("beginDate")), "BEGIN_DATE", queryVO.getFieldValueByFieldName("beginDate"));
        queryWrapper.le(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("endDate")), "END_DATE", queryVO.getFieldValueByFieldName("endDate"));
    }

    @Override
    protected String getTypeCode() {
        return "MDM0106";
    }


}
