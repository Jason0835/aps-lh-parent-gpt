package com.zlt.aps.mp.setting.controller;

import com.alibaba.nacos.common.utils.CollectionUtils;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.security.annotation.PreAuthorize;
import com.ruoyi.common.text.Convert;
import com.zlt.aps.maindata.service.IMdmMoldingMachineStatusService;
import com.zlt.aps.monthplan.api.domain.entity.MdmMoldingMachineStatus;
import com.zlt.aps.monthplan.api.domain.vo.CopyParamVo;
import com.zlt.aps.monthplan.api.domain.vo.MdmMoldingMachineStatusVo;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 基础数据-成型机可用信息Controller
 *
 * @author chen
 * @date 2021-09-08
 */
@RestController
@RequestMapping("/docMoldingMachineStatus")
public class MdmMoldingMachineStatusController extends BaseController {
    @Autowired
    private IMdmMoldingMachineStatusService docMoldingMachineStatusService;

    /**
     * 查询基础数据-成型机可用信息列表
     */
//    @PreAuthorize(hasPermi = "fac:docMoldingMachineStatus:list")
//     @DataAuth(docFields = {"b.PRODUCT_TYPE_CODE", "a.FACTORY_CODE"}, docTypes = {DocTypeEnum.PRODUCT_NAME, DocTypeEnum.FACTORY_CODE})
    @ApiOperation("查询基础数据-成型机可用信息列表")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody MdmMoldingMachineStatus docMoldingMachineStatus) {
        startPage(false);
        List<MdmMoldingMachineStatusVo> list = docMoldingMachineStatusService.selectDocMoldingMachineStatusList(docMoldingMachineStatus);
        return getDataTable(list);
    }

    /**
     * 获取基础数据-成型机可用信息详细信息
     */
    @PreAuthorize(hasPermi = "fac:docMoldingMachineStatus:query")
    @ApiOperation("获取基础数据-成型机可用信息详细信息")
    @GetMapping(value = "/{id}")
    public MdmMoldingMachineStatus getInfo(@PathVariable("id") Long id) {
        return docMoldingMachineStatusService.selectDocMoldingMachineStatusById(id);
    }

    /**
     * 新增基础数据-成型机可用信息
     */
    @Log(title = "ui.data.column.docMoldingMachineStatus.modelName", businessType = BusinessType.INSERT)
    @PreAuthorize(hasPermi = "fac:docMoldingMachineStatus:add")
    @ApiOperation("新增基础数据-成型机可用信息")
    @PostMapping("/add")
    public AjaxResult add(@RequestBody MdmMoldingMachineStatus docMoldingMachineStatus) {
        return toAjax(docMoldingMachineStatusService.insertDocMoldingMachineStatus(docMoldingMachineStatus));
    }

    /**
     * 修改基础数据-成型机可用信息
     */
    @Log(title = "ui.data.column.docMoldingMachineStatus.modelName", businessType = BusinessType.UPDATE)
    @PreAuthorize(hasPermi = "fac:docMoldingMachineStatus:edit")
    @ApiOperation("修改基础数据-成型机可用信息")
    @PostMapping("/edit")
    public AjaxResult edit(@RequestParam("ids") Long[] ids, @RequestParam("status") String status) {
        return toAjax(docMoldingMachineStatusService.updateDocMoldingMachineStatus(ids, status));
    }

    /**
     * 删除基础数据-成型机可用信息
     */
    @Log(title = "ui.data.column.docMoldingMachineStatus.modelName", businessType = BusinessType.DELETE)
    @PreAuthorize(hasPermi = "fac:docMoldingMachineStatus:remove")
    @ApiOperation("删除基础数据-成型机可用信息")
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(docMoldingMachineStatusService.deleteDocMoldingMachineStatusByIds(ids));
    }

    /**
     * 导出基础数据-成型机可用信息列表
     */
    // @DataAuth(docFields = {"b.PRODUCT_TYPE_CODE", "a.FACTORY_CODE"}, docTypes = {DocTypeEnum.PRODUCT_NAME, DocTypeEnum.FACTORY_CODE})
    @Log(title = "ui.data.column.docMoldingMachineStatus.modelName", businessType = BusinessType.EXPORT)
