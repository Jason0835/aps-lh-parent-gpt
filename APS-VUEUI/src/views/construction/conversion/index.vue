
<template>
  <basic-container class="conversion-container" v-loading="loading">
    <div class="computed-search">
      <el-form :model="form" inline>
        <el-form-item :label="$t('ui.data.column.conversion.embryoCode')">
          <el-input v-model="form.embryoCode"
        /></el-form-item>
        <el-form-item :label="$t('ui.data.column.conversion.bomDataVersion')">
          <el-select v-model="form.bomDataVersion">
            <el-option
              v-for="item in embryoVersions"
              :key="item.embryoVersion"
              :label="item.embryoVersion"
              :value="item.embryoVersion"
            ></el-option>
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('ui.data.column.conversion.queryPlan')">
          <el-input v-model="form.queryPlan"
        /></el-form-item>
        <el-form-item :label="$t('ui.data.column.scheduleResult.scheduleDate')">
          <el-date-picker
            type="date"
            v-model="form.scheduleDate"
            value-format="yyyy-MM-dd"
          ></el-date-picker
        ></el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleCompute">计算</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>
    <div class="computed-content">
      <page-table
        tableRef="CuringDispatcherLogMainTable"
        :calcHeight="true"
        :columns="columns"
        :searchColumns="[]"
        :data="data"
        :page="undefined"
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
            v-hasPermi="['cx:conversion:save']"
            :disabled="selection.length == 0"
            @click="handleBatchSave(selection)"
            >{{ $t("common.button.save") }}</el-button
          >
          <el-button
            type="primary"
            v-hasPermi="['cx:conversion:publish']"
            :disabled="selection.length == 0"
            @click="handleBatchPublish(selection)"
            >{{ $t("ui.lh.moldChange.publish") }}</el-button
          >
        </template>
      </page-table>
    </div>
    <!-- <el-button style="display: none" ref="hidePopoverBtnRef"></el-button> -->
    <!-- <tlt-upload
      ref="tltUpload"
      downloadUrl="/cx/badNumber/importTemplate"
      uploadUrl="/cx/badNumber/importData"
      @uploadSuccess="getList"
    /> -->
    <!-- <infoDialog ref="infoRef" @success="getList" /> -->
    <chooseMachineDialog ref="chooseRef" @success="updateRowMachine" />
  </basic-container>
</template>
<script>
//lib
import moment from "moment";
//utils
import { downloadLink } from "@/utils/request";
//interface
import {
  listConversion,
  batchSaveConversion,
  batchPublishConversion,
} from "@/api/cx/conversion";
import { getVersionsByEmbryoCode } from "@/api/cx/productConstruction";

//components
// import tltUpload from "@/components/tltUpload/tltUpload.vue";

// import infoDialog from "./components/infoDialog.vue";
import chooseMachineDialog from "./components/chooseMachineDialog";
import TPopover from "@/views/components/tPopover.vue";

