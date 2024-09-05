package com.zlt.aps.nc.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.nc.api.domain.entity.NcAssistSpec;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 内衬外协规格管理对外暴露接口
 */
@FeignClient(contextId = "iNcAssistSpecService", value = ServiceNameConstants.GATEWAY_SERVICE, path="${api.path.nc:nc}")
public interface INcAssistSpecService {

    /**
     * 根据条件查询外协规格管理列表
     */
    @GetMapping("/assistSpec/listAssistSpec")
    TableDataInfo listAssistSpec(@SpringQueryMap NcAssistSpec dto);

    /**
     * 根据id查询外协规格管理信息
     */
    @GetMapping("/assistSpec/getAssistSpec/{id}")
    NcAssistSpec getAssistSpec(@PathVariable("id") Long id);

    /**
     * 保存外协规格管理信息（id为空则新增，id不为空则修改）
     */
    @PostMapping("/assistSpec/saveAssistSpec")
    AjaxResult saveAssistSpec(@RequestBody NcAssistSpec dto);

    /**
     * 根据code判断代号是否已经存在
     */
    @PostMapping("/assistSpec/checkAssistSpecCodeUnique")
    String checkAssistSpecCodeUnique(@RequestBody NcAssistSpec dto);

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
    List<NcAssistSpec> exportData(@SpringQueryMap NcAssistSpec dto);

    @PostMapping("/assistSpec/importData")
    @ApiOperation("导入内衬外协规格管理")
    public AjaxResult importData(@RequestBody List<NcAssistSpec> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);
}
