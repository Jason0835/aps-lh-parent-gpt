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

import infoForm from "@/views/components/infoForm.vue";

import { editStockLocationMapping } from "@/api/cx/stockLocationMapping";

export default {
  components: { infoForm },
  inject: ["parentDict"],
  data() {
    return {
      loading: false,
      visible: false,
      isEdit: false,
      editType: null,
      form: {},
      rules: {
        apsStorageLocation: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "blur",
          },
        ],
        mpsStorageLocation: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
      },
      columns: [
        {
          label: this.$t(
            "ui.data.column.stockLocationMapping.apsStorageLocation"
          ),
          prop: "apsStorageLocation",
          span: 24,
          required: true,
          type: "select", //STORAGE_LOCATION
          dictData: this.parentDict.type.STORAGE_LOCATION,
        },
        {
          label: this.$t(
            "ui.data.column.stockLocationMapping.mpsStorageLocation"
          ),
          prop: "mpsStorageLocation",
          span: 24,
          required: true,
        },
        {
          label: this.$t("ui.data.column.stockLocationMapping.tireStoreType"),
          prop: "tireStoreType",
          span: 24,
          required: true,
          type: "select", //STOCK_LOCATION_TIRE_TYPE
          dictData: this.parentDict.type.STOCK_LOCATION_TIRE_TYPE,
        },
        {
          label: this.$t("ui.common.column.remark"),
          prop: "remark",
          span: 24,
          required: true,
          type: "textarea",
          maxlength: "300",
        },
      ],
    };
  },
  computed: {
    title: function () {
      return (
        (this.isEdit
          ? this.$t("common.button.edit")
          : this.$t("common.button.add")) +
        this.$t("ui.data.column.cx.monthStock.modelName")
      );
    },
  },
  methods: {
    // api
    async save(params) {
      try {
        this.loading = true;

        const res = await editStockLocationMapping(params);
        this.$modal.msgSuccess(res.msg);
        this.$emit("success");
        this.hide();

        this.loading = false;
      } catch (error) {
        console.log(error);
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
      this.$refs.form.triggerResetForm();
      // this.resetForm("infoForm");
      this.isEdit = false;
      this.visible = false;
    },
    handleConfirm() {
      this.$refs.form.triggerConfirm(this.save);
    },

    handleEmbryoCode(val) {
      console.log(val);
      if (!val) {
        return;
      }
      getProductEmbryoVersions({ embryoCode: val })
        .then((res) => {
          console.log(res);
        })
        .catch((e) => {
          console.error(e);
        });
    },
  },
};
</script>
