package com.zlt.mix.setting.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.mix.setting.api.domain.entity.GlueLossSetting;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * 胶料损耗率设定Service接口
 * @author Joran.zhang
 * @date 2022-05-23
 */
@FeignClient(contextId = "IGlueLossSettingService", value =ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.schedule:mixSchedule}")
public interface IGlueLossSettingService {

    /**
     * 查询胶料损耗率设定列表
     */
    @PostMapping("/glueLossSetting/list")
    TableDataInfo listGlueLossSetting(@RequestBody GlueLossSetting glueLossSetting);

    /**
    * 根据ID获取详细信息
    */
    @GetMapping(value = "/glueLossSetting/{id}")
    GlueLossSetting getGlueLossSettingInfo(@PathVariable("id") Long id);

    /**
    * 保存胶料损耗率设定信息（id为空则新增，id不为空则修改）
    */
    @PostMapping("/glueLossSetting/save")
    AjaxResult saveGlueLossSetting(@RequestBody GlueLossSetting glueLossSetting);

    /**
     * 批量删除胶料损耗率设定
     */
    @PostMapping("/glueLossSetting/delete/{ids}")
    AjaxResult deleteGlueLossSetting(@PathVariable("ids") Long[] ids);

    /**
     * 校验胶料损耗率设定唯一性
     */
    @ApiOperation("校验胶料损耗率设定唯一性")
    @PostMapping("/glueLossSetting/checkGlueLossSettingUnique")
    String checkGlueLossSettingUnique(@RequestBody GlueLossSetting glueLossSetting);

    /**
     * 导出胶料损耗率设定列表
     */
    @PostMapping("/glueLossSetting/exportData")
    List<GlueLossSetting> exportData(@RequestBody GlueLossSetting glueLossSetting);

    /**
     * 导入胶料损耗率设定数据
     */
    @ApiOperation("导入胶料损耗率设定")
    @PostMapping("/glueLossSetting/importData")
    public AjaxResult importData(@RequestBody List<GlueLossSetting> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);
}
