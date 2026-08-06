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
      <el-button @click="hide">{{ this.$t('common.button.cancel') }}</el-button>
      <el-button type="primary" :loading="loading" @click="handleConfirm">{{
        this.$t('common.button.confirm')
      }}</el-button>
    </template>
  </el-dialog>
</template>

<script>
import { mapState } from 'vuex';

import infoForm from '@/views/components/infoForm.vue';

import { addMachineMaintenance, updateMachineMaintenance } from '@/api/nc/machineMaintenance';

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
        machineCode: [
          {
            required: true,
            message: this.$t('common.rule.input'),
            trigger: 'blur',
          },
        ],
        stopStartTime: [
          {
            required: true,
            message: this.$t('common.rule.input'),
            trigger: 'blur',
          },
        ],
        stopEndTime: [
          {
            required: true,
            message: this.$t('common.rule.input'),
            trigger: 'blur',
          },
        ],
      },
    };
  },
  computed: {
    ...mapState({
      machines: (state) => state.insideLiner.machines,
    }),
    title: function () {
      return (
        (this.isEdit
          ? this.$t('common.button.edit')
          : this.$t('common.button.add')) +
        this.$t('ui.data.column.nc.machineMaintenance.modelName')
      );
    },
    columns() {
      return [
        {
          label: this.$t('ui.data.column.nc.machineMaintenance.machineCode'),
          prop: 'machineCode',
          span: 12,
          required: true,
          type: 'select',
          dictData: this.machines,
          labelKey: 'machineName',
          valueKey: 'machineCode',
        },
        {
          label: this.$t('ui.data.column.nc.machineMaintenance.stopStartTime'),
          prop: 'stopStartTime',
          span: 12,
          required: true,
          type: 'datetime',
        },
        {
          label: this.$t('ui.data.column.nc.machineMaintenance.stopEndTime'),
          prop: 'stopEndTime',
          span: 12,
          required: true,
          type: 'datetime',
        },
        {
          label: this.$t('ui.common.column.remark'),
          prop: 'remark',
          span: 24,
          type: 'textarea',
          maxlength: '300',
        },
      ];
    },
  },
  methods: {
    async save(params) {
      try {
        this.loading = true;

        const res = this.isEdit ? await updateMachineMaintenance(params) : await addMachineMaintenance(params);
        this.$modal.msgSuccess(res.msg);
        this.$emit('success');
        this.hide();

        this.loading = false;
      } catch (error) {
        console.log(error);
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
