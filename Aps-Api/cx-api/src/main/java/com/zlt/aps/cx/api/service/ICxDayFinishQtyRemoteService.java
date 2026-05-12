package com.zlt.aps.cx.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.cx.api.domain.entity.CxDayFinishQty;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

/**
 * 成型排程日完成量远程接口
 *
 * @author APS Team
 * @since 2026/05/12
 */
@FeignClient(contextId = "ICxDayFinishQtyRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.cx:/cx}")
public interface ICxDayFinishQtyRemoteService {

    /**
     * 查询成型排程日完成量列表
     *
     * @param queryVO 查询条件
     * @return 分页结果
     */
    @ApiOperation("查询列表")
    @PostMapping("/cxDayFinishQty/list")
    TableDataInfo list(@RequestBody CxDayFinishQty queryVO);

    /**
     * 根据ID获取详情
     *
     * @param id 主键ID
     * @return 详情
     */
    @ApiOperation("获取详情")
    @GetMapping(value = "/cxDayFinishQty/{id}")
    CxDayFinishQty getInfo(@PathVariable("id") Long id);

    /**
     * 导出数据
     *
     * @param queryVO  查询条件
     * @param fileName 文件名
     * @return 导出字节
     */
    @ApiOperation("导出数据")
    @PostMapping("/cxDayFinishQty/exportData/{fileName}")
    byte[] exportData(@RequestBody CxDayFinishQty queryVO, @PathVariable("fileName") String fileName);
}
