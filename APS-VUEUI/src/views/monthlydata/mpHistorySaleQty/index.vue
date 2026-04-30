
<template>
  <basic-container>
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
        <el-button
          type="primary"
          plain
          v-hasPermi="['monthplan:mpHistorySaleQty:add']"
          @click="handleAdd"
          >{{ $t("ui.frame.btn.add") }}</el-button
        >
        <!-- <el-button
          type="primary"
          plain
          v-hasPermi="['monthplan:mpHistorySaleQty:edit']"
          @click="handleEdit(selection[0])"
          >{{ $t("ui.frame.btn.modify") }}</el-button
        > -->
        <!-- <el-button
          type="primary"
          plain
          v-hasPermi="['monthplan:mpHistorySaleQty:edit']"
          @click="handleDelete(selection)"
          >{{ $t("ui.frame.btn.delete") }}</el-button
        > -->
        <el-button
          v-hasPermi="['monthplan:mpHistorySaleQty:import']"
          @click="$refs.tltUpload.handleImport()"
          >{{ $t("ui.frame.btn.import") }}</el-button
        >
        <el-button
          v-hasPermi="['monthplan:mpHistorySaleQty:importMonth']"
          @click="$refs.tltUpload2.handleImport()"
          >{{ $t("ui.data.column.mpHistorySaleQty.importMonth") }}</el-button
        >
        <el-button
          @click="handleExport"
          v-hasPermi="['monthplan:mpHistorySaleQty:exportYear']"
          >{{ $t("ui.frame.btn.export") }}</el-button
        >
      </template>
    </page-table>
    <!-- <el-button style="display: none" ref="hidePopoverBtnRef"></el-button> -->
    <tlt-upload
      ref="tltUpload"
      downloadUrl="/monthplan/mpHistorySaleQty/importTemplate"
      uploadUrl="/monthplan/mpHistorySaleQty/importData"
      @uploadSuccess="getList"
    />
    <tlt-upload
      ref="tltUpload2"
      downloadUrl="/monthplan/mpHistorySaleQty/importTemplate4Month"
      uploadUrl="/monthplan/mpHistorySaleQty/importMonthData"
      @uploadSuccess="getList"
    />
    <!-- <infoDialog ref="infoRef" @success="getList" /> -->
  </basic-container>
</template>
<script>
//lib
import moment from "moment";
//utils

//interface
import {
  listMpHistorySaleQty,
  exportData,
} from "@/api/monthplan/mpHistorySaleQty";
//components
import tltUpload from "@/components/tltUpload/tltUpload.vue";

// import infoDialog from "./components/infoDialog.vue";

export default {
  name: "MpHistorySaleQty",
  components: {
    tltUpload,
    // infoDialog,
  },
  dicts: ["biz_stor_type", "biz_factory_name"],
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
      search: {
        mainPlanMonth: "",
      },
      query: {
        mainPlanMonth: "",
      },
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
          label: this.$t("common.year"),
        },
        {
          prop: "month",
          label: this.$t("common.month"),
        },

        {
          prop: "factoryCode",
          label: this.$t("ui.data.column.mpHistorySaleQty.factoryCode"),
          formatter: (row) => {
            return this.selectDictLabel(
              this.dict.type.biz_factory_name,
              row.factoryCode
            );
          },
        },
        {
          prop: "locationType",
          label: this.$t("ui.data.column.mpHistorySaleQty.locationType"),
          formatter: (row) => {
            return this.selectDictLabel(
              this.dict.type.biz_stor_type,
              row.locationType
            );
          },
        },
        {
          prop: "saleQty",
          label: this.$t("ui.data.column.mpHistorySaleQty.saleQty"),
          type: "number",
        },
        {
          prop: "productCode",
          label: this.$t("ui.data.column.mpHistorySaleQty.productCode"),
        },
        {
          prop: "productDesc",
          label: this.$t("ui.data.column.mpHistorySaleQty.productDesc"),
          width: 250,
        },
        {
          prop: "remark",
          label: this.$t("common.remark"),
          minWidth: 100,
        },
        {
          prop: "createByName",
          label: this.$t("common.createByName"),
        },
        {
          prop: "createTime",
          label: this.$t("common.createTime"),
          width: 180,
        },
      ];

      return columns;
    },
    searchColumns() {
      return [
        {
          label: this.$t("common.year"),
          prop: "year",
          type: "date",
          dateType: "year",
          valueFormat: "yyyy",
        },
        {
          label: this.$t("common.month"),
          prop: "month",
          type: "date",
          dateType: "month",
          valueFormat: "MM",
        },
        {
          prop: "factoryCode",
          label: this.$t("ui.data.column.mpHistorySaleQty.factoryCode"),
          type: "select",
          dictData: this.dict.type.biz_factory_name,
        },
        {
          prop: "locationType",
          label: this.$t("ui.data.column.mpHistorySaleQty.locationType"),
          type: "select",
          dictData: this.dict.type.biz_stor_type,
        },
        {
          prop: "productCode",
          label: this.$t("ui.data.column.mpHistorySaleQty.productCode"),
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
        const ids = row.id;
        console.log(ids);
        removeArea({ ids }).then((data) => {
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
          const res = await editMachine({
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

    // 转机台弹窗
    handleChangeMachine() {
      if (this.$refs.changeMachineRef) {
        let row = this.selection[0];
        this.$refs.changeMachineRef.show(row);
      }
    },

    handleGotoMachineGant() {
      this.$router.push("/curingPlan/machineGantChart");
    },
    handleGotoSpecDescGant() {
      this.$router.push("/curingPlan/specDescGantChart");
    },
    // 调量
    handleChangePlan() {
      if (this.$refs.changePlanRef) {
        let row = this.selection[0];
        this.$refs.changePlanRef.show(row);
      }
    },

    handleQuery() {},
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
    handleSelectionChange(rows) {
      this.selection = rows;
    },
    handleExport() {
      exportData(this.formatParams(false));
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
        const data = await listMpHistorySaleQty(this.formatParams());
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
