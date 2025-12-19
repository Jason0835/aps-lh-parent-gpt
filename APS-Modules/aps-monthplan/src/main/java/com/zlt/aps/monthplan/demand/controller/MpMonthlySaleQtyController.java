package com.zlt.aps.monthplan.demand.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.ExportLog;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.api.gateway.system.service.IExportLogService;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.utils.ServletUtils;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.tlt.aps.utils.JsonI18nConvertUtils;
import com.zlt.aps.maindata.mapper.MpHistorySaleRecordEntityMapper;
import com.zlt.aps.maindata.mapper.MpMonthlySaleQtyEntityMapper;
import com.zlt.aps.maindata.service.IMpMonthlySaleQtyService;
import com.zlt.aps.maindata.utils.ScmListUtils;
import com.zlt.aps.monthplan.api.domain.entity.MpHistorySaleRecord;
import com.zlt.aps.monthplan.api.domain.entity.MpMonthlySaleQty;
import com.zlt.aps.monthplan.api.domain.vo.AreaConvertVo;
import com.zlt.bill.common.controller.AbstractDocBizController;
import com.zlt.bill.common.service.IDocService;
import com.zlt.common.exception.QueryExprException;
import com.zlt.common.utils.ExcelReadUtils;
import com.zlt.common.utils.PubUtil;
import com.zlt.core.queryformulas.QueryFormulaUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MpMonthlySaleQtyController.java
 * 描    述：月均销量 控制层类：....
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-12-11
 */
@Slf4j
@Api(tags = "月均销量")
@RestController
@RequestMapping("/mpMonthlySaleQty")
public class MpMonthlySaleQtyController extends AbstractDocBizController<MpMonthlySaleQty> {

    @Autowired
    private IMpMonthlySaleQtyService mpMonthlySaleQtyService;

    @Autowired
    private MpMonthlySaleQtyEntityMapper entityMapper;

    @Autowired
    private MpHistorySaleRecordEntityMapper mpHistorySaleRecordEntityMapper;

    @Autowired
    private IExportLogService iExportLogService;

    /**
     * 查询月均销量列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody MpMonthlySaleQty queryVO) {
        TableDataInfo tableDataInfo = super.list(queryVO);
        List<MpMonthlySaleQty> list = (List<MpMonthlySaleQty>) tableDataInfo.getRows();
        // 转换区域名称、赋值区域分组、月份分组汇总集合
        convertAndSetAreaMonthGroupList(queryVO, list);
        return tableDataInfo;
    }

    /**
     * 转换区域名称、赋值区域分组、月份分组汇总集合
     *
     * @param queryVO 查询条件
     * @param list    要转换的列表
     */
    private void convertAndSetAreaMonthGroupList(MpMonthlySaleQty queryVO, List<MpMonthlySaleQty> list) {
        // 把区域都转成名称
        List<AreaConvertVo> convertVoList = list.stream().map(MpMonthlySaleQty::getSaleArea)
                .flatMap(item -> Arrays.stream(item.split(",")))
                .distinct()
                .map(item -> {
                    AreaConvertVo areaConvertVo = new AreaConvertVo();
                    areaConvertVo.setAreaCode(item);
                    return areaConvertVo;
                })
                .sorted(Comparator.comparing(AreaConvertVo::getAreaCode))
                .collect(Collectors.toList());
        Map<String, String> areaNameMap = getAreaNameMap(convertVoList);

        List<String> codeList = new ArrayList<>();
        // 查询历史销售记录
        int passTwelveMonth = 12;
        Calendar instance = Calendar.getInstance();
        instance.setTime(new Date());
        int year = instance.get(Calendar.YEAR);
        String month = String.format("%02d", instance.get(Calendar.MONTH) + 1);
        String maxYearMonth = year + month;

        instance.add(Calendar.MONTH, -passTwelveMonth);
        int last12Year = instance.get(Calendar.YEAR);
        String last12Month = String.format("%02d", instance.get(Calendar.MONTH) + 1);
        String last12YearMonth = last12Year + last12Month;

        String factoryCode = queryVO.getFactoryCode();
        List<MpHistorySaleRecord> sumQtyGroupByAreaList = getAreaGroupHistorySaleRecordList(factoryCode, last12YearMonth, maxYearMonth, codeList);

        Map<String, List<MpHistorySaleRecord>> sumQtyGroupByAreaMap = new HashMap<>(16);
        if (CollectionUtils.isNotEmpty(sumQtyGroupByAreaList)) {
            sumQtyGroupByAreaMap = sumQtyGroupByAreaList.stream().collect(Collectors.groupingBy(MpHistorySaleRecord::getMaterialCode));
        }

        List<MpHistorySaleRecord> sumQtyGroupByMonthList = mpHistorySaleRecordEntityMapper.selectSumQtyGroupByMonth(factoryCode, last12YearMonth, maxYearMonth, codeList);
        Map<String, List<MpHistorySaleRecord>> sumQtyGroupByMonthMap = new HashMap<>(16);
        if (CollectionUtils.isNotEmpty(sumQtyGroupByMonthList)) {
            sumQtyGroupByMonthMap = sumQtyGroupByMonthList.stream().collect(Collectors.groupingBy(MpHistorySaleRecord::getMaterialCode));
        }

        List<List<MpMonthlySaleQty>> splitList = ScmListUtils.getSplitList(list, 1000);

        for (List<MpMonthlySaleQty> monthlySaleQtyList : splitList) {
//            List<String> codeList = monthlySaleQtyList.stream().map(MpMonthlySaleQty::getMaterialCode).collect(Collectors.toList());

            for (MpMonthlySaleQty monthlySaleQty : monthlySaleQtyList) {
                String saleArea = monthlySaleQty.getSaleArea();
                String[] areaSplitArr = saleArea.split(",");
                List<String> areaNameList = new ArrayList<>();
                for (String areaCode : areaSplitArr) {
                    if (areaNameMap.containsKey(areaCode)) {
                        String name = areaNameMap.get(areaCode);
                        areaNameList.add(name);
                    }
                }
                monthlySaleQty.setSaleAreaName(String.join(",", areaNameList));
                String materialCode = monthlySaleQty.getMaterialCode();
                if (sumQtyGroupByAreaMap.containsKey(materialCode)) {
                    List<MpHistorySaleRecord> areaGroupList = sumQtyGroupByAreaMap.get(materialCode);
                    monthlySaleQty.setAreaGroupList(areaGroupList);
                }
                if (sumQtyGroupByMonthMap.containsKey(materialCode)) {
                    List<MpHistorySaleRecord> monthGroupList = sumQtyGroupByMonthMap.get(materialCode);
                    monthlySaleQty.setMonthGroupList(monthGroupList);
                }
            }
        }
    }

