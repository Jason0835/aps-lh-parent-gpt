package com.zlt.aps.gsq.controller;

import cn.hutool.core.date.DateUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.gsq.api.domain.entity.GsqDayFinishQty;
import com.zlt.aps.gsq.api.domain.entity.GsqStock;
import com.zlt.aps.gsq.api.service.IGsqMesSyncRemoteService;
import com.zlt.aps.gsq.service.IGsqDayFinishQtyService;
import com.zlt.aps.gsq.service.IGsqStockService;
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
 * 钢丝圈MES同步控制器
 * 实现IGsqMesSyncRemoteService Feign接口，供ITF模块远程调用
 *
 * @author APS Team
 * @since 2026/08/11
 */
@Slf4j
@Api(tags = "钢丝圈MES同步")
@RestController
public class GsqMesSyncController implements IGsqMesSyncRemoteService {

    @Autowired
    private IGsqDayFinishQtyService gsqDayFinishQtyService;

    @Autowired
    private IGsqStockService gsqStockService;

    /**
     * 逻辑删除并批量保存钢丝圈排程日完成量（事务性操作）
     * 步骤1：逻辑删除指定分厂+排程日期的旧数据（IS_DELETE置为1）
     * 步骤2：批量插入MES最新排程日完成量数据（新记录，IS_DELETE=0）
     *
     * @param factoryCode  分厂编号
     * @param scheduleDate 排程日期，格式：yyyy-MM-dd
     * @param updateBy     更新者
     * @param list         待插入的钢丝圈排程日完成量列表
     * @return 结果
     */
    @Override
    @ApiOperation("逻辑删除并批量保存钢丝圈排程日完成量（事务性操作）")
    @PostMapping("/gsqMesSync/logicDeleteAndSaveDayFinishQty")
    public AjaxResult logicDeleteAndSaveDayFinishQty(@RequestParam("factoryCode") String factoryCode,
                                                      @RequestParam("scheduleDate") String scheduleDate,
                                                      @RequestParam("updateBy") String updateBy,
                                                      @RequestBody List<GsqDayFinishQty> list) {
        Date date = DateUtil.parse(scheduleDate);
        gsqDayFinishQtyService.logicDeleteAndSaveBatch(factoryCode, date, updateBy, list);
        return AjaxResult.success();
    }

    /**
     * 逻辑删除并批量保存钢丝圈库存（事务性操作）
     * 步骤1：逻辑删除指定库存日期的旧数据（IS_DELETE置为1）
     * 步骤2：批量插入MES最新钢丝圈库存数据（新记录，IS_DELETE=0）
     *
     * @param stockDate 库存日期，格式：yyyy-MM-dd
     * @param updateBy  更新者
     * @param list      待插入的钢丝圈库存列表
     * @return 结果
     */
    @Override
    @ApiOperation("逻辑删除并批量保存钢丝圈库存（事务性操作）")
    @PostMapping("/gsqMesSync/logicDeleteAndSaveGsqStockByStockDate")
    public AjaxResult logicDeleteAndSaveGsqStockByStockDate(@RequestParam("stockDate") String stockDate,
                                                             @RequestParam("updateBy") String updateBy,
                                                             @RequestBody List<GsqStock> list) {
        Date date = DateUtil.parse(stockDate);
        gsqStockService.logicDeleteAndSaveBatch(date, updateBy, list);
        return AjaxResult.success();
    }
}
