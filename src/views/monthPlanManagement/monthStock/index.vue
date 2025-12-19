
<template>
  <basic-container>
    <page-table
      tableRef="MonthPlanMonthStockMainTable"
      :calcHeight="true"
      v-loading="loading"
      :columns="columns"
      :searchColumns="searchColumns"
      :data="data"
      :page="page"
      :search="search"
      @refresh="getList"
      @search="handleSearch"
      @pageChange="handlePageChange"
      @sort-change="handleSortChange"
      @selection-change="handleSelectionChange"
      :showSummary="false"
      :selectArea="false"
    >
      <template slot="header">
        <el-button
          type="primary"
          plain
          v-hasPermi="['monthplan:monthStock:add']"
          @click="handleAdd"
          >{{ $t("common.button.add") }}</el-button
        >
        <!-- <el-button
          type="primary"
          plain
          v-hasPermi="['monthplan:monthStock:edit']"
          @click="handleEdit(selection[0])"
          >{{ $t("ui.frame.btn.modify") }}</el-button
        > -->
        <el-button
          type="danger"
          plain
          :disabled="selection.length === 0"
          v-hasPermi="['monthplan:monthStock:remove']"
          @click="handleDelete(selection)"
          >{{ $t("ui.frame.btn.delete") }}</el-button
        >
        <el-button
          v-hasPermi="['monthplan:monthStock:import']"
          @click="$refs.tltUpload.handleImport()"
          >{{ $t("ui.frame.btn.import") }}</el-button
        >
        <el-button
          @click="handleExport"
          v-hasPermi="['monthplan:monthStock:export']"
          >{{ $t("ui.frame.btn.export") }}</el-button
        >
        <el-button
          @click="handleCraw"
          v-hasPermi="['monthplan:monthStock:craw']"
          >{{ $t("ui.data.column.monthStock.craw") }}</el-button
        >
      </template>
    </page-table>
    <!-- <el-button style="display: none" ref="hidePopoverBtnRef"></el-button> -->
    <tlt-upload
      ref="tltUpload"
      downloadUrl="/monthplan/monthStock/importTemplate"
      uploadUrl="/monthplan/monthStock/importData"
      @uploadSuccess="getList"
    />
    <infoDialog ref="infoRef" @success="getList" />
  </basic-container>
</template>
<script>
//lib
import moment from "moment";
//utils
import { downloadLink } from "@/utils/request";
//interface
import {
  listMonthStock,
  removeMonthStock,
  editMonthStock,
  crawMonthStock,
} from "@/api/monthplan/monthStock";
//components
import tltUpload from "@/components/tltUpload/tltUpload.vue";

import infoDialog from "./components/infoDialog.vue";

