<template>
  <el-dialog :title="title" :visible="visible" width="720px" @close="hide" :close-on-click-modal="false" :close-on-press-escape="false" :append-to-body="true">
    <info-form class="form-item-height" ref="form" :form="form" :rules="rules" :columns="columns" label-position="right" label-width="130px" v-loading="loading" />
    <template slot="footer">
      <el-button @click="hide">{{ $t("common.button.cancel") }}</el-button>
      <el-button type="primary" :loading="loading" @click="handleConfirm">{{ $t("common.button.confirm") }}</el-button>
    </template>
  </el-dialog>
</template>

<script>
import { addStorageLaneLimit, updateStorageLaneLimit } from "@/api/cd15/storageLaneLimit";
import { getCd15ParamValue } from "@/api/cd15/cd15Params";
import infoForm from "@/views/components/infoForm.vue";

const DEFAULT_FACTORY_CODE = "116";
const DEFAULT_MAX_CAR_NUM_PARAM_CODE = "SYS0601039";

export default {
  components: { infoForm },
  inject: ["parentDict"],
  props: {
    steelStripOptions: {
      type: Array,
      default: () => [],
    },
  },
  data() {
    const requiredSelect = { required: true, message: this.$t("common.rule.select"), trigger: "change" };
    const requiredInput = { required: true, message: this.$t("common.rule.input"), trigger: "blur" };
    const maxCarNumPositive = {
      validator: (rule, value, callback) => {
        if (value === null || value === undefined || value === "") {
          return callback(new Error(this.$t("common.rule.input")));
        }
        if (!/^\d+$/.test(String(value)) || Number(value) <= 0) {
          return callback(new Error(this.$t("ui.data.column.cd15StorageLaneLimit.maxCarNumPositive")));
        }
        callback();
      },
      trigger: "blur",
    };
    const carNumNonNegative = {
      validator: (rule, value, callback) => {
        if (value === null || value === undefined || value === "") {
          return callback();
        }
        if (!/^\d+$/.test(String(value)) || Number(value) < 0) {
          return callback(new Error(this.$t("ui.data.column.cd15StorageLaneLimit.carNumNonNegative")));
        }
        callback();
      },
      trigger: "blur",
    };
    const carNumNotExceedMax = {
      validator: (rule, value, callback) => {
        if (value === null || value === undefined || value === "") {
          return callback();
        }
        const max = this.form.maxCarNum;
        if (max !== null && max !== undefined && max !== "" && Number(value) > Number(max)) {
          return callback(new Error(this.$t("ui.data.column.cd15StorageLaneLimit.carNumExceedMax")));
        }
        callback();
      },
      trigger: "blur",
    };
    const emptyLaneCarNumZero = {
      validator: (rule, value, callback) => {
        if (value === null || value === undefined || value === "") {
          return callback();
        }
        const materialCode = this.form.materialCode;
        if ((!materialCode || materialCode === "") && Number(value) !== 0) {
          return callback(new Error(this.$t("ui.data.column.cd15StorageLaneLimit.emptyLaneCarNumZero")));
        }
        callback();
      },
      trigger: "blur",
    };
    return {
      loading: false,
      visible: false,
      isEdit: false,
      form: {},
      rules: {
        factoryCode: [requiredSelect],
        laneDate: [requiredInput],
        shiftCode: [requiredSelect],
        storageLaneCode: [requiredInput],
        carNum: [carNumNonNegative, carNumNotExceedMax, emptyLaneCarNumZero],
        maxCarNum: [requiredInput, maxCarNumPositive],
      },
    };
  },
  computed: {
    title() {
      return this.isEdit ? this.$t("common.button.edit") : this.$t("common.button.add");
    },
    columns() {
      return [
        { prop: "factoryCode", label: this.$t("ui.data.column.cd15StorageLaneLimit.factoryCode"), type: "select", dictData: this.parentDict.type.biz_factory_name, filterable: true },
        { prop: "materialCode", label: this.$t("ui.data.column.cd15StorageLaneLimit.materialCode"), type: "select", dictData: this.steelStripOptions, filterable: true, clearable: true },
        { prop: "laneDate", label: this.$t("ui.data.column.cd15StorageLaneLimit.laneDate"), type: "date" },
        { prop: "shiftCode", label: this.$t("ui.data.column.cd15StorageLaneLimit.shiftCode"), type: "select", dictData: this.parentDict.type.class_num_three_plan, filterable: true },
        { prop: "storageLaneCode", label: this.$t("ui.data.column.cd15StorageLaneLimit.storageLaneCode"), maxlength: 50 },
        { prop: "carNum", label: this.$t("ui.data.column.cd15StorageLaneLimit.carNum"), type: "number" },
        { prop: "maxCarNum", label: this.$t("ui.data.column.cd15StorageLaneLimit.maxCarNum"), type: "number", required: true },
        { prop: "remark", label: this.$t("ui.common.column.remark"), type: "textarea", rows: 3, maxlength: 900 },
      ];
    },
  },
  methods: {
    async save(params) {
      this.loading = true;
      try {
        const payload = {
          ...params,
          carNum: params.carNum === null || params.carNum === undefined || params.carNum === "" ? 0 : params.carNum,
        };
        const res = this.isEdit ? await updateStorageLaneLimit(payload) : await addStorageLaneLimit(payload);
        this.$modal.msgSuccess(res.msg);
        this.$emit("success");
        this.hide();
      } finally {
        this.loading = false;
      }
    },
    async show(data) {
      this.visible = true;
      if (data) {
        this.isEdit = true;
        this.form = { ...data };
      } else {
        this.form = { factoryCode: DEFAULT_FACTORY_CODE, carNum: 0 };
        try {
          const response = await getCd15ParamValue(DEFAULT_FACTORY_CODE, DEFAULT_MAX_CAR_NUM_PARAM_CODE);
          const paramValue = response && response.data !== undefined ? response.data : response;
          if (/^\d+$/.test(String(paramValue)) && Number(paramValue) > 0) {
            this.form = { ...this.form, maxCarNum: Number(paramValue) };
          }
        } catch {
          this.form = { ...this.form };
        }
      }
    },
    hide() {
      this.form = {};
      this.$refs.form && this.$refs.form.triggerResetForm();
      this.isEdit = false;
      this.visible = false;
    },
    handleConfirm() {
      this.$refs.form.triggerConfirm(this.save);
    },
  },
};
</script>
