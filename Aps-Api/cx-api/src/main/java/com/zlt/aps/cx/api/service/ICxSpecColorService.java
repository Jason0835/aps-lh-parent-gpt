package com.zlt.aps.cx.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.cx.api.domain.dto.CxSpecColorDto;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * 规格字体颜色设置Service接口
 *
 * @author chen
 * @date 2021-08-21
 */
@FeignClient(contextId = "ICxSpecColorService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.cx:cx}")
public interface ICxSpecColorService {

    /**
     * 查询规格字体颜色设置列表
     */
    @PostMapping("/specColor/list")
    TableDataInfo list(@RequestBody CxSpecColorDto dto);

    /**
     * 新增规格字体颜色设置
     */
    @PostMapping("/specColor/add")
    AjaxResult add(@RequestBody CxSpecColorDto dto);

    /**
     * 修改规格字体颜色设置
     */
    @PostMapping("/specColor/edit")
    AjaxResult edit(@RequestBody CxSpecColorDto dto);

    /**
     * 删除规格字体颜色设置
     */
    @DeleteMapping("/specColor/{ids}")
    AjaxResult remove(@PathVariable("ids") Long[] ids);

    /**
     * 根据ID获取详细信息
     */
    @GetMapping(value = "/specColor/{id}")
    CxSpecColorDto getInfo(@PathVariable("id") Long id);

    /**
     * 校验规格字体颜色设置唯一性
     */
    @PostMapping("/specColor/checkCxSpecColorUnique")
    String checkCxSpecColorUnique(@RequestBody CxSpecColorDto dto);

    /**
     * 导出规格字体颜色设置列表
     */
    @PostMapping("/specColor/getList")
    List<CxSpecColorDto> getList(@RequestBody CxSpecColorDto dto);

    @PostMapping("/specColor/importData")
    @ApiOperation("导入规格字体颜色设置信息")
    public AjaxResult importData(@RequestBody List<CxSpecColorDto> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);
}
