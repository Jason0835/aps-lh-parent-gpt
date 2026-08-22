package com.zlt.aps.gsq.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.gsq.api.domain.entity.GsqTwiningDiscSpec;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * 钢丝圈缠绕盘-规格关系对外暴露接口（Feign）
 * 路径前缀：/gsq/discSpec
 *
 * @author zlt
 * @date 2026-08-21
 */
@FeignClient(contextId = "iGsqTwiningDiscSpecService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.gsq:gsq}")
public interface IGsqTwiningDiscSpecService {

    /**
     * 查询缠绕盘-规格关系列表（含缠绕盘名称/英寸/排列方式反显）
     *
     * @param entity 查询条件
     * @return 列表数据
     */
    @PostMapping("/gsq/discSpec/list")
    @ApiOperation("查询缠绕盘-规格关系列表")
    TableDataInfo list(@RequestBody GsqTwiningDiscSpec entity);

    /**
     * 获取缠绕盘-规格关系详细信息
     *
     * @param id 主键ID
     * @return 详细信息
     */
    @GetMapping("/gsq/discSpec/{id}")
    @ApiOperation("获取缠绕盘-规格关系详细信息")
    GsqTwiningDiscSpec getInfo(@PathVariable("id") Long id);

    /**
     * 保存缠绕盘-规格关系（id为空新增，id不为空修改）
     * 保存前校验：缠绕盘编码存在性、钢丝圈编号存在性、组合唯一性
     *
     * @param entity 实体
     * @return 操作结果
     */
    @PostMapping("/gsq/discSpec/save")
    @ApiOperation("保存缠绕盘-规格关系")
    AjaxResult save(@RequestBody GsqTwiningDiscSpec entity);

    /**
     * 删除缠绕盘-规格关系（逻辑删除）
     *
     * @param ids 主键ID集合
     * @return 操作结果
     */
    @PostMapping("/gsq/discSpec/delete/{ids}")
    @ApiOperation("删除缠绕盘-规格关系")
    AjaxResult removeByIds(@PathVariable("ids") List<Long> ids);

    /**
     * 导出缠绕盘-规格关系
     *
     * @param entity   查询条件
     * @param fileName 文件名
     * @return 文件字节
     */
    @PostMapping("/gsq/discSpec/exportData/{fileName}")
    @ApiOperation("导出缠绕盘-规格关系")
    byte[] exportData(@RequestBody GsqTwiningDiscSpec entity, @PathVariable("fileName") String fileName);

    /**
     * 校验缠绕盘+钢丝圈规格组合唯一性
     *
     * @param entity 实体
     * @return 唯一性结果
     */
    @PostMapping("/gsq/discSpec/checkUnique")
    @ApiOperation("校验缠绕盘+钢丝圈规格组合唯一性")
    String checkUnique(@RequestBody GsqTwiningDiscSpec entity);

    /**
     * 查询施工信息表全部钢丝圈选项（编码+名称，去重），供规格关系页面下拉选择使用
     *
     * @return 钢丝圈选项列表
     */
    @GetMapping("/gsq/discSpec/listSteelRingOptions")
    @ApiOperation("查询钢丝圈下拉选项")
    AjaxResult listSteelRingOptions();
}
