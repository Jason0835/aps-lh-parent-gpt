package com.zlt.aps.controller.monthplan;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common4ui.constant.UserConstants;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.ruoyi.common4ui.exception.BusinessException;
import com.zlt.aps.mp.api.domain.entity.FactoryMonthPlanProductionFinalResult;
import com.zlt.aps.mp.api.service.IFactoryMonthPlanProductionFinalResultRemoteService;
import com.zlt.common.utils.PubUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.MapUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.stereotype.Controller;

import lombok.extern.slf4j.Slf4j;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.zlt.file.encryptbyll.FileEncryptUtils;
import org.apache.commons.io.IOUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import javax.servlet.http.HttpServletResponse;
import com.zlt.aps.mp.api.domain.entity.MpStructureAllocation;
import com.zlt.aps.mp.api.service.IMpStructureAllocationRemoteService;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MpStructureAllocationUIController.java
 * 描    述：排产过程_结构排产 UI控制层类：....
 *@author zlt
 *@date 2025-12-29
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */
@Slf4j
@Api(tags = "排产过程_结构排产")
@Controller
@RequiredArgsConstructor
@RequestMapping("/monthplan/mpStructureAllocation")
public class MpStructureAllocationUIController extends BaseUIController<MpStructureAllocation> {

    private final IMpStructureAllocationRemoteService iMpStructureAllocationService;

    private final IFactoryMonthPlanProductionFinalResultRemoteService iFactoryMonthPlanProductionFinalResultService;


    /**
     * 根据条件查询主表数据
     */
    @RequiresPermissions("monthplan:mpStructureAllocation:list")
    @ResponseBody
    @PostMapping("/list")
    @ApiOperation("根据条件查询结构排产信息")
    public TableDataInfo list(MpStructureAllocation mpStructureAllocation) {
        TableDataInfo list = iMpStructureAllocationService.list(mpStructureAllocation);
        // 实体转换
        List<MpStructureAllocation> resultList = convertToEntityList(list.getRows());
        return getTableDataInfo(resultList, list.getTotal());
    }

    private TableDataInfo getTableDataInfo(List<?> resultList, long total) {
        TableDataInfo rspData = new TableDataInfo();
        rspData.setCode(200);
        rspData.setRows(resultList);
        rspData.setMsg(I18nUtil.getMessage("common.msg.base.query.success"));
        rspData.setTotal(total);
        return rspData;
    }

    /**
     * 将List中的LinkedHashMap转换为MpStructureAllocation实体类
     * @param rows
     * @return
     */
    private List<MpStructureAllocation> convertToEntityList(List<?> rows) {
        List<MpStructureAllocation> entityList = new ArrayList<>();
        if (PubUtil.isEmpty(rows)) {
            return entityList;
        }
        ObjectMapper objectMapper = new ObjectMapper();
        for (Object obj : rows) {
            if (obj instanceof MpStructureAllocation) {
                entityList.add((MpStructureAllocation) obj);
            } else if (obj instanceof Map) {
                MpStructureAllocation entity = objectMapper.convertValue(obj, MpStructureAllocation.class);
                entityList.add(entity);
            }
        }
        return entityList;
    }


