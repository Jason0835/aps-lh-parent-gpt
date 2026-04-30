
<template>
  <basic-container>
    <page-table
      tableRef="steelPressAssistScheduleMainTable"
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
          v-hasPermi="['gdyy:assistSchedule']"
          type="primary"
          plain
          @click="handleExportUiExcel"
        >
          {{ $t("ui.frame.btn.export") }}
        </el-button>
      </template>
    </page-table>
  </basic-container>
</template>
<script>
//lib
import moment from "moment";
//utils
import { downloadLink } from "@/utils/request";
//interface
import { listAssistSchedule } from "@/api/gdyy/assistSchedule.js";
//components
// import TltUploadForm from "@/views/components/tltUploadForm.vue";

export default {
  name: "SteelPressAssistSchedule",
  components: {
    // TltUploadForm,
  },
  dicts: ["TASK_TYPE"],
  provide() {
    return {
      parentDict: this.dict,
    };
  },
  data() {
    return {
      searchColumns: [
        {
          label: this.$t("ui.data.column.scheduleResult.scheduleDate"),
          prop: "scheduleDate",
          type: "date",
          valueFormat: "yyyy-MM-dd",
        },
        {
          label: this.$t("ui.data.column.gdyy.scheduleResult.steelPressCode"),
          prop: "bigRollCode",
        },
        {
          label: this.$t("ui.data.column.scheduleResult.glueCode"),
          prop: "wholeGlueCode",
        },
        {
          label: this.$t("ui.data.column.scheduleResult.mouthPlateCode"),
          prop: "mouthPlateCode",
        },
        {
          label: this.$t("ui.data.column.scheduleResult.isRelease"),
          prop: "isRelease",
          type: "select",
          dictData: [], // "IS_RELEASE",
        },
        {
          label: this.$t("ui.data.column.scheduleResult.produceLine"),
          prop: "produceLine",
        },
        {
          label: this.$t("ui.data.column.scheduleResult.supplyTime"),
          prop: "hasVersion",
          type: "date",
          dateType: "daterange",
          valueFormat: "yyyy-MM-dd",
        },
      ],
      loading: false,
      data: [],
      selection: [],
      // page: {
      //   current: 1,
      //   pageSize: 20,
      //   total: 0,
      // },
      page: undefined,
      sort: {},
      search: {
        mainPlanMonth: "",
      },
      query: {
        mainPlanMonth: "",
      },
      importDefaultValue: {},
      importRules: {},
    };
  },
  computed: {
    columns() {
      let columns = [
        { type: "selection", fixed: "left" },
        {
          label: this.$t("ui.data.column.scheduleResult.baseInfo"),
          children: [
            {
              prop: "isRelease",
              valign: "middle",
              align: "center",
              halign: "center",
              label: this.$t("ui.data.column.scheduleResult.isRelease"),
              // formatter: (row, column, value, index) => {
              //   return this.selectDictLabel(isReleaseDatas, value);
              // },
            },
            {
              prop: "bigRollCode",
              valign: "middle",
              halign: "center",
              align: "center",
             //  sortable: "custom",
              label: this.$t(
                "ui.data.column.gdyy.scheduleResult.steelPressCode"
              ),
            },
            {
              prop: "wholeGlueCode",
              valign: "middle",
              halign: "center",
              align: "left",
             //  sortable: "custom",
              label: this.$t("ui.data.column.scheduleResult.glueCode"),
            },
            // {
            //     prop: 'reinforceSealGlue',
            //     valign: 'middle',
            //     halign:'center',
            //     align: 'center',
            //     sortable: "custom",
            //     label: this.$t("ui.construction.reinforceSealGlue2"),
            // },
            {
              prop: "glueSeq",
              valign: "middle",
              halign: "center",
              align: "center",
             //  sortable: "custom",
              label: this.$t("ui.data.column.scheduleResult.br.glueSeq"),
            },
            {
              prop: "mouthPlateCode",
              valign: "middle",
              halign: "center",
              align: "center",
             //  sortable: "custom",
              label: this.$t("ui.data.column.scheduleResult.mouthPlateCode"),
            },
            {
              prop: "machineId",
              valign: "middle",
              halign: "center",
              align: "left",
             //  sortable: "custom",
              label: this.$t("ui.data.column.scheduleResult.produceLine"),
              // formatter: (row, column, value, index) => {
              //   if ($.common.isEmpty(value)) {
              //     var actions = [];
              //     actions.push(
              //       '<a href="javascript:void(0)" onclick="chooseMachine(\'' +
              //         row.id +
              //         "','" +
              //         index +
              //         "')\">" +
              //         this.$t("ui.data.column.selectMachineName") +
              //         "</a> "
              //     );
              //     return actions.join("");
              //   }
              //   let machineName = selectMachineName(machineNameList, value);
              //   if (value.indexOf(",") > 0) {
              //     var actions = [];
              //     actions.push(
              //       '<a href="javascript:void(0)" onclick="chooseMachine(\'' +
              //         row.id +
              //         "','" +
              //         index +
              //         "')\">" +
              //         machineName +
              //         "</a> "
              //     );
              //     return actions.join("");
              //   }
              //   return machineName;
              // },
              // cellStyle: function (value, row, index) {
              //   if (row.changeMachine == 1) {
              //     return { css: { background: "#ef6776" } };
              //   }
              //   return {};
              // },
            },
            {
              prop: "monthPlanOs",
              valign: "middle",
              halign: "center",
              align: "right",
             //  sortable: "custom",
              label: this.$t(
                "ui.data.column.scheduleResult.br.monthPlanOs.meter"
              ),
            },
            {
              prop: "stockQty",
              valign: "middle",
              halign: "center",
              align: "right",
             //  sortable: "custom",
              label: this.$t("ui.data.column.scheduleResult.stockQty.meter2"),
            },
            {
              prop: "supplyTime",
              valign: "middle",
              halign: "center",
              align: "right",
             //  sortable: "custom",
              label: this.$t(
                "ui.data.column.scheduleResult.br.supplyTime.hour"
              ),
            },
            {
              prop: "dailyTotalQty",
              valign: "middle",
              halign: "center",
              align: "right",
             //  sortable: "custom",
              label: this.$t(
                "ui.data.column.scheduleResult.dailyTotalQty.meter"
              ),
            },
          ],
        },
        {
          label: this.$t("ui.data.column.scheduleResult.nightPlan"),
          children: [
            {
              prop: "dayPlanQty",
              valign: "middle",
              halign: "center",
              align: "right",
             //  sortable: "custom",
              label: this.$t("ui.data.column.scheduleResult.plan.meter"),
              // editable: {
              //   type: "text",
              //   label:
              //     this.$t("ui.data.column.scheduleResult.plan") +
              //     "(" +
              //     this.$t("ui.data.column.scheduleResult.unit.meter") +
              //     ")",
              //   validate: function (value) {
              //     var regu = /^[0-9]+?$/;
              //     if (!regu.test(value)) {
              //       layer.msg(
              //         this.$t(
              //           "ui.data.column.scheduleResult.msg.nonNegativeInteger"
              //         )
              //       );
              //       return this.$t(
              //         "ui.data.column.scheduleResult.msg.nonNegativeInteger"
              //       );
              //     }
              //     if (value > 9999999) {
              //       layer.msg(
              //         this.$t("ui.data.column.mdmMonthProdPlan.greatThan")
              //       );
              //       return this.$t("ui.data.column.mdmMonthProdPlan.greatThan");
              //     }
              //   },
              // },
              // cellStyle: function (value, row, index) {
              //   if (row.changeDayPlan == 1) {
              //     return { css: { background: "#ef6776" } };
              //   }
              //   if (row.dayPlanQty > 0 && row.nightPlanQty > 0) {
              //     return { css: { background: "yellow" } };
              //   }
              //   return {};
              // },
            },
            {
              prop: "dayFinishQty",
              valign: "middle",
              halign: "center",
              align: "right",
             //  sortable: "custom",
              label: this.$t("ui.data.column.scheduleResult.finish.meter"),
            },
            {
              prop: "dayProduceOrder",
              valign: "middle",
              halign: "center",
              align: "right",
             //  sortable: "custom",
              label: this.$t("ui.data.column.scheduleResult.produceOrder"),
              // editable: {
              //   type: "text",
              //   label: this.$t("ui.data.column.scheduleResult.produceOrder"),
              //   validate: function (value) {
              //     var regu = /^[0-9]+?$/;
              //     if (!regu.test(value)) {
              //       layer.msg(
              //         this.$t(
              //           "ui.data.column.scheduleResult.msg.nonNegativeInteger"
              //         )
              //       );
              //       return this.$t(
              //         "ui.data.column.scheduleResult.msg.nonNegativeInteger"
              //       );
              //     }
              //     if (value > 999999) {
              //       var str = this.$t(
              //         "ui.data.column.mdmMonthProdPlan.greatThan"
              //       );
              //       layer.msg(String(str).substring(0, str.length - 1));
              //       return String(str).substring(0, str.length - 1);
              //     }
              //   },
              // },
            },
            {
              prop: "dayFinishRate",
              valign: "middle",
              halign: "center",
              align: "right",
             //  sortable: "custom",
              label: this.$t("ui.data.column.scheduleResult.finishRate"),
              formatter: function (row, column, value, index) {
                if (value == 0 || value == null) {
                  return "0%";
                }
                var str = Number(value * 100).toFixed(2);
                return (str += "%");
              },
            },
            {
              prop: "daySysAnalysis",
              valign: "middle",
              halign: "center",
              align: "left",
             //  sortable: "custom",
              label: this.$t("ui.data.column.scheduleResult.analysis"),
              formatter: (row, column, value, index) => {
                var reasion = "";
                var HandAnaly = row.dayHandAnalysis;
                if (value != null) {
                  reasion = reasion + value;
                }
                if (HandAnaly != null) {
                  if (reasion != "") {
                    reasion = reasion + "," + HandAnaly;
                  } else {
                    reasion = HandAnaly;
                  }
                }
                return reasion;
              },
            },
          ],
        },
        {
          label: this.$t("ui.data.column.scheduleResult.dayPlan"),
          children: [
            {
              prop: "nightPlanQty",
              valign: "middle",
              halign: "center",
              align: "right",
             //  sortable: "custom",
              label: this.$t("ui.data.column.scheduleResult.plan.meter"),
              // editable: {
              //   type: "text",
              //   label:
              //     this.$t("ui.data.column.scheduleResult.plan") +
              //     "(" +
              //     this.$t("ui.data.column.scheduleResult.unit.meter") +
              //     ")",
              //   validate: function (value) {
              //     var regu = /^[0-9]+?$/;
              //     if (!regu.test(value)) {
              //       layer.msg(
              //         this.$t(
              //           "ui.data.column.scheduleResult.msg.nonNegativeInteger"
              //         )
              //       );
              //       return this.$t(
              //         "ui.data.column.scheduleResult.msg.nonNegativeInteger"
              //       );
              //     }
              //     if (value > 9999999) {
              //       layer.msg(
              //         this.$t("ui.data.column.mdmMonthProdPlan.greatThan")
              //       );
              //       return this.$t("ui.data.column.mdmMonthProdPlan.greatThan");
              //     }
              //   },
              // },
              // cellStyle: function (value, row, index) {
              //   if (row.changeNightPlan == 1) {
              //     return { css: { background: "#ef6776" } };
              //   }
              //   if (row.dayPlanQty > 0 && row.nightPlanQty > 0) {
              //     return { css: { background: "yellow" } };
              //   }
              //   return {};
              // },
            },
            {
              prop: "nightFinishQty",
              valign: "middle",
              halign: "center",
              align: "right",
             //  sortable: "custom",
              label: this.$t("ui.data.column.scheduleResult.finish.meter"),
            },
            {
              prop: "nightProduceOrder",
              valign: "middle",
              halign: "center",
              align: "right",
             //  sortable: "custom",
              label: this.$t("ui.data.column.scheduleResult.produceOrder"),
              // editable: {
              //   type: "text",
              //   label: this.$t("ui.data.column.scheduleResult.produceOrder"),
              //   validate: function (value) {
              //     var regu = /^[0-9]+?$/;
              //     if (!regu.test(value)) {
              //       layer.msg(
              //         this.$t(
              //           "ui.data.column.scheduleResult.msg.nonNegativeInteger"
              //         )
              //       );
              //       return this.$t(
              //         "ui.data.column.scheduleResult.msg.nonNegativeInteger"
              //       );
              //     }
              //     if (value > 999999) {
              //       var str = this.$t(
              //         "ui.data.column.mdmMonthProdPlan.greatThan"
              //       );
              //       layer.msg(String(str).substring(0, str.length - 1));
              //       return String(str).substring(0, str.length - 1);
              //     }
              //   },
              // },
            },
            {
              prop: "nightFinishRate",
              valign: "middle",
              halign: "center",
              align: "right",
             //  sortable: "custom",
              label: this.$t("ui.data.column.scheduleResult.finishRate"),
              formatter: function (row, column, value, index) {
                if (value == 0 || value == null) {
                  return "0%";
                }
                var str = Number(value * 100).toFixed(2);
                return (str += "%");
              },
            },
            {
              prop: "nightSysAnalysis",
              valign: "middle",
              halign: "center",
              align: "left",
             //  sortable: "custom",
              label: this.$t("ui.data.column.scheduleResult.analysis"),
              formatter: (row, column, value, index) => {
                var reasion = "";
                var HandAnaly = row.nightHandAnalysis;
                if (value != null) {
                  reasion = reasion + value;
                }
                if (HandAnaly != null) {
                  if (reasion != "") {
                    reasion = reasion + "," + HandAnaly;
                  } else {
                    reasion = HandAnaly;
                  }
                }
                return reasion;
              },
            },
          ],
        },
        {
          label: this.$t("ui.data.column.scheduleResult.prePlanQty"),
          children: [
            {
              prop: "prePlanQty",
              valign: "middle",
              halign: "center",
              align: "right",
             //  sortable: "custom",
              label: this.$t("ui.data.column.scheduleResult.plan.meter"),
              // editable: {
              //   type: "text",
              //   label:
              //     this.$t("ui.data.column.scheduleResult.plan") +

              //     "(" +
              //     this.$t("ui.data.column.scheduleResult.unit.meter") +
              //     ")",
              //   validate: function (value) {
              //     var regu = /^[0-9]+?$/;
              //     if (!regu.test(value)) {
              //       layer.msg(
              //         this.$t(
              //           "ui.data.column.scheduleResult.msg.nonNegativeInteger"
              //         )
              //       );
              //       return this.$t(
              //         "ui.data.column.scheduleResult.msg.nonNegativeInteger"
              //       );
              //     }
              //     if (value > 9999999) {
              //       layer.msg(
              //         this.$t("ui.data.column.mdmMonthProdPlan.greatThan")
              //       );
              //       return this.$t("ui.data.column.mdmMonthProdPlan.greatThan");
              //     }
              //   },
              // },
            },
          ],
        },

        {
          label: this.$t("ui.data.column.scheduleResult.tcPlan2"),
          children: [
            {
              prop: "cxClass1Plan",
              valign: "middle",
              halign: "center",
              align: "right",
             //  sortable: "custom",
              label: this.$t(
                "ui.data.column.scheduleResult.br.class1Plan.meter"
              ),
            },
            {
              prop: "cxClass2Plan",
              valign: "middle",
              halign: "center",
              align: "right",
             //  sortable: "custom",
              label: this.$t(
                "ui.data.column.scheduleResult.br.class2Plan.meter"
              ),
            },
            {
              prop: "cxClass3Plan",
              valign: "middle",
              halign: "center",
              align: "right",
             //  sortable: "custom",
              label: this.$t(
                "ui.data.column.scheduleResult.br.class3Plan.meter"
              ),
            },
            {
              prop: "cxClass4Plan",
              valign: "middle",
              halign: "center",
              align: "right",
             //  sortable: "custom",
              label: this.$t(
                "ui.data.column.scheduleResult.br.cxClass4Plan.meter"
              ),
            },
            // {
            //   prop: "cxClass5Plan",
            //   valign: "middle",
            //   halign: "center",
            //   align: "right",
            //   sortable: "custom",
            //   label: this.$t(
            //     "ui.data.column.scheduleResult.br.cxClass5Plan.meter"
            //   ),
            // },
            ,
          ],
        },
        {
          // label: this.$t("ui.biz.user.other.info"),
          label: this.$t("其他信息"),
          children: [
            {
              prop: "remark",
              valign: "middle",
              halign: "center",
              align: "center",
              minWidth: 100, 
              //  sortable: "custom",
              label: this.$t("ui.common.column.remark"),
              // formatter: (row, column, value, index) => {
              //   return $.table.tooltip(value);
              // },
            },
          ],
        },
      ];

      return columns;
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
    handleDelete(row) {
      this.$confirm(this.$t("common.confirm.delete"), {
        type: "warning",
      }).then(() => {
        const ids = row.id;
        // console.log(ids);
        // removeTmScheduleResult({ ids }).then((data) => {
        //   this.$modal.msgSuccess(data.msg);
        //   // this.$set(this.page, "current", 1);
        //   this.getList();
        // });
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
      this.$confirm(this.$t("ui.biz.alter.makeSurePublish")).then(() => {
        this.publishSchedule();
      });
    },

    async handleModifyMonthQty() {
      try {
        let row = this.selection[0];
        const valid = await hasRecordValidate(row);
        if (valid.code == 200) {
          // let params = row.embryoCode+","+row.sapCode+","+row.cxBatchNo+","+row.bomDataVersion;
          //
          // modifyQty(params).then(() => {});
        }
      } catch (error) {
        console.error(error);
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
      // this.$set(this.page, "current", current);
      // this.$set(this.page, "pageSize", pageSize);
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

    handleExportUiExcel() {
      downloadLink("/gdyy/scheduleResult/export", this.formatParams(false));
    },
    handleSelectionChange(rows) {
      this.selection = rows;
    },

    // utils

    formatParams(hasPage = true) {
      const params = {
        ...this.query,
        ...this.sort,
      };

      // if (hasPage) {
      //   params.pageSize = this.page.pageSize;
      //   params.pageNum = this.page.current;
      // }

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
        const data = await listAssistSchedule(this.formatParams());
        console.log(data);
        this.data = data.rows;
        // this.page.total = data.total;
      } catch (error) {
        console.error(error);
      } finally {
        this.loading = false;
      }
    },
  },
  created() {
    //设置默认排程时间
    let date = moment().add(1, "days").format("YYYY-MM-DD");
    // date = "2023-06-01"; //test
    this.query.scheduleDate = date;
    this.search.scheduleDate = date;
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
