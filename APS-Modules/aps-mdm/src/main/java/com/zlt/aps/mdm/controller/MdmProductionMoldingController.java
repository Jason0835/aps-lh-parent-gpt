package com.zlt.aps.mdm.controller;

import com.alibaba.nacos.client.naming.utils.CollectionUtils;
import com.github.pagehelper.PageInfo;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.security.annotation.PreAuthorize;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.mdm.service.IMdmMaterialInfoService;
import com.zlt.aps.mdm.service.IMdmProductionMoldingService;
import com.zlt.aps.mdm.api.domain.entity.MdmMaterialInfo;
import com.zlt.aps.mdm.api.domain.entity.MdmProductionMolding;
import com.zlt.aps.mdm.api.domain.vo.MdmProductionMoldingPageVo;
import com.zlt.aps.mdm.api.domain.vo.MdmProductionMoldingVo;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 分厂成型正在生产的品种Controller
 *
 * @author hsc
 * @date 2021-08-30
 */
@RestController
@RequestMapping("/factoryProductionProduct")
public class MdmProductionMoldingController extends BaseController {

    @Resource
    private IMdmProductionMoldingService ifactoryProductionProductService;

    @Autowired
    private IMdmMaterialInfoService iProductInfoService;

    /**
     * 查询分厂成型正在生产的品种列表
     */
//    @PreAuthorize(hasPermi = "lean:factoryProductionProduct:list")
//     @DataAuth(docTypes = {DocTypeEnum.FACTORY_CODE})
    @ApiOperation("查询分厂成型正在生产的品种列表")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody MdmProductionMolding mdmProductionMolding) {
        startPage(false);
        List<MdmProductionMolding> list = ifactoryProductionProductService.selectFactoryProductionProductList(mdmProductionMolding);
        List<MdmProductionMoldingVo> vos = buildVoList(list);
        long rows = (new PageInfo(list)).getTotal();
        TableDataInfo realPage = getDataTable(vos);
        realPage.setTotal(rows);
        return realPage;
    }

    /**
     * 补充物料名称字段的vo
     */
    private List<MdmProductionMoldingVo> buildVoList(List<MdmProductionMolding> list) {
        List<MdmProductionMoldingVo> vos = new ArrayList<>();
        if (CollectionUtils.isEmpty(list)) {
            return vos;
        }

        List<String> codeList = list.stream().map(MdmProductionMolding::getProductCode).filter(StringUtils::isNotBlank).distinct().collect(Collectors.toList());
        List<MdmMaterialInfo> infoList = iProductInfoService.selectListByProductCode(codeList);
        Map<String, String> infoMap = infoList.stream().filter(v -> StringUtils.isNotBlank(v.getProductTypeCode()))
                .collect(Collectors.toMap(MdmMaterialInfo::getMaterialCode, MdmMaterialInfo::getProductTypeCode, (v1, v2) -> v1));
        list.forEach(l -> {
            MdmProductionMoldingVo vo = new MdmProductionMoldingVo();
            BeanUtils.copyProperties(l, vo);
            vo.setProductName(infoMap.get(vo.getProductCode()));
            vos.add(vo);
        });

        return vos;
    }

    /**
     * 导出分厂成型正在生产的品种列表
     */
//    @PreAuthorize(hasPermi = "lean:factoryProductionProduct:export")
    @Log(title = "ui.data.column.factoryProductionProduct.modelName", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, MdmProductionMolding mdmProductionMolding) throws IOException {
        List<MdmProductionMolding> list = ifactoryProductionProductService.selectFactoryProductionProductList(mdmProductionMolding);
        ExcelUtil<MdmProductionMolding> util = new ExcelUtil<MdmProductionMolding>(MdmProductionMolding.class);
        util.exportExcel(response, list, "分厂成型正在生产的品种数据");
    }

    /**
     * 获取分厂成型正在生产的品种详细信息
     */
//    @PreAuthorize(hasPermi = "lean:factoryProductionProduct:query")
    @ApiOperation("获取分厂成型正在生产的品种详细信息")
    @GetMapping(value = "/{id}")
    public MdmProductionMolding getInfo(@PathVariable("id") Long id) {
        return ifactoryProductionProductService.selectFactoryProductionProductById(id);
    }

    /**
     * 新增分厂成型正在生产的品种
     */
    @Log(title = "ui.data.column.factoryProductionProduct.modelName", businessType = BusinessType.INSERT)
