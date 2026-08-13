package com.zlt.aps.cd90.controller;

import cn.hutool.core.date.DateUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.cd90.api.domain.entity.Cd90ScheFinishQty;
import com.zlt.aps.cd90.api.domain.entity.Cd90ShiftStock;
import com.zlt.aps.cd90.api.service.ICd90MesSyncRemoteService;
import com.zlt.aps.cd90.service.ICd90ScheFinishQtyService;
import com.zlt.aps.cd90.service.ICd90ShiftStockService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 直裁MES同步控制器。
 */
@Api(tags = "直裁MES同步")
@RestController
@RequiredArgsConstructor
public class Cd90MesSyncController implements ICd90MesSyncRemoteService {

    private final ICd90ScheFinishQtyService cd90ScheFinishQtyService;
    private final ICd90ShiftStockService cd90ShiftStockService;

    /**
     * 替换直裁自动滚动目标班次库存快照。
     */
    @Override
    @ApiOperation("替换直裁自动滚动班次库存快照")
    @PostMapping("/cd90MesSync/replaceShiftStock")
    public AjaxResult replaceShiftStock(@RequestParam("factoryCode") String factoryCode,
                                        @RequestParam("stockDate") String stockDate,
                                        @RequestParam("shiftCode") String shiftCode,
                                        @RequestParam("shiftStartTime") String shiftStartTime,
                                        @RequestParam("updateBy") String updateBy,
                                        @RequestBody List<Cd90ShiftStock> stockList) {
        this.cd90ShiftStockService.replaceShiftStock(factoryCode, DateUtil.parseDate(stockDate),
                shiftCode, DateUtil.parseDateTime(shiftStartTime), updateBy, stockList);
        return AjaxResult.success();
    }

    /**
     * 替换指定工厂、日期的直裁每日完成量。
     */
    @Override
    @ApiOperation("逻辑删除并批量保存直裁每日完成量")
    @PostMapping("/cd90MesSync/logicDeleteAndSaveScheFinishQty")
    public AjaxResult logicDeleteAndSaveScheFinishQty(@RequestParam("factoryCode") String factoryCode,
                                                       @RequestParam("scheduleDate") String scheduleDate,
                                                       @RequestParam("updateBy") String updateBy,
                                                       @RequestBody List<Cd90ScheFinishQty> finishQtyList) {
        this.cd90ScheFinishQtyService.logicDeleteAndSaveBatch(factoryCode, DateUtil.parseDate(scheduleDate),
                updateBy, finishQtyList);
        return AjaxResult.success();
    }

    /**
     * 将直裁每日完成量回写到排程结果。
     */
    @Override
    @ApiOperation("直裁每日完成量回写排程结果")
    @PostMapping("/cd90MesSync/writeBackScheduleResultFinishQty")
    public AjaxResult writeBackScheduleResultFinishQty(@RequestBody List<Cd90ScheFinishQty> finishQtyList) {
        return this.cd90ScheFinishQtyService.writeBackScheduleResultFinishQty(finishQtyList);
    }
}
