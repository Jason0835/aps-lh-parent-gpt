<template>
  <basic-container>
    <page-table
      tableRef="adjustPlanRequireInfoMainTable"
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
          v-hasPermi="['monthplan:adjustPlanRequireInfo:add']"
          @click="handleAdd"
        >{{ $t("ui.frame.btn.add") }}</el-button>
        <el-button
          type="primary"
          plain
          v-hasPermi="['monthplan:adjustPlanRequireInfo:edit']"
          :disabled="selection.length !== 1"
          @click="handleBatchEdit"
        >{{ $t("ui.frame.btn.update") }}</el-button>
        <el-button
          type="danger"
          v-hasPermi="['monthplan:adjustPlanRequireInfo:remove']"
          :disabled="selection.length === 0"
          @click="handleBatchDelete"
        >{{ $t("ui.frame.btn.delete") }}</el-button>
        <el-button
          v-hasPermi="['monthplan:adjustPlanRequireInfo:import']"
          @click="$refs.tltUpload.handleImport()"
        >{{ $t("ui.frame.btn.import") }}</el-button>
        <el-button
          @click="handleExport"
          v-hasPermi="['monthplan:adjustPlanRequireInfo:export']"
        >{{ $t("ui.frame.btn.export") }}</el-button>
      </template>
    </page-table>
    <tlt-upload-form
      ref="tltUpload"
      :updateSupport="true"
      downloadUrl="/monthplan/adjustPlanRequireInfo/importTemplate"
      uploadUrl="/monthplan/adjustPlanRequireInfo/importData"
      @uploadSuccess="getList"
      labelWidth="0"
      :columns="importColumns"
    />
    <info-dialog ref="infoRef" @success="getList" />
  </basic-container>
</template>

<script>
import { listAdjustPlanRequireInfo, delAdjustPlanRequireInfo, exportAdjustPlanRequireInfo } from "@/api/monthplan/adjustPlanRequireInfo";
import TltUploadForm from "@/views/components/tltUploadForm.vue";
import infoDialog from "./components/infoDialog.vue";

