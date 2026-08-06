package com.zlt.aps.job.task;

import cn.hutool.core.date.DateUtil;
import com.zlt.aps.autoLogin.feign.FeignTokenHelper;
import com.zlt.aps.autoLogin.loginUtils.annotation.AutoLoginLog;
import com.zlt.aps.itf.mes.IMesItfService;
import com.zlt.aps.itf.vo.AuxReqSyncDataLogs;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
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
        FeignTokenHelper.runWithToken(() -> iMesItfService.syncMesCd90Stock(new AuxReqSyncDataLogs()));
    }

    /**
     * 同步直裁库排状态。
     */
    @ApiOperation("同步直裁库排状态")
    @AutoLoginLog
    public void syncStorageLaneLimit() {
        FeignTokenHelper.runWithToken(() ->
                iMesItfService.syncCd90StorageLaneLimit(new AuxReqSyncDataLogs()));
    }

    /**
     * 同步直裁每日三班完成量（从 MES 中间表 MES_CD90_CLASS_SHIFT_FINISH_QTY 同步到 t_cd90_sche_finish_qty 并回写排程结果）
     * 默认按上一天 scheduleDate 抓取：夜、早、中三班数据需等当天结束后才完整，定时任务凌晨执行
     */
    @ApiOperation("同步直裁每日三班完成量")
    @AutoLoginLog
    public void syncCd90ClassShiftFinishQty() {
        FeignTokenHelper.runWithToken(() -> {
            AuxReqSyncDataLogs syncDataLogs = new AuxReqSyncDataLogs();
            HashMap<String, Object> queryParams = new HashMap<>();
            queryParams.put("scheduleDate", DateUtil.format(DateUtil.offsetDay(new Date(), -1), "yyyy-MM-dd"));
            syncDataLogs.setQueryParams(queryParams);
            iMesItfService.syncCd90ClassShiftFinishQty(syncDataLogs);
        });
    }
}
