
<template>
  <basic-container>
    <page-table
      tableRef="mixRubberDemandPlanMainTable"
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
          v-hasPermi="['schedule:glueCollectPlan:summaryPlan']"
          schedule:glueCollectPlan:summaryPlan
          @click="handleSummaryPlan"
          >{{ $t("schedule.glueCollectPlan.btn.summaryPlan") }}</el-button
        >
        <!-- <el-button
          type="warning"
          :disabled="selection.length != 1"
          @click="handleEdit(selection[0])"
          v-hasPermi="['schedule:glueCollectPlan:edit']"
          >{{ $t("ui.frame.btn.modify") }}</el-button
        > -->
        <el-button
          type="danger"
          plain
          :disabled="selection.length === 0"
          v-hasPermi="['schedule:glueCollectPlan:remove']"
          @click="handleDelete"
          >{{ $t("ui.frame.btn.delete") }}</el-button
        >
        <!-- <el-button
          v-hasPermi="['schedule:glueCollectPlan:import']"
          type="primary"
          :disabled="selection.length === 0"
          @click="() => $refs.tltUploadForm.handleImport(importDefaultValue)"
          >{{ $t("ui.frame.btn.import") }}</el-button
        > -->
        <el-button
          v-hasPermi="['schedule:glueCollectPlan:export']"
          plain
          @click="handleExportUiExcel"
          >{{ $t("ui.frame.btn.export") }}</el-button
        >
      </template>
    </page-table>

    <!-- <addDialog ref="addRef" @success="getList" />
    <editDialog ref="editRef" @success="getList" /> -->
    <summaryPlanDialog ref="sumRef" @success="getList" />

    <!-- <tlt-upload-form
      ref="tltUploadForm"
      title="导入排程结果数据"
      downloadUrl="/schedule/glueCollectPlan/importTemplate"
      uploadUrl="/schedule/glueCollectPlan/importData"
      @uploadSuccess="getList"
      :columns="[
        {
          label: '排程日期',
          prop: 'planDate',
        },
      ]"
      :rules="importRules"
    /> -->
  </basic-container>
</template>
<script>
//lib
import moment from "moment";
//utils
import {downloadLink} from "@/utils/request";
//interface
import {editGlueCollectPlan, listGlueCollectPlan, removeGlueCollectPlan,} from "@/api/schedule/glueCollectPlan.js";
//components
// import TltUploadForm from "@/views/components/tltUploadForm.vue";

// import addDialog from "./components/addDialog.vue";
// import editDialog from "./components/editDialog.vue";
import summaryPlanDialog from "./components/summaryPlanDialog.vue";

