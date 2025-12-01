package com.zlt.mix.setting.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.mix.setting.api.domain.dto.MlGlueStockDto;
import com.zlt.mix.setting.api.domain.entity.MlGlueStock;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 母炼库存信息Service接口
 *
 * @author Liam
 * @date 2022-04-12
 */
@FeignClient(contextId = "IMlGlueStockService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.schedule:mixSchedule}")
public interface IMlGlueStockService {

    /**
     * 查询母炼库存信息列表
     */
    @PostMapping("/mlstock/list")
    TableDataInfo listMlGlueStock(@RequestBody MlGlueStock mlGlueStock);

    /**
     * 根据ID获取详细信息
     */
    @GetMapping(value = "/mlstock/{id}")
    MlGlueStockDto getMlGlueStockInfo(@PathVariable("id") Long id);

    /**
     * 保存母炼库存信息信息（id为空则新增，id不为空则修改）
     */
    @PostMapping("/mlstock/save")
    AjaxResult saveMlGlueStock(@RequestBody MlGlueStockDto mlGlueStockDto);

    /**
     * 批量删除母炼库存信息
     */
    @PostMapping("/mlstock/delete/{ids}")
    AjaxResult deleteMlGlueStock(@PathVariable("ids") Long[] ids);

    /**
     * 校验母炼库存信息唯一性
     */
    @ApiOperation("校验母炼库存信息唯一性")
    @PostMapping("/mlstock/checkMlGlueStockUnique")
    String checkMlGlueStockUnique(@RequestBody MlGlueStock mlGlueStock);

    /**
     * 导出母炼库存信息列表
     */
    @PostMapping("/mlstock/exportData")
    List<MlGlueStockDto> exportData(@RequestBody MlGlueStock mlGlueStock);

    /**
     * 导入母炼库存信息数据
     */
    @ApiOperation("导入母炼库存信息")
    @PostMapping("/mlstock/importData")
    public AjaxResult importData(@RequestBody List<MlGlueStock> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);
}
