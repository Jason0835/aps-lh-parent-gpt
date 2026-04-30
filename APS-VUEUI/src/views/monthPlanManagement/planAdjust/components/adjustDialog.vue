<template>
  <el-dialog
    :title="title"
    :visible="visible"
    width="800px"
    @close="hide"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    :append-to-body="true"
    fullscreen
  >
    <div class="adjust" v-loading="loading">
      <div class="header">
        <el-form inline>
          <el-form-item label="模具">
            <el-input disabled :value="info.mouldNo" />
          </el-form-item>
          <el-form-item label="SAP代码">
            <el-input disabled :value="info.productCode" />
          </el-form-item>
          <el-form-item label="开始日期">
            <el-input disabled :value="info.startDate" />
          </el-form-item>
          <el-form-item label="调整量">
            <el-input disabled :value="info.adjustNumber" />
          </el-form-item>
        </el-form>
      </div>
      <div class="content">
        <page-table
          ref="tableRef"
          tableRef="adjustDetailDialogTable"
          :calcHeight="true"
          :data="info.planSubtractList"
          :columns="columns"
          @current-change="handleCurrentChange"
          highlight-current-row
        >
          <template slot="headerRight">
            <div class="heard-content">
              <div class="item">
                <label class="label">SAP代码</label>
                <el-input
                  readonly
                  placeholder="请选择表格行数据"
                  :value="currentRow ? currentRow.productCode : ''"
                />
              </div>
              <div class="item">
                <label class="label">开始日期</label>
                <el-date-picker
                  value-format="yyyy-MM-dd"
                  v-model="form.startDate"
                  :clearable="false"
                />
              </div>
              <div class="item">
                <label class="label">调减量</label>
                <el-input-number
                  v-model="form.adjustNumber"
                  :controls="false"
                  :min="0"
                  :precision="0"
                />
              </div>
              <el-button
                class="btn"
                type="primary"
                plain
                :disabled="currentRow === null"
                @click="handleReduce"
                >应用</el-button
              >
            </div>
          </template>
        </page-table>
      </div>
    </div>
    <template slot="footer">
      <el-button @click="hide">{{ this.$t("common.button.cancel") }}</el-button>
      <el-button  v-hasPermi="['monthplan:planAdjust:confirm']"  type="primary" :loading="loading" @click="handleConfirm">{{
        this.$t("common.button.confirm")
      }}</el-button>
    </template>
  </el-dialog>
</template>

<script>
import moment from "moment";

import infoForm from "@/views/components/infoForm.vue";

