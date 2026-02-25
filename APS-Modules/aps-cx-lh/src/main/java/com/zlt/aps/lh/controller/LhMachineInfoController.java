package com.zlt.aps.lh.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.utils.SecurityUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.lh.service.ILhMachineInfoService;
import com.zlt.aps.maindata.mapper.LhMachineInfoEntityMapper;
import com.zlt.aps.mp.api.domain.entity.LhMachineInfo;
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
import java.util.Date;
import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：LhMachineInfoController.java
 * 描    述：硫化机台信息 控制层类：....
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-03-07
 */
@Slf4j
@Api(tags = "硫化机台信息")
@RestController
@RequestMapping("/info")
public class LhMachineInfoController extends AbstractDocBizController<LhMachineInfo> {

    @Autowired
    private ILhMachineInfoService lhMachineInfoService;

    @Resource
    private LhMachineInfoEntityMapper lhMachineInfoEntityMapper;

    /**
     * 查询硫化机台信息列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody LhMachineInfo queryVO) {
        return super.list(queryVO);
    }


    /**
     * 保存
     */
    @Log(title = "ui.data.column.info.modelName", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("保存")
    @PostMapping("/save")
    @Override
    public AjaxResult save(@RequestBody LhMachineInfo lhMachineInfo) {
        if (UserConstants.NOT_UNIQUE.equals(lhMachineInfoService.checkUnique(lhMachineInfo))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.error.message.quota.unique"));
        }
        return super.save(lhMachineInfo);
    }

    /**
     * 删除
     */
    @Log(title = "ui.data.column.info.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除")
    @DeleteMapping("/remove")
    @Override
    public AjaxResult removeByIds(@RequestBody List<Long> ids) {
        LambdaUpdateWrapper<LhMachineInfo> wrapper = new LambdaUpdateWrapper<>();
        wrapper.set(BaseEntity::getIsDelete, null)
                .set(BaseEntity::getUpdateBy, SecurityUtils.getUsername())
                .set(BaseEntity::getUpdateTime, new Date())
                .in(BaseEntity::getId, ids);
        return toAjax(lhMachineInfoEntityMapper.update(null, wrapper));
    }


    /**
     * 获取硫化机台信息详细信息
     */
    @ApiOperation("获取详细信息")
    @GetMapping(value = "/{billId}")
    @Override
    public LhMachineInfo getInfo(@PathVariable("billId") Long billId) {
        return super.getInfo(billId);
    }


    /**
     * 根据集合导入硫化机台信息数据
     *
     * @param importContext 导入上下文
     * @param updateSupport 已存在记录是否更新
     * @return 结果
     */
    @Log(title = "ui.data.column.info.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    @Override
    public AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        return super.importData(importContext, updateSupport);
    }

    /**
     * 导出列表
     */
    @Log(title = "硫化机台信息", businessType = BusinessType.EXPORT)
    @ApiOperation("导入数据")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody LhMachineInfo queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return super.exportData(queryVO, fileName, response);
    }

    @Override
    public List<LhMachineInfo> listExportData(LhMachineInfo port) {
        startPage("create_time desc");
        QueryWrapper<LhMachineInfo> queryWrapper = new QueryWrapper<>();
        this.builderCondition(queryWrapper, port);
        return lhMachineInfoService.selectListExportData(queryWrapper);
    }

    @Override
    protected IDocService getDocService() {
        return lhMachineInfoService;
    }

    /**
     * 条件拼接
     *
     * @param queryWrapper
     * @param queryVO
     */
    @Override
    protected void builderCondition(QueryWrapper<LhMachineInfo> queryWrapper, LhMachineInfo queryVO) {
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("machineCode")), "MACHINE_CODE", queryVO.getFieldValueByFieldName("machineCode"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("machineName")), "MACHINE_NAME", queryVO.getFieldValueByFieldName("machineName"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("dimension")), "DIMENSION", queryVO.getFieldValueByFieldName("dimension"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("centripetalMechanism")), "CENTRIPETAL_MECHANISM", queryVO.getFieldValueByFieldName("centripetalMechanism"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("dimensionMinimum")), "DIMENSION_MINIMUM", queryVO.getFieldValueByFieldName("dimensionMinimum"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("dimensionMaximum")), "DIMENSION_MAXIMUM", queryVO.getFieldValueByFieldName("dimensionMaximum"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("classShift")), "CLASS_SHIFT", queryVO.getFieldValueByFieldName("classShift"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("maxMoldNum")), "MAX_MOLD_NUM", queryVO.getFieldValueByFieldName("maxMoldNum"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("quota")), "QUOTA", queryVO.getFieldValueByFieldName("quota"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("openMachineClass")), "OPEN_MACHINE_CLASS", queryVO.getFieldValueByFieldName("openMachineClass"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("status")), "STATUS", queryVO.getFieldValueByFieldName("status"));
    }


    @Override
    protected String getTypeCode() {
        return "0114";
    }

    @Override
    protected String getOrderBy() {
        return "create_time, id desc";
    }

}
