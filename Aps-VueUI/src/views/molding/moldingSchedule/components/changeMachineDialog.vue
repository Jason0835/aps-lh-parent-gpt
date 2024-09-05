<template>
  <el-dialog
    :title="title"
    :visible="visible"
    width="600px"
    @close="hide"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
  >
    <el-form
      class="form-item-height"
      ref="form"
      :defaultValue="defaultValue"
      :rules="rules"
      label-position="top"
      label-width="80px"
    >
      <el-form-item
        :label="$t('ui.data.column.cxScheduleResult.cxMachineCode')"
        prop="cxMachineCode"
      >
        <el-select class="w100">
          <el-option-group key="定点机台" label="定点机台"> </el-option-group>
          <el-option-group key="其他机台" label="其他机台"></el-option-group>
        </el-select>
      </el-form-item>
    </el-form>
    <template slot="footer">
      <el-button @click="hide">{{ this.$t("common.button.cancel") }}</el-button>
      <el-button type="primary" :loading="loading" @click="handleConfirm">{{
        this.$t("common.button.confirm")
      }}</el-button>
    </template>
  </el-dialog>
</template>

<script>
import moment from "moment";

import infoForm from "@/views/components/infoForm.vue";

import {
  validateChangeMachine,
  cxScheduleResultEdit,
} from "@/api/cx/cxScheduleResult";

export default {
  components: { infoForm },
  data() {
    return {
      loading: false,
      visible: false,
      isEdit: false,
      editType: null,
      defaultValue: {
        scheduleDate: moment().add(1, "days").format("yyyy-MM-DD"),
      },
      rules: {
        // specName: [
        //   {
        //     required: true,
        //     message: this.$t("common.rule.input"),
        //     trigger: "blur",
        //   },
        // ],
      },
      columns: [
        {
          label: this.$t("ui.data.column.cxScheduleResult.cxMachineCode"),
          prop: "cxMachineCode",
        },
      ],
    };
  },
  computed: {
    title: function () {
      return this.$t("ui.data.column.cxScheduleResult.modalName");
    },
  },
  methods: {
    // api
    async save(params) {
      try {
        this.loading = true;
        let valid = await validateChangeMachine(params);
        if (valid) {
          const result = await cxScheduleResultEdit();
          if (result.code == 200) {
            this.$emit("success");
            this.hide();
          }
        }
        this.loading = false;
      } catch (error) {
        console.error(error);
        this.loading = false;
      }
    },

    //utils
    show(data) {
      this.visible = true;
      // if (data) {
      //   this.isEdit = true;
      //   this.defaultValue = {
      //     ...data,
      //   };
      // }
    },
    hide() {
      this.defaultValue = {};
      this.$refs.form.resetForm();
      this.isEdit = false;
      this.visible = false;
    },

    handleConfirm() {
      this.$refs.form.validate((valid) => {
        if (valid) {
          this.save({
            ...this.form,
          });
        }
      });
    },
  },
};
</script>
