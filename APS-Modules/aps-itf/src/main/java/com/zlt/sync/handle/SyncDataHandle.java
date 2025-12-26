package com.zlt.sync.handle;

import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.nacos.common.utils.MD5Utils;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.exception.CustomException;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.itf.mes.enums.ItfSyncKeyEnum;
import com.zlt.aps.itf.vo.AuxReqSyncDataLogs;
import com.zlt.sync.domain.AuxDataVersions;
import com.zlt.sync.domain.AuxReqSyncDataLogsHis;
import com.zlt.sync.domain.vo.AuxDataVersionsVO;
import com.zlt.sync.handle.dockSys.DockCommonHandle;
import com.zlt.sync.mapper.AuxDataVersionsMapper;
import com.zlt.sync.mapper.AuxReqSyncDataLogsHisMapper;
import com.zlt.sync.mapper.AuxReqSyncDataLogsMapper;
import com.zlt.sync.povo.SyncParamsVO;
import com.zlt.sync.utils.RedisLock;
import com.zlt.sync.utils.SpringBeanUtils;

/**
 * 公共同步调用接口方法
 * 需要继承 且注解 @Component
 */
@Service
public class SyncDataHandle {
    private static final Logger logger = LoggerFactory.getLogger(SyncDataHandle.class);

    @Autowired
    private RedisLock redisLock;

    /**
     * 缓存前缀KEY
     */
    private static String SYNC_DATA_KEY = "SYNC_DATA";

    /**
     * 最小重复提交间隔时长, 单位 分钟
     */
    private static Integer MIN_INTERVAL_TIME = 5;
    private static String VERSION_FIRST_NUMBER = "000001";

    ///////////////////////////////////////////////////////////////////////

    // mapper
    @Autowired
    private AuxReqSyncDataLogsMapper auxReqSyncDataLogsMapper;

    @Autowired
    private AuxReqSyncDataLogsHisMapper auxReqSyncDataLogsHisMapper;

    @Autowired
    private AuxDataVersionsMapper auxDataVersionsMapper;

    @Autowired
    private DockCommonHandle dockCommonHandle;
    ////////////////////////////////////////////////////////////////////////

    /**
     * 数据请求
     * {
     *     syncKey: ‘KEY’，
     *     dockSys: 'CRM0',
     *     params: {
     *         //目前排程MES需要自己传递参数
     *         //主计划由同步时服务生成参数
     *     }
     * }
     * @return
     */
    public AjaxResult syncRequest(SyncParamsVO paramsVO) {

        if (paramsVO == null) {
            logger.error("syncRequest-001 参数访问不能为空");
            return AjaxResult.error("参数访问不能为空");
        }

        if (StringUtils.isEmpty(paramsVO.getSyncKey())) {
            logger.error("syncRequest-002 请求同步服务Key syncKey 不能为空");
            return AjaxResult.error("请求同步服务Key syncKey 不能为空");
        }

        logger.info("syncRequest-003 请求数据, 参数: " + paramsVO.toString());

        /**
         * 主计划目前 params 为空，但目前主计划接口基本一个一个请求
         * (R)equest +SYNCKEY+ YYYYMMDDHHmm + PARAMS
         */
        String mergeStr = "R" + paramsVO.getSyncKey() + DateUtils.dateTimeNow("yyyyMMddHHmm") + (paramsVO.getParams() == null? "" : paramsVO.getParams().toJSONString() );

        logger.info("syncRequest-005 请求数据(保证分钟内不重复提交), msgKey: " + mergeStr);

        String msgKey = null;
        try {
            msgKey = MD5Utils.md5Hex(mergeStr.getBytes());
        }catch (NoSuchAlgorithmException ex) {
            logger.error("syncNotice-0051 请求数据(保证分钟内不重复提交), msgKey: " + mergeStr + "; 生成md5 报错: " + ex.getMessage(), ex);
            return AjaxResult.error("请求数据(保证分钟内不重复提交), msgKey: " + mergeStr + "; 生成md5 报错: " + ex.getMessage());
        }

        Map<String, Object> params = new HashMap<>();
        params.put("msgKey", msgKey);
        params.put("interval", MIN_INTERVAL_TIME);
        params.put("searchType", "interval");
        List<AuxReqSyncDataLogs> existsLog = auxReqSyncDataLogsMapper.queryReqSyncDataLogs(params);

        if (!CollectionUtils.isEmpty(existsLog)) {
            logger.error("syncRequest-006 重复提交请求, msgKey: " + msgKey);
            return AjaxResult.error("重复提交请求, msgKey: " + msgKey);
        }

        // 生成版本号
        initVersion(paramsVO);

        AuxReqSyncDataLogs dataLogs = getAuxReqSyncDataLogsInit(paramsVO, msgKey);

        // 转到对应系统处理类
        try {
            DockSysFactory.handle(paramsVO, dataLogs);
        } catch (Exception ex) {
            logger.error("syncRequest-007 提交请求发生异常: " + ex.getMessage(), ex);
            return AjaxResult.error(ex.getMessage());
        }
        logger.info("syncRequest-008 请求数据提交成功: " + paramsVO.toString());

        return AjaxResult.success();
    }

