
<template>
  <basic-container>
    <page-table
      tableRef="beadScheduleMainTable"
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
          v-hasPermi="['schedule:glueDemandPlan:add']"
          @click="handleAdd"
          >{{ $t("ui.frame.btn.add") }}</el-button
        >
        <el-button
          type="primary"
          plain
          :disabled="selection.length != 1"
          @click="() => handleEdit(selection[0])"
          v-hasPermi="['schedule:glueDemandPlan:edit']"
          >{{ $t("ui.frame.btn.modify") }}</el-button
        >
        <el-button
          type="danger"
          plain
          :disabled="selection.length === 0"
          v-hasPermi="['schedule:glueDemandPlan:remove']"
          @click="handleDelete(selection)"
          >{{ $t("ui.frame.btn.delete") }}</el-button
        >
        <el-button
          v-hasPermi="['schedule:glueDemandPlan:import']"
          @click="() => $refs.tltUploadForm.handleImport(importDefaultValue)"
          >{{ $t("ui.frame.btn.import") }}</el-button
        >
        <el-button
          v-hasPermi="['schedule:glueDemandPlan:export']"
          @click="handleExportUiExcel"
          >{{ $t("ui.frame.btn.export") }}</el-button
        >
        <el-button
          v-hasPermi="['schedule:glueDemandPlan:rematch']"
          type="primary"
          plain
          @click="handleRematch"
          >{{ $t("schedule.glueDemandPlan.btn.rematch") }}</el-button
        >
        <!--<el-button-->
        <!--  v-hasPermi="['schedule:glueDemandPlan:grab']"-->
        <!--  type="primary"-->
        <!--  plain-->
        <!--  @click="handleGrab"-->
        <!--  >{{ $t("schedule.glueDemandPlan.btn.grab") }}</el-button-->
        <!--&gt;-->
      </template>
    </page-table>

    <addDialog ref="addRef" @success="getList" />
    <editDialog ref="editRef" @success="getList" />
    <grabDialog ref="grabRef" @success="getList" />
    <tlt-upload-form
      ref="tltUploadForm"
      title="导入分厂胶料需求计划数据"
      downloadUrl="/schedule/glueDemandPlan/importTemplate"
      uploadUrl="/schedule/glueDemandPlan/importData"
      @uploadSuccess="getList"
      :columns="importColumns"
      :rules="importRules"
    />
  </basic-container>
</template>
<script>
//lib
import moment from "moment";
//utils
import {downloadLink} from "@/utils/request";
//interface
import {listGlueDemandPlan, rematchGlueDemandPlan, removeGlueDemandPlan,} from "@/api/schedule/glueDemandPlan";
//components
import TltUploadForm from "@/views/components/tltUploadForm.vue";

import addDialog from "./components/addDialog.vue";
import editDialog from "./components/editDialog.vue";
import grabDialog from "./components/grabDialog.vue";

