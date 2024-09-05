package com.zlt.aps.common.engine.mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import com.zlt.aps.common.engine.domain.MdmMonthPlanMain;

/**
 * planmainMapper接口
 * 
 * @author Joran.zhang
 * @date 2021-06-24
 */
public interface MdmMonthPlanMainMapper 
{
    /**
     * 查询planmain
     * 
     * @param id planmainID
     * @return planmain
     */
    public MdmMonthPlanMain selectMdmMonthPlanMainById(Long id);

    /**
     * 查询planmain列表
     * 
     * @param mdmMonthPlanMain planmain
     * @return planmain集合
     */
    public List<MdmMonthPlanMain> selectMdmMonthPlanMainList(MdmMonthPlanMain mdmMonthPlanMain);

    /**
     * 新增planmain
     * 
     * @param mdmMonthPlanMain planmain
     * @return 结果
     */
    public int insertMdmMonthPlanMain(MdmMonthPlanMain mdmMonthPlanMain);

    int insertAll(MdmMonthPlanMain mdmMonthPlanMain);

    /**
     * 修改planmain
     * 
     * @param mdmMonthPlanMain planmain
     * @return 结果
     */
    public int updateMdmMonthPlanMain(MdmMonthPlanMain mdmMonthPlanMain);

    /**
     * 删除planmain
     * 
     * @param id planmainID
     * @return 结果
     */
    public int deleteMdmMonthPlanMainById(Long id);

    int deleteByApsVersion(String apsVersion);

    int deleteByYearAndMonthAndIsFinal(@Param("year") String year, @Param("month") String month, @Param("isFinal") String isFinal);

    /**
     * 批量删除planmain
     * 
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deleteMdmMonthPlanMainByIds(Long[] ids);

    /**
     * 查询当月定稿数据，只取一条
     * @param mdmMonthPlanMain
     * @return
     */
    public MdmMonthPlanMain getValidPlanMainVersion(MdmMonthPlanMain mdmMonthPlanMain);

    /**
     * 查最新的主表信息
     * @return
     */
    public MdmMonthPlanMain selectNewestPlanMain();
}
