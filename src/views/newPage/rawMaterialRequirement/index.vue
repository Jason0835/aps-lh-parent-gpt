
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
        <!-- <el-button
          type="primary"
          plain
          v-hasPermi="['monthplan:ProductMoldingLimit:add']"
          @click="handleAdd"
          >{{ $t("ui.frame.btn.add") }}</el-button
        > -->
        <el-button
          type="primary"
          v-hasPermi="['maindata:rawMaterialRequirePlan:generate']"
          @click="generatePlan"
          >{{ $t("ui.data.column.rawMaterial.genger") }}</el-button
        >
        <el-button
          type="danger"
          v-hasPermi="['maindata:rawMaterialRequirePlan:remove']"
          :disabled="selection.length == 0"
          @click="handleDeleteAll"
          >{{ $t("ui.frame.btn.delete") }}</el-button
        >
        <!-- <el-button
          type="primary"
          plain
          v-hasPermi="['monthplan:ProductMoldingLimit:add']"
          >{{ $t("查单模拟排产") }}</el-button
        > -->
        <!-- <el-button
          type="warning"
          v-hasPermi="['monthplan:ProductMoldingLimit:edit']"
          @click="handleEdit(selection[0])"
          >{{ $t("ui.frame.btn.modify") }}</el-button
        > -->
        <!-- <el-button
          v-hasPermi="['monthplan:ProductMoldingLimit:import']"
          @click="$refs.tltUpload.handleImport()"
          >{{ $t("ui.frame.btn.import") }}</el-button
        > -->
        <el-button
          @click="handleExport"
          v-hasPermi="['maindata:rawMaterialRequirePlan:export']"
          >{{ $t("ui.frame.btn.export") }}</el-button
        >
        <el-button
          type="primary"
          @click="goRawUsage"
          plain
          v-hasPermi="['maindata:rawWeekUsage:list']"
          >{{ $t("ui.data.column.rawMaterial.usage") }}</el-button
        >
      </template>
    </page-table>
    <!-- <el-button style="display: none" ref="hidePopoverBtnRef"></el-button> -->
    <tlt-upload
      ref="tltUpload"
      downloadUrl="/mdm/productMoldingLimit/importTemplate"
      uploadUrl="/mdm/productMoldingLimit/importData"
      @uploadSuccess="getList"
    />
    <infoDialog ref="infoRef" @success="getList" />
    <createDialog ref="createRef" @success="getList" />
  </basic-container>
</template>
<script>
//lib
import { mapState } from "vuex";
//utils
import { downloadLink } from "@/utils/request";

import {
  listMdmProductConstruction,
  generateMdmProductConstruction,
  removeMdmProductConstruction,
  getMdmProductVersion,
} from "@/api/maindata/rawMaterialRequirePlan​";

//components
import tltUpload from "@/components/tltUpload/tltUpload.vue";

import infoDialog from "./components/infoDialog.vue";
import createDialog from "./components/createDialog.vue";

