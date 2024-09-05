
<template>
  <basic-container>
    <page-table
      tableRef="monthProductionPlanTable"
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
      :showSummary="false"
      :selectArea="false"
    >
      <template slot="header">
        <el-button @click="() => $refs.tltUploadForm.handleImport(importDefaultValue)"
          >{{$t("ui.frame.btn.import")}}</el-button
        >
        <el-button type="warning" @click="handleExport">{{$t("ui.frame.btn.export")}}</el-button>
        <el-button type="warning" @click="handleExportExpectedExcessArrears"
          >{{$t("ui.data.column.mdmMonthProdPlan.expectedExport")}}</el-button
        >
        <el-button type="warning" @click="handleExportExcessArrears"
          >{{$t("ui.data.column.mdmMonthProdPlan.overProdExport")}}</el-button
        >
        <el-button type="primary" @click="handleIssuePlan"
          >{{$t("ui.data.column.mdmMonthProdPlan.issuePlan")}}</el-button
        >
        <el-button
          type="primary"
          @click="
            () => {
              dailyPlanVisiable = !dailyPlanVisiable;
            }
          "
          >{{ dailyPlanVisiable ? "隐藏" : "显示" }}{{$t("每日数据")}}</el-button
        >
        <el-button @click="handleGotoGant">{{$t("ui.data.column.scheduleResult.monthPlan.gantt")}}</el-button>
      </template>
    </page-table>
    <tlt-upload-form
      ref="tltUploadForm"
      title="导入月度生产计划数据"
      downloadUrl="/cx/mdmMonthProdPlan/importTemplate/{mainPlanMonth}"
      :downloadUrlFormatter="handleDownloadUrl"
      uploadUrl="/cx/mdmMonthProdPlan/importData"
      @uploadSuccess="getList"
      :columns="importColumns"
      :rules="importRules"
    />
    <BomDataVersionDialog ref="bomDataVersionDialogRef" />
    <el-button style="display: none" ref="hidePopoverBtnRef"></el-button>
  </basic-container>
