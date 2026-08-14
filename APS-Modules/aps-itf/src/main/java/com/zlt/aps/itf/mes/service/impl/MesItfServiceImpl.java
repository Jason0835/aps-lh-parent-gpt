package com.zlt.aps.itf.mes.service.impl;

import cn.hutool.core.date.DateUtil;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.dynamic.datasource.toolkit.DynamicDataSourceContextHolder;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.autoLogin.feign.FeignTokenHelper;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.core.enums.MouldFinishStatusEnum;
import com.zlt.aps.common.core.utils.AjaxResultUtils;
import com.zlt.aps.common.core.utils.BigDecimalUtils;
import com.zlt.aps.constant.FactoryConstant;
import com.zlt.aps.cx.api.domain.entity.*;
import com.zlt.aps.cx.api.service.ICxMesSyncRemoteService;
import com.zlt.aps.cx.api.service.ICxPrecisionPlanRemoteService;
import com.zlt.aps.enums.LocationTypeEnum;
import com.zlt.aps.enums.ProductTypeEnum;
import com.zlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.gsq.api.domain.entity.GsqDayFinishQty;
import com.zlt.aps.gsq.api.domain.entity.GsqStock;
import com.zlt.aps.gsq.api.service.IGsqMesSyncRemoteService;
import com.zlt.aps.itf.constant.DataSource;
import com.zlt.aps.itf.constant.SysCode;
import com.zlt.aps.itf.mes.enums.ItfSyncKeyEnum;
import com.zlt.aps.itf.mes.enums.MouldCategoryConvertEnum;
import com.zlt.aps.itf.mes.mapper.LhScheduleResultQueryMapper;
import com.zlt.aps.itf.mes.mapper.MesItfMapper;
import com.zlt.aps.itf.mes.mapper.MesViewMapper;
import com.zlt.aps.itf.mes.mapper.MoldAlterPlanIssueMapper;
import com.zlt.aps.itf.mes.service.IPrecisionPlanIssueService;
import com.zlt.aps.itf.mes.service.ITmScheduleResultIssueService;
import com.zlt.aps.itf.mes.service.MesItfService;
import com.zlt.aps.itf.mes.vo.MoldAlterPlanIssue;
import com.zlt.aps.itf.scm.service.ScmItfService;
import com.zlt.aps.itf.vo.*;
import com.zlt.aps.lh.api.constant.LhScheduleParamConstant;
import com.zlt.aps.lh.api.domain.entity.*;
import com.zlt.aps.lh.api.enums.TrialStatusEnum;
import com.zlt.aps.lh.api.service.ILhChipStockRemoteService;
import com.zlt.aps.lh.api.service.ILhMesSyncRemoteService;
import com.zlt.aps.lh.api.service.ILhPrecisionPlanRemoteService;
import com.zlt.aps.maindata.enums.MonthPlanEnums;
import com.zlt.aps.maindata.mapper.*;
import com.zlt.aps.maindata.service.IFactoryParamService;
import com.zlt.aps.maindata.service.IMdmProductModelRelationService;
import com.zlt.aps.maindata.service.IMdmSkuStructureRefService;
import com.zlt.aps.maindata.utils.ScmListUtils;
import com.zlt.aps.mdm.api.domain.entity.MdmMoldAlterPlan;
import com.zlt.aps.mp.api.domain.entity.*;
import com.zlt.aps.tm.api.domain.entity.*;
import com.zlt.aps.tm.api.service.ITmMesSyncRemoteService;
import com.zlt.aps.tq.api.domain.entity.*;
import com.zlt.aps.tq.api.service.ITqMesSyncRemoteService;
import com.zlt.aps.utils.GenerageMapKeyUtils;
import com.zlt.core.dao.basedao.BaseDao;
import com.zlt.sync.handle.SyncDataHandle;
import com.zlt.sync.povo.SyncParamsVO;
import com.zlt.sync.service.SyncDataLogsService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.ParseException;
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

    /**
     * SQL Server单次请求参数上限2100，安全批次大小为50
     */
    private static final int BATCH_SIZE = 50;

    /**
     * Feign调用成功的业务码。
     * 框架bug：AjaxResult.success()硬编码返回code=200，但AjaxResult.Type.SUCCESS.value()=0，两者不一致。
     * 因此判断Feign返回是否成功时，必须用AJAX_SUCCESS_CODE(200)比较，不能用Type.SUCCESS.value()(0)，否则永远误判为失败。
     */
    private static final int AJAX_SUCCESS_CODE = 200;

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
    private MdmDevicePlanShutEntityMapper devicePlanShutEntityMapper;

    @Autowired
    private ICxMesSyncRemoteService cxMesSyncRemoteService;

    @Autowired
    private ITqMesSyncRemoteService tqMesSyncRemoteService;

    @Autowired
    private IGsqMesSyncRemoteService gsqMesSyncRemoteService;

    @Autowired
    private ITmMesSyncRemoteService tmMesSyncRemoteService;

    @Autowired
    private ITmScheduleResultIssueService tmScheduleResultIssueService;

