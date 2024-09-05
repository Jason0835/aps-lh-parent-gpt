package com.zlt.aps.xwyy.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.xwyy.api.domain.entity.XwyyOriginalLineSpec;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 纤维压延原线规格管理对外暴露接口
 */
@FeignClient(contextId = "iXwyyOriginalLineSpecService", value = ServiceNameConstants.GATEWAY_SERVICE, path="${api.path.xwyy:xwyy}")
public interface IXwyyOriginalLineSpecService {

    /**
     * 根据条件查询原线规格管理列表
     */
    @GetMapping("/originalLineSpec/listOriginalLineSpec")
    TableDataInfo listOriginalLineSpec(@SpringQueryMap XwyyOriginalLineSpec dto);

    /**
     * 根据id查询原线规格管理信息
     */
    @GetMapping("/originalLineSpec/getOriginalLineSpec/{id}")
    XwyyOriginalLineSpec getOriginalLineSpec(@PathVariable("id") Long id);

    /**
     * 保存原线规格管理信息（id为空则新增，id不为空则修改）
     */
    @PostMapping("/originalLineSpec/saveOriginalLineSpec")
    AjaxResult saveOriginalLineSpec(@RequestBody XwyyOriginalLineSpec dto);

    /**
     * 根据code判断代号是否已经存在
     */
    @PostMapping("/originalLineSpec/checkOriginalLineSpecCodeUnique")
    String checkOriginalLineSpecCodeUnique(@RequestBody XwyyOriginalLineSpec dto);

    /**
     * 批量删除原线规格管理信息(逻辑删)
     * @param ids 多个id逗号分割
     */
    @PostMapping("/originalLineSpec/deleteOriginalLineSpec/{ids}")
    AjaxResult deleteOriginalLineSpec(@PathVariable("ids") Long[] ids);

    /**
     * 导出接口
     * @param dto
     */
    @GetMapping("/originalLineSpec/exportData")
    List<XwyyOriginalLineSpec> exportData(@SpringQueryMap XwyyOriginalLineSpec dto);

    @PostMapping("/originalLineSpec/importData")
    @ApiOperation("导入纤维压延原线规格管理")
    public AjaxResult importData(@RequestBody List<XwyyOriginalLineSpec> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);
}
