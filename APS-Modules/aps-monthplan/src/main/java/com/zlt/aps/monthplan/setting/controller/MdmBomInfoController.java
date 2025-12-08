package com.zlt.aps.monthplan.setting.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.common.security.annotation.RequiresPermissions;
import com.zlt.aps.maindata.mapper.MdmBomInfoEntityMapper;
import com.zlt.aps.monthplan.api.domain.entity.MdmBomInfo;
import com.zlt.common.utils.PubUtil;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import lombok.extern.slf4j.Slf4j;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

import com.zlt.aps.maindata.service.IMdmBomInfoService;

import com.ruoyi.common.core.web.page.TableDataInfo;

import com.zlt.bill.common.controller.AbstractDocBizController;
import com.zlt.bill.common.service.IDocService ;

/**
* Copyright (c) 2022, All rights reserved。
* 文件名称：MdmBomInfoController.java
* 描    述：BOM示方书 控制层类：....
*@author zlt
*@date 2025-12-05
*@version 1.0
*
 *  修改记录：
*     修改时间：...
*     修 改 人：zlt
*     修改内容：...
*/
@Slf4j
@Api(tags = "BOM示方书")
@RestController
@RequestMapping("/mdmBomInfo")
public class MdmBomInfoController extends AbstractDocBizController<MdmBomInfo> {

    @Autowired
    private IMdmBomInfoService mdmBomInfoService;

    @Autowired
    private MdmBomInfoEntityMapper entityMapper;

    /**
     * 查询BOM示方书列表
     */
    @RequiresPermissions( "maindata:mdmBomInfo:list")
    @ApiOperation("查询列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody MdmBomInfo queryVO) {
        return super.list(queryVO);
    }

    @Override
    protected String getOrderBy() {
        return "create_time desc";
    }

    /**
     * 保存
     */
    @Log(title = "ui.data.column.mdmBomInfo.modelName", businessType = BusinessType.INSERT_OR_UPDATE)
    @RequiresPermissions( "maindata:mdmBomInfo:save")
    @ApiOperation("保存")
    @PostMapping("/save")
    @Override
    public AjaxResult save(@RequestBody MdmBomInfo billVO){
        return super.save(billVO);
    }

    /**
     * 删除
     */
    @Log(title = "ui.data.column.mdmBomInfo.modelName", businessType = BusinessType.DELETE)
    @RequiresPermissions( "maindata:mdmBomInfo:remove")
    @ApiOperation("删除")
    @DeleteMapping("/remove")
    @Override
    public AjaxResult removeByIds(@RequestBody List<Long> ids){
        return super.removeByIds(ids);
    }


    /**
     * 获取BOM示方书详细信息
     */
    @RequiresPermissions( "maindata:mdmBomInfo:query")
    @ApiOperation("获取详细信息")
    @GetMapping(value = "/{billId}")
    @Override
    public MdmBomInfo getInfo(@PathVariable("billId") Long billId) {
        return super.getInfo(billId);
    }


    /**
     * 根据集合导入BOM示方书数据
     * @param importContext 导入上下文
     * @param updateSupport 已存在记录是否更新
     * @return 结果
     */
    @RequiresPermissions( "maindata:mdmBomInfo:import")
    @Log(title = "ui.data.column.mdmBomInfo.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    @Override
    public AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        return super.importData(importContext,updateSupport);
    }

    /**
     * 导出列表
     */
    @RequiresPermissions( "maindata:mdmBomInfo:export")
    @Log(title = "BOM示方书", businessType = BusinessType.EXPORT)
    @ApiOperation("导出数据")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody MdmBomInfo queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return super.exportData(queryVO, fileName, response);
    }

    @Override
    protected List<MdmBomInfo> listExportData(MdmBomInfo obj) {
        QueryWrapper<MdmBomInfo> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, obj);
        return entityMapper.selectList(wrapper);
    }

    @Override
    protected IDocService getDocService(){
        return mdmBomInfoService;
    }

    /**
     * 条件拼接
     *
     * @param queryWrapper
     * @param queryVO
     */
    @Override
    protected void builderCondition(QueryWrapper<MdmBomInfo> queryWrapper, MdmBomInfo queryVO) {
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("childMaterialCode")), "CHILD_MATERIAL_CODE", queryVO.getFieldValueByFieldName("childMaterialCode"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("childMaterialName")), "CHILD_MATERIAL_NAME", queryVO.getFieldValueByFieldName("childMaterialName"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("childMaterialNameCode")), "CHILD_MATERIAL_NAME_CODE", queryVO.getFieldValueByFieldName("childMaterialNameCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("childCode")), "CHILD_CODE", queryVO.getFieldValueByFieldName("childCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("unit")), "UNIT", queryVO.getFieldValueByFieldName("unit"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("dosage")), "DOSAGE", queryVO.getFieldValueByFieldName("dosage"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("dosageForm")), "DOSAGE_FORM", queryVO.getFieldValueByFieldName("dosageForm"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("parentMaterialCode")), "PARENT_MATERIAL_CODE", queryVO.getFieldValueByFieldName("parentMaterialCode"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("parentMaterialName")), "PARENT_MATERIAL_NAME", queryVO.getFieldValueByFieldName("parentMaterialName"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("parentCode")), "PARENT_CODE", queryVO.getFieldValueByFieldName("parentCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("productionStage")), "PRODUCTION_STAGE", queryVO.getFieldValueByFieldName("productionStage"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("productionStageCode")), "PRODUCTION_STAGE_CODE", queryVO.getFieldValueByFieldName("productionStageCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("bomVersion")), "BOM_VERSION", queryVO.getFieldValueByFieldName("bomVersion"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("childMaterialVersion")), "CHILD_MATERIAL_VERSION", queryVO.getFieldValueByFieldName("childMaterialVersion"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("bomType")), "BOM_TYPE", queryVO.getFieldValueByFieldName("bomType"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("status")), "STATUS", queryVO.getFieldValueByFieldName("status"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("mesCreateDate")), "MES_CREATE_DATE", queryVO.getFieldValueByFieldName("mesCreateDate"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("mesUpdateDate")), "MES_UPDATE_DATE", queryVO.getFieldValueByFieldName("mesUpdateDate"));
    }


    @Override
    protected String getTypeCode(){
        return "MDM0106";
    }


    /**
     * 抓取MES数据
     * @return 结果
     */
    @ApiOperation("抓取MES数据")
    @PostMapping("/mesCapture")
    public AjaxResult mesCapture() {
        // TODO...
        return AjaxResult.success();
    }

}
