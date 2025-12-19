
<template>
  <basic-container>
    <page-table
      tableRef="mixRubberDemandPlanDecomposeMainTable"
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
          v-hasPermi="['schedule:glueDecomposePlan:add']"
          @click="handleAdd"
          >{{ $t("ui.frame.btn.add") }}</el-button
        >
        <el-button
          type="primary"
          plain
          :disabled="selection.length != 1"
          @click="() => handleEdit(selection[0])"
          v-hasPermi="['schedule:glueDecomposePlan:edit']"
          >{{ $t("ui.frame.btn.modify") }}</el-button
        >
        <el-button
          type="danger"
          plain
          :disabled="selection.length === 0"
          v-hasPermi="['schedule:glueDecomposePlan:remove']"
          @click="handleDelete"
          >{{ $t("ui.frame.btn.delete") }}</el-button
        >
        <!-- <el-button
          v-hasPermi="['schedule:glueDecomposePlan:import']"
          type="primary"
          :disabled="selection.length === 0"
          @click="() => $refs.tltUploadForm.handleImport(importDefaultValue)"
          >{{ $t("ui.frame.btn.import") }}</el-button
        > -->
        <el-button
          v-hasPermi="['schedule:glueDecomposePlan:export']"
          plain
          @click="handleExportUiExcel"
          >{{ $t("ui.frame.btn.export") }}</el-button
        >
        <el-button
          v-hasPermi="['schedule:glueDecomposePlan:decompositionPlan']"
          type="primary"
          plain
          @click="handleDecompositionPlan"
          >{{
            $t("schedule.glueDecomposePlan.btn.decompositionPlan")
          }}</el-button
        >
        <!-- <el-button
          v-hasPermi="['schedule:glueDecomposePlan:glueSpanSend']"
          type="primary"
          plain
          @click="handleSendCrossRegional"
          >{{
            $t("schedule.glueDecomposePlan.btn.sendCrossRegional")
          }}</el-button
        >
        <el-button
          v-hasPermi="['schedule:glueDecomposePlan:glueSpanReceive']"
          type="primary"
          plain
          @click="handleReceiveCrossRegional"
          >{{
            $t("schedule.glueDecomposePlan.btn.receiveCrossRegional")
          }}</el-button
        > -->
      </template>
    </page-table>

    <addDialog ref="addRef" @success="getList" />
    <editDialog ref="editRef" @success="getList" />
    <decomposeDialog ref="deRef" @success="getList" />
    <!-- <tlt-upload-form
      ref="tltUploadForm"
      title="导入排程结果数据"
      downloadUrl="/schedule/glueDecomposePlan/importTemplate"
      uploadUrl="/schedule/glueDecomposePlan/importData"
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
import { downloadLink } from "@/utils/request";
//interface
import {
  listGlueDecomposePlan,
  removeGlueDecomposePlan,
} from "@/api/schedule/glueDecomposePlan";
//components
// import TltUploadForm from "@/views/components/tltUploadForm.vue";

import addDialog from "./components/addDialog.vue";
import editDialog from "./components/editDialog.vue";
import decomposeDialog from "./components/decomposeDialog.vue";

