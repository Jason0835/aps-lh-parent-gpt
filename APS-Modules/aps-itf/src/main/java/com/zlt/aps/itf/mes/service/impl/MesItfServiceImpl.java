package com.zlt.aps.itf.mes.service.impl;

import com.baomidou.dynamic.datasource.toolkit.DynamicDataSourceContextHolder;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.tlt.aps.enums.LocationTypeEnum;
import com.tlt.aps.enums.YesOrNoEnum;
import com.tlt.aps.utils.GenerageMapKeyUtils;
import com.zlt.aps.itf.constant.DataSource;
import com.zlt.aps.itf.mes.mapper.MesItfMapper;
import com.zlt.aps.itf.mes.mapper.MesViewMapper;
import com.zlt.aps.itf.mes.service.MesItfService;
import com.zlt.aps.itf.vo.AuxReqSyncDataLogs;
import com.zlt.aps.maindata.enums.MonthPlanEnums;
import com.zlt.aps.maindata.mapper.*;
import com.zlt.aps.maindata.service.IFactoryParamService;
import com.zlt.aps.maindata.utils.ScmListUtils;
import com.zlt.aps.monthplan.api.domain.entity.*;
import com.zlt.core.dao.basedao.BaseDao;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Chen
 * @since 2025/12/16
 */
@Slf4j
@Service("mesItfService")
public class MesItfServiceImpl implements MesItfService {

    @Autowired
    private MesItfMapper mesItfMapper;
    @Autowired
    private MesViewMapper mesViewMapper;
    @Autowired
    private MdmProductModelRelationEntityMapper productModelRelationEntityMapper;
    @Autowired
    private MdmModelInfoEntityMapper modelInfoEntityMapper;
    @Autowired
    private MdmMaterialInfoEntityMapper materialInfoEntityMapper;
    @Autowired
    private MdmMouldShellInfoEntityMapper mouldShellInfoEntityMapper;
    @Autowired
    private BaseDao baseDao;
    @Autowired
    private IFactoryParamService iFactoryParamService;
    @Autowired
    private MdmCycleSchStruConfEntityMapper cycleSchStruConfEntityMapper;

    /**
     * 同步SKU与模具关系
     *
     * @param syncDataLogs SKU与模具关系
     * @return 结果
     */
    @Override
    public AjaxResult syncProductModRelation(AuxReqSyncDataLogs syncDataLogs) {
        // 查询中间表
        MdmSkuMouldRel mdmSkuMouldRel = new MdmSkuMouldRel();
        mdmSkuMouldRel.setDataVersion(syncDataLogs.getDataVersion());
        List<MdmSkuMouldRel> list = this.getMdmSkuMouldRelList(mdmSkuMouldRel);
        // 型腔模号+物料编码作为匹配条件，如果存在，则更新，不存在则插入
        try {
            // 切换APS数据源 start
            DynamicDataSourceContextHolder.push(DataSource.APS);
            List<List<MdmSkuMouldRel>> splitList = ScmListUtils.getSplitList(list, 1000);
            for (List<MdmSkuMouldRel> skuMouldRelList : splitList) {
                List<MdmSkuMouldRel> existsList = productModelRelationEntityMapper.selectByUniqueKeyList(skuMouldRelList);
                Map<String, MdmSkuMouldRel> existsMap = new HashMap<>(16);
                if (CollectionUtils.isNotEmpty(existsList)) {
                    existsMap = existsList.stream().collect(Collectors.toMap(item -> GenerageMapKeyUtils.createMapKey(item.getFactoryCode(), item.getMouldCode(), item.getMaterialCode()), Function.identity()));
                }
                for (MdmSkuMouldRel skuMouldRel : skuMouldRelList) {
                    String mapKey = GenerageMapKeyUtils.createMapKey(skuMouldRel.getFactoryCode(), skuMouldRel.getMouldCode(), skuMouldRel.getMaterialCode());
                    if (existsMap.containsKey(mapKey)) {
                        MdmSkuMouldRel existsData = existsMap.get(mapKey);
                        skuMouldRel.setId(existsData.getId());
                    }
                }
                baseDao.saveBatch(skuMouldRelList);
            }
        } finally {
            DynamicDataSourceContextHolder.clear();
            // 切换APS数据源 end
        }
        return AjaxResult.success();
    }

