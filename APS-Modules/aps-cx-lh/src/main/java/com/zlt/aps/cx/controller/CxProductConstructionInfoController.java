package com.zlt.aps.cx.controller;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;

import com.zlt.aps.cx.service.ConstructionParseService;
import com.zlt.aps.cx.service.CxProductConstructionInfoService;
import com.zlt.aps.cxlh.cx.api.domain.entity.CxProductConstructionInfo;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 投产施工信息Controller
 *
 * @author zlt
 * @date 2021-12-02
 */
@RestController
@RequestMapping("/productConstruction")
public class CxProductConstructionInfoController extends BaseController {
    @Autowired
    private CxProductConstructionInfoService cxProductConstructionInfoService;

    @Autowired
    private ConstructionParseService constructionParseService;


    /**
     * 查询投产施工信息列表
     */
    @ApiOperation("查询投产施工信息列表")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody CxProductConstructionInfo cxProductConstructionInfo) {
        startPage();
        cxProductConstructionInfo.setOrderStr(orderStr());
        List<CxProductConstructionInfo> list = cxProductConstructionInfoService.selectCxProductConstructionInfoList(cxProductConstructionInfo);
        return getDataTable(list);
    }

    /**
     * 获取投产施工信息详细信息
     */
    @ApiOperation("获取投产施工信息详细信息")
    @GetMapping(value = "/{id}")
    public CxProductConstructionInfo getInfo(@PathVariable("id") Long id) {
        return cxProductConstructionInfoService.selectCxProductConstructionInfoById(id);
    }

    /**
     * 新增投产施工信息
     */
    @Log(title = "ui.data.column.productConstruction.modelName", businessType = BusinessType.INSERT)
    @ApiOperation("新增投产施工信息")
    @PostMapping("/add")
    public AjaxResult add(@RequestBody CxProductConstructionInfo cxProductConstructionInfo) {
        return toAjax(cxProductConstructionInfoService.insertCxProductConstructionInfo(cxProductConstructionInfo));
    }

    /**
     * 修改投产施工信息
     */
    @Log(title = "ui.data.column.productConstruction.modelName", businessType = BusinessType.UPDATE)
    @ApiOperation("修改投产施工信息")
    @PostMapping("/edit1")
    public AjaxResult edit1(@RequestBody CxProductConstructionInfo cxProductConstructionInfo){
        return toAjax(cxProductConstructionInfoService.updateCxProductConstructionInfo2(cxProductConstructionInfo));
    }

    /**
     * 依据半部件版本设置各自半部件字段值
     */
    @Log(title = "ui.data.column.productConstruction.modelName", businessType = BusinessType.UPDATE)
    @ApiOperation("依据半部件版本设置各自半部件字段值")
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody CxProductConstructionInfo pc) {
        List<CxProductConstructionInfo> list = constructionParseService.getPartsConstruction(pc, false);
        if (CollectionUtils.isEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.productConstruction.partIsNull"));
        }
        //按QueryType、版本、设置半部件的字段值
        if (CollectionUtils.isNotEmpty(list)) {
            String queryType = pc.getQueryType();
            if ("TREAD".equals(queryType)) { //胎面：TREAD
                List<CxProductConstructionInfo> cpcList = list.stream().filter(a -> pc.getTreadVersion().equals(a.getTreadVersion())).collect(Collectors.toList());
                if (CollectionUtils.isNotEmpty(cpcList)) {
                    CxProductConstructionInfo source = cpcList.get(0);
                    pc.setTreadCode(source.getTreadCode());
                    pc.setTreadSap(source.getTreadSap());
                    pc.setTreadVersion(source.getTreadVersion());
                    pc.setTreadRubberCategory(source.getTreadRubberCategory());
                    pc.setTreadMouthPlate(source.getTreadMouthPlate());
                    pc.setTreadShoulderLength(source.getTreadShoulderLength());
                }
            } else if ("SIDEWALL".equals(queryType)) { //胎侧：SIDEWALL
                List<CxProductConstructionInfo> cpcList = list.stream().filter(a -> pc.getSidewallVersion().equals(a.getSidewallVersion())).collect(Collectors.toList());
                if (CollectionUtils.isNotEmpty(cpcList)) {
                    CxProductConstructionInfo source = cpcList.get(0);
                    pc.setSidewallCode(source.getSidewallCode());
                    pc.setSidewallSap(source.getSidewallSap());
                    pc.setSidewallVersion(source.getSidewallVersion());
                    pc.setSidewallRubber(source.getSidewallRubber());
                    pc.setSidewallMouthPlate(source.getSidewallMouthPlate());
                    pc.setSidewallLength(source.getSidewallLength());
                }
            } else if ("INSIDE".equals(queryType)) { //内衬：INSIDE
                List<CxProductConstructionInfo> cpcList = list.stream().filter(a -> pc.getInsideVersion().equals(a.getInsideVersion())).collect(Collectors.toList());
                if (CollectionUtils.isNotEmpty(cpcList)) {
                    CxProductConstructionInfo source = cpcList.get(0);
                    pc.setInsideCode(source.getInsideCode());
                    pc.setInsideSap(source.getInsideSap());
                    pc.setInsideVersion(source.getInsideVersion());
                    pc.setInsideRubber(source.getInsideRubber());
                }
            } else if ("TIRE_RING".equals(queryType)) { //胎圈：TIRE_RING
                List<CxProductConstructionInfo> cpcList = list.stream().filter(a -> pc.getTireRingVersion().equals(a.getTireRingVersion())).collect(Collectors.toList());
                if (CollectionUtils.isNotEmpty(cpcList)) {
                    CxProductConstructionInfo source = cpcList.get(0);
                    pc.setTireRingCode(source.getTireRingCode());
                    pc.setTireRingSap(source.getTireRingSap());
                    pc.setTireRingVersion(source.getTireRingVersion());
                    pc.setApexCode(source.getApexCode());
                    pc.setHexagonRubberCode(source.getHexagonRubberCode());
                    pc.setHexagonMouthPlate(source.getHexagonMouthPlate());
                    pc.setHexagonRubberDimension(source.getHexagonRubberDimension());
                }
            } else if ("BEAD".equals(queryType)) { //钢丝圈：BEAD
                List<CxProductConstructionInfo> cpcList = list.stream().filter(a -> pc.getBeadVersion().equals(a.getBeadVersion())).collect(Collectors.toList());
                if (CollectionUtils.isNotEmpty(cpcList)) {
                    CxProductConstructionInfo source = cpcList.get(0);
                    pc.setBeadCode(source.getBeadCode());
                    pc.setBeadSap(source.getBeadSap());
                    pc.setBeadVersion(source.getBeadVersion());
                    pc.setBeadType(source.getBeadType());
                    pc.setBeadArrange(source.getBeadArrange());
                }
            } else if ("BELT1".equals(queryType)) { //1#钢带：BELT1
                List<CxProductConstructionInfo> cpcList = list.stream().filter(a -> pc.getBelt1Version().equals(a.getBelt1Version())).collect(Collectors.toList());
                if (CollectionUtils.isNotEmpty(cpcList)) {
                    CxProductConstructionInfo source = cpcList.get(0);
                    pc.setFitDrumPerimeter(source.getFitDrumPerimeter());
                    pc.setBeltCuttingAngle(source.getBeltCuttingAngle());
                    pc.setBeltCode1(source.getBeltCode1());
                    pc.setBeltSap1(source.getBeltSap1());
                    pc.setBelt1Version(source.getBelt1Version());
                    pc.setBeltCraft1(source.getBeltCraft1());
                }
            } else if ("BELT2".equals(queryType)) { //2#钢带：BELT2
                List<CxProductConstructionInfo> cpcList = list.stream().filter(a -> pc.getBelt2Version().equals(a.getBelt2Version())).collect(Collectors.toList());
                if (CollectionUtils.isNotEmpty(cpcList)) {
                    CxProductConstructionInfo source = cpcList.get(0);
                    pc.setBeltCode2(source.getBeltCode2());
                    pc.setBeltSap2(source.getBeltSap2());
                    pc.setBelt2Version(source.getBelt2Version());
                    pc.setBeltCraft2(source.getBeltCraft2());
                }
            } else if ("ARTICLE_CROWN".equals(queryType)) { //钢压大卷：ARTICLE_CROWN
                List<CxProductConstructionInfo> cpcList = list.stream().filter(a -> pc.getArticleCrownVersion().equals(a.getArticleCrownVersion())).collect(Collectors.toList());
                if (CollectionUtils.isNotEmpty(cpcList)) {
                    CxProductConstructionInfo source = cpcList.get(0);
                    pc.setArticleCrownSpec(source.getArticleCrownSpec());
                    pc.setArticleCrownSap(source.getArticleCrownSap());
                    pc.setArticleCrownVersion(source.getArticleCrownVersion());
                }
            } else if ("TIRE_FABRIC1".equals(queryType)) { //1#胎体布：TIRE_FABRIC1
                List<CxProductConstructionInfo> cpcList = list.stream().filter(a -> pc.getTireFabric1Version().equals(a.getTireFabric1Version())).collect(Collectors.toList());
                if (CollectionUtils.isNotEmpty(cpcList)) {
                    CxProductConstructionInfo source = cpcList.get(0);
                    pc.setTireFabricCode1(source.getTireFabricCode1());
                    pc.setTireFabricSap1(source.getTireFabricSap1());
                    pc.setTireFabric1Version(source.getTireFabric1Version());
                    pc.setTireFabricCraft1(source.getTireFabricCraft1());
                }
            } else if ("TIRE_FABRIC2".equals(queryType)) { //2#胎体布：TIRE_FABRIC2
                List<CxProductConstructionInfo> cpcList = list.stream().filter(a -> pc.getTireFabric2Version().equals(a.getTireFabric2Version())).collect(Collectors.toList());
                if (CollectionUtils.isNotEmpty(cpcList)) {
                    CxProductConstructionInfo source = cpcList.get(0);
                    pc.setTireFabricCode2(source.getTireFabricCode2());
                    pc.setTireFabricSap2(source.getTireFabricSap2());
                    pc.setTireFabric2Version(source.getTireFabric2Version());
                    pc.setTireFabricCraft2(source.getTireFabricCraft2());
                }
            } else if ("TIRE_FABRIC3".equals(queryType)) {  //3#胎体布：TIRE_FABRIC3
                List<CxProductConstructionInfo> cpcList = list.stream().filter(a -> pc.getTireFabric3Version().equals(a.getTireFabric3Version())).collect(Collectors.toList());
                if (CollectionUtils.isNotEmpty(cpcList)) {
                    CxProductConstructionInfo source = cpcList.get(0);
                    pc.setTireFabricCode3(source.getTireFabricCode3());
                    pc.setTireFabricSap3(source.getTireFabricSap3());
                    pc.setTireFabric3Version(source.getTireFabric3Version());
                    pc.setTireFabricCraft3(source.getTireFabricCraft3());
                }
            } else if ("CORD".equals(queryType)) { //帘布大卷：CORD
                List<CxProductConstructionInfo> cpcList = list.stream().filter(a -> pc.getCordVersion().equals(a.getCordVersion())).collect(Collectors.toList());
                if (CollectionUtils.isNotEmpty(cpcList)) {
                    CxProductConstructionInfo source = cpcList.get(0);
                    pc.setCordSpec(source.getCordSpec());
                    pc.setCordSap(source.getCordSap());
                    pc.setCordVersion(source.getCordVersion());
                    pc.setOriginalLineCode(source.getOriginalLineCode());
                }
            }
        }
        return toAjax(cxProductConstructionInfoService.updateCxProductConstructionInfo(pc));
    }

    /**
     * 依据半部件版本设置各自半部件字段值
     */
    @Log(title = "ui.data.column.productConstruction.modelName", businessType = BusinessType.UPDATE)
    @ApiOperation("修改生产阶段")
    @PostMapping("/updateProductionStage")
    public AjaxResult updateProductionStage(@RequestBody CxProductConstructionInfo pc) {
        return toAjax(cxProductConstructionInfoService.updateProductionStage(pc));
    }

    /**
     * 删除投产施工信息
     */
    @Log(title = "ui.data.column.productConstruction.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除投产施工信息")
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        List<CxProductConstructionInfo> list= cxProductConstructionInfoService.selectCxScheduleMongthPlan(ids);
        if(CollectionUtils.isNotEmpty(list)){
            Map<String, Long> groupMap = list.stream().collect(Collectors.groupingBy(a -> a.getSpecDesc(), Collectors.counting()));
            if(groupMap.containsKey("CX") && groupMap.containsKey("MP")){
                return AjaxResult.error(I18nUtil.getMessage("ui.data.column.productConstruction.delete3"));
            }
            if(groupMap.get("CX")>0){
                return AjaxResult.error(I18nUtil.getMessage("ui.data.column.productConstruction.delete1"));
            }
            if(groupMap.get("MP")>0){
                return AjaxResult.error(I18nUtil.getMessage("ui.data.column.productConstruction.delete2"));
            }
        }
        return toAjax(cxProductConstructionInfoService.deleteCxProductConstructionInfoByIds(ids));
    }

    /**
     * 导出投产施工信息列表
     */
    @Log(title = "ui.data.column.productConstruction.modelName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出投产施工信息列表")
    @PostMapping("/getList")
    public List<CxProductConstructionInfo> getList(@RequestBody CxProductConstructionInfo cxProductConstructionInfo) {
        startPage();
        cxProductConstructionInfo.setOrderStr(orderStr());
        return cxProductConstructionInfoService.selectCxProductConstructionInfoList(cxProductConstructionInfo);
    }

    /**
     * 校验投产施工信息唯一性
     */
    @ApiOperation("校验投产施工信息唯一性")
    @PostMapping("/checkCxProductConstructionInfoUnique")
    public String checkCxProductConstructionInfoUnique(@RequestBody CxProductConstructionInfo cxProductConstructionInfo) {
        return cxProductConstructionInfoService.checkCxProductConstructionInfoUnique(cxProductConstructionInfo);
    }

    /**
     * 根据集合导入投产施工信息数据
     *
     * @param list          集合
     * @param updateSupport 已存在记录是否更新
     * @param importLogId   导入日志id
     * @return 结果
     */
    @Log(title = "ui.data.column.productConstruction.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入投产施工信息数据")
    @PostMapping("/importData")
    public AjaxResult importData(@RequestBody List<CxProductConstructionInfo> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        if (CollectionUtils.isEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        return cxProductConstructionInfoService.importData(list, updateSupport, importLogId);
    }

    @ApiOperation("获取胎胚版本列表")
    @PostMapping("/getEmbryoVersions")
    public List<CxProductConstructionInfo> getEmbryoVersions(@RequestBody CxProductConstructionInfo pc) {
        startPage("create_time desc");
        return cxProductConstructionInfoService.getEmbryoVersions(pc);
    }

    @ApiOperation("获取半部件版本列表")
    @PostMapping("/getPartVersions")
    public List<CxProductConstructionInfo> getPartVersions(@RequestBody CxProductConstructionInfo pc) {
        return constructionParseService.getPartsConstruction(pc, false);
    }
}
