
<template>
  <basic-container>
    <page-table
      ref="consoleRef"
      tableRef="ConsoleMainTable"
      :calcHeight="true"
      v-loading="loading"
      :columns="columns"
      :searchColumns="searchColumns"
      :data="data"
      :page="undefined"
      :search="search"
      @refresh="getList"
      @search="handleSearch"
      @pageChange="handlePageChange"
      @sort-change="handleSortChange"
      @selection-change="handleSelectionChange"
      :showSummary="false"
      :selectArea="false"
      :span-method="spanMethod"
      @select-all="handleSelectAll"
    >
      <template slot="header">
        <el-button
          type="primary"
          plain
          v-hasPermi="['monthplan:monthSaleOrderPlan:createSaleRequirePlan']"
          @click="handleAdd"
          >{{ $t("选择需求计划版本") }}</el-button
        >
        <!-- <el-button
          type="primary"
          plain
          v-hasPermi="['monthplan:monthSaleOrderPlan:createSaleRequirePlan']"
          >{{ $t("插单模拟排产") }}</el-button
        >
        <el-button
          type="primary"
          plain
          v-hasPermi="['monthplan:monthSaleOrderPlan:createSaleRequirePlan']"
          >{{ $t("订单预测") }}</el-button
        > -->
        <el-button
          type="primary"
          plain
          v-hasPermi="['monthplan:console:finalized']"
          :disabled="this.selection.length !== 1"
          @click="handleFinalized"
          >{{ $t("定稿") }}</el-button
        >
        <el-button
          type="primary"
          plain
          v-hasPermi="['monthplan:monthSaleOrderPlan:add']"
          @click="handleAdd"
          >{{ $t("调整") }}</el-button
        >
        <el-button
          type="primary"
          plain
          v-hasPermi="['monthplan:monthSaleOrderPlan:add']"
          @click="handleAdd"
          >{{ $t("上线设置") }}</el-button
        >
        <!-- <el-button
          type="primary"
          plain
          v-hasPermi="['monthplan:monthSaleOrderPlan:edit']"
          @click="handleEdit(selection[0])"
          >{{ $t("ui.frame.btn.modify") }}</el-button
        > -->
        <!-- <el-button
          type="danger"
          plain
          :disabled="selection.length === 0"
          v-hasPermi="['monthplan:monthSaleOrderPlan:remove']"
          @click="handleDelete(selection)"
          >{{ $t("ui.frame.btn.delete") }}</el-button
        > -->
        <el-button
          v-hasPermi="['monthplan:monthSaleOrderPlan:import']"
          @click="$refs.tltUpload.handleImport()"
          >{{ $t("ui.frame.btn.import") }}</el-button
        >
        <!-- <el-button
          @click="handleExport"
          v-hasPermi="['monthplan:monthSaleOrderPlan:export']"
          >{{ $t("ui.frame.btn.export") }}</el-button
        > -->
      </template>
    </page-table>
    <!-- <el-button style="display: none" ref="hidePopoverBtnRef"></el-button> -->
    <tlt-upload
      ref="tltUpload"
      downloadUrl="/monthplan/mouldingDayResult/importTemplate"
      uploadUrl="/monthplan/mouldingDayResult/importData"
      @uploadSuccess="getList"
    />
    <infoDialog ref="infoRef" @success="getList" />
    <addDialog ref="addRef" @success="getList" />
    <checkDialog ref="checkRef" @success="getList" />
    <noVersionList ref="noVersionListRef" @success="getList" />

    <finalizedDialog ref="finRef" @success="getList" />
  </basic-container>
</template>
<script>
//lib
import moment from "moment";
//utils
import { downloadLink } from "@/utils/request";
//interface
import {
  listConsole,
  removeConsole,
  editConsole,
  createSaleRequirePlan,
  initFactoryProduction,
  factoryMouldingProduction,
  factoryWholeCourseProduction,
  deleteMonthPlanProductionVersion,
  deleteMonthPlanRequire,
} from "@/api/factory/console";
//components
import { getVersionList as requireProductionPlanVersionList } from "@/api/demand/requireProductionPlan";
import { listProductionVersionList } from "@/api/monthplan/mouldingDayResult";
import tltUpload from "@/components/tltUpload/tltUpload.vue";

