
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
          v-hasPermi="['cx:machine:add']"
          @click="handleAdd"
          >{{ $t("ui.frame.btn.add") }}</el-button
        >
        <!-- <el-button
          type="warning"
          v-hasPermi="['cx:machine:edit']"
          @click="handleEdit"
          >{{ $t("ui.frame.btn.modify") }}</el-button
        > -->
        <el-button
          type="warning"
          v-hasPermi="['cx:machine:import']"
          @click="$refs.tltUpload.handleImport()"
          >{{ $t("ui.frame.btn.import") }}</el-button
        >
        <el-button
          type="primary"
          @click="handleExport"
          v-hasPermi="['cx:machine:export']"
          >{{ $t("ui.frame.btn.export") }}</el-button
        >
      </template>
    </page-table>
    <!-- <el-button style="display: none" ref="hidePopoverBtnRef"></el-button> -->
    <tlt-upload
      ref="tltUpload"
      downloadUrl="/cx/machine/importTemplate"
      uploadUrl="/cx/machine/importData"
      @uploadSuccess="getList"
    />
    <infoDialog ref="infoRef"  @success="getList"/>
  </basic-container>
</template>
<script>
//lib
import moment from "moment";
//utils
import { downloadLink } from "@/utils/request";
//interface
import { listMachine, editMachine } from "@/api/cx/machine";
//components
import tltUpload from "@/components/tltUpload/tltUpload.vue";

import infoDialog from "./components/infoDialog.vue";

export default {
  name: "machine",
  components: {
    tltUpload,
    infoDialog,
  },
  dicts: ["TASK_TYPE"],
  provide() {
    return {
      parentDict: this.dict,
    };
  },
  data() {
    return {
      searchColumns: [
        {
          label: this.$t("ui.data.column.cx.machine.machineCode"),
          prop: "machineCode",
        },
        {
          label: this.$t("ui.data.column.cx.machine.machineName"),
          prop: "machineName",
        },
        {
          label: this.$t("ui.data.column.machine.machineType"),
          prop: "machineType",
          type: "select",
          dictData: [], // "CX_MACHINE_TYPE",
        },
        {
          label: this.$t("ui.data.column.cx.machine.type"),
          prop: "type",
          type: "select",
          dictData: [], // "MACHINE_TYPE",
        },
        {
          label: this.$t("ui.data.column.cx.machine.status"),
          prop: "status",
          type: "select",
          dictData: [], // "STATUS",
        },
      ],
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
          prop: "machineCode",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.cx.machine.machineCode"),
          sortable: "custom",
        },
        {
          prop: "machineName",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.cx.machine.machineName"),
          sortable: "custom",
        },
        {
          prop: "type",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.machine.machineType"),
          sortable: "custom",
          // formatter: function (value, row, index) {
          //   return $.table.selectDictLabel(typeData, row.type);
          // }, CX_MACHINE_TYPE
        },
        {
          prop: "machineType",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.cx.machine.type"),
          sortable: "custom",
          // formatter: function (value, row, index) {
          //   return $.table.selectDictLabel(MACHINE_TYPE, row.machineType);
          // },
        },
        {
          prop: "dimensionMiniMum",
          align: "right",
          halign: "center",
          label: this.$t("ui.data.column.cx.machine.dimensionMiniMum"),
          sortable: "custom",
        },
        {
          prop: "dimensionMaxiMum",
          align: "right",
          halign: "center",
          label: this.$t("ui.data.column.cx.machine.dimensionMaxiMum"),
          sortable: "custom",
        },
        {
          prop: "quata",
          align: "right",
          halign: "center",
          label: this.$t("ui.data.column.cx.machine.quata"),
          sortable: "custom",
        },
        {
          prop: "quotaRatio",
          align: "right",
          halign: "center",
          label: this.$t("ui.data.column.cx.machine.quotaRatio"),
          sortable: "custom",
        },
        {
          prop: "classShift",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.cx.machine.classShift"),
          sortable: "custom",
          // formatter: function (value, row, index) {
          //   return $.table.selectDictLabel(CLASS_SHIFT, row.classShift);
          // },
        },
        {
          prop: "status",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.cx.machine.status"),
          sortable: "custom",
          // formatter: function (value, row, index) {
          //   return statusTools(row);
          // },
          render: ({ row }) => {
            return (
              <el-switch
                value={row.status}
                active-value="1"
                inactive-value="0"
                onChange={(value) => this.handleChangeStatus(value, row)}
              />
            );
          },
        },
        {
          prop: "operatorQty",
          align: "right",
          halign: "center",
          label: this.$t("ui.data.column.cx.machine.operatorQty"),
          sortable: "custom",
        },
        {
          prop: "remark",
          halign: "center",
          label: this.$t("ui.common.column.remark"),
          sortable: "custom",
          width: 50,
        },
        {
          align: "center",
          halign: "center",
          label: this.$t("ui.data.btn.option"),
          // formatter: function (value, row, index) {
          //   var actions = [];
          //   actions.push(
          //     '<a class="btn btn-success btn-xs ' +
          //       editFlag +
          //       '" href="javascript:void(0)" onclick="$.operate.edit(\'' +
          //       row.id +
          //       '\')"><i class="fa fa-edit"></i>' +
          //       this.$t("ui.frame.btn.update") +
          //       "</a> "
          //   );
          //   return actions.join("");
          // },
          render: ({ row }) => {
            return (
              <el-button type="success" onClick={() => this.handleEdit(row)}>
                {" "}
                {this.$t("ui.frame.btn.update")}
              </el-button>
            );
          },
        },
      ];

      return columns;
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
    handleDelete(row) {
      this.$confirm(this.$t("common.confirm.delete"), {
        type: "warning",
      }).then(() => {
        const ids = row.id;
        console.log(ids);
        // removeArea({ ids }).then((data) => {
        //   this.$modal.msgSuccess(data.msg);
        //   this.$set(this.page, "current", 1);
        //   this.getList();
        // });
      });
    },
    handleChangeStatus(status, row) {
      console.log(status);
      let title =
        status === "1"
          ? this.$t("ui.biz.alter.isOpen")
          : this.$t("ui.biz.alter.isStop");

      this.$confirm(title, {
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
      this.$confirm(this.$t("ui.biz.alter.makeSurePublish")).then(() => {
        this.publishSchedule();
      });
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
      // this.$set(this.page, "current", current);
      // this.$set(this.page, "pageSize", pageSize);
      this.getList();
    },
    handelSuccess() {
      this.getList();
    },
    handleSortChange({ column, prop, order }) {
      if (order) {
        this.sort = {
          orderBy: prop,
          isAsc: order == "ascending",
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
      downloadLink("/cx/machine/export", this.formatParams(false));
    },
    handleProducingIssue() {
      this.$$confirm(this.$t("ui.biz.alter.producingIssue")).then(async () => {
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
      this.$$confirm(
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
        params: {
          ...this.sort,
        },
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
            const result = await publishCxScheduleResult();
            this.$emit("success");
            this.hide();
          });
        } else {
          const result = await publishCxScheduleResult();
          this.$emit("success");
          this.hide();
        }
      } catch (error) {
        console.error(error);
      }
    },
  },
  created() {
    //设置默认排程时间
    let date = moment().add(1, "days").format("YYYY-MM-DD");
    date = "2023-06-01"; //test
    this.query.scheduleDate = date;
    this.search.scheduleDate = date;
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