//    @PreAuthorize(hasPermi = "lean:factoryProductionProduct:edit")
    @ApiOperation("新增分厂成型正在生产的品种")
    @PostMapping("/add")
    public AjaxResult add(@RequestBody MdmProductionMolding mdmProductionMolding) {
        // 是否新增
        Boolean isAdd = doJugeFactoryProductionProduct(mdmProductionMolding);
        if (!isAdd) {
            return AjaxResult.error(I18nUtil.getMessage("lean.factoryProductionProduct.unique.msg"));
        } else {
            return toAjax(ifactoryProductionProductService.insertFactoryProductionProduct(mdmProductionMolding));
        }
    }

    /**
     * 修改分厂成型正在生产的品种
     */
    @Log(title = "ui.data.column.factoryProductionProduct.modelName", businessType = BusinessType.UPDATE)
//    @PreAuthorize(hasPermi = "lean:factoryProductionProduct:edit")
    @ApiOperation("修改分厂成型正在生产的品种")
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody MdmProductionMolding mdmProductionMolding) {
        // 是否修改
        Boolean isModify = doJugeFactoryProductionProduct(mdmProductionMolding);
        if (!isModify) {
            return AjaxResult.error(I18nUtil.getMessage("lean.factoryProductionProduct.unique.msg"));
        } else {
            return toAjax(ifactoryProductionProductService.updateFactoryProductionProduct(mdmProductionMolding));
        }

    }

    /**
     * 删除分厂成型正在生产的品种
     */
    @Log(title = "ui.data.column.factoryProductionProduct.modelName", businessType = BusinessType.DELETE)
    @PreAuthorize(hasPermi = "lean:factoryProductionProduct:remove")
    @ApiOperation("删除分厂成型正在生产的品种")
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(ifactoryProductionProductService.deleteFactoryProductionProductByIds(ids));
    }

    /**
     * 导出分厂成型正在生产的品种列表
     */
    @Log(title = "ui.data.column.factoryProductionProduct.modelName", businessType = BusinessType.EXPORT)
//    @PreAuthorize(hasPermi = "lean:factoryProductionProduct:export")
    @ApiOperation("导出分厂成型正在生产的品种列表")
    // @DataAuth(docTypes = {DocTypeEnum.FACTORY_CODE})
    @PostMapping("/getList")
    public List<MdmProductionMoldingVo> getList(@RequestBody MdmProductionMolding mdmProductionMolding) {
        startPage(false);
        List<MdmProductionMolding> list = ifactoryProductionProductService.selectFactoryProductionProductList(mdmProductionMolding);
        return buildVoList(list);
    }

    /**
     * 校验分厂成型正在生产的品种唯一性
     */
    @ApiOperation("校验分厂成型正在生产的品种唯一性")
    @PostMapping("/checkFactoryProductionProductUnique")
    public String checkFactoryProductionProductUnique(@RequestBody MdmProductionMolding mdmProductionMolding) {
        return ifactoryProductionProductService.checkFactoryProductionProductUnique(mdmProductionMolding);
    }

    /**
     * 根据集合导入分厂成型正在生产的品种数据
     *
     * @param list          集合
     * @param updateSupport 已存在记录是否更新
     * @param importLogId   导入日志id
     * @return 结果
     */
    @Log(title = "ui.data.column.factoryProductionProduct.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入分厂成型正在生产的品种数据")
    @PostMapping("/importData/{updateSupport}/{importLogId}")
    public AjaxResult importData(@RequestBody List<MdmProductionMolding> list, @PathVariable("updateSupport") boolean updateSupport, @PathVariable("importLogId") Long importLogId) {
        if (CollectionUtils.isEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        return ifactoryProductionProductService.importData(list, updateSupport, importLogId);
    }

    /**
     * 校验是否新增
     *
     * @param mdmProductionMolding
     * @return
     */
    private Boolean doJugeFactoryProductionProduct(MdmProductionMolding mdmProductionMolding) {
        // 查询是否存在分厂成型正在生产品种
        List<MdmProductionMolding> mdmProductionMoldings = ifactoryProductionProductService.selectFactoryProductionProductList(mdmProductionMolding);
        return CollectionUtils.isEmpty(mdmProductionMoldings);
    }

    // /**
    //  * 抓取数据
    //  *
    //  * @param year
    //  * @param month
    //  * @return
    //  */
    // @ApiOperation("抓取成型在产品种")
    // @PostMapping("/grabData")
    // @Log(title = "ui.data.column.factoryProductionProduct.modelName", businessType = BusinessType.OTHER)
    // public AjaxResult grabData(@RequestParam("year") Long year, @RequestParam("month") Long month) {
    //     ifactoryProductionProductService.grabData(year, month);
    //     return AjaxResult.success();
    // }

    @ApiOperation("获取成型法")
    @PostMapping("/getMachineMethod")
    AjaxResult getMachineMethod(@RequestBody MdmProductionMoldingPageVo vo) {
        MdmProductionMoldingPageVo result = ifactoryProductionProductService.getMachineMethod(vo);
        return AjaxResult.success(result);
    }
}
