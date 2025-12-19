
<template>
  <basic-container>
    <page-table
      tableRef="insideLinerMachineMainTable"
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
          v-hasPermi="['setting:machineGlueDecompose:add']"
          @click="handleAdd"
          >{{ $t("ui.frame.btn.add") }}</el-button
        >
        <!-- <el-button
          v-hasPermi="['setting:machineGlueDecompose:edit']"
          @click="handleEdit(selection[0])"
          :disabled="selection.length != 1"
          >{{ $t("ui.frame.btn.modify") }}</el-button
        > -->
        <!-- <el-button
          type="danger"
          @click="handleDeleteMulti"
          :disabled="selection.length == 0"
          >{{ $t("ui.frame.btn.delete") }}</el-button
        > -->
        <el-button
          v-hasPermi="['setting:machineGlueDecompose:import']"
          @click="() => $refs.tltUploadForm.handleImport(importDefaultValue)"
          >{{ $t("ui.frame.btn.import") }}</el-button
        >
        <el-button
          v-hasPermi="['setting:machineGlueDecompose:export']"
          @click="handleExport"
          >{{ $t("ui.frame.btn.export") }}</el-button
        >
      </template>
    </page-table>
    <tlt-upload-form
      ref="tltUploadForm"
      title="导入信息数据"
      downloadUrl="/setting/machineGlueDecompose/importTemplate"
      uploadUrl="/setting/machineGlueDecompose/importData"
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
  listMachineGlueDecompose,
  removeMachineGlueDecompose,
  exportData,
} from "@/api/setting/machineGlueDecompose";
import InfoDialog from "./components/infoDialog.vue";
import TltUploadForm from "@/views/components/tltUploadForm.vue";
export default {
  name: "MachineRubberDecompose",
  components: { InfoDialog, TltUploadForm },
  dicts: [
    // "sys_yes_no",
    // "MOLD_CHANGE_TYPE",
    // "IS_RELEASE"
    "STATUS",
    "MIX_AREA",
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
      searchColumns: [
        {
          label: this.$t("setting.machineGlueDecompose.glue"),
          prop: "glue",
        },
        {
          label: this.$t("setting.machineGlueDecompose.mixArea"),
          prop: "mixArea",
          render: (form) => {
            return (
              <dict-select
                clearable
                v-model={form.mixArea}
                options={this.dict.type.MIX_AREA}
              />
            );
          },
        },
        // {
        //   label: this.$t("setting.machineGlueDecompose.machineName2"),
        //   prop: "machineCode",
        //   render: (form) => {
        //     return (
        //       <dict-select
        //         v-model={form.machineCode}
        //         options={this.dict.type.STATUS}
        //       />
        //     );
        //   },
        // },
      ],
      columns: [
        { type: "selection", fixed: "left" },
        {
          prop: "mixArea",
          // sortable: "custom",
          halign: "center",
          align: "center",
          label: this.$t("setting.machineGlueDecompose.mixArea"),
          formatter: (row, column, value, index) => {
                return this.selectDictLabel(
                  this.dict.type.MIX_AREA,
                  value
                );
              }
        },
        {
          prop: "machineCode",
          // sortable: "custom",
          halign: "center",
          align: "center",
          label: this.$t("setting.machineGlueDecompose.machineCode"),
        },
        {
          prop: "machineName",
          // sortable: "custom",
          halign: "center",
          align: "center",
          label: this.$t("setting.machineGlueDecompose.machineName"),
        },
        {
          prop: "glue",
          // sortable: "custom",
          halign: "center",
          align: "center",
          label: this.$t("setting.machineGlueDecompose.glue"),
        },
        {
          prop: "motherGlue1",
          // sortable: "custom",
          halign: "center",
          align: "center",
          label: this.$t("setting.machineGlueDecompose.motherGlue1"),
        },
        {
          prop: "motherGlue2",
          // sortable: "custom",
          halign: "center",
          align: "center",
          label: this.$t("setting.machineGlueDecompose.motherGlue2"),
        },
        {
          prop: "motherGlue3",
          // sortable: "custom",
          halign: "center",
          align: "center",
          label: this.$t("setting.machineGlueDecompose.motherGlue3"),
        },
        {
          prop: "motherGlue4",
          // sortable: "custom",
          halign: "center",
          align: "center",
          label: this.$t("setting.machineGlueDecompose.motherGlue4"),
        },
        {
          prop: "motherGlue5",
          // sortable: "custom",
          halign: "center",
          align: "center",
          label: this.$t("setting.machineGlueDecompose.motherGlue5"),
        },
        {
          prop: "motherGlue6",
          // sortable: "custom",
          halign: "center",
          align: "center",
          label: this.$t("setting.machineGlueDecompose.motherGlue6"),
        },
        {
          prop: "motherGlue7",
          // sortable: "custom",
          halign: "center",
          align: "center",
          label: this.$t("setting.machineGlueDecompose.motherGlue7"),
        },
        {
          prop: "motherGlue8",
          // sortable: "custom",
          halign: "center",
          align: "center",
          label: this.$t("setting.machineGlueDecompose.motherGlue8"),
        },
        {
          prop: "motherGlue9",
          // sortable: "custom",
          halign: "center",
          align: "center",
          label: this.$t("setting.machineGlueDecompose.motherGlue9"),
        },
        {
          prop: "remark",
          // sortable: "custom",
          label: this.$t("ui.common.column.remark"),
          halign: "center",
          align: "left",
          // formatter: (row, column, value, index) => {
          //   return $.table.tooltip(value, 20);
          // },
          minWidth: 100,        },
        {
          align: "center",
          halign: "center",
          label: this.$t("ui.data.btn.option"),
          minWidth: 180,
          width: 180,
          fixed: "right",
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
                <el-button
                  class="minus"
                  type="danger"
                  onClick={() => this.handleDelete(row)}
                >
                  {this.$t("ui.frame.btn.delete")}
                </el-button>
              </div>
            );
          },
        },
      ],
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
  computed: {},
  methods: {
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
        removeMachineGlueDecompose({ ids }).then((data) => {
          this.$modal.msgSuccess(data.msg);
          this.$set(this.page, "current", 1);
          this.getList();
        });
      });
    },
    handleExport() {
      this.$confirm(this.$t(`确定导出所有信息？`), {
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
        const data = await listMachineGlueDecompose(this.formatParams());

        this.data = data.rows.map((el) => {
          return {
            ...el,
            // tempStatus: el.status,
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