export default {
  name: "MixRubberDemandPlan",
  components: {
    TltUploadForm,
    addDialog,
    editDialog,
    grabDialog,
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
      // page: {
      //   current: 1,
      //   pageSize: 20,
      //   total: 0,
      // },
      page: undefined,
      sort: {},
      search: {},
      query: {},
      importDefaultValue: {
        planDate:  moment().add(1, "days").format("YYYY-MM-DD")
      },
      importRules: {
        planDate: [{
          required: true,
          message: this.$t("common.rule.select"),
        }],
        factory: [{
          required: true,
          message: this.$t("common.rule.select"),
        }],
      },
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
              label: this.$t("schedule.glueDemandPlan.planDate"),
              minWidth: 100
            },
            {
              prop: "factory",
              halign: "center",
              align: "center",
             //  sortable: "custom",
              label: this.$t("schedule.glueDemandPlan.factory"),
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
              label: this.$t("schedule.glueDemandPlan.glue"),
            },
            {
              prop: "mixArea",
              halign: "center",
              align: "center",
             //  sortable: "custom",
              label: this.$t("schedule.glueDemandPlan.mixArea"),
              formatter: (row, column, value, index) => {
                if (this.isEmpty(value)) {
                  return "";
                }
                return this.selectDictLabel(this.dict.type.MIX_AREA, value);
              },
              // editable: {
              //   type: "text",
              //   label: this.$t("schedule.glueDemandPlan.mixArea"),
              //   emptytext: "-",
              //   validate: function (value) {
              //     // 只可输入英文校验
              //     var regu = /^[\x00-\xff]*$/;
              //     if (!regu.test(value) && $.common.isNotEmpty(value)) {
              //       layer.msg(this.$t("ui.message.editable.isCode"));
              //       return this.$t("ui.message.editable.isCode");
              //     }
              //     var valueArr = value == null ? [] : value.split(",");
              //     // 输入密炼区是否有重复判断
              //     if (isRepeat(valueArr)) {
              //       layer.msg(this.$t("ui.message.editable.isRepeat"));
              //       return this.$t("ui.message.editable.isRepeat");
              //     }
              //     // 密炼区字典校验
              //     for (let i = 0; i < valueArr.length; i++) {
              //       if (mixAreaDictLabels.indexOf(valueArr[i]) < 0) {
              //         layer.msg(
              //           valueArr[i] +
              //             this.$t("ui.message.editable.mixAreaNotExist")
              //         );
              //         return (
              //           valueArr[i] +
              //           this.$t("ui.message.editable.mixAreaNotExist")
              //         );
              //       }
              //     }
              //   },
              // },
            },
            {
              prop: "totalPlanQty",
             //  sortable: "custom",
              halign: "center",
              align: "right",
              label: this.$t("schedule.glueDemandPlan.totalPlanQty"),
              // editable: {
              //   type: "text",
              //   label: this.$t("schedule.glueDemandPlan.totalPlanQty"),
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
              label: this.$t("schedule.glueDemandPlan.midPlanQty"),
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
              label: this.$t("schedule.glueDemandPlan.midRemark"),
              // editable: {
              //   type: "text",
              //   label: this.$t("schedule.glueDemandPlan.midRemark"),
              //   emptytext: "-",
              //   validate: function (value) {
              //     if (value.length > 300) {
              //       layer.msg(this.$t("ui.message.editable.maxLength"));
              //       return this.$t("ui.message.editable.maxLength");
              //     }
              //   },
              // },
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
              label: this.$t("schedule.glueDemandPlan.nightPlanQty"),
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
              label: this.$t("schedule.glueDemandPlan.nightRemark"),
              // editable: {
              //   type: "text",
              //   label: this.$t("schedule.glueDemandPlan.nightRemark"),
              //   emptytext: "-",
              //   validate: function (value) {
              //     if (value.length > 300) {
              //       layer.msg(this.$t("ui.message.editable.maxLength"));
              //       return this.$t("ui.message.editable.maxLength");
              //     }
              //   },
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
          label: this.$t("schedule.glueDemandPlan.planDate"),
          prop: "planDate",
          type: "date",
          valueFormat: "yyyy-MM-dd",
        },
        {
          label: this.$t("schedule.glueDemandPlan.factory"),
          prop: "factory",
          type: "select", //FACTORY
          dictData: this.dict.type.FACTORY,
        },
        {
          label: this.$t("schedule.glueDemandPlan.mixArea"),
          prop: "mixArea",
          type: "select", //MIX_AREA
          dictData: this.dict.type.MIX_AREA,
        },
        {
          label: this.$t("schedule.glueDemandPlan.glue"),
          prop: "glue",
        },
      ];
    },
    importColumns() {return [
      {
        label: "排程日期",
        prop: "planDate",
        type: "date",
        valueFormat: "yyyy-MM-dd",
      },
      {
        label: this.$t("schedule.glueDemandPlan.factory"),
        prop: "factory",
        type: "select", //FACTORY
        dictData: this.dict.type.FACTORY,
      },
    ]},
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
        removeGlueDemandPlan({ ids: ids }).then((data) => {
          this.$modal.msgSuccess(data.msg);
          // this.$set(this.page, "current", 1);
          this.getList();
        });
      });
    },
    handleRematch() {
      this.$confirm(
        this.$t("schedule.glueDemandPlan.btn.makeSureRematch")
      ).then(async () => {
        try {
          this.loading = true;
          let ids = this.selection.map((row) => row.id).join(",");
          const res = await rematchGlueDemandPlan({
            ids: ids,
            planDate: this.query.planDate,
          });
          this.$modal.msgSuccess(res.msg);
          this.loading = false;
        } catch (error) {
          console.error(error);
          this.loading = false;
        }
      });
    },
    handleGrab() {
      if (this.$refs.grabRef) {
        this.$refs.grabRef.show();
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
      const params = this.formatParams(false);
      downloadLink("/schedule/glueDemandPlan/export", params);
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
        const data = await listGlueDemandPlan(this.formatParams());
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
