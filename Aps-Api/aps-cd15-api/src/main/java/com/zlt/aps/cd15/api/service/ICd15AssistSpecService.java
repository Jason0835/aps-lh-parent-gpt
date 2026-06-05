package com.zlt.aps.cd15.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.cd15.api.domain.entity.Cd15AssistSpec;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 15度裁断外协规格管理对外暴露接口
 */
@FeignClient(contextId = "iCd15AssistSpecService", value = ServiceNameConstants.GATEWAY_SERVICE, path="${api.path.cd15:cd15}")
public interface ICd15AssistSpecService {

    /**
     * 根据条件查询外协规格管理列表
     */
    @PostMapping("/cd15/assistSpec/listAssistSpec")
    TableDataInfo listAssistSpec(@RequestBody Cd15AssistSpec dto);

    /**
     * 根据id查询外协规格管理信息
     */
    @GetMapping("/cd15/assistSpec/getAssistSpec/{id}")
    Cd15AssistSpec getAssistSpec(@PathVariable("id") Long id);

    /**
     * 保存外协规格管理信息（id为空则新增，id不为空则修改）
     */
    @PostMapping("/cd15/assistSpec/saveAssistSpec")
    AjaxResult saveAssistSpec(@RequestBody Cd15AssistSpec dto);

    /**
     * 根据code判断钢压大卷代号是否已经存在
     */
    @PostMapping("/cd15/assistSpec/checkAssistSpecCodeUnique")
    String checkAssistSpecCodeUnique(@RequestBody Cd15AssistSpec dto);

    /**
     * 批量删除外协规格管理信息(逻辑删)
     * @param ids 多个id逗号分割
     */
    @PostMapping("/cd15/assistSpec/deleteAssistSpec/{ids}")
    AjaxResult deleteAssistSpec(@PathVariable("ids") Long[] ids);

    /**
     * 导出接口
     * @param dto
     */
    @PostMapping("/cd15/assistSpec/exportData")
    List<Cd15AssistSpec> exportData(@RequestBody Cd15AssistSpec dto);

    @PostMapping("/cd15/assistSpec/importData")
    @ApiOperation("导入15度裁断外协规格管理")
    public AjaxResult importData(@RequestBody List<Cd15AssistSpec> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);
}
