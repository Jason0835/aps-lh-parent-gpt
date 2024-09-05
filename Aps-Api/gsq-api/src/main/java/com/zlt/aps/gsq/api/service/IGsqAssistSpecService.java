package com.zlt.aps.gsq.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.gsq.api.domain.entity.GsqAssistSpec;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 钢丝圈外协规格管理对外暴露接口
 */
@FeignClient(contextId = "iGsqAssistSpecService", value = ServiceNameConstants.GATEWAY_SERVICE, path="${api.path.gsq:gsq}")
public interface IGsqAssistSpecService {

    /**
     * 根据条件查询外协规格管理列表
     */
    @GetMapping("/assistSpec/listAssistSpec")
    TableDataInfo listAssistSpec(@SpringQueryMap GsqAssistSpec dto);

    /**
     * 根据id查询外协规格管理信息
     */
    @GetMapping("/assistSpec/getAssistSpec/{id}")
    GsqAssistSpec getAssistSpec(@PathVariable("id") Long id);

    /**
     * 保存外协规格管理信息（id为空则新增，id不为空则修改）
     */
    @PostMapping("/assistSpec/saveAssistSpec")
    AjaxResult saveAssistSpec(@RequestBody GsqAssistSpec dto);

    /**
     * 根据code判断代号是否已经存在
     */
    @PostMapping("/assistSpec/checkAssistSpecCodeUnique")
    String checkAssistSpecCodeUnique(@RequestBody GsqAssistSpec dto);

    /**
     * 批量删除外协规格管理信息(逻辑删)
     * @param ids 多个id逗号分割
     */
    @PostMapping("/assistSpec/deleteAssistSpec/{ids}")
    AjaxResult deleteAssistSpec(@PathVariable("ids") Long[] ids);

    /**
     * 导出接口
     * @param dto
     */
    @GetMapping("/assistSpec/exportData")
    List<GsqAssistSpec> exportData(@SpringQueryMap GsqAssistSpec dto);

    @PostMapping("/assistSpec/importData")
    @ApiOperation("导入钢丝圈外协规格管理")
    public AjaxResult importData(@RequestBody List<GsqAssistSpec> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);
}