    /**
     * 同步模具台账
     *
     * @param syncDataLogs 模具台账
     * @return 结果
     */
    @Override
    public AjaxResult syncModelInfo(AuxReqSyncDataLogs syncDataLogs) {
        // 查询中间表
        MdmModelInfo mdmSkuMouldRel = new MdmModelInfo();
        mdmSkuMouldRel.setDataVersion(syncDataLogs.getDataVersion());
        List<MdmModelInfo> list = getMdmModelInfoList(mdmSkuMouldRel);
        // 型腔模号+物料编码作为匹配条件，如果存在，则更新，不存在则插入
        try {
            // 切换APS数据源 start
            DynamicDataSourceContextHolder.push(DataSource.APS);
            List<List<MdmModelInfo>> splitList = ScmListUtils.getSplitList(list, 1000);
            for (List<MdmModelInfo> saveList : splitList) {
                List<MdmModelInfo> existsList = modelInfoEntityMapper.selectByUniqueKeyList(saveList);
                Map<String, MdmModelInfo> existsMap = new HashMap<>(16);
                if (CollectionUtils.isNotEmpty(existsList)) {
                    existsMap = existsList.stream().collect(Collectors.toMap(item -> GenerageMapKeyUtils.createMapKey(item.getFactoryCode(), item.getMouldCode()), Function.identity()));
                }
                for (MdmModelInfo entity : saveList) {
                    String mapKey = GenerageMapKeyUtils.createMapKey(entity.getFactoryCode(), entity.getMouldCode());
                    if (existsMap.containsKey(mapKey)) {
                        MdmModelInfo existsData = existsMap.get(mapKey);
                        entity.setId(existsData.getId());
                    }
                }
                baseDao.saveBatch(saveList);
            }
        } finally {
            DynamicDataSourceContextHolder.clear();
            // 切换APS数据源 end
        }
        return AjaxResult.success();
    }

    /**
     * 获取模具台账List
     *
     * @param modelInfo 模具台账
     * @return 结果
     */
    @Override
    public List<MdmModelInfo> getMdmModelInfoList(MdmModelInfo modelInfo) {
        return mesItfMapper.selectModelInfoList(modelInfo);
    }

    /**
     * 获取AP与模具关系
     *
     * @param mdmSkuMouldRel SKU与模具关系
     * @return 结果
     */
    @Override
    public List<MdmSkuMouldRel> getMdmSkuMouldRelList(MdmSkuMouldRel mdmSkuMouldRel) {
        return mesItfMapper.selectSkuMouldRelList(mdmSkuMouldRel);
    }

    /**
     * 添加超期SKU
     *
     * @param productStock       成品库存
     * @param stockYear          库存年
     * @param stockMonth         库存月
     * @param overdueRegularTime 超期定期时间
     * @param overdueCycleTime   超期周期时间
     * @param time               时间
     * @param mpOverdueSkuList   要添加的列表
     * @param cycleSchStruConfMap 周期排产结构配置表
     */
    private static void addOverdueSku(MdmProductStock productStock, int stockYear, int stockMonth, Date overdueRegularTime, Date overdueCycleTime, Date time, List<MpOverdueSku> mpOverdueSkuList,
                                      Map<String, MdmCycleSchStruConf> cycleSchStruConfMap) {
        if (YesOrNoEnum.YES.getCode().equals(productStock.getIsExceedTire())) {
            MpOverdueSku mpOverdueSku = new MpOverdueSku();
            String factoryCode = productStock.getFactoryCode();
            mpOverdueSku.setFactoryCode(factoryCode);
            mpOverdueSku.setYear(stockYear);
            mpOverdueSku.setMonth(stockMonth);
            mpOverdueSku.setMesMaterialCode(productStock.getMesMaterialCode());
            mpOverdueSku.setMaterialCode(productStock.getMaterialCode());
            mpOverdueSku.setMaterialDesc(productStock.getMaterialDesc());
            mpOverdueSku.setWeekYear(productStock.getWeekYear());
            mpOverdueSku.setStockDate(productStock.getStockDate());
            mpOverdueSku.setIsOverdueRegular(YesOrNoEnum.NO.getCode());
            mpOverdueSku.setIsOverdueCycle(YesOrNoEnum.NO.getCode());
            mpOverdueSku.setOverdueRegularDate(overdueRegularTime);
            mpOverdueSku.setOverdueCycleDate(overdueCycleTime);
            String structureName = productStock.getStructureName();
            String mapKey = GenerageMapKeyUtils.createMapKey(factoryCode, structureName);
            if (cycleSchStruConfMap.containsKey(mapKey)) {
                mpOverdueSku.setIsOverdueCycle(YesOrNoEnum.YES.getCode());
            } else {
                mpOverdueSku.setIsOverdueRegular(YesOrNoEnum.YES.getCode());
            }
            /*if (time.before(overdueRegularTime)) {
                mpOverdueSku.setIsOverdueRegular(YesOrNoEnum.YES.getCode());
            }
            if (time.before(overdueCycleTime)) {
                mpOverdueSku.setIsOverdueCycle(YesOrNoEnum.YES.getCode());
            }*/
            mpOverdueSkuList.add(mpOverdueSku);
        }
    }

