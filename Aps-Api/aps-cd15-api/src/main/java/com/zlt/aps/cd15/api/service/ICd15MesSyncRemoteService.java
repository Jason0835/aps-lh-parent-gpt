package com.zlt.aps.cd15.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.cd15.api.domain.entity.Cd15ShiftStock;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 斜裁MES同步远程服务。
 */
@FeignClient(contextId = "ICd15MesSyncRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE,
        path = "${api.path.cd15:/cd15}")
public interface ICd15MesSyncRemoteService {

    /** 替换斜裁自动滚动目标班次库存快照。 */
    @ApiOperation("替换斜裁自动滚动班次库存快照")
    @PostMapping("/cd15MesSync/replaceShiftStock")
    AjaxResult replaceShiftStock(@RequestParam("factoryCode") String factoryCode,
                                 @RequestParam("stockDate") String stockDate,
                                 @RequestParam("shiftCode") String shiftCode,
                                 @RequestParam("shiftStartTime") String shiftStartTime,
                                 @RequestParam("updateBy") String updateBy,
                                 @RequestBody List<Cd15ShiftStock> stockList);
}