</template>
<script>
import moment from "moment";
import {
  listMonthProdPlan,
  issuePlan,
  exportMonthProdPlan,
  exportExpectedExcessArrears,
  exportExcessArrears,
  updateExpectedExcessArrears,
} from "@/api/cx/mdmMonthProdPlan";
import BomDataVersionDialog from "./components/bomDataVersionDialog.vue";
import TltUploadForm from "@/views/components/tltUploadForm.vue";
export default {
  name: "monthlyProductionPlan",
  components: { BomDataVersionDialog, TltUploadForm },
  dicts: [
    // "sys_yes_no"
  ],
  data() {
    return {
      dailyPlanVisiable: false,
      importDefaultValue: {
        isFinamized: 0,
      },
      importColumns: [
        {
          label: this.$t("月度"),
          prop: "mainPlanMonth",
          type: "date",
          dateType: "month",
          valueFormat: "yyyy-MM",
          clearable: false,
        },
        {
          label: this.$t("是否定稿"),
          prop: "isFinamized",
          render: (form) => {
            return (
              <dict-select
                v-model={form.isFinamized}
                options={[
                  { label: "是", value: 0 },
                  { label: "否", value: 1 },
                ]}
                disabled
              />
            );
          },
        },
      ],
      importRules: {
        mainPlanMonth: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "blur",
          },
        ],
        isFinamized: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "blur",
          },
        ],
      },
      searchColumns: [
        {
          label: this.$t("ui.data.column.mdmMonthProdPlan.materialCode"),
          prop: "materialCode",
        },
        {
          label: this.$t("ui.data.column.mdmMonthProdPlan.embryoCode"),
          prop: "embryoCode",
        },
        {
          label: this.$t("ui.data.column.mdmMonthProdPlan.hasVersion"),
          prop: "hasVersion",
          render: (form) => {
            return (
              <dict-select
                v-model={form.hasVersion}
                options={[
                  { label: "是", value: 0 },
                  { label: "否", value: 1 },
                ]}
              />
            );
          },
        },
        {
          label: this.$t("ui.data.column.mdmMonthProdPlan.mainPlanMonth"),
          prop: "mainPlanMonth",
          type: "date",
          dateType: "month",
          valueFormat: "yyyy-MM",
          clearable: false,
        },
        {
          label: this.$t("ui.data.column.productStatus.bomDataVersion"),
          prop: "bomDataVersion",
        },
        {
          label: this.$t("ui.data.column.cxScheduleResult.cxMachineCode"),
          prop: "cxMachineCode",
          render: (form) => {
            return <dict-select v-model={form.cxMachineCode} />;
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
        mainPlanMonth: "",
      },
      query: {
        mainPlanMonth: "",
      },
    };
  },
  computed: {
    columns() {
      let columns = [
        // { type: "selection", fixed: "left" },
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
          label: this.$t("ui.data.column.mdmMonthProdPlan.materialCode"),
          prop: "materialCode",
          minWidth: 100,
          sortable: "custom",
        },
        {
          label: this.$t("ui.data.column.mdmMonthProdPlan.embryoCode"),
          prop: "embryoCode",
          minWidth: 100,
          sortable: "custom",

        },
        {
          label: this.$t("ui.data.column.productStatus.bomDataVersion"),
          prop: "bomDataVersion",
          minWidth: 100,
          sortable: "custom",
          render: ({ row }) => {
            return (
              <el-link
                type="primary"
                onClick={() => {
                  if (this.$refs.bomDataVersionDialogRef) {
                    this.$refs.bomDataVersionDialogRef.show(row);
                  }
                }}
              >
                {row.bomDataVersion}
              </el-link>
            );
          },
        },
        {
          label: this.$t("ui.data.column.mdmMonthProdPlan.qualityGrade"),
          prop: "qualityGrade",
          minWidth: 100,
          sortable: "custom",
        },
        {
          label: this.$t("ui.data.column.cxScheduleResult.cxMachineCode"),
          prop: "cxMachineCode",
          minWidth: 100,
          sortable: "custom",
        },
        {
          label: this.$t("ui.data.column.stockLocationSort.stockLocation"),
          prop: "storageLocation",
          minWidth: 100,
          sortable: "custom",
        },
        {
          label: this.$t("ui.data.column.mdmMonthProdPlan.specDesc"),
          prop: "specDesc",
          minWidth: 100,
          sortable: "custom",
        },
        {
          label: this.$t("ui.data.column.mdmMonthProdPlan.specDimension"),
          prop: "specDimension",
          minWidth: 100,
          sortable: "custom",
        },
        {
          label: this.$t("ui.data.column.mdmMonthProdPlan.theoryProductionPlan"),
          prop: "theoryProductionPlan",
          minWidth: 100,
          sortable: "custom",
        },
        {
          label: this.$t("ui.data.column.mdmMonthProdPlan.balance"),
          prop: "balance",
          minWidth: 100,
          sortable: "custom",
        },
        {
          label: this.$t("ui.data.column.mdmMonthProdPlan.actualArrangement"),
          prop: "actualArrangement",
          minWidth: 100,
          sortable: "custom",
        },
        {
          label: this.$t("ui.data.column.mdmMonthProdPlan.expectedExcessArrears"),
          prop: "expectedExcessArrears",
          minWidth: 100,
          sortable: "custom",
          render: (props) => {
            const { row, $index, column } = props;
            return (
              <el-popover
                placement="bottom"
                title={this.$t("ui.data.column.mdmMonthProdPlan.expectedExcessArrears")}
                width="235"
                trigger="click"
              >
                <div style="margin: 0">
                  <el-input-number
                    style="width:100px;margin-right:10px"
                    controls={false}
                    min={-9999999}
                    max={9999999}
                    clearable
                    v-model={row.tempExpectedExcessArrears}
                  />
                  <el-button
                    type="primary"
                    size="mini"
                    icon="el-icon-check"
                    loading={this.loading}
                    onClick={async () => {
                      try {
                        this.loading =true;
                        const data = await updateExpectedExcessArrears({
                          id:row.id,
                          expectedExcessArrears:row.tempExpectedExcessArrears,
                        })
                        this.$modal.msgSuccess(data.msg);
                        row.expectedExcessArrears = row.tempExpectedExcessArrears;
                        this.$refs.hidePopoverBtnRef.$el.click();
                        this.getList();
                      } catch (error) {
                        console.error(error)
                      } finally {
                        this.loading = false;
                      }

                    }}
                  ></el-button>
                  <el-button
                    size="mini"
                    icon="el-icon-close"
                    onClick={() => this.$refs.hidePopoverBtnRef.$el.click()}
                  ></el-button>
                </div>
                <el-link
                  type="primary"
                  slot="reference"
                  onClick={() => {
                    row.tempExpectedExcessArrears = row.expectedExcessArrears;
                  }}
                >
                  {row.expectedExcessArrears}
                </el-link>
              </el-popover>
            );
          },
        },
        {
          label: this.$t("ui.data.column.mdmMonthProdPlan.actualOverProduction"),
          prop: "actualOverProduction",
          minWidth: 100,
          sortable: "custom",
        },
        {
          label: this.$t("ui.data.column.mdmMonthProdPlan.beginDate"),
          prop: "beginDate",
          minWidth: 100,
          sortable: "custom",
        },
        {
          label: this.$t("ui.data.column.mdmMonthProdPlan.endDate"),
          prop: "endDate",
          minWidth: 100,
          sortable: "custom",
        },
        {
          label: this.$t("ui.data.column.mdmMonthProdPlan.specialRequirements"),
          prop: "specialRequirements",
          minWidth: 100,
          sortable: "custom",
        },
        {
          label: this.$t("common.remark"),
          prop: "remark",
          minWidth: 100,
          sortable: "custom",
        },
      ];
      if (this.dailyPlanVisiable) {
        //显示每日数据
        const date = moment(this.query.mainPlanMonth);
        // const year = date.year();
        const month = date.month() + 1;
        const days = date.daysInMonth();

        for (let i = 0; i < days; i++) {
          columns.push({
            label: `${i + 1}号生产数量`,
            prop: `productQty${i + 1}`,
            minWidth: "160px",
            type: "number",
          });
        }
      }
      return columns;
    },
  },
  methods: {
    handleDownloadUrl(form) {
      return "/cx/mdmMonthProdPlan/importTemplate/" + form.mainPlanMonth;
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
        removeArea({ ids }).then((data) => {
          this.$modal.msgSuccess(data.msg);
          this.$set(this.page, "current", 1);
          this.getList();
        });
      });
    },
    handleExport() {
      this.$confirm(this.$t(`确定导出所有月度生产计划？`), {
        type: "warning",
      }).then(() => {
        try {
          this.loading = true;
          let params = this.formatParams();
          params = {
            ...params,
            pageSize: undefined,
            pageNum: undefined,
          };
          exportMonthProdPlan(params);
        } catch (error) {
          console.error(error);
        } finally {
          this.loading = false;
        }
      });
    },
    handleExportExpectedExcessArrears() {
      this.$confirm(this.$t(`确定导出所有月度生产计划-预计超欠产？`), {
        type: "warning",
      }).then(() => {
        try {
          this.loading = true;
          let params = this.formatParams();
          params = {
            ...params,
            pageSize: undefined,
            pageNum: undefined,
          };
          exportExpectedExcessArrears(params);
        } catch (error) {
          console.error(error);
        } finally {
          this.loading = false;
        }
      });
    },
    handleExportExcessArrears() {
      this.$confirm(this.$t(`确定导出所有月度生产计划-超欠产？`), {
        type: "warning",
      }).then(() => {
        try {
          this.loading = true;
          let params = this.formatParams();
          params = {
            ...params,
            pageSize: undefined,
            pageNum: undefined,
          };
          exportExcessArrears(params);
        } catch (error) {
          console.error(error);
        } finally {
          this.loading = false;
        }
      });
    },
    handleIssuePlan() {
      this.$confirm(
        this.$t(`确认要下发 ${this.search.mainPlanMonth} 定稿计划？`),
        {
          type: "warning",
        }
      ).then(async () => {
        try {
          this.loading = true;
          const data = await issuePlan({
            planMonth: this.search.mainPlanMonth,
          });
          this.$modal.msgSuccess(data.msg);
          this.$set(this.page, "current", 1);
          this.getList();
        } catch (error) {
          console.error(error);
        } finally {
          this.loading = false;
        }
      });
    },
    handleGotoGant(){
      this.$router.push("/monthPlanManagement/monthlyPlanGantChart");
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
        pageSize: this.page.pageSize,
        pageNum: this.page.current,
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
        const data = await listMonthProdPlan(this.formatParams());
        // const data = await this.$axios.get("monthPlan/monthProductionPlan/list");

        this.data = data.rows.map((el) => {
          return {
            ...el,
            tempExpectedExcessArrears: el.expectedExcessArrears,
          };
        });
        this.page.total = data.total;
      } catch (error) {
        console.error(error);
      } finally {
        this.loading = false;
      }
    },
  },
  mounted() {
    //设置默认月度
    // let nowDate = new Date();
    console.log(moment().format("YYYY-MM"));
    this.query.mainPlanMonth = moment().format("YYYY-MM");
    this.search.mainPlanMonth = moment().format("YYYY-MM");
    this.importDefaultValue.mainPlanMonth = moment().format("YYYY-MM");
  },
  activated() {
    this.getList();
  },
};
</script>
