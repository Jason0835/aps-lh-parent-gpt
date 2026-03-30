package com.zlt.aps.common;

import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.core.utils.RedisLock;
import com.zlt.aps.domain.SyncDataLogs;
import com.zlt.aps.sync.mapper.SyncDataLogsMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * @author Chen
 * @date 2025/4/27
 */
@Service
@Slf4j
public class SyncDataLogsService {

    @Autowired
    private SyncDataLogsMapper syncDataLogsMapper;

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 反馈超时时间（秒）
     */
    @Value("${syncdata.feedback.timeOut:60}")
    private Integer feedbackTimeOut;

    /**
     * 防重复发布锁定时间（毫秒）
     */
    @Value("${syncdata.publish.lockTime:1000}")
    private Integer publishLockTime;

    /**
     * 检查开关
     */
    @Value("${syncdata.publish.checkSyncResult:0}")
    private String checkSyncResult;

    /**
     * 几秒钟后
     *
     * @param seconds
     * @return
     */
    public static Date secondLater(Integer seconds) {
        Calendar calendar = Calendar.getInstance(Locale.SIMPLIFIED_CHINESE);
        calendar.add(Calendar.SECOND, seconds);

        return calendar.getTime();
    }

    /**
     * 获取同步日志的反馈状态
     *
     * @param dataVersion 数据版本
     * @return
     */
    public SyncDataLogs getSyncDataResult(String dataVersion) {
        if ("0".equals(checkSyncResult)) { // 是否检查接口返回状态，不检查则直接返回
            SyncDataLogs logs = new SyncDataLogs();
            logs.setStatus(ApsConstant.IS_RELEASE);
            return logs;
        }
        // 扫描截止时间：30秒后
        Date endTime = secondLater(feedbackTimeOut);
        while (true) {
            SyncDataLogs logs = syncDataLogsMapper.getSyncDataLogs(dataVersion);
            SyncDataLogs resultLog = this.checkLogStatus(dataVersion, logs, endTime);
            if (resultLog != null) {
                return resultLog;
            }
        }
    }

    /**
     * 检查日志状态
     *
     * @param dataVersion
     * @param logs
     * @param endTime
     * @return
     */
    private SyncDataLogs checkLogStatus(String dataVersion, SyncDataLogs logs, Date endTime) {
        if (logs != null) {
            if (ApsConstant.IS_RELEASE.equals(logs.getStatus())
                    || ApsConstant.FAILURE_RELEASE.equals(logs.getStatus())) {
                // 异常情况，需要处理异常信息
                if (ApsConstant.FAILURE_RELEASE.equals(logs.getStatus())) {
                    String msg = I18nUtil.getMessage("ui.common.column.schuedule.publish.error") + logs.getMsg();
                    logs.setMsg(msg);
                }
                // 成功或者失败，状态确定，因此返回结果
                return logs;
            }
        }
        Date currentTime = new Date();
        if (currentTime.compareTo(endTime) > 0) {
            // 超时，直接返回超时状态
            logs = new SyncDataLogs();
            logs.setDataVersion(dataVersion);
            logs.setMsg(I18nUtil.getMessage("ui.common.column.schuedule.publish.timeOut"));
            logs.setStatus(ApsConstant.TIMEOUT_FAILURE);
            return logs;
        }
        // 等待3秒后重新扫描
        try {
            Thread.sleep(3000L);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    /**
     * 获取请求日志的反馈状态
     *
     * @param dataVersion 数据版本
     * @return
     */
    public SyncDataLogs getReqDataResult(String dataVersion) {
        // 扫描截止时间：30秒后
        Date endTime = secondLater(feedbackTimeOut);
        while (true) {
            SyncDataLogs logs = syncDataLogsMapper.getReqDataLogs(dataVersion);
            SyncDataLogs resultLog = this.checkLogStatus(dataVersion, logs, endTime);
            if (resultLog != null) {
                return resultLog;
            }
        }
    }

    /**
     * 检查待发布排程记录是否已被锁定
     *
     * @param lockKey    锁key
     * @param publishIds 待发布记录ID
     * @return
     */
    public boolean checkPublishLocking(String lockKey, Long[] publishIds) {
        if (publishIds == null || publishIds.length == 0) {
            // 如果传入的记录ID为空，则相当于没锁定，由调用服务自行处理
            return false;
        }
        // 将ID拼接到key上，作为redis锁的key
        StringBuffer lockKeyBuffer = new StringBuffer(lockKey);
        lockKeyBuffer.append(":").append(StringUtils.join(publishIds));
        RedisLock redisLock = new RedisLock(redisTemplate, lockKeyBuffer.toString(), publishLockTime);
        return !redisLock.lock();
    }
}
