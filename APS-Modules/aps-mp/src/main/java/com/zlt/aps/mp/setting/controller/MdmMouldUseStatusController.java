package com.zlt.aps.mp.setting.controller;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.security.annotation.PreAuthorize;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.maindata.service.IMdmMouldUseStatusService;
import com.zlt.aps.mp.api.domain.entity.MdmMouldUseStatus;
import com.zlt.aps.mp.api.domain.vo.MdmMouldUseStatusVo;
import com.zlt.aps.mp.api.domain.vo.PeriodInfo;
import com.zlt.common.controller.BusiController;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * 模具可用状态Controller
 *
 * @author leo
 * @date 2021-08-27
 */
@RestController
@RequestMapping("/mouldusestatus")
public class MdmMouldUseStatusController extends BusiController<MdmMouldUseStatus> {
    @Autowired
    private IMdmMouldUseStatusService imouldUseStatusServiceMdm;

    /**
     * 查询模具可用状态列表
     */
//    @PreAuthorize(hasPermi = "lean:mouldusestatus:list")
//     @DataAuth(docFields = {"t.factory_code"}, docTypes = {DocTypeEnum.FACTORY_CODE})
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody MdmMouldUseStatus mdmMouldUseStatus) {
        startPage("`YEAR` DESC, `MONTH` DESC");
        if (StringUtils.isNotEmpty(mdmMouldUseStatus.getProductCode())) {
            List<MdmMouldUseStatus> list = imouldUseStatusServiceMdm.selectMouldUseStatusListForProductCode(mdmMouldUseStatus);
            return getDataTable(list);
        }
        List<MdmMouldUseStatus> list = imouldUseStatusServiceMdm.selectMouldUseStatusList(mdmMouldUseStatus);
        return getDataTable(list);
    }

    /**
     * 查询模具可用状态汇总
     */
    // @DataAuth(docFields = {"t.factory_code"}, docTypes = {DocTypeEnum.FACTORY_CODE})
    @PostMapping("/listTotal")
    public MdmMouldUseStatusVo listTotal(@RequestBody MdmMouldUseStatus mdmMouldUseStatus) {
        return imouldUseStatusServiceMdm.listTotal(mdmMouldUseStatus);
    }


    /**
     * 获取模具可用状态详细信息
     */
//    @PreAuthorize(hasPermi = "lean:mouldusestatus:query")
    @GetMapping(value = "/{id}")
    public MdmMouldUseStatus getInfo(@PathVariable("id") Long id) {
        return imouldUseStatusServiceMdm.selectMouldUseStatusById(id);
    }

    /**
     * 新增模具可用状态
     */
//    @PreAuthorize(hasPermi = "lean:mouldusestatus:add")
    @PostMapping("/add")
    @Log(title = "ui.data.column.mouldusestatus.modelName", businessType = BusinessType.INSERT)
    public AjaxResult add(@RequestBody MdmMouldUseStatus mdmMouldUseStatus) {
        return toAjax(imouldUseStatusServiceMdm.insertMouldUseStatus(mdmMouldUseStatus));
    }

    /**
     * 复制模具可用状态
     */
    @PreAuthorize(hasPermi = "lean:mouldusestatus:copy")
    @PostMapping("/copy")
    @Log(title = "ui.data.column.mouldusestatus.modelName", businessType = BusinessType.UPDATE)
    public AjaxResult copy(@RequestBody PeriodInfo vo) {
        return imouldUseStatusServiceMdm.copy(vo);
    }


    /**
     * 合并模具可用状态
     */
    @PreAuthorize(hasPermi = "lean:mouldusestatus:copy")
    @PostMapping("/merge")
    @Log(title = "ui.data.column.mouldusestatus.modelName", businessType = BusinessType.INSERT_OR_UPDATE)
    public AjaxResult merge(@RequestBody PeriodInfo vo) {
        return imouldUseStatusServiceMdm.merge(vo);
    }

    /**
     * 修改模具可用状态
     */
