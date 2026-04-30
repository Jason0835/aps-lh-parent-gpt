<template>
  <el-dialog
    :title="title"
    :visible="visible"
    fullscreen
    @close="hide"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    :append-to-body="true"
  >
    <div class="adjust" v-loading="loading">
      <div class="header">
        <info-form
          class="form-item-height"
          ref="form"
          :form="form"
          :rules="rules"
          :columns="columns"
          label-position="right"
          label-width="120px"
          v-loading="loading"
        >
        </info-form>
      </div>
      <div class="content">
        <page-table
          ref="tableRef"
          tableRef="MonthPlanAdjustNoticeAdjustDialogTable"
          :calcHeight="true"
          :columns="tableColumns"
          :data="tableData"
          :toolbar="false"
          @current-change="handleCurrentChange"
          highlight-current-row
        >
          <template slot="headerRight">
            <div class="heard-content">
              <div class="item">
                <label class="label">SAP代码</label>
                <el-input
                  readonly
                  disabled
                  placeholder="请选择表格行数据"
                  :value="currentRow ? currentRow.productCode : ''"
                />
              </div>
              <div class="item">
                <label class="label">开始日期</label>
                <el-date-picker
                  value-format="yyyy-MM-dd"
                  v-model="adjustForm.startDate"
                  :clearable="false"
                  :picker-options="{
                    disabledDate: adjustDisabledDate,
                  }"
                />
              </div>
              <div class="item">
                <label class="label">调减量</label>
                <el-input-number
                  v-model="adjustForm.adjustNumber"
                  :controls="false"
                  :min="0"
                  :max="Math.abs(form.planQty)"
                  :precision="0"
                />
              </div>
              <el-button
                class="btn"
                type="primary"
                plain
                :disabled="
                  currentRow === null ||
                  adjustForm.adjustNumber === undefined ||
                  adjustForm.adjustNumber === null ||
                  adjustForm.adjustNumber === 0
                "
                @click="handleCalculateAddQty"
                >应用</el-button
              >
            </div>
          </template>
        </page-table>
      </div>
    </div>
    <template slot="footer">
      <el-button @click="hide">{{ this.$t("common.button.cancel") }}</el-button>
      <el-button
        type="primary"
        :loading="loading"
        :disabled="confirmDisabled"
        @click="handleConfirm"
        >{{ this.$t("common.button.confirm") }}</el-button
      >
    </template>
  </el-dialog>
</template>

<script>
import moment from "moment";

import infoForm from "@/views/components/infoForm.vue";

import {
  confirmAdjust,
  getOperatePlanList,
  calculateAddQty,
  executeAdjust,
} from "@/api/factory/monthPlanAdjustNotice";
import { matchMouldConfiguration } from "@/api/maindata/relation.js";

