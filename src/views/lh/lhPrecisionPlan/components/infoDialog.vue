<template>
  <el-dialog
    :title="title"
    :visible="visible"
    width="600px"
    @close="hide"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    :append-to-body="true"
  >
    <el-form
      ref="form"
      :model="form"
      :rules="rules"
      label-position="right"
      label-width="120px"
      v-loading="loading"
    >
      <el-row>
        <el-col :span="24">
          <el-form-item :label="$t('common.factory')" prop="factoryCode">
            <el-select
              v-model="form.factoryCode"
              :placeholder="$t('common.rule.select')"
              clearable
              style="width: 100%"
            >
              <el-option
                v-for="item in parentDict.type.biz_factory_name"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item :label="$t('ui.lh.precision.plan.machine.code')" prop="machineCode">
            <el-select
              v-model="form.machineCode"
              :placeholder="$t('common.rule.select')"
              filterable
              clearable
              style="width: 100%"
            >
              <el-option
                v-for="item in parentDict.type.lh_machine"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item :label="$t('ui.lh.precision.plan.precision.type')" prop="precisionType">
            <el-select
              v-model="form.precisionType"
              :placeholder="$t('common.rule.select')"
              clearable
              style="width: 100%"
            >
              <el-option
                v-for="item in parentDict.type.lh_precision_type"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item :label="$t('ui.lh.precision.plan.plan.date')" prop="planDate">
            <el-date-picker
              v-model="form.planDate"
              type="date"
              :placeholder="$t('common.rule.select')"
              value-format="yyyy-MM-dd"
              style="width: 100%"
              clearable
              popper-class="el-popper"
              editable
            >
            </el-date-picker>
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item :label="$t('ui.lh.precision.plan.actual.date')" prop="actualDate">
            <el-date-picker
              v-model="form.actualDate"
              type="date"
              :placeholder="$t('common.rule.select')"
              value-format="yyyy-MM-dd"
              style="width: 100%"
              clearable
              popper-class="el-popper"
              editable
            >
            </el-date-picker>
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item :label="$t('ui.lh.precision.plan.due.date')" prop="dueDate">
            <el-date-picker
              v-model="form.dueDate"
              type="date"
              :placeholder="$t('common.rule.select')"
              value-format="yyyy-MM-dd"
              style="width: 100%"
              clearable
              popper-class="el-popper"
              editable
            >
            </el-date-picker>
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item :label="$t('ui.lh.precision.plan.last.maintenance.date')" prop="lastMaintenanceDate">
            <el-date-picker
              v-model="form.lastMaintenanceDate"
              type="date"
              :placeholder="$t('common.rule.select')"
              value-format="yyyy-MM-dd"
              style="width: 100%"
              clearable
              popper-class="el-popper"
              editable
            >
            </el-date-picker>
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item :label="$t('ui.lh.precision.plan.completion.status')" prop="completionStatus">
            <el-select
              v-model="form.completionStatus"
              :placeholder="$t('common.rule.select')"
              clearable
              style="width: 100%"
            >
              <el-option
                v-for="item in parentDict.type.lh_completion_status"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item :label="$t('ui.lh.precision.plan.year')" prop="year">
            <el-input
              v-model="form.year"
              :placeholder="$t('common.rule.input')"
              clearable
            />
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item :label="$t('ui.lh.precision.plan.data.source')" prop="dataSource">
            <el-select
              v-model="form.dataSource"
              :placeholder="$t('common.rule.select')"
              clearable
              style="width: 100%"
            >
              <el-option
                v-for="item in parentDict.type.lh_precision_data_source"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>
    <template slot="footer">
      <el-button @click="hide">{{ $t("common.button.cancel") }}</el-button>
      <el-button type="primary" :loading="loading" @click="handleConfirm">{{
        $t("common.button.confirm")
      }}</el-button>
    </template>
  </el-dialog>
</template>

<script>
import { saveLhPrecisionPlan } from "@/api/lh/lhPrecisionPlan";

export default {
  inject: ["parentDict"],
  data() {
    return {
      loading: false,
      visible: false,
      isEdit: false,
      form: {},
      rules: {
        factoryCode: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "change",
          },
        ],
        machineCode: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "change",
          },
        ],
        precisionType: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "change",
          },
        ],
        planDate: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "change",
          },
        ],
      },
    };
  },
  computed: {
    title: function () {
      return this.isEdit
        ? this.$t("common.button.edit")
        : this.$t("common.button.add");
    },
  },
  methods: {
    async save() {
      try {
        this.loading = true;
        const res = await saveLhPrecisionPlan(this.form);
        this.$modal.msgSuccess(res.msg || this.$t("common.success"));
        this.$emit("success");
        this.hide();
      } catch (error) {
        console.log(error);
      } finally {
        this.loading = false;
      }
    },
    show(data) {
      this.visible = true;
      if (data) {
        this.isEdit = true;
        this.form = { ...data };
      } else {
        this.form = {
          factoryCode: "116",
        };
      }
    },
    hide() {
      this.form = {};
      if (this.$refs.form) {
        this.$refs.form.resetFields();
      }
      this.isEdit = false;
      this.visible = false;
    },
    handleConfirm() {
      this.$refs.form.validate((valid) => {
        if (valid) {
          this.save();
        }
      });
    },
  },
};
</script>
