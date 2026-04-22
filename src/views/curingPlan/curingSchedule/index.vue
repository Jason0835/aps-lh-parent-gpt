
<template>
  <basic-container>
    <page-table
      tableRef="curingScheduleTable"
      :calcHeight="true"
      v-loading="loading"
      :columns="columns"
      :searchColumns="searchColumns"
      :page="page"
      :data="data"
      :search="search"
      @refresh="getList"
      @search="handleSearch"
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
          v-hasPermi="['lh:lhScheduleResult:autoLhScheduleResult']"
          type="warning"
          @click="handleAutoPlan"
          >{{ $t("自动排产") }}</el-button
        >
        <!-- <el-button
          v-hasPermi="['lh:lhScheduleResult:autoLhScheduleResult']"
          type="warning"
          @click="handleAutoPlan"
          >{{ $t("生成模具交替计划") }}</el-button
        > -->
        <el-button
          v-hasPermi="['lh:lhScheduleResult:insertOrder']"
          type="warning"
          @click="handleAdd"
          >{{ $t("ui.data.column.scheduleResult.insertOrder") }}</el-button
        >
        <el-button
          v-hasPermi="['lh:lhScheduleResult:save']"
          type="warning"
          @click="
            () => {
              handleEdit(this.selection[0]);
            }
          "
          :disabled="selection.length != 1"
          >{{ $t("ui.frame.btn.modify") }}</el-button
        >
        <el-button
          v-hasPermi="['lh:lhScheduleResult:remove']"
          type="danger"
          @click="handleDeleteMulti"
          :disabled="selection.length == 0"
          >{{ $t("ui.frame.btn.delete") }}</el-button
        >
        <el-button
          v-hasPermi="['lh:lhScheduleResult:changeMachine']"
          type="primary"
          @click="handleChangeMachine"
          :disabled="selection.length == 0"
          >{{ $t("ui.data.column.scheduleResult.changeMachine") }}</el-button
        >
        <el-button
          v-hasPermi="['lh:lhScheduleResult:changeMachine']"
          type="primary"
          @click="handleChangePlan"
          :disabled="selection.length == 0"
          >{{ $t("调量") }}</el-button
        >

        <el-button
          type="primary"
          :disabled="selection.length === 0"
          @click="handlePublish"
          >{{ $t("排产发布") }}</el-button
        >
        <el-button
          v-hasPermi="['lh:lhScheduleResult:changeMachine']"
          type="primary"
          :disabled="selection.length == 0"
          @click="getAdjustTextNo"
          >{{ $t("文字示方调整") }}</el-button
        >
        <el-button
          v-hasPermi="['monthplan:mouldingDayResult:import']"
          @click="$refs.tltUpload.handleImport()"
          >{{ $t("ui.frame.btn.import") }}</el-button
        >
        <el-button
          @click="handleExport"
          v-hasPermi="['monthplan:mouldingDayResult:export']"
          >{{ $t("ui.frame.btn.export") }}</el-button
        >
        <!-- <el-dropdown>
          <el-button type="primary" style="margin-left: 10px">
            更多按钮<i class="el-icon-arrow-down el-icon--right"></i>
          </el-button>
          <el-dropdown-menu slot="dropdown">
            <el-dropdown-item>
              <el-button
                v-hasPermi="['lh:scheduleResult:export']"
                type="primary"
                class="more-btn"
                @click="handleExport"
                >{{ $t("ui.frame.btn.export") }}</el-button
              >
            </el-dropdown-item>
            <el-dropdown-item>
              <el-button
                v-hasPermi="['lh:scheduleResult:export']"
                type="primary"
                class="more-btn"
                @click="handleExportCombine"
                >{{ $t("ui.data.column.productStatus.exportSpec") }}</el-button
              >
            </el-dropdown-item>
            <el-dropdown-item>
              <el-button
                class="more-btn"
                v-hasPermi="['lh:scheduleResult:import']"
                type="primary"
                @click="
                  () => $refs.tltUploadForm2.handleImport(importDefaultValue)
                "
                >{{ $t("ui.frame.btn.import") }}</el-button
              >
            </el-dropdown-item>

            <el-dropdown-item>
              <el-button
                v-hasRole="['admin']"
                type="primary"
                class="more-btn"
                @click="handleChangeReleaseStatus"
              >
                {{ $t("ui.data.column.scheduleResult.changeReleaseStatus") }}
              </el-button>
            </el-dropdown-item>
          </el-dropdown-menu>
        </el-dropdown> -->
      </template>
    </page-table>
    <tlt-upload-form
      ref="tltUploadForm"
      title="导入硫化排程结果信息数据"
      downloadUrl="/lh/lhScheduleResult/importTemplate"
      uploadUrl="/lh/lhScheduleResult/exportCombine"
      @uploadSuccess="getList"
      :columns="importColumns"
      :rules="importRules"
    />
    <tlt-upload-form
      ref="tltUploadForm2"
      title="导入硫化排程结果信息数据"
      downloadUrl="/lh/lhScheduleResult/importTemplate"
      uploadUrl="/lh/lhScheduleResult/importData2"
      @uploadSuccess="getList"
      :columns="importColumns"
      :rules="importRules"
    />
    <tlt-upload
      ref="tltUpload"
      title="导入完成量"
      downloadUrl="/lh/lhScheduleResult/importTemplate"
      uploadUrl="/lh/lhScheduleResult/importData"
      @uploadSuccess="getList"
    />
    <AddDialog ref="addDialogRef" @success="handelSuccess" />
    <InfoDialog ref="infoDialogRef" @success="handelSuccess" />
    <AutoPlanDialog ref="autoPlanDialogRef" @success="handleAutoPlanSuccess" />
    <ChangeMachineDialog
      ref="changeMachineDialogRef"
      @success="handelSuccess"
    />
    <ChangeReleaseStatusDialog ref="changeReleaseStatusDialogRef" />
    <el-button style="display: none" ref="hidePopoverBtnRef"></el-button>
    <changePlanDialog ref="changePlanRef" @success="getList" />
  </basic-container>
