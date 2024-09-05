package com.zlt.aps.xwyy.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.xwyy.api.domain.dto.XwyyBigRollColorDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 帘布大卷颜色提示信息维护对外暴露接口
 */
@FeignClient(contextId = "xwyyBigRollColorService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.xwyy:xwyy}")
public interface XwyyBigRollColorService {

    /**
     * 根据条件查询帘布大卷颜色提示信息维护列表
     */
    @PostMapping("/xwyyBigRollColor/listXwyyBigRollColor")
    TableDataInfo listXwyyBigRollColor(@RequestBody XwyyBigRollColorDto dto);

    /**
     * 根据id查询帘布大卷颜色提示信息维护
     */
    @GetMapping("/xwyyBigRollColor/getXwyyBigRollColor/{id}")
    XwyyBigRollColorDto getXwyyBigRollColor(@PathVariable("id") Long id);

    /**
     * 保存帘布大卷颜色提示信息维护信息（id为空则新增，id不为空则修改）
     */
    @PostMapping("/xwyyBigRollColor/saveXwyyBigRollColor")
    AjaxResult saveXwyyBigRollColor(@RequestBody XwyyBigRollColorDto dto);

    /**
     * 保存帘布大卷颜色提示信息维护信息（id为空则新增，id不为空则修改）
     */
    @PostMapping("/xwyyBigRollColor/checkXwyyBigRollColor")
    String checkXwyyBigRollColor(@RequestBody XwyyBigRollColorDto dto);

    /**
     * 批量删除帘布大卷颜色提示信息维护信息(逻辑删)
     *
     * @param ids 多个id逗号分割
     */
    @PostMapping("/xwyyBigRollColor/deleteXwyyBigRollColor/{ids}")
    AjaxResult deleteXwyyBigRollColor(@PathVariable("ids") Long[] ids);

    /**
     * 导出接口
     *
     * @param dto
     */
    @GetMapping("/xwyyBigRollColor/exportData")
    List<XwyyBigRollColorDto> exportData(@SpringQueryMap XwyyBigRollColorDto dto);

    /**
     * 导入数据
     */
    @PostMapping("/xwyyBigRollColor/importData")
    public AjaxResult importData(@RequestBody List<XwyyBigRollColorDto> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);
}
