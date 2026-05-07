package com.zlt.aps.mp.setting.controller;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.maindata.mapper.MdmMoldingMachineEntityMapper;
import com.zlt.aps.maindata.service.IMdmMoldingMachineService;
import com.zlt.aps.mp.api.domain.entity.MdmMoldingMachine;
import com.zlt.bill.common.controller.AbstractDocBizController;
import com.zlt.bill.common.service.IDocService;
import com.zlt.common.utils.PubUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
* Copyright (c) 2022, All rights reserved。
* 文件名称：MdmMoldingMachineController.java
* 描    述：基础数据-成型机档案 控制层类：....
*@author zlt
*@date 2025-12-14
*@version 1.0
*
 *  修改记录：
*     修改时间：...
*     修 改 人：zlt
*     修改内容：...
*/
@Slf4j
@Api(tags = "基础数据-成型机档案")
@RestController
@RequestMapping("/mdmMoldingMachine")
public class MdmMoldingMachineController extends AbstractDocBizController<MdmMoldingMachine> {

    @Autowired
    private IMdmMoldingMachineService mdmMoldingMachineService;

    @Autowired
    private MdmMoldingMachineEntityMapper entityMapper;

    /**
     * 查询基础数据-成型机档案列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody MdmMoldingMachine queryVO) {
        TableDataInfo tableDataInfo = super.list(queryVO);
        fillMonthStructureNames(tableDataInfo, queryVO);
        return tableDataInfo;
    }

    @Override
    protected String getOrderBy() {
        return "CX_MACHINE_CODE ASC";
    }

    /**
     * 保存
     */
    @Log(title = "ui.data.column.mdmMoldingMachine.modelName", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("保存")
    @PostMapping("/save")
    @Override
    public AjaxResult save(@RequestBody MdmMoldingMachine billVO){
        return super.save(billVO);
    }

    /**
     * 删除
     */
    @Log(title = "ui.data.column.mdmMoldingMachine.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除")
    @DeleteMapping("/remove")
    @Override
    public AjaxResult removeByIds(@RequestBody List<Long> ids){
        return super.removeByIds(ids);
    }


    /**
     * 获取基础数据-成型机档案详细信息
     */
    @ApiOperation("获取详细信息")
    @GetMapping(value = "/{billId}")
    @Override
    public MdmMoldingMachine getInfo(@PathVariable("billId") Long billId) {
        return super.getInfo(billId);
    }


    /**
     * 根据集合导入基础数据-成型机档案数据
     * @param importContext 导入上下文
     * @param updateSupport 已存在记录是否更新
     * @return 结果
     */
    @Log(title = "ui.data.column.mdmMoldingMachine.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    @Override
    public AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        return super.importData(importContext,updateSupport);
    }

    /**
     * 导出列表
     */
    @Log(title = "基础数据-成型机档案", businessType = BusinessType.EXPORT)
    @ApiOperation("导出数据")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody MdmMoldingMachine queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return super.exportData(queryVO, fileName, response);
    }

    @Override
    protected List<MdmMoldingMachine> listExportData(MdmMoldingMachine obj) {
        QueryWrapper<MdmMoldingMachine> wrapper = new QueryWrapper<>();
        startPage(getOrderBy());
        this.builderCondition(wrapper, obj);
        clearPage();
        return entityMapper.selectList(wrapper);
    }

    @Override
    protected IDocService getDocService(){
        return mdmMoldingMachineService;
    }

