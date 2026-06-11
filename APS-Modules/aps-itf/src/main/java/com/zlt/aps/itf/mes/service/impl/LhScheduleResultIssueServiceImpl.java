package com.zlt.aps.itf.mes.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.itf.constant.SysCode;
import com.zlt.aps.itf.mes.enums.ItfSyncKeyEnum;
import com.zlt.aps.itf.mes.mapper.LhScheduleResultIssueMapper;
import com.zlt.aps.itf.mes.service.ILhScheduleResultIssueService;
import com.zlt.aps.itf.vo.MesLhScheduleResult;
import com.zlt.aps.itf.vo.SyncDataLogs;
import com.zlt.aps.mp.api.domain.entity.LhScheduleResultIssue;
import com.zlt.sync.handle.SyncDataHandle;
import com.zlt.sync.povo.SyncParamsVO;
import com.zlt.sync.service.SyncDataLogsService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 硫化排程结果下发服务实现
 *
 * @author APS Team
 * @since 2.0.0
 */
@Slf4j
@Service
public class LhScheduleResultIssueServiceImpl implements ILhScheduleResultIssueService {

    @Autowired
    private SyncDataHandle syncDataHandle;

    @Autowired
    private SyncDataLogsService syncDataLogsService;

    @Autowired
    private LhScheduleResultIssueMapper lhScheduleResultIssueMapper;

    /**
     * 日期格式化器
     */
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * 下发硫化排程结果到MES
     * 业务规则：
     * 每条硫化排程结果自带8班数据，覆盖排程窗口T日到T+2日三天（排程日期为T+1日）：
     * 1. 窗口首日（T日，排程日期前一天）数据：更新（仅更新早中班，不覆盖夜班），不存在则插入
     * 2. 窗口次日（T+1日，排程日期当天）数据：更新（存在则更新，不存在则插入），包含夜早中3班
     * 3. 窗口第三日（T+2日，排程日期后一天）数据：先删除后插入，包含夜早中3班
     * 日期从下发数据中推导，不再依赖LocalDate.now()
     *
     * @param lhScheduleResultIssueList 硫化排程结果列表
     * @param factoryCode               厂别
     * @param companyCode               分公司编码
     * @return 下发结果
     */
    @Override
    public AjaxResult issueLhScheduleResult(List<LhScheduleResultIssue> lhScheduleResultIssueList, String factoryCode, String companyCode) {
        if (CollectionUtils.isEmpty(lhScheduleResultIssueList)) {
            return AjaxResult.success();
        }

        // 获取下发接口版本号
        String dataVersion = syncDataHandle.getDataVersion(ItfSyncKeyEnum.SYNC_LH_SCHEDULE_RESULT.getCode());

        // 从数据中提取所有不重复的排程日期并排序
        List<LocalDate> distinctDates = lhScheduleResultIssueList.stream()
                .filter(item -> item.getScheduleDate() != null)
                .map(item -> item.getScheduleDate().toLocalDate())
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        if (distinctDates.isEmpty()) {
            return AjaxResult.success("没有需要下发的数据");
        }

        LocalDate firstDate = distinctDates.get(0);
        LocalDate lastDate = distinctDates.get(distinctDates.size() - 1);

        // 按日期分组处理数据
        List<LhScheduleResultIssue> day1List = filterByDate(lhScheduleResultIssueList, distinctDates.get(0));
        List<LhScheduleResultIssue> day2List = distinctDates.size() > 1
                ? filterByDate(lhScheduleResultIssueList, distinctDates.get(1))
                : new ArrayList<>();
        List<LhScheduleResultIssue> day3List = distinctDates.size() > 2
                ? filterByDate(lhScheduleResultIssueList, distinctDates.get(2))
                : new ArrayList<>();

        // 处理窗口首日数据：转换为MES实体
        List<MesLhScheduleResult> day1MesList = convertToMesList(day1List, dataVersion, companyCode, factoryCode);

        // 处理窗口次日数据：转换为MES实体
        List<MesLhScheduleResult> day2MesList = convertToMesList(day2List, dataVersion, companyCode, factoryCode);

        // 处理排程日期当天数据：转换为MES实体
        List<MesLhScheduleResult> day3MesList = convertToMesList(day3List, dataVersion, companyCode, factoryCode);

        // 窗口首日数据：仅更新早中班（不覆盖夜班），不存在则插入
        upsertDay1LhScheduleResult(day1MesList, dataVersion);
        // 窗口次日数据：更新（存在则更新，不存在则插入）
        upsertLhScheduleResult(day2MesList, dataVersion);

        // 排程日期当天数据：先删除后插入（确保数据干净）
        insertLhScheduleResult(day3MesList, lastDate, dataVersion);

        // 合并所有数据用于发送MQ
        List<MesLhScheduleResult> allMesList = new ArrayList<>();
        allMesList.addAll(day1MesList);
        allMesList.addAll(day2MesList);
        allMesList.addAll(day3MesList);

        if (CollectionUtils.isEmpty(allMesList)) {
            return AjaxResult.success("没有需要下发的数据");
        }

        // 发送MQ通知MES
        return sendMqNotice(allMesList, firstDate, lastDate, dataVersion, factoryCode, companyCode);
    }

    /**
     * 按日期过滤数据
     */
    private List<LhScheduleResultIssue> filterByDate(List<LhScheduleResultIssue> list, LocalDate date) {
        if (CollectionUtils.isEmpty(list)) {
            return new ArrayList<>();
        }
        return list.stream()
                .filter(item -> item.getScheduleDate() != null)
                .filter(item -> {
                    java.time.LocalDate itemDate = item.getScheduleDate().toLocalDate();
                    return itemDate.equals(date);
                })
                .collect(Collectors.toList());
    }

