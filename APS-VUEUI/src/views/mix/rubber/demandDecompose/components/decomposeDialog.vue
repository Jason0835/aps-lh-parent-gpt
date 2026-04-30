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

import {checkPlanDateAndMixAreaExist, decompositionPlan,} from "@/api/schedule/glueDecomposePlan.js";

export default {
  components: { infoForm },
  inject: ["parentDict"],
  data() {
    return {
      loading: false,
      visible: false,
      isEdit: false,
      editType: null,
      form: {
        planDate: moment().add(1, "days").format("yyyy-MM-DD"),
      },
      rules: {
        planDate: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
        mixArea: [
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
      return (
        this.$t("schedule.glueDecomposePlan.modelName") +
        this.$t("schedule.glueDecomposePlan.btn.decompositionPlan")
      );
    },
    columns: function () {
      return [
        {
          label: this.$t("schedule.glueCollectPlan.planDate"),
          prop: "planDate",
          span: 24,
          type: "date",
          valueFormat: "yyyy-MM-dd",
        },
        {
          label: this.$t("schedule.glueDecomposePlan.mixArea"),
          prop: "mixArea",
          span: 24,
          type: "select",
          dictData: this.parentDict.type.MIX_AREA,
        },
      ];
    },
  },
  methods: {
    // api
    async save(params) {
      try {
        this.loading = true;
        let valid = await checkPlanDateAndMixAreaExist(params);
        if (valid.msg == "0") {
          let result = await decompositionPlan(params);
          this.loading = false;
          if (result.code == 200) {
            this.$emit("success");
            this.hide();
          }
        } else if (valid.msg == "1") {
          this.$confirm(
            this.$t("schedule.glueDecomposePlan.database.exist.tip")
          )
            .then(async () => {
              let result = await decompositionPlan(params);
              this.loading = false;
              if (result.code == 200) {
                this.$emit("success");
                this.hide();
              }
            })
            .catch(() => {
              this.loading = false;
            });
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
      // if (data) {
      //   this.isEdit = true;
      //   this.form = {
      //     ...data,
      //   };
      // }
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
