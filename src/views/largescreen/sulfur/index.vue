<template>
  <div class="screen">
    <div
    class="screen-flex"
      :style="{
        transform: `scale(${size.scale})`,
        position: 'absolute',
        //top: `${size.top}px`,
        //left: `${size.left}px`,
        '--extraY': `${size.extraY}px`,
        '--extraX': `${size.extraX}px`,
      }"
    >
      <div class="header">
        <img
          class="header-bg"
          height="98"
          :width="1000"
          src="@/assets/largescreen/title_bg.png"
        />
        <div class="header-title">加硫</div>
      </div>
      <div class="tap-container">
        <div
          :class="['tap-item', focusTap == 0 ? 'tap-left-focus' : 'tap-left']"
          @click="changeTap(0)"
        >
          硫化即时产量
        </div>
        <div
          :class="[
            'tap-item',
            focusTap == 1 ? 'tap-center-focus' : 'tap-center',
          ]"
          style="height: 36px; padding: 0 10px"
          @click="changeTap(1)"
        >
          硫化停机损失条数
        </div>
        <div
          :class="['tap-item', focusTap == 2 ? 'tap-right-focus' : 'tap-right']"
          style="width: 180px"
          @click="changeTap(2)"
        >
          生胎库存量可硫化时间
        </div>
      </div>
      <div class="content">
        <div class="search-content">
          <div class="search-item">
            <div class="search-name">日期：</div>
            <el-date-picker v-model="value1" type="date" placeholder="选择日期">
            </el-date-picker>
          </div>
          <div class="search-item">
            <div class="search-name">机台：</div>
            <el-select v-model="value" placeholder="请选择">
              <el-option
                v-for="item in options"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              >
              </el-option>
            </el-select>
          </div>
          <div class="search-item">
            <div class="search-name" style="width: 150px">研发品号：</div>
            <el-input v-model="input" placeholder="请输入内容"></el-input>
          </div>
        </div>
        <div v-show="focusTap == 0">
          <table class="production-table">
            <thead>
              <tr>
                <th>单位</th>
                <th>加硫一部</th>
                <th>加硫二部</th>
                <th>合计</th>
              </tr>
            </thead>
            <tbody>
              <!-- 第一行：即时预定量（条） -->
              <tr>
                <td class="first-col">即时预定量 (条)</td>
                <td>100</td>
                <td>100</td>
                <td>100</td>
              </tr>
              <!-- 第二行：完成量（条） -->
              <tr>
                <td class="first-col">完成量 (条)</td>
                <td>100</td>
                <td>100</td>
                <td>100</td>
              </tr>
              <!-- 第三行：即时达成率 —— 严格按图片加硫一部97.75%、二部96.89%、合计97.33% -->
              <tr>
                <td class="first-col">即时达成率</td>
                <td>100</td>
                <td>100</td>
                <td>100</td>
              </tr>
            </tbody>
          </table>
          <div class="table-content">
            <!-- <ScrollTable
              :columns="skuColumns"
              :data="skuTableData"
              :height="200 + size.extraY / 3 + 'px'"
            /> -->
            <el-table
              :data="skuTableData"
              style="width: 100%; background: none"
              :header-cell-style="headerCellStyle"
            >
              <el-table-column
                v-for="item in skuColumns"
                :key="item"
                :prop="item.prop"
                :label="item.label"
              >
              </el-table-column>
            </el-table>
          </div>
        </div>
        <div v-show="focusTap == 1">
          <div class="table-content">
            <ScrollTable
              :columns="shutdownColumns"
              :data="shutdownData"
              :height="200 + size.extraY / 3 + 'px'"
            />
          </div>
        </div>
        <div v-show="focusTap == 2">
          <div class="table-content">
            <ScrollTable
              :columns="birthColumns"
              :data="birthData"
              :height="200 + size.extraY / 3 + 'px'"
            />
          </div>
        </div>
      </div>
      <div class="pageination-container">
        <el-pagination background layout="prev, pager, next" :total="1000">
        </el-pagination>
      </div>
    </div>
  </div>
