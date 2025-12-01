package com.zlt.aps.monthplan.setting.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.tlt.aps.constant.StringConstant;
import com.zlt.aps.maindata.mapper.LocationChannelConfigurationMapper;
import com.zlt.aps.maindata.service.ILocationChannelConfigurationService;
import com.zlt.aps.monthplan.api.domain.entity.LocationChannelConfiguration;
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
* 文件名称：LocationChannelConfigurationController.java
* 描    述：库位类别渠道品牌配置 控制层类：....
*@author ZLT
*@date 2025-02-28
*@version 1.0
*
 *  修改记录：
*     修改时间：...
*     修 改 人：ZLT
*     修改内容：...
*/
@Slf4j
@Api(tags = "库位类别渠道品牌配置")
@RestController
@RequestMapping("/LocationChannelConfiguration")
public class LocationChannelConfigurationController extends AbstractDocBizController<LocationChannelConfiguration> {

    @Autowired
    private ILocationChannelConfigurationService locationChannelConfigurationService;

    @Autowired
    private LocationChannelConfigurationMapper mapper;

    /**
     * 查询库位类别渠道品牌配置列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody LocationChannelConfiguration queryVO) {
        this.startPage(this.getOrderBy());
        QueryWrapper<LocationChannelConfiguration> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, queryVO);
        List<LocationChannelConfiguration> list = mapper.selectList(wrapper);
        return getDataTable(list);
    }

    @Override
    protected String getOrderBy() {
        return "create_time desc";
    }

    /**
     * 保存
     */
    @Log(title = "ui.data.column.LocationChannelConfiguration.modelName", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("保存")
    @PostMapping("/save")
    @Override
    public AjaxResult save(@RequestBody LocationChannelConfiguration billVO){
        return super.save(billVO);
    }

    /**
     * 删除
     */
    @Log(title = "ui.data.column.LocationChannelConfiguration.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除")
    @DeleteMapping("/remove")
    @Override
    public AjaxResult removeByIds(@RequestBody List<Long> ids){
        return super.removeByIds(ids);
    }


    /**
     * 获取库位类别渠道品牌配置详细信息
     */
    @ApiOperation("获取详细信息")
    @GetMapping(value = "/{billId}")
    @Override
    public LocationChannelConfiguration getInfo(@PathVariable("billId") Long billId) {
        return super.getInfo(billId);
    }


    /**
     * 根据集合导入库位类别渠道品牌配置数据
     * @param importContext 导入上下文
     * @param updateSupport 已存在记录是否更新
     * @return 结果
     */
    @Log(title = "ui.data.column.LocationChannelConfiguration.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    @Override
    public AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        return super.importData(importContext,updateSupport);
    }

    /**
     * 导出列表
     */
    @Log(title = "库位类别渠道品牌配置", businessType = BusinessType.EXPORT)
    @ApiOperation("导入数据")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody LocationChannelConfiguration queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return super.exportData(queryVO, fileName, response);
    }

    @Override
    protected List<LocationChannelConfiguration> listExportData(LocationChannelConfiguration obj) {
        QueryWrapper<LocationChannelConfiguration> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, obj);
        return mapper.selectList(wrapper);
    }

    @Override
    protected IDocService getDocService(){
        return locationChannelConfigurationService;
    }

    /**
     * 条件拼接
     *
     * @param queryWrapper
     * @param queryVO
     */
    @Override
    protected void builderCondition(QueryWrapper<LocationChannelConfiguration> queryWrapper, LocationChannelConfiguration queryVO) {
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("factoryCode")), "FACTORY_CODE", queryVO.getFieldValueByFieldName("factoryCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("locationType")), "LOCATION_TYPE", queryVO.getFieldValueByFieldName("locationType"));
        if (!StringConstant.ALL_MATCH.equals(queryVO.getChannel())) {
            queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("channel")), "CHANNEL", queryVO.getFieldValueByFieldName("channel"));
        }
        if (!StringConstant.ALL_MATCH.equals(queryVO.getBrand())) {
            queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("brand")), "BRAND", queryVO.getFieldValueByFieldName("brand"));
        }
    }

    @Override
    protected String getTypeCode(){
        return "0132";
    }



}
