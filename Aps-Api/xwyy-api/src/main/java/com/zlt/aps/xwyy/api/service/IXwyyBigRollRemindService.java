package com.zlt.aps.xwyy.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.xwyy.api.domain.entity.XwyyBigRollRemind;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 帘布大卷原线提醒Service接口
 * @author chen
 * @date 2022-04-27
 */
@FeignClient(contextId = "IXwyyBigRollRemindService", value =ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.xwyy:xwyy}")
public interface IXwyyBigRollRemindService {

    /**
     * 查询帘布大卷原线提醒列表
     */
    @ApiOperation("查询帘布大卷原线提醒列表")
    @PostMapping("/xwyy/bigRollRemind/list")
    TableDataInfo list(@RequestBody XwyyBigRollRemind xwyyBigRollRemind);

    /**
    * 新增帘布大卷原线提醒
    */
    @ApiOperation("新增帘布大卷原线提醒")
    @PostMapping("/xwyy/bigRollRemind/add")
    AjaxResult add(@RequestBody XwyyBigRollRemind xwyyBigRollRemind);

    /**
     * 修改帘布大卷原线提醒
     */
    @ApiOperation("修改帘布大卷原线提醒")
    @PostMapping("/xwyy/bigRollRemind/edit")
    AjaxResult edit(@RequestBody XwyyBigRollRemind xwyyBigRollRemind);

    /**
     * 删除帘布大卷原线提醒
     */
    @ApiOperation("删除帘布大卷原线提醒")
    @DeleteMapping("/xwyy/bigRollRemind/{ids}")
    AjaxResult remove(@PathVariable("ids") Long[] ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/xwyy/bigRollRemind/{id}")
    XwyyBigRollRemind getInfo(@PathVariable("id") Long id);

    /**
     * 校验帘布大卷原线提醒唯一性
     */
    @ApiOperation("校验帘布大卷原线提醒唯一性")
    @PostMapping("/xwyy/bigRollRemind/checkXwyyBigRollRemindUnique")
    String checkXwyyBigRollRemindUnique(@RequestBody XwyyBigRollRemind xwyyBigRollRemind);

    /**
     * 导出帘布大卷原线提醒列表
     */
    @ApiOperation("导出帘布大卷原线提醒列表")
    @PostMapping("/xwyy/bigRollRemind/getList")
    List<XwyyBigRollRemind> getList(@RequestBody XwyyBigRollRemind xwyyBigRollRemind);

    /**
     * 导入帘布大卷原线提醒数据
     */
    @ApiOperation("导入帘布大卷原线提醒")
    @PostMapping("/xwyy/bigRollRemind/importData")
    public AjaxResult importData(@RequestBody List<XwyyBigRollRemind> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);
}
