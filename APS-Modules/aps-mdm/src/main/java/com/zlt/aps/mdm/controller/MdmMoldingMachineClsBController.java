package com.zlt.aps.monthplan.setting.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.maindata.mapper.MdmMoldingMachineClsBEntityMapper;
import com.zlt.aps.maindata.service.IMdmMoldingMachineClsBService;
import com.zlt.aps.monthplan.api.domain.entity.MdmMoldingMachineClsB;
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
* 文件名称：MdmMoldingMachineClsBController.java
* 描    述：成型机单机班产 控制层类：....
*@author zlt
*@date 2025-02-27
*@version 1.0
*
 *  修改记录：
*     修改时间：...
*     修 改 人：zlt
*     修改内容：...
*/
@Slf4j
@Api(tags = "成型机单机班产")
@RestController
@RequestMapping("/mdmMoldingMachineClsB")
public class MdmMoldingMachineClsBController extends AbstractDocBizController<MdmMoldingMachineClsB> {

    @Autowired
    private IMdmMoldingMachineClsBService mdmMoldingMachineClsBService;

    @Autowired
    private MdmMoldingMachineClsBEntityMapper entityMapper;

    /**
     * 查询成型机单机班产列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody MdmMoldingMachineClsB queryVO) {
        this.startPage(this.getOrderBy());
        QueryWrapper<MdmMoldingMachineClsB> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, queryVO);
        List<MdmMoldingMachineClsB> list = entityMapper.selectList(wrapper);
        try {
            QueryFormulaUtil.execFormula(list, this.getQueryFormulas());
        } catch (QueryExprException e) {
            this.logger.error(e.getMessage(), e);
            throw new ServiceException("执行查询公式时发生错误.");
        }
        return getDataTable(list);
    }

    @Override
    protected String getOrderBy() {
        return "create_time desc";
    }

    /**
     * 保存
     */
    @Log(title = "ui.data.column.mdmMoldingMachineClsB.modelName", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("保存")
    @PostMapping("/save")
    @Override
    public AjaxResult save(@RequestBody MdmMoldingMachineClsB billVO){
        return super.save(billVO);
    }

    /**
     * 删除
     */
    @Log(title = "ui.data.column.mdmMoldingMachineClsB.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除")
    @DeleteMapping("/remove")
    @Override
    public AjaxResult removeByIds(@RequestBody List<Long> ids){
        return super.removeByIds(ids);
    }


    /**
     * 获取成型机单机班产详细信息
     */
    @ApiOperation("获取详细信息")
    @GetMapping(value = "/{billId}")
    @Override
    public MdmMoldingMachineClsB getInfo(@PathVariable("billId") Long billId) {
        return super.getInfo(billId);
    }


    /**
     * 根据集合导入成型机单机班产数据
     * @param importContext 导入上下文
     * @param updateSupport 已存在记录是否更新
     * @return 结果
     */
    @Log(title = "ui.data.column.mdmMoldingMachineClsB.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    @Override
    public AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        return super.importData(importContext,updateSupport);
    }

    /**
     * 导出列表
     */
    @Log(title = "成型机单机班产", businessType = BusinessType.EXPORT)
    @ApiOperation("导入数据")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody MdmMoldingMachineClsB queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return super.exportData(queryVO, fileName, response);
    }

    @Override
    protected List<MdmMoldingMachineClsB> listExportData(MdmMoldingMachineClsB obj) {
        QueryWrapper<MdmMoldingMachineClsB> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, obj);
        List<MdmMoldingMachineClsB> list = entityMapper.selectList(wrapper);
        try {
            QueryFormulaUtil.execFormula(list, this.getQueryFormulas());
        } catch (QueryExprException e) {
            this.logger.error(e.getMessage(), e);
            throw new ServiceException("执行查询公式时发生错误.");
        }
        return list;
    }

    @Override
    protected IDocService getDocService(){
        return mdmMoldingMachineClsBService;
    }

    /**
     * 条件拼接
     *
     * @param queryWrapper
     * @param queryVO
     */
    @Override
    protected void builderCondition(QueryWrapper<MdmMoldingMachineClsB> queryWrapper, MdmMoldingMachineClsB queryVO) {
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("moldingMachineClassId")), "MOLDING_MACHINE_CLASS_ID", queryVO.getFieldValueByFieldName("moldingMachineClassId"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("proSize")), "PRO_SIZE", queryVO.getFieldValueByFieldName("proSize"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("productionQuotaQty")), "PRODUCTION_QUOTA_QTY", queryVO.getFieldValueByFieldName("productionQuotaQty"));
    }

    @Override
    protected String getTypeCode(){
        return "0119";
    }

    @Override
    protected String[] getQueryFormulas() {
        return new String[] {
                "moldingMachineClassCode->getcolvalue(T_MDM_MOLDING_MACHINE_CLS, MOLDING_MACHINE_CLASS_CODE, id, moldingMachineClassId)",
        };
    }
}