export default {
  name: "MixRubberDemandPlanTotal",
  components: {
    // TltUploadForm,
    // addDialog,
    // editDialog,
    summaryPlanDialog,
  },
  dicts: ["MIX_AREA", "FACTORY"],
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
      page: undefined,
      sort: {},
      search: {},
      query: {},
      importDefaultValue: {},
      importRules: {},
    };
  },
  computed: {
    columns() {
      let columns = [
        { type: "selection", fixed: "left" },
        {
          label: this.$t("ui.baseInfo"),
          children: [
            {
              prop: "planDate",
             //  sortable: "custom",
              halign: "center",
              align: "center",
              label: this.$t("schedule.glueCollectPlan.planDate"),
              minWidth: 100,
            },
            {
              prop: "factory",
              halign: "center",
              align: "center",
             //  sortable: "custom",
              label: this.$t("工厂"),
              formatter: (row, column, value, index) => {
                if (this.isEmpty(value)) {
                  return "";
                }
                return this.selectDictLabel(this.dict.type.FACTORY, value);
              },
            },
            {
              prop: "glue",
             //  sortable: "custom",
              halign: "center",
              align: "center",
              label: this.$t("schedule.glueCollectPlan.glue"),
            },
            // {
            //   prop: "mixArea",
            //   halign: "center",
            //   align: "center",
            //  //  sortable: "custom",
            //   label: this.$t("schedule.glueCollectPlan.mixArea"),
            //   formatter: (row, column, value, index) => {
            //     if (this.isEmpty(value)) {
            //       return "";
            //     }
            //     return this.selectDictLabel(this.dict.type.MIX_AREA, value);
            //   },
            //   // editable: {
            //   //   type: "text",
            //   //   label: this.$t("schedule.glueCollectPlan.mixArea"),
            //   //   emptytext: "-",
            //   //   validate: function (value) {
            //   //     // 只可输入英文校验
            //   //     var regu = /^[\x00-\xff]*$/;
            //   //     if (!regu.test(value) && $.common.isNotEmpty(value)) {
            //   //       layer.msg(this.$t("ui.message.editable.isCode"));
            //   //       return this.$t("ui.message.editable.isCode");
            //   //     }
            //   //     var valueArr = value == null ? [] : value.split(",");
            //   //     // 输入密炼区是否有重复判断
            //   //     if (isRepeat(valueArr)) {
            //   //       layer.msg(this.$t("ui.message.editable.isRepeat"));
            //   //       return this.$t("ui.message.editable.isRepeat");
            //   //     }
            //   //     // 密炼区字典校验
            //   //     for (let i = 0; i < valueArr.length; i++) {
            //   //       if (mixAreaDictLabels.indexOf(valueArr[i]) < 0) {
            //   //         layer.msg(
            //   //           valueArr[i] +
            //   //             this.$t("ui.message.editable.mixAreaNotExist")
            //   //         );
            //   //         return (
            //   //           valueArr[i] +
            //   //           this.$t("ui.message.editable.mixAreaNotExist")
            //   //         );
            //   //       }
            //   //     }
            //   //   },
            //   // },
            // },
            // {
            //   prop: "machineCode",
            //   sortable: true,
            //   halign: "center",
            //   align: "center",
            //   label: this.$t("schedule.glueCollectPlan.machineCode"),
            //   formatter: (row, column, value, index) => {
            //     return row.machineName;
            //   },
            //   // formatter: (row, column, value, index) => {
            //   //   var actions = [];
            //   //   if ($.common.isEmpty(value)) {
            //   //     actions.push(
            //   //       '<a href="javascript:void(0)" onclick="chooseMachine(\'' +
            //   //         row.id +
            //   //         "','" +
            //   //         index +
            //   //         "')\">" +
            //   //         this.$t("ui.frame.btn.choose") +
            //   //         this.$t("schedule.glueCollectPlan.machineCode") +
            //   //         "</a> "
            //   //     );
            //   //     return actions.join("");
            //   //   }
            //   //   var machineName = selectMachineName(machineNameList, row);
            //   //   actions.push(
            //   //     '<a href="javascript:void(0)" onclick="chooseMachine(\'' +
            //   //       row.id +
            //   //       "','" +
            //   //       index +
            //   //       "')\">" +
            //   //       machineName +
            //   //       "</a> "
            //   //   );
            //   //   return actions.join("");
            //   // },
            // },
            // {
            //   prop: "isFinishing",
            //   sortable: true,
            //   halign: "center",
            //   align: "center",
            //   label: this.$t("schedule.glueCollectPlan.isFinishing"),
            //   render: ({ row }) => {
            //     return (
            //       <el-switch
            //         value={row.isFinishing}
            //         active-value="1"
            //         inactive-value="0"
            //         onChange={(value) => this.handleChangeStatus(value, row)}
            //       />
            //     );
            //   },
            // },
            {
              prop: "totalPlanQty",
             //  sortable: "custom",
              halign: "center",
              align: "right",
              label: this.$t("schedule.glueCollectPlan.totalPlanQty"),
            },
            {
              prop: "lastSurplus",
              sortable: true,
              halign: "center",
              align: "right",
              label: this.$t("数据来源"),
            },
            {
              prop: "produceQty",
              sortable: true,
              halign: "center",
              align: "right",
              label: this.$t("schedule.glueCollectPlan.produceQty"),
            },
          ],
        },
        {
          label: this.$t("schedule.common.midClass"),
          children: [
            {
              prop: "midPlanQty",
             //  sortable: "custom",
              halign: "center",
              align: "right",
              label: this.$t("schedule.glueCollectPlan.midPlanQty"),
              formatter: function (row, column, value, index) {
                // 3878 【分厂胶料需求计划】页面修改中、夜、白三班计划量为0时直接放空处理
                if (value === 0) {
                  return null;
                }
                return value;
              },
            },
            {
              prop: "midRemark",
             //  sortable: "custom",
              halign: "center",
              align: "center",
              label: this.$t("schedule.glueCollectPlan.midRemark"),
            },
          ],
        },
        {
          label: this.$t("schedule.common.nightClass"),
          children: [
            {
              prop: "nightPlanQty",
             //  sortable: "custom",
              halign: "center",
              align: "right",
              label: this.$t("schedule.glueCollectPlan.nightPlanQty"),
              formatter: function (row, column, value, index) {
                // 3878 【分厂胶料需求计划】页面修改中、夜、白三班计划量为0时直接放空处理
                if (value === 0) {
                  return null;
                }
                return value;
              },
            },
            {
              prop: "nightRemark",
             //  sortable: "custom",
              halign: "center",
              align: "center",
              label: this.$t("schedule.glueCollectPlan.nightRemark"),
            },
          ],
        },
      ];

      return columns;
    },
    searchColumns() {
      return [
        {
          label: this.$t("schedule.glueCollectPlan.planDate"),
          prop: "planDate",
          type: "date",
          valueFormat: "yyyy-MM-dd",
        },
        {
          label: this.$t("schedule.glueCollectPlan.mixArea"),
          prop: "mixArea",
          type: "select", //MIX_AREA
          dictData: this.dict.type.MIX_AREA,
        },
        {
          label: this.$t("schedule.glueCollectPlan.glue"),
          prop: "glue",
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
    handleDelete(row) {
      this.$confirm(this.$t("common.confirm.delete"), {
        type: "warning",
      }).then(() => {
        const ids = this.selection.map((row) => row.id).join(",");
        // console.log(ids);
        removeGlueCollectPlan({ ids }).then((data) => {
          // this.$modal.msgSuccess(data.msg);
          // this.$set(this.page, "current", 1);
          this.getList();
        });
      });
    },
    handleChangeStatus(isFinishing, row) {
      console.log(isFinishing);
      let title =
        isFinishing === "1"
          ? this.$t("ui.biz.alter.isOpen")
          : this.$t("ui.biz.alter.isStop");

      this.$confirm(title, {
        type: "warning",
      }).then(async () => {
        try {
          this.loading = true;
          const res = await editGlueCollectPlan({
            ...row,
            isFinishing,
          });
          this.$modal.msgSuccess(res.msg);
          this.getList();
        } catch (error) {
          this.loading = false;
        }
      });
    },
    handleSummaryPlan() {
      //
      if (this.$refs.sumRef) {
        this.$refs.sumRef.show();
      }
    },

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
      downloadLink("/schedule/glueCollectPlan/export", this.formatParams(false));
    },

    handleSelectionChange(rows) {
      this.selection = rows;
    },

    // utils
    updateTableHeaderTitle() {
      //  TODO 更新表头标题
    },

    formatParams() {
      const params = {
        // pageSize: this.page.pageSize,
        // pageNum: this.page.current,
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
    // api
    async getList() {
      try {
        this.loading = true;
        const data = await listGlueCollectPlan(this.formatParams());
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
    //// date = "2023-06-01"; //test
    this.query.planDate = date;
    this.search.planDate = date;
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
