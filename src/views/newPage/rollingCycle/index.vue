<template>
  <basic-container>
    <page-table
      tableRef="cxFixedMachineMainTable"
      key="cxFixedMachineMainTable"
      ref="tableRef"
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
      row-key="id"
      :expand-row-keys="expands"
      @expand-change="handleExpandChange"
    >
      <template slot="header">
        <el-tabs v-model="activeName" @tab-click="handleClick" type="card">
          <el-tab-pane label="结构内" name="first">
            <el-button @click="handShowResult(true)">{{
              $t("获取调整订单")
            }}</el-button>
            <el-button @click="handShowResult(true)">{{
              $t("自动调整")
            }}</el-button>
          </el-tab-pane>
          <el-tab-pane label="结构调整" name="second">
            <el-button @click="handleAdd">{{ $t("单选结构调整") }}</el-button>
            <el-button @click="handleShowSpecial">{{
              $t("特殊材料生产情况")
            }}</el-button>
            <el-button @click="handleAddSpecial">{{
              $t("新增结构")
            }}</el-button>
          </el-tab-pane>
          <el-tab-pane label="调整结果" name="three"> </el-tab-pane>
        </el-tabs>
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

    <special ref="specialRef"></special>
    <addModal ref="addModalRef" />
  </basic-container>
</template>
<script>
//lib
import { mapState } from "vuex";
//utils
import { downloadLink } from "@/utils/request";

// import {
//   listProductMoldingLimit,
//   removeProductMoldingLimit,
// } from "@/api/mdm/productMoldingLimit";

//components
import tltUpload from "@/components/tltUpload/tltUpload.vue";

import infoDialog from "./components/infoDialog.vue";
import result from "./components/result.vue";
import special from "./components/special.vue";
import addModal from "./components/addModal.vue";

