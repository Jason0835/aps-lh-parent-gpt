package com.zlt.aps.tm.controller;

import cn.hutool.core.date.DateUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.tm.api.domain.entity.TmDayFinishQty;
import com.zlt.aps.tm.api.domain.entity.TmScheFinishQty;
import com.zlt.aps.tm.api.domain.entity.TmShiftStock;
import com.zlt.aps.tm.api.domain.entity.TmStock;
import com.zlt.aps.tm.api.service.ITmMesSyncRemoteService;
import com.zlt.aps.tm.service.ITmDayFinishQtyService;
import com.zlt.aps.tm.service.ITmScheFinishQtyService;
import com.zlt.aps.tm.service.ITmShiftStockService;
import com.zlt.aps.tm.service.ITmStockService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.List;

/**
 * 胎面MES同步控制器
 * 实现ITmMesSyncRemoteService Feign接口，供ITF模块远程调用
 *
 * @author APS Team
 */
@Slf4j
@Api(tags = "胎面MES同步")
@RestController
public class TmMesSyncController implements ITmMesSyncRemoteService {

    @Autowired
    private ITmStockService tmStockService;

    @Autowired
    private ITmShiftStockService tmShiftStockService;

    @Autowired
    private ITmScheFinishQtyService tmScheFinishQtyService;

    @Autowired
    private ITmDayFinishQtyService tmDayFinishQtyService;

    /**
     * 逻辑删除并批量保存胎面库存（事务性操作）
     * 步骤1：逻辑删除指定库存日期的旧数据（IS_DELETE置为1）
     * 步骤2：批量插入MES最新库存数据（新记录，IS_DELETE=0）
     * 历史数据保留，只删当天库存日期的数据
     *
     * @param stockDate 库存日期，格式：yyyy-MM-dd
     * @param updateBy  更新者
     * @param list      待插入的胎面库存列表
     * @return 结果
     */
    @Override
    @ApiOperation("逻辑删除并批量保存胎面库存（事务性操作）")
    @PostMapping("/tmMesSync/logicDeleteAndSaveTmStockByStockDate")
    public AjaxResult logicDeleteAndSaveTmStockByStockDate(@RequestParam("stockDate") String stockDate,
                                                            @RequestParam("updateBy") String updateBy,
                                                            @RequestBody List<TmStock> list) {
        Date date = DateUtil.parse(stockDate);
        tmStockService.logicDeleteAndSaveBatch(date, updateBy, list);
        return AjaxResult.success();
    }

    /**
     * 替换指定工厂和库存日期的胎面库存快照。
     *
     * @param factoryCode 工厂编码
     * @param stockDate 库存日期
     * @param updateBy 更新人
     * @param stockList 库存列表，空集合表示清空快照
     * @return 保存结果
     */
    @Override
    @ApiOperation("替换胎面库存快照")
    @PostMapping("/tmMesSync/replaceStock")
    public AjaxResult replaceStock(@RequestParam("factoryCode") String factoryCode,
                                   @RequestParam("stockDate") String stockDate,
                                   @RequestParam("updateBy") String updateBy,
                                   @RequestBody List<TmStock> stockList) {
        this.tmStockService.replaceStock(factoryCode, DateUtil.parseDate(stockDate), updateBy, stockList);
        return AjaxResult.success();
    }

    /**
     * 替换胎面自动滚动班次库存快照。
     *
     * @param factoryCode 工厂编码
     * @param stockDate MES库存物理日期
     * @param shiftOrder 班次顺序
     * @param updateBy 更新人
     * @param stockList 班次库存列表
     * @return 保存结果
     */
    @Override
    @ApiOperation("替换胎面自动滚动班次库存快照")
    @PostMapping("/tmMesSync/replaceShiftStock")
    public AjaxResult replaceShiftStock(@RequestParam("factoryCode") String factoryCode,
                                        @RequestParam("stockDate") String stockDate,
                                        @RequestParam("shiftOrder") Integer shiftOrder,
                                        @RequestParam("updateBy") String updateBy,
                                        @RequestBody List<TmShiftStock> stockList) {
        this.tmShiftStockService.replaceShiftStock(factoryCode, DateUtil.parseDate(stockDate),
                shiftOrder, updateBy, stockList);
        return AjaxResult.success();
    }

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
    @Override
    @ApiOperation("逻辑删除并批量保存胎面排程完成量（事务性操作）")
    @PostMapping("/tmMesSync/logicDeleteAndSaveScheFinishQty")
    public AjaxResult logicDeleteAndSaveScheFinishQty(@RequestParam("factoryCode") String factoryCode,
                                                       @RequestParam("scheduleDate") String scheduleDate,
                                                       @RequestParam("updateBy") String updateBy,
                                                       @RequestBody List<TmScheFinishQty> list) {
        Date date = DateUtil.parse(scheduleDate);
        tmScheFinishQtyService.logicDeleteAndSaveBatch(factoryCode, date, updateBy, list);
        return AjaxResult.success();
    }

    /**
     * 胎面排程完成量回写胎面排程结果表各班次完成量
     * 根据完成量回报数据，按胎面代码+工单号+排程日期汇总后，
     * 查询排程结果表并按班次映射关系回写完成量
     *
     * @param list 完成量回报数据列表
     * @return 回写结果
     */
    @Override
    @ApiOperation("胎面排程完成量回写胎面排程结果表各班次完成量")
    @PostMapping("/tmMesSync/writeBackScheduleResultFinishQty")
    public AjaxResult writeBackScheduleResultFinishQty(@RequestBody List<TmScheFinishQty> list) {
        return tmScheFinishQtyService.writeBackScheduleResultFinishQty(list);
    }

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
    @Override
    @ApiOperation("逻辑删除并批量保存胎面排程日完成量（事务性操作）")
    @PostMapping("/tmMesSync/logicDeleteAndSaveDayFinishQty")
    public AjaxResult logicDeleteAndSaveDayFinishQty(@RequestParam("factoryCode") String factoryCode,
                                                      @RequestParam("scheduleDate") String scheduleDate,
                                                      @RequestParam("updateBy") String updateBy,
                                                      @RequestBody List<TmDayFinishQty> list) {
        Date date = DateUtil.parse(scheduleDate);
        tmDayFinishQtyService.logicDeleteAndSaveBatch(factoryCode, date, updateBy, list);
        return AjaxResult.success();
    }
}
