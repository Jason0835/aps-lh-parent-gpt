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
      :form="form"
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
import { getInfoModifyQty } from "@/api/cx/cxScheduleResult.js";

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
    title: function () {
      return this.$t("ui.data.column.productStatus.modalName");
    },
    columns() {
      return [
        {
          label: this.$t("ui.data.column.productStatus.monthPlanApsVersion"),
          prop: "monthPlanApsVersion",
          disabled: true,
        },

        {
          label: this.$t("ui.data.column.productStatus.bomDataVersion"),
          prop: "bomDataVersion",
          disabled: true,
        },
        {
          label: this.$t("ui.data.column.productStatus.sapCode"),
          prop: "sapCode",
          disabled: true,
        },
        {
          label: this.$t("ui.data.column.productStatus.embryoCode"),
          prop: "embryoCode",
          disabled: true,
        },
        {
          label: this.$t("ui.data.column.productStatus.productStatus"),
          prop: "productStatus",
          disabled: this.editType == "2",
          type: "select",
          dictData: this.parentDict.type.PRODUCT_STATUS,
        },
        {
          label: this.$t("ui.data.column.productStatus.markUnProduct"),
          prop: "markUnProduct",
          disabled: this.editType == "2",
          type: "select",
          dictData: this.parentDict.type.MARK_UN_PRODUCT,
        },
        {
          label: this.$t("ui.data.column.productStatus.monthPlanTotalQty"),
          prop: "monthPlanTotalQty",
          disabled: true,
        },
        {
          label: this.$t("ui.data.column.stock.modifyNum"),
          prop: "monthPlanTotalModifyQty",
          disabled: this.editType == "1",
          type: "number",
          min: -999999,
          max: 999999,
          precision: 0,
        },
        {
          label: this.$t("ui.data.column.productStatus.specDimension"),
          prop: "specDimension",
          disabled: true,
        },

        {
          label: this.$t("ui.data.column.remark"),
          prop: "scheduleDate",
          type: "textarea",
          maxlength: "300",
        },
      ];
    },
  },
  methods: {
    // api
    async getInfo() {
      try {
        this.loading = true;
        const res = await getInfoModifyQty(params);
        this.form = {
          ...res
        }
        this.loading = false;
      } catch (error) {
        console.error(error);
        this.loading = false;
      }
    },

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
        if (editType === "1") {
          this.form = {
            ...data,
          };
        } else if (editType === "2") {
          this.getInfo(data);
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
