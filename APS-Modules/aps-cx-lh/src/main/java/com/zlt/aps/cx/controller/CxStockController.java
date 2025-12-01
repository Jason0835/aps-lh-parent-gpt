package com.zlt.aps.cx.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.security.annotation.RequiresPermissions;
import com.zlt.aps.cx.mapper.entity.CxStockEntityMapper;
import com.zlt.aps.cx.service.ICxStockService;
import com.zlt.aps.cxlh.cx.api.domain.entity.CxStock;
import com.zlt.bill.common.controller.AbstractDocBizController;
import com.zlt.bill.common.service.IDocService;
import com.zlt.common.utils.PubUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
* Copyright (c) 2022, All rights reserved。
* 文件名称：CxStockController.java
* 描    述：成型库存信息 控制层类：....
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
@Api(tags = "成型库存信息")
@RestController
@RequestMapping("/cxStock")
public class CxStockController extends AbstractDocBizController<CxStock> {

    @Autowired
    private ICxStockService cxStockService;

    @Autowired
    private CxStockEntityMapper cxStockEntityMapper;

    /**
     * 查询成型库存信息列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody CxStock queryVO) {
        this.startPage(this.getOrderBy());
        QueryWrapper<CxStock> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, queryVO);
        List<CxStock> list = cxStockEntityMapper.selectList(wrapper);
        for (CxStock cxStock : list) {
            int stockNum = cxStock.getStockNum() == null ? 0 : cxStock.getStockNum();
            int modifyNum = cxStock.getModifyNum() == null ? 0 : cxStock.getModifyNum();
            int badNum = cxStock.getBadNum() == null ? 0 : cxStock.getBadNum();
            int result = stockNum + modifyNum - badNum;
            cxStock.setScheduleUseStock((long) result);
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
    @Log(title = "ui.data.column.cxStock.modelName", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("保存")
    @PostMapping("/save")
    @Override
    public AjaxResult save(@RequestBody CxStock billVO){
        return super.save(billVO);
    }

    /**
     * 删除
     */
    @Log(title = "ui.data.column.cxStock.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除")
    @DeleteMapping("/remove")
    @Override
    public AjaxResult removeByIds(@RequestBody List<Long> ids){
        return super.removeByIds(ids);
    }


    /**
     * 获取成型库存信息详细信息
     */
    @ApiOperation("获取详细信息")
    @GetMapping(value = "/{billId}")
    @Override
    public CxStock getInfo(@PathVariable("billId") Long billId) {
        return super.getInfo(billId);
    }


    @Log(title = "ui.cx.stock.export.fileName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    public AjaxResult importData(@RequestBody List<CxStock> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        if (CollectionUtils.isEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        return cxStockService.importData(list, updateSupport, importLogId);
    }

    /**
     * 导出列表
     */
    @Log(title = "成型库存信息", businessType = BusinessType.EXPORT)
    @ApiOperation("导入数据")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody CxStock queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return super.exportData(queryVO, fileName, response);
    }

    @Override
    protected List<CxStock> listExportData(CxStock obj) {
        QueryWrapper<CxStock> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, obj);
        List<CxStock> list = cxStockEntityMapper.selectList(wrapper);
        for (CxStock cxStock : list) {
            int stockNum = cxStock.getStockNum() == null ? 0 : cxStock.getStockNum();
            int modifyNum = cxStock.getModifyNum() == null ? 0 : cxStock.getModifyNum();
            int badNum = cxStock.getBadNum() == null ? 0 : cxStock.getBadNum();
            int result = stockNum + modifyNum - badNum;
            cxStock.setScheduleUseStock((long) result);
        }
        return list;
    }

    @Override
    protected IDocService getDocService(){
        return cxStockService;
    }

    /**
     * 条件拼接
     *
     * @param queryWrapper
     * @param queryVO
     */
    @Override
    protected void builderCondition(QueryWrapper<CxStock> queryWrapper, CxStock queryVO) {
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("stockDate")), "STOCK_DATE", queryVO.getFieldValueByFieldName("stockDate"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("embryoCode")), "EMBRYO_CODE", queryVO.getFieldValueByFieldName("embryoCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("stockNum")), "STOCK_NUM", queryVO.getFieldValueByFieldName("stockNum"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("overTimeStock")), "OVER_TIME_STOCK", queryVO.getFieldValueByFieldName("overTimeStock"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("modifyNum")), "MODIFY_NUM", queryVO.getFieldValueByFieldName("modifyNum"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("badNum")), "BAD_NUM", queryVO.getFieldValueByFieldName("badNum"));
    }


    @Override
    protected String getTypeCode(){
        return "9002CX";
    }

    /**
     * 新增成型库存信息
     */
    @Log(title = "ui.cx.stock.export.fileName", businessType = BusinessType.INSERT)
    @ApiOperation("新增成型库存信息（id不为空）")
    @PostMapping("/add")
    public AjaxResult add(@RequestBody CxStock cxStock) {
        //唯一性校验（使用库存日期+物料编号为逻辑主键）
        String unique = cxStockService.checkUnique(cxStock);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.stock.message.unique"));
        } else {
            return this.save(cxStock);
        }
    }

    /**
     * 修改成型库存信息
     */
    @Log(title = "ui.cx.stock.export.fileName", businessType = BusinessType.UPDATE)
    @ApiOperation("修改成型库存信息（id不为空）")
    @PutMapping("/edit")
    public AjaxResult edit(@RequestBody CxStock cxStock) {
        //唯一性校验（使用库存日期+物料编号为逻辑主键）
        String unique = cxStockService.checkUnique(cxStock);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.stock.message.unique"));
        } else {
            return this.save(cxStock);
        }
    }

    /**
     * 查询列表
     */
    @Log(title = "ui.cx.stock.export.fileName", businessType = BusinessType.EXPORT)
    @PostMapping("/exportList")
    public List<CxStock> exportList(@RequestBody CxStock query) {
        QueryWrapper<CxStock> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, query);
        List<CxStock> list =  cxStockEntityMapper.selectList(wrapper);
        for (CxStock cxStock : list) {
            int stockNum = cxStock.getStockNum() == null ? 0 : cxStock.getStockNum();
            int modifyNum = cxStock.getModifyNum() == null ? 0 : cxStock.getModifyNum();
            int badNum = cxStock.getBadNum() == null ? 0 : cxStock.getBadNum();
            int result = stockNum + modifyNum - badNum;
            cxStock.setScheduleUseStock((long) result);
        }
        return list;
    }
}
