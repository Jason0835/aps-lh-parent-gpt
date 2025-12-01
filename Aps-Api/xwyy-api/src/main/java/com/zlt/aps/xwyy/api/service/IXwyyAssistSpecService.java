package com.zlt.aps.xwyy.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.xwyy.api.domain.entity.XwyyAssistSpec;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 纤维压延外协规格管理对外暴露接口
 */
@FeignClient(contextId = "iXwyyAssistSpecService", value = ServiceNameConstants.GATEWAY_SERVICE, path="${api.path.xwyy:xwyy}")
public interface IXwyyAssistSpecService {

    /**
     * 根据条件查询外协规格管理列表
     */
    @PostMapping("/xwyy/assistSpec/listAssistSpec")
    TableDataInfo listAssistSpec(@RequestBody XwyyAssistSpec dto);

    /**
     * 根据id查询外协规格管理信息
     */
    @GetMapping("/xwyy/assistSpec/getAssistSpec/{id}")
    XwyyAssistSpec getAssistSpec(@PathVariable("id") Long id);

    /**
     * 保存外协规格管理信息（id为空则新增，id不为空则修改）
     */
    @PostMapping("/xwyy/assistSpec/saveAssistSpec")
    AjaxResult saveAssistSpec(@RequestBody XwyyAssistSpec dto);

    /**
     * 根据code判断代号是否已经存在
     */
    @PostMapping("/xwyy/assistSpec/checkAssistSpecCodeUnique")
    String checkAssistSpecCodeUnique(@RequestBody XwyyAssistSpec dto);

    /**
     * 批量删除外协规格管理信息(逻辑删)
     * @param ids 多个id逗号分割
     */
    @PostMapping("/xwyy/assistSpec/deleteAssistSpec/{ids}")
    AjaxResult deleteAssistSpec(@PathVariable("ids") Long[] ids);

    /**
     * 导出接口
     * @param dto
     */
    @PostMapping("/xwyy/assistSpec/exportData")
    List<XwyyAssistSpec> exportData(@RequestBody XwyyAssistSpec dto);

    @PostMapping("/xwyy/assistSpec/importData")
    @ApiOperation("导入纤维压延外协规格管理")
    public AjaxResult importData(@RequestBody List<XwyyAssistSpec> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);
}
