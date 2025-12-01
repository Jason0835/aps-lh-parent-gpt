package com.zlt.aps.tm.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.tm.api.domain.entity.TmGlueMachineReal;
import com.zlt.aps.tm.api.domain.entity.TmMachineInfo;
import com.zlt.aps.tm.mapper.TmGlueMachineRealEntityMapper;
import com.zlt.aps.tm.mapper.TmMachineInfoMapper;
import com.zlt.aps.tm.service.ITmGlueMachineRealService;
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
 * 文件名称：TmGlueMachineRealController.java
 * 描    述：胎面胶料与机台关系 控制层类：....
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-07-08
 */
@Slf4j
@Api(tags = "胎面胶料与机台关系")
@RestController
@RequestMapping("/tmGlueMachineReal")
public class TmGlueMachineRealController extends AbstractDocBizController<TmGlueMachineReal> {

    @Autowired
    private ITmGlueMachineRealService tmGlueMachineRealService;

    @Autowired
    private TmGlueMachineRealEntityMapper entityMapper;

    @Autowired
    private TmMachineInfoMapper tmMachineInfoMapper;

    /**
     * 查询胎面胶料与机台关系列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody TmGlueMachineReal queryVO) {
        return super.list(queryVO);
    }

    @Override
    protected String getOrderBy() {
        return "create_time desc";
    }

    /**
     * 保存
     */
    @Log(title = "ui.data.column.tmGlueMachineReal.modelName", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("保存")
    @PostMapping("/save")
    @Override
    public AjaxResult save(@RequestBody TmGlueMachineReal billVO) {
        // 查询对应ID的机台，回写机台名称
        Long machineId = billVO.getMachineId();
        TmMachineInfo machineInfo = tmMachineInfoMapper.selectMachineInfoById(machineId);
        if (machineInfo == null) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.alert.tmGlueMachineReal.machineNotExist"));
        }
        billVO.setMachineName(machineInfo.getMachineName());
        return super.save(billVO);
    }

    /**
     * 删除
     */
    @Log(title = "ui.data.column.tmGlueMachineReal.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除")
    @DeleteMapping("/remove")
    @Override
    public AjaxResult removeByIds(@RequestBody List<Long> ids) {
        return super.removeByIds(ids);
    }


    /**
     * 获取胎面胶料与机台关系详细信息
     */
    @ApiOperation("获取详细信息")
    @GetMapping(value = "/{billId}")
    @Override
    public TmGlueMachineReal getInfo(@PathVariable("billId") Long billId) {
        return super.getInfo(billId);
    }


    /**
     * 根据集合导入胎面胶料与机台关系数据
     *
     * @param importContext 导入上下文
     * @param updateSupport 已存在记录是否更新
     * @return 结果
     */
    @Log(title = "ui.data.column.tmGlueMachineReal.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    @Override
    public AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        return super.importData(importContext, updateSupport);
    }

    /**
     * 导出列表
     */
    @Log(title = "胎面胶料与机台关系", businessType = BusinessType.EXPORT)
    @ApiOperation("导入数据")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody TmGlueMachineReal queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return super.exportData(queryVO, fileName, response);
    }

    @Override
    protected List<TmGlueMachineReal> listExportData(TmGlueMachineReal obj) {
        QueryWrapper<TmGlueMachineReal> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, obj);
        return entityMapper.selectList(wrapper);
    }

    @Override
    protected IDocService getDocService() {
        return tmGlueMachineRealService;
    }

    /**
     * 条件拼接
     *
     * @param queryWrapper
     * @param queryVO
     */
    @Override
    protected void builderCondition(QueryWrapper<TmGlueMachineReal> queryWrapper, TmGlueMachineReal queryVO) {
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("glueCode")), "GLUE_CODE", queryVO.getFieldValueByFieldName("glueCode"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("machineName")), "MACHINE_NAME", queryVO.getFieldValueByFieldName("machineName"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("machineClass")), "MACHINE_CLASS", queryVO.getFieldValueByFieldName("machineClass"));
    }


    @Override
    protected String getTypeCode() {
        return "TM0100";
    }


}
