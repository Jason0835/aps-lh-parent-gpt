package com.zlt.aps.lh.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.lh.api.domain.entity.LhMoldChangePlanLog;
import com.zlt.aps.lh.mapper.LhMoldChangePlanLogEntityMapper;
import com.zlt.aps.lh.service.ILhMoldChangePlanLogService;
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
* 文件名称：LhMoldChangePlanLogController.java
* 描    述：模具变动单日志 控制层类：....
*@author zlt
*@date 2025-03-17
*@version 1.0
*
 *  修改记录：
*     修改时间：...
*     修 改 人：zlt
*     修改内容：...
*/
@Slf4j
@Api(tags = "模具变动单日志")
@RestController
@RequestMapping("/lhMoldChangePlanLog")
public class LhMoldChangePlanLogController extends AbstractDocBizController<LhMoldChangePlanLog> {

    @Autowired
    private ILhMoldChangePlanLogService lhMoldChangePlanLogService;

    @Autowired
    private LhMoldChangePlanLogEntityMapper entityMapper;

    /**
     * 查询模具变动单日志列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody LhMoldChangePlanLog queryVO) {
        return super.list(queryVO);
    }

    /**
     * 保存
     */
    @Log(title = "ui.data.column.lhMoldChangePlanLog.modelName", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("保存")
    @PostMapping("/save")
    @Override
    public AjaxResult save(@RequestBody LhMoldChangePlanLog billVO){
        return super.save(billVO);
    }

    /**
     * 删除
     */
    @Log(title = "ui.data.column.lhMoldChangePlanLog.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除")
    @DeleteMapping("/remove")
    @Override
    public AjaxResult removeByIds(@RequestBody List<Long> ids){
        return super.removeByIds(ids);
    }


    /**
     * 获取模具变动单日志详细信息
     */
    @ApiOperation("获取详细信息")
    @GetMapping(value = "/{billId}")
    @Override
    public LhMoldChangePlanLog getInfo(@PathVariable("billId") Long billId) {
        return super.getInfo(billId);
    }


    /**
     * 根据集合导入模具变动单日志数据
     * @param importContext 导入上下文
     * @param updateSupport 已存在记录是否更新
     * @return 结果
     */
    @Log(title = "ui.data.column.lhMoldChangePlanLog.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    @Override
    public AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        return super.importData(importContext,updateSupport);
    }

    /**
     * 导出列表
     */
    @Log(title = "模具变动单日志", businessType = BusinessType.EXPORT)
    @ApiOperation("导入数据")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody LhMoldChangePlanLog queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return super.exportData(queryVO, fileName, response);
    }

    @Override
    protected List<LhMoldChangePlanLog> listExportData(LhMoldChangePlanLog obj) {
        QueryWrapper<LhMoldChangePlanLog> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, obj);
        return entityMapper.selectList(wrapper);
    }

    @Override
    protected IDocService getDocService(){
        return lhMoldChangePlanLogService;
    }

    /**
     * 条件拼接
     *
     * @param queryWrapper
     * @param queryVO
     */
    @Override
    protected void builderCondition(QueryWrapper<LhMoldChangePlanLog> queryWrapper, LhMoldChangePlanLog queryVO) {
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("factoryCode")), "FACTORY_CODE", queryVO.getFieldValueByFieldName("factoryCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("moldBatchNo")), "MOLD_BATCH_NO", queryVO.getFieldValueByFieldName("moldBatchNo"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("lhMachineCode")), "LH_MACHINE_CODE", queryVO.getFieldValueByFieldName("lhMachineCode"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("lhMachineName")), "LH_MACHINE_NAME", queryVO.getFieldValueByFieldName("lhMachineName"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("beforeSpecCode")), "BEFORE_SPEC_CODE", queryVO.getFieldValueByFieldName("beforeSpecCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("beforeSpecDesc")), "BEFORE_SPEC_DESC", queryVO.getFieldValueByFieldName("beforeSpecDesc"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("tireRoughStock")), "TIRE_ROUGH_STOCK", queryVO.getFieldValueByFieldName("tireRoughStock"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("changeType")), "CHANGE_TYPE", queryVO.getFieldValueByFieldName("changeType"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("afterSpecCode")), "AFTER_SPEC_CODE", queryVO.getFieldValueByFieldName("afterSpecCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("afterSpecDesc")), "AFTER_SPEC_DESC", queryVO.getFieldValueByFieldName("afterSpecDesc"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("stockArea")), "STOCK_AREA", queryVO.getFieldValueByFieldName("stockArea"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("changeTime")), "CHANGE_TIME", queryVO.getFieldValueByFieldName("changeTime"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("sourceCxOrder")), "SOURCE_CX_ORDER", queryVO.getFieldValueByFieldName("sourceCxOrder"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("isRelease")), "IS_RELEASE", queryVO.getFieldValueByFieldName("isRelease"));
    }


    @Override
    protected String getTypeCode(){
        return "0302";
    }

    @Override
    protected String getOrderBy() {
        return "create_time desc";
    }
}
