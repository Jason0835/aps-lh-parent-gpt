
<template>
  <basic-container>
    <page-table
      tableRef="beadMachineMainTable"
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
          v-hasPermi="['tq:machine:add']"
          type="primary"
          plain
          @click="handleAdd"
          >{{ $t("ui.frame.btn.add") }}</el-button
        >
        <el-button
          v-hasPermi="['tq:machine:edit']"
          type="primary"
          plain
          @click="() => handleEdit(selection[0])"
          :disabled="selection.length != 1"
          >{{ $t("ui.frame.btn.modify") }}</el-button
        >
        <!-- <el-button
          type="danger"
          @click="handleDeleteMulti"
          :disabled="selection.length == 0"
          >{{ $t("ui.frame.btn.delete") }}</el-button
        > -->
        <el-button
          v-hasPermi="['tq:machine:import']"
          @click="() => $refs.tltUploadForm.handleImport(importDefaultValue)"
          >{{ $t("ui.frame.btn.import") }}</el-button
        >
        <el-button v-hasPermi="['tq:machine:export']" @click="handleExport">{{
          $t("ui.frame.btn.export")
        }}</el-button>
      </template>
    </page-table>
    <tlt-upload-form
      ref="tltUploadForm"
      title="导入机台信息数据"
      downloadUrl="/tq/machine/importTemplate"
      uploadUrl="/tq/machine/importData"
      @uploadSuccess="getList"
      labelWidth="0"
      :columns="importColumns"
      :rules="importRules"
    />
    <InfoDialog ref="infoDialogRef" @success="handelSuccess" />
  </basic-container>
</template>
<script>
import moment from "moment";

import {
  listMachine,
  exportData,
  editMachine,
  publishApsMoldAdjustPlan,
  removeApsMoldAdjustPlan,
} from "@/api/tq/machine";
import InfoDialog from "./components/infoDialog.vue";
import TltUploadForm from "@/views/components/tltUploadForm.vue";
import { render } from "nprogress";
export default {
  name: "BeadMachine",
  components: { InfoDialog, TltUploadForm },
  dicts: [
    // "sys_yes_no",
    // "MOLD_CHANGE_TYPE",
    // "IS_RELEASE"
    "STATUS",
    "CLASS_SHIFT",
    "CLASS_NUM",
    "CLASS_NUM_THREE",
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
      importRules: {
        planDate: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "blur",
          },
        ],
      },

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
    searchColumns() {
      return [
        {
          label: this.$t("ui.data.column.machine.machineCode"),
          prop: "machineCode",
        },
        {
          label: this.$t("ui.data.column.machine.machineName"),
          prop: "machineName",
        },
        {
          label: this.$t("ui.data.column.machine.status"),
          prop: "status",
          type: "select",
          dictData: this.dict.type.STATUS,
        },
      ];
    },
    columns() {
      return [
        { type: "selection", fixed: "left" },
        {
          label: this.$t("common.option"),
          prop: "option",
          width: "100px",
          fixed: "left",
          render: ({ row }) => {
            return (
              <div>
                <el-button
                  class="minus"
                  type="success"
                  onClick={() => this.handleEdit(row)}
                >
                  {this.$t("ui.frame.btn.update")}
                </el-button>
              </div>
            );
          },
        },
        {
          label: this.$t("ui.data.column.machine.machineCode"),
          prop: "machineCode",
          minWidth: 100,
          // sortable: "custom",
        },
        {
          label: this.$t("ui.data.column.machine.machineName"),
          prop: "machineName",
          minWidth: 100,
          // sortable: "custom",
        },
        {
          label: this.$t("ui.data.column.machine.efficiency"),
          prop: "efficiency",
          minWidth: 100,
          // sortable: "custom",
          type: "number",
        },
        {
          label: this.$t("ui.data.column.machine.toolingInfo"),
          prop: "toolingInfo",
          minWidth: 100,
          // sortable: "custom",
          type: "number",
        },
        {
          label: this.$t("ui.data.column.machine.quata"),
          prop: "quata",
          minWidth: 100,
          type: "number",
        },
        {
          label: this.$t("ui.data.column.machine.classShift"),
          prop: "classShift",
          minWidth: 100,
          // sortable: "custom", //CLASS_SHIFT
          render: ({ row }) => {
            let value = row.classShift;
            if (this.isEmpty(value)) {
              return "";
            }
            return this.selectDictLabels(this.dict.type.CLASS_SHIFT, value);
          },
        },
        {
          label: this.$t("ui.data.column.machine.openMachineClass"),
          prop: "openMachineClass",
          minWidth: 100,
          // sortable: "custom",
          render: ({ row }) => {
            let value = row.openMachineClass;
            if (this.isEmpty(value)) {
              return "";
            }
            if (row.classShift === "3") {
              return this.selectDictLabels(
                this.dict.type.CLASS_NUM_THREE,
                value
              );
            }
            return this.selectDictLabels(this.dict.type.CLASS_NUM, value);
          },
        },
        {
          label: this.$t("ui.data.column.machine.status"),
          prop: "status",
          minWidth: 100,
          // sortable: "custom",
          render: ({ row }) => {
            return (
              <el-switch
                value={row.status}
                active-value="0"
                inactive-value="1"
                onChange={(value) => this.handleChangeStatus(value, row)}
              />
            );
          },
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
      ];
    },
  },
  methods: {
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

    handleDelete(row) {
      this.$confirm(this.$t("common.confirm.delete"), {
        type: "warning",
      }).then(() => {
        const ids = row.id;
        removeApsMoldAdjustPlan({ ids }).then((data) => {
          this.$modal.msgSuccess(data.msg);
          this.$set(this.page, "current", 1);
          this.getList();
        });
      });
    },
    handleExport() {
      this.$confirm(this.$t(`确定导出所有机台信息？`), {
        type: "warning",
      }).then(() => {
        try {
          this.loading = true;
          let params = this.formatParams(false);
          params = {
            ...params,
            pageSize: undefined,
            pageNum: undefined,
          };
          exportData(params);
        } catch (error) {
          console.error(error);
        } finally {
          this.loading = false;
        }
      });
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
      try {
        this.loading = true;
        const data = await listMachine(this.formatParams());
        // const data = await this.$axios.get("monthPlan/apsMoldAdjustPlan/list");

        this.data = data.rows.map((el) => {
          return {
            ...el,
            tempStatus: el.status,
          };
        });
        this.page.total = data.total;
      } catch (error) {
        console.error(error);
      } finally {
        this.loading = false;
      }
    },
  },
  mounted() {},
  activated() {
    this.getList();
  },
};
</script>
