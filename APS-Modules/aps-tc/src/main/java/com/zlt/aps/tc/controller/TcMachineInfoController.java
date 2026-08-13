package com.zlt.aps.tc.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.common.core.utils.MachineShiftDictUtil;
import com.zlt.aps.constant.FactoryConstant;
import com.zlt.aps.tc.api.domain.entity.TcMachineInfo;
import com.zlt.aps.tc.mapper.TcMachineInfoMapper;
import com.zlt.aps.tc.service.ITcMachineInfoService;
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
import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：TcMachineInfoController.java
 * 描    述：胎侧机台基础表 控制层类
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2026-07-07
 */
@Slf4j
@Api(tags = "胎侧机台基础表")
@RestController
@RequestMapping("/tcMachineInfo")
public class TcMachineInfoController extends AbstractDocBizController<TcMachineInfo> {

    @Autowired
    private ITcMachineInfoService tcMachineInfoService;

    @Resource
    private TcMachineInfoMapper tcMachineInfoMapper;

    @ApiOperation("查询列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody TcMachineInfo queryVO) {
        return super.list(queryVO);
    }

    @Log(title = "ui.data.column.tc.MachineInfo.modelName", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("保存")
    @PostMapping("/save")
    @Override
    public AjaxResult save(@RequestBody TcMachineInfo billVO) {
        if (StringUtil.isBlank(billVO.getFactoryCode())) {
            billVO.setFactoryCode(FactoryConstant.DEFAULT_FACTORY_CODE);
        }
        return super.save(billVO);
    }

    @Log(title = "ui.data.column.tc.MachineInfo.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除")
    @DeleteMapping("/remove")
    @Override
    public AjaxResult removeByIds(@RequestBody List<Long> ids) {
        return super.removeByIds(ids);
    }

    @ApiOperation("获取详细信息")
    @GetMapping(value = "/{id}")
    @Override
    public TcMachineInfo getInfo(@PathVariable("id") Long id) {
        return super.getInfo(id);
    }

    @ApiOperation("校验唯一性")
    @PostMapping("/checkUnique")
    public String checkUnique(@RequestBody TcMachineInfo query) {
        return tcMachineInfoService.checkUnique(query);
    }

    @Log(title = "ui.data.column.tc.MachineInfo.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    @Override
    public AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        return super.importData(importContext, updateSupport);
    }

    @Log(title = "ui.data.column.tc.MachineInfo.modelName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出数据")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody TcMachineInfo queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return super.exportData(queryVO, fileName, response);
    }

    @Override
    protected List<TcMachineInfo> listExportData(TcMachineInfo obj) {
        QueryWrapper<TcMachineInfo> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, obj);
        // 与列表页排序保持一致，避免导出数据顺序与页面不一致
        wrapper.last("ORDER BY " + this.getOrderBy(obj));
        List<TcMachineInfo> list = tcMachineInfoMapper.selectList(wrapper);
        // 开机班次：导出时把数据库字典值(01,02)转回班次名称(夜班,早班)
        list.forEach(entity -> entity.setOpenShiftCode(MachineShiftDictUtil.valuesToLabels(entity.getOpenShiftCode())));
        return list;
    }

    @Override
    protected IDocService getDocService() {
        return tcMachineInfoService;
    }

    @Override
    protected void builderCondition(QueryWrapper<TcMachineInfo> queryWrapper, TcMachineInfo queryVO) {
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("factoryCode")), "FACTORY_CODE", queryVO.getFieldValueByFieldName("factoryCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("machineCode")), "MACHINE_CODE", queryVO.getFieldValueByFieldName("machineCode"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("machineName")), "MACHINE_NAME", queryVO.getFieldValueByFieldName("machineName"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("maxCapacity")), "MAX_CAPACITY", queryVO.getFieldValueByFieldName("maxCapacity"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("openShiftCode")), "OPEN_SHIFT_CODE", queryVO.getFieldValueByFieldName("openShiftCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("machineStatus")), "MACHINE_STATUS", queryVO.getFieldValueByFieldName("machineStatus"));
    }

    @Override
    protected String getTypeCode() {
        return "TC0903";
    }

    @Override
    protected String getOrderBy() {
        return "create_time desc";
    }
}
