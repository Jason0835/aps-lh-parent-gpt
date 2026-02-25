package com.zlt.aps.controller.i18n;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.mp.api.domain.entity.I18nChange;
import com.zlt.aps.mp.api.domain.vo.I18nJsonVo;
import com.zlt.aps.mp.api.service.I18nChangeRemoteService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Slf4j
@Api(tags = "国际化变更")
@Controller
@RequestMapping("/bd/i18nChange")
public class I18nChangeUIController {
    @Autowired
    private I18nChangeRemoteService i18nChangeRemoteService;

    /**
     * 根据条件查询主表数据
     */
    @ApiOperation(value = "查询", notes = "根据条件查询国际化变更")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(I18nChange query) {
        return i18nChangeRemoteService.list(query);
    }

    /**
     * 通过id取当相应对象数据
     */
    @ApiOperation("通过id取当相应对象数据")
    @GetMapping("/{id}")
    @ResponseBody
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return AjaxResult.success("", i18nChangeRemoteService.getInfo(id));
    }

    /**
     * 保存国际化变更
     */
    @ApiOperation(value = "保存", notes = "保存国际化变更")
    @PostMapping("/save")
    @RequiresPermissions("system:language:edit")
    @ResponseBody
    public AjaxResult save(I18nChange i18nChange) {
        return i18nChangeRemoteService.save(i18nChange);
    }

    /**
     * 查询语言包JSON
     */
    @ApiOperation(value = "查询语言包JSON", notes = "查询语言包JSON")
    @PostMapping("/pageJson")
    @ResponseBody
    public AjaxResult pageJson(I18nJsonVo jsonVo) {
        return i18nChangeRemoteService.pageJson(jsonVo);
    }

    /**
     * 下载国际化语言包
     */
    @ApiOperation(value = "下载国际化语言包", notes = "下载国际化语言包")
    @GetMapping("/download")
    @RequiresPermissions("system:language:export")
    public void download(HttpServletResponse response) throws IOException {
        byte[] data = i18nChangeRemoteService.download();
        response.reset();
        response.setHeader("Content-Disposition", "attachment; filename=\"i18n.zip\"");
        response.addHeader("Content-Length", "" + data.length);
        response.setContentType("application/octet-stream; charset=UTF-8");
        IOUtils.write(data, response.getOutputStream());
    }

}
