package com.zlt.aps.mp.mdm.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.maindata.service.IFixedPointConfigurationService;
import com.zlt.aps.monthplan.api.domain.entity.FixedPointConfiguration;
import com.zlt.common.utils.PubUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：FixedPointConfigurationController.java
 * 描    述：基础数据-定点机台主 控制层类：....
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-02-21
 */
@Slf4j
@RestController
@RequestMapping("/fixedPointConfiguration")
@Api(tags = "基础数据-定点生产配置前端配置业务后端实现服务")
public class FixedPointConfigurationController extends BaseController<FixedPointConfiguration> {

    private final IFixedPointConfigurationService fixedPointConfigurationService;

    public FixedPointConfigurationController(IFixedPointConfigurationService fixedPointConfigurationService) {
        this.fixedPointConfigurationService = fixedPointConfigurationService;
    }

    /**
     * 查询基础数据-定点机台主列表
     */
//    @PostMapping("/list")
//    @RequiresPermissions("monthplan:fixedPointConfiguration:list")
//    @ApiOperation("查询列表")
//    public TableDataInfo list(@RequestBody MdmFixPointVo mdmFixPointVo) {
//        startPage("create_time desc");
//        List<MdmFixPointVo> vos = fixedPointConfigurationService.selectDocFixPoints(docFixPointVo);
//        if (!vos.isEmpty()) {
//            for (DocFixPointVo l : vos) {
//                // 查询定点施工数据
//                List<DocFixedPointProductRelaEntity> docFixedPointProductRelaEntities = iDocFixedPointProductRelaService.selectDocFixedPointProductRelas(l.getId());
//                // 拼接组合施工号、物料编号
//                splitProductRela(docFixedPointProductRelaEntities, l);
//                // 查询定点成型机列表数据
//                List<DocFixPointMoldingRelaEntity> docFixPointMoldingRelaEntities = iDocFixPointMoldingRelaService.selectDocFixPointMoldingRelas(l.getId());
//                // 拼接组合成型机号
//                splitmoldingMachineCode(docFixPointMoldingRelaEntities, l);
//                // 查询定点硫化排数据
//                List<DocFixedPointVulcaniRelaEntity> docFixedPointVulcaniRelaEntities = iDocFixedPointVulcaniRelaService.selectDocFixedPointVulcaniRelas(l.getId());
//                // 拼接组合硫化排号
//                splitlineCode(docFixedPointVulcaniRelaEntities, l);
//            }
//        }
//        return getDataTable(vos);
//    }


    /**
     * 保存
     */
    @Log(title = "ui.data.column.fixedPointConfiguration.modelName", businessType = BusinessType.INSERT_OR_UPDATE)
    @PostMapping("/save")
    @ApiOperation("保存")
    public AjaxResult save(@RequestBody FixedPointConfiguration billVO) {
        return null;
    }

    /**
     * 删除
     */
    @Log(title = "ui.data.column.fixedPointConfiguration.modelName", businessType = BusinessType.DELETE)
    @DeleteMapping("/remove")
    @ApiOperation("删除")
    public AjaxResult removeByIds(@RequestBody List<Long> ids) {
        return null;
    }


    /**
     * 获取基础数据-定点机台主详细信息
     */
    @GetMapping(value = "/{billId}")
    @ApiOperation("获取详细信息")
    public FixedPointConfiguration getInfo(@PathVariable("billId") Long billId) {
        return null;
    }


    /**
     * 根据集合导入基础数据-定点机台主数据
     *
     * @param importContext 导入上下文
     * @param updateSupport 已存在记录是否更新
     * @return 结果
     */
    @Log(title = "ui.data.column.fixedPointConfiguration.modelName", businessType = BusinessType.IMPORT)
    @PostMapping("/importData/{updateSupport}")
    @ApiOperation("导入数据")
    public AjaxResult importData(@RequestBody ImportContext importContext, @PathVariable("updateSupport") boolean updateSupport) throws Exception {
        return null;
    }

    /**
     * 导出列表
     */
    @Log(title = "基础数据-定点机台主", businessType = BusinessType.EXPORT)
    @PostMapping("/exportData/{fileName}")
    @ApiOperation("导入数据")
    public byte[] exportData(@RequestBody FixedPointConfiguration queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return null;
    }

    /**
     * 条件拼接
     *
     * @param queryWrapper
     * @param queryVO
     */
    private void builderCondition(QueryWrapper<FixedPointConfiguration> queryWrapper, FixedPointConfiguration queryVO) {
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("factoryCode")), "FACTORY_CODE", queryVO.getFieldValueByFieldName("factoryCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("productionLineType")), "PRODUCTION_LINE_TYPE", queryVO.getFieldValueByFieldName("productionLineType"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("isClosed")), "IS_CLOSED", queryVO.getFieldValueByFieldName("isClosed"));
    }

}
