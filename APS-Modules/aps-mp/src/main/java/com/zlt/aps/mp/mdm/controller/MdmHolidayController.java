package com.zlt.aps.mp.mdm.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.security.annotation.RequiresPermissions;
import com.zlt.aps.maindata.mapper.MdmHolidayEntityMapper;
import com.zlt.aps.maindata.service.IMdmHolidayService;
import com.zlt.aps.mp.api.domain.entity.MdmHoliday;
import com.zlt.bill.common.controller.AbstractDocBizController;
import com.zlt.bill.common.service.IDocService;
import com.zlt.common.utils.PubUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
* Copyright (c) 2022, All rights reserved。
* 文件名称：MdmHolidayController.java
* 描    述：0150基础数据_节假日配置 控制层类：....
*@author zlt
*@date 2026-01-06
*@version 1.0
*
 *  修改记录：
*     修改时间：...
*     修 改 人：zlt
*     修改内容：...
*/
@Slf4j
@Api(tags = "0150基础数据_节假日配置")
@RestController
@RequestMapping("/mdmHoliday")
public class MdmHolidayController extends AbstractDocBizController<MdmHoliday> {

    @Autowired
    private IMdmHolidayService mdmHolidayService;

    @Autowired
    private MdmHolidayEntityMapper entityMapper;

    /**
     * 查询0150基础数据_节假日配置列表
     */
    @RequiresPermissions( "maindata:mdmHoliday:list")
    @ApiOperation("查询列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody MdmHoliday queryVO) {
        return super.list(queryVO);
    }

    @Override
    protected String getOrderBy() {
        return "create_time desc";
    }

    /**
     * 保存
     */
    @Log(title = "ui.data.column.mdmHoliday.modelName", businessType = BusinessType.INSERT_OR_UPDATE)
    @RequiresPermissions( "maindata:mdmHoliday:save")
    @ApiOperation("保存")
    @PostMapping("/save")
    @Override
    public AjaxResult save(@RequestBody MdmHoliday billVO){
        return super.save(billVO);
    }

    /**
     * 删除
     */
    @Log(title = "ui.data.column.mdmHoliday.modelName", businessType = BusinessType.DELETE)
    @RequiresPermissions( "maindata:mdmHoliday:remove")
    @ApiOperation("删除")
    @DeleteMapping("/remove")
    @Override
    public AjaxResult removeByIds(@RequestBody List<Long> ids){
        return super.removeByIds(ids);
    }


    /**
     * 获取0150基础数据_节假日配置详细信息
     */
    @RequiresPermissions( "maindata:mdmHoliday:query")
    @ApiOperation("获取详细信息")
    @GetMapping(value = "/{billId}")
    @Override
    public MdmHoliday getInfo(@PathVariable("billId") Long billId) {
        return super.getInfo(billId);
    }


    /**
     * 根据集合导入0150基础数据_节假日配置数据
     * @param importContext 导入上下文
     * @param updateSupport 已存在记录是否更新
     * @return 结果
     */
    @RequiresPermissions( "maindata:mdmHoliday:import")
    @Log(title = "ui.data.column.mdmHoliday.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    @Override
    public AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        return super.importData(importContext,updateSupport);
    }

    /**
     * 导出列表
     */
    @RequiresPermissions( "maindata:mdmHoliday:export")
    @Log(title = "0150基础数据_节假日配置", businessType = BusinessType.EXPORT)
    @ApiOperation("导入数据")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody MdmHoliday queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return super.exportData(queryVO, fileName, response);
    }

    @Override
    protected List<MdmHoliday> listExportData(MdmHoliday obj) {
        QueryWrapper<MdmHoliday> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, obj);
        return entityMapper.selectList(wrapper);
    }

    @Override
    protected IDocService getDocService(){
        return mdmHolidayService;
    }

    /**
     * 条件拼接
     *
     * @param queryWrapper
     * @param queryVO
     */
    @Override
    protected void builderCondition(QueryWrapper<MdmHoliday> queryWrapper, MdmHoliday queryVO) {
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("holidayDate")), "HOLIDAY_DATE", queryVO.getFieldValueByFieldName("holidayDate"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("holidayNames")), "HOLIDAY_NAMES", queryVO.getFieldValueByFieldName("holidayNames"));

        queryWrapper.ge(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("holidayDateStartTime")), "HOLIDAY_DATE", queryVO.getFieldValueByFieldName("holidayDateStartTime"));
        queryWrapper.le(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("holidayDateEndTime")), "HOLIDAY_DATE", queryVO.getFieldValueByFieldName("holidayDateEndTime"));
    }

    @Override
    protected String getTypeCode(){
        return "MDM0150";
    }


}
