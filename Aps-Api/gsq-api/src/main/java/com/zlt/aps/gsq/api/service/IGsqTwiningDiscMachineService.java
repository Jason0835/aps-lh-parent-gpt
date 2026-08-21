package com.zlt.aps.gsq.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.gsq.api.domain.entity.GsqTwiningDiscMachine;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 钢丝圈缠绕盘-机台关系对外暴露接口（Feign）
 * 路径前缀：/gsq/discMachine
 *
 * @author zlt
 * @date 2026-08-20
 */
@FeignClient(contextId = "iGsqTwiningDiscMachineService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.gsq:gsq}")
public interface IGsqTwiningDiscMachineService {

    /**
     * 查询缠绕盘-机台关系列表（含缠绕盘名称/英寸/排列方式/机台名称反显）
     *
     * @param entity 查询条件
     * @return 列表数据
     */
    @PostMapping("/gsq/discMachine/list")
    @ApiOperation("查询缠绕盘-机台关系列表")
    TableDataInfo list(@RequestBody GsqTwiningDiscMachine entity);

    /**
     * 获取缠绕盘-机台关系详细信息
     *
     * @param id 主键ID
     * @return 详细信息
     */
    @GetMapping("/gsq/discMachine/{id}")
    @ApiOperation("获取缠绕盘-机台关系详细信息")
    GsqTwiningDiscMachine getInfo(@PathVariable("id") Long id);

    /**
     * 保存缠绕盘-机台关系（id为空新增，id不为空修改）
     * 保存前校验：缠绕盘编码存在性、机台编号存在性、组合唯一性
     *
     * @param entity 实体
     * @return 操作结果
     */
    @PostMapping("/gsq/discMachine/save")
    @ApiOperation("保存缠绕盘-机台关系")
    AjaxResult save(@RequestBody GsqTwiningDiscMachine entity);

    /**
     * 删除缠绕盘-机台关系（逻辑删除）
     *
     * @param ids 主键ID集合
     * @return 操作结果
     */
    @PostMapping("/gsq/discMachine/delete/{ids}")
    @ApiOperation("删除缠绕盘-机台关系")
    AjaxResult removeByIds(@PathVariable("ids") java.util.List<Long> ids);

    /**
     * 导出缠绕盘-机台关系
     *
     * @param entity   查询条件
     * @param fileName 文件名
     * @return 文件字节
     */
    @PostMapping("/gsq/discMachine/exportData/{fileName}")
    @ApiOperation("导出缠绕盘-机台关系")
    byte[] exportData(@RequestBody GsqTwiningDiscMachine entity, @PathVariable("fileName") String fileName);

    /**
     * 校验缠绕盘+机台组合唯一性
     *
     * @param entity 实体
     * @return 唯一性结果
     */
    @PostMapping("/gsq/discMachine/checkUnique")
    @ApiOperation("校验缠绕盘+机台组合唯一性")
    String checkUnique(@RequestBody GsqTwiningDiscMachine entity);
}