    /**
     * 获取区域分组历史销售记录销量汇总
     *
     * @param factoryCode     分厂
     * @param last12YearMonth 过去十二个月字符
     * @param maxYearMonth    当前年月字符
     * @param codeList        物料编码列表
     * @return 结果
     */
    private List<MpHistorySaleRecord> getAreaGroupHistorySaleRecordList(String factoryCode, String last12YearMonth, String maxYearMonth, List<String> codeList) {
        List<MpHistorySaleRecord> sumQtyGroupByAreaList = mpHistorySaleRecordEntityMapper.selectSumQtyGroupByArea(factoryCode, last12YearMonth, maxYearMonth, codeList);

        // 执行表达式，转义区域
        try {
            QueryFormulaUtil.execFormula(sumQtyGroupByAreaList, new String[]{
                    "areaCodeName->getcolvaluewithcondition(t_dp_area, area_name, area_code, areaCode, is_delete = 0)",
            });
        } catch (QueryExprException e) {
            this.logger.error(e.getMessage(), e);
            throw new ServiceException("转换区域，执行查询公式时发生错误.");
        }
        JsonI18nConvertUtils.conventJsonI18n(sumQtyGroupByAreaList, MpHistorySaleRecord.class);
        return sumQtyGroupByAreaList;
    }

    private Map<String, String> getAreaNameMap(List<AreaConvertVo> convertVoList) {
        // 执行表达式，转义区域
        try {
            QueryFormulaUtil.execFormula(convertVoList, new String[]{
                    "areaCodeName->getcolvaluewithcondition(t_dp_area, area_name, area_code, areaCode, is_delete = 0)",
            });
        } catch (QueryExprException e) {
            this.logger.error(e.getMessage(), e);
            throw new ServiceException("转换区域，执行查询公式时发生错误.");
        }
        JsonI18nConvertUtils.conventJsonI18n(convertVoList, AreaConvertVo.class);
        return convertVoList.stream().collect(Collectors.toMap(AreaConvertVo::getAreaCode, AreaConvertVo::getAreaCodeNameI18n));
    }

    @Override
    protected String getOrderBy() {
        return "create_time desc";
    }

