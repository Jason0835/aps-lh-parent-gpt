package com.zlt.mix.schedule.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.mix.common.core.constant.ZltConstant;
import com.zlt.mix.schedule.api.domain.entity.GlueSpanReceive;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import com.zlt.mix.schedule.mapper.GlueSpanSendMapper;
import com.zlt.mix.schedule.api.domain.entity.GlueSpanSend;
import com.zlt.mix.schedule.service.GlueSpanSendService;

import javax.annotation.Resource;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 胶料跨区发送Service业务层处理
 *
 * @author chen
 * @date 2022-08-15
 */
@Service
public class GlueSpanSendServiceImpl extends ServiceImpl<GlueSpanSendMapper, GlueSpanSend> implements GlueSpanSendService {
    @Resource
    private GlueSpanSendMapper glueSpanSendMapper;

    /**
     * 新增跨区发送记录
     * @param glueSpanSend 要新增的记录
     * @return 结果
     */
    @Override
    public boolean insertGlueSpanSend(GlueSpanSend glueSpanSend) {
        /*String unique = this.checkGlueSpanSendUnique(glueSpanSend);
        if (ZltConstant.NOT_UNIQUE.equals(unique)) {
            throw new RuntimeException(I18nUtil.getMessage("schedule.glueSpanSend.database.unique"));
        }*/
        return this.save(glueSpanSend);
    }

    /**
     * 校验胶料跨区发送唯一性
     */
    @Override
    public String checkGlueSpanSendUnique(GlueSpanSend glueSpanSend) {
        if (glueSpanSend == null) {
            return ZltConstant.NOT_UNIQUE;
        }

        LambdaQueryWrapper<GlueSpanSend> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(GlueSpanSend::getDelFlag, ZltConstant.DEL_FLAG_NORMAL);
        queryWrapper.eq(GlueSpanSend::getScheduleDate, glueSpanSend.getScheduleDate());
        queryWrapper.eq(GlueSpanSend::getEntrustMixArea, glueSpanSend.getEntrustMixArea());
        queryWrapper.eq(GlueSpanSend::getEntrustedMixArea, glueSpanSend.getEntrustedMixArea());
        queryWrapper.eq(GlueSpanSend::getGlue, glueSpanSend.getGlue());
        if (glueSpanSend.getId() != null) {
            queryWrapper.ne(GlueSpanSend::getId, glueSpanSend.getId());  //编辑的时候校验，要过滤掉自身的id
        }
        List<GlueSpanSend> list = glueSpanSendMapper.selectList(queryWrapper);
        if (list.size() > 0) {
            return ZltConstant.NOT_UNIQUE;
        }
        return ZltConstant.UNIQUE;
    }

    /**
     * 根据条件查询分解胶料需求量跨区发送列表
     *
     * @param entity 查询条件
     * @return 结果
     */
    @Override
    public List<GlueSpanSend> listGlueSpanSend(GlueSpanSend entity) {
        return glueSpanSendMapper.listGlueSpanSend(entity);
    }

    /**
     * 批量新增跨区发送请求记录
     *
     * @param glueSpanSendList 要批量保存的记录
     * @return 影响行数
     */
    @Override
    public int batchInsertGlueSpanSend(List<GlueSpanSend> glueSpanSendList) {
        int result = 0;
        if (CollectionUtils.isNotEmpty(glueSpanSendList)) {
            /*List<ImportErrorLog> codeUniqueErrorLogs = glueSpanSendMapper.listGlueSpanSendNotUnique(glueSpanSendList);
            Map<Integer, Long> codeUniqueErrorMap = codeUniqueErrorLogs.stream().collect(Collectors.groupingBy(ImportErrorLog::getErrorRow, Collectors.counting()));
            for (int i = 0; i < glueSpanSendList.size(); i++) {
                if (codeUniqueErrorMap.containsKey(i)) {
                    throw new RuntimeException(I18nUtil.getMessage("schedule.glueSpanSend.database.unique"));
                }
            }*/
            result = glueSpanSendMapper.batchInsertGlueSpanSend(glueSpanSendList);
        }
        return result;
    }

    /**
     * 批量更新跨区发送记录,仅更新发布状态，更新人，更新时间，通过接收表的 send_id关联更新
     *
     * @param receiveList 批量更新的记录
     * @return 影响行数
     */
    @Override
    public int mergeGlueSpanSend(List<GlueSpanReceive> receiveList) {
        return glueSpanSendMapper.mergeGlueSpanSend(receiveList);
    }

    /**
     * 根据id查询已接收的记录数
     *
     * @param ids id
     * @return 已接收记录数
     */
    @Override
    public Integer getAlreadyReceivedCount(Long[] ids) {
        return glueSpanSendMapper.getAlreadyReceivedCount(ids);
    }

    /**
     * 根据id删除发送记录
     *
     * @param ids id
     * @return 结果
     */
    @Override
    public int deleteByIds(Long[] ids) {
        return glueSpanSendMapper.deleteByIds(ids);
    }
}
