package com.zlt.aps.gdyy.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.gdyy.api.domain.dto.GdyyParamsDto;
import com.zlt.aps.gdyy.entity.GdyyParams;
import com.zlt.aps.gdyy.mapper.GdyyParamsMapper;
import com.zlt.aps.gdyy.service.GdyyParamsService;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Pattern;

/**
 * 钢带压延参数信息Service业务层处理
 *
 * @author zlt
 * @date 2021-05-25
 */
@Service
public class GdyyParamsServiceImpl extends ServiceImpl<GdyyParamsMapper, GdyyParams> implements GdyyParamsService {
    @Autowired
    private GdyyParamsMapper gdyyParamsMapper;

    /**
     * 查询钢带压延参数信息
     *
     * @param id 钢带压延参数信息ID
     * @return 钢带压延参数信息
     */
    @Override
    public GdyyParams selectParamsById(Long id) {
        LambdaQueryWrapper<GdyyParams> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GdyyParams::getDelFlag, ApsConstant.DEL_FLAG_NORMAL);
        wrapper.eq(GdyyParams::getId, id);
        return gdyyParamsMapper.selectOne(wrapper);
    }

    /**
     * 查询钢带压延参数信息列表
     *
     * @param params 钢带压延参数信息
     * @return 钢带压延参数信息
     */
    @Override
    public List<GdyyParamsDto> selectParamsList(GdyyParams params) {
        return gdyyParamsMapper.listParams(params);
    }

    /**
     * 修改钢带压延参数信息
     *
     * @param params 钢带压延参数信息
     * @return 结果
     */
    @Override
    public AjaxResult updateParams(GdyyParams params) {
        //基本参数校验
        if (!ObjectUtils.allNotNull(params.getId(), params.getParamCode(), params.getParamName(), params.getParamValue())) {
            return AjaxResult.error(I18nUtil.getMessage("ui.params.message.invalidParameter"));
        }
        String unique = checkParamsCodeUnique(params);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.params.message.unique"));
        }
        //校验正则表达式
        boolean regularResult = true;
        String regularExpression = params.getRegularExpression();
        if (StringUtils.isNotEmpty(regularExpression)) {
            Pattern pattern = Pattern.compile(regularExpression);
            regularResult = pattern.matcher(params.getParamValue()).matches();
        }
        if (regularResult) {
            params.setBaseVale(params.getId());
            gdyyParamsMapper.updateById(params);
        }
        return regularResult ? AjaxResult.success() : AjaxResult.error(params.getErrorTips());
    }

    /**
     * 校验参数代码唯一性
     *
     * @param params 钢带压延参数信息
     * @return 是否唯一
     */
    @Override
    public String checkParamsCodeUnique(GdyyParams params) {
        //校验参数代码字段唯一性
        Long paramsId = StringUtils.isNull(params.getId()) ? -1L : params.getId();
        GdyyParams info = gdyyParamsMapper.checkParamsCodeUnique(params.getParamCode(), paramsId);
        if (StringUtils.isNotNull(info) && info.getId().longValue() != paramsId.longValue()) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }
}