    /**
     * 同步成品库存
     *
     * @param mdmProductStock 参数
     * @return 结果
     */
    @Override
    public AjaxResult syncProductStock(MdmProductStock mdmProductStock) throws ParseException {
        List<MdmProductStock> productStockList = this.getProductStock(mdmProductStock);
        try {
            List<MdmProductStock> saveList = new ArrayList<>();
            // 切换APS数据源 start
            DynamicDataSourceContextHolder.push(DataSource.APS);
            List<String> materialCodeList = productStockList.stream().map(MdmProductStock::getMaterialCode).distinct().collect(Collectors.toList());
            Map<String, MdmMaterialInfo> materialInfoMap = getMaterialInfoMap(materialCodeList);
            for (MdmProductStock stock : productStockList) {
                // 默认外销
                stock.setLocationType(LocationTypeEnum.FOREIGN_LOCATION.getValue());
                String mapKey = GenerageMapKeyUtils.createMapKey(stock.getFactoryCode(), stock.getMaterialCode());
                if (materialInfoMap.containsKey(mapKey)) {
                    MdmMaterialInfo materialInfo = materialInfoMap.get(mapKey);
                    stock.setMaterialDesc(materialInfo.getMaterialDesc());
                    stock.setStructureName(materialInfo.getStructureName());
                    stock.setBrand(materialInfo.getBrand());
                    stock.setProductTypeCode(materialInfo.getProductTypeCode());
                    saveList.add(stock);
                }
            }
            // 先删后增，日期
            Date stockDate = mdmProductStock.getStockDate();
            if (stockDate == null) {
                stockDate = DateUtils.getNowDate("yyyy-MM-dd");
            }
            String factoryCode = mdmProductStock.getFactoryCode();
            String productTypeCode = mdmProductStock.getProductTypeCode();

            Calendar stockDateCalendar = Calendar.getInstance();
            stockDateCalendar.setTime(stockDate);
            int stockYear = stockDateCalendar.get(Calendar.YEAR);
            int stockMonth = stockDateCalendar.get(Calendar.MONTH) + 1;

            this.deleteMdmProductStock(factoryCode, stockDate);

            this.deleteOverDueSku(factoryCode, stockYear, stockMonth);

            int subMonthParam1 = 3, subMonthParam2 = 6, subMonthParam3 = 9, subMonthParam4 = 12;

            stockDateCalendar.add(Calendar.MONTH, -subMonthParam1);
            Date subTime1 = stockDateCalendar.getTime();

            stockDateCalendar.setTime(stockDate);
            stockDateCalendar.add(Calendar.MONTH, -subMonthParam2);
            Date subTime2 = stockDateCalendar.getTime();

            stockDateCalendar.setTime(stockDate);
            stockDateCalendar.add(Calendar.MONTH, -subMonthParam3);
            Date subTime3 = stockDateCalendar.getTime();

            stockDateCalendar.setTime(stockDate);
            stockDateCalendar.add(Calendar.MONTH, -subMonthParam4);
            Date subTime4 = stockDateCalendar.getTime();

            FactoryParam param = new FactoryParam();
            param.setFactoryCode(factoryCode);
            param.setProductTypeCode(productTypeCode);
            param.setParamCode(MonthPlanEnums.OVERDUE_REGULAR.getCode());
            param.setProductTypeCode(mdmProductStock.getProductTypeCode());
            Date overdueRegularTime = this.getOverdueTime(param, stockDateCalendar, stockDate);

            param.setParamCode(MonthPlanEnums.OVERDUE_CYCLE.getCode());
            Date overdueCycleTime = this.getOverdueTime(param, stockDateCalendar, stockDate);

            param.setParamCode(MonthPlanEnums.OVERDUE_TIRE_WARNING.getCode());
            Date overdueTireWaringTime = this.getOverdueTime(param, stockDateCalendar, stockDate);

            // 查询周期排产结构配置表
            LambdaQueryWrapper<MdmCycleSchStruConf> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(MdmCycleSchStruConf::getFactoryCode, mdmProductStock.getFactoryCode());
            queryWrapper.eq(MdmCycleSchStruConf::getIsDelete, YesOrNoEnum.NO.getCode());
            List<MdmCycleSchStruConf> mdmCycleSchStruConfList = cycleSchStruConfEntityMapper.selectList(queryWrapper);
            Map<String, MdmCycleSchStruConf> cycleSchStruConfMap = new HashMap<>(16);
            if (CollectionUtils.isNotEmpty(mdmCycleSchStruConfList)) {
                cycleSchStruConfMap = mdmCycleSchStruConfList.stream().collect(Collectors.toMap(item -> GenerageMapKeyUtils.createMapKey(item.getFactoryCode(), item.getStructureName()), Function.identity(), (v1, v2) -> v1));
            }

            List<List<MdmProductStock>> splitList = ScmListUtils.getSplitList(saveList, 1000);
            for (List<MdmProductStock> importList : splitList) {
                List<MpOverdueSku> mpOverdueSkuList = new ArrayList<>();
                for (MdmProductStock productStock : importList) {
                    String weekYear = productStock.getWeekYear();
                    if (weekYear.length() != 4) {
                        continue;
                    }
                    int week, year;
                    try {
                        week = Integer.parseInt(weekYear.substring(0, 2));
                        year = Integer.parseInt("20" + weekYear.substring(2, 4));
                    } catch (NumberFormatException e) {
                        log.error("解析年周号失败：{}", weekYear);
                        continue;
                    }
                    Calendar instance = Calendar.getInstance();
                    instance.set(Calendar.YEAR, year);
                    instance.set(Calendar.WEEK_OF_YEAR, week);
                    // 根据年周号对应月份判断超期时间
                    Date time = instance.getTime();
                    // 赋值是否超期胎
                    productStock.initExceedTireStatus(YesOrNoEnum.NO.getCode());

                    boolean isExceedTire = time.before(overdueTireWaringTime);
                    if (time.before(subTime4)) {
                        productStock.setExceedStatusToYes(YesOrNoEnum.YES.getCode(), isExceedTire, true, true, true, true);
                    } else if (time.before(subTime3)) {
                        productStock.setExceedStatusToYes(YesOrNoEnum.YES.getCode(), isExceedTire, true, true, true, false);
                    } else if (time.before(subTime2)) {
                        productStock.setExceedStatusToYes(YesOrNoEnum.YES.getCode(), isExceedTire, true, true, false, false);
                    } else if (time.before(subTime1)) {
                        productStock.setExceedStatusToYes(YesOrNoEnum.YES.getCode(), isExceedTire, true, false, false, false);
                    }
                    addOverdueSku(productStock, stockYear, stockMonth, overdueRegularTime, overdueCycleTime, time, mpOverdueSkuList, cycleSchStruConfMap);
                }
                baseDao.insertBatch(importList);
                baseDao.insertBatch(mpOverdueSkuList);
            }
        } finally {
            DynamicDataSourceContextHolder.clear();
            // 切换APS数据源 end
        }
        return AjaxResult.success();
    }

