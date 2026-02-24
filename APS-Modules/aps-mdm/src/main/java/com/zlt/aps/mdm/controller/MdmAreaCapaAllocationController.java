package com.zlt.aps.mdm.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.ImportLog;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.api.gateway.system.service.IImportErrorLogService;
import com.ruoyi.api.gateway.system.service.IImportLogService;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.utils.JsonI18nConvertUtils;
import com.zlt.aps.mdm.mapper.MdmAreaCapaAllocationEntityMapper;
import com.zlt.aps.mdm.service.IMdmAreaCapaAllocationService;
import com.zlt.aps.mdm.utils.RemoteImportExcelUtils;
import com.zlt.aps.mdm.api.domain.entity.MdmAreaCapaAllocation;
import com.zlt.bill.common.controller.AbstractDocBizController;
import com.zlt.bill.common.service.IDocService;
import com.zlt.common.exception.QueryExprException;
import com.zlt.common.utils.ImportExcelUtils;
import com.zlt.common.utils.PubUtil;
import com.zlt.core.queryformulas.QueryFormulaUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MdmAreaCapaAllocationController.java
 * 描    述：区域产能分配 控制层类：....
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-12-08
 */
@Slf4j
@Api(tags = "区域产能分配")
@RestController
@RequestMapping("/mdmAreaCapaAllocation")
public class MdmAreaCapaAllocationController extends AbstractDocBizController<MdmAreaCapaAllocation> {

    @Autowired
    private IMdmAreaCapaAllocationService mdmAreaCapaAllocationService;

    @Autowired
    private MdmAreaCapaAllocationEntityMapper entityMapper;

    @Autowired
    private IImportLogService iImportLogService;

    @Autowired
    private IImportErrorLogService iImportErrorLogService;

    /**
     * 查询区域产能分配列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody MdmAreaCapaAllocation queryVO) {
        TableDataInfo tableDataInfo = super.list(queryVO);
        List<MdmAreaCapaAllocation> list = (List<MdmAreaCapaAllocation>) tableDataInfo.getRows();
        JsonI18nConvertUtils.conventJsonI18n(list, MdmAreaCapaAllocation.class);
        return tableDataInfo;
    }

    @Override
    protected String getOrderBy() {
        return "create_time desc";
    }

    /**
     * 保存
     */
    @Log(title = "ui.data.column.mdmAreaCapaAllocation.modelName", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("保存")
    @PostMapping("/save")
    @Override
    public AjaxResult save(@RequestBody MdmAreaCapaAllocation billVO) {
        return super.save(billVO);
    }

    /**
     * 删除
     */
    @Log(title = "ui.data.column.mdmAreaCapaAllocation.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除")
    @DeleteMapping("/remove")
    @Override
    public AjaxResult removeByIds(@RequestBody List<Long> ids) {
        return super.removeByIds(ids);
    }


    /**
     * 获取区域产能分配详细信息
     */
    @ApiOperation("获取详细信息")
    @GetMapping(value = "/{billId}")
    @Override
    public MdmAreaCapaAllocation getInfo(@PathVariable("billId") Long billId) {
        MdmAreaCapaAllocation areaCapaAllocation = super.getInfo(billId);
        List<MdmAreaCapaAllocation> list = Collections.singletonList(areaCapaAllocation);
        try {
            QueryFormulaUtil.execFormula(list, this.getQueryFormulas());
        } catch (QueryExprException e) {
            this.logger.error(e.getMessage(), e);
            throw new ServiceException("执行查询公式时发生错误.");
        }
        JsonI18nConvertUtils.conventJsonI18n(list, MdmAreaCapaAllocation.class);
        return areaCapaAllocation;
    }


    /**
     * 根据集合导入区域产能分配数据
     *
     * @param importContext 导入上下文
     * @param updateSupport 已存在记录是否更新
     * @return 结果
     */
    @Log(title = "ui.data.column.mdmAreaCapaAllocation.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    @Override
    public AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        Date beginTime = DateUtils.getNowDate();
        ImportLog importLog = ImportExcelUtils.getImportLogAndUploadFile(importContext.getFileBytes(), importContext.getImportFilePath(), importContext.getProcedureCode(), importContext.getFunctionName(), importContext.getOriFileName(), 1);
        importLog = this.iImportLogService.add(importLog);
        ExcelUtil<MdmAreaCapaAllocation> util = new ExcelUtil<>(this.getTClass());
        InputStream is = new ByteArrayInputStream(importContext.getFileBytes());
        List<MdmAreaCapaAllocation> list = util.importExcel(is);
        return this.doImportDataAsync(list, updateSupport, importLog.getId(), importLog, beginTime);
    }

    public AjaxResult doImportDataAsync(List<MdmAreaCapaAllocation> list, boolean updateSupport, long importLogId, ImportLog importLog, Date beginTime) {
        if (CollectionUtils.isEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        if (list.size() > 500) {
            // 传递请求头信息（主要是语言包），避免主线程执行后清空request，拷贝一个虚拟的request
            ServletRequestAttributes virtualAttr = RemoteImportExcelUtils.copyRequestHeaderAttribute();
            mdmAreaCapaAllocationService.importDataAsync(list, updateSupport, importLogId, importLog, beginTime, virtualAttr);
            return AjaxResult.success(I18nUtil.getMessage("ui.data.column.common.importTimeOut"));
        }
        AjaxResult result = getDocService().importData(list, updateSupport, importLogId);
        Date endTime = DateUtils.getNowDate();
        importLog.setRowCount(list.size());
        importLog.setBeginTime(beginTime);
        importLog.setEndTime(endTime);
        importLog.setSpendTime(DateUtils.getDiffTime(endTime, beginTime));
        ImportExcelUtils.updateImportLogAndFormatMsg(importLog, result, this.iImportLogService);
        ImportExcelUtils.saveImportErrorLogs(result, this.iImportErrorLogService);
        return result;
    }

    /**
     * 导出列表
     */
    @Log(title = "区域产能分配", businessType = BusinessType.EXPORT)
    @ApiOperation("导入数据")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody MdmAreaCapaAllocation queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return super.exportData(queryVO, fileName, response);
    }

    @Override
    protected List<MdmAreaCapaAllocation> listExportData(MdmAreaCapaAllocation obj) {
        QueryWrapper<MdmAreaCapaAllocation> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, obj);
        List<MdmAreaCapaAllocation> list = entityMapper.selectList(wrapper);
        try {
            QueryFormulaUtil.execFormula(list, this.getQueryFormulas());
        } catch (QueryExprException e) {
            this.logger.error(e.getMessage(), e);
            throw new ServiceException("执行查询公式时发生错误.");
        }
        JsonI18nConvertUtils.conventJsonI18n(list, MdmAreaCapaAllocation.class);
        return list;
    }