</template>
<script>
import ScrollTable from "@/views/largescreen/components/ScrollTable";
export default {
  components: {
    ScrollTable,
  },
  data() {
    return {
      options: [
        {
          value: "选项1",
          label: "黄金糕",
        },
        {
          value: "选项2",
          label: "双皮奶",
        },
      ],
      size: {
        height: 1080,
        width: 1920,
        scale: 1,
        top: 0,
        left: 0,
      },
      focusTap: 0,
      skuColumns: [
        { label: "序号", prop: "index", width: "80" },
        { label: "机台", prop: "sku", width: "80" },
        { label: "成品代号	", prop: "name", width: "120" },
        { label: "研发品号	", prop: "specs", width: "120" },
        { label: "阶段	", prop: "inventory", width: "120" },
        { label: "规格明细", prop: "sulfurTime", width: "180" },
        { label: "班制", prop: "sulfurTime", width: "80" },
        { label: "班别", prop: "sulfurTime", width: "80" },
        { label: "预定量", prop: "sulfurTime", width: "80" },
        { label: "即时预定量", prop: "sulfurTime", width: "80" },
        { label: "实际完成量", prop: "sulfurTime", width: "80" },
        { label: "量即时达成率", prop: "sulfurTime", width: "80" },
      ],
      skuTableData: [],
      shutdownColumns: [
        { label: "序号", prop: "index" },
        { label: "胎别", prop: "index" },
        { label: "停机机台编号", prop: "index" },
        { label: "责任单位", prop: "index" },
        { label: "停机原因", prop: "index" },
        { label: "开始停机时间", prop: "index" },
        { label: "结束停机时间", prop: "index" },
        { label: "停机时长(分钟)", prop: "index" },
        { label: "停机损失条数", prop: "index" },
        { label: "备注", prop: "index" },
      ],
      shutdownData: [],
      birthColumns: [
        { label: "序号", prop: "index" },
        { label: "加硫机台", prop: "index" },
        { label: "成品代号", prop: "index" },
        { label: "研发品号", prop: "index" },
        { label: "阶段", prop: "index" },
        { label: "版次", prop: "index" },
        { label: "规格明细", prop: "index" },
        { label: "早8库存", prop: "index" },
        { label: "成型量", prop: "index" },
        { label: "加硫量", prop: "index" },
        { label: "即时库存", prop: "index" },
        { label: "可加硫时间(时)", prop: "index" },
        { label: "单模产能", prop: "index" },
      ],
      birthData: [],
    };
  },
  mounted() {
    this.resize();
    window.addEventListener("resize", this.resize);
  },
  beforeDestroy() {
    window.removeEventListener("resize", this.resize);
  },
  methods: {
    // 表头样式
    headerCellStyle({ row, column, rowIndex, columnIndex }) {
      return {
        background: "rgba(2,138,220)",
        color: "white",
        fontWeight: "600",
        fontSize: "14px",
        borderRight: "1px solid #2e7a7a",
        textAlign: "center",
        padding: "16px 0",
      };
    },
    changeTap(tap) {
      this.focusTap = tap;
    },
    calcSize(width, height) {
      this.size.height = height;
      this.size.width = width;
      const xScale = height / 1080;
      const yScale = width / 1920;
      // console.log(yScale, xScale)

      if (xScale > yScale) {
        // 屏幕更高，上方留出空间，按更小的缩放
        this.size.scale = yScale;
        this.size.top = ((xScale - yScale) * 1080) / 2;
        this.size.left = 0;
        this.size.extraY = ((xScale - yScale) * 1080) / yScale;
        this.size.extraX = 0;
      } else {
        // 屏幕更扁，左右方留出空间，按更小的缩放
        this.size.scale = xScale;
        this.size.top = 0;
        this.size.left = ((yScale - xScale) * 1920) / 2;
        this.size.extraY = 0;
        this.size.extraX = ((yScale - xScale) * 1920) / xScale;
      }
    },
    resize() {
      let width = window.innerWidth;
      let height = window.innerHeight;
      if (this.fullScreen === false && this.$el) {
        width = this.$el.offsetWidth;
        height = this.$el.offsetHeight;
        // console.log(this.$el.offsetHeight, this.$el.innerWidth)
      }

      this.calcSize(width, height);
    },
  },
};
</script>
<style lang="scss" scoped>
@font-face {
  font-family: "DingTalk";
  src: url("../../../assets/largescreen/fonts/DingTalk JinBuTi.ttf");
}

