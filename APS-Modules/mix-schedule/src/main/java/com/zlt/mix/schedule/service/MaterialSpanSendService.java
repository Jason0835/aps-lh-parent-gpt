package com.zlt.mix.schedule.service;

import java.util.Date;
import java.util.List;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zlt.mix.schedule.api.domain.entity.MaterialSpanReceive;
import com.zlt.mix.schedule.api.domain.entity.MaterialSpanSend;

/**
 * 硫磺辅料跨区发送Service接口
 *
 * @author cxy
 * @date 2022-08-30
 */
public interface MaterialSpanSendService extends IService<MaterialSpanSend> {

    /**
     * 新增跨区发送记录
     *
     * @param materialSpanSend 要新增的记录
     * @return 结果
     */
    boolean insertMaterialSpanSend(MaterialSpanSend materialSpanSend);

    /**
     * 校验胶料跨区发送唯一性
     */
    String checkMaterialSpanSendUnique(MaterialSpanSend materialSpanSend);

    /**
     * 根据条件查询分解胶料需求量跨区发送列表
     *
     * @param entity 查询条件
     * @return 结果
     */
    List<MaterialSpanSend> listMaterialSpanSend(MaterialSpanSend entity);

    /**
     * 批量新增跨区发送请求记录
     *
     * @param materialSpanSendList 要批量保存的记录
     * @return 影响行数
     */
    int batchInsertMaterialSpanSend(List<MaterialSpanSend> materialSpanSendList);

    /**
     * 批量更新跨区发送记录,仅更新发布状态，更新人，更新时间，通过接收表的 send_id关联更新
     *
     * @param receiveList 批量更新的记录
     * @return 影响行数
     */
    int mergeMaterialSpanSend(List<MaterialSpanReceive> receiveList);

    /**
     * 根据id查询已接收的记录数
     *
     * @param ids id
     * @return 已接收记录数
     */
    Integer getAlreadyReceivedCount(Long[] ids);

    /**
     * 根据id删除发送记录
     *
     * @param ids id
     * @return 结果
     */
    int deleteByIds(Long[] ids);

    /**
     *  删除还未接收的跨区发送记录
     * @param mixArea  密炼区
     * @param scheduleDate  排程日期
     */
    void deleteNotReceived(String mixArea, Date scheduleDate);
}
