package com.zlt.mix.setting.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.mix.setting.api.domain.entity.MesBasMaterial;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * 物料Service接口
 * @author Joran.zhang
 * @date 2022-05-30
 */
@FeignClient(contextId = "IMesBasMaterialService", value =ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.schedule:mixSchedule}")
public interface IMesBasMaterialService {

    /**
     * 查询物料列表
     */
    @PostMapping("/material/list")
    TableDataInfo listMesBasMaterial(@RequestBody MesBasMaterial mesBasMaterial);

    /**
    * 根据ID获取详细信息
    */
    @GetMapping(value = "/material/{id}")
    MesBasMaterial getMesBasMaterialInfo(@PathVariable("id") Long id);

    /**
    * 保存物料信息（id为空则新增，id不为空则修改）
    */
    @PostMapping("/material/save")
    AjaxResult saveMesBasMaterial(@RequestBody MesBasMaterial mesBasMaterial);

    /**
     * 批量删除物料
     */
    @PostMapping("/material/delete/{ids}")
    AjaxResult deleteMesBasMaterial(@PathVariable("ids") Long[] ids);

    /**
     * 校验物料唯一性
     */
    @ApiOperation("校验物料唯一性")
    @PostMapping("/material/checkMesBasMaterialUnique")
    String checkMesBasMaterialUnique(@RequestBody MesBasMaterial mesBasMaterial);

    /**
     * 导出物料列表
     */
    @PostMapping("/material/exportData")
    List<MesBasMaterial> exportData(@RequestBody MesBasMaterial mesBasMaterial);

    /**
     * 导入物料数据
     */
    @ApiOperation("导入物料")
    @PostMapping("/material/importData")
    public AjaxResult importData(@RequestBody List<MesBasMaterial> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);

    /**
     * 汇总胶料需求计划选机台
     */
    @PostMapping("/material/chooseGlue")
    public AjaxResult chooseGlue(@RequestBody MesBasMaterial mesBasMaterial);

    /**
     * 根据物料大类列表查询物料名称列表
     */
    @PostMapping("/material/listMaterialName")
    List<String> listMaterialName(@RequestBody List<Integer> majorTypes);
}
