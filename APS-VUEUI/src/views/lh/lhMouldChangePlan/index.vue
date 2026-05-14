<template>
  <basic-container>
    <page-table
      tableRef="lhMouldChangePlanMainTable"
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
          v-hasPermi="['lh:lhMouldChangePlan:edit']"
          @click="handleAdd"
        >{{ $t("ui.frame.btn.add") }}</el-button>
        <el-button
          v-hasPermi="['lh:lhMouldChangePlan:edit']"
          @click="handleBatchEdit"
          :disabled="selection.length !== 1"
        >{{ $t("ui.frame.btn.update") }}</el-button>
        <el-button
          type="danger"
          v-hasPermi="['lh:lhMouldChangePlan:remove']"
          @click="handleBatchDelete"
          :disabled="selection.length == 0"
        >{{ $t("ui.frame.btn.delete") }}</el-button>
        <el-button
          v-hasPermi="['lh:lhMouldChangePlan:import']"
          @click="$refs.tltUpload.handleImport()"
        >{{ $t("ui.frame.btn.import") }}</el-button>
         <el-button
           @click="handleExport"
           v-hasPermi="['lh:lhMouldChangePlan:export']"
         >{{ $t("ui.frame.btn.export") }}</el-button>
          <el-button
            type="primary"
            plain
            v-hasPermi="['lh:lhMouldChangePlan:issue']"
            @click="handleIssueSchedule"
          >{{ $t("ui.data.btn.lhMouldChangePlan.issueSchedule") }}</el-button>
       </template>
    </page-table>
    <tlt-upload-form
      ref="tltUpload"
      :updateSupport="true"
      downloadUrl="/lh/lhMouldChangePlan/importTemplate"
      uploadUrl="/lh/lhMouldChangePlan/importData"
      @uploadSuccess="getList"
      labelWidth="0"
      :columns="importColumns"
    ></tlt-upload-form>
    <infoDialog ref="infoRef" @success="getList" />
  </basic-container>
</template>
<script>
import { downloadLink } from "@/utils/request";
import { listLhMouldChangePlan, removeLhMouldChangePlan, issueScheduleByQuery } from "@/api/lh/lhMouldChangePlan";
import TltUploadForm from "@/views/components/tltUploadForm.vue";
import infoDialog from "./components/infoDialog.vue";

const formatDate = (date) => {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
};

const getOffsetDate = (offsetDay) => {
  const date = new Date();
  date.setDate(date.getDate() + offsetDay);
  return formatDate(date);
};

