
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
        <el-button
          type="danger"
          @click="handleDeleteMulti"
          :disabled="selection.length == 0"
          >{{ $t("ui.frame.btn.delete") }}</el-button
        >
        <el-button
          type="primary"
          @click="handlePublish"
          :disabled="selection.length == 0"
          >{{ $t("ui.data.column.scheduleResult.publish") }}</el-button
        >
        <el-button @click="() => $refs.tltUploadForm.handleImport(importDefaultValue)">{{
          $t("ui.frame.btn.import")
        }}</el-button>
        <el-button type="warning" @click="handleExport">{{
          $t("ui.frame.btn.export")
        }}</el-button>
        <el-button
          type="primary"
          @click="handleChangeExecute"
          :disabled="selection.length == 0"
          >{{ $t("ui.btn.lhApsMoldAdjustPlan.changeExecute") }}</el-button
        >
      </template>
    </page-table>
    <tlt-upload-form
      ref="tltUploadForm"
      title="导入APS模具变动单数据"
      downloadUrl="/lh/lhApsMoldAdjustPlan/importTemplate"
      uploadUrl="/lh/lhApsMoldAdjustPlan/importData"
      @uploadSuccess="getList"
      :columns="importColumns"
      :rules="importRules"
    />
    <AddDialog ref="addDialogRef"  @success="handelSuccess"/>
    <InfoDialog ref="infoDialogRef"  @success="handelSuccess"/>
    <ChangeExecuteDialog ref="changeExecuteDialogRef" />
    <el-button style="display: none" ref="hidePopoverBtnRef"></el-button>
  </basic-container>
</template>
<script>
import moment from "moment";

