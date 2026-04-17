package com.zlt.aps.controller.lh;

import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.zlt.aps.lh.api.domain.entity.LhMouldCleanWarn;
import com.zlt.aps.lh.api.service.ILhMouldCleanWarnRemoteService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * 模具清洗预警UIController
 *
 * @author APS Team
 * @since 2026/04/10
 */
@Slf4j
@Api(tags = "模具清洗预警")
@Controller
@RequestMapping("/lh/mouldCleanWarn")
public class LhMouldCleanWarnUIController extends BaseUIController<LhMouldCleanWarn> {

    @Autowired
    private ILhMouldCleanWarnRemoteService iLhMouldCleanWarnService;

    @ApiOperation("查询列表")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(LhMouldCleanWarn query) {
        return iLhMouldCleanWarnService.list(query);
    }

    @ApiOperation("获取详细信息")
    @GetMapping("/{id}")
    @ResponseBody
    public Object getInfo(@PathVariable("id") Long id) {
        return iLhMouldCleanWarnService.getInfo(id);
    }

    @ApiOperation("导出数据")
    @GetMapping("/export")
    @RequiresPermissions("lh:mouldCleanWarn:export")
    public void export(HttpServletResponse response, LhMouldCleanWarn entity) throws IOException {
        String fileName = I18nUtil.getMessage("ui.data.column.mouldCleanWarn.modelName");
        byte[] excelBytes = iLhMouldCleanWarnService.exportData(entity, fileName);
        ByteArrayInputStream in = new ByteArrayInputStream(excelBytes);
        ExcelUtil.setResponseHeader(response, fileName, ".xlsx");
        IOUtils.copy(in, response.getOutputStream());
        response.flushBuffer();
    }
}
