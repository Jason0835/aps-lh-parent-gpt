package com.zlt.aps.tm.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.tm.api.domain.entity.TmDepthConfig;
import com.zlt.aps.tm.mapper.TmDepthConfigMapper;
import com.zlt.aps.tm.service.ITmDepthConfigService;
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
import java.util.List;

/**
 * 备库班数配置Controller
 *
 * @author zlt
 */
@Api(tags = {"备库班数配置接口"})
@RestController
@RequestMapping("/depthConfig")
public class TmDepthConfigController extends AbstractDocBizController<TmDepthConfig> {

    @Autowired
    private ITmDepthConfigService depthConfigService;

    @Resource
    private TmDepthConfigMapper tmDepthConfigMapper;

    /**
     * 查询信息列表
     */
    @PostMapping("/list")
    @ApiOperation("根据条件查询列表信息")
    public TableDataInfo list(@RequestBody TmDepthConfig queryVO) {
        return super.list(queryVO);
    }

    /**
     * 保存信息（新增/修改）
     */
    @ApiOperation("保存信息（id为空新增，id不为空修改）")
    @PostMapping("/save")
    public AjaxResult save(@RequestBody TmDepthConfig entity) {
        // 统一校验区间字段、连续性和完整性
        if (UserConstants.NOT_UNIQUE.equals(depthConfigService.checkRangeCross(entity))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.tm.depthConfig.rangeCross"));
        }
        return super.save(entity);
    }

    /**
     * 兼容旧调用路径，执行连续区间校验。
     */
    @ApiOperation("兼容旧调用路径校验连续区间")
    @PostMapping("/checkUnique")
    public String checkUnique(@RequestBody TmDepthConfig entity) {
        return depthConfigService.checkRangeCross(entity);
    }

    /**
     * 校验范围交叉
     */
    @ApiOperation("校验范围交叉")
    @PostMapping("/checkRangeCross")
    public String checkRangeCross(@RequestBody TmDepthConfig entity) {
        return depthConfigService.checkRangeCross(entity);
    }

    /**
     * 查询备库班数配置导出数据。
     *
     * @param obj 导出筛选条件
     * @return 按页面排序口径排列的导出数据
     */
    @Override
    protected List<TmDepthConfig> listExportData(TmDepthConfig obj) {
        QueryWrapper<TmDepthConfig> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, obj);
        wrapper.last("ORDER BY " + this.getOrderBy(obj));
        return tmDepthConfigMapper.selectList(wrapper);
    }

    @Override
    @SuppressWarnings("rawtypes")
    protected IDocService getDocService() {
        return depthConfigService;
    }

    @Override
    protected String getTypeCode() {
        return "TM0816";
    }

    @Override
    protected String getOrderBy() {
        return "MIN_MACHINE_QTY";
    }
}
