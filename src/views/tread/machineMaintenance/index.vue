
<template>
  <basic-container>
    <page-table
      tableRef="treadTmMachineMaintenanceMainTable"
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
          v-hasPermi="['tm:tmMachineMaintenance:add']"
          type="primary"
          plain
          @click="handleAdd"
          >{{ $t("ui.frame.btn.add") }}</el-button
        >
        <!-- <el-button
          v-hasPermi="['tm:tmMachineMaintenance:edit']"
          type="primary"
          plain
          @click="handleEdit(selection[0])"
          :disabled="selection.length != 1"
          >{{ $t("ui.frame.btn.modify") }}</el-button
        > -->
        <el-button
          v-hasPermi="['tm:tmMachineMaintenance:remove']"
          type="danger"
          @click="handleDelete(selection)"
          :disabled="selection.length == 0"
          >{{ $t("ui.frame.btn.delete") }}</el-button
        >
        <el-button
          v-hasPermi="['tm:tmMachineMaintenance:import']"
          @click="() => $refs.tltUploadForm.handleImport(importDefaultValue)"
          >{{ $t("ui.frame.btn.import") }}</el-button
        >
        <el-button
          v-hasPermi="['tm:tmMachineMaintenance:export']"
          @click="handleExport"
          >{{ $t("ui.frame.btn.export") }}</el-button
        >
        <el-button
          type="primary"
          @click="handleLeave"
          >{{ $t("操作工请假") }}</el-button
        >
      </template>
    </page-table>
    <tlt-upload-form
      ref="tltUploadForm"
      title="导入"
      downloadUrl="/tm/tmMachineMaintenance/importTemplate"
      uploadUrl="/tm/tmMachineMaintenance/importData"
      @uploadSuccess="getList"
      labelWidth="0"
      :columns="importColumns"
      :rules="importRules"
    />
    <InfoDialog ref="infoDialogRef" @success="handelSuccess" />
    <leave-dialog ref="leaveRef" @success="getList" />
  </basic-container>
</template>
<script>
import { mapState } from "vuex";
import moment from "moment";

