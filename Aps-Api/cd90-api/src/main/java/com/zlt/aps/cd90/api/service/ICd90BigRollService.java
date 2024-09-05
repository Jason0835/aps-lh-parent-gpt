package com.zlt.aps.cd90.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.cd90.api.domain.dto.Cd90BigRollDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 90度裁断帘布大卷信息对外暴露接口
 */
@FeignClient(contextId = "iCd90BigRollService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.cd90:cd90}")
public interface ICd90BigRollService {

    /**
     * 根据条件查询帘布大卷信息列表
     */
    @GetMapping("/bigRoll/listBigRoll")
    TableDataInfo listBigRoll(@SpringQueryMap Cd90BigRollDto dto);

    /**
     * 根据id查询帘布大卷信息信息
     */
    @GetMapping("/bigRoll/getBigRoll/{id}")
    Cd90BigRollDto getBigRoll(@PathVariable("id") Long id);

    /**
     * 保存帘布大卷信息信息（id为空则新增，id不为空则修改）
     */
    @PostMapping("/bigRoll/saveBigRoll")
    AjaxResult saveBigRoll(@RequestBody Cd90BigRollDto dto);

    /**
     * 根据code判断纤维大卷代号是否已经存在
     */
    @PostMapping("/bigRoll/checkBigRollCodeUnique")
    String checkBigRollCodeUnique(@RequestBody Cd90BigRollDto dto);

    /**
     * 批量删除帘布大卷信息信息(逻辑删)
     *
     * @param ids 多个id逗号分割
     */
    @PostMapping("/bigRoll/deleteBigRoll/{ids}")
    AjaxResult deleteBigRoll(@PathVariable("ids") Long[] ids);

    /**
     * 导出接口
     *
     * @param dto
     */
    @GetMapping("/bigRoll/exportData")
    List<Cd90BigRollDto> exportData(@SpringQueryMap Cd90BigRollDto dto);

    /**
     * 导入数据
     */
    @PostMapping("/bigRoll/importData")
    public AjaxResult importData(@RequestBody List<Cd90BigRollDto> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);
}
