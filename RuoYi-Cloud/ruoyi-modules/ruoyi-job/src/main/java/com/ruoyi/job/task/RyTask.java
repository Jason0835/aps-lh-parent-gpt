package com.ruoyi.job.task;

import com.ruoyi.common.i18n.utils.I18nUtil;
import org.springframework.stereotype.Component;
import com.ruoyi.common.utils.StringUtils;

/**
 * 定时任务调度测试
 * 
 * @author ruoyi
 */
@Component("ryTask")
public class RyTask
{
    public void ryMultipleParams(String s, Boolean b, Long l, Double d, Integer i)
    {
        System.out.println(StringUtils.format(I18nUtil.getMessage("job.msg.log.run.mulitparam"), s, b, l, d, i));
    }

    public void ryParams(String params)
    {
        //System.out.println("执行有参方法：" + params);
        System.out.println(StringUtils.format(I18nUtil.getMessage("job.msg.log.run.mulitparam"), params));
    }

    public void ryNoParams()
    {
        System.out.println(I18nUtil.getMessage("job.msg.log.run.noparam"));
    }
}