export default {
  name: "MixRubberDemandPlanDecompose",
  components: {
    // TltUploadForm,
    addDialog,
    editDialog,
    decomposeDialog,
  },
  dicts: ["MIX_AREA", 'IS_HAVE'],
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
      // page: {
      //   current: 1,
      //   pageSize: 20,
      //   total: 0,
      // },
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
              minWidth: 100,
              label: this.$t("schedule.glueDecomposePlan.planDate"),
            },

            {
              prop: "glue",
             //  sortable: "custom",
              halign: "center",
              align: "left",
              minWidth: 100,
              label: this.$t("schedule.glueDecomposePlan.glue"),
            },
            {
              prop: "isFinishing",
             //  sortable: "custom",
              halign: "center",
              align: "center",
              label: this.$t("schedule.glueCollectPlan.isFinishing"),
              formatter: (row, column, value, index) => {
                return this.selectDictLabel(this.dict.type.IS_HAVE, value);
              },
            },
          ],
        },
        {
          label: this.$t("schedule.glueDecomposePlan.mixArea"),
          children: [
            {
              prop: "planQty",
             //  sortable: "custom",
              halign: "center",
              align: "right",
              label: this.$t("schedule.glueDecomposePlan.planQty"),
            },
            {
              prop: "stockQty",
             //  sortable: "custom",
              halign: "center",
              align: "right",
              label: this.$t("schedule.glueDecomposePlan.stockQty"),
            },
            {
              prop: "safeStockQty",
             //  sortable: "custom",
              halign: "center",
              align: "right",
              label: this.$t("schedule.glueDecomposePlan.safeStockQty"),
              // editable: {
              //   type: "text",
              //   label: this.$t("schedule.glueDecomposePlan.safeStockQty"),
              //   emptytext: "-",
              //   validate: function (value) {
              //     // 输入非负整数正则校验
              //     var regu = /^\d+$/;
              //     if (!regu.test(value) && $.common.isNotEmpty(value)) {
              //       layer.msg(this.$t("ui.message.editable.digits"));
              //       return this.$t("ui.message.editable.digits");
              //     }
              //     if (value.length > 7) {
              //       layer.msg(this.$t("ui.message.editable.maxSeven"));
              //       return this.$t("ui.message.editable.maxSeven");
              //     }
              //   },
              // },
            },
            {
              prop: "reserveStockRate",
              label: this.$t("schedule.glueDecomposePlan.reserveStockRate"),
              align: "right",
            },
            {
              prop: "produceQty",
             //  sortable: "custom",
              halign: "center",
              align: "right",
              label: this.$t("schedule.glueDecomposePlan.produceQty"),
              // editable: {
              //   type: "text",
              //   label: this.$t("schedule.glueDecomposePlan.produceQty"),
              //   emptytext: "-",
              //   validate: function (value) {
              //     // 输入非负整数正则校验
              //     var regu = /^\d+$/;
              //     if (!regu.test(value) && $.common.isNotEmpty(value)) {
              //       layer.msg(this.$t("ui.message.editable.digits"));
              //       return this.$t("ui.message.editable.digits");
              //     }
              //     if (value.length > 7) {
              //       layer.msg(this.$t("ui.message.editable.maxSeven"));
              //       return this.$t("ui.message.editable.maxSeven");
              //     }
              //   },
              // },
            },
            {
              prop: "machineName",
             //  sortable: "custom",
              halign: "center",
              align: "center",
              label: this.$t("setting.machine.machineName"),
              minWidth: 120,
              // formatter: (row, column, value, index) => {
              //   if ($.common.isEmpty(value)) {
              //     value = "-";
              //   }
              //   if (row.glue.indexOf("/") >= 0) {
              //     var actions = [];
              //     var result = [];
              //     if (value.indexOf(",") > 0) {
              //       var valueArr = value.split(",");
              //       for (let i = 0; i < valueArr.length; i++) {
              //         result.push(
              //           $.common.sprintf(
              //             "<span class='badge badge-primary'>%s</span> ",
              //             valueArr[i]
              //           )
              //         );
              //       }
              //     } else {
              //       result.push(
              //         $.common.sprintf(
              //           "<span class='badge badge-primary'>%s</span> ",
              //           value
              //         )
              //       );
              //     }
              //     actions.push(
              //       '<a href="javascript:void(0)" onclick="modifyMachine(\'' +
              //         row.id +
              //         "')\">" +
              //         result.join("") +
              //         "</a> "
              //     );
              //     return actions.join("");
              //   } else {
              //     return value;
              //   }
              // },
            },
          ],
        },
      ];

      return columns;
    },
    searchColumns() {
      return [
        {
          label: this.$t("schedule.glueDecomposePlan.planDate"),
          prop: "planDate",
          type: "date",
          valueFormat: "yyyy-MM-dd",
        },
        {
          label: this.$t("schedule.glueDecomposePlan.mixArea"),
          prop: "mixArea",
          type: "select", //MIX_AREA
          dictData: this.dict.type.MIX_AREA,
        },
        {
          label: this.$t("schedule.glueDecomposePlan.glue"),
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
        console.log(ids);
        removeGlueDecomposePlan({ ids }).then((data) => {
          this.$modal.msgSuccess(data.msg);
          // this.$set(this.page, "current", 1);
          this.getList();
        });
      });
    },
    handleDecompositionPlan() {
      this.$refs.deRef.show();
    },
    handleSendCrossRegional() {
      this.$router.push({
        path: "/mixSchedule/mixRubberDemandPlanDecomposeSendCrossRegional",
        query: {
          entrustMixArea: this.query.mixArea,
        },
      });
    },
    handleReceiveCrossRegional() {},

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
      downloadLink("/schedule/glueDecomposePlan/export", this.formatParams());
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
        const data = await listGlueDecomposePlan(this.formatParams());
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
