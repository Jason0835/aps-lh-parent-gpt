package com.zlt.aps.itf.mes.service.impl;

import com.baomidou.dynamic.datasource.toolkit.DynamicDataSourceContextHolder;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.core.utils.AjaxResultUtils;
import com.zlt.aps.constant.FactoryConstant;
import com.zlt.aps.enums.LocationTypeEnum;
import com.zlt.aps.enums.ProductTypeEnum;
import com.zlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.itf.constant.DataSource;
import com.zlt.aps.itf.mes.enums.MouldCategoryConvertEnum;
import com.zlt.aps.itf.mes.mapper.MesItfMapper;
import com.zlt.aps.itf.mes.mapper.MesViewMapper;
import com.zlt.aps.itf.mes.service.MesItfService;
import com.zlt.aps.itf.mes.vo.MoldAlterPlanIssue;
import com.zlt.aps.itf.scm.service.ScmItfService;
import com.zlt.aps.itf.vo.*;
import com.zlt.aps.maindata.enums.MonthPlanEnums;
import com.zlt.aps.maindata.mapper.*;
import com.zlt.aps.maindata.mapper.MdmCxScheFinishQtyEntityMapper;
import com.zlt.aps.maindata.mapper.MdmLhScheFinishQtyEntityMapper;
import com.zlt.aps.maindata.mapper.MdmCxScheDayFinishQtyEntityMapper;
import com.zlt.aps.maindata.mapper.MdmLhScheDayFinishQtyEntityMapper;
import com.zlt.aps.maindata.mapper.MdmMoldAlterPlanFinishEntityMapper;
import com.zlt.aps.maindata.service.IFactoryParamService;
import com.zlt.aps.maindata.service.IMdmProductModelRelationService;
import com.zlt.aps.maindata.service.IMdmSkuStructureRefService;
import com.zlt.aps.maindata.utils.ScmListUtils;
import com.zlt.aps.mdm.api.domain.entity.MdmLhRepairCapsule;
import com.zlt.aps.mdm.api.domain.entity.MdmMoldAlterPlan;
import com.zlt.aps.mdm.api.domain.entity.MdmMouldCleanWarn;
import com.zlt.aps.mdm.api.domain.entity.MdmStructureTreadConfig;
import com.zlt.aps.mp.api.domain.entity.*;
import com.zlt.aps.utils.GenerageMapKeyUtils;
import com.zlt.core.dao.basedao.BaseDao;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
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
    private MdmCxScheFinishQtyEntityMapper cxScheFinishQtyEntityMapper;
    @Autowired
    private MdmLhScheFinishQtyEntityMapper lhScheFinishQtyEntityMapper;
    @Autowired
    private MdmCxScheDayFinishQtyEntityMapper cxScheDayFinishQtyEntityMapper;
    @Autowired
    private MdmLhScheDayFinishQtyEntityMapper lhScheDayFinishQtyEntityMapper;
    @Autowired
    private MdmMoldAlterPlanFinishEntityMapper moldAlterPlanFinishEntityMapper;
    @Autowired
    private BaseDao baseDao;
    @Autowired
    private IFactoryParamService iFactoryParamService;

    @Autowired
    private MdmCycleSchStruConfEntityMapper cycleSchStruConfEntityMapper;

    @Autowired
    private MdmDevMaintenancePlanEntityMapper devMaintenancePlanEntityMapper;

    @Autowired
    private MdmLhRepairCapsuleEntityMapper lhRepairCapsuleEntityMapper;

    @Autowired
    private MdmMouldCleanPlanEntityMapper mouldCleanPlanEntityMapper;

    @Autowired
    private MdmStructureTreadConfigEntityMapper structureTreadConfigEntityMapper;

    @Autowired
    private MdmTreadStockEntityMapper treadStockEntityMapper;

    @Autowired
    private IMdmProductModelRelationService iMdmProductModelRelationService;

    @Autowired
    private IMdmSkuStructureRefService iMdmSkuStructureRefService;

    @Autowired
    private ScmItfService scmItfService;

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
        BeanUtils.copyProperties(syncDataLogs, mdmSkuMouldRel);
        List<MdmSkuMouldRel> list = this.getMdmSkuMouldRelList(mdmSkuMouldRel);
        // 唯一键重复随机取一条
        Map<String, MdmSkuMouldRel> groupMap = list.stream().collect(Collectors.toMap(item -> item.getFactoryCode() + "|" + item.getMouldCode() + "|" + item.getMaterialCode(), Function.identity(), (v1, v2) -> v1));
        list = new ArrayList<>(groupMap.values());
        // 型腔模号+物料编码作为匹配条件，如果存在，则更新，不存在则插入
        try {
            // 切换APS数据源 start
            DynamicDataSourceContextHolder.push(DataSource.APS);
            List<List<MdmSkuMouldRel>> splitList = ScmListUtils.getSplitList(list, 1000);
            for (List<MdmSkuMouldRel> skuMouldRelList : splitList) {
                List<MdmSkuMouldRel> existsList = productModelRelationEntityMapper.selectByUniqueKeyList(skuMouldRelList);
                Map<String, MdmSkuMouldRel> existsMap = new HashMap<>(16);
                if (CollectionUtils.isNotEmpty(existsList)) {
                    existsMap = existsList.stream().collect(Collectors.toMap(item -> GenerageMapKeyUtils.createMapKey(item.getFactoryCode(), item.getMouldCode(), item.getMaterialCode()), Function.identity(), (v1, v2) -> v1));
                }
                for (MdmSkuMouldRel skuMouldRel : skuMouldRelList) {
                    skuMouldRel.setBaseVale(null);
                    if (StringUtils.isBlank(skuMouldRel.getIsSamePatterPanel())) {
                        skuMouldRel.setIsSamePatterPanel(YesOrNoEnum.NO.getCode());
                    }
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
        MdmModelInfo mdmModelInfo = new MdmModelInfo();
        BeanUtils.copyProperties(syncDataLogs, mdmModelInfo);
        List<MdmModelInfo> list = getMdmModelInfoList(mdmModelInfo);
        // 唯一键重复随机取一条
        Map<String, MdmModelInfo> groupMap = list.stream().collect(Collectors.toMap(item -> item.getFactoryCode() + "|" + item.getMouldCode(), Function.identity(), (v1, v2) -> v1));
        list = new ArrayList<>(groupMap.values());
        // 型腔模号+物料编码作为匹配条件，如果存在，则更新，不存在则插入
        try {
            // 切换APS数据源 start
            DynamicDataSourceContextHolder.push(DataSource.APS);
            List<List<MdmModelInfo>> splitList = ScmListUtils.getSplitList(list, 1000);
            for (List<MdmModelInfo> saveList : splitList) {
                List<MdmModelInfo> existsList = modelInfoEntityMapper.selectByUniqueKeyList(saveList);
                Map<String, MdmModelInfo> existsMap = new HashMap<>(16);
                if (CollectionUtils.isNotEmpty(existsList)) {
                    existsMap = existsList.stream().collect(Collectors.toMap(item -> GenerageMapKeyUtils.createMapKey(item.getFactoryCode(), item.getMouldCode()), Function.identity(), (v1, v2) -> v1));
                }
                for (MdmModelInfo entity : saveList) {
                    entity.setBaseVale(null);
                    entity.setCreateBy("MES");
                    entity.setUpdateBy("MES");
                    String mapKey = GenerageMapKeyUtils.createMapKey(entity.getFactoryCode(), entity.getMouldCode());
                    if (existsMap.containsKey(mapKey)) {
                        MdmModelInfo existsData = existsMap.get(mapKey);
                        entity.setId(existsData.getId());
                    }
                }
                baseDao.saveBatch(saveList);
            }
            // 更新SKU与模具关系的主花纹
            MdmModelInfo modelInfo = new MdmModelInfo();
            modelInfo.setBaseVale(null);
            productModelRelationEntityMapper.updateMainPatternByModelInfo(modelInfo);
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
     * 获取SKU与模具关系
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
    private static void addOverdueSku(MdmProductStock productStock, int stockYear, int stockMonth, Date overdueRegularTime, Date overdueCycleTime, Date time, List<MpOverdueSku> mpOverdueSkuList) {
        if (time.before(overdueRegularTime) || time.before(overdueCycleTime)) {
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
            if (time.before(overdueCycleTime)) {
                mpOverdueSku.setIsOverdueCycle(YesOrNoEnum.YES.getCode());
            }
            if (time.before(overdueRegularTime)) {
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
    public AjaxResult syncProductStock(MdmProductStock mdmProductStock) {
        List<MdmProductStock> productStockList = this.getProductStock(mdmProductStock);
        try {
            List<MdmProductStock> saveList = new ArrayList<>();
            // 切换APS数据源 start
            DynamicDataSourceContextHolder.push(DataSource.APS);
            List<String> materialCodeList = productStockList.stream().map(MdmProductStock::getMaterialCode).distinct().collect(Collectors.toList());
            Map<String, MdmMaterialInfo> materialInfoMap = getMaterialInfoMap(materialCodeList);
            for (MdmProductStock stock : productStockList) {
                // 年周号为空或等于0的数据，跳过
                if (StringUtils.isBlank(stock.getWeekYear()) || ApsConstant.APS_STRING_0.equals(stock.getWeekYear())) {
                    continue;
                }
                // 默认外销
                String mapKey = GenerageMapKeyUtils.createMapKey(stock.getFactoryCode(), stock.getMaterialCode());
                try {
                    stock.setStockDate(DateUtils.getNowDate("yyyy-MM-dd"));
                } catch (ParseException e) {
                    throw new RuntimeException(e);
                }
                if (materialInfoMap.containsKey(mapKey)) {
                    MdmMaterialInfo materialInfo = materialInfoMap.get(mapKey);
                    stock.setMaterialDesc(materialInfo.getMaterialDesc());
                    stock.setStructureName(materialInfo.getStructureName());
                    stock.setBrand(materialInfo.getBrand());
                    stock.setProductTypeCode(materialInfo.getProductTypeCode());
                    saveList.add(stock);
                }
            }
            if (CollectionUtils.isNotEmpty(saveList)) {

                // 初始化上下文
                MdmProductStockContext context = new MdmProductStockContext();
                context.initContext(mdmProductStock);

                // 先删后增，日期，删除成品库存
                this.deleteMdmProductStock(context.getFactoryCode(), context.getStockDate());

                // 生成超期SKU，保存成品库存
                context.setIsSaveStock(Boolean.TRUE);
                genOverDueSku(context, saveList);
            }
        } finally {
            DynamicDataSourceContextHolder.clear();
            // 切换APS数据源 end
        }
        return AjaxResult.success();
    }

    /**
     * 生成超期SKU
     * @param mdmProductStock 参数
     * @return 结果
     */
    @Override
    public AjaxResult genOverDueSkuByStock(MdmProductStock mdmProductStock) {
        try {
            // 切换APS数据源 start
            DynamicDataSourceContextHolder.push(DataSource.APS);
            // 初始化上下文
            MdmProductStockContext context = new MdmProductStockContext();
            context.initContext(mdmProductStock);
            // 生成超期SKU，不保存成品库存
            context.setIsSaveStock(Boolean.FALSE);
            // 查询成品库存列表
            Map<String, Object> map = new HashMap<>();
            map.put("FACTORY_CODE", context.getFactoryCode());
            map.put("STOCK_DATE", context.getStockDate());
            List<MdmProductStock> saveList = baseDao.selectByMap(MdmProductStock.class, map);
            genOverDueSku(context, saveList);
        } finally {
            DynamicDataSourceContextHolder.clear();
            // 切换APS数据源 end
        }
        return AjaxResult.success();
    }

    /**
     * 根据库存计算生成超期SKU
     * @param context 上下文
     * @param saveList 库存列表
     */
    private void genOverDueSku(MdmProductStockContext context, List<MdmProductStock> saveList) {
        // 删除超期SKU
        this.deleteOverDueSku(context.getFactoryCode(), context.getStockYear(), context.getStockMonth());

        // 赋值上下文超期时间
        this.setContextOverDueTime(context);

        List<List<MdmProductStock>> splitList = ScmListUtils.getSplitList(saveList, 1000);
        for (List<MdmProductStock> importList : splitList) {
            List<MpOverdueSku> mpOverdueSkuList = new ArrayList<>();
            for (MdmProductStock productStock : importList) {
                productStock.setCreateTime(DateUtils.getNowDate());
                productStock.setUpdateTime(DateUtils.getNowDate());
                String weekYear = StringUtils.defaultIfBlank(productStock.getWeekYear(), "");
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

                boolean isExceedTire = time.before(context.getOverdueTireWaringTime());
                if (time.before(context.getSubTime4())) {
                    productStock.setExceedStatusToYes(YesOrNoEnum.YES.getCode(), isExceedTire, true, true, true, true);
                } else if (time.before(context.getSubTime3())) {
                    productStock.setExceedStatusToYes(YesOrNoEnum.YES.getCode(), isExceedTire, true, true, true, false);
                } else if (time.before(context.getSubTime2())) {
                    productStock.setExceedStatusToYes(YesOrNoEnum.YES.getCode(), isExceedTire, true, true, false, false);
                } else if (time.before(context.getSubTime1())) {
                    productStock.setExceedStatusToYes(YesOrNoEnum.YES.getCode(), isExceedTire, true, false, false, false);
                }
                addOverdueSku(productStock, context.getStockYear(), context.getStockMonth(), context.getOverdueRegularTime(), context.getOverdueCycleTime(), time, mpOverdueSkuList);
            }
            if (context.getIsSaveStock()) {
                baseDao.insertBatch(importList);
            }
            baseDao.insertBatch(mpOverdueSkuList);
        }
    }

    private void setContextOverDueTime(MdmProductStockContext context) {
        Date stockDate = context.getStockDate();
        Calendar stockDateCalendar = context.getStockDateCalendar();

        FactoryParam param = new FactoryParam();
        param.setFactoryCode(context.getFactoryCode());
        param.setProductTypeCode(context.getProductTypeCode());
        param.setParamCode(MonthPlanEnums.OVERDUE_REGULAR.getCode());
        Date overdueRegularTime = this.getOverdueTime(param, stockDateCalendar, stockDate);

        param.setParamCode(MonthPlanEnums.OVERDUE_CYCLE.getCode());
        Date overdueCycleTime = this.getOverdueTime(param, stockDateCalendar, stockDate);

        param.setParamCode(MonthPlanEnums.OVERDUE_TIRE_WARNING.getCode());
        Date overdueTireWaringTime = this.getOverdueTime(param, stockDateCalendar, stockDate);

        context.setOverdueRegularTime(overdueRegularTime);
        context.setOverdueCycleTime(overdueCycleTime);
        context.setOverdueTireWaringTime(overdueTireWaringTime);
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
//        map.put("STOCK_DATE", stockDate);
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
        List<MdmProductStock> mdmProductStockList = mesViewMapper.selectProductStock(productStockMonth);
        for (MdmProductStock productStock : mdmProductStockList) {
            // 默认外销、全钢
            productStock.setLocationType(LocationTypeEnum.FOREIGN_LOCATION.getValue());
            productStock.setProductTypeCode(ProductTypeEnum.WHOLE_STEEL.getValue());
        }
        return mdmProductStockList;
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
        // 先删后增
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

            if (CollectionUtils.isNotEmpty(saveList)) {
                Map<String, Object> map = new HashMap<>();
                map.put("FACTORY_CODE", mdmUnqualifiedStock.getFactoryCode());
                map.put("YEAR", mdmUnqualifiedStock.getYear());
                map.put("MONTH", mdmUnqualifiedStock.getMonth());
                baseDao.deleteByMap(ProductStockMonth.class, map);
                List<List<MdmUnqualifiedStock>> splitList = ScmListUtils.getSplitList(saveList, 1000);
                for (List<MdmUnqualifiedStock> importList : splitList) {
                    baseDao.insertBatch(importList);
                }
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
        List<MdmUnqualifiedStock> unqualifiedStockList = mesViewMapper.selectUnqualifiedStock(mdmUnqualifiedStock);
        for (MdmUnqualifiedStock unqualifiedStock : unqualifiedStockList) {
            try {
                unqualifiedStock.setStockDate(DateUtils.getNowDate("yyyy-MM-dd"));
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
            Date stockDate = unqualifiedStock.getStockDate();
            unqualifiedStock.setYear(DateUtils.getYear(stockDate));
            unqualifiedStock.setMonth(DateUtils.getMonth(stockDate));
        }
        return unqualifiedStockList;
    }

    /**
     * 同步特殊材料库存
     *
     * @param rawSpecialMaterialStock 参数
     * @return 结果
     */
    @Override
    public AjaxResult syncRawSpecialMaterialStock(RawSpecialMaterialStock rawSpecialMaterialStock) throws ParseException {
        List<RawSpecialMaterialStock> saveList = this.getRawSpecialMaterialStock(rawSpecialMaterialStock);
        // 先删后增，日期
        try {
            // 切换APS数据源 start
            DynamicDataSourceContextHolder.push(DataSource.APS);

            if (CollectionUtils.isNotEmpty(saveList)) {
                Date stockDate = rawSpecialMaterialStock.getStockDate();
                if (stockDate == null) {
                    stockDate = DateUtils.getNowDate("yyyy-MM-dd");
                }
                Map<String, Object> map = new HashMap<>();
                map.put("FACTORY_CODE", rawSpecialMaterialStock.getFactoryCode());
                map.put("YEAR", saveList.get(0).getYear());
                map.put("MONTH", saveList.get(0).getMonth());
                baseDao.deleteByMap(RawSpecialMaterialStock.class, map);
                List<List<RawSpecialMaterialStock>> splitList = ScmListUtils.getSplitList(saveList, 1000);
                for (List<RawSpecialMaterialStock> importList : splitList) {
                    baseDao.insertBatch(importList);
                }
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
        List<RawSpecialMaterialStock> stockList = mesViewMapper.selectRawSpecialMaterialStock(rawSpecialMaterialStock);
        for (RawSpecialMaterialStock stock : stockList) {
            try {
                stock.setStockDate(DateUtils.getNowDate("yyyy-MM-dd"));
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
            Date stockDate = stock.getStockDate();
            stock.setYear(DateUtils.getYear(stockDate));
            stock.setMonth(DateUtils.getMonth(stockDate));
        }
        return stockList;
    }

    /**
     * 同步原材料出库
     *
     * @param syncDataLogs 参数
     * @return 结果
     */
    @Override
    public AjaxResult syncRawMaterialOutboundRecord(AuxReqSyncDataLogs syncDataLogs) throws ParseException {
        Date outboundDate = DateUtils.getNowDate("yyyy-MM-dd");
        List<RawMaterialOutboundRecord> rawMaterialOutboundRecords = mesItfMapper.syncRawMaterialOutboundRecord(syncDataLogs);
        try {
            // 切换APS数据源 start
            DynamicDataSourceContextHolder.push(DataSource.APS);

            List<RawMaterialOutboundRecord> saveList = new ArrayList<>();
            // 获取物料信息回写物料描述
            List<String> materialCodeList = rawMaterialOutboundRecords.stream().map(RawMaterialOutboundRecord::getMaterialCode).distinct().collect(Collectors.toList());
            Map<String, MdmMaterialInfo> materialInfoMap = getMaterialInfoMap(materialCodeList);
            for (RawMaterialOutboundRecord record : rawMaterialOutboundRecords) {
                record.setBaseVale(null);
                String mapKey = GenerageMapKeyUtils.createMapKey(record.getFactoryCode(), record.getMaterialCode());
                if (materialInfoMap.containsKey(mapKey)) {
                    MdmMaterialInfo materialInfo = materialInfoMap.get(mapKey);
                    record.setMaterialDesc(materialInfo.getMaterialDesc());
                    record.setMesMaterialCode(materialInfo.getMesMaterialCode());
                }
                saveList.add(record);
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
        List<GoodsBoxVo> goodsBoxList = getGoodsBoxList(syncDataLogs);
        Map<String, GoodsBoxVo> goodsBoxVoMap = new HashMap<>();
        if (CollectionUtils.isNotEmpty(goodsBoxList)) {
            goodsBoxVoMap = goodsBoxList.stream().collect(Collectors.toMap(item -> GenerageMapKeyUtils.createMapKey(item.getFacCode(), item.getgCode()), Function.identity(), (s1, s2) -> s1));
        }
        // 唯一键重复随机取一条
        Map<String, MdmMaterialInfo> groupMap = list.stream().collect(Collectors.toMap(item -> item.getFactoryCode() + "|" + item.getMaterialCode(), Function.identity(), (v1, v2) -> v1));
        list = new ArrayList<>(groupMap.values());
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
                    existsMap = existsList.stream().collect(Collectors.toMap(item -> GenerageMapKeyUtils.createMapKey(item.getFactoryCode(), item.getMaterialCode()), Function.identity(), (v1, v2) -> v1));
                }
                for (MdmMaterialInfo entity : saveList) {
                    // 默认全钢
                    entity.setProductTypeCode(ProductTypeEnum.WHOLE_STEEL.getValue());
                    entity.setProductTypeName(ProductTypeEnum.WHOLE_STEEL.getName());
                    // 是否不可生产，默认否
                    if (entity.getCantProduce() == null) {
                        entity.setCantProduce(ApsConstant.APS_YES_NO_0);
                    }
                    // 物料类型转换
                    String mesMaterialCategory = entity.getMesMaterialCategory();
                    MouldCategoryConvertEnum convertEnum = MouldCategoryConvertEnum.getByMesCode(mesMaterialCategory);
                    if (convertEnum != null) {
                        entity.setMaterialCategory(convertEnum.getCode());
                    }
                    String goodBoxMapKey = GenerageMapKeyUtils.createMapKey(entity.getFactoryCode(), entity.getMaterialCode());
                    if (goodsBoxVoMap.containsKey(goodBoxMapKey)) {
                        GoodsBoxVo goodsBoxVo = goodsBoxVoMap.get(goodBoxMapKey);
                        entity.setQualityStateCode(goodsBoxVo.getQualityStateCode());
                    }
                    entity.setBaseVale(null);
                    entity.setCreateBy("MES");
                    entity.setUpdateBy("MES");
                    String mapKey = GenerageMapKeyUtils.createMapKey(entity.getFactoryCode(), entity.getMaterialCode());
                    if (existsMap.containsKey(mapKey)) {
                        MdmMaterialInfo existsData = existsMap.get(mapKey);
                        entity.setId(existsData.getId());
                        // 结构不更新---艺琴说的
                        entity.setStructureName(existsData.getStructureName());
                    }
                }
                baseDao.saveBatch(saveList);
            }
            // 更新主花纹到物料信息
            iMdmProductModelRelationService.updateMainPatternToMaterial(new MdmSkuMouldRel());
            // 更新结构到物料信息
            iMdmSkuStructureRefService.updateStructureToMaterial(new MdmSkuStructureRef());
        } finally {
            DynamicDataSourceContextHolder.clear();
            // 切换APS数据源 end
        }
        return AjaxResult.success();
    }

    /**
     * 获取scm质控状态列表
     * @return 结果
     */
    private List<GoodsBoxVo> getGoodsBoxList(AuxReqSyncDataLogs syncDataLogs) {
        GoodsBoxVo goodsBox = new GoodsBoxVo();
        goodsBox.setFacCode(syncDataLogs.getFactoryCode());
        AjaxResult ajaxResult = scmItfService.selectGoodsBox(goodsBox);
        return AjaxResultUtils.getList(ajaxResult, GoodsBoxVo.class);
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
        // 唯一键重复随机取一条
        Map<String, MdmMouldShellInfo> groupMap = list.stream().collect(Collectors.toMap(item -> item.getFactoryCode() + "|" + item.getMouldSetCode(), Function.identity(), (v1, v2) -> v1));
        list = new ArrayList<>(groupMap.values());
        // 工厂+模套型号作为匹配条件，如果存在，则更新，不存在则插入
        try {
            // 切换APS数据源 start
            DynamicDataSourceContextHolder.push(DataSource.APS);

            List<List<MdmMouldShellInfo>> splitList = ScmListUtils.getSplitList(list, 1000);
            for (List<MdmMouldShellInfo> saveList : splitList) {
                List<MdmMouldShellInfo> existsList = mouldShellInfoEntityMapper.selectByUniqueKeyList(saveList);
                Map<String, MdmMouldShellInfo> existsMap = new HashMap<>(16);
                if (CollectionUtils.isNotEmpty(existsList)) {
                    existsMap = existsList.stream().collect(Collectors.toMap(item -> GenerageMapKeyUtils.createMapKey(item.getFactoryCode(), item.getMouldSetCode()), Function.identity(), (v1, v2) -> v1));
                }
                for (MdmMouldShellInfo entity : saveList) {
                    entity.setBaseVale(null);
                    entity.setCreateBy("MES");
                    entity.setUpdateBy("MES");
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

    /**
     * 查询品牌信息
     *
     * @return 结果
     */
    @Override
    public List<MesBrandDict> selectMesBrandDict() {
        return mesViewMapper.selectMesBrandDict();
    }

    /**
     * 同步成型在机数据
     * @param mdmCxMachineOnlineInfo 参数
     * @return 结果
     */
    @Override
    public AjaxResult syncMachineOnlineInfo(MdmCxMachineOnlineInfo mdmCxMachineOnlineInfo) {
        DynamicDataSourceContextHolder.push(DataSource.MES);
        List<MdmCxMachineOnlineInfo> syncList = mesItfMapper.selectCxMachineOnlineSyncList(mdmCxMachineOnlineInfo);
        DynamicDataSourceContextHolder.poll();

        try {
            DynamicDataSourceContextHolder.push(DataSource.APS);

            if (CollectionUtils.isNotEmpty(syncList)) {
                Map<String, Object> map = new HashMap<>();
                map.put("FACTORY_CODE", mdmCxMachineOnlineInfo.getFactoryCode());
                baseDao.deleteByMap(MdmCxMachineOnlineInfo.class, map);

                for (MdmCxMachineOnlineInfo info : syncList) {
                    info.setCreateBy("MES");
                    info.setUpdateBy("MES");
                    info.setCreateTime(DateUtils.getNowDate());
                    info.setUpdateTime(DateUtils.getNowDate());
                }

                List<List<MdmCxMachineOnlineInfo>> splitList = ScmListUtils.getSplitList(syncList, 1000);
                for (List<MdmCxMachineOnlineInfo> importList : splitList) {
                    baseDao.insertBatch(importList);
                }
            }
        } finally {
            DynamicDataSourceContextHolder.poll();
        }
        return AjaxResult.success();
    }

    /**
     * 同步硫化在机数据
     * @param mdmLhMachineOnlineInfo 参数
     * @return 结果
     */
    @Override
    public AjaxResult syncLhMachineOnlineInfo(MdmLhMachineOnlineInfo mdmLhMachineOnlineInfo) {
        DynamicDataSourceContextHolder.push(DataSource.MES);
        List<MdmLhMachineOnlineInfo> syncList = mesItfMapper.selectLhMachineOnlineSyncList(mdmLhMachineOnlineInfo);
        DynamicDataSourceContextHolder.poll();

        try {
            DynamicDataSourceContextHolder.push(DataSource.APS);

            if (CollectionUtils.isNotEmpty(syncList)) {
                Map<String, Object> map = new HashMap<>();
                map.put("FACTORY_CODE", mdmLhMachineOnlineInfo.getFactoryCode());
                baseDao.deleteByMap(MdmLhMachineOnlineInfo.class, map);

                for (MdmLhMachineOnlineInfo info : syncList) {
                    info.setCreateBy("MES");
                    info.setUpdateBy("MES");
                    info.setCreateTime(DateUtils.getNowDate());
                    info.setUpdateTime(DateUtils.getNowDate());
                }

                List<List<MdmLhMachineOnlineInfo>> splitList = ScmListUtils.getSplitList(syncList, 1000);
                for (List<MdmLhMachineOnlineInfo> importList : splitList) {
                    baseDao.insertBatch(importList);
                }
            }
        } finally {
            DynamicDataSourceContextHolder.poll();
        }
        return AjaxResult.success();
    }

    /**
     * 同步设备保养计划
     * 采用更新删除标识模式，而不是先删后插
     * @param syncDataLogs 同步参数
     * @return 结果
     */
    @Override
    public AjaxResult syncDevMaintenancePlan(AuxReqSyncDataLogs syncDataLogs) {
        DynamicDataSourceContextHolder.push(DataSource.MES);
        List<DevMaintenancePlan> syncList = mesItfMapper.selectDevMaintenancePlanList(syncDataLogs);
        DynamicDataSourceContextHolder.poll();

        Map<String, DevMaintenancePlan> groupMap = syncList.stream()
                .collect(Collectors.toMap(
                        item -> item.getFactoryCode() + "|" + item.getDevCode() + "|" + item.getPrecisionType(),
                        Function.identity(),
                        (v1, v2) -> v1
                ));
        syncList = new ArrayList<>(groupMap.values());

        try {
            DynamicDataSourceContextHolder.push(DataSource.APS);

            List<List<DevMaintenancePlan>> splitList = ScmListUtils.getSplitList(syncList, 1000);
            for (List<DevMaintenancePlan> saveList : splitList) {
                List<MdmDevMaintenancePlan> existsList = devMaintenancePlanEntityMapper.selectByUniqueKeyList(
                        saveList.stream().map(item -> {
                            MdmDevMaintenancePlan plan = new MdmDevMaintenancePlan();
                            plan.setDevCode(item.getDevCode());
                            plan.setPrecisionType(item.getPrecisionType());
                            plan.setFactoryCode(item.getFactoryCode());
                            return plan;
                        }).collect(Collectors.toList())
                );

                Map<String, MdmDevMaintenancePlan> existsMap = new HashMap<>(16);
                if (CollectionUtils.isNotEmpty(existsList)) {
                    existsMap = existsList.stream()
                            .collect(Collectors.toMap(
                                    item -> GenerageMapKeyUtils.createMapKey(item.getFactoryCode(), item.getDevCode(), item.getPrecisionType()),
                                    Function.identity(),
                                    (v1, v2) -> v1
                            ));
                }

                List<MdmDevMaintenancePlan> insertOrUpdateList = new ArrayList<>();
                for (DevMaintenancePlan item : saveList) {
                    MdmDevMaintenancePlan entity = new MdmDevMaintenancePlan();
                    entity.setDevCode(item.getDevCode());
                    entity.setPrecisionType(item.getPrecisionType());
                    entity.setFactoryCode(item.getFactoryCode());
                    entity.setCompanyCode(item.getCompanyCode());
                    entity.setDataVersion(item.getDataVersion());
                    entity.setCreateBy("MES");
                    entity.setUpdateBy("MES");

                    if (StringUtils.isNotBlank(item.getOperTime())) {
                        try {
                            entity.setOperTime(DateUtils.parseDate(item.getOperTime(), "yyyy-MM-dd HH:mm:ss.SSS", "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd"));
                        } catch (Exception e) {
                            log.error("解析计划时间失败：{}", item.getOperTime(), e);
                        }
                    }
                    if (StringUtils.isNotBlank(item.getFirstWashTime())) {
                        try {
                            entity.setFirstWashTime(DateUtils.parseDate(item.getFirstWashTime(), "yyyy-MM-dd HH:mm:ss.SSS", "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd"));
                        } catch (Exception e) {
                            log.error("解析实际时间失败：{}", item.getFirstWashTime(), e);
                        }
                    }

                    if (StringUtils.isNotBlank(item.getDelFlag())) {
                        entity.setIsDelete(Integer.valueOf(item.getDelFlag()));
                    } else {
                        entity.setIsDelete(0);
                    }

                    String mapKey = GenerageMapKeyUtils.createMapKey(entity.getFactoryCode(), entity.getDevCode(), entity.getPrecisionType());
                    if (existsMap.containsKey(mapKey)) {
                        MdmDevMaintenancePlan existsData = existsMap.get(mapKey);
                        entity.setId(existsData.getId());
                    }
                    insertOrUpdateList.add(entity);
                }

                baseDao.saveBatch(insertOrUpdateList);
            }
        } finally {
            DynamicDataSourceContextHolder.poll();
        }
        return AjaxResult.success();
    }


    /**
     * 同步模具清洗预警计划
     * 采用更新删除标识模式，而不是先删后插
     * @param syncDataLogs 同步参数
     * @return 结果
     */
    @Override
    public AjaxResult syncMouldCleanWarn(AuxReqSyncDataLogs syncDataLogs) {
        DynamicDataSourceContextHolder.push(DataSource.MES);
        List<MouldCleanPlan> syncList = mesItfMapper.selectMouldCleanPlanList(syncDataLogs);
        DynamicDataSourceContextHolder.poll();

        Map<String, MouldCleanPlan> groupMap = syncList.stream()
                .collect(Collectors.toMap(
                        item -> item.getFactoryCode() + "|" + item.getLhCode(),
                        Function.identity(),
                        (v1, v2) -> v1
                ));
        syncList = new ArrayList<>(groupMap.values());

        try {
            DynamicDataSourceContextHolder.push(DataSource.APS);

            List<List<MouldCleanPlan>> splitList = ScmListUtils.getSplitList(syncList, 1000);
            for (List<MouldCleanPlan> saveList : splitList) {
                List<MdmMouldCleanWarn> existsList = mouldCleanPlanEntityMapper.selectByUniqueKeyList(
                        saveList.stream().map(item -> {
                            MdmMouldCleanWarn plan = new MdmMouldCleanWarn();
                            plan.setLhCode(item.getLhCode());
                            plan.setFactoryCode(item.getFactoryCode());
                            return plan;
                        }).collect(Collectors.toList())
                );

                Map<String, MdmMouldCleanWarn> existsMap = new HashMap<>(16);
                if (CollectionUtils.isNotEmpty(existsList)) {
                    existsMap = existsList.stream()
                            .collect(Collectors.toMap(
                                    item -> GenerageMapKeyUtils.createMapKey(item.getFactoryCode(), item.getLhCode()),
                                    Function.identity(),
                                    (v1, v2) -> v1
                            ));
                }

                List<MdmMouldCleanWarn> insertOrUpdateList = new ArrayList<>();
                for (MouldCleanPlan item : saveList) {
                    MdmMouldCleanWarn entity = new MdmMouldCleanWarn();
                    entity.setLhCode(item.getLhCode());
                    entity.setFactoryCode(item.getFactoryCode());
                    entity.setCompanyCode(item.getCompanyCode());
                    entity.setDataVersion(item.getDataVersion());
                    entity.setRemark(item.getRemark());
                    entity.setCreateBy("MES");
                    entity.setUpdateBy("MES");

                    if (StringUtils.isNotBlank(item.getOperTime())) {
                        try {
                            entity.setOperTime(DateUtils.parseDate(item.getOperTime(), "yyyy-MM-dd HH:mm:ss.SSS", "yyyy-MM-dd HH:mm:ss"));
                        } catch (Exception e) {
                            log.error("解析上机时间失败：{}", item.getOperTime(), e);
                        }
                    }

                    if (StringUtils.isNotBlank(item.getFirstWashTime())) {
                        try {
                            entity.setFirstWashTime(DateUtils.parseDate(item.getFirstWashTime(), "yyyy-MM-dd HH:mm:ss.SSS", "yyyy-MM-dd HH:mm:ss"));
                        } catch (Exception e) {
                            log.error("解析首次清洗时间失败：{}", item.getFirstWashTime(), e);
                        }
                    }

                    if (StringUtils.isNotBlank(item.getSecondWashTime())) {
                        try {
                            entity.setSecondWashTime(DateUtils.parseDate(item.getSecondWashTime(), "yyyy-MM-dd HH:mm:ss.SSS", "yyyy-MM-dd HH:mm:ss"));
                        } catch (Exception e) {
                            log.error("解析二次清洗时间失败：{}", item.getSecondWashTime(), e);
                        }
                    }

                    if (StringUtils.isNotBlank(item.getDelFlag())) {
                        entity.setIsDelete(Integer.valueOf(item.getDelFlag()));
                    } else {
                        entity.setIsDelete(0);
                    }

                    String mapKey = GenerageMapKeyUtils.createMapKey(entity.getFactoryCode(), entity.getLhCode());
                    if (existsMap.containsKey(mapKey)) {
                        MdmMouldCleanWarn existsData = existsMap.get(mapKey);
                        entity.setId(existsData.getId());
                    }
                    insertOrUpdateList.add(entity);
                }

                baseDao.saveBatch(insertOrUpdateList);
            }
        } finally {
            DynamicDataSourceContextHolder.poll();
        }
        return AjaxResult.success();
    }

    /**
     * 同步胶囊已使用次数
     * 采用先删后插模式
     * @param syncDataLogs 同步参数
     * @return 结果
     */
    @Override
    public AjaxResult syncLhRepairCapsule(AuxReqSyncDataLogs syncDataLogs) {
        DynamicDataSourceContextHolder.push(DataSource.MES);
        List<LhRepairCapsule> syncList = mesItfMapper.selectLhRepairCapsuleList(syncDataLogs);
        DynamicDataSourceContextHolder.poll();

        Map<String, LhRepairCapsule> groupMap = syncList.stream()
                .collect(Collectors.toMap(
                        item -> item.getFactoryCode() + "|" + item.getLhCode(),
                        Function.identity(),
                        (v1, v2) -> v1
                ));
        syncList = new ArrayList<>(groupMap.values());

        try {
            DynamicDataSourceContextHolder.push(DataSource.APS);

            if (CollectionUtils.isNotEmpty(syncList)) {
                Map<String, Object> map = new HashMap<>();
                map.put("FACTORY_CODE", syncDataLogs.getFactoryCode());
                baseDao.deleteByMap(MdmLhRepairCapsule.class, map);

                List<MdmLhRepairCapsule> insertList = new ArrayList<>();
                for (LhRepairCapsule item : syncList) {
                    MdmLhRepairCapsule entity = new MdmLhRepairCapsule();
                    entity.setLhCode(item.getLhCode());
                    entity.setMaterialCode(item.getMaterialCode());
                    entity.setReplaceCapsuleCount(item.getReplaceCapsuleCount());
                    entity.setReplaceCapsuleCount2(item.getReplaceCapsuleCount2());
                    entity.setBrand(item.getBrand());
//                    entity.setRemark(item.getRemark());
//                    entity.setDataVersion(item.getDataVersion());
                    entity.setCompanyCode(FactoryConstant.DEFAULT_FACTORY_CODE);
                    entity.setFactoryCode(FactoryConstant.DEFAULT_FACTORY_CODE);
                    entity.setIsDelete(0);
                    entity.setCreateBy("MES");
                    entity.setUpdateBy("MES");
                    entity.setCreateTime(DateUtils.getNowDate());
                    entity.setUpdateTime(DateUtils.getNowDate());

                    if (StringUtils.isNotBlank(item.getObtainTime())) {
                        try {
                            entity.setObtainTime(DateUtils.parseDate(item.getObtainTime(), "yyyy-MM-dd HH:mm:ss.SSS", "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd"));
                        } catch (Exception e) {
                            log.error("解析获取日期失败：{}", item.getObtainTime(), e);
                        }
                    }

                    insertList.add(entity);
                }

                List<List<MdmLhRepairCapsule>> splitList = ScmListUtils.getSplitList(insertList, 1000);
                for (List<MdmLhRepairCapsule> importList : splitList) {
                    baseDao.insertBatch(importList);
                }
            }
        } finally {
            DynamicDataSourceContextHolder.poll();
        }
        return AjaxResult.success();
    }

    /**
     * 同步结构整车胎面配置
     * 采用更新删除标识模式，而不是先删后插
     * @param syncDataLogs 同步参数
     * @return 结果
     */
    @Override
    public AjaxResult syncStructureTreadConfig(AuxReqSyncDataLogs syncDataLogs) {
        DynamicDataSourceContextHolder.push(DataSource.MES);
        List<StructureTreadConfig> syncList = mesItfMapper.selectStructureTreadConfigList(syncDataLogs);
        DynamicDataSourceContextHolder.poll();

        Map<String, StructureTreadConfig> groupMap = syncList.stream()
                .collect(Collectors.toMap(
                        item -> item.getFactoryCode() + "|" + item.getStructureCode(),
                        Function.identity(),
                        (v1, v2) -> v1
                ));
        syncList = new ArrayList<>(groupMap.values());

        try {
            DynamicDataSourceContextHolder.push(DataSource.APS);

            List<List<StructureTreadConfig>> splitList = ScmListUtils.getSplitList(syncList, 1000);
            for (List<StructureTreadConfig> saveList : splitList) {
                List<MdmStructureTreadConfig> existsList = structureTreadConfigEntityMapper.selectByUniqueKeyList(
                        saveList.stream().map(item -> {
                            MdmStructureTreadConfig config = new MdmStructureTreadConfig();
                            config.setStructureCode(item.getStructureCode());
                            config.setFactoryCode(item.getFactoryCode());
                            return config;
                        }).collect(Collectors.toList())
                );

                Map<String, MdmStructureTreadConfig> existsMap = new HashMap<>(16);
                if (CollectionUtils.isNotEmpty(existsList)) {
                    existsMap = existsList.stream()
                            .collect(Collectors.toMap(
                                    item -> GenerageMapKeyUtils.createMapKey(item.getFactoryCode(), item.getStructureCode()),
                                    Function.identity(),
                                    (v1, v2) -> v1
                            ));
                }

                List<MdmStructureTreadConfig> insertOrUpdateList = new ArrayList<>();
                for (StructureTreadConfig item : saveList) {
                    MdmStructureTreadConfig entity = new MdmStructureTreadConfig();
                    entity.setStructureCode(item.getStructureCode());
                    entity.setTreadCount(item.getTreadCount());
                    entity.setDataVersion(item.getDataVersion());
                    entity.setCompanyCode(item.getCompanyCode());
                    entity.setFactoryCode(item.getFactoryCode());
                    entity.setCreateBy("MES");
                    entity.setUpdateBy("MES");

                    if (StringUtils.isNotBlank(item.getDelFlag())) {
                        entity.setIsDelete(Integer.valueOf(item.getDelFlag()));
                    } else {
                        entity.setIsDelete(0);
                    }

                    String mapKey = GenerageMapKeyUtils.createMapKey(entity.getFactoryCode(), entity.getStructureCode());
                    if (existsMap.containsKey(mapKey)) {
                        MdmStructureTreadConfig existsData = existsMap.get(mapKey);
                        entity.setId(existsData.getId());
                    }
                    insertOrUpdateList.add(entity);
                }

                baseDao.saveBatch(insertOrUpdateList);
            }
        } finally {
            DynamicDataSourceContextHolder.poll();
        }
        return AjaxResult.success();
    }

    @Autowired
    private MdmMesCxStockEntityMapper mesCxStockEntityMapper;

    /**
     * 同步生胎库存
     * 采用先删后插模式
     * @param syncDataLogs 同步参数
     * @return 结果
     */
    @Override
    public AjaxResult syncMesCxStock(AuxReqSyncDataLogs syncDataLogs) {
        DynamicDataSourceContextHolder.push(DataSource.MES);
        List<MdmMesCxStock> syncList = mesItfMapper.selectMesCxStockList(syncDataLogs);
        DynamicDataSourceContextHolder.poll();

        Map<String, MdmMesCxStock> groupMap = syncList.stream()
                .collect(Collectors.toMap(
                        item -> item.getFactoryCode() + "|" + item.getEmbryoCode(),
                        Function.identity(),
                        (v1, v2) -> v1
                ));
        syncList = new ArrayList<>(groupMap.values());

        try {
            DynamicDataSourceContextHolder.push(DataSource.APS);

            if (CollectionUtils.isNotEmpty(syncList)) {
                Map<String, Object> map = new HashMap<>();
                map.put("FACTORY_CODE", syncDataLogs.getFactoryCode());
                baseDao.deleteByMap(MdmMesCxStock.class, map);

                List<MdmMesCxStock> insertList = new ArrayList<>();
                for (MdmMesCxStock item : syncList) {
                    MdmMesCxStock entity = new MdmMesCxStock();
                    BeanUtils.copyProperties(item, entity);
                    entity.setCreateBy("MES");
                    entity.setUpdateBy("MES");
                    entity.setCreateTime(DateUtils.getNowDate());
                    entity.setUpdateTime(DateUtils.getNowDate());
                    insertList.add(entity);
                }

                List<List<MdmMesCxStock>> splitList = ScmListUtils.getSplitList(insertList, 1000);
                for (List<MdmMesCxStock> importList : splitList) {
                    baseDao.insertBatch(importList);
                }
            }
        } finally {
            DynamicDataSourceContextHolder.poll();
        }
        return AjaxResult.success();
    }

    /**
     * 同步成型排程完成量
     * 采用更新删除标识模式，而不是先删后插
     * @param syncDataLogs 同步参数
     * @return 结果
     */
    @Override
    public AjaxResult syncCxClassShiftFinishQty(AuxReqSyncDataLogs syncDataLogs) {
        DynamicDataSourceContextHolder.push(DataSource.MES);
        List<MdmCxScheFinishQty> syncList = mesItfMapper.selectCxClassShiftFinishQtyList(syncDataLogs);
        DynamicDataSourceContextHolder.poll();

        Map<String, MdmCxScheFinishQty> groupMap = syncList.stream()
                .collect(Collectors.toMap(
                        item -> item.getFactoryCode() + "|" + item.getOrderNo() + "|" + item.getScheduleDate() + "|" + item.getCxMachineCode(),
                        Function.identity(),
                        (v1, v2) -> v1
                ));
        syncList = new ArrayList<>(groupMap.values());

        try {
            DynamicDataSourceContextHolder.push(DataSource.APS);

            List<List<MdmCxScheFinishQty>> splitList = ScmListUtils.getSplitList(syncList, 1000);
            for (List<MdmCxScheFinishQty> saveList : splitList) {
                List<MdmCxScheFinishQty> existsList = cxScheFinishQtyEntityMapper.selectByUniqueKeyList(
                        saveList.stream().map(item -> {
                            MdmCxScheFinishQty qty = new MdmCxScheFinishQty();
                            qty.setOrderNo(item.getOrderNo());
                            qty.setScheduleDate(item.getScheduleDate());
                            qty.setCxMachineCode(item.getCxMachineCode());
                            qty.setFactoryCode(item.getFactoryCode());
                            return qty;
                        }).collect(Collectors.toList())
                );

                Map<String, MdmCxScheFinishQty> existsMap = new HashMap<>(16);
                if (CollectionUtils.isNotEmpty(existsList)) {
                    existsMap = existsList.stream()
                            .collect(Collectors.toMap(
                                    item -> GenerageMapKeyUtils.createMapKey(item.getFactoryCode(), item.getOrderNo(), String.valueOf(item.getScheduleDate()), item.getCxMachineCode()),
                                    Function.identity(),
                                    (v1, v2) -> v1
                            ));
                }

                List<MdmCxScheFinishQty> insertOrUpdateList = new ArrayList<>();
                for (MdmCxScheFinishQty item : saveList) {
                    MdmCxScheFinishQty entity = new MdmCxScheFinishQty();
                    BeanUtils.copyProperties(item, entity);
                    entity.setCreateBy("MES");
                    entity.setUpdateBy("MES");

                    if (entity.getIsDelete() == null) {
                        entity.setIsDelete(0);
                    }

                    String mapKey = GenerageMapKeyUtils.createMapKey(entity.getFactoryCode(), entity.getOrderNo(), String.valueOf(entity.getScheduleDate()), entity.getCxMachineCode());
                    if (existsMap.containsKey(mapKey)) {
                        MdmCxScheFinishQty existsData = existsMap.get(mapKey);
                        entity.setId(existsData.getId());
                    }
                    insertOrUpdateList.add(entity);
                }

                baseDao.saveBatch(insertOrUpdateList);
            }
        } finally {
            DynamicDataSourceContextHolder.poll();
        }
        return AjaxResult.success();
    }

    /**
     * 同步硫化排程完成量
     * 采用更新删除标识模式，而不是先删后插
     * @param syncDataLogs 同步参数
     * @return 结果
     */
    @Override
    public AjaxResult syncLhClassShiftFinishQty(AuxReqSyncDataLogs syncDataLogs) {
        DynamicDataSourceContextHolder.push(DataSource.MES);
        List<MdmLhScheFinishQty> syncList = mesItfMapper.selectLhClassShiftFinishQtyList(syncDataLogs);
        DynamicDataSourceContextHolder.poll();

        Map<String, MdmLhScheFinishQty> groupMap = syncList.stream()
                .collect(Collectors.toMap(
                        item -> item.getFactoryCode() + "|" + item.getOrderNo() + "|" + item.getScheduleDate() + "|" + item.getLhMachineCode(),
                        Function.identity(),
                        (v1, v2) -> v1
                ));
        syncList = new ArrayList<>(groupMap.values());

        try {
            DynamicDataSourceContextHolder.push(DataSource.APS);

            List<List<MdmLhScheFinishQty>> splitList = ScmListUtils.getSplitList(syncList, 1000);
            for (List<MdmLhScheFinishQty> saveList : splitList) {
                List<MdmLhScheFinishQty> existsList = lhScheFinishQtyEntityMapper.selectByUniqueKeyList(
                        saveList.stream().map(item -> {
                            MdmLhScheFinishQty qty = new MdmLhScheFinishQty();
                            qty.setOrderNo(item.getOrderNo());
                            qty.setScheduleDate(item.getScheduleDate());
                            qty.setLhMachineCode(item.getLhMachineCode());
                            qty.setFactoryCode(item.getFactoryCode());
                            return qty;
                        }).collect(Collectors.toList())
                );

                Map<String, MdmLhScheFinishQty> existsMap = new HashMap<>(16);
                if (CollectionUtils.isNotEmpty(existsList)) {
                    existsMap = existsList.stream()
                            .collect(Collectors.toMap(
                                    item -> GenerageMapKeyUtils.createMapKey(item.getFactoryCode(), item.getOrderNo(), String.valueOf(item.getScheduleDate()), item.getLhMachineCode()),
                                    Function.identity(),
                                    (v1, v2) -> v1
                            ));
                }

                List<MdmLhScheFinishQty> insertOrUpdateList = new ArrayList<>();
                for (MdmLhScheFinishQty item : saveList) {
                    MdmLhScheFinishQty entity = new MdmLhScheFinishQty();
                    BeanUtils.copyProperties(item, entity);
                    entity.setCreateBy("MES");
                    entity.setUpdateBy("MES");

                    if (entity.getIsDelete() == null) {
                        entity.setIsDelete(0);
                    }

                    String mapKey = GenerageMapKeyUtils.createMapKey(entity.getFactoryCode(), entity.getOrderNo(), String.valueOf(entity.getScheduleDate()), entity.getLhMachineCode());
                    if (existsMap.containsKey(mapKey)) {
                        MdmLhScheFinishQty existsData = existsMap.get(mapKey);
                        entity.setId(existsData.getId());
                    }
                    insertOrUpdateList.add(entity);
                }

                baseDao.saveBatch(insertOrUpdateList);
            }
        } finally {
            DynamicDataSourceContextHolder.poll();
        }
        return AjaxResult.success();
    }

    /**
     * 同步成型排程日完成量
     * 采用更新删除标识模式，而不是先删后插
     * @param syncDataLogs 同步参数
     * @return 结果
     */
    @Override
    public AjaxResult syncCxScheDayFinishQty(AuxReqSyncDataLogs syncDataLogs) {
        DynamicDataSourceContextHolder.push(DataSource.MES);
        List<MdmCxScheDayFinishQty> syncList = mesItfMapper.selectCxScheDayFinishQtyList(syncDataLogs);
        DynamicDataSourceContextHolder.poll();

        Map<String, MdmCxScheDayFinishQty> groupMap = syncList.stream()
                .collect(Collectors.toMap(
                        item -> item.getFactoryCode() + "|" + item.getFinishDate() + "|" + item.getEmbryoCode() + "|" + item.getBomDataVersion(),
                        Function.identity(),
                        (v1, v2) -> v1
                ));
        syncList = new ArrayList<>(groupMap.values());

        try {
            DynamicDataSourceContextHolder.push(DataSource.APS);

            List<List<MdmCxScheDayFinishQty>> splitList = ScmListUtils.getSplitList(syncList, 1000);
            for (List<MdmCxScheDayFinishQty> saveList : splitList) {
                List<MdmCxScheDayFinishQty> existsList = cxScheDayFinishQtyEntityMapper.selectByUniqueKeyList(
                        saveList.stream().map(item -> {
                            MdmCxScheDayFinishQty qty = new MdmCxScheDayFinishQty();
                            qty.setFinishDate(item.getFinishDate());
                            qty.setEmbryoCode(item.getEmbryoCode());
                            qty.setBomDataVersion(item.getBomDataVersion());
                            qty.setFactoryCode(item.getFactoryCode());
                            return qty;
                        }).collect(Collectors.toList())
                );

                Map<String, MdmCxScheDayFinishQty> existsMap = new HashMap<>(16);
                if (CollectionUtils.isNotEmpty(existsList)) {
                    existsMap = existsList.stream()
                            .collect(Collectors.toMap(
                                    item -> GenerageMapKeyUtils.createMapKey(item.getFactoryCode(), String.valueOf(item.getFinishDate()), item.getEmbryoCode(), item.getBomDataVersion()),
                                    Function.identity(),
                                    (v1, v2) -> v1
                            ));
                }

                List<MdmCxScheDayFinishQty> insertOrUpdateList = new ArrayList<>();
                for (MdmCxScheDayFinishQty item : saveList) {
                    MdmCxScheDayFinishQty entity = new MdmCxScheDayFinishQty();
                    BeanUtils.copyProperties(item, entity);
                    entity.setCreateBy("MES");
                    entity.setUpdateBy("MES");

                    if (entity.getIsDelete() == null) {
                        entity.setIsDelete(0);
                    }

                    String mapKey = GenerageMapKeyUtils.createMapKey(entity.getFactoryCode(), String.valueOf(entity.getFinishDate()), entity.getEmbryoCode(), entity.getBomDataVersion());
                    if (existsMap.containsKey(mapKey)) {
                        MdmCxScheDayFinishQty existsData = existsMap.get(mapKey);
                        entity.setId(existsData.getId());
                    }
                    insertOrUpdateList.add(entity);
                }

                baseDao.saveBatch(insertOrUpdateList);
            }
        } finally {
            DynamicDataSourceContextHolder.poll();
        }
        return AjaxResult.success();
    }

    /**
     * 同步硫化排程日完成量
     * 采用更新删除标识模式，而不是先删后插
     * @param syncDataLogs 同步参数
     * @return 结果
     */
    @Override
    public AjaxResult syncLhScheDayFinishQty(AuxReqSyncDataLogs syncDataLogs) {
        DynamicDataSourceContextHolder.push(DataSource.MES);
        List<MdmLhScheDayFinishQty> syncList = mesItfMapper.selectLhScheDayFinishQtyList(syncDataLogs);
        DynamicDataSourceContextHolder.poll();

        Map<String, MdmLhScheDayFinishQty> groupMap = syncList.stream()
                .collect(Collectors.toMap(
                        item -> item.getFactoryCode() + "|" + item.getFinishDate() + "|" + item.getMaterialCode() + "|" + item.getMesMaterialCode(),
                        Function.identity(),
                        (v1, v2) -> v1
                ));
        syncList = new ArrayList<>(groupMap.values());

        try {
            DynamicDataSourceContextHolder.push(DataSource.APS);

            List<List<MdmLhScheDayFinishQty>> splitList = ScmListUtils.getSplitList(syncList, 1000);
            for (List<MdmLhScheDayFinishQty> saveList : splitList) {
                List<MdmLhScheDayFinishQty> existsList = lhScheDayFinishQtyEntityMapper.selectByUniqueKeyList(
                        saveList.stream().map(item -> {
                            MdmLhScheDayFinishQty qty = new MdmLhScheDayFinishQty();
                            qty.setFinishDate(item.getFinishDate());
                            qty.setMaterialCode(item.getMaterialCode());
                            qty.setMesMaterialCode(item.getMesMaterialCode());
                            qty.setFactoryCode(item.getFactoryCode());
                            return qty;
                        }).collect(Collectors.toList())
                );

                Map<String, MdmLhScheDayFinishQty> existsMap = new HashMap<>(16);
                if (CollectionUtils.isNotEmpty(existsList)) {
                    existsMap = existsList.stream()
                            .collect(Collectors.toMap(
                                    item -> GenerageMapKeyUtils.createMapKey(item.getFactoryCode(), String.valueOf(item.getFinishDate()), item.getMaterialCode(), item.getMesMaterialCode()),
                                    Function.identity(),
                                    (v1, v2) -> v1
                            ));
                }

                List<MdmLhScheDayFinishQty> insertOrUpdateList = new ArrayList<>();
                for (MdmLhScheDayFinishQty item : saveList) {
                    MdmLhScheDayFinishQty entity = new MdmLhScheDayFinishQty();
                    BeanUtils.copyProperties(item, entity);
                    entity.setCreateBy("MES");
                    entity.setUpdateBy("MES");

                    if (entity.getIsDelete() == null) {
                        entity.setIsDelete(0);
                    }

                    String mapKey = GenerageMapKeyUtils.createMapKey(entity.getFactoryCode(), String.valueOf(entity.getFinishDate()), entity.getMaterialCode(), entity.getMesMaterialCode());
                    if (existsMap.containsKey(mapKey)) {
                        MdmLhScheDayFinishQty existsData = existsMap.get(mapKey);
                        entity.setId(existsData.getId());
                    }
                    insertOrUpdateList.add(entity);
                }

                baseDao.saveBatch(insertOrUpdateList);
            }
        } finally {
            DynamicDataSourceContextHolder.poll();
        }
        return AjaxResult.success();
    }

    /**
     * 模具交替计划下发到MES
     * @param moldAlterPlanList 模具交替计划列表
     * @return 结果
     */
    @Override
    public AjaxResult issueMoldAlterPlan(List<MdmMoldAlterPlan> moldAlterPlanList) {
        if (CollectionUtils.isEmpty(moldAlterPlanList)) {
            return AjaxResult.success();
        }

        // 转换为中间表实体
        List<MoldAlterPlanIssue> issueList = new ArrayList<>();
        for (MdmMoldAlterPlan plan : moldAlterPlanList) {
            MoldAlterPlanIssue issue = new MoldAlterPlanIssue();
            BeanUtils.copyProperties(plan, issue);
            issueList.add(issue);
        }

        try {
            // 切换MES数据源 start
            DynamicDataSourceContextHolder.push(DataSource.MES);

            // 批量插入到中间表
            mesItfMapper.insertMoldAlterPlanList(issueList);
        } finally {
            DynamicDataSourceContextHolder.clear();
            // 切换MES数据源 end
        }
        return AjaxResult.success();
    }

    /**
     * 同步模具交替计划完成回报
     * 采用更新删除标识模式，而不是先删后插
     * @param syncDataLogs 同步参数
     * @return 结果
     */
    @Override
    public AjaxResult syncMoldAlterPlanFinish(AuxReqSyncDataLogs syncDataLogs) {
        DynamicDataSourceContextHolder.push(DataSource.MES);
        List<MdmMoldAlterPlanFinish> syncList = mesItfMapper.selectMoldAlterPlanFinishList(syncDataLogs);
        DynamicDataSourceContextHolder.poll();

        Map<String, MdmMoldAlterPlanFinish> groupMap = syncList.stream()
                .collect(Collectors.toMap(
                        item -> item.getFactoryCode() + "|" + item.getLhBatchNo() + "|" + item.getOrderNo() + "|" + item.getScheduleDate() + "|" + item.getLhMachineCode() + "|" + item.getLeftRightMold(),
                        Function.identity(),
                        (v1, v2) -> v1
                ));
        syncList = new ArrayList<>(groupMap.values());

        try {
            DynamicDataSourceContextHolder.push(DataSource.APS);

            List<List<MdmMoldAlterPlanFinish>> splitList = ScmListUtils.getSplitList(syncList, 1000);
            for (List<MdmMoldAlterPlanFinish> saveList : splitList) {
                List<MdmMoldAlterPlanFinish> existsList = moldAlterPlanFinishEntityMapper.selectByUniqueKeyList(
                        saveList.stream().map(item -> {
                            MdmMoldAlterPlanFinish finish = new MdmMoldAlterPlanFinish();
                            finish.setLhBatchNo(item.getLhBatchNo());
                            finish.setOrderNo(item.getOrderNo());
                            finish.setScheduleDate(item.getScheduleDate());
                            finish.setLhMachineCode(item.getLhMachineCode());
                            finish.setLeftRightMold(item.getLeftRightMold());
                            finish.setFactoryCode(item.getFactoryCode());
                            return finish;
                        }).collect(Collectors.toList())
                );

                Map<String, MdmMoldAlterPlanFinish> existsMap = new HashMap<>(16);
                if (CollectionUtils.isNotEmpty(existsList)) {
                    existsMap = existsList.stream()
                            .collect(Collectors.toMap(
                                    item -> GenerageMapKeyUtils.createMapKey(item.getFactoryCode(), item.getLhBatchNo(), item.getOrderNo(), String.valueOf(item.getScheduleDate()), item.getLhMachineCode(), item.getLeftRightMold()),
                                    Function.identity(),
                                    (v1, v2) -> v1
                            ));
                }

                List<MdmMoldAlterPlanFinish> insertOrUpdateList = new ArrayList<>();
                for (MdmMoldAlterPlanFinish item : saveList) {
                    MdmMoldAlterPlanFinish entity = new MdmMoldAlterPlanFinish();
                    BeanUtils.copyProperties(item, entity);
                    entity.setCreateBy("MES");
                    entity.setUpdateBy("MES");

                    if (entity.getIsDelete() == null) {
                        entity.setIsDelete(0);
                    }

                    String mapKey = GenerageMapKeyUtils.createMapKey(entity.getFactoryCode(), entity.getLhBatchNo(), entity.getOrderNo(), String.valueOf(entity.getScheduleDate()), entity.getLhMachineCode(), entity.getLeftRightMold());
                    if (existsMap.containsKey(mapKey)) {
                        MdmMoldAlterPlanFinish existsData = existsMap.get(mapKey);
                        entity.setId(existsData.getId());
                    }
                    insertOrUpdateList.add(entity);
                }

                baseDao.saveBatch(insertOrUpdateList);
            }
        } finally {
            DynamicDataSourceContextHolder.poll();
        }
        return AjaxResult.success();
    }

    /**
     * 同步出库未扫描订单
     *
     * @param outbountOrdersNotScan 参数
     * @return 结果
     */
    @Override
    public AjaxResult syncOutbountOrdersNotScan(MdmOutbountOrdersNotScan outbountOrdersNotScan) {
        // 设置默认分厂
        if (StringUtils.isBlank(outbountOrdersNotScan.getFactoryCode())) {
            outbountOrdersNotScan.setFactoryCode(FactoryConstant.DEFAULT_FACTORY_CODE);
        }

        DynamicDataSourceContextHolder.push(DataSource.MES);
        List<MdmOutbountOrdersNotScan> orderList = this.getOutbountOrdersNotScan(outbountOrdersNotScan);
        try {
            DynamicDataSourceContextHolder.push(DataSource.APS);
            if (CollectionUtils.isNotEmpty(orderList)) {
                Map<String, Object> map = new HashMap<>();
                map.put("FACTORY_CODE", outbountOrdersNotScan.getFactoryCode());
                map.put("STOCK_DATE", DateUtils.getNowDate("yyyy-MM-dd"));
                baseDao.deleteByMap(MdmOutbountOrdersNotScan.class, map);

                // 转换为APS实体并设置创建信息
                List<MdmOutbountOrdersNotScan> insertList = new ArrayList<>();
                for (MdmOutbountOrdersNotScan item : orderList) {
                    MdmOutbountOrdersNotScan entity = new MdmOutbountOrdersNotScan();
                    BeanUtils.copyProperties(item, entity);
                    entity.setStockDate(DateUtils.getNowDate("yyyy-MM-dd"));
                    entity.setCompanyCode(FactoryConstant.DEFAULT_COMPANY_CODE);
                    entity.setFactoryCode(FactoryConstant.DEFAULT_FACTORY_CODE);
                    entity.setCreateBy("MES");
                    entity.setUpdateBy("MES");
                    entity.setCreateTime(DateUtils.getNowDate());
                    entity.setUpdateTime(DateUtils.getNowDate());
                    insertList.add(entity);
                }

                // 分批插入
                List<List<MdmOutbountOrdersNotScan>> splitList = ScmListUtils.getSplitList(insertList, 1000);
                for (List<MdmOutbountOrdersNotScan> importList : splitList) {
                    baseDao.insertBatch(importList);
                }
            }
        } catch (ParseException e) {
            throw new RuntimeException(e);
        } finally {
            DynamicDataSourceContextHolder.clear();
        }
        return AjaxResult.success();
    }

    /**
     * 查询出库未扫描订单
     *
     * @param outbountOrdersNotScan 参数
     * @return 结果
     */
    @Override
    public List<MdmOutbountOrdersNotScan> getOutbountOrdersNotScan(MdmOutbountOrdersNotScan outbountOrdersNotScan) {
        return mesViewMapper.selectOutbountOrdersNotScan(outbountOrdersNotScan);
    }

    /**
     * 同步胎面库存
     * 采用更新删除标识模式，而不是先删后插
     * @param syncDataLogs 同步参数
     * @return 结果
     */
    @Override
    public AjaxResult syncTreadStock(AuxReqSyncDataLogs syncDataLogs) {
        DynamicDataSourceContextHolder.push(DataSource.MES);
        List<MdmTreadStock> syncList = mesItfMapper.selectTreadStockList(syncDataLogs);
        DynamicDataSourceContextHolder.poll();

        Map<String, MdmTreadStock> groupMap = syncList.stream()
                .collect(Collectors.toMap(
                        item -> item.getFactoryCode() + "|" + item.getStockDate() + "|" + item.getMaterialCode(),
                        Function.identity(),
                        (v1, v2) -> v1
                ));
        syncList = new ArrayList<>(groupMap.values());

        try {
            DynamicDataSourceContextHolder.push(DataSource.APS);

            List<List<MdmTreadStock>> splitList = ScmListUtils.getSplitList(syncList, 1000);
            for (List<MdmTreadStock> saveList : splitList) {
                List<MdmTreadStock> existsList = treadStockEntityMapper.selectByUniqueKeyList(
                        saveList.stream().map(item -> {
                            MdmTreadStock stock = new MdmTreadStock();
                            stock.setStockDate(item.getStockDate());
                            stock.setMaterialCode(item.getMaterialCode());
                            stock.setFactoryCode(item.getFactoryCode());
                            return stock;
                        }).collect(Collectors.toList())
                );

                Map<String, MdmTreadStock> existsMap = new HashMap<>(16);
                if (CollectionUtils.isNotEmpty(existsList)) {
                    existsMap = existsList.stream()
                            .collect(Collectors.toMap(
                                    item -> GenerageMapKeyUtils.createMapKey(item.getFactoryCode(), String.valueOf(item.getStockDate()), item.getMaterialCode()),
                                    Function.identity(),
                                    (v1, v2) -> v1
                            ));
                }

                List<MdmTreadStock> insertOrUpdateList = new ArrayList<>();
                for (MdmTreadStock item : saveList) {
                    MdmTreadStock entity = new MdmTreadStock();
                    BeanUtils.copyProperties(item, entity);
                    entity.setCreateBy("MES");
                    entity.setUpdateBy("MES");

                    if (entity.getIsDelete() == null) {
                        entity.setIsDelete(0);
                    }

                    String mapKey = GenerageMapKeyUtils.createMapKey(entity.getFactoryCode(), String.valueOf(entity.getStockDate()), entity.getMaterialCode());
                    if (existsMap.containsKey(mapKey)) {
                        MdmTreadStock existsData = existsMap.get(mapKey);
                        entity.setId(existsData.getId());
                    }
                    insertOrUpdateList.add(entity);
                }

                baseDao.saveBatch(insertOrUpdateList);
            }
        } finally {
            DynamicDataSourceContextHolder.poll();
        }
        return AjaxResult.success();
    }
}
