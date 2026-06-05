package com.zlt.aps.cd15.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.cd15.api.domain.dto.Cd15MachineRollMappingDto;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * 15度裁断钢压大卷与机台的映射表对外暴露接口
 * </p>
 *
 * @author duanjuntao
 * @since 2021-06-15
 */
@FeignClient(contextId = "ICd15MachineRollMappingService", value = ServiceNameConstants.GATEWAY_SERVICE, path="${api.path.cd15:cd15}")
public interface ICd15MachineRollMappingService {
    /**
     * 根据条件查询钢压大卷与机台的映射表
     */
    @PostMapping("/cd15/MachineRollMapping/listMachineRollMapping")
    TableDataInfo listMachineRollMapping(@RequestBody Cd15MachineRollMappingDto dto);

    /**
     * 根据id查询钢压大卷与机台的映射表
     */
    @GetMapping("/cd15/MachineRollMapping/getMachineRollMapping/{id}")
    Cd15MachineRollMappingDto getBigRollColor(@PathVariable("id") Long id);

    /**
     * 保存钢压大卷与机台的映射表（id为空则新增，id不为空则修改）
     */
    @PostMapping("/cd15/MachineRollMapping/saveMachineRollMapping")
    AjaxResult saveMachineRollMapping(@RequestBody Cd15MachineRollMappingDto dto);

    /**
     * 保存钢压大卷与机台的映射表（id为空则新增，id不为空则修改）
     */
    @PostMapping("/cd15/MachineRollMapping/checkMachineRollMapping")
    String checkMachineRollMapping(@RequestBody Cd15MachineRollMappingDto dto);

    /**
     * 批量删除钢压大卷与机台的映射表(逻辑删)
     * @param ids 多个id逗号分割
     */
    @PostMapping("/cd15/MachineRollMapping/deleteMachineRollMapping/{ids}")
    AjaxResult deleteMachineRollMapping(@PathVariable("ids") Long[] ids);

    /**
     * 导出接口
     * @param dto
     */
    @PostMapping("/cd15/MachineRollMapping/exportData")
    List<Cd15MachineRollMappingDto> exportData(@RequestBody Cd15MachineRollMappingDto dto);

    @PostMapping("/cd15/MachineRollMapping/importData")
    @ApiOperation("导入15度裁断钢压大卷与机台映射信息")
    public AjaxResult importData(@RequestBody List<Cd15MachineRollMappingDto> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);

    /**
     * 删除全部(逻辑删)
     */
    @PostMapping("/cd15/MachineRollMapping/deleteAll")
    AjaxResult deleteAll();
}
