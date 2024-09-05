package com.zlt.aps.cx.api.service;

import com.ruoyi.api.gateway.system.domain.Ztree;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.cx.api.domain.dto.BomInfoDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.io.IOException;
import java.util.List;

/**
 * BOM信息Controller
 *
 * @author Chen
 * @date 2021-06-11
 */
@FeignClient(contextId = "ICxBomInfoService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.cx:cx}")
public interface ICxBomInfoService {

    /**
     * 查询BOM信息列表
     */
    @PostMapping("/cx/bom/list")
    public List<BomInfoDto> list(@RequestBody BomInfoDto dto);

    /**
     * 导出BOM信息列表
     */
    @PostMapping("/cx/bom/export")
    public List<BomInfoDto> export(BomInfoDto dto) throws IOException;

    /**
     * 获取BOM信息详细信息
     */
    @GetMapping(value = "/cx/bom/{id}")
    public BomInfoDto getInfo(@PathVariable("id") Long id);

    /**
     * 新增BOM信息
     */
    @PostMapping("/cx/bom/add")
    public AjaxResult add(@RequestBody BomInfoDto dto);

    /**
     * 修改BOM信息
     */
    @PostMapping("/cx/bom/edit")
    public AjaxResult edit(@RequestBody BomInfoDto dto);

    /**
     * 删除BOM信息
     */
    @GetMapping("/cx/bom/remove/{id}")
    public AjaxResult remove(@PathVariable("id") Long id);

    /**
     * 获取树形下拉列表数据
     *
     * @return 结果
     */
    @PostMapping("/cx/bom/getTreeData")
    List<Ztree> getTreeData();

    /**
     * 根据排除条件查询bom数据
     *
     * @param dto 排除条件
     * @return 结果
     */
    @PostMapping("/cx/bom/selectBomInfoExcludeById")
    public BomInfoDto selectBomInfoExcludeById(@RequestBody BomInfoDto dto);
}
