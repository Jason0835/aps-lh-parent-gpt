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
import { addSteelRingStock, editSteelRingStock } from "@/api/gsq/steelRingStock";

export default {
  components: { infoForm },
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
            message: this.$t("common.rule.select"),
            trigger: "change",
          },
        ],
        steelRingCode: [
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
      },
    };
  },
  computed: {
    title: function () {
      return (
        (this.isEdit
          ? this.$t("common.button.edit")
          : this.$t("common.button.add")) +
        this.$t("ui.data.column.gsq.steelRingStock.modalName")
      );
    },
    columns() {
      return [
        {
          label: this.$t("ui.data.column.gsq.steelRingStock.stockDate"),
          prop: "stockDate",
          span: 24,
          required: true,
          type: "date",
          valueFormat: "yyyy-MM-dd",
        },
        {
          label: this.$t("ui.data.column.gsq.steelRingStock.steelRingCode"),
          prop: "steelRingCode",
          span: 24,
          required: true,
          type: "input",
          maxlength: 60,
        },
        {
          label: this.$t("ui.data.column.gsq.steelRingStock.stockNum"),
          prop: "stockNum",
          span: 24,
          required: true,
          type: "number",
          min: 0,
        },
        {
          label: this.$t("ui.data.column.gsq.steelRingStock.modifyNum"),
          prop: "modifyNum",
          span: 24,
          type: "number",
        },
        {
          label: this.$t("ui.data.column.gsq.steelRingStock.badNum"),
          prop: "badNum",
          span: 24,
          type: "number",
          min: 0,
        },
        {
          label: this.$t("ui.common.column.remark"),
          prop: "remark",
          span: 24,
          type: "textarea",
          maxlength: "900",
        },
      ];
    },
  },
  methods: {
    async save(params) {
      try {
        this.loading = true;
        const res = this.isEdit
          ? await editSteelRingStock(params)
          : await addSteelRingStock(params);
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
        this.form = {
          ...data,
        };
      } else {
        this.isEdit = false;
        this.form = {};
      }
    },
    hide() {
      this.form = {};
      this.$refs.form.triggerResetForm();
      this.isEdit = false;
      this.visible = false;
    },
    handleConfirm() {
      this.$refs.form.triggerConfirm(this.save);
    },
  },
};
</script>
