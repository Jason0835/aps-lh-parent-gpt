
<template>
  <basic-container>
    <page-table
      tableRef="curingScheduleTable"
      :calcHeight="true"
      v-loading="loading"
      :columns="columns"
      :searchColumns="searchColumns"
      :data="data"
      :search="search"
      @refresh="getList"
      @search="handleSearch"
      @sort-change="handleSortChange"
      @selection-change="handleSelectionChange"
      :showSummary="false"
      :selectArea="false"
    >
      <template slot="header">
        <el-button type="warning" @click="handleAutoPlan">{{
          $t("ui.data.column.cxScheduleResult.lhAutoPlan")
        }}</el-button>
        <el-button @click="handleAdd">{{
          $t("ui.data.column.scheduleResult.insertOrder")
        }}</el-button>
        <el-button
          @click="
            () => {
              handleEdit(this.selection[0]);
            }
          "
          :disabled="selection.length != 1"
          >{{ $t("ui.frame.btn.modify") }}</el-button
        >
        <el-button
          type="danger"
          @click="handleDeleteMulti"
          :disabled="selection.length == 0"
          >{{ $t("ui.frame.btn.delete") }}</el-button
        >
        <el-button
          type="danger"
          @click="
            () => {
              handleChangeMachine(this.selection[0]);
            }
          "
          :disabled="selection.length != 1"
          >{{ $t("ui.data.column.scheduleResult.changeMachine") }}</el-button
        >
        <el-button @click="handleGotoMachineGant">{{
          $t("ui.data.column.scheduleResult.machine.gantt")
        }}</el-button>
        <el-button  @click="handleGotoSpecDescGant">{{
          $t("ui.data.column.scheduleResult.specDesc.gantt")
        }}</el-button>
        <el-button
          type="primary"
          @click="
            () => {
              handleShowChangeQty(this.selection[0]);
            }
          "
          :disabled="selection.length != 1"
          >{{ $t("ui.data.column.scheduleResult.changePlan") }}</el-button
        >
        <el-button
          type="primary"
          @click="handlePublish"
          :disabled="selection.length == 0"
          >{{ $t("ui.data.column.scheduleResult.publish") }}</el-button
        >
        <el-button @click="() => $refs.tltUploadForm.handleImport(importDefaultValue)">{{
          $t("ui.frame.btn.import")
        }}</el-button>
        <el-button @click="() => $refs.tltUploadForm.handleImport(importDefaultValue)">{{
          $t("导入2")
        }}</el-button>
        <el-button type="warning" @click="handleExport">{{
          $t("ui.frame.btn.export")
        }}</el-button>
        <el-button type="success" @click="handleChangeReleaseStatus">{{
          $t("ui.data.column.scheduleResult.changeReleaseStatus")
        }}</el-button>
      </template>
    </page-table>
    <tlt-upload-form
      ref="tltUploadForm"
      title="导入硫化排程结果信息数据"
      downloadUrl="/lh/scheduleResult/importTemplate"
      uploadUrl="/lh/scheduleResult/importScheduleData"
      @uploadSuccess="getList"
      :columns="importColumns"
      :rules="importRules"
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
  </basic-container>
</template>
<script>
import moment from "moment";

