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
      @selection-change="handleSelectionChange"
      row-key="id"
      :expand-row-keys="expands"
      @expand-change="handleExpandChange"
    >
      <template slot="header">
        <el-tabs v-model="activeName" @tab-click="handleClick" type="card">
          <el-tab-pane label="结构内" name="first">
            <el-button @click="adjustOrder" :loading="getLoading">{{
              $t("获取调整订单")
            }}</el-button>
            <el-button @click="handShowResult(true)" :loading="autoLoading">{{
              $t("自动调整")
            }}</el-button>
          </el-tab-pane>
          <el-tab-pane label="结构调整" name="second">
            <el-button @click="handleAdd" :disabled="selection.length != 1">{{
              $t("单选结构调整")
            }}</el-button>
            <el-button @click="handleShowSpecial">{{
              $t("特殊材料生产情况")
            }}</el-button>
            <el-button @click="handleAddSpecial">{{
              $t("新增结构")
            }}</el-button>
          </el-tab-pane>
          <el-tab-pane label="调整结果" disabled name="three"> </el-tab-pane>
        </el-tabs>
      </template>
      <template slot="footer" v-if="activeName == 'three'">
        <div
          style="
            display: flex;
            flex-direction: row;
            align-items: center;
            justify-content: flex-end;
          "
        >
          <el-button @click="backPlan">
            {{ this.$t("common.button.cancel") }}</el-button
          >
          <el-button
            type="primary"
            @click="confirmResult"
            :loading="loading"
            :disabled="data.length == 0"
          >
            {{ this.$t("common.button.confirm") }}</el-button
          >
        </div>
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
    <addModal ref="addModalRef" @success="getList" />
  </basic-container>
</template>
<script>
//lib
import { mapState } from "vuex";
//utils
import { downloadLink } from "@/utils/request";

