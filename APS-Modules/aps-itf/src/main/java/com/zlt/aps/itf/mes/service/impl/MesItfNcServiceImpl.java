package com.zlt.aps.itf.mes.service.impl;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.cloud.commons.lang.StringUtils;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.dynamic.datasource.toolkit.DynamicDataSourceContextHolder;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.constant.FactoryConstant;
import com.zlt.aps.itf.constant.DataSource;
import com.zlt.aps.itf.constant.SysCode;
import com.zlt.aps.itf.mes.enums.ItfSyncKeyEnum;
import com.zlt.aps.itf.mes.mapper.MesNcScheduleResultMapper;
import com.zlt.aps.itf.mes.mapper.NcMesSourceMapper;
import com.zlt.aps.itf.mes.service.IMesItfNcService;
import com.zlt.aps.itf.vo.AuxReqSyncDataLogs;
import com.zlt.aps.itf.vo.MesNcScheduleResult;
import com.zlt.aps.itf.vo.SyncDataLogs;
import com.zlt.aps.nc.api.domain.entity.NcDayFinishQty;
import com.zlt.aps.nc.api.domain.entity.NcScheFinishQty;
import com.zlt.aps.nc.api.domain.entity.NcScheduleResult;
import com.zlt.aps.nc.api.domain.entity.NcStock;
import com.zlt.core.dao.basedao.BaseDao;
import com.zlt.sync.handle.SyncDataHandle;
import com.zlt.sync.povo.SyncParamsVO;
import com.zlt.sync.service.SyncDataLogsService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service("mesItfNcService")
public class MesItfNcServiceImpl implements IMesItfNcService {

    /** 日期格式化器 */
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    /** SQL Server单次请求参数上限2100，每条记录约40个参数，安全批次大小为50 */
    private static final int BATCH_SIZE = 50;
    @Autowired
    private SyncDataHandle syncDataHandle;
    @Autowired
    private SyncDataLogsService syncDataLogsService;

    @Autowired
    private NcMesSourceMapper ncMesSourceMapper;
    @Autowired
    private MesNcScheduleResultMapper mesNcScheduleResultMapper;
    @Autowired
    private BaseDao baseDao;

    @Override
    public AjaxResult syncStock(AuxReqSyncDataLogs request) {
        if (StringUtils.isEmpty(request.getFactoryCode())) {
            request.setFactoryCode(FactoryConstant.DEFAULT_FACTORY_CODE);
        }
        if (StringUtils.isEmpty(request.getCompanyCode())) {
            request.setCompanyCode(FactoryConstant.DEFAULT_FACTORY_CODE);
        }
        List<NcStock> stockList = ncMesSourceMapper.selectStockList(request);
        if (CollectionUtils.isEmpty(stockList)) {
            return AjaxResult.error();
        }
        Date createTime = new Date();
        stockList.stream().forEach(stock -> {
            stock.setCreateBy("MES");
            stock.setCreateTime(createTime);
        });
        try {
            /** 切换APS数据源 start **/
            DynamicDataSourceContextHolder.push(DataSource.APS);
            baseDao.saveBatch(stockList);
        } finally {
            DynamicDataSourceContextHolder.clear();
            /** 切换APS数据源 end **/
        }
        return AjaxResult.success();
    }

    @Override
    public AjaxResult syncShiftFinishQty(AuxReqSyncDataLogs request) {
        if (StringUtils.isEmpty(request.getFactoryCode())) {
            request.setFactoryCode(FactoryConstant.DEFAULT_FACTORY_CODE);
        }
        if (StringUtils.isEmpty(request.getCompanyCode())) {
            request.setCompanyCode(FactoryConstant.DEFAULT_FACTORY_CODE);
        }
        List<NcScheFinishQty> sourceList = ncMesSourceMapper.selectShiftFinishQtyList(request);
        if (CollectionUtils.isEmpty(sourceList)) {
            return AjaxResult.error();
        }
        try {
            /** 切换APS数据源 start **/
            DynamicDataSourceContextHolder.push(DataSource.APS);
            baseDao.saveBatch(sourceList);
        } finally {
            DynamicDataSourceContextHolder.clear();
            /** 切换APS数据源 end **/
        }
        return AjaxResult.success();
    }

    @Override
    public AjaxResult syncDayFinishQty(AuxReqSyncDataLogs request) {
        if (StringUtils.isEmpty(request.getFactoryCode())) {
            request.setFactoryCode(FactoryConstant.DEFAULT_FACTORY_CODE);
        }
        if (StringUtils.isEmpty(request.getCompanyCode())) {
            request.setCompanyCode(FactoryConstant.DEFAULT_FACTORY_CODE);
        }
        List<NcDayFinishQty> sourceList = ncMesSourceMapper.selectDayFinishQtyList(request);
        if (CollectionUtils.isEmpty(sourceList)) {
            return AjaxResult.success(I18nUtil.getMessage("ui.tc.schedule.mes.noSourceData"));
        }
        try {
            /** 切换APS数据源 start **/
            DynamicDataSourceContextHolder.push(DataSource.APS);
            baseDao.saveBatch(sourceList);
        } finally {
            DynamicDataSourceContextHolder.clear();
            /** 切换APS数据源 end **/
        }
        return AjaxResult.success();
    }

