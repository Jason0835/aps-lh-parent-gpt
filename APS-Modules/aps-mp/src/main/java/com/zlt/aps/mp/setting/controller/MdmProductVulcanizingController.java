package com.zlt.aps.mp.setting.controller;

import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.security.annotation.PreAuthorize;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.maindata.service.IMdmMaterialInfoService;
import com.zlt.aps.maindata.service.IMdmProductVulcanizingService;
import com.zlt.aps.maindata.service.IVulcanizingMachineService;
import com.zlt.aps.monthplan.api.domain.entity.MdmMaterialInfo;
import com.zlt.aps.monthplan.api.domain.entity.MdmProductVulcanizing;
import com.zlt.aps.monthplan.api.domain.entity.VulcanizingMachine;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.collections.CollectionUtils;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * 基础数据-硫化机正在生产品种Controller
 *
 * @author hsc
 * @date 2021-09-01
 */
@RestController
@RequestMapping("/vulcanization")
public class MdmProductVulcanizingController extends BaseController {

    @Autowired
    private IMdmProductVulcanizingService idocProductVulcanizingService;
    @Autowired
    private IVulcanizingMachineService iDocVulcanizingMachineService;
    @Autowired
    private IMdmMaterialInfoService iMdmMaterialInfoService;

    /**
     * 查询基础数据-硫化机正在生产品种列表
     */
//    @PreAuthorize(hasPermi = "fac:vulcanization:list")
//     @DataAuth(docFields = {"PRODUCT_TYPE_CODE", "FACTORY_CODE"}, docTypes = {DocTypeEnum.PRODUCT_NAME, DocTypeEnum.FACTORY_CODE})
    @ApiOperation("查询基础数据-硫化机正在生产品种列表")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody MdmProductVulcanizing docProductVulcanization) {
        startPage("create_time desc");
        List<MdmProductVulcanizing> list = idocProductVulcanizingService.selectDocProductVulcanizationList(docProductVulcanization);
        return getDataTable(list);
    }

    /**
     * 导出基础数据-硫化机正在生产品种列表
     */
    @PreAuthorize(hasPermi = "fac:vulcanization:export")
    @Log(title = "ui.data.column.vulcanization.modelName", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, MdmProductVulcanizing docProductVulcanization) throws IOException {
        List<MdmProductVulcanizing> list = idocProductVulcanizingService.selectDocProductVulcanizationList(docProductVulcanization);
        ExcelUtil<MdmProductVulcanizing> util = new ExcelUtil<MdmProductVulcanizing>(MdmProductVulcanizing.class);
        util.exportExcel(response, list, "基础数据-硫化机正在生产品种数据");
    }

    /**
     * 获取基础数据-硫化机正在生产品种详细信息
     */
    @PreAuthorize(hasPermi = "fac:vulcanization:query")
    @ApiOperation("获取基础数据-硫化机正在生产品种详细信息")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return AjaxResult.success(idocProductVulcanizingService.selectDocProductVulcanizationById(id));
    }

    /**
     * 新增基础数据-硫化机正在生产品种
     */
    @Log(title = "ui.data.column.vulcanization.modelName", businessType = BusinessType.INSERT)
    @PreAuthorize(hasPermi = "lean:vulcanization:add")
    @ApiOperation("新增基础数据-硫化机正在生产品种")
    @PostMapping("/add")
    public AjaxResult add(@RequestBody MdmProductVulcanizing docProductVulcanization) {
        AjaxResult error = buildMachineId(docProductVulcanization);
        if (error != null) return error;
        return toAjax(idocProductVulcanizingService.insertDocProductVulcanization(docProductVulcanization));
    }

    @Nullable
    private AjaxResult buildMachineId(MdmProductVulcanizing docProductVulcanization) {
        if (StringUtils.isNotBlank(docProductVulcanization.getVulcanizingMachineCode())) {
            // 查询硫化机档案信息
            VulcanizingMachine vulcanizingMachine = new VulcanizingMachine();
            List<VulcanizingMachine> machineList = iDocVulcanizingMachineService.selectListByVulcanizingMachine(vulcanizingMachine);
            if (CollectionUtils.isEmpty(machineList)) {
                return AjaxResult.error(I18nUtil.getMessage("biz.error.vulcanizingMachine.is.not.exist"));
            }
            // 硫化机id
            docProductVulcanization.setVulcanizingMachineId(machineList.get(0).getId());
        }
        return null;
    }

    /**
     * 修改基础数据-硫化机正在生产品种
     */
    @Log(title = "ui.data.column.vulcanization.modelName", businessType = BusinessType.UPDATE)
    @PreAuthorize(hasPermi = "fac:vulcanization:edit")
    @ApiOperation("修改基础数据-硫化机正在生产品种")
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody MdmProductVulcanizing docProductVulcanization) {
        AjaxResult error = buildMachineId(docProductVulcanization);
        if (error != null) return error;
        return toAjax(idocProductVulcanizingService.updateDocProductVulcanization(docProductVulcanization));
    }

    /**
     * 删除基础数据-硫化机正在生产品种
     */
    @Log(title = "ui.data.column.vulcanization.modelName", businessType = BusinessType.DELETE)
    @PreAuthorize(hasPermi = "lean:vulcanization:remove")
    @ApiOperation("删除基础数据-硫化机正在生产品种")
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(idocProductVulcanizingService.deleteDocProductVulcanizationByIds(ids));
    }

    /**
     * 导出基础数据-硫化机正在生产品种列表
     */
    @Log(title = "ui.data.column.vulcanization.modelName", businessType = BusinessType.EXPORT)
//    @PreAuthorize(hasPermi = "fac:vulcanization:export")
    @ApiOperation("导出基础数据-硫化机正在生产品种列表")
    @PostMapping("/getList")
    public List<MdmProductVulcanizing> getList(@RequestBody MdmProductVulcanizing docProductVulcanization) {
        startPage("create_time desc");
        return idocProductVulcanizingService.selectDocProductVulcanizationList(docProductVulcanization);
    }

    /**
     * 校验基础数据-硫化机正在生产品种唯一性
     */
    @ApiOperation("校验基础数据-硫化机正在生产品种唯一性")
    @PostMapping("/checkDocProductVulcanizationUnique")
    public String checkDocProductVulcanizationUnique(@RequestBody MdmProductVulcanizing docProductVulcanization) {
        return idocProductVulcanizingService.checkDocProductVulcanizationUnique(docProductVulcanization);
    }

    /**
     * 根据集合导入基础数据-硫化机正在生产品种数据
     *
     * @param list          集合
     * @param updateSupport 已存在记录是否更新
     * @param importLogId   导入日志id
     * @return 结果
     */
    @Log(title = "ui.data.column.vulcanization.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入基础数据-硫化机正在生产品种数据")
    @PostMapping("/importData/{updateSupport}/{importLogId}")
    public AjaxResult importData(@RequestBody List<MdmProductVulcanizing> list, @PathVariable("updateSupport") boolean updateSupport, @PathVariable("importLogId") Long importLogId) {
        if (CollectionUtils.isEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        return idocProductVulcanizingService.importData(list, updateSupport, importLogId);
    }

    @ApiOperation("获取物料信息")
    @PostMapping("/getProductInfo")
    public AjaxResult getProductInfo(@RequestParam("productCode") String productCode) {
        // 查询物料信息
        List<MdmMaterialInfo> productInfoList = iMdmMaterialInfoService.selectListByProductCode(Collections.singletonList(productCode));
        if (CollectionUtils.isNotEmpty(productInfoList)) {
            return AjaxResult.success(productInfoList.get(0));
        }
        return AjaxResult.success();
    }
}