export default {
  name: "LhMouldChangePlan",
  components: {
    TltUploadForm,
    infoDialog,
  },
  dicts: ["biz_factory_name", "biz_yes_no", "CHANGE_MOULD_TYPE", "IS_RELEASE", "class_num_two_mm", "lr_molds", "finish_completion"],
  provide() {
    return {
      parentDict: this.dict,
    };
  },
  data() {
    return {
      importColumns: [
        {
          label: "",
          prop: "updateSupport",
          render: (form) => {
            return (
              <el-checkbox
                label={this.$t("common.rule.updateSupport")}
                v-model={form.updateSupport}
              >
                {this.$t("common.rule.updateSupport")}
              </el-checkbox>
            );
          },
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
        factoryCode: "116",
        scheduleDate: getOffsetDate(2),
      },
      query: {
        factoryCode: "116",
        scheduleDate: getOffsetDate(2),
      },
    };
  },
  computed: {
    columns() {
      let columns = [
        { type: "selection", fixed: "left" },
        {
          prop: "factoryCode",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.factoryCode"),
          minWidth: 120,
          formatter: (row, column, value, index) => {
            return this.selectDictLabel(this.dict.type.biz_factory_name, value);
          },
        },
        {
          prop: "planDate",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.lhMouldChangePlan.planDate"),
          minWidth: 140,
        },
        {
          prop: "scheduleDate",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.lhMouldChangePlan.scheduleDate"),
          minWidth: 120,
        },
        {
          prop: "classIndex",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.lhMouldChangePlan.classIndex"),
          minWidth: 120,
          formatter: (row, column, value, index) => {
            return this.selectDictLabel(this.dict.type.class_num_two_mm, value);
          },
        },
        {
          prop: "leftRightMould",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.lhMouldChangePlan.leftRightMould"),
          minWidth: 120,
          formatter: (row, column, value, index) => {
            return this.selectDictLabel(this.dict.type.lr_molds, value);
          },
        },
        {
          prop: "lhMachineCode",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.lhMouldChangePlan.lhMachineCode"),
          minWidth: 140,
        },
        // {
        //   prop: "lhMachineName",
        //   align: "center",
        //   halign: "center",
        //   label: this.$t("ui.data.column.lhMouldChangePlan.lhMachineName"),
        //   minWidth: 160,
        // },
        {
          prop: "beforeMaterialCode",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.lhMouldChangePlan.beforeMaterialCode"),
          minWidth: 150,
        },
        {
          prop: "beforeMaterialDesc",
          align: "left",
          halign: "center",
          label: this.$t("ui.data.column.lhMouldChangePlan.beforeMaterialDesc"),
          minWidth: 350,
          render: ({ row }) => this.renderMaterialDesc(row.beforeMaterialDesc),
        },
        {
          prop: "afterMaterialCode",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.lhMouldChangePlan.afterMaterialCode"),
          minWidth: 150,
        },
        {
          prop: "afterMaterialDesc",
          align: "left",
          halign: "center",
          label: this.$t("ui.data.column.lhMouldChangePlan.afterMaterialDesc"),
          minWidth: 350,
          render: ({ row }) => this.renderMaterialDesc(row.afterMaterialDesc),
        },
        {
          prop: "changeMouldType",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.lhMouldChangePlan.changeMouldType"),
          minWidth: 120,
          formatter: (row, column, value, index) => {
            return this.selectDictLabel(this.dict.type.CHANGE_MOULD_TYPE, value);
          },
        },
        {
          prop: "changeTime",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.lhMouldChangePlan.changeTime"),
          minWidth: 140,
        },
        // {
        //   prop: "mouldCode",
        //   align: "center",
        //   halign: "center",
        //   label: this.$t("ui.data.column.lhMouldChangePlan.mouldCode"),
        //   width: 450,
        // },
        {
          prop: "isRelease",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.lhMouldChangePlan.isRelease"),
          minWidth: 120,
          formatter: (row, column, value, index) => {
            return this.selectDictLabel(this.dict.type.IS_RELEASE, value);
          },
        },
        {
          prop: "mouldStatus",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.lhMouldChangePlan.mouldStatus"),
          minWidth: 140,
          formatter: (row, column, value, index) => {
            return this.selectDictLabel(this.dict.type.finish_completion, value);
          },
        },
        {
          prop: "lhResultBatchNo",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.lhMouldChangePlan.lhResultBatchNo"),
          minWidth: 140,
        },
        {
          prop: "orderNo",
          align: "left",
          halign: "left",
          label: this.$t("ui.data.column.lhMouldChangePlan.orderNo"),
          minWidth: 160,
        },
        {
          prop: "updateTime",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.updateTime"),
          minWidth: 180,
        },

        // {
        //   prop: "createByName",
        //   align: "center",
        //   halign: "center",
        //   label: this.$t("ui.data.column.createBy"),
        //   minWidth: 120,
        // },
        // {
        //   prop: "createTime",
        //   align: "center",
        //   halign: "center",
        //   label: this.$t("ui.data.column.createTime"),
        //   minWidth: 160,
        // },
        {
          align: "center",
          halign: "center",
          label: this.$t("ui.data.btn.option"),
          prop: "option",
          minWidth: 150,
          render: ({ row }) => {
            return (
              <div>
                <el-button
                  v-hasPermi={["lh:lhMouldChangePlan:edit"]}
                  class="minus"
                  type="success"
                  onClick={() => this.handleEdit(row)}
                >
                  {this.$t("ui.frame.btn.update")}
                </el-button>
                <el-button
                  v-hasPermi={["lh:lhMouldChangePlan:remove"]}
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
      ];
      return columns;
    },
    searchColumns() {
      return [
        {
          label: this.$t("ui.data.column.factoryCode"),
          prop: "factoryCode",
          type: "select",
          dictData: this.dict.type.biz_factory_name,
          filterable: true,
        },
        {
          label: this.$t("ui.data.column.lhMouldChangePlan.lhResultBatchNo"),
          prop: "lhResultBatchNo",
        },
        {
          label: this.$t("ui.data.column.lhMouldChangePlan.lhMachineCode"),
          prop: "lhMachineCode",
        },
        {
          label: this.$t("ui.data.column.lhMouldChangePlan.orderNo"),
          prop: "orderNo",
          align: "left",
          minWidth: 160,
        },
        {
          label: this.$t("ui.data.column.lhMouldChangePlan.planDate"),
          prop: "planDate",
          type: "date",
          valueFormat: "yyyy-MM-dd",
        },
        {
          label: this.$t("ui.data.column.lhMouldChangePlan.scheduleDate"),
          prop: "scheduleDate",
          type: "date",
          valueFormat: "yyyy-MM-dd",
        },
        {
          label: this.$t("ui.data.column.lhMouldChangePlan.mouldCode"),
          prop: "mouldCode",
        },
        {
          label: this.$t("ui.data.column.lhMouldChangePlan.isRelease"),
          prop: "isRelease",
          type: "select",
          dictData: this.dict.type.IS_RELEASE,
          filterable: true,
        },
      ];
    },

  },
  methods: {
    getTodayDate() {
      return getOffsetDate(0);
    },
    renderMaterialDesc(value) {
      return (
        <div style="white-space: normal; word-break: break-all; line-height: 20px; text-align: left;">
          {value}
        </div>
      );
    },
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
        removeLhMouldChangePlan(ids).then((data) => {
          this.$modal.msgSuccess(data.msg);
          this.$set(this.page, "current", 1);
          this.getList();
        });
      });
    },
    handleBatchDelete() {
      if (!this.selection || this.selection.length === 0) {
        this.$modal.msgWarning(this.$t("common.tip.selectOne"));
        return;
      }
      const ids = this.selection.map(item => item.id).join(',');
      this.$confirm(this.$t("common.confirm.delete"), {
        type: "warning",
      }).then(() => {
        removeLhMouldChangePlan(ids).then((data) => {
          this.$modal.msgSuccess(data.msg);
          this.$set(this.page, "current", 1);
          this.getList();
        });
      });
    },
    handleBatchEdit() {
      if (this.selection && this.selection.length === 1) {
        this.handleEdit(this.selection[0]);
      }
    },
    handleSearch(data) {
      this.query = { ...data };
      this.$set(this.page, "current", 1);
      this.getList();
    },
    handlePageChange(current, pageSize) {
      this.$set(this.page, "current", current);
      this.$set(this.page, "pageSize", pageSize);
      this.getList();
    },
    handleSortChange({ column, prop, order }) {
      if (order) {
        this.sort = {
          orderByColumn: prop,
          isAsc: order == "ascending" ? "asc" : "desc",
        };
      } else {
        this.sort = {};
      }
      this.getList();
    },
    handleSelectionChange(rows) {
      this.selection = rows;
    },
    handleExport() {
      downloadLink("/lh/lhMouldChangePlan/export", this.formatParams(false));
    },
    formatParams(hasPage = true) {
      const params = {
        ...this.query,
        ...this.sort,
      };
      if (!params.orderByColumn) {
        params.orderByColumn = "updateTime,lhMachineCode";
        params.isAsc = "desc,asc";
      }
      if (hasPage) {
        params.pageSize = this.page.pageSize;
        params.pageNum = this.page.current;
      }
      return params;
    },
        async getList() {
            try {
                this.loading = true;
                const data = await listLhMouldChangePlan(this.formatParams());
                this.data = data.rows;
                this.page.total = data.total;
            } catch (error) {
                console.error(error);
            } finally {
                this.loading = false;
            }
        },
        handleIssueSchedule() {
            if (!this.data || this.data.length === 0) {
                this.$modal.msgWarning(this.$t("ui.data.alert.lhMouldChangePlan.noData"));
                return;
            }

            this.$confirm(this.$t("ui.data.alert.lhMouldChangePlan.issueConfirm"), {
                type: "warning",
            }).then(() => {
                issueScheduleByQuery(this.formatParams(false)).then((res) => {
                    this.$modal.msgSuccess(res.msg);
                    this.$set(this.page, "current", 1);
                    this.getList();
                });
            });
        },
    },
    activated() {
        this.getList();
    },
};
</script>
<style lang="scss" scoped>
</style>
