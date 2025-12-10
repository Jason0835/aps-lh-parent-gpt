package com.zlt.aps.monthplan.setting.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.security.annotation.RequiresPermissions;
import com.zlt.aps.maindata.mapper.MdmCycleSchStruConfEntityMapper;
import com.zlt.aps.maindata.service.IMdmCycleSchStruConfService;
import com.zlt.aps.monthplan.api.domain.entity.MdmCycleSchStruConf;
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
 * 文件名称：MdmCycleSchStruConfController.java
 * 描    述：周期排产结构配置 控制层类：....
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-12-09
 */
@Slf4j
@Api(tags = "周期排产结构配置")
@RestController
@RequestMapping("/mdmCycleSchStruConf")
public class MdmCycleSchStruConfController extends AbstractDocBizController<MdmCycleSchStruConf> {

    @Autowired
    private IMdmCycleSchStruConfService mdmCycleSchStruConfService;

    @Autowired
    private MdmCycleSchStruConfEntityMapper entityMapper;

    /**
     * 查询周期排产结构配置列表
     */
    @RequiresPermissions("monthplan:mdmCycleSchStruConf:list")
    @ApiOperation("查询列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody MdmCycleSchStruConf queryVO) {
        return super.list(queryVO);
    }

    @Override
    protected String getOrderBy() {
        return "create_time desc";
    }

    /**
     * 保存
     */
    @Log(title = "ui.data.column.mdmCycleSchStruConf.modelName", businessType = BusinessType.INSERT_OR_UPDATE)
    @RequiresPermissions("monthplan:mdmCycleSchStruConf:save")
    @ApiOperation("保存")
    @PostMapping("/save")
    @Override
    public AjaxResult save(@RequestBody MdmCycleSchStruConf billVO) {
        return super.save(billVO);
    }

    /**
     * 删除
     */
    @Log(title = "ui.data.column.mdmCycleSchStruConf.modelName", businessType = BusinessType.DELETE)
    @RequiresPermissions("monthplan:mdmCycleSchStruConf:remove")
    @ApiOperation("删除")
    @DeleteMapping("/remove")
    @Override
    public AjaxResult removeByIds(@RequestBody List<Long> ids) {
        return super.removeByIds(ids);
    }


    /**
     * 获取周期排产结构配置详细信息
     */
    @RequiresPermissions("monthplan:mdmCycleSchStruConf:query")
    @ApiOperation("获取详细信息")
    @GetMapping(value = "/{billId}")
    @Override
    public MdmCycleSchStruConf getInfo(@PathVariable("billId") Long billId) {
        return super.getInfo(billId);
    }


    /**
     * 根据集合导入周期排产结构配置数据
     *
     * @param importContext 导入上下文
     * @param updateSupport 已存在记录是否更新
     * @return 结果
     */
    @RequiresPermissions("monthplan:mdmCycleSchStruConf:import")
    @Log(title = "ui.data.column.mdmCycleSchStruConf.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    @Override
    public AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        return super.importData(importContext, updateSupport);
    }

    /**
     * 导出列表
     */
    @RequiresPermissions("monthplan:mdmCycleSchStruConf:export")
    @Log(title = "周期排产结构配置", businessType = BusinessType.EXPORT)
    @ApiOperation("导入数据")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody MdmCycleSchStruConf queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return super.exportData(queryVO, fileName, response);
    }

    @Override
    protected List<MdmCycleSchStruConf> listExportData(MdmCycleSchStruConf obj) {
        QueryWrapper<MdmCycleSchStruConf> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, obj);
        return entityMapper.selectList(wrapper);
    }

    @Override
    protected IDocService getDocService() {
        return mdmCycleSchStruConfService;
    }

    /**
     * 条件拼接
     *
     * @param queryWrapper
     * @param queryVO
     */
    @Override
    protected void builderCondition(QueryWrapper<MdmCycleSchStruConf> queryWrapper, MdmCycleSchStruConf queryVO) {
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("factoryCode")), "FACTORY_CODE", queryVO.getFieldValueByFieldName("factoryCode"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("structureName")), "STRUCTURE_NAME", queryVO.getFieldValueByFieldName("structureName"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("turnoverMonth")), "TURNOVER_MONTH", queryVO.getFieldValueByFieldName("turnoverMonth"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("minVulcanizingMachine")), "MIN_VULCANIZING_MACHINE", queryVO.getFieldValueByFieldName("minVulcanizingMachine"));
    }

    @Override
    protected String getTypeCode() {
        return "MDM0142";
    }


    /**
     * 生成月周期排产结构配置
     *
     * @param mdmCycleSchStruConf 参数
     * @return 结果
     */
    @ApiOperation("生成月周期排产结构配置")
    @PostMapping("/genMonthCycleSchStruConf")
    public AjaxResult genMonthCycleSchStruConf(@RequestBody MdmCycleSchStruConf mdmCycleSchStruConf) {
        return mdmCycleSchStruConfService.genMonthCycleSchStruConf(mdmCycleSchStruConf);
    }
}
