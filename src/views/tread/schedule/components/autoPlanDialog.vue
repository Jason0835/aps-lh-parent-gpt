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

import { autoPlan, validateAutoPlan } from "@/api/tm/tmScheduleResult";

export default {
  components: { infoForm },
  data() {
    const iniDate = moment().add(1, "days").format("yyyy-MM-DD");
    return {
      loading: false,
      visible: false,
      isEdit: false,
      editType: null,
      initDate: iniDate,
      form: {
        scheduleDate: iniDate,
      },
      rules: {
        scheduleDate: [
        {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "blur",
          },
        ]
      },
    };
  },
  computed: {
    title: function () {
      return this.$t("ui.data.column.scheduleResult.autoPlan");
    },
    columns () {
      return [
        {
          label: this.$t("ui.data.column.scheduleResult.scheduleDate"),
          prop: "scheduleDate",
          span: 24,
          type: "date",
          valueFormat: "yyyy-MM-dd",
          pickerOptions: {
            // disabledDate:(time) => {
            //   return moment(time).isBefore(this.initDate, "day");
            // },
          },
        },
      ];
    },
  },
  methods: {
    // api
    async save(params) {
      // console.log(params);
      try {
        this.loading = true;
        const result = await validateAutoPlan(params);
        if (result.msg == "1") {
          //已经生成，提示是否重新生成
          this.$confirm(this.$t("ui.biz.alter.makeSureRecreate"))
            .then(async () => {
              try {
                const data = await autoPlan(params);
                this.$modal.msgSuccess(data.msg);
                this.$emit("success", params);
              } catch (error) {
                console.error(error);
              }
            })
            .catch(() => {
              this.loading = false;
            });
        } else if (result.msg == "2") {
          //未生成，直接生成
          const data = await autoPlan(params);
          this.$modal.msgSuccess(data.msg);
          this.$emit("success", params);
        } else if (result.msg == "3") {
          //已发布，提示不能重新生成
          this.$modal.msgError(this.$t("ui.biz.alter.CanNotRecreate"));
        }

        this.hide();
      } catch (error) {
        console.log(error);
      } finally {
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
    },
  },
};
</script>
