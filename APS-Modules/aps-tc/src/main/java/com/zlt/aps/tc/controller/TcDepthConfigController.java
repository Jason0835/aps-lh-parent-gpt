package com.zlt.aps.tc.controller;

import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.tc.api.domain.entity.TcDepthConfig;
import com.zlt.aps.tc.mapper.TcDepthConfigMapper;
import com.zlt.aps.tc.service.ITcDepthConfigService;
import com.zlt.bill.common.controller.AbstractDocBizController;
import com.zlt.bill.common.service.IDocService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 胎侧备库班数配置Controller
 *
 * @author zlt
 */
@Api(tags = {"胎侧备库班数配置接口"})
@RestController
@RequestMapping("/depthConfig")
public class TcDepthConfigController extends AbstractDocBizController<TcDepthConfig> {

    @Autowired
    private ITcDepthConfigService depthConfigService;

    @Resource
    private TcDepthConfigMapper depthConfigMapper;

    /**
     * 查询信息列表
     */
    @PostMapping("/list")
    @ApiOperation("根据条件查询列表信息")
    public TableDataInfo list(@RequestBody TcDepthConfig queryVO) {
        return super.list(queryVO);
    }

    /**
     * 保存信息（新增/修改）
     */
    @ApiOperation("保存信息（id为空新增，id不为空修改）")
    @PostMapping("/save")
    public AjaxResult save(@RequestBody TcDepthConfig entity) {
        // 校验业务唯一约束（同一工厂下同台数同范围条件只能有一条）
        if (UserConstants.NOT_UNIQUE.equals(depthConfigService.checkUnique(entity))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.tc.depthConfig.notUnique"));
        }
        // 校验范围交叉（新增/修改的规则不能与现有规则有交集）
        if (UserConstants.NOT_UNIQUE.equals(depthConfigService.checkRangeCross(entity))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.tc.depthConfig.rangeCross"));
        }
        return super.save(entity);
    }

    /**
     * 校验唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/checkUnique")
    public String checkUnique(@RequestBody TcDepthConfig entity) {
        return depthConfigService.checkUnique(entity);
    }

    /**
     * 校验范围交叉
     */
    @ApiOperation("校验范围交叉")
    @PostMapping("/checkRangeCross")
    public String checkRangeCross(@RequestBody TcDepthConfig entity) {
        return depthConfigService.checkRangeCross(entity);
    }

    @Override
    @SuppressWarnings("rawtypes")
    protected IDocService getDocService() {
        return depthConfigService;
    }

    @Override
    protected String getTypeCode() {
        return "TC0916";
    }

    @Override
    protected String getOrderBy() {
        return "MACHINE_QTY desc";
    }
}