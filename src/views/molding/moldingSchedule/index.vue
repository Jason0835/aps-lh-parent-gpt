
<template>
  <basic-container>
    <page-table
      tableRef="MoldingScheduleMainTable"
      :calcHeight="true"
      v-loading="loading"
      :columns="columns"
      :searchColumns="searchColumns"
      :data="data"
      :page="page"
      :search="search"
      @refresh="getList"
      @search="handleSearch"
      @reset="handleReset"
      @pageChange="handlePageChange"
      @sort-change="handleSortChange"
      @selection-change="handleSelectionChange"
      :showSummary="false"
      :selectArea="false"
      :row-style="rowStyle"
      :cell-style="cellStyle"
    >
      <template slot="header">
        <el-button
          v-hasPermi="['cx:cxScheduleResult:autoPlan']"
          type="warning"
          @click="handleAutoPlan"
          >{{ $t("自动排产") }}</el-button
        >
        <!-- <el-button v-hasPermi="['cx:cxScheduleResult:add']" type="warning">{{
          $t("成型机操作工请假")
        }}</el-button> -->
        <el-button
          v-hasPermi="['cx:cxScheduleResult:add']"
          type="warning"
          @click="handleAdd"
          >{{ $t("插单") }}</el-button
        >
        <el-button
          v-hasPermi="['cx:cxScheduleResult:edit']"
          type="warning"
          :disabled="selection.length != 1"
          @click="() => handleEdit(selection[0])"
          >{{ $t("ui.frame.btn.modify") }}</el-button
        >
        <el-button
          v-hasPermi="['cx:cxScheduleResult:remove']"
          type="danger"
          :disabled="selection.length != 1"
          @click="handleDelete(selection)"
          >{{ $t("ui.frame.btn.delete") }}</el-button
        >
        <el-button
          v-hasPermi="['cx:cxScheduleResult:changeMachine']"
          type="primary"
          :disabled="selection.length != 1"
          @click="handleChangeMachine"
          >{{ $t("ui.data.column.scheduleResult.changeMachine") }}</el-button
        >
        <!-- <el-button type="primary" plain @click="handleGotoMachineGant">{{
          $t("ui.data.column.scheduleResult.machine.gantt")
        }}</el-button>
        <el-button type="primary" plain @click="handleGotoSpecDescGant">{{
          $t("ui.data.column.scheduleResult.specDesc.gantt")
        }}</el-button> -->
        <el-button
          v-hasPermi="['cx:cxScheduleResult:changePlan']"
          type="primary"
          :disabled="selection.length !== 1"
          @click="handleChangePlan"
          >{{ $t("ui.data.column.scheduleResult.changePlan") }}</el-button
        >
        <el-button
          v-hasPermi="['cx:cxScheduleResult:publish']"
          type="primary"
          :disabled="selection.length === 0"
          @click="handlePublish"
          >{{ $t("排产发布") }}</el-button
        >
        <el-button
          v-hasPermi="['cx:productConstruction:import']"
          @click="$refs.tltUpload.handleImport()"
          >{{ $t("ui.frame.btn.import") }}</el-button
        >
        <el-button
          @click="handleExport"
          v-hasPermi="['cx:productConstruction:export']"
          >{{ $t("ui.frame.btn.export") }}</el-button
        >
      </template>
    </page-table>
    <!-- <el-button style="display: none" ref="hidePopoverBtnRef"></el-button> -->
    <autoPlanDialog ref="autoPlanRef" @success="getList" />
    <modifyLhMachineQtyDialog ref="qtyRef" @success="getList" />
    <addDialog ref="addRef" @success="getList" />
    <editDialog ref="editRef" @success="getList" />
    <changeMachineDialog ref="changeMachineRef" @success="getList" />
    <changePlanDialog ref="changePlanRef" @success="getList" />
    <releaseStatusDialog
      ref="releaseStatusRef"
      :scheduleDate="this.query.scheduleDate"
      @success="getList"
    />
    <tlt-upload-form
      ref="tltUploadForm"
      title="导入成型排查结果数据"
      downloadUrl="/cx/cxScheduleResult/importTemplate"
      uploadUrl="/cx/cxScheduleResult/importData"
      @uploadSuccess="getList"
      :columns="[
        {
          label: '排程日期',
          prop: 'scheduleDate',
          type: 'date',
          dateType: 'date',
          valueFormat: 'yyyy-MM-dd',
        },
      ]"
      :rules="importRules"
    />
    <tlt-upload-form
      ref="tltUploadForm2"
      title="导入现场计划"
      uploadUrl="/cx/cxScheduleResult/importData2"
      @uploadSuccess="getList"
      :columns="[
        {
          label: '排程日期',
          prop: 'scheduleDate',
          type: 'date',
          dateType: 'date',
          valueFormat: 'yyyy-MM-dd',
        },
      ]"
      :rules="importRules"
    />
    <tlt-upload
      ref="tltUpload"
      downloadUrl="/cx/cxScheduleResult/importTemplate"
      uploadUrl="/cx/cxScheduleResult/importData"
      @uploadSuccess="getList"
    />
    <productStatusEditDialog ref="psEditRef" @success="handelSuccess" />
    <statusDialog ref="statusRef" @success="handelSuccess" />
    <detailDialog ref="detailRef" />
  </basic-container>
