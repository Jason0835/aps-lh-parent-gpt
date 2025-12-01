package com.zlt.aps.monthplan.mdm.controller;

import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.utils.StringUtils;
import com.tlt.aps.enums.SysParamDataTypeEnum;
import com.tlt.aps.exception.BusinessException;
import com.zlt.aps.maindata.service.IFactoryParamService;
import com.zlt.aps.monthplan.api.domain.entity.FactoryParam;
import com.zlt.aps.monthplan.api.domain.vo.FactoryParamVo;
import com.zlt.common.utils.PubUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：FactoryParamController.java
 * 描    述：系统参数（排产设定） 控制层类：....
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
@RestController
@RequestMapping("/factoryParam")
public class FactoryParamController extends BaseController<FactoryParam> {

    private final IFactoryParamService factoryParamService;

    public FactoryParamController(IFactoryParamService factoryParamService) {
        this.factoryParamService = factoryParamService;
    }

    /**
     * 查询分厂排产设定列表
     */
    @ApiOperation("查询分厂排产设定列表`")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody FactoryParam entity) {
        startPage("FACTORY_CODE,BUSINESS_GROUP,PARAM_CODE asc");
        List<FactoryParam> list = factoryParamService.getFacParamByList(entity);
        return getDataTable(list);
    }


    /**
     * 修改分厂排产设定
     */
    @Log(title = "ui.data.column.docFactoryParam.modelName", businessType = BusinessType.UPDATE)
//    @PreAuthorize(hasPermi = "fac:docFactoryParam:edit")
    @ApiOperation("修改分厂排产设定")
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody FactoryParam entity) {
        this.checkValidParams(entity);
        return toAjax(factoryParamService.updateById(entity));
    }

    /**
     * 复制分厂排产设定
     */
    @Log(title = "ui.data.column.docFactoryParam.modelName", businessType = BusinessType.OTHER)
    @ApiOperation("复制分厂排产设定")
//    @PreAuthorize(hasPermi = "fac:docFactoryParam:copy")
    @PostMapping("/copy")
    @ResponseBody
    public AjaxResult copy(@RequestBody FactoryParamVo factoryParamVo) {
        return factoryParamService.copy(factoryParamVo);
    }

    private void checkValidParams(@RequestBody FactoryParam entity) {
        SysParamDataTypeEnum sysParamDataTypeEnum = SysParamDataTypeEnum.getEnumByValue(entity.getDataType().intValue());
        if (SysParamDataTypeEnum.NUMBER.equals(sysParamDataTypeEnum)) {
            try {
                new BigDecimal(entity.getParamValue());
            } catch (NumberFormatException e) {
                throw new BusinessException(String.format("系统参数【1$s %2$s】解析参数值错误.", entity.getParamValue(), entity.getParamName()));
            }
        } else if (SysParamDataTypeEnum.INTEGER.equals(sysParamDataTypeEnum)) {

            try {
                Integer.parseInt(entity.getParamValue());
            } catch (NumberFormatException e) {
                throw new BusinessException(String.format("系统参数【1$s %2$s】解析参数值错误.", entity.getParamValue(), entity.getParamName()));
            }
        } else if (SysParamDataTypeEnum.BOOLEAN.equals(sysParamDataTypeEnum)) {
            try {
                PubUtil.isTrue(entity.getParamValue());
            } catch (NumberFormatException e) {
                throw new BusinessException(String.format("系统参数【1$s %2$s】解析参数值错误.", entity.getParamValue(), entity.getParamName()));
            }
        } else if (SysParamDataTypeEnum.DATE.equals(sysParamDataTypeEnum)) {
            try {
                DateUtils.parseDate(entity.getParamValue(), DateUtils.YYYY_MM_DD);
            } catch (NumberFormatException | ParseException e) {
                throw new BusinessException(String.format("系统参数【1$s %2$s】解析参数值错误.", entity.getParamValue(), entity.getParamName()));
            }
        } else if (SysParamDataTypeEnum.CUSTOM.equals(sysParamDataTypeEnum)) {
            if (!entity.getParamValue().matches("^\\w+:\\w+$")) {
                throw new BusinessException(String.format("系统参数【%s %s】格式应为x:y.", entity.getParamValue(), entity.getParamName()));
            }
        } else if (SysParamDataTypeEnum.STRING.equals(sysParamDataTypeEnum)) {
            //20251021 ZLT 字符类型允许为空
            return ;
        } else {
            throw new BusinessException(String.format("系统参数【%1$s %2$s】数据类型不对.", entity.getParamCode(), entity.getParamName()));
        }
    }



}
