package com.zlt.aps.cx.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.cx.api.domain.dto.CxQuotaSettingDto;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 成型定额设定Service接口
 *
 * @author chen
 * @date 2021-06-16
 */
@FeignClient(contextId = "ICxQuotaSettingService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.cx:cx}")
public interface ICxQuotaSettingService {
    /**
     * 查询成型定额设定列表
     *
     * @param dto 成型定额设定
     * @return 成型定额设定集合
     */
    @PostMapping("/cx/quota/list")
    @ApiOperation("查询成型定额设定信息维护列表")
    public TableDataInfo list(@RequestBody CxQuotaSettingDto dto);

    /**
     * 查询成型定额设定
     *
     * @param id 成型定额设定ID
     * @return 成型定额设定
     */
    @GetMapping("/cx/quota/{id}")
    @ApiOperation("查询成型定额设定信息维护列表")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", dataType = "int", value = "主键id", paramType = "query")
    })
    public CxQuotaSettingDto getInfo(@PathVariable("id") Long id);

    /**
     * 修改成型定额设定
     *
     * @param dto 成型定额设定
     * @return 结果
     */
    @PostMapping("/cx/quota/edit")
    @ApiOperation("修改成型定额设定（id为空则新增，id不为空则修改）")
    public AjaxResult edit(@RequestBody CxQuotaSettingDto dto);

    /**
     * 删除成型定额设定
     *
     * @param ids 需要删除的成型定额设定ID
     * @return 结果
     */
    @PostMapping("/cx/quota/{ids}")
    @ApiOperation("删除成型定额设定信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "ids", dataType = "Array", value = "id数组", paramType = "query")
    })
    public AjaxResult remove(@PathVariable("ids") Long[] ids);

    /**
     * 导出成型定额设定信息
     */
    @PostMapping("/cx/quota/export")
    @ApiOperation("导出成型定额设定信息")
    public List<CxQuotaSettingDto> exportData(@SpringQueryMap CxQuotaSettingDto dto);

    /**
     * 导入数据
     */
    @PostMapping("/cx/quota/importData")
    public AjaxResult importData(@RequestBody List<CxQuotaSettingDto> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);
}
