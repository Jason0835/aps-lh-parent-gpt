package com.zlt.aps.monthplan.setting.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.ExportLog;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.api.gateway.system.service.IExportLogService;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.utils.ServletUtils;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.maindata.mapper.MdmModelInfoEntityMapper;
import com.zlt.aps.maindata.service.IMdmModelInfoService;
import com.zlt.aps.monthplan.api.domain.entity.MdmModelInfo;
import com.zlt.aps.monthplan.api.domain.vo.MdmModelInfoExportVo;
import com.zlt.bill.common.controller.AbstractDocBizController;
import com.zlt.bill.common.service.IDocService;
import com.zlt.common.utils.ExcelReadUtils;
import com.zlt.common.utils.PubUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MdmModelInfoController.java
 * 描    述：模具信息 控制层类：....
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-02-24
 */
@Slf4j
@Api(tags = "模具信息")
@RestController
@RequestMapping("/mdmModelInfo")
public class MdmModelInfoController extends AbstractDocBizController<MdmModelInfo> {

    @Autowired
    private IMdmModelInfoService mdmModelInfoService;

    @Autowired
    private MdmModelInfoEntityMapper mdmModelInfoEntityMapper;

    @Autowired
    private IExportLogService iExportLogService;

    /**
     * 查询模具信息列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody MdmModelInfo queryVO) {
        this.startPage(this.getOrderBy());
        QueryWrapper<MdmModelInfo> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, queryVO);
        List<MdmModelInfo> list = mdmModelInfoEntityMapper.selectList(wrapper);
        mdmModelInfoService.setProSize(list);
        return getDataTable(list);
    }

    @Override
    protected String getOrderBy() {
        return "create_time desc";
    }

    /**
     * 保存
     */
    @Log(title = "ui.data.column.mdmModelInfo.modelName", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("保存")
    @PostMapping("/save")
    @Override
    public AjaxResult save(@RequestBody MdmModelInfo billVO) {
        return super.save(billVO);
    }

    /**
     * 删除
     */
    @Log(title = "ui.data.column.mdmModelInfo.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除")
    @DeleteMapping("/remove")
    @Override
    public AjaxResult removeByIds(@RequestBody List<Long> ids) {
        return super.removeByIds(ids);
    }

    /**
     * 获取模具信息详细信息
     */
    @ApiOperation("获取详细信息")
    @GetMapping(value = "/{billId}")
    @Override
    public MdmModelInfo getInfo(@PathVariable("billId") Long billId) {
        return super.getInfo(billId);
    }

    /**
     * 根据集合导入模具信息数据
     *
     * @param importContext 导入上下文
     * @param updateSupport 已存在记录是否更新
     * @return 结果
     */
    @Log(title = "ui.data.column.mdmModelInfo.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    @Override
    public AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        return super.importData(importContext, updateSupport);
    }

    /**
     * 导出列表
     */
    @Log(title = "模具信息", businessType = BusinessType.EXPORT)
    @ApiOperation("导入数据")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody MdmModelInfo queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return super.exportData(queryVO, fileName, response);
    }

    @Override
    protected List<MdmModelInfo> listExportData(MdmModelInfo obj) {
        QueryWrapper<MdmModelInfo> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, obj);
        List<MdmModelInfo> mdmModelInfoList = mdmModelInfoEntityMapper.selectList(wrapper);
        mdmModelInfoService.setProSize(mdmModelInfoList);
        return mdmModelInfoList;
    }

    @Override
    protected IDocService getDocService() {
        return mdmModelInfoService;
    }

    /**
     * 条件拼接
     *
     * @param queryWrapper
     * @param queryVO
     */
    @Override
    protected void builderCondition(QueryWrapper<MdmModelInfo> queryWrapper, MdmModelInfo queryVO) {
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("mouldCode")), "MOULD_CODE", queryVO.getFieldValueByFieldName("mouldCode"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("mouldNo")), "MOULD_NO", queryVO.getFieldValueByFieldName("mouldNo"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("specifications")), "SPECIFICATIONS", queryVO.getFieldValueByFieldName("specifications"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("pattern")), "PATTERN", queryVO.getFieldValueByFieldName("pattern"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("mouldType")), "MOULD_TYPE", queryVO.getFieldValueByFieldName("mouldType"));
    }

    @Override
    protected String getTypeCode() {
        return "0112-1";
    }

    /**
     * 导出模具汇总列表
     * @param queryVO 查询参数
     * @param fileName 文件名
     * @param response 响应
     * @return 结果
     * @throws IOException 异常
     */
    @Log(title = "模具信息", businessType = BusinessType.EXPORT)
    @ApiOperation("导入数据")
    @PostMapping("/exportModelGroup/{fileName}")
    public byte[] exportModelGroup(@RequestBody MdmModelInfo queryVO, @PathVariable("fileName") String fileName,
                               HttpServletResponse response) throws IOException {
        Date beginTime = DateUtils.getNowDate();
        List<MdmModelInfo> list = this.listExportData(queryVO);
        List<MdmModelInfoExportVo> exportVoList = new ArrayList<>();
        Map<String, List<MdmModelInfo>> groupMap = list.stream().collect(Collectors.groupingBy(MdmModelInfo::getMouldNo));
        Set<Map.Entry<String, List<MdmModelInfo>>> entriedSet = groupMap.entrySet();
        for (Map.Entry<String, List<MdmModelInfo>> entry : entriedSet) {
            MdmModelInfoExportVo exportVo = new MdmModelInfoExportVo();
            String key = entry.getKey();
            List<MdmModelInfo> value = entry.getValue();

            exportVo.setMouldNo(key);
            String proSize = "";
            Integer mouldNo = 0;
            for (MdmModelInfo mdmModelInfo : value) {
                proSize = mdmModelInfo.getProSize();
                mouldNo++;
            }
            exportVo.setProSize(proSize);
            exportVo.setMouldNum(mouldNo);
            exportVoList.add(exportVo);
        }
        ExcelUtil<MdmModelInfoExportVo> util = new ExcelUtil<>(MdmModelInfoExportVo.class);
        Workbook workbook = util.exportExcel2(response, exportVoList, fileName);
        byte[] resultBytes = ExcelReadUtils.writeExcel(workbook);
        Date endTime = DateUtils.getNowDate();
        ExportLog exportLog = new ExportLog();
        exportLog.setProcedureCode("0");
        exportLog.setExportParams(queryVO.toString());
        String uri = ServletUtils.getRequest().getRequestURI();
        exportLog.setFunctionCode(uri.split("/")[1]);
        exportLog.setFunctionName(fileName);
        exportLog.setFileName(fileName + ".xlsx");
        exportLog.setRowCount(list.size());
        exportLog.setBeginTime(beginTime);
        exportLog.setEndTime(endTime);
        exportLog.setSpendTime(DateUtils.getDiffTime(endTime, beginTime));
        this.iExportLogService.add(exportLog);
        return resultBytes;
    }

    /**
     * 抓取MES数据
     *
     * @return 结果
     */
    @ApiOperation("抓取MES数据")
    @PostMapping("/mesCapture")
    public AjaxResult mesCapture() {
        return mdmModelInfoService.mesCapture();
    }
}
