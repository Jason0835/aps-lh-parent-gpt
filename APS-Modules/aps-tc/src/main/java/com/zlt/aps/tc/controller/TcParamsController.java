package com.zlt.aps.tc.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.constant.FactoryConstant;
import com.zlt.aps.tc.api.domain.entity.TcParams;
import com.zlt.aps.tc.mapper.TcParamsMapper;
import com.zlt.aps.tc.service.ITcParamsService;
import com.zlt.bill.common.controller.AbstractDocBizController;
import com.zlt.bill.common.service.IDocService;
import com.zlt.common.utils.PubUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jodd.util.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：TcParamsController.java
 * 描    述：胎侧排程参数配置 控制层类
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2026-07-07
 */
@Slf4j
@Api(tags = "胎侧排程参数配置")
@RestController
@RequestMapping("/tcParams")
public class TcParamsController extends AbstractDocBizController<TcParams> {

    @Autowired
    private ITcParamsService tcParamsService;

    @Resource
    private TcParamsMapper tcParamsMapper;

    @ApiOperation("查询列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody TcParams queryVO) {
        return super.list(queryVO);
    }

    @Log(title = "ui.data.column.tc.Params.modelName", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("保存")
    @PostMapping("/save")
    @Override
    public AjaxResult save(@RequestBody TcParams billVO) {
        if (StringUtil.isBlank(billVO.getFactoryCode())) {
            billVO.setFactoryCode(FactoryConstant.DEFAULT_FACTORY_CODE);
        }
        return super.save(billVO);
    }

    @Log(title = "ui.data.column.tc.Params.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除")
    @DeleteMapping("/remove")
    @Override
    public AjaxResult removeByIds(@RequestBody List<Long> ids) {
        return super.removeByIds(ids);
    }

    @ApiOperation("获取详细信息")
    @GetMapping(value = "/{id}")
    @Override
    public TcParams getInfo(@PathVariable("id") Long id) {
        return super.getInfo(id);
    }

    @ApiOperation("校验唯一性")
    @PostMapping("/checkUnique")
    public String checkUnique(@RequestBody TcParams query) {
        return tcParamsService.checkUnique(query);
    }

    @ApiOperation("根据参数编码查询")
    @GetMapping("/selectOneByParamCode")
    public AjaxResult selectOneByParamCode(@RequestParam("paramCode") String paramCode, @RequestParam("factoryCode") String factoryCode) {
        return AjaxResult.success(tcParamsService.selectOneByParamCode(paramCode, factoryCode));
    }

    @ApiOperation("查询参数Map")
    @GetMapping("/listTcParams")
    public AjaxResult listTcParams(@RequestParam("factoryCode") String factoryCode) {
        return AjaxResult.success(tcParamsService.listTcParams(factoryCode));
    }

    @Log(title = "ui.data.column.tc.Params.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    @Override
    public AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        return super.importData(importContext, updateSupport);
    }

    @Log(title = "ui.data.column.tc.Params.modelName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出数据")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody TcParams queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return super.exportData(queryVO, fileName, response);
    }

    @Override
    protected List<TcParams> listExportData(TcParams obj) {
        QueryWrapper<TcParams> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, obj);
        return tcParamsMapper.selectList(wrapper);
    }

    @Override
    protected IDocService getDocService() {
        return tcParamsService;
    }

    @Override
    protected void builderCondition(QueryWrapper<TcParams> queryWrapper, TcParams queryVO) {
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("factoryCode")), "FACTORY_CODE", queryVO.getFieldValueByFieldName("factoryCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("paramCode")), "PARAM_CODE", queryVO.getFieldValueByFieldName("paramCode"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("paramName")), "PARAM_NAME", queryVO.getFieldValueByFieldName("paramName"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("paramGroup")), "PARAM_GROUP", queryVO.getFieldValueByFieldName("paramGroup"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("valueType")), "VALUE_TYPE", queryVO.getFieldValueByFieldName("valueType"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("enableStatus")), "ENABLE_STATUS", queryVO.getFieldValueByFieldName("enableStatus"));
    }

    @Override
    protected String getTypeCode() {
        return "TC0901";
    }

    @Override
    protected String getOrderBy() {
        return "create_time desc";
    }
}
