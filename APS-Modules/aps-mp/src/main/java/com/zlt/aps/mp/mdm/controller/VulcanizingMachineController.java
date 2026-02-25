package com.zlt.aps.mp.mdm.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.maindata.mapper.VulcanizingMachineMapper;
import com.zlt.aps.maindata.service.IVulcanizingMachineService;
import com.zlt.aps.mp.api.domain.entity.VulcanizingMachine;
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
 * 文件名称：VulcanizingMachineController.java
 * 描    述：基础数据-硫化机档案 控制层类：....
 *
 * @author ZLT
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：ZLT
 * 修改内容：...
 * @date 2025-02-20
 */
@Slf4j
@RestController
@RequestMapping("/vulcanizingMachine")
@Api(tags = "基础数据-硫化机档案基础服务业务")
public class VulcanizingMachineController extends AbstractDocBizController<VulcanizingMachine> {

    @Autowired
    private VulcanizingMachineMapper vulcanizingMachineMapper;

    private final IVulcanizingMachineService vulcanizingMachineService;

    public VulcanizingMachineController(IVulcanizingMachineService vulcanizingMachineService) {
        this.vulcanizingMachineService = vulcanizingMachineService;
    }

    /**
     * 查询基础数据-硫化机档案列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody VulcanizingMachine queryVO) {
        this.startPage(this.getOrderBy());
        QueryWrapper<VulcanizingMachine> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, queryVO);
        List<VulcanizingMachine> mdmModelInfos = vulcanizingMachineMapper.selectList(wrapper);
        return getDataTable(mdmModelInfos);
    }

    @Override
    protected String getOrderBy() {
        return "create_time desc";
    }

    /**
     * 保存
     */
    @Log(title = "ui.data.column.VulcanizingMachine.modelName", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("保存")
    @PostMapping("/save")
    @Override
    public AjaxResult save(@RequestBody VulcanizingMachine billVO) {
        return super.save(billVO);
    }

    /**
     * 删除
     */
    @Log(title = "ui.data.column.VulcanizingMachine.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除")
    @DeleteMapping("/remove")
    @Override
    public AjaxResult removeByIds(@RequestBody List<Long> ids) {
        return super.removeByIds(ids);
    }


    /**
     * 获取基础数据-硫化机档案详细信息
     */
    @ApiOperation("获取详细信息")
    @GetMapping(value = "/{billId}")
    @Override
    public VulcanizingMachine getInfo(@PathVariable("billId") Long billId) {
        return super.getInfo(billId);
    }


    /**
     * 根据集合导入基础数据-硫化机档案数据
     *
     * @param importContext 导入上下文
     * @param updateSupport 已存在记录是否更新
     * @return 结果
     */
    @Log(title = "ui.data.column.VulcanizingMachine.modelName", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    @ApiOperation("导入数据")
    @Override
    public AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        return super.importData(importContext, updateSupport);
    }

    /**
     * 导出列表
     */
    @Log(title = "基础数据-硫化机档案", businessType = BusinessType.EXPORT)
    @PostMapping("/exportData/{fileName}")
    @ApiOperation("导入数据")
    @Override
    public byte[] exportData(@RequestBody VulcanizingMachine queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return super.exportData(queryVO, fileName, response);
    }

    @Override
    protected List<VulcanizingMachine> listExportData(VulcanizingMachine obj) {
        QueryWrapper<VulcanizingMachine> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, obj);
        return vulcanizingMachineMapper.selectList(wrapper);
    }

    /**
     * 条件拼接
     *
     * @param queryWrapper
     * @param queryVO
     */
    @Override
    protected void builderCondition(QueryWrapper<VulcanizingMachine> queryWrapper, VulcanizingMachine queryVO) {
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("factoryCode")), "FACTORY_CODE", queryVO.getFieldValueByFieldName("factoryCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("lineId")), "LINE_ID", queryVO.getFieldValueByFieldName("lineId"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("lineCode")), "LINE_CODE", queryVO.getFieldValueByFieldName("lineCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("vulcanizingMachineCode")), "VULCANIZING_MACHINE_CODE", queryVO.getFieldValueByFieldName("vulcanizingMachineCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("productTypeCode")), "PRODUCT_TYPE_CODE", queryVO.getFieldValueByFieldName("productTypeCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("mouldNum")), "MOULD_NUM", queryVO.getFieldValueByFieldName("mouldNum"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("mouldType")), "MOULD_TYPE", queryVO.getFieldValueByFieldName("mouldType"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("single")), "SINGLE", queryVO.getFieldValueByFieldName("single"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("isClosed")), "IS_CLOSED", queryVO.getFieldValueByFieldName("isClosed"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("dayStatus")), "DAY_STATUS", queryVO.getFieldValueByFieldName("dayStatus"));
    }

    @Override
    protected IDocService getDocService() {
        return vulcanizingMachineService;
    }

    @Override
    protected String getTypeCode() {
        return "0122";
    }
}
