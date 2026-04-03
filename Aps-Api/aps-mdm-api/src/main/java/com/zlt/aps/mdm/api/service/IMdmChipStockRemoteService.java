package com.zlt.aps.mdm.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.mdm.api.domain.entity.MdmChipStock;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 芯片库存前端接口
 *
 * @author APS Team
 * @date 2026-04-02
 */
@FeignClient(contextId = "IMdmChipStockRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.lh:/lh}")
public interface IMdmChipStockRemoteService {

    /**
     * 查询列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/mdmChipStock/list")
    TableDataInfo list(@RequestBody MdmChipStock queryVO);

    /**
     * 保存
     */
    @ApiOperation("保存")
    @PostMapping("/mdmChipStock/save")
    AjaxResult save(@RequestBody MdmChipStock mdmChipStock);

    /**
     * 删除
     */
    @ApiOperation("删除")
    @PostMapping("/mdmChipStock/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/mdmChipStock/{id}")
    MdmChipStock getInfo(@PathVariable("id") Long id);

    /**
     * 校验唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/mdmChipStock/checkUnique")
    String checkUnique(@RequestBody MdmChipStock mdmChipStock);

    /**
     * 导出芯片库存列表
     */
    @ApiOperation("导出列表")
    @PostMapping("/mdmChipStock/exportData/{fileName}")
    byte[] exportData(@RequestBody MdmChipStock queryVO, @PathVariable("fileName") String fileName);

    /**
     * 导入芯片库存数据
     */
    @ApiOperation("导入数据")
    @PostMapping("/mdmChipStock/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);

    /**
     * 更新完成量 - 硫化排程回填接口
     */
    @ApiOperation("更新完成量")
    @PostMapping("/mdmChipStock/updateFinishQty")
    AjaxResult updateFinishQty(@RequestParam("factoryCode") String factoryCode,
                               @RequestParam("chipCode") String chipCode,
                               @RequestParam("finishQty") Integer finishQty);

    /**
     * 合并保存 - 新增时检测到重复，将库存量和完成量累加到已有数据上
     */
    @ApiOperation("合并保存")
    @PostMapping("/mdmChipStock/mergeSave")
    AjaxResult mergeSave(@RequestBody MdmChipStock mdmChipStock);
}