    /**
     * 查询SKU明细并设置成型机编码（多个以,分隔）
     * @param list
     * @param mpStructureAllocation
     */
    private void setCxMachineCode(List<MpStructureAllocation> list, MpStructureAllocation mpStructureAllocation) {
        if (PubUtil.isEmpty(list)) {
            return;
        }
        if (StringUtils.isEmpty(mpStructureAllocation.getFactoryCode()) || mpStructureAllocation.getYear() == null ||
                mpStructureAllocation.getMonth() == null || StringUtils.isEmpty(mpStructureAllocation.getProductionVersion())) {
            return;
        }
        // 查询SKU明细
        FactoryMonthPlanProductionFinalResult condition = new FactoryMonthPlanProductionFinalResult();
        condition.setFactoryCode(mpStructureAllocation.getFactoryCode());
        condition.setYear(mpStructureAllocation.getYear());
        condition.setMonth(mpStructureAllocation.getMonth());
        condition.setProductionVersion(mpStructureAllocation.getProductionVersion());
        TableDataInfo tableDataInfo = iFactoryMonthPlanProductionFinalResultService.list(condition);
        if (PubUtil.isEmpty(tableDataInfo.getRows())) {
            return;
        }
        // 按照结构分组
        List<FactoryMonthPlanProductionFinalResult> monthPlanList = (List<FactoryMonthPlanProductionFinalResult>) tableDataInfo.getRows();
        monthPlanList = cn.hutool.core.convert.Convert.toList(FactoryMonthPlanProductionFinalResult.class, monthPlanList);
        log.debug("最终排产计划定稿列表大小：{}", monthPlanList.size());
        Map<String, List<FactoryMonthPlanProductionFinalResult>> monthPlanMap = new HashMap<>();
        for (FactoryMonthPlanProductionFinalResult monthPlan : monthPlanList) {
            String structureName = monthPlan.getStructureName();
            if (!monthPlanMap.containsKey(structureName)) {
                monthPlanMap.put(structureName, new ArrayList<>());
            }
            List<FactoryMonthPlanProductionFinalResult> monthPlanResultList = monthPlanMap.get(structureName);
            monthPlanResultList.add(monthPlan);
        }

        // 设置成型机编码
        Set<String> cxMachineCodeSet = new HashSet<>();
        log.debug("结构排产列表大小：{}", list.size());
        Iterator<MpStructureAllocation> iterator = list.iterator();
        while (iterator.hasNext()) {
            cxMachineCodeSet.clear();
            MpStructureAllocation structureAllocation = iterator.next();
            if (StringUtils.isEmpty(structureAllocation.getStructureName())) {
                continue;
            }
            String structureName = structureAllocation.getStructureName();
            // 匹配月度生产计划
            List<FactoryMonthPlanProductionFinalResult> matchMonthPlanList = MapUtils.getObject(monthPlanMap, structureName, new ArrayList<>());
            log.debug("匹配最终排产计划定稿列表 结构:{},大小：{}", structureName, matchMonthPlanList.size());
            cxMachineCodeSet = matchMonthPlanList.stream()
                    .filter(s -> StringUtils.isNotEmpty(s.getCxMachineCode()))
                    .map(FactoryMonthPlanProductionFinalResult::getCxMachineCode)
                    .flatMap(s -> Arrays.stream(s.split(",")))
                    .collect(Collectors.toSet());
            if (PubUtil.isEmpty(cxMachineCodeSet)) {
                continue;
            }
            // 排序
            List<String> sortedList = new ArrayList<>(cxMachineCodeSet);
            Collections.sort(sortedList);
            log.debug("匹配最终结果,结构:{},机台列表:{}", structureName, sortedList);
            structureAllocation.setCxMachineCode(String.join(",", sortedList));
        }
    }


    /**
     * 导出模板文件的文件名，派生类重写名称。
     * 示例：支持多语言写法： String fileName = I18nUtil.getMessage("ui.cd90.machine.export.fileName");
     * @return
     */
    @Override
    public String getExportTemplateFileName(){
        return this.getFunctionName();
    }


    /**
     * 继承时重写方法。
     *
     * @return
     */
    @Override
    public String getProcedureCode() {
        return "0";
    }

    /**
     * 继承时重写方法。
     *
     * @return
     */
    @Override
    public String getFunctionName() {
        return I18nUtil.getMessage("ui.data.column.mpStructureAllocation.modelName");
    }

    /**
     * 重写导入模板的生成逻辑
     */
    @ApiOperation("下载导入模板")
    @Override
    public AjaxResult importTemplate(HttpServletResponse response) throws IOException {
        String fileName = this.getExportTemplateFileName();
        ExcelUtil<MpStructureAllocation> util = new ExcelUtil<>(MpStructureAllocation.class);
        util.exportExcel(response, null, fileName, fileName);
        return AjaxResult.success();
    }


