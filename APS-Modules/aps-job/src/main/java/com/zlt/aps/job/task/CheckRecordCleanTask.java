package com.zlt.aps.job.task;

import com.zlt.aps.mp.api.service.IMpCheckItemRecordRemoteService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class CheckRecordCleanTask {

    @Autowired
    private IMpCheckItemRecordRemoteService iMpCheckItemRecordRemoteService;

    // 每天凌晨 2 点执行一次
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanExpiredRecords() {
        log.info("开始清理过期的检测记录...");
        iMpCheckItemRecordRemoteService.clearInvalidData();
        log.info("过期的检测记录清理完成");
    }
}
