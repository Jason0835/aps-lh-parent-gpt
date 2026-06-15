<template>
  <basic-container>
    <page-table
      tableRef="tmScheduleResultMainTable"
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
          v-hasPermi="['tm:tmScheduleResult:autoPlan']"
          @click="handleAutoPlan"
        >{{ $t("ui.data.column.scheduleResult.autoPlan") }}</el-button>
        <el-button
          plain
          @click="handleBoardRefresh"
        >{{ $t("ui.frame.btn.refresh") }}</el-button>
        <el-button
          type="primary"
          plain
          v-hasPermi="['tm:tmScheduleResult:edit']"
          @click="handleAdd"
        >{{ $t("ui.data.column.scheduleResult.insertOrder") }}</el-button>
        <el-button
          type="danger"
          v-hasPermi="['tm:tmScheduleResult:remove']"
          :disabled="selection.length == 0"
          @click="handleDeleteAll"
        >{{ $t("ui.frame.btn.delete") }}</el-button>
        <el-button
          v-hasPermi="['tm:tmScheduleResult:import']"
          @click="$refs.tltUpload.handleImport()"
        >{{ $t("ui.frame.btn.import") }}</el-button>
        <el-button
          type="primary"
          :disabled="selection.length === 0"
          v-hasPermi="['tm:tmScheduleResult:changeMachine']"
          @click="handleChangeMachine"
        >{{ $t("ui.data.column.scheduleResult.changeMachine") }}</el-button>
        <el-button
          type="warning"
          :disabled="selection.length !== 1"
          v-hasPermi="['tm:tmScheduleResult:changeQty']"
          @click="handleChangeQty"
        >{{ $t("ui.data.column.scheduleResult.changePlan") }}</el-button>
        <el-button
          type="success"
          :disabled="selection.length === 0"
          v-hasPermi="['tm:tmScheduleResult:publish']"
          @click="handlePublish"
        >{{ $t("ui.data.column.scheduleResult.publish") }}</el-button>
        <el-button
          @click="handleExport"
          v-hasPermi="['tm:tmScheduleResult:export']"
        >{{ $t("ui.frame.btn.export") }}</el-button>
      </template>
    </page-table>
    <tlt-upload-form
      ref="tltUpload"
      :updateSupport="true"
      downloadUrl="/tm/tmScheduleResult/importTemplate"
      uploadUrl="/tm/tmScheduleResult/importData"
      @uploadSuccess="getList"
      labelWidth="0"
      :columns="importColumns"
    ></tlt-upload-form>
    <infoDialog ref="infoRef" @success="getList" />
    <changeMachineDialog ref="changeMachineRef" @success="getList" />
  </basic-container>
</template>
<script>
import {mapState} from "vuex";
import {downloadLink} from "@/utils/request";
import {
  autoPlan,
  listTmScheduleBoard,
  listTmScheduleResult,
  publishScheduleResult,
  publishValidate,
  removeTmScheduleResult,
} from "@/api/tm/scheduleResult";
import tltUpload from "@/components/tltUpload/tltUpload.vue";
import TltUploadForm from "@/views/components/tltUploadForm.vue";
import infoDialog from "./components/infoDialog.vue";
import changeMachineDialog from "./components/changeMachineDialog.vue";

