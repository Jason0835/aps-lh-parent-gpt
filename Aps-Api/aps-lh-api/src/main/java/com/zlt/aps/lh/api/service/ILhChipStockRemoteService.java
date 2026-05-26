package com.zlt.aps.lh.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.lh.api.domain.entity.LhChipStock;
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
@FeignClient(contextId = "ILhChipStockRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.lh:/lh}")
public interface ILhChipStockRemoteService {

    /**
     * 查询列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/lhChipStock/list")
    TableDataInfo list(@RequestBody LhChipStock queryVO);

    /**
     * 保存
     */
    @ApiOperation("保存")
    @PostMapping("/lhChipStock/save")
    AjaxResult save(@RequestBody LhChipStock lhChipStock);

    /**
     * 删除
     */
    @ApiOperation("删除")
    @PostMapping("/lhChipStock/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/lhChipStock/{id}")
    LhChipStock getInfo(@PathVariable("id") Long id);

    /**
     * 校验唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/lhChipStock/checkUnique")
    String checkUnique(@RequestBody LhChipStock lhChipStock);

    /**
     * 导出芯片库存列表
     */
    @ApiOperation("导出列表")
    @PostMapping("/lhChipStock/exportData/{fileName}")
    byte[] exportData(@RequestBody LhChipStock queryVO, @PathVariable("fileName") String fileName);

    /**
     * 导入芯片库存数据
     */
    @ApiOperation("导入数据")
    @PostMapping("/lhChipStock/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);

    /**
     * 累加更新完成量 - 硫化排程回填接口
     * 在原有完成量的基础上叠加传入的完成量值，而非直接覆盖
     */
    @ApiOperation("累加更新完成量")
    @PostMapping("/lhChipStock/updateFinishQty")
    AjaxResult updateFinishQty(@RequestParam("factoryCode") String factoryCode,
                               @RequestParam("chipCode") String chipCode,
                               @RequestParam("finishQty") Integer finishQty);

    /**
     * 根据分厂编号和数据来源逻辑删除芯片库存
     */
    @ApiOperation("根据分厂编号和数据来源逻辑删除芯片库存")
    @PostMapping("/lhChipStock/logicDeleteByDataSource")
    AjaxResult logicDeleteByDataSource(@RequestParam("factoryCode") String factoryCode,
                                       @RequestParam("dataSource") String dataSource,
                                       @RequestParam("updateBy") String updateBy);

    /**
     * 批量保存芯片库存
     */
    @ApiOperation("批量保存芯片库存")
    @PostMapping("/lhChipStock/saveBatch")
    AjaxResult saveChipStockBatch(@RequestBody List<LhChipStock> list);

    @ApiOperation("逻辑删除并批量保存芯片库存（事务性操作）")
    @PostMapping("/lhChipStock/logicDeleteAndSaveByDataSource")
    AjaxResult logicDeleteAndSaveByDataSource(@RequestParam("factoryCode") String factoryCode,
                                              @RequestParam("dataSource") String dataSource,
                                              @RequestParam("updateBy") String updateBy,
                                              @RequestBody List<LhChipStock> list);

    /**
     * 增量更新芯片库存完成量
     * 根据分厂编号+芯片编码匹配：已存在则累加完成量，不存在则新增记录
     */
    @ApiOperation("增量更新芯片库存完成量")
    @PostMapping("/lhChipStock/upsertFinishQty")
    AjaxResult upsertFinishQty(@RequestParam("factoryCode") String factoryCode,
                               @RequestBody List<LhChipStock> list);

    @ApiOperation("覆盖更新芯片库存完成量（定时任务全量回填）")
    @PostMapping("/lhChipStock/overwriteFinishQty")
    AjaxResult overwriteFinishQty(@RequestParam("factoryCode") String factoryCode,
                                  @RequestBody List<LhChipStock> list);

    /**
     * 合并保存 - 新增时检测到重复，将库存量和完成量累加到已有数据上
     */
    @ApiOperation("合并保存")
    @PostMapping("/lhChipStock/mergeSave")
    AjaxResult mergeSave(@RequestBody LhChipStock lhChipStock);
}
