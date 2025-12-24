package com.zlt.sync.handle;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONValidator;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.utils.SpringUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.itf.vo.AuxReqSyncDataLogs;
import com.zlt.sync.configuration.QueueConfigDatas;
import com.zlt.sync.constants.ParamConstants;
import com.zlt.sync.constants.SyncConstants;
import com.zlt.sync.domain.AuxReqSyncDataLogsHis;
import com.zlt.sync.mapper.AuxReqSyncDataLogsHisMapper;
import com.zlt.sync.mapper.AuxReqSyncDataLogsMapper;
import com.zlt.sync.povo.SyncDataVO;
import com.zlt.sync.povo.SyncParamsVO;
import com.zlt.sync.service.SyncMsgSenderService;
import com.zlt.sync.utils.SpringBeanUtils;
import com.zlt.sync.utils.SyncDateUtils;
import com.zlt.sync.worker.Task;
import com.zlt.sync.worker.WorkThreadService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.*;

/**
 * 单点定时执行 所要同步的数据;
 * 注解 @Component、@EnableClusterScheduling
 */
@Component
//@EnableClusterScheduling
public class SyncScheduleTask {
    private static final Logger logger = LoggerFactory.getLogger(SyncScheduleTask.class);

    @Autowired
    private QueueConfigDatas queueConfigDatas;

    @Autowired
    private AuxReqSyncDataLogsMapper auxReqSyncDataLogsMapper;

    @Autowired
    private SyncMsgSenderService syncMsgSenderService;

    @Autowired
    private AuxReqSyncDataLogsHisMapper auxReqSyncDataLogsHisMapper;

    @Autowired
    private SyncConstants syncConstants;

    /**
     * 最大执行线程
     */
    private static Integer MAX_GROUP_THREAD = 10;

    /**
     * 默认超时重发间隔时长
     */
    private static Integer MAX_TIMEOUT_RESEND = 2;

    /**
     * 默认读取2小时前数据
     */
    private static Integer READ_TIMEOUT_HOURS = 2;

    /**
     * DATA_SYS=(MPS, APS), HAS_DATA=1, STATUS = 1, (BACK_ISSUE is not null & BACK_ISSUE != 1)
     *  DATA_SYS: 数据需求系统 主计划: MPS, 排程: APS
     *  BACK_ISSUE： 表示是否下发回传标志，这里过滤掉下载回传的数据
     *  sync-data-xxx-dev.yml: configs 节点的 接口配置，如果 是下发回传的，需要添加 backIssue: 1 配置
     *
     * @return
     */
    public List<AuxReqSyncDataLogs> querySyncDatasByType(List<String> syncKeys, String searchType, Map<String, Object> params) { //dockSys

        if (StringUtils.isEmpty(queueConfigDatas.getDataSys())) {
            logger.info("querySyncDatasByType-001, 同步需要系统数据: sync_data_xxx.yml没有配置 dataSys 项;");
            return null;
        }
        if (params == null) {
            params = new HashMap<>();
        }
        params.put("dataSys", queueConfigDatas.getDataSys());
        params.put("searchType", searchType);
        params.put("syncKeys", syncKeys);
        List<AuxReqSyncDataLogs> lists = auxReqSyncDataLogsMapper.queryReqSyncDataLogs(params);

        logger.info("querySyncDatasByType-001, 同步需要系统数据: " + queueConfigDatas.getDataSys() + "; searchType: " + searchType + "; syncKeys: " + StringUtils.join(syncKeys, ","));
        if (CollectionUtils.isEmpty(lists)) {
            logger.info("querySyncDatasByType-002: 可同步数据为空");
        } else {
            logger.info("querySyncDatasByType-003: 可同步数据数: " + lists.size());
        }

        return lists;
    }

