package com.zlt.aps.xwyy.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.xwyy.api.domain.dto.XwyyMachineRollMappingDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * 纤维压延帘布大卷与机台的映射表对外暴露接口
 * </p>
 *
 * @author duanjuntao
 * @since 2021-06-15
 */
@FeignClient(contextId = "XwyyMachineRollMappingService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.xwyy:xwyy}")
public interface XwyyMachineRollMappingService {
    /**
     * 根据条件查询纤维压延帘布大卷与机台的映射表
     */
    @PostMapping("/xwyyMachineRollMapping/listXwyyMachineRollMapping")
    TableDataInfo listXwyyMachineRollMapping(@RequestBody XwyyMachineRollMappingDto dto);

    /**
     * 根据id查询纤维压延帘布大卷与机台的映射表
     */
    @GetMapping("/xwyyMachineRollMapping/getXwyyMachineRollMapping/{id}")
    XwyyMachineRollMappingDto getXwyyBigRollColor(@PathVariable("id") Long id);

    /**
     * 保存纤维压延帘布大卷与机台的映射表（id为空则新增，id不为空则修改）
     */
    @PostMapping("/xwyyMachineRollMapping/saveXwyyMachineRollMapping")
    AjaxResult saveXwyyMachineRollMapping(@RequestBody XwyyMachineRollMappingDto dto);

    /**
     * 检查帘纤维压延帘布大卷与机台的映射表数据是否已存在
     */
    @PostMapping("/xwyyMachineRollMapping/checkXwyyMachineRollMapping")
    String checkXwyyMachineRollMapping(@RequestBody XwyyMachineRollMappingDto dto);

    /**
     * 批量删除纤维压延帘布大卷与机台的映射表(逻辑删)
     *
     * @param ids 多个id逗号分割
     */
    @PostMapping("/xwyyMachineRollMapping/deleteXwyyMachineRollMapping/{ids}")
    AjaxResult deleteXwyyMachineRollMapping(@PathVariable("ids") Long[] ids);

    /**
     * 导出接口
     *
     * @param dto
     */
    @PostMapping("/xwyyMachineRollMapping/exportData")
    List<XwyyMachineRollMappingDto> exportData(@RequestBody XwyyMachineRollMappingDto dto);

    /**
     * 导入数据
     */
    @PostMapping("/xwyyMachineRollMapping/importData")
    public AjaxResult importData(@RequestBody List<XwyyMachineRollMappingDto> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);
}
