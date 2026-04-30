<!--
 * @Description: 审批流程 页面
 * @Author: qy
 * @Date: 2024/2/26
-->
<template>
  <div class="w100 h100" style="background-color: #ffffff;position: relative">
    <div></div>
    <div class="row flex flex-items-center" v-for="(item, index) in data" :key="index">
      <div class="approval-type" :class="[ item.type === 1 ? 'error' : item.type === 2 ? 'success' : item.type === 3 ? 'warning' : item.type === 4 ? 'primary' : 'gray' ]">{{ item.type === 1 ? '选审' : item.type === 2 ? '提交' : item.type === 3 ? '选审' : item.type === 4 ? '单审' : '选审' }}</div>
      <div class="process-line-top" v-if="index > 0"></div>
      <div class="process-line-bottom" v-if="index < data.length - 1" :class="{'arrow': index === 0}"></div>
      <div class="c8e date text-center">2029-10-18 11:27:58</div>
      <div class="b68 process-text text-center">TMS派车</div>
      <div class="dot-line"></div>
      <div class="content flex-1">
        <div class="title c8e">TMS派车</div>
        <div class="flex step" style="align-items: start">
          <div class="step-icon flex">
            <div class="dot"></div>
            <div class="count">1</div>
            <div class="use-head">
              <svg-icon icon-class="user" style="font-size: 45px;margin-bottom: 0;margin-left: 6px;color: #d4d7db" />
            </div>
          </div>
          <div class="info">
            <div class="flex c5c">
              <div class="item">指审人：成全娟</div>
              <div>实审人：赵蒙蒙</div>
            </div>
            <div class="flex c8e mt10">
              <div class="item">创建时间：2029-10-18 11:27:58</div>
              <div class="item">完成时间：2029-10-18 11:27:58</div>
              <div>停留时长：9秒</div>
            </div>
            <div class="flex c5c mt10">
              <div>审批附言：同意</div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  data() {
    return {
      data: [{ type: 1 }, { type: 2 }, { type: 3 }, { type: 4 }]
    }
  }
}
</script>

<style scoped>
.h100 {
  height: 100%;
}
.flex {
  display: flex;
}
.flex-1 {
  flex: 1
}
.flex-items-center {
  align-items: center;
}
.c8e {
  color: #8e96b7
}
.c5c {
  color: #5c606e
}
.b68 {
  background-color: #688bf8;
}
.error {
  background-color: #ff5454;
}
.warning {
  background-color: #f8d343;
}
.success {
  background-color: #00c88a;
}
.gray {
  background-color: #a0a4af;
}
.primary {
  background-color: #688bf8;
}
.mt10 {
  margin-top: 10px;
}
.text-center {
  text-align: center;
}
.row {
  position: relative;
  padding-bottom: 10px;
  overflow: hidden;
}
.row .date {
  width: 85px;
  font-size: 14px;
}
.row .approval-type {
  width: 60px;
  height: 20px;
  transform: rotate(45deg);
  position: absolute;
  right: -15px;
  top: 5px;
  text-align: center;
  color: #fff
}
.row .process-line-top {
  width: 2px;
  height: calc(50% - 30px);
  background-color: #688bf8;
  position: absolute;
  top: 0;
  left: 120px;
  z-index: 0;
}
.row .process-line-bottom {
  width: 2px;
  height: calc(50% - 20px);
  background-color: #688bf8;
  position: absolute;
  bottom: 0;
  left: 120px;
  z-index: 0;
}
.row .process-line-bottom.arrow {
  height: calc(50% - 30px)!important;
}
.row .process-line-bottom.arrow:before {
  content: '';
  width: 0;
  height: 0;
  border-left: 6px solid transparent;
  border-right: 6px solid transparent;
  border-bottom: 12px solid #688bf8;
  top: -10px;
  left: -5px;
  position: absolute;
}
.row .process-text {
  width: 72px;
  color: #fff;
  padding: 4px 4px;
  border-radius: 5px;
  font-size: 14px;
  font-weight: bold;
  z-index: 1;
}
.dot-line {
  width: 15px;
  height: 15px;
  border-radius: 50%;
  background-color: #00c88a;
  display: flex;
  align-items: center;
  margin-left: 30px;
  margin-right: 30px;
}
.dot-line:before {
  content: '';
  width: 28px;
  height: 1px;
  margin-left: -30px;
  margin-right: 35px;
  background-color: #c0c3c8;
  display: block;
}
.dot-line:after {
  content: '';
  width: 28px;
  height: 1px;
  margin-right: -30px;
  background-color: #c0c3c8;
  display: block;
}
.content {
  border: 1px solid #e2e5e6;
  border-radius: 5px;
  overflow: hidden;
  margin-left: 10px;
}
.content>.title {
  background-color: #f3f6f7;
  padding: 10px 15px;
}
.content>.step {
  padding: 20px 15px;
}
.step-icon .dot {
  width: 15px;
  height: 15px;
  border-radius: 50%;
  background-color: #00c88a;
}
.step-icon .count {
  width: 20px;
  height: 20px;
  line-height: 18px;
  text-align: center;
  border-radius: 50%;
  border: 1px solid #627193;
  margin-left: 10px;
}
.step-icon .use-head {
  width: 60px;
  height: 60px;
  border: 1px solid #627193;
  border-radius: 50%;
  margin-left: 10px;
  display: flex;
  align-items: flex-end;
  text-align: center;
  overflow: hidden;
  justify-items: center;
}
.content>.step>.info {
  padding-left: 20px;
}
.content>.step>.info .item {
  width: 280px
}
</style>
