<template>
  <el-dialog
    :title="title"
    :visible="visible"
    width="600px"
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
      label-width="150px"
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
import infoForm from "@/views/components/infoForm.vue";
import { saveMaterBatch } from "@/api/setting/mlstock";

export default {
  components: { infoForm },
  inject: ["parentDict"],
  data() {
    return {
      loading: false,
      visible: false,
      isEdit: false,
      form: {},
      rules: {
        stockDate: [
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
        glue: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
        stockNum: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
        stockWeight: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
      },
      columns: [
        {
          label: this.$t("setting.mlstock.stockDate"),
          prop: "stockDate",
          type: "date",
          valueFormat: "yyyy-MM-dd",
          disabled: true,
        },
        // {
        //   label: this.$t("setting.mlstock.barCode"),
        //   prop: "barCode",
        //   maxlength: "60",
        //   required: true,
        // },
        // {
        //   label: this.$t("setting.mlstock.validTime"),
        //   prop: "validTime",
        //   type: "date",
        //   valueFormat: "yyyy-MM-dd HH:mm:ss",
        // },
        {
          label: this.$t("setting.mlstock.mixArea"),
          prop: "mixArea",
          maxlength: "20",
          required: true,
          type: "select",
          dictData: this.parentDict.type.MIX_AREA,
          disabled: true,
        },
        {
          label: this.$t("setting.mlstock.glue"),
          prop: "glue",
          maxlength: "30",
          required: true,
          disabled: true,
        },
        {
          label: this.$t("setting.mlstock.stockNum"),
          prop: "stockNum",
          required: true,
          type: "number",
          min: 0,
          max: 9999999,
          disabled: true,
        },
        {
          label: this.$t("setting.mlstock.stockWeightNum"),
          prop: "stockWeight",
          required: true,
          type: "number",
          min: 0,
          max: 9999999,
          disabled: true,
        },
        {
          label: this.$t("setting.mlstock.safeStock"),
          prop: "safeStock",
          required: true,
          type: "number",
          min: 0,
          max: 9999999,
          disabled: false,
        },

        {
          label: this.$t("ui.common.column.remark"),
          prop: "remark",
          type: "textarea",
          maxlength: "300",
          disabled: true,
        },
      ],
    };
  },
  computed: {
    title: function () {
      return this.$t("setting.mlstock.modelName");
    },
  },
  methods: {
    // api
    async save(params) {
      // console.log(params);
      try {
        this.loading = true;
        const data = await saveMaterBatch(params);
        this.$modal.msgSuccess(
          this.$t("common.msg.ajax.operation.success")
        );
        this.$emit("success");
        this.hide();
      } catch (error) {
        console.log(error);
      } finally {
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
          safeStock:
            data.safeStock === "" || data.safeStock === null
              ? undefined
              : data.safeStock,
        };
      } else {
        this.form = {};
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
      this.$refs.form.triggerConfirm((params) => {
        this.save({
          ...params,
          safeStock: params.safeStock === undefined ? "" : params.safeStock,
        });
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
