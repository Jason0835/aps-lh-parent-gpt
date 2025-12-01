package com.zlt.mix.setting.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.mix.setting.api.domain.entity.BhgGlueStock;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 不合格胶库存信息Service接口
 *
 * @author Liam
 * @date 2022-04-12
 */
@FeignClient(contextId = "IBhgGlueStockService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.schedule:mixSchedule}")
public interface IBhgGlueStockService {

    /**
     * 查询不合格胶库存信息列表
     */
    @PostMapping("/bhgstock/list")
    TableDataInfo listBhgGlueStock(@RequestBody BhgGlueStock bhgGlueStock);

    /**
     * 根据ID获取详细信息
     */
    @GetMapping(value = "/bhgstock/{id}")
    BhgGlueStock getBhgGlueStockInfo(@PathVariable("id") Long id);

    /**
     * 保存不合格胶库存信息信息（id为空则新增，id不为空则修改）
     */
    @PostMapping("/bhgstock/save")
    AjaxResult saveBhgGlueStock(@RequestBody BhgGlueStock bhgGlueStock);

    /**
     * 批量删除不合格胶库存信息
     */
    @PostMapping("/bhgstock/delete/{ids}")
    AjaxResult deleteBhgGlueStock(@PathVariable("ids") Long[] ids);

    /**
     * 校验不合格胶库存信息唯一性
     */
    @ApiOperation("校验不合格胶库存信息唯一性")
    @PostMapping("/bhgstock/checkBhgGlueStockUnique")
    String checkBhgGlueStockUnique(@RequestBody BhgGlueStock bhgGlueStock);

    /**
     * 导出不合格胶库存信息列表
     */
    @PostMapping("/bhgstock/exportData")
    List<BhgGlueStock> exportData(@RequestBody BhgGlueStock bhgGlueStock);

    /**
     * 导入不合格胶库存信息数据
     */
    @ApiOperation("导入不合格胶库存信息")
    @PostMapping("/bhgstock/importData")
    public AjaxResult importData(@RequestBody List<BhgGlueStock> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);
}
