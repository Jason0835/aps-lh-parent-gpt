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
      :form="form"
      :rules="rules"
      label-position="top"
      label-width="80px"
    >
      <el-form-item
        :label="$t('ui.data.column.scheduleResult.produceLine')"
        prop="machineId"
      >
        <el-select
          class="w100"
          :placeholder="$t('ui.data.column.selectMachineName')"
        >
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

import { batchChangeMachine } from "@/api/schedule/glueScheduleResult.js";

export default {
  components: { infoForm },
  data() {
    return {
      loading: false,
      visible: false,
      isEdit: false,
      editType: null,
      form: {},
      rules: {
        machineId: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
      },
      columns: [],
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

        const result = await batchChangeMachine(params);
        if (result.code == 200) {
          this.$modal.msgSuccess("操作成功");
          this.$emit("success");
          this.hide();
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
      if (data) {
        this.isEdit = true;
        this.form = {
          ...data,
        };
      }
    },
    hide() {
      this.form = {};
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