export default {
  name: "MoldingClosingStageProgress",
  components: {
    tltUpload,
    infoDialog,
    result,
    special,
    addModal,
  },
  dicts: ["LINE_TYPE", "JOB_TYPE", "biz_factory_name"],
  provide() {
    return {
      parentDict: this.dict,
    };
  },
  data() {
    return {
      show: true,
      subLoading: false,
      activeName: "first",
      expands: [],
      tableData: [],

      subLoading: false,
      subTableData: [],
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
      testCloumn: [],
    };
  },
  computed: {
    ...mapState({
      moldingMachines: (state) => state.molding.machines,
    }),
    columns() {

      if(!this.show){
        return []
      }
      if (this.activeName == "first") {
       return [
          {
            prop: "productStructure",
            label: this.$t("产品结构"),
          },
          {
            prop: "schedulingMachine",
            label: this.$t("排产机台"),
          },
          {
            prop: "ncMaterialCode",
            label: this.$t("NC物料编码"),
          },
          {
            prop: "materialDescription",
            label: this.$t("物料描述"),
          },
          {
            prop: "isContainMaterials",
            label: this.$t("是否含材料"),
          },
          {
            prop: "lastWeekOriginalNetDemand",
            label: this.$t("调整前净需求量（上周）"),
          },
          {
            prop: "currentNetDemand",
            label: this.$t("当前净需求量"),
          },
          {
            prop: "netDemandChange",
            label: this.$t("净需求变动"),
          },
          {
            prop: "monthlyPlanProductionQuantity",
            label: this.$t("月计划已排产量"),
          },
          {
            prop: "pendingAdjustmentAmount",
            label: this.$t("待调整量（降序）"),
          },
          {
            prop: "confirmAdjustmentAmount",
            label: this.$t("确认调整量"),
            render: ({ row }) => {
              return (
                <el-input
                  v-model={row.确认调整量}
                  placeholder="请输入内容"
                  size="mini"
                ></el-input>
              );
            },
          },
          {
            prop: "adjustPriorities",
            label: this.$t("调整优先级"),
            render: ({ row }) => {
              return (
                <el-input
                  v-model={row.调整优先级}
                  placeholder="请输入内容"
                  size="mini"
                ></el-input>
              );
            },
          },
          {
            prop: "actualAdjustment",
            label: this.$t("实际调整"),
          },
          {
            prop: "adjustmentReason",
            label: this.$t("调整原因"),
          },
          {
            align: "center",
            label: this.$t("ui.data.btn.option"),
            fixed: "right",
            render: ({ row }) => {
              return (
                <div>
                  <el-button
                    v-hasPermi={["monthplan:ProductMoldingLimit:remove"]}
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
      }
      if (this.activeName == "second") {
        return [
          {
            prop: "expand",
            type: "expand",
            render: () => {
              return (
                <div class="expend-table" v-loading={this.subLoading}>
                  <el-table border data={this.subTableData} max-height="200px">
                    {this.subColumns.map((item) => {
                      return (
                        <el-table-column
                          prop={item.prop}
                          label={item.label}
                          minWidth={80}
                        />
                      );
                    })}
                  </el-table>
                </div>
              );
            },
          },

          {
            prop: "formingMachine",
            label: this.$t("成型机台"),
          },
          {
            prop: "productStructure",
            label: this.$t("产品结构"),
          },
          {
            prop: "planQuantity",
            label: this.$t("计划量"),
          },
          {
            prop: "startDate",
            label: this.$t("开始日期"),
          },
          {
            prop: "endDate",
            label: this.$t("结束日期"),
          },
          {
            prop: "adjustPlanQuantity",
            label: this.$t("调整后计划量"),
          },
          {
            prop: "adjustStartDate",
            label: this.$t("调整后开始日期"),
          },
          {
            prop: "adjustEndDate",
            label: this.$t("调整后结束日期"),
          },
        ];
      }
      if (this.activeName == "three") {
        return [
          {
            prop: "formingMachine",
            label: this.$t("成型机台"),
          },
          {
            prop: "productStructure",
            label: this.$t("产品结构"),
          },
          {
            prop: "planQuantity",
            label: this.$t("NC物料编码"),
          },
          {
            prop: "startDate",
            label: this.$t("物料描述"),
          },
          {
            prop: "endDate",
            label: this.$t("是否含材料"),
          },
          {
            prop: "adjustPlanQuantity",
            label: this.$t("计划量"),
          },
          {
            prop: "adjustStartDate",
            label: this.$t("开始日期"),
          },
          {
            prop: "adjustEndDate",
            label: this.$t("结束日期"),
          },
          {
            prop: "adjustEndDate",
            label: this.$t("锁定上机日期"),
          },
        ];
      }

      return columns;
    },
    searchColumns() {
      return [
        {
          prop: "yearMonth",
          label: this.$t("ui.data.column.report.proSizeSummary.yearMonth"),
          type: "date",
          dateType: "month",
          valueFormat: "yyyy-MM",
          clearable: false,
        },
        {
          prop: "成型机台",
          label: this.$t("成型机台"),
        },
        {
          prop: "产品结构",
          label: this.$t("产品结构"),
        },
        {
          prop: "版本",
          label: this.$t("版本"),
        },
        {
          prop: "NC物料编码",
          label: this.$t("NC物料编码"),
        },
        {
          prop: "物料描述",
          label: this.$t("物料描述"),
        },
      ];
    },
    subColumns() {
      return [
        {
          label: "机台号",
          prop: "deviceCode",
        },
        {
          label: "设备组分类",
          prop: "deviceGroupDetailName",
        },
        {
          label: "计划执行月",
          prop: "planMonth",
        },
        {
          label: "执行人",
          prop: "execByName",
        },
        {
          label: "实际执行月",
          prop: "execMonth",
        },
        {
          label: "完成状态",
          prop: "status",
          formatter: (row, column, cellValue) => {
            return this.selectDictLabel(this.dict.type.task_type, cellValue);
          },
        },
      ];
    },
  },
  methods: {
    handleExpandChange(row, expandedRows) {
      // console.log(row, expandedRows, this.expands);
      this.expands = [];
      //通过当前的行获取
      if (expandedRows.length > 0) {
        this.subTableData = [];
        this.expands.push(row ? row.id : []);
        // this.getSubList(row.id);
      }
    },
    handleAddSpecial() {
      if (this.$refs.addModalRef) {
        this.$refs.addModalRef.show(true);
      }
    },
    handleClick(tab, event) {
      this.loading=true
      this.show = false;
      setTimeout(() => {
        // this.$refs.tableRef.onReset()
        this.show = true;
        this.loading = false;
      }, 300);
    },
    handShowResult() {
      this.show = false;
      setTimeout(() => {
        this.show = true;
      }, 1000);
      // this.$router.push("/new/rollingCycleResult");
      // // if (this.$refs.resultRef) {
      // //   this.$refs.resultRef.show(true);
      // // }
      this.activeName = "three";
    },
    handleShowSpecial() {
      if (this.$refs.specialRef) {
        this.$refs.specialRef.show();
      }
    },
    handleAdd() {
      if (this.$refs.infoRef) {
        this.$refs.infoRef.show();
      }
    },

    handleDelete(row) {
      this.$confirm(this.$t("common.confirm.delete"), {
        type: "warning",
      }).then(() => {
        const ids = row.id;
        // removeProductMoldingLimit({ ids }).then((data) => {
        //   this.$modal.msgSuccess(data.msg);
        //   this.$set(this.page, "current", 1);
        //   this.getList();
        // });
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
      downloadLink("/mdm/productMoldingLimit/export", this.formatParams(false));
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
    // api
    async getList() {
      try {
        this.loading = true;
        const data = [
          {
            id: 1,
            productStructure: "315/80R22.5-JD758零度",
            schedulingMachine: "H1101H1102",
            ncMaterialCode: "3302000915",
            materialDescription: "315/80R22.5 156/153K 20PR JD755 BL0EJY",
            isContainMaterials: "否",
            lastWeekOriginalNetDemand: "100",
            currentNetDemand: "0",
            netDemandChange: "-100",
            monthlyPlanProductionQuantity: "100",
            pendingAdjustmentAmount: "-100",
            confirmAdjustmentAmount: "-100",
            adjustPriorities: "",
            actualAdjustment: "",
            adjustmentReason: "",
            formingMachine: "H1101",
            productStructure: "315/80R22.5-JD758零度",
            planQuantity: "100",
            startDate: "1",
            endDate: "5",
            adjustPlanQuantity: "100",
            adjustStartDate: "1",
            adjustEndDate: "10",
          },
          {
            id: 2,
            productStructure: "315/80R22.5-JD758零度",
            schedulingMachine: "H1101H1102",
            ncMaterialCode: "3302002306",
            materialDescription: "315/80R22.5 156/150J 20PR JD755 BL0EJY DL",
            isContainMaterials: "否",
            lastWeekOriginalNetDemand: "200",
            currentNetDemand: "150",
            netDemandChange: "-50",
            monthlyPlanProductionQuantity: "150",
            pendingAdjustmentAmount: "0",
            confirmAdjustmentAmount: "0",
            adjustPriorities: "",
            actualAdjustment: "",
            adjustmentReason: "",
            formingMachine: "H1101",
            productStructure: "315/80R22.5-JD759零度",
            planQuantity: "120",
            startDate: "6",
            endDate: "10",
            adjustPlanQuantity: "100",
            adjustStartDate: "1",
            adjustEndDate: "10",
          },
          {
            id: 3,
            productStructure: "315/80R22.5-JD758零度",
            schedulingMachine: "H1101H1102",
            ncMaterialCode: "3302002356",
            materialDescription: "315/80R22.5 156/150J 20PR BD290 BL0EBL DL",
            isContainMaterials: "否",
            lastWeekOriginalNetDemand: "250",
            currentNetDemand: "330",
            netDemandChange: "80",
            monthlyPlanProductionQuantity: "200",
            pendingAdjustmentAmount: "130",
            confirmAdjustmentAmount: "130",
            adjustPriorities: "1",
            actualAdjustment: "",
            adjustmentReason: "",
            formingMachine: "H1102",
            productStructure: "315/80R22.5-JD760零度",
            planQuantity: "150",
            startDate: "11",
            endDate: "15",
            adjustPlanQuantity: "100",
            adjustStartDate: "1",
            adjustEndDate: "10",
          },
        ];
        this.data = data;
        this.total = 3;
        // const data = await listProductMoldingLimit(this.formatParams());
        // this.data = data.rows;
        // this.page.total = data.total;
      } catch (error) {
        console.error(error);
      } finally {
        this.loading = false;
      }
    },
  },
  mounted() {
    console.log("mounted");
    this.getList();
  },
  created() {
    this.getList();
  },
  activated() {
    // console.log('activated')
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