import {
  listApsMoldAdjustPlan,
  exportApsMoldAdjustPlan,
  publishApsMoldAdjustPlan,
  removeApsMoldAdjustPlan,
} from "@/api/lh/lhApsMoldAdjustPlan";
import InfoDialog from "./components/infoDialog.vue";
import TltUploadForm from "@/views/components/tltUploadForm.vue";
import AddDialog from "./components/addDialog.vue";
import ChangeExecuteDialog from "./components/changeExecuteDialog.vue";
export default {
  name: "curingApsmoldAdjust",
  components: { InfoDialog, TltUploadForm, AddDialog, ChangeExecuteDialog },
  dicts: [
    // "sys_yes_no",
    // "MOLD_CHANGE_TYPE",
    // "IS_RELEASE"
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
        planDate: tomorrow,
      },
      importColumns: [
        {
          label: this.$t("ui.data.column.lhApsMoldAdjustPlan.planDate"),
          prop: "planDate",
          type: "date",
          dateType: "date",
          valueFormat: "yyyy-MM-dd",
          clearable: false,
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
          label: this.$t("ui.data.column.lhApsMoldAdjustPlan.planDate"),
          prop: "planDate",
          type: "date",
          dateType: "date",
          valueFormat: "yyyy-MM-dd",
          clearable: false,
        },
        {
          label: this.$t("ui.data.column.lhApsMoldAdjustPlan.lhMachineName"),
          prop: "lhMachineName",
        },
        {
          label: this.$t("ui.data.column.lhApsMoldAdjustPlan.changeType"),
          prop: "changeType",
          render: (form) => {
            return (
              <dict-select
                v-model={form.changeType}
                options={this.dict.type.MOLD_CHANGE_TYPE}
              />
            );
          },
        },
        {
          label: this.$t("ui.data.column.scheduleResult.isRelease"),
          prop: "isRelease",
          render: (form) => {
            return (
              <dict-select
                v-model={form.isRelease}
                options={this.dict.type.IS_RELEASE}
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
                <text-button
                  onClick={() => {
                    this.handleDelete(row);
                  }}
                >
                  {this.$t("common.button.delete")}
                </text-button>
              </div>
            );
          },
        },
        {
          label: this.$t("ui.data.column.lhApsMoldAdjustPlan.planDate"),
          prop: "planDate",
          minWidth: 100,
          sortable: "custom",
        },
        {
          label: this.$t("ui.data.column.lhApsMoldAdjustPlan.lhMachineName"),
          prop: "lhMachineName",
          minWidth: 100,
          sortable: "custom",
        },
        {
          label: this.$t("ui.data.column.scheduleResult.isRelease"),
          prop: "isRelease",
          minWidth: 100,
          sortable: "custom",
          render: ({ row }) => {
            return (
              <dict-tag
                value={row.isRelease}
                options={this.dict.type.IS_RELEASE}
              />
            );
          },
        },
        {
          label: this.$t("ui.data.column.lhApsMoldAdjustPlan.beforeSapCode"),
          prop: "beforeSapCode",
          minWidth: 100,
          sortable: "custom",
        },
        {
          label: this.$t("ui.data.column.lhApsMoldAdjustPlan.beforeSpecDesc"),
          prop: "beforeSpecDesc",
          minWidth: 100,
          sortable: "custom",
        },
        {
          label: this.$t("ui.data.column.lhApsMoldAdjustPlan.beforeEmbryoCode"),
          prop: "beforeEmbryoCode",
          minWidth: 100,
          sortable: "custom",
        },
        {
          label: this.$t("ui.data.column.lhApsMoldAdjustPlan.tireRoughStock"),
          prop: "tireRoughStock",
          minWidth: 100,
          sortable: "custom",
          formatter: (row) => {
            return row.tireRoughStock || "-";
          },
        },
        {
          label: this.$t("ui.data.column.lhApsMoldAdjustPlan.useMoldNumber"),
          prop: "useMoldNumber",
          minWidth: 100,
          sortable: "custom",
        },
        {
          label: this.$t("ui.data.column.lhApsMoldAdjustPlan.leftRightMold"),
          prop: "leftRightMold",
          minWidth: 100,
          sortable: "custom",
          formatter: (row) => {
            return row.leftRightMold || "-";
          },
        },
        {
          label: this.$t("ui.data.column.lhApsMoldAdjustPlan.changeType"),
          prop: "changeType",
          minWidth: 100,
          sortable: "custom",
          render: ({ row }) => {
            return (
              <dict-tag
                value={row.changeType}
                options={this.dict.type.MOLD_CHANGE_TYPE}
              />
            );
          },
        },
        {
          label: this.$t("ui.data.column.lhApsMoldAdjustPlan.afterSapCode"),
          prop: "afterSapCode",
          minWidth: 100,
          sortable: "custom",
        },
        {
          label: this.$t("ui.data.column.lhApsMoldAdjustPlan.afterSpecDesc"),
          prop: "afterSpecDesc",
          minWidth: 100,
          sortable: "custom",
        },
        {
          label: this.$t("ui.data.column.lhApsMoldAdjustPlan.afterEmbryoCode"),
          prop: "afterEmbryoCode",
          minWidth: 100,
          sortable: "custom",
        },
        {
          label: this.$t("ui.data.column.lhApsMoldAdjustPlan.changeMoldTime"),
          prop: "changeMoldTime",
          minWidth: 100,
          sortable: "custom",
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
        {
          label: this.$t("ui.data.column.lhApsMoldAdjustPlan.isExecute"),
          prop: "isExecute",
          minWidth: 100,
          sortable: "custom",
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
      this.$confirm(this.$t(`确定导出所有APS模具变动单？`), {
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
          exportApsMoldAdjustPlan(params);
        } catch (error) {
          console.error(error);
        } finally {
          this.loading = false;
        }
      });
    },
    handlePublish() {
      this.$confirm(this.$t(`确认要发布排程吗？`), {
        type: "warning",
      }).then(async () => {
        try {
          this.loading = true;
          let ids = [];
          this.selection.forEach((element) => {
            ids.push(element.id);
          });
          const params = {
            planDate:this.query.planDate,
            ids: ids.join(),
          };
          const data = await publishApsMoldAdjustPlan(params);
          this.$modal.msgSuccess(data.msg);
          this.$set(this.page, "current", 1);
          this.getList();
        } catch (error) {
          console.error(error);
        } finally {
          this.loading = false;
        }
      });
    },
    handleChangeExecute() {
      if (this.$refs.changeExecuteDialogRef) {
        let ids = [];
        this.selection.forEach((element) => {
          ids.push(element.id);
        });
        this.$refs.changeExecuteDialogRef.show(ids.join());
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

      if (params.createTime && params.createTime[0]) {
        params.createTimeStart = params.createTime[0];
        params.createTimeEnd = params.createTime[1];
        params.createTime = undefined;
      }

      return params;
    },
    //
    async getList() {
      try {
        this.loading = true;
        const data = await listApsMoldAdjustPlan(this.formatParams());
        // const data = await this.$axios.get("monthPlan/apsMoldAdjustPlan/list");

        this.data = data.rows;
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
