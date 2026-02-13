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
        <div class="header-title">成型</div>
      </div>

      <div class="content">
        <div class="tap-container">
          <div
            :class="focusTap == 0 ? 'tap-action' : 'tap-item'"
            @click="changeTap(0)"
          >
            A区
          </div>
          <div
            :class="focusTap == 1 ? 'tap-action' : 'tap-item'"
            @click="changeTap(1)"
          >
            B区
          </div>
          <div class="tap-line" :style="{ left: moveLeft + 'px' }"></div>
        </div>
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
          <el-button
            type="primary"
            icon="el-icon-search"
            class="blue-button"
            @click="show"
            >搜索</el-button
          >
          <el-button type="danger" icon="el-icon-search">订单甘特图</el-button>
          <el-button type="warning" icon="el-icon-search">机台甘特图</el-button>
          <el-button type="success" icon="el-icon-search">规格甘特图</el-button>
        </div>
        <div>
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
      </div>
      <div class="pageination-container">
        <el-pagination
          background
          layout="prev, pager, next"
          class="my-pagination"
          :total="1000"
        >
        </el-pagination>
      </div>
    </div>
    <div class="cover-container" v-if="showModal">
      <div class="cover-back" @click="hideModal"></div>
      <div class="cover-content">
        <div class="cover-title">成型备料情况</div>
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
          <el-button type="primary" icon="el-icon-search" class="blue-button"
            >搜索</el-button
          >
        </div>
        <div>
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
      moveLeft: 0,
      showModal: false ,
      activeName: "first",
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
        { label: "研发品号	", prop: "name", width: "120" },
        { label: "当前作业规格	", prop: "specs", width: "120" },
        { label: "当前排程预产量	", prop: "inventory", width: "120" },
        { label: "当前排程完成量", prop: "sulfurTime", width: "180" },
        { label: "总预定量", prop: "sulfurTime", width: "80" },
        { label: "总完成量", prop: "sulfurTime", width: "80" },
        { label: "即时达成率", prop: "sulfurTime", width: "80" },
        { label: "达成率", prop: "sulfurTime", width: "80" },
        { label: "实际完成量", prop: "sulfurTime", width: "80" },
        { label: "备料情况", prop: "sulfurTime", width: "80" },
      ],
      skuTableData: [],
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
    show() {
      this.showModal = true;
    },
    hideModal() {
      this.showModal = false;
    },
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
      if (tap == 0) {
        this.moveLeft = 0;
      } else {
        this.moveLeft = 120;
      }
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
  position: relative;
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

.content {
  padding: 0 20px;
  display: flex;
  flex-direction: column;
  flex: 1;
  // margin-top: 18px;
}
.tap-container {
  margin-bottom: 18px;
  border-bottom: solid 2px #1959a2;
  height: 60px;
  position: relative;

  width: 100%;
  display: flex;
  flex-direction: row;
}
.tap-action {
  width: 120px;
  height: 60px;
  display: flex;
  flex-direction: row;
  align-items: center;
  justify-content: center;
  color: #23cefd;
  font-size: 18px;
  cursor: pointer;
}
.tap-item {
  width: 120px;
  height: 60px;
  display: flex;
  flex-direction: row;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 18px;
  cursor: pointer;
}
.tap-line {
  width: 120px;
  height: 2px;
  background: #23cefd;
  position: absolute;
  bottom: 0;
  transition: all 0.3s;
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

.el-table::before {
  display: none !important;
  height: 0 !important;
  content: none !important;
}
.pageination-container {
  display: flex;
  justify-content: flex-end;
  padding: 0 20px;
}
.blue-button {
  background: #009ff4;
}
.el-pagination.is-background .el-pager li.active {
  background-color: #fc813b !important;
}
.cover-container {
  position: absolute;
  top: 0;
  left: 0;
  bottom: 0;
  right: 0;
  display: flex;
  flex-direction: row;
  align-items: center;
  justify-content: center;
}
.cover-back {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.5);
}
.cover-content {
  width: 90%;
  height: 450px;
  background-image: radial-gradient(#1a5ce2, #022457);
  border: solid 1px #2e9fff;
  box-shadow: 0px 0px 70px #2e9fff;
  z-index: 1002;
  border-radius: 4px;
  padding: 10px;
}
.cover-title {
  font-size: 18px;
  color: #23cefd;
  margin-bottom: 18px;
}
</style>