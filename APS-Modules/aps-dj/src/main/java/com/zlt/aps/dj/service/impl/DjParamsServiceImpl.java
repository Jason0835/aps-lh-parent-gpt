package com.zlt.aps.dj.service.impl;

import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.dj.api.domain.dto.DjParamsDto;
import com.zlt.aps.dj.api.domain.entity.DjParams;
import com.zlt.aps.dj.mapper.DjParamsMapper;
import com.zlt.aps.dj.service.DjParamsService;

/**
 * 垫胶参数信息Service业务层处理
 *
 * @author zlt
 * @date 2026-05-25
 */
@Service
public class DjParamsServiceImpl extends ServiceImpl<DjParamsMapper, DjParams> implements DjParamsService {
    @Autowired
    private DjParamsMapper ncParamsMapper;

    /**
     * 查询垫胶参数信息
     *
     * @param id 垫胶参数信息ID
     * @return 垫胶参数信息
     */
    @Override
    public DjParams selectParamsById(Long id) {
        LambdaQueryWrapper<DjParams> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DjParams::getDelFlag, ApsConstant.DEL_FLAG_NORMAL);
        wrapper.eq(DjParams::getId, id);
        return ncParamsMapper.selectOne(wrapper);
    }

    /**
     * 查询垫胶参数信息列表
     *
     * @param params 垫胶参数信息
     * @return 垫胶参数信息
     */
    @Override
    public List<DjParamsDto> selectParamsList(DjParams params) {
        return ncParamsMapper.listParams(params);
    }

    /**
     * 修改垫胶参数信息
     *
     * @param params 垫胶参数信息
     * @return 结果
     */
    @Override
    public AjaxResult updateParams(DjParams params) {
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
            ncParamsMapper.updateById(params);
        }
        return regularResult ? AjaxResult.success() : AjaxResult.error(params.getErrorTips());
    }

    /**
     * 校验参数代码唯一性
     *
     * @param params 垫胶参数信息
     * @return 是否唯一
     */
    @Override
    public String checkParamsCodeUnique(DjParams params) {
        //校验参数代码字段唯一性
        Long paramsId = StringUtils.isNull(params.getId()) ? -1L : params.getId();
        DjParams info = ncParamsMapper.checkParamsCodeUnique(params.getParamCode(), paramsId);
        if (StringUtils.isNotNull(info) && info.getId().longValue() != paramsId.longValue()) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }
}