import { adjustFactoryMonthPlan } from "@/api/factory/monthPlanAdjust.js";
export default {
  components: { infoForm },
  props: {
    startAdjustDate: String,
  },
  inject: ["parentDict"],
  data() {
    return {
      loading: false,
      visible: false,
      isEdit: false,
      editType: null,
      info: {},
      tableData: [],
      selection: [],
      currentRow: null,
      form: {},
      confirmSubtractMap: {},
    };
  },
  computed: {
    title: function () {
      return "需要调减计划";
    },
    columns() {
      let columns = [
        // {
        //   label: "",
        //   prop: "selection",
        //   render: () => {
        //     return
        //   }

        // },
        // {
        //   label: this.$t("ui.data.column.mouldingDayResult.物料编码"),
        //   prop: "productCode",
        //   minWidth: 100,
        //   sortable: "custom",
        // },
        // {
        //   label: this.$t("ui.data.column.mouldingDayResult.物料描述"),
        //   prop: "productDesc",
        //   minWidth: 100,
        //   sortable: "custom",
        // },
        {
          label: this.$t("ui.data.column.mouldingDayResult.factoryCode"),
          prop: "factoryCode",
          minWidth: 100,
          formatter: (row, column, value) => {
            return this.selectDictLabel(
              this.parentDict.type.biz_factory_name,
              value
            );
          },
        },
        {
          label: this.$t("ui.data.column.mouldingDayResult.year"),
          prop: "year",
          minWidth: 100,
          // sortable: "custom",
        },
        {
          label: this.$t("ui.data.column.mouldingDayResult.month"),
          prop: "month",
          minWidth: 100,
          // sortable: "custom",
        },
        {
          label: this.$t("ui.data.column.mouldingDayResult.productCode"),
          prop: "productCode",
          minWidth: 100,
          // sortable: "custom",
        },
        {
          label: this.$t("ui.data.column.mouldingDayResult.productDesc"),
          prop: "productDesc",
          minWidth: 100,
          width: 250,
          // sortable: "custom",
        },

        // {
        //   label: this.$t("ui.data.column.mouldingDayResult.施工号"),
        //   prop: "productDesc",
        //   minWidth: 100,
        //   sortable: "custom",
        // },
        {
          label: this.$t("ui.data.column.mouldingDayResult.locationType"),
          prop: "locationType",
          minWidth: 100,
          // sortable: "custom",
          formatter: (row, column, value) => {
            return this.selectDictLabel(
              this.parentDict.type.biz_stor_type,
              value
            );
          },
        },
        {
          label: this.$t("ui.data.column.mouldingDayResult.channel"),
          prop: "channel",
          minWidth: 100,
          // sortable: "custom",
          formatter: (row, column, value) => {
            return this.selectDictLabel(
              this.parentDict.type.biz_channel_type,
              value
            );
          },
        },
        {
          label: this.$t("ui.data.column.mouldingDayResult.brand"),
          prop: "brand",
          minWidth: 100,
          // sortable: "custom",
          formatter: (row, column, value) => {
            return this.selectDictLabel(
              this.parentDict.type.biz_brand_type,
              value
            );
          },
        },
        {
          label: this.$t("ui.data.column.mouldingDayResult.specifications"),
          prop: "specifications",
          minWidth: 100,
          // sortable: "custom",
        },

        {
          label: this.$t("ui.data.column.mouldingDayResult.proSize"),
          prop: "proSize",
          minWidth: 100,
          // sortable: "custom",
        },
        {
          label: this.$t("ui.data.column.mouldingDayResult.pattern"),
          prop: "pattern",
          minWidth: 140,
          // sortable: "custom",
        },

        {
          label: this.$t("ui.data.column.mouldingDayResult.prodReqPlan"),
          prop: "prodReqPlan",
          minWidth: 100,
        },
        {
          label: this.$t("ui.data.column.mouldingDayResult.totalQty"),
          prop: "totalQty",
          minWidth: 100,
        },
        {
          label: this.$t("ui.data.column.mouldingDayResult.factProdReqQty"),
          prop: "factProdReqQty",
          minWidth: 100,
        },
        {
          label: this.$t("ui.data.column.mouldingDayResult.differenceQty"),
          prop: "differenceQty",
          minWidth: 100,
        },
        {
          label: this.$t("ui.data.column.mouldingDayResult.mouldNo"),
          prop: "mouldNo",
          minWidth: 100,
        },
        {
          label: this.$t("ui.data.column.mouldingDayResult.mouldQty"),
          prop: "mouldQty",
          minWidth: 100,
        },
        {
          label: this.$t("ui.data.column.mouldingDayResult.beginDate"),
          prop: "beginDate",
          minWidth: 100,
        },
        {
          label: this.$t("ui.data.column.mouldingDayResult.endDay"),
          prop: "endDay",
          minWidth: 100,
        },
        // {
        //   label: this.$t("ui.data.column.mouldingDayResult.isDeliveryDate"),
        //   prop: "isDeliveryDate",
        //   minWidth: 100,
        //   formatter: (row, column, value) => {
        //     return this.selectDictLabel(this.parentDict.type.biz_yes_no, value);
        //   },
        // },
        // {
        //   label: this.$t("ui.data.column.mouldingDayResult.reason"),
        //   prop: "reason",
        //   minWidth: 140,
        // },
        // {
        //   label: this.$t("common.remark"),
        //   prop: "remark",
        //   minWidth: 100,
        //   // sortable: "custom",
        // },
      ];
      if (true) {
        //显示每日数据
        const date = moment();
        // const year = date.year();
        const month = date.month() + 1;
        const days = date.daysInMonth();

        for (let i = 0; i < days; i++) {
          columns.push({
            // label: `${i + 1}号`,
            label: this.$t("ui.data.column.mouldingDayResult.day", {
              day: i + 1,
            }),
            prop: `day${i + 1}`,
            minWidth: "80px",
            type: "number",
          });
        }
      }
      return columns;
    },
  },
  methods: {
    // api
    async save(params) {
      try {
        this.loading = true;

        // this.$modal.msgSuccess(res.msg);
        this.$emit("success", params, (status) => {
          if (status === "success") {
            this.hide();
          }
          this.loading = false;
        });
      } catch (error) {
        console.log(error);
        this.loading = false;
      }
    },
    //utils
    show(data) {
      this.visible = true;
      if (data) {
        this.isEdit = true;
        this.info = data;

        this.form.startDate = data.startDate;
      }
    },
    hide() {
      this.currentRow = null;
      this.info = {};
      this.form = {};
      this.confirmSubtractMap = {};
      this.isEdit = false;
      this.visible = false;
    },
    update(data) {
      this.currentRow = null;
      this.confirmSubtractMap = {};
      if (this.$refs.tableRef) {
        this.$refs.tableRef.getTableRef().setCurrentRow(null);
      }
      this.info = data;
      this.form = {
        startDate: data.startDate,
      };
    },

    numberEmpty(val) {
      return this.isEmpty(val) ? undefined : val;
    },

    handleCurrentChange(row) {
      this.currentRow = row;
    },
    handleSelectionChange(rows) {
      this.selection = rows;
    },
    handleReduce() {
      const day = moment(this.info.startDate).format("DD");
      console.log(day);
      let adjustNumber = this.form.adjustNumber;
      // let sum = ajs;
      for (let i = 20; i <= 31; i++) {
        let key = `day${i}`;

        let num = this.currentRow[key];
        if (num && !isNaN(num) && num > 0) {
          if (num > adjustNumber) {
            this.currentRow[key] = num - adjustNumber;
            adjustNumber = 0;
            break;
          } else {
            adjustNumber -= num;
            this.currentRow[key] = 0;
          }
        }
      }

      if (this.confirmSubtractMap[this.currentRow.productionNo]) {
        let adjustNumber =
          this.confirmSubtractMap[this.currentRow.productionNo].adjustNumber;
        adjustNumber -= this.form.adjustNumber;
        this.confirmSubtractMap[this.currentRow.productionNo].adjustNumber =
          adjustNumber;
      } else {
        this.confirmSubtractMap[this.currentRow.productionNo] = {
          productionNo: this.currentRow.productionNo,
          startDate: this.form.startDate,
          adjustNumber: 0 - this.form.adjustNumber,
        };
      }

      // this.selection.map(() => ())
    },

    handleConfirm() {
      this.loading = true;
      this.$emit(
        "success",
        {
          adjustNumber: this.info.adjustNumber,
          factoryCode: this.info.factoryCode,
          month: this.info.month,
          mouldNo: this.info.mouldNo,
          productCode: this.info.productCode,
          productionNo: this.info.productionNo,
          startDate: this.info.startDate,
          year: this.info.year,
          confirmSubtractList: Object.values(this.confirmSubtractMap),
        },
        (status, data) => {
          console.log(data);
          this.loading = false;
          if (this.status === "error") {
            return;
          }

          if (status == 1) {
            this.hide();
            this.$modal.msgSuccess("操作成功");
          } else {
            this.update(data);
            this.$modal.msgError("调整数量不满足， 需要继续调整");
          }
        }
      );
    },
  },
};
</script>
<style lang="scss" scoped>
.adjust {
  display: flex;
  flex-direction: column;
  height: 100%;
  .header {
    flex: 0 0 auto;
    margin-bottom: 20px;
  }
  .content {
    flex: 1 1 auto;
  }
}

.heard-content {
  display: flex;
  align-items: center;
  .item {
    display: flex;
    align-items: center;
    margin-right: 10px;
    margin-bottom: 5px;
    .label {
      flex: 0 0 auto;
      &::after {
        content: "：";
      }
    }
  }
  .btn {
    margin-right: 10px;
  }
}
</style>