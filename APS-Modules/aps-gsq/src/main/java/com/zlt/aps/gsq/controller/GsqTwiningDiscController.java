package com.zlt.aps.gsq.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.ImportLog;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.api.gateway.system.service.IImportErrorLogService;
import com.ruoyi.api.gateway.system.service.IImportLogService;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.gsq.api.domain.entity.GsqTwiningDisc;
import com.zlt.aps.gsq.api.domain.vo.GsqTwiningDiscImportVo;
import com.zlt.aps.gsq.mapper.GsqTwiningDiscMapper;
import com.zlt.aps.gsq.service.IGsqTwiningDiscService;
import com.zlt.aps.utils.AppUtils;
import com.zlt.bill.common.controller.AbstractDocBizController;
import com.zlt.bill.common.service.IDocService;
import com.zlt.common.utils.ImportExcelUtils;
import com.zlt.common.utils.PubUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.List;

/**
 * 钢丝圈缠绕盘Controller
 * 路径：/gsq/twiningDisc
 * <p>主子表管理：列表显示主表，新增/编辑弹窗内含主表表单与子表明细（钢丝圈）</p>
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

    /** 导入日志服务（主子表平铺导入不走框架super.importData，需自行记录导入日志） */
    @Autowired
    private IImportLogService iImportLogService;

    /** 导入错误明细服务 */
    @Autowired
    private IImportErrorLogService iImportErrorLogService;

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
     * 保存钢丝圈缠绕盘（id为空新增，id不为空修改），级联保存子表明细
     */
    @Log(title = "钢丝圈缠绕盘", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("保存")
    @PostMapping("/save")
    @Override
    public AjaxResult save(@RequestBody GsqTwiningDisc billVO) {
        return gsqTwiningDiscService.saveMainAndSub(billVO);
    }

    /**
     * 删除钢丝圈缠绕盘（逻辑删除主表，级联逻辑删除子表）
     */
    @Log(title = "钢丝圈缠绕盘", businessType = BusinessType.DELETE)
    @ApiOperation("删除")
    @PostMapping("/delete/{ids}")
    public AjaxResult deleteByIds(@PathVariable("ids") List<Long> ids) {
        return gsqTwiningDiscService.removeMainAndSub(ids);
    }

    /**
     * 获取钢丝圈缠绕盘详细信息（含子表明细及钢丝圈名称反显）
     */
    @ApiOperation("获取详细信息")
    @GetMapping("/{id}")
    @Override
    public GsqTwiningDisc getInfo(@PathVariable("id") Long id) {
        GsqTwiningDisc entity = super.getInfo(id);
        if (entity != null) {
            entity.setSubList(gsqTwiningDiscService.querySubListByDiscId(id));
        }
        return entity;
    }

    /**
     * 导入钢丝圈缠绕盘（主子表平铺格式）
     * <p>模板与解析均使用 GsqTwiningDiscImportVo：一行 = 主表字段 + 子表字段，
     * 按缠绕盘编码分组组装主表+子表明细后级联保存</p>
     */
    @Log(title = "钢丝圈缠绕盘", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    @Override
    public AjaxResult importData(@RequestBody ImportContext importContext,
                                 @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        // 记录导入日志并上传导入文件
        Date beginTime = DateUtils.getNowDate();
        ImportLog importLog = ImportExcelUtils.getImportLogAndUploadFile(importContext.getFileBytes(),
                importContext.getImportFilePath(), importContext.getProcedureCode(), importContext.getFunctionName(),
                importContext.getOriFileName(), 1);
        importLog = this.iImportLogService.add(importLog);

        // 按导入VO解析平铺结构（主表字段+子表字段）
        ExcelUtil<GsqTwiningDiscImportVo> util = new ExcelUtil<>(GsqTwiningDiscImportVo.class);
        InputStream is = new ByteArrayInputStream(importContext.getFileBytes());
        List<GsqTwiningDiscImportVo> list = util.importExcel(is);
        AjaxResult ajaxResult = gsqTwiningDiscService.importMainAndSubData(list, updateSupport, importLog.getId());

        // 回写导入日志（行数、耗时）并保存错误明细
        Date endTime = DateUtils.getNowDate();
        importLog.setRowCount(list.size());
        importLog.setBeginTime(beginTime);
        importLog.setEndTime(endTime);
        importLog.setSpendTime(DateUtils.getDiffTime(endTime, beginTime));
        ImportExcelUtils.updateImportLogAndFormatMsg(importLog, ajaxResult, this.iImportLogService);
        ImportExcelUtils.saveImportErrorLogs(ajaxResult, this.iImportErrorLogService);
        return ajaxResult;
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
    }

    /**
     * 获取导出数据列表，并补反显字段
     */
    @Override
    protected List<GsqTwiningDisc> listExportData(GsqTwiningDisc obj) {
        QueryWrapper<GsqTwiningDisc> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, obj);
        wrapper.last("ORDER BY " + getOrderBy());
        List<GsqTwiningDisc> list = gsqTwiningDiscMapper.selectList(wrapper);
        AppUtils.formatData(list, getQueryFormulas());
        return list;
    }
}
