package com.zlt.aps.controller.lh;

import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.zlt.aps.lh.api.domain.entity.LhMachineOnlineInfo;
import com.zlt.aps.lh.api.service.ILhMachineOnlineInfoRemoteService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * 硫化在机信息 UI Controller
 *
 * @author APS Team
 * @date 2026-04-17
 */
@Slf4j
@Api(tags = "硫化在机信息")
@Controller
@RequestMapping("/lh/lhMachineOnlineInfo")
public class LhMachineOnlineInfoUIController extends BaseUIController<LhMachineOnlineInfo> {

    @Autowired
    private ILhMachineOnlineInfoRemoteService iLhMachineOnlineInfoService;

    @ApiOperation("查询列表")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(LhMachineOnlineInfo query) {
        return iLhMachineOnlineInfoService.list(query);
    }

    @ApiOperation("获取详情信息")
    @GetMapping("/{id}")
    @ResponseBody
    public Object getInfo(@PathVariable("id") Long id) {
        return iLhMachineOnlineInfoService.getInfo(id);
    }

    @ApiOperation("导出数据")
    @GetMapping("/export")
    public void export(HttpServletResponse response, LhMachineOnlineInfo entity) throws IOException {
        String fileName = I18nUtil.getMessage("ui.data.column.lhMachineOnlineInfo.modelName");
        byte[] excelBytes = iLhMachineOnlineInfoService.exportData(entity, fileName);
        ByteArrayInputStream in = new ByteArrayInputStream(excelBytes);
        ExcelUtil.setResponseHeader(response, fileName, ".xlsx");
        IOUtils.copy(in, response.getOutputStream());
        response.flushBuffer();
    }
}

