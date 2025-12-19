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
        :label="$t('ui.data.column.cxScheduleResult.cxMachineCode')"
        prop="cxMachineCode"
      >
        <el-select class="w100" v-model="form.cxMachineCode" filterable>
          <!-- <el-option-group key="定点机台" label="定点机台"></el-option-group>
          <el-option-group key="其他机台" label="其他机台"></el-option-group> -->
          <el-option
            v-for="item in moldingMachines"
            :key="item.moldingMachineCode"
            :value="item.moldingMachineCode"
            :label="item.moldingMachineCode"
          >
          </el-option>
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

import infoForm from "@/views/components/infoForm.vue";

import {
  validateChangeMachine,
  cxScheduleResultEdit,
  changeMachine,
} from "@/api/cx/cxScheduleResult";

export default {
  components: { infoForm },
  data() {
    return {
      loading: false,
      visible: false,
      isEdit: false,
      editType: null,
      form: {},
      rules: {
        cxMachineCode: [
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
      return this.$t("ui.data.column.cxScheduleResult.modalName");
    },
    ...mapState({
      moldingMachines: (state) => state.molding.machines,
    }),
  },
  methods: {
    // api
    async save(params) {
      try {
        this.loading = true;
        // let valid = await validateChangeMachine(params);
        // if (valid) {
        //   const result = await cxScheduleResultEdit(params);
        //   if (result.code == 200) {
        //     this.$emit("success");
        //     this.hide();
        //   }
        // }
        const result = await changeMachine({
          id: params.id,
          cxMachineCode: params.cxMachineCode,
          factoryCode: params.factoryCode,
        });
        if (result.code == 200) {
          this.$modal.msgSuccess(result.msg);
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
      if (data) {
        this.isEdit = true;
        this.form = {
          ...data,
        };
      }
    },
    hide() {
      this.form = {};
      this.$refs.form.resetForm();
      this.isEdit = false;
      this.visible = false;
    },

    handleConfirm() {
      this.$refs.form.validate((valid) => {
        if (valid) {
          this.save({
            ...this.form,
            local: "zjt",
          });
        }
      });
    },
  },
};
</script>
