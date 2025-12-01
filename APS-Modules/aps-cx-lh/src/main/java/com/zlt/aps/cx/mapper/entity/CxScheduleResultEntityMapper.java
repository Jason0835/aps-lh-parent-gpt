package com.zlt.aps.cx.mapper.entity;

import com.zlt.aps.common.core.domain.SchedulePublishRecord;
import com.zlt.aps.cxlh.cx.api.domain.entity.CxProductConstructionInfo;
import com.zlt.aps.cxlh.cx.api.domain.entity.CxScheduleResult;
import com.zlt.aps.cxlh.cx.api.domain.vo.CxGanttVo;
import com.zlt.core.dao.basemapper.CommBaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;


/**
 * Description: 成型排程结果Mapper
 * @author 16799
 */
@Mapper
public interface CxScheduleResultEntityMapper extends CommBaseMapper<CxScheduleResult> {

    /**
     * 根据id查询当前日期发布状态为"发布中"或"超时失败"的记录
     * @param ids id
     * @return 查询到的记录数
     */
    int isReleasingOrTimeoutByIds(long[] ids);

    /**
     * 更改发布状态
     * @param entity 排程日期
     * @return 结果
     */
    int changeReleaseStatus(CxScheduleResult entity);


    public int batchUpdate(@Param("array") Long[] ids, @Param("status") String status);

    /**
     * 更新成型完成量汇总
     *
     * @return 结果
     */
    public int updateMonthPlanSurplus();


    /**
     * 查询待删除列表校验需要校验
     *
     * @param ids
     */
    public List<CxScheduleResult> selectRemoveList(@Param("ids") Long[] ids);

    /**
     * 把排程数据发布到中间库
     *
     * @param dataVersion 接口发布版本号
     * @param ids         排程发布的ids
     * @param factoryCode 厂别
     * @param companyCode 分公司编号
     */
    public void deployScheduleToMes(@Param("dataVersion") String dataVersion, @Param("ids") long[] ids,
                                    @Param("factoryCode") String factoryCode, @Param("companyCode") String companyCode, @Param("language") String language);

    /**
     * 保存发布日志
     *
     * @param schedulePublishRecord 要保存的发布日志
     * @return 结果
     */
    public int insertPublishRecord(SchedulePublishRecord schedulePublishRecord);

    /**
     * 查询成型甘特图数据
     *
     * @param queryVO 查询参数
     * @return 结果
     */
    List<CxGanttVo> getCxGanttData(CxGanttVo queryVO);

    /**
     * 查询老版本和新版本的施工信息
     *
     * @param embryoCode parts编码
     * @param oldVersion 老版本
     * @param newVersion 新版本
     * @return 结果
     */
    List<CxProductConstructionInfo> selectOldNewConstruction(@Param("embryoCode") String embryoCode,
                                                             @Param("oldVersion") String oldVersion,
                                                             @Param("newVersion") String newVersion);

    /**
     * 根据半部件编码查询是否存在共用胎胚的
     * @param oldFieldValue 半部件编码
     * @param scheduleDate 排程日期
     * @param partFieldName 半部件属性名
     * @return 结果
     */
    List<CxScheduleResult> selectByEmbryoCodeAndScheduleDate(@Param("oldFieldValue") Object oldFieldValue,
                                                             @Param("scheduleDate") Date scheduleDate,
                                                             @Param("partFieldName") String partFieldName);

    /**
     * 根据半部件属性名查询半部件对应库存
     * @param oldFieldValue 半部件编号
     * @param scheduleDate 排程日期
     * @param partFieldName 半部件属性名
     * @return 库存
     */
    BigDecimal selectByPartFieldNameAndScheduleDate(@Param("oldFieldValue") Object oldFieldValue,
                                                    @Param("scheduleDate") Date scheduleDate,
                                                    @Param("partFieldName") String partFieldName);
}
