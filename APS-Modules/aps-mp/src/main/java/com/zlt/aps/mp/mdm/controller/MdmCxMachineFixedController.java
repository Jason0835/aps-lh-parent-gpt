package com.zlt.aps.mp.mdm.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.maindata.mapper.MdmCxMachineFixedEntityMapper;
import com.zlt.aps.maindata.service.IMdmCxMachineFixedService;
import com.zlt.aps.mp.api.domain.entity.MdmCxMachineFixed;
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
 * 文件名称：MdmCxMachineFixedController.java
 * 描    述：成型固定机台 控制层类：....
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-12-12
 */
@Slf4j
@Api(tags = "成型固定机台")
@RestController
@RequestMapping("/mdmCxMachineFixed")
public class MdmCxMachineFixedController extends AbstractDocBizController<MdmCxMachineFixed> {

    @Autowired
    private IMdmCxMachineFixedService mdmCxMachineFixedService;

    @Autowired
    private MdmCxMachineFixedEntityMapper entityMapper;

    /**
     * 查询成型固定机台列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody MdmCxMachineFixed queryVO) {
        return super.list(queryVO);
    }

    @Override
    protected String getOrderBy() {
        return "create_time desc";
    }

    /**
     * 保存
     */
    @Log(title = "ui.data.column.mdmCxMachineFixed.modelName", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("保存")
    @PostMapping("/save")
    @Override
    public AjaxResult save(@RequestBody MdmCxMachineFixed billVO) {
        return super.save(billVO);
    }

    /**
     * 删除
     */
    @Log(title = "ui.data.column.mdmCxMachineFixed.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除")
    @DeleteMapping("/remove")
    @Override
    public AjaxResult removeByIds(@RequestBody List<Long> ids) {
        return super.removeByIds(ids);
    }


    /**
     * 获取成型固定机台详细信息
     */
    @ApiOperation("获取详细信息")
    @GetMapping(value = "/{billId}")
    @Override
    public MdmCxMachineFixed getInfo(@PathVariable("billId") Long billId) {
        return super.getInfo(billId);
    }


    /**
     * 根据集合导入成型固定机台数据
     *
     * @param importContext 导入上下文
     * @param updateSupport 已存在记录是否更新
     * @return 结果
     */
    @Log(title = "ui.data.column.mdmCxMachineFixed.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    @Override
    public AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        return super.importData(importContext, updateSupport);
    }

    /**
     * 导出列表
     */
    @Log(title = "成型固定机台", businessType = BusinessType.EXPORT)
    @ApiOperation("导入数据")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody MdmCxMachineFixed queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return super.exportData(queryVO, fileName, response);
    }

    @Override
    protected List<MdmCxMachineFixed> listExportData(MdmCxMachineFixed obj) {
        QueryWrapper<MdmCxMachineFixed> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, obj);
        return entityMapper.selectList(wrapper);
    }

    @Override
    protected IDocService getDocService() {
        return mdmCxMachineFixedService;
    }

    /**
     * 条件拼接
     *
     * @param queryWrapper
     * @param queryVO
     */
    @Override
    protected void builderCondition(QueryWrapper<MdmCxMachineFixed> queryWrapper, MdmCxMachineFixed queryVO) {
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("factoryCode")), "FACTORY_CODE", queryVO.getFieldValueByFieldName("factoryCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("cxMachineCode")), "CX_MACHINE_CODE", queryVO.getFieldValueByFieldName("cxMachineCode"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("fixedStructure1")), "FIXED_STRUCTURE1", queryVO.getFieldValueByFieldName("fixedStructure1"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("fixedStructure2")), "FIXED_STRUCTURE2", queryVO.getFieldValueByFieldName("fixedStructure2"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("fixedStructure3")), "FIXED_STRUCTURE3", queryVO.getFieldValueByFieldName("fixedStructure3"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("fixedMaterialCode")), "FIXED_MATERIAL_CODE", queryVO.getFieldValueByFieldName("fixedMaterialCode"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("disableStructure")), "DISABLE_STRUCTURE", queryVO.getFieldValueByFieldName("disableStructure"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("disableMaterialCode")), "DISABLE_MATERIAL_CODE", queryVO.getFieldValueByFieldName("disableMaterialCode"));
    }

    @Override
    protected String getTypeCode() {
        return "MDM0133";
    }
}
