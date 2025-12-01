package com.zlt.mix.schedule.service.impl;

import java.util.List;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import javax.annotation.Resource;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.zlt.mix.common.core.constant.ZltConstant;
import com.zlt.mix.schedule.mapper.GlueSpanReceiveMapper;
import com.zlt.mix.schedule.api.domain.entity.GlueSpanReceive;
import com.zlt.mix.schedule.engine.service.glueschedule.GlueScheduleEngineService;
import com.zlt.mix.schedule.service.GlueSpanReceiveService;

/**
 * 胶料跨区接收Service业务层处理
 *
 * @author chen
 * @date 2022-08-16
 */
@Service
public class GlueSpanReceiveServiceImpl extends ServiceImpl<GlueSpanReceiveMapper, GlueSpanReceive> implements GlueSpanReceiveService {
    @Resource
    private GlueSpanReceiveMapper glueSpanReceiveMapper;
    @Autowired
    private GlueScheduleEngineService glueScheduleEngineService;

    /**
     * 校验胶料跨区接收唯一性
     */
    @Override
    public String checkGlueSpanReceiveUnique(GlueSpanReceive glueSpanReceive) {
        if (glueSpanReceive == null) {
            return ZltConstant.NOT_UNIQUE;
        }

        LambdaQueryWrapper<GlueSpanReceive> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(GlueSpanReceive::getDelFlag, ZltConstant.DEL_FLAG_NORMAL);
        queryWrapper.eq(GlueSpanReceive::getScheduleDate, glueSpanReceive.getScheduleDate());
        queryWrapper.eq(GlueSpanReceive::getEntrustMixArea, glueSpanReceive.getEntrustMixArea());
        queryWrapper.eq(GlueSpanReceive::getEntrustedMixArea, glueSpanReceive.getEntrustedMixArea());
        queryWrapper.eq(GlueSpanReceive::getGlue, glueSpanReceive.getGlue());
        if (glueSpanReceive.getId() != null) {
            queryWrapper.ne(GlueSpanReceive::getId, glueSpanReceive.getId());  //编辑的时候校验，要过滤掉自身的id
        }

        List<GlueSpanReceive> list = glueSpanReceiveMapper.selectList(queryWrapper);
        if (list.size() > 0) {
            return ZltConstant.NOT_UNIQUE;
        }
        return ZltConstant.UNIQUE;
    }

    /**
     * 批量新增跨区接收请求记录
     *
     * @param glueSpanReceiveList 要批量保存的记录
     * @return 影响行数
     */
    @Override
    public int batchInsertGlueSpanReceive(List<GlueSpanReceive> glueSpanReceiveList) {
        int result = 0;
        if (CollectionUtils.isNotEmpty(glueSpanReceiveList)) {
            /*List<ImportErrorLog> codeUniqueErrorLogs = glueSpanReceiveMapper.listGlueSpanReceiveNotUnique(glueSpanReceiveList);
            Map<Integer, Long> codeUniqueErrorMap = codeUniqueErrorLogs.stream().collect(Collectors.groupingBy(ImportErrorLog::getErrorRow, Collectors.counting()));
            for (int i = 0; i < glueSpanReceiveList.size(); i++) {
                if (codeUniqueErrorMap.containsKey(i)) {
                    throw new RuntimeException(I18nUtil.getMessage("schedule.glueSpanReceive.database.unique"));
                }
            }*/
            result = glueSpanReceiveMapper.batchInsertGlueSpanReceive(glueSpanReceiveList);
        }
        return result;
    }

    /**
     * 查询跨区接收列表
     *
     * @param entity 参数
     * @return 结果
     */
    @Override
    public List<GlueSpanReceive> listGlueSpanReceive(GlueSpanReceive entity) {
        return glueSpanReceiveMapper.listGlueSpanReceive(entity);
    }

    /**
     * 根据id查询跨区接收信息
     *
     * @param entity id
     * @return 查询到的记录
     */
    @Override
    public GlueSpanReceive getGlueSpanReceiveInfo(GlueSpanReceive entity) {
        return glueSpanReceiveMapper.getGlueSpanReceiveInfo(entity);
    }

    /**
     * 批量更新跨区接收记录
     *
     * @param receiveList 批量更新的记录
     * @return 影响行数
     */
    @Override
    public int mergeGlueSpanReceive(List<GlueSpanReceive> receiveList) {
    	// 调用引擎接口生成计划
    	glueScheduleEngineService.glueSpanReceive(receiveList);
        return glueSpanReceiveMapper.mergeGlueSpanReceive(receiveList);
    }

    /**
     * 根据排程日期、被委托密炼区查询未被接收的跨区请求总数
     *
     * @param glueSpanReceive 参数
     * @return 未接收的总数
     */
    @Override
    public Integer selectUnReceiveCount(GlueSpanReceive glueSpanReceive) {
        return glueSpanReceiveMapper.selectUnReceiveCount(glueSpanReceive);
    }

    /**
     * 根据sendIds查询已接收的记录数
     *
     * @param sendIds sendIds
     * @return 已接收记录数
     */
    @Override
    public Integer getAlreadyReceivedCount(Long[] sendIds) {
        return glueSpanReceiveMapper.getAlreadyReceivedCount(sendIds);
    }

    /**
     * 根据Id查询已接收的记录数
     *
     * @param ids ids
     * @return 已接收记录数
     */
    @Override
    public Integer getAlreadyReceivedCountByIds(Long[] ids) {
        return glueSpanReceiveMapper.getAlreadyReceivedCountByIds(ids);
    }

    /**
     * 根据send_id删除发送记录
     * @param sendIds sendId
     * @return 结果
     */
    @Override
    public int deleteBySendIds(Long[] sendIds) {
        return glueSpanReceiveMapper.deleteBySendIds(sendIds);
    }
}
