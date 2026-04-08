
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
          v-hasPermi="['monthplan:mdmMonCycleSchStruConf:edit']"
          @click="handleAdd"
          >{{ $t("ui.frame.btn.add") }}</el-button
        >
        <el-button
          type="danger"
          v-hasPermi="['monthplan:mdmMonCycleSchStruConf:remove']"
          @click="handleDeleteAll"
          :disabled="selection.length == 0"
          >{{ $t("ui.frame.btn.delete") }}</el-button
        >
        <!-- <el-button
          type="primary"
          plain
          v-hasPermi="['monthplan:ProductMoldingLimit:add']"
          >{{ $t("生成当前周期排产") }}</el-button
        >
        <el-button
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
          v-hasPermi="['monthplan:mdmMonCycleSchStruConf:export']"
          >{{ $t("ui.frame.btn.export") }}</el-button
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
  </basic-container>
</template>
<script>
//lib
import { mapState, mapGetters } from "vuex";
//utils
import { downloadLink } from "@/utils/request";

import {
  listMonCycleSchStruConf,
  removeMonCycleSchStruConf,
} from "@/api/monthplan/mdmMonCycleSchStruConf";
import { selectSkuStructure } from "@/api/monthplan/skuStructure";
//components
import tltUpload from "@/components/tltUpload/tltUpload.vue";

import infoDialog from "./components/infoDialog.vue";

export default {
  name: "PeriodicSchedSetup",
  components: {
    tltUpload,
    infoDialog,
  },
  dicts: ["LINE_TYPE", "JOB_TYPE", "biz_factory_name"],
  provide() {
    return {
      parentDict: this.dict,
    };
  },
  data() {
    return {
      // structureList:[],
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
    ...mapGetters("globalList", ["structureList"]),
    ...mapState({
      moldingMachines: (state) => state.molding.machines,
    }),
    columns() {
      let columns = [
        { type: "selection", fixed: "left" },
        {
          prop: "factoryCode",
          width: 180,
          label: this.$t("common.factory"),
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_factory_name, value);
          },
        },
        {
          prop: "year",
          width: 180,
          label: this.$t("ui.data.colume.year"),
        },
        {
          prop: "month",
          width: 180,
          label: this.$t("ui.data.colume.month"),
        },
        {
          prop: "structureName",
          label: this.$t("ui.data.column.finishStock.structureName"),
        },
        {
          prop: "turnoverMonth",
          width: 180,
          label: this.$t("ui.data.column.curingPlan.turnoverMonth"),
        },
        {
          prop: "minVulcanizingMachine",
          width: 180,
          label: this.$t("ui.data.column.curingPlan.minVulcanizingMachine"),
        },

        // {
        //   prop: "备注",
        //   label: this.$t("备注"),
        // },
        {
          prop: "updateTime",
          width: 180,
          label: this.$t("ui.data.column.scheduleAdjust.updata"),
        },
        {
          align: "center",
          label: this.$t("ui.data.btn.option"),
          minWidth: 180,
          width: 200,
          fixed: "right",
          render: ({ row }) => {
            return (
              <div>
                  <el-button
                v-hasPermi={["monthplan:mdmMonCycleSchStruConf:edit"]}
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
                  v-hasPermi={["monthplan:mdmMonCycleSchStruConf:remove"]}
                >
                  {this.$t("ui.frame.btn.delete")}
                </el-button>

              </div>
            );
          },
        },
        // {
        //   align: "center",
        //   label: this.$t("ui.data.btn.option"),
        //   fixed: "right",
        //   render: ({ row }) => {
        //     return (
        //       <div>
        //         <el-button
        //           v-hasPermi={["monthplan:ProductMoldingLimit:edit"]}
        //           class="minus"
        //           type="success"
        //           onClick={() => this.handleEdit(row)}
        //         >
        //           {this.$t("ui.frame.btn.update")}
        //         </el-button>

        //       </div>
        //     );
        //   },
        // },
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
          dateType: "month",
          valueFormat: "yyyy-MM",
        },
        {
          prop: "structureName",
          label: this.$t("ui.data.column.finishStock.structureName"),
          type: "select",
          dictData: this.structureList,
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
        removeMonCycleSchStruConf({ ids }).then((data) => {
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
        removeMonCycleSchStruConf({ ids }).then((data) => {
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
        "/monthplan/mdmMonCycleSchStruConf/export",
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

      if (params.yearMonth) {
        let array = params.yearMonth.split("-");
        params.year = array[0];
        params.month = array[1];
      }

      return params;
    },
    // api
    async getList() {
      try {
        this.loading = true;
        const data = await listMonCycleSchStruConf(this.formatParams());
        this.data = data.rows;
        this.page.total = data.total;
      } catch (error) {
        console.error(error);
      } finally {
        this.loading = false;
      }
    },
    // async getStructureList() {
    //   try {

    //     const res = await selectSkuStructure({
    //       pageSize: 1000,
    //       pageNum: 1,

    //     });
    //     let list=[]
    //     for (let i = 0; i < res.rows.length; i++) {
    //       let obj={
    //         label:res.rows[i].structureName,
    //         value:res.rows[i].structureName
    //       }
    //       list.push(obj)

    //     }
    //     this.structureList=list
    //     console.log(res);
    //   } catch (error) {
    //     console.log(error);
    //   } finally {
    //   }
    // },
  },

  created() {
    // this.getStructureList()
    const now = new Date();

    // const year = now.getFullYear();
    // const month = String(now.getMonth() + 1).padStart(2, "0"); // ui.data.colume.month从0开始，需要+1
    const nextMonth = new Date(now.getFullYear(), now.getMonth() + 1, 1);
      const year = nextMonth.getFullYear();
      const month = nextMonth.getMonth() + 1; // 月份从0开始，需要+1
    let defaultParams = {
      factoryCode: "116",
      yearMonth: `${year}-${month}`,
    };
    this.search = {
      ...defaultParams,
    };
    this.query = {
      ...defaultParams,
    };
    this.getList();
  },
  activated() {},
};
</script>
<style lang="scss" scoped>
.more-btn {
  margin: 2px 0;
  width: 100%;
}
</style>