    /**
     * 获取超期时间
     *
     * @param param             查询参数
     * @param stockDateCalendar 库存日期
     * @param stockDate         库存日期
     * @return 结果
     */
    private Date getOverdueTime(FactoryParam param, Calendar stockDateCalendar, Date stockDate) {
        FactoryParam overdueParam = iFactoryParamService.getFacParamSingle(param);
        stockDateCalendar.setTime(stockDate);
        String defaultValue = overdueParam.getDefauleValue();
        String paramValue = StringUtils.defaultIfBlank(overdueParam.getParamValue(), defaultValue);
        stockDateCalendar.add(Calendar.MONTH, -Integer.parseInt(paramValue));
        return stockDateCalendar.getTime();
    }

    /**
     * 删除超期SKU
     *
     * @param factoryCode 工厂代码
     * @param stockYear   库存年
     * @param stockMonth  库存月
     */
    private void deleteOverDueSku(String factoryCode, int stockYear, int stockMonth) {
        Map<String, Object> overdueSkuMap = new HashMap<>();
        overdueSkuMap.put("FACTORY_CODE", factoryCode);
        overdueSkuMap.put("YEAR", stockYear);
        overdueSkuMap.put("MONTH", stockMonth);
        baseDao.deleteByMap(MpOverdueSku.class, overdueSkuMap);
    }

