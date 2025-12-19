<template>
  <el-dialog
    :title="title"
    :visible="visible"
    width="800px"
    @close="hide"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    :append-to-body="true"
  >
    <info-form
      class="form-item-height"
      ref="form"
      :form="form"
      :rules="rules"
      :columns="columns"
      label-position="right"
      label-width="160px"
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
import { mapState } from "vuex";

import infoForm from "@/views/components/infoForm.vue";

import { validateAdd, editScheduleResult } from "@/api/schedule/glueScheduleResult";

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
        // specName: [
        //   {
        //     required: true,
        //     message: this.$t("common.rule.input"),
        //     trigger: "blur",
        //   },
        // ],
      },
    
    };
  },
  computed: {
    ...mapState({
      machines: (state) => state.mix.machines,
    }),
    title: function () {
      return this.$t("schedule.materialScheduleResult.modelName");
    },
    columns(){return [
        {
          label: this.$t("ui.data.column.scheduleResult.scheduleDate"),
          prop: "scheduleDate",
          span: 24,
          type: "date",
          valueFormat: "yyyy-MM-dd",
        },
        {
          label: this.$t("schedule.materialScheduleResult.mixArea"),
          prop: "treadCode",
          span: 24,
          maxlength: "20",
          listeners: {
            blur: this.toUpperCase,
          },
        },
        {
          label: this.$t("schedule.materialScheduleResult.materialName"),
          prop: "materialName",
          span: 24,
          type: "select",
        },
        {
          label: this.$t("schedule.materialScheduleResult.machineName"),
          prop: "machineCode",
          span: 24,
        },
        {
          label: this.$t("schedule.materialScheduleResult.recipeTypeName"),
          prop: "recipeTypeName",
          span: 24,
          maxlength: "100",
        },
        {
          label: this.$t("schedule.materialScheduleResult.recipeVersionId"),
          prop: "recipeVersionId",
          span: 24,
        },
        {
          label: this.$t("schedule.materialScheduleResult.recipeStage"),
          prop: "recipeStage",
          span: 24,
          maxlength: "100",
        },
        {
          label: this.$t("schedule.materialScheduleResult.midPlanQty"),
          prop: "midPlanQty",
          span: 24,
          maxlength: "100",
        },
        {
          label: this.$t("schedule.materialScheduleResult.midProduceOrder"),
          prop: "midProduceOrder",
          span: 24,
          maxlength: "100",
        },
        {
          label: this.$t("schedule.materialScheduleResult.midRemark"),
          prop: "midRemark",
          span: 24,
          maxlength: "100",
        },
        {
          label: this.$t("schedule.materialScheduleResult.nightPlanQty"),
          prop: "nightPlanQty",
          span: 24,
          maxlength: "100",
        },
        {
          label: this.$t("schedule.materialScheduleResult.nightProduceOrder"),
          prop: "nightProduceOrder",
          span: 24,
          maxlength: "100",
        },
        {
          label: this.$t("schedule.materialScheduleResult.nightRemark"),
          prop: "nightRemark",
          span: 24,
          maxlength: "100",
        },
        {
          label: this.$t("schedule.materialScheduleResult.dayPlanQty"),
          prop: "dayPlanQty",
          span: 24,
          maxlength: "100",
        },

        {
          label: this.$t("schedule.materialScheduleResult.dayProduceOrder"),
          prop: "dayProduceOrder",
          span: 24,
          maxlength: "100",
        },
                {
          label: this.$t("schedule.materialScheduleResult.dayRemark"),
          prop: "dayRemark",
          span: 24,
          maxlength: "100",
        },
        {
          label: this.$t("ui.common.column.remark"),
          prop: "class1PlanQty",
          span: 24,
          type: "textarea",
        },
      ]},
  },
  methods: {
    // api
    async save(params) {
      try {
        this.loading = true;
        let valid = await validateAdd(params);
        if (valid.msg == "0") {
          this.$confirm(
            this.$t("ui.data.column.scheduleResult.isContinueAdd")
          ).then(async () => {
            let result = await save(params);
            this.loading = false;
            if (result.code == 200) {
              this.$modal.msgSuccess(result.msg);
              this.$emit("success");
              this.hide();
            }
          });
        } else {
          let result = await save(params);
          this.loading = false;
          if (result.code == 200) {
            this.$modal.msgSuccess(result.msg);
            this.$emit("success");
            this.hide();
          }
          this.loading = false;
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
      }
    },
    hide() {
      this.form = {};
      this.$refs.form.triggerResetForm();
      // this.resetForm("infoForm");
      this.isEdit = false;
      this.visible = false;
    },
    toUpperCase(e) {
      let value = e.target.value;
      if (value.length) {
        this.form.treadCode = value.toUpperCase();
      }
    },

    handleConfirm() {
      this.$refs.form.triggerConfirm((params) => {
        this.save(params);
      });
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