import infoDialog from "./components/infoDialog.vue";
import addDialog from "./components/addDialog.vue";
import checkDialog from "./components/checkDialog.vue";
import finalizedDialog from "./components/finalizedDialog.vue";
import noVersionList from "./components/noVersionList.vue";
import cos from "highlight.js/lib/languages/cos";
import { f } from "tlt-ui";

export default {
  name: "Console",
  components: {
    tltUpload,
    addDialog,
    infoDialog,
    finalizedDialog,
    noVersionList,
    checkDialog
  },
  dicts: [
    "biz_factory_name",
    "biz_stor_type",
    "biz_yes_no",
    "biz_channel_type",
    "biz_brand_type",
    "biz_product_type",
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
      map: null,
      planVersionList: [],
      productionVersionList: [],
    };
  },
  computed: {
    columns() {
      let columns = [
        { type: "selection", fixed: "left" },
        {
          label: this.$t("ui.data.demandPlan.monthPlanVersion"),
          prop: "monthPlanVersion",
          align: "center",
          render: ({ row }) => {
            return (
              <div>
                {row.monthPlanVersion}
                <div>
                  <text-button
                    onClick={() => this.handleRouterMonthPlanVersion(row)}
                  >
                  {this.$t("plan.planProduction.detail")}
                  </text-button>
                  <text-button
                    onClick={() => {
                      this.handleDelete(row);
                    }}
                  >
                    {this.$t("common.button.delete")}
                  </text-button>
                  <text-button
                    onClick={() => {
                      downloadLink("/monthplan/demandPlanSum/export", {
                        year: row.year,
                        month: row.month,
                        monthPlanVersion: row.monthPlanVersion,
                      });
                    }}
                  >
                    {this.$t("common.button.export")}
                  </text-button>
                  {/* <text-button onClick={() => {}}>导入月计划</text-button> */}
                </div>
              </div>
            );
          },
        },
        {
          prop: "factoryCode",
          label: this.$t("common.factory"),
          align: "center",

          formatter: (row) => {
            return this.selectDictLabel(
              this.dict.type.biz_factory_name,
              row.factoryCode
            );
          },
        },
        {
          label: this.$t("ui.data.column.monthplan.productType"),
          prop: "productTypeCode",
          align: "center",

          render: ({ row }) => {
            return (
              <div>
                <div>
                  {this.selectDictLabel(
                    this.dict.type.biz_product_type,
                    row.productTypeCode
                  )}
                </div>
                <div>
                  <text-button
                    onClick={() => {
                      this.handleGenerate(row);
                    }}
                  >
                    {this.$t("common.button.generate")}
                  </text-button>
                </div>
              </div>
            );
          },
        },
        {
          label: this.$t("排结构"),
          prop: "init",
          align: "center",

          render: ({ row }) => {
            if (!row.productionVersion) {
              return "";
            }

            return (
              <div>
                <div>
                  <text-button
                    onClick={() => {
                      this.handleInit(row);
                    }}
                  >
                    {this.$t("生成下一步")}
                  </text-button>
                </div>
                <div>
                  <text-button onClick={() => this.handleInitDetail(row)}>
                    {this.$t("plan.planProduction.detail")}
                  </text-button>
                </div>
              </div>
            );
          },
        },
        {
          label: this.$t("排模具"),
          prop: "mould",
          align: "center",
          render: ({ row }) => {
            if (!row.productionVersion) {
              return "";
            }

            return (
              <div>
                <div>
                  <text-button
                    onClick={() => {
                      this.handleMould(row);
                    }}
                  >
                    {this.$t("生成下一步")}
                  </text-button>
                </div>
              </div>
            );
          },
        },

        {
          label: this.$t("ui.data.column.console.productionVersion"),
          prop: "productionVersion",
          align: "center",
          minWidth: 180,
          render: ({ row }) => {
            if (!row.productionVersion) {
              return "";
            }

            return (
              <div>
                <div> {row.productionVersion}</div>

                <text-button
                  onClick={() => this.handleRouterProductionVersions(row)}
                >
                  { this.$t("排产明细")}
                </text-button>
                <text-button
                  onClick={() =>
                    this.handleRouterMonthPlanNoProductionPlan(row)
                  }
                >
                  { this.$t("未排产明细")}
                </text-button>
                <text-button onClick={() => this.handleDeleteChild(row)}>
                  {this.$t("common.button.delete")}
                </text-button>
                {/* <text-button onClick={() => this.handleReport(row)}>报表</text-button> */}
              </div>
            );
          },
        },
        {
          label: this.$t("ui.data.column.console.isFinal"),
          prop: "isFinal",
          align: "center",
          // formatter: (row, column, value) => {
          //   return this.selectDictLabel(this.dict.type.biz_yes_no, value);
          // },
          render: ({ row }) => {
            return (
             <span>{row.isFinal==1?'是':''}</span>
            );
          },
        },
        // {
        //   prop: "remark",
        //   label: this.$t("common.remark"),
        // },
        // {
        //   align: "center",
        //   halign: "center",
        //   label: this.$t("ui.data.btn.option"),
        //   render: ({ row }) => {
        //     return (
        //       <el-button
        //         class="minus"
        //         type="success"
        //         onClick={() => this.handleEdit(row)}
        //       >
        //         {this.$t("ui.frame.btn.update")}
        //       </el-button>
        //     );
        //   },
        // },
      ];

      return columns;
    },
    searchColumns() {
      return [
        // {
        //   prop: "year",
        //   label: this.$t("ui.data.column.monthSaleOrderPlan.year"),
        //   type: "date",
        //   dateType: "year",
        //   valueFormat: "yyyy",
        //   clearable: false,
        // },
        {
          prop: "yearMonth",
          label: this.$t("ui.data.column.monthSaleOrderPlan.yearMonth"),
          type: "date",
          dateType: "month",
          valueFormat: "yyyy-MM",
          clearable: false,
          listeners: {
            change: this.handleYearMonthChange,
          },
        },
        {
          prop: "factoryCode",
          label: this.$t("common.factory"),
          type: "select",
          dictData: this.dict.type.biz_factory_name,
          clearable: false,
        },
        {
          label: this.$t("ui.data.column.monthplan.productType"),
          prop: "productTypeCode",
          type: "select",
          dictData: this.dict.type.biz_product_type,
        },
        {
          label: this.$t("ui.data.demandPlan.monthPlanVersion"),
          prop: "monthPlanVersion",
          type: "select",
          dictData: this.planVersionList,
          filterable: true,
          listeners: {
            change: this.handlePlanChange,
          },
        },
        {
          label: this.$t("ui.data.monthlyProductionPlan.productionVersion"),
          prop: "productionVersion",
          type: "select",
          dictData: this.productionVersionList,
          filterable: true
        },
      ];
    },
  },
  methods: {
    handleSelectAll(selection) {
      this.$refs.consoleRef.getTableRef().clearSelection()

    },
    handleChange(val) {
      // this.search.monthPlanVersion = val;
      // this.query.monthPlanVersion = val;
      // this.listProductionVersionList();
    },
    async requireProductionPlanVersionList() {
      try {
        const res = await requireProductionPlanVersionList(this.formatParams());

        let list = [];
        for (let index = 0; index < res.length; index++) {
          let obj = {};
          obj.label = res[index];
          obj.value = res[index];
          list.push(obj);
        }
        this.planVersionList = list;
      } catch (error) {
        console.log(error);
      }
    },
    async listProductionVersionList() {
      try {
        const res = await listProductionVersionList(this.formatParams());

        let list = [];
        for (let index = 0; index < res.length; index++) {
          let obj = {};
          obj.label = res[index];
          obj.value = res[index];
          list.push(obj);
        }
        this.productionVersionList = list;
      } catch (error) {
        console.log(error);
      }
    },
    handleYearMonthChange(val) {
      console.log(val);
      this.query.yearMonth = val;
      this.search.yearMonth = val;
      this.$set(this.search, "monthPlanVersion", "");
      this.$set(this.query, "monthPlanVersion", "");
      this.$set(this.search, "productionVersion", "");
      this.$set(this.query, "productionVersion", "");
      this.requireProductionPlanVersionList();
    },
    handlePlanChange(val) {
      console.log("查询");
      this.$set(this.search, "productionVersion", "");
      this.$set(this.query, "productionVersion", "");
      this.query.monthPlanVersion = val;
      this.search.monthPlanVersion = val;

      this.listProductionVersionList();
    },
    handleAdd() {
      if (this.$refs.noVersionListRef) {
        const params = {
          ...this.query,
          ...this.sort,
        };
        this.$refs.noVersionListRef.show(params);
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
        deleteMonthPlanRequire({
          factoryCode: row.factoryCode,
          year: row.year,
          month: row.month,
          monthPlanVersion: row.monthPlanVersion,
        }).then((data) => {
          this.$modal.msgSuccess(data.msg);
          this.$set(this.page, "current", 1);
          this.getList();
        });
      });
    },
    handleDeleteChild(row) {
      this.$confirm(this.$t("common.confirm.delete"), {
        type: "warning",
      }).then(() => {
        deleteMonthPlanProductionVersion({
          factoryCode: row.factoryCode,
          year: row.year,
          month: row.month,
          monthPlanVersion: row.monthPlanVersion,
          productionVersion: row.productionVersion,
        }).then((data) => {
          this.$modal.msgSuccess(data.msg);
          this.$set(this.page, "current", 1);
          this.getList();
        });
      });
    },
    handleChangeStatus(status, row) {
      console.log(status);
      let label =
        status === "0"
          ? this.$t("ui.biz.alter.isOpen")
          : this.$t("ui.biz.alter.isStop");

      this.$confirm(label, {
        type: "warning",
      }).then(async () => {
        try {
          this.loading = true;
          const res = await editConsole({
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

    handleExport() {
      downloadLink(
        "/demand/monthSaleOrderPlan/export",
        this.formatParams(false)
      );
    },

    handleSelectionChange(rows) {
      console.log(rows)
      this.selection = rows;
    },
    handleFinalized() {
      if (this.$refs.finRef) {
        this.$refs.finRef.show(this.selection[0]);
      }
    },
    handleGenerate(row) {

      {/* if (this.$refs.checkRef) {
        this.$refs.checkRef.show();
      }
    return */}
      this.$confirm("确定生成？").then(() => {
        this.loading = true;
        factoryWholeCourseProduction({
          factoryCode: row.factoryCode,
          year: row.year,
          month: row.month,
          monthPlanVersion: row.monthPlanVersion,
        })
          .then((res) => {
            this.$modal.msgSuccess(res.msg);
            this.getList();
          })
          .catch(() => {
            this.loading = false;
          });
      });
    },
    handleInit(row) {
      this.$confirm("确定初始化？").then(() => {
        this.loading = true;
        initFactoryProduction({
          factoryCode: row.factoryCode,
          year: row.year,
          month: row.month,
          monthPlanVersion: row.monthPlanVersion,
          productionVersion: row.productionVersion,
        })
          .then((res) => {
            this.$modal.msgSuccess(res.msg);
            this.getList();
          })
          .catch(() => {
            this.loading = false;
          });
      });
    },
    handleInitDetail(row) {
      this.$router.push({
        path: `./console/productionMonthPlanInit/${row.initVersion}`,
        query: {
          year: row.year,
          month: row.month,
          factoryCode: row.factoryCode,
          monthPlanVersion: row.monthPlanVersion,
          productionVersion: row.productionVersion,
        },
      });
    },
    handleMould(row) {
      this.$confirm("确定排模具？").then(() => {
        this.loading = true;
        factoryMouldingProduction({
          factoryCode: row.factoryCode,
          year: row.year,
          month: row.month,
          monthPlanVersion: row.monthPlanVersion,
          productionVersion: row.productionVersion,
        })
          .then((res) => {
            this.$modal.msgSuccess(res.msg);
            this.getList();
          })
          .catch(() => {
            this.loading = false;
          });
      });
    },
    handleRouterMonthPlanVersion(row) {
      this.$router.push({
        name: `DemandPlan`,
        query: {
          yearMonth: `${row.year}-${row.month}`,
          factoryCode: row.factoryCode,
          monthPlanVersion: row.monthPlanVersion,
        },
      });
    },
    handleRouterProductionVersions(row) {
      let query = {
        yearMonth: `${row.year}-${row.month}`,
        factoryCode: row.factoryCode,
        monthPlanVersion: row.monthPlanVersion,
        productionVersion: row.productionVersion,
      };
      if (row.productionStartDate) {
        query.productionStartDate = row.productionStartDate;
      }

      this.$router.push({
        path: `./mouldingDayResult/` + row.productionVersion,
        query,
      });
    },
    handleRouterMonthPlanNoProductionPlan(row) {
      this.$router.push({
        path: `./monthPlanNoProductionPlan/` + row.productionVersion,
        // query: {
        //   yearMonth: `${row.year}-${row.month}`,
        //   factoryCode: row.factoryCode,
        //   monthPlanVersion: row.monthPlanVersion,
        // },
      });
    },
    handleReport(row) {
      this.$router.push({
        path: `./report/` + row.productionVersion,
        // query: {
        //   yearMonth: `${row.year}-${row.month}`,
        //   factoryCode: row.factoryCode,
        //   monthPlanVersion: row.monthPlanVersion,
        // },
      });
    },

    // utils
    updateTableHeaderlabel() {
      //  TODO 更新表头标题
    },
    spanMethod({ row, column, rowIndex, columnIndex }) {
      if (
        (columnIndex === 0 ||
          columnIndex === 1 ||
          columnIndex === 2 ||
          columnIndex === 3) &&
        this.map[row.monthPlanVersion]
      ) {
        let arr = this.map[row.monthPlanVersion];
        let length = arr.length;
        if (row.productionVersion === arr[0].productionVersion) {
          return {
            rowspan: length,
            colspan: 1,
          };
        } else {
          return {
            rowspan: 0,
            colspan: 0,
          };
        }
      }
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
        let arr = params.yearMonth.split("-");
        params.year = arr[0];
        params.month = arr[1];
        params.yearMonth = undefined;
      }

      if (params.createTime && params.createTime[0]) {
        params.createTimeStart = params.createTime[0];
        params.createTimeEnd = params.createTime[1];
        params.createTime = undefined;
      }

      return params;
    },
    // api
    async getList() {
      try {
        this.loading = true;
        this.map = {};
        const data = await listConsole(this.formatParams());
        // this.data = data.rows;
        this.originTableData = data.rows;

        const list = [];
        data.rows.forEach((row) => {
          if (row.productVersionList.length) {
            this.map[row.monthPlanVersion] = row.productVersionList;
            row.productVersionList.forEach((chidRow) => {
              let obj = {
                ...row,
                ...chidRow,
              };
              delete obj.productVersionList;
              list.push(obj);
            });
          } else {
            let obj = { ...row };
            delete obj.productVersionList;
            list.push(obj);
          }
        });
        console.log(list);
        // this.data = [
        //   {
        //   'year':2025,
        //   'month':'11',
        //   'factoryCode':'116',
        //   'productTypeCode':'TBR',
        //   'monthPlanVersion':'20251107162223',
        //   'initVersion':'20251107162244',
        //   'productionVersion':'20251107162311',
        //   'createTime':'2025-11-07 16:22:23',
        //   'isFinal':'1',
        //   'isNaturalMonth':null,
        //   initVersion:"20251107162244",
        //   productionStartDate:'20251107162244',
        //   productionVersion:'20251107162311'
        // }
        // ];
        this.data = list;

        this.page.total = data.total;
      } catch (error) {
        console.error(error);
      } finally {
        this.loading = false;
      }
    },
  },
  created() {
    const date = moment();
    let defaultParams = {
      yearMonth: date.format("yyyy-MM"),
      factoryCode: "116",
    };
    this.search = {
      ...defaultParams,
    };
    this.query = {
      ...defaultParams,
    };
    this.requireProductionPlanVersionList();
    this.listProductionVersionList();
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