    /**
     * 删除成品库存
     *
     * @param factoryCode 工厂代码
     * @param stockDate   库存日期
     */
    private void deleteMdmProductStock(String factoryCode, Date stockDate) {
        Map<String, Object> map = new HashMap<>();
        map.put("FACTORY_CODE", factoryCode);
        map.put("STOCK_DATE", stockDate);
        baseDao.deleteByMap(MdmProductStock.class, map);
    }

    /**
     * 查询成品库存
     *
     * @param productStockMonth 参数
     * @return 结果
     */
    @Override
    public List<MdmProductStock> getProductStock(MdmProductStock productStockMonth) {
        // 查询视图
        return mesViewMapper.selectProductStock(productStockMonth);
    }

    /**
     * 同步不合格库存
     *
     * @param mdmUnqualifiedStock 参数
     * @return 结果
     */
    @Override
    public AjaxResult syncUnqualifiedStock(MdmUnqualifiedStock mdmUnqualifiedStock) throws ParseException {
        List<MdmUnqualifiedStock> unqualifiedStock = this.getUnqualifiedStock(mdmUnqualifiedStock);
        // 先删后增，日期
        try {
            // 切换APS数据源 start
            DynamicDataSourceContextHolder.push(DataSource.APS);

            List<MdmUnqualifiedStock> saveList = new ArrayList<>();
            List<String> materialCodeList = unqualifiedStock.stream().map(MdmUnqualifiedStock::getMaterialCode).distinct().collect(Collectors.toList());
            Map<String, MdmMaterialInfo> materialInfoMap = getMaterialInfoMap(materialCodeList);
            for (MdmUnqualifiedStock stock : unqualifiedStock) {
                String mapKey = GenerageMapKeyUtils.createMapKey(stock.getFactoryCode(), stock.getMaterialCode());
                if (materialInfoMap.containsKey(mapKey)) {
                    MdmMaterialInfo materialInfo = materialInfoMap.get(mapKey);
                    stock.setMaterialDesc(materialInfo.getMaterialDesc());
                    saveList.add(stock);
                }
            }
            Date stockDate = mdmUnqualifiedStock.getStockDate();
            if (stockDate == null) {
                stockDate = DateUtils.getNowDate("yyyy-MM-dd");
            }
            Map<String, Object> map = new HashMap<>();
            map.put("STOCK_DATE", stockDate);
            baseDao.deleteByMap(ProductStockMonth.class, map);
            List<List<MdmUnqualifiedStock>> splitList = ScmListUtils.getSplitList(saveList, 1000);
            for (List<MdmUnqualifiedStock> importList : splitList) {
                baseDao.insertBatch(importList);
            }
        } finally {
            DynamicDataSourceContextHolder.clear();
            // 切换APS数据源 end
        }
        return AjaxResult.success();
    }

    /**
     * 同步不合格库存
     *
     * @param mdmUnqualifiedStock 参数
     * @return 结果
     */
    @Override
    public List<MdmUnqualifiedStock> getUnqualifiedStock(MdmUnqualifiedStock mdmUnqualifiedStock) {
        // 查询视图
        return mesViewMapper.selectUnqualifiedStock(mdmUnqualifiedStock);
    }

