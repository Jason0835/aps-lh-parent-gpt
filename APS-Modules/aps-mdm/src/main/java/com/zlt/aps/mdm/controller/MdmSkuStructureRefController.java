package com.zlt.aps.monthplan.setting.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.maindata.mapper.MdmSkuStructureRefEntityMapper;
import com.zlt.aps.maindata.service.IMdmSkuStructureRefService;
import com.zlt.aps.monthplan.api.domain.entity.MdmSkuStructureRef;
import com.zlt.bill.common.controller.AbstractDocBizController;
import com.zlt.bill.common.service.IDocService;
import com.zlt.common.exception.QueryExprException;
import com.zlt.common.utils.PubUtil;
import com.zlt.core.queryformulas.QueryFormulaUtil;
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
* 文件名称：MdmSkuStructureRefController.java
* 描    述：SKU与结构关系 控制层类：....
*@author zlt
*@date 2025-12-06
*@version 1.0
*
 *  修改记录：
*     修改时间：...
*     修 改 人：zlt
*     修改内容：...
*/
@Slf4j
@Api(tags = "SKU与结构关系")
@RestController
@RequestMapping("/mdmSkuStructureRef")
public class MdmSkuStructureRefController extends AbstractDocBizController<MdmSkuStructureRef> {

    @Autowired
    private IMdmSkuStructureRefService mdmSkuStructureRefService;

    @Autowired
    private MdmSkuStructureRefEntityMapper entityMapper;

    /**
     * 查询SKU与结构关系列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody MdmSkuStructureRef queryVO) {
        QueryWrapper<MdmSkuStructureRef> queryWrapper = new QueryWrapper<>();
        // 条件拼接
        builderCondition(queryWrapper, queryVO);
        startPage(getOrderBy());
        List<MdmSkuStructureRef> list = entityMapper.getMdmSkuStructureRefList(queryWrapper);
        clearPage();
        try {
            QueryFormulaUtil.execFormula(list, this.getQueryFormulas());
        } catch (QueryExprException e) {
            throw new ServiceException("执行查询公式时发生错误.");
        }
        return getDataTable(list);
    }

    @Override
    protected String[] getQueryFormulas() {
        return new String[]{
                "materialDesc -> getcolvalue(T_MDM_MATERIAL_INFO, MATERIAL_DESC, MATERIAL_CODE, materialCode)"
        };
    }


    @Override
    protected String getOrderBy() {
        return "update_time desc";
    }

    /**
     * 保存
     */
    @Log(title = "ui.data.column.mdmSkuStructureRef.modelName", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("保存")
    @PostMapping("/save")
    @Override
    public AjaxResult save(@RequestBody MdmSkuStructureRef billVO){
        return super.save(billVO);
    }

    /**
     * 删除
     */
    @Log(title = "ui.data.column.mdmSkuStructureRef.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除")
    @DeleteMapping("/remove")
    @Override
    public AjaxResult removeByIds(@RequestBody List<Long> ids){
        return super.removeByIds(ids);
    }


    /**
     * 获取SKU与结构关系详细信息
     */
    @ApiOperation("获取详细信息")
    @GetMapping(value = "/{billId}")
    @Override
    public MdmSkuStructureRef getInfo(@PathVariable("billId") Long billId) {
        return super.getInfo(billId);
    }


    /**
     * 根据集合导入SKU与结构关系数据
     * @param importContext 导入上下文
     * @param updateSupport 已存在记录是否更新
     * @return 结果
     */
    @Log(title = "ui.data.column.mdmSkuStructureRef.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    @Override
    public AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        return super.importData(importContext,updateSupport);
    }

    /**
     * 导出列表
     */
    @Log(title = "SKU与结构关系", businessType = BusinessType.EXPORT)
    @ApiOperation("导出数据")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody MdmSkuStructureRef queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return super.exportData(queryVO, fileName, response);
    }

    @Override
    protected List<MdmSkuStructureRef> listExportData(MdmSkuStructureRef obj) {
        QueryWrapper<MdmSkuStructureRef> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, obj);
        startPage(getOrderBy());
        List<MdmSkuStructureRef> list = entityMapper.getMdmSkuStructureRefList(wrapper);
        clearPage();
        try {
            QueryFormulaUtil.execFormula(list, this.getQueryFormulas());
        } catch (QueryExprException e) {
            throw new ServiceException("执行查询公式时发生错误.");
        }
        return list;
    }

    @Override
    protected IDocService getDocService(){
        return mdmSkuStructureRefService;
    }

    /**
     * 条件拼接
     *
     * @param queryWrapper
     * @param queryVO
     */
    @Override
    protected void builderCondition(QueryWrapper<MdmSkuStructureRef> queryWrapper, MdmSkuStructureRef queryVO) {
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("factoryCode")), "a.FACTORY_CODE", queryVO.getFieldValueByFieldName("factoryCode"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("materialCode")), "a.MATERIAL_CODE", queryVO.getFieldValueByFieldName("materialCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("mainMaterialDesc")), "a.MAIN_MATERIAL_DESC", queryVO.getFieldValueByFieldName("mainMaterialDesc"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("structureName")), "a.STRUCTURE_NAME", queryVO.getFieldValueByFieldName("structureName"));
        queryWrapper.eq("a.IS_DELETE", YesOrNoEnum.NO.getValue());
        // 新增：MATERIAL_DESC 模糊查询（关联 T_MDM_MATERIAL_INFO 表）
        Object materialDesc = queryVO.getFieldValueByFieldName("materialDesc");
        queryWrapper.like(PubUtil.isNotEmpty(materialDesc), "b.MATERIAL_DESC", materialDesc);
    }


    @Override
    protected String getTypeCode(){
        return "MDM0134";
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

    /**
     * 查询结构选择列表
     */
    @ApiOperation("查询结构选择列表")
    @PostMapping("/getStructureSelectList")
    public TableDataInfo getStructureSelectList(@RequestBody MdmSkuStructureRef queryVO) {
        this.startPage();
        List<MdmSkuStructureRef> list = entityMapper.getStructureSelectList(queryVO);
        this.clearPage();
        return this.getDataTable(list);
    }

    /**
     * 更新结构到物料表
     * @param queryVO 参数
     * @return 结果
     */
    @ApiOperation("更新结构到物料表")
    @PostMapping("/updateStructureToMaterial")
    public AjaxResult updateStructureToMaterial(@RequestBody MdmSkuStructureRef queryVO) {
        return mdmSkuStructureRefService.updateStructureToMaterial(queryVO);
    }




}
