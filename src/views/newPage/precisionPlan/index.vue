<template>
  <basic-container>
    <page-table
      tableRef="precisionPlanMainTable"
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
          v-hasPermi="['lh:precisionPlan:generate']"
          @click="handleGenerate"
          >{{ $t("ui.lh.precisionPlan.generate") }}</el-button
        >
        <el-button
          type="primary"
          plain
          v-hasPermi="['lh:precisionPlan:edit']"
          :disabled="selection.length !== 1"
          @click="handleEdit(selection[0])"
          >{{ $t("ui.frame.btn.edit") }}</el-button
        >
        <el-button
          type="danger"
          v-hasPermi="['lh:precisionPlan:remove']"
          :disabled="selection.length === 0"
          @click="handleDelete"
          >{{ $t("ui.frame.btn.delete") }}</el-button
        >
        <el-button
          v-hasPermi="['lh:precisionPlan:import']"
          @click="$refs.tltUpload.handleImport()"
          >{{ $t("ui.frame.btn.import") }}</el-button
        >
        <el-button
          @click="handleExport"
          v-hasPermi="['lh:precisionPlan:export']"
          >{{ $t("ui.frame.btn.export") }}</el-button
        >
      </template>
    </page-table>
    <tlt-upload
      ref="tltUpload"
      downloadUrl="/schedule/lhPrecisionPlan/importTemplate"
      uploadUrl="/schedule/lhPrecisionPlan/importData"
      @uploadSuccess="getList"
    />
    <infoDialog ref="infoRef" @success="getList" />
  </basic-container>
</template>

<script>
import { mapState } from "vuex";
import { downloadLink } from "@/utils/request";
import {
  listPrecisionPlan,
  removePrecisionPlan,
  generateFromMes,
} from "@/api/lh/precisionPlan";
import tltUpload from "@/components/tltUpload/tltUpload.vue";
import infoDialog from "./components/infoDialog.vue";

export default {
  name: "precisionPlan",
  components: {
    tltUpload,
    infoDialog,
  },
  dicts: ["MACHINE_ACCURACY_TYPE", "lh_precision_completion_status", "lh_precision_data_source", "biz_factory_name"],
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
      yearList: [],
    };
  },
  computed: {
    ...mapState({
      moldingMachines: (state) => state.molding.machines,
    }),
    columns() {
      return [
        { type: "selection", fixed: "left" },
        {
          prop: "factoryCode",
          label: this.$t("ui.data.column.lhPrecisionPlan.factoryCode"),
          type: "select",
          dicData: this.dict.biz_factory_name,
        },
        {
          prop: "machineCode",
          label: this.$t("ui.data.column.lhPrecisionPlan.machineCode"),
        },
        {
          prop: "precisionType",
          label: this.$t("ui.data.column.lhPrecisionPlan.precisionType"),
          type: "select",
          dicData: this.dict.MACHINE_ACCURACY_TYPE,
        },
        {
          prop: "planDate",
          label: this.$t("ui.data.column.lhPrecisionPlan.planDate"),
        },
        {
          prop: "actualDate",
          label: this.$t("ui.data.column.lhPrecisionPlan.actualDate"),
        },
        {
          prop: "dueDate",
          label: this.$t("ui.data.column.lhPrecisionPlan.dueDate"),
        },
        {
          prop: "daysToDue",
          label: this.$t("ui.data.column.lhPrecisionPlan.daysToDue"),
        },
        {
          prop: "dataSource",
          label: this.$t("ui.data.column.lhPrecisionPlan.dataSource"),
          type: "select",
          dicData: this.dict.lh_precision_data_source,
        },
        {
          prop: "remark",
          label: this.$t("ui.data.column.remark"),
        },
        {
          prop: "updateTime",
          label: this.$t("ui.data.column.lhPrecisionPlan.updateTime"),
        },
      ];
    },
    searchColumns() {
      return [
        {
          prop: "year",
          label: this.$t("ui.data.column.lhPrecisionPlan.year"),
          type: "select",
          dicData: this.yearList,
          value: new Date().getFullYear().toString(),
        },
        {
          prop: "machineCode",
          label: this.$t("ui.data.column.lhPrecisionPlan.machineCode"),
          type: "select",
          dicData: this.moldingMachines,
          props: {
            label: "machineCode",
            value: "machineCode",
          },
        },
        {
          prop: "precisionType",
          label: this.$t("ui.data.column.lhPrecisionPlan.precisionType"),
          type: "select",
          dicData: this.dict.MACHINE_ACCURACY_TYPE,
        },
        {
          prop: "planDate",
          label: this.$t("ui.data.column.lhPrecisionPlan.planDate"),
          type: "date",
        },
        {
          prop: "actualDate",
          label: this.$t("ui.data.column.lhPrecisionPlan.actualDate"),
          type: "date",
        },
      ];
    },
  },
  methods: {
    handleEdit(row) {
      if (this.$refs.infoRef) {
        this.$refs.infoRef.show(row);
      }
    },
    handleDelete() {
      this.$confirm(this.$t("common.confirm.delete"), {
        type: "warning",
      }).then(() => {
        const ids = this.selection.map((item) => item.id);
        removePrecisionPlan(ids).then((res) => {
          this.$modal.msgSuccess(res.msg);
          this.$set(this.page, "current", 1);
          this.getList();
        });
      });
    },
    handleGenerate() {
      const year = this.query.year || new Date().getFullYear();
      this.$confirm(
        this.$t("ui.lh.precisionPlan.generate.confirm", [year]),
        {
          type: "warning",
        }
      ).then(() => {
        this.loading = true;
        generateFromMes(year)
          .then((res) => {
            this.$modal.msgSuccess(
              this.$t("ui.lh.precisionPlan.generate.success", [res.data])
            );
            this.$set(this.page, "current", 1);
            this.getList();
          })
          .catch(() => {
            this.$modal.msgError(this.$t("ui.lh.precisionPlan.generate.fail"));
          })
          .finally(() => {
            this.loading = false;
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
      downloadLink(
        "/schedule/lhPrecisionPlan/export",
        this.formatParams(false)
      );
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

      if (params.planDate && params.planDate[0]) {
        params.planDateStart = params.planDate[0];
        params.planDateEnd = params.planDate[1];
        params.planDate = undefined;
      }

      if (params.actualDate && params.actualDate[0]) {
        params.actualDateStart = params.actualDate[0];
        params.actualDateEnd = params.actualDate[1];
        params.actualDate = undefined;
      }

      return params;
    },
    async getList() {
      try {
        this.loading = true;
        const data = await listPrecisionPlan(this.formatParams());
        this.data = data.rows;
        this.page.total = data.total;
      } catch (error) {
        console.error(error);
      } finally {
        this.loading = false;
      }
    },
    initYearList() {
      const currentYear = new Date().getFullYear();
      const years = [];
      for (let i = currentYear - 2; i <= currentYear + 2; i++) {
        years.push({
          label: i.toString(),
          value: i.toString(),
        });
      }
      this.yearList = years;
    },
  },
  created() {
    this.initYearList();
    if (this.moldingMachines.length === 0) {
      this.$store.dispatch("molding/getMachineList");
    }
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