export default {
  name: "RawMaterialRequirement",
  components: {
    tltUpload,
    infoDialog,
    createDialog,
  },
  dicts: [
    "LINE_TYPE",
    "JOB_TYPE",
    "biz_factory_name",
    "biz_rawMaterial_type",
    "biz_yes_no",
  ],
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
      selectList: [],
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
          prop: "version",
          label: this.$t("plan.planProduction.planVersion"),
        },
        {
          prop: "materialType",
          label: this.$t("ui.data.column.trialPlan.trialType"),
          formatter: (row, column, value) => {
            return this.selectDictLabel(
              this.dict.type.biz_rawMaterial_type,
              value
            );
          },
          width: 120,
        },
        {
          prop: "materialDesc",
          label: this.$t("common.name"),
          width: 200,
        },
        {
          prop: "materialCode",
          label: this.$t("ui.data.column.monthplan.oriMaterialCode"),
          width: 180,
        },
        // {
        //   prop: "物料代码",
        //   label: this.$t("物料代码"),
        // },
        // {
        //   prop: "材料名称",
        //   label: this.$t("材料名称"),
        // },
        {
          prop: "curMonthQty",
          label: this.$t("ui.data.column.rawMaterial.curMonthQty"),
        },
        {
          prop: "t1MonthQty",
          label: this.$t("ui.data.column.rawMaterial.t1MonthQty"),
        },

        {
          prop: "t2MonthQty",
          label: this.$t("ui.data.column.rawMaterial.t2MonthQty"),
        },
        {
          prop: "curMonthRudrQty",
          label: this.$t("ui.data.column.rawMaterial.curMonthRudrQty"),
        },
        {
          prop: "t1MonthEudrQty",
          label: this.$t("ui.data.column.rawMaterial.t1MonthEudrQty"),
        },
        {
          prop: "t2MonthEudrQty",
          label: this.$t("ui.data.column.rawMaterial.t2MonthEudrQty"),
        },
        {
          prop: "remark",
          label: this.$t("common.remark"),
          width: 180,
        },
        {
          prop: "updateTime",
          label: this.$t("ui.data.column.scheduleAdjust.updata"),
          width: 200,
          render: ({ row }) => {
            return (
              <div>{row.updateTime ? row.updateTime : row.createTime}</div>
            );
          },
        },
        {
          align: "center",
          label: this.$t("ui.data.btn.option"),
          fixed: "right",
          width: 200,
          render: ({ row }) => {
            return (
              <div>
                <el-button
                  v-hasPermi={["maindata:rawMaterialRequirePlan:edit"]}
                  class="minus"
                  type="success"
                  onClick={() => this.handleEdit(row)}
                >
                  {this.$t("ui.frame.btn.update")}
                </el-button>
                <el-button
                  v-hasPermi={["maindata:rawMaterialRequirePlan:remove"]}
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
          label: this.$t("common.factory"),
          prop: "factoryCode",
          type: "select",
          dictData: this.dict.type.biz_factory_name,
          clearable: false,
          listeners: {
            change: this.handleFactoryChange,
          },
        },
        {
          prop: "yearMonth",
          label: this.$t("ui.data.column.report.proSizeSummary.yearMonth"),
          type: "date",
          dateType: "month",
          valueFormat: "yyyy-MM",
          clearable: false,
          listeners: {
            change: this.handleYearMonthChange,
          },
        },
        {
          prop: "materialCode",
          label: this.$t("ui.data.column.monthplan.oriMaterialCode"),
        },
        {
          prop: "materialDesc",
          label: this.$t("common.name"),
        },
        // {
        //   prop: "物料代码",
        //   label: this.$t("物料代码"),
        // },
        // {
        //   prop: "材料名称",
        //   label: this.$t("材料名称"),
        // },
        {
          prop: "version",
          label: this.$t("plan.planProduction.planVersion"),
          type: "select",
          dictData: this.selectList,
        },
        {
          prop: "materialType",
          label: this.$t("ui.data.column.trialPlan.trialType"),
          type: "select",
          dictData: this.dict.type.biz_rawMaterial_type,
        },
      ];
    },
  },
  methods: {
    handleYearMonthChange(val) {
      this.search = {
        ...this.search,
        yearMonth: val,
      };
      this.query = {
        ...this.search,
        yearMonth: val,
      };
      this.getVersion();

      // this.$set(this.search,'materialCode',val)
      // this.$set(this.query,'materialCode',val)
    },
    handleFactoryChange(val) {
      this.search = {
        ...this.search,
        factoryCode: val,
      };
      this.query = {
        ...this.search,
        factoryCode: val,
      };

      this.getVersion();
    },
    goRawUsage() {
      {
        /* this.$router.push("/rawMaterial/rawWeekUsage"); */
      }
      this.$router.push({
        name: "RawWeekUsage",
      });
    },
    async generatePlan() {
      if (this.$refs.createRef) {
        this.$refs.createRef.show();
      }
      return;
      try {
        this.loading = true;
        let res = await generateMdmProductConstruction(this.formatParams());
        this.$modal.msgSuccess(res.msg);
        this.getList();
      } catch (error) {
        console.error(error);
      } finally {
        this.loading = false;
      }
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
        removeMdmProductConstruction({ ids }).then((data) => {
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
        removeMdmProductConstruction({ ids }).then((data) => {
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
      downloadLink(
        "/maindata/rawMaterialRequirePlan/export",
        this.formatParams(false)
      );
    },

    formatParams(hasPage = true) {
      const params = {
        ...this.query,
        ...this.search,
      };

      if (hasPage) {
        params.pageSize = this.page.pageSize;
        params.pageNum = this.page.current;
      }

      if (params.yearMonth) {
        let arr = params.yearMonth.split("-");
        params.year = arr[0];
        params.month = arr[1];
      }

      return params;
    },
    // api
    async getList() {
      try {
        this.loading = true;

        const data = await listMdmProductConstruction(this.formatParams());
        this.data = data.rows;
        this.page.total = data.total;
      } catch (error) {
        console.error(error);
      } finally {
        this.loading = false;
      }
    },
    async getVersion(isGet = false) {
      try {
        this.loading = true;
        const params = {
          ...this.query,
          ...this.search,
        };
        let arr = params.yearMonth.split("-");
        const res = await getMdmProductVersion({
          factoryCode: params.factoryCode,
          year: arr[0],
          month: arr[1],
        });
        let list = [];
        for (let i = 0; i < res.length; i++) {
          list.push({
            label: res[i].version,
            value: res[i].version,
          });
        }
        this.selectList = list;
        if (res.length > 0) {
          this.$set(this.query, "version", res[0].version);
          this.$set(this.search, "version", res[0].version);
        } else {
          this.$set(this.query, "version", "");
          this.$set(this.search, "version", "");
        }
      } catch (error) {
        console.error(error);
      } finally {
        this.loading = false;
        if (isGet) {
          this.getList();
        }
      }
    },
  },
  created() {
    const now = new Date();
    const nextMonth = new Date(now.getFullYear(), now.getMonth() + 1, 1);
    const year = nextMonth.getFullYear();
    const month = nextMonth.getMonth() + 1; // 月份从0开始，需要+1

    let defaultParams = {
      factoryCode: "116",
      yearMonth: `${year}-${month < 10 ? "0" + month : month}`,
    };
    this.search = {
      ...defaultParams,
    };
    this.query = {
      ...defaultParams,
    };
    // this.getList();
    this.getVersion(true);
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
