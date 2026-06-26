<template>
  <el-dialog
    :title="$t('ui.data.column.scheduleResult.changePlan')"
    :visible="visible"
    width="700px"
    @close="hide"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    :append-to-body="true"
  >
    <el-form ref="form" :model="form" label-width="120px">
      <el-form-item :label="$t('ui.data.column.gdyyScheduleResult.bigRollCode')">
        <el-input :value="form.bigRollCode" disabled />
      </el-form-item>
      <el-row :gutter="20">
        <el-col :span="8">
          <el-form-item label="CLASS1">
            <el-input-number v-model="form.class1PlanQty" :min="0" :precision="2" controls-position="right" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="CLASS2">
            <el-input-number v-model="form.class2PlanQty" :min="0" :precision="2" controls-position="right" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="CLASS3">
            <el-input-number v-model="form.class3PlanQty" :min="0" :precision="2" controls-position="right" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="20">
        <el-col :span="8">
          <el-form-item label="CLASS4">
            <el-input-number v-model="form.class4PlanQty" :min="0" :precision="2" controls-position="right" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="CLASS5">
            <el-input-number v-model="form.class5PlanQty" :min="0" :precision="2" controls-position="right" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="CLASS6">
            <el-input-number v-model="form.class6PlanQty" :min="0" :precision="2" controls-position="right" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="20">
        <el-col :span="8">
          <el-form-item label="CLASS7">
            <el-input-number v-model="form.class7PlanQty" :min="0" :precision="2" controls-position="right" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="CLASS8">
            <el-input-number v-model="form.class8PlanQty" :min="0" :precision="2" controls-position="right" />
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>
    <template slot="footer">
      <el-button @click="hide">{{ $t("common.button.cancel") }}</el-button>
      <el-button type="primary" :loading="loading" @click="handleConfirm">{{ $t("common.button.confirm") }}</el-button>
    </template>
  </el-dialog>
</template>

<script>
import { changeQtyGdyyScheduleResult } from "@/api/gdyy/gdyyScheduleResult";

export default {
  data() {
    return {
      loading: false,
      visible: false,
      form: {},
    };
  },
  methods: {
    show(data) {
      this.visible = true;
      this.form = { ...data };
    },
    hide() {
      this.visible = false;
      this.form = {};
    },
    handleConfirm() {
      this.loading = true;
      changeQtyGdyyScheduleResult(this.form)
        .then((res) => {
          this.$modal.msgSuccess(res.msg);
          this.$emit("success");
          this.hide();
        })
        .finally(() => {
          this.loading = false;
        });
    },
    openDialog(data) {
      this.show(data);
    },
  },
};
</script>
