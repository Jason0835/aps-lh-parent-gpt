package com.zlt.mix.setting.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.mix.setting.api.domain.entity.LhflGlueStock;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 硫磺辅料终炼库存信息Service接口
 *
 * @author Liam
 * @date 2022-04-18
 */
@FeignClient(contextId = "ILhflGlueStockService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.schedule:mixSchedule}")
public interface ILhflGlueStockService {

    /**
     * 查询硫磺辅料终炼库存信息列表
     */
    @PostMapping("/lhflGlueStock/list")
    TableDataInfo listLhflGlueStock(@RequestBody LhflGlueStock lhflGlueStock);
    
    /**
     * 查询硫磺辅料终炼库存信息列表（不分页）
     */
    @PostMapping("/lhflGlueStock/selectLhflGlueStock")
    List<LhflGlueStock> selectLhflGlueStock(@RequestBody LhflGlueStock lhflGlueStock);

    /**
     * 根据ID获取详细信息
     */
    @GetMapping(value = "/lhflGlueStock/{id}")
    LhflGlueStock getLhflGlueStockInfo(@PathVariable("id") Long id);

    /**
     * 保存硫磺辅料终炼库存信息信息（id为空则新增，id不为空则修改）
     */
    @PostMapping("/lhflGlueStock/save")
    AjaxResult saveLhflGlueStock(@RequestBody LhflGlueStock lhflGlueStock);

    /**
     * 批量删除硫磺辅料终炼库存信息
     */
    @PostMapping("/lhflGlueStock/delete/{ids}")
    AjaxResult deleteLhflGlueStock(@PathVariable("ids") Long[] ids);

    /**
     * 校验硫磺辅料终炼库存信息唯一性
     */
    @ApiOperation("校验硫磺辅料终炼库存信息唯一性")
    @PostMapping("/lhflGlueStock/checkLhflGlueStockUnique")
    String checkLhflGlueStockUnique(@RequestBody LhflGlueStock lhflGlueStock);

    /**
     * 导出硫磺辅料终炼库存信息列表
     */
    @PostMapping("/lhflGlueStock/exportData")
    List<LhflGlueStock> exportData(@RequestBody LhflGlueStock lhflGlueStock);

    /**
     * 导入硫磺辅料终炼库存信息数据
     */
    @ApiOperation("导入硫磺辅料终炼库存信息")
    @PostMapping("/lhflGlueStock/importData")
    public AjaxResult importData(@RequestBody List<LhflGlueStock> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);
}
