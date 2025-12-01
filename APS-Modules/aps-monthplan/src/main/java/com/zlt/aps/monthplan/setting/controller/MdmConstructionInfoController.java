package com.zlt.aps.monthplan.setting.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.maindata.mapper.MdmConstructionInfoEntityMapper;
import com.zlt.aps.maindata.service.IMdmConstructionInfoService;
import com.zlt.aps.monthplan.api.domain.entity.MdmConstructionInfo;
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
* 文件名称：MdmConstructionInfoController.java
* 描    述：投产胎胚施工信息 控制层类：....
*@author zlt
*@date 2025-02-24
*@version 1.0
*
 *  修改记录：
*     修改时间：...
*     修 改 人：zlt
*     修改内容：...
*/
@Slf4j
@Api(tags = "投产胎胚施工信息")
@RestController
@RequestMapping("/mdmConstructionInfo")
public class MdmConstructionInfoController extends AbstractDocBizController<MdmConstructionInfo> {

    @Autowired
    private IMdmConstructionInfoService mdmConstructionInfoService;

    @Autowired
    private MdmConstructionInfoEntityMapper mdmConstructionInfoEntityMapper;

    /**
     * 查询投产胎胚施工信息列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody MdmConstructionInfo queryVO) {
        QueryWrapper<MdmConstructionInfo> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, queryVO);
        List<MdmConstructionInfo> mdmConstructionInfoList = mdmConstructionInfoEntityMapper.selectList(wrapper);
        return getDataTable(mdmConstructionInfoList);
    }


    /**
     * 保存
     */
    @Log(title = "ui.data.column.mdmConstructionInfo.modelName", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("保存")
    @PostMapping("/save")
    @Override
    public AjaxResult save(@RequestBody MdmConstructionInfo billVO){
        return super.save(billVO);
    }

    /**
     * 删除
     */
    @Log(title = "ui.data.column.mdmConstructionInfo.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除")
    @DeleteMapping("/remove")
    @Override
    public AjaxResult removeByIds(@RequestBody List<Long> ids){
        return super.removeByIds(ids);
    }

    /**
     * 获取投产胎胚施工信息详细信息
     */
    @ApiOperation("获取详细信息")
    @GetMapping(value = "/{billId}")
    @Override
    public MdmConstructionInfo getInfo(@PathVariable("billId") Long billId) {
        return super.getInfo(billId);
    }

    /**
     * 根据集合导入投产胎胚施工信息数据
     * @param importContext 导入上下文
     * @param updateSupport 已存在记录是否更新
     * @return 结果
     */
    @Log(title = "ui.data.column.mdmConstructionInfo.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    @Override
    public AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        return super.importData(importContext,updateSupport);
    }

    /**
     * 导出列表
     */
    @Log(title = "投产胎胚施工信息", businessType = BusinessType.EXPORT)
    @ApiOperation("导入数据")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody MdmConstructionInfo queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return super.exportData(queryVO, fileName, response);
    }

    @Override
    protected List<MdmConstructionInfo> listExportData(MdmConstructionInfo obj) {
        QueryWrapper<MdmConstructionInfo> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, obj);
        return mdmConstructionInfoEntityMapper.selectList(wrapper);
    }

    @Override
    protected IDocService getDocService(){
        return mdmConstructionInfoService;
    }

    /**
     * 条件拼接
     *
     * @param queryWrapper
     * @param queryVO
     */
    @Override
    protected void builderCondition(QueryWrapper<MdmConstructionInfo> queryWrapper, MdmConstructionInfo queryVO) {
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("sapCode")), "SAP_CODE", queryVO.getFieldValueByFieldName("sapCode"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("constructionCode")), "CONSTRUCTION_CODE", queryVO.getFieldValueByFieldName("constructionCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("proSize")), "PRO_SIZE", queryVO.getFieldValueByFieldName("proSize"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("specifications")), "SPECIFICATIONS", queryVO.getFieldValueByFieldName("specifications"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("productionStage")), "PRODUCTION_STAGE", queryVO.getFieldValueByFieldName("productionStage"));
    }


    @Override
    protected String getTypeCode(){
        return "0106";
    }


}
