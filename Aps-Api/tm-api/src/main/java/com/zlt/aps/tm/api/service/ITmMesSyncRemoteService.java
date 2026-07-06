package com.zlt.aps.tm.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.tm.api.domain.entity.TmDayFinishQty;
import com.zlt.aps.tm.api.domain.entity.TmScheFinishQty;
import com.zlt.aps.tm.api.domain.entity.TmStock;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 胎面库存MES同步远程服务接口
 *
 * @author APS Team
 */
@FeignClient(contextId = "ITmMesSyncRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.tm:tm}")
public interface ITmMesSyncRemoteService {

    /**
     * 逻辑删除并批量保存胎面库存（事务性操作）
     * 步骤1：逻辑删除指定库存日期的旧数据（IS_DELETE置为1）
     * 步骤2：批量插入MES最新库存数据（新记录，IS_DELETE=0）
     *
     * @param stockDate 库存日期，格式：yyyy-MM-dd
     * @param updateBy  更新者
     * @param list      待插入的胎面库存列表
     * @return 结果
     */
    @ApiOperation("逻辑删除并批量保存胎面库存（事务性操作）")
    @PostMapping("/tmMesSync/logicDeleteAndSaveTmStockByStockDate")
    AjaxResult logicDeleteAndSaveTmStockByStockDate(@RequestParam("stockDate") String stockDate,
                                                     @RequestParam("updateBy") String updateBy,
                                                     @RequestBody List<TmStock> list);

    /**
     * 逻辑删除并批量保存胎面排程完成量（事务性操作）
     * 步骤1：逻辑删除指定分厂+排程日期的旧数据（IS_DELETE置为1）
     * 步骤2：批量插入MES最新排程完成量数据（新记录，IS_DELETE=0）
     *
     * @param factoryCode  分厂编号
     * @param scheduleDate 排程日期，格式：yyyy-MM-dd
     * @param updateBy     更新者
     * @param list         待插入的胎面排程完成量列表
     * @return 结果
     */
    @ApiOperation("逻辑删除并批量保存胎面排程完成量（事务性操作）")
    @PostMapping("/tmMesSync/logicDeleteAndSaveScheFinishQty")
    AjaxResult logicDeleteAndSaveScheFinishQty(@RequestParam("factoryCode") String factoryCode,
                                                @RequestParam("scheduleDate") String scheduleDate,
                                                @RequestParam("updateBy") String updateBy,
                                                @RequestBody List<TmScheFinishQty> list);

    /**
     * 胎面排程完成量回写胎面排程结果表各班次完成量
     * 根据完成量回报数据，按胎面代码+工单号+排程日期汇总后，
     * 查询排程结果表（排程日期为D-1、D、D+1）并按班次映射关系回写完成量
     *
     * @param list 完成量回报数据列表
     * @return 回写结果
     */
    @ApiOperation("胎面排程完成量回写胎面排程结果表各班次完成量")
    @PostMapping("/tmMesSync/writeBackScheduleResultFinishQty")
    AjaxResult writeBackScheduleResultFinishQty(@RequestBody List<TmScheFinishQty> list);

    /**
     * 逻辑删除并批量保存胎面排程日完成量（事务性操作）
     * 步骤1：逻辑删除指定分厂+排程日期的旧数据（IS_DELETE置为1）
     * 步骤2：批量插入MES最新排程日完成量数据（新记录，IS_DELETE=0）
     *
     * @param factoryCode  分厂编号
     * @param scheduleDate 排程日期，格式：yyyy-MM-dd
     * @param updateBy     更新者
     * @param list         待插入的胎面排程日完成量列表
     * @return 结果
     */
    @ApiOperation("逻辑删除并批量保存胎面排程日完成量（事务性操作）")
    @PostMapping("/tmMesSync/logicDeleteAndSaveDayFinishQty")
    AjaxResult logicDeleteAndSaveDayFinishQty(@RequestParam("factoryCode") String factoryCode,
                                               @RequestParam("scheduleDate") String scheduleDate,
                                               @RequestParam("updateBy") String updateBy,
                                               @RequestBody List<TmDayFinishQty> list);
}
