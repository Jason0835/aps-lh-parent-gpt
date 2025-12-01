package com.zlt.mix.setting.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.mix.setting.api.domain.entity.FactoryGlueAreaRelation;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * 分厂胶料与密炼区对应关系Service接口
 * @author zlt
 * @date 2022-11-22
 */
@FeignClient(contextId = "IFactoryGlueAreaRelationService", value =ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.schedule:mixSchedule}")
public interface IFactoryGlueAreaRelationService {

    /**
     * 查询分厂胶料与密炼区对应关系列表
     */
    @ApiOperation("查询分厂胶料与密炼区对应关系列表")
    @PostMapping("/factoryGlueAreaRelation/list")
    TableDataInfo list(@RequestBody FactoryGlueAreaRelation tFactoryGlueAreaRelation);

    /**
    * 新增分厂胶料与密炼区对应关系
    */
    @ApiOperation("新增分厂胶料与密炼区对应关系")
    @PostMapping("/factoryGlueAreaRelation/add")
    AjaxResult add(@RequestBody FactoryGlueAreaRelation tFactoryGlueAreaRelation);

    /**
     * 修改分厂胶料与密炼区对应关系
     */
    @ApiOperation("修改分厂胶料与密炼区对应关系")
    @PostMapping("/factoryGlueAreaRelation/edit")
    AjaxResult edit(@RequestBody FactoryGlueAreaRelation tFactoryGlueAreaRelation);

    /**
     * 删除分厂胶料与密炼区对应关系
     */
    @ApiOperation("删除分厂胶料与密炼区对应关系")
    @DeleteMapping("/factoryGlueAreaRelation/{ids}")
    AjaxResult remove(@PathVariable("ids") Long[] ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/factoryGlueAreaRelation/{id}")
    FactoryGlueAreaRelation getInfo(@PathVariable("id") Long id);

    /**
     * 校验分厂胶料与密炼区对应关系唯一性
     */
    @ApiOperation("校验分厂胶料与密炼区对应关系唯一性")
    @PostMapping("/factoryGlueAreaRelation/checkFactoryGlueAreaRelationUnique")
    String checkFactoryGlueAreaRelationUnique(@RequestBody FactoryGlueAreaRelation tFactoryGlueAreaRelation);

    /**
     * 导出分厂胶料与密炼区对应关系列表
     */
    @ApiOperation("导出分厂胶料与密炼区对应关系列表")
    @PostMapping("/factoryGlueAreaRelation/getList")
    List<FactoryGlueAreaRelation> getList(@RequestBody FactoryGlueAreaRelation tFactoryGlueAreaRelation);

    /**
     * 导入分厂胶料与密炼区对应关系数据
     */
    @ApiOperation("导入分厂胶料与密炼区对应关系")
    @PostMapping("/factoryGlueAreaRelation/importData")
    public AjaxResult importData(@RequestBody List<FactoryGlueAreaRelation> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);
}
