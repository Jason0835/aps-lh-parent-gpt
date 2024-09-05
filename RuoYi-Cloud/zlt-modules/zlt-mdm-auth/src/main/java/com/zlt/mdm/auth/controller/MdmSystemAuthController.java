package com.zlt.mdm.auth.controller;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.mdm.auth.api.domain.MdmSystemData;
import com.zlt.mdm.auth.api.domain.vo.UserSystemVo;
import com.zlt.mdm.auth.service.IMdmSystemAuthService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 系统权限查询的接口
 *
 * @author lbn 20201203
 */
@Api("用户系统权限管理")
@RestController
@RequestMapping("/mdm/api")
public class MdmSystemAuthController extends BaseController {

    @Autowired
    IMdmSystemAuthService iMdmSystemAuthService;

    @ApiOperation("获取用户被授予的系统权限")
    @ApiImplicitParam(name = "userId", value = "用户ID", required = true, dataType = "Long")
    @GetMapping("/userSystemAuth/{userId}")
    public AjaxResult getUserSystemAuth(@PathVariable("userId") Long userId) {
        UserSystemVo vo = iMdmSystemAuthService.selectSystemDataByUserId(userId);
        return AjaxResult.success(vo);
    }

    @ApiOperation("获取所有的系统配置")
    @GetMapping("/list")
    public AjaxResult getSystemDataList() {
        List<MdmSystemData> data = iMdmSystemAuthService.selectSystemDataList();
        return AjaxResult.success(data);
    }
}