export default {
  name: "AdjustPlanRequireInfo",
  components: {
    TltUploadForm,
    infoDialog,
  },
  dicts: [
    "biz_factory_name",
    "biz_stor_type",
    "biz_plan_adjust_type",
    "biz_adjust_reason",
  ],
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
      searchPlanAdjustType: "",
      search: {
        factoryCode: "116",
      },
      query: {
        factoryCode: "116",
      },
    };
  },
  computed: {
    filterSearchReasons() {
      const reasons = this.dict.type.biz_adjust_reason || [];
      if (!this.searchPlanAdjustType) {
        return reasons;
      }
      return reasons.filter((d) => d.value && d.value.startsWith(this.searchPlanAdjustType));
    },
    columns() {
      return [
        { type: "selection", fixed: "left" },
        {
          prop: "factoryCode",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.mpAdjustPlanInfo.factoryCode"),
          minWidth: 120,
          formatter: (row, column, value) => this.selectDictLabel(this.dict.type.biz_factory_name, value),
        },
        {
          prop: "locationType",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.mpAdjustPlanInfo.locationType"),
          minWidth: 100,
          formatter: (row, column, value) => this.selectDictLabel(this.dict.type.biz_stor_type, value),
        },
        {
          prop: "adjustDate",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.mpAdjustPlanInfo.adjustDate"),
          minWidth: 110,
        },
        {
          prop: "area",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.mpAdjustPlanInfo.area"),
          minWidth: 100,
        },
        {
          prop: "planAdjustType",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.mpAdjustPlanInfo.planAdjustType"),
          minWidth: 110,
          formatter: (row, column, value) => this.selectDictLabel(this.dict.type.biz_plan_adjust_type, value),
        },
        {
          prop: "adjustReason",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.mpAdjustPlanInfo.adjustReason"),
          minWidth: 130,
          formatter: (row, column, value) => this.selectDictLabel(this.dict.type.biz_adjust_reason, value),
        },
        {
          prop: "structureName",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.mpAdjustPlanInfo.structureName"),
          minWidth: 120,
        },
        {
          prop: "materialCode",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.mpAdjustPlanInfo.materialCode"),
          minWidth: 120,
        },
        {
          prop: "materialDesc",
          align: "left",
          halign: "center",
          label: this.$t("ui.data.column.mpAdjustPlanInfo.materialDesc"),
          minWidth: 260,
        },
        {
          prop: "monthPlanQty",
          align: "right",
          halign: "center",
          label: this.$t("ui.data.column.mpAdjustPlanInfo.monthPlanQty"),
          minWidth: 110,
        },
        {
          prop: "adjustPlanQty",
          align: "right",
          halign: "center",
          label: this.$t("ui.data.column.mpAdjustPlanInfo.adjustPlanQty"),
          minWidth: 100,
        },
        {
          prop: "adjustFinalQty",
          align: "right",
          halign: "center",
          label: this.$t("ui.data.column.mpAdjustPlanInfo.adjustFinalQty"),
          minWidth: 110,
        },
        {
          prop: "realAdjustQty",
          align: "right",
          halign: "center",
          label: this.$t("ui.data.column.mpAdjustPlanInfo.realAdjustQty"),
          minWidth: 110,
        },
        {
          prop: "remark",
          align: "left",
          halign: "center",
          label: this.$t("ui.common.column.remark"),
          minWidth: 120,
        },
        {
          prop: "updateTime",
          align: "center",
          halign: "center",
          label: this.$t("common.updateTime"),
          minWidth: 160,
        },
        {
          align: "center",
          halign: "center",
          label: this.$t("ui.data.btn.option"),
          prop: "option",
          minWidth: 150,
          fixed: "right",
          render: ({ row }) => {
            return (
              <div>
                <el-button
                  v-hasPermi={["monthplan:adjustPlanRequireInfo:edit"]}
                  class="minus"
                  type="success"
                  onClick={() => this.handleEdit(row)}
                >
                  {this.$t("ui.frame.btn.update")}
                </el-button>
                <el-button
                  v-hasPermi={["monthplan:adjustPlanRequireInfo:remove"]}
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
          label: this.$t("ui.data.column.mpAdjustPlanInfo.factoryCode"),
          prop: "factoryCode",
          type: "select",
          dictData: this.dict.type.biz_factory_name,
          filterable: true,
        },
        {
          label: this.$t("ui.data.column.mpAdjustPlanInfo.adjustDate"),
          prop: "adjustDate",
          type: "date",
          dateType: "daterange",
          valueFormat: "yyyy-MM-dd",
        },
        {
          label: this.$t("ui.data.column.mpAdjustPlanInfo.area"),
          prop: "area",
        },
        {
          label: this.$t("ui.data.column.mpAdjustPlanInfo.planAdjustType"),
          prop: "planAdjustType",
          type: "select",
          dictData: this.dict.type.biz_plan_adjust_type,
          listeners: {
            change: (val) => {
              // 实时联动：选择调整类型后立即过滤调整原因选项
              this.searchPlanAdjustType = (val || "");
            },
          },
        },
        {
          label: this.$t("ui.data.column.mpAdjustPlanInfo.adjustReason"),
          prop: "adjustReason",
          type: "select",
          dictData: this.filterSearchReasons,
          filterable: true,
        },
        {
          label: this.$t("ui.data.column.mpAdjustPlanInfo.structureName"),
          prop: "structureName",
        },
        {
          label: this.$t("ui.data.column.mpAdjustPlanInfo.materialCode"),
          prop: "materialCode",
        },
        {
          label: this.$t("ui.data.column.mpAdjustPlanInfo.materialDesc"),
          prop: "materialDesc",
        },
      ];
    },
  },
  methods: {
    handleAdd() {
      this.$refs.infoRef && this.$refs.infoRef.show();
    },
    handleEdit(row) {
      this.$refs.infoRef && this.$refs.infoRef.show(row);
    },
    handleBatchEdit() {
      if (this.selection.length === 1) {
        this.handleEdit(this.selection[0]);
      }
    },
    handleDelete(row) {
      this.$confirm(this.$t("common.confirm.delete"), {
        type: "warning",
      }).then(() => {
        const ids = row.id;
        delAdjustPlanRequireInfo({ ids }).then((data) => {
          this.$modal.msgSuccess(data.msg);
          this.$set(this.page, "current", 1);
          this.getList();
        });
      });
    },
    handleBatchDelete() {
      if (!this.selection || this.selection.length === 0) {
        return;
      }
      this.$confirm(this.$t("common.confirm.delete"), {
        type: "warning",
      }).then(() => {
        const ids = this.selection.map((item) => item.id).join(",");
        delAdjustPlanRequireInfo({ ids }).then((data) => {
          this.$modal.msgSuccess(data.msg);
          this.selection = [];
          this.$set(this.page, "current", 1);
          this.getList();
        });
      });
    },
    handleSearch(data) {
      this.query = data;
      this.searchPlanAdjustType = (data && data.planAdjustType) || "";
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
      exportAdjustPlanRequireInfo(this.formatParams(false));
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

      if (params.adjustDate && params.adjustDate.length === 2) {
        params.adjustDateStart = params.adjustDate[0];
        params.adjustDateEnd = params.adjustDate[1];
        params.adjustDate = undefined;
      }

      return params;
    },
    // api
    async getList() {
      try {
        this.loading = true;
        const data = await listAdjustPlanRequireInfo(this.formatParams());
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
    this.search = {
      factoryCode: "116",
    };
    this.query = {
      factoryCode: "116",
    };
  },
  activated() {
    this.getList();
  },
};
</script>

<style lang="scss" scoped></style>
