package com.zlt.aps.lh.controller;

import com.ruoyi.common.core.utils.PageUtils;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.security.annotation.RequiresPermissions;
import com.zlt.aps.lh.api.domain.entity.MesLhScheduleResult;
import com.zlt.aps.lh.service.IMesLhScheduleResultService;
import com.zlt.common.controller.BusiController;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
* Copyright (c) 2022, All rights reserved。
* 文件名称：MesLhScheduleResultController.java
* 描    述：硫化排程下发接口 控制层类：....
*@author zlt
*@date 2025-03-18
*@version 1.0
*
 *  修改记录：
*     修改时间：...
*     修 改 人：zlt
*     修改内容：...
*/
@Slf4j
@Api(tags = "硫化排程下发接口")
@RestController
@RequestMapping("/mesLhScheduleResult")
@RequiredArgsConstructor
public class MesLhScheduleResultController extends BusiController<MesLhScheduleResult> {

    private final IMesLhScheduleResultService mesLhScheduleResultService;


    /**
     * 查询硫化排程下发接口列表
     */
    @RequiresPermissions( "cxlh:mesLhScheduleResult:list")
    @ApiOperation("查询列表")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody MesLhScheduleResult queryVO) {
        try {
            startPage("create_time desc");
            List<MesLhScheduleResult> list = mesLhScheduleResultService.selectList(queryVO);
            return getDataTable(list);
        } finally {
            PageUtils.clearPage();
        }
    }

    // @Override
    // protected String getOrderBy() {
    //     return "create_time desc";
    // }
    //
    // /**
    //  * 保存
    //  */
    // @Log(title = "ui.data.column.mesLhScheduleResult.modelName", businessType = BusinessType.INSERT_OR_UPDATE)
    // @RequiresPermissions( "cxlh:mesLhScheduleResult:save")
    // @ApiOperation("保存")
    // @PostMapping("/save")
    // @Override
    // public AjaxResult save(@RequestBody MesLhScheduleResult billVO){
    //     return super.save(billVO);
    // }
    //
    // /**
    //  * 删除
    //  */
    // @Log(title = "ui.data.column.mesLhScheduleResult.modelName", businessType = BusinessType.DELETE)
    // @RequiresPermissions( "cxlh:mesLhScheduleResult:remove")
    // @ApiOperation("删除")
    // @DeleteMapping("/remove")
    // @Override
    // public AjaxResult removeByIds(@RequestBody List<Long> ids){
    //     return super.removeByIds(ids);
    // }
    //
    //
    // /**
    //  * 获取硫化排程下发接口详细信息
    //  */
    // @RequiresPermissions( "cxlh:mesLhScheduleResult:query")
    // @ApiOperation("获取详细信息")
    // @GetMapping(value = "/{billId}")
    // @Override
    // public MesLhScheduleResult getInfo(@PathVariable("billId") Long billId) {
    //     return super.getInfo(billId);
    // }
    //
    //
    // /**
    //  * 根据集合导入硫化排程下发接口数据
    //  * @param importContext 导入上下文
    //  * @param updateSupport 已存在记录是否更新
    //  * @return 结果
    //  */
    // @RequiresPermissions( "cxlh:mesLhScheduleResult:import")
    // @Log(title = "ui.data.column.mesLhScheduleResult.modelName", businessType = BusinessType.IMPORT)
    // @ApiOperation("导入数据")
    // @PostMapping("/importData")
    // @Override
    // public AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport) throws Exception {
    //     return super.importData(importContext,updateSupport);
    // }
    //
    // /**
    //  * 导出列表
    //  */
    // @RequiresPermissions( "cxlh:mesLhScheduleResult:export")
    // @Log(title = "硫化排程下发接口", businessType = BusinessType.EXPORT)
    // @ApiOperation("导入数据")
    // @PostMapping("/exportData/{fileName}")
    // @Override
    // public byte[] exportData(@RequestBody MesLhScheduleResult queryVO, @PathVariable("fileName") String fileName,
    //                          HttpServletResponse response) throws IOException {
    //     return super.exportData(queryVO, fileName, response);
    // }
    //
    // @Override
    // protected List<MesLhScheduleResult> listExportData(MesLhScheduleResult obj) {
    //     QueryWrapper<MesLhScheduleResult> wrapper = new QueryWrapper<>();
    //     this.builderCondition(wrapper, obj);
    //     return entityMapper.selectList(wrapper);
    // }

}
