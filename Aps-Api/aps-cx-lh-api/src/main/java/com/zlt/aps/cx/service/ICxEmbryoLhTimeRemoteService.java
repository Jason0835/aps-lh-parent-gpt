package com.zlt.aps.cx.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.cx.entity.config.CxEmbryoLhTime;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 胎胚最早可供硫化时间前端接口
 *
 * @author APS Team
 */
@FeignClient(contextId = "ICxEmbryoLhTimeRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.cx:/cx}")
public interface ICxEmbryoLhTimeRemoteService {

    /**
     * 查询列表
     *
     * @param queryVO 查询条件
     * @return 列表数据
     */
    @ApiOperation("查询列表")
    @PostMapping("/cxEmbryoLhTime/list")
    TableDataInfo list(@RequestBody CxEmbryoLhTime queryVO);

    /**
     * 保存
     *
     * @param entity 实体对象
     * @return 操作结果
     */
    @ApiOperation("保存")
    @PostMapping("/cxEmbryoLhTime/save")
    AjaxResult save(@RequestBody CxEmbryoLhTime entity);

    /**
     * 删除
     *
     * @param ids 主键ID列表
     * @return 操作结果
     */
    @ApiOperation("删除")
    @PostMapping("/cxEmbryoLhTime/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    /**
     * 根据ID获取详细信息
     *
     * @param id 主键ID
     * @return 实体对象
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/cxEmbryoLhTime/{id}")
    CxEmbryoLhTime getInfo(@PathVariable("id") Long id);

    /**
     * 校验唯一性
     *
     * @param entity 实体对象
     * @return 唯一性校验结果
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/cxEmbryoLhTime/checkUnique")
    String checkUnique(@RequestBody CxEmbryoLhTime entity);

    /**
     * 导出胎胚最早可供硫化时间列表
     *
     * @param queryVO  查询条件
     * @param fileName 文件名
     * @return Excel文件字节数组
     */
    @ApiOperation("导出列表")
    @PostMapping("/cxEmbryoLhTime/exportData/{fileName}")
    byte[] exportData(@RequestBody CxEmbryoLhTime queryVO, @PathVariable("fileName") String fileName);

    /**
     * 导入胎胚最早可供硫化时间数据
     *
     * @param importContext 导入上下文
     * @param updateSupport 已存在记录是否更新
     * @return 操作结果
     */
    @ApiOperation("导入数据")
    @PostMapping("/cxEmbryoLhTime/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);
}