//    @PreAuthorize(hasPermi = "fac:docMoldingMachineStatus:export")
    @ApiOperation("导出基础数据-成型机可用信息列表")
    @PostMapping("/getList")
    public List<MdmMoldingMachineStatusVo> getList(@RequestBody MdmMoldingMachineStatusVo docMoldingMachineStatus) {
        startPage(false);
        return docMoldingMachineStatusService.selectDocMoldingMachineStatusList(docMoldingMachineStatus);
    }

    /**
     * 校验基础数据-成型机可用信息唯一性
     */
    @ApiOperation("校验基础数据-成型机可用信息唯一性")
    @PostMapping("/checkDocMoldingMachineStatusUnique")
    public String checkDocMoldingMachineStatusUnique(@RequestBody MdmMoldingMachineStatus docMoldingMachineStatus) {
        return docMoldingMachineStatusService.checkDocMoldingMachineStatusUnique(docMoldingMachineStatus);
    }

    /**
     * 复制可用台账信息
     */
    @Log(title = "ui.data.column.docMoldingMachineStatus.modelName", businessType = BusinessType.INSERT)
    @ApiOperation("复制可用台账信息")
    @PostMapping("/copyDocVulcanizingMachStatus")
    // @DataAuth(docTypes = {DocTypeEnum.FACTORY_CODE})
    public AjaxResult copyMoldingMachineStatus(@RequestBody CopyParamVo params) {
        return toAjax(docMoldingMachineStatusService.copyMoldingMachineStatus(params));
    }

    /**
     * 合并可用台账信息
     */
    @Log(title = "ui.data.column.docMoldingMachineStatus.modelName", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("复制可用台账信息")
    @PostMapping("/mergeMoldingMachineStatus")
    // @DataAuth(docTypes = {DocTypeEnum.FACTORY_CODE})
    public AjaxResult mergeMoldingMachineStatus(@RequestBody CopyParamVo params) {
        return toAjax(docMoldingMachineStatusService.mergeMoldingMachineStatus(params));
    }

    /**
     * 生成可用台账信息
     */
    @Log(title = "ui.data.column.docMoldingMachineStatus.modelName", businessType = BusinessType.INSERT)
    @ApiOperation("生成可用台账信息")
    @PostMapping("/generateDocVulcanizingMachStatus")
    // @DataAuth(docTypes = {DocTypeEnum.FACTORY_CODE})
    public AjaxResult generateMoldingMachineStatus(@RequestBody CopyParamVo params) {
        return toAjax(docMoldingMachineStatusService.generateMoldingMachineStatus(params));
    }

    /**
     * 拷贝前校验 1.源月份没有数据 2.复制到的月份有数据是否要覆盖
     *
     * @return 结果
     */
    @Log(title = "ui.data.column.docMoldingMachineStatus.modelName", businessType = BusinessType.OTHER)
    @ApiOperation("拷贝前校验")
    @PostMapping("/copyValidate")
    public AjaxResult copyValidate(@RequestParam Map<String, Object> params) {
        MdmMoldingMachineStatus entity = new MdmMoldingMachineStatus();
        entity.setFactoryCode(Convert.toStr(params.get("factoryCode")));
        entity.setYear(Convert.toInt(params.get("fromYear")));
        entity.setMonth(Convert.toInt(params.get("fromMonth")));
        List<MdmMoldingMachineStatusVo> fromList = docMoldingMachineStatusService.selectDocMoldingMachineStatusList(entity);
        if (CollectionUtils.isEmpty(fromList)) {
            return AjaxResult.error("ui.data.column.docVulcanizationMachStatus.sourceDataNotExist");
        }
        entity.setYear(Convert.toInt(params.get("copyToYear")));
        entity.setMonth(Convert.toInt(params.get("copyToMonth")));
        List<MdmMoldingMachineStatusVo> toList = docMoldingMachineStatusService.selectDocMoldingMachineStatusList(entity);
        if (CollectionUtils.isNotEmpty(toList)) {
            return AjaxResult.error("ui.data.column.docVulcanizationMachStatus.existData");
        }
        return AjaxResult.success();
    }

    /**
     * 根据集合导入基础数据-成型机可用信息数据
     *
     * @param list          集合
     * @param updateSupport 已存在记录是否更新
     * @param importLogId   导入日志id
     * @return 结果
     */
    @Log(title = "ui.data.column.docMoldingMachineStatus.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入基础数据-成型机可用信息数据")
    @PostMapping("/importData")
    public AjaxResult importData(@RequestBody List<MdmMoldingMachineStatusVo> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        if (CollectionUtils.isEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        return docMoldingMachineStatusService.importData(list, updateSupport, importLogId);
    }
}