export default {
  components: { infoForm },
  inject: ["parentDict"],
  data() {
    return {
      loading: false,
      visible: false,
      isEdit: false,
      editType: null,
      form: {},
      adjustForm: {},
      tableData: [],
      specCodeList: [],
      currentRow: null,
      confirmSubtractMap: {},
      rules: {
        factoryCode: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
        yearMonth: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "blur",
          },
        ],
        locationType: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
        productCode: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
        needQty: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
        channel: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "blur",
          },
        ],
      },
      startAdjustDateTime: null,
      endAdjustDateTime: null,
    };
  },
  computed: {
    title: function () {
      if (this.form) {
        return this.form.planQty > 0 ? "调整" : "调减";
      }

      return this.isEdit
        ? this.$t("common.button.edit")
        : this.$t("common.button.add");
    },
    columns() {
      const columns = [
        {
          prop: "noticeNo",
          label: this.$t("ui.data.column.monthPlanAdjustNotice.noticeNo"),
          span: 6,
          disabled: true,
        },
        {
          prop: "factoryCode",
          label: this.$t("ui.data.column.monthPlanAdjustNotice.factoryCode"),
          span: 6,
          disabled: true,
          type: "select",
          dictData: this.parentDict.type.biz_factory_name,
        },
        {
          prop: "yearMonth",
          label: this.$t("ui.data.colume.yearMonth"),
          span: 6,
          disabled: true,
          type: "date",
          dateType: "month",
          valueFormat: "yyyy-MM",
        },
        {
          prop: "productCode",
          label: this.$t("ui.data.column.monthPlanAdjustNotice.productCode"),
          span: 6,
          disabled: true,
        },
        {
          prop: "locationType",
          label: this.$t("ui.data.column.monthPlanAdjustNotice.locationType"),
          span: 6,
          disabled: true,
          type: "select",
          dictData: this.parentDict.type.biz_stor_type,
        },
        {
          prop: "channel",
          label: this.$t("ui.data.column.monthPlanAdjustNotice.channel"),
          span: 6,
          disabled: true,
          type: "select",
          dictData: this.parentDict.type.biz_channel_type,
        },
        {
          prop: "startDate",
          label: this.$t("ui.data.column.monthPlanAdjustNotice.startDate"),
          span: 6,
          disabled: false,
          type: "date",
          valueFormat: "yyyy-MM-dd",
          listeners: {
            change: this.handleStartDateChange,
          },
          pickerOptions: {
            disabledDate: (time) => {
              if (
                time < this.startAdjustDateTime ||
                time > this.endAdjustDateTime
              ) {
                return true;
              } else {
                return false;
              }
            },
          },
        },
        {
          prop: "needQty",
          label: this.$t("ui.data.column.monthPlanAdjustNotice.planQty"),
          span: 6,
          disabled: true,
          // min: -99999999,
          // max: 99999999,
          // precision: 0,
        },
      ];

      if (this.form.planQty > 0) {
        columns.push(
          {
            label: "规格代码",
            prop: "specCode",
            span: 6,
            type: "select",
            dictData: this.specCodeList,
            listeners: {
              change: this.handleStartDateChange,
            },
          },
          {
            label: "模具",
            prop: "mouldNo",
            span: 6,
            disabled: true,
          },
          {
            label: "调整量",
            prop: "adjustNumber",
            span: 6,
            type: "number",
            min: 0,
            precision: 0,
            attrs: {
              controls: false,
            },
            listeners: {
              change: this.handleAdjustNumber,
            },
            // disabled: true,
          },
          {
            label: "空余产能量",
            prop: "leftOverQty",
            span: 6,
            disabled: true,
            placeholder: " ",
          },
          {
            label: "还需增量",
            prop: "stillNeedQty",
            span: 6,
            disabled: true,
          }
        );
      } else {
        columns.push(
          {
            label: "调整量",
            prop: "adjustNumber",
            span: 6,
            type: "number",
            max: 0,
            precision: 0,
            attrs: {
              controls: false,
            },
            // disabled: true,
            listeners: {
              change: this.handleAdjustNumber,
            },
          },
          {
            label: "还需调减量",
            prop: "stillNeedQty",
            span: 6,
            disabled: true,
          }
        );
      }
      columns.push({
        span: 6,
        render: () => {
          return (
            <el-button type="primary" onClick={this.handleOperatePlan}>
              调整建议
            </el-button>
          );
        },
      });

      return columns;
    },
    tableColumns() {
      const columns = [
        {
          prop: "year",
          label: this.$t("ui.data.column.monthPlanAdjustNotice.year"),
        },
        {
          prop: "month",
          label: this.$t("ui.data.column.monthPlanAdjustNotice.month"),
        },
        {
          prop: "productCode",
          label: this.$t("ui.data.column.monthPlanAdjustNotice.productCode"),
        },
        {
          prop: "specCode",
          label: this.$t("ui.data.column.monthPlanAdjustNotice.specCode"),
        },
        {
          prop: "mouldQty",
          label: this.$t("ui.data.column.monthPlanAdjustNotice.mouldNum"),
        },
        {
          prop: "mouldNo",
          label: this.$t("ui.data.column.monthPlanAdjustNotice.mouldNo"),
        },
        {
          prop: "locationType",
          label: this.$t("ui.data.column.monthPlanAdjustNotice.locationType"),
          width: 120,
          formatter: (row) => {
            return this.selectDictLabel(
              this.parentDict.type.biz_stor_type,
              row.locationType
            );
          },
        },
        {
          prop: "channel",
          label: this.$t("ui.data.column.monthPlanAdjustNotice.channel"),
          width: 120,
          formatter: (row, column, value) => {
            return this.selectDictLabel(
              this.parentDict.type.biz_channel_type,
              value
            );
          },
        },
        {
          prop: "brand",
          label: this.$t("ui.data.column.monthPlanAdjustNotice.brand"),
          formatter: (row, column, value) => {
            return this.selectDictLabel(
              this.parentDict.type.biz_brand_type,
              value
            );
          },
        },
        {
          prop: "prodReqPlan",
          label: this.$t("ui.data.column.monthPlanAdjustNotice.prodReqPlan"),
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
          prop: "beginDate",
          label: this.$t("ui.data.column.monthPlanAdjustNotice.beginDate"),
        },
        {
          prop: "endDay",
          label: this.$t("ui.data.column.monthPlanAdjustNotice.endDate"),
        },
      ];

      if (this.form.productionStartDate) {
        let start = moment(this.form.productionStartDate);
        let end = moment(this.form.productionStartDate).add(1, "M");
        let list = [];

        while (start.isBefore(end)) {
          list.push(start.format("DD"));
          start.add(1, "d");
        }
        for (let i = 0; i < list.length; i++) {
          let dayNumStr = list[i];
          columns.push({
            // label: `${i + 1}号`,
            label: this.$t("ui.data.column.mouldingDayResult.day", {
              day: Number(dayNumStr),
            }),
            prop: `day${i + 1}`,
            minWidth: "80px",
            type: "number",
          });
        }
      } else {
        const date = moment(this.form.yearMonth);
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
    startAdjustFormDateTime: function (val) {
      if (this.form.startDate) {
        return new Date(this.form.startDate + " 00:00:00").getTime();
      }
      return null;
    },
    confirmDisabled: function () {
      if (this.form.planQty < 0) {
        if (this.tableData.length) {
          return false;
        }
        return true;
      } else {
        return false;
      }
    },
  },
  watch: {
    confirmSubtractMap: {
      handler: function (val) {
        let total = 0;
        Object.values(this.confirmSubtractMap).forEach((item) => {
          total += item.needAdjustNumber;
        });

        let computeValue = Math.abs(this.form.adjustNumber) - total;
        if (this.form.planQty > 0) {
          let leftOverQty = this.form.leftOverQty || 0;
          computeValue =
            leftOverQty < computeValue ? computeValue - leftOverQty : 0;
        }
        console.log(computeValue);
        this.$set(this.form, "stillNeedQty", computeValue);
      },
      deep: true,
    },
    // "form.leftOverQty": function (val) {
    //   let total = 0;
    //   Object.values(this.confirmSubtractMap).forEach((item) => {
    //     total += item.needAdjustNumber;
    //   });

    //   let computeValue = Math.abs(this.form.adjustNumber) - total;
    //   if (this.form.planQty > 0) {
    //     computeValue = computeValue - (this.form.leftOverQty || 0);
    //   }
    //   console.log(computeValue);
    //   this.$set(this.form, "stillNeedQty", computeValue);
    // },
  },
  methods: {
    // api
    async save(params) {
      try {
        this.loading = true;
        const res = await executeAdjust(params);
        if (res.code == 301) {
          this.loading = false;
          this.check(res.msg, {
            ...params,
            isIgnoreInconsistent: 1,
          });
        } else {
          this.$modal.msgSuccess(res.msg);
          this.$emit("success");
          this.hide();
          this.loading = false;
        }
      } catch (error) {
        console.log(error);
        this.loading = false;
      }
    },
    async getMouldNo() {
      try {
        const res = await matchMouldConfiguration({
          factoryCode: this.form.factoryCode,
          productCode: this.form.productCode,
          month: this.form.month,
          year: this.form.year,
        });
        // this.form.brand = res.brand;
        if (res.mouldConfigurationList && res.mouldConfigurationList[0]) {
          let mould = res.mouldConfigurationList[0];
          this.form.mouldNo = mould.mouldNo;
          this.specCodeList = (mould.specCodeList || []).map((val) => {
            return {
              label: val,
              value: val,
            };
          });
          if (mould.specCodeList && mould.specCodeList[0]) {
            let specCode = mould.specCodeList[0];
            this.$set(this.form, "specCode", specCode);
            // this.form.specCode = specCode;
          }
        }
      } catch (error) {
        console.log(error);
      }
    },

    //utils
    adjustDisabledDate(time) {
      if (!this.startAdjustFormDateTime) {
        return true;
      }
      if (
        time < this.startAdjustFormDateTime ||
        time > this.endAdjustDateTime
      ) {
        return true;
      } else {
        return false;
      }
    },
    check(msg, params) {
      this.$confirm(msg, {
        type: "warning",
      }).then(() => {
        this.save(params);
      });
    },
    show(data) {
      this.visible = true;
      if (data) {
        this.isEdit = true;
        this.form = {
          yearMonth: `${data.year}-${data.month}`,
          ...data,
          adjustNumber: data.planQty,
          stillNeedQty: Math.abs(data.planQty),
        };
        this.adjustForm = {
          startDate: data.startDate,
        };
        this.startAdjustDateTime = new Date(
          this.form.startDate + " 00:00:00"
        ).getTime();
        this.endAdjustDateTime = new Date(
          this.form.productionEndDate + " 00:00:00"
        ).getTime();
        this.getMouldNo();
      }
    },
    hide() {
      this.form = {};
      this.currentRow = null;
      this.confirmSubtractMap = {};
      this.adjustForm = {};
      this.tableData = [];
      this.specCodeList = [];
      this.startAdjustDateTime = null;
      this.endAdjustDateTime = null;
      this.$refs.form.triggerResetForm();
      // this.resetForm("infoForm");
      this.isEdit = false;
      this.visible = false;
    },
    numberEmpty(val) {
      return this.isEmpty(val) ? undefined : val;
    },

    handleCurrentChange(row) {
      this.currentRow = row;
      if (row) {
        if (this.confirmSubtractMap[this.currentRow.productionNo]) {
          const obj = this.confirmSubtractMap[this.currentRow.productionNo];
          this.$set(this.adjustForm, "startDate", obj.startAdjustDate);
          this.$set(this.adjustForm, "adjustNumber", obj.needAdjustNumber);
        } else {
          this.$set(this.adjustForm, "startDate", this.form.startDate);
          this.$set(this.adjustForm, "adjustNumber", undefined);
        }
      }
    },
    async handleCalculateAddQty() {
      try {
        this.loading = true;
        const res = await calculateAddQty({
          adjustNumber: this.form.adjustNumber,
          specCode: this.form.specCode,
          mouldNo: this.form.mouldNo,
          noticeNo: this.form.noticeNo,
          applySubtract: {
            productionNo: this.currentRow.productionNo,
            startAdjustDate: this.adjustForm.startDate,
            needAdjustNumber: this.adjustForm.adjustNumber,
          },
        });
        // console.log(res);

        if (this.confirmSubtractMap[this.currentRow.productionNo]) {
          const obj = this.confirmSubtractMap[this.currentRow.productionNo];

          let needAdjustNumber = obj.needAdjustNumber;
          needAdjustNumber = res.addAdjustQty;

          obj.needAdjustNumber = needAdjustNumber;
          this.$set(this.confirmSubtractMap, this.currentRow.productionNo, obj);
        } else {
          this.$set(this.confirmSubtractMap, this.currentRow.productionNo, {
            productionNo: this.currentRow.productionNo,
            startAdjustDate: this.adjustForm.startDate,
            needAdjustNumber: res.addAdjustQty,
          });
        }
        const ids = this.tableData.map((item) => item.productionNo);
        const index = ids.indexOf(this.currentRow.productionNo);
        if (index !== -1) {
          this.$set(this.tableData, index, res.updateData);

          if (this.$refs.tableRef) {
            this.$refs.tableRef
              .getTableRef()
              .setCurrentRow(this.tableData[index]);
          }
        }

        this.loading = false;
      } catch (error) {
        console.log(error);
        this.loading = false;
      }
    },

    handleReduce() {
      const day = moment(this.form.startDate).format("DD");

      let adjustNumber = this.adjustForm.adjustNumber;
      let maxAdjustNumber = 0;

      // let sum = ajs;
      for (let i = day; i <= 31; i++) {
        let key = `day${i}`;

        let num = this.currentRow[key];
        if (num && !isNaN(num) && num > 0) {
          if (num > adjustNumber) {
            this.currentRow[key] = num - adjustNumber;
            adjustNumber = 0;
            maxAdjustNumber = adjustNumber;
            break;
          } else {
            adjustNumber -= num;
            maxAdjustNumber += num;
            this.currentRow[key] = 0;
          }
        }
      }

      if (this.confirmSubtractMap[this.currentRow.productionNo]) {
        const obj = this.confirmSubtractMap[this.currentRow.productionNo];

        let needAdjustNumber = obj.needAdjustNumber;
        needAdjustNumber += maxAdjustNumber;

        obj.needAdjustNumber = needAdjustNumber;
        this.$set(this.confirmSubtractMap, this.currentRow.productionNo, obj);
      } else {
        this.$set(this.confirmSubtractMap, this.currentRow.productionNo, {
          productionNo: this.currentRow.productionNo,
          startAdjustDate: this.adjustForm.startDate,
          needAdjustNumber: maxAdjustNumber,
        });
      }

      // console.log(this.confirmSubtractMap);
      // this.selection.map(() => ())
    },

    async handleOperatePlan() {
      try {
        this.loading = true;
        const res = await getOperatePlanList(this.form);
        if (res.code == 301) {
          this.$modal.msgError(res.msg);
          this.$emit("success");
          this.hide();
        } else {
          if (this.form.planQty > 0) {
            this.$set(this.form, "leftOverQty", res.leftOverQty);

            const adjustNumber = Math.abs(this.form.adjustNumber);
            this.$set(
              this.form,
              "stillNeedQty",
              adjustNumber > res.leftOverQty
                ? adjustNumber - res.leftOverQty
                : 0
            );
          }
          this.tableData = res.subtractPlanList;
        }

        this.loading = false;
      } catch (error) {
        this.loading = false;
      }
    },

    handleStartDateChange(val) {
      this.currentRow = null;
      this.confirmSubtractMap = {};
      this.$set(this.adjustForm, "startDate", val);
      // this.$set(this.form, "stillNeedQty", Math.abs(this.form.planQty));
      this.tableData = [];
    },
    handleAdjustNumber(val) {
      this.currentRow = null;
      this.confirmSubtractMap = {};
      this.tableData = [];
    },

    handleConfirm() {
      this.save({
        ...this.form,
        isIgnoreInconsistent: 0,
        confirmSubtractList: Object.values(this.confirmSubtractMap),
      });
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