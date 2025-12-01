package com.zlt.aps.monthplan.itf.controller;

import com.ruoyi.api.gateway.system.domain.SysConfig;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.monthplan.api.domain.itf.InSaleOrderDto;
import com.zlt.aps.monthplan.itf.mapper.SaleOrderMapper;
import com.zlt.aps.monthplan.itf.service.IInSaleOrderSyncService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.UnsupportedEncodingException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * @author Chen
 * @date 2025/4/8
 */
@Api(tags = "内销接口同步服务")
@RestController
@RequestMapping("/saleOrderSync")
public class SaleOrderSyncController {

    @Autowired
    private IInSaleOrderSyncService inSaleOrderSyncService;

    @Autowired
    private SaleOrderMapper saleOrderMapper;

    /**
     * 内销销售订单同步
     *
     * @param inSaleOrderDto 年月
     * @return 结果
     */
    @ApiOperation("内销销售订单同步")
    @PostMapping("/syncInSaleOrder")
    public AjaxResult syncInSaleOrder(@RequestBody InSaleOrderDto inSaleOrderDto) throws UnsupportedEncodingException, IllegalAccessException {
        return inSaleOrderSyncService.syncInSaleOrder(inSaleOrderDto);
    }

    /**
     * 外销销售订单同步
     *
     * @param inSaleOrderDto 年月
     * @return 结果
     */
    @ApiOperation("外销销售订单同步")
    @PostMapping("/syncOutSaleOrder")
    public AjaxResult syncOutSaleOrder(@RequestBody InSaleOrderDto inSaleOrderDto) throws UnsupportedEncodingException, IllegalAccessException {
        return inSaleOrderSyncService.syncOutSaleOrder(inSaleOrderDto);
    }

    /**
     * 内销历史销售订单同步
     *
     * @return 结果
     */
    @ApiOperation("内销历史销售订单同步")
    @PostMapping("/syncInHisSaleOrder")
    public AjaxResult syncInHisSaleOrder() throws UnsupportedEncodingException, IllegalAccessException {
        return syncInHisSaleOrderWithArgs(null);
    }

    /**
     * 内销历史销售订单同步测试使用
     *
     * @return 结果
     */
    @ApiOperation("内销历史销售订单同步测试使用")
    @PostMapping("/syncInHisSaleOrderTest")
    public AjaxResult syncInHisSaleOrderWithArgs(@RequestBody InSaleOrderDto inSaleOrderDto) throws UnsupportedEncodingException, IllegalAccessException {
        // 取参数，取当前日期到过去指定参数天的数据
        int minusDays = 30;
        SysConfig queryParam = new SysConfig();
        queryParam.setConfigKey("his.order.minusDays");
        SysConfig configByKey = saleOrderMapper.getSysConfigByKey(queryParam);
        if (configByKey != null) {
            minusDays = Integer.parseInt(configByKey.getConfigValue());
        }
        // 获取当前日期
        LocalDate currentDate = LocalDate.now();
        // 获取当前日期的 23:59:59
        LocalDateTime endDateTime = currentDate.atTime(LocalTime.MAX);
        // 获取 30 天前的日期
        LocalDate thirtyDaysAgo = currentDate.minusDays(minusDays);
        // 获取 30 天前日期的 00:00:00
        LocalDateTime startDateTime = thirtyDaysAgo.atStartOfDay();
        // 创建 DateTimeFormatter 对象，指定日期时间格式
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        if (inSaleOrderDto == null) {
            inSaleOrderDto = new InSaleOrderDto();
        }
        inSaleOrderDto.setDates1(startDateTime.format(formatter));
        inSaleOrderDto.setDates2(endDateTime.format(formatter));
        return inSaleOrderSyncService.syncInHisSaleOrder(inSaleOrderDto);
    }
}
