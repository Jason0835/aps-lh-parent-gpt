package com.zlt.aps.gsq.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.gsq.api.domain.entity.GsqSpecifyMachine;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 钢丝圈定点机台对外暴露接口
 *
 * @author zlt
 * @date 2026-07-08
 */
@FeignClient(contextId = "iGsqSpecifyMachineService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.gsq:gsq}")
public interface IGsqSpecifyMachineService {

    /**
     * 查询钢丝圈定点机台列表
     *
     * @param entity 查询条件
     * @return 列表数据
     */
    @PostMapping("/gsq/specifyMachine/list")
    @ApiOperation("查询钢丝圈定点机台列表")
    TableDataInfo list(@RequestBody GsqSpecifyMachine entity);

    /**
     * 获取钢丝圈定点机台详细信息
     *
     * @param id 主键ID
     * @return 详细信息
     */
    @GetMapping(value = "/gsq/specifyMachine/{id}")
    @ApiOperation("获取钢丝圈定点机台详细信息")
    GsqSpecifyMachine getInfo(@PathVariable("id") Long id);

    /**
     * 保存钢丝圈定点机台（id为空则新增，id不为空则修改）
     *
     * @param entity 实体
     * @return 操作结果
     */
    @PostMapping("/gsq/specifyMachine/save")
    @ApiOperation("保存钢丝圈定点机台（id为空则新增，id不为空则修改）")
    AjaxResult save(@RequestBody GsqSpecifyMachine entity);

    /**
     * 删除钢丝圈定点机台
     *
     * @param ids 主键ID集合
     * @return 操作结果
     */
    @PostMapping("/gsq/specifyMachine/delete/{ids}")
    @ApiOperation("删除钢丝圈定点机台")
    AjaxResult removeByIds(@PathVariable("ids") List<Long> ids);

    /**
     * 导出钢丝圈定点机台
     *
     * @param entity   查询条件
     * @param fileName 文件名
     * @return 文件字节
     */
    @PostMapping("/gsq/specifyMachine/exportData/{fileName}")
    @ApiOperation("导出钢丝圈定点机台")
    byte[] exportData(@RequestBody GsqSpecifyMachine entity, @PathVariable("fileName") String fileName);

    /**
     * 导入钢丝圈定点机台
     *
     * @param importContext 导入上下文
     * @param updateSupport 已存在是否更新
     * @return 操作结果
     */
    @PostMapping("/gsq/specifyMachine/importData")
    @ApiOperation("导入钢丝圈定点机台")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);

    /**
     * 校验钢丝圈定点机台唯一性
     *
     * @param entity 实体
     * @return 唯一性结果
     */
    @PostMapping("/gsq/specifyMachine/checkUnique")
    @ApiOperation("校验钢丝圈定点机台唯一性")
    String checkUnique(@RequestBody GsqSpecifyMachine entity);
}
