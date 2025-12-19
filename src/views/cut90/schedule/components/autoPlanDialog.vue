<template>
  <el-dialog
    :title="title"
    :visible="visible"
    width="400px"
    @close="hide"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
  >
    <info-form
      class="form-item-height"
      ref="form"
      :form="form"
      :rules="rules"
      :columns="columns"
      label-position="right"
      label-width="80px"
      v-loading="loading"
    >
    </info-form>
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

import { autoPlan, validateAutoPlan } from "@/api/cd90/scheduleResult";

export default {
  components: { infoForm },
  data() {
    return {
      loading: false,
      visible: false,
      isEdit: false,
      editType: null,
      form: {
        scheduleDate: moment().add(1, "days").format("yyyy-MM-DD"),
      },
      rules: {
        scheduleDate: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "blur",
          },
        ],
      },
    };
  },
  computed: {
    title: function () {
      return this.$t("ui.data.column.cd90ScheduleResult.modalName");
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
        let valid = await validateAutoPlan(params);
        if (valid.msg == "0") {
          this.$confirm(
            this.$t("ui.biz.alter.makeSureRecreateByPublished")
          ).then(async () => {
            let result = await autoPlan(params);
            this.loading = false;
            if (result.code == 200) {
              this.$modal.msgSuccess(result.msg);
              this.$emit("success");
              this.hide();
            }
          });
        } else if (valid.msg == "1") {
          this.$confirm(this.$t("ui.biz.alter.makeSureRecreate")).then(
            async () => {
              let result = await autoPlan(params);
              this.loading = false;
              if (result.code == 200) {
                this.$modal.msgSuccess(result.msg);
                this.$emit("success");
                this.hide();
              }
            }
          );
        } else if (valid.msg == "2") {
          let result = await autoPlan(params);
          this.loading = false;
          if (result.code == 200) {
            this.$modal.msgSuccess(result.msg);
            this.$emit("success");
            this.hide();
          }
        } else if (valid.msg == "3") {
          this.loading = false;
          this.$modal.warning(this.$t("ui.biz.alter.CanNotRecreate"));
        } else {
          this.loading = false;
          this.$modal.warning(result.msg);
        }
      } catch (error) {
        console.error(error);
        this.loading = false;
      }
    },

    //utils
    show(data, editType) {
      this.visible = true;
      if (data) {
        this.isEdit = true;
        this.form = {
          ...data,
        };
      } else {
        this.form = {
          scheduleDate: moment().add(1, "days").format("yyyy-MM-DD"),
        }
      }
    },
    hide() {
      this.form = {};
      this.$refs.form.triggerResetForm();
      // this.resetForm("infoForm");
      this.isEdit = false;
      this.visible = false;
    },

    handleConfirm() {
      this.$refs.form.triggerConfirm(this.save);
      // this.$refs.form.validate((valid) => {
      //   if (valid) {
      //     this.save({
      //       ...this.form,
      //     });
      //   }
      // });
    },
  },
};
</script>
