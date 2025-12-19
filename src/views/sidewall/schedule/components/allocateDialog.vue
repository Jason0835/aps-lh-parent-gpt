<template>
  <el-dialog
    :title="title"
    :visible="visible"
    width="400px"
    @close="hide"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
  >
    <t-form ref="form" :model="form">
      <el-form-item
        :label="$t('ui.data.column.combinationMiddleAndNight.shift')"
        prop="classifiedShift"
      >
        <el-radio-group v-model="form.classifiedShift">
          <el-radio label="1">{{
            $t("ui.data.column.combinationMiddleAndNight.nightShift")
          }}</el-radio>
          <el-radio label="2">{{
            $t("ui.data.column.combinationMiddleAndNight.middleShift")
          }}</el-radio>
        </el-radio-group>
        <div v-if="tipVisible" style="color: red" class="col-sm-4">
          历史排程记录不允许进行“中夜班归并”操作
        </div>
      </el-form-item>
    </t-form>
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

import { combinationMiddleAndNight } from "@/api/tc/tcScheduleResult.js";

export default {
  components: { infoForm },
  props: {
    scheduleDate: {
      type: String,
      require: true,
    },
  },
  data() {
    return {
      loading: false,
      visible: false,
      isEdit: false,
      tipVisible: false,
      form: {},
      rules: {},
    };
  },
  computed: {
    title: function () {
      return this.$t("ui.data.column.combinationMiddleAndNight");
    },
    columns: function () {
      return [
        {
          label: this.$t("ui.data.column.scheduleResult.scheduleDate"),
          prop: "scheduleDate",
          span: 24,
          type: "date",
          valueFormat: "yyyy-MM-dd",
        },
      ];
    },
  },
  methods: {
    // api
    async save(params) {
      try {
        this.loading = true;
        let res = await combinationMiddleAndNight(params);
        this.$modal.msgSuccess(res.msg);
        this.$emit("success");
        this.hide();
        this.loading = false;
      } catch (error) {
        console.error(error);
        this.loading = false;
      }
    },

    //utils
    show(ids) {
      this.visible = true;
      this.isEdit = true;
      this.ids = ids;
    },
    hide() {
      // this.form = {};
      // this.$refs.form.triggerResetForm();
      this.resetForm("form");
      this.isEdit = false;
      this.visible = false;
      this.tipVisible = false;
    },

    handleConfirm() {
      var scheduleTimeStr = this.scheduleDate.replace(/-/g, "/");
      var scheduleTime = new Date(scheduleTimeStr);
      if (new Date() > scheduleTime) {
        this.tipVisible = true;
        return;
      }

      this.$refs.form.validate((valid) => {
        if (valid) {
          this.save({
            ids: this.ids,
            ...this.form,
          });
        }
      });
    },
  },
};
</script>
