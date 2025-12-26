
<template>
  <basic-container>
    <page-table
      tableRef="cxFixedMachineMainTable"
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
          v-hasPermi="['monthplan:mpTrialPlan:edit']"
          @click="handleAdd"
          >{{ $t("ui.frame.btn.add") }}</el-button
        >
        <el-button
          type="danger"
          v-hasPermi="['monthplan:mpTrialPlan:remove']"
          :disabled="selection.length == 0"
          @click="handleDeleteAll"
          >{{ $t("ui.frame.btn.delete") }}</el-button
        >
        <el-button
          v-hasPermi="['monthplan:mpTrialPlan:import']"
          @click="$refs.tltUpload.handleImport()"
          >{{ $t("ui.frame.btn.import") }}</el-button
        >
        <el-button
          @click="handleExport"
          v-hasPermi="['monthplan:mpTrialPlan:export']"
          >{{ $t("ui.frame.btn.export") }}</el-button
        >
      </template>
    </page-table>
    <!-- <el-button style="display: none" ref="hidePopoverBtnRef"></el-button> -->
    <tlt-upload
      ref="tltUpload"
      downloadUrl="/monthplan/mpTrialPlan/importTemplate"
      uploadUrl="/monthplan/mpTrialPlan/importData"
      @uploadSuccess="getList"
    />
    <infoDialog ref="infoRef" @success="getList" />
  </basic-container>
</template>
<script>
//lib
import { mapState } from "vuex";
//utils
import { downloadLink } from "@/utils/request";

import {
  listMpTrialPlan,
  removeMpTrialPlan,
} from "@/api/monthplan/mpTrialPlan";

//components
import tltUpload from "@/components/tltUpload/tltUpload.vue";

import infoDialog from "./components/infoDialog.vue";
import { di } from "@fullcalendar/core/internal-common";

export default {
  name: "trialPlan",
  components: {
    tltUpload,
    infoDialog,
  },
  dicts: ["LINE_TYPE", "JOB_TYPE", "biz_factory_name",'biz_trial_type','biz_construction_stage','biz_urgency_type'],
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
      search: {},
      query: {},
      importDefaultValue: {},
      importRules: {},
    };
  },
  computed: {
    ...mapState({
      moldingMachines: (state) => state.molding.machines,
    }),
    columns() {
      let columns = [
        { type: "selection", fixed: "left" },
        {
          prop: "factoryCode",
          label: this.$t("common.factory"),
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_factory_name, value);
          },
        },
        {
          prop: "year",
          label: this.$t("ui.data.colume.year"),
        },
        {
          prop: "month",
          label: this.$t("ui.data.colume.month"),
        },
        {
          prop: "trialType",
          label: this.$t("ui.data.column.trialPlan.trialType"),
          width: 120,
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_trial_type, value);
          },
        },
        {
          prop: "specifications",
          label: this.$t("ui.data.column.trialPlan.specifications"),
          width: 120,
        },
        {
          prop: "pattern",
          label: this.$t("ui.data.column.modelinfo.pattern"),
          width: 120
        },
        {
          prop: "trialStatus",
          label: this.$t("ui.data.column.trialPlan.trialStatus"),
          width: 120,
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_construction_stage, value);
          },
        },
        {
          prop: "trialQty",
          label: this.$t("common.num"),
          width: 120,
        },
        {
          prop: "urgencyType",
          label: this.$t("ui.data.column.trialPlan.urgencyType"),
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_urgency_type, value);
          },
        },
        {
          prop: "planDate",
          label: this.$t("ui.data.column.trialPlan.planDate"),
          width: 120,
        },
        {
          prop: "productionDate",
          label: this.$t("ui.data.column.trialPlan.productionDate"),
        },
        {
          prop: "madeInfo",
          label: this.$t("ui.data.column.trialPlan.madeInfo"),
        },
        {
          prop: "moldingInfo",
          label: this.$t("ui.data.column.trialPlan.moldingInfo"),
        },
        {
          prop: "vulcanizationInfo",
          label: this.$t("ui.data.column.trialPlan.vulcanizationInfo"),
        },
        {
          prop: "updateByName",
          label: this.$t("common.updateByName"),
        },
        {
          prop: "deptName",
          label: this.$t("ui.data.column.trialPlan.deptName"),
        },
        {
          prop: "remark",
          label: this.$t("common.remark"),
          width: 120,
        },
        {
          prop: "updateTime",
          label: this.$t("ui.data.column.scheduleAdjust.updata"),
          width: 180,
        },
        {
          align: "center",
          label: this.$t("ui.data.btn.option"),
          fixed: "right",
          width:120,
          render: ({ row }) => {
            return (
              <div>
                <el-button
                  v-hasPermi={["monthplan:mpTrialPlan:edit"]}
                  class="minus"
                  type="success"
                  onClick={() => this.handleEdit(row)}
                >
                  {this.$t("ui.frame.btn.update")}
                </el-button>
                <el-button
                  v-hasPermi={["monthplan:mpTrialPlan:remove"]}
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
          prop: "factoryCode",
          label: this.$t("common.factory"),
          type: "select",
          dictData: this.dict.type.biz_factory_name,
        },
        {
          prop: "yearMonth",
          label: this.$t("ui.data.colume.yearMonth"),
          type: "date",
          dateType:'month',
          valueFormat: "yyyy-MM",
        },
        {
          prop: "specifications",
          label: this.$t("ui.data.column.trialPlan.specifications"),

        },
        {
          prop: "trialStatus",
          label: this.$t("ui.data.column.trialPlan.trialStatus"),
          type: "select",
          dictData: this.dict.type.biz_construction_stage,
        },
        {
          prop: "urgencyType",
          label: this.$t("ui.data.column.trialPlan.urgencyType"),
          type: "select",
          dictData: this.dict.type.biz_urgency_type,
        },
        {
          prop: "planDate",
          label: this.$t("ui.data.column.trialPlan.planDate"),
          type: "date",
          valueFormat: "yyyy-MM-dd",
        },
        {
          prop: "pattern",
          label: this.$t("ui.data.column.modelinfo.pattern")
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
    handleDelete(row) {
      this.$confirm(this.$t("common.confirm.delete"), {
        type: "warning",
      }).then(() => {
        const ids = row.id;
        removeMpTrialPlan({ ids }).then((data) => {
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
        removeMpTrialPlan({ ids }).then((data) => {
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
        //默认排序
        this.sort = {};
      }
      this.getList();
    },
    handleSelectionChange(rows) {
      this.selection = rows;
    },
    handleExport() {
      downloadLink("/monthplan/mpTrialPlan/export", this.formatParams(false));
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

      if (params.yearMonth) {
        let arr=params.yearMonth.split("-");
        params.year = arr[0];
        params.month =arr[1];
        params.yearMonth = undefined;
      }

      return params;
    },
    // api
    async getList() {
      try {
        this.loading = true;

        const data = await listMpTrialPlan(this.formatParams());
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
