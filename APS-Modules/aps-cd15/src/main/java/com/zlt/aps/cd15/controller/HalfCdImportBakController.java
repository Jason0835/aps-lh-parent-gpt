package com.zlt.aps.cd15.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.ExportLog;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.api.gateway.system.domain.ImportLog;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.api.gateway.system.service.IExportLogService;
import com.ruoyi.api.gateway.system.service.IImportErrorLogService;
import com.ruoyi.api.gateway.system.service.IImportLogService;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.utils.ServletUtils;
import com.ruoyi.common.core.utils.reflect.ReflectUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.security.annotation.RequiresPermissions;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.cd15.api.domain.entity.Cd15ScheduleResult;
import com.zlt.aps.cd15.api.domain.entity.HalfCdImportBak;
import com.zlt.aps.cd15.api.domain.vo.HalfCdImportBakExportVo;
import com.zlt.aps.cd15.entity.Cd15Params;
import com.zlt.aps.cd15.enums.CdMachineExportEnums;
import com.zlt.aps.cd15.mapper.Cd15ParamsMapper;
import com.zlt.aps.cd15.mapper.HalfCdImportBakEntityMapper;
import com.zlt.aps.cd15.service.Cd15ScheduleResultService;
import com.zlt.aps.cd15.service.IHalfCdImportBakService;
import com.zlt.aps.cd90.api.domain.entity.Cd90ScheduleResult;
import com.zlt.aps.cd90.service.Cd90ScheduleResultService;
import com.zlt.aps.common.core.utils.BigDecimalUtil;
import com.zlt.aps.common.engine.constants.EngineConstants;
import com.zlt.aps.common.engine.domain.EngineConstructionInfo;
import com.zlt.aps.common.engine.enums.OpenMachineClassEnums;
import com.zlt.aps.gsq.api.domain.dto.GsqScheduleResultDto;
import com.zlt.aps.gsq.service.GsqScheduleResultService;
import com.zlt.aps.nc.api.domain.dto.NcParamsDto;
import com.zlt.aps.nc.api.domain.entity.NcCurlRoll;
import com.zlt.aps.nc.api.domain.entity.NcScheduleResult;
import com.zlt.aps.nc.entity.NcParams;
import com.zlt.aps.nc.service.NcCurlRollService;
import com.zlt.aps.nc.service.NcParamsService;
import com.zlt.aps.nc.service.NcScheduleResultService;
import com.zlt.aps.tq.api.domain.dto.TqScheduleResultDto;
import com.zlt.aps.tq.service.TqScheduleResultService;
import com.zlt.bill.common.controller.AbstractDocBizController;
import com.zlt.bill.common.service.IDocService;
import com.zlt.common.utils.ExcelReadUtils;
import com.zlt.common.utils.ImportExcelUtils;
import com.zlt.common.utils.PubUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.apache.poi.ss.usermodel.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：HalfCdImportBakController.java
 * 描    述：裁断线下计划导入导出 控制层类：....
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-05-29
 */
@Slf4j
@Api(tags = "裁断线下计划导入导出")
@RestController
@RequestMapping("/halfCdImportBak")
public class HalfCdImportBakController extends AbstractDocBizController<HalfCdImportBak> {

    @Autowired
    private IHalfCdImportBakService halfCdImportBakService;

    @Autowired
    private HalfCdImportBakEntityMapper entityMapper;

    @Autowired
    private IExportLogService iExportLogService;
    @Autowired
    private IImportLogService iImportLogService;
    @Autowired
    private IImportErrorLogService iImportErrorLogService;
    private List<String> notExistFieldNameList = Arrays.asList("serialVersionUID", "id", "searchValue", "createBy", "createTime", "updateBy", "updateTime", "remark", "isDelete", "params", "rowState");

    @Autowired
    private Cd15ParamsMapper cd15ParamsMapper;

    /**
     * 查询裁断线下计划导入导出列表
     */
    @RequiresPermissions("cd15:halfCdImportBak:list")
    @ApiOperation("查询列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody HalfCdImportBak queryVO) {
        return super.list(queryVO);
    }

    @Override
    protected String getOrderBy() {
        return "create_time desc";
    }

    /**
     * 保存
     */
    @Log(title = "ui.data.column.halfCdImportBak.modelName", businessType = BusinessType.INSERT_OR_UPDATE)
    @RequiresPermissions("cd15:halfCdImportBak:save")
    @ApiOperation("保存")
    @PostMapping("/save")
    @Override
    public AjaxResult save(@RequestBody HalfCdImportBak billVO) {
        return super.save(billVO);
    }

    /**
     * 删除
     */
    @Log(title = "ui.data.column.halfCdImportBak.modelName", businessType = BusinessType.DELETE)
    @RequiresPermissions("cd15:halfCdImportBak:remove")
    @ApiOperation("删除")
    @DeleteMapping("/remove")
    @Override
    public AjaxResult removeByIds(@RequestBody List<Long> ids) {
        return super.removeByIds(ids);
    }


    /**
     * 获取裁断线下计划导入导出详细信息
     */
    @RequiresPermissions("cd15:halfCdImportBak:query")
    @ApiOperation("获取详细信息")
    @GetMapping(value = "/{billId}")
    @Override
    public HalfCdImportBak getInfo(@PathVariable("billId") Long billId) {
        return super.getInfo(billId);
    }

