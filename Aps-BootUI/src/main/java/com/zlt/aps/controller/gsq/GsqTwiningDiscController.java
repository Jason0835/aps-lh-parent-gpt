package com.zlt.aps.controller.gsq;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common4ui.constant.UserConstants;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.zlt.aps.gsq.api.domain.entity.GsqTwiningDisc;
import com.zlt.aps.gsq.api.domain.vo.GsqTwiningDiscImportVo;
import com.zlt.aps.gsq.api.service.IGsqTwiningDiscService;
import com.zlt.file.encryptbyll.FileEncryptUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.io.IOUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;

/**
 * 钢丝圈缠绕盘管理 UI 控制层
 * <p>主子表管理：列表显示主表，新增/编辑弹窗含主表表单与子表明细</p>
 *
 * @author zlt
 * @date 2026-07-08
 */
@Api(tags = "钢丝圈缠绕盘管理")
@Controller
@RequestMapping("/gsq/twiningDisc")
public class GsqTwiningDiscController extends BaseUIController<GsqTwiningDisc> {

    @Resource
    private IGsqTwiningDiscService gsqTwiningDiscRemoteService;

    /**
     * 查询钢丝圈缠绕盘列表
     */
    @ApiOperation("查询钢丝圈缠绕盘列表")
    @RequiresPermissions("gsq:twiningDisc:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(GsqTwiningDisc queryVO) {
        return gsqTwiningDiscRemoteService.list(queryVO);
    }

    /**
     * 获取钢丝圈缠绕盘详情（含子表明细及钢丝圈名称反显）
     */
    @ApiOperation("获取钢丝圈缠绕盘详情")
    @GetMapping("/getInfo/{id}")
    @ResponseBody
    public GsqTwiningDisc getInfo(@PathVariable("id") Long id) {
        return gsqTwiningDiscRemoteService.getInfo(id);
    }

    /**
     * 保存钢丝圈缠绕盘（id为空新增，id不为空修改），级联保存子表明细
     * 保存前先校验缠绕盘编码唯一性
     * 注意：前端统一以multipart/form-data提交，此处不能用@RequestBody，需用表单绑定（与其他gsq模块一致）
     */
    @ApiOperation("保存钢丝圈缠绕盘")
    @RequiresPermissions("gsq:twiningDisc:edit")
    @PostMapping("/save")
    @ResponseBody
    public AjaxResult save(GsqTwiningDisc entity) {
        if (UserConstants.NOT_UNIQUE.equals(gsqTwiningDiscRemoteService.checkUnique(entity))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.gsq.twiningDisc.conflict"));
        }
        return gsqTwiningDiscRemoteService.save(entity);
    }

    /**
     * 删除钢丝圈缠绕盘（逻辑删除主表，级联逻辑删除子表）
     */
    @ApiOperation("删除钢丝圈缠绕盘")
    @RequiresPermissions("gsq:twiningDisc:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] idArray = Convert.toLongArray(ids);
        return gsqTwiningDiscRemoteService.removeByIds(Arrays.asList(idArray));
    }

    /**
     * 校验缠绕盘编码唯一性（表单绑定，不能用@RequestBody）
     */
    @ApiOperation("校验缠绕盘编码唯一性")
    @PostMapping("/checkUnique")
    @ResponseBody
    public String checkUnique(GsqTwiningDisc entity) {
        return gsqTwiningDiscRemoteService.checkUnique(entity);
    }

    @Override
    public String getExportTemplateFileName() {
        return getFunctionName();
    }

    @Override
    public String getProcedureCode() {
        return "GSQ";
    }

    @Override
    public String getFunctionName() {
        return I18nUtil.getMessage("ui.data.column.gsq.twiningDisc.modelName");
    }

    /**
     * 下载导入模板
     * <p>使用主子表平铺结构的导入VO生成模板：主表字段（缠绕盘编号/名称/状态/英寸/数量/主表备注）
     * + 子表字段（钢丝圈编号/名称/明细备注）在同一行填写，与导入解析保持一致</p>
     */
    @ApiOperation("下载导入模板")
    @Override
    public AjaxResult importTemplate(HttpServletResponse response) throws IOException {
        String fileName = getExportTemplateFileName();
        ExcelUtil<GsqTwiningDiscImportVo> util = new ExcelUtil<>(GsqTwiningDiscImportVo.class);
        util.exportExcel(response, null, fileName, fileName);
        return AjaxResult.success();
    }

    /**
     * 导出钢丝圈缠绕盘
     */
    @ApiOperation("导出钢丝圈缠绕盘")
    @RequiresPermissions("gsq:twiningDisc:export")
    @GetMapping("/export")
    @ResponseBody
    @Override
    public void export(HttpServletResponse response, GsqTwiningDisc entity) throws IOException {
        String fileName = getExportTemplateFileName();
        byte[] excelBytes = gsqTwiningDiscRemoteService.exportData(entity, fileName);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(excelBytes);
        ExcelUtil.setResponseHeader(response, fileName, ".xlsx");
        IOUtils.copy(inputStream, response.getOutputStream());
        response.flushBuffer();
    }

    /**
     * 导入钢丝圈缠绕盘
     */
    @ApiOperation("导入钢丝圈缠绕盘")
    @RequiresPermissions("gsq:twiningDisc:import")
    @PostMapping("/importData")
    @ResponseBody
    @Override
    public AjaxResult importData(@RequestPart("file") MultipartFile file, boolean updateSupport) throws Exception {
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();
        ImportContext context = new ImportContext();
        context.setImportFilePath(this.importFilePath);
        context.setFunctionName(getFunctionName());
        context.setProcedureCode(getProcedureCode());
        context.setOriFileName(file.getOriginalFilename());
        context.setFileBytes(data);
        return gsqTwiningDiscRemoteService.importData(context, updateSupport);
    }
}
