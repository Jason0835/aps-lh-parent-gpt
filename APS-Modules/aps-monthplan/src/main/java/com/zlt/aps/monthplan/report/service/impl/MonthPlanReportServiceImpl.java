package com.zlt.aps.monthplan.report.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.api.gateway.system.service.ISysConfigService;
import com.ruoyi.api.gateway.system.service.ISysDictDataCacheService;
import com.ruoyi.common.core.domain.SysDictData;
import com.ruoyi.common.core.utils.bean.BeanUtils;
import com.ruoyi.common.core.utils.reflect.ReflectUtils;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.constant.FactoryConstant;
import com.zlt.aps.enums.*;
import com.zlt.aps.utils.BeanCopyUtils;
import com.zlt.aps.utils.JsonI18nConvertUtils;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.core.domain.ExcelCellRangeAddress;
import com.zlt.aps.common.core.enums.BrandProSizeSummaryTypeEnum;
import com.zlt.aps.common.core.enums.HalfComponentMeteringUnitEnums;
import com.zlt.aps.common.core.utils.BigDecimalUtils;
import com.zlt.aps.common.core.utils.ExcelUtils;
import com.zlt.aps.factory.utils.DateUtils;
import com.zlt.aps.maindata.mapper.ItfInterfaceLogEntityMapper;
import com.zlt.aps.maindata.mapper.MdmMaterialInfoEntityMapper;
import com.zlt.aps.monthplan.api.domain.dto.*;
import com.zlt.aps.monthplan.api.domain.entity.*;
import com.zlt.aps.monthplan.api.domain.vo.*;
import com.zlt.aps.monthplan.demand.mapper.OrderPlanAllocationMapper;
import com.zlt.aps.monthplan.factory.mapper.FactoryMonthPlanProdFinalMapper;
import com.zlt.aps.monthplan.report.mapper.DemoOrderAcceptEntityMapper;
import com.zlt.aps.monthplan.report.mapper.DemoYearPlanFinishEntityMapper;
import com.zlt.aps.monthplan.report.mapper.MonthPlanReportMapper;
import com.zlt.aps.monthplan.report.service.IMonthPlanReportService;
import com.zlt.common.utils.PubUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Chen
 */
@Slf4j
@Service
public class MonthPlanReportServiceImpl implements IMonthPlanReportService {

    @Autowired
    private MonthPlanReportMapper monthPlanReportMapper;

    /**
     * 月份
     */
    private static final int MONTH_NUM = 12;
    /**
     * 月份最大天数
     */
    private static final int MONTH_DAY = 31;
    /**
     * 比例转换数值
     */
    private static final BigDecimal SCALING_NUM = new BigDecimal("100");
    /**
     * 库位类别字典类型
     */
    private final static String LOCATION_DICT_TYPE = "biz_stor_type";
    /**
     * 渠道字典类型
     */
    private final static String CHANNEL_DICT_TYPE = "biz_channel_type";
    /**
     * 品牌字典类型
     */
    private final static String BRAND_DICT_TYPE = "biz_brand_type";
    @Autowired
    private ISysDictDataCacheService sysDictDataService;

    /**
     * 分隔符
     */
    private static final String DELIMITER = ",";
    @Autowired
    private OrderPlanAllocationMapper orderPlanAllocationMapper;

    /**
     * 胎类区分-排产受限满足率，四季胎花纹列表
     */
    private static final List<String> PATTERN_LIST = Arrays.asList("WASP-PLUS", "TUTTI HP", "DASP-PLUS");

    /**
     * 胎类区分-排产受限满足率，轻卡四季胎花纹列表
     */
    private static final List<String> LT_PATTERN_LIST = Arrays.asList("WASL-PLUS", "TUTTI TRO", "DASL-PLUS");
    @Autowired
    private FactoryMonthPlanProdFinalMapper factoryMonthPlanProdFinalMapper;

    /**
     * 会计周期本月开始日期
     */
    private static final int THIS_MONTH_START_DAY = 1;

    /**
     * 会计周期本月结束日期
     */
    private static final int THIS_MONTH_END_DAY = 25;

    /**
     * 会计周期上个月开始日期
     */
    private static final int LAST_MONTH_START_DAY = 26;

    /**
     * 会计周期上个月结束日期
     */
    private static final int LAST_MONTH_END_DAY = 31;
    @Autowired
    private ItfInterfaceLogEntityMapper itfInterfaceLogEntityMapper;

    /**
     * 根据map设置字段值
     *
     * @param classificationGapVos 数据列表
     * @param salePlanMap          销售计划map
     * @param proPlanMap           生产计划map
     * @param stockPlanMap         库存map
     */
    private static void setFieldValueByMap(List<ClassificationGapVo> classificationGapVos, Map<String, ClassificationGapVo> salePlanMap, Map<String, ClassificationGapVo> proPlanMap, Map<String, ClassificationGapVo> stockPlanMap, Map<String, ClassificationGapVo> adjustNoticeMap) {
        // 赋值对应字段
        for (ClassificationGapVo classificationVo : classificationGapVos) {
            String mapKey = String.join("|", classificationVo.getProductCode(),
                    classificationVo.getBrand(), classificationVo.getProSize(), classificationVo.getLocationType(), classificationVo.getChannel());
            if (salePlanMap.containsKey(mapKey)) {
                ClassificationGapVo classificationGapVo = salePlanMap.get(mapKey);
                BigDecimal salePlanQty = classificationGapVo.getSalePlanQty();
                BigDecimal saleSkuCount = classificationGapVo.getSaleSkuCount();
                classificationVo.setSaleSkuCount(saleSkuCount);
                classificationVo.setSalePlanQty(salePlanQty);
            }

            // 添加调整通知单计划数
            if (adjustNoticeMap.containsKey(mapKey)) {
                ClassificationGapVo classificationGapVo = salePlanMap.get(mapKey);
                BigDecimal salePlanQty = classificationGapVo.getSalePlanQty();
                BigDecimal saleSkuCount = classificationGapVo.getSaleSkuCount();
                classificationVo.setSaleSkuCount(BigDecimalUtils.add(classificationVo.getSaleSkuCount(), saleSkuCount));
                classificationVo.setSalePlanQty(BigDecimalUtils.add(classificationVo.getSalePlanQty(), salePlanQty));
            }

            if (proPlanMap.containsKey(mapKey)) {
                ClassificationGapVo classificationGapVo = proPlanMap.get(mapKey);
                BigDecimal producePlanQty = classificationGapVo.getProducePlanQty();
                BigDecimal produceSkuCount = classificationGapVo.getProduceSkuCount();
                classificationVo.setProduceSkuCount(produceSkuCount);
                classificationVo.setProducePlanQty(producePlanQty);
            }

            if (stockPlanMap.containsKey(mapKey)) {
                ClassificationGapVo classificationGapVo = stockPlanMap.get(mapKey);
                BigDecimal stockQty = classificationGapVo.getStockQty();
                classificationVo.setStockQty(stockQty);
            }
            BigDecimal stockQty = classificationVo.getStockQty();
            BigDecimal proPlanQty = classificationVo.getProducePlanQty();
            BigDecimal salePlanQty = classificationVo.getSalePlanQty();
            BigDecimal orderGapQty = BigDecimalUtils.sub(BigDecimalUtils.add(stockQty, proPlanQty), salePlanQty);
            classificationVo.setOrderGapQty(orderGapQty);
            if (orderGapQty.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal gapSkuCount = ObjectUtils.defaultIfNull(classificationVo.getGapSkuCount(), BigDecimal.ZERO);
                classificationVo.setGapSkuCount(BigDecimalUtils.add(gapSkuCount, BigDecimal.ONE));
            }
        }
    }

    /**
     * 导出T月完成率数据
     *
     * @param queryDto 查询参数
     * @return 文件数组
     */
    @Override
    public byte[] exportMonthFinishRate(MonthPlanReportDto queryDto) {
        // 获取模板
        ClassLoader classLoader = this.getClass().getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream("excelModel/monthFinishRate.xlsx");
        // 表头信息
        Map<String, Object> tableMap = new HashMap<>(16);
        tableMap.put("title", queryDto.getMonth() + "月份产销完成率、达成率分析");
        // 列表数据
        List<List<Map<String, Object>>> excelDataList = new ArrayList<>();
        // 查询数据
        MonthFinishRateVo monthFinishRateVo = this.listMonthFinishRate(queryDto);
        tableMap.put("producePlanQty", monthFinishRateVo.getProducePlanQty());
        tableMap.put("produceFinishPlanQty", monthFinishRateVo.getProduceFinishPlanQty());
        tableMap.put("produceFinishRate", monthFinishRateVo.getProduceFinishRate());

        tableMap.put("salePlanQty", monthFinishRateVo.getSalePlanQty());
        tableMap.put("saleFinishPlanQty", monthFinishRateVo.getSaleFinishPlanQty());
        tableMap.put("saleFinishRate", monthFinishRateVo.getSaleFinishRate());

        List<MonthFinishRateRangeVo> produceResultList = monthFinishRateVo.getProduceResultList();
        if (produceResultList != null && !produceResultList.isEmpty()) {
            List<Map<String, Object>> list = new ArrayList<>();
            for (int i = 0; i < produceResultList.size(); i++) {
                Map<String, Object> listDataMap = new HashMap<>(16);
                String suffix = ExcelUtils.convertIndexToLetter(i);
                MonthFinishRateRangeVo monthFinishRateRangeVo = produceResultList.get(i);
                tableMap.put("produceRangeLabel", monthFinishRateRangeVo.getRangeLabel());
                tableMap.put("produceSkuCount" + suffix, monthFinishRateRangeVo.getSkuCount());
                tableMap.put("produceUnit", monthFinishRateRangeVo.getUnit());
                list.add(listDataMap);
                excelDataList.add(list);
            }
        }

        List<MonthFinishRateRangeVo> saleResultList = monthFinishRateVo.getSaleResultList();
        if (saleResultList != null && !saleResultList.isEmpty()) {
            List<Map<String, Object>> list = new ArrayList<>();
            for (int i = 0; i < saleResultList.size(); i++) {
                Map<String, Object> listDataMap = new HashMap<>(16);
                String suffix = ExcelUtils.convertIndexToLetter(i);
                MonthFinishRateRangeVo monthFinishRateRangeVo = saleResultList.get(i);
                tableMap.put("saleRangeLabel", monthFinishRateRangeVo.getRangeLabel());
                tableMap.put("saleSkuCount" + suffix, monthFinishRateRangeVo.getSkuCount());
                tableMap.put("saleUnit", monthFinishRateRangeVo.getUnit());
                list.add(listDataMap);
                excelDataList.add(list);
            }
        }
        // 写到文件
        return ExcelUtils.writeMultiList(inputStream
                , 0, tableMap, excelDataList);
    }

    /**
     * 查询T月完成率列表数据
     *
     * @param queryDto 查询参数
     * @return 结果
     */
    @Override
    public MonthFinishRateVo listMonthFinishRate(MonthPlanReportDto queryDto) {
        // 查询终稿的版本，用于查询关联版本、过滤数据
        String monthPlanVersion = monthPlanReportMapper.selectFinalMonthPlanVersion(queryDto);
        queryDto.setMonthPlanVersion(monthPlanVersion);

        List<MonthFinishRateRangeVo> produceResultList = monthPlanReportMapper.listProduceMonthFinishRate(queryDto);
        List<MonthFinishRateRangeVo> saleResultList = monthPlanReportMapper.listSaleMonthFinishRate(queryDto);
        MonthFinishRateVo monthFinishRateVo = monthPlanReportMapper.selectProduceAndSale(queryDto);
        // 调整通知单数量
//        List<MonthPlanNoticeOrder> monthPlanNoticeOrderList = monthPlanReportMapper.selectMonthPlanAdjustNotice(queryDto);
//        if (CollectionUtils.isNotEmpty(monthPlanNoticeOrderList)) {
//            Long sumAdjustNoticePlan = monthPlanNoticeOrderList.stream().filter(item -> item.getNeedQty() != null && item.getNeedQty() > 0)
//                    .mapToLong(MonthPlanNoticeOrder::getNeedQty).sum();
//            // 添加调整通知单计划数
//            monthFinishRateVo.setProducePlanQty(monthFinishRateVo.getProducePlanQty() + sumAdjustNoticePlan.intValue());
//        }

        Integer producePlanQty = monthFinishRateVo.getProducePlanQty();
        Integer produceFinishPlanQty = monthFinishRateVo.getProduceFinishPlanQty();
        BigDecimal produceFinishRate = null;
        if (produceFinishPlanQty != null && producePlanQty != null && producePlanQty != 0) {
            produceFinishRate = BigDecimal.valueOf(produceFinishPlanQty).divide(BigDecimal.valueOf(producePlanQty), 2, RoundingMode.HALF_UP);
        }
        monthFinishRateVo.setProduceFinishRate(produceFinishRate);

        Integer saleFinishPlanQty = monthFinishRateVo.getSaleFinishPlanQty();
        Integer salePlanQty = monthFinishRateVo.getSalePlanQty();
        BigDecimal saleFinishRate = null;
        if (saleFinishPlanQty != null && salePlanQty != null && salePlanQty != 0) {
            saleFinishRate = BigDecimal.valueOf(saleFinishPlanQty).divide(BigDecimal.valueOf(salePlanQty), 2, RoundingMode.HALF_UP);
        }
        monthFinishRateVo.setSaleFinishRate(saleFinishRate);

        // 试制或量试
        MonthFinishRateRangeVo monthFinishRateRangeVo = monthPlanReportMapper.selectTrialProduceMonthFinishRate(queryDto);
        if (monthFinishRateRangeVo == null) {
            monthFinishRateRangeVo = new MonthFinishRateRangeVo();
            monthFinishRateRangeVo.setRangeLabel("试制或量试");
            monthFinishRateRangeVo.setRangeKey("-1");
            monthFinishRateRangeVo.setSkuCount(0);
            monthFinishRateRangeVo.setUnit("");
        }
        produceResultList.add(monthFinishRateRangeVo);
        monthFinishRateVo.setProduceResultList(produceResultList);
        monthFinishRateVo.setSaleResultList(saleResultList);
        return monthFinishRateVo;
    }

    /**
     * 导出T月完成率-品牌数据
     *
     * @param queryDto 查询参数
     * @return 文件数组
     */
    @Override
    public byte[] exportMonthFinishRateBrand(MonthPlanReportDto queryDto) {
        // 获取模板
        ClassLoader classLoader = this.getClass().getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream("excelModel/monthFinishRateBrand.xlsx");
        // 表头信息
        Map<String, Object> tableMap = new HashMap<>(16);
        tableMap.put("title", queryDto.getMonth() + "月份分品牌产销数量分析");
        // 列表数据
        List<List<Map<String, Object>>> excelDataList = new ArrayList<>();
        // 查询数据
        List<MonthFinishRateBrandVo> brandVoList = this.listMonthFinishRateBrand(queryDto);

        if (brandVoList != null && !brandVoList.isEmpty()) {
            List<Map<String, Object>> list = new ArrayList<>();
            MonthFinishRateBrandVo sumBrandVo = new MonthFinishRateBrandVo();

            for (MonthFinishRateBrandVo finishRateBrandVo : brandVoList) {
                Map<String, Object> listDataMap = new HashMap<>(16);
                putFieldToMap(finishRateBrandVo, listDataMap, Collections.emptyList(), StringUtils.EMPTY, Boolean.FALSE);
                Integer stockSkuCount = ObjectUtils.defaultIfNull(finishRateBrandVo.getStockSkuCount(), 0);
                sumBrandVo.setStockSkuCount(ObjectUtils.defaultIfNull(sumBrandVo.getStockSkuCount(), 0) + stockSkuCount);

                Integer stockQty = ObjectUtils.defaultIfNull(finishRateBrandVo.getStockQty(), 0);
                sumBrandVo.setStockQty(ObjectUtils.defaultIfNull(sumBrandVo.getStockQty(), 0) + stockQty);

                Integer produceSkuCount = ObjectUtils.defaultIfNull(finishRateBrandVo.getProduceSkuCount(), 0);
                sumBrandVo.setProduceSkuCount(ObjectUtils.defaultIfNull(sumBrandVo.getProduceSkuCount(), 0) + produceSkuCount);

                Integer producePlanQty = ObjectUtils.defaultIfNull(finishRateBrandVo.getProducePlanQty(), 0);
                sumBrandVo.setProducePlanQty(ObjectUtils.defaultIfNull(sumBrandVo.getProducePlanQty(), 0) + producePlanQty);

                Integer produceFinishPlanQty = ObjectUtils.defaultIfNull(finishRateBrandVo.getProduceFinishPlanQty(), 0);
                sumBrandVo.setProduceFinishPlanQty(ObjectUtils.defaultIfNull(sumBrandVo.getProduceFinishPlanQty(), 0) + produceFinishPlanQty);

                Integer salePlanSkuCount = ObjectUtils.defaultIfNull(finishRateBrandVo.getSalePlanSkuCount(), 0);
                sumBrandVo.setSalePlanSkuCount(ObjectUtils.defaultIfNull(sumBrandVo.getSalePlanSkuCount(), 0) + salePlanSkuCount);

                Integer salePlanQty = ObjectUtils.defaultIfNull(finishRateBrandVo.getSalePlanQty(), 0);
                sumBrandVo.setSalePlanQty(ObjectUtils.defaultIfNull(sumBrandVo.getSalePlanQty(), 0) + salePlanQty);

                Integer saleFinishSkuCount = ObjectUtils.defaultIfNull(finishRateBrandVo.getSaleFinishSkuCount(), 0);
                sumBrandVo.setSaleFinishSkuCount(ObjectUtils.defaultIfNull(sumBrandVo.getSaleFinishSkuCount(), 0) + saleFinishSkuCount);

                Integer saleFinishPlanQty = ObjectUtils.defaultIfNull(finishRateBrandVo.getSaleFinishPlanQty(), 0);
                sumBrandVo.setSaleFinishPlanQty(ObjectUtils.defaultIfNull(sumBrandVo.getSaleFinishPlanQty(), 0) + saleFinishPlanQty);
                list.add(listDataMap);
            }

            // 添加合计行
            Map<String, Object> listDataMap = new HashMap<>(16);
            sumBrandVo.setBrandName("合计");
            calculateFinishRateBrandRate(sumBrandVo);
            putFieldToMap(sumBrandVo, listDataMap, Collections.emptyList(), StringUtils.EMPTY, Boolean.FALSE);
            list.add(listDataMap);
            excelDataList.add(list);
        }
        // 写到文件
        return ExcelUtils.writeMultiList(inputStream
                , 0, tableMap, excelDataList);
    }

    /**
     * 计算T月完成率-品牌列表满足率等数据
     *
     * @param monthFinishRateBrandVo 要计算的数据
     */
    private static void calculateFinishRateBrandRate(MonthFinishRateBrandVo monthFinishRateBrandVo) {
        // 生产完成率计算
        Integer producePlanQty = ObjectUtils.defaultIfNull(monthFinishRateBrandVo.getProducePlanQty(), 0);
        BigDecimal produceFinishRate = null;
        Integer produceFinishPlanQty = ObjectUtils.defaultIfNull(monthFinishRateBrandVo.getProduceFinishPlanQty(), 0);
        if (producePlanQty != 0) {
            produceFinishRate = BigDecimal.valueOf(produceFinishPlanQty).divide(BigDecimal.valueOf(producePlanQty), 2, RoundingMode.HALF_UP);
        }
        monthFinishRateBrandVo.setProduceFinishRate(produceFinishRate);

        // 准确率计算
        Integer saleFinishPlanQty = ObjectUtils.defaultIfNull(monthFinishRateBrandVo.getSaleFinishPlanQty(), 0);
        Integer salePlanQty = ObjectUtils.defaultIfNull(monthFinishRateBrandVo.getSalePlanQty(), 0);
        BigDecimal accuracyRate = null;
        if (salePlanQty != 0) {
            accuracyRate = BigDecimal.valueOf(saleFinishPlanQty).divide(BigDecimal.valueOf(salePlanQty), 2, RoundingMode.HALF_UP);
        }
        monthFinishRateBrandVo.setAccuracyRate(accuracyRate);

        // 排产满足率计算
        Integer stockQty = ObjectUtils.defaultIfNull(monthFinishRateBrandVo.getStockQty(), 0);
        int produceQty = stockQty + producePlanQty;
        BigDecimal produceSatisfyRate = null;
        if (salePlanQty != 0) {
            produceSatisfyRate = BigDecimal.valueOf(produceQty).divide(BigDecimal.valueOf(salePlanQty), 2, RoundingMode.HALF_UP);
        }
        monthFinishRateBrandVo.setProduceSatisfyRate(produceSatisfyRate);

        // 完成满足率计算
        int saleQty = stockQty + produceFinishPlanQty;
        BigDecimal finishSatisfyRate = null;
        if (saleFinishPlanQty != 0) {
            finishSatisfyRate = BigDecimal.valueOf(saleQty).divide(BigDecimal.valueOf(saleFinishPlanQty), 2, RoundingMode.HALF_UP);
        }
        monthFinishRateBrandVo.setFinishSatisfyRate(finishSatisfyRate);

    }

    /**
     * 查询T月完成率-品牌列表数据
     *
     * @param queryDto 查询参数
     * @return 结果
     */
    @Override
    public List<MonthFinishRateBrandVo> listMonthFinishRateBrand(MonthPlanReportDto queryDto) {
        // 查询终稿的版本，用于查询关联版本、过滤数据
        String monthPlanVersion = monthPlanReportMapper.selectFinalMonthPlanVersion(queryDto);
        queryDto.setMonthPlanVersion(monthPlanVersion);
        List<MonthFinishRateBrandVo> brandVoList = monthPlanReportMapper.selectMonthPlanFinishBrand(queryDto);
        Map<String, String> brandDictMap = getDictMapByType(BRAND_DICT_TYPE);
        // 调整通知单数量
//        List<MonthPlanNoticeOrder> monthPlanNoticeOrderList = monthPlanReportMapper.selectMonthPlanAdjustNotice(queryDto);
//        Map<String, List<MonthPlanNoticeOrder>> adjustNoticeMap = new HashMap<>(16);
//        if (CollectionUtils.isNotEmpty(monthPlanNoticeOrderList)) {
//            adjustNoticeMap = monthPlanNoticeOrderList.stream().filter(item -> item.getNeedQty() != null && item.getNeedQty() > 0)
//                    .collect(Collectors.groupingBy(MonthPlanNoticeOrder::getBrand));
//        }
//
//        for (MonthFinishRateBrandVo monthFinishRateBrandVo : brandVoList) {
//            String brand = monthFinishRateBrandVo.getBrand();
//            if (adjustNoticeMap.containsKey(brand)) {
//                List<MonthPlanNoticeOrder> noticeList = adjustNoticeMap.get(brand);
//                int sumAdjustQty = 0;
//                for (MonthPlanNoticeOrder planNoticeOrder : noticeList) {
//                    sumAdjustQty += planNoticeOrder.getNeedQty();
//                }
//                // 添加调整通知单计划数
//                monthFinishRateBrandVo.setSalePlanQty(monthFinishRateBrandVo.getSalePlanQty() + sumAdjustQty);
//                monthFinishRateBrandVo.setSalePlanSkuCount(monthFinishRateBrandVo.getSalePlanSkuCount() + noticeList.size());
//            }
//            calculateFinishRateBrandRate(monthFinishRateBrandVo);
//            String dictLabel = brandDictMap.getOrDefault(brand, brand);
//            monthFinishRateBrandVo.setBrandName(dictLabel);
//        }
        return brandVoList;
    }

