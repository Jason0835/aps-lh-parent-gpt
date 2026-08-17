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
 * CD15（斜裁）MES接口定时任务。
 */
@Slf4j
@Component("cd15MesTask")
@RequiredArgsConstructor
public class Cd15MesTask {

    private final IMesItfService mesItfService;

    /** 从MES_CD15_STOCK同步斜裁库存，工厂固定116。 */
    @ApiOperation("同步斜裁库存")
    @AutoLoginLog
    public void syncMesCd15Stock() {
        this.syncMesCd15Stock(null);
    }

    /**
     * 按指定日期同步斜裁库存。
     *
     * @param stockDate 指定库存日期，格式yyyy-MM-dd；为空时走原默认流程
     */
    @ApiOperation("按指定日期同步斜裁库存")
    @AutoLoginLog
    public void syncMesCd15Stock(String stockDate) {
        FeignTokenHelper.runWithToken(() ->
                this.mesItfService.syncMesCd15Stock(
                        this.buildDateRequest("stockDate", stockDate)));
    }

    /** 同步斜裁库排状态，保留为人工补偿入口。 */
    @ApiOperation("同步斜裁库排状态")
    @AutoLoginLog
    public void syncStorageLaneLimit() {
        this.syncStorageLaneLimit(null);
    }

    /**
     * 按指定日期同步斜裁库排状态。
     *
     * @param laneDate 指定库排日期，格式yyyy-MM-dd；为空时走原默认流程
     */
    @ApiOperation("按指定日期同步斜裁库排状态")
    @AutoLoginLog
    public void syncStorageLaneLimit(String laneDate) {
        FeignTokenHelper.runWithToken(() ->
                this.mesItfService.syncCd15StorageLaneLimit(
                        this.buildDateRequest("laneDate", laneDate)));
    }

    /**
     * 构造可选日期同步请求；未指定日期时保持原空请求。
     *
     * @param parameterName 日期参数名
     * @param parameterValue 日期参数值
     * @return MES同步请求
     */
    private AuxReqSyncDataLogs buildDateRequest(
            String parameterName, String parameterValue) {
        AuxReqSyncDataLogs syncDataLogs = new AuxReqSyncDataLogs();
        if (StringUtils.isNotBlank(parameterValue)) {
            HashMap<String, Object> queryParams = new HashMap<>();
            queryParams.put(parameterName, parameterValue.trim());
            syncDataLogs.setQueryParams(queryParams);
        }
        return syncDataLogs;
    }
}
