package com.zlt.aps.mp.factory.controller;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.ExportLog;
import com.ruoyi.api.gateway.system.service.IExportLogService;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.utils.PageUtils;
import com.ruoyi.common.core.utils.ServletUtils;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.exception.BusinessException;
import com.zlt.aps.mp.api.domain.entity.MpAdjustStructureIn;
import com.zlt.aps.mp.common.utils.CommaFieldSortUtil;
import com.zlt.aps.utils.SpringContextSupplierUtil;
import com.zlt.aps.maindata.mapper.MdmMaterialConsumeDetailMapper;
import com.zlt.aps.maindata.mapper.RawSpecialMaterialRecordEntityMapper;
import com.zlt.aps.mp.api.domain.dto.FactoryMonthPlanProductionFinalResultParam;
import com.zlt.aps.mp.api.domain.entity.FactoryMonthPlanProductionFinalResult;
import com.zlt.aps.mp.api.domain.entity.MdmMaterialConsumeDetail;
import com.zlt.aps.mp.api.domain.entity.RawSpecialMaterialRecord;
import com.zlt.aps.mp.api.domain.vo.FactoryMonthPlanFinalAdjustExportVo;
import com.zlt.aps.mp.api.domain.vo.FactoryMonthPlanFinalAdjustVo;
import com.zlt.aps.mp.factory.mapper.FactoryMonthPlanProductionFinalResultEntityMapper;
import com.zlt.aps.mp.factory.service.IFactoryMonthPlanProductionFinalResultService;
import com.zlt.bill.common.controller.AbstractDocBizController;
import com.zlt.bill.common.service.IDocService;
import com.zlt.common.utils.ExcelReadUtils;
import com.zlt.common.utils.PubUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StopWatch;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：FactoryMonthPlanProductionFinalResultController.java
 * 描    述：工厂月生产计划-最终排产计划定稿 控制层类：....
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-12-23
 */
@Slf4j
@Api(tags = "工厂月生产计划-最终排产计划定稿")
@RestController
@RequestMapping("/factoryMonthPlanFinalResult")
public class FactoryMonthPlanProductionFinalResultController extends AbstractDocBizController<FactoryMonthPlanProductionFinalResult> {

    @Autowired
    private IFactoryMonthPlanProductionFinalResultService factoryMonthPlanProductionFinalResultService;

    @Autowired
    private FactoryMonthPlanProductionFinalResultEntityMapper entityMapper;

    @Autowired
    protected RawSpecialMaterialRecordEntityMapper rawSpecialMaterialRecordMapper;

    @Autowired
    protected MdmMaterialConsumeDetailMapper mdmMaterialConsumeDetailMapper;

    @Autowired
    private IExportLogService iExportLogService;