    /**
     * 导出列表
     */
    @RequiresPermissions("cd15:halfCdImportBak:export")
    @Log(title = "裁断线下计划导入导出", businessType = BusinessType.EXPORT)
    @ApiOperation("导入数据")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody HalfCdImportBak queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return super.exportData(queryVO, fileName, response);
    }

    @Override
    protected List<HalfCdImportBak> listExportData(HalfCdImportBak obj) {
        QueryWrapper<HalfCdImportBak> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, obj);
        return entityMapper.selectList(wrapper);
    }

    @Override
    protected IDocService getDocService() {
        return null;
    }

    /**
     * 条件拼接
     *
     * @param queryWrapper
     * @param queryVO
     */
    @Override
    protected void builderCondition(QueryWrapper<HalfCdImportBak> queryWrapper, HalfCdImportBak queryVO) {
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("cx1")), "CX1", queryVO.getFieldValueByFieldName("cx1"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("cx2")), "CX2", queryVO.getFieldValueByFieldName("cx2"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("cx3")), "CX3", queryVO.getFieldValueByFieldName("cx3"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("cx4")), "CX4", queryVO.getFieldValueByFieldName("cx4"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("cx5")), "CX5", queryVO.getFieldValueByFieldName("cx5"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("cx6")), "CX6", queryVO.getFieldValueByFieldName("cx6"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("cx7")), "CX7", queryVO.getFieldValueByFieldName("cx7"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("cx8")), "CX8", queryVO.getFieldValueByFieldName("cx8"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("lb1")), "LB1", queryVO.getFieldValueByFieldName("lb1"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("lb2")), "LB2", queryVO.getFieldValueByFieldName("lb2"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("lb3")), "LB3", queryVO.getFieldValueByFieldName("lb3"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("lb4")), "LB4", queryVO.getFieldValueByFieldName("lb4"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("lb5")), "LB5", queryVO.getFieldValueByFieldName("lb5"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("lb6")), "LB6", queryVO.getFieldValueByFieldName("lb6"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("lb7")), "LB7", queryVO.getFieldValueByFieldName("lb7"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("lb8")), "LB8", queryVO.getFieldValueByFieldName("lb8"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("lb9")), "LB9", queryVO.getFieldValueByFieldName("lb9"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("lb10")), "LB10", queryVO.getFieldValueByFieldName("lb10"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("lb11")), "LB11", queryVO.getFieldValueByFieldName("lb11"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("lb12")), "LB12", queryVO.getFieldValueByFieldName("lb12"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("lb13")), "LB13", queryVO.getFieldValueByFieldName("lb13"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("lb14")), "LB14", queryVO.getFieldValueByFieldName("lb14"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("lb15")), "LB15", queryVO.getFieldValueByFieldName("lb15"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("lb16")), "LB16", queryVO.getFieldValueByFieldName("lb16"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("lb17")), "LB17", queryVO.getFieldValueByFieldName("lb17"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("lb18")), "LB18", queryVO.getFieldValueByFieldName("lb18"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("lb19")), "LB19", queryVO.getFieldValueByFieldName("lb19"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("lb20")), "LB20", queryVO.getFieldValueByFieldName("lb20"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("lb21")), "LB21", queryVO.getFieldValueByFieldName("lb21"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("lbt1")), "LBT1", queryVO.getFieldValueByFieldName("lbt1"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("lbt2")), "LBT2", queryVO.getFieldValueByFieldName("lbt2"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("lbt3")), "LBT3", queryVO.getFieldValueByFieldName("lbt3"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("lbt4")), "LBT4", queryVO.getFieldValueByFieldName("lbt4"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("lbt5")), "LBT5", queryVO.getFieldValueByFieldName("lbt5"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("lbt6")), "LBT6", queryVO.getFieldValueByFieldName("lbt6"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("lbt7")), "LBT7", queryVO.getFieldValueByFieldName("lbt7"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("lbt8")), "LBT8", queryVO.getFieldValueByFieldName("lbt8"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("lbt9")), "LBT9", queryVO.getFieldValueByFieldName("lbt9"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("lbt10")), "LBT10", queryVO.getFieldValueByFieldName("lbt10"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("lbt11")), "LBT11", queryVO.getFieldValueByFieldName("lbt11"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("lbt12")), "LBT12", queryVO.getFieldValueByFieldName("lbt12"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("lbt13")), "LBT13", queryVO.getFieldValueByFieldName("lbt13"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("lbt14")), "LBT14", queryVO.getFieldValueByFieldName("lbt14"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("lbt15")), "LBT15", queryVO.getFieldValueByFieldName("lbt15"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("lbt16")), "LBT16", queryVO.getFieldValueByFieldName("lbt16"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("lbt17")), "LBT17", queryVO.getFieldValueByFieldName("lbt17"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("lbt18")), "LBT18", queryVO.getFieldValueByFieldName("lbt18"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("lbt19")), "LBT19", queryVO.getFieldValueByFieldName("lbt19"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("lbt20")), "LBT20", queryVO.getFieldValueByFieldName("lbt20"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("lbt21")), "LBT21", queryVO.getFieldValueByFieldName("lbt21"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("nc1")), "NC1", queryVO.getFieldValueByFieldName("nc1"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("nc2")), "NC2", queryVO.getFieldValueByFieldName("nc2"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("nc3")), "NC3", queryVO.getFieldValueByFieldName("nc3"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("nc4")), "NC4", queryVO.getFieldValueByFieldName("nc4"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("nc5")), "NC5", queryVO.getFieldValueByFieldName("nc5"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("nc6")), "NC6", queryVO.getFieldValueByFieldName("nc6"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("nc7")), "NC7", queryVO.getFieldValueByFieldName("nc7"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("nc8")), "NC8", queryVO.getFieldValueByFieldName("nc8"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("nc9")), "NC9", queryVO.getFieldValueByFieldName("nc9"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("nc10")), "NC10", queryVO.getFieldValueByFieldName("nc10"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("nc11")), "NC11", queryVO.getFieldValueByFieldName("nc11"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("nc12")), "NC12", queryVO.getFieldValueByFieldName("nc12"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("nc13")), "NC13", queryVO.getFieldValueByFieldName("nc13"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("nc14")), "NC14", queryVO.getFieldValueByFieldName("nc14"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("nc15")), "NC15", queryVO.getFieldValueByFieldName("nc15"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("nc16")), "NC16", queryVO.getFieldValueByFieldName("nc16"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("nc17")), "NC17", queryVO.getFieldValueByFieldName("nc17"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("nc18")), "NC18", queryVO.getFieldValueByFieldName("nc18"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("gd1")), "GD1", queryVO.getFieldValueByFieldName("gd1"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("gd2")), "GD2", queryVO.getFieldValueByFieldName("gd2"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("gd3")), "GD3", queryVO.getFieldValueByFieldName("gd3"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("gd4")), "GD4", queryVO.getFieldValueByFieldName("gd4"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("gd5")), "GD5", queryVO.getFieldValueByFieldName("gd5"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("gd6")), "GD6", queryVO.getFieldValueByFieldName("gd6"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("gd7")), "GD7", queryVO.getFieldValueByFieldName("gd7"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("gd8")), "GD8", queryVO.getFieldValueByFieldName("gd8"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("gd9")), "GD9", queryVO.getFieldValueByFieldName("gd9"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("gd10")), "GD10", queryVO.getFieldValueByFieldName("gd10"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("gd11")), "GD11", queryVO.getFieldValueByFieldName("gd11"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("gd12")), "GD12", queryVO.getFieldValueByFieldName("gd12"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("gd13")), "GD13", queryVO.getFieldValueByFieldName("gd13"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("gd14")), "GD14", queryVO.getFieldValueByFieldName("gd14"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("gd15")), "GD15", queryVO.getFieldValueByFieldName("gd15"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("gd16")), "GD16", queryVO.getFieldValueByFieldName("gd16"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("gd17")), "GD17", queryVO.getFieldValueByFieldName("gd17"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("gd18")), "GD18", queryVO.getFieldValueByFieldName("gd18"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("gd19")), "GD19", queryVO.getFieldValueByFieldName("gd19"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("gd20")), "GD20", queryVO.getFieldValueByFieldName("gd20"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("gd21")), "GD21", queryVO.getFieldValueByFieldName("gd21"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("gd22")), "GD22", queryVO.getFieldValueByFieldName("gd22"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("gdt1")), "GDT1", queryVO.getFieldValueByFieldName("gdt1"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("gdt2")), "GDT2", queryVO.getFieldValueByFieldName("gdt2"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("gdt3")), "GDT3", queryVO.getFieldValueByFieldName("gdt3"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("gdt4")), "GDT4", queryVO.getFieldValueByFieldName("gdt4"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("gdt5")), "GDT5", queryVO.getFieldValueByFieldName("gdt5"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("gdt6")), "GDT6", queryVO.getFieldValueByFieldName("gdt6"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("gdt7")), "GDT7", queryVO.getFieldValueByFieldName("gdt7"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("gdt8")), "GDT8", queryVO.getFieldValueByFieldName("gdt8"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("gdt9")), "GDT9", queryVO.getFieldValueByFieldName("gdt9"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("gdt10")), "GDT10", queryVO.getFieldValueByFieldName("gdt10"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("gdt11")), "GDT11", queryVO.getFieldValueByFieldName("gdt11"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("gdt12")), "GDT12", queryVO.getFieldValueByFieldName("gdt12"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("gdt13")), "GDT13", queryVO.getFieldValueByFieldName("gdt13"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("gdt14")), "GDT14", queryVO.getFieldValueByFieldName("gdt14"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("gdt15")), "GDT15", queryVO.getFieldValueByFieldName("gdt15"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("gdt16")), "GDT16", queryVO.getFieldValueByFieldName("gdt16"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("gdt17")), "GDT17", queryVO.getFieldValueByFieldName("gdt17"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("gdt18")), "GDT18", queryVO.getFieldValueByFieldName("gdt18"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("gdt19")), "GDT19", queryVO.getFieldValueByFieldName("gdt19"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("gdt20")), "GDT20", queryVO.getFieldValueByFieldName("gdt20"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("gdt21")), "GDT21", queryVO.getFieldValueByFieldName("gdt21"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("gdt22")), "GDT22", queryVO.getFieldValueByFieldName("gdt22"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("zk1")), "ZK1", queryVO.getFieldValueByFieldName("zk1"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("zk2")), "ZK2", queryVO.getFieldValueByFieldName("zk2"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("zk3")), "ZK3", queryVO.getFieldValueByFieldName("zk3"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("zk4")), "ZK4", queryVO.getFieldValueByFieldName("zk4"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("zk5")), "ZK5", queryVO.getFieldValueByFieldName("zk5"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("zk6")), "ZK6", queryVO.getFieldValueByFieldName("zk6"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("zk7")), "ZK7", queryVO.getFieldValueByFieldName("zk7"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("zk8")), "ZK8", queryVO.getFieldValueByFieldName("zk8"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("zk9")), "ZK9", queryVO.getFieldValueByFieldName("zk9"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("zk10")), "ZK10", queryVO.getFieldValueByFieldName("zk10"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("zk11")), "ZK11", queryVO.getFieldValueByFieldName("zk11"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("zk12")), "ZK12", queryVO.getFieldValueByFieldName("zk12"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("zk13")), "ZK13", queryVO.getFieldValueByFieldName("zk13"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("zk14")), "ZK14", queryVO.getFieldValueByFieldName("zk14"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("gsq1")), "GSQ1", queryVO.getFieldValueByFieldName("gsq1"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("gsq2")), "GSQ2", queryVO.getFieldValueByFieldName("gsq2"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("gsq3")), "GSQ3", queryVO.getFieldValueByFieldName("gsq3"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("gsq4")), "GSQ4", queryVO.getFieldValueByFieldName("gsq4"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("gsq5")), "GSQ5", queryVO.getFieldValueByFieldName("gsq5"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("gsq6")), "GSQ6", queryVO.getFieldValueByFieldName("gsq6"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("gsq7")), "GSQ7", queryVO.getFieldValueByFieldName("gsq7"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("gsq8")), "GSQ8", queryVO.getFieldValueByFieldName("gsq8"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("gsq9")), "GSQ9", queryVO.getFieldValueByFieldName("gsq9"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("gsq10")), "GSQ10", queryVO.getFieldValueByFieldName("gsq10"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("gsq11")), "GSQ11", queryVO.getFieldValueByFieldName("gsq11"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("gsq12")), "GSQ12", queryVO.getFieldValueByFieldName("gsq12"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("gsq13")), "GSQ13", queryVO.getFieldValueByFieldName("gsq13"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("gsq14")), "GSQ14", queryVO.getFieldValueByFieldName("gsq14"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("gsq15")), "GSQ15", queryVO.getFieldValueByFieldName("gsq15"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("tq1")), "TQ1", queryVO.getFieldValueByFieldName("tq1"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("tq2")), "TQ2", queryVO.getFieldValueByFieldName("tq2"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("tq3")), "TQ3", queryVO.getFieldValueByFieldName("tq3"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("tq4")), "TQ4", queryVO.getFieldValueByFieldName("tq4"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("tq5")), "TQ5", queryVO.getFieldValueByFieldName("tq5"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("tq6")), "TQ6", queryVO.getFieldValueByFieldName("tq6"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("tq7")), "TQ7", queryVO.getFieldValueByFieldName("tq7"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("tq8")), "TQ8", queryVO.getFieldValueByFieldName("tq8"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("tq9")), "TQ9", queryVO.getFieldValueByFieldName("tq9"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("tq10")), "TQ10", queryVO.getFieldValueByFieldName("tq10"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("tq11")), "TQ11", queryVO.getFieldValueByFieldName("tq11"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("tq12")), "TQ12", queryVO.getFieldValueByFieldName("tq12"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("tq13")), "TQ13", queryVO.getFieldValueByFieldName("tq13"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("tq14")), "TQ14", queryVO.getFieldValueByFieldName("tq14"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("tq15")), "TQ15", queryVO.getFieldValueByFieldName("tq15"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("tq16")), "TQ16", queryVO.getFieldValueByFieldName("tq16"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("tq17")), "TQ17", queryVO.getFieldValueByFieldName("tq17"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("tq18")), "TQ18", queryVO.getFieldValueByFieldName("tq18"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("tq19")), "TQ19", queryVO.getFieldValueByFieldName("tq19"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("tq20")), "TQ20", queryVO.getFieldValueByFieldName("tq20"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("tq21")), "TQ21", queryVO.getFieldValueByFieldName("tq21"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("tq22")), "TQ22", queryVO.getFieldValueByFieldName("tq22"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("tq23")), "TQ23", queryVO.getFieldValueByFieldName("tq23"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("tq24")), "TQ24", queryVO.getFieldValueByFieldName("tq24"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("tq25")), "TQ25", queryVO.getFieldValueByFieldName("tq25"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("tq26")), "TQ26", queryVO.getFieldValueByFieldName("tq26"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("tq27")), "TQ27", queryVO.getFieldValueByFieldName("tq27"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("tq28")), "TQ28", queryVO.getFieldValueByFieldName("tq28"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("tq29")), "TQ29", queryVO.getFieldValueByFieldName("tq29"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("tq30")), "TQ30", queryVO.getFieldValueByFieldName("tq30"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("tq31")), "TQ31", queryVO.getFieldValueByFieldName("tq31"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("scheduleDate")), "SCHEDULE_DATE", queryVO.getFieldValueByFieldName("scheduleDate"));
    }


    @Override
    protected String getTypeCode() {
        return "CD150099";
    }

    /**
     * 根据集合导入线下计划导入数据
     *
     * @param importContext 导入上下文
     * @param updateSupport 已存在记录是否更新
     * @return 结果
     */
    @Log(title = "ui.data.column.HalfCdImportBak.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    @Override
    public AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        Date beginTime = DateUtils.getNowDate();
        ImportLog importLog = ImportExcelUtils.getImportLogAndUploadFile(importContext.getFileBytes(), importContext.getImportFilePath(), importContext.getProcedureCode(), importContext.getFunctionName(), importContext.getOriFileName(), 1);
        importLog = this.iImportLogService.add(importLog);
        InputStream is = new ByteArrayInputStream(importContext.getFileBytes());
        List<HalfCdImportBak> list = this.importExcel("", is);
        AjaxResult ajaxResult = halfCdImportBakService.importData(list);
//        AjaxResult ajaxResult = AjaxResult.success();
        Date endTime = DateUtils.getNowDate();
        importLog.setRowCount(list.size());
        importLog.setBeginTime(beginTime);
        importLog.setEndTime(endTime);
        importLog.setSpendTime(DateUtils.getDiffTime(endTime, beginTime));
        ImportExcelUtils.updateImportLogAndFormatMsg(importLog, ajaxResult, this.iImportLogService);
        ImportExcelUtils.saveImportErrorLogs(ajaxResult, this.iImportErrorLogService);
        return ajaxResult;
    }

    public List<HalfCdImportBak> importExcel(String sheetName, InputStream is) throws Exception {
        // 设置更低的压缩率阈值（例如 0.005），避免Zip bomb detected! 报错
        ZipSecureFile.setMinInflateRatio(0.005);

        Workbook wb = WorkbookFactory.create(is);
        List<HalfCdImportBak> list = new LinkedList<>();
        Sheet sheet = null;
        if (StringUtils.isNotEmpty(sheetName)) {
            sheet = wb.getSheet(sheetName);
        } else {
            sheet = wb.getSheetAt(0);
        }

        if (sheet == null) {
            throw new IOException(I18nUtil.getMessage("common.error.util.file.sheet.noexist"));
        } else {
            List<Field> classField = getClassField(HalfCdImportBak.class);
            classField = classField.stream().filter(item -> !notExistFieldNameList.contains(item.getName())).collect(Collectors.toList());

            int scheduleDateRowNum = 0;
            for (int i = 0; i < sheet.getLastRowNum(); i++) {
                Object firstDateCellValue = getCellValue(sheet.getRow(i), 0);
                if (firstDateCellValue instanceof Date) {
                    scheduleDateRowNum = i;
                    break;
                }
            }

            Date scheduleDateCellValue = (Date) getCellValue(sheet.getRow(scheduleDateRowNum), 2);
//            scheduleDateCellValue = DateUtils.addYears(scheduleDateCellValue, -1);
            /*while (true) {
                Object cellValue = getCellValue(sheet.getRow(scheduleDataRowNum), 2);
                if (nowDateStr.equals(cellValue)) {
                    break;
                } else {
                    scheduleDataRowNum += 3508;
                }
            }*/
            LambdaQueryWrapper<Cd15Params> paramsWrapper = new LambdaQueryWrapper<>();
            paramsWrapper.eq(Cd15Params::getParamCode, "EXCEL_DATA_SUM_ROW_NUM");
            Cd15Params dataRowNumParams = cd15ParamsMapper.selectOne(paramsWrapper);
            int dataRowNum = Integer.parseInt(dataRowNumParams.getParamValue());
            int dataStartRowNum = scheduleDateRowNum + 4;
            int lastRowNum = dataStartRowNum + dataRowNum;

            for (int rowNum = dataStartRowNum; rowNum <= lastRowNum; ++rowNum) {
                Row row = sheet.getRow(rowNum);
                HalfCdImportBak halfCdImportBak = new HalfCdImportBak();
                for (int i = 0; i < classField.size(); i++) {
                    Field field = classField.get(i);
                    String fieldName = field.getName();
                    Object cellValue = getCellValue(row, i);
                    if (i == 0 && ObjectUtils.isEmpty(cellValue)) {
                        break;
                    }
                    if (StringUtils.isNotBlank(cellValue.toString()) && !"scheduleDate".equals(fieldName)) {
                        if (field.getType() == Integer.class) {
                            cellValue = Integer.parseInt(cellValue.toString());
                        } else if (field.getType() == Double.class) {
                            try {
                                cellValue = Double.parseDouble(cellValue.toString());
                            } catch (NumberFormatException e) {
                                log.error("数值转换异常,{}", e.getMessage());
                                continue;
                            }
                        }
                        if (field.getType() == cellValue.getClass()) {
                            ReflectUtils.setFieldValue(halfCdImportBak, fieldName, cellValue);
                        }
                    }
                }
                ReflectUtils.setFieldValue(halfCdImportBak, "scheduleDate", scheduleDateCellValue);
                list.add(halfCdImportBak);
            }
            return list;
        }
    }

    public Object getCellValue(Row row, int column) {
        if (row == null) {
            return row;
        } else {
            Object val = "";

            try {
                Cell cell = row.getCell(column);
                if (StringUtils.isNotNull(cell)) {
                    if (cell.getCellType() != CellType.NUMERIC && cell.getCellType() != CellType.FORMULA) {
                        if (cell.getCellType() == CellType.STRING) {
                            val = cell.getStringCellValue();
                        } else if (cell.getCellType() == CellType.BOOLEAN) {
                            val = cell.getBooleanCellValue();
                        } else if (cell.getCellType() == CellType.ERROR) {
                            val = cell.getErrorCellValue();
                        }
                    } else {
                        val = cell.getNumericCellValue();
                        if (DateUtil.isCellDateFormatted(cell)) {
                            val = DateUtils.getJavaDate((Double) val, TimeZone.getDefault());
                        } else if ((Double) val % 1.0 != 0.0) {
                            val = new BigDecimal(val.toString());
                        } else {
                            val = (new DecimalFormat("0")).format(val);
                        }
                    }
                }

                return val;
            } catch (Exception var5) {
                return val;
            }
        }
    }

    public List<Field> getClassField(Class<? super HalfCdImportBak> tClass) {
        List<Field> tempFields = new ArrayList<>();

        while (tClass != null) {
            tempFields.addAll(Arrays.asList(tClass.getDeclaredFields()));
            tClass = tClass.getSuperclass();
            if (StringUtils.equals(tClass.getSimpleName(), BaseEntity.class.getSimpleName())) {
                break;
            }
        }
        return tempFields;
    }

    /**
     * 将排程数据导出到文件
     *
     * @param importContext 导入上下文
     * @return 结果
     */
    @Log(title = "ui.data.column.HalfCdImportBak.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("将排程数据导出到文件")
    @PostMapping("/importExcelToListAndExport")
    public byte[] importExcelToListAndExport(@RequestBody ImportContext importContext, HttpServletResponse response) throws Exception {
        Date beginTime = DateUtils.getNowDate();
        InputStream is = new ByteArrayInputStream(importContext.getFileBytes());
        String fileName = I18nUtil.getMessage("ui.data.column.HalfCdImportBak.modelName");
        Workbook workbook = this.importExcelToListAndExport("", is, response, fileName);
        byte[] resultBytes = ExcelReadUtils.writeExcel(workbook);
        Date endTime = DateUtils.getNowDate();
        ExportLog exportLog = new ExportLog();
        exportLog.setProcedureCode("0");
        String uri = ServletUtils.getRequest().getRequestURI();
        exportLog.setFunctionCode(uri.split("/")[1]);
        exportLog.setFunctionName(fileName);
        exportLog.setFileName(fileName + ".xlsx");
        exportLog.setBeginTime(beginTime);
        exportLog.setEndTime(endTime);
        exportLog.setSpendTime(DateUtils.getDiffTime(endTime, beginTime));
        this.iExportLogService.add(exportLog);
        return resultBytes;
    }

    private final List<String> tqMachineNameList = Arrays.asList("1号", "2号", "3号", "4号", "5号", "7号", "8号", "9号", "12号", "13号");

    public Workbook importExcelToListAndExport(String sheetName, InputStream is, HttpServletResponse response, String fileName) throws Exception {
        // 设置更低的压缩率阈值（例如 0.005），避免Zip bomb detected! 报错
        ZipSecureFile.setMinInflateRatio(0.005);

        Workbook wb = WorkbookFactory.create(is);
        List<HalfCdImportBak> list = new ArrayList<>();
        List<HalfCdImportBak> nextDayList = new ArrayList<>();
        Sheet sheet = null;
        if (StringUtils.isNotEmpty(sheetName)) {
            sheet = wb.getSheet(sheetName);
        } else {
            sheet = wb.getSheetAt(0);
        }

        if (sheet == null) {
            throw new IOException(I18nUtil.getMessage("common.error.util.file.sheet.noexist"));
        } else {

            LambdaQueryWrapper<Cd15Params> paramsWrapper = new LambdaQueryWrapper<>();
            paramsWrapper.eq(Cd15Params::getParamCode, "EXCEL_DATA_SUM_ROW_NUM");
            Cd15Params dataRowNumParams = cd15ParamsMapper.selectOne(paramsWrapper);
            int dataRowNum = Integer.parseInt(dataRowNumParams.getParamValue());

            int scheduleDateRowNum = 0;
            for (int i = 0; i < sheet.getLastRowNum(); i++) {
                Object firstDateCellValue = getCellValue(sheet.getRow(i), 0);
                if (firstDateCellValue instanceof Date) {
                    scheduleDateRowNum = i;
                    break;
                }
            }
            int dataStartRowNum = 4 + scheduleDateRowNum;
            int lastRowNum = dataRowNum + 2 + scheduleDateRowNum;

            int nextDayDateRowNum = dataRowNum + 8 + scheduleDateRowNum;
            // 查询成型计划
            String scheduleDateStr = DateFormatUtils.format((Date) getCellValue(sheet.getRow(nextDayDateRowNum), 2), "yyyy-MM-dd");
            List<HalfCdImportBakExportVo> cxPlanQtyList = entityMapper.selectCxScheduleResult(scheduleDateStr);
            Map<String, Integer> cxPlanQtyMap = new HashMap<>(16);
            if (CollectionUtils.isNotEmpty(cxPlanQtyList)) {
                cxPlanQtyMap = cxPlanQtyList.stream().collect(Collectors.toMap(HalfCdImportBakExportVo::getCode, HalfCdImportBakExportVo::getCxPlanQty));
            }
            // 先遍历所有的规格代码，重算计划用量，再写值到对应的计划量栏位
            for (int rowNum = dataStartRowNum; rowNum <= lastRowNum; ++rowNum) {
                Row row = sheet.getRow(rowNum);
                String cx3CellValue = getCellValue(row, 2).toString();
                if (cxPlanQtyMap.containsKey(cx3CellValue)) {
                    Integer cxTotalPlan = cxPlanQtyMap.get(cx3CellValue);
                    row.getCell(6).setBlank();
                    row.getCell(6).setCellValue(cxTotalPlan);
                }
            }
            for (int rowNum = dataStartRowNum + nextDayDateRowNum; rowNum <= lastRowNum + nextDayDateRowNum; ++rowNum) {
                Row row = sheet.getRow(rowNum);
                String cx3CellValue = getCellValue(row, 2).toString();
                if (cxPlanQtyMap.containsKey(cx3CellValue)) {
                    Integer cxTotalPlan = cxPlanQtyMap.get(cx3CellValue);
                    row.getCell(6).setBlank();
                    row.getCell(6).setCellValue(cxTotalPlan);
                }
            }
            // 重算计划用量公式
            FormulaEvaluator evaluator = wb.getCreationHelper().createFormulaEvaluator();
            evaluator.evaluateAll();

            // 查询胎圈的计划，先写入excel再更新公式，因为钢丝圈的计划用量是用胎圈的计划计算的
            List<HalfCdImportBakExportVo> tqScheduleResultList = entityMapper.selectTqScheduleResult(scheduleDateStr);
            Map<String, HalfCdImportBakExportVo> tqPlanQtyMap = new HashMap<>(16);
            if (CollectionUtils.isNotEmpty(tqScheduleResultList)) {
                tqPlanQtyMap = tqScheduleResultList.stream().collect(Collectors.toMap(HalfCdImportBakExportVo::getCode, Function.identity()));
            }
            int nightClassIndex = OpenMachineClassEnums.CLASS_TWO.getClassIndex();
            for (int rowNum = dataStartRowNum; rowNum <= lastRowNum; ++rowNum) {
                Row row = sheet.getRow(rowNum);
                // 胎圈
                String tq1 = getCellValue(row, 142).toString();
                Object tq5 = getCellValue(row, 146);
                // 先清空胎圈机台所有计划量
                for (int i = 0; i < tqMachineNameList.size(); i++) {
                    int cellNum = i + 160;
                    row.getCell(cellNum).setBlank();
                }

                if (StringUtils.isNotBlank(tq1) && tq5 != null
                        && StringUtils.isNotBlank(tq5.toString()) && Double.parseDouble(tq5.toString()) > 0) {
                    for (int i = 0; i < tqMachineNameList.size(); i++) {
                        String machineName = tqMachineNameList.get(i);
                        String code = tq1 + "-" + machineName;
                        String fieldCode = "tq" + i;
                        if (tqPlanQtyMap.containsKey(code)) {
                            HalfCdImportBakExportVo exportVo = tqPlanQtyMap.get(code);
                            // 计划
                            Double dayPlanQtyRollNum = ReflectUtils.getFieldValue(exportVo, "dayPlanQty");
                            if (dayPlanQtyRollNum == null) {
                                continue;
                            }
                            CdMachineExportEnums machineExportEnums = CdMachineExportEnums.getInstance(fieldCode + "-" + nightClassIndex);
                            int cellNum = i + 160;
                            if (machineExportEnums != null) {
                                Cell cell = row.getCell(cellNum);
                                if (cell != null) {
                                    cell.setCellValue(dayPlanQtyRollNum);
                                }
                            }
                        }
                    }
                }
            }

            int dayClassIndex = OpenMachineClassEnums.CLASS_THREE.getClassIndex();
            int nextNightClassIndex = OpenMachineClassEnums.CLASS_TWO.getClassIndex();
            for (int rowNum = dataStartRowNum + nextDayDateRowNum; rowNum <= lastRowNum + nextDayDateRowNum; ++rowNum) {
                Row row = sheet.getRow(rowNum);
                // 胎圈
                String tq1 = getCellValue(row, 142).toString();
                Object tq5 = getCellValue(row, 146);
                // 先清空胎圈机台所有计划量
                for (int i = 0; i < tqMachineNameList.size(); i++) {
                    int cellNum1 = i + 149;
                    int cellNum2 = i + 160;
                    row.getCell(cellNum1).setBlank();
                    row.getCell(cellNum2).setBlank();
                }
                if (StringUtils.isNotBlank(tq1) && tq5 != null
                        && StringUtils.isNotBlank(tq5.toString()) && Double.parseDouble(tq5.toString()) > 0) {
                    for (int i = 0; i < tqMachineNameList.size(); i++) {
                        String machineName = tqMachineNameList.get(i);
                        String code = tq1 + "-" + machineName;
                        String fieldCode = "tq" + i;
                        if (tqPlanQtyMap.containsKey(code)) {
                            HalfCdImportBakExportVo exportVo = tqPlanQtyMap.get(code);
                            // 早班计划
                            Double nightPlanQtyRollNum = ReflectUtils.getFieldValue(exportVo, "nightPlanQty");
                            CdMachineExportEnums nightMachineExportEnums = CdMachineExportEnums.getInstance(fieldCode + "-" + dayClassIndex);
                            if (nightMachineExportEnums != null) {
                                int cellNum = i + 149;
                                row.getCell(cellNum).setCellValue(nightPlanQtyRollNum);
                            }
                            // 次日夜班计划
                            Double nextDayPlanQtyRollNum = ReflectUtils.getFieldValue(exportVo, "nextDayPlanQty");
                            if (nextDayPlanQtyRollNum == null) {
                                continue;
                            }
                            CdMachineExportEnums nextDayMachineExportEnums = CdMachineExportEnums.getInstance(fieldCode + "-" + nextNightClassIndex);
                            if (nextDayMachineExportEnums != null) {
                                int cellNum = i + 160;
                                Cell cell = row.getCell(cellNum);
                                if (cell != null) {
                                    cell.setCellValue(nextDayPlanQtyRollNum);
                                }
                            }
                        }
                    }
                }
            }
            // 重算钢丝圈的计划用量
            // 清除所有公式计算缓存结果
            evaluator.clearAllCachedResultValues();
            evaluator.evaluateAll();

            List<Field> classField = getClassField(HalfCdImportBak.class);
            classField = classField.stream().filter(item -> !notExistFieldNameList.contains(item.getName())).collect(Collectors.toList());

            List<String> setFieldNameNight = Arrays.asList(
                    "lb14", "lb15", "lb16", "lb17", "lb18",
                    "lbt14", "lbt15", "lbt16", "lbt17", "lbt18",
                    "nc10", "nc13",
                    "gd8", "gd15", "gd16", "gd17", "gd18",
                    "gdt8", "gdt15", "gdt16", "gdt17", "gdt18",
                    "gsq10", "gsq11", "gsq12",
                    "tq19", "tq20", "tq21", "tq22", "tq23", "tq24", "tq25", "tq26", "tq27", "tq28"
            );
            List<String> setFieldNameDay = new ArrayList<>(Arrays.asList(
                    "lb8", "lb9", "lb10", "lb11", "lb12",
                    "lbt8", "lbt9", "lbt10", "lbt11", "lbt12",
                    "nc10", "nc11", "nc13",
                    "gd8", "gd9", "gd10", "gd11", "gd12",
                    "gdt8", "gdt9", "gdt10", "gdt11", "gdt12",
                    "gsq6", "gsq7", "gsq8",
                    "tq7", "tq8", "tq9", "tq10", "tq11", "tq12", "tq13", "tq14", "tq15", "tq16", "tq17"
            ));
            setFieldNameDay.addAll(setFieldNameNight);

            Object scheduleDateCellValue = getCellValue(sheet.getRow(scheduleDateRowNum), 2);
            for (int rowNum = dataStartRowNum; rowNum <= lastRowNum; ++rowNum) {
                Row row = sheet.getRow(rowNum);
                HalfCdImportBak halfCdImportBak = new HalfCdImportBak();
                for (int i = 0; i < classField.size(); i++) {
                    Field field = classField.get(i);
                    String fieldName = field.getName();
                    // 胎圈计划上面已经写入了，这里直接写入，后面就不写了
                    if (setFieldNameNight.contains(fieldName) && !fieldName.contains("tq")) {
                        continue;
                    }
                    Object cellValue = getCellValue(row, i);
                    if (StringUtils.isNotBlank(cellValue.toString()) && !"scheduleDate".equals(fieldName)) {
                        if (field.getType() == Integer.class) {
                            try {
                                cellValue = Integer.parseInt(cellValue.toString());
                            } catch (NumberFormatException e) {
                                log.error("数值转换异常：{}", e.getMessage());
                                continue;
                            }
                        } else if (field.getType() == Double.class) {
                            try {
                                cellValue = Double.parseDouble(cellValue.toString());
                            } catch (NumberFormatException e) {
                                log.error("数值转换异常：{}", e.getMessage());
                                continue;
                            }
                        }
                        if (field.getType() == cellValue.getClass()) {
                            ReflectUtils.setFieldValue(halfCdImportBak, fieldName, cellValue);
                        }
                    }
                }
                ReflectUtils.setFieldValue(halfCdImportBak, "scheduleDate", scheduleDateCellValue);
                list.add(halfCdImportBak);
            }

            int nextDayDataStartRowNum = dataRowNum + 12 + scheduleDateRowNum;
            int nextDayLastRowNum = dataRowNum * 2 + 10 + scheduleDateRowNum;
            scheduleDateCellValue = getCellValue(sheet.getRow(nextDayDateRowNum), 2);
            for (int rowNum = nextDayDataStartRowNum; rowNum <= nextDayLastRowNum; ++rowNum) {
                Row row = sheet.getRow(rowNum);
                HalfCdImportBak halfCdImportBak = new HalfCdImportBak();
                for (int i = 0; i < classField.size(); i++) {
                    Field field = classField.get(i);
                    String fieldName = field.getName();
                    // 胎圈计划上面已经写入了，这里直接写入，后面就不写了
                    if (setFieldNameDay.contains(fieldName) && !fieldName.contains("tq")) {
                        continue;
                    }
                    Object cellValue = getCellValue(row, i);
                    if (StringUtils.isNotBlank(cellValue.toString()) && !"scheduleDate".equals(fieldName)) {
                        if (field.getType() == Integer.class) {
                            try {
                                cellValue = Integer.parseInt(cellValue.toString());
                            } catch (NumberFormatException e) {
                                log.error("数值转换异常：{}", e.getMessage());
                                continue;
                            }
                        } else if (field.getType() == Double.class) {
                            try {
                                cellValue = Double.parseDouble(cellValue.toString());
                            } catch (NumberFormatException e) {
                                log.error("数值转换异常：{}", e.getMessage());
                                continue;
                            }
                        }
                        if (field.getType() == cellValue.getClass()) {
                            ReflectUtils.setFieldValue(halfCdImportBak, fieldName, cellValue);
                        }
                    }
                }
                ReflectUtils.setFieldValue(halfCdImportBak, "scheduleDate", scheduleDateCellValue);
                nextDayList.add(halfCdImportBak);
            }

            Date scheduleDate = null;
            if (CollectionUtils.isNotEmpty(nextDayList)) {
                scheduleDate = nextDayList.get(0).getScheduleDate();
            }
            halfCdImportBakService.exportDataToList(list, nextDayList, scheduleDate);
//            nextDayList = HalfCdImportBakService.exportDataToList(nextDayList, scheduleDate, OpenMachineClassEnums.CLASS_THREE.getClassIndex());

            for (int i = 0; i < list.size(); i++) {
                HalfCdImportBak halfCdImportBak = list.get(i);
                Row row = sheet.getRow(i + dataStartRowNum);
                for (int j = 0; j < classField.size(); j++) {
                    Field field = classField.get(j);
                    String fieldName = field.getName();
                    if (setFieldNameNight.contains(fieldName)) {
                        Object fieldValue = ReflectUtils.getFieldValue(halfCdImportBak, fieldName);
                        if (Objects.nonNull(fieldValue)) {
                            double cellValue = Double.parseDouble(fieldValue.toString());
                            Cell cell = row.getCell(j);
                            // 原有单元格有值，重新写入值可能未覆盖，这里需要清空原有值
                            cell.setBlank();
                            if (cellValue > 0) {
                                cell.setCellValue(cellValue);
                            }
                        } else {
                            Cell cell = row.getCell(j);
                            cell.setBlank();
                        }
                    }
                }
            }
            for (int i = 0; i < nextDayList.size(); i++) {
                HalfCdImportBak halfCdImportBak = nextDayList.get(i);
                Row row = sheet.getRow(i + nextDayDataStartRowNum);
                for (int j = 0; j < classField.size(); j++) {
                    Field field = classField.get(j);
                    String fieldName = field.getName();
                    if (setFieldNameDay.contains(fieldName)) {
                        Object fieldValue = ReflectUtils.getFieldValue(halfCdImportBak, fieldName);
                        if (Objects.nonNull(fieldValue)) {
                            double cellValue = Double.parseDouble(fieldValue.toString());
                            Cell cell = row.getCell(j);
                            // 原有单元格有值，重新写入值可能未覆盖，这里需要清空原有值
                            cell.setBlank();
                            if (cellValue > 0) {
                                cell.setCellValue(cellValue);
                            }
                        } else {
                            Cell cell = row.getCell(j);
                            cell.setBlank();
                        }
                    }
                }
            }

            // 重算求和公式
            // 清除所有公式计算缓存结果
            evaluator.clearAllCachedResultValues();
            evaluator.evaluateAll();
        }
        return wb;
    }

    @Autowired
    private NcParamsService ncParamsService;

    @Resource
    private NcCurlRollService ncCurlRollService;

    @Autowired
    private Cd15ScheduleResultService cd15ScheduleResultService;

    @Autowired
    private Cd90ScheduleResultService cd90ScheduleResultService;

    @Autowired
    private NcScheduleResultService ncScheduleResultService;

    @Autowired
    private TqScheduleResultService tqScheduleResultService;

    @Autowired
    private GsqScheduleResultService gsqScheduleResultService;

    /**
     * 导入线下模板，并覆盖原有排程数据
     *
     * @param importContext sheet名称
     * @return 结果
     * @throws Exception 异常
     */
    @Log(title = "ui.data.column.halfCdImportBak.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入线下模板调整")
    @PostMapping("/import4OfflineTemplate")
    public AjaxResult import4OfflineTemplate(@RequestBody ImportContext importContext, HttpServletResponse response) throws Exception {
        Date beginTime = DateUtils.getNowDate();
        ImportLog importLog = ImportExcelUtils.getImportLogAndUploadFile(importContext.getFileBytes(), importContext.getImportFilePath(), importContext.getProcedureCode(), importContext.getFunctionName(), importContext.getOriFileName(), 1);
        importLog = this.iImportLogService.add(importLog);
        InputStream is = new ByteArrayInputStream(importContext.getFileBytes());
        String sheetName = "";
        // 设置更低的压缩率阈值（例如 0.005），避免Zip bomb detected! 报错
        ZipSecureFile.setMinInflateRatio(0.005);

        Workbook wb = WorkbookFactory.create(is);
        Sheet sheet = null;
        if (StringUtils.isNotEmpty(sheetName)) {
            sheet = wb.getSheet(sheetName);
        } else {
            sheet = wb.getSheetAt(0);
        }

        if (sheet == null) {
            throw new IOException(I18nUtil.getMessage("common.error.util.file.sheet.noexist"));
        } else {
            Map<Integer, HalfCdImportBak> importBakHashMap = new HashMap<>(16);

            int scheduleDateRowNum = 0;
            for (int i = 0; i < sheet.getLastRowNum(); i++) {
                Object firstDateCellValue = getCellValue(sheet.getRow(i), 0);
                if (firstDateCellValue instanceof Date) {
                    scheduleDateRowNum = i;
                    break;
                }
            }


            LambdaQueryWrapper<Cd15Params> paramsWrapper = new LambdaQueryWrapper<>();
            paramsWrapper.eq(Cd15Params::getParamCode, "EXCEL_DATA_SUM_ROW_NUM");
            Cd15Params dataRowNumParams = cd15ParamsMapper.selectOne(paramsWrapper);
            int dataRowNum = Integer.parseInt(dataRowNumParams.getParamValue());

            int dataStartRowNum = 4 + scheduleDateRowNum;
            int lastRowNum = dataRowNum + 2 + scheduleDateRowNum;

            int nextDayDateRowNum = dataRowNum + 8 + scheduleDateRowNum;

            List<Field> classField = getClassField(HalfCdImportBak.class);
            classField = classField.stream().filter(item -> !notExistFieldNameList.contains(item.getName())).collect(Collectors.toList());
            int nightClassIndex = OpenMachineClassEnums.CLASS_TWO.getClassIndex();
            List<String> nightFieldNameList = Arrays.stream(CdMachineExportEnums.values()).filter(item -> item.getCode().contains("-" + nightClassIndex)).map(CdMachineExportEnums::getFieldName).collect(Collectors.toList());

            Object scheduleDateCellValue = getCellValue(sheet.getRow(scheduleDateRowNum), 2);
            for (int rowNum = dataStartRowNum; rowNum <= lastRowNum; ++rowNum) {
                Row row = sheet.getRow(rowNum);
                HalfCdImportBak halfCdImportBak = new HalfCdImportBak();
                for (int i = 0; i < classField.size(); i++) {
                    Field field = classField.get(i);
                    String fieldName = field.getName();
                    Object cellValue = getCellValue(row, i);
                    if (StringUtils.isNotBlank(cellValue.toString()) && !"scheduleDate".equals(fieldName)) {
                        if (field.getType() == Integer.class) {
                            try {
                                cellValue = Integer.parseInt(cellValue.toString());
                            } catch (NumberFormatException e) {
                                log.error("数值转换异常：{}", e.getMessage());
                                continue;
                            }
                        } else if (field.getType() == Double.class) {
                            try {
                                cellValue = Double.parseDouble(cellValue.toString());
                            } catch (NumberFormatException e) {
                                log.error("数值转换异常：{}", e.getMessage());
                                continue;
                            }
                        } else if (field.getType() == String.class) {
                            cellValue = cellValue.toString();
                        }
                        ReflectUtils.setFieldValue(halfCdImportBak, fieldName, cellValue);
                    }
                }
                ReflectUtils.setFieldValue(halfCdImportBak, "scheduleDate", scheduleDateCellValue);
//                list.add(halfCdImportBak);
                boolean isPlan = false;
                for (String fieldName : nightFieldNameList) {
                    Double fieldValue = ObjectUtils.defaultIfNull(ReflectUtils.getFieldValue(halfCdImportBak, fieldName), 0D);
                    if (!isPlan) {
                        isPlan = fieldValue > 0;
                    }
                }
                if (isPlan) {
                    importBakHashMap.put(rowNum - dataStartRowNum, halfCdImportBak);
                }
            }

            int nextDayDataStartRowNum = dataRowNum + 12 + scheduleDateRowNum;
            int nextDayLastRowNum = dataRowNum * 2 + 10 + scheduleDateRowNum;
            scheduleDateCellValue = getCellValue(sheet.getRow(nextDayDateRowNum), 2);

            int dayClassIndex = OpenMachineClassEnums.CLASS_THREE.getClassIndex();
            List<String> dayFieldNameList = Arrays.stream(CdMachineExportEnums.values()).filter(item -> item.getCode().contains("-" + dayClassIndex)).map(CdMachineExportEnums::getFieldName).collect(Collectors.toList());

            for (int rowNum = nextDayDataStartRowNum; rowNum <= nextDayLastRowNum; ++rowNum) {
                Row row = sheet.getRow(rowNum);
                if (row == null) {
                    continue;
                }
                HalfCdImportBak halfCdImportBak = new HalfCdImportBak();
                for (int i = 0; i < classField.size(); i++) {
                    Field field = classField.get(i);
                    String fieldName = field.getName();
                    Object cellValue = getCellValue(row, i);
                    if (StringUtils.isNotBlank(cellValue.toString()) && !"scheduleDate".equals(fieldName)) {
                        if (field.getType() == Integer.class) {
                            try {
                                cellValue = Integer.parseInt(cellValue.toString());
                            } catch (NumberFormatException e) {
                                log.error("数值转换异常：{}", e.getMessage());
                                continue;
                            }
                        } else if (field.getType() == Double.class) {
                            try {
                                cellValue = Double.parseDouble(cellValue.toString());
                            } catch (NumberFormatException e) {
                                log.error("数值转换异常：{}", e.getMessage());
                                continue;
                            }
                        } else if (field.getType() == String.class) {
                            cellValue = cellValue.toString();
                        }
                        ReflectUtils.setFieldValue(halfCdImportBak, fieldName, cellValue);
                    }
                }
                ReflectUtils.setFieldValue(halfCdImportBak, "scheduleDate", scheduleDateCellValue);
//                nextDayList.add(halfCdImportBak);
                boolean isPlan = false;
                for (String fieldName : dayFieldNameList) {
                    Double fieldValue = ObjectUtils.defaultIfNull(ReflectUtils.getFieldValue(halfCdImportBak, fieldName), 0D);
                    if (!isPlan) {
                        isPlan = fieldValue > 0;
                    }
                }
                int key = rowNum - dataStartRowNum;
                if (importBakHashMap.containsKey(key)) {
                    HalfCdImportBak halfCdImportBak1 = importBakHashMap.get(key);
                    for (String fieldName : dayFieldNameList) {
                        Double fieldValue = ObjectUtils.defaultIfNull(ReflectUtils.getFieldValue(halfCdImportBak, fieldName), 0D);
                        ReflectUtils.setFieldValue(halfCdImportBak1, fieldName, fieldValue);
                    }
                    halfCdImportBak = halfCdImportBak1;
                    ReflectUtils.setFieldValue(halfCdImportBak, "scheduleDate", scheduleDateCellValue);
                }
                if (isPlan) {
                    importBakHashMap.put(key, halfCdImportBak);
                }
            }

            Date scheduleDate = null;
            List<HalfCdImportBak> halfCdImportBakList = new ArrayList<>(importBakHashMap.values());
            if (CollectionUtils.isNotEmpty(halfCdImportBakList)) {
                scheduleDate = halfCdImportBakList.get(0).getScheduleDate();
            }
            List<NcScheduleResult> ncScheduleResultList = new ArrayList<>();
            List<Cd15ScheduleResult> cd15ScheduleResultList = new ArrayList<>();
            List<Cd90ScheduleResult> cd90ScheduleResultList = new ArrayList<>();
            List<TqScheduleResultDto> tqScheduleResultList = new ArrayList<>();
            List<GsqScheduleResultDto> gsqScheduleResultList = new ArrayList<>();

            // 查询胎面卷曲长度
            List<NcCurlRoll> ncCurlRollList = ncCurlRollService.listCurlRoll(new NcCurlRoll());
            Map<String, BigDecimal> ncCurlLengthMap = new HashMap<>(16);
            if (CollectionUtils.isNotEmpty(ncCurlRollList)) {
                ncCurlLengthMap = ncCurlRollList.stream().collect(Collectors.toMap(NcCurlRoll::getLiningCode, NcCurlRoll::getCurlLength));
            }

            List<String> embryoCodeList = halfCdImportBakList.stream().map(HalfCdImportBak::getCx3).collect(Collectors.toList());

            // 查询施工，赋值胶料、口型等
            List<EngineConstructionInfo> constructionInfoList = cd15ScheduleResultService.listConstruction(embryoCodeList, "1");
            Map<String, EngineConstructionInfo> constructionInfoMap = new HashMap<>(16);
            if (CollectionUtils.isNotEmpty(constructionInfoList)) {
                constructionInfoMap = constructionInfoList.stream().collect(Collectors.toMap(EngineConstructionInfo::getEmbryoCode, Function.identity()));
            }

            NcParams params = new NcParams();
            params.setParamCode(EngineConstants.STANDARD_CRIMP_LENGTH);
            List<NcParamsDto> ncParamsDtos = ncParamsService.selectParamsList(params);
            BigDecimal defaultNcLength = BigDecimal.valueOf(80);
            if (CollectionUtils.isNotEmpty(ncParamsDtos)) {
                NcParamsDto ncParamsDto = ncParamsDtos.get(0);
                String paramValue = ncParamsDto.getParamValue();
                defaultNcLength = new BigDecimal(paramValue);
            }

            // 导入操作
            for (HalfCdImportBak halfCdImportBak : halfCdImportBakList) {
                // 内衬
                NcScheduleResult ncScheduleResult = new NcScheduleResult();
                ncScheduleResult.setScheduleDate(scheduleDate);
                String cx3 = halfCdImportBak.getCx3();
                String nc1 = halfCdImportBak.getNc1();
                EngineConstructionInfo constructionInfo = null;
                if (constructionInfoMap.containsKey(cx3)) {
                    constructionInfo = constructionInfoMap.get(cx3);
                }
                ncScheduleResult.setLiningCode(nc1);
                // 赋值胶料
                if (constructionInfo != null) {
                    ncScheduleResult.setGlueCode(constructionInfo.getInsideRubber());
                }
                Double nc11 = ObjectUtils.defaultIfNull(halfCdImportBak.getNc11(), 0D);
                Double nc13 = ObjectUtils.defaultIfNull(halfCdImportBak.getNc13(), 0D);
                // 将卷转成米
                BigDecimal ncCurlLength = ncCurlLengthMap.getOrDefault(cx3, defaultNcLength);
                ncScheduleResult.setMachineId("压延L3");
                ncScheduleResult.setDayPlanQty(Math.ceil(nc11 * ncCurlLength.doubleValue()));
                ncScheduleResult.setNightPlanQty(Math.ceil(nc13 * ncCurlLength.doubleValue()));
                if (StringUtils.isNotBlank(ncScheduleResult.getMachineId())) {
                    ncScheduleResultList.add(ncScheduleResult);
                }
                // 斜裁
                Cd15ScheduleResult cd15ScheduleResult1 = new Cd15ScheduleResult();
                cd15ScheduleResult1.setScheduleDate(scheduleDate);
                String gd1 = StringUtils.defaultIfBlank(halfCdImportBak.getGd1(), "");
                // 赋值胶料、口型
                if (constructionInfo != null) {
                    cd15ScheduleResult1.setBigRollCode(constructionInfo.getArticleCrownSpec());
                    cd15ScheduleResult1.setCuttingAngle(Double.valueOf(constructionInfo.getBeltCuttingAngle()));
                }
                cd15ScheduleResult1.setSteelStripCode1(gd1);
                Double gd9 = ObjectUtils.defaultIfNull(halfCdImportBak.getGd9(), 0D);
                Double gd10 = ObjectUtils.defaultIfNull(halfCdImportBak.getGd10(), 0D);
                Double gd11 = ObjectUtils.defaultIfNull(halfCdImportBak.getGd11(), 0D);
                Double gd12 = ObjectUtils.defaultIfNull(halfCdImportBak.getGd12(), 0D);

                Double gd15 = ObjectUtils.defaultIfNull(halfCdImportBak.getGd15(), 0D);
                Double gd16 = ObjectUtils.defaultIfNull(halfCdImportBak.getGd16(), 0D);
                Double gd17 = ObjectUtils.defaultIfNull(halfCdImportBak.getGd17(), 0D);
                Double gd18 = ObjectUtils.defaultIfNull(halfCdImportBak.getGd18(), 0D);
                // 将卷转成米
                double cd15CurlLength = 190D;
                if (gd9 > 0 || gd15 > 0) {
                    cd15ScheduleResult1.setMachineId("1#机");
                    cd15ScheduleResult1.setDayPlanQty1(Math.ceil(gd9 * cd15CurlLength));
                    cd15ScheduleResult1.setNightPlanQty1(Math.ceil(gd15 * cd15CurlLength));
                } else if (gd10 > 0 || gd16 > 0) {
                    cd15ScheduleResult1.setMachineId("2#机");
                    cd15ScheduleResult1.setDayPlanQty1(Math.ceil(gd10 * cd15CurlLength));
                    cd15ScheduleResult1.setNightPlanQty1(Math.ceil(gd16 * cd15CurlLength));
                } else if (gd11 > 0 || gd17 > 0) {
                    cd15ScheduleResult1.setMachineId("3#机");
                    cd15ScheduleResult1.setDayPlanQty1(Math.ceil(gd11 * cd15CurlLength));
                    cd15ScheduleResult1.setNightPlanQty1(Math.ceil(gd17 * cd15CurlLength));
                } else if (gd12 > 0 || gd18 > 0) {
                    cd15ScheduleResult1.setMachineId("4#机");
                    cd15ScheduleResult1.setDayPlanQty1(Math.ceil(gd12 * cd15CurlLength));
                    cd15ScheduleResult1.setNightPlanQty1(Math.ceil(gd18 * cd15CurlLength));
                }
                if (StringUtils.isNotBlank(cd15ScheduleResult1.getMachineId())) {
                    cd15ScheduleResultList.add(cd15ScheduleResult1);
                }

                Cd15ScheduleResult cd15ScheduleResult2 = new Cd15ScheduleResult();
                cd15ScheduleResult2.setScheduleDate(scheduleDate);
                String gdt1 = StringUtils.defaultIfBlank(halfCdImportBak.getGdt1(), "");
                // 赋值胶料、口型
                if (constructionInfo != null) {
                    cd15ScheduleResult2.setBigRollCode(constructionInfo.getArticleCrownSpec());
                    cd15ScheduleResult2.setCuttingAngle(Double.valueOf(constructionInfo.getBeltCuttingAngle()));
                }
                cd15ScheduleResult2.setSteelStripCode2(gdt1);
                Double gdt9 = ObjectUtils.defaultIfNull(halfCdImportBak.getGdt9(), 0D);
                Double gdt10 = ObjectUtils.defaultIfNull(halfCdImportBak.getGdt10(), 0D);
                Double gdt11 = ObjectUtils.defaultIfNull(halfCdImportBak.getGdt11(), 0D);
                Double gdt12 = ObjectUtils.defaultIfNull(halfCdImportBak.getGdt12(), 0D);

                Double gdt15 = ObjectUtils.defaultIfNull(halfCdImportBak.getGdt15(), 0D);
                Double gdt16 = ObjectUtils.defaultIfNull(halfCdImportBak.getGdt16(), 0D);
                Double gdt17 = ObjectUtils.defaultIfNull(halfCdImportBak.getGdt17(), 0D);
                Double gdt18 = ObjectUtils.defaultIfNull(halfCdImportBak.getGdt18(), 0D);
                // 将卷转成米
                if (gdt9 > 0 || gdt15 > 0) {
                    cd15ScheduleResult2.setMachineId("1#机");
                    cd15ScheduleResult2.setDayPlanQty1(Math.ceil(gdt9 * cd15CurlLength));
                    cd15ScheduleResult2.setNightPlanQty1(Math.ceil(gdt15 * cd15CurlLength));
                } else if (gdt10 > 0 || gdt16 > 0) {
                    cd15ScheduleResult2.setMachineId("2#机");
                    cd15ScheduleResult2.setDayPlanQty1(Math.ceil(gdt10 * cd15CurlLength));
                    cd15ScheduleResult2.setNightPlanQty1(Math.ceil(gdt16 * cd15CurlLength));
                } else if (gdt11 > 0 || gdt17 > 0) {
                    cd15ScheduleResult2.setMachineId("3#机");
                    cd15ScheduleResult2.setDayPlanQty1(Math.ceil(gdt11 * cd15CurlLength));
                    cd15ScheduleResult2.setNightPlanQty1(Math.ceil(gdt17 * cd15CurlLength));
                } else if (gdt12 > 0 || gdt18 > 0) {
                    cd15ScheduleResult2.setMachineId("4#机");
                    cd15ScheduleResult2.setDayPlanQty1(Math.ceil(gdt12 * cd15CurlLength));
                    cd15ScheduleResult2.setNightPlanQty1(Math.ceil(gdt18 * cd15CurlLength));
                }
                if (StringUtils.isNotBlank(cd15ScheduleResult2.getMachineId())) {
                    cd15ScheduleResultList.add(cd15ScheduleResult2);
                }
                // 直裁
                Cd90ScheduleResult cd90ScheduleResult = new Cd90ScheduleResult();
                cd90ScheduleResult.setScheduleDate(scheduleDate);
                String lb1 = StringUtils.defaultIfBlank(halfCdImportBak.getLb1(), "");
                // 赋值胶料、口型
                if (constructionInfo != null) {
                    cd90ScheduleResult.setBigRollCode(constructionInfo.getCordSpec());
                    cd90ScheduleResult.setCraft(constructionInfo.getTireFabricCraft1());
                }
                cd90ScheduleResult.setClothCode(lb1);
                Double lb9 = ObjectUtils.defaultIfNull(halfCdImportBak.getLb9(), 0D);
                Double lb10 = ObjectUtils.defaultIfNull(halfCdImportBak.getLb10(), 0D);
                Double lb11 = ObjectUtils.defaultIfNull(halfCdImportBak.getLb11(), 0D);
                Double lb12 = ObjectUtils.defaultIfNull(halfCdImportBak.getLb12(), 0D);

                Double lb15 = ObjectUtils.defaultIfNull(halfCdImportBak.getLb15(), 0D);
                Double lb16 = ObjectUtils.defaultIfNull(halfCdImportBak.getLb16(), 0D);
                Double lb17 = ObjectUtils.defaultIfNull(halfCdImportBak.getLb17(), 0D);
                Double lb18 = ObjectUtils.defaultIfNull(halfCdImportBak.getLb18(), 0D);
                // 将卷转成米
                double cd90CurlLength = 87D;
                if (lb9 > 0 || lb15 > 0) {
                    cd90ScheduleResult.setMachineId("1#直裁");
                    cd90ScheduleResult.setDayPlanQty(Math.ceil(lb9 * cd90CurlLength));
                    cd90ScheduleResult.setNightPlanQty(Math.ceil(lb15 * cd90CurlLength));
                } else if (lb10 > 0 || lb16 > 0) {
                    cd90ScheduleResult.setMachineId("2#直裁");
                    cd90ScheduleResult.setDayPlanQty(Math.ceil(lb10 * cd90CurlLength));
                    cd90ScheduleResult.setNightPlanQty(Math.ceil(lb16 * cd90CurlLength));
                } else if (lb11 > 0 || lb17 > 0) {
                    cd90ScheduleResult.setMachineId("3#直裁");
                    cd90ScheduleResult.setDayPlanQty(Math.ceil(lb11 * cd90CurlLength));
                    cd90ScheduleResult.setNightPlanQty(Math.ceil(lb17 * cd90CurlLength));
                } else if (lb12 > 0 || lb18 > 0) {
                    cd90ScheduleResult.setMachineId("4#直裁");
                    cd90ScheduleResult.setDayPlanQty(Math.ceil(lb12 * cd90CurlLength));
                    cd90ScheduleResult.setNightPlanQty(Math.ceil(lb18 * cd90CurlLength));
                }
                if (StringUtils.isNotBlank(cd90ScheduleResult.getMachineId())) {
                    cd90ScheduleResultList.add(cd90ScheduleResult);
                }
                // 2#直裁
                Cd90ScheduleResult cd90ScheduleResult1 = new Cd90ScheduleResult();
                cd90ScheduleResult1.setScheduleDate(scheduleDate);
                String lbt1 = StringUtils.defaultIfBlank(halfCdImportBak.getLbt1(), "");
                // 赋值胶料、口型
                if (constructionInfo != null) {
                    cd90ScheduleResult1.setBigRollCode(constructionInfo.getCordSpec());
                    cd90ScheduleResult1.setCraft(constructionInfo.getTireFabricCraft1());
                }
                cd90ScheduleResult1.setClothCode(lbt1);
                Double lbt9 = ObjectUtils.defaultIfNull(halfCdImportBak.getLbt9(), 0D);
                Double lbt10 = ObjectUtils.defaultIfNull(halfCdImportBak.getLbt10(), 0D);
                Double lbt11 = ObjectUtils.defaultIfNull(halfCdImportBak.getLbt11(), 0D);
                Double lbt12 = ObjectUtils.defaultIfNull(halfCdImportBak.getLbt12(), 0D);

                Double lbt15 = ObjectUtils.defaultIfNull(halfCdImportBak.getLb15(), 0D);
                Double lbt16 = ObjectUtils.defaultIfNull(halfCdImportBak.getLb16(), 0D);
                Double lbt17 = ObjectUtils.defaultIfNull(halfCdImportBak.getLb17(), 0D);
                Double lbt18 = ObjectUtils.defaultIfNull(halfCdImportBak.getLb18(), 0D);
                // 将卷转成米
                if (lbt9 > 0 || lbt15 > 0) {
                    cd90ScheduleResult1.setMachineId("1#直裁");
                    cd90ScheduleResult1.setDayPlanQty(Math.ceil(lbt9 * cd90CurlLength));
                    cd90ScheduleResult1.setNightPlanQty(Math.ceil(lbt15 * cd90CurlLength));
                } else if (lbt10 > 0 || lbt16 > 0) {
                    cd90ScheduleResult1.setMachineId("2#直裁");
                    cd90ScheduleResult1.setDayPlanQty(Math.ceil(lbt10 * cd90CurlLength));
                    cd90ScheduleResult1.setNightPlanQty(Math.ceil(lbt16 * cd90CurlLength));
                } else if (lbt11 > 0 || lbt17 > 0) {
                    cd90ScheduleResult1.setMachineId("3#直裁");
                    cd90ScheduleResult1.setDayPlanQty(Math.ceil(lbt11 * cd90CurlLength));
                    cd90ScheduleResult1.setNightPlanQty(Math.ceil(lbt17 * cd90CurlLength));
                } else if (lbt12 > 0 || lbt18 > 0) {
                    cd90ScheduleResult1.setMachineId("4#直裁");
                    cd90ScheduleResult1.setDayPlanQty(Math.ceil(lbt12 * cd90CurlLength));
                    cd90ScheduleResult1.setNightPlanQty(Math.ceil(lbt18 * cd90CurlLength));
                }
                if (StringUtils.isNotBlank(cd90ScheduleResult1.getMachineId())) {
                    cd90ScheduleResultList.add(cd90ScheduleResult1);
                }
                // 胎圈
                TqScheduleResultDto tqScheduleResult = new TqScheduleResultDto();
                tqScheduleResult.setScheduleDate(scheduleDate);
                String tq1 = StringUtils.defaultIfBlank(halfCdImportBak.getTq1(), "");
                // 赋值胶料、口型
                if (constructionInfo != null) {
                    tqScheduleResult.setSteelRingCode(constructionInfo.getBeadCode());
                    tqScheduleResult.setTriangleGlueCode(constructionInfo.getApexCode());
                    tqScheduleResult.setGlueCode(constructionInfo.getHexagonRubberCode());
                    tqScheduleResult.setMouthPlateCode(constructionInfo.getHexagonMouthPlate());
                    tqScheduleResult.setSpecSize(constructionInfo.getHexagonRubberDimension());
                }
                tqScheduleResult.setBeadCode(tq1);
                Double tq8 = ObjectUtils.defaultIfNull(halfCdImportBak.getTq8(), 0D);
                Double tq9 = ObjectUtils.defaultIfNull(halfCdImportBak.getTq9(), 0D);
                Double tq10 = ObjectUtils.defaultIfNull(halfCdImportBak.getTq10(), 0D);
                Double tq11 = ObjectUtils.defaultIfNull(halfCdImportBak.getTq11(), 0D);
                Double tq12 = ObjectUtils.defaultIfNull(halfCdImportBak.getTq12(), 0D);
                Double tq13 = ObjectUtils.defaultIfNull(halfCdImportBak.getTq13(), 0D);
                Double tq14 = ObjectUtils.defaultIfNull(halfCdImportBak.getTq14(), 0D);
                Double tq15 = ObjectUtils.defaultIfNull(halfCdImportBak.getTq15(), 0D);
                Double tq16 = ObjectUtils.defaultIfNull(halfCdImportBak.getTq16(), 0D);
                Double tq17 = ObjectUtils.defaultIfNull(halfCdImportBak.getTq17(), 0D);

                Double tq19 = ObjectUtils.defaultIfNull(halfCdImportBak.getTq19(), 0D);
                Double tq20 = ObjectUtils.defaultIfNull(halfCdImportBak.getTq20(), 0D);
                Double tq21 = ObjectUtils.defaultIfNull(halfCdImportBak.getTq21(), 0D);
                Double tq22 = ObjectUtils.defaultIfNull(halfCdImportBak.getTq22(), 0D);
                Double tq23 = ObjectUtils.defaultIfNull(halfCdImportBak.getTq23(), 0D);
                Double tq24 = ObjectUtils.defaultIfNull(halfCdImportBak.getTq24(), 0D);
                Double tq25 = ObjectUtils.defaultIfNull(halfCdImportBak.getTq25(), 0D);
                Double tq26 = ObjectUtils.defaultIfNull(halfCdImportBak.getTq26(), 0D);
                Double tq27 = ObjectUtils.defaultIfNull(halfCdImportBak.getTq27(), 0D);
                Double tq28 = ObjectUtils.defaultIfNull(halfCdImportBak.getTq28(), 0D);
                // 将卷转成米
                if (tq8 > 0 || tq19 > 0) {
                    tqScheduleResult.setMachineId("1号");
                    tqScheduleResult.setDayPlanQty(tq8);
                    tqScheduleResult.setNightPlanQty(tq19);
                } else if (tq9 > 0 || tq20 > 0) {
                    tqScheduleResult.setMachineId("2号");
                    tqScheduleResult.setDayPlanQty(tq9);
                    tqScheduleResult.setNightPlanQty(tq20);
                } else if (tq10 > 0 || tq21 > 0) {
                    tqScheduleResult.setMachineId("3号");
                    tqScheduleResult.setDayPlanQty(tq10);
                    tqScheduleResult.setNightPlanQty(tq21);
                } else if (tq11 > 0 || tq22 > 0) {
                    tqScheduleResult.setMachineId("4号");
                    tqScheduleResult.setDayPlanQty(tq11);
                    tqScheduleResult.setNightPlanQty(tq22);
                } else if (tq12 > 0 || tq23 > 0) {
                    tqScheduleResult.setMachineId("5号");
                    tqScheduleResult.setDayPlanQty(tq12);
                    tqScheduleResult.setNightPlanQty(tq23);
                } else if (tq13 > 0 || tq24 > 0) {
                    tqScheduleResult.setMachineId("7号");
                    tqScheduleResult.setDayPlanQty(tq13);
                    tqScheduleResult.setNightPlanQty(tq24);
                } else if (tq14 > 0 || tq25 > 0) {
                    tqScheduleResult.setMachineId("8号");
                    tqScheduleResult.setDayPlanQty(tq14);
                    tqScheduleResult.setNightPlanQty(tq25);
                } else if (tq15 > 0 || tq26 > 0) {
                    tqScheduleResult.setMachineId("9号");
                    tqScheduleResult.setDayPlanQty(tq15);
                    tqScheduleResult.setNightPlanQty(tq26);
                } else if (tq16 > 0 || tq27 > 0) {
                    tqScheduleResult.setMachineId("12号");
                    tqScheduleResult.setDayPlanQty(tq16);
                    tqScheduleResult.setNightPlanQty(tq27);
                } else if (tq17 > 0 || tq28 > 0) {
                    tqScheduleResult.setMachineId("13号");
                    tqScheduleResult.setDayPlanQty(tq17);
                    tqScheduleResult.setNightPlanQty(tq28);
                }
                if (StringUtils.isNotBlank(tqScheduleResult.getMachineId())) {
                    tqScheduleResultList.add(tqScheduleResult);
                }
                // 钢丝圈
                GsqScheduleResultDto gsqScheduleResult = new GsqScheduleResultDto();
                gsqScheduleResult.setScheduleDate(scheduleDate);
                String gsq1 = StringUtils.defaultIfBlank(halfCdImportBak.getGsq1(), "");
                // 赋值胶料、口型
                if (constructionInfo != null) {
                    gsqScheduleResult.setSteelType(constructionInfo.getBeadType());
                    gsqScheduleResult.setDimension(constructionInfo.getDimension().toString());
                    gsqScheduleResult.setRank(constructionInfo.getBeadArrange());
                }
                gsqScheduleResult.setSteelRingCode(gsq1);
                Double gsq6 = ObjectUtils.defaultIfNull(halfCdImportBak.getGsq6(), 0D);
                Double gsq7 = ObjectUtils.defaultIfNull(halfCdImportBak.getGsq7(), 0D);
                Double gsq8 = ObjectUtils.defaultIfNull(halfCdImportBak.getGsq8(), 0D);

                Double gsq10 = ObjectUtils.defaultIfNull(halfCdImportBak.getGsq10(), 0D);
                Double gsq11 = ObjectUtils.defaultIfNull(halfCdImportBak.getGsq11(), 0D);
                Double gsq12 = ObjectUtils.defaultIfNull(halfCdImportBak.getGsq12(), 0D);
                if (gsq6 > 0 || gsq10 > 0) {
                    gsqScheduleResult.setMachineId("1#机");
                    gsqScheduleResult.setDayPlanQty(gsq6);
                    gsqScheduleResult.setNightPlanQty(gsq10);
                } else if (gsq7 > 0 || gsq11 > 0) {
                    gsqScheduleResult.setMachineId("3#机");
                    gsqScheduleResult.setDayPlanQty(gsq7);
                    gsqScheduleResult.setNightPlanQty(gsq11);
                } else if (gsq8 > 0 || gsq12 > 0) {
                    gsqScheduleResult.setMachineId("4#机");
                    gsqScheduleResult.setDayPlanQty(gsq8);
                    gsqScheduleResult.setNightPlanQty(gsq12);
                }
                if (StringUtils.isNotBlank(gsqScheduleResult.getMachineId())) {
                    gsqScheduleResultList.add(gsqScheduleResult);
                }
            }

            String scheduleDateStr = DateUtils.parseDateToStr("yyyy-MM-dd", scheduleDate);

            Map<String, List<NcScheduleResult>> ncScheduleMachineMap = ncScheduleResultList.stream().collect(Collectors.groupingBy(NcScheduleResult::getMachineId));
            Set<Map.Entry<String, List<NcScheduleResult>>> ncEntrySet = ncScheduleMachineMap.entrySet();
            for (Map.Entry<String, List<NcScheduleResult>> entry : ncEntrySet) {
                List<NcScheduleResult> value = entry.getValue();
                long dayProduceOrder = 1;
                long nightProduceOrder = 1;
                for (NcScheduleResult scheduleResult : value) {
                    Double dayPlanQty = scheduleResult.getDayPlanQty();
                    Long dayOrder = scheduleResult.getDayProduceOrder();
                    if (dayOrder == null && dayPlanQty != null && dayPlanQty > 0) {
                        scheduleResult.setDayProduceOrder(dayProduceOrder);
                        dayProduceOrder++;
                    }
                    Double nightPlanQty = scheduleResult.getNightPlanQty();
                    Long nightOrder = scheduleResult.getDayProduceOrder();
                    if (nightOrder == null && nightPlanQty != null && nightPlanQty > 0) {
                        scheduleResult.setNightProduceOrder(nightProduceOrder);
                        nightProduceOrder++;
                    }
                }
            }
            AjaxResult ajaxResult = ncScheduleResultService.importData(ncScheduleResultList, importLog.getId(), scheduleDateStr);
            List<ImportErrorLog> importErrorLogs = new ArrayList<>();
            if (ajaxResult.get(AjaxResult.CODE_TAG).equals(AjaxResult.Type.ERROR.value())) {
                List<ImportErrorLog> tmImportErrorLog = (List<ImportErrorLog>) ajaxResult.get(AjaxResult.DATA_TAG);
                importErrorLogs.addAll(tmImportErrorLog);
            }
            importLog.setRowCount(ncScheduleResultList.size());
            importLog.setBeginTime(beginTime);
            Date endTime = DateUtils.getNowDate();
            importLog.setEndTime(endTime);
            importLog.setSpendTime(DateUtils.getDiffTime(endTime, beginTime));
            ImportExcelUtils.updateImportLogAndFormatMsg(importLog, ajaxResult, this.iImportLogService);
            ImportExcelUtils.saveImportErrorLogs(ajaxResult, this.iImportErrorLogService);
            importLog = this.iImportLogService.add(importLog);

            Map<String, List<Cd15ScheduleResult>> cd15ScheduleMachineMap = cd15ScheduleResultList.stream().collect(Collectors.groupingBy(Cd15ScheduleResult::getMachineId));
            Set<Map.Entry<String, List<Cd15ScheduleResult>>> cd15EntrySet = cd15ScheduleMachineMap.entrySet();
            for (Map.Entry<String, List<Cd15ScheduleResult>> entry : cd15EntrySet) {
                List<Cd15ScheduleResult> value = entry.getValue();
                long dayProduceOrder = 1;
                long nightProduceOrder = 1;
                for (Cd15ScheduleResult scheduleResult : value) {
                    Double dayPlanQty = scheduleResult.getDayPlanQty1();
                    Long dayOrder = scheduleResult.getDayProduceOrder1();
                    if (dayOrder == null && dayPlanQty != null && dayPlanQty > 0) {
                        scheduleResult.setDayProduceOrder1(dayProduceOrder);
                        dayProduceOrder++;
                    }
                    Double nightPlanQty = scheduleResult.getNightPlanQty1();
                    Long nightOrder = scheduleResult.getDayProduceOrder1();
                    if (nightOrder == null && nightPlanQty != null && nightPlanQty > 0) {
                        scheduleResult.setNightProduceOrder1(nightProduceOrder);
                        nightProduceOrder++;
                    }
                }
            }
            AjaxResult ajaxResult1 = cd15ScheduleResultService.importData(cd15ScheduleResultList, importLog.getId(), scheduleDateStr);
            if (ajaxResult1.get(AjaxResult.CODE_TAG).equals(AjaxResult.Type.ERROR.value())) {
                List<ImportErrorLog> tcImportErrorLog = (List<ImportErrorLog>) ajaxResult1.get(AjaxResult.DATA_TAG);
                importErrorLogs.addAll(tcImportErrorLog);
            }
            importLog.setRowCount(cd15ScheduleResultList.size());
            importLog.setBeginTime(beginTime);
            importLog.setEndTime(endTime);
            importLog.setSpendTime(DateUtils.getDiffTime(endTime, beginTime));
//            ImportExcelUtils.updateImportLogAndFormatMsg(importLog, tcAjaxResult, this.iImportLogService);
//            ImportExcelUtils.saveImportErrorLogs(tcAjaxResult, this.iImportErrorLogService);

            // 因为1#帘布和2#帘布的编号是同一个编号，特殊处理，同编号的合并对应计划量
            Map<String, Cd90ScheduleResult> cd90ScheduleResultMap = cd90ScheduleResultList.stream().collect(Collectors.toMap(Cd90ScheduleResult::getClothCode, Function.identity(), (s1, s2) -> {
                double sumDayPlanQty = BigDecimalUtil.add(ObjectUtils.defaultIfNull(s1.getDayPlanQty(), 0D), ObjectUtils.defaultIfNull(s2.getDayPlanQty(), 0D));
                double sumNightPlanQty = BigDecimalUtil.add(ObjectUtils.defaultIfNull(s1.getNightPlanQty(), 0D), ObjectUtils.defaultIfNull(s2.getNightPlanQty(), 0D));

                s1.setDayPlanQty(sumDayPlanQty);
                s1.setNightPlanQty(sumNightPlanQty);
                return s1;
            }));

            Map<String, List<Cd90ScheduleResult>> cd90ScheduleMachineMap = cd90ScheduleResultMap.values().stream().collect(Collectors.groupingBy(Cd90ScheduleResult::getMachineId));
            Set<Map.Entry<String, List<Cd90ScheduleResult>>> cd90EntrySet = cd90ScheduleMachineMap.entrySet();
            for (Map.Entry<String, List<Cd90ScheduleResult>> entry : cd90EntrySet) {
                List<Cd90ScheduleResult> value = entry.getValue();
                long dayProduceOrder = 1;
                long nightProduceOrder = 1;
                for (Cd90ScheduleResult scheduleResult : value) {
                    Double dayPlanQty = scheduleResult.getDayPlanQty();
                    Long dayOrder = scheduleResult.getDayProduceOrder();
                    if (dayOrder == null && dayPlanQty != null && dayPlanQty > 0) {
                        scheduleResult.setDayProduceOrder(dayProduceOrder);
                        dayProduceOrder++;
                    }
                    Double nightPlanQty = scheduleResult.getNightPlanQty();
                    Long nightOrder = scheduleResult.getDayProduceOrder();
                    if (nightOrder == null && nightPlanQty != null && nightPlanQty > 0) {
                        scheduleResult.setNightProduceOrder(nightProduceOrder);
                        nightProduceOrder++;
                    }
                }
            }
            AjaxResult ajaxResult2 = cd90ScheduleResultService.importData(cd90ScheduleResultList, importLog.getId(), scheduleDateStr);
            if (ajaxResult2.get(AjaxResult.CODE_TAG).equals(AjaxResult.Type.ERROR.value())) {
                List<ImportErrorLog> tmImportErrorLog = (List<ImportErrorLog>) ajaxResult2.get(AjaxResult.DATA_TAG);
                importErrorLogs.addAll(tmImportErrorLog);
            }
            importLog.setRowCount(cd90ScheduleResultList.size());
            importLog.setBeginTime(beginTime);
            importLog.setEndTime(endTime);
            importLog.setSpendTime(DateUtils.getDiffTime(endTime, beginTime));
//            ImportExcelUtils.updateImportLogAndFormatMsg(importLog, ajaxResult2, this.iImportLogService);
//            ImportExcelUtils.saveImportErrorLogs(ajaxResult2, this.iImportErrorLogService);
            importLog = this.iImportLogService.add(importLog);

            Map<String, List<TqScheduleResultDto>> tqScheduleMachineMap = tqScheduleResultList.stream().collect(Collectors.groupingBy(TqScheduleResultDto::getMachineId));
            Set<Map.Entry<String, List<TqScheduleResultDto>>> tqEntrySet = tqScheduleMachineMap.entrySet();
            for (Map.Entry<String, List<TqScheduleResultDto>> entry : tqEntrySet) {
                List<TqScheduleResultDto> value = entry.getValue();
                int dayProduceOrder = 1;
                int nightProduceOrder = 1;
                for (TqScheduleResultDto scheduleResult : value) {
                    Double dayPlanQty = scheduleResult.getDayPlanQty();
                    Integer dayOrder = scheduleResult.getDayProduceOrder();
                    if (dayOrder == null && dayPlanQty != null && dayPlanQty > 0) {
                        scheduleResult.setDayProduceOrder(dayProduceOrder);
                        dayProduceOrder++;
                    }
                    Double nightPlanQty = scheduleResult.getNightPlanQty();
                    Integer nightOrder = scheduleResult.getDayProduceOrder();
                    if (nightOrder == null && nightPlanQty != null && nightPlanQty > 0) {
                        scheduleResult.setNightProduceOrder(nightProduceOrder);
                        nightProduceOrder++;
                    }
                }
            }
            AjaxResult ajaxResult3 = tqScheduleResultService.importData(tqScheduleResultList, importLog.getId(), scheduleDate);
            if (ajaxResult3.get(AjaxResult.CODE_TAG).equals(AjaxResult.Type.ERROR.value())) {
                List<ImportErrorLog> tmImportErrorLog = (List<ImportErrorLog>) ajaxResult3.get(AjaxResult.DATA_TAG);
                importErrorLogs.addAll(tmImportErrorLog);
            }
            importLog.setRowCount(tqScheduleResultList.size());
            importLog.setBeginTime(beginTime);
            importLog.setEndTime(endTime);
            importLog.setSpendTime(DateUtils.getDiffTime(endTime, beginTime));
//            ImportExcelUtils.updateImportLogAndFormatMsg(importLog, ajaxResult2, this.iImportLogService);
//            ImportExcelUtils.saveImportErrorLogs(ajaxResult2, this.iImportErrorLogService);
            importLog = this.iImportLogService.add(importLog);

            Map<String, List<GsqScheduleResultDto>> gsqScheduleMachineMap = gsqScheduleResultList.stream().collect(Collectors.groupingBy(GsqScheduleResultDto::getMachineId));
            Set<Map.Entry<String, List<GsqScheduleResultDto>>> gsqEntrySet = gsqScheduleMachineMap.entrySet();
            for (Map.Entry<String, List<GsqScheduleResultDto>> entry : gsqEntrySet) {
                List<GsqScheduleResultDto> value = entry.getValue();
                int dayProduceOrder = 1;
                int nightProduceOrder = 1;
                for (GsqScheduleResultDto scheduleResult : value) {
                    Double dayPlanQty = scheduleResult.getDayPlanQty();
                    Integer dayOrder = scheduleResult.getDayProduceOrder();
                    if (dayOrder == null && dayPlanQty != null && dayPlanQty > 0) {
                        scheduleResult.setDayProduceOrder(dayProduceOrder);
                        dayProduceOrder++;
                    }
                    Double nightPlanQty = scheduleResult.getNightPlanQty();
                    Integer nightOrder = scheduleResult.getDayProduceOrder();
                    if (nightOrder == null && nightPlanQty != null && nightPlanQty > 0) {
                        scheduleResult.setNightProduceOrder(nightProduceOrder);
                        nightProduceOrder++;
                    }
                }
            }
            AjaxResult ajaxResult4 = tqScheduleResultService.importData(tqScheduleResultList, importLog.getId(), scheduleDate);
            if (ajaxResult4.get(AjaxResult.CODE_TAG).equals(AjaxResult.Type.ERROR.value())) {
                List<ImportErrorLog> importErrorLogs1 = (List<ImportErrorLog>) ajaxResult4.get(AjaxResult.DATA_TAG);
                importErrorLogs.addAll(importErrorLogs1);
            }
            importLog.setRowCount(tqScheduleResultList.size());
            importLog.setBeginTime(beginTime);
            importLog.setEndTime(endTime);
            importLog.setSpendTime(DateUtils.getDiffTime(endTime, beginTime));
//            ImportExcelUtils.updateImportLogAndFormatMsg(importLog, ajaxResult2, this.iImportLogService);
//            ImportExcelUtils.saveImportErrorLogs(ajaxResult2, this.iImportErrorLogService);
            importLog = this.iImportLogService.add(importLog);

            if (CollectionUtils.isNotEmpty(importErrorLogs)) {
                return AjaxResult.error("导入失败", importErrorLogs);
            }
        }
        return AjaxResult.success();
    }

}
