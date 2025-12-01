package com.zlt.aps.controller.maindata;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common4ui.constant.UserConstants;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.zlt.aps.monthplan.api.domain.entity.FactoryParam;
import com.zlt.aps.monthplan.api.domain.vo.FactoryParamVo;
import com.zlt.aps.monthplan.api.service.IFactoryParamRemoteService;
import com.zlt.file.encryptbyll.FileEncryptUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：FactoryParamUIController.java
 * 描    述：系统参数（排产设定） UI控制层类：....
 *
 * @author ZLT
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：ZLT
 * 修改内容：...
 * @date 2025-02-20
 */
@Slf4j
@Api(tags = "系统参数（排产设定）")
@Controller
@RequestMapping("/monthplan/factoryParam")
public class FactoryParamUIController extends BaseUIController<FactoryParam> {

    private final IFactoryParamRemoteService iFactoryParamService;

    public FactoryParamUIController(IFactoryParamRemoteService iFactoryParamService) {
        this.iFactoryParamService = iFactoryParamService;
    }


    /**
     * 根据条件查询主表数据
     */
    @ApiOperation("根据条件查询主表数据")
    @RequiresPermissions("monthplan:factoryParam:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(FactoryParam factoryParam) {
        return iFactoryParamService.list(factoryParam);
    }

    /**
     * 修改或新增分厂排产设定
     */
    @ApiOperation("修改或新增分厂排产设定")
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult editSave(FactoryParam factoryParam) {
        AjaxResult ajaxResult = null;
        if (factoryParam.getId() != null) {
            ajaxResult = iFactoryParamService.edit(factoryParam);
        } else {
            return AjaxResult.error();
        }
        return ajaxResult;
    }

    /**
     * 复制分厂排产设定
     */
    @ApiOperation("复制分厂排产设定")
    @RequiresPermissions("fac:docFactoryParam:copy")
    @PostMapping("/copy")
    @ResponseBody
    public AjaxResult copy(FactoryParamVo vo) {
        return iFactoryParamService.copy(vo);
    }
}