    @RequiresPermissions("monthplan:mpStructureAllocation:export")
    @ApiOperation("数据导出")
    @GetMapping({"/export"})
    @ResponseBody
    @Override
    public void export(HttpServletResponse response, MpStructureAllocation entity) throws IOException {
        String fileName = this.getExportTemplateFileName();
        byte[] excelBytes = iMpStructureAllocationService.exportData(entity,fileName);
        ByteArrayInputStream in = new ByteArrayInputStream(excelBytes);
        ExcelUtil.setResponseHeader(response, fileName, ".xlsx");
        IOUtils.copy(in, response.getOutputStream());
        response.flushBuffer();
    }

//    @RequiresPermissions("monthplan:mpStructureAllocation:import")
    @PostMapping({"/importData"})
    @ResponseBody
    @ApiOperation("数据导入")
    @Override
    public AjaxResult importData(@RequestPart("file") MultipartFile file, boolean updateSupport) throws Exception {
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();

        ImportContext context = new ImportContext();
        context.setImportFilePath(this.importFilePath);
        context.setFunctionName(this.getFunctionName());
        context.setProcedureCode(this.getProcedureCode());
        context.setOriFileName(file.getOriginalFilename());
        context.setFileBytes(data);
        AjaxResult ajaxResult = iMpStructureAllocationService.importData(context,false);
        return ajaxResult;
    }

    /**
     * 修改或新增
     */
    @RequiresPermissions("monthplan:mpStructureAllocation:save")
    @ApiOperation("修改或新增")
    @PostMapping("/save")
    @ResponseBody
    public AjaxResult save(MpStructureAllocation mpStructureAllocation) {
        if (UserConstants.NOT_UNIQUE.equals(iMpStructureAllocationService.checkUnique(mpStructureAllocation))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.mpStructureAllocation.checkUnique"));
        }

        return iMpStructureAllocationService.save(mpStructureAllocation);
    }

    /**
     * 删除排产过程_结构排产
     */
    @ApiOperation("删除,id不为空")
    @RequiresPermissions("monthplan:mpStructureAllocation:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iMpStructureAllocationService.removeByIds(Arrays.asList(arr));
    }

    /**
     * 查询版本列表
     */
    @ApiOperation("查询版本列表")
    @PostMapping("/getVersionList")
    @ResponseBody
    public TableDataInfo getVersionList(MpStructureAllocation queryVO) {
        return iMpStructureAllocationService.getVersionList(queryVO);
    }

    /**
     * 获取日期最接近的下一个结构
     */
    @ApiOperation("获取日期最接近的下一个结构")
    @PostMapping("/getNextStructure")
    @ResponseBody
    public AjaxResult getNextStructure(MpStructureAllocation queryVO) {
        if (StringUtils.isEmpty(queryVO.getFactoryCode()) || queryVO.getYear() == null || queryVO.getMonth() == null
                || StringUtils.isEmpty(queryVO.getCxMachineCode()) || queryVO.getBeginDay() == null || queryVO.getEndDay() == null) {
            throw new BusinessException(I18nUtil.getMessage("ui.data.column.mpStructureAllocation.notQueryCondition"));
        }
        MpStructureAllocation mpStructureAllocation = iMpStructureAllocationService.getNextStructure(queryVO);
        return AjaxResult.success(mpStructureAllocation);
    }

    /**
     * 获取日期最接近的上一个结构
     */
    @ApiOperation("获取日期最接近的上一个结构")
    @PostMapping("/getPreviousStructure")
    @ResponseBody
    public AjaxResult getPreviousStructure(MpStructureAllocation queryVO) {
        if (StringUtils.isEmpty(queryVO.getFactoryCode()) || queryVO.getYear() == null || queryVO.getMonth() == null
                || StringUtils.isEmpty(queryVO.getCxMachineCode()) || queryVO.getBeginDay() == null || queryVO.getEndDay() == null) {
            throw new BusinessException(I18nUtil.getMessage("ui.data.column.mpStructureAllocation.notQueryCondition"));
        }
        MpStructureAllocation mpStructureAllocation = iMpStructureAllocationService.getPreviousStructure(queryVO);
//        // 测试暂时写死
//        if (mpStructureAllocation == null) {
//            mpStructureAllocation = new MpStructureAllocation();
//            mpStructureAllocation.setAdjustStartDay(10);
//        }
        return AjaxResult.success(mpStructureAllocation);
    }

}
