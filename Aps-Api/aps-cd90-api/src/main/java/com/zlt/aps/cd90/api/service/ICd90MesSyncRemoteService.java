package com.zlt.aps.cd90.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.cd90.api.domain.entity.Cd90ScheFinishQty;
import com.zlt.aps.cd90.api.domain.entity.Cd90ShiftStock;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 直裁MES同步远程服务。
 */
@FeignClient(contextId = "ICd90MesSyncRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE,
        path = "${api.path.cd90:/cd90}")
public interface ICd90MesSyncRemoteService {

    /**
     * 替换直裁自动滚动目标班次库存快照。
     *
     * @param factoryCode 工厂编码
     * @param stockDate 班次开始自然日期
     * @param shiftCode 物理班次编码
     * @param shiftStartTime 班次开始时间
     * @param updateBy 更新人
     * @param stockList 班次库存列表，空集合表示清空
     * @return 保存结果
     */
    @ApiOperation("替换直裁自动滚动班次库存快照")
    @PostMapping("/cd90MesSync/replaceShiftStock")
    AjaxResult replaceShiftStock(@RequestParam("factoryCode") String factoryCode,
                                 @RequestParam("stockDate") String stockDate,
                                 @RequestParam("shiftCode") String shiftCode,
                                 @RequestParam("shiftStartTime") String shiftStartTime,
                                 @RequestParam("updateBy") String updateBy,
                                 @RequestBody List<Cd90ShiftStock> stockList);

    /**
     * 逻辑删除并批量保存指定工厂、日期的直裁每日完成量。
     *
     * @param factoryCode 工厂编码
     * @param scheduleDate MES完成量归属日期，格式yyyy-MM-dd
     * @param updateBy 更新人
     * @param finishQtyList 每日完成量列表
     * @return 保存结果
     */
    @ApiOperation("逻辑删除并批量保存直裁每日完成量")
    @PostMapping("/cd90MesSync/logicDeleteAndSaveScheFinishQty")
    AjaxResult logicDeleteAndSaveScheFinishQty(@RequestParam("factoryCode") String factoryCode,
                                                @RequestParam("scheduleDate") String scheduleDate,
                                                @RequestParam("updateBy") String updateBy,
                                                @RequestBody List<Cd90ScheFinishQty> finishQtyList);

    /**
     * 将直裁每日完成量回写到排程结果对应班次。
     *
     * @param finishQtyList 每日完成量列表
     * @return 回写结果
     */
    @ApiOperation("直裁每日完成量回写排程结果")
    @PostMapping("/cd90MesSync/writeBackScheduleResultFinishQty")
    AjaxResult writeBackScheduleResultFinishQty(@RequestBody List<Cd90ScheFinishQty> finishQtyList);
}
