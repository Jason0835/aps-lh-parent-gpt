package com.zlt.aps.lh.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.lh.api.domain.entity.LhSpecialMaterialBom;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 特殊物料清单配置前端接口
 *
 * @author zlt
 * @date 2026-05-06
 */
@FeignClient(contextId = "ILhSpecialMaterialBomRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.lh:/lh}")
public interface ILhSpecialMaterialBomRemoteService {

    /**
     * 查询列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/lhSpecialMaterialBom/list")
    TableDataInfo list(@RequestBody LhSpecialMaterialBom queryVO);

    /**
     * 保存
     */
    @ApiOperation("保存")
    @PostMapping("/lhSpecialMaterialBom/save")
    AjaxResult save(@RequestBody LhSpecialMaterialBom lhSpecialMaterialBom);

    /**
     * 删除
     */
    @ApiOperation("删除")
    @DeleteMapping("/lhSpecialMaterialBom/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/lhSpecialMaterialBom/{id}")
    LhSpecialMaterialBom getInfo(@PathVariable("id") Long id);

    /**
     * 校验唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/lhSpecialMaterialBom/checkUnique")
    String checkUnique(@RequestBody LhSpecialMaterialBom lhSpecialMaterialBomVO);

    /**
     * 导出特殊物料清单配置列表
     */
    @ApiOperation("导出列表")
    @PostMapping("/lhSpecialMaterialBom/exportData/{fileName}")
    byte[] exportData(@RequestBody LhSpecialMaterialBom queryVO, @PathVariable("fileName") String fileName);

    /**
     * 导入特殊物料清单配置数据
     */
    @ApiOperation("导入特殊物料清单配置")
    @PostMapping("/lhSpecialMaterialBom/importData/{updateSupport}")
    AjaxResult importData(@RequestBody ImportContext importContext, @PathVariable("updateSupport") boolean updateSupport);
}