    @Override
    protected IDocService getDocService() {
        return mdmAreaCapaAllocationService;
    }

    /**
     * 条件拼接
     *
     * @param queryWrapper
     * @param queryVO
     */
    @Override
    protected void builderCondition(QueryWrapper<MdmAreaCapaAllocation> queryWrapper, MdmAreaCapaAllocation queryVO) {
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("factoryCode")), "FACTORY_CODE", queryVO.getFieldValueByFieldName("factoryCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("productTypeCode")), "PRODUCT_TYPE_CODE", queryVO.getFieldValueByFieldName("productTypeCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("year")), "YEAR", queryVO.getFieldValueByFieldName("year"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("month")), "MONTH", queryVO.getFieldValueByFieldName("month"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("areaCode")), "AREA_CODE", queryVO.getFieldValueByFieldName("areaCode"));
    }

    @Override
    protected String getTypeCode() {
        return "MDM0141";
    }

    @Override
    protected String[] getQueryFormulas() {
        return new String[]{
                "createByName->getcolvalue(SYS_USER, nick_name, user_name, createBy)",
                "updateByName->getcolvalue(SYS_USER, nick_name, user_name, updateBy)",
                "areaCodeName->getcolvaluewithcondition(t_dp_area, area_name, area_code, areaCode, is_delete = 0)",
        };
    }

    /**
     * 复制
     *
     * @param entity 参数
     * @return 结果
     */
    @ApiOperation("复制")
    @PostMapping("/copy")
    public AjaxResult copy(@RequestBody MdmAreaCapaAllocation entity) {
        return mdmAreaCapaAllocationService.copy(entity);
    }

    /**
     * 复制前校验
     *
     * @param entity 参数
     * @return 结果
     */
    @ApiOperation("复制前校验")
    @PostMapping("/checkBeforeCopy")
    public AjaxResult checkBeforeCopy(@RequestBody MdmAreaCapaAllocation entity) {
        return mdmAreaCapaAllocationService.checkBeforeCopy(entity);
    }

    /**
     * 获取总产能分配
     *
     * @param entity 参数
     * @return 结果
     */
    @ApiOperation("获取总产能分配")
    @PostMapping("/getSumCapacityAllocation")
    public AjaxResult getSumCapacityAllocation(@RequestBody MdmAreaCapaAllocation entity) {
        QueryWrapper<MdmAreaCapaAllocation> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, entity);
        List<MdmAreaCapaAllocation> list = entityMapper.selectList(wrapper);
        int sum = list.stream().map(MdmAreaCapaAllocation::getCapacityAllocation).mapToInt(BigDecimal::intValue).sum();
        return AjaxResult.success(sum);
    }
}
