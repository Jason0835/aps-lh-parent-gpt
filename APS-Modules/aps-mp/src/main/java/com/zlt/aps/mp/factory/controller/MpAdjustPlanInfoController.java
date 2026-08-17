package com.zlt.aps.mp.factory.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.maindata.mapper.MdmSkuStructureRefEntityMapper;
import com.zlt.aps.mp.api.domain.entity.MdmSkuStructureRef;
import com.zlt.aps.mp.api.domain.entity.MpAdjustPlanRequireInfo;
import com.zlt.aps.mp.factory.service.IMpAdjustPlanRequireInfoService;
import com.zlt.aps.utils.AppUtils;
import com.zlt.bill.common.controller.AbstractDocBizController;
import com.zlt.bill.common.service.IDocService;
import com.zlt.common.exception.QueryExprException;
import com.zlt.common.utils.PubUtil;
import com.zlt.core.queryformulas.QueryFormulaUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MpAdjustPlanInfoController.java
 * 描    述：S2-0611.计划调整需求信息 控制层类：....
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 20260716
 */
@Slf4j
@Api(tags = "计划调整需求信息")
@RestController
@RequestMapping("/adjustPlanRequireInfo")
@RequiredArgsConstructor
public class MpAdjustPlanInfoController extends AbstractDocBizController<MpAdjustPlanRequireInfo> {

    private final IMpAdjustPlanRequireInfoService mpAdjustPlanInfoService;

    private final MdmSkuStructureRefEntityMapper mdmSkuStructureRefMapper;

