package com.zlt.aps.dj.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.dj.api.domain.entity.DjAssistSpec;

import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 垫胶外协规格管理对外暴露接口
 */
@FeignClient(contextId = "iNcAssistSpecService", value = ServiceNameConstants.GATEWAY_SERVICE, path="${api.path.dj:nc}")
public interface IDjAssistSpecService {

    /**
     * 根据条件查询外协规格管理列表
     */
    @PostMapping("/dj/assistSpec/listAssistSpec")
    TableDataInfo listAssistSpec(@RequestBody DjAssistSpec dto);

    /**
     * 根据id查询外协规格管理信息
     */
    @GetMapping("/dj/assistSpec/getAssistSpec/{id}")
    DjAssistSpec getAssistSpec(@PathVariable("id") Long id);

    /**
     * 保存外协规格管理信息（id为空则新增，id不为空则修改）
     */
    @PostMapping("/dj/assistSpec/saveAssistSpec")
    AjaxResult saveAssistSpec(@RequestBody DjAssistSpec dto);

    /**
     * 根据code判断代号是否已经存在
     */
    @PostMapping("/dj/assistSpec/checkAssistSpecCodeUnique")
    String checkAssistSpecCodeUnique(@RequestBody DjAssistSpec dto);

    /**
     * 批量删除外协规格管理信息(逻辑删)
     * @param ids 多个id逗号分割
     */
    @PostMapping("/dj/assistSpec/deleteAssistSpec/{ids}")
    AjaxResult deleteAssistSpec(@PathVariable("ids") Long[] ids);

    /**
     * 导出接口
     * @param dto
     */
    @PostMapping("/dj/assistSpec/exportData")
    List<DjAssistSpec> exportData(@RequestBody DjAssistSpec dto);

    @PostMapping("/dj/assistSpec/importData")
    @ApiOperation("导入垫胶外协规格管理")
    public AjaxResult importData(@RequestBody List<DjAssistSpec> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);
}
