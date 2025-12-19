<template>
  <select-dialog
    ref="dialogRef"
    :title="getTitle()"
    :value="showValue"
    @confirm="handleConfirm"
    @clear="handleClear"
    :disabled="disabled"
    @show="handleShow"
    @cancel="handleCancel"
  >
    <!-- <div class="header">
      <el-input
        v-model="searchKey"
        :placeholder="$t(`名称`)"
        clearable
      ></el-input>
      <el-button class="btn" type="primary" @click="handleSearch">{{
        $t(`dialog.search`)
      }}</el-button>
    </div> -->
    <div class="table-container" v-loading="loading">
      <page-table
        :calcHeight="true"
        ref="tableRef"
        :columns="columns"
        :searchColumns="searchColumns"
        :data="data"
        :toolbar="false"
        :page="page"
        :highlight-current-row="true"
        @current-change="handleCurrentChange"
        @row-dblclick="handleDbClick"
        @pageChange="handlePageChange"
        @selection-change="handleSelectionChange"
        @search="handleSearch"
      >
      </page-table>
    </div>
  </select-dialog>
</template>

<script>
import { deepClone } from "@/utils";

import selectDialog from "@/components/Table/SelectDialog.vue";
import { listProductinfo } from "@/api/lean/productinfo";
export default {
  components: { selectDialog },

  model: {
    prop: "value",
    event: "change",
  },
  props: {
    value: String | Number,
    title: String,
    disabled: Boolean,
    label: String,
    multiple: {
      type: Boolean,
      default: false,
    },
  },
  data() {
    return {
      searchKey: "",
      searchColumns: [
        {
          // compellation: "姓名"
          label: this.$t("ui.data.column.lean.productinfo.productCode"),
          prop: "productCode",
        },
      ],
      filterKey: "",
      page: {
        current: 1,
        pageSize: 10,
        total: 0,
      },
      query: {},
      showValue: "",
      loading: false,
      valueProp: "productCode",
      labelProp: "productCode",
      data: [],
    };
  },
  computed: {
    columns: function () {
      const list = [
        {
          prop: "productCode",
          label: this.$t("ui.data.column.lean.productinfo.productCode"),
        },
        {
          prop: "productDesc",
          label: this.$t("ui.data.column.lean.productinfo.productDesc"),
          width: 250,
        },
        {
          prop: "proSize",
          label: this.$t("ui.data.column.lean.productinfo.proSize"),
        },
        {
          prop: "specifications",
          label: this.$t("ui.data.column.lean.productinfo.specifications"),
          width: 120,
        },
        {
          prop: "pattern",
          label: this.$t("ui.data.column.lean.productinfo.pattern"),
          width: 140,
        },
        {
          prop: "brand",
          label: this.$t("ui.data.column.lean.productinfo.brand"),
        },
      ];
      if (this.multiple) {
        list.unshift({
          type: "selection",
        });
      }

      return list;
    },
  },

  watch: {
    value: function (val) {
      if (!val) {
        this.showValue = "";
      }
    },

    label: function (val) {
      if (!!val) {
        this.showValue = val;
      } else {
        this.showValue = this.value;
      }
    },
  },

  methods: {
    async getList() {
      try {
        this.loading = true;
        const data = await listProductinfo(this.formatParams());
        this.data = data.rows;
        this.page.total = data.total;
      } catch (error) {
        console.log(error);
      } finally {
        this.loading = false;
      }
    },

    //
    formatParams() {
      return {
        pageSize: this.page.pageSize,
        pageNum: this.page.current,
        ...this.query,
        // userName: this.filterKey,
        status: 0, //过滤，只显示启用的用户
      };
    },
    getTitle() {
      return this.title ? this.title : "物料信息";
    },

    /**
     * 重置
     */
    handleSearchReset() {
      this.searchKey = "";
      let query = {};
      this.handleSearch(query);
    },
    handleSearch(query) {
      this.query = query;
      // this.filterKey = this.searchKey;

      this.$set(this.page, "current", 1);
      this.getList();
    },
    handlePageChange(current, pageSize) {
      this.$set(this.page, "current", current);
      this.$set(this.page, "pageSize", pageSize);
      this.getList();
    },

    handleChange(val, row) {
      this.$emit("change", val, row);
    },

    handleShow() {
      this.getList();
    },
    handleCancel() {
      this.data = [];
      this.query = {};
      this.searchKey = "";
      this.filterKey = "";
      this.page = {
        current: 1,
        pageSize: 10,
        total: 0,
      };
    },
    handleCurrentChange(row) {
      this.currentRow = row;
    },
    handleDbClick(row) {
      this.currentRow = row;
      this.handleConfirm(() => {
        this.$refs.dialogRef.hide();
      });
    },
    handleSelectionChange(rows) {
      this.selection = rows;
    },
    handleClear() {
      if (this.$refs.tableRef) {
        const tableRef = this.$refs.tableRef.getTableRef();
        tableRef && tableRef.setCurrentRow();
      }
      this.showValue = "";
      this.$emit("change", undefined);
    },
    handleConfirm(done) {
      if (!this.multiple) {
        if (this.currentRow) {
          this.showValue = this.currentRow[this.labelProp] + "";
          this.$emit("updateValue", this.currentRow[this.valueProp]);
          this.$emit("change", this.currentRow[this.valueProp], {
            ...this.currentRow,
          });
        }
        done();
      } else {
        if (this.selection.length) {
          const ids = this.selection
            .map((item) => item[this.valueProp])
            .join(",");
          this.showValue = this.selection
            .map((item) => item[this.labelProp])
            .join(",");
          this.$emit("updateValue", ids);
          this.$emit("change", ids, deepClone(this.selection));
        }
        done();
      }
    },
  },
  mounted() {
    if (this.label) {
      this.showValue = this.label;
    } else {
      this.showValue = this.value;
    }
    // this.getList();
  },
};
</script>
<style lang="scss" scoped>
.header {
  margin-bottom: 10px;
  display: flex;
  justify-content: flex-start;
  align-items: center;
  .input {
    max-width: 300px;
    flex: 1 1 auto;
  }
  .btn {
    margin-left: 10px;
    flex: 0 0 auto;
  }
}
.table-container {
  height: 450px;
}
</style>
