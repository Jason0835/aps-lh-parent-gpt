package com.zlt.aps.tm.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.tm.api.domain.entity.TmAssistSpec;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 胎面外协规格管理对外暴露接口
 */
@FeignClient(contextId = "iTmAssistSpecService", value = ServiceNameConstants.GATEWAY_SERVICE, path="${api.path.tm:tm}")
public interface ITmAssistSpecService {

    /**
     * 根据条件查询外协规格管理列表
     */
    @PostMapping("/tm/assistSpec/listAssistSpec")
    TableDataInfo listAssistSpec(@RequestBody TmAssistSpec dto);

    /**
     * 根据id查询外协规格管理信息
     */
    @GetMapping("/tm/assistSpec/getAssistSpec/{id}")
    TmAssistSpec getAssistSpec(@PathVariable("id") Long id);

    /**
     * 保存外协规格管理信息（id为空则新增，id不为空则修改）
     */
    @PostMapping("/tm/assistSpec/saveAssistSpec")
    AjaxResult saveAssistSpec(@RequestBody TmAssistSpec dto);

    /**
     * 根据code判断代号是否已经存在
     */
    @PostMapping("/tm/assistSpec/checkAssistSpecCodeUnique")
    String checkAssistSpecCodeUnique(@RequestBody TmAssistSpec dto);

    /**
     * 批量删除外协规格管理信息(逻辑删)
     * @param ids 多个id逗号分割
     */
    @PostMapping("/tm/assistSpec/deleteAssistSpec/{ids}")
    AjaxResult deleteAssistSpec(@PathVariable("ids") Long[] ids);

    /**
     * 导出接口
     * @param dto
     */
    @PostMapping("/tm/assistSpec/exportData")
    List<TmAssistSpec> exportData(@RequestBody TmAssistSpec dto);

    @PostMapping("/tm/assistSpec/importData")
    @ApiOperation("导入胎面外协规格管理")
    public AjaxResult importData(@RequestBody List<TmAssistSpec> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);
}
