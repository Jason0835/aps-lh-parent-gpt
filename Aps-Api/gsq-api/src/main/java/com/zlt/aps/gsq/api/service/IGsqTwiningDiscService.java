package com.zlt.aps.gsq.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.gsq.api.domain.entity.GsqTwiningDisc;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 钢丝圈缠绕盘对外暴露接口（Feign）
 * 路径前缀：/gsq/twiningDisc
 *
 * @author zlt
 * @date 2026-07-08
 */
@FeignClient(contextId = "iGsqTwiningDiscService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.gsq:gsq}")
public interface IGsqTwiningDiscService {

    /**
     * 查询钢丝圈缠绕盘列表
     *
     * @param entity 查询条件
     * @return 列表数据
     */
    @PostMapping("/gsq/twiningDisc/list")
    @ApiOperation("查询钢丝圈缠绕盘列表")
    TableDataInfo list(@RequestBody GsqTwiningDisc entity);

    /**
     * 获取钢丝圈缠绕盘详细信息（含子表明细）
     *
     * @param id 主键ID
     * @return 详细信息（含 subList）
     */
    @GetMapping("/gsq/twiningDisc/{id}")
    @ApiOperation("获取钢丝圈缠绕盘详细信息")
    GsqTwiningDisc getInfo(@PathVariable("id") Long id);

    /**
     * 保存钢丝圈缠绕盘（id为空新增，id不为空修改），同时级联保存子表明细
     *
     * @param entity 实体（含 subList）
     * @return 操作结果
     */
    @PostMapping("/gsq/twiningDisc/save")
    @ApiOperation("保存钢丝圈缠绕盘（含子表）")
    AjaxResult save(@RequestBody GsqTwiningDisc entity);

    /**
     * 删除钢丝圈缠绕盘（逻辑删除，级联删除子表）
     *
     * @param ids 主键ID集合
     * @return 操作结果
     */
    @PostMapping("/gsq/twiningDisc/delete/{ids}")
    @ApiOperation("删除钢丝圈缠绕盘")
    AjaxResult removeByIds(@PathVariable("ids") List<Long> ids);

    /**
     * 导出钢丝圈缠绕盘
     *
     * @param entity   查询条件
     * @param fileName 文件名
     * @return 文件字节
     */
    @PostMapping("/gsq/twiningDisc/exportData/{fileName}")
    @ApiOperation("导出钢丝圈缠绕盘")
    byte[] exportData(@RequestBody GsqTwiningDisc entity, @PathVariable("fileName") String fileName);

    /**
     * 导入钢丝圈缠绕盘
     *
     * @param importContext 导入上下文
     * @param updateSupport 已存在是否更新
     * @return 操作结果
     */
    @PostMapping("/gsq/twiningDisc/importData")
    @ApiOperation("导入钢丝圈缠绕盘")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);

    /**
     * 校验缠绕盘编码唯一性
     *
     * @param entity 实体
     * @return 唯一性结果
     */
    @PostMapping("/gsq/twiningDisc/checkUnique")
    @ApiOperation("校验缠绕盘编码唯一性")
    String checkUnique(@RequestBody GsqTwiningDisc entity);
}
