package com.zlt.aps.controller.lh;

import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.lh.api.domain.entity.LhRepairCapsule;
import com.zlt.aps.lh.api.service.ILhRepairCapsuleRemoteService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;

@Slf4j
@Api(tags = "APS胶囊已使用次数")
@Controller
@RequestMapping("/lh/lhRepairCapsule")
public class LhRepairCapsuleUIController {

    @Autowired
    private ILhRepairCapsuleRemoteService iLhRepairCapsuleService;

    @ApiOperation("根据条件查询数据")
    @RequiresPermissions("lh:lhRepairCapsule:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(LhRepairCapsule query) {
        return iLhRepairCapsuleService.list(query);
    }

    @ApiOperation("获取详细信息")
    @RequiresPermissions("lh:lhRepairCapsule:list")
    @GetMapping("/getInfo/{id}")
    @ResponseBody
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return AjaxResult.success(iLhRepairCapsuleService.getInfo(id));
    }

    @ApiOperation("数据导出")
    @RequiresPermissions("lh:lhRepairCapsule:export")
    @GetMapping({"/export"})
    @ResponseBody
    public void export(HttpServletResponse response, LhRepairCapsule entity) throws IOException {
        String fileName = I18nUtil.getMessage("ui.data.column.lhRepairCapsule.modelName");
        byte[] excelBytes = iLhRepairCapsuleService.exportData(entity, fileName);
        ByteArrayInputStream in = new ByteArrayInputStream(excelBytes);
        ExcelUtil.setResponseHeader(response, fileName, ".xlsx");
        IOUtils.copy(in, response.getOutputStream());
        response.flushBuffer();
    }
}

