package com.zlt.aps.tm.controller;

import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.tm.api.domain.entity.TmStockCoverClass;
import com.zlt.aps.tm.mapper.TmStockCoverClassMapper;
import com.zlt.aps.tm.service.ITmStockCoverClassService;
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
 * 备库班数配置Controller
 *
 * @author zlt
 */
@Api(tags = {"备库班数配置接口"})
@RestController
@RequestMapping("/tmStockCoverClass")
public class TmStockCoverClassController extends AbstractDocBizController<TmStockCoverClass> {

    @Autowired
    private ITmStockCoverClassService tmStockCoverClassService;

    @Resource
    private TmStockCoverClassMapper tmStockCoverClassMapper;

    /**
     * 查询信息列表
     */
    @PostMapping("/list")
    @ApiOperation("根据条件查询列表信息")
    public TableDataInfo list(@RequestBody TmStockCoverClass queryVO) {
        return super.list(queryVO);
    }

    /**
     * 保存信息（新增/修改）
     */
    @ApiOperation("保存信息（id为空新增，id不为空修改）")
    @PostMapping("/save")
    public AjaxResult save(@RequestBody TmStockCoverClass entity) {
        // 校验业务唯一约束（同一工厂下同台数同范围条件只能有一条）
        if (UserConstants.NOT_UNIQUE.equals(tmStockCoverClassService.checkUnique(entity))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.tm.stockCoverClass.notUnique"));
        }
        // 校验范围交叉（新增/修改的规则不能与现有规则有交集）
        if (UserConstants.NOT_UNIQUE.equals(tmStockCoverClassService.checkRangeCross(entity))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.tm.stockCoverClass.rangeCross"));
        }
        return super.save(entity);
    }

    /**
     * 校验唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/checkUnique")
    public String checkUnique(@RequestBody TmStockCoverClass entity) {
        return tmStockCoverClassService.checkUnique(entity);
    }

    /**
     * 校验范围交叉
     */
    @ApiOperation("校验范围交叉")
    @PostMapping("/checkRangeCross")
    public String checkRangeCross(@RequestBody TmStockCoverClass entity) {
        return tmStockCoverClassService.checkRangeCross(entity);
    }

    @Override
    @SuppressWarnings("rawtypes")
    protected IDocService getDocService() {
        return tmStockCoverClassService;
    }

    @Override
    protected String getTypeCode() {
        return "TM0816";
    }

    @Override
    protected String getOrderBy() {
        return "MACHINE_QTY desc";
    }
}
