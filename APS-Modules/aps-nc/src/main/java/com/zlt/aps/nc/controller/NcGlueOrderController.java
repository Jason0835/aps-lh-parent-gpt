package com.zlt.aps.nc.controller;

import java.io.IOException;
import java.util.List;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.nc.api.domain.dto.NcGlueOrderDto;
import com.zlt.aps.nc.api.domain.entity.NcGlueOrder;
import com.zlt.aps.nc.mapper.NcGlueOrderMapper;
import com.zlt.aps.nc.service.NcGlueOrderService;
import com.zlt.aps.utils.AppUtils;
import com.zlt.bill.common.controller.AbstractDocBizController;
import com.zlt.bill.common.service.IDocService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;

@Api(tags = {"内衬胶料顺序维护接口"})
@RestController
@RequestMapping("/nc/glueOrder")
public class NcGlueOrderController extends AbstractDocBizController<NcGlueOrder> {

    @Autowired
    private NcGlueOrderService glueOrderService;

    @Resource
    private NcGlueOrderMapper glueOrderMapper;

    @ApiOperation("根据条件查询胶料顺序列表")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody NcGlueOrder queryVO) {
        return super.list(queryVO);
    }

    @ApiOperation("保存胶料顺序信息（id为空则新增，id不为空则修改）")
    @Log(title = "ui.nc.glueOrder.column.modalName", businessType = BusinessType.INSERT_OR_UPDATE)
    @PostMapping("/save")
    public AjaxResult save(@RequestBody NcGlueOrder entity) {
        if (UserConstants.NOT_UNIQUE.equals(glueOrderService.checkUnique(entity))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.alert.djMachine.embryoCodeNotUnique"));
        }
        return super.save(entity);
    }

    @ApiOperation("根据code判断胶料是否已经存在")
    @PostMapping("/checkGlueCodeUnique")
    public String checkGlueCodeUnique(@RequestBody NcGlueOrder dto) {
        return glueOrderService.checkGlueCodeUnique(dto);
    }

    @ApiOperation("批量删除胶料顺序信息(逻辑删)")
    @Log(title = "ui.nc.glueOrder.column.modalName", businessType = BusinessType.DELETE)
    @PostMapping("/remove")
    public AjaxResult removeByIds(@RequestBody List<Long> ids) {
        return super.removeByIds(ids);
    }

    @ApiOperation("导出数据")
    @Log(title = "ui.nc.glueOrder.column.modalName", businessType = BusinessType.EXPORT)
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody NcGlueOrder queryVO, @PathVariable("fileName") String fileName,
            HttpServletResponse response) throws IOException {
        return super.exportData(queryVO, fileName, response);
    }

    @Override
    protected List<NcGlueOrder> listExportData(NcGlueOrder obj) {
        QueryWrapper<NcGlueOrder> wrapper = new QueryWrapper<>();
        startPage("update_time desc");
        this.builderCondition(wrapper, obj);
        List<NcGlueOrder> list = glueOrderMapper.selectList(wrapper);
        AppUtils.formatData(list, getQueryFormulas());
        return list;
    }

    @ApiOperation("导入内衬胶料顺序信息")
    @Log(title = "ui.nc.glueOrder.column.modalName", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    public AjaxResult importData(@RequestBody List<NcGlueOrder> list, @RequestParam("updateSupport") boolean updateSupport,
            @RequestParam("importLogId") Long importLogId) {
        if (StringUtils.isNull(list) || list.size() == 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        return glueOrderService.importData(list, updateSupport, importLogId);
    }

    @Override
    @SuppressWarnings("rawtypes")
    protected IDocService getDocService() {
        return glueOrderService;
    }

    @Override
    protected String getTypeCode() {
        return "0";
    }

    @Override
    protected String getOrderBy() {
        return "GLUE_CODE";
    }

    @Override
    protected String[] getQueryFormulas() {
        return new String[]{
                "glueGroupCode,glueGroupName,glueGroupOrderNum->getcolsvalue(T_NC_GLUE_GROUP_ORDER, [GLUE_GROUP_CODE, GLUE_GROUP_NAME, ORDER_NUM], ID, glueGroupId)"
        };
    }
}