    protected AuxReqSyncDataLogs getAuxReqSyncDataLogsInit(SyncParamsVO paramsVO, String msgKey) {
        AuxReqSyncDataLogs dataLogs = new AuxReqSyncDataLogs();
        SpringBeanUtils.copyPropertiesIgnoreNull(paramsVO, dataLogs);
        dataLogs.setMsgId(UUID.randomUUID().toString());
        if (paramsVO.getStatus() == null) { // 如果参数已经包含状态，则直接使用参数中的状态
        	dataLogs.setStatus(0);
        }
        dataLogs.setMsgKey(msgKey);
        dataLogs.setCreateDate(new Date());
        dataLogs.setUpdateDate(new Date());
        return dataLogs;
    }

    protected void initVersion(SyncParamsVO paramsVO) {
        paramsVO.setDataVersion(getDataVersion(paramsVO.getSyncKey()));
    }

    /**
     * 数据同步通知，用于主动通知对接系统同步
     * {
     *     syncKey: 'KEY',
     *     dockSys: 'CRM0',
     *     version: '202107270001'
     * }
     * @return
     */
    public AjaxResult syncNotice(SyncParamsVO paramsVO) {
        if (paramsVO == null) {
            logger.error("syncNotice-001 参数访问不能为空");
            return AjaxResult.error("参数访问不能为空");
        }

        if (StringUtils.isEmpty(paramsVO.getSyncKey())) {
            logger.error("syncNotice-002 通知同步服务Key syncKey 不能为空");
            return AjaxResult.error("通知同步服务Key syncKey 不能为空");
        }

        if (StringUtils.isEmpty(paramsVO.getDataVersion())) {
            logger.error("syncNotice-003 通知同步服务数据版本号 dataVersion 不能为空");
            return AjaxResult.error("通知同步服务数据版本号 dataVersion 不能为空");
        }

        logger.info("syncNotice-004 通知数据, 参数: " + paramsVO.toString());

        JSONObject params = paramsVO.getParams();

        if (params == null) {
            params = new JSONObject();
        }

        params.put("version", paramsVO.getDataVersion());

        /**
         * 主计划目前 params 为空，但目前主计划接口基本一个一个请求,通知
         * (N)otice +SYNCKEY+ YYYYMMDDHHmm + PARAMS
         */
        String mergeStr = "N" + paramsVO.getSyncKey() + DateUtils.dateTimeNow("yyyyMMddHHmm") + (paramsVO.getParams() == null? "" : paramsVO.getParams().toJSONString() );

        logger.info("syncNotice-006 请求数据(保证分钟内不重复提交), msgKey: " + mergeStr);

        String msgKey = null;
        try {
            msgKey = MD5Utils.md5Hex(mergeStr.getBytes());
        }catch (NoSuchAlgorithmException ex) {
            logger.error("syncNotice-0061 通知同步服务(保证分钟内不重复提交), msgKey: " + mergeStr + "; 生成md5 报错: " + ex.getMessage(), ex);
            return AjaxResult.error("通知同步服务(保证分钟内不重复提交), msgKey: " + mergeStr + "; 生成md5 报错: " + ex.getMessage());
        }

        Map<String, Object> searParams = new HashMap<>();
        searParams.put("msgKey", msgKey);
        searParams.put("interval", MIN_INTERVAL_TIME);
        searParams.put("searchType", "interval");
        List<AuxReqSyncDataLogs> existsLog = auxReqSyncDataLogsMapper.queryReqSyncDataLogs(searParams);

        if (!CollectionUtils.isEmpty(existsLog)) {
            logger.error("syncNotice-007 重复提交请求, msgKey: " + msgKey);
            return AjaxResult.error("重复提交请求, msgKey: " + msgKey);
        }

        paramsVO.setHasData(1); //主动通知同步数据, 表示有数据同步
        paramsVO.setStatus(1);

        AuxReqSyncDataLogs dataLogs = new AuxReqSyncDataLogs();
        SpringBeanUtils.copyPropertiesIgnoreNull(paramsVO, dataLogs);
        dataLogs.setMsgId(UUID.randomUUID().toString());
        dataLogs.setStatus(1);
        dataLogs.setMsgKey(msgKey);
        dataLogs.setCreateDate(new Date());
        dataLogs.setUpdateDate(new Date());

        // 转到对应系统处理类
        try {
            DockSysFactory.handle(paramsVO, dataLogs);
        } catch (Exception ex) {
            logger.error("syncNotice-008 通知同步服务发生异常: " + ex.getMessage(), ex);
            return AjaxResult.error(ex.getMessage());
        }
        logger.info("syncNotice-009 通知同步服务提交成功: " + paramsVO.toString());

        return AjaxResult.success();
    }