    /**
     * 导出T月完成率-品牌寸别数据
     *
     * @param queryDto 查询参数
     * @return 文件数组
     */
    @Override
    public byte[] exportMonthFinishRateBrandProSize(MonthPlanReportDto queryDto) {
        // 获取模板
        ClassLoader classLoader = this.getClass().getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream("excelModel/monthFinishRateBrandProSize.xlsx");
        // 表头信息
        Map<String, Object> tableMap = new HashMap<>(16);
        tableMap.put("title", queryDto.getMonth() + "月份分品牌寸别产销分析");
        // 列表数据
        List<List<Map<String, Object>>> excelDataList = new ArrayList<>();
        // 查询数据
        List<MonthFinishRateProSizeVo> brandVoProSizeList = this.listMonthFinishRateBrandProSize(queryDto);

        if (brandVoProSizeList != null && !brandVoProSizeList.isEmpty()) {
            List<Map<String, Object>> list = new ArrayList<>();
            List<ExcelCellRangeAddress> rangeAddressList = new ArrayList<>();
            int startMergeRowNum = 3;
            int endMergeRowNum = startMergeRowNum - 1;

            Map<String, List<MonthFinishRateProSizeVo>> brandGroupMap = brandVoProSizeList.stream().collect(Collectors.groupingBy(MonthFinishRateProSizeVo::getBrandName, LinkedHashMap::new, Collectors.toList()));
            Set<Map.Entry<String, List<MonthFinishRateProSizeVo>>> entrySet = brandGroupMap.entrySet();
            for (Map.Entry<String, List<MonthFinishRateProSizeVo>> entry : entrySet) {
                MonthFinishRateProSizeVo sumBrandProSizeVo = new MonthFinishRateProSizeVo();
                List<MonthFinishRateProSizeVo> monthFinishRateProSizeVoList = entry.getValue();

                for (MonthFinishRateProSizeVo finishRateProSizeVo : monthFinishRateProSizeVoList) {
                    Map<String, Object> listDataMap = new HashMap<>(16);
                    putFieldToMap(finishRateProSizeVo, listDataMap, Collections.emptyList(), StringUtils.EMPTY, Boolean.FALSE);
                    Integer producePlanQty = ObjectUtils.defaultIfNull(finishRateProSizeVo.getProducePlanQty(), 0);
                    sumBrandProSizeVo.setProducePlanQty(ObjectUtils.defaultIfNull(sumBrandProSizeVo.getProducePlanQty(), 0) + producePlanQty);

                    Integer produceFinishPlanQty = ObjectUtils.defaultIfNull(finishRateProSizeVo.getProduceFinishPlanQty(), 0);
                    sumBrandProSizeVo.setProduceFinishPlanQty(ObjectUtils.defaultIfNull(sumBrandProSizeVo.getProduceFinishPlanQty(), 0) + produceFinishPlanQty);

                    Integer salePlanQty = ObjectUtils.defaultIfNull(finishRateProSizeVo.getSalePlanQty(), 0);
                    sumBrandProSizeVo.setSalePlanQty(ObjectUtils.defaultIfNull(sumBrandProSizeVo.getSalePlanQty(), 0) + salePlanQty);

                    Integer saleFinishPlanQty = ObjectUtils.defaultIfNull(finishRateProSizeVo.getSaleFinishPlanQty(), 0);
                    sumBrandProSizeVo.setSaleFinishPlanQty(ObjectUtils.defaultIfNull(sumBrandProSizeVo.getSaleFinishPlanQty(), 0) + saleFinishPlanQty);
                    list.add(listDataMap);
                    endMergeRowNum++;
                }

                // 添加合计行
                Map<String, Object> listDataMap = new HashMap<>(16);
                sumBrandProSizeVo.setProSize("合计");
                calculateMonthSkuSummaryList(sumBrandProSizeVo);
                putFieldToMap(sumBrandProSizeVo, listDataMap, Collections.emptyList(), StringUtils.EMPTY, Boolean.FALSE);
                list.add(listDataMap);
                endMergeRowNum++;

                // 合并单元格
                ExcelCellRangeAddress address2 = new ExcelCellRangeAddress(startMergeRowNum, endMergeRowNum, 0, 0);
                rangeAddressList.add(address2);

                startMergeRowNum = endMergeRowNum + 1;
                endMergeRowNum = startMergeRowNum - 1;
            }


            tableMap.put(ExcelUtils.RANGE_ADDRESS, rangeAddressList);
            excelDataList.add(list);
        }
        // 写到文件
        return ExcelUtils.writeMultiList(inputStream
                , 0, tableMap, excelDataList);
    }

    /**
     * 计算T月完成率-品牌列表满足率等数据
     *
     * @param finishRateProSizeVo 要计算的数据
     */
    private static void calculateMonthSkuSummaryList(MonthFinishRateProSizeVo finishRateProSizeVo) {
        // 生产完成率计算
        Integer producePlanQty = finishRateProSizeVo.getProducePlanQty();
        Integer produceFinishPlanQty = finishRateProSizeVo.getProduceFinishPlanQty();
        BigDecimal produceFinishRate = null;
        if (Objects.nonNull(produceFinishPlanQty) && Objects.nonNull(producePlanQty) && producePlanQty != 0) {
            produceFinishRate = BigDecimal.valueOf(produceFinishPlanQty).divide(BigDecimal.valueOf(producePlanQty), 2, RoundingMode.HALF_UP);
        }
        finishRateProSizeVo.setProduceFinishRate(produceFinishRate);

        // 准确率计算
        Integer saleFinishPlanQty = finishRateProSizeVo.getSaleFinishPlanQty();
        Integer salePlanQty = finishRateProSizeVo.getSalePlanQty();
        BigDecimal saleFinishRate = null;
        if (Objects.nonNull(saleFinishPlanQty) && Objects.nonNull(salePlanQty) && salePlanQty != 0) {
            saleFinishRate = BigDecimal.valueOf(saleFinishPlanQty).divide(BigDecimal.valueOf(salePlanQty), 2, RoundingMode.HALF_UP);
        }
        finishRateProSizeVo.setSaleFinishRate(saleFinishRate);
    }

    /**
     * 转换品牌+库位、寸别+渠道的情况
     *
     * @param reportClassificationVoList 报表基础数据列表
     * @param mainDictMap                列表展示的主字段字典，找不到对应字典，则显示对应字典值
     * @param assistDictMap              分组的辅字段字典，找不到对应字典，则显示对应字典值
     */
    private static void transformTwoDict(List<ReportClassificationVo> reportClassificationVoList, Map<String, String> mainDictMap, Map<String, String> assistDictMap) {
        if (PubUtil.isEmpty(reportClassificationVoList)) {
            return;
        }
        for (ReportClassificationVo classificationVo : reportClassificationVoList) {
            String classificationValue = classificationVo.getClassificationValue();
            if (classificationValue != null && classificationValue.contains("|")) {
                String[] splitNameArr = classificationValue.split("\\|");
                for (int i = 0; i < splitNameArr.length; i++) {
                    String dictValue = splitNameArr[i];
                    if (i == 0) {
                        String dictLabel = mainDictMap.getOrDefault(dictValue, dictValue);
                        // 前端展示的主字段，品牌+库位=品牌、寸别+渠道=寸别
                        classificationVo.setClassificationName(dictLabel);
                    }
                    if (i == 1) {
                        String dictLabel = assistDictMap.getOrDefault(dictValue, dictValue);
                        classificationVo.setClassificationName1(dictLabel);
                    }
                }
            }
        }
    }

    /**
     * 查询sku汇总分析
     *
     * @param queryDto 查询参数
     * @return 结果
     */
    @Override
    public SkuMonthQtyVo listSkuSummary(MonthPlanReportDto queryDto) {
        List<ProduceSkuSummaryVo> list = getProduceSkuSummaryVoList(queryDto);
        return calculateSkuSummaryListGenSkuMonthQtyVo(queryDto, list, Boolean.TRUE);
    }