export default {
  name: "MonthStock",
  components: {
    tltUpload,
    infoDialog,
  },
  dicts: [
    "biz_factory_name",
    "biz_stor_type",
    // "biz_channel_type",
    // "biz_brand_type",
  ],
  provide() {
    return {
      parentDict: this.dict,
    };
  },
  data() {
    return {
      loading: false,
      data: [],
      selection: [],
      page: {
        current: 1,
        pageSize: 20,
        total: 0,
      },
      sort: {},
      search: {},
      query: {},
      importDefaultValue: {},
      importRules: {},
    };
  },
  computed: {
    columns() {
      let columns = [
        { type: "selection", fixed: "left" },
        {
          prop: "year",
          label: this.$t("ui.data.column.monthStock.year"),
        },
        {
          prop: "month",
          label: this.$t("ui.data.column.monthStock.month"),
        },
        {
          prop: "factoryCode",
          label: this.$t("ui.data.column.monthStock.factoryCode"),
          formatter: (row) => {
            return this.selectDictLabel(
              this.dict.type.biz_factory_name,
              row.factoryCode
            );
          },
        },
        {
          prop: "locationType",
          label: this.$t("ui.data.column.monthStock.locationType"),
          formatter: (row) => {
            return this.selectDictLabel(
              this.dict.type.biz_stor_type,
              row.locationType
            );
          },
        },
        {
          prop: "productCode",
          label: this.$t("ui.data.column.monthStock.productCode"),
        },
        {
          prop: "productDesc",
          label: this.$t("ui.data.column.monthStock.productDesc"),
          width: 250,
        },
        // {
        //   prop: "channel",
        //   label: this.$t("ui.data.column.monthStock.channel"),
        //   formatter: (row, column, value) => {
        //     return this.selectDictLabel(this.dict.type.biz_channel_type, value);
        //   },
        // },
        // {
        //   prop: "brand",
        //   label: this.$t("ui.data.column.monthStock.brand"),
        //   formatter: (row, column, value) => {
        //     return this.selectDictLabel(this.dict.type.biz_brand_type, value);
        //   },
        // },
        {
          prop: "specifications",
          label: this.$t("ui.data.column.monthStock.specifications"),
          width: 120,
        },
        {
          prop: "pattern",
          label: this.$t("ui.data.column.monthStock.pattern"),
          width: 140,
        },
        {
          prop: "proSize",
          label: this.$t("ui.data.column.monthStock.proSize"),
        },
        {
          prop: "stockQty",
          label: this.$t("ui.data.column.monthStock.stockQty"),
          type: "number"
        },
        {
          prop: "remark",
          label: this.$t("common.remark"),
          minWidth: 100,
        },
      ];
      if (this.$auth.hasPermi("monthplan:monthStock:edit")) {
        columns.push({
          align: "center",
          halign: "center",
          label: this.$t("ui.data.btn.option"),
          render: ({ row }) => {
            return (
              <el-button
                class="minus"
                type="success"
                onClick={() => this.handleEdit(row)}
              >
                {this.$t("ui.frame.btn.update")}
              </el-button>
            );
          },
        });
      }

      return columns;
    },
    searchColumns() {
      return [
        {
          prop: "yearMonth",
          label: this.$t("ui.data.column.monthStock.yearMonth"),
          type: "date",
          dateType: "month",
          valueFormat: "yyyy-MM",
          clearable: false,
        },
        {
          prop: "factoryCode",
          label: this.$t("ui.data.column.monthStock.factoryCode"),
          type: "select",
          dictData: this.dict.type.biz_factory_name,
        },
        {
          prop: "productCode",
          label: this.$t("ui.data.column.monthStock.productCode"),
        },
        {
          prop: "productDesc",
          label: this.$t("ui.data.column.monthStock.productDesc"),
        },
        {
          prop: "locationType",
          label: this.$t("ui.data.column.monthStock.locationType"),
          type: "select",
          dictData: this.dict.type.biz_stor_type,
        },
        {
          prop: "specifications",
          label: this.$t("ui.data.column.monthStock.specifications"),
        },
        {
          prop: "pattern",
          label: this.$t("ui.data.column.monthStock.pattern"),
        },
      ];
    },
  },
  methods: {
    handleAdd() {
      if (this.$refs.infoRef) {
        this.$refs.infoRef.show();
      }
    },
    handleEdit(row) {
      if (this.$refs.infoRef) {
        this.$refs.infoRef.show(row);
      }
    },
    handleDelete(rows) {
      this.$confirm(this.$t("common.confirm.delete"), {
        type: "warning",
      }).then(() => {
        const ids = rows.map((row) => row.id).join(",");
        removeMonthStock({ ids }).then((data) => {
          this.$modal.msgSuccess(data.msg);
          this.$set(this.page, "current", 1);
          this.getList();
        });
      });
    },
    handleCraw(rows) {
      this.$confirm(this.$t("ui.data.column.monthStock.msg.craw"), {
        type: "warning",
      }).then(() => {
        crawMonthStock({}).then((data) => {
          this.$modal.msgSuccess(data.msg);
          this.$set(this.page, "current", 1);
          this.getList();
        });
      });
    },
    handleChangeStatus(status, row) {
      console.log(status);
      let label =
        status === "0"
          ? this.$t("ui.biz.alter.isOpen")
          : this.$t("ui.biz.alter.isStop");

      this.$confirm(label, {
        type: "warning",
      }).then(async () => {
        try {
          this.loading = true;
          const res = await editMonthStock({
            ...row,
            status,
          });
          this.$modal.msgSuccess(res.msg);
          this.getList();
        } catch (error) {
          this.loading = false;
        }
      });
    },

    handleSearch(data) {
      this.query = data;
      this.$set(this.page, "current", 1);
      this.getList();
    },
    handlePageChange(current, pageSize) {
      this.$set(this.page, "current", current);
      this.$set(this.page, "pageSize", pageSize);
      this.getList();
    },
    handelSuccess() {
      this.getList();
    },
    handleSortChange({ column, prop, order }) {
      if (order) {
        this.sort = {
          orderByColumn: prop,
          isAsc: order == "ascending" ? "asc" : "desc",
        };
      } else {
        //默认排序
        this.sort = {};
      }
      this.getList();
    },

    handleExport() {
      downloadLink("/monthplan/monthStock/export", this.formatParams(false));
    },

    handleSelectionChange(rows) {
      this.selection = rows;
    },

    // utils
    updateTableHeaderlabel() {
      //  TODO 更新表头标题
    },

    formatParams(hasPage = true) {
      const params = {
        ...this.query,
        ...this.sort,
      };

      if (hasPage) {
        params.pageSize = this.page.pageSize;
        params.pageNum = this.page.current;
      }

      if (params.yearMonth) {
        let arr = params.yearMonth.split("-");
        params.year = arr[0];
        params.month = arr[1];
        params.yearMonth = undefined;
      }

      if (params.createTime && params.createTime[0]) {
        params.createTimeStart = params.createTime[0];
        params.createTimeEnd = params.createTime[1];
        params.createTime = undefined;
      }

      return params;
    },
    // api
    async getList() {
      try {
        this.loading = true;
        const data = await listMonthStock(this.formatParams());
        console.log(data);
        this.data = data.rows;
        this.page.total = data.total;
      } catch (error) {
        console.error(error);
      } finally {
        this.loading = false;
      }
    },
  },
  created() {
    const date = moment();
    let defaultParams = {
      yearMonth: date.format("yyyy-MM"),
    };
    this.search = {
      ...defaultParams,
    };

    this.query = {
      ...defaultParams,
    };
  },
  activated() {
    this.getList();
  },
};
</script>
<style lang="scss" scoped>
.more-btn {
  margin: 2px 0;
  width: 100%;
}
</style>
