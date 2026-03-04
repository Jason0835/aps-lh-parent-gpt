package com.zlt.aps.mp.mdm.controller;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.maindata.mapper.MdmCxMachineFixedEntityMapper;
import com.zlt.aps.maindata.service.IMdmCxMachineFixedService;
import com.zlt.aps.mp.api.domain.entity.MdmCxMachineFixed;
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
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MdmCxMachineFixedController.java
 * 描    述：成型固定机台 控制层类：....
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-12-12
 */
@Slf4j
@Api(tags = "成型固定机台")
@RestController
@RequestMapping("/mdmCxMachineFixed")
public class MdmCxMachineFixedController extends AbstractDocBizController<MdmCxMachineFixed> {

    @Autowired
    private IMdmCxMachineFixedService mdmCxMachineFixedService;

    @Autowired
    private MdmCxMachineFixedEntityMapper entityMapper;

    /**
     * 查询成型固定机台列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody MdmCxMachineFixed queryVO) {
        return super.list(queryVO);
    }

    @Override
    protected String getOrderBy() {
        return "create_time desc";
    }

    /**
     * 保存
     */
    @Log(title = "ui.data.column.mdmCxMachineFixed.modelName", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("保存")
    @PostMapping("/save")
    @Override
    public AjaxResult save(@RequestBody MdmCxMachineFixed billVO) {
        return super.save(billVO);
    }

    /**
     * 删除
     */
    @Log(title = "ui.data.column.mdmCxMachineFixed.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除")
    @DeleteMapping("/remove")
    @Override
    public AjaxResult removeByIds(@RequestBody List<Long> ids) {
        return super.removeByIds(ids);
    }


    /**
     * 获取成型固定机台详细信息
     */
    @ApiOperation("获取详细信息")
    @GetMapping(value = "/{billId}")
    @Override
    public MdmCxMachineFixed getInfo(@PathVariable("billId") Long billId) {
        return super.getInfo(billId);
    }


    /**
     * 根据集合导入成型固定机台数据
     *
     * @param importContext 导入上下文
     * @param updateSupport 已存在记录是否更新
     * @return 结果
     */
    @Log(title = "ui.data.column.mdmCxMachineFixed.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    @Override
    public AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        return super.importData(importContext, updateSupport);
    }

    @Override
    protected AjaxResult doImportData(List list, boolean updateSupport, long importLogId) {
        // 根据机台分组，把固定结构拼接
        List<MdmCxMachineFixed> resultList = (List<MdmCxMachineFixed>) list;
        Map<String, List<MdmCxMachineFixed>> groupMap = resultList.stream()
                .filter(item -> StringUtils.isNotBlank(item.getFactoryCode())
                        && StringUtils.isNotBlank(item.getCxMachineCode()))
                .collect(Collectors.groupingBy(item ->
                        item.getFactoryCode().trim() + "&_&" + item.getCxMachineCode().trim()));
        Set<Map.Entry<String, List<MdmCxMachineFixed>>> entries = groupMap.entrySet();
        List<MdmCxMachineFixed> importList = new ArrayList<>();
        for (Map.Entry<String, List<MdmCxMachineFixed>> entry : entries) {
            List<MdmCxMachineFixed> groupItemList = entry.getValue();

            if (groupItemList.isEmpty()) {
                continue;
            }
            // 取分组第一个元素作为基础对象（复用非拼接字段）
            MdmCxMachineFixed firstItem = groupItemList.get(0);
            MdmCxMachineFixed mergedItem = new MdmCxMachineFixed();
            // 基础字段赋值
            mergedItem.setFactoryCode(firstItem.getFactoryCode().trim());
            mergedItem.setCxMachineCode(firstItem.getCxMachineCode().trim());
            mergedItem.setBaseVale(null);
            // 合并固定结构1
            mergedItem.setFixedStructure1(mergeField(groupItemList, MdmCxMachineFixed::getFixedStructure1));
            // 合并固定结构2
            mergedItem.setFixedStructure2(mergeField(groupItemList, MdmCxMachineFixed::getFixedStructure2));
            // 合并固定结构3
            mergedItem.setFixedStructure3(mergeField(groupItemList, MdmCxMachineFixed::getFixedStructure3));
            // 合并固定SKU
            mergedItem.setFixedMaterialCode(mergeField(groupItemList, MdmCxMachineFixed::getFixedMaterialCode));
            // 合并固定物料描述
            mergedItem.setFixedMaterialDesc(mergeField(groupItemList, MdmCxMachineFixed::getFixedMaterialDesc));
            // 合并不可作业结构
            mergedItem.setDisableStructure(mergeField(groupItemList, MdmCxMachineFixed::getDisableStructure));
            // 合并不可作业SKU
            mergedItem.setDisableMaterialCode(mergeField(groupItemList, MdmCxMachineFixed::getDisableMaterialCode));
            // 合并不可作业物料描述
            mergedItem.setDisableMaterialDesc(mergeField(groupItemList, MdmCxMachineFixed::getDisableMaterialDesc));

            importList.add(mergedItem);
        }
        return super.doImportData(importList, updateSupport, importLogId);
    }

    /**
     * 拆分字段内容、去重、重新拼接
     *
     * @param groupItems 分组内的对象列表
     * @param fieldGetter 字段获取器
     * @return 合并后的字段字符串（逗号分隔，去重）
     */
    private static String mergeField(List<MdmCxMachineFixed> groupItems,
                                     Function<MdmCxMachineFixed, String> fieldGetter) {
        // 收集所有非空字段值，拆分后去重
        // LinkedHashSet保证有序+去重
        Set<String> fieldValues = new LinkedHashSet<>();
        for (MdmCxMachineFixed item : groupItems) {
            String fieldValue = fieldGetter.apply(item);
            if (StringUtils.isBlank(fieldValue)) {
                continue;
            }
            // 拆分原始字段（按逗号），过滤空值并去重
            Arrays.stream(fieldValue.split(","))
                    .map(String::trim)
                    .filter(StringUtils::isNotBlank)
                    .forEach(fieldValues::add);
        }
        // 重新拼接成逗号分隔的字符串
        return String.join(",", fieldValues);
    }

    /**
     * 导出列表
     */
    @Log(title = "成型固定机台", businessType = BusinessType.EXPORT)
    @ApiOperation("导入数据")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody MdmCxMachineFixed queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return super.exportData(queryVO, fileName, response);
    }