</template>
<script>
//lib
import moment from "moment";
import { mapState } from "vuex";
//utils
import { downloadLink } from "@/utils/request";
//interface
import {
  listCxScheduleResult,
  publishValidate,
  publishScheduleResult,
  modifyQty,
  manualClose,
  producingIssue,
  validateConstruction,
  hasRecordValidate,
  removeCxScheduleResult,
  parseCxScheduleResult,
} from "@/api/cx/cxScheduleResult";
import { getScheduleDate } from "@/api/lh/scheduleResult";
//components
import TltUploadForm from "@/views/components/tltUploadForm.vue";

import productStatusEditDialog from "@/views/molding/productStatus/components/editDialog";

import autoPlanDialog from "./components/autoPlanDialog";
import modifyLhMachineQtyDialog from "./components/modifyLhMachineQtyDialog.vue";
import addDialog from "./components/addDialog.vue";
import editDialog from "./components/editDialog.vue";
import changeMachineDialog from "./components/changeMachineDialog.vue";
import changePlanDialog from "./components/changePlanDialog.vue";
import releaseStatusDialog from "./components/releaseStatusDialog.vue";
import statusDialog from "./components/statusDialog.vue";
import detailDialog from "./components/detailDialog.vue";
import tltUpload from "@/components/tltUpload/tltUpload.vue";

