package com.zlt.aps.cx.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.cx.api.domain.dto.ConstructionInfoDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 施工信息对外暴露接口
 */
@FeignClient(contextId = "iCxConstructionInfoService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.cx:cx}")
public interface ICxConstructionInfoService {

    /**
     * 根据条件查询胶料组别顺序列表
     */
    @GetMapping("/constructionInfo/listConstructionInfo")
    TableDataInfo listConstructionInfo(@SpringQueryMap ConstructionInfoDto dto);

    /**
     * 根据id查询胶料组别顺序信息
     */
    @GetMapping("/constructionInfo/getConstructionInfo/{id}")
    ConstructionInfoDto getConstructionInfo(@PathVariable("id") Long id);

    /**
     * 保存胶料组别顺序信息（id为空则新增，id不为空则修改）
     */
    @PostMapping("/constructionInfo/saveConstructionInfo")
    AjaxResult saveConstructionInfo(@RequestBody ConstructionInfoDto dto);

    /**
     * 校验胎胚代码唯一性
     */
    @PostMapping("/constructionInfo/checkEmbryoCodeUnique")
    String checkEmbryoCodeUnique(@RequestBody ConstructionInfoDto dto);

    /**
     * 批量删除胶料组别顺序信息(逻辑删)
     *
     * @param ids 多个id逗号分割
     */
    @PostMapping("/constructionInfo/deleteConstructionInfo/{ids}")
    AjaxResult deleteConstructionInfo(@PathVariable("ids") Long[] ids);

    /**
     * 导出接口
     *
     * @param dto
     */
    @GetMapping("/constructionInfo/exportData")
    List<ConstructionInfoDto> exportData(@SpringQueryMap ConstructionInfoDto dto);

    /**
     * 导入数据
     */
    @PostMapping("/constructionInfo/importData")
    public AjaxResult importData(@RequestBody List<ConstructionInfoDto> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);

}
