package com.zlt.aps.tm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.tm.api.domain.dto.TmParamsDto;
import com.zlt.aps.tm.entity.TmParams;
import com.zlt.aps.tm.mapper.TmParamsMapper;
import com.zlt.aps.tm.service.TmParamsService;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Pattern;

/**
 * 胎侧参数信息Service业务层处理
 *
 * @author zlt
 * @date 2021-05-25
 */
@Service
public class TmParamsServiceImpl extends ServiceImpl<TmParamsMapper, TmParams> implements TmParamsService {
    @Autowired
    private TmParamsMapper tmParamsMapper;

    /**
     * 查询胎侧参数信息
     *
     * @param id 胎侧参数信息ID
     * @return 胎侧参数信息
     */
    @Override
    public TmParams selectParamsById(Long id) {
        LambdaQueryWrapper<TmParams> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TmParams::getDelFlag, ApsConstant.DEL_FLAG_NORMAL);
        wrapper.eq(TmParams::getId, id);
        return tmParamsMapper.selectOne(wrapper);
    }

    /**
     * 查询胎侧参数信息列表
     *
     * @param tmParams 胎侧参数信息
     * @return 胎侧参数信息
     */
    @Override
    public List<TmParamsDto> selectParamsList(TmParams tmParams) {
        return tmParamsMapper.listParams(tmParams);
    }

    /**
     * 修改胎侧参数信息
     *
     * @param tmParams 胎侧参数信息
     * @return 结果
     */
    @Override
    public AjaxResult updateParams(TmParams tmParams) {
        //基本参数校验
        if (!ObjectUtils.allNotNull(tmParams.getId(), tmParams.getParamCode(), tmParams.getParamName(), tmParams.getParamValue())) {
            return AjaxResult.error(I18nUtil.getMessage("ui.params.message.invalidParameter"));
        }
        String unique = checkParamsCodeUnique(tmParams);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.params.message.unique"));
        }
        //校验正则表达式
        boolean regularResult = true;
        String regularExpression = tmParams.getRegularExpression();
        if (StringUtils.isNotEmpty(regularExpression)) {
            Pattern pattern = Pattern.compile(regularExpression);
            regularResult = pattern.matcher(tmParams.getParamValue()).matches();
        }
        if (regularResult) {
            tmParams.setBaseVale(tmParams.getId());
            tmParamsMapper.updateById(tmParams);
        }
        return regularResult ? AjaxResult.success() : AjaxResult.error(tmParams.getErrorTips());
    }

    /**
     * 校验参数代码唯一性
     *
     * @param tmParams 胎侧参数信息
     * @return 是否唯一
     */
    @Override
    public String checkParamsCodeUnique(TmParams tmParams) {
        //校验参数代码字段唯一性
        Long paramsId = StringUtils.isNull(tmParams.getId()) ? -1L : tmParams.getId();
        TmParams info = tmParamsMapper.checkParamsCodeUnique(tmParams.getParamCode(), paramsId);
        if (StringUtils.isNotNull(info) && info.getId().longValue() != paramsId.longValue()) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }
}
