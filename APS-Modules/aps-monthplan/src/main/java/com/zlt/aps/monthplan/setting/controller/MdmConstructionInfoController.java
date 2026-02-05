package com.zlt.aps.monthplan.setting.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.common.security.annotation.RequiresPermissions;
import com.zlt.aps.maindata.mapper.MdmConstructionInfoEntityMapper;
import com.zlt.aps.maindata.service.IMdmConstructionInfoService;
import com.zlt.aps.monthplan.api.domain.entity.MdmConstructionInfo;
import com.zlt.common.utils.PubUtil;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import lombok.extern.slf4j.Slf4j;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;


import com.ruoyi.common.core.web.page.TableDataInfo;

import com.zlt.bill.common.controller.AbstractDocBizController;
import com.zlt.bill.common.service.IDocService ;

/**
* Copyright (c) 2022, All rights reserved。
* 文件名称：MdmConstructionInfoController.java
* 描    述：投产胎胚施工信息 控制层类：....
*@author zlt
*@date 2025-12-10
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
    private MdmConstructionInfoEntityMapper entityMapper;

    /**
     * 查询投产胎胚施工信息列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody MdmConstructionInfo queryVO) {
        return super.list(queryVO);
    }

    @Override
    protected String getOrderBy() {
        return "create_time desc";
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
    @ApiOperation("导出数据")
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
        return entityMapper.selectList(wrapper);
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
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("materialCode")), "MATERIAL_CODE", queryVO.getFieldValueByFieldName("materialCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("mesMaterialCode")), "MES_MATERIAL_CODE", queryVO.getFieldValueByFieldName("mesMaterialCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("specCode")), "SPEC_CODE", queryVO.getFieldValueByFieldName("specCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("constructionCode")), "CONSTRUCTION_CODE", queryVO.getFieldValueByFieldName("constructionCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("constructionVersion")), "CONSTRUCTION_VERSION", queryVO.getFieldValueByFieldName("constructionVersion"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("mouldMethod")), "MOULD_METHOD", queryVO.getFieldValueByFieldName("mouldMethod"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("proSize")), "PRO_SIZE", queryVO.getFieldValueByFieldName("proSize"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("specifications")), "SPECIFICATIONS", queryVO.getFieldValueByFieldName("specifications"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("headWidth")), "HEAD_WIDTH", queryVO.getFieldValueByFieldName("headWidth"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("bucklePlageDiameter")), "BUCKLE_PLAGE_DIAMETER", queryVO.getFieldValueByFieldName("bucklePlageDiameter"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("sectionWidth")), "SECTION_WIDTH", queryVO.getFieldValueByFieldName("sectionWidth"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("fitDrumPerimeter")), "FIT_DRUM_PERIMETER", queryVO.getFieldValueByFieldName("fitDrumPerimeter"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("chuckDiameter")), "CHUCK_DIAMETER", queryVO.getFieldValueByFieldName("chuckDiameter"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("stretchWidth")), "STRETCH_WIDTH", queryVO.getFieldValueByFieldName("stretchWidth"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("qualitativeWidth")), "QUALITATIVE_WIDTH", queryVO.getFieldValueByFieldName("qualitativeWidth"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("embryoCircle")), "EMBRYO_CIRCLE", queryVO.getFieldValueByFieldName("embryoCircle"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("tireFabricCode1")), "TIRE_FABRIC_CODE1", queryVO.getFieldValueByFieldName("tireFabricCode1"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("tireFabric1Version")), "TIRE_FABRIC1_VERSION", queryVO.getFieldValueByFieldName("tireFabric1Version"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("tireFabricCraft1")), "TIRE_FABRIC_CRAFT1", queryVO.getFieldValueByFieldName("tireFabricCraft1"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("tireFabricCode2")), "TIRE_FABRIC_CODE2", queryVO.getFieldValueByFieldName("tireFabricCode2"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("tireFabric2Version")), "TIRE_FABRIC2_VERSION", queryVO.getFieldValueByFieldName("tireFabric2Version"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("tireFabricCraft2")), "TIRE_FABRIC_CRAFT2", queryVO.getFieldValueByFieldName("tireFabricCraft2"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("tireFabricCode3")), "TIRE_FABRIC_CODE3", queryVO.getFieldValueByFieldName("tireFabricCode3"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("tireFabric3Version")), "TIRE_FABRIC3_VERSION", queryVO.getFieldValueByFieldName("tireFabric3Version"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("tireFabricCraft3")), "TIRE_FABRIC_CRAFT3", queryVO.getFieldValueByFieldName("tireFabricCraft3"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("originalLineCode")), "ORIGINAL_LINE_CODE", queryVO.getFieldValueByFieldName("originalLineCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("cordSpec")), "CORD_SPEC", queryVO.getFieldValueByFieldName("cordSpec"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("cordVersion")), "CORD_VERSION", queryVO.getFieldValueByFieldName("cordVersion"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("reinforceSealGlue")), "REINFORCE_SEAL_GLUE", queryVO.getFieldValueByFieldName("reinforceSealGlue"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("insideRubber")), "INSIDE_RUBBER", queryVO.getFieldValueByFieldName("insideRubber"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("insideCode")), "INSIDE_CODE", queryVO.getFieldValueByFieldName("insideCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("insideVersion")), "INSIDE_VERSION", queryVO.getFieldValueByFieldName("insideVersion"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("insideCraft")), "INSIDE_CRAFT", queryVO.getFieldValueByFieldName("insideCraft"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("sidewallCode")), "SIDEWALL_CODE", queryVO.getFieldValueByFieldName("sidewallCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("sidewallVersion")), "SIDEWALL_VERSION", queryVO.getFieldValueByFieldName("sidewallVersion"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("sidewallCraft")), "SIDEWALL_CRAFT", queryVO.getFieldValueByFieldName("sidewallCraft"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("sidewallMouthPlate")), "SIDEWALL_MOUTH_PLATE", queryVO.getFieldValueByFieldName("sidewallMouthPlate"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("sidewallCenter")), "SIDEWALL_CENTER", queryVO.getFieldValueByFieldName("sidewallCenter"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("sidewallLength")), "SIDEWALL_LENGTH", queryVO.getFieldValueByFieldName("sidewallLength"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("sidewallRubber")), "SIDEWALL_RUBBER", queryVO.getFieldValueByFieldName("sidewallRubber"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("sidewallWeight")), "SIDEWALL_WEIGHT", queryVO.getFieldValueByFieldName("sidewallWeight"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("sidewallWearpRubberWeight")), "SIDEWALL_WEARP_RUBBER_WEIGHT", queryVO.getFieldValueByFieldName("sidewallWearpRubberWeight"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("supportCode")), "SUPPORT_CODE", queryVO.getFieldValueByFieldName("supportCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("supportRubberCode")), "SUPPORT_RUBBER_CODE", queryVO.getFieldValueByFieldName("supportRubberCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("supportLength")), "SUPPORT_LENGTH", queryVO.getFieldValueByFieldName("supportLength"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("beadCode")), "BEAD_CODE", queryVO.getFieldValueByFieldName("beadCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("beadVersion")), "BEAD_VERSION", queryVO.getFieldValueByFieldName("beadVersion"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("beadArrange")), "BEAD_ARRANGE", queryVO.getFieldValueByFieldName("beadArrange"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("beadType")), "BEAD_TYPE", queryVO.getFieldValueByFieldName("beadType"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("tireRingCode")), "TIRE_RING_CODE", queryVO.getFieldValueByFieldName("tireRingCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("tireRingVersion")), "TIRE_RING_VERSION", queryVO.getFieldValueByFieldName("tireRingVersion"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("apexCode")), "APEX_CODE", queryVO.getFieldValueByFieldName("apexCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("hexagonRubberCode")), "HEXAGON_RUBBER_CODE", queryVO.getFieldValueByFieldName("hexagonRubberCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("hexagonMouthPlate")), "HEXAGON_MOUTH_PLATE", queryVO.getFieldValueByFieldName("hexagonMouthPlate"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("hexagonRubberDimension")), "HEXAGON_RUBBER_DIMENSION", queryVO.getFieldValueByFieldName("hexagonRubberDimension"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("apexWeight")), "APEX_WEIGHT", queryVO.getFieldValueByFieldName("apexWeight"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("beltCode1")), "BELT_CODE1", queryVO.getFieldValueByFieldName("beltCode1"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("belt1Version")), "BELT1_VERSION", queryVO.getFieldValueByFieldName("belt1Version"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("beltCraft1")), "BELT_CRAFT1", queryVO.getFieldValueByFieldName("beltCraft1"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("beltSideRubber1")), "BELT_SIDE_RUBBER1", queryVO.getFieldValueByFieldName("beltSideRubber1"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("beltRubber1")), "BELT_RUBBER1", queryVO.getFieldValueByFieldName("beltRubber1"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("beltCode2")), "BELT_CODE2", queryVO.getFieldValueByFieldName("beltCode2"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("belt2Version")), "BELT2_VERSION", queryVO.getFieldValueByFieldName("belt2Version"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("beltCraft2")), "BELT_CRAFT2", queryVO.getFieldValueByFieldName("beltCraft2"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("beltSideRubber2")), "BELT_SIDE_RUBBER2", queryVO.getFieldValueByFieldName("beltSideRubber2"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("beltRubber2")), "BELT_RUBBER2", queryVO.getFieldValueByFieldName("beltRubber2"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("beltCuttingAngle")), "BELT_CUTTING_ANGLE", queryVO.getFieldValueByFieldName("beltCuttingAngle"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("articleCrownSpec")), "ARTICLE_CROWN_SPEC", queryVO.getFieldValueByFieldName("articleCrownSpec"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("articleCrownVersion")), "ARTICLE_CROWN_VERSION", queryVO.getFieldValueByFieldName("articleCrownVersion"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("articleCrownCode")), "ARTICLE_CROWN_CODE", queryVO.getFieldValueByFieldName("articleCrownCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("treadCode")), "TREAD_CODE", queryVO.getFieldValueByFieldName("treadCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("treadVersion")), "TREAD_VERSION", queryVO.getFieldValueByFieldName("treadVersion"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("treadShoulderWidth")), "TREAD_SHOULDER_WIDTH", queryVO.getFieldValueByFieldName("treadShoulderWidth"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("treadShoulderJwidth")), "TREAD_SHOULDER_JWIDTH", queryVO.getFieldValueByFieldName("treadShoulderJwidth"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("treadShoulderLength")), "TREAD_SHOULDER_LENGTH", queryVO.getFieldValueByFieldName("treadShoulderLength"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("treadRubberCategory")), "TREAD_RUBBER_CATEGORY", queryVO.getFieldValueByFieldName("treadRubberCategory"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("tireCrownUpWidthWeight")), "TIRE_CROWN_UP_WIDTH_WEIGHT", queryVO.getFieldValueByFieldName("tireCrownUpWidthWeight"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("tireCrownDownWidthWeight")), "TIRE_CROWN_DOWN_WIDTH_WEIGHT", queryVO.getFieldValueByFieldName("tireCrownDownWidthWeight"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("tireWingWidthWeight")), "TIRE_WING_WIDTH_WEIGHT", queryVO.getFieldValueByFieldName("tireWingWidthWeight"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("primerWeight")), "PRIMER_WEIGHT", queryVO.getFieldValueByFieldName("primerWeight"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("conductingResinWeight")), "CONDUCTING_RESIN_WEIGHT", queryVO.getFieldValueByFieldName("conductingResinWeight"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("treadMouthPlate")), "TREAD_MOUTH_PLATE", queryVO.getFieldValueByFieldName("treadMouthPlate"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("mouldClampingPressure")), "MOULD_CLAMPING_PRESSURE", queryVO.getFieldValueByFieldName("mouldClampingPressure"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("curingTime")), "CURING_TIME", queryVO.getFieldValueByFieldName("curingTime"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("hydraulicPressureCuringTime")), "HYDRAULIC_PRESSURE_CURING_TIME", queryVO.getFieldValueByFieldName("hydraulicPressureCuringTime"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("moldCavity")), "MOLD_CAVITY", queryVO.getFieldValueByFieldName("moldCavity"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("productionStage")), "PRODUCTION_STAGE", queryVO.getFieldValueByFieldName("productionStage"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("pattern")), "PATTERN", queryVO.getFieldValueByFieldName("pattern"));
    }


    @Override
    protected String getTypeCode(){
        return "MDM0124";
    }

    /**
     * 抓取MES数据
     * @return 结果
     */
    @ApiOperation("抓取MES数据")
    @PostMapping("/mesCapture")
    public AjaxResult mesCapture() {
        // TODO...对接接口
        return AjaxResult.success();
    }

}
