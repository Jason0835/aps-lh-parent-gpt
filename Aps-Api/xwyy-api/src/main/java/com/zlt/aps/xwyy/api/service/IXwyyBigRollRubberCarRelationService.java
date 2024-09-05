package com.zlt.aps.xwyy.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.xwyy.api.domain.entity.XwyyBigRollRubberCarRelation;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 帘布大卷原线胶料号车数关系Service接口
 * @author Joran.Zhang
 * @date 2022-05-10
 */
@FeignClient(contextId = "IXwyyBigRollRubberCarRelationService", value =ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.xwyy:xwyy}")
public interface IXwyyBigRollRubberCarRelationService {

    /**
     * 查询帘布大卷原线胶料号车数关系列表
     */
    @ApiOperation("查询帘布大卷原线胶料号车数关系列表")
    @PostMapping("/bigRollRubberCarRelation/list")
    TableDataInfo list(@RequestBody XwyyBigRollRubberCarRelation xwyyBigRollRubberCarRelation);

    /**
    * 新增帘布大卷原线胶料号车数关系
    */
    @ApiOperation("新增帘布大卷原线胶料号车数关系")
    @PostMapping("/bigRollRubberCarRelation/add")
    AjaxResult add(@RequestBody XwyyBigRollRubberCarRelation xwyyBigRollRubberCarRelation);

    /**
     * 修改帘布大卷原线胶料号车数关系
     */
    @ApiOperation("修改帘布大卷原线胶料号车数关系")
    @PostMapping("/bigRollRubberCarRelation/edit")
    AjaxResult edit(@RequestBody XwyyBigRollRubberCarRelation xwyyBigRollRubberCarRelation);

    /**
     * 删除帘布大卷原线胶料号车数关系
     */
    @ApiOperation("删除帘布大卷原线胶料号车数关系")
    @DeleteMapping("/bigRollRubberCarRelation/{ids}")
    AjaxResult remove(@PathVariable("ids") Long[] ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/bigRollRubberCarRelation/{id}")
    XwyyBigRollRubberCarRelation getInfo(@PathVariable("id") Long id);

    /**
     * 校验帘布大卷原线胶料号车数关系唯一性
     */
    @ApiOperation("校验帘布大卷原线胶料号车数关系唯一性")
    @PostMapping("/bigRollRubberCarRelation/checkXwyyBigRollRubberCarRelationUnique")
    String checkXwyyBigRollRubberCarRelationUnique(@RequestBody XwyyBigRollRubberCarRelation xwyyBigRollRubberCarRelation);

    /**
     * 导出帘布大卷原线胶料号车数关系列表
     */
    @ApiOperation("导出帘布大卷原线胶料号车数关系列表")
    @PostMapping("/bigRollRubberCarRelation/getList")
    List<XwyyBigRollRubberCarRelation> getList(@RequestBody XwyyBigRollRubberCarRelation xwyyBigRollRubberCarRelation);

    /**
     * 导入帘布大卷原线胶料号车数关系数据
     */
    @ApiOperation("导入帘布大卷原线胶料号车数关系")
    @PostMapping("/bigRollRubberCarRelation/importData")
    public AjaxResult importData(@RequestBody List<XwyyBigRollRubberCarRelation> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);

    /**
     * 根据帘布大卷查询对应的关系
     */
    @ApiOperation("根据帘布大卷查询对应的关系")
    @PostMapping("/bigRollRubberCarRelation/selectByBigRollCode")
    public XwyyBigRollRubberCarRelation selectByBigRollCode(@RequestBody XwyyBigRollRubberCarRelation xwyyBigRollRubberCarRelation);
}
