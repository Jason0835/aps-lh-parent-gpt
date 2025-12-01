package com.zlt.aps.cxlh.cx.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.cxlh.cx.api.domain.entity.CxCloseOutRange;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * 成型收尾范围系数Service接口
 * @author zlt
 * @date 2021-12-28
 */
@FeignClient(contextId = "ICxCloseOutRangeService", value =ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.cxlh:cxlh}")
public interface ICxCloseOutRangeService {

    /**
     * 查询成型收尾范围系数列表
     */
    @ApiOperation("查询成型收尾范围系数列表")
    @PostMapping("/closeOutRange/list")
    TableDataInfo list(@RequestBody CxCloseOutRange cxCloseOutRange);

    /**
    * 新增成型收尾范围系数
    */
    @ApiOperation("新增成型收尾范围系数")
    @PostMapping("/closeOutRange/add")
    AjaxResult add(@RequestBody CxCloseOutRange cxCloseOutRange);

    /**
     * 修改成型收尾范围系数
     */
    @ApiOperation("修改成型收尾范围系数")
    @PostMapping("/closeOutRange/edit")
    AjaxResult edit(@RequestBody CxCloseOutRange cxCloseOutRange);

    /**
     * 删除成型收尾范围系数
     */
    @ApiOperation("删除成型收尾范围系数")
    @DeleteMapping("/closeOutRange/{ids}")
    AjaxResult remove(@PathVariable("ids") Long[] ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/closeOutRange/{id}")
    CxCloseOutRange getInfo(@PathVariable("id") Long id);

    /**
     * 校验成型收尾范围系数唯一性
     */
    @ApiOperation("校验成型收尾范围系数唯一性")
    @PostMapping("/closeOutRange/checkCxCloseOutRangeUnique")
    String checkCxCloseOutRangeUnique(@RequestBody CxCloseOutRange cxCloseOutRange);

    /**
     * 导出成型收尾范围系数列表
     */
    @ApiOperation("导出成型收尾范围系数列表")
    @PostMapping("/closeOutRange/getList")
    List<CxCloseOutRange> getList(@RequestBody CxCloseOutRange cxCloseOutRange);

    /**
     * 导入成型收尾范围系数数据
     */
    @ApiOperation("导入成型收尾范围系数")
    @PostMapping("/closeOutRange/importData")
    public AjaxResult importData(@RequestBody List<CxCloseOutRange> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);
}
