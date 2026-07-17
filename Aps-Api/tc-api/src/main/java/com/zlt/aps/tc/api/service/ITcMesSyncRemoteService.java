package com.zlt.aps.tc.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.tc.api.domain.entity.TcDayFinishQty;
import com.zlt.aps.tc.api.domain.entity.TcScheFinishQty;
import com.zlt.aps.tc.api.domain.entity.TcStock;
import com.zlt.aps.tc.api.domain.vo.TcReleaseFeedbackVo;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 胎侧MES数据回写远程服务。
 */
@FeignClient(contextId = "ITcMesSyncRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE,
        path = "${api.path.tc:/tc}")
public interface ITcMesSyncRemoteService {

    /**
     * 失效并保存胎侧库存快照。
     *
     * @param factoryCode 工厂编码
     * @param stockDate 库存日期
     * @param updateBy 更新人
     * @param stockList 库存列表
     * @return 保存结果
     */
    @ApiOperation("失效并保存胎侧库存快照")
    @PostMapping("/tcMesSync/logicDeleteAndSaveStock")
    AjaxResult logicDeleteAndSaveStock(@RequestParam("factoryCode") String factoryCode,
                                       @RequestParam("stockDate") String stockDate,
                                       @RequestParam("updateBy") String updateBy,
                                       @RequestBody List<TcStock> stockList);

    /**
     * 失效并保存胎侧班次完成量。
     *
     * @param factoryCode 工厂编码
     * @param scheduleDate MES业务日期
     * @param updateBy 更新人
     * @param finishQtyList 完成量列表
     * @return 保存结果
     */
    @ApiOperation("失效并保存胎侧班次完成量")
    @PostMapping("/tcMesSync/logicDeleteAndSaveScheFinishQty")
    AjaxResult logicDeleteAndSaveScheFinishQty(@RequestParam("factoryCode") String factoryCode,
                                                @RequestParam("scheduleDate") String scheduleDate,
                                                @RequestParam("updateBy") String updateBy,
                                                @RequestBody List<TcScheFinishQty> finishQtyList);

    /**
     * 将MES班次完成量回写六班排程结果。
     *
     * @param finishQtyList 完成量列表
     * @return 回写结果
     */
    @ApiOperation("回写胎侧排程结果完成量")
    @PostMapping("/tcMesSync/writeBackScheduleResultFinishQty")
    AjaxResult writeBackScheduleResultFinishQty(@RequestBody List<TcScheFinishQty> finishQtyList);

    /**
     * 失效并保存胎侧日完成量。
     *
     * @param factoryCode 工厂编码
     * @param scheduleDate MES业务日期
     * @param updateBy 更新人
     * @param finishQtyList 日完成量列表
     * @return 保存结果
     */
    @ApiOperation("失效并保存胎侧日完成量")
    @PostMapping("/tcMesSync/logicDeleteAndSaveDayFinishQty")
    AjaxResult logicDeleteAndSaveDayFinishQty(@RequestParam("factoryCode") String factoryCode,
                                               @RequestParam("scheduleDate") String scheduleDate,
                                               @RequestParam("updateBy") String updateBy,
                                               @RequestBody List<TcDayFinishQty> finishQtyList);

    /**
     * 接收MES发布反馈。
     *
     * @param feedback 发布反馈
     * @return 处理结果
     */
    @ApiOperation("接收胎侧发布MES反馈")
    @PostMapping("/tcMesSync/releaseFeedback")
    AjaxResult releaseFeedback(@RequestBody TcReleaseFeedbackVo feedback);
}