    /**
     * 条件拼接
     *
     * @param queryWrapper
     * @param queryVO
     */
    @Override
    protected void builderCondition(QueryWrapper<MdmMoldingMachine> queryWrapper, MdmMoldingMachine queryVO) {
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("factoryCode")), "FACTORY_CODE", queryVO.getFieldValueByFieldName("factoryCode"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("cxMachineCode")), "CX_MACHINE_CODE", queryVO.getFieldValueByFieldName("cxMachineCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("cxMachineTypeCode")), "CX_MACHINE_TYPE_CODE", queryVO.getFieldValueByFieldName("cxMachineTypeCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("rollOverType")), "ROLL_OVER_TYPE", queryVO.getFieldValueByFieldName("rollOverType"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("isZeroRack")), "IS_ZERO_RACK", queryVO.getFieldValueByFieldName("isZeroRack"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("lhMachineMaxQty")), "LH_MACHINE_MAX_QTY", queryVO.getFieldValueByFieldName("lhMachineMaxQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("maxDayCapacity")), "MAX_DAY_CAPACITY", queryVO.getFieldValueByFieldName("maxDayCapacity"));
    }


    @Override
    protected String getTypeCode(){
        return "MDM0138";
    }

    /**
     * 为当前分页成型机台补充最近12个月结构名称。
     *
     * @param tableDataInfo 分页结果
     * @param queryVO 查询条件
     */
    private void fillMonthStructureNames(TableDataInfo tableDataInfo, MdmMoldingMachine queryVO) {
        List<?> rows = tableDataInfo.getRows();
        if (CollectionUtils.isEmpty(rows)) {
            return;
        }

        List<MdmMoldingMachine> machineList = rows.stream()
                .filter(MdmMoldingMachine.class::isInstance)
                .map(MdmMoldingMachine.class::cast)
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(machineList)) {
            return;
        }

        Set<String> currentPageMachineCodes = machineList.stream()
                .map(MdmMoldingMachine::getCxMachineCode)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (CollectionUtils.isEmpty(currentPageMachineCodes)) {
            return;
        }

        List<String> yearMonths = buildLastTwelveYearMonths();
        List<MdmMoldingMachineEntityMapper.MachineMonthStructureName> structureNameList =
                entityMapper.selectMachineMonthStructureNames(queryVO.getFactoryCode(), yearMonths);
        if (CollectionUtils.isEmpty(structureNameList)) {
            machineList.forEach(item -> item.setMonthStructureNameMap(new LinkedHashMap<>()));
            return;
        }

        Map<String, Map<String, Set<String>>> machineMonthStructureMap = new LinkedHashMap<>();
        for (MdmMoldingMachineEntityMapper.MachineMonthStructureName item : structureNameList) {
            if (StringUtils.isBlank(item.getCxMachineCode()) || StringUtils.isBlank(item.getStructureName())) {
                continue;
            }
            for (String machineCode : item.getCxMachineCode().split(",")) {
                String trimmedMachineCode = StringUtils.trim(machineCode);
                if (StringUtils.isBlank(trimmedMachineCode) || !currentPageMachineCodes.contains(trimmedMachineCode)) {
                    continue;
                }
                machineMonthStructureMap
                        .computeIfAbsent(trimmedMachineCode, key -> new LinkedHashMap<>())
                        .computeIfAbsent(item.getYearMonth(), key -> new LinkedHashSet<>())
                        .add(item.getStructureName());
            }
        }

        for (MdmMoldingMachine machine : machineList) {
            Map<String, String> monthStructureNameMap = new LinkedHashMap<>();
            Map<String, Set<String>> monthMap = machineMonthStructureMap.get(machine.getCxMachineCode());
            for (String yearMonth : yearMonths) {
                Set<String> names = monthMap == null ? null : monthMap.get(yearMonth);
                monthStructureNameMap.put(yearMonth, CollectionUtils.isEmpty(names) ? "" : String.join(",", names));
            }
            machine.setMonthStructureNameMap(monthStructureNameMap);
        }
    }

    /**
     * 构建最近12个历史月份，不包含当前月。
     *
     * @return 年月集合，格式yyyy-MM
     */
    private List<String> buildLastTwelveYearMonths() {
        List<String> yearMonths = new ArrayList<>();
        for (int i = 1; i <= 12; i++) {
            yearMonths.add(DateUtil.format(DateUtil.offsetMonth(DateUtil.date(), -i), "yyyy-MM"));
        }
        return yearMonths;
    }

}
