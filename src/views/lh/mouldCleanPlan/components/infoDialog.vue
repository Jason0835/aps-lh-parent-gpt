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
          <el-form-item :label="$t('ui.data.column.factoryCode')" prop="factoryCode">
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
          <el-form-item :label="$t('ui.data.column.mouldCleanPlan.brand')" prop="brand">
            <el-select
              v-model="form.brand"
              :placeholder="$t('common.rule.select')"
              clearable
              style="width: 100%"
            >
              <el-option
                v-for="item in parentDict.type.biz_brand_type"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item :label="$t('ui.data.column.mouldCleanPlan.lhCode')" prop="lhCode">
            <el-input v-model="form.lhCode" :placeholder="$t('common.rule.input')" clearable />
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item :label="$t('ui.data.column.mouldCleanPlan.operTime')" prop="operTime">
            <el-date-picker
              v-model="form.operTime"
              type="date"
              placeholder="选择日期"
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
          <el-form-item :label="$t('ui.data.column.mouldCleanPlan.firstWashTime')" prop="firstWashTime">
            <el-date-picker
              v-model="form.firstWashTime"
              type="date"
              placeholder="选择日期"
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
          <el-form-item :label="$t('ui.data.column.mouldCleanPlan.secondWashTime')" prop="secondWashTime">
            <el-date-picker
              v-model="form.secondWashTime"
              type="date"
              placeholder="选择日期"
              value-format="yyyy-MM-dd"
              style="width: 100%"
              clearable
              popper-class="el-popper"
              editable
            >
            </el-date-picker>
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
import { editMouldCleanPlan } from "@/api/lh/mouldCleanPlan";

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
        brand: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "change",
          },
        ],
        lhCode: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
      },
    };
  },
  computed: {
    title: function () {
      return this.$t("ui.data.column.mouldCleanPlan.modelName");
    },
  },
  methods: {
    async save() {
      try {
        this.loading = true;
        const res = await editMouldCleanPlan(this.form);
        this.$modal.msgSuccess(res.msg);
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
        this.form = {};
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
