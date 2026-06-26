package com.zlt.aps.gdyy.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.gdyy.api.domain.dto.GdyyQuotaSettingDto;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * 钢带压延定额设定Service接口
 *
 * @author chen
 * @date 2021-06-30
 */
@FeignClient(contextId = "IGdyyQuotaSettingService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.gdyy:gdyy}")
public interface IGdyyQuotaSettingService {

    /**
     * 查询钢带压延定额设定列表
     *
     * @param dto 钢带压延定额设定
     * @return 钢带压延定额设定集合
     */
    @PostMapping("/gdyy/quota/list")
    @ApiOperation("查询钢带压延定额设定信息维护列表")
    public TableDataInfo list(@RequestBody GdyyQuotaSettingDto dto);

    /**
     * 查询钢带压延定额设定
     *
     * @param id 钢带压延定额设定ID
     * @return 钢带压延定额设定
     */
    @GetMapping("/gdyy/quota/{id}")
    @ApiOperation("查询钢带压延定额设定信息维护列表")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", dataType = "int", value = "主键id", paramType = "query")
    })
    public GdyyQuotaSettingDto getInfo(@PathVariable("id") Long id);

    /**
     * 修改钢带压延定额设定
     *
     * @param dto 钢带压延定额设定
     * @return 结果
     */
    @PostMapping("/gdyy/quota/edit")
    @ApiOperation("修改钢带压延定额设定（id为空则新增，id不为空则修改）")
    public AjaxResult edit(@RequestBody GdyyQuotaSettingDto dto);

    /**
     * 删除钢带压延定额设定
     *
     * @param ids 需要删除的钢带压延定额设定ID
     * @return 结果
     */
    @PostMapping("/gdyy/quota/{ids}")
    @ApiOperation("删除钢带压延定额设定信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "ids", dataType = "Array", value = "id数组", paramType = "query")
    })
    public AjaxResult remove(@PathVariable("ids") Long[] ids);

    /**
     * 导出钢带压延定额设定信息
     */
    @PostMapping("/gdyy/quota/export")
    @ApiOperation("导出钢带压延定额设定信息")
    public List<GdyyQuotaSettingDto> exportData(@RequestBody GdyyQuotaSettingDto dto);


    @PostMapping("/gdyy/quota/importData")
    @ApiOperation("导入钢带压延定额设定信息")
    public AjaxResult importData(@RequestBody List<GdyyQuotaSettingDto> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);

}
