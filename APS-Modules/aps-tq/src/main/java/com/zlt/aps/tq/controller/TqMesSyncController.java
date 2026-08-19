package com.zlt.aps.tq.controller;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.tq.api.domain.entity.TqDayFinishQty;
import com.zlt.aps.tq.api.domain.entity.TqScheFinishQty;
import com.zlt.aps.tq.api.domain.entity.TqShiftStock;
import com.zlt.aps.tq.api.domain.entity.TqStock;
import com.zlt.aps.tq.api.service.ITqMesSyncRemoteService;
import com.zlt.aps.tq.service.ITqDayFinishQtyService;
import com.zlt.aps.tq.service.ITqScheFinishQtyService;
import com.zlt.aps.tq.service.ITqShiftStockService;
import com.zlt.aps.tq.service.ITqStockService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cn.hutool.core.date.DateUtil;

import java.util.Date;
import java.util.List;

/**
 * 胎圈MES同步控制器
 * 实现ITqMesSyncRemoteService Feign接口，供ITF模块远程调用
 *
 * @author APS Team
 */
@Slf4j
@Api(tags = "胎圈MES同步")
@RestController
public class TqMesSyncController implements ITqMesSyncRemoteService {

    @Autowired
    private ITqStockService tqStockService;

    @Autowired
    private ITqShiftStockService tqShiftStockService;

    @Autowired
    private ITqScheFinishQtyService tqScheFinishQtyService;

    @Autowired
    private ITqDayFinishQtyService tqDayFinishQtyService;

    /**
     * 逻辑删除并批量保存胎圈库存（事务性操作）
     * 步骤1：逻辑删除指定库存日期的旧数据（IS_DELETE置为1）
     * 步骤2：批量插入MES最新库存数据（新记录，IS_DELETE=0）
     * 历史数据保留，只删当天库存日期的数据
     * 日期字符串由Service在目标JVM时区解析为Date，彻底规避跨时区日期偏移
     *
     * @param stockDate 库存日期，格式：yyyy-MM-dd（字符串形式传输，避免Feign Jackson序列化Date偏移）
     * @param updateBy  更新者
     * @param list      待插入的胎圈库存列表（stockDate未设置，由Service统一回填）
     * @return 结果
     */
    @Override
    @ApiOperation("逻辑删除并批量保存胎圈库存（事务性操作）")
    @PostMapping("/tqMesSync/logicDeleteAndSaveTqStockByStockDate")
    public AjaxResult logicDeleteAndSaveTqStockByStockDate(@RequestParam("stockDate") String stockDate,
                                                            @RequestParam("updateBy") String updateBy,
                                                            @RequestBody List<TqStock> list) {
        tqStockService.logicDeleteAndSaveBatch(stockDate, updateBy, list);
        return AjaxResult.success();
    }

    /**
     * 替换胎圈自动滚动班次库存快照。
     *
     * <p>对齐胎面 TmMesSyncController.replaceShiftStock，
     * 实现 ITqMesSyncRemoteService.replaceShiftStock Feign 接口，
     * 委托 ITqShiftStockService 完成先逻辑删除旧快照、再批量插入新快照的事务性操作。</p>
     *
     * @param factoryCode 工厂编码
     * @param stockDate MES库存物理日期，格式：yyyy-MM-dd
     * @param shiftOrder 班次顺序（1~6）
     * @param updateBy 更新人
     * @param stockList 班次库存列表，空集合表示清空快照
     * @return 保存结果
     */
    @Override
    @ApiOperation("替换胎圈自动滚动班次库存快照")
    @PostMapping("/tqMesSync/replaceShiftStock")
    public AjaxResult replaceShiftStock(@RequestParam("factoryCode") String factoryCode,
                                         @RequestParam("stockDate") String stockDate,
                                         @RequestParam("shiftOrder") Integer shiftOrder,
                                         @RequestParam("updateBy") String updateBy,
                                         @RequestBody List<TqShiftStock> stockList) {
        this.tqShiftStockService.replaceShiftStock(factoryCode, DateUtil.parseDate(stockDate),
                shiftOrder, updateBy, stockList);
        return AjaxResult.success();
    }

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
    @Override
    @ApiOperation("逻辑删除并批量保存胎圈排程完成量（事务性操作）")
    @PostMapping("/tqMesSync/logicDeleteAndSaveScheFinishQty")
    public AjaxResult logicDeleteAndSaveScheFinishQty(@RequestParam("factoryCode") String factoryCode,
                                                       @RequestParam("scheduleDate") String scheduleDate,
                                                       @RequestParam("updateBy") String updateBy,
                                                       @RequestBody List<TqScheFinishQty> list) {
        Date date = DateUtil.parse(scheduleDate);
        tqScheFinishQtyService.logicDeleteAndSaveBatch(factoryCode, date, updateBy, list);
        return AjaxResult.success();
    }

    /**
     * 胎圈排程完成量回写胎圈排程结果表各班次完成量
     * 根据完成量回报数据，按胎圈代码+工单号+排程日期汇总后，
     * 查询排程结果表（排程日期为D-1、D）并按班次映射关系回写完成量
     *
     * @param list 完成量回报数据列表
     * @return 回写结果
     */
    @Override
    @ApiOperation("胎圈排程完成量回写胎圈排程结果表各班次完成量")
    @PostMapping("/tqMesSync/writeBackScheduleResultFinishQty")
    public AjaxResult writeBackScheduleResultFinishQty(@RequestBody List<TqScheFinishQty> list) {
        return tqScheFinishQtyService.writeBackScheduleResultFinishQty(list);
    }

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
    @Override
    @ApiOperation("逻辑删除并批量保存胎圈排程日完成量（事务性操作）")
    @PostMapping("/tqMesSync/logicDeleteAndSaveDayFinishQty")
    public AjaxResult logicDeleteAndSaveDayFinishQty(@RequestParam("factoryCode") String factoryCode,
                                                      @RequestParam("scheduleDate") String scheduleDate,
                                                      @RequestParam("updateBy") String updateBy,
                                                      @RequestBody List<TqDayFinishQty> list) {
        Date date = DateUtil.parse(scheduleDate);
        tqDayFinishQtyService.logicDeleteAndSaveBatch(factoryCode, date, updateBy, list);
        return AjaxResult.success();
    }
}