</template>
<script>
import { mapState } from "vuex";
import moment from "moment";

import {
  listScheduleResult,
  changeQty,
  removeScheduleResult,
  exportScheduleResult,
  publishScheduleResult,
  exportCombine,
  getScheduleDate,
  adjustTextNo,
} from "@/api/lh/scheduleResult";
import { checkPermi } from "@/utils/permission";

import TltUploadForm from "@/views/components/tltUploadForm.vue";
import TltUpload from "@/components/tltUpload/tltUpload.vue";
import InfoDialog from "./components/infoDialog.vue";
import AddDialog from "./components/addDialog.vue";
import TPopover from "@/views/components/tPopover.vue";
import AutoPlanDialog from "./components/autoPlanDialog.vue";
import ChangeMachineDialog from "./components/changeMachineDialog.vue";
import ChangeReleaseStatusDialog from "./components/changeReleaseStatusDialog.vue";
import changePlanDialog from "./components/changePlanDialog.vue";

export default {
  name: "CuringSchedule",
  components: {
    AutoPlanDialog,
    InfoDialog,
    TltUploadForm,
    TltUpload,
    AddDialog,
    TPopover,
    ChangeMachineDialog,
    ChangeReleaseStatusDialog,
    changePlanDialog,
  },
  dicts: [
    "adjust_type",
    "IS_RELEASE",
    "biz_factory_name",
    "biz_end_type",
    "biz_construction_stage",
    "lh_schedule_type",
  ],
  provide() {
    return {
      parentDict: this.dict,
    };
  },
  data() {
    let defaultDate = moment().add(1, "days").format("YYYY-MM-DD"); //明天
    // let defaultDate = "2024-06-01";
    return {
      importDefaultValue: {
        scheduleDate: defaultDate,
      },
      importColumns: [
        {
          label: this.$t("ui.data.column.scheduleResult.scheduleDate"),
          prop: "scheduleDate",
          type: "date",
          dateType: "date",
          valueFormat: "yyyy-MM-dd",
          clearable: false,
        },
      ],
      importRules: {
        scheduleDate: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "blur",
          },
        ],
      },

      loading: false,
      data: [],
      page: {
        current: 1,
        pageSize: 20,
        total: 0,
      },
      sort: {},
      search: {
        scheduleDate: defaultDate,
      },
      query: {
        scheduleDate: defaultDate,
      },
      selection: [],
      dateList: [
        {
          shift: 1,
          shiftDate: "",
        },
        {
          shift: 2,
          shiftDate: "",
        },
        {
          shift: 3,
          shiftDate: "",
        },
        {
          shift: 4,
          shiftDate: "",
        },
        {
          shift: 5,
          shiftDate: "",
        },
        {
          shift: 6,
          shiftDate: "",
        },
        {
          shift: 7,
          shiftDate: "",
        },
        {
          shift: 8,
          shiftDate: "",
        },
      ],
    };
  },
  computed: {
    ...mapState({
      curingMachines: (state) => state.curing.machines,
    }),
    columns() {
      let columns = [
        { type: "selection", fixed: "left" },

        {
          label: this.$t("硫化机台"),
          prop: "lhMachineCode",
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
          prop: "scheduleType",
          label: this.$t("ui.data.column.scheduleResult.scheduleType"),
          minWidth: 100,
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.lh_schedule_type, value);
          },
        },

        {
          label: this.$t("合计余量"),
          prop: "mouldSurplusQty",
        },
        {
          label: this.$t("胎胚库存"),
          prop: "embryoStock",
        },
        {
          label: this.$t("硫化班产"),
          prop: "singleMouldShiftQty",
        },
        {
          label: this.$t("左右模"),
          prop: "leftRightMould",
        },
        {
          prop: "constructionStage",
          label: this.$t("示方类型"),
          formatter: (row, column, value, index) => {
            return this.selectDictLabel(this.dict.type.biz_construction_stage , value);
          },
        },
        {
          label: this.$t("类型"),
          prop: "isEnd",
          formatter: (row, column, value, index) => {
            return this.selectDictLabel(this.dict.type.biz_end_type, value);
          },
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
          label: this.$t("早班") + " " + this.dateList[0].shiftDate,
          children: [
            // {
            //   prop: "class1Sort",
            //   label: this.$t("顺序"),
            // },
            {
              prop: "class1PlanQty",
              label: this.$t("计划"),
            },
            {
              prop: "class1FinishQty",
              label: this.$t("实际"),
            },
            // {
            //   prop: "class2AvailableLhShift",
            //   label: this.$t("类型"),
            // },
            {
              prop: "class1Analysis",
              label: this.$t("备注"),
            },
          ],
        },
        {
          label: this.$t("中班") + " " + this.dateList[1].shiftDate,
          children: [
            // {
            //   prop: "class2Sort",
            //   label: this.$t("顺序"),
            // },
            {
              prop: "class2PlanQty",
              label: this.$t("计划"),
            },
            {
              prop: "class2FinishQty",
              label: this.$t("实际"),
            },
            // {
            //   prop: "class2AvailableLhShift",
            //   label: this.$t("类型"),
            // },
            {
              prop: "class2Analysis",
              label: this.$t("备注"),
            },

          ],
        },
        {
          label: this.$t("晚班") + " " + this.dateList[2].shiftDate,
          children: [
            // {
            //   prop: "class3Sort",
            //   label: this.$t("顺序"),
            // },
            {
              prop: "class3PlanQty",
              label: this.$t("计划"),
            },
            {
              prop: "class3FinishQty",
              label: this.$t("实际"),
            },
            // {
            //   prop: "class2AvailableLhShift",
            //   label: this.$t("类型"),
            // },
            {
              prop: "class3Analysis",
              label: this.$t("备注"),
            },

          ],
        },
        {
          label: this.$t("早班") + " " + this.dateList[3].shiftDate,
          children: [
            // {
            //   prop: "class4Sort",
            //   label: this.$t("顺序"),
            // },
            {
              prop: "class4PlanQty",
              label: this.$t("计划"),
            },
            {
              prop: "class4FinishQty",
              label: this.$t("实际"),
            },
            // {
            //   prop: "class2AvailableLhShift",
            //   label: this.$t("类型"),
            // },
            {
              prop: "class4Analysis",
              label: this.$t("备注"),
            },

          ],
        },
        {
          label: this.$t("中班") + " " + this.dateList[4].shiftDate,
          children: [
            // {
            //   prop: "class5Sort",
            //   label: this.$t("顺序"),
            // },
            {
              prop: "class5PlanQty",
              label: this.$t("计划"),
            },
            {
              prop: "class5FinishQty",
              label: this.$t("实际"),
            },
            // {
            //   prop: "class2AvailableLhShift",
            //   label: this.$t("类型"),
            // },
            {
              prop: "class5Analysis",
              label: this.$t("备注"),
            },

          ],
        },
        {
          label: this.$t("晚班") + " " + this.dateList[5].shiftDate,
          children: [
            // {
            //   prop: "class6Sort",
            //   label: this.$t("顺序"),
            // },
            {
              prop: "class6PlanQty",
              label: this.$t("计划"),
            },
            {
              prop: "class6FinishQty",
              label: this.$t("实际"),
            },
            // {
            //   prop: "class2AvailableLhShift",
            //   label: this.$t("类型"),
            // },
            {
              prop: "class6Analysis",
              label: this.$t("备注"),
            },

          ],
        },
        {
          label: this.$t("早班") + " " + this.dateList[6].shiftDate,
          children: [
            // {
            //   prop: "class7Sort",
            //   label: this.$t("顺序"),
            // },
            {
              prop: "class7PlanQty",
              label: this.$t("计划"),
            },
            {
              prop: "class7FinishQty",
              label: this.$t("实际"),
            },
            // {
            //   prop: "class2AvailableLhShift",
            //   label: this.$t("类型"),
            // },
            {
              prop: "class7Analysis",
              label: this.$t("备注"),
            },

          ],
        },
        {
          label: this.$t("中班") + " " + this.dateList[7].shiftDate,
          children: [
            // {
            //   prop: "class8Sort",
            //   label: this.$t("顺序"),
            // },
            {
              prop: "class8PlanQty",
              label: this.$t("计划"),
            },
            {
              prop: "class8FinishQty",
              label: this.$t("实际"),
            },
            // {
            //   prop: "class2AvailableLhShift",
            //   label: this.$t("类型"),
            // },
            {
              prop: "class8Analysis",
              label: this.$t("备注"),
            },

          ],
        },
        {
          prop: "remark",
          label: this.$t("备注"),
        },
        {
          prop: "updateTime",
          label: this.$t("ui.data.column.scheduleResult.updateTime"),
          minWidth: 160,
        },
        {
          prop: "rowOperator",
          label: this.$t("common.option"),
          width: 150,
          fixed: "right",
          render: ({ row }) => {
            return (
              <div>
                {checkPermi(["lh:lhScheduleResult:save"]) ? (
                  <el-button
                    type="text"
                    size="mini"
                    icon="el-icon-edit"
                    onClick={() => this.handleEdit(row)}
                  >
                    {this.$t("ui.frame.btn.modify")}
                  </el-button>
                ) : null}
                {checkPermi(["lh:lhScheduleResult:remove"]) ? (
                  <el-button
                    type="text"
                    size="mini"
                    icon="el-icon-delete"
                    onClick={() => this.handleDelete(row)}
                  >
                    {this.$t("ui.frame.btn.delete")}
                  </el-button>
                ) : null}
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
          label: this.$t("ui.data.column.scheduleResult.scheduleDate"),
          prop: "scheduleDate",
          type: "date",
          valueFormat: "yyyy-MM-dd",
        },
        {
          label: this.$t("ui.data.column.scheduleResult.isRelease"),
          prop: "isRelease",
          render: (form) => {
            return (
              <dict-select
                v-model={form.isRelease}
                options={this.dict.type.IS_RELEASE}
              />
            );
          },
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
          label: this.$t("胚胎描述"),
          prop: "mainMaterialDesc",
        },
        {
          label: this.$t("ui.data.column.cxScheduleResult.lhMachineCode"),
          prop: "lhMachineCode",
          type: "select",
          dictData: this.curingMachines,
          labelKey: "machineCode",
          valueKey: "machineCode",
          filterable: true,
        },
      ];
    },
  },
  methods: {
    async getAdjustTextNo() {
      try {
        this.loading = true;
        let row = this.selection[0];
        const data = await adjustTextNo({
          id: row.id,
          factoryCode: row.factoryCode,
          lhMachineCode: row.lhMachineCode,
        });
        this.$modal.msgSuccess(data.msg);
        this.getList();
      } catch (error) {
        console.error(error);
      } finally {
        this.loading = false;
      }
    },
    // 调量
    handleChangePlan() {
      if (this.$refs.changePlanRef) {
        let row = this.selection[0];
        this.$refs.changePlanRef.show(row);
      }
    },
    async getDate() {
      try {
        let res = await getScheduleDate({
          scheduleDate: this.query.scheduleDate,
        });
        console.log(res);
        this.dateList = res;
      } catch (error) {}
    },
    handleAutoPlan() {
      if (this.$refs.autoPlanDialogRef) {
        this.$refs.autoPlanDialogRef.show();
      }
    },
    handleAutoPlanSuccess(params) {
      // this.search.scheduleDate = params.scheduleDate;
      // this.query.scheduleDate = params.scheduleDate;
      this.getList();
    },
    handleAdd() {
      if (this.$refs.addDialogRef) {
        this.$refs.addDialogRef.show();
      }
    },
    handleEdit(row) {
      if (this.$refs.infoDialogRef) {
        this.$refs.infoDialogRef.show(row);
      }
    },
    handleChangeMachine() {
      if (this.$refs.changeMachineDialogRef) {
        let row = this.selection[0];
        this.$refs.changeMachineDialogRef.show(row);
      }
    },
    handleGotoMachineGant() {
      this.$router.push("/curingPlan/machineGantChart");
    },
    handleGotoSpecDescGant() {
      this.$router.push("/curingPlan/specDescGantChart");
    },
    handleShowChangeQty(row) {
      if (this.$refs.infoDialogRef) {
        this.$refs.infoDialogRef.show(row, true);
      }
    },
    async handleChangeQty(row) {
      try {
        this.loading = true;
        const data = await changeQty(row);
        this.$modal.msgSuccess(data.msg);
        // this.$set(this.page, "current", 1);
        this.getList();
      } catch (error) {
        console.error(error);
      } finally {
        this.loading = false;
      }
    },

    handleDelete(row) {
      this.$confirm(this.$t("common.confirm.delete"), {
        type: "warning",
      }).then(() => {
        const ids = row.id;
        removeScheduleResult({ ids }).then((data) => {
          this.$modal.msgSuccess(data.msg);
          // this.$set(this.page, "current", 1);
          this.getList();
        });
      });
    },
    handleDeleteMulti() {
      // if (this.selection.length == 0) {
      //   this.$modal.msgWarning(this.$t("请至少选择一条记录"));
      //   return;
      // }
      this.$confirm(this.$t("common.confirm.delete"), { type: "warning" })
        .then(async () => {
          //确认提交
          try {
            this.loading = true;
            let ids = [];
            this.selection.forEach((element) => {
              ids.push(element.id);
            });
            const params = {
              ids: ids.join(),
            };
            const data = await removeScheduleResult(params);
            this.$modal.msgSuccess(data.msg);
            this.$set(this.page, "current", 1);
            this.getList();
          } catch (error) {
          } finally {
            this.loading = false;
          }
        })
        .catch(() => {});
    },
    handleExport() {
      this.$confirm(this.$t(`确定导出所有硫化排程结果信息？`), {
        type: "warning",
      }).then(() => {
        try {
          this.loading = true;
          let params = this.formatParams();
          params = {
            ...params,
            // pageSize: undefined,
            // pageNum: undefined,
          };
          exportScheduleResult(params);
        } catch (error) {
          console.error(error);
        } finally {
          this.loading = false;
        }
      });
    },
    handleExportCombine() {
      let params = this.formatParams();
      params = {
        ...params,
        // pageSize: undefined,
        // pageNum: undefined,
      };
      exportCombine(params);
    },
    handlePublish() {
      this.$confirm(this.$t(`ui.biz.alter.makeSurePublish`), {
        type: "warning",
      }).then(async () => {
        try {
          this.loading = true;
          let ids = [];
          this.selection.forEach((element) => {
            ids.push(element.id);
          });
          const params = {
            scheduleDate: this.query.scheduleDate,
            ids: ids.join(),
          };
          const data = await publishScheduleResult(params);
          this.$modal.msgSuccess(data.msg);
          // this.$set(this.page, "current", 1);
          this.getList();
        } catch (error) {
          console.error(error);
        } finally {
          this.loading = false;
        }
      });
    },
    handleChangeReleaseStatus() {
      if (this.$refs.changeReleaseStatusDialogRef) {
        this.$refs.changeReleaseStatusDialogRef.show(this.query.scheduleDate);
      }
    },
    handleQuery() {},
    handleHistoryQuery() {},

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
    handleSelectionChange(rows) {
      this.selection = rows;
    },
    handleUnschedule() {
      let query = {
        scheduleDate: this.query.scheduleDate,
      };
      if (this.data[0]) {
        query.batchNo = this.data[0].batchNo;
      }

      this.$router.push({
        path: "./curingUnschedule",
        query: query,
      });
    },

    //util
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
      if (row.multipleEmbryosOfSameSapFlag > 1) {
        return { "background-color": "#BFE0F7" };
      }
      return {};
    },
    cellStyle({ row, column, rowIndex, columnIndex }) {
      if (column.property === "lhMachineCode") {
        if (row.changeMachine == 1) {
          return { background: "#ef6776" };
        }
      }
      if (column.property === "class1PlanQty") {
        if (row.changeClass1Plan == 1) {
          return { background: "#ef6776" };
        }
      }
      if (column.property === "class2PlanQty") {
        if (row.changeClass2Plan == 1) {
          return { background: "#ef6776" };
        }
      }
      if (column.property === "class3PlanQty") {
        if (row.changeClass3Plan == 1) {
          return { background: "#ef6776" };
        }
      }
      if (column.property === "class4PlanQty") {
        if (row.changeClass4Plan == 1) {
          return { background: "#ef6776" };
        }
      }
      if (column.property === "class5PlanQty") {
        if (row.changeClass5Plan == 1) {
          return { background: "#ef6776" };
        }
      }
      if (column.property === "class6PlanQty") {
        if (row.changeClass6Plan == 1) {
          return { background: "#ef6776" };
        }
      }
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

    formatParams() {
      const params = {
        pageSize: this.page.pageSize,
        pageNum: this.page.current,
        ...this.query,
        ...this.sort,
      };

      if (params.createTime && params.createTime[0]) {
        params.createTimeStart = params.createTime[0];
        params.createTimeEnd = params.createTime[1];
        params.createTime = undefined;
      }

      return params;
    },
    //
    async getList() {
      try {
        this.loading = true;
        const data = await listScheduleResult(this.formatParams());

        this.data = data.rows;
        this.page.total = data.total;
      } catch (error) {
        console.error(error);
      } finally {
        this.loading = false;
        this.getDate();
      }
    },
  },
  created() {
    //设置默认排程时间
    let date = moment().add(2, "days").format("YYYY-MM-DD");

    // date = "2023-06-01"; //test
    this.query.scheduleDate = date;
    this.search.scheduleDate = date;
    this.$store.dispatch("curing/getMachineList");
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