    /**
     * 保存
     */
    @Log(title = "ui.data.column.mpMonthlySaleQty.modelName", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("保存")
    @PostMapping("/save")
    @Override
    public AjaxResult save(@RequestBody MpMonthlySaleQty billVO) {
        return super.save(billVO);
    }

    /**
     * 删除
     */
    @Log(title = "ui.data.column.mpMonthlySaleQty.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除")
    @DeleteMapping("/remove")
    @Override
    public AjaxResult removeByIds(@RequestBody List<Long> ids) {
        return super.removeByIds(ids);
    }


    /**
     * 获取月均销量详细信息
     */
    @ApiOperation("获取详细信息")
    @GetMapping(value = "/{billId}")
    @Override
    public MpMonthlySaleQty getInfo(@PathVariable("billId") Long billId) {
        return super.getInfo(billId);
    }


    /**
     * 根据集合导入月均销量数据
     *
     * @param importContext 导入上下文
     * @param updateSupport 已存在记录是否更新
     * @return 结果
     */
    @Log(title = "ui.data.column.mpMonthlySaleQty.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    @Override
    public AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        return super.importData(importContext, updateSupport);
    }

    /**
     * 导出列表
     */
    @Log(title = "月均销量", businessType = BusinessType.EXPORT)
    @ApiOperation("导入数据")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody MpMonthlySaleQty queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        Date beginTime = DateUtils.getNowDate();
        List<MpMonthlySaleQty> list = this.listExportData(queryVO);
        ExcelUtil<MpMonthlySaleQty> util = new ExcelUtil<>(this.getTClass());
        Workbook workbook = util.exportExcel2(response, list, fileName);
        byte[] resultBytes = ExcelReadUtils.writeExcel(workbook);
        Date endTime = DateUtils.getNowDate();
        ExportLog exportLog = new ExportLog();
        exportLog.setProcedureCode("0");
        exportLog.setExportParams(queryVO.toString());
        String uri = ServletUtils.getRequest().getRequestURI();
        exportLog.setFunctionCode(uri.split("/")[1]);
        exportLog.setFunctionName(fileName);
        exportLog.setFileName(fileName + ".xlsx");
        exportLog.setRowCount(list.size());
        exportLog.setBeginTime(beginTime);
        exportLog.setEndTime(endTime);
        exportLog.setSpendTime(DateUtils.getDiffTime(endTime, beginTime));
        this.iExportLogService.add(exportLog);
        return resultBytes;
    }

