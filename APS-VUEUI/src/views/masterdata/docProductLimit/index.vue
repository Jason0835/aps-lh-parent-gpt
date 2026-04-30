
<template>
  <basic-container>
    <page-table
      tableRef="cxMachineMsgMainTable"
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
          type="warning"
          v-hasPermi="['fac:docVulcanizingLine:add']"
          @click="handleAdd"
          >{{ $t("ui.frame.btn.add") }}</el-button
        >
        <!-- <el-button
          type="warning"
          v-hasPermi="['fac:docVulcanizingLine:edit']"
          @click="handleEdit(selection[0])"
          >{{ $t("ui.frame.btn.modify") }}</el-button
        > -->
        <el-button
          type="warning"
          v-hasPermi="['fac:docVulcanizingLine:edit']"
          @click="handleDelete(selection)"
          >{{ $t("ui.frame.btn.delete") }}</el-button
        >
        <el-button
          type="warning"
          v-hasPermi="['fac:docVulcanizingLine:import']"
          @click="$refs.tltUpload.handleImport()"
          >{{ $t("ui.frame.btn.import") }}</el-button
        >
        <el-button
          type="primary"
          @click="handleExport"
          v-hasPermi="['fac:docVulcanizingLine:export']"
          >{{ $t("ui.frame.btn.export") }}</el-button
        >
      </template>
    </page-table>
    <!-- <el-button style="display: none" ref="hidePopoverBtnRef"></el-button> -->
    <tlt-upload
      ref="tltUpload"
      downloadUrl="/fac/docVulcanizingLine/importTemplate"
      uploadUrl="/fac/docVulcanizingLine/importData"
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
// import { listMachine, editMachine } from "@/api/fac/docMoldingMachineCls";
//components
import tltUpload from "@/components/tltUpload/tltUpload.vue";

import infoDialog from "./components/infoDialog.vue";

export default {
 name: "Machine",
  components: {
    tltUpload,
    infoDialog,
  },
  dicts: ["STATUS", 'biz_product_name'],
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
        { type: "selection", fixed: "left" },
        {
          field: "productCode",
          title: this.$t("$1"),
        },
        {
          field: "productDescription",
          title: this.$t("ui.data.column.docProductLimit.productDescription"),
        },
        {
          field: "companyCode",
          title: this.$t("ui.data.column.docProductLimit.companyCode"),
        },
        {
          field: "factoryCode",
          title: this.$t("ui.data.column.docProductLimit.factoryCode"),
        },
        {
          field: "productTypeCode",
          title: this.$t("ui.data.column.docProductLimit.productName"),
          formatter: (row, column, value, index) => {
            return this.selectDictLabel(
              this.dict.type.biz_product_name,
              row.productTypeCode
            );
          },
        },
        {
          field: "moldingMachineCode",
          title: this.$t("ui.data.column.docProductLimit.moldingMachineCode"),
        },
        {
          field: "limitType",
          title: this.$t("ui.data.column.docProductLimit.limitTypeName"),
          formatter: (row, column, value, index) => {
            return this.selectDictLabel(this.dict.type.TODO, row.limitType);
          },
        },
        {
          align: "center",
          halign: "center",
          label: this.$t("ui.data.btn.option"),
          render: ({ row }) => {
            return (
              <el-button
                v-hasPermi={["fac:docVulcanizingLine:edit"]}
                class="minus"
                type="success"
                onClick={() => this.handleEdit(row)}
              >
                {this.$t("ui.frame.btn.update")}
              </el-button>
            );
          },
        },
      ];

      return columns;
    },
    searchColumns() {
      return [
        {
          label: this.$t("ui.data.column.docMoldingMachineCls.moldingMethod"),
          prop: "moldingMethod",
          // type: "select",
          // dictData: this.dict.type.MACHINE_TYPE,
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
    async handlePublish() {
      this.publishSchedule();
    },

    async handleModifyMonthQty() {
      try {
        let row = this.selection[0];
        const valid = await hasRecordValidate(row);
        if (valid.code == 200) {
          // let params = row.embryoCode+","+row.sapCode+","+row.cxBatchNo+","+row.bomDataVersion;
          //
          // modifyQty(params).then(() => {});
        }
      } catch (error) {
        console.error(error);
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
    handleAutoPlan() {
      console.log("handleAutoPlan");
      if (this.$refs.autoPlanRef) {
        this.$refs.autoPlanRef.show("", "1");
      }
    },
    handleModifyLhMachineQty() {
      if (this.$refs.qtyRef) {
        this.$refs.qtyRef.show();
      }
    },

    handleModelChange() {
      if (this.$refs.autoPlanRef) {
        this.$refs.autoPlanRef.show("", "3");
      }
    },
    handleProductStatus() {
      this.$router.push("/moldingPlanManagement/productStatus");
    },
    handleManualClose() {
      this.$confirm(
        this.$t("ui.data.column.cxScheduleResult.manualClose")
      ).then(async () => {
        const ids = this.selection.map((row) => row.id).join(",");
        const data = await manualClose({ ids });
        this.$modal.msgSuccess(data.msg);
        this.handelSuccess();
      });
    },
    handleToFinishList() {
      this.$router.push("/moldingPlanManagement/finished");
    },
    handleExport() {
      downloadLink("/fac/docVulcanizingLine/export", this.formatParams(false));
    },
    handleProducingIssue() {
      this.$confirm(this.$t("ui.biz.alter.producingIssue")).then(async () => {
        try {
          this.loading = false;
          const res = await producingIssue({
            cxMachineCode: cxMachineCode,
            embryoCode: embryoCode,
            taskType: taskType,
            scheduleDate: scheduleDate,
          });
          this.$modal.msgSuccess(res.msg);
          this.getList();
        } catch (error) {
          console.error(error);
          this.loading = false;
        }
      });
    },
    handleLastDaySupplyPlan() {
      this.$router.push("/moldingPlanManagement/lastDaySupplyPlan");
    },

    handleChangeReleaseStatus() {
      this.$refs.releaseStatusRef.show();
    },
    handleValidateConstruction() {
      this.$confirm(
        this.$t("ui.data.column.cxScheduleResult.validateConstruction")
      ).then(async () => {
        try {
          this.loading = true;
          const ids = this.selection.map((row) => row.id).join(",");
          const res = await validateConstruction({ ids });
          this.$modal.msgSuccess(res.msg);
          this.getList();
        } catch (error) {
          console.error(error);
          this.loading = false;
        }
      });
    },
    handleSelectionChange(rows) {
      this.selection = rows;
    },

    // utils
    updateTableHeaderTitle() {
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
        const data = await listMachine(this.formatParams());
        console.log(data);
        this.data = data.rows;
        this.page.total = data.total;
      } catch (error) {
        console.error(error);
      } finally {
        this.loading = false;
      }
    },

    async publishSchedule() {
      try {
        this.loading = true;
        const valid = await publishValidate();
        if (valid.msg == "0") {
          this.$confirm(
            this.$t("ui.data.column.scheduleResult.hasNullLhMachineCode")
          ).then(async () => {
            const result = await publishScheduleResult();
            this.$emit("success");
            this.hide();
          });
        } else {
          const result = await publishScheduleResult();
          this.$emit("success");
          this.hide();
        }
      } catch (error) {
        console.error(error);
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
