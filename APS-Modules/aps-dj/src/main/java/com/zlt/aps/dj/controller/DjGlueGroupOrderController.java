package com.zlt.aps.dj.controller;

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
import com.zlt.aps.dj.api.domain.dto.DjGlueGroupOrderDto;
import com.zlt.aps.dj.api.domain.entity.DjGlueGroupOrder;
import com.zlt.aps.dj.mapper.DjGlueGroupOrderMapper;
import com.zlt.aps.dj.service.DjGlueGroupOrderService;
import com.zlt.aps.utils.AppUtils;
import com.zlt.bill.common.controller.AbstractDocBizController;
import com.zlt.bill.common.service.IDocService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;

/**
 * <p>
 * 垫胶胶料组别顺序维护 前端控制器
 * </p>
 *
 * @author zhangbinglin
 */
@Api(tags = {"垫胶胶料组别顺序维护接口"})
@RestController
@RequestMapping("/dj/glueGroupOrder")
public class DjGlueGroupOrderController extends AbstractDocBizController<DjGlueGroupOrder> {

    @Autowired
    private DjGlueGroupOrderService glueGroupOrderService;

    @Resource
    private DjGlueGroupOrderMapper glueGroupOrderMapper;

    @ApiOperation("根据条件查询胶料组别顺序列表")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody DjGlueGroupOrder queryVO) {
        return super.list(queryVO);
    }

    @ApiOperation("根据code判断胶料组号是否已经存在")
    @PostMapping("/checkGlueGroupCodeUnique")
    public String checkGlueGroupCodeUnique(@RequestBody DjGlueGroupOrderDto dto) {
        return glueGroupOrderService.checkGlueGroupCodeUnique(dto);
    }

    @ApiOperation("保存胶料组别顺序信息（id为空则新增，id不为空则修改）")
    @Log(title = "ui.dj.glueGroupOrder.column.modalName", businessType = BusinessType.INSERT_OR_UPDATE)
    @PostMapping("/save")
    public AjaxResult save(@RequestBody DjGlueGroupOrder entity) {
        if (UserConstants.NOT_UNIQUE.equals(glueGroupOrderService.checkUnique(entity))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.error.message.quota.unique"));
        }
        return super.save(entity);
    }

    @ApiOperation("批量删除胶料组别顺序信息(逻辑删)")
    @Log(title = "ui.dj.glueGroupOrder.column.modalName", businessType = BusinessType.DELETE)
    @PostMapping("/remove")
    public AjaxResult removeByIds(@RequestBody List<Long> ids) {
        return super.removeByIds(ids);
    }

    @ApiOperation("导出数据")
    @Log(title = "ui.dj.glueGroupOrder.column.modalName", businessType = BusinessType.EXPORT)
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody DjGlueGroupOrder queryVO, @PathVariable("fileName") String fileName,
            HttpServletResponse response) throws IOException {
        return super.exportData(queryVO, fileName, response);
    }

    @Override
    protected List<DjGlueGroupOrder> listExportData(DjGlueGroupOrder obj) {
        QueryWrapper<DjGlueGroupOrder> wrapper = new QueryWrapper<>();
        startPage("update_time desc");
        this.builderCondition(wrapper, obj);
        List<DjGlueGroupOrder> list = glueGroupOrderMapper.selectList(wrapper);
        AppUtils.formatData(list, getQueryFormulas());
        return list;
    }

    @ApiOperation("导入数据")
    @Log(title = "ui.dj.glueGroupOrder.column.modalName", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    public AjaxResult importData(@RequestBody List<DjGlueGroupOrder> list, @RequestParam("updateSupport") boolean updateSupport,
            @RequestParam("importLogId") Long importLogId) {
        if (StringUtils.isNull(list) || list.size() == 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        return glueGroupOrderService.importData(list, updateSupport, importLogId);
    }

    @Override
    @SuppressWarnings("rawtypes")
    protected IDocService getDocService() {
        return glueGroupOrderService;
    }

    @Override
    protected String getTypeCode() {
        return "0";
    }

    @Override
    protected String getOrderBy() {
        return "GLUE_GROUP_CODE";
    }
}
