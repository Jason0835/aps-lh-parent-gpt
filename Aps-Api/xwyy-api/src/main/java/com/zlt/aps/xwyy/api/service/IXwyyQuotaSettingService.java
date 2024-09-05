package com.zlt.aps.xwyy.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.xwyy.api.domain.dto.XwyyQuotaSettingDto;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * 纤维压延定额设定Service接口
 *
 * @author chen
 * @date 2021-06-29
 */
@FeignClient(contextId = "IXwyyQuotaSettingService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.xwyy:xwyy}")
public interface IXwyyQuotaSettingService {

    /**
     * 查询纤维压延定额设定列表
     *
     * @param dto 纤维压延定额设定
     * @return 纤维压延定额设定集合
     */
    @PostMapping("/xwyy/quota/list")
    @ApiOperation("查询纤维压延定额设定信息维护列表")
    public TableDataInfo list(@RequestBody XwyyQuotaSettingDto dto);

    /**
     * 查询纤维压延定额设定
     *
     * @param id 纤维压延定额设定ID
     * @return 纤维压延定额设定
     */
    @GetMapping("/xwyy/quota/{id}")
    @ApiOperation("查询纤维压延定额设定信息维护列表")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", dataType = "int", value = "主键id", paramType = "query")
    })
    public XwyyQuotaSettingDto getInfo(@PathVariable("id") Long id);

    /**
     * 修改纤维压延定额设定
     *
     * @param dto 纤维压延定额设定
     * @return 结果
     */
    @PostMapping("/xwyy/quota/edit")
    @ApiOperation("修改纤维压延定额设定（id为空则新增，id不为空则修改）")
    public AjaxResult edit(@RequestBody XwyyQuotaSettingDto dto);

    /**
     * 删除纤维压延定额设定
     *
     * @param ids 需要删除的纤维压延定额设定ID
     * @return 结果
     */
    @PostMapping("/xwyy/quota/{ids}")
    @ApiOperation("删除纤维压延定额设定信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "ids", dataType = "Array", value = "id数组", paramType = "query")
    })
    public AjaxResult remove(@PathVariable("ids") Long[] ids);

    /**
     * 导出纤维压延定额设定信息
     */
    @PostMapping("/xwyy/quota/export")
    @ApiOperation("导出纤维压延定额设定信息")
    public List<XwyyQuotaSettingDto> exportData(@SpringQueryMap XwyyQuotaSettingDto dto);

    /**
     * 导入数据
     */
    @PostMapping("/xwyy/quota/importData")
    public AjaxResult importData(@RequestBody List<XwyyQuotaSettingDto> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);
}
