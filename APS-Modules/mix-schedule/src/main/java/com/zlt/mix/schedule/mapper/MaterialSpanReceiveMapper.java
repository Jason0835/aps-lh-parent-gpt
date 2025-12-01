package com.zlt.mix.schedule.mapper;

import java.util.Date;
import java.util.List;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.zlt.mix.schedule.api.domain.entity.MaterialSpanReceive;
import org.apache.ibatis.annotations.Param;
import com.zlt.mix.schedule.api.domain.entity.MaterialSpanReceive;

/**
 * 硫磺辅料跨区接收Mapper接口
 *
 * @author cxy
 * @date 2022-08-30
 */
public interface MaterialSpanReceiveMapper extends BaseMapper<MaterialSpanReceive> {

    /**
     * 批量新增跨区接收请求记录
     * @param materialSpanReceiveList 要批量保存的记录
     * @return 影响行数
     */
    int batchInsertMaterialSpanReceive(@Param("list") List<MaterialSpanReceive> materialSpanReceiveList);

    /**
     * 查询跨区接收列表
     * @param entity 参数
     * @return 结果
     */
    List<MaterialSpanReceive> listMaterialSpanReceive(MaterialSpanReceive entity);

    /**
     * 根据id查询跨区接收信息
     * @param entity id
     * @return 查询到的记录
     */
    MaterialSpanReceive getMaterialSpanReceiveInfo(MaterialSpanReceive entity);

    /**
     * 批量更新跨区接收记录
     * @param receiveList 批量更新的记录
     * @return 影响行数
     */
    int mergeMaterialSpanReceive(@Param("list") List<MaterialSpanReceive> receiveList);

    /**
     * 查询出批量新增的数据中，在系统中已经存在的数据
     * @param list 要校验的数据列表
     * @return 结果
     */
    List<ImportErrorLog> listMaterialSpanReceiveNotUnique(List<MaterialSpanReceive> list);

    /**
     * 根据排程日期、被委托密炼区查询未被接收的跨区请求总数
     * @param materialSpanReceive 参数
     * @return 未接收的总数
     */
    Integer selectUnReceiveCount(MaterialSpanReceive materialSpanReceive);

    /**
     * 根据sendId查询已接收的记录数
     * @param sendIds sendId
     * @return 已接收记录数
     */
    Integer getAlreadyReceivedCount(Long[] sendIds);

    /**
     * 根据send_id删除发送记录
     * @param sendIds sendId
     * @return 结果
     */
    int deleteBySendIds(Long[] sendIds);

    /**
     * 根据Id查询已接收的记录数
     *
     * @param ids ids
     * @return 已接收记录数
     */
    Integer getAlreadyReceivedCountByIds(Long[] ids);

    /**
     *  删除还未接收的跨区接收记录
     * @param mixArea  密炼区
     * @param scheduleDate  排程日期
     */
    void deleteNotReceived(@Param("mixArea") String mixArea, @Param("scheduleDate") Date scheduleDate);
}
