package com.zlt.aps.cd90.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.cd90.api.domain.entity.Cd90AssistSpec;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 90度裁断外协规格管理对外暴露接口
 */
@FeignClient(contextId = "iCd90AssistSpecService", value = ServiceNameConstants.GATEWAY_SERVICE, path="${api.path.cd90:cd90}")
public interface ICd90AssistSpecService {

    /**
     * 根据条件查询外协规格管理列表
     */
    @PostMapping("/cd90/assistSpec/listAssistSpec")
    TableDataInfo listAssistSpec(@RequestBody Cd90AssistSpec dto);

    /**
     * 根据id查询外协规格管理信息
     */
    @GetMapping("/cd90/assistSpec/getAssistSpec/{id}")
    Cd90AssistSpec getAssistSpec(@PathVariable("id") Long id);

    /**
     * 保存外协规格管理信息（id为空则新增，id不为空则修改）
     */
    @PostMapping("/cd90/assistSpec/saveAssistSpec")
    AjaxResult saveAssistSpec(@RequestBody Cd90AssistSpec dto);

    /**
     * 根据code判断代号是否已经存在
     */
    @PostMapping("/cd90/assistSpec/checkAssistSpecCodeUnique")
    String checkAssistSpecCodeUnique(@RequestBody Cd90AssistSpec dto);

    /**
     * 批量删除外协规格管理信息(逻辑删)
     * @param ids 多个id逗号分割
     */
    @PostMapping("/cd90/assistSpec/deleteAssistSpec/{ids}")
    AjaxResult deleteAssistSpec(@PathVariable("ids") Long[] ids);

    /**
     * 导出接口
     * @param dto
     */
    @PostMapping("/cd90/assistSpec/exportData")
    List<Cd90AssistSpec> exportData(@RequestBody Cd90AssistSpec dto);

    @PostMapping("/cd90/assistSpec/importData")
    @ApiOperation("导入90度裁断外协规格管理")
    public AjaxResult importData(@RequestBody List<Cd90AssistSpec> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);
}