//    @PreAuthorize(hasPermi = "lean:mouldusestatus:edit")
    @PostMapping("/edit")
    @Log(title = "ui.data.column.mouldusestatus.modelName", businessType = BusinessType.UPDATE)
    public AjaxResult edit(@RequestBody MdmMouldUseStatus mdmMouldUseStatus) {
        return toAjax(imouldUseStatusServiceMdm.updateMouldUseStatus(mdmMouldUseStatus));
    }

    /**
     * 删除模具可用状态
     */
    @PreAuthorize(hasPermi = "lean:mouldusestatus:remove")
    @DeleteMapping("/{ids}")
    @Log(title = "ui.data.column.mouldusestatus.modelName", businessType = BusinessType.DELETE)
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(imouldUseStatusServiceMdm.deleteMouldUseStatusByIds(ids));
    }

    /**
     * 导出模具可用状态列表
     */
//    @PreAuthorize(hasPermi = "lean:mouldusestatus:export")
//     @DataAuth(docFields = {"t.factory_code"}, docTypes = {DocTypeEnum.FACTORY_CODE})
    @PostMapping("/getList")
    @Log(title = "ui.data.column.mouldusestatus.modelName", businessType = BusinessType.EXPORT)
    public List<MdmMouldUseStatus> getList(@RequestBody MdmMouldUseStatus mdmMouldUseStatus) {
        startPage("create_time desc");
        if (StringUtils.isNotEmpty(mdmMouldUseStatus.getProductCode())) {
            List<MdmMouldUseStatus> list = imouldUseStatusServiceMdm.selectMouldUseStatusListForProductCode(mdmMouldUseStatus);
            return list;
        }
        return imouldUseStatusServiceMdm.selectMouldUseStatusList(mdmMouldUseStatus);
    }

    /**
     * 导出模具可用状态列表
     */
    @Log(title = "ui.data.column.mouldusestatus.modelName", businessType = BusinessType.EXPORT)
    @PostMapping("/exportData/{fileName}")
    public byte[] exportData(@RequestBody MdmMouldUseStatus mdmMustFinishPlan, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return commonExport(mdmMustFinishPlan, fileName, response);
    }

    @Override
    public List<MdmMouldUseStatus> listExportData(MdmMouldUseStatus mdmMustFinishPlan) {
        return this.getList(mdmMustFinishPlan);
    }

    /**
     * 校验模具可用状态唯一性
     */
    @ApiOperation("校验模具可用状态唯一性")
    @PostMapping("/checkMouldUseStatusUnique")
    public String checkMouldUseStatusUnique(@RequestBody MdmMouldUseStatus mdmMouldUseStatus) {
        return imouldUseStatusServiceMdm.checkMouldUseStatusUnique(mdmMouldUseStatus);
    }

    // /**
    //  * @param list          集合
    //  * @param updateSupport 已存在记录是否更新
    //  * @param importLogId   导入日志id
    //  * @return 结果
    //  */
    // @Log(title = "ui.data.column.mouldusestatus.modelName", businessType = BusinessType.IMPORT)
    // @ApiOperation("excel导入")
    // @PostMapping("/importData/{updateSupport}/{importLogId}")
    // public AjaxResult importData(@RequestBody List<MdmMouldUseStatus> list, @PathVariable("updateSupport") boolean updateSupport, @PathVariable("importLogId") Long importLogId) {
    //     if (CollectionUtils.isEmpty(list)) {
    //         return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
    //     }
    //     return imouldUseStatusServiceMdm.importData(list, updateSupport, importLogId);
    // }

    /**
     * 根据集合导入模具可用状态
     *
     * @param importContext 导入上下文
     * @param updateSupport 已存在记录是否更新
     * @return 结果
     */
    @Log(title = "ui.data.column.mouldusestatus.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入模具可用状态")
    @PostMapping("/importData/{updateSupport}")
    public AjaxResult importData(@RequestBody ImportContext importContext, @PathVariable("updateSupport") boolean updateSupport) throws Exception {
        return commonImport(importContext, updateSupport);
    }

    @Override
    public AjaxResult doImportData(List<MdmMouldUseStatus> list, boolean updateSupport, long importLogId) {
        if (CollectionUtils.isEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        return imouldUseStatusServiceMdm.importData(list, updateSupport, importLogId);
    }
}