import {
  listInternalStructure,
  getAdjustDetailList,
  listOutsideStructure,
  confirmAdjust,
  autoAdjust,
} from "@/api/monthplan/adjustStructure";

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
  dicts: [
    "biz_yes_no",
    "biz_factory_name",
    "biz_machine_brand",
    "biz_class_type",
  ],
  provide() {
    return {
      parentDict: this.dict,
    };
  },
  data() {
    return {
      getLoading: false,
      autoLoading: false,
      adjustType: "01",
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
      isEdit: false,
    };
  },
  computed: {
    ...mapState({
      moldingMachines: (state) => state.molding.machines,
    }),
    columns() {
      if (!this.show) {
        return [];
      }
      if (this.activeName == "first") {
        return [
          {
            prop: "structureName",
            label: this.$t("产品结构"),
          },
          {
            prop: "scheduledMachines",
            label: this.$t("排产机台"),
          },
          {
            prop: "materialCode",
            label: this.$t("NC物料编码"),
          },
          {
            prop: "materialDesc",
            label: this.$t("物料描述"),
          },
          {
            prop: "hasSpecialMaterial",
            label: this.$t("是否含材料"),
            formatter: (row, column, value) => {
              return this.selectDictLabel(this.dict.type.biz_yes_no, value);
            },
          },
          {
            prop: "previousNetQty",
            label: this.$t("调整前净需求量（上周）"),
          },
          {
            prop: "currentNetQty",
            label: this.$t("当前净需求量"),
          },
          {
            prop: "netQtyChange",
            label: this.$t("净需求变动"),
          },
          {
            prop: "monthScheduledQty",
            label: this.$t("月计划已排产量"),
          },
          {
            prop: "pendingQty",
            label: this.$t("待调整量（降序）"),
          },
          {
            prop: "confirmAdjustQty",
            label: this.$t("确认调整量"),
            render: ({ row }) => {
              return (
                <div>
                  {this.isEdit && (
                    <el-input
                      v-model={row.confirmAdjustQty}
                      placeholder="请输入内容"
                      size="mini"
                    ></el-input>
                  )}
                  {!this.isEdit && <span>{row.confirmAdjustQty}</span>}
                </div>
              );
            },
          },
          {
            prop: "adjustPriorities",
            label: this.$t("调整优先级"),
            render: ({ row }) => {
              return (
                <div>
                  {this.isEdit && (
                    <el-select v-model={row.adjustPriorities} size="mini">
                      <el-option label="1" value="1" key="1" />
                      <el-option label="2" value="2" key="2" />
                      <el-option label="3" value="3" key="3" />
                    </el-select>
                  )}
                  {!this.isEdit && <span>{row.adjustPriorities}</span>}
                </div>
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
          { type: "selection", fixed: "left" },
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
            prop: "cxMachineCode",
            label: this.$t("成型机台"),
          },
          {
            prop: "structureName",
            label: this.$t("产品结构"),
          },

          {
            prop: "beginDay",
            label: this.$t("开始日期"),
          },
          {
            prop: "endDay",
            label: this.$t("结束日期"),
          },
          // {
          //   prop: "beforePlanQty",
          //   label: this.$t("计划量"),
          // },
          // {
          //   prop: "afterPlanQty",
          //   label: this.$t("调整后计划量"),
          // },
          // {
          //   prop: "beforeEndDate",
          //   label: this.$t("调整后开始日期"),
          // },
          // {
          //   prop: "afterStartDate",
          //   label: this.$t("调整后结束日期"),
          // },
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
          prop: "factoryCode",
          label: this.$t("common.factory"),
          type: "select",
          dictData: this.dict.type.biz_factory_name,
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
          prop: "cxMachineCode",
          label: this.$t("成型机台"),
        },
        {
          prop: "structureName",
          label: this.$t("产品结构"),
        },
        {
          prop: "version",
          label: this.$t("版本"),
        },
        {
          prop: "materialCode",
          label: this.$t("NC物料编码"),
        },
        {
          prop: "materialDesc",
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
    handleYearMonthChange(val) {
      this.search = {
        ...this.search,
        yearMonth: val,
      };
      this.query = {
        ...this.search,
        yearMonth: val,
      };
    },
    backPlan() {
      if (this.adjustType == "01") {
        this.activeName = "first";
      } else {
        this.activeName = "second";
      }
    },
    //确认调整结果
    async confirmResult() {
      try {
        let res = await confirmAdjust({
          adjustResultList: JSON.stringify(this.data),
          adjustType: this.adjustType,
        });
        console.log(res);
      } catch (err) {}
    },
    //获取调整订单
    async adjustOrder() {
      try {
        this.getLoading = true;
        let params = {
          ...this.query,
          ...this.sort,
        };
        if (params.yearMonth) {
          let arr = params.yearMonth.split("-");
          params.mpYear = arr[0];
          params.mpMonth = arr[1];
          params.yearMonth = "";
        }
        // if (this.activeName == "first") {
        //   params.adjustType = "01";
        // } else {
        //   params.adjustType = "02";
        // }
        params.adjustType = this.adjustType;
        this.isEdit = true;
        let res = await getAdjustDetailList(params);
        if(res.rows){
          this.data = res.rows;
        }
        this.getLoading = false;
      } catch (err) {
        this.getLoading = false;
      }
    },
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
      this.loading = true;
      this.show = false;
      this.getList();
      if (this.activeName == "first") {
        this.adjustType = "01";
      } else {
        this.adjustType = "02";
      }

      // setTimeout(() => {
      //   // this.$refs.tableRef.onReset()
      //   this.show = true;
      //   this.loading = false;
      // }, 300);
    },
    async handShowResult() {
      this.show = false;
      this.loading = true;
      this.autoLoading = true;
      try {
        let params = {
          ...this.query,
          ...this.sort,
          adjustType: this.adjustType,
          adjustResultList: this.data,
        };
        if (params.yearMonth) {
          let arr = params.yearMonth.split("-");
          params.mpYear = arr[0];
          params.mpMonth = arr[1];
          params.yearMonth = "";
        }
        console.log(params);
        let res = await autoAdjust(params);
        if(res.rows){
          this.data = res.rows;
        }
        // this.data=res.rows
        this.show = true;
        this.loading = false;
        this.autoLoading = false;
      } catch (err) {
        this.show = true;
        this.loading = false;
        this.autoLoading = false;
      }

      // setTimeout(() => {
      //   this.show = true;
      // }, 1000);
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
    async handleAdd() {
      try {
        let params = {
          ...this.query,
          ...this.sort,
        };
        if (params.yearMonth) {
          let arr = params.yearMonth.split("-");
          params.mpYear = arr[0];
          params.mpMonth = arr[1];
          params.yearMonth = "";
        }
        params.scheduledMachines = this.selection[0].cxMachineCode;
        params.structureName = this.selection[0].structureName;

        params.adjustType = this.adjustType;
        this.isEdit = true;
        let res = await getAdjustDetailList(params);
        console.log(res);
        if (this.$refs.infoRef) {
          this.$refs.infoRef.show(res);
        }
      } catch (err) {}
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
      console.log(rows);
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

      if (params.yearMonth) {
        let arr = params.yearMonth.split("-");
        params.year = arr[0];
        params.month = arr[1];
      }

      return params;
    },
    // api
    async getList() {
      console.log(this.adjustType);
      try {
        this.isEdit = false;
        this.loading = true;
        let data;
        if (this.activeName == "first") {
          data = await listInternalStructure(this.formatParams());
        }
        if (this.activeName == "second") {
          data = await listOutsideStructure(this.formatParams());
        }
        this.data = data.rows;
        this.page.total = data.total;
        this.show = true;
      } catch (error) {
        console.error(error);
      } finally {
        this.loading = false;
        this.show = true;
      }
    },
  },
  mounted() {
    // console.log("mounted");
    // this.getList();
  },
  created() {
    // 获取当前ui.data.colume.year和月份
    const now = new Date();
    const year = now.getFullYear(); // 2024
    const month = now.getMonth() + 1; // 注意：月份从0开始，需要+1
    let defaultParams = {
      yearMonth: `${year}-${month < 10 ? "0" + month : month}`,
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