    /**
     * 通用数据定时同步 (MES)
     * 1. 排程MES 需求比较即时
     * 2. CRM 基于一天一次
     * 3. WMS 报表数据很大
     * 4. sap 类似基础类型，比较固定，定时较长时间读取 (自己生成 待同步SAP数据同步 status=1, hasData=1)
     */
    public void commonSyncData(/*List<String> syncKeys*/String syncKeysStr) {
        logger.info("commonSyncData-000 执行同步数据检测: " + syncKeysStr);
        List<String> syncKeys = Arrays.asList(syncKeysStr.split(","));
        List<AuxReqSyncDataLogs> syncDatas = querySyncDatasByType(syncKeys, ParamConstants.SEAR_TYPE_CAN_SYNC_DATAS, null);
        logger.info("commonSyncData-001: 执行同步数据检测: ");

        if (CollectionUtils.isEmpty(syncDatas)) {
            logger.info("commonSyncData-002: 执行同步数据检测: 数据为空 ");
            return;
        }

        Map<String, List<AuxReqSyncDataLogs>> mergeDatas = mergeAvgSyncDatas(syncDatas);

        /**
         * 提交多线程执行
         */
        for (String threadId : mergeDatas.keySet()) {
            List<AuxReqSyncDataLogs> syncList = mergeDatas.get(threadId);
            if (CollectionUtils.isEmpty(syncList)) {
                continue;
            }

            Task task = new Task();
            task.setThreadId(threadId);
            task.setSyncKeys(syncKeys);
            task.setTaskType("SYNC_DATA");
            task.setSyncList(syncList);
            WorkThreadService.submit(task);
        }
    }

    /**
     * 平衡数组, 线程分组
     * @return
     */
    private Map<String, List<AuxReqSyncDataLogs>> mergeAvgSyncDatas(List<AuxReqSyncDataLogs> dataLogs) {
        logger.info("mergeAvgSyncDatas-001: 平衡数组, 线程分组, 待同步数量: " + dataLogs.size());
        Map<String, List<AuxReqSyncDataLogs>> syncKeyListMap = new HashMap<>();

        // 按syncKey时行分组
        List<AuxReqSyncDataLogs> dataList = null;
        for (AuxReqSyncDataLogs dataLog : dataLogs) {

            if (syncKeyListMap.containsKey(dataLog.getSyncKey())) {
                dataList = syncKeyListMap.get(dataLog.getSyncKey());
            } else {
                dataList = new ArrayList<>();
                syncKeyListMap.put(dataLog.getSyncKey(), dataList);
            }

            dataList.add(dataLog);
        }

        logger.info("mergeAvgSyncDatas-002: 平衡数组, 线程分组, 根据syncKey 分组, 接口类型数: " + syncKeyListMap.size());

        // 初始化 最大线程数
        Map<String, List<AuxReqSyncDataLogs>> grpMaps = new HashMap<>();
        for (int i=0; i< MAX_GROUP_THREAD; i++) {
            grpMaps.put("grp" + i, new ArrayList<AuxReqSyncDataLogs>());
        }

        // 取得最长记录
        int maxLength = 0;
        Map<String, Integer> maxListMap = new HashMap<>(); //初始最大数组

        for (String syncKey : syncKeyListMap.keySet()) {
            List<AuxReqSyncDataLogs> oneList = syncKeyListMap.get(syncKey);

            int curLength = oneList.size();
            if (maxLength == 0) {
                maxLength = curLength;
                maxListMap.put(syncKey, curLength);
            } else if (curLength == maxLength) { //假如相等
                maxListMap.put(syncKey, curLength);
            } else if (curLength > maxLength) {
                maxLength = curLength;
                maxListMap.clear();
                maxListMap.put(syncKey, curLength);
            }
        } //End

        logger.info("mergeAvgSyncDatas-003: 平衡数组, 线程分组, 根据syncKey 分组, 最大记录长度: " + maxLength + "; 共有最大记录: " + maxListMap.size());

        // 将最大记录添加到 初始化的分组里 grpMap
        int maxI = 0;
        for (String syncKey : maxListMap.keySet()) {
            grpMaps.put("grp" + maxI, syncKeyListMap.get(syncKey));
            maxI++;
        }

        // 第一层分组 (所有的数据)
        Set<Map.Entry<String, List<AuxReqSyncDataLogs>>> sets = syncKeyListMap.entrySet();
        Iterator<Map.Entry<String, List<AuxReqSyncDataLogs>>> iterator = sets.iterator();

        while (iterator.hasNext()) {
            Map.Entry<String, List<AuxReqSyncDataLogs>> entry = iterator.next();

            String syncKey = entry.getKey();
            if (maxListMap.containsKey(syncKey)) {
                iterator.remove();
                continue;
            }

            List<AuxReqSyncDataLogs> qlist = entry.getValue();

            for (int i=0; i< MAX_GROUP_THREAD; i++) {
                List<AuxReqSyncDataLogs> grpListOne = grpMaps.get("grp" + i);

                if (grpListOne.size() == maxLength) {
                    continue;
                }
                else if (qlist.size() + grpListOne.size() > maxLength) { //超过最大记录
                    continue;
                }

                grpListOne.addAll(qlist);
                iterator.remove();
                break;
            }
        } //while

        logger.info("mergeAvgSyncDatas-003: 平衡数组, 线程分组, 第一层分组结束");

        // 第二层分组
        if (sets.size() > 0) {
            iterator = sets.iterator();

            while (iterator.hasNext()) {
                Map.Entry<String, List<AuxReqSyncDataLogs>> entry = iterator.next();
                List<AuxReqSyncDataLogs> qlist = entry.getValue();

                List<AuxReqSyncDataLogs> grpListOne = grpMaps.get("grp" + getMinGrpListIndex(grpMaps, qlist, maxLength));
                grpListOne.addAll(qlist);
            }
        }

        return grpMaps;
    }

