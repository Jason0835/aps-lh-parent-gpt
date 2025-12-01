package com.zlt.aps.cx.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.security.annotation.Logical;
import com.ruoyi.common.security.annotation.RequiresPermissions;
import com.zlt.aps.cx.service.ICxPersionTrainSettingService;
import com.zlt.aps.cxlh.cx.api.domain.entity.CxPersionTrainSetting;
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
* 文件名称：CxPersionTrainSettingController.java
* 描    述：成型工序开机档数 控制层类：....
*@author zlt
*@date 2025-02-17
*@version 1.0
*
 *  修改记录：
*     修改时间：...
*     修 改 人：zlt
*     修改内容：...
*/
@Slf4j
@Api(tags = "成型工序开机档数")
@RestController
@RequestMapping("/cxPersionTrainSetting")
public class CxPersionTrainSettingController extends AbstractDocBizController<CxPersionTrainSetting> {

    @Autowired
    private ICxPersionTrainSettingService cxPersionTrainSettingService;

    /**
     * 查询成型工序开机档数列表
     */
    @RequiresPermissions( "cx:cxPersionTrainSetting:list")
    @ApiOperation("查询列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody CxPersionTrainSetting queryVO) {
        // 无需分页
        List<CxPersionTrainSetting> list = cxPersionTrainSettingService.selectList(queryVO);
        return getDataTable(list);
    }


    /**
     * 保存
     */
    @Log(title = "ui.data.column.cxPersionTrainSetting.modelName", businessType = BusinessType.INSERT_OR_UPDATE)
    @RequiresPermissions(value = {"cx:cxPersionTrainSetting:edit", "cx:cxPersionTrainSetting:add"}, logical = Logical.OR)
    @ApiOperation("保存")
    @PostMapping("/save")
    @Override
    public AjaxResult save(@RequestBody CxPersionTrainSetting billVO){
        return toAjax(cxPersionTrainSettingService.save(billVO));
    }

    /**
     * 删除
     */
    @Log(title = "ui.data.column.cxPersionTrainSetting.modelName", businessType = BusinessType.DELETE)
    @RequiresPermissions( "cx:cxPersionTrainSetting:remove")
    @ApiOperation("删除")
    @DeleteMapping("/remove")
    @Override
    public AjaxResult removeByIds(@RequestBody List<Long> ids){
        return toAjax(cxPersionTrainSettingService.removeByIds(ids));
    }


    /**
     * 获取成型工序开机档数详细信息
     */
    @ApiOperation("获取详细信息")
    @GetMapping(value = "/{billId}")
    @Override
    public CxPersionTrainSetting getInfo(@PathVariable("billId") Long billId) {
        return cxPersionTrainSettingService.getInfo(billId);
    }


    /**
     * 根据集合导入成型工序开机档数数据
     * @param importContext 导入上下文
     * @param updateSupport 已存在记录是否更新
     * @return 结果
     */
    @RequiresPermissions( "cx:cxPersionTrainSetting:import")
    @Log(title = "ui.data.column.cxPersionTrainSetting.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData/{updateSupport}")
    @Override
    public AjaxResult importData(@RequestBody ImportContext importContext, @PathVariable("updateSupport") boolean updateSupport) throws Exception {
        return super.importData(importContext,updateSupport);
    }

    /**
     * 导出列表
     */
    @RequiresPermissions( "cx:cxPersionTrainSetting:export")
    @Log(title = "成型工序开机档数", businessType = BusinessType.EXPORT)
    @ApiOperation("导入数据")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody CxPersionTrainSetting queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return super.exportData(queryVO, fileName, response);
    }

    /**
     * 列表校验唯一并保存
     */
    @ApiOperation("列表校验唯一并保存")
    @PostMapping("/saveList")
    public AjaxResult saveList(@RequestBody List<CxPersionTrainSetting> list) {
        return cxPersionTrainSettingService.saveList(list);
    }

    @Override
    protected IDocService getDocService(){
        return cxPersionTrainSettingService;
    }

    /**
     * 条件拼接
     *
     * @param queryWrapper
     * @param queryVO
     */
    @Override
    protected void builderCondition(QueryWrapper<CxPersionTrainSetting> queryWrapper, CxPersionTrainSetting queryVO) {
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("scheduleDate")), "SCHEDULE_DATE", queryVO.getFieldValueByFieldName("scheduleDate"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("mouldMethod")), "MOULD_METHOD", queryVO.getFieldValueByFieldName("mouldMethod"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("isFull")), "IS_FULL", queryVO.getFieldValueByFieldName("isFull"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("quotaMorning")), "QUOTA_MORNING", queryVO.getFieldValueByFieldName("quotaMorning"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("quotaNight")), "QUOTA_NIGHT", queryVO.getFieldValueByFieldName("quotaNight"));
    }


    @Override
    protected String getTypeCode(){
        return "9004CX";
    }


}
