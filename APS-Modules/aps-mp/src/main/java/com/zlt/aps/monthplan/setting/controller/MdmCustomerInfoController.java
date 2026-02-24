package com.zlt.aps.monthplan.setting.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.maindata.service.IMdmCustomerInfoService;
import com.zlt.aps.monthplan.api.domain.entity.MdmCustomerInfo;
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
* 文件名称：MdmCustomerInfoController.java
* 描    述：客户信息 控制层类：....
*@author zlt
*@date 2025-03-04
*@version 1.0
*
 *  修改记录：
*     修改时间：...
*     修 改 人：zlt
*     修改内容：...
*/
@Slf4j
@Api(tags = "客户信息")
@RestController
@RequestMapping("/mdmCustomerInfo")
public class MdmCustomerInfoController extends AbstractDocBizController<MdmCustomerInfo> {

    @Autowired
    private IMdmCustomerInfoService mdmCustomerInfoService;

    /**
     * 查询客户信息列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody MdmCustomerInfo queryVO) {
        this.startPage(this.getOrderBy());
        QueryWrapper<MdmCustomerInfo> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, queryVO);
        List<MdmCustomerInfo> list = mdmCustomerInfoService.selectList(wrapper);
        return getDataTable(list);
    }

    @Override
    protected String getOrderBy() {
        return "create_time desc";
    }

    /**
     * 保存
     */
    @Log(title = "ui.data.column.mdmCustomerInfo.modelName", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("保存")
    @PostMapping("/save")
    @Override
    public AjaxResult save(@RequestBody MdmCustomerInfo billVO){
        return super.save(billVO);
    }

    /**
     * 删除
     */
    @Log(title = "ui.data.column.mdmCustomerInfo.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除")
    @DeleteMapping("/remove")
    @Override
    public AjaxResult removeByIds(@RequestBody List<Long> ids){
        return super.removeByIds(ids);
    }

    /**
     * 获取客户信息详细信息
     */
    @ApiOperation("获取详细信息")
    @GetMapping(value = "/{billId}")
    @Override
    public MdmCustomerInfo getInfo(@PathVariable("billId") Long billId) {
        return super.getInfo(billId);
    }


    /**
     * 根据集合导入客户信息数据
     * @param importContext 导入上下文
     * @param updateSupport 已存在记录是否更新
     * @return 结果
     */
    @Log(title = "ui.data.column.mdmCustomerInfo.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    @Override
    public AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        return super.importData(importContext,updateSupport);
    }

    /**
     * 导出列表
     */
    @Log(title = "客户信息", businessType = BusinessType.EXPORT)
    @ApiOperation("导入数据")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody MdmCustomerInfo queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return super.exportData(queryVO, fileName, response);
    }

    @Override
    protected List<MdmCustomerInfo> listExportData(MdmCustomerInfo obj) {
        QueryWrapper<MdmCustomerInfo> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, obj);
        return mdmCustomerInfoService.selectList(wrapper);
    }

    @Override
    protected IDocService getDocService(){
        return mdmCustomerInfoService;
    }

    /**
     * 条件拼接
     *
     * @param queryWrapper
     * @param queryVO
     */
    @Override
    protected void builderCondition(QueryWrapper<MdmCustomerInfo> queryWrapper, MdmCustomerInfo queryVO) {
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("factoryCode")), "FACTORY_CODE", queryVO.getFieldValueByFieldName("factoryCode"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("customCode")), "CUSTOM_CODE", queryVO.getFieldValueByFieldName("customCode"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("customName")), "CUSTOM_NAME", queryVO.getFieldValueByFieldName("customName"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("isImportantCustom")), "IS_IMPORTANT_CUSTOM", queryVO.getFieldValueByFieldName("isImportantCustom"));
    }

    @Override
    protected String getTypeCode(){
        return "0142";
    }


}