    /**
     * 转换为MES实体列表
     */
    private List<MesLhScheduleResult> convertToMesList(List<LhScheduleResultIssue> list, String dataVersion, 
                                                     String companyCode, String factoryCode) {
        List<MesLhScheduleResult> result = new ArrayList<>();
        for (LhScheduleResultIssue item : list) {
            MesLhScheduleResult mesItem = convertToMesEntity(item, dataVersion, companyCode, factoryCode);
            result.add(mesItem);
        }
        return result;
    }

    /**
     * 转换为MES中间表实体
     */
    private MesLhScheduleResult convertToMesEntity(LhScheduleResultIssue item, String dataVersion, 
                                                String companyCode, String factoryCode) {
        MesLhScheduleResult mesItem = new MesLhScheduleResult();
        BeanUtils.copyProperties(item, mesItem);
        mesItem.setDataVersion(dataVersion);
        mesItem.setCompanyCode(companyCode);
        mesItem.setFactoryCode(factoryCode);
        // 将LocalDateTime转换为LocalDate
        if (item.getScheduleDate() != null) {
            mesItem.setScheduleDate(item.getScheduleDate().toLocalDate());
        }
        return mesItem;
    }

    /**
     * 更新或插入数据（存在则更新，不存在则插入）
     * 中间表MES_LH_SCHEDULE_RESULT建在MES分库，Mapper已通过@DS(DataSource.MES)指定数据源
     */
    private void upsertLhScheduleResult(List<MesLhScheduleResult> mesList, String dataVersion) {
        if (CollectionUtils.isEmpty(mesList)) {
            return;
        }
        for (MesLhScheduleResult mesItem : mesList) {
            int updateCount = lhScheduleResultIssueMapper.updateByScheduleDateAndMachine(mesItem);
            if (updateCount == 0) {
                List<MesLhScheduleResult> insertList = new ArrayList<>();
                insertList.add(mesItem);
                lhScheduleResultIssueMapper.batchInsertLhScheduleResult(insertList);
            }
        }
    }

    /**
     * 窗口首日（T日，排程日期前一天）更新或插入数据
     * 仅更新早中班（class2、class3），不覆盖夜班（class1）数据
     * 窗口首日无夜班排产数据，避免将MES已有的夜班数据覆盖为空
     * 中间表MES_LH_SCHEDULE_RESULT建在MES分库，Mapper已通过@DS(DataSource.MES)指定数据源
     */
    private void upsertDay1LhScheduleResult(List<MesLhScheduleResult> mesList, String dataVersion) {
        if (CollectionUtils.isEmpty(mesList)) {
            return;
        }
        for (MesLhScheduleResult mesItem : mesList) {
            int updateCount = lhScheduleResultIssueMapper.updateDay1ByScheduleDateAndMachine(mesItem);
            if (updateCount == 0) {
                List<MesLhScheduleResult> insertList = new ArrayList<>();
                insertList.add(mesItem);
                lhScheduleResultIssueMapper.batchInsertLhScheduleResult(insertList);
            }
        }
    }

    /**
     * 插入数据（先删除指定日期的旧数据，再插入新数据）
     * 中间表MES_LH_SCHEDULE_RESULT建在MES分库，Mapper已通过@DS(DataSource.MES)指定数据源
     */
    private void insertLhScheduleResult(List<MesLhScheduleResult> mesList, LocalDate scheduleDate, String dataVersion) {
        if (CollectionUtils.isEmpty(mesList)) {
            return;
        }
        String dateStr = scheduleDate.format(DATE_FORMATTER);
        lhScheduleResultIssueMapper.deleteByScheduleDate(dateStr, dataVersion);
        lhScheduleResultIssueMapper.batchInsertLhScheduleResult(mesList);
    }

    /**
     * 发送MQ通知
     */
    private AjaxResult sendMqNotice(List<MesLhScheduleResult> allMesList, LocalDate startDate,
                                  LocalDate endDate, String dataVersion,
                                  String factoryCode, String companyCode) {
        AjaxResult ajaxResult;
        try {
            SyncParamsVO syncParamsVO = new SyncParamsVO();
            syncParamsVO.setSyncKey(ItfSyncKeyEnum.SYNC_LH_SCHEDULE_RESULT.getCode());
            syncParamsVO.setDataVersion(dataVersion);

            // 请求参数
            JSONObject params = new JSONObject();
            params.put("rowCount", allMesList.size());
            params.put("startDate", startDate.format(DATE_FORMATTER));
            params.put("endDate", endDate.format(DATE_FORMATTER));
            syncParamsVO.setParams(params);
            syncParamsVO.setDataSys(SysCode.APS);
            syncParamsVO.setDockSys(com.zlt.aps.common.core.constant.ApsConstant.DOCK_SYS_MES);
            syncParamsVO.setFactoryCode(factoryCode);
            syncParamsVO.setCompanyCode(companyCode);

            // 往消息队列发送消息
            syncDataHandle.syncNotice(syncParamsVO);

            // 取回mes的反馈结果
            SyncDataLogs logs = syncDataLogsService.getSyncDataResult(dataVersion);
            String status = logs.getStatus();
            if (com.zlt.aps.common.core.constant.ApsConstant.IS_RELEASE.equals(status)) {
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
}