    @Override
    protected List<MpMonthlySaleQty> listExportData(MpMonthlySaleQty obj) {
        QueryWrapper<MpMonthlySaleQty> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, obj);
        List<MpMonthlySaleQty> monthlySaleQtyList = entityMapper.selectList(wrapper);
        this.convertAndSetAreaMonthGroupList(obj, monthlySaleQtyList);
        return monthlySaleQtyList;
    }

    @Override
    protected IDocService getDocService() {
        return mpMonthlySaleQtyService;
    }

    /**
     * 条件拼接
     *
     * @param queryWrapper
     * @param queryVO
     */
    @Override
    protected void builderCondition(QueryWrapper<MpMonthlySaleQty> queryWrapper, MpMonthlySaleQty queryVO) {
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("factoryCode")), "FACTORY_CODE", queryVO.getFieldValueByFieldName("factoryCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("productTypeCode")), "PRODUCT_TYPE_CODE", queryVO.getFieldValueByFieldName("productTypeCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("locationType")), "LOCATION_TYPE", queryVO.getFieldValueByFieldName("locationType"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("brand")), "BRAND", queryVO.getFieldValueByFieldName("brand"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("materialCode")), "MATERIAL_CODE", queryVO.getFieldValueByFieldName("materialCode"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("materialDesc")), "MATERIAL_DESC", queryVO.getFieldValueByFieldName("materialDesc"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("rollTwelveMonthSaleQty")), "ROLL_TWELVE_MONTH_SALE_QTY", queryVO.getFieldValueByFieldName("rollTwelveMonthSaleQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("averageSaleQty")), "AVERAGE_SALE_QTY", queryVO.getFieldValueByFieldName("averageSaleQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("passThreeMonthSaleQty")), "PASS_THREE_MONTH_SALE_QTY", queryVO.getFieldValueByFieldName("passThreeMonthSaleQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("saleArea")), "SALE_AREA", queryVO.getFieldValueByFieldName("saleArea"));
    }

    @Override
    protected String getTypeCode() {
        return "MP1209";
    }

    @Override
    protected String[] getQueryFormulas() {
        return new String[]{
                "createByName->getcolvalue(SYS_USER, nick_name, user_name, createBy)",
                "updateByName->getcolvalue(SYS_USER, nick_name, user_name, updateBy)",
                "saleAreaName->getcolvaluewithcondition(t_dp_area, area_name, area_code, saleArea, is_delete = 0)",
        };
    }

    /**
     * 获取表头展示列表
     *
     * @return 结果
     */
    @PostMapping("/getShowTableTitleList")
    @ApiOperation("获取表头展示列表")
    public AjaxResult getShowTableTitleList(@RequestBody MpMonthlySaleQty queryVO) {
        // 查询列表
        TableDataInfo tableDataInfo = super.list(queryVO);
        List<MpMonthlySaleQty> list = (List<MpMonthlySaleQty>) tableDataInfo.getRows();

        List<String> codeList = list.stream().map(MpMonthlySaleQty::getMaterialCode).collect(Collectors.toList());

        String factoryCode = queryVO.getFactoryCode();
        // 查询历史销售记录
        int passTwelveMonth = 12;
        Calendar instance = Calendar.getInstance();
        instance.setTime(new Date());
        int year = instance.get(Calendar.YEAR);
        String month = String.format("%02d", instance.get(Calendar.MONTH) + 1);
        String maxYearMonth = year + month;

        instance.add(Calendar.MONTH, -passTwelveMonth);
        int last12Year = instance.get(Calendar.YEAR);
        String last12Month = String.format("%02d", instance.get(Calendar.MONTH) + 1);
        String last12YearMonth = last12Year + last12Month;
        List<MpHistorySaleRecord> sumQtyGroupByAreaList = getAreaGroupHistorySaleRecordList(factoryCode, last12YearMonth, maxYearMonth, codeList);

        Map<String, Object> map = new HashMap<>();

        List<Map<String, Object>> areaMapList = new ArrayList<>();

        Map<String, MpHistorySaleRecord> areaGroupMap = sumQtyGroupByAreaList.stream()
                .sorted(Comparator.comparing(MpHistorySaleRecord::getAreaCode))
                .collect(Collectors.toMap(MpHistorySaleRecord::getAreaCode, Function.identity(), (v1, v2) -> v1));
        Set<Map.Entry<String, MpHistorySaleRecord>> areaEntrySet = areaGroupMap.entrySet();

        for (Map.Entry<String, MpHistorySaleRecord> entry : areaEntrySet) {
            String areaCode = entry.getKey();
            String mapKey = "areaCode" + areaCode;
            MpHistorySaleRecord value = entry.getValue();

            Map<String, Object> listMap = new HashMap<>();
            listMap.put("areaCode", areaCode);
            listMap.put("areaCodeShow", mapKey);
            listMap.put("areaCodeName", value.getAreaCodeName());
            listMap.put("areaCodeNameI18n", value.getAreaCodeNameI18n());
            areaMapList.add(listMap);
        }

        List<Map<String, Object>> monthMapList = new ArrayList<>();
        List<MpHistorySaleRecord> sumQtyGroupByMonthList = mpHistorySaleRecordEntityMapper.selectSumQtyGroupByMonth(factoryCode, last12YearMonth, maxYearMonth, codeList);
        Map<Integer, MpHistorySaleRecord> monthGroupMap = sumQtyGroupByMonthList.stream()
                .sorted(Comparator.comparing(MpHistorySaleRecord::getMonth))
                .collect(Collectors.toMap(MpHistorySaleRecord::getMonth, Function.identity(), (v1, v2) -> v1));
        Set<Map.Entry<Integer, MpHistorySaleRecord>> monthEntrySet = monthGroupMap.entrySet();

        for (Map.Entry<Integer, MpHistorySaleRecord> entry : monthEntrySet) {
            Integer key = entry.getKey();
            String mapKey = "month" + key;
            MpHistorySaleRecord value = entry.getValue();
            Map<String, Object> listMap = new HashMap<>();
            listMap.put("monthShow", mapKey);
            listMap.put("month", value.getMonth());
            monthMapList.add(listMap);
        }

        map.put("areaTableTitle", areaMapList);
        map.put("monthTableTitle", monthMapList);

        return AjaxResult.success(map);
    }

    /**
     * 生成月均销量
     *
     * @param mpMonthlySaleQty 参数
     * @return 结果
     */
    @ApiOperation("生成月均销量")
    @PostMapping("/genMonthlySaleQty")
    public AjaxResult genMonthlySaleQty(@RequestBody MpMonthlySaleQty mpMonthlySaleQty) {
        return mpMonthlySaleQtyService.genMonthlySaleQty(mpMonthlySaleQty);
    }

}
