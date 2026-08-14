package com.zlt.aps.job.task;

import com.zlt.aps.autoLogin.feign.FeignTokenHelper;
import com.zlt.aps.autoLogin.loginUtils.annotation.AutoLoginLog;
import com.zlt.aps.itf.mes.IMesItfService;
import com.zlt.aps.itf.vo.AuxReqSyncDataLogs;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.HashMap;

/**
 * TM（胎面）MES接口定时任务。
 */
@Slf4j
@Component("tmMesTask")
@RequiredArgsConstructor
public class TmMesTask {

    private final IMesItfService mesItfService;

    /** 同步胎面库存。 */
    @ApiOperation("同步胎面库存")
    @AutoLoginLog
    public void syncMesTmStock() {
        this.syncMesTmStock(null);
    }

    /**
     * 按指定日期同步胎面库存。
     *
     * @param stockDate 指定库存日期，格式yyyy-MM-dd；为空时走原默认流程
     */
    @ApiOperation("按指定日期同步胎面库存")
    @AutoLoginLog
    public void syncMesTmStock(String stockDate) {
        FeignTokenHelper.runWithToken(() -> {
            AuxReqSyncDataLogs syncDataLogs = new AuxReqSyncDataLogs();
            if (StringUtils.isNotBlank(stockDate)) {
                HashMap<String, Object> queryParams = new HashMap<>();
                queryParams.put("stockDate", stockDate.trim());
                syncDataLogs.setQueryParams(queryParams);
            }
            this.mesItfService.syncTmStock(syncDataLogs);
        });
    }
}
