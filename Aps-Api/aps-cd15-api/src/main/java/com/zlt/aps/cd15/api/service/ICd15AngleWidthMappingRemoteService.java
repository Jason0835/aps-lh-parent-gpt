package com.zlt.aps.cd15.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.cd15.api.domain.entity.Cd15AngleWidthMapping;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * CD15角度宽度对应关系远程服务
 */
@FeignClient(contextId = "ICd15AngleWidthMappingRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.cd15:/cd15}")
public interface ICd15AngleWidthMappingRemoteService {

    /**
     * 查询列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/cd15AngleWidthMapping/list")
    TableDataInfo list(@RequestBody Cd15AngleWidthMapping queryVO);

    /**
     * 获取详情
     */
    @ApiOperation("获取详情")
    @GetMapping("/cd15AngleWidthMapping/getInfo/{id}")
    Cd15AngleWidthMapping getInfo(@PathVariable("id") Long id);

    /**
     * 新增
     */
    @ApiOperation("新增")
    @PostMapping("/cd15AngleWidthMapping/add")
    AjaxResult add(@RequestBody Cd15AngleWidthMapping entity);

    /**
     * 编辑
     */
    @ApiOperation("编辑")
    @PostMapping("/cd15AngleWidthMapping/edit")
    AjaxResult edit(@RequestBody Cd15AngleWidthMapping entity);

    /**
     * 删除
     */
    @ApiOperation("删除")
    @PostMapping("/cd15AngleWidthMapping/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    /**
     * 校验唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/cd15AngleWidthMapping/checkUnique")
    String checkUnique(@RequestBody Cd15AngleWidthMapping entity);

    /**
     * 导出
     */
    @ApiOperation("导出")
    @PostMapping("/cd15AngleWidthMapping/exportData/{fileName}")
    byte[] exportData(@RequestBody Cd15AngleWidthMapping queryVO, @PathVariable("fileName") String fileName);

    /**
     * 导入
     */
    @ApiOperation("导入")
    @PostMapping("/cd15AngleWidthMapping/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);
}
