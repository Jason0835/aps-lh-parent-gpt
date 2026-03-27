package com.zlt.aps.lh.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.lh.api.domain.entity.LhDayFinishQty;
import com.zlt.aps.lh.service.ILhDayFinishQtyService;
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
* 文件名称：LhDayFinishQtyController.java
* 描    述：硫化排程日完成量 控制层类：....
*@author zlt
*@date 2025-02-21
*@version 1.0
*
 *  修改记录：
*     修改时间：...
*     修 改 人：zlt
*     修改内容：...
*/
@Slf4j
@Api(tags = "硫化排程日完成量")
@RestController
@RequestMapping("/lhDayFinishQty")
public class LhDayFinishQtyController extends AbstractDocBizController<LhDayFinishQty> {

    @Autowired
    private ILhDayFinishQtyService lhDayFinishQtyService;

    /**
     * 查询硫化排程日完成量列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody LhDayFinishQty queryVO) {
        return super.list(queryVO);
    }


    /**
     * 保存
     */
    @Log(title = "ui.data.column.lhDayFinishQty.modelName", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("保存")
    @PostMapping("/save")
    @Override
    public AjaxResult save(@RequestBody LhDayFinishQty billVO){
        return super.save(billVO);
    }

    /**
     * 删除
     */
    @Log(title = "ui.data.column.lhDayFinishQty.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除")
    @DeleteMapping("/remove")
    @Override
    public AjaxResult removeByIds(@RequestBody List<Long> ids){
        return super.removeByIds(ids);
    }


    /**
     * 获取硫化排程日完成量详细信息
     */
    @ApiOperation("获取详细信息")
    @GetMapping(value = "/{billId}")
    @Override
    public LhDayFinishQty getInfo(@PathVariable("billId") Long billId) {
        return super.getInfo(billId);
    }


    /**
     * 根据集合导入硫化排程日完成量数据
     * @param importContext 导入上下文
     * @param updateSupport 已存在记录是否更新
     * @return 结果
     */
    @Log(title = "ui.data.column.lhDayFinishQty.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData/{updateSupport}")
    @Override
    public AjaxResult importData(@RequestBody ImportContext importContext, @PathVariable("updateSupport") boolean updateSupport) throws Exception {
        return super.importData(importContext,updateSupport);
    }

    /**
     * 导出列表
     */
    @Log(title = "硫化排程日完成量", businessType = BusinessType.EXPORT)
    @ApiOperation("导入数据")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody LhDayFinishQty queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return super.exportData(queryVO, fileName, response);
    }

    @Override
    protected IDocService getDocService(){
        return lhDayFinishQtyService;
    }

    /**
     * 条件拼接
     *
     * @param queryWrapper
     * @param queryVO
     */
    @Override
    protected void builderCondition(QueryWrapper<LhDayFinishQty> queryWrapper, LhDayFinishQty queryVO) {
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("finishDate")), "FINISH_DATE", queryVO.getFieldValueByFieldName("finishDate"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("specsCode")), "SPECS_CODE", queryVO.getFieldValueByFieldName("specsCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("dayFinishQty")), "DAY_FINISH_QTY", queryVO.getFieldValueByFieldName("dayFinishQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("factoryCode")), "FACTORY_CODE", queryVO.getFieldValueByFieldName("factoryCode"));
    }


    @Override
    protected String getTypeCode(){
        return "0105";
    }

    @Override
    protected String getOrderBy() {
        return "create_time desc";
    }

    /**
     * 导入完成量
     *
     * @param list        完成量集合
     * @param importLogId 导入记录id
     * @return 结果
     */
    @PostMapping("/importFinishQty")
    @ApiOperation("导入完成量")
    public AjaxResult importFinishQty(@RequestBody List<LhDayFinishQty> list, @RequestParam("importLogId") Long importLogId) {
        if (StringUtils.isNull(list) || list.isEmpty()) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        return lhDayFinishQtyService.importFinishQty(list, importLogId);
    }
}
