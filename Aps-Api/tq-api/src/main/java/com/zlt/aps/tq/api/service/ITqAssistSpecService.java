package com.zlt.aps.tq.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.tq.api.domain.entity.TqAssistSpec;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 胎圈外协规格管理对外暴露接口
 */
@FeignClient(contextId = "iTqAssistSpecService", value = ServiceNameConstants.GATEWAY_SERVICE, path="${api.path.tq:tq}")
public interface ITqAssistSpecService {

    /**
     * 根据条件查询外协规格管理列表
     */
    @PostMapping("/tq/assistSpec/listAssistSpec")
    TableDataInfo listAssistSpec(@RequestBody TqAssistSpec dto);

    /**
     * 根据id查询外协规格管理信息
     */
    @GetMapping("/tq/assistSpec/getAssistSpec/{id}")
    TqAssistSpec getAssistSpec(@PathVariable("id") Long id);

    /**
     * 保存外协规格管理信息（id为空则新增，id不为空则修改）
     */
    @PostMapping("/tq/assistSpec/saveAssistSpec")
    AjaxResult saveAssistSpec(@RequestBody TqAssistSpec dto);

    /**
     * 根据code判断代号是否已经存在
     */
    @PostMapping("/tq/assistSpec/checkAssistSpecCodeUnique")
    String checkAssistSpecCodeUnique(@RequestBody TqAssistSpec dto);

    /**
     * 批量删除外协规格管理信息(逻辑删)
     * @param ids 多个id逗号分割
     */
    @PostMapping("/tq/assistSpec/deleteAssistSpec/{ids}")
    AjaxResult deleteAssistSpec(@PathVariable("ids") Long[] ids);

    /**
     * 导出接口
     * @param dto
     */
    @PostMapping("/tq/assistSpec/exportData")
    List<TqAssistSpec> exportData(@RequestBody TqAssistSpec dto);

    @PostMapping("/tq/assistSpec/importData")
    @ApiOperation("导入胎圈外协规格管理")
    public AjaxResult importData(@RequestBody List<TqAssistSpec> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);
}
