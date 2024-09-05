package com.zlt.aps.cd15.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.cd15.api.domain.dto.Cd15BigRollDto;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 15度裁断钢压大卷信息对外暴露接口
 */
@FeignClient(contextId = "iCd15BigRollService", value = ServiceNameConstants.GATEWAY_SERVICE, path="${api.path.cd15:cd15}")
public interface ICd15BigRollService {

    /**
     * 根据条件查询钢压大卷信息列表
     */
    @GetMapping("/bigRoll/listBigRoll")
    TableDataInfo listBigRoll(@SpringQueryMap Cd15BigRollDto dto);

    /**
     * 根据id查询钢压大卷信息信息
     */
    @GetMapping("/bigRoll/getBigRoll/{id}")
    Cd15BigRollDto getBigRoll(@PathVariable("id") Long id);

    /**
     * 保存钢压大卷信息信息（id为空则新增，id不为空则修改）
     */
    @PostMapping("/bigRoll/saveBigRoll")
    AjaxResult saveBigRoll(@RequestBody Cd15BigRollDto dto);

    /**
     * 根据code判断钢压大卷代号是否已经存在
     */
    @PostMapping("/bigRoll/checkBigRollCodeUnique")
    String checkBigRollCodeUnique(@RequestBody Cd15BigRollDto dto);

    /**
     * 批量删除钢压大卷信息信息(逻辑删)
     * @param ids 多个id逗号分割
     */
    @PostMapping("/bigRoll/deleteBigRoll/{ids}")
    AjaxResult deleteBigRoll(@PathVariable("ids") Long[] ids);

    /**
     * 导出接口
     * @param dto
     */
    @GetMapping("/bigRoll/exportData")
    List<Cd15BigRollDto> exportData(@SpringQueryMap Cd15BigRollDto dto);

    @PostMapping("/bigRoll/importData")
    @ApiOperation("导入15度裁断钢压大卷信息")
    public AjaxResult importData(@RequestBody List<Cd15BigRollDto> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);
}
