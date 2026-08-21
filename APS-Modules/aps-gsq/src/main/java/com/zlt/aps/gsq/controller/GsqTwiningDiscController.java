package com.zlt.aps.gsq.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.gsq.api.domain.entity.GsqTwiningDisc;
import com.zlt.aps.gsq.mapper.GsqTwiningDiscMapper;
import com.zlt.aps.gsq.service.IGsqTwiningDiscService;
import com.zlt.aps.utils.AppUtils;
import com.zlt.bill.common.controller.AbstractDocBizController;
import com.zlt.bill.common.service.IDocService;
import com.zlt.common.utils.PubUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * 钢丝圈缠绕盘Controller
 * 路径：/gsq/twiningDisc
 * <p>单表管理：缠绕盘基础信息；规格关系/机台关系按编码关联，独立页面维护</p>
 *
 * @author zlt
 * @date 2026-07-08
 */
@Slf4j
@Api(tags = "钢丝圈缠绕盘")
@RestController
@RequestMapping("/gsq/twiningDisc")
public class GsqTwiningDiscController extends AbstractDocBizController<GsqTwiningDisc> {

    @Autowired
    private IGsqTwiningDiscService gsqTwiningDiscService;

    @Resource
    private GsqTwiningDiscMapper gsqTwiningDiscMapper;

    /**
     * 查询钢丝圈缠绕盘列表
     */
    @ApiOperation("查询钢丝圈缠绕盘列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody GsqTwiningDisc queryVO) {
        startPage();
        List<GsqTwiningDisc> list = gsqTwiningDiscMapper.listTwiningDisc(queryVO);
        return getDataTable(list);
    }

    /**
     * 保存钢丝圈缠绕盘（单表保存，带编码唯一性校验）
     */
    @Log(title = "钢丝圈缠绕盘", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("保存")
    @PostMapping("/save")
    @Override
    public AjaxResult save(@RequestBody GsqTwiningDisc billVO) {
        return gsqTwiningDiscService.saveWithCheck(billVO);
    }

    /**
     * 删除钢丝圈缠绕盘（逻辑删除主表，按缠绕盘编码级联逻辑删除规格关系及机台关系）
     */
    @Log(title = "钢丝圈缠绕盘", businessType = BusinessType.DELETE)
    @ApiOperation("删除")
    @PostMapping("/delete/{ids}")
    public AjaxResult deleteByIds(@PathVariable("ids") List<Long> ids) {
        return gsqTwiningDiscService.removeMainAndRelation(ids);
    }

    /**
     * 获取钢丝圈缠绕盘详细信息
     */
    @ApiOperation("获取详细信息")
    @GetMapping("/{id}")
    @Override
    public GsqTwiningDisc getInfo(@PathVariable("id") Long id) {
        return super.getInfo(id);
    }

    /**
     * 导入钢丝圈缠绕盘（单表格式，走框架标准导入：模板解析/导入日志/错误明细）
     */
    @Log(title = "钢丝圈缠绕盘", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    @Override
    public AjaxResult importData(com.ruoyi.api.gateway.system.domain.vo.ImportContext importContext,
                                 @org.springframework.web.bind.annotation.RequestParam("updateSupport") boolean updateSupport) throws Exception {
        return super.importData(importContext, updateSupport);
    }

    /**
     * 导出钢丝圈缠绕盘
     */
    @Log(title = "钢丝圈缠绕盘", businessType = BusinessType.EXPORT)
    @ApiOperation("导出数据")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody GsqTwiningDisc queryVO,
                             @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        List<GsqTwiningDisc> list = listExportData(queryVO);
        ExcelUtil<GsqTwiningDisc> util = new ExcelUtil<>(GsqTwiningDisc.class);
        Workbook workbook = util.exportExcelFromList(list, fileName);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        return out.toByteArray();
    }

    /**
     * 校验缠绕盘编码唯一性
     */
    @ApiOperation("校验缠绕盘编码唯一性")
    @PostMapping("/checkUnique")
    public String checkUnique(@RequestBody GsqTwiningDisc entity) {
        return gsqTwiningDiscService.checkUnique(entity);
    }

    /**
     * 查询施工信息表全部钢丝圈选项（编码+名称，去重），供规格关系页面下拉选择使用
     */
    @ApiOperation("查询钢丝圈下拉选项")
    @GetMapping("/listSteelRingOptions")
    public AjaxResult listSteelRingOptions() {
        return AjaxResult.success(gsqTwiningDiscService.listSteelRingOptions());
    }

    @Override
    protected IDocService getDocService() {
        return gsqTwiningDiscService;
    }

    @Override
    protected String getTypeCode() {
        return "0";
    }

    @Override
    protected String getOrderBy() {
        return "CREATE_TIME desc";
    }

    /**
     * 主表反显公式
     */
    @Override
    protected String[] getQueryFormulas() {
        return gsqTwiningDiscService.getQueryFormulas();
    }

    /**
     * 构建查询条件（手动追加 IS_DELETE=0 过滤逻辑删除数据）
     */
    @Override
    protected void builderCondition(QueryWrapper<GsqTwiningDisc> queryWrapper, GsqTwiningDisc queryVO) {
        queryWrapper.eq("IS_DELETE", "0");
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getTwiningDiscCode()), "TWINING_DISC_CODE", queryVO.getTwiningDiscCode());
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getTwiningDiscName()), "TWINING_DISC_NAME", queryVO.getTwiningDiscName());
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getStatus()), "STATUS", queryVO.getStatus());
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getSortType()), "SORT_TYPE", queryVO.getSortType());
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFactoryCode()), "FACTORY_CODE", queryVO.getFactoryCode());
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getDataSource()), "DATA_SOURCE", queryVO.getDataSource());
    }

    /**
     * 获取导出数据列表（使用含规格数/机台数统计的自定义查询），并补反显字段
     */
    @Override
    protected List<GsqTwiningDisc> listExportData(GsqTwiningDisc obj) {
        // 使用listTwiningDisc自定义SQL查询，该SQL包含关联规格数(specCount)、关联机台数(machineCount)的子查询统计
        List<GsqTwiningDisc> list = gsqTwiningDiscMapper.listTwiningDisc(obj);
        AppUtils.formatData(list, getQueryFormulas());
        return list;
    }
}
