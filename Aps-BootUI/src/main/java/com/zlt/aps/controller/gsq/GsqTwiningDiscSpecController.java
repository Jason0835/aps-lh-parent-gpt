package com.zlt.aps.controller.gsq;

import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common4ui.constant.UserConstants;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.zlt.aps.gsq.api.domain.entity.GsqTwiningDiscSpec;
import com.zlt.aps.gsq.api.service.IGsqTwiningDiscSpecService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.io.IOUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;

/**
 * 钢丝圈缠绕盘-规格关系管理 UI 控制层
 * <p>独立菜单页面：维护缠绕盘与钢丝圈规格的对应关系（多对多，MES同步+手工维护），
 * 数据表T_GSQ_TWINING_DISC_SPEC（与机台关系表对称，统一TWINING_DISC_CODE编码关联）</p>
 *
 * @author zlt
 * @date 2026-08-21
 */
@Api(tags = "钢丝圈缠绕盘-规格关系管理")
@Controller
@RequestMapping("/gsq/discSpec")
public class GsqTwiningDiscSpecController extends BaseUIController<GsqTwiningDiscSpec> {

    @Resource
    private IGsqTwiningDiscSpecService gsqTwiningDiscSpecRemoteService;

    /**
     * 查询缠绕盘-规格关系列表
     */
    @ApiOperation("查询缠绕盘-规格关系列表")
    @RequiresPermissions("gsq:discSpec:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(GsqTwiningDiscSpec queryVO) {
        return gsqTwiningDiscSpecRemoteService.list(queryVO);
    }

    /**
     * 获取缠绕盘-规格关系详细信息
     */
    @ApiOperation("获取缠绕盘-规格关系详细信息")
    @GetMapping("/getInfo/{id}")
    @ResponseBody
    public GsqTwiningDiscSpec getInfo(@PathVariable("id") Long id) {
        return gsqTwiningDiscSpecRemoteService.getInfo(id);
    }

    /**
     * 保存缠绕盘-规格关系（id为空新增，id不为空修改）
     * 注意：前端统一以multipart/form-data提交，此处不能用@RequestBody，需用表单绑定（与其他gsq模块一致）
     */
    @ApiOperation("保存缠绕盘-规格关系")
    @RequiresPermissions("gsq:discSpec:edit")
    @PostMapping("/save")
    @ResponseBody
    public AjaxResult save(GsqTwiningDiscSpec entity) {
        // 组合唯一性前置校验（缠绕盘存在性/钢丝圈存在性由后端saveWithCheck统一校验）
        if (UserConstants.NOT_UNIQUE.equals(gsqTwiningDiscSpecRemoteService.checkUnique(entity))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.gsq.discSpec.conflict"));
        }
        return gsqTwiningDiscSpecRemoteService.save(entity);
    }

    /**
     * 删除缠绕盘-规格关系（逻辑删除）
     */
    @ApiOperation("删除缠绕盘-规格关系")
    @RequiresPermissions("gsq:discSpec:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] idArray = Convert.toLongArray(ids);
        return gsqTwiningDiscSpecRemoteService.removeByIds(Arrays.asList(idArray));
    }

    /**
     * 校验缠绕盘+钢丝圈规格组合唯一性（表单绑定，不能用@RequestBody）
     */
    @ApiOperation("校验缠绕盘+钢丝圈规格组合唯一性")
    @PostMapping("/checkUnique")
    @ResponseBody
    public String checkUnique(GsqTwiningDiscSpec entity) {
        return gsqTwiningDiscSpecRemoteService.checkUnique(entity);
    }

    /**
     * 查询施工信息表全部钢丝圈选项（编码+名称，去重），供规格关系页面下拉选择使用
     */
    @ApiOperation("查询钢丝圈下拉选项")
    @GetMapping("/listSteelRingOptions")
    @ResponseBody
    public AjaxResult listSteelRingOptions() {
        return gsqTwiningDiscSpecRemoteService.listSteelRingOptions();
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
        return I18nUtil.getMessage("ui.data.column.gsq.discSpec.modelName");
    }

    /**
     * 导出缠绕盘-规格关系
     */
    @ApiOperation("导出缠绕盘-规格关系")
    @RequiresPermissions("gsq:discSpec:export")
    @GetMapping("/export")
    @ResponseBody
    @Override
    public void export(HttpServletResponse response, GsqTwiningDiscSpec entity) throws IOException {
        String fileName = getExportTemplateFileName();
        byte[] excelBytes = gsqTwiningDiscSpecRemoteService.exportData(entity, fileName);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(excelBytes);
        ExcelUtil.setResponseHeader(response, fileName, ".xlsx");
        IOUtils.copy(inputStream, response.getOutputStream());
        response.flushBuffer();
    }
}
