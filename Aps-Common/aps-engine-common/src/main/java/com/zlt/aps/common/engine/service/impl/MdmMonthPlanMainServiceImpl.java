package com.zlt.aps.common.engine.service.impl;

import java.util.Date;
import java.util.List;
import com.ruoyi.common.core.utils.DateUtils;
import com.zlt.aps.common.engine.domain.MdmMonthPlanMain;
import com.zlt.aps.common.engine.mapper.MdmMonthPlanMainMapper;
import com.zlt.aps.common.engine.service.MdmMonthPlanMainService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * planmainService业务层处理
 * 
 * @author Joran.zhang
 * @date 2021-06-24
 */
@Service
public class MdmMonthPlanMainServiceImpl implements MdmMonthPlanMainService
{
    @Resource
    private MdmMonthPlanMainMapper mdmMonthPlanMainMapper;

    /**
     * 查询planmain
     * 
     * @param id planmainID
     * @return planmain
     */
    @Override
    public MdmMonthPlanMain selectMdmMonthPlanMainById(Long id)
    {
        return mdmMonthPlanMainMapper.selectMdmMonthPlanMainById(id);
    }

    /**
     * 查询planmain列表
     * 
     * @param mdmMonthPlanMain planmain
     * @return planmain
     */
    @Override
    public List<MdmMonthPlanMain> selectMdmMonthPlanMainList(MdmMonthPlanMain mdmMonthPlanMain)
    {
        return mdmMonthPlanMainMapper.selectMdmMonthPlanMainList(mdmMonthPlanMain);
    }

    /**
     * 新增planmain
     * 
     * @param mdmMonthPlanMain planmain
     * @return 结果
     */
    @Override
    public int insertMdmMonthPlanMain(MdmMonthPlanMain mdmMonthPlanMain)
    {
        mdmMonthPlanMain.setCreateTime(DateUtils.getNowDate());
        return mdmMonthPlanMainMapper.insertAll(mdmMonthPlanMain);
    }

    /**
     * 修改planmain
     * 
     * @param mdmMonthPlanMain planmain
     * @return 结果
     */
    @Override
    public int updateMdmMonthPlanMain(MdmMonthPlanMain mdmMonthPlanMain)
    {
        mdmMonthPlanMain.setUpdateTime(DateUtils.getNowDate());
        return mdmMonthPlanMainMapper.updateMdmMonthPlanMain(mdmMonthPlanMain);
    }

    /**
     * 批量删除planmain
     * 
     * @param ids 需要删除的planmainID
     * @return 结果
     */
    @Override
    public int deleteMdmMonthPlanMainByIds(Long[] ids)
    {
        return mdmMonthPlanMainMapper.deleteMdmMonthPlanMainByIds(ids);
    }

    /**
     * 删除planmain信息
     * 
     * @param id planmainID
     * @return 结果
     */
    @Override
    public int deleteMdmMonthPlanMainById(Long id)
    {
        return mdmMonthPlanMainMapper.deleteMdmMonthPlanMainById(id);
    }

    @Override
    public int deleteByApsVersion(String apsVersion) {
        return mdmMonthPlanMainMapper.deleteByApsVersion(apsVersion);
    }

    @Override
    public int deleteByYearAndMonthAndIsFinal(String year, String month, String isFinal) {
        return mdmMonthPlanMainMapper.deleteByYearAndMonthAndIsFinal(year, month, isFinal);
    }

    /**
     *  获取月度计划主表中定稿且未删除的版本
     * @param scheduleDate 排程日期
     * @return
     */
    @Override
    public MdmMonthPlanMain getValidPlanMainVersion(Date scheduleDate) {
        String year=DateUtils.parseDateToStr(DateUtils.YYYY,scheduleDate);
        String month=DateUtils.parseDateToStr("MM",scheduleDate);
        MdmMonthPlanMain condition=new MdmMonthPlanMain();
        condition.setIsFinalized("0");//定稿数据
        condition.setYear(year);
        condition.setMonth(month);
        return mdmMonthPlanMainMapper.getValidPlanMainVersion(condition);
    }

    /**
     *  获取最新版本的施工
     * @return
     */
    @Override
    public MdmMonthPlanMain selectNewestPlanMain() {
        return mdmMonthPlanMainMapper.selectNewestPlanMain();
    }
}
