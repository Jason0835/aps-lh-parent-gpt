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
  validateProduction,
  editProductStatus,
  getInfoModifyQty,
} from "@/api/cx/productStatus.js";

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
        scheduleDate: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "blur",
          },
        ],
        cxMachineCode: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "blur",
          },
        ],
        storageLocation: [
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
      return this.$t("ui.data.column.productStatus.modalName");
    },
    columns() {
      return [
        {
          label: this.$t("ui.data.column.scheduleResult.productionDate"),
          prop: "scheduleDate",
          type: "date",
          valueFormat: "yyyy-MM-dd",
          required: true,
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
          label: this.$t("ui.data.column.cxScheduleResult.cxMachineCode"),
          prop: "cxMachineCode",
          type: "select",
          required: true,
        },
        {
          label: this.$t("ui.data.column.cxScheduleResult.storageLocation"),
          prop: "storageLocation",
          type: "select",
          dictData: this.parentDict.type.STORAGE_LOCATION, // STORAGE_LOCATION
          required: true,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.class1PlanQty"),
          prop: "class1PlanQty",
          type: "number",
          min: 0,
          max: 9999999,
          precision: 0,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.class2PlanQty"),
          prop: "class2PlanQty",
          type: "number",
          min: 0,
          max: 9999999,
          precision: 0,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.class3PlanQty"),
          prop: "class3PlanQty",
          type: "number",
          min: 0,
          max: 9999999,
          precision: 0,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.class4PlanQty"),
          prop: "class4PlanQty",
          type: "number",
          min: 0,
          max: 9999999,
          precision: 0,
        },
        {
          label: this.$t("ui.data.column.scheduleResult.class5PlanQty"),
          prop: "class5PlanQty",
          type: "number",
          min: 0,
          max: 9999999,
          precision: 0,
        },
      ];
    },
  },
  methods: {
    // api
    async getInfoModifyQty(id) {
      try {
        this.loading = true;
        const data = await getInfoModifyQty({ 
          id,
        });
        this.form = {
          ...data,
        };
        this.loading = false;
      } catch (error) {
        console.error(error);
        this.loading = false;
      }
    },

    async valid(params) {
      return new Promise((resolve, reject) => {
        validateProduction(params)
          .then((valid) => {
            if (valid.msg) {
              this.$confirm(valid.msg)
                .then(() => {
                  resolve();
                })
                .catch((e) => {
                  reject(e);
                });
            } else {
              resolve();
            }
          })
          .catch((error) => {
            console.error(error);
            reject(error);
          });
      });
    },

    async save(params) {
      try {
        this.loading = true;
        await this.valid(params);
        const data = await editProductStatus(params);
        this.$modal.msgSuccess(data.msg);
        this.$emit("success");
        this.hide();
        this.loading = false;
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
        this.getInfoModifyQty(data.id);
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
        this.valid({
          ...this.form,
        });
      });
    },
  },
};
</script>
