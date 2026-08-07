package com.zlt.aps.cd15.controller;

import cn.hutool.core.date.DateUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.cd15.api.domain.entity.Cd15ShiftStock;
import com.zlt.aps.cd15.api.service.ICd15MesSyncRemoteService;
import com.zlt.aps.cd15.service.ICd15ShiftStockService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 斜裁MES同步控制器。
 */
@Api(tags = "斜裁MES同步")
@RestController
@RequiredArgsConstructor
public class Cd15MesSyncController implements ICd15MesSyncRemoteService {

    private final ICd15ShiftStockService cd15ShiftStockService;

    @Override
    @ApiOperation("替换斜裁自动滚动班次库存快照")
    @PostMapping("/cd15MesSync/replaceShiftStock")
    public AjaxResult replaceShiftStock(@RequestParam("factoryCode") String factoryCode,
                                        @RequestParam("stockDate") String stockDate,
                                        @RequestParam("shiftCode") String shiftCode,
                                        @RequestParam("shiftStartTime") String shiftStartTime,
                                        @RequestParam("updateBy") String updateBy,
                                        @RequestBody List<Cd15ShiftStock> stockList) {
        this.cd15ShiftStockService.replaceShiftStock(factoryCode, DateUtil.parseDate(stockDate),
                shiftCode, DateUtil.parseDateTime(shiftStartTime), updateBy, stockList);
        return AjaxResult.success();
    }
}