    /**
     * 查询sku汇总分析
     *
     * @param queryDto 参数
     * @return 结果
     */
    private List<ProduceSkuSummaryVo> getProduceSkuSummaryVoList(MonthPlanReportDto queryDto) {
        // 查询数据
        List<AccountingPeriodVo> stopDayList = monthPlanReportMapper.selectStopDay(queryDto);
        Map<Integer, Integer> stopDayMap = new HashMap<>(16);
        if (CollectionUtils.isNotEmpty(stopDayList)) {
            stopDayMap = stopDayList.stream().collect(Collectors.toMap(AccountingPeriodVo::getMonth, AccountingPeriodVo::getStopDay, (s1, s2) -> s1, LinkedHashMap::new));
        }
        List<AccountingPeriodVo> produceDateCountList = monthPlanReportMapper.selectProduceQtyAndCount(queryDto);
        Map<Integer, AccountingPeriodVo> produceDateCountMap = new HashMap<>(16);
        if (CollectionUtils.isNotEmpty(produceDateCountList)) {
            produceDateCountMap = produceDateCountList.stream().collect(Collectors.toMap(AccountingPeriodVo::getMonth, Function.identity(), (s1, s2) -> s1, LinkedHashMap::new));
        }
        List<AccountingPeriodVo> finishDateCountList = monthPlanReportMapper.selectFinishQtyAndCount(queryDto);
        Map<Integer, AccountingPeriodVo> finishDateCountMap = new HashMap<>(16);
        if (CollectionUtils.isNotEmpty(finishDateCountList)) {
            finishDateCountMap = finishDateCountList.stream().collect(Collectors.toMap(AccountingPeriodVo::getMonth, Function.identity(), (s1, s2) -> s1, LinkedHashMap::new));
        }
        List<ProduceSkuSummaryVo> list = new ArrayList<>();
        for (int i = 1; i <= MONTH_NUM; i++) {
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.YEAR, queryDto.getYear());
            ProduceSkuSummaryVo produceSkuSummaryVo = new ProduceSkuSummaryVo();
            produceSkuSummaryVo.setMonth(i);
            // 赋值计划天数
            calendar.set(Calendar.MONTH, i - 1);
            calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
            int monthLastDay = calendar.get(Calendar.DAY_OF_MONTH);
            int planDay = monthLastDay - stopDayMap.getOrDefault(i, 0);
            produceSkuSummaryVo.setPlanDay(BigDecimal.valueOf(planDay));
            // 赋值计划总数
            AccountingPeriodVo produceDateCount = produceDateCountMap.getOrDefault(i, new AccountingPeriodVo());
            produceSkuSummaryVo.setProduceTotal(ObjectUtils.defaultIfNull(produceDateCount.getProduceTotal(), BigDecimal.ZERO));
            produceSkuSummaryVo.setProduceSkuCount(ObjectUtils.defaultIfNull(produceDateCount.getProduceSkuCount(), BigDecimal.ZERO));
            // 赋值完成总数
            AccountingPeriodVo finishDateCount = finishDateCountMap.getOrDefault(i, new AccountingPeriodVo());
            produceSkuSummaryVo.setFinishTotal(ObjectUtils.defaultIfNull(finishDateCount.getFinishTotal(), BigDecimal.ZERO));
            produceSkuSummaryVo.setFinishSkuCount(ObjectUtils.defaultIfNull(finishDateCount.getFinishSkuCount(), BigDecimal.ZERO));
            produceSkuSummaryVo.setActualDay(finishDateCount.getFinishDateCount() == null ? BigDecimal.ZERO : BigDecimal.valueOf(finishDateCount.getFinishDateCount()));
            list.add(produceSkuSummaryVo);
        }
        return list;
    }

    /**
     * 导出sku汇总分析
     *
     * @param queryDto 查询参数
     * @return 结果
     */
    @Override
    public byte[] exportSkuSummary(MonthPlanReportDto queryDto) {
        // 获取模板
        ClassLoader classLoader = this.getClass().getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream("excelModel/skuSummary.xlsx");
        // 表头信息
        Map<String, Object> tableMap = new HashMap<>(16);
        // 列表数据
        List<List<Map<String, Object>>> excelDataList = new ArrayList<>();
        // 查询数据
        List<ProduceSkuSummaryVo> skuSummaryVoList = this.getProduceSkuSummaryVoList(queryDto);
        if (CollectionUtils.isNotEmpty(skuSummaryVoList)) {
            skuSummaryVoList.sort(Comparator.comparing(ProduceSkuSummaryVo::getMonth));
            List<Map<String, Object>> list = new ArrayList<>();
            for (int i = 0; i < skuSummaryVoList.size(); i++) {
                Map<String, Object> listDataMap = new HashMap<>(16);
                String suffix = ExcelUtils.convertIndexToLetter(i);
                ProduceSkuSummaryVo produceSkuSummaryVo = skuSummaryVoList.get(i);
                putFieldToMap(produceSkuSummaryVo, tableMap,
                        Arrays.asList("month", "avgDailyProduce", "produceAvgSkuCount", "avgDailyFinish", "finishAvgSkuCount"),
                        suffix, Boolean.FALSE);
                list.add(listDataMap);
                excelDataList.add(list);
            }
        }
        SkuMonthQtyVo skuMonthQtyVo = calculateSkuSummaryListGenSkuMonthQtyVo(queryDto, skuSummaryVoList, false);
        ProduceSkuSummaryVo currentMonthAvgDiff = skuMonthQtyVo.getCurrentMonthAvgDiff();
        putFieldToMap(currentMonthAvgDiff, tableMap, Collections.emptyList(), "Diff", Boolean.FALSE);
        // 写到文件
        return ExcelUtils.writeMultiList(inputStream
                , 0, tableMap, excelDataList);
    }

    /**
     * 计算sku汇总平均日产、平均sku数量等数据，并汇总计算年累计、月平均、月对比数据
     *
     * @param skuSummaryVoList 要计算的数据
     */
    private static SkuMonthQtyVo calculateSkuSummaryListGenSkuMonthQtyVo(MonthPlanReportDto queryDto, List<ProduceSkuSummaryVo> skuSummaryVoList, Boolean isCalculateMonthAvg) {
        SkuMonthQtyVo skuMonthQtyVo = new SkuMonthQtyVo();
        ProduceSkuSummaryVo yearSum = new ProduceSkuSummaryVo();
        for (ProduceSkuSummaryVo produceSkuSummaryVo : skuSummaryVoList) {
            // 计算单个sku的平均日产、平均sku个数
            calculateOneSkuSummary(produceSkuSummaryVo);
            Integer month = produceSkuSummaryVo.getMonth();
            ReflectUtils.setFieldValue(skuMonthQtyVo, "month" + month, produceSkuSummaryVo);
            // 汇总年数据
            sumAllField(produceSkuSummaryVo, yearSum, Arrays.asList("serialVersionUID", "month"));
        }
        skuMonthQtyVo.setYearSum(yearSum);
        // 计算月平均数据
        ProduceSkuSummaryVo monthAvg = new ProduceSkuSummaryVo();
        monthAvgAllField(yearSum, monthAvg, skuSummaryVoList, Arrays.asList("serialVersionUID", "month"));
        skuMonthQtyVo.setMonthAvg(monthAvg);
        // 计算月对比数据
        ProduceSkuSummaryVo currentMonthAvgDiff = new ProduceSkuSummaryVo();
        Integer month = queryDto.getMonth();
        ProduceSkuSummaryVo monthSkuSummaryVo = ReflectUtils.getFieldValue(skuMonthQtyVo, "month" + month);
        monthAvgDiffAllField(monthAvg, monthSkuSummaryVo, currentMonthAvgDiff, Arrays.asList("serialVersionUID", "month"));
        skuMonthQtyVo.setCurrentMonthAvgDiff(currentMonthAvgDiff);
        return skuMonthQtyVo;
    }

    /**
     * 计算单个sku汇总平均日产、平均sku数量等数据
     *
     * @param produceSkuSummaryVo 要计算的数据
     */
    private static void calculateOneSkuSummary(ProduceSkuSummaryVo produceSkuSummaryVo) {
        BigDecimal produceTotal = produceSkuSummaryVo.getProduceTotal();
        BigDecimal planDay = ObjectUtils.defaultIfNull(produceSkuSummaryVo.getPlanDay(), BigDecimal.ZERO);
        BigDecimal avgDailyProduce = null;
        if (produceTotal != null && BigDecimal.ZERO.compareTo(planDay) != 0) {
            avgDailyProduce = BigDecimalUtils.div(produceTotal, planDay, 0, true, BigDecimal.ROUND_HALF_UP);
        }
        BigDecimal produceSkuCount = produceSkuSummaryVo.getProduceSkuCount();
        BigDecimal produceAvgSkuCount = null;
        if (produceTotal != null && BigDecimal.ZERO.compareTo(produceSkuCount) != 0) {
            produceAvgSkuCount = BigDecimalUtils.div(produceTotal, produceSkuCount, 0, true, BigDecimal.ROUND_HALF_UP);
        }
        produceSkuSummaryVo.setAvgDailyProduce(avgDailyProduce);
        produceSkuSummaryVo.setProduceAvgSkuCount(produceAvgSkuCount);

        BigDecimal finishTotal = produceSkuSummaryVo.getFinishTotal();
        BigDecimal actualDay = ObjectUtils.defaultIfNull(produceSkuSummaryVo.getActualDay(), BigDecimal.ZERO);
        BigDecimal avgDailyFinish = null;
        if (finishTotal != null && BigDecimal.ZERO.compareTo(actualDay) != 0) {
            avgDailyFinish = BigDecimalUtils.div(finishTotal, actualDay, 0, true, BigDecimal.ROUND_HALF_UP);
        }
        BigDecimal finishSkuCount = produceSkuSummaryVo.getFinishSkuCount();
        BigDecimal finishAvgSkuCount = null;
        if (finishTotal != null && BigDecimal.ZERO.compareTo(finishSkuCount) != 0) {
            finishAvgSkuCount = BigDecimalUtils.div(finishTotal, finishSkuCount, 0, true, BigDecimal.ROUND_HALF_UP);
        }
        produceSkuSummaryVo.setAvgDailyFinish(avgDailyFinish);
        produceSkuSummaryVo.setFinishAvgSkuCount(finishAvgSkuCount);
    }

    /**
     * 胎类区分-排产受限满足率，白胎侧规格后缀
     */
    private static final String WHITE_SIDEWALL_SUFFIX = "(W)";

    /**
     * 将数据字段都除以月份数
     *
     * @param obj              取值的对象
     * @param resultObj        计算的对象
     * @param skuSummaryVoList 要统计的数
     * @param ignoreFieldNames 要忽略的属性名
     */
    private static void monthAvgAllField(Object obj, Object resultObj, List<?> skuSummaryVoList, List<String> ignoreFieldNames) {
        Field[] fields = obj.getClass().getDeclaredFields();
        List<Field> fieldList = Arrays.asList(fields);
        if (CollectionUtils.isNotEmpty(ignoreFieldNames)) {
            fieldList = Arrays.stream(fields).filter(item -> !ignoreFieldNames.contains(item.getName())).collect(Collectors.toList());
        }
        for (Field field : fieldList) {
            field.setAccessible(true);
            String fieldName = field.getName();
            try {
                int monthNum = 0;
                for (Object object : skuSummaryVoList) {
                    BigDecimal fieldValue = ObjectUtils.defaultIfNull(ReflectUtils.getFieldValue(object, fieldName), BigDecimal.ZERO);
                    if (fieldValue.compareTo(BigDecimal.ZERO) > 0) {
                        monthNum++;
                    }
                }
                Object value = ObjectUtils.defaultIfNull(field.get(obj), BigDecimal.ZERO);
                BigDecimal result = BigDecimalUtils.div(value, BigDecimal.valueOf(monthNum), 0, Boolean.TRUE, BigDecimal.ROUND_HALF_UP);
                ReflectUtils.setFieldValue(resultObj, fieldName, result);
            } catch (IllegalAccessException e) {
                log.error(e.getMessage());
            }
        }
    }

    /**
     * 取汇总对象的字段除以月份对象的字段 -1，赋值给结果对象字段
     *
     * @param sumObj           汇总的对象
     * @param monthObj         月份的对象
     * @param resultObj        结果对象
     * @param ignoreFieldNames 要忽略的属性名
     */
    private static void monthAvgDiffAllField(Object sumObj, Object monthObj, Object resultObj, List<String> ignoreFieldNames) {
        Field[] fields = sumObj.getClass().getDeclaredFields();
        List<Field> fieldList = Arrays.asList(fields);
        if (CollectionUtils.isNotEmpty(ignoreFieldNames)) {
            fieldList = Arrays.stream(fields).filter(item -> !ignoreFieldNames.contains(item.getName())).collect(Collectors.toList());
        }
        for (Field field : fieldList) {
            field.setAccessible(true);
            String fieldName = field.getName();
            try {
                BigDecimal value = ObjectUtils.defaultIfNull((BigDecimal) field.get(sumObj), BigDecimal.ZERO);
                BigDecimal monthValue = ObjectUtils.defaultIfNull(ReflectUtils.getFieldValue(monthObj, fieldName), null);
                BigDecimal result = null;
                if (monthValue != null && BigDecimal.ZERO.compareTo(value) != 0) {
                    result = BigDecimalUtils.div(monthValue, value, 2);
                }
                if (result != null) {
                    result = result.subtract(BigDecimal.ONE);
                }
                ReflectUtils.setFieldValue(resultObj, fieldName, result);
            } catch (IllegalAccessException e) {
                log.error(e.getMessage());
            }
        }
    }

    /**
     * 查询投产sku汇总分析
     *
     * @param queryDto 查询参数
     * @return 结果
     */
    @Override
    public List<SkuSummaryProduceVo> listSkuSummaryProduce(MonthPlanReportDto queryDto) {
        List<SkuSummaryProduceVo> skuSummaryProduceVoList = monthPlanReportMapper.selectSkuSummaryProduce(queryDto);
        // 添加年累计、H1、环比
        calculateSkuSummaryProduceList(queryDto, skuSummaryProduceVoList, Boolean.TRUE);
        return skuSummaryProduceVoList;
    }

    /**
     * 导出投产sku汇总分析
     *
     * @param queryDto 查询参数
     * @return 结果
     */
    @Override
    public byte[] exportSkuSummaryProduce(MonthPlanReportDto queryDto) {
        // 获取模板
        ClassLoader classLoader = this.getClass().getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream("excelModel/skuSummaryProduce.xlsx");
        // 表头信息
        Map<String, Object> tableMap = new HashMap<>(16);
        // 列表数据
        List<List<Map<String, Object>>> excelDataList = new ArrayList<>();
        // 查询数据
        List<SkuSummaryProduceVo> skuSummaryProduceVoList = this.listSkuSummaryProduce(queryDto);
        if (CollectionUtils.isNotEmpty(skuSummaryProduceVoList)) {
            skuSummaryProduceVoList.sort(Comparator.comparing(SkuSummaryProduceVo::getMonth));
            List<Map<String, Object>> list = new ArrayList<>();
            for (SkuSummaryProduceVo summaryProduceVo : skuSummaryProduceVoList) {
                Map<String, Object> listDataMap = new HashMap<>(16);
                SkuSummaryProduceVo skuSummaryProduceVo = summaryProduceVo;
                Integer month = skuSummaryProduceVo.getMonth();
                String suffix = ExcelUtils.convertIndexToLetter(month - 1);
                if (month == 13 || month == 14) {
                    // 年累计、月对比不添加，Excel公式进行计算
                    continue;
                }
                if (month == 15) {
                    suffix = "Diff";
                }
                putFieldToMap(skuSummaryProduceVo, tableMap,
                        Arrays.asList("serialVersionUID", "month"),
                        suffix, Boolean.FALSE);
                list.add(listDataMap);
                excelDataList.add(list);
            }
        }
        // 写到文件
        return ExcelUtils.writeMultiList(inputStream
                , 0, tableMap, excelDataList);
    }

    /**
     * 计算投产sku汇总平均日产、平均sku数量等数据，并汇总计算年累计、月平均、月对比数据
     *
     * @param skuSummaryProduceVoList 要计算的数据
     */
    private static void calculateSkuSummaryProduceList(MonthPlanReportDto queryDto, List<SkuSummaryProduceVo> skuSummaryProduceVoList, Boolean isCalculateMonthAvg) {
        if (CollectionUtils.isEmpty(skuSummaryProduceVoList)) {
            return;
        }
        Map<Integer, SkuSummaryProduceVo> monthGroupMap = new HashMap<>(16);
        SkuSummaryProduceVo yearSum = new SkuSummaryProduceVo();
        yearSum.setMonth(13);
        for (SkuSummaryProduceVo skuSummaryProduceVo : skuSummaryProduceVoList) {
            // 汇总年数据
            sumAllField(skuSummaryProduceVo, yearSum, Arrays.asList("serialVersionUID", "month"));
            monthGroupMap.put(skuSummaryProduceVo.getMonth(), skuSummaryProduceVo);
        }
        List<SkuSummaryProduceVo> copyList = BeanCopyUtils.copyBeanList(skuSummaryProduceVoList, SkuSummaryProduceVo.class);
        skuSummaryProduceVoList.add(yearSum);

        // 计算月平均数据
        SkuSummaryProduceVo monthAvg = new SkuSummaryProduceVo();
        monthAvg.setMonth(14);
        monthAvgAllField(yearSum, monthAvg, copyList, Arrays.asList("serialVersionUID", "month"));
        skuSummaryProduceVoList.add(monthAvg);

        // 计算月对比数据
        SkuSummaryProduceVo currentMonthAvgDiff = new SkuSummaryProduceVo();
        currentMonthAvgDiff.setMonth(15);
        Integer month = queryDto.getMonth();
        if (monthGroupMap.containsKey(month)) {
            SkuSummaryProduceVo monthSkuSummaryProduceVo = monthGroupMap.get(month);
            monthAvgDiffAllField(monthAvg, monthSkuSummaryProduceVo, currentMonthAvgDiff, Arrays.asList("serialVersionUID", "month"));
        }
        skuSummaryProduceVoList.add(currentMonthAvgDiff);
    }

    /**
     * 导出试制sku汇总分析
     *
     * @param queryDto 查询参数
     * @return 结果
     */
    @Override
    public byte[] exportSkuSummaryTrial(MonthPlanReportDto queryDto) {
        // 获取模板
        ClassLoader classLoader = this.getClass().getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream("excelModel/skuSummaryTrial.xlsx");
        // 表头信息
        Map<String, Object> tableMap = new HashMap<>(16);
        // 列表数据
        List<List<Map<String, Object>>> excelDataList = new ArrayList<>();
        // 查询数据
        List<SkuSummaryTrialVo> skuSummaryTrialVoList = this.listSkuSummaryTrial(queryDto);
        if (CollectionUtils.isNotEmpty(skuSummaryTrialVoList)) {
            skuSummaryTrialVoList.sort(Comparator.comparing(SkuSummaryTrialVo::getMonth));
            List<Map<String, Object>> list = new ArrayList<>();
            for (SkuSummaryTrialVo skuSummaryTrialVo : skuSummaryTrialVoList) {
                int monthIndex = skuSummaryTrialVo.getMonth() - 1;
                if (monthIndex == 12) {
                    // 年累计、月对比不添加，Excel公式进行计算
                    continue;
                }
                Map<String, Object> listDataMap = new HashMap<>(16);
                String suffix = ExcelUtils.convertIndexToLetter(monthIndex);
                if (monthIndex == 14) {
                    suffix = "Avg";
                }
                if (monthIndex == 15) {
                    suffix = "Diff";
                }
                putFieldToMap(skuSummaryTrialVo, tableMap,
                        Arrays.asList("serialVersionUID", "month"),
                        suffix, Boolean.FALSE);
                list.add(listDataMap);
                excelDataList.add(list);
            }
        }
        // 写到文件
        return ExcelUtils.writeMultiList(inputStream
                , 0, tableMap, excelDataList);
    }

    /**
     * 计算投产sku汇总平均日产、平均sku数量等数据，并汇总计算年累计、月平均、月对比数据
     *
     * @param voList 要计算的数据
     */
    private static void calculateSkuSummaryTrialList(MonthPlanReportDto queryDto, List<SkuSummaryTrialVo> voList, Boolean isCalculateMonthAvg) {
        if (CollectionUtils.isEmpty(voList)) {
            return;
        }
        Map<Integer, SkuSummaryTrialVo> monthGroupMap = new HashMap<>(16);
        SkuSummaryTrialVo yearSum = new SkuSummaryTrialVo();
        yearSum.setMonth(13);
        int monthNum = voList.size();
        for (SkuSummaryTrialVo vo : voList) {
            // 汇总年数据
            sumAllField(vo, yearSum, Arrays.asList("serialVersionUID", "month"));
            monthGroupMap.put(vo.getMonth(), vo);
        }
        List<SkuSummaryTrialVo> copyList = BeanCopyUtils.copyBeanList(voList, SkuSummaryTrialVo.class);

        voList.add(yearSum);

        // 计算月平均数据
        SkuSummaryTrialVo monthAvg = new SkuSummaryTrialVo();
        monthAvg.setMonth(14);
        monthAvgAllField(yearSum, monthAvg, copyList, Arrays.asList("serialVersionUID", "month"));
        voList.add(monthAvg);

        // 计算月对比数据
        Integer month = queryDto.getMonth();
        SkuSummaryTrialVo currentMonthAvgDiff = new SkuSummaryTrialVo();
        currentMonthAvgDiff.setMonth(15);
        if (monthGroupMap.containsKey(month)) {
            SkuSummaryTrialVo monthSkuSummaryTrialVo = monthGroupMap.get(month);
            monthAvgDiffAllField(monthAvg, monthSkuSummaryTrialVo, currentMonthAvgDiff, Arrays.asList("serialVersionUID", "month"));
        }
        voList.add(currentMonthAvgDiff);
    }

    /**
     * 赋值对应计划量、完成量，计算完成率等
     *
     * @param brandProSizeSummaryVo   基础对象
     * @param brandProSizeSummaryType 类型
     * @param proPlanMap              生产计划
     * @param proFinishMap            生产完成
     * @param salePlanMap             销售计划
     * @param saleFinishMap           销售完成
     * @param stockMap                库存
     */
    private static void setQtyAndCalculateRate(BrandProSizeSummaryVo brandProSizeSummaryVo, String brandProSizeSummaryType, Map<String, BigDecimal> proPlanMap, Map<String, BigDecimal> proFinishMap, Map<String, BigDecimal> salePlanMap, Map<String, BigDecimal> saleFinishMap, Map<String, BigDecimal> stockMap) {
        if (BrandProSizeSummaryTypeEnum.SUMMARY_TYPE_BRAND.getTypeCode().equals(brandProSizeSummaryType)) {
            String brand = brandProSizeSummaryVo.getBrand();
            BigDecimal proPlan = proPlanMap.getOrDefault(brand, BigDecimal.ZERO);
            brandProSizeSummaryVo.setProPlanQty(proPlan);
            BigDecimal proFinish = proFinishMap.getOrDefault(brand, BigDecimal.ZERO);
            brandProSizeSummaryVo.setProFinishQty(proFinish);
            BigDecimal salePlan = salePlanMap.getOrDefault(brand, BigDecimal.ZERO);
            brandProSizeSummaryVo.setSalePlanQty(salePlan);
            BigDecimal saleFinish = saleFinishMap.getOrDefault(brand, BigDecimal.ZERO);
            brandProSizeSummaryVo.setSaleFinishQty(saleFinish);
            BigDecimal stockQty = stockMap.getOrDefault(brand, BigDecimal.ZERO);
            brandProSizeSummaryVo.setStockQty(stockQty);
        } else if (BrandProSizeSummaryTypeEnum.SUMMARY_TYPE_PRO_SIZE.getTypeCode().equals(brandProSizeSummaryType)) {
            String proSize = brandProSizeSummaryVo.getProSize();
            BigDecimal proPlan = proPlanMap.getOrDefault(proSize, BigDecimal.ZERO);
            brandProSizeSummaryVo.setProPlanQty(proPlan);
            BigDecimal proFinish = proFinishMap.getOrDefault(proSize, BigDecimal.ZERO);
            brandProSizeSummaryVo.setProFinishQty(proFinish);
            BigDecimal salePlan = salePlanMap.getOrDefault(proSize, BigDecimal.ZERO);
            brandProSizeSummaryVo.setSalePlanQty(salePlan);
            BigDecimal saleFinish = saleFinishMap.getOrDefault(proSize, BigDecimal.ZERO);
            brandProSizeSummaryVo.setSaleFinishQty(saleFinish);
            BigDecimal stockQty = stockMap.getOrDefault(proSize, BigDecimal.ZERO);
            brandProSizeSummaryVo.setStockQty(stockQty);
        }
        brandProSizeSummaryVo.setProFinishRate(BigDecimalUtils.div(brandProSizeSummaryVo.getProFinishQty(), brandProSizeSummaryVo.getProPlanQty(), 2, true, BigDecimal.ROUND_HALF_UP));
        brandProSizeSummaryVo.setSaleFinishRate(BigDecimalUtils.div(brandProSizeSummaryVo.getSaleFinishQty(), brandProSizeSummaryVo.getSalePlanQty(), 2, true, BigDecimal.ROUND_HALF_UP));
        brandProSizeSummaryVo.setNextMonthStock(BigDecimalUtils.sub(BigDecimalUtils.add(brandProSizeSummaryVo.getStockQty(), brandProSizeSummaryVo.getProFinishQty()), brandProSizeSummaryVo.getSaleFinishQty()));
    }

    /**
     * 查询T月完成率-品牌寸别列表数据
     *
     * @param queryDto 查询参数
     * @return 结果
     */
    @Override
    public List<MonthFinishRateProSizeVo> listMonthFinishRateBrandProSize(MonthPlanReportDto queryDto) {
        // 查询终稿的版本，用于查询关联版本、过滤数据
        String monthPlanVersion = monthPlanReportMapper.selectFinalMonthPlanVersion(queryDto);
        queryDto.setMonthPlanVersion(monthPlanVersion);
        List<MonthFinishRateProSizeVo> brandProSizeVoList = monthPlanReportMapper.selectMonthPlanFinishBrandSize(queryDto);
        Map<String, String> brandDictMap = getDictMapByType(BRAND_DICT_TYPE);
        // 调整通知单数量
//        List<MonthPlanNoticeOrder> monthPlanNoticeOrderList = monthPlanReportMapper.selectMonthPlanAdjustNotice(queryDto);
//        Map<String, List<MonthPlanNoticeOrder>> adjustNoticeMap = new HashMap<>(16);
//        if (CollectionUtils.isNotEmpty(monthPlanNoticeOrderList)) {
//            adjustNoticeMap = monthPlanNoticeOrderList.stream().filter(item -> item.getNeedQty() != null && item.getNeedQty() > 0)
//                    .collect(Collectors.groupingBy(item -> GenerageMapKeyUtils.createMapKey(item.getBrand(), item.getProSize())));
//        }
//        for (MonthFinishRateProSizeVo monthFinishRateBrandVo : brandProSizeVoList) {
//            String brand = monthFinishRateBrandVo.getBrand();
//            String proSize = monthFinishRateBrandVo.getProSize();
//            String mapKey = GenerageMapKeyUtils.createMapKey(brand, proSize);
//            if (adjustNoticeMap.containsKey(mapKey)) {
//                List<MonthPlanNoticeOrder> noticeList = adjustNoticeMap.get(mapKey);
//                int sumAdjustQty = 0;
//                for (MonthPlanNoticeOrder planNoticeOrder : noticeList) {
//                    sumAdjustQty += planNoticeOrder.getNeedQty();
//                }
//                // 添加调整通知单计划数
//                monthFinishRateBrandVo.setSalePlanQty(monthFinishRateBrandVo.getSalePlanQty() + sumAdjustQty);
//            }
//            calculateMonthSkuSummaryList(monthFinishRateBrandVo);
//            String dictLabel = brandDictMap.getOrDefault(brand, brand);
//            monthFinishRateBrandVo.setBrandName(dictLabel);
//        }
        return brandProSizeVoList;
    }

    /**
     * 查询品牌-尺寸汇总分析
     *
     * @param queryDto 查询条件
     * @return 结果
     */
    @Override
    public List<BrandProSizeSummaryVo> listBrandProSizeSummary(MonthPlanReportDto queryDto) {
        log.info("查询：品牌-尺寸汇总分析，start");
        long timeMillis = System.currentTimeMillis();
        String monthPlanVersion = monthPlanReportMapper.selectFinalMonthPlanVersion(queryDto);
        queryDto.setMonthPlanVersion(monthPlanVersion);
        long timeMillis1 = System.currentTimeMillis();
        log.info("查询：品牌-尺寸汇总分析，查询版本耗时{}", timeMillis1 - timeMillis);
        List<BrandProSizeSummaryVo> brandProSizeSummaryVoList = monthPlanReportMapper.selectBrandProSizeSummary(queryDto);
        long timeMillis2 = System.currentTimeMillis();
        log.info("查询：品牌-尺寸汇总分析，基础列表耗时{}", timeMillis2 - timeMillis1);
        String brandProSizeSummaryType = queryDto.getBrandProSizeSummaryType();

        List<BrandProSizeSummaryVo> proPlanBrandProSizeSummaryList = monthPlanReportMapper.selectProPlanBrandProSizeSummary(queryDto);
        Map<String, BigDecimal> proPlanMap = this.getSummaryTypeMap(brandProSizeSummaryType, proPlanBrandProSizeSummaryList, BrandProSizeSummaryVo::getProPlanQty);
        Map<String, Long> proPlanCountMap = this.getSummaryTypeMap4Long(brandProSizeSummaryType, proPlanBrandProSizeSummaryList, BrandProSizeSummaryVo::getProPlanCount);
        long timeMillis3 = System.currentTimeMillis();
        log.info("查询：品牌-尺寸汇总分析，生产计划耗时{}", timeMillis3 - timeMillis2);

        List<BrandProSizeSummaryVo> proFinishBrandProSizeSummaryList = monthPlanReportMapper.selectProFinishBrandProSizeSummary(queryDto);
        Map<String, BigDecimal> proFinishMap = this.getSummaryTypeMap(brandProSizeSummaryType, proFinishBrandProSizeSummaryList, BrandProSizeSummaryVo::getProFinishQty);
        Map<String, Long> proFinishCountMap = this.getSummaryTypeMap4Long(brandProSizeSummaryType, proFinishBrandProSizeSummaryList, BrandProSizeSummaryVo::getProFinishCount);
        long timeMillis4 = System.currentTimeMillis();
        log.info("查询：品牌-尺寸汇总分析，生产完成耗时{}", timeMillis4 - timeMillis3);

        List<BrandProSizeSummaryVo> salePlanBrandProSizeSummaryList = monthPlanReportMapper.selectSalePlanBrandProSizeSummary(queryDto);
        Map<String, BigDecimal> salePlanMap = this.getSummaryTypeMap(brandProSizeSummaryType, salePlanBrandProSizeSummaryList, BrandProSizeSummaryVo::getSalePlanQty);
        Map<String, Long> salePlanCountMap = this.getSummaryTypeMap4Long(brandProSizeSummaryType, salePlanBrandProSizeSummaryList, BrandProSizeSummaryVo::getSalePlanCount);
        // 调整通知单数量
//        List<MonthPlanNoticeOrder> monthPlanNoticeOrderList = monthPlanReportMapper.selectMonthPlanAdjustNotice(queryDto);
//        Map<String, List<MonthPlanNoticeOrder>> adjustNoticeMap = new HashMap<>(16);
//        if (CollectionUtils.isNotEmpty(monthPlanNoticeOrderList)) {
//            if (BrandProSizeSummaryTypeEnum.SUMMARY_TYPE_BRAND.getTypeCode().equals(brandProSizeSummaryType)) {
//                adjustNoticeMap = monthPlanNoticeOrderList.stream().filter(item -> item.getNeedQty() != null && item.getNeedQty() > 0)
//                        .collect(Collectors.groupingBy(MonthPlanNoticeOrder::getBrand));
//            } else if (BrandProSizeSummaryTypeEnum.SUMMARY_TYPE_PRO_SIZE.getTypeCode().equals(brandProSizeSummaryType)) {
//                adjustNoticeMap = monthPlanNoticeOrderList.stream().filter(item -> item.getNeedQty() != null && item.getNeedQty() > 0)
//                        .collect(Collectors.groupingBy(item -> item.getProSize().toString()));
//            }
//        }
        long timeMillis5 = System.currentTimeMillis();
        log.info("查询：品牌-尺寸汇总分析，销售计划耗时{}", timeMillis5 - timeMillis4);

        List<BrandProSizeSummaryVo> saleFinishBrandProSizeSummaryList = monthPlanReportMapper.selectSaleFinishBrandProSizeSummary(queryDto);
        Map<String, BigDecimal> saleFinishMap = this.getSummaryTypeMap(brandProSizeSummaryType, saleFinishBrandProSizeSummaryList, BrandProSizeSummaryVo::getSaleFinishQty);
        Map<String, Long> saleFinishCountMap = this.getSummaryTypeMap4Long(brandProSizeSummaryType, saleFinishBrandProSizeSummaryList, BrandProSizeSummaryVo::getSaleFinishCount);
        long timeMillis6 = System.currentTimeMillis();
        log.info("查询：品牌-尺寸汇总分析，销售完成耗时{}", timeMillis6 - timeMillis5);

        List<BrandProSizeSummaryVo> stockBrandProSizeSummaryList = monthPlanReportMapper.selectStockBrandProSizeSummary(queryDto);
        Map<String, BigDecimal> stockMap = this.getSummaryTypeMap(brandProSizeSummaryType, stockBrandProSizeSummaryList, BrandProSizeSummaryVo::getStockQty);
        Map<String, Long> stockCountMap = this.getSummaryTypeMap4Long(brandProSizeSummaryType, stockBrandProSizeSummaryList, BrandProSizeSummaryVo::getStockCount);
        long timeMillis7 = System.currentTimeMillis();
        log.info("查询：品牌-尺寸汇总分析，库存耗时{}", timeMillis7 - timeMillis6);

        Map<String, String> brandDictMap = getDictMapByType(BRAND_DICT_TYPE);
        for (BrandProSizeSummaryVo brandProSizeSummaryVo : brandProSizeSummaryVoList) {
            String brand = brandProSizeSummaryVo.getBrand();
            String dictLabel = brandDictMap.getOrDefault(brand, brand);
            brandProSizeSummaryVo.setBrandName(dictLabel);

            setQtyAndCalculateRate(brandProSizeSummaryVo, brandProSizeSummaryType, proPlanMap, proFinishMap, salePlanMap, saleFinishMap, stockMap);
            setCountAndCalculateRate(brandProSizeSummaryVo, brandProSizeSummaryType, proPlanCountMap, proFinishCountMap, salePlanCountMap, saleFinishCountMap, stockCountMap);
            addAdjustNoticeAndCalculateRate(brandProSizeSummaryVo, brandProSizeSummaryType, null);
        }
        long timeMillis8 = System.currentTimeMillis();
        log.info("查询：品牌-尺寸汇总分析，数据处理耗时{}", timeMillis8 - timeMillis7);
        log.info("查询：品牌-尺寸汇总分析，end");
        return brandProSizeSummaryVoList;
    }

    private void setCountAndCalculateRate(BrandProSizeSummaryVo brandProSizeSummaryVo, String brandProSizeSummaryType, Map<String, Long> proPlanCountMap, Map<String, Long> proFinishCountMap, Map<String, Long> salePlanCountMap, Map<String, Long> saleFinishCountMap, Map<String, Long> stockCountMap) {
        if (BrandProSizeSummaryTypeEnum.SUMMARY_TYPE_BRAND.getTypeCode().equals(brandProSizeSummaryType)) {
            String brand = brandProSizeSummaryVo.getBrand();
            long proPlan = proPlanCountMap.getOrDefault(brand, 0L);
            brandProSizeSummaryVo.setProPlanCount(proPlan);
            long proFinish = proFinishCountMap.getOrDefault(brand, 0L);
            brandProSizeSummaryVo.setProFinishCount(proFinish);
            long salePlan = salePlanCountMap.getOrDefault(brand, 0L);
            brandProSizeSummaryVo.setSalePlanCount(salePlan);
            long saleFinish = saleFinishCountMap.getOrDefault(brand, 0L);
            brandProSizeSummaryVo.setSaleFinishCount(saleFinish);
            long stockQty = stockCountMap.getOrDefault(brand, 0L);
            brandProSizeSummaryVo.setStockCount(stockQty);
        } else if (BrandProSizeSummaryTypeEnum.SUMMARY_TYPE_PRO_SIZE.getTypeCode().equals(brandProSizeSummaryType)) {
            String proSize = brandProSizeSummaryVo.getProSize();
            long proPlan = proPlanCountMap.getOrDefault(proSize, 0L);
            brandProSizeSummaryVo.setProPlanCount(proPlan);
            long proFinish = proFinishCountMap.getOrDefault(proSize, 0L);
            brandProSizeSummaryVo.setProFinishCount(proFinish);
            long salePlan = salePlanCountMap.getOrDefault(proSize, 0L);
            brandProSizeSummaryVo.setSalePlanCount(salePlan);
            long saleFinish = saleFinishCountMap.getOrDefault(proSize, 0L);
            brandProSizeSummaryVo.setSaleFinishCount(saleFinish);
            long stockQty = stockCountMap.getOrDefault(proSize, 0L);
            brandProSizeSummaryVo.setStockCount(stockQty);
        }
        brandProSizeSummaryVo.setProFinishCountRate(BigDecimalUtils.div(brandProSizeSummaryVo.getProFinishCount(), brandProSizeSummaryVo.getProPlanCount(), 2, true, BigDecimal.ROUND_HALF_UP));
        brandProSizeSummaryVo.setSaleFinishCountRate(BigDecimalUtils.div(brandProSizeSummaryVo.getSaleFinishCount(), brandProSizeSummaryVo.getSalePlanCount(), 2, true, BigDecimal.ROUND_HALF_UP));
    }

    /**
     * 分组后统计个数map
     *
     * @param brandProSizeSummaryType 查询类型
     * @return 结果
     */
    private Map<String, Long> getGroupCountMap(String brandProSizeSummaryType, List<BrandProSizeSummaryVo> proPlanBrandProSizeSummaryList) {
        Map<String, Long> proPlanCountMap = new HashMap<>();
        if (BrandProSizeSummaryTypeEnum.SUMMARY_TYPE_BRAND.getTypeCode().equals(brandProSizeSummaryType)) {
            proPlanCountMap = proPlanBrandProSizeSummaryList.stream().collect(Collectors.groupingBy(BrandProSizeSummaryVo::getBrand, Collectors.counting()));
        } else if (BrandProSizeSummaryTypeEnum.SUMMARY_TYPE_PRO_SIZE.getTypeCode().equals(brandProSizeSummaryType)) {
            proPlanCountMap = proPlanBrandProSizeSummaryList.stream().collect(Collectors.groupingBy(BrandProSizeSummaryVo::getProSize, Collectors.counting()));
        }
        return proPlanCountMap;
    }

    /**
     * 根据类型返回对应的map
     *
     * @param brandProSizeSummaryType 查询类型
     * @return 结果
     */
    private Map<String, BigDecimal> getSummaryTypeMap(String brandProSizeSummaryType, List<BrandProSizeSummaryVo> summaryVoList, Function<? super BrandProSizeSummaryVo, ? extends BigDecimal> valueMapper) {
        Map<String, BigDecimal> map = new HashMap<>(16);
        if (CollectionUtils.isNotEmpty(summaryVoList)) {
            if (BrandProSizeSummaryTypeEnum.SUMMARY_TYPE_BRAND.getTypeCode().equals(brandProSizeSummaryType)) {
                map = summaryVoList.stream().collect(Collectors.toMap(BrandProSizeSummaryVo::getBrand, valueMapper, BigDecimal::add));
            } else if (BrandProSizeSummaryTypeEnum.SUMMARY_TYPE_PRO_SIZE.getTypeCode().equals(brandProSizeSummaryType)) {
                map = summaryVoList.stream().collect(Collectors.toMap(BrandProSizeSummaryVo::getProSize, valueMapper, BigDecimal::add));
            }
        }
        return map;
    }

    /**
     * 根据类型返回对应的map
     *
     * @param brandProSizeSummaryType 查询类型
     * @return 结果
     */
    private Map<String, Long> getSummaryTypeMap4Long(String brandProSizeSummaryType, List<BrandProSizeSummaryVo> summaryVoList, Function<? super BrandProSizeSummaryVo, ? extends Long> valueMapper) {
        Map<String, Long> map = new HashMap<>(16);
        if (CollectionUtils.isNotEmpty(summaryVoList)) {
            if (BrandProSizeSummaryTypeEnum.SUMMARY_TYPE_BRAND.getTypeCode().equals(brandProSizeSummaryType)) {
                map = summaryVoList.stream().collect(Collectors.toMap(BrandProSizeSummaryVo::getBrand, valueMapper, Long::sum));
            } else if (BrandProSizeSummaryTypeEnum.SUMMARY_TYPE_PRO_SIZE.getTypeCode().equals(brandProSizeSummaryType)) {
                map = summaryVoList.stream().collect(Collectors.toMap(BrandProSizeSummaryVo::getProSize, valueMapper, Long::sum));
            }
        }
        return map;
    }

    /**
     * 导出品牌-尺寸汇总分析
     *
     * @param queryDto 查询条件
     * @return 结果
     */
    @Override
    public byte[] exportBrandProSizeSummary(MonthPlanReportDto queryDto) {
        String brandProSizeSummaryType = queryDto.getBrandProSizeSummaryType();
        BrandProSizeSummaryTypeEnum brandProSizeSummaryTypeEnum = BrandProSizeSummaryTypeEnum.getNameByCode(brandProSizeSummaryType);
        String brandProSizeSummaryTypeName = StringUtils.EMPTY;
        if (brandProSizeSummaryTypeEnum != null) {
            brandProSizeSummaryTypeName = brandProSizeSummaryTypeEnum.getTypeName();
        }
        // 获取模板
        ClassLoader classLoader = this.getClass().getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream("excelModel/" + brandProSizeSummaryTypeName + "Summary.xlsx");
        // 表头信息
        Map<String, Object> tableMap = new HashMap<>(16);
        // 列表数据
        List<List<Map<String, Object>>> excelDataList = new ArrayList<>();
        // 查询数据
        List<BrandProSizeSummaryVo> brandProSizeSummaryVoList = this.listBrandProSizeSummary(queryDto);
        if (CollectionUtils.isNotEmpty(brandProSizeSummaryVoList)) {
            BrandProSizeSummaryVo sumSummaryVo = new BrandProSizeSummaryVo();
            List<Map<String, Object>> list = new ArrayList<>();
            for (BrandProSizeSummaryVo brandProSizeSummaryVo : brandProSizeSummaryVoList) {
                Map<String, Object> listDataMap = new HashMap<>(16);
                // 添加数据
                putFieldToMap(brandProSizeSummaryVo, listDataMap,
                        Collections.singletonList("serialVersionUID"),
                        StringUtils.EMPTY, Boolean.FALSE);
                // 汇总字段
                sumAllField(brandProSizeSummaryVo, sumSummaryVo, Arrays.asList("serialVersionUID", "brand", "proSize", "brandName"));
                list.add(listDataMap);
                excelDataList.add(list);
            }
            // 合计行
            Map<String, Object> totalDataMap = new HashMap<>(16);
            totalDataMap.put(brandProSizeSummaryTypeName, "总计");
            putFieldToMap(sumSummaryVo, totalDataMap,
                    Arrays.asList("serialVersionUID", "brand", "proSize"),
                    StringUtils.EMPTY, Boolean.FALSE);
            list.add(totalDataMap);
            excelDataList.add(list);
        }
        // 写到文件
        return ExcelUtils.writeMultiList(inputStream
                , 0, tableMap, excelDataList);
    }

    /**
     * 查询试制sku汇总分析
     *
     * @param queryDto 查询参数
     * @return 结果
     */
    @Override
    public List<SkuSummaryTrialVo> listSkuSummaryTrial(MonthPlanReportDto queryDto) {
        List<Integer> list = new ArrayList<>();
        for (int i = 1; i <= MONTH_DAY; i++) {
            list.add(i);
        }
//        List<SkuSummaryTrialVo> skuSummaryProduceVoList = monthPlanReportMapper.selectSkuSummaryTrial(queryDto, list);
        List<SkuSummaryTrialProductVo> skuSummaryProduceVoDetailList = monthPlanReportMapper.selectSkuSummaryTrial4Detail(queryDto, list);
        List<SkuSummaryTrialVo> skuSummaryProduceVoList = handlerProductDetailToResultVo(skuSummaryProduceVoDetailList, queryDto, list);
        // 添加年累计、H1、环比
        calculateSkuSummaryTrialList(queryDto, skuSummaryProduceVoList, Boolean.TRUE);
        return skuSummaryProduceVoList;
    }

    /**
     * 将结果数据转成展示数据
     *
     * @param skuSummaryProduceVoDetailList 详情数据信息
     * @param queryDto                      查询参数
     * @param list                          月份列表
     * @return 结果
     */
    private List<SkuSummaryTrialVo> handlerProductDetailToResultVo(List<SkuSummaryTrialProductVo> skuSummaryProduceVoDetailList, MonthPlanReportDto queryDto, List<Integer> list) {
        if (CollectionUtils.isEmpty(skuSummaryProduceVoDetailList)) {
            return new ArrayList<>();
        }
        List<SkuSummaryTrialVo> resultList = new ArrayList<>();
        Map<Integer, List<SkuSummaryTrialProductVo>> monthGroupMap = skuSummaryProduceVoDetailList.stream().collect(Collectors.groupingBy(SkuSummaryTrialProductVo::getMonth));
        Set<Map.Entry<Integer, List<SkuSummaryTrialProductVo>>> entriedSet = monthGroupMap.entrySet();
        for (Map.Entry<Integer, List<SkuSummaryTrialProductVo>> entry : entriedSet) {
            Integer month = entry.getKey();
            List<SkuSummaryTrialProductVo> value = entry.getValue();
            SkuSummaryTrialVo skuSummaryTrialVo = new SkuSummaryTrialVo();
            skuSummaryTrialVo.setMonth(month);
            BigDecimal totalCount = BigDecimal.ZERO;
            BigDecimal totalSum = BigDecimal.ZERO;
            for (SkuSummaryTrialProductVo skuSummaryTrialProductVo : value) {
                BigDecimal finishDay = skuSummaryTrialProductVo.getFinishDay();
                BigDecimal finishSum = skuSummaryTrialProductVo.getFinishSum();
                Object oldDayCount = ReflectUtils.getFieldValue(skuSummaryTrialVo, "dayCount" + finishDay);
                Object oldDaySum = ReflectUtils.getFieldValue(skuSummaryTrialVo, "daySum" + finishDay);
                ReflectUtils.setFieldValue(skuSummaryTrialVo, "dayCount" + finishDay, BigDecimalUtils.add(oldDayCount, BigDecimal.ONE));
                ReflectUtils.setFieldValue(skuSummaryTrialVo, "daySum" + finishDay, BigDecimalUtils.add(oldDaySum, finishSum));
                totalCount = BigDecimalUtils.add(totalCount, BigDecimal.ONE);
                totalSum = BigDecimalUtils.add(totalSum, finishSum);
            }
            skuSummaryTrialVo.setTotalCount(totalCount);
            skuSummaryTrialVo.setTotalSum(totalSum);
            resultList.add(skuSummaryTrialVo);
        }
        return resultList;
    }

    /**
     * 查询渠道分类缺口差异
     *
     * @param queryDto 查询条件
     * @return 结果
     */
    @Override
    public List<ReportClassificationVo> listChannelClassification(ClassificationReportDto queryDto) {
        return getReportClassificationVoList(queryDto, ClassificationGapVo::getChannel, CHANNEL_DICT_TYPE);
    }

    /**
     * 导出渠道分类缺口差异
     *
     * @param queryDto 查询条件
     * @return 结果
     */
    @Override
    public byte[] exportChannelClassification(ClassificationReportDto queryDto) {
        String classificationName = I18nUtil.getMessage("ui.data.column.channelClassification.title");
        return baseClassificationExport(queryDto, this.listChannelClassification(queryDto), classificationName);
    }

    /**
     * 查询品牌分类缺口差异
     *
     * @param queryDto 查询条件
     * @return 结果
     */
    @Override
    public List<ReportClassificationVo> listBrandClassification(ClassificationReportDto queryDto) {
        return getReportClassificationVoList(queryDto, ClassificationGapVo::getBrand, BRAND_DICT_TYPE);
    }

    /**
     * 导出品牌分类缺口差异
     *
     * @param queryDto 查询条件
     * @return 结果
     */
    @Override
    public byte[] exportBrandClassification(ClassificationReportDto queryDto) {
        String classificationName = I18nUtil.getMessage("ui.data.column.brandClassification.title");
        return baseClassificationExport(queryDto, this.listBrandClassification(queryDto), classificationName);
    }

    /**
     * 查询寸别分类缺口差异
     *
     * @param queryDto 查询条件
     * @return 结果
     */
    @Override
    public List<ReportClassificationVo> listProSizeClassification(ClassificationReportDto queryDto) {
        return getReportClassificationVoList(queryDto, ClassificationGapVo::getProSize, StringUtils.EMPTY);
    }

    /**
     * 导出寸别分类缺口差异
     *
     * @param queryDto 查询条件
     * @return 结果
     */
    @Override
    public byte[] exportProSizeClassification(ClassificationReportDto queryDto) {
        String classificationName = I18nUtil.getMessage("ui.data.column.proSizeClassification.title");
        return baseClassificationExport(queryDto, this.listProSizeClassification(queryDto), classificationName);
    }

    /**
     * 查询品牌库位分类缺口差异
     *
     * @param queryDto 查询条件
     * @return 结果
     */
    @Override
    public List<ReportClassificationVo> listBrandLocationClassification(ClassificationReportDto queryDto) {
        List<ReportClassificationVo> reportClassificationVoList = getReportClassificationVoList(queryDto, item -> String.join("|", item.getBrand(), item.getLocationType()), StringUtils.EMPTY);
        transformTwoDict(reportClassificationVoList, getDictMapByType(BRAND_DICT_TYPE), getDictMapByType(LOCATION_DICT_TYPE));
        return reportClassificationVoList;
    }

    /**
     * 导出品牌库位分类缺口差异
     *
     * @param queryDto 查询条件
     * @return 结果
     */
    @Override
    public byte[] exportBrandLocationClassification(ClassificationReportDto queryDto) {
        String classificationName = I18nUtil.getMessage("ui.data.column.brandLocationClassification.suffix");
        return baseClassificationExport(queryDto, this.listBrandLocationClassification(queryDto), classificationName);
    }

    /**
     * 查询寸别渠道分类缺口差异
     *
     * @param queryDto 查询条件
     * @return 结果
     */
    @Override
    public List<ReportClassificationVo> listProSizeChannelClassification(ClassificationReportDto queryDto) {
        List<ReportClassificationVo> reportClassificationVoList = getReportClassificationVoList(queryDto, item -> String.join("|", item.getProSize(), item.getChannel()), StringUtils.EMPTY);
        transformTwoDict(reportClassificationVoList, Collections.emptyMap(), getDictMapByType(CHANNEL_DICT_TYPE));
        return reportClassificationVoList;
    }

    /**
     * 导出寸别渠道分类缺口差异
     *
     * @param queryDto 查询条件
     * @return 结果
     */
    @Override
    public byte[] exportProSizeChannelClassification(ClassificationReportDto queryDto) {
        String classificationName = I18nUtil.getMessage("ui.data.column.proSizeChannelClassification.suffix");
        return baseClassificationExport(queryDto, this.listProSizeChannelClassification(queryDto), classificationName);
    }

    /**
     * 基础分类差异导出
     *
     * @param queryDto           查询条件
     * @param classificationName 分类名称
     * @return 结果
     */
    private byte[] baseClassificationExport(ClassificationReportDto queryDto, List<ReportClassificationVo> classificationVoList, String classificationName) {
        // 获取模板
        ClassLoader classLoader = this.getClass().getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream("excelModel/classificationReport.xlsx");
        // 表头信息
        Map<String, Object> tableMap = new HashMap<>(16);
        // 列表数据
        List<List<Map<String, Object>>> excelDataList = new ArrayList<>();
        String prefix = StringUtils.EMPTY;
        if (CollectionUtils.isNotEmpty(classificationVoList)) {
            List<Map<String, Object>> list = new ArrayList<>();
            for (ReportClassificationVo brandProSizeSummaryVo : classificationVoList) {
                Map<String, Object> listDataMap = new HashMap<>(16);
                // 添加数据
                putFieldToMap(brandProSizeSummaryVo, listDataMap,
                        Collections.singletonList("serialVersionUID"),
                        StringUtils.EMPTY, Boolean.FALSE);
                list.add(listDataMap);
                excelDataList.add(list);
            }
            prefix = StringUtils.defaultIfBlank(classificationVoList.get(0).getClassificationName1(), "");
        }
        tableMap.put("classificationName", prefix + classificationName);
        tableMap.put("title", queryDto.getYear().toString() + "-" + queryDto.getMonth().toString());
        // 写到文件
        return ExcelUtils.writeMultiList(inputStream
                , 0, tableMap, excelDataList);
    }

    /**
     * 根据字典类型获取字典数据
     *
     * @param dictType 字典类型
     * @return 结果, K-字典值,V-字典名称
     */
    private Map<String, String> getDictMapByType(String dictType) {
        List<SysDictData> dictDataList = sysDictDataService.getType(dictType);
        Map<String, String> dictMap = new HashMap<>(16);
        if (PubUtil.isNotEmpty(dictDataList)) {
            dictMap = dictDataList.stream().collect(Collectors.toMap(SysDictData::getDictValue, SysDictData::getDictLabel, (key1, key2) -> key2, LinkedHashMap::new));
        }
        return dictMap;
    }

    /**
     * 赋值占比
     *
     * @param resultList   结果列表
     * @param sumChannelVo 汇总数据
     * @param dictMap      字典map
     */
    private static void setProportion(List<ReportClassificationVo> resultList, ReportClassificationVo sumChannelVo, Map<String, String> dictMap) {
        for (ReportClassificationVo classificationVo : resultList) {
            BigDecimal totalSalePlanQty = sumChannelVo.getSalePlanQty();
            BigDecimal salePlanQty = classificationVo.getSalePlanQty();
            if (BigDecimal.ZERO.compareTo(totalSalePlanQty) != 0) {
                classificationVo.setSaleProportion(BigDecimalUtils.div(salePlanQty, totalSalePlanQty, 4));
            }

            BigDecimal totalProducePlanQty = sumChannelVo.getProducePlanQty();
            BigDecimal producePlanQty = classificationVo.getProducePlanQty();
            if (BigDecimal.ZERO.compareTo(totalProducePlanQty) != 0) {
                classificationVo.setProduceProportion(BigDecimalUtils.div(producePlanQty, totalProducePlanQty, 4));
            }

            BigDecimal saleSkuCount = classificationVo.getSaleSkuCount();
            BigDecimal produceSkuCount = classificationVo.getProduceSkuCount();
            if (BigDecimal.ZERO.compareTo(saleSkuCount) != 0) {
                classificationVo.setSpecFinishRate(BigDecimalUtils.div(produceSkuCount, saleSkuCount, 4));
            }

            if (BigDecimal.ZERO.compareTo(salePlanQty) != 0) {
                classificationVo.setPlanFinishRate(BigDecimalUtils.div(producePlanQty, salePlanQty, 4));
            }

            String dictValue = classificationVo.getClassificationValue();
            String dictLabel = dictMap.getOrDefault(dictValue, dictValue);
            classificationVo.setClassificationName(dictLabel);
        }
    }

    /**
     * 赋值属性值并添加到结果列表中
     *
     * @param entrySet     分组集合
     * @param sumChannelVo 汇总数据
     * @param resultList   结果列表
     */
    private static void setFieldAddToResultList(Set<Map.Entry<String, List<ClassificationGapVo>>> entrySet, ReportClassificationVo sumChannelVo, List<ReportClassificationVo> resultList) {
        for (Map.Entry<String, List<ClassificationGapVo>> entry : entrySet) {
            String classificationValue = entry.getKey();
            List<ClassificationGapVo> value = entry.getValue();
            ReportClassificationVo classificationVo = new ReportClassificationVo();
            classificationVo.setClassificationValue(classificationValue);
            ClassificationGapVo sumVo = new ClassificationGapVo();
            for (ClassificationGapVo classificationGapVo : value) {
                sumAllField(classificationGapVo, sumVo, Arrays.asList("serialVersionUID", "productCode", "brand", "proSize", "locationType", "channel"));
            }
            BigDecimal saleSkuCount = sumVo.getSaleSkuCount();
            BigDecimal salePlanQty = sumVo.getSalePlanQty();
            classificationVo.setSaleSkuCount(saleSkuCount);
            classificationVo.setSalePlanQty(salePlanQty);
            if (BigDecimal.ZERO.compareTo(saleSkuCount) != 0) {
                classificationVo.setSaleAvgSingleSkuQty(BigDecimalUtils.div(salePlanQty, saleSkuCount, 0));
            }
            BigDecimal produceSkuCount = sumVo.getProduceSkuCount();
            BigDecimal producePlanQty = sumVo.getProducePlanQty();
            classificationVo.setProduceSkuCount(produceSkuCount);
            classificationVo.setProducePlanQty(producePlanQty);
            if (BigDecimal.ZERO.compareTo(produceSkuCount) != 0) {
                classificationVo.setProduceAvgSingleSkuQty(BigDecimalUtils.div(producePlanQty, produceSkuCount, 0));
            }
            classificationVo.setStockQty(sumVo.getStockQty());
            classificationVo.setGapSkuCount(sumVo.getGapSkuCount());
            BigDecimal orderGapQty = sumVo.getOrderGapQty();
            classificationVo.setOrderGapQty(orderGapQty);
            if (BigDecimal.ZERO.compareTo(salePlanQty) != 0) {
                classificationVo.setGapProportion(BigDecimalUtils.div(orderGapQty, salePlanQty, 2));
            }
            sumAllField(classificationVo, sumChannelVo, Arrays.asList("serialVersionUID", "classificationName", "classificationName1", "classificationValue"));
            resultList.add(classificationVo);
        }
    }

    /**
     * 添加调整通知单计划量并重算完成率
     *
     * @param brandProSizeSummaryVo 要添加的对象
     * @param adjustNoticeMap       调整通知单计划 map
     */
    private void addAdjustNoticeAndCalculateRate(BrandProSizeSummaryVo brandProSizeSummaryVo, String brandProSizeSummaryType, Map<String, List<Object>> adjustNoticeMap) {
        long adjustSum = 0L;
        int adjustCount = 0;
//        if (BrandProSizeSummaryTypeEnum.SUMMARY_TYPE_BRAND.getTypeCode().equals(brandProSizeSummaryType)) {
//            String brand = brandProSizeSummaryVo.getBrand();
//            List<MonthPlanNoticeOrder> adjustNoticeList = adjustNoticeMap.getOrDefault(brand, Collections.emptyList());
//            adjustSum = adjustNoticeList.stream().filter(item -> item.getNeedQty() != null && item.getNeedQty() > 0)
//                    .mapToLong(MonthPlanNoticeOrder::getNeedQty).sum();
//            adjustCount = adjustNoticeList.size();
//        } else if (BrandProSizeSummaryTypeEnum.SUMMARY_TYPE_PRO_SIZE.getTypeCode().equals(brandProSizeSummaryType)) {
//            String proSize = brandProSizeSummaryVo.getProSize();
//            List<MonthPlanNoticeOrder> adjustNoticeList = adjustNoticeMap.getOrDefault(proSize, Collections.emptyList());
//            adjustSum = adjustNoticeList.stream().filter(item -> item.getNeedQty() != null && item.getNeedQty() > 0)
//                    .mapToLong(MonthPlanNoticeOrder::getNeedQty).sum();
//            adjustCount = adjustNoticeList.size();
//        }
        brandProSizeSummaryVo.setSalePlanQty(BigDecimalUtils.add(brandProSizeSummaryVo.getSalePlanQty(), adjustSum));
        brandProSizeSummaryVo.setSalePlanCount(brandProSizeSummaryVo.getSalePlanCount() + adjustCount);
        brandProSizeSummaryVo.setProFinishRate(BigDecimalUtils.div(brandProSizeSummaryVo.getProFinishQty(), brandProSizeSummaryVo.getProPlanQty(), 2, true, BigDecimal.ROUND_HALF_UP));
        brandProSizeSummaryVo.setProFinishCountRate(BigDecimalUtils.div(brandProSizeSummaryVo.getProFinishCount(), brandProSizeSummaryVo.getProPlanCount(), 2, true, BigDecimal.ROUND_HALF_UP));
        brandProSizeSummaryVo.setNextMonthStock(BigDecimalUtils.sub(BigDecimalUtils.add(brandProSizeSummaryVo.getStockQty(), brandProSizeSummaryVo.getProFinishQty()), brandProSizeSummaryVo.getSaleFinishQty()));
    }

    private static Map<String, ClassificationGapVo> getClassificationMap(Function<ClassificationGapVo, String> keyExtractor, List<ClassificationGapVo> classificationGapVos) {
        Map<String, ClassificationGapVo> map = new HashMap<>(16);
        if (CollectionUtils.isNotEmpty(classificationGapVos)) {
            map = classificationGapVos.stream().collect(Collectors.toMap(keyExtractor,
                    Function.identity(), (s1, s2) -> s1));
        }
        return map;
    }

    /**
     * 获取分类差异列表
     *
     * @param queryDto     查询条件
     * @param keyExtractor 分组字段
     * @return 结果
     */
    private List<ReportClassificationVo> getReportClassificationVoList(ClassificationReportDto queryDto, Function<ClassificationGapVo, String> keyExtractor, String keyDictType) {
        log.info("获取分类差异列表start");
        long timeMillis = System.currentTimeMillis();
        String monthPlanVersion = monthPlanReportMapper.selectFinalMonthPlanVersion(queryDto);
        queryDto.setMonthPlanVersion(monthPlanVersion);
        List<ClassificationGapVo> classificationGapVos = monthPlanReportMapper.selectBaseClassificationList(queryDto);
        long timeMillis1 = System.currentTimeMillis();
        log.info("获取分类差异列表，获取基础数据列表耗时{}", timeMillis1 - timeMillis);
        if (PubUtil.isEmpty(classificationGapVos)) {
            return Collections.emptyList();
        }

        Function<ClassificationGapVo, String> mapExtractor = item -> String.join("|", item.getProductCode(),
                item.getBrand(), item.getProSize(), item.getLocationType(), item.getChannel());

        List<ClassificationGapVo> proPlanClassificationGapVos = monthPlanReportMapper.selectProPlanClassification(queryDto);
        Map<String, ClassificationGapVo> proPlanMap = getClassificationMap(mapExtractor, proPlanClassificationGapVos);
        long timeMillis2 = System.currentTimeMillis();
        log.info("获取分类差异列表，获取生产计划map耗时{}", timeMillis2 - timeMillis1);

        List<ClassificationGapVo> salePlanClassificationGapVos = monthPlanReportMapper.selectSalePlanClassification(queryDto);
        Map<String, ClassificationGapVo> salePlanMap = getClassificationMap(mapExtractor, salePlanClassificationGapVos);
        long timeMillis3 = System.currentTimeMillis();
        log.info("获取分类差异列表，获取销售计划map耗时{}", timeMillis3 - timeMillis2);

        List<ClassificationGapVo> stockPlanClassificationGapVos = monthPlanReportMapper.selectStockClassification(queryDto);
        Map<String, ClassificationGapVo> stockPlanMap = getClassificationMap(mapExtractor, stockPlanClassificationGapVos);
        long timeMillis4 = System.currentTimeMillis();
        log.info("获取分类差异列表，获取库存map耗时{}", timeMillis4 - timeMillis3);

        // 调整通知单数量
//        List<MonthPlanNoticeOrder> monthPlanNoticeOrderList = monthPlanReportMapper.selectMonthPlanAdjustNotice(queryDto);
//        Map<String, List<MonthPlanNoticeOrder>> adjustNoticeListMap = new HashMap<>(16);
//        Map<String, ClassificationGapVo> adjustNoticeMap = new HashMap<>(16);
//        if (CollectionUtils.isNotEmpty(monthPlanNoticeOrderList)) {
//            adjustNoticeListMap = monthPlanNoticeOrderList.stream().filter(item -> item.getNeedQty() != null && item.getNeedQty() > 0)
//                    .collect(Collectors.groupingBy(item -> String.join("|", item.getProductCode(),
//                            item.getBrand(), item.getProSize().toString(), item.getLocationType(), item.getChannel())));
//            for (Map.Entry<String, List<MonthPlanNoticeOrder>> entry : adjustNoticeListMap.entrySet()) {
//                String key = entry.getKey();
//                List<MonthPlanNoticeOrder> value = entry.getValue();
//                long skuCount = value.stream().map(MonthPlanNoticeOrder::getProductCode).distinct().count();
//                long salePlan = 0L;
//                for (MonthPlanNoticeOrder monthPlanNoticeOrder : value) {
//                    Long needQty = monthPlanNoticeOrder.getNeedQty();
//                    salePlan += needQty;
//                }
//                ClassificationGapVo classificationGapVo;
//                if (adjustNoticeMap.containsKey(key)) {
//                    classificationGapVo = adjustNoticeMap.get(key);
//                    BigDecimal salePlanQty = classificationGapVo.getSalePlanQty();
//                    BigDecimal saleSkuCount = classificationGapVo.getSaleSkuCount();
//                    BigDecimal salePlanQtyResult = BigDecimalUtils.add(salePlanQty, salePlan);
//                    BigDecimal saleSkuCountResult = BigDecimalUtils.add(saleSkuCount, skuCount);
//                    classificationGapVo.setSalePlanQty(salePlanQtyResult);
//                    classificationGapVo.setSaleSkuCount(saleSkuCountResult);
//                } else {
//                    classificationGapVo = new ClassificationGapVo();
//                    classificationGapVo.setSalePlanQty(BigDecimal.valueOf(salePlan));
//                    classificationGapVo.setSaleSkuCount(BigDecimal.valueOf(skuCount));
//                }
//                adjustNoticeMap.put(key, classificationGapVo);
//            }
//        }
        setFieldValueByMap(classificationGapVos, salePlanMap, proPlanMap, stockPlanMap, null);

        long timeMillis5 = System.currentTimeMillis();
        log.info("获取分类差异列表，根据map赋值属性耗时{}", timeMillis5 - timeMillis4);

        List<ReportClassificationVo> resultList = new ArrayList<>();
        Map<String, List<ClassificationGapVo>> channelGroupMap = classificationGapVos.stream()
                .filter(item -> StringUtils.isNotBlank(keyExtractor.apply(item)))
                .collect(Collectors.groupingBy(keyExtractor, LinkedHashMap::new, Collectors.toList()));

        Set<Map.Entry<String, List<ClassificationGapVo>>> entrySet = channelGroupMap.entrySet();
        ReportClassificationVo sumChannelVo = new ReportClassificationVo();

        setFieldAddToResultList(entrySet, sumChannelVo, resultList);
        long timeMillis6 = System.currentTimeMillis();
        log.info("获取分类差异列表，分组后赋值添加结果耗时{}", timeMillis6 - timeMillis5);

        List<SysDictData> dictDataList = new ArrayList<>();
        if (StringUtils.isNotBlank(keyDictType)) {
            dictDataList = sysDictDataService.getType(keyDictType);
        }

        Map<String, String> dictMap = new HashMap<>(16);

        if (PubUtil.isNotEmpty(dictDataList)) {
            dictMap = dictDataList.stream().collect(Collectors.toMap(SysDictData::getDictValue, SysDictData::getDictLabel, (s1, s2) -> s1, LinkedHashMap::new));
        }

        setProportion(resultList, sumChannelVo, dictMap);
        long timeMillis7 = System.currentTimeMillis();
        log.info("获取分类差异列表，赋值占比耗时{}", timeMillis7 - timeMillis6);

        resultList = resultList.stream().sorted(Comparator.comparing(ReportClassificationVo::getClassificationValue)).collect(Collectors.toList());

        sumChannelVo.setClassificationName("合计");
        BigDecimal salePlanQty = sumChannelVo.getSalePlanQty();
        BigDecimal orderGapQty = sumChannelVo.getOrderGapQty();
        if (BigDecimal.ZERO.compareTo(salePlanQty) != 0) {
            sumChannelVo.setGapProportion(BigDecimalUtils.div(orderGapQty, salePlanQty, 2));
        }
        sumChannelVo.setSaleProportion(BigDecimal.ONE);
        sumChannelVo.setProduceProportion(BigDecimal.ONE);
        resultList.add(sumChannelVo);
        long timeMillis8 = System.currentTimeMillis();
        log.info("获取分类差异列表，排序添加合计耗时{}", timeMillis8 - timeMillis7);
        log.info("获取分类差异列表end");
        return resultList;
    }

    /**
     * 胎类区分-排产受限满足率，静音棉后缀
     */
    private static final String SILENT_COTTON_SUFFIX = "(静音棉)";

    /**
     * 将数据字段放入map中
     *
     * @param obj              要赋值的对象
     * @param map              要存入的map
     * @param ignoreFieldNames 忽略的属性名
     * @param fileNameSuffix   后缀
     * @param ignoreZero       是否忽略为0的值
     */
    private static void putFieldToMap(Object obj, Map<String, Object> map, List<String> ignoreFieldNames, String fileNameSuffix, Boolean ignoreZero) {
        // 获取对象的所有属性名，并且将属性名放入map中，忽略某些属性
        Field[] fields = obj.getClass().getDeclaredFields();
        List<Field> fieldList = Arrays.asList(fields);
        if (CollectionUtils.isNotEmpty(ignoreFieldNames)) {
            fieldList = Arrays.stream(fields).filter(item -> !ignoreFieldNames.contains(item.getName())).collect(Collectors.toList());
        }
        for (Field field : fieldList) {
            field.setAccessible(true);
            String fieldName = field.getName();
            try {
                Object value = field.get(obj);
                if (value != null) {
                    if (ignoreZero) {
                        try {
                            if (Integer.parseInt(value.toString()) == 0) {
                                continue;
                            }
                        } catch (NumberFormatException e) {
                            log.debug("{},转换整数0失败，跳过", value);
                        }
                    }
                    // 如果是数值类型，转成数值写到Excel
                    try {
                        value = new BigDecimal(value.toString());
                    } catch (NumberFormatException e) {
                        log.debug("{},转换数值失败，跳过", value);
                    }
                }
                map.put(fieldName + fileNameSuffix, value);
            } catch (IllegalAccessException e) {
                log.error(e.getMessage());
            }
        }
    }

    /**
     * 将数据字段都累加到目标对象
     *
     * @param obj              取值的对象
     * @param resultObj        要累加的对象
     * @param ignoreFieldNames 要忽略的属性名
     */
    private static void sumAllField(Object obj, Object resultObj, List<String> ignoreFieldNames) {
        sumAllField(obj, resultObj, ignoreFieldNames, false);
    }

    /**
     * 将数据字段都累加到目标对象
     *
     * @param obj              取值的对象
     * @param resultObj        要累加的对象
     * @param ignoreFieldNames 要忽略的属性名
     */
    private static void sumAllField(Object obj, Object resultObj, List<String> ignoreFieldNames, Boolean isSumSuperClass) {
        Field[] fields = obj.getClass().getDeclaredFields();
        List<Field> fieldList = Arrays.asList(fields);
        if (CollectionUtils.isNotEmpty(ignoreFieldNames)) {
            fieldList = Arrays.stream(fields).filter(item -> !ignoreFieldNames.contains(item.getName())).collect(Collectors.toList());
            if (isSumSuperClass) {
                Class<?> superclass = obj.getClass().getSuperclass();
                Field[] superFields = superclass.getDeclaredFields();
                List<Field> superFieldList = Arrays.stream(superFields).filter(item -> !ignoreFieldNames.contains(item.getName())).collect(Collectors.toList());
                fieldList.addAll(superFieldList);
            }
        }
        for (Field field : fieldList) {
            if (!Number.class.isAssignableFrom(field.getType())) {
                continue;
            }
            field.setAccessible(true);
            String fieldName = field.getName();
            try {
                BigDecimal value = ObjectUtils.defaultIfNull((BigDecimal) field.get(obj), BigDecimal.ZERO);
                BigDecimal sumObjValue = ObjectUtils.defaultIfNull(ReflectUtils.getFieldValue(resultObj, fieldName), BigDecimal.ZERO);
                BigDecimal result = BigDecimalUtils.add(value, sumObjValue);
                ReflectUtils.setFieldValue(resultObj, fieldName, result);
            } catch (IllegalAccessException e) {
                log.error(e.getMessage());
            }
        }
    }

    /**
     * 将数据字段都拼接到目标对象
     *
     * @param obj              取值的对象
     * @param resultObj        要拼接的对象
     * @param ignoreFieldNames 要忽略的属性名
     */
    private static void appendAllField(Object obj, Object resultObj, List<String> ignoreFieldNames) {
        appendAllField(obj, resultObj, ignoreFieldNames, StringUtils.EMPTY, Boolean.FALSE);
    }

    /**
     * 将数据字段都拼接到目标对象
     *
     * @param obj              取值的对象
     * @param resultObj        要拼接的对象
     * @param ignoreFieldNames 要忽略的属性名
     */
    private static void appendAllField(Object obj, Object resultObj, List<String> ignoreFieldNames, CharSequence delimiter, Boolean isSumSuperClass) {
        Field[] fields = obj.getClass().getDeclaredFields();
        List<Field> fieldList = Arrays.asList(fields);
        if (CollectionUtils.isNotEmpty(ignoreFieldNames)) {
            fieldList = Arrays.stream(fields).filter(item -> !ignoreFieldNames.contains(item.getName())).collect(Collectors.toList());
            if (isSumSuperClass) {
                Class<?> superclass = obj.getClass().getSuperclass();
                Field[] superFields = superclass.getDeclaredFields();
                List<Field> superFieldList = Arrays.stream(superFields).filter(item -> !ignoreFieldNames.contains(item.getName())).collect(Collectors.toList());
                fieldList.addAll(superFieldList);
            }
        }
        for (Field field : fieldList) {
            if (field.getType() != String.class) {
                continue;
            }
            field.setAccessible(true);
            String fieldName = field.getName();
            try {
                String value = ObjectUtils.defaultIfNull((String) field.get(obj), StringUtils.EMPTY);
                String sumObjValue = ObjectUtils.defaultIfNull(ReflectUtils.getFieldValue(resultObj, fieldName), StringUtils.EMPTY);
                if (StringUtils.isNotBlank(value) && !sumObjValue.contains(value)) {
                    String result;
                    if (StringUtils.isBlank(sumObjValue)) {
                        result = value;
                    } else {
                        result = sumObjValue + delimiter + value;
                    }
                    ReflectUtils.setFieldValue(resultObj, fieldName, result);
                }
            } catch (IllegalAccessException e) {
                log.error(e.getMessage());
            }
        }
    }

    /**
     * 获取胎类区分-排产受限满足率列表
     *
     * @param queryDto 查询参数
     * @return 结果
     */
    @Override
    public List<TireTypeReportSatisfyRateVo> getReportTireTypeSatisfyRateList(BaseReportDto queryDto) {
        String monthPlanVersion = monthPlanReportMapper.selectFinalMonthPlanVersion(queryDto);
        queryDto.setMonthPlanVersion(monthPlanVersion);
//        List<TireTypeReportSatisfyRateVo> tireTypeSaleList = monthPlanReportMapper.selectBaseTireTypeSatisfyRateList(queryDto);
//        if (CollectionUtils.isEmpty(tireTypeSaleList)) {
//            return Collections.emptyList();
//        }

        List<TireTypeReportSatisfyRateVo> resultList = new ArrayList<>();
        TireTypeReportSatisfyRateVo patternVoAllSeason = new TireTypeReportSatisfyRateVo();
        patternVoAllSeason.setTypeName("四季胎");

        TireTypeReportSatisfyRateVo patternVoLt = new TireTypeReportSatisfyRateVo();
        patternVoLt.setTypeName("轻卡四季胎");

        TireTypeReportSatisfyRateVo outWinVo = new TireTypeReportSatisfyRateVo();
        outWinVo.setTypeName("外贸雪地胎");

        TireTypeReportSatisfyRateVo inWinVo = new TireTypeReportSatisfyRateVo();
        inWinVo.setTypeName("内销雪地胎");

        TireTypeReportSatisfyRateVo outWhiteSideVo = new TireTypeReportSatisfyRateVo();
        outWhiteSideVo.setTypeName("外销白胎侧规格");

        TireTypeReportSatisfyRateVo inWhiteSideVo = new TireTypeReportSatisfyRateVo();
        inWhiteSideVo.setTypeName("内销白胎侧规格");

        TireTypeReportSatisfyRateVo silentCottonVo = new TireTypeReportSatisfyRateVo();
        silentCottonVo.setTypeName("静音棉");

        TireTypeReportSatisfyRateVo lessThan300Vo = new TireTypeReportSatisfyRateVo();
        lessThan300Vo.setTypeName("单个SKU需求小于等于300条");

//        List<CxScheduleResultReportVo> cxScheduleResultReportVoList = monthPlanReportMapper.selectCxScheduleResultQty(queryDto);
//        Map<String, CxScheduleResultReportVo> cxResultMap = cxScheduleResultReportVoList.stream().collect(Collectors.toMap(item -> String.join("|", item.getProductCode(), item.getProSize().toString()), Function.identity(), (v1, v2) -> v1, LinkedHashMap::new));
        TireTypeReportSatisfyRateVo proSize14LtVo = new TireTypeReportSatisfyRateVo();
        proSize14LtVo.setTypeName("14寸轻卡");

        TireTypeReportSatisfyRateVo proSize14WinVo = new TireTypeReportSatisfyRateVo();
        proSize14WinVo.setTypeName("14寸雪地胎");

        TireTypeReportSatisfyRateVo proSize14WinAndLtVo = new TireTypeReportSatisfyRateVo();
        proSize14WinAndLtVo.setTypeName("14寸轻卡雪地胎");

        TireTypeReportSatisfyRateVo proSize18Vo = new TireTypeReportSatisfyRateVo();
        proSize18Vo.setTypeName("18寸");

        TireTypeReportSatisfyRateVo proSize19Vo = new TireTypeReportSatisfyRateVo();
        proSize19Vo.setTypeName("19寸");

        TireTypeReportSatisfyRateVo singleMould = new TireTypeReportSatisfyRateVo();
        singleMould.setTypeName("单模排产情况");
        List<String> singleMouldProductCodeList = monthPlanReportMapper.selectSingleMouldProductCodeList();

        // 合计
        TireTypeReportSatisfyRateVo totalVo = new TireTypeReportSatisfyRateVo();
        totalVo.setTypeName("合计");

        // 关联模具关系表取规格代码
        Map<String, String> productMoldeRealMap = new HashMap<>(16);
        List<MdmSkuMouldRel> productMoldeRealList = monthPlanReportMapper.selectProductMoldeRealList(queryDto);
        if (CollectionUtils.isNotEmpty(productMoldeRealList)) {
            productMoldeRealMap = productMoldeRealList.stream().collect(Collectors.toMap(MdmSkuMouldRel::getMaterialCode, MdmSkuMouldRel::getSpecCode, (s1, s2) -> String.join(",", s1, s2)));
        }
        // 关联施工赋值胶种
        List<TireTypeConstructionRealVo> tireTypeConstructionRealVoList = monthPlanReportMapper.selectConstructionReal();
        Map<String, TireTypeConstructionRealVo> constructionRealVoMap = tireTypeConstructionRealVoList.stream().collect(Collectors
                .toMap(item -> String.join("|", item.getProductCode(), item.getSpecCode()), Function.identity(), (v1, v2) -> v1, LinkedHashMap::new));

        // 基础数据列表Map，K：关联唯一键，V：唯一键对应的数据，处理完了之后走原有的统计方法
        Map<String, TireTypeReportSatisfyRateVo> baseGroupMap = new HashMap<>(16);
        // 生产计划列表
        List<TireTypeReportSatisfyRateVo> proPlanList = monthPlanReportMapper.selectProPlan4TireTypeSatisfyRate(queryDto);

        if (CollectionUtils.isNotEmpty(proPlanList)) {
            JsonI18nConvertUtils.conventJsonI18n(proPlanList, TireTypeReportSatisfyRateVo.class);

            for (TireTypeReportSatisfyRateVo proPlanVo : proPlanList) {
                TireTypeReportSatisfyRateVo defaultValue = new TireTypeReportSatisfyRateVo();
                String productCode = proPlanVo.getProductCode();
                BeanUtils.copyPropertiesIgnoreNull(proPlanVo, defaultValue);
                defaultValue.setProducePlanQty(BigDecimal.ZERO);
                defaultValue.setProduceSkuCount(BigDecimal.ZERO);
                TireTypeReportSatisfyRateVo productVo = baseGroupMap.getOrDefault(productCode, defaultValue);
                BigDecimal producePlanQty = proPlanVo.getProducePlanQty();
                BigDecimal produceSkuCount = proPlanVo.getProduceSkuCount();
                productVo.setProducePlanQty(BigDecimalUtils.add(producePlanQty, productVo.getProducePlanQty()));
                productVo.setProduceSkuCount(baseGroupMap.containsKey(productCode) ? produceSkuCount : BigDecimalUtils.add(produceSkuCount, productVo.getProduceSkuCount()));
                baseGroupMap.put(productCode, productVo);
            }
        }

        // 销售计划列表
        List<TireTypeReportSatisfyRateVo> salePlanList = monthPlanReportMapper.selectSalePlan4TireTypeSatisfyRate(queryDto);
        if (CollectionUtils.isNotEmpty(salePlanList)) {
            for (TireTypeReportSatisfyRateVo salePlanVo : salePlanList) {
                TireTypeReportSatisfyRateVo defaultValue = new TireTypeReportSatisfyRateVo();
                String productCode = salePlanVo.getProductCode();
                BeanUtils.copyPropertiesIgnoreNull(salePlanVo, defaultValue);
                defaultValue.setSalePlanQty(BigDecimal.ZERO);
                defaultValue.setSaleSkuCount(BigDecimal.ZERO);
                TireTypeReportSatisfyRateVo productVo = baseGroupMap.getOrDefault(productCode, defaultValue);
                BigDecimal salePlanQty = salePlanVo.getSalePlanQty();
                BigDecimal saleSkuCount = salePlanVo.getSaleSkuCount();
                productVo.setSalePlanQty(BigDecimalUtils.add(salePlanQty, productVo.getSalePlanQty()));
                productVo.setSaleSkuCount(baseGroupMap.containsKey(productCode) ? saleSkuCount : BigDecimalUtils.add(saleSkuCount, productVo.getSaleSkuCount()));
                baseGroupMap.put(productCode, productVo);
            }
        }

        // 添加调整通知单计划数
//        List<MonthPlanNoticeOrder> monthPlanNoticeOrderList = monthPlanReportMapper.selectMonthPlanAdjustNotice(queryDto);
//        Map<String, List<MonthPlanNoticeOrder>> adjustNoticeMap = new HashMap<>(16);
//        if (CollectionUtils.isNotEmpty(monthPlanNoticeOrderList)) {
//            // 调整通知单数量
//            if (CollectionUtils.isNotEmpty(monthPlanNoticeOrderList)) {
//                adjustNoticeMap = monthPlanNoticeOrderList.stream().filter(item -> item.getNeedQty() != null && item.getNeedQty() > 0)
//                        .collect(Collectors.groupingBy(MonthPlanNoticeOrder::getProductCode));
//            }
//            Set<Map.Entry<String, List<MonthPlanNoticeOrder>>> entrySet = adjustNoticeMap.entrySet();
//            for (Map.Entry<String, List<MonthPlanNoticeOrder>> entry : entrySet) {
//                String productCode = entry.getKey();
//                List<MonthPlanNoticeOrder> value = entry.getValue();
//                long needQty = 0;
//                TireTypeReportSatisfyRateVo defaultValue = new TireTypeReportSatisfyRateVo();
//                for (MonthPlanNoticeOrder planNoticeOrder : value) {
//                    needQty += planNoticeOrder.getNeedQty();
//                    defaultValue.setProductCode(productCode);
//                    defaultValue.setProductDesc(planNoticeOrder.getProductDesc());
//                    defaultValue.setProSize(planNoticeOrder.getProSize());
//                    defaultValue.setBrand(planNoticeOrder.getBrand());
//                    defaultValue.setPattern(planNoticeOrder.getPattern());
//                    defaultValue.setLocationType(planNoticeOrder.getLocationType());
//                    defaultValue.setChannel(planNoticeOrder.getChannel());
//                }
//                defaultValue.setSalePlanQty(BigDecimal.ZERO);
//                defaultValue.setSaleSkuCount(BigDecimal.ZERO);
//                TireTypeReportSatisfyRateVo productVo = baseGroupMap.getOrDefault(productCode, defaultValue);
//                productVo.setSalePlanQty(BigDecimalUtils.add(needQty, productVo.getSalePlanQty()));
//                productVo.setSaleSkuCount(baseGroupMap.containsKey(productCode) ? BigDecimal.ONE : BigDecimalUtils.add(1, productVo.getSaleSkuCount()));
//                baseGroupMap.put(productCode, productVo);
//            }
//        }

        // 库存列表
        List<TireTypeReportSatisfyRateVo> stockList = monthPlanReportMapper.selectStock4TireTypeSatisfyRate(queryDto);
        if (CollectionUtils.isNotEmpty(stockList)) {
            for (TireTypeReportSatisfyRateVo stockVo : stockList) {
                TireTypeReportSatisfyRateVo defaultValue = new TireTypeReportSatisfyRateVo();
                String productCode = stockVo.getProductCode();
                BeanUtils.copyPropertiesIgnoreNull(stockVo, defaultValue);
                defaultValue.setStockQty(BigDecimal.ZERO);
                defaultValue.setStockSkuCount(BigDecimal.ZERO);
                TireTypeReportSatisfyRateVo productVo = baseGroupMap.getOrDefault(productCode, defaultValue);
                BigDecimal stockQty = stockVo.getStockQty();
                BigDecimal stockSkuCount = stockVo.getStockSkuCount();
                productVo.setStockQty(BigDecimalUtils.add(stockQty, productVo.getStockQty()));
                productVo.setStockSkuCount(BigDecimalUtils.add(stockSkuCount, productVo.getStockSkuCount()));
                productVo.setProduceSkuCount(baseGroupMap.containsKey(productCode) ? stockSkuCount : BigDecimalUtils.add(stockSkuCount, productVo.getStockSkuCount()));
                baseGroupMap.put(productCode, productVo);
            }
        }

        for (TireTypeReportSatisfyRateVo tireTypeReportSatisfyRateVo : baseGroupMap.values()) {
            String notSatisfiedReason = StringUtils.defaultIfBlank(tireTypeReportSatisfyRateVo.getNotSatisfiedReason(), "");
            if (notSatisfiedReason.contains("扣除超出模具产能数") || notSatisfiedReason.contains("扣除寸口产能控制限制")) {
                tireTypeReportSatisfyRateVo.setNotSatisfiedReason("");
                tireTypeReportSatisfyRateVo.setNotSatisfiedReasonI18n("");
                tireTypeReportSatisfyRateVo.setNotSatisfiedReasonI18n_zh_CN("");
            }
            String pattern = tireTypeReportSatisfyRateVo.getPattern();
            String tireType = tireTypeReportSatisfyRateVo.getTireType();
            String locationType = tireTypeReportSatisfyRateVo.getLocationType();
            String productCode = tireTypeReportSatisfyRateVo.getProductCode();
            BigDecimal proSize = tireTypeReportSatisfyRateVo.getProSize();

            BigDecimal stockQty = ObjectUtils.defaultIfNull(tireTypeReportSatisfyRateVo.getStockQty(), BigDecimal.ZERO);
            BigDecimal producePlanQty = ObjectUtils.defaultIfNull(tireTypeReportSatisfyRateVo.getProducePlanQty(), BigDecimal.ZERO);
            BigDecimal salePlanQty = ObjectUtils.defaultIfNull(tireTypeReportSatisfyRateVo.getSalePlanQty(), BigDecimal.ZERO);
            BigDecimal result = BigDecimalUtils.add(stockQty, producePlanQty).subtract(salePlanQty);
            tireTypeReportSatisfyRateVo.setGapSkuCount(result.compareTo(BigDecimal.ZERO) > 0 ? BigDecimal.ZERO : BigDecimal.ONE);
            tireTypeReportSatisfyRateVo.setGapQty(result);

            // 赋值胶料，根据物料关联规格代号，取物料号+规格代号关联施工取
            if (productMoldeRealMap.containsKey(productCode)) {
                String specCodeStr = productMoldeRealMap.get(productCode);
                if (specCodeStr.contains(",")) {
                    String[] splitArr = specCodeStr.split(",");
                    for (String specCode : splitArr) {
                        String key = String.join("|", productCode, specCode);
                        if (constructionRealVoMap.containsKey(key)) {
                            TireTypeConstructionRealVo tireTypeConstructionRealVo = constructionRealVoMap.get(key);
                            tireTypeReportSatisfyRateVo.setGlue(tireTypeConstructionRealVo.getTreadCode());
                        }
                    }
                } else {
                    String key = String.join("|", productCode, specCodeStr);
                    if (constructionRealVoMap.containsKey(key)) {
                        TireTypeConstructionRealVo tireTypeConstructionRealVo = constructionRealVoMap.get(key);
                        tireTypeReportSatisfyRateVo.setGlue(tireTypeConstructionRealVo.getTreadCode());
                    }
                }
            }

            List<String> tireTypeIgnoreFieldNameList = new ArrayList<>(this.getTireTypeIgnoreFieldNames());
            tireTypeIgnoreFieldNameList.add("glue");

            if (StringUtils.isNotBlank(pattern)) {
                // 四季胎
                List<String> containsList = PATTERN_LIST.stream().filter(pattern::contains).collect(Collectors.toList());
                if (!containsList.isEmpty()) {
                    sumAllField(tireTypeReportSatisfyRateVo, patternVoAllSeason, this.getTireTypeIgnoreFieldNames());
                    appendAllField(tireTypeReportSatisfyRateVo, patternVoAllSeason, this.getTireTypeIgnoreFieldNames(), ",", Boolean.FALSE);
                }
                List<String> ltContainsList = LT_PATTERN_LIST.stream().filter(pattern::contains).collect(Collectors.toList());
                // 轻卡四季胎
                if (!ltContainsList.isEmpty() && TireTypeEnum.LT.getValue().equals(tireType)) {
                    sumAllField(tireTypeReportSatisfyRateVo, patternVoLt, this.getTireTypeIgnoreFieldNames());
                    appendAllField(tireTypeReportSatisfyRateVo, patternVoLt, this.getTireTypeIgnoreFieldNames(), ",", Boolean.FALSE);
                    sumAllField(tireTypeReportSatisfyRateVo, totalVo, this.getTireTypeIgnoreFieldNames());
                }
                // 雪地胎
                if (TireTypeEnum.WIN.getValue().equals(tireType)) {
                    // 外贸
                    if (LocationTypeEnum.FOREIGN_LOCATION.getValue().equals(locationType)) {
                        sumAllField(tireTypeReportSatisfyRateVo, outWinVo, this.getTireTypeIgnoreFieldNames());
                        appendAllField(tireTypeReportSatisfyRateVo, outWinVo, this.getTireTypeIgnoreFieldNames(), ",", Boolean.FALSE);
                        sumAllField(tireTypeReportSatisfyRateVo, totalVo, this.getTireTypeIgnoreFieldNames());
                    }
                    // 内销
                    if (LocationTypeEnum.DOMESTIC_LOCATION.getValue().equals(locationType)) {
                        sumAllField(tireTypeReportSatisfyRateVo, inWinVo, this.getTireTypeIgnoreFieldNames());
                        appendAllField(tireTypeReportSatisfyRateVo, inWinVo, this.getTireTypeIgnoreFieldNames(), ",", Boolean.FALSE);
                        sumAllField(tireTypeReportSatisfyRateVo, totalVo, this.getTireTypeIgnoreFieldNames());
                    }
                }
                // 白胎侧
                if (pattern.endsWith(WHITE_SIDEWALL_SUFFIX)) {
                    // 外贸
                    if (LocationTypeEnum.FOREIGN_LOCATION.getValue().equals(locationType)) {
                        sumAllField(tireTypeReportSatisfyRateVo, outWhiteSideVo, this.getTireTypeIgnoreFieldNames());
                        appendAllField(tireTypeReportSatisfyRateVo, outWhiteSideVo, this.getTireTypeIgnoreFieldNames(), ",", Boolean.FALSE);
                        sumAllField(tireTypeReportSatisfyRateVo, totalVo, this.getTireTypeIgnoreFieldNames());
                    }
                    // 内销
                    if (LocationTypeEnum.DOMESTIC_LOCATION.getValue().equals(locationType)) {
                        sumAllField(tireTypeReportSatisfyRateVo, inWhiteSideVo, this.getTireTypeIgnoreFieldNames());
                        appendAllField(tireTypeReportSatisfyRateVo, inWhiteSideVo, this.getTireTypeIgnoreFieldNames(), ",", Boolean.FALSE);
                        sumAllField(tireTypeReportSatisfyRateVo, totalVo, this.getTireTypeIgnoreFieldNames());
                    }
                }
                // 静音棉
                if (pattern.endsWith(SILENT_COTTON_SUFFIX)) {
                    sumAllField(tireTypeReportSatisfyRateVo, silentCottonVo, this.getTireTypeIgnoreFieldNames());
                    appendAllField(tireTypeReportSatisfyRateVo, silentCottonVo, tireTypeIgnoreFieldNameList, ",", Boolean.FALSE);
                    sumAllField(tireTypeReportSatisfyRateVo, totalVo, this.getTireTypeIgnoreFieldNames());
                }
            }

            /*String cxResultKey = String.join("|", productCode, proSize.toString());
            if (cxResultMap.containsKey(cxResultKey)) {

            }*/
            // 14寸轻卡、雪地胎、轻卡雪地胎
            if (BigDecimal.valueOf(14).compareTo(proSize) == 0) {
                // 轻卡胎
                if (TireTypeEnum.LT.getValue().equals(tireType)) {
                    sumAllField(tireTypeReportSatisfyRateVo, proSize14LtVo, this.getTireTypeIgnoreFieldNames());
                    appendAllField(tireTypeReportSatisfyRateVo, proSize14LtVo, tireTypeIgnoreFieldNameList, ",", Boolean.FALSE);
                    sumAllField(tireTypeReportSatisfyRateVo, totalVo, this.getTireTypeIgnoreFieldNames());
                }
                // 雪地胎
                if (TireTypeEnum.WIN.getValue().equals(tireType)) {
                    sumAllField(tireTypeReportSatisfyRateVo, proSize14WinVo, this.getTireTypeIgnoreFieldNames());
                    appendAllField(tireTypeReportSatisfyRateVo, proSize14WinVo, tireTypeIgnoreFieldNameList, ",", Boolean.FALSE);
                    sumAllField(tireTypeReportSatisfyRateVo, totalVo, this.getTireTypeIgnoreFieldNames());
                }
                // 雪地轻卡胎
                if (TireTypeEnum.LT_WIN.getValue().equals(tireType)) {
                    sumAllField(tireTypeReportSatisfyRateVo, proSize14WinAndLtVo, this.getTireTypeIgnoreFieldNames());
                    appendAllField(tireTypeReportSatisfyRateVo, proSize14WinAndLtVo, tireTypeIgnoreFieldNameList, ",", Boolean.FALSE);
                    sumAllField(tireTypeReportSatisfyRateVo, totalVo, this.getTireTypeIgnoreFieldNames());
                }
            }
            // 18寸
            if (BigDecimal.valueOf(18).compareTo(proSize) == 0) {
                sumAllField(tireTypeReportSatisfyRateVo, proSize18Vo, this.getTireTypeIgnoreFieldNames());
                appendAllField(tireTypeReportSatisfyRateVo, proSize18Vo, tireTypeIgnoreFieldNameList, ",", Boolean.FALSE);
                sumAllField(tireTypeReportSatisfyRateVo, totalVo, this.getTireTypeIgnoreFieldNames());
            }
            // 19寸
            if (BigDecimal.valueOf(19).compareTo(proSize) == 0) {
                sumAllField(tireTypeReportSatisfyRateVo, proSize19Vo, this.getTireTypeIgnoreFieldNames());
                appendAllField(tireTypeReportSatisfyRateVo, proSize19Vo, tireTypeIgnoreFieldNameList, ",", Boolean.FALSE);
                sumAllField(tireTypeReportSatisfyRateVo, totalVo, this.getTireTypeIgnoreFieldNames());
            }
            // 生产计划量<=300
            if (salePlanQty.compareTo(BigDecimal.valueOf(300)) <= 0) {
                sumAllField(tireTypeReportSatisfyRateVo, lessThan300Vo, this.getTireTypeIgnoreFieldNames());
                appendAllField(tireTypeReportSatisfyRateVo, lessThan300Vo, tireTypeIgnoreFieldNameList, ",", Boolean.FALSE);
            }
            // 单模排产
            if (singleMouldProductCodeList.contains(productCode)) {
                sumAllField(tireTypeReportSatisfyRateVo, singleMould, this.getTireTypeIgnoreFieldNames());
                appendAllField(tireTypeReportSatisfyRateVo, singleMould, tireTypeIgnoreFieldNameList, ",", Boolean.FALSE);
            }
        }

        resultList.add(patternVoAllSeason);
        resultList.add(patternVoLt);
        resultList.add(outWinVo);
        resultList.add(inWinVo);
        resultList.add(outWhiteSideVo);
        resultList.add(inWhiteSideVo);
        resultList.add(silentCottonVo);
        resultList.add(proSize14LtVo);
        resultList.add(proSize14WinVo);
        resultList.add(proSize14WinAndLtVo);
        resultList.add(proSize18Vo);
        resultList.add(proSize19Vo);
        resultList.add(lessThan300Vo);
        resultList.add(singleMould);
        resultList.add(totalVo);
        return resultList;
    }

    /**
     * 导出胎类区分-排产受限满足率列表
     *
     * @param queryDto 查询条件
     * @return 结果
     */
    @Override
    public byte[] exportTireTypeSatisfyRate(BaseReportDto queryDto) {
        // 获取模板
        ClassLoader classLoader = this.getClass().getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream("excelModel/tireTypeSatisfyRate.xlsx");
        // 表头信息
        Map<String, Object> tableMap = new HashMap<>(16);
        // 列表数据
        List<List<Map<String, Object>>> excelDataList = new ArrayList<>();
        List<TireTypeReportSatisfyRateVo> voList = this.getReportTireTypeSatisfyRateList(queryDto);
        if (CollectionUtils.isNotEmpty(voList)) {
            List<Map<String, Object>> list = new ArrayList<>();
            for (int i = 0; i < voList.size(); i++) {
                TireTypeReportSatisfyRateVo vo = voList.get(i);
                Map<String, Object> listDataMap = new HashMap<>(16);
                // 添加数据
                putFieldToMap(vo, listDataMap,
                        Arrays.asList("serialVersionUID", "productCode", "productDesc", "proSize", "brand", "tireType", "pattern", "locationType", "channel"),
                        StringUtils.EMPTY, Boolean.FALSE);
                listDataMap.put("index", i + 1);
                list.add(listDataMap);
                excelDataList.add(list);
            }
        }
        // 写到文件
        return ExcelUtils.writeMultiList(inputStream
                , 0, tableMap, excelDataList);
    }

    private List<String> getTireTypeIgnoreFieldNames() {
        return Arrays.asList("serialVersionUID", "productCode", "productDesc", "proSize", "brand", "tireType", "pattern", "locationType", "channel", "typeName");
    }

    /**
     * 查询胎类区分-月份综合
     *
     * @param queryDto 查询条件
     * @return 结果
     */
    @Override
    public List<TireTypeClassificationVo> selectMonthTireTypeList(TireTypeReportDto queryDto) {
        String monthPlanVersion = monthPlanReportMapper.selectFinalMonthPlanVersion(queryDto);
        queryDto.setMonthPlanVersion(monthPlanVersion);
        List<TireTypeClassificationVo> resultList = new ArrayList<>();
        LambdaQueryWrapper<OrderPlanAllocation> orderQueryWrapper = new LambdaQueryWrapper<>();
        orderQueryWrapper.eq(StringUtils.isNotBlank(queryDto.getChannel()), OrderPlanAllocation::getChannel, queryDto.getChannel());
        List<OrderPlanAllocation> orderPlanAllocationList = orderPlanAllocationMapper.selectList(orderQueryWrapper);
        Map<String, OrderPlanAllocation> orderPlanAllocationMap = orderPlanAllocationList.stream().collect(Collectors.toMap(OrderPlanAllocation::getOrderNo, Function.identity(), (v1, v2) -> v1, LinkedHashMap::new));

        Map<String, FactoryMonthPlanProdFinal> prodFinalMap = new HashMap<>(16);
        LambdaQueryWrapper<FactoryMonthPlanProdFinal> finalQueryWrapper = new LambdaQueryWrapper<>();
        finalQueryWrapper.eq(FactoryMonthPlanProdFinal::getYear, queryDto.getYear());
        finalQueryWrapper.eq(FactoryMonthPlanProdFinal::getMonth, queryDto.getMonth());
        finalQueryWrapper.eq(FactoryMonthPlanProdFinal::getMonthPlanVersion, monthPlanVersion);
        List<FactoryMonthPlanProdFinal> factoryMonthPlanProdFinalList = factoryMonthPlanProdFinalMapper.selectList(finalQueryWrapper);
        if (CollectionUtils.isNotEmpty(factoryMonthPlanProdFinalList)) {
            JsonI18nConvertUtils.conventJsonI18n(factoryMonthPlanProdFinalList, FactoryMonthPlanProdFinal.class);
            prodFinalMap = factoryMonthPlanProdFinalList.stream().collect(Collectors.toMap(FactoryMonthPlanProdFinal::getUpdateImportValue, Function.identity(), (v1, v2) -> v1, LinkedHashMap::new));
        }

        // 总库存、需求、排产的情况
        TireTypeClassificationVo allStockSaleProduce = new TireTypeClassificationVo();
        allStockSaleProduce.setClassificationName("总库存、需求、排产的情况");
        queryDto.setHasOrder(ApsConstant.APS_STRING_1);
        List<TireTypeClassificationAllVo> list = monthPlanReportMapper.selectTireTypeProduceList(queryDto);
        this.addAndCalculateFields4AllVo(queryDto, allStockSaleProduce, orderPlanAllocationMap, resultList, list, prodFinalMap);
        // 无订单销售需求的SKU
        TireTypeClassificationVo saleProduce = new TireTypeClassificationVo();
        saleProduce.setClassificationName("无订单销售需求的SKU");
        queryDto.setHasOrder(ApsConstant.APS_STRING_0);
        queryDto.setHasGap(ApsConstant.APS_STRING_2);
        list = monthPlanReportMapper.selectTireTypeProduceList(queryDto);
        this.addAndCalculateFields4AllVo(queryDto, saleProduce, orderPlanAllocationMap, resultList, list, prodFinalMap);
        // 有订单无缺口(无未排)
        TireTypeClassificationVo hasOrderNoProduce = new TireTypeClassificationVo();
        hasOrderNoProduce.setClassificationName("有订单无缺口");
        queryDto.setHasOrder(ApsConstant.APS_STRING_1);
        queryDto.setHasNoProduce(ApsConstant.APS_STRING_2);
        queryDto.setHasGap(ApsConstant.APS_STRING_0);
        list = monthPlanReportMapper.selectTireTypeProduceList(queryDto);
        this.addAndCalculateFields4AllVo(queryDto, hasOrderNoProduce, orderPlanAllocationMap, resultList, list, prodFinalMap);
        // 有订单有缺口(部分未排)
        TireTypeClassificationVo hasOrderSomeNoProduce = new TireTypeClassificationVo();
        hasOrderSomeNoProduce.setClassificationName("有订单需求有缺口（部分排产：没有模具或者模具产能不足）");
        queryDto.setHasOrder(ApsConstant.APS_STRING_1);
        queryDto.setHasNoProduce(ApsConstant.APS_STRING_0);
        queryDto.setHasGap(ApsConstant.APS_STRING_1);
        List<TireTypeClassificationVo> voList = monthPlanReportMapper.selectTireTypeNoProduceList(queryDto);
        this.addAndCalculateFields(queryDto, hasOrderSomeNoProduce, orderPlanAllocationMap, resultList, voList);
        // 有订单有缺口(全部未排)
        TireTypeClassificationVo hasOrderAllNoProduce = new TireTypeClassificationVo();
        hasOrderAllNoProduce.setClassificationName("有订单需求有缺口（全部未排产）");
        queryDto.setHasOrder(ApsConstant.APS_STRING_1);
        queryDto.setHasNoProduce(ApsConstant.APS_STRING_1);
        queryDto.setHasGap(ApsConstant.APS_STRING_1);
        voList = monthPlanReportMapper.selectTireTypeNoProduceList(queryDto);
        this.addAndCalculateFields(queryDto, hasOrderAllNoProduce, orderPlanAllocationMap, resultList, voList);
        // 无订单欠产
        TireTypeClassificationVo noOrderDebt = new TireTypeClassificationVo();
        noOrderDebt.setClassificationName("无订单需求欠产排产");
        queryDto.setHasOrder(ApsConstant.APS_STRING_0);
        queryDto.setIsDebitPlan(YesOrNoEnum.YES.getValue());
        queryDto.setHasGap(ApsConstant.APS_STRING_2);
        list = monthPlanReportMapper.selectTireTypeProduceList(queryDto);
        this.addAndCalculateFields4AllVo(queryDto, noOrderDebt, orderPlanAllocationMap, resultList, list, prodFinalMap);
        // 无订单备货
        TireTypeClassificationVo noOrderStockUp = new TireTypeClassificationVo();
        noOrderStockUp.setClassificationName("无订单需求备货排产");
        queryDto.setHasOrder(ApsConstant.APS_STRING_0);
        queryDto.setIsStockUp(YesOrNoEnum.YES.getValue());
        queryDto.setHasGap(ApsConstant.APS_STRING_2);
        list = monthPlanReportMapper.selectTireTypeProduceList(queryDto);
        this.addAndCalculateFields4AllVo(queryDto, noOrderStockUp, orderPlanAllocationMap, resultList, list, prodFinalMap);
        // 有订单未排库存直接满足
        TireTypeClassificationVo stockSatisfied = new TireTypeClassificationVo();
        stockSatisfied.setClassificationName("有订单需求未排产库存直接满足");
        queryDto.setHasGap(ApsConstant.APS_STRING_2);
        voList = monthPlanReportMapper.selectTireTypeStockSatisfiedList(queryDto);
        this.addAndCalculateFields(queryDto, stockSatisfied, orderPlanAllocationMap, resultList, voList);
        // 有订单未排试制量试无工艺
        TireTypeClassificationVo trialVo = new TireTypeClassificationVo();
        trialVo.setClassificationName("有订单未排试制量试无工艺");
        queryDto.setHasGap(ApsConstant.APS_STRING_2);
        voList = monthPlanReportMapper.selectTireTypeTrialList(queryDto);
        this.addAndCalculateFields(queryDto, trialVo, orderPlanAllocationMap, resultList, voList);
        return resultList;
    }

    /**
     * 导出胎类区分-月份综合
     *
     * @param queryDto 查询条件
     * @return 结果
     */
    @Override
    public byte[] exportMonthTireType(TireTypeReportDto queryDto) {
        // 获取模板
        ClassLoader classLoader = this.getClass().getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream("excelModel/monthTireType.xlsx");
        // 表头信息
        Map<String, Object> tableMap = new HashMap<>(16);
        // 列表数据
        List<List<Map<String, Object>>> excelDataList = new ArrayList<>();
        List<TireTypeClassificationVo> voList = this.selectMonthTireTypeList(queryDto);
        String channel = queryDto.getChannel();
        if (StringUtils.isNotBlank(channel)) {
            Map<String, String> dictMapByType = getDictMapByType(CHANNEL_DICT_TYPE);
            if (dictMapByType.containsKey(channel)) {
                String dictLabel = dictMapByType.get(channel);
                tableMap.put("title", dictLabel);
            }
        } else {
            tableMap.put("title", queryDto.getMonth() + "月份综合");
        }
        if (CollectionUtils.isNotEmpty(voList)) {
            List<Map<String, Object>> list = new ArrayList<>();
            for (int i = 0; i < voList.size(); i++) {
                TireTypeClassificationVo vo = voList.get(i);
                Map<String, Object> listDataMap = new HashMap<>(16);
                // 添加数据
                putFieldToMap(vo, listDataMap,
                        Arrays.asList("serialVersionUID", "orderNo"),
                        StringUtils.EMPTY, Boolean.FALSE);
                listDataMap.put("index", i + 1);
                list.add(listDataMap);
                excelDataList.add(list);
            }
        }
        // 写到文件
        return ExcelUtils.writeMultiList(inputStream
                , 0, tableMap, excelDataList);
    }

    /**
     * 添加对应数据
     *
     * @param queryDto               对应数据查询参数
     * @param classificationVo       要统计的对象
     * @param orderPlanAllocationMap 订单排产Map
     * @param resultList             结果列表
     * @param list                   要统计的列表
     */
    private void addAndCalculateFields(TireTypeReportDto queryDto, TireTypeClassificationVo classificationVo, Map<String, OrderPlanAllocation> orderPlanAllocationMap, List<TireTypeClassificationVo> resultList, List<TireTypeClassificationVo> list) {
        for (TireTypeClassificationVo vo : list) {
            String notSatisfiedReason = StringUtils.defaultIfBlank(vo.getNotSatisfiedReason(), "");
            if (notSatisfiedReason.contains("扣除超出模具产能数") || notSatisfiedReason.contains("扣除寸口产能控制限制")) {
                vo.setNotSatisfiedReason("");
                vo.setNotSatisfiedReasonI18n("");
            }
            // 计算缺口等
            vo.calculateFields();
            BigDecimal gapQty = Optional.ofNullable(vo.getGapQty()).orElse(BigDecimal.ZERO);
            // 统计的数据是否有缺口
            if (ApsConstant.APS_STRING_0.equals(queryDto.getHasGap()) && BigDecimal.ZERO.compareTo(gapQty) != 0) {
                continue;
            }
            if (ApsConstant.APS_STRING_1.equals(queryDto.getHasGap()) && BigDecimal.ZERO.compareTo(gapQty) == 0) {
                continue;
            }
            sumAllField(vo, classificationVo, Collections.singletonList("classificationName"));
            appendAllField(vo, classificationVo, Collections.singletonList("classificationName"), DELIMITER, Boolean.FALSE);

            String orderNo = vo.getOrderNo();
            if (StringUtils.isNotBlank(orderNo)) {
                String[] splitArr = orderNo.split(",");
                for (String order : splitArr) {
                    if (orderPlanAllocationMap.containsKey(order)) {
                        OrderPlanAllocation orderPlanAllocation = orderPlanAllocationMap.get(order);
                        Long planQty = Optional.ofNullable(orderPlanAllocation.getPlanQty()).orElse(0L);
                        BigDecimal totalSalePlanQty = Optional.ofNullable(classificationVo.getTotalSalePlanQty()).orElse(BigDecimal.ZERO);
                        classificationVo.setTotalSalePlanQty(BigDecimalUtils.add(totalSalePlanQty, new BigDecimal(planQty)));
                        BigDecimal saleSkuCount = Optional.ofNullable(classificationVo.getSaleSkuCount()).orElse(BigDecimal.ZERO);
                        classificationVo.setSaleSkuCount(BigDecimalUtils.add(saleSkuCount, BigDecimal.ONE));
                    }
                }
            }
        }
        classificationVo.calculateFields();
        resultList.add(classificationVo);
    }

    /**
     * 添加对应数据
     *
     * @param queryDto               对应数据查询参数
     * @param classificationVo       要统计的对象
     * @param orderPlanAllocationMap 订单排产Map
     * @param resultList             结果列表
     * @param list                   要统计的列表
     */
    private void addAndCalculateFields4AllVo(TireTypeReportDto queryDto, TireTypeClassificationVo classificationVo, Map<String, OrderPlanAllocation> orderPlanAllocationMap, List<TireTypeClassificationVo> resultList, List<TireTypeClassificationAllVo> list, Map<String, FactoryMonthPlanProdFinal> prodFinalMap) {
        for (TireTypeClassificationAllVo vo : list) {
            String notSatisfiedReason = StringUtils.defaultIfBlank(vo.getNotSatisfiedReason(), "");
            if (notSatisfiedReason.contains("扣除超出模具产能数") || notSatisfiedReason.contains("扣除寸口产能控制限制")) {
                vo.setNotSatisfiedReason("");
                vo.setNotSatisfiedReasonI18n("");
            }
            // 计算缺口等
            vo.calculateFields();
            BigDecimal gapQty = Optional.ofNullable(vo.getGapQty()).orElse(BigDecimal.ZERO);
            // 统计的数据是否有缺口
            if (ApsConstant.APS_STRING_0.equals(queryDto.getHasGap()) && BigDecimal.ZERO.compareTo(gapQty) != 0) {
                continue;
            }
            if (ApsConstant.APS_STRING_1.equals(queryDto.getHasGap()) && BigDecimal.ZERO.compareTo(gapQty) == 0) {
                continue;
            }
            String summaryValue = vo.getSummaryValue();
            if (prodFinalMap.containsKey(summaryValue)) {
                FactoryMonthPlanProdFinal factoryMonthPlanProdFinal = prodFinalMap.get(summaryValue);
                String reasonI18n = StringUtils.defaultIfBlank(factoryMonthPlanProdFinal.getReasonI18n(), "");
                if (reasonI18n.contains("扣除超出模具产能数") || reasonI18n.contains("扣除寸口产能控制限制")) {
                    reasonI18n = "";
                }
                vo.setNotSatisfiedReasonI18n(reasonI18n);
            }
            sumAllField(vo, classificationVo, Arrays.asList("classificationName", "productionVersion", "productCode", "locationType", "brand", "channel", "deliveryDateDue", "isImport", "specCode"), Boolean.TRUE);
            appendAllField(vo, classificationVo, Arrays.asList("classificationName", "productionVersion", "productCode", "locationType", "brand", "channel", "deliveryDateDue", "isImport", "specCode"), DELIMITER, Boolean.TRUE);

            String orderNo = vo.getOrderNo();
            if (StringUtils.isNotBlank(orderNo)) {
                String[] splitArr = orderNo.split(",");
                for (String order : splitArr) {
                    if (orderPlanAllocationMap.containsKey(order)) {
                        OrderPlanAllocation orderPlanAllocation = orderPlanAllocationMap.get(order);
                        Long planQty = Optional.ofNullable(orderPlanAllocation.getPlanQty()).orElse(0L);
                        BigDecimal totalSalePlanQty = Optional.ofNullable(classificationVo.getTotalSalePlanQty()).orElse(BigDecimal.ZERO);
                        classificationVo.setTotalSalePlanQty(BigDecimalUtils.add(totalSalePlanQty, new BigDecimal(planQty)));
                        BigDecimal saleSkuCount = Optional.ofNullable(classificationVo.getSaleSkuCount()).orElse(BigDecimal.ZERO);
                        classificationVo.setSaleSkuCount(BigDecimalUtils.add(saleSkuCount, BigDecimal.ONE));
                    }
                }
            }
        }
        classificationVo.calculateFields();
        resultList.add(classificationVo);
    }

    /**
     * 添加寸口维度合计数据
     *
     * @param baseList     基础数据列表
     * @param resultVoList 结果列表
     */
    private static void addProSizeTotalVo(List<ProduceSalePlanResultVo> baseList, List<ProduceSalePlanResultVo> resultVoList) {
        ProduceSalePlanResultVo proSizeTotalSumVo = new ProduceSalePlanResultVo();
        Map<BigDecimal, List<ProduceSalePlanResultVo>> proSizeGroupMap = baseList.stream().collect(Collectors.groupingBy(ProduceSalePlanResultVo::getProSize, LinkedHashMap::new, Collectors.toList()));
        Set<Map.Entry<BigDecimal, List<ProduceSalePlanResultVo>>> proSizeEntrySet = proSizeGroupMap.entrySet();

        Map<BigDecimal, ProduceSalePlanResultVo> proSizeSumMap = new LinkedHashMap<>(16);
        for (Map.Entry<BigDecimal, List<ProduceSalePlanResultVo>> entry : proSizeEntrySet) {
            List<ProduceSalePlanResultVo> value = entry.getValue();
            ProduceSalePlanResultVo proSizeSumVo = new ProduceSalePlanResultVo();
            proSizeSumVo.setProSize(entry.getKey());
            proSizeSumVo.setShowProSize(entry.getKey().toString());
            proSizeSumVo.setChannelName("合计");
            for (ProduceSalePlanResultVo produceSalePlanResultVo : value) {
                sumAllField(produceSalePlanResultVo, proSizeSumVo, Arrays.asList("serialVersionUID", "showProSize", "proSize", "channel", "singleTireWeight"));
                sumAllField(produceSalePlanResultVo, proSizeTotalSumVo, Arrays.asList("serialVersionUID", "showProSize", "proSize", "channel", "singleTireWeight", "monthProduceQtyProportion", "monthProduceWeightProportion", "monthSaleQtyProportion", "monthSaleWeightProportion"));
            }
            proSizeSumMap.put(entry.getKey(), proSizeSumVo);
        }
        List<ProduceSalePlanResultVo> proSizeSumList = new ArrayList<>();
        for (Map.Entry<BigDecimal, ProduceSalePlanResultVo> entry : proSizeSumMap.entrySet()) {
            ProduceSalePlanResultVo value = entry.getValue();
            value.calculateProportion(proSizeTotalSumVo);
            proSizeSumList.add(value);
        }
        proSizeSumList.sort(Comparator.comparing(ProduceSalePlanResultVo::getProSize));
        resultVoList.addAll(proSizeSumList);
        /*for (Map.Entry<BigDecimal, List<ProduceSalePlanResultVo>> entry : proSizeEntrySet) {
            BigDecimal proSize = entry.getKey();
            List<ProduceSalePlanResultVo> value = entry.getValue();

            ProduceSalePlanResultVo proSizeSumVo = proSizeSumMap.get(proSize);


            for (ProduceSalePlanResultVo produceSalePlanResultVo : value) {
                produceSalePlanResultVo.calculateProportion(proSizeSumVo);
                resultVoList.add(proSizeSumVo);
            }
        }*/
        proSizeTotalSumVo.setShowProSize("合计");
        proSizeTotalSumVo.setChannelName("合计");
        resultVoList.add(proSizeTotalSumVo);
    }

    /**
     * 获取上个月的查询参数
     *
     * @param queryDto 结果
     * @return 结果
     */
    private BaseReportDto getLastMonthQueryDto(BaseReportDto queryDto) {
        Integer year = queryDto.getYear();
        Integer month = queryDto.getMonth();
        if (month == 1) {
            year = year - 1;
            month = 12;
        } else {
            month = month - 1;
        }
        BaseReportDto lastMonthQueryDto = new BaseReportDto();
        lastMonthQueryDto.setYear(year);
        lastMonthQueryDto.setMonth(month);
        return lastMonthQueryDto;
    }

    /**
     * 查询生产销售计划数据
     *
     * @param queryDto 查询参数
     * @return 结果
     */
    @Override
    public List<ProduceSalePlanResultVo> selectProduceSalePlanList(BaseReportDto queryDto) {
        List<ProduceSalePlanResultVo> resultVoList = new ArrayList<>();
        String monthPlanVersion = monthPlanReportMapper.selectFinalMonthPlanVersion(queryDto);
        queryDto.setMonthPlanVersion(monthPlanVersion);
        List<ProduceSalePlanResultVo> baseList = monthPlanReportMapper.selectSalePlanProSizeLocationTypeWeight(queryDto);
        if (CollectionUtils.isEmpty(baseList)) {
            return Collections.emptyList();
        }

        // 赋值月生产量和月生产重量
        this.setMonthProduceQty(queryDto, baseList);

        // 添加渠道合计数据
        this.addChannelTotalVo(baseList, resultVoList);

        // 添加寸口合计数据
        addProSizeTotalVo(baseList, resultVoList);

        return resultVoList;
    }

    /**
     * 导出生产销售计划数据
     *
     * @param queryDto 查询参数
     * @return 结果
     */
    @Override
    public byte[] exportProduceSalePlan(BaseReportDto queryDto) {
        // 获取模板
        ClassLoader classLoader = this.getClass().getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream("excelModel/produceSalePlan.xlsx");
        // 表头信息
        Map<String, Object> tableMap = new HashMap<>(16);
        // 列表数据
        List<List<Map<String, Object>>> excelDataList = new ArrayList<>();
        List<ProduceSalePlanResultVo> voList = this.selectProduceSalePlanList(queryDto);
        if (CollectionUtils.isNotEmpty(voList)) {
            List<Map<String, Object>> list = new ArrayList<>();
            LinkedHashMap<String, List<ProduceSalePlanResultVo>> channelGroupMap = voList.stream().collect(Collectors.groupingBy(ProduceSalePlanResultVo::getChannelName, LinkedHashMap::new, Collectors.toList()));

            List<ExcelCellRangeAddress> rangeAddressList = new ArrayList<>();

            int startMergeRowNum = 1;
            int endMergeRowNum = 0;

            Set<Map.Entry<String, List<ProduceSalePlanResultVo>>> entrySet = channelGroupMap.entrySet();
            for (Map.Entry<String, List<ProduceSalePlanResultVo>> entry : entrySet) {

                List<ProduceSalePlanResultVo> value = entry.getValue();
                for (ProduceSalePlanResultVo vo : value) {
                    Map<String, Object> listDataMap = new HashMap<>(16);
                    // 添加数据
                    putFieldToMap(vo, listDataMap,
                            Collections.singletonList("serialVersionUID"),
                            StringUtils.EMPTY, Boolean.FALSE);
                    list.add(listDataMap);
                    excelDataList.add(list);
                    endMergeRowNum++;
                }

                // 合并单元格
                ExcelCellRangeAddress address2 = new ExcelCellRangeAddress(startMergeRowNum, endMergeRowNum, 0, 0);
                rangeAddressList.add(address2);

                startMergeRowNum = endMergeRowNum + 1;
                endMergeRowNum = startMergeRowNum - 1;
            }
            tableMap.put(ExcelUtils.RANGE_ADDRESS, rangeAddressList);
        }
        // 写到文件
        return ExcelUtils.writeMultiList(inputStream
                , 0, tableMap, excelDataList);
    }

    /**
     * 添加渠道合计数据
     *
     * @param baseList     基础数据列表
     * @param resultVoList 结果列表
     */
    private void addChannelTotalVo(List<ProduceSalePlanResultVo> baseList, List<ProduceSalePlanResultVo> resultVoList) {
        // 渠道字典
        Map<String, String> dictMapByType = getDictMapByType(CHANNEL_DICT_TYPE);

        // 渠道分组
        Map<String, List<ProduceSalePlanResultVo>> channelGroupMap = baseList.stream().collect(Collectors.groupingBy(ProduceSalePlanResultVo::getChannel, LinkedHashMap::new, Collectors.toList()));
        Set<Map.Entry<String, List<ProduceSalePlanResultVo>>> channelEntrySet = channelGroupMap.entrySet();

        Map<String, ProduceSalePlanResultVo> channelSumMap = new HashMap<>(16);
        // 计算汇总数据
        for (Map.Entry<String, List<ProduceSalePlanResultVo>> entry : channelEntrySet) {
            List<ProduceSalePlanResultVo> value = entry.getValue();
            ProduceSalePlanResultVo channelSumVo = new ProduceSalePlanResultVo();
            for (ProduceSalePlanResultVo produceSalePlanResultVo : value) {
                sumAllField(produceSalePlanResultVo, channelSumVo, Arrays.asList("serialVersionUID", "showProSize", "proSize", "channel", "singleTireWeight"));
                String channel = produceSalePlanResultVo.getChannel();
                if (dictMapByType.containsKey(channel)) {
                    String dictLabel = dictMapByType.get(channel);
                    produceSalePlanResultVo.setChannelName(dictLabel);
                    channelSumVo.setChannel(channel);
                    channelSumVo.setChannelName(dictLabel);
                }
            }
            channelSumMap.put(channelSumVo.getChannel(), channelSumVo);
        }
        // 计算占比
        for (Map.Entry<String, List<ProduceSalePlanResultVo>> entry : channelEntrySet) {
            String key = entry.getKey();
            List<ProduceSalePlanResultVo> value = entry.getValue();

            ProduceSalePlanResultVo channelSumVo = channelSumMap.get(key);

            for (ProduceSalePlanResultVo produceSalePlanResultVo : value) {
                produceSalePlanResultVo.setShowProSize(produceSalePlanResultVo.getProSize().toString());
                produceSalePlanResultVo.calculateProportion(channelSumVo);
                resultVoList.add(produceSalePlanResultVo);
            }
            channelSumVo.setShowProSize("合计");
            resultVoList.add(channelSumVo);
        }
    }

    /**
     * 赋值月度生产销售计划数据
     *
     * @param queryDto 查询条件
     * @param baseList 基础数据列表
     */
    private void setMonthProduceQty(BaseReportDto queryDto, List<ProduceSalePlanResultVo> baseList) {
        List<ProduceSalePlanVo> thisMonthProPlanList = monthPlanReportMapper.selectProPlanProSizeLocationTypeWeight(queryDto);
        Map<String, List<ProduceSalePlanVo>> thisMonthProPlanMap = new HashMap<>(16);
        if (CollectionUtils.isNotEmpty(thisMonthProPlanList)) {
            thisMonthProPlanMap = thisMonthProPlanList.stream().collect(Collectors.groupingBy(item -> String.join("|", item.getProSize().toString(), item.getChannel()), LinkedHashMap::new, Collectors.toList()));
        }
        Map<String, ProduceSalePlanResultVo> groupMap = baseList.stream().collect(Collectors.toMap(item -> String.join("|", item.getProSize().toString(), item.getChannel()), Function.identity(), (s1, s2) -> s1, LinkedHashMap::new));
        Set<Map.Entry<String, ProduceSalePlanResultVo>> entrySet = groupMap.entrySet();
        for (Map.Entry<String, ProduceSalePlanResultVo> entry : entrySet) {
            String key = entry.getKey();
            ProduceSalePlanResultVo resultVo = entry.getValue();
            BigDecimal singleTireWeight = resultVo.getSingleTireWeight();
            if (thisMonthProPlanMap.containsKey(key)) {
                List<ProduceSalePlanVo> produceSalePlanVos = thisMonthProPlanMap.get(key);
                for (ProduceSalePlanVo produceSalePlanVo : produceSalePlanVos) {
                    for (int i = THIS_MONTH_START_DAY; i <= LAST_MONTH_END_DAY; i++) {
                        BigDecimal fieldValue = ObjectUtils.defaultIfNull(ReflectUtils.getFieldValue(produceSalePlanVo, "day" + i), BigDecimal.ZERO);
                        BigDecimal result = Optional.ofNullable(resultVo.getMonthProduceQty()).orElse(BigDecimal.ZERO);
                        BigDecimal monthProduceQty = BigDecimalUtils.add(result, fieldValue);
                        resultVo.setMonthProduceQty(monthProduceQty);
                        resultVo.setMonthProduceWeight(BigDecimalUtils.multiply(monthProduceQty, singleTireWeight));
                    }
                }
            }
        }
    }

    /**
     * 赋值月度生产销售计划数据
     *
     * @param queryDto 查询条件
     * @param baseList 基础数据列表
     */
    private void setMonthProduceQty4AccountingPeriod(BaseReportDto queryDto, List<ProduceSalePlanResultVo> baseList) {
        List<ProduceSalePlanVo> thisMonthProPlanList = monthPlanReportMapper.selectProPlanProSizeLocationTypeWeight(queryDto);
        List<ProduceSalePlanVo> lastMonthProPlanList = monthPlanReportMapper.selectProPlanProSizeLocationTypeWeight(getLastMonthQueryDto(queryDto));
        Map<String, List<ProduceSalePlanVo>> thisMonthProPlanMap = new HashMap<>(16);
        if (CollectionUtils.isNotEmpty(thisMonthProPlanList)) {
            thisMonthProPlanMap = thisMonthProPlanList.stream().collect(Collectors.groupingBy(item -> String.join("|", item.getProSize().toString(), item.getChannel()), LinkedHashMap::new, Collectors.toList()));
        }
        Map<String, List<ProduceSalePlanVo>> lastMonthProPlanMap = new HashMap<>(16);
        if (CollectionUtils.isNotEmpty(lastMonthProPlanList)) {
            lastMonthProPlanMap = lastMonthProPlanList.stream().collect(Collectors.groupingBy(item -> String.join("|", item.getProSize().toString(), item.getChannel()), LinkedHashMap::new, Collectors.toList()));
        }
        Map<String, ProduceSalePlanResultVo> groupMap = baseList.stream().collect(Collectors.toMap(item -> String.join("|", item.getProSize().toString(), item.getChannel()), Function.identity(), (s1, s2) -> s1, LinkedHashMap::new));
        Set<Map.Entry<String, ProduceSalePlanResultVo>> entrySet = groupMap.entrySet();
        for (Map.Entry<String, ProduceSalePlanResultVo> entry : entrySet) {
            String key = entry.getKey();
            ProduceSalePlanResultVo resultVo = entry.getValue();
            BigDecimal singleTireWeight = resultVo.getSingleTireWeight();
            if (thisMonthProPlanMap.containsKey(key)) {
                List<ProduceSalePlanVo> produceSalePlanVos = thisMonthProPlanMap.get(key);
                for (ProduceSalePlanVo produceSalePlanVo : produceSalePlanVos) {
                    for (int i = THIS_MONTH_START_DAY; i <= THIS_MONTH_END_DAY; i++) {
                        BigDecimal fieldValue = ObjectUtils.defaultIfNull(ReflectUtils.getFieldValue(produceSalePlanVo, "day" + i), BigDecimal.ZERO);
                        BigDecimal result = Optional.ofNullable(resultVo.getMonthProduceQty()).orElse(BigDecimal.ZERO);
                        BigDecimal monthProduceQty = BigDecimalUtils.add(result, fieldValue);
                        resultVo.setMonthProduceQty(monthProduceQty);
                        resultVo.setMonthProduceWeight(BigDecimalUtils.multiply(monthProduceQty, singleTireWeight));
                    }
                }
            }
            if (lastMonthProPlanMap.containsKey(key)) {
                List<ProduceSalePlanVo> produceSalePlanVos = lastMonthProPlanMap.get(key);
                for (ProduceSalePlanVo produceSalePlanVo : produceSalePlanVos) {
                    for (int i = LAST_MONTH_START_DAY; i <= LAST_MONTH_END_DAY; i++) {
                        BigDecimal fieldValue = ObjectUtils.defaultIfNull(ReflectUtils.getFieldValue(produceSalePlanVo, "day" + i), BigDecimal.ZERO);
                        BigDecimal result = Optional.ofNullable(resultVo.getMonthProduceQty()).orElse(BigDecimal.ZERO);
                        BigDecimal monthProduceQty = BigDecimalUtils.add(result, fieldValue);
                        resultVo.setMonthProduceQty(monthProduceQty);
                        resultVo.setMonthProduceWeight(BigDecimalUtils.multiply(monthProduceQty, singleTireWeight));
                    }
                }
            }
        }
    }

    @Autowired
    private MdmMaterialInfoEntityMapper productInfoEntityMapper;

    /**
     * 查询排产版本列表数据
     *
     * @param queryDto 查询参数
     * @return 结果
     */
    @Override
    public List<ProductVersionReportVo> listProduceVersionList(ProduceVersionDto queryDto) {
        long timeMillis1 = System.currentTimeMillis();
        // 查询版本对应年月、分厂
        if (ApsConstant.DEL_FLAG_DEL.equals(queryDto.getQueryType())) {
            String productVersion = monthPlanReportMapper.selectProductVersion(queryDto);
            queryDto.setMonthPlanVersion(productVersion);
        }
        ProduceVersionDto baseReportDto = monthPlanReportMapper.selectByMonthPlanVersion(queryDto);
        baseReportDto.setProductCode(queryDto.getProductCode());
        baseReportDto.setProductDesc(queryDto.getProductDesc());
        baseReportDto.setBrand(queryDto.getBrand());
        baseReportDto.setProSize(queryDto.getProSize());
        baseReportDto.setPattern(queryDto.getPattern());
        long timeMillis2 = System.currentTimeMillis();
        log.info("版本对应年月、分厂查询耗时：{}", timeMillis2 - timeMillis1);
        // 先查询理论备货量，查询的数据当主表
        List<ProductVersionReportVo> stockUpPlanQtyList = monthPlanReportMapper.selectStockUpPlanQtyList(baseReportDto);
        if (CollectionUtils.isEmpty(stockUpPlanQtyList)) {
            return Collections.emptyList();
        }
        long timeMillis3 = System.currentTimeMillis();
        log.info("理论备货查询耗时：{}", timeMillis3 - timeMillis2);
        // 查询对应物料信息
        List<String> productCodeList = stockUpPlanQtyList.stream().map(ProductVersionReportVo::getProductCode).collect(Collectors.toList());
        List<MdmMaterialInfo> productInfoList = productInfoEntityMapper.queryByFactoryCodeAndProductCodes(FactoryConstant.DEFAULT_FACTORY_CODE, productCodeList);
        Map<String, MdmMaterialInfo> productInfoMap = new HashMap<>(16);
        if (CollectionUtils.isNotEmpty(productInfoList)) {
            productInfoMap = productInfoList.stream().collect(Collectors.toMap(MdmMaterialInfo::getMaterialCode, Function.identity(), (v1, v2) -> v1));
        }
        long timeMillis4 = System.currentTimeMillis();
        log.info("物料信息查询耗时：{}", timeMillis4 - timeMillis3);
        // 查询施工信息
        /*List<CxProductConstructionInfo> productConstructionInfoList = monthPlanReportMapper.selectProductConstructionInfo4RequirePlan(baseReportDto);
        Map<String, CxProductConstructionInfo> productConstructionInfoMap = new HashMap<>(16);
        if (CollectionUtils.isNotEmpty(productConstructionInfoList)) {
            productConstructionInfoMap = productConstructionInfoList.stream().collect(Collectors.toMap(CxProductConstructionInfo::getSapCode, Function.identity(), (v1, v2) -> v1));
        }*/
        // 查询SAP与施工关系
        List<MdmProductConstruction> productConstructionList = monthPlanReportMapper.selectProductConstruction4RequirePlan(baseReportDto);
        Map<String, MdmProductConstruction> productConstructionMap = new HashMap<>(16);
        if (CollectionUtils.isNotEmpty(productConstructionList)) {
            productConstructionMap = productConstructionList.stream().collect(Collectors.toMap(MdmProductConstruction::getProductCode, Function.identity(), (v1, v2) -> v1));
        }
        long timeMillis5 = System.currentTimeMillis();
        log.info("理论备货量对应施工查询耗时：{}", timeMillis5 - timeMillis4);
        // 查询需求量、排产量、库存量、实际备货量、未排产量。赋值给理论备货集合的指定属性
        List<ProductVersionReportVo> demandQtyList = monthPlanReportMapper.selectDemandQtyList(baseReportDto);
        Map<String, List<ProductVersionReportVo>> demandQtyMap = new HashMap<>(16);
        if (CollectionUtils.isNotEmpty(demandQtyList)) {
            demandQtyMap = demandQtyList.stream().collect(Collectors.groupingBy(ProductVersionReportVo::getProductCode));
        }
        long timeMillis6 = System.currentTimeMillis();
        log.info("需求量查询耗时：{}", timeMillis6 - timeMillis5);
        List<ProductVersionReportVo> productQtyList;
        if (ApsConstant.DEL_FLAG_DEL.equals(queryDto.getQueryType())) {
            productQtyList = monthPlanReportMapper.selectProductQtyList4FinalDetail(baseReportDto);
        } else {
            productQtyList = monthPlanReportMapper.selectProductQtyList(baseReportDto);
        }
        Map<String, ProductVersionReportVo> productQtyMap = new HashMap<>(16);
        if (CollectionUtils.isNotEmpty(productQtyList)) {
            productQtyMap = productQtyList.stream().collect(Collectors.toMap(ProductVersionReportVo::getProductCode, Function.identity(), (s1, s2) -> s1));
        }
        long timeMillis7 = System.currentTimeMillis();
        log.info("排产量查询耗时：{}", timeMillis7 - timeMillis6);
        List<ProductVersionReportVo> stockQtyList = monthPlanReportMapper.selectStockQtyList(baseReportDto);
        Map<String, ProductVersionReportVo> stockQtyMap = new HashMap<>(16);
        if (CollectionUtils.isNotEmpty(stockQtyList)) {
            stockQtyMap = stockQtyList.stream().collect(Collectors.toMap(ProductVersionReportVo::getProductCode, Function.identity(), (s1, s2) -> s1));
        }
        long timeMillis8 = System.currentTimeMillis();
        log.info("库存量查询耗时：{}", timeMillis8 - timeMillis7);
        List<ProductVersionReportVo> stockUpActQtyList = monthPlanReportMapper.selectStockUpActQtyList(baseReportDto);
        Map<String, ProductVersionReportVo> stockUpActQtyMap = new HashMap<>(16);
        if (CollectionUtils.isNotEmpty(stockUpActQtyList)) {
            stockUpActQtyMap = stockUpActQtyList.stream().collect(Collectors.toMap(ProductVersionReportVo::getProductCode, Function.identity(), (s1, s2) -> s1));
        }
        long timeMillis9 = System.currentTimeMillis();
        log.info("实际备货量查询耗时：{}", timeMillis9 - timeMillis8);
        List<ProductVersionReportVo> unProductQtyList = monthPlanReportMapper.selectUnProductQtyList(baseReportDto);
        Map<String, ProductVersionReportVo> unProductQtyMap = new HashMap<>(16);
        if (CollectionUtils.isNotEmpty(unProductQtyList)) {
            unProductQtyMap = unProductQtyList.stream().collect(Collectors.toMap(ProductVersionReportVo::getProductCode, Function.identity(), (s1, s2) -> s1));
        }
        long timeMillis10 = System.currentTimeMillis();
        log.info("未排量查询耗时：{}", timeMillis10 - timeMillis9);

        log.info("查询结束，数据处理start");
        for (ProductVersionReportVo reportVo : stockUpPlanQtyList) {
            String productCode = reportVo.getProductCode();
            if (productInfoMap.containsKey(productCode)) {
                MdmMaterialInfo productInfo = productInfoMap.get(productCode);
                reportVo.setProductDesc(productInfo.getMaterialDesc());
                // reportVo.setProSize(productInfo.getProSize().toString());
                reportVo.setBrand(productInfo.getBrand());
                reportVo.setPattern(productInfo.getPattern());
            }
            /*if (productConstructionInfoMap.containsKey(productCode)) {
                CxProductConstructionInfo productConstructionInfo = productConstructionInfoMap.get(productCode);
                reportVo.setConstructionStage(productConstructionInfo.getProductionStage());
            }*/
            if (productConstructionMap.containsKey(productCode)) {
                MdmProductConstruction mdmProductConstruction = productConstructionMap.get(productCode);
                String constructionCode = mdmProductConstruction.getConstructionCode();
                reportVo.setSpecCode(mdmProductConstruction.getSpecCode());
                reportVo.setConstructionCode(constructionCode);
                reportVo.setEmbryoCode(mdmProductConstruction.getEmbryoCode());
                if (StringUtils.isNotBlank(constructionCode)) {
                    ConstructionStageEnum constructionStageEnum = ConstructionStageEnum.matchByConstructionCode(constructionCode);
                    if (constructionStageEnum != null) {
                        reportVo.setConstructionStage(constructionStageEnum.getStage().toString());
                    }
                }
            }
            List<ProductVersionReportVo> demandQty = demandQtyMap.get(productCode);
            if (CollectionUtils.isNotEmpty(demandQty)) {
                for (ProductVersionReportVo productVersionReportVo : demandQty) {
                    String channel = productVersionReportVo.getChannel();
                    if ("01".equals(channel)) {
                        Integer oeDemandQty = ObjectUtils.defaultIfNull(productVersionReportVo.getOeDemandQty(), 0);
                        reportVo.setOeDemandQty(oeDemandQty);
                    } else if ("05".equals(channel)) {
                        Integer outDemandQty = ObjectUtils.defaultIfNull(productVersionReportVo.getOutDemandQty(), 0);
                        reportVo.setOutDemandQty(outDemandQty);
                    } else {
                        Integer inDemandQty = ObjectUtils.defaultIfNull(productVersionReportVo.getInDemandQty(), 0);
                        ReflectUtils.setFieldValue(reportVo, "inDemandQty_" + channel, inDemandQty);
                    }
                }
            }
            ProductVersionReportVo productQty = productQtyMap.get(productCode);
            if (productQty != null) {
                Integer inProductQty = ObjectUtils.defaultIfNull(productQty.getInProductQty(), 0);
                reportVo.setInProductQty(inProductQty);
                Integer outProductQty = ObjectUtils.defaultIfNull(productQty.getOutProductQty(), 0);
                reportVo.setOutProductQty(outProductQty);
                Integer oeProductQty = ObjectUtils.defaultIfNull(productQty.getOeProductQty(), 0);
                reportVo.setOeProductQty(oeProductQty);
                reportVo.setSumProductQty(inProductQty + outProductQty + oeProductQty);

                String mouldNo = productQty.getMouldNo();
                reportVo.setMouldNo(mouldNo);
                Integer mouldQty = productQty.getMouldQty();
                reportVo.setMouldQty(mouldQty);
            }
            ProductVersionReportVo stockQty = stockQtyMap.get(productCode);
            if (stockQty != null) {
                Integer inStockQty = ObjectUtils.defaultIfNull(stockQty.getInStockQty(), 0);
                reportVo.setInStockQty(inStockQty);
                Integer outStockQty = ObjectUtils.defaultIfNull(stockQty.getOutStockQty(), 0);
                reportVo.setOutStockQty(outStockQty);
                Integer oeStockQty = ObjectUtils.defaultIfNull(stockQty.getOeStockQty(), 0);
                reportVo.setOeStockQty(oeStockQty);
                reportVo.setSumStockQty(inStockQty + outStockQty + oeStockQty);
            }
            ProductVersionReportVo stockUpActQty = stockUpActQtyMap.get(productCode);
            if (stockUpActQty != null) {
                Integer inStockUpActQty = ObjectUtils.defaultIfNull(stockUpActQty.getInStockUpActQty(), 0);
                reportVo.setInStockUpActQty(inStockUpActQty);
                Integer outStockUpActQty = ObjectUtils.defaultIfNull(stockUpActQty.getOutStockUpActQty(), 0);
                reportVo.setOutStockUpActQty(outStockUpActQty);
                Integer oeStockUpActQty = ObjectUtils.defaultIfNull(stockUpActQty.getOeStockUpActQty(), 0);
                reportVo.setOeStockUpActQty(oeStockUpActQty);
                reportVo.setSumStockUpActQty(inStockUpActQty + outStockUpActQty + oeStockUpActQty);
            }
            ProductVersionReportVo unProductQty = unProductQtyMap.get(productCode);
            if (unProductQty != null) {
                Integer inUnProductQty = ObjectUtils.defaultIfNull(unProductQty.getInUnProductQty(), 0);
                reportVo.setInUnProductQty(inUnProductQty);
                Integer outUnProductQty = ObjectUtils.defaultIfNull(unProductQty.getOutUnProductQty(), 0);
                reportVo.setOutUnProductQty(outUnProductQty);
                Integer oeUnProductQty = ObjectUtils.defaultIfNull(unProductQty.getOeUnProductQty(), 0);
                reportVo.setOeUnProductQty(oeUnProductQty);
                reportVo.setSumUnProductQty(inUnProductQty + outUnProductQty + oeUnProductQty);
            }
            // 计算缺口
            Integer outDemandQty = reportVo.getOutDemandQty();
            Integer outStockQty = reportVo.getOutStockQty();
            Integer outProductQty = reportVo.getOutProductQty();
            int outStockGapQty = outDemandQty - outStockQty;
            reportVo.setOutStockGapQty(outStockGapQty);
            int outPlanStockGapQty = outStockGapQty - outProductQty;
            reportVo.setOutPlanStockGapQty(outPlanStockGapQty);

            // 计算内销需求量
            reportVo.calculateInDemandQty();
            Integer inDemandQty = reportVo.getInDemandQty();
            Integer inStockQty = reportVo.getInStockQty();
            Integer inProductQty = reportVo.getInProductQty();
            int inStockGapQty = inDemandQty - inStockQty;
            reportVo.setInStockGapQty(inStockGapQty);
            int inPlanStockGapQty = inStockGapQty - inProductQty;
            reportVo.setInPlanStockGapQty(inPlanStockGapQty);

            reportVo.setSumStockGapQty(outStockGapQty + inStockGapQty);
            reportVo.setSumPlanStockGapQty(outPlanStockGapQty + inPlanStockGapQty);
        }
        long timeMillis11 = System.currentTimeMillis();
        log.info("查询结束，数据处理end，耗时：{}", timeMillis11 - timeMillis10);

        return stockUpPlanQtyList;
    }

    @Autowired
    private DemoYearPlanFinishEntityMapper demoYearPlanFinishEntityMapper;

    @Autowired
    private ISysConfigService iSysConfigService;

    /**
     * 查询首页计划数据
     *
     * @param date 日期
     * @return 结果
     */
    @Override
    public HomePage4PlanVo homePage4Plan(Date date) {
        HomePage4PlanVo resultVo = new HomePage4PlanVo();
        Integer year = DateUtils.getYear(date);
        Integer month = DateUtils.getMonthsByYear(date);
        List<HomePage4PlanVo> yearMonthPlanQtyList = monthPlanReportMapper.selectYearMonthPlanQty(year);
        Double yearTotalPlanQty = 0D;
        for (HomePage4PlanVo yearMonthPlanVo : yearMonthPlanQtyList) {
            Double monthTotalPlanQty = yearMonthPlanVo.getMonthTotalPlanQty();
            Double monthTotalSpecQty = yearMonthPlanVo.getMonthTotalSpecQty();
            if (Objects.equals(yearMonthPlanVo.getMonth(), month)) {
                resultVo.setMonthTotalPlanQty(monthTotalPlanQty);
                resultVo.setMonthTotalSpecQty(monthTotalSpecQty);
            }
            yearTotalPlanQty += monthTotalPlanQty;
        }
        resultVo.setYearTotalPlanQty(yearTotalPlanQty);
        Double yearTotalFinishQty = 0D;
        List<HomePage4PlanVo> yearMonthFinishQtyList = monthPlanReportMapper.selectYearMonthFinishQty(year);
        for (HomePage4PlanVo yearMonthFinishVo : yearMonthFinishQtyList) {
            Double monthTotalFinishQty = yearMonthFinishVo.getMonthTotalFinishQty();
            Double monthTotalSpecFinishQty = yearMonthFinishVo.getMonthTotalSpecFinishQty();
            if (Objects.equals(yearMonthFinishVo.getMonth(), month)) {
                resultVo.setMonthTotalFinishQty(monthTotalFinishQty);
                resultVo.setMonthTotalSpecFinishQty(monthTotalSpecFinishQty);
            }
            yearTotalFinishQty += monthTotalFinishQty;
        }
        resultVo.setYearTotalFinishQty(yearTotalFinishQty);
        Double monthTotalPlanQty = resultVo.getMonthTotalPlanQty();
        if (monthTotalPlanQty != 0) {
            resultVo.setPlanFinishRate(resultVo.getMonthTotalFinishQty() / monthTotalPlanQty);
        }
        Double monthTotalSpecQty = resultVo.getMonthTotalSpecQty();
        if (monthTotalSpecQty != 0) {
            resultVo.setMonthSpecFinishRate(resultVo.getMonthTotalSpecFinishQty() / monthTotalSpecQty);
        }

        String configValue = iSysConfigService.selectConfigByKey("report.show.flag");
        if (ApsConstant.APS_STRING_1.equals(configValue)) {
            LambdaQueryWrapper<DemoYearPlanFinish> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(DemoYearPlanFinish::getYear, year);
            List<DemoYearPlanFinish> demoYearPlanFinishList = demoYearPlanFinishEntityMapper.selectList(wrapper);
            Integer totalPlan = 0;
            Integer totalFinish = 0;
            for (DemoYearPlanFinish demoYearPlanFinish : demoYearPlanFinishList) {
                Integer planQty = demoYearPlanFinish.getPlanQty() == null ? 0 : demoYearPlanFinish.getPlanQty();
                Integer finishQty = demoYearPlanFinish.getFinishQty() == null ? 0 : demoYearPlanFinish.getFinishQty();
                totalPlan += planQty;
                totalFinish += finishQty;
            }
            resultVo.setYearTotalPlanQty(totalPlan.doubleValue());
            resultVo.setYearTotalFinishQty(totalFinish.doubleValue());
        }

        return resultVo;
    }

    @Autowired
    private DemoOrderAcceptEntityMapper demoOrderAcceptEntityMapper;

    /**
     * 查询首页订单接单情况
     *
     * @param date 日期
     * @return 结果
     */
    @Override
    public List<HomePage4OrderVo> homePage4Order(Date date) {
        Integer year = DateUtils.getYear(date);
        List<HomePage4OrderVo> orderAndStockUpQtyList = monthPlanReportMapper.selectOrderAndStockUpQty(year);
        String configValue = iSysConfigService.selectConfigByKey("report.show.order.flag");
        if (ApsConstant.APS_STRING_1.equals(configValue)) {
            LambdaQueryWrapper<DemoOrderAccept> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(DemoOrderAccept::getYear, year);
            List<DemoOrderAccept> demoOrderAcceptList = demoOrderAcceptEntityMapper.selectList(wrapper);
            Map<Integer, DemoOrderAccept> acceptMap = demoOrderAcceptList.stream().collect(Collectors.toMap(DemoOrderAccept::getMonth, Function.identity(), (v1, v2) -> v1));
            for (HomePage4OrderVo homePage4OrderVo : orderAndStockUpQtyList) {
                Integer month = homePage4OrderVo.getMonth();
                if (acceptMap.containsKey(month)) {
                    DemoOrderAccept demoOrderAccept = acceptMap.get(month);
                    homePage4OrderVo.setPlanOrderQty(demoOrderAccept.getPlanOrderQty() == null ? 0D : demoOrderAccept.getPlanOrderQty().doubleValue());
                    homePage4OrderVo.setStockUpQty(demoOrderAccept.getStockUpQty() == null ? 0D : demoOrderAccept.getStockUpQty().doubleValue());
                    homePage4OrderVo.setOrderQty(demoOrderAccept.getPlanDemandQty() == null ? 0D : demoOrderAccept.getPlanDemandQty().doubleValue());
                    homePage4OrderVo.setPlanQty(demoOrderAccept.getActualPlanQty() == null ? 0D : demoOrderAccept.getActualPlanQty().doubleValue());
                }
            }
        }
        return orderAndStockUpQtyList;
    }

    /**
     * 查询首页工序完成情况
     *
     * @param date 日期
     * @return 结果
     */
    @Override
    public List<HomePage4ProductProcessesVo> homePage4ProductionProcesses(Date date) {
        String dateToStr = com.ruoyi.common.core.utils.DateUtils.parseDateToStr(com.ruoyi.common.core.utils.DateUtils.YYYY_MM_DD, date);
        List<HomePage4ProductProcessesVo> processesVoList = monthPlanReportMapper.selectProductionProcesses(dateToStr);
        for (HomePage4ProductProcessesVo productProcessesVo : processesVoList) {
            Double planQty = productProcessesVo.getPlanQty();
            Double finishQty = productProcessesVo.getFinishQty();
            if (planQty != 0) {
                productProcessesVo.setFinishRate(finishQty / planQty);
            }
        }
        return processesVoList;
    }

    /**
     * 查询首页工厂设备情况
     *
     * @param date 日期
     * @return 结果
     */
    @Override
    public List<HomePage4MachineVo> homePage4Machine(Date date) {
        return monthPlanReportMapper.selectFactoryMachine();
    }

    /**
     * 查询今年生产情况
     *
     * @param date 日期
     * @return 结果
     */
    @Override
    public List<HomePage4PlanVo> homePage4YearProduct(Date date) {
        Integer year = DateUtils.getYear(date);
        return monthPlanReportMapper.selectYearMonthFinishQty(year);
    }

    /**
     * 查询首页工序完成情况-7天
     *
     * @param date 日期
     * @return 结果
     */
    @Override
    public List<HomePage4ProductProcessesVo> selectProductionProcessesByDate7(Date date) {
        String dateToStr = com.ruoyi.common.core.utils.DateUtils.parseDateToStr(com.ruoyi.common.core.utils.DateUtils.YYYY_MM_DD, date);
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        // 减7天
        calendar.add(Calendar.DAY_OF_MONTH, -7);
        String dateToStrBefore7Day = com.ruoyi.common.core.utils.DateUtils.parseDateToStr(com.ruoyi.common.core.utils.DateUtils.YYYY_MM_DD, calendar.getTime());
        List<HomePage4ProductProcessesVo> processesVoList = monthPlanReportMapper.selectProductionProcessesByDate7(dateToStr, dateToStrBefore7Day);
        for (HomePage4ProductProcessesVo productProcessesVo : processesVoList) {
            Double planQty = productProcessesVo.getPlanQty();
            Double finishQty = productProcessesVo.getFinishQty();
            if (planQty != 0) {
                productProcessesVo.setFinishRate(finishQty / planQty);
            }
        }
        return processesVoList;
    }

    /**
     * 查询成型、硫化机台可用、停机情况
     *
     * @param date 日期
     * @return 结果
     */
    @Override
    public List<CxLhMachineVo> selectCxLhMachine(Date date) {
        Integer year = DateUtils.getYear(date);
        Integer month = DateUtils.getMonthsByYear(date);
        List<CxLhMachineVo> cxLhMachineVoList = monthPlanReportMapper.selectCxLhMachine(year, month);
        Map<Integer, CxLhMachineVo> cxLhMachineVoMap = cxLhMachineVoList.stream().collect(Collectors.toMap(CxLhMachineVo::getMachineType, Function.identity()));
        for (MdmMachineTypeEnum value : MdmMachineTypeEnum.values()) {
            if (value.getValue() == 99) {
                continue;
            }
            if (!cxLhMachineVoMap.containsKey(value.getValue())) {
                CxLhMachineVo cxLhMachineVo = new CxLhMachineVo();
                cxLhMachineVo.setMachineType(value.getValue());
                switch (value.getValue()) {
                    case 0:
                        cxLhMachineVo.setMachineTypeName("成型维修机台总时数");
                        break;
                    case 1:
                        cxLhMachineVo.setMachineTypeName("硫化维修机台总时数");
                        break;
                    case 2:
                        cxLhMachineVo.setMachineTypeName("模具维修机台总时数");
                        break;
                    case 3:
                        cxLhMachineVo.setMachineTypeName("洗模机台总时数");
                        break;
                    default:
                        break;
                }
                cxLhMachineVo.setQty(0D);
                cxLhMachineVoList.add(cxLhMachineVo);
            }
        }
        cxLhMachineVoList.sort(Comparator.comparing(CxLhMachineVo::getMachineType));
        return cxLhMachineVoList;
    }

    /**
     * 查询sku汇总分析-大屏
     *
     * @param queryDto 查询参数
     * @return 结果
     */
    @Override
    public SkuMonthQtyVo selectSkuSummary4BigScreen(MonthPlanReportDto queryDto) {
        // SKU汇总分析，过滤出前3个月的数据
        List<ProduceSkuSummaryVo> list = getProduceSkuSummaryVoList(queryDto);
        SkuMonthQtyVo skuMonthQtyVo = calculateSkuSummaryListGenSkuMonthQtyVo(queryDto, list, Boolean.TRUE);
        // 查询投产SKU汇总
        List<SkuSummaryProduceVo> skuSummaryProduceVoList = monthPlanReportMapper.selectSkuSummaryProduce(queryDto);
        // 添加年累计、H1、环比
        calculateSkuSummaryProduceList(queryDto, skuSummaryProduceVoList, Boolean.TRUE);
        Map<Integer, SkuSummaryProduceVo> map = new HashMap<>(16);
        if (CollectionUtils.isNotEmpty(skuSummaryProduceVoList)) {
            map = skuSummaryProduceVoList.stream().collect(Collectors.toMap(SkuSummaryProduceVo::getMonth, Function.identity()));
        }
        for (Map.Entry<Integer, SkuSummaryProduceVo> entry : map.entrySet()) {
            Integer month = entry.getKey();
            SkuSummaryProduceVo value = entry.getValue();
            String fieldName = "month" + month;
            if (month == 13) {
                fieldName = "yearSum";
            } else if (month == 14) {
                fieldName = "monthAvg";
            } else if (month == 15) {
                fieldName = "currentMonthAvgDiff";
            }
            ProduceSkuSummaryVo monthVo = ReflectUtils.getFieldValue(skuMonthQtyVo, fieldName);
            BeanUtils.copyProperties(value, monthVo);
        }
        return skuMonthQtyVo;
    }

    /**
     * 查询系统运行报表
     *
     * @param queryDto 查询参数
     * @return 结果
     */
    @Override
    public List<SystemRunReportVo> selectSystemRunReport(SystemRunReportDto queryDto) {
        List<SystemRunReportVo> resultList = monthPlanReportMapper.selectSystemRunReport(queryDto);
        String queryProductProcess = queryDto.getProductProcess();
        if (StringUtils.isNotEmpty(queryProductProcess)) {
            resultList = resultList.stream().filter(item -> queryProductProcess.equals(item.getProductProcess())).collect(Collectors.toList());
        }
        if (CollectionUtils.isEmpty(resultList)) {
            return resultList;
        }
        Map<String, SystemRunReportVo> summaryMap = new HashMap<>(16);
        // 查询成型消耗
        List<CxConsumeVo> cxConsumeVoList = monthPlanReportMapper.selectCxConsume(queryDto);
        Map<String, CxConsumeVo> cxConsumeVoMap = new HashMap<>(16);
        if (CollectionUtils.isNotEmpty(cxConsumeVoList)) {
            cxConsumeVoMap = cxConsumeVoList.stream().collect(Collectors.toMap(CxConsumeVo::getScheduleDate, Function.identity()));
        }
        List<ScheduleSummaryVo> gsqCxConsumeList = monthPlanReportMapper.getGsqCxConsume(queryDto);
        Map<String, ScheduleSummaryVo> gsqCxConsumeMap = new HashMap<>(16);
        if (CollectionUtils.isNotEmpty(gsqCxConsumeList)) {
            gsqCxConsumeMap = gsqCxConsumeList.stream().collect(Collectors.toMap(ScheduleSummaryVo::getScheduleDate, Function.identity()));
        }
        for (SystemRunReportVo systemRunReportVo : resultList) {
            // 根据工序代号匹配计量单位
            String productProcess = systemRunReportVo.getProductProcess();
            HalfComponentMeteringUnitEnums enumByProductProcess = HalfComponentMeteringUnitEnums.getEnumByProductProcess(productProcess);
            systemRunReportVo.setMeteringUnit(enumByProductProcess.getMeteringUnit());
            // 取成型消耗
            String scheduleDate = systemRunReportVo.getScheduleDate();
            Double cxConsume = 0D;
            if (HalfComponentMeteringUnitEnums.GSQ.getProductProcess().equals(productProcess)) {
                if (gsqCxConsumeMap.containsKey(scheduleDate)) {
                    cxConsume = gsqCxConsumeMap.get(scheduleDate).getCxConsumeQty();
                }
            } else {
                String consumeFieldName = enumByProductProcess.getConsumeFieldName();
                if (StringUtils.isNotBlank(consumeFieldName)) {
                    if (cxConsumeVoMap.containsKey(scheduleDate)) {
                        cxConsume = ObjectUtils.defaultIfNull(ReflectUtils.getFieldValue(cxConsumeVoMap.get(scheduleDate), consumeFieldName), 0D);
                    }
                }
            }
            // 计算交接班库存（期末库存）理论交接班库存=昨日早班计划+库存+夜班计划-(成型昨日早班消耗量+成型夜班消耗量)
            Double lastDayPlanQty = systemRunReportVo.getLastDayPlanQty();
            Double stockQty = systemRunReportVo.getStockQty();
            Double nightPlanQty = systemRunReportVo.getNightPlanQty();
            systemRunReportVo.setPlanStockQty(BigDecimalUtils.add(lastDayPlanQty, stockQty, nightPlanQty).doubleValue() - cxConsume);
            // 添加汇总行数据
            SystemRunReportVo summaryVo = summaryMap.getOrDefault(productProcess, new SystemRunReportVo());
            summaryVo.setProductProcess(productProcess);
            summaryVo.setProductProcessName(systemRunReportVo.getProductProcessName());
            summaryVo.setPlanSkuCount(summaryVo.getPlanSkuCount() + systemRunReportVo.getPlanSkuCount());
            summaryVo.setPlanSkuQty(summaryVo.getPlanSkuQty() + systemRunReportVo.getPlanSkuQty());
            summaryVo.setFinishQty(summaryVo.getFinishQty() + systemRunReportVo.getFinishQty());
            summaryVo.setStockQty(summaryVo.getStockQty() + stockQty);
            summaryVo.setPlanStockQty(summaryVo.getPlanStockQty() + systemRunReportVo.getPlanStockQty());
            summaryVo.setFinishRate(summaryVo.getFinishRate() + systemRunReportVo.getFinishRate());
            summaryVo.setMeteringUnit(systemRunReportVo.getMeteringUnit());
            summaryMap.put(productProcess, summaryVo);
        }
        Collection<SystemRunReportVo> values = summaryMap.values();
        for (SystemRunReportVo summaryVo : values) {
            Double finishQty = summaryVo.getFinishQty();
            Double planSkuQty = summaryVo.getPlanSkuQty();
            if (planSkuQty != 0) {
                summaryVo.setFinishRate(finishQty / planSkuQty);
            } else {
                summaryVo.setFinishRate(0D);
            }
            summaryVo.setScheduleDate("汇总");
        }
        resultList.addAll(values);
        return resultList;
    }
}
