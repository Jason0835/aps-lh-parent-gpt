package com.zlt.aps.job.task;

import cn.hutool.core.date.DateUtil;
import com.zlt.aps.autoLogin.feign.FeignTokenHelper;
import com.zlt.aps.autoLogin.loginUtils.annotation.AutoLoginLog;
import com.zlt.aps.itf.mes.IMesItfService;
import com.zlt.aps.itf.vo.AuxReqSyncDataLogs;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;

/**
 * CD90（直裁）MES接口定时任务
 *
 * @author xueh
 * @since 2026/08/05
 */
@Slf4j
@Component("cd90MesTask")
public class Cd90MesTask {

    @Autowired
    private IMesItfService iMesItfService;

    /**
     * 同步直裁库存（从 MES 中间表 T_MES_CD90_STOCK 同步到 t_cd90_stock）
     */
    @ApiOperation("同步直裁库存")
    @AutoLoginLog
    public void syncMesCd90Stock() {
        this.syncMesCd90Stock(null);
    }

    /**
     * 按指定日期同步直裁库存。
     *
     * @param stockDate 指定库存日期，格式yyyy-MM-dd；为空时走原默认流程
     */
    @ApiOperation("按指定日期同步直裁库存")
    @AutoLoginLog
    public void syncMesCd90Stock(String stockDate) {
        FeignTokenHelper.runWithToken(() ->
                this.iMesItfService.syncMesCd90Stock(
                        this.buildDateRequest("stockDate", stockDate)));
    }

    /**
     * 同步直裁库排状态。
     */
    @ApiOperation("同步直裁库排状态")
    @AutoLoginLog
    public void syncStorageLaneLimit() {
        this.syncStorageLaneLimit(null);
    }

    /**
     * 按指定日期同步直裁库排状态。
     *
     * @param laneDate 指定库排日期，格式yyyy-MM-dd；为空时走原默认流程
     */
    @ApiOperation("按指定日期同步直裁库排状态")
    @AutoLoginLog
    public void syncStorageLaneLimit(String laneDate) {
        FeignTokenHelper.runWithToken(() ->
                this.iMesItfService.syncCd90StorageLaneLimit(
                        this.buildDateRequest("laneDate", laneDate)));
    }

    /**
     * 同步直裁每日三班完成量（从 MES 中间表 MES_CD90_CLASS_SHIFT_FINISH_QTY 同步到 t_cd90_sche_finish_qty 并回写排程结果）
     * 默认按上一天 scheduleDate 抓取：夜、早、中三班数据需等当天结束后才完整，定时任务凌晨执行
     */
    @ApiOperation("同步直裁每日三班完成量")
    @AutoLoginLog
    public void syncCd90ClassShiftFinishQty() {
        this.syncCd90ClassShiftFinishQty(null);
    }

    /**
     * 同步直裁每日三班完成量。
     *
     * @param scheduleDate 指定同步日期，格式yyyy-MM-dd；为空时默认同步上一天
     */
    @ApiOperation("按指定日期同步直裁每日三班完成量")
    @AutoLoginLog
    public void syncCd90ClassShiftFinishQty(String scheduleDate) {
        FeignTokenHelper.runWithToken(() -> {
            AuxReqSyncDataLogs syncDataLogs = new AuxReqSyncDataLogs();
            HashMap<String, Object> queryParams = new HashMap<>();
            String targetScheduleDate = StringUtils.isBlank(scheduleDate)
                    ? DateUtil.format(DateUtil.offsetDay(new Date(), -1), "yyyy-MM-dd")
                    : scheduleDate.trim();
            queryParams.put("scheduleDate", targetScheduleDate);
            syncDataLogs.setQueryParams(queryParams);
            this.iMesItfService.syncCd90ClassShiftFinishQty(syncDataLogs);
        });
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