    @Override
    public AjaxResult issueNcScheduleResult(List<NcScheduleResult> ncScheduleResultIssueList, String factoryCode,
            String companyCode) {
        if (CollectionUtils.isEmpty(ncScheduleResultIssueList)) {
            return AjaxResult.success();
        }
        String dataVersion;
        try {
            /** 切换APS数据源 start **/
            DynamicDataSourceContextHolder.push(DataSource.MASTER);
            // 获取下发接口版本号
            dataVersion = syncDataHandle.getDataVersion(ItfSyncKeyEnum.PAD_SCHE_FBK.getCode());
        } finally {
            DynamicDataSourceContextHolder.clear();
            /** 切换APS数据源 end **/
        }
        // 获取今天、明天、后天的日期
        LocalDate today = LocalDate.now();

        // 按日期分组处理数据
        List<NcScheduleResult> todayList = this.filterByDate(ncScheduleResultIssueList, today);
        // 转换为MES实体
        List<MesNcScheduleResult> mesList = this.convertToMesList(todayList, dataVersion, companyCode, factoryCode);

        if (CollectionUtils.isEmpty(mesList)) {
            return AjaxResult.success("没有需要下发的数据");
        }
        try {
            /** 切换MES数据源 start **/
            DynamicDataSourceContextHolder.push(DataSource.MES);
            // 分批保存，避免SQL Server参数上限2100的问题
            for (List<MesNcScheduleResult> batch : this.partitionList(mesList)) {
                mesNcScheduleResultMapper.batchInsert(batch);
            }
        } finally {
            DynamicDataSourceContextHolder.clear();
            /** 切换APS数据源 end **/
        }

        // 发送MQ通知MES
        return this.sendMqNotice(mesList, today, dataVersion, factoryCode, companyCode);
    }

    /**
     * 发送MQ通知
     */
    private AjaxResult sendMqNotice(List<MesNcScheduleResult> allMesList, LocalDate today, String dataVersion,
            String factoryCode, String companyCode) {
        AjaxResult ajaxResult;
        try {
            SyncParamsVO syncParamsVO = new SyncParamsVO();
            syncParamsVO.setSyncKey(ItfSyncKeyEnum.SYNC_TM_SCHEDULE_RESULT.getCode());
            syncParamsVO.setDataVersion(dataVersion);

            // 请求参数
            JSONObject params = new JSONObject();
            params.put("rowCount", allMesList.size());
            params.put("startDate", today.format(DATE_FORMATTER));
            params.put("endDate", today.format(DATE_FORMATTER));
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
                ajaxResult = AjaxResult.success(I18nUtil.getMessage("ui.data.column.scheduleResult.successPublish"));
            } else {
                ajaxResult = AjaxResult.error(logs.getMsg());
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.failedPublish"));
        }

        return ajaxResult;
    }

    /**
     * 根据日期过滤数据
     */
    private List<NcScheduleResult> filterByDate(List<NcScheduleResult> list, LocalDate date) {
        return list.stream().filter(item -> item.getScheduleDate() != null)
                .filter(item -> item.getScheduleDate().equals(date)).collect(Collectors.toList());
    }

    /**
     * 转换为MES实体列表
     */
    private List<MesNcScheduleResult> convertToMesList(List<NcScheduleResult> list, String dataVersion,
            String companyCode, String factoryCode) {
        return list.stream().map(item -> this.convertToMesEntity(item, dataVersion, companyCode, factoryCode))
                .collect(Collectors.toList());
    }

    /**
     * 转换为MES中间表实体
     */
    private MesNcScheduleResult convertToMesEntity(NcScheduleResult item, String dataVersion, String companyCode,
            String factoryCode) {
        MesNcScheduleResult mesItem = new MesNcScheduleResult();
        mesItem.setScheduleDate(item.getScheduleDate());
        mesItem.setBatchNo(item.getBatchNo());
        mesItem.setOrderNo(item.getOrderNo());
        mesItem.setLiningCode(item.getLiningCode());
        mesItem.setMaterialCode(item.getMachineCode());
        mesItem.setGlueCode(item.getGlueCode());
        mesItem.setMouthPlateCode(item.getMouthPlateCode());
        mesItem.setMachineCode(item.getMachineCode());
        // 班次计划量
        mesItem.setMidPlanQty(item.getClass1PlanQty());
        mesItem.setMidProduceOrder(item.getClass1Sequence());
        mesItem.setMidSysAnalysis(item.getClass1Analysis());
        mesItem.setNightPlanQty(item.getClass2PlanQty());
        mesItem.setNightProduceOrder(item.getClass2Sequence());
        mesItem.setNightSysAnalysis(item.getClass2Analysis());
        mesItem.setDayPlanQty(item.getClass3PlanQty());
        mesItem.setDayProduceOrder(item.getClass3Sequence());
        mesItem.setDaySysAnalysis(item.getClass3Analysis());
        // 状态与公共字段
        mesItem.setIsRelease("1");
        mesItem.setTailFlag(item.getTailFlag());
        mesItem.setRemark(item.getRemark());
        mesItem.setDataVersion(dataVersion);
        mesItem.setCompanyCode(companyCode);
        mesItem.setFactoryCode(factoryCode);
        return mesItem;
    }

    /**
     * 将列表按BATCH_SIZE分批，避免SQL Server参数上限2100的问题
     */
    private <T> List<List<T>> partitionList(List<T> list) {
        List<List<T>> partitions = new ArrayList<>();
        for (int i = 0; i < list.size(); i += BATCH_SIZE) {
            partitions.add(list.subList(i, Math.min(i + BATCH_SIZE, list.size())));
        }
        return partitions;
    }
}
