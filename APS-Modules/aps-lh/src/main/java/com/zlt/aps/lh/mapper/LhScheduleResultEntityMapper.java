package com.zlt.aps.lh.mapper;

import com.zlt.aps.common.core.domain.SchedulePublishRecord;
import com.zlt.aps.lh.api.domain.entity.LhScheduleResult;
import com.zlt.aps.lh.api.domain.vo.LhGanttVo;
import com.zlt.aps.mp.api.domain.vo.FactoryMonthPlanProdFinalVo;
import com.zlt.aps.mp.api.domain.vo.LhScheduleResultTotalVo;
import com.zlt.core.dao.basemapper.CommBaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;


/**
 * Description: 硫化排程结果Mapper
 * @author
 */
@Mapper
public interface LhScheduleResultEntityMapper extends CommBaseMapper<LhScheduleResult> {

    /**
     * 查询在一定时间范围内存在的规格代号列表
     *
     * @param factoryCode 工厂
     * @param beginDate 开始日期
     * @param endDate 结束日期
     * @param checkSpecCodeList 验证的规格代号列表
     * @return 结果 存在的规格代号列表
     */
    public List<String> selectLhSpecCodeList(@Param("factoryCode") String factoryCode,
            @Param("beginDate") String beginDate,@Param("endDate") String endDate,
             @Param("checkSpecCodeList") List<String> checkSpecCodeList);

    /**
     * 根据id查询当前日期发布状态为"发布中"或"超时失败"的记录
     * @param ids id
     * @return 查询到的记录数
     */
     int isReleasingOrTimeoutByIds(long[] ids);

    /**
     * 根据分厂编码查询最后一条排程数据的排程时间
     * @return
     */
    Date selectLastScheduleTime(@Param("factoryCode") String factoryCode, @Param("lastScheduleTime") Date lastScheduleTime);

    /**
     * 根据分厂编码和排产日期查询批次号
     * @param scheduleDate
     * @param factoryCode
     * @return
     */
     String selectBatchNoByScheduleDateAndFactoryCode(@Param("scheduleDate") Date scheduleDate,@Param("factoryCode") String factoryCode);

    /**
     * 根据分厂编码和排程时间删除记录
     *
     * @param factoryCode 分厂编码
     * @param scheduleDate 排程时间
     * @return 受影响的行数
     */
    int deleteByFactoryCodeAndScheduleDate(@Param("factoryCode") String factoryCode,
                                           @Param("scheduleDate") Date scheduleDate);

    /**
     * 保存发布日志
     *
     * @param schedulePublishRecord 要保存的发布日志
     * @return 结果
     */
    public int insertPublishRecord(SchedulePublishRecord schedulePublishRecord);

    /**
     * 查询指定日期的排程结果是否已经发布
     *
     * @param schedulePublishRecord 要查询的日期及工序参数
     * @return 查询到的记录条数
     */
    public int isPublish(SchedulePublishRecord schedulePublishRecord);

    /**
     * 批量更新发布状态
     *
     * @param ids    要更新的ID
     * @param status 状态
     * @return 结果
     */
    public int batchUpdate(@Param("array") Long[] ids, @Param("status") String status);

    /**
     * 根据数据版本更新发布日志状态
     *
     * @param dataVersion 数据版本
     * @param status      状态
     * @return 结果
     */
    public int updatePublishRecordVersion(@Param("dataVersion") String dataVersion, @Param("status") String status);

    /**
     * 把排程数据发布到中间库
     *
     * @param dataVersion 接口发布版本号
     * @param ids         排程发布的ids
     * @param factoryCode 厂别
     * @param companyCode 分公司编号
     */
    public void deployScheduleToMes(@Param("dataVersion") String dataVersion, @Param("ids") long[] ids,
                                    @Param("factoryCode") String factoryCode, @Param("companyCode") String companyCode);

    /**
     * 查询硫化甘特图数据
     *
     * @param queryVO 查询参数
     * @return 结果
     */
    public List<LhGanttVo> getLhGanttData(LhGanttVo queryVO);

    List<LhScheduleResultTotalVo> selectLhScheduleResultTotal(@Param("vo") FactoryMonthPlanProdFinalVo factoryMonthPlanProdFinalVo);
}