export default {
  name: "Conversion",
  components: {
    // tltUpload,
    // infoDialog,
    chooseMachineDialog,
  },
  dicts: ["half_part_type", "unit", "IS_RELEASE"],
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
      importDefaultValue: {},
      importRules: {},
      embryoVersions: [],
      form: {
        scheduleDate: moment().add(1, "days").format("YYYY-MM-DD"),
      },
    };
  },
  computed: {
    columns() {
      let columns = [
        { type: "selection", fixed: "left" },
        {
          prop: "halfPartType",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.conversion.halfPartType"),
          formatter: (row, column, value, index) => {
            return this.selectDictLabel(this.dict.type.half_part_type, value);
          },
        },
        {
          prop: "halfPartCode",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.conversion.halfPartCode"),
        },
        {
          prop: "plan",
          align: "right",
          halign: "center",
          label: this.$t("ui.data.column.conversion.plan"),
        },
        {
          prop: "unit",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.conversion.unit"),
          formatter: (row, column, value, index) => {
            return this.selectDictLabel(this.dict.type.unit, value);
          },
        },
        {
          prop: "machineId",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.scheduleResult.produceLine"),
          width: 180,
          formatter: (row, column, value, index) => {
            if (row.halfPartType == "8") {
              return value;
            }

            let val = this.isEmpty(value)
              ? this.$t("ui.data.column.selectMachineName")
              : row.machineName;
            return (
              <text-button onClick={() => this.handleChooseMachine(row, index)}>
                {val}
              </text-button>
            );
          },
        },
        {
          prop: "class1Plan",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.scheduleResult.nightPlanQty"),
          render: ({ row, $index }) => {
            return (
              <TPopover
                title={
                  this.$t("ui.data.column.scheduleResult.plan") +
                  "(" +
                  this.$t("ui.data.column.scheduleResult.unit.meter") +
                  ")"
                }
                v-model={row.class1Plan}
                showClose={false}
                type="number"
                min={0}
                onConfirm={(val) => {
                  this.updateRow({ key: "class1Plan", value: val }, $index);
                }}
              ></TPopover>
            );
          },
          // editable: {
          //   type: "text",
          //   label:
          //     this.$t("ui.data.column.scheduleResult.plan") +
          //     "(" +
          //     this.$t("ui.data.column.scheduleResult.unit.meter") +
          //     ")",
          //   emptytext: "-",
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
          //       layer.msg(this.$t("ui.data.column.mdmMonthProdPlan.greatThan"));
          //       return this.$t("ui.data.column.mdmMonthProdPlan.greatThan");
          //     }
          //   },
          // },
        },
        {
          prop: "class2Plan",
          align: "center",
          halign: "center",
          // label: this.$t("ui.data.column.scheduleResult.nightPlanQty"),
          label: this.$t("ui.data.column.scheduleResult.dayPlanQty"),
          render: ({ row, $index }) => {
            return (
              <TPopover
                title={
                  this.$t("ui.data.column.scheduleResult.plan") +
                  "(" +
                  this.$t("ui.data.column.scheduleResult.unit.meter") +
                  ")"
                }
                v-model={row.class2Plan}
                showClose={false}
                type="number"
                min={0}
                onConfirm={(val) => {
                  this.updateRow({ key: "class2Plan", value: val }, $index);
                }}
              ></TPopover>
            );
          },
          // editable: {
          //   type: "text",
          //   label:
          //     this.$t("ui.data.column.scheduleResult.plan") +
          //     "(" +
          //     this.$t("ui.data.column.scheduleResult.unit.meter") +
          //     ")",
          //   emptytext: "-",
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
          //       layer.msg(this.$t("ui.data.column.mdmMonthProdPlan.greatThan"));
          //       return this.$t("ui.data.column.mdmMonthProdPlan.greatThan");
          //     }
          //   },
          // },
        },

        {
          prop: "status",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.status"),
        },
        {
          prop: "option",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.btn.option"),
          render: ({ row }) => {
            return (
              <div>
                <el-button
                  class="minus"
                  type="primary"
                  onClick={() => this.handleBatchSave([row])}
                >
                  {this.$t("common.button.save")}
                </el-button>
                <el-button
                  class="minus"
                  type="success"
                  onClick={() => this.handleBatchPublish([row])}
                >
                  {this.$t("ui.lh.moldChange.publish")}
                </el-button>
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
          label: this.$t("ui.data.column.conversion.embryoCode"),
          prop: "sapCode",
        },
        {
          label: this.$t("ui.data.column.conversion.bomDataVersion"),
          prop: "bomDataVersion",
          type: "select",
        },
        {
          label: this.$t("ui.data.column.conversion.queryPlan"),
          prop: "queryPlan",
        },
        {
          label: this.$t("ui.data.column.scheduleResult.scheduleDate"),
          prop: "scheduleDate",
        },
      ];
    },
  },
  watch: {
    "form.embryoCode": function (val) {
      if (val) {
        this.getVersionsByEmbryoCode(val);
      } else {
        this.embryoVersions = [];
      }
    },
  },
  methods: {
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
      // this.$confirm(this.$t("common.confirm.delete"), {
      //   type: "warning",
      // }).then(() => {
      //   const ids = row.id;
      //   this.loading = true;
      //   removeQuota({ ids })
      //     .then((data) => {
      //       this.$modal.msgSuccess(data.msg);
      //       this.$set(this.page, "current", 1);
      //       this.getList();
      //     })
      //     .catch((error) => {
      //       console.log(error);
      //       this.loading = false;
      //     });
      // });
    },
    handleBatchSave(rowData) {
      for (var i = 0; i < rowData.length; i++) {
        if (!rowData[i].machineId && rowData[i].halfPartType != "8") {
          this.$modal.alertWarning(
            this.$t("mes.error.message.cxHalfPartConversion.machineIdIsNull")
          );
          return;
        }
      }
      this.$confirm(`${this.$t("是否保存")}`).then(async () => {
        try {
          this.loading = true;
          const res = await batchSaveConversion({
            listStr: JSON.stringify(rowData),
            scheduleDate: this.form.scheduleDate,
          });
          this.$modal.msgSuccess("操作成功");
          this.getList();
          // console.log(res);
        } catch (error) {
          console.log(error);
          this.loading = false;
        }
      });
    },
    handleBatchPublish(rowData) {
      for (var i = 0; i < rowData.length; i++) {
        if (!rowData[i].machineId && rowData[i].halfPartType != "8") {
          this.$modal.alertWarning(
            this.$t("mes.error.message.cxHalfPartConversion.machineIdIsNull")
          );
          return;
        }
      }
      this.$confirm(`确认${this.$t("ui.lh.moldChange.publish")}?`).then(
        async () => {
          try {
            this.loading = true;
            const res = await batchPublishConversion({
              listStr: JSON.stringify(rowData),
              scheduleDate: this.form.scheduleDate,
            });
            this.$modal.msgSuccess("操作成功");
            this.getList();
            console.log(res);
          } catch (error) {
            console.error(error);
            this.loading = false;
          }
        }
      );
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
      downloadLink("/cx/dispatcherLog/export", this.formatParams(false));
    },

    handleCompute() {
      this.getList();
    },
    handleReset() {
      this.form = {};
      this.data = [];
    },
    handleChooseMachine(row, index) {
      if (this.$refs.chooseRef) {
        this.$refs.chooseRef.show(row, index);
      }
    },
    updateRowMachine(data, index) {
      this.data[index].machineId = data.machineId;
      this.data[index].machineName = data.machineName;
    },
    updateRow(data, index) {
      this.data[index][data.key] = data.value;
    },

    // utils
    formatParams(hasPage = true) {
      const params = {
        ...this.form,
        ...this.sort,
      };

      if (hasPage) {
        // params.pageSize = this.page.pageSize;
        // params.pageNum = this.page.current;
        params.pageSize = 10000;
        params.pageNum = 1;
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
        const data = await listConversion(this.formatParams());
        this.data = data.rows;
        this.page.total = data.total;
      } catch (error) {
        console.error(error);
      } finally {
        this.loading = false;
      }
    },
    async getVersionsByEmbryoCode(val) {
      try {
        const res = await getVersionsByEmbryoCode({
          embryoCode: val,
        });
        this.embryoVersions = res;
        if (res.length) {
          this.form.bomDataVersion = res[0].embryoVersion;
        }
      } catch (error) {
        console.error(error);
        this.embryoVersions = [];
      }
    },
  },
  created() {},
  activated() {
    this.getList();
  },
};
</script>
<style lang="scss" scoped>
.conversion-container {
  display: flex;
  flex-direction: column;
  // height: 100%;
  .computed-search {
    flex: 0 0 auto;
    // height: 40px;
  }
  .computed-content {
    flex: 1 1 auto;
    // height: calc(100% - 40px);
  }
}
</style>
