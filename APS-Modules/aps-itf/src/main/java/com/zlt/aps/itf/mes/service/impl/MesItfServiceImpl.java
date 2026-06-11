package com.zlt.aps.itf.mes.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.dynamic.datasource.toolkit.DynamicDataSourceContextHolder;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zlt.aps.autoLogin.feign.FeignTokenHelper;
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
import com.zlt.aps.itf.constant.SysCode;
import com.zlt.aps.itf.mes.enums.ItfSyncKeyEnum;
import com.zlt.aps.itf.mes.mapper.MesItfMapper;
import com.zlt.aps.itf.mes.mapper.MoldAlterPlanIssueMapper;
import com.zlt.aps.itf.mes.mapper.MesViewMapper;
import com.zlt.aps.itf.mes.service.IPrecisionPlanIssueService;
import com.zlt.aps.itf.mes.service.MesItfService;
import com.zlt.aps.itf.mes.vo.MoldAlterPlanIssue;
import com.zlt.aps.itf.scm.service.ScmItfService;
import com.zlt.aps.itf.vo.*;
import com.zlt.sync.handle.SyncDataHandle;
import com.zlt.sync.povo.SyncParamsVO;
import com.zlt.sync.service.SyncDataLogsService;
import com.zlt.aps.lh.api.domain.entity.*;
import com.zlt.aps.lh.api.constant.LhScheduleParamConstant;
import com.zlt.aps.maindata.enums.MonthPlanEnums;
import com.zlt.aps.maindata.mapper.*;
import com.zlt.aps.maindata.service.IFactoryParamService;
import com.zlt.aps.maindata.service.IMdmProductModelRelationService;
import com.zlt.aps.maindata.service.IMdmSkuStructureRefService;
import com.zlt.aps.maindata.utils.ScmListUtils;
import com.zlt.aps.mdm.api.domain.entity.MdmMoldAlterPlan;
import com.zlt.aps.mp.api.domain.entity.*;
import com.zlt.aps.cx.api.domain.entity.CxMachineOnlineInfo;
import com.zlt.aps.cx.api.domain.entity.CxStructureTreadConfig;
import com.zlt.aps.cx.api.domain.entity.CxMesStock;
import com.zlt.aps.cx.api.domain.entity.CxStock;
import com.zlt.aps.cx.api.domain.entity.CxScheFinishQty;
import com.zlt.aps.cx.api.domain.entity.CxDayFinishQty;
import com.zlt.aps.cx.api.service.ICxMesSyncRemoteService;
import com.zlt.aps.lh.api.service.ILhMesSyncRemoteService;
import com.zlt.aps.lh.api.service.ILhChipStockRemoteService;
import com.zlt.aps.lh.api.domain.entity.LhChipStock;
import com.zlt.aps.lh.api.service.ILhPrecisionPlanRemoteService;
import com.zlt.aps.cx.api.service.ICxPrecisionPlanRemoteService;

