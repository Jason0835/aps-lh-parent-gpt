package com.zlt.aps.monthplan.raw.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.common.security.annotation.RequiresPermissions;
import com.zlt.aps.maindata.mapper.RawWarningConfigEntityMapper;
import com.zlt.aps.maindata.service.IRawWarningConfigService;
import com.zlt.aps.monthplan.api.domain.entity.RawWarningConfig;
import com.zlt.aps.monthplan.raw.service.IRawWarningService;
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
import java.util.Map;


import com.ruoyi.common.core.web.page.TableDataInfo;

import com.zlt.bill.common.controller.AbstractDocBizController;
import com.zlt.bill.common.service.IDocService ;

/**
* Copyright (c) 2022, All rights reserved。
* 文件名称：RawWarningConfigController.java
* 描    述：原材料预警配置 控制层类：....
*@author zlt
*@date 2025-12-17
*@version 1.0
*
 *  修改记录：
*     修改时间：...
*     修 改 人：zlt
*     修改内容：...
*/
@Slf4j
@Api(tags = "原材料预警配置")
@RestController
@RequestMapping("/rawWarningConfig")
public class RawWarningConfigController extends AbstractDocBizController<RawWarningConfig> {

    @Autowired
    private IRawWarningConfigService rawWarningConfigService;

    @Autowired
    private RawWarningConfigEntityMapper entityMapper;


    /**
     * 查询原材料预警配置列表
     */
    @RequiresPermissions( "maindata:rawWarningConfig:list")
    @ApiOperation("查询列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody RawWarningConfig queryVO) {
        return super.list(queryVO);
    }

    @Override
    protected String getOrderBy() {
        return "create_time desc";
    }

    /**
     * 保存
     */
    @Log(title = "ui.data.column.rawWarningConfig.modelName", businessType = BusinessType.INSERT_OR_UPDATE)
    @RequiresPermissions( "maindata:rawWarningConfig:save")
    @ApiOperation("保存")
    @PostMapping("/save")
    @Override
    public AjaxResult save(@RequestBody RawWarningConfig billVO){
        return super.save(billVO);
    }

    /**
     * 删除
     */
    @Log(title = "ui.data.column.rawWarningConfig.modelName", businessType = BusinessType.DELETE)
    @RequiresPermissions( "maindata:rawWarningConfig:remove")
    @ApiOperation("删除")
    @DeleteMapping("/remove")
    @Override
    public AjaxResult removeByIds(@RequestBody List<Long> ids){
        return super.removeByIds(ids);
    }


    /**
     * 获取原材料预警配置详细信息
     */
    @RequiresPermissions( "maindata:rawWarningConfig:query")
    @ApiOperation("获取详细信息")
    @GetMapping(value = "/{billId}")
    @Override
    public RawWarningConfig getInfo(@PathVariable("billId") Long billId) {
        return super.getInfo(billId);
    }


    /**
     * 根据集合导入原材料预警配置数据
     * @param importContext 导入上下文
     * @param updateSupport 已存在记录是否更新
     * @return 结果
     */
    @RequiresPermissions( "maindata:rawWarningConfig:import")
    @Log(title = "ui.data.column.rawWarningConfig.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    @Override
    public AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        return super.importData(importContext,updateSupport);
    }

    /**
     * 导出列表
     */
    @RequiresPermissions( "maindata:rawWarningConfig:export")
    @Log(title = "原材料预警配置", businessType = BusinessType.EXPORT)
    @ApiOperation("导入数据")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody RawWarningConfig queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return super.exportData(queryVO, fileName, response);
    }

    @Override
    protected List<RawWarningConfig> listExportData(RawWarningConfig obj) {
        QueryWrapper<RawWarningConfig> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, obj);
        return entityMapper.selectList(wrapper);
    }

    @Override
    protected IDocService getDocService(){
        return rawWarningConfigService;
    }

    /**
     * 条件拼接
     *
     * @param queryWrapper
     * @param queryVO
     */
    @Override
    protected void builderCondition(QueryWrapper<RawWarningConfig> queryWrapper, RawWarningConfig queryVO) {
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("factoryCode")), "FACTORY_CODE", queryVO.getFieldValueByFieldName("factoryCode"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("materialCode")), "MATERIAL_CODE", queryVO.getFieldValueByFieldName("materialCode"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("materialDesc")), "MATERIAL_DESC", queryVO.getFieldValueByFieldName("materialDesc"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("warningType")), "WARNING_TYPE", queryVO.getFieldValueByFieldName("warningType"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("deviationUpper")), "DEVIATION_UPPER", queryVO.getFieldValueByFieldName("deviationUpper"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("deviationLower")), "DEVIATION_LOWER", queryVO.getFieldValueByFieldName("deviationLower"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("enabled")), "ENABLED", queryVO.getFieldValueByFieldName("enabled"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("warningLevel")), "WARNING_LEVEL", queryVO.getFieldValueByFieldName("warningLevel"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("notifyTypes")), "NOTIFY_TYPES", queryVO.getFieldValueByFieldName("notifyTypes"));
    }


    @Override
    protected String getTypeCode(){
        return "S3520";
    }

}
