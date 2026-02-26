package com.zlt.aps.mdm.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.security.annotation.RequiresPermissions;
import com.zlt.aps.mdm.mapper.MdmStructureNameEntityMapper;
import com.zlt.aps.mdm.service.IMdmStructureNameService;
import com.zlt.aps.mp.api.domain.entity.MdmStructureName;
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
* 文件名称：MdmStructureNameController.java
* 描    述：结构信息(SKU与结构关系选择结构使用) 控制层类：....
*@author zlt
*@date 2026-02-26
*@version 1.0
*
 *  修改记录：
*     修改时间：...
*     修 改 人：zlt
*     修改内容：...
*/
@Slf4j
@Api(tags = "结构信息(SKU与结构关系选择结构使用)")
@RestController
@RequestMapping("/mdmStructureName")
public class MdmStructureNameController extends AbstractDocBizController<MdmStructureName> {

    @Autowired
    private IMdmStructureNameService mdmStructureNameService;

    @Autowired
    private MdmStructureNameEntityMapper entityMapper;

    /**
     * 查询结构信息(SKU与结构关系选择结构使用)列表
     */
    @RequiresPermissions( "mdm:mdmStructureName:list")
    @ApiOperation("查询列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody MdmStructureName queryVO) {
        return super.list(queryVO);
    }

    @Override
    protected String getOrderBy() {
        return "create_time desc";
    }

    /**
     * 保存
     */
    @Log(title = "ui.data.column.mdmStructureName.modelName", businessType = BusinessType.INSERT_OR_UPDATE)
    @RequiresPermissions( "mdm:mdmStructureName:save")
    @ApiOperation("保存")
    @PostMapping("/save")
    @Override
    public AjaxResult save(@RequestBody MdmStructureName billVO){
        return super.save(billVO);
    }

    /**
     * 删除
     */
    @Log(title = "ui.data.column.mdmStructureName.modelName", businessType = BusinessType.DELETE)
    @RequiresPermissions( "mdm:mdmStructureName:remove")
    @ApiOperation("删除")
    @DeleteMapping("/remove")
    @Override
    public AjaxResult removeByIds(@RequestBody List<Long> ids){
        return super.removeByIds(ids);
    }


    /**
     * 获取结构信息(SKU与结构关系选择结构使用)详细信息
     */
    @RequiresPermissions( "mdm:mdmStructureName:query")
    @ApiOperation("获取详细信息")
    @GetMapping(value = "/{billId}")
    @Override
    public MdmStructureName getInfo(@PathVariable("billId") Long billId) {
        return super.getInfo(billId);
    }


    /**
     * 根据集合导入结构信息(SKU与结构关系选择结构使用)数据
     * @param importContext 导入上下文
     * @param updateSupport 已存在记录是否更新
     * @return 结果
     */
    @RequiresPermissions( "mdm:mdmStructureName:import")
    @Log(title = "ui.data.column.mdmStructureName.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    @Override
    public AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        return super.importData(importContext,updateSupport);
    }

    /**
     * 导出列表
     */
    @RequiresPermissions( "mdm:mdmStructureName:export")
    @Log(title = "结构信息(SKU与结构关系选择结构使用)", businessType = BusinessType.EXPORT)
    @ApiOperation("导入数据")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody MdmStructureName queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return super.exportData(queryVO, fileName, response);
    }

    @Override
    protected List<MdmStructureName> listExportData(MdmStructureName obj) {
        QueryWrapper<MdmStructureName> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, obj);
        return entityMapper.selectList(wrapper);
    }

    @Override
    protected IDocService getDocService(){
        return mdmStructureNameService;
    }

    /**
     * 条件拼接
     *
     * @param queryWrapper
     * @param queryVO
     */
    @Override
    protected void builderCondition(QueryWrapper<MdmStructureName> queryWrapper, MdmStructureName queryVO) {
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("structureName")), "STRUCTURE_NAME", queryVO.getFieldValueByFieldName("structureName"));
    }

    @Override
    protected String getTypeCode(){
        return "MDM0807";
    }


}
