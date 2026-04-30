
<template>
  <basic-container class="mdm-stock-up-plan" v-loading="loading">
    <div class="computed-search">
      <el-form :model="form" inline>
        <el-form-item :label="$t('轮胎类型')">
          <el-select v-model="form.tireType" clearable>
            <el-option
              v-for="item in dict.type.TIRE_TYPE"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            ></el-option> </el-select
        ></el-form-item>
        <el-form-item :label="$t('月份')">
          <el-select v-model="form.monthRange">
            <el-option
              v-for="item in dict.type.month_range"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            ></el-option>
          </el-select>
        </el-form-item>

        <el-form-item>
          <!--<el-button type="primary" @click="handleCompute">计算备货</el-button>-->
          <el-button type="primary" @click="handleCreate">确定生成</el-button>
        </el-form-item>
      </el-form>
    </div>
    <div class="computed-content">
      <page-table
        tableRef="MonthlydataMpHistoryQtyMainTable"
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
          <!-- <el-button
            type="primary"
            plain
            v-hasPermi="['monthplan:mdmStockUpPlan:create']"
            @click="handleCreate"
            >{{ $t("生成备货计划") }}</el-button
          > -->
          <!-- <el-button
          type="warning"
          v-hasPermi="['monthplan:mdmStockUpPlan:add']"
          @click="handleAdd"
          >{{ $t("ui.frame.btn.add") }}</el-button
        > -->
          <!-- <el-button
          type="warning"
          v-hasPermi="['monthplan:mdmStockUpPlan:edit']"
          @click="handleEdit(selection[0])"
          >{{ $t("ui.frame.btn.modify") }}</el-button
        > -->
          <!-- <el-button
          type="warning"
          v-hasPermi="['monthplan:mdmStockUpPlan:edit']"
          @click="handleDelete(selection)"
          >{{ $t("ui.frame.btn.delete") }}</el-button
        > -->
          <!-- <el-button
            v-hasPermi="['monthplan:mdmStockUpPlan:import']"
            @click="$refs.tltUpload.handleImport()"
            >{{ $t("ui.frame.btn.import") }}</el-button
          > -->
          <!-- <el-button
          @click="handleExport"
          v-hasPermi="['monthplan:mdmStockUpPlan:export']"
          >{{ $t("ui.frame.btn.export") }}</el-button
        > -->
        </template>
      </page-table>
    </div>
    <!-- <el-button style="display: none" ref="hidePopoverBtnRef"></el-button> -->
    <!-- <tlt-upload
      ref="tltUpload"
      downloadUrl="/monthplan/mdmStockUpPlan/importTemplate"
      uploadUrl="/monthplan/mdmStockUpPlan/importData"
      @uploadSuccess="getList"
    /> -->
    <!-- <infoDialog ref="infoRef" @success="getList" /> -->
  </basic-container>
</template>
<script>
//lib
import moment from "moment";
//utils
import { downloadLink } from "@/utils/request";
//interface
import { createStockUpPlan } from "@/api/monthplan/mdmStockUpPlan";
import { queryCalcStocking } from "@/api/monthplan/mpHistorySaleQty";
//components
// import tltUpload from "@/components/tltUpload/tltUpload.vue";

// import infoDialog from "./components/infoDialog.vue";

export default {
  name: "mdmStockUpPlanCreate",
  components: {
    // tltUpload,
    // infoDialog,
  },
  dicts: ["TIRE_TYPE", "month_range", "biz_factory_name", "biz_stor_type"],
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
      form: {},
      search: {},
      query: {},
      importDefaultValue: {},
      importRules: {},
    };
  },
  computed: {
    columns() {
      let columns = [
        // { type: "selection", fixed: "left" },
        {
          prop: "year",
          label: this.$t("ui.data.column.mouldusestatus.year"),
        },
        {
          prop: "month",
          label: this.$t("ui.data.column.mouldusestatus.month"),
        },

        {
          prop: "factoryCode",
          label: this.$t("ui.data.column.mouldusestatus.factoryCode"),
          formatter: (row) => {
            return this.selectDictLabel(
              this.dict.type.biz_factory_name,
              row.factoryCode
            );
          },
        },
        {
          prop: "locationType",
          label: this.$t("库位类别"),
          formatter: (row) => {
            return this.selectDictLabel(
              this.dict.type.biz_stor_type,
              row.locationType
            );
          },
        },
        {
          prop: "productCode",
          label: this.$t("ui.data.column.mdmStockUpPlan.productCode"),
        },
        {
          prop: "productDesc",
          label: this.$t("ui.data.column.mouldusestatus.specifications"),
          width: 250,
        },
        {
          prop: "pattern",
          label: this.$t("ui.data.column.mouldusestatus.pattern"),
          width: 140,
        },
        {
          prop: "proSize",
          label: this.$t("ui.data.column.mdmStockUpPlan.proSize"),
        },
        {
          prop: "specifications",
          label: this.$t("ui.data.column.mdmStockUpPlan.specifications"),
          width: 120,
        },
        {
          prop: "saleQty",
          label: this.$t("ui.data.column.mdmStockUpPlan.saleQty"),
        },
        {
          prop: "augQty",
          label: this.$t("ui.data.column.mdmStockUpPlan.augQty"),
        },
        {
          prop: "factorValue",
          label: this.$t("ui.data.column.mdmStockUpPlan.factorValue"),
        },
        {
          prop: "stockQty",
          label: this.$t("ui.data.column.mdmStockUpPlan.stockQty"),
        },
        // {
        //   prop: "remark",
        //   label: this.$t("ui.data.column.mouldusestatus.remark"),
        // },
        // {
        //   prop: "createByName",
        //   label: this.$t("common.createByName"),
        // },
        // {
        //   prop: "createTime",
        //   label: this.$t("common.createTime"),
        //   width: 180,
        // },
      ];

      return columns;
    },
    searchColumns() {
      return undefined;
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
        console.log(ids);
        removeArea({ ids }).then((data) => {
          this.$modal.msgSuccess(data.msg);
          this.$set(this.page, "current", 1);
          this.getList();
        });
      });
    },
    handleHistoryQuery() {},

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
      downloadLink(
        "/monthplan/mdmStockUpPlan/export",
        this.formatParams(false)
      );
    },
    handleSelectionChange(rows) {
      this.selection = rows;
    },
    handleCompute() {
      if (!this.form.monthRange) {
        return;
      }
      this.getList();
    },
    handleCreate() {
      if (!this.form.monthRange) {
        return;
      }
      this.$confirm(this.$t("确认生成备货计划"), {
        type: "warning",
      }).then(() => {
        this.loading = true;
        createStockUpPlan(this.form)
          .then(async (data) => {
            this.$modal.msgSuccess(data.msg);
            this.$set(this.page, "current", 1);
            await this.getList();
            // this.close
            this.loading = false;
          })
          .catch(() => {
            this.loading = false;
          });
      });
    },

    // utils

    formatParams(hasPage = true) {
      const params = {
        ...this.form,
        // ...this.sort,
      };

      if (hasPage) {
        params.pageSize = this.page.pageSize;
        params.pageNum = this.page.current;
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
        const data = await queryCalcStocking(this.formatParams());
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
  created() {},
  activated() {
    // this.getList();
  },
};
</script>
<style lang="scss" scoped>
.mdm-stock-up-plan {
  display: flex;
  flex-direction: column;
  .computed-search {
    flex: 0 0 auto;
  }
  .computed-content {
    flex: 1 1 auto;
  }
}

.more-btn {
  margin: 2px 0;
  width: 100%;
}
</style>
