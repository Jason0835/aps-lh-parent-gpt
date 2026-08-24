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
import { saveMachineChuck, checkMachineChuckUnique } from "@/api/tq/machineChuck";
import { listEnabledMachines } from "@/api/tq/machine";

export default {
  components: { infoForm },
  data() {
    return {
      loading: false,
      machineLoading: false,
      visible: false,
      isEdit: false,
      form: {},
      machineList: [],
      rules: {
        machineCode: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "change",
          },
        ],
        chuckCode: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
          // 机台编码+寸口编码组合唯一性校验（表单红字提示，后端保存时兜底校验）
          {
            validator: this.checkUniqueValidator,
            trigger: ["blur", "change"],
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
        this.$t("ui.tq.machineChuck.column.modalName")
      );
    },
    columns() {
      return [
        {
          label: this.$t("ui.tq.machineChuck.column.machineCode"),
          prop: "machineCode",
          span: 24,
          required: true,
          type: "select",
          dictData: this.machineList,
          filterable: true,
          loading: this.machineLoading,
          props: {
            label: "machineCode",
            value: "machineCode",
          },
          onFocus: this.handleMachineFocus,
          // 机台编码变化后重新触发寸口编码的唯一性校验（组合维度变化）
          listeners: {
            change: () => this.reValidateChuckCode(),
          },
        },
        {
          label: this.$t("ui.tq.machineChuck.column.chuckCode"),
          prop: "chuckCode",
          span: 24,
          required: true,
          maxlength: "50",
        },
        {
          label: this.$t("ui.tq.machineChuck.column.chuckName"),
          prop: "chuckName",
          span: 24,
          maxlength: "100",
        },
        {
          label: this.$t("ui.tq.machineChuck.column.inchSize"),
          prop: "inchSize",
          span: 24,
          type: "number",
        },
        {
          label: this.$t("ui.common.column.remark"),
          prop: "remark",
          span: 24,
          type: "textarea",
          maxlength: "500",
        },
      ];
    },
  },
  methods: {
    /**
     * 机台编码+寸口编码组合唯一性校验器（表单红字提示）
     * 后端返回 "1"（NOT_UNIQUE）不唯一 / "0"（UNIQUE）唯一
     */
    checkUniqueValidator(rule, value, callback) {
      // 机台编码或寸口编码未填写完整时跳过唯一性校验（由必填规则处理）
      if (!this.form.machineCode || !value) {
        callback();
        return;
      }
      checkMachineChuckUnique({ ...this.form })
        .then((res) => {
          if (String(res) === "1") {
            callback(new Error(this.$t("ui.tq.machineChuck.column.conflict")));
          } else {
            callback();
          }
        })
        .catch(() => {
          // 校验接口异常时放行，由保存时后端唯一性校验兜底拦截
          callback();
        });
    },
    /**
     * 机台编码变化后重新触发寸口编码字段校验（组合唯一性维度变化）
     */
    reValidateChuckCode() {
      if (this.$refs.form && this.$refs.form.$refs.infoForm && this.form.chuckCode) {
        this.$refs.form.$refs.infoForm.validateField("chuckCode");
      }
    },
    async save(params) {
      try {
        this.loading = true;
        const res = await saveMachineChuck(params);
        this.$modal.msgSuccess(res.msg);
        this.$emit("success");
        this.hide();
      } catch (error) {
        console.log(error);
      } finally {
        this.loading = false;
      }
    },
    async loadMachineList() {
      this.machineLoading = true;
      try {
        const res = await listEnabledMachines();
        const list = Array.isArray(res) ? res : (res.data || res.rows || []);
        this.machineList = list;
      } catch (error) {
        console.log(error);
      } finally {
        this.machineLoading = false;
      }
    },
    handleMachineFocus() {
      if (this.machineList.length === 0) {
        this.loadMachineList();
      }
    },
    async show(data) {
      this.visible = true;
      this.machineList = [];
      if (data) {
        this.isEdit = true;
        this.form = {
          ...data,
        };
        // 先加载所有机台列表
        await this.loadMachineList();
        // 如果当前机台不在列表中，添加到列表
        if (data.machineCode && data.machineName) {
          const exists = this.machineList.some(item => item.machineCode === data.machineCode);
          if (!exists) {
            this.machineList.unshift({
              machineCode: data.machineCode,
              machineName: data.machineName,
            });
          }
        }
      } else {
        this.isEdit = false;
        this.form = {};
        // 新增时预加载机台列表
        await this.loadMachineList();
      }
    },
    hide() {
      this.form = {};
      this.$refs.form.triggerResetForm();
      this.isEdit = false;
      this.visible = false;
      this.machineList = [];
    },
    handleConfirm() {
      this.$refs.form.triggerConfirm(this.save);
    },
  },
};
</script>
