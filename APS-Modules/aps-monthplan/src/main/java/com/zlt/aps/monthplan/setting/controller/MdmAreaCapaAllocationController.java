package com.zlt.aps.monthplan.setting.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.maindata.mapper.MdmAreaCapaAllocationEntityMapper;
import com.zlt.aps.maindata.service.IMdmAreaCapaAllocationService;
import com.zlt.aps.monthplan.api.domain.entity.MdmAreaCapaAllocation;
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
 * 文件名称：MdmAreaCapaAllocationController.java
 * 描    述：区域产能分配 控制层类：....
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-12-08
 */
@Slf4j
@Api(tags = "区域产能分配")
@RestController
@RequestMapping("/mdmAreaCapaAllocation")
public class MdmAreaCapaAllocationController extends AbstractDocBizController<MdmAreaCapaAllocation> {

    @Autowired
    private IMdmAreaCapaAllocationService mdmAreaCapaAllocationService;

    @Autowired
    private MdmAreaCapaAllocationEntityMapper entityMapper;

    /**
     * 查询区域产能分配列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody MdmAreaCapaAllocation queryVO) {
        return super.list(queryVO);
    }

    @Override
    protected String getOrderBy() {
        return "create_time desc";
    }

    /**
     * 保存
     */
    @Log(title = "ui.data.column.mdmAreaCapaAllocation.modelName", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("保存")
    @PostMapping("/save")
    @Override
    public AjaxResult save(@RequestBody MdmAreaCapaAllocation billVO) {
        return super.save(billVO);
    }

    /**
     * 删除
     */
    @Log(title = "ui.data.column.mdmAreaCapaAllocation.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除")
    @DeleteMapping("/remove")
    @Override
    public AjaxResult removeByIds(@RequestBody List<Long> ids) {
        return super.removeByIds(ids);
    }


    /**
     * 获取区域产能分配详细信息
     */
    @ApiOperation("获取详细信息")
    @GetMapping(value = "/{billId}")
    @Override
    public MdmAreaCapaAllocation getInfo(@PathVariable("billId") Long billId) {
        return super.getInfo(billId);
    }


    /**
     * 根据集合导入区域产能分配数据
     *
     * @param importContext 导入上下文
     * @param updateSupport 已存在记录是否更新
     * @return 结果
     */
    @Log(title = "ui.data.column.mdmAreaCapaAllocation.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    @Override
    public AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        return super.importData(importContext, updateSupport);
    }

    /**
     * 导出列表
     */
    @Log(title = "区域产能分配", businessType = BusinessType.EXPORT)
    @ApiOperation("导入数据")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody MdmAreaCapaAllocation queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return super.exportData(queryVO, fileName, response);
    }

    @Override
    protected List<MdmAreaCapaAllocation> listExportData(MdmAreaCapaAllocation obj) {
        QueryWrapper<MdmAreaCapaAllocation> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, obj);
        return entityMapper.selectList(wrapper);
    }

    @Override
    protected IDocService getDocService() {
        return mdmAreaCapaAllocationService;
    }

    /**
     * 条件拼接
     *
     * @param queryWrapper
     * @param queryVO
     */
    @Override
    protected void builderCondition(QueryWrapper<MdmAreaCapaAllocation> queryWrapper, MdmAreaCapaAllocation queryVO) {
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("factoryCode")), "FACTORY_CODE", queryVO.getFieldValueByFieldName("factoryCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("productTypeCode")), "PRODUCT_TYPE_CODE", queryVO.getFieldValueByFieldName("productTypeCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("year")), "YEAR", queryVO.getFieldValueByFieldName("year"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("month")), "MONTH", queryVO.getFieldValueByFieldName("month"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("areaCode")), "AREA_CODE", queryVO.getFieldValueByFieldName("areaCode"));
    }

    @Override
    protected String getTypeCode() {
        return "MDM0141";
    }

    /**
     * 复制
     *
     * @param entity 参数
     * @return 结果
     */
    @ApiOperation("复制")
    @PostMapping("/copy")
    public AjaxResult copy(@RequestBody MdmAreaCapaAllocation entity) {
        return mdmAreaCapaAllocationService.copy(entity);
    }

    /**
     * 复制前校验
     *
     * @param entity 参数
     * @return 结果
     */
    @ApiOperation("复制前校验")
    @PostMapping("/checkBeforeCopy")
    public AjaxResult checkBeforeCopy(@RequestBody MdmAreaCapaAllocation entity) {
        return mdmAreaCapaAllocationService.checkBeforeCopy(entity);
    }
}
