package com.zlt.aps.mdm.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.security.annotation.RequiresPermissions;
import com.zlt.aps.mdm.mapper.MdmSkuScheduleCategoryEntityMapper;
import com.zlt.aps.mdm.service.IMdmSkuScheduleCategoryService;
import com.zlt.aps.mdm.api.domain.entity.MdmSkuScheduleCategory;
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
 * 文件名称：MdmSkuScheduleCategoryController.java
 * 描    述：SKU排产分类 控制层类：....
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-12-11
 */
@Slf4j
@Api(tags = "SKU排产分类")
@RestController
@RequestMapping("/mdmSkuScheduleCategory")
public class MdmSkuScheduleCategoryController extends AbstractDocBizController<MdmSkuScheduleCategory> {

    @Autowired
    private IMdmSkuScheduleCategoryService mdmSkuScheduleCategoryService;

    @Autowired
    private MdmSkuScheduleCategoryEntityMapper entityMapper;

    /**
     * 查询SKU排产分类列表
     */
    @RequiresPermissions("mdm:mdmSkuScheduleCategory:list")
    @ApiOperation("查询列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody MdmSkuScheduleCategory queryVO) {
        return super.list(queryVO);
    }

    @Override
    protected String getOrderBy() {
        return "create_time desc";
    }

    /**
     * 保存
     */
    @Log(title = "ui.data.column.mdmSkuScheduleCategory.modelName", businessType = BusinessType.INSERT_OR_UPDATE)
    @RequiresPermissions("mdm:mdmSkuScheduleCategory:save")
    @ApiOperation("保存")
    @PostMapping("/save")
    @Override
    public AjaxResult save(@RequestBody MdmSkuScheduleCategory billVO) {
        return super.save(billVO);
    }

    /**
     * 删除
     */
    @Log(title = "ui.data.column.mdmSkuScheduleCategory.modelName", businessType = BusinessType.DELETE)
    @RequiresPermissions("mdm:mdmSkuScheduleCategory:remove")
    @ApiOperation("删除")
    @DeleteMapping("/remove")
    @Override
    public AjaxResult removeByIds(@RequestBody List<Long> ids) {
        return super.removeByIds(ids);
    }


    /**
     * 获取SKU排产分类详细信息
     */
    @RequiresPermissions("mdm:mdmSkuScheduleCategory:query")
    @ApiOperation("获取详细信息")
    @GetMapping(value = "/{billId}")
    @Override
    public MdmSkuScheduleCategory getInfo(@PathVariable("billId") Long billId) {
        return super.getInfo(billId);
    }


    /**
     * 根据集合导入SKU排产分类数据
     *
     * @param importContext 导入上下文
     * @param updateSupport 已存在记录是否更新
     * @return 结果
     */
    @RequiresPermissions("mdm:mdmSkuScheduleCategory:import")
    @Log(title = "ui.data.column.mdmSkuScheduleCategory.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    @Override
    public AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        return super.importData(importContext, updateSupport);
    }

    /**
     * 导出列表
     */
    @RequiresPermissions("mdm:mdmSkuScheduleCategory:export")
    @Log(title = "SKU排产分类", businessType = BusinessType.EXPORT)
    @ApiOperation("导入数据")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody MdmSkuScheduleCategory queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return super.exportData(queryVO, fileName, response);
    }

    @Override
    protected List<MdmSkuScheduleCategory> listExportData(MdmSkuScheduleCategory obj) {
        QueryWrapper<MdmSkuScheduleCategory> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, obj);
        return entityMapper.selectList(wrapper);
    }

    @Override
    protected IDocService getDocService() {
        return mdmSkuScheduleCategoryService;
    }

    /**
     * 条件拼接
     *
     * @param queryWrapper
     * @param queryVO
     */
    @Override
    protected void builderCondition(QueryWrapper<MdmSkuScheduleCategory> queryWrapper, MdmSkuScheduleCategory queryVO) {
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("factoryCode")), "FACTORY_CODE", queryVO.getFieldValueByFieldName("factoryCode"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("materialCode")), "MATERIAL_CODE", queryVO.getFieldValueByFieldName("materialCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("scheduleType")), "SCHEDULE_TYPE", queryVO.getFieldValueByFieldName("scheduleType"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("genrateDate")), "GENRATE_DATE", queryVO.getFieldValueByFieldName("genrateDate"));
    }

    @Override
    protected String getTypeCode() {
        return "MDM0146";
    }


}
