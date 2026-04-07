package com.zlt.aps.mp.setting.controller;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.maindata.mapper.MdmMonCycleSchStruConfEntityMapper;
import com.zlt.aps.maindata.service.IMdmMonCycleSchStruConfService;
import com.zlt.aps.mp.api.domain.entity.MdmCycleSchStruConf;
import com.zlt.aps.mp.api.domain.entity.MdmMonCycleSchStruConf;
import com.zlt.bill.common.controller.AbstractDocBizController;
import com.zlt.bill.common.service.IDocService;
import com.zlt.common.utils.PubUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MdmMonCycleSchStruConfController.java
 * 描    述：月周期排产结构配置 控制层类：....
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
@Api(tags = "月周期排产结构配置")
@RestController
@RequestMapping("/mdmMonCycleSchStruConf")
public class MdmMonCycleSchStruConfController extends AbstractDocBizController<MdmMonCycleSchStruConf> {

    @Autowired
    private IMdmMonCycleSchStruConfService mdmMonCycleSchStruConfService;

    @Autowired
    private MdmMonCycleSchStruConfEntityMapper entityMapper;

    /**
     * 查询月周期排产结构配置列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody MdmMonCycleSchStruConf queryVO) {
        return super.list(queryVO);
    }

    @Override
    protected String getOrderBy() {
        return "create_time desc";
    }

    /**
     * 保存
     */
    @Log(title = "ui.data.column.mdmMonCycleSchStruConf.modelName", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("保存")
    @PostMapping("/save")
    @Override
    public AjaxResult save(@RequestBody MdmMonCycleSchStruConf billVO) {
        return super.save(billVO);
    }

    /**
     * 新增保存
     */
    @Log(title = "ui.data.column.mdmMonCycleSchStruConf.modelName", businessType = BusinessType.INSERT)
    @ApiOperation("新增保存")
    @PostMapping("/addSave")
    public AjaxResult addSave(@RequestBody MdmMonCycleSchStruConf billVO) {
        return mdmMonCycleSchStruConfService.addSave(billVO);
    }

    /**
     * 查询可新增结构列表
     */
    @ApiOperation("查询可新增结构列表")
    @PostMapping("/queryAddStructList")
    public List<MdmMonCycleSchStruConf> queryAddStructList(@RequestBody MdmCycleSchStruConf queryVO) {
        return mdmMonCycleSchStruConfService.queryAddStructList(queryVO);
    }

    /**
     * 删除
     */
    @Log(title = "ui.data.column.mdmMonCycleSchStruConf.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除")
    @DeleteMapping("/remove")
    @Override
    public AjaxResult removeByIds(@RequestBody List<Long> ids) {
        return super.removeByIds(ids);
    }


    /**
     * 获取月周期排产结构配置详细信息
     */
    @ApiOperation("获取详细信息")
    @GetMapping(value = "/{billId}")
    @Override
    public MdmMonCycleSchStruConf getInfo(@PathVariable("billId") Long billId) {
        return super.getInfo(billId);
    }


    /**
     * 根据集合导入月周期排产结构配置数据
     *
     * @param importContext 导入上下文
     * @param updateSupport 已存在记录是否更新
     * @return 结果
     */
    @Log(title = "ui.data.column.mdmMonCycleSchStruConf.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    @Override
    public AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        return super.importData(importContext, updateSupport);
    }

    /**
     * 导出列表
     */
    @Log(title = "月周期排产结构配置", businessType = BusinessType.EXPORT)
    @ApiOperation("导入数据")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody MdmMonCycleSchStruConf queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return super.exportData(queryVO, fileName, response);
    }

    @Override
    protected List<MdmMonCycleSchStruConf> listExportData(MdmMonCycleSchStruConf obj) {
        QueryWrapper<MdmMonCycleSchStruConf> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, obj);
        List<MdmMonCycleSchStruConf> list = entityMapper.selectList(wrapper);
        this.translationList(list);
        return list;
    }

    private void translationList(List<MdmMonCycleSchStruConf> list) {
        if(CollectionUtils.isEmpty(list)) {
            return;
        }
        for (MdmMonCycleSchStruConf item : list) {
            item.setUpdateDate(DateUtil.formatDateTime(item.getUpdateTime()));
        }
    }

    @Override
    protected IDocService getDocService() {
        return mdmMonCycleSchStruConfService;
    }

    /**
     * 条件拼接
     *
     * @param queryWrapper
     * @param queryVO
     */
    @Override
    protected void builderCondition(QueryWrapper<MdmMonCycleSchStruConf> queryWrapper, MdmMonCycleSchStruConf queryVO) {
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("factoryCode")), "FACTORY_CODE", queryVO.getFieldValueByFieldName("factoryCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("year")), "YEAR", queryVO.getFieldValueByFieldName("year"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("month")), "MONTH", queryVO.getFieldValueByFieldName("month"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("structureName")), "STRUCTURE_NAME", queryVO.getFieldValueByFieldName("structureName"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("turnoverMonth")), "TURNOVER_MONTH", queryVO.getFieldValueByFieldName("turnoverMonth"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("minVulcanizingMachine")), "MIN_VULCANIZING_MACHINE", queryVO.getFieldValueByFieldName("minVulcanizingMachine"));
    }

    @Override
    protected String getTypeCode() {
        return "MDM0143";
    }


}
