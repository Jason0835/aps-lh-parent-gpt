package com.zlt.mix.setting.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.mix.setting.api.domain.entity.FhGlueReturnRate;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 返回胶日返回率Service接口
 * @author zlt
 * @date 2022-11-28
 */
@FeignClient(contextId = "IFhGlueReturnRateService", value =ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.schedule:mixSchedule}")
public interface IFhGlueReturnRateService {

    /**
     * 查询返回胶日返回率列表
     */
    @PostMapping("/fhGlueRate/list")
    TableDataInfo listFhGlueReturnRate(@RequestBody FhGlueReturnRate fhGlueReturnRate);

    /**
    * 根据ID获取详细信息
    */
    @GetMapping(value = "/fhGlueRate/{id}")
    FhGlueReturnRate getFhGlueReturnRateInfo(@PathVariable("id") Long id);

    /**
    * 保存返回胶日返回率信息（id为空则新增，id不为空则修改）
    */
    @PostMapping("/fhGlueRate/save")
    AjaxResult saveFhGlueReturnRate(@RequestBody FhGlueReturnRate fhGlueReturnRate);

    /**
     * 批量删除返回胶日返回率
     */
    @PostMapping("/fhGlueRate/delete/{ids}")
    AjaxResult deleteFhGlueReturnRate(@PathVariable("ids") Long[] ids);

    /**
     * 校验返回胶日返回率唯一性
     */
    @ApiOperation("校验返回胶日返回率唯一性")
    @PostMapping("/fhGlueRate/checkFhGlueReturnRateUnique")
    String checkFhGlueReturnRateUnique(@RequestBody FhGlueReturnRate fhGlueReturnRate);

    /**
     * 导出返回胶日返回率列表
     */
    @PostMapping("/fhGlueRate/exportData")
    List<FhGlueReturnRate> exportData(@RequestBody FhGlueReturnRate fhGlueReturnRate);

    /**
     * 导入返回胶日返回率数据
     */
    @ApiOperation("导入返回胶日返回率")
    @PostMapping("/fhGlueRate/importData")
    public AjaxResult importData(@RequestBody List<FhGlueReturnRate> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);
}