    @Override
    protected List<MdmCxMachineFixed> listExportData(MdmCxMachineFixed obj) {
        QueryWrapper<MdmCxMachineFixed> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, obj);
        List<MdmCxMachineFixed> mdmCxMachineFixedList = entityMapper.selectList(wrapper);
        // 转换成用户模板
        return transformToUserTemplate(mdmCxMachineFixedList);
    }

    /**
     * 转换成用户模板
     * @param mdmCxMachineFixedList 要转换的数据
     * @return 结果
     */
    private static List<MdmCxMachineFixed> transformToUserTemplate(List<MdmCxMachineFixed> mdmCxMachineFixedList) {
        List<MdmCxMachineFixed> resultList = new ArrayList<>();
        if (CollUtil.isNotEmpty(mdmCxMachineFixedList)) {
            for (MdmCxMachineFixed machineFixed : mdmCxMachineFixedList) {
                List<String> splitFixedStructure1 = machineFixed.getSplitFixedStructure1();
                List<String> splitFixedStructure2 = machineFixed.getSplitFixedStructure2();
                List<String> splitFixedStructure3 = machineFixed.getSplitFixedStructure3();

                int splitFixedStructureMaxSize = Math.max(Math.max(splitFixedStructure1.size(), splitFixedStructure2.size()), splitFixedStructure3.size());

                List<String> splitFixedMaterialCode = machineFixed.getSplitFixedMaterialCode();
                List<String> splitFixedMaterialDesc = machineFixed.getSplitFixedMaterialDesc();
                int fixedMaterialMaxSize = Math.max(splitFixedMaterialCode.size(), splitFixedMaterialDesc.size());

                List<String> splitDisableStructure = machineFixed.getSplitDisableStructure();
                List<String> splitDisableMaterialCode = machineFixed.getSplitDisableMaterialCode();
                List<String> splitDisableMaterialDesc = machineFixed.getSplitDisableMaterialDesc();
                int disableMaxSize = Math.max(Math.max(splitDisableStructure.size(), splitDisableMaterialCode.size()), splitDisableMaterialDesc.size());

                int maxListSize = Math.max(Math.max(splitFixedStructureMaxSize, fixedMaterialMaxSize), disableMaxSize);

                for (int i = 0; i < maxListSize; i++) {
                    MdmCxMachineFixed fixed = new MdmCxMachineFixed();
                    fixed.setFactoryCode(machineFixed.getFactoryCode());
                    fixed.setCxMachineCode(machineFixed.getCxMachineCode());

                    if (i < splitFixedStructure1.size()) {
                        fixed.setFixedStructure1(splitFixedStructure1.get(i));
                    }
                    if (i < splitFixedStructure2.size()) {
                        fixed.setFixedStructure2(splitFixedStructure2.get(i));
                    }
                    if (i < splitFixedStructure3.size()) {
                        fixed.setFixedStructure3(splitFixedStructure3.get(i));
                    }

                    if (i < splitFixedMaterialCode.size()) {
                        fixed.setFixedMaterialCode(splitFixedMaterialCode.get(i));
                    }
                    if (i < splitFixedMaterialDesc.size()) {
                        fixed.setFixedMaterialDesc(splitFixedMaterialDesc.get(i));
                    }

                    if (i < splitDisableStructure.size()) {
                        fixed.setDisableStructure(splitDisableStructure.get(i));
                    }
                    if (i < splitDisableMaterialCode.size()) {
                        fixed.setDisableMaterialCode(splitDisableMaterialCode.get(i));
                    }
                    if (i < splitDisableMaterialDesc.size()) {
                        fixed.setDisableMaterialDesc(splitDisableMaterialDesc.get(i));
                    }
                    resultList.add(fixed);
                }
            }
        }
        return resultList;
    }

    @Override
    protected IDocService getDocService() {
        return mdmCxMachineFixedService;
    }

    /**
     * 条件拼接
     *
     * @param queryWrapper
     * @param queryVO
     */
    @Override
    protected void builderCondition(QueryWrapper<MdmCxMachineFixed> queryWrapper, MdmCxMachineFixed queryVO) {
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("factoryCode")), "FACTORY_CODE", queryVO.getFieldValueByFieldName("factoryCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("cxMachineCode")), "CX_MACHINE_CODE", queryVO.getFieldValueByFieldName("cxMachineCode"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("fixedStructure1")), "FIXED_STRUCTURE1", queryVO.getFieldValueByFieldName("fixedStructure1"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("fixedStructure2")), "FIXED_STRUCTURE2", queryVO.getFieldValueByFieldName("fixedStructure2"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("fixedStructure3")), "FIXED_STRUCTURE3", queryVO.getFieldValueByFieldName("fixedStructure3"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("fixedMaterialCode")), "FIXED_MATERIAL_CODE", queryVO.getFieldValueByFieldName("fixedMaterialCode"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("disableStructure")), "DISABLE_STRUCTURE", queryVO.getFieldValueByFieldName("disableStructure"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("disableMaterialCode")), "DISABLE_MATERIAL_CODE", queryVO.getFieldValueByFieldName("disableMaterialCode"));
    }

    @Override
    protected String getTypeCode() {
        return "MDM0133";
    }
}