    /**
     * 同步特殊材料库存
     *
     * @param rawSpecialMaterialStock 参数
     * @return 结果
     */
    @Override
    public AjaxResult syncRawSpecialMaterialStock(RawSpecialMaterialStock rawSpecialMaterialStock) throws ParseException {
        List<RawSpecialMaterialStock> productStockList = this.getRawSpecialMaterialStock(rawSpecialMaterialStock);
        // 先删后增，日期
        try {
            // 切换APS数据源 start
            DynamicDataSourceContextHolder.push(DataSource.APS);

            Date stockDate = rawSpecialMaterialStock.getStockDate();
            if (stockDate == null) {
                stockDate = DateUtils.getNowDate("yyyy-MM-dd");
            }
            Map<String, Object> map = new HashMap<>();
            map.put("STOCK_DATE", stockDate);
            baseDao.deleteByMap(RawSpecialMaterialStock.class, map);
            List<List<RawSpecialMaterialStock>> splitList = ScmListUtils.getSplitList(productStockList, 1000);
            for (List<RawSpecialMaterialStock> importList : splitList) {
                baseDao.insertBatch(importList);
            }
        } finally {
            DynamicDataSourceContextHolder.clear();
            // 切换APS数据源 end
        }
        return AjaxResult.success();
    }

    /**
     * 查询特殊材料库存
     *
     * @param rawSpecialMaterialStock 参数
     * @return 结果
     */
    @Override
    public List<RawSpecialMaterialStock> getRawSpecialMaterialStock(RawSpecialMaterialStock rawSpecialMaterialStock) {
        // 查询视图
        return mesViewMapper.selectRawSpecialMaterialStock(rawSpecialMaterialStock);
    }

    /**
     * 同步原材料出库
     *
     * @param materialOutboundRecord 参数
     * @return 结果
     */
    @Override
    public AjaxResult syncRawMaterialOutboundRecord(RawMaterialOutboundRecord materialOutboundRecord) throws ParseException {
        Date outboundDate = materialOutboundRecord.getOutboundDate();
        if (outboundDate == null) {
            outboundDate = DateUtils.getNowDate("yyyy-MM-dd");
        }
        List<RawMaterialOutboundRecord> rawMaterialOutboundRecords = mesViewMapper.syncRawMaterialOutboundRecord(materialOutboundRecord);
        try {
            // 切换APS数据源 start
            DynamicDataSourceContextHolder.push(DataSource.APS);

            List<RawMaterialOutboundRecord> saveList = new ArrayList<>();
            // 获取物料信息回写物料描述
            List<String> materialCodeList = rawMaterialOutboundRecords.stream().map(RawMaterialOutboundRecord::getMaterialCode).distinct().collect(Collectors.toList());
            Map<String, MdmMaterialInfo> materialInfoMap = getMaterialInfoMap(materialCodeList);
            for (RawMaterialOutboundRecord record : rawMaterialOutboundRecords) {
                String mapKey = GenerageMapKeyUtils.createMapKey(record.getFactoryCode(), record.getMaterialCode());
                if (materialInfoMap.containsKey(mapKey)) {
                    MdmMaterialInfo materialInfo = materialInfoMap.get(mapKey);
                    record.setMaterialDesc(materialInfo.getMaterialDesc());
                    saveList.add(record);
                }
            }
            Map<String, Object> map = new HashMap<>(16);
            map.put("OUTBOUND_DATE", outboundDate);
            baseDao.deleteByMap(RawMaterialOutboundRecord.class, map);
            baseDao.insertBatch(saveList);
        } finally {
            DynamicDataSourceContextHolder.clear();
            // 切换APS数据源 end
        }
        return AjaxResult.success();
    }

    /**
     * 获取物料信息
     *
     * @param materialCodeList 物料信息
     * @return 结果
     */
    private Map<String, MdmMaterialInfo> getMaterialInfoMap(List<String> materialCodeList) {
        if (CollectionUtils.isEmpty(materialCodeList)) {
            return new HashMap<>();
        }
        List<MdmMaterialInfo> materialInfoList = new ArrayList<>();
        List<List<String>> splitList = ScmListUtils.getSplitList(materialCodeList, 1000);
        for (List<String> codeList : splitList) {
            LambdaQueryWrapper<MdmMaterialInfo> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.in(MdmMaterialInfo::getMaterialCode, codeList);
            materialInfoList.addAll(materialInfoEntityMapper.selectList(queryWrapper));
        }
        Function<MdmMaterialInfo, String> keyMapper = item -> GenerageMapKeyUtils.createMapKey(item.getFactoryCode(), item.getMaterialCode());
        Map<String, MdmMaterialInfo> materialInfoMap = new HashMap<>();
        if (CollectionUtils.isNotEmpty(materialInfoList)) {
            materialInfoMap = materialInfoList.stream().collect(Collectors.toMap(keyMapper, Function.identity(), (v1, v2) -> v1));
        }
        return materialInfoMap;
    }