    /**
     * 产品结构下拉数据（来源 mdmSkuStructureRef，去重）
     */
    @ApiOperation("产品结构下拉数据")
    @GetMapping("/structureOptions")
    public AjaxResult structureOptions(@RequestParam(value = "factoryCode", required = false) String factoryCode,
                                       @RequestParam(value = "structureName", required = false) String structureName) {
        MdmSkuStructureRef query = new MdmSkuStructureRef();
        query.setFactoryCode(PubUtil.isNotEmpty(factoryCode) ? factoryCode : "116");
        query.setStructureName(structureName);
        List<MdmSkuStructureRef> list = mdmSkuStructureRefMapper.getStructureSelectList(query);
        List<Map<String, Object>> options = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(list)) {
            for (MdmSkuStructureRef r : list) {
                if (PubUtil.isNotEmpty(r.getStructureName())) {
                    Map<String, Object> m = new HashMap<>();
                    m.put("label", r.getStructureName());
                    m.put("value", r.getStructureName());
                    options.add(m);
                }
            }
        }
        return AjaxResult.success(options);
    }

    /**
     * 物料编码下拉数据（来源 mdmSkuStructureRef，按结构过滤，含物料描述反显）
     */
    @ApiOperation("物料编码下拉数据")
    @GetMapping("/materialOptions")
    public AjaxResult materialOptions(@RequestParam(value = "factoryCode", required = false) String factoryCode,
                                      @RequestParam(value = "structureName", required = false) String structureName,
                                      @RequestParam(value = "materialCode", required = false) String materialCode) {
        MdmSkuStructureRef query = new MdmSkuStructureRef();
        query.setFactoryCode(PubUtil.isNotEmpty(factoryCode) ? factoryCode : "116");
        query.setStructureName(structureName);
        query.setMaterialCode(materialCode);
        QueryWrapper<MdmSkuStructureRef> wrapper = new QueryWrapper<>();
        wrapper.eq(PubUtil.isNotEmpty(query.getFactoryCode()), "a.FACTORY_CODE", query.getFactoryCode());
        wrapper.like(PubUtil.isNotEmpty(query.getStructureName()), "a.STRUCTURE_NAME", query.getStructureName());
        wrapper.like(PubUtil.isNotEmpty(query.getMaterialCode()), "a.MATERIAL_CODE", query.getMaterialCode());
        wrapper.eq("a.IS_DELETE", YesOrNoEnum.NO.getValue());
        List<MdmSkuStructureRef> list = mdmSkuStructureRefMapper.getMdmSkuStructureRefList(wrapper);
        if (CollectionUtils.isNotEmpty(list)) {
            try {
                QueryFormulaUtil.execFormula(list, new String[]{
                        "materialDesc -> getcolvalue(T_MDM_MATERIAL_INFO, MATERIAL_DESC, MATERIAL_CODE, materialCode)"
                });
            } catch (QueryExprException e) {
                throw new ServiceException("执行查询公式时发生错误.");
            }
        }
        List<Map<String, Object>> options = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(list)) {
            for (MdmSkuStructureRef r : list) {
                if (PubUtil.isNotEmpty(r.getMaterialCode())) {
                    Map<String, Object> m = new HashMap<>();
                    m.put("label", r.getMaterialCode());
                    m.put("value", r.getMaterialCode());
                    m.put("materialDesc", r.getMaterialDesc());
                    options.add(m);
                }
            }
        }
        return AjaxResult.success(options);
    }

    /** 查询计划调整需求信息列表 */
    @ApiOperation("查询计划调整需求信息列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody MpAdjustPlanRequireInfo queryVO) {
        return super.list(queryVO);
    }

    /** 新增计划调整需求信息 */
    @Log(title = "ui.data.column.mpAdjustPlanInfo.modelName", businessType = BusinessType.INSERT)
    @ApiOperation("新增计划调整需求信息")
    @PostMapping("/add")
    public AjaxResult add(@RequestBody MpAdjustPlanRequireInfo entity) {
        return super.save(entity);
    }

    /** 编辑计划调整需求信息 */
    @Log(title = "ui.data.column.mpAdjustPlanInfo.modelName", businessType = BusinessType.UPDATE)
    @ApiOperation("编辑计划调整需求信息")
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody MpAdjustPlanRequireInfo entity) {
        return super.save(entity);
    }

    /** 删除计划调整需求信息 */
    @Log(title = "ui.data.column.mpAdjustPlanInfo.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除计划调整需求信息")
    @PostMapping("/remove")
    @Override
    public AjaxResult removeByIds(@RequestBody List<Long> ids) {
        return super.removeByIds(ids);
    }

    /** 获取计划调整需求信息详情 */
    @ApiOperation("获取计划调整需求信息详情")
    @GetMapping("/getInfo/{id}")
    @Override
    public MpAdjustPlanRequireInfo getInfo(@PathVariable("id") Long id) {
        return super.getInfo(id);
    }

    /** 导入计划调整需求信息 */
    @Log(title = "ui.data.column.mpAdjustPlanInfo.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入计划调整需求信息")
    @PostMapping("/importData")
    @Override
    public AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        return super.importData(importContext, updateSupport);
    }

    /** 导出计划调整需求信息 */
    @Log(title = "ui.data.column.mpAdjustPlanInfo.modelName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出计划调整需求信息")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody MpAdjustPlanRequireInfo queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return super.exportData(queryVO, fileName, response);
    }

    @Override
    protected List<MpAdjustPlanRequireInfo> listExportData(MpAdjustPlanRequireInfo obj) {
        QueryWrapper<MpAdjustPlanRequireInfo> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, obj);
        wrapper.orderByAsc("ADJUST_DATE", "MATERIAL_DESC");
        List<MpAdjustPlanRequireInfo> list = mpAdjustPlanInfoService.getListByCondition(wrapper);
        // 导出前按反显公式回填物料描述（物料编码 -> 主数据物料描述）
        AppUtils.formatData(list, getQueryFormulas());
        return list;
    }

    /**
     * 反显公式：物料描述按物料编码从主数据反显（列表/导出）
     */
    @Override
    protected String[] getQueryFormulas() {
        return mpAdjustPlanInfoService.getQueryFormulas();
    }

    @Override
    protected IDocService getDocService() {
        return mpAdjustPlanInfoService;
    }

    /**
     * 条件拼接
     *
     * @param queryWrapper 查询包装器
     * @param queryVO      查询条件
     */
    @Override
    protected void builderCondition(QueryWrapper<MpAdjustPlanRequireInfo> queryWrapper, MpAdjustPlanRequireInfo queryVO) {
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFactoryCode()), "FACTORY_CODE", queryVO.getFactoryCode());
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getStructureName()), "STRUCTURE_NAME", queryVO.getStructureName());
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getMesMaterialCode()), "MES_MATERIAL_CODE", queryVO.getMesMaterialCode());
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getMaterialCode()), "MATERIAL_CODE", queryVO.getMaterialCode());
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getMaterialDesc()), "MATERIAL_DESC", queryVO.getMaterialDesc());
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getArea()), "AREA", queryVO.getArea());
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getPlanAdjustType()), "PLAN_ADJUST_TYPE", queryVO.getPlanAdjustType());
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getAdjustReason()), "ADJUST_REASON", queryVO.getAdjustReason());
        queryWrapper.ge(PubUtil.isNotEmpty(queryVO.getAdjustDateStart()), "ADJUST_DATE", queryVO.getAdjustDateStart());
        queryWrapper.le(PubUtil.isNotEmpty(queryVO.getAdjustDateEnd()), "ADJUST_DATE", queryVO.getAdjustDateEnd());
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getRemark()), "REMARK", queryVO.getRemark());
    }

    @Override
    protected String getTypeCode() {
        return "S2-0801";
    }

    @Override
    protected String getOrderBy() {
        return "ADJUST_DATE,STRUCTURE_NAME,MATERIAL_DESC";
    }
}