@font-face {
  font-family: "DingTalk Sans";
  src: url("../../../assets/largescreen/fonts/DingTalk Sans.ttf");
}

@import "../../../assets/largescreen/style.scss";

.screen {
  background: url(../../../assets/largescreen/bg.png);
}
.full-screen {
  background: url(../../../assets/largescreen/bg.png);
}
.processes-block {
  background-image: url(../../../assets/largescreen/block.png);
}
.block-title {
  background-image: url(../../../assets/largescreen/block_title_bg.png);
}
.tap-container {
  display: flex;
  flex-direction: row;
  justify-content: center;
  font-size: 13px;
  align-items: center;
  margin-bottom: 16px;
}
.tap-item {
  display: flex;
  flex-direction: row;
  align-items: center;
  justify-content: center;
  height: 50px;
  width: 150px;
  font-size: 16px;
  cursor: pointer;
}
.tap-left {
  background: url(../../../assets/largescreen/tabbg_left.png) no-repeat left;

  color: #009ff4;
}
.tap-left-focus {
  background: url(../../../assets/largescreen/tabbg_left_focus.png) no-repeat
    left;

  color: #fcc924;
}
.tap-right {
  background: url(../../../assets/largescreen/tabbg_right.png) no-repeat right;

  color: #009ff4;
}
.tap-right-focus {
  background: url(../../../assets/largescreen/tabbg_right_focus.png) no-repeat
    right;

  color: #fcc924;
}
.tap-center {
  border: solid 1px #139ef8;
  box-shadow: rgb(19, 158, 248, 0.5) 0px 0px 10px inset;
  color: #009ff4;
  margin: 0 10px;
}
.tap-center-focus {
  border: solid 1px #fcc51f;
  box-shadow: rgb(252, 197, 31, 0.5) 0px 0px 10px inset;
  color: #fcc924;
  margin: 0 10px;
}
.content {
  padding: 0 20px;
  display: flex;
  flex-direction: column;
  flex: 1;
}
.search-content {
  display: flex;
  flex-direction: row;
  align-items: center;
  margin-bottom: 18px;
}
.search-item {
  display: flex;
  flex-direction: row;
  align-items: center;
  margin-right: 30px;
}
.search-name {
  font-size: 18px;
  color: #23cefd;
}

.production-table {
  width: 100%;
  overflow: hidden;
  border-collapse: collapse; /* 合并边框，避免双线 */

  box-shadow: 0 8px 18px rgba(30, 90, 90, 0.08);
  margin-top: 18px;
  margin-bottom: 18px;
}
.production-table th {
  color: white;
  font-weight: 540;
  font-size: 1.05rem;
  padding: 20px 18px;
  text-align: center;
  border: 1px solid #fff;
  background: rgba(2, 138, 220, 0.2);
}
.production-table th:first-child {
  width: 140px;
}
.production-table td:first-child {
  background: rgba(2, 138, 220, 0.2);
}
.production-table td {
  padding: 18px 16px;
  text-align: center;
  color: #fff;
  font-size: 1.04rem;
  font-weight: 460;
  border: 1px solid #fff;
}
.production-table td:last-child {
}
.production-table tr:last-child td {
}
.first-col {
  font-weight: 620;
  color: #fff;
  text-align: left;
  padding-left: 22px;
}
.rate-cell {
  font-weight: 600;
  border: solid 1px #fff;
}
.el-table::before {
  display: none !important;
  height: 0 !important;
  content: none !important;
}
.pageination-container{
  display: flex;
  justify-content: flex-end;
  padding: 0 20px;
}

</style>