    /**
     * 获取分组中最小记录;
     * @param grpMaps
     * @return
     */
    private int getMinGrpListIndex(Map<String, List<AuxReqSyncDataLogs>> grpMaps, List<AuxReqSyncDataLogs> qlist, int maxLength) {
        int minLength = -1,
            minI = -1;

        //从现有记录中添加
        for (int i=0; i< MAX_GROUP_THREAD; i++) {
            List<AuxReqSyncDataLogs> grpListOne = grpMaps.get("grp" + i);

            if (grpListOne.size() > 0 && grpListOne.size() + qlist.size() <= maxLength) {
                minI = i;
                break;
            }
        }

        if (minI != -1) {
            return minI;
        }

        //寻找最小记录
        for (int i=0; i< MAX_GROUP_THREAD; i++) {
            List<AuxReqSyncDataLogs> grpListOne = grpMaps.get("grp" + i);
            if (minLength == -1) {
                minLength = grpListOne.size();
                minI = i;
            }
            else if (minLength >= 0 && minLength > grpListOne.size()) {
                minLength = grpListOne.size();
                minI = i;
            }
        }
        return minI;
    }

    /**
     * syncSucced 从中间库成功同步到业务库
     * syncCntSucced 从业务库成功同步到中间库
     * 定义获取已经同步成功的数据
     */
    public void feedbackSyncdSucssDatas(String searchType) {

        List<AuxReqSyncDataLogs> succList = querySyncDatasByType(null, searchType, null); //"syncSucced"
        if (CollectionUtils.isEmpty(succList)) {
            logger.error("feedbackSyncdSucssDatas-001 定义获取已经同步成功的数据, 数据为空, dataSys: " + queueConfigDatas.getDataSys());
            return;
        }

        logger.info("feedbackSyncdSucssDatas-002 定义获取已经同步成功的数据, 数据量: "+ succList.size() +"; dataSys: " + queueConfigDatas.getDataSys());

        if (!ParamConstants.SEAR_TYPE_SUCCESSED_CNT_METHOD.equals(searchType)) {
            SyncDataHandle handle = SpringUtils.getBean(SyncDataHandle.class);
            if (handle != null) {
                AjaxResult ajaxResult = new AjaxResult();
                ajaxResult.put(AjaxResult.CODE_TAG, 200);
                ajaxResult.put(AjaxResult.MSG_TAG, "");
                ajaxResult.put(AjaxResult.DATA_TAG, succList);
                handle.asyncResult(ajaxResult);
                logger.info("feedbackSyncdSucssDatas-003 定义获取已经同步成功的数据; 返回前端成功结果");
            } else {
                logger.error("feedbackSyncdSucssDatas-004 定义获取已经同步成功的数据，没有实现SyncDataHandle抽象类");
            }
        } else {
            for (AuxReqSyncDataLogs dataLogs: succList) {
                syncCntSuccedHandle(dataLogs);
            }
        }
    }

