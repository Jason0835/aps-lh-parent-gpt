package com.zlt.aps.controller.monthplan;

import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common4ui.constant.UserConstants;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.zlt.aps.monthplan.api.domain.entity.MdmPersonLevel;
import com.zlt.aps.monthplan.api.service.IMdmPersonLevelService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;

import lombok.extern.slf4j.Slf4j;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.zlt.file.encryptbyll.FileEncryptUtils;
import org.apache.commons.io.IOUtils;

import java.util.Arrays;
import java.util.List;
import java.io.IOException;
import java.io.ByteArrayInputStream;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MdmPersonLevelController.java
 * 描    述：成型机人员档配置 UI控制层类：....
 *
 * @author hsc
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：hsc
 * 修改内容：...
 * @date 2025-02-19
 */
@Slf4j
@Api(tags = "成型机人员档配置")
@Controller
@RequestMapping("/monthplan/mdmPersonLevel")
public class MdmPersonLevelController extends BaseUIController<MdmPersonLevel> {

    @Autowired
    private IMdmPersonLevelService iMdmPersonLevelService;


    /**
     * 根据条件查询主表数据
     */
    @ApiOperation("根据条件查询主表数据")
    @RequiresPermissions("monthplan:mdmPersonLevel:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(MdmPersonLevel mdmPersonLevel) {
        return iMdmPersonLevelService.list(mdmPersonLevel);
    }

    @ApiOperation("根据id查询主表数据")
    @RequiresPermissions("monthplan:mdmPersonLevel:getInfo")
    @GetMapping("/{id}")
    @ResponseBody
    public MdmPersonLevel getInfo(@PathVariable("id") Long id) {
        return iMdmPersonLevelService.getInfo(id);
    }

    /**
     * 修改或新增
     */
    @ApiOperation("修改或新增")
    @RequiresPermissions("monthplan:mdmPersonLevel:edit")
    @PostMapping("/saveMdmPersonLevel")
    @ResponseBody
    public AjaxResult saveMdmPersonLevel(MdmPersonLevel mdmPersonLevel) {
        AjaxResult ajaxResult = null;
        if (UserConstants.NOT_UNIQUE.equals(iMdmPersonLevelService.checkUnique(mdmPersonLevel))) {
            return ajaxResult.error(I18nUtil.getMessage("ui.data.column.mdmPersonLevel.checkUnique"));
        }

        return iMdmPersonLevelService.save(mdmPersonLevel);
    }

    /**
     * 删除成型机人员档配置
     */
    @ApiOperation("删除,id不为空）")
    @RequiresPermissions("monthplan:mdmPersonLevel:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iMdmPersonLevelService.remove(arr);
    }

    /**
     * 校验成型机人员档配置唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/checkUnique")
    @ResponseBody
    public String checkUnique(MdmPersonLevel mdmPersonLevel) {
        return iMdmPersonLevelService.checkUnique(mdmPersonLevel);
    }

    @ApiOperation("更新成型机人员档配置")
    @PutMapping("/updateMdmPersonLevel")
    @ResponseBody
    public AjaxResult updateMdmPersonLevel(MdmPersonLevel mdmPersonLevel) {
        return iMdmPersonLevelService.updateMdmPersonLevel(mdmPersonLevel);
    }
}
