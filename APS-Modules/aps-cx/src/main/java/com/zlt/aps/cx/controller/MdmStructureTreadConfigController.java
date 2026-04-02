package com.zlt.aps.cx.controller;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.maindata.mapper.MdmStructureTreadConfigEntityMapper;
import com.zlt.aps.maindata.service.IMdmStructureTreadConfigService;
import com.zlt.aps.mdm.api.domain.entity.MdmStructureTreadConfig;
import com.zlt.bill.common.controller.AbstractDocBizController;
import com.zlt.bill.common.service.IDocService;
import com.zlt.common.utils.PubUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
* Copyright (c) 2022, All rights reserved。
* 文件名称：MdmStructureTreadConfigController.java
* 描    述：胎面整车配置 控制层类
*@author APS Team
*@date 2026-04-02
*@version 1.0
*
 * 修改记录：
*     修改时间：...
*     修 改 人：...
*     修改内容：...
*/
@Slf4j
@Api(tags = "胎面整车配置")
@RestController
@RequestMapping("/mdmStructureTreadConfig")
public class MdmStructureTreadConfigController extends AbstractDocBizController<MdmStructureTreadConfig> {

    @Autowired
    private IMdmStructureTreadConfigService mdmStructureTreadConfigService;

    @Resource
    private MdmStructureTreadConfigEntityMapper mdmStructureTreadConfigEntityMapper;

    /**
     * 查询胎面整车配置列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody MdmStructureTreadConfig queryVO) {
        return super.list(queryVO);
    }

    /**
     * 保存
     */
    @Log(title = "ui.data.column.mdmStructureTreadConfig.modelName", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("保存")
    @PostMapping("/save")
    @Override
    public AjaxResult save(@RequestBody MdmStructureTreadConfig entity) {
        return super.save(entity);
    }

    /**
     * 删除胎面整车配置
     */
    @Log(title = "ui.data.column.mdmStructureTreadConfig.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除")
    @DeleteMapping("/remove")
    @Override
    public AjaxResult removeByIds(@RequestBody List<Long> ids) {
        return super.removeByIds(ids);
    }

    /**
     * 获取胎面整车配置详细信息
     */
    @ApiOperation("获取详细信息")
    @GetMapping(value = "/{billId}")
    @Override
    public MdmStructureTreadConfig getInfo(@PathVariable("billId") Long billId) {
        return super.getInfo(billId);
    }

    /**
     * 根据集合导入胎面整车配置数据
     * @param importContext 导入上下文
     * @param updateSupport 已存在记录是否更新
     * @return 结果
     */
    @Log(title = "ui.data.column.mdmStructureTreadConfig.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    @Override
    public AjaxResult importData(@RequestBody com.ruoyi.api.gateway.system.domain.vo.ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        return super.importData(importContext, updateSupport);
    }

    /**
     * 导出胎面整车配置列表
     */
    @Log(title = "ui.data.column.mdmStructureTreadConfig.modelName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出数据")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody MdmStructureTreadConfig queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return super.exportData(queryVO, fileName, response);
    }

    @Override
    protected List<MdmStructureTreadConfig> listExportData(MdmStructureTreadConfig obj) {
        QueryWrapper<MdmStructureTreadConfig> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, obj);
        return mdmStructureTreadConfigEntityMapper.selectList(wrapper);
    }

    @Override
    protected IDocService getDocService() {
        return mdmStructureTreadConfigService;
    }

    @Override
    protected void builderCondition(QueryWrapper<MdmStructureTreadConfig> queryWrapper, MdmStructureTreadConfig queryVO) {
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("factoryCode")), "FACTORY_CODE", queryVO.getFieldValueByFieldName("factoryCode"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("structureCode")), "STRUCTURE_CODE", queryVO.getFieldValueByFieldName("structureCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("treadCount")), "TREAD_COUNT", queryVO.getFieldValueByFieldName("treadCount"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("dataVersion")), "DATA_VERSION", queryVO.getFieldValueByFieldName("dataVersion"));
    }

    @Override
    protected String getTypeCode() {
        return "0";
    }

    @Override
    protected String getOrderBy() {
        return "create_time desc";
    }
}
