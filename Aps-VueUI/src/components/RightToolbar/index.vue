<template>
  <div class="top-right-btn" :style="style">
    <el-row>
      <el-tooltip
        class="item"
        effect="dark"
        :content="showSearch ? $t('common.button.hideSearch') : $t('common.button.showSearch')"
        placement="top"
        v-if="search"
      >
        <el-button
          size="mini"
          circle
          icon="el-icon-search"
          @click="toggleSearch()"
        />
      </el-tooltip>
      <el-tooltip class="item" effect="dark" :content="$t('common.button.refresh')" placement="top">
        <el-button
          size="mini"
          circle
          icon="el-icon-refresh"
          @click="refresh()"
        />
      </el-tooltip>
      <el-tooltip
        class="item"
        effect="dark"
        content="显隐列"
        placement="top"
        v-if="columns"
      >
        <el-button
          size="mini"
          circle
          icon="el-icon-menu"
          @click="showColumn()"
        />
      </el-tooltip>
      <table-column-adjust :tableRef="tableRef"/>
    </el-row>
    <el-dialog :title="title" :visible.sync="open" append-to-body>
      <el-transfer
        :titles="['显示', '隐藏']"
        v-model="value"
        :data="columns"
        @change="dataChange"
      ></el-transfer>
    </el-dialog>
  </div>
</template>
<script>
export default {
  name: "RightToolbar",
  data() {
    return {
      // 显隐数据
      value: [],
      // 弹出层标题
      title: "显示/隐藏",
      // 是否显示弹出层
      open: false,
    };
  },
  props: {
    showSearch: {
      type: Boolean,
      default: true,
    },
    columns: {
      type: Array,
    },
    search: {
      type: Boolean,
      default: true,
    },
    gutter: {
      type: Number,
      default: 10,
    },
    tableRef: {
      type: String,
    },
  },
  computed: {
    style() {
      const ret = {};
      if (this.gutter) {
        ret.marginRight = `${this.gutter / 2}px`;
      }
      return ret;
    },
  },
  created() {
    // 显隐列初始默认隐藏列
    for (let item in this.columns) {
      if (this.columns[item].visible === false) {
        this.value.push(parseInt(item));
      }
    }
  },
  methods: {
    configColumn() {
      let parent = this.$parent;
      let table = null;
      while (parent) {
        if (parent?.$refs?.[this.tableRef]) {
          table = parent.$refs[this.tableRef];
          break;
        }
        parent = parent.$parent;
      }
      if (!table) {
        console.error(`表格${this.tableRef}未找到`);
        return;
      }
      if (!table?.showTableConfig) {
        console.error(`ref:${this.tableRef} 非 t-table 组件`);
        return;
      }
      table.showTableConfig();
    },
    // 搜索
    toggleSearch() {
      this.$emit("update:showSearch", !this.showSearch);
    },
    // 刷新
    refresh() {
      this.$emit("queryTable");
    },
    // 右侧列表元素变化
    dataChange(data) {
      for (let item in this.columns) {
        const key = this.columns[item].key;
        this.columns[item].visible = !data.includes(key);
      }
    },
    // 打开显隐列dialog
    showColumn() {
      this.open = true;
    },
  },
};
</script>
<style lang="scss" scoped>
::v-deep .el-transfer__button {
  border-radius: 50%;
  padding: 12px;
  display: block;
  margin-left: 0px;
}
::v-deep .el-transfer__button:first-child {
  margin-bottom: 10px;
}
</style>