    /**
     * 查询工厂月度生产计划-最终排产计划定稿
     */
    @Override
    @ApiOperation("查询列表")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody FactoryMonthPlanProductionFinalResult queryCondition) {
        try {
            startPage();
            List<FactoryMonthPlanProductionFinalResult> list = factoryMonthPlanProductionFinalResultService.getDataList(queryCondition);
            return getDataTable(list);
        } finally {
            PageUtils.clearPage();
        }
    }

    /**
     * 条件拼接
     *
     * @param queryWrapper
     * @param queryVO
     */
    @Override
    protected void builderCondition(QueryWrapper<FactoryMonthPlanProductionFinalResult> queryWrapper, FactoryMonthPlanProductionFinalResult queryVO) {
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("productionNo")), "PRODUCTION_NO", queryVO.getFieldValueByFieldName("productionNo"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("factoryCode")), "FACTORY_CODE", queryVO.getFieldValueByFieldName("factoryCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("year")), "YEAR", queryVO.getFieldValueByFieldName("year"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("month")), "MONTH", queryVO.getFieldValueByFieldName("month"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("yearMonth")), "YEAR_MONTH", queryVO.getFieldValueByFieldName("yearMonth"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("monthPlanVersion")), "MONTH_PLAN_VERSION", queryVO.getFieldValueByFieldName("monthPlanVersion"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("productionVersion")), "PRODUCTION_VERSION", queryVO.getFieldValueByFieldName("productionVersion"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("productTypeCode")), "PRODUCT_TYPE_CODE", queryVO.getFieldValueByFieldName("productTypeCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("productStatus")), "PRODUCT_STATUS", queryVO.getFieldValueByFieldName("productStatus"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("structureName")), "STRUCTURE_NAME", queryVO.getFieldValueByFieldName("structureName"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("mainMaterialDesc")), "MAIN_MATERIAL_DESC", queryVO.getFieldValueByFieldName("mainMaterialDesc"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("mesMaterialCode")), "MES_MATERIAL_CODE", queryVO.getFieldValueByFieldName("mesMaterialCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("materialCode")), "MATERIAL_CODE", queryVO.getFieldValueByFieldName("materialCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("materialDesc")), "MATERIAL_DESC", queryVO.getFieldValueByFieldName("materialDesc"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("proSize")), "PRO_SIZE", queryVO.getFieldValueByFieldName("proSize"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("constructionStage")), "CONSTRUCTION_STAGE", queryVO.getFieldValueByFieldName("constructionStage"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("brand")), "BRAND", queryVO.getFieldValueByFieldName("brand"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("specifications")), "SPECIFICATIONS", queryVO.getFieldValueByFieldName("specifications"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("mainPattern")), "MAIN_PATTERN", queryVO.getFieldValueByFieldName("mainPattern"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("pattern")), "PATTERN", queryVO.getFieldValueByFieldName("pattern"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("mouldCavityQty")), "MOULD_CAVITY_QTY", queryVO.getFieldValueByFieldName("mouldCavityQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("typeBlockQty")), "TYPE_BLOCK_QTY", queryVO.getFieldValueByFieldName("typeBlockQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("heightQty")), "HEIGHT_QTY", queryVO.getFieldValueByFieldName("heightQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("averageQty")), "AVERAGE_QTY", queryVO.getFieldValueByFieldName("averageQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("inventorySalesRatio")), "INVENTORY_SALES_RATIO", queryVO.getFieldValueByFieldName("inventorySalesRatio"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("dayVulcanizationQty")), "DAY_VULCANIZATION_QTY", queryVO.getFieldValueByFieldName("dayVulcanizationQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("dynamicBalanceQty")), "DYNAMIC_BALANCE_QTY", queryVO.getFieldValueByFieldName("dynamicBalanceQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("uniformityQty")), "UNIFORMITY_QTY", queryVO.getFieldValueByFieldName("uniformityQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("isImport")), "IS_IMPORT", queryVO.getFieldValueByFieldName("isImport"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("productionSequence")), "PRODUCTION_SEQUENCE", queryVO.getFieldValueByFieldName("productionSequence"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("curingTime")), "CURING_TIME", queryVO.getFieldValueByFieldName("curingTime"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("prodReqPlan")), "PROD_REQ_PLAN", queryVO.getFieldValueByFieldName("prodReqPlan"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("heightProductionQty")), "HEIGHT_PRODUCTION_QTY", queryVO.getFieldValueByFieldName("heightProductionQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("factProdReqQty")), "FACT_PROD_REQ_QTY", queryVO.getFieldValueByFieldName("factProdReqQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("totalQty")), "TOTAL_QTY", queryVO.getFieldValueByFieldName("totalQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("midProductionQty")), "MID_PRODUCTION_QTY", queryVO.getFieldValueByFieldName("midProductionQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("cycleProductionQty")), "CYCLE_PRODUCTION_QTY", queryVO.getFieldValueByFieldName("cycleProductionQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("conventionProductionQty")), "CONVENTION_PRODUCTION_QTY", queryVO.getFieldValueByFieldName("conventionProductionQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("postponeProductionQty")), "POSTPONE_PRODUCTION_QTY", queryVO.getFieldValueByFieldName("postponeProductionQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("differenceQty")), "DIFFERENCE_QTY", queryVO.getFieldValueByFieldName("differenceQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("reason")), "REASON", queryVO.getFieldValueByFieldName("reason"));
    }

    @Override
    protected IDocService getDocService() {
        return factoryMonthPlanProductionFinalResultService;
    }

    @Override
    protected String getTypeCode() {
        return "FIN0001";
    }

    /**
     * 导出列表
     */
    @Log(title = "月计划定稿", businessType = BusinessType.EXPORT)
    @ApiOperation("导出数据")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody FactoryMonthPlanProductionFinalResult queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return super.exportData(queryVO, fileName, response);
    }

    /**
     * 导出SKU排产明细
     */
