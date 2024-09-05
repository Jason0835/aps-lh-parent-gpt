<template>
  <el-dialog
    :title="title"
    :visible="visible"
    width="1000px"
    @close="hide"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    :append-to-body="true"
  >
    <info-form
      class="form-item-height"
      ref="form"
      :defaultValue="defaultValue"
      :rules="rules"
      :columns="columns"
      label-position="right"
      label-width="120px"
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

import {
  editProductStatus,
  modifyQtyProductStatus,
} from "@/api/cx/productStatus.js";

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
          label: this.$t("ui.data.column.productStatus.monthPlanApsVersion"),
          prop: "monthPlanApsVersion",
        },

        {
          label: this.$t("ui.data.column.productStatus.bomDataVersion"),
          prop: "bomDataVersion",
        },
        {
          label: this.$t("ui.data.column.productStatus.sapCode"),
          prop: "sapCode",
        },
        {
          label: this.$t("ui.data.column.productStatus.embryoCode"),
          prop: "embryoCode",
        },
        {
          label: this.$t("ui.data.column.productStatus.productStatus"),
          prop: "productStatus",
          type: "select",
          disabled: this.editType == "2",
        },
        {
          label: this.$t("ui.data.column.productStatus.markUnProduct"),
          prop: "markUnProduct",
          type: "select",
          disabled: this.editType == "2",
        },
        {
          label: this.$t("ui.data.column.productStatus.monthPlanTotalQty"),
          prop: "monthPlanTotalQty",
        },
        {
          label: this.$t("ui.data.column.stock.modifyNum"),
          prop: "monthPlanTotalModifyQty",
          disabled: this.editType == "1",
        },
        {
          label: this.$t("ui.data.column.productStatus.specDimension"),
          prop: "specDimension",
        },

        {
          label: this.$t("ui.data.column.remark"),
          prop: "scheduleDate",
          span: 12,
          type: "input",
        },
      ],
    };
  },
  computed: {
    title: function () {
      return this.$t("ui.data.column.productStatus.modalName");
    },
  },
  methods: {
    // api
    async save(params) {
      try {
        this.loading = true;
        const data = await editProductStatus(params);
        this.$modal.msgSuccess(data.msg);
        this.$emit("success");
        this.hide();
      } catch (error) {
        console.error(error);
        this.loading = false;
      }
    },
    async modifyQty(params) {
      try {
        this.loading = true;
        const data = await modifyQtyProductStatus(params);
        this.$modal.msgSuccess(data.msg);
        this.$emit("success");
        this.hide();
      } catch (error) {
        console.error(error);
        this.loading = false;
      }
    },
    //utils
    show(data, editType) {
      this.visible = true;
      this.editType = editType;
      if (data) {
        this.isEdit = true;
        this.defaultValue = {
          ...data,
        };
      }
    },
    hide() {
      this.defaultValue = {};
      this.$refs.form.triggerResetForm();
      // this.resetForm("infoForm");
      this.isEdit = false;
      this.visible = false;
    },

    handleConfirm() {
      this.$refs.form.triggerConfirm((valid) => {
        if (!valid) {
          return;
        }
        if (this.editType === "1") {
          this.save({
            ...this.form,
          });
        } else if (this.editType == 2) {
          this.modifyQty({
            ...this.form,
          });
        }
      });
    },
  },
};
</script>
