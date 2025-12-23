package com.zlt.aps.itf.mes.service.impl;

import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.tlt.aps.enums.YesOrNoEnum;
import com.tlt.aps.utils.GenerageMapKeyUtils;
import com.zlt.aps.itf.mes.mapper.MesItfMapper;
import com.zlt.aps.itf.mes.service.MesItfService;
import com.zlt.aps.maindata.enums.MonthPlanEnums;
import com.zlt.aps.maindata.mapper.MdmModelInfoEntityMapper;
import com.zlt.aps.maindata.mapper.MdmProductModelRelationEntityMapper;
import com.zlt.aps.maindata.service.IFactoryParamService;
import com.zlt.aps.maindata.utils.ScmListUtils;
import com.zlt.aps.monthplan.api.domain.entity.*;
import com.zlt.core.dao.basedao.BaseDao;
import com.zlt.sync.domain.AuxReqSyncDataLogs;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
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
    private MdmProductModelRelationEntityMapper productModelRelationEntityMapper;
    @Autowired
    private MdmModelInfoEntityMapper modelInfoEntityMapper;
    @Autowired
    private BaseDao baseDao;
    @Autowired
    private IFactoryParamService iFactoryParamService;

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
        // 型腔模号+NC物料编码作为匹配条件，如果存在，则更新，不存在则插入
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
        // 型腔模号+NC物料编码作为匹配条件，如果存在，则更新，不存在则插入
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
     */
    private static List<MpOverdueSku> addOverdueSku(MdmProductStock productStock, int stockYear, int stockMonth, Date overdueRegularTime, Date overdueCycleTime, Date time, List<MpOverdueSku> mpOverdueSkuList) {
        if (YesOrNoEnum.YES.getCode().equals(productStock.getIsExceedTire())) {
            MpOverdueSku mpOverdueSku = new MpOverdueSku();
            mpOverdueSku.setFactoryCode(productStock.getFactoryCode());
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
            if (time.before(overdueRegularTime)) {
                mpOverdueSku.setIsOverdueRegular(YesOrNoEnum.YES.getCode());
            }
            if (time.before(overdueCycleTime)) {
                mpOverdueSku.setIsOverdueCycle(YesOrNoEnum.YES.getCode());
            }
            mpOverdueSkuList.add(mpOverdueSku);
        }
        return mpOverdueSkuList;
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
        // 先删后增，日期
        Date stockDate = mdmProductStock.getStockDate();
        if (stockDate == null) {
            stockDate = DateUtils.getNowDate("yyyy-MM-dd");
        }
        String factoryCode = mdmProductStock.getFactoryCode();

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
        param.setParamCode(MonthPlanEnums.OVERDUE_REGULAR.getCode());
        Date overdueRegularTime = this.getOverdueTime(param, stockDateCalendar, stockDate);

        param.setParamCode(MonthPlanEnums.OVERDUE_CYCLE.getCode());
        Date overdueCycleTime = this.getOverdueTime(param, stockDateCalendar, stockDate);

        List<List<MdmProductStock>> splitList = ScmListUtils.getSplitList(productStockList, 1000);
        for (List<MdmProductStock> importList : splitList) {
            List<MpOverdueSku> mpOverdueSkuList = new ArrayList<>();
            for (MdmProductStock productStock : importList) {
                String weekYear = productStock.getWeekYear();
                if (weekYear.length() != 4) {
                    continue;
                }
                int week, year;
                try {
                    week = Integer.parseInt(weekYear.substring(2));
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
                if (time.before(subTime4)) {
                    productStock.setExceedStatusToYes(YesOrNoEnum.YES.getCode(), true, true, true, true, true);
                } else if (time.before(subTime3)) {
                    productStock.setExceedStatusToYes(YesOrNoEnum.YES.getCode(), true, true, true, true, false);
                } else if (time.before(subTime2)) {
                    productStock.setExceedStatusToYes(YesOrNoEnum.YES.getCode(), true, true, true, false, false);
                } else if (time.before(subTime1)) {
                    productStock.setExceedStatusToYes(YesOrNoEnum.YES.getCode(), true, true, false, false, false);
                }
                mpOverdueSkuList = addOverdueSku(productStock, stockYear, stockMonth, overdueRegularTime, overdueCycleTime, time, mpOverdueSkuList);
            }
            baseDao.insertBatch(importList);
            baseDao.insertBatch(mpOverdueSkuList);
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
        stockDateCalendar.add(Calendar.MONTH, -Integer.parseInt(overdueParam.getParamValue()));
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
        return mesItfMapper.selectProductStock(productStockMonth);
    }

    /**
     * 同步不合格库存
     *
     * @param mdmUnqualifiedStock 参数
     * @return 结果
     */
    @Override
    public AjaxResult syncUnqualifiedStock(MdmUnqualifiedStock mdmUnqualifiedStock) throws ParseException {
        List<MdmUnqualifiedStock> productStockList = this.getUnqualifiedStock(mdmUnqualifiedStock);
        // 先删后增，日期
        Date stockDate = mdmUnqualifiedStock.getStockDate();
        if (stockDate == null) {
            stockDate = DateUtils.getNowDate("yyyy-MM-dd");
        }
        Map<String, Object> map = new HashMap<>();
        map.put("STOCK_DATE", stockDate);
        baseDao.deleteByMap(ProductStockMonth.class, map);
        List<List<MdmUnqualifiedStock>> splitList = ScmListUtils.getSplitList(productStockList, 1000);
        for (List<MdmUnqualifiedStock> importList : splitList) {
            baseDao.insertBatch(importList);
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
        return mesItfMapper.selectUnqualifiedStock(mdmUnqualifiedStock);
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
        return mesItfMapper.selectRawSpecialMaterialStock(rawSpecialMaterialStock);
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
        List<RawMaterialOutboundRecord> rawMaterialOutboundRecords = mesItfMapper.syncRawMaterialOutboundRecord(materialOutboundRecord);
        Map<String, Object> map = new HashMap<>(16);
        map.put("OUTBOUND_DATE", outboundDate);
        baseDao.deleteByMap(RawMaterialOutboundRecord.class, map);
        baseDao.insertBatch(rawMaterialOutboundRecords);
        return AjaxResult.success();
    }
}
