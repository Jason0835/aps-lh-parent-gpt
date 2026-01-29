/**
 * @Description:  页面
 * @Author: qy
 * @Date: 2024/3/26
 **/
import request from "@/utils/request";

// 查询通知消息接口
export function messageListNoticeMessage(data) {
  return request({
    // url: '/message/messageCenter/listNoticeMessage',
    url: '/message/messageList/list',
    method: 'post',
    // headers: {
    //   'Content-Type': 'application/json;charset=UTF-8'
    // },
    data
  })
}

// 查询待办任务消息接口
export function messageListTaskMessage(data) {
  return request({
    // url: '/message/messageCenter/listTaskMessage',
    url: '/message/messageTaskList/list',
    method: 'post',
    // headers: {
    //   'Content-Type': 'application/json;charset=UTF-8'
    // },
    data
  })
}