//    @Autowired
//    private IGsqScheduleResultIssueService gsqScheduleResultIssueService;

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
     * 硫化排程结果查询Mapper（APS数据源），用于试制/量试完成量回报规则查询当日计划量
     */
    @Autowired
    private LhScheduleResultQueryMapper lhScheduleResultQueryMapper;

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
            //20260611+ 更新SKU与模具关系的主花纹
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
            AjaxResult saveResult = FeignTokenHelper.callWithToken(() ->
                    lhMesSyncRemoteService.logicDeleteAndSaveMachineOnlineInfo(factoryCode, onlineDateStr, "MES", insertList));
            if (AJAX_SUCCESS_CODE != (Integer) saveResult.get(AjaxResult.CODE_TAG)) {
                log.error("硫化在机同步：同步失败，factoryCode={}, 返回code={}, 返回消息={}",
                        factoryCode, saveResult.get(AjaxResult.CODE_TAG), saveResult.get(AjaxResult.MSG_TAG));
                return AjaxResult.error("硫化在机同步失败：" + saveResult.get(AjaxResult.MSG_TAG));
            }

            log.info("硫化在机同步：同步完成，factoryCode={}, 插入数量={}", factoryCode, insertList.size());
        } catch (Exception e) {
            log.error("硫化在机同步：Feign调用异常，factoryCode={}, 待插入数量={}", lhMachineOnlineInfo.getFactoryCode(), insertList.size(), e);
            return AjaxResult.error("硫化在机同步失败：" + e.getMessage());
        }
        return AjaxResult.success();
    }

    /**
     * 按指定版本号同步硫化在机数据（临时任务）
     * 与原syncLhMachineOnlineInfo的区别：不限日期，按指定版本号查询MES中间表所有日期数据
     * 同步逻辑参考硫化排程完成量回报按版本号同步（syncLhClassShiftFinishQtyByVersion）：
     * 由于指定版本可能包含多个onlineDate的数据，按onlineDate分组后逐组调用逻辑删除+插入
     * 硫化在机数据不涉及排程结果回填，无需调用回填接口
     *
     * @param dataVersion 指定版本号
     * @return 结果
     */
    @Override
    public AjaxResult syncLhMachineOnlineInfoByVersion(String dataVersion) {
        LhMachineOnlineInfo queryParam = new LhMachineOnlineInfo();
        queryParam.setDataVersion(dataVersion);

        DynamicDataSourceContextHolder.push(DataSource.MES);
        List<LhMachineOnlineInfo> syncList = mesItfMapper.selectLhMachineOnlineSyncListByVersion(queryParam);
        DynamicDataSourceContextHolder.poll();

        if (CollectionUtils.isEmpty(syncList)) {
            log.warn("硫化在机按版本号同步：MES中间表查询结果为空，dataVersion={}", dataVersion);
            return AjaxResult.success("MES中间表无数据可同步");
        }

        List<LhMachineOnlineInfo> insertList = new ArrayList<>();
        for (LhMachineOnlineInfo item : syncList) {
            LhMachineOnlineInfo entity = new LhMachineOnlineInfo();
            BeanUtils.copyProperties(item, entity);
            entity.setCreateBy("MES");
            entity.setUpdateBy("MES");
            entity.setCreateTime(DateUtils.getNowDate());
            entity.setUpdateTime(DateUtils.getNowDate());
            entity.setIsDelete(0);
            insertList.add(entity);
        }

        // 按onlineDate分组，逐组同步（原逻辑按单个onlineDate做逻辑删除+插入，跨日期需分组避免误删）
        Map<String, List<LhMachineOnlineInfo>> groupByOnlineDate = insertList.stream()
                .collect(Collectors.groupingBy(item -> {
                    Date onlineDate = item.getOnlineDate();
                    return onlineDate != null ? DateUtil.formatDate(onlineDate) : "unknown";
                }));

        for (Map.Entry<String, List<LhMachineOnlineInfo>> entry : groupByOnlineDate.entrySet()) {
            String onlineDateStr = entry.getKey();
            List<LhMachineOnlineInfo> groupList = entry.getValue();
            String factoryCode = groupList.get(0).getFactoryCode();

            try {
                log.info("硫化在机按版本号同步：开始同步，dataVersion={}, factoryCode={}, onlineDate={}, 待插入数量={}",
                        dataVersion, factoryCode, onlineDateStr, groupList.size());

                String finalFactoryCode = factoryCode;
                FeignTokenHelper.runWithToken(() -> {
                    lhMesSyncRemoteService.logicDeleteAndSaveMachineOnlineInfo(finalFactoryCode, onlineDateStr, "MES", groupList);
                });

                log.info("硫化在机按版本号同步：同步完成，dataVersion={}, factoryCode={}, onlineDate={}, 插入数量={}",
                        dataVersion, factoryCode, onlineDateStr, groupList.size());
            } catch (Exception e) {
                log.error("硫化在机按版本号同步：Feign调用异常，dataVersion={}, factoryCode={}, onlineDate={}",
                        dataVersion, factoryCode, onlineDateStr, e);
                return AjaxResult.error("硫化在机按版本号同步失败：" + e.getMessage());
            }
        }
        return AjaxResult.success();
    }

    /**
     * 同步设备保养计划
     * 采用更新删除标识模式，而不是先删后插
     * @param syncDataLogs 同步参数
     * @return 结果
     * @deprecated 原逻辑：MES做完精度写入中间表，APS抓取后回填实际完成日期并生成下一次精度计划。
     *             现逻辑改为MES全权决定计划与完成时间，APS只做同步+分发，不再回填/生成。
     *             此方法保留备份以防后续改回原逻辑。新逻辑请使用 {@link #syncAndDispatchDevMaintenancePlan}
     */
    @Deprecated
    @Override
    public AjaxResult syncDevMaintenancePlan(AuxReqSyncDataLogs syncDataLogs) {
        // 查询MES中间表指定精度类型的最大版本号，只同步最新版本的数据
        String precisionType = syncDataLogs != null ? syncDataLogs.getPrecisionType() : null;
        String oldFactoryCode = syncDataLogs != null ? syncDataLogs.getFactoryCode() : null;
        DynamicDataSourceContextHolder.push(DataSource.MES);
        String maxVersion = mesItfMapper.selectMaxDataVersionFromMes(oldFactoryCode, precisionType);
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

    @Transactional(rollbackFor = Exception.class)
    @Override
    public AjaxResult syncDevMaintenancePlanOnly(AuxReqSyncDataLogs syncDataLogs) {
        // 查询MES中间表指定精度类型的最大版本号，只同步最新版本的数据
        // 若调用方已传入dataVersion（如临时任务按版本号抓取），则跳过查最大版本号，直接使用传入版本号
        String precisionType = syncDataLogs != null ? syncDataLogs.getPrecisionType() : null;
        String inputVersion = syncDataLogs != null ? syncDataLogs.getDataVersion() : null;
        // 补充factoryCode默认值，与Controller逻辑一致，避免by-version路径factoryCode为空导致：
        // 1. MES查询不按分厂过滤
        // 2. 逻辑删除SQL无FACTORY_CODE条件被BlockAttackInnerInterceptor拦截
        String factoryCode = syncDataLogs != null ? syncDataLogs.getFactoryCode() : null;
        if (StringUtils.isBlank(factoryCode)) {
            factoryCode = FactoryConstant.DEFAULT_FACTORY_CODE;
            if (syncDataLogs == null) {
                syncDataLogs = new AuxReqSyncDataLogs();
            }
            syncDataLogs.setFactoryCode(factoryCode);
        }

        if (StringUtils.isBlank(inputVersion)) {
            DynamicDataSourceContextHolder.push(DataSource.MES);
            String maxVersion = mesItfMapper.selectMaxDataVersionFromMes(factoryCode, precisionType);
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
        } else {
            log.info("仅同步设备保养计划（不触发生成），指定版本号={}，精度类型={}", inputVersion, precisionType);
        }

        DynamicDataSourceContextHolder.push(DataSource.MES);
        List<DevMaintenancePlan> syncList = mesItfMapper.selectDevMaintenancePlanList(syncDataLogs);
        DynamicDataSourceContextHolder.poll();

        // 按factoryCode|devCode|precisionType|operTime去重，同一设备同一精度类型不同计划日期的记录不能合并
        Map<String, DevMaintenancePlan> groupMap = syncList.stream()
                .collect(Collectors.toMap(
                        item -> item.getFactoryCode() + "|" + item.getDevCode() + "|" + item.getPrecisionType() + "|" + item.getOperTime(),
                        Function.identity(),
                        (v1, v2) -> v1
                ));
        syncList = new ArrayList<>(groupMap.values());

        try {
            DynamicDataSourceContextHolder.push(DataSource.APS);

            // 步骤1：按分厂+精度类型逻辑删除APS本地表所有旧数据（IS_DELETE置为1）
            // MES每次推送的版本号是全量快照，先删后插可保证APS本地表与MES最新版本完全一致
            // 同时解决MES硬删除记录在APS残留、null字段不覆盖、去重丢失等问题
            // 使用@Update注解方式，WHERE必须包含FACTORY_CODE业务主键，否则会被BlockAttackInnerInterceptor拦截
            int deletedCount;
            if (StringUtils.isBlank(precisionType)) {
                // 未指定精度类型，按分厂删除全部
                deletedCount = devMaintenancePlanEntityMapper.logicDeleteByFactoryCode(factoryCode);
            } else if ("硫化精度".equals(precisionType)) {
                // 硫化精度：分厂+精确匹配
                deletedCount = devMaintenancePlanEntityMapper.logicDeleteByFactoryCodeAndPrecisionType(factoryCode, "硫化精度");
            } else if (precisionType.startsWith("成型精度")) {
                // 成型精度15天/成型精度60天：分厂+前缀匹配
                deletedCount = devMaintenancePlanEntityMapper.logicDeleteByFactoryCodeAndPrecisionTypePrefix(factoryCode, "成型精度");
            } else {
                // 其他精度类型：分厂+精确匹配
                deletedCount = devMaintenancePlanEntityMapper.logicDeleteByFactoryCodeAndPrecisionType(factoryCode, precisionType);
            }
            log.info("逻辑删除APS本地表旧设备保养计划数据完成，分厂={}，精度类型={}，删除{}条", factoryCode, precisionType, deletedCount);

            // 步骤2：将MES新版本数据全量插入（IS_DELETE=0）
            // MES标记DEL_FLAG=1的记录也插入，但IS_DELETE=1，保持与MES一致
            List<MdmDevMaintenancePlan> insertList = new ArrayList<>();
            for (DevMaintenancePlan item : syncList) {
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

                // MES标记删除的记录也插入，但IS_DELETE=1；正常记录IS_DELETE=0
                if (StringUtils.isNotBlank(item.getDelFlag())) {
                    entity.setIsDelete(Integer.valueOf(item.getDelFlag()));
                } else {
                    entity.setIsDelete(0);
                }

                insertList.add(entity);
            }

            // 批量插入，按1000条分批
            List<List<MdmDevMaintenancePlan>> splitList = ScmListUtils.getSplitList(insertList, 1000);
            for (List<MdmDevMaintenancePlan> batch : splitList) {
                baseDao.saveBatch(batch);
            }

            log.info("仅同步设备保养计划完成（不触发生成精度计划），精度类型={}，同步{}条", precisionType, syncList.size());
        } finally {
            DynamicDataSourceContextHolder.poll();
        }
        return AjaxResult.success();
    }

    /**
     * 同步设备保养计划并按精度类型分发写入对应的精度计划表（现逻辑）
     * 现逻辑：MES全权决定计划时间(OPER_TIME)和实际完成时间(FIRST_WASH_TIME)，
     * APS侧不再回填实际日期、不再生成下一次精度计划。
     *
     * 执行步骤：
     * 1. 调用syncDevMaintenancePlanOnly同步MES数据到T_MDM_DEV_MAINTENANCE_PLAN
     * 2. 从T_MDM_DEV_MAINTENANCE_PLAN按版本号查询刚同步的数据
     * 3. 按PRECISION_TYPE分组，调用对应模块的dispatchFromMaintenancePlan分发写入：
     *    - "硫化精度" → 调lh模块写入T_LH_PRECISION_PLAN
     *    - "成型精度15天"/"成型精度60天" → 调cx模块写入T_CX_PRECISION_PLAN
     *
     * @param syncDataLogs 同步参数（可指定精度类型，为空时同步全部）
     * @return 同步+分发结果
     */
    @Override
    public AjaxResult syncAndDispatchDevMaintenancePlan(AuxReqSyncDataLogs syncDataLogs) {
        log.info("开始同步并分发设备保养计划，精度类型={}", syncDataLogs != null ? syncDataLogs.getPrecisionType() : null);

        // 步骤1：先调用仅同步方法，把MES数据同步到T_MDM_DEV_MAINTENANCE_PLAN
        AjaxResult syncResult = syncDevMaintenancePlanOnly(syncDataLogs);
        if (syncResult == null || AJAX_SUCCESS_CODE != Integer.parseInt(syncResult.get("code").toString())) {
            log.error("同步设备保养计划失败，跳过分发");
            return AjaxResult.error("同步设备保养计划失败");
        }

        // 步骤2：从T_MDM_DEV_MAINTENANCE_PLAN查询最新版本号的数据
        String precisionType = syncDataLogs != null ? syncDataLogs.getPrecisionType() : null;
        try {
            DynamicDataSourceContextHolder.push(DataSource.APS);

            // 查询APS本地表中指定精度类型的最大版本号
            LambdaQueryWrapper<MdmDevMaintenancePlan> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(MdmDevMaintenancePlan::getIsDelete, 0);
            if (StringUtils.isNotBlank(precisionType)) {
                if ("硫化精度".equals(precisionType)) {
                    wrapper.eq(MdmDevMaintenancePlan::getPrecisionType, "硫化精度");
                } else if (precisionType.startsWith("成型精度")) {
                    wrapper.likeRight(MdmDevMaintenancePlan::getPrecisionType, "成型精度");
                }
            }
            // 按更新时间倒序取最新同步的数据
            wrapper.orderByDesc(MdmDevMaintenancePlan::getUpdateTime);
            List<MdmDevMaintenancePlan> allPlans = devMaintenancePlanEntityMapper.selectList(wrapper);

            if (CollectionUtils.isEmpty(allPlans)) {
                log.info("APS本地表中无设备保养计划数据，跳过分发");
                return AjaxResult.success("无数据可分发");
            }

            // 步骤3：按精度类型分组ID，调用对应模块分发
            // 硫化精度 → lh模块
            List<Long> lhIds = allPlans.stream()
                    .filter(p -> "硫化精度".equals(p.getPrecisionType()))
                    .map(MdmDevMaintenancePlan::getId)
                    .collect(Collectors.toList());
            // 成型精度（包含15天/60天）→ cx模块
            List<Long> cxIds = allPlans.stream()
                    .filter(p -> p.getPrecisionType() != null && p.getPrecisionType().startsWith("成型精度"))
                    .map(MdmDevMaintenancePlan::getId)
                    .collect(Collectors.toList());

            log.info("待分发数据：硫化精度{}条，成型精度{}条", lhIds.size(), cxIds.size());

            int totalDispatched = 0;

            // 分发硫化精度计划到lh模块
            if (!lhIds.isEmpty()) {
                try {
                    final List<Long> finalLhIds = lhIds;
                    AjaxResult lhResult = FeignTokenHelper.callWithToken(() ->
                            lhPrecisionPlanRemoteService.dispatchFromMaintenancePlan(finalLhIds));
                    if (lhResult != null && AJAX_SUCCESS_CODE == Integer.parseInt(lhResult.get("code").toString())) {
                        Object data = lhResult.get("data");
                        int count = data != null ? Integer.parseInt(data.toString()) : 0;
                        totalDispatched += count;
                        log.info("分发硫化精度计划{}条", count);
                    } else {
                        log.error("分发硫化精度计划失败：{}", lhResult != null ? lhResult.get("msg") : "返回为空");
                    }
                } catch (Exception e) {
                    log.error("调用lh模块分发硫化精度计划异常", e);
                }
            }

            // 分发成型精度计划到cx模块
            if (!cxIds.isEmpty()) {
                try {
                    final List<Long> finalCxIds = cxIds;
                    AjaxResult cxResult = FeignTokenHelper.callWithToken(() ->
                            cxPrecisionPlanRemoteService.dispatchFromMaintenancePlan(finalCxIds));
                    if (cxResult != null && AJAX_SUCCESS_CODE == Integer.parseInt(cxResult.get("code").toString())) {
                        Object data = cxResult.get("data");
                        int count = data != null ? Integer.parseInt(data.toString()) : 0;
                        totalDispatched += count;
                        log.info("分发成型精度计划{}条", count);
                    } else {
                        log.error("分发成型精度计划失败：{}", cxResult != null ? cxResult.get("msg") : "返回为空");
                    }
                } catch (Exception e) {
                    log.error("调用cx模块分发成型精度计划异常", e);
                }
            }

            log.info("同步并分发设备保养计划完成，共分发{}条", totalDispatched);
            return AjaxResult.success("同步并分发完成", totalDispatched);
        } finally {
            DynamicDataSourceContextHolder.poll();
        }
    }

    /**
     * 按指定版本号同步设备保养计划并分发写入精度计划表（临时任务）
     * 与原syncAndDispatchDevMaintenancePlan的区别：
     * 1. 不查最大版本号，直接使用传入的dataVersion查询MES中间表
     * 2. 不限精度类型，同步指定版本下的全部精度类型数据
     * 3. 分发时按dataVersion精确查询APS本地表，仅分发本次同步的数据（避免历史数据重复分发）
     *
     * 执行步骤：
     * 1. 构造含dataVersion的同步参数，调用syncDevMaintenancePlanOnly同步到T_MDM_DEV_MAINTENANCE_PLAN
     * 2. 按dataVersion从T_MDM_DEV_MAINTENANCE_PLAN查询本次同步的数据
     * 3. 按PRECISION_TYPE分组ID，调用对应模块分发：
     *    - "硫化精度" → 调lh模块dispatchFromMaintenancePlan写入T_LH_PRECISION_PLAN
     *    - "成型精度15天"/"成型精度60天" → 调cx模块dispatchFromMaintenancePlan写入T_CX_PRECISION_PLAN
     *
     * @param dataVersion 指定版本号
     * @return 同步+分发结果
     */
    @Override
    public AjaxResult syncAndDispatchDevMaintenancePlanByVersion(String dataVersion) {
        log.info("开始按指定版本号同步并分发设备保养计划，dataVersion={}", dataVersion);
        if (StringUtils.isBlank(dataVersion)) {
            return AjaxResult.error("版本号不能为空");
        }

        // 步骤1：构造含dataVersion的同步参数，调用syncDevMaintenancePlanOnly同步
        // 改造后的syncDevMaintenancePlanOnly会跳过查最大版本号，直接用传入版本号查询MES中间表
        AuxReqSyncDataLogs syncDataLogs = new AuxReqSyncDataLogs();
        syncDataLogs.setDataVersion(dataVersion);
        AjaxResult syncResult = syncDevMaintenancePlanOnly(syncDataLogs);
        if (syncResult == null || AJAX_SUCCESS_CODE != Integer.parseInt(syncResult.get("code").toString())) {
            log.error("按版本号同步设备保养计划失败，dataVersion={}", dataVersion);
            return AjaxResult.error("按版本号同步设备保养计划失败");
        }

        // 步骤2：从T_MDM_DEV_MAINTENANCE_PLAN按dataVersion精确查询本次同步的数据
        try {
            DynamicDataSourceContextHolder.push(DataSource.APS);

            LambdaQueryWrapper<MdmDevMaintenancePlan> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(MdmDevMaintenancePlan::getIsDelete, 0)
                    .eq(MdmDevMaintenancePlan::getDataVersion, dataVersion);
            List<MdmDevMaintenancePlan> syncedPlans = devMaintenancePlanEntityMapper.selectList(wrapper);

            if (CollectionUtils.isEmpty(syncedPlans)) {
                log.info("按版本号同步后，APS本地表无该版本数据，跳过分发，dataVersion={}", dataVersion);
                return AjaxResult.success("无数据可分发");
            }

            // 步骤3：按精度类型分组ID，调用对应模块分发
            // 硫化精度 → lh模块
            List<Long> lhIds = syncedPlans.stream()
                    .filter(p -> "硫化精度".equals(p.getPrecisionType()))
                    .map(MdmDevMaintenancePlan::getId)
                    .collect(Collectors.toList());
            // 成型精度（包含15天/60天）→ cx模块
            List<Long> cxIds = syncedPlans.stream()
                    .filter(p -> p.getPrecisionType() != null && p.getPrecisionType().startsWith("成型精度"))
                    .map(MdmDevMaintenancePlan::getId)
                    .collect(Collectors.toList());

            log.info("版本号{}待分发数据：硫化精度{}条，成型精度{}条", dataVersion, lhIds.size(), cxIds.size());

            int totalDispatched = 0;

            // 分发硫化精度计划到lh模块
            if (!lhIds.isEmpty()) {
                try {
                    final List<Long> finalLhIds = lhIds;
                    AjaxResult lhResult = FeignTokenHelper.callWithToken(() ->
                            lhPrecisionPlanRemoteService.dispatchFromMaintenancePlan(finalLhIds));
                    if (lhResult != null && AJAX_SUCCESS_CODE == Integer.parseInt(lhResult.get("code").toString())) {
                        Object data = lhResult.get("data");
                        int count = data != null ? Integer.parseInt(data.toString()) : 0;
                        totalDispatched += count;
                        log.info("版本号{}分发硫化精度计划{}条", dataVersion, count);
                    } else {
                        log.error("版本号{}分发硫化精度计划失败：{}", dataVersion,
                                lhResult != null ? lhResult.get("msg") : "返回为空");
                    }
                } catch (Exception e) {
                    log.error("版本号{}调用lh模块分发硫化精度计划异常", dataVersion, e);
                }
            }

            // 分发成型精度计划到cx模块
            if (!cxIds.isEmpty()) {
                try {
                    final List<Long> finalCxIds = cxIds;
                    AjaxResult cxResult = FeignTokenHelper.callWithToken(() ->
                            cxPrecisionPlanRemoteService.dispatchFromMaintenancePlan(finalCxIds));
                    if (cxResult != null && AJAX_SUCCESS_CODE == Integer.parseInt(cxResult.get("code").toString())) {
                        Object data = cxResult.get("data");
                        int count = data != null ? Integer.parseInt(data.toString()) : 0;
                        totalDispatched += count;
                        log.info("版本号{}分发成型精度计划{}条", dataVersion, count);
                    } else {
                        log.error("版本号{}分发成型精度计划失败：{}", dataVersion,
                                cxResult != null ? cxResult.get("msg") : "返回为空");
                    }
                } catch (Exception e) {
                    log.error("版本号{}调用cx模块分发成型精度计划异常", dataVersion, e);
                }
            }

            log.info("按版本号同步并分发设备保养计划完成，dataVersion={}，共分发{}条", dataVersion, totalDispatched);
            return AjaxResult.success("按版本号同步并分发完成", totalDispatched);
        } finally {
            DynamicDataSourceContextHolder.poll();
        }
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
        List<CxMesStock> syncList = mesItfMapper.selectMesEmbryoStockSixList(syncDataLogs);
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
     * 同步胎圈库存
     * T_TQ_STOCK：采用逻辑删除+插入方案
     *   步骤1：逻辑删除当天库存日期的所有数据（IS_DELETE置为1）
     *   步骤2：将MES最新库存数据批量插入（新记录，IS_DELETE=0）
     *   历史数据保留，只删当天库存日期的数据
     *
     * @param syncDataLogs 同步参数
     * @return 结果
     */
    @Override
    public AjaxResult syncMesTqStock(AuxReqSyncDataLogs syncDataLogs) {
        // 切换到MES数据源查询中间表
        DynamicDataSourceContextHolder.push(DataSource.MES);
        List<TqMesStock> syncList = mesItfMapper.selectMesTqStockList(syncDataLogs);
        DynamicDataSourceContextHolder.poll();

        // 按库存日期+物料编码去重，保留第一条
        Map<String, TqMesStock> groupMap = syncList.stream()
                .collect(Collectors.toMap(
                        item -> DateUtil.formatDate(item.getStockDate()) + "|" + item.getMaterialCode(),
                        Function.identity(),
                        (v1, v2) -> v1
                ));
        syncList = new ArrayList<>(groupMap.values());

        if (CollectionUtils.isEmpty(syncList)) {
            log.warn("胎圈库存同步：MES中间表查询结果为空");
            return AjaxResult.success("MES中间表无数据可同步");
        }

        // 转换 TqMesStock → TqStock
        List<TqStock> tqStockInsertList = syncList.stream().map(item -> {
            TqStock tqStock = new TqStock();
            tqStock.setStockDate(item.getStockDate());
            tqStock.setBeadCode(item.getMaterialCode());
            tqStock.setStockNum(item.getAvailableStock() != null ? item.getAvailableStock() : BigDecimal.ZERO);
            tqStock.setCreateBy("MES");
            tqStock.setUpdateBy("MES");
            tqStock.setCreateTime(DateUtils.getNowDate());
            tqStock.setUpdateTime(DateUtils.getNowDate());
            return tqStock;
        }).collect(Collectors.toList());

        try {
            // 取第一条数据的库存日期作为逻辑删除条件
            Date stockDate = tqStockInsertList.stream()
                    .map(TqStock::getStockDate)
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(DateUtils.getNowDate());
            String stockDateStr = DateUtil.formatDate(stockDate);

            log.info("胎圈库存同步：开始同步，待插入数量={}, 库存日期={}", tqStockInsertList.size(), stockDateStr);

            FeignTokenHelper.runWithToken(() -> {
                tqMesSyncRemoteService.logicDeleteAndSaveTqStockByStockDate(stockDateStr, "MES", tqStockInsertList);
            });

            log.info("胎圈库存同步：同步完成，插入数量={}", tqStockInsertList.size());
        } catch (Exception e) {
            log.error("胎圈库存同步：Feign调用异常，待插入数量={}", tqStockInsertList.size(), e);
            return AjaxResult.error("胎圈库存同步失败：" + e.getMessage());
        }
        return AjaxResult.success();
    }

    /**
     * 同步钢丝圈库存
     * 从MES中间表MES_GSQ_STOCK查询全量数据，
     * 逻辑删除APS旧数据并插入新数据
     *
     * @param syncDataLogs 同步参数
     * @return 结果
     */
    @Override
    public AjaxResult syncMesGsqStock(AuxReqSyncDataLogs syncDataLogs) {
        // 切换到MES数据源查询中间表
        DynamicDataSourceContextHolder.push(DataSource.MES);
        List<GsqStock> syncList = mesItfMapper.selectMesGsqStockList(syncDataLogs);
        DynamicDataSourceContextHolder.poll();

        if (CollectionUtils.isEmpty(syncList)) {
            log.warn("钢丝圈库存同步：MES中间表查询结果为空");
            return AjaxResult.success("MES中间表无数据可同步");
        }

        // 按库存日期+钢丝圈代码去重，保留第一条
        Map<String, GsqStock> groupMap = syncList.stream()
                .collect(Collectors.toMap(
                        item -> DateUtil.formatDate(item.getStockDate()) + "|" + item.getSteelRingCode(),
                        Function.identity(),
                        (v1, v2) -> v1
                ));
        syncList = new ArrayList<>(groupMap.values());

        // 补审计字段
        List<GsqStock> gsqStockInsertList = syncList.stream().map(item -> {
            item.setCreateBy("MES");
            item.setUpdateBy("MES");
            item.setCreateTime(DateUtils.getNowDate());
            item.setUpdateTime(DateUtils.getNowDate());
            return item;
        }).collect(Collectors.toList());

        try {
            // 取第一条数据的库存日期作为逻辑删除条件
            Date stockDate = gsqStockInsertList.stream()
                    .map(GsqStock::getStockDate)
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(DateUtils.getNowDate());
            String stockDateStr = DateUtil.formatDate(stockDate);

            log.info("钢丝圈库存同步：开始同步，待插入数量={}, 库存日期={}", gsqStockInsertList.size(), stockDateStr);

            FeignTokenHelper.runWithToken(() -> {
                gsqMesSyncRemoteService.logicDeleteAndSaveGsqStockByStockDate(stockDateStr, "MES", gsqStockInsertList);
            });

            log.info("钢丝圈库存同步：同步完成，插入数量={}", gsqStockInsertList.size());
        } catch (Exception e) {
            log.error("钢丝圈库存同步：Feign调用异常，待插入数量={}", gsqStockInsertList.size(), e);
            return AjaxResult.error("钢丝圈库存同步失败：" + e.getMessage());
        }
        return AjaxResult.success();
    }

    /**
     * 从MES读取指定物理日的胎圈库存，并替换自动滚动班次快照。
     *
     * <p>对齐胎面 syncTreadShiftStock，按工厂+物理日+班序从 MES_TQ_STOCK 取最新版本数据，
     * 转换为 TqShiftStock 后远程调用 tqMesSyncRemoteService.replaceShiftStock 替换快照。</p>
     *
     * <p>MES无数据时仍调用 TQ 清空对应快照，防止自动滚动继续使用旧库存。
     * 动态数据源上下文始终在finally中恢复，避免查询异常污染后续线程调用。</p>
     *
     * @param request 工厂、物理库存日和班序
     * @return 同步数量
     * @throws ServiceException 参数非法或远程保存失败时抛出
     */
    @Override
    public AjaxResult syncBeadShiftStock(MesShiftStockSyncRequest request) {
        if (request == null || request.getStockDate() == null || request.getShiftOrder() == null
                || request.getShiftOrder() < 1 || request.getShiftOrder() > 6) {
            throw new ServiceException(I18nUtil.getMessage("ui.itf.mes.shiftStockArgumentsInvalid"));
        }
        request.setFactoryCode(StringUtils.defaultIfBlank(request.getFactoryCode(),
                FactoryConstant.DEFAULT_FACTORY_CODE));
        request.setCompanyCode(StringUtils.defaultIfBlank(request.getCompanyCode(), request.getFactoryCode()));
        request.setStockDate(DateUtil.beginOfDay(request.getStockDate()));
        List<TqMesStock> sourceList;
        DynamicDataSourceContextHolder.push(DataSource.MES);
        try {
            sourceList = this.mesItfMapper.selectBeadShiftStockList(request);
        } finally {
            DynamicDataSourceContextHolder.poll();
        }
        Map<String, TqMesStock> uniqueMap = CollectionUtils.emptyIfNull(sourceList).stream()
                .filter(source -> StringUtils.isNotBlank(source.getMaterialCode()))
                .collect(Collectors.toMap(TqMesStock::getMaterialCode, Function.identity(),
                        (first, ignored) -> first, LinkedHashMap::new));
        List<TqShiftStock> stockList = uniqueMap.values().stream().map(source -> {
            TqShiftStock target = new TqShiftStock();
            target.setFactoryCode(request.getFactoryCode());
            target.setStockDate(request.getStockDate());
            target.setShiftOrder(request.getShiftOrder());
            target.setBeadCode(source.getMaterialCode());
            target.setStockQty(BigDecimalUtils.valueOf(source.getAvailableStock()));
            target.setBadQty(BigDecimal.ZERO);
            target.setAdjustQty(BigDecimal.ZERO);
            return target;
        }).collect(Collectors.toList());
        AjaxResult saveResult = FeignTokenHelper.callWithToken(() ->
                this.tqMesSyncRemoteService.replaceShiftStock(request.getFactoryCode(),
                        DateUtil.formatDate(request.getStockDate()), request.getShiftOrder(), "MES", stockList));
        if (saveResult == null || !Objects.equals(AJAX_SUCCESS_CODE,
                saveResult.get(AjaxResult.CODE_TAG))) {
            throw new ServiceException(I18nUtil.getMessage("ui.itf.mes.shiftStockRemoteFailed"));
        }
        return AjaxResult.success(stockList.size());
    }

    /**
     * 同步胎圈排程完成量
     * 从MES中间表MES_TQ_CLASS_FINISH_QTY查询当天最新版本数据，
     * 逻辑删除APS旧数据并插入新数据，最后回写胎圈排程结果表各班次完成量
     *
     * @param syncDataLogs 同步参数
     * @return 结果
     */
    @Override
    public AjaxResult syncTqClassShiftFinishQty(AuxReqSyncDataLogs syncDataLogs) {
        DynamicDataSourceContextHolder.push(DataSource.MES);
        List<TqScheFinishQty> syncList = mesItfMapper.selectTqClassShiftFinishQtyList(syncDataLogs);
        DynamicDataSourceContextHolder.poll();

        if (CollectionUtils.isEmpty(syncList)) {
            log.warn("胎圈排程完成量同步：MES中间表查询结果为空，factoryCode={}", syncDataLogs.getFactoryCode());
            return AjaxResult.success("MES中间表无数据可同步");
        }

        List<TqScheFinishQty> insertList = new ArrayList<>();
        for (TqScheFinishQty item : syncList) {
            TqScheFinishQty entity = new TqScheFinishQty();
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
            Date scheduleDate = insertList.stream().map(TqScheFinishQty::getScheduleDate).filter(Objects::nonNull).findFirst().orElse(DateUtils.getNowDate());
            String scheduleDateStr = DateUtil.formatDate(scheduleDate);
            log.info("胎圈排程完成量同步：开始同步，factoryCode={}, scheduleDate={}, 待插入数量={}", factoryCode, scheduleDateStr, insertList.size());

            // 接收Feign返回值并校验，避免服务端异常被全局异常处理器吞掉返回HTTP 200+AjaxResult.error时，itf端误判为成功
            AjaxResult saveResult = FeignTokenHelper.callWithToken(() ->
                    tqMesSyncRemoteService.logicDeleteAndSaveScheFinishQty(factoryCode, scheduleDateStr, "MES", insertList));
            if (AJAX_SUCCESS_CODE != (Integer) saveResult.get(AjaxResult.CODE_TAG)) {
                log.error("胎圈排程完成量同步：同步失败，factoryCode={}, 返回code={}, 返回消息={}",
                        factoryCode, saveResult.get(AjaxResult.CODE_TAG), saveResult.get(AjaxResult.MSG_TAG));
                return AjaxResult.error("胎圈排程完成量同步失败：" + saveResult.get(AjaxResult.MSG_TAG));
            }

            log.info("胎圈排程完成量同步：同步完成，factoryCode={}, 插入数量={}", factoryCode, insertList.size());
        } catch (Exception e) {
            log.error("胎圈排程完成量同步：Feign调用异常，factoryCode={}, 待插入数量={}", syncDataLogs.getFactoryCode(), insertList.size(), e);
            return AjaxResult.error("胎圈排程完成量同步失败：" + e.getMessage());
        }

        try {
            FeignTokenHelper.runWithToken(() -> {
                tqMesSyncRemoteService.writeBackScheduleResultFinishQty(insertList);
            });
        } catch (Exception e) {
            log.error("【胎圈排程完成量回写】回写胎圈排程结果表完成量异常", e);
        }
        return AjaxResult.success();
    }

    /**
     * 同步胎圈排程日完成量
     * 从MES中间表MES_TQ_DAY_FINISH_TOTL查询前一天的数据，
     * 逻辑删除APS旧数据并插入新数据
     *
     * @param syncDataLogs 同步参数
     * @return 结果
     */
    @Override
    public AjaxResult syncTqScheDayFinishQty(AuxReqSyncDataLogs syncDataLogs) {
        DynamicDataSourceContextHolder.push(DataSource.MES);
        Date nowDate = DateUtils.truncate(DateUtils.getNowDate(), Calendar.DATE);
        Date lastDate = DateUtils.addDays(nowDate, -1);
        syncDataLogs.setQueryParams(new HashMap<>());
        syncDataLogs.getQueryParams().put("scheduleDate", lastDate);
        List<TqDayFinishQty> syncList = mesItfMapper.selectTqScheDayFinishQtyList(syncDataLogs);
        DynamicDataSourceContextHolder.poll();

        if (CollectionUtils.isEmpty(syncList)) {
            log.warn("胎圈排程日完成量同步：MES中间表查询结果为空，factoryCode={}", syncDataLogs.getFactoryCode());
            return AjaxResult.success("MES中间表无数据可同步");
        }

        Map<String, TqDayFinishQty> groupMap = syncList.stream()
                .collect(Collectors.toMap(
                        item -> item.getFactoryCode() + "|" + DateUtil.formatDate(item.getScheduleDate()) + "|" + item.getBeadCode(),
                        Function.identity(),
                        (v1, v2) -> v1
                ));
        syncList = new ArrayList<>(groupMap.values());

        List<TqDayFinishQty> insertList = new ArrayList<>();
        for (TqDayFinishQty item : syncList) {
            TqDayFinishQty entity = new TqDayFinishQty();
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
            Date scheduleDate = insertList.stream().map(TqDayFinishQty::getScheduleDate).filter(Objects::nonNull).findFirst().orElse(DateUtils.getNowDate());
            String scheduleDateStr = DateUtil.formatDate(scheduleDate);
            log.info("胎圈排程日完成量同步：开始同步，factoryCode={}, scheduleDate={}, 待插入数量={}", factoryCode, scheduleDateStr, insertList.size());

            FeignTokenHelper.runWithToken(() -> {
                tqMesSyncRemoteService.logicDeleteAndSaveDayFinishQty(factoryCode, scheduleDateStr, "MES", insertList);
            });

            log.info("胎圈排程日完成量同步：同步完成，factoryCode={}, 插入数量={}", factoryCode, insertList.size());
        } catch (Exception e) {
            log.error("胎圈排程日完成量同步：Feign调用异常，factoryCode={}, 待插入数量={}", syncDataLogs.getFactoryCode(), insertList.size(), e);
            return AjaxResult.error("胎圈排程日完成量同步失败：" + e.getMessage());
        }
        return AjaxResult.success();
    }

    /**
     * 同步钢丝圈排程日完成量
     * 从MES中间表MES_GSQ_DAY_FINISH_TOTL查询前一天的数据，
     * 逻辑删除APS旧数据并插入新数据
     *
     * @param syncDataLogs 同步参数
     * @return 结果
     */
    @Override
    public AjaxResult syncGsqScheDayFinishQty(AuxReqSyncDataLogs syncDataLogs) {
        DynamicDataSourceContextHolder.push(DataSource.MES);
        Date nowDate = DateUtils.truncate(DateUtils.getNowDate(), Calendar.DATE);
        Date lastDate = DateUtils.addDays(nowDate, -1);
        syncDataLogs.setQueryParams(new HashMap<>());
        syncDataLogs.getQueryParams().put("scheduleDate", lastDate);
        List<GsqDayFinishQty> syncList = mesItfMapper.selectGsqScheDayFinishQtyList(syncDataLogs);
        DynamicDataSourceContextHolder.poll();

        if (CollectionUtils.isEmpty(syncList)) {
            log.warn("钢丝圈排程日完成量同步：MES中间表查询结果为空，factoryCode={}", syncDataLogs.getFactoryCode());
            return AjaxResult.success("MES中间表无数据可同步");
        }

        Map<String, GsqDayFinishQty> groupMap = syncList.stream()
                .collect(Collectors.toMap(
                        item -> item.getFactoryCode() + "|" + DateUtil.formatDate(item.getScheduleDate()) + "|" + item.getSteelRingCode(),
                        Function.identity(),
                        (v1, v2) -> v1
                ));
        syncList = new ArrayList<>(groupMap.values());

        List<GsqDayFinishQty> insertList = new ArrayList<>();
        for (GsqDayFinishQty item : syncList) {
            GsqDayFinishQty entity = new GsqDayFinishQty();
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
            Date scheduleDate = insertList.stream().map(GsqDayFinishQty::getScheduleDate).filter(Objects::nonNull).findFirst().orElse(DateUtils.getNowDate());
            String scheduleDateStr = DateUtil.formatDate(scheduleDate);
            log.info("钢丝圈排程日完成量同步：开始同步，factoryCode={}, scheduleDate={}, 待插入数量={}", factoryCode, scheduleDateStr, insertList.size());

            FeignTokenHelper.runWithToken(() -> {
                gsqMesSyncRemoteService.logicDeleteAndSaveDayFinishQty(factoryCode, scheduleDateStr, "MES", insertList);
            });

            log.info("钢丝圈排程日完成量同步：同步完成，factoryCode={}, 插入数量={}", factoryCode, insertList.size());
        } catch (Exception e) {
            log.error("钢丝圈排程日完成量同步：Feign调用异常，factoryCode={}, 待插入数量={}", syncDataLogs.getFactoryCode(), insertList.size(), e);
            return AjaxResult.error("钢丝圈排程日完成量同步失败：" + e.getMessage());
        }
        return AjaxResult.success();
    }

    /**
     * 实时查询MES生胎库存（不写入APS本地表，仅供成型排程实时调用）
     * 直接从MES中间表查询，映射为CxStock返回，不经过CxMesStock中间转换
     *
     * @param syncDataLogs 查询参数（可传factoryCode过滤分厂）
     * @return 生胎库存列表
     */
    @Override
    public List<CxStock> getCxStock(AuxReqSyncDataLogs syncDataLogs) {
        DynamicDataSourceContextHolder.push(DataSource.MES);
        List<CxStock> stockList = mesItfMapper.selectCxStockFromMes(syncDataLogs);
        DynamicDataSourceContextHolder.poll();

        if (CollectionUtils.isEmpty(stockList)) {
            log.warn("实时查询MES生胎库存：MES中间表查询结果为空，factoryCode={}", syncDataLogs.getFactoryCode());
            return stockList;
        }

        // 按分厂+胎胚编码去重（取第一条）
        Map<String, CxStock> groupMap = stockList.stream()
                .collect(Collectors.toMap(
                        item -> item.getFactoryCode() + "|" + item.getEmbryoCode(),
                        Function.identity(),
                        (v1, v2) -> v1
                ));
        stockList = new ArrayList<>(groupMap.values());

        // 设置数据来源标识
        stockList.forEach(item -> item.setDataSource(ApsConstant.DATA_SOURCE_MES));

        log.info("实时查询MES生胎库存：查询完成，factoryCode={}, 返回数量={}", syncDataLogs.getFactoryCode(), stockList.size());
        return stockList;
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

            // 接收Feign返回值并校验，避免服务端异常被全局异常处理器吞掉返回HTTP 200+AjaxResult.error时，itf端误判为成功
            AjaxResult saveResult = FeignTokenHelper.callWithToken(() ->
                    cxMesSyncRemoteService.logicDeleteAndSaveScheFinishQty(factoryCode, scheduleDateStr, "MES", insertList));
            if (AJAX_SUCCESS_CODE != (Integer) saveResult.get(AjaxResult.CODE_TAG)) {
                log.error("成型排程完成量同步：同步失败，factoryCode={}, 返回code={}, 返回消息={}",
                        factoryCode, saveResult.get(AjaxResult.CODE_TAG), saveResult.get(AjaxResult.MSG_TAG));
                return AjaxResult.error("成型排程完成量同步失败：" + saveResult.get(AjaxResult.MSG_TAG));
            }

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

            // 接收Feign返回值并校验，避免服务端异常被全局异常处理器吞掉返回HTTP 200+AjaxResult.error时，itf端误判为成功
            AjaxResult saveResult = FeignTokenHelper.callWithToken(() ->
                    lhMesSyncRemoteService.logicDeleteAndSaveScheFinishQty(factoryCode, scheduleDateStr, "MES", insertList));
            if (AJAX_SUCCESS_CODE != (Integer) saveResult.get(AjaxResult.CODE_TAG)) {
                log.error("硫化排程完成量同步：同步失败，factoryCode={}, 返回code={}, 返回消息={}",
                        factoryCode, saveResult.get(AjaxResult.CODE_TAG), saveResult.get(AjaxResult.MSG_TAG));
                return AjaxResult.error("硫化排程完成量同步失败：" + saveResult.get(AjaxResult.MSG_TAG));
            }

            log.info("硫化排程完成量同步：同步完成，factoryCode={}, 插入数量={}", factoryCode, insertList.size());
        } catch (Exception e) {
            log.error("硫化排程完成量同步：Feign调用异常，factoryCode={}, 待插入数量={}", syncDataLogs.getFactoryCode(), insertList.size(), e);
            return AjaxResult.error("硫化排程完成量同步失败：" + e.getMessage());
        }

        // 回写硫化排程结果表：接收Feign返回值并校验，避免回写失败被吞导致接口误判成功
        try {
            AjaxResult writeBackResult = FeignTokenHelper.callWithToken(() ->
                    lhMesSyncRemoteService.writeBackScheduleResultFinishQty(insertList));
            if (AJAX_SUCCESS_CODE != (Integer) writeBackResult.get(AjaxResult.CODE_TAG)) {
                log.error("硫化排程完成量回写失败：factoryCode={}, 返回code={}, 返回消息={}",
                        syncDataLogs.getFactoryCode(), writeBackResult.get(AjaxResult.CODE_TAG), writeBackResult.get(AjaxResult.MSG_TAG));
                return AjaxResult.error("硫化排程完成量回写失败：" + writeBackResult.get(AjaxResult.MSG_TAG));
            }
            log.info("硫化排程完成量回写完成：factoryCode={}, 回写数据条数={}", syncDataLogs.getFactoryCode(), insertList.size());
        } catch (Exception e) {
            log.error("硫化排程完成量回写Feign调用异常：factoryCode={}, 待回写数据条数={}",
                    syncDataLogs.getFactoryCode(), insertList.size(), e);
            return AjaxResult.error("硫化排程完成量回写失败：" + e.getMessage());
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

            // 接收Feign返回值并校验，避免服务端异常被全局异常处理器吞掉返回HTTP 200+AjaxResult.error时，itf端误判为成功
            AjaxResult saveResult = FeignTokenHelper.callWithToken(() ->
                    lhMesSyncRemoteService.logicDeleteAndSaveScheFinishQty(factoryCode, scheduleDateStr, "MES", insertList));
            if (AJAX_SUCCESS_CODE != (Integer) saveResult.get(AjaxResult.CODE_TAG)) {
                log.error("硫化排程完成量按上一天最新版本同步：同步失败，factoryCode={}, 返回code={}, 返回消息={}",
                        factoryCode, saveResult.get(AjaxResult.CODE_TAG), saveResult.get(AjaxResult.MSG_TAG));
                return AjaxResult.error("硫化排程完成量按上一天最新版本同步失败：" + saveResult.get(AjaxResult.MSG_TAG));
            }

            log.info("硫化排程完成量按上一天最新版本同步：同步完成，factoryCode={}, 插入数量={}", factoryCode, insertList.size());
        } catch (Exception e) {
            log.error("硫化排程完成量按上一天最新版本同步：Feign调用异常，factoryCode={}, 待插入数量={}", syncDataLogs.getFactoryCode(), insertList.size(), e);
            return AjaxResult.error("硫化排程完成量按上一天最新版本同步失败：" + e.getMessage());
        }

        // 回写硫化排程结果表：接收Feign返回值并校验，避免回写失败被吞导致接口误判成功
        try {
            AjaxResult writeBackResult = FeignTokenHelper.callWithToken(() ->
                    lhMesSyncRemoteService.writeBackScheduleResultFinishQty(insertList));
            if (AJAX_SUCCESS_CODE != (Integer) writeBackResult.get(AjaxResult.CODE_TAG)) {
                log.error("硫化排程完成量按上一天最新版本回写失败：factoryCode={}, 返回code={}, 返回消息={}",
                        syncDataLogs.getFactoryCode(), writeBackResult.get(AjaxResult.CODE_TAG), writeBackResult.get(AjaxResult.MSG_TAG));
                return AjaxResult.error("硫化排程完成量按上一天最新版本回写失败：" + writeBackResult.get(AjaxResult.MSG_TAG));
            }
            log.info("硫化排程完成量按上一天最新版本回写完成：factoryCode={}, 回写数据条数={}",
                    syncDataLogs.getFactoryCode(), insertList.size());
        } catch (Exception e) {
            log.error("硫化排程完成量按上一天最新版本回写Feign调用异常：factoryCode={}, 待回写数据条数={}",
                    syncDataLogs.getFactoryCode(), insertList.size(), e);
            return AjaxResult.error("硫化排程完成量按上一天最新版本回写失败：" + e.getMessage());
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
                // 接收Feign返回值并校验，避免服务端异常被全局异常处理器吞掉返回HTTP 200+AjaxResult.error时，itf端误判为成功
                AjaxResult saveResult = FeignTokenHelper.callWithToken(() ->
                        lhMesSyncRemoteService.logicDeleteAndSaveScheFinishQty(finalFactoryCode, scheduleDateStr, "MES", groupList));
                if (AJAX_SUCCESS_CODE != (Integer) saveResult.get(AjaxResult.CODE_TAG)) {
                    log.error("硫化排程完成量按版本号同步：同步失败，dataVersion={}, factoryCode={}, scheduleDate={}, 返回code={}, 返回消息={}",
                            dataVersion, factoryCode, scheduleDateStr, saveResult.get(AjaxResult.CODE_TAG), saveResult.get(AjaxResult.MSG_TAG));
                    return AjaxResult.error("硫化排程完成量按版本号同步失败：" + saveResult.get(AjaxResult.MSG_TAG));
                }

                log.info("硫化排程完成量按版本号同步：同步完成，dataVersion={}, factoryCode={}, scheduleDate={}, 插入数量={}",
                        dataVersion, factoryCode, scheduleDateStr, groupList.size());
            } catch (Exception e) {
                log.error("硫化排程完成量按版本号同步：Feign调用异常，dataVersion={}, factoryCode={}, scheduleDate={}",
                        dataVersion, factoryCode, scheduleDateStr, e);
                return AjaxResult.error("硫化排程完成量按版本号同步失败：" + e.getMessage());
            }
        }

        // 回写硫化排程结果表：接收Feign返回值并校验，避免回写失败被吞导致接口误判成功
        try {
            AjaxResult writeBackResult = FeignTokenHelper.callWithToken(() ->
                    lhMesSyncRemoteService.writeBackScheduleResultFinishQty(insertList));
            if (AJAX_SUCCESS_CODE != (Integer) writeBackResult.get(AjaxResult.CODE_TAG)) {
                log.error("硫化排程完成量按版本号回写失败：dataVersion={}, 返回code={}, 返回消息={}",
                        dataVersion, writeBackResult.get(AjaxResult.CODE_TAG), writeBackResult.get(AjaxResult.MSG_TAG));
                return AjaxResult.error("硫化排程完成量按版本号回写失败：" + writeBackResult.get(AjaxResult.MSG_TAG));
            }
            log.info("硫化排程完成量按版本号回写完成：dataVersion={}, 回写数据条数={}", dataVersion, insertList.size());
        } catch (Exception e) {
            log.error("硫化排程完成量按版本号回写Feign调用异常：dataVersion={}, 待回写数据条数={}",
                    dataVersion, insertList.size(), e);
            return AjaxResult.error("硫化排程完成量按版本号回写失败：" + e.getMessage());
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
     * MES数据返回周期已由单日变更为多日（如7/10返回7/3至7/9的连续数据），
     * 采用按最新版本号查询MES中间表，获取多日完成量数据后：
     * 1. 逻辑删除前查询窗口范围内的旧数据，按芯片编码汇总（用于芯片库存差值更新）
     * 2. 按完成日期分组，逐组调用逻辑删除+插入
     * 3. 月计划监控按各完成日期的年月分别更新
     * 4. 芯片库存按差值更新（FINISH_QTY += 新值 - 旧值），避免多日滚动数据重复累加
     * @param syncDataLogs 同步参数
     * @return 结果
     */
    @Override
    public AjaxResult syncLhScheDayFinishQty(AuxReqSyncDataLogs syncDataLogs) {
        // 查询MES中间表最新版本号
        DynamicDataSourceContextHolder.push(DataSource.MES);
        String latestVersion = mesItfMapper.selectMaxDataVersionFromLhDayFinishQty(syncDataLogs.getFactoryCode());
        if (StringUtils.isBlank(latestVersion)) {
            DynamicDataSourceContextHolder.poll();
            log.warn("硫化排程日完成量同步：MES中间表无版本号数据，factoryCode={}", syncDataLogs.getFactoryCode());
            return AjaxResult.success("MES中间表无数据可同步");
        }
        log.info("硫化排程日完成量同步：查询到最新版本号={}，factoryCode={}", latestVersion, syncDataLogs.getFactoryCode());

        // 按最新版本号查询MES中间表多日数据（不传finishDate，取该版本下所有日期数据）
        syncDataLogs.setDataVersion(latestVersion);
        syncDataLogs.setQueryParams(new HashMap<>());
        List<LhDayFinishQty> syncList = mesItfMapper.selectLhScheDayFinishQtyList(syncDataLogs);
        DynamicDataSourceContextHolder.poll();

        if (CollectionUtils.isEmpty(syncList)) {
            log.warn("硫化排程日完成量同步：MES中间表查询结果为空，factoryCode={}, dataVersion={}", syncDataLogs.getFactoryCode(), latestVersion);
            return AjaxResult.success("MES中间表无数据可同步");
        }

        // 分组去重：工厂|完成日期|物料|MES物料|示方类型（不同示方类型的示方号可能相同，需用示方类型做维度）
        Map<String, LhDayFinishQty> groupMap = syncList.stream()
                .collect(Collectors.toMap(
                        item -> item.getFactoryCode() + "|" + item.getFinishDate() + "|" + item.getMaterialCode() + "|" + item.getMesMaterialCode() + "|" + item.getLhType(),
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

        // 量试合格品充抵正规订单：由开关 SYS0312001 控制，
        // 开启时在 handleTrialFinishQtyRule 之前调用，
        // 判断条件：同一完成日期+同一SKU在 MES 回传数据中同时存在量试(T)和正规(S)两条数据时，
        // 额外插入一条正规(S)记录（值取自量试T数据），用于量试合格品充抵正规订单。
        // 注意：必须在 handleTrialFinishQtyRule 之前调用，因为充抵记录使用的是 MES 原始值，不能被试制/量试规则调整。
        // 新增的正规记录 lhType=S，handleTrialFinishQtyRule 会跳过（isTrialOrMassTrial 返回 false），不受影响。
        if (this.isMassTrialToFormalEnabled(syncDataLogs.getFactoryCode())) {
            List<LhDayFinishQty> extraFormalRecords = this.buildMassTrialToFormalRecords(
                    syncDataLogs.getFactoryCode(), syncList);
            if (CollectionUtils.isNotEmpty(extraFormalRecords)) {
                // 补充审计字段（与 syncLhScheDayFinishQtyByLatestVersion 保持一致）
                for (LhDayFinishQty extra : extraFormalRecords) {
                    extra.setCreateBy("MES");
                    extra.setUpdateBy("MES");
                    extra.setCreateTime(DateUtils.getNowDate());
                    extra.setUpdateTime(DateUtils.getNowDate());
                    extra.setIsDelete(0);
                }
                insertList.addAll(extraFormalRecords);
            }
        } else {
            log.info("量试充抵正规开关已关闭（SYS0312001=0），跳过量试→正规充抵记录，factoryCode={}",
                    syncDataLogs.getFactoryCode());
        }

        // 处理试制/量试完成量回报规则（跨日合并计划量；≤日计划量按计划量回报、>日计划量按实际回报；
        // 无计划量视为0，按规则3.1/3.2处理，不过滤不同步）
        this.handleTrialFinishQtyRule(insertList, syncDataLogs.getFactoryCode());
        if (CollectionUtils.isEmpty(insertList)) {
            log.info("硫化排程日完成量同步：试制/量试规则处理后待同步列表为空，factoryCode={}", syncDataLogs.getFactoryCode());
            return AjaxResult.success("试制/量试规则处理后无数据可同步");
        }

        String factoryCode = syncDataLogs.getFactoryCode();

        // 提取窗口日期范围：min(finishDate) ~ max(finishDate)
        Date minFinishDate = insertList.stream().map(LhDayFinishQty::getFinishDate)
                .filter(Objects::nonNull).min(Date::compareTo).orElse(DateUtils.getNowDate());
        Date maxFinishDate = insertList.stream().map(LhDayFinishQty::getFinishDate)
                .filter(Objects::nonNull).max(Date::compareTo).orElse(DateUtils.getNowDate());
        String minDateStr = DateUtil.formatDate(minFinishDate);
        String maxDateStr = DateUtil.formatDate(maxFinishDate);
        log.info("硫化排程日完成量同步：窗口日期范围={} ~ {}，factoryCode={}", minDateStr, maxDateStr, factoryCode);

        // 【芯片库存差值更新-步骤1】逻辑删除前查询窗口范围内的旧数据，按芯片编码汇总
        // 必须在逻辑删除之前执行，否则旧数据已被删除，无法查询
        Map<String, Integer> oldChipFinishQtyMap = queryOldChipFinishQty(factoryCode, minDateStr, maxDateStr);

        // 按完成日期分组，逐组同步（原逻辑按单个完成日期做逻辑删除+插入）
        Map<String, List<LhDayFinishQty>> groupByFinishDate = insertList.stream()
                .collect(Collectors.groupingBy(item -> {
                    Date finishDate = item.getFinishDate();
                    return finishDate != null ? DateUtil.formatDate(finishDate) : "unknown";
                }));

        for (Map.Entry<String, List<LhDayFinishQty>> entry : groupByFinishDate.entrySet()) {
            String finishDateStr = entry.getKey();
            List<LhDayFinishQty> groupList = entry.getValue();
            String groupFactoryCode = groupList.get(0).getFactoryCode();

            try {
                log.info("硫化排程日完成量同步：开始同步，factoryCode={}, finishDate={}, 待插入数量={}", groupFactoryCode, finishDateStr, groupList.size());

                String finalFactoryCode = groupFactoryCode;
                FeignTokenHelper.runWithToken(() -> {
                    lhMesSyncRemoteService.logicDeleteAndSaveDayFinishQty(finalFactoryCode, finishDateStr, "MES", groupList);
                });

                log.info("硫化排程日完成量同步：同步完成，factoryCode={}, finishDate={}, 插入数量={}", groupFactoryCode, finishDateStr, groupList.size());
            } catch (Exception e) {
                log.error("硫化排程日完成量同步：Feign调用异常，factoryCode={}, finishDate={}", groupFactoryCode, finishDateStr, e);
                return AjaxResult.error("硫化排程日完成量同步失败：" + e.getMessage());
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
            String[] parts = yearMonth.split("-");
            Integer year = Integer.parseInt(parts[0]);
            Integer month = Integer.parseInt(parts[1]);
            try {
                MpMonthPlanMonitor paramVo = new MpMonthPlanMonitor();
                paramVo.setFactoryCode(factoryCode);
                paramVo.setYear(year);
                paramVo.setMonth(month);
                DynamicDataSourceContextHolder.push(DataSource.APS);
                mpMonthPlanMonitorEntityMapper.updateByDayFinish(paramVo);
            } finally {
                DynamicDataSourceContextHolder.poll();
            }
        }

        // 【芯片库存差值更新-步骤2】按差值更新芯片库存（FINISH_QTY += 新值 - 旧值）
        updateChipStockFinishQty(factoryCode, syncList, oldChipFinishQtyMap, latestVersion);
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

        // 分组去重：工厂|完成日期|物料|MES物料|示方类型（不同示方类型的示方号可能相同，需用示方类型做维度）
        Map<String, LhDayFinishQty> groupMap = syncList.stream()
                .collect(Collectors.toMap(
                        item -> item.getFactoryCode() + "|" + item.getFinishDate() + "|" + item.getMaterialCode() + "|" + item.getMesMaterialCode() + "|" + item.getLhType(),
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

        // 量试合格品充抵正规订单：由开关 SYS0312001 控制，
        // 开启时在 handleTrialFinishQtyRule 之前调用，
        // 判断条件：同一完成日期+同一SKU在 MES 回传数据中同时存在量试(T)和正规(S)两条数据时，
        // 额外插入一条正规(S)记录（值取自量试T数据），用于量试合格品充抵正规订单。
        // 注意：必须在 handleTrialFinishQtyRule 之前调用，因为充抵记录使用的是 MES 原始值，不能被试制/量试规则调整。
        // 新增的正规记录 lhType=S，handleTrialFinishQtyRule 会跳过（isTrialOrMassTrial 返回 false），不受影响。
        String factoryCodeForBuild = insertList.get(0).getFactoryCode();
        if (this.isMassTrialToFormalEnabled(factoryCodeForBuild)) {
            List<LhDayFinishQty> extraFormalRecords = this.buildMassTrialToFormalRecords(
                    factoryCodeForBuild, syncList);
            if (CollectionUtils.isNotEmpty(extraFormalRecords)) {
                // 补充审计字段
                for (LhDayFinishQty extra : extraFormalRecords) {
                    extra.setCreateBy("MES");
                    extra.setUpdateBy("MES");
                    extra.setCreateTime(DateUtils.getNowDate());
                    extra.setUpdateTime(DateUtils.getNowDate());
                    extra.setIsDelete(0);
                }
                insertList.addAll(extraFormalRecords);
            }
        } else {
            log.info("量试充抵正规开关已关闭（SYS0312001=0），跳过量试→正规充抵记录，factoryCode={}",
                    factoryCodeForBuild);
        }

        // 处理试制/量试完成量回报规则（跨日合并计划量；≤日计划量按计划量回报、>日计划量按实际回报；
        // 无计划量视为0，按规则3.1/3.2处理，不过滤不同步）
        // 注意：此处统一处理所有日期的试制/量试数据，规则处理后再按完成日期分组同步
        String trialFactoryCode = insertList.get(0).getFactoryCode();
        this.handleTrialFinishQtyRule(insertList, trialFactoryCode);
        if (CollectionUtils.isEmpty(insertList)) {
            log.info("硫化排程日完成量按最新版本号同步：试制/量试规则处理后待同步列表为空，dataVersion={}", dataVersion);
            return AjaxResult.success("试制/量试规则处理后无数据可同步");
        }

        // 提取窗口日期范围：min(finishDate) ~ max(finishDate)
        Date minFinishDate = insertList.stream().map(LhDayFinishQty::getFinishDate)
                .filter(Objects::nonNull).min(Date::compareTo).orElse(DateUtils.getNowDate());
        Date maxFinishDate = insertList.stream().map(LhDayFinishQty::getFinishDate)
                .filter(Objects::nonNull).max(Date::compareTo).orElse(DateUtils.getNowDate());
        String minDateStr = DateUtil.formatDate(minFinishDate);
        String maxDateStr = DateUtil.formatDate(maxFinishDate);
        log.info("硫化排程日完成量按最新版本号同步：窗口日期范围={} ~ {}，factoryCode={}", minDateStr, maxDateStr, trialFactoryCode);

        // 【芯片库存差值更新-步骤1】逻辑删除前查询窗口范围内的旧数据，按芯片编码汇总
        Map<String, Integer> oldChipFinishQtyMap = queryOldChipFinishQty(trialFactoryCode, minDateStr, maxDateStr);

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
            String[] parts = yearMonth.split("-");
            Integer year = Integer.parseInt(parts[0]);
            Integer month = Integer.parseInt(parts[1]);
            try {
                MpMonthPlanMonitor paramVo = new MpMonthPlanMonitor();
                paramVo.setFactoryCode(insertList.get(0).getFactoryCode());
                paramVo.setYear(year);
                paramVo.setMonth(month);
                DynamicDataSourceContextHolder.push(DataSource.APS);
                mpMonthPlanMonitorEntityMapper.updateByDayFinish(paramVo);
            } finally {
                DynamicDataSourceContextHolder.poll();
            }
        }

        // 【芯片库存差值更新-步骤2】按差值更新芯片库存完成量
        updateChipStockFinishQty(trialFactoryCode, syncList, oldChipFinishQtyMap, dataVersion);
        return AjaxResult.success();
    }

    /**
     * 处理试制(X)/量试(T)数据的日完成量回报规则。
     * <p>规则：
     * <ul>
     *   <li>1) 计划量查不到（三个班次示方类型均为NULL或排程数据不存在）：过滤不同步</li>
     *   <li>2) 完成量 ≤ 当日计划量：将完成量调整为当日计划量进行回报</li>
     *   <li>3) 完成量 > 当日计划量：保持原实际完成量不变</li>
     * </ul>
     * 当日计划量取自T_LH_SCHEDULE_RESULT，跨日滚动合并三段（MES完成日期D）：
     * <ul>
     *   <li>排程日期D的CLASS3（D日夜班）</li>
     *   <li>排程日期(D+1)的CLASS1（D日早班，滚动排程后D+1版本最新）</li>
     *   <li>排程日期(D+1)的CLASS2（D日中班，滚动排程后D+1版本最新）</li>
     * </ul>
     * 三个班次各自带示方类型，按物料编码+示方类型分组汇总。</p>
     * <p>匹配维度：物料编码 + 示方类型。若三个班次示方类型均为NULL或排程数据不存在，
     * 视为当日计划量=0，按业务规则3.1/3.2处理（完成量>0按实际值回报、完成量<=0按0回报），不再过滤不同步。</p>
     * <p>正规(S)数据不做处理，原样同步。</p>
     * <p>完成量为0或null时：落入规则3.1按日计划量回报（0值放大）。</p>
     *
     * @param insertList 待同步的日完成量列表（会被原地修改：调整dayFinishQty）
     * @param factoryCode 工厂编码
     */
    private void handleTrialFinishQtyRule(List<LhDayFinishQty> insertList, String factoryCode) {
        if (CollectionUtils.isEmpty(insertList)) {
            return;
        }
        // 过滤出试制(X)/量试(T)数据，正规(S)数据保持原样不同步处理
        List<LhDayFinishQty> trialList = insertList.stream()
                .filter(item -> this.isTrialOrMassTrial(item.getLhType()))
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(trialList)) {
            return;
        }

        // 按完成日期分组（用LocalDate避免Date精度问题），逐日查询当日计划量
        Map<java.time.LocalDate, List<LhDayFinishQty>> dateGroupedMap = trialList.stream()
                .filter(item -> item.getFinishDate() != null)
                .collect(Collectors.groupingBy(item ->
                        DateUtil.date(item.getFinishDate()).toLocalDateTime().toLocalDate()));

        // 收集所有涉及的物料编码（试制/量试数据全集）
        List<String> allMaterialCodes = trialList.stream()
                .map(LhDayFinishQty::getMaterialCode)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .collect(Collectors.toList());

        // 完成量为0按日计划量放大的记录计数，用于WARN日志追踪
        int zeroFilledCount = 0;
        // 无计划量(视为0)仍按实际值回报的记录计数，用于INFO日志追踪
        int noPlanQtyKeepCount = 0;

        for (Map.Entry<java.time.LocalDate, List<LhDayFinishQty>> dateEntry : dateGroupedMap.entrySet()) {
            java.time.LocalDate scheduleLocalDate = dateEntry.getKey();
            List<LhDayFinishQty> dayItems = dateEntry.getValue();
            // 取该日期下任意一条数据的finishDate作为查询参数（同一天任意一条都行）
            Date finishDate = dayItems.get(0).getFinishDate();

            // 查询当日计划量（APS数据源，跨日滚动合并CLASS3+CLASS1+CLASS2）
            List<LhDayPlanQtyVo> planQtyList = Collections.emptyList();
            if (!allMaterialCodes.isEmpty()) {
                try {
                    DynamicDataSourceContextHolder.push(DataSource.APS);
                    planQtyList = lhScheduleResultQueryMapper.sumDayPlanQtyByFinishDate(
                            factoryCode, finishDate, allMaterialCodes);
                } finally {
                    DynamicDataSourceContextHolder.poll();
                }
            }

            // 构建 (物料编码 + "|" + 示方类型) -> 日计划量 映射
            Map<String, Integer> planQtyMap = new HashMap<>();
            if (CollectionUtils.isNotEmpty(planQtyList)) {
                for (LhDayPlanQtyVo vo : planQtyList) {
                    if (vo.getMaterialCode() == null || vo.getLhType() == null) {
                        continue;
                    }
                    String key = vo.getMaterialCode() + "|" + vo.getLhType();
                    // 同key理论上不重复（SQL已按物料+示方类型分组），出现重复时累加
                    planQtyMap.merge(key, vo.getDayPlanQty() == null ? 0 : vo.getDayPlanQty(), Integer::sum);
                }
            }

            // 逐条应用试制/量试完成量回报规则
            for (LhDayFinishQty item : dayItems) {
                BigDecimal finishQty = item.getDayFinishQty();
                // 完成量为null时按0处理，统一落入下方 ≤ 日计划量 分支
                boolean isZeroFinishQty = finishQty == null || finishQty.compareTo(BigDecimal.ZERO) == 0;
                if (isZeroFinishQty) {
                    finishQty = BigDecimal.ZERO;
                }
                // 获取当日计划量；若key不存在说明三个班次示方类型均为NULL或排程数据不存在，
                // 视为当日计划量=0（MES回报的试制/量试数据即使APS无对应计划量也应同步），
                // 按业务规则3.1/3.2处理：完成量>0按实际值回报、完成量<=0按0回报
                String key = item.getMaterialCode() + "|" + item.getLhType();
                boolean hasPlanQty = planQtyMap.containsKey(key);
                BigDecimal dayPlanQty = hasPlanQty
                        ? BigDecimal.valueOf(planQtyMap.get(key))
                        : BigDecimal.ZERO;
                // 3.1) 完成量 ≤ 日计划量：按当日计划量回报
                // 注：完成量为0时落入此分支按日计划量回报（日计划量=0时回报0，>0时回报日计划量即0值放大）
                if (finishQty.compareTo(dayPlanQty) <= 0) {
                    if (isZeroFinishQty && dayPlanQty.compareTo(BigDecimal.ZERO) > 0) {
                        zeroFilledCount++;
                    }
                    item.setDayFinishQty(dayPlanQty);
                }
                // 3.2) 完成量 > 日计划量：按实际完成量回报（保持原值，无需处理）
                // 无计划量(=0)且完成量>0时落入此分支，保留MES原始实际完成量
                if (!hasPlanQty && finishQty.compareTo(BigDecimal.ZERO) > 0) {
                    noPlanQtyKeepCount++;
                }
            }

            log.info("试制/量试完成量回报规则处理：factoryCode={}, finishDate={}, 当日计划量匹配数={}, 当日试制/量试数据数={}",
                    factoryCode, scheduleLocalDate, planQtyMap.size(), dayItems.size());
        }

        // 完成量为0但按日计划量放大的记录，输出WARN便于上线追踪影响范围
        if (zeroFilledCount > 0) {
            log.warn("试制/量试完成量回报规则处理：完成量为0按日计划量放大的记录数={}, factoryCode={}",
                    zeroFilledCount, factoryCode);
        }
        // 无计划量(视为0)仍按实际值回报的记录，输出INFO便于上线追踪
        if (noPlanQtyKeepCount > 0) {
            log.info("试制/量试完成量回报规则处理：无计划量(视为0)按实际值回报的记录数={}, factoryCode={}",
                    noPlanQtyKeepCount, factoryCode);
        }
    }

    /**
     * 判断示方类型是否为试制(X)或量试(T)
     *
     * @param lhType 示方类型编码
     * @return true表示试制或量试，false表示正规或其他
     */
    private boolean isTrialOrMassTrial(String lhType) {
        TrialStatusEnum status = TrialStatusEnum.getByCode(lhType);
        return TrialStatusEnum.TRIAL == status || TrialStatusEnum.MASS_TRIAL == status;
    }
    /**
     * 判断量试充抵正规开关是否开启。
     * <p>读取硫化参数 SYS0312001 的值，默认为关闭（"0"），不再额外插入量试→正规充抵记录。
     * 参数值为 "1" 时开启，与存量行为一致。</p>
     *
     * @param factoryCode 分厂编码
     * @return true-开关开启（执行充抵），false-开关关闭（跳过充抵）
     */
    private boolean isMassTrialToFormalEnabled(String factoryCode) {
        try {
            LhParams paramResult = FeignTokenHelper.callWithToken(() ->
                    lhMesSyncRemoteService.selectLhParamsByCode(
                            LhScheduleParamConstant.ENABLE_MASS_TRIAL_TO_FORMAL, factoryCode));
            if (paramResult == null || StringUtils.isBlank(paramResult.getParamValue())) {
                // 未配置时默认关闭，与存量行为一致
                return false;
            }
            boolean enabled = "1".equals(paramResult.getParamValue().trim());
            log.info("量试充抵正规开关：factoryCode={}, paramValue={}, enabled={}",
                    factoryCode, paramResult.getParamValue(), enabled);
            return enabled;
        } catch (Exception e) {
            log.error("量试充抵正规开关：读取硫化参数异常，factoryCode={}，默认关闭", factoryCode, e);
            return false;
        }
    }

    /**
     * 构造量试→正规充抵记录。
     * <p>业务规则：若 SKU 是量试(T)且同物料存在正规计划(productStatus=S)，
     * 则在日完成量表额外插入一条正规(S)记录，完成量取 MES 原始回传值（未经试制/量试规则调整），
     * 用于让量试合格品充抵正规订单需求。</p>
     * <p>维度规则：所有比对、汇总、生成均以【完成日期+物料编码+示方类型】为维度。
     * 同一物料不同完成日期的量试数据，会分别生成对应日期的正规充抵记录。</p>
     * <p>规则细节：
     * <ul>
     *   <li>仅处理量试(T)数据，试制(X)和正规(S)不参与</li>
     *   <li>新增记录的完成量 = MES 原始回传值（不经试制/量试规则调整）</li>
     *   <li>新增记录的 lhType = S（正规），后续 handleTrialFinishQtyRule 会跳过</li>
     *   <li>新增记录的 lhNo = 量试记录的 lhNo</li>
     *   <li>新增记录的 mesMaterialCode = 量试记录的 mesMaterialCode</li>
     *   <li>同一【完成日期+物料编码】下多条量试记录时，按该维度汇总只生成一条正规记录（完成量=sum）</li>
     *   <li>不同完成日期的同物料量试数据，分别生成对应日期的正规记录</li>
     *   <li>仅当 MES 回传量试数据且 APS 存在该物料的正规计划时才新增</li>
     * </ul>
     * </p>
     *
     * @param factoryCode 工厂编码
     * @param syncList MES 原始同步数据列表（未经试制/量试规则调整）
     * @return 需要新增的正规充抵记录列表（lhType=S，完成量为 MES 原始值）
     */
    private List<LhDayFinishQty> buildMassTrialToFormalRecords(String factoryCode,
                                                               List<LhDayFinishQty> syncList) {
        if (CollectionUtils.isEmpty(syncList)) {
            return Collections.emptyList();
        }

        // Step1: 构建"存在正规(S)记录"的【完成日期+物料编码】集合
        // 判断条件不依赖外部表，直接从 MES 同步数据内部判断同一完成日期+同一SKU是否同时存在量试(T)和正规(S)
        Set<String> formalDimKeys = new HashSet<>();
        for (LhDayFinishQty item : syncList) {
            if (TrialStatusEnum.FORMAL.getCode().equals(item.getLhType())
                    && item.getFinishDate() != null
                    && StringUtils.isNotBlank(item.getMaterialCode())) {
                String dimKey = DateUtil.formatDate(item.getFinishDate()) + "|" + item.getMaterialCode();
                formalDimKeys.add(dimKey);
            }
        }
        if (formalDimKeys.isEmpty()) {
            return Collections.emptyList();
        }

        // Step2: 按【完成日期+物料编码】维度汇总量试(T)数据（使用MES原始完成量，不经试制/量试规则调整）
        // 同一维度下的多条量试记录汇总完成量，并保留第一条作为模板
        Map<String, Integer> dateMaterialFinishMap = new HashMap<>();
        Map<String, LhDayFinishQty> dateMaterialTemplateMap = new HashMap<>();
        for (LhDayFinishQty item : syncList) {
            if (!TrialStatusEnum.MASS_TRIAL.getCode().equals(item.getLhType())) {
                continue;
            }
            if (item.getDayFinishQty() == null
                    || item.getDayFinishQty().compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            if (item.getFinishDate() == null) {
                continue;
            }
            String materialCode = item.getMaterialCode();
            if (StringUtils.isBlank(materialCode)) {
                continue;
            }
            // 维度key：完成日期 + "|" + 物料编码
            String dimKey = DateUtil.formatDate(item.getFinishDate()) + "|" + materialCode;
            dateMaterialFinishMap.merge(dimKey, item.getDayFinishQty().intValue(), Integer::sum);
            // 保留第一条作为模板（同一【完成日期+物料编码】的多条量试记录 lhNo/mesMaterialCode 一致）
            dateMaterialTemplateMap.putIfAbsent(dimKey, item);
        }
        if (dateMaterialFinishMap.isEmpty()) {
            return Collections.emptyList();
        }

        log.info("量试充抵正规：factoryCode={}, 量试维度数={}, 存在正规(S)的维度数={}",
                factoryCode, dateMaterialFinishMap.size(), formalDimKeys.size());

        // Step3: 仅当同一【完成日期+物料编码】同时有量试(T)和正规(S)时，才生成正规充抵记录
        List<LhDayFinishQty> extraList = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : dateMaterialFinishMap.entrySet()) {
            String dimKey = entry.getKey();
            Integer massTrialFinish = entry.getValue();
            if (massTrialFinish == null || massTrialFinish <= 0) {
                continue;
            }
            // 同一【完成日期+物料编码】不存在正规(S)记录，跳过
            if (!formalDimKeys.contains(dimKey)) {
                continue;
            }
            // 从维度key解析出完成日期和物料编码
            String[] parts = dimKey.split("\\|", 2);
            String materialCode = parts[1];
            LhDayFinishQty template = dateMaterialTemplateMap.get(dimKey);
            if (template == null) {
                continue;
            }

            // 以量试记录为模板复制字段，然后改写 lhType 和完成量
            LhDayFinishQty formalRecord = new LhDayFinishQty();
            BeanUtils.copyProperties(template, formalRecord);
            // 标志为正规
            formalRecord.setLhType(TrialStatusEnum.FORMAL.getCode());
            // 完成量取 MES 原始汇总值（不经试制/量试规则调整）
            formalRecord.setDayFinishQty(BigDecimal.valueOf(massTrialFinish));
            // lhNo、mesMaterialCode、finishDate 已通过 copyProperties 从量试记录复制
            // 清除主键，作为新增记录
            formalRecord.setId(null);

            extraList.add(formalRecord);
            log.info("量试充抵正规：构造正规充抵记录，factoryCode={}, materialCode={}, "
                            + "finishDate={}, lhType=S, 完成量(MES原始)={}, lhNo={}",
                    factoryCode, materialCode,
                    template.getFinishDate(), massTrialFinish, template.getLhNo());
        }
        log.info("量试充抵正规：构造完成，factoryCode={}, 量试维度数={}, 新增正规记录数={}",
                factoryCode, dateMaterialFinishMap.size(), extraList.size());
        return extraList;
    }

    /**
     * 查询APS表中窗口日期范围内的旧数据，按芯片编码汇总完成量
     * 用于芯片库存差值更新：必须在逻辑删除之前执行，否则旧数据已被删除无法查询
     *
     * @param factoryCode 分厂编码
     * @param minDateStr  窗口起始日期（yyyy-MM-dd）
     * @param maxDateStr  窗口结束日期（yyyy-MM-dd）
     * @return 芯片编码 -> 旧完成量汇总值
     */
    private Map<String, Integer> queryOldChipFinishQty(String factoryCode, String minDateStr, String maxDateStr) {
        Map<String, Integer> oldChipFinishQtyMap = new HashMap<>();
        try {
            List<LhDayFinishQty> oldList = FeignTokenHelper.callWithToken(() ->
                    lhMesSyncRemoteService.selectDayFinishQtyByDateRange(factoryCode, minDateStr, maxDateStr));
            if (CollectionUtils.isEmpty(oldList)) {
                log.info("芯片库存差值更新：窗口范围旧数据为空，factoryCode={}, dateRange={}~{}", factoryCode, minDateStr, maxDateStr);
                return oldChipFinishQtyMap;
            }
            log.info("芯片库存差值更新：窗口范围旧数据条数={}，factoryCode={}, dateRange={}~{}", oldList.size(), factoryCode, minDateStr, maxDateStr);

            // 查询芯片编码配置，用于过滤匹配芯片编码的旧数据
            LhParams paramResult = FeignTokenHelper.callWithToken(() ->
                    lhMesSyncRemoteService.selectLhParamsByCode(LhScheduleParamConstant.CHIP_CODE_STOCK_UPDATE, factoryCode));
            if (paramResult == null || StringUtils.isBlank(paramResult.getParamValue())) {
                log.warn("芯片库存差值更新：硫化参数CHIP_CODE_STOCK_UPDATE未配置，factoryCode={}", factoryCode);
                return oldChipFinishQtyMap;
            }
            Set<String> chipCodeSet = Arrays.stream(paramResult.getParamValue().split(","))
                    .map(String::trim)
                    .filter(StringUtils::isNotBlank)
                    .collect(Collectors.toSet());

            // 按芯片编码汇总旧数据完成量
            oldChipFinishQtyMap = oldList.stream()
                    .filter(item -> chipCodeSet.contains(item.getMaterialCode()))
                    .filter(item -> item.getDayFinishQty() != null)
                    .collect(Collectors.groupingBy(
                            LhDayFinishQty::getMaterialCode,
                            Collectors.summingInt(item -> item.getDayFinishQty().intValue())
                    ));
            log.info("芯片库存差值更新：旧数据汇总结果={}，factoryCode={}", oldChipFinishQtyMap, factoryCode);
        } catch (Exception e) {
            log.error("芯片库存差值更新：查询旧数据异常，factoryCode={}, dateRange={}~{}", factoryCode, minDateStr, maxDateStr, e);
        }
        return oldChipFinishQtyMap;
    }

    /**
     * 根据参数配置CHIP_CODE_STOCK_UPDATE里的芯片编码，过滤物料编码对应的日完成量数据差值更新芯片库存
     * 采用差值更新模式：FINISH_QTY += 新值 - 旧值
     *   新值 = 本次MES返回的窗口范围内数据按芯片编码汇总
     *   旧值 = 逻辑删除前查询的APS表窗口范围内数据按芯片编码汇总
     *   差值 = 新值 - 旧值，差值为0时跳过更新
     * 避免多日滚动数据同步时重复累加芯片库存完成量
     *
     * @param factoryCode          分厂编码
     * @param syncList             MES返回的日完成量列表
     * @param oldChipFinishQtyMap  逻辑删除前查询的旧数据汇总（芯片编码 -> 旧完成量汇总值）
     * @param dataVersion          本次同步的数据版本号
     */
    private void updateChipStockFinishQty(String factoryCode, List<LhDayFinishQty> syncList,
                                          Map<String, Integer> oldChipFinishQtyMap, String dataVersion) {
        log.info("【芯片库存差值更新】开始处理，factoryCode={}, syncList.size={}", factoryCode, syncList.size());

        LhParams paramResult;
        try {
            paramResult = FeignTokenHelper.callWithToken(() ->
                    lhMesSyncRemoteService.selectLhParamsByCode(LhScheduleParamConstant.CHIP_CODE_STOCK_UPDATE, factoryCode));
        } catch (Exception e) {
            log.error("【芯片库存差值更新】查询硫化参数配置异常，factoryCode={}", factoryCode, e);
            return;
        }
        if (paramResult == null || StringUtils.isBlank(paramResult.getParamValue())) {
            log.warn("【芯片库存差值更新】硫化参数CHIP_CODE_STOCK_UPDATE未配置或值为空，factoryCode={}", factoryCode);
            return;
        }
        log.info("【芯片库存差值更新】硫化参数CHIP_CODE_STOCK_UPDATE配置值：factoryCode={}, paramValue={}", factoryCode, paramResult.getParamValue());

        Set<String> chipCodeSet = Arrays.stream(paramResult.getParamValue().split(","))
                .map(String::trim)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toSet());
        if (CollectionUtils.isEmpty(chipCodeSet)) {
            log.warn("【芯片库存差值更新】解析芯片编码集合为空，factoryCode={}", factoryCode);
            return;
        }
        log.info("【芯片库存差值更新】芯片编码集合：{}", chipCodeSet);

        List<LhDayFinishQty> chipDataList = syncList.stream()
                .filter(item -> chipCodeSet.contains(item.getMaterialCode()))
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(chipDataList)) {
            log.warn("【芯片库存差值更新】过滤后芯片数据为空，无匹配的物料编码！chipCodeSet={}", chipCodeSet);
            return;
        }
        log.info("【芯片库存差值更新】过滤后芯片数据条数：{}", chipDataList.size());

        // 按芯片编码汇总新数据完成量
        Map<String, Integer> newChipFinishQtyMap = chipDataList.stream()
                .filter(item -> item.getDayFinishQty() != null)
                .collect(Collectors.groupingBy(
                        LhDayFinishQty::getMaterialCode,
                        Collectors.summingInt(item -> item.getDayFinishQty().intValue())
                ));
        log.info("【芯片库存差值更新】新数据汇总结果：{}", newChipFinishQtyMap);

        // 计算差值：delta = 新值 - 旧值，差值不为0才需要更新
        List<LhChipStock> chipStockList = new ArrayList<>();
        for (String chipCode : chipCodeSet) {
            int newSum = newChipFinishQtyMap.getOrDefault(chipCode, 0);
            int oldSum = oldChipFinishQtyMap.getOrDefault(chipCode, 0);
            int delta = newSum - oldSum;
            if (delta != 0) {
                LhChipStock chipStock = new LhChipStock();
                chipStock.setFactoryCode(factoryCode);
                chipStock.setChipCode(chipCode);
                chipStock.setFinishQty(delta);
                chipStock.setDataVersion(dataVersion);
                chipStockList.add(chipStock);
                log.info("芯片库存差值更新：chipCode={}, newSum={}, oldSum={}, delta={}", chipCode, newSum, oldSum, delta);
            } else {
                log.info("芯片库存差值更新：chipCode={}, delta=0, 跳过更新（newSum={}, oldSum={}）", chipCode, newSum, oldSum);
            }
        }

        if (CollectionUtils.isEmpty(chipStockList)) {
            log.info("芯片库存差值更新：所有芯片编码差值为0，无需更新，factoryCode={}", factoryCode);
            return;
        }

        try {
            FeignTokenHelper.runWithToken(() -> {
                log.info("芯片库存差值更新：开始同步，factoryCode={}, 待处理数量={}", factoryCode, chipStockList.size());
                lhChipStockRemoteService.upsertFinishQty(factoryCode, chipStockList);
                log.info("芯片库存差值更新：同步完成，factoryCode={}, 处理数量={}", factoryCode, chipStockList.size());
            });
        } catch (Exception e) {
            log.error("芯片库存差值更新异常, factoryCode={}", factoryCode, e);
        }
    }

    /**
     * 模具交替计划下发到MES
     * 1. 清理中间表中同工单号的旧数据，避免脏数据残留
     * 2. 写入MES中间表MOLD_ALTER_PLAN（建在MES分库）
     * 3. 发送MQ通知MES来获取数据
     * 分批处理避免SQL Server参数上限2100的问题
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

        // 从数据中获取factoryCode，分公司编码与分厂编码保持一致
        String factoryCode = moldAlterPlanList.get(0).getFactoryCode();
        String companyCode = factoryCode;

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
            // 分公司编码与分厂编码保持一致
            issue.setCompanyCode(plan.getFactoryCode());
            issueList.add(issue);
        }

        try {
            // 先清理中间表中同工单号的旧数据，避免脏数据残留导致MES消费异常
            // 分批删除，避免SQL Server IN子句参数上限2100的问题
            if (CollectionUtils.isNotEmpty(orderNos) && StringUtils.isNotBlank(factoryCode)) {
                for (int i = 0; i < orderNos.size(); i += BATCH_SIZE) {
                    List<String> batchOrderNos = orderNos.subList(i, Math.min(i + BATCH_SIZE, orderNos.size()));
                    int deleted = moldAlterPlanIssueMapper.deleteByOrderNosAndFactoryCode(batchOrderNos, factoryCode);
                    if (deleted > 0) {
                        log.info("清理模具交替计划中间表旧数据, 分厂: {}, 工单号批次: {}/{}, 删除数量: {}", factoryCode, (i / BATCH_SIZE) + 1, (orderNos.size() + BATCH_SIZE - 1) / BATCH_SIZE, deleted);
                    }
                }
            }
            // 分批插入，避免SQL Server参数上限2100的问题
            for (int i = 0; i < issueList.size(); i += BATCH_SIZE) {
                List<MoldAlterPlanIssue> batch = issueList.subList(i, Math.min(i + BATCH_SIZE, issueList.size()));
                moldAlterPlanIssueMapper.insertMoldAlterPlanList(batch);
            }
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
     * 调用合并接口：一次Feign调用完成"插入或更新完成状态 + 回填模具交替计划表"，在同一事务中
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

            // MES回报的完成状态可能为中文（未完成/已完成），统一转换为数值编码（0/1）
            entity.setFinishStatus(MouldFinishStatusEnum.convertToCode(entity.getFinishStatus()));

            if (entity.getIsDelete() == null) {
                entity.setIsDelete(0);
            }

            insertOrUpdateList.add(entity);
        }

        if (CollectionUtils.isNotEmpty(insertOrUpdateList)) {
            FeignTokenHelper.runWithToken(() -> {
                // 合并接口：一次调用完成"插入或更新 + 回填模具交替计划完成状态"，在同一事务中
                List<List<LhMoldAlterPlanFinish>> splitList = ScmListUtils.getSplitList(insertOrUpdateList, 1000);
                for (List<LhMoldAlterPlanFinish> subList : splitList) {
                    lhMesSyncRemoteService.saveOrUpdateMoldAlterPlanFinishAndWriteBack(subList);
                }
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
     * 同步指定物理日的胎面库存快照。
     *
     * <p>MES_TM_STOCK 仅提供库存日期、物料编码和可用库存，工厂归属由请求参数确定；
     * 同一物料编码的可用库存汇总后替换 APS 中指定工厂和日期的完整快照。</p>
     *
     * @param syncDataLogs 同步参数，queryParams.stockDate 必传
     * @return 同步结果
     */
    @Override
    public AjaxResult syncTreadStock(AuxReqSyncDataLogs syncDataLogs) {
        if (syncDataLogs == null || syncDataLogs.getQueryParams() == null
                || syncDataLogs.getQueryParams().get("stockDate") == null) {
            return AjaxResult.error(I18nUtil.getMessage("ui.itf.mes.stockArgumentsInvalid"));
        }
        String factoryCode = StringUtils.defaultIfBlank(syncDataLogs.getFactoryCode(),
                FactoryConstant.DEFAULT_FACTORY_CODE);
        Date stockDate;
        try {
            Object stockDateValue = syncDataLogs.getQueryParams().get("stockDate");
            stockDate = stockDateValue instanceof Date ? (Date) stockDateValue
                    : DateUtil.parseDate(String.valueOf(stockDateValue));
        } catch (Exception exception) {
            return AjaxResult.error(I18nUtil.getMessage("ui.itf.mes.stockArgumentsInvalid"));
        }
        if (stockDate == null) {
            return AjaxResult.error(I18nUtil.getMessage("ui.itf.mes.stockArgumentsInvalid"));
        }
        Date normalizedStockDate = DateUtil.beginOfDay(stockDate);
        syncDataLogs.setFactoryCode(factoryCode);
        syncDataLogs.getQueryParams().put("stockDate", normalizedStockDate);
        List<TmMesStock> sourceList;
        DynamicDataSourceContextHolder.push(DataSource.MES);
        try {
            sourceList = this.mesItfMapper.selectTreadStockList(syncDataLogs);
        } finally {
            DynamicDataSourceContextHolder.poll();
        }
        Map<String, BigDecimal> stockQtyMap = CollectionUtils.emptyIfNull(sourceList).stream()
                .filter(source -> StringUtils.isNotBlank(source.getMaterialCode()))
                .collect(Collectors.toMap(source -> StringUtils.trim(source.getMaterialCode()),
                        source -> BigDecimalUtils.valueOf(source.getAvailableStock()), BigDecimal::add,
                        LinkedHashMap::new));
        List<TmStock> stockList = stockQtyMap.entrySet().stream().map(entry -> {
            TmStock tmStock = new TmStock();
            tmStock.setTreadCode(entry.getKey());
            tmStock.setStockQty(entry.getValue());
            tmStock.setBadQty(BigDecimal.ZERO);
            tmStock.setAdjustQty(BigDecimal.ZERO);
            return tmStock;
        }).collect(Collectors.toList());
        try {
            AjaxResult result = FeignTokenHelper.callWithToken(() -> this.tmMesSyncRemoteService.replaceStock(
                    factoryCode, DateUtil.formatDate(normalizedStockDate), "MES", stockList));
            if (result == null || !Objects.equals(AJAX_SUCCESS_CODE, result.get(AjaxResult.CODE_TAG))) {
                return AjaxResult.error(I18nUtil.getMessage("ui.itf.mes.stockRemoteFailed"));
            }
        } catch (Exception exception) {
            log.error("胎面库存同步失败，factoryCode={}，stockDate={}，数量={}", factoryCode,
                    DateUtil.formatDate(normalizedStockDate), stockList.size(), exception);
            return AjaxResult.error(I18nUtil.getMessage("ui.itf.mes.stockRemoteFailed"));
        }
        return AjaxResult.success(stockList.size());
    }

    /**
     * 从MES读取指定物理日的胎面库存，并替换自动滚动班次快照。
     *
     * <p>MES无数据时仍调用TM清空对应快照，防止自动滚动继续使用旧库存。
     * 动态数据源上下文始终在finally中恢复，避免查询异常污染后续线程调用。</p>
     *
     * @param request 工厂、物理库存日和班序
    // 接收Feign返回值并校验，避免服务端异常被全局异常处理器吞掉返回HTTP 200+AjaxResult.error时，itf端误判为成功
    AjaxResult saveResult = FeignTokenHelper.callWithToken(() ->
    tmMesSyncRemoteService.logicDeleteAndSaveScheFinishQty(factoryCode, scheduleDateStr, "MES", insertList));
    if (AJAX_SUCCESS_CODE != (Integer) saveResult.get(AjaxResult.CODE_TAG)) {
    log.error("胎面排程完成量同步：同步失败，factoryCode={}, 返回code={}, 返回消息={}",
    factoryCode, saveResult.get(AjaxResult.CODE_TAG), saveResult.get(AjaxResult.MSG_TAG));
    return AjaxResult.error("胎面排程完成量同步失败：" + saveResult.get(AjaxResult.MSG_TAG));
    }
     * @return 同步数量
     * @throws ServiceException 参数非法或远程保存失败时抛出
     */
    @Override
    public AjaxResult syncTreadShiftStock(MesShiftStockSyncRequest request) {
        if (request == null || request.getStockDate() == null || request.getShiftOrder() == null
                || request.getShiftOrder() < 1 || request.getShiftOrder() > 6) {
            throw new ServiceException(I18nUtil.getMessage("ui.itf.mes.shiftStockArgumentsInvalid"));
        }
        request.setFactoryCode(StringUtils.defaultIfBlank(request.getFactoryCode(),
                FactoryConstant.DEFAULT_FACTORY_CODE));
        request.setCompanyCode(StringUtils.defaultIfBlank(request.getCompanyCode(), request.getFactoryCode()));
        request.setStockDate(DateUtil.beginOfDay(request.getStockDate()));
        List<TmMesStock> sourceList;
        DynamicDataSourceContextHolder.push(DataSource.MES);
        try {
            sourceList = this.mesItfMapper.selectTreadShiftStockList(request);
        } finally {
            DynamicDataSourceContextHolder.poll();
        }
        Map<String, BigDecimal> stockQtyMap = CollectionUtils.emptyIfNull(sourceList).stream()
                .filter(source -> StringUtils.isNotBlank(source.getMaterialCode()))
                .collect(Collectors.toMap(source -> StringUtils.trim(source.getMaterialCode()),
                        source -> BigDecimalUtils.valueOf(source.getAvailableStock()), BigDecimal::add,
                        LinkedHashMap::new));
        List<TmShiftStock> stockList = stockQtyMap.entrySet().stream().map(entry -> {
            TmShiftStock target = new TmShiftStock();
            target.setFactoryCode(request.getFactoryCode());
            target.setStockDate(request.getStockDate());
            target.setShiftOrder(request.getShiftOrder());
            target.setTreadCode(entry.getKey());
            target.setStockQty(entry.getValue());
            target.setBadQty(BigDecimal.ZERO);
            target.setAdjustQty(BigDecimal.ZERO);
            return target;
        }).collect(Collectors.toList());
        AjaxResult saveResult = FeignTokenHelper.callWithToken(() ->
                this.tmMesSyncRemoteService.replaceShiftStock(request.getFactoryCode(),
                        DateUtil.formatDate(request.getStockDate()), request.getShiftOrder(), "MES", stockList));
        if (saveResult == null || !Objects.equals(AJAX_SUCCESS_CODE,
                saveResult.get(AjaxResult.CODE_TAG))) {
            throw new ServiceException(I18nUtil.getMessage("ui.itf.mes.shiftStockRemoteFailed"));
        }
        return AjaxResult.success(stockList.size());
    }

    /**
     * 同步胎面排程完成量
     * 从MES中间表MES_TM_CLASS_FINISH_QTY查询当天最新版本数据，
     * 逻辑删除APS旧数据并插入新数据，最后回写胎面排程结果表各班次完成量
     *
     * @param syncDataLogs 同步参数
     * @return 结果
     */
    @Override
    public AjaxResult syncTmClassShiftFinishQty(AuxReqSyncDataLogs syncDataLogs) {
        DynamicDataSourceContextHolder.push(DataSource.MES);
        List<TmScheFinishQty> syncList = mesItfMapper.selectTmClassShiftFinishQtyList(syncDataLogs);
        DynamicDataSourceContextHolder.poll();

        if (CollectionUtils.isEmpty(syncList)) {
            log.warn("胎面排程完成量同步：MES中间表查询结果为空，factoryCode={}", syncDataLogs.getFactoryCode());
            return AjaxResult.success("MES中间表无数据可同步");
        }

        List<TmScheFinishQty> insertList = new ArrayList<>();
        for (TmScheFinishQty item : syncList) {
            TmScheFinishQty entity = new TmScheFinishQty();
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
            Date scheduleDate = insertList.stream().map(TmScheFinishQty::getScheduleDate).filter(Objects::nonNull).findFirst().orElse(DateUtils.getNowDate());
            String scheduleDateStr = DateUtil.formatDate(scheduleDate);
            log.info("胎面排程完成量同步：开始同步，factoryCode={}, scheduleDate={}, 待插入数量={}", factoryCode, scheduleDateStr, insertList.size());

            // 接收Feign返回值并校验，避免服务端异常被全局异常处理器吞掉返回HTTP 200+AjaxResult.error时，itf端误判为成功
            AjaxResult saveResult = FeignTokenHelper.callWithToken(() ->
                    tmMesSyncRemoteService.logicDeleteAndSaveScheFinishQty(factoryCode, scheduleDateStr, "MES", insertList));
            if (AJAX_SUCCESS_CODE != (Integer) saveResult.get(AjaxResult.CODE_TAG)) {
                log.error("胎面排程完成量同步：同步失败，factoryCode={}, 返回code={}, 返回消息={}",
                        factoryCode, saveResult.get(AjaxResult.CODE_TAG), saveResult.get(AjaxResult.MSG_TAG));
                return AjaxResult.error("胎面排程完成量同步失败：" + saveResult.get(AjaxResult.MSG_TAG));
            }

            log.info("胎面排程完成量同步：同步完成，factoryCode={}, 插入数量={}", factoryCode, insertList.size());
        } catch (Exception e) {
            log.error("胎面排程完成量同步：Feign调用异常，factoryCode={}, 待插入数量={}", syncDataLogs.getFactoryCode(), insertList.size(), e);
            return AjaxResult.error("胎面排程完成量同步失败：" + e.getMessage());
        }

        try {
            FeignTokenHelper.runWithToken(() -> {
                tmMesSyncRemoteService.writeBackScheduleResultFinishQty(insertList);
            });
        } catch (Exception e) {
            log.error("【胎面排程完成量回写】回写胎面排程结果表完成量异常", e);
        }
        return AjaxResult.success();
    }

    /**
     * 同步胎面排程日完成量
     * 从MES中间表MES_TM_DAY_FINISH_TOTL查询前一天的数据，
     * 逻辑删除APS旧数据并插入新数据
     *
     * @param syncDataLogs 同步参数
     * @return 结果
     */
    @Override
    public AjaxResult syncTmScheDayFinishQty(AuxReqSyncDataLogs syncDataLogs) {
        DynamicDataSourceContextHolder.push(DataSource.MES);
        Date nowDate = DateUtils.truncate(DateUtils.getNowDate(), Calendar.DATE);
        Date lastDate = DateUtils.addDays(nowDate, -1);
        syncDataLogs.setQueryParams(new HashMap<>());
        syncDataLogs.getQueryParams().put("scheduleDate", lastDate);
        List<TmDayFinishQty> syncList = mesItfMapper.selectTmScheDayFinishQtyList(syncDataLogs);
        DynamicDataSourceContextHolder.poll();

        if (CollectionUtils.isEmpty(syncList)) {
            log.warn("胎面排程日完成量同步：MES中间表查询结果为空，factoryCode={}", syncDataLogs.getFactoryCode());
            return AjaxResult.success("MES中间表无数据可同步");
        }

        Map<String, TmDayFinishQty> groupMap = syncList.stream()
                .collect(Collectors.toMap(
                        item -> item.getFactoryCode() + "|" + DateUtil.formatDate(item.getScheduleDate()) + "|" + item.getTreadCode(),
                        Function.identity(),
                        (v1, v2) -> v1
                ));
        syncList = new ArrayList<>(groupMap.values());

        List<TmDayFinishQty> insertList = new ArrayList<>();
        for (TmDayFinishQty item : syncList) {
            TmDayFinishQty entity = new TmDayFinishQty();
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
            Date scheduleDate = insertList.stream().map(TmDayFinishQty::getScheduleDate).filter(Objects::nonNull).findFirst().orElse(DateUtils.getNowDate());
            String scheduleDateStr = DateUtil.formatDate(scheduleDate);
            log.info("胎面排程日完成量同步：开始同步，factoryCode={}, scheduleDate={}, 待插入数量={}", factoryCode, scheduleDateStr, insertList.size());

            FeignTokenHelper.runWithToken(() -> {
                tmMesSyncRemoteService.logicDeleteAndSaveDayFinishQty(factoryCode, scheduleDateStr, "MES", insertList);
            });

            log.info("胎面排程日完成量同步：同步完成，factoryCode={}, 插入数量={}", factoryCode, insertList.size());
        } catch (Exception e) {
            log.error("胎面排程日完成量同步：Feign调用异常，factoryCode={}, 待插入数量={}", syncDataLogs.getFactoryCode(), insertList.size(), e);
            return AjaxResult.error("胎面排程日完成量同步失败：" + e.getMessage());
        }
        return AjaxResult.success();
    }

    /**
     * 胎面排程结果下发到MES
     * 业务规则（与胎圈一致）：
     * 1. D日（今天）：更新中班数据，夜班早班已过不下发
     * 2. D+1日（明天）：更新夜早中3班数据
     * 3. D+2日（后天）：先删后插夜早2班数据，中班尚未排产不下发
     *
     * @param tmScheduleResultIssueList 胎面排程结果下发列表（已按3天拆分）
     * @return 结果
     */
    @Override
    public AjaxResult issueTmScheduleResult(List<TmScheduleResultIssue> tmScheduleResultIssueList) {
        // 分公司编码与分厂编码保持一致
        String factoryCode = FactoryConstant.DEFAULT_FACTORY_CODE;
        String companyCode = factoryCode;
        return tmScheduleResultIssueService.issueTmScheduleResult(tmScheduleResultIssueList, factoryCode, companyCode);
    }

    /**
     * 钢丝圈排程结果下发到MES
     * 业务规则（与胎圈一致）：
     * 1. D日（今天）：更新中班数据（钢丝圈1班→MES中班），夜班早班已过不下发
     * 2. D+1日（明天）：更新夜早中3班数据（钢丝圈2/3/4班→MES夜/早/中班）
     * 3. D+2日（后天）：先删后插夜早2班数据（钢丝圈5/6班→MES夜/早班），中班尚未排产不下发
     * TQ_CLASS1~6_PLAN 全量传递到每条记录
     *
     * @param gsqScheduleResultIssueList 钢丝圈排程结果下发列表（已按3天拆分）
     * @return 下发结果（data 字段携带 mesStatus：IS_RELEASE/FAILURE_RELEASE/TIMEOUT_FAILURE）
     */
//    @Override
//    public AjaxResult issueGsqScheduleResult(List<GsqScheduleResultIssue> gsqScheduleResultIssueList) {
//        // 分公司编码与分厂编码保持一致
//        String factoryCode = FactoryConstant.DEFAULT_FACTORY_CODE;
//        String companyCode = factoryCode;
//        return gsqScheduleResultIssueService.issueGsqScheduleResult(gsqScheduleResultIssueList, factoryCode, companyCode);
//    }

    /**
     * @deprecated 原逻辑：APS从MES中间表抓取已完成的精度数据，回填实际执行日期到T_LH_PRECISION_PLAN并生成下一次精度计划。
     *             现逻辑改为MES全权决定计划与完成时间，APS只做同步+分发，不再回填/生成。
     *             此方法保留备份以防后续改回原逻辑。新逻辑请使用 {@link #syncAndDispatchDevMaintenancePlan}
     */
    @Deprecated
    @Override
    public AjaxResult syncLhPrecisionPlanActual(AuxReqSyncDataLogs syncDataLogs) {
        // 先查询MES中间表硫化精度类型的最大版本号，只同步最新版本的数据
        String actualFactoryCode = syncDataLogs != null ? syncDataLogs.getFactoryCode() : null;
        DynamicDataSourceContextHolder.push(DataSource.MES);
        String maxVersion = mesItfMapper.selectMaxDataVersionFromMes(actualFactoryCode, "硫化精度");
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

    /**
     * @deprecated 原逻辑：APS下发硫化精度计划到MES，与"MES决定计划时间"语义冲突。
     *             现逻辑改为MES全权决定计划与完成时间，APS不再下发精度计划。
     *             此方法保留备份以防后续改回原逻辑。
     */
    @Deprecated
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

    /**
     * @deprecated 原逻辑：同步MES数据并回填实际日期+生成下一次精度计划。
     *             现逻辑改为MES全权决定计划与完成时间，APS只做同步+分发，不再回填/生成。
     *             此方法保留备份以防后续改回原逻辑。新逻辑请使用 {@link #syncAndDispatchDevMaintenancePlan}
     */
    @Deprecated
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

    /**
     * @deprecated 原逻辑：按版本前缀同步MES数据并回填实际日期+生成下一次精度计划。
     *             现逻辑改为MES全权决定计划与完成时间，APS只做同步+分发，不再回填/生成。
     *             此方法保留备份以防后续改回原逻辑。新逻辑请使用 {@link #syncAndDispatchDevMaintenancePlan}
     */
    @Deprecated
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

    /**
     * @deprecated 原逻辑：按版本前缀同步MES数据(不限最大版本号)并回填实际日期+生成下一次精度计划。
     *             现逻辑改为MES全权决定计划与完成时间，APS只做同步+分发，不再回填/生成。
     *             此方法保留备份以防后续改回原逻辑。新逻辑请使用 {@link #syncAndDispatchDevMaintenancePlan}
     */
    @Deprecated
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

    /**
     * @deprecated 原逻辑：按计划时间年份同步回填实际日期+生成下一年度精度计划。
     *             现逻辑改为MES全权决定计划与完成时间，APS只做同步+分发，不再回填/生成。
     *             此方法保留备份以防后续改回原逻辑。新逻辑请使用 {@link #syncAndDispatchDevMaintenancePlan}
     */
    @Deprecated
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

    /**
     * 同步设备计划停机（MES→APS）
     * 采用更新删除标识模式，而不是先删后插
     * 支持全量/增量同步：
     * - 首次全量同步：不传版本号时查询MES中间表最大版本号后同步
     * - 后续增量同步：按版本号增量拉取
     * - 删除数据同步：DEL_FLAG=1的记录映射为APS表的IS_DELETE=1
     * - 停机类型=06（临时性故障）特殊处理：MES分步写入开始时间和结束时间，按唯一键更新
     *
     * @param syncDataLogs 同步参数
     * @return 结果
     */
    @Override
    public AjaxResult syncDevPlanClose(AuxReqSyncDataLogs syncDataLogs) {
        // 查询MES中间表设备计划停机的最大版本号，只同步最新版本的数据
        String factoryCode = syncDataLogs != null ? syncDataLogs.getFactoryCode() : null;
        DynamicDataSourceContextHolder.push(DataSource.MES);
        String maxVersion = mesItfMapper.selectMaxDataVersionFromDevPlanClose(factoryCode);
        DynamicDataSourceContextHolder.poll();

        if (maxVersion != null && !maxVersion.isEmpty()) {
            if (syncDataLogs == null) {
                syncDataLogs = new AuxReqSyncDataLogs();
            }
            syncDataLogs.setDataVersion(maxVersion);
            log.info("同步设备计划停机，最新版本号={}", maxVersion);
        } else {
            log.info("MES中间表无设备计划停机版本数据，factoryCode={}", factoryCode);
        }

        // 从MES中间表查询数据
        DynamicDataSourceContextHolder.push(DataSource.MES);
        List<DevPlanCloseVo> syncList = mesItfMapper.selectDevPlanCloseList(syncDataLogs);
        DynamicDataSourceContextHolder.poll();

        if (CollectionUtils.isEmpty(syncList)) {
            log.warn("设备计划停机同步：MES中间表查询结果为空，factoryCode={}", factoryCode);
            return AjaxResult.success("MES中间表无数据可同步");
        }

        // 按唯一键去重：FACTORY_CODE + MACHINE_CODE + MACHINE_TYPE + MACHINE_STOP_TYPE + BEGIN_DATE
        Map<String, DevPlanCloseVo> groupMap = syncList.stream()
                .collect(Collectors.toMap(
                        item -> item.getFactoryCode() + "|" + item.getMachineCode() + "|"
                                + item.getMachineType() + "|" + item.getMachineStopType() + "|"
                                + item.getBeginDate().getTime(),
                        Function.identity(),
                        (v1, v2) -> v1
                ));
        syncList = new ArrayList<>(groupMap.values());

        try {
            DynamicDataSourceContextHolder.push(DataSource.APS);

            // 分批处理，每批1000条
            List<List<DevPlanCloseVo>> splitList = ScmListUtils.getSplitList(syncList, 1000);
            List<MdmDevicePlanShut> insertOrUpdateList = null;
            for (List<DevPlanCloseVo> saveList : splitList) {
                // 1. 优先按MES_ID批量查询APS已有数据（用于精准匹配更新）
                List<MdmDevicePlanShut> mesIdQueryList = saveList.stream()
                        .map(DevPlanCloseVo::getId)
                        .filter(Objects::nonNull)
                        .map(id -> {
                            MdmDevicePlanShut shut = new MdmDevicePlanShut();
                            shut.setMesId(id);
                            return shut;
                        })
                        .collect(Collectors.toList());
                Map<Long, MdmDevicePlanShut> existsByMesIdMap = new HashMap<>(16);
                if (CollectionUtils.isNotEmpty(mesIdQueryList)) {
                    List<MdmDevicePlanShut> existsByMesIdList = devicePlanShutEntityMapper.selectByMesIdList(mesIdQueryList);
                    if (CollectionUtils.isNotEmpty(existsByMesIdList)) {
                        existsByMesIdMap = existsByMesIdList.stream()
                                .filter(item -> item.getMesId() != null)
                                .collect(Collectors.toMap(
                                        MdmDevicePlanShut::getMesId,
                                        Function.identity(),
                                        (v1, v2) -> v1
                                ));
                    }
                }

                // 2. 回退方案：按唯一键批量查询APS已有数据（兼容历史无MES_ID的APS数据，同时回填MES_ID）
                List<MdmDevicePlanShut> existsList = devicePlanShutEntityMapper.selectByUniqueKeyList(
                        saveList.stream().map(item -> {
                            MdmDevicePlanShut shut = new MdmDevicePlanShut();
                            shut.setMachineCode(item.getMachineCode());
                            shut.setMachineType(item.getMachineType());
                            shut.setMachineStopType(item.getMachineStopType());
                            shut.setFactoryCode(item.getFactoryCode());
                            shut.setBeginDate(item.getBeginDate());
                            return shut;
                        }).collect(Collectors.toList())
                );

                // 构建已存在数据的Map，key为唯一键
                Map<String, MdmDevicePlanShut> existsMap = new HashMap<>(16);
                if (CollectionUtils.isNotEmpty(existsList)) {
                    existsMap = existsList.stream()
                            .collect(Collectors.toMap(
                                    item -> GenerageMapKeyUtils.createMapKey(
                                            item.getFactoryCode(), item.getMachineCode(),
                                            item.getMachineType(), item.getMachineStopType(),
                                            item.getBeginDate() != null ? String.valueOf(item.getBeginDate().getTime()) : ""),
                                    Function.identity(),
                                    (v1, v2) -> v1
                            ));
                }

                insertOrUpdateList = new ArrayList<>();
                for (DevPlanCloseVo item : saveList) {
                    MdmDevicePlanShut entity = new MdmDevicePlanShut();
                    entity.setFactoryCode(item.getFactoryCode());
                    entity.setMachineCode(item.getMachineCode());
                    entity.setMachineType(item.getMachineType());
                    entity.setMachineStopType(item.getMachineStopType());
                    entity.setBeginDate(item.getBeginDate());
                    entity.setEndDate(item.getEndDate());
                    entity.setRemark(item.getRemark());
                    entity.setDataVersion(item.getDataVersion());
                    entity.setDataSource("0"); // 数据来源：0-MES
                    entity.setCreateBy("MES");
                    entity.setUpdateBy("MES");
                    // 存储MES设备停机计划表ID，用于后续同步按MES_ID精准匹配
                    entity.setMesId(item.getId());

                    // 处理删除标识：MES的DEL_FLAG映射为APS的IS_DELETE
                    if (StringUtils.isNotBlank(item.getDelFlag())) {
                        entity.setIsDelete(Integer.valueOf(item.getDelFlag()));
                    } else {
                        entity.setIsDelete(0);
                    }

                    // 匹配策略：优先按MES_ID匹配（精准），未命中时回退唯一键匹配
                    // （兼容历史无MES_ID数据，同时回填MES_ID），都未命中则插入
                    if (item.getId() != null && existsByMesIdMap.containsKey(item.getId())) {
                        // 按MES_ID命中 → 更新
                        MdmDevicePlanShut existsData = existsByMesIdMap.get(item.getId());
                        entity.setId(existsData.getId());
                    } else {
                        // 回退唯一键匹配
                        String mapKey = GenerageMapKeyUtils.createMapKey(
                                entity.getFactoryCode(), entity.getMachineCode(),
                                entity.getMachineType(), entity.getMachineStopType(),
                                entity.getBeginDate() != null ? String.valueOf(entity.getBeginDate().getTime()) : "");
                        if (existsMap.containsKey(mapKey)) {
                            MdmDevicePlanShut existsData = existsMap.get(mapKey);
                            entity.setId(existsData.getId());
                        }
                    }
                    insertOrUpdateList.add(entity);
                }

                // 批量保存（insertOrUpdate）
                baseDao.saveBatch(insertOrUpdateList);
            }

            log.info("设备计划停机同步完成，同步数量={}", syncList.size());
        } catch (Exception e) {
            log.error("设备计划停机同步失败，factoryCode={}", factoryCode, e);
            return AjaxResult.error("设备计划停机同步失败：" + e.getMessage());
        } finally {
            DynamicDataSourceContextHolder.poll();
        }
        return AjaxResult.success();
    }
}

