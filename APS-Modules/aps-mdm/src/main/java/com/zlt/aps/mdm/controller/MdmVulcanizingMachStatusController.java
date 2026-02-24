package com.zlt.aps.mdm.controller;

import com.alibaba.nacos.common.utils.CollectionUtils;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.security.annotation.PreAuthorize;
import com.ruoyi.common.text.Convert;
import com.zlt.aps.mdm.service.IMdmVulcanizingMachStatusService;
import com.zlt.aps.mdm.api.domain.entity.MdmVulcanizingMachStatus;
import com.zlt.aps.mdm.api.domain.vo.CopyParamVo;
import com.zlt.aps.mdm.api.domain.vo.MdmVulcanizingMachStatusVo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 成型机可用信息Controller
 *
 * @author chen
 */
@Api("硫化机可用信息")
@RestController
@RequestMapping("/docVulcanizationMachStatus")
public class MdmVulcanizingMachStatusController extends BaseController {

    @Autowired
    private IMdmVulcanizingMachStatusService docVulcanizingMachStatusService;

    /**
     * 查询基础数据-硫化机可用信息列表
     */
//    @PreAuthorize(hasPermi = "fac:docVulcanizationMachStatus:list")
//     @DataAuth(docFields = {"b.PRODUCT_TYPE_CODE", "a.FACTORY_CODE"}, docTypes = {DocTypeEnum.PRODUCT_NAME, DocTypeEnum.FACTORY_CODE})
    @ApiOperation("查询基础数据-硫化机可用信息列表")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody MdmVulcanizingMachStatusVo docVulcanizingMachStatus) {
        startPage(false);
        List<MdmVulcanizingMachStatusVo> list = docVulcanizingMachStatusService.selectDocVulcanizingMachStatusEntityList(docVulcanizingMachStatus);
        return getDataTable(list);
    }

    /**
     * 获取基础数据-硫化机可用信息详细信息
     */
    @PreAuthorize(hasPermi = "fac:docVulcanizationMachStatus:query")
    @ApiOperation("获取基础数据-硫化机可用信息详细信息")
    @GetMapping(value = "/{id}")
    public MdmVulcanizingMachStatus getInfo(@PathVariable("id") Long id) {
        return docVulcanizingMachStatusService.getDocVulcanizingMachStatusEntityById(id);
    }

    /**
     * 新增基础数据-硫化机可用信息
     */
    @Log(title = "ui.data.column.docVulcanizationMachStatus.modelName", businessType = BusinessType.INSERT)
    @PreAuthorize(hasPermi = "biz:docVulcanizationMachStatus:add")
    @ApiOperation("新增基础数据-硫化机可用信息")
    @PostMapping("/add")
    public AjaxResult add(@RequestBody MdmVulcanizingMachStatusVo docVulcanizingMachStatus) {
        return toAjax(docVulcanizingMachStatusService.insertDocVulcanizingMachStatusEntity(docVulcanizingMachStatus));
    }

    /**
     * 修改基础数据-硫化机可用信息
     */
    @Log(title = "ui.data.column.docVulcanizationMachStatus.modelName", businessType = BusinessType.UPDATE)
    @PreAuthorize(hasPermi = "biz:docVulcanizationMachStatus:edit")
    @ApiOperation("修改基础数据-硫化机可用信息")
    @PostMapping("/edit")
    public AjaxResult edit(@RequestParam("ids") Long[] ids, @RequestParam("status") String status) {
        return toAjax(docVulcanizingMachStatusService.updateDocVulcanizingMachStatusEntity(ids, status));
    }

    /**
     * 删除基础数据-硫化机可用信息
     */
    @Log(title = "ui.data.column.docVulcanizationMachStatus.modelName", businessType = BusinessType.DELETE)
    @PreAuthorize(hasPermi = "biz:docVulcanizationMachStatus:remove")
    @ApiOperation("删除基础数据-硫化机可用信息")
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(docVulcanizingMachStatusService.deleteDocVulcanizingMachStatusEntityByIds(ids));
    }

    /**
     * 校验基础数据-硫化机可用信息唯一性
     */
    @ApiOperation("校验基础数据-硫化机可用信息唯一性")
    @PostMapping("/checkDocVulcanizingMachStatusUnique")
    public String checkDocVulcanizingMachStatusUnique(@RequestBody MdmVulcanizingMachStatusVo docVulcanizingMachStatus) {
        return docVulcanizingMachStatusService.checkDocVulcanizingMachStatusEntityUnique(docVulcanizingMachStatus);
    }

    /**
     * 复制可用台账信息
     */
    @Log(title = "ui.data.column.docVulcanizationMachStatus.modelName", businessType = BusinessType.INSERT)
    @ApiOperation("复制可用台账信息")
    @PostMapping("/copyDocVulcanizingMachStatus")
    // @DataAuth(docTypes = {DocTypeEnum.FACTORY_CODE})
    public AjaxResult copyDocVulcanizingMachStatus(@RequestBody CopyParamVo copyParamVo) {
        return toAjax(docVulcanizingMachStatusService.copyDocVulcanizingMachStatus(copyParamVo));
    }

    /**
     * 合并可用台账信息
     */
    @Log(title = "ui.data.column.docVulcanizationMachStatus.modelName", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("合并可用台账信息")
    @PostMapping("/mergeDocVulcanizingMachStatus")
    // @DataAuth(docTypes = {DocTypeEnum.FACTORY_CODE})
    public AjaxResult mergeDocVulcanizingMachStatus(@RequestBody CopyParamVo copyParamVo) {
        return toAjax(docVulcanizingMachStatusService.mergeDocVulcanizingMachStatus(copyParamVo));
    }

    /**
     * 生成可用台账信息
     */
    @Log(title = "ui.data.column.docVulcanizationMachStatus.modelName", businessType = BusinessType.INSERT)
    @ApiOperation("生成可用台账信息")
    @PostMapping("/generateDocVulcanizingMachStatus")
    public AjaxResult generateDocVulcanizingMachStatus(@RequestBody CopyParamVo params) {
        return toAjax(docVulcanizingMachStatusService.generateDocVulcanizingMachStatus(params));
    }

    /**
     * 拷贝前校验 1.源月份没有数据 2.复制到的月份有数据是否要覆盖
     *
     * @param param 参数
     * @return 结果
     */
    @Log(title = "ui.data.column.docVulcanizationMachStatus.modelName", businessType = BusinessType.OTHER)
    @ApiOperation("拷贝前校验")
    @PostMapping("/copyValidate")
    public AjaxResult copyValidate(@RequestParam Map<String, Object> param) {
        MdmVulcanizingMachStatusVo params = new MdmVulcanizingMachStatusVo();
        params.setFactoryCode(Convert.toStr(param.get("factoryCode")));
        params.setYear(Convert.toInt(param.get("fromYear")));
        params.setMonth(Convert.toInt(param.get("fromMonth")));
        List<MdmVulcanizingMachStatusVo> fromList = docVulcanizingMachStatusService.selectDocVulcanizingMachStatusEntityList(params);
        if (CollectionUtils.isEmpty(fromList)) {
            return AjaxResult.error("ui.data.column.docVulcanizationMachStatus.sourceDataNotExist");
        }
        params.setYear(Convert.toInt(param.get("copyToYear")));
        params.setMonth(Convert.toInt(param.get("copyToMonth")));
        List<MdmVulcanizingMachStatusVo> toList = docVulcanizingMachStatusService.selectDocVulcanizingMachStatusEntityList(params);
        if (CollectionUtils.isNotEmpty(toList)) {
            return AjaxResult.error("ui.data.column.docVulcanizationMachStatus.existData");
        }
        return AjaxResult.success();
    }


    /**
     * 导出基础数据-硫化机可用信息列表
     */
    // @DataAuth(docFields = {"b.PRODUCT_TYPE_CODE", "a.FACTORY_CODE"}, docTypes = {DocTypeEnum.PRODUCT_NAME, DocTypeEnum.FACTORY_CODE})
    @Log(title = "ui.data.column.docVulcanizationMachStatus.modelName", businessType = BusinessType.EXPORT)
//    @PreAuthorize(hasPermi = "fac:docVulcanizationMachStatus:export")
    @ApiOperation("导出基础数据-硫化机可用信息列表")
    @PostMapping("/getList")
    public List<MdmVulcanizingMachStatusVo> getList(@RequestBody MdmVulcanizingMachStatusVo docVulcanizingMachStatusVo) {
        return docVulcanizingMachStatusService.selectDocVulcanizingMachStatusEntityList(docVulcanizingMachStatusVo);
    }
//

    /**
     * 根据集合导入基础数据-硫化机可用信息数据
     *
     * @param list          集合
     * @param updateSupport 已存在记录是否更新
     * @param importLogId   导入日志id
     * @return 结果
     */
    @Log(title = "ui.data.column.docVulcanizationMachStatus.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入基础数据-硫化机可用信息数据")
    @PostMapping("/importData/{updateSupport}/{importLogId}")
    public AjaxResult importData(@RequestBody List<MdmVulcanizingMachStatusVo> list, @PathVariable("updateSupport") boolean updateSupport, @PathVariable("importLogId") Long importLogId) {
        if (CollectionUtils.isEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        return docVulcanizingMachStatusService.importData(list, updateSupport, importLogId);
    }
}