// import {
//   listTmMachineMaintenance,
//   editTmMachineMaintenance,
//   removeTmMachineMaintenance,
// } from "@/api/tm/tmMachineMaintenance";
import InfoDialog from "./components/infoDialog.vue";
import TltUploadForm from "@/views/components/tltUploadForm.vue";
import { downloadLink } from "@/utils/request";
import leaveDialog from "./components/leaveDialog.vue";
export default {
  name: "TreadMachineMaintenance",
  components: { InfoDialog, TltUploadForm ,leaveDialog},
  dicts: [
    // "sys_yes_no",
    // "MOLD_CHANGE_TYPE",
    // "IS_RELEASE"
    "STATUS",
    "CLASS_SHIFT",
    "CLASS_NUM",
    "CLASS_NUM_THREE",
    "IS_SUPPORTED",
  ],
  provide() {
    return {
      parentDict: this.dict,
    };
  },
  data() {
    let tomorrow = moment().add(1, "days").format("YYYY-MM-DD");
    return {
      importDefaultValue: {
        updateSupport: false,
      },
      importColumns: [
        {
          label: "",
          prop: "updateSupport",
          render: (form) => {
            console.log(form);
            return (
              <el-checkbox
                label="是否更新已经存在的用户数据"
                true-label={true}
                false-label={false}
                v-model={form.updateSupport}
              >
                是否更新已经存在的用户数据
              </el-checkbox>
            );
          },
        },
      ],
      importRules: {},

      loading: false,
      data: [],
      page: {
        current: 1,
        pageSize: 20,
        total: 0,
      },
      sort: {},
      search: {
        planDate: tomorrow,
      },
      query: {
        planDate: tomorrow,
      },
      selection: [],
    };
  },
  computed: {
    ...mapState({
      machines: (state) => state.tread.machines,
    }),
    columns() {
      return [
        { type: "selection", fixed: "left" },
        {
          label: this.$t("停机日期"),
          prop: "stopDate",
          minWidth: 100,
          // sortable: "custom",
        },
        {
          label: this.$t("机台名称"),
          prop: "machineName",
          minWidth: 100,
          // sortable: "custom",
        },
        {
          label: this.$t("预计开始时间"),
          prop: "machineName",
          minWidth: 100,
          // sortable: "custom",
        },
        {
          label: this.$t("预计结束时间"),
          prop: "machineName",
          minWidth: 100,
          // sortable: "custom",
        },
        {
          label: this.$t("时间类型"),
          prop: "machineName",
          minWidth: 100,
          // sortable: "custom",
        },

        {
          label: this.$t("停机班次"),
          prop: "stopShift",
          minWidth: 100,
          // sortable: "custom", //CLASS_NUM
          render: ({ row }) => {
            let value = row.stopShift;
            if (this.isEmpty(value)) {
              return "";
            }
            return this.selectDictLabels(this.dict.type.CLASS_NUM, value);
          },
        },
        {
          label: this.$t("停机时间(H)"),
          prop: "stopTime",
          minWidth: 100,
          // sortable: "custom",
          type: "number",
        },
        {
          label: this.$t("ui.common.column.remark"),
          prop: "remark",
          minWidth: 100,
          // sortable: "custom",
          formatter: (row) => {
            return row.remark || "-";
          },
        },
        {
          align: "center",
          halign: "center",
          label: this.$t("ui.data.btn.option"),
          prop: "option",
          render: ({ row }) => {
            return (
              <div>
                <el-button
                  v-hasPermi={["tm:mouthPlate:edit"]}
                  class="minus"
                  type="success"
                  onClick={() => this.handleEdit(row)}
                >
                  {this.$t("ui.frame.btn.update")}
                </el-button>
                <el-button
                  v-hasPermi={["tm:mouthPlate:remove"]}
                  class="minus"
                  type="danger"
                  onClick={() => this.handleDelete([row])}
                >
                  {this.$t("ui.frame.btn.delete")}
                </el-button>
              </div>
            );
          },
        },
      ];
    },
    searchColumns() {
      return [
        {
          label: this.$t("停机日期"),
          prop: "stopDate",
          type: "date",
          dateType: "date",
          valueFormat: "yyyy-MM-dd",
        },
        {
          label: this.$t("机台"),
          prop: "machineId",
          type: "select",
          dictData: this.machines,
          valueKey: "id",
          labelKey: "machineName",
        },
      ];
    },
  },
  methods: {
    handleLeave(){
      if (this.$refs.leaveRef) {
        this.$refs.leaveRef.show();
      }
    },
    handleChangeStatus(status, row) {
      console.log(status);
      let title =
        status === "0"
          ? this.$t("ui.biz.alter.isOpen")
          : this.$t("ui.biz.alter.isStop");

      this.$confirm(title, {
        type: "warning",
      }).then(async () => {
        try {
          this.loading = true;
          // const res = await editTmMachineMaintenance({
          //   ...row,
          //   status,
          // });
          this.$modal.msgSuccess(res.msg);
          this.getList();
        } catch (error) {
          this.loading = false;
        }
      });
    },
    handleAdd() {
      if (this.$refs.infoDialogRef) {
        this.$refs.infoDialogRef.show();
      }
    },
    handleEdit(row) {
      if (this.$refs.infoDialogRef) {
        this.$refs.infoDialogRef.show(row);
      }
    },

    handleDelete(rows) {
      this.$confirm(this.$t("common.confirm.delete"), {
        type: "warning",
      }).then(() => {
        const ids = rows.map((row) => row.id).join(",");
        // removeTmMachineMaintenance({ ids }).then((data) => {
        //   this.$modal.msgSuccess(data.msg);
        //   this.$set(this.page, "current", 1);
        //   this.getList();
        // });
      });
    },
    handleExport() {
      downloadLink("/tm/tmMachineMaintenance/export", this.formatParams(false));
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
    handleSelectionChange(rows) {
      this.selection = rows;
    },

    //util
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
    /**
     * 获得列表参数
     * @param {boolean} hasPage
     * @returns {object}
     */
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
    // a

    //
    async getList() {
      // try {
      //   this.loading = true;
      //   const data = await listTmMachineMaintenance(this.formatParams());

      //   this.data = data.rows.map((el) => {
      //     return {
      //       ...el,
      //       tempStatus: el.status,
      //     };
      //   });
      //   this.page.total = data.total;
      // } catch (error) {
      //   console.error(error);
      // } finally {
      //   this.loading = false;
      // }
    },
  },
  mounted() {},
  created() {
    this.$store.dispatch("tread/getMachineList");
  },
  activated() {
    this.getList();
  },
};
</script>
