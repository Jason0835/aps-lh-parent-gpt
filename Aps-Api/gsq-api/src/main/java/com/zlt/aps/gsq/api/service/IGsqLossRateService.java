package com.zlt.aps.gsq.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.gsq.api.domain.entity.GsqLossRate;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 钢丝圈损耗率管理对外暴露接口
 *
 * @author zlt
 * @date 2026-07-08
 */
@FeignClient(contextId = "iGsqLossRateService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.gsq:gsq}")
public interface IGsqLossRateService {

    /**
     * 查询钢丝圈损耗率列表
     *
     * @param entity 查询条件
     * @return 列表数据
     */
    @PostMapping("/gsq/lossRate/list")
    @ApiOperation("查询钢丝圈损耗率列表")
    TableDataInfo list(@RequestBody GsqLossRate entity);

    /**
     * 获取钢丝圈损耗率详细信息
     *
     * @param id 主键ID
     * @return 详细信息
     */
    @GetMapping(value = "/gsq/lossRate/getInfo/{id}")
    @ApiOperation("获取钢丝圈损耗率详细信息")
    GsqLossRate getInfo(@PathVariable("id") Long id);

    /**
     * 新增钢丝圈损耗率
     *
     * @param entity 实体
     * @return 操作结果
     */
    @PostMapping("/gsq/lossRate/add")
    @ApiOperation("新增钢丝圈损耗率")
    AjaxResult add(@RequestBody GsqLossRate entity);

    /**
     * 编辑钢丝圈损耗率
     *
     * @param entity 实体
     * @return 操作结果
     */
    @PostMapping("/gsq/lossRate/edit")
    @ApiOperation("编辑钢丝圈损耗率")
    AjaxResult edit(@RequestBody GsqLossRate entity);

    /**
     * 删除钢丝圈损耗率
     *
     * @param ids 主键ID集合
     * @return 操作结果
     */
    @PostMapping("/gsq/lossRate/remove")
    @ApiOperation("删除钢丝圈损耗率")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    /**
     * 导出钢丝圈损耗率
     *
     * @param entity   查询条件
     * @param fileName 文件名
     * @return 文件字节
     */
    @PostMapping("/gsq/lossRate/exportData/{fileName}")
    @ApiOperation("导出钢丝圈损耗率")
    byte[] exportData(@RequestBody GsqLossRate entity, @PathVariable("fileName") String fileName);

    /**
     * 导入钢丝圈损耗率
     *
     * @param importContext 导入上下文
     * @param updateSupport 已存在是否更新
     * @return 操作结果
     */
    @PostMapping("/gsq/lossRate/importData")
    @ApiOperation("导入钢丝圈损耗率")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);

    /**
     * 校验钢丝圈损耗率唯一性
     *
     * @param entity 实体
     * @return 唯一性结果
     */
    @PostMapping("/gsq/lossRate/checkUnique")
    @ApiOperation("校验钢丝圈损耗率唯一性")
    String checkUnique(@RequestBody GsqLossRate entity);
}