    /**
     * 同步成品物料信息
     *
     * @param syncDataLogs 参数
     * @return 结果
     */
    @Override
    public AjaxResult syncMaterial(AuxReqSyncDataLogs syncDataLogs) {
        // 查询中间表
        List<MdmMaterialInfo> list = getMaterialInfoList(syncDataLogs);
        // 工厂+物料编码作为匹配条件，如果存在，则更新，不存在则插入
        try {
            // 切换APS数据源 start
            DynamicDataSourceContextHolder.push(DataSource.APS);

            List<List<MdmMaterialInfo>> splitList = ScmListUtils.getSplitList(list, 1000);
            for (List<MdmMaterialInfo> saveList : splitList) {
                List<String> uniqueKeyList = saveList.stream().map(productInfo ->
                        String.join("|", productInfo.getFactoryCode(), productInfo.getMaterialCode())).collect(Collectors.toList());
                List<MdmMaterialInfo> existsList = materialInfoEntityMapper.selectByUniqueKeyList(uniqueKeyList);
                Map<String, MdmMaterialInfo> existsMap = new HashMap<>(16);
                if (CollectionUtils.isNotEmpty(existsList)) {
                    existsMap = existsList.stream().collect(Collectors.toMap(item -> GenerageMapKeyUtils.createMapKey(item.getFactoryCode(), item.getMaterialCode()), Function.identity()));
                }
                for (MdmMaterialInfo entity : saveList) {
                    String mapKey = GenerageMapKeyUtils.createMapKey(entity.getFactoryCode(), entity.getMaterialCode());
                    if (existsMap.containsKey(mapKey)) {
                        MdmMaterialInfo existsData = existsMap.get(mapKey);
                        entity.setId(existsData.getId());
                    }
                }
                baseDao.saveBatch(saveList);
            }
        } finally {
            DynamicDataSourceContextHolder.clear();
            // 切换APS数据源 end
        }
        return AjaxResult.success();
    }

    /**
     * 查询成品物料信息
     *
     * @param syncDataLogs 参数
     * @return 结果
     */
    private List<MdmMaterialInfo> getMaterialInfoList(AuxReqSyncDataLogs syncDataLogs) {
        return mesItfMapper.selectMaterialList(syncDataLogs);
    }

    /**
     * 同步模壳台账信息
     *
     * @param syncDataLogs 参数
     * @return 结果
     */
    @Override
    public AjaxResult syncMoldShell(AuxReqSyncDataLogs syncDataLogs) {
        // 查询中间表
        List<MdmMouldShellInfo> list = this.getMoldShellList(syncDataLogs);
        // 工厂+模套型号作为匹配条件，如果存在，则更新，不存在则插入
        try {
            // 切换APS数据源 start
            DynamicDataSourceContextHolder.push(DataSource.APS);

            List<List<MdmMouldShellInfo>> splitList = ScmListUtils.getSplitList(list, 1000);
            for (List<MdmMouldShellInfo> saveList : splitList) {
                List<MdmMouldShellInfo> existsList = mouldShellInfoEntityMapper.selectByUniqueKeyList(saveList);
                Map<String, MdmMouldShellInfo> existsMap = new HashMap<>(16);
                if (CollectionUtils.isNotEmpty(existsList)) {
                    existsMap = existsList.stream().collect(Collectors.toMap(item -> GenerageMapKeyUtils.createMapKey(item.getFactoryCode(), item.getMouldSetCode()), Function.identity()));
                }
                for (MdmMouldShellInfo entity : saveList) {
                    String mapKey = GenerageMapKeyUtils.createMapKey(entity.getFactoryCode(), entity.getMouldSetCode());
                    if (existsMap.containsKey(mapKey)) {
                        MdmMouldShellInfo existsData = existsMap.get(mapKey);
                        entity.setId(existsData.getId());
                    }
                }
                baseDao.saveBatch(saveList);
            }
        } finally {
            DynamicDataSourceContextHolder.clear();
            // 切换APS数据源 end
        }
        return AjaxResult.success();
    }

    /**
     * 查询模壳台账信息
     *
     * @param syncDataLogs 参数
     * @return 结果
     */
    private List<MdmMouldShellInfo> getMoldShellList(AuxReqSyncDataLogs syncDataLogs) {
        return mesItfMapper.selectMoldShellList(syncDataLogs);
    }
}