//    @Log(title = "导出SKU排产明细", businessType = BusinessType.EXPORT)
    @ApiOperation("导出SKU排产明细")
    @SuppressWarnings("unchecked")
    @PostMapping("/exportSkuScheduleItems/{fileName}")
    public byte[] exportSkuScheduleItems(@RequestBody FactoryMonthPlanProductionFinalResultParam param, @PathVariable("fileName") String fileName,
                                         HttpServletResponse response) throws IOException {
        Date beginTime = DateUtils.getNowDate();
        // 1、加载数据
        List<FactoryMonthPlanFinalAdjustVo> list = (List<FactoryMonthPlanFinalAdjustVo>)this.listSkuScheduleItems(param).getRows();
        List<FactoryMonthPlanFinalAdjustExportVo> exportList = list.stream().map(vo -> {
            FactoryMonthPlanFinalAdjustExportVo exportVo = new FactoryMonthPlanFinalAdjustExportVo();
            BeanUtils.copyProperties(vo, exportVo);
            return exportVo;
        }).collect(Collectors.toList());

        // 2、调用导出实例
        ExcelUtil<FactoryMonthPlanFinalAdjustExportVo> util = new ExcelUtil<>(FactoryMonthPlanFinalAdjustExportVo.class);
        Workbook workbook = util.exportExcel2(response, exportList, fileName);
        byte[] resultBytes =  ExcelReadUtils.writeExcel(workbook);
        Date endTime = DateUtils.getNowDate();
//        FileOutputStream fo = new FileOutputStream("test.xlsx");
//        fo.write(resultBytes);

        // 3、保存导出记录
        ExportLog exportLog = new ExportLog();
        exportLog.setProcedureCode("0");
        exportLog.setExportParams(param.toString());
        String uri = ServletUtils.getRequest().getRequestURI();
        exportLog.setFunctionCode(uri.split("/")[1]);
        exportLog.setFunctionName(fileName);
        exportLog.setFileName(fileName + ExcelUtil.XLSX_FILE);
        exportLog.setRowCount(list.size());
        exportLog.setBeginTime(beginTime);
        exportLog.setEndTime(endTime);
        exportLog.setSpendTime(DateUtils.getDiffTime(endTime,beginTime));
        iExportLogService.add(exportLog);
        return resultBytes;
    }

    @Override
    protected List<FactoryMonthPlanProductionFinalResult> listExportData(FactoryMonthPlanProductionFinalResult obj) {
        return factoryMonthPlanProductionFinalResultService.getDataList(obj);
    }

    /**
     * 查询版本列表
     */
    @ApiOperation("查询版本列表")
    @PostMapping("/getVersionList")
    public TableDataInfo getVersionList(@RequestBody FactoryMonthPlanProductionFinalResult queryVO) {
        this.startPage();
        List<FactoryMonthPlanProductionFinalResult> list = entityMapper.getVersionList(queryVO);
        this.clearPage();
        return this.getDataTable(list);
    }

    /**
     * 获取SKU排产明细
     */
    @ApiOperation("获取SKU排产明细")
    @PostMapping("/listSkuScheduleItems")
    public TableDataInfo listSkuScheduleItems(@RequestBody FactoryMonthPlanProductionFinalResultParam param) {
        if (null == param || null == param.getMonth() || null == param.getYear() || StringUtils.isBlank(param.getFactoryCode())) {
            throw new BusinessException(I18nUtil.getMessage("ui.data.query.param.checkFactoryYearMonth"));
        }

        // 创建计时器
        StopWatch watch = new StopWatch();
        watch.start();

        // 创建查询数据的异步任务
        // 查询SKU排产明细
        CompletableFuture<List<FactoryMonthPlanFinalAdjustVo>> monthPlanFinalAdjustFuture = CompletableFuture.supplyAsync(
                // 解决父子上下文传递问题
                SpringContextSupplierUtil.wrap(() -> queryFactoryMonthPlanFinalAdjustList(param))
        );
        // 查询BOM物料消耗明细
        CompletableFuture<List<MdmMaterialConsumeDetail>> materialConsumeDetailFuture = CompletableFuture.supplyAsync(() -> queryMaterialConsumeDetailList(param));
        // 查询特殊材料记录
        CompletableFuture<List<RawSpecialMaterialRecord>> specialMaterialFuture = CompletableFuture.supplyAsync(() -> querySpecialMaterialRecordList(param));
        try {
            // 等待所有异步任务执行完成
            CompletableFuture.allOf(
                    monthPlanFinalAdjustFuture,
                    materialConsumeDetailFuture,
                    specialMaterialFuture
            ).join();

            log.info("并行查询数据执行完成");

        } catch (CompletionException e) {
            // 异常处理
            Throwable throwable = e.getCause();
            log.error("查询数据失败! 失败原因:{}", throwable.getMessage(), throwable);
            throw new BusinessException(I18nUtil.getMessage("ui.data.alert.mpWeekRollAdjust.initDataFailure"), throwable);
        } finally {
            watch.stop();
        }

        List<FactoryMonthPlanFinalAdjustVo> monthPlanFinalAdjustVoList = monthPlanFinalAdjustFuture.join();
        List<MdmMaterialConsumeDetail> materialConsumeDetailList = materialConsumeDetailFuture.join();
        List<RawSpecialMaterialRecord> specialMaterialRecordList = specialMaterialFuture.join();

        // 遍历设置是否特殊材料
        for (FactoryMonthPlanFinalAdjustVo monthPlanFinalAdjustVo : monthPlanFinalAdjustVoList) {
//            boolean hasSpecialMaterial = hasSpecialMaterial(monthPlanFinalAdjustVo.getMaterialCode(), materialConsumeDetailList, specialMaterialRecordList);
//            monthPlanFinalAdjustVo.setHasSpecialMaterial(hasSpecialMaterial ? ApsConstant.TRUE : ApsConstant.FALSE);
            monthPlanFinalAdjustVo.setHasSpecialMaterial(null);
        }

        // 集合逗号分隔字段升序排序
        CommaFieldSortUtil.sortAndUpdateCommaField(monthPlanFinalAdjustVoList, FactoryMonthPlanFinalAdjustVo::getCxMachineCode, FactoryMonthPlanFinalAdjustVo::setCxMachineCode);

        return getDataTable(monthPlanFinalAdjustVoList);
    }


    /**
     * 保存
     */
    @ApiOperation("保存")
    @PostMapping("/save")
    @Override
    public AjaxResult save(@RequestBody FactoryMonthPlanProductionFinalResult factoryMonthPlanProdFinal){
        return super.save(factoryMonthPlanProdFinal);
    }



    /**
     * 判断是否特殊材料
     * @param targetEmbryoCode 目标胚胎编码
     * @param mdmMaterialConsumeDetailList BOM物料消耗明细列表
     * @param specialMaterialList 特殊材料清单列表
     * @return
     */
    protected boolean hasSpecialMaterial(String targetEmbryoCode, List<MdmMaterialConsumeDetail> mdmMaterialConsumeDetailList,
                                         List<RawSpecialMaterialRecord> specialMaterialList) {

        if (StringUtils.isEmpty(targetEmbryoCode) || PubUtil.isEmpty(mdmMaterialConsumeDetailList)
                || PubUtil.isEmpty(specialMaterialList)) {
            return Boolean.FALSE;
        }

        // 从BOM物料消耗明细列表中通过胎胚代码筛选出匹配的所有数据
        Set<String> childMaterialCodes = mdmMaterialConsumeDetailList.stream()
                .filter(detail -> StringUtils.equals(targetEmbryoCode, detail.getEmbryoCode()))
                .map(MdmMaterialConsumeDetail::getChildMaterialCode)
                .collect(Collectors.toSet());

        // 如果没有匹配到直接返回false
        if (PubUtil.isEmpty(childMaterialCodes)) {
            return Boolean.FALSE;
        }

        // 检查特殊材料清单列表中是否存在匹配的数据
        return specialMaterialList.stream()
                .map(RawSpecialMaterialRecord::getMaterialCode)
                .anyMatch(childMaterialCodes::contains);
    }


    private List<FactoryMonthPlanFinalAdjustVo> queryFactoryMonthPlanFinalAdjustList(FactoryMonthPlanProductionFinalResultParam param) {
        FactoryMonthPlanProductionFinalResult condition = new FactoryMonthPlanProductionFinalResult();
        BeanUtils.copyProperties(param, condition);
        List<FactoryMonthPlanProductionFinalResult> list = factoryMonthPlanProductionFinalResultService.getDataList(condition);
        return BeanUtil.copyToList(list, FactoryMonthPlanFinalAdjustVo.class);
    }


    /**
     * 查询特殊材料记录
     *
     * @param param
     */
    private List<RawSpecialMaterialRecord> querySpecialMaterialRecordList(FactoryMonthPlanProductionFinalResultParam param) {
        RawSpecialMaterialRecord queryVO = new RawSpecialMaterialRecord();
        queryVO.setFactoryCode(param.getFactoryCode());

        LambdaQueryWrapper<RawSpecialMaterialRecord> queryWrapper = new LambdaQueryWrapper<>();
        buildSpecialMaterialCondition(queryWrapper, queryVO);
        return rawSpecialMaterialRecordMapper.selectList(queryWrapper);
    }

    /**
     * 构建特殊材料条件
     *
     * @param queryWrapper
     * @param queryVO
     */
    private void buildSpecialMaterialCondition(LambdaQueryWrapper<RawSpecialMaterialRecord> queryWrapper, RawSpecialMaterialRecord queryVO) {
        queryWrapper.eq(RawSpecialMaterialRecord::getFactoryCode, queryVO.getFactoryCode());
        queryWrapper.eq(RawSpecialMaterialRecord::getIsDelete, YesOrNoEnum.NO.getValue());
    }

    /**
     * 查询BOM物料消耗明细
     *
     * @param param
     */
    private List<MdmMaterialConsumeDetail> queryMaterialConsumeDetailList(FactoryMonthPlanProductionFinalResultParam param) {
        MdmMaterialConsumeDetail queryVO = new MdmMaterialConsumeDetail();
        queryVO.setFactoryCode(param.getFactoryCode());

        LambdaQueryWrapper<MdmMaterialConsumeDetail> queryWrapper = new LambdaQueryWrapper<>();
        buildMaterialConsumeDetailCondition(queryWrapper, queryVO);
        return mdmMaterialConsumeDetailMapper.selectList(queryWrapper);
    }

    /**
     * 构建BOM物料消耗明细条件
     *
     * @param queryWrapper
     * @param queryVO
     */
    private void buildMaterialConsumeDetailCondition(LambdaQueryWrapper<MdmMaterialConsumeDetail> queryWrapper, MdmMaterialConsumeDetail queryVO) {
        queryWrapper.eq(MdmMaterialConsumeDetail::getFactoryCode, queryVO.getFactoryCode());
        queryWrapper.eq(MdmMaterialConsumeDetail::getIsDelete, YesOrNoEnum.NO.getValue());
    }

    /**
     * 下发月计划
     *
     * @param factoryMonthPlanProdFinal 参数
     * @return 结果
     */
    @ApiOperation("下发月计划 - 年月+分厂+需求计划版本+分厂月计划版本")
    @PostMapping("/issueMonthPlan")
    public AjaxResult issueMonthPlan(@RequestBody FactoryMonthPlanProductionFinalResult factoryMonthPlanProdFinal) {
        return factoryMonthPlanProductionFinalResultService.issueMonthPlan(factoryMonthPlanProdFinal);
    }
}
