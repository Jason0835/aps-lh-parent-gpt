package com.zlt.aps.gsq.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.gsq.api.domain.entity.GsqSteelRingStock;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 钢丝圈库存管理对外暴露接口
 *
 * @author zlt
 * @date 2026-07-08
 */
@FeignClient(contextId = "iGsqSteelRingStockRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.gsq:gsq}")
public interface IGsqSteelRingStockRemoteService {

    /**
     * 查询钢丝圈库存列表
     *
     * @param entity 查询条件
     * @return 列表数据
     */
    @PostMapping("/gsq/steelRingStock/list")
    @ApiOperation("查询钢丝圈库存列表")
    TableDataInfo list(@RequestBody GsqSteelRingStock entity);

    /**
     * 获取钢丝圈库存详细信息
     *
     * @param id 主键ID
     * @return 详细信息
     */
    @GetMapping(value = "/gsq/steelRingStock/getInfo/{id}")
    @ApiOperation("获取钢丝圈库存详细信息")
    GsqSteelRingStock getInfo(@PathVariable("id") Long id);

    /**
     * 新增钢丝圈库存
     *
     * @param entity 实体
     * @return 操作结果
     */
    @PostMapping("/gsq/steelRingStock/add")
    @ApiOperation("新增钢丝圈库存")
    AjaxResult add(@RequestBody GsqSteelRingStock entity);

    /**
     * 编辑钢丝圈库存
     *
     * @param entity 实体
     * @return 操作结果
     */
    @PostMapping("/gsq/steelRingStock/edit")
    @ApiOperation("编辑钢丝圈库存")
    AjaxResult edit(@RequestBody GsqSteelRingStock entity);

    /**
     * 删除钢丝圈库存
     *
     * @param ids 主键ID集合
     * @return 操作结果
     */
    @PostMapping("/gsq/steelRingStock/remove")
    @ApiOperation("删除钢丝圈库存")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    /**
     * 导出钢丝圈库存
     *
     * @param entity   查询条件
     * @param fileName 文件名
     * @return 文件字节
     */
    @PostMapping("/gsq/steelRingStock/exportData/{fileName}")
    @ApiOperation("导出钢丝圈库存")
    byte[] exportData(@RequestBody GsqSteelRingStock entity, @PathVariable("fileName") String fileName);

    /**
     * 导入钢丝圈库存
     *
     * @param importContext 导入上下文
     * @param updateSupport 已存在是否更新
     * @return 操作结果
     */
    @PostMapping("/gsq/steelRingStock/importData")
    @ApiOperation("导入钢丝圈库存")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);

    /**
     * 校验钢丝圈库存唯一性
     *
     * @param entity 实体
     * @return 唯一性结果
     */
    @PostMapping("/gsq/steelRingStock/checkUnique")
    @ApiOperation("校验钢丝圈库存唯一性")
    String checkUnique(@RequestBody GsqSteelRingStock entity);
}
