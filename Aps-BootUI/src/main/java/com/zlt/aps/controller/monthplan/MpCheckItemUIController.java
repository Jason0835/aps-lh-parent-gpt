package com.zlt.aps.controller.monthplan;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common4ui.constant.UserConstants;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.ruoyi.common4ui.exception.base.BaseException;
import com.zlt.aps.monthplan.api.domain.entity.MpCheckItemRecord;
import com.zlt.aps.monthplan.api.service.IMpCheckItemRecordRemoteService;
import com.zlt.aps.monthplan.api.service.IMpCheckItemRemoteService;
import com.zlt.file.encryptbyll.FileEncryptUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MpCheckItemUIController.java
 * 描    述：检测项检测 UI控制层类：....
 *
 * @author hsc
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：hsc
 * 修改内容：...
 * @date 2026-01-29
 */
@Slf4j
@Api(tags = "检测项检测")
@Controller
@RequestMapping("/monthplan/checkItem")
public class MpCheckItemUIController extends BaseUIController {

    private IMpCheckItemRemoteService iMpCheckItemRemoteService;


}
