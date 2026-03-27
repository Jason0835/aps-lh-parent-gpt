package com.zlt.aps.lh.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.lh.api.domain.entity.LhParams;
import com.zlt.aps.lh.api.domain.entity.LhSpecifyMachine;
import com.zlt.aps.lh.service.ILhParamsService;
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
 * 文件名称：LhParamsController.java
 * 描    述：硫化参数信息 控制层类：....
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-02-14
 */
@Slf4j
@Api(tags = "硫化参数信息")
@RestController
@RequestMapping("/lhParams")
public class LhParamsController extends AbstractDocBizController<LhParams> {

    @Autowired
    private ILhParamsService lhParamsService;


    /**
     * 查询硫化参数信息列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody LhParams queryVO) {
        return super.list(queryVO);
    }


    /**
     * 保存
     */
    @Log(title = "ui.data.column.lhParams.modelName", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("保存")
    @PostMapping("/save")
    @Override
    public AjaxResult save(@RequestBody LhParams billVO) {
        return super.save(billVO);
    }

    /**
     * 删除
     */
    @Log(title = "ui.data.column.lhParams.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除")
    @DeleteMapping("/remove")
    @Override
    public AjaxResult removeByIds(@RequestBody List<Long> ids) {
        return super.removeByIds(ids);
    }


    /**
     * 获取硫化参数信息详细信息
     */
    @ApiOperation("获取详细信息")
    @GetMapping(value = "/{id}")
    @Override
    public LhParams getInfo(@PathVariable("id") Long id) {
        return super.getInfo(id);
    }

    /**
     * 校验预计超欠产唯一性
     */
    @ApiOperation("校验预计超欠产唯一性")
    @PostMapping("/checkUnique")
    public String checkUnique(@RequestBody LhParams query) {
        return lhParamsService.checkUnique(query);
    }

    /**
     * 根据集合导入硫化参数信息数据
     *
     * @param importContext 导入上下文
     * @param updateSupport 已存在记录是否更新
     * @return 结果
     */
    @Log(title = "ui.data.column.lhParams.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData/{updateSupport}")
    @Override
    public AjaxResult importData(@RequestBody ImportContext importContext, @PathVariable("updateSupport") boolean updateSupport) throws Exception {
        return super.importData(importContext, updateSupport);
    }

    /**
     * 导出列表
     */
    @Log(title = "硫化参数信息", businessType = BusinessType.EXPORT)
    @ApiOperation("导出数据")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody LhParams queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return super.exportData(queryVO, fileName, response);
    }

    @Override
    protected List<LhParams> listExportData(LhParams obj) {
        QueryWrapper<LhParams> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, obj);
        return lhParamsService.selectList(wrapper);
    }

    @Override
    protected IDocService getDocService() {
        return lhParamsService;
    }

    /**
     * 条件拼接
     *
     * @param queryWrapper
     * @param queryVO
     */
    @Override
    protected void builderCondition(QueryWrapper<LhParams> queryWrapper, LhParams queryVO) {
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("factoryCode")), "FACTORY_CODE", queryVO.getFieldValueByFieldName("factoryCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("paramCode")), "PARAM_CODE", queryVO.getFieldValueByFieldName("paramCode"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("paramName")), "PARAM_NAME", queryVO.getFieldValueByFieldName("paramName"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("paramValue")), "PARAM_VALUE", queryVO.getFieldValueByFieldName("paramValue"));
    }


    @Override
    protected String getTypeCode() {
        return "0101";
    }

    @Override
    protected String getOrderBy() {
        return "create_time desc";
    }

}
