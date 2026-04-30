<template>
  <el-dialog
    :title="title"
    :visible="visible"
    width="600px"
    @close="hide"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
  >
    <el-form
      class="form-item-height"
      ref="form"
      :model="form"
      :rules="rules"
      label-position="top"
      label-width="80px"
    >
      <el-form-item
        :label="$t('ui.data.column.scheduleResult.produceLine')"
        prop="machineId"
      >
        <el-select
          class="w100"
          :placeholder="$t('ui.data.column.selectMachineName')"
          v-model="form.machineId"
        >
          <el-option
            v-for="item in machines"
            :key="item.id"
            :value="item.id + ''"
            :label="item.machineName"
          ></el-option>
        </el-select>
      </el-form-item>
    </el-form>
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

import { batchChangeMachine } from "@/api/xwyy/scheduleResult.js";

export default {
  components: {},
  data() {
    return {
      loading: false,
      visible: false,
      isEdit: false,
      editType: null,
      form: {},
      rules: {
        machineId: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
      },
      tableRows: [],
    };
  },
  computed: {
    ...mapState({
      machines: (state) => state.fiberPress.machines,
    }),
    title: function () {
      return this.$t("ui.data.column.xwyy.scheduleResult.modelName");
    },
  },
  methods: {
    // api
    async save(params) {
      try {
        this.loading = true;

        const result = await batchChangeMachine(params.machineId, {
          selects: JSON.stringify(this.tableRows),
        });
        if (result.code == 200) {
          this.$modal.msgSuccess("操作成功");
          this.$emit("success");
          this.hide();
        }

        this.loading = false;
      } catch (error) {
        console.error(error);
        this.loading = false;
      }
    },

    //utils
    show(data) {
      this.visible = true;
      this.tableRows = data;
      this.isEdit = true;
    },
    hide() {
      this.form = {};
      this.tableRows = null;
      this.$refs.form.resetForm();
      this.isEdit = false;
      this.visible = false;
    },

    handleConfirm() {
      if (this.tableRows.length === 1) {
        if (this.tableRows[0].machineId === this.form.machineId) {
          this.hide();
          return;
        }
      }

      this.$refs.form.validate((valid) => {
        if (valid) {
          this.save({
            ...this.form,
          });
        }
      });
    },
  },
};
</script>
