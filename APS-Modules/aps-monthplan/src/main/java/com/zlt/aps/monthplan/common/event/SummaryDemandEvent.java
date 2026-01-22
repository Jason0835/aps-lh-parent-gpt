package com.zlt.aps.monthplan.common.event;

import org.springframework.context.ApplicationEvent;

/**
 * 汇总需求事件
 * @author Yelq
 *
 */
public class SummaryDemandEvent extends ApplicationEvent {

  public SummaryDemandEvent(Object source) {
    super(source);
  }
}
