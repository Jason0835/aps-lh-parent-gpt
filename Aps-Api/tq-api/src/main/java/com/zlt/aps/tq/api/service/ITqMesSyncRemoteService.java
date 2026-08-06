package com.zlt.aps.tq.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.tq.api.domain.entity.TqDayFinishQty;
import com.zlt.aps.tq.api.domain.entity.TqScheFinishQty;
import com.zlt.aps.tq.api.domain.entity.TqShiftStock;
import com.zlt.aps.tq.api.domain.entity.TqStock;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 胎圈库存MES同步远程服务接口
 *
 * @author APS Team
 */
@FeignClient(contextId = "ITqMesSyncRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.tq:tq}")
public interface ITqMesSyncRemoteService {

    /**
     * 逻辑删除并批量保存胎圈库存（事务性操作）
     * 步骤1：逻辑删除指定库存日期的旧数据（IS_DELETE置为1）
     * 步骤2：批量插入MES最新库存数据（新记录，IS_DELETE=0）
     *
     * @param stockDate 库存日期，格式：yyyy-MM-dd
     * @param updateBy  更新者
     * @param list      待插入的胎圈库存列表
     * @return 结果
     */
    @ApiOperation("逻辑删除并批量保存胎圈库存（事务性操作）")
    @PostMapping("/tqMesSync/logicDeleteAndSaveTqStockByStockDate")
    AjaxResult logicDeleteAndSaveTqStockByStockDate(@RequestParam("stockDate") String stockDate,
                                                     @RequestParam("updateBy") String updateBy,
                                                     @RequestBody List<TqStock> list);

    /**
     * 替换胎圈自动滚动班次库存快照。
     *
     * <p>对齐胎面 ITmMesSyncRemoteService.replaceShiftStock，
     * 由 aps-itf 的 syncBeadShiftStock 远程调用，先逻辑删除指定工厂+物理日+班序的旧快照，
     * 再批量插入新快照。</p>
     *
     * @param factoryCode 工厂编码
     * @param stockDate MES库存物理日期，格式：yyyy-MM-dd
     * @param shiftOrder 班次顺序（1~6）
     * @param updateBy 更新人
     * @param stockList 班次库存列表，空集合表示清空快照
     * @return 保存结果
     */
    @ApiOperation("替换胎圈自动滚动班次库存快照")
    @PostMapping("/tqMesSync/replaceShiftStock")
    AjaxResult replaceShiftStock(@RequestParam("factoryCode") String factoryCode,
                                  @RequestParam("stockDate") String stockDate,
                                  @RequestParam("shiftOrder") Integer shiftOrder,
                                  @RequestParam("updateBy") String updateBy,
                                  @RequestBody List<TqShiftStock> stockList);

    /**
     * 逻辑删除并批量保存胎圈排程完成量（事务性操作）
     * 步骤1：逻辑删除指定分厂+排程日期的旧数据（IS_DELETE置为1）
     * 步骤2：批量插入MES最新排程完成量数据（新记录，IS_DELETE=0）
     *
     * @param factoryCode  分厂编号
     * @param scheduleDate 排程日期，格式：yyyy-MM-dd
     * @param updateBy     更新者
     * @param list         待插入的胎圈排程完成量列表
     * @return 结果
     */
    @ApiOperation("逻辑删除并批量保存胎圈排程完成量（事务性操作）")
    @PostMapping("/tqMesSync/logicDeleteAndSaveScheFinishQty")
    AjaxResult logicDeleteAndSaveScheFinishQty(@RequestParam("factoryCode") String factoryCode,
                                                @RequestParam("scheduleDate") String scheduleDate,
                                                @RequestParam("updateBy") String updateBy,
                                                @RequestBody List<TqScheFinishQty> list);

    /**
     * 胎圈排程完成量回写胎圈排程结果表各班次完成量
     * 根据完成量回报数据，按胎圈代码+工单号+排程日期汇总后，
     * 查询排程结果表（排程日期为D-1、D）并按班次映射关系回写完成量
     *
     * @param list 完成量回报数据列表
     * @return 回写结果
     */
    @ApiOperation("胎圈排程完成量回写胎圈排程结果表各班次完成量")
    @PostMapping("/tqMesSync/writeBackScheduleResultFinishQty")
    AjaxResult writeBackScheduleResultFinishQty(@RequestBody List<TqScheFinishQty> list);

    /**
     * 逻辑删除并批量保存胎圈排程日完成量（事务性操作）
     * 步骤1：逻辑删除指定分厂+排程日期的旧数据（IS_DELETE置为1）
     * 步骤2：批量插入MES最新排程日完成量数据（新记录，IS_DELETE=0）
     *
     * @param factoryCode  分厂编号
     * @param scheduleDate 排程日期，格式：yyyy-MM-dd
     * @param updateBy     更新者
     * @param list         待插入的胎圈排程日完成量列表
     * @return 结果
     */
    @ApiOperation("逻辑删除并批量保存胎圈排程日完成量（事务性操作）")
    @PostMapping("/tqMesSync/logicDeleteAndSaveDayFinishQty")
    AjaxResult logicDeleteAndSaveDayFinishQty(@RequestParam("factoryCode") String factoryCode,
                                               @RequestParam("scheduleDate") String scheduleDate,
                                               @RequestParam("updateBy") String updateBy,
                                               @RequestBody List<TqDayFinishQty> list);
}
