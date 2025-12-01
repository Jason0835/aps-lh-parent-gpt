package com.zlt.aps.monthplan.setting.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.core.utils.SecurityUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.maindata.mapper.MdmMoldingMachineClsBEntityMapper;
import com.zlt.aps.maindata.mapper.MdmMoldingMachineClsEntityMapper;
import com.zlt.aps.maindata.service.IMdmMoldingMachineClsService;
import com.zlt.aps.monthplan.api.domain.entity.MdmMoldingMachineCls;
import com.zlt.aps.monthplan.api.domain.entity.MdmMoldingMachineClsB;
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
import java.util.Date;
import java.util.List;

/**
* Copyright (c) 2022, All rights reserved。
* 文件名称：MdmMoldingMachineClsController.java
* 描    述：成型机类型 控制层类：....
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
@Api(tags = "成型机类型")
@RestController
@RequestMapping("/mdmMoldingMachineCls")
public class MdmMoldingMachineClsController extends AbstractDocBizController<MdmMoldingMachineCls> {

    @Autowired
    private IMdmMoldingMachineClsService mdmMoldingMachineClsService;

    @Autowired
    private MdmMoldingMachineClsEntityMapper entityMapper;

    @Autowired
    private MdmMoldingMachineClsBEntityMapper mdmMoldingMachineClsBEntityMapper;

    /**
     * 查询成型机类型列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody MdmMoldingMachineCls queryVO) {
        this.startPage(this.getOrderBy());
        QueryWrapper<MdmMoldingMachineCls> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, queryVO);
        List<MdmMoldingMachineCls> list = entityMapper.selectList(wrapper);
        return getDataTable(list);
    }

    @Override
    protected String getOrderBy() {
        return "create_time desc";
    }


    /**
     * 保存
     */
    @Log(title = "ui.data.column.mdmMoldingMachineCls.modelName", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("保存")
    @PostMapping("/save")
    @Override
    public AjaxResult save(@RequestBody MdmMoldingMachineCls billVO){
        return super.save(billVO);
    }

    /**
     * 删除
     */
    @Log(title = "ui.data.column.mdmMoldingMachineCls.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除")
    @DeleteMapping("/remove")
    @Override
    public AjaxResult removeByIds(@RequestBody List<Long> ids){
        AjaxResult ajaxResult = super.removeByIds(ids);
        // 成功删除对应子表
        if (!ajaxResult.get(AjaxResult.CODE_TAG).equals(HttpStatus.ERROR)) {
            LambdaUpdateWrapper<MdmMoldingMachineClsB> wrapper = new LambdaUpdateWrapper<>();
            wrapper.in(MdmMoldingMachineClsB::getMoldingMachineClassId, ids)
                    .set(MdmMoldingMachineClsB::getIsDelete, 1)
                    .set(MdmMoldingMachineClsB::getUpdateBy, SecurityUtils.getUsername())
                    .set(MdmMoldingMachineClsB::getUpdateTime, new Date());
            mdmMoldingMachineClsBEntityMapper.update(null, wrapper);
        }
        return ajaxResult;
    }


    /**
     * 获取成型机类型详细信息
     */
    @ApiOperation("获取详细信息")
    @GetMapping(value = "/{billId}")
    @Override
    public MdmMoldingMachineCls getInfo(@PathVariable("billId") Long billId) {
        return super.getInfo(billId);
    }


    /**
     * 根据集合导入成型机类型数据
     * @param importContext 导入上下文
     * @param updateSupport 已存在记录是否更新
     * @return 结果
     */
    @Log(title = "ui.data.column.mdmMoldingMachineCls.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    @Override
    public AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        return super.importData(importContext,updateSupport);
    }

    /**
     * 导出列表
     */
    @Log(title = "成型机类型", businessType = BusinessType.EXPORT)
    @ApiOperation("导入数据")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody MdmMoldingMachineCls queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return super.exportData(queryVO, fileName, response);
    }

    @Override
    protected List<MdmMoldingMachineCls> listExportData(MdmMoldingMachineCls obj) {
        QueryWrapper<MdmMoldingMachineCls> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, obj);
        return entityMapper.selectList(wrapper);
    }

    @Override
    protected IDocService getDocService(){
        return mdmMoldingMachineClsService;
    }

    /**
     * 条件拼接
     *
     * @param queryWrapper
     * @param queryVO
     */
    @Override
    protected void builderCondition(QueryWrapper<MdmMoldingMachineCls> queryWrapper, MdmMoldingMachineCls queryVO) {
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("factoryCode")), "FACTORY_CODE", queryVO.getFieldValueByFieldName("factoryCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("moldingMachineClassCode")), "MOLDING_MACHINE_CLASS_CODE", queryVO.getFieldValueByFieldName("moldingMachineClassCode"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("moldingMachineClassName")), "MOLDING_MACHINE_CLASS_NAME", queryVO.getFieldValueByFieldName("moldingMachineClassName"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("mouldMethod")), "MOULD_METHOD", queryVO.getFieldValueByFieldName("mouldMethod"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("ratio")), "RATIO", queryVO.getFieldValueByFieldName("ratio"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("productionMode")), "PRODUCTION_MODE", queryVO.getFieldValueByFieldName("productionMode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("isClosed")), "IS_CLOSED", queryVO.getFieldValueByFieldName("isClosed"));
    }


    @Override
    protected String getTypeCode(){
        return "0118";
    }


}