    /**
     * 用于异步返回的统一通知方法
     * 业务模块自己实现
     */
    public void asyncResult(AjaxResult result) {
    	
    }

    /**
     * 此方法用于获取 版本号
     * redis锁
     * @param syncKey 用于返回数据所属对接系统
     * @return
     */
    public String getDataVersion(String syncKey) {
    	ItfSyncKeyEnum itfSyncKey = ItfSyncKeyEnum.getByCode(syncKey);
        if (itfSyncKey == null) {
            logger.error("getDataVersion-001 获取数据版本号: syncKey: " + syncKey + ", 获取对象为空;");
            throw new CustomException("同步数据标志不存在: " + syncKey + ", 请对照 sync-data-${spring.profiles.active}.yml 获取");
        }
        return getDataVersion(itfSyncKey);
    }

    /**
     * 此方法用于获取 版本号
     * redis锁
     * @param dataVO 用于返回数据所属对接系统
     * @return
     */
    public String getDataVersion(ItfSyncKeyEnum itfSyncKey) {
        if (itfSyncKey == null) {
            logger.error("getDataVersion-001 获取数据版本号: 传入对象为空");
            throw new CustomException("dataVo由 syncKey 在前置阶段获取");
        }

        logger.info("getDataVersion-003 获取数据版本号: FROM_SYS: " + itfSyncKey.getDataSys() + "_TO_SYS: " + itfSyncKey.getDockSys());

        String lockKey = SYNC_DATA_KEY + ":" + itfSyncKey.getDataSys() + ":" + itfSyncKey.getDockSys();
        if (!redisLock.lock(lockKey, 3000, 2)) {
            logger.error("getDataVersion-0031 获取数据版本号: 没有获取版本锁，请重试 ");
            throw new CustomException("没有获取版本锁，请重试");
        }

        try {
            logger.info("getDataVersion-005 获取数据版本号: FROM_SYS: " + itfSyncKey.getDataSys() + "_TO_SYS: " + itfSyncKey.getDockSys() + ", 获取到REDIS 锁");
            //获取当前最新版本号
            Map<String, Object> params = new HashMap<>();
            params.put("fromSys", itfSyncKey.getDataSys());
            params.put("toSys", itfSyncKey.getDockSys());
            List<AuxDataVersionsVO> versions = auxDataVersionsMapper.queryDataVersion(params);

            if (CollectionUtils.isEmpty(versions)) {
                logger.error("getDataVersion-0051 获取版本数据为空异常，至少会有一条记录;FROM_SYS: " + itfSyncKey.getDataSys() + "_TO_SYS: " + itfSyncKey.getDockSys());
                throw new CustomException("获取版本数据为空异常，至少会有一条记录;FROM_SYS: " + itfSyncKey.getDataSys() + "_TO_SYS: " + itfSyncKey.getDockSys());
            }

            AuxDataVersionsVO version = versions.get(0);

            if (null == version || StringUtils.isEmpty(version.getYyyyMMdd())) {
                logger.error("getDataVersion-0052 获取版本数据为空异常，数据异常日期为null;FROM_SYS: " + itfSyncKey.getDataSys() + "_TO_SYS: " + itfSyncKey.getDockSys());
                throw new CustomException("获取版本数据为空异常，数据异常日期为null;FROM_SYS: " + itfSyncKey.getDataSys() + "_TO_SYS: " + itfSyncKey.getDockSys());
            }

            //获取当前日期
            String nowDay = version.getYyyyMMdd(),
                    prefix = itfSyncKey.getDataSys() + "_" + itfSyncKey.getDockSys() + "_" + nowDay;

            String dataVersion = prefix + VERSION_FIRST_NUMBER;
            logger.info("getDataVersion-004 获取数据版本号: FROM_SYS: " + itfSyncKey.getDataSys() + "_TO_SYS: " + itfSyncKey.getDockSys() + ", 版本前缀: " + prefix);

            if (!StringUtils.isEmpty(version.getVerId())) {
                if (!StringUtils.isEmpty(version.getYmdVersion())) { //getDataVersion
                    if (version.getYmdVersion().equals(nowDay)) {

                        Integer verVersion = version.getVerVersion();

                        if (verVersion == null) {
                            verVersion = 0;
                        }

                        verVersion++;
                        dataVersion = prefix + String.format("%06d", verVersion);
                        version.setDataVersion(dataVersion);
                        version.setVerVersion(verVersion);

                    } else {
                        // 新的一天的
                        version.setDataVersion(dataVersion);
                        version.setYmdVersion(nowDay);
                        version.setVerVersion(1);
                    }

                } else {
                    // 数据为空异常, 一般不会走到这
                    logger.error("getDataVersion-007 获取数据版本号: FROM_SYS: " + itfSyncKey.getDataSys() + "_TO_SYS: " + itfSyncKey.getDockSys() + ", 版本值 为空异常");
                    version.setDataVersion(dataVersion);
                    version.setYmdVersion(nowDay);
                    version.setVerVersion(1);
                }

                version.setUpdateDate(new Date());

                logger.info("getDataVersion-008 获取数据版本号: FROM_SYS: " + itfSyncKey.getDataSys() + "_TO_SYS: " + itfSyncKey.getDockSys() + ", 获取当前版本号: " + version.getDataVersion());
                // 更新版本数据
                auxDataVersionsMapper.update(version);
            } else {
                // 没找到数据
                AuxDataVersions newVersion = new AuxDataVersions();
                newVersion.setFromSys(itfSyncKey.getDataSys());
                newVersion.setToSys(itfSyncKey.getDockSys());
                newVersion.setVerId(UUID.randomUUID().toString());
                newVersion.setCreateDate(new Date());
                newVersion.setDataVersion(dataVersion);
                newVersion.setUpdateDate(version.getCreateDate());
                newVersion.setYmdVersion(nowDay);
                newVersion.setVerVersion(1);

                logger.info("getDataVersion-009 获取数据版本号: FROM_SYS: " + itfSyncKey.getDataSys() + "_TO_SYS: " + itfSyncKey.getDockSys() + ", (new)获取当前版本号: " + newVersion.getDataVersion());

                auxDataVersionsMapper.insert(newVersion);
            }

            return dataVersion;

        } finally {
            redisLock.unlock(lockKey);
            logger.info("getDataVersion-010 获取数据版本号: FROM_SYS: " + itfSyncKey.getDataSys() + "_TO_SYS: " + itfSyncKey.getDockSys() + ", 释放 Redis 锁");
        }

    }