export default {
  name: "MoldingSchedule",
  components: {
    autoPlanDialog,
    modifyLhMachineQtyDialog,
    addDialog,
    editDialog,
    changeMachineDialog,
    changePlanDialog,
    releaseStatusDialog,
    TltUploadForm,
    productStatusEditDialog,
    statusDialog,
    detailDialog,
    tltUpload
  },
  dicts: [
    "TASK_TYPE",
    "IS_RELEASE",
    "ISORNOT",
    "STORAGE_LOCATION",
    "CLASS_SHIFT",
    "PRODUCTION_STATUS",
    "biz_factory_name",
    "MACHINE_TYPE",
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
      importDefaultValue: {
        scheduleDate: moment().add(1, "days").format("YYYY-MM-DD"),
      },
      importRules: {
        scheduleDate: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "blur",
          },
        ],
      },
      dateList: [
        { shift: 1, shiftDate: "" },
        { shift: 2, shiftDate: "" },
        { shift: 3, shiftDate: "" },
        { shift: 4, shiftDate: "" },
        { shift: 5, shiftDate: "" },
        { shift: 6, shiftDate: "" },
        { shift: 7, shiftDate: "" },
        { shift: 8, shiftDate: "" },
      ],
    };
  },
  computed: {
    ...mapState({
      moldingMachines: (state) => state.molding.machines,
      curingMachines: (state) => state.curing.machines,
    }),
    columns() {
      let columns = [
        { type: "selection", fixed: "left" },
        {
          label: this.$t("成型机台"),
          prop: "cxMachineCode",
          align: "center",
        },
        {
          label: this.$t("硫化机台"),
          prop: "lhMachineCode",
          align: "center",
        },
        {
          label: this.$t("物料编码"),
          prop: "materialCode",
          minWidth: 100,
          align: "center",
        },
        {
          label: this.$t("物料描述"),
          prop: "materialDesc",
          minWidth: 350,
        },
        {
          label: this.$t("胎胚描述"),
          prop: "mainMaterialDesc",
          minWidth: 350,
        },
        {
          label: this.$t("成型余量"),
          prop: "cxRemainQty",
          align: "center",
        },
        {
          label: this.$t("硫化余量"),
          prop: "lhRemainQty",
          align: "center",
        },
        {
          label: this.$t("胎胚库存"),
          prop: "totalStock",
          align: "center",
        },
        {
          label: this.$t("硫化班产"),
          prop: "lhClassQty",
          align: "center",
        },
        // {
        //   label: this.$t("ui.data.column.scheduleResult.baseInfo"),
        //   visible: true,
        //   children: [
        //     {
        //       label: this.$t("ui.data.column.scheduleResult.isRelease"),
        //       prop: "isRelease",
        //       minWidth: 100,
        //       formatter: (row, column, value, index) => {
        //         return this.selectDictLabel(this.dict.type.IS_RELEASE, value);
        //       },
        //     },
        //     {
        //       label: this.$t("批次号"),
        //       prop: "orderNo",
        //       width: 160,
        //     },
        //     {
        //       label: this.$t("工单号"),
        //       prop: "factoryCode",
        //     },
        //     {
        //       label: this.$t("工厂"),
        //       prop: "factoryCode",
        //     },

        //     {
        //       label: this.$t("月度计划"),
        //       prop: "factoryCode",
        //     },
        //     {
        //       label: this.$t("成型产量"),
        //       prop: "factoryCode",
        //     },
        //     {
        //       label: this.$t("月计划剩余量"),
        //       prop: "factoryCode",
        //     },

        //     {
        //       label: this.$t("胎胚库存"),
        //       prop: "factoryCode",
        //     },
        //     {
        //       label: this.$t("单班硫化量"),
        //       prop: "factoryCode",
        //     },
        //   ],
        // },
        {
          label: this.$t("一班") + " " + this.dateList[0].shiftDate,
          children: [
            {
              prop: "class1PlanQty",
              label: this.$t("计划"),
              align: "center",
            },
            {
              prop: "class1FinishQty",
              label: this.$t("实际"),
              align: "center",
            },
            // {
            //   prop: "class2AvailableLhShift",
            //   label: this.$t("类型"),
            // },
            {
              prop: "class1Analysis",
              label: this.$t("原因分析"),
            },
            {
              prop: "class1RecipeType",
              label: this.$t("示方类型"),
              align: "center",
            },

          ],
        },
        {
          label: this.$t("二班") + " " + this.dateList[1].shiftDate,
          children: [
            // {
            //   prop: "class2Sort",
            //   label: this.$t("顺序"),
            // },
            {
              prop: "class2PlanQty",
              label: this.$t("计划"),
              align: "center",
            },
            {
              prop: "class2FinishQty",
              label: this.$t("实际"),
              align: "center",
            },
            // {
            //   prop: "class2AvailableLhShift",
            //   label: this.$t("类型"),
            // },
            {
              prop: "class2Analysis",
              label: this.$t("原因分析"),
              align: "center",
            },
            {
              prop: "class2RecipeType",
              label: this.$t("示方类型"),
              align: "center",
            },

          ],
        },
        {
          label: this.$t("三班") + " " + this.dateList[2].shiftDate,
          children: [
            // {
            //   prop: "class3Sort",
            //   label: this.$t("顺序"),
            // },
            {
              prop: "class3PlanQty",
              label: this.$t("计划"),
              align: "center",
            },
            {
              prop: "class3FinishQty",
              label: this.$t("实际"),
              align: "center",
            },
            // {
            //   prop: "class2AvailableLhShift",
            //   label: this.$t("类型"),
            // },
            {
              prop: "class3Analysis",
              label: this.$t("原因分析"),
              align: "center",
            },
            {
              prop: "class3RecipeType",
              label: this.$t("示方类型"),
              align: "center",
            },

          ],
        },
        {
          label: this.$t("四班") + " " + this.dateList[3].shiftDate,
          children: [
            // {
            //   prop: "class4Sort",
            //   label: this.$t("顺序"),
            // },
            {
              prop: "class4PlanQty",
              label: this.$t("计划"),
              align: "center",
            },
            {
              prop: "class4FinishQty",
              label: this.$t("实际"),
              align: "center",
            },
            // {
            //   prop: "class2AvailableLhShift",
            //   label: this.$t("类型"),
            // },
            {
              prop: "class4Analysis",
              label: this.$t("原因分析"),
              align: "center",
            },
            {
              prop: "class4RecipeType",
              label: this.$t("示方类型"),
              align: "center",
            },

          ],
        },
        {
          label: this.$t("五班") + " " + this.dateList[4].shiftDate,
          children: [
            // {
            //   prop: "class5Sort",
            //   label: this.$t("顺序"),
            // },
            {
              prop: "class5PlanQty",
              label: this.$t("计划"),
              align: "center",
            },
            {
              prop: "class5FinishQty",
              label: this.$t("实际"),
              align: "center",
            },
            // {
            //   prop: "class2AvailableLhShift",
            //   label: this.$t("类型"),
            // },
            {
              prop: "class5Analysis",
              label: this.$t("原因分析"),
              align: "center",
            },
            {
              prop: "class5RecipeType",
              label: this.$t("示方类型"),
              align: "center",
            },

          ],
        },
        {
          label: this.$t("六班") + " " + this.dateList[5].shiftDate,
          children: [
          // {
          //     prop: "class6Sort",
          //     label: this.$t("顺序"),
          //   },
            {
              prop: "class6PlanQty",
              label: this.$t("计划"),
              align: "center",
            },
            {
              prop: "class6FinishQty",
              label: this.$t("实际"),
              align: "center",
            },
            // {
            //   prop: "class2AvailableLhShift",
            //   label: this.$t("类型"),
            // },
            {
              prop: "class6Analysis",
              label: this.$t("原因分析"),
              align: "center",
            },
            {
              prop: "class6RecipeType",
              label: this.$t("示方类型"),
              align: "center",
            },

          ],
        },
        {
          label: this.$t("七班") + " " + this.dateList[6].shiftDate,
          children: [
            // {
            //   prop: "class7Sort",
            //   label: this.$t("顺序"),
            // },
            {
              prop: "class7PlanQty",
              label: this.$t("计划"),
              align: "center",
            },
            {
              prop: "class7FinishQty",
              label: this.$t("实际"),
              align: "center",
            },
            // {
            //   prop: "class2AvailableLhShift",
            //   label: this.$t("类型"),
            // },
            {
              prop: "class7Analysis",
              label: this.$t("原因分析"),
              align: "center",
            },
            {
              prop: "class7RecipeType",
              label: this.$t("示方类型"),
              align: "center",
            },

          ],
        },
        {
          label: this.$t("八班") + " " + this.dateList[7].shiftDate,
          children: [
          // {
          //     prop: "class8Sort",
          //     label: this.$t("顺序"),
          //   },
            {
              prop: "class8PlanQty",
              label: this.$t("计划"),
              align: "center",
            },
            {
              prop: "class8FinishQty",
              label: this.$t("实际"),
              align: "center",
            },
            // {
            //   prop: "class2AvailableLhShift",
            //   label: this.$t("类型"),
            // },
            {
              prop: "class8Analysis",
              label: this.$t("原因分析"),
              align: "center",
            },
            {
              prop: "class8RecipeType",
              label: this.$t("示方类型"),
              align: "center",
            },

          ],
        },

        // {
        //   label: this.$t("T+1日"),
        //   children: [
        //     {
        //       prop: "class3AvailableLhShift",
        //       label: this.$t("夜班计划量"),
        //     },
        //     {
        //       prop: "class3AvailableLhShift",
        //       label: this.$t("夜班计划顺位"),
        //     },
        //     {
        //       prop: "class3AvailableLhShift",
        //       label: this.$t("夜班完成量"),
        //     },
        //     {
        //       prop: "class3AvailableLhShift",
        //       label: this.$t("夜班原因分析"),
        //     },
        //     {
        //       prop: "class3AvailableLhShift",
        //       label: this.$t("早班计划量"),
        //     },
        //     {
        //       prop: "class3AvailableLhShift",
        //       label: this.$t("早班计划顺位"),
        //     },
        //     {
        //       prop: "class3AvailableLhShift",
        //       label: this.$t("早班完成量"),
        //     },
        //     {
        //       prop: "class3AvailableLhShift",
        //       label: this.$t("早班原因分析"),
        //     },
        //     {
        //       prop: "class3AvailableLhShift",
        //       label: this.$t("中班计划量"),
        //     },
        //     {
        //       prop: "class3AvailableLhShift",
        //       label: this.$t("中班计划顺位"),
        //     },

        //     {
        //       prop: "class3AvailableLhShift",
        //       label: this.$t("中班完成量"),
        //     },
        //     {
        //       prop: "class3AvailableLhShift",
        //       label: this.$t("中班原因分析"),
        //     },
        //   ],
        // },
        // {
        //   label: this.$t("T+2日"),
        //   children: [
        //     {
        //       prop: "class3AvailableLhShift",
        //       label: this.$t("夜班计划量"),
        //     },
        //     {
        //       prop: "class3AvailableLhShift",
        //       label: this.$t("夜班计划顺位"),
        //     },
        //     {
        //       prop: "class3AvailableLhShift",
        //       label: this.$t("夜班完成量"),
        //     },
        //     {
        //       prop: "class3AvailableLhShift",
        //       label: this.$t("夜班原因分析"),
        //     },

        //     {
        //       prop: "class3AvailableLhShift",
        //       label: this.$t("早班计划量"),
        //     },
        //     {
        //       prop: "class3AvailableLhShift",
        //       label: this.$t("早班计划顺位"),
        //     },

        //     {
        //       prop: "class3AvailableLhShift",
        //       label: this.$t("早班完成量"),
        //     },
        //     {
        //       prop: "class3AvailableLhShift",
        //       label: this.$t("早班原因分析"),
        //     },

        //     {
        //       prop: "class3AvailableLhShift",
        //       label: this.$t("中班计划量"),
        //     },
        //     {
        //       prop: "class3AvailableLhShift",
        //       label: this.$t("中班计划顺位"),
        //     },

        //     {
        //       prop: "class3AvailableLhShift",
        //       label: this.$t("中班完成量"),
        //     },
        //     {
        //       prop: "class3AvailableLhShift",
        //       label: this.$t("中班原因分析"),
        //     },
        //   ],
        // },
      ];
      columns.push({
        align: "center",
        width: 120,
        fixed: "right",
        label: this.$t("操作"),
        render: ({ row }) => {
          return (
            <el-button
              type="text"
              onClick={(event) => {
                event.stopPropagation();
                this.handleViewDetail(row);
              }}
            >
              {this.$t("查看车次")}
            </el-button>
          );
        },
      });

      return columns;
    },
    searchColumns() {
      return [
      {
          label: this.$t("ui.data.column.scheduleResult.scheduleDate"),
          prop: "scheduleDate",
          type: "date",
          valueFormat: "yyyy-MM-dd",
        },
        {
          label: this.$t("ui.data.column.scheduleResult.isRelease"),
          prop: "isRelease",
          type: "select",
          dictData: this.dict.type.IS_RELEASE, // "IS_RELEASE",
        },
        {
          label: this.$t("物料编码"),
          prop: "materialCode",
        },
        {
          label: this.$t("物料描述"),
          prop: "materialDesc",
        },
        {
          label: this.$t("胎胚描述"),
          prop: "mainMaterialDesc",
        },
        {
          label: this.$t("ui.data.column.cxScheduleResult.cxMachineCode"),
          prop: "cxMachineCode",
          type: "select",
          dictData: this.moldingMachines,
          labelKey: "cxMachineCode",
          valueKey: "cxMachineCode",
          filterable: true,
        },
      ];
    },
  },
  methods: {
    handleAdd() {
      if (this.$refs.addRef) {
        this.$refs.addRef.show();
      }
    },
    handleEdit(row) {
      if (this.$refs.editRef) {
        this.$refs.editRef.show(row);
      }
    },
    handleDelete(rows) {
      this.$confirm(this.$t("common.confirm.delete"), {
        type: "warning",
      }).then(() => {
        const ids = rows.map((row) => row.id).join(",");
        removeCxScheduleResult({ ids }).then((data) => {
          this.$modal.msgSuccess(data.msg);
          this.$set(this.page, "current", 1);
          this.getList();
        });
      });
    },
    // 转机台弹窗
    handleChangeMachine() {
      if (this.$refs.changeMachineRef) {
        let row = this.selection[0];
        this.$refs.changeMachineRef.show(row);
      }
    },

    handleGotoMachineGant() {
      this.$router.push("/curingPlan/machineGantChart");
    },
    handleGotoSpecDescGant() {
      this.$router.push("/curingPlan/specDescGantChart");
    },
    // 调量
    handleChangePlan() {
      if (this.$refs.changePlanRef) {
        let row = this.selection[0];
        this.$refs.changePlanRef.show(row);
      }
    },
    async handlePublish() {
      this.publishSchedule();
    },

    async handleModifyMonthQty() {
      try {
        this.loading = true;
        let row = this.selection[0];
        const valid = await hasRecordValidate(row);
        this.loading = false;

        this.$refs.psEditRef.show(
          {
            params:
              row.embryoCode +
              "," +
              row.sapCode +
              "," +
              row.cxBatchNo +
              "," +
              row.bomDataVersion,
          },
          "2"
        );
      } catch (error) {
        console.error(error);
        this.loading = false;
      }
    },
    handleStatusChange(row) {
      if (this.$refs.statusRef) {
        this.$refs.statusRef.show(row);
      }
    },
    handleViewDetail(row) {
      const mainId = row && (row.id || row.mainId || row.scheduleMainId);
      if (!mainId) {
        this.$modal.msgWarning(this.$t("未获取到主表id"));
        return;
      }
      if (this.$refs.detailRef) {
        this.$refs.detailRef.show(mainId);
      }
    },

    handleQuery() {},
    handleHistoryQuery() {},

    handleSearch(data) {
      // 过滤掉 null、undefined 和空字符串，但保留 0、false 等有效值
      const filteredData = {};
      Object.keys(data).forEach(key => {
        const value = data[key];
        if (value !== null && value !== undefined && value !== '') {
          filteredData[key] = value;
        }
      });
      console.log('Search params:', filteredData);
      // 完全替换query，不保留旧的条件
      this.query = filteredData;
      this.$set(this.page, "current", 1);
      this.getList();
    },
    handleReset() {
      // 重置查询条件到初始状态
      const date = moment().add(1, "days").format("YYYY-MM-DD");
      this.query = { scheduleDate: date };
      this.search = { scheduleDate: date };
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
    handleAutoPlan() {
      console.log("handleAutoPlan");
      if (this.$refs.autoPlanRef) {
        this.$refs.autoPlanRef.show("", "1");
      }
    },
    handleModifyLhMachineQty() {
      if (this.$refs.qtyRef) {
        this.$refs.qtyRef.show();
      }
    },

    handleModelChange() {
      if (this.$refs.autoPlanRef) {
        this.$refs.autoPlanRef.show("", "3");
      }
    },
    handleProductStatus() {
      this.$router.push("/moldingPlanManagement/productStatus");
    },
    handleManualClose() {
      this.$confirm(
        this.$t("ui.data.column.cxScheduleResult.manualClose")
      ).then(async () => {
        const ids = this.selection.map((row) => row.id).join(",");
        const data = await manualClose({ ids });
        this.$modal.msgSuccess(data.msg);
        this.handelSuccess();
      });
    },
    handleToFinishList() {
      this.$router.push("/moldingPlanManagement/finished");
    },
    handleExportUiExcel() {
      downloadLink("/cx/cxScheduleResult/export", this.formatParams(false));
    },
    handleExport() {
      downloadLink("/cx/cxScheduleResult/export", this.formatParams(false));
    },
    handleProducingIssue() {
      this.$confirm(this.$t("ui.biz.alter.producingIssue")).then(async () => {
        try {
          this.loading = true;
          const res = await producingIssue({
            cxMachineCode: this.query.cxMachineCode,
            embryoCode: this.query.embryoCode,
            taskType: this.query.taskType,
            scheduleDate: this.query.scheduleDate,
          });
          this.$modal.msgSuccess(res.msg);
          this.getList();
        } catch (error) {
          console.error(error);
          this.loading = false;
        }
      });
    },
    handleLastDaySupplyPlan() {
      this.$router.push("/moldingPlanManagement/lastDaySupplyPlan");
    },

    handleChangeReleaseStatus() {
      this.$refs.releaseStatusRef.show();
    },
    handleValidateConstruction() {
      this.$confirm(
        this.$t("ui.data.column.cxScheduleResult.validateConstruction")
      ).then(async () => {
        try {
          this.loading = true;
          const ids = this.selection.map((row) => row.id).join(",");
          const res = await validateConstruction({ ids });
          this.$modal.msgSuccess(res.msg);
          this.getList();
        } catch (error) {
          console.error(error);
          this.loading = false;
        }
      });
    },
    handleSelectionChange(rows) {
      this.selection = rows;
    },
    handleUnschedule() {
      this.$router.push({
        path: "./moldingUnschedule",
        query: {
          scheduleDate: this.query.scheduleDate,
        },
      });
    },
    handleExportPlan() {
      downloadLink("/cx/cxScheduleResult/export2", this.formatParams(false));
    },
    async handleParse() {
      this.$confirm(this.$t("是否解析现场计划")).then(async () => {
        try {
          this.loading = true;
          const res = await parseCxScheduleResult(this.formatParams(false));
          this.$modal.msgSuccess(res.msg);
          this.loading = false;
        } catch (error) {
          console.error(error);
          this.loading = false;
        }
      });
    },

    // utils
    updateTableHeaderTitle() {
      //  TODO 更新表头标题
    },
    rowStyle({ row }) {
      // console.log(row.markCloseOutTip);
      //标记收尾背景色
      if (row.markCloseOutTip == "0") {
        return { "background-color": "#FFFFBF" };
      }
      //插单背景色
      if (row.dataSource == "1") {
        return { "background-color": "#BFE0F7" };
      }
      return {};
    },
    cellStyle({ row, column, rowIndex, columnIndex }) {
      if (column.property === "lhMachineCode") {
        if (row.colorForLhMachine > 1) {
          return { background: "#40EE67" };
        }
      }
      if (column.property === "lhMachineQty") {
        if (row.minimumLhMachineComRatioColor) {
          return { background: row.minimumLhMachineComRatioColor };
        }
        // let usedModels = Number(row.lhMachineQty);
        // let minimumLhMachine = 0;
        // let minLhMachines =
        //   Number(row.minimumLhMachineComQty) * 2 * minimumLhMachine;
        // if (usedModels > minLhMachines) {
        //   return { background: "#40EE67" };
        // } else if (usedModels < minLhMachines) {
        //   return { background: "#FF7B7B" };
        // }
      }
      if (column.property === "cxMachineCode") {
        if (row.changeCxMachine == 1) {
          return { background: "#ef6776" };
        }
      }
      if (column.property === "sapCode") {
        if (row.colorForSapCode > 1) {
          return { background: "#40EE67" };
        }
      }
      if (column.property === "storageLocation") {
        let value = row.storageLocation;
        if ((value == "T6" || value == "T2") && row.dataSource != "1") {
          return { "font-weight": "bold" };
        } else if ((value == "T6" || value == "T2") && row.dataSource == "1") {
          return {
            "font-weight": "bold",
            "background-color": "#BFE0F7",
          };
        }
      }
      if (column.property === "specDesc") {
        if (row.colorType == "1") {
          return { background: row.colorCode };
        }
        if (row.markCloseOutTip == "0") {
          return { "background-color": "#FFFFBF" };
        }
      }
      if (column.property === "embryoCode") {
        if (row.colorForEmbryoCode > 1) {
          return { background: "#40EE67" };
        }
      }
      if (column.property === "monthPlanOs") {
        // let monthPlanSurplusTip = 0; //
        // const monthPlanOs = Number(row.monthPlanOs);
        // if (monthPlanOs <= monthPlanSurplusTip) {
        //   return { background: "#FF7B7B" };
        // }
        if (row.monthPlanSurplusTipColor) {
          return { background: row.monthPlanSurplusTipColor };
        }
      }
      if (column.property === "class1PlanQty") {
        if (row.changeClass1Plan == 1) {
          return { background: "#ef6776" };
        }
        if (row.stopClassShift <= 0 && row.scheduleStop === 1) {
          return { background: "#ff0000" };
        }
      }
      if (column.property === "class2PlanQty") {
        if (row.changeClass2Plan == 1) {
          return { background: "#ef6776" };
        }
        if (row.stopClassShift <= 0 && row.scheduleStop === 1) {
          return { background: "#ff0000" };
        }
      }
      if (column.property === "class3PlanQty") {
        if (row.changeClass3Plan == 1) {
          return { background: "#ef6776" };
        }
        if (row.stopClassShift <= 0 && row.scheduleStop === 1) {
          return { background: "#ff0000" };
        }
      }
      if (column.property === "class4PlanQty") {
        if (row.changeClass4Plan == 1) {
          return { background: "#ef6776" };
        }
        if (row.stopClassShift <= 0 && row.scheduleStop === 1) {
          return { background: "#ff0000" };
        }
      }
      if (column.property === "class5PlanQty") {
        if (row.changeClass5Plan == 1) {
          return { background: "#ef6776" };
        }
        if (row.stopClassShift <= 0 && row.scheduleStop === 1) {
          return { background: "#ff0000" };
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
        const data = await listCxScheduleResult(this.formatParams());
        console.log(data);
        this.data = data.rows;
        this.page.total = data.total;
      } catch (error) {
        console.error(error);
      } finally {
        this.$nextTick(() => {
          this.loading = false;
        });
        this.getDate();
      }
    },
    async getDate() {
      try {
        const res = await getScheduleDate({
          scheduleDate: this.query.scheduleDate,
        });
        if (res && res.length > 0) {
          this.dateList = res;
        }
      } catch (error) {
        console.error(error);
      }
    },

    async publishSchedule() {
      try {
        const params = {
          scheduleDate: this.query.scheduleDate,
          ids: this.selection.map((row) => row.id).join(","),
        };
        this.loading = true;
        const valid = await publishValidate(params);
        if (valid.msg == "0") {
          this.$confirm(
            this.$t("ui.data.column.scheduleResult.hasNullLhMachineCode")
          )
            .then(async () => {
                const res = await publishScheduleResult(params);
                this.$modal.msgSuccess(res.msg);
                this.getList();
            })
            .catch(() => {
              this.loading = false;
            });
        } else {
            const res = await publishScheduleResult(params);
            this.$modal.msgSuccess(res.msg);

            this.getList();
        }
      } catch (error) {
        console.error(error);
        this.loading = false;
      }
    },
  },
  created() {
    //设置默认排程时间
    let date =moment().add(1, "days").format("YYYY-MM-DD");

    // date = "2023-06-01"; //test
    this.query.scheduleDate = date;
    this.search.scheduleDate = date;

    this.$store.dispatch("molding/getMachineList");
    this.$store.dispatch("curing/getMachineList");
  },
  watch: {
    moldingMachines() {
      this.$forceUpdate();
    },
    curingMachines() {
      this.$forceUpdate();
    },
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
