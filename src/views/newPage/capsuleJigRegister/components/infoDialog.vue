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
import { mapState } from "vuex";

import { saveMdmCapsuleChuck } from "@/api/monthplan/mdmCapsuleChuck";

import infoForm from "@/views/components/infoForm.vue";
import d from "highlight.js/lib/languages/d";
import { di } from "@fullcalendar/core/internal-common";

export default {
  components: { infoForm },
  inject: ["parentDict"],
  data() {
    // 验证大于0的整数
    const validatePositiveInteger = (rule, value, callback) => {
      if (value === "" || value === null || value === undefined) {
        if (rule.required) {
          return callback(new Error(this.$t("common.rule.noData")));
        }
        return callback();
      }
      const strValue = String(value).trim();

      // 检查是否只包含数字
      if (!/^\d+$/.test(strValue)) {
        return callback(
          new Error(this.$t("common.rule.noPoint"))
        );
      }

      // 转换为数字
      const numValue = Number(strValue);
      if (numValue > 999999) {
        return callback(new Error(this.$t("common.rule.inoutMax")));
      }

      if (!Number.isInteger(numValue)) {
        return callback(new Error(this.$t("common.rule.peleaseInteger")));
      }

      callback();
    };
    return {
      loading: false,
      visible: false,
      isEdit: false,
      editType: null,
      form: {},
      rules: {
        factoryCode: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "change",
          },
        ],
        specifications: [
          {
            required: false,
            message: this.$t("common.rule.input"),
            trigger: "change",
          },
        ],
        internalQty: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "change",
          },
          {
            validator: (rule, value, callback) => {
              validatePositiveInteger({ required: true }, value, callback);
            },
            trigger: ["change"],
          },
        ],
        newChuckQty: [
        {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "change",
          },
          {
            validator: (rule, value, callback) => {
              validatePositiveInteger({ required: true }, value, callback);
            },
            trigger: ["change"],
          },
        ],
        lineType: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "change",
          },
        ],
        proSize: [
          {
            required: false,
            message: this.$t("common.rule.input"),
            trigger: "change",
          },
        ],
      },
    };
  },
  computed: {
    ...mapState({
      moldingMachines: (state) => state.molding.machines,
    }),
    title: function () {
      return this.isEdit
        ? this.$t("common.button.edit")
        : this.$t("common.button.add");
    },
    columns() {
      return [
        {
          prop: "factoryCode",
          label: this.$t("common.factory"),
          type: "select",
          dictData: this.parentDict.type.biz_factory_name,
        },
        {
          prop: "specifications",
          label: this.$t("ui.data.column.capsuleChuck.specifications"),
          maxlength: 64,
        },
        {
          prop: "proSize",
          label: this.$t("ui.data.column.capsuleChuck.proSize"),
          maxlength: 64,
        },
        {
          prop: "internalQty",
          label: this.$t("ui.data.column.capsuleChuck.internalQty"),
          type: "number",
          listeners: {
            change: this.handleChangeGetMachines,
          },
        },

        {
          prop: "newChuckQty",
          label: this.$t("新卡盘"),
          type: "number",
          listeners: {
            change: this.handleChangeGetMachines,
          },
        },
        {
          prop: "totalQty",
          label: this.$t("common.sum"),
          type: "number",
          disabled: true,
        },
        {
          prop: "remark",
          label: this.$t("common.remark"),
          maxlength: 256,
        },
      ];
    },
  },
  methods: {
    // api
    async save(params) {
      try {
        this.loading = true;

        const res = await saveMdmCapsuleChuck(params);
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
          totalQty: Number(data.internalQty) + Number(data.newChuckQty),
        };
      } else {
        this.form = {
          factoryCode: "116",
        };
      }
    },
    handleChangeGetMachines() {
      if (this.form.internalQty && this.form.newChuckQty) {
        this.form.totalQty =
          Number(this.form.internalQty) + Number(this.form.newChuckQty);
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
