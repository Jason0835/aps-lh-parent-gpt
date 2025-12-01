package com.zlt.mix.schedule.service.impl;

import java.util.Date;
import java.util.List;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import javax.annotation.Resource;

import com.zlt.mix.schedule.api.domain.entity.MaterialSpanReceive;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import com.zlt.mix.common.core.constant.ZltConstant;
import com.zlt.mix.schedule.mapper.MaterialSpanSendMapper;
import com.zlt.mix.schedule.api.domain.entity.MaterialSpanSend;
import com.zlt.mix.schedule.service.MaterialSpanSendService;

/**
 * 硫磺辅料跨区发送Service业务层处理
 *
 * @author cxy
 * @date 2022-08-30
 */
@Service
public class MaterialSpanSendServiceImpl extends ServiceImpl<MaterialSpanSendMapper, MaterialSpanSend> implements MaterialSpanSendService {
    @Resource
    private MaterialSpanSendMapper materialSpanSendMapper;

    /**
     * 新增跨区发送记录
     *
     * @param materialSpanSend 要新增的记录
     * @return 结果
     */
    @Override
    public boolean insertMaterialSpanSend(MaterialSpanSend materialSpanSend) {
        /*String unique = this.checkMaterialSpanSendUnique(materialSpanSend);
        if (ZltConstant.NOT_UNIQUE.equals(unique)) {
            throw new RuntimeException(I18nUtil.getMessage("schedule.materialSpanSend.database.unique"));
        }*/
        return this.save(materialSpanSend);
    }

    /**
     * 校验胶料跨区发送唯一性
     */
    @Override
    public String checkMaterialSpanSendUnique(MaterialSpanSend materialSpanSend) {
        if (materialSpanSend == null) {
            return ZltConstant.NOT_UNIQUE;
        }

        LambdaQueryWrapper<MaterialSpanSend> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(MaterialSpanSend::getDelFlag, ZltConstant.DEL_FLAG_NORMAL);
        queryWrapper.eq(MaterialSpanSend::getScheduleDate, materialSpanSend.getScheduleDate());
        queryWrapper.eq(MaterialSpanSend::getEntrustMixArea, materialSpanSend.getEntrustMixArea());
        queryWrapper.eq(MaterialSpanSend::getEntrustedMixArea, materialSpanSend.getEntrustedMixArea());
        queryWrapper.eq(MaterialSpanSend::getMaterialName, materialSpanSend.getMaterialName());
        if (materialSpanSend.getId() != null) {
            queryWrapper.ne(MaterialSpanSend::getId, materialSpanSend.getId());  //编辑的时候校验，要过滤掉自身的id
        }
        List<MaterialSpanSend> list = materialSpanSendMapper.selectList(queryWrapper);
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
    public List<MaterialSpanSend> listMaterialSpanSend(MaterialSpanSend entity) {
        return materialSpanSendMapper.listMaterialSpanSend(entity);
    }

    /**
     * 批量新增跨区发送请求记录
     *
     * @param materialSpanSendList 要批量保存的记录
     * @return 影响行数
     */
    @Override
    public int batchInsertMaterialSpanSend(List<MaterialSpanSend> materialSpanSendList) {
        int result = 0;
        if (CollectionUtils.isNotEmpty(materialSpanSendList)) {
            /*List<ImportErrorLog> codeUniqueErrorLogs = materialSpanSendMapper.listMaterialSpanSendNotUnique(materialSpanSendList);
            Map<Integer, Long> codeUniqueErrorMap = codeUniqueErrorLogs.stream().collect(Collectors.groupingBy(ImportErrorLog::getErrorRow, Collectors.counting()));
            for (int i = 0; i < materialSpanSendList.size(); i++) {
                if (codeUniqueErrorMap.containsKey(i)) {
                    throw new RuntimeException(I18nUtil.getMessage("schedule.materialSpanSend.database.unique"));
                }
            }*/
            result = materialSpanSendMapper.batchInsertMaterialSpanSend(materialSpanSendList);
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
    public int mergeMaterialSpanSend(List<MaterialSpanReceive> receiveList) {
        return materialSpanSendMapper.mergeMaterialSpanSend(receiveList);
    }

    /**
     * 根据id查询已接收的记录数
     *
     * @param ids id
     * @return 已接收记录数
     */
    @Override
    public Integer getAlreadyReceivedCount(Long[] ids) {
        return materialSpanSendMapper.getAlreadyReceivedCount(ids);
    }

    /**
     * 根据id删除发送记录
     *
     * @param ids id
     * @return 结果
     */
    @Override
    public int deleteByIds(Long[] ids) {
        return materialSpanSendMapper.deleteByIds(ids);
    }

    /**
     *  删除还未接收的跨区发送记录
     * @param mixArea  密炼区
     * @param scheduleDate  排程日期
     */
    public void deleteNotReceived(String mixArea, Date scheduleDate) {
        materialSpanSendMapper.deleteNotReceived(mixArea, scheduleDate);
    }
}
