
<template>
  <basic-container>
    <page-table
      tableRef="curingApsmoldAdjustTable"
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
        <el-button @click="handleAdd">{{ $t("ui.frame.btn.add") }}</el-button>
        <el-button @click="handleEdit" :disabled="selection.length != 1">{{
          $t("ui.frame.btn.modify")
        }}</el-button>
        <!-- <el-button
          type="danger"
          @click="handleDeleteMulti"
          :disabled="selection.length == 0"
          >{{ $t("ui.frame.btn.delete") }}</el-button
        > -->
        <el-button
          @click="() => $refs.tltUploadForm.handleImport(importDefaultValue)"
          >{{ $t("ui.frame.btn.import") }}</el-button
        >
        <el-button type="warning" @click="handleExport">{{
          $t("ui.frame.btn.export")
        }}</el-button>
      </template>
    </page-table>
    <tlt-upload-form
      ref="tltUploadForm"
      title="导入机台信息数据"
      downloadUrl="/lh/machine/importTemplate"
      uploadUrl="/lh/machine/importData"
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
} from "@/api/lh/machine";
import InfoDialog from "./components/infoDialog.vue";
import TltUploadForm from "@/views/components/tltUploadForm.vue";
import { render } from "nprogress";
export default {
  name: "curingApsmoldAdjust",
  components: { InfoDialog, TltUploadForm },
  dicts: [
    // "sys_yes_no",
    // "MOLD_CHANGE_TYPE",
    // "IS_RELEASE"
    "STATUS",
  ],
  provide() {
    return {
      parentDict: this.dict,
    };
  },
  data() {
    let tomorrow = moment().add(1, "days").format("YYYY-MM-DD");
    return {
      dailyPlanVisiable: false,
      importDefaultValue: {
        updateSupport: null,
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
                true-label="on"
                false-label={null}
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
          label: this.$t("ui.data.column.machine.machineCode"),
          prop: "machineCode",
        },
        {
          label: this.$t("ui.data.column.machine.machineName"),
          prop: "machineName",
        },
        {
          label: this.$t("ui.data.column.machine.status"),
          prop: "changeType",
          render: (form) => {
            return (
              <dict-select
                v-model={form.changeType}
                options={this.dict.type.STATUS}
              />
            );
          },
        },
      ],
      columns: [
        { type: "selection", fixed: "left" },
        // { type: "index", fixed: "left" },
        {
          label: this.$t("common.option"),
          prop: "option",
          width: "100px",
          fixed: "left",
          render: ({ row }) => {
            return (
              <div>
                <text-button
                  onClick={() => {
                    this.handleEdit(row);
                  }}
                >
                  {this.$t("common.button.modify")}
                </text-button>
              </div>
            );
          },
        },
        {
          label: this.$t("ui.data.column.machine.id"),
          prop: "id",
          minWidth: 100,
          sortable: "custom",
          visible: false,
        },
        {
          label: this.$t("ui.data.column.machine.machineCode"),
          prop: "machineCode",
          minWidth: 100,
          sortable: "custom",
        },
        {
          label: this.$t("ui.data.column.machine.machineName"),
          prop: "machineName",
          minWidth: 100,
          sortable: "custom",
        },
        {
          label: this.$t("ui.data.column.machine.dimension"),
          prop: "dimension",
          minWidth: 100,
          sortable: "custom",
        },
        {
          label: this.$t("ui.data.column.machine.dimensionMinmum"),
          prop: "dimensionMinmum",
          minWidth: 100,
          sortable: "custom",
        },
        {
          label: this.$t("ui.data.column.machine.dimensionMaximum"),
          prop: "dimensionMaximum",
          minWidth: 100,
          sortable: "custom",
        },
        {
          label: this.$t("ui.data.column.machine.centripetalMechanism"),
          prop: "centripetalMechanism",
          minWidth: 100,
          sortable: "custom",
        },
        {
          label: this.$t("ui.data.column.machine.maxMoldNum"),
          prop: "maxMoldNum",
          minWidth: 100,
          sortable: "custom",
        },
        {
          label: this.$t("ui.data.column.machine.quata"),
          prop: "quata",
          minWidth: 100,
          sortable: "custom",
        },
        {
          label: this.$t("ui.data.column.machine.classShift"),
          prop: "classShift",
          minWidth: 100,
          sortable: "custom",
        },
        {
          label: this.$t("ui.data.column.machine.openMachineClass"),
          prop: "openMachineClass",
          minWidth: 100,
          sortable: "custom",
        },
        {
          label: this.$t("ui.data.column.machine.status"),
          prop: "status",
          minWidth: 100,
          sortable: "custom",
          render: ({ row }) => {
            return (
              <el-switch
                active-value="0"
                inactive-value="1"
                disabled={this.loading}
                value={row.status}
                onChange={(val) => {
                  let text = val == "1" ? "禁用" : "启用";
                  this.$confirm(`确认${text}吗？`, { type: "warning" }).then(
                    async () => {
                      try {
                        this.loading = true;
                        const data = await editMachine({
                          ...row,
                          status: val,
                        });
                        this.$modal.msgSuccess(data.msg);
                        // this.$set(this.page, "current", 1);
                        this.getList();
                      } catch (error) {
                        console.error(error);
                      } finally {
                        this.loading = false;
                      }
                    }
                  );
                }}
              ></el-switch>
            );
          },
        },
        {
          label: this.$t("ui.common.column.remark"),
          prop: "remark",
          minWidth: 100,
          sortable: "custom",
          formatter: (row) => {
            return row.remark || "-";
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
    handleDownloadUrl(form) {
      return "/cx/mdmMonthProdPlan/importTemplate/" + form.mainPlanMonth;
    },
    handleAdd() {
      if (this.$refs.addDialogRef) {
        this.$refs.addDialogRef.show();
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
    handleDeleteMulti() {
      // if (this.selection.length == 0) {
      //   this.$modal.msgWarning(this.$t("请至少选择一条记录"));
      //   return;
      // }
      this.$confirm(this.$t("common.confirm.delete"), { type: "warning" })
        .then(async () => {
          //确认提交
          try {
            this.loading = true;
            let ids = [];
            this.selection.forEach((element) => {
              ids.push(element.id);
            });
            const params = {
              ids: ids.join(),
            };
            const data = await removeApsMoldAdjustPlan(params);
            this.$modal.msgSuccess(data.msg);
            this.$set(this.page, "current", 1);
            this.getList();
          } catch (error) {
          } finally {
            this.loading = false;
          }
        })
        .catch(() => {});
    },
    handleExport() {
      this.$confirm(this.$t(`确定导出所有机台信息？`), {
        type: "warning",
      }).then(() => {
        try {
          this.loading = true;
          let params = this.formatParams();
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
          orderBy: prop,
          isAsc: order == "ascending",
        };
      } else {
        //默认排序
        this.sort = {};
      }
      this.getList();
    },

    formatParams() {
      const params = {
        pageSize: this.page.pageSize,
        pageNum: this.page.current,
        ...this.query,
        params: {
          ...this.sort,
        },
      };

      return params;
    },
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