    /**
     * 更新同步后处理数据状态;
     *   如果 成功: 6
     *   如果 失败: 3
     * @param dataLogs
     */
    public void setSyncDataStatusSuccess(List<AuxReqSyncDataLogs> dataLogs) {
        for (AuxReqSyncDataLogs dataLog : dataLogs) {
            dataLog.setStatus(6);
        }

        setSyncDataStatus(dataLogs);
    }

    /**
     * 更新同步后处理数据状态;
     *   如果 成功: 6
     *   如果 失败: 3
     * @param dataLogs
     */
    public void setSyncDataStatusFailure(List<AuxReqSyncDataLogs> dataLogs) {
        for (AuxReqSyncDataLogs dataLog : dataLogs) {
            dataLog.setStatus(3);
        }

        setSyncDataStatus(dataLogs);
    }

    /**
     * 更新数据请求状态
     * @param dataLogs
     */
    private void setSyncDataStatus(List<AuxReqSyncDataLogs> dataLogs) {

        for (AuxReqSyncDataLogs dataLog : dataLogs) {
            dataLog.setUpdateDate(new Date());
            auxReqSyncDataLogsMapper.update(dataLog);

            AuxReqSyncDataLogsHis logsHis = new AuxReqSyncDataLogsHis();
            SpringBeanUtils.copyPropertiesIgnoreNull(dataLog, logsHis);

            logsHis.setMsgId(UUID.randomUUID().toString());
            logsHis.setCreateDate(new Date());
            logsHis.setUpdateDate(logsHis.getCreateDate());
            auxReqSyncDataLogsHisMapper.insert(logsHis);
        }
    }

    /**
     * 重新发送
     * @param paramsVO
     */
    public void syncReRequest(SyncParamsVO paramsVO) {

        // 生成版本号
        paramsVO.setDataVersion(getDataVersion(paramsVO.getSyncKey()));

        JSONObject params = paramsVO.getParams();
        params.put("version", paramsVO.getDataVersion());

        AuxReqSyncDataLogs dataLogs = new AuxReqSyncDataLogs();
        SpringBeanUtils.copyPropertiesIgnoreNull(paramsVO, dataLogs);
        dataLogs.setMsgId(UUID.randomUUID().toString());
        dataLogs.setStatus(0);
        dataLogs.setMsgKey("_RE_SEND");
        dataLogs.setCreateDate(new Date());
        dataLogs.setUpdateDate(new Date());

        dockCommonHandle.handle(paramsVO, dataLogs);
    }
}