import com.zlt.aps.utils.GenerageMapKeyUtils;
import com.zlt.core.dao.basedao.BaseDao;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.ParseException;
import cn.hutool.core.date.DateUtil;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
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
    private MoldAlterPlanIssueMapper moldAlterPlanIssueMapper;
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

    @Autowired
    private MdmDevMaintenancePlanEntityMapper devMaintenancePlanEntityMapper;

    @Autowired
    private ICxMesSyncRemoteService cxMesSyncRemoteService;

    @Autowired
    private ILhMesSyncRemoteService lhMesSyncRemoteService;

    @Autowired
    private ILhChipStockRemoteService lhChipStockRemoteService;

    @Autowired
    private ILhPrecisionPlanRemoteService lhPrecisionPlanRemoteService;

    @Autowired
    private IPrecisionPlanIssueService precisionPlanIssueService;

    @Autowired
    private ICxPrecisionPlanRemoteService cxPrecisionPlanRemoteService;


    @Autowired
    private MdmTreadStockEntityMapper treadStockEntityMapper;

    @Autowired
    private IMdmProductModelRelationService iMdmProductModelRelationService;

    @Autowired
    private IMdmSkuStructureRefService iMdmSkuStructureRefService;

    @Autowired
    private ScmItfService scmItfService;

    @Autowired
    private SyncDataHandle syncDataHandle;

    @Autowired
    private SyncDataLogsService syncDataLogsService;

    @Autowired
    private MpMonthPlanMonitorEntityMapper mpMonthPlanMonitorEntityMapper;

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
     * 采用逻辑删除后插入模式
     * @param cxMachineOnlineInfo 参数
     * @return 结果
     */
    @Override
    public AjaxResult syncMachineOnlineInfo(CxMachineOnlineInfo cxMachineOnlineInfo) {
        DynamicDataSourceContextHolder.push(DataSource.MES);
        List<CxMachineOnlineInfo> syncList = mesItfMapper.selectCxMachineOnlineSyncList(cxMachineOnlineInfo);
        DynamicDataSourceContextHolder.poll();

        if (CollectionUtils.isEmpty(syncList)) {
            log.warn("成型在机同步：MES中间表CX_MACHINE_ONLINE_SYNC查询结果为空，factoryCode={}", cxMachineOnlineInfo.getFactoryCode());
            return AjaxResult.success("MES中间表无数据可同步");
        }

        List<CxMachineOnlineInfo> insertList = new ArrayList<>();
        for (CxMachineOnlineInfo info : syncList) {
            CxMachineOnlineInfo entity = new CxMachineOnlineInfo();
            BeanUtils.copyProperties(info, entity);
            entity.setCreateBy("MES");
            entity.setUpdateBy("MES");
            entity.setCreateTime(DateUtils.getNowDate());
            entity.setUpdateTime(DateUtils.getNowDate());
            insertList.add(entity);
        }

        try {
            String factoryCode = cxMachineOnlineInfo.getFactoryCode();
            Date onlineDate = insertList.stream().map(CxMachineOnlineInfo::getOnlineDate).filter(Objects::nonNull).findFirst().orElse(DateUtils.getNowDate());
            String onlineDateStr = DateUtil.formatDate(onlineDate);
            log.info("成型在机同步：开始同步，factoryCode={}, onlineDate={}, 待插入数量={}", factoryCode, onlineDateStr, insertList.size());

            FeignTokenHelper.runWithToken(() -> {
                cxMesSyncRemoteService.logicDeleteAndSaveMachineOnlineInfo(factoryCode, onlineDateStr, "MES", insertList);
            });

            log.info("成型在机同步：同步完成，factoryCode={}, 插入数量={}", factoryCode, insertList.size());
        } catch (Exception e) {
            log.error("成型在机同步：Feign调用异常，factoryCode={}, 待插入数量={}", cxMachineOnlineInfo.getFactoryCode(), insertList.size(), e);
            return AjaxResult.error("成型在机同步失败：" + e.getMessage());
        }
        return AjaxResult.success();
    }

    /**
     * 同步硫化在机数据
     * 采用逻辑删除后插入模式，删除和插入在同一事务中执行，保证原子性
     * @param lhMachineOnlineInfo 参数
     * @return 结果
     */
    @Override
    public AjaxResult syncLhMachineOnlineInfo(LhMachineOnlineInfo lhMachineOnlineInfo) {
        DynamicDataSourceContextHolder.push(DataSource.MES);
        List<LhMachineOnlineInfo> syncList = mesItfMapper.selectLhMachineOnlineSyncList(lhMachineOnlineInfo);
        DynamicDataSourceContextHolder.poll();

        if (CollectionUtils.isEmpty(syncList)) {
            log.warn("硫化在机同步：MES中间表LH_MACHINE_ONLINE_SYNC查询结果为空，factoryCode={}", lhMachineOnlineInfo.getFactoryCode());
            return AjaxResult.success("MES中间表无数据可同步");
        }

        List<LhMachineOnlineInfo> insertList = new ArrayList<>();
        for (LhMachineOnlineInfo info : syncList) {
            LhMachineOnlineInfo entity = new LhMachineOnlineInfo();
            BeanUtils.copyProperties(info, entity);
            entity.setCreateBy("MES");
            entity.setUpdateBy("MES");
            entity.setCreateTime(DateUtils.getNowDate());
            entity.setUpdateTime(DateUtils.getNowDate());
            insertList.add(entity);
        }

        try {
            String factoryCode = lhMachineOnlineInfo.getFactoryCode();
            Date onlineDate = insertList.stream().map(LhMachineOnlineInfo::getOnlineDate).filter(Objects::nonNull).findFirst().orElse(DateUtils.getNowDate());
            String onlineDateStr = DateUtil.formatDate(onlineDate);
            log.info("硫化在机同步：开始同步，factoryCode={}, onlineDate={}, 待插入数量={}", factoryCode, onlineDateStr, insertList.size());

            FeignTokenHelper.runWithToken(() -> {
                lhMesSyncRemoteService.logicDeleteAndSaveMachineOnlineInfo(factoryCode, onlineDateStr, "MES", insertList);
            });

            log.info("硫化在机同步：同步完成，factoryCode={}, 插入数量={}", factoryCode, insertList.size());
        } catch (Exception e) {
            log.error("硫化在机同步：Feign调用异常，factoryCode={}, 待插入数量={}", lhMachineOnlineInfo.getFactoryCode(), insertList.size(), e);
            return AjaxResult.error("硫化在机同步失败：" + e.getMessage());
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
        // 查询MES中间表指定精度类型的最大版本号，只同步最新版本的数据
        String precisionType = syncDataLogs != null ? syncDataLogs.getPrecisionType() : null;
        DynamicDataSourceContextHolder.push(DataSource.MES);
        String maxVersion = mesItfMapper.selectMaxDataVersionFromMes(precisionType);
        DynamicDataSourceContextHolder.poll();

        if (maxVersion != null && !maxVersion.isEmpty()) {
            if (syncDataLogs == null) {
                syncDataLogs = new AuxReqSyncDataLogs();
            }
            syncDataLogs.setDataVersion(maxVersion);
            log.info("同步设备保养计划，精度类型={}，最新版本号={}", precisionType, maxVersion);
        } else {
            log.info("MES中间表无设备保养计划版本数据，精度类型={}", precisionType);
        }

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
            List<MdmDevMaintenancePlan> insertOrUpdateList = null;
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

                insertOrUpdateList = new ArrayList<>();
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

            if (!insertOrUpdateList.isEmpty()) {
                try {
                    List<Long> lhIds = new ArrayList<>();
                    List<Long> cx15Ids = new ArrayList<>();
                    String filterPrecisionType = syncDataLogs.getPrecisionType();

                    for (MdmDevMaintenancePlan plan : insertOrUpdateList) {
                        if ("硫化精度".equals(plan.getPrecisionType()) && plan.getId() != null) {
                            lhIds.add(plan.getId());
                        } else if ("成型精度15天".equals(plan.getPrecisionType()) && plan.getId() != null) {
                            cx15Ids.add(plan.getId());
                        }
                    }

                    if (StringUtils.isBlank(filterPrecisionType) || "硫化精度".equals(filterPrecisionType)) {
                        if (!lhIds.isEmpty()) {
                            FeignTokenHelper.runWithToken(() -> {
                                try {
                                    lhPrecisionPlanRemoteService.generateFromMaintenancePlan(lhIds);
                                } catch (Exception e) {
                                    log.error("自动生成并推算硫化精度计划失败", e);
                                }
                            });
                        }
                    }

                    if (StringUtils.isBlank(filterPrecisionType) || "成型精度15天".equals(filterPrecisionType)) {
                        if (!cx15Ids.isEmpty()) {
                            FeignTokenHelper.runWithToken(() -> {
                                try {
                                    cxPrecisionPlanRemoteService.generateFromMaintenancePlan(cx15Ids, 15);
                                } catch (Exception e) {
                                    log.error("自动生成并推算成型精度计划（15天）失败", e);
                                }
                            });
                        }
                    }
                } catch (Exception e) {
                    log.error("自动生成并推算精度计划失败", e);
                }
            }
        } finally {
            DynamicDataSourceContextHolder.poll();
        }
        return AjaxResult.success();
    }

    @Override
    public AjaxResult syncDevMaintenancePlanOnly(AuxReqSyncDataLogs syncDataLogs) {
        // 查询MES中间表指定精度类型的最大版本号，只同步最新版本的数据
        String precisionType = syncDataLogs != null ? syncDataLogs.getPrecisionType() : null;
        DynamicDataSourceContextHolder.push(DataSource.MES);
        String maxVersion = mesItfMapper.selectMaxDataVersionFromMes(precisionType);
        DynamicDataSourceContextHolder.poll();

        if (maxVersion != null && !maxVersion.isEmpty()) {
            if (syncDataLogs == null) {
                syncDataLogs = new AuxReqSyncDataLogs();
            }
            syncDataLogs.setDataVersion(maxVersion);
            log.info("仅同步设备保养计划（不触发生成），精度类型={}，最新版本号={}", precisionType, maxVersion);
        } else {
            log.info("MES中间表无设备保养计划版本数据，精度类型={}", precisionType);
        }

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

            log.info("仅同步设备保养计划完成（不触发生成精度计划），精度类型={}，同步{}条", precisionType, syncList.size());
        } finally {
            DynamicDataSourceContextHolder.poll();
        }
        return AjaxResult.success();
    }


    /**
     * 同步模具清洗预警计划
     * 采用更新删除标识模式，而不是先删后插
     * 只抓取MES最新版本号的数据进行同步，更新时版本号也同步更新
     * 同步预警数据完成后自动触发模具清洗计划的增量同步
     * @param syncDataLogs 同步参数
     * @return 结果
     */
    @Override
    public AjaxResult syncMouldCleanWarn(AuxReqSyncDataLogs syncDataLogs) {
        // 查询MES中间表模具清洗预警计划的最大版本号，只同步最新版本的数据
        String factoryCode = syncDataLogs != null ? syncDataLogs.getFactoryCode() : null;
        DynamicDataSourceContextHolder.push(DataSource.MES);
        String maxVersion = mesItfMapper.selectMaxDataVersionFromMouldCleanPlan(factoryCode);
        DynamicDataSourceContextHolder.poll();

        if (maxVersion != null && !maxVersion.isEmpty()) {
            if (syncDataLogs == null) {
                syncDataLogs = new AuxReqSyncDataLogs();
            }
            syncDataLogs.setDataVersion(maxVersion);
            log.info("同步模具清洗预警计划，最新版本号={}", maxVersion);
        } else {
            log.info("MES中间表无模具清洗预警计划版本数据");
            return AjaxResult.success("MES中间表无模具清洗预警计划版本数据");
        }

        DynamicDataSourceContextHolder.push(DataSource.MES);
        List<MouldCleanPlanVo> syncList = mesItfMapper.selectMouldCleanPlanList(syncDataLogs);
        DynamicDataSourceContextHolder.poll();

        Map<String, MouldCleanPlanVo> groupMap = syncList.stream()
                .collect(Collectors.toMap(
                        item -> item.getFactoryCode() + "|" + item.getLhCode(),
                        Function.identity(),
                        (v1, v2) -> v1
                ));
        syncList = new ArrayList<>(groupMap.values());

        List<LhMouldCleanWarn> insertOrUpdateList = new ArrayList<>();
        for (MouldCleanPlanVo item : syncList) {
            LhMouldCleanWarn entity = new LhMouldCleanWarn();
            entity.setLhCode(item.getLhCode());
            entity.setFactoryCode(item.getFactoryCode());
            entity.setCompanyCode(item.getCompanyCode());
            entity.setDataVersion(item.getDataVersion());
//            entity.setRemark(item.getRemark());
            entity.setCreateBy("MES");
            entity.setUpdateBy("MES");
            entity.setCreateTime(DateUtils.getNowDate());
            entity.setUpdateTime(DateUtils.getNowDate());

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

            insertOrUpdateList.add(entity);
        }

        if (CollectionUtils.isNotEmpty(insertOrUpdateList)) {
            FeignTokenHelper.runWithToken(() -> {
                List<LhMouldCleanWarn> existsList = lhMesSyncRemoteService.selectMouldCleanWarnExists(insertOrUpdateList);
                Map<String, LhMouldCleanWarn> existsMap = new HashMap<>(16);
                if (CollectionUtils.isNotEmpty(existsList)) {
                    existsMap = existsList.stream()
                            .collect(Collectors.toMap(
                                    item -> GenerageMapKeyUtils.createMapKey(item.getFactoryCode(), item.getLhCode()),
                                    Function.identity(),
                                    (v1, v2) -> v1
                            ));
                }

                for (LhMouldCleanWarn entity : insertOrUpdateList) {
                    String mapKey = GenerageMapKeyUtils.createMapKey(entity.getFactoryCode(), entity.getLhCode());
                    if (existsMap.containsKey(mapKey)) {
                        LhMouldCleanWarn existsData = existsMap.get(mapKey);
                        entity.setId(existsData.getId());
                    }
                }

                List<List<LhMouldCleanWarn>> splitList = ScmListUtils.getSplitList(insertOrUpdateList, 1000);
                for (List<LhMouldCleanWarn> saveList : splitList) {
                    lhMesSyncRemoteService.saveMouldCleanWarnBatch(saveList);
                }
            });

            // 预警数据同步完成后，自动触发模具清洗计划的增量同步
            try {
                FeignTokenHelper.runWithToken(() -> {
                    try {
                        AjaxResult planResult = lhMesSyncRemoteService.syncMouldCleanPlanFromWarn();
                        log.info("自动同步模具清洗计划结果：{}", planResult != null ? planResult.get("msg") : "null");
                    } catch (Exception e) {
                        log.error("自动同步模具清洗计划失败", e);
                    }
                });
            } catch (Exception e) {
                log.error("自动同步模具清洗计划异常", e);
            }
        }
        return AjaxResult.success();
    }

    /**
     * 同步胶囊已使用次数
     * 采用逻辑删除后插入模式
     * @param syncDataLogs 同步参数
     * @return 结果
     */
    @Override
    public AjaxResult syncLhRepairCapsule(AuxReqSyncDataLogs syncDataLogs) {
        DynamicDataSourceContextHolder.push(DataSource.MES);
        List<LhRepairCapsuleVo> syncList = mesItfMapper.selectLhRepairCapsuleList(syncDataLogs);
        DynamicDataSourceContextHolder.poll();

        Map<String, LhRepairCapsuleVo> groupMap = syncList.stream()
                .collect(Collectors.toMap(
                        item -> item.getFactoryCode() + "|" + item.getLhCode(),
                        Function.identity(),
                        (v1, v2) -> v1
                ));
        syncList = new ArrayList<>(groupMap.values());

        if (CollectionUtils.isEmpty(syncList)) {
            log.warn("胶囊已使用次数同步：MES中间表查询结果为空，factoryCode={}", syncDataLogs.getFactoryCode());
            return AjaxResult.success("MES中间表无数据可同步");
        }

        List<LhRepairCapsule> insertList = new ArrayList<>();
        for (LhRepairCapsuleVo item : syncList) {
            LhRepairCapsule entity = new LhRepairCapsule();
            entity.setLhCode(item.getLhCode());
            entity.setMaterialCode(item.getMaterialCode());
            entity.setReplaceCapsuleCount(item.getReplaceCapsuleCount());
            entity.setReplaceCapsuleCount2(item.getReplaceCapsuleCount2());
            entity.setBrand(item.getBrand());
            entity.setCompanyCode(FactoryConstant.DEFAULT_FACTORY_CODE);
            entity.setFactoryCode(FactoryConstant.DEFAULT_FACTORY_CODE);
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

        try {
            String factoryCode = syncDataLogs.getFactoryCode();
            Date obtainTime = insertList.stream().map(LhRepairCapsule::getObtainTime).filter(Objects::nonNull).findFirst().orElse(DateUtils.getNowDate());
            String obtainTimeStr = DateUtil.formatDate(obtainTime);
            log.info("胶囊已使用次数同步：开始同步，factoryCode={}, obtainTime={}, 待插入数量={}", factoryCode, obtainTimeStr, insertList.size());

            FeignTokenHelper.runWithToken(() -> {
                lhMesSyncRemoteService.logicDeleteAndSaveRepairCapsule(factoryCode, obtainTimeStr, "MES", insertList);
            });

            log.info("胶囊已使用次数同步：同步完成，factoryCode={}, 插入数量={}", factoryCode, insertList.size());
        } catch (Exception e) {
            log.error("胶囊已使用次数同步：Feign调用异常，factoryCode={}, 待插入数量={}", syncDataLogs.getFactoryCode(), insertList.size(), e);
            return AjaxResult.error("胶囊已使用次数同步失败：" + e.getMessage());
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
        List<StructureTreadConfigVo> syncList = mesItfMapper.selectStructureTreadConfigList(syncDataLogs);
        DynamicDataSourceContextHolder.poll();

        Map<String, StructureTreadConfigVo> groupMap = syncList.stream()
                .collect(Collectors.toMap(
                        item -> item.getFactoryCode() + "|" + item.getStructureCode(),
                        Function.identity(),
                        (v1, v2) -> v1
                ));
        syncList = new ArrayList<>(groupMap.values());

        List<CxStructureTreadConfig> insertOrUpdateList = new ArrayList<>();
        for (StructureTreadConfigVo item : syncList) {
            CxStructureTreadConfig entity = new CxStructureTreadConfig();
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

            insertOrUpdateList.add(entity);
        }

        if (CollectionUtils.isNotEmpty(insertOrUpdateList)) {
            FeignTokenHelper.runWithToken(() -> {
                List<CxStructureTreadConfig> existsList = cxMesSyncRemoteService.selectStructureTreadConfigExists(insertOrUpdateList);
                Map<String, CxStructureTreadConfig> existsMap = new HashMap<>(16);
                if (CollectionUtils.isNotEmpty(existsList)) {
                    existsMap = existsList.stream()
                            .collect(Collectors.toMap(
                                    item -> GenerageMapKeyUtils.createMapKey(item.getFactoryCode(), item.getStructureCode()),
                                    Function.identity(),
                                    (v1, v2) -> v1
                            ));
                }

                for (CxStructureTreadConfig entity : insertOrUpdateList) {
                    String mapKey = GenerageMapKeyUtils.createMapKey(entity.getFactoryCode(), entity.getStructureCode());
                    if (existsMap.containsKey(mapKey)) {
                        CxStructureTreadConfig existsData = existsMap.get(mapKey);
                        entity.setId(existsData.getId());
                    }
                }

                List<List<CxStructureTreadConfig>> splitList = ScmListUtils.getSplitList(insertOrUpdateList, 1000);
                for (List<CxStructureTreadConfig> saveList : splitList) {
                    cxMesSyncRemoteService.saveStructureTreadConfigBatch(saveList);
                }
            });
        }
        return AjaxResult.success();
    }

    /**
     * 同步生胎库存
     * T_CX_STOCK：采用逻辑删除+插入方案
     *   步骤1：逻辑删除该分厂下数据来源为MES的所有库存数据（IS_DELETE置为1）
     *   步骤2：将MES最新库存数据批量插入（新记录，IS_DELETE=0）
     *   APS有(dataSource=MANUAL) → 完全不动
     * @param syncDataLogs 同步参数
     * @return 结果
     */
    @Override
    public AjaxResult syncMesCxStock(AuxReqSyncDataLogs syncDataLogs) {
        DynamicDataSourceContextHolder.push(DataSource.MES);
        List<CxMesStock> syncList = mesItfMapper.selectMesCxStockList(syncDataLogs);
        DynamicDataSourceContextHolder.poll();

        Map<String, CxMesStock> groupMap = syncList.stream()
                .collect(Collectors.toMap(
                        item -> item.getFactoryCode() + "|" + item.getEmbryoCode(),
                        Function.identity(),
                        (v1, v2) -> v1
                ));
        syncList = new ArrayList<>(groupMap.values());

        if (CollectionUtils.isEmpty(syncList)) {
            log.warn("生胎库存同步：MES中间表查询结果为空，factoryCode={}", syncDataLogs.getFactoryCode());
            return AjaxResult.success("MES中间表无数据可同步");
        }

        String factoryCode = StringUtils.isBlank(syncDataLogs.getFactoryCode()) ? FactoryConstant.DEFAULT_FACTORY_CODE : syncDataLogs.getFactoryCode();

        List<CxStock> cxStockInsertList = syncList.stream().map(item -> {
            CxStock cxStock = new CxStock();
            cxStock.setFactoryCode(StringUtils.isBlank(item.getFactoryCode()) ? FactoryConstant.DEFAULT_FACTORY_CODE : item.getFactoryCode());
            cxStock.setStockDate(item.getStockDate());
            cxStock.setEmbryoCode(item.getEmbryoCode());
            cxStock.setStockNum(item.getStockNum() != null ? item.getStockNum().intValue() : 0);
            cxStock.setDataSource(ApsConstant.DATA_SOURCE_MES);
            cxStock.setCreateBy("MES");
            cxStock.setUpdateBy("MES");
            cxStock.setCreateTime(DateUtils.getNowDate());
            cxStock.setUpdateTime(DateUtils.getNowDate());
            return cxStock;
        }).collect(Collectors.toList());

        try {
            log.info("生胎库存同步：开始同步，factoryCode={}, 待插入数量={}", factoryCode, cxStockInsertList.size());
            Date stockDate = cxStockInsertList.stream().map(CxStock::getStockDate).filter(Objects::nonNull).findFirst().orElse(DateUtils.getNowDate());
            String stockDateStr = DateUtil.formatDate(stockDate);

            FeignTokenHelper.runWithToken(() -> {
                cxMesSyncRemoteService.logicDeleteAndSaveCxStockByDataSource(factoryCode, ApsConstant.DATA_SOURCE_MES, stockDateStr, "MES", cxStockInsertList);
            });

            log.info("生胎库存同步：同步完成，factoryCode={}, 插入数量={}", factoryCode, cxStockInsertList.size());
        } catch (Exception e) {
            log.error("生胎库存同步：Feign调用异常，factoryCode={}, 待插入数量={}", factoryCode, cxStockInsertList.size(), e);
            return AjaxResult.error("生胎库存同步失败：" + e.getMessage());
        }
        return AjaxResult.success();
    }

    /**
     * 同步成型排程完成量
     * 采用逻辑删除后插入模式
     * 同步完成后回写成型排程结果表各班次完成量
     * @param syncDataLogs 同步参数
     * @return 结果
     */
    @Override
    public AjaxResult syncCxClassShiftFinishQty(AuxReqSyncDataLogs syncDataLogs) {
        DynamicDataSourceContextHolder.push(DataSource.MES);
        List<CxScheFinishQty> syncList = mesItfMapper.selectCxClassShiftFinishQtyList(syncDataLogs);
        DynamicDataSourceContextHolder.poll();

        if (CollectionUtils.isEmpty(syncList)) {
            log.warn("成型排程完成量同步：MES中间表查询结果为空，factoryCode={}", syncDataLogs.getFactoryCode());
            return AjaxResult.success("MES中间表无数据可同步");
        }

        List<CxScheFinishQty> insertList = new ArrayList<>();
        for (CxScheFinishQty item : syncList) {
            CxScheFinishQty entity = new CxScheFinishQty();
            BeanUtils.copyProperties(item, entity);
            entity.setCreateBy("MES");
            entity.setUpdateBy("MES");
            entity.setCreateTime(DateUtils.getNowDate());
            entity.setUpdateTime(DateUtils.getNowDate());
            entity.setIsDelete(0);
            insertList.add(entity);
        }

        try {
            String factoryCode = syncDataLogs.getFactoryCode();
            Date scheduleDate = insertList.stream().map(CxScheFinishQty::getScheduleDate).filter(Objects::nonNull).findFirst().orElse(DateUtils.getNowDate());
            String scheduleDateStr = DateUtil.formatDate(scheduleDate);
            log.info("成型排程完成量同步：开始同步，factoryCode={}, scheduleDate={}, 待插入数量={}", factoryCode, scheduleDateStr, insertList.size());

            FeignTokenHelper.runWithToken(() -> {
                cxMesSyncRemoteService.logicDeleteAndSaveScheFinishQty(factoryCode, scheduleDateStr, "MES", insertList);
            });

            log.info("成型排程完成量同步：同步完成，factoryCode={}, 插入数量={}", factoryCode, insertList.size());
        } catch (Exception e) {
            log.error("成型排程完成量同步：Feign调用异常，factoryCode={}, 待插入数量={}", syncDataLogs.getFactoryCode(), insertList.size(), e);
            return AjaxResult.error("成型排程完成量同步失败：" + e.getMessage());
        }

        try {
            FeignTokenHelper.runWithToken(() -> {
                cxMesSyncRemoteService.writeBackScheduleResultFinishQty(insertList);
            });
        } catch (Exception e) {
            log.error("【成型排程完成量回写】回写成型排程结果表完成量异常", e);
        }
        return AjaxResult.success();
    }

    /**
     * 同步硫化排程完成量
     * 采用逻辑删除后插入模式
     * @param syncDataLogs 同步参数
     * @return 结果
     */
    @Override
    public AjaxResult syncLhClassShiftFinishQty(AuxReqSyncDataLogs syncDataLogs) {
        DynamicDataSourceContextHolder.push(DataSource.MES);
        List<LhScheFinishQty> syncList = mesItfMapper.selectLhClassShiftFinishQtyList(syncDataLogs);
        DynamicDataSourceContextHolder.poll();

        if (CollectionUtils.isEmpty(syncList)) {
            log.warn("硫化排程完成量同步：MES中间表查询结果为空，factoryCode={}", syncDataLogs.getFactoryCode());
            return AjaxResult.success("MES中间表无数据可同步");
        }

        List<LhScheFinishQty> insertList = new ArrayList<>();
        for (LhScheFinishQty item : syncList) {
            LhScheFinishQty entity = new LhScheFinishQty();
            BeanUtils.copyProperties(item, entity);
            entity.setCreateBy("MES");
            entity.setUpdateBy("MES");
            entity.setCreateTime(DateUtils.getNowDate());
            entity.setUpdateTime(DateUtils.getNowDate());
            entity.setIsDelete(0);
            insertList.add(entity);
        }

        try {
            String factoryCode = syncDataLogs.getFactoryCode();
            Date scheduleDate = insertList.stream().map(LhScheFinishQty::getScheduleDate).filter(Objects::nonNull).findFirst().orElse(DateUtils.getNowDate());
            String scheduleDateStr = DateUtil.formatDate(scheduleDate);
            log.info("硫化排程完成量同步：开始同步，factoryCode={}, scheduleDate={}, 待插入数量={}", factoryCode, scheduleDateStr, insertList.size());

            FeignTokenHelper.runWithToken(() -> {
                lhMesSyncRemoteService.logicDeleteAndSaveScheFinishQty(factoryCode, scheduleDateStr, "MES", insertList);
            });

            log.info("硫化排程完成量同步：同步完成，factoryCode={}, 插入数量={}", factoryCode, insertList.size());
        } catch (Exception e) {
            log.error("硫化排程完成量同步：Feign调用异常，factoryCode={}, 待插入数量={}", syncDataLogs.getFactoryCode(), insertList.size(), e);
            return AjaxResult.error("硫化排程完成量同步失败：" + e.getMessage());
        }

        try {
            FeignTokenHelper.runWithToken(() -> {
                lhMesSyncRemoteService.writeBackScheduleResultFinishQty(insertList);
            });
        } catch (Exception e) {
            log.error("【硫化排程完成量回写】回写硫化排程结果表完成量异常", e);
        }
        return AjaxResult.success();
    }

    /**
     * 按上一天最新版本号同步硫化排程完成量（临时任务）
     * 逻辑同syncLhClassShiftFinishQty（抓当天最新版本），但日期条件改为上一天
     * 从MES中间表查询上一天（SCHEDULE_DATE = DATEADD(DAY, -1, GETDATE())）的硫化排程完成量数据，
     * 按排程日期+硫化机台+订单号分组取MAX(DATA_VERSION)，然后逻辑删除APS旧数据并插入新数据，最后回填排程结果
     *
     * @param syncDataLogs 同步参数
     * @return 结果
     */
    @Override
    public AjaxResult syncLhClassShiftFinishQtyByYesterday(AuxReqSyncDataLogs syncDataLogs) {
        DynamicDataSourceContextHolder.push(DataSource.MES);
        List<LhScheFinishQty> syncList = mesItfMapper.selectLhClassShiftFinishQtyByYesterday(syncDataLogs);
        DynamicDataSourceContextHolder.poll();

        if (CollectionUtils.isEmpty(syncList)) {
            log.warn("硫化排程完成量按上一天最新版本同步：MES中间表查询结果为空，factoryCode={}", syncDataLogs.getFactoryCode());
            return AjaxResult.success("MES中间表无数据可同步");
        }

        List<LhScheFinishQty> insertList = new ArrayList<>();
        for (LhScheFinishQty item : syncList) {
            LhScheFinishQty entity = new LhScheFinishQty();
            BeanUtils.copyProperties(item, entity);
            entity.setCreateBy("MES");
            entity.setUpdateBy("MES");
            entity.setCreateTime(DateUtils.getNowDate());
            entity.setUpdateTime(DateUtils.getNowDate());
            entity.setIsDelete(0);
            insertList.add(entity);
        }

        try {
            String factoryCode = syncDataLogs.getFactoryCode();
            Date scheduleDate = insertList.stream().map(LhScheFinishQty::getScheduleDate).filter(Objects::nonNull).findFirst().orElse(DateUtils.getNowDate());
            String scheduleDateStr = DateUtil.formatDate(scheduleDate);
            log.info("硫化排程完成量按上一天最新版本同步：开始同步，factoryCode={}, scheduleDate={}, 待插入数量={}", factoryCode, scheduleDateStr, insertList.size());

            FeignTokenHelper.runWithToken(() -> {
                lhMesSyncRemoteService.logicDeleteAndSaveScheFinishQty(factoryCode, scheduleDateStr, "MES", insertList);
            });

            log.info("硫化排程完成量按上一天最新版本同步：同步完成，factoryCode={}, 插入数量={}", factoryCode, insertList.size());
        } catch (Exception e) {
            log.error("硫化排程完成量按上一天最新版本同步：Feign调用异常，factoryCode={}, 待插入数量={}", syncDataLogs.getFactoryCode(), insertList.size(), e);
            return AjaxResult.error("硫化排程完成量按上一天最新版本同步失败：" + e.getMessage());
        }

        try {
            FeignTokenHelper.runWithToken(() -> {
                lhMesSyncRemoteService.writeBackScheduleResultFinishQty(insertList);
            });
        } catch (Exception e) {
            log.error("【硫化排程完成量按上一天最新版本回写】回写硫化排程结果表完成量异常", e);
        }
        return AjaxResult.success();
    }

    /**
     * 按指定版本号同步硫化排程完成量（临时任务）
     * 与原syncLhClassShiftFinishQty的区别：不限日期，按指定版本号查询MES中间表所有日期数据
     * 同步后同样回填排程结果
     * 由于指定版本可能包含多个排程日期的数据，按排程日期分组后逐组调用逻辑删除+插入
     *
     * @param dataVersion 指定版本号
     * @return 结果
     */
    @Override
    public AjaxResult syncLhClassShiftFinishQtyByVersion(String dataVersion) {
        AuxReqSyncDataLogs syncDataLogs = new AuxReqSyncDataLogs();
        syncDataLogs.setDataVersion(dataVersion);

        DynamicDataSourceContextHolder.push(DataSource.MES);
        List<LhScheFinishQty> syncList = mesItfMapper.selectLhClassShiftFinishQtyByVersion(syncDataLogs);
        DynamicDataSourceContextHolder.poll();

        if (CollectionUtils.isEmpty(syncList)) {
            log.warn("硫化排程完成量按版本号同步：MES中间表查询结果为空，dataVersion={}", dataVersion);
            return AjaxResult.success("MES中间表无数据可同步");
        }

        List<LhScheFinishQty> insertList = new ArrayList<>();
        for (LhScheFinishQty item : syncList) {
            LhScheFinishQty entity = new LhScheFinishQty();
            BeanUtils.copyProperties(item, entity);
            entity.setCreateBy("MES");
            entity.setUpdateBy("MES");
            entity.setCreateTime(DateUtils.getNowDate());
            entity.setUpdateTime(DateUtils.getNowDate());
            entity.setIsDelete(0);
            insertList.add(entity);
        }

        // 按排程日期分组，逐组同步（原逻辑按单个排程日期做逻辑删除+插入）
        Map<String, List<LhScheFinishQty>> groupByScheduleDate = insertList.stream()
                .collect(Collectors.groupingBy(item -> {
                    Date scheduleDate = item.getScheduleDate();
                    return scheduleDate != null ? DateUtil.formatDate(scheduleDate) : "unknown";
                }));

        for (Map.Entry<String, List<LhScheFinishQty>> entry : groupByScheduleDate.entrySet()) {
            String scheduleDateStr = entry.getKey();
            List<LhScheFinishQty> groupList = entry.getValue();
            String factoryCode = groupList.get(0).getFactoryCode();

            try {
                log.info("硫化排程完成量按版本号同步：开始同步，dataVersion={}, factoryCode={}, scheduleDate={}, 待插入数量={}",
                        dataVersion, factoryCode, scheduleDateStr, groupList.size());

                String finalFactoryCode = factoryCode;
                FeignTokenHelper.runWithToken(() -> {
                    lhMesSyncRemoteService.logicDeleteAndSaveScheFinishQty(finalFactoryCode, scheduleDateStr, "MES", groupList);
                });

                log.info("硫化排程完成量按版本号同步：同步完成，dataVersion={}, factoryCode={}, scheduleDate={}, 插入数量={}",
                        dataVersion, factoryCode, scheduleDateStr, groupList.size());
            } catch (Exception e) {
                log.error("硫化排程完成量按版本号同步：Feign调用异常，dataVersion={}, factoryCode={}, scheduleDate={}",
                        dataVersion, factoryCode, scheduleDateStr, e);
                return AjaxResult.error("硫化排程完成量按版本号同步失败：" + e.getMessage());
            }
        }

        // 回填排程结果
        try {
            FeignTokenHelper.runWithToken(() -> {
                lhMesSyncRemoteService.writeBackScheduleResultFinishQty(insertList);
            });
        } catch (Exception e) {
            log.error("【硫化排程完成量按版本号回写】回写硫化排程结果表完成量异常，dataVersion={}", dataVersion, e);
        }
        return AjaxResult.success();
    }

    /**
     * 同步成型排程日完成量
     * 采用逻辑删除后插入模式
     * @param syncDataLogs 同步参数
     * @return 结果
     */
    @Override
    public AjaxResult syncCxScheDayFinishQty(AuxReqSyncDataLogs syncDataLogs) {
        DynamicDataSourceContextHolder.push(DataSource.MES);
        List<CxDayFinishQty> syncList = mesItfMapper.selectCxScheDayFinishQtyList(syncDataLogs);
        DynamicDataSourceContextHolder.poll();

        if (CollectionUtils.isEmpty(syncList)) {
            log.warn("成型排程日完成量同步：MES中间表查询结果为空，factoryCode={}", syncDataLogs.getFactoryCode());
            return AjaxResult.success("MES中间表无数据可同步");
        }

        Map<String, CxDayFinishQty> groupMap = syncList.stream()
                .collect(Collectors.toMap(
                        item -> item.getFactoryCode() + "|" + item.getFinishDate() + "|" + item.getEmbryoCode() + "|" + item.getBomDataVersion(),
                        Function.identity(),
                        (v1, v2) -> v1
                ));
        syncList = new ArrayList<>(groupMap.values());

        List<CxDayFinishQty> insertList = new ArrayList<>();
        for (CxDayFinishQty item : syncList) {
            CxDayFinishQty entity = new CxDayFinishQty();
            BeanUtils.copyProperties(item, entity);
            entity.setCreateBy("MES");
            entity.setUpdateBy("MES");
            entity.setCreateTime(DateUtils.getNowDate());
            entity.setUpdateTime(DateUtils.getNowDate());
            entity.setIsDelete(0);
            insertList.add(entity);
        }

        try {
            String factoryCode = syncDataLogs.getFactoryCode();
            Date finishDate = insertList.stream().map(CxDayFinishQty::getFinishDate).filter(Objects::nonNull).findFirst().orElse(DateUtils.getNowDate());
            String finishDateStr = DateUtil.formatDate(finishDate);
            log.info("成型排程日完成量同步：开始同步，factoryCode={}, finishDate={}, 待插入数量={}", factoryCode, finishDateStr, insertList.size());

            FeignTokenHelper.runWithToken(() -> {
                cxMesSyncRemoteService.logicDeleteAndSaveDayFinishQty(factoryCode, finishDateStr, "MES", insertList);
            });

            log.info("成型排程日完成量同步：同步完成，factoryCode={}, 插入数量={}", factoryCode, insertList.size());
        } catch (Exception e) {
            log.error("成型排程日完成量同步：Feign调用异常，factoryCode={}, 待插入数量={}", syncDataLogs.getFactoryCode(), insertList.size(), e);
            return AjaxResult.error("成型排程日完成量同步失败：" + e.getMessage());
        }
        return AjaxResult.success();
    }

    /**
     * 同步硫化排程日完成量
     * 采用逻辑删除后插入模式
     * 同步完成后根据参数配置CHIP_CODE_STOCK_UPDATE里的芯片编码，过滤物料编码对应的编码数据增量更新芯片库存的完成量
     * @param syncDataLogs 同步参数
     * @return 结果
     */
    @Override
    public AjaxResult syncLhScheDayFinishQty(AuxReqSyncDataLogs syncDataLogs) {
        DynamicDataSourceContextHolder.push(DataSource.MES);
        Date nowDate = DateUtils.truncate(DateUtils.getNowDate(), Calendar.DATE);
        Date lastDate = DateUtils.addDays(nowDate, -1);
        syncDataLogs.setQueryParams(new HashMap<>());
        syncDataLogs.getQueryParams().put("finishDate", lastDate);
        List<LhDayFinishQty> syncList = mesItfMapper.selectLhScheDayFinishQtyList(syncDataLogs);
        DynamicDataSourceContextHolder.poll();

        if (CollectionUtils.isEmpty(syncList)) {
            log.warn("硫化排程日完成量同步：MES中间表查询结果为空，factoryCode={}", syncDataLogs.getFactoryCode());
            return AjaxResult.success("MES中间表无数据可同步");
        }

        Map<String, LhDayFinishQty> groupMap = syncList.stream()
                .collect(Collectors.toMap(
                        item -> item.getFactoryCode() + "|" + item.getFinishDate() + "|" + item.getMaterialCode() + "|" + item.getMesMaterialCode(),
                        Function.identity(),
                        (v1, v2) -> v1
                ));
        syncList = new ArrayList<>(groupMap.values());

        List<LhDayFinishQty> insertList = new ArrayList<>();
        for (LhDayFinishQty item : syncList) {
            LhDayFinishQty entity = new LhDayFinishQty();
            BeanUtils.copyProperties(item, entity);
            entity.setCreateBy("MES");
            entity.setUpdateBy("MES");
            entity.setCreateTime(DateUtils.getNowDate());
            entity.setUpdateTime(DateUtils.getNowDate());
            entity.setIsDelete(0);
            insertList.add(entity);
        }

        try {
            String factoryCode = syncDataLogs.getFactoryCode();
            Date finishDate = insertList.stream().map(LhDayFinishQty::getFinishDate).filter(Objects::nonNull).findFirst().orElse(DateUtils.getNowDate());
            String finishDateStr = DateUtil.formatDate(finishDate);
            log.info("硫化排程日完成量同步：开始同步，factoryCode={}, finishDate={}, 待插入数量={}", factoryCode, finishDateStr, insertList.size());

            FeignTokenHelper.runWithToken(() -> {
                lhMesSyncRemoteService.logicDeleteAndSaveDayFinishQty(factoryCode, finishDateStr, "MES", insertList);
            });

            log.info("硫化排程日完成量同步：同步完成，factoryCode={}, 插入数量={}", factoryCode, insertList.size());
        } catch (Exception e) {
            log.error("硫化排程日完成量同步：Feign调用异常，factoryCode={}, 待插入数量={}", syncDataLogs.getFactoryCode(), insertList.size(), e);
            return AjaxResult.error("硫化排程日完成量同步失败：" + e.getMessage());
        }

        try {
            MpMonthPlanMonitor paramVo = new MpMonthPlanMonitor();
            paramVo.setFactoryCode(syncDataLogs.getFactoryCode());
            paramVo.setYear(DateUtils.getYear(lastDate));
            paramVo.setMonth(DateUtils.getMonth(lastDate));
            DynamicDataSourceContextHolder.push(DataSource.APS);
            mpMonthPlanMonitorEntityMapper.updateByDayFinish(paramVo);
        } finally {
            DynamicDataSourceContextHolder.poll();
        }

        updateChipStockFinishQty(syncDataLogs.getFactoryCode(), syncList);
        return AjaxResult.success();
    }

    /**
     * 按最新版本号同步硫化排程日完成量（临时任务）
     * 与原syncLhScheDayFinishQty的区别：
     * 1. 不限日期（去掉前一天日期条件），取MES中间表最新版本号查询所有日期数据
     * 2. 按完成日期分组后逐组调用逻辑删除+插入
     * 3. 月计划监控按各日期的年月分别更新
     *
     * @return 结果
     */
    @Override
    public AjaxResult syncLhScheDayFinishQtyByLatestVersion(String dataVersion) {
        AuxReqSyncDataLogs syncDataLogs = new AuxReqSyncDataLogs();
        syncDataLogs.setDataVersion(dataVersion);
        // 不设置queryParams.finishDate，即不限日期
        DynamicDataSourceContextHolder.push(DataSource.MES);
        List<LhDayFinishQty> syncList = mesItfMapper.selectLhScheDayFinishQtyList(syncDataLogs);
        DynamicDataSourceContextHolder.poll();

        if (CollectionUtils.isEmpty(syncList)) {
            log.warn("硫化排程日完成量按最新版本号同步：MES中间表查询结果为空，dataVersion={}", dataVersion);
            return AjaxResult.success("MES中间表无数据可同步");
        }

        Map<String, LhDayFinishQty> groupMap = syncList.stream()
                .collect(Collectors.toMap(
                        item -> item.getFactoryCode() + "|" + item.getFinishDate() + "|" + item.getMaterialCode() + "|" + item.getMesMaterialCode(),
                        Function.identity(),
                        (v1, v2) -> v1
                ));
        syncList = new ArrayList<>(groupMap.values());

        List<LhDayFinishQty> insertList = new ArrayList<>();
        for (LhDayFinishQty item : syncList) {
            LhDayFinishQty entity = new LhDayFinishQty();
            BeanUtils.copyProperties(item, entity);
            entity.setCreateBy("MES");
            entity.setUpdateBy("MES");
            entity.setCreateTime(DateUtils.getNowDate());
            entity.setUpdateTime(DateUtils.getNowDate());
            entity.setIsDelete(0);
            insertList.add(entity);
        }

        // 按完成日期分组，逐组同步（原逻辑按单个完成日期做逻辑删除+插入）
        Map<String, List<LhDayFinishQty>> groupByFinishDate = insertList.stream()
                .collect(Collectors.groupingBy(item -> {
                    Date finishDate = item.getFinishDate();
                    return finishDate != null ? DateUtil.formatDate(finishDate) : "unknown";
                }));

        for (Map.Entry<String, List<LhDayFinishQty>> entry : groupByFinishDate.entrySet()) {
            String finishDateStr = entry.getKey();
            List<LhDayFinishQty> groupList = entry.getValue();
            String factoryCode = groupList.get(0).getFactoryCode();

            try {
                log.info("硫化排程日完成量按最新版本号同步：开始同步，dataVersion={}, factoryCode={}, finishDate={}, 待插入数量={}",
                        dataVersion, factoryCode, finishDateStr, groupList.size());

                String finalFactoryCode = factoryCode;
                FeignTokenHelper.runWithToken(() -> {
                    lhMesSyncRemoteService.logicDeleteAndSaveDayFinishQty(finalFactoryCode, finishDateStr, "MES", groupList);
                });

                log.info("硫化排程日完成量按最新版本号同步：同步完成，dataVersion={}, factoryCode={}, finishDate={}, 插入数量={}",
                        dataVersion, factoryCode, finishDateStr, groupList.size());
            } catch (Exception e) {
                log.error("硫化排程日完成量按最新版本号同步：Feign调用异常，dataVersion={}, factoryCode={}, finishDate={}",
                        dataVersion, factoryCode, finishDateStr, e);
                return AjaxResult.error("硫化排程日完成量按最新版本号同步失败：" + e.getMessage());
            }
        }

        // 月计划监控更新：按各完成日期的年月分别更新
        Set<String> yearMonthSet = new HashSet<>();
        for (LhDayFinishQty item : insertList) {
            if (item.getFinishDate() != null) {
                String yearMonth = DateUtils.getYear(item.getFinishDate()) + "-" + DateUtils.getMonth(item.getFinishDate());
                yearMonthSet.add(yearMonth);
            }
        }
        for (String yearMonth : yearMonthSet) {
            try {
                String[] parts = yearMonth.split("-");
                MpMonthPlanMonitor paramVo = new MpMonthPlanMonitor();
                paramVo.setFactoryCode(insertList.get(0).getFactoryCode());
                paramVo.setYear(Integer.parseInt(parts[0]));
                paramVo.setMonth(Integer.parseInt(parts[1]));
                DynamicDataSourceContextHolder.push(DataSource.APS);
                mpMonthPlanMonitorEntityMapper.updateByDayFinish(paramVo);
            } finally {
                DynamicDataSourceContextHolder.poll();
            }
        }

        // 增量更新芯片库存完成量
        updateChipStockFinishQty(insertList.get(0).getFactoryCode(), syncList);
        return AjaxResult.success();
    }

    /**
     * 根据参数配置CHIP_CODE_STOCK_UPDATE里的芯片编码，过滤物料编码对应的日完成量数据增量更新芯片库存
     * 采用增量更新模式：
     *   已存在（分厂+芯片编码匹配）：累加完成量
     *   不存在：新增记录（仅设置完成量，库存量由用户手动维护）
     * @param factoryCode 分厂编码
     * @param syncList 硫化排程日完成量列表
     */
    private void updateChipStockFinishQty(String factoryCode, List<LhDayFinishQty> syncList) {
        log.info("【芯片库存回填排查】开始处理，factoryCode={}, syncList.size={}", factoryCode, syncList.size());

        LhParams paramResult;
        try {
            paramResult = FeignTokenHelper.callWithToken(() ->
                    lhMesSyncRemoteService.selectLhParamsByCode(LhScheduleParamConstant.CHIP_CODE_STOCK_UPDATE, factoryCode));
        } catch (Exception e) {
            log.error("【芯片库存回填排查】查询硫化参数配置异常，factoryCode={}", factoryCode, e);
            return;
        }
        if (paramResult == null || StringUtils.isBlank(paramResult.getParamValue())) {
            log.warn("【芯片库存回填排查】硫化参数CHIP_CODE_STOCK_UPDATE未配置或值为空，factoryCode={}, paramResult={}", factoryCode, paramResult);
            return;
        }
        log.info("【芯片库存回填排查】硫化参数CHIP_CODE_STOCK_UPDATE配置值：factoryCode={}, paramValue={}", factoryCode, paramResult.getParamValue());

        Set<String> chipCodeSet = Arrays.stream(paramResult.getParamValue().split(","))
                .map(String::trim)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toSet());
        if (CollectionUtils.isEmpty(chipCodeSet)) {
            log.warn("【芯片库存回填排查】解析芯片编码集合为空，factoryCode={}", factoryCode);
            return;
        }
        log.info("【芯片库存回填排查】芯片编码集合：{}", chipCodeSet);

        List<String> syncMaterialCodes = syncList.stream()
                .map(LhDayFinishQty::getMaterialCode)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .collect(Collectors.toList());
        log.info("【芯片库存回填排查】syncList中物料编码列表：{}", syncMaterialCodes);

        List<String> syncMesMaterialCodes = syncList.stream()
                .map(LhDayFinishQty::getMesMaterialCode)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .collect(Collectors.toList());
        log.info("【芯片库存回填排查】syncList中MES物料编码列表：{}", syncMesMaterialCodes);

        List<String> matchedMaterialCodes = syncMaterialCodes.stream()
                .filter(chipCodeSet::contains)
                .collect(Collectors.toList());
        log.info("【芯片库存回填排查】物料编码与芯片编码集合匹配结果：matched={}, unmatched={}", matchedMaterialCodes, syncMaterialCodes.stream().filter(code -> !chipCodeSet.contains(code)).collect(Collectors.toList()));

        List<LhDayFinishQty> chipDataList = syncList.stream()
                .filter(item -> chipCodeSet.contains(item.getMaterialCode()))
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(chipDataList)) {
            log.warn("【芯片库存回填排查】过滤后芯片数据为空，无匹配的物料编码！chipCodeSet={}, syncMaterialCodes={}", chipCodeSet, syncMaterialCodes);
            return;
        }
        log.info("【芯片库存回填排查】过滤后芯片数据条数：{}", chipDataList.size());

        Map<String, Integer> chipFinishQtyMap = chipDataList.stream()
                .filter(item -> item.getDayFinishQty() != null)
                .collect(Collectors.groupingBy(
                        LhDayFinishQty::getMaterialCode,
                        Collectors.summingInt(item -> item.getDayFinishQty().intValue())
                ));
        log.info("【芯片库存回填排查】芯片完成量汇总结果：{}", chipFinishQtyMap);

        Map<String, String> chipVersionMap = chipDataList.stream()
                .filter(item -> item.getDataVersion() != null && !item.getDataVersion().isEmpty())
                .collect(Collectors.groupingBy(
                        LhDayFinishQty::getMaterialCode,
                        Collectors.collectingAndThen(
                                Collectors.maxBy(Comparator.comparing(LhDayFinishQty::getDataVersion, Comparator.nullsFirst(Comparator.naturalOrder()))),
                                opt -> opt.map(LhDayFinishQty::getDataVersion).orElse(null)
                        )
                ));

        List<LhChipStock> chipStockList = chipFinishQtyMap.entrySet().stream().map(entry -> {
            LhChipStock chipStock = new LhChipStock();
            chipStock.setFactoryCode(factoryCode);
            chipStock.setChipCode(entry.getKey());
            chipStock.setFinishQty(entry.getValue());
            chipStock.setDataVersion(chipVersionMap.get(entry.getKey()));
            return chipStock;
        }).collect(Collectors.toList());

        try {
            FeignTokenHelper.runWithToken(() -> {
                log.info("芯片库存增量更新：开始同步，factoryCode={}, 待处理数量={}", factoryCode, chipStockList.size());
                lhChipStockRemoteService.upsertFinishQty(factoryCode, chipStockList);
                log.info("芯片库存增量更新：同步完成，factoryCode={}, 处理数量={}", factoryCode, chipStockList.size());
            });
        } catch (Exception e) {
            log.error("芯片库存增量更新异常, factoryCode={}", factoryCode, e);
        }
    }

    /**
     * 模具交替计划下发到MES
     * 1. 清理中间表中同工单号的旧数据，避免脏数据残留
     * 2. 写入MES中间表MOLD_ALTER_PLAN（建在MES分库）
     * 3. 发送MQ通知MES来获取数据
     * @param moldAlterPlanList 模具交替计划列表
     * @return 结果
     */
    @Override
    public AjaxResult issueMoldAlterPlan(List<MdmMoldAlterPlan> moldAlterPlanList) {
        if (CollectionUtils.isEmpty(moldAlterPlanList)) {
            return AjaxResult.success();
        }

        // 获取下发接口版本号
        String dataVersion = syncDataHandle.getDataVersion(ItfSyncKeyEnum.MOLD_ALTER_PLAN_ISSUE.getCode());

        // 从数据中获取factoryCode和companyCode
        String factoryCode = moldAlterPlanList.get(0).getFactoryCode();
        String companyCode = moldAlterPlanList.get(0).getCompanyCode();

        // 收集工单号列表，用于清理中间表旧数据
        List<String> orderNos = moldAlterPlanList.stream()
                .map(MdmMoldAlterPlan::getOrderNo)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .collect(Collectors.toList());

        // 转换为中间表实体
        List<MoldAlterPlanIssue> issueList = new ArrayList<>();
        for (MdmMoldAlterPlan plan : moldAlterPlanList) {
            MoldAlterPlanIssue issue = new MoldAlterPlanIssue();
            BeanUtils.copyProperties(plan, issue);
            issue.setDataVersion(dataVersion);
            issueList.add(issue);
        }

        try {
            // 先清理中间表中同工单号的旧数据，避免脏数据残留导致MES消费异常
            if (CollectionUtils.isNotEmpty(orderNos) && StringUtils.isNotBlank(factoryCode)) {
                int deleted = moldAlterPlanIssueMapper.deleteByOrderNosAndFactoryCode(orderNos, factoryCode);
                if (deleted > 0) {
                    log.info("清理模具交替计划中间表旧数据, 分厂: {}, 工单号: {}, 删除数量: {}", factoryCode, orderNos, deleted);
                }
            }
            // MOLD_ALTER_PLAN表建在MES分库，使用@DS(DataSource.MES)的独立Mapper直接写入
            moldAlterPlanIssueMapper.insertMoldAlterPlanList(issueList);
        } catch (Exception e) {
            log.error("模具交替计划下发到MES中间表失败", e);
            throw e;
        }

        // 发送MQ通知MES来获取数据
        return sendMoldAlterPlanMqNotice(issueList.size(), dataVersion, factoryCode, companyCode);
    }

    /**
     * 发送模具交替计划MQ通知MES
     */
    private AjaxResult sendMoldAlterPlanMqNotice(int rowCount, String dataVersion, String factoryCode, String companyCode) {
        try {
            SyncParamsVO syncParamsVO = new SyncParamsVO();
            syncParamsVO.setSyncKey(ItfSyncKeyEnum.MOLD_ALTER_PLAN_ISSUE.getCode());
            syncParamsVO.setDataVersion(dataVersion);

            JSONObject params = new JSONObject();
            params.put("rowCount", rowCount);
            syncParamsVO.setParams(params);
            syncParamsVO.setDataSys(SysCode.APS);
            syncParamsVO.setDockSys(ApsConstant.DOCK_SYS_MES);
            syncParamsVO.setFactoryCode(factoryCode);
            syncParamsVO.setCompanyCode(companyCode);

            // 往消息队列发送消息
            syncDataHandle.syncNotice(syncParamsVO);

            // 取回mes的反馈结果
            SyncDataLogs logs = syncDataLogsService.getSyncDataResult(dataVersion);
            String status = logs.getStatus();
            if (ApsConstant.IS_RELEASE.equals(status)) {
                return AjaxResult.success("模具交替计划下发成功");
            } else {
                return AjaxResult.error(logs.getMsg());
            }
        } catch (Exception e) {
            log.error("模具交替计划下发MQ通知MES失败", e);
            return AjaxResult.error("模具交替计划下发MES失败：" + e.getMessage());
        }
    }

    /**
     * 同步模具交替计划完成回报
     * 采用更新删除标识模式，而不是先删后插
     * 同步完成后回填流程排程结果表的模具交替完成状态
     * @param syncDataLogs 同步参数
     * @return 结果
     */
    @Override
    public AjaxResult syncMoldAlterPlanFinish(AuxReqSyncDataLogs syncDataLogs) {
        DynamicDataSourceContextHolder.push(DataSource.MES);
        List<LhMoldAlterPlanFinish> syncList = mesItfMapper.selectMoldAlterPlanFinishList(syncDataLogs);
        DynamicDataSourceContextHolder.poll();

        Map<String, LhMoldAlterPlanFinish> groupMap = syncList.stream()
                .collect(Collectors.toMap(
                        item -> item.getFactoryCode() + "|" + item.getLhBatchNo() + "|" + item.getOrderNo() + "|" + item.getScheduleDate() + "|" + item.getLhMachineCode() + "|" + item.getLeftRightMold(),
                        Function.identity(),
                        (v1, v2) -> v1
                ));
        syncList = new ArrayList<>(groupMap.values());

        List<LhMoldAlterPlanFinish> insertOrUpdateList = new ArrayList<>();
        for (LhMoldAlterPlanFinish item : syncList) {
            LhMoldAlterPlanFinish entity = new LhMoldAlterPlanFinish();
            BeanUtils.copyProperties(item, entity);
            entity.setCreateBy("MES");
            entity.setUpdateBy("MES");

            if (entity.getIsDelete() == null) {
                entity.setIsDelete(0);
            }

            insertOrUpdateList.add(entity);
        }

        if (CollectionUtils.isNotEmpty(insertOrUpdateList)) {
            FeignTokenHelper.runWithToken(() -> {
                List<LhMoldAlterPlanFinish> existsList = lhMesSyncRemoteService.selectMoldAlterPlanFinishExists(insertOrUpdateList);
                Map<String, LhMoldAlterPlanFinish> existsMap = new HashMap<>(16);
                if (CollectionUtils.isNotEmpty(existsList)) {
                    existsMap = existsList.stream()
                            .collect(Collectors.toMap(
                                    item -> GenerageMapKeyUtils.createMapKey(item.getFactoryCode(), item.getLhBatchNo(), item.getOrderNo(), String.valueOf(item.getScheduleDate()), item.getLhMachineCode(), item.getLeftRightMold()),
                                    Function.identity(),
                                    (v1, v2) -> v1
                            ));
                }

                for (LhMoldAlterPlanFinish entity : insertOrUpdateList) {
                    String mapKey = GenerageMapKeyUtils.createMapKey(entity.getFactoryCode(), entity.getLhBatchNo(), entity.getOrderNo(), String.valueOf(entity.getScheduleDate()), entity.getLhMachineCode(), entity.getLeftRightMold());
                    if (existsMap.containsKey(mapKey)) {
                        LhMoldAlterPlanFinish existsData = existsMap.get(mapKey);
                        entity.setId(existsData.getId());
                    }
                }

                List<List<LhMoldAlterPlanFinish>> splitList = ScmListUtils.getSplitList(insertOrUpdateList, 1000);
                for (List<LhMoldAlterPlanFinish> saveList : splitList) {
                    lhMesSyncRemoteService.saveMoldAlterPlanFinishBatch(saveList);
                }

                lhMesSyncRemoteService.writeBackMouldChangePlanFinishStatus(insertOrUpdateList);
            });
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
//                map.put("STOCK_DATE", DateUtils.getNowDate("yyyy-MM-dd"));
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

    @Override
    public AjaxResult syncLhPrecisionPlanActual(AuxReqSyncDataLogs syncDataLogs) {
        // 先查询MES中间表硫化精度类型的最大版本号，只同步最新版本的数据
        DynamicDataSourceContextHolder.push(DataSource.MES);
        String maxVersion = mesItfMapper.selectMaxDataVersionFromMes("硫化精度");
        DynamicDataSourceContextHolder.poll();

        if (maxVersion == null || maxVersion.isEmpty()) {
            return AjaxResult.success("MES中间表无硫化精度版本数据");
        }
        log.info("MES中间表硫化精度最新版本号：{}", maxVersion);

        // 将最新版本号设置到查询参数中，只查该版本的数据
        if (syncDataLogs == null) {
            syncDataLogs = new AuxReqSyncDataLogs();
        }
        syncDataLogs.setDataVersion(maxVersion);

        DynamicDataSourceContextHolder.push(DataSource.MES);
        List<DevMaintenancePlan> syncList = mesItfMapper.selectLhPrecisionPlanActualList(syncDataLogs);
        DynamicDataSourceContextHolder.poll();

        if (CollectionUtils.isEmpty(syncList)) {
            return AjaxResult.success("没有需要同步的硫化精度计划实际执行日期数据");
        }

        List<java.util.Map<String, Object>> fillList = new ArrayList<>();
        for (DevMaintenancePlan mesPlan : syncList) {
            if (StringUtils.isBlank(mesPlan.getFirstWashTime()) || StringUtils.isBlank(mesPlan.getDevCode())) {
                continue;
            }

            try {
                java.util.Date actualDate = DateUtils.parseDate(mesPlan.getFirstWashTime(), "yyyy-MM-dd HH:mm:ss.SSS", "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd");
                java.util.Map<String, Object> item = new java.util.HashMap<>();
                item.put("machineCode", mesPlan.getDevCode());
                item.put("factoryCode", mesPlan.getFactoryCode());
                item.put("actualDate", actualDate);
                fillList.add(item);
            } catch (Exception e) {
                log.error("解析实际执行日期失败：机台={}, 日期={}", mesPlan.getDevCode(), mesPlan.getFirstWashTime(), e);
            }
        }

        if (!fillList.isEmpty()) {
            try {
                FeignTokenHelper.runWithToken(() -> {
                    lhPrecisionPlanRemoteService.batchFillActualDateAndGenerateNext(fillList);
                });
            } catch (Exception e) {
                log.error("批量MES回填硫化精度计划实际执行日期失败", e);
                return AjaxResult.error("批量回填失败：" + e.getMessage());
            }
        }
        return AjaxResult.success();
    }

    @Override
    public AjaxResult issueLhPrecisionPlan(String factoryCode) {
        log.info("开始下发硫化精度计划到MES：分厂={}", factoryCode);

        try {
            String queryFactoryCode = factoryCode;
            if (org.apache.commons.lang.StringUtils.isBlank(queryFactoryCode)) {
                queryFactoryCode = FactoryConstant.DEFAULT_FACTORY_CODE;
            }
            String finalQueryFactoryCode = queryFactoryCode;
            AjaxResult pendingResult = FeignTokenHelper.callWithToken(() -> lhPrecisionPlanRemoteService.listPendingIssuePlans(finalQueryFactoryCode));
            if (pendingResult == null || pendingResult.get("data") == null) {
                log.info("没有待下发的硫化精度计划数据");
                return AjaxResult.success("没有待下发的硫化精度计划数据");
            }

            List<LhPrecisionPlanIssue> pendingList =
                    JSONObject.parseArray(
                            JSONObject.toJSONString(pendingResult.get("data")),
                            LhPrecisionPlanIssue.class
                    );

            if (CollectionUtils.isEmpty(pendingList)) {
                log.info("没有待下发的硫化精度计划数据");
                return AjaxResult.success("没有待下发的硫化精度计划数据");
            }

            String companyCode = pendingList.get(0).getCompanyCode();
            return precisionPlanIssueService.issueLhPrecisionPlan(pendingList, factoryCode, companyCode);
        } catch (Exception e) {
            log.error("下发硫化精度计划到MES失败", e);
            return AjaxResult.error("下发失败：" + e.getMessage());
        }
    }

    @Override
    public AjaxResult syncAndGenerateLhPrecisionPlan(Integer year) {
        log.info("开始执行同步MES数据并生成硫化精度计划（综合接口），年度={}", year);

        StringBuilder resultMsg = new StringBuilder();
        int totalGenerated = 0;

        // 步骤1：同步MES设备保养计划到APS（仅硫化精度），同步时按最新版本号增量同步
        log.info("步骤1：同步MES设备保养计划到APS（仅硫化精度）");
        try {
            AuxReqSyncDataLogs lhSyncParam = new AuxReqSyncDataLogs();
            lhSyncParam.setPrecisionType("硫化精度");
            AjaxResult syncResult = syncDevMaintenancePlan(lhSyncParam);
            log.info("同步设备保养计划结果：{}", syncResult.get("msg"));
            resultMsg.append("同步硫化设备保养计划完成；");
        } catch (Exception e) {
            log.error("同步硫化设备保养计划失败", e);
            resultMsg.append("同步硫化设备保养计划失败：").append(e.getMessage()).append("；");
        }

        // 步骤2：同步MES硫化精度计划实际执行日期回填数据（按最新版本号增量查询）
        log.info("步骤2：同步MES硫化精度计划实际执行日期回填数据");
        try {
            AjaxResult actualResult = syncLhPrecisionPlanActual(new AuxReqSyncDataLogs());
            log.info("同步实际执行日期结果：{}", actualResult.get("msg"));
            resultMsg.append("同步实际执行日期完成；");
        } catch (Exception e) {
            log.error("同步实际执行日期失败", e);
            resultMsg.append("同步实际执行日期失败：").append(e.getMessage()).append("；");
        }

        // 步骤3：从MES同步数据生成硫化精度计划（只处理最新版本号的数据）
        log.info("步骤3：从MES同步数据生成硫化精度计划");
        try {
            AjaxResult generateResult = lhPrecisionPlanRemoteService.generatePlansFromMes(year);
            Object data = generateResult.get("data");
            int count = data != null ? Integer.parseInt(data.toString()) : 0;
            totalGenerated += count;
            log.info("从MES同步数据生成硫化精度计划{}条", count);
            resultMsg.append("从MES同步数据生成硫化精度计划").append(count).append("条；");
        } catch (Exception e) {
            log.error("从MES同步数据生成硫化精度计划失败", e);
            resultMsg.append("从MES同步数据生成硫化精度计划失败：").append(e.getMessage()).append("；");
        }

        // 步骤4：自动推算生成下一年度硫化精度计划
        log.info("步骤4：自动推算生成下一年度硫化精度计划");
        try {
            AjaxResult autoResult = lhPrecisionPlanRemoteService.autoCalculateLhPrecisionPlan(year + 1);
            Object autoData = autoResult.get("data");
            int autoCount = autoData != null ? Integer.parseInt(autoData.toString()) : 0;
            totalGenerated += autoCount;
            log.info("自动推算生成下一年度硫化精度计划{}条", autoCount);
            resultMsg.append("自动推算生成下一年度硫化精度计划").append(autoCount).append("条；");
        } catch (Exception e) {
            log.error("自动推算生成下一年度硫化精度计划失败", e);
            resultMsg.append("自动推算生成下一年度硫化精度计划失败：").append(e.getMessage()).append("；");
        }

        log.info("同步MES数据并生成硫化精度计划执行完成");
        return AjaxResult.success(resultMsg.toString(), totalGenerated);
    }

    @Override
    public AjaxResult syncAndGenerateLhPrecisionPlanByVersionPrefix(String versionPrefix, Integer year) {
        log.info("开始执行同步MES数据并生成硫化精度计划（版本前缀={}，年度={}）", versionPrefix, year);

        StringBuilder resultMsg = new StringBuilder();
        int totalGenerated = 0;

        // 步骤1：同步MES设备保养计划到APS（仅硫化精度），同步时按最新版本号增量同步
        log.info("步骤1：同步MES设备保养计划到APS（仅硫化精度）");
        try {
            AuxReqSyncDataLogs lhSyncParam = new AuxReqSyncDataLogs();
            lhSyncParam.setPrecisionType("硫化精度");
            AjaxResult syncResult = syncDevMaintenancePlan(lhSyncParam);
            log.info("同步设备保养计划结果：{}", syncResult.get("msg"));
            resultMsg.append("同步硫化设备保养计划完成；");
        } catch (Exception e) {
            log.error("同步硫化设备保养计划失败", e);
            resultMsg.append("同步硫化设备保养计划失败：").append(e.getMessage()).append("；");
        }

        // 步骤2：同步MES硫化精度计划实际执行日期回填数据（按最新版本号增量查询）
        log.info("步骤2：同步MES硫化精度计划实际执行日期回填数据");
        try {
            AjaxResult actualResult = syncLhPrecisionPlanActual(new AuxReqSyncDataLogs());
            log.info("同步实际执行日期结果：{}", actualResult.get("msg"));
            resultMsg.append("同步实际执行日期完成；");
        } catch (Exception e) {
            log.error("同步实际执行日期失败", e);
            resultMsg.append("同步实际执行日期失败：").append(e.getMessage()).append("；");
        }

        // 步骤3：按版本前缀从MES同步数据生成硫化精度计划
        log.info("步骤3：按版本前缀={}从MES同步数据生成硫化精度计划", versionPrefix);
        try {
            AjaxResult generateResult = lhPrecisionPlanRemoteService.generatePlansFromMesByVersionPrefix(versionPrefix, year);
            Object data = generateResult.get("data");
            int count = data != null ? Integer.parseInt(data.toString()) : 0;
            totalGenerated += count;
            log.info("按版本前缀={}从MES同步数据生成硫化精度计划{}条", versionPrefix, count);
            resultMsg.append("按版本前缀生成硫化精度计划").append(count).append("条；");
        } catch (Exception e) {
            log.error("按版本前缀={}从MES同步数据生成硫化精度计划失败", versionPrefix, e);
            resultMsg.append("按版本前缀生成硫化精度计划失败：").append(e.getMessage()).append("；");
        }

        // 步骤4：自动推算生成下一年度硫化精度计划
        log.info("步骤4：自动推算生成下一年度硫化精度计划");
        try {
            AjaxResult autoResult = lhPrecisionPlanRemoteService.autoCalculateLhPrecisionPlan(year + 1);
            Object autoData = autoResult.get("data");
            int autoCount = autoData != null ? Integer.parseInt(autoData.toString()) : 0;
            totalGenerated += autoCount;
            log.info("自动推算生成下一年度硫化精度计划{}条", autoCount);
            resultMsg.append("自动推算生成下一年度硫化精度计划").append(autoCount).append("条；");
        } catch (Exception e) {
            log.error("自动推算生成下一年度硫化精度计划失败", e);
            resultMsg.append("自动推算生成下一年度硫化精度计划失败：").append(e.getMessage()).append("；");
        }

        log.info("同步MES数据并生成硫化精度计划（版本前缀={}）执行完成", versionPrefix);
        return AjaxResult.success(resultMsg.toString(), totalGenerated);
    }

    @Override
    public AjaxResult syncAndGenerateLhPrecisionPlanByVersionPrefixAllVersions(String versionPrefix, Integer year) {
        log.info("开始执行同步MES数据并生成硫化精度计划（版本前缀={}，不限最大版本号，年度={}）", versionPrefix, year);

        StringBuilder resultMsg = new StringBuilder();
        int totalGenerated = 0;

        // 步骤1：同步MES设备保养计划到APS（仅硫化精度），同步时按最新版本号增量同步
        log.info("步骤1：同步MES设备保养计划到APS（仅硫化精度）");
        try {
            AuxReqSyncDataLogs lhSyncParam = new AuxReqSyncDataLogs();
            lhSyncParam.setPrecisionType("硫化精度");
            AjaxResult syncResult = syncDevMaintenancePlan(lhSyncParam);
            log.info("同步设备保养计划结果：{}", syncResult.get("msg"));
            resultMsg.append("同步硫化设备保养计划完成；");
        } catch (Exception e) {
            log.error("同步硫化设备保养计划失败", e);
            resultMsg.append("同步硫化设备保养计划失败：").append(e.getMessage()).append("；");
        }

        // 步骤2：同步MES硫化精度计划实际执行日期回填数据（按最新版本号增量查询）
        log.info("步骤2：同步MES硫化精度计划实际执行日期回填数据");
        try {
            AjaxResult actualResult = syncLhPrecisionPlanActual(new AuxReqSyncDataLogs());
            log.info("同步实际执行日期结果：{}", actualResult.get("msg"));
            resultMsg.append("同步实际执行日期完成；");
        } catch (Exception e) {
            log.error("同步实际执行日期失败", e);
            resultMsg.append("同步实际执行日期失败：").append(e.getMessage()).append("；");
        }

        // 步骤3：按版本前缀从MES同步数据生成硫化精度计划（不限最大版本号）
        log.info("步骤3：按版本前缀={}从MES同步数据生成硫化精度计划（不限最大版本号）", versionPrefix);
        try {
            AjaxResult generateResult = lhPrecisionPlanRemoteService.generatePlansFromMesByVersionPrefixAllVersions(versionPrefix, year);
            Object data = generateResult.get("data");
            int count = data != null ? Integer.parseInt(data.toString()) : 0;
            totalGenerated += count;
            log.info("按版本前缀={}从MES同步数据生成硫化精度计划{}条（不限最大版本号）", versionPrefix, count);
            resultMsg.append("按版本前缀生成硫化精度计划（不限最大版本号）").append(count).append("条；");
        } catch (Exception e) {
            log.error("按版本前缀={}从MES同步数据生成硫化精度计划失败（不限最大版本号）", versionPrefix, e);
            resultMsg.append("按版本前缀生成硫化精度计划失败（不限最大版本号）：").append(e.getMessage()).append("；");
        }

        // 步骤4：自动推算生成下一年度硫化精度计划
        log.info("步骤4：自动推算生成下一年度硫化精度计划");
        try {
            AjaxResult autoResult = lhPrecisionPlanRemoteService.autoCalculateLhPrecisionPlan(year + 1);
            Object autoData = autoResult.get("data");
            int autoCount = autoData != null ? Integer.parseInt(autoData.toString()) : 0;
            totalGenerated += autoCount;
            log.info("自动推算生成下一年度硫化精度计划{}条", autoCount);
            resultMsg.append("自动推算生成下一年度硫化精度计划").append(autoCount).append("条；");
        } catch (Exception e) {
            log.error("自动推算生成下一年度硫化精度计划失败", e);
            resultMsg.append("自动推算生成下一年度硫化精度计划失败：").append(e.getMessage()).append("；");
        }

        log.info("同步MES数据并生成硫化精度计划（版本前缀={}，不限最大版本号）执行完成", versionPrefix);
        return AjaxResult.success(resultMsg.toString(), totalGenerated);
    }

    @Override
    public AjaxResult syncAndFillActualDateByOperYear(String versionPrefix, Integer operYear) {
        log.info("开始执行临时任务：按计划时间年份同步回填实际日期并生成下一年度精度计划（版本前缀={}，计划时间年份={}）", versionPrefix, operYear);

        StringBuilder resultMsg = new StringBuilder();

        // 步骤1：同步MES设备保养计划到APS（仅硫化精度，不触发生成精度计划），确保目标年份数据在本地表中
        log.info("步骤1：同步MES设备保养计划到APS（仅硫化精度，不触发生成精度计划）");
        try {
            AuxReqSyncDataLogs lhSyncParam = new AuxReqSyncDataLogs();
            lhSyncParam.setPrecisionType("硫化精度");
            AjaxResult syncResult = syncDevMaintenancePlanOnly(lhSyncParam);
            log.info("同步设备保养计划结果：{}", syncResult.get("msg"));
            resultMsg.append("同步硫化设备保养计划完成；");
        } catch (Exception e) {
            log.error("同步硫化设备保养计划失败", e);
            resultMsg.append("同步硫化设备保养计划失败：").append(e.getMessage()).append("；");
        }

        // 步骤2：从APS本地表查指定版本前缀+硫化精度+计划时间在指定年份且有实际执行时间的数据
        log.info("步骤2：从APS本地表查版本前缀={}、计划时间在{}年且有实际执行时间的硫化精度数据", versionPrefix, operYear);
        String maxVersion = devMaintenancePlanEntityMapper.selectMaxDataVersionByPrefix("硫化精度", versionPrefix);
        if (maxVersion == null || maxVersion.isEmpty()) {
            log.warn("APS本地表中无版本前缀为{}的硫化精度版本数据，跳过回填", versionPrefix);
            return AjaxResult.error("APS本地表中无版本前缀为" + versionPrefix + "的硫化精度版本数据");
        }
        log.info("版本前缀={}的硫化精度最新版本号：{}", versionPrefix, maxVersion);

        LambdaQueryWrapper<MdmDevMaintenancePlan> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MdmDevMaintenancePlan::getPrecisionType, "硫化精度")
               .eq(MdmDevMaintenancePlan::getIsDelete, 0)
               .eq(MdmDevMaintenancePlan::getDataVersion, maxVersion)
               .isNotNull(MdmDevMaintenancePlan::getFirstWashTime)
               .apply("YEAR(oper_time) = {0}", operYear);

        List<MdmDevMaintenancePlan> mesPlans = devMaintenancePlanEntityMapper.selectList(wrapper);
        if (mesPlans == null || mesPlans.isEmpty()) {
            log.warn("APS本地表中无版本前缀={}、计划时间在{}年且有实际执行时间的硫化精度数据", versionPrefix, operYear);
            return AjaxResult.success("无符合条件的回填数据");
        }

        log.info("查到版本前缀={}、计划时间在{}年且有实际执行时间的硫化精度数据{}条", versionPrefix, operYear, mesPlans.size());

        // 步骤3：构建回填数据列表，调用batchFillActualDateAndGenerateNext回填+生成
        // 注意：MdmDevMaintenancePlan.firstWashTime是Date类型（APS本地表），不需要字符串解析
        List<java.util.Map<String, Object>> fillList = new ArrayList<>();
        for (MdmDevMaintenancePlan mesPlan : mesPlans) {
            if (mesPlan.getFirstWashTime() == null || StringUtils.isBlank(mesPlan.getDevCode())) {
                continue;
            }
            java.util.Map<String, Object> item = new java.util.HashMap<>();
            item.put("machineCode", mesPlan.getDevCode());
            item.put("factoryCode", mesPlan.getFactoryCode());
            item.put("actualDate", mesPlan.getFirstWashTime());
            fillList.add(item);
        }

        if (!fillList.isEmpty()) {
            try {
                AjaxResult fillResult = lhPrecisionPlanRemoteService.batchFillActualDateAndGenerateNext(fillList);
                Object data = fillResult != null ? fillResult.get("data") : null;
                int count = data != null ? Integer.parseInt(data.toString()) : 0;
                log.info("回填实际执行日期并生成下一年度计划{}条", count);
                resultMsg.append("回填实际执行日期并生成下一年度计划").append(count).append("条；");
            } catch (Exception e) {
                log.error("批量回填硫化精度计划实际执行日期失败", e);
                resultMsg.append("批量回填失败：").append(e.getMessage()).append("；");
            }
        } else {
            resultMsg.append("无有效的实际执行日期数据可回填；");
        }

        log.info("临时任务：按计划时间年份同步回填实际日期并生成下一年度精度计划执行完成（版本前缀={}，计划时间年份={}）", versionPrefix, operYear);
        return AjaxResult.success(resultMsg.toString());
    }

    /**
     * 临时任务：清理并重新同步所有MES历史数据（含今天）
     * 执行步骤：
     * 1. 逻辑删除APS库中今天及今天之前的所有数据（8张表）
     * 2. 从MES库重新抓取每天（含今天）最新版本数据
     * 3. 将MES数据插入到APS库
     * 涉及表：成型在机、硫化在机、胶囊已使用次数、生胎库存、成型排程完成量、成型排程日完成量、硫化排程完成量、硫化排程日完成量
     *
     * @return 执行结果
     */
    @Override
    public AjaxResult cleanAndResyncAllHistory() {
        log.info("===== 开始清理并重新同步所有MES历史数据（含今天） =====");
        StringBuilder resultMsg = new StringBuilder();

        resultMsg.append(resyncCxMachineOnlineHistory());
        resultMsg.append(resyncLhMachineOnlineHistory());
        resultMsg.append(resyncLhRepairCapsuleHistory());
        resultMsg.append(resyncCxStockHistory());
        resultMsg.append(resyncCxScheFinishQtyHistory());
        resultMsg.append(resyncCxDayFinishQtyHistory());
        resultMsg.append(resyncLhScheFinishQtyHistory());
        resultMsg.append(resyncLhDayFinishQtyHistory());

        log.info("===== 清理并重新同步所有MES历史数据（含今天）完成 =====");
        return AjaxResult.success(resultMsg.toString());
    }

    /**
     * 重新同步成型在机历史数据（含今天）
     * 1. 逻辑删除APS库今天及今天之前所有成型在机数据
     * 2. 从MES库查询每天（含今天）最新版本的成型在机数据
     * 3. 插入到APS库
     */
    private String resyncCxMachineOnlineHistory() {
        String tableName = "成型在机";
        log.info("开始重新同步{}历史数据", tableName);
        try {
            FeignTokenHelper.runWithToken(() -> {
                AjaxResult deleteResult = cxMesSyncRemoteService.logicDeleteCxMachineOnlineAllBeforeToday();
                log.info("逻辑删除{}今天及今天之前所有数据结果：{}", tableName, deleteResult.get("msg"));
            });

            DynamicDataSourceContextHolder.push(DataSource.MES);
            List<CxMachineOnlineInfo> syncList = mesItfMapper.selectCxMachineOnlineHistorySyncList(new CxMachineOnlineInfo());
            DynamicDataSourceContextHolder.poll();

            if (CollectionUtils.isEmpty(syncList)) {
                log.info("{}历史数据：MES中间表无历史数据可同步", tableName);
                return tableName + "：MES无历史数据；";
            }

            List<CxMachineOnlineInfo> insertList = new ArrayList<>();
            for (CxMachineOnlineInfo info : syncList) {
                CxMachineOnlineInfo entity = new CxMachineOnlineInfo();
                BeanUtils.copyProperties(info, entity);
                entity.setCreateBy("CLEAN_TASK");
                entity.setUpdateBy("CLEAN_TASK");
                entity.setCreateTime(DateUtils.getNowDate());
                entity.setUpdateTime(DateUtils.getNowDate());
                insertList.add(entity);
            }

            log.info("{}历史数据：从MES抓取到{}条记录，开始插入APS库", tableName, insertList.size());
            FeignTokenHelper.runWithToken(() -> {
                cxMesSyncRemoteService.saveMachineOnlineInfoBatch(insertList);
            });
            log.info("{}历史数据：重新同步完成，插入{}条", tableName, insertList.size());
            return tableName + "：插入" + insertList.size() + "条；";
        } catch (Exception e) {
            log.error("{}历史数据重新同步异常", tableName, e);
            return tableName + "：异常-" + e.getMessage() + "；";
        }
    }

    /**
     * 重新同步硫化在机历史数据（含今天）
     * 1. 逻辑删除APS库今天及今天之前所有硫化在机数据
     * 2. 从MES库查询每天（含今天）最新版本的硫化在机数据
     * 3. 插入到APS库
     */
    private String resyncLhMachineOnlineHistory() {
        String tableName = "硫化在机";
        log.info("开始重新同步{}历史数据", tableName);
        try {
            FeignTokenHelper.runWithToken(() -> {
                AjaxResult deleteResult = lhMesSyncRemoteService.logicDeleteLhMachineOnlineAllBeforeToday();
                log.info("逻辑删除{}今天及今天之前所有数据结果：{}", tableName, deleteResult.get("msg"));
            });

            DynamicDataSourceContextHolder.push(DataSource.MES);
            List<LhMachineOnlineInfo> syncList = mesItfMapper.selectLhMachineOnlineHistorySyncList(new LhMachineOnlineInfo());
            DynamicDataSourceContextHolder.poll();

            if (CollectionUtils.isEmpty(syncList)) {
                log.info("{}历史数据：MES中间表无历史数据可同步", tableName);
                return tableName + "：MES无历史数据；";
            }

            List<LhMachineOnlineInfo> insertList = new ArrayList<>();
            for (LhMachineOnlineInfo info : syncList) {
                LhMachineOnlineInfo entity = new LhMachineOnlineInfo();
                BeanUtils.copyProperties(info, entity);
                entity.setCreateBy("CLEAN_TASK");
                entity.setUpdateBy("CLEAN_TASK");
                entity.setCreateTime(DateUtils.getNowDate());
                entity.setUpdateTime(DateUtils.getNowDate());
                insertList.add(entity);
            }

            log.info("{}历史数据：从MES抓取到{}条记录，开始插入APS库", tableName, insertList.size());
            FeignTokenHelper.runWithToken(() -> {
                lhMesSyncRemoteService.saveMachineOnlineInfoBatch(insertList);
            });
            log.info("{}历史数据：重新同步完成，插入{}条", tableName, insertList.size());
            return tableName + "：插入" + insertList.size() + "条；";
        } catch (Exception e) {
            log.error("{}历史数据重新同步异常", tableName, e);
            return tableName + "：异常-" + e.getMessage() + "；";
        }
    }

    /**
     * 重新同步胶囊已使用次数历史数据（含今天）
     * 1. 逻辑删除APS库今天及今天之前所有胶囊已使用次数数据
     * 2. 从MES库查询每天（含今天）最新版本的胶囊已使用次数数据
     * 3. 插入到APS库
     */
    private String resyncLhRepairCapsuleHistory() {
        String tableName = "胶囊已使用次数";
        log.info("开始重新同步{}历史数据", tableName);
        try {
            FeignTokenHelper.runWithToken(() -> {
                AjaxResult deleteResult = lhMesSyncRemoteService.logicDeleteLhRepairCapsuleAllBeforeToday();
                log.info("逻辑删除{}今天及今天之前所有数据结果：{}", tableName, deleteResult.get("msg"));
            });

            DynamicDataSourceContextHolder.push(DataSource.MES);
            List<LhRepairCapsuleVo> syncList = mesItfMapper.selectLhRepairCapsuleList(new AuxReqSyncDataLogs());
            DynamicDataSourceContextHolder.poll();

            if (CollectionUtils.isEmpty(syncList)) {
                log.info("{}历史数据：MES中间表无历史数据可同步", tableName);
                return tableName + "：MES无历史数据；";
            }

            Map<String, LhRepairCapsuleVo> groupMap = syncList.stream()
                    .collect(Collectors.toMap(
                            item -> item.getFactoryCode() + "|" + item.getLhCode(),
                            Function.identity(),
                            (v1, v2) -> v1
                    ));
            syncList = new ArrayList<>(groupMap.values());

            List<LhRepairCapsule> insertList = new ArrayList<>();
            for (LhRepairCapsuleVo item : syncList) {
                LhRepairCapsule entity = new LhRepairCapsule();
                entity.setLhCode(item.getLhCode());
                entity.setMaterialCode(item.getMaterialCode());
                entity.setReplaceCapsuleCount(item.getReplaceCapsuleCount());
                entity.setReplaceCapsuleCount2(item.getReplaceCapsuleCount2());
                entity.setBrand(item.getBrand());
                entity.setCompanyCode(StringUtils.isBlank(item.getCompanyCode()) ? FactoryConstant.DEFAULT_COMPANY_CODE : item.getCompanyCode());
                entity.setFactoryCode(StringUtils.isBlank(item.getFactoryCode()) ? FactoryConstant.DEFAULT_FACTORY_CODE : item.getFactoryCode());
                entity.setCreateBy("CLEAN_TASK");
                entity.setUpdateBy("CLEAN_TASK");
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

            log.info("{}历史数据：从MES抓取到{}条记录，开始插入APS库", tableName, insertList.size());
            FeignTokenHelper.runWithToken(() -> {
                lhMesSyncRemoteService.saveRepairCapsuleBatch(insertList);
            });
            log.info("{}历史数据：重新同步完成，插入{}条", tableName, insertList.size());
            return tableName + "：插入" + insertList.size() + "条；";
        } catch (Exception e) {
            log.error("{}历史数据重新同步异常", tableName, e);
            return tableName + "：异常-" + e.getMessage() + "；";
        }
    }

    /**
     * 重新同步生胎库存历史数据（含今天）
     * 1. 逻辑删除APS库今天及今天之前所有生胎库存数据
     * 2. 从MES库查询每天（含今天）的生胎库存数据
     * 3. 插入到APS库
     */
    private String resyncCxStockHistory() {
        String tableName = "生胎库存";
        log.info("开始重新同步{}历史数据", tableName);
        try {
            FeignTokenHelper.runWithToken(() -> {
                AjaxResult deleteResult = cxMesSyncRemoteService.logicDeleteCxStockAllBeforeToday();
                log.info("逻辑删除{}今天及今天之前所有数据结果：{}", tableName, deleteResult.get("msg"));
            });

            DynamicDataSourceContextHolder.push(DataSource.MES);
            List<CxMesStock> syncList = mesItfMapper.selectMesCxStockHistoryList(new AuxReqSyncDataLogs());
            DynamicDataSourceContextHolder.poll();

            if (CollectionUtils.isEmpty(syncList)) {
                log.info("{}历史数据：MES中间表无历史数据可同步", tableName);
                return tableName + "：MES无历史数据；";
            }

            Map<String, CxMesStock> groupMap = syncList.stream()
                    .collect(Collectors.toMap(
                            item -> item.getFactoryCode() + "|" + item.getEmbryoCode(),
                            Function.identity(),
                            (v1, v2) -> v1
                    ));
            syncList = new ArrayList<>(groupMap.values());

            List<CxStock> cxStockInsertList = syncList.stream().map(item -> {
                CxStock cxStock = new CxStock();
                cxStock.setFactoryCode(StringUtils.isBlank(item.getFactoryCode()) ? FactoryConstant.DEFAULT_FACTORY_CODE : item.getFactoryCode());
                cxStock.setStockDate(item.getStockDate());
                cxStock.setEmbryoCode(item.getEmbryoCode());
                cxStock.setStockNum(item.getStockNum() != null ? item.getStockNum().intValue() : 0);
                cxStock.setDataSource(ApsConstant.DATA_SOURCE_MES);
                cxStock.setCreateBy("CLEAN_TASK");
                cxStock.setUpdateBy("CLEAN_TASK");
                cxStock.setCreateTime(DateUtils.getNowDate());
                cxStock.setUpdateTime(DateUtils.getNowDate());
                return cxStock;
            }).collect(Collectors.toList());

            log.info("{}历史数据：从MES抓取到{}条记录，开始插入APS库", tableName, cxStockInsertList.size());
            FeignTokenHelper.runWithToken(() -> {
                cxMesSyncRemoteService.saveCxStockBatch(cxStockInsertList);
            });
            log.info("{}历史数据：重新同步完成，插入{}条", tableName, cxStockInsertList.size());
            return tableName + "：插入" + cxStockInsertList.size() + "条；";
        } catch (Exception e) {
            log.error("{}历史数据重新同步异常", tableName, e);
            return tableName + "：异常-" + e.getMessage() + "；";
        }
    }

    /**
     * 重新同步硫化排程完成量历史数据
     * 1. 逻辑删除APS库今天之前所有硫化排程完成量数据
     * 2. 从MES库查询今天之前每天最新版本的硫化排程完成量数据
     * 3. 插入到APS库
     */
    private String resyncLhScheFinishQtyHistory() {
        String tableName = "硫化排程完成量";
        log.info("开始重新同步{}历史数据", tableName);
        try {
            FeignTokenHelper.runWithToken(() -> {
                AjaxResult deleteResult = lhMesSyncRemoteService.logicDeleteLhScheFinishQtyAllBeforeToday();
                log.info("逻辑删除{}今天及今天之前所有数据结果：{}", tableName, deleteResult.get("msg"));
            });

            DynamicDataSourceContextHolder.push(DataSource.MES);
            List<LhScheFinishQty> syncList = mesItfMapper.selectLhClassShiftFinishQtyHistoryList(new AuxReqSyncDataLogs());
            DynamicDataSourceContextHolder.poll();

            if (CollectionUtils.isEmpty(syncList)) {
                log.info("{}历史数据：MES中间表无历史数据可同步", tableName);
                return tableName + "：MES无历史数据；";
            }

            List<LhScheFinishQty> insertList = new ArrayList<>();
            for (LhScheFinishQty item : syncList) {
                LhScheFinishQty entity = new LhScheFinishQty();
                BeanUtils.copyProperties(item, entity);
                entity.setCreateBy("CLEAN_TASK");
                entity.setUpdateBy("CLEAN_TASK");
                entity.setCreateTime(DateUtils.getNowDate());
                entity.setUpdateTime(DateUtils.getNowDate());
                entity.setIsDelete(0);
                insertList.add(entity);
            }

            log.info("{}历史数据：从MES抓取到{}条记录，开始插入APS库", tableName, insertList.size());
            FeignTokenHelper.runWithToken(() -> {
                lhMesSyncRemoteService.saveScheFinishQtyBatch(insertList);
            });
            log.info("{}历史数据：重新同步完成，插入{}条", tableName, insertList.size());
            return tableName + "：插入" + insertList.size() + "条；";
        } catch (Exception e) {
            log.error("{}历史数据重新同步异常", tableName, e);
            return tableName + "：异常-" + e.getMessage() + "；";
        }
    }

    /**
     * 重新同步硫化排程日完成量历史数据
     * 1. 逻辑删除APS库今天之前所有硫化排程日完成量数据
     * 2. 从MES库查询今天之前每天最新版本的硫化排程日完成量数据
     * 3. 插入到APS库
     */
    private String resyncLhDayFinishQtyHistory() {
        String tableName = "硫化排程日完成量";
        log.info("开始重新同步{}历史数据", tableName);
        try {
            FeignTokenHelper.runWithToken(() -> {
                AjaxResult deleteResult = lhMesSyncRemoteService.logicDeleteLhDayFinishQtyAllBeforeToday();
                log.info("逻辑删除{}今天及今天之前所有数据结果：{}", tableName, deleteResult.get("msg"));
            });

            DynamicDataSourceContextHolder.push(DataSource.MES);
            List<LhDayFinishQty> syncList = mesItfMapper.selectLhScheDayFinishQtyHistoryList(new AuxReqSyncDataLogs());
            DynamicDataSourceContextHolder.poll();

            if (CollectionUtils.isEmpty(syncList)) {
                log.info("{}历史数据：MES中间表无历史数据可同步", tableName);
                return tableName + "：MES无历史数据；";
            }

            Map<String, LhDayFinishQty> groupMap = syncList.stream()
                    .collect(Collectors.toMap(
                            item -> item.getFactoryCode() + "|" + item.getFinishDate() + "|" + item.getMaterialCode() + "|" + item.getMesMaterialCode(),
                            Function.identity(),
                            (v1, v2) -> v1
                    ));
            syncList = new ArrayList<>(groupMap.values());

            List<LhDayFinishQty> insertList = new ArrayList<>();
            for (LhDayFinishQty item : syncList) {
                LhDayFinishQty entity = new LhDayFinishQty();
                BeanUtils.copyProperties(item, entity);
                entity.setCreateBy("CLEAN_TASK");
                entity.setUpdateBy("CLEAN_TASK");
                entity.setCreateTime(DateUtils.getNowDate());
                entity.setUpdateTime(DateUtils.getNowDate());
                entity.setIsDelete(0);
                insertList.add(entity);
            }

            log.info("{}历史数据：从MES抓取到{}条记录，开始插入APS库", tableName, insertList.size());
            FeignTokenHelper.runWithToken(() -> {
                lhMesSyncRemoteService.saveDayFinishQtyBatch(insertList);
            });
            log.info("{}历史数据：重新同步完成，插入{}条", tableName, insertList.size());
            return tableName + "：插入" + insertList.size() + "条；";
        } catch (Exception e) {
            log.error("{}历史数据重新同步异常", tableName, e);
            return tableName + "：异常-" + e.getMessage() + "；";
        }
    }

    /**
     * 重新同步成型排程完成量历史数据
     * 1. 逻辑删除APS库今天之前所有成型排程完成量数据
     * 2. 从MES库查询今天之前每天最新版本的成型排程完成量数据
     * 3. 插入到APS库
     */
    private String resyncCxScheFinishQtyHistory() {
        String tableName = "成型排程完成量";
        log.info("开始重新同步{}历史数据", tableName);
        try {
            FeignTokenHelper.runWithToken(() -> {
                AjaxResult deleteResult = cxMesSyncRemoteService.logicDeleteCxScheFinishQtyAllBeforeToday();
                log.info("逻辑删除{}今天及今天之前所有数据结果：{}", tableName, deleteResult.get("msg"));
            });

            DynamicDataSourceContextHolder.push(DataSource.MES);
            List<CxScheFinishQty> syncList = mesItfMapper.selectCxClassShiftFinishQtyHistoryList(new AuxReqSyncDataLogs());
            DynamicDataSourceContextHolder.poll();

            if (CollectionUtils.isEmpty(syncList)) {
                log.info("{}历史数据：MES中间表无历史数据可同步", tableName);
                return tableName + "：MES无历史数据；";
            }

            List<CxScheFinishQty> insertList = new ArrayList<>();
            for (CxScheFinishQty item : syncList) {
                CxScheFinishQty entity = new CxScheFinishQty();
                BeanUtils.copyProperties(item, entity);
                entity.setCreateBy("CLEAN_TASK");
                entity.setUpdateBy("CLEAN_TASK");
                entity.setCreateTime(DateUtils.getNowDate());
                entity.setUpdateTime(DateUtils.getNowDate());
                entity.setIsDelete(0);
                insertList.add(entity);
            }

            log.info("{}历史数据：从MES抓取到{}条记录，开始插入APS库", tableName, insertList.size());
            FeignTokenHelper.runWithToken(() -> {
                cxMesSyncRemoteService.saveScheFinishQtyBatch(insertList);
            });
            log.info("{}历史数据：重新同步完成，插入{}条", tableName, insertList.size());
            return tableName + "：插入" + insertList.size() + "条；";
        } catch (Exception e) {
            log.error("{}历史数据重新同步异常", tableName, e);
            return tableName + "：异常-" + e.getMessage() + "；";
        }
    }

    /**
     * 重新同步成型排程日完成量历史数据（含今天）
     * 1. 逻辑删除APS库今天及今天之前所有成型排程日完成量数据
     * 2. 从MES库查询每天（含今天）最新版本的成型排程日完成量数据
     * 3. 插入到APS库
     */
    private String resyncCxDayFinishQtyHistory() {
        String tableName = "成型排程日完成量";
        log.info("开始重新同步{}历史数据", tableName);
        try {
            FeignTokenHelper.runWithToken(() -> {
                AjaxResult deleteResult = cxMesSyncRemoteService.logicDeleteCxDayFinishQtyAllBeforeToday();
                log.info("逻辑删除{}今天及今天之前所有数据结果：{}", tableName, deleteResult.get("msg"));
            });

            DynamicDataSourceContextHolder.push(DataSource.MES);
            List<CxDayFinishQty> syncList = mesItfMapper.selectCxScheDayFinishQtyHistoryList(new AuxReqSyncDataLogs());
            DynamicDataSourceContextHolder.poll();

            if (CollectionUtils.isEmpty(syncList)) {
                log.info("{}历史数据：MES中间表无历史数据可同步", tableName);
                return tableName + "：MES无历史数据；";
            }

            Map<String, CxDayFinishQty> groupMap = syncList.stream()
                    .collect(Collectors.toMap(
                            item -> item.getFactoryCode() + "|" + item.getFinishDate() + "|" + item.getEmbryoCode() + "|" + item.getBomDataVersion(),
                            Function.identity(),
                            (v1, v2) -> v1
                    ));
            syncList = new ArrayList<>(groupMap.values());

            List<CxDayFinishQty> insertList = new ArrayList<>();
            for (CxDayFinishQty item : syncList) {
                CxDayFinishQty entity = new CxDayFinishQty();
                BeanUtils.copyProperties(item, entity);
                entity.setCreateBy("CLEAN_TASK");
                entity.setUpdateBy("CLEAN_TASK");
                entity.setCreateTime(DateUtils.getNowDate());
                entity.setUpdateTime(DateUtils.getNowDate());
                entity.setIsDelete(0);
                insertList.add(entity);
            }

            log.info("{}历史数据：从MES抓取到{}条记录，开始插入APS库", tableName, insertList.size());
            FeignTokenHelper.runWithToken(() -> {
                cxMesSyncRemoteService.saveDayFinishQtyBatch(insertList);
            });
            log.info("{}历史数据：重新同步完成，插入{}条", tableName, insertList.size());
            return tableName + "：插入" + insertList.size() + "条；";
        } catch (Exception e) {
            log.error("{}历史数据重新同步异常", tableName, e);
            return tableName + "：异常-" + e.getMessage() + "；";
        }
    }

    /**
     * 临时任务：按版本迭代同步模具清洗预警数据并生成清洗计划
     * 执行步骤：
     * 1. 清空APS现有的模具清洗预警和清洗计划表全部数据
     * 2. 从MES获取全部模具清洗预警版本号（升序排列）
     * 3. 从最小版本号开始，先插入APS作为初始数据
     * 4. 逐个版本迭代，对后续版本进行更新和新增
     * 5. 迭代到最新版本后，基于全部预警数据（不限制版本号）生成模具清洗计划
     * 6. 删除的预警也同步生成计划（标记为已删除的计划）
     *
     * @param syncDataLogs 同步参数
     * @return 执行结果
     */
    @Override
    public AjaxResult syncAllVersionsMouldCleanWarnAndGenPlan(AuxReqSyncDataLogs syncDataLogs) {
        log.info("临时任务-开始按版本迭代同步模具清洗预警数据并生成清洗计划");
        String factoryCode = syncDataLogs != null ? syncDataLogs.getFactoryCode() : null;

        // 步骤1：清空APS现有的模具清洗预警和清洗计划表全部数据
        log.info("临时任务-步骤1：清空APS现有的模具清洗预警和清洗计划表全部数据");
        try {
            FeignTokenHelper.runWithToken(() -> {
                AjaxResult cleanResult = lhMesSyncRemoteService.cleanAllMouldCleanWarnAndPlan();
                log.info("清空预警和计划表结果：{}", cleanResult != null ? cleanResult.get("msg") : "null");
            });
        } catch (Exception e) {
            log.error("清空预警和计划表异常", e);
            return AjaxResult.error("清空预警和计划表失败：" + e.getMessage());
        }

        // 步骤2：从MES获取所有版本号（升序排列）
        log.info("临时任务-步骤2：从MES获取所有版本号");
        DynamicDataSourceContextHolder.push(DataSource.MES);
        List<String> allVersions = mesItfMapper.selectAllDataVersionsFromMouldCleanPlan(factoryCode);
        DynamicDataSourceContextHolder.poll();

        if (CollectionUtils.isEmpty(allVersions)) {
            log.info("MES中间表无模具清洗预警计划版本数据");
            return AjaxResult.success("MES中间表无模具清洗预警计划版本数据");
        }

        log.info("MES中间表模具清洗预警共有{}个版本，从{}到{}",
                allVersions.size(), allVersions.get(0), allVersions.get(allVersions.size() - 1));

        AtomicInteger totalInsertCount = new AtomicInteger(0);
        AtomicInteger totalUpdateCount = new AtomicInteger(0);

        // 步骤3和4：按版本号从小到大迭代同步预警数据
        log.info("临时任务-步骤3和4：按版本号从小到大迭代同步预警数据");
        for (int i = 0; i < allVersions.size(); i++) {
            String version = allVersions.get(i);
            boolean isFirstVersion = (i == 0);
            log.info("开始同步版本号={}，是否为初始版本={}", version, isFirstVersion);

            // 从MES查询该版本的数据
            AuxReqSyncDataLogs versionParam = new AuxReqSyncDataLogs();
            versionParam.setFactoryCode(factoryCode);
            versionParam.setDataVersion(version);

            DynamicDataSourceContextHolder.push(DataSource.MES);
            List<MouldCleanPlanVo> syncList = mesItfMapper.selectMouldCleanPlanList(versionParam);
            DynamicDataSourceContextHolder.poll();

            if (CollectionUtils.isEmpty(syncList)) {
                log.info("版本号={}的MES数据为空，跳过", version);
                continue;
            }

            // 按factoryCode|lhCode去重，同一机台取第一条
            Map<String, MouldCleanPlanVo> groupMap = syncList.stream()
                    .collect(Collectors.toMap(
                            item -> item.getFactoryCode() + "|" + item.getLhCode(),
                            Function.identity(),
                            (v1, v2) -> v1
                    ));
            syncList = new ArrayList<>(groupMap.values());

            // 转换为LhMouldCleanWarn实体
            List<LhMouldCleanWarn> insertOrUpdateList = new ArrayList<>();
            for (MouldCleanPlanVo item : syncList) {
                LhMouldCleanWarn entity = new LhMouldCleanWarn();
                entity.setLhCode(item.getLhCode());
                entity.setFactoryCode(item.getFactoryCode());
                entity.setCompanyCode(item.getCompanyCode());
                entity.setDataVersion(item.getDataVersion());
                entity.setCreateBy("MES_VERSION_SYNC");
                entity.setUpdateBy("MES_VERSION_SYNC");
                entity.setCreateTime(DateUtils.getNowDate());
                entity.setUpdateTime(DateUtils.getNowDate());

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

                insertOrUpdateList.add(entity);
            }

            if (CollectionUtils.isEmpty(insertOrUpdateList)) {
                continue;
            }

            // 查询APS中已存在的预警数据，判断是新增还是更新
            FeignTokenHelper.runWithToken(() -> {
                List<LhMouldCleanWarn> existsList = lhMesSyncRemoteService.selectMouldCleanWarnExists(insertOrUpdateList);
                Map<String, LhMouldCleanWarn> existsMap = new HashMap<>(16);
                if (CollectionUtils.isNotEmpty(existsList)) {
                    existsMap = existsList.stream()
                            .collect(Collectors.toMap(
                                    item -> GenerageMapKeyUtils.createMapKey(item.getFactoryCode(), item.getLhCode()),
                                    Function.identity(),
                                    (v1, v2) -> v1
                            ));
                }

                int versionInsertCount = 0;
                int versionUpdateCount = 0;

                for (LhMouldCleanWarn entity : insertOrUpdateList) {
                    String mapKey = GenerageMapKeyUtils.createMapKey(entity.getFactoryCode(), entity.getLhCode());
                    if (existsMap.containsKey(mapKey)) {
                        // 已存在：更新（设置ID以覆盖）
                        LhMouldCleanWarn existsData = existsMap.get(mapKey);
                        entity.setId(existsData.getId());
                        versionUpdateCount++;
                    } else {
                        // 不存在：新增
                        versionInsertCount++;
                    }
                }

                // 分批保存（新增+更新混合保存）
                List<List<LhMouldCleanWarn>> splitList = ScmListUtils.getSplitList(insertOrUpdateList, 1000);
                for (List<LhMouldCleanWarn> saveList : splitList) {
                    lhMesSyncRemoteService.saveMouldCleanWarnBatch(saveList);
                }

                log.info("版本号={}同步完成，新增{}条，更新{}条", version, versionInsertCount, versionUpdateCount);
                totalInsertCount.addAndGet(versionInsertCount);
                totalUpdateCount.addAndGet(versionUpdateCount);
            });
        }

        log.info("模具清洗预警全部版本迭代同步完成，共处理{}个版本，总新增{}条，总更新{}条",
                allVersions.size(), totalInsertCount.get(), totalUpdateCount.get());

        // 步骤5：预警数据同步完成后，基于全部预警数据（不限制版本号）生成模具清洗计划
        // 使用syncAllMouldCleanPlanFromWarn而非syncMouldCleanPlanFromWarn
        // 因为syncMouldCleanPlanFromWarn只取MAX(DATA_VERSION)的预警来生成计划，会丢失历史版本数据
        log.info("临时任务-步骤5：基于全部预警数据全量生成清洗计划");
        try {
            FeignTokenHelper.runWithToken(() -> {
                try {
                    AjaxResult planResult = lhMesSyncRemoteService.syncAllMouldCleanPlanFromWarn();
                    log.info("基于全部预警数据全量生成清洗计划结果：{}", planResult != null ? planResult.get("msg") : "null");
                } catch (Exception e) {
                    log.error("基于全部预警数据全量生成清洗计划失败", e);
                }
            });
        } catch (Exception e) {
            log.error("基于全部预警数据全量生成清洗计划异常", e);
        }

        log.info("临时任务-按版本迭代同步模具清洗预警数据并生成清洗计划完成");
        return AjaxResult.success("同步完成，共处理" + allVersions.size() + "个版本，总新增" + totalInsertCount.get() + "条，总更新" + totalUpdateCount.get() + "条");
    }

    @Override
    public AjaxResult syncDayFinishQtyToChipStock() {
        log.info("【硫化日完成量回填芯片库存】定时任务开始执行");

        Map<String, Set<String>> factoryChipCodeMap = loadChipCodeConfig();
        if (factoryChipCodeMap.isEmpty()) {
            log.warn("【硫化日完成量回填芯片库存】未找到芯片编码配置（CHIP_CODE_STOCK_UPDATE），跳过执行");
            return AjaxResult.error("未找到芯片编码配置（CHIP_CODE_STOCK_UPDATE）");
        }

        List<LhDayFinishQty> allDayFinishQtyList;
        try {
            allDayFinishQtyList = FeignTokenHelper.callWithToken(() -> lhMesSyncRemoteService.queryLatestDayFinishQty());
        } catch (Exception e) {
            log.error("【硫化日完成量回填芯片库存】查询日完成量数据异常", e);
            return AjaxResult.error("查询日完成量数据异常：" + e.getMessage());
        }

        if (CollectionUtils.isEmpty(allDayFinishQtyList)) {
            log.info("【硫化日完成量回填芯片库存】硫化日完成量回报数据为空，跳过执行");
            return AjaxResult.success("日完成量数据为空");
        }
        log.info("【硫化日完成量回填芯片库存】查询到日完成量数据总条数：{}", allDayFinishQtyList.size());

        int totalInsert = 0;
        int totalUpdate = 0;
        for (Map.Entry<String, Set<String>> entry : factoryChipCodeMap.entrySet()) {
            String factoryCode = entry.getKey();
            Set<String> chipCodeSet = entry.getValue();
            log.info("【硫化日完成量回填芯片库存】分厂={}，芯片编码配置集合={}", factoryCode, chipCodeSet);

            List<String> allMaterialCodes = allDayFinishQtyList.stream()
                    .filter(item -> factoryCode.equals(item.getFactoryCode()))
                    .map(LhDayFinishQty::getMaterialCode)
                    .filter(StringUtils::isNotEmpty)
                    .distinct()
                    .collect(Collectors.toList());
            log.info("【硫化日完成量回填芯片库存】分厂={}，日完成量中物料编码列表={}", factoryCode, allMaterialCodes);

            List<String> allMesMaterialCodes = allDayFinishQtyList.stream()
                    .filter(item -> factoryCode.equals(item.getFactoryCode()))
                    .map(LhDayFinishQty::getMesMaterialCode)
                    .filter(StringUtils::isNotEmpty)
                    .distinct()
                    .collect(Collectors.toList());
            log.info("【硫化日完成量回填芯片库存】分厂={}，日完成量中MES物料编码列表={}", factoryCode, allMesMaterialCodes);

            List<String> matchedCodes = allMaterialCodes.stream()
                    .filter(chipCodeSet::contains)
                    .collect(Collectors.toList());
            log.info("【硫化日完成量回填芯片库存】分厂={}，物料编码与芯片编码集合匹配结果：matched={}, unmatched={}",
                    factoryCode, matchedCodes, allMaterialCodes.stream().filter(code -> !chipCodeSet.contains(code)).collect(Collectors.toList()));

            List<LhDayFinishQty> filteredList = allDayFinishQtyList.stream()
                    .filter(item -> factoryCode.equals(item.getFactoryCode()))
                    .filter(item -> chipCodeSet.contains(item.getMaterialCode()))
                    .collect(Collectors.toList());

            if (CollectionUtils.isEmpty(filteredList)) {
                log.warn("【硫化日完成量回填芯片库存】分厂{}无匹配芯片编码的日完成量数据，跳过。chipCodeSet={}, allMaterialCodes={}", factoryCode, chipCodeSet, allMaterialCodes);
                continue;
            }

            Map<String, BigDecimal> chipFinishQtyMap = filteredList.stream()
                    .filter(item -> item.getDayFinishQty() != null)
                    .collect(Collectors.groupingBy(
                            LhDayFinishQty::getMaterialCode,
                            Collectors.reducing(BigDecimal.ZERO, LhDayFinishQty::getDayFinishQty, BigDecimal::add)
                    ));

            Map<String, String> chipVersionMap = filteredList.stream()
                    .filter(item -> item.getDataVersion() != null && !item.getDataVersion().isEmpty())
                    .collect(Collectors.groupingBy(
                            LhDayFinishQty::getMaterialCode,
                            Collectors.collectingAndThen(
                                    Collectors.maxBy(Comparator.comparing(LhDayFinishQty::getDataVersion, Comparator.nullsFirst(Comparator.naturalOrder()))),
                                    opt -> opt.map(LhDayFinishQty::getDataVersion).orElse(null)
                            )
                    ));

            log.info("【硫化日完成量回填芯片库存】分厂{}，匹配芯片编码数量={}，汇总后芯片数量={}，版本号映射={}",
                    factoryCode, chipCodeSet.size(), chipFinishQtyMap.size(), chipVersionMap);

            List<LhChipStock> chipStockList = chipFinishQtyMap.entrySet().stream().map(e -> {
                LhChipStock chipStock = new LhChipStock();
                chipStock.setFactoryCode(factoryCode);
                chipStock.setChipCode(e.getKey());
                chipStock.setFinishQty(e.getValue().intValue());
                chipStock.setDataVersion(chipVersionMap.get(e.getKey()));
                return chipStock;
            }).collect(Collectors.toList());

            try {
                FeignTokenHelper.runWithToken(() -> {
                    log.info("【硫化日完成量回填芯片库存】开始覆盖更新，factoryCode={}, 待处理数量={}", factoryCode, chipStockList.size());
                    lhChipStockRemoteService.overwriteFinishQty(factoryCode, chipStockList);
                    log.info("【硫化日完成量回填芯片库存】覆盖更新完成，factoryCode={}, 处理数量={}", factoryCode, chipStockList.size());
                });
            } catch (Exception e) {
                log.error("【硫化日完成量回填芯片库存】覆盖更新异常, factoryCode={}", factoryCode, e);
            }
        }

        log.info("【硫化日完成量回填芯片库存】定时任务执行完成");
        return AjaxResult.success("执行完成");
    }

    private Map<String, Set<String>> loadChipCodeConfig() {
        List<LhParams> paramList;
        try {
            paramList = FeignTokenHelper.callWithToken(() ->
                    lhMesSyncRemoteService.selectLhParamsListByParamCode(LhScheduleParamConstant.CHIP_CODE_STOCK_UPDATE));
        } catch (Exception e) {
            log.error("【硫化日完成量回填芯片库存】查询硫化参数配置异常", e);
            return Collections.emptyMap();
        }
        if (CollectionUtils.isEmpty(paramList)) {
            return Collections.emptyMap();
        }
        Map<String, Set<String>> result = new LinkedHashMap<>();
        for (LhParams param : paramList) {
            if (param.getParamValue() == null || param.getParamValue().trim().isEmpty()) {
                continue;
            }
            Set<String> chipCodeSet = Arrays.stream(param.getParamValue().split(","))
                    .map(String::trim)
                    .filter(code -> !code.isEmpty())
                    .collect(Collectors.toSet());
            if (!chipCodeSet.isEmpty()) {
                result.put(param.getFactoryCode(), chipCodeSet);
            }
        }
        return result;
    }
}