export default {
  name: "TmScheduleResult",
  components: {
    tltUpload,
    infoDialog,
    TltUploadForm,
    changeMachineDialog,
  },
  dicts: ["biz_factory_name", "biz_yes_no", "IS_RELEASE", "tm_data_source"],
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
      search: {},
      query: {},
      importDefaultValue: {},
      importRules: {},
    };
  },
  computed: {
    ...mapState({
      machines: (state) => state.tm.machines,
    }),
    columns() {
      return [
        { type: "selection", fixed: "left" },
        {
          prop: "factoryCode",
          label: this.$t("ui.data.column.tm.scheduleResult.factoryCode"),
          type: "select",
          filterable: true,
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_factory_name, value);
          },
        },
        {
          prop: "batchNo",
          align: "left",
          minWidth: 160,
          label: this.$t("ui.data.column.tm.scheduleResult.batchNo"),
        },
        {
          prop: "orderNo",
          align: "left",
          minWidth: 160,
          label: this.$t("ui.data.column.tm.scheduleResult.orderNo"),
        },
        {
          prop: "scheduleDate",
          align: "center",
          minWidth: 120,
          label: this.$t("ui.data.column.tm.scheduleResult.scheduleDate"),
        },
        {
          prop: "machineCode",
          halign: "center",
          label: this.$t("ui.data.column.tm.scheduleResult.machineCode"),
        },
        {
          prop: "treadCode",
          halign: "center",
          label: this.$t("ui.data.column.tm.scheduleResult.treadCode"),
        },
        {
          prop: "glueCode",
          halign: "center",
          label: this.$t("ui.data.column.tm.scheduleResult.glueCode"),
        },
        {
          prop: "releaseStatus",
          halign: "center",
          label: this.$t("ui.data.column.tm.scheduleResult.releaseStatus"),
          type: "select",
          filterable: true,
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.IS_RELEASE, value);
          },
        },
        {
          prop: "tailFlag",
          halign: "center",
          label: this.$t("ui.data.column.tm.scheduleResult.tailFlag"),
          type: "select",
          filterable: true,
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_yes_no, value);
          },
        },
        {
          prop: "updateTime",
          label: this.$t("ui.data.column.updateTime"),
          width: 180,
        },
        {
          align: "center",
          halign: "center",
          label: this.$t("ui.data.btn.option"),
          minWidth: 180,
          width: 200,
          fixed: "right",
          render: ({ row }) => {
            return (
              <div>
                <el-button
                  v-hasPermi={["tm:tmScheduleResult:edit"]}
                  class="minus"
                  type="success"
                  onClick={() => this.handleEdit(row)}
                >
                  {this.$t("ui.data.column.scheduleResult.changePlan")}
                </el-button>
                <el-button
                  v-hasPermi={["tm:tmScheduleResult:remove"]}
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
    },
    searchColumns() {
      return [
        {
          prop: "factoryCode",
          label: this.$t("ui.data.column.tm.scheduleResult.factoryCode"),
          type: "select",
          dictData: this.dict.type.biz_factory_name,
          filterable: true,
        },
        {
          prop: "batchNo",
          label: this.$t("ui.data.column.tm.scheduleResult.batchNo"),
        },
        {
          prop: "orderNo",
          label: this.$t("ui.data.column.tm.scheduleResult.orderNo"),
        },
        {
          prop: "scheduleDate",
          label: this.$t("ui.data.column.tm.scheduleResult.scheduleDate"),
          type: "daterange",
          valueFormat: "yyyy-MM-dd",
        },
        {
          prop: "treadCode",
          label: this.$t("ui.data.column.tm.scheduleResult.treadCode"),
        },
        {
          prop: "machineCode",
          label: this.$t("ui.data.column.tm.scheduleResult.machineCode"),
          type: "select",
          dictData: this.machines,
          labelKey: "machineCode",
          valueKey: "machineCode",
          filterable: true,
        },
        {
          prop: "releaseStatus",
          label: this.$t("ui.data.column.tm.scheduleResult.releaseStatus"),
          type: "select",
          dictData: this.dict.type.IS_RELEASE,
          filterable: true,
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
    // 自动排程入口：只调用结构闭环接口，完整算法由后端后续接入。
    handleAutoPlan() {
      const params = this.buildAutoPlanParams();
      autoPlan(params).then((data) => {
        const message = data.data && data.data.message ? data.data.message : data.msg;
        this.$modal.msgSuccess(message);
        this.getList();
      });
    },
    // 看板刷新入口：使用兼容路径查询看板数据，不改变当前筛选条件。
    async handleBoardRefresh() {
      try {
        this.loading = true;
        const data = await listTmScheduleBoard(this.formatParams(false));
        this.data = data.data || [];
        this.page.total = this.data.length;
      } finally {
        this.loading = false;
      }
    },
    // 转机台弹窗
    handleChangeMachine() {
      if (this.$refs.changeMachineRef) {
        let row = this.selection;
        this.$refs.changeMachineRef.show(row);
      }
    },
    // 调量入口：复用编辑弹窗，由弹窗根据编辑状态调用调量接口。
    handleChangeQty() {
      if (this.$refs.infoRef && this.selection.length === 1) {
        this.$refs.infoRef.show(this.selection[0]);
      }
    },
    // 发布入口：先校验再标记待发布，真实 MES 发布由后续发布流程接入。
    handlePublish() {
      const ids = this.selection.map((item) => item.id);
      this.$confirm(this.$t("ui.biz.alter.makeSurePublish"), {
        type: "warning",
      }).then(() => {
        publishValidate(ids).then(() => {
          publishScheduleResult(ids).then((data) => {
            this.$modal.msgSuccess(data.msg);
            this.getList();
          });
        });
      });
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
        removeTmScheduleResult({ ids }).then((data) => {
          this.$modal.msgSuccess(data.msg);
          this.$set(this.page, "current", 1);
          this.getList();
        });
      });
    },
    handleDeleteAll() {
      let ids = "";
      for (let i = 0; i < this.selection.length; i++) {
        if (i == this.selection.length - 1) {
          ids = ids + this.selection[i].id;
        } else {
          ids = ids + this.selection[i].id + ",";
        }
      }
      this.$confirm(this.$t("common.confirm.delete"), {
        type: "warning",
      }).then(() => {
        removeTmScheduleResult({ ids }).then((data) => {
          this.$modal.msgSuccess(data.msg);
          this.$set(this.page, "current", 1);
          this.getList();
        });
      });
    },
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
        this.sort = {};
      }
      this.getList();
    },
    handleSelectionChange(rows) {
      this.selection = rows;
    },
    handleExport() {
      downloadLink("/tm/tmScheduleResult/export", this.formatParams(false));
    },

    buildAutoPlanParams() {
      const scheduleDate = Array.isArray(this.query.scheduleDate)
        ? this.query.scheduleDate[0]
        : this.query.scheduleDate;
      return {
        factoryCode: this.query.factoryCode || this.search.factoryCode,
        scheduleDate,
        dataSource: "AUTO",
      };
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
    async getList() {
      try {
        this.loading = true;
        const data = await listTmScheduleResult(this.formatParams());
        this.data = data.rows;
        this.page.total = data.total;
      } catch (error) {
        console.error(error);
      } finally {
        this.loading = false;
      }
    },
  },
  created() {
    this.$store.dispatch("tm/getMachineList");
    let defaultParams = {
      factoryCode: "116",
    };
    this.search = {
      ...defaultParams,
    };
    this.query = {
      ...defaultParams,
    };
    this.getList();
  },
  activated() {
    // this.getList();
  },
};
</script>
<style lang="scss" scoped>
.more-btn {
  margin: 2px 0;
  width: 100%;
}
</style>