    @Transactional
    public void syncCntSuccedHandle(AuxReqSyncDataLogs dataLogs) {
        logger.info("syncCntSuccedHandle-001 回传至中间库成功后发送消息 - 更改请求状态=1");

        // 先更改状态
        dataLogs.setStatus(1);
        dataLogs.setUpdateDate(new Date());
        auxReqSyncDataLogsMapper.update(dataLogs);

        logger.info("syncCntSuccedHandle-001 回传至中间库成功后发送消息 - 添加历史记录");
        AuxReqSyncDataLogsHis logsHis = new AuxReqSyncDataLogsHis();
        SpringBeanUtils.copyPropertiesIgnoreNull(dataLogs, logsHis);
        logsHis.setMsgId(UUID.randomUUID().toString());
        auxReqSyncDataLogsHisMapper.insert(logsHis);

        logger.info("syncCntSuccedHandle-001 回传至中间库成功后发送消息 - 发送消息给对接系统");
        SyncParamsVO paramsVO = new SyncParamsVO();
        SpringBeanUtils.copyPropertiesIgnoreNull(dataLogs, paramsVO);
        JSONObject params;
        try {
            params = JSONObject.parseObject(dataLogs.getParams());
        } catch (Exception ex) {
            params = new JSONObject();
            params.put("version", dataLogs.getDataVersion());
        }
        paramsVO.setParams(params);

        syncMsgSenderService.send(paramsVO.toJSONString(), paramsVO.getDataSys(), paramsVO.getDockSys());

        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * 只针对 status=0,3 的状态进行检测
     * 超过 2 个小时的
     * (未反馈类型)
     * mapper 读取大于等于 1 小时的数据;
     */
    public void checkTimeoutRequest() {
        Map<String, Object> params = new HashMap<>();
        Integer readTimeoutHour = queueConfigDatas.getReadTimeoutHours() != null ?
                queueConfigDatas.getReadTimeoutHours() : READ_TIMEOUT_HOURS;
        params.put("hours", readTimeoutHour);

        List<AuxReqSyncDataLogs> outList = querySyncDatasByType(null, "requestTimeout", params); //"requestTimeout"
        if (CollectionUtils.isEmpty(outList)) {
            logger.info("checkTimeoutRequest-001 针对 status=0,3 的状态进行检测, 数据为空, dataSys: " + queueConfigDatas.getDataSys());
            return;
        }

        SyncDataHandle handle = SpringUtils.getBean(SyncDataHandle.class);
        if (handle == null) {
            logger.error("checkTimeoutRequest-002 针对 status=0,3 的状态进行检测: SyncDataHandle 未实现");
            return;
        }
        for (AuxReqSyncDataLogs dataLogs : outList) {
            SyncDataVO dataVO = syncConstants.getSyncDataByKey(dataLogs.getSyncKey());

            if (dataVO == null) {
                logger.error("checkTimeoutRequest-003 针对 status=0,3 的状态进行检测: 不存在syncKey: " + dataLogs.getSyncKey() + "; 修改为异常超时");
                // 设置超异常状态，记录历史
                setTimeoutHis(dataLogs);
                continue;
            }

           Integer timeOut = dataVO.getTimeout() != null ? dataVO.getTimeout() : MAX_TIMEOUT_RESEND;
            logger.info("checkTimeoutRequest-004 针对 status=0,3 的状态进行检测: syncKey: " + dataLogs.getSyncKey() + "; " + (dataVO.getTimeout() != null ? "配置值" : "默认值" ));

            long between = SyncDateUtils.between(dataLogs.getUpdateDate(), new Date(), SyncDateUtils.BETWEEN_HOUR);

            if (between < timeOut.longValue()) {
                logger.info("checkTimeoutRequest-005 针对 status=0,3 的状态进行检测: syncKey: " + dataLogs.getSyncKey() + "; 未超时: " + timeOut + "; " + between);
                continue;
            }

            logger.error("checkTimeoutRequest-006 针对 status=0,3 的状态进行检测: syncKey: " + dataLogs.getSyncKey() + "; 已超时: " + timeOut + "; " + between);

            // 设置超异常状态，记录历史
            setTimeoutHis(dataLogs);

            // 检测创建时间，如果不是当天的不发送
            if (dataLogs.getCreateDate() == null) {
                logger.error("checkTimeoutRequest-007 针对 status=0,3 的状态进行检测: syncKey: " + dataLogs.getSyncKey() + "; 已超时: " + timeOut + "; " + between + "; 创建日期异常;");
                continue;
            }

            String todayYmd = DateUtils.dateTimeNow(DateUtils.YYYY_MM_DD),
                    logYmd = DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD, dataLogs.getCreateDate());

            if (!todayYmd.equals(logYmd)) {
                logger.error("checkTimeoutRequest-008 针对 status=0,3 的状态进行检测: syncKey: " + dataLogs.getSyncKey() + "; 已超时: " + timeOut + "; " + between + "; 非当天记录;");
                continue;
            }

            /**
             * 重新发送请求
             * dataVersion及params.version会变
             */
            if (Integer.valueOf(1).equals(dataVO.getReSend())) {
                logger.error("checkTimeoutRequest-009 针对 status=0,3 的状态进行检测: syncKey: " + dataLogs.getSyncKey() + "; 已超时: " + timeOut + "; " + between + "; 需要重新发达请求;");
                SyncParamsVO paramsVO = new SyncParamsVO();

                SpringBeanUtils.copyPropertiesIgnoreNull(dataVO, paramsVO);

                paramsVO.setSyncKey(dataLogs.getSyncKey());

                paramsVO.setCompanyCode(dataLogs.getCompanyCode());
                paramsVO.setFactoryCode(dataLogs.getFactoryCode());

                JSONValidator validator = JSONValidator.from(dataLogs.getParams());
                if (validator.validate()) {
                    paramsVO.setParams(JSONObject.parseObject(dataLogs.getParams()));
                    handle.syncReRequest(paramsVO);
                } else {
                    logger.warn("checkTimeoutRequest-010 针对 status=0,3 的状态进行检测: syncKey: " + dataLogs.getSyncKey() + "; 原有 params不可用，格式不符json;");
                    handle.syncRequest(paramsVO);
                }

            }
        }
    }

    /**
     * 设置超状态，记录历史
     * @param dataLogs
     */
    private void setTimeoutHis(AuxReqSyncDataLogs dataLogs) {
        dataLogs.setStatus(4);
        dataLogs.setUpdateDate(new Date());
        auxReqSyncDataLogsMapper.update(dataLogs);

        logger.info("setTimeoutHis-001 置超状态，记录历史 - 添加历史记录");
        AuxReqSyncDataLogsHis logsHis = new AuxReqSyncDataLogsHis();
        SpringBeanUtils.copyPropertiesIgnoreNull(dataLogs, logsHis);
        logsHis.setMsgId(UUID.randomUUID().toString());
        auxReqSyncDataLogsHisMapper.insert(logsHis);
    }

}
