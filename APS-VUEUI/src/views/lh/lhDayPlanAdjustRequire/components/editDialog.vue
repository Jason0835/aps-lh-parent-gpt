<template>
  <el-dialog
    :title="$t('ui.data.column.lhDayPlanAdjustRequire.editTitle')"
    :visible.sync="visible"
    width="1200px"
    append-to-body
    :close-on-click-modal="false"
    @close="hide"
  >
    <el-form v-loading="loading" :model="form" label-width="110px">
      <el-row :gutter="16">
        <el-col :span="8">
          <el-form-item :label="$t('ui.data.column.lhDayPlanAdjustRequire.factoryCode')">
            <el-input :value="factoryName" disabled />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item :label="$t('ui.data.column.lhDayPlanAdjustRequire.yearMonth')">
            <el-input :value="formatYearMonth(form.yearMonth)" disabled />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item :label="$t('ui.data.column.lhDayPlanAdjustRequire.productStatus')">
            <el-input :value="productStatusName" disabled />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="16">
        <el-col :span="8">
          <el-form-item :label="$t('ui.data.column.lhDayPlanAdjustRequire.materialCode')">
            <el-input v-model="form.materialCode" disabled />
          </el-form-item>
        </el-col>
        <el-col :span="16">
          <el-form-item :label="$t('ui.data.column.lhDayPlanAdjustRequire.materialDesc')">
            <el-input v-model="form.materialDesc" disabled />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="16">
        <el-col :span="24">
          <el-form-item :label="$t('ui.data.column.lhDayPlanAdjustRequire.treadGlueTd')">
            <el-input v-model="form.treadGlueTd" disabled />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="16">
        <el-col :span="8">
          <el-form-item :label="$t('ui.data.column.lhDayPlanAdjustRequire.monthPlanQty')">
            <el-input :value="formatQty(form.monthPlanQty)" disabled />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item :label="$t('ui.data.column.lhDayPlanAdjustRequire.adjustedTotalQty')">
            <el-input :value="formatQty(adjustedTotalQty)" disabled />
          </el-form-item>
        </el-col>
      </el-row>

      <el-table :data="adjustRows" border size="small" style="width:100%">
        <el-table-column
          :label="$t('ui.data.column.lhDayPlanAdjustRequire.adjustGroup')"
          width="120"
          align="center"
        >
          <template slot-scope="scope">
            {{ $t('ui.data.column.lhDayPlanAdjustRequire.adjust', [scope.row.adjustIndex]) }}
          </template>
        </el-table-column>
        <el-table-column
          :label="$t('ui.data.column.lhDayPlanAdjustRequire.adjustQty')"
          width="190"
          align="center"
        >
          <template slot-scope="scope">
            <el-input-number
              v-model="scope.row.adjustQty"
              :controls="false"
              :precision="0"
              :min="-99999999"
              :max="99999999"
              style="width:150px"
            />
          </template>
        </el-table-column>
        <el-table-column :label="$t('ui.data.column.lhDayPlanAdjustRequire.adjustReason')" min-width="400">
          <template slot-scope="scope">
            <el-input
              v-model="scope.row.adjustReason"
              type="textarea"
              :rows="2"
              maxlength="2000"
              show-word-limit
            />
          </template>
        </el-table-column>
        <el-table-column
          prop="adjuster"
          :label="$t('ui.data.column.lhDayPlanAdjustRequire.adjuster')"
          width="130"
        />
        <el-table-column
          :label="$t('ui.data.column.lhDayPlanAdjustRequire.adjustTime')"
          width="180"
        >
          <template slot-scope="scope">
            {{ formatAdjustTime(scope.row.adjustTime) }}
          </template>
        </el-table-column>
      </el-table>
    </el-form>
    <template slot="footer">
      <el-button @click="hide">{{ $t('common.button.cancel') }}</el-button>
      <el-button type="primary" :loading="loading" @click="handleConfirm">
        {{ $t('common.button.confirm') }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script>
import moment from "moment";
import { saveLhDayPlanAdjustRequire } from "@/api/lh/lhDayPlanAdjustRequire";

const ADJUST_SLOT_COUNT = 3;

const DEFAULT_FORM = () => ({
  factoryCode: "",
  yearMonth: null,
  materialCode: "",
  materialDesc: "",
  monthPlanQty: null,
  productStatus: "",
  treadGlueTd: "",
});

export default {
  name: "LhDayPlanAdjustRequireEditDialog",
  inject: ["parentDict"],
  data() {
    return {
      visible: false,
      loading: false,
      form: DEFAULT_FORM(),
      adjustRows: [],
    };
  },
  computed: {
    factoryName() {
      return this.selectDictLabel(
        this.parentDict.type.biz_factory_name,
        this.form.factoryCode
      );
    },
    productStatusName() {
      return this.selectDictLabel(
        this.parentDict.type.lh_trial_status,
        this.form.productStatus
      );
    },
    adjustedTotalQty() {
      return this.adjustRows.reduce(
        (totalQty, row) => totalQty + Number(row.adjustQty || 0),
        Number(this.form.monthPlanQty || 0)
      );
    },
  },
  methods: {
    show(row) {
      this.form = { ...DEFAULT_FORM(), ...(row || {}) };
      this.adjustRows = Array.from({ length: ADJUST_SLOT_COUNT }, (item, index) => {
        const adjustIndex = index + 1;
        const adjustQty = this.form[`adjustQty${adjustIndex}`];
        return {
          adjustIndex,
          adjustId: this.form[`adjustId${adjustIndex}`],
          adjustQty: adjustQty === null ? undefined : adjustQty,
          adjustReason: this.form[`adjustReason${adjustIndex}`] || "",
          adjuster: this.form[`adjuster${adjustIndex}`] || "",
          adjustTime: this.form[`adjustTime${adjustIndex}`] || "",
        };
      });
      this.visible = true;
    },
    hide() {
      this.visible = false;
      this.loading = false;
      this.form = DEFAULT_FORM();
      this.adjustRows = [];
    },
    validateAdjustRows() {
      for (const row of this.adjustRows) {
        const adjustReason = (row.adjustReason || "").trim();
        if ((row.adjustQty === null || row.adjustQty === undefined) && adjustReason) {
          this.$modal.msgError(
            this.$t("ui.data.alert.lhDayPlanAdjustRequire.adjustQtyRequired", [
              this.form.materialCode,
              row.adjustIndex,
            ])
          );
          return false;
        }
        if (row.adjustQty !== null && row.adjustQty !== undefined && !adjustReason) {
          this.$modal.msgError(
            this.$t("ui.data.alert.lhDayPlanAdjustRequire.adjustReasonRequired", [
              this.form.materialCode,
              row.adjustIndex,
            ])
          );
          return false;
        }
      }
      return true;
    },
    buildRequest() {
      const request = {
        factoryCode: this.form.factoryCode,
        yearMonth: this.form.yearMonth,
        materialCode: this.form.materialCode,
        productStatus: this.form.productStatus,
      };
      this.adjustRows.forEach((row) => {
        request[`adjustId${row.adjustIndex}`] = row.adjustId;
        request[`adjustQty${row.adjustIndex}`] = row.adjustQty;
        request[`adjustReason${row.adjustIndex}`] = row.adjustReason;
      });
      return request;
    },
    async handleConfirm() {
      if (!this.validateAdjustRows()) {
        return;
      }
      this.loading = true;
      try {
        const result = await saveLhDayPlanAdjustRequire(this.buildRequest());
        this.$modal.msgSuccess(result.msg);
        this.$emit("success");
        this.hide();
      } finally {
        this.loading = false;
      }
    },
    formatYearMonth(value) {
      const yearMonth = String(value || "");
      return yearMonth.length === 6
        ? `${yearMonth.slice(0, 4)}-${yearMonth.slice(4)}`
        : yearMonth;
    },
    formatQty(value) {
      return value === null || value === undefined || value === "" ? "" : Number(value);
    },
    formatAdjustTime(value) {
      return value ? moment(value).format("YYYY-MM-DD HH:mm:ss") : "";
    },
  },
};
</script>