import {
  listScheduleResult,
  changeQty,
  removeScheduleResult,
  exportScheduleResult,
  publishScheduleResult,
} from "@/api/lh/scheduleResult";
import TltUploadForm from "@/views/components/tltUploadForm.vue";
import InfoDialog from "./components/infoDialog.vue";
import AddDialog from "./components/addDialog.vue";
import TPopover from "@/views/components/tPopover.vue";
import AutoPlanDialog from "./components/autoPlanDialog.vue";
import ChangeMachineDialog from "./components/changeMachineDialog.vue";
import ChangeReleaseStatusDialog from "./components/changeReleaseStatusDialog.vue";
export default {
  name: "curingSchedule",
  components: {
    AutoPlanDialog,
    InfoDialog,
    TltUploadForm,
    AddDialog,
    TPopover,
    ChangeMachineDialog,
    ChangeReleaseStatusDialog,
  },
  dicts: [
     // "sys_yes_no",
    // "MOLD_CHANGE_TYPE",
    // "IS_RELEASE"
  ],
  provide() {
    return {
      parentDict: this.dict,
    };
  },
  data() {
    // let defaultDate = moment().add(1, "days").format("YYYY-MM-DD"); //明天
    let defaultDate = "2024-06-01";
    return {
      dailyPlanVisiable: false,
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
      searchColumns: [
        {
          label: this.$t("ui.data.column.scheduleResult.scheduleDate"),
          prop: "scheduleDate",
          type: "date",
          dateType: "date",
          valueFormat: "yyyy-MM-dd",
          clearable: false,
        },
        {
          label: this.$t("ui.data.column.cxScheduleResult.lhMachineCode"),
          prop: "lhMachineCode",
        },
        {
          label: this.$t("ui.data.column.scheduleResult.sapCode"),
          prop: "sapCode",
        },
        {
          label: this.$t("ui.data.column.scheduleResult.embryoCode"),
          prop: "embryoCode",
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
      ],

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
    };
  },
  computed: {
    columns() {
      var year = this.query.scheduleDate.split("-")[0];
      var month = this.query.scheduleDate.split("-")[1];
      var day = this.query.scheduleDate.split("-")[2];
      let textDate;
      if (this.$i18n.locale === "zh") {
        textDate = month + "月" + day + "日";
      } else {
        textDate = year + "-" + month + "-" + day;
        let chinaDateArray = new Date(
          this.query.scheduleDate.replace(/-/g, "/")
        )
          .toDateString()
          .split(" ");
        textDate = `${chinaDateArray[1]} ${chinaDateArray[2]}`;
      }
      let firstTitle = this.$t("ui.data.column.scheduleResult.lh.baseInfo", [
        textDate,
        0,
        0,
        0,
        0,
      ]);
      if (this.data.length > 0) {
        var class1PlanQty = 0;
        var class2PlanQty = 0;
        var class3PlanQty = 0;
        var class4PlanQty = 0;
        var class5PlanQty = 0;
        for (let i = 0; i < this.data.length; i++) {
          class1PlanQty = class1PlanQty + Number(this.data[i]["class1PlanQty"]);
          class2PlanQty = class2PlanQty + Number(this.data[i]["class2PlanQty"]);
          class3PlanQty = class3PlanQty + Number(this.data[i]["class3PlanQty"]);
        }
        var totalPlan = class1PlanQty + class2PlanQty + class3PlanQty;
        firstTitle = this.$t("ui.data.column.scheduleResult.firstTitle", [
          textDate,
          class1PlanQty,
          class2PlanQty,
          class3PlanQty,
          totalPlan,
        ]);
      }
      let columns = [
        { type: "selection", fixed: "left" },
        { type: "index", fixed: "left" },
        // {
        //   label: this.$t("common.option"),
        //   prop: "option",
        //   width: "100px",
        //   fixed: "left",
        //   render: ({ row }) => {
        //     return (
        //       <div>
        //         <text-button
        //           onClick={() => {
        //             this.handleEdit(row);
        //           }}
        //         >
        //           {this.$t("common.button.modify")}
        //         </text-button>
        //       </div>
        //     );
        //   },
        // },
        {
          label: firstTitle,
          children: [
            {
              label: this.$t("ui.data.column.scheduleResult.isRelease"),
              prop: "isRelease",
              minWidth: 100,
              sortable: "custom",
            },
            // {
            //   label: "id",
            //   prop: "id",
            //   minWidth: 100,
            //   sortable: "custom",
            //   visible: false,
            // },
            {
              label: this.$t("ui.data.column.scheduleResult.machine"),
              prop: "lhMachineName",
              minWidth: 100,
              sortable: "custom",
            },
            {
              label: this.$t("ui.data.column.scheduleResult.leftRightMold"),
              prop: "leftRightMold",
              minWidth: 100,
              sortable: "custom",
            },
            {
              label: this.$t("ui.data.column.remark"),
              prop: "orderNo",
              minWidth: 100,
              sortable: "custom",
            },
            {
              label: this.$t("ui.data.column.scheduleResult.sapCode"),
              prop: "sapCode",
              minWidth: 100,
              sortable: "custom",
            },
            {
              label: this.$t("ui.data.column.scheduleResult.embryoCode"),
              prop: "embryoCode",
              minWidth: 100,
              sortable: "custom",
              render: ({ row }) => {
                return (
                  <TPopover
                    title={this.$t("ui.data.column.scheduleResult.embryoCode")}
                    v-model={row.embryoCode}
                    showClose={false}
                    onConfirm={(val) => {
                      this.handleChangeQty({
                        ...row,
                        embryoCode: val,
                      });
                    }}
                  >
                    <dict-Select
                      v-model={row.embryoCode}
                      options={[
                        { value: row.embryoCode, label: row.embryoCode },
                      ]}
                    />
                  </TPopover>
                );
              },
            },
            {
              label: this.$t("ui.data.column.scheduleResult.model"),
              prop: "specDesc",
              minWidth: 140,
              sortable: "custom",
            },
            {
              label: this.$t("ui.data.column.scheduleResult.lhTime"),
              prop: "lhTime",
              minWidth: 100,
              sortable: "custom",
            },
            {
              label: this.$t("ui.data.column.scheduleResult.dailyPlanQty"),
              prop: "dailyPlanQty",
              minWidth: 100,
              sortable: "custom",
            },
          ],
        },
        {
          label: this.$t("ui.data.column.scheduleResult.class1"),
          children: [
            {
              label:
                this.$t("ui.data.column.scheduleResult.class11") +
                this.$t("ui.data.column.scheduleResult.plan"),
              prop: "class1PlanQty",
              minWidth: 100,
              sortable: "custom",
              render: ({ row }) => {
                return (
                  <TPopover
                    title={this.$t("ui.data.column.scheduleResult.plan")}
                    v-model={row.class1PlanQty}
                    showClose={false}
                    min={0}
                    onConfirm={(val) => {
                      this.handleChangeQty({
                        ...row,
                        class1PlanQty: val,
                      });
                    }}
                  />
                );
              },
            },
            {
              label:
                this.$t("ui.data.column.scheduleResult.class11") +
                this.$t("ui.data.column.scheduleResult.finish"),
              prop: "class1FinishQty",
              minWidth: 100,
              sortable: "custom",
            },
            {
              label:
                this.$t("ui.data.column.scheduleResult.class11") +
                this.$t("ui.data.column.scheduleResult.analysis"),
              prop: "class1Analysis",
              minWidth: 100,
              sortable: "custom",
            },
          ],
        },
        {
          label: this.$t("ui.data.column.scheduleResult.class2"),
          children: [
            {
              label:
                this.$t("ui.data.column.scheduleResult.class22") +
                this.$t("ui.data.column.scheduleResult.plan"),
              prop: "class2PlanQty",
              minWidth: 100,
              sortable: "custom",
              render: ({ row }) => {
                return (
                  <TPopover
                    title={this.$t("ui.data.column.scheduleResult.plan")}
                    v-model={row.class2PlanQty}
                    showClose={false}
                    min={0}
                    onConfirm={(val) => {
                      this.handleChangeQty({
                        ...row,
                        class2PlanQty: val,
                      });
                    }}
                  />
                );
              },
            },
            {
              label:
                this.$t("ui.data.column.scheduleResult.class22") +
                this.$t("ui.data.column.scheduleResult.finish"),
              prop: "class2FinishQty",
              minWidth: 100,
              sortable: "custom",
            },
            {
              label:
                this.$t("ui.data.column.scheduleResult.class22") +
                this.$t("ui.data.column.scheduleResult.analysis"),
              prop: "class2Analysis",
              minWidth: 100,
              sortable: "custom",
            },
          ],
        },
        {
          label: this.$t("ui.data.column.scheduleResult.class3"),
          children: [
            {
              label:
                this.$t("ui.data.column.scheduleResult.class33") +
                this.$t("ui.data.column.scheduleResult.plan"),
              prop: "class3PlanQty",
              minWidth: 100,
              sortable: "custom",
              render: ({ row }) => {
                return (
                  <TPopover
                    title={this.$t("ui.data.column.scheduleResult.plan")}
                    v-model={row.class3PlanQty}
                    showClose={false}
                    min={0}
                    onConfirm={(val) => {
                      this.handleChangeQty({
                        ...row,
                        class3PlanQty: val,
                      });
                    }}
                  />
                );
              },
            },
            {
              label:
                this.$t("ui.data.column.scheduleResult.class33") +
                this.$t("ui.data.column.scheduleResult.finish"),
              prop: "class3FinishQty",
              minWidth: 100,
              sortable: "custom",
            },
            {
              label:
                this.$t("ui.data.column.scheduleResult.class33") +
                this.$t("ui.data.column.scheduleResult.analysis"),
              prop: "class3Analysis",
              minWidth: 100,
              sortable: "custom",
            },
          ],
        },
      ];
      return columns;
    },
  },
  methods: {
    handleDownloadUrl(form) {
      return "/cx/mdmMonthProdPlan/importTemplate/" + form.mainPlanMonth;
    },
    handleAutoPlan() {
      if (this.$refs.autoPlanDialogRef) {
        this.$refs.autoPlanDialogRef.show();
      }
    },
    handleAutoPlanSuccess(params) {
      this.search.scheduleDate = params.scheduleDate;
      this.query.scheduleDate = params.scheduleDate;
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
    handleChangeMachine(row) {
      if (this.$refs.changeMachineDialogRef) {
        this.$refs.changeMachineDialogRef.show(row);
      }
    },
    handleGotoMachineGant(){
      this.$router.push("/curingPlan/machineGantChart");
    },
    handleGotoSpecDescGant(){
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
        // this.getList();
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
            // this.$set(this.page, "current", 1);
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
    handlePublish() {
      this.$confirm(this.$t(`确认要发布排程吗？`), {
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
      // this.$set(this.page, "current", 1);
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

    //util
    handleSortChange({ column, prop, order }) {
      if (order) {
        this.sort = {
          orderBy: prop,
          isAsc: order == "ascending",
        };
      } else {
        //默认排序
        this.sort = {};
      }
      this.getList();
    },

    formatParams() {
      const params = {
        // pageSize: this.page.pageSize,
        // pageNum: this.page.current,
        ...this.query,
        params: {
          ...this.sort,
        },
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
        // const data = await this.$axios.get("monthPlan/apsMoldAdjustPlan/list");

        this.data = data.rows;
        this.page.total = data.total;
      } catch (error) {
        console.error(error);
      } finally {
        this.loading = false;
      }
    },
  },
  mounted() {},
  activated() {
    this.getList();
  },
};
</script>